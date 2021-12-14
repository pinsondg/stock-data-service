package com.dpgrandslam.stockdataservice.domain.service;

import com.dpgrandslam.stockdataservice.domain.model.FearGreedIndex;

import java.util.Set;

public interface FearGreedIndexDataLoadService {

    Set<FearGreedIndex> loadCurrentFearGreedIndex();

}
