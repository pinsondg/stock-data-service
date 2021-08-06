package com.dpgrandslam.stockdataservice.domain.config;

import com.dpgrandslam.stockdataservice.domain.model.options.HistoricalOption;
import com.dpgrandslam.stockdataservice.domain.model.tiingo.TiingoStockSearchResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfiguration {


    @Bean
    public Cache<String, List<TiingoStockSearchResponse>> stockSearchCache() {
        return Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(200, TimeUnit.DAYS)
                .build();
    }

    @Bean
    public Cache<String, Set<HistoricalOption>> historicalOptionCache() {
        return Caffeine.newBuilder()
                .maximumSize(50)
                .expireAfterWrite(1, TimeUnit.DAYS)
                .build();
    }
}
