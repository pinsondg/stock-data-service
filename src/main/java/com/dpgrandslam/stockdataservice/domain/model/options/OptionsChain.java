package com.dpgrandslam.stockdataservice.domain.model.options;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class OptionsChain {

    @Getter
    private final String ticker;

    @Getter
    private final LocalDate expirationDate;

    @JsonIgnore
    private Map<OptionChainKey, Option> options;

    @Builder
    public OptionsChain(String ticker, LocalDate expirationDate) {
        this.ticker = ticker;
        this.expirationDate = expirationDate;
        options = new HashMap<>();
    }

    /**
     * Adds an option to the option chain. If the option already exists in the chain, the option price data
     * is added to the option
     *
     * @param option the option to add
     */
    public void addOption(Option option) {
        if (option.getExpiration() == null && option.getTicker() == null) {
            option.setTicker(ticker);
            option.setExpiration(expirationDate);
        }
        if (!option.getExpiration().equals(expirationDate) || !ticker.equalsIgnoreCase(option.getTicker()))  {
            throw new IllegalArgumentException("Option provided either has an incorrect expirationDate or ticker to be a part of this options chain. " + option);
        }
        if (options.containsKey(option.getOptionChainKey())) {
            // Option already exists
            Option currentData = options.get(option.getOptionChainKey());
            // If it is a live option, convert to historic option
            if (currentData instanceof LiveOption) {
                currentData = ((LiveOption) currentData).toHistoricalOption();
                options.put(option.getOptionChainKey(), currentData);
            }
            // Add new data
            currentData.getOptionPriceData().addAll(option.getOptionPriceData());
        } else {
            options.put(option.getOptionChainKey(), option);
        }
    }

    /**
     * Adds a collection of options to the chain.
     * @param options the options to add
     */
    public void addOptions(Collection<Option> options) {
        options.forEach(this::addOption);
    }

    /**
     * Get an option at a particular strike and type
     * @param strike the strike of the option
     * @param optionType the type of option
     * @return the Option that was found
     */
    public Option getOption(Double strike, Option.OptionType optionType) {
        return getOption(new OptionChainKey(strike, optionType));
    }

    public Option getOption(OptionChainKey key) {
        return options.get(key);
    }

    public Collection<Option> getAllOptions() {
        return options.values();
    }
}
