# 로그 메시지 저장 시스템 설계 문서

## 1. 개요

액티비티들의 모든 로그 메시지를 데이터베이스에 저장하고, 에러 로그에 고유 ID를 부여하여 추적 및 분석이 가능하도록 하는 시스템을 설계합니다.

## 2. 데이터베이스 테이블 설계

### 2.1 tbl_log_messages 테이블

```sql
CREATE TABLE IF NOT EXISTS tbl_log_messages (
    clm_log_id INTEGER PRIMARY KEY AUTOINCREMENT,
    clm_log_type TEXT NOT NULL,              -- ERROR, WARNING, INFO, DEBUG, VERBOSE
    clm_error_id TEXT,                        -- 에러 로그인 경우 고유 ID (예: ERR-001, ERR-BT-001)
    clm_category TEXT NOT NULL,               -- PS, BT, US, SI, ER, CA, TH, BI, RS
    clm_tag TEXT NOT NULL,                    -- 로그 태그 (클래스명 등)
    clm_message TEXT NOT NULL,                -- 로그 메시지
    clm_stack_trace TEXT,                     -- 스택 트레이스 (에러인 경우)
    clm_timestamp TEXT NOT NULL,              -- 로그 발생 시간 (yyyy-MM-dd HH:mm:ss.SSS)
    clm_thread_name TEXT,                     -- 스레드 이름
    clm_user_id TEXT,                         -- 사용자 ID (선택적)
    clm_device_info TEXT,                     -- 기기 정보 (선택적)
    clm_extra_data TEXT,                      -- 추가 데이터 (JSON 형식, 선택적)
    clm_is_synced INTEGER DEFAULT 0,          -- 서버 동기화 여부 (0: 미동기화, 1: 동기화)
    clm_synced_timestamp TEXT                  -- 서버 동기화 시간
);

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_log_type ON tbl_log_messages(clm_log_type);
CREATE INDEX IF NOT EXISTS idx_error_id ON tbl_log_messages(clm_error_id);
CREATE INDEX IF NOT EXISTS idx_category ON tbl_log_messages(clm_category);
CREATE INDEX IF NOT EXISTS idx_timestamp ON tbl_log_messages(clm_timestamp);
CREATE INDEX IF NOT EXISTS idx_synced ON tbl_log_messages(clm_is_synced);
```

### 2.2 tbl_error_catalog 테이블 (에러 카탈로그)

```sql
CREATE TABLE IF NOT EXISTS tbl_error_catalog (
    clm_error_id TEXT PRIMARY KEY,            -- 에러 ID (예: ERR-001, ERR-BT-001)
    clm_error_category TEXT NOT NULL,         -- 에러 카테고리 (BT, ER, US 등)
    clm_error_code TEXT NOT NULL,             -- 에러 코드 (001, 002 등)
    clm_error_name_ko TEXT,                    -- 에러 이름 (한국어)
    clm_error_name_en TEXT,                    -- 에러 이름 (영어)
    clm_error_description_ko TEXT,             -- 에러 설명 (한국어)
    clm_error_description_en TEXT,             -- 에러 설명 (영어)
    clm_severity TEXT NOT NULL,                -- 심각도 (CRITICAL, HIGH, MEDIUM, LOW)
    clm_solution_ko TEXT,                      -- 해결 방법 (한국어)
    clm_solution_en TEXT,                      -- 해결 방법 (영어)
    clm_created_timestamp TEXT NOT NULL,       -- 생성 시간
    clm_updated_timestamp TEXT                 -- 수정 시간
);

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_error_category ON tbl_error_catalog(clm_error_category);
CREATE INDEX IF NOT EXISTS idx_error_severity ON tbl_error_catalog(clm_severity);
```

## 3. 에러 로그 ID 체계

### 3.1 ID 형식

```
ERR-{CATEGORY}-{NUMBER}
```

- `ERR`: 에러 로그임을 나타내는 접두사
- `{CATEGORY}`: 로그 카테고리 코드 (BT, ER, US, SI 등)
- `{NUMBER}`: 3자리 숫자 (001, 002, ...)

### 3.2 예시

