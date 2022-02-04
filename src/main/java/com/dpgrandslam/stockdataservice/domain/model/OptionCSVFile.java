package com.dpgrandslam.stockdataservice.domain.model;

import lombok.Data;

@Data
public class OptionCSVFile {

    private String optionKey;
    private String symbol;
    private String expirationDate;
    private String askPrice;
    private String askSize;
    private String bidPrice;
    private String bidSize;
    private String lastPrice;
    private String putCall;
    private String strikePrice;
    private String volume;
    private String openInterest;
    private String underlyingPrice;
    private String dataDate;

}
