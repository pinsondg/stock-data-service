package com.dpgrandslam.stockdataservice.unit.job.feargreedbatch;

import com.dpgrandslam.stockdataservice.domain.jobs.feargreedbatch.FearGreedJSONFile;
import com.dpgrandslam.stockdataservice.domain.jobs.feargreedbatch.FearGreedJSONItemProcessor;
import com.dpgrandslam.stockdataservice.domain.model.FearGreedIndex;
import com.dpgrandslam.stockdataservice.domain.service.FearGreedDataLoadService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FearGreedJSONItemProcessorTest {

    @Mock
    private FearGreedDataLoadService fearGreedDataLoadService;

    @InjectMocks
    private FearGreedJSONItemProcessor subject;

    @Test
    public void testProcess() throws Exception {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = LocalDate.now().minusDays(1);

        FearGreedIndex existing = new FearGreedIndex();
        existing.setValue(50);
        existing.setTradeDate(today);

        FearGreedJSONFile input = new FearGreedJSONFile();
        Set<FearGreedJSONFile.FearGreedJSONData> fearGreedJSONData = new HashSet<>();

        FearGreedJSONFile.FearGreedJSONData data1 = new FearGreedJSONFile.FearGreedJSONData();
        data1.setValue(50.0);
        data1.setTimestamp(Double.parseDouble(Long.valueOf(today.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()).toString()));

        FearGreedJSONFile.FearGreedJSONData data2 = new FearGreedJSONFile.FearGreedJSONData();
        data2.setValue(49.0);
        data2.setTimestamp(Double.parseDouble(Long.valueOf(yesterday.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()).toString()));

        fearGreedJSONData.add(data1);
        fearGreedJSONData.add(data2);
        input.setData(fearGreedJSONData);

        when(fearGreedDataLoadService.getFearGreedIndexOfDay(eq(today))).thenReturn(Optional.of(existing));
        when(fearGreedDataLoadService.getFearGreedIndexOfDay(eq(yesterday))).thenReturn(Optional.empty());

        Set<FearGreedIndex> actual = subject.process(input);

        assertNotNull(actual);
        assertEquals(1, actual.size());
        assertEquals(49, actual.stream().findFirst().get().getValue().longValue());
        assertEquals(yesterday, actual.stream().findFirst().get().getTradeDate());

        verify(fearGreedDataLoadService, times(2)).getFearGreedIndexOfDay(any());
    }
}
