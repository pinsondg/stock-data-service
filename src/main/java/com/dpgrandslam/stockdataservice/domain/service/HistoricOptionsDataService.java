package com.dpgrandslam.stockdataservice.domain.service;

import com.dpgrandslam.stockdataservice.adapter.repository.HistoricalOptionRepository;
import com.dpgrandslam.stockdataservice.domain.model.options.HistoricalOption;
import com.dpgrandslam.stockdataservice.domain.model.options.Option;
import com.dpgrandslam.stockdataservice.domain.model.options.OptionPriceData;
import com.dpgrandslam.stockdataservice.domain.model.options.OptionsChain;
import com.dpgrandslam.stockdataservice.domain.util.TimerUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class HistoricOptionsDataService {

    private final HistoricalOptionRepository historicalOptionRepository;

    private final Cache<String, Set<HistoricalOption>> historicOptionCache;

    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    public List<HistoricalOption> findAll() {
        return historicalOptionRepository.findAll();
    }

    public HistoricalOption findById(Long id) {
        return historicalOptionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Could not find option with id " + id));
    }

    public HistoricalOption addOption(Option option) {
        TimerUtil timerUtil = new TimerUtil();
        timerUtil.start();
        HistoricalOption ret;
        log.info("Adding new option (ticker: {}, strike: {}, expiration: {}, type: {}) to database.", option.getTicker(), option.getStrike(), option.getExpiration(), option.getOptionType());
        Optional<HistoricalOption> found = historicalOptionRepository.findByTickerStrikeOptionTypeAndExpiration(option.getExpiration(),
                option.getTicker(),
                option.getStrike(),
                option.getOptionType());
        if (found.isPresent()) {
            log.debug("Option {} already exists. Adding price data instead.", found.get());
            ret = addPriceDataToOption(found.get(), option.getOptionPriceData());
        } else {
            ret = historicalOptionRepository.save(option.toHistoricalOption());
        }
        log.info("Took {} ms to add option (ticker: {}, strike: {}, expiration: {}, type: {})", timerUtil.stop(), option.getTicker(), option.getStrike(), option.getExpiration(), option.getOptionType());
        return ret;
    }

    @Transactional
    public void addFullOptionsChain(List<OptionsChain> fullOptionsChain) {
        fullOptionsChain.forEach(this::addOptionsChain);
    }

    public void addOptionsChain(OptionsChain optionsChain) {
        TimerUtil timerUtil = TimerUtil.startTimer();
        log.info("Adding new options chain with ticker {} and expiration {} to database.", optionsChain.getTicker(),
                optionsChain.getExpirationDate());
        List<Callable<HistoricalOption>> callables = new LinkedList<>();
        optionsChain.getAllOptions()
                .forEach((option) -> callables.add(() -> addOption(option)));
        try {
            executor.invokeAll(callables);
        } catch (InterruptedException e) {
            log.error("Could not add all options to the chain for option chain with ticker {} and expiration {}",
                    optionsChain.getTicker(),
                    optionsChain.getExpirationDate(),
                    e);
            return;
        }
        log.info("Took {} ms to add options chain with ticker {} and expiration {}.", timerUtil.stop(),
                optionsChain.getTicker(),
                optionsChain.getExpirationDate());
    }

    public Set<HistoricalOption> findOptions(String ticker) {
        log.info("Searching DB for options with ticker: {}", ticker);
        TimerUtil timerUtil = new TimerUtil();
        timerUtil.start();
        Set<HistoricalOption> options = historicOptionCache.get(ticker, historicalOptionRepository::findByTicker);
        log.info("Took {} ms to load options with ticker: {}", timerUtil.stop(), ticker);
        log.info("Found {} options with ticker: {}", options.size(), ticker);
        CacheStats cacheStats = historicOptionCache.stats();
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
        TimerUtil timerUtil = new TimerUtil();
        timerUtil.start();
        Set<HistoricalOption> options = historicalOptionRepository.findByExpirationAndTicker(expiration, ticker);
        log.info("Took {} ms to load options with ticker: {} and expiration {}", timerUtil.stop(), ticker, expiration);
        log.info("Found {} options with ticker: {} and expiration: {}", options.size(), ticker, expiration);
        return options;
    }

    public Set<HistoricalOption> findOptions(String ticker, LocalDate expiration, LocalDate startDate, LocalDate endDate) {
        return filterPriceDataBetweenDates(findOptions(ticker, expiration), startDate, endDate);
    }

    public HistoricalOption findOption(String ticker, LocalDate expiration, Double strike, Option.OptionType optionType) {
        log.info("Searching DB for option with ticker: {}, expiration: {}, strike: {}, and optionType: {}", ticker, expiration, strike, optionType.name());
        long start = System.currentTimeMillis();
        HistoricalOption option =  historicalOptionRepository.findByTickerStrikeOptionTypeAndExpiration(expiration, ticker, strike, optionType)
                .orElseThrow(() -> new EntityNotFoundException("Could not find option matching given criteria. " +
                        "Ticker: " + ticker + "," +
                        "Expiration: " + expiration + "," +
                        "Strike: " + strike + "," +
                        "OptionType: " + optionType));
        log.info("Took {} ms to load option with ticker: {}, expiration: {}, strike: {}, and optionType: {}", System.currentTimeMillis() - start,
                ticker, expiration, strike, optionType.name());
        return option;
    }

    public HistoricalOption addPriceDataToOption(HistoricalOption historicalOption, Collection<OptionPriceData> optionPriceData) {
        return doPriceDataAdd(optionPriceData, TimerUtil.startTimer(), historicalOption);
    }

    public HistoricalOption addPriceDataToOption(Long optionId, OptionPriceData optionPriceData) {
        HistoricalOption option = findById(optionId);
        optionPriceData.setOption(option);
        option.getHistoricalPriceData().add(optionPriceData);
        return historicalOptionRepository.save(option);
    }

    public HistoricalOption addPriceDataToOption(Long optionId, Collection<OptionPriceData> optionPriceData) {
        TimerUtil timerUtil = TimerUtil.startTimer();
        HistoricalOption option = findById(optionId);
        return doPriceDataAdd(optionPriceData, timerUtil, option);
    }

    private HistoricalOption doPriceDataAdd(Collection<OptionPriceData> optionPriceData, TimerUtil timerUtil, HistoricalOption option) {
        HistoricalOption saved = option;
        log.info("Adding new price data {} to option {}", optionPriceData, option);
        Set<OptionPriceData> priceDataCopy = new HashSet<>(optionPriceData);
        priceDataCopy.removeIf(data -> option.getHistoricalPriceData()
                .stream()
                .anyMatch(x -> data.getTradeDate().equals(x.getTradeDate())));
        priceDataCopy.forEach(data -> data.setOption(option));
        if (priceDataCopy.size() == 0) {
            log.info("Price data for option {} at trade date {} already exists. Skipping addition...", option, optionPriceData.stream().findFirst().get().getTradeDate());
        } else {
            option.getHistoricalPriceData().addAll(priceDataCopy);
            saved = historicalOptionRepository.save(option);
        }
        log.info("Took {} ms to add price data {} to option {}", timerUtil.stop(), optionPriceData, option);
        return saved;
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
