package itf.com.app.lms.util;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PLC 메모리 스캔 유틸리티
 * PLC의 전체 메모리를 스캔하여 데이터가 저장된 메모리만 읽어옴
 */
public class PlcMemoryScanner {
    private static final String TAG = "PlcMemoryScanner";
    
    /**
     * 메모리 타입
     */
    public enum MemoryType {
        DW("DW", "Double Word"),  // 32비트
        DM("DM", "Data Memory"),  // 16비트
        D("D", "Data Register"), // 16비트
        W("W", "Word"),          // 16비트
        R("R", "Relay");         // 비트
        
        private final String code;
        private final String description;
        
        MemoryType(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 메모리 주소 정보
     */
    public static class MemoryAddress {
        public final MemoryType type;
        public final int address;
        public final String addressString;
        
        public MemoryAddress(MemoryType type, int address) {
            this.type = type;
            this.address = address;
            this.addressString = type.getCode() + address;
        }
        
        @Override
        public String toString() {
            return addressString;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            MemoryAddress that = (MemoryAddress) obj;
            return type == that.type && address == that.address;
        }
        
        @Override
        public int hashCode() {
            return type.hashCode() * 31 + address;
        }
    }
    
    /**
     * 메모리 데이터 정보
     */
    public static class MemoryData {
        public final MemoryAddress address;
        public final String rawValue;      // 원시 응답 값
        public final int intValue;         // 정수 값
        public final String hexValue;       // 16진수 값
        public final String binaryValue;   // 2진수 값
        
        public MemoryData(MemoryAddress address, String rawValue, int intValue) {
            this.address = address;
            this.rawValue = rawValue;
            this.intValue = intValue;
            this.hexValue = String.format("%04X", intValue & 0xFFFF);
            this.binaryValue = Integer.toBinaryString(intValue);
        }
        
        @Override
        public String toString() {
            return String.format("%s: %d (0x%s)", address, intValue, hexValue);
        }
    }
    
    /**
     * 스캔 결과
     */
    public static class ScanResult {
        public final MemoryType memoryType;
        public final int startAddress;
        public final int endAddress;
        public final List<MemoryData> memoryDataList;
        public final int totalScanned;
        public final int dataFound;
        public final long scanTimeMs;
        
        public ScanResult(MemoryType memoryType, int startAddress, int endAddress,
                         List<MemoryData> memoryDataList, int totalScanned, long scanTimeMs) {
            this.memoryType = memoryType;
            this.startAddress = startAddress;
            this.endAddress = endAddress;
            this.memoryDataList = memoryDataList;
            this.totalScanned = totalScanned;
            this.dataFound = memoryDataList != null ? memoryDataList.size() : 0;
            this.scanTimeMs = scanTimeMs;
        }
    }
    
    /**
     * 진행 상황 콜백
     */
    public interface ProgressCallback {
        void onProgress(int current, int total, String message);
        void onComplete(String message);
        void onError(String error);
    }
    
    /**
     * PLC 명령 전송 인터페이스
     */
    public interface PlcCommandSender {
        CompletableFuture<String> sendCommand(byte[] command, String description);
    }
    
    /**
     * PLC 메모리 읽기 명령 생성
     * @param memoryType 메모리 타입
     * @param address 주소
     * @return 명령 바이트 배열
     */
    public static byte[] createReadCommand(MemoryType memoryType, int address) {
        // 명령 형식: STX(0x05) + "00RSS0107" + "%" + 메모리타입 + 주소 + ETX(0x04)
        // 예: "\u000500RSS0107%DW1006\u0004"
        String commandStr = String.format("\u000500RSS0107%%%s%d\u0004", 
                                         memoryType.getCode(), address);
        Log.i(TAG, "> createReadCommand.commandStr " + commandStr);
        return commandStr.getBytes();
    }
    
    /**
     * PLC 응답 파싱
     * @param response PLC 응답 문자열
     * @return 파싱된 값 (16진수 문자열)
     */
    public static String parseResponse(String response) {
        if (response == null || response.isEmpty()) {
            return null;
        }
        
        // 응답 형식: "00RSS0102" + 4자리 16진수
        // 예: "00RSS0102000C"
        Pattern pattern = Pattern.compile("00RSS0102([0-9A-Fa-f]{4})");
        Matcher matcher = pattern.matcher(response);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
    
    /**
     * 16진수 문자열을 정수로 변환
     */
    public static int hexToInt(String hex) {
        try {
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            Log.w(TAG, "Failed to parse hex: " + hex);
            return 0;
        }
    }
    
    /**
     * 메모리 스캔 실행
     * @param sender 명령 전송 인터페이스
     * @param memoryType 메모리 타입
     * @param startAddress 시작 주소
     * @param endAddress 종료 주소
     * @param progressCallback 진행 상황 콜백
     * @return 스캔 결과
     */
    public static CompletableFuture<ScanResult> scanMemory(
            PlcCommandSender sender,
            MemoryType memoryType,
            int startAddress,
            int endAddress,
            ProgressCallback progressCallback) {
        
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            List<MemoryData> memoryDataList = new ArrayList<>();
            int totalScanned = 0;
            
            try {
                if (progressCallback != null) {
                    progressCallback.onProgress(0, endAddress - startAddress + 1, 
                                              "메모리 스캔 시작: " + memoryType.getDescription());
                }
                
                for (int addr = startAddress; addr <= endAddress; addr++) {
                    totalScanned++;
                    
                    try {
                        // 명령 생성 및 전송
                        byte[] command = createReadCommand(memoryType, addr);
                        String description = String.format("Read %s%d", memoryType.getCode(), addr);
                        
                        CompletableFuture<String> responseFuture = sender.sendCommand(command, description);
                        String response = responseFuture.get(3, TimeUnit.SECONDS);
                        
                        if (response != null && !response.isEmpty()) {
                            // 응답 파싱
                            String hexValue = parseResponse(response);
                            if (hexValue != null) {
                                int intValue = hexToInt(hexValue);
                                
                                // 데이터가 0이 아닌 경우만 저장 (또는 모든 데이터 저장)
                                MemoryAddress memAddr = new MemoryAddress(memoryType, addr);
                                MemoryData memData = new MemoryData(memAddr, response, intValue);
                                memoryDataList.add(memData);
                                
                                Log.d(TAG, String.format("Found data at %s%d: %d (0x%s)", 
                                                         memoryType.getCode(), addr, intValue, hexValue));
                            }
                        }
                        
                        // 진행 상황 업데이트
                        if (progressCallback != null && (addr % 10 == 0 || addr == endAddress)) {
                            int progress = addr - startAddress + 1;
                            int total = endAddress - startAddress + 1;
                            String message = String.format("스캔 중... %d/%d (발견: %d)", 
                                                          progress, total, memoryDataList.size());
                            progressCallback.onProgress(progress, total, message);
                        }
                        
                        // 스캔 속도 조절 (너무 빠르면 PLC가 따라가지 못할 수 있음)
                        Thread.sleep(50); // 50ms 대기
                        
                    } catch (Exception e) {
                        Log.w(TAG, "Error reading address " + addr + ": " + e.getMessage());
                        // 에러가 발생해도 계속 진행
                    }
                }
                
                long scanTime = System.currentTimeMillis() - startTime;
                
                if (progressCallback != null) {
                    String message = String.format("스캔 완료: %d개 주소 중 %d개 데이터 발견 (소요 시간: %.2f초)",
                                                  totalScanned, memoryDataList.size(), scanTime / 1000.0);
                    progressCallback.onComplete(message);
                }
                
                return new ScanResult(memoryType, startAddress, endAddress, 
                                    memoryDataList, totalScanned, scanTime);
                
            } catch (Exception e) {
                Log.e(TAG, "Memory scan failed", e);
                if (progressCallback != null) {
                    progressCallback.onError("스캔 실패: " + e.getMessage());
                }
                throw new RuntimeException("Memory scan failed", e);
            }
        });
    }
    
