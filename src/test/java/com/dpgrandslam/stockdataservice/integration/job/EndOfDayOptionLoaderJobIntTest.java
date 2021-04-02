package com.dpgrandslam.stockdataservice.integration.job;

import com.dpgrandslam.stockdataservice.StockDataServiceApplication;
import com.dpgrandslam.stockdataservice.adapter.apiclient.WebpageLoader;
import com.dpgrandslam.stockdataservice.adapter.repository.TrackedStocksRepository;
import com.dpgrandslam.stockdataservice.domain.error.OptionsChainLoadException;
import com.dpgrandslam.stockdataservice.domain.jobs.EndOfDayOptionsLoaderJob;
import com.dpgrandslam.stockdataservice.domain.model.options.OptionsChain;
import com.dpgrandslam.stockdataservice.domain.model.stock.TrackedStock;
import com.dpgrandslam.stockdataservice.domain.service.HistoricOptionsDataService;
import com.dpgrandslam.stockdataservice.domain.service.OptionsChainLoadService;
import com.dpgrandslam.stockdataservice.domain.service.TrackedStockService;
import com.dpgrandslam.stockdataservice.domain.util.TimeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.util.Pair;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.LocalDate;
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

    @Autowired
    private EndOfDayOptionsLoaderJob subject;

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

        ReflectionTestUtils.setField(subject, "jobStatus", EndOfDayOptionsLoaderJob.JobStatus.RUNNING_SCHEDULED);
        ReflectionTestUtils.setField(subject, "trackedStocks", queue);

        when(trackedStocksRepository.findById(anyString())).thenReturn(Optional.of(trackedStock));
        when(webpageLoader.parseUrl(any())).thenReturn(mockSuccessDoc).thenReturn(mockSuccessDoc).thenReturn(mockErrorDoc);

        subject.weekdayLoadJobAfterHours();

        verify(historicOptionsDataService, times(1)).addFullOptionsChain(optionsChainListAC.capture());
        verify(trackedStockService, times(1)).updateOptionUpdatedTimestamp(eq("TEST"));

        List<OptionsChain> optionsChains = optionsChainListAC.getValue();

        assertEquals(1, optionsChains.size());
        assertEquals(16, ((Queue) ReflectionTestUtils.getField(subject, "retryQueue")).size());
        assertEquals(EndOfDayOptionsLoaderJob.JobStatus.COMPLETE_WITH_FAILURES, ReflectionTestUtils.getField(subject, "jobStatus"));
    }

    @Test
    public void testRetryQueue() throws OptionsChainLoadException {
        Queue<Pair<String, LocalDate>> retryQueue = new ConcurrentLinkedQueue<>();
        retryQueue.add(Pair.of("TEST", LocalDate.of(2021, 3, 12)));

        ReflectionTestUtils.setField(subject, "retryQueue", retryQueue);
        ReflectionTestUtils.setField(subject, "jobStatus", EndOfDayOptionsLoaderJob.JobStatus.COMPLETE_WITH_FAILURES);


        doNothing().when(historicOptionsDataService).addOptionsChain(any(OptionsChain.class));
        when(webpageLoader.parseUrl(any())).thenReturn(mockSuccessDoc);

        subject.retryQueueJob();

        verify(historicOptionsDataService, times(1)).addOptionsChain(any(OptionsChain.class));
        verify(optionsChainLoadService, times(1)).loadLiveOptionsChainForExpirationDate(eq("TEST"), eq(LocalDate.of(2021, 3, 12)));
        assertTrue(retryQueue.isEmpty());
    }

    @Test
    public void testRetryQueue_failsLoad_doesNotAddBack() throws OptionsChainLoadException {
        Queue<Pair<String, LocalDate>> retryQueue = new ConcurrentLinkedQueue<>();
        retryQueue.add(Pair.of("TEST", LocalDate.of(2021, 3, 12)));

        ReflectionTestUtils.setField(subject, "retryQueue", retryQueue);
        ReflectionTestUtils.setField(subject, "jobStatus", EndOfDayOptionsLoaderJob.JobStatus.COMPLETE_WITH_FAILURES);

        when(webpageLoader.parseUrl(any())).thenReturn(mockErrorDoc);

        subject.retryQueueJob();

        verify(historicOptionsDataService, never()).addOptionsChain(any(OptionsChain.class));
        verify(optionsChainLoadService, times(1)).loadLiveOptionsChainForExpirationDate(eq("TEST"), eq(LocalDate.of(2021, 3, 12)));
        assertFalse(retryQueue.isEmpty(), "Retry queue should not be empty.");
    }

    @Test
    public void testRetryQueue_emptyQueue_doesNotRun() throws OptionsChainLoadException {
        Queue<Pair<String, LocalDate>> emptyRetryQueue = new ConcurrentLinkedQueue<>();

        ReflectionTestUtils.setField(subject, "retryQueue", emptyRetryQueue);
        ReflectionTestUtils.setField(subject, "jobStatus", EndOfDayOptionsLoaderJob.JobStatus.COMPLETE_WITH_FAILURES);

        subject.retryQueueJob();

        verify(historicOptionsDataService, never()).addOptionsChain(any());
        verify(optionsChainLoadService, never()).loadLiveOptionsChainForExpirationDate(anyString(), any());
    }

    @Test
    public void testRetryQueue_jobStatusComplete_doesNotRun() throws OptionsChainLoadException {
        Queue<Pair<String, LocalDate>> retryQueue = new ConcurrentLinkedQueue<>();
        retryQueue.add(Pair.of("TEST", LocalDate.now()));

        ReflectionTestUtils.setField(subject, "retryQueue", retryQueue);
        ReflectionTestUtils.setField(subject, "jobStatus", EndOfDayOptionsLoaderJob.JobStatus.COMPLETE);

        subject.retryQueueJob();

        verify(historicOptionsDataService, never()).addOptionsChain(any());
        verify(optionsChainLoadService, never()).loadLiveOptionsChainForExpirationDate(anyString(), any());
    }
}
