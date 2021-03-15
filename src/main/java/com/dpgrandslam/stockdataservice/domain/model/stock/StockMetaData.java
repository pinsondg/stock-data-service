package com.dpgrandslam.stockdataservice.domain.model.stock;

import java.time.LocalDate;

public interface StockMetaData {

    String getTicker();

    String getDescription();

    String getName();

    LocalDate getHistoricDataStartDate();

    LocalDate getHistoricDataEndDate();

    boolean isValid();
}
