package com.dpgrandslam.stockdataservice.domain.event;

import com.dpgrandslam.stockdataservice.domain.model.stock.TrackedStock;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Collection;

@Getter
public class TrackedStockAddedEvent extends ApplicationEvent {

    private final Collection<TrackedStock> trackedStocks;

    public TrackedStockAddedEvent(Object source, Collection<TrackedStock> trackedStocks) {
        super(source);
        this.trackedStocks = trackedStocks;
    }

}
