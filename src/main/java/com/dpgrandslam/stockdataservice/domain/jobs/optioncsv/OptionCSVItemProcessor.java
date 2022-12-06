package com.dpgrandslam.stockdataservice.domain.jobs.optioncsv;

import com.dpgrandslam.stockdataservice.domain.model.options.HistoricalOption;
import com.dpgrandslam.stockdataservice.domain.model.options.Option;
import com.dpgrandslam.stockdataservice.domain.model.options.OptionPriceData;
import com.dpgrandslam.stockdataservice.domain.model.stock.TrackedStock;
import com.dpgrandslam.stockdataservice.domain.service.HistoricOptionsDataService;
import com.dpgrandslam.stockdataservice.domain.service.TrackedStockService;
import com.dpgrandslam.stockdataservice.domain.util.HistoricOptionBetweenDateCache;
import lombok.*;
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

    private final TrackedStockService trackedStockService;
    private final HistoricOptionBetweenDateCache historicOptionBetweenDateCache;

    private Map<String, LocalDate> tracked = new HashMap<>();
    private boolean trackedStocksLoaded = false;
    private LocalDate cacheStartDate;
    private LocalDate cacheEndDate;

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
        LocalDate dataDate = parseDate(optionCSVFile.getDataDate());

        handleCache(dataDate);

        HistoricalOption historicalOption = HistoricalOption.builder()
                .optionType(optionCSVFile.getPutCall().equalsIgnoreCase("call") ? Option.OptionType.CALL : Option.OptionType.PUT)
                .strike(Double.parseDouble(optionCSVFile.getStrikePrice()))
                .expiration(parseDate(optionCSVFile.getExpirationDate()))
                .ticker(optionCSVFile.getSymbol().toUpperCase())
                .build();

        OptionPriceData optionPriceData = OptionPriceData.builder()
                .tradeDate(dataDate)
                .openInterest(Integer.parseInt(optionCSVFile.getOpenInterest()))
                .bid(Double.parseDouble(optionCSVFile.getBidPrice()))
                .ask(Double.parseDouble(optionCSVFile.getAskPrice()))
                .volume(Integer.parseInt(optionCSVFile.getVolume()))
                .lastTradePrice(Double.parseDouble(optionCSVFile.getLastPrice()))
                .dataObtainedDate(Timestamp.from(Instant.now()))
                .build();

        if (optionPriceData.getTradeDate().isAfter(historicalOption.getExpiration())) {
            log.warn("Trade date for {} is after expiration: {}. Skipping...", optionPriceData, historicalOption.getExpiration());
            return null;
        }

        Optional<HistoricalOption> existing = historicOptionBetweenDateCache.findOption(historicalOption.getTicker(), historicalOption.getStrike(),
                historicalOption.getExpiration(), historicalOption.getOptionType());
        if (existing.isPresent() && existing.get().getOptionPriceData().stream()
                .map(OptionPriceData::getTradeDate)
                .anyMatch(x -> x.equals(optionPriceData.getTradeDate()))) {
            log.info("Option Price data for {} already exists. Skipping...", optionPriceData);
            return null;
        }

        existing.ifPresentOrElse((x) -> {
            x.getOptionPriceData().add(optionPriceData);
            optionPriceData.setOption(x);
        }, () -> {
            historicalOption.setOptionPriceData(Collections.singleton(optionPriceData));
            optionPriceData.setOption(historicalOption);
        });

        if (optionPriceData.getTradeDate().isBefore(tracked.get(historicalOption.getTicker()))) {
            try {
                TrackedStock trackedStock = trackedStockService.findByTicker(optionCSVFile.getSymbol());
                trackedStock.setOptionsHistoricDataStartDate(optionPriceData.getTradeDate());
                trackedStockService.saveTrackedStock(trackedStock);
                tracked.put(historicalOption.getTicker(), optionPriceData.getTradeDate());
            } catch (EntityNotFoundException e) {
                log.warn("Exception encountered. Skipping update...", e);
                return null;
            }
        }
        return existing.orElse(historicalOption);
    }

    private void handleCache(LocalDate optionTradeDate) {
        if (cacheStartDate == null) {
            cacheStartDate = optionTradeDate.minusDays(10);
            cacheEndDate = optionTradeDate.plusMonths(1);
            historicOptionBetweenDateCache.addDataToCache(cacheStartDate, cacheEndDate);
        }
        if (optionTradeDate.isAfter(cacheEndDate)) {
            LocalDate prevCacheStart = cacheStartDate;
            LocalDate prevCacheEnd = cacheEndDate;
            cacheStartDate = optionTradeDate.minusDays(10);
            cacheEndDate = optionTradeDate.plusMonths(1);
            historicOptionBetweenDateCache.clearDataFromCache(prevCacheStart, prevCacheEnd.minusDays(5));
            historicOptionBetweenDateCache.addDataToCache(cacheStartDate, cacheEndDate);
        }
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

}
