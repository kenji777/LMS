package itf.com.app.lms.logic.state;

import java.util.concurrent.atomic.AtomicInteger;

public class TestProgress {
    private String serialNo = "";
    private String modelId = "";
    private String currentTestItem = "";
    private final AtomicInteger totalTests = new AtomicInteger(0);
    private final AtomicInteger completedTests = new AtomicInteger(0);
    private final AtomicInteger okCount = new AtomicInteger(0);
    private final AtomicInteger ngCount = new AtomicInteger(0);
    private long startTimeMs = 0;
    private long endTimeMs = 0;

    public void reset() {
        serialNo = "";
        modelId = "";
        currentTestItem = "";
        totalTests.set(0);
        completedTests.set(0);
        okCount.set(0);
        ngCount.set(0);
        startTimeMs = 0;
        endTimeMs = 0;
    }

    public int getProgressPercent() {
        int total = totalTests.get();
        if (total == 0) return 0;
        return (int) ((completedTests.get() * 100.0f) / total);
    }

    public boolean isComplete() {
        return completedTests.get() >= totalTests.get() && totalTests.get() > 0;
    }

    public long getElapsedTimeMs() {
        long end = endTimeMs == 0 ? System.currentTimeMillis() : endTimeMs;
        return startTimeMs == 0 ? 0 : Math.max(0, end - startTimeMs);
    }

    public void incrementCompletedTests() {
        completedTests.incrementAndGet();
    }

    public void incrementOkCount() {
        okCount.incrementAndGet();
    }

    public void incrementNgCount() {
        ngCount.incrementAndGet();
    }

    // Getters / setters
    public String getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(String serialNo) {
        this.serialNo = serialNo;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getCurrentTestItem() {
        return currentTestItem;
    }

    public void setCurrentTestItem(String currentTestItem) {
        this.currentTestItem = currentTestItem;
    }

    public void setTotalTests(int total) {
        totalTests.set(total);
    }

    public int getTotalTests() {
        return totalTests.get();
    }

    public int getCompletedTests() {
        return completedTests.get();
    }

    public int getOkCount() {
        return okCount.get();
    }

    public int getNgCount() {
        return ngCount.get();
    }

    public void setStartTimeMs(long startTimeMs) {
        this.startTimeMs = startTimeMs;
    }

    public void setEndTimeMs(long endTimeMs) {
        this.endTimeMs = endTimeMs;
    }
}










