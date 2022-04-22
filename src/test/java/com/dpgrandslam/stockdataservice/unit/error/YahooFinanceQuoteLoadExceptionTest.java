package com.dpgrandslam.stockdataservice.unit.error;

import com.dpgrandslam.stockdataservice.domain.error.YahooFinanceQuoteLoadException;
import org.junit.Test;

import java.time.LocalDate;

import static junit.framework.TestCase.assertEquals;

public class YahooFinanceQuoteLoadExceptionTest {

    private YahooFinanceQuoteLoadException subject;

    @Test
    public void testGetMessage_weekday() {
        LocalDate d =  LocalDate.of(2021, 10, 6);
        subject = new YahooFinanceQuoteLoadException("^VIX", d, d, "Test Message");
        String expected = "Could not load yahoo finance quote for ticker ^VIX for date 2021-10-06. Test Message";
        assertEquals(expected, subject.getMessage());
    }

    @Test
    public void testGetMessage_weekend() {
        LocalDate d = LocalDate.of(2021, 10, 3);
        subject = new YahooFinanceQuoteLoadException("^VIX", d , d, "Test Message", new RuntimeException("Test Exception"));
        String expected = "Could not load yahoo finance quote for ticker ^VIX for date 2021-10-03. Failure most likely due to the date being a weekend. Test Message";
        assertEquals(expected, subject.getMessage());
    }

}
