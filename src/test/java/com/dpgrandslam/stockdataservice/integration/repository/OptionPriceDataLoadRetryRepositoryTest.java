package com.dpgrandslam.stockdataservice.integration.repository;

import com.dpgrandslam.stockdataservice.adapter.repository.OptionPriceDataLoadRetryRepository;
import com.dpgrandslam.stockdataservice.domain.model.OptionPriceDataLoadRetry;
import com.dpgrandslam.stockdataservice.testUtils.TestDataFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
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
}
