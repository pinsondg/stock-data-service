package com.dpgrandslam.stockdataservice.integration.service;

import com.dpgrandslam.stockdataservice.domain.model.options.HistoricalOption;
import com.dpgrandslam.stockdataservice.domain.model.options.Option;
import com.dpgrandslam.stockdataservice.domain.service.HistoricOptionsDataService;
import com.dpgrandslam.stockdataservice.integration.client.MockClientTest;
import com.dpgrandslam.stockdataservice.testUtils.TestDataFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.*;

public class HistoricalOptionsDataServiceTest extends MockClientTest {

    @Autowired
    private HistoricOptionsDataService subject;

    @Test
    public void testAddOptions() throws IOException, InterruptedException {
        HistoricalOption testOption1 = TestDataFactory.HistoricalOptionMother.noPriceData().ticker("TEST").build();

        HistoricalOption saved = subject.addOption(testOption1);

        assertNotNull(saved.getId());
        assertEquals(0, saved.getOptionPriceData().size());

        HistoricalOption testOption2 = TestDataFactory.HistoricalOptionMother.completeWithOnePriceData().ticker("TEST").build();

        HistoricalOption saved2 = subject.addOption(testOption2);

        List<Option> options = new ArrayList<>(subject.findOptionsWithCache("TEST"));
        assertEquals(1, options.size());
        assertEquals(1, options.get(0).getOptionPriceData().size());
        assertEquals(saved.getId(), saved2.getId());
    }
}
