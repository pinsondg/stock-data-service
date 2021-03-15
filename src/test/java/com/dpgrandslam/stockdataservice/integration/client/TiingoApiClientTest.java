package com.dpgrandslam.stockdataservice.integration.client;

import com.dpgrandslam.stockdataservice.adapter.apiclient.tiingo.TiingoApiClient;
import com.dpgrandslam.stockdataservice.domain.model.tiingo.TiingoStockEndOfDayResponse;
import com.dpgrandslam.stockdataservice.domain.model.tiingo.TiingoStockSearchResponse;
import org.junit.Test;
import org.mockserver.matchers.Times;
import org.mockserver.model.Header;
import org.mockserver.model.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static junit.framework.TestCase.assertEquals;
import static org.mockserver.model.HttpRequest.request;

public class TiingoApiClientTest extends MockClientTest {

    @Autowired
    private TiingoApiClient subject;

    @Test
    public void testSearch() throws IOException {
        mockServerRule.getClient().when(
                request()
                        .withMethod("GET")
                        .withPath("/tiingo/utilities/search")
                        .withQueryStringParameter("query", "apple")
                        .withHeader(Header.header("Authorization.*")),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withBody(loadBodyFromTestResourceFile("mocks/tiingo/mock-search-response-apple.json")));

        List<TiingoStockSearchResponse> response = subject.searchStock("apple");

        assertEquals(10, response.size());
    }

    @Test
    public void testGetEndOfDayStockData() throws IOException {
        mockServerRule.getClient().when(
                request()
                        .withMethod("GET")
                        .withPath("/tiingo/daily/AAPL/prices")
                        .withHeader(Header.header("Authorization.*")),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withBody(loadBodyFromTestResourceFile("mocks/tiingo/mock-end-of-day-responce-aapl.json")));

        Set<TiingoStockEndOfDayResponse> endOfDayResponse = subject.getEndOfDayInfo("AAPL");

        assertEquals(LocalDate.of(2021, 3, 8), endOfDayResponse.stream().findFirst().get().getDate());
    }
}
