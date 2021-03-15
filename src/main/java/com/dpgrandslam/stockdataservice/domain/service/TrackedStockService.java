package com.dpgrandslam.stockdataservice.domain.service;

import com.dpgrandslam.stockdataservice.adapter.repository.TrackedStocksRepository;
import com.dpgrandslam.stockdataservice.domain.model.stock.StockMetaData;
import com.dpgrandslam.stockdataservice.domain.model.stock.TrackedStock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDate;
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

    public TrackedStock findByTicker(String ticker) {
        return trackedStocksRepository.findById(ticker).orElseThrow(() -> new EntityNotFoundException("Not currently tracking stock with ticker: " + ticker));
    }

    public List<TrackedStock> getAllTrackedStocks() {
        return trackedStocksRepository.findAll();
    }

    public List<TrackedStock> getAllActiveTrackedStocks() {
        return trackedStocksRepository.findAllByActiveIsTrue();
    }

    public void setTrackedStockActive(String ticker, boolean isActive) {
        TrackedStock trackedStock = findByTicker(ticker);
        trackedStock.setActive(isActive);
        trackedStocksRepository.save(trackedStock);
    }

    public void updateOptionUpdatedTimestamp(String ticker)  {
        TrackedStock trackedStock = findByTicker(ticker);
        trackedStock.setLastOptionsHistoricDataUpdate(LocalDate.now());
        trackedStocksRepository.save(trackedStock);
    }

    public void addTrackedStocks(List<String> tickers) {
        log.info("Attempting to track tickers {}", tickers);
        trackedStocksRepository.saveAll(tickers.stream()
                .map(ticker -> verifyAndBuildTrackedStock(ticker)
                        .orElseThrow(() -> new IllegalStateException("Ticker: " + ticker + " is not valid. Skipping addition.")))
                .collect(Collectors.toList()));
    }

    public void addTrackedStock(String ticker) {
        log.info("Adding tracked stock: {}", ticker);
        verifyAndBuildTrackedStock(ticker).ifPresent(trackedStock -> {
            TrackedStock saved = trackedStocksRepository.save(trackedStock);
            log.info("Added tracked stock: {}", saved);
        });
    }

    public Optional<TrackedStock> verifyAndBuildTrackedStock(String ticker) {
        StockMetaData stockMetaData = stockDataLoadService.getStockMetaData(ticker);
        TrackedStock trackedStock = new TrackedStock();
        if (stockMetaData != null && stockMetaData.isValid()) {
            trackedStock.setName(stockMetaData.getName());
            trackedStock.setTicker(stockMetaData.getTicker());
            trackedStock.setOptionsHistoricDataStartDate(LocalDate.now());
            trackedStock.setActive(true);
        } else {
            trackedStock = null;
        }
        return Optional.ofNullable(trackedStock);
    }
}
