package com.dpgrandslam.stockdataservice.acceptance;

import com.amazonaws.services.s3.AmazonS3;
import com.dpgrandslam.stockdataservice.adapter.repository.FearGreedIndexRepository;
import com.dpgrandslam.stockdataservice.domain.config.FearGreedJSONLoadJobConfig;
import com.dpgrandslam.stockdataservice.domain.model.FearGreedIndex;
import com.dpgrandslam.stockdataservice.domain.service.FearGreedDataLoadService;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.After;
import org.junit.Ignore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDate;
import java.util.List;

import static junit.framework.TestCase.*;

@Ignore
public class FearGreedJSONBatchLoadAcceptanceTestSteps extends BaseAcceptanceTestSteps {

    @Value("${job.fear-greed-json.test-bucket-name}")
    private String testBucketName;

    @Value("${job.fear-greed-json.test-key}")
    private String testBucketKey;

    @Autowired
    private AmazonS3 amazonS3;

    @Autowired
    @Qualifier("CNNFearGreedDataLoadAPIService")
    private FearGreedDataLoadService fearGreedDataLoadService;

    @Autowired
    private FearGreedIndexRepository fearGreedIndexRepository;

    private BaseBatchJobAcceptanceTestSteps baseBatchJobAcceptanceTestSteps;

    @Before
    public void setup() {
        baseBatchJobAcceptanceTestSteps = new BaseBatchJobAcceptanceTestSteps(testBucketName, testBucketKey, amazonS3, mockMvc);
    }

    @And("a fear-greed test file exists in S3")
    public void checkFileExists() {
        baseBatchJobAcceptanceTestSteps.checkTestFileExist();
    }

    @When("the fear-greed job is triggered through the API")
    public void triggerJob() throws Exception {
        baseBatchJobAcceptanceTestSteps.triggerJob(FearGreedJSONLoadJobConfig.JOB_NAME);
    }

    @Then("the fear-greed job succeeds within {int} seconds")
    public void waitForJobToComplete(int seconds) throws Exception {
        baseBatchJobAcceptanceTestSteps.waitForJobToComplete(seconds);
    }

    @Given("fear-greed data exists in DB")
    public void fearGreedDataExistsInDB() {
        FearGreedIndex fearGreedIndex = new FearGreedIndex();
        fearGreedIndex.setValue(50);
        fearGreedIndex.setTradeDate(LocalDate.of(2022, 5, 17));

        fearGreedDataLoadService.saveFearGreedData(fearGreedIndex);
    }


    @And("the fear-greed data is updated in the database")
    public void theFearGreedDataIsUpdatedInTheDatabase() {
        List<FearGreedIndex> fearGreedIndices = fearGreedIndexRepository.findAll();
        assertTrue(fearGreedIndices.size() > 1);
        fearGreedIndices.forEach(index -> {
            assertNotNull(index.getTradeDate());
            assertNotNull(index.getId());
            assertNotNull(index.getValue());
            assertNotNull(index.getCreateTime());
        });
        assertEquals(432, fearGreedIndices.size());
    }

    @After
    public void cleanup() {
        fearGreedIndexRepository.deleteAll();
    }
}
