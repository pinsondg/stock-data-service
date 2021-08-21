package com.dpgrandslam.stockdataservice.domain.util;

public class TimerUtil {

    private long startTime;
    private long stopTime;
    private TimerStatus timerStatus;

    public TimerUtil() {
        reset();
    }

    public void reset() {
        stopTime = 0;
        startTime = 0;
        timerStatus = TimerStatus.STOPPED;
    }

    public void start() {
        if (timerStatus != TimerStatus.RUNNING) {
            startTime = System.currentTimeMillis();
            timerStatus = TimerStatus.RUNNING;
        }
    }

    public long stop() {
        if (timerStatus != TimerStatus.STOPPED) {
            stopTime = System.currentTimeMillis();
            return stopTime - startTime;
        } else {
            throw new IllegalStateException("Timer is already stopped.");
        }
    }

    public long getTime() {
        if (timerStatus == TimerStatus.STOPPED) {
            return stopTime - startTime;
        } else if (timerStatus == TimerStatus.RUNNING) {
            return System.currentTimeMillis() - startTime;
        }
        return 0L;
    }

    private enum TimerStatus {
        RUNNING, STOPPED
    }

    public static TimerUtil startTimer() {
        TimerUtil timerUtil = new TimerUtil();
        timerUtil.start();
        return timerUtil;
    }
}
