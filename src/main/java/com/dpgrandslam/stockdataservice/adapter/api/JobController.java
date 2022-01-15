package com.dpgrandslam.stockdataservice.adapter.api;

import com.dpgrandslam.stockdataservice.domain.model.JobRunRequest;
import com.dpgrandslam.stockdataservice.domain.model.JobRunResponse;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/job")
public class JobController {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job optionCSVLoadJob;

    @PostMapping("/run")
    public ResponseEntity<JobRunResponse> runOptionCSVLoadJob(@RequestBody JobRunRequest runRequest) throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        Map<String, JobParameter> jobParameterMap = runRequest.getJobParams().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, x -> new JobParameter(x.getValue())));
        JobExecution jobExecution = jobLauncher.run(optionCSVLoadJob, new JobParameters(jobParameterMap));
        JobRunResponse jobRunResponse = new JobRunResponse();
        jobRunResponse.setJobId(jobExecution.getJobId());
        jobRunResponse.setJobExecutionId(jobExecution.getId());
        jobRunResponse.setJobStatus(jobExecution.getStatus().name());
        return ResponseEntity.ok(jobRunResponse);
    }

    @GetMapping("/status")
    public ResponseEntity<JobRunResponse> getJobStatus(@RequestParam String jobId, @RequestParam String executionId) {
        return null;
    }
}
