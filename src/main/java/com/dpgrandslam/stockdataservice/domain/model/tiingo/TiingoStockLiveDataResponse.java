package com.dpgrandslam.stockdataservice.domain.model.tiingo;

import com.dpgrandslam.stockdataservice.domain.model.stock.LiveStockData;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
public class TiingoStockLiveDataResponse implements LiveStockData {

    private String ticker;
    private String timestamp;
    private String quoteTimestamp;
    private String lastSaleTimestamp;
    private Double last;
    private Integer lastSize;
    private Double tngoLast;
    private Double prevClose;
    private Double open;
    private Double high;
    private Double low;
    private Double mid;
    private Integer volume;
    private Double bidSize;
    private Double bidPrice;
    private Double askPrice;
    private Double askSize;

    @Override
    public Double getMarketPrice() {
        return tngoLast;
    }

    @Override
    public Instant getLastSaleTimestamp() {
        return Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(lastSaleTimestamp));
    }

    @Override
    public Instant getTimestamp() {
        return Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(timestamp));
    }
}
