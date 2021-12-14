package com.dpgrandslam.stockdataservice.domain.jobs;

import com.dpgrandslam.stockdataservice.domain.model.FearGreedIndex;
import com.dpgrandslam.stockdataservice.domain.service.FearGreedDataLoadService;
import com.dpgrandslam.stockdataservice.domain.util.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

@Component
@Slf4j
public class EndOfDayFearGreedLoaderJob {

    @Autowired
    private FearGreedDataLoadService fearGreedDataLoadService;

    @Autowired
    private TimeUtils timeUtils;

    @Scheduled(cron = "0 0 17-23 * * MON-FRI")
    public void runJob() {
        LocalDate tradeDate = timeUtils.getCurrentOrLastTradeDate();
        Optional<FearGreedIndex> existing = fearGreedDataLoadService.getFearGreedIndexOfDay(tradeDate);
        if (existing.isEmpty()) {
            try {
                log.info("Loading fear greed data for day {}...", tradeDate);
                Set<FearGreedIndex> fearGreedIndices = fearGreedDataLoadService.loadCurrentFearGreedIndex();
                log.info("Found fear greed data for day {}: {}", tradeDate, fearGreedIndices);
                fearGreedDataLoadService.saveFearGreedData(fearGreedIndices);
                log.info("Saved fear greed data to the database. Job Complete!");
            } catch (Exception e) {
                log.error("Error loading fear greed data for day {}.", tradeDate, e);
            }
        } else {
            log.info("Existing fear greed data for day {} already exists... skipping", tradeDate);
        }
    }
}
