package com.dpgrandslam.stockdataservice.domain.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class TimeUtils {

    public LocalDateTime getNowAmericaNewYork() {
        return LocalDateTime.now(ZoneId.of("America/New_York"));
    }
}
