package com.dpgrandslam.stockdataservice.domain.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A simple security configuration for the api without having to use spring security
 */
@Configuration
public class APISecurityConfig implements WebMvcConfigurer {

    @Bean
    @ConfigurationProperties(prefix = "api.security")
    public APISecurityConfigurationProperties apiSecurityConfigurationProperties() {
        return new APISecurityConfigurationProperties();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        WebMvcConfigurer.super.addInterceptors(registry);
        registry.addInterceptor(new HandlerInterceptor() {

            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                APISecurityConfigurationProperties securityConfigurationProperties = apiSecurityConfigurationProperties();
                if (securityConfigurationProperties.getEnabled()
                        && (StringUtils.isBlank(securityConfigurationProperties.getPassword())
                        || !securityConfigurationProperties.getPassword().equals(request.getHeader("stock-data-password")))) {
                    response.setStatus(401);
                    return false;
                }
                return HandlerInterceptor.super.preHandle(request, response, handler);
            }
        });
    }
}
