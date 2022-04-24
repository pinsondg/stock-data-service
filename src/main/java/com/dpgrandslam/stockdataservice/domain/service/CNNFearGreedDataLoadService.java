package com.dpgrandslam.stockdataservice.domain.service;

import com.dpgrandslam.stockdataservice.adapter.apiclient.CNNFearGreedClient;
import com.dpgrandslam.stockdataservice.adapter.apiclient.WebpageLoader;
import com.dpgrandslam.stockdataservice.adapter.repository.FearGreedIndexRepository;
import com.dpgrandslam.stockdataservice.domain.config.ApiClientConfigurationProperties;
import com.dpgrandslam.stockdataservice.domain.model.CNNFearGreedResponse;
import com.dpgrandslam.stockdataservice.domain.model.FearGreedIndex;
import com.dpgrandslam.stockdataservice.domain.util.TimeUtils;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class CNNFearGreedDataLoadService extends FearGreedDataLoadService {

    public static final String PATH = "/data/fear-and-greed/";
    private static final Pattern REGEX = Pattern.compile("^Fear & Greed (.*): ([0-9]*) \\(.*\\)$");

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
        CNNFearGreedResponse fearGreedResponse = cnnFearGreedClient.getFearGreedData(LocalDate.now());

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

        FearGreedIndex fearGreedIndexNow = new FearGreedIndex();
        fearGreedIndexNow.setValue(fearGreedResponse.getFearAndGreed().getScore().intValue());
        fearGreedIndexNow.setTradeDate(timeUtils.getCurrentOrLastTradeDate());
        res.add(fearGreedIndexNow);

        return res;
    }

    private Set<FearGreedIndex> parseDocument(Document document) {
        Set<FearGreedIndex> fearGreedIndices = document.body().selectFirst(".market-fng-gauge__historical").children().stream()
                .map(this::parseChildForFearGreed)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Element nowScore = document.body().selectFirst("market-fng-gauge__dial-number-value");
        FearGreedIndex fearGreedNow = new FearGreedIndex();
        fearGreedNow.setTradeDate(timeUtils.getCurrentOrLastTradeDate());
        fearGreedNow.setValue(Integer.parseInt(nowScore.text()));
        fearGreedIndices.add(fearGreedNow);
        return fearGreedIndices;
    }

    private FearGreedIndex parseChildForFearGreed(Element el) {
        FearGreedIndex fearGreedIndex = new FearGreedIndex();
        String type = el.children().get(0).text();
        switch (type.toLowerCase()) {
            case "previous close":
                fearGreedIndex.setTradeDate(timeUtils.getCurrentOrLastTradeDate(LocalDateTime.now().minusDays(1)));
                break;
            case "1 week ago":
                fearGreedIndex.setTradeDate(timeUtils.getCurrentOrLastTradeDate(LocalDateTime.now().minusWeeks(1)));
                break;
            case "1 month ago":
                fearGreedIndex.setTradeDate(timeUtils.getCurrentOrLastTradeDate(LocalDateTime.now().minusMonths(1)));
                break;
            case "1 year ago":
                fearGreedIndex.setTradeDate(timeUtils.getCurrentOrLastTradeDate(LocalDateTime.now().minusYears(1)));
                break;
            case "now":
                fearGreedIndex.setTradeDate(timeUtils.getCurrentOrLastTradeDate());
                break;
            default:
                return null;
        }
        String val = el.selectFirst(".market-fng-gauge__historical-item-index-value").text();
        fearGreedIndex.setValue(Integer.parseInt(val));
        return fearGreedIndex;
    }

    private FearGreedIndex parseValStringForIndex(String val) {
        FearGreedIndex fearGreedIndex = new FearGreedIndex();

        Matcher m = REGEX.matcher(val);
        if (m.matches()) {
            String time = m.group(1);
            switch (time) {
                case "Previous Close":
                    fearGreedIndex.setTradeDate(timeUtils.getCurrentOrLastTradeDate(LocalDateTime.now().minusDays(1)));
                    break;
                case "1 Week Ago":
                    fearGreedIndex.setTradeDate(timeUtils.getCurrentOrLastTradeDate(LocalDateTime.now().minusWeeks(1)));
                    break;
                case "1 Month Ago":
                    fearGreedIndex.setTradeDate(timeUtils.getCurrentOrLastTradeDate(LocalDateTime.now().minusMonths(1)));
                    break;
                case "1 Year Ago":
                    fearGreedIndex.setTradeDate(timeUtils.getCurrentOrLastTradeDate(LocalDateTime.now().minusYears(1)));
                    break;
                case "Now":
                    fearGreedIndex.setTradeDate(timeUtils.getCurrentOrLastTradeDate());
                    break;
            }
            Integer v = Integer.parseInt(m.group(2));
            fearGreedIndex.setValue(v);
            return fearGreedIndex;
        }
        return null;
    }
}
