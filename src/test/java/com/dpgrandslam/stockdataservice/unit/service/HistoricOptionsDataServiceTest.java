package com.dpgrandslam.stockdataservice.unit.service;

import com.dpgrandslam.stockdataservice.adapter.repository.HistoricalOptionRepository;
import com.dpgrandslam.stockdataservice.domain.model.options.HistoricalOption;
import com.dpgrandslam.stockdataservice.domain.model.options.Option;
import com.dpgrandslam.stockdataservice.domain.model.options.OptionPriceData;
import com.dpgrandslam.stockdataservice.domain.service.HistoricOptionsDataService;
import com.dpgrandslam.stockdataservice.testUtils.TestDataFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Stream;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class HistoricOptionsDataServiceTest {

    @Mock
    private HistoricalOptionRepository historicalOptionRepository;

    @InjectMocks
    private HistoricOptionsDataService subject;

    @Captor
    private ArgumentCaptor<HistoricalOption> historicalOptionAC;

    @Test
    public void testAddOption_noOptionExists_addsOption() {

        when(historicalOptionRepository.findDistinctFirstByExpirationAndTickerAndStrikeAndOptionType(any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        subject.addOption(TestDataFactory.HistoricalOptionMother.completeWithOnePriceData().build());

        verify(historicalOptionRepository, times(1)).save(historicalOptionAC.capture());

        HistoricalOption saved = historicalOptionAC.getValue();

        assertEquals("TEST", saved.getTicker());
    }

    @Test
    public void testAddOption_optionExists_addsPriceDataToOption() {
        when(historicalOptionRepository.findDistinctFirstByExpirationAndTickerAndStrikeAndOptionType(any(), anyString(), any(), any()))
                .thenReturn(Optional.of(TestDataFactory.HistoricalOptionMother.noPriceData().build()));
        when(historicalOptionRepository.findById(any())).thenReturn(Optional.of(TestDataFactory.HistoricalOptionMother.noPriceData().build()));

        subject.addOption(TestDataFactory.HistoricalOptionMother.completeWithOnePriceData().build());

        verify(historicalOptionRepository, times(1)).save(historicalOptionAC.capture());
        verify(historicalOptionRepository, times(1)).findById(any());

        HistoricalOption saved = historicalOptionAC.getValue();
        assertEquals(1, saved.getHistoricalPriceData().size());
    }

    @Test
    public void testFindOption_optionExists() {
        LocalDate expiration = LocalDate.now(ZoneId.of("America/New_York"));
        when(historicalOptionRepository.findDistinctFirstByExpirationAndTickerAndStrikeAndOptionType(any(), any(), any(), any()))
                .thenReturn(Optional.of(TestDataFactory.HistoricalOptionMother.completeWithOnePriceData().build()));

        subject.findOption("TEST", LocalDate.now(ZoneId.of("America/New_York")), 12.5, Option.OptionType.CALL);

        verify(historicalOptionRepository, times(1)).findDistinctFirstByExpirationAndTickerAndStrikeAndOptionType(eq(expiration), eq("TEST"), eq(12.5), eq(Option.OptionType.CALL));
    }

    @Test(expected = EntityNotFoundException.class)
    public void testFindOption_optionDoesNotExist_throwsException() {
        when(historicalOptionRepository.findDistinctFirstByExpirationAndTickerAndStrikeAndOptionType(any(), any(), any(), any())).thenReturn(Optional.empty());

        subject.findOption("TEST", LocalDate.now(ZoneId.of("America/New_York")), 12.5, Option.OptionType.PUT);
    }

    @Test
    public void testFindOptions_expirationAndTicker_callsCorrectMethod() {
        LocalDate now = LocalDate.now(ZoneId.of("America/New_York"));
        when(historicalOptionRepository.findByExpirationAndTicker(any(), anyString())).thenReturn(Stream.of(TestDataFactory.HistoricalOptionMother.completeWithOnePriceData().build()));

        subject.findOptions("TEST", now);

        verify(historicalOptionRepository, times(1)).findByExpirationAndTicker(eq(now), eq("TEST"));
    }

    @Test
    public void testFindOptions_byTicker_callsCorrectMethod() {
        LocalDate now = LocalDate.now(ZoneId.of("America/New_York"));
        when(historicalOptionRepository.findByTicker(anyString())).thenReturn(Stream.of(TestDataFactory.HistoricalOptionMother.completeWithOnePriceData().build()));

        subject.findOptions("TEST");

        verify(historicalOptionRepository, times(1)).findByTicker(eq("TEST"));
    }

    @Test
    public void testAddPriceDataToOption_duplicateDate_notAdded() {
        LocalDate now = LocalDate.now();
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        Set<OptionPriceData> priceDataSet = new HashSet<>();
        priceDataSet.add(TestDataFactory.OptionPriceDataMother.complete().tradeDate(now).build());
        priceDataSet.add(TestDataFactory.OptionPriceDataMother.complete().tradeDate(tomorrow).build());

        Set<OptionPriceData> existingPriceDataSet = new HashSet<>();
        existingPriceDataSet.add(TestDataFactory.OptionPriceDataMother.complete().tradeDate(now).build());
        when(historicalOptionRepository.findById(anyLong())).thenReturn(Optional.of(TestDataFactory.HistoricalOptionMother.noPriceData()
        .historicalPriceData(existingPriceDataSet).build()));

        subject.addPriceDataToOption(1234L, priceDataSet);

        verify(historicalOptionRepository, times(1)).findById(eq(1234L));
        verify(historicalOptionRepository, times(1)).save(historicalOptionAC.capture());

        HistoricalOption saved = historicalOptionAC.getValue();

        assertEquals(2, saved.getHistoricalPriceData().size());
        assertTrue(saved.getHistoricalPriceData().stream().anyMatch(data -> data.getTradeDate().equals(tomorrow)));
    }
}
