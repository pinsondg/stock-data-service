package com.dpgrandslam.stockdataservice.acceptance;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.dpgrandslam.stockdataservice.adapter.repository.HistoricalOptionRepository;
import com.dpgrandslam.stockdataservice.adapter.repository.TrackedStocksRepository;
import com.dpgrandslam.stockdataservice.domain.model.JobRunResponse;
import com.dpgrandslam.stockdataservice.domain.model.options.HistoricalOption;
import com.dpgrandslam.stockdataservice.domain.model.options.Option;
import com.dpgrandslam.stockdataservice.domain.model.stock.TrackedStock;
import com.dpgrandslam.stockdataservice.domain.service.HistoricOptionsDataService;
import com.dpgrandslam.stockdataservice.domain.service.TrackedStockService;
import com.dpgrandslam.stockdataservice.testUtils.TestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.After;
import org.junit.Ignore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import javax.batch.runtime.BatchStatus;
import javax.transaction.Transactional;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Ignore
public class OptionCSVBatchLoadAcceptanceTestSteps extends BaseAcceptanceTestSteps {

    private static final String JOB_ENDPOINT_POST = "/job/run";
    private static final String JOB_ENDPOINT_GET = "/job/status";

    @Value("${job.option-csv.test-bucket-name}")
    private String testBucketName;

    @Value("${job.option-csv.test-key}")
    private String testS3Key;

    @Autowired
    private AmazonS3 amazonS3;

    @Autowired
    private HistoricOptionsDataService historicOptionsDataService;

    @Autowired
    private TrackedStocksRepository trackedStocksRepository;

    @Autowired
    private HistoricalOptionRepository historicalOptionRepository;

    @Autowired
    private TrackedStockService trackedStockService;

    private long jobExecutionId;

    @Before
    public void setup() throws IOException {
//        AcceptanceTest.mockServerRule.getClient().when(
//                request().withPath("/tiingo/daily/*").withMethod("GET"),
//                Times.unlimited()
//        ).respond(HttpResponse.response()
//                .withStatusCode(200)
//                .withBody(TestUtils.loadBodyFromTestResourceFile("mocks/tiingo/mock-search-response-spy.json")));
    }

    @Given("^options data for ([^\"]*) exists in DB$")
    @Transactional
    public void uploadStartingData(String ticker) {
        HistoricalOption saveOption = TestDataFactory.HistoricalOptionMother.noPriceData()
                .ticker(ticker)
                .strike(100.0)
                .optionType(Option.OptionType.CALL)
                .historicalPriceData(Collections.singleton(TestDataFactory.OptionPriceDataMother.complete()
                        .tradeDate(LocalDate.now().minusDays(1))
                        .dataObtainedDate(Timestamp.from(Instant.now()))
                        .build()
                ))
                .build();
        TrackedStock trackedStock = TrackedStock.builder()
                .ticker("SPY")
                .name("S&P 500 ETF TRUST ETF")
                .optionsHistoricDataStartDate(LocalDate.now().minusDays(1))
                .lastOptionsHistoricDataUpdate(LocalDate.now().minusDays(1))
                .active(true)
                .build();
        trackedStocksRepository.save(trackedStock);
        historicOptionsDataService.saveOption(saveOption);
    }

    @And("^test option csv files exist in S3$")
    public void checkTestCSVsExist() {
        List<S3ObjectSummary> objectSummaries = amazonS3.listObjectsV2(testBucketName, testS3Key)
                .getObjectSummaries();
        assertFalse("S3 Bucket does not contain csv files.", objectSummaries.isEmpty());
    }

    @When("^the job is triggered through the API$")
    public void triggerJob() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> body = new HashMap<>();
        Map<String, String> jobParams = new HashMap<>();
        jobParams.put("bucket", testBucketName);
        jobParams.put("keyPrefix", testS3Key);
        body.put("jobName", "job1");
        body.put("jobParams", jobParams);

        MvcResult mvcResult = mockMvc.perform(post(JOB_ENDPOINT_POST).contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        JobRunResponse response = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), JobRunResponse.class);
        assertEquals("STARTING", response.getJobStatus());
        assertNotNull(response.getJobId());
        assertNotNull(response.getJobExecutionId());
        jobExecutionId = response.getJobExecutionId();
    }

    @Then("the job succeeds within {int} seconds")
    public void waitForJobToComplete(int timeoutSeconds) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        long start = System.currentTimeMillis();
        JobRunResponse response = null;
        while (((System.currentTimeMillis() - start) / 1000 < timeoutSeconds) && (response == null || !BatchStatus.COMPLETED.name().equalsIgnoreCase(response.getJobStatus()))) {
            MvcResult result = mockMvc.perform(get(JOB_ENDPOINT_GET + "?executionId=" + jobExecutionId))
                    .andExpect(status().is2xxSuccessful())
                    .andReturn();
            response = objectMapper.readValue(result.getResponse().getContentAsString(), JobRunResponse.class);
            assertNotEquals("FAILED", response.getJobStatus());
            Thread.sleep(2000);
        }
        assertNotNull(response);
        assertNotNull(response.getJobStatus());
        assertEquals("Job did not complete on time.", BatchStatus.COMPLETED.name(), response.getJobStatus());
    }


    @And("^the data for ([^\"]*) updated in the database$")
    public void theDataForSPYUpdatedInTheDatabase(String ticker) {
        Set<HistoricalOption> options = historicOptionsDataService.findOptions(ticker);
        assertTrue(options.size() > 4000);
        HistoricalOption specfic = historicOptionsDataService.findOption("SPY", LocalDate.of(2019, 1, 2), 100.0, Option.OptionType.CALL);
        assertNotNull(specfic);
        assertEquals(1, specfic.getOptionPriceData().size());
        specfic = historicOptionsDataService.findOption("SPY" , LocalDate.of(2019,6, 21), 345.00, Option.OptionType.CALL);
        assertEquals(2, specfic.getOptionPriceData().size());
        specfic = historicOptionsDataService.findOption("SPY", LocalDate.of(2019, 2, 4),160.00, Option.OptionType.PUT);
        assertEquals(2, specfic.getOptionPriceData().size());
        assertNotNull(specfic.getOptionPriceData().stream().findFirst().get().getDataObtainedDate());
        assertNotNull(specfic.getOptionPriceData().stream().findFirst().get().getBid());
        assertNotNull(specfic.getOptionPriceData().stream().findFirst().get().getAsk());
        assertNotNull(specfic.getOptionPriceData().stream().findFirst().get().getTradeDate());
        assertNotNull(specfic.getOptionPriceData().stream().findFirst().get().getLastTradePrice());

        TrackedStock trackedStock = trackedStockService.findByTicker("SPY");
        assertEquals(LocalDate.of(2019, 1, 2), trackedStock.getOptionsHistoricDataStartDate());
    }

    @After
    public void cleanup() {
        trackedStocksRepository.deleteAll();
        historicalOptionRepository.deleteAll();
    }
}
