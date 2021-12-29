package com.dpgrandslam.stockdataservice.unit.service;

import com.dpgrandslam.stockdataservice.adapter.apiclient.WebpageLoader;
import com.dpgrandslam.stockdataservice.adapter.repository.FearGreedIndexRepository;
import com.dpgrandslam.stockdataservice.domain.model.FearGreedIndex;
import com.dpgrandslam.stockdataservice.domain.service.CNNFearGreedDataLoadService;
import com.github.benmanes.caffeine.cache.Cache;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CNNFearGreedDataLoadServiceTest {

    @Mock
    private Cache<Pair<LocalDate, LocalDate>, List<FearGreedIndex>> fearGreedBetweenDatesCache;

    @Mock
    private FearGreedIndexRepository fearGreedIndexRepository;

    @Mock
    private WebpageLoader webpageLoader;

    @InjectMocks
    private CNNFearGreedDataLoadService subject;

    @Before
    public void setup() {
        when(fearGreedBetweenDatesCache.get(any(), any())).then(invok -> {
            Function<Pair<LocalDate, LocalDate>, List<FearGreedIndex>> f = invok.getArgument(1);
            return f.apply(invok.getArgument(0));
        });
    }

    @Test
    public void testLoadBetweenDates() {
        FearGreedIndex fearGreedIndex1 = new FearGreedIndex();
        fearGreedIndex1.setTradeDate(LocalDate.now().minusDays(1));
        fearGreedIndex1.setValue(1);
        fearGreedIndex1.setId(1L);
        fearGreedIndex1.setCreateTime(Timestamp.from(Instant.now()));

        when(fearGreedIndexRepository.findFearGreedIndexByTradeDateBetween(any(), any())).thenReturn(Arrays.asList(fearGreedIndex1));

        List<FearGreedIndex> actual = subject.loadFearGreedDataBetweenDates(LocalDate.now().minusDays(10), LocalDate.now());

        verify(fearGreedBetweenDatesCache, times(1)).get(eq(Pair.of(LocalDate.now().minusDays(10), LocalDate.now())), any());
        verify(fearGreedIndexRepository, times(1)).findFearGreedIndexByTradeDateBetween(eq(LocalDate.now().minusDays(10)), eq(LocalDate.now()));

        assertEquals(1, actual.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadBetweenDates_badEndDate() {
        subject.loadFearGreedDataBetweenDates(LocalDate.now(), LocalDate.now().plusDays(10));
    }

    @Test
    public void testSaveFearGreedData() {
        FearGreedIndex fearGreedIndex = new FearGreedIndex();
        fearGreedIndex.setValue(20);
        fearGreedIndex.setId(1L);
        fearGreedIndex.setTradeDate(LocalDate.now());

        subject.saveFearGreedData(fearGreedIndex);

        verify(fearGreedIndexRepository, times(1)).save(eq(fearGreedIndex));
    }

    @Test
    public void testSaveFearGreedData_collection() {
        FearGreedIndex fearGreedIndex = new FearGreedIndex();
        fearGreedIndex.setValue(20);
        fearGreedIndex.setId(1L);
        fearGreedIndex.setTradeDate(LocalDate.now());

        subject.saveFearGreedData(Arrays.asList(fearGreedIndex));

        verify(fearGreedIndexRepository, times(1)).saveAll(eq(Arrays.asList(fearGreedIndex)));
    }

    @Test
    public void testGetFearGreedIndexOfDay() {
        FearGreedIndex fearGreedIndex = new FearGreedIndex();
        fearGreedIndex.setValue(20);
        fearGreedIndex.setId(1L);
        fearGreedIndex.setTradeDate(LocalDate.now());

        when(fearGreedIndexRepository.findFearGreedIndexByTradeDate(any())).thenReturn(Optional.of(fearGreedIndex));

        Optional<FearGreedIndex> o = subject.getFearGreedIndexOfDay(LocalDate.now());

        verify(fearGreedIndexRepository, times(1)).findFearGreedIndexByTradeDate(eq(LocalDate.now()));

        assertTrue(o.isPresent());
        assertEquals(fearGreedIndex, o.get());
    }


}
