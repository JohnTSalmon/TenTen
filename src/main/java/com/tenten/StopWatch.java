package com.tenten;

import static java.util.Objects.isNull;

public class StopWatch {
    private long startTime = 0L;
    private long endTime = 0L;

    public void start() {
        startTime = System.currentTimeMillis();
    }

    public void stop() {
        endTime = System.currentTimeMillis();
    }

    public String printElapsed() {
        if (isNull(startTime) || isNull(endTime)) {
            throw new IllegalStateException("StopWatch : Both start() and stop() must be called and in that order");
        }
        return (endTime - startTime) + " milliseconds.";
    }
}
