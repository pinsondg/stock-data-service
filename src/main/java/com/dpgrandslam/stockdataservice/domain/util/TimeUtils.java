package com.dpgrandslam.stockdataservice.domain.util;

import com.dpgrandslam.stockdataservice.domain.model.Holiday;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class TimeUtils {

    private static final String HOLIDAYS_FILE_PATH = "data/market-holidays.csv";

    private Set<Holiday> stockMarketHolidays;

    public TimeUtils() {
        Assert.notNull(getStockMarketHolidays(), "Holidays are null on startup.");
    }

    public LocalDateTime getNowAmericaNewYork() {
        return LocalDateTime.now(ZoneId.of("America/New_York"));
    }

    public Set<Holiday> getStockMarketHolidays() {
        if (stockMarketHolidays == null) {
            try {
                stockMarketHolidays = new HashSet<>(parseStockMarketHolidays());
            } catch (IOException e) {
                log.error("Error reading holiday file.", e);
            }
        }
        return stockMarketHolidays;
    }

    public LocalDate getCurrentOrLastTradeDate(LocalDateTime dateTime) {
        LocalDateTime now = dateTime;
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

    public LocalDate getCurrentOrLastTradeDate() {
        return getCurrentOrLastTradeDate(this.getNowAmericaNewYork());
    }

    public LocalDate getStartDayOfCurrentTradeWeek() {
        return getStartDayOfCurrentTradeWeek(0);
    }

    public LocalDate getStartDayOfCurrentTradeWeek(int weekOffset) {
        if (weekOffset < 0) {
            throw new IllegalArgumentException("Week offset must be greater than 1");
        }
        LocalDate now = this.getNowAmericaNewYork().toLocalDate();
        for (int i = weekOffset; i >= 0; i--) {
            if (i == weekOffset) {
                now = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            } else {
                now = now.with(TemporalAdjusters.previous(DayOfWeek.MONDAY));
            }
            while (isStockMarketHoliday(now)) {
                now = now.plusDays(1);
            }
        }
        if (now.isAfter(LocalDate.now())) {
            return null;
        }
        return now;
    }

    public boolean isStockMarketHoliday(LocalDate date) {
        Set<Holiday> stockMarketHolidays = getStockMarketHolidays();
        if (stockMarketHolidays != null) {
            return stockMarketHolidays.stream().map(Holiday::getDate).anyMatch(date::equals);
        }
        return false;
    }

    public boolean isTradingOpenOnDay(LocalDate date) {
        return !isStockMarketHoliday(date) && isWeekday(date.getDayOfWeek());
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

