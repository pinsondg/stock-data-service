package com.dpgrandslam.stockdataservice.adapter.repository;

import com.dpgrandslam.stockdataservice.domain.model.options.OptionPriceData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import javax.persistence.QueryHint;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Stream;

@Repository
public interface OptionPriceDataRepository extends JpaRepository<OptionPriceData, Long> {

    @QueryHints(@QueryHint(name = org.hibernate.annotations.QueryHints.CACHEABLE, value = "true"))
    Set<OptionPriceData> findAllByOptionId(Long optionId);

    Set<OptionPriceData> findAllByOptionIdAndDataObtainedDateBetween(Long optionId, Timestamp start,  Timestamp end);

    Long countAllByTradeDate(LocalDate tradeDate);
}
