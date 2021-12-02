package com.dpgrandslam.stockdataservice.domain.service;

import com.dpgrandslam.stockdataservice.adapter.apiclient.WebpageLoader;
import com.dpgrandslam.stockdataservice.domain.config.ApiClientConfigurationProperties;
import com.dpgrandslam.stockdataservice.domain.error.AllOptionsExpirationDatesNotPresentException;
import com.dpgrandslam.stockdataservice.domain.error.OptionsChainLoadException;
import com.dpgrandslam.stockdataservice.domain.event.OptionChainParseFailedEvent;
import com.dpgrandslam.stockdataservice.domain.model.options.LiveOption;
import com.dpgrandslam.stockdataservice.domain.model.options.Option;
import com.dpgrandslam.stockdataservice.domain.model.options.OptionsChain;
import com.dpgrandslam.stockdataservice.domain.util.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;

@Service
@Slf4j
public class YahooFinanceOptionsChainLoadService extends OptionsChainLoadService {

    private final WebpageLoader basicWebPageLoader;

    private final ApplicationEventPublisher eventPublisher;

    private final ApiClientConfigurationProperties clientConfigurationProperties;

    public YahooFinanceOptionsChainLoadService(HistoricOptionsDataService historicOptionsDataService,
                                               TimeUtils timeUtils, WebpageLoader webpageLoader,
                                               ApplicationEventPublisher applicationEventPublisher,
                                               @Qualifier("YahooFinanceApiClientConfigurationProperties")
                                                       ApiClientConfigurationProperties clientConfigurationProperties) {
        super(historicOptionsDataService, timeUtils);
        this.basicWebPageLoader = webpageLoader;
        this.eventPublisher = applicationEventPublisher;
        this.clientConfigurationProperties = clientConfigurationProperties;
    }

    @Override
    public OptionsChain loadLiveOptionsChainForClosestExpiration(String ticker) throws OptionsChainLoadException {
        Document document = doCall(ticker);
        LocalDate expiration = null;
        try {
            expiration = parseDocumentForExpirationDates(document).get(0);
            return buildOptionsChain(ticker, expiration, document);
        } catch (Exception e) {
            eventPublisher.publishEvent(new OptionChainParseFailedEvent(this, ticker, expiration, timeUtils.getLastTradeDate()));
            throw new OptionsChainLoadException(ticker, document.baseUri(), "Options chain load failure most likely due to too many calls.", e);
        }
    }

    @Override
    public List<OptionsChain> loadFullLiveOptionsChain(String ticker) throws OptionsChainLoadException {
        log.info("Started loading of full options chain from yahoo-finance for ticker {}.", ticker);
        long startTime = System.currentTimeMillis();
        List<OptionsChain> optionsChains = Collections.synchronizedList(new ArrayList<>());
        Document document = doCall(ticker);
        try {
            List<LocalDate> expirationDates = parseDocumentForExpirationDates(document);
            log.info("Found {} option expiration dates for ticker {}: {}", expirationDates.size(), ticker, expirationDates);
            try {
                validateExpirationDates(ticker, expirationDates);
            } catch (AllOptionsExpirationDatesNotPresentException e) {
                log.warn("Not all options dates could be loaded from yahoo for ticker {}.", ticker, e);
                e.getMissingDates().forEach(date -> eventPublisher.publishEvent(new OptionChainParseFailedEvent(this, ticker, date, timeUtils.getLastTradeDate())));
            }
            for (LocalDate expiration : expirationDates) {
                try {
                    optionsChains.add(loadLiveOptionsChainForExpirationDate(ticker, expiration));
                } catch (OptionsChainLoadException e) {
                    log.warn("Could not load options chain for ticker {} and date {}. Live data for this option will not be added to the chain.",
                            ticker, expiration);
                }
            }
            log.info("Loading of options from yahoo-finance for ticker {} complete. Took {} seconds to load all options.", ticker, (System.currentTimeMillis() - startTime) / 1000.0);
        } catch (Exception e) {
            throw new OptionsChainLoadException(ticker, document.baseUri(), "Options chain load failure most likely due to too many calls.", e);
        }
        return optionsChains;
    }

    @Override
    public OptionsChain loadLiveOptionsChainForExpirationDate(String ticker, LocalDate expirationDate) throws OptionsChainLoadException {
        Document document = doCall(ticker, expirationDate);
        try {
            if (parseDocumentForExpirationDates(document).stream().noneMatch(x -> x.compareTo(expirationDate) == 0)) {
                throw new IllegalArgumentException("The expiration date provided (" + expirationDate.toString() + ") is not valid for ticker: " + ticker + ".");
            }
        } catch (Exception e) {
            String uri = "";
            if (document != null) {
                uri = document.baseUri();
            }
            eventPublisher.publishEvent(new OptionChainParseFailedEvent(this, ticker, expirationDate, timeUtils.getLastTradeDate()));
            throw new OptionsChainLoadException(ticker, uri, "Options chain load failure most likely due to too many calls.", e);
        }
        return buildOptionsChain(ticker, expirationDate, document);
    }

    @Override
    public List<LocalDate> getOptionExpirationDates(String ticker) throws OptionsChainLoadException {
        Document document = doCall(ticker);
        try {
            return parseDocumentForExpirationDates(document);
        } catch (Exception e) {
            throw new OptionsChainLoadException(ticker, document.baseUri(), "Options chain load failure most likely due to too many calls.", e);
        }
    }

