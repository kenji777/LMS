package itf.com.app.lms.util;

import android.util.Log;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * USB 시리얼 통신 명령 큐 관리 클래스
 * 시리얼 통신 충돌 방지를 위해 모든 명령을 순차적으로 처리
 */
public class UsbCommandQueue {
    private static final String TAG = "UsbCommandQueue";
    
    /**
     * 명령 타입
     */
    public enum CommandType {
        POLLING,        // 소비전력 폴링 명령 (우선순위 낮음)
        USER_COMMAND,   // 사용자 명령 (우선순위 높음)
        SYSTEM          // 시스템 명령
    }
    
    /**
     * USB 명령 정보
     */
    public static class UsbCommand {
        public final CommandType type;
        public final byte[] data;
        public final String description;
        public final long timestamp;
        public final Runnable onSuccess;
        public final Runnable onError;
        
        public UsbCommand(CommandType type, byte[] data, String description, 
                         Runnable onSuccess, Runnable onError) {
            this.type = type;
            this.data = data;
            this.description = description;
            this.timestamp = System.currentTimeMillis();
            this.onSuccess = onSuccess;
            this.onError = onError;
        }
        
        public UsbCommand(CommandType type, String command, String description) {
            this(type, command.getBytes(), description, null, null);
        }
    }
    
    private final BlockingQueue<UsbCommand> commandQueue = new LinkedBlockingQueue<>();
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private Thread commandProcessorThread = null;
    private UsbService usbService = null;
    
    // 응답 대기 관련
    private static final long RESPONSE_TIMEOUT_MS = 2000; // 2초 타임아웃
    private volatile boolean waitingForResponse = false;
    private volatile long lastCommandTime = 0;
    
    /**
     * UsbService 설정
     */
    public void setUsbService(UsbService service) {
        this.usbService = service;
    }
    
    /**
     * 명령 큐 시작
     */
    public synchronized void start() {
        if (isRunning.get()) {
            Log.w(TAG, "Command queue is already running");
            return;
        }
        
        isRunning.set(true);
        commandProcessorThread = new Thread(this::processCommands, "UsbCommandProcessor");
        commandProcessorThread.setDaemon(true);
        commandProcessorThread.start();
        Log.i(TAG, "USB command queue started");
    }
    
    /**
     * 명령 큐 중지
     */
    public synchronized void stop() {
        if (!isRunning.get()) {
            return;
        }
        
        isRunning.set(false);
        commandQueue.clear();
        
        if (commandProcessorThread != null) {
            commandProcessorThread.interrupt();
            try {
                commandProcessorThread.join(1000);
            } catch (InterruptedException e) {
                Log.w(TAG, "Interrupted while waiting for command processor thread to stop");
            }
            commandProcessorThread = null;
        }
        
        Log.i(TAG, "USB command queue stopped");
    }
    
