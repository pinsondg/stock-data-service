package com.dpgrandslam.stockdataservice.domain.service;

import com.dpgrandslam.stockdataservice.domain.model.options.Option;
import com.dpgrandslam.stockdataservice.domain.model.options.OptionPriceData;
import com.dpgrandslam.stockdataservice.domain.model.options.OptionsChain;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Timestamp;
import java.time.*;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class OptionsChainLoadService {

    @Autowired
    private HistoricOptionsDataService historicOptionsDataService;

    /**
     * Gets the a live OptionChain for a ticker that is for the closest expiration date.
     * @param ticker the ticker to search
     * @return the live option data for the closest expiration
     */
    public abstract OptionsChain loadLiveOptionsChainForClosestExpiration(String ticker);

    /**
     * Loads the full options chain (List of OptionsChain) for a ticker. A full options chain is the complete chain for
     * every available expiration date.
     *
     * @param ticker the ticker to look for
     * @return a list of OptionsChain
     */
    public abstract List<OptionsChain> loadFullLiveOptionsChain(String ticker);

    /**
     * Loads the live options chain for a ticker and expiration date.
     *
     * @param ticker the ticker to look for
     * @param expirationDate the expiration date of the option
     * @return the live options chain
     */
    public abstract OptionsChain loadLiveOptionsChainForExpirationDate(String ticker, LocalDate expirationDate);

    /**
     * Loads possible expiration dates for the option as of today.
     *
     * @param ticker the ticker to look for
     * @return a list of expiration dates
     */
    public abstract List<LocalDate> getOptionExpirationDates(String ticker);

    /**
     * Loads the options chain for a specific ticker on a specific expiration date. Includes stored historic options
     * date for the date range given.
     *
     * @param ticker the ticker to load the chain for
     * @param expirationDate the expiration date to load the chain for
     * @param priceDataStart the start date of historic data
     * @param priceDataEnd the end date of historic data, defaults to null
     * @return the OptionsChain containing the historic and live data
     */
    public OptionsChain loadCompleteOptionsChainForExpirationDateWithPriceDataInRange(String ticker,
                                                                                      LocalDate expirationDate,
                                                                                      Timestamp priceDataStart,
                                                                                      Timestamp priceDataEnd) {
        OptionsChain optionsChain = new OptionsChain(ticker, expirationDate);
        if (priceDataEnd == null) {
            optionsChain = loadLiveOptionsChainForExpirationDate(ticker, expirationDate);
            priceDataEnd = Timestamp.from(Instant.now());
        }
        List<Option> historicOptions = historicOptionsDataService.findOptions(ticker, expirationDate)
                .collect(Collectors.toList());
        Timestamp finalEndDate = priceDataEnd;
        filterOptionsDataByDates(priceDataStart, historicOptions, finalEndDate);
        optionsChain.addOptions(historicOptions);
        return optionsChain;
    }

    /**
     * Loads the options chain for a specific ticker on a specific expiration date. Includes all historic data and live data.
     * @param ticker the stock ticker to load options for
     * @param expirationDate the expiration date to load for
     * @return the options chain
     */
    public OptionsChain loadOptionsChainForExpirationDateWithAllData(String ticker, LocalDate expirationDate) {
        OptionsChain liveOptionsChain = loadLiveOptionsChainForExpirationDate(ticker, expirationDate);
        liveOptionsChain.addOptions(historicOptionsDataService.findOptions(ticker, expirationDate).collect(Collectors.toList()));
        return liveOptionsChain;
    }

    public List<OptionsChain> loadFullOptionsChainWithAllData(String ticker) {
        List<OptionsChain> fullChain = loadFullLiveOptionsChain(ticker);
        combineLiveAndHistoricData(ticker, fullChain);
        return fullChain;
    }

    public List<OptionsChain> loadFullOptionsChainWithAllDataBetweenDates(String ticker, Timestamp start, Timestamp end) {
        List<OptionsChain> fullChain = loadFullLiveOptionsChain(ticker);
        if (end == null) {
            end = Timestamp.from(Instant.now());
        }
        combineLiveAndHistoricData(ticker, fullChain);
        Timestamp finalEnd = end;
        fullChain.forEach(chain -> filterOptionsDataByDates(start, chain.getAllOptions(), finalEnd));
        return fullChain;
    }

    private void filterOptionsDataByDates(Timestamp priceDataStart, Collection<Option> historicOptions, Timestamp finalEndDate) {
        historicOptions.forEach(option -> {
            List<OptionPriceData> filteredData = option.getOptionPriceData().stream()
                    .filter(data -> data.getDataObtainedDate().compareTo(priceDataStart) >= 0
                            && data.getDataObtainedDate().compareTo(finalEndDate) <= 0).collect(Collectors.toList());
            option.setOptionPriceData(filteredData);
        });
    }

    private void combineLiveAndHistoricData(String ticker, List<OptionsChain> fullChain) {
        historicOptionsDataService.findOptions(ticker).forEach(option -> {
            Optional<OptionsChain> found = fullChain.stream()
                    .filter(x -> x.getTicker().equalsIgnoreCase(option.getTicker())
                            && x.getExpirationDate().equals(option.getExpiration()))
                    .findFirst();
            if (found.isPresent()) {
                found.get().addOption(option);
            } else {
                OptionsChain optionsChain = OptionsChain.builder()
                        .expirationDate(option.getExpiration())
                        .ticker(option.getTicker())
                        .build();
                optionsChain.addOption(option);
                fullChain.add(optionsChain);
            }
        });
    }

    protected LocalDate getLastTradeDate() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("America/New_York"));
        // If now is before a weekday at 9:30am set to previous day
        if (isWeekday(now.getDayOfWeek()) && now.getHour() <= 9 && now.getMinute() < 30) {
            now = now.minusDays(1);
        }
        //If it is a weekend go until Friday
        while (now.getDayOfWeek() == DayOfWeek.SUNDAY || now.getDayOfWeek() == DayOfWeek.SATURDAY) {
            now = now.minusDays(1);
        }
        return now.toLocalDate();
    }

    private boolean isWeekday(DayOfWeek dayOfWeek) {
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }
}
