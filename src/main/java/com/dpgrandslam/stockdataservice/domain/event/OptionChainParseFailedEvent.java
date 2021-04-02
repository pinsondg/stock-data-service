package com.dpgrandslam.stockdataservice.domain.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;

@Getter
public class OptionChainParseFailedEvent extends ApplicationEvent {

    private String ticker;
    private LocalDate expiration;

    public OptionChainParseFailedEvent(Object source, String ticker, LocalDate expiration) {
        super(source);
        this.ticker = ticker;
        this.expiration = expiration;
    }

}
