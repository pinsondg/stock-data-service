package com.dpgrandslam.stockdataservice.integration.job;

import com.dpgrandslam.stockdataservice.StockDataServiceApplication;
import com.dpgrandslam.stockdataservice.adapter.apiclient.WebpageLoader;
import com.dpgrandslam.stockdataservice.adapter.repository.OptionPriceDataLoadRetryRepository;
import com.dpgrandslam.stockdataservice.adapter.repository.TrackedStocksRepository;
import com.dpgrandslam.stockdataservice.domain.error.OptionsChainLoadException;
import com.dpgrandslam.stockdataservice.domain.event.TrackedStockAddedEvent;
import com.dpgrandslam.stockdataservice.domain.jobs.EndOfDayOptionsLoaderJob;
import com.dpgrandslam.stockdataservice.domain.model.Holiday;
import com.dpgrandslam.stockdataservice.domain.model.OptionPriceDataLoadRetry;
import com.dpgrandslam.stockdataservice.domain.model.options.OptionsChain;
import com.dpgrandslam.stockdataservice.domain.model.stock.TrackedStock;
import com.dpgrandslam.stockdataservice.domain.service.HistoricOptionsDataService;
import com.dpgrandslam.stockdataservice.domain.service.OptionPriceDataLoadRetryService;
import com.dpgrandslam.stockdataservice.domain.service.OptionsChainLoadService;
import com.dpgrandslam.stockdataservice.domain.service.TrackedStockService;
import com.dpgrandslam.stockdataservice.domain.util.TimeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.dpgrandslam.stockdataservice.testUtils.TestUtils.loadHtmlFileAndClean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = StockDataServiceApplication.class)
public class EndOfDayOptionLoaderJobIntTest {

    @MockBean
    private WebpageLoader webpageLoader;

    @SpyBean
    private HistoricOptionsDataService historicOptionsDataService;

    @SpyBean
    private TrackedStockService trackedStockService;

    @SpyBean
    private OptionsChainLoadService optionsChainLoadService;

    @SpyBean
    private OptionPriceDataLoadRetryService retryService;

    @Autowired
    private EndOfDayOptionsLoaderJob subject;

    @Autowired
    private OptionPriceDataLoadRetryRepository retryRepository;

    @MockBean
    private TimeUtils timeUtils;

    @MockBean
    private TrackedStocksRepository trackedStocksRepository;

    @Captor
    private ArgumentCaptor<List<OptionsChain>> optionsChainListAC;

    private Document mockSuccessDoc;
    private Document mockErrorDoc;

    @Before
    public void init() throws IOException {
        when(timeUtils.isStockMarketHoliday(any())).thenReturn(false);
        when(timeUtils.getCurrentOrLastTradeDate()).thenReturn(LocalDate.now());
        mockErrorDoc = Jsoup.parse(loadHtmlFileAndClean("mocks/yahoofinance/yahoo-finance-aapl_error.html"));
        mockSuccessDoc = Jsoup.parse(loadHtmlFileAndClean("mocks/yahoofinance/yahoo-finance-aapl_empty-chain.html"));
    }

