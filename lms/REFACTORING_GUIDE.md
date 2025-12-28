# ActivityModelTestProcess 리팩토링 가이드

## 개요
ActivityModelTestProcess.java (10,495줄)를 기능별로 분리하여 유지보수성을 향상시키기 위한 리팩토링 가이드입니다.

---

## 생성된 핵심 클래스 목록

### 1. 통신 관리자 (Communication Managers)

#### 1.1 UsbConnectionManager
**위치**: `app/src/main/java/itf/com/app/lms/conn/usb/UsbConnectionManager.java`

**주요 기능**:
- USB 장치 검색 및 권한 처리
- USB 서비스 연결 관리
- USB 폴링 관리 (주기적 데이터 수신)
- USB 명령 큐 관리
- USB 재연결 로직
- USB 응답 처리

**주요 메서드**:
- `initialize()`: USB 매니저 초기화
- `startUsbSearch()`: USB 장치 검색 시작
- `startUsbPolling(boolean immediate)`: USB 폴링 시작
- `stopUsbPolling()`: USB 폴링 중지
- `sendUsbCommand(String command, String description, Runnable onSuccess, Runnable onError)`: USB 명령 전송
- `sendUsbCommandWithResponse(String command, String description, long timeoutMs)`: 응답을 받는 USB 명령 전송
- `handleUsbResponse(String response)`: USB 응답 처리
- `scheduleUsbReconnect(boolean immediate)`: USB 재연결 스케줄링
- `cleanup()`: 리소스 정리 (onDestroy에서 호출 필수)

**ActivityModelTestProcess.java에 적용할 내용**:
```java
// 필드 선언
private UsbConnectionManager usbConnectionManager;

// onCreate에서 초기화
usbConnectionManager = new UsbConnectionManager(getApplicationContext(), new UsbConnectionManager.UsbListener() {
    @Override
    public void onUsbResponseReceived(String response) {
        // 기존 handleUsbResponse 로직
    }
    
    @Override
    public void onUsbConnectionStateChanged(ConnectionState state) {
        // 연결 상태 변경 처리
    }
    
    @Override
    public void onUsbError(UsbError error, String message) {
        // 에러 처리
    }
    
    @Override
    public void onUsbPermissionRequired() {
        // 권한 요청 처리
    }
    
    @Override
    public void onUsbDataReceived(String data, int electricValue) {
        // 전력 데이터 수신 처리
    }
});
usbConnectionManager.initialize();

// 기존 메서드들을 UsbConnectionManager로 위임
// usbSearch() → usbConnectionManager.startUsbSearch()
// startUsbPolling() → usbConnectionManager.startUsbPolling()
// stopUsbPolling() → usbConnectionManager.stopUsbPolling()
// handleUsbResponse() → usbConnectionManager.handleUsbResponse()
// sendUsbCommand() → usbConnectionManager.sendUsbCommand()

// onDestroy에서 cleanup
usbConnectionManager.cleanup();
```

**제거/대체할 메서드**:
- `usbSearch()` → `usbConnectionManager.startUsbSearch()`
- `startUsbPolling(boolean immediate)` → `usbConnectionManager.startUsbPolling(immediate)`
- `stopUsbPolling()` → `usbConnectionManager.stopUsbPolling()`
- `handleUsbResponse(String response)` → `usbConnectionManager.handleUsbResponse(response)`
- `sendUsbCommand(...)` → `usbConnectionManager.sendUsbCommand(...)`
- `scheduleUsbReconnect(...)` → `usbConnectionManager.scheduleUsbReconnect(...)`

---

### 2. 서비스 관리자 (Service Managers)

#### 2.1 TimerManager
**위치**: `app/src/main/java/itf/com/app/lms/managers/TimerManager.java`

**주요 기능**:
- 모든 타이머 통합 관리
- 타이머 메모리 누수 방지
- 스레드 안전한 타이머 작업
- 타이머 일괄 중지

