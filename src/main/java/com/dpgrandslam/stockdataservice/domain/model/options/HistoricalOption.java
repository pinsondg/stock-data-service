package com.dpgrandslam.stockdataservice.domain.model.options;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.*;

@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(indexes = {
        @Index(name = "idx_strk_expr_tkr_type", columnList = "strike, expiration, ticker, option_type", unique = true),
        @Index(name = "idx_tkr", columnList = "ticker")
})
public class HistoricalOption extends Option {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "option_id")
    @EqualsAndHashCode.Include
    @JsonIgnore
    @Getter
    @Setter
    private Long id;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "option", fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("tradeDate DESC")
    @EqualsAndHashCode.Include
    @JsonIgnore
    @ToString.Exclude
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
        if (priceData != null) {
            priceData.forEach(item -> {
                item.setOption(this);
            });
            this.historicalPriceData = new LinkedList<>(priceData);
        } else {
            this.historicalPriceData = new LinkedList<>();
        }
    }

    public static HistoricalOption fromCacheableHistoricalOption(CacheableHistoricalOption ho) {
        HistoricalOption historicalOption = new HistoricalOption();
        historicalOption.setOptionPriceData(ho.getOptionPriceData());
        historicalOption.setOptionType(ho.getOptionType());
        historicalOption.setId(ho.getId());
        historicalOption.setExpiration(ho.getExpiration());
        historicalOption.setStrike(ho.getStrike());
        historicalOption.setTicker(ho.getTicker());
        return historicalOption;
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class CacheableHistoricalOption extends Option {

        private List<OptionPriceData> optionPriceData;
        private Long id;

        private CacheableHistoricalOption(HistoricalOption historicalOption) {
            super(historicalOption.getTicker(),
                    historicalOption.getOptionType(),
                    historicalOption.getExpiration(),
                    historicalOption.getStrike());
            this.id = historicalOption.id;
            setOptionPriceData(historicalOption.getOptionPriceData());
        }

        @Override
        public Collection<OptionPriceData> getOptionPriceData() {
            return optionPriceData;
        }

        @Override
        public OptionPriceData getMostRecentPriceData() {
            return optionPriceData.stream()
                    .min(Comparator.comparing(OptionPriceData::getDataObtainedDate))
                    .orElse(null);
        }

        @Override
        public void setOptionPriceData(Collection<OptionPriceData> optionPriceData) {
            List<OptionPriceData> opd = new LinkedList<>(optionPriceData);
            opd.forEach(d -> d.setOption(null));
            this.optionPriceData = opd;
        }

        public static CacheableHistoricalOption fromHistoricalOption(HistoricalOption historicalOption) {
            return new CacheableHistoricalOption(historicalOption);
        }
    }
}
