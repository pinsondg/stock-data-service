package com.dpgrandslam.stockdataservice.domain.config;

import com.amazonaws.services.s3.AmazonS3;
import com.dpgrandslam.stockdataservice.domain.jobs.optioncsv.AWSS3ItemReader;
import com.dpgrandslam.stockdataservice.domain.jobs.optioncsv.OptionCSVItemProcessor;
import com.dpgrandslam.stockdataservice.domain.model.OptionCSVFile;
import com.dpgrandslam.stockdataservice.domain.model.options.HistoricalOption;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.*;
import org.springframework.batch.core.job.DefaultJobParametersValidator;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.io.IOException;
import java.time.format.DateTimeParseException;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableBatchProcessing
@Slf4j
@AllArgsConstructor
public class OptionCSVLoadJobConfig {

    public static final String JOB_NAME = "option-csv-load-job";
    public static final String STEP_NAME = "option-csv-load-step";

    @Bean
    public Step optionCsvLoadJobStep(StepBuilderFactory stepBuilderFactory,
                                     ItemReader<OptionCSVFile> itemReader,
                                     OptionCSVItemProcessor itemProcessor,
                                     ItemWriter<HistoricalOption> itemWriter) {
        return stepBuilderFactory.get(STEP_NAME)
                .<OptionCSVFile, HistoricalOption>chunk(50)
                .reader(itemReader)
                .processor(itemProcessor)
                .writer(itemWriter)
                .faultTolerant()
                .skipLimit(1000000)
                .skip(DateTimeParseException.class)
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public TaskExecutor taskExecutor() {
        int cores = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(cores - 1);
        taskExecutor.setMaxPoolSize(cores - 1);
        taskExecutor.setQueueCapacity(10);
        taskExecutor.setThreadNamePrefix("MultiThreaded-");
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return taskExecutor;
    }

    @Bean
    public Job optionCSVLoadJob(JobBuilderFactory jobBuilderFactory, Step optionCsvLoadJobStep) {
        return jobBuilderFactory.get(JOB_NAME)
                .incrementer(new RunIdIncrementer())
                .validator(jobParametersValidator())
                .start(optionCsvLoadJobStep)
                .build();
    }

    private JobParametersValidator jobParametersValidator() {
        return new DefaultJobParametersValidator(
                new String[] {"bucket", "keyPrefix"},
                new String[0]
        );
    }


    @Bean
    @StepScope
    public ItemStreamReader<OptionCSVFile> itemReader(@Value("#{jobParameters['bucket']}") String bucket,
                                                      @Value("#{jobParameters['keyPrefix']}") String keyPrefix,
                                                      AmazonS3 amazonS3) throws IOException {
        FlatFileItemReader<OptionCSVFile> csvReader = new FlatFileItemReader<>();
        csvReader.setLineMapper(lineMapper());
        csvReader.setName("optionCSVReader");
        csvReader.setLinesToSkip(1);

        AWSS3ItemReader awss3ItemReader = new AWSS3ItemReader(amazonS3);
        awss3ItemReader.setBucket(bucket);
        awss3ItemReader.setKeyPrefix(keyPrefix);
        awss3ItemReader.setDelegate(csvReader);

        SynchronizedItemStreamReader<OptionCSVFile> synchronizedItemStreamReader = new SynchronizedItemStreamReader<>();
        synchronizedItemStreamReader.setDelegate(awss3ItemReader);

        return synchronizedItemStreamReader;
    }

    @Bean
    public BatchConfigurer batchConfigurer(DataSource dataSource, EntityManagerFactory entityManagerFactory) {
        return new DefaultBatchConfigurer(dataSource) {

            @SneakyThrows
            @Override
            public JobLauncher getJobLauncher() {
                SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
                jobLauncher.setJobRepository(getJobRepository());
                jobLauncher.setTaskExecutor(new SimpleAsyncTaskExecutor());
                jobLauncher.afterPropertiesSet();
                return jobLauncher;
            }

            @SneakyThrows
            @Override
            public JobRepository getJobRepository() {
                JobRepositoryFactoryBean jobRepositoryFactoryBean = new JobRepositoryFactoryBean();
                jobRepositoryFactoryBean.setDataSource(dataSource);
                jobRepositoryFactoryBean.setTransactionManager(getTransactionManager());
                // set other properties
                return jobRepositoryFactoryBean.getObject();
            }

            @Override
            public PlatformTransactionManager getTransactionManager() {
                return new JpaTransactionManager(entityManagerFactory);
            }
        };
    }

    private LineMapper<OptionCSVFile> lineMapper() {
        DefaultLineMapper<OptionCSVFile> defaultLineMapper = new DefaultLineMapper<>();
        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();

        lineTokenizer.setDelimiter(",");
        lineTokenizer.setStrict(false);
        lineTokenizer.setNames("optionKey", "symbol", "expirationDate", "askPrice", "askSize", "bidPrice", "bidSize",
                "lastPrice", "putCall", "strikePrice", "volume", "openInterest", "underlyingPrice", "dataDate");
        BeanWrapperFieldSetMapper<OptionCSVFile> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(OptionCSVFile.class);

        defaultLineMapper.setLineTokenizer(lineTokenizer);
        defaultLineMapper.setFieldSetMapper(fieldSetMapper);

        return defaultLineMapper;
    }
}
