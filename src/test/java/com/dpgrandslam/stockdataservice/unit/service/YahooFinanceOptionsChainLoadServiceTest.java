package com.dpgrandslam.stockdataservice.unit.service;

import com.dpgrandslam.stockdataservice.adapter.apiclient.WebpageLoader;
import com.dpgrandslam.stockdataservice.domain.config.ApiClientConfigurationProperties;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

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

    @InjectMocks
    private YahooFinanceOptionsChainLoadService subject;

    @Before
    public void setup() throws IOException {
        when(clientConfigurationProperties.getUrl()).thenReturn(TEST_URL);
        when(webpageLoader.parseUrl(anyString())).thenReturn(Jsoup.parse(TestUtils
                .loadResourceFile("mocks/yahoofinance/yahoo-finance-spy.html"), "UTF-8"));
        when(timeUtils.getNowAmericaNewYork()).thenCallRealMethod();
        when(timeUtils.isStockMarketHoliday(any(LocalDate.class))).thenReturn(false);
    }

    @Test
    public void testLoadOptionsChainForClosestExpiration_noPortInUrl() {
        when(clientConfigurationProperties.getPort()).thenReturn(null);
        OptionsChain optionsChain = subject.loadLiveOptionsChainForClosestExpiration("test");

        assertEquals("TEST", optionsChain.getTicker());
        assertEquals(LocalDate.of(2021, 3, 5), optionsChain.getExpirationDate());
        assertEquals(269, optionsChain.getAllOptions().size());
        assertNotNull(optionsChain.getOption(new OptionChainKey(406.0, Option.OptionType.CALL)));

        verify(webpageLoader, times(1)).parseUrl(eq(TEST_URL + "/quote/TEST/options?p=TEST"));
    }

    @Test
    public void testLoadOptionsChainForClosestExpiration_portInUrl() {
        when(clientConfigurationProperties.getPort()).thenReturn(8080);

        subject.loadLiveOptionsChainForClosestExpiration("test");
        verify(webpageLoader, times(1)).parseUrl(eq(TEST_URL + ":8080/quote/TEST/options?p=TEST"));
    }

    @Test
    public void testLoadCompleteOptionsChainForExpirationDateWithPriceDataInRange_dateRangeBeforeToday() {
        Timestamp actual = Timestamp.from(Instant.now().minus(5, ChronoUnit.DAYS));
        double strike = 1.0;
        List<HistoricalOption> options = buildHistoricalOptions(actual, "TEST", LocalDate.now(), strike);

        when(historicOptionsDataService.findOptions(anyString(), any())).thenReturn(options.stream());

        OptionsChain chain = subject.loadCompleteOptionsChainForExpirationDateWithPriceDataInRange("TEST",
                LocalDate.now(),
                LocalDate.now().minusDays(9),
                LocalDate.now().minusDays(1));
        assertEquals(1, chain.getAllOptions().size());
        Option option = chain.getOption(OptionChainKey.builder().strike(strike).optionType(Option.OptionType.CALL).build());
        assertNotNull(option);
        assertEquals("TEST", option.getTicker());
        assertEquals(1, option.getOptionPriceData().size());
        assertEquals(actual, option.getOptionPriceData().stream().findFirst().get().getDataObtainedDate());

        verify(historicOptionsDataService, times(1)).findOptions(
                eq("TEST"), eq(LocalDate.now()));
        verify(webpageLoader, never()).parseUrl(any());
    }

    @Test
    public void testLoadCompleteOptionsChainForExpirationDateWithPriceDataInRange_nullEndDate() {
        LocalDate march15th2021 = LocalDate.of(2021, 3, 5);
        Timestamp actual = Timestamp.from(Instant.now().minus(4, ChronoUnit.DAYS));
        Double strike = 405.0;
        List<HistoricalOption> options = buildHistoricalOptions(actual, "AAPL", march15th2021, strike);

        when(historicOptionsDataService.findOptions(anyString(), any(LocalDate.class))).thenReturn(options.stream());

        OptionsChain chain = subject.loadCompleteOptionsChainForExpirationDateWithPriceDataInRange("AAPL",
                march15th2021,
                LocalDate.now().minusDays(9),
                null);
        Option option = chain.getOption(OptionChainKey.builder().strike(strike).optionType(Option.OptionType.CALL).build());
        assertNotNull(option);
        assertEquals("AAPL", option.getTicker());
        assertEquals(4, option.getOptionPriceData().size());
        assertTrue(option.getOptionPriceData().stream().anyMatch(data -> data.getDataObtainedDate().equals(actual)));

        verify(historicOptionsDataService, times(1)).findOptions(
                eq("AAPL"), eq(march15th2021));
        verify(webpageLoader, times(1)).parseUrl(any());
    }

    @Test
    public void testLoadOptionsChainForExpirationDateWithAllHistoricData() {
        LocalDate march15th2021 = LocalDate.of(2021, 3, 5);
        Timestamp actual = Timestamp.from(Instant.now().minus(4, ChronoUnit.DAYS));
        Double strike = 405.0;
        List<HistoricalOption> options = buildHistoricalOptions(actual, "AAPL", march15th2021, strike);

        when(historicOptionsDataService.findOptions(anyString(), any())).thenReturn(options.stream());

        OptionsChain chain = subject.loadOptionsChainForExpirationDateWithAllData("AAPL", march15th2021);
        Option option = chain.getOption(OptionChainKey.builder().strike(strike).optionType(Option.OptionType.CALL).build());
        assertNotNull(option);
        assertEquals("AAPL", option.getTicker());
        assertEquals(5, option.getOptionPriceData().size());
        assertTrue(option.getOptionPriceData().stream().anyMatch(data -> data.getDataObtainedDate().equals(actual)));

        verify(historicOptionsDataService, times(1)).findOptions(
                eq("AAPL"), eq(march15th2021));
        verify(webpageLoader, times(1)).parseUrl(any());
    }

    @Test
    public void testGetOptionExpirationDates() {
        List<LocalDate> actualExpirationDates = subject.getOptionExpirationDates("AAPL");

        assertNotNull(actualExpirationDates);
        assertFalse(actualExpirationDates.isEmpty());
        assertEquals(37, actualExpirationDates.size());
        assertEquals(LocalDate.of(2021, 3, 5), actualExpirationDates.get(0));
        assertEquals(LocalDate.of(2023, 12,15), actualExpirationDates.get(actualExpirationDates.size() - 1));
    }

    @Test
    public void testLoadLiveOptionsChain_weekdayMorning_beforeMarketOpen() {
        LocalDate nextTuesday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.TUESDAY));
        when(timeUtils.getNowAmericaNewYork()).thenReturn(LocalDateTime.of(nextTuesday, LocalTime.of(8, 45)));

        OptionsChain optionsChain = subject.loadLiveOptionsChainForClosestExpiration("AAPL");

        assertEquals(nextTuesday.minusDays(1), optionsChain.getAllOptions().stream().findFirst().get().getMostRecentPriceData().getTradeDate());
        verify(timeUtils, atLeastOnce()).getNowAmericaNewYork();
    }

    @Test
    public void testLoadLiveOptionsChain_sunday() {
        LocalDate nextSunday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
        when(timeUtils.getNowAmericaNewYork()).thenReturn(LocalDateTime.of(nextSunday, LocalTime.of(8, 45)));

        OptionsChain optionsChain = subject.loadLiveOptionsChainForClosestExpiration("AAPL");

        assertEquals(nextSunday.with(TemporalAdjusters.previous(DayOfWeek.FRIDAY)), optionsChain.getAllOptions().stream().findFirst().get().getMostRecentPriceData().getTradeDate());
        verify(timeUtils, atLeastOnce()).getNowAmericaNewYork();
    }

    @Test
    public void testLoadLiveOptionsChain_saturday() {
        LocalDate nextSunday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SATURDAY));
        when(timeUtils.getNowAmericaNewYork()).thenReturn(LocalDateTime.of(nextSunday, LocalTime.of(8, 45)));

        OptionsChain optionsChain = subject.loadLiveOptionsChainForClosestExpiration("AAPL");

        assertEquals(nextSunday.with(TemporalAdjusters.previous(DayOfWeek.FRIDAY)), optionsChain.getAllOptions().stream().findFirst().get().getMostRecentPriceData().getTradeDate());
        verify(timeUtils, atLeastOnce()).getNowAmericaNewYork();
    }

    @Test
    public void testLoadLiveOptionsChain_mondayMorning_beforeMarketOpen() {
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        when(timeUtils.getNowAmericaNewYork()).thenReturn(LocalDateTime.of(monday, LocalTime.of(8, 45)));

        OptionsChain optionsChain = subject.loadLiveOptionsChainForClosestExpiration("AAPL");

        assertEquals(monday.with(TemporalAdjusters.previous(DayOfWeek.FRIDAY)), optionsChain.getAllOptions().stream().findFirst().get().getMostRecentPriceData().getTradeDate());
        verify(timeUtils, atLeastOnce()).getNowAmericaNewYork();
    }

    @Test
    public void testLoadLiveOptionsChain_mondayMorning_afterMarketOpen() {
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        when(timeUtils.getNowAmericaNewYork()).thenReturn(LocalDateTime.of(monday, LocalTime.of(9, 30)));

        OptionsChain optionsChain = subject.loadLiveOptionsChainForClosestExpiration("AAPL");

        assertEquals(monday, optionsChain.getAllOptions().stream().findFirst().get().getMostRecentPriceData().getTradeDate());
        verify(timeUtils, atLeastOnce()).getNowAmericaNewYork();
    }

    @Test
    public void testLoadLiveOptionsChain_mondayHoliday() {
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        when(timeUtils.getNowAmericaNewYork()).thenReturn(LocalDateTime.of(monday, LocalTime.of(9, 30)));
        when(timeUtils.isStockMarketHoliday(any())).thenReturn(true);

        OptionsChain optionsChain = subject.loadLiveOptionsChainForClosestExpiration("AAPL");

        assertEquals(monday.with(TemporalAdjusters.previous(DayOfWeek.FRIDAY)), optionsChain.getAllOptions().stream().findFirst().get().getMostRecentPriceData().getTradeDate());
        verify(timeUtils, atLeastOnce()).getNowAmericaNewYork();
    }

    private List<HistoricalOption> buildHistoricalOptions(Timestamp actual, String ticker, LocalDate expiration, Double strike) {
        List<HistoricalOption> options = new ArrayList<>();
        options.add(TestDataFactory.HistoricalOptionMother.noPriceData().ticker(ticker).strike(strike).expiration(expiration).historicalPriceData(new HashSet<>(Arrays.asList(
                TestDataFactory.OptionPriceDataMother.complete().dataObtainedDate(Timestamp.from(Instant.now().minus(10, ChronoUnit.DAYS))).build(),
                TestDataFactory.OptionPriceDataMother.complete().dataObtainedDate(actual).build(),
                TestDataFactory.OptionPriceDataMother.complete().dataObtainedDate(Timestamp.from(Instant.now().minus(10, ChronoUnit.MINUTES))).build(),
                TestDataFactory.OptionPriceDataMother.complete().dataObtainedDate(Timestamp.from(Instant.now())).build()
        ))).build());
        return options;
    }
}
