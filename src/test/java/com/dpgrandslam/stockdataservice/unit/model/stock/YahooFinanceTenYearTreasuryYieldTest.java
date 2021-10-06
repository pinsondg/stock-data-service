package com.dpgrandslam.stockdataservice.unit.model.stock;

import com.dpgrandslam.stockdataservice.domain.model.stock.YahooFinanceTenYearTreasuryYield;
import org.junit.Test;

import static junit.framework.TestCase.assertNull;

public class YahooFinanceTenYearTreasuryYieldTest {

    @Test
    public void testCorrectValuesNull() {
        YahooFinanceTenYearTreasuryYield treasuryYield = new YahooFinanceTenYearTreasuryYield();
        assertNull(treasuryYield.getAdjOpen());
        assertNull(treasuryYield.getVolume());
        assertNull(treasuryYield.getAdjVolume());
        assertNull(treasuryYield.getSplitFactor());
    }
}
