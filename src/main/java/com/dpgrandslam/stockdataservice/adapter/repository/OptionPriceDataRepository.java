package com.dpgrandslam.stockdataservice.adapter.repository;

import com.dpgrandslam.stockdataservice.domain.model.options.OptionPriceData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Set;
import java.util.stream.Stream;

@Repository
public interface OptionPriceDataRepository extends JpaRepository<OptionPriceData, Long> {

    Set<OptionPriceData> findAllByOptionId(Long optionId);

    Set<OptionPriceData> findAllByOptionIdAndDataObtainedDateBetween(Long optionId, Timestamp start,  Timestamp end);
}
