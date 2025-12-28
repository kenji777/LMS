/*
 * INTEGRATION PATCH FOR ActivityModel_0002.java
 *
 * This file contains the code snippets to ADD to your existing ActivityModel_0002.java
 *
 * HOW TO USE:
 * 1. Open your original ActivityModel_0002.java in Android Studio
 * 2. Follow the instructions below for each section
 * 3. Copy-paste the code at the specified locations
 * 4. Add the import statements at the top
 * 5. Compile and test
 *
 * IMPORTANT: Keep your original file as backup!
 */

// ============================================================================
// STEP 1: ADD IMPORTS (at the top of the file, after package declaration)
// ============================================================================

import itf.com.app.simple_line_test_ovio_new.bluetooth.BluetoothManager;
import itf.com.app.simple_line_test_ovio_new.usb.UsbManager;
import itf.com.app.simple_line_test_ovio_new.network.NetworkManager;

// ============================================================================
// STEP 2: ADD MANAGER FIELDS (around line 205, BEFORE static BluetoothAdapter)
// ============================================================================

    // ========== PHASE 1: Communication Managers (NEW) ==========
    private BluetoothManager bluetoothManager;
    private UsbManager usbManager;
    private NetworkManager networkManager;
    // ========== End Manager Fields ==========

// ============================================================================
// STEP 3: ADD TO END OF onCreate() METHOD (around line 1000-1100)
// Find: } at the end of onCreate() and add BEFORE that closing brace
// ============================================================================

        // ========== PHASE 1: Initialize Communication Managers ==========
        initializeManagers();
        // ========== End Manager Initialization ==========

