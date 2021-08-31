package com.dpgrandslam.stockdataservice.domain.model.options;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "option")
@JsonIgnoreProperties(ignoreUnknown = true)
@Table(indexes = {
        @Index(name = "idx_optionId_tradeDate", columnList = "option_id, trade_date", unique = true),
        @Index(name = "idx_optionId", columnList = "option_id")
})
public class OptionPriceData {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    @JsonIgnore
    private Long id;

    @EqualsAndHashCode.Include
    private Double lastTradePrice;

    @EqualsAndHashCode.Include
    private Double bid;

    @EqualsAndHashCode.Include
    private Double ask;

    @EqualsAndHashCode.Include
    private Integer volume;

    @EqualsAndHashCode.Include
    private Integer openInterest;

    @EqualsAndHashCode.Include
    private Double impliedVolatility;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id")
    @JsonIgnore
    @Cascade(org.hibernate.annotations.CascadeType.ALL)
    private HistoricalOption option;

    @EqualsAndHashCode.Include
    private Timestamp dataObtainedDate;

    @Column(name = "trade_date")
    @EqualsAndHashCode.Include
    private LocalDate tradeDate;

    @Transient
    public double getMarketPrice() {
        return BigDecimal.valueOf((bid + ask) / 2.0)
                .setScale(2, RoundingMode.UP)
                .doubleValue();
    }
}
