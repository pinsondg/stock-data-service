package com.dpgrandslam.stockdataservice.unit.service;

import com.dpgrandslam.stockdataservice.adapter.repository.OptionPriceDataLoadRetryRepository;
import com.dpgrandslam.stockdataservice.domain.model.OptionPriceDataLoadRetry;
import com.dpgrandslam.stockdataservice.domain.service.OptionPriceDataLoadRetryService;
import org.apache.tomcat.jni.Local;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OptionPriceDataLoadRetryServiceTest {

    @Mock
    private OptionPriceDataLoadRetryRepository retryRepository;

    @Mock
    private OptionPriceDataLoadRetry mockRetryRecord;

    @InjectMocks
    private OptionPriceDataLoadRetryService subject;

    @Captor
    private ArgumentCaptor<OptionPriceDataLoadRetry> retryCaptor;

    @Test
    public void testFindById() {
        when(retryRepository.findById(anyLong())).thenReturn(Optional.of(mockRetryRecord));

        subject.findById(1234L);

        verify(retryRepository, times(1)).findById(eq(1234L));
    }

    @Test
    public void testGetAllWithinTradeDate() {
        when(retryRepository.findAllByTradeDate(any(LocalDate.class))).thenReturn(Collections.singleton(mockRetryRecord));

        subject.getAllWithTradeDate(LocalDate.now());

        verify(retryRepository, times(1)).findAllByTradeDate(eq(LocalDate.now()));
    }

    @Test
    public void testGetAll() {
        when(retryRepository.findAll()).thenReturn(Collections.singletonList(mockRetryRecord));

        subject.getAll();

        verify(retryRepository, times(1)).findAll();
    }

    @Test
    public void testGetRetry() {
        when(retryRepository.findByOptionTickerAndOptionExpirationAndTradeDate(any(), any(), any())).thenReturn(mockRetryRecord);

        subject.getRetry("TEST", LocalDate.now(), LocalDate.now().plusDays(1));

        verify(retryRepository, times(1)).findByOptionTickerAndOptionExpirationAndTradeDate(
                eq("TEST"),
                eq(LocalDate.now()),
                eq(LocalDate.now().plusDays(1))
        );
    }

    @Test
    public void testGetRetries() {
        when(retryRepository.findAllByOptionTickerAndOptionExpiration(any(), any())).thenReturn(Collections.singleton(mockRetryRecord));

        subject.getRetries("TEST", LocalDate.now());

        verify(retryRepository, times(1)).findAllByOptionTickerAndOptionExpiration(eq("TEST"), eq(LocalDate.now()));
    }

    @Test
    public void testAddOrUpdateRetry_noExisting() {
        when(retryRepository.save(any(OptionPriceDataLoadRetry.class))).thenReturn(mockRetryRecord);
        when(retryRepository.findByOptionTickerAndOptionExpirationAndTradeDate(any(), any(), any())).thenReturn(null);

        subject.addOrUpdateRetry("TEST", LocalDate.now(), LocalDate.now().plusDays(1));

        verify(retryRepository, times(1)).findByOptionTickerAndOptionExpirationAndTradeDate(eq("TEST"), eq(LocalDate.now()), eq(LocalDate.now().plusDays(1)));
        verify(retryRepository, times(1)).save(retryCaptor.capture());

        OptionPriceDataLoadRetry retry = retryCaptor.getValue();
        assertEquals("TEST", retry.getOptionTicker());
        assertEquals(LocalDate.now(), retry.getOptionExpiration());
        assertEquals(LocalDate.now().plusDays(1), retry.getTradeDate());
        assertEquals(0, retry.getRetryCount());
        assertNull(retry.getLastFailure());
        assertNull(retry.getFirstFailure());
    }

    @Test
    public void testAddOrUpdateRetry_existing() {
        when(retryRepository.save(any(OptionPriceDataLoadRetry.class))).thenReturn(mockRetryRecord);
        when(retryRepository.findByOptionTickerAndOptionExpirationAndTradeDate(any(), any(), any())).thenReturn(mockRetryRecord);
        when(mockRetryRecord.getRetryCount()).thenReturn(1);
        doCallRealMethod().when(mockRetryRecord).setRetryCount(anyInt());

        subject.addOrUpdateRetry("TEST", LocalDate.now(), LocalDate.now().plusDays(1));

        verify(retryRepository, times(1)).findByOptionTickerAndOptionExpirationAndTradeDate(eq("TEST"), eq(LocalDate.now()), eq(LocalDate.now().plusDays(1)));
        verify(retryRepository, times(1)).save(any(OptionPriceDataLoadRetry.class));
        verify(mockRetryRecord, times(1)).setRetryCount(eq(2));
        verify(mockRetryRecord, times(1)).getRetryCount();
    }

    @Test
    public void testUpdateRetryCount() {
        when(mockRetryRecord.getRetryCount()).thenReturn(1);
        doCallRealMethod().when(mockRetryRecord).setRetryCount(anyInt());
        when(retryRepository.save(any(OptionPriceDataLoadRetry.class))).thenReturn(mockRetryRecord);

        subject.updateRetryCount(mockRetryRecord);

        verify(mockRetryRecord, times(1)).getRetryCount();
        verify(mockRetryRecord, times(1)).setRetryCount(eq(2));
        verify(retryRepository, times(1)).save(eq(mockRetryRecord));
    }

    @Test
    public void testRemoveRetry() {
        doNothing().when(retryRepository).deleteById(anyLong());

        subject.removeRetry(1234L);

        verify(retryRepository, times(1)).deleteById(eq(1234L));
    }

    @Test
    public void testRemoveRetry_ticker() {
        doNothing().when(retryRepository).delete(any(OptionPriceDataLoadRetry.class));
        when(retryRepository.findByOptionTickerAndOptionExpirationAndTradeDate(any(), any(), any())).thenReturn(mockRetryRecord);
        when(mockRetryRecord.getOptionTicker()).thenReturn("TEST");

        subject.removeRetry("TEST", LocalDate.now(), LocalDate.now().plusDays(1));

        verify(retryRepository, times(1)).delete(retryCaptor.capture());

        assertEquals("TEST", retryCaptor.getValue().getOptionTicker());
    }

    @Test
    public void testUpdateRetryCount_byCriteria() {
        when(retryRepository.findByOptionTickerAndOptionExpirationAndTradeDate(any(), any(), any())).thenReturn(mockRetryRecord);
        when(mockRetryRecord.getRetryCount()).thenReturn(1);
        doCallRealMethod().when(mockRetryRecord).setRetryCount(anyInt());
        when(retryRepository.save(any(OptionPriceDataLoadRetry.class))).thenReturn(mockRetryRecord);

        subject.updateRetryCount("TEST", LocalDate.now(), LocalDate.now().plusDays(1));

        verify(mockRetryRecord, times(1)).getRetryCount();
        verify(mockRetryRecord, times(1)).setRetryCount(eq(2));
        verify(retryRepository, times(1)).save(eq(mockRetryRecord));
        verify(retryRepository, times(1)).findByOptionTickerAndOptionExpirationAndTradeDate(eq("TEST"), eq(LocalDate.now()), eq(LocalDate.now().plusDays(1)));
    }
}
