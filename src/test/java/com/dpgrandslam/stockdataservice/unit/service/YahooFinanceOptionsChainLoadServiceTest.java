package com.dpgrandslam.stockdataservice.unit.service;

import com.dpgrandslam.stockdataservice.adapter.apiclient.WebpageLoader;
import com.dpgrandslam.stockdataservice.domain.config.ApiClientConfigurationProperties;
import com.dpgrandslam.stockdataservice.domain.error.OptionsChainLoadException;
import com.dpgrandslam.stockdataservice.domain.event.OptionChainParseFailedEvent;
import com.dpgrandslam.stockdataservice.domain.model.options.HistoricalOption;
import com.dpgrandslam.stockdataservice.domain.model.options.Option;
import com.dpgrandslam.stockdataservice.domain.model.options.OptionChainKey;
import com.dpgrandslam.stockdataservice.domain.model.options.OptionsChain;
import com.dpgrandslam.stockdataservice.domain.service.HistoricOptionsDataService;
import com.dpgrandslam.stockdataservice.domain.service.YahooFinanceOptionsChainLoadService;
import com.dpgrandslam.stockdataservice.domain.util.TimeUtils;
import com.dpgrandslam.stockdataservice.testUtils.TestDataFactory;
import com.dpgrandslam.stockdataservice.testUtils.TestUtils;
import org.jsoup.Jsoup;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationEventPublisher;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

