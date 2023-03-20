package com.dpgrandslam.stockdataservice.domain.service;

import com.dpgrandslam.stockdataservice.domain.error.YahooFinanceQuoteLoadException;
import com.dpgrandslam.stockdataservice.domain.model.stock.YahooFinanceQuote;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TenYearTreasuryYieldService {

    @Autowired
    private YahooFinanceHistoricStockDataLoadService historicStockDataLoadService;

    @Autowired
    @Qualifier("TreasuryYieldCache")
    private Cache<Pair<LocalDate, LocalDate>, List<YahooFinanceQuote>> treasuryYieldCache;

    public List<YahooFinanceQuote> getTreasuryYieldForDate(LocalDate startDate, LocalDate endDate) {
        final String ticker = "^TNX";
        return treasuryYieldCache.get(Pair.of(startDate, endDate), (d) -> historicStockDataLoadService.loadQuoteForDates(ticker, startDate, endDate).stream()
                .filter(q -> (q.getDate().equals(startDate) || q.getDate().equals(endDate) || q.getDate().isBefore(endDate) || q.getDate().isAfter(startDate)) && q.getClose() != null)
                .collect(Collectors.toList()));
    }
}