    @Test
    public void test_endOfDatOptionsLoaderJob_onFailedLoad_addsToRetryQueue() throws IOException, InterruptedException {
        TrackedStock trackedStock = new TrackedStock();
        trackedStock.setTicker("TEST");
        trackedStock.setActive(true);
        Queue<String> queue = new ConcurrentLinkedQueue<>();
        queue.add(trackedStock.getTicker());

        Document mockErrorDoc = Jsoup.parse(loadHtmlFileAndClean("mocks/yahoofinance/yahoo-finance-aapl_error.html"));
        Document mockSuccessDoc = Jsoup.parse(loadHtmlFileAndClean("mocks/yahoofinance/yahoo-finance-aapl_empty-chain.html"));

        ReflectionTestUtils.setField(subject, "mainJobStatus", EndOfDayOptionsLoaderJob.JobStatus.RUNNING_SCHEDULED);
        ReflectionTestUtils.setField(subject, "trackedStocks", queue);

        when(trackedStocksRepository.findById(anyString())).thenReturn(Optional.of(trackedStock));
        when(webpageLoader.parseUrl(any())).thenReturn(mockSuccessDoc).thenReturn(mockSuccessDoc).thenReturn(mockErrorDoc);

        subject.weekdayLoadJobAfterHours();

        verify(historicOptionsDataService, times(1)).addFullOptionsChain(optionsChainListAC.capture());
        verify(trackedStockService, times(1)).updateOptionUpdatedTimestamp(eq("TEST"));

        List<OptionsChain> optionsChains = optionsChainListAC.getValue();

        assertEquals(1, optionsChains.size());
        assertEquals(16, retryService.getAllWithTradeDate(timeUtils.getCurrentOrLastTradeDate()).size());
        assertEquals(EndOfDayOptionsLoaderJob.JobStatus.COMPLETE_WITH_FAILURES, ReflectionTestUtils.getField(subject, "mainJobStatus"));
    }

    @Test
    public void testRetryQueue() throws OptionsChainLoadException {
        Long retryId = retryService.addOrUpdateRetry("TEST", LocalDate.of(2021, 3, 12), timeUtils.getCurrentOrLastTradeDate()).getRetryId();

        ReflectionTestUtils.setField(subject, "mainJobStatus", EndOfDayOptionsLoaderJob.JobStatus.COMPLETE_WITH_FAILURES);

        doNothing().when(historicOptionsDataService).addOptionsChain(any(OptionsChain.class));
        when(webpageLoader.parseUrl(any())).thenReturn(mockSuccessDoc);

        subject.runRetryBeforeMidnight();

        verify(historicOptionsDataService, times(1)).addOptionsChain(any(OptionsChain.class));
        verify(optionsChainLoadService, times(1)).loadLiveOptionsChainForExpirationDate(eq("TEST"), eq(LocalDate.of(2021, 3, 12)));
        verify(retryService, times(1)).removeRetry(eq(retryId));
        verify(retryService, atLeastOnce()).getAllWithTradeDate(eq(timeUtils.getCurrentOrLastTradeDate()));
    }

    @Test
    public void testRetryQueue_failsLoad_doesNotAddBack() throws OptionsChainLoadException {
        OptionPriceDataLoadRetry retry = retryService.addOrUpdateRetry("TEST", LocalDate.of(2021, 3, 12), timeUtils.getCurrentOrLastTradeDate());

        ReflectionTestUtils.setField(subject, "mainJobStatus", EndOfDayOptionsLoaderJob.JobStatus.COMPLETE_WITH_FAILURES);

        when(webpageLoader.parseUrl(any())).thenReturn(mockErrorDoc);

        subject.runRetryBeforeMidnight();

        verify(historicOptionsDataService, never()).addOptionsChain(any(OptionsChain.class));
        verify(optionsChainLoadService, times(1)).loadLiveOptionsChainForExpirationDate(eq("TEST"), eq(LocalDate.of(2021, 3, 12)));
        verify(retryService, atLeastOnce()).addOrUpdateRetry(eq("TEST"), any(LocalDate.class), any(LocalDate.class));
        verify(retryService, never()).removeRetry(anyLong());
    }

    @Test
    public void testRetryQueue_emptyQueue_doesNotRun() throws OptionsChainLoadException {
        when(retryService.getAllWithTradeDate(any())).thenReturn(Collections.emptySet());
        ReflectionTestUtils.setField(subject, "mainJobStatus", EndOfDayOptionsLoaderJob.JobStatus.COMPLETE_WITH_FAILURES);

        subject.runRetryBeforeMidnight();

        verify(historicOptionsDataService, never()).addOptionsChain(any());
        verify(optionsChainLoadService, never()).loadLiveOptionsChainForExpirationDate(anyString(), any());
    }