**주요 메서드**:
- `startBtMessageTimer(TimerTask task, long intervalMs)`: BT 메시지 타이머 시작
- `stopBtMessageTimer()`: BT 메시지 타이머 중지
- `startResetTimer(TimerTask task, long intervalMs)`: 리셋 타이머 시작
- `cancelResetTimer()`: 리셋 타이머 취소
- `startFinishedRestartTimer(...)`: 완료 재시작 타이머 시작
- `stopFinishedRestartTimer()`: 완료 재시작 타이머 중지
- `startUnfinishedRestartTimer(...)`: 미완료 재시작 타이머 시작
- `stopUnfinishedRestartTimer()`: 미완료 재시작 타이머 중지
- `startRemoteCommandTimer(...)`: 원격 명령 타이머 시작
- `stopRemoteCommandTimer()`: 원격 명령 타이머 중지
- `startCheckDurationTimer(...)`: 지속 시간 체크 타이머 시작
- `stopCheckDurationTimer()`: 지속 시간 체크 타이머 중지
- `startBarcodeRequestTimer(...)`: 바코드 요청 타이머 시작
- `stopBarcodeRequestTimer()`: 바코드 요청 타이머 중지
- `startControlResponseTimeoutTimer(...)`: 제어 응답 타임아웃 타이머 시작
- `stopControlResponseTimeoutTimer()`: 제어 응답 타임아웃 타이머 중지
- `startControlTestTimer(...)`: 제어 테스트 타이머 시작
- `stopControlTestTimer()`: 제어 테스트 타이머 중지
- `stopAllTimers()`: 모든 타이머 중지 (onDestroy에서 호출 필수)

**ActivityModelTestProcess.java에 적용할 내용**:
```java
// 필드 선언
private TimerManager timerManager;

// onCreate에서 초기화 (이미 initializeServiceManagers에서 초기화됨)

// 기존 타이머 메서드들을 TimerManager로 위임
// startBtMessageTimer() → timerManager.startBtMessageTimer(task, interval)
// stopBtMessageTimer() → timerManager.stopBtMessageTimer()
// startResetTimer() → timerManager.startResetTimer(task, interval)
// cancelResetTimer() → timerManager.cancelResetTimer()
// stopAllTimers() → timerManager.stopAllTimers()

// onDestroy에서 cleanup
timerManager.stopAllTimers();
```

**제거/대체할 메서드**:
- `startBtMessageTimer()` → `timerManager.startBtMessageTimer(...)`
- `stopBtMessageTimer()` → `timerManager.stopBtMessageTimer()`
- `startResetTimer()` → `timerManager.startResetTimer(...)`
- `cancelResetTimer()` → `timerManager.cancelResetTimer()`
- `stopFinishedRestartTimer()` → `timerManager.stopFinishedRestartTimer()`
- `stopUnfinishedRestartTimer()` → `timerManager.stopUnfinishedRestartTimer()`
- `stopRemoteCommandTimer()` → `timerManager.stopRemoteCommandTimer()`
- `stopCheckDurationTimer()` → `timerManager.stopCheckDurationTimer()`
- `stopBarcodeRequestTimer()` → `timerManager.stopBarcodeRequestTimer()`
- `stopAppResetTimerTask()` → `timerManager.stopAppResetTimerTask()`
- `stopControlResponseTimeout()` → `timerManager.stopControlResponseTimeoutTimer()`
- `stopControlTestTimer()` → `timerManager.stopControlTestTimer()`
- `cleanupAllTimers()` → `timerManager.stopAllTimers()`

---

#### 2.2 PermissionManager
**위치**: `app/src/main/java/itf/com/app/lms/managers/PermissionManager.java`

**주요 기능**:
- 런타임 권한 체크 및 요청
- 권한 결과 처리
- 영구 거부된 권한 처리
- 앱 설정 열기

