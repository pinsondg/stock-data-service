package com.dpgrandslam.stockdataservice.integration.repository;

import com.dpgrandslam.stockdataservice.adapter.repository.HistoricalOptionRepository;
import com.dpgrandslam.stockdataservice.domain.model.options.HistoricalOption;
import com.dpgrandslam.stockdataservice.domain.model.options.Option;
import com.dpgrandslam.stockdataservice.testUtils.TestDataFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

@Slf4j
public class HistoricalOptionRepositoryTest extends RepositoryIntTestBase {

    @Autowired
    protected HistoricalOptionRepository subject;

    @Test
    public void testAddAndRemoveData() {
        HistoricalOption option = subject.save(TestDataFactory.HistoricalOptionMother.completeWithOnePriceData().build());

        assertNotNull(option.getId());

        Slice<HistoricalOption> found = subject.findByTicker("TEST", PageRequest.of(0, 10));
        assertEquals(1, found.getNumberOfElements());
        assertTrue("Historical data should not be empty.", found.stream().anyMatch(item -> item.getHistoricalPriceData() != null && !item.getHistoricalPriceData().isEmpty()));

        Slice<HistoricalOption> nonFound = subject.findByTicker("1234", PageRequest.of(0, 10));
        assertEquals(0, nonFound.getNumberOfElements());
    }

    @Test
    public void testFindByTickerAndExpirationDate() {
        subject.save(TestDataFactory.HistoricalOptionMother.noPriceData().expiration(LocalDate.now()).ticker("AAPL").build());
        subject.save(TestDataFactory.HistoricalOptionMother.completeWithOnePriceData().strike(101.5).ticker("AAPL").build());

        Slice<HistoricalOption> found = subject.findByExpirationAndTicker(LocalDate
                .now(),"AAPL", PageRequest.of(0, 10));

        assertEquals(2, found.getContent().size());

        Slice<HistoricalOption> notFound = subject.findByExpirationAndTicker(LocalDate.now().minusDays(1), "AAPL", PageRequest.of(1, 10));
        assertEquals(0, notFound.getNumberOfElements());

        notFound = subject.findByExpirationAndTicker(LocalDate.now(), "TEST", PageRequest.of(0, 10));
        assertEquals(0, notFound.getNumberOfElements());
    }

    @Test
    public void testFindByTickerAndExpirationDateAndStrikeAndOptionType() {
        subject.save(TestDataFactory.HistoricalOptionMother.completeWithOnePriceData().strike(12.5).build());
        subject.save(TestDataFactory.HistoricalOptionMother.completeWithOnePriceData().strike(13.0).build());

        HistoricalOption found = subject.findDistinctFirstByExpirationAndTickerAndStrikeAndOptionType(LocalDate.now(), "TEST", 12.5, Option.OptionType.CALL).orElseGet(() -> {
            fail("Option not found.");
            return null;
        });
        assertEquals(12.5, found.getStrike(), 0.01);
        assertNotNull(found.getMostRecentPriceData());
        assertEquals(1, found.getHistoricalPriceData().size());
    }

    @Test
    public void testAddMultipleSameOptionsCallAndPut_addsOptions() {
        subject.save(TestDataFactory.HistoricalOptionMother.completeWithOnePriceData().optionType(Option.OptionType.CALL).build());
        subject.saveAndFlush(TestDataFactory.HistoricalOptionMother.completeWithOnePriceData().optionType(Option.OptionType.PUT).build());

        Slice<HistoricalOption> results = subject.findByTicker("TEST", PageRequest.of(0, 10));
        assertEquals(2, results.getContent().size());
    }

    @Test
    public void testAddMultipleSameOptions_throwsException() {
        subject.save(TestDataFactory.HistoricalOptionMother.completeWithOnePriceData().build());
        try {
            subject.saveAndFlush(TestDataFactory.HistoricalOptionMother.completeWithOnePriceData().build());
            fail("Exception not thrown when saving duplicate options.");
        } catch (Exception e) {
            log.info("Exception thrown", e);
            assertTrue(true);
        }
    }

//    @Test
//    public void testFindByExpirationAndTickerWithDataInDateRange() {
//        Set<OptionPriceData> optionPriceData = new HashSet<>();
//        OptionPriceData optionPriceData1 = TestDataFactory.OptionPriceDataMother.complete().dataObtainedDate(Timestamp.from(Instant.now().minus(5, ChronoUnit.DAYS))).build();
//        OptionPriceData optionPriceData2 = TestDataFactory.OptionPriceDataMother.complete().dataObtainedDate(Timestamp.from(Instant.now().minus(10, ChronoUnit.DAYS))).build();
//        OptionPriceData optionPriceData3 = TestDataFactory.OptionPriceDataMother.complete().dataObtainedDate(Timestamp.from(Instant.now().minusSeconds(1000L))).build();
//        optionPriceData.add(optionPriceData1);
//        optionPriceData.add(optionPriceData2);
//        optionPriceData.add(optionPriceData3);
//        HistoricalOption historicalOption = TestDataFactory.HistoricalOptionMother.noPriceData().historicalPriceData(optionPriceData).build();
//        optionPriceData1.setOption(historicalOption);
//        optionPriceData2.setOption(historicalOption);
//        optionPriceData3.setOption(historicalOption);
//        subject.save(historicalOption);
//
//        Stream<HistoricalOption> returned = subject.findByTickerAndExpirationWithDataBetweenDates("TEST",
//                LocalDate.now(ZoneId.of("America/New_York")),
//                Timestamp.from(Instant.now().minus(6, ChronoUnit.DAYS)),
//                Timestamp.from(Instant.now()));
//        Set<OptionPriceData> priceData = returned.findFirst().get().getHistoricalPriceData();
//        assertEquals(2, priceData.size());
//    }
}
