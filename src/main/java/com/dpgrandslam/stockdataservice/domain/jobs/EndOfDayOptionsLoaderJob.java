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
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;

@Component
@Slf4j
public class EndOfDayOptionsLoaderJob {

    private static final int STEPS = 5;

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

    @Scheduled(cron = "0 * * * * *", zone = "EST")
//    @Scheduled(cron = "* 10-15 ? ? ?", zone = "EST")
    private void resetJob() {
        log.info("Started Restart");
        if (jobComplete) {
            log.info("Resetting data load job for next run.");
            reset();
        }
    }

//    @Scheduled(cron = "* 0-9,16-24 ? ? 1-5", zone = "EST")
    @Scheduled(cron = "5 * * * * 1-5", zone = "EST")
    public void storeOptionsChainEndOfDayData() {
        if (!jobComplete) {
            log.info("Starting data load job batch.");
            for (int i = 0; i < STEPS; i++) {
                if (!trackedStocks.isEmpty()) {
                    executorService.execute(() -> {
                        TrackedStock current = trackedStocks.poll();
                        log.info("Executing update for {}", current);
                        if (current != null) {
                            List<OptionsChain> fullOptionsChain = optionsChainLoadService
                                    .loadFullLiveOptionsChain(current.getTicker());
                            historicOptionsDataService.addFullOptionsChain(fullOptionsChain);
                            current.setLastOptionsHistoricDataUpdate(LocalDate.now());
                            trackedStockService.updateOptionUpdatedTimestamp(current.getTicker());
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
