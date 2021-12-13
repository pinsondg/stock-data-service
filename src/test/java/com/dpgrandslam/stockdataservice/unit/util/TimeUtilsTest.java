package com.dpgrandslam.stockdataservice.unit.util;

import com.dpgrandslam.stockdataservice.domain.util.TimeUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;

import static junit.framework.TestCase.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TimeUtilsTest {

    @Spy
    private TimeUtils timeUtils;

    @Test
    public void testIsStockMarketHoliday() {
        LocalDate holiday = LocalDate.of(2021, 4, 2);

        assertTrue(timeUtils.isStockMarketHoliday(holiday));

        LocalDate nonHoliday = LocalDate.of(2021, 2, 5);

        assertFalse(timeUtils.isStockMarketHoliday(nonHoliday));
    }

    @Test
    public void testGetStartDayOfTradeWeek() {
        LocalDate startDate = LocalDate.of(2021, Month.OCTOBER, 29);

        when(timeUtils.getNowAmericaNewYork()).thenReturn(startDate.atStartOfDay());

        LocalDate actual = timeUtils.getStartDayOfCurrentTradeWeek();

        assertEquals(startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), actual);

        actual = timeUtils.getStartDayOfCurrentTradeWeek(3);

        assertEquals(LocalDate.of(2021, Month.OCTOBER, 4), actual);
    }
}
