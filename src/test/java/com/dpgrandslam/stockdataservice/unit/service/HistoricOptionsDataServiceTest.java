package com.dpgrandslam.stockdataservice.unit.service;

import com.dpgrandslam.stockdataservice.adapter.repository.HistoricalOptionRepository;
import com.dpgrandslam.stockdataservice.domain.model.options.HistoricalOption;
import com.dpgrandslam.stockdataservice.domain.model.options.Option;
import com.dpgrandslam.stockdataservice.domain.model.options.OptionPriceData;
import com.dpgrandslam.stockdataservice.domain.service.HistoricOptionsDataService;
import com.dpgrandslam.stockdataservice.testUtils.TestDataFactory;
import com.github.benmanes.caffeine.cache.Cache;
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
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class HistoricOptionsDataServiceTest {

    @Mock
    private HistoricalOptionRepository historicalOptionRepository;

    @Mock
    private Cache<String, Set<HistoricalOption>> historicalOptionCache;

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

        subject.addOption(TestDataFactory.HistoricalOptionMother.completeWithOnePriceData().build());

        verify(historicalOptionRepository, times(1)).save(historicalOptionAC.capture());
        verify(historicalOptionRepository, never()).findById(any());

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
        when(historicalOptionRepository.findByExpirationAndTicker(any(), anyString())).thenReturn(Stream.of(TestDataFactory.HistoricalOptionMother.completeWithOnePriceData().build()).collect(Collectors.toSet()));

        subject.findOptions("TEST", now);

        verify(historicalOptionRepository, times(1)).findByExpirationAndTicker(eq(now), eq("TEST"));
    }

    @Test
    public void testFindOptions_byTicker_callsCorrectMethod() throws ExecutionException {
        LocalDate now = LocalDate.now(ZoneId.of("America/New_York"));
        when(historicalOptionCache.get(anyString(), any())).thenReturn(Stream.of(TestDataFactory.HistoricalOptionMother.completeWithOnePriceData().build()).collect(Collectors.toSet()));

        subject.findOptions("TEST");

        verify(historicalOptionCache, times(1)).get(eq("TEST"), any());
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

    @Test
    public void testFindOptions_betweenDates_returnsCorrect() throws ExecutionException {
        OptionPriceData priceDataEarly = TestDataFactory.OptionPriceDataMother.complete().tradeDate(LocalDate.now().minusDays(20)).build();
        OptionPriceData priceData1 = TestDataFactory.OptionPriceDataMother.complete().tradeDate(LocalDate.now().minusDays(5)).build();
        OptionPriceData priceData2 = TestDataFactory.OptionPriceDataMother.complete().tradeDate(LocalDate.now().minusDays(4)).build();
        OptionPriceData priceData3 = TestDataFactory.OptionPriceDataMother.complete().tradeDate(LocalDate.now().minusDays(2)).build();
        OptionPriceData priceData4 = TestDataFactory.OptionPriceDataMother.complete().tradeDate(LocalDate.now().minusDays(1)).build();

        Set<OptionPriceData> priceDataSet = new HashSet<>();
        priceDataSet.add(priceDataEarly);
        priceDataSet.add(priceData1);
        priceDataSet.add(priceData2);
        priceDataSet.add(priceData3);
        priceDataSet.add(priceData4);

        HistoricalOption historicalOption1 = TestDataFactory.HistoricalOptionMother.noPriceData().strike(13.0).historicalPriceData(priceDataSet).build();
        HistoricalOption historicalOption2 = TestDataFactory.HistoricalOptionMother.noPriceData().strike(12.5).build();

        Set<HistoricalOption> retSet = new HashSet<>();
        retSet.add(historicalOption1);
        retSet.add(historicalOption2);

        when(historicalOptionCache.get(any(), any())).thenReturn(retSet);

        Set<HistoricalOption> historicalOptions = subject.findOptions("TEST", LocalDate.now().minusDays(5), LocalDate.now().minusDays(2));

        HistoricalOption actual = historicalOptions.stream().findFirst().get();

        assertEquals(1, historicalOptions.size());
        assertEquals(3, actual.getHistoricalPriceData().size());
    }
}
