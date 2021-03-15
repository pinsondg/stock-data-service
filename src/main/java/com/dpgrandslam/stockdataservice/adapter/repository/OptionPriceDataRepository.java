package com.dpgrandslam.stockdataservice.adapter.repository;

import com.dpgrandslam.stockdataservice.domain.model.options.OptionPriceData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.stream.Stream;

@Repository
public interface OptionPriceDataRepository extends JpaRepository<OptionPriceData, Long> {

    Stream<OptionPriceData> findAllByOptionId(Long optionId);

    Stream<OptionPriceData> findAllByOptionIdAndDataObtainedDateBetween(Long optionId, Timestamp start,  Timestamp end);
}
