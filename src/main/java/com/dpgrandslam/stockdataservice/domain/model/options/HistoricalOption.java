package com.dpgrandslam.stockdataservice.domain.model.options;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Data
@ToString(callSuper = true)
@Entity
@Table(indexes = @Index(name = "idx_strk_expr_tkr_type", columnList = "strike, expiration, ticker, option_type", unique = true))
public class HistoricalOption extends Option {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "option_id")
    @EqualsAndHashCode.Include
    @JsonIgnore
    private Long id;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "option", fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("dataObtainedDate DESC")
    @EqualsAndHashCode.Include
    @JsonIgnore
    @ToString.Exclude
    private Set<OptionPriceData> historicalPriceData;

    public HistoricalOption() {
        historicalPriceData = new HashSet<>();
    }

    @Builder
    public HistoricalOption(String ticker, OptionType optionType, LocalDate expiration, Double strike, Set<OptionPriceData> historicalPriceData) {
        super.ticker =  ticker;
        super.optionType = optionType;
        super.expiration = expiration;
        super.strike = strike;
        if (historicalPriceData == null) {
            this.historicalPriceData = new HashSet<>();
        } else {
            initializeHistoricalPriceData(historicalPriceData);
        }
    }

    @Transient
    @Override
    public Collection<OptionPriceData> getOptionPriceData() {
        return historicalPriceData;
    }

    @Transient
    @Override
    public OptionPriceData getMostRecentPriceData() {
        return historicalPriceData.stream()
                .min(Comparator.comparing(OptionPriceData::getDataObtainedDate))
                .orElse(null);
    }

    @Override
    public void setOptionPriceData(Collection<OptionPriceData> optionPriceData) {
        historicalPriceData = new HashSet<>(optionPriceData);
    }

    public void initializeHistoricalPriceData(Set<OptionPriceData> priceData) {
        priceData.forEach(item -> {
            item.setOption(this);
        });
        this.historicalPriceData = priceData;
    }
}
