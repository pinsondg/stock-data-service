package com.dpgrandslam.stockdataservice.integration.service;

import com.dpgrandslam.stockdataservice.domain.model.FearGreedIndex;
import com.dpgrandslam.stockdataservice.domain.service.CNNFearGreedDataLoadService;
import com.dpgrandslam.stockdataservice.domain.util.TimeUtils;
import com.dpgrandslam.stockdataservice.integration.client.MockClientTest;
import com.dpgrandslam.stockdataservice.testUtils.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockserver.model.HttpRequest.request;

public class CNNFearGreedDataLoadServiceTest extends MockClientTest {

    @Autowired
    private CNNFearGreedDataLoadService subject;

    @MockBean
    private TimeUtils timeUtils;

    @Before
    public void setup() {
        when(timeUtils.getCurrentOrLastTradeDate(any(LocalDateTime.class))).then(invok -> {
            LocalDateTime ld = invok.getArgument(0);
            return ld.toLocalDate();
        });
    }

    @Test
    public void testLoadFearGreedIndex_beforeClose() throws IOException {
        mockServerRule.getClient().when(
                request().withMethod("GET").withPath("/index/fearandgreed/graphdata/.*"),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withHeader("Content-Type", "application/json")
                .withBody(TestUtils.loadBodyFromTestResourceFile("mocks/cnn/cnn-fear-greed-index-api-response.json"))
        );

        when(timeUtils.getCurrentOrLastTradeDate()).thenReturn(LocalDate.now());
        when(timeUtils.getNowAmericaNewYork()).thenReturn(LocalDateTime.of(LocalDate.now(), LocalTime.of(3, 30)));

        Set<FearGreedIndex> actual = subject.loadCurrentFearGreedIndex();
        assertEquals(5, actual.size());
        assertTrue(actual.stream().anyMatch(x -> x.getValue() == 40 && x.getTradeDate().equals(LocalDate.now().minusDays(1))));
        assertTrue(actual.stream().anyMatch(x -> x.getValue() == 45 && x.getTradeDate().equals(LocalDate.now().minusWeeks(1))));
        assertTrue(actual.stream().anyMatch(x -> x.getValue() == 46 && x.getTradeDate().equals(LocalDate.now().minusMonths(1))));
        assertTrue(actual.stream().anyMatch(x -> x.getValue() == 53 && x.getTradeDate().equals(LocalDate.now().minusYears(1))));
        assertTrue(actual.stream().anyMatch(x -> x.getValue() == 31 && x.getTradeDate().equals(LocalDate.now())));

    }

    @Test
    public void testSaveAll_repeatingItems_stillSaves() {
        FearGreedIndex fearGreedIndex1 = new FearGreedIndex();
        fearGreedIndex1.setTradeDate(LocalDate.now());
        fearGreedIndex1.setValue(21);
        FearGreedIndex fearGreedIndex2 = new FearGreedIndex();
        fearGreedIndex2.setValue(22);
        fearGreedIndex2.setTradeDate(LocalDate.now().minusDays(10));
        FearGreedIndex fearGreedIndex3 = new FearGreedIndex();
        fearGreedIndex3.setValue(12);
        fearGreedIndex3.setTradeDate(LocalDate.now().minusDays(20));
        FearGreedIndex fearGreedIndex4 = new FearGreedIndex();
        fearGreedIndex4.setTradeDate(LocalDate.now());
        fearGreedIndex4.setValue(21);

        //Save 1 twice
        subject.saveFearGreedData(Arrays.asList(fearGreedIndex1, fearGreedIndex2));
        subject.saveFearGreedData(Arrays.asList(fearGreedIndex4, fearGreedIndex3));

        List<FearGreedIndex> fearGreedIndexList = subject.loadFearGreedDataBetweenDates(LocalDate.now().minusDays(20), LocalDate.now());
        assertEquals(3, fearGreedIndexList.size());
        assertTrue(fearGreedIndexList.stream().anyMatch(x -> x.getValue() == 21 && x.getTradeDate().equals(LocalDate.now())));

    }
}
