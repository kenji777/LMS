package itf.com.app.lms.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * ActivityModel_0002 및 ActivityModelList에서 사용되는 다이얼로그들을 중앙에서 관리하는 클래스
 * 각 다이얼로그별로 boolean 변수를 두고, 열린 상태에서는 다시 호출하지 않음
 */
public class DialogManager {
    private static final String TAG = "DialogManager";
    
    /**
     * 다이얼로그 타입 정의
     */
    public enum DialogType {
        // ActivityModel_0002에서 사용되는 다이얼로그
        PERMISSION,                    // 권한 요청 다이얼로그
        BLUETOOTH_ENABLE,              // 블루투스 활성화 다이얼로그
        NO_PAIRED_DEVICES,             // 페어링된 블루투스 장비 없음 다이얼로그
        
        // ActivityModelList에서 사용되는 다이얼로그
        APP_EXIT_CONFIRM,              // 앱 종료 확인 다이얼로그 (fab_close 버튼 클릭 시)
        
        // 필요시 추가 다이얼로그 타입을 여기에 추가
    }
    
    private final Activity activity;
    private final Map<DialogType, AlertDialog> activeDialogs = new HashMap<>();
    private final Map<DialogType, Boolean> dialogShowingFlags = new HashMap<>();
    
    /**
     * 생성자
     * @param activity 다이얼로그를 표시할 Activity
     */
    public DialogManager(@NonNull Activity activity) {
        this.activity = activity;
        // 모든 다이얼로그 타입의 플래그 초기화
        for (DialogType type : DialogType.values()) {
            dialogShowingFlags.put(type, false);
        }
    }
    
    /**
     * 다이얼로그 표시 (중복 체크 포함)
     * @param type 다이얼로그 타입
     * @param config 다이얼로그 설정 인터페이스
     * @return 다이얼로그가 표시되었으면 true, 이미 표시 중이거나 표시 실패 시 false
     */
    public boolean showDialog(@NonNull DialogType type, @NonNull DialogConfig config) {
        // 이미 표시 중이면 무시
        if (isDialogShowing(type)) {
            Log.d(TAG, "다이얼로그가 이미 표시 중입니다: " + type);
            return false;
        }
        
        // Activity가 유효한지 확인
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Log.w(TAG, "Activity가 유효하지 않습니다: " + type);
            return false;
        }
        
        // UI 스레드에서 다이얼로그 생성 및 표시
        activity.runOnUiThread(() -> {
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
            
            // 기존 다이얼로그가 있으면 정리
            dismissDialog(type);
            
            try {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                
                // 다이얼로그 설정 적용
                config.configure(builder);
                
                // 다이얼로그 생성
                AlertDialog dialog = builder.create();
                
                // 다이얼로그 닫힘 리스너 설정
                dialog.setOnDismissListener(d -> {
                    activeDialogs.remove(type);
                    dialogShowingFlags.put(type, false);
                });
                
                // 다이얼로그 취소 리스너 설정
                dialog.setOnCancelListener(d -> {
                    activeDialogs.remove(type);
                    dialogShowingFlags.put(type, false);
                });
                
                // 다이얼로그 저장 및 플래그 설정
                activeDialogs.put(type, dialog);
                dialogShowingFlags.put(type, true);
                
                // 다이얼로그 표시
                dialog.show();
                
                Log.d(TAG, "다이얼로그 표시: " + type);
            } catch (Exception e) {
                Log.e(TAG, "다이얼로그 표시 실패: " + type, e);
                activeDialogs.remove(type);
                dialogShowingFlags.put(type, false);
            }
        });
        
