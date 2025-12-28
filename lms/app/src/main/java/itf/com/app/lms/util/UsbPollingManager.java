package itf.com.app.lms.util;

import android.util.Log;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * USB 폴링 최적화 관리 클래스
 * 
 * 주요 기능:
 * 1. 적응형 폴링 간격 (지수 백오프, 최대 1초)
 * 2. 단일 타이머 보장 (동시성 제어)
 * 3. 성능 모니터링
 * 
 * 참고: 조건 기반 폴링 및 상태 기반 제어는 향후 추가 예정
 */
public class UsbPollingManager {
    private static final String TAG = "UsbPollingManager";
    
    // 폴링 간격 설정
    private static final long BASE_INTERVAL_MS = Constants.Timeouts.USB_TIMER_INTERVAL_MS; // 500ms
    private static final long MAX_INTERVAL_MS = 1000; // 1초 (최대 간격)
    private static final long MIN_INTERVAL_MS = 100; // 100ms
    private static final double BACKOFF_MULTIPLIER = 2.0; // 지수 백오프 배수
    
    // 실패 임계값
    private static final int FAILURE_THRESHOLD = 5; // 5회 실패 시 백오프
    private static final int SUCCESS_RESET_THRESHOLD = 3; // 3회 성공 시 간격 복구
    
    // Executor 및 Future
    private final ScheduledExecutorService executor;
    private final ReentrantLock pollingLock = new ReentrantLock();
    private ScheduledFuture<?> pollingFuture = null;
    
    // 상태 관리
    private final AtomicBoolean pollingEnabled = new AtomicBoolean(false);
    private final AtomicBoolean pollingRequested = new AtomicBoolean(false);
    
    // 적응형 간격 관리
    private final AtomicLong currentIntervalMs = new AtomicLong(BASE_INTERVAL_MS);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicInteger consecutiveSuccesses = new AtomicInteger(0);
    
    // 성능 통계
    private final AtomicLong totalPollingCount = new AtomicLong(0);
    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong failureCount = new AtomicLong(0);
    private final AtomicLong totalResponseTimeMs = new AtomicLong(0);
    
    // 폴링 실행자 인터페이스
    public interface PollingExecutor {
        /**
         * 폴링 명령 실행
         * @return true if polling command was sent successfully
         */
        boolean executePolling();
    }
    
    private PollingExecutor pollingExecutor;
    
    // 간격 변경 리스너 (선택사항)
    public interface IntervalChangeListener {
        void onIntervalChanged(long oldInterval, long newInterval);
    }
    
    private IntervalChangeListener intervalChangeListener;
    
    public UsbPollingManager(ScheduledExecutorService executor) {
        this.executor = executor;
    }
    
    /**
     * 폴링 실행자 설정
     */
    public void setPollingExecutor(PollingExecutor executor) {
        this.pollingExecutor = executor;
    }
    
    /**
     * 간격 변경 리스너 설정 (선택사항)
     */
    public void setIntervalChangeListener(IntervalChangeListener listener) {
        this.intervalChangeListener = listener;
    }
    
    /**
     * 폴링 시작
     * @param immediate 즉시 실행 여부
     */
    public void startPolling(boolean immediate) {
        pollingLock.lock();
        try {
            if (executor.isShutdown()) {
                Log.w(TAG, "Executor is shut down; cannot start polling");
                return;
            }
            
            // 이미 실행 중이면 중복 시작 방지 (단일 타이머 보장)
            if (pollingEnabled.get() && pollingFuture != null && !pollingFuture.isCancelled()) {
                Log.d(TAG, "Polling already running; skipping restart");
                return;
            }
            
            // 기존 타이머 정리
            stopPollingInternal();
            
            // 상태 설정
            pollingEnabled.set(true);
            pollingRequested.set(true);
            consecutiveFailures.set(0);
            consecutiveSuccesses.set(0);
            currentIntervalMs.set(BASE_INTERVAL_MS);
            
            // 폴링 시작
            long initialDelay = immediate ? 0 : currentIntervalMs.get();
            pollingFuture = executor.scheduleAtFixedRate(
                this::executePolling,
                initialDelay,
                currentIntervalMs.get(),
                TimeUnit.MILLISECONDS
            );
            
            Log.i(TAG, String.format("USB polling started (interval: %d ms, immediate: %s)", 
                currentIntervalMs.get(), immediate));
        } catch (Exception e) {
            Log.e(TAG, "Failed to start polling", e);
            pollingEnabled.set(false);
            pollingRequested.set(false);
        } finally {
            pollingLock.unlock();
        }
    }
    
