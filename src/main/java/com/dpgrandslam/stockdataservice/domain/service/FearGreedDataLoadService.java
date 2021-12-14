package com.dpgrandslam.stockdataservice.domain.service;

import com.dpgrandslam.stockdataservice.adapter.repository.FearGreedIndexRepository;
import com.dpgrandslam.stockdataservice.domain.model.FearGreedIndex;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public abstract class FearGreedDataLoadService {

    protected final FearGreedIndexRepository fearGreedIndexRepository;
    private final Cache<Pair<LocalDate, LocalDate>, List<FearGreedIndex>> fearGreedBetweenDatesCache;

    public abstract Set<FearGreedIndex> loadCurrentFearGreedIndex();

    public List<FearGreedIndex> loadFearGreedDataBetweenDates(LocalDate startDate, LocalDate endDate) {
        if (endDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("endDate cannot be in the future");
        }
        if (startDate.equals(LocalDate.now()) && endDate.equals(LocalDate.now())) {
            return new ArrayList<>(loadCurrentFearGreedIndex());
        }
        return fearGreedBetweenDatesCache.get(Pair.of(startDate, endDate), pair -> fearGreedIndexRepository
                .findFearGreedIndexByTradeDateBetween(pair.getLeft(), pair.getRight()).stream()
                .sorted(Comparator.comparing(FearGreedIndex::getTradeDate))
                .collect(Collectors.toList()));
    }

    public FearGreedIndex saveFearGreedData(FearGreedIndex fearGreedIndex) {
        return fearGreedIndexRepository.save(fearGreedIndex);
    }

    public List<FearGreedIndex> saveFearGreedData(Collection<FearGreedIndex> fearGreedIndices) {
        return fearGreedIndexRepository.saveAll(fearGreedIndices);
    }

    public Optional<FearGreedIndex> getFearGreedIndexOfDay(LocalDate date) {
        return fearGreedIndexRepository.findFearGreedIndexByTradeDate(date);
    }

}
