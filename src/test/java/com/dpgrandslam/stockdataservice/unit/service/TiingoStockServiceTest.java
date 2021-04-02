package com.dpgrandslam.stockdataservice.unit.service;

import com.dpgrandslam.stockdataservice.adapter.apiclient.tiingo.TiingoApiClient;
import com.dpgrandslam.stockdataservice.domain.model.stock.EndOfDayStockData;
import com.dpgrandslam.stockdataservice.domain.model.stock.LiveStockData;
import com.dpgrandslam.stockdataservice.domain.model.tiingo.TiingoMetaDataResponse;
import com.dpgrandslam.stockdataservice.domain.model.tiingo.TiingoStockEndOfDayResponse;
import com.dpgrandslam.stockdataservice.domain.model.tiingo.TiingoStockLiveDataResponse;
import com.dpgrandslam.stockdataservice.domain.service.TiingoStockService;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static junit.framework.TestCase.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TiingoStockServiceTest {

    @Mock
    private TiingoApiClient apiClient;

    @InjectMocks
    private TiingoStockService subject;

    @Mock
    private TiingoStockEndOfDayResponse mockEndOfDayStockData;

    @Mock
    private TiingoStockLiveDataResponse mockLiveDataResponse;

    @Mock
    private TiingoMetaDataResponse tiingoMetaDataResponse;

    @Test
    public void testGetEndOfDayStockData() {
        when(mockEndOfDayStockData.getVolume()).thenReturn(100);
        when(apiClient.getHistoricalInfo(anyString(), anyString(), anyString())).thenReturn(Collections.singletonList(mockEndOfDayStockData));

        List<EndOfDayStockData> result = subject.getEndOfDayStockData("TEST", LocalDate.now().minusDays(2), LocalDate.now());

        verify(apiClient, times(1)).getHistoricalInfo(eq("TEST"), eq(LocalDate.now().minusDays(2).toString()), eq(LocalDate.now().toString()));

        assertEquals(100, result.get(0).getVolume().intValue());
    }

    @Test
    public void testGetMostRecentEndOfDayStockData() {
        when(apiClient.getEndOfDayInfo(anyString())).thenReturn(Collections.singleton(mockEndOfDayStockData));

        subject.getMostRecentEndOfDayStockData("TEST");

        verify(apiClient, times(1)).getEndOfDayInfo(eq("TEST"));
    }

    @Test
    public void testGetLiveStockData() {
        when(mockLiveDataResponse.getMarketPrice()).thenReturn(12.0);
        when(apiClient.getLiveStockData(anyString())).thenReturn(Collections.singletonList(mockLiveDataResponse));

        LiveStockData actual = subject.getLiveStockData("TEST");

        verify(apiClient, times(1)).getLiveStockData(eq("TEST"));

        assertEquals(mockLiveDataResponse.getMarketPrice(), actual.getMarketPrice());
    }

    @Test
    public void testIsTickerValid_isValid() {
        when(tiingoMetaDataResponse.isValid()).thenReturn(true);
        when(apiClient.getStockMetaData(anyString())).thenReturn(tiingoMetaDataResponse);

        boolean actual = subject.isTickerValid("TEST");

        verify(apiClient, times(1)).getStockMetaData(eq("TEST"));

        assertTrue(actual);
    }

    @Test
    public void testIsTickerValid_notValid() {
        when(tiingoMetaDataResponse.isValid()).thenReturn(false);
        when(apiClient.getStockMetaData(anyString())).thenReturn(tiingoMetaDataResponse);

        boolean actual = subject.isTickerValid("TEST");

        verify(apiClient, times(1)).getStockMetaData(eq("TEST"));

        assertFalse(actual);
    }

    @Test
    public void testIsTickerValid_errorCallingService_returnsFalse() {
        when(apiClient.getStockMetaData(anyString())).thenThrow(FeignException.errorStatus("GET", Response.builder().request(Request.create("GET", "", new HashMap<>(), null, null)).build()));

        boolean actual = subject.isTickerValid("TEST");

        verify(apiClient, times(1)).getStockMetaData(eq("TEST"));
        assertFalse(actual);
    }
}
