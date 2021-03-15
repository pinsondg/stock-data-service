package com.dpgrandslam.stockdataservice.domain.service;

import com.dpgrandslam.stockdataservice.domain.model.stock.EndOfDayStockData;
import com.dpgrandslam.stockdataservice.domain.model.stock.LiveStockData;
import com.dpgrandslam.stockdataservice.domain.model.stock.StockMetaData;
import com.dpgrandslam.stockdataservice.domain.model.stock.StockSearchResult;

import java.time.LocalDate;
import java.util.List;

public interface StockDataLoadService {

    boolean isDataLoadThresholdReached();

    List<? extends EndOfDayStockData> getEndOfDayStockData(String ticker, LocalDate startDate, LocalDate endDate);

    List<? extends EndOfDayStockData> getMostRecentEndOfDayStockData(String ticker);

    LiveStockData getLiveStockData(String ticker);

    boolean isTickerValid(String ticker);

    List<? extends StockSearchResult> searchStock(String query);

    StockMetaData getStockMetaData(String ticker);
}
