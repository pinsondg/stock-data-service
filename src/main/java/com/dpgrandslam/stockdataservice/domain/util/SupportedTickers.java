package com.dpgrandslam.stockdataservice.domain.util;

import lombok.Getter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@Getter
public class SupportedTickers {

    private List<String> supportedTickers;

    @PostConstruct
    private void init() throws IOException {
        supportedTickers = buildTickersList();
    }

    public boolean isTickerSupported(String ticker) {
        return Collections.binarySearch(supportedTickers, ticker) >= 0;
    }

    private List<String> buildTickersList() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(new ClassPathResource("data/supported_tickers.csv").getFile()));
        List<String> tickers = new ArrayList<>();
        String line = reader.readLine();
        //skip header
        line = reader.readLine();
        while (line != null)  {
            String[] spilt = line.split(",");
            tickers.add(spilt[0]);
            line = reader.readLine();
        }
        reader.close();
        return tickers;
    }
}
