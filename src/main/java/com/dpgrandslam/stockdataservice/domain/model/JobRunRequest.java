package com.dpgrandslam.stockdataservice.domain.model;

import lombok.Data;

import java.util.Map;

@Data
public class JobRunRequest {

    private String jobName;
    private Map<String, String> jobParams;

}
