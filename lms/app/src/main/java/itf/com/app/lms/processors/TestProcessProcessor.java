package itf.com.app.lms.processors;

import itf.com.app.lms.item.ItemAdapterTestItem;
import itf.com.app.lms.util.Constants;
import itf.com.app.lms.util.LogManager;
import itf.com.app.lms.vo.VoTestItem;

import java.util.ArrayList;
import java.util.List;

/**
 * TestProcessProcessor - Handles test process execution and result processing
 *
 * Responsibilities:
 * - Test item list management
 * - Test counter management
 * - Test result calculation
 * - Test progress tracking
 *
 * Usage:
 * <pre>
 * TestProcessProcessor processor = new TestProcessProcessor();
 * processor.rebuildTestItemList(testItems);
 * processor.recalcTestCounts(adapter);
 * </pre>
 */
public class TestProcessProcessor {

    private static final String TAG = "TestProcessProcessor";

    // Test state
    private int testOkCnt = 0;
    private int testNgCnt = 0;
    private int testItemIdx = 0;
    private int testItemCounter = 0;
    private int testTotalCounter = 0;
    private String currentTestItem = Constants.InitialValues.CURRENT_TEST_ITEM;

    /**
     * Listener interface for test process events
     */
    public interface TestProcessListener {
        /**
         * Called when test counts are updated
         */
        void onTestCountsUpdated(int okCount, int ngCount);

        /**
         * Called when test item changes
         */
        void onTestItemChanged(int itemIdx, String itemCommand);

        /**
         * Called when test progress updates
         */
        void onTestProgressUpdated(int totalCounter, int itemCounter);
    }

    private TestProcessListener listener;

    /**
     * Constructor
     */
    public TestProcessProcessor() {
    }

    /**
     * Set test process listener
     */
    public void setTestProcessListener(TestProcessListener listener) {
        this.listener = listener;
    }

    /**
     * Rebuild test item list
     */
    public void rebuildTestItemList(String[][] testItems) {
        if (testItems == null || testItems.length == 0) {
            LogManager.w(LogManager.LogCategory.PS, TAG, "Test items array is null or empty");
            return;
        }

        // Reset counters
        testItemIdx = 0;
        testItemCounter = 0;
        testTotalCounter = 0;
        currentTestItem = testItems.length > 0 && testItems[0].length > 1 
                ? testItems[0][1] 
                : Constants.InitialValues.CURRENT_TEST_ITEM;

        LogManager.i(LogManager.LogCategory.PS, TAG, 
                "Test item list rebuilt: " + testItems.length + " items");

        if (listener != null) {
            listener.onTestItemChanged(testItemIdx, currentTestItem);
        }
    }

    /**
     * Recalculate test counts from adapter
     */
    public void recalcTestCountsFromAdapter(ItemAdapterTestItem adapter) {
        if (adapter == null) {
            return;
        }

        int calculatedOk = 0;
        int calculatedNg = 0;
        for (int i = 0; i < adapter.getCount(); i++) {
            VoTestItem item = (VoTestItem) adapter.getItem(i);
            String result = item.getTest_item_result();
            switch (result) {
                case Constants.ResultStatus.OK:
                    calculatedOk++;
                    break;
                case Constants.ResultStatus.NG:
                    calculatedNg++;
                    break;
            }
        }

        testOkCnt = calculatedOk;
        testNgCnt = calculatedNg;

        if (listener != null) {
            listener.onTestCountsUpdated(calculatedOk, calculatedNg);
        }
    }

    /**
     * Increment test item counter
     */
    public void incrementTestItemCounter() {
        testItemCounter++;
        testTotalCounter++;
        if (listener != null) {
            listener.onTestProgressUpdated(testTotalCounter, testItemCounter);
        }
    }

    /**
     * Move to next test item
     */
    public boolean moveToNextTestItem(String[][] testItems, int requiredCount) {
        if (testItems == null || testItemIdx >= testItems.length) {
            return false;
        }

        if (testItemCounter >= requiredCount) {
            testItemCounter = 0;
            testItemIdx++;
            if (testItemIdx < testItems.length && testItems[testItemIdx].length > 1) {
                currentTestItem = testItems[testItemIdx][1];
                if (listener != null) {
                    listener.onTestItemChanged(testItemIdx, currentTestItem);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Get current test item
     */
    public String getCurrentTestItem() {
        return currentTestItem;
    }

    /**
     * Set current test item
     */
    public void setCurrentTestItem(String testItem) {
        this.currentTestItem = testItem;
        if (listener != null) {
            listener.onTestItemChanged(testItemIdx, currentTestItem);
        }
    }

    /**
     * Get test OK count
     */
    public int getTestOkCnt() {
        return testOkCnt;
    }

    /**
     * Get test NG count
     */
    public int getTestNgCnt() {
        return testNgCnt;
    }

    /**
     * Get test item index
     */
    public int getTestItemIdx() {
        return testItemIdx;
    }

    /**
     * Set test item index
     */
    public void setTestItemIdx(int idx) {
        this.testItemIdx = idx;
    }

    /**
     * Get test item counter
     */
    public int getTestItemCounter() {
        return testItemCounter;
    }

    /**
     * Set test item counter
     */
    public void setTestItemCounter(int counter) {
        this.testItemCounter = counter;
    }

    /**
     * Get test total counter
     */
    public int getTestTotalCounter() {
        return testTotalCounter;
    }

    /**
     * Set test total counter
     */
    public void setTestTotalCounter(int counter) {
        this.testTotalCounter = counter;
    }

    /**
     * Reset all counters
     */
    public void resetCounters() {
        testOkCnt = 0;
        testNgCnt = 0;
        testItemIdx = 0;
        testItemCounter = 0;
        testTotalCounter = 0;
        currentTestItem = Constants.InitialValues.CURRENT_TEST_ITEM;
    }

    /**
     * Check if test is complete
     */
    public boolean isTestComplete(String[][] testItems) {
        return testItems != null && testItemIdx >= testItems.length;
    }
}


