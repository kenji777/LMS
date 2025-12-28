package itf.com.app.lms.util;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 통합 로그 관리 유틸리티 클래스 (데이터베이스 저장 기능 포함)
 * 
 * 기능:
 * - 카테고리별 로그 관리 (PS, BT, US, SI, ER, CA, TH, BI, RS)
 * - 로그 레벨 관리 (VERBOSE, DEBUG, INFO, WARN, ERROR)
 * - 배치 처리 지원 (성능 최적화)
 * - 데이터베이스 저장 (모든 로그)
 * - 에러 로그 ID 자동 할당
 * - 통일된 로그 포맷
 * - Thread-safe 로그 출력
 */
public class LogManager {
    
    // ==================== 로그 카테고리 ====================
    
    /**
     * 로그 카테고리 정의
     */
    public enum LogCategory {
        PS("PS", "Process"),      // 프로세스 관련
        BT("BT", "Bluetooth"),    // 블루투스 관련
        US("US", "USB"),          // USB 관련
        SI("SI", "ServerInfo"),   // 서버 정보
        ER("ER", "Error"),        // 에러
        CA("CA", "Cache"),        // 캐시
        TH("TH", "Temperature"),  // 온도
        BI("BI", "BarcodeInfo"),  // 바코드 정보
        RS("RS", "Result");       // 결과
        
        private final String code;
        private final String name;
        
        LogCategory(String code, String name) {
            this.code = code;
            this.name = name;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getName() {
            return name;
        }
    }
    
    // ==================== 로그 레벨 ====================
    
    /**
     * 로그 레벨 정의
     */
    public enum LogLevel {
        VERBOSE(0, Constants.InitialValues.LOG_LEVEL_VERBOSE),
        DEBUG(1, Constants.InitialValues.LOG_LEVEL_DEBUG),
        INFO(2, Constants.InitialValues.LOG_LEVEL_INFO),
        WARN(3, Constants.InitialValues.LOG_LEVEL_WARN),
        ERROR(4, Constants.InitialValues.LOG_LEVEL_ERROR);
        
        private final int priority;
        private final String code;
        
        LogLevel(int priority, String code) {
            this.priority = priority;
            this.code = code;
        }
        
        public int getPriority() {
            return priority;
        }
        
        public String getCode() {
            return code;
        }
    }
    
    // ==================== 로그 엔트리 ====================
    
    /**
     * 로그 엔트리 클래스
     */
    private static class LogEntry {
        final LogLevel level;
        final LogCategory category;
        final String tag;
        final String message;
        final Throwable throwable;
        final long timestamp;
        String errorId; // 에러 ID 추가
        
        LogEntry(LogLevel level, LogCategory category, String tag, String message, Throwable throwable) {
            this.level = level;
            this.category = category;
            this.tag = tag;
            this.message = message;
            this.throwable = throwable;
            this.timestamp = System.currentTimeMillis();
            this.errorId = null;
        }
    }
    
    // ==================== 싱글톤 인스턴스 ====================
    
    private static volatile LogManager instance;
    private static final Object INSTANCE_LOCK = new Object();
    
    // ==================== 설정 ====================
    
    private LogLevel minLogLevel = LogLevel.DEBUG;
    private boolean enableBatching = true;
    private long batchIntervalMs = 100; // 배치 처리 간격 (밀리초)
    private boolean enabled = true;
    
    // ==================== 로그 저장 관련 설정 ====================
    
    private static final int MAX_LOG_QUEUE_SIZE = 1000;
    private static final long LOG_SAVE_INTERVAL_MS = 5000; // 5초마다 저장
    private static final int MAX_LOG_AGE_DAYS = 30; // 30일 이상 된 로그 삭제
    
    private boolean enableLogSaving = true;
    private Context applicationContext;
    private ExecutorService logSaveExecutor;
    
    // ==================== 핸들러 및 큐 ====================
    
