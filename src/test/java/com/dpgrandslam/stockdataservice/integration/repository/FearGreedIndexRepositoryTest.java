package com.dpgrandslam.stockdataservice.integration.repository;


import com.dpgrandslam.stockdataservice.adapter.repository.FearGreedIndexRepository;
import com.dpgrandslam.stockdataservice.domain.model.FearGreedIndex;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.*;

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

        List<FearGreedIndex> fearGreedIndices = subject.saveAllAndFlush(Arrays.asList(fearGreedIndex1, fearGreedIndex2));
        fearGreedIndex1 = fearGreedIndices.get(0);
        fearGreedIndex2 = fearGreedIndices.get(1);

        FearGreedIndex actual1 = subject.findFearGreedIndexByTradeDate(LocalDate.now()).get();

        assertEquals(fearGreedIndex1, actual1);
        assertEquals(LocalDate.now(), actual1.getTradeDate());

        List<FearGreedIndex> actual2 = subject.findFearGreedIndicesByTradeDateGreaterThanEqual(LocalDate.now().minusDays(10));

        assertEquals(1, actual2.size());
        assertEquals(10, actual2.get(0).getValue().intValue());

        List<FearGreedIndex> actual3 = subject.findFearGreedIndicesByTradeDateGreaterThanEqual(LocalDate.now().minusDays(20));
        assertEquals(2, actual3.size());

        assertFalse(subject.findFearGreedIndexByTradeDate(LocalDate.now().minusDays(5)).isPresent());
    }

    @Test
    public void testFindBetweenDates() {
        FearGreedIndex fearGreedIndex1 = new FearGreedIndex();
        fearGreedIndex1.setTradeDate(LocalDate.now().minusDays(2));
        fearGreedIndex1.setValue(20);

        FearGreedIndex fearGreedIndex2 = new FearGreedIndex();
        fearGreedIndex2.setValue(30);
        fearGreedIndex2.setTradeDate(LocalDate.now().minusDays(10));

        subject.saveAllAndFlush(Arrays.asList(fearGreedIndex1, fearGreedIndex2));

        assertEquals(2, subject.findFearGreedIndexByTradeDateBetween(LocalDate.now().minusDays(20), LocalDate.now()).size());
        assertEquals(1, subject.findFearGreedIndexByTradeDateBetween(LocalDate.now().minusDays(5), LocalDate.now()).size());
        assertTrue(subject.findFearGreedIndexByTradeDateBetween(LocalDate.now().minusDays(5),
                LocalDate.now()).stream()
                .anyMatch(x -> x.getTradeDate().equals(LocalDate.now().minusDays(2)) && x.getValue() == 20));
        assertEquals(0, subject.findFearGreedIndexByTradeDateBetween(LocalDate.now().minusDays(5), LocalDate.now().minusDays(3)).size());
        assertEquals(1, subject.findFearGreedIndexByTradeDateBetween(LocalDate.now().minusDays(10), LocalDate.now().minusDays(10)).size());
        assertEquals(2, subject.findFearGreedIndexByTradeDateBetween(LocalDate.now().minusDays(10), LocalDate.now().minusDays(2)).size());
    }

    @Test
    public void testEquals() {
        FearGreedIndex fearGreedIndex1 = new FearGreedIndex();
        fearGreedIndex1.setId(123L);
        fearGreedIndex1.setValue(10);

        FearGreedIndex fearGreedIndex2 = new FearGreedIndex();
        fearGreedIndex2.setId(123L);
        fearGreedIndex2.setValue(10);

        assertEquals(fearGreedIndex1, fearGreedIndex2);
    }
}
