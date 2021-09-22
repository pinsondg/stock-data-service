package com.dpgrandslam.stockdataservice.unit.service;

import com.dpgrandslam.stockdataservice.adapter.apiclient.tiingo.TiingoApiClient;
import com.dpgrandslam.stockdataservice.domain.config.CacheConfiguration;
import com.dpgrandslam.stockdataservice.domain.model.stock.EndOfDayStockData;
import com.dpgrandslam.stockdataservice.domain.model.stock.LiveStockData;
import com.dpgrandslam.stockdataservice.domain.model.tiingo.TiingoMetaDataResponse;
import com.dpgrandslam.stockdataservice.domain.model.tiingo.TiingoStockEndOfDayResponse;
import com.dpgrandslam.stockdataservice.domain.model.tiingo.TiingoStockLiveDataResponse;
import com.dpgrandslam.stockdataservice.domain.service.TiingoStockService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

    private Cache<CacheConfiguration.HistoricOptionsDataCacheKey, List<EndOfDayStockData>> historicCacheSpy;

    @Before
    public void setup() {
        Cache<CacheConfiguration.HistoricOptionsDataCacheKey, List<EndOfDayStockData>> historicCache = Caffeine
                .newBuilder().expireAfterWrite(1, TimeUnit.DAYS)
                .build();
        historicCacheSpy = spy(historicCache);
        ReflectionTestUtils.setField(subject, "endOfDayStockDataCache", historicCacheSpy);
    }

    @Test
    public void testGetEndOfDayStockData() {
        List<TiingoStockEndOfDayResponse> endOfDayStockDataList = new LinkedList<>();
        TiingoStockEndOfDayResponse endOfDayStockData1 = TiingoStockEndOfDayResponse.builder()
                .date(LocalDate.now().minusDays(1).atStartOfDay().toString()).build();
        TiingoStockEndOfDayResponse endOfDayStockData2 = TiingoStockEndOfDayResponse.builder()
                .date(LocalDate.now().minusDays(5).atStartOfDay().toString()).build();
        TiingoStockEndOfDayResponse endOfDayStockData3 = TiingoStockEndOfDayResponse.builder()
                .date(LocalDate.now().minusDays(7).atStartOfDay().toString()).build();
        TiingoStockEndOfDayResponse endOfDayStockData4 = TiingoStockEndOfDayResponse.builder()
                .date(LocalDate.now().minusDays(10).atStartOfDay().toString()).build();
        endOfDayStockDataList.add(endOfDayStockData1);
        endOfDayStockDataList.add(endOfDayStockData2);
        endOfDayStockDataList.add(endOfDayStockData3);
        endOfDayStockDataList.add(endOfDayStockData4);


        when(apiClient.getHistoricalInfo(anyString(), anyString(), anyString())).thenReturn(endOfDayStockDataList);

        List<EndOfDayStockData> actual = subject.getEndOfDayStockData("SPY", LocalDate.now().minusDays(10), LocalDate.now().minusDays(1));
        assertEquals(4, actual.size());

        actual = subject.getEndOfDayStockData("SPY", LocalDate.now().minusDays(7), LocalDate.now().minusDays(5));
        assertEquals(2, actual.size());

        actual = subject.getEndOfDayStockData("SPY", LocalDate.now().minusDays(10), LocalDate.now().minusDays(5));
        assertEquals(3, actual.size());

        actual = subject.getEndOfDayStockData("SPY", LocalDate.now().minusDays(7), LocalDate.now().minusDays(1));
        assertEquals(3, actual.size());

        verify(apiClient, times(1)).getHistoricalInfo(eq("SPY"),
                eq(LocalDate.now().minusDays(10).toString()),
                eq(LocalDate.now().minusDays(1).toString()));
        verify(historicCacheSpy, times(4)).get(any(), any());
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
