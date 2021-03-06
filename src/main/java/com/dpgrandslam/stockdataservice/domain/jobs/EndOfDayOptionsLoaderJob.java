package com.dpgrandslam.stockdataservice.domain.jobs;

import com.dpgrandslam.stockdataservice.domain.error.OptionsChainLoadException;
import com.dpgrandslam.stockdataservice.domain.event.OptionChainParseFailedEvent;
import com.dpgrandslam.stockdataservice.domain.event.TrackedStockAddedEvent;
import com.dpgrandslam.stockdataservice.domain.model.OptionPriceDataLoadRetry;
import com.dpgrandslam.stockdataservice.domain.model.options.OptionsChain;
import com.dpgrandslam.stockdataservice.domain.model.stock.TrackedStock;
import com.dpgrandslam.stockdataservice.domain.service.HistoricOptionsDataService;
import com.dpgrandslam.stockdataservice.domain.service.OptionPriceDataLoadRetryService;
import com.dpgrandslam.stockdataservice.domain.service.OptionsChainLoadService;
import com.dpgrandslam.stockdataservice.domain.service.TrackedStockService;
import com.dpgrandslam.stockdataservice.domain.util.TimeUtils;
import com.dpgrandslam.stockdataservice.domain.util.TimerUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

@Component
@Slf4j
public class EndOfDayOptionsLoaderJob {

    private static final int STEPS = 3;
    private static final int MAX_RETRY = 10;

    private static final int MAIN_JOB = 0;
    private static final int RETRY_JOB = 1;

    @Autowired
    private OptionsChainLoadService optionsChainLoadService;

    @Autowired
    private TrackedStockService trackedStockService;

    @Autowired
    private HistoricOptionsDataService historicOptionsDataService;

    @Autowired
    private TimeUtils timeUtils;

    @Autowired
    private OptionPriceDataLoadRetryService optionRetryService;

    private Queue<String> trackedStocks;

    private Map<String, Integer> failCountMap;

    private JobStatus mainJobStatus;
    private JobStatus retryJobStatus;

    @Getter
    public enum JobStatus {
        NOT_STARTED(false), RUNNING_MANUAL(true), RUNNING_SCHEDULED(true), COMPLETE(false), COMPLETE_WITH_FAILURES(false);

        boolean isRunning;

        JobStatus(boolean isRunning) {
            this.isRunning = isRunning;
        }
    }

    public EndOfDayOptionsLoaderJob() {
        this.mainJobStatus = JobStatus.NOT_STARTED;
        this.trackedStocks = new ConcurrentLinkedQueue<>();
        failCountMap = new HashMap<>();
    }

    @PostConstruct
    private void init() {
        reset();
    }

    private void reset() {
        mainJobStatus = JobStatus.NOT_STARTED;
        retryJobStatus = JobStatus.NOT_STARTED;
        trackedStocks = new ConcurrentLinkedQueue<>();
        // Put all tracked stock tickers into queue for processing by batch
        trackedStocks.addAll(trackedStockService.getAllTrackedStocks(true).stream()
                .filter(trackedStock -> trackedStock.getLastOptionsHistoricDataUpdate() == null || trackedStock.getLastOptionsHistoricDataUpdate().isBefore(timeUtils.getCurrentOrLastTradeDate()))
                .map(TrackedStock::getTicker)
                .collect(Collectors.toList()));
        failCountMap = new HashMap<>();
    }

    @Scheduled(cron = "0 * 10-15 * * *")
    public void weekdayReset() {
        resetJob();
    }

    @Scheduled(cron = "0 * 10-15 * * *")
    public void weekendReset() {
        resetJob();
    }


    private void resetJob() {
        if (mainJobStatus == JobStatus.COMPLETE || mainJobStatus == JobStatus.COMPLETE_WITH_FAILURES) {
            log.info("Resetting data load job for next run.");
            reset();
        }
    }

    @Scheduled(cron = "0 0/5 * * * SAT") // Every minute on Saturday
    public void weekendLoadJob() {
        startJob();
        storeOptionsChainEndOfDayData();
    }

