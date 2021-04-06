package com.dpgrandslam.stockdataservice.domain.service;

import com.dpgrandslam.stockdataservice.adapter.repository.OptionPriceDataLoadRetryRepository;
import com.dpgrandslam.stockdataservice.domain.model.OptionPriceDataLoadRetry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class OptionPriceDataLoadRetryService {

    private final OptionPriceDataLoadRetryRepository retryRepository;

    public OptionPriceDataLoadRetryService(OptionPriceDataLoadRetryRepository retryRepository) {
        this.retryRepository = retryRepository;
    }

    public Optional<OptionPriceDataLoadRetry> findById(Long id) {
        return retryRepository.findById(id);
    }

    public List<OptionPriceDataLoadRetry> getAll() {
        return retryRepository.findAll();
    }

    public Set<OptionPriceDataLoadRetry> getAllWithTradeDate(LocalDate tradeDate) {
        return retryRepository.findAllByTradeDate(tradeDate);
    }

    public OptionPriceDataLoadRetry getRetry(String ticker, LocalDate expiration, LocalDate tradeDate) {
        return retryRepository.findByOptionTickerAndOptionExpirationAndTradeDate(ticker, expiration, tradeDate);
    }

    public Set<OptionPriceDataLoadRetry> getRetries(String ticker, LocalDate expiration) {
        return retryRepository.findAllByOptionTickerAndOptionExpiration(ticker, expiration);
    }

    public OptionPriceDataLoadRetry addRetry(String ticker, LocalDate expiration, LocalDate tradeDate) {
        log.info("Adding new retry record with ticker [{}], expiration [{}], and tradeDate [{}]",
                ticker, expiration, tradeDate);
        OptionPriceDataLoadRetry optionPriceDataLoadRetry = new OptionPriceDataLoadRetry();
        optionPriceDataLoadRetry.setOptionExpiration(expiration);
        optionPriceDataLoadRetry.setOptionTicker(ticker);
        optionPriceDataLoadRetry.setTradeDate(tradeDate);
        optionPriceDataLoadRetry.setRetryCount(0);

        return retryRepository.save(optionPriceDataLoadRetry);
    }

    public OptionPriceDataLoadRetry updateRetryCount(OptionPriceDataLoadRetry optionPriceDataLoadRetry) {
        int newCount = optionPriceDataLoadRetry.getRetryCount() + 1;
        optionPriceDataLoadRetry.setRetryCount(newCount);
        return retryRepository.save(optionPriceDataLoadRetry);
    }

    public OptionPriceDataLoadRetry updateRetryCount(String ticker, LocalDate expiration, LocalDate tradeDate) {
        OptionPriceDataLoadRetry retry = getRetry(ticker, expiration, tradeDate);
        if (retry != null) {
            int newCount = retry.getRetryCount() + 1;
            retry.setRetryCount(newCount);
            return retryRepository.save(retry);
        } else {
            log.warn("Retry with ticker {}, expiration {}, and tradeDate {} could not be found. Retry count was not updated.",
                    ticker, expiration, tradeDate);
            return null;
        }
    }

    public void removeRetry(Long id) {
        retryRepository.deleteById(id);
    }

    public void removeRetry(String ticker, LocalDate expiration, LocalDate tradeDate) {
        OptionPriceDataLoadRetry retry = retryRepository.findByOptionTickerAndOptionExpirationAndTradeDate(ticker,
                expiration, tradeDate);
        if (retry != null) {
            retryRepository.delete(retry);
        } else {
            log.warn("Could not find retry record with ticker {} and expiration {} and tradeDate {} so no record was removed.",
                    ticker, expiration, tradeDate);
        }
    }

    /**
     * A job to remove all of the stale retries in the retry table. Runs at market open every weekday.
     */
    @Scheduled(cron = "0 30 9 * * 1-5")
    private void removeStaleRetries() {
        retryRepository.deleteAllByTradeDateBefore(LocalDate.now());
    }

}
