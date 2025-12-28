package itf.com.app.lms.managers;

import itf.com.app.lms.util.Constants;
import itf.com.app.lms.util.LogManager;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TimerManager - Centralized timer management for ActivityModelTestProcess
 *
 * Responsibilities:
 * - Manage all timers in a centralized way
 * - Prevent timer leaks and memory issues
 * - Provide thread-safe timer operations
 * - Cleanup all timers on demand
 *
 * Usage:
 * <pre>
 * TimerManager timerManager = new TimerManager();
 * timerManager.startBtMessageTimer(task, interval);
 * // ... use timers
 * timerManager.stopAllTimers(); // MUST call in onDestroy()
 * </pre>
 */
public class TimerManager {

    private static final String TAG = "TimerManager";

    // Timer instances
    private Timer btMessageTimer = null;
    private TimerTask btMessageTimerTask = null;
    private final AtomicBoolean btMessageTimerRunning = new AtomicBoolean(false);
    private final Object btMessageTimerLock = new Object();

    private Timer resetTimer = null;
    private TimerTask resetTimerTask = null;

    private Timer finishedRestartTimer = null;
    private TimerTask finishedRestartTimerTask = null;
    private final AtomicBoolean finishedRestartTimerRunning = new AtomicBoolean(false);
    private final Object finishedRestartTimerLock = new Object();

    private Timer unfinishedRestartTimer = null;
    private TimerTask unfinishedRestartTimerTask = null;
    private final AtomicBoolean unfinishedRestartTimerRunning = new AtomicBoolean(false);
    private final Object unfinishedRestartTimerLock = new Object();

    private Timer remoteCommandTimer = null;
    private TimerTask remoteCommandTimerTask = null;

    private Timer checkDurationTimer = null;
    private TimerTask checkDurationTimerTask = null;

    private Timer barcodeRequestTimer = null;
    private TimerTask barcodeRequestTimerTask = null;

    private Timer controlResponseTimeoutTimer = null;
    private TimerTask controlResponseTimeoutTask = null;

    private Timer controlTestTimer = null;
    private TimerTask controlTestTimerTask = null;
    private final AtomicBoolean controlTestTimerRunning = new AtomicBoolean(false);
    private final Object controlTestTimerLock = new Object();

    private TimerTask appResetTimerTask = null;

    // Generic timer storage for additional timers
    private final ConcurrentHashMap<String, Timer> genericTimers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, TimerTask> genericTimerTasks = new ConcurrentHashMap<>();

    /**
     * Start Bluetooth message timer
     */
    public void startBtMessageTimer(TimerTask task, long intervalMs) {
        synchronized (btMessageTimerLock) {
            if (btMessageTimerRunning.get()) {
                LogManager.w(LogManager.LogCategory.BT, TAG, "BT message timer already running");
                return;
            }

            if (btMessageTimer != null || btMessageTimerTask != null) {
                stopBtMessageTimer();
            }

            try {
                btMessageTimer = new Timer("BtMsgTimer");
                btMessageTimerTask = task;
                btMessageTimer.schedule(btMessageTimerTask, 0, intervalMs);
                btMessageTimerRunning.set(true);
                LogManager.i(LogManager.LogCategory.BT, TAG, "BT message timer started");
            } catch (Exception e) {
                btMessageTimerRunning.set(false);
                LogManager.e(LogManager.LogCategory.ER, TAG, "Error starting BT message timer", e);
            }
        }
    }

