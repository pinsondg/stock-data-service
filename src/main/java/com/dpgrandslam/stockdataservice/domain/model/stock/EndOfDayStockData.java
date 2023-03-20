package com.dpgrandslam.stockdataservice.domain.model.stock;

import java.time.LocalDate;

public interface EndOfDayStockData {

    LocalDate getDate();

    Double getOpen();

    Double getAdjOpen();

    Double getHigh();

    Double getAdjHigh();

    Double getLow();

    Double getAdjLow();

    Double getClose();

    Double getAdjClose();

    Integer getVolume();

    Integer getAdjVolume();

    Double getSplitFactor();
}
