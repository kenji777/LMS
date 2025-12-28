package itf.com.app.lms.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * USB 통신 인프라를 관리하는 싱글톤 클래스
 * ActivityModelTestProcess와 독립적으로 USB 명령을 전송하고 응답을 받을 수 있음
 */
public class UsbCommandManager {
    private static final String TAG = "UsbCommandManager";
    private static UsbCommandManager instance;
    
    private Context context;
    private UsbService usbService;
    private UsbCommandQueue usbCommandQueue;
    private UsbHandler usbHandler;
    private boolean isInitialized = false;
    
    // 응답 처리를 위한 Map과 Lock
    private final ConcurrentHashMap<String, CompletableFuture<String>> pendingUsbResponses = new ConcurrentHashMap<>();
    private final Object usbResponseLock = new Object();
    
    // ServiceConnection
    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                if (service instanceof UsbService.UsbBinder) {
                    UsbService.UsbBinder binder = (UsbService.UsbBinder) service;
                    usbService = binder.getService();
                    
                    if (usbService != null) {
                        // UsbHandler 생성 및 등록
                        if (usbHandler == null) {
                            usbHandler = new UsbHandler(UsbCommandManager.this);
                        }
                        usbService.setHandler(usbHandler);
                        
                        // 명령 큐에 UsbService 설정
                        if (usbCommandQueue != null) {
                            usbCommandQueue.setUsbService(usbService);
                        } else {
                            usbCommandQueue = new UsbCommandQueue();
                            usbCommandQueue.setUsbService(usbService);
                            usbCommandQueue.start();
                        }
                        
                        isInitialized = true;
                        Log.i(TAG, "USB service connected and initialized");
                    } else {
                        Log.e(TAG, "UsbService is null after binding");
                    }
                } else {
                    Log.e(TAG, "Invalid binder type. Expected UsbService.UsbBinder");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in onServiceConnected", e);
            }
        }
        
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.w(TAG, "USB service disconnected");
            usbService = null;
            isInitialized = false;
            
            // 모든 대기 중인 응답에 예외 발생
            synchronized (usbResponseLock) {
                for (CompletableFuture<String> future : pendingUsbResponses.values()) {
                    if (!future.isDone()) {
                        future.completeExceptionally(new RuntimeException("USB service disconnected"));
                    }
                }
                pendingUsbResponses.clear();
            }
        }
    };
    
    /**
     * 싱글톤 인스턴스 가져오기
     */
    public static synchronized UsbCommandManager getInstance() {
        if (instance == null) {
            instance = new UsbCommandManager();
        }
        return instance;
    }
    
    private UsbCommandManager() {
        // Private constructor for singleton
    }
    
    /**
     * USB 통신 초기화
     * @param context Application Context 또는 Activity Context
     */
    public void initialize(Context context) {
        if (isInitialized) {
            Log.w(TAG, "Already initialized");
            return;
        }
        
        this.context = context.getApplicationContext();
        
        // UsbService 바인딩
        Intent serviceIntent = new Intent(this.context, UsbService.class);
        this.context.startService(serviceIntent);
        this.context.bindService(serviceIntent, usbConnection, Context.BIND_AUTO_CREATE);
        
        Log.i(TAG, "USB command manager initialization started");
    }
    
    /**
     * USB 통신 종료
     */
    public void shutdown() {
        if (usbCommandQueue != null) {
            usbCommandQueue.stop();
            usbCommandQueue = null;
        }
        
        if (usbService != null && context != null) {
            try {
                context.unbindService(usbConnection);
            } catch (Exception e) {
                Log.e(TAG, "Error unbinding USB service", e);
            }
            usbService = null;
        }
        
        usbHandler = null;
        isInitialized = false;
        
        // 모든 대기 중인 응답에 예외 발생
        synchronized (usbResponseLock) {
            for (CompletableFuture<String> future : pendingUsbResponses.values()) {
                if (!future.isDone()) {
                    future.completeExceptionally(new RuntimeException("USB command manager shutdown"));
                }
            }
            pendingUsbResponses.clear();
        }
        
        Log.i(TAG, "USB command manager shutdown");
    }
    
    /**
     * USB 명령 전송 및 응답 대기
     * @param command PLC 명령 문자열
     * @param description 명령 설명
     * @param timeoutMs 타임아웃 (밀리초)
     * @return CompletableFuture<String> 응답 문자열
     */
    public CompletableFuture<String> sendUsbCommandWithResponse(String command, String description, long timeoutMs) {
        CompletableFuture<String> future = new CompletableFuture<>();
        
        if (!isInitialized || usbCommandQueue == null || !usbCommandQueue.isRunning()) {
            future.completeExceptionally(new RuntimeException("USB command queue is not running"));
            return future;
        }
        
        if (command == null || command.trim().isEmpty()) {
            future.completeExceptionally(new IllegalArgumentException("Command cannot be null or empty"));
            return future;
        }
        
        // 응답을 받기 위한 키 생성 (명령 + 타임스탬프)
        String responseKey = "resp_" + System.currentTimeMillis() + "_" + description.hashCode();
        
        // Future를 Map에 저장
        synchronized (usbResponseLock) {
            pendingUsbResponses.put(responseKey, future);
        }
        
        Runnable onSuccess = () -> {
            // 명령 전송 성공 (응답은 USB Handler에서 처리)
        };
        
        Runnable onError = () -> {
            synchronized (usbResponseLock) {
                pendingUsbResponses.remove(responseKey);
            }
            future.completeExceptionally(new RuntimeException("Failed to send command: " + description));
        };
        
        // 명령 전송
        UsbCommandQueue.UsbCommand usbCommand = new UsbCommandQueue.UsbCommand(
            UsbCommandQueue.CommandType.USER_COMMAND,
            command.getBytes(),
            description,
            onSuccess,
            onError
        );
        
        boolean sent = usbCommandQueue.enqueuePriority(usbCommand);
        if (!sent) {
            synchronized (usbResponseLock) {
                pendingUsbResponses.remove(responseKey);
            }
            future.completeExceptionally(new RuntimeException("Failed to enqueue command: " + description));
            return future;
        }
        
        // 타임아웃 설정
        final String finalResponseKey = responseKey;
        new Thread(() -> {
            try {
                Thread.sleep(timeoutMs);
                synchronized (usbResponseLock) {
                    CompletableFuture<String> removed = pendingUsbResponses.remove(finalResponseKey);
                    if (removed != null && !removed.isDone()) {
                        removed.completeExceptionally(new java.util.concurrent.TimeoutException("Response timeout for: " + description));
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                synchronized (usbResponseLock) {
                    CompletableFuture<String> removed = pendingUsbResponses.remove(finalResponseKey);
                    if (removed != null && !removed.isDone()) {
                        removed.completeExceptionally(e);
                    }
                }
            }
        }).start();
        
        return future;
    }
    
    /**
     * USB 응답 처리 (USB Handler에서 호출)
     * @param response USB 응답 문자열
     */
    private void handleUsbResponse(String response) {
        if (response == null || response.isEmpty()) {
            return;
        }
        
        synchronized (usbResponseLock) {
            if (!pendingUsbResponses.isEmpty()) {
                // 가장 오래된 응답부터 처리 (FIFO)
                String oldestKey = null;
                long oldestTime = Long.MAX_VALUE;
                for (String key : pendingUsbResponses.keySet()) {
                    try {
                        String[] parts = key.split("_");
                        if (parts.length > 1) {
                            long timestamp = Long.parseLong(parts[1]);
                            if (timestamp < oldestTime) {
                                oldestTime = timestamp;
                                oldestKey = key;
                            }
                        }
                    } catch (Exception e) {
                        // 파싱 실패 시 무시
                    }
                }
                
                if (oldestKey != null) {
                    CompletableFuture<String> future = pendingUsbResponses.remove(oldestKey);
                    if (future != null && !future.isDone()) {
                        future.complete(response);
                    }
                }
            }
        }
    }
    
    /**
     * 초기화 상태 확인
     */
    public boolean isInitialized() {
        return isInitialized && usbCommandQueue != null && usbCommandQueue.isRunning();
    }
    
    /**
     * USB Handler 클래스
     * UsbService로부터 받은 메시지를 처리
     */
    private static class UsbHandler extends Handler {
        private final WeakReference<UsbCommandManager> managerRef;
        
        public UsbHandler(UsbCommandManager manager) {
            super(Looper.getMainLooper());
            this.managerRef = new WeakReference<>(manager);
        }
        
        @Override
        public void handleMessage(Message msg) {
            UsbCommandManager manager = managerRef.get();
            if (manager == null) {
                return;
            }
            
            switch (msg.what) {
                case UsbService.MESSAGE_FROM_SERIAL_PORT:
                    try {
                        String data = (String) msg.obj;
                        if (data != null && !data.isEmpty()) {
                            // 응답 수신 알림 (명령 큐의 응답 대기 상태 해제)
                            if (manager.usbCommandQueue != null) {
                                manager.usbCommandQueue.notifyResponseReceived();
                            }
                            
                            // USB 응답 처리
                            manager.handleUsbResponse(data);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing USB message", e);
                    }
                    break;
                case UsbService.CTS_CHANGE:
                    // CTS 변경 이벤트 (필요시 처리)
                    break;
                case UsbService.DSR_CHANGE:
                    // DSR 변경 이벤트 (필요시 처리)
                    break;
            }
        }
    }
}

