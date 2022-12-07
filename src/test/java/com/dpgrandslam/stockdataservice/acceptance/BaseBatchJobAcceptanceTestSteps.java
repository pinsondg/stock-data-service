package com.dpgrandslam.stockdataservice.acceptance;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.dpgrandslam.stockdataservice.domain.model.JobRunResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.batch.runtime.BatchStatus;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
public class BaseBatchJobAcceptanceTestSteps {

    private static final String JOB_ENDPOINT_POST = "/job/run";
    private static final String JOB_ENDPOINT_GET = "/job/status";

    private final @NotNull String testBucketName;
    private final @NotNull String testS3Key;
    private final @NotNull AmazonS3 amazonS3;
    private final @NotNull MockMvc mockMvc;
    private long jobExecutionId;

    public void checkTestFileExist() {
        List<S3ObjectSummary> objectSummaries = amazonS3.listObjectsV2(testBucketName, testS3Key)
                .getObjectSummaries();
        assertFalse("S3 Bucket does not contain test files.", objectSummaries.isEmpty());
    }

    public void triggerJob(String jobName) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> body = new HashMap<>();
        Map<String, String> jobParams = new HashMap<>();
        jobParams.put("bucket", testBucketName);
        jobParams.put("keyPrefix", testS3Key);
        body.put("jobName", jobName);
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

    public void waitForJobToComplete(int timeoutSeconds) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        long start = System.currentTimeMillis();
        JobRunResponse response = null;
        while (((System.currentTimeMillis() - start) / 1000 < timeoutSeconds) && (response == null || !BatchStatus.COMPLETED.name().equalsIgnoreCase(response.getJobStatus()))) {
            MvcResult result = mockMvc.perform(get(JOB_ENDPOINT_GET + "?executionId=" + jobExecutionId))
                    .andExpect(status().is2xxSuccessful())
                    .andReturn();
            response = objectMapper.readValue(result.getResponse().getContentAsString(), JobRunResponse.class);
            assertNotEquals("The batch job had failed. Reason: " + response.getMessage(), "FAILED", response.getJobStatus());
            Thread.sleep(1000);
        }
        assertNotNull(response);
        assertNotNull(response.getJobStatus());
        assertEquals("Job did not complete on time.", BatchStatus.COMPLETED.name(), response.getJobStatus());
    }

    public long getJobExecutionId() {
        return jobExecutionId;
    }
}
