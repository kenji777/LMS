package itf.com.app.simple_line_test_ovio_new.kiosk;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.net.ServerSocket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import itf.com.app.simple_line_test_ovio_new.util.AppSettings;
import itf.com.app.simple_line_test_ovio_new.util.TestData;

/**
 * 키오스크 모드를 모든 Activity에 자동으로 적용하는 Application 클래스
 */
public class KioskModeApplication extends android.app.Application {
    
    private static final String TAG = "KioskModeApplication";
    
    // 키오스크 모드: 시스템 UI 지속적으로 숨기기 위한 Handler
    private Handler kioskModeHandler = null;
    private Runnable kioskModeRunnable = null;
    
    // WebServer 인스턴스
    private itf.com.app.simple_line_test_ovio_new.util.WebServer webServer = null;
    
    // WebServer 관리
    private ExecutorService webServerExecutor = null;
    private ScheduledExecutorService webServerMonitorExecutor = null;
    private ScheduledFuture<?> webServerMonitorTask = null;
    private final AtomicBoolean isWebServerInitializing = new AtomicBoolean(false);
    private static final int WEBSERVER_START_RETRY_DELAY_MS = 3000; // 재시도 대기 시간 (3초)
    private static final int WEBSERVER_MAX_START_RETRIES = 5; // 최대 재시도 횟수
    private static final int WEBSERVER_MONITOR_INTERVAL_SEC = 30; // 모니터링 간격 (30초)

    // Crash cleanup handler (Java-level crashes only)
    private volatile Thread.UncaughtExceptionHandler previousUncaughtExceptionHandler = null;
    
