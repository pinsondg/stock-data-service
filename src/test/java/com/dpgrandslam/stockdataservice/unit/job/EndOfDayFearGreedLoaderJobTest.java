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
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

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

        Set<FearGreedIndex> fearGreedIndexSet = Collections.singleton(fearGreedIndex1);
        when(fearGreedDataLoadService.getFearGreedIndexOfDay(any())).thenReturn(Optional.empty());
        when(timeUtils.getLastTradeDate()).thenReturn(LocalDate.now());
        when(fearGreedDataLoadService.loadCurrentFearGreedIndex()).thenReturn(fearGreedIndexSet);

        subject.runJob();

        verify(fearGreedDataLoadService, times(1)).getFearGreedIndexOfDay(eq(LocalDate.now()));
        verify(fearGreedDataLoadService, times(1)).loadCurrentFearGreedIndex();
        verify(fearGreedDataLoadService, times(1)).saveFearGreedData(eq(fearGreedIndexSet));
    }

}
