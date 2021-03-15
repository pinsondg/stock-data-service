package com.dpgrandslam.stockdataservice.domain.model.tiingo;

import com.dpgrandslam.stockdataservice.domain.model.stock.StockSearchResult;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class TiingoStockSearchResponse implements StockSearchResult {

    private String ticker;
    private String name;
    private String assetType;
    private boolean isActive;
    private String permaTicker;
    private String openFIGI;

}
