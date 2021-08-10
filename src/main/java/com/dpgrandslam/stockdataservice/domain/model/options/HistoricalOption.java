package com.dpgrandslam.stockdataservice.domain.model.options;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.*;

@Data
@ToString(callSuper = true)
@Entity
@Table(indexes = {
        @Index(name = "idx_strk_expr_tkr_type", columnList = "strike, expiration, ticker, option_type", unique = true),
        @Index(name = "idx_tkr", columnList = "ticker")
})
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
public class HistoricalOption extends Option {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "option_id")
    @EqualsAndHashCode.Include
    @JsonIgnore
    private Long id;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "option", fetch = FetchType.EAGER, orphanRemoval = true)
    @OrderBy("tradeDate DESC")
    @EqualsAndHashCode.Include
    @JsonIgnore
    @ToString.Exclude
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        HistoricalOption that = (HistoricalOption) o;

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return 889797582;
    }
}
