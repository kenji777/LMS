package itf.com.app.lms.util;

import android.util.Log;

/**
 * USB 폴링 필요 조건 체크 클래스
 * 
 * 폴링이 필요한 조건들을 체크하여 불필요한 폴링을 방지
 */
public class UsbPollingConditionChecker {
    private static final String TAG = "UsbPollingConditionChecker";
    
    // 조건 체크 인터페이스
    public interface ConditionProvider {
        /**
         * USB 서비스가 연결되어 있는지 확인
         */
        boolean isUsbServiceConnected();
        
        /**
         * USB 권한이 획득되었는지 확인
         */
        boolean isUsbPermissionGranted();
        
        /**
         * USB 장치가 연결되어 있는지 확인
         */
        boolean isUsbDeviceConnected();
        
        /**
         * 테스트가 실행 중인지 확인
         */
        boolean isTestRunning();
        
        /**
         * 제어 모드가 활성화되어 있는지 확인
         */
        boolean isControlModeActive();
        
        /**
         * 앱이 포그라운드에 있는지 확인
         */
        boolean isAppInForeground();
    }
    
    private ConditionProvider conditionProvider;
    
    public UsbPollingConditionChecker(ConditionProvider provider) {
        this.conditionProvider = provider;
    }
    
    /**
     * 폴링이 필요한지 확인
     * 
     * 필수 조건:
     * 1. USB 서비스 연결됨
     * 2. USB 권한 획득됨
     * 3. USB 장치 연결됨
     * 
     * 선택 조건 (하나라도 만족):
     * 1. 테스트 실행 중
     * 2. 제어 모드 활성화
     * 
     * 추가 조건:
     * 1. 앱이 포그라운드에 있음
     */
    public boolean isPollingNeeded() {
        if (conditionProvider == null) {
            Log.w(TAG, "Condition provider is null; assuming polling is not needed");
            return false;
        }
        
        // 필수 조건 체크
        if (!conditionProvider.isUsbServiceConnected()) {
            Log.d(TAG, "USB service not connected; polling not needed");
            return false;
        }
        
        if (!conditionProvider.isUsbPermissionGranted()) {
            Log.d(TAG, "USB permission not granted; polling not needed");
            return false;
        }
        
        if (!conditionProvider.isUsbDeviceConnected()) {
            Log.d(TAG, "USB device not connected; polling not needed");
            return false;
        }
        
        // 선택 조건 체크 (하나라도 만족하면 OK)
        boolean testRunning = conditionProvider.isTestRunning();
        boolean controlModeActive = conditionProvider.isControlModeActive();
        
        if (!testRunning && !controlModeActive) {
            Log.d(TAG, "Test not running and control mode not active; polling not needed");
            return false;
        }
        
        // 추가 조건: 앱이 포그라운드에 있어야 함
        if (!conditionProvider.isAppInForeground()) {
            Log.d(TAG, "App not in foreground; polling not needed");
            return false;
        }
        
        // 모든 조건 만족
        return true;
    }
    
    /**
     * 조건 상세 정보 반환 (디버깅용)
     */
    public String getConditionDetails() {
        if (conditionProvider == null) {
            return "Condition provider is null";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("USB Polling Conditions:\n");
        sb.append("  - USB Service Connected: ").append(conditionProvider.isUsbServiceConnected()).append("\n");
        sb.append("  - USB Permission Granted: ").append(conditionProvider.isUsbPermissionGranted()).append("\n");
        sb.append("  - USB Device Connected: ").append(conditionProvider.isUsbDeviceConnected()).append("\n");
        sb.append("  - Test Running: ").append(conditionProvider.isTestRunning()).append("\n");
        sb.append("  - Control Mode Active: ").append(conditionProvider.isControlModeActive()).append("\n");
        sb.append("  - App In Foreground: ").append(conditionProvider.isAppInForeground()).append("\n");
        sb.append("  - Polling Needed: ").append(isPollingNeeded());
        
        return sb.toString();
    }
}

