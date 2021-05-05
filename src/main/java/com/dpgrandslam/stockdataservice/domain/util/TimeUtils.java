package com.dpgrandslam.stockdataservice.domain.util;

import com.dpgrandslam.stockdataservice.domain.model.Holiday;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class TimeUtils {

    private static final String HOLIDAYS_FILE_PATH = "data/market-holidays.csv";

    private List<Holiday> stockMarketHolidays;


    public TimeUtils() {
        Assert.notNull(getStockMarketHolidays(), "Holidays are null on startup.");
    }

    public LocalDateTime getNowAmericaNewYork() {
        return LocalDateTime.now(ZoneId.of("America/New_York"));
    }

    public List<Holiday> getStockMarketHolidays() {
        if (stockMarketHolidays == null) {
            try {
                stockMarketHolidays = parseStockMarketHolidays();
            } catch (IOException e) {
                log.error("Error reading holiday file.", e);
            }
        }
        return stockMarketHolidays;
    }

    public LocalDate getLastTradeDate() {
        LocalDateTime now = this.getNowAmericaNewYork();
        // If now is before a weekday at 9:30am set to previous day
        if (isWeekday(now.getDayOfWeek()) && now.toLocalTime().compareTo(LocalTime.of(9, 30)) < 0) {
            now = now.minusDays(1);
        }
        //If it is a holiday subtract a day
        if (this.isStockMarketHoliday(now.toLocalDate())) {
            now = now.minusDays(1);
        }
        //If it is a weekend go until Friday
        while (now.getDayOfWeek() == DayOfWeek.SUNDAY || now.getDayOfWeek() == DayOfWeek.SATURDAY) {
            now = now.minusDays(1);
        }
        return now.toLocalDate();
    }

    public boolean isStockMarketHoliday(LocalDate date) {
        List<Holiday> stockMarketHolidays = getStockMarketHolidays();
        if (stockMarketHolidays != null) {
            return stockMarketHolidays.stream().map(Holiday::getDate).anyMatch(date::equals);
        }
        return false;
    }

    public boolean isTodayAmericaNewYorkStockMarketHoliday() {
        return isStockMarketHoliday(getNowAmericaNewYork().toLocalDate());
    }

    private List<Holiday> parseStockMarketHolidays() throws IOException {
        List<Holiday> holidays = new ArrayList<>();
        try (BufferedReader reader =  new BufferedReader(new FileReader(FileUtils.getResourceFile(HOLIDAYS_FILE_PATH)))) {
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
        }
        return holidays;
    }

    private boolean isWeekday(DayOfWeek dayOfWeek) {
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }
}

