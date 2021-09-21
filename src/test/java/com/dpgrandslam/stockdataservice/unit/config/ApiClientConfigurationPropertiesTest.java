package com.dpgrandslam.stockdataservice.unit.config;

import com.dpgrandslam.stockdataservice.domain.config.ApiClientConfigurationProperties;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class ApiClientConfigurationPropertiesTest {

    private static final String TEST_URL = "http://localhost";
    private static final Integer TEST_PORT = 8080;

    @Test
    public void testGetUrlAndPort_noPort() {
        ApiClientConfigurationProperties properties = new ApiClientConfigurationProperties();
        properties.setUrl(TEST_URL);

        assertEquals(TEST_URL, properties.getUrlAndPort());
    }

    @Test
    public void testGetUrlAndPort_withPort() {
        ApiClientConfigurationProperties properties = new ApiClientConfigurationProperties();
        properties.setUrl(TEST_URL);
        properties.setPort(TEST_PORT);

        assertEquals(TEST_URL + ":" + TEST_PORT, properties.getUrlAndPort());
    }
}
