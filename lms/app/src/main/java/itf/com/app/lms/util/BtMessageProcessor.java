package itf.com.app.lms.util;

import android.content.Context;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 블루투스 메시지 처리 최적화 클래스
 * 
 * 주요 기능:
 * 1. 스펙 데이터 사전 로딩 및 캐시 관리
 * 2. 메시지 처리 직렬화
 * 3. UI 업데이트 번들화 지원
 */
public class BtMessageProcessor {
    private static final String TAG = "BtMessageProcessor";
    
    // 스펙 데이터 캐시 (command -> spec data)
    private final Map<String, Map<String, String>> specCache = new ConcurrentHashMap<>();
    
    // 메시지 처리 큐
    private final LinkedBlockingQueue<BtMessage> messageQueue = new LinkedBlockingQueue<>();
    
    // 메시지 처리 Executor (단일 스레드로 직렬화)
    private final ExecutorService messageProcessor = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "BtMessageProcessor");
            thread.setPriority(Thread.NORM_PRIORITY - 1);
            return thread;
        }
    });
    
    // 처리 중 플래그
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    
    // Context (DB 조회용)
    private final Context context;
    
    // 모델 ID (스펙 데이터 로딩용)
    private String modelId;
    
    // 메시지 처리 리스너
    public interface MessageProcessListener {
        /**
         * 메시지 처리 완료 시 호출
         * @param message 처리된 메시지
         * @param specData 조회된 스펙 데이터 (null일 수 있음)
         */
        void onMessageProcessed(BtMessage message, Map<String, String> specData);
        
        /**
         * UI 업데이트 번들을 반환
         * @param message 처리된 메시지
         * @param specData 조회된 스펙 데이터
         * @return UI 업데이트 번들 (null 가능)
         */
        UiUpdateBundle createUpdateBundle(BtMessage message, Map<String, String> specData);
    }
    
    private MessageProcessListener listener;
    
    /**
     * 블루투스 메시지 데이터 클래스
     */
    public static class BtMessage {
        public final byte[] rawData;
        public final String readMessage;
        public final String receiveCommand;
        public final String receiveCommandResponse;
        public final long timestamp;
        
        public BtMessage(byte[] rawData, String readMessage, String receiveCommand, String receiveCommandResponse) {
            this.rawData = rawData;
            this.readMessage = readMessage;
            this.receiveCommand = receiveCommand;
            this.receiveCommandResponse = receiveCommandResponse;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    
    /**
     * Constructor
     * ⚠️ IMPORTANT: Pass ApplicationContext to prevent memory leaks, not Activity context
     * @param context Application context (use context.getApplicationContext())
     */
    public BtMessageProcessor(Context context) {
        this.context = context;
        startMessageProcessor();
    }
    
    /**
     * 모델 ID 설정 및 스펙 데이터 사전 로딩
     */
    public void loadSpecDataForModel(String modelId) {
        this.modelId = modelId;
        messageProcessor.execute(() -> {
            try {
                Log.i(TAG, "Loading all test spec data for model: " + modelId);
                String queryCondition = Constants.Database.QUERY_AND + " " + 
                    Constants.JsonKeys.CLM_MODEL_ID + Constants.Common.EQUAL + 
                    Constants.Common.SINGLE_QUETATION + modelId + Constants.Common.SINGLE_QUETATION;
                
                List<Map<String, String>> allSpecs = TestData.selectTestSpecData(context, queryCondition);
                
                if (allSpecs != null && !allSpecs.isEmpty()) {
                    specCache.clear();
                    for (Map<String, String> spec : allSpecs) {
                        String command = spec.get(Constants.JsonKeys.CLM_TEST_COMMAND);
                        if (command != null && !command.isEmpty()) {
                            specCache.put(command, new HashMap<>(spec));
                        }
                    }
                    Log.i(TAG, "Loaded " + specCache.size() + " test spec entries into cache");
                } else {
                    Log.w(TAG, "No test spec data found for model: " + modelId);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading test spec data", e);
            }
        });
    }
    
    /**
     * 스펙 데이터 조회 (캐시만 사용, DB 조회 없음)
     */
    public Map<String, String> getSpecData(String command) {
        if (command == null || command.trim().isEmpty()) {
            return null;
        }
        
        // 캐시에서 직접 조회
        Map<String, String> cached = specCache.get(command);
        if (cached != null) {
            return new HashMap<>(cached); // 방어적 복사
        }
        
        // 캐시 미스 시 로그만 남기고 null 반환
        // (DB 조회는 하지 않음 - 성능 최적화)
        Log.w(TAG, "Spec data not found in cache for command: " + command);
        return null;
    }
    
    /**
     * 메시지 처리 리스너 설정
     */
    public void setMessageProcessListener(MessageProcessListener listener) {
        this.listener = listener;
    }
    
    /**
     * 메시지 큐에 추가
     */
    public void enqueueMessage(byte[] rawData, String readMessage, String receiveCommand, String receiveCommandResponse) {
        BtMessage message = new BtMessage(rawData, readMessage, receiveCommand, receiveCommandResponse);
        try {
            messageQueue.put(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e(TAG, "Interrupted while enqueueing message", e);
        }
    }
    
    /**
     * 메시지 처리기 시작
     */
    private void startMessageProcessor() {
        messageProcessor.execute(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    BtMessage message = messageQueue.take(); // 블로킹 대기
                    processMessage(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "Error processing message", e);
                }
            }
        });
    }
    
    /**
     * 개별 메시지 처리
     */
    private void processMessage(BtMessage message) {
        isProcessing.set(true);
        try {
            // 스펙 데이터 조회 (캐시만 사용)
            Map<String, String> specData = getSpecData(message.receiveCommand);
            
            // 리스너에게 처리 완료 알림
            if (listener != null) {
                listener.onMessageProcessed(message, specData);
            }
        } finally {
            isProcessing.set(false);
        }
    }
    
    /**
     * 캐시에 스펙 데이터 추가 (동적 업데이트용)
     */
    public void updateSpecCache(String command, Map<String, String> specData) {
        if (command != null && specData != null) {
            specCache.put(command, new HashMap<>(specData));
        }
    }
    
    /**
     * 캐시 초기화
     */
    public void clearCache() {
        specCache.clear();
    }
    
    /**
     * 현재 큐에 대기 중인 메시지 수
     */
    public int getQueueSize() {
        return messageQueue.size();
    }
    
    /**
        // ⚠️ CRITICAL FIX: Clear listener reference to prevent memory leak
        listener = null;
     * 리소스 정리
     */
    public void shutdown() {
        messageProcessor.shutdown();
        messageQueue.clear();
        specCache.clear();
        // ⚠️ CRITICAL FIX: Clear listener reference to prevent memory leak
        listener = null;
    }
    
    /**
     * 캐시 통계 정보
     */
    public String getCacheStats() {
        return String.format("Cache size: %d, Queue size: %d, Processing: %s", 
            specCache.size(), messageQueue.size(), isProcessing.get());
    }
}

