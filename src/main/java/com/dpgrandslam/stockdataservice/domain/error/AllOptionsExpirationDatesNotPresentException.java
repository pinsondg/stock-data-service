package com.dpgrandslam.stockdataservice.domain.error;

import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
public class AllOptionsExpirationDatesNotPresentException extends Exception {

    public List<LocalDate> missingDates;

    public AllOptionsExpirationDatesNotPresentException(String message, List<LocalDate> missingDates) {
        super(message);
        this.missingDates = missingDates;
    }

    public AllOptionsExpirationDatesNotPresentException(List<LocalDate> missingDates) {
        super();
        this.missingDates = missingDates;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder("Found missing options dates: ");
        for (int i = 0; i < missingDates.size(); i++) {
            sb.append(missingDates.get(i));
            if (i != missingDates.size() - 1) {
                sb.append(", ");
            }
        }
        if (super.getMessage() != null) {
            sb.append(". Message: ").append(super.getMessage());
        }
        return sb.toString();
    }
}
