package com.dpgrandslam.stockdataservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Holiday {

    private String name;
    private LocalDate date;

    public void setDate(String dateStr) {
        this.date = LocalDate.parse(dateStr);
    }
}
