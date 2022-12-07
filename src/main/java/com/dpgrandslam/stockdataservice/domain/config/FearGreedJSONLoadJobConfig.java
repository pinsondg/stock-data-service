package com.dpgrandslam.stockdataservice.domain.config;

import com.amazonaws.services.s3.AmazonS3;
import com.dpgrandslam.stockdataservice.domain.jobs.feargreedbatch.FearGreedJSONFile;
import com.dpgrandslam.stockdataservice.domain.jobs.feargreedbatch.FearGreedJSONItemProcessor;
import com.dpgrandslam.stockdataservice.domain.jobs.feargreedbatch.FearGreedJSONItemWriter;
import com.dpgrandslam.stockdataservice.domain.model.FearGreedIndex;
import com.dpgrandslam.stockdataservice.domain.util.AWSS3ItemReader;
import com.dpgrandslam.stockdataservice.domain.util.SingleJacksonJsonObjectReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.DefaultJobParametersValidator;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.json.JacksonJsonObjectReader;
import org.springframework.batch.item.json.JsonItemReader;
import org.springframework.batch.item.json.JsonObjectReader;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;

import java.util.Set;

@Configuration
@EnableBatchProcessing
@Slf4j
@RequiredArgsConstructor
public class FearGreedJSONLoadJobConfig {

    public static final String JOB_NAME = "fear-greed-json-load-job";
    public static final String STEP_NAME = "fear-greed-json-load-step";


    @Bean("fearGreedJSONLoadJobStep")
    public Step fearGreedJSONLoadJobStep(StepBuilderFactory stepBuilderFactory,
                                         ItemReader<FearGreedJSONFile> itemReader,
                                         FearGreedJSONItemProcessor itemProcessor,
                                         FearGreedJSONItemWriter itemWriter) {
        return stepBuilderFactory.get(STEP_NAME)
                .<FearGreedJSONFile, Set<FearGreedIndex>>chunk(5)
                .reader(itemReader)
                .processor(itemProcessor)
                .writer(itemWriter)
                .faultTolerant()
                .build();
    }

    @Bean("fearGreedJSONLoadJob")
    public Job fearGreedJSONLoadJob(JobBuilderFactory jobBuilderFactory, @Qualifier("fearGreedJSONLoadJobStep") Step fearGreedJSONLoadJobStep) {
        return jobBuilderFactory.get(JOB_NAME)
                .incrementer(new RunIdIncrementer())
                .validator(jobParametersValidator())
                .start(fearGreedJSONLoadJobStep)
                .build();
    }

    private JobParametersValidator jobParametersValidator() {
        return new DefaultJobParametersValidator(
                new String[] {"bucket", "keyPrefix"},
                new String[0]
        );
    }

    @Bean("fearGreedJSONFileItemStreamReader")
    @StepScope
    public ItemStreamReader<FearGreedJSONFile> fearGreedJSONFileItemStreamReader(@Value("#{jobParameters['bucket']}") String bucket,
                                                                                 @Value("#{jobParameters['keyPrefix']}") String keyPrefix,
                                                                                 AmazonS3 amazonS3) {
        JsonItemReader<FearGreedJSONFile> jsonFileReader = new JsonItemReader<>();
        jsonFileReader.setName("fearGreedJSONReader");
        jsonFileReader.setStrict(false);
        jsonFileReader.setJsonObjectReader(jsonObjectReader());

        AWSS3ItemReader<FearGreedJSONFile> awss3ItemReader = new AWSS3ItemReader<>(amazonS3);
        awss3ItemReader.setBucket(bucket);
        awss3ItemReader.setKeyPrefix(keyPrefix);
        awss3ItemReader.setDelegate(jsonFileReader);

        SynchronizedItemStreamReader<FearGreedJSONFile> synchronizedItemStreamReader = new SynchronizedItemStreamReader<>();
        synchronizedItemStreamReader.setDelegate(awss3ItemReader);

        return synchronizedItemStreamReader;
    }

    private JsonObjectReader<FearGreedJSONFile> jsonObjectReader() {
        return new SingleJacksonJsonObjectReader<>(FearGreedJSONFile.class);
    }





}
