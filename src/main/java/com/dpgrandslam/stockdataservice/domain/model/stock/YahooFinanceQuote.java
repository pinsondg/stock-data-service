package com.dpgrandslam.stockdataservice.domain.model.stock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YahooFinanceQuote implements EndOfDayStockData {

    private LocalDate date;
    private Double open;
    private Double close;
    private Double high;
    private Double low;
    private Double adjClose;
    private String ticker;


    @Override
    public Double getAdjOpen() {
        return null;
    }

    @Override
    public Long getVolume() {
        return null;
    }

    @Override
    public Long getAdjVolume() {
        return null;
    }

    @Override
    public Double getAdjHigh() {
        return null;
    }

    @Override
    public Double getAdjLow() {
        return null;
    }

    @Override
    public Double getSplitFactor() {
        return null;
    }
}