**주요 메서드**:
- `checkAndRequestPermissionsAsync(List<String> requiredPermissions)`: 권한 비동기 체크 및 요청
- `requestRuntimePermissions(List<String> missingPermissions)`: 런타임 권한 요청
- `handlePermissionResult(int requestCode, String[] permissions, int[] grantResults)`: 권한 결과 처리 (onRequestPermissionsResult에서 호출)
- `hasAllPermissions(String[] permissions)`: 모든 권한이 허용되었는지 확인
- `hasPermission(String permission)`: 특정 권한이 허용되었는지 확인
- `showPermissionPrompt(boolean permanentlyDenied)`: 권한 프롬프트 표시
- `openAppSettings()`: 앱 설정 열기

**ActivityModelTestProcess.java에 적용할 내용**:
```java
// 필드 선언
private PermissionManager permissionManager;

// onCreate에서 초기화 (이미 initializeServiceManagers에서 초기화됨)

// 기존 권한 메서드들을 PermissionManager로 위임
// checkAndRequestPermissionsAsync() → permissionManager.checkAndRequestPermissionsAsync(requiredPermissions)
// requestRuntimePermissions() → permissionManager.requestRuntimePermissions(missingPermissions)
// onRequestPermissionsResult() → permissionManager.handlePermissionResult(requestCode, permissions, grantResults)
// hasAllBluetoothPermissions() → permissionManager.hasAllPermissions(permissions)
// showPermissionPrompt() → permissionManager.showPermissionPrompt(permanentlyDenied)
// openAppSettings() → permissionManager.openAppSettings()
```

**제거/대체할 메서드**:
- `checkAndRequestPermissionsAsync()` → `permissionManager.checkAndRequestPermissionsAsync(...)`
- `requestRuntimePermissionsOnMainThread(...)` → `permissionManager.requestRuntimePermissions(...)`
- `onRequestPermissionsResult(...)` → `permissionManager.handlePermissionResult(...)`
- `handlePermissionDenial(...)` → PermissionManager 리스너에서 처리
- `hasAllBluetoothPermissions()` → `permissionManager.hasAllPermissions(...)`
- `showPermissionPrompt(...)` → `permissionManager.showPermissionPrompt(...)`
- `openAppSettings()` → `permissionManager.openAppSettings()`

---

#### 2.3 LoggingService
**위치**: `app/src/main/java/itf/com/app/lms/services/LoggingService.java`

**주요 기능**:
- 통합 로깅 인터페이스 제공
- LogManager 래퍼

**주요 메서드**:
- `logInfo(LogCategory category, String message)`: 정보 로그
- `logDebug(LogCategory category, String message)`: 디버그 로그
- `logWarn(LogCategory category, String message)`: 경고 로그
- `logError(LogCategory category, String message, Throwable throwable)`: 에러 로그
- `logBtTestResponse(...)`: BT 테스트 응답 로그

**ActivityModelTestProcess.java에 적용할 내용**:
```java
// 필드 선언
private LoggingService loggingService;

// onCreate에서 초기화 (이미 initializeServiceManagers에서 초기화됨)

// 기존 로깅 메서드들을 LoggingService로 위임 (선택사항)
// logInfo(...) → loggingService.logInfo(...)
// logDebug(...) → loggingService.logDebug(...)
// logWarn(...) → loggingService.logWarn(...)
// logError(...) → loggingService.logError(...)
// logBtTestResponse(...) → loggingService.logBtTestResponse(...)
```

**제거/대체할 메서드** (선택사항):
- `logInfo(...)` → `loggingService.logInfo(...)` (또는 기존 LogManager 직접 사용 유지)
- `logDebug(...)` → `loggingService.logDebug(...)`
- `logWarn(...)` → `loggingService.logWarn(...)`
- `logError(...)` → `loggingService.logError(...)`
- `logBtTestResponse(...)` → `loggingService.logBtTestResponse(...)`

---

### 3. 프로세서 (Processors)

#### 3.1 TestProcessProcessor
**위치**: `app/src/main/java/itf/com/app/lms/processors/TestProcessProcessor.java`

**주요 기능**:
- 테스트 항목 리스트 관리
- 테스트 카운터 관리 (OK/NG)
- 테스트 진행 상태 추적
- 테스트 결과 계산