        return true;
    }
    
    /**
     * 특정 타입의 다이얼로그가 표시 중인지 확인
     * @param type 다이얼로그 타입
     * @return 표시 중이면 true
     */
    public boolean isDialogShowing(@NonNull DialogType type) {
        Boolean showing = dialogShowingFlags.get(type);
        if (showing == null || !showing) {
            return false;
        }
        
        // 실제 다이얼로그 객체도 확인
        AlertDialog dialog = activeDialogs.get(type);
        if (dialog != null && dialog.isShowing()) {
            return true;
        } else {
            // 다이얼로그가 닫혔지만 플래그가 남아있는 경우 정리
            if (showing) {
                dialogShowingFlags.put(type, false);
                activeDialogs.remove(type);
            }
            return false;
        }
    }
    
    /**
     * 특정 타입의 다이얼로그 닫기
     * @param type 다이얼로그 타입
     */
    public void dismissDialog(@NonNull DialogType type) {
        AlertDialog dialog = activeDialogs.get(type);
        if (dialog != null) {
            try {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
            } catch (Exception e) {
                Log.e(TAG, "다이얼로그 닫기 실패: " + type, e);
            } finally {
                activeDialogs.remove(type);
                dialogShowingFlags.put(type, false);
            }
        }
    }
    
    /**
     * 모든 다이얼로그 닫기
     */
    public void dismissAllDialogs() {
        for (DialogType type : DialogType.values()) {
            dismissDialog(type);
        }
    }
    
    /**
     * 특정 타입의 다이얼로그 강제로 닫고 플래그 리셋
     * @param type 다이얼로그 타입
     */
    public void resetDialog(@NonNull DialogType type) {
        dismissDialog(type);
        dialogShowingFlags.put(type, false);
        activeDialogs.remove(type);
    }
    
    /**
     * 모든 다이얼로그 상태 리셋
     */
    public void resetAllDialogs() {
        dismissAllDialogs();
        for (DialogType type : DialogType.values()) {
            dialogShowingFlags.put(type, false);
        }
        activeDialogs.clear();
    }
    
    /**
     * 다이얼로그 설정 인터페이스
     * 각 다이얼로그 타입별로 설정을 커스터마이징할 수 있음
     */
    public interface DialogConfig {
        /**
         * AlertDialog.Builder를 설정하는 메서드
         * @param builder AlertDialog.Builder
         */
        void configure(@NonNull AlertDialog.Builder builder);
    }
    
    /**
     * 간단한 다이얼로그 설정을 위한 빌더 클래스
     */
    public static class SimpleDialogConfig implements DialogConfig {
        private String title;
        private String message;
        private String positiveButtonText;
        private String negativeButtonText;
        private DialogInterface.OnClickListener positiveListener;
        private DialogInterface.OnClickListener negativeListener;
        private boolean cancelable = true;
        
        public SimpleDialogConfig setTitle(String title) {
            this.title = title;
            return this;
        }
        
        public SimpleDialogConfig setMessage(String message) {
            this.message = message;
            return this;
        }
        
        public SimpleDialogConfig setPositiveButton(String text, DialogInterface.OnClickListener listener) {
            this.positiveButtonText = text;
            this.positiveListener = listener;
            return this;
        }
        
        public SimpleDialogConfig setNegativeButton(String text, DialogInterface.OnClickListener listener) {
            this.negativeButtonText = text;
            this.negativeListener = listener;
            return this;
        }
        
        public SimpleDialogConfig setCancelable(boolean cancelable) {
            this.cancelable = cancelable;
            return this;
        }
        
        @Override
        public void configure(@NonNull AlertDialog.Builder builder) {
            if (title != null) {
                builder.setTitle(title);
            }
            if (message != null) {
                builder.setMessage(message);
            }
            if (positiveButtonText != null) {
                builder.setPositiveButton(positiveButtonText, positiveListener);
            }
            if (negativeButtonText != null) {
                builder.setNegativeButton(negativeButtonText, negativeListener);
            }
            builder.setCancelable(cancelable);
        }
    }
    
    // ============================================
    // ActivityModelList 전용 편의 메서드
    // ============================================
    
    /**
     * 앱 종료 확인 다이얼로그 표시 (ActivityModelList용)
     * @param onConfirm 종료 확인 시 실행할 Runnable
     * @param onCancel 취소 시 실행할 Runnable (null 가능)
     * @return 다이얼로그가 표시되었으면 true
     */
    public boolean showAppExitConfirmDialog(@NonNull Runnable onConfirm, @Nullable Runnable onCancel) {
        SimpleDialogConfig config = new SimpleDialogConfig()
                .setTitle("앱 종료")
                .setMessage("정말 테스트 유닛 어플리케이션을 종료하시겠습니까?")
                .setPositiveButton("종료", (dialog, which) -> {
                    dialog.dismiss();
                    if (onConfirm != null) {
                        onConfirm.run();
                    }
                })
                .setNegativeButton("취소", (dialog, which) -> {
                    dialog.dismiss();
                    if (onCancel != null) {
                        onCancel.run();
                    }
                })
                .setCancelable(true);
        
        return showDialog(DialogType.APP_EXIT_CONFIRM, config);
    }
    
    /**
     * 앱 종료 확인 다이얼로그가 표시 중인지 확인 (ActivityModelList용)
     * @return 표시 중이면 true
     */
    public boolean isAppExitConfirmDialogShowing() {
        return isDialogShowing(DialogType.APP_EXIT_CONFIRM);
    }
    
    /**
     * 앱 종료 확인 다이얼로그 닫기 (ActivityModelList용)
     */
    public void dismissAppExitConfirmDialog() {
        dismissDialog(DialogType.APP_EXIT_CONFIRM);
    }

    /**
     * ⚠️ CRITICAL FIX: Cleanup method to prevent memory leaks
     * MUST be called in Activity onDestroy()
     * 
     * This method dismisses all dialogs and clears references.
     * Note: Activity reference is final and cannot be nulled.
     * 
     * RECOMMENDATION: Consider using WeakReference<Activity> instead of 
     * final Activity reference to prevent memory leaks if DialogManager 
     * outlives the Activity.
     */
    public void cleanup() {
        dismissAllDialogs();
        activeDialogs.clear();
        // Note: activity is final and cannot be nulled
        // Best practice: Ensure DialogManager lifecycle matches Activity lifecycle
        // or use WeakReference<Activity>
    }
}