    @Test
    public void test_jobHoliday_doesNotRun() throws OptionsChainLoadException {
        Holiday holiday = new Holiday("Test Holiday", LocalDate.now());
        Set<Holiday> mockHolidays = Collections.singleton(holiday);
        when(timeUtils.isTodayAmericaNewYorkStockMarketHoliday()).thenReturn(true);
        when(timeUtils.getStockMarketHolidays()).thenReturn(mockHolidays);
        when(timeUtils.getNowAmericaNewYork()).thenCallRealMethod();

        ReflectionTestUtils.setField(subject, "mainJobStatus", EndOfDayOptionsLoaderJob.JobStatus.RUNNING_SCHEDULED);

        subject.weekdayLoadJobBeforeHours();

        verify(timeUtils, times(2)).isTodayAmericaNewYorkStockMarketHoliday();
        verify(timeUtils, times(1)).getStockMarketHolidays();
        verify(historicOptionsDataService, never()).addFullOptionsChain(any());
        verify(optionsChainLoadService, never()).loadFullLiveOptionsChain(any());
        verify(trackedStockService, never()).updateOptionUpdatedTimestamp(any());
        verify(trackedStockService, never()).findByTicker(any());
    }

    @Test
    public void testTrackStockAddedEvent() {
        ReflectionTestUtils.setField(subject, "mainJobStatus", EndOfDayOptionsLoaderJob.JobStatus.COMPLETE);
        TrackedStock trackedStock = new TrackedStock();
        trackedStock.setActive(true);
        trackedStock.setTicker("TEST");
        trackedStock.setName("Test");
        TrackedStockAddedEvent trackedStockAddedEvent = new TrackedStockAddedEvent(this, Collections.singletonList(trackedStock));
        subject.onApplicationEvent(trackedStockAddedEvent);

        Queue<String> trackedStocks = (Queue<String>) ReflectionTestUtils.getField(subject, "trackedStocks");
        assertNotNull(trackedStocks);
        assertEquals(1, trackedStocks.size());
        assertEquals("TEST", trackedStocks.peek());
        assertEquals(EndOfDayOptionsLoaderJob.JobStatus.RUNNING_MANUAL, ReflectionTestUtils.getField(subject, "mainJobStatus"));
    }

    @Test
    public void test_endOfDatOptionsLoaderJob_onCompleteFailedLoad_addsBackToQueue_updatesFailedCount() throws OptionsChainLoadException {
        TrackedStock trackedStock = TrackedStock.builder()
                .active(true)
                .ticker("TEST")
                .name("Test")
                .lastOptionsHistoricDataUpdate(LocalDate.now().minusDays(1))
                .build();
        Queue<String> queue = new ConcurrentLinkedQueue<>();
        queue.add(trackedStock.getTicker());

        ReflectionTestUtils.setField(subject, "trackedStocks", queue);
        ReflectionTestUtils.setField(subject, "mainJobStatus", EndOfDayOptionsLoaderJob.JobStatus.RUNNING_SCHEDULED);

        when(webpageLoader.parseUrl(any())).thenReturn(mockErrorDoc);
        when(trackedStocksRepository.findById(eq(trackedStock.getTicker()))).thenReturn(Optional.of(trackedStock));

        Map<String, Integer> failMap = (Map<String, Integer>) ReflectionTestUtils.getField(subject, "failCountMap");
        Integer steps = (Integer) ReflectionTestUtils.getField(subject, "STEPS");
        Integer maxRetry = (Integer) ReflectionTestUtils.getField(subject, "MAX_RETRY");

        assertNotNull(steps);
        assertNotNull(maxRetry);

        for (int i = 1; i < 10; i++) {
            int timesTried = (i * steps) + 1;
            subject.weekdayLoadJobAfterHours();
            assertEquals(timesTried <= maxRetry ? 1 : 0, queue.size());
            assertEquals(timesTried <= maxRetry ? timesTried : null, failMap.get(trackedStock.getTicker()));
        }

        assertEquals(0, queue.size());
        assertEquals(0, failMap.size());

        verify(optionsChainLoadService, times(maxRetry)).loadFullLiveOptionsChain(eq(trackedStock.getTicker()));
    }

    @After
    public void cleanup() {
        retryRepository.deleteAll();
    }
}
