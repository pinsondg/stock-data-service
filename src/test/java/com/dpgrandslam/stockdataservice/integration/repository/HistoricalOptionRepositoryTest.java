package com.dpgrandslam.stockdataservice.integration.repository;

import com.dpgrandslam.stockdataservice.adapter.repository.HistoricalOptionRepository;
import com.dpgrandslam.stockdataservice.domain.model.options.HistoricalOption;
import com.dpgrandslam.stockdataservice.domain.model.options.Option;
import com.dpgrandslam.stockdataservice.testUtils.TestDataFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

@Slf4j
public class HistoricalOptionRepositoryTest extends RepositoryIntTestBase {

    @Autowired
    protected HistoricalOptionRepository subject;

    @Test
    public void testAddAndRemoveData() {
        HistoricalOption option = subject.save(TestDataFactory.HistoricalOptionMother.completeWithOnePriceData().build());

        assertNotNull(option.getId());

        List<HistoricalOption> found = new ArrayList<>(subject.findByTicker("TEST"));
        assertEquals(1, found.size());
        assertTrue("Historical data should not be empty.", found.stream().anyMatch(item -> item.getHistoricalPriceData() != null && !item.getHistoricalPriceData().isEmpty()));

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

        HistoricalOption found = subject.findByTickerStrikeOptionTypeAndExpiration(LocalDate.now(), "TEST", 12.5, Option.OptionType.CALL).orElseGet(() -> {
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
