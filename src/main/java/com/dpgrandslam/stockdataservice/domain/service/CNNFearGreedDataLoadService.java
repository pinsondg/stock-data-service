package com.dpgrandslam.stockdataservice.domain.service;

import com.dpgrandslam.stockdataservice.adapter.apiclient.CNNFearGreedClient;
import com.dpgrandslam.stockdataservice.adapter.repository.FearGreedIndexRepository;
import com.dpgrandslam.stockdataservice.domain.model.CNNFearGreedResponse;
import com.dpgrandslam.stockdataservice.domain.model.FearGreedIndex;
import com.dpgrandslam.stockdataservice.domain.util.TimeUtils;
import com.github.benmanes.caffeine.cache.Cache;
import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class CNNFearGreedDataLoadService extends FearGreedDataLoadService {

    public static final String PATH = "/data/fear-and-greed/";

    private final TimeUtils timeUtils;
    private final CNNFearGreedClient cnnFearGreedClient;

    public CNNFearGreedDataLoadService(FearGreedIndexRepository fearGreedIndexRepository,
                                       Cache<Pair<LocalDate, LocalDate>, List<FearGreedIndex>> fearGreedCache,
                                       CNNFearGreedClient cnnFearGreedClient,
                                       TimeUtils timeUtils) {
        super(fearGreedIndexRepository, fearGreedCache);
        this.cnnFearGreedClient = cnnFearGreedClient;
        this.timeUtils = timeUtils;
    }


    @Override
    public Set<FearGreedIndex> loadCurrentFearGreedIndex() {
        Set<FearGreedIndex> res = new HashSet<>();
        CNNFearGreedResponse fearGreedResponse = cnnFearGreedClient.getFearGreedData(LocalDate.now().toString());

        FearGreedIndex fearGreedIndexOneWeek = new FearGreedIndex();
        fearGreedIndexOneWeek.setValue(fearGreedResponse.getFearAndGreed().getPreviousOneWeek().intValue());
        fearGreedIndexOneWeek.setTradeDate(timeUtils.getCurrentOrLastTradeDate(LocalDateTime.now().minusWeeks(1)));
        res.add(fearGreedIndexOneWeek);

        FearGreedIndex fearGreedIndexOneMonth = new FearGreedIndex();
        fearGreedIndexOneMonth.setValue(fearGreedResponse.getFearAndGreed().getPreviousOneMonth().intValue());
        fearGreedIndexOneMonth.setTradeDate(timeUtils.getCurrentOrLastTradeDate(LocalDateTime.now().minusMonths(1)));
        res.add(fearGreedIndexOneMonth);

        FearGreedIndex fearGreedIndexOneYear = new FearGreedIndex();
        fearGreedIndexOneYear.setValue(fearGreedResponse.getFearAndGreed().getPreviousOneYear().intValue());
        fearGreedIndexOneYear.setTradeDate(timeUtils.getCurrentOrLastTradeDate(LocalDateTime.now().minusYears(1)));
        res.add(fearGreedIndexOneYear);

        FearGreedIndex fearGreedIndexPreviousClose = new FearGreedIndex();
        fearGreedIndexPreviousClose.setValue(fearGreedResponse.getFearAndGreed().getPreviousClose().intValue());
        fearGreedIndexPreviousClose.setTradeDate(timeUtils.getCurrentOrLastTradeDate(LocalDateTime.now().minusDays(1)));
        res.add(fearGreedIndexPreviousClose);

        FearGreedIndex fearGreedIndexNow = new FearGreedIndex();
        fearGreedIndexNow.setValue(fearGreedResponse.getFearAndGreed().getScore().intValue());
        fearGreedIndexNow.setTradeDate(timeUtils.getCurrentOrLastTradeDate());
        res.add(fearGreedIndexNow);

        return res;
    }

}
