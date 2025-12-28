package itf.com.lms.util;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * 권한 체크 및 요청을 처리하는 유틸리티 클래스
 */
public class PermissionChecker {
    private static final String TAG = "PermissionChecker";
    private static final int MULTIPLE_PERMISSIONS = 1801;

    /**
     * 권한 요청 결과 콜백 인터페이스
     */
    public interface PermissionCallback {
        /**
         * 권한이 허용되었을 때 호출
         */
        void onPermissionGranted();

        /**
         * 권한이 거부되었을 때 호출
         */
        void onPermissionDenied();
    }

    // 권한 상수 정의
    private static final String[] MODERN_BT_PERMISSIONS = {
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
    };
    private static final String[] LEGACY_BT_PERMISSIONS = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
    };
    private static final String[] MODERN_MEDIA_PERMISSIONS = {
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
    };
    private static final String[] LEGACY_STORAGE_PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    // 권한 체크 관련 변수
    private boolean permissionRequestInProgress = false;
    private boolean permissionCheckCompleted = false;
    private boolean settingsDialogShown = false;
    private Activity activity;
    private PermissionCallback permissionCallback = null;

    public PermissionChecker(Activity activity) {
        this.activity = activity;
    }

    /**
     * 필요한 권한 목록 반환
     */
    public String[] getRequiredPermissions() {
        // Android 버전에 따라 블루투스 권한과 미디어/스토리지 권한을 결합
        String[] btPermissions = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ? MODERN_BT_PERMISSIONS
                : LEGACY_BT_PERMISSIONS;

        String[] mediaPermissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 (API 33) 이상: 미디어 권한
            mediaPermissions = MODERN_MEDIA_PERMISSIONS;
        } else {
            // Android 12 이하: 스토리지 권한
            mediaPermissions = LEGACY_STORAGE_PERMISSIONS;
        }

        // 두 배열을 결합
        String[] allPermissions = new String[btPermissions.length + mediaPermissions.length];
        System.arraycopy(btPermissions, 0, allPermissions, 0, btPermissions.length);
        System.arraycopy(mediaPermissions, 0, allPermissions, btPermissions.length, mediaPermissions.length);

        return allPermissions;
    }

    /**
     * 모든 필요한 권한이 허용되었는지 확인
     */
    public boolean hasAllRequiredPermissions() {
        for (String permission : getRequiredPermissions()) {
            if (ActivityCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * 권한 체크 및 요청
     */
    public void checkAndRequestPermissions() {
        // Android 6.0 미만에서는 런타임 권한이 없으므로 자동으로 허용된 것으로 처리
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Log.i(TAG, "▶ [PS] Android version < 6.0, permissions granted at install time");
            permissionCheckCompleted = true;
            if (permissionCallback != null) {
                permissionCallback.onPermissionGranted();
            }
            return;
        }

        if (activity == null) {
            Log.w(TAG, "▶ [PS] Activity is null, skipping permission request");
            return;
        }

        // Activity 상태 확인
        if (activity.isFinishing() || activity.isDestroyed()) {
            Log.w(TAG, "▶ [PS] Activity is finishing or destroyed, skipping permission request");
            return;
        }

        if (hasAllRequiredPermissions()) {
            Log.i(TAG, "▶ [PS] All permissions already granted");
            permissionCheckCompleted = true;
            return;
        }

        if (permissionRequestInProgress) {
            Log.i(TAG, "▶ [PS] Permission request already in progress");
            return;
        }

        List<String> missingPermissions = new ArrayList<>();
        List<String> permanentlyDeniedPermissions = new ArrayList<>();
        String[] requiredPermissions = getRequiredPermissions();

        for (String permission : requiredPermissions) {
            if (ActivityCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
                Log.d(TAG, "▶ [PS] Missing permission: " + permission);

                // 영구적으로 거부되었는지 확인
                // ACCESS_BACKGROUND_LOCATION은 특별한 권한으로 항상 false를 반환할 수 있음
                if (permission.equals(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                    // BACKGROUND_LOCATION은 별도 처리
                    permanentlyDeniedPermissions.add(permission);
                    Log.w(TAG, "▶ [PS] Background location permission requires special handling");
                } else if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                    permanentlyDeniedPermissions.add(permission);
                    Log.w(TAG, "▶ [PS] Permission permanently denied: " + permission);
                }
            }
        }

        if (missingPermissions.isEmpty()) {
            Log.i(TAG, "▶ [PS] All permissions granted");
            permissionCheckCompleted = true;
            return;
        }

        // 영구적으로 거부된 권한이 대부분이면 설정 화면으로 안내 (한 번만)
        // 단, 권한이 이미 모두 있으면 다이얼로그를 표시하지 않음
        if (hasAllRequiredPermissions()) {
            Log.i(TAG, "▶ [PS] All permissions already granted, skipping settings dialog");
            permissionCheckCompleted = true;
            return;
        }
        
        int permanentlyDeniedCount = permanentlyDeniedPermissions.size();
        if (permanentlyDeniedCount > 0 && permanentlyDeniedCount >= missingPermissions.size() - 1) {
            // 대부분의 권한이 영구적으로 거부된 경우
            if (!settingsDialogShown) {
                Log.w(TAG, "▶ [PS] Most permissions are permanently denied, showing settings dialog");
                settingsDialogShown = true;
                permissionCheckCompleted = true; // 더 이상 체크하지 않음
                showPermissionSettingsDialog();
            }
            return;
        }

        // Window 포커스 확인
        Window window = activity.getWindow();
        if (window == null || !activity.hasWindowFocus()) {
            Log.w(TAG, "▶ [PS] Window not ready, skipping permission request");
            permissionCheckCompleted = true; // Window가 준비되지 않으면 체크 완료로 표시
            return;
        }

        // 권한 요청 (영구적으로 거부된 권한 및 BACKGROUND_LOCATION 제외)
        List<String> requestablePermissions = new ArrayList<>();
        for (String permission : missingPermissions) {
            if (!permanentlyDeniedPermissions.contains(permission) &&
                    !permission.equals(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                requestablePermissions.add(permission);
            }
        }

        if (requestablePermissions.isEmpty()) {
            // 권한이 이미 모두 있으면 다이얼로그를 표시하지 않음
            if (hasAllRequiredPermissions()) {
                Log.i(TAG, "▶ [PS] All permissions already granted, skipping settings dialog");
                permissionCheckCompleted = true;
                return;
            }
            
            Log.w(TAG, "▶ [PS] No requestable permissions, showing settings dialog");
            if (!settingsDialogShown) {
                settingsDialogShown = true;
                permissionCheckCompleted = true;
                showPermissionSettingsDialog();
            }
            return;
        }

        // 권한 요청
        permissionRequestInProgress = true;
        Log.i(TAG, "▶ [PS] Requesting permissions: " + requestablePermissions.size() + " (total missing: " + missingPermissions.size() + ")");
        try {
            // 메인 스레드에서 실행되도록 보장
            activity.runOnUiThread(() -> {
                try {
                    ActivityCompat.requestPermissions(
                            activity,
                            requestablePermissions.toArray(new String[0]),
                            MULTIPLE_PERMISSIONS
                    );
                    Log.i(TAG, "▶ [PS] Permission request dialog should be displayed");
                } catch (Exception e) {
                    permissionRequestInProgress = false;
                    permissionCheckCompleted = true;
                    Log.e(TAG, "▶ [ER] Failed to request permissions in UI thread: " + e.getMessage(), e);
                }
            });
        } catch (Exception e) {
            permissionRequestInProgress = false;
            permissionCheckCompleted = true;
            Log.e(TAG, "▶ [ER] Failed to request permissions: " + e.getMessage(), e);
        }
    }

    /**
     * 권한 설정 화면으로 안내하는 다이얼로그 표시 (메인 스레드에서 실행)
     */
    private void showPermissionSettingsDialog() {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        // 메인 스레드에서 실행되도록 보장
        activity.runOnUiThread(() -> {
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                return;
            }

            try {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle("권한 필요");
                builder.setMessage("앱을 정상적으로 사용하려면 권한이 필요합니다.\n설정 화면에서 권한을 허용해주세요.");
                builder.setPositiveButton("설정으로 이동", (dialog, which) -> {
                    try {
                        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(android.net.Uri.parse("package:" + activity.getPackageName()));
                        activity.startActivity(intent);
                    } catch (Exception e) {
                        Log.e(TAG, "▶ [ER] Failed to open settings: " + e.getMessage(), e);
                    }
                    dialog.dismiss();
                });
                builder.setNegativeButton("취소", (dialog, which) -> {
                    dialog.dismiss();
                });
                builder.setCancelable(false);
                builder.show();
            } catch (Exception e) {
                Log.e(TAG, "▶ [ER] Failed to show permission settings dialog: " + e.getMessage(), e);
            }
        });
    }

    /**
     * 권한 요청 결과 처리
     */
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i(TAG, "▶ [PS] onRequestPermissionsResult called: requestCode=" + requestCode + ", permissions=" + permissions.length);

        if (requestCode == MULTIPLE_PERMISSIONS) {
            permissionRequestInProgress = false;
            permissionCheckCompleted = true; // 권한 요청 결과를 받았으므로 체크 완료

            boolean allGranted = grantResults.length > 0;
            for (int i = 0; i < grantResults.length; i++) {
                int grantResult = grantResults[i];
                String permission = i < permissions.length ? permissions[i] : "unknown";
                Log.d(TAG, "▶ [PS] Permission result: " + permission + " = " +
                        (grantResult == PackageManager.PERMISSION_GRANTED ? "GRANTED" : "DENIED"));
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                }
            }

            if (allGranted && hasAllRequiredPermissions()) {
                Log.i(TAG, "▶ [PS] All permissions granted by user");
                // 콜백 호출
                if (permissionCallback != null) {
                    permissionCallback.onPermissionGranted();
                }
            } else {
                Log.w(TAG, "▶ [PS] Some permissions were denied");
                // 권한이 거부된 경우 더 이상 반복 체크하지 않음
                // 콜백 호출
                if (permissionCallback != null) {
                    permissionCallback.onPermissionDenied();
                }
            }
        } else {

            Log.w(TAG, "▶ [PS] Unknown request code: " + requestCode);
        }
    }

    /**
     * 권한 체크 및 요청 (콜백 포함)
     * @param callback 권한 요청 결과를 받을 콜백
     * @return 권한이 이미 모두 허용되어 있으면 true, 그렇지 않으면 false
     */
    public boolean ensurePermissions(PermissionCallback callback) {
        this.permissionCallback = callback;
        // Android 6.0 미만에서는 런타임 권한이 없으므로 자동으로 허용된 것으로 처리
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Log.i(TAG, "▶ [PS] Android version < 6.0, permissions granted at install time");
            permissionCheckCompleted = true;
            if (callback != null) {
                callback.onPermissionGranted();
            }
            return true;
        }

        if (activity == null) {
            Log.w(TAG, "▶ [PS] Activity is null, skipping permission request");
            if (callback != null) {
                callback.onPermissionDenied();
            }
            return false;
        }

        // Activity 상태 확인
        if (activity.isFinishing() || activity.isDestroyed()) {
            Log.w(TAG, "▶ [PS] Activity is finishing or destroyed, skipping permission request");
            if (callback != null) {
                callback.onPermissionDenied();
            }
            return false;
        }

        if (hasAllRequiredPermissions()) {
            Log.i(TAG, "▶ [PS] All permissions already granted");
            permissionCheckCompleted = true;
            if (callback != null) {
                callback.onPermissionGranted();
            }
            return true;
        }

        if (permissionRequestInProgress) {
            Log.i(TAG, "▶ [PS] Permission request already in progress");
            return false;
        }

        // 권한 요청 시작
        checkAndRequestPermissions();
        return false;
    }

    /**
     * 권한 체크 완료 여부 반환
     */
    public boolean isPermissionCheckCompleted() {
        return permissionCheckCompleted;
    }

    /**
     * 권한 체크 완료 플래그 리셋 (필요한 경우)
     */
    public void resetPermissionCheck() {
        permissionCheckCompleted = false;
        settingsDialogShown = false;
        permissionRequestInProgress = false;
    }

    /**
     * 권한 체크 완료 상태로 설정 (권한이 이미 있는 경우 사용)
     */
    public void setPermissionCheckCompleted() {
        permissionCheckCompleted = true;
    }

    /**
     * 권한 요청 진행 중 여부 반환
     */
    public boolean isPermissionRequestInProgress() {
        return permissionRequestInProgress;
    }
}

