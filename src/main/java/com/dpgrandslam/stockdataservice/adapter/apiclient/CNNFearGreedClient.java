package com.dpgrandslam.stockdataservice.adapter.apiclient;

import com.dpgrandslam.stockdataservice.domain.model.CNNFearGreedResponse;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

import java.time.LocalDate;

@Headers("Accept: application/json")
public interface CNNFearGreedClient {

    @RequestLine("GET /index/fearandgreed/graphdata/{date}")
    CNNFearGreedResponse getFearGreedData(@Param("date") String date);

}