// ============================================================================
// STEP 4: ADD THESE NEW METHODS AFTER onCreate() (around line 1100)
// ============================================================================

    // ========== PHASE 1: Manager Initialization Methods (NEW) ==========

    /**
     * Initialize all communication managers
     */
    private void initializeManagers() {
        Log.i(TAG, "Initializing communication managers...");

        try {
            initializeBluetoothManager();
            initializeUsbManager();
            initializeNetworkManager();

            Log.i(TAG, "All managers initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing managers", e);
        }
    }

    /**
     * Initialize Bluetooth Manager
     */
    private void initializeBluetoothManager() {
        bluetoothManager = new BluetoothManager(
            getApplicationContext(),
            new BluetoothManager.BluetoothListener() {
                @Override
                public void onMessageReceived(byte[] data) {
                    // Delegate to existing processBtMessage logic
                    processBtMessage(data);
                }

                @Override
                public void onConnectionStateChanged(BluetoothManager.ConnectionState state) {
                    runOnUiThread(() -> {
                        try {
                            switch (state) {
                                case CONNECTED:
                                    tvConnectBtRamp.setBackgroundColor(getResources().getColor(R.color.blue_01));
                                    tvConnectBtRamp.setText("BT: Connected");
                                    btConnected = true;
                                    break;
                                case CONNECTING:
                                    tvConnectBtRamp.setBackgroundColor(getResources().getColor(R.color.yellow_01));
                                    tvConnectBtRamp.setText("BT: Connecting...");
                                    btConnected = false;
                                    break;
                                case RECONNECTING:
                                    tvConnectBtRamp.setBackgroundColor(getResources().getColor(R.color.orange_01));
                                    tvConnectBtRamp.setText("BT: Reconnecting...");
                                    btConnected = false;
                                    break;
                                case DISCONNECTED:
                                case FAILED:
                                    tvConnectBtRamp.setBackgroundColor(getResources().getColor(R.color.red_01));
                                    tvConnectBtRamp.setText("BT: Disconnected");
                                    btConnected = false;
                                    break;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating BT UI", e);
                        }
                    });
                }

                @Override
                public void onError(BluetoothManager.BluetoothError error, String message) {
                    Log.e(TAG, "Bluetooth error: " + error + " - " + message);
                    runOnUiThread(() -> {
                        try {
                            clAlert.setVisibility(View.VISIBLE);
                            tvAlertMessage.setText("Bluetooth Error: " + message);
                        } catch (Exception e) {
                            Log.e(TAG, "Error showing BT error", e);
                        }
                    });
                }

                @Override
                public void onPermissionsRequired(String[] permissions) {
                    ActivityCompat.requestPermissions(
                        ActivityModel_0002.this,
                        permissions,
                        PERMISSION_REQUEST_CODE_BT
                    );
                }
            }
        );

        bluetoothManager.initialize();
        Log.i(TAG, "BluetoothManager initialized");
    }

    /**
     * Initialize USB Manager
     */
    private void initializeUsbManager() {
        usbManager = new UsbManager(
            getApplicationContext(),
            new UsbManager.UsbListener() {
                @Override
                public void onVoltageDataReceived(String data) {
                    if (data != null && !data.isEmpty()) {
                        try {
                            // Extract watt value from PLC response
                            // Format: "\u000215RSS0107%DW100638B4\u0003"
                            String wattStr = data.substring(data.indexOf("DW") + 6, data.indexOf("\u0003"));
                            int wattValue = Integer.parseInt(wattStr, 16);
                            decElectricValue = wattValue / 10.0;

                            runOnUiThread(() -> {
                                if (tvWattValue != null) {
                                    tvWattValue.setText(String.format("%.1fW", decElectricValue));
                                }
                            });
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing voltage data: " + data, e);
                        }
                    }
                }

                @Override
                public void onConnectionStateChanged(UsbManager.ConnectionState state) {
                    runOnUiThread(() -> {
                        try {
                            switch (state) {
                                case CONNECTED:
                                    tvConnectPlcRamp.setBackgroundColor(getResources().getColor(R.color.blue_01));
                                    tvConnectPlcRamp.setText("USB: Connected");
                                    break;
                                case CONNECTING:
                                    tvConnectPlcRamp.setBackgroundColor(getResources().getColor(R.color.yellow_01));
                                    tvConnectPlcRamp.setText("USB: Connecting...");
                                    break;
                                case RECONNECTING:
                                    tvConnectPlcRamp.setBackgroundColor(getResources().getColor(R.color.orange_01));
                                    tvConnectPlcRamp.setText("USB: Reconnecting...");
                                    break;
                                case DISCONNECTED:
                                    tvConnectPlcRamp.setBackgroundColor(getResources().getColor(R.color.red_01));
                                    tvConnectPlcRamp.setText("USB: Disconnected");
                                    break;
                                case PERMISSION_REQUIRED:
                                    tvConnectPlcRamp.setBackgroundColor(getResources().getColor(R.color.yellow_01));
                                    tvConnectPlcRamp.setText("USB: Permission Needed");
                                    break;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating USB UI", e);
                        }
                    });
                }

                @Override
                public void onError(UsbManager.UsbError error, String message) {
                    Log.e(TAG, "USB error: " + error + " - " + message);
                }

                @Override
                public void onPermissionRequired() {
                    runOnUiThread(() -> {
                        try {
                            tvAlertMessage.setText("USB permission required");
                            clAlert.setVisibility(View.VISIBLE);
                        } catch (Exception e) {
                            Log.e(TAG, "Error showing USB permission dialog", e);
                        }
                    });
                }
            }
        );

        usbManager.initialize();
        Log.i(TAG, "UsbManager initialized");
    }

    /**
     * Initialize Network Manager
     */
    private void initializeNetworkManager() {
        networkManager = new NetworkManager(
            getApplicationContext(),
            new NetworkManager.NetworkListener() {
                @Override
                public void onTestSpecReceived(List<Map<String, String>> specData) {
                    // Store in existing field for compatibility
                    lstData = specData;

                    // Process spec data (existing logic)
                    runOnUiThread(() -> {
                        try {
                            processTestSpecData(specData);
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing test spec data", e);
                        }
                    });
                }

                @Override
                public void onBarcodeInfoReceived(String serialNo, Map<String, String> productInfo) {
                    runOnUiThread(() -> {
                        try {
                            globalProductSerialNo = serialNo;
                            barcodeReadCheck = true;

                            // Extract product info
                            String modelId = productInfo.get("model_id");
                            String modelName = productInfo.get("model_name");

                            if (modelId != null) globalModelId = modelId;
                            if (modelName != null) globalModelName = modelName;

                            Log.i(TAG, "Barcode info received: " + serialNo + " -> " + modelName);
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing barcode info", e);
                        }
                    });
                }

                @Override
                public void onUploadComplete(boolean success, String message) {
                    Log.i(TAG, "Upload " + (success ? "succeeded" : "failed") + ": " + message);
                }

                @Override
                public void onVersionInfoReceived(String version) {
                    Log.i(TAG, "Version: " + version);
                }

                @Override
                public void onError(NetworkManager.NetworkError error, String message) {
                    Log.e(TAG, "Network error: " + error + " - " + message);
                    // Optionally show error to user
                }

                @Override
                public void onProgress(int progress, String message) {
                    Log.d(TAG, "Network progress: " + progress + "% - " + message);
                }
            }
        );

        networkManager.initialize();

        // Set server IPs from existing configuration
        if (serverIp != null && !serverIp.isEmpty()) {
            networkManager.setServerIpAddresses(serverIp, serverResetIp, serverDomain);
        }

        Log.i(TAG, "NetworkManager initialized");
    }

    /**
     * Start all communication managers
     */
    private void startManagers() {
        Log.i(TAG, "Starting communication managers...");

        // Start Bluetooth if enabled
        if (bluetoothManager != null && btSearchOnOff) {
            bluetoothManager.startDeviceSearch();
        }

        // Start USB connection
        if (usbManager != null) {
            usbManager.startDeviceConnection();
        }

        // Fetch test spec data
        if (networkManager != null && globalModelId != null) {
            networkManager.fetchTestSpec(globalModelId);
        }
    }

    /**
     * Cleanup all communication managers
     * CRITICAL: Must be called to prevent memory leaks!
     */
    private void cleanupManagers() {
        Log.i(TAG, "Cleaning up communication managers...");

        if (bluetoothManager != null) {
            try {
                bluetoothManager.cleanup();
                bluetoothManager = null;
                Log.i(TAG, "BluetoothManager cleaned up");
            } catch (Exception e) {
                Log.e(TAG, "Error cleaning up BluetoothManager", e);
            }
        }

        if (usbManager != null) {
            try {
                usbManager.cleanup();
                usbManager = null;
                Log.i(TAG, "UsbManager cleaned up");
            } catch (Exception e) {
                Log.e(TAG, "Error cleaning up UsbManager", e);
            }
        }

        if (networkManager != null) {
            try {
                networkManager.cleanup();
                networkManager = null;
                Log.i(TAG, "NetworkManager cleaned up");
            } catch (Exception e) {
                Log.e(TAG, "Error cleaning up NetworkManager", e);
            }
        }

        Log.i(TAG, "Manager cleanup complete");
    }

    // ========== End Manager Initialization Methods ==========

// ============================================================================
// STEP 5: ADD TO END OF onResume() METHOD (around line 3261)
// Find: } at the end of onResume() and add BEFORE that closing brace
// ============================================================================

        // ========== PHASE 1: Start Managers ==========
        startManagers();
        // ========== End Manager Startup ==========

// ============================================================================
// STEP 6: ADD AT BEGINNING OF onDestroy() METHOD (around line 794)
// Add this as the FIRST thing in onDestroy(), right after super.onDestroy()
// ============================================================================

        // ========== PHASE 1: Cleanup Managers (CRITICAL!) ==========
        cleanupManagers();
        // ========== End Manager Cleanup ==========

// ============================================================================
// STEP 7: OPTIONAL - Replace sendBtMessage() calls throughout the file
// Search for all "sendBtMessage(" and replace with the wrapped version below
// ============================================================================

    /**
     * Send Bluetooth message via manager (with fallback)
     */
    private void sendBtMessageViaManager(String message) {
        if (bluetoothManager != null && bluetoothManager.isConnected()) {
            bluetoothManager.sendMessage(message);
        } else {
            // Fallback to old method if manager not ready
            sendBtMessage(message);
        }
    }

// ============================================================================
// USAGE INSTRUCTIONS
// ============================================================================

/*
 * AFTER ADDING ALL THE ABOVE CODE:
 *
 * 1. Verify imports are added at the top
 * 2. Verify manager fields are added (Step 2)
 * 3. Verify initializeManagers() call is in onCreate()
 * 4. Verify all 4 new methods are added (Step 4)
 * 5. Verify startManagers() call is in onResume()
 * 6. Verify cleanupManagers() call is in onDestroy()
 *
 * COMPILE AND TEST:
 * - Build the project
 * - Fix any compilation errors
 * - Run on device/emulator
 * - Test Bluetooth connection
 * - Test USB connection
 * - Test network requests
 * - Check for memory leaks
 *
 * OPTIONAL CLEANUP (do later):
 * - Gradually replace sendBtMessage() calls with sendBtMessageViaManager()
 * - Eventually remove old Bluetooth/USB/Network code
 * - Remove static BluetoothAdapter and related fields
 */
