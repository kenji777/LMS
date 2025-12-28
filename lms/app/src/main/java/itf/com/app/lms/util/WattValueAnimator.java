package itf.com.app.lms.util;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

/**
 * 소비전력 값 애니메이션 유틸리티 클래스
 * 
 * TextView에 값을 부드럽게 증가/감소하여 표시하는 기능 제공
 * +1, -1씩 점진적으로 변경하여 자연스러운 효과 구현
 */
public class WattValueAnimator {
    private static final String TAG = "WattValueAnimator";
    
    // 애니메이션 속도 설정 (밀리초)
    private static final long ANIMATION_INTERVAL_MS = 20; // 20ms마다 업데이트 (약 50fps)
    private static final int ANIMATION_STEP = 1; // 한 번에 변경할 값 (+1 또는 -1)
    
    private final Handler mainHandler;
    private final TextView targetTextView;
    
    // 현재 표시 중인 값과 목표 값
    private int currentDisplayValue = 0;
    private int targetValue = 0;
    
    // 애니메이션 실행 중인지 여부
    private boolean isAnimating = false;
    private Runnable animationRunnable = null;
    
    /**
     * 생성자
     * @param targetTextView 애니메이션을 적용할 TextView
     */
    public WattValueAnimator(TextView targetTextView) {
        this.targetTextView = targetTextView;
        this.mainHandler = new Handler(Looper.getMainLooper());
        
        // 초기값 설정
        if (targetTextView != null) {
            try {
                String currentText = targetTextView.getText().toString().trim();
                // "Watt" 같은 단위 제거
                currentText = currentText.replace("Watt", "").trim();
                if (!currentText.isEmpty()) {
                    currentDisplayValue = Integer.parseInt(currentText);
                }
            } catch (NumberFormatException e) {
                currentDisplayValue = 0;
            }
            targetValue = currentDisplayValue;
        }
    }
    
    /**
     * 목표 값으로 애니메이션 시작
     * @param newValue 새로운 목표 값
     */
    public void animateToValue(int newValue) {
        if (targetTextView == null) {
            Log.w(TAG, "Target TextView is null, cannot animate");
            return;
        }
        
        // 목표 값이 현재 표시 값과 같으면 애니메이션 불필요
        if (newValue == currentDisplayValue) {
            return;
        }
        
        // 기존 애니메이션 중지
        stopAnimation();
        
        // 목표 값 설정
        targetValue = newValue;
        
        // 애니메이션 시작
        startAnimation();
    }
    
    /**
     * 애니메이션 시작
     */
    private void startAnimation() {
        if (isAnimating) {
            return;
        }
        
        isAnimating = true;
        
        animationRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAnimating || targetTextView == null) {
                    return;
                }
                
                // 목표 값에 도달했는지 확인
                if (currentDisplayValue == targetValue) {
                    // 애니메이션 완료
                    isAnimating = false;
                    updateTextView(targetValue);
                    return;
                }
                
                // 현재 값 업데이트 (+1 또는 -1)
                if (currentDisplayValue < targetValue) {
                    currentDisplayValue += ANIMATION_STEP;
                    // 목표 값을 초과하지 않도록 제한
                    if (currentDisplayValue > targetValue) {
                        currentDisplayValue = targetValue;
                    }
                } else {
                    currentDisplayValue -= ANIMATION_STEP;
                    // 목표 값보다 작아지지 않도록 제한
                    if (currentDisplayValue < targetValue) {
                        currentDisplayValue = targetValue;
                    }
                }
                
                // TextView 업데이트
                updateTextView(currentDisplayValue);
                
                // 다음 프레임 스케줄링
                if (isAnimating && currentDisplayValue != targetValue) {
                    mainHandler.postDelayed(this, ANIMATION_INTERVAL_MS);
                } else {
                    // 애니메이션 완료
                    isAnimating = false;
                }
            }
        };
        
        // 첫 번째 프레임 즉시 실행
        mainHandler.post(animationRunnable);
    }
    
    /**
     * 애니메이션 중지
     */
    public void stopAnimation() {
        isAnimating = false;
        if (animationRunnable != null) {
            mainHandler.removeCallbacks(animationRunnable);
            animationRunnable = null;
        }
    }
    
    /**
     * TextView 업데이트
     * @param value 표시할 값
     */
    private void updateTextView(int value) {
        if (targetTextView != null) {
            try {
                targetTextView.setText(String.valueOf(value));
            } catch (Exception e) {
                Log.e(TAG, "Error updating TextView", e);
            }
        }
    }
    
    /**
     * 현재 표시 중인 값 조회
     * @return 현재 표시 중인 값
     */
    public int getCurrentDisplayValue() {
        return currentDisplayValue;
    }
    
    /**
     * 목표 값 조회
     * @return 목표 값
     */
    public int getTargetValue() {
        return targetValue;
    }
    
    /**
     * 애니메이션 실행 중인지 여부
     * @return true if animating
     */
    public boolean isAnimating() {
        return isAnimating;
    }
    
    /**
     * 즉시 값 설정 (애니메이션 없이)
     * @param value 설정할 값
     */
    public void setValueImmediately(int value) {
        stopAnimation();
        currentDisplayValue = value;
        targetValue = value;
        updateTextView(value);
    }
    
    /**
     * 리소스 정리
     */
    public void cleanup() {
        stopAnimation();
    }
}

