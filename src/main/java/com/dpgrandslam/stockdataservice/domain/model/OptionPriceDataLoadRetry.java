package com.dpgrandslam.stockdataservice.domain.model;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "options_load_retry", indexes = {@Index(name = "idx_optionTick_optionExpr_tradeDate",
        columnList = "option_ticker, option_expiration, trade_date", unique = true)})
@Builder
@NoArgsConstructor
@EqualsAndHashCode
@AllArgsConstructor
public class OptionPriceDataLoadRetry {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long retryId;

    @Column(name = "option_ticker")
    private String optionTicker;

    @Column(name = "option_expiration")
    private LocalDate optionExpiration;

    private Integer retryCount;

    @Column(name = "trade_date")
    private LocalDate tradeDate;

    @CreationTimestamp
    private Timestamp firstFailure;

    @UpdateTimestamp
    private Timestamp lastFailure;
}