    /**
     * 명령 추가 (비동기)
     * @param command 명령
     * @return true if added successfully
     */
    public boolean enqueue(UsbCommand command) {
        if (!isRunning.get()) {
            Log.w(TAG, "Command queue is not running, cannot enqueue: " + command.description);
            return false;
        }
        
        if (command == null || command.data == null) {
            Log.w(TAG, "Invalid command, cannot enqueue");
            return false;
        }
        
        try {
            boolean added = commandQueue.offer(command, 100, TimeUnit.MILLISECONDS);
            if (added) {
                Log.d(TAG, "Command enqueued: " + command.description + " (Type: " + command.type + ", Queue size: " + commandQueue.size() + ")");
            } else {
                Log.w(TAG, "Failed to enqueue command (queue full): " + command.description);
            }
            return added;
        } catch (InterruptedException e) {
            Log.w(TAG, "Interrupted while enqueueing command: " + command.description);
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    /**
     * 명령 추가 (동기, 우선순위 높음)
     * 사용자 명령 등 즉시 처리해야 하는 경우 사용
     */
    public boolean enqueuePriority(UsbCommand command) {
        if (!isRunning.get()) {
            Log.w(TAG, "Command queue is not running, cannot enqueue priority command: " + command.description);
            return false;
        }
        
        if (command == null || command.data == null) {
            Log.w(TAG, "Invalid command, cannot enqueue");
            return false;
        }
        
        // 큐의 앞부분에 추가하기 위해 큐를 재구성
        // LinkedBlockingQueue는 직접 앞에 삽입할 수 없으므로, 
        // 대신 큐를 비우고 우선순위 명령을 먼저 넣은 후 나머지를 다시 넣음
        try {
            BlockingQueue<UsbCommand> tempQueue = new LinkedBlockingQueue<>();
            tempQueue.offer(command);
            commandQueue.drainTo(tempQueue);
            commandQueue.addAll(tempQueue);
            
            Log.d(TAG, "Priority command enqueued: " + command.description + " (Queue size: " + commandQueue.size() + ")");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to enqueue priority command: " + command.description, e);
            return false;
        }
    }
    
    /**
     * 명령 처리 스레드
     */
    private void processCommands() {
        Log.i(TAG, "Command processor thread started");
        
        while (isRunning.get() && !Thread.currentThread().isInterrupted()) {
            try {
                UsbCommand command = commandQueue.take(); // 큐에서 명령 가져오기 (블로킹)
                
                if (!isRunning.get()) {
                    break;
                }
                
                // 응답 대기 중이면 잠시 대기
                if (waitingForResponse) {
                    long elapsed = System.currentTimeMillis() - lastCommandTime;
                    if (elapsed < RESPONSE_TIMEOUT_MS) {
                        long waitTime = RESPONSE_TIMEOUT_MS - elapsed;
                        Log.d(TAG, "Waiting for previous response, sleeping " + waitTime + " ms");
                        Thread.sleep(waitTime);
                    }
                }
                
                // 명령 전송
                if (usbService != null) {
                    isProcessing.set(true);
                    waitingForResponse = true;
                    lastCommandTime = System.currentTimeMillis();
                    
                    Log.d(TAG, "Sending command: " + command.description + " (Type: " + command.type + ")");
                    usbService.write(command.data);
                    
                    // 성공 콜백 실행
                    if (command.onSuccess != null) {
                        try {
                            command.onSuccess.run();
                        } catch (Exception e) {
                            Log.e(TAG, "Error in onSuccess callback for: " + command.description, e);
                        }
                    }
                    
                    // 응답 대기 시간 (명령 타입에 따라 다르게)
                    long responseWaitTime = (command.type == CommandType.POLLING) ? 100 : 200;
                    Thread.sleep(responseWaitTime);
                    
                } else {
                    Log.w(TAG, "UsbService is null, cannot send command: " + command.description);
                    if (command.onError != null) {
                        try {
                            command.onError.run();
                        } catch (Exception e) {
                            Log.e(TAG, "Error in onError callback for: " + command.description, e);
                        }
                    }
                }
                
                isProcessing.set(false);
                
            } catch (InterruptedException e) {
                Log.i(TAG, "Command processor thread interrupted");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                Log.e(TAG, "Error processing command", e);
                isProcessing.set(false);
                waitingForResponse = false;
            }
        }
        
        Log.i(TAG, "Command processor thread stopped");
    }
    
    /**
     * 응답 수신 알림 (응답 대기 상태 해제)
     */
    public void notifyResponseReceived() {
        waitingForResponse = false;
    }
    
    /**
     * 큐 크기 반환
     */
    public int getQueueSize() {
        return commandQueue.size();
    }
    
    /**
     * 큐 비우기
     */
    public void clear() {
        commandQueue.clear();
        Log.d(TAG, "Command queue cleared");
    }
    
    /**
     * 처리 중인지 확인
     */
    public boolean isProcessing() {
        return isProcessing.get();
    }
    
    /**
     * 실행 중인지 확인
     */
    public boolean isRunning() {
        return isRunning.get();
    }
}


