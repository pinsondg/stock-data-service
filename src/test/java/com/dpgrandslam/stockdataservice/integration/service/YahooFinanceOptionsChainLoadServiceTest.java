package com.dpgrandslam.stockdataservice.integration.service;

import com.dpgrandslam.stockdataservice.domain.model.options.OptionsChain;
import com.dpgrandslam.stockdataservice.domain.service.YahooFinanceOptionsChainLoadService;
import com.dpgrandslam.stockdataservice.integration.client.MockClientTest;
import org.junit.Ignore;
import org.junit.Test;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.time.LocalDate;

import static junit.framework.TestCase.assertNotNull;
import static org.mockserver.model.HttpRequest.request;

@Ignore
public class YahooFinanceOptionsChainLoadServiceTest extends MockClientTest {

    @Autowired
    private YahooFinanceOptionsChainLoadService subject;

    @Test
    public void testLoadClosestOptionsChain() throws IOException {
        mockServerRule.getClient().when(
                request()
                        .withMethod("GET")
                        .withPath("/quote/SPY/options")
                        .withQueryStringParameter("p", "SPY"),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withHeader("Content-Type", "text/html")
                .withBody(loadBodyFromTestResourceFile("mocks/yahoofinance/yahoo-finance-spy.html")));

        OptionsChain optionsChain = subject.loadLiveOptionsChainForClosestExpiration("SPY");

        assertNotNull(optionsChain);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadOptionsChain_emptyOptionsChain_throwsException() throws IOException {
        mockServerRule.getClient().when(request()
                .withMethod("GET")
                .withPath("/quote/AAPL/options")
                .withQueryStringParameter("p", "AAPL")
                .withQueryStringParameter("date", "*"),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withBody(loadBodyFromTestResourceFile("mocks/yahoofinance/yahoo-finance-aapl_empty-chain.html"))
        );

        OptionsChain chain = subject.loadLiveOptionsChainForExpirationDate("AAPL", LocalDate.now());
    }

}
