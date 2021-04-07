package com.dpgrandslam.stockdataservice.domain.model.options;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor
@MappedSuperclass
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class Option {

    @Column(nullable = false)
    @EqualsAndHashCode.Include
    protected String ticker;

    @Enumerated(EnumType.STRING)
    @Column(name = "option_type", nullable = false)
    @EqualsAndHashCode.Include
    protected OptionType optionType;

    @Column(nullable = false)
    @EqualsAndHashCode.Include
    protected LocalDate expiration;

    @Column(nullable = false)
    @EqualsAndHashCode.Include
    protected Double strike;

    @Transient
    public boolean isExpired() {
        return expiration.compareTo(LocalDate.now()) < 0;
    }

    @Transient
    public abstract Collection<OptionPriceData> getOptionPriceData();

    @Transient
    @JsonIgnore
    public abstract OptionPriceData getMostRecentPriceData();

    public abstract void setOptionPriceData(Collection<OptionPriceData> optionPriceData);

    public HistoricalOption toHistoricalOption() {
        return HistoricalOption.builder()
                .optionType(this.optionType)
                .strike(strike)
                .ticker(ticker)
                .expiration(expiration)
                .historicalPriceData(new HashSet<>(getOptionPriceData()))
                .build();
    }

    @Transient
    @JsonIgnore
    public OptionChainKey getOptionChainKey() {
        return new OptionChainKey(this.strike, this.optionType);
    }

    public enum OptionType {
        CALL, PUT;
    }
}
