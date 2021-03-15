package com.dpgrandslam.stockdataservice.adapter.repository;

import com.dpgrandslam.stockdataservice.domain.model.options.HistoricalOption;
import com.dpgrandslam.stockdataservice.domain.model.options.Option;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

@Repository
public interface HistoricalOptionRepository extends JpaRepository<HistoricalOption, Long> {

    Stream<HistoricalOption> findByTicker(String ticker);

    Stream<HistoricalOption> findByExpirationAndTicker(LocalDate expiration, String ticker);

//    @Query("select option from HistoricalOption option inner join OptionPriceData data on option.id = data.option.id where (option.ticker = ?1 and option.expiration = ?2) and (data.dataObtainedDate between ?3 and ?4)")
//    Stream<HistoricalOption> findByTickerAndExpirationWithDataBetweenDates(String ticker, LocalDate expiration, Timestamp start, Timestamp end);

    Optional<HistoricalOption> findDistinctFirstByExpirationAndTickerAndStrikeAndOptionType(LocalDate expiration, String ticker, Double strike, Option.OptionType optionType);

}
