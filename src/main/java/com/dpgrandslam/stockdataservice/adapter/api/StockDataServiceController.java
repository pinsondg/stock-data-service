package com.dpgrandslam.stockdataservice.adapter.api;

import com.dpgrandslam.stockdataservice.domain.error.OptionsChainLoadException;
import com.dpgrandslam.stockdataservice.domain.model.FearGreedIndex;
import com.dpgrandslam.stockdataservice.domain.model.options.Option;
import com.dpgrandslam.stockdataservice.domain.model.options.OptionsChain;
import com.dpgrandslam.stockdataservice.domain.model.stock.*;
import com.dpgrandslam.stockdataservice.domain.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:3000")
@Controller
@RequestMapping("/data")
@Slf4j
public class StockDataServiceController {

    private static final LocalDate MIN_DATE = LocalDate.of(1900, 1, 1);

    @Autowired
    private OptionsChainLoadService optionsChainLoadService;

    @Autowired
    private TrackedStockService trackedStockService;

    @Autowired
    private StockDataLoadService stockDataLoadService;

    @Autowired
    private TenYearTreasuryYieldService treasuryYieldService;

    @Autowired
    private FearGreedDataLoadService fearGreedDataLoadService;

    @Autowired
    private VIXLoadService vixLoadService;

    @GetMapping("/option/{ticker}")
    public ResponseEntity<List<OptionsChain>> getOptionsChain(@PathVariable(name = "ticker") String ticker,
                                                             @RequestParam(name = "expirationDate") Optional<String> expirationDate,
                                                             @RequestParam(name = "startDate") Optional<String> startDate,
                                                             @RequestParam(name = "endDate") Optional<String> endDate) throws OptionsChainLoadException {
        log.info("Received request for options data with ticker: {}, expirationDate: {}, startDate: {}, and endDate: {}",
                ticker, expirationDate.orElse(null), startDate.orElse(null), endDate.orElse(null));
        List<OptionsChain> retVal = new ArrayList<>();
        if ((startDate.isPresent() || endDate.isPresent()) && expirationDate.isPresent()) {
            retVal.add(optionsChainLoadService.loadCompleteOptionsChainForExpirationDateWithPriceDataInRange(ticker,
                    expirationDate.map(LocalDate::parse).get(),
                    startDate.map(LocalDate::parse).orElse(MIN_DATE),
                    endDate.map(LocalDate::parse).orElse(LocalDate.now())));
        } else if (startDate.isPresent() || endDate.isPresent()) {
            retVal = optionsChainLoadService.loadFullOptionsChainWithAllDataBetweenDates(
                    ticker,
                    startDate.map(LocalDate::parse).orElse(MIN_DATE),
                    endDate.map(LocalDate::parse).orElse(LocalDate.now())
            );
        } else if (expirationDate.isPresent()){
            retVal.add(optionsChainLoadService.loadLiveOptionsChainForExpirationDate(ticker, expirationDate.map(LocalDate::parse).get()));
        } else {
            retVal = optionsChainLoadService.loadFullLiveOptionsChain(ticker);
        }
        return ResponseEntity.ok(retVal);
    }

    @GetMapping("/option/{ticker}/all")
    @Transactional
    public ResponseEntity<List<OptionsChain>> getFullOptionsChain(@PathVariable String ticker) throws OptionsChainLoadException {
        return ResponseEntity.ok(optionsChainLoadService.loadFullOptionsChainWithAllData(ticker));
    }

    @GetMapping("/stock/{ticker}")
    public ResponseEntity<List<EndOfDayStockData>> getEndOfDayStockData(@PathVariable String ticker,
                                                                  Optional<String> startDate,
                                                                  Optional<String> endDate) {
        if (startDate.isEmpty() && endDate.isEmpty()) {
            return ResponseEntity.ok(stockDataLoadService.getMostRecentEndOfDayStockData(ticker));
        }
        return ResponseEntity.ok(stockDataLoadService.getEndOfDayStockData(ticker, startDate.map(LocalDate::parse).orElse(LocalDate.MIN),
                endDate.map(LocalDate::parse).orElse(LocalDate.now())));
    }

    @GetMapping("/stock/{ticker}/live")
    public ResponseEntity<LiveStockData> getLiveStockData(@PathVariable(name = "ticker") String ticker) {
        return ResponseEntity.ok(stockDataLoadService.getLiveStockData(ticker));
    }

    @GetMapping("/stock/search")
    public ResponseEntity<List<? extends StockSearchResult>> searchStock(@RequestParam(name = "q") String query) {
        return ResponseEntity.ok(stockDataLoadService.searchStock(query));
    }

    @GetMapping("/tracked")
    public ResponseEntity<List<TrackedStock>> getTrackedStocks(@RequestParam(name = "activeOnly", required = false, defaultValue = "true") boolean activeOnly) {
        return ResponseEntity.ok(trackedStockService.getAllTrackedStocks(activeOnly));
    }

    @PutMapping("/tracked/active")
    public ResponseEntity<Map<String, Object>> updateTrackedStocksActive(@RequestBody Map<String, Boolean> body) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> failedUpdates = new HashMap<>();
        body.forEach((key, value) -> {
            try {
                trackedStockService.setTrackedStockActive(key, value);
            } catch (Exception e) {
                failedUpdates.put(key, "Could not update status of tracked stock " + key + ". Reason: " + e.getMessage());
            }
        });
        if (failedUpdates.isEmpty()) {
            response.put("status", "success");
        } else {
            response.put("status", "has_failures");
            response.put("failed", failedUpdates);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/tracked", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity addTrackedStocks(@RequestBody List<String> tickers) {
        trackedStockService.addTrackedStocks(tickers);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/treasury-yield")
    public ResponseEntity<YahooFinanceQuote> getTreasuryYield(@RequestParam Optional<String> date) {
        return ResponseEntity.ok(treasuryYieldService.getTreasuryYieldForDate(date.map(LocalDate::parse)
                .orElse(LocalDate.now())));
    }

    @GetMapping("/fear-greed")
    public ResponseEntity<List<FearGreedIndex>> getFearGreedIndexBetweenDates(@RequestParam Optional<String> startDate,
                                                                              @RequestParam Optional<String> endDate) {
        LocalDate sd = startDate.map(LocalDate::parse).orElse(LocalDate.now());
        LocalDate ed = endDate.map(LocalDate::parse).orElse(LocalDate.now());

        if (sd.equals(LocalDate.now()) && ed.equals(LocalDate.now())) {
            return ResponseEntity.ok(fearGreedDataLoadService.loadCurrentFearGreedIndex().stream()
                    .sorted(Comparator.comparing(FearGreedIndex::getTradeDate))
                    .collect(Collectors.toList()));
        } else if (sd.equals(ed)) {
            Optional<FearGreedIndex> fgIndex = fearGreedDataLoadService.getFearGreedIndexOfDay(sd);
            return fgIndex.map(fearGreedIndex -> ResponseEntity.ok(Collections.singletonList(fearGreedIndex))).orElseGet(() -> ResponseEntity.notFound().build());
        }
        return ResponseEntity.ok(fearGreedDataLoadService.loadFearGreedDataBetweenDates(sd, ed));
    }

    @GetMapping("/vix")
    public ResponseEntity<List<YahooFinanceQuote>> getVixForDates(@RequestParam String startDate, @RequestParam Optional<String> endDate) {
        return ResponseEntity.ok(vixLoadService.loadVIXBetweenDates(LocalDate.parse(startDate), endDate.map(LocalDate::parse).orElse(LocalDate.parse(startDate))));
    }
}
