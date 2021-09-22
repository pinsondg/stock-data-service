package com.dpgrandslam.stockdataservice.domain.service;

import com.dpgrandslam.stockdataservice.adapter.apiclient.tiingo.TiingoApiClient;
import com.dpgrandslam.stockdataservice.domain.config.CacheConfiguration;
import com.dpgrandslam.stockdataservice.domain.model.stock.EndOfDayStockData;
import com.dpgrandslam.stockdataservice.domain.model.stock.LiveStockData;
import com.dpgrandslam.stockdataservice.domain.model.stock.StockMetaData;
import com.dpgrandslam.stockdataservice.domain.model.stock.StockSearchResult;
import com.dpgrandslam.stockdataservice.domain.model.tiingo.TiingoStockSearchResponse;
import com.github.benmanes.caffeine.cache.Cache;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TiingoStockService implements StockDataLoadService {

    @Autowired
    private Cache<String, List<TiingoStockSearchResponse>> stockSearchCache;

    @Autowired
    private Cache<CacheConfiguration.HistoricOptionsDataCacheKey, List<EndOfDayStockData>> endOfDayStockDataCache;

    @Autowired
    private TiingoApiClient apiClient;

    @Override
    public boolean isDataLoadThresholdReached() {
        return false;
    }

    @Override
    public List<EndOfDayStockData> getEndOfDayStockData(String ticker, LocalDate startDate, LocalDate endDate) {
        List<EndOfDayStockData> retList;
        CacheConfiguration.HistoricOptionsDataCacheKey cacheKey = new CacheConfiguration.HistoricOptionsDataCacheKey(ticker, startDate, endDate);
        Optional<CacheConfiguration.HistoricOptionsDataCacheKey> withinBounds = endOfDayStockDataCache.asMap().keySet()
                .stream().filter(x -> x.isWithinBounds(cacheKey)).findAny();
        // if we are requesting historical data within the bounds of already cached data, use cached data and filter
        if (withinBounds.isPresent()) {
            List<EndOfDayStockData> fullList = endOfDayStockDataCache.get(withinBounds.get(), (k) -> new LinkedList<>(apiClient.getHistoricalInfo(k.getTicker(),
                    k.getStartDate().toString(), k.getEndDate().toString())));
            retList = fullList.stream()
                    .filter(stock -> (stock.getDate().isEqual(startDate) || startDate.isBefore(stock.getDate()))
                    && (stock.getDate().isEqual(endDate) || endDate.isAfter(stock.getDate()))).collect(Collectors.toList());
        } else {
            retList = endOfDayStockDataCache.get(cacheKey, key -> new LinkedList<>(apiClient.getHistoricalInfo(key.getTicker(),
                    key.getStartDate().toString(), key.getEndDate().toString())));
        }
        return retList;
    }

    @Override
    public List<EndOfDayStockData> getMostRecentEndOfDayStockData(String ticker) {
        return new ArrayList<>(apiClient.getEndOfDayInfo(ticker));
    }

    @Override
    public LiveStockData getLiveStockData(String ticker) {
        return apiClient.getLiveStockData(ticker).get(0);
    }

    @Override
    public boolean isTickerValid(String ticker) {
        try {
            StockMetaData metaDataResponse = getStockMetaData(ticker);
            return metaDataResponse.isValid();
        } catch (FeignException e) {
            log.warn("Meta data call failed. {}", e.getMessage());
            return false;
        }
    }

    @Override
    public List<? extends StockSearchResult> searchStock(String query) {
        return stockSearchCache.get(query, apiClient::searchStock);
    }

    @Override
    public StockMetaData getStockMetaData(String ticker) {
        return apiClient.getStockMetaData(ticker);
    }
}
