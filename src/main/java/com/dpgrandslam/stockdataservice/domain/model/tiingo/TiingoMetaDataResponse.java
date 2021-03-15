package com.dpgrandslam.stockdataservice.domain.model.tiingo;

import com.dpgrandslam.stockdataservice.domain.model.stock.StockMetaData;
import lombok.Data;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Data
public class TiingoMetaDataResponse implements StockMetaData {

    private String ticker;
    private String name;
    private String exchangeCode;
    private String startDate;
    private String endDate;
    private String description;
    private String detail;

    public boolean isValid() {
        return !"Not found.".equalsIgnoreCase(detail);
    }

    @Override
    public LocalDate getHistoricDataStartDate() {
        return LocalDate.parse(startDate, DateTimeFormatter.ISO_DATE);
    }

    @Override
    public LocalDate getHistoricDataEndDate() {
        return LocalDate.parse(endDate, DateTimeFormatter.ISO_DATE);
    }
}
