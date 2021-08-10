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
import java.time.ZoneId;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

@Component
@Slf4j
public class EndOfDayOptionsLoaderJob {

    private static final int STEPS = 2;

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

    private Queue<TrackedStock> trackedStocks;

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
        if (jobStatus == JobStatus.COMPLETE || jobStatus == JobStatus.COMPLETE_WITH_FAILURES) {
            log.info("Resetting data load job for next run.");
            reset();
        }
    }

    @Scheduled(cron = "0 * * * * SAT") // Every minute on Saturday
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

    private void retryQueueJob() {
        LocalDate tradeDate = timeUtils.getLastTradeDate();
        log.info("Getting options in retry table for trade date {}.", tradeDate);
        Set<OptionPriceDataLoadRetry> retrySet = optionRetryService.getAllWithTradeDate(tradeDate);
        log.info("Found {} options in retry table for trade date {}.", retrySet.size(),tradeDate);
        if (!retrySet.isEmpty()) {
            log.info("Starting retry job. Retry queue has {} options to retry.", retrySet.size());
            jobStatus = JobStatus.RETRY;
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
                        TimerUtil timerUtil = new TimerUtil();
                        timerUtil.start();
                        try {
                            List<OptionsChain> fullOptionsChain = optionsChainLoadService
                                    .loadFullLiveOptionsChain(current.getTicker());
                            historicOptionsDataService.addFullOptionsChain(fullOptionsChain);
                            trackedStockService.updateOptionUpdatedTimestamp(current.getTicker());
                            log.info("Options chain for {} processed successfully.", current.getTicker());
                            log.info("Took {} seconds to process options for {}", timerUtil.stop() / 1000.0, current.getTicker());
                        } catch (OptionsChainLoadException e) {
                            log.error("Failed to load options chain for tracked stock: {}.", current.getTicker(), e);
                        }
                    } else if (current != null && !current.isActive() && !current.getLastOptionsHistoricDataUpdate().equals(timeUtils.getNowAmericaNewYork().toLocalDate())){
                        completeJob(MAIN_JOB);
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
        if (jobStatus == JobStatus.NOT_STARTED) {
            jobStatus = JobStatus.RUNNING_SCHEDULED;
        }
    }

    private void completeJob(int job) {
        if (job == RETRY_JOB && jobStatus == JobStatus.RETRY) {
            log.info("Retry Job Finished.");
            setCompleteJobStatus();
        } else if (job == MAIN_JOB && jobStatus.isRunning()) {
            log.info("Job finished.");
            setCompleteJobStatus();
        }
    }

    private void setCompleteJobStatus() {
        if (optionRetryService.getAllWithTradeDate(timeUtils.getLastTradeDate()).isEmpty()) {
            log.info("Job finished with no failures.");
            jobStatus = JobStatus.COMPLETE;
        } else {
            log.info("Job finished with failures.");
            jobStatus = JobStatus.COMPLETE_WITH_FAILURES;
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
            optionRetryService.addOrUpdateRetry(e.getTicker(), e.getExpiration(), e.getTradeDate());
            log.info("Successfully added option with ticker [{}], expiration date [{}], and trade date [{}] to retry queue. " +
                    "There are now {} options in the retry queue.", e.getTicker(), e.getExpiration(), e.getTradeDate(), optionRetryService.getAllWithTradeDate(e.getTradeDate()).size());
        }
    }
}
