package com.dpgrandslam.stockdataservice.domain.model.stock;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrackedStockUpdateResponse {
    private String status;
    private List<FailedUpdate> failedUpdates;

    @Data

    @AllArgsConstructor
    @NoArgsConstructor
    public static class FailedUpdate {
        private String ticker;
        private String message;
    }

}
