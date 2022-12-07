package com.dpgrandslam.stockdataservice.domain.jobs.feargreedbatch;

import com.dpgrandslam.stockdataservice.domain.model.FearGreedIndex;
import com.dpgrandslam.stockdataservice.domain.service.FearGreedDataLoadService;
import com.dpgrandslam.stockdataservice.domain.util.TimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class FearGreedJSONItemProcessor implements ItemProcessor<FearGreedJSONFile, Set<FearGreedIndex>> {

    @Autowired
    @Qualifier("CNNFearGreedDataLoadAPIService")
    private FearGreedDataLoadService fearGreedDataLoadService;

    @Autowired
    private TimeUtils timeUtils;

    @Override
    public Set<FearGreedIndex> process(FearGreedJSONFile fearGreedJSONFile) throws Exception {
        Set<FearGreedIndex> fearGreedIndices = new HashSet<>();
        fearGreedJSONFile.getData().forEach(data -> {
            Instant instant = Instant.ofEpochMilli(data.getTimestamp().longValue());
            LocalDate tradeDate = LocalDate.ofInstant(instant, ZoneOffset.UTC);
            if (fearGreedDataLoadService.getFearGreedIndexOfDay(tradeDate).isEmpty() && timeUtils.isTradingOpenOnDay(tradeDate)) {
                FearGreedIndex fearGreedIndex = new FearGreedIndex();
                fearGreedIndex.setTradeDate(tradeDate);
                fearGreedIndex.setValue(data.getValue().intValue());
                fearGreedIndices.add(fearGreedIndex);
            }
        });
        Set<FearGreedIndex> filteredFearGreedIndices = fearGreedIndices.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        log.info("Processed {} fear-greed record successfully.", filteredFearGreedIndices.size());
        return filteredFearGreedIndices;
    }
}
