package com.dpgrandslam.stockdataservice.domain.service;

import com.dpgrandslam.stockdataservice.adapter.repository.HistoricalOptionJDBCRepository;
import com.dpgrandslam.stockdataservice.adapter.repository.HistoricalOptionRepository;
import com.dpgrandslam.stockdataservice.adapter.repository.OptionPriceDataRepository;
import com.dpgrandslam.stockdataservice.domain.model.options.HistoricalOption;
import com.dpgrandslam.stockdataservice.domain.model.options.Option;
import com.dpgrandslam.stockdataservice.domain.model.options.OptionPriceData;
import com.dpgrandslam.stockdataservice.domain.model.options.OptionsChain;
import com.dpgrandslam.stockdataservice.domain.util.TimerUtil;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import com.github.benmanes.caffeine.cache.Cache;


@Service
@Slf4j
@RequiredArgsConstructor
public class HistoricOptionsDataService {

    private final HistoricalOptionRepository historicalOptionRepository;

    private final Cache<String, Set<HistoricalOption.CacheableHistoricalOption>> historicalOptionCache;

    private final HistoricalOptionJDBCRepository historicalOptionJDBCRepository;

    private final OptionPriceDataRepository optionPriceDataRepository;

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3);

    public List<HistoricalOption> findAll() {
        return historicalOptionRepository.findAll();
    }

    public HistoricalOption findById(Long id) {
        return historicalOptionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Could not find option with id " + id));
    }

    /**
     * Adds an option to the database, if the option already exists, add the price data instead.
     *
     * @param option the option to add
     * @return the saved entity
     */
    public HistoricalOption addOption(Option option) {
        TimerUtil timerUtil = new TimerUtil();
        timerUtil.start();
        HistoricalOption ret;
        log.debug("Adding new option (ticker: {}, strike: {}, expiration: {}, type: {}) to database.", option.getTicker(), option.getStrike(), option.getExpiration(), option.getOptionType());
        Optional<HistoricalOption> found = historicalOptionRepository.findByStrikeAndExpirationAndTickerAndOptionType(option.getStrike(), option.getExpiration(),
                option.getTicker(),
                option.getOptionType());
        if (found.isPresent()) {
            log.debug("Option {} already exists. Adding price data instead.", found.get());
            ret = addPriceDataToOption(found.get(), option.getOptionPriceData());
        } else {
            ret = historicalOptionRepository.save(option.toHistoricalOption());
        }
        log.debug("Took {} ms to add option (ticker: {}, strike: {}, expiration: {}, type: {})", timerUtil.stop(), option.getTicker(), option.getStrike(), option.getExpiration(), option.getOptionType());
        return ret;
    }

    public void addFullOptionsChain(List<OptionsChain> fullOptionsChain) {
        fullOptionsChain.forEach(this::addOptionsChain);
    }

    @Synchronized
    public void addOptionsChain(OptionsChain optionsChain) {
        TimerUtil timerUtil = TimerUtil.startTimer();
        log.info("Adding new options chain with ticker {} and expiration {} to database.", optionsChain.getTicker(),
                optionsChain.getExpirationDate());
        CompletableFuture<?>[] cfs = new CompletableFuture<?>[optionsChain.getAllOptions().size()];
        List<CompletableFuture<HistoricalOption>> futures = new LinkedList<>();
        optionsChain.getAllOptions().forEach(option -> futures.add(CompletableFuture.supplyAsync(() -> addOption(option), EXECUTOR)));
        try {
            CompletableFuture.allOf(futures.toArray(cfs)).thenRun(() -> log.info("Options chain with ticker {} and expiration {} added successfully.", optionsChain.getTicker(), optionsChain.getExpirationDate())).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Could not add all options to the chain for option chain with ticker {} and expiration {}",
                    optionsChain.getTicker(),
                    optionsChain.getExpirationDate(),
                    e);
            return;
        }
        log.debug("Took {} ms to add options chain with ticker {} and expiration {}.", timerUtil.stop(),
                optionsChain.getTicker(),
                optionsChain.getExpirationDate());
    }

    /**
     * Finds options for the given ticker. Uses a cache to find them faster.
     * @param ticker the ticker
     * @return a set of options
     */
    //This method is very weird. Not sure why, but the cache only works this way...
    public Set<HistoricalOption> findOptions(String ticker) {
        log.info("Searching DB for options with ticker: {}", ticker);
        TimerUtil timerUtil = TimerUtil.startTimer();
        Set<HistoricalOption.CacheableHistoricalOption> options =  historicalOptionCache.get(ticker, t -> {
            Set<HistoricalOption> o = historicalOptionRepository.findByTicker(t);
            return o.stream().map(HistoricalOption.CacheableHistoricalOption::fromHistoricalOption).collect(Collectors.toSet());
        });
        log.debug("Took {} ms to load options with ticker: {}", timerUtil.stop(), ticker);
        log.debug("Found {} options with ticker: {}", options.size(), ticker);
        return options.stream().map(HistoricalOption::fromCacheableHistoricalOption).collect(Collectors.toSet());
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
        return historicalOptionJDBCRepository.findByTickerBetweenDates(ticker, startDate, endDate);
    }

    public Set<HistoricalOption> findOptions(String ticker, LocalDate expiration) {
        log.info("Searching DB for options with ticker: {} and expiration: {}", ticker, expiration);
        TimerUtil timerUtil = new TimerUtil();
        timerUtil.start();
        Set<HistoricalOption> options = historicalOptionRepository.findByExpirationAndTicker(expiration, ticker);
        log.debug("Took {} ms to load options with ticker: {} and expiration {}", timerUtil.stop(), ticker, expiration);
        log.debug("Found {} options with ticker: {} and expiration: {}", options.size(), ticker, expiration);
        return options;
    }

    public Set<HistoricalOption> findOptions(String ticker, LocalDate expiration, LocalDate startDate, LocalDate endDate) {
        return filterPriceDataBetweenDates(findOptions(ticker, expiration), startDate, endDate);
    }

    public HistoricalOption findOption(String ticker, LocalDate expiration, Double strike, Option.OptionType optionType) {
        log.info("Searching DB for option with ticker: {}, expiration: {}, strike: {}, and optionType: {}", ticker, expiration, strike, optionType.name());
        long start = System.currentTimeMillis();
        HistoricalOption option =  historicalOptionRepository.findByStrikeAndExpirationAndTickerAndOptionType(strike, expiration, ticker, optionType)
                .orElseThrow(() -> new EntityNotFoundException("Could not find option matching given criteria. " +
                        "Ticker: " + ticker + "," +
                        "Expiration: " + expiration + "," +
                        "Strike: " + strike + "," +
                        "OptionType: " + optionType));
        log.debug("Took {} ms to load option with ticker: {}, expiration: {}, strike: {}, and optionType: {}", System.currentTimeMillis() - start,
                ticker, expiration, strike, optionType.name());
        return option;
    }

    public HistoricalOption addPriceDataToOption(HistoricalOption historicalOption, Collection<OptionPriceData> optionPriceData) {
        return doPriceDataAdd(optionPriceData, TimerUtil.startTimer(), historicalOption);
    }

    public HistoricalOption addPriceDataToOption(Long optionId, OptionPriceData optionPriceData) {
        HistoricalOption option = findById(optionId);
        optionPriceData.setOption(option);
        option.getOptionPriceData().add(optionPriceData);
        return historicalOptionRepository.save(option);
    }

    public HistoricalOption addPriceDataToOption(Long optionId, Collection<OptionPriceData> optionPriceData) {
        TimerUtil timerUtil = TimerUtil.startTimer();
        HistoricalOption option = findById(optionId);
        return doPriceDataAdd(optionPriceData, timerUtil, option);
    }

    private HistoricalOption doPriceDataAdd(Collection<OptionPriceData> optionPriceData, TimerUtil timerUtil, HistoricalOption option) {
        HistoricalOption saved = option;
        log.debug("Adding new price data {} to option {}", optionPriceData, option);
        Set<OptionPriceData> priceDataCopy = new HashSet<>(optionPriceData);
        priceDataCopy.removeIf(data -> option.getOptionPriceData()
                .stream()
                .anyMatch(x -> data.getTradeDate().equals(x.getTradeDate())));
        priceDataCopy.forEach(data -> data.setOption(option));
        if (priceDataCopy.size() == 0) {
            log.debug("Price data for option {} at trade date {} already exists. Skipping addition...", option, optionPriceData.stream().findFirst().get().getTradeDate());
        } else {
            option.getOptionPriceData().addAll(priceDataCopy);
            saved = historicalOptionRepository.save(option);
        }
        log.debug("Took {} ms to add price data {} to option {}", timerUtil.stop(), optionPriceData, option);
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

    public Long countOptionsLoadedOnTradeDate(LocalDate tradeDate) {
        return optionPriceDataRepository.countAllByTradeDate(tradeDate);
    }

    public Set<LocalDate> getExpirationDatesAtStartDate(String ticker, LocalDate startDate) {
        if (startDate == null) {
            return Collections.emptySet();
        }
        return historicalOptionJDBCRepository.getExpirationDatesForOptionsAfterDate(ticker, startDate);
    }

    private Set<HistoricalOption> filterPriceDataBetweenDates(Set<HistoricalOption> historicalOptions, LocalDate startDate, LocalDate endDate) {
        if (endDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("End date cannot be after today.");
        }
        if (historicalOptions != null && !historicalOptions.isEmpty()) {
            historicalOptions.forEach(ho -> ho.getOptionPriceData().removeIf(priceData -> !((startDate.isEqual(priceData.getTradeDate()) || endDate.isEqual(priceData.getTradeDate()))
                    || (startDate.isBefore(priceData.getTradeDate()) && endDate.isAfter(priceData.getTradeDate())))));
            return historicalOptions.stream().filter(x -> !x.getOptionPriceData().isEmpty()).collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }
}