    /**
     * 빠른 스캔 (데이터가 있는 주소만 찾기)
     * @param sender 명령 전송 인터페이스
     * @param memoryType 메모리 타입
     * @param startAddress 시작 주소
     * @param endAddress 종료 주소
     * @param progressCallback 진행 상황 콜백
     * @return 데이터가 있는 주소 리스트
     */
    public static CompletableFuture<List<MemoryAddress>> quickScan(
            PlcCommandSender sender,
            MemoryType memoryType,
            int startAddress,
            int endAddress,
            ProgressCallback progressCallback) {
        
        return scanMemory(sender, memoryType, startAddress, endAddress, progressCallback)
            .thenApply(result -> {
                List<MemoryAddress> addresses = new ArrayList<>();
                for (MemoryData data : result.memoryDataList) {
                    addresses.add(data.address);
                }
                return addresses;
            });
    }
    
    /**
     * 특정 주소 리스트의 메모리 데이터 읽기
     * @param sender 명령 전송 인터페이스
     * @param addresses 읽을 메모리 주소 리스트
     * @param progressCallback 진행 상황 콜백
     * @return 메모리 데이터 리스트
     */
    public static CompletableFuture<List<MemoryData>> readMemoryData(
            PlcCommandSender sender,
            List<MemoryAddress> addresses,
            ProgressCallback progressCallback) {
        
        return CompletableFuture.supplyAsync(() -> {
            List<MemoryData> memoryDataList = new ArrayList<>();
            int total = addresses.size();
            
            try {
                if (progressCallback != null) {
                    progressCallback.onProgress(0, total, "메모리 데이터 읽기 시작");
                }
                
                for (int i = 0; i < addresses.size(); i++) {
                    MemoryAddress addr = addresses.get(i);
                    
                    try {
                        byte[] command = createReadCommand(addr.type, addr.address);
                        String description = "Read " + addr.addressString;
                        
                        CompletableFuture<String> responseFuture = sender.sendCommand(command, description);
                        String response = responseFuture.get(3, TimeUnit.SECONDS);
                        
                        if (response != null && !response.isEmpty()) {
                            String hexValue = parseResponse(response);
                            if (hexValue != null) {
                                int intValue = hexToInt(hexValue);
                                MemoryData memData = new MemoryData(addr, response, intValue);
                                memoryDataList.add(memData);
                            }
                        }
                        
                        if (progressCallback != null) {
                            progressCallback.onProgress(i + 1, total, String.format("읽기 중... %d/%d", i + 1, total));
                        }
                        
                        Thread.sleep(50);
                        
                    } catch (Exception e) {
                        Log.w(TAG, "Error reading " + addr + ": " + e.getMessage());
                    }
                }
                
                if (progressCallback != null) {
                    progressCallback.onComplete("데이터 읽기 완료: " + memoryDataList.size() + "개");
                }
                
                return memoryDataList;
                
            } catch (Exception e) {
                Log.e(TAG, "Read memory data failed", e);
                if (progressCallback != null) {
                    progressCallback.onError("읽기 실패: " + e.getMessage());
                }
                throw new RuntimeException("Read memory data failed", e);
            }
        });
    }
}

