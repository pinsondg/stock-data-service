package com.dpgrandslam.stockdataservice.domain.config;

import com.amazonaws.services.s3.AmazonS3;
import com.dpgrandslam.stockdataservice.domain.jobs.optioncsv.AWSS3ItemReader;
import com.dpgrandslam.stockdataservice.domain.jobs.optioncsv.OptionCSVItemProcessor;
import com.dpgrandslam.stockdataservice.domain.model.OptionCSVFile;
import com.dpgrandslam.stockdataservice.domain.model.options.HistoricalOption;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.batch.core.Entity;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.*;
import org.springframework.batch.core.job.DefaultJobParametersValidator;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.HibernateItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.jpa.JpaDialect;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.io.IOException;

@Configuration
@EnableBatchProcessing
@Slf4j
@AllArgsConstructor
public class OptionCSVLoadJobConfig {

    public static final String JOB_NAME = "option-csv-load-job";
    public static final String STEP_NAME = "option-csv-load-step";

    private final EntityManagerFactory entityManagerFactory;

    @Bean
    public Step optionCsvLoadJobStep(StepBuilderFactory stepBuilderFactory,
                                     ItemReader<OptionCSVFile> itemReader,
                                     OptionCSVItemProcessor itemProcessor,
                                     ItemWriter<HistoricalOption> itemWriter) {
        return stepBuilderFactory.get(STEP_NAME)
                .<OptionCSVFile, HistoricalOption>chunk(100)
                .reader(itemReader)
                .processor(itemProcessor)
                .writer(itemWriter)
                .build();
    }

//    @Bean
//    public JpaItemWriter<HistoricalOption> getItemWriter() {
//        JpaItemWriter<HistoricalOption> itemWriter = new JpaItemWriter<>();
//        itemWriter.setEntityManagerFactory(entityManagerFactory);
//        return itemWriter;
//    }

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

//        MultiResourceItemReader<OptionCSVFile> itemReader = new MultiResourceItemReader<>();
//        itemReader.setDelegate(csvReader);
//        String formattedResourcePath = resourcePath;
//        if (!formattedResourcePath.endsWith("*.csv")) {
//            formattedResourcePath += "*.csv";
//        }
//        Resource[] resources = resourcePatternResolver.getResources(formattedResourcePath);
//        Arrays.stream(resources).forEach(x -> log.debug("Resource: ", x.getDescription()));
//        itemReader.setResources(new Resource[] {resourceLoader.getResource(resourcePath)});
//
        SynchronizedItemStreamReader<OptionCSVFile> synchronizedItemStreamReader = new SynchronizedItemStreamReader<>();
        synchronizedItemStreamReader.setDelegate(awss3ItemReader);

        return synchronizedItemStreamReader;
    }

    @Bean
    public BatchConfigurer batchConfigurer(DataSource dataSource) {
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

            @Override
            public PlatformTransactionManager getTransactionManager() {
//                JpaTransactionManager jpaTransactionManager = new JpaTransactionManager();
//                jpaTransactionManager.setDataSource(dataSource);
//                jpaTransactionManager.setEntityManagerFactory(entityManagerFactory);
//                return jpaTransactionManager;
                return super.getTransactionManager();
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
