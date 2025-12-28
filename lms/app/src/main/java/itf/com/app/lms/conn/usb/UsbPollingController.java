package itf.com.app.lms.conn.usb;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import itf.com.app.lms.util.Constants;
import itf.com.app.lms.util.LogManager;
import itf.com.app.lms.util.UsbCommandQueue;
import itf.com.app.lms.util.UsbService;

public class UsbPollingController {
    public interface Dependencies {
        void logInfo(LogManager.LogCategory category, String message);

        void logWarn(LogManager.LogCategory category, String message);

        void logDebug(LogManager.LogCategory category, String message);

        void logError(LogManager.LogCategory category, String message, Throwable throwable);

        void scheduleUsbPermissionRecovery();

        void updateUsbLampDisconnected();

        void updateUsbLampReconnecting();

        void updateUsbLampReady();

        void onUsbReconnectFailed();

        void startUsbService();

        boolean isUsbPermissionGranted();

        UsbService getUsbService();

        UsbCommandQueue getUsbCommandQueue();

        void setUsbCommandQueue(UsbCommandQueue queue);
    }

    private static final int USB_POLLING_FAILURE_THRESHOLD = 5;

    private final Dependencies deps;
    private final ScheduledExecutorService pollingExecutor;
    private ScheduledFuture<?> pollingFuture;
    private final long defaultPollingIntervalMs;
    private long pollingIntervalMs;
    private final long pollingBackoffMs;
    private final long permissionRecoveryDelayMs;
    private final int maxReconnectAttempts;

    private volatile boolean pollingEnabled = false;
    private boolean pollingRequested = false;
    private int pollingFailureCount = 0;

    private final Handler reconnectHandler = new Handler(Looper.getMainLooper());
    private boolean isUsbReconnecting = false;
    private int reconnectAttempts = 0;
    private Runnable reconnectRunnable = null;

    public UsbPollingController(
            Dependencies deps,
            long defaultPollingIntervalMs,
            long pollingBackoffMs,
            long permissionRecoveryDelayMs,
            int maxReconnectAttempts
    ) {
        this.deps = deps;
        this.defaultPollingIntervalMs = defaultPollingIntervalMs;
        this.pollingIntervalMs = defaultPollingIntervalMs;
        this.pollingBackoffMs = pollingBackoffMs;
        this.permissionRecoveryDelayMs = permissionRecoveryDelayMs;
        this.maxReconnectAttempts = maxReconnectAttempts;

        this.pollingExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "UsbPolling");
                thread.setPriority(Thread.NORM_PRIORITY - 2);
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    public void startPolling(boolean immediate) {
        if (pollingExecutor.isShutdown()) {
            deps.logWarn(LogManager.LogCategory.US, "USB polling executor is shut down; cannot schedule polling");
            return;
        }
        UsbService usbService = deps.getUsbService();
        if (usbService == null) {
            deps.logWarn(LogManager.LogCategory.US, "UsbService is null; skipping polling start");
            return;
        }
        if (!deps.isUsbPermissionGranted()) {
            deps.logWarn(LogManager.LogCategory.US, "USB permission not granted; skipping polling start");
            deps.scheduleUsbPermissionRecovery();
            return;
        }

        UsbCommandQueue usbCommandQueue = deps.getUsbCommandQueue();
        if (usbCommandQueue == null) {
            usbCommandQueue = new UsbCommandQueue();
            usbCommandQueue.setUsbService(usbService);
            usbCommandQueue.start();
            deps.setUsbCommandQueue(usbCommandQueue);
            deps.logInfo(LogManager.LogCategory.US, "USB command queue initialized and started");
        }

        boolean pollingActive = pollingEnabled && pollingFuture != null && !pollingFuture.isCancelled();
        if (pollingActive) {
            deps.logDebug(LogManager.LogCategory.US, "USB polling already running; skipping restart");
            return;
        }
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> stopUsbPolling 2");
        stopPolling();
        pollingEnabled = true;
        pollingRequested = true;
        pollingFailureCount = 0;
        long initialDelay = immediate ? 0 : pollingIntervalMs;
        try {
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> stopUsbPolling 2-1 usbPollingEnabled:" + pollingEnabled + " usbService:" + usbService);
            pollingFuture = pollingExecutor.scheduleAtFixedRate(() -> {
                if (!pollingEnabled) {
                    System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> stopUsbPolling 3");
                    stopPolling();
                    return;
                }
                UsbService service = deps.getUsbService();
                UsbCommandQueue queue = deps.getUsbCommandQueue();
                if (service == null || queue == null) {
                    deps.logWarn(LogManager.LogCategory.US, "UsbService or command queue is null; stopping polling");
                    System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> stopUsbPolling 4");
                    stopPolling();
                    return;
                }
                try {
                    UsbCommandQueue.UsbCommand pollingCommand = new UsbCommandQueue.UsbCommand(
                            UsbCommandQueue.CommandType.POLLING,
                            Constants.PLCCommands.RSS0107_DW1006,
                            "Power consumption polling"
                    );

                    boolean enqueued = queue.enqueue(pollingCommand);
                    if (enqueued) {
                        pollingFailureCount = 0;
                    } else {
                        pollingFailureCount++;
                        deps.logWarn(LogManager.LogCategory.US, "Failed to enqueue polling command (queue may be full)");
                    }

                    if (pollingFailureCount >= USB_POLLING_FAILURE_THRESHOLD) {
                        deps.logWarn(LogManager.LogCategory.US, "USB polling failure threshold reached; backing off");
                        stopPolling();
                        pollingIntervalMs = pollingBackoffMs;
                        startPolling(false);
                    }
                } catch (Exception e) {
                    pollingFailureCount++;
                    deps.logError(LogManager.LogCategory.ER, Constants.ErrorMessages.USB_SERVICE_ERROR, e);
                    if (pollingFailureCount >= USB_POLLING_FAILURE_THRESHOLD) {
                        deps.logWarn(LogManager.LogCategory.US, "USB polling failure threshold reached; backing off");
                        stopPolling();
                        pollingIntervalMs = pollingBackoffMs;
                        startPolling(false);
                    }
                }
            }, initialDelay, pollingIntervalMs, TimeUnit.MILLISECONDS);
            deps.logInfo(LogManager.LogCategory.US, "USB polling scheduled (interval: " + pollingIntervalMs + " ms)");
        } catch (Exception e) {
            deps.logError(LogManager.LogCategory.ER, "Failed to schedule USB polling", e);
            pollingEnabled = false;
            pollingRequested = false;
        }
    }

