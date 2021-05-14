package com.dpgrandslam.stockdataservice.unit.model;

import com.dpgrandslam.stockdataservice.domain.model.options.OptionPriceData;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OptionPriceDataTest {

    private OptionPriceData optionPriceData;

    @Before
    public void init() {
        optionPriceData = new OptionPriceData();
    }

    @Test
    public void testGetMarketPrice() {
        optionPriceData.setLastTradePrice(1234.0);
        optionPriceData.setAsk(12.0);
        optionPriceData.setBid(24.0);

        assertEquals(18.00, optionPriceData.getMarketPrice(), 0.00);
    }

    @Test
    public void testGetMargetPrice_zeroAsk() {
        optionPriceData.setAsk(0.0);
        optionPriceData.setBid(20.0);

        assertEquals(10.00, optionPriceData.getMarketPrice(), 0.00);
    }

    @Test
    public void testGetMarketPrice_zeroAsk_oneBid() {
        optionPriceData.setBid(0.01);
        optionPriceData.setAsk(0.0);

        assertEquals(0.01, optionPriceData.getMarketPrice(), 0.00);
    }

    @Test
    public void testGetMarketPrice_roundsCorrectly() {
        optionPriceData.setAsk(0.98);
        optionPriceData.setBid(0.97);

        assertEquals(0.980, optionPriceData.getMarketPrice(), 0.000);
    }

    @Test
    public void testGetMarketPrice_zeroBidAndAsk() {
        optionPriceData.setBid(0.0);
        optionPriceData.setAsk(0.0);

        assertEquals(0.00, optionPriceData.getMarketPrice(), 0.00);
    }
}
