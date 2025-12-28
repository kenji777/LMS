package itf.com.app.lms.managers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import itf.com.app.lms.util.LogManager;

import java.util.ArrayList;
import java.util.List;

/**
 * PermissionManager - Centralized permission management
 *
 * Responsibilities:
 * - Check and request runtime permissions
 * - Handle permission results
 * - Show permission prompts
 * - Open app settings for permanently denied permissions
 *
 * Usage:
 * <pre>
 * PermissionManager permissionManager = new PermissionManager(activity);
 * permissionManager.checkAndRequestPermissions(requiredPermissions, callback);
 * </pre>
 */
public class PermissionManager {

    private static final String TAG = "PermissionManager";

    public static final int PERMISSION_REQUEST_CODE = 1000;
    public static final int PERMISSION_REQUEST_CODE_BT = 1001;
    public static final int MULTIPLE_PERMISSIONS = 1801;

    private final Activity activity;
    private boolean permissionRequestInProgress = false;
    private int permissionDenialCount = 0;

    /**
     * Listener interface for permission events
     */
    public interface PermissionListener {
        /**
         * Called when all permissions are granted
         */
        void onAllPermissionsGranted();

        /**
         * Called when some permissions are denied
         */
        void onPermissionsDenied(String[] deniedPermissions, boolean permanentlyDenied);

        /**
         * Called when permission request is needed
         */
        void onPermissionRequestNeeded(String[] permissions);
    }

    private PermissionListener listener;

    /**
     * Constructor
     *
     * @param activity Activity instance for permission requests
     */
    public PermissionManager(Activity activity) {
        if (activity == null) {
            throw new IllegalArgumentException("Activity cannot be null");
        }
        this.activity = activity;
    }

    /**
     * Set permission listener
     */
    public void setPermissionListener(PermissionListener listener) {
        this.listener = listener;
    }

    /**
     * Check and request permissions asynchronously
     */
    public void checkAndRequestPermissionsAsync(List<String> requiredPermissions) {
        if (requiredPermissions == null || requiredPermissions.isEmpty()) {
            if (listener != null) {
                listener.onAllPermissionsGranted();
            }
            return;
        }

        // Check permissions in background
        new Thread(() -> {
            List<String> missingPermissions = new ArrayList<>();
            for (String permission : requiredPermissions) {
                if (ActivityCompat.checkSelfPermission(activity, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    missingPermissions.add(permission);
                }
            }

            final List<String> finalMissingPermissions = missingPermissions;
            final boolean allGranted = finalMissingPermissions.isEmpty();

            // Request permissions on main thread
            activity.runOnUiThread(() -> {
                if (allGranted) {
                    permissionDenialCount = 0;
                    if (listener != null) {
                        listener.onAllPermissionsGranted();
                    }
                } else {
                    requestRuntimePermissions(finalMissingPermissions);
                }
            });
        }).start();
    }

    /**
     * Request runtime permissions on main thread
     */
    public void requestRuntimePermissions(List<String> missingPermissions) {
        if (permissionRequestInProgress) {
            LogManager.i(LogManager.LogCategory.BT, TAG, "Permission request already in progress");
            return;
        }
        if (missingPermissions == null || missingPermissions.isEmpty()) {
            permissionDenialCount = 0;
            if (listener != null) {
                listener.onAllPermissionsGranted();
            }
            return;
        }

        permissionRequestInProgress = true;
        if (listener != null) {
            listener.onPermissionRequestNeeded(missingPermissions.toArray(new String[0]));
        }
        ActivityCompat.requestPermissions(
                activity,
                missingPermissions.toArray(new String[0]),
                MULTIPLE_PERMISSIONS
        );
    }

    /**
     * Handle permission request result
     * Call this from Activity.onRequestPermissionsResult()
     */
    public void handlePermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != MULTIPLE_PERMISSIONS &&
                requestCode != PERMISSION_REQUEST_CODE &&
                requestCode != PERMISSION_REQUEST_CODE_BT) {
            return;
        }

        permissionRequestInProgress = false;

        boolean allGranted = grantResults.length > 0;
        for (int grantResult : grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted && hasAllPermissions(permissions)) {
            permissionDenialCount = 0;
            if (listener != null) {
                listener.onAllPermissionsGranted();
            }
        } else {
            permissionDenialCount++;
            List<String> deniedPermissions = new ArrayList<>();
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    deniedPermissions.add(permissions[i]);
                }
            }

            boolean permanentlyDenied = false;
            for (String permission : deniedPermissions) {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                    permanentlyDenied = true;
                    break;
                }
            }

            if (listener != null) {
                listener.onPermissionsDenied(
                        deniedPermissions.toArray(new String[0]),
                        permanentlyDenied
                );
            }
        }
    }

    /**
     * Check if all permissions are granted
     */
    public boolean hasAllPermissions(String[] permissions) {
        if (permissions == null || permissions.length == 0) {
            return true;
        }
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(activity, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if a specific permission is granted
     */
    public boolean hasPermission(String permission) {
        return ActivityCompat.checkSelfPermission(activity, permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Show permission prompt
     */
    public void showPermissionPrompt(boolean permanentlyDenied) {
        // This can be customized based on your UI requirements
        if (permanentlyDenied) {
            openAppSettings();
        } else {
            // Show rationale dialog
            LogManager.w(LogManager.LogCategory.BT, TAG, "Permission denied. Please grant permissions.");
        }
    }

    /**
     * Open app settings
     */
    public void openAppSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
            intent.setData(uri);
            activity.startActivity(intent);
        } catch (Exception e) {
            LogManager.e(LogManager.LogCategory.ER, TAG, "Error opening app settings", e);
        }
    }

    /**
     * Get permission denial count
     */
    public int getPermissionDenialCount() {
        return permissionDenialCount;
    }

    /**
     * Reset permission denial count
     */
    public void resetPermissionDenialCount() {
        permissionDenialCount = 0;
    }

    /**
     * Check if permission request is in progress
     */
    public boolean isPermissionRequestInProgress() {
        return permissionRequestInProgress;
    }
}


