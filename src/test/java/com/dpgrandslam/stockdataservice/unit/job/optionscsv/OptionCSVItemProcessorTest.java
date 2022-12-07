package com.dpgrandslam.stockdataservice.unit.job.optionscsv;

import com.dpgrandslam.stockdataservice.domain.jobs.optioncsv.OptionCSVItemProcessor;
import com.dpgrandslam.stockdataservice.domain.jobs.optioncsv.OptionCSVFile;
import com.dpgrandslam.stockdataservice.domain.model.options.HistoricalOption;
import com.dpgrandslam.stockdataservice.domain.model.options.Option;
import com.dpgrandslam.stockdataservice.domain.model.stock.TrackedStock;
import com.dpgrandslam.stockdataservice.domain.service.HistoricOptionsDataService;
import com.dpgrandslam.stockdataservice.domain.service.TrackedStockService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.Collections;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OptionCSVItemProcessorTest {

    @Mock
    private HistoricOptionsDataService historicOptionsDataService;

    @Mock
    private TrackedStockService trackedStockService;

    @InjectMocks
    private OptionCSVItemProcessor subject;

    @Test
    public void testProcess_nonISODate() throws Exception {
        String expiration = "1/2/2020";
        String tradeDate = "12/23/2019";

        OptionCSVFile file = new OptionCSVFile();
        file.setAskPrice("1.0");
        file.setBidPrice("1.0");
        file.setDataDate(tradeDate);
        file.setExpirationDate(expiration);
        file.setSymbol("SPY");
        file.setAskSize("100");
        file.setBidSize("100");
        file.setLastPrice("1.0");
        file.setOpenInterest("1000");
        file.setVolume("1000");
        file.setPutCall("CALL");
        file.setStrikePrice("120.0");

        when(trackedStockService.getAllTrackedStocks(anyBoolean())).thenReturn(Collections.singletonList(TrackedStock
                .builder()
                .ticker("SPY")
                .active(true)
                .optionsHistoricDataStartDate(LocalDate.MIN)
                .build()));

        HistoricalOption actual = subject.process(file);

        assertNotNull(actual);
        assertEquals(actual.getOptionType(), Option.OptionType.CALL);
        assertEquals(actual.getExpiration(), LocalDate.of(2020, 1, 2));
        assertEquals(actual.getOptionPriceData().stream().findFirst().get().getTradeDate(), LocalDate.of(2019, 12, 23));
    }
}
