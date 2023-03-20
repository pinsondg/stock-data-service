package com.dpgrandslam.stockdataservice.domain.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class CNNFearGreedResponse {

    @SerializedName("fear_and_greed")
    private FearAndGreed fearAndGreed;

    @Data
    public static class FearAndGreed {

        @SerializedName("previous_1_month")
        private Double previousOneMonth;

        @SerializedName("previous_1_week")
        private Double previousOneWeek;

        @SerializedName("previous_1_year")
        private Double previousOneYear;

        @SerializedName("previous_close")
        private Double previousClose;

        private String rating;
        private Double score;
        private String timestamp;
    }
}
