package itf.com.app.lms.kiosk;

import android.content.Intent;
import android.os.Bundle;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.HapticFeedbackConstants;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import itf.com.app.lms.R;
import itf.com.app.lms.util.AppSettings;
import itf.com.app.lms.util.StringResourceManager;

/**
 * 모든 Activity가 상속받아 키오스크 모드를 자동으로 적용하는 Base Activity
 * Application 클래스의 ActivityLifecycleCallbacks와 함께 작동
 */
public class BaseKioskActivity extends AppCompatActivity {
    
    private static final String TAG = "BaseKioskActivity";
    private static volatile boolean noVibratorWarned = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 액티비티 진입 애니메이션 제거
        overridePendingTransition(0, 0);
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        installHapticFeedbackHooks();
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        installHapticFeedbackHooks();
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        installHapticFeedbackHooks();
    }
    
    @SuppressWarnings("MissingSuperCall")
    @Override
    public void onBackPressed() {
        // ⚠️ 키오스크 모드: 뒤로가기 버튼 완전히 비활성화
        // 아무 동작도 하지 않음 (키오스크 모드에서는 앱 종료 방지)
        // Note: super.onBackPressed() intentionally NOT called to prevent navigation in kiosk mode
        Log.d(TAG, "Back button pressed - ignored in kiosk mode");
        // return; // 아무것도 하지 않음
    }
    
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // ⚠️ 키오스크 모드: 포커스를 받을 때마다 시스템 UI 숨기기
        if (hasFocus) {
            hideSystemUI();
        }
    }
    
    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        // ⚠️ 키오스크 모드: 사용자 상호작용 시에도 시스템 UI 숨기기
        // 터치 이벤트가 발생할 때마다 시스템 UI가 나타나지 않도록 함
        hideSystemUI();
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // ⚠️ 키오스크 모드: 터치 이벤트 발생 시 즉시 시스템 UI 숨기기
        // 상단 스와이프로 시스템 UI가 나타나는 것을 방지
        if (event.getAction() == MotionEvent.ACTION_DOWN || 
            event.getAction() == MotionEvent.ACTION_MOVE) {
            hideSystemUI();
        }
        return super.onTouchEvent(event);
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        // ⚠️ 키오스크 모드: Activity 시작 시 Window 플래그 추가 설정
        setupWindowFlags();
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
        // 액티비티 전환 애니메이션 제거 (start)
        overridePendingTransition(0, 0);
    }

    @Override
    public void startActivity(Intent intent, Bundle options) {
        super.startActivity(intent, options);
        // 액티비티 전환 애니메이션 제거 (start)
        overridePendingTransition(0, 0);
    }

    @Override
    public void finish() {
        super.finish();
        // 액티비티 종료 애니메이션 제거 (finish)
        overridePendingTransition(0, 0);
    }

    /**
     * 버튼/클릭 가능한 뷰 터치 시 진동(햅틱) 발생하도록 공통 훅 설치
     * - 기존 onClick 로직은 건드리지 않음 (OnTouchListener에서 false 반환)
     */
    private void installHapticFeedbackHooks() {
        try {
            View root = getWindow() != null ? getWindow().getDecorView() : null;
            if (root == null) return;
            enableHapticsRecursively(root);
        } catch (Exception e) {
            // 조용히 실패
        }
    }

    private void enableHapticsRecursively(View v) {
        if (v == null) return;

        // 이미 설치된 경우 스킵
        Object installed = v.getTag(R.id.tag_haptic_installed);
        if (installed instanceof Boolean && (Boolean) installed) {
            // continue traversal for children below (still safe)
        } else {
            // 클릭 가능한 뷰에만 적용
            if (v.isClickable() || v.isLongClickable()) {
                v.setHapticFeedbackEnabled(true);
                v.setTag(R.id.tag_haptic_installed, true);

                v.setOnTouchListener((view, event) -> {
                    try {
                        if (event != null && event.getAction() == MotionEvent.ACTION_DOWN) {
                            int constant = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1
                                    ? HapticFeedbackConstants.KEYBOARD_TAP
                                    : HapticFeedbackConstants.VIRTUAL_KEY;
                            view.performHapticFeedback(constant);
                            triggerClickVibration("touch");
                        }
                    } catch (Exception ignored) {
                        // ignore
                    }
                    // return false so the original onClick continues to work
                    return false;
                });
            }
        }

        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                enableHapticsRecursively(vg.getChildAt(i));
            }
        }
    }

    /**
     * 시스템 "터치 피드백" 설정과 무관하게 짧게 진동 (VIBRATE permission 필요)
     */
    /**
     * 클릭 진동 트리거 (테스트/디버깅을 위해 boolean 리턴)
     * @return true if vibration was triggered, false if vibrator unavailable
     */
    protected boolean triggerClickVibration(String source) {
        try {
            final long durationMs = 20L;

            Vibrator vibrator;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vm = (VibratorManager) getSystemService(VIBRATOR_MANAGER_SERVICE);
                vibrator = (vm != null) ? vm.getDefaultVibrator() : null;
            } else {
                vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            }

            if (vibrator == null || !vibrator.hasVibrator()) {
                Log.w(TAG, "No vibrator available (source=" + source + ")");
                return false;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Integer amp = AppSettings.getVibrationAmplitude();
                int amplitude = (amp != null) ? amp : VibrationEffect.DEFAULT_AMPLITUDE;
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude));
            } else {
                //noinspection deprecation
                vibrator.vibrate(durationMs);
            }
            return true;
        } catch (Exception ignored) {
            // ignore
            Log.w(TAG, "Failed to vibrate (source=" + source + ")");
            return false;
        }
    }

    /**
     * 기기에 진동 모터가 없는 경우 1회만 사용자에게 안내
     */
    protected void warnIfNoVibratorOnce() {
        if (noVibratorWarned) return;
        noVibratorWarned = true;
        try {
            android.widget.Toast.makeText(this, "이 기기에는 진동 모터가 없어서 진동이 동작하지 않습니다.", android.widget.Toast.LENGTH_LONG).show();
        } catch (Exception ignored) {
        }
    }
    
    /**
     * Window 플래그 추가 설정으로 시스템 UI 표시 완전 차단
     */
    private void setupWindowFlags() {
        try {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            
            WindowManager.LayoutParams params = getWindow().getAttributes();
            // 시스템 UI 표시를 방지하는 추가 플래그
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            
            // Android 11+ 에서는 추가 설정
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                getWindow().setDecorFitsSystemWindows(false);
            }
        } catch (Exception e) {
            // 조용히 실패
        }
    }
    
    /**
     * 시스템 UI 강제로 숨기기
     * 모든 Android 버전에서 작동하는 강력한 방법
     * ⚠️ 중요: protected로 선언하여 자식 클래스에서도 호출 가능하도록 함
     * ⚠️ 중요: 상단 스와이프로 시스템 UI가 나타나지 않도록 BEHAVIOR_DEFAULT 설정
     */
    protected void hideSystemUI() {
        try {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            
            View decorView = getWindow().getDecorView();
            
            // Android 11 (API 30) 이상
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsetsController controller = getWindow().getInsetsController();
                if (controller != null) {
                    // 상태바와 내비게이션 바 즉시 숨기기
                    controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                    // ⚠️ 중요: BEHAVIOR_DEFAULT로 설정하여 스와이프로 나타나지 않도록 함
                    // 매번 호출할 때마다 재설정하여 확실하게 차단
                    controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_DEFAULT);
                }
            } else {
                // Android 10 이하
                int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                decorView.setSystemUiVisibility(uiOptions);
            }
        } catch (Exception e) {
            // 조용히 실패 (너무 자주 호출되므로 로그는 남기지 않음)
        }
    }
    
    // ==================== StringResourceManager 헬퍼 메서드 ====================
    
    /**
     * 문자열 리소스 조회 (편의 메서드)
     * @param key 문자열 키
     * @return 번역된 문자열
     */
    protected String getStringResource(String key) {
        StringResourceManager manager = StringResourceManager.getInstance();
        if (getApplicationContext() != null) {
            manager.initialize(getApplicationContext());
        }
        return manager.getString(key);
    }
    
    /**
     * 문자열 리소스 조회 (파라미터 포함)
     * @param key 문자열 키
     * @param args 파라미터
     * @return 번역된 문자열
     */
    protected String getStringResource(String key, Object... args) {
        StringResourceManager manager = StringResourceManager.getInstance();
        if (getApplicationContext() != null) {
            manager.initialize(getApplicationContext());
        }
        return manager.getString(key, args);
    }
}

