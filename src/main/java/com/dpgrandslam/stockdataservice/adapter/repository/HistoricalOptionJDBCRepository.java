package com.dpgrandslam.stockdataservice.adapter.repository;

import com.dpgrandslam.stockdataservice.domain.model.options.HistoricalOption;

import java.time.LocalDate;
import java.util.Set;

public interface HistoricalOptionJDBCRepository {

    Set<HistoricalOption> findByTickerBetweenDates(String ticker, LocalDate startDate, LocalDate endDate);

    Set<LocalDate> getExpirationDatesForOptionsAfterDate(String ticker, LocalDate date);

}
