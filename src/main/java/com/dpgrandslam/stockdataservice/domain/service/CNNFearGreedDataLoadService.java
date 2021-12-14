package com.dpgrandslam.stockdataservice.domain.service;

import com.dpgrandslam.stockdataservice.adapter.apiclient.WebpageLoader;
import com.dpgrandslam.stockdataservice.adapter.repository.FearGreedIndexRepository;
import com.dpgrandslam.stockdataservice.domain.model.FearGreedIndex;
import com.dpgrandslam.stockdataservice.domain.util.TimeUtils;
import com.github.benmanes.caffeine.cache.Cache;
import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class CNNFearGreedDataLoadService extends FearGreedDataLoadService {

    public static final String URL = "https://money.cnn.com/data/fear-and-greed/";
    private static final Pattern REGEX = Pattern.compile("^Fear & Greed (.*): ([0-9]*) \\(.*\\)$");

    private final WebpageLoader webpageLoader;
    private final TimeUtils timeUtils;

    public CNNFearGreedDataLoadService(FearGreedIndexRepository fearGreedIndexRepository, Cache<Pair<LocalDate, LocalDate>, List<FearGreedIndex>> fearGreedCache,  WebpageLoader webpageLoader, TimeUtils timeUtils) {
        super(fearGreedIndexRepository, fearGreedCache);
        this.webpageLoader = webpageLoader;
        this.timeUtils = timeUtils;
    }


    @Override
    public Set<FearGreedIndex> loadCurrentFearGreedIndex() {
        return parseDocument(webpageLoader.parseUrl(URL));
    }

    private Set<FearGreedIndex> parseDocument(Document document) {
        return document.body().selectFirst("#needleChart").selectFirst("ul").children().stream()
                .map(x -> parseValStringForIndex(x.text()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
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