**주요 메서드**:
- `rebuildTestItemList(String[][] testItems)`: 테스트 항목 리스트 재구성
- `recalcTestCountsFromAdapter(ItemAdapterTestItem adapter)`: 어댑터에서 테스트 카운트 재계산
- `incrementTestItemCounter()`: 테스트 카운터 증가
- `moveToNextTestItem(String[][] testItems, int requiredCount)`: 다음 테스트 항목으로 이동
- `getCurrentTestItem()`: 현재 테스트 항목 반환
- `getTestOkCnt()`: OK 카운트 반환
- `getTestNgCnt()`: NG 카운트 반환
- `resetCounters()`: 모든 카운터 리셋

**ActivityModelTestProcess.java에 적용할 내용**:
```java
// 필드 선언
private TestProcessProcessor testProcessProcessor;

// onCreate에서 초기화 (이미 initializeServiceManagers에서 초기화됨)

// 리스너 설정
testProcessProcessor.setTestProcessListener(new TestProcessProcessor.TestProcessListener() {
    @Override
    public void onTestCountsUpdated(int okCount, int ngCount) {
        testOkCnt = okCount;
        testNgCnt = ngCount;
        tvTestOkCnt.setText(String.valueOf(okCount));
        tvTestNgCnt.setText(String.valueOf(ngCount));
    }
    
    @Override
    public void onTestItemChanged(int itemIdx, String itemCommand) {
        // 테스트 항목 변경 처리
    }
    
    @Override
    public void onTestProgressUpdated(int totalCounter, int itemCounter) {
        // 테스트 진행 업데이트 처리
    }
});

// 기존 메서드들을 TestProcessProcessor로 위임
// rebuildTestItemList() → testProcessProcessor.rebuildTestItemList(testItems)
// recalcTestCountsFromAdapter() → testProcessProcessor.recalcTestCountsFromAdapter(adapter)
// testItemCounter 증가 → testProcessProcessor.incrementTestItemCounter()
// 다음 항목 이동 → testProcessProcessor.moveToNextTestItem(...)
```

**제거/대체할 메서드**:
- `rebuildTestItemList()` → `testProcessProcessor.rebuildTestItemList(...)`
- `recalcTestCountsFromAdapter(...)` → `testProcessProcessor.recalcTestCountsFromAdapter(...)`
- 테스트 카운터 관련 로직 → `testProcessProcessor` 메서드 사용

**제거/대체할 필드**:
- `testOkCnt` → `testProcessProcessor.getTestOkCnt()`
- `testNgCnt` → `testProcessProcessor.getTestNgCnt()`
- `testItemIdx` → `testProcessProcessor.getTestItemIdx()`
- `testItemCounter` → `testProcessProcessor.getTestItemCounter()`
- `testTotalCounter` → `testProcessProcessor.getTestTotalCounter()`
- `currentTestItem` → `testProcessProcessor.getCurrentTestItem()`

---

#### 3.2 ControlModeProcessor
**위치**: `app/src/main/java/itf/com/app/lms/processors/ControlModeProcessor.java`

**주요 기능**:
- 제어 모드 상태 관리
- 제어 모드 테스트 항목 실행
- 제어 모드 응답 처리
- 제어 모드 타임아웃 관리

**주요 메서드**:
- `toggleControlMode()`: 제어 모드 토글
- `setControlOn(boolean on)`: 제어 ON/OFF 설정
- `executeControlModeTestItem(String command)`: 제어 모드 테스트 항목 실행
- `handleControlModeResponse(String response)`: 제어 모드 응답 처리
- `startWaitingForControlResponse(String command)`: 제어 응답 대기 시작
- `startControlTestTimer(...)`: 제어 테스트 타이머 시작
- `stopControlTestTimer()`: 제어 테스트 타이머 중지
- `updateControlTestResult(...)`: 제어 테스트 결과 업데이트

