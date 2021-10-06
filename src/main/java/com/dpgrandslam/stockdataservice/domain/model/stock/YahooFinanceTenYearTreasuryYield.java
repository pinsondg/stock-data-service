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
public class YahooFinanceTenYearTreasuryYield implements EndOfDayStockData {

    private LocalDate date;
    private Double open;
    private Double close;
    private Double high;
    private Double low;
    private Double adjClose;


    @Override
    public Double getAdjOpen() {
        return null;
    }

    @Override
    public Integer getVolume() {
        return null;
    }

    @Override
    public Integer getAdjVolume() {
        return null;
    }

    @Override
    public Double getSplitFactor() {
        return null;
    }
}
