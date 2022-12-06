package com.dpgrandslam.stockdataservice.domain.util;

import com.dpgrandslam.stockdataservice.domain.jobs.optioncsv.OptionCSVItemProcessor;
import com.dpgrandslam.stockdataservice.domain.model.options.HistoricalOption;
import com.dpgrandslam.stockdataservice.domain.model.options.Option;
import com.dpgrandslam.stockdataservice.domain.model.options.OptionPriceData;
import com.dpgrandslam.stockdataservice.domain.service.HistoricOptionsDataService;
import io.swagger.models.auth.In;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class HistoricOptionBetweenDateCache {

    private final HistoricOptionsDataService historicOptionsDataService;

    private Map<OptionCacheKey, Pair<Long, Set<OptionPriceData>>> optionCache = Collections.synchronizedMap(new HashMap<>());

    public void addDataToCache(LocalDate startDate, LocalDate endDate) {
        log.info("Loading option data between dates {} and {} into the cache.", startDate, endDate);
        Instant start = Instant.now();
        historicOptionsDataService.findOptions(startDate, endDate).forEach(option -> {
            OptionCacheKey key = new OptionCacheKey(option.getTicker(), option.getExpiration(), option.getOptionType(), option.getStrike());
            Pair<Long, Set<OptionPriceData>> item = optionCache.getOrDefault(key, Pair.of(option.getId(), new HashSet<>(option.getOptionPriceData())));
            item.getRight().addAll(option.getOptionPriceData());
            optionCache.put(key, item);
        });
        Instant end = Instant.now();
        log.debug("Finished loading option data between dates {} and {} into the cache. Took {} ms.", startDate, endDate, ChronoUnit.MILLIS.between(start, end));
    }

    public Optional<HistoricalOption> findOption(String ticker, Double strike, LocalDate expiration, Option.OptionType optionType) {
        OptionCacheKey optionCacheKey = new OptionCacheKey(ticker, expiration, optionType, strike);
        return Optional.ofNullable(optionCache.get(optionCacheKey)).map(x -> {
            HistoricalOption historicalOption = new HistoricalOption();
            historicalOption.setStrike(strike);
            historicalOption.setOptionType(optionType);
            historicalOption.setExpiration(expiration);
            historicalOption.setTicker(ticker);
            historicalOption.setOptionPriceData(x.getRight());
            historicalOption.setId(x.getLeft());
            return historicalOption;
        });
    }

    public void clearDataFromCache(LocalDate startDate, LocalDate endDate) {
        log.info("Started clearing cache data from {} to {}", startDate.toString(), endDate.toString());
        Set<OptionCacheKey> keysToRemove = new HashSet<>();
        optionCache.forEach((x, y) -> {
            y.getRight().removeIf(optionPriceData -> optionPriceData.getTradeDate().isAfter(startDate) && optionPriceData.getTradeDate().isBefore(endDate));
            if (y.getRight().isEmpty()) {
                keysToRemove.add(x);
            }
        });
        keysToRemove.forEach(x -> optionCache.remove(x));
        log.info("Finished clearing cache data from {} to {}", startDate.toString(), endDate.toString());
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class OptionCacheKey {
        private String ticker;
        private LocalDate expiration;
        private Option.OptionType optionType;
        private Double strike;
    }
}
