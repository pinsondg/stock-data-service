package com.dpgrandslam.stockdataservice.domain.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class TimeUtils {

    private static final String HOLIDAYS_FILE_PATH = "data/market-holidays.csv";

    private List<Holiday> stockMarketHolidays;

    public LocalDateTime getNowAmericaNewYork() {
        return LocalDateTime.now(ZoneId.of("America/New_York"));
    }

    public List<Holiday> getStockMarketHolidays() {
        if (stockMarketHolidays == null) {
            try {
                stockMarketHolidays = parseStockMarketHolidays();
            } catch (IOException e) {
                log.error("Error reading holiday file.");
            }
        }
        return stockMarketHolidays;
    }

    public boolean isStockMarketHoliday(LocalDate date) {
        return getStockMarketHolidays().stream().map(Holiday::getDate).anyMatch(date::equals);
    }

    public boolean isTodayAmericaNewYorkStockMarketHoliday() {
        return isStockMarketHoliday(getNowAmericaNewYork().toLocalDate());
    }

    private List<Holiday> parseStockMarketHolidays() throws IOException {
        List<Holiday> holidays = new ArrayList<>();
        BufferedReader reader =  new BufferedReader(new FileReader(new ClassPathResource(HOLIDAYS_FILE_PATH).getFile()));
        String line = reader.readLine();
        //skip headers
        line = reader.readLine();
        while (line != null) {
            Holiday holiday = new Holiday();
            String[] vals = line.split(",");
            holiday.setName(vals[0]);
            holiday.setDate(vals[1]);
            holidays.add(holiday);
            line = reader.readLine();
        }
        return holidays;
    }
}