import static junit.framework.TestCase.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class YahooFinanceOptionsChainLoadServiceTest {

    private static final String TEST_URL = "https://yahoofinancetest.com";

    @Mock
    private ApiClientConfigurationProperties clientConfigurationProperties;

    @Mock
    private WebpageLoader webpageLoader;

    @Mock
    private HistoricOptionsDataService historicOptionsDataService;

    @Mock
    private TimeUtils timeUtils;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private YahooFinanceOptionsChainLoadService subject;

    @Captor
    private ArgumentCaptor<OptionChainParseFailedEvent> optionChainParseFailedEventAC;

    @Before
    public void setup() throws IOException {
        when(clientConfigurationProperties.getUrlAndPort()).thenReturn(TEST_URL + ":8080");
        when(webpageLoader.parseUrl(anyString())).thenReturn(Jsoup.parse(TestUtils
                .loadResourceFile("mocks/yahoofinance/yahoo-finance-spy.html"), "UTF-8"));
        when(timeUtils.getNowAmericaNewYork()).thenCallRealMethod();
        when(timeUtils.isStockMarketHoliday(any(LocalDate.class))).thenReturn(false);
        when(timeUtils.getLastTradeDate()).thenCallRealMethod();
        when(historicOptionsDataService.getExpirationDatesAtStartDate(anyString(), any())).thenReturn(Collections.emptySet());
    }

    @Test
    public void testLoadOptionsChainForClosestExpiration_noPortInUrl() throws OptionsChainLoadException {
        when(clientConfigurationProperties.getUrlAndPort()).thenReturn(TEST_URL);
        OptionsChain optionsChain = subject.loadLiveOptionsChainForClosestExpiration("test");

        assertEquals("TEST", optionsChain.getTicker());
        assertEquals(LocalDate.of(2021, 3, 5), optionsChain.getExpirationDate());
        assertEquals(269, optionsChain.getAllOptions().size());
        assertNotNull(optionsChain.getOption(new OptionChainKey(406.0, Option.OptionType.CALL)));

        verify(webpageLoader, times(1)).parseUrl(eq(TEST_URL + "/quote/TEST/options?p=TEST"));
    }

    @Test
    public void testLoadOptionsChainForClosestExpiration_portInUrl() throws OptionsChainLoadException {
        subject.loadLiveOptionsChainForClosestExpiration("test");
        verify(webpageLoader, times(1)).parseUrl(eq(TEST_URL + ":8080/quote/TEST/options?p=TEST"));
    }

    @Test
    public void testLoadCompleteOptionsChainForExpirationDateWithPriceDataInRange_dateRangeBeforeToday() throws OptionsChainLoadException {
        LocalDate actual = LocalDate.now().minusDays(5);
        double strike = 1.0;
        Set<? extends Option> options = buildHistoricalOptions(actual, "TEST", LocalDate.now(), strike);

        when(historicOptionsDataService.findOptions(anyString(), any(), any(LocalDate.class), any(LocalDate.class))).then((x) -> options.stream().collect(Collectors.toSet()));

        OptionsChain chain = subject.loadCompleteOptionsChainForExpirationDateWithPriceDataInRange("TEST",
                LocalDate.now(),
                LocalDate.now().minusDays(9),
                LocalDate.now().minusDays(2));
        assertEquals(1, chain.getAllOptions().size());
        Option option = chain.getOption(OptionChainKey.builder().strike(strike).optionType(Option.OptionType.CALL).build());
        assertNotNull(option);
        assertEquals("TEST", option.getTicker());

        verify(historicOptionsDataService, times(1)).findOptions(
                eq("TEST"), eq(LocalDate.now()), eq(LocalDate.now().minusDays(9)), eq(LocalDate.now().minusDays(2)));
        verify(webpageLoader, never()).parseUrl(any());
    }

    @Test
    public void testLoadCompleteOptionsChainForExpirationDateWithPriceDataInRange_nullEndDate() throws OptionsChainLoadException {
        LocalDate march15th2021 = LocalDate.of(2021, 3, 5);
        LocalDate actual = LocalDate.now().minusDays(4);
        Double strike = 405.0;
        Set<HistoricalOption> options = buildHistoricalOptions(actual, "AAPL", march15th2021, strike);

        when(historicOptionsDataService.findOptions(anyString(), any(LocalDate.class), any(LocalDate.class), any(LocalDate.class))).then(x -> options.stream().collect(Collectors.toSet()));

        OptionsChain chain = subject.loadCompleteOptionsChainForExpirationDateWithPriceDataInRange("AAPL",
                march15th2021,
                LocalDate.now().minusDays(9),
                null);
        Option option = chain.getOption(OptionChainKey.builder().strike(strike).optionType(Option.OptionType.CALL).build());
        assertNotNull(option);
        assertEquals("AAPL", option.getTicker());
        assertTrue(option.getOptionPriceData().stream().anyMatch(data -> data.getTradeDate().equals(actual)));

        verify(historicOptionsDataService, times(1)).findOptions(
                eq("AAPL"), eq(march15th2021), eq(LocalDate.now().minusDays(9)), eq(LocalDate.now()));
        verify(webpageLoader, times(1)).parseUrl(any());
    }

    @Test
    public void testLoadOptionsChainForExpirationDateWithAllHistoricData() throws OptionsChainLoadException {
        LocalDate march15th2021 = LocalDate.of(2021, 3, 5);
        LocalDate actual = LocalDate.now().minusDays(4);
        Double strike = 405.0;
        Set<HistoricalOption> options = buildHistoricalOptions(actual, "AAPL", march15th2021, strike);

        when(historicOptionsDataService.findOptions(anyString(), any())).then(x -> options.stream().collect(Collectors.toSet()));

        OptionsChain chain = subject.loadOptionsChainForExpirationDateWithAllData("AAPL", march15th2021);
        Option option = chain.getOption(OptionChainKey.builder().strike(strike).optionType(Option.OptionType.CALL).build());
        assertNotNull(option);
        assertEquals("AAPL", option.getTicker());
        assertEquals(5, option.getOptionPriceData().size());
        assertTrue(option.getOptionPriceData().stream().anyMatch(data -> data.getTradeDate().equals(actual)));

        verify(historicOptionsDataService, times(1)).findOptions(
                eq("AAPL"), eq(march15th2021));
        verify(webpageLoader, times(1)).parseUrl(any());
    }

    @Test
    public void testGetOptionExpirationDates() throws OptionsChainLoadException {
        List<LocalDate> actualExpirationDates = subject.getOptionExpirationDates("AAPL");

        assertNotNull(actualExpirationDates);
        assertFalse(actualExpirationDates.isEmpty());
        assertEquals(37, actualExpirationDates.size());
        assertEquals(LocalDate.of(2021, 3, 5), actualExpirationDates.get(0));
        assertEquals(LocalDate.of(2023, 12,15), actualExpirationDates.get(actualExpirationDates.size() - 1));
    }

    @Test
    public void testLoadLiveOptionsChain_weekdayMorning_beforeMarketOpen() throws OptionsChainLoadException {
        LocalDate nextTuesday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.TUESDAY));
        when(timeUtils.getNowAmericaNewYork()).thenReturn(LocalDateTime.of(nextTuesday, LocalTime.of(8, 45)));

        OptionsChain optionsChain = subject.loadLiveOptionsChainForClosestExpiration("AAPL");

        assertEquals(nextTuesday.minusDays(1), optionsChain.getAllOptions().stream().findFirst().get().getMostRecentPriceData().getTradeDate());
        verify(timeUtils, atLeastOnce()).getNowAmericaNewYork();
    }

    @Test
    public void testLoadLiveOptionsChain_sunday() throws OptionsChainLoadException {
        LocalDate nextSunday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
        when(timeUtils.getNowAmericaNewYork()).thenReturn(LocalDateTime.of(nextSunday, LocalTime.of(8, 45)));

        OptionsChain optionsChain = subject.loadLiveOptionsChainForClosestExpiration("AAPL");

        assertEquals(nextSunday.with(TemporalAdjusters.previous(DayOfWeek.FRIDAY)), optionsChain.getAllOptions().stream().findFirst().get().getMostRecentPriceData().getTradeDate());
        verify(timeUtils, atLeastOnce()).getNowAmericaNewYork();
    }

    @Test
    public void testLoadLiveOptionsChain_saturday() throws OptionsChainLoadException {
        LocalDate nextSunday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SATURDAY));
        when(timeUtils.getNowAmericaNewYork()).thenReturn(LocalDateTime.of(nextSunday, LocalTime.of(8, 45)));

        OptionsChain optionsChain = subject.loadLiveOptionsChainForClosestExpiration("AAPL");

        assertEquals(nextSunday.with(TemporalAdjusters.previous(DayOfWeek.FRIDAY)), optionsChain.getAllOptions().stream().findFirst().get().getMostRecentPriceData().getTradeDate());
        verify(timeUtils, atLeastOnce()).getNowAmericaNewYork();
    }

    @Test
    public void testLoadLiveOptionsChain_mondayMorning_beforeMarketOpen() throws OptionsChainLoadException {
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        when(timeUtils.getNowAmericaNewYork()).thenReturn(LocalDateTime.of(monday, LocalTime.of(8, 45)));

        OptionsChain optionsChain = subject.loadLiveOptionsChainForClosestExpiration("AAPL");

        assertEquals(monday.with(TemporalAdjusters.previous(DayOfWeek.FRIDAY)), optionsChain.getAllOptions().stream().findFirst().get().getMostRecentPriceData().getTradeDate());
        verify(timeUtils, atLeastOnce()).getNowAmericaNewYork();
    }

    @Test
    public void testLoadLiveOptionsChain_mondayMorning_afterMarketOpen() throws OptionsChainLoadException {
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        when(timeUtils.getNowAmericaNewYork()).thenReturn(LocalDateTime.of(monday, LocalTime.of(9, 30)));

        OptionsChain optionsChain = subject.loadLiveOptionsChainForClosestExpiration("AAPL");

        assertEquals(monday, optionsChain.getAllOptions().stream().findFirst().get().getMostRecentPriceData().getTradeDate());
        verify(timeUtils, atLeastOnce()).getNowAmericaNewYork();
    }

    @Test
    public void testLoadLiveOptionsChain_mondayHoliday() throws OptionsChainLoadException {
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        when(timeUtils.getNowAmericaNewYork()).thenReturn(LocalDateTime.of(monday, LocalTime.of(9, 30)));
        when(timeUtils.isStockMarketHoliday(any())).thenReturn(true);

        OptionsChain optionsChain = subject.loadLiveOptionsChainForClosestExpiration("AAPL");

        assertEquals(monday.with(TemporalAdjusters.previous(DayOfWeek.FRIDAY)), optionsChain.getAllOptions().stream().findFirst().get().getMostRecentPriceData().getTradeDate());
        verify(timeUtils, atLeastOnce()).getNowAmericaNewYork();
    }

    @Test
    public void testLoadFullOptionsChainWithAllData() throws OptionsChainLoadException {
        Set<HistoricalOption> options = new HashSet<>();
        options.add(TestDataFactory.HistoricalOptionMother
                .noPriceData().ticker("SPY").expiration(LocalDate.of(2021, 3, 5)).strike(407.5).optionType(Option.OptionType.PUT)
                .historicalPriceData(Collections.singleton(TestDataFactory.OptionPriceDataMother.complete()
                        .tradeDate(LocalDate.now().minusDays(100)).build())).build());
        options.add(TestDataFactory.HistoricalOptionMother.completeWithOnePriceData().ticker("SPY").expiration(LocalDate.of(2025, 3, 5)).build());
        doReturn(options.stream().collect(Collectors.toSet())).when(historicOptionsDataService).findOptions(anyString(), any(LocalDate.class), any(LocalDate.class));

        List<OptionsChain> optionsChains = subject.loadFullOptionsChainWithAllData("SPY");

        Option testSpyOption = optionsChains.stream()
                .filter(optionsChain -> optionsChain.getTicker().equals("SPY")
                        && optionsChain.getExpirationDate().equals(LocalDate.of(2021, 3, 5)))
                .findFirst().get().getOption(407.5, Option.OptionType.PUT);
        Option testTestOption = optionsChains.stream()
                .filter(optionsChain -> optionsChain.getTicker().equals("SPY") && optionsChain.getExpirationDate().equals(LocalDate.of(2025, 3, 5)))
                .findFirst().get().getAllOptions().stream().findFirst().get();

        assertEquals(2, testSpyOption.getOptionPriceData().size());
        assertEquals(1, testTestOption.getOptionPriceData().size());
    }

    @Test
    public void loadFullOptionsChainWithAllDataBetweenDates_endDateNotToday() throws OptionsChainLoadException {
        LocalDate endDate = LocalDate.now().minusDays(1);

        subject.loadFullOptionsChainWithAllDataBetweenDates("SPCE", null, endDate);

        verify(webpageLoader, never()).parseUrl(any());
    }

    @Test
    public void loadFullOptionsChain_expirationDatesMissing_publishesEvent() throws OptionsChainLoadException {
        Set<LocalDate> localDates = new HashSet<>(subject.getOptionExpirationDates("TEST"));
        localDates.add(LocalDate.now().plusYears(20));
        localDates.add(LocalDate.now().minusDays(1));
        localDates.add(LocalDate.now());

        when(historicOptionsDataService.getExpirationDatesAtStartDate(anyString(), any())).thenReturn(localDates);

        subject.loadFullLiveOptionsChain("TEST");

        verify(applicationEventPublisher, times(2)).publishEvent(optionChainParseFailedEventAC.capture());

        OptionChainParseFailedEvent optionChainParseFailedEvent = optionChainParseFailedEventAC.getAllValues().get(0);

        assertEquals("TEST", optionChainParseFailedEvent.getTicker());
        assertEquals(LocalDate.now().plusYears(20), optionChainParseFailedEvent.getExpiration());
    }

    private Set<HistoricalOption> buildHistoricalOptions(LocalDate actual, String ticker, LocalDate expiration, Double strike) {
        Set<HistoricalOption> options = new HashSet<>();
        options.add(TestDataFactory.HistoricalOptionMother.noPriceData().ticker(ticker).strike(strike).expiration(expiration).historicalPriceData(new HashSet<>(Arrays.asList(
                TestDataFactory.OptionPriceDataMother.complete().tradeDate(LocalDate.now().minus(10, ChronoUnit.DAYS)).build(),
                TestDataFactory.OptionPriceDataMother.complete().tradeDate(actual).build(),
                TestDataFactory.OptionPriceDataMother.complete().tradeDate(LocalDate.now().minusDays(1)).build(),
                TestDataFactory.OptionPriceDataMother.complete().dataObtainedDate(Timestamp.from(Instant.now())).build()
        ))).build());
        return options;
    }
}
