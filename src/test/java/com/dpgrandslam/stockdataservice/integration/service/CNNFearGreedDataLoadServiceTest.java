package com.dpgrandslam.stockdataservice.integration.service;

import com.dpgrandslam.stockdataservice.domain.model.FearGreedIndex;
import com.dpgrandslam.stockdataservice.domain.service.CNNFearGreedDataLoadService;
import com.dpgrandslam.stockdataservice.domain.util.TimeUtils;
import com.dpgrandslam.stockdataservice.integration.client.MockClientTest;
import com.dpgrandslam.stockdataservice.testUtils.TestUtils;
import org.junit.Test;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockserver.model.HttpRequest.request;

public class CNNFearGreedDataLoadServiceTest extends MockClientTest {

    @Autowired
    private CNNFearGreedDataLoadService subject;

    @MockBean
    private TimeUtils timeUtils;

    @Test
    public void testLoadFearGreedIndex_beforeClose() throws IOException {
        mockServerRule.getClient().when(
                request().withMethod("GET").withPath("/data/fear-and-greed"),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withHeader("Content-Type", "text/html")
                .withBody(TestUtils.loadHtmlFileAndClean("mocks/cnn/cnn-fear-greed-index.html"))
        );

        when(timeUtils.getLastTradeDate()).thenReturn(LocalDate.now());
        when(timeUtils.getNowAmericaNewYork()).thenReturn(LocalDateTime.of(LocalDate.now(), LocalTime.of(3, 30)));

        Set<FearGreedIndex> actual = subject.loadCurrentFearGreedIndex();
        assertEquals(4, actual.size());
        assertTrue(actual.stream().anyMatch(x -> x.getValue() == 30 && x.getTradeDate().equals(LocalDate.now().minusDays(1))));
        assertTrue(actual.stream().anyMatch(x -> x.getValue() == 35 && x.getTradeDate().equals(LocalDate.now().minusWeeks(1))));
        assertTrue(actual.stream().anyMatch(x -> x.getValue() == 83 && x.getTradeDate().equals(LocalDate.now().minusMonths(1))));
        assertTrue(actual.stream().anyMatch(x -> x.getValue() == 69 && x.getTradeDate().equals(LocalDate.now().minusYears(1))));

    }

    @Test
    public void testLoadFearGreedIndex_afterClose() throws IOException {
        mockServerRule.getClient().when(
                request().withMethod("GET").withPath("/data/fear-and-greed"),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withHeader("Content-Type", "text/html")
                .withBody(TestUtils.loadHtmlFileAndClean("mocks/cnn/cnn-fear-greed-index.html"))
        );

        when(timeUtils.getLastTradeDate()).thenReturn(LocalDate.now());
        when(timeUtils.getNowAmericaNewYork()).thenReturn(LocalDateTime.of(LocalDate.now(), LocalTime.of(5, 30)));

        Set<FearGreedIndex> actual = subject.loadCurrentFearGreedIndex();
        assertEquals(4, actual.size());
        assertTrue(actual.stream().anyMatch(x -> x.getValue() == 30 && x.getTradeDate().equals(LocalDate.now())));
        assertTrue(actual.stream().anyMatch(x -> x.getValue() == 35 && x.getTradeDate().equals(LocalDate.now().minusWeeks(1))));
        assertTrue(actual.stream().anyMatch(x -> x.getValue() == 83 && x.getTradeDate().equals(LocalDate.now().minusMonths(1))));
        assertTrue(actual.stream().anyMatch(x -> x.getValue() == 69 && x.getTradeDate().equals(LocalDate.now().minusYears(1))));
    }
}
