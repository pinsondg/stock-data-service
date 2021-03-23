package com.dpgrandslam.stockdataservice.domain.jobs;

import com.dpgrandslam.stockdataservice.domain.event.TrackedStockAddedEvent;
import com.dpgrandslam.stockdataservice.domain.model.options.OptionsChain;
import com.dpgrandslam.stockdataservice.domain.model.stock.TrackedStock;
import com.dpgrandslam.stockdataservice.domain.service.HistoricOptionsDataService;
import com.dpgrandslam.stockdataservice.domain.service.OptionsChainLoadService;
import com.dpgrandslam.stockdataservice.domain.service.TrackedStockService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Component
@Slf4j
public class EndOfDayOptionsLoaderJob implements ApplicationListener<TrackedStockAddedEvent> {

    private static final int STEPS = 2;

    private ExecutorService executorService = Executors.newFixedThreadPool(STEPS);

    @Autowired
    private OptionsChainLoadService optionsChainLoadService;

    @Autowired
    private TrackedStockService trackedStockService;

    @Autowired
    private HistoricOptionsDataService historicOptionsDataService;

    private Queue<TrackedStock> trackedStocks;

    private JobStatus jobStatus;

    @Getter
    private enum JobStatus {
        NOT_STARTED(false), RUNNING_MANUAL(true), RUNNING_SCHEDULED(true), COMPLETE(true);

        boolean isRunning;

        private JobStatus(boolean isRunning) {
            this.isRunning = isRunning;
        }
    }

    public EndOfDayOptionsLoaderJob() {
        this.jobStatus = JobStatus.NOT_STARTED;
        this.trackedStocks = new ConcurrentLinkedQueue<>();
    }

    @PostConstruct
    private void init() {
        reset();
    }

    private void reset() {
        jobStatus = JobStatus.NOT_STARTED;
        trackedStocks = new ConcurrentLinkedQueue<>();
        trackedStocks.addAll(trackedStockService.getAllActiveTrackedStocks());
    }

//    @Scheduled(cron = "0 * * * * *", zone = "EST")
    @Scheduled(cron = "0 * 10-15 * * *", zone = "EST")
    public void weekdayReset() {
        resetJob();
    }

    @Scheduled(cron = "0 * 10-15 * * *", zone = "EST")
    public void weekendReset() {
        resetJob();
    }


    private void resetJob() {
        if (jobStatus == JobStatus.COMPLETE) {
            log.info("Resetting data load job for next run.");
            reset();
        }
    }

    @Scheduled(cron = "0 * * * * 6", zone = "EST") // Every minute on Saturday
    public void weekendLoadJob() {
        startJob();
        storeOptionsChainEndOfDayData();
    }

    @Scheduled(cron = "0 * 0-9 * * 1-5", zone = "EST")
    public void weekdayLoadJobBeforeHours() {
        startJob();
        storeOptionsChainEndOfDayData();
    }

    @Scheduled(cron = "0 * 16-23 * * 1-5", zone = "EST") // Every minute from
    public void weekdayLoadJobAfterHours() {
        startJob();
        storeOptionsChainEndOfDayData();
    }

//    @Scheduled(cron = "5 * * * * 1-5", zone = "EST")
    private void storeOptionsChainEndOfDayData() {
        if (jobStatus.isRunning()) {
            log.info("Starting data load job batch.");
            for (int i = 0; i < STEPS; i++) {
                if (!trackedStocks.isEmpty()) {
                    executorService.execute(() -> {
                        TrackedStock current = trackedStocks.poll();
                        if (current != null && jobStatus.isRunning() && current.isActive()) {
                            log.info("Executing update for {}", current);
                            try {
                                List<OptionsChain> fullOptionsChain = optionsChainLoadService
                                        .loadFullLiveOptionsChain(current.getTicker());
                                historicOptionsDataService.addFullOptionsChain(fullOptionsChain);
                                current.setLastOptionsHistoricDataUpdate(LocalDate.now(ZoneId.of("America/New_York")));
                                trackedStockService.updateOptionUpdatedTimestamp(current.getTicker());
                            } catch (Exception e) {
                                log.error("Failed to load options chain for tracked stock: {}. Putting back in queue for retry later.", current.getTicker(), e);
                                trackedStocks.add(current);
                            }
                        } else {
                            completeJob();
                        }
                    });
                } else {
                    completeJob();
                }
            }
        }
    }

    private void startJob() {
        if (jobStatus == JobStatus.NOT_STARTED) {
            jobStatus = JobStatus.RUNNING_SCHEDULED;
        }
    }

    private void completeJob() {
        if (jobStatus.isRunning()) {
            log.info("Job finished.");
            jobStatus = JobStatus.COMPLETE;
        }
    }

    @Override
    public void onApplicationEvent(TrackedStockAddedEvent trackedStockAddedEvent) {
        log.info("Adding newly added tracked stocks {} to the queue to run in the next job.", trackedStockAddedEvent.getTrackedStocks()
                .stream()
                .map(TrackedStock::getTicker)
                .collect(Collectors.toList()));
        trackedStocks.addAll(trackedStockAddedEvent.getTrackedStocks());
        if (jobStatus == JobStatus.COMPLETE) {
            log.info("Setting job status to running for newly added tickers.");
            jobStatus = JobStatus.RUNNING_MANUAL;
        }
    }
}
