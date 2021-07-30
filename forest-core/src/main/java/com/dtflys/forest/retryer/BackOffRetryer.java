package com.dtflys.forest.retryer;

import com.dtflys.forest.exceptions.ForestRetryException;
import com.dtflys.forest.http.ForestRequest;

/**
 * 基于退避算法的重试器
 */
public class BackOffRetryer implements Retryer {

    protected final ForestRequest request;

    protected final int maxRetryCount;

    protected final long maxRetryInterval;

    protected long waitedTime;


    public BackOffRetryer(ForestRequest request) {
        this.request = request;
        this.maxRetryCount = request.getRetryCount();
        this.maxRetryInterval = request.getMaxRetryInterval();
        this.waitedTime = 0;
    }

    @Override
    public void canRetry(ForestRetryException ex) throws Throwable {
        int currentCount = ex.getCurrentRetryCount();
        if (currentCount >= maxRetryCount) {
            if (currentCount == 0) {
                throw ex.getCause();
            }
            throw ex;
        }
        long interval = nextInterval(currentCount);
        if (interval > maxRetryInterval) {
            interval = maxRetryInterval;
        }
        if (interval > 0) {
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw ex;
            }
        }
        this.waitedTime += interval;
    }

    protected long nextInterval(int currentCount) {
        long interval = (long) Math.pow(2.0, currentCount) * 1000;
        if (maxRetryInterval >= 0 && interval > maxRetryInterval) {
            return maxRetryInterval;
        }
        return interval;
    }

    public ForestRequest getRequest() {
        return request;
    }

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public long getMaxRetryInterval() {
        return maxRetryInterval;
    }

    public long getWaitedTime() {
        return waitedTime;
    }
}
