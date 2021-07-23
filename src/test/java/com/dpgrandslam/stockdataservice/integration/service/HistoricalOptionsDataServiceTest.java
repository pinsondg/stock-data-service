package com.dpgrandslam.stockdataservice.integration.service;

import com.dpgrandslam.stockdataservice.domain.model.options.HistoricalOption;
import com.dpgrandslam.stockdataservice.domain.service.HistoricOptionsDataService;
import com.dpgrandslam.stockdataservice.integration.client.MockClientTest;
import com.dpgrandslam.stockdataservice.testUtils.TestDataFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Slice;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static junit.framework.TestCase.*;

public class HistoricalOptionsDataServiceTest extends MockClientTest {

    @Autowired
    private HistoricOptionsDataService subject;

    @Test
    @Transactional
    public void testAddOptions() throws IOException {
        HistoricalOption testOption1 = TestDataFactory.HistoricalOptionMother.noPriceData().ticker("TEST").build();

        HistoricalOption saved = subject.addOption(testOption1);

        assertNotNull(saved.getId());
        assertEquals(0, saved.getHistoricalPriceData().size());

        HistoricalOption testOption2 = TestDataFactory.HistoricalOptionMother.completeWithOnePriceData().ticker("TEST").build();

        HistoricalOption saved2 = subject.addOption(testOption2);

        Slice<HistoricalOption> options = subject.findOptions("TEST", 0, 100);
        assertEquals(1, options.getContent().size());
        assertEquals(1, options.getContent().get(0).getHistoricalPriceData().size());
        assertEquals(saved.getId(), saved2.getId());
    }
}
