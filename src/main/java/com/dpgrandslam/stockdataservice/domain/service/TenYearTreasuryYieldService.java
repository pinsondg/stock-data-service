package com.dpgrandslam.stockdataservice.domain.service;

import com.dpgrandslam.stockdataservice.adapter.apiclient.BasicWebPageLoader;
import com.dpgrandslam.stockdataservice.domain.config.ApiClientConfigurationProperties;
import com.dpgrandslam.stockdataservice.domain.error.TreasuryYieldLoadException;
import com.dpgrandslam.stockdataservice.domain.model.stock.YahooFinanceTenYearTreasuryYield;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenYearTreasuryYieldService {

    private final BasicWebPageLoader webPageLoader;
    private final Cache<LocalDate, YahooFinanceTenYearTreasuryYield> treasuryYieldCache;

    @Autowired
    @Qualifier("YahooFinanceApiClientConfigurationProperties")
    private ApiClientConfigurationProperties clientConfigurationProperties;

    public YahooFinanceTenYearTreasuryYield getTreasuryYieldForDate(LocalDate date) {
        final String url = clientConfigurationProperties.getUrlAndPort() + "/quote/%5ETNX/history?period1="
                + convertDate(date) + "&period2=" + convertDate(date.plusDays(1))
                + "&interval=1d&filter=history&frequency=1d&includeAdjustedClose=true";
        try {
            return treasuryYieldCache.get(date, (d) -> parseDocument(webPageLoader.parseUrl(url)));
        } catch (Exception e) {
            log.error("Error parsing document at url {}", url);
            throw new TreasuryYieldLoadException(date);
        }
    }

    private Long convertDate(LocalDate date) {
        return date.atStartOfDay().toInstant(ZoneOffset.UTC).getEpochSecond();
    }

    private YahooFinanceTenYearTreasuryYield parseDocument(Document document) {
        Element mainContent = document.body().selectFirst("div#Main");
        Element historicalPricesTable = mainContent.selectFirst("table[data-test='historical-prices']");
        Element firstTableRow = historicalPricesTable.selectFirst("tbody").selectFirst("tr");
        return parseTableRow(firstTableRow);
    }

    private YahooFinanceTenYearTreasuryYield parseTableRow(Element tableRow) {
        Elements dataPoints = tableRow.select("td");
        return YahooFinanceTenYearTreasuryYield.builder()
                .date(parseYahooFinanceDate(dataPoints.get(0)))
                .open(Double.parseDouble(dataPoints.get(1).selectFirst("span").text()))
                .high(Double.parseDouble(dataPoints.get(2).selectFirst("span").text()))
                .low(Double.parseDouble(dataPoints.get(3).selectFirst("span").text()))
                .close(Double.parseDouble(dataPoints.get(4).selectFirst("span").text()))
                .adjClose(Double.parseDouble(dataPoints.get(5).selectFirst("span").text()))
                .build();
    }

    private LocalDate parseYahooFinanceDate(Element dateElement) {
        return LocalDate.parse( dateElement.select("span").text(), DateTimeFormatter.ofPattern("MMM dd, yyyy"));
    }
}