    private final Handler mainHandler;
    private final ConcurrentLinkedQueue<LogEntry> logBatchQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<LogEntry> logSaveQueue = new ConcurrentLinkedQueue<>();
    private final Object LOG_BATCH_LOCK = new Object();
    private final Object LOG_SAVE_LOCK = new Object();
    private Runnable logBatchTask;
    private Runnable logSaveTask;
    
    // ==================== 생성자 ====================
    
    private LogManager() {
        // 메인 스레드 핸들러 초기화
        if (Looper.myLooper() == Looper.getMainLooper()) {
            this.mainHandler = new Handler(Looper.getMainLooper());
        } else {
            // 백그라운드 스레드에서 초기화되는 경우
            this.mainHandler = new Handler(Looper.getMainLooper());
        }
    }
    
    /**
     * LogManager 인스턴스 가져오기 (싱글톤)
     */
    public static LogManager getInstance() {
        if (instance == null) {
            synchronized (INSTANCE_LOCK) {
                if (instance == null) {
                    instance = new LogManager();
                }
            }
        }
        return instance;
    }
    
    // ==================== 설정 메서드 ====================
    
    /**
     * 최소 로그 레벨 설정
     */
    public void setMinLogLevel(LogLevel level) {
        this.minLogLevel = level;
    }
    
    /**
     * 배치 처리 활성화/비활성화
     */
    public void setBatchingEnabled(boolean enabled) {
        this.enableBatching = enabled;
    }
    
    /**
     * 배치 처리 간격 설정
     */
    public void setBatchInterval(long intervalMs) {
        this.batchIntervalMs = intervalMs;
    }
    
    /**
     * 로그 활성화/비활성화
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * 로그 저장 활성화/비활성화
     */
    public void setLogSavingEnabled(boolean enabled) {
        this.enableLogSaving = enabled;
    }
    
    /**
     * LogManager 초기화 (Context 설정 및 로그 저장 활성화)
     */
    public void initialize(Context context) {
        this.applicationContext = context.getApplicationContext();
        
        // 주기적으로 오래된 로그 정리 (하루에 한 번)
        scheduleLogCleanup();
    }
    
    // ==================== 핵심 로그 메서드 ====================
    
    /**
     * 통합 로그 메서드 - 배치 처리 지원
     */
    public void log(LogLevel level, LogCategory category, String tag, String message, Throwable throwable) {
        if (!enabled) {
            return;
        }
        
        // 로그 레벨 체크
        if (level.getPriority() < minLogLevel.getPriority()) {
            return;
        }
        
        // 로그 포맷 통일
        String formattedMessage = String.format("▶ [%s] %s", category.getCode(), message);
        
        // 로그 엔트리 생성
        LogEntry entry = new LogEntry(level, category, tag, formattedMessage, throwable);
        
        // 에러 로그인 경우 에러 ID 할당
        if (level == LogLevel.ERROR) {
            entry.errorId = ErrorIdMapper.inferErrorId(category, message, throwable);
        }
        
        // 데이터베이스 저장 큐에 추가
        saveLogToDatabase(entry);
        
        if (enableBatching && level.getPriority() >= LogLevel.INFO.getPriority()) {
            // 배치 처리 대상 로그 (INFO 이상)
            synchronized (LOG_BATCH_LOCK) {
                logBatchQueue.offer(entry);
                
                // 배치 작업이 예약되지 않았다면 예약
                if (logBatchTask == null) {
                    logBatchTask = new Runnable() {
                        @Override
                        public void run() {
                            flushLogBatch();
                        }
                    };
                    mainHandler.postDelayed(logBatchTask, batchIntervalMs);
                }
            }
        } else {
            // 즉시 출력 (ERROR, WARN, DEBUG)
            outputLog(level, tag, formattedMessage, throwable);
        }
    }
    