    @Scheduled(cron = "0 0/5 0-9 * * MON-FRI")
    public void weekdayLoadJobBeforeHours() {
        startJob();
        storeOptionsChainEndOfDayData();
    }

    @Scheduled(cron = "0 0/5 16-23 * * MON-FRI") // Every minute from
    public void weekdayLoadJobAfterHours() {
        startJob();
        storeOptionsChainEndOfDayData();
    }

    @Scheduled(cron = "0 0 16-23 * * MON-FRI") // Run retry job every hour
    @Transactional
    public void runRetryBeforeMidnight() {
        retryQueueJob();
    }

    @Scheduled(cron = "0 0 0-9 * * MON-SAT") // Run retry job every hour
    @Transactional
    public void runRetryAfterMidnight() {
        retryQueueJob();
    }

    /**
     * A retry job that trys again for any failed reads.
     */
    private void retryQueueJob() {
        LocalDate tradeDate = timeUtils.getCurrentOrLastTradeDate();
        log.info("Getting options in retry table for trade date {}.", tradeDate);
        Set<OptionPriceDataLoadRetry> retrySet = optionRetryService.getAllWithTradeDate(tradeDate);
        log.info("Found {} options in retry table for trade date {}.", retrySet.size(),tradeDate);
        if (!retrySet.isEmpty()) {
            log.info("Starting retry job. Retry queue has {} options to retry.", retrySet.size());
            retryJobStatus = JobStatus.RUNNING_SCHEDULED;
            retrySet.forEach(failed -> {
                try {
                    TimerUtil timerUtil = new TimerUtil();
                    timerUtil.start();
                    log.info("Retrying for option with ticker {} and expiration {}.", failed.getOptionTicker(), failed.getOptionExpiration());

                    OptionsChain optionsChain = optionsChainLoadService.loadLiveOptionsChainForExpirationDate(failed.getOptionTicker(),
                            failed.getOptionExpiration());
                    historicOptionsDataService.addOptionsChain(optionsChain);

                    log.info("Took {} seconds to process retry for option with ticker {} and expiration {}.",
                            timerUtil.stop() / 1000.0, failed.getOptionTicker(), failed.getOptionExpiration());
                    optionRetryService.removeRetry(failed.getRetryId());
                } catch (OptionsChainLoadException e) {
                    log.error("Retry failed for option {}.", failed);
                    optionRetryService.updateRetryCount(failed);
                } finally {
                    try {
                        Thread.sleep(10000); // Sleep so we don't make too many calls at once
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        completeJob(RETRY_JOB);
    }

    /**
     * The main job that reads options end of day data from the options chain and stores it into
     * the database.
     */
    private void storeOptionsChainEndOfDayData() {
        if (mainJobStatus.isRunning() && !timeUtils.isTodayAmericaNewYorkStockMarketHoliday()) {
            log.info("Starting data load job batch.");
            for (int i = 0; i < STEPS; i++) {
                if (!trackedStocks.isEmpty()) {
                    // Get current status of tracked stock.
                    TrackedStock current = trackedStockService.findByTicker(trackedStocks.poll());
                    // If the current is null or not active or already updated today, do nothing.
                    if (current != null && mainJobStatus.isRunning() && current.isActive()
                            && (current.getLastOptionsHistoricDataUpdate() == null || !current.getLastOptionsHistoricDataUpdate().equals(timeUtils.getCurrentOrLastTradeDate()))) {
                        // Do options data load and put into database
                        log.info("Executing update for {}", current);
                        TimerUtil timerUtil = TimerUtil.startTimer();
                        try {
                            List<OptionsChain> fullOptionsChain = optionsChainLoadService
                                    .loadFullLiveOptionsChain(current.getTicker());
                            historicOptionsDataService.addFullOptionsChain(fullOptionsChain);
                            trackedStockService.updateOptionUpdatedTimestamp(current.getTicker());
                            failCountMap.remove(current.getTicker());
                            log.info("Options chain for {} processed successfully.", current.getTicker());
                            log.info("Took {} seconds to process options for {}", timerUtil.stop() / 1000.0, current.getTicker());
                        } catch (OptionsChainLoadException e) {
                            Integer failCount = failCountMap.putIfAbsent(current.getTicker(), 1);
                            if (failCount == null || failCount < MAX_RETRY) {
                                failCount = failCountMap.get(current.getTicker());
                                log.warn("Failed to load options chain for tracked stock: {}. Adding back to queue. Failed {} times", current.getTicker(), failCount, e);
                                trackedStocks.add(current.getTicker());
                                failCountMap.put(current.getTicker(), failCount + 1);
                            } else {
                                log.error("Failed to load options chain for tracked stock: {}. Failed > {} times and will not be added back to queue.", current.getTicker(), MAX_RETRY);
                                failCountMap.remove(current.getTicker());
                            }
                        }
                    }
                } else {
                    completeJob(MAIN_JOB);
                    break;
                }
            }
        } else if (timeUtils.isTodayAmericaNewYorkStockMarketHoliday()) {
            timeUtils.getStockMarketHolidays().stream()
                    .filter(d -> d.getDate().equals(timeUtils.getNowAmericaNewYork().toLocalDate()))
                    .findFirst()
                    .ifPresent((h) -> log.info("Not running job because today is a holiday ({})", h.getName()));
            completeJob(MAIN_JOB);
        }
    }

    private void startJob() {
        if (mainJobStatus == JobStatus.NOT_STARTED) {
            mainJobStatus = JobStatus.RUNNING_SCHEDULED;
        }
    }

    private void completeJob(int job) {
        if (job == RETRY_JOB && retryJobStatus.isRunning()) {
            if (optionRetryService.getAllWithTradeDate(timeUtils.getCurrentOrLastTradeDate()).isEmpty()) {
                retryJobStatus = JobStatus.COMPLETE;
            } else {
                retryJobStatus = JobStatus.COMPLETE_WITH_FAILURES;
            }
            log.info("Retry job finished with status: {}", retryJobStatus.name());
        } else if (job == MAIN_JOB && mainJobStatus.isRunning()) {
            log.info("Main job finished.");
            if (optionRetryService.getAllWithTradeDate(timeUtils.getCurrentOrLastTradeDate()).isEmpty()) {
                mainJobStatus = JobStatus.COMPLETE;
            } else {
                mainJobStatus = JobStatus.COMPLETE_WITH_FAILURES;
            }
            log.info("Main job finished with status: {}", mainJobStatus.name());
        }
        long optionsLoadedCount = historicOptionsDataService.countOptionsLoadedOnTradeDate(timeUtils.getCurrentOrLastTradeDate());
        log.info("Loaded {} options for date {}", optionsLoadedCount, timeUtils.getCurrentOrLastTradeDate());
    }

    @EventListener(TrackedStockAddedEvent.class)
    public void onApplicationEvent(TrackedStockAddedEvent trackedStockAddedEvent) {
        log.info("Adding newly added tracked stocks {} to the queue to run in the next job.", trackedStockAddedEvent.getTrackedStocks()
                .stream()
                .map(TrackedStock::getTicker)
                .collect(Collectors.toList()));
        trackedStocks.addAll(trackedStockAddedEvent.getTrackedStocks().stream().map(TrackedStock::getTicker).collect(Collectors.toSet()));
        if (mainJobStatus == JobStatus.COMPLETE || mainJobStatus == JobStatus.COMPLETE_WITH_FAILURES) {
            log.info("Setting job status to running for newly added tickers.");
            mainJobStatus = JobStatus.RUNNING_MANUAL;
        }
    }

    @EventListener({OptionChainParseFailedEvent.class})
    public void onApplicationEvent(OptionChainParseFailedEvent e) {
        if (mainJobStatus.isRunning() || mainJobStatus == JobStatus.COMPLETE_WITH_FAILURES) {
            optionRetryService.addOrUpdateRetry(e.getTicker(), e.getExpiration(), e.getTradeDate());
            log.info("Successfully added option with ticker [{}], expiration date [{}], and trade date [{}] to retry queue. " +
                    "There are now {} options in the retry queue.", e.getTicker(), e.getExpiration(), e.getTradeDate(), optionRetryService.getAllWithTradeDate(e.getTradeDate()).size());
        }
    }
}
