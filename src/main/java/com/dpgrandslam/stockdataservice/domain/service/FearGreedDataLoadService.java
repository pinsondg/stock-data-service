package com.dpgrandslam.stockdataservice.domain.service;

import com.dpgrandslam.stockdataservice.adapter.repository.FearGreedIndexRepository;
import com.dpgrandslam.stockdataservice.domain.model.FearGreedIndex;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
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
        try {
            return fearGreedIndexRepository.save(fearGreedIndex);
        } catch (DataIntegrityViolationException e) {
            log.warn("Tried to save duplicate value for {}. Ignoring save. Message: {}", fearGreedIndex, e.getMessage());
        }
        return fearGreedIndex;
    }

    public List<FearGreedIndex> saveFearGreedData(Collection<FearGreedIndex> fearGreedIndices) {
        try {
            return fearGreedIndexRepository.saveAll(fearGreedIndices);
        } catch (DataIntegrityViolationException e) {
            return fearGreedIndices.stream()
                    .map(this::saveFearGreedData)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
    }

    public Optional<FearGreedIndex> getFearGreedIndexOfDay(LocalDate date) {
        return fearGreedIndexRepository.findFearGreedIndexByTradeDate(date);
    }

}
