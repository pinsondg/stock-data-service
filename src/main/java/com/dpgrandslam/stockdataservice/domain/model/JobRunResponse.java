package com.dpgrandslam.stockdataservice.domain.model;

import lombok.Data;

@Data
public class JobRunResponse {

    private String jobStatus;
    private Long jobId;
    private Long jobExecutionId;
    private String message;
}
