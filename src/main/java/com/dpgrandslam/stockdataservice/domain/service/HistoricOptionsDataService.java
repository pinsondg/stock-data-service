package com.dpgrandslam.stockdataservice.domain.service;

import com.dpgrandslam.stockdataservice.adapter.repository.HistoricalOptionRepository;
import com.dpgrandslam.stockdataservice.adapter.repository.OptionPriceDataRepository;
import com.dpgrandslam.stockdataservice.domain.model.options.HistoricalOption;
import com.dpgrandslam.stockdataservice.domain.model.options.Option;
import com.dpgrandslam.stockdataservice.domain.model.options.OptionPriceData;
import com.dpgrandslam.stockdataservice.domain.model.options.OptionsChain;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.*;

@Service
@Slf4j
public class HistoricOptionsDataService {

    @Autowired
    private HistoricalOptionRepository historicalOptionRepository;

    @Autowired
    private OptionPriceDataRepository optionPriceDataRepository;

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

    public Slice<HistoricalOption> findOptions(String ticker, Integer page, Integer size) {
        log.info("Searching DB for options with ticker: {} with size {} and page {}", ticker, size, page);
        long start = System.currentTimeMillis();
        Slice<HistoricalOption> options = historicalOptionRepository.findByTicker(ticker, PageRequest.of(page, size, Sort.by("expiration").descending()));
        log.info("Took {} ms to load options with ticker: {}", System.currentTimeMillis() - start, ticker);
        log.info("Found {} options with ticker: {}", options.getNumberOfElements(), ticker);
        return options;
    }

    public Slice<HistoricalOption> findOptions(String ticker, LocalDate expiration, Integer page, Integer size) {
        log.info("Searching DB for options with ticker: {} and expiration: {}", ticker, expiration);
        long start = System.currentTimeMillis();
        Slice<HistoricalOption> options = historicalOptionRepository.findByExpirationAndTicker(expiration, ticker, PageRequest.of(page, size, Sort.by("expiration").descending()));
        log.info("Took {} ms to load options with ticker: {} and expiration {}", System.currentTimeMillis() - start, ticker, expiration);
        log.info("Found {} options with ticker: {} and expiration: {}", options.getNumberOfElements(), ticker, expiration);
        return options;
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
}
