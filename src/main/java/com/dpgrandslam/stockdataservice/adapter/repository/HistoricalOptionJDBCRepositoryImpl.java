package com.dpgrandslam.stockdataservice.adapter.repository;

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

    private static final String FIND_BETWEEN_DATES_SQL = "SELECT * FROM historical_option ho left join option_price_data pd on ho.option_id = pd.option_id WHERE pd.trade_date >= ? and pd.trade_date <= ?";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Set<HistoricalOption> findByTickerBetweenDates(String ticker, LocalDate startDate, LocalDate endDate) {
        return jdbcTemplate.query(FIND_BETWEEN_DATES_SQL, (ps) -> {
            ps.setDate(1, Date.valueOf(startDate));
            ps.setDate(2, Date.valueOf(endDate));
        }, new HistoricalOptionResultSetExtractor());
    }
}
