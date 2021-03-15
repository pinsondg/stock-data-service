package com.dpgrandslam.stockdataservice.unit.model;

import com.dpgrandslam.stockdataservice.domain.model.options.HistoricalOption;
import com.dpgrandslam.stockdataservice.testUtils.TestDataFactory;
import org.junit.Test;

import java.time.LocalDate;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class OptionTest {

    @Test
    public void testIsExpired() {
        HistoricalOption option = TestDataFactory.HistoricalOptionMother.completeWithOnePriceData().expiration(LocalDate.now().minusDays(2)).build();
        assertTrue(option.isExpired());

        option = TestDataFactory.HistoricalOptionMother.noPriceData().expiration(LocalDate.now().plusDays(10)).build();
        assertFalse(option.isExpired());

        option = TestDataFactory.HistoricalOptionMother.noPriceData().build();
        assertFalse(option.isExpired());
    }
}
