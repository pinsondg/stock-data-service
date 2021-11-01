package com.dpgrandslam.stockdataservice.adapter.api;

import com.dpgrandslam.stockdataservice.domain.error.OptionsChainLoadException;
import com.dpgrandslam.stockdataservice.domain.model.options.OptionsChain;
import com.dpgrandslam.stockdataservice.domain.model.stock.*;
import com.dpgrandslam.stockdataservice.domain.service.OptionsChainLoadService;
import com.dpgrandslam.stockdataservice.domain.service.StockDataLoadService;
import com.dpgrandslam.stockdataservice.domain.service.TenYearTreasuryYieldService;
import com.dpgrandslam.stockdataservice.domain.service.TrackedStockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

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

    @GetMapping(value = "/option/{ticker}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<OptionsChain>> getOptionsChain(@PathVariable(name = "ticker") String ticker,
                                                             @RequestParam(name = "expirationDate", required = false) Optional<String> expirationDate,
                                                             @RequestParam(name = "startDate", required = false) Optional<String> startDate,
                                                             @RequestParam(name = "endDate", required = false) Optional<String> endDate) throws OptionsChainLoadException {
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

    @GetMapping(value = "/option/{ticker}/all", produces = APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<List<OptionsChain>> getFullOptionsChain(@PathVariable String ticker) throws OptionsChainLoadException {
        return ResponseEntity.ok(optionsChainLoadService.loadFullOptionsChainWithAllData(ticker));
    }

    @GetMapping(value = "/stock/{ticker}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<EndOfDayStockData>> getEndOfDayStockData(@PathVariable String ticker,
                                                                  @RequestParam(required = false) Optional<String> startDate,
                                                                  @RequestParam(required = false) Optional<String> endDate) {
        if (startDate.isEmpty() && endDate.isEmpty()) {
            return ResponseEntity.ok(stockDataLoadService.getMostRecentEndOfDayStockData(ticker));
        }
        return ResponseEntity.ok(stockDataLoadService.getEndOfDayStockData(ticker, startDate.map(LocalDate::parse).orElse(LocalDate.MIN),
                endDate.map(LocalDate::parse).orElse(LocalDate.now())));
    }

    @GetMapping(value = "/stock/{ticker}/live", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<LiveStockData> getLiveStockData(@PathVariable(name = "ticker") String ticker) {
        return ResponseEntity.ok(stockDataLoadService.getLiveStockData(ticker));
    }

    @GetMapping(value = "/stock/search", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<? extends StockSearchResult>> searchStock(@RequestParam(name = "q") String query) {
        return ResponseEntity.ok(stockDataLoadService.searchStock(query));
    }

    @GetMapping(value = "/tracked", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TrackedStock>> getTrackedStocks(@RequestParam(name = "activeOnly", required = false, defaultValue = "true") boolean activeOnly) {
        return ResponseEntity.ok(trackedStockService.getAllTrackedStocks(activeOnly));
    }

    @PutMapping(value = "/tracked/active", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<TrackedStockUpdateResponse> updateTrackedStocksActive(@RequestBody List<TrackedStockUpdateRequest> body) {
        TrackedStockUpdateResponse response = new TrackedStockUpdateResponse();
        List<TrackedStockUpdateResponse.FailedUpdate> failedUpdates = new ArrayList<>();
        body.forEach((request) -> {
            try {
                trackedStockService.setTrackedStockActive(request.getTicker(), request.getTracked());
            } catch (Exception e) {
                failedUpdates.add(new TrackedStockUpdateResponse.FailedUpdate(request.getTicker(), "Could not update status of tracked stock " + request.getTicker() + ". Reason: " + e.getMessage()));
            }
        });
        if (failedUpdates.isEmpty()) {
            response.setStatus("success");
        } else {
            response.setStatus("has_failures");
            response.setFailedUpdates(failedUpdates);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/tracked", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity addTrackedStocks(@RequestBody List<String> tickers) {
        trackedStockService.addTrackedStocks(tickers);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping(value = "/treasury-yield", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<YahooFinanceTenYearTreasuryYield> getTreasuryYield(@RequestParam(required = false) Optional<String> date) {
        return ResponseEntity.ok(treasuryYieldService.getTreasuryYieldForDate(date.map(LocalDate::parse)
                .orElse(LocalDate.now())));
    }
}
