package com.dpgrandslam.stockdataservice.adapter.api;

import com.dpgrandslam.stockdataservice.domain.error.OptionsChainLoadException;
import com.dpgrandslam.stockdataservice.domain.model.options.OptionsChain;
import com.dpgrandslam.stockdataservice.domain.model.stock.*;
import com.dpgrandslam.stockdataservice.domain.service.OptionsChainLoadService;
import com.dpgrandslam.stockdataservice.domain.service.StockDataLoadService;
import com.dpgrandslam.stockdataservice.domain.service.TrackedStockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/data")
public class StockDataServiceController {

    @Autowired
    private OptionsChainLoadService optionsChainLoadService;

    @Autowired
    private TrackedStockService trackedStockService;

    @Autowired
    private StockDataLoadService stockDataLoadService;

    @GetMapping("/option/{ticker}")
    public ResponseEntity<List<OptionsChain>> getOptionsChain(@PathVariable(name = "ticker") String ticker,
                                                             @RequestParam(name = "expirationDate") Optional<String> expirationDate,
                                                             @RequestParam(name = "startDate") Optional<String> startDate,
                                                             @RequestParam(name = "endDate") Optional<String> endDate) throws OptionsChainLoadException {
        List<OptionsChain> retVal = new ArrayList<>();
        if ((startDate.isPresent() || endDate.isPresent()) && expirationDate.isPresent()) {
            retVal.add(optionsChainLoadService.loadCompleteOptionsChainForExpirationDateWithPriceDataInRange(ticker,
                    expirationDate.map(LocalDate::parse).get(),
                    startDate.map(LocalDate::parse).orElse(LocalDate.MIN),
                    endDate.map(LocalDate::parse).orElse(LocalDate.now())));
        } else if (startDate.isPresent() || endDate.isPresent()) {
            retVal = optionsChainLoadService.loadFullOptionsChainWithAllDataBetweenDates(
                    ticker,
                    startDate.map(LocalDate::parse).orElse(LocalDate.MIN),
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
}
