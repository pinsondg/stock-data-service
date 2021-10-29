package com.dpgrandslam.stockdataservice.adapter.repository.mapper;

import com.dpgrandslam.stockdataservice.domain.model.options.HistoricalOption;
import com.dpgrandslam.stockdataservice.domain.model.options.Option;
import com.dpgrandslam.stockdataservice.domain.model.options.OptionPriceData;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class HistoricalOptionResultSetExtractor implements ResultSetExtractor<Set<HistoricalOption>> {
    @Override
    public Set<HistoricalOption> extractData(ResultSet rs) throws SQLException, DataAccessException {
        final Map<Long, List<OptionPriceData>> optionPriceDataMap = new HashMap<>();
        Set<HistoricalOption> historicalOptions = new HashSet<>();
        while (rs.next()) {
            OptionPriceData opd = OptionPriceData.builder()
                    .id(rs.getLong("id"))
                    .ask(rs.getDouble("ask"))
                    .bid(rs.getDouble("bid"))
                    .dataObtainedDate(rs.getTimestamp("data_obtained_date"))
                    .impliedVolatility(rs.getDouble("implied_volatility"))
                    .lastTradePrice(rs.getDouble("last_trade_price"))
                    .openInterest(rs.getInt("open_interest"))
                    .tradeDate(rs.getDate("trade_date").toLocalDate())
                    .volume(rs.getInt("volume"))
                    .build();
            HistoricalOption ho = HistoricalOption.builder()
                    .ticker(rs.getString("ticker"))
                    .strike(rs.getDouble("strike"))
                    .optionType(Option.OptionType.valueOf(rs.getString("option_type")))
                    .expiration(rs.getDate("expiration").toLocalDate())
                    .build();
            Long id = rs.getLong("option_id");
            ho.setId(id);
            optionPriceDataMap.computeIfAbsent(id, x -> new ArrayList<>()).add(opd);
            historicalOptions.add(ho);
        }
        historicalOptions.forEach(option -> option.initializeHistoricalPriceData(optionPriceDataMap.get(option.getId())));
        return historicalOptions;
    }
}
