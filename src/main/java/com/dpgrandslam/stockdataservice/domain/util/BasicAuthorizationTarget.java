package com.dpgrandslam.stockdataservice.domain.util;

import com.dpgrandslam.stockdataservice.domain.config.ApiClientConfigurationProperties;
import feign.Request;
import feign.RequestTemplate;
import feign.Target;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class BasicAuthorizationTarget<T> implements Target<T> {

    private ApiClientConfigurationProperties clientConfigurationProperties;
    private Class<T> clazz;

    public BasicAuthorizationTarget(Class<T> clazz,
                                    ApiClientConfigurationProperties clientConfigurationProperties) {
        this.clazz = clazz;
        this.clientConfigurationProperties = clientConfigurationProperties;
        Assert.notNull(clientConfigurationProperties.getUrl(), "Client url is null. Please provide url in configuration properties.");
    }

    @Override
    public Class<T> type() {
        return clazz;
    }

    @Override
    public String name() {
        return getFullUrl();
    }

    @Override
    public String url() {
        return getFullUrl();
    }

    @Override
    public Request apply(RequestTemplate requestTemplate) {
        Map<String, Collection<String>> defaultTargetHeaders = new HashMap<>();
        if (requestTemplate.url().indexOf("http") != 0) {
            requestTemplate.target(url());
        }
        if (clientConfigurationProperties.getAuthorizationToken() != null && clientConfigurationProperties.getAuthType() != null) {
            String authString = clientConfigurationProperties.getAuthType() + " " + clientConfigurationProperties.getAuthorizationToken();
            log.debug("Setting Authorization token to {} for call to {}.", authString, requestTemplate.url());
            defaultTargetHeaders.put("Authorization", Collections.singleton(authString));
        } else {
            log.debug("No authorization specified. Skipping authorization header for call to {}", requestTemplate.url());
        }
        requestTemplate.headers(defaultTargetHeaders);
        return requestTemplate.request();
    }

    private String getFullUrl() {
        String url = clientConfigurationProperties.getUrl();
        if (clientConfigurationProperties.getPort() != null) {
            url += ":" + clientConfigurationProperties.getPort();
        }
        return url;
    }
}
