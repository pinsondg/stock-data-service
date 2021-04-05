package com.dpgrandslam.stockdataservice.domain.jobs;

import com.dpgrandslam.stockdataservice.domain.error.OptionsChainLoadException;
import com.dpgrandslam.stockdataservice.domain.event.OptionChainParseFailedEvent;
import com.dpgrandslam.stockdataservice.domain.event.TrackedStockAddedEvent;
import com.dpgrandslam.stockdataservice.domain.model.options.OptionsChain;
import com.dpgrandslam.stockdataservice.domain.model.stock.TrackedStock;
import com.dpgrandslam.stockdataservice.domain.service.HistoricOptionsDataService;
import com.dpgrandslam.stockdataservice.domain.service.OptionsChainLoadService;
import com.dpgrandslam.stockdataservice.domain.service.TrackedStockService;
import com.dpgrandslam.stockdataservice.domain.util.TimeUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Component
@Slf4j
public class EndOfDayOptionsLoaderJob {

    private static final int STEPS = 2;

    @Autowired
    private OptionsChainLoadService optionsChainLoadService;

    @Autowired
    private TrackedStockService trackedStockService;

    @Autowired
    private HistoricOptionsDataService historicOptionsDataService;

    @Autowired
    private TimeUtils timeUtils;

    private Queue<TrackedStock> trackedStocks;

    private Queue<Pair<String, LocalDate>> retryQueue;

    private JobStatus jobStatus;

    @Getter
    public enum JobStatus {
        NOT_STARTED(false), RUNNING_MANUAL(true), RUNNING_SCHEDULED(true), COMPLETE(false), COMPLETE_WITH_FAILURES(false), RETRY(true);

        boolean isRunning;

        private JobStatus(boolean isRunning) {
            this.isRunning = isRunning;
        }
    }

    public EndOfDayOptionsLoaderJob() {
        this.jobStatus = JobStatus.NOT_STARTED;
        this.trackedStocks = new ConcurrentLinkedQueue<>();
        this.retryQueue = new ConcurrentLinkedQueue<>();
    }

    @PostConstruct
    private void init() {
        reset();
    }

    private void reset() {
        jobStatus = JobStatus.NOT_STARTED;
        trackedStocks = new ConcurrentLinkedQueue<>();
        trackedStocks.addAll(trackedStockService.getAllTrackedStocks(true).stream()
                .filter(trackedStock -> trackedStock.getLastOptionsHistoricDataUpdate() == null || trackedStock.getLastOptionsHistoricDataUpdate().isBefore(timeUtils.getNowAmericaNewYork().toLocalDate()))
                .collect(Collectors.toList()));
        retryQueue = new ConcurrentLinkedQueue<>();
    }

//    @Scheduled(cron = "0 * * * * *", zone = "EST")
    @Scheduled(cron = "0 * 10-15 * * *")
    public void weekdayReset() {
        resetJob();
    }

    @Scheduled(cron = "0 * 10-15 * * *")
    public void weekendReset() {
        resetJob();
    }


    private void resetJob() {
        if (jobStatus == JobStatus.COMPLETE) {
            log.info("Resetting data load job for next run.");
            reset();
        }
    }

    @Scheduled(cron = "0 * * * * 6") // Every minute on Saturday
    public void weekendLoadJob() {
        startJob();
        storeOptionsChainEndOfDayData();
    }

    @Scheduled(cron = "0 0/5 0-9 * * 1-5")
    public void weekdayLoadJobBeforeHours() {
        startJob();
        storeOptionsChainEndOfDayData();
    }

    @Scheduled(cron = "0 0/5 16-23 * * 1-5") // Every minute from
    public void weekdayLoadJobAfterHours() {
        startJob();
        storeOptionsChainEndOfDayData();
    }

