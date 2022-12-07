package com.dpgrandslam.stockdataservice.domain.service;

import com.dpgrandslam.stockdataservice.adapter.apiclient.WebpageLoader;
import com.dpgrandslam.stockdataservice.adapter.repository.FearGreedIndexRepository;
import com.dpgrandslam.stockdataservice.domain.config.ApiClientConfigurationProperties;
import com.dpgrandslam.stockdataservice.domain.model.FearGreedIndex;
import com.dpgrandslam.stockdataservice.domain.util.TimeUtils;
import com.github.benmanes.caffeine.cache.Cache;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
public class CNNFearGreedDataLoadWebService extends FearGreedDataLoadService {

    private final WebpageLoader webpageLoader;
    private final TimeUtils timeUtils;
    private final ApiClientConfigurationProperties apiClientConfigurationProperties;


    public CNNFearGreedDataLoadWebService(FearGreedIndexRepository fearGreedIndexRepository,
                                          Cache<Pair<LocalDate, LocalDate>, List<FearGreedIndex>> fearGreedBetweenDatesCache,
                                          WebpageLoader webpageLoader,
                                          TimeUtils timeUtils,
                                          @Qualifier("CNNClientConfigurationProperties") ApiClientConfigurationProperties apiClientConfigurationProperties) {
        super(fearGreedIndexRepository, fearGreedBetweenDatesCache);
        this.webpageLoader = webpageLoader;
        this.timeUtils = timeUtils;
        this.apiClientConfigurationProperties = apiClientConfigurationProperties;
    }

    @Override
    public Set<FearGreedIndex> loadCurrentFearGreedIndex() {
        return null;
    }

}