**ActivityModelTestProcess.java에 적용할 내용**:
```java
// 필드 선언
private ControlModeProcessor controlModeProcessor;

// onCreate에서 초기화 (이미 initializeServiceManagers에서 초기화됨)

// 리스너 설정
controlModeProcessor.setControlModeListener(new ControlModeProcessor.ControlModeListener() {
    @Override
    public void onControlModeStateChanged(boolean isControlMode, boolean isControlOn) {
        // 제어 모드 상태 변경 처리
        ActivityModelTestProcess.this.isControlMode = isControlMode;
        ActivityModelTestProcess.this.isControlOn = isControlOn;
        updateHeaderBackgroundColor(isControlOn);
    }
    
    @Override
    public void onControlModeTestItemExecuted(String command) {
        // 제어 모드 테스트 항목 실행 처리
    }
    
    @Override
    public void onControlModeResponseReceived(String command, String response) {
        // 제어 모드 응답 수신 처리
    }
    
    @Override
    public void onControlModeResponseTimeout(String command) {
        // 제어 모드 응답 타임아웃 처리
    }
    
    @Override
    public void onControlModeTestResult(String command, String response, String resultValue, String result) {
        // 제어 모드 테스트 결과 처리
    }
});

// 기존 메서드들을 ControlModeProcessor로 위임
// toggleControlMode() → controlModeProcessor.toggleControlMode()
// executeControlModeTestItem(...) → controlModeProcessor.executeControlModeTestItem(...)
// handleControlModeResponse(...) → controlModeProcessor.handleControlModeResponse(...)
// startWaitingForControlResponse(...) → controlModeProcessor.startWaitingForControlResponse(...)
```

**제거/대체할 메서드**:
- `toggleControlMode()` → `controlModeProcessor.toggleControlMode()`
- `executeControlModeTestItem(...)` → `controlModeProcessor.executeControlModeTestItem(...)`
- `handleControlModeResponse(...)` → `controlModeProcessor.handleControlModeResponse(...)`
- `startWaitingForControlResponse(...)` → `controlModeProcessor.startWaitingForControlResponse(...)`
- `stopControlResponseTimeout()` → `controlModeProcessor.stopControlResponseTimeout()` (내부 메서드)
- `stopControlTestTimer()` → `controlModeProcessor.stopControlTestTimer()`
- `clearControlTestInfo()` → `controlModeProcessor.clearControlTestInfo()`

**제거/대체할 필드**:
- `isControlMode` → `controlModeProcessor.isControlMode()`
- `isControlOn` → `controlModeProcessor.isControlOn()`
- `controlOwnerIsAndroidApp` → `controlModeProcessor.isControlOwnerIsAndroidApp()`
- `controlSt0101SuccessCount` → `controlModeProcessor.getControlSt0101SuccessCount()`
- `controlModeReady` → `controlModeProcessor.isControlModeReady()`
- 제어 모드 관련 타이머 및 상태 변수들

---

#### 3.3 MessageProcessor
**위치**: `app/src/main/java/itf/com/app/lms/processors/MessageProcessor.java`

**주요 기능**:
- Bluetooth 메시지 파싱
- 메시지 검증
- 메시지 데이터 추출
- 응답 생성

**주요 메서드**:
- `processBtMessage(byte[] raw)`: Bluetooth 메시지 처리
- `validateMessage(byte[] raw)`: 메시지 형식 검증
- `extractNumericValue(String message, String pattern)`: 숫자 값 추출

**ActivityModelTestProcess.java에 적용할 내용**:
```java
// 필드 선언
private MessageProcessor messageProcessor;

// onCreate에서 초기화 (이미 initializeServiceManagers에서 초기화됨)

// 리스너 설정
messageProcessor.setMessageProcessorListener(new MessageProcessor.MessageProcessorListener() {
    @Override
    public void onMessageParsed(String command, String response, byte[] rawData) {
        // 메시지 파싱 완료 처리
    }
    
    @Override
    public void onMessageParseError(String error, byte[] rawData) {
        // 메시지 파싱 에러 처리
    }
    
    @Override
    public void onMessageDataExtracted(String command, String data) {
        // 메시지 데이터 추출 완료 처리
    }
});

// 기존 메서드들을 MessageProcessor로 위임
// processBtMessage(byte[] raw) → messageProcessor.processBtMessage(raw)
```