    @Override
    public void onCreate() {
        super.onCreate();

        // Java-level crash 시 WebServer 종료 시도 (native crash / SIGKILL 등은 불가)
        installCrashCleanupHandler();

        // 앱 재시작/시작 시점에 포트 점유 상태 + 웹서버 상태 로깅 (StrictMode 회피 위해 백그라운드에서 수행)
        logWebServerStartupDiagnosticsAsync("onCreate");

        // 설정 기본 로우 보장 + 진동 설정 로드 (백그라운드)
        ensureDefaultSettingsAsync();
        
        // 모든 Activity의 생명주기를 모니터링하여 키오스크 모드 자동 적용
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                // Activity 생성 시 Window 플래그만 설정 (Window가 완전히 준비되지 않았을 수 있음)
                // ⚠️ 중요: onActivityCreated에서는 Window 플래그만 설정하고, 시스템 UI 숨기기는 onActivityStarted에서 수행
                if (activity instanceof AppCompatActivity) {
                    try {
                        // Window 플래그 설정 (Window가 준비되지 않았을 수 있으므로 안전하게 처리)
                        if (activity.getWindow() != null) {
                            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Error setting window flags in onActivityCreated: " + e.getMessage());
                    }
                }
            }
            
            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                // Activity 시작 시 키오스크 모드 설정 (Window가 준비된 후)
                if (activity instanceof AppCompatActivity) {
                    // Window와 DecorView가 준비되었는지 확인 후 시스템 UI 숨기기
                    if (isWindowReady(activity)) {
                        setupKioskMode(activity);
                        hideSystemUI(activity);
                    }
                }
            }
            
            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                // Activity 재개 시 키오스크 모드 재적용
                if (activity instanceof AppCompatActivity) {
                    // Window와 DecorView가 준비되었는지 확인 후 키오스크 모드 설정
                    if (isWindowReady(activity)) {
                        setupKioskMode(activity);
                        startKioskModeMonitoring(activity);
                    }
                }
            }
            
            @Override
            public void onActivityPaused(@NonNull Activity activity) {
                // Activity 일시정지 시 모니터링 중지
                stopKioskModeMonitoring();
            }
            
            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                // Activity 정지 시 모니터링 중지
                stopKioskModeMonitoring();
            }
            
            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
                // 상태 저장 시 아무것도 하지 않음
            }
            
            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                // Activity 종료 시 모니터링 중지
                stopKioskModeMonitoring();
            }
        });
        
        // WebServer 초기화 및 시작 (백그라운드 스레드에서 실행)
        initializeWebServer();
        
        Log.i(TAG, "KioskModeApplication initialized - All activities will have kiosk mode enabled");
    }

    /**
     * tbl_setting_info에 기본 설정 로우를 보장하고(없으면 생성), AppSettings 캐시를 로드
     * - 기존 값이 있으면 절대 덮어쓰지 않음
     */
    private void ensureDefaultSettingsAsync() {
        try {
            Thread t = new Thread(() -> {
                try {
                    // 진동 강도 설정 로우 보장
                    ensureSettingRowExists(
                            AppSettings.SETTING_ID_VIBRATION_AMPLITUDE,
                            "DEFAULT",
                            "진동 강도 (1~255 또는 DEFAULT)"
                    );

                    // 메모리 캐시 로드
                    AppSettings.loadFromDb(this);
                } catch (Exception e) {
                    Log.w(TAG, "Failed to ensure default settings: " + e.getMessage());
                }
            }, "Settings-Init");
            t.setDaemon(true);
            t.start();
        } catch (Exception e) {
            Log.w(TAG, "Failed to start Settings-Init thread: " + e.getMessage());
        }
    }

    private void ensureSettingRowExists(String settingId, String defaultValue, String comment) {
        try {
            if (settingId == null || settingId.trim().isEmpty()) return;

            boolean exists = false;
            try {
                java.util.List<java.util.Map<String, String>> settings = TestData.selectSettingInfo(this);
                if (settings != null) {
                    for (java.util.Map<String, String> row : settings) {
                        if (row == null) continue;
                        if (settingId.equals(row.get("clm_setting_id"))) {
                            exists = true;
                            break;
                        }
                    }
                }
            } catch (Exception ignored) {
            }

            if (exists) return;

            String now = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());
            Map<String, String> data = new HashMap<>();
            data.put("clm_setting_id", settingId);
            data.put("clm_setting_value", defaultValue != null ? defaultValue : "");
            data.put("clm_comment", comment != null ? comment : "");
            data.put("clm_test_timestamp", now);

            TestData.insertSettingInfo(this, data);
            Log.i(TAG, "Inserted default setting row: " + settingId + "=" + defaultValue);
        } catch (Exception e) {
            Log.w(TAG, "Failed to ensure setting row exists: " + settingId + " - " + e.getMessage());
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        // WebServer 종료 및 리소스 정리
        shutdownWebServer();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        // 메모리가 매우 부족한 경우, 리소스 정리를 통해 프로세스 생존 가능성 향상
        try {
            shutdownWebServer();
        } catch (Exception e) {
            Log.w(TAG, "Error while shutting down WebServer onLowMemory: " + e.getMessage());
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        // 프로세스가 백그라운드로 밀리거나 종료 직전 단계에서 최대한 정리
        // TRIM_MEMORY_COMPLETE: 프로세스가 곧 종료될 수 있는 가장 강한 신호
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE) {
            try {
                shutdownWebServer();
            } catch (Exception e) {
                Log.w(TAG, "Error while shutting down WebServer onTrimMemory(" + level + "): " + e.getMessage());
            }
        }
    }

    /**
     * Java-level crash(UncaughtException) 시 WebServer를 먼저 종료 시도.
     * - 주의: SIGKILL/네이티브 크래시/강제 종료(설정에서 강제중지) 등에서는 호출되지 않음.
     */
    private void installCrashCleanupHandler() {
        try {
            previousUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();

            Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
                try {
                    Log.e(TAG, "UncaughtException detected. Attempting to shutdown WebServer before crash...", throwable);
                    shutdownWebServer();
                } catch (Exception e) {
                    Log.w(TAG, "Failed to shutdown WebServer during crash handling: " + e.getMessage());
                } finally {
                    // 기존 핸들러로 위임 (시스템 크래시 다이얼로그/로그 유지)
                    if (previousUncaughtExceptionHandler != null) {
                        previousUncaughtExceptionHandler.uncaughtException(thread, throwable);
                    } else {
                        // fallback: 프로세스 종료
                        android.os.Process.killProcess(android.os.Process.myPid());
                        System.exit(10);
                    }
                }
            });
        } catch (Exception e) {
            Log.w(TAG, "Failed to install crash cleanup handler: " + e.getMessage());
        }
    }
    
    /**
     * WebServer 초기화 및 시작 (백그라운드 스레드에서 실행)
     */
    private void initializeWebServer() {
        if (isWebServerInitializing.getAndSet(true)) {
            Log.w(TAG, "WebServer initialization already in progress");
            return;
        }
        
        // ExecutorService 생성
        if (webServerExecutor == null) {
            webServerExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r, "WebServer-Init");
                thread.setPriority(Thread.NORM_PRIORITY);
                return thread;
            });
        }
        
        // 백그라운드 스레드에서 웹서버 시작
        webServerExecutor.execute(() -> {
            try {
                String serverIp = itf.com.app.simple_line_test_ovio_new.util.Constants.ServerConfig.SERVER_IP_PORT;
                // 재시도 로직을 포함한 웹서버 시작 (내부에서 포트 선택 + 인스턴스 생성까지 수행)
                boolean started = startWebServerWithRetry();
                
                if (started) {
                    Log.i(TAG, "WebServer started successfully on port " + webServer.getActualPort());
                    Log.i(TAG, "WebServer status: " + webServer.getStatusInfo());
                    
                    // 웹서버 모니터링 시작
                    startWebServerMonitoring();
                } else {
                    Log.e(TAG, "Failed to start WebServer after retries");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error initializing WebServer", e);
            } finally {
                isWebServerInitializing.set(false);
            }
        });
    }
    
    /**
     * 재시도 로직을 포함한 웹서버 시작
     * @return true if started successfully, false otherwise
     */
    private boolean startWebServerWithRetry() {
        int retryCount = 0;
        boolean started = false;
        
        while (retryCount < WEBSERVER_MAX_START_RETRIES && !started) {
            retryCount++;
            
            try {
                // 기존 인스턴스가 남아있으면 먼저 정리 (같은 프로세스 내에서만 "강제 종료" 가능)
                if (webServer != null) {
                    try {
                        webServer.stopServer();
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to stop existing WebServer before restart: " + e.getMessage());
                    }
                    webServer = null;
                }

                // NanoHTTPD는 포트 변경이 필요하면 새 인스턴스를 생성해야 함.
                // 8080/8081이 사용 중이면 범위 내에서 실제로 start()가 성공하는 포트를 찾아 시작한다.
                String serverIp = itf.com.app.simple_line_test_ovio_new.util.Constants.ServerConfig.SERVER_IP_PORT;
                itf.com.app.simple_line_test_ovio_new.util.WebServer startedServer =
                        itf.com.app.simple_line_test_ovio_new.util.WebServer.startNewServerWithPortFallback(
                                this, serverIp, itf.com.app.simple_line_test_ovio_new.util.Constants.InitialValues.WS_PORT
                        );

                if (startedServer != null) {
                    webServer = startedServer;
                    started = true;
                } else {
                    started = false;
                }
                
                if (started) {
                    Log.i(TAG, "WebServer started successfully on attempt " + retryCount);
                    break;
                } else {
                    Log.w(TAG, "WebServer start failed on attempt " + retryCount + "/" + WEBSERVER_MAX_START_RETRIES);
                    
                    if (retryCount < WEBSERVER_MAX_START_RETRIES) {
                        Log.i(TAG, "Retrying WebServer start in " + WEBSERVER_START_RETRY_DELAY_MS + "ms...");
                        try {
                            Thread.sleep(WEBSERVER_START_RETRY_DELAY_MS);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            Log.w(TAG, "WebServer start retry interrupted");
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception during WebServer start attempt " + retryCount, e);
                
                if (retryCount < WEBSERVER_MAX_START_RETRIES) {
                    try {
                        Thread.sleep(WEBSERVER_START_RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        return started;
    }
    
    /**
     * 웹서버 상태 모니터링 시작 (주기적으로 상태 확인 및 자동 재시작)
     */
    private void startWebServerMonitoring() {
        if (webServerMonitorExecutor == null) {
            webServerMonitorExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "WebServer-Monitor");
                thread.setPriority(Thread.NORM_PRIORITY);
                thread.setDaemon(true);
                return thread;
            });
        }
        
        // 기존 모니터링 태스크가 있으면 취소
        if (webServerMonitorTask != null && !webServerMonitorTask.isCancelled()) {
            webServerMonitorTask.cancel(false);
        }
        
        // 주기적으로 웹서버 상태 확인
        webServerMonitorTask = webServerMonitorExecutor.scheduleWithFixedDelay(
            () -> {
                try {
                    if (webServer == null) {
                        Log.w(TAG, "WebServer instance is null, attempting to reinitialize...");
                        isWebServerInitializing.set(false);
                        initializeWebServer();
                        return;
                    }
                    
                    boolean isRunning = webServer.isRunning();
                    
                    if (!isRunning) {
                        Log.w(TAG, "WebServer is not running, attempting to restart...");
                        boolean restarted = webServer.restartServer();
                        
                        if (restarted) {
                            Log.i(TAG, "WebServer restarted successfully");
                        } else {
                            Log.e(TAG, "Failed to restart WebServer, will retry on next check");
                        }
                    } else {
                        // 정상 동작 중 (디버그 로그는 필요시에만)
                        // Log.d(TAG, "WebServer is running normally on port " + webServer.getActualPort());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error during WebServer monitoring", e);
                }
            },
            WEBSERVER_MONITOR_INTERVAL_SEC, // 초기 지연
            WEBSERVER_MONITOR_INTERVAL_SEC, // 주기
            TimeUnit.SECONDS
        );
        
        Log.i(TAG, "WebServer monitoring started (interval: " + WEBSERVER_MONITOR_INTERVAL_SEC + "s)");
    }
    
    /**
     * 웹서버 모니터링 중지
     */
    private void stopWebServerMonitoring() {
        if (webServerMonitorTask != null && !webServerMonitorTask.isCancelled()) {
            webServerMonitorTask.cancel(false);
            webServerMonitorTask = null;
            Log.i(TAG, "WebServer monitoring stopped");
        }
    }
    
    /**
     * 웹서버 종료 및 리소스 정리
     */
    private void shutdownWebServer() {
        Log.i(TAG, "Shutting down WebServer...");
        
        // 모니터링 중지
        stopWebServerMonitoring();
        
        // 웹서버 종료
        if (webServer != null) {
            try {
                webServer.stopServer();
                Log.i(TAG, "WebServer stopped");
            } catch (Exception e) {
                Log.e(TAG, "Failed to stop WebServer", e);
            }
        }
        
        // ExecutorService 종료
        if (webServerExecutor != null) {
            try {
                webServerExecutor.shutdown();
                if (!webServerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    webServerExecutor.shutdownNow();
                }
                Log.i(TAG, "WebServer executor shutdown");
            } catch (Exception e) {
                Log.e(TAG, "Error shutting down WebServer executor", e);
                webServerExecutor.shutdownNow();
            }
        }
        
        if (webServerMonitorExecutor != null) {
            try {
                webServerMonitorExecutor.shutdown();
                if (!webServerMonitorExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    webServerMonitorExecutor.shutdownNow();
                }
                Log.i(TAG, "WebServer monitor executor shutdown");
            } catch (Exception e) {
                Log.e(TAG, "Error shutting down WebServer monitor executor", e);
                webServerMonitorExecutor.shutdownNow();
            }
        }
    }

    /**
     * 앱 시작/재시작 시점에 포트 점유 상태(8080~8090) 및 WebServer 기동 여부를 로그로 남김
     * - 다른 프로세스가 포트를 점유 중인지 여부는 "사용 가능/불가"로만 판단 가능 (소유 프로세스 식별은 불가)
     */
    private void logWebServerStartupDiagnosticsAsync(String when) {
        try {
            Thread t = new Thread(() -> {
                try {
                    StringBuilder sb = new StringBuilder();
                    sb.append("WebServer startup diagnostics [").append(when).append("] ");
                    sb.append("pid=").append(android.os.Process.myPid());

                    // 현재 WebServer 인스턴스 상태 (초기에는 null일 수 있음)
                    boolean instanceExists = (webServer != null);
                    sb.append(", instance=").append(instanceExists ? "exists" : "null");
                    if (instanceExists) {
                        try {
                            sb.append(", running=").append(webServer.isRunning());
                            sb.append(", actualPort=").append(webServer.getActualPort());
                        } catch (Exception ignored) {
                            // ignore
                        }
                    }

                    // 포트 점유 상태 확인 (8080~8090)
                    sb.append(", ports=");
                    for (int port = 8080; port <= 8090; port++) {
                        boolean available = isPortAvailableForDiagnostics(port);
                        sb.append(port).append(available ? "(free)" : "(in_use)");
                        if (port < 8090) sb.append(",");
                    }

                    Log.i(TAG, sb.toString());
                } catch (Exception e) {
                    Log.w(TAG, "Failed to log WebServer startup diagnostics: " + e.getMessage());
                }
            }, "WebServer-Diag");
            t.setDaemon(true);
            t.start();
        } catch (Exception e) {
            Log.w(TAG, "Failed to start diagnostics thread: " + e.getMessage());
        }
    }

    private boolean isPortAvailableForDiagnostics(int port) {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (Exception ignored) {
                    // ignore
                }
            }
        }
    }

    /**
     * WebServer 인스턴스 반환
     */
    public itf.com.app.simple_line_test_ovio_new.util.WebServer getWebServer() {
        return webServer;
    }
    
    /**
     * Window와 DecorView가 준비되었는지 확인
     * ⚠️ 중요: Window 관련 작업 전에 반드시 호출하여 NullPointerException 방지
     */
    private boolean isWindowReady(Activity activity) {
        try {
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                return false;
            }
            
            if (activity.getWindow() == null) {
                return false;
            }
            
            View decorView = activity.getWindow().getDecorView();
            if (decorView == null) {
                return false;
            }
            
            // Android 11+ 에서는 WindowInsetsController도 확인
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsetsController controller = activity.getWindow().getInsetsController();
                if (controller == null) {
                    return false;
                }
            }
            
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Error checking window readiness: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 키오스크 모드 설정 (시스템 UI 완전히 숨기기)
     * Android 버전에 따라 다른 API 사용
     * ⚠️ 중요: 상단 스와이프로 시스템 UI가 나타나지 않도록 강력하게 설정
     * ⚠️ 중요: Window가 준비된 후에만 호출해야 함 (isWindowReady()로 확인)
     */
    private void setupKioskMode(Activity activity) {
        try {
            // Window 준비 상태 확인
            if (!isWindowReady(activity)) {
                Log.w(TAG, "Window not ready, skipping kiosk mode setup");
                return;
            }
            
            // Window 플래그 추가 설정
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            
            // Android 11 (API 30) 이상
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.getWindow().setDecorFitsSystemWindows(false);
                WindowInsetsController controller = activity.getWindow().getInsetsController();
                if (controller != null) {
                    // 상태바와 내비게이션 바 숨기기
                    controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                    // ⚠️ 중요: BEHAVIOR_DEFAULT 사용 (스와이프로 나타나지 않도록)
                    // BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE는 스와이프로 나타나게 함
                    controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_DEFAULT);
                }
            } else {
                // Android 10 이하
                View decorView = activity.getWindow().getDecorView();
                if (decorView != null) {
                    int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                    decorView.setSystemUiVisibility(uiOptions);
                    
                    // 시스템 UI가 다시 나타나는 것을 방지하는 리스너
                    decorView.setOnSystemUiVisibilityChangeListener(
                        new View.OnSystemUiVisibilityChangeListener() {
                            @Override
                            public void onSystemUiVisibilityChange(int visibility) {
                                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                                    // 시스템 UI가 다시 나타나면 즉시 숨기기
                                    if (isWindowReady(activity)) {
                                        setupKioskMode(activity);
                                    }
                                }
                            }
                        }
                    );
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up kiosk mode", e);
        }
    }
    
    /**
     * 키오스크 모드 지속적 모니터링 시작
     * 주기적으로 시스템 UI를 숨겨서 사용자가 접근할 수 없도록 함
     */
    private void startKioskModeMonitoring(Activity activity) {
        try {
            // 기존 모니터링이 있으면 정리
            stopKioskModeMonitoring();
            
            final Activity currentActivity = activity; // final 변수로 캡처
            kioskModeHandler = new Handler(Looper.getMainLooper());
            kioskModeRunnable = new Runnable() {
                @Override
                public void run() {
                    // Activity가 유효한지 확인
                    if (currentActivity != null && !currentActivity.isFinishing() && !currentActivity.isDestroyed()) {
                        // ⚠️ 중요: 주기적으로 시스템 UI 숨기기 (100ms마다 - 더 빠른 반응)
                        // 상단 스와이프로 시스템 UI가 나타나는 것을 즉시 차단
                        hideSystemUI(currentActivity);
                        
                        // 다음 실행 예약 (100ms 간격으로 더 자주 체크)
                        if (kioskModeHandler != null && kioskModeRunnable != null) {
                            kioskModeHandler.postDelayed(this, 100);
                        }
                    } else {
                        // Activity가 종료되었으면 모니터링 중지
                        stopKioskModeMonitoring();
                    }
                }
            };
            
            // 즉시 실행하고 주기적으로 반복
            kioskModeHandler.post(kioskModeRunnable);
            Log.d(TAG, "Kiosk mode monitoring started for: " + activity.getClass().getSimpleName());
        } catch (Exception e) {
            Log.e(TAG, "Error starting kiosk mode monitoring", e);
        }
    }
    
    /**
     * 키오스크 모드 지속적 모니터링 중지
     */
    private void stopKioskModeMonitoring() {
        try {
            if (kioskModeHandler != null && kioskModeRunnable != null) {
                kioskModeHandler.removeCallbacks(kioskModeRunnable);
                kioskModeRunnable = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping kiosk mode monitoring", e);
        }
    }
    
    /**
     * 시스템 UI 강제로 숨기기
     * 모든 Android 버전에서 작동하는 강력한 방법
     * ⚠️ 중요: 상단 스와이프로 시스템 UI가 나타나지 않도록 즉시 숨김
     * ⚠️ 중요: Window가 준비된 후에만 호출해야 함
     */
    private void hideSystemUI(Activity activity) {
        try {
            // Window 준비 상태 확인
            if (!isWindowReady(activity)) {
                return;
            }
            
            View decorView = activity.getWindow().getDecorView();
            if (decorView == null) {
                return;
            }
            
            // Android 11 (API 30) 이상
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsetsController controller = activity.getWindow().getInsetsController();
                if (controller != null) {
                    // 상태바와 내비게이션 바 즉시 숨기기
                    controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                    // BEHAVIOR_DEFAULT로 설정하여 스와이프로 나타나지 않도록 함
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
    
    /**
     * Screen Pinning 활성화 (앱 고정 모드)
     * Android 5.0 (API 21) 이상에서 지원
     * 주의: 사용자가 뒤로가기 + 최근 앱 버튼을 동시에 누르면 해제 가능
     */
    public static void enableScreenPinning(Activity activity) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // Android 6.0 이상에서는 상태 확인 후 호출
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    ActivityManager activityManager = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
                    if (activityManager != null) {
                        int lockTaskMode = activityManager.getLockTaskModeState();
                        // 이미 Lock Task 모드가 아닌 경우에만 시작
                        if (lockTaskMode == ActivityManager.LOCK_TASK_MODE_NONE) {
                            activity.startLockTask();
                            Log.i(TAG, "Screen pinning enabled");
                        } else {
                            Log.d(TAG, "Screen pinning already active (mode: " + lockTaskMode + ")");
                        }
                    }
                } else {
                    // Android 5.0-5.1: 직접 호출 (예외 처리로 안전하게)
                    activity.startLockTask();
                    Log.i(TAG, "Screen pinning enabled");
                }
            }
        } catch (SecurityException e) {
            // Lock Task 모드가 지원되지 않거나 권한이 없는 경우
            Log.w(TAG, "Lock task mode not supported or permission denied: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error enabling screen pinning", e);
        }
    }
    
    /**
     * Screen Pinning 비활성화
     */
    public static void disableScreenPinning(Activity activity) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                activity.stopLockTask();
                Log.i(TAG, "Screen pinning disabled");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error disabling screen pinning", e);
        }
    }
}

