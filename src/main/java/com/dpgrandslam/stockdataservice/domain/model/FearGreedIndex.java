package com.dpgrandslam.stockdataservice.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(indexes = {
        @Index(name = "idx_trade_date", columnList = "trade_date", unique = true)
})
@ToString
@NoArgsConstructor
public class FearGreedIndex {

    @Id
    @GeneratedValue
    @JsonIgnore
    @Getter
    @Setter
    private Long id;

    @Getter
    @Setter
    @NotNull
    private Integer value;

    @Getter
    @Setter
    @Column(name = "trade_date")
    @NotNull
    private LocalDate tradeDate;

    @Getter
    @Setter
    @CreationTimestamp
    private Timestamp createTime;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        FearGreedIndex that = (FearGreedIndex) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, tradeDate);
    }
}
