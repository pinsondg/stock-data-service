package com.dpgrandslam.stockdataservice.integration.repository;

import com.dpgrandslam.stockdataservice.adapter.repository.TrackedStocksRepository;
import com.dpgrandslam.stockdataservice.domain.model.stock.TrackedStock;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

public class TrackedStockRepositoryTest extends RepositoryIntTestBase {

    @Autowired
    private TrackedStocksRepository subject;

    @Test
    public void testGetAllActiveStocks() {
        TrackedStock active = new TrackedStock();
        active.setActive(true);
        active.setTicker("AAL");
        active.setOptionsHistoricDataStartDate(LocalDate.now());
        active.setName("American Airlines");

        TrackedStock inactive = new TrackedStock();
        inactive.setName("Inactive");
        inactive.setTicker("TEST");
        inactive.setOptionsHistoricDataStartDate(LocalDate.now());

        subject.save(active);
        subject.save(inactive);

        List<TrackedStock> stocks = subject.findAllByActiveIsTrue();
        assertEquals(1, stocks.size());
        assertEquals("AAL", stocks.get(0).getTicker());
    }
}
