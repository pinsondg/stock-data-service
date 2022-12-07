package com.dpgrandslam.stockdataservice.unit.service;

import com.dpgrandslam.stockdataservice.domain.error.YahooFinanceQuoteLoadException;
import com.dpgrandslam.stockdataservice.domain.model.stock.YahooFinanceQuote;
import com.dpgrandslam.stockdataservice.domain.service.TenYearTreasuryYieldService;
import com.dpgrandslam.stockdataservice.domain.service.YahooFinanceHistoricStockDataLoadService;
import com.github.benmanes.caffeine.cache.Cache;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TenYearTreasuryYieldServiceTest {

    @Mock
    private Cache<Pair<LocalDate, LocalDate>, List<YahooFinanceQuote>> treasuryYieldCache;

    @Mock
    private YahooFinanceHistoricStockDataLoadService historicStockDataLoadService;

    @InjectMocks
    private TenYearTreasuryYieldService subject;

    @Test
    public void testGetTreasuryYieldForDate() {
        YahooFinanceQuote yield = YahooFinanceQuote.builder()
                .date(LocalDate.now())
                .open(1.2)
                .close(1.3)
                .low(1.1)
                .high(1.5)
                .adjClose(1.3)
                .build();
        when(historicStockDataLoadService.loadQuoteForDates(any(), any(), any())).thenReturn(Collections.singletonList(yield));
        when(treasuryYieldCache.get(any(), any())).then(invok -> {
            Function<Pair<LocalDate, LocalDate>, List<YahooFinanceQuote>> func = invok.getArgument(1);
            return func.apply(invok.getArgument(0));
        });
        List<YahooFinanceQuote> actual = subject.getTreasuryYieldForDate(LocalDate.now().minusDays(1), LocalDate.now());

        verify(treasuryYieldCache, times(1)).get(eq(Pair.of(LocalDate.now().minusDays(1), LocalDate.now())), any());
        verify(historicStockDataLoadService, times(1)).loadQuoteForDates(eq("^TNX"), eq(LocalDate.now().minusDays(1)), eq(LocalDate.now()));

        assertEquals(Collections.singletonList(yield), actual);
    }
}
