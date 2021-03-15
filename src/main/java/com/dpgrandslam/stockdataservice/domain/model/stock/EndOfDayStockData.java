package com.dpgrandslam.stockdataservice.domain.model.stock;

import java.time.LocalDate;

public interface EndOfDayStockData {

    LocalDate getDate();

    Double getOpen();

    Double getHigh();

    Double getLow();

    Double getClose();

    Integer getVolume();
}
