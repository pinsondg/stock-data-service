package com.dpgrandslam.stockdataservice.domain.config;

import lombok.Data;

@Data
public class APISecurityConfigurationProperties {

    private Boolean enabled = false;
    private String password = "";

}