- `ERR-BT-001`: 블루투스 연결 실패
- `ERR-BT-002`: 블루투스 권한 거부
- `ERR-ER-001`: 일반 에러
- `ERR-US-001`: USB 연결 실패
- `ERR-SI-001`: 서버 연결 실패
- `ERR-PS-001`: 프로세스 초기화 실패

### 3.3 에러 ID 매핑 클래스

```java
public class ErrorIdMapper {
    // 블루투스 관련 에러
    public static final String ERR_BT_001 = "ERR-BT-001"; // 블루투스 연결 실패
    public static final String ERR_BT_002 = "ERR-BT-002"; // 블루투스 권한 거부
    public static final String ERR_BT_003 = "ERR-BT-003"; // 블루투스 장치 이름 파싱 실패
    
    // USB 관련 에러
    public static final String ERR_US_001 = "ERR-US-001"; // USB 연결 실패
    public static final String ERR_US_002 = "ERR-US-002"; // USB 권한 거부
    public static final String ERR_US_003 = "ERR-US-003"; // USB 장치 미지원
    
    // 서버 관련 에러
    public static final String ERR_SI_001 = "ERR-SI-001"; // 서버 연결 실패
    public static final String ERR_SI_002 = "ERR-SI-002"; // HTTP 요청 실패
    public static final String ERR_SI_003 = "ERR-SI-003"; // JSON 파싱 실패
    
    // 프로세스 관련 에러
    public static final String ERR_PS_001 = "ERR-PS-001"; // 프로세스 초기화 실패
    public static final String ERR_PS_002 = "ERR-PS-002"; // 프로세스 실행 실패
    
    // 일반 에러
    public static final String ERR_ER_001 = "ERR-ER-001"; // 일반 예외 발생
    public static final String ERR_ER_002 = "ERR-ER-002"; // 데이터베이스 오류
    public static final String ERR_ER_003 = "ERR-ER-003"; // 메모리 부족
    
    // 에러 ID를 카테고리로부터 자동 생성하는 메서드
    public static String generateErrorId(LogCategory category, int errorNumber) {
        return String.format("ERR-%s-%03d", category.getCode(), errorNumber);
    }
    
    // 메시지 패턴으로부터 에러 ID를 추론하는 메서드
    public static String inferErrorId(LogCategory category, String message, Throwable throwable) {
        if (message == null) message = "";
        String lowerMessage = message.toLowerCase();
        
        // 블루투스 관련
        if (category == LogCategory.BT) {
            if (lowerMessage.contains("연결 실패") || lowerMessage.contains("connection failed")) {
                return ERR_BT_001;
            }
            if (lowerMessage.contains("권한") || lowerMessage.contains("permission")) {
                return ERR_BT_002;
            }
            if (lowerMessage.contains("파싱") || lowerMessage.contains("parsing")) {
                return ERR_BT_003;
            }
        }
        
        // USB 관련
        if (category == LogCategory.US) {
            if (lowerMessage.contains("연결 실패") || lowerMessage.contains("connection failed")) {
                return ERR_US_001;
            }
            if (lowerMessage.contains("권한") || lowerMessage.contains("permission")) {
                return ERR_US_002;
            }
            if (lowerMessage.contains("미지원") || lowerMessage.contains("not supported")) {
                return ERR_US_003;
            }
        }
        
        // 서버 관련
        if (category == LogCategory.SI) {
            if (lowerMessage.contains("연결 실패") || lowerMessage.contains("connection failed")) {
                return ERR_SI_001;
            }
            if (lowerMessage.contains("http") || lowerMessage.contains("요청")) {
                return ERR_SI_002;
            }
            if (lowerMessage.contains("json") || lowerMessage.contains("파싱")) {
                return ERR_SI_003;
            }
        }
        
        // 기본값: 카테고리 기반 일반 에러
        return generateErrorId(category, 1);
    }
}
```

## 4. LogManager 개선 방안

### 4.1 주요 변경사항

1. **로그 저장 기능 추가**: 모든 로그를 데이터베이스에 저장
2. **에러 ID 자동 할당**: 에러 로그에 자동으로 ID 부여
3. **배치 저장**: 성능 최적화를 위한 배치 저장
4. **비동기 저장**: UI 스레드 블로킹 방지
5. **로그 정리**: 오래된 로그 자동 삭제

