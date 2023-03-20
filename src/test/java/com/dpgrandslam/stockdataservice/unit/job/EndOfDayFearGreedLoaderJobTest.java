package com.dpgrandslam.stockdataservice.unit.job;

import com.dpgrandslam.stockdataservice.domain.jobs.EndOfDayFearGreedLoaderJob;
import com.dpgrandslam.stockdataservice.domain.model.FearGreedIndex;
import com.dpgrandslam.stockdataservice.domain.service.FearGreedDataLoadService;
import com.dpgrandslam.stockdataservice.domain.util.TimeUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDate;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EndOfDayFearGreedLoaderJobTest {

    @Mock
    private FearGreedDataLoadService fearGreedDataLoadService;

    @Mock
    private TimeUtils timeUtils;

    @InjectMocks
    private EndOfDayFearGreedLoaderJob subject;

    @Test
    public void testRunJob_nonExist() {
        FearGreedIndex fearGreedIndex1 = new FearGreedIndex();
        fearGreedIndex1.setTradeDate(LocalDate.now());
        fearGreedIndex1.setId(1L);
        fearGreedIndex1.setValue(20);
        FearGreedIndex fearGreedIndex2 = new FearGreedIndex();
        fearGreedIndex2.setTradeDate(LocalDate.now().minusDays(1));
        fearGreedIndex2.setId(2L);
        fearGreedIndex2.setValue(21);

        Set<FearGreedIndex> fearGreedIndexSet = new HashSet<>(Arrays.asList(fearGreedIndex1, fearGreedIndex2));
        when(fearGreedDataLoadService.getFearGreedIndexOfDay(any())).thenReturn(Optional.empty());
        when(timeUtils.getCurrentOrLastTradeDate()).thenReturn(LocalDate.now());
        when(timeUtils.isStockMarketHoliday(eq(LocalDate.now()))).thenReturn(false);
        when(timeUtils.isStockMarketHoliday(eq(LocalDate.now().minusDays(1)))).thenReturn(true);
        when(fearGreedDataLoadService.loadCurrentFearGreedIndex()).thenReturn(fearGreedIndexSet);

        subject.runJob();

        verify(fearGreedDataLoadService, atLeastOnce()).getFearGreedIndexOfDay(eq(LocalDate.now()));
        verify(fearGreedDataLoadService, times(1)).loadCurrentFearGreedIndex();
        verify(fearGreedDataLoadService, times(1)).saveFearGreedData(eq(Collections.singleton(fearGreedIndex1)));
    }

    @Test
    public void testRunJob_stockMarketHoliday_doesNotRun() {

        when(fearGreedDataLoadService.getFearGreedIndexOfDay(any())).thenReturn(Optional.empty());
        when(timeUtils.getCurrentOrLastTradeDate()).thenReturn(LocalDate.now());
        when(timeUtils.isStockMarketHoliday(any(LocalDate.class))).thenReturn(true);

        subject.runJob();

        verify(timeUtils, times(1)).isStockMarketHoliday(eq(LocalDate.now()));
        verify(fearGreedDataLoadService, never()).saveFearGreedData(any(FearGreedIndex.class));
        verify(fearGreedDataLoadService, never()).saveFearGreedData(anyList());
        verify(fearGreedDataLoadService, never()).loadCurrentFearGreedIndex();
    }

}
