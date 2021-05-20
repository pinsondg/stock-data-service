package com.dpgrandslam.stockdataservice.adapter.repository;

import com.dpgrandslam.stockdataservice.domain.model.OptionPriceDataLoadRetry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Set;

@Repository
public interface OptionPriceDataLoadRetryRepository extends JpaRepository<OptionPriceDataLoadRetry, Long> {

    Set<OptionPriceDataLoadRetry> findAllByOptionTickerAndOptionExpiration(String ticker, LocalDate expiration);

    OptionPriceDataLoadRetry findByOptionTickerAndOptionExpirationAndTradeDate(String ticker, LocalDate expiration, LocalDate tradeDate);

    void deleteAllByTradeDateBefore(LocalDate date);

    Set<OptionPriceDataLoadRetry> findAllByTradeDate(LocalDate tradeDate);
}
