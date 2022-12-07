package com.dpgrandslam.stockdataservice.domain.jobs.optioncsv;

import com.dpgrandslam.stockdataservice.domain.model.options.HistoricalOption;
import com.dpgrandslam.stockdataservice.domain.service.HistoricOptionsDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class OptionCSVItemWriter implements ItemWriter<HistoricalOption> {

    private final HistoricOptionsDataService dataService;

    @Override
    public void write(List<? extends HistoricalOption> list) throws Exception {
        try {
            dataService.saveOptions(list.stream().map(x -> (HistoricalOption) x).collect(Collectors.toList()));
        } catch (Exception e) {
            log.error("Failed to update options in DB.", e);
            throw e;
        }
    }
}
