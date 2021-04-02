package com.dpgrandslam.stockdataservice.domain.model.tiingo;

import com.dpgrandslam.stockdataservice.domain.model.stock.StockSearchResult;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class TiingoStockSearchResponse implements StockSearchResult {

    private String ticker;
    private String name;

    @JsonIgnore
    private String assetType;

    @JsonIgnore
    private boolean isActive;

    @JsonIgnore
    private String permaTicker;

    @JsonIgnore
    private String openFIGI;

}
