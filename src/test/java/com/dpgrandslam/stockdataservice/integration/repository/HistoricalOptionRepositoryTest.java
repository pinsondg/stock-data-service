package com.dpgrandslam.stockdataservice.integration.repository;

import com.dpgrandslam.stockdataservice.adapter.repository.HistoricalOptionJDBCRepository;
import com.dpgrandslam.stockdataservice.adapter.repository.HistoricalOptionRepository;
import com.dpgrandslam.stockdataservice.domain.model.options.HistoricalOption;
import com.dpgrandslam.stockdataservice.domain.model.options.Option;
import com.dpgrandslam.stockdataservice.domain.model.options.OptionPriceData;
import com.dpgrandslam.stockdataservice.testUtils.TestDataFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

@Slf4j
public class HistoricalOptionRepositoryTest extends RepositoryIntTestBase {

    @Autowired
    protected HistoricalOptionRepository subject;

    @Autowired
    protected HistoricalOptionJDBCRepository jdbcRepository;

    @Test
    public void testAddAndRemoveData() {
        HistoricalOption option = subject.save(TestDataFactory.HistoricalOptionMother.completeWithOnePriceData().build());

        assertNotNull(option.getId());

        List<HistoricalOption> found = new ArrayList<>(subject.findByTicker("TEST"));
        assertEquals(1, found.size());
        assertTrue("Historical data should not be empty.", found.stream().anyMatch(item -> item.getOptionPriceData() != null && !item.getOptionPriceData().isEmpty()));

        Set<HistoricalOption> nonFound = subject.findByTicker("1234");
        assertEquals(0, nonFound.size());
    }

    @Test
    public void testFindByTickerAndExpirationDate() {
        subject.save(TestDataFactory.HistoricalOptionMother.noPriceData().expiration(LocalDate.now()).ticker("AAPL").build());
        subject.save(TestDataFactory.HistoricalOptionMother.completeWithOnePriceData().strike(101.5).ticker("AAPL").build());

        Set<HistoricalOption> found = subject.findByExpirationAndTicker(LocalDate
                .now(),"AAPL");

        assertEquals(2, found.size());

        Set<HistoricalOption> notFound = subject.findByExpirationAndTicker(LocalDate.now().minusDays(1), "AAPL");
        assertEquals(0, notFound.size());

        notFound = subject.findByExpirationAndTicker(LocalDate.now(), "TEST");
        assertEquals(0, notFound.size());
    }

    @Test
    public void testFindByTickerAndExpirationDateAndStrikeAndOptionType() {
        subject.save(TestDataFactory.HistoricalOptionMother.completeWithOnePriceData().strike(12.5).build());
        subject.save(TestDataFactory.HistoricalOptionMother.completeWithOnePriceData().strike(13.0).build());

        HistoricalOption found = subject.findByStrikeAndExpirationAndTickerAndOptionType(12.5, LocalDate.now(), "TEST", Option.OptionType.CALL).orElseGet(() -> {
            fail("Option not found.");
            return null;
        });
        assertEquals(12.5, found.getStrike(), 0.01);
        assertNotNull(found.getMostRecentPriceData());
        assertEquals(1, found.getOptionPriceData().size());
    }

