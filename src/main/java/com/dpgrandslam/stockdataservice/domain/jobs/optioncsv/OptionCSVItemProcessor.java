package com.dpgrandslam.stockdataservice.domain.jobs.optioncsv;

import com.dpgrandslam.stockdataservice.domain.model.OptionCSVFile;
import com.dpgrandslam.stockdataservice.domain.model.options.HistoricalOption;
import com.dpgrandslam.stockdataservice.domain.model.options.Option;
import com.dpgrandslam.stockdataservice.domain.model.options.OptionPriceData;
import com.dpgrandslam.stockdataservice.domain.model.stock.StockMetaData;
import com.dpgrandslam.stockdataservice.domain.model.stock.TrackedStock;
import com.dpgrandslam.stockdataservice.domain.service.HistoricOptionsDataService;
import com.dpgrandslam.stockdataservice.domain.service.StockDataLoadService;
import com.dpgrandslam.stockdataservice.domain.service.TrackedStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class OptionCSVItemProcessor implements ItemProcessor<OptionCSVFile, HistoricalOption> {

    private final HistoricOptionsDataService historicOptionsDataService;
    private final TrackedStockService trackedStockService;
    private final StockDataLoadService stockDataLoadService;

    @Override
    @Transactional
    public HistoricalOption process(OptionCSVFile optionCSVFile) throws Exception {
        TrackedStock trackedStock;
        try {
            trackedStock = trackedStockService.findByTicker(optionCSVFile.getSymbol());
        } catch (EntityNotFoundException e) {
            log.warn("{}. Skipping update...", e.getMessage());
            return null;
        }

        if (trackedStock == null) {
            trackedStock = new TrackedStock();
            StockMetaData metaData = stockDataLoadService.getStockMetaData(optionCSVFile.getSymbol());
            trackedStock.setName(metaData.getName());
            trackedStock.setTicker(metaData.getTicker());
            trackedStock.setActive(true);
            trackedStock.setLastOptionsHistoricDataUpdate(LocalDate.parse(optionCSVFile.getDataDate()));
            trackedStock.setOptionsHistoricDataStartDate(LocalDate.parse(optionCSVFile.getDataDate()));
        }

        HistoricalOption historicalOption = HistoricalOption.builder()
                .optionType(optionCSVFile.getPutCall().equalsIgnoreCase("call") ? Option.OptionType.CALL : Option.OptionType.PUT)
                .strike(Double.parseDouble(optionCSVFile.getStrikePrice()))
                .expiration(LocalDate.parse(optionCSVFile.getExpirationDate()))
                .ticker(optionCSVFile.getSymbol().toUpperCase())
                .build();

        OptionPriceData optionPriceData = OptionPriceData.builder()
                .tradeDate(LocalDate.parse(optionCSVFile.getDataDate()))
                .openInterest(Integer.parseInt(optionCSVFile.getOpenInterest()))
                .bid(Double.parseDouble(optionCSVFile.getBidPrice()))
                .ask(Double.parseDouble(optionCSVFile.getAskPrice()))
                .volume(Integer.parseInt(optionCSVFile.getVolume()))
                .lastTradePrice(Double.parseDouble(optionCSVFile.getLastPrice()))
                .dataObtainedDate(Timestamp.from(Instant.now()))
                .build();
        HistoricalOption existing;
        try {
            existing = historicOptionsDataService.findOption(historicalOption.getTicker(), historicalOption.getExpiration(),
                    historicalOption.getStrike(), historicalOption.getOptionType());
            if (existing.getOptionPriceData().stream().map(OptionPriceData::getTradeDate)
                    .collect(Collectors.toSet()).contains(optionPriceData.getTradeDate())) {
                log.warn("Option Price data for {} already exists. Skipping...", optionPriceData);
                return null;
            }
        } catch (EntityNotFoundException e) {
            existing = null;
            log.debug("Option does not exist, creating new one");
        }
        if (existing != null) {
            existing.getOptionPriceData().add(optionPriceData);
            optionPriceData.setOption(existing);
        } else {
            historicalOption.setOptionPriceData(Collections.singleton(optionPriceData));
            optionPriceData.setOption(historicalOption);
        }
        if (optionPriceData.getTradeDate().isBefore(trackedStock.getOptionsHistoricDataStartDate())) {
            trackedStock.setOptionsHistoricDataStartDate(optionPriceData.getTradeDate());
            trackedStockService.saveTrackedStock(trackedStock);
        }
        return existing != null ? existing : historicalOption;
    }
}
