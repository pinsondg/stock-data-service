package com.dpgrandslam.stockdataservice.adapter.repository;

import com.dpgrandslam.stockdataservice.domain.model.options.HistoricalOption;
import com.dpgrandslam.stockdataservice.domain.model.options.Option;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.QueryHint;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

@Repository
public interface HistoricalOptionRepository extends JpaRepository<HistoricalOption, Long> {

    @Query("select ho from HistoricalOption ho left join fetch ho.historicalPriceData where ho.ticker =:ticker")
    @QueryHints(@QueryHint(name = org.hibernate.annotations.QueryHints.CACHEABLE, value = "true"))
    Set<HistoricalOption> findByTicker(@Param("ticker") String ticker);

    Set<HistoricalOption> findByExpirationAndTicker(LocalDate expiration, String ticker);

//    @Query("select option from HistoricalOption option inner join OptionPriceData data on option.id = data.option.id where (option.ticker = ?1 and option.expiration = ?2) and (data.dataObtainedDate between ?3 and ?4)")
//    Stream<HistoricalOption> findByTickerAndExpirationWithDataBetweenDates(String ticker, LocalDate expiration, Timestamp start, Timestamp end);

    @Query("select h from HistoricalOption h left join fetch h.historicalPriceData where h.strike = :strike and h.expiration = :expiration and h.ticker = :ticker and h.optionType = :optionType")
    Optional<HistoricalOption> findByStrikeAndExpirationAndTickerAndOptionType(@Param("strike") Double strike, @Param("expiration") LocalDate expiration, @Param("ticker") String ticker, @Param("optionType") Option.OptionType optionType);
}
