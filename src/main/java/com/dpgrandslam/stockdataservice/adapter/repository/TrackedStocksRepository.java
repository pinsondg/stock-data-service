package com.dpgrandslam.stockdataservice.adapter.repository;

import com.dpgrandslam.stockdataservice.domain.model.stock.TrackedStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrackedStocksRepository extends JpaRepository<TrackedStock, String> {

    List<TrackedStock> findAllByActiveIsTrue();
}
