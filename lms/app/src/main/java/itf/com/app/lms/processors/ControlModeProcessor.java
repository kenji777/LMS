package itf.com.app.lms.processors;

import itf.com.app.lms.util.Constants;
import itf.com.app.lms.util.LogManager;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ControlModeProcessor - Handles control mode state and test execution
 *
 * Responsibilities:
 * - Control mode state management
 * - Control mode test item execution
 * - Control mode response handling
 * - Control mode timeout management
 *
 * Usage:
 * <pre>
 * ControlModeProcessor processor = new ControlModeProcessor();
 * processor.setControlMode(true);
 * processor.executeControlModeTestItem(command);
 * </pre>
 */
public class ControlModeProcessor {

    private static final String TAG = "ControlModeProcessor";
    private static final int CONTROL_ST0101_REQUIRED_COUNT = 3;
    private static final long CONTROL_RESPONSE_TIMEOUT_MS = 10000; // 10초

    // Control mode state
    private boolean isControlMode = false;
    private boolean isControlOn = false;
    private boolean controlOwnerIsAndroidApp = false;
    private int controlSt0101SuccessCount = 0;
    private boolean controlModeReady = false;

    // Control mode response waiting
    private final AtomicBoolean waitingForControlResponse = new AtomicBoolean(false);
    private String pendingControlCommand = null;
    private Timer controlResponseTimeoutTimer = null;
    private TimerTask controlResponseTimeoutTask = null;
    private final Object controlResponseLock = new Object();

    // Control mode test
    private Timer controlTestTimer = null;
    private TimerTask controlTestTimerTask = null;
    private final AtomicBoolean controlTestTimerRunning = new AtomicBoolean(false);
    private final Object controlTestTimerLock = new Object();
    private int controlTestItemIdx = -1;
    private int controlTestItemCounter = 0;
    private String controlCurrentTestItem = null;
    private String controlTestReceiveCommand = null;
    private String controlTestReceiveResponse = null;
    private String controlTestResultValue = null;
    private String controlTestResult = null;

    /**
     * Listener interface for control mode events
     */
    public interface ControlModeListener {
        /**
         * Called when control mode state changes
         */
        void onControlModeStateChanged(boolean isControlMode, boolean isControlOn);

        /**
         * Called when control mode test item is executed
         */
        void onControlModeTestItemExecuted(String command);

        /**
         * Called when control mode response is received
         */
        void onControlModeResponseReceived(String command, String response);

        /**
         * Called when control mode response timeout occurs
         */
        void onControlModeResponseTimeout(String command);

        /**
         * Called when control mode test result is available
         */
        void onControlModeTestResult(String command, String response, String resultValue, String result);
    }

    private ControlModeListener listener;

    /**
     * Constructor
     */
    public ControlModeProcessor() {
    }

    /**
     * Set control mode listener
     */
    public void setControlModeListener(ControlModeListener listener) {
        this.listener = listener;
    }

    /**
     * Toggle control mode
     */
    public void toggleControlMode() {
        if (!isControlMode) {
            // Enter control mode
            isControlMode = true;
            isControlOn = false;
            controlOwnerIsAndroidApp = false;
            controlSt0101SuccessCount = 0;
            controlModeReady = false;
            LogManager.i(LogManager.LogCategory.PS, TAG, "Control mode entered");
        } else {
            // Exit control mode
            isControlMode = false;
            isControlOn = false;
            controlOwnerIsAndroidApp = false;
            controlSt0101SuccessCount = 0;
            controlModeReady = false;
            stopControlTestTimer();
            stopControlResponseTimeout();
            clearControlTestInfo();
            LogManager.i(LogManager.LogCategory.PS, TAG, "Control mode exited");
        }

        if (listener != null) {
            listener.onControlModeStateChanged(isControlMode, isControlOn);
        }
    }

    /**
     * Set control ON/OFF
     */
    public void setControlOn(boolean on) {
        this.isControlOn = on;
        if (listener != null) {
            listener.onControlModeStateChanged(isControlMode, isControlOn);
        }
    }

    /**
     * Execute control mode test item
     */
    public void executeControlModeTestItem(String command) {
        if (!isControlMode || !isControlOn) {
            LogManager.w(LogManager.LogCategory.PS, TAG, "Control mode not active, cannot execute test item");
            return;
        }

        if (command == null || command.isEmpty()) {
            LogManager.w(LogManager.LogCategory.PS, TAG, "Invalid command for control mode test");
            return;
        }

        controlCurrentTestItem = command;
        controlTestItemCounter++;
        controlTestReceiveCommand = null;
        controlTestReceiveResponse = null;
        controlTestResultValue = null;
        controlTestResult = null;

        LogManager.i(LogManager.LogCategory.PS, TAG, 
                "Executing control mode test item: " + command);

        if (listener != null) {
            listener.onControlModeTestItemExecuted(command);
        }
    }

    /**
     * Handle control mode response
     */
    public void handleControlModeResponse(String response) {
        if (response == null || response.isEmpty()) {
            return;
        }

        synchronized (controlResponseLock) {
            if (waitingForControlResponse.get() && pendingControlCommand != null) {
                waitingForControlResponse.set(false);
                stopControlResponseTimeout();
                String command = pendingControlCommand;
                pendingControlCommand = null;

                if (listener != null) {
                    listener.onControlModeResponseReceived(command, response);
                }
            }
        }
    }

