package com.dpgrandslam.stockdataservice.domain.service;

import com.dpgrandslam.stockdataservice.domain.model.stock.YahooFinanceQuote;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class VIXLoadService {

    @Autowired
    @Qualifier("VIXCache")
    private Cache<Pair<LocalDate, LocalDate>, List<YahooFinanceQuote>> vixCache;

    @Autowired
    private YahooFinanceHistoricStockDataLoadService historicStockDataLoadService;

    public List<YahooFinanceQuote> loadVIXBetweenDates(LocalDate startDate, LocalDate endDate) {
        log.info("Loading VIX data for dates {} - {}", startDate, endDate);
        return vixCache.get(Pair.of(startDate, endDate), (pair) -> historicStockDataLoadService
                .loadQuoteForDates("^VIX", startDate, endDate));
    }
}
