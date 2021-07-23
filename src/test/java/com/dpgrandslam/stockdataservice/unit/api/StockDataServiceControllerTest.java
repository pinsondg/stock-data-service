package com.dpgrandslam.stockdataservice.unit.api;

import com.dpgrandslam.stockdataservice.adapter.api.StockDataServiceController;
import com.dpgrandslam.stockdataservice.domain.dto.PageableResult;
import com.dpgrandslam.stockdataservice.domain.error.OptionsChainLoadException;
import com.dpgrandslam.stockdataservice.domain.model.options.Option;
import com.dpgrandslam.stockdataservice.domain.model.options.OptionsChain;
import com.dpgrandslam.stockdataservice.domain.model.stock.EndOfDayStockData;
import com.dpgrandslam.stockdataservice.domain.model.stock.LiveStockData;
import com.dpgrandslam.stockdataservice.domain.model.stock.StockSearchResult;
import com.dpgrandslam.stockdataservice.domain.model.stock.TrackedStock;
import com.dpgrandslam.stockdataservice.domain.model.tiingo.TiingoStockSearchResponse;
import com.dpgrandslam.stockdataservice.domain.service.OptionsChainLoadService;
import com.dpgrandslam.stockdataservice.domain.service.StockDataLoadService;
import com.dpgrandslam.stockdataservice.domain.service.TrackedStockService;
import com.dpgrandslam.stockdataservice.testUtils.TestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.ResponseEntity;