    /**
     * Start waiting for control response
     */
    public void startWaitingForControlResponse(String command) {
        if (command == null || command.isEmpty()) {
            return;
        }

        synchronized (controlResponseLock) {
            if (waitingForControlResponse.get()) {
                LogManager.w(LogManager.LogCategory.PS, TAG, 
                        "Already waiting for control response");
                return;
            }

            waitingForControlResponse.set(true);
            pendingControlCommand = command;

            // Start timeout timer
            stopControlResponseTimeout();
            controlResponseTimeoutTimer = new Timer("ControlResponseTimeout");
            controlResponseTimeoutTask = new TimerTask() {
                @Override
                public void run() {
                    synchronized (controlResponseLock) {
                        if (waitingForControlResponse.get()) {
                            waitingForControlResponse.set(false);
                            String timeoutCommand = pendingControlCommand;
                            pendingControlCommand = null;

                            if (listener != null) {
                                listener.onControlModeResponseTimeout(timeoutCommand);
                            }
                        }
                    }
                }
            };
            controlResponseTimeoutTimer.schedule(controlResponseTimeoutTask, CONTROL_RESPONSE_TIMEOUT_MS);
        }
    }

    /**
     * Stop control response timeout
     */
    private void stopControlResponseTimeout() {
        if (controlResponseTimeoutTimer != null) {
            try {
                controlResponseTimeoutTimer.cancel();
                controlResponseTimeoutTimer.purge();
                controlResponseTimeoutTimer = null;
            } catch (Exception e) {
                LogManager.e(LogManager.LogCategory.ER, TAG, 
                        "Error canceling control response timeout timer", e);
            }
        }
        if (controlResponseTimeoutTask != null) {
            try {
                controlResponseTimeoutTask.cancel();
                controlResponseTimeoutTask = null;
            } catch (Exception e) {
                LogManager.e(LogManager.LogCategory.ER, TAG, 
                        "Error canceling control response timeout task", e);
            }
        }
    }

    /**
     * Start control test timer
     */
    public void startControlTestTimer(TimerTask task, long delayMs, long intervalMs) {
        synchronized (controlTestTimerLock) {
            if (controlTestTimerRunning.compareAndSet(false, true)) {
                stopControlTestTimer();
                try {
                    controlTestTimer = new Timer("ControlTestTimer");
                    controlTestTimerTask = task;
                    controlTestTimer.schedule(controlTestTimerTask, delayMs, intervalMs);
                    LogManager.i(LogManager.LogCategory.PS, TAG, "Control test timer started");
                } catch (Exception e) {
                    controlTestTimerRunning.set(false);
                    LogManager.e(LogManager.LogCategory.ER, TAG, 
                            "Error starting control test timer", e);
                }
            }
        }
    }

    /**
     * Stop control test timer
     */
    public void stopControlTestTimer() {
        synchronized (controlTestTimerLock) {
            if (controlTestTimerRunning.compareAndSet(true, false)) {
                if (controlTestTimer != null) {
                    try {
                        controlTestTimer.cancel();
                        controlTestTimer.purge();
                        controlTestTimer = null;
                    } catch (Exception e) {
                        LogManager.e(LogManager.LogCategory.ER, TAG, 
                                "Error canceling control test timer", e);
                    }
                }
                if (controlTestTimerTask != null) {
                    try {
                        controlTestTimerTask.cancel();
                        controlTestTimerTask = null;
                    } catch (Exception e) {
                        LogManager.e(LogManager.LogCategory.ER, TAG, 
                                "Error canceling control test timer task", e);
                    }
                }
            }
        }
    }

    /**
     * Clear control test info
     */
    public void clearControlTestInfo() {
        controlTestItemIdx = -1;
        controlTestItemCounter = 0;
        controlCurrentTestItem = null;
        controlTestReceiveCommand = null;
        controlTestReceiveResponse = null;
        controlTestResultValue = null;
        controlTestResult = null;
    }

    /**
     * Update control test result
     */
    public void updateControlTestResult(String command, String response, 
                                       String resultValue, String result) {
        controlTestReceiveCommand = command;
        controlTestReceiveResponse = response;
        controlTestResultValue = resultValue;
        controlTestResult = result;

        if (listener != null) {
            listener.onControlModeTestResult(command, response, resultValue, result);
        }
    }

    // Getters and setters
    public boolean isControlMode() {
        return isControlMode;
    }

    public boolean isControlOn() {
        return isControlOn;
    }

    public boolean isControlOwnerIsAndroidApp() {
        return controlOwnerIsAndroidApp;
    }

    public void setControlOwnerIsAndroidApp(boolean ownerIsAndroidApp) {
        this.controlOwnerIsAndroidApp = ownerIsAndroidApp;
    }

    public int getControlSt0101SuccessCount() {
        return controlSt0101SuccessCount;
    }

    public void incrementControlSt0101SuccessCount() {
        controlSt0101SuccessCount++;
        if (controlSt0101SuccessCount >= CONTROL_ST0101_REQUIRED_COUNT) {
            controlModeReady = true;
        }
    }

    public boolean isControlModeReady() {
        return controlModeReady;
    }

    public String getControlCurrentTestItem() {
        return controlCurrentTestItem;
    }

    public String getControlTestResult() {
        return controlTestResult;
    }

    public String getControlTestResultValue() {
        return controlTestResultValue;
    }

    public boolean isControlTestTimerRunning() {
        return controlTestTimerRunning.get();
    }
}


