package com.dpgrandslam.stockdataservice.adapter.apiclient.tiingo;

import com.dpgrandslam.stockdataservice.domain.model.tiingo.TiingoMetaDataResponse;
import com.dpgrandslam.stockdataservice.domain.model.tiingo.TiingoStockEndOfDayResponse;
import com.dpgrandslam.stockdataservice.domain.model.tiingo.TiingoStockLiveDataResponse;
import com.dpgrandslam.stockdataservice.domain.model.tiingo.TiingoStockSearchResponse;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

import java.util.List;
import java.util.Set;

@Headers("Accept: application/json")
public interface TiingoApiClient {

    @RequestLine("GET /tiingo/utilities/search?query={query}")
    List<TiingoStockSearchResponse> searchStock(@Param("query") String query);

    @RequestLine("GET /tiingo/daily/{ticker}/prices")
    Set<TiingoStockEndOfDayResponse> getEndOfDayInfo(@Param("ticker") String ticker);

    @RequestLine("GET /tiingo/daily/{ticker}/prices?startDate={startDate}&endDate={endDate}")
    List<TiingoStockEndOfDayResponse> getHistoricalInfo(@Param("ticker") String ticker,
                                                        @Param("startDate") String startDate,
                                                        @Param("endDate") String endDate);
    @RequestLine("GET /iex/{ticker}")
    TiingoStockLiveDataResponse getLiveStockData(@Param("ticker") String ticker);

    @RequestLine("GET /tiingo/daily/{ticker}")
    TiingoMetaDataResponse getStockMetaData(@Param("ticker") String ticker);

}