import javax.persistence.EntityNotFoundException;
import java.awt.print.Pageable;
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

    @Mock
    private StockDataLoadService stockDataLoadService;

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
    public void testGetOptionsChain_noDates_loadsLive() throws OptionsChainLoadException {
        when(optionsChainLoadService.loadFullLiveOptionsChain(anyString())).thenReturn(Collections.singletonList(
                TestDataFactory.OptionsChainMother.oneOption()));

        ResponseEntity<PageableResult<OptionsChain>> response = subject.getOptionsChain("TEST", Optional.empty(), Optional.empty(), Optional.empty(), 1, 100);

        verify(optionsChainLoadService, times(1)).loadFullLiveOptionsChain(eq("TEST"));
        verify(optionsChainLoadService, never()).loadLiveOptionsChainForExpirationDate(any(),  any());
        verify(optionsChainLoadService, never()).loadCompleteOptionsChainForExpirationDateWithPriceDataInRange(any(), any(), any(), any(), any(), any());
        assertTrue(response.getStatusCode().is2xxSuccessful());
    }

    @Test
    public void testGetOptionsChain_pastDates_loadsHistoric() throws OptionsChainLoadException {
        LocalDate now = LocalDate.now();
        when(optionsChainLoadService.loadCompleteOptionsChainForExpirationDateWithPriceDataInRange(anyString(), any(), any(), any(), anyInt(), anyInt()))
            .thenReturn(TestDataFactory.OptionsChainMother.oneOption());

        LocalDate expiration = LocalDate.of(2021, 1, 1);
        ResponseEntity response = subject.getOptionsChain("TEST",
                Optional.of(expiration.toString()),
                Optional.of(now.minusDays(1).toString()),
                Optional.of(now.toString()),
                1,
                100);

        assertTrue(response.getStatusCode().is2xxSuccessful());

        verify(optionsChainLoadService, times(1))
                .loadCompleteOptionsChainForExpirationDateWithPriceDataInRange(eq("TEST"),
                        eq(expiration), eq(now.minusDays(1)), eq(now), eq(1), eq(100));
        verify(optionsChainLoadService, never()).loadFullLiveOptionsChain(any());
        verify(optionsChainLoadService, never()).loadLiveOptionsChainForExpirationDate(any() , any());
    }

    @Test
    public void testGetOptionsChain_endDateOnly_loadsHistoric() throws OptionsChainLoadException {
        LocalDate now = LocalDate.now();
        LocalDate end =  now.minusDays(100);

        when(optionsChainLoadService.loadCompleteOptionsChainForExpirationDateWithPriceDataInRange(anyString(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(TestDataFactory.OptionsChainMother.oneOption());

        ResponseEntity response = subject.getOptionsChain("TEST", Optional.of(now.toString()), Optional.empty(), Optional.of(end.toString()), 1, 100);

        assertTrue(response.getStatusCode().is2xxSuccessful());
        verify(optionsChainLoadService, times(1)).loadCompleteOptionsChainForExpirationDateWithPriceDataInRange(
                eq("TEST"),
                eq(now),
                eq(LocalDate.MIN),
                eq(end),
                eq(1),
                eq(100)
        );
        verify(optionsChainLoadService, never()).loadFullLiveOptionsChain(any());
        verify(optionsChainLoadService, never()).loadLiveOptionsChainForExpirationDate(any() , any());
    }

    @Test
    public void testGetOptionsChain_expirationDate_loadsLiveForExpiration() throws OptionsChainLoadException {
        LocalDate expiration = LocalDate.now().plusDays(100);

        when(optionsChainLoadService.loadLiveOptionsChainForExpirationDate(anyString(), any())).thenReturn(TestDataFactory.OptionsChainMother.oneOption());

        ResponseEntity response = subject.getOptionsChain("TEST", Optional.of(expiration.toString()), Optional.empty(), Optional.empty(), 1, 100);

        assertTrue(response.getStatusCode().is2xxSuccessful());

        verify(optionsChainLoadService, times(1)).loadLiveOptionsChainForExpirationDate(eq("TEST"), eq(expiration));
        verify(optionsChainLoadService, never()).loadCompleteOptionsChainForExpirationDateWithPriceDataInRange(any(), any(), any(), any(), any(), any());
        verify(optionsChainLoadService, never()).loadFullLiveOptionsChain(any());
    }

    @Test
    public void testGetOptionsChain_empty_returnsNothing() throws OptionsChainLoadException {
        when(optionsChainLoadService.loadFullLiveOptionsChain(anyString())).thenReturn(Collections.emptyList());

        ResponseEntity<PageableResult<OptionsChain>> response = subject.getOptionsChain("TEST", Optional.empty(),
                Optional.empty(), Optional.empty(), 1, 100);

        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertTrue(response.getBody().getData().isEmpty());
    }

    @Test
    public void testGetLiveStockData_callsService() {
        when(stockDataLoadService.getLiveStockData(anyString())).thenReturn(mock(LiveStockData.class));

        subject.getLiveStockData("TEST");

        verify(stockDataLoadService, times(1)).getLiveStockData(eq("TEST"));
    }

    @Test
    public void testSearchStock_callsService() {
        String testTicker = "TEST";
        TiingoStockSearchResponse mockResult = mock(TiingoStockSearchResponse.class);
        List<StockSearchResult> l = new ArrayList<>();
        l.add(mockResult);

        when(mockResult.getTicker()).thenReturn(testTicker);
        doReturn(l).when(stockDataLoadService).searchStock(anyString());

        ResponseEntity<List<? extends StockSearchResult>> response = subject.searchStock(testTicker);

        verify(stockDataLoadService, times(1)).searchStock(eq(testTicker));
        assertEquals(1, Objects.requireNonNull(response.getBody()).size());
        assertEquals(testTicker, response.getBody().get(0).getTicker());
    }

    @Test
    public void testGetEndOfDayData_noDates_getsMostRecentData() {
        EndOfDayStockData endOfDayStockData = mock(EndOfDayStockData.class);

        when(stockDataLoadService.getMostRecentEndOfDayStockData(anyString())).thenReturn(Collections.singletonList(endOfDayStockData));

        subject.getEndOfDayStockData("TEST", Optional.empty(), Optional.empty());

        verify(stockDataLoadService, times(1)).getMostRecentEndOfDayStockData(eq("TEST"));
        verify(stockDataLoadService, never()).getEndOfDayStockData(any(), any(), any());
    }

    @Test
    public void testGetEndOfDayData_withDates_getsHistoricData() {
        EndOfDayStockData endOfDayStockData = mock(EndOfDayStockData.class);

        subject.getEndOfDayStockData("TEST", Optional.of(LocalDate.now().toString()), Optional.of(LocalDate.now().toString()));

        verify(stockDataLoadService, times(0)).getMostRecentEndOfDayStockData(any());
        verify(stockDataLoadService, times(1)).getEndOfDayStockData(eq("TEST"), eq(LocalDate.now()), eq(LocalDate.now()));
    }

    @Test
    public void testGetEndOfDayData_withOnlyEndDate_getsHistoricDataWithMinStart() {
        EndOfDayStockData endOfDayStockData = mock(EndOfDayStockData.class);

        subject.getEndOfDayStockData("TEST", Optional.empty(), Optional.of(LocalDate.now().toString()));

        verify(stockDataLoadService, times(0)).getMostRecentEndOfDayStockData(any());
        verify(stockDataLoadService, times(1)).getEndOfDayStockData(eq("TEST"), eq(LocalDate.MIN), eq(LocalDate.now()));
    }

    @Test
    public void testGetEndOfDayData_withOnlyStartDate_getsHistoricDataWithTodayEnd() {
        EndOfDayStockData endOfDayStockData = mock(EndOfDayStockData.class);

        subject.getEndOfDayStockData("TEST", Optional.of(LocalDate.now().minusDays(1).toString()), Optional.empty());

        verify(stockDataLoadService, times(0)).getMostRecentEndOfDayStockData(any());
        verify(stockDataLoadService, times(1)).getEndOfDayStockData(eq("TEST"), eq(LocalDate.now().minusDays(1)), eq(LocalDate.now()));
    }

    @Test
    public void testLoadFullOptionsChain() throws OptionsChainLoadException {
        subject.getFullOptionsChain("TEST", 1, 100);

        verify(optionsChainLoadService, times(1)).loadFullOptionsChainWithAllData(eq("TEST"), eq(1), eq(100));
    }


}