    /**
     * 로그 배치 플러시
     */
    private void flushLogBatch() {
        synchronized (LOG_BATCH_LOCK) {
            if (logBatchQueue.isEmpty()) {
                logBatchTask = null;
                return;
            }
            
            // 배치 로그를 한 번에 출력
            List<LogEntry> batch = new ArrayList<>();
            LogEntry entry;
            while ((entry = logBatchQueue.poll()) != null) {
                batch.add(entry);
            }
            logBatchTask = null;
            
            // 배치 로그 출력
            for (LogEntry logEntry : batch) {
                outputLog(logEntry.level, logEntry.tag, logEntry.message, logEntry.throwable);
            }
        }
    }
    
    /**
     * 실제 로그 출력
     */
    private void outputLog(LogLevel level, String tag, String message, Throwable throwable) {
        switch (level) {
            case VERBOSE:
                if (throwable != null) {
                    Log.v(tag, message, throwable);
                } else {
                    Log.v(tag, message);
                }
                break;
            case DEBUG:
                if (throwable != null) {
                    Log.d(tag, message, throwable);
                } else {
                    Log.d(tag, message);
                }
                break;
            case INFO:
                if (throwable != null) {
                    Log.i(tag, message, throwable);
                } else {
                    Log.i(tag, message);
                }
                break;
            case WARN:
                if (throwable != null) {
                    Log.w(tag, message, throwable);
                } else {
                    Log.w(tag, message);
                }
                break;
            case ERROR:
                if (throwable != null) {
                    Log.e(tag, message, throwable);
                } else {
                    Log.e(tag, message);
                }
                break;
        }
    }
    
    // ==================== 편의 메서드 (카테고리별) ====================
    
    /**
     * VERBOSE 로그
     */
    public void verbose(LogCategory category, String tag, String message) {
        log(LogLevel.VERBOSE, category, tag, message, null);
    }
    
    /**
     * DEBUG 로그
     */
    public void debug(LogCategory category, String tag, String message) {
        log(LogLevel.DEBUG, category, tag, message, null);
    }
    
    /**
     * INFO 로그
     */
    public void info(LogCategory category, String tag, String message) {
        log(LogLevel.INFO, category, tag, message, null);
    }
    
    /**
     * WARN 로그
     */
    public void warn(LogCategory category, String tag, String message) {
        log(LogLevel.WARN, category, tag, message, null);
    }
    
    /**
     * ERROR 로그
     */
    public void error(LogCategory category, String tag, String message) {
        log(LogLevel.ERROR, category, tag, message, null);
    }
    
    /**
     * ERROR 로그 (예외 포함)
     */
    public void error(LogCategory category, String tag, String message, Throwable throwable) {
        log(LogLevel.ERROR, category, tag, message, throwable);
    }
    
    // ==================== 편의 메서드 (클래스명 자동 태그) ====================
    
    /**
     * DEBUG 로그 (클래스명 자동 태그)
     */
    public void debug(LogCategory category, Class<?> clazz, String message) {
        log(LogLevel.DEBUG, category, clazz.getSimpleName(), message, null);
    }
    
    /**
     * INFO 로그 (클래스명 자동 태그)
     */
    public void info(LogCategory category, Class<?> clazz, String message) {
        log(LogLevel.INFO, category, clazz.getSimpleName(), message, null);
    }
    
    /**
     * WARN 로그 (클래스명 자동 태그)
     */
    public void warn(LogCategory category, Class<?> clazz, String message) {
        log(LogLevel.WARN, category, clazz.getSimpleName(), message, null);
    }
    
    /**
     * ERROR 로그 (클래스명 자동 태그)
     */
    public void error(LogCategory category, Class<?> clazz, String message) {
        log(LogLevel.ERROR, category, clazz.getSimpleName(), message, null);
    }
    
    /**
     * ERROR 로그 (클래스명 자동 태그, 예외 포함)
     */
    public void error(LogCategory category, Class<?> clazz, String message, Throwable throwable) {
        log(LogLevel.ERROR, category, clazz.getSimpleName(), message, throwable);
    }
    
