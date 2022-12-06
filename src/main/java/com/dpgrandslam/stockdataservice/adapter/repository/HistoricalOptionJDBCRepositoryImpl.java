package com.dpgrandslam.stockdataservice.adapter.repository;

import com.dpgrandslam.stockdataservice.adapter.repository.mapper.ExpirationDateResultSetExtractor;
import com.dpgrandslam.stockdataservice.adapter.repository.mapper.HistoricalOptionResultSetExtractor;
import com.dpgrandslam.stockdataservice.domain.model.options.HistoricalOption;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class HistoricalOptionJDBCRepositoryImpl implements HistoricalOptionJDBCRepository{

    private static final String FIND_BETWEEN_DATES_TICKER_SQL = "SELECT * FROM historical_option ho left join option_price_data " +
            "pd on ho.option_id = pd.option_id WHERE ho.ticker = ? and pd.trade_date >= ? and pd.trade_date <= ?";
    private static final String FIND_EXPIRATION_AFTER_DATE_SQL = "select distinct expiration from historical_option " +
            "inner join option_price_data opd on historical_option.option_id = opd.option_id where trade_date >= ? " +
            "and ticker = ? order by expiration";
    private static final String FIND_BETWEEN_DATES_SQL = "SELECT * FROM historical_option ho left join " +
            "option_price_data pd on ho.option_id = pd.option_id where pd.trade_date >= ? and pd.trade_date <= ?";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Set<HistoricalOption> findByTickerBetweenDates(final String ticker, LocalDate startDate, LocalDate endDate) {
        return jdbcTemplate.query(FIND_BETWEEN_DATES_TICKER_SQL, (ps) -> {
            ps.setString(1, ticker);
            ps.setDate(2, Date.valueOf(startDate));
            ps.setDate(3, Date.valueOf(endDate));
        }, new HistoricalOptionResultSetExtractor());
    }

    @Override
    public Set<LocalDate> getExpirationDatesForOptionsAfterDate(String ticker, LocalDate date) {
        return jdbcTemplate.query(FIND_EXPIRATION_AFTER_DATE_SQL, (ps) -> {
            ps.setDate(1, Date.valueOf(date));
            ps.setString(2, ticker);
        }, new ExpirationDateResultSetExtractor());
    }

    @Override
    public Set<HistoricalOption> findBetweenDates(LocalDate startDate, LocalDate endDate) {
        return jdbcTemplate.query(FIND_BETWEEN_DATES_SQL, (ps) -> {
            ps.setDate(1, Date.valueOf(startDate));
            ps.setDate(2, Date.valueOf(endDate));
        }, new HistoricalOptionResultSetExtractor());
    }

}
