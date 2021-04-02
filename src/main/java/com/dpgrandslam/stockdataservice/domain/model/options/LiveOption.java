package com.dpgrandslam.stockdataservice.domain.model.options;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Data
public class LiveOption extends Option {

    @EqualsAndHashCode.Include
    @JsonIgnore
    private OptionPriceData livePriceData;

    public LiveOption() {
        livePriceData = new OptionPriceData();
    }

    @Builder
    public LiveOption(String ticker, Double strike, OptionType optionType, LocalDate expiration, OptionPriceData optionPriceData) {
        super.ticker = ticker;
        super.strike = strike;
        super.expiration = expiration;
        super.optionType = optionType;
        this.livePriceData = optionPriceData;
    }

    @Override
    public Collection<OptionPriceData> getOptionPriceData() {
        Collection<OptionPriceData> optionPriceData = new HashSet<>();
        if (livePriceData != null) {
            optionPriceData.add(livePriceData);
        }
        return optionPriceData;
    }

    @Override
    public OptionPriceData getMostRecentPriceData() {
        return livePriceData;
    }

    @Override
    public void setOptionPriceData(Collection<OptionPriceData> optionPriceData) {
        if (optionPriceData.size() > 1) {
            throw new IllegalArgumentException("Live option can only contain one price data point.");
        }
        livePriceData = optionPriceData.stream().findFirst().orElse(null);
    }

    public HistoricalOption toHistoricalOption() {
        return HistoricalOption.builder()
                .expiration(expiration)
                .ticker(ticker)
                .strike(strike)
                .historicalPriceData(new HashSet<>(Arrays.asList(livePriceData)))
                .optionType(optionType)
                .build();
    }
}