    // ==================== 정적 편의 메서드 ====================
    
    /**
     * 정적 VERBOSE 로그 (간편 사용)
     */
    public static void v(LogCategory category, String tag, String message) {
        getInstance().verbose(category, tag, message);
    }
    
    /**
     * 정적 DEBUG 로그 (간편 사용)
     */
    public static void d(LogCategory category, String tag, String message) {
        getInstance().debug(category, tag, message);
    }
    
    /**
     * 정적 INFO 로그 (간편 사용)
     */
    public static void i(LogCategory category, String tag, String message) {
        getInstance().info(category, tag, message);
    }
    
    /**
     * 정적 WARN 로그 (간편 사용)
     */
    public static void w(LogCategory category, String tag, String message) {
        getInstance().warn(category, tag, message);
    }
    
    /**
     * 정적 ERROR 로그 (간편 사용)
     */
    public static void e(LogCategory category, String tag, String message) {
        getInstance().error(category, tag, message);
    }
    
    /**
     * 정적 ERROR 로그 (간편 사용, 예외 포함)
     */
    public static void e(LogCategory category, String tag, String message, Throwable throwable) {
        getInstance().error(category, tag, message, throwable);
    }
    
    // ==================== 정리 메서드 ====================
    
    /**
     * 로그 배치 큐 정리
     */
    public void clearLogBatchQueue() {
        synchronized (LOG_BATCH_LOCK) {
            if (logBatchTask != null) {
                mainHandler.removeCallbacks(logBatchTask);
                logBatchTask = null;
            }
            // 남은 로그를 플러시
            flushLogBatch();
        }
    }
    
    /**
     * 리소스 정리
     */
    public void cleanup() {
        clearLogBatchQueue();
        logBatchQueue.clear();
        
        if (logSaveExecutor != null) {
            logSaveExecutor.shutdown();
            logSaveExecutor = null;
        }
        
        synchronized (LOG_SAVE_LOCK) {
            logSaveQueue.clear();
            if (logSaveTask != null) {
                mainHandler.removeCallbacks(logSaveTask);
                logSaveTask = null;
            }
        }
    }
    
    // ==================== 로그 저장 관련 메서드 ====================
    
    /**
     * 로그를 데이터베이스 저장 큐에 추가
     */
    private void saveLogToDatabase(LogEntry entry) {
        if (!enableLogSaving || applicationContext == null) {
            return;
        }
        
        // 큐에 추가
        synchronized (LOG_SAVE_LOCK) {
            logSaveQueue.offer(entry);
            
            // 큐가 너무 크면 오래된 항목 제거
            if (logSaveQueue.size() > MAX_LOG_QUEUE_SIZE) {
                logSaveQueue.poll();
            }
        }
        
        // 배치 저장 스케줄링
        scheduleLogSave();
    }
    
    /**
     * 배치 저장 스케줄링
     */
    private void scheduleLogSave() {
        synchronized (LOG_SAVE_LOCK) {
            if (logSaveTask != null) {
                mainHandler.removeCallbacks(logSaveTask);
            }
            
            logSaveTask = () -> {
                flushLogSaveQueue();
                logSaveTask = null;
            };
            
            mainHandler.postDelayed(logSaveTask, LOG_SAVE_INTERVAL_MS);
        }
    }
    
    /**
     * 로그 저장 큐 플러시
     */
    private void flushLogSaveQueue() {
        if (logSaveQueue.isEmpty()) {
            return;
        }
        
        if (logSaveExecutor == null) {
            logSaveExecutor = Executors.newSingleThreadExecutor();
        }
        
        logSaveExecutor.execute(() -> {
            List<LogEntry> entriesToSave = new ArrayList<>();
            synchronized (LOG_SAVE_LOCK) {
                while (!logSaveQueue.isEmpty() && entriesToSave.size() < 100) {
                    LogEntry entry = logSaveQueue.poll();
                    if (entry != null) {
                        entriesToSave.add(entry);
                    }
                }
            }
            
            if (!entriesToSave.isEmpty()) {
                saveLogsToDatabase(entriesToSave);
            }
        });
    }
    
