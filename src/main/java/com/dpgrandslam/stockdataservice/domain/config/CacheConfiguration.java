package com.dpgrandslam.stockdataservice.domain.config;

import com.dpgrandslam.stockdataservice.domain.model.tiingo.TiingoStockSearchResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class CacheConfiguration {


    @Bean
    public Cache<String, List<TiingoStockSearchResponse>> stockSearchCache() {
        return Caffeine.newBuilder()
                .maximumSize(100)
                .build();
    }
}
