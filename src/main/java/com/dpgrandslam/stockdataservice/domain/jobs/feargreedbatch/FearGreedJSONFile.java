package com.dpgrandslam.stockdataservice.domain.jobs.feargreedbatch;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Set;

@Data
public class FearGreedJSONFile {

    private Set<FearGreedJSONData> data;

    @Data
    public static class FearGreedJSONData {

        @JsonProperty("x")
        private Double timestamp;

        @JsonProperty("y")
        private Double value;

        private String rating;
    }
}
