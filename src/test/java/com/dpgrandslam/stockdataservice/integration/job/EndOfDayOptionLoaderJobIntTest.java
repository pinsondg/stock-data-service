package com.dpgrandslam.stockdataservice.integration.job;

import com.dpgrandslam.stockdataservice.StockDataServiceApplication;
import com.dpgrandslam.stockdataservice.adapter.apiclient.WebpageLoader;
import com.dpgrandslam.stockdataservice.adapter.repository.TrackedStocksRepository;
import com.dpgrandslam.stockdataservice.domain.jobs.EndOfDayOptionsLoaderJob;
import com.dpgrandslam.stockdataservice.domain.model.options.OptionsChain;
import com.dpgrandslam.stockdataservice.domain.model.stock.TrackedStock;
import com.dpgrandslam.stockdataservice.domain.service.HistoricOptionsDataService;
import com.dpgrandslam.stockdataservice.domain.service.TrackedStockService;
import com.dpgrandslam.stockdataservice.domain.util.TimeUtils;
import com.dpgrandslam.stockdataservice.testUtils.TestUtils;
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
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Autowired
    private EndOfDayOptionsLoaderJob subject;

    @MockBean
    private TimeUtils timeUtils;

    @MockBean
    private TrackedStocksRepository trackedStocksRepository;

    @Captor
    private ArgumentCaptor<List<OptionsChain>> optionsChainListAC;

    @Before
    public void init() {
        when(timeUtils.isStockMarketHoliday(any())).thenReturn(false);
    }

    @Test
    public void test_endOfDatOptionsLoaderJob_onFailedLoad_addsToRetryQueue() throws IOException, InterruptedException {
        TrackedStock trackedStock = new TrackedStock();
        trackedStock.setTicker("TEST");
        trackedStock.setActive(true);
        Queue<TrackedStock> queue = new ConcurrentLinkedQueue<>();
        queue.add(trackedStock);

        Document mockErrorDoc = Jsoup.parse(TestUtils.loadHtmlFileAndClean("mocks/yahoofinance/yahoo-finance-aapl_error.html"));
        Document mockSuccessDoc = Jsoup.parse(TestUtils.loadHtmlFileAndClean("mocks/yahoofinance/yahoo-finance-aapl_empty-chain.html"));

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
}
