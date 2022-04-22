package com.dpgrandslam.stockdataservice.domain.service;

import com.dpgrandslam.stockdataservice.adapter.apiclient.WebpageLoader;
import com.dpgrandslam.stockdataservice.domain.config.ApiClientConfigurationProperties;
import com.dpgrandslam.stockdataservice.domain.error.YahooFinanceQuoteLoadException;
import com.dpgrandslam.stockdataservice.domain.model.stock.YahooFinanceQuote;
import com.google.common.base.Charsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class YahooFinanceHistoricStockDataLoadService {

    private final WebpageLoader basicWebPageLoader;

    @Autowired
    @Qualifier("YahooFinanceApiClientConfigurationProperties")
    private ApiClientConfigurationProperties clientConfigurationProperties;


    public List<YahooFinanceQuote> loadQuoteForDates(String ticker, LocalDate startDate, LocalDate endDate) {
        StringBuilder sb = new StringBuilder(clientConfigurationProperties.getUrlAndPort());
        sb.append("/quote/");
        sb.append(URLEncoder.encode(ticker, Charsets.UTF_8));
        sb.append("/history?period1=");
        sb.append(convertDate(startDate));
        sb.append("&period2=");
        Long period2 = convertDate(endDate == null || endDate.equals(startDate) ? startDate.plusDays(1) : endDate);
        sb.append(period2);
        sb.append( "&interval=1d&filter=history&frequency=1d&includeAdjustedClose=true");
        String url = sb.toString();
        List<YahooFinanceQuote> quotes;
        try {
            quotes = parseDocument(basicWebPageLoader.parseUrl(url));
        }  catch (Exception e) {
            log.error("Error parsing document at url {}", url, e);
            throw new YahooFinanceQuoteLoadException(ticker, startDate, endDate, e);
        }
        if (quotes.isEmpty()) {
            // If no quotes there was probably an error
            throw new YahooFinanceQuoteLoadException(ticker, startDate, endDate);
        }
        quotes.forEach(quote -> quote.setTicker(ticker));
        return quotes;
    }

    private Long convertDate(LocalDate date) {
        return date.atStartOfDay().toInstant(ZoneOffset.UTC).getEpochSecond();
    }

    private List<YahooFinanceQuote> parseDocument(Document document) {
        Element mainContent = document.body().selectFirst("div#Main");
        Element historicalPricesTable = mainContent.selectFirst("table[data-test='historical-prices']");
        List<Element> tableRows = historicalPricesTable.selectFirst("tbody").select("tr");
        return tableRows.stream().map(this::parseTableRow).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private YahooFinanceQuote parseTableRow(Element tableRow) {
        Elements dataPoints = tableRow.select("td");
        LocalDate d = parseYahooFinanceDate(dataPoints.get(0));
        YahooFinanceQuote.YahooFinanceQuoteBuilder builder = YahooFinanceQuote.builder()
                .date(d);

        try {
            builder.open(Double.parseDouble(dataPoints.get(1).selectFirst("span").text()))
                    .high(Double.parseDouble(dataPoints.get(2).selectFirst("span").text()))
                    .low(Double.parseDouble(dataPoints.get(3).selectFirst("span").text()))
                    .close(Double.parseDouble(dataPoints.get(4).selectFirst("span").text()))
                    .adjClose(Double.parseDouble(dataPoints.get(5).selectFirst("span").text()));
        } catch (NullPointerException e) {
            log.warn("Could not parse row for date {} in chart.", d);
        }
        return builder.build();
    }

    private LocalDate parseYahooFinanceDate(Element dateElement) {
        return LocalDate.parse( dateElement.select("span").text(), DateTimeFormatter.ofPattern("MMM dd, yyyy"));
    }
}
