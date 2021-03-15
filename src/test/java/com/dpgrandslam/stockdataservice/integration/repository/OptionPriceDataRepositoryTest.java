package com.dpgrandslam.stockdataservice.integration.repository;

import com.dpgrandslam.stockdataservice.adapter.repository.HistoricalOptionRepository;
import com.dpgrandslam.stockdataservice.adapter.repository.OptionPriceDataRepository;
import com.dpgrandslam.stockdataservice.domain.model.options.HistoricalOption;
import com.dpgrandslam.stockdataservice.domain.model.options.OptionPriceData;
import com.dpgrandslam.stockdataservice.testUtils.TestDataFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

public class OptionPriceDataRepositoryTest extends RepositoryIntTestBase {

    @Autowired
    private OptionPriceDataRepository subject;

    @Autowired
    private HistoricalOptionRepository historicalOptionRepository;


    @Test
    public void testFindByOptionId() {
        HistoricalOption option = historicalOptionRepository.save(TestDataFactory.HistoricalOptionMother.completeWithOnePriceData().build());

        assertEquals(1, subject.findAllByOptionId(option.getId()).count());
    }

    @Test
    public void testFindAllByOptionIdAndDataObtainedDateBetween() {
        OptionPriceData priceData1 = TestDataFactory.OptionPriceDataMother.complete().dataObtainedDate(Timestamp.from(Instant.now().minus(2, ChronoUnit.DAYS))).build();
        OptionPriceData priceData2 = TestDataFactory.OptionPriceDataMother.complete().dataObtainedDate(Timestamp.from(Instant.now().minus(10, ChronoUnit.DAYS))).build();
        OptionPriceData priceData3 = TestDataFactory.OptionPriceDataMother.complete().dataObtainedDate(Timestamp.from(Instant.now().minus(1000, ChronoUnit.SECONDS))).build();

        Set<OptionPriceData> priceDataSet = new HashSet<>();
        priceDataSet.add(priceData1);
        priceDataSet.add(priceData2);
        priceDataSet.add(priceData3);

        HistoricalOption option = TestDataFactory.HistoricalOptionMother.noPriceData().historicalPriceData(priceDataSet).build();

        option = historicalOptionRepository.save(option);

        List<OptionPriceData> found = subject.findAllByOptionIdAndDataObtainedDateBetween(option.getId(),
                Timestamp.from(Instant.now().minus(5, ChronoUnit.DAYS)),
                Timestamp.from(Instant.now()))
                .collect(Collectors.toList());

        assertEquals(2, found.size());
        assertNotNull(found.get(0).getOption());
        assertEquals(option.getId(), found.get(0).getOption().getId());
    }
}
