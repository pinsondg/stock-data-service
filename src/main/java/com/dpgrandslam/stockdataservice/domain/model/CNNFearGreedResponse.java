package com.dpgrandslam.stockdataservice.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CNNFearGreedResponse {

    @JsonProperty("fear_and_greed")
    private FearAndGreed fearAndGreed;

    @Data
    public static class FearAndGreed {

        @JsonProperty("previous_1_month")
        private Double previousOneMonth;

        @JsonProperty("previous_1_week")
        private Double previousOneWeek;

        @JsonProperty("previous_1_year")
        private Double previousOneYear;

        @JsonProperty("previous_close")
        private Double previousClose;

        private String rating;
        private Double score;
        private String timestamp;
    }
}
