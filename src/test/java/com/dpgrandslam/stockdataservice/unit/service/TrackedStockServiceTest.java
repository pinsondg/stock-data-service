package com.dpgrandslam.stockdataservice.unit.service;

import com.dpgrandslam.stockdataservice.adapter.repository.TrackedStocksRepository;
import com.dpgrandslam.stockdataservice.domain.model.stock.StockMetaData;
import com.dpgrandslam.stockdataservice.domain.model.stock.TrackedStock;
import com.dpgrandslam.stockdataservice.domain.service.StockDataLoadService;
import com.dpgrandslam.stockdataservice.domain.service.TrackedStockService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TrackedStockServiceTest {

    private static final String TICKER = "TEST";

    @Mock
    private TrackedStocksRepository trackedStocksRepository;

    @Mock
    private StockDataLoadService stockDataLoadService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private TrackedStockService subject;

    @Mock
    private TrackedStock mockTrackedStock;

    @Captor
    private ArgumentCaptor<TrackedStock> trackedStockAC;

    @Captor
    private ArgumentCaptor<List<TrackedStock>> trackedStockListAC;

    @Before
    public void setup() {
        when(mockTrackedStock.getTicker()).thenReturn(TICKER);
    }

    @Test
    public void testFindByTicker_callsService() {
        when(trackedStocksRepository.findById(anyString())).thenReturn(Optional.of(mockTrackedStock));

        subject.findByTicker(TICKER);

        verify(trackedStocksRepository, times(1)).findById(eq(TICKER));
    }

    @Test
    public void testGetAllTrackedStocks_activeOnly() {
        when(mockTrackedStock.isActive()).thenReturn(true);
        when(trackedStocksRepository.findAllByActiveIsTrue()).thenReturn(Collections.singletonList(mockTrackedStock));

        List<TrackedStock> l = subject.getAllTrackedStocks(true);

        verify(trackedStocksRepository, times(1)).findAllByActiveIsTrue();
        verify(trackedStocksRepository, never()).findAll();
        assertTrue(l.get(0).isActive());
    }

    @Test
    public void testGetAllTrackedStocks() {
        when(trackedStocksRepository.findAll()).thenReturn(Collections.singletonList(mockTrackedStock));

        subject.getAllTrackedStocks(false);

        verify(trackedStocksRepository, times(1)).findAll();
        verify(trackedStocksRepository, never()).findAllByActiveIsTrue();
    }

    @Test
    public void testSetTrackedStockActive() {
        when(trackedStocksRepository.findById(anyString())).thenReturn(Optional.of(mockTrackedStock));
        when(mockTrackedStock.isActive()).thenCallRealMethod();
        doCallRealMethod().when(mockTrackedStock).setActive(anyBoolean());
        when(trackedStocksRepository.save(any())).thenReturn(mockTrackedStock);

        subject.setTrackedStockActive(TICKER, true);

        verify(trackedStocksRepository, times(1)).findById(eq(TICKER));
        verify(trackedStocksRepository, times(1)).save(trackedStockAC.capture());

        TrackedStock saved = trackedStockAC.getValue();

        assertTrue(saved.isActive());
    }


    @Test
    public void testAddTrackedStocks() {
        StockMetaData stockMetaData = mock(StockMetaData.class);

        when(mockTrackedStock.isActive()).thenReturn(true);
        when(stockMetaData.getName()).thenReturn("Test");
        when(stockMetaData.getTicker()).thenReturn(TICKER);
        when(stockMetaData.isValid()).thenReturn(true);
        when(stockDataLoadService.getStockMetaData(anyString())).thenReturn(stockMetaData);

        subject.addTrackedStocks(Collections.singletonList(TICKER));

        verify(stockDataLoadService, times(1)).getStockMetaData(eq(TICKER));
        verify(trackedStocksRepository, times(1)).saveAll(trackedStockListAC.capture());

        List<TrackedStock> expected = Collections.singletonList(mockTrackedStock);
        List<TrackedStock> actual = trackedStockListAC.getValue();
        assertEquals(expected.get(0).getTicker(), actual.get(0).getTicker());
        assertEquals(expected.get(0).isActive(), actual.get(0).isActive());
    }

    @Test
    public void testAddTrackedStock() {
        StockMetaData stockMetaData = mock(StockMetaData.class);

        when(mockTrackedStock.isActive()).thenReturn(true);
        when(stockMetaData.getName()).thenReturn("Test");
        when(stockMetaData.getTicker()).thenReturn(TICKER);
        when(stockMetaData.isValid()).thenReturn(true);
        when(stockDataLoadService.getStockMetaData(anyString())).thenReturn(stockMetaData);

        subject.addTrackedStock(TICKER);

        verify(stockDataLoadService, times(1)).getStockMetaData(eq(TICKER));
        verify(trackedStocksRepository, times(1)).save(trackedStockAC.capture());

        TrackedStock expected = mockTrackedStock;
        TrackedStock actual = trackedStockAC.getValue();
        assertEquals(expected.getTicker(), actual.getTicker());
        assertEquals(expected.isActive(), actual.isActive());
    }
}
