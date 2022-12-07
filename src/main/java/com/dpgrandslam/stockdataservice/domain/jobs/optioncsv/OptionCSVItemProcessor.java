package com.dpgrandslam.stockdataservice.domain.jobs.optioncsv;

import com.dpgrandslam.stockdataservice.domain.model.options.HistoricalOption;
import com.dpgrandslam.stockdataservice.domain.model.options.Option;
import com.dpgrandslam.stockdataservice.domain.model.options.OptionPriceData;
import com.dpgrandslam.stockdataservice.domain.model.stock.TrackedStock;
import com.dpgrandslam.stockdataservice.domain.service.HistoricOptionsDataService;
import com.dpgrandslam.stockdataservice.domain.service.TrackedStockService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class OptionCSVItemProcessor implements ItemProcessor<OptionCSVFile, HistoricalOption> {

    private final HistoricOptionsDataService historicOptionsDataService;
    private final TrackedStockService trackedStockService;

    private Map<String, LocalDate> tracked = new HashMap<>();
    private boolean trackedStocksLoaded = false;
    private final HistoricOptionCache historicOptionCache = new HistoricOptionCache();

    @Override
    @Transactional
    public HistoricalOption process(OptionCSVFile optionCSVFile) throws Exception {
        if (!trackedStocksLoaded && tracked.isEmpty()) {
            tracked = trackedStockService.getAllTrackedStocks(true).stream()
                    .collect(Collectors.toMap(TrackedStock::getTicker, TrackedStock::getOptionsHistoricDataStartDate));
            trackedStocksLoaded = true;
        }
        if (!tracked.containsKey(optionCSVFile.getSymbol())) {
            return null;
        }

        if (!historicOptionCache.containsTicker(optionCSVFile.getSymbol())) {
            historicOptionCache.addOptions(optionCSVFile.getSymbol(), loadOptionsForTicker(optionCSVFile.getSymbol()));
        }

        HistoricalOption newHistoricalOption = HistoricalOption.builder()
                .optionType(optionCSVFile.getPutCall().equalsIgnoreCase("call") ? Option.OptionType.CALL : Option.OptionType.PUT)
                .strike(Double.parseDouble(optionCSVFile.getStrikePrice()))
                .expiration(parseDate(optionCSVFile.getExpirationDate()))
                .ticker(optionCSVFile.getSymbol().toUpperCase())
                .build();

        OptionPriceData optionPriceData = OptionPriceData.builder()
                .tradeDate(parseDate(optionCSVFile.getDataDate()))
                .openInterest(Integer.parseInt(optionCSVFile.getOpenInterest()))
                .bid(Double.parseDouble(optionCSVFile.getBidPrice()))
                .ask(Double.parseDouble(optionCSVFile.getAskPrice()))
                .volume(Integer.parseInt(optionCSVFile.getVolume()))
                .lastTradePrice(Double.parseDouble(optionCSVFile.getLastPrice()))
                .dataObtainedDate(Timestamp.from(Instant.now()))
                .build();

        if (optionPriceData.getTradeDate().isAfter(newHistoricalOption.getExpiration())) {
            log.warn("Trade date for {} is after expiration: {}. Skipping...", optionPriceData, newHistoricalOption.getExpiration());
            return null;
        }
        Optional<HistoricalOption> existing = historicOptionCache.getOption(newHistoricalOption.getTicker(), newHistoricalOption.getStrike(),
                    newHistoricalOption.getExpiration(), newHistoricalOption.getOptionType());
            if (existing.isPresent() && existing.get().getOptionPriceData().stream().map(OptionPriceData::getTradeDate)
                    .collect(Collectors.toSet()).contains(optionPriceData.getTradeDate())) {
                log.warn("Option Price data for option {} on trade date {} already exists. Skipping...", newHistoricalOption, optionPriceData.getTradeDate());
                return null;
            }

        HistoricalOption presistantHistoricalOption = null;
        if (existing.isPresent()) {
            presistantHistoricalOption = historicOptionsDataService.findOption(newHistoricalOption.getTicker(),
                    newHistoricalOption.getExpiration(),
                    newHistoricalOption.getStrike(),
                    newHistoricalOption.getOptionType());
            presistantHistoricalOption.getOptionPriceData().add(optionPriceData);
            optionPriceData.setOption(presistantHistoricalOption);
        } else {
            log.debug("Option (ticker: {}, expiration: {}, strike: {}, type: {}) does not exist, creating new one.",
                    newHistoricalOption.getTicker(), newHistoricalOption.getExpiration(), newHistoricalOption.getStrike(),
                    newHistoricalOption.getOptionType());
            newHistoricalOption.setOptionPriceData(Arrays.asList(optionPriceData));
            optionPriceData.setOption(newHistoricalOption);
        }

        if (optionPriceData.getTradeDate().isBefore(tracked.get(newHistoricalOption.getTicker()))) {
            try {
                TrackedStock trackedStock = trackedStockService.findByTicker(optionCSVFile.getSymbol());
                trackedStock.setOptionsHistoricDataStartDate(optionPriceData.getTradeDate());
                trackedStockService.saveTrackedStock(trackedStock);
                tracked.put(newHistoricalOption.getTicker(), optionPriceData.getTradeDate());
            } catch (EntityNotFoundException e) {
                log.warn("{}. Skipping update...", e.getMessage());
                return null;
            }
        }
        historicOptionCache.updateOption(newHistoricalOption.getTicker(), presistantHistoricalOption != null ? presistantHistoricalOption : newHistoricalOption);
        return presistantHistoricalOption != null ? presistantHistoricalOption : newHistoricalOption;
    }

    private LocalDate parseDate(String dateString) {
        LocalDate date;
        try {
            date = LocalDate.parse(dateString);
        } catch (DateTimeParseException e) {
            date = LocalDate.parse(dateString, DateTimeFormatter.ofPattern("M/d/yyyy"));
        }
        return date;
    }

    @Synchronized
    private Set<HistoricalOption> loadOptionsForTicker(String ticker) {
        return historicOptionsDataService.findOptionsNoCache(ticker);
    }

    @Data
    @AllArgsConstructor
    private static class HistoricOptionCacheKey {

        private Double strike;
        private LocalDate expiration;
        private Option.OptionType optionType;

    }

    private static class HistoricOptionCache {

        private static final int MAX_SIZE = 3;
        private final Queue<String> keys = new LinkedList<>();
        private final Map<String, Map<HistoricOptionCacheKey, HistoricalOption>> cache = Collections.synchronizedMap(new HashMap<>());

        public void addOptions(String ticker, Collection<HistoricalOption> options) {
            if (containsTicker(ticker)) {
                return;
            }
            Map<HistoricOptionCacheKey, HistoricalOption> m = Collections.synchronizedMap(new HashMap<>());
            options.forEach(option -> {
                HistoricOptionCacheKey key = new HistoricOptionCacheKey(option.getStrike(), option.getExpiration(), option.getOptionType());
                m.put(key, option);
            });
            cache.putIfAbsent(ticker, m);
            if (!keys.contains(ticker)) {
                keys.add(ticker);
            }
            if (cache.keySet().size() > MAX_SIZE) {
                cache.remove(keys.poll());
            }
        }

        public boolean containsTicker(String ticker) {
            return cache.containsKey(ticker);
        }

        @Synchronized
        public void updateOption(String ticker, HistoricalOption historicalOption) {
            if (!containsTicker(ticker)) {
                return;
            }
            Map<HistoricOptionCacheKey, HistoricalOption> optionMap = cache.get(ticker);
            HistoricOptionCacheKey key = new HistoricOptionCacheKey(historicalOption.getStrike(), historicalOption.getExpiration(), historicalOption.getOptionType());
            optionMap.put(key, historicalOption);
        }

        public Optional<Map<HistoricOptionCacheKey, HistoricalOption>> getOptions(String ticker) {
            return Optional.ofNullable(cache.get(ticker));
        }

        @Synchronized
        public Optional<HistoricalOption> getOption(String ticker, Double strike, LocalDate expiration, Option.OptionType optionType) {
            HistoricOptionCacheKey cacheKey = new HistoricOptionCacheKey(strike, expiration, optionType);
            if (cache.containsKey(ticker)) {
                return Optional.ofNullable(cache.get(ticker).get(cacheKey));
            }
            return Optional.empty();
        }
    }
}
