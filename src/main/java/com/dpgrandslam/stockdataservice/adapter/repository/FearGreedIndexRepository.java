package com.dpgrandslam.stockdataservice.adapter.repository;

import com.dpgrandslam.stockdataservice.domain.model.FearGreedIndex;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FearGreedIndexRepository extends JpaRepository<FearGreedIndex, Long> {

    Optional<FearGreedIndex> findFearGreedIndexByTradeDate(LocalDate tradeDate);

    List<FearGreedIndex> findFearGreedIndexByTradeDateBetween(LocalDate startDate, LocalDate endDate);

    List<FearGreedIndex> findFearGreedIndicesByTradeDateGreaterThanEqual(LocalDate tradeDate);

}