    /**
     * 폴링 중지
     */
    public void stopPolling() {
        pollingLock.lock();
        try {
            stopPollingInternal();
        } finally {
            pollingLock.unlock();
        }
    }
    
    /**
     * 폴링 중지 (내부 메서드, lock 필요)
     */
    private void stopPollingInternal() {
        pollingEnabled.set(false);
        pollingRequested.set(false);
        
        if (pollingFuture != null) {
            pollingFuture.cancel(true);
            pollingFuture = null;
        }
        
        // 간격 초기화
        currentIntervalMs.set(BASE_INTERVAL_MS);
        consecutiveFailures.set(0);
        consecutiveSuccesses.set(0);
        
        Log.i(TAG, "USB polling stopped");
    }
    
    /**
     * 폴링 실행 (스케줄된 작업)
     */
    private void executePolling() {
        if (!pollingEnabled.get()) {
            stopPolling();
            return;
        }
        
        long startTime = System.currentTimeMillis();
        totalPollingCount.incrementAndGet();
        
        try {
            boolean success = false;
            if (pollingExecutor != null) {
                success = pollingExecutor.executePolling();
            }
            
            long responseTime = System.currentTimeMillis() - startTime;
            totalResponseTimeMs.addAndGet(responseTime);
            
            if (success) {
                handlePollingSuccess();
            } else {
                handlePollingFailure();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during polling execution", e);
            handlePollingFailure();
        }
    }
    
    /**
     * 폴링 성공 처리
     */
    private void handlePollingSuccess() {
        successCount.incrementAndGet();
        consecutiveSuccesses.incrementAndGet();
        consecutiveFailures.set(0);
        
        // 백오프 중이면 간격 복구
        long currentInterval = currentIntervalMs.get();
        if (currentInterval > BASE_INTERVAL_MS) {
            if (consecutiveSuccesses.get() >= SUCCESS_RESET_THRESHOLD) {
                // 간격 복구
                currentIntervalMs.set(BASE_INTERVAL_MS);
                consecutiveSuccesses.set(0);
                
                // 간격 변경 알림
                if (intervalChangeListener != null) {
                    intervalChangeListener.onIntervalChanged(currentInterval, BASE_INTERVAL_MS);
                }
                
                // 새로운 간격으로 재시작
                restartPollingWithNewInterval();
                
                Log.i(TAG, String.format("Polling recovered; interval reset to %d ms", BASE_INTERVAL_MS));
            }
        }
    }
    
    /**
     * 폴링 실패 처리
     */
    private void handlePollingFailure() {
        failureCount.incrementAndGet();
        consecutiveFailures.incrementAndGet();
        consecutiveSuccesses.set(0);
        
        // 실패 임계값 도달 시 백오프
        if (consecutiveFailures.get() >= FAILURE_THRESHOLD) {
            long oldInterval = currentIntervalMs.get();
            long newInterval = calculateBackoffInterval(oldInterval);
            
            if (newInterval > oldInterval) {
                currentIntervalMs.set(newInterval);
                
                // 간격 변경 알림
                if (intervalChangeListener != null) {
                    intervalChangeListener.onIntervalChanged(oldInterval, newInterval);
                }
                
                // 새로운 간격으로 재시작
                restartPollingWithNewInterval();
                
                Log.w(TAG, String.format("Polling failure threshold reached; backing off to %d ms", newInterval));
            }
        }
    }
    
    /**
     * 백오프 간격 계산 (지수 백오프, 최대 1초)
     */
    private long calculateBackoffInterval(long currentInterval) {
        long newInterval = (long) (currentInterval * BACKOFF_MULTIPLIER);
        // 최대 간격을 1초로 제한
        return Math.min(Math.max(newInterval, MIN_INTERVAL_MS), MAX_INTERVAL_MS);
    }
    
    /**
     * 새로운 간격으로 폴링 재시작
     */
    private void restartPollingWithNewInterval() {
        pollingLock.lock();
        try {
            if (!pollingEnabled.get()) {
                return;
            }
            
            // 기존 타이머 취소
            if (pollingFuture != null) {
                pollingFuture.cancel(true);
                pollingFuture = null;
            }
            
            // 새로운 간격으로 재시작
            pollingFuture = executor.scheduleAtFixedRate(
                this::executePolling,
                0,
                currentIntervalMs.get(),
                TimeUnit.MILLISECONDS
            );
            
            Log.d(TAG, String.format("Polling restarted with new interval: %d ms", currentIntervalMs.get()));
        } catch (Exception e) {
            Log.e(TAG, "Failed to restart polling with new interval", e);
        } finally {
            pollingLock.unlock();
        }
    }
    
    /**
     * 현재 간격 조회
     */
    public long getCurrentInterval() {
        return currentIntervalMs.get();
    }
    
    /**
     * 폴링 활성화 여부
     */
    public boolean isPollingActive() {
        return pollingEnabled.get() && pollingFuture != null && !pollingFuture.isCancelled();
    }
    
    /**
     * 성능 통계 조회
     */
    public PollingStats getStats() {
        long total = totalPollingCount.get();
        long success = successCount.get();
        long failure = failureCount.get();
        long avgResponseTime = total > 0 ? totalResponseTimeMs.get() / total : 0;
        double successRate = total > 0 ? (double) success / total * 100.0 : 0.0;
        
        return new PollingStats(
            total,
            success,
            failure,
            avgResponseTime,
            successRate,
            currentIntervalMs.get(),
            consecutiveFailures.get(),
            consecutiveSuccesses.get()
        );
    }
    
    /**
     * 통계 초기화
     */
    public void resetStats() {
        totalPollingCount.set(0);
        successCount.set(0);
        failureCount.set(0);
        totalResponseTimeMs.set(0);
    }
    
    /**
     * 리소스 정리
     */
    public void shutdown() {
        pollingLock.lock();
        try {
            stopPollingInternal();
            
            // ⚠️ CRITICAL FIX: Clear listener references to prevent memory leaks
            pollingExecutor = null;
            intervalChangeListener = null;
        } finally {
            pollingLock.unlock();
        }
    }
    
    /**
     * 성능 통계 클래스
     */
    public static class PollingStats {
        public final long totalCount;
        public final long successCount;
        public final long failureCount;
        public final long avgResponseTimeMs;
        public final double successRate;
        public final long currentIntervalMs;
        public final int consecutiveFailures;
        public final int consecutiveSuccesses;
        
        public PollingStats(long totalCount, long successCount, long failureCount,
                           long avgResponseTimeMs, double successRate,
                           long currentIntervalMs, int consecutiveFailures, int consecutiveSuccesses) {
            this.totalCount = totalCount;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.avgResponseTimeMs = avgResponseTimeMs;
            this.successRate = successRate;
            this.currentIntervalMs = currentIntervalMs;
            this.consecutiveFailures = consecutiveFailures;
            this.consecutiveSuccesses = consecutiveSuccesses;
        }
        
        @Override
        public String toString() {
            return String.format(
                "PollingStats{total=%d, success=%d, failure=%d, avgResponse=%dms, successRate=%.2f%%, interval=%dms, failures=%d, successes=%d}",
                totalCount, successCount, failureCount, avgResponseTimeMs, successRate, 
                currentIntervalMs, consecutiveFailures, consecutiveSuccesses
            );
        }
    }
}
