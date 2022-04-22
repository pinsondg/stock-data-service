package com.dpgrandslam.stockdataservice.domain.service;

import com.dpgrandslam.stockdataservice.domain.model.stock.YahooFinanceQuote;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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
        return vixCache.get(Pair.of(startDate, endDate), (pair) -> historicStockDataLoadService
                .loadQuoteForDates("^VIX", pair.getLeft(), pair.getRight()).stream()
                .filter(x -> x.getClose() != null)
                .collect(Collectors.toList()));
    }
}
