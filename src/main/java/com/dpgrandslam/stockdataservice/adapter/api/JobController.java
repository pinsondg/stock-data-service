package com.dpgrandslam.stockdataservice.adapter.api;

import com.dpgrandslam.stockdataservice.domain.model.JobRunRequest;
import com.dpgrandslam.stockdataservice.domain.model.JobRunResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/job")
public class JobController {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private List<Job> batchJobs;

    @Autowired
    private JobExplorer jobExplorer;

    @PostMapping("/run")
    public ResponseEntity<JobRunResponse> runOptionCSVLoadJob(@RequestBody JobRunRequest runRequest) throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        Map<String, JobParameter> jobParameterMap = runRequest.getJobParams().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, x -> new JobParameter(x.getValue())));
        Optional<Job> jobToRun = batchJobs.stream().filter(job -> job.getName().equals(runRequest.getJobName())).findFirst();
        if (jobToRun.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        log.info("Batch job {} started through API call.", runRequest.getJobName());
        JobExecution jobExecution = jobLauncher.run(jobToRun.get(), new JobParameters(jobParameterMap));
        JobRunResponse jobRunResponse = new JobRunResponse();
        jobRunResponse.setJobId(jobExecution.getJobId());
        jobRunResponse.setJobExecutionId(jobExecution.getId());
        jobRunResponse.setJobStatus(jobExecution.getStatus().name());
        return ResponseEntity.ok(jobRunResponse);
    }

    @GetMapping("/status")
    public ResponseEntity<JobRunResponse> getJobStatus(@RequestParam Long executionId) {
        JobRunResponse jobRunResponse = new JobRunResponse();
        JobExecution jobExecution = jobExplorer.getJobExecution(executionId);
        jobRunResponse.setJobStatus(jobExecution.getStatus().name());
        String message = jobExecution.getAllFailureExceptions().stream().findFirst().map(Throwable::getMessage).orElse(null);
        jobRunResponse.setMessage(message);
        jobRunResponse.setJobId(jobExecution.getJobId());
        jobRunResponse.setJobExecutionId(jobExecution.getId());
        return ResponseEntity.ok(jobRunResponse);
    }

}