### 4.2 LogManager 구조

```java
public class LogManager {
    // 기존 코드...
    
    // 로그 저장 관련 필드
    private static final int MAX_LOG_QUEUE_SIZE = 1000;
    private static final long LOG_SAVE_INTERVAL_MS = 5000; // 5초마다 저장
    private static final int MAX_LOG_AGE_DAYS = 30; // 30일 이상 된 로그 삭제
    
    private final ConcurrentLinkedQueue<LogEntry> logSaveQueue = new ConcurrentLinkedQueue<>();
    private final Handler logSaveHandler = new Handler(Looper.getMainLooper());
    private Runnable logSaveTask = null;
    private Context applicationContext;
    private ExecutorService logSaveExecutor;
    
    // 로그 저장 활성화 여부
    private boolean enableLogSaving = true;
    
    // 로그 저장 메서드
    private void saveLogToDatabase(LogEntry entry) {
        if (!enableLogSaving || applicationContext == null) {
            return;
        }
        
        // 에러 로그인 경우 에러 ID 할당
        if (entry.level == LogLevel.ERROR) {
            String errorId = ErrorIdMapper.inferErrorId(
                entry.category, 
                entry.message, 
                entry.throwable
            );
            entry.errorId = errorId;
        }
        
        // 큐에 추가
        logSaveQueue.offer(entry);
        
        // 큐가 너무 크면 오래된 항목 제거
        if (logSaveQueue.size() > MAX_LOG_QUEUE_SIZE) {
            logSaveQueue.poll();
        }
        
        // 배치 저장 스케줄링
        scheduleLogSave();
    }
    
    // 배치 저장 스케줄링
    private void scheduleLogSave() {
        if (logSaveTask != null) {
            logSaveHandler.removeCallbacks(logSaveTask);
        }
        
        logSaveTask = () -> {
            flushLogSaveQueue();
            logSaveTask = null;
        };
        
        logSaveHandler.postDelayed(logSaveTask, LOG_SAVE_INTERVAL_MS);
    }
    
    // 로그 저장 큐 플러시
    private void flushLogSaveQueue() {
        if (logSaveQueue.isEmpty()) {
            return;
        }
        
        if (logSaveExecutor == null) {
            logSaveExecutor = Executors.newSingleThreadExecutor();
        }
        
        logSaveExecutor.execute(() -> {
            List<LogEntry> entriesToSave = new ArrayList<>();
            while (!logSaveQueue.isEmpty() && entriesToSave.size() < 100) {
                LogEntry entry = logSaveQueue.poll();
                if (entry != null) {
                    entriesToSave.add(entry);
                }
            }
            
            if (!entriesToSave.isEmpty()) {
                saveLogsToDatabase(entriesToSave);
            }
        });
    }
    
    // 데이터베이스에 로그 저장
    private void saveLogsToDatabase(List<LogEntry> entries) {
        DBHelper helper = null;
        SQLiteDatabase db = null;
        
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
            android.util.Log.e("LogManager", "Failed to save logs to database", e);
        } finally {
            if (helper != null) {
                helper.close();
            }
        }
    }
    
    // 기기 정보 가져오기
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
    
    // 오래된 로그 정리
    public void cleanupOldLogs() {
        if (applicationContext == null) return;
        
        if (logSaveExecutor == null) {
            logSaveExecutor = Executors.newSingleThreadExecutor();
        }
        
        logSaveExecutor.execute(() -> {
            DBHelper helper = null;
            SQLiteDatabase db = null;
            
            try {
                helper = new DBHelper(applicationContext, "itf_temperature_table.db", null, 
                    Constants.InitialValues.DATABASE_VERSION);
                db = helper.getWritableDatabase();
                
                // 30일 이상 된 로그 삭제
                String deleteSql = "DELETE FROM tbl_log_messages WHERE " +
                    "datetime(clm_timestamp) < datetime('now', '-' || ? || ' days')";
                
                db.execSQL(deleteSql, new Object[]{MAX_LOG_AGE_DAYS});
                
            } catch (Exception e) {
                android.util.Log.e("LogManager", "Failed to cleanup old logs", e);
            } finally {
                if (helper != null) {
                    helper.close();
                }
            }
        });
    }
    
    // 초기화 메서드
    public void initialize(Context context) {
        this.applicationContext = context.getApplicationContext();
        
        // 주기적으로 오래된 로그 정리 (하루에 한 번)
        scheduleLogCleanup();
    }
    
    // 로그 정리 스케줄링
    private void scheduleLogCleanup() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
            cleanupOldLogs();
            scheduleLogCleanup(); // 다음 날 다시 스케줄링
        }, 24 * 60 * 60 * 1000); // 24시간
    }
}
```

