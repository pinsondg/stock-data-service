package com.dpgrandslam.stockdataservice.domain.model.tiingo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TiingoFundamentalsResponse {

    private String date;
    private Double marketCap;
    private Double enterpriseVal;
    private Double peRatio;
    private Double pbRatio;
    private Double trailingPEG1Y;

}
