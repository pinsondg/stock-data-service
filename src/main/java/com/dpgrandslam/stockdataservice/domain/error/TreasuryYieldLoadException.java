package com.dpgrandslam.stockdataservice.domain.error;

import java.time.DayOfWeek;
import java.time.LocalDate;

public class TreasuryYieldLoadException extends RuntimeException {

    private final LocalDate date;

    public TreasuryYieldLoadException(LocalDate date, String message) {
        super(message);
        this.date = date;
    }

    public TreasuryYieldLoadException(LocalDate date) {
        super();
        this.date = date;
    }

    public TreasuryYieldLoadException(LocalDate date, String message, Throwable e) {
        super(message, e);
        this.date = date;
    }

    @Override
    public String getMessage() {
        String defaultMessage = "Could not load 10 yr treasury yield for date " + date.toString() + ".";
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            defaultMessage += " Failure most likely due to the date being a weekend.";
        }
        return defaultMessage + (super.getMessage() != null ? " " + super.getMessage() : "");
    }
}