    /**
     * Stop Bluetooth message timer
     */
    public void stopBtMessageTimer() {
        synchronized (btMessageTimerLock) {
            if (btMessageTimerRunning.compareAndSet(true, false)) {
                if (btMessageTimer != null) {
                    try {
                        btMessageTimer.cancel();
                        btMessageTimer.purge();
                        btMessageTimer = null;
                    } catch (Exception e) {
                        LogManager.e(LogManager.LogCategory.ER, TAG, "Error canceling BT message timer", e);
                    }
                }
                if (btMessageTimerTask != null) {
                    try {
                        btMessageTimerTask.cancel();
                        btMessageTimerTask = null;
                    } catch (Exception e) {
                        LogManager.e(LogManager.LogCategory.ER, TAG, "Error canceling BT message timer task", e);
                    }
                }
                LogManager.i(LogManager.LogCategory.BT, TAG, "BT message timer stopped");
            } else {
                // Cleanup even if not running
                if (btMessageTimer != null) {
                    try {
                        btMessageTimer.cancel();
                        btMessageTimer.purge();
                        btMessageTimer = null;
                    } catch (Exception ignored) {
                    }
                }
                if (btMessageTimerTask != null) {
                    try {
                        btMessageTimerTask.cancel();
                        btMessageTimerTask = null;
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    /**
     * Start reset timer
     */
    public void startResetTimer(TimerTask task, long intervalMs) {
        synchronized (finishedRestartTimerLock) {
            cancelResetTimer();
            try {
                resetTimer = new Timer("UsbResetTimer");
                resetTimerTask = task;
                resetTimer.schedule(resetTimerTask, 0, intervalMs);
                LogManager.i(LogManager.LogCategory.PS, TAG, "Reset timer started");
            } catch (Exception e) {
                LogManager.e(LogManager.LogCategory.ER, TAG, "Error starting reset timer", e);
            }
        }
    }

    /**
     * Cancel reset timer
     */
    public void cancelResetTimer() {
        synchronized (finishedRestartTimerLock) {
            if (resetTimer != null) {
                try {
                    resetTimer.cancel();
                    resetTimer.purge();
                    resetTimer = null;
                } catch (Exception ignored) {
                }
            }
            if (resetTimerTask != null) {
                try {
                    resetTimerTask.cancel();
                    resetTimerTask = null;
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * Start finished restart timer
     */
    public void startFinishedRestartTimer(TimerTask task, long delayMs, long intervalMs) {
        synchronized (finishedRestartTimerLock) {
            if (finishedRestartTimerRunning.compareAndSet(false, true)) {
                if (finishedRestartTimer != null || finishedRestartTimerTask != null) {
                    stopFinishedRestartTimer();
                }
                try {
                    finishedRestartTimer = new Timer("FinishedRestartTimer");
                    finishedRestartTimerTask = task;
                    finishedRestartTimer.schedule(finishedRestartTimerTask, delayMs, intervalMs);
                    LogManager.i(LogManager.LogCategory.PS, TAG, "Finished restart timer started");
                } catch (Exception e) {
                    finishedRestartTimerRunning.set(false);
                    LogManager.e(LogManager.LogCategory.ER, TAG, "Error starting finished restart timer", e);
                }
            }
        }
    }

    /**
     * Stop finished restart timer
     */
    public void stopFinishedRestartTimer() {
        synchronized (finishedRestartTimerLock) {
            if (finishedRestartTimerRunning.compareAndSet(true, false)) {
                if (finishedRestartTimer != null) {
                    try {
                        finishedRestartTimer.cancel();
                        finishedRestartTimer.purge();
                        finishedRestartTimer = null;
                    } catch (Exception e) {
                        LogManager.e(LogManager.LogCategory.ER, TAG, "Error canceling finished restart timer", e);
                    }
                }
                if (finishedRestartTimerTask != null) {
                    try {
                        finishedRestartTimerTask.cancel();
                        finishedRestartTimerTask = null;
                    } catch (Exception e) {
                        LogManager.e(LogManager.LogCategory.ER, TAG, "Error canceling finished restart timer task", e);
                    }
                }
            } else {
                // Cleanup even if not running
                if (finishedRestartTimer != null) {
                    try {
                        finishedRestartTimer.cancel();
                        finishedRestartTimer.purge();
                        finishedRestartTimer = null;
                    } catch (Exception ignored) {
                    }
                }
                if (finishedRestartTimerTask != null) {
                    try {
                        finishedRestartTimerTask.cancel();
                        finishedRestartTimerTask = null;
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    /**
     * Start unfinished restart timer
     */
    public void startUnfinishedRestartTimer(TimerTask task, long delayMs, long intervalMs) {
        synchronized (unfinishedRestartTimerLock) {
            if (unfinishedRestartTimerRunning.compareAndSet(false, true)) {
                if (unfinishedRestartTimer != null || unfinishedRestartTimerTask != null) {
                    stopUnfinishedRestartTimer();
                }
                try {
                    unfinishedRestartTimer = new Timer("UnfinishedRestartTimer");
                    unfinishedRestartTimerTask = task;
                    unfinishedRestartTimer.schedule(unfinishedRestartTimerTask, delayMs, intervalMs);
                    LogManager.i(LogManager.LogCategory.PS, TAG, "Unfinished restart timer started");
                } catch (Exception e) {
                    unfinishedRestartTimerRunning.set(false);
                    LogManager.e(LogManager.LogCategory.ER, TAG, "Error starting unfinished restart timer", e);
                }
            }
        }
    }

    /**
     * Stop unfinished restart timer
     */
    public void stopUnfinishedRestartTimer() {
        synchronized (unfinishedRestartTimerLock) {
            if (unfinishedRestartTimerRunning.compareAndSet(true, false)) {
                if (unfinishedRestartTimer != null) {
                    try {
                        unfinishedRestartTimer.cancel();
                        unfinishedRestartTimer.purge();
                        unfinishedRestartTimer = null;
                    } catch (Exception e) {
                        LogManager.e(LogManager.LogCategory.ER, TAG, "Error canceling unfinished restart timer", e);
                    }
                }
                if (unfinishedRestartTimerTask != null) {
                    try {
                        unfinishedRestartTimerTask.cancel();
                        unfinishedRestartTimerTask = null;
                    } catch (Exception e) {
                        LogManager.e(LogManager.LogCategory.ER, TAG, "Error canceling unfinished restart timer task", e);
                    }
                }
            } else {
                // Cleanup even if not running
                if (unfinishedRestartTimer != null) {
                    try {
                        unfinishedRestartTimer.cancel();
                        unfinishedRestartTimer.purge();
                        unfinishedRestartTimer = null;
                    } catch (Exception ignored) {
                    }
                }
                if (unfinishedRestartTimerTask != null) {
                    try {
                        unfinishedRestartTimerTask.cancel();
                        unfinishedRestartTimerTask = null;
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    /**
     * Start remote command timer
     */
    public void startRemoteCommandTimer(TimerTask task, long delayMs, long intervalMs) {
        stopRemoteCommandTimer();
        try {
            remoteCommandTimer = new Timer("RemoteCommandTimer");
            remoteCommandTimerTask = task;
            remoteCommandTimer.schedule(remoteCommandTimerTask, delayMs, intervalMs);
            LogManager.i(LogManager.LogCategory.PS, TAG, "Remote command timer started");
        } catch (Exception e) {
            LogManager.e(LogManager.LogCategory.ER, TAG, "Error starting remote command timer", e);
        }
    }

    /**
     * Stop remote command timer
     */
    public void stopRemoteCommandTimer() {
        if (remoteCommandTimer != null) {
            try {
                remoteCommandTimer.cancel();
                remoteCommandTimer.purge();
                remoteCommandTimer = null;
            } catch (Exception e) {
                LogManager.e(LogManager.LogCategory.ER, TAG, "Error canceling remote command timer", e);
            }
        }
        if (remoteCommandTimerTask != null) {
            try {
                remoteCommandTimerTask.cancel();
                remoteCommandTimerTask = null;
            } catch (Exception e) {
                LogManager.e(LogManager.LogCategory.ER, TAG, "Error canceling remote command timer task", e);
            }
        }
    }

    /**
     * Start check duration timer
     */
    public void startCheckDurationTimer(TimerTask task, long delayMs, long intervalMs) {
        stopCheckDurationTimer();
        try {
            checkDurationTimer = new Timer("CheckDurationTimer");
            checkDurationTimerTask = task;
            checkDurationTimer.schedule(checkDurationTimerTask, delayMs, intervalMs);
            LogManager.i(LogManager.LogCategory.PS, TAG, "Check duration timer started");
        } catch (Exception e) {
            LogManager.e(LogManager.LogCategory.ER, TAG, "Error starting check duration timer", e);
        }
    }

    /**
     * Stop check duration timer
     */
    public void stopCheckDurationTimer() {
        if (checkDurationTimer != null) {
            try {
                checkDurationTimer.cancel();
                checkDurationTimer.purge();
                checkDurationTimer = null;
            } catch (Exception e) {
                LogManager.e(LogManager.LogCategory.ER, TAG, "Error canceling check duration timer", e);
            }
        }
        if (checkDurationTimerTask != null) {
            try {
                checkDurationTimerTask.cancel();
                checkDurationTimerTask = null;
            } catch (Exception e) {
                LogManager.e(LogManager.LogCategory.ER, TAG, "Error canceling check duration timer task", e);
            }
        }
    }

    /**
     * Start barcode request timer
     */
    public void startBarcodeRequestTimer(TimerTask task, long delayMs, long intervalMs) {
        stopBarcodeRequestTimer();
        try {
            barcodeRequestTimer = new Timer("BarcodeRequestTimer");
            barcodeRequestTimerTask = task;
            barcodeRequestTimer.schedule(barcodeRequestTimerTask, delayMs, intervalMs);
            LogManager.i(LogManager.LogCategory.PS, TAG, "Barcode request timer started");
        } catch (Exception e) {
            LogManager.e(LogManager.LogCategory.ER, TAG, "Error starting barcode request timer", e);
        }
    }

    /**
     * Stop barcode request timer
     */
    public void stopBarcodeRequestTimer() {
        if (barcodeRequestTimer != null) {
            try {
                barcodeRequestTimer.cancel();
                barcodeRequestTimer.purge();
                barcodeRequestTimer = null;
            } catch (Exception e) {
                LogManager.e(LogManager.LogCategory.ER, TAG, "Error canceling barcode request timer", e);
            }
        }
        if (barcodeRequestTimerTask != null) {
            try {
                barcodeRequestTimerTask.cancel();
                barcodeRequestTimerTask = null;
            } catch (Exception e) {
                LogManager.e(LogManager.LogCategory.ER, TAG, "Error canceling barcode request timer task", e);
            }
        }
    }

    /**
     * Start control response timeout timer
     */
    public void startControlResponseTimeoutTimer(TimerTask task, long delayMs) {
        stopControlResponseTimeoutTimer();
        try {
            controlResponseTimeoutTimer = new Timer("ControlResponseTimeoutTimer");
            controlResponseTimeoutTask = task;
            controlResponseTimeoutTimer.schedule(controlResponseTimeoutTask, delayMs);
            LogManager.i(LogManager.LogCategory.PS, TAG, "Control response timeout timer started");
        } catch (Exception e) {
            LogManager.e(LogManager.LogCategory.ER, TAG, "Error starting control response timeout timer", e);
        }
    }

    /**
     * Stop control response timeout timer
     */
    public void stopControlResponseTimeoutTimer() {
        if (controlResponseTimeoutTimer != null) {
            try {
                controlResponseTimeoutTimer.cancel();
                controlResponseTimeoutTimer.purge();
                controlResponseTimeoutTimer = null;
            } catch (Exception e) {
                LogManager.e(LogManager.LogCategory.ER, TAG, "Error canceling control response timeout timer", e);
            }
        }
        if (controlResponseTimeoutTask != null) {
            try {
                controlResponseTimeoutTask.cancel();
                controlResponseTimeoutTask = null;
            } catch (Exception e) {
                LogManager.e(LogManager.LogCategory.ER, TAG, "Error canceling control response timeout timer task", e);
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
                    LogManager.e(LogManager.LogCategory.ER, TAG, "Error starting control test timer", e);
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
                        LogManager.e(LogManager.LogCategory.ER, TAG, "Error canceling control test timer", e);
                    }
                }
                if (controlTestTimerTask != null) {
                    try {
                        controlTestTimerTask.cancel();
                        controlTestTimerTask = null;
                    } catch (Exception e) {
                        LogManager.e(LogManager.LogCategory.ER, TAG, "Error canceling control test timer task", e);
                    }
                }
            } else {
                // Cleanup even if not running
                if (controlTestTimer != null) {
                    try {
                        controlTestTimer.cancel();
                        controlTestTimer.purge();
                        controlTestTimer = null;
                    } catch (Exception ignored) {
                    }
                }
                if (controlTestTimerTask != null) {
                    try {
                        controlTestTimerTask.cancel();
                        controlTestTimerTask = null;
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    /**
     * Set app reset timer task
     */
    public void setAppResetTimerTask(TimerTask task) {
        if (appResetTimerTask != null) {
            try {
                appResetTimerTask.cancel();
            } catch (Exception ignored) {
            }
        }
        appResetTimerTask = task;
    }

    /**
     * Stop app reset timer task
     */
    public void stopAppResetTimerTask() {
        if (appResetTimerTask != null) {
            try {
                appResetTimerTask.cancel();
                appResetTimerTask = null;
            } catch (Exception e) {
                LogManager.e(LogManager.LogCategory.ER, TAG, "Error canceling app reset timer task", e);
            }
        }
    }

    /**
     * Start a generic timer with a name
     */
    public void startGenericTimer(String name, TimerTask task, long delayMs, long intervalMs) {
        stopGenericTimer(name);
        try {
            Timer timer = new Timer(name);
            genericTimers.put(name, timer);
            genericTimerTasks.put(name, task);
            timer.schedule(task, delayMs, intervalMs);
            LogManager.i(LogManager.LogCategory.PS, TAG, "Generic timer started: " + name);
        } catch (Exception e) {
            LogManager.e(LogManager.LogCategory.ER, TAG, "Error starting generic timer: " + name, e);
        }
    }

    /**
     * Stop a generic timer by name
     */
    public void stopGenericTimer(String name) {
        Timer timer = genericTimers.remove(name);
        if (timer != null) {
            try {
                timer.cancel();
                timer.purge();
            } catch (Exception e) {
                LogManager.e(LogManager.LogCategory.ER, TAG, "Error canceling generic timer: " + name, e);
            }
        }
        TimerTask task = genericTimerTasks.remove(name);
        if (task != null) {
            try {
                task.cancel();
            } catch (Exception e) {
                LogManager.e(LogManager.LogCategory.ER, TAG, "Error canceling generic timer task: " + name, e);
            }
        }
    }

    /**
     * Stop all timers
     * MUST be called in onDestroy()
     */
    public void stopAllTimers() {
        try {
            stopBtMessageTimer();
            stopFinishedRestartTimer();
            stopUnfinishedRestartTimer();
            stopRemoteCommandTimer();
            cancelResetTimer();
            stopCheckDurationTimer();
            stopBarcodeRequestTimer();
            stopAppResetTimerTask();
            stopControlResponseTimeoutTimer();
            stopControlTestTimer();

            // Stop all generic timers
            for (String name : genericTimers.keySet()) {
                stopGenericTimer(name);
            }

            LogManager.i(LogManager.LogCategory.PS, TAG, "All timers stopped");
        } catch (Exception e) {
            LogManager.e(LogManager.LogCategory.ER, TAG, "Error stopping all timers", e);
        }
    }

    /**
     * Check if BT message timer is running
     */
    public boolean isBtMessageTimerRunning() {
        return btMessageTimerRunning.get();
    }

    /**
     * Check if control test timer is running
     */
    public boolean isControlTestTimerRunning() {
        return controlTestTimerRunning.get();
    }
}


