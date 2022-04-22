package com.dpgrandslam.stockdataservice.integration.service;

import com.dpgrandslam.stockdataservice.domain.model.stock.YahooFinanceQuote;
import com.dpgrandslam.stockdataservice.domain.service.TenYearTreasuryYieldService;
import com.dpgrandslam.stockdataservice.integration.client.MockClientTest;
import com.dpgrandslam.stockdataservice.testUtils.TestUtils;
import org.junit.Test;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.time.LocalDate;

import static junit.framework.TestCase.assertEquals;
import static org.mockserver.model.HttpRequest.request;

public class YahooFinanceTenYearTreasuryYieldTest extends MockClientTest {

    @Autowired
    private TenYearTreasuryYieldService subject;

    @Test
    public void testLoadTenYearTreasuryYield() throws IOException {
        mockServerRule.getClient().when(
                request()
                        .withMethod("GET")
                        .withPath("/quote/%5ETNX/history"),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withHeader("Content-Type", "text/html")
                .withBody(TestUtils.loadHtmlFileAndClean("mocks/yahoofinance/yahoo-finance-tnx.html")));

        LocalDate date = LocalDate.of(2021, 9, 14);
        YahooFinanceQuote tenYearTreasuryYield = subject.getTreasuryYieldForDate(date);

        assertEquals(date, tenYearTreasuryYield.getDate());
        assertEquals(1.3460, tenYearTreasuryYield.getOpen());
        assertEquals(1.3500, tenYearTreasuryYield.getHigh());
        assertEquals(1.2650, tenYearTreasuryYield.getLow());
        assertEquals(1.2770, tenYearTreasuryYield.getClose());
        assertEquals(1.2770, tenYearTreasuryYield.getAdjClose());
    }
}
