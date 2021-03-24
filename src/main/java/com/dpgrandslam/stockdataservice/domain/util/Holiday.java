package com.dpgrandslam.stockdataservice.domain.util;

import lombok.Data;

import java.time.LocalDate;

@Data
public class Holiday {

    private String name;
    private LocalDate date;

    public void setDate(String dateStr) {
        this.date = LocalDate.parse(dateStr);
    }
}