    /**
     * 데이터베이스에 로그 저장
     */
    private void saveLogsToDatabase(List<LogEntry> entries) {
        DBHelper helper = null;
        android.database.sqlite.SQLiteDatabase db = null;
        
        try {
            helper = new DBHelper(applicationContext, "itf_temperature_table.db", null, 
                Constants.InitialValues.DATABASE_VERSION);
            db = helper.getWritableDatabase();
            
            db.beginTransaction();
            try {
                String sql = "INSERT INTO tbl_log_messages (" +
                    "clm_log_type, clm_error_id, clm_category, clm_tag, " +
                    "clm_message, clm_stack_trace, clm_timestamp, clm_thread_name, " +
                    "clm_user_id, clm_device_info, clm_extra_data, clm_is_synced" +
                    ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                
                for (LogEntry entry : entries) {
                    String stackTrace = null;
                    if (entry.throwable != null) {
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        entry.throwable.printStackTrace(pw);
                        stackTrace = sw.toString();
                    }
                    
                    String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", 
                        Locale.getDefault()).format(new Date(entry.timestamp));
                    
                    String threadName = Thread.currentThread().getName();
                    
                    db.execSQL(sql, new Object[]{
                        entry.level.getCode(),
                        entry.errorId,
                        entry.category.getCode(),
                        entry.tag,
                        entry.message,
                        stackTrace,
                        timestamp,
                        threadName,
                        null, // user_id
                        getDeviceInfo(), // device_info
                        null, // extra_data
                        0 // is_synced
                    });
                }
                
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            
        } catch (Exception e) {
            // 로그 저장 실패는 조용히 처리 (무한 루프 방지)
            Log.e("LogManager", "Failed to save logs to database", e);
        } finally {
            if (helper != null) {
                helper.close();
            }
        }
    }
    
    /**
     * 기기 정보 가져오기
     */
    private String getDeviceInfo() {
        try {
            if (applicationContext == null) return null;
            
            String model = Build.MODEL;
            String manufacturer = Build.MANUFACTURER;
            String androidVersion = Build.VERSION.RELEASE;
            
            return String.format("%s %s (Android %s)", manufacturer, model, androidVersion);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 오래된 로그 정리
     */
    public void cleanupOldLogs() {
        if (applicationContext == null) return;
        
        if (logSaveExecutor == null) {
            logSaveExecutor = Executors.newSingleThreadExecutor();
        }
        
        logSaveExecutor.execute(() -> {
            DBHelper helper = null;
            android.database.sqlite.SQLiteDatabase db = null;
            
            try {
                helper = new DBHelper(applicationContext, "itf_temperature_table.db", null, 
                    Constants.InitialValues.DATABASE_VERSION);
                db = helper.getWritableDatabase();
                
                // 30일 이상 된 로그 삭제
                String deleteSql = "DELETE FROM tbl_log_messages WHERE " +
                    "datetime(clm_timestamp) < datetime('now', '-' || ? || ' days')";
                
                db.execSQL(deleteSql, new Object[]{MAX_LOG_AGE_DAYS});
                
            } catch (Exception e) {
                Log.e("LogManager", "Failed to cleanup old logs", e);
            } finally {
                if (helper != null) {
                    helper.close();
                }
            }
        });
    }
    
    /**
     * 로그 정리 스케줄링
     */
    private void scheduleLogCleanup() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
            cleanupOldLogs();
            scheduleLogCleanup(); // 다음 날 다시 스케줄링
        }, 24 * 60 * 60 * 1000); // 24시간
    }
}

