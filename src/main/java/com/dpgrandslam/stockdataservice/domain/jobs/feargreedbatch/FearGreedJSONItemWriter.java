package com.dpgrandslam.stockdataservice.domain.jobs.feargreedbatch;

import com.dpgrandslam.stockdataservice.domain.model.FearGreedIndex;
import com.dpgrandslam.stockdataservice.domain.service.FearGreedDataLoadService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class FearGreedJSONItemWriter implements ItemWriter<Set<FearGreedIndex>> {

    @Autowired
    @Qualifier("CNNFearGreedDataLoadAPIService")
    private FearGreedDataLoadService fearGreedDataLoadService;

    @Override
    public void write(List<? extends Set<FearGreedIndex>> list) throws Exception {
        list.forEach(fearGreedDataLoadService::saveFearGreedData);
    }
}
