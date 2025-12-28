package itf.com.app.simple_line_test_ovio_new.conn.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import itf.com.app.simple_line_test_ovio_new.util.Constants;

/**
 * NetworkManager - Handles all HTTP/Network communication operations
 *
 * Responsibilities:
 * - HTTP request execution (GET/POST)
 * - JSON parsing and data transformation
 * - Test spec data fetching
 * - Barcode/Serial number validation
 * - Test result upload
 * - Error handling and retry logic
 *
 * IMPORTANT: Replaces deprecated AsyncTask pattern with modern ExecutorService
 *
 * Usage:
 * <pre>
 * NetworkManager networkManager = new NetworkManager(context, listener);
 * networkManager.initialize();
 * networkManager.fetchTestSpec(modelId);
 * // ... use manager
 * networkManager.cleanup(); // MUST call in onDestroy()
 * </pre>
 */
public class NetworkManager {

    private static final String TAG = "NetworkManager";

    // Network configuration
    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 15000;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 2000;

    // Context reference
    private final Context appContext;

    // Listener for callbacks
    private final NetworkListener listener;

    // Thread management
    private final ExecutorService networkExecutor;
    private final Handler mainHandler;

    // State management
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);

    // Server configuration (configurable)
    private String primaryServerIp = "172.16.1.249:8080";
    private String secondaryServerIp = "172.16.1.250:8080";
    private String ddnsServer = "itfactoryddns.iptime.org:10004";
    private String currentServerIp;

    public String mode_type = Constants.InitialValues.MODE_TYPE;
    public String serverIp = "";

    /**
     * Listener interface for network events
     */
    public interface NetworkListener {
        /**
         * Called when test spec data is successfully fetched
         */
        void onTestSpecReceived(List<Map<String, String>> specData);

        /**
         * Called when barcode info is successfully fetched
         */
        void onBarcodeInfoReceived(String serialNo, Map<String, String> productInfo);

        /**
         * Called when test results are successfully uploaded
         */
        void onUploadComplete(boolean success, String message);

        /**
         * Called when version info is received
         */
        void onVersionInfoReceived(String version);

        /**
         * Called when an error occurs
         */
        void onError(NetworkError error, String message);

        /**
         * Called to report progress (optional)
         */
        void onProgress(int progress, String message);
    }

    /**
     * Network error types
     */
    public enum NetworkError {
        CONNECTION_FAILED,
        TIMEOUT,
        PARSE_ERROR,
        SERVER_ERROR,
        INVALID_RESPONSE,
        NO_INTERNET,
        UNKNOWN
    }

    /**
     * Request types
     */
    public enum RequestType {
        TEST_SPEC,
        BARCODE_INFO,
        UPLOAD_RESULTS,
        VERSION_INFO,
        TASK_UPDATE
    }

    /**
     * Constructor
     *
     * @param context Application context (NOT Activity)
     * @param listener Callback listener
     */
    public NetworkManager(Context context, NetworkListener listener) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }

        this.appContext = context.getApplicationContext();
        this.listener = listener;
        this.mainHandler = new Handler(Looper.getMainLooper());

        // Create thread pool for network operations
        this.networkExecutor = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r);
            thread.setName("Network-" + thread.getId());
            thread.setPriority(Thread.NORM_PRIORITY);
            return thread;
        });

        if (mode_type.equals(Constants.ResultStatus.MODE_TYPE_TEST)) {
            serverIp = Constants.ServerConfig.SERVER_IP_PORT_ITF;
        } else {
            serverIp = Constants.ServerConfig.SERVER_IP_PORT;
        }

        Log.i(TAG, "NetworkManager.serverIp: " + serverIp);
        this.currentServerIp = serverIp;
    }

    /**
     * Initialize network manager
     */
    public void initialize() {
        if (isInitialized.getAndSet(true)) {
            Log.w(TAG, "NetworkManager already initialized");
            return;
        }

        Log.i(TAG, "NetworkManager initialized successfully");
        Log.i(TAG, "Primary server: " + primaryServerIp);
        Log.i(TAG, "Secondary server: " + secondaryServerIp);
    }

    /**
     * Set server IP addresses
     */
    public void setServerIpAddresses(String primary, String secondary, String ddns) {
        if (primary != null && !primary.isEmpty()) {
            this.primaryServerIp = primary;
            this.currentServerIp = primary;
        }
        if (secondary != null && !secondary.isEmpty()) {
            this.secondaryServerIp = secondary;
        }
        if (ddns != null && !ddns.isEmpty()) {
            this.ddnsServer = ddns;
        }
        Log.i(TAG, "Server IPs updated - Primary: " + primaryServerIp + ", Secondary: " + secondaryServerIp);
    }

    /**
     * Fetch test specification data from server
     *
     * @param modelId Model ID to fetch specs for
     */
    public void fetchTestSpec(String modelId) {
        if (!ensureInitialized()) {
            return;
        }

        networkExecutor.execute(() -> {
            int attempts = 0;
            boolean success = false;

            while (attempts < MAX_RETRY_ATTEMPTS && !success) {
                attempts++;

                try {
                    String urlStr = "http://" + currentServerIp + "/OVIO/TestInfoList.jsp";
                    Log.i(TAG, "fetchTestSpec.urlStr: " + urlStr);

                    if (modelId != null && !modelId.isEmpty()) {
                        urlStr += "?clm_model_id=" + URLEncoder.encode(modelId, "UTF-8");
                    }

                    Log.i(TAG, "Fetching test spec from: " + urlStr + " (attempt " + attempts + "/" + MAX_RETRY_ATTEMPTS + ")");
                    notifyProgress(10, "Connecting to server...");

                    // Create connection
                    URL url = new URL(urlStr);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
                    connection.setReadTimeout(READ_TIMEOUT_MS);
                    connection.setRequestProperty("Accept", "application/json");

                    int responseCode = connection.getResponseCode();
                    Log.i(TAG, "Response code: " + responseCode);

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        notifyProgress(50, "Receiving data...");

                        // Read response
                        StringBuilder response = new StringBuilder();
                        try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)
                        )) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                response.append(line);
                            }
                        }
                        connection.disconnect();

                        String responseBody = response.toString();
                        Log.d(TAG, "Response body (first 500 chars): " + (responseBody.length() > 500 ? responseBody.substring(0, 500) : responseBody));

                        notifyProgress(75, "Parsing data...");

                        // Parse JSON
                        List<Map<String, String>> specData = parseTestSpecJson(responseBody);

                        if (specData != null && !specData.isEmpty()) {
                            Log.i(TAG, "Successfully fetched " + specData.size() + " test spec items");
                            notifyProgress(100, "Complete");
                            notifyTestSpecReceived(specData);
                            success = true;
                        } else {
                            throw new Exception("Empty or invalid response");
                        }
                    } else {
                        connection.disconnect();
                        throw new Exception("HTTP error: " + responseCode);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Attempt " + attempts + " failed: " + e.getMessage(), e);

                    if (attempts >= MAX_RETRY_ATTEMPTS) {
                        notifyError(NetworkError.CONNECTION_FAILED,
                            "Failed to fetch test spec after " + MAX_RETRY_ATTEMPTS + " attempts: " + e.getMessage());
                    } else {
                        // Try failover server
                        if (currentServerIp.equals(primaryServerIp)) {
                            currentServerIp = secondaryServerIp;
                            Log.i(TAG, "Failing over to secondary server: " + currentServerIp);
                        }

                        // Wait before retry
                        try {
                            Thread.sleep(RETRY_DELAY_MS);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
        });
    }

    /**
     * Fetch barcode/product information
     *
     * @param serialNo Serial number to look up
     */
    public void fetchBarcodeInfo(String serialNo) {
        if (!ensureInitialized()) {
            return;
        }

        if (serialNo == null || serialNo.trim().isEmpty()) {
            notifyError(NetworkError.INVALID_RESPONSE, "Serial number cannot be empty");
            return;
        }

        networkExecutor.execute(() -> {
            try {
                String urlStr = "http://" + currentServerIp + "/OVIO/ProductSerialInfoList.jsp?serial_no="
                    + URLEncoder.encode(serialNo.trim(), "UTF-8");

                Log.i(TAG, "Fetching barcode info: " + urlStr);

                URL url = new URL(urlStr);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
                connection.setReadTimeout(READ_TIMEOUT_MS);

                int responseCode = connection.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    StringBuilder response = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)
                    )) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                    }
                    connection.disconnect();

                    // Parse barcode JSON
                    Map<String, String> productInfo = parseBarcodeJson(response.toString());

                    if (productInfo != null && !productInfo.isEmpty()) {
                        Log.i(TAG, "Barcode info received for: " + serialNo);
                        notifyBarcodeInfoReceived(serialNo, productInfo);
                    } else {
                        notifyError(NetworkError.INVALID_RESPONSE, "No product info found for serial: " + serialNo);
                    }

                } else {
                    connection.disconnect();
                    notifyError(NetworkError.SERVER_ERROR, "HTTP error: " + responseCode);
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to fetch barcode info", e);
                notifyError(NetworkError.CONNECTION_FAILED, "Barcode fetch failed: " + e.getMessage());
            }
        });
    }

    /**
     * Upload test results to server
     *
     * @param results Map containing test result data
     */
    public void uploadTestResults(Map<String, String> results) {
        if (!ensureInitialized()) {
            return;
        }

        if (results == null || results.isEmpty()) {
            notifyError(NetworkError.INVALID_RESPONSE, "Results cannot be empty");
            return;
        }

        networkExecutor.execute(() -> {
            try {
                // Build URL with parameters
                StringBuilder urlBuilder = new StringBuilder();
                urlBuilder.append("http://").append(currentServerIp).append("/OVIO/UpdateResultTestInfo.jsp?");

                boolean first = true;
                for (Map.Entry<String, String> entry : results.entrySet()) {
                    if (!first) {
                        urlBuilder.append("&");
                    }
                    urlBuilder.append(URLEncoder.encode(entry.getKey(), "UTF-8"))
                               .append("=")
                               .append(URLEncoder.encode(entry.getValue(), "UTF-8"));
                    first = false;
                }

                String urlStr = urlBuilder.toString();
                Log.i(TAG, "Uploading results to: " + urlStr);

                URL url = new URL(urlStr);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET"); // Using GET as per original implementation
                connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
                connection.setReadTimeout(READ_TIMEOUT_MS);

                int responseCode = connection.getResponseCode();
                connection.disconnect();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.i(TAG, "Test results uploaded successfully");
                    notifyUploadComplete(true, "Results uploaded successfully");
                } else {
                    Log.w(TAG, "Upload response code: " + responseCode);
                    notifyUploadComplete(false, "Server returned code: " + responseCode);
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to upload test results", e);
                notifyError(NetworkError.CONNECTION_FAILED, "Upload failed: " + e.getMessage());
                notifyUploadComplete(false, e.getMessage());
            }
        });
    }

    /**
     * Fetch version information from server
     */
    public void fetchVersionInfo() {
        if (!ensureInitialized()) {
            return;
        }

        networkExecutor.execute(() -> {
            try {
                String urlStr = "http://" + currentServerIp + "/OVIO/VersionInfo.jsp";
                Log.i(TAG, "Fetching version info: " + urlStr);

                URL url = new URL(urlStr);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
                connection.setReadTimeout(READ_TIMEOUT_MS);

                int responseCode = connection.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    StringBuilder response = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)
                    )) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                    }
                    connection.disconnect();

                    String version = parseVersionJson(response.toString());
                    if (version != null && !version.isEmpty()) {
                        Log.i(TAG, "Version info received: " + version);
                        notifyVersionInfoReceived(version);
                    } else {
                        notifyError(NetworkError.PARSE_ERROR, "Invalid version response");
                    }

                } else {
                    connection.disconnect();
                    notifyError(NetworkError.SERVER_ERROR, "HTTP error: " + responseCode);
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to fetch version info", e);
                notifyError(NetworkError.CONNECTION_FAILED, "Version fetch failed: " + e.getMessage());
            }
        });
    }

    /**
     * Parse test specification JSON response
     */
    private List<Map<String, String>> parseTestSpecJson(String jsonString) {
        List<Map<String, String>> result = new ArrayList<>();

        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            
            // Try "test_spec" first (as used in ActivityModel), then fallback to "data"
            JSONArray dataArray = jsonObject.optJSONArray(Constants.JsonKeys.TEST_SPEC);
            if (dataArray == null || dataArray.length() == 0) {
                dataArray = jsonObject.optJSONArray("data");
            }

            if (dataArray == null || dataArray.length() == 0) {
                Log.w(TAG, "No data array in JSON response. Available keys: " + jsonObject.keys());
                Log.w(TAG, "JSON structure: " + jsonObject.toString());
                return result;
            }

            for (int i = 0; i < dataArray.length() - 1; i++) {
                JSONObject item = dataArray.getJSONObject(i);
                Map<String, String> map = new HashMap<>();

                // Extract all fields from JSON using Constants.JsonKeys for consistency
                // Try both Constants.JsonKeys field names and alternative names
                map.put(Constants.JsonKeys.CLM_TEST_SEQ, item.optString(Constants.JsonKeys.CLM_TEST_SEQ, item.optString("test_item_seq", String.valueOf(i))));
                map.put(Constants.JsonKeys.CLM_TEST_COMMAND, item.optString(Constants.JsonKeys.CLM_TEST_COMMAND, item.optString("test_item_command", "")));
                map.put(Constants.JsonKeys.CLM_TEST_NAME, item.optString(Constants.JsonKeys.CLM_TEST_NAME, item.optString("test_item_name", "")));
                map.put(Constants.JsonKeys.CLM_TEST_TYPE, item.optString(Constants.JsonKeys.CLM_TEST_TYPE, item.optString("test_item_type", "")));
                map.put(Constants.JsonKeys.CLM_TEST_LOWER_VALUE, item.optString(Constants.JsonKeys.CLM_TEST_LOWER_VALUE, item.optString("test_item_lower_value", "")));
                map.put(Constants.JsonKeys.CLM_TEST_UPPER_VALUE, item.optString(Constants.JsonKeys.CLM_TEST_UPPER_VALUE, item.optString("test_item_upper_value", "")));
                map.put(Constants.JsonKeys.CLM_TEST_SEC, item.optString(Constants.JsonKeys.CLM_TEST_SEC, item.optString("test_item_count", "")));
                map.put(Constants.JsonKeys.CLM_MODEL_ID, item.optString(Constants.JsonKeys.CLM_MODEL_ID, item.optString("model_id", "")));
                map.put(Constants.JsonKeys.CLM_MODEL_NAME, item.optString(Constants.JsonKeys.CLM_MODEL_NAME, item.optString("model_name", "")));
                
                // Additional fields that might be in the response
                map.put(Constants.JsonKeys.CLM_LOWER_VALUE, item.optString(Constants.JsonKeys.CLM_LOWER_VALUE, ""));
                map.put(Constants.JsonKeys.CLM_UPPER_VALUE, item.optString(Constants.JsonKeys.CLM_UPPER_VALUE, ""));
                map.put(Constants.JsonKeys.CLM_TEST_RESPONSE_VALUE, item.optString(Constants.JsonKeys.CLM_TEST_RESPONSE_VALUE, ""));
                map.put(Constants.JsonKeys.CLM_TEST_VERSION_ID, item.optString(Constants.JsonKeys.CLM_TEST_VERSION_ID, ""));
                map.put(Constants.JsonKeys.CLM_MODEL_NATION, item.optString(Constants.JsonKeys.CLM_MODEL_NATION, ""));
                map.put(Constants.JsonKeys.CLM_MODEL_VERSION, item.optString(Constants.JsonKeys.CLM_MODEL_VERSION, ""));

                result.add(map);
            }

            Log.i(TAG, "Parsed " + result.size() + " test spec items");

        } catch (JSONException e) {
            Log.e(TAG, "JSON parsing error", e);
            notifyError(NetworkError.PARSE_ERROR, "Failed to parse JSON: " + e.getMessage());
        }

        return result;
    }

    /**
     * Parse barcode JSON response
     */
    private Map<String, String> parseBarcodeJson(String jsonString) {
        Map<String, String> result = new HashMap<>();

        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            JSONArray dataArray = jsonObject.optJSONArray("data");

            if (dataArray != null && dataArray.length() > 0) {
                JSONObject item = dataArray.getJSONObject(0);

                result.put("serial_no", item.optString("serial_no", ""));
                result.put("model_id", item.optString("model_id", ""));
                result.put("model_name", item.optString("model_name", ""));
                result.put("product_date", item.optString("product_date", ""));
                result.put("line_no", item.optString("line_no", ""));

                Log.i(TAG, "Parsed barcode info for: " + result.get("serial_no"));
            }

        } catch (JSONException e) {
            Log.e(TAG, "Barcode JSON parsing error", e);
            notifyError(NetworkError.PARSE_ERROR, "Failed to parse barcode JSON: " + e.getMessage());
        }

        return result;
    }

    /**
     * Parse version JSON response
     */
    private String parseVersionJson(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            return jsonObject.optString("version", "");
        } catch (JSONException e) {
            Log.e(TAG, "Version JSON parsing error", e);
            return null;
        }
    }

    /**
     * Notify test spec received on main thread
     */
    private void notifyTestSpecReceived(List<Map<String, String>> specData) {
        mainHandler.post(() -> listener.onTestSpecReceived(specData));
    }

    /**
     * Notify barcode info received on main thread
     */
    private void notifyBarcodeInfoReceived(String serialNo, Map<String, String> productInfo) {
        mainHandler.post(() -> listener.onBarcodeInfoReceived(serialNo, productInfo));
    }

    /**
     * Notify upload complete on main thread
     */
    private void notifyUploadComplete(boolean success, String message) {
        mainHandler.post(() -> listener.onUploadComplete(success, message));
    }

    /**
     * Notify version info received on main thread
     */
    private void notifyVersionInfoReceived(String version) {
        mainHandler.post(() -> listener.onVersionInfoReceived(version));
    }

    /**
     * Notify error on main thread
     */
    private void notifyError(NetworkError error, String message) {
        mainHandler.post(() -> listener.onError(error, message));
    }

    /**
     * Notify progress on main thread
     */
    private void notifyProgress(int progress, String message) {
        mainHandler.post(() -> listener.onProgress(progress, message));
    }

    /**
     * Ensure manager is initialized
     */
    private boolean ensureInitialized() {
        if (!isInitialized.get()) {
            Log.w(TAG, "NetworkManager not initialized");
            notifyError(NetworkError.UNKNOWN, "Manager not initialized");
            return false;
        }
        return true;
    }

    /**
     * Get current server IP
     */
    public String getCurrentServerIp() {
        return currentServerIp;
    }

    /**
     * Switch to backup server
     */
    public void switchToBackupServer() {
        if (currentServerIp.equals(primaryServerIp)) {
            currentServerIp = secondaryServerIp;
            Log.i(TAG, "Switched to secondary server: " + currentServerIp);
        } else {
            currentServerIp = primaryServerIp;
            Log.i(TAG, "Switched to primary server: " + currentServerIp);
        }
    }

    /**
     * Cleanup resources
     * MUST be called in Activity.onDestroy()
     */
    public void cleanup() {
        Log.i(TAG, "Cleaning up NetworkManager...");

        // Remove all pending handler callbacks
        mainHandler.removeCallbacksAndMessages(null);

        // Shutdown executor
        if (!networkExecutor.isShutdown()) {
            networkExecutor.shutdown();
            try {
                if (!networkExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    networkExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                networkExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Reset state
        isInitialized.set(false);

        Log.i(TAG, "NetworkManager cleanup complete");
    }
}
