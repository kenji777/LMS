package itf.com.app.simple_line_test_ovio_new.conn.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import itf.com.app.simple_line_test_ovio_new.util.ConnectedThreadOptimized;

/**
 * BluetoothManager - Handles all Bluetooth communication operations
 *
 * Responsibilities:
 * - Bluetooth adapter management
 * - Device discovery and pairing
 * - Connection establishment
 * - Message sending/receiving
 * - Reconnection logic
 * - Permission handling
 *
 * IMPORTANT: This is NOT a static class - it must be properly instantiated
 * and cleaned up to prevent memory leaks.
 *
 * Usage:
 * <pre>
 * BluetoothManager btManager = new BluetoothManager(context, listener);
 * btManager.initialize();
 * btManager.startDeviceSearch();
 * // ... use manager
 * btManager.cleanup(); // MUST call in onDestroy()
 * </pre>
 */
public class BluetoothManager {

    private static final String TAG = "BluetoothManager";
    private static final UUID BT_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int MESSAGE_READ = 2;

    // Context reference (use ApplicationContext to prevent leaks)
    private final Context appContext;

    // Listener for callbacks
    private final BluetoothListener listener;

    // Bluetooth components (NON-STATIC to prevent memory leaks)
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private ConnectedThreadOptimized connectedThread;
    private Set<BluetoothDevice> pairedDevices;

    // Thread management
    private final ExecutorService btWorkerExecutor;
    private final Handler mainHandler;

