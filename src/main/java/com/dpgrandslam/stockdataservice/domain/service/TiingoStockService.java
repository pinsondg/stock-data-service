package com.dpgrandslam.stockdataservice.domain.service;

import com.dpgrandslam.stockdataservice.adapter.apiclient.tiingo.TiingoApiClient;
import com.dpgrandslam.stockdataservice.domain.model.stock.EndOfDayStockData;
import com.dpgrandslam.stockdataservice.domain.model.stock.LiveStockData;
import com.dpgrandslam.stockdataservice.domain.model.stock.StockMetaData;
import com.dpgrandslam.stockdataservice.domain.model.stock.StockSearchResult;
import com.dpgrandslam.stockdataservice.domain.model.tiingo.TiingoMetaDataResponse;
import com.dpgrandslam.stockdataservice.domain.model.tiingo.TiingoStockSearchResponse;
import com.github.benmanes.caffeine.cache.Cache;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class TiingoStockService implements StockDataLoadService {

    @Autowired
    private Cache<String, List<TiingoStockSearchResponse>> stockSearchCache;

    @Autowired
    private TiingoApiClient apiClient;

    @Override
    public boolean isDataLoadThresholdReached() {
        return false;
    }

    @Override
    public List<? extends EndOfDayStockData> getEndOfDayStockData(String ticker, LocalDate startDate, LocalDate endDate) {
        return new ArrayList<>(apiClient.getHistoricalInfo(ticker, startDate.toString(), endDate.toString()));
    }

    @Override
    public List<? extends EndOfDayStockData> getMostRecentEndOfDayStockData(String ticker) {
        return new ArrayList<>(apiClient.getEndOfDayInfo(ticker));
    }

    @Override
    public LiveStockData getLiveStockData(String ticker) {
        return apiClient.getLiveStockData(ticker);
    }

    @Override
    public boolean isTickerValid(String ticker) {
        try {
            TiingoMetaDataResponse metaDataResponse = apiClient.getStockMetaData(ticker);
            return metaDataResponse.isValid();
        } catch (FeignException e) {
            log.warn("Meta data call failed. {}", e.getMessage());
            return false;
        }
    }

    @Override
    public List<? extends StockSearchResult> searchStock(String query) {
        return apiClient.searchStock(query);
    }

    @Override
    public StockMetaData getStockMetaData(String ticker) {
        return apiClient.getStockMetaData(ticker);
    }
}
