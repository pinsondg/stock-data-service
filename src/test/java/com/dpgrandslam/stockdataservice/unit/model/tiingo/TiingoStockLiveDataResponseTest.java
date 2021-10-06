package com.dpgrandslam.stockdataservice.unit.model.tiingo;

import com.dpgrandslam.stockdataservice.domain.model.tiingo.TiingoStockLiveDataResponse;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class TiingoStockLiveDataResponseTest {

    @Test
    public void test() {
        TiingoStockLiveDataResponse dataResponse = new TiingoStockLiveDataResponse();
        dataResponse.setAskPrice(12.0);
        dataResponse.setBidPrice(11.0);
        dataResponse.setTngoLast(11.5);
        dataResponse.setLastSaleTimestamp("2021-01-01T00:00:00.00Z");
        dataResponse.setTimestamp("2021-01-01T00:00:00.00Z");
        assertEquals(11.5, dataResponse.getMarketPrice());
        assertEquals(1609459200L, dataResponse.getLastSaleTimestamp().getEpochSecond());
        assertEquals(1609459200L, dataResponse.getTimestamp().getEpochSecond());
    }
}
