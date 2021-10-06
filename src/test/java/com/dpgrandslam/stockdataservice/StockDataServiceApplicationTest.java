package com.dpgrandslam.stockdataservice;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertTrue;

public class StockDataServiceApplicationTest {

    @Test
    public void testApplicationStarts() {
        StockDataServiceApplication.main(new String[0]);
        assertTrue(true);
    }
}
