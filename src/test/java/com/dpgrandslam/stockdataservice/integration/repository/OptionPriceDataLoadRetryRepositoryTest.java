package com.dpgrandslam.stockdataservice.integration.repository;

import com.dpgrandslam.stockdataservice.adapter.repository.OptionPriceDataLoadRetryRepository;
import com.dpgrandslam.stockdataservice.domain.model.OptionPriceDataLoadRetry;
import com.dpgrandslam.stockdataservice.testUtils.TestDataFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static junit.framework.TestCase.*;

public class OptionPriceDataLoadRetryRepositoryTest extends RepositoryIntTestBase {

    @Autowired
    private OptionPriceDataLoadRetryRepository subject;

    @Test
    public void testAddAndRemoveData() {
        OptionPriceDataLoadRetry retry = TestDataFactory.OptionPriceDataLoadRetryMother.complete().build();

        OptionPriceDataLoadRetry saved = subject.saveAndFlush(retry);

        assertNotNull(saved.getRetryId());


        OptionPriceDataLoadRetry retrieved = subject.findById(retry.getRetryId()).orElseGet(() -> {
            fail("Retry not found.");
            return null;
        });
        assertEquals(retrieved, saved);
        assertEquals(Instant.now().getEpochSecond(), retrieved.getFirstFailure().toInstant().getEpochSecond());
        assertEquals(Instant.now().getEpochSecond(), retrieved.getLastFailure().toInstant().getEpochSecond());
    }

    @Test
    public void testFindAllByOptionTickerAndOptionExpiration() {
        OptionPriceDataLoadRetry retry1 = TestDataFactory.OptionPriceDataLoadRetryMother.complete()
                .tradeDate(LocalDate.now().minusDays(4))
                .build();

        OptionPriceDataLoadRetry retry2 = TestDataFactory.OptionPriceDataLoadRetryMother.complete()
                .tradeDate(LocalDate.now().minusDays(2))
                .build();

        OptionPriceDataLoadRetry retry3 = TestDataFactory.OptionPriceDataLoadRetryMother.complete()
                .tradeDate(LocalDate.now().minusDays(1))
                .optionTicker("TEST2")
                .build();

        OptionPriceDataLoadRetry retry4 = TestDataFactory.OptionPriceDataLoadRetryMother.complete()
                .optionExpiration(LocalDate.now().minusDays(20))
                .build();

        subject.saveAll(Arrays.asList(retry1, retry2, retry3, retry4));

        Set<OptionPriceDataLoadRetry> retrieved = subject.findAllByOptionTickerAndOptionExpiration("TEST", LocalDate.now());
        assertEquals(2, retrieved.size());
        assertTrue(retrieved.stream().anyMatch(x -> x.equals(retry1)));
        assertTrue(retrieved.stream().anyMatch(x -> x.equals(retry2)));
    }

    @Test
    public void testFindAllByOptionTickerAndOptionExpirationAndTradeDate() {
        OptionPriceDataLoadRetry retry1 = TestDataFactory.OptionPriceDataLoadRetryMother.complete()
                .tradeDate(LocalDate.now().minusDays(4))
                .build();

        OptionPriceDataLoadRetry retry2 = TestDataFactory.OptionPriceDataLoadRetryMother.complete()
                .tradeDate(LocalDate.now().minusDays(2))
                .build();

        OptionPriceDataLoadRetry retry3 = TestDataFactory.OptionPriceDataLoadRetryMother.complete()
                .tradeDate(LocalDate.now().minusDays(1))
                .optionTicker("TEST2")
                .build();

        OptionPriceDataLoadRetry retry4 = TestDataFactory.OptionPriceDataLoadRetryMother.complete()
                .optionExpiration(LocalDate.now().minusDays(20))
                .build();

        subject.saveAll(Arrays.asList(retry1, retry2, retry3, retry4));

        OptionPriceDataLoadRetry retrieved = subject.findByOptionTickerAndOptionExpirationAndTradeDate("TEST", LocalDate.now().minusDays(20), LocalDate.now());
        assertEquals(retry4, retrieved);
    }

    @Test
    public void testDeleteAllByTradeDateBefore() {
        OptionPriceDataLoadRetry retry1 = TestDataFactory.OptionPriceDataLoadRetryMother.complete()
                .tradeDate(LocalDate.now().minusDays(10))
                .build();

        OptionPriceDataLoadRetry retry2 = TestDataFactory.OptionPriceDataLoadRetryMother.complete()
                .tradeDate(LocalDate.now().minusDays(5))
                .build();

        OptionPriceDataLoadRetry retry3 = TestDataFactory.OptionPriceDataLoadRetryMother.complete()
                .tradeDate(LocalDate.now().minusDays(1))
                .optionTicker("NODELETE")
                .build();

        //ADD
        subject.saveAll(Arrays.asList(retry1, retry2, retry3));

        //DELETE
        subject.deleteAllByTradeDateBefore(LocalDate.now().minusDays(2));

        //GET
        List<OptionPriceDataLoadRetry> notDeleted = subject.findAll();

        assertEquals(1, notDeleted.size());
        assertEquals("NODELETE", notDeleted.stream().findFirst().get().getOptionTicker());

    }

    @Test
    public void testFindAllByTradeDate() {
        OptionPriceDataLoadRetry retry1 = TestDataFactory.OptionPriceDataLoadRetryMother.complete()
                .tradeDate(LocalDate.now().minusDays(1))
                .build();

        OptionPriceDataLoadRetry retry2 = TestDataFactory.OptionPriceDataLoadRetryMother.complete()
                .tradeDate(LocalDate.now().minusDays(2))
                .build();

        subject.saveAll(Arrays.asList(retry1, retry2));

        Set<OptionPriceDataLoadRetry> found = subject.findAllByTradeDate(LocalDate.now().minusDays(2));

        assertEquals(1, found.size());
        assertEquals(LocalDate.now().minusDays(2), found.stream().findFirst().get().getTradeDate());
    }

    @Test
    public void testAddSameRecords_throwsError() {
        OptionPriceDataLoadRetry retry1 = TestDataFactory.OptionPriceDataLoadRetryMother.complete().build();

        OptionPriceDataLoadRetry retry2 = TestDataFactory.OptionPriceDataLoadRetryMother.complete().build();

        subject.save(retry1);
        subject.save(retry2);
    }
}
