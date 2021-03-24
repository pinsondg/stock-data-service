package com.dpgrandslam.stockdataservice.unit.util;

import com.dpgrandslam.stockdataservice.domain.util.TimeUtils;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class TimeUtilsTest {

    private TimeUtils timeUtils;

    @Before
    public void setup() {
        timeUtils = new TimeUtils();
    }

    @Test
    public void testIsStockMarketHoliday() {
        LocalDate holiday = LocalDate.of(2021, 4, 2);

        assertTrue(timeUtils.isStockMarketHoliday(holiday));

        LocalDate nonHoliday = LocalDate.of(2021, 2, 5);

        assertFalse(timeUtils.isStockMarketHoliday(nonHoliday));
    }
}
