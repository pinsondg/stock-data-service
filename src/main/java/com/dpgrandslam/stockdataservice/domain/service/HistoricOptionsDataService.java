package com.dpgrandslam.stockdataservice.domain.service;

import com.dpgrandslam.stockdataservice.adapter.repository.HistoricalOptionRepository;
import com.dpgrandslam.stockdataservice.domain.model.options.HistoricalOption;
import com.dpgrandslam.stockdataservice.domain.model.options.Option;
import com.dpgrandslam.stockdataservice.domain.model.options.OptionPriceData;
import com.dpgrandslam.stockdataservice.domain.model.options.OptionsChain;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class HistoricOptionsDataService {

    private final HistoricalOptionRepository historicalOptionRepository;

    private final Cache<String, Set<HistoricalOption>> historicOptionCache;

    public List<HistoricalOption> findAll() {
        return historicalOptionRepository.findAll();
    }

    public HistoricalOption findById(Long id) {
        return historicalOptionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Could not find option with id " + id));
    }

    public HistoricalOption addOption(Option option) {
        Optional<HistoricalOption> found = historicalOptionRepository.findDistinctFirstByExpirationAndTickerAndStrikeAndOptionType(option.getExpiration(),
                option.getTicker(),
                option.getStrike(),
                option.getOptionType());
        if (found.isPresent()) {
            log.debug("Option {} already exists. Adding price data instead.", found.get());
            return addPriceDataToOption(found.get().getId(), option.getOptionPriceData());
        } else {
            return historicalOptionRepository.save(option.toHistoricalOption());
        }
    }

    @Transactional
    public void addFullOptionsChain(List<OptionsChain> fullOptionsChain) {
        fullOptionsChain.forEach(this::addOptionsChain);
    }

    public void addOptionsChain(OptionsChain optionsChain) {
        optionsChain.getAllOptions().forEach(this::addOption);
    }

    public Set<HistoricalOption> findOptions(String ticker) {
        log.info("Searching DB for options with ticker: {}", ticker);
        long start = System.currentTimeMillis();
        Set<HistoricalOption> options = historicOptionCache.get(ticker, historicalOptionRepository::findByTicker);
        log.info("Took {} ms to load options with ticker: {}", System.currentTimeMillis() - start, ticker);
        log.info("Found {} options with ticker: {}", options.size(), ticker);
        return options;
    }

    /**
     * Finds options with price data within the two dates.
     *
     * @param ticker the ticker to look for
     * @param startDate the start date
     * @param endDate the end date
     * @return a set of option that has the data between the dates
     */
    public Set<HistoricalOption> findOptions(String ticker, final LocalDate startDate, final LocalDate endDate) {
        return filterPriceDataBetweenDates(findOptions(ticker), startDate, endDate);
    }

    public Set<HistoricalOption> findOptions(String ticker, LocalDate expiration) {
        log.info("Searching DB for options with ticker: {} and expiration: {}", ticker, expiration);
        long start = System.currentTimeMillis();
        Set<HistoricalOption> options = historicalOptionRepository.findByExpirationAndTicker(expiration, ticker);
        log.info("Took {} ms to load options with ticker: {} and expiration {}", System.currentTimeMillis() - start, ticker, expiration);
        log.info("Found {} options with ticker: {} and expiration: {}", options.size(), ticker, expiration);
        return options;
    }

    public Set<HistoricalOption> findOptions(String ticker, LocalDate expiration, LocalDate startDate, LocalDate endDate) {
        return filterPriceDataBetweenDates(findOptions(ticker, expiration), startDate, endDate);
    }

    public HistoricalOption findOption(String ticker, LocalDate expiration, Double strike, Option.OptionType optionType) {
        log.info("Searching DB for option with ticker: {}, expiration: {}, strike: {}, and optionType: {}", ticker, expiration, strike, optionType.name());
        long start = System.currentTimeMillis();
        HistoricalOption option =  historicalOptionRepository.findDistinctFirstByExpirationAndTickerAndStrikeAndOptionType(expiration, ticker, strike, optionType)
                .orElseThrow(() -> new EntityNotFoundException("Could not find option matching given criteria. " +
                        "Ticker: " + ticker + "," +
                        "Expiration: " + expiration + "," +
                        "Strike: " + strike + "," +
                        "OptionType: " + optionType));
        log.info("Took {} ms to load option with ticker: {}, expiration: {}, strike: {}, and optionType: {}", System.currentTimeMillis() - start,
                ticker, expiration, strike, optionType.name());
        return option;
    }

    public HistoricalOption addPriceDataToOption(Long optionId, OptionPriceData optionPriceData) {
        HistoricalOption option = findById(optionId);
        optionPriceData.setOption(option);
        option.getHistoricalPriceData().add(optionPriceData);
        return historicalOptionRepository.save(option);
    }

    public HistoricalOption addPriceDataToOption(Long optionId, Collection<OptionPriceData> optionPriceData) {
        HistoricalOption option = findById(optionId);
        log.debug("Adding new price data {} to option {}", optionPriceData, option);
        Set<OptionPriceData> priceDataCopy = new HashSet<>(optionPriceData);
        priceDataCopy.removeIf(data -> option.getHistoricalPriceData()
                .stream()
                .anyMatch(x -> data.getTradeDate().equals(x.getTradeDate())));
        priceDataCopy.forEach(data -> data.setOption(option));
        if (priceDataCopy.size() == 0) {
            log.info("Price data for option {} at trade date {} already exists. Skipping addition...", option, optionPriceData.stream().findFirst().get().getTradeDate());
        } else {
            option.getHistoricalPriceData().addAll(priceDataCopy);
            return historicalOptionRepository.save(option);
        }
        return option;
    }

    public void removeOption(Long optionId) {
        historicalOptionRepository.deleteById(optionId);
    }

    public void removeOption(HistoricalOption option) {
        historicalOptionRepository.delete(option);
    }

    public HistoricalOption saveOption(HistoricalOption historicalOption) {
        return historicalOptionRepository.saveAndFlush(historicalOption);
    }

    private Set<HistoricalOption> filterPriceDataBetweenDates(Set<HistoricalOption> historicalOptions, LocalDate startDate, LocalDate endDate) {
        if (endDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("End date cannot be after today.");
        }
        if (historicalOptions != null && !historicalOptions.isEmpty()) {
            historicalOptions.forEach(ho -> ho.getHistoricalPriceData().removeIf(priceData -> !((startDate.isEqual(priceData.getTradeDate()) || endDate.isEqual(priceData.getTradeDate()))
                    || (startDate.isBefore(priceData.getTradeDate()) && endDate.isAfter(priceData.getTradeDate())))));
            return historicalOptions.stream().filter(x -> !x.getHistoricalPriceData().isEmpty()).collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }
}
