package com.dpgrandslam.stockdataservice.unit.util;

import com.dpgrandslam.stockdataservice.domain.util.TimerUtil;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class TimerUtilTest {

    @Test
    public void testStartStop() throws InterruptedException {
        TimerUtil timerUtil = new TimerUtil();
        timerUtil.start();
        Thread.sleep(100);
        long time = timerUtil.getTime();
        assertEquals(100L, time, 100);
        Thread.sleep(100);
        time = timerUtil.stop();
        assertEquals(200L, time, 100);
    }
}
