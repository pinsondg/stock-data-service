package com.dpgrandslam.stockdataservice.integration.api;

import com.dpgrandslam.stockdataservice.domain.service.OptionsChainLoadService;
import com.dpgrandslam.stockdataservice.domain.service.StockDataLoadService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockserver.matchers.Times;
import org.mockserver.model.Header;
import org.mockserver.model.HttpResponse;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.io.IOException;
import java.time.LocalDate;

import static com.dpgrandslam.stockdataservice.testUtils.TestUtils.loadBodyFromTestResourceFile;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockserver.model.HttpRequest.request;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class StockDataServiceControllerIntTest extends APIIntTestBase {

    @SpyBean
    private OptionsChainLoadService optionsChainLoadService;

    @SpyBean
    private StockDataLoadService stockDataLoadService;

    @Before
    public void init() throws IOException {
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

        mockServerRule.getClient().when(
                request()
                        .withMethod("GET")
                        .withPath("/quote/SPY/options")
                        .withQueryStringParameter("p", "SPY"),
                Times.unlimited()
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withHeader("Content-Type", "text/html")
                .withBody(loadBodyFromTestResourceFile("mocks/yahoofinance/yahoo-finance-spy.html")));
    }

    @Test
    public void test_getOptionsChain_withEndDate_jsonStructureCorrect() throws Exception {
        verifyCorrectJsonStructure_options(mockMvc.perform(get("/data/option/SPY?endDate=" + LocalDate.now().plusDays(3).toString())
                .accept(MediaType.APPLICATION_JSON)));

        verify(optionsChainLoadService, times(1)).loadFullOptionsChainWithAllDataBetweenDates(
                eq("SPY"),
                eq(LocalDate.MIN),
                eq(LocalDate.now().plusDays(3))
        );
    }

    @Test
    public void test_getOptionsChain_withEndDateOutOfRange_emptyOptions() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mockMvc.perform(get("/data/option/SPY?endDate=2019-01-01")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("[0].allOptions").isEmpty())
        .andReturn().getResponse().getContentAsString();


        verify(optionsChainLoadService, times(1)).loadFullOptionsChainWithAllDataBetweenDates(
                eq("SPY"),
                eq(LocalDate.MIN),
                eq(LocalDate.of(2019, 1, 1))
        );
    }

    @Test
    public void test_getOptionsChain_withStartDate_jsonStructureCorrect() throws Exception {
        verifyCorrectJsonStructure_options(mockMvc.perform(get("/data/option/SPY?startDate=2021-03-01")
                .accept(MediaType.APPLICATION_JSON)));


        verify(optionsChainLoadService, times(1)).loadFullOptionsChainWithAllDataBetweenDates(
                eq("SPY"),
                eq(LocalDate.of(2021, 3, 1)),
                eq(LocalDate.now())
        );
    }

    @Test
    public void test_searchStock_jsonStructureCorrect() throws Exception {
        verifyCorrectJsonStructure_stockSearch(mockMvc.perform(get("/data/stock/search?q=apple")));

        verify(stockDataLoadService, times(1)).searchStock(eq("apple"));
    }

    private MvcResult verifyCorrectJsonStructure_stockSearch(ResultActions jsonResultActions) throws Exception {
        return jsonResultActions.andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("[0].ticker").isString())
                .andExpect(jsonPath("[0].name").isString())
                .andReturn();
    }

    private MvcResult verifyCorrectJsonStructure_options(ResultActions jsonResultActions) throws Exception {
        return jsonResultActions.andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("[0].ticker").value("SPY"))
                .andExpect(jsonPath("[0].expirationDate").value("2021-03-05"))
                .andExpect(jsonPath("[0].allOptions").isArray())
                .andExpect(jsonPath("[0].allOptions[0].ticker").isNotEmpty())
                .andExpect(jsonPath("[0].allOptions[0].strike").isNotEmpty())
                .andExpect(jsonPath("[0].allOptions[0].optionType").isNotEmpty())
                .andExpect(jsonPath("[0].allOptions[0].expiration").isNotEmpty())
                .andExpect(jsonPath("[0].allOptions[0].optionPriceData").isArray())
                .andReturn();
    }
}
