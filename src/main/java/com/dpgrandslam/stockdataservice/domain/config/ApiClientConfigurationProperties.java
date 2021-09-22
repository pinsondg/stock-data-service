package com.dpgrandslam.stockdataservice.domain.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiClientConfigurationProperties {

    private String url;
    private String authorizationToken;
    private Integer port;
    private String authType;

    public String getUrlAndPort() {
        String url = this.getUrl();
        if (this.getPort() != null) {
            url += ":" + this.getPort();
        }
        return url;
    }

}
