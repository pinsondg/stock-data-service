package com.dpgrandslam.stockdataservice.unit.model.stock;

import com.dpgrandslam.stockdataservice.domain.model.stock.YahooFinanceQuote;
import org.junit.Test;

import static junit.framework.TestCase.assertNull;

public class YahooFinanceQuoteTest {

    @Test
    public void testCorrectValuesNull() {
        YahooFinanceQuote treasuryYield = new YahooFinanceQuote();
        assertNull(treasuryYield.getAdjOpen());
        assertNull(treasuryYield.getVolume());
        assertNull(treasuryYield.getAdjVolume());
        assertNull(treasuryYield.getSplitFactor());
        assertNull(treasuryYield.getAdjOpen());
        assertNull(treasuryYield.getAdjClose());
        assertNull(treasuryYield.getAdjLow());
        assertNull(treasuryYield.getAdjHigh());
        assertNull(treasuryYield.getSplitFactor());
    }
}