## 5. LogEntry 클래스 확장

```java
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
```

## 6. DBHelper에 테이블 생성 추가

```java
// DBHelper.onCreate() 메서드에 추가

// tbl_log_messages 테이블 생성
sql = "CREATE TABLE IF NOT EXISTS tbl_log_messages ("
    + "clm_log_id INTEGER PRIMARY KEY AUTOINCREMENT,"
    + "clm_log_type TEXT NOT NULL,"
    + "clm_error_id TEXT,"
    + "clm_category TEXT NOT NULL,"
    + "clm_tag TEXT NOT NULL,"
    + "clm_message TEXT NOT NULL,"
    + "clm_stack_trace TEXT,"
    + "clm_timestamp TEXT NOT NULL,"
    + "clm_thread_name TEXT,"
    + "clm_user_id TEXT,"
    + "clm_device_info TEXT,"
    + "clm_extra_data TEXT,"
    + "clm_is_synced INTEGER DEFAULT 0,"
    + "clm_synced_timestamp TEXT"
    + ");";
db.execSQL(sql);

// 인덱스 생성
sql = "CREATE INDEX IF NOT EXISTS idx_log_type ON tbl_log_messages(clm_log_type);";
db.execSQL(sql);

sql = "CREATE INDEX IF NOT EXISTS idx_error_id ON tbl_log_messages(clm_error_id);";
db.execSQL(sql);

sql = "CREATE INDEX IF NOT EXISTS idx_category ON tbl_log_messages(clm_category);";
db.execSQL(sql);

sql = "CREATE INDEX IF NOT EXISTS idx_timestamp ON tbl_log_messages(clm_timestamp);";
db.execSQL(sql);

sql = "CREATE INDEX IF NOT EXISTS idx_synced ON tbl_log_messages(clm_is_synced);";
db.execSQL(sql);

// tbl_error_catalog 테이블 생성
sql = "CREATE TABLE IF NOT EXISTS tbl_error_catalog ("
    + "clm_error_id TEXT PRIMARY KEY,"
    + "clm_error_category TEXT NOT NULL,"
    + "clm_error_code TEXT NOT NULL,"
    + "clm_error_name_ko TEXT,"
    + "clm_error_name_en TEXT,"
    + "clm_error_description_ko TEXT,"
    + "clm_error_description_en TEXT,"
    + "clm_severity TEXT NOT NULL,"
    + "clm_solution_ko TEXT,"
    + "clm_solution_en TEXT,"
    + "clm_created_timestamp TEXT NOT NULL,"
    + "clm_updated_timestamp TEXT"
    + ");";
db.execSQL(sql);

// 인덱스 생성
sql = "CREATE INDEX IF NOT EXISTS idx_error_category ON tbl_error_catalog(clm_error_category);";
db.execSQL(sql);

sql = "CREATE INDEX IF NOT EXISTS idx_error_severity ON tbl_error_catalog(clm_severity);";
db.execSQL(sql);
```

## 7. 초기 에러 카탈로그 데이터

