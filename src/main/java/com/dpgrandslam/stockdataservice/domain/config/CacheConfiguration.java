package com.dpgrandslam.stockdataservice.domain.config;

import com.dpgrandslam.stockdataservice.domain.model.FearGreedIndex;
import com.dpgrandslam.stockdataservice.domain.model.options.HistoricalOption;
import com.dpgrandslam.stockdataservice.domain.model.options.Option;
import com.dpgrandslam.stockdataservice.domain.model.stock.EndOfDayStockData;
import com.dpgrandslam.stockdataservice.domain.model.stock.YahooFinanceQuote;
import com.dpgrandslam.stockdataservice.domain.model.tiingo.TiingoStockSearchResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

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
                .maximumSize(2)
                .build();
    }

    @Bean("TreasuryYieldCache")
    public Cache<Pair<LocalDate, LocalDate>, List<YahooFinanceQuote>> treasuryYieldCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.DAYS)
                .maximumSize(1000)
                .build();
    }

    @Bean
    public Cache<HistoricOptionsDataCacheKey, List<EndOfDayStockData>> endOfDayStockDataCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build();
    }

    @Bean
    public Cache<Pair<LocalDate, LocalDate>, List<FearGreedIndex>> fearGreedBetweenDatesCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(2, TimeUnit.HOURS)
                .maximumSize(2000)
                .build();
    }

    @Bean("VIXCache")
    public Cache<Pair<LocalDate, LocalDate>, List<YahooFinanceQuote>> vixCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(2, TimeUnit.DAYS)
                .maximumSize(1000)
                .build();
    }

    @Bean
    public Cache<SingleHistoricOptionCacheKey, HistoricalOption> singleOptionCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.HOURS)
                .maximumSize(1000)
                .build();
    }

    @Data
    @AllArgsConstructor
    public static class SingleHistoricOptionCacheKey {
        @NotNull
        private String ticker;

        @NotNull
        private LocalDate expiration;

        @NotNull
        private Option.OptionType optionType;

        @NotNull
        private Double strike;
    }

    @Data
    public static class HistoricOptionsDataCacheKey {

        private String ticker;
        private LocalDate startDate;
        private LocalDate endDate;

        private Function<HistoricOptionsDataCacheKey, Boolean> IS_WITHIN_BOUNDS_LEFT = (other) -> (other.getStartDate().isBefore(this.startDate) || other.getStartDate().isEqual(this.startDate))
                && (other.getEndDate().isAfter(this.startDate));

        private Function<HistoricOptionsDataCacheKey, Boolean> IS_WITHIN_BOUNDS_RIGHT = (other) -> (other.getEndDate().isAfter(this.endDate) || other.getEndDate().isEqual(this.endDate))
                && (other.getStartDate().isBefore(this.endDate));

        public HistoricOptionsDataCacheKey(@NonNull String ticker, @NonNull LocalDate startDate, @NonNull LocalDate endDate) {
            if (startDate.isAfter(endDate) || endDate.isBefore(startDate)) {
                throw new IllegalArgumentException("Start date cannot after end date and end date cannot be before start date.");
            }
            this.ticker = ticker;
            this.startDate = startDate;
            this.endDate = endDate;
        }

        public boolean isWithinBounds(HistoricOptionsDataCacheKey other) {
            return ticker.equals(other.ticker)
                    && (other.getStartDate().isAfter(this.startDate) || other.getStartDate().equals(this.startDate))
                    && (other.getEndDate().isBefore(this.endDate) || other.getEndDate().equals(this.endDate));
        }

        private boolean canMerge(HistoricOptionsDataCacheKey other) {
            return IS_WITHIN_BOUNDS_LEFT.apply(other) || IS_WITHIN_BOUNDS_RIGHT.apply(other);
        }
    }
}
