package com.dpgrandslam.stockdataservice.domain.error;

import java.time.DayOfWeek;
import java.time.LocalDate;

public class YahooFinanceQuoteLoadException extends RuntimeException {

    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String ticker;

    public YahooFinanceQuoteLoadException(String ticker, LocalDate startDate, LocalDate endDate, String message) {
        super(message);
        this.startDate = startDate;
        this.endDate = endDate;
        this.ticker = ticker;
    }

    public YahooFinanceQuoteLoadException(String ticker, LocalDate startDate, LocalDate endDate) {
        super();
        this.startDate = startDate;
        this.endDate = endDate;
        this.ticker = ticker;

    }

    public YahooFinanceQuoteLoadException(String ticker, LocalDate startDate, LocalDate endDate, Throwable e) {
        super(e);
        this.startDate = startDate;
        this.endDate = endDate;
        this.ticker = ticker;
    }

    public YahooFinanceQuoteLoadException(String ticker, LocalDate startDate, LocalDate endDate, String message, Throwable e) {
        super(message, e);
        this.startDate = startDate;
        this.endDate = endDate;
        this.ticker = ticker;
    }

    @Override
    public String getMessage() {
        String defaultMessage = "Could not load yahoo finance quote for ticker " + ticker +" for date " + startDate.toString() + ".";
        if (startDate.getDayOfWeek() == DayOfWeek.SATURDAY || startDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            defaultMessage += " Failure most likely due to the date being a weekend.";
        }
        return defaultMessage + (super.getMessage() != null ? " " + super.getMessage() : "");
    }
}
