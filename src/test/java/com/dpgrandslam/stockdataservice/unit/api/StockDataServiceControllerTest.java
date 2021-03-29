package com.dpgrandslam.stockdataservice.unit.api;

import com.dpgrandslam.stockdataservice.adapter.api.StockDataServiceController;
import com.dpgrandslam.stockdataservice.domain.model.options.OptionsChain;
import com.dpgrandslam.stockdataservice.domain.model.stock.TrackedStock;
import com.dpgrandslam.stockdataservice.domain.service.OptionsChainLoadService;
import com.dpgrandslam.stockdataservice.domain.service.TrackedStockService;
import com.dpgrandslam.stockdataservice.testUtils.TestDataFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.ResponseEntity;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.*;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class StockDataServiceControllerTest {

    @Mock
    private OptionsChainLoadService optionsChainLoadService;

    @Mock
    private TrackedStockService trackedStockService;

    @InjectMocks
    private StockDataServiceController subject;

    @Before
    public void setup() {
        TrackedStock active = new TrackedStock();
        active.setActive(true);
        active.setTicker("TEST1");

        TrackedStock inactive = new TrackedStock();
        inactive.setActive(false);
        inactive.setTicker("TEST2");

        when(trackedStockService.getAllTrackedStocks(eq(true))).thenReturn(Collections.singletonList(active));
        when(trackedStockService.getAllTrackedStocks(eq(false))).thenReturn(Arrays.asList(active, inactive));
    }

    @Test
    public void testGetTrackedStocks_activeOnly_returnsOnlyActive() {
        subject.getTrackedStocks(true);

        verify(trackedStockService, times(1)).getAllTrackedStocks(eq(true));

    }

    @Test
    public void testGetTrackedStocks_all_returnsAll() {
        subject.getTrackedStocks(false);

        verify(trackedStockService, times(1)).getAllTrackedStocks(eq(false));
    }

    @Test
    public void testUpdateTrackedStocksActive() {
        Map<String, Boolean> request = new HashMap<>();
        request.put("TEST1", true);
        request.put("TEST2", false);
        request.put("TEST3", true);

        doNothing().doNothing().doThrow(new EntityNotFoundException("Not Found")).when(trackedStockService).setTrackedStockActive(anyString(), anyBoolean());

        ResponseEntity<Map<String, Object>> response = subject.updateTrackedStocksActive(request);

        verify(trackedStockService ,times(1)).setTrackedStockActive(eq("TEST1"), eq(true));
        verify(trackedStockService, times(1)).setTrackedStockActive(eq("TEST2"), eq(false));
        verify(trackedStockService, times(1)).setTrackedStockActive(eq("TEST3"), eq(true));

        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("has_failures", response.getBody().get("status"));
        assertEquals(1, ((Map<String, String>)response.getBody().get("failed")).size());
    }

    @Test
    public void testAddTrackedStocks() {
        List<String> stocks = Arrays.asList("TEST1", "TEST2");

        ResponseEntity response = subject.addTrackedStocks(stocks);

        verify(trackedStockService, times(1)).addTrackedStocks(eq(stocks));
        assertEquals(201, response.getStatusCode().value());
    }

    @Test
    public void testGetOptionsChain_noDates_loadsLive() {
        when(optionsChainLoadService.loadFullLiveOptionsChain(anyString())).thenReturn(Collections.singletonList(
                TestDataFactory.OptionsChainMother.oneOption()));

        ResponseEntity<List<OptionsChain>> response = subject.getOptionsChain("TEST", Optional.empty(), Optional.empty(), Optional.empty());

        verify(optionsChainLoadService, times(1)).loadFullLiveOptionsChain(eq("TEST"));
        verify(optionsChainLoadService, never()).loadLiveOptionsChainForExpirationDate(any(),  any());
        verify(optionsChainLoadService, never()).loadCompleteOptionsChainForExpirationDateWithPriceDataInRange(any(), any(), any(), any());
        assertTrue(response.getStatusCode().is2xxSuccessful());
    }

    @Test
    public void testGetOptionsChain_pastDates_loadsHistoric() {
        LocalDate now = LocalDate.now();
        when(optionsChainLoadService.loadCompleteOptionsChainForExpirationDateWithPriceDataInRange(anyString(), any(), any(), any()))
            .thenReturn(TestDataFactory.OptionsChainMother.oneOption());

        LocalDate expiration = LocalDate.of(2021, 1, 1);
        ResponseEntity response = subject.getOptionsChain("TEST",
                Optional.of(expiration),
                Optional.of(now.minusDays(1)),
                Optional.of(now));

        assertTrue(response.getStatusCode().is2xxSuccessful());

        verify(optionsChainLoadService, times(1))
                .loadCompleteOptionsChainForExpirationDateWithPriceDataInRange(eq("TEST"),
                        eq(expiration), eq(now.minusDays(1)), eq(now));
        verify(optionsChainLoadService, never()).loadFullLiveOptionsChain(any());
        verify(optionsChainLoadService, never()).loadLiveOptionsChainForExpirationDate(any() , any());
    }

    @Test
    public void testGetOptionsChain_endDateOnly_loadsHistoric() {
        LocalDate now = LocalDate.now();
        LocalDate end =  now.minusDays(100);

        when(optionsChainLoadService.loadCompleteOptionsChainForExpirationDateWithPriceDataInRange(anyString(), any(), any(), any()))
                .thenReturn(TestDataFactory.OptionsChainMother.oneOption());

        ResponseEntity response = subject.getOptionsChain("TEST", Optional.of(now), Optional.empty(), Optional.of(end));

        assertTrue(response.getStatusCode().is2xxSuccessful());
        verify(optionsChainLoadService, times(1)).loadCompleteOptionsChainForExpirationDateWithPriceDataInRange(
                eq("TEST"),
                eq(now),
                eq(LocalDate.MIN),
                eq(end)
        );
        verify(optionsChainLoadService, never()).loadFullLiveOptionsChain(any());
        verify(optionsChainLoadService, never()).loadLiveOptionsChainForExpirationDate(any() , any());
    }

    @Test
    public void testGetOptionsChain_expirationDate_loadsLiveForExpiration() {
        LocalDate expiration = LocalDate.now().plusDays(100);

        when(optionsChainLoadService.loadLiveOptionsChainForExpirationDate(anyString(), any())).thenReturn(TestDataFactory.OptionsChainMother.oneOption());

        ResponseEntity response = subject.getOptionsChain("TEST", Optional.of(expiration), Optional.empty(), Optional.empty());

        assertTrue(response.getStatusCode().is2xxSuccessful());

        verify(optionsChainLoadService, times(1)).loadLiveOptionsChainForExpirationDate(eq("TEST"), eq(expiration));
        verify(optionsChainLoadService, never()).loadCompleteOptionsChainForExpirationDateWithPriceDataInRange(any(), any(), any(), any());
        verify(optionsChainLoadService, never()).loadFullLiveOptionsChain(any());
    }

    @Test
    public void testGetOptionsChain_empty_returnsNotFound() {
        when(optionsChainLoadService.loadFullLiveOptionsChain(anyString())).thenReturn(Collections.emptyList());

        ResponseEntity response = subject.getOptionsChain("TEST", Optional.empty(), Optional.empty(), Optional.empty());

        assertTrue(response.getStatusCode().is4xxClientError());
    }
}
