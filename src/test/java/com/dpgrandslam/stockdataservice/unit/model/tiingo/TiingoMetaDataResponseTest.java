package com.dpgrandslam.stockdataservice.unit.model.tiingo;

import com.dpgrandslam.stockdataservice.domain.model.tiingo.TiingoMetaDataResponse;
import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class TiingoMetaDataResponseTest {

    @Test
    public void test() {
        TiingoMetaDataResponse response = new TiingoMetaDataResponse();
        response.setStartDate("2021-01-01");
        response.setEndDate("2021-01-01");
        response.setDetail("Not Found.");
        assertEquals(LocalDate.of(2021, 1, 1), response.getHistoricDataStartDate());
        assertEquals(LocalDate.of(2021, 1, 1), response.getHistoricDataEndDate());
        assertFalse(response.isValid());
    }
}
