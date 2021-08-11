package com.dpgrandslam.stockdataservice.adapter.repository;

import com.dpgrandslam.stockdataservice.domain.model.options.HistoricalOption;
import com.dpgrandslam.stockdataservice.domain.model.options.Option;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Repository
public interface HistoricalOptionRepository extends JpaRepository<HistoricalOption, Long> {

    @Query("select ho from HistoricalOption ho inner join fetch ho.historicalPriceData where ho.ticker =:ticker")
    Set<HistoricalOption> findByTicker(@Param("ticker") String ticker);

    Set<HistoricalOption> findByExpirationAndTicker(LocalDate expiration, String ticker);

//    @Query("select option from HistoricalOption option inner join OptionPriceData data on option.id = data.option.id where (option.ticker = ?1 and option.expiration = ?2) and (data.dataObtainedDate between ?3 and ?4)")
//    Stream<HistoricalOption> findByTickerAndExpirationWithDataBetweenDates(String ticker, LocalDate expiration, Timestamp start, Timestamp end);

    @Query("select ho from HistoricalOption ho inner join fetch ho.historicalPriceData where ho.expiration =:expiration and ho.ticker =:ticker and ho.strike=:strike and ho.optionType=:optionType")
    Optional<HistoricalOption> findByTickerStrikeOptionTypeAndExpiration(@Param("expiration") LocalDate expiration,
                                                                         @Param("ticker") String ticker,
                                                                         @Param("strike") Double strike,
                                                                         @Param("optionType") Option.OptionType optionType);

}