**제거/대체할 메서드**:
- `processBtMessage(byte[] raw)` → `messageProcessor.processBtMessage(raw)` (핵심 파싱 로직만)
- 메시지 파싱 관련 헬퍼 메서드들 → MessageProcessor로 이동

---

#### 3.4 DataParser
**위치**: `app/src/main/java/itf/com/app/lms/processors/DataParser.java`

**주요 기능**:
- JSON 파싱 (테스트 스펙, 바코드 정보 등)
- Excel 파일 파싱 (온도 데이터)
- 데이터 변환
- 에러 처리

**주요 메서드**:
- `parseTestSpecJson(String json)`: 테스트 스펙 JSON 파싱
- `parseBarcodeJson(String json)`: 바코드 정보 JSON 파싱
- `readTemperatureExcel(InputStream inputStream, String tableType)`: Excel에서 온도 데이터 읽기
- `parseJsonByType(String dataType, String json)`: 데이터 타입별 JSON 파싱
- `validateJson(String json)`: JSON 형식 검증

**ActivityModelTestProcess.java에 적용할 내용**:
```java
// 필드 선언
private DataParser dataParser;

// onCreate에서 초기화 (이미 initializeServiceManagers에서 초기화됨)

// 기존 메서드들을 DataParser로 위임
// jsonParsing("test_spec", json) → dataParser.parseTestSpecJson(json)
// jsonParsing("barcode", json) → dataParser.parseBarcodeJson(json)
// jsonParsingBarcode(...) → dataParser.parseBarcodeJson(json)
// processTestSpecJson(...) → dataParser.parseTestSpecJson(json)
// readTemperatureExcel(...) → dataParser.readTemperatureExcel(inputStream, tableType)
```

**제거/대체할 메서드**:
- `jsonParsing(String data_type, String json)` → `dataParser.parseJsonByType(data_type, json)`
- `jsonParsingBarcode(...)` → `dataParser.parseBarcodeJson(json)`
- `processTestSpecJson(String json)` → `dataParser.parseTestSpecJson(json)`
- `readTemperatureExcel(...)` → `dataParser.readTemperatureExcel(inputStream, tableType)`

---

## 적용 우선순위

### Phase 1: 즉시 적용 가능 (독립적)
1. ✅ **TimerManager** - 타이머 관리 (이미 초기화됨)
2. ✅ **PermissionManager** - 권한 관리 (이미 초기화됨)
3. ✅ **LoggingService** - 로깅 (선택사항)

### Phase 2: 점진적 적용 (의존성 있음)
4. **UsbConnectionManager** - USB 통신 (기존 코드와 통합 필요)
5. **TestProcessProcessor** - 테스트 프로세스 (상태 변수 마이그레이션 필요)
6. **ControlModeProcessor** - 제어 모드 (복잡한 상태 관리)

### Phase 3: 세부 적용 (핵심 로직)
7. **MessageProcessor** - 메시지 처리 (processBtMessage 핵심 로직)
8. **DataParser** - 데이터 파싱 (JSON/Excel 파싱)

---

## 적용 시 주의사항

1. **점진적 적용**: 한 번에 모든 것을 변경하지 말고, 하나씩 적용하고 테스트
2. **기존 기능 유지**: 리팩토링 후에도 모든 기능이 정상 동작해야 함
3. **리스너 설정**: 각 클래스의 리스너를 적절히 설정하여 기존 동작 유지
4. **상태 동기화**: 프로세서로 이동한 상태 변수들은 getter/setter로 접근
5. **메모리 누수 방지**: onDestroy에서 모든 cleanup 메서드 호출 필수

---

## 예상 효과

- **파일 크기 감소**: ~10,495줄 → ~3,000-4,000줄 (메인 Activity)
- **유지보수성 향상**: 기능별로 분리되어 수정이 용이
- **테스트 용이성**: 각 클래스를 독립적으로 테스트 가능
- **코드 재사용성**: 다른 Activity에서도 사용 가능
- **가독성 향상**: 각 클래스의 책임이 명확

