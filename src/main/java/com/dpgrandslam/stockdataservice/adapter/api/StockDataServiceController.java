package com.dpgrandslam.stockdataservice.adapter.api;

import com.dpgrandslam.stockdataservice.domain.model.options.OptionsChain;
import com.dpgrandslam.stockdataservice.domain.model.stock.TrackedStock;
import com.dpgrandslam.stockdataservice.domain.service.OptionsChainLoadService;
import com.dpgrandslam.stockdataservice.domain.service.TrackedStockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
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

    @GetMapping("/option/{ticker}")
    public ResponseEntity<List<OptionsChain>> getOptionsChain(@PathVariable(name = "ticker") String ticker,
                                                             @RequestParam(name = "expirationDate") Optional<LocalDate> expirationDate,
                                                             @RequestParam(name = "startDate") Optional<LocalDate> startDate,
                                                             @RequestParam(name = "endDate") Optional<LocalDate> endDate) {
        List<OptionsChain> retVal = new ArrayList<>();
        if ((startDate.isPresent() || endDate.isPresent()) && expirationDate.isPresent()) {
            retVal.add(optionsChainLoadService.loadCompleteOptionsChainForExpirationDateWithPriceDataInRange(ticker,
                    expirationDate.get(),
                    startDate.orElse(LocalDate.MIN),
                    endDate.orElse(LocalDate.now())));
        } else if (startDate.isPresent() || endDate.isPresent()) {
            optionsChainLoadService.loadFullOptionsChainWithAllDataBetweenDates(
                    ticker,
                    startDate.orElse(LocalDate.MIN),
                    endDate.orElse(LocalDate.now())
            );
        } else if (expirationDate.isPresent()){
            retVal.add(optionsChainLoadService.loadLiveOptionsChainForExpirationDate(ticker, expirationDate.get()));
        } else {
            retVal = optionsChainLoadService.loadFullLiveOptionsChain(ticker);
        }
        if (retVal.size() > 0) {
            return ResponseEntity.ok(retVal);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
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