    private Document doCall(String ticker) {
        return doCall(ticker, null);
    }

    private Document doCall(String ticker, LocalDate expirationDate) {
        String fullUrl = clientConfigurationProperties.getUrlAndPort() + "/quote/" + ticker.toUpperCase() + "/options?p=" + ticker.toUpperCase();
        if (expirationDate != null) {
            fullUrl += "&date=" + expirationDate.atStartOfDay().toInstant(ZoneOffset.UTC).getEpochSecond();
        }

        return basicWebPageLoader.parseUrl(fullUrl);
    }

    private OptionsChain buildOptionsChain(String ticker, LocalDate expirationDate, Document document) {
        String upperCaseTicker = ticker.toUpperCase();
        OptionsChain optionsChain = new OptionsChain(upperCaseTicker, expirationDate);
        if (document != null) {
            Element mainContent = document.selectFirst("div#Col1-1-OptionContracts-Proxy");
            Element calls = mainContent.selectFirst("table.calls");
            Element puts = mainContent.selectFirst("table.puts");

            List<Option> options = new LinkedList<>();
            if (calls != null) {
                options.addAll(parseOptions(calls, Option.OptionType.CALL));
            }
            if (puts != null) {
                options.addAll(parseOptions(puts, Option.OptionType.PUT));
            }
            optionsChain.addOptions(options);
        }
        return optionsChain;
    }

    private List<Option> parseOptions(Element section, Option.OptionType optionType) {
        List<Option> options = new ArrayList<>();
        section.select("tbody").select("tr").forEach(optionRow -> {
            Option option = new LiveOption();
            parseNumberFieldOrLogError(optionRow.selectFirst("td.data-col2").selectFirst("a").text(), "strike", null)
                    .ifPresent(val -> option.setStrike(val.doubleValue()));
            parseNumberFieldOrLogError(optionRow.selectFirst("td.data-col3").text(), "marketPrice", option.getStrike())
                    .ifPresent(val -> option.getMostRecentPriceData().setLastTradePrice(val.doubleValue()));
            parseNumberFieldOrLogError(optionRow.selectFirst("td.data-col4").text(), "bid", option.getStrike())
                    .ifPresent(val -> option.getMostRecentPriceData().setBid(val.doubleValue()));
            parseNumberFieldOrLogError(optionRow.selectFirst("td.data-col5").text(), "ask", option.getStrike())
                    .ifPresent(val -> option.getMostRecentPriceData().setAsk(val.doubleValue()));
            parseNumberFieldOrLogError(optionRow.selectFirst("td.data-col8").text(), "volume", option.getStrike())
                    .ifPresent(val -> option.getMostRecentPriceData().setVolume(val.intValue()));
            parseNumberFieldOrLogError(optionRow.selectFirst("td.data-col9").text(), "openInterest", option.getStrike())
                    .ifPresent(val -> option.getMostRecentPriceData().setOpenInterest(val.intValue()));
            option.getMostRecentPriceData().setImpliedVolatility(extractPercent(optionRow.selectFirst("td.data-col10").text()));
            option.getMostRecentPriceData().setDataObtainedDate(Timestamp.from(Instant.now()));
            option.getMostRecentPriceData().setTradeDate(timeUtils.getLastTradeDate());
            option.setOptionType(optionType);
            options.add(option);
        });
        return options;
    }

    private Optional<Number> parseNumberFieldOrLogError(String value, String fieldName, Double optionStrike) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
        Number number = null;
        try {
            if (value.equals("-")) {
                number = 0;
            } else {
                number = numberFormat.parse(value);
            }
        } catch (ParseException e) {
            log.warn("Could not parse option with strike ${} for field {} due to reason: {}.", optionStrike, fieldName, e.getMessage());
        }
        return Optional.ofNullable(number);
    }

    private Double extractPercent(String percentString) {
        int index  = percentString.indexOf('%');
        try {
            return NumberFormat.getInstance(Locale.US).parse(percentString.substring(0, index)).doubleValue();
        } catch (Exception e) {
            log.warn("Exception while parsing implied volatility.",  e);
            return null;
        }
    }

    private List<LocalDate> parseDocumentForExpirationDates(Document document) {
        List<LocalDate> timestamps = new LinkedList<>();
        if (document != null) {
            Elements dateSelectors = document.selectFirst("div#Col1-1-OptionContracts-Proxy")
                    .selectFirst("div.controls").selectFirst("select")
                    .select("option");
            dateSelectors.forEach(element -> {
                Long timestamp = Long.parseLong(element.attr("value"));
                timestamps.add(LocalDate.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.of("Z")));
            });
        }
        return timestamps;
    }

    private void validateExpirationDates(String ticker, List<LocalDate> expirationDates) throws AllOptionsExpirationDatesNotPresentException {
        Set<LocalDate> expDatesCopy = new HashSet<>(expirationDates);
        Set<LocalDate> storedExpirationDates = super.historicOptionsDataService.getExpirationDatesAtStartDate(ticker, timeUtils.getStartDayOfTradeWeek());
        storedExpirationDates.removeAll(expDatesCopy);
        if (!storedExpirationDates.isEmpty()) {
            throw new AllOptionsExpirationDatesNotPresentException(new ArrayList<>(storedExpirationDates));
        }
    }
}
