package com.dpgrandslam.stockdataservice.domain.model.options;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.*;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Data
@ToString(callSuper = true)
@Entity
@Table(indexes = {
        @Index(name = "idx_strk_expr_tkr_type", columnList = "strike, expiration, ticker, option_type", unique = true),
        @Index(name = "idx_tkr", columnList = "ticker")
})
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class HistoricalOption extends Option {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "option_id")
    @EqualsAndHashCode.Include
    @JsonIgnore
    private Long id;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "option", fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("tradeDate DESC")
    @EqualsAndHashCode.Include
    @JsonIgnore
    @ToString.Exclude
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private List<OptionPriceData> historicalPriceData;

    public HistoricalOption() {
        historicalPriceData = new LinkedList<>();
    }

    @Builder
    public HistoricalOption(String ticker, OptionType optionType, LocalDate expiration, Double strike, Set<OptionPriceData> historicalPriceData) {
        super.ticker =  ticker;
        super.optionType = optionType;
        super.expiration = expiration;
        super.strike = strike;
        if (historicalPriceData == null) {
            this.historicalPriceData = new LinkedList<>();
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
        historicalPriceData = new LinkedList<>(optionPriceData);
    }

    public void initializeHistoricalPriceData(Collection<OptionPriceData> priceData) {
        priceData.forEach(item -> {
            item.setOption(this);
        });
        this.historicalPriceData = new LinkedList<>(priceData);
    }
}
