package itf.com.app.lms.conn.usb;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.core.content.ContextCompat;

import itf.com.app.lms.util.Constants;
import itf.com.app.lms.util.LogManager;
import itf.com.app.lms.util.UsbCommandQueue;
import itf.com.app.lms.util.UsbService;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * UsbConnectionManager - Handles all USB communication operations
 *
 * Responsibilities:
 * - USB device discovery and permission handling
 * - USB service connection management
 * - USB polling management
 * - USB command queue management
 * - USB reconnection logic
 * - USB response handling
 *
 * IMPORTANT: This is NOT a static class - it must be properly instantiated
 * and cleaned up to prevent memory leaks.
 *
 * Usage:
 * <pre>
 * UsbConnectionManager usbManager = new UsbConnectionManager(context, listener);
 * usbManager.initialize();
 * usbManager.startUsbSearch();
 * // ... use manager
 * usbManager.cleanup(); // MUST call in onDestroy()
 * </pre>
 */
public class UsbConnectionManager {

    private static final String TAG = "UsbConnectionManager";
    private static final String ACTION_USB_PERMISSION = "itf.com.app.USB_PERMISSION";
    private static final int USB_POLLING_FAILURE_THRESHOLD = 5;
    private static final long USB_PERMISSION_RECOVERY_DELAY_MS = 1000L;
    private static final int USB_RETRY_MAX_ATTEMPTS = 10;

    // Context reference (use ApplicationContext to prevent leaks)
    private final Context appContext;

    // Listener for callbacks
    private final UsbListener listener;

    // USB components
    private UsbService usbService;
    private UsbCommandQueue usbCommandQueue;
    private BroadcastReceiver usbReceiver;
    private ServiceConnection usbConnection;
    private boolean usbConnPermissionGranted = false;
    private boolean usbReceiverRegistered = false;

