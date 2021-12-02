package com.dpgrandslam.stockdataservice.adapter.repository.mapper;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

public class ExpirationDateResultSetExtractor implements ResultSetExtractor<Set<LocalDate>> {

    @Override
    public Set<LocalDate> extractData(ResultSet rs) throws SQLException, DataAccessException {
        Set<LocalDate> dates = new HashSet<>();
        while (rs.next()) {
            dates.add(rs.getDate("expiration").toLocalDate());
        }
        return dates;
    }
}
