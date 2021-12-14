package com.dpgrandslam.stockdataservice.integration.repository;


import com.dpgrandslam.stockdataservice.adapter.repository.FearGreedIndexRepository;
import com.dpgrandslam.stockdataservice.domain.model.FearGreedIndex;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

public class FearGreedIndexRepositoryTest extends RepositoryIntTestBase {

    @Autowired
    private FearGreedIndexRepository subject;

    @Test
    public void testInsertAndFindByDate() {
        FearGreedIndex fearGreedIndex1 = new FearGreedIndex();
        fearGreedIndex1.setTradeDate(LocalDate.now());
        fearGreedIndex1.setValue(10);
        FearGreedIndex fearGreedIndex2 = new FearGreedIndex();
        fearGreedIndex2.setValue(50);
        fearGreedIndex2.setTradeDate(LocalDate.now().minusDays(20));

        subject.saveAllAndFlush(Arrays.asList(fearGreedIndex1, fearGreedIndex2));

        FearGreedIndex actual1 = subject.findFearGreedIndexByTradeDate(LocalDate.now());

        assertEquals(10, actual1.getValue().intValue());
        assertEquals(LocalDate.now(), actual1.getTradeDate());

        List<FearGreedIndex> actual2 = subject.findFearGreedIndicesByTradeDateGreaterThanEqual(LocalDate.now().minusDays(10));

        assertEquals(1, actual2.size());
        assertEquals(10, actual2.get(0).getValue().intValue());

        List<FearGreedIndex> actual3 = subject.findFearGreedIndicesByTradeDateGreaterThanEqual(LocalDate.now().minusDays(20));
        assertEquals(2, actual3.size());
    }
}
