package com.dpgrandslam.stockdataservice.unit.error;

import com.dpgrandslam.stockdataservice.domain.error.TreasuryYieldLoadException;
import org.junit.Test;

import java.time.LocalDate;

import static junit.framework.TestCase.assertEquals;

public class TreasuryYieldLoadExceptionTest {

    private TreasuryYieldLoadException subject;

    @Test
    public void testGetMessage_weekday() {
        subject = new TreasuryYieldLoadException(LocalDate.of(2021, 10, 6), "Test Message");
        String expected = "Could not load 10 yr treasury yield for date 2021-10-06. Test Message";
        assertEquals(expected, subject.getMessage());
    }

    @Test
    public void testGetMessage_weekend() {
        subject = new TreasuryYieldLoadException(LocalDate.of(2021, 10, 3), "Test Message", new RuntimeException("Test Exception"));
        String expected = "Could not load 10 yr treasury yield for date 2021-10-03. Failure most likely due to the date being a weekend. Test Message";
        assertEquals(expected, subject.getMessage());
    }

}