    @Test
    public void testAddMultipleSameOptionsCallAndPut_addsOptions() {
        subject.save(TestDataFactory.HistoricalOptionMother.completeWithOnePriceData().optionType(Option.OptionType.CALL).build());
        subject.saveAndFlush(TestDataFactory.HistoricalOptionMother.completeWithOnePriceData().optionType(Option.OptionType.PUT).build());

        Set<HistoricalOption> results = subject.findByTicker("TEST");
        assertEquals(2, results.size());
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

    @Test
    public void testFindByExpirationAndTickerWithDataInDateRange() {
        Set<OptionPriceData> optionPriceData = new HashSet<>();
        OptionPriceData optionPriceData1 = TestDataFactory.OptionPriceDataMother.complete().tradeDate(LocalDate.now().minusDays(5)).build();
        OptionPriceData optionPriceData2 = TestDataFactory.OptionPriceDataMother.complete().tradeDate(LocalDate.now().minusDays(10)).build();
        OptionPriceData optionPriceData3 = TestDataFactory.OptionPriceDataMother.complete().tradeDate(LocalDate.now()).build();
        OptionPriceData optionPriceData4 = TestDataFactory.OptionPriceDataMother.complete().tradeDate(LocalDate.now().minusDays(20)).build();
        OptionPriceData optionPriceData5 = TestDataFactory.OptionPriceDataMother.complete().tradeDate(LocalDate.now().minusDays(1)).build();
        optionPriceData.add(optionPriceData1);
        optionPriceData.add(optionPriceData2);
        optionPriceData.add(optionPriceData3);
        HistoricalOption historicalOption = TestDataFactory.HistoricalOptionMother.noPriceData().historicalPriceData(optionPriceData).build();
        HistoricalOption historicalOption2 = TestDataFactory.HistoricalOptionMother.noPriceData().expiration(LocalDate.now().minusDays(20)).historicalPriceData(Collections.singleton(optionPriceData4)).build();
        HistoricalOption historicalOption3 = TestDataFactory.HistoricalOptionMother.noPriceData().expiration(LocalDate.now().minusDays(50)).historicalPriceData(Collections.singleton(optionPriceData5)).build();
        optionPriceData1.setOption(historicalOption);
        optionPriceData2.setOption(historicalOption);
        optionPriceData3.setOption(historicalOption);
        optionPriceData4.setOption(historicalOption2);
        optionPriceData5.setOption(historicalOption3);
        subject.saveAllAndFlush(Arrays.asList(historicalOption, historicalOption2, historicalOption3));

        Set<HistoricalOption> returned = jdbcRepository.findByTickerBetweenDates("TEST", LocalDate.now().minusDays(6), LocalDate.now());
        assertEquals(2, returned.size());
        Collection<OptionPriceData> priceData = returned.stream().filter(x -> x.getExpiration().equals(LocalDate.now())).findFirst().get().getOptionPriceData();
        assertEquals(2, priceData.size());
    }

    @Test
    public void testGetExpirationDatesForOptionsAfterDate() {
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        LocalDate expiration1 = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));
        LocalDate expiration2 = expiration1.with(TemporalAdjusters.next(DayOfWeek.FRIDAY));
        LocalDate expiration3 = expiration2.with(TemporalAdjusters.next(DayOfWeek.FRIDAY));

        HistoricalOption option1 = TestDataFactory.HistoricalOptionMother.noPriceData().historicalPriceData(new HashSet<>(Arrays.asList(
                TestDataFactory.OptionPriceDataMother.complete().tradeDate(monday).build(),
                TestDataFactory.OptionPriceDataMother.complete().tradeDate(monday.plusDays(1)).build(),
                TestDataFactory.OptionPriceDataMother.complete().tradeDate(monday.plusDays(3)).build()
        ))).expiration(expiration1).build();
        HistoricalOption option2 = TestDataFactory.HistoricalOptionMother.noPriceData().historicalPriceData(new HashSet<>(Arrays.asList(
                TestDataFactory.OptionPriceDataMother.complete().tradeDate(monday).build(),
                TestDataFactory.OptionPriceDataMother.complete().tradeDate(monday.plusDays(2)).build(),
                TestDataFactory.OptionPriceDataMother.complete().tradeDate(monday.plusDays(3)).build()
        ))).expiration(expiration2).build();
        HistoricalOption option3 = TestDataFactory.HistoricalOptionMother.noPriceData().historicalPriceData(new HashSet<>(Arrays.asList(
                TestDataFactory.OptionPriceDataMother.complete().tradeDate(monday.plusDays(1)).build(),
                TestDataFactory.OptionPriceDataMother.complete().tradeDate(monday.plusDays(2)).build(),
                TestDataFactory.OptionPriceDataMother.complete().tradeDate(monday.plusDays(3)).build()
        ))).expiration(expiration3).build();

        subject.saveAllAndFlush(Arrays.asList(option1, option2, option3));

        Set<LocalDate> actual = jdbcRepository.getExpirationDatesForOptionsAfterDate("TEST", monday);

        assertEquals(3, actual.size());
        assertTrue(actual.contains(expiration1));
        assertTrue(actual.contains(expiration2));
        assertTrue(actual.contains(expiration3));
    }
}
