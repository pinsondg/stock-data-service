package com.dpgrandslam.stockdataservice.unit.service;

import com.dpgrandslam.stockdataservice.adapter.apiclient.WebpageLoader;
import com.dpgrandslam.stockdataservice.domain.config.ApiClientConfigurationProperties;
import com.dpgrandslam.stockdataservice.domain.error.TreasuryYieldLoadException;
import com.dpgrandslam.stockdataservice.domain.model.stock.YahooFinanceTenYearTreasuryYield;
import com.dpgrandslam.stockdataservice.domain.service.TenYearTreasuryYieldService;
import com.github.benmanes.caffeine.cache.Cache;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TenYearTreasuryYieldServiceTest {

    @Mock
    private Cache<LocalDate, YahooFinanceTenYearTreasuryYield> treasuryYieldCache;

    @Mock
    private WebpageLoader basicWebPageLoader;

    @Mock
    private ApiClientConfigurationProperties clientConfigurationProperties;

    @InjectMocks
    private TenYearTreasuryYieldService subject;

    @Before
    public void setup() {
        when(clientConfigurationProperties.getUrlAndPort()).thenReturn("http://localhost:8080");
        ReflectionTestUtils.setField(subject, "clientConfigurationProperties", clientConfigurationProperties);
    }

    @Test
    public void testGetTreasuryYieldForDate() {
        YahooFinanceTenYearTreasuryYield yield = YahooFinanceTenYearTreasuryYield.builder()
                .date(LocalDate.now())
                .open(1.2)
                .close(1.3)
                .low(1.1)
                .high(1.5)
                .adjClose(1.3)
                .build();
        when(treasuryYieldCache.get(any(), any())).thenReturn(yield);

        YahooFinanceTenYearTreasuryYield actual = subject.getTreasuryYieldForDate(LocalDate.now());

        verify(treasuryYieldCache, times(1)).get(eq(LocalDate.now()), any());

        assertEquals(yield, actual);
    }

    @Test(expected = TreasuryYieldLoadException.class)
    public void testGetTreasuryYieldForDate_throwsException() {
        when(treasuryYieldCache.get(any(), any())).thenReturn(new YahooFinanceTenYearTreasuryYield());

        subject.getTreasuryYieldForDate(LocalDate.now());
    }
}
