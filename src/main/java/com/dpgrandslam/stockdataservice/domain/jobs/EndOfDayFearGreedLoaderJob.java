package com.dpgrandslam.stockdataservice.domain.jobs;

import com.dpgrandslam.stockdataservice.domain.model.FearGreedIndex;
import com.dpgrandslam.stockdataservice.domain.service.FearGreedDataLoadService;
import com.dpgrandslam.stockdataservice.domain.util.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class EndOfDayFearGreedLoaderJob {

    @Autowired
    @Qualifier("CNNFearGreedDataLoadAPIService")
    private FearGreedDataLoadService fearGreedDataLoadService;

    @Autowired
    private TimeUtils timeUtils;

    @Scheduled(cron = "50 59 15-23 * * MON-FRI")
    public void runJob() {
        LocalDate tradeDate = timeUtils.getCurrentOrLastTradeDate();
        Optional<FearGreedIndex> existing = fearGreedDataLoadService.getFearGreedIndexOfDay(tradeDate);
        if (existing.isEmpty() && !timeUtils.isStockMarketHoliday(tradeDate)) {
            try {
                log.info("Loading fear greed data for day {}...", tradeDate);
                Set<FearGreedIndex> fearGreedIndices = fearGreedDataLoadService.loadCurrentFearGreedIndex().stream()
                        .filter(x -> !timeUtils.isStockMarketHoliday(x.getTradeDate()) && fearGreedDataLoadService.getFearGreedIndexOfDay(x.getTradeDate()).isEmpty())
                        .collect(Collectors.toSet());
                log.info("Found fear greed data for day {}: {}", tradeDate, fearGreedIndices);
                fearGreedDataLoadService.saveFearGreedData(fearGreedIndices);
                log.info("Saved fear greed data to the database. Job Complete!");
            } catch (Exception e) {
                log.error("Error loading fear greed data for day {}.", tradeDate, e);
            }
        } else if (existing.isEmpty()) {
            log.info("Existing fear greed data for day {} already exists... skipping", tradeDate);
        } else if (!timeUtils.isStockMarketHoliday(tradeDate)) {
            log.info("Today is stock market holiday, no fear greed data will be loaded.");
        }

    }
}
