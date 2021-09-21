package com.dpgrandslam.stockdataservice.domain.config;

import com.dpgrandslam.stockdataservice.domain.model.options.HistoricalOption;
import com.dpgrandslam.stockdataservice.domain.model.options.OptionPriceData;
import com.dpgrandslam.stockdataservice.domain.model.stock.YahooFinanceTenYearTreasuryYield;
import com.dpgrandslam.stockdataservice.domain.model.tiingo.TiingoStockSearchResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
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
    public Cache<String, Set<HistoricalOption.CacheableHistoricalOption>> historicalOptionCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .recordStats()
                .maximumSize(10)
                .build();
    }

    @Bean
    public Cache<LocalDate, YahooFinanceTenYearTreasuryYield> treasuryYieldCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.DAYS)
                .maximumSize(1000)
                .build();
    }
}
