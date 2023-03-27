package com.dpgrandslam.stockdataservice.domain.model.tiingo;

import com.dpgrandslam.stockdataservice.domain.model.stock.EndOfDayStockData;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Data
@Builder
@EqualsAndHashCode
public class TiingoStockEndOfDayResponse implements EndOfDayStockData {

    private String date;
    private Double open;
    private Double high;
    private Double low;
    private Double close;
    private Long volume;
    private Double adjOpen;
    private Double adjClose;
    private Double adjHigh;
    private Double adjLow;
    private Long adjVolume;
    private Double divCash;
    private Double splitFactor;

    @Override
    public LocalDate getDate() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME;
        return LocalDate.parse(date, dateTimeFormatter);
    }
}