```java
// TestData.java에 추가할 메서드

public static void insertInitialErrorCatalog(Context context) {
    DBHelper helper = null;
    SQLiteDatabase db = null;
    
    try {
        helper = new DBHelper(context, "itf_temperature_table.db", null, DATABASE_VERSION);
        db = helper.getWritableDatabase();
        
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(new Date());
        
        String[][] errorCatalog = {
            // 블루투스 관련
            {"ERR-BT-001", "BT", "001", "블루투스 연결 실패", "Bluetooth Connection Failed",
             "블루투스 장치와의 연결에 실패했습니다.", "Failed to connect to Bluetooth device.",
             "HIGH", "블루투스가 활성화되어 있는지 확인하고, 장치가 페어링되어 있는지 확인하세요.",
             "Check if Bluetooth is enabled and the device is paired.", timestamp, timestamp},
            
            {"ERR-BT-002", "BT", "002", "블루투스 권한 거부", "Bluetooth Permission Denied",
             "블루투스 권한이 거부되었습니다.", "Bluetooth permission has been denied.",
             "HIGH", "설정에서 블루투스 권한을 허용해주세요.", 
             "Please allow Bluetooth permission in settings.", timestamp, timestamp},
            
            // USB 관련
            {"ERR-US-001", "US", "001", "USB 연결 실패", "USB Connection Failed",
             "USB 장치와의 연결에 실패했습니다.", "Failed to connect to USB device.",
             "HIGH", "USB 케이블이 제대로 연결되어 있는지 확인하세요.",
             "Check if the USB cable is properly connected.", timestamp, timestamp},
            
            // 서버 관련
            {"ERR-SI-001", "SI", "001", "서버 연결 실패", "Server Connection Failed",
             "서버와의 연결에 실패했습니다.", "Failed to connect to server.",
             "HIGH", "네트워크 연결을 확인하고 서버 주소가 올바른지 확인하세요.",
             "Check network connection and verify server address.", timestamp, timestamp},
            
            // 일반 에러
            {"ERR-ER-001", "ER", "001", "일반 예외 발생", "General Exception",
             "예상치 못한 오류가 발생했습니다.", "An unexpected error occurred.",
             "MEDIUM", "앱을 재시작하거나 개발자에게 문의하세요.",
             "Restart the app or contact the developer.", timestamp, timestamp},
        };
        
        String sql = "INSERT OR IGNORE INTO tbl_error_catalog (" +
            "clm_error_id, clm_error_category, clm_error_code, " +
            "clm_error_name_ko, clm_error_name_en, " +
            "clm_error_description_ko, clm_error_description_en, " +
            "clm_severity, clm_solution_ko, clm_solution_en, " +
            "clm_created_timestamp, clm_updated_timestamp" +
            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        db.beginTransaction();
        try {
            for (String[] row : errorCatalog) {
                db.execSQL(sql, row);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        
    } catch (Exception e) {
        Log.e(TAG, "Failed to insert error catalog", e);
    } finally {
        if (helper != null) {
            helper.close();
        }
    }
}
```

## 8. 사용 방법

### 8.1 LogManager 초기화

```java
// ActivitySplash.java의 onCreate에서
LogManager.getInstance().initialize(this);
```

### 8.2 로그 사용 (기존과 동일)

```java
// 에러 로그는 자동으로 에러 ID가 할당됨
LogManager.e(LogManager.LogCategory.BT, TAG, "블루투스 연결 실패", exception);
// → ERR-BT-001로 자동 할당

// 일반 로그도 자동으로 저장됨
LogManager.i(LogManager.LogCategory.PS, TAG, "프로세스 시작");
```

### 8.3 에러 로그 조회

```java
// 특정 에러 ID로 로그 조회
String errorId = "ERR-BT-001";
List<LogEntry> logs = queryLogsByErrorId(errorId);

// 카테고리별 로그 조회
List<LogEntry> logs = queryLogsByCategory(LogCategory.BT);

// 기간별 로그 조회
List<LogEntry> logs = queryLogsByDateRange(startDate, endDate);
```

## 9. 성능 고려사항

1. **비동기 저장**: 로그 저장은 별도 스레드에서 실행
2. **배치 저장**: 여러 로그를 한 번에 저장하여 성능 최적화
3. **큐 크기 제한**: 메모리 사용량 제한
4. **인덱스 활용**: 빠른 조회를 위한 인덱스 생성
5. **자동 정리**: 오래된 로그 자동 삭제

## 10. 향후 확장 가능성

1. **서버 동기화**: 로그를 서버로 전송
2. **로그 분석**: 에러 패턴 분석
3. **알림 기능**: 특정 에러 발생 시 알림
4. **대시보드**: 로그 모니터링 대시보드



