package com.dpgrandslam.stockdataservice.acceptance;


import com.dpgrandslam.stockdataservice.adapter.repository.HistoricalOptionRepository;
import com.dpgrandslam.stockdataservice.domain.model.options.*;
import com.dpgrandslam.stockdataservice.testUtils.TestDataFactory;
import com.dpgrandslam.stockdataservice.testUtils.TestUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.Ignore;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static junit.framework.TestCase.*;
import static org.mockserver.model.HttpRequest.request;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Ignore
public class StockDataServiceApiAcceptanceTestSteps extends BaseAcceptanceTestSteps {

    private static final String OPTIONS_ENDPOINT_FORMAT = "/data/option/%s";

    @Autowired
    private HistoricalOptionRepository historicalOptionRepository;

    private MvcResult result;

    private HistoricalOption savedOption;

    @Given("^a historical option with ticker ([^\"]*) exists and has price data$")
    public void addHistoricalOptionsData(String ticker) {
        Set<OptionPriceData> optionPriceDataSet = new HashSet<>();
        optionPriceDataSet.add(TestDataFactory.OptionPriceDataMother.complete().build());
        optionPriceDataSet.add(TestDataFactory.OptionPriceDataMother.complete().build());
        savedOption = TestDataFactory.HistoricalOptionMother.noPriceData().historicalPriceData(optionPriceDataSet).ticker(ticker).build();
        savedOption.getOptionPriceData().add(TestDataFactory.OptionPriceDataMother.complete().build());
        savedOption = historicalOptionRepository.saveAndFlush(savedOption);
        assertNotNull(savedOption.getId());
        assertNotNull("Option should have price data.", savedOption.getOptionPriceData());
        assertFalse("Option should have price data.", savedOption.getOptionPriceData().isEmpty());
    }

    @When("^a successful API call is made to /data/option/([^\"]*) with an end date of today$")
    public void madeAPICallWithEndDate(String ticker) throws Exception {
        AcceptanceTest.mockServerRule.getClient().when(
                request()
                    .withPath("/data/option/" + ticker)
                    .withMethod("GET"),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withBody(TestUtils.loadHtmlFileAndClean("mocks/yahoofinance/yahoo-finance-spy.html"))
        );
        result = mockMvc.perform(get(String.format(OPTIONS_ENDPOINT_FORMAT + "?endDate=" + LocalDate.now(), ticker)))
                .andExpect(status().is2xxSuccessful()).andReturn();
    }

    @Then("the options chain is contained in the response body")
    public void verifyBodyCorrect() throws UnsupportedEncodingException, JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module =
                new SimpleModule("OptionsChainDeserializer", new Version(1, 0, 0, null, null, null));
        module.addDeserializer(OptionsChain.class, new OptionsChain.OptionsChainDeserializer());
        objectMapper.registerModule(module);
        JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, OptionsChain.class);
        List<OptionsChain> returned = objectMapper.readValue(result.getResponse().getContentAsString(), type);
        assertEquals(2, returned.get(0).getOption(new OptionChainKey(12.5, Option.OptionType.CALL)).getOptionPriceData().size());
    }

    @After
    public void cleanup() {
        historicalOptionRepository.deleteAll();
    }

}
