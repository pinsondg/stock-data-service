package com.dpgrandslam.stockdataservice.domain.service;

import com.dpgrandslam.stockdataservice.adapter.repository.HistoricalOptionRepository;
import com.dpgrandslam.stockdataservice.adapter.repository.OptionPriceDataRepository;
import com.dpgrandslam.stockdataservice.domain.model.options.HistoricalOption;
import com.dpgrandslam.stockdataservice.domain.model.options.Option;
import com.dpgrandslam.stockdataservice.domain.model.options.OptionPriceData;
import com.dpgrandslam.stockdataservice.domain.model.options.OptionsChain;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

@Service
@Slf4j
@Transactional
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

    public void addFullOptionsChain(List<OptionsChain> fullOptionsChain) {
        fullOptionsChain.forEach(this::addOptionsChain);
    }

    public void addOptionsChain(OptionsChain optionsChain) {
        optionsChain.getAllOptions().forEach(this::addOption);
    }

    public Stream<HistoricalOption> findOptions(String ticker) {
        return historicalOptionRepository.findByTicker(ticker);
    }

    public Stream<HistoricalOption> findOptions(String ticker, LocalDate expiration) {
        return historicalOptionRepository.findByExpirationAndTicker(expiration, ticker);
    }

    public HistoricalOption findOption(String ticker, LocalDate expiration, Double strike, Option.OptionType optionType) {
        return historicalOptionRepository.findDistinctFirstByExpirationAndTickerAndStrikeAndOptionType(expiration, ticker, strike, optionType)
                .orElseThrow(() -> new EntityNotFoundException("Could not find option matching given criteria. " +
                        "Ticker: " + ticker + "," +
                        "Expiration: " + expiration + "," +
                        "Strike: " + strike + "," +
                        "OptionType: " + optionType));
    }

    public HistoricalOption addPriceDataToOption(Long optionId, OptionPriceData optionPriceData) {
        HistoricalOption option = findById(optionId);
        optionPriceData.setOption(option);
        option.getHistoricalPriceData().add(optionPriceData);
        return historicalOptionRepository.save(option);
    }

    @Transactional
    public HistoricalOption addPriceDataToOption(Long optionId, Collection<OptionPriceData> optionPriceData) {
        HistoricalOption option = findById(optionId);
        log.debug("Adding new price data {} to option {}", optionPriceData, option);
        Set<OptionPriceData> priceDataCopy = new HashSet<>(optionPriceData);
        priceDataCopy.removeIf(data -> option.getHistoricalPriceData()
                .stream()
                .anyMatch(x -> data.getTradeDate().equals(x.getTradeDate())));
        priceDataCopy.forEach(data -> data.setOption(option));
        if (priceDataCopy.size() == 0) {
            log.info("Price data for trade date {} already exists. Skipping addition...", optionPriceData.stream().findFirst().get().getTradeDate());
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
