package com.dpgrandslam.stockdataservice.domain.service;

import com.dpgrandslam.stockdataservice.adapter.apiclient.BasicWebPageLoader;
import com.dpgrandslam.stockdataservice.domain.model.FearGreedIndex;
import com.dpgrandslam.stockdataservice.domain.util.TimeUtils;
import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CNNFearGreedIndexDataLoadService implements FearGreedIndexDataLoadService {

    public static final String URL = "https://money.cnn.com/data/fear-and-greed/";
    private static final Pattern REGEX = Pattern.compile("^Fear & Greed (.*): ([0-9]*) \\(.*\\)$");

    private final BasicWebPageLoader basicWebPageLoader;
    private final TimeUtils timeUtils;


    @Override
    public Set<FearGreedIndex> loadCurrentFearGreedIndex() {
        return parseDocument(basicWebPageLoader.parseUrl(URL));
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
                    if (timeUtils.getNowAmericaNewYork().isAfter(LocalDateTime.of(LocalDate.now(), LocalTime.of(4, 30)))) {
                        fearGreedIndex.setTradeDate(timeUtils.getLastTradeDate());
                    } else {
                        fearGreedIndex.setTradeDate(LocalDate.now().minusDays(1));
                    }
                    break;
                case "1 Week Ago":
                    fearGreedIndex.setTradeDate(LocalDate.now().minusWeeks(1));
                    break;
                case "1 Month Ago":
                    fearGreedIndex.setTradeDate(LocalDate.now().minusMonths(1));
                    break;
                case "1 Year Ago":
                    fearGreedIndex.setTradeDate(LocalDate.now().minusYears(1));
                    break;
                case "Now":
                    return null;
            }
            Integer v = Integer.parseInt(m.group(2));
            fearGreedIndex.setValue(v);
            return fearGreedIndex;
        }
        return null;
    }
}
