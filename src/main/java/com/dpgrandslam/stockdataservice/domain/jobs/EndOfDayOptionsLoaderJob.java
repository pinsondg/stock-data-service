package com.dpgrandslam.stockdataservice.domain.jobs;

import com.dpgrandslam.stockdataservice.domain.model.options.OptionsChain;
import com.dpgrandslam.stockdataservice.domain.model.stock.TrackedStock;
import com.dpgrandslam.stockdataservice.domain.service.HistoricOptionsDataService;
import com.dpgrandslam.stockdataservice.domain.service.OptionsChainLoadService;
import com.dpgrandslam.stockdataservice.domain.service.TrackedStockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

@Component
@Slf4j
public class EndOfDayOptionsLoaderJob {

    private static final int STEPS = 2;

    private ExecutorService executorService = Executors.newFixedThreadPool(STEPS);

    @Autowired
    private OptionsChainLoadService optionsChainLoadService;

    @Autowired
    private TrackedStockService trackedStockService;

    @Autowired
    private HistoricOptionsDataService historicOptionsDataService;

    private Queue<TrackedStock> trackedStocks;

    private boolean jobComplete;

    public EndOfDayOptionsLoaderJob() {
        this.jobComplete = false;
        this.trackedStocks = new ConcurrentLinkedQueue<>();
    }

    @PostConstruct
    private void init() {
        reset();
    }

    private void reset() {
        jobComplete = false;
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
        if (jobComplete) {
            log.info("Resetting data load job for next run.");
            reset();
        }
    }

    @Scheduled(cron = "0 * * * * 6", zone = "EST") // Every minute on Saturday
    public void weekendLoadJob() {
        storeOptionsChainEndOfDayData();
    }

    @Scheduled(cron = "0 * 0-9 * * 1-5", zone = "EST")
    public void weekdayLoadJobBeforeHours() {
        storeOptionsChainEndOfDayData();
    }

    @Scheduled(cron = "0 * 16-23 * * 1-5", zone = "EST") // Every minute from
    public void weekdayLoadJobAfterHours() {
        storeOptionsChainEndOfDayData();
    }

//    @Scheduled(cron = "5 * * * * 1-5", zone = "EST")
    private void storeOptionsChainEndOfDayData() {
        if (!jobComplete) {
            log.info("Starting data load job batch.");
            for (int i = 0; i < STEPS; i++) {
                if (!trackedStocks.isEmpty()) {
                    executorService.execute(() -> {
                        TrackedStock current = trackedStocks.poll();
                        if (current != null && !jobComplete) {
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

    private void completeJob() {
        if (!jobComplete) {
            log.info("Job finished.");
            jobComplete = true;
        }
    }
}
