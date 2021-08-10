package com.dpgrandslam.stockdataservice.integration.job;

import com.dpgrandslam.stockdataservice.StockDataServiceApplication;
import com.dpgrandslam.stockdataservice.adapter.apiclient.WebpageLoader;
import com.dpgrandslam.stockdataservice.adapter.repository.OptionPriceDataLoadRetryRepository;
import com.dpgrandslam.stockdataservice.adapter.repository.TrackedStocksRepository;
import com.dpgrandslam.stockdataservice.domain.error.OptionsChainLoadException;
import com.dpgrandslam.stockdataservice.domain.jobs.EndOfDayOptionsLoaderJob;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
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
        when(timeUtils.getLastTradeDate()).thenReturn(LocalDate.now());
        mockErrorDoc = Jsoup.parse(loadHtmlFileAndClean("mocks/yahoofinance/yahoo-finance-aapl_error.html"));
        mockSuccessDoc = Jsoup.parse(loadHtmlFileAndClean("mocks/yahoofinance/yahoo-finance-aapl_empty-chain.html"));
    }

    @Test
    public void test_endOfDatOptionsLoaderJob_onFailedLoad_addsToRetryQueue() throws IOException, InterruptedException {
        TrackedStock trackedStock = new TrackedStock();
        trackedStock.setTicker("TEST");
        trackedStock.setActive(true);
        Queue<TrackedStock> queue = new ConcurrentLinkedQueue<>();
        queue.add(trackedStock);

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
        assertEquals(16, retryService.getAllWithTradeDate(timeUtils.getLastTradeDate()).size());
        assertEquals(EndOfDayOptionsLoaderJob.JobStatus.COMPLETE_WITH_FAILURES, ReflectionTestUtils.getField(subject, "mainJobStatus"));
    }

    @Test
    public void testRetryQueue() throws OptionsChainLoadException {
        Long retryId = retryService.addOrUpdateRetry("TEST", LocalDate.of(2021, 3, 12), timeUtils.getLastTradeDate()).getRetryId();

        ReflectionTestUtils.setField(subject, "mainJobStatus", EndOfDayOptionsLoaderJob.JobStatus.COMPLETE_WITH_FAILURES);

        doNothing().when(historicOptionsDataService).addOptionsChain(any(OptionsChain.class));
        when(webpageLoader.parseUrl(any())).thenReturn(mockSuccessDoc);

        subject.runRetryBeforeMidnight();

        verify(historicOptionsDataService, times(1)).addOptionsChain(any(OptionsChain.class));
        verify(optionsChainLoadService, times(1)).loadLiveOptionsChainForExpirationDate(eq("TEST"), eq(LocalDate.of(2021, 3, 12)));
        verify(retryService, times(1)).removeRetry(eq(retryId));
        verify(retryService, atLeastOnce()).getAllWithTradeDate(eq(timeUtils.getLastTradeDate()));
    }

    @Test
    public void testRetryQueue_failsLoad_doesNotAddBack() throws OptionsChainLoadException {
        OptionPriceDataLoadRetry retry = retryService.addOrUpdateRetry("TEST", LocalDate.of(2021, 3, 12), timeUtils.getLastTradeDate());

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

    @After
    public void cleanup() {
        retryRepository.deleteAll();
    }
}
