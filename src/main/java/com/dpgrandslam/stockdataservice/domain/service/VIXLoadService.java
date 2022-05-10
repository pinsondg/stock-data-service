package com.dpgrandslam.stockdataservice.domain.service;

import com.dpgrandslam.stockdataservice.domain.model.stock.YahooFinanceQuote;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VIXLoadService {

    private final Cache<Pair<LocalDate, LocalDate>, List<YahooFinanceQuote>> vixCache;

    private final YahooFinanceHistoricStockDataLoadService historicStockDataLoadService;

    public List<YahooFinanceQuote> loadVIXBetweenDates(LocalDate startDate, LocalDate endDate) {
        log.info("Loading VIX data for dates {} - {}", startDate, endDate);
        return vixCache.get(Pair.of(startDate, endDate), (pair) -> {
                    // Break into 3 month chunks since yahoo finance is weird about long dates
                    List<YahooFinanceQuote> quotes = new ArrayList<>();
                    LocalDate sd = startDate;
                    LocalDate ed = sd.plusMonths(3);
                    while (sd.isBefore(endDate)) {
                        quotes.addAll(historicStockDataLoadService.loadQuoteForDates("^VIX", sd, ed).stream()
                                .filter(x -> x.getClose() != null)
                                .collect(Collectors.toList()));
                        sd = ed.plusDays(1);
                        ed = sd.plusMonths(3);
                    }
                    return quotes.stream().filter(x -> (x.getDate().isAfter(startDate) || x.getDate().equals(startDate))
                                    && (x.getDate().isBefore(endDate) || x.getDate().equals(endDate)))
                            .sorted(Comparator.comparing(YahooFinanceQuote::getDate))
                            .collect(Collectors.toList());
                }
        );
    }
}
