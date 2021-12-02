package com.dpgrandslam.stockdataservice.domain.model.stock;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDate;

@Entity
@Data
@ToString
@EqualsAndHashCode
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TrackedStock {

    @Id
    private String ticker;

    private String name;
    private LocalDate optionsHistoricDataStartDate;
    private LocalDate lastOptionsHistoricDataUpdate;

    @Column(name = "active")
    private boolean active;
}
