package com.dpgrandslam.stockdataservice.domain.model.stock;

import java.time.Instant;

public interface LiveStockData {

    String getTicker();

    Instant getTimestamp();

    Instant getLastSaleTimestamp();

    Integer getVolume();

    Double getBidSize();

    Double getBidPrice();

    Double getAskSize();

    Double getAskPrice();

    Double getMarketPrice();

    Double getLast();

    Integer getLastSize();

    Double getPrevClose();

    Double getOpen();

    Double getHigh();

    Double getLow();
}