    public void stopPolling() {
        pollingEnabled = false;
        pollingRequested = false;
        pollingIntervalMs = defaultPollingIntervalMs;
        if (pollingFuture != null) {
            pollingFuture.cancel(true);
            pollingFuture = null;
            deps.logInfo(LogManager.LogCategory.US, "USB polling stopped");
        }
    }

    public boolean isPollingActive() {
        return pollingEnabled && pollingFuture != null && !pollingFuture.isCancelled();
    }

    public boolean isPollingEnabled() {
        return pollingEnabled;
    }

    public void scheduleReconnect(boolean immediate) {
        if (isUsbReconnecting) {
            return;
        }
        isUsbReconnecting = true;
        if (reconnectRunnable == null) {
            reconnectRunnable = this::attemptReconnect;
        }
        reconnectHandler.removeCallbacks(reconnectRunnable);
        reconnectHandler.postDelayed(reconnectRunnable, immediate ? 0 : permissionRecoveryDelayMs);
        deps.updateUsbLampReconnecting();
    }

    public void cancelReconnect() {
        reconnectHandler.removeCallbacks(reconnectRunnable);
        isUsbReconnecting = false;
    }

    public boolean tryReconnect() {
        try {
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> stopUsbPolling 6");
            stopPolling();
            deps.startUsbService();
            return deps.isUsbPermissionGranted() && deps.getUsbService() != null;
        } catch (Exception e) {
            deps.logError(LogManager.LogCategory.US, "USB reconnect attempt failed", e);
            return false;
        }
    }

    public int getReconnectAttempts() {
        return reconnectAttempts;
    }

    public void resetReconnectAttempts() {
        reconnectAttempts = 0;
    }

    public int incrementReconnectAttempts() {
        reconnectAttempts++;
        return reconnectAttempts;
    }

    public int getMaxReconnectAttempts() {
        return maxReconnectAttempts;
    }

    public void resetPollingState() {
        pollingRequested = false;
        pollingEnabled = false;
        pollingFailureCount = 0;
        pollingFuture = null;
        pollingIntervalMs = defaultPollingIntervalMs;
    }

    public void clearReconnectCallbacks() {
        reconnectHandler.removeCallbacksAndMessages(null);
        if (reconnectRunnable != null) {
            reconnectHandler.removeCallbacks(reconnectRunnable);
        }
        reconnectRunnable = null;
        isUsbReconnecting = false;
    }

    public void shutdown() {
        if (!pollingExecutor.isShutdown()) {
            pollingExecutor.shutdownNow();
            try {
                if (!pollingExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                    deps.logWarn(LogManager.LogCategory.US, "usbPollingExecutor did not terminate within timeout");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                deps.logWarn(LogManager.LogCategory.US, "Interrupted while waiting for usbPollingExecutor termination");
            }
        }
    }

    private void attemptReconnect() {
        if (!isUsbReconnecting) {
            return;
        }
        boolean success = tryReconnect();
        if (success) {
            cancelReconnect();
            reconnectAttempts = 0;
            deps.updateUsbLampReady();
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> startUsbPolling 4");
            startPolling(true);
            return;
        }
        reconnectAttempts++;
        if (reconnectAttempts >= maxReconnectAttempts) {
            reconnectAttempts = 0;
            deps.logWarn(LogManager.LogCategory.US, "USB reconnect failed after " + reconnectAttempts + " attempts");
            cancelReconnect();
            deps.scheduleUsbPermissionRecovery();
            deps.onUsbReconnectFailed();
        } else {
            reconnectHandler.postDelayed(
                    reconnectRunnable,
                    permissionRecoveryDelayMs * Math.min(reconnectAttempts, 5)
            );
        }
    }
}