    // Polling management
    private final ScheduledExecutorService usbPollingExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "UsbPolling");
        thread.setPriority(Thread.NORM_PRIORITY - 2);
        thread.setDaemon(true);
        return thread;
    });
    private ScheduledFuture<?> usbPollingFuture = null;
    private long usbPollingIntervalMs = Constants.Timeouts.USB_TIMER_INTERVAL_MS;
    private volatile boolean usbPollingEnabled = false;
    private int usbPollingFailureCount = 0;
    private boolean usbPollingRequested = false;

    // Reconnection management
    private final Handler usbRecoveryHandler = new Handler(Looper.getMainLooper());
    private Runnable usbPermissionRecoveryRunnable = null;
    private final Handler usbReconnectHandler = new Handler(Looper.getMainLooper());
    private boolean isUsbReconnecting = false;
    private int usbReconnectAttempts = 0;
    private Runnable usbReconnectRunnable = null;

    // Response handling
    private final Object usbResponseLock = new Object();
    private final Map<String, CompletableFuture<String>> pendingUsbResponses = new ConcurrentHashMap<>();

    // State
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);

    /**
     * Listener interface for USB events
     */
    public interface UsbListener {
        /**
         * Called when USB response is received
         */
        void onUsbResponseReceived(String response);

        /**
         * Called when USB connection state changes
         */
        void onUsbConnectionStateChanged(ConnectionState state);

        /**
         * Called when an error occurs
         */
        void onUsbError(UsbError error, String message);

        /**
         * Called when USB permission is needed
         */
        void onUsbPermissionRequired();

        /**
         * Called when USB data is received (for watt measurement)
         */
        void onUsbDataReceived(String data, int electricValue);
    }

    /**
     * Connection state enum
     */
    public enum ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        RECONNECTING,
        FAILED
    }

    /**
     * USB error types
     */
    public enum UsbError {
        SERVICE_NOT_AVAILABLE,
        PERMISSION_DENIED,
        CONNECTION_FAILED,
        DEVICE_NOT_FOUND,
        POLLING_FAILED,
        UNKNOWN
    }

    /**
     * Constructor
     *
     * @param context Application context (NOT Activity context to prevent leaks)
     * @param listener Callback listener for events
     */
    public UsbConnectionManager(Context context, UsbListener listener) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }

        this.appContext = context.getApplicationContext();
        this.listener = listener;
    }

    /**
     * Initialize USB manager
     * Call this once after construction
     */
    public void initialize() {
        if (isInitialized.getAndSet(true)) {
            LogManager.w(LogManager.LogCategory.US, TAG, "UsbConnectionManager already initialized");
            return;
        }

        LogManager.i(LogManager.LogCategory.US, TAG, "UsbConnectionManager initialized successfully");
    }

    /**
     * Start USB device search
     */
    public void startUsbSearch() {
        if (!ensureInitialized()) {
            return;
        }

        try {
            final String ACTION_USB_PERMISSION = "itf.com.app.USB_PERMISSION";
            UsbManager usbManager = (UsbManager) appContext.getSystemService(Context.USB_SERVICE);
            if (usbManager == null) {
                LogManager.w(LogManager.LogCategory.US, TAG, "UsbManager is null");
                return;
            }

            HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
            if (deviceList == null || deviceList.isEmpty()) {
                return;
            }

            for (UsbDevice device : deviceList.values()) {
                if (!usbManager.hasPermission(device)) {
                    Intent intent = new Intent(ACTION_USB_PERMISSION);
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(
                            appContext, 0, intent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                    usbManager.requestPermission(device, pendingIntent);
                    listener.onUsbPermissionRequired();
                } else {
                    if (usbService != null) {
                        // Service already connected
                    } else {
                        Intent usbIntent = new Intent(appContext, UsbService.class);
                        try {
                            appContext.stopService(usbIntent);
                        } catch (Exception ignored) {
                        }
                        try {
                            appContext.startService(usbIntent);
                        } catch (Exception e) {
                            LogManager.w(LogManager.LogCategory.US, TAG, "Start UsbService failed: " + e.getMessage());
                        }
                    }
                }
                break; // 첫 번째 장치만 처리
            }
        } catch (Exception e) {
            LogManager.e(LogManager.LogCategory.ER, TAG, "usbSearch error", e);
            listener.onUsbError(UsbError.UNKNOWN, e.getMessage());
        }
    }

    /**
     * Start USB polling
     */
    public void startUsbPolling(boolean immediate) {
        if (usbPollingExecutor.isShutdown()) {
            LogManager.w(LogManager.LogCategory.US, TAG, "USB polling executor is shut down");
            return;
        }
        if (usbService == null) {
            LogManager.w(LogManager.LogCategory.US, TAG, "UsbService is null; skipping polling start");
            return;
        }
        if (!usbConnPermissionGranted) {
            LogManager.w(LogManager.LogCategory.US, TAG, "USB permission not granted; skipping polling start");
            scheduleUsbPermissionRecovery();
            return;
        }

        // 명령 큐 초기화 및 시작
        if (usbCommandQueue == null) {
            usbCommandQueue = new UsbCommandQueue();
            usbCommandQueue.setUsbService(usbService);
            usbCommandQueue.start();
            LogManager.i(LogManager.LogCategory.US, TAG, "USB command queue initialized and started");
        }

        boolean pollingActive = usbPollingEnabled && usbPollingFuture != null && !usbPollingFuture.isCancelled();
        if (pollingActive) {
            LogManager.d(LogManager.LogCategory.US, TAG, "USB polling already running; skipping restart");
            return;
        }

        stopUsbPolling();
        usbPollingEnabled = true;
        usbPollingRequested = true;
        usbPollingFailureCount = 0;
        long initialDelay = immediate ? 0 : usbPollingIntervalMs;

        try {
            usbPollingFuture = usbPollingExecutor.scheduleAtFixedRate(() -> {
                if (!usbPollingEnabled) {
                    stopUsbPolling();
                    return;
                }
                if (usbService == null || usbCommandQueue == null) {
                    LogManager.w(LogManager.LogCategory.US, TAG, "UsbService or command queue is null; stopping polling");
                    stopUsbPolling();
                    return;
                }
                try {
                    // 명령 큐를 통해 폴링 명령 전송
                    UsbCommandQueue.UsbCommand pollingCommand = new UsbCommandQueue.UsbCommand(
                            UsbCommandQueue.CommandType.POLLING,
                            Constants.PLCCommands.RSS0107_DW1006,
                            "Power consumption polling"
                    );

                    boolean enqueued = usbCommandQueue.enqueue(pollingCommand);
                    if (enqueued) {
                        usbPollingFailureCount = 0;
                    } else {
                        usbPollingFailureCount++;
                        LogManager.w(LogManager.LogCategory.US, TAG, "Failed to enqueue polling command");
                    }

                    if (usbPollingFailureCount >= USB_POLLING_FAILURE_THRESHOLD) {
                        LogManager.w(LogManager.LogCategory.US, TAG, "USB polling failure threshold reached; backing off");
                        stopUsbPolling();
                        usbPollingIntervalMs = Constants.Timeouts.USB_TIMER_INTERVAL_MS * 5;
                        startUsbPolling(false);
                    }
                } catch (Exception e) {
                    usbPollingFailureCount++;
                    LogManager.e(LogManager.LogCategory.ER, TAG, "USB polling error", e);
                    if (usbPollingFailureCount >= USB_POLLING_FAILURE_THRESHOLD) {
                        LogManager.w(LogManager.LogCategory.US, TAG, "USB polling failure threshold reached; backing off");
                        stopUsbPolling();
                        usbPollingIntervalMs = Constants.Timeouts.USB_TIMER_INTERVAL_MS * 5;
                        startUsbPolling(false);
                    }
                }
            }, initialDelay, usbPollingIntervalMs, TimeUnit.MILLISECONDS);
            LogManager.i(LogManager.LogCategory.US, TAG, "USB polling scheduled (interval: " + usbPollingIntervalMs + " ms)");
        } catch (Exception e) {
            LogManager.e(LogManager.LogCategory.ER, TAG, "Failed to schedule USB polling", e);
            usbPollingEnabled = false;
            usbPollingRequested = false;
        }
    }

    /**
     * Stop USB polling
     */
    public void stopUsbPolling() {
        usbPollingEnabled = false;
        usbPollingRequested = false;
        usbPollingIntervalMs = Constants.Timeouts.USB_TIMER_INTERVAL_MS;
        if (usbPollingFuture != null) {
            usbPollingFuture.cancel(true);
            usbPollingFuture = null;
            LogManager.i(LogManager.LogCategory.US, TAG, "USB polling stopped");
        }
    }

    /**
     * Send USB command
     */
    public boolean sendUsbCommand(String command, String description, Runnable onSuccess, Runnable onError) {
        if (usbCommandQueue == null || !usbCommandQueue.isRunning()) {
            LogManager.w(LogManager.LogCategory.US, TAG, "USB command queue is not running");
            if (onError != null) {
                onError.run();
            }
            return false;
        }

        if (command == null || command.trim().isEmpty()) {
            LogManager.w(LogManager.LogCategory.US, TAG, "Invalid command");
            if (onError != null) {
                onError.run();
            }
            return false;
        }

        UsbCommandQueue.UsbCommand usbCommand = new UsbCommandQueue.UsbCommand(
                UsbCommandQueue.CommandType.USER_COMMAND,
                command.getBytes(),
                description,
                onSuccess,
                onError
        );

        boolean enqueued = usbCommandQueue.enqueuePriority(usbCommand);
        if (enqueued) {
            LogManager.i(LogManager.LogCategory.US, TAG, "USB command enqueued: " + description);
        } else {
            LogManager.w(LogManager.LogCategory.US, TAG, "Failed to enqueue USB command: " + description);
            if (onError != null) {
                onError.run();
            }
        }

        return enqueued;
    }

    /**
     * Send USB command with response
     */
    public CompletableFuture<String> sendUsbCommandWithResponse(String command, String description, long timeoutMs) {
        CompletableFuture<String> future = new CompletableFuture<>();

        if (usbCommandQueue == null || !usbCommandQueue.isRunning()) {
            future.completeExceptionally(new RuntimeException("USB command queue is not running"));
            return future;
        }

        String responseKey = "resp_" + System.currentTimeMillis() + "_" + description.hashCode();

        synchronized (usbResponseLock) {
            pendingUsbResponses.put(responseKey, future);
        }

        Runnable onSuccess = () -> {
            // Command sent successfully
        };

        Runnable onError = () -> {
            synchronized (usbResponseLock) {
                pendingUsbResponses.remove(responseKey);
            }
            future.completeExceptionally(new RuntimeException("Failed to send command: " + description));
        };

        boolean sent = sendUsbCommand(command, description, onSuccess, onError);
        if (!sent) {
            synchronized (usbResponseLock) {
                pendingUsbResponses.remove(responseKey);
            }
            future.completeExceptionally(new RuntimeException("Failed to enqueue command: " + description));
            return future;
        }

        // Timeout handling
        final String finalResponseKey = responseKey;
        new Thread(() -> {
            try {
                Thread.sleep(timeoutMs);
                synchronized (usbResponseLock) {
                    CompletableFuture<String> removed = pendingUsbResponses.remove(finalResponseKey);
                    if (removed != null && !removed.isDone()) {
                        removed.completeExceptionally(new java.util.concurrent.TimeoutException("Response timeout"));
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                synchronized (usbResponseLock) {
                    CompletableFuture<String> removed = pendingUsbResponses.remove(finalResponseKey);
                    if (removed != null && !removed.isDone()) {
                        removed.completeExceptionally(e);
                    }
                }
            }
        }).start();

        return future;
    }

    /**
     * Handle USB response
     */
    public void handleUsbResponse(String response) {
        if (response == null || response.isEmpty()) {
            return;
        }

        synchronized (usbResponseLock) {
            if (!pendingUsbResponses.isEmpty()) {
                String oldestKey = null;
                long oldestTime = Long.MAX_VALUE;
                for (String key : pendingUsbResponses.keySet()) {
                    try {
                        String[] parts = key.split("_");
                        if (parts.length > 1) {
                            long timestamp = Long.parseLong(parts[1]);
                            if (timestamp < oldestTime) {
                                oldestTime = timestamp;
                                oldestKey = key;
                            }
                        }
                    } catch (Exception e) {
                        // Ignore parsing errors
                    }
                }

                if (oldestKey != null) {
                    CompletableFuture<String> future = pendingUsbResponses.remove(oldestKey);
                    if (future != null && !future.isDone()) {
                        future.complete(response);
                    }
                }
            }
        }

        listener.onUsbResponseReceived(response);
    }

    /**
     * Schedule USB reconnect
     */
    public void scheduleUsbReconnect(boolean immediate) {
        if (isUsbReconnecting) {
            return;
        }
        isUsbReconnecting = true;
        if (usbReconnectRunnable == null) {
            usbReconnectRunnable = this::attemptUsbReconnect;
        }
        usbReconnectHandler.removeCallbacks(usbReconnectRunnable);
        usbReconnectHandler.postDelayed(usbReconnectRunnable, immediate ? 0 : USB_PERMISSION_RECOVERY_DELAY_MS);
        listener.onUsbConnectionStateChanged(ConnectionState.RECONNECTING);
    }

    /**
     * Cancel USB reconnect
     */
    public void cancelUsbReconnect() {
        usbReconnectHandler.removeCallbacks(usbReconnectRunnable);
        isUsbReconnecting = false;
    }

    /**
     * Attempt USB reconnect
     */
    private void attemptUsbReconnect() {
        if (!isUsbReconnecting) {
            return;
        }
        // Reconnection logic would go here
        // This is a simplified version
        usbReconnectAttempts++;
        if (usbReconnectAttempts >= USB_RETRY_MAX_ATTEMPTS) {
            usbReconnectAttempts = 0;
            LogManager.w(LogManager.LogCategory.US, TAG, "USB reconnect failed after max attempts");
            cancelUsbReconnect();
            scheduleUsbPermissionRecovery();
            listener.onUsbConnectionStateChanged(ConnectionState.FAILED);
        } else {
            usbReconnectHandler.postDelayed(usbReconnectRunnable,
                    USB_PERMISSION_RECOVERY_DELAY_MS * Math.min(usbReconnectAttempts, 5));
        }
    }

    /**
     * Schedule USB permission recovery
     */
    private void scheduleUsbPermissionRecovery() {
        if (usbConnPermissionGranted) {
            LogManager.d(LogManager.LogCategory.US, TAG, "USB permission already granted");
            return;
        }

        if (usbPermissionRecoveryRunnable == null) {
            usbPermissionRecoveryRunnable = () -> {
                if (usbConnPermissionGranted) {
                    return;
                }
                LogManager.i(LogManager.LogCategory.US, TAG, "Attempting USB permission recovery");
                listener.onUsbPermissionRequired();
            };
        }

        usbRecoveryHandler.removeCallbacks(usbPermissionRecoveryRunnable);
        usbRecoveryHandler.postDelayed(usbPermissionRecoveryRunnable, USB_PERMISSION_RECOVERY_DELAY_MS);
    }

    /**
     * Set USB service
     */
    public void setUsbService(UsbService service) {
        this.usbService = service;
        if (usbCommandQueue != null) {
            usbCommandQueue.setUsbService(service);
        }
    }

    /**
     * Set USB connection permission granted
     */
    public void setUsbConnPermissionGranted(boolean granted) {
        this.usbConnPermissionGranted = granted;
        if (granted) {
            listener.onUsbConnectionStateChanged(ConnectionState.CONNECTED);
        }
    }

    /**
     * Check if USB is ready
     */
    public boolean isUsbReady() {
        return usbConnPermissionGranted && usbService != null && usbPollingEnabled;
    }

    /**
     * Cleanup resources
     * MUST be called in onDestroy()
     */
    public void cleanup() {
        stopUsbPolling();
        if (usbPollingExecutor != null && !usbPollingExecutor.isShutdown()) {
            usbPollingExecutor.shutdown();
        }
        cancelUsbReconnect();
        if (usbRecoveryHandler != null) {
            usbRecoveryHandler.removeCallbacks(usbPermissionRecoveryRunnable);
        }
        synchronized (usbResponseLock) {
            pendingUsbResponses.clear();
        }
        isInitialized.set(false);
        LogManager.i(LogManager.LogCategory.US, TAG, "UsbConnectionManager cleaned up");
    }

    /**
     * Ensure initialized
     */
    private boolean ensureInitialized() {
        if (!isInitialized.get()) {
            LogManager.w(LogManager.LogCategory.US, TAG, "UsbConnectionManager not initialized");
            return false;
        }
        return true;
    }
}


