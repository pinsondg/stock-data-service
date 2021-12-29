package com.dpgrandslam.stockdataservice.domain.model.stock;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrackedStockUpdateRequest {
    private String ticker;
    private Boolean tracked;
}
