package com.dpgrandslam.stockdataservice.adapter.api;

import com.dpgrandslam.stockdataservice.domain.error.OptionsChainLoadException;
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

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
                                                             @RequestParam(name = "endDate") Optional<LocalDate> endDate) throws OptionsChainLoadException {
        List<OptionsChain> retVal = new ArrayList<>();
        if ((startDate.isPresent() || endDate.isPresent()) && expirationDate.isPresent()) {
            retVal.add(optionsChainLoadService.loadCompleteOptionsChainForExpirationDateWithPriceDataInRange(ticker,
                    expirationDate.get(),
                    Timestamp.from(Instant.from(startDate.orElse(LocalDate.now(ZoneId.of("America/New_York"))))),
                    Timestamp.from(Instant.from(endDate.orElse(LocalDate.now(ZoneId.of("America/New_York")))))));
        } else if (expirationDate.isPresent()){
            retVal.add(optionsChainLoadService.loadLiveOptionsChainForExpirationDate(ticker, expirationDate.get()));
        } else {
            retVal = optionsChainLoadService.loadFullLiveOptionsChain(ticker);
        }
        return ResponseEntity.ok(retVal);
    }

    @GetMapping("/tracked")
    public ResponseEntity<List<TrackedStock>> getTrackedStocks(@RequestParam(name = "activeOnly", required = false, defaultValue = "true") boolean activeOnly) {
        return ResponseEntity.ok(trackedStockService.getAllTrackedStocks());
    }

    @PostMapping(value = "/tracked", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity addTrackedStocks(@RequestBody List<String> tickers) {
        trackedStockService.addTrackedStocks(tickers);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