    @Scheduled(cron = "0 0 16-23 * * 1-6") // Run retry job every hour
    public void retryQueueJob() {
        // Copy retry queue so subsequent failures do not get added back
        List<Pair<String, LocalDate>> retryQueueCopy = new ArrayList<>(retryQueue);
        if (jobStatus == JobStatus.COMPLETE_WITH_FAILURES && !retryQueueCopy.isEmpty()) {
            log.info("Starting retry job. Retry queue has {} options to retry.", retryQueueCopy.size());
            jobStatus = JobStatus.RETRY;
            retryQueueCopy.forEach(failed -> {
                retryQueue.remove(failed);
                try {
                    long start = System.currentTimeMillis();
                    log.info("Retrying for option with ticker {} and expiration {}.", failed.getFirst(), failed.getSecond());

                    OptionsChain optionsChain = optionsChainLoadService.loadLiveOptionsChainForExpirationDate(failed.getFirst(),
                            failed.getSecond());
                    historicOptionsDataService.addOptionsChain(optionsChain);

                    log.info("Took {} seconds to process retry for option with ticker {} and expiration {}.",
                            (System.currentTimeMillis() - start) / 1000.0, failed.getFirst(), failed.getSecond());

                } catch (OptionsChainLoadException e) {
                    log.error("Retry failed for option {}. Adding back to queue.", failed);
                    retryQueue.add(failed);
                } finally {
                    try {
                        Thread.sleep(10000); // Sleep so we don't make too many calls at once
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        completeJob();
    }

//    @Scheduled(cron = "5 * * * * 1-5", zone = "EST")
    private void storeOptionsChainEndOfDayData() {
        if (jobStatus.isRunning() && !timeUtils.isTodayAmericaNewYorkStockMarketHoliday()) {
            log.info("Starting data load job batch.");
            for (int i = 0; i < STEPS; i++) {
                if (!trackedStocks.isEmpty()) {
                    TrackedStock current = trackedStocks.poll();
                    if (current != null && jobStatus.isRunning() && current.isActive()
                            && (current.getLastOptionsHistoricDataUpdate() == null || !current.getLastOptionsHistoricDataUpdate().equals(timeUtils.getNowAmericaNewYork().toLocalDate()))) {
                        log.info("Executing update for {}", current);
                        long start = System.currentTimeMillis();
                        try {
                            List<OptionsChain> fullOptionsChain = optionsChainLoadService
                                    .loadFullLiveOptionsChain(current.getTicker());
                            historicOptionsDataService.addFullOptionsChain(fullOptionsChain);
                            current.setLastOptionsHistoricDataUpdate(LocalDate.now(ZoneId.of("America/New_York")));
                            trackedStockService.updateOptionUpdatedTimestamp(current.getTicker());
                            log.info("Options chain for {} processed successfully.", current.getTicker());
                            log.info("Took {} seconds to process options for {}", (System.currentTimeMillis() - start) / 1000.0, current.getTicker());
                        } catch (OptionsChainLoadException e) {
                            log.error("Failed to load options chain for tracked stock: {}. Putting back in queue for retry later.", current.getTicker(), e);
                            trackedStocks.add(current);
                        }
                    } else if (current != null && !current.isActive() && !current.getLastOptionsHistoricDataUpdate().equals(timeUtils.getNowAmericaNewYork().toLocalDate())){
                        completeJob();
                    }
                } else {
                    completeJob();
                    break;
                }
            }
        } else if (timeUtils.isTodayAmericaNewYorkStockMarketHoliday()) {
            timeUtils.getStockMarketHolidays().stream()
                    .filter(d -> d.getDate().equals(timeUtils.getNowAmericaNewYork().toLocalDate()))
                    .findFirst()
                    .ifPresent((h) -> log.info("Not running job because today is a holiday ({})", h.getName()));
            completeJob();
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
            if (retryQueue.isEmpty()) {
                jobStatus = JobStatus.COMPLETE;
            } else {
                jobStatus = JobStatus.COMPLETE_WITH_FAILURES;
            }
        }
    }

    @EventListener(TrackedStockAddedEvent.class)
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

    @EventListener({OptionChainParseFailedEvent.class})
    public void onApplicationEvent(OptionChainParseFailedEvent e) {
        if (jobStatus.isRunning() || jobStatus == JobStatus.COMPLETE_WITH_FAILURES) {
            log.info("Adding option with ticker {} and expiration date {} to retry queue. There are now {}" +
                    "options in the retry queue.", e.getTicker(), e.getExpiration(), retryQueue.size());
            retryQueue.add(Pair.of(e.getTicker(), e.getExpiration()));
        }
    }
}
