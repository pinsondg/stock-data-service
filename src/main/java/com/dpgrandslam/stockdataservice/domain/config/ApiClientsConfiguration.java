package com.dpgrandslam.stockdataservice.domain.config;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.dpgrandslam.stockdataservice.adapter.apiclient.CNNFearGreedClient;
import com.dpgrandslam.stockdataservice.adapter.apiclient.tiingo.TiingoApiClient;
import com.dpgrandslam.stockdataservice.domain.util.BasicAuthorizationTarget;
import feign.Feign;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import feign.okhttp.OkHttpClient;
import feign.slf4j.Slf4jLogger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiClientsConfiguration {

    @Bean("TiingoApiClientConfigurationProperties")
    @ConfigurationProperties(prefix = "api.client.tiingo")
    public ApiClientConfigurationProperties tiingoApiClientConfigurationProperties() {
        return new ApiClientConfigurationProperties();
    }

    @Bean("YahooFinanceApiClientConfigurationProperties")
    @ConfigurationProperties(prefix = "api.client.yahoo-finance")
    public ApiClientConfigurationProperties yahooFinanceApiClientConfigurationProperties() {
        return new ApiClientConfigurationProperties();
    }

    @Bean
    public TiingoApiClient tiingoApiClient(
            @Qualifier("TiingoApiClientConfigurationProperties") ApiClientConfigurationProperties configurationProperties) {
        return Feign.builder()
                .decoder(new GsonDecoder())
                .encoder(new GsonEncoder())
                .logger(new Slf4jLogger(TiingoApiClient.class))
                .client(new OkHttpClient())
                .target(new BasicAuthorizationTarget<>(TiingoApiClient.class, configurationProperties));
    }

    @Bean("CNNClientConfigurationProperties")
    @ConfigurationProperties(prefix = "api.client.cnn")
    public ApiClientConfigurationProperties cnnClientConfigurationProperties() {
        return new ApiClientConfigurationProperties();
    }

    @Bean("FearGreedClientConfigurationProperties")
    @ConfigurationProperties(prefix = "api.client.fear-greed")
    public ApiClientConfigurationProperties fearGreedClientConfigurationProperties() {
        return new ApiClientConfigurationProperties();
    }

    @Bean
    public CNNFearGreedClient cnnFearGreedClient(@Qualifier("FearGreedClientConfigurationProperties") ApiClientConfigurationProperties configurationProperties) {
        return Feign.builder()
                .decoder(new GsonDecoder())
                .encoder(new GsonEncoder())
                .logger(new Slf4jLogger(CNNFearGreedClient.class))
                .client(new OkHttpClient())
                .target(new BasicAuthorizationTarget<>(CNNFearGreedClient.class, configurationProperties));
    }

    @Bean
    public AmazonS3 amazonS3() {
        return AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
    }
}
