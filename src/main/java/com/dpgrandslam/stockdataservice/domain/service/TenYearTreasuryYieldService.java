package com.dpgrandslam.stockdataservice.domain.service;

import com.dpgrandslam.stockdataservice.domain.error.YahooFinanceQuoteLoadException;
import com.dpgrandslam.stockdataservice.domain.model.stock.YahooFinanceQuote;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenYearTreasuryYieldService {

    private final YahooFinanceHistoricStockDataLoadService historicStockDataLoadService;
    private final Cache<LocalDate, YahooFinanceQuote> treasuryYieldCache;

    public YahooFinanceQuote getTreasuryYieldForDate(LocalDate date) {
        final String ticker = "^TNX";
        return treasuryYieldCache.get(date, (d) -> historicStockDataLoadService.loadQuoteForDates("^TNX", date, date).stream()
                .filter(q -> q.getDate().equals(date) && q.getClose() != null)
                .findFirst()
                .orElseThrow(() -> new YahooFinanceQuoteLoadException(ticker, date, date)));
    }
}
