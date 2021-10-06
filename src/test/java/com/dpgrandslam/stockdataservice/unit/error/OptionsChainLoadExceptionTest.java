package com.dpgrandslam.stockdataservice.unit.error;

import com.dpgrandslam.stockdataservice.domain.error.OptionsChainLoadException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OptionsChainLoadExceptionTest {

    private OptionsChainLoadException optionsChainLoadException;

    @Test
    public void testOptionsChainLoadException_getMessage() {
        optionsChainLoadException = new OptionsChainLoadException("TEST", "http://test.com", "Error");
        String expected = "Could not load options chain for ticker TEST at URL http://test.com. Error";
        assertEquals(expected, optionsChainLoadException.getMessage());
    }
}