    // Connection state
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final AtomicBoolean isReconnecting = new AtomicBoolean(false);
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);

    // Reconnection parameters
    private String lastConnectedDeviceName;
    private String lastConnectedDeviceAddress;
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private static final long RECONNECT_DELAY_MS = 3000;

    // Permissions
    private boolean permissionsGranted = false;

    /**
     * Listener interface for Bluetooth events
     */
    public interface BluetoothListener {
        /**
         * Called when a message is received from Bluetooth device
         */
        void onMessageReceived(byte[] data);

        /**
         * Called when connection status changes
         */
        void onConnectionStateChanged(ConnectionState state);

        /**
         * Called when an error occurs
         */
        void onError(BluetoothError error, String message);

        /**
         * Called when permissions are needed
         */
        void onPermissionsRequired(String[] permissions);
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
     * Bluetooth error types
     */
    public enum BluetoothError {
        ADAPTER_NOT_AVAILABLE,
        PERMISSION_DENIED,
        CONNECTION_FAILED,
        DEVICE_NOT_FOUND,
        SEND_MESSAGE_FAILED,
        UNKNOWN
    }

    /**
     * Constructor
     *
     * @param context Application context (NOT Activity context to prevent leaks)
     * @param listener Callback listener for events
     */
    public BluetoothManager(Context context, BluetoothListener listener) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }

        this.appContext = context.getApplicationContext();
        this.listener = listener;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.btWorkerExecutor = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r);
            thread.setName("BT-Worker-" + thread.getId());
            thread.setPriority(Thread.NORM_PRIORITY - 1);
            return thread;
        });
    }

    /**
     * Initialize Bluetooth manager
     * Call this once after construction
     */
    public void initialize() {
        if (isInitialized.getAndSet(true)) {
            Log.w(TAG, "BluetoothManager already initialized");
            return;
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            notifyError(BluetoothError.ADAPTER_NOT_AVAILABLE, "Bluetooth not available on this device");
            return;
        }

        if (!checkPermissions()) {
            requestPermissions();
            return;
        }

        permissionsGranted = true;
        Log.i(TAG, "BluetoothManager initialized successfully");
    }

    /**
     * Start searching for Bluetooth devices
     */
    public void startDeviceSearch() {
        if (!ensureInitialized() || !ensurePermissions()) {
            return;
        }

        btWorkerExecutor.execute(() -> {
            try {
                Log.i(TAG, "Starting device search...");

                if (ActivityCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    notifyError(BluetoothError.PERMISSION_DENIED, "BLUETOOTH_CONNECT permission not granted");
                    return;
                }

                pairedDevices = bluetoothAdapter.getBondedDevices();

                if (pairedDevices == null || pairedDevices.isEmpty()) {
                    Log.w(TAG, "No paired devices found");
                    notifyConnectionState(ConnectionState.DISCONNECTED);
                    return;
                }

                Log.i(TAG, "Found " + pairedDevices.size() + " paired devices");

                // Try to find preferred device
                BluetoothDevice targetDevice = selectPreferredDevice();
                if (targetDevice != null) {
                    connectToDevice(targetDevice);
                } else {
                    Log.w(TAG, "No preferred device found");
                    notifyConnectionState(ConnectionState.DISCONNECTED);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error during device search", e);
                notifyError(BluetoothError.UNKNOWN, "Device search failed: " + e.getMessage());
            }
        });
    }

    /**
     * Select preferred device from paired devices
     * Override this method for custom device selection logic
     */
    protected BluetoothDevice selectPreferredDevice() {
        if (pairedDevices == null || pairedDevices.isEmpty()) {
            return null;
        }

        // Try to reconnect to last connected device
        if (lastConnectedDeviceAddress != null) {
            for (BluetoothDevice device : pairedDevices) {
                if (ActivityCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return null;
                }
                if (device.getAddress().equals(lastConnectedDeviceAddress)) {
                    Log.i(TAG, "Found last connected device: " + lastConnectedDeviceName);
                    return device;
                }
            }
        }

        // Otherwise, select first available device
        // TODO: Implement custom device selection logic based on device name patterns
        return pairedDevices.iterator().next();
    }

    /**
     * Connect to a specific Bluetooth device
     */
    public void connectToDevice(BluetoothDevice device) {
        if (device == null) {
            notifyError(BluetoothError.DEVICE_NOT_FOUND, "Device is null");
            return;
        }

        btWorkerExecutor.execute(() -> {
            try {
                if (ActivityCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    notifyError(BluetoothError.PERMISSION_DENIED, "BLUETOOTH_CONNECT permission not granted");
                    return;
                }

                Log.i(TAG, "Attempting to connect to device: " + device.getName() + " [" + device.getAddress() + "]");
                notifyConnectionState(ConnectionState.CONNECTING);

                // Close existing connection if any
                closeConnection();

                // Create socket
                bluetoothSocket = device.createRfcommSocketToServiceRecord(BT_MODULE_UUID);

                if (bluetoothSocket == null) {
                    notifyError(BluetoothError.CONNECTION_FAILED, "Failed to create socket");
                    return;
                }

                // Cancel discovery to improve connection speed
                if (bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                }

                // Attempt connection
                bluetoothSocket.connect();

                // Connection successful
                lastConnectedDeviceName = device.getName();
                lastConnectedDeviceAddress = device.getAddress();
                reconnectAttempts = 0;
                isConnected.set(true);

                Log.i(TAG, "Successfully connected to " + device.getName());
                notifyConnectionState(ConnectionState.CONNECTED);

                // Start message listener thread
                startMessageListener();

            } catch (IOException e) {
                Log.e(TAG, "Connection failed: " + e.getMessage(), e);
                isConnected.set(false);
                notifyError(BluetoothError.CONNECTION_FAILED, "Connection failed: " + e.getMessage());
                notifyConnectionState(ConnectionState.FAILED);

                // Attempt reconnection
                scheduleReconnect();
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error during connection", e);
                isConnected.set(false);
                notifyError(BluetoothError.UNKNOWN, "Unexpected error: " + e.getMessage());
            }
        });
    }

    /**
     * Start message listener thread
     */
    private void startMessageListener() {
        if (bluetoothSocket == null || !bluetoothSocket.isConnected()) {
            Log.w(TAG, "Cannot start message listener: socket not connected");
            return;
        }

        try {
            // Create and start connected thread
            connectedThread = new ConnectedThreadOptimized(bluetoothSocket, new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(android.os.Message msg) {
                    if (msg.what == MESSAGE_READ) {
                        byte[] buffer = (byte[]) msg.obj;
                        int bytes = msg.arg1;
                        byte[] data = new byte[bytes];
                        System.arraycopy(buffer, 0, data, 0, bytes);
                        listener.onMessageReceived(data);
                    }
                }
            });

            Log.i(TAG, "Message listener started");

        } catch (Exception e) {
            Log.e(TAG, "Failed to start message listener", e);
            notifyError(BluetoothError.UNKNOWN, "Failed to start message listener: " + e.getMessage());
        }
    }

    /**
     * Send a message to the connected device
     */
    public void sendMessage(String message) {
        if (message == null || message.isEmpty()) {
            Log.w(TAG, "Cannot send empty message");
            return;
        }

        if (!isConnected.get()) {
            Log.w(TAG, "Cannot send message: not connected");
            notifyError(BluetoothError.SEND_MESSAGE_FAILED, "Not connected to device");
            return;
        }

        if (connectedThread == null) {
            Log.w(TAG, "Cannot send message: connected thread is null");
            notifyError(BluetoothError.SEND_MESSAGE_FAILED, "Message thread not initialized");
            return;
        }

        btWorkerExecutor.execute(() -> {
            try {
                connectedThread.write(message);
                Log.d(TAG, "Message sent: " + message);
            } catch (Exception e) {
                Log.e(TAG, "Failed to send message", e);
                notifyError(BluetoothError.SEND_MESSAGE_FAILED, "Send failed: " + e.getMessage());
            }
        });
    }

    /**
     * Schedule reconnection attempt
     */
    private void scheduleReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Log.w(TAG, "Max reconnection attempts reached");
            notifyConnectionState(ConnectionState.FAILED);
            return;
        }

        if (isReconnecting.getAndSet(true)) {
            Log.w(TAG, "Reconnection already in progress");
            return;
        }

        reconnectAttempts++;
        Log.i(TAG, "Scheduling reconnection attempt " + reconnectAttempts + "/" + MAX_RECONNECT_ATTEMPTS);
        notifyConnectionState(ConnectionState.RECONNECTING);

        mainHandler.postDelayed(() -> {
            isReconnecting.set(false);
            if (lastConnectedDeviceAddress != null) {
                startDeviceSearch();
            } else {
                Log.w(TAG, "No device to reconnect to");
            }
        }, RECONNECT_DELAY_MS);
    }

    /**
     * Close Bluetooth connection
     */
    private void closeConnection() {
        try {
            if (connectedThread != null) {
                connectedThread.cancel();
                connectedThread = null;
            }

            if (bluetoothSocket != null) {
                bluetoothSocket.close();
                bluetoothSocket = null;
            }

            isConnected.set(false);
            Log.i(TAG, "Bluetooth connection closed");

        } catch (IOException e) {
            Log.e(TAG, "Error closing Bluetooth connection", e);
        }
    }

    /**
     * Check if Bluetooth permissions are granted
     */
    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ActivityCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * Request required permissions
     */
    private void requestPermissions() {
        List<String> requiredPermissions = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requiredPermissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            requiredPermissions.add(Manifest.permission.BLUETOOTH_SCAN);
        } else {
            requiredPermissions.add(Manifest.permission.BLUETOOTH);
            requiredPermissions.add(Manifest.permission.BLUETOOTH_ADMIN);
        }

        listener.onPermissionsRequired(requiredPermissions.toArray(new String[0]));
    }

    /**
     * Notify connection state change on main thread
     */
    private void notifyConnectionState(ConnectionState state) {
        mainHandler.post(() -> listener.onConnectionStateChanged(state));
    }

    /**
     * Notify error on main thread
     */
    private void notifyError(BluetoothError error, String message) {
        mainHandler.post(() -> listener.onError(error, message));
    }

    /**
     * Ensure manager is initialized
     */
    private boolean ensureInitialized() {
        if (!isInitialized.get()) {
            Log.w(TAG, "BluetoothManager not initialized");
            notifyError(BluetoothError.UNKNOWN, "Manager not initialized");
            return false;
        }
        return true;
    }

    /**
     * Ensure permissions are granted
     */
    private boolean ensurePermissions() {
        if (!permissionsGranted) {
            Log.w(TAG, "Bluetooth permissions not granted");
            requestPermissions();
            return false;
        }
        return true;
    }

    /**
     * Get current connection state
     */
    public boolean isConnected() {
        return isConnected.get();
    }

    /**
     * Get last connected device name
     */
    public String getLastConnectedDeviceName() {
        return lastConnectedDeviceName;
    }

    /**
     * Get last connected device address
     */
    public String getLastConnectedDeviceAddress() {
        return lastConnectedDeviceAddress;
    }

    /**
     * Cleanup resources
     * MUST be called in Activity.onDestroy() to prevent memory leaks
     */
    public void cleanup() {
        Log.i(TAG, "Cleaning up BluetoothManager...");

        // Cancel any pending reconnections
        mainHandler.removeCallbacksAndMessages(null);

        // Close connection
        closeConnection();

        // Shutdown thread pool
        if (!btWorkerExecutor.isShutdown()) {
            btWorkerExecutor.shutdown();
            try {
                if (!btWorkerExecutor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                    btWorkerExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                btWorkerExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Clear references
        bluetoothAdapter = null;
        pairedDevices = null;

        // Reset state
        isConnected.set(false);
        isReconnecting.set(false);
        isInitialized.set(false);

        Log.i(TAG, "BluetoothManager cleanup complete");
    }
}
