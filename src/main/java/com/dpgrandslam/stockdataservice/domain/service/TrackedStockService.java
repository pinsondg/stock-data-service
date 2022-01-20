package com.dpgrandslam.stockdataservice.domain.service;

import com.dpgrandslam.stockdataservice.adapter.repository.TrackedStocksRepository;
import com.dpgrandslam.stockdataservice.domain.event.TrackedStockAddedEvent;
import com.dpgrandslam.stockdataservice.domain.model.stock.StockMetaData;
import com.dpgrandslam.stockdataservice.domain.model.stock.TrackedStock;
import com.dpgrandslam.stockdataservice.domain.util.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import javax.sound.midi.Track;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TrackedStockService {

    @Autowired
    private TrackedStocksRepository trackedStocksRepository;

    @Autowired
    private StockDataLoadService stockDataLoadService;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private TimeUtils timeUtils;

    public TrackedStock findByTicker(String ticker) {
        return trackedStocksRepository.findById(ticker).orElseThrow(() -> new EntityNotFoundException("Not currently tracking stock with ticker: " + ticker));
    }

    public List<TrackedStock> getAllTrackedStocks(boolean activeOnly) {
        if (activeOnly) {
            return trackedStocksRepository.findAllByActiveIsTrue();
        }
        return trackedStocksRepository.findAll();
    }

    public void setTrackedStockActive(String ticker, boolean isActive) {
        TrackedStock trackedStock = findByTicker(ticker);
        trackedStock.setActive(isActive);
        trackedStocksRepository.save(trackedStock);
    }

    public void updateOptionUpdatedTimestamp(String ticker)  {
        TrackedStock trackedStock = findByTicker(ticker);
        trackedStock.setLastOptionsHistoricDataUpdate(timeUtils.getCurrentOrLastTradeDate());
        trackedStocksRepository.save(trackedStock);
    }

    public void addTrackedStocks(List<String> tickers) {
        log.info("Attempting to track tickers {}", tickers);
        List<TrackedStock> added = trackedStocksRepository.saveAll(tickers.stream()
                .map(ticker -> verifyAndBuildTrackedStock(ticker)
                        .orElseThrow(() -> new IllegalStateException("Ticker: " + ticker + " is not valid. Skipping addition.")))
                .collect(Collectors.toList()));
        applicationEventPublisher.publishEvent(new TrackedStockAddedEvent(this, added));
    }

    public void addTrackedStock(String ticker) {
        log.info("Adding tracked stock: {}", ticker);
        verifyAndBuildTrackedStock(ticker).ifPresent(trackedStock -> {
            TrackedStock saved = trackedStocksRepository.save(trackedStock);
            log.info("Added tracked stock: {}", saved);
            applicationEventPublisher.publishEvent(new TrackedStockAddedEvent(this, Collections.singleton(saved)));
        });
    }

    public TrackedStock saveTrackedStock(TrackedStock trackedStock) {
        log.info("Adding tracked stock with ticker: {}", trackedStock.getTicker());
        return trackedStocksRepository.save(trackedStock);
    }

    private Optional<TrackedStock> verifyAndBuildTrackedStock(String ticker) {
        StockMetaData stockMetaData = stockDataLoadService.getStockMetaData(ticker);
        TrackedStock trackedStock = new TrackedStock();
        if (stockMetaData != null && stockMetaData.isValid()) {
            trackedStock.setName(stockMetaData.getName());
            trackedStock.setTicker(stockMetaData.getTicker());
            trackedStock.setOptionsHistoricDataStartDate(LocalDate.now(ZoneId.of("America/New_York")));
            trackedStock.setActive(true);
        } else {
            trackedStock = null;
        }
        return Optional.ofNullable(trackedStock);
    }
}
