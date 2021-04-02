package com.dpgrandslam.stockdataservice.domain.model.stock;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDate;

@Entity
@Data
@ToString
@EqualsAndHashCode
public class TrackedStock {

    @Id
    private String ticker;

    private String name;
    private LocalDate optionsHistoricDataStartDate;
    private LocalDate lastOptionsHistoricDataUpdate;

    @Column(name = "active")
    private boolean active;
}
