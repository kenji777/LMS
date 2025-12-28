package itf.com.lms.logic.test;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import itf.com.lms.conn.bluetooth.BluetoothManager;
import itf.com.lms.logic.data.DataRepository;
import itf.com.lms.logic.state.StateManager;

/**
 * Coordinates test execution between Bluetooth and USB managers.
 * This is a lightweight skeleton implementation to enable Phase 2 integration.
 */
public class TestExecutor {

    private static final String TAG = "TestExecutor";

    private final Context appContext;
    private final TestExecutorListener listener;
    private final BluetoothManager bluetoothManager;
    private final DataRepository dataRepository;
    private final StateManager stateManager;
    private final ScheduledExecutorService scheduler;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    public TestExecutor(Context context,
                        TestExecutorListener listener,
                        BluetoothManager bluetoothManager,
                        DataRepository dataRepository,
                        StateManager stateManager) {
        this.appContext = context.getApplicationContext();
        this.listener = listener;
        this.bluetoothManager = bluetoothManager;
        this.dataRepository = dataRepository;
        this.stateManager = stateManager;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "TestExecutor");
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        });
    }

    public void initialize() {
        Log.i(TAG, "TestExecutor initialized");
    }

    public void executeTest(TestSequence sequence) {
        if (sequence == null || sequence.getTotalSteps() == 0) {
            notifyFailure(TestError.INVALID_SEQUENCE, "Sequence is empty");
            return;
        }

        if (!isRunning.compareAndSet(false, true)) {
            notifyFailure(TestError.ALREADY_RUNNING, "Test already running");
            return;
        }

        mainHandler.post(() -> listener.onTestStarted(sequence.getTestItemCode()));
        runStep(sequence, 0);
    }

    private void runStep(TestSequence sequence, int index) {
        if (index >= sequence.getTotalSteps()) {
            complete(sequence.getTestItemCode());
            return;
        }

        TestStep step = sequence.getStep(index);
        scheduler.execute(() -> {
            try {
                switch (step.getType()) {
                    case BLUETOOTH_COMMAND:
                        executeBtCommand(step);
                        break;
                    case USB_DATA_COLLECTION:
                        collectUsbData(step);
                        break;
                    case WAIT:
                        waitStep(step);
                        break;
                    case EVALUATE:
                        evaluateResults(step);
                        break;
                }
                final int nextIndex = index + 1;
                mainHandler.post(() -> listener.onTestStepCompleted(nextIndex, sequence.getTotalSteps()));
                runStep(sequence, nextIndex);
            } catch (Exception e) {
                notifyFailure(TestError.EXECUTION_ERROR, e.getMessage());
            }
        });
    }

    private void executeBtCommand(TestStep step) {
        if (bluetoothManager != null && bluetoothManager.isConnected()) {
            bluetoothManager.sendMessage(step.getCommand());
        }
    }

    private void collectUsbData(TestStep step) {
        // Placeholder: real implementation should coordinate with UsbManager callbacks
        scheduler.schedule(() -> {
        }, Math.max(step.getTimeoutMs(), 0), TimeUnit.MILLISECONDS);
    }

    private void waitStep(TestStep step) throws InterruptedException {
        if (step.getTimeoutMs() > 0) {
            Thread.sleep(step.getTimeoutMs());
        }
    }

    private void evaluateResults(TestStep step) {
        // Placeholder: integrate with TestEvaluator in future iterations
    }

    public void cancelTest() {
        if (isRunning.compareAndSet(true, false)) {
            scheduler.shutdownNow();
            notifyFailure(TestError.CANCELLED, "Cancelled");
        }
    }

    private void complete(String testItemCode) {
        isRunning.set(false);
        mainHandler.post(() -> listener.onTestCompleted(new TestResult(true, 0, "Completed")));
    }

    private void notifyFailure(TestError error, String message) {
        isRunning.set(false);
        mainHandler.post(() -> listener.onTestFailed(error, message));
    }

    public void cleanup() {
        scheduler.shutdownNow();
        isRunning.set(false);
        Log.i(TAG, "TestExecutor cleaned up");
    }

    public interface TestExecutorListener {
        void onTestStarted(String testItemCode);

        void onTestStepCompleted(int stepIndex, int totalSteps);

        void onTestCompleted(TestResult result);

        void onTestFailed(TestError error, String message);

        void onTestTimeout(String testItemCode);

        void onProgressUpdate(int percent, String status);
    }

    public enum TestError {
        INVALID_SEQUENCE,
        ALREADY_RUNNING,
        EXECUTION_ERROR,
        CANCELLED,
        TIMEOUT
    }
}








