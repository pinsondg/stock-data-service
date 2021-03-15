package com.dpgrandslam.stockdataservice.domain.model.options;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.sql.Timestamp;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "option")
public class OptionPriceData {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private Long id;

    @EqualsAndHashCode.Include
    private Double marketPrice;

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

}
