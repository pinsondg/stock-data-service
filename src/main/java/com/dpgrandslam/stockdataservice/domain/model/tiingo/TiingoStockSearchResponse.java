package com.dpgrandslam.stockdataservice.domain.model.tiingo;

import com.dpgrandslam.stockdataservice.domain.model.stock.StockSearchResult;
import com.fasterxml.jackson.annotation.JsonIgnore;
import liquibase.pro.packaged.E;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
public class TiingoStockSearchResponse implements StockSearchResult {

    private String ticker;
    private String name;

    @JsonIgnore
    @EqualsAndHashCode.Exclude
    private String assetType;

    @JsonIgnore
    @EqualsAndHashCode.Exclude
    private boolean isActive;

    @JsonIgnore
    @EqualsAndHashCode.Exclude
    private String permaTicker;

    @JsonIgnore
    @EqualsAndHashCode.Exclude
    private String openFIGI;

}
