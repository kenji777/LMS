package itf.com.app.lms;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static itf.com.app.lms.ActivityModelList.cookie_info;
import static itf.com.app.lms.ActivityModelList.cookie_preferences;
import static itf.com.app.lms.ActivityModelList.globalLastTestStartTimestamp;
import static itf.com.app.lms.ActivityModelList.globalModelId;
import static itf.com.app.lms.ActivityModelList.globalModelName;
import static itf.com.app.lms.ActivityModelList.globalModelNation;
import static itf.com.app.lms.ActivityModelList.globalProductSerialNo;
import static itf.com.app.lms.util.Constants.TestItemCodes.ST0101;

import itf.com.app.lms.util.LogManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import itf.com.app.lms.conn.bluetooth.BluetoothManager;
import itf.com.app.lms.conn.network.NetworkManager;
import itf.com.app.lms.conn.usb.UsbConnectionManager;
import itf.com.app.lms.managers.TimerManager;
import itf.com.app.lms.managers.PermissionManager;
import itf.com.app.lms.services.LoggingService;
import itf.com.app.lms.processors.TestProcessProcessor;
import itf.com.app.lms.processors.ControlModeProcessor;
import itf.com.app.lms.processors.MessageProcessor;
import itf.com.app.lms.processors.DataParser;
import itf.com.app.lms.databinding.ActivityMainBinding;
import itf.com.app.lms.item.ItemAdapterTestItem;
import itf.com.app.lms.kiosk.BaseKioskActivity;
import itf.com.app.lms.util.ConnectedThreadOptimized;
import itf.com.app.lms.util.Constants;
import itf.com.app.lms.util.DialogManager;
import itf.com.app.lms.util.StringResourceManager;
import itf.com.app.lms.util.TestData;
import itf.com.app.lms.util.UsbCommandQueue;
import itf.com.app.lms.util.UsbService;
import itf.com.app.lms.util.WattValueAnimator;
import itf.com.app.lms.vo.VoTestItem;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;

@RequiresApi(api = Build.VERSION_CODES.S)
public class ActivityModelTestProcess extends BaseKioskActivity {

    private static final String TAG = ActivityModelTestProcess.class.getSimpleName();

    private static final UUID BT_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Bluetooth 관련 상수
    public final static int REQUEST_ENABLE_BT = 1;
    public final static int MESSAGE_READ = 2;
    public final static int CONNECTING_STATUS = 3;

    // ⚠️ 타임아웃 및 지연 시간 상수는 Constants.Timeouts로 이동됨
    // ⚠️ 테스트 카운터 임계값은 Constants.TestThresholds로 이동됨
    // ⚠️ 네트워크 관련 상수는 Constants.Network로 이동됨
    // ⚠️ UI 관련 상수는 Constants.UI로 이동됨

    // 권한 요청 코드
    private static final int PERMISSION_REQUEST_CODE = 1000;
    private static final int PERMISSION_REQUEST_CODE_BT = 1001;
    private static final Logger log = Logger.getLogger(ActivityModelTestProcess.class);

    private ConstraintLayout clDialogForPreprocess = null;
    private TextView tvDialogMessage = null;
    private TextView tvUnitId = null;
    private TextView tvUnitMessage = null;


    private ConstraintLayout clTestResult;
    private ConstraintLayout cl_dialog_for_logger;
    private TextView tv_dialog_for_logger_watt;
    private TextView tv_dialog_for_logger_temp;
    private TextView tvPopupProcessResult;
    public TextView tvWattValue;
    private TextView tvCompWattValue;
    private TextView tvHeaterWattValue;
    private TextView tvPumpWattValue;

    // ========== 소비전력 값 애니메이션 ==========
    private WattValueAnimator wattValueAnimator;
    // ========== End Watt Value Animation Fields ==========
    private int testOkCnt = 0;
    private int testNgCnt = 0;
    private TextView tvConnectBtRamp;
    private TextView tvConnectPlcRamp;
    private TextView tvRunWsRamp;
    private TextView tvTestOkCnt;
    private TextView tvTestNgCnt;
    private TextView tvCompValueWatt;
    private TextView tvHeaterValueWatt;
    private TextView tvPumpValueWatt;
    private TextView tvCompLowerValueWatt;
    private TextView tvCompUpperValueWatt;
    private TextView tvPumpLowerValueWatt;
    private TextView tvPumpUpperValueWatt;
    private TextView tvHeaterLowerValueWatt;
    private TextView tvHeaterUpperValueWatt;
    private TextView tvModelName;
    private TextView tvModelId;
    private TextView tv_dialog_barcode;
    private TextView tv_dialog_title = null;
    private TextView tvModelNationality;
    public TextView tvSerialNo;
    private TextView tvTotalTimeCnt;
    private TextView tvEllapsedTimeCnt;
    // GUI Components
    private TextView tvCurrentProcess;
    private TextView mBluetoothStatus;
    private TextView mReadBuffer;
    private TextView mReadText;
    private Button btnTestResultClose;
    private ListView lvTestItem;
    private ConstraintLayout cl_log;
    private TextView tv_log;
    public String mode_type = Constants.InitialValues.MODE_TYPE;
    private String log_text = "";
    private String log_text_param = "";

    // 제어 모드 상태 관리 (전역 변수)
    private boolean isControlMode = false;  // 제어 모드 활성화 여부
    private boolean isControlOn = false;   // 제어 ON/OFF 상태 (기본값: OFF)
    private boolean controlOwnerIsAndroidApp = false;  // 제어 ON을 안드로이드 앱에서 했는지 여부
    private static final int CONTROL_ST0101_REQUIRED_COUNT = 3;  // 제어 모드 진입을 위한 ST0101 응답 횟수
    private int controlSt0101SuccessCount = 0;  // 수신한 ST0101 응답 누적 횟수
    private boolean controlModeReady = false;   // 제어 모드 진입 완료 여부

    // 제어 모드 응답 대기 메커니즘
    private final AtomicBoolean waitingForControlResponse = new AtomicBoolean(false);
    private String pendingControlCommand = null;  // 대기 중인 명령
    private Timer controlResponseTimeoutTimer = null;
    private TimerTask controlResponseTimeoutTask = null;
    private final Object controlResponseLock = new Object();
    private static final long CONTROL_RESPONSE_TIMEOUT_MS = 10000; // 10초 타임아웃

    // 제어 모드 검사 실행 메커니즘
    private Timer controlTestTimer = null;
    private TimerTask controlTestTimerTask = null;
    private final AtomicBoolean controlTestTimerRunning = new AtomicBoolean(false);
    private final Object controlTestTimerLock = new Object();
    private int controlTestItemIdx = -1;  // 제어 모드에서 실행 중인 검사 항목 인덱스
    private int controlTestItemCounter = 0;  // 제어 모드 검사 항목 카운터
    private String controlCurrentTestItem = null;  // 제어 모드 현재 검사 항목
    private String controlTestReceiveCommand = null;  // 제어 모드 검사 수신 명령어
    private String controlTestReceiveResponse = null;  // 제어 모드 검사 수신 응답
    private String controlTestResultValue = null;  // 제어 모드 검사 결과 값 (소비전력 또는 온도)
    private String controlTestResult = null;  // 제어 모드 검사 결과 (OK/NG)

    // 제어 모드 다이얼로그 원래 설정 저장 변수들
    private float originalDialogMessageTextSize = 0f;  // tvDialogMessage의 원래 텍스트 크기
    private int originalDialogBarcodeVisibility = GONE;  // tv_dialog_barcode의 원래 visibility
    private String originalDialogTitleText = null;    // tv_dialog_title의 원래 텍스트
    private boolean isControlModeDialogConfigured = false;  // 제어 모드 다이얼로그 설정 여부

    static boolean usbReceiverRegisted = false;

    private TextView btnTestRestart;

    // ========== PHASE 1: Communication Managers (NEW) ==========
    private BluetoothManager bluetoothManager;
    private NetworkManager networkManager;
    private itf.com.app.lms.conn.usb.UsbConnectionManager usbConnectionManager;
    // ========== End Manager Fields ==========

    // ========== PHASE 2: Service Managers (NEW) ==========
    private itf.com.app.lms.managers.TimerManager timerManager;
    private itf.com.app.lms.managers.PermissionManager permissionManager;
    private itf.com.app.lms.services.LoggingService loggingService;
    // ========== End Service Manager Fields ==========

    // ========== PHASE 2: Processors (NEW) ==========
    private TestProcessProcessor testProcessProcessor;
    private ControlModeProcessor controlModeProcessor;
    private MessageProcessor messageProcessor;
    private DataParser dataParser;
    // ========== End Processor Fields ==========

    static public BluetoothAdapter mBTAdapter;
    private Set<BluetoothDevice> mPairedDevices;
    private ArrayAdapter<String> mBTArrayAdapter;

    private final ExecutorService btWorkerExecutor = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "BtWorker");
        thread.setPriority(Thread.NORM_PRIORITY - 1);
        return thread;
    });

    private final Map<String, Map<String, String>> specCache = new ConcurrentHashMap<>();
    private final Map<String, Double> latestWattByCommand = new ConcurrentHashMap<>();
    private static final Set<String> WATT_TRACKING_COMMANDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            Constants.TestItemCodes.CM0101,
            Constants.TestItemCodes.CM0102,
            Constants.TestItemCodes.HT0101,
            Constants.TestItemCodes.PM0101,
            Constants.TestItemCodes.SV0101,
            Constants.TestItemCodes.SV0201,
            Constants.TestItemCodes.SV0301,
            Constants.TestItemCodes.SV0401
    )));
    private static final Set<String> SOL_VALVE_COMMANDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            Constants.TestItemCodes.SV0101,
            Constants.TestItemCodes.SV0201,
            Constants.TestItemCodes.SV0301,
            Constants.TestItemCodes.SV0401
    )));
    // private final Map<String, Double> latestWattByCommand = new ConcurrentHashMap<>();
    private volatile String lastSpecSignature = "";

    private Handler btHandler;
    private ConnectedThreadOptimized btConnectedThread; // 최적화된 클래스 사용
    private BluetoothSocket btSocket = null;

    private TextView tvPopupProcessResultCompValue;
    private TextView tvPopupProcessResultHeaterValue;
    private TextView tvResourceInfo;

    // ViewBinding: findViewById 호출 제거 및 성능 최적화
    private ActivityMainBinding binding;

    // UI 업데이트 배치 처리를 위한 변수 (Phase 1: 배치 처리 시스템 구현)
    private final List<Runnable> pendingUiUpdates = new ArrayList<>();
    private final Object uiUpdateLock = new Object();
    private Runnable uiUpdateBatchTask = null;
    private static final long UI_UPDATE_BATCH_DELAY_MS = 16; // 1 frame (60fps)

    private String readMessage = null;
//    private static String readMessagePlcTmp = null;
//    private String readMessageBTTmp = null;

    private ItemAdapterTestItem listItemAdapter = null;
    private String sendResultYn = Constants.InitialValues.SEND_RESULT_YN;


    private int totalTimeCnt = 0;
    private int testItemIdx = 0;
    private int testItemCounter = 0;
    private int testTotalCounter = 0;
    private String currentTestItem = Constants.InitialValues.CURRENT_TEST_ITEM;
    private final String datetimeFormat = Constants.DateTimeFormats.DATETIME_FORMAT;
    private final String timestampFormat = Constants.DateTimeFormats.TIMESTAMP_FORMAT;

    private final ScheduledExecutorService usbPollingExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "UsbPolling");
        thread.setPriority(Thread.NORM_PRIORITY - 2);
        thread.setDaemon(true);
        return thread;
    });
    private ScheduledFuture<?> usbPollingFuture = null;
    private long usbPollingIntervalMs = Constants.Timeouts.USB_TIMER_INTERVAL_MS;
    private volatile boolean usbPollingEnabled = false;
    private int usbPollingFailureCount = 0;
    private static final int USB_POLLING_FAILURE_THRESHOLD = 5;
    private static final long USB_POLLING_BACKOFF_MS = Constants.Timeouts.USB_TIMER_INTERVAL_MS * 5;
    private static final long USB_PERMISSION_RECOVERY_DELAY_MS = 1000L;
    private boolean usbPollingRequested = false;
    private final Handler usbRecoveryHandler = new Handler(Looper.getMainLooper());
    private Runnable usbPermissionRecoveryRunnable = null;
    private final Handler usbReconnectHandler = new Handler(Looper.getMainLooper());
    private boolean isUsbReconnecting = false;
    private int usbReconnectAttempts = 0;
    private Runnable usbReconnectRunnable = null;
    private static final int USB_RETRY_MAX_ATTEMPTS = 10;
    private static final int BT_RETRY_MAX_ATTEMPTS = 3;


    static private final String decTemperature = "";
    static private final double decTemperatureValue = 0;
    private List<Map<String, String>> coldTemperatureData = null;
    private List<Map<String, String>> hotTemperatureData = null;
    private List<Map<String, String>> temperatureData = null;
    private boolean entranceCheck = false;
    private boolean barcodeReadCheck = false;

    // private double decElectricValue = 0;
    private int decElectricValue = 0;
    private String resultValue = "";
    static private String resultInfo = Constants.Common.EMPTY_STRING;
    private double decElectricValueForComp = 0;
    private double decElectricValueForHeater = 0;
    private double decElectricValueForPump = 0;

    private String plcCommand = "";

    private TextView tvTemperature = null;


    public static int tmrAppResetCnt = 0;
    public static int tmrAppResetCnt2 = 0;
    public static int tmrAppResetCnt1 = 0;
    private TimerTask appResetTimerTask = null;
    private Timer tmrBTMessageSend = null;
    private TimerTask ttBTMessageSend = null;

    private boolean shouldUpdateDialog = false;

    // Phase 3: Timer 중복 생성 방지 메커니즘
    private final AtomicBoolean btMessageTimerRunning = new AtomicBoolean(false);
    private final Object btMessageTimerLock = new Object();

    private void startBtMessageTimer() {
        // Phase 3: Timer 중복 생성 방지 - synchronized 블록으로 동시성 제어
        synchronized (btMessageTimerLock) {
            // 이미 실행 중이면 중복 시작 방지
            if (btMessageTimerRunning.get()) {
                logWarn(LogManager.LogCategory.BT, "BT message timer already running, skipping start");
                return;
            }

            // 블루투스 연결 상태 확인 (제어 모드가 아닐 때만)
            if (!isControlMode) {
                if (!btConnected || btSocket == null || !btSocket.isConnected()) {
                    logWarn(LogManager.LogCategory.BT, "Bluetooth not connected, cannot start message timer");
                    return;
                }
            }

            // 기존 Timer 정리
            if (tmrBTMessageSend != null || ttBTMessageSend != null) {
                stopBtMessageTimer();
            }

            // 제어 모드가 아닐 때만 헤더 배경색 변경 (제어 모드에서는 toggleControlMode에서 관리)
            // 실제 isControlOn 상태를 전달해야 함 (타이머 시작은 제어 ON 상태를 의미하지 않음)
            if (!isControlMode) {
                updateHeaderBackgroundColor(isControlOn);
            }

            // btMessageTimerRunning 상태를 true로 설정
            btMessageTimerRunning.set(true);

            try {
                tmrBTMessageSend = new Timer("BtMsgTimer");
                ttBTMessageSend = new TimerTask() {
                    @Override
                    public void run() {
                        // 제어 모드이고 제어 ON 상태면 메시지 전송 안 함
                        if (isControlMode && isControlOn) {
                            if(controlSt0101SuccessCount<3) {
                                sendBtMessage(ST0101);
                            }
                            return; // 검사 진행 안 함
                        }

                        // 제어 모드가 아닐 때만 블루투스 연결 상태 확인 및 타이머 중지
                        if (!isControlMode) {
                            if (!btConnected || btSocket == null || !btSocket.isConnected()) {
                                logWarn(LogManager.LogCategory.BT, "Bluetooth socket not connected; stopping message timer");
                                stopBtMessageTimer();
                                scheduleBluetoothReconnect(false);
                                return;
                            }
                        } else {
                            // 제어 모드이지만 제어 OFF 상태: 블루투스 연결 상태만 확인
                            if (!btConnected || btSocket == null || !btSocket.isConnected()) {
                                return; // 메시지 전송만 건너뜀
                            }
                        }

                        if (!entranceCheck) {
                            if (!barcodeReadCheck) {
                                runOnBtWorker(() -> callBarcodeInfoServer());
                            }
                            sendBtMessage(currentTestItem);
                        } else {
                            // ⚠️ 중요: arrTestItems가 초기화되기 전에 접근하는 것을 방지
                            if (arrTestItems == null || arrTestItems.length == 0) {
                                logWarn(LogManager.LogCategory.BT, "arrTestItems not initialized yet, skipping message processing");
                                return;
                            }
                            if (testItemIdx < arrTestItems.length) {
                                currentTestItem = arrTestItems[testItemIdx][1];
                                disconnectCheckCount = receivedMessageCnt - sendingMessageCnt;
                                // logInfo(LogManager.LogCategory.PS, ">>>>>>>>>>>>>>>>> readMessage " + readMessage);
                                // if ((disconnectCheckCount > Constants.TestThresholds.DISCONNECT_CHECK_THRESHOLD || disconnectCheckCount < -Constants.TestThresholds.DISCONNECT_CHECK_THRESHOLD) && (readMessage.equals("") || readMessage==null)) {
                                if (disconnectCheckCount > Constants.TestThresholds.DISCONNECT_CHECK_THRESHOLD || Math.abs(disconnectCheckCount) > Constants.TestThresholds.DISCONNECT_CHECK_THRESHOLD) {
                                    // if (receiveCommand.equals("")) {
//                                        if(receiveCommandEmptyCnt<=BT_RETRY_MAX_ATTEMPTS) {
//                                            // logInfo(LogManager.LogCategory.PS, ">>>>>>>>>>>>>>>>> receiveCommandEmptyCnt " + receiveCommandEmptyCnt);
//                                            receiveCommandEmptyCnt++;
//                                        }
//                                        else {
                                    logWarn(LogManager.LogCategory.BT, String.format("검사 항목 신호 송신.B [T:%d/%d][C:%d/%s][P:%d/%d][S:%s][G:%d=%d-%d] 수신 없음 %s",
                                            testTotalCounter, totalTimeCnt, testItemCounter, arrTestItems[testItemIdx][2],
                                            testItemIdx, arrTestItems.length, currentTestItem, disconnectCheckCount,
                                            receivedMessageCnt, sendingMessageCnt, receiveCommandEmptyCnt));
                                    stopBtMessageTimer();
                                    // Phase 5: Bluetooth Handler 내부 통합 - scheduleUiUpdate 사용
                                    scheduleUiUpdate(() -> {
                                        final int finalUsbStatusColor = R.color.red_01;
                                        final String finalUsbStatusText = "";
                                        tvConnectBtRamp.setBackgroundColor(getColor(finalUsbStatusColor));
                                        if (finalUsbStatusText != null) {
                                            // tvConnectBtRamp.setText(finalUsbStatusText);
                                        }

                                        clAlert.setVisibility(GONE);
                                        if (clDialogForPreprocess.getVisibility() == VISIBLE) {
                                            clDialogForPreprocess.setVisibility(GONE);
                                            tvDialogMessage.setText("");
                                        }
                                        clAlert.setVisibility(VISIBLE);
                                        tvAlertMessage.setText(getStringResource("ui.message.no_received_data"));
                                        btnAlertClose.setOnClickListener(new Button.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                clAlert.setVisibility(GONE);
                                            }
                                        });
                                    });

                                    startResetTimer();
                                    // }
                                    // }
                                }
                                logInfo(LogManager.LogCategory.BT, String.format(Constants.LogMessages.TEST_ITEM_SIGNAL_SENT + Constants.Common.EMPTY_STRING + " [T:%d/%d][C:%d/%s][P:%d/%d][S:%s][G:%d=%d-%d][W:%s]",
                                        testTotalCounter, totalTimeCnt, testItemCounter, arrTestItems[testItemIdx][2],
                                        testItemIdx, arrTestItems.length, currentTestItem, disconnectCheckCount,
                                        receivedMessageCnt, sendingMessageCnt, decElectricValue));

                                if (Integer.parseInt(arrTestItems[testItemIdx][2]) <= testItemCounter) {
                                    testItemCounter = 0;
                                    testItemIdx++;
                                    // Phase 2: 단순 UI 업데이트를 scheduleUiUpdate로 변경
                                    scheduleUiUpdate(() -> lvTestItem.smoothScrollToPosition(testItemIdx));
                                }

                                if (currentTestItem.equals(Constants.TestItemCodes.CM0100)) {
                                    final String finalReceiveResponseResult = Constants.ResultStatus.OK;
                                    final String finalReceiveResponseResultTxt = Constants.Common.SUCCESS;
                                    runOnUiThread(() -> {
                                        try {
                                            int CM0101Index = -1;
                                            for (int i = 0; i < listItemAdapter.getCount(); i++) {
                                                if (Constants.TestItemCodes.CM0101.equals(((VoTestItem) listItemAdapter.getItem(i)).getTest_item_command())) {
                                                    CM0101Index = i;
                                                    break;
                                                }
                                            }

                                            if (CM0101Index >= 0 && CM0101Index + 1 < listItemAdapter.getCount()) {
                                                ((VoTestItem) listItemAdapter.getItem(CM0101Index + 1)).setTest_item_name(Constants.Common.COMP_COOLING_PERFORMANCE + finalReceiveResponseResultTxt);
                                                ((VoTestItem) listItemAdapter.getItem(CM0101Index + 1)).setTest_item_result(finalReceiveResponseResult);
                                                ((VoTestItem) listItemAdapter.getItem(CM0101Index + 1)).setTest_result_value(finalReceiveResponseResult + " / 01");
                                                ((VoTestItem) listItemAdapter.getItem(CM0101Index + 1)).setTest_finish_yn(Constants.ResultStatus.YES);
                                                listItemAdapter.updateListAdapter();
                                            } else {
                                                logWarn(LogManager.LogCategory.BT, Constants.LogMessages.CM0101_INDEX_NOT_FOUND + CM0101Index);
                                            }
                                        } catch (Exception e) {
                                            logError(LogManager.LogCategory.ER, Constants.ErrorMessages.ERROR_UPDATING_CM0100_IN_RUN_ON_UI_THREAD, e);
                                        }
                                    });
                                }

                                sendingMessageCnt++;
                                int testItemIdxTmp = (testItemIdx != arrTestItems.length) ? testItemIdx : testItemIdx - 1;
                                if (testItemIdx != arrTestItems.length) {
                                    sendBtMessage(currentTestItem);
                                }
                                log_text_param = "[" + getCurrentDatetime(datetimeFormat) + Constants.Common.LOGGER_DEVIDER_01 + String.format(Constants.InitialValues.NUMBER_FORMMATING_03D, testItemCounter) + Constants.Common.LOGGER_DEVIDER_01 + String.format(Constants.InitialValues.NUMBER_FORMMATING_03D, Integer.parseInt(arrTestItems[testItemIdxTmp][2])) + "] " + currentTestItem + " \t";
                            }
                        }

                        // Phase 2: 단순 UI 업데이트를 scheduleUiUpdate로 변경
                        scheduleUiUpdate(() -> tvEllapsedTimeCnt.setText(String.valueOf(testTotalCounter)));

                        wattTemp = new String[]{getCurrentDatetime(timestampFormat), calculatedTemperatureTmp, String.valueOf(decElectricValue), currentTestItem};
                        lstMapWattTemp.add(wattTemp);

                        testItemCounter++;
                        testTotalCounter++;
                    }
                };
                tmrBTMessageSend.schedule(ttBTMessageSend, 0, Constants.Timeouts.TIMER_INTERVAL_MS);
                btMessageTimerRunning.set(true);
            } catch (Exception e) {
                btMessageTimerRunning.set(false);
                logError(LogManager.LogCategory.ER, "Error starting BT message timer", e);
            }
        }
    }

    int resetCnt = 0;
    Timer tmrReset = null;
    TimerTask ttReset = null;

    private void stopBtMessageTimer() {
        // Phase 3: Timer 중복 생성 방지 - synchronized 블록으로 동시성 제어
        synchronized (btMessageTimerLock) {
            if (btMessageTimerRunning.compareAndSet(true, false)) {
                // 제어 모드가 아닐 때만 헤더 배경색 원복 (제어 모드에서는 toggleControlMode에서 관리)
                if (!isControlMode) {
                    updateHeaderBackgroundColor(false);
                }

                if (tmrBTMessageSend != null) {
                    try {
                        tmrBTMessageSend.cancel();
                        tmrBTMessageSend.purge();
                        tmrBTMessageSend = null;
                    } catch (Exception e) {
                        logError(LogManager.LogCategory.ER, "Error canceling BT message timer", e);
                    }
                }
                if (ttBTMessageSend != null) {
                    try {
                        ttBTMessageSend.cancel();
                        ttBTMessageSend = null;
                    } catch (Exception e) {
                        logError(LogManager.LogCategory.ER, "Error canceling BT message timer task", e);
                    }
                }
            } else {
                // 이미 정지된 상태이지만 안전을 위해 한 번 더 정리
                // 제어 모드가 아닐 때만 헤더 배경색 원복 (제어 모드에서는 toggleControlMode에서 관리)
                if (!isControlMode) {
                    updateHeaderBackgroundColor(false);
                }

                if (tmrBTMessageSend != null) {
                    try {
                        tmrBTMessageSend.cancel();
                        tmrBTMessageSend.purge();
                        tmrBTMessageSend = null;
                    } catch (Exception ignored) {
                    }
                }
                if (ttBTMessageSend != null) {
                    try {
                        ttBTMessageSend.cancel();
                        ttBTMessageSend = null;
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    /**
     * 제어 ON/OFF 상태에 따라 헤더 배경색 업데이트
     * 검사 대기 중이고 제어 ON일 때만 #cc0000로 변경
     *
     * @param isControlOn 제어 ON 여부
     */
    private void updateHeaderBackgroundColor(boolean isControlOn) {
        // 메인 스레드에서 UI 업데이트
        runOnUiThread(() -> {
            try {
                if (binding == null) {
                    return;
                }

                // ll_top_header LinearLayout 가져오기
                android.widget.LinearLayout llTopHeader = binding.llTopHeader;
                if (llTopHeader == null) {
                    return;
                }

                // 검사 대기 중인지 확인 (currentTestItem이 비어있거나 초기값인지)
                boolean isTestWaiting = currentTestItem == null ||
                        currentTestItem.isEmpty() ||
                        currentTestItem.equals(Constants.InitialValues.CURRENT_TEST_ITEM);

                System.out.println(">>>>>>>>>>>>>>>>> Setting header background color to red for control mode isControlOn " + isControlOn + " isTestWaiting " + isTestWaiting);
                if (isControlOn && isTestWaiting) {
                    System.out.println(">>>>>>>>>>>>>>>>> Setting header background color to red for control mode AAAAA");
                    // 제어 ON이고 검사 대기 중일 때 배경색을 #cc0000로 변경
                    llTopHeader.setBackgroundColor(0xFFCC0000); // #cc0000
                } else {
                    System.out.println(">>>>>>>>>>>>>>>>> Setting header background color to red for control mode CCCCC");
                    // 제어 OFF이거나 검사 중일 때 원래 배경색으로 복귀
                    llTopHeader.setBackgroundColor(getColor(R.color.blue_for_ovio));
                }
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, "Error updating header background color", e);
            }
        });
    }

    /**
     * 제어 모드 토글 (ON ↔ OFF)
     */
    private void toggleControlMode() {
        // 검사 진행 중이면 제어 불가
        boolean isTestRunning = currentTestItem != null &&
                !currentTestItem.isEmpty() &&
                !currentTestItem.equals(Constants.InitialValues.CURRENT_TEST_ITEM);

        if (isTestRunning) {
            runOnUiThread(() -> {
                android.widget.Toast.makeText(this, getStringResource("ui.message.test_control_disabled"), android.widget.Toast.LENGTH_SHORT).show();
            });
            return;
        }

        // 제어 모드 토글
        System.out.println(">>>>>>>>>>>>>>>>> Setting header background color to red for control mode isControlOn.before " + isControlOn);
        isControlOn = !isControlOn;
        System.out.println(">>>>>>>>>>>>>>>>> Setting header background color to red for control mode isControlOn.after " + isControlOn);

        if (isControlOn) {
            // 제어 모드로 진입: 모든 타이머 중지 및 헤더 배경색 red로 설정
            isControlMode = true;  // 제어 모드 활성화
            controlOwnerIsAndroidApp = true;  // 안드로이드 앱에서 제어 ON
            stopAllTimers();
            updateHeaderBackgroundColorToRed();

            // 웹서버에 안드로이드 앱에서 제어 ON을 했다는 신호 전달
            notifyWebServerControlOwner(true);
        } else {
            // 제어 모드 종료: 제어 모드 비활성화, 타이머 재시작, 헤더 배경색 원복
            // 안드로이드 앱에서 제어 ON을 한 경우에만 제어 OFF 가능
            if (!controlOwnerIsAndroidApp) {
                // 웹에서 제어 ON을 한 경우, 안드로이드 앱에서 제어 OFF 불가
                runOnUiThread(() -> {
                    android.widget.Toast.makeText(this, getStringResource("ui.message.control_off_only_by_owner"), android.widget.Toast.LENGTH_SHORT).show();
                });
                // 상태 원복
                isControlOn = !isControlOn;
                return;
            }

            isControlMode = false;  // 제어 모드 비활성화
            controlOwnerIsAndroidApp = false;  // 제어 소유자 초기화

            // 제어 모드 해제 시 다이얼로그 설정도 원상 복구
            if (isControlModeDialogConfigured) {
                if (tvDialogMessage != null && originalDialogMessageTextSize > 0) {
                    tvDialogMessage.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, originalDialogMessageTextSize);
                }

                if (tv_dialog_barcode != null && originalDialogBarcodeVisibility != GONE) {
                    int visibility = originalDialogBarcodeVisibility;
                    switch (visibility) {
                        case VISIBLE:
                            tv_dialog_barcode.setVisibility(VISIBLE);
                            break;
                        case INVISIBLE:
                            tv_dialog_barcode.setVisibility(INVISIBLE);
                            break;
                        case GONE:
                            tv_dialog_barcode.setVisibility(GONE);
                            break;
                    }
                }

                if (tv_dialog_title != null && originalDialogTitleText != null) {
                    tv_dialog_title.setText(originalDialogTitleText);
                }

                isControlModeDialogConfigured = false;
            }

            // 웹 클라이언트에 종료 신호 전달
            notifyWebClientShutdown("CONTROL_MODE_EXIT");
            // 웹서버에 제어 소유자 초기화 신호 전달
            notifyWebServerControlOwner(false);
            restoreTimersAfterControlMode();
            updateHeaderBackgroundColor(false);
        }

        // 버튼 상태 업데이트
        updateControlModeButton();
    }

    /**
     * 제어 모드 해제 후 타이머 원상 복구
     */
    private void restoreTimersAfterControlMode() {
        try {
            // BT 메시지 타이머 재시작 (블루투스가 연결되어 있는 경우)
            if (btConnected && btSocket != null && btSocket.isConnected()) {
                // startBtMessageTimer()는 제어 모드 관련 로직이 포함되어 있으므로
                // 제어 모드 해제 시에는 직접 타이머를 시작
                synchronized (btMessageTimerLock) {
                    if (!btMessageTimerRunning.get()) {
                        // 기존 Timer 정리
                        if (tmrBTMessageSend != null || ttBTMessageSend != null) {
                            try {
                                if (tmrBTMessageSend != null) {
                                    tmrBTMessageSend.cancel();
                                    tmrBTMessageSend.purge();
                                    tmrBTMessageSend = null;
                                }
                                if (ttBTMessageSend != null) {
                                    ttBTMessageSend.cancel();
                                    ttBTMessageSend = null;
                                }
                            } catch (Exception e) {
                                logError(LogManager.LogCategory.ER, "Error cleaning up BT message timer before restart", e);
                            }
                        }

                        btMessageTimerRunning.set(true);

                        try {
                            tmrBTMessageSend = new Timer("BtMsgTimer");
                            ttBTMessageSend = new TimerTask() {
                                @Override
                                public void run() {
                                    if (!btConnected || btSocket == null || !btSocket.isConnected()) {
                                        logWarn(LogManager.LogCategory.BT, "Bluetooth socket not connected; stopping message timer");
                                        stopBtMessageTimer();
                                        scheduleBluetoothReconnect(false);
                                        return;
                                    }

                                    if (!entranceCheck) {
                                        if (!barcodeReadCheck) {
                                            runOnBtWorker(() -> callBarcodeInfoServer());
                                        }
                                        sendBtMessage(currentTestItem);
                                    } else {
                                        // ⚠️ 중요: arrTestItems가 초기화되기 전에 접근하는 것을 방지
                                        if (arrTestItems == null || arrTestItems.length == 0) {
                                            logWarn(LogManager.LogCategory.BT, "arrTestItems not initialized yet, skipping message processing");
                                            return;
                                        }
                                        if (testItemIdx < arrTestItems.length) {
                                            currentTestItem = arrTestItems[testItemIdx][1];
                                            disconnectCheckCount = receivedMessageCnt - sendingMessageCnt;
                                            if (disconnectCheckCount > Constants.TestThresholds.DISCONNECT_CHECK_THRESHOLD || Math.abs(disconnectCheckCount) > Constants.TestThresholds.DISCONNECT_CHECK_THRESHOLD) {
                                                logWarn(LogManager.LogCategory.BT, String.format("검사 항목 신호 송신.B [T:%d/%d][C:%d/%s][P:%d/%d][S:%s][G:%d=%d-%d] 수신 없음 %s",
                                                        testTotalCounter, totalTimeCnt, testItemCounter, arrTestItems[testItemIdx][2],
                                                        testItemIdx, arrTestItems.length, currentTestItem, disconnectCheckCount,
                                                        receivedMessageCnt, sendingMessageCnt, receiveCommandEmptyCnt));
                                                stopBtMessageTimer();
                                                scheduleUiUpdate(() -> {
                                                    final int finalUsbStatusColor = R.color.red_01;
                                                    final String finalUsbStatusText = "";
                                                    tvConnectBtRamp.setBackgroundColor(getColor(finalUsbStatusColor));
                                                    if (finalUsbStatusText != null) {
                                                        // tvConnectBtRamp.setText(finalUsbStatusText);
                                                    }

                                                    clAlert.setVisibility(GONE);
                                                    if (clDialogForPreprocess.getVisibility() == VISIBLE) {
                                                        clDialogForPreprocess.setVisibility(GONE);
                                                        tvDialogMessage.setText("");
                                                    }
                                                    clAlert.setVisibility(VISIBLE);
                                                    tvAlertMessage.setText(getStringResource("ui.message.no_received_data"));
                                                    btnAlertClose.setOnClickListener(new Button.OnClickListener() {
                                                        @Override
                                                        public void onClick(View view) {
                                                            clAlert.setVisibility(GONE);
                                                        }
                                                    });
                                                    clAlert.setBackgroundColor(getColor(R.color.red_01));
                                                });

                                                startResetTimer();
                                                return;
                                            }

                                            if (readMessage != null && !readMessage.isEmpty()) {
                                                receiveCommandEmptyCnt = 0;
                                                String[] arrReadMessage = readMessage.split(Constants.Common.LOGGER_DEVIDER_01.trim());
                                                if (arrReadMessage.length > 0) {
                                                    receiveCommand = arrReadMessage[0];
                                                }
                                            } else {
                                                receiveCommand = "";
                                            }

                                            if (receiveCommand.equals(arrTestItems[testItemIdx][0])) {
                                                int testItemIdxTmp = testItemIdx;
                                                testItemIdx++;
                                                receivedMessageCnt++;
                                                if (testItemIdx >= arrTestItems.length) {
                                                    entranceCheck = false;
                                                    testItemIdx = 0;
                                                    finishedCorrectly = true;
                                                    stopBtMessageTimer();
                                                    return;
                                                }
                                                log_text_param = "[" + getCurrentDatetime(datetimeFormat) + Constants.Common.LOGGER_DEVIDER_01 + String.format(Constants.InitialValues.NUMBER_FORMMATING_03D, testItemCounter) + Constants.Common.LOGGER_DEVIDER_01 + String.format(Constants.InitialValues.NUMBER_FORMMATING_03D, Integer.parseInt(arrTestItems[testItemIdxTmp][2])) + "] " + currentTestItem + " \t";
                                            }
                                        }
                                    }

                                    // Phase 2: 단순 UI 업데이트를 scheduleUiUpdate로 변경
                                    scheduleUiUpdate(() -> tvEllapsedTimeCnt.setText(String.valueOf(testTotalCounter)));

                                    wattTemp = new String[]{getCurrentDatetime(timestampFormat), calculatedTemperatureTmp, String.valueOf(decElectricValue), currentTestItem};
                                    lstMapWattTemp.add(wattTemp);

                                    testItemCounter++;
                                    testTotalCounter++;
                                }
                            };
                            tmrBTMessageSend.schedule(ttBTMessageSend, 0, Constants.Timeouts.TIMER_INTERVAL_MS);
                            btMessageTimerRunning.set(true);
                            logInfo(LogManager.LogCategory.PS, "BT message timer restarted after control mode exit");
                        } catch (Exception e) {
                            btMessageTimerRunning.set(false);
                            logError(LogManager.LogCategory.ER, "Error restarting BT message timer after control mode", e);
                        }
                    }
                }
            } else {
                logWarn(LogManager.LogCategory.BT, "Bluetooth not connected, skipping BT message timer restart");
            }
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, "Error restoring timers after control mode", e);
        }
    }

    /**
     * 모든 동작 중인 타이머 중지
     * 제어 모드 진입 시 모든 타이머를 중지하여 시스템을 안전한 상태로 만듭니다.
     */
    private void stopAllTimers() {
        try {
            // 1. BT 메시지 타이머 중지
            stopBtMessageTimer();

            // 2. Finished Restart 타이머 중지
            stopFinishedRestartTimer();

            // 3. Unfinished Restart 타이머 중지
            stopUnfinishedRestartTimer();

            // 4. Remote Command 타이머 중지
            stopRemoteCommandTimer();

            // 5. Reset 타이머 중지
            stopResetTimer();

            // 6. Duration 체크 타이머 중지
            stopCheckDurationTimer();

            // 7. 바코드 요청 타이머 중지
            stopBarcodeRequestTimer();

            // 8. 앱 리셋 타이머 태스크 중지
            stopAppResetTimerTask();

            // 9. USB 폴링 Executor 중지 - 제어 모드에서도 USB 통신은 유지해야 하므로 중지하지 않음
            // stopUsbPollingTimer();

            logInfo(LogManager.LogCategory.PS, "All timers stopped for control mode (USB polling timer kept running)");
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, "Error stopping all timers", e);
        }
    }

    /**
     * Finished Restart 타이머 중지
     */
    private void stopFinishedRestartTimer() {
        synchronized (finishedRestartTimerLock) {
            if (finishedRestartTimerRunning.compareAndSet(true, false)) {
                if (tmrFinishedRestart != null) {
                    try {
                        tmrFinishedRestart.cancel();
                        tmrFinishedRestart.purge();
                        tmrFinishedRestart = null;
                    } catch (Exception e) {
                        logError(LogManager.LogCategory.ER, "Error canceling finished restart timer", e);
                    }
                }
                if (ttFinishedRestart != null) {
                    try {
                        ttFinishedRestart.cancel();
                        ttFinishedRestart = null;
                    } catch (Exception e) {
                        logError(LogManager.LogCategory.ER, "Error canceling finished restart timer task", e);
                    }
                }
            } else {
                // 실행 중이 아니어도 정리 (안전을 위해)
                if (tmrFinishedRestart != null) {
                    try {
                        tmrFinishedRestart.cancel();
                        tmrFinishedRestart.purge();
                        tmrFinishedRestart = null;
                    } catch (Exception ignored) {
                    }
                }
                if (ttFinishedRestart != null) {
                    try {
                        ttFinishedRestart.cancel();
                        ttFinishedRestart = null;
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    /**
     * Unfinished Restart 타이머 중지
     */
    private void stopUnfinishedRestartTimer() {
        synchronized (unfinishedRestartTimerLock) {
            if (unfinishedRestartTimerRunning.compareAndSet(true, false)) {
                if (tmrUnfinishedRestart != null) {
                    try {
                        tmrUnfinishedRestart.cancel();
                        tmrUnfinishedRestart.purge();
                        tmrUnfinishedRestart = null;
                    } catch (Exception e) {
                        logError(LogManager.LogCategory.ER, "Error canceling unfinished restart timer", e);
                    }
                }
                if (ttUnfinishedRestart != null) {
                    try {
                        ttUnfinishedRestart.cancel();
                        ttUnfinishedRestart = null;
                    } catch (Exception e) {
                        logError(LogManager.LogCategory.ER, "Error canceling unfinished restart timer task", e);
                    }
                }
            } else {
                // 실행 중이 아니어도 정리 (안전을 위해)
                if (tmrUnfinishedRestart != null) {
                    try {
                        tmrUnfinishedRestart.cancel();
                        tmrUnfinishedRestart.purge();
                        tmrUnfinishedRestart = null;
                    } catch (Exception ignored) {
                    }
                }
                if (ttUnfinishedRestart != null) {
                    try {
                        ttUnfinishedRestart.cancel();
                        ttUnfinishedRestart = null;
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    /**
     * Remote Command 타이머 중지
     */
    private void stopRemoteCommandTimer() {
        if (tmrRemoteCommand != null) {
            try {
                tmrRemoteCommand.cancel();
                tmrRemoteCommand.purge();
                tmrRemoteCommand = null;
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, "Error canceling remote command timer", e);
            }
        }
        if (ttRemoteCommand != null) {
            try {
                ttRemoteCommand.cancel();
                ttRemoteCommand = null;
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, "Error canceling remote command timer task", e);
            }
        }
    }

    /**
     * Reset 타이머 중지
     */
    private void stopResetTimer() {
        if (tmrReset != null) {
            try {
                tmrReset.cancel();
                tmrReset.purge();
                tmrReset = null;
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, "Error canceling reset timer", e);
            }
        }
        if (ttReset != null) {
            try {
                ttReset.cancel();
                ttReset = null;
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, "Error canceling reset timer task", e);
            }
        }
    }

    /**
     * Duration 체크 타이머 중지
     */
    private void stopCheckDurationTimer() {
        if (checkDuration != null) {
            try {
                checkDuration.cancel();
                checkDuration.purge();
                checkDuration = null;
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, "Error canceling check duration timer", e);
            }
        }
    }

    /**
     * 바코드 요청 타이머 중지
     */
    private void stopBarcodeRequestTimer() {
        if (barcodeRequestTimer != null) {
            try {
                barcodeRequestTimer.cancel();
                barcodeRequestTimer.purge();
                barcodeRequestTimer = null;
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, "Error canceling barcode request timer", e);
            }
        }
    }

    /**
     * 앱 리셋 타이머 태스크 중지
     */
    private void stopAppResetTimerTask() {
        if (appResetTimerTask != null) {
            try {
                appResetTimerTask.cancel();
                appResetTimerTask = null;
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, "Error canceling app reset timer task", e);
            }
        }
    }

    /**
     * USB 폴링 타이머 중지
     */
    private void stopUsbPollingTimer() {
        try {
            stopUsbPolling();
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, "Error stopping USB polling timer", e);
        }
    }

    /**
     * 헤더 배경색을 red로 설정
     */
    private void updateHeaderBackgroundColorToRed() {
        runOnUiThread(() -> {
            try {
                if (binding == null) {
                    return;
                }

                // ll_top_header LinearLayout 가져오기
                android.widget.LinearLayout llTopHeader = binding.llTopHeader;
                if (llTopHeader == null) {
                    return;
                }

                System.out.println(">>>>>>>>>>>>>>>>> Setting header background color to red for control mode BBBB");
                // 헤더 배경색을 red로 설정
                llTopHeader.setBackgroundColor(0xFFFF0000); // red
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, "Error updating header background color to red", e);
            }
        });
    }

    /**
     * 제어 모드 버튼 상태 업데이트 (색상 및 아이콘)
     */
    private void updateControlModeButton() {
        runOnUiThread(() -> {
            try {
                if (fab_control_mode == null) {
                    return;
                }

                if (isControlOn) {
                    // 제어 ON 상태: 주황색 배경
                    fab_control_mode.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFF0000)); // #FF9800
                    fab_control_mode.setImageResource(R.drawable.remote_gen_24dp_ffffff_fill0_wght400_grad0_opsz24);
                } else {
                    // 제어 OFF 상태: 기본 파란색 배경
                    fab_control_mode.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.blue_for_ovio)));
                    fab_control_mode.setImageResource(R.drawable.remote_gen_24dp_ffffff_fill0_wght400_grad0_opsz24);
                }
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, "Error updating control mode button", e);
            }
        });
    }

    /**
     * 제어 모드 상태 가져오기
     * @return 제어 모드 활성화 여부
     */
    public boolean isControlMode() {
        return isControlMode;
    }

    /**
     * 제어 ON/OFF 상태 가져오기
     * @return 제어 ON 여부
     */
    public boolean isControlOn() {
        return isControlOn;
    }

    /**
     * 제어 요청 사용자 접근 알림 표시 (제어 ON 상태일 때만)
     */
    public void notifyControlRequestAccess() {
        if (!isControlOn || !isControlMode) {
            return; // 제어 ON 상태가 아니면 표시하지 않음
        }

        runOnUiThread(() -> {
            try {
                if (tvUnitId == null) {
                    return;
                }

                // 원래 키트 번호 저장 (최초 1회만)
                if (originalUnitIdText == null) {
                    originalUnitIdText = tvUnitId.getText() != null ? tvUnitId.getText().toString() : "";
                }

                /*
                // 키트 번호와 제어 요청 메시지 표시
                String displayText = originalUnitIdText;
                if (!displayText.isEmpty()) {
                    displayText = " 제어 사용자 접근";
                } else {
                    displayText = " 제어 사용자 접근";
                }
                */

                String displayText = " 제어 사용자 접근";

                tvUnitMessage.setText(displayText);
                // tvUnitId.setText(displayText);

                /*
                // 5초 후 원래 텍스트로 복구
                if (controlRequestAccessHandler != null) {
                    controlRequestAccessHandler.removeCallbacks(controlRequestAccessRunnable);
                }
                controlRequestAccessHandler.postDelayed(controlRequestAccessRunnable, 5000);
                */
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, "Error displaying control request access message", e);
            }
        });
    }

    /**
     * 제어 요청 접근 메시지 삭제 (제어 OFF 상태가 될 때)
     */
    public void clearControlRequestAccessMessage() {
        runOnUiThread(() -> {
            try {
                if (tvUnitMessage != null) {
                    tvUnitMessage.setText("");
                }

                // 예약된 복구 작업도 취소
                if (controlRequestAccessHandler != null) {
                    controlRequestAccessHandler.removeCallbacks(controlRequestAccessRunnable);
                }
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, "Error clearing control request access message", e);
            }
        });
    }

    /**
     * 제어 모드에서 블루투스 응답 대기 시작
     * @param command 전송한 명령
     */
    public void startWaitingForControlResponse(String command) {
        synchronized (controlResponseLock) {
            if (!isControlMode || !isControlOn) {
                return; // 제어 모드가 아니면 대기하지 않음
            }

            waitingForControlResponse.set(true);
            pendingControlCommand = command;

            // 기존 타임아웃 타이머 정리
            stopControlResponseTimeout();

            // 타임아웃 타이머 시작
            controlResponseTimeoutTimer = new Timer("ControlResponseTimeout");
            controlResponseTimeoutTask = new TimerTask() {
                @Override
                public void run() {
                    synchronized (controlResponseLock) {
                        if (waitingForControlResponse.get()) {
                            waitingForControlResponse.set(false);
                            String timeoutCommand = pendingControlCommand;
                            pendingControlCommand = null;

                            // 타임아웃 다이얼로그 표시
                            scheduleUiUpdate(() -> {
                                showControlResponseDialog(timeoutCommand, null, true);
                            });

                            logWarn(LogManager.LogCategory.BT, "Control mode response timeout for command: " + timeoutCommand);
                        }
                    }
                }
            };
            controlResponseTimeoutTimer.schedule(controlResponseTimeoutTask, CONTROL_RESPONSE_TIMEOUT_MS);

            logInfo(LogManager.LogCategory.BT, "Started waiting for control response: " + command);
        }
    }

    /**
     * 제어 모드 응답 타임아웃 타이머 중지
     */
    private void stopControlResponseTimeout() {
        synchronized (controlResponseLock) {
            if (controlResponseTimeoutTimer != null) {
                controlResponseTimeoutTimer.cancel();
                controlResponseTimeoutTimer.purge();
                controlResponseTimeoutTimer = null;
            }
            if (controlResponseTimeoutTask != null) {
                controlResponseTimeoutTask.cancel();
                controlResponseTimeoutTask = null;
            }
        }
    }

    /**
     * 제어 모드에서 블루투스 응답 처리
     * @param response 수신한 응답 메시지
     */
    private void handleControlModeResponse(String response) {
        synchronized (controlResponseLock) {
            if (!waitingForControlResponse.get()) {
                return; // 응답 대기 중이 아니면 무시
            }

            // 타임아웃 타이머 중지
            stopControlResponseTimeout();

            // 응답 정보 저장
            String command = pendingControlCommand;
            String responseMessage = response;

            // 대기 상태 해제
            waitingForControlResponse.set(false);
            pendingControlCommand = null;

            // 다이얼로그 표시
            scheduleUiUpdate(() -> {
                showControlResponseDialog(command, responseMessage, false);
            });

            logInfo(LogManager.LogCategory.BT, "Control mode response received: " + responseMessage + " for command: " + command);
        }
    }

    /**
     * 제어 모드 응답 다이얼로그 표시
     * @param command 전송한 명령
     * @param response 수신한 응답 (null이면 타임아웃)
     * @param isTimeout 타임아웃 여부
     */
    private void showControlResponseDialog(String command, String response, boolean isTimeout) {
        try {
            if (clDialogForPreprocess == null || tvDialogMessage == null) {
                logWarn(LogManager.LogCategory.BT, "Dialog components not initialized");
                return;
            }

            // 웹 화면에서 호출한 경우 (제어 모드일 때)
            boolean isFromWebControl = isControlMode && isControlOn;

            if (isFromWebControl) {
                // 원래 설정 저장 (최초 1회만)
                if (!isControlModeDialogConfigured) {
                    // tvDialogMessage의 원래 텍스트 크기 저장
                    originalDialogMessageTextSize = tvDialogMessage.getTextSize() / getResources().getDisplayMetrics().scaledDensity;

                    // tv_dialog_barcode의 원래 visibility 저장
                    if (tv_dialog_barcode != null) {
                        originalDialogBarcodeVisibility = tv_dialog_barcode.getVisibility();
                    }

                    // tv_dialog_title의 원래 텍스트 저장
                    if (tv_dialog_title != null) {
                        originalDialogTitleText = tv_dialog_title.getText() != null ? tv_dialog_title.getText().toString() : "";
                    }

                    isControlModeDialogConfigured = true;
                }

                // 제어 모드용 설정 적용
                // 1. tvDialogMessage 폰트 크기 작게 설정 (예: 14sp)
                tvDialogMessage.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f);

                // 2. tv_dialog_barcode visibility를 GONE으로 설정
                if (tv_dialog_barcode != null) {
                    tv_dialog_barcode.setVisibility(GONE);
                }

                // 3. tv_dialog_title 텍스트를 '제어 모드 검사'로 설정
                if (tv_dialog_title != null) {
                    tv_dialog_title.setText(getStringResource("ui.message.control_mode_test"));
                }
            } else {
                // 이 외의 경우 원래 설정으로 복구
                if (isControlModeDialogConfigured) {
                    // tvDialogMessage 원래 텍스트 크기로 복구
                    if (originalDialogMessageTextSize > 0) {
                        tvDialogMessage.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, originalDialogMessageTextSize);
                    }

                    // tv_dialog_barcode 원래 visibility로 복구
                    if (tv_dialog_barcode != null && originalDialogBarcodeVisibility != GONE) {
                        int visibility = originalDialogBarcodeVisibility;
                        switch (visibility) {
                            case VISIBLE:
                                tv_dialog_barcode.setVisibility(VISIBLE);
                                break;
                            case INVISIBLE:
                                tv_dialog_barcode.setVisibility(INVISIBLE);
                                break;
                            case GONE:
                                tv_dialog_barcode.setVisibility(GONE);
                                break;
                        }
                    }

                    // tv_dialog_title 원래 텍스트로 복구
                    if (tv_dialog_title != null && originalDialogTitleText != null) {
                        tv_dialog_title.setText(originalDialogTitleText);
                    }

                    isControlModeDialogConfigured = false;
                }
            }

            String message;
            if (isTimeout) {
                message = getStringResource("ui.message.command_timeout", command);
            } else {
                message = getStringResource("ui.message.command_response", command, (response != null ? response : getStringResource("ui.label.none")));
            }

            tvDialogMessage.setText(message);
            clDialogForPreprocess.setVisibility(VISIBLE);

            // 5초 후 자동으로 닫기
            mainHandler.postDelayed(() -> {
                if (clDialogForPreprocess != null) {
                    clDialogForPreprocess.setVisibility(GONE);
                    tvDialogMessage.setText("");

                    // 다이얼로그 닫을 때 원래 설정으로 복구 (제어 모드가 아닌 경우)
                    if (!isControlMode || !isControlOn) {
                        if (isControlModeDialogConfigured) {
                            // tvDialogMessage 원래 텍스트 크기로 복구
                            if (originalDialogMessageTextSize > 0) {
                                tvDialogMessage.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, originalDialogMessageTextSize);
                            }

                            // tv_dialog_barcode 원래 visibility로 복구
                            if (tv_dialog_barcode != null && originalDialogBarcodeVisibility != GONE) {
                                tv_dialog_barcode.setVisibility(originalDialogBarcodeVisibility);
                            }

                            // tv_dialog_title 원래 텍스트로 복구
                            if (tv_dialog_title != null && originalDialogTitleText != null) {
                                tv_dialog_title.setText(originalDialogTitleText);
                            }

                            isControlModeDialogConfigured = false;
                        }
                    }
                }
            }, 5000);

        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, "Failed to show control response dialog", e);
        }
    }

    /**
     * 제어 모드에서 검사 항목 실행 (일반 모드와 동일한 검사 로직 수행)
     * @param command 실행할 명령어
     */
    public void executeControlModeTestItem(String command) {
        if (!isControlMode || !isControlOn) {
            logWarn(LogManager.LogCategory.BT, "Not in control mode, cannot execute control test item");
            return;
        }

        if (arrTestItems == null || arrTestItems.length == 0) {
            logWarn(LogManager.LogCategory.BT, "arrTestItems not initialized, cannot execute control test item");
            return;
        }

        // 명령어로부터 검사 항목 인덱스 찾기
        int foundIndex = -1;
        for (int i = 0; i < arrTestItems.length; i++) {
            if (arrTestItems[i][1].equals(command)) {
                foundIndex = i;
                break;
            }
        }

        if (foundIndex == -1) {
            logWarn(LogManager.LogCategory.BT, "Test item not found for command: " + command);
            return;
        }

        // 기존 제어 모드 검사 타이머 중지
        stopControlTestTimer();

        // 제어 모드 검사 항목 정보 설정
        synchronized (controlTestTimerLock) {
            controlTestItemIdx = foundIndex;
            controlTestItemCounter = 0;
            controlCurrentTestItem = command;
            controlTestTimerRunning.set(true);
        }

        logInfo(LogManager.LogCategory.BT, "Starting control mode test item: " + command + " (index: " + foundIndex + ", duration: " + arrTestItems[foundIndex][2] + " seconds)");

        // 제어 모드 검사 타이머 시작
        try {
            controlTestTimer = new Timer("ControlTestTimer");
            controlTestTimerTask = new TimerTask() {
                @Override
                public void run() {
                    synchronized (controlTestTimerLock) {
                        if (!controlTestTimerRunning.get() || controlTestItemIdx == -1) {
                            return;
                        }

                        // 블루투스 연결 상태 확인
                        if (!btConnected || btSocket == null || !btSocket.isConnected()) {
                            logWarn(LogManager.LogCategory.BT, "Bluetooth not connected, stopping control test timer");
                            stopControlTestTimer();
                            return;
                        }

                        // 검사 시간 확인
                        int testDuration = 0;
                        try {
                            testDuration = Integer.parseInt(arrTestItems[controlTestItemIdx][2]);
                            testDuration = (testDuration==1)?2:testDuration;
                        } catch (NumberFormatException e) {
                            logError(LogManager.LogCategory.ER, "Invalid test duration: " + arrTestItems[controlTestItemIdx][2], e);
                            stopControlTestTimer();
                            return;
                        }

                        // 명령 전송
                        sendBtMessage(controlCurrentTestItem);
                        controlTestItemCounter++;

                        logInfo(LogManager.LogCategory.BT, String.format("Control test item signal sent [C:%d/%d][S:%s]",
                                controlTestItemCounter, testDuration, controlCurrentTestItem));

                        // 검사 시간 도달 시 타이머 중지 및 검사 결과 표시
                        if (controlTestItemCounter >= testDuration) {
                            logInfo(LogManager.LogCategory.BT, "Control test item completed: " + controlCurrentTestItem);
                            stopControlTestTimer();

                            // 제어 모드 검사 결과 다이얼로그 표시
                            showControlTestResultDialog();
                        }
                    }
                }
            };

            // 1초마다 실행 (일반 모드와 동일)
            controlTestTimer.schedule(controlTestTimerTask, 0, Constants.Timeouts.TIMER_INTERVAL_MS);

        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, "Failed to start control test timer", e);
            synchronized (controlTestTimerLock) {
                controlTestTimerRunning.set(false);
                controlTestItemIdx = -1;
                controlTestItemCounter = 0;
                controlCurrentTestItem = null;
            }
        }
    }

    /**
     * 제어 모드 검사 타이머 중지
     */
    private void stopControlTestTimer() {
        synchronized (controlTestTimerLock) {
            if (controlTestTimer != null) {
                controlTestTimer.cancel();
                controlTestTimer.purge();
                controlTestTimer = null;
            }
            if (controlTestTimerTask != null) {
                controlTestTimerTask.cancel();
                controlTestTimerTask = null;
            }
            controlTestTimerRunning.set(false);
            // 검사 정보는 다이얼로그 표시 후 초기화
        }
    }

    /**
     * 재검사 등으로 제어 모드에서 일반 모드로 강제 전환할 때 사용
     */
    private void forceExitControlModeForRestart() {
        // 상태 플래그 초기화
        isControlMode = false;
        isControlOn = false;
        controlOwnerIsAndroidApp = false;
        controlModeReady = false;
        controlSt0101SuccessCount = 0;
        waitingForControlResponse.set(false);
        pendingControlCommand = null;

        // 타이머/검사 정보 정리
        stopControlTestTimer();
        clearControlTestInfo();

        // 헤더 색상 복구
        updateHeaderBackgroundColor(false);

        // 서버에 제어 소유자 정보 초기화 통보
        notifyWebServerControlOwner(false);

        logInfo(LogManager.LogCategory.PS, "Force exit control mode due to test restart");
    }

    /**
     * 제어 모드 검사 정보 초기화
     */
    private void clearControlTestInfo() {
        synchronized (controlTestTimerLock) {
            controlTestItemIdx = -1;
            controlTestItemCounter = 0;
            controlCurrentTestItem = null;
            controlTestReceiveCommand = null;
            controlTestReceiveResponse = null;
            controlTestResultValue = null;
            controlTestResult = null;
        }
    }

    /**
     * 제어 모드 검사 결과 다이얼로그 표시 (새로운 제어 모드용 다이얼로그)
     */
    private void showControlModeTestResultDialog() {
        if (!isControlMode || !isControlOn) {
            return; // 제어 모드가 아니면 표시하지 않음
        }

        synchronized (controlTestTimerLock) {
            if (controlCurrentTestItem == null) {
                logWarn(LogManager.LogCategory.BT, "Cannot show control mode test result dialog: no test item");
                return;
            }

            try {
                // 제어 모드 검사 정보 가져오기
                String command = controlCurrentTestItem;
                String receiveCommand = controlTestReceiveCommand != null ? controlTestReceiveCommand : "";
                String receiveResponse = controlTestReceiveResponse != null ? controlTestReceiveResponse : "";
                String resultValue = controlTestResultValue != null ? controlTestResultValue : "";
                String result = controlTestResult != null ? controlTestResult : "";

                // 다이얼로그 메시지 구성
                StringBuilder message = new StringBuilder();
                message.append(getStringResource("ui.message.control_command", command)).append("\n");
                message.append(getStringResource("ui.message.received_command", receiveCommand)).append("\n");

                if (!receiveResponse.isEmpty()) {
                    message.append(getStringResource("ui.message.received_response", receiveResponse)).append("\n");
                }

                // 소비전력 또는 온도 정보 표시
                if (!resultValue.isEmpty()) {
                    if (command.contains(Constants.TestItemCodes.CM0101) ||
                            command.contains(Constants.TestItemCodes.HT0101) ||
                            command.contains(Constants.TestItemCodes.PM0101) ||
                            command.contains(Constants.TestItemCodes.SV0101) ||
                            command.contains(Constants.TestItemCodes.SV0201) ||
                            command.contains(Constants.TestItemCodes.SV0301) ||
                            command.contains(Constants.TestItemCodes.SV0401)) {
                        message.append(getStringResource("ui.message.power_consumption", resultValue)).append("\n");
                    } else if (command.contains(Constants.TestItemCodes.TA0101) ||
                            command.contains(Constants.TestItemCodes.TA0201) ||
                            command.contains(Constants.TestItemCodes.TA0301)) {
                        message.append(getStringResource("ui.message.temperature", resultValue)).append("\n");
                    } else {
                        message.append(getStringResource("ui.message.result_value", resultValue)).append("\n");
                    }
                }

                if (!result.isEmpty()) {
                    message.append(getStringResource("ui.message.test_result", result));
                }

                final String finalMessage = message.toString();

                // 제어 모드용 다이얼로그 표시
                scheduleUiUpdate(() -> {
                    try {
                        if (clDialogForPreprocess == null || tvDialogMessage == null) {
                            logWarn(LogManager.LogCategory.BT, "Dialog components not initialized");
                            return;
                        }

                        // 웹 화면에서 호출한 경우 (제어 모드일 때)
                        boolean isFromWebControl = isControlMode && isControlOn;

                        if (isFromWebControl) {
                            // 원래 설정 저장 (최초 1회만)
                            if (!isControlModeDialogConfigured) {
                                // tvDialogMessage의 원래 텍스트 크기 저장
                                originalDialogMessageTextSize = tvDialogMessage.getTextSize() / getResources().getDisplayMetrics().scaledDensity;

                                // tv_dialog_barcode의 원래 visibility 저장
                                if (tv_dialog_barcode != null) {
                                    originalDialogBarcodeVisibility = tv_dialog_barcode.getVisibility();
                                }

                                // tv_dialog_title의 원래 텍스트 저장
                                if (tv_dialog_title != null) {
                                    originalDialogTitleText = tv_dialog_title.getText() != null ? tv_dialog_title.getText().toString() : "";
                                }

                                isControlModeDialogConfigured = true;
                            }

                            // 제어 모드용 설정 적용
                            // 1. tvDialogMessage 폰트 크기 작게 설정 (예: 14sp)
                            // tvDialogMessage.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f);

                            // 2. tv_dialog_barcode visibility를 GONE으로 설정
                            if (tv_dialog_barcode != null) {
                                // tv_dialog_barcode.setVisibility(GONE);
                            }

                            // 3. tv_dialog_title 텍스트를 '제어 모드 검사'로 설정
                            if (tv_dialog_title != null) {
                                tv_dialog_title.setText(getStringResource("ui.message.control_mode_test"));
                            }
                        }

                        tvDialogMessage.setText(finalMessage);
                        clDialogForPreprocess.setVisibility(VISIBLE);

                        // 5초 후 자동으로 닫기
                        mainHandler.postDelayed(() -> {
                            if (clDialogForPreprocess != null) {
                                clDialogForPreprocess.setVisibility(GONE);
                                tvDialogMessage.setText("");

                                // 제어 모드 검사 정보 초기화
                                clearControlTestInfo();

                                // 다이얼로그 닫을 때 원래 설정으로 복구 (제어 모드가 아닌 경우)
                                if (!isControlMode || !isControlOn) {
                                    if (isControlModeDialogConfigured) {
                                        // tvDialogMessage 원래 텍스트 크기로 복구
                                        if (originalDialogMessageTextSize > 0) {
                                            tvDialogMessage.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, originalDialogMessageTextSize);
                                        }

                                        // tv_dialog_barcode 원래 visibility로 복구
                                        if (tv_dialog_barcode != null && originalDialogBarcodeVisibility != GONE) {
                                            tv_dialog_barcode.setVisibility(originalDialogBarcodeVisibility);
                                        }

                                        // tv_dialog_title 원래 텍스트로 복구
                                        if (tv_dialog_title != null && originalDialogTitleText != null) {
                                            tv_dialog_title.setText(originalDialogTitleText);
                                        }

                                        isControlModeDialogConfigured = false;
                                    }
                                }
                            }
                        }, 5000);

                        logInfo(LogManager.LogCategory.BT, "Control mode test result dialog shown: " + command);
                    } catch (Exception e) {
                        logError(LogManager.LogCategory.ER, "Failed to show control mode test result dialog", e);
                    }
                });
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, "Failed to show control mode test result dialog", e);
            }
        }
    }

    /**
     * 제어 모드 검사 결과 다이얼로그 표시 (기존 메서드 - 호환성 유지)
     */
    private void showControlTestResultDialog() {
        if (!isControlMode || !isControlOn) {
            return; // 제어 모드가 아니면 표시하지 않음
        }

        if (listItemAdapter == null || controlTestItemIdx < 0 || controlTestItemIdx >= listItemAdapter.getCount()) {
            logWarn(LogManager.LogCategory.BT, "Cannot show control test result dialog: invalid test item index");
            return;
        }

        try {
            // 해당 검사 항목의 결과 확인
            VoTestItem testItem = (VoTestItem) listItemAdapter.getItem(controlTestItemIdx);
            String testResult = testItem.getTest_item_result();
            String testCheckValue = testItem.getTest_result_check_value();

            // 검사 결과 카운트 업데이트
            recalcTestCountsFromAdapter(listItemAdapter);

            // 검사 결과 다이얼로그 표시
            scheduleUiUpdate(() -> {
                try {
                    if (clTestResult == null || tvPopupProcessResult == null) {
                        logWarn(LogManager.LogCategory.BT, "Test result dialog components not initialized");
                        return;
                    }

                    // 검사 결과 텍스트 설정
                    if (testNgCnt > 0) {
                        tvPopupProcessResult.setText(Constants.ResultStatus.NG);
                        tvPopupProcessResult.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, Constants.UI.TEXT_SIZE_LARGE_SP);
                        if (tvPopupProcessResultCompValue != null) {
                            tvPopupProcessResultCompValue.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, Constants.UI.TEXT_SIZE_MEDIUM_SP);
                        }
                        if (tvPopupProcessResultHeaterValue != null) {
                            tvPopupProcessResultHeaterValue.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, Constants.UI.TEXT_SIZE_MEDIUM_SP);
                        }
                    } else {
                        tvPopupProcessResult.setText(Constants.ResultStatus.OK);
                    }

                    // 검사 결과 값 설정
                    if (tvPopupProcessResultCompValue != null && testItem.getTest_item_command().contains(Constants.TestItemCodes.CM0101)) {
                        tvPopupProcessResultCompValue.setText(testCheckValue);
                    }
                    if (tvPopupProcessResultHeaterValue != null && testItem.getTest_item_command().contains(Constants.TestItemCodes.HT0101)) {
                        tvPopupProcessResultHeaterValue.setText(testCheckValue);
                    }

                    // OK/NG 카운트 표시
                    if (tvTestOkCnt != null) {
                        tvTestOkCnt.setText(String.valueOf(testOkCnt));
                    }
                    if (tvTestNgCnt != null) {
                        tvTestNgCnt.setText(String.valueOf(testNgCnt));
                    }

                    // 검사 결과 다이얼로그 표시
                    clTestResult.setVisibility(VISIBLE);

                    logInfo(LogManager.LogCategory.BT, "Control test result dialog shown: " + testResult + " (OK: " + testOkCnt + ", NG: " + testNgCnt + ")");
                } catch (Exception e) {
                    logError(LogManager.LogCategory.ER, "Failed to show control test result dialog", e);
                }
            });
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, "Failed to show control test result dialog", e);
        }
    }

    /**
     * 웹서버에 제어 소유자 정보 전달 (안드로이드 앱에서 제어 ON/OFF 시)
     * @param isAndroidAppOwner true: 안드로이드 앱에서 제어 ON, false: 제어 OFF (소유자 초기화)
     */
    private void notifyWebServerControlOwner(boolean isAndroidAppOwner) {
        new Thread(() -> {
            try {
                // KioskModeApplication에서 WebServer 인스턴스 가져오기
                android.app.Application app = getApplication();
                if (app instanceof itf.com.app.lms.kiosk.KioskModeApplication) {
                    itf.com.app.lms.kiosk.KioskModeApplication kioskApp =
                            (itf.com.app.lms.kiosk.KioskModeApplication) app;
                    itf.com.app.lms.util.WebServer webServer = kioskApp.getWebServer();

                    if (webServer != null && webServer.isRunning()) {
                        int port = webServer.getActualPort();
                        String url = "http://127.0.0.1:" + port + "/test/control/owner";

                        java.net.URL obj = new java.net.URL(url);
                        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) obj.openConnection();

                        try {
                            connection.setRequestMethod("POST");
                            connection.setDoOutput(true);
                            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                            connection.setConnectTimeout(1000); // 1초 타임아웃
                            connection.setReadTimeout(1000);

                            // POST 데이터 전송
                            String postData = "is_android_app=" + (isAndroidAppOwner ? "true" : "false");
                            java.io.OutputStream os = connection.getOutputStream();
                            os.write(postData.getBytes("UTF-8"));
                            os.flush();
                            os.close();

                            int responseCode = connection.getResponseCode();
                            if (responseCode == 200) {
                                logInfo(LogManager.LogCategory.PS, "Control owner notification sent to web server: " + isAndroidAppOwner);
                            } else {
                                logWarn(LogManager.LogCategory.PS, "Failed to send control owner notification, response code: " + responseCode);
                            }
                        } finally {
                            connection.disconnect();
                        }
                    }
                }
            } catch (Exception e) {
                // 웹서버에 신호를 보내지 못해도 계속 진행
                logWarn(LogManager.LogCategory.PS, "Failed to notify web server of control owner: " + e.getMessage());
            }
        }).start();
    }

    /**
     * 웹 클라이언트에 종료 신호 전달
     * @param reason 종료 원인 ("FAB_CLOSE", "CONTROL_MODE_EXIT", "UNEXPECTED")
     */
    private void notifyWebClientShutdown(String reason) {
        new Thread(() -> {
            try {
                // KioskModeApplication에서 WebServer 인스턴스 가져오기
                android.app.Application app = getApplication();
                if (app instanceof itf.com.app.lms.kiosk.KioskModeApplication) {
                    itf.com.app.lms.kiosk.KioskModeApplication kioskApp =
                            (itf.com.app.lms.kiosk.KioskModeApplication) app;
                    itf.com.app.lms.util.WebServer webServer = kioskApp.getWebServer();

                    if (webServer != null && webServer.isRunning()) {
                        int port = webServer.getActualPort();
                        String url = "http://127.0.0.1:" + port + "/test/shutdown/notify";

                        java.net.URL obj = new java.net.URL(url);
                        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) obj.openConnection();

                        try {
                            connection.setRequestMethod("POST");
                            connection.setDoOutput(true);
                            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                            connection.setConnectTimeout(1000); // 1초 타임아웃
                            connection.setReadTimeout(1000);

                            // POST 데이터 전송
                            String postData = "reason=" + java.net.URLEncoder.encode(reason, "UTF-8");
                            java.io.OutputStream os = connection.getOutputStream();
                            os.write(postData.getBytes("UTF-8"));
                            os.flush();
                            os.close();

                            int responseCode = connection.getResponseCode();
                            if (responseCode == 200) {
                                logInfo(LogManager.LogCategory.PS, "Shutdown notification sent to web client: " + reason);
                            } else {
                                logWarn(LogManager.LogCategory.PS, "Failed to send shutdown notification, response code: " + responseCode);
                            }
                        } finally {
                            connection.disconnect();
                        }
                    }
                }
            } catch (Exception e) {
                // 웹서버에 신호를 보내지 못해도 애플리케이션 종료는 계속 진행
                logWarn(LogManager.LogCategory.PS, "Failed to notify web client of shutdown: " + e.getMessage());
            }
        }).start();
    }

    // 제어 요청 접근 메시지 관련 변수
    private String originalUnitIdText = null;
    private final Handler controlRequestAccessHandler = new Handler(Looper.getMainLooper());
    private final Runnable controlRequestAccessRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (tvUnitMessage != null) {
                    System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>> controlRequestAccessRunnable ");
                    tvUnitMessage.setText("");
                }
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, "Error clearing control request access message", e);
            }
        }
    };

    // 통합된 스케줄링 Handler: 블루투스 재연결, HTTP 통신 스케줄링
    private final Handler schedulerHandler = new Handler(Looper.getMainLooper());
    private Runnable btReconnectRunnable = null;
    private long btReconnectDelayMs = Constants.Timeouts.BT_LIST_TIMER_INTERVAL_MS;
    private static final long BT_RECONNECT_DELAY_MAX_MS = Constants.Timeouts.BT_LIST_TIMER_INTERVAL_MS * 60;
    private boolean btConnectionInProgress = false;

    // 블루투스 연결 인디케이터
    private boolean btConnectionIndicatorRunning = false;
    private Runnable btConnectionIndicatorRunnable = null;
    private static final long BT_CONNECTION_INDICATOR_INTERVAL_MS = 500; // 0.5초마다 색상 변경

    public List<Map<String, String>> lstTestResult = null;
    private static List<Map<String, String>> lstTestTemperature = null;
    private static Map<String, String> mapTestTemperature = null;

    public TextView tvAlertMessage = null;
    private TextView btnTestHistoryList = null;

    private ConstraintLayout clAlert = null;
    private Button btnAlertClose = null;

    // 다이얼로그 관리자
    private DialogManager dialogManager = null;


    static String[][] arrTestItems = null;
    static String[][] arrTestItemsZig = null;
    String productSerialNo = "";
    static String testProcessId = "";

    static public String serverIp = "";
    static public String serverDomain = "";
    static public String serverResetIp = "";


    static private String btDeviceName = "";
    static private String btDeviceAddr = "";
    // Static Activity 참조를 WeakReference로 변경하여 메모리 누수 방지
    private static WeakReference<ActivityModelTestProcess> mainActivityRef = null;

    static int usbConnTryCnt = 0;
    static boolean usbConnPermissionGranted = false;

    public static final String REGEXP_CHAR_NUM = "^[0-9a-zA-Zㄱ-ㅎ가-힣]*$";
    public List<Map<String, String>> lstData = null;
    public List<Map<String, String>> lstDataTmp = null;
    static BluetoothDevice deviceSelected = null;
    private String dataBuffer = "";
    private String dataBufferTmp = "";

    String currentTimestamp = "";

    String[][] arrTestModels = Constants.TestModels.ARR_TEST_MODELS;

    // DBHelper dbHelper;


    int temperature12Bit = 0;
    String temperatureTmp = Constants.InitialValues.TEMPERATURE_TMP;

    String valueWatt = Constants.InitialValues.VALUE_WATT;
    String lowerValueWatt = Constants.InitialValues.LOWER_VALUE_WATT;
    String upperValueWatt = Constants.InitialValues.UPPER_VALUE_WATT;
    // String valueWattLog = "";


    public boolean productSerialNoYn = false;
    public static List<Map<String, String>> btInfoList = null;
    private final Map<String, String> btInfoMap = null;

    String urlStr = "";
    String urlTestTaskStr = "";
    ActivityModelTestProcess.RequestTestTaskThreadAsync testTaskThread = null;
    static String urlStrBarcode = "";

    // HTTP Handler 배치 처리를 위한 디바운싱 변수
    private static final Object HTTP_HANDLER_LOCK = new Object();
    private Runnable pendingHttpTask = null;
    private static final long HTTP_DEBOUNCE_DELAY_MS = 100; // 100ms 디바운스

    // private WebServer server;
    public static final int WS_PORT = 8080;
    public static String ipAddress;


    static int lastTestIdx = 0;
    public static boolean btSearchOnOff = false;
    public static int sensorNgCount = 0;

    private SharedPreferences preferences;
    static SharedPreferences.Editor test_info = null;

    public double currentPumpWattValue = 0;
    public String currentPumpWattValueArr = "";
    public double currentCompWattValue = 0;
    public double currentHeaterWattValue = 0;
    public double currentValveWattValue = 0;

    public double currentCompWattValueProp = 0;
    public double currentHeaterWattValueProp = 0;

    public TextView tv_current_version = null;


    double dblValTemp = 0;

    List<Map<String, String>> lstSpecData = null;
    // List<Map<String, String>> lstWattData = null;

    String lmsTestSeq = getCurrentDatetime(timestampFormat);
    String lmsTestTimestamp = getCurrentDatetime(datetimeFormat);

    int unit_no = 0;
    String unit_id = "";


    TableRow trPreprocessContent = null;


    Handler hdlTestEntrance = null;
    Message msgTestEntrance = null;
    Handler hdlTestTimerTask = null;
    Message msgTestTimerTask = null;

    boolean testProcessStarted = false;
    int uvCheckCnt = 0;


    int preprocessColor = 0;

    private WindowManager.LayoutParams params;
    private float brightness; // 밝기값은 float형으로 저장되어 있습니다.
    private UsbService usbService;
    private ActivityModelTestProcess.UsbHandler usbHandler;
    private UsbCommandQueue usbCommandQueue;
    // USB 응답을 받기 위한 Future 저장소
    private final java.util.concurrent.ConcurrentHashMap<String, CompletableFuture<String>> pendingUsbResponses = new java.util.concurrent.ConcurrentHashMap<>();
    private final Object usbResponseLock = new Object();
    // public List<Map<String, String>> lstWatt = new ArrayList<>();
    public List<Integer> lstWatt = new ArrayList<>();
    public List<String[]> lstMapWattTemp = new ArrayList<>();
    // private Map<String, String[]> mapWattTemp = null;
    private String[] wattTemp = null;

    public HttpURLConnection connection = null;
    // private final Map<String, Double> latestWattByCommand = new ConcurrentHashMap<>();


    boolean finishedCorrectly = false;
    Timer tmrFinishedRestart = null;
    TimerTask ttFinishedRestart = null;
    // Phase 3: Timer 중복 생성 방지 메커니즘
    private final AtomicBoolean finishedRestartTimerRunning = new AtomicBoolean(false);
    private final Object finishedRestartTimerLock = new Object();

    int restartCntFinished = 0;
    int restartCntUnfinished = 0;
    int restartCntMargin = 0;

    public static final int MULTIPLE_PERMISSIONS = 1801;
    private static final String[] MODERN_BT_PERMISSIONS = {
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
    };
    private static final String[] LEGACY_BT_PERMISSIONS = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
    };

    // Android 13 (API 33) 이상: 미디어 권한
    private static final String[] MODERN_MEDIA_PERMISSIONS = {
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
    };

    // Android 11-12 (API 30-32): 스토리지 권한 (READ만)
    // 중요: Android 11 (API 30) 이상에서는 WRITE_EXTERNAL_STORAGE 권한이 더 이상 효과가 없음
    // Scoped Storage 도입으로 인해 항상 거부(-1)를 반환하므로 요청하지 않음
    private static final String[] LEGACY_STORAGE_PERMISSIONS_API30_UP = {
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    // Android 10 이하 (API 29 이하): 스토리지 권한 (READ + WRITE)
    private static final String[] LEGACY_STORAGE_PERMISSIONS_API29_DOWN = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private boolean btPermissionsGranted = false;
    private boolean permissionRequestInProgress = false;
    private boolean permissionDialogShowing = false;
    private int permissionDenialCount = 0; // 권한 거부 횟수 추적
    private static final int MAX_PERMISSION_DENIAL_COUNT = 3; // 최대 재시도 횟수

    private enum PermissionAction {
        LIST_PAIRED_DEVICES,
        BT_RECONNECT
    }

    private final EnumSet<PermissionAction> pendingPermissionActions = EnumSet.noneOf(PermissionAction.class);

    int wattLower = 0;
    int wattUpper = 0;
    static boolean bln_test_yn = false;
    static String test_logger = Constants.Common.EMPTY_STRING;
    static String test_logger_temp = Constants.Common.EMPTY_STRING;
    String receiveCommand = "";
    int receiveCommandEmptyCnt = 0;
    String decElectricValueList = "";
    String receiveCommandResponseOK = "";
    String receiveCommandResponse = "";
    String receiveCompAgingResponse = Constants.InitialValues.RECEIVE_COMP_AGING_RESPONSE;  // 08 버전 생산라인
    // static String receiveCompAgingResponse = "";  // 09 버전 테스트
    String receiveResponseResult = Constants.InitialValues.RECEIVE_RESPONSE_RESULT;
    String receiveResponseResultTxt = "";

    Timer tmrUnfinishedRestart = null;
    TimerTask ttUnfinishedRestart = null;
    // Phase 3: Timer 중복 생성 방지 메커니즘
    private final AtomicBoolean unfinishedRestartTimerRunning = new AtomicBoolean(false);
    private final Object unfinishedRestartTimerLock = new Object();
    // Phase 3: Timer 중복 생성 방지 메커니즘
//    private final AtomicBoolean unfinishedRestartTimerRunning = new AtomicBoolean(false);
//    private final Object unfinishedRestartTimerLock = new Object();
    FloatingActionButton fab_close = null;
    FloatingActionButton fab_control_mode = null;
    int min_diff = 0;
    int reload_min_diff = 20;

    int sendingMessageCnt = 0;
    int receivedMessageCnt = 0;
    int disconnectCheckCount = 0;

    static int postTestInfoColor = 0;

    // Phase 1: Static Timer를 Instance 변수로 변경 (메모리 누수 방지)
    private Timer checkDuration = null;

    // 통합된 메인 스레드 Handler: UI 업데이트, 리소스 정보, 로그 배치 처리
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable resourceInfoRunnable = new Runnable() {
        @Override
        public void run() {
            updateResourceInfo();
            mainHandler.postDelayed(this, Constants.Timeouts.TIMER_INTERVAL_MS);
        }
    };
    private boolean isConnected = false;
    // ⚠️ CONNECTION_TIMEOUT은 Constants.Timeouts.CONNECTION_TIMEOUT으로 이동됨

    // 키오스크 모드: 시스템 UI 지속적으로 숨기기 위한 Handler

    static boolean blnBarcodeReceived = false;
    static int aTryingCnt = 0;

    String iRemoteCommandCallCondition = Constants.InitialValues.REMOTE_COMMAND_CALL_CONDITION;
    int iRemoteCommandCnt = 0;
    Timer tmrRemoteCommand = null;
    TimerTask ttRemoteCommand = null;
    // Phase 3: Timer 중복 생성 방지 메커니즘
    private final AtomicBoolean remoteCommandTimerRunning = new AtomicBoolean(false);
    private final Object remoteCommandTimerLock = new Object();

    private String decTemperatureColdValue = "";
    private String decTemperatureHotValue = "";
    private String decTemperatureValueCompStart = "";
    private boolean compAgingStarted = false;
    private String decTemperatureValueCompEnd = "";
    private String temperatureValueCompDiff = "";
    private final String currentPBAVersion = "";

    private boolean btConnected = false;
    private boolean isHandlingUsbReconnection = false;

    // Phase 1: Static Timer를 Instance 변수로 변경 (메모리 누수 방지)
    private Timer barcodeRequestTimer = null;

    private String calculatedTemperatureTmp = Constants.Common.ZERO;


    private String test_version_id = "";
    private String model_id = "";

    // LogManager 인스턴스 (싱글톤)
    private static final LogManager logManager = LogManager.getInstance();

    // DON'T FORGET to stop the server
    @Override
    public void onDestroy() {
        try {
            // ========== PHASE 1: Cleanup Managers (CRITICAL!) ==========
            cleanupManagers();
            // ========== End Manager Cleanup ==========

            super.onDestroy();
            logInfo(LogManager.LogCategory.PS, Constants.LogMessages.ON_DESTROY);

            /*
            // 모든 Timer 정리 (메모리 누수 방지)
            cleanupAllTimers();

            // 로그 배치 큐 정리
            clearLogBatchQueue();

            // WeakReference 정리 (메모리 누수 방지)
            clearMainActivityReference();

            unregisterReceiver(usbReceiver);

            // finishApplication(getApplicationContext());
            resetBluetoothSessionKeepUsb();
            */

            // ========== Handler 작업 취소 (중요: 종료 후 예약된 작업 실행 방지) ==========
            if (schedulerHandler != null) {
                schedulerHandler.removeCallbacksAndMessages(null);
            }
            if (controlRequestAccessHandler != null) {
                controlRequestAccessHandler.removeCallbacksAndMessages(null);
            }
            if (usbReconnectHandler != null) {
                usbReconnectHandler.removeCallbacksAndMessages(null);
            }
            if (usbRecoveryHandler != null) {
                usbRecoveryHandler.removeCallbacksAndMessages(null);
            }
            if (mainHandler != null) {
                mainHandler.removeCallbacksAndMessages(null);
            }
            // ========== End Handler Cleanup ==========

            // ========== 블루투스 타이머 및 커넥터 종료 ==========
            stopBtMessageTimer();
            stopBtConnectionIndicator();
            clearBluetoothReconnect();

            // 블루투스 소켓 종료
            if (btSocket != null) {
                try {
                    if (btSocket.isConnected()) {
                        btSocket.close();
                    }
                } catch (Exception e) {
                    logWarn(LogManager.LogCategory.BT, "Error closing Bluetooth socket in onDestroy: " + e.getMessage());
                }
                btSocket = null;
            }

            // 블루투스 연결 스레드 종료
            if (btConnectedThread != null) {
                try {
                    btConnectedThread.cancel();
                } catch (Exception e) {
                    logWarn(LogManager.LogCategory.BT, "Error canceling Bluetooth connected thread: " + e.getMessage());
                }
                btConnectedThread = null;
            }

            btConnected = false;
            btConnectionInProgress = false;
            // ========== End Bluetooth Cleanup ==========

            // ========== Executor 종료 ==========
            if (btWorkerExecutor != null && !btWorkerExecutor.isShutdown()) {
                btWorkerExecutor.shutdownNow();
                try {
                    if (!btWorkerExecutor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                        logWarn(LogManager.LogCategory.BT, "btWorkerExecutor did not terminate within timeout");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logWarn(LogManager.LogCategory.BT, "Interrupted while waiting for btWorkerExecutor termination");
                }
            }

            if (usbPollingExecutor != null && !usbPollingExecutor.isShutdown()) {
                usbPollingExecutor.shutdownNow();
                try {
                    if (!usbPollingExecutor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                        logWarn(LogManager.LogCategory.US, "usbPollingExecutor did not terminate within timeout");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logWarn(LogManager.LogCategory.US, "Interrupted while waiting for usbPollingExecutor termination");
                }
            }
            // ========== End Executor Cleanup ==========

            cleanupAllResources();

            // ⚠️ ViewBinding 정리: 메모리 누수 방지
            if (binding != null) {
                binding = null;
            }

            // ⚠️ UI 업데이트 배치 처리 큐 정리
            clearUiUpdateBatchQueue();

            // DialogManager 정리
            if (dialogManager != null) {
                dialogManager.cleanup();  // ⚠️ CRITICAL FIX: Use cleanup() instead of dismissAllDialogs()
                dialogManager = null;
            }

            // 소비전력 값 애니메이션 리소스 정리
            if (wattValueAnimator != null) {
                wattValueAnimator.cleanup();
                wattValueAnimator = null;
            }

            resetActivityState();

            // 서버로 결과 전달
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, Constants.ErrorMessages.ON_DESTROY_ERROR + e, e);
        }
    }

    public String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        String ipAddr = inetAddress.getHostAddress();
                        return ipAddr;
                    }
                }
            }
        } catch (SocketException e) {
            logError(LogManager.LogCategory.ER, Constants.ErrorMessages.GET_LOCAL_IP_ADDRESS_ERROR, e);
        }
        return null;
    }

    /**
     * ═══════════════════════════════════════════════════════════════════════════════
     * Enable StrictMode for ANR and Memory Leak Detection
     * ═══════════════════════════════════════════════════════════════════════════════
     * <p>
     * This method configures Android's StrictMode to detect performance violations
     * and memory leaks during development. StrictMode helps identify issues that
     * could cause ANR (Application Not Responding) errors or memory leaks.
     * <p>
     * IMPORTANT: This only runs in DEBUG builds. Zero overhead in production.
     * <p>
     * WHAT GETS DETECTED:
     * <p>
     * 1. THREAD POLICY (Performance Issues):
     * - Network operations on UI thread → Causes ANR (5+ second freeze)
     * - Disk reads on UI thread → Causes UI jank/stutter
     * - Disk writes on UI thread → Causes UI jank/stutter
     * - Custom slow calls → Operations marked with detectCustomSlowCalls
     * <p>
     * 2. VM POLICY (Memory Leaks):
     * - Leaked SQLite objects → Database cursors not closed
     * - Leaked Closeable objects → Files, streams not closed
     * - Activity leaks → Activity held in memory after destroy
     * - Leaked registrations → BroadcastReceivers not unregistered
     * <p>
     * HOW TO USE VIOLATIONS:
     * 1. Run app in DEBUG mode
     * 2. Check Logcat for "StrictMode policy violation"
     * 3. Click violation to see full stack trace
     * 4. Stack trace shows EXACT line causing violation
     * 5. Fix the code (move to background thread, close resource, etc.)
     * <p>
     * EXAMPLE VIOLATION:
     * D/StrictMode: StrictMode policy violation; ~duration=152 ms
     * android.os.StrictMode$StrictModeNetworkViolation
     * at java.net.HttpURLConnection.connect
     * at ActivityModel_0002.executeResultUpload:3173
     * <p>
     * → This tells you: Line 3173 is doing network on UI thread!
     * <p>
     * AGGRESSIVE TESTING MODE:
     * - Uncomment ".penaltyDeath()" lines below
     * - App will CRASH immediately on any violation
     * - Use ONLY during focused debugging (makes violations impossible to miss)
     * - Remember to comment it back out for normal development
     * <p>
     * PERFORMANCE IMPACT:
     * - DEBUG builds: ~2-5ms overhead per operation (negligible)
     * - RELEASE builds: 0ms (entire method not called due to BuildConfig.DEBUG check)
     * <p>
     * ═══════════════════════════════════════════════════════════════════════════════
     */
    private void enableStrictMode() {
        // ───────────────────────────────────────────────────────────────────────────
        // THREAD POLICY: Detect UI thread violations (ANR causes)
        // ───────────────────────────────────────────────────────────────────────────
        android.os.StrictMode.setThreadPolicy(
                new android.os.StrictMode.ThreadPolicy.Builder()
                        // Detect network operations on main thread
                        // → Catches: HttpURLConnection, Socket.connect(), etc.
                        // → Why: Network calls can take 5-30 seconds → ANR!
                        .detectNetwork()

                        // Detect disk reads on main thread
                        // → Catches: FileInputStream, SQLite queries, SharedPreferences.get(), etc.
                        // → Why: Disk I/O can cause 100-500ms stutter → UI jank
                        .detectDiskReads()

                        // Detect disk writes on main thread
                        // → Catches: FileOutputStream, SQLite insert/update, SharedPreferences.edit().commit()
                        // → Why: Disk writes can take 50-200ms → UI jank
                        .detectDiskWrites()

                        // Detect custom slow calls (methods marked with @SlowCall annotation)
                        // → Catches: Any method you've marked as potentially slow
                        // → Why: Helps identify your own performance bottlenecks
                        .detectCustomSlowCalls()

                        // What to do when violation detected:
                        // → penaltyLog: Write to Logcat (default, always useful)
                        .penaltyLog()

                        // → penaltyDeath: CRASH the app (uncomment for aggressive testing)
                        // ⚠️ UNCOMMENT BELOW FOR AGGRESSIVE TESTING (App will crash on violations)
                        // .penaltyDeath()

                        .build()
        );

        // ───────────────────────────────────────────────────────────────────────────
        // VM POLICY: Detect memory leaks and resource leaks
        // ───────────────────────────────────────────────────────────────────────────
        android.os.StrictMode.setVmPolicy(
                new android.os.StrictMode.VmPolicy.Builder()
                        // Detect leaked SQLite objects (cursors, databases not closed)
                        // → Catches: Cursor.close() not called, SQLiteDatabase not closed
                        // → Why: Each leaked cursor holds ~1KB-10MB of memory
                        .detectLeakedSqlLiteObjects()

                        // Detect leaked Closeable objects (files, streams not closed)
                        // → Catches: FileInputStream, OutputStream, Reader, Writer not closed
                        // → Why: Leaked file handles eventually cause "Too many open files" crashes
                        .detectLeakedClosableObjects()

                        // Detect activity leaks (activity kept in memory after destruction)
                        // → Catches: Static references to Activity, Handler leaks, anonymous inner classes
                        // → Why: Each leaked activity holds ~5-50MB (entire view hierarchy!)
                        .detectActivityLeaks()

                        // Detect leaked registrations (BroadcastReceivers, ServiceConnections not unregistered)
                        // → Catches: registerReceiver() without unregisterReceiver()
                        // → Why: Leaked receivers prevent garbage collection + can cause crashes
                        .detectLeakedRegistrationObjects()

                        // What to do when violation detected:
                        // → penaltyLog: Write to Logcat (default, always useful)
                        .penaltyLog()

                        // → penaltyDeath: CRASH the app (uncomment for aggressive testing)
                        // ⚠️ UNCOMMENT BELOW FOR AGGRESSIVE TESTING (App will crash on violations)
                        // .penaltyDeath()

                        .build()
        );

        // Log that StrictMode is active (helps confirm it's working)
        logInfo(LogManager.LogCategory.PS, "✅ StrictMode enabled: ThreadPolicy (network, disk I/O) + VmPolicy (memory leaks)");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            // ═══════════════════════════════════════════════════════════════════════════════
            // ⚠️ ANR & MEMORY LEAK DETECTION - StrictMode Configuration
            // ═══════════════════════════════════════════════════════════════════════════════
            //
            // PURPOSE: Detect performance issues and memory leaks during development
            //
            // WHEN ACTIVE: Only in DEBUG builds (BuildConfig.DEBUG = true)
            // WHEN DISABLED: Automatically disabled in RELEASE builds (zero overhead)
            //
            // WHAT IT DETECTS:
            //   1. Network operations on UI thread (causes ANR)
            //   2. File I/O operations on UI thread (causes jank/ANR)
            //   3. Memory leaks (unclosed resources, activity leaks)
            //   4. Untagged network sockets
            //
            // HOW TO USE:
            //   - Run app in DEBUG mode
            //   - Check Logcat for "StrictMode policy violation"
            //   - Click on violation to see stack trace
            //   - Fix the code path shown in stack trace
            //
            // EXAMPLE LOGCAT OUTPUT:
            //   D/StrictMode: StrictMode policy violation; ~duration=152 ms
            //       android.os.StrictMode$StrictModeNetworkViolation
            //       at java.net.HttpURLConnection.connect
            //       at ActivityModel_0002.executeResultUpload:3173
            //
            // HOW TO MAKE IT CRASH (for aggressive testing):
            //   - Uncomment ".penaltyDeath()" below
            //   - App will crash immediately on any violation
            //   - Use ONLY during focused debugging sessions
            //
            // PERFORMANCE IMPACT:
            //   - DEBUG builds: ~2-5ms overhead (negligible)
            //   - RELEASE builds: 0ms (code is completely removed)
            //
            // ═══════════════════════════════════════════════════════════════════════════════
            if (BuildConfig.DEBUG) {
                enableStrictMode();  // Configure StrictMode detection
            }

            // ⚠️ 키오스크 모드: KioskModeApplication이 자동으로 적용함 (onCreate에서 enableFullKioskMode 호출됨)

            logInfo(LogManager.LogCategory.PS, Constants.LogMessages.ON_CREATE_ENTRANCE_CHECK + entranceCheck + " / currentTestItem:" + currentTestItem);

            // ⚠️ ViewBinding 사용: findViewById 호출 제거 및 성능 최적화
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            // onCreate() - 메인 스레드
            checkAndRequestPermissionsAsync();  // 백그라운드에서 체크, 메인으로 포스팅
//            btPermissionsGranted = hasAllBluetoothPermissions();
//            if (!btPermissionsGranted) {
//                requestRuntimePermissions();
//            }

            // ⚠️ ViewBinding을 통한 View 접근: findViewById 호출 제거
            btnTestResultClose = binding.btnTestResultClose;

            tvConnectBtRamp = binding.tvConnectBtRamp;
            tvConnectPlcRamp = binding.tvConnectPlcRamp;
            tvRunWsRamp = binding.tvRunWsRamp;

            tvTestOkCnt = binding.tvTestOkCnt;
            tvTestNgCnt = binding.tvTestNgCnt;

            clAlert = binding.clAlert;
            btnAlertClose = binding.btnAlertClose;
            clTestResult = binding.clTestResult;

            clDialogForPreprocess = binding.clDialogForPreprocess;
            tvDialogMessage = binding.tvDialogMessage;

            tvUnitId = binding.tvUnitId;
            tvUnitMessage = binding.tvUnitMessage;

            tvPopupProcessResult = binding.tvPopupProcessResult;
            tvPopupProcessResultCompValue = binding.tvPopupProcessResultCompValue;
            tvPopupProcessResultHeaterValue = binding.tvPopupProcessResultHeaterValue;
            tvCompWattValue = binding.tvCompWattValue;
            tvHeaterWattValue = binding.tvHeaterWattValue;
            tvPumpWattValue = binding.tvPumpWattValue;

            tvCompValueWatt = binding.tvCompValueWatt;
            tvHeaterValueWatt = binding.tvHeaterValueWatt;
            tvPumpValueWatt = binding.tvPumpValueWatt;
            tvCompLowerValueWatt = binding.tvCompLowerValueWatt;
            tvCompUpperValueWatt = binding.tvCompUpperValueWatt;
            tvHeaterLowerValueWatt = binding.tvHeaterLowerValueWatt;
            tvHeaterUpperValueWatt = binding.tvHeaterUpperValueWatt;
            tvPumpLowerValueWatt = binding.tvPumpLowerValueWatt;
            tvPumpUpperValueWatt = binding.tvPumpUpperValueWatt;

            tvEllapsedTimeCnt = binding.tvEllapsedTimeCnt;
            tvModelName = binding.tvModelName;
            tvModelNationality = binding.tvModelNationality;
            tvSerialNo = binding.tvSerialNo;
            tvModelId = binding.tvModelId;
            tv_dialog_barcode = binding.tvDialogBarcode;
            tv_dialog_title = binding.tvDialogTitle;
            tvCurrentProcess = binding.tvCurrentProcess;
            tvTotalTimeCnt = binding.tvTotalTimeCnt;
            mBluetoothStatus = binding.bluetoothStatus;
            mReadBuffer = binding.readBuffer;
            mReadText = binding.receiveText;
            lvTestItem = binding.lstTestItem;
            tvTemperature = binding.tvTemperature;

            btnTestRestart = binding.btnTestRestart;

            tvAlertMessage = binding.tvAlertMessage;
            btnTestHistoryList = binding.btnTestHistoryList;

            tvWattValue = binding.tvWattValue;

            // ========== 소비전력 값 애니메이션 초기화 ==========
            if (tvWattValue != null) {
                wattValueAnimator = new WattValueAnimator(tvWattValue);
            }
            // ========== End Watt Value Animation Initialization ==========

            trPreprocessContent = binding.trPreprocessContent;

            // ⚠️ 중복 호출 제거됨 (764-765번 라인)

            tv_current_version = binding.tvCurrentVersion;

            cl_dialog_for_logger = binding.clDialogForLogger;    // 20240522 바코드 인식 표시
            tv_dialog_for_logger_watt = binding.tvDialogForLoggerWatt;    // 20240522 바코드 인식 표시
            tv_dialog_for_logger_temp = binding.tvDialogForLoggerTemp;    // 20240522 바코드 인식 표시

            fab_close = binding.fabClose;

            fab_close.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 웹 클라이언트에 종료 신호 전달
                    notifyWebClientShutdown("FAB_CLOSE");
                    finishApplication(getApplicationContext());
                    // resetBluetoothSessionKeepUsb();
                    // 서버로 결과 전달
                }
            });

            // 제어 모드 FAB 초기화
            fab_control_mode = binding.fabControlMode;
            if (fab_control_mode != null) {
                // 기본 상태: 제어 OFF
                isControlOn = false;
                isControlMode = false;
                updateControlModeButton();

                fab_control_mode.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        toggleControlMode();
                    }
                });
            }

            mBTArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
            mBTAdapter = BluetoothAdapter.getDefaultAdapter(); // get a handle on the bluetooth radio

            restartCntMargin = Constants.Network.RESTART_CNT_MARGIN_MULTIPLIER * Constants.Timeouts.MINUTE_TO_MULTIPLE;
            // restartCntMargin = 60;

            // String folder_name = Constants.FilePaths.FOLDER_NAME;

            mReadBuffer.setMovementMethod(ScrollingMovementMethod.getInstance());

            // WeakReference로 Activity 참조 저장 (메모리 누수 방지)
            mainActivityRef = new WeakReference<>(this);

            cl_log = binding.clLog;
            tv_log = binding.tvLog;
            finishedCorrectly = false;
            testProcessId = Constants.InitialValues.TEST_PROCESS_ID;

            tv_log.setMovementMethod(ScrollingMovementMethod.getInstance());

            // HTTP 통신 관련 runOnUiThread 최적화: scheduleUiUpdate 사용
            ActivityModelTestProcess activity = getMainActivity();
            if (activity != null && !activity.isFinishing()) {
                activity.scheduleUiUpdate(() -> {
                    switch (mode_type) {
                        case Constants.ResultStatus.MODE_TYPE_NORMAL:
                            cl_log.setVisibility(GONE);
                            tv_log.setText(Constants.Common.EMPTY_STRING);
                            break;
                        case Constants.ResultStatus.MODE_TYPE_TEST:
                            // cl_log.setVisibility(View.VISIBLE);
                            cl_log.setVisibility(GONE);
                            tv_log.setText("");
                            break;
                    }
                });
            }

            binding.getRoot().post(this::startDeferredInitialization);

            // ========== PHASE 1: Initialize Communication Managers ==========
            initializeManagers();
            // ========== End Manager Initialization ==========
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, Constants.ErrorMessages.ON_CREATE_ERROR, e);
        }
    }

    // 개선된 권한 처리 방식 (샘플 코드)

    /**
     * 백그라운드 스레드에서 권한 상태를 확인하고,
     * 필요한 경우 메인 스레드로 권한 요청을 포스팅
     */
    private void checkAndRequestPermissionsAsync() {
        runOnBtWorker(() -> {
            // 1. 백그라운드에서 권한 상태 확인 (스레드 안전)
            List<String> missingPermissions = new ArrayList<>();
            for (String permission : getRequiredPermissions()) {
                if (ActivityCompat.checkSelfPermission(this, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    missingPermissions.add(permission);
                }
            }

            // 2. 결과를 final 변수로 캡처
            final List<String> finalMissingPermissions = missingPermissions;
            final boolean allGranted = finalMissingPermissions.isEmpty();

            // 3. UI 업데이트 및 권한 요청은 메인 스레드로 포스팅
            runOnUiThread(() -> {
                if (allGranted) {
                    permissionDenialCount = 0; // 권한이 이미 허용되었으면 거부 횟수 리셋
                    btPermissionsGranted = true;
                    runPendingPermissionActions();
                } else {
                    // 권한 요청은 메인 스레드에서만 가능
                    logWarn(LogManager.LogCategory.BT, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Permission A.requestRuntimePermissionsOnMainThread ");
                    requestRuntimePermissionsOnMainThread(finalMissingPermissions);
                }
            });
        });
    }

    /**
     * 메인 스레드에서 실행되는 권한 요청 메서드
     */
    private void requestRuntimePermissionsOnMainThread(List<String> missingPermissions) {
        if (permissionRequestInProgress) {
            logInfo(LogManager.LogCategory.BT, "Permission request already in progress; skipping duplicate request");
            return;
        }
        if (missingPermissions.isEmpty()) {
            permissionDenialCount = 0; // 권한이 이미 허용되었으면 거부 횟수 리셋
            btPermissionsGranted = true;
            runPendingPermissionActions();
            return;
        }
        permissionRequestInProgress = true;
        ActivityCompat.requestPermissions(
                this,
                missingPermissions.toArray(new String[0]),
                MULTIPLE_PERMISSIONS
        );
    }

    private void startDeferredInitialization() {
        // DialogManager 초기화
        dialogManager = new DialogManager(this);

        // Initialize service managers
        initializeServiceManagers();

        try {
            usbHandler = new ActivityModelTestProcess.UsbHandler(this);
            // if (Constants.ResultStatus.MODE_TYPE_TEST.equals(mode_type)) {
            //    setupResourceInfoOverlay();
            // }

            setupResourceInfoOverlay();
            setDisplayLightValueChange(0.5f);
            ensureAdaptersInitialized();
            runOnBtWorker(this::initializeAppStateAsync);
            wireDeferredListeners();
            initBluetoothHandler();
            startBluetoothConnectionFlow();
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, "Deferred initialization failed", e);
        }
    }

    private void ensureAdaptersInitialized() {
        if (listItemAdapter == null) {
            listItemAdapter = new ItemAdapterTestItem();
        }
        if (lstTestResult == null) {
            lstTestResult = new ArrayList<>();
        } else {
            lstTestResult.clear();
        }
        if (lstTestTemperature == null) {
            lstTestTemperature = new ArrayList<>();
        } else {
            lstTestTemperature.clear();
        }
    }

    private void wireDeferredListeners() {
        if (btnTestResultClose != null) {
            btnTestResultClose.setOnClickListener(view -> clTestResult.setVisibility(GONE));
        }

        if (btnTestRestart != null) {
            btnTestRestart.setOnClickListener(view -> {
                logWarn(LogManager.LogCategory.BT, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> EEE isControlMode " + isControlMode + " / isControlOn " + isControlOn );
                // 제어 모드라면 재검사 전에 일반 모드로 강제 전환
                if (isControlMode || isControlOn) {
                    forceExitControlModeForRestart();
                }
                finishApplication(getApplicationContext());
            });
            btnTestRestart.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            logWarn(LogManager.LogCategory.BT, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> FFF isControlMode " + isControlMode + " / isControlOn " + isControlOn );
                            resetBluetoothSessionKeepUsb();
                            if (isControlMode || isControlOn) {
                                forceExitControlModeForRestart();
                            }
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            return true;
                        case MotionEvent.ACTION_UP:
                        default:
                            return false;
                    }
                }
            });
        }

        if (btnTestHistoryList != null) {
            btnTestHistoryList.setOnClickListener(view -> {
            });
        }
    }

    private void initBluetoothHandler() {
        btHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case MESSAGE_READ:
                        byte[] payload = Arrays.copyOf((byte[]) msg.obj, msg.arg1);
                        // logWarn(LogManager.LogCategory.BT, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> DDD " + isControlMode);
                        if (clTestResult.getVisibility() == VISIBLE && !isControlMode) {
                            clTestResult.setVisibility(GONE);
                        }

                        // 블루투스 메시지 Handler 수신 로그
                        String handlerMessage = new String(payload, StandardCharsets.UTF_8);
                        // logInfo(LogManager.LogCategory.BT, "▶ [BT-HANDLER] Message received in Handler - Bytes: " + payload.length + ", Message: [" + handlerMessage + "]");

                        runOnBtWorker(() -> processBtMessage(payload));
                        break;
                    case CONNECTING_STATUS:
                        if (msg.arg1 == 1) {
                            logInfo(LogManager.LogCategory.BT, Constants.LogMessages.BLUETOOTH_DEVICE_CONNECTION_SUCCESS);
                            scheduleUiUpdate(() -> {
                                try {
                                    String[] btDeviceNameInfo = btDeviceName.split(Constants.Common.UNDER_SCORE);
                                    tvConnectBtRamp.setBackgroundColor(getBaseContext().getResources().getColor(R.color.blue_01));
                                    // tvConnectBtRamp.setText(...) 유지 필요 시 추가
                                } catch (Exception e) {
                                    logError(LogManager.LogCategory.ER, Constants.ErrorMessages.BLUETOOTH_DEVICE_NAME_PARSING_ERROR, e);
                                }
                            });
                            // 기존 타이머 정리 후 항상 재시작 (제어 모드도 keep-alive 전송 필요)
                            stopBtMessageTimer();
                            startBtMessageTimer();
                        } else {
                            logWarn(LogManager.LogCategory.BT, Constants.LogMessages.BLUETOOTH_DEVICE_CONNECTION_FAILED);
                        }
                        break;
                    default:
                        break;
                }
            }
        };
    }

    private void startBluetoothConnectionFlow() {
        btSearchOnOff = true;
        if (!btSearchOnOff) {
            logInfo(LogManager.LogCategory.BT, Constants.LogMessages.SERVER_NOT_CONNECTED_BLUETOOTH_CONNECTION_ATTEMPT);
            return;
        }
        logInfo(LogManager.LogCategory.BT, Constants.LogMessages.SERVER_CONNECTED_BLUETOOTH_CONNECTION_ATTEMPT);
        new Thread(this::bluetoothSearch).start();
    }

    // ========== PHASE 1: Manager Initialization Methods (NEW) ==========

    /**
     * Initialize all communication managers
     */
    private void initializeManagers() {
        logInfo(LogManager.LogCategory.PS, "Initializing communication managers...");

        try {
            initializeBluetoothManager();
            initializeNetworkManager();
            // UsbConnectionManager will be initialized separately when needed

            logInfo(LogManager.LogCategory.PS, "All managers initialized successfully");
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, "Error initializing managers", e);
        }
    }

    /**
     * Initialize service managers
     */
    private void initializeServiceManagers() {
        try {
            // Initialize TimerManager
            timerManager = new TimerManager();
            logInfo(LogManager.LogCategory.PS, "TimerManager initialized");

            // Initialize PermissionManager
            permissionManager = new PermissionManager(this);
            permissionManager.setPermissionListener(new PermissionManager.PermissionListener() {
                @Override
                public void onAllPermissionsGranted() {
                    btPermissionsGranted = true;
                    permissionDenialCount = 0;
                    runPendingPermissionActions();
                }

                @Override
                public void onPermissionsDenied(String[] deniedPermissions, boolean permanentlyDenied) {
                    btPermissionsGranted = false;
                    permissionDenialCount++;
                    pendingPermissionActions.clear();
                    handlePermissionDenial(deniedPermissions, new int[deniedPermissions.length]);
                }

                @Override
                public void onPermissionRequestNeeded(String[] permissions) {
                    // Permission request will be handled by PermissionManager
                }
            });
            logInfo(LogManager.LogCategory.PS, "PermissionManager initialized");

            // Initialize LoggingService
            loggingService = new LoggingService();
            logInfo(LogManager.LogCategory.PS, "LoggingService initialized");

            // Initialize Processors
            testProcessProcessor = new TestProcessProcessor();
            controlModeProcessor = new ControlModeProcessor();
            messageProcessor = new MessageProcessor();
            dataParser = new DataParser();
            logInfo(LogManager.LogCategory.PS, "All processors initialized");
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, "Error initializing service managers", e);
        }
    }

    /**
     * Initialize Bluetooth Manager
     */
    private void initializeBluetoothManager() {
        bluetoothManager = new BluetoothManager(
                getApplicationContext(),
                new BluetoothManager.BluetoothListener() {
                    @Override
                    public void onMessageReceived(byte[] data) {
                        // Delegate to existing processBtMessage logic
                        processBtMessage(data);
                    }

                    @Override
                    public void onConnectionStateChanged(BluetoothManager.ConnectionState state) {
                        runOnUiThread(() -> {
                            try {
                                switch (state) {
                                    case CONNECTED:
                                        tvConnectBtRamp.setBackgroundColor(getResources().getColor(R.color.blue_01));
                                        tvConnectBtRamp.setText("BT: Connected");
                                        btConnected = true;
                                        break;
                                    case CONNECTING:
                                        tvConnectBtRamp.setBackgroundColor(getResources().getColor(R.color.yellow_01));
                                        tvConnectBtRamp.setText("BT: Connecting...");
                                        btConnected = false;
                                        break;
                                    case RECONNECTING:
                                        tvConnectBtRamp.setBackgroundColor(getResources().getColor(R.color.orange_01));
                                        tvConnectBtRamp.setText("BT: Reconnecting...");
                                        btConnected = false;
                                        break;
                                    case DISCONNECTED:
                                    case FAILED:
                                        tvConnectBtRamp.setBackgroundColor(getResources().getColor(R.color.red_01));
                                        tvConnectBtRamp.setText("BT: Disconnected");
                                        btConnected = false;
                                        break;
                                }
                            } catch (Exception e) {
                                logError(LogManager.LogCategory.ER, "Error updating BT UI", e);
                            }
                        });
                    }

                    @Override
                    public void onError(BluetoothManager.BluetoothError error, String message) {
                        logError(LogManager.LogCategory.ER, "Bluetooth error: " + error + " - " + message, null);
                        runOnUiThread(() -> {
                            try {
                                clAlert.setVisibility(View.VISIBLE);
                                tvAlertMessage.setText("Bluetooth Error: " + message);
                            } catch (Exception e) {
                                logError(LogManager.LogCategory.ER, "Error showing BT error", e);
                            }
                        });
                    }

                    @Override
                    public void onPermissionsRequired(String[] permissions) {
                        ActivityCompat.requestPermissions(
                                ActivityModelTestProcess.this,
                                permissions,
                                PERMISSION_REQUEST_CODE_BT
                        );
                    }
                }
        );

        bluetoothManager.initialize();
        logInfo(LogManager.LogCategory.BT, "BluetoothManager initialized");
    }


    /**
     * Initialize Network Manager
     */
    private void initializeNetworkManager() {
        networkManager = new NetworkManager(
                getApplicationContext(),
                new NetworkManager.NetworkListener() {
                    @Override
                    public void onTestSpecReceived(List<Map<String, String>> specData) {
                        // Store in existing field for compatibility
                        lstData = specData;

                        // Data is now available in lstData for use by the activity
                        logInfo(LogManager.LogCategory.SI, "Test spec data received: " + (specData != null ? specData.size() : 0) + " items");
                    }

                    @Override
                    public void onBarcodeInfoReceived(String serialNo, Map<String, String> productInfo) {
                        runOnUiThread(() -> {
                            try {
                                globalProductSerialNo = serialNo;
                                barcodeReadCheck = true;

                                // Extract product info
                                String modelId = productInfo.get("model_id");
                                String modelName = productInfo.get("model_name");

                                if (modelId != null) globalModelId = modelId;
                                if (modelName != null) globalModelName = modelName;

                                logInfo(LogManager.LogCategory.BI, "Barcode info received: " + serialNo + " -> " + modelName);
                            } catch (Exception e) {
                                logError(LogManager.LogCategory.ER, "Error processing barcode info", e);
                            }
                        });
                    }

                    @Override
                    public void onUploadComplete(boolean success, String message) {
                        logInfo(LogManager.LogCategory.SI, "Upload " + (success ? "succeeded" : "failed") + ": " + message);
                    }

                    @Override
                    public void onVersionInfoReceived(String version) {
                        logInfo(LogManager.LogCategory.SI, "Version: " + version);
                    }

                    @Override
                    public void onError(NetworkManager.NetworkError error, String message) {
                        logError(LogManager.LogCategory.ER, "Network error: " + error + " - " + message, null);
                        // Optionally show error to user
                    }

                    @Override
                    public void onProgress(int progress, String message) {
                        logDebug(LogManager.LogCategory.SI, "Network progress: " + progress + "% - " + message);
                    }
                }
        );

        networkManager.initialize();

        // Set server IPs from existing configuration
        if (serverIp != null && !serverIp.isEmpty()) {
            networkManager.setServerIpAddresses(serverIp, serverResetIp, serverDomain);
        }

        logInfo(LogManager.LogCategory.SI, "NetworkManager initialized");
    }

    /**
     * Start all communication managers
     */
    private void startManagers() {
        logInfo(LogManager.LogCategory.PS, "Starting communication managers...");

        // Start Bluetooth if enabled
        if (bluetoothManager != null && btSearchOnOff) {
            bluetoothManager.startDeviceSearch();
        }

        // USB connection is handled directly via UsbService in ActivityModel_0002
        // (usbSearch(), usbConnection, startUsbPolling(), etc.)

        // Fetch test spec data
        if (networkManager != null && globalModelId != null) {
            networkManager.fetchTestSpec(globalModelId);
        }
    }

    /**
     * Cleanup all communication managers
     * CRITICAL: Must be called to prevent memory leaks!
     */
    private void cleanupManagers() {
        logInfo(LogManager.LogCategory.PS, "Cleaning up communication managers...");

        if (bluetoothManager != null) {
            try {
                bluetoothManager.cleanup();
                bluetoothManager = null;
                logInfo(LogManager.LogCategory.PS, "BluetoothManager cleaned up");
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, "Error cleaning up BluetoothManager", e);
            }
        }

        // USB cleanup
        if (usbConnectionManager != null) {
            try {
                usbConnectionManager.cleanup();
                usbConnectionManager = null;
                logInfo(LogManager.LogCategory.PS, "UsbConnectionManager cleaned up");
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, "Error cleaning up UsbConnectionManager", e);
            }
        }

        if (networkManager != null) {
            try {
                networkManager.cleanup();
                networkManager = null;
                logInfo(LogManager.LogCategory.PS, "NetworkManager cleaned up");
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, "Error cleaning up NetworkManager", e);
            }
        }

        // Service managers cleanup
        if (timerManager != null) {
            try {
                timerManager.stopAllTimers();
                timerManager = null;
                logInfo(LogManager.LogCategory.PS, "TimerManager cleaned up");
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, "Error cleaning up TimerManager", e);
            }
        }

        if (permissionManager != null) {
            permissionManager = null;
        }

        if (loggingService != null) {
            loggingService = null;
        }

        // Processors cleanup
        if (testProcessProcessor != null) {
            testProcessProcessor = null;
        }
        if (controlModeProcessor != null) {
            controlModeProcessor = null;
        }
        if (messageProcessor != null) {
            messageProcessor = null;
        }
        if (dataParser != null) {
            dataParser = null;
        }

        logInfo(LogManager.LogCategory.PS, "Manager cleanup complete");
    }

    // ========== End Manager Initialization Methods ==========

    /**
     * Bluetooth MESSAGE_READ 처리를 백그라운드에서 수행하고,
     * UI 변경은 scheduleUiUpdate()를 통해 전달하는 통합 메서드
     */
    private void processBtMessage(byte[] raw) {

        // if (msg.what == MESSAGE_READ) {
        try {
            if (!btConnected) {
                btConnected = true;
                clearBluetoothReconnect();
            }
            readMessage = new String(raw, StandardCharsets.UTF_8);

            // 블루투스 메시지 처리 시작 로그
            // logInfo(LogManager.LogCategory.BT, "▶ [BT-PROCESS] Processing Bluetooth message - Bytes: " + raw.length + ", Message: [" + readMessage + "], entranceCheck: " + entranceCheck + ", currentTestItem: " + currentTestItem);

            // 제어 모드가 아닌 경우 누적 카운트 리셋
            if (!(isControlMode && isControlOn)) {
                controlSt0101SuccessCount = 0;
                controlModeReady = false;
            }

            // Log.i(TAG, "▶ [BT] entranceCheck >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> " + entranceCheck + " " + currentTestItem + " " + readMessage);

            // 제어 모드에서 검사 실행 중이면 일반 모드와 동일한 검사 로직 수행
            final boolean[] isControlTestRunning = {false};
            final boolean[] originalEntranceCheck = {entranceCheck};
            final int[] originalTestItemIdx = {testItemIdx};
            final int[] originalTestItemCounter = {testItemCounter};
            final String[] originalCurrentTestItem = {currentTestItem};

            synchronized (controlTestTimerLock) {
                isControlTestRunning[0] = (isControlMode && isControlOn && controlTestTimerRunning.get() && controlTestItemIdx >= 0);
            }

            if (isControlTestRunning[0]) {
                // 제어 모드 검사 실행 중: 일반 모드와 동일한 검사 로직 수행
                // entranceCheck를 임시로 true로 설정하여 검사 로직이 실행되도록 함
                originalEntranceCheck[0] = entranceCheck;
                entranceCheck = true;

                // testItemIdx를 제어 모드 검사 항목 인덱스로 설정
                originalTestItemIdx[0] = testItemIdx;
                originalTestItemCounter[0] = testItemCounter;
                originalCurrentTestItem[0] = currentTestItem;

                synchronized (controlTestTimerLock) {
                    testItemIdx = controlTestItemIdx;
                    testItemCounter = controlTestItemCounter;
                    currentTestItem = controlCurrentTestItem;
                }

                // 일반 모드 검사 로직 수행 (아래 코드 계속 실행)
            } else if (isControlMode && isControlOn && waitingForControlResponse.get()) {
                // 제어 모드에서 단순 응답 대기 중이면 응답 처리만 수행
                handleControlModeResponse(readMessage);
                return; // 제어 모드 응답 처리 후 일반 처리 건너뜀
            }

            // ⚠️ 중요: arrTestItems와 listItemAdapter가 초기화되기 전에 블루투스 메시지가 도착할 수 있으므로 null 체크 추가
            if (arrTestItems == null || listItemAdapter == null) {
                logWarn(LogManager.LogCategory.BT, "arrTestItems or listItemAdapter not initialized yet, skipping message processing");
                // 제어 모드 검사 실행 중이었으면 원래 값 복구
                if (isControlTestRunning[0]) {
                    entranceCheck = originalEntranceCheck[0];
                    testItemIdx = originalTestItemIdx[0];
                    testItemCounter = originalTestItemCounter[0];
                    currentTestItem = originalCurrentTestItem[0];
                }
                return;
            }

            if (testItemIdx > arrTestItems.length - 1) {
                testItemIdx = arrTestItems.length - 1;
            }

            // logWarn(LogManager.LogCategory.BT, "readMessage : " + readMessage.length() + ", Message: " + readMessage);

            if (readMessage.contains(ST0101)) {
                // 제어 모드일 때는 제어 검사 대기 상태로만 설정 (일반 검사 시작 로직 실행 안 함)
                if (isControlMode && isControlOn) {
                    // ST0101 응답 3회 수신 시에만 제어 모드 진입
                    if (!controlModeReady) {
                        controlSt0101SuccessCount++;
                        logInfo(LogManager.LogCategory.BT, "Control mode: ST0101 response received (" + controlSt0101SuccessCount + "/" + CONTROL_ST0101_REQUIRED_COUNT + ")");

                        if (controlSt0101SuccessCount >= CONTROL_ST0101_REQUIRED_COUNT) {
                            controlModeReady = true;
                            logInfo(LogManager.LogCategory.BT, "Control mode: ST0101 responses completed. Entering control mode.");
                        } else {
                            logInfo(LogManager.LogCategory.BT, "Control mode: Waiting for more ST0101 responses to enter control mode.");
                            return; // 3회 누적 전에는 제어 모드 진입하지 않음
                        }
                    }

                    logInfo(LogManager.LogCategory.PS, "Control mode: ST0101 received, setting control test ready state");

                    // 헤더에 '제어 준비 완료' 메시지 표시
                    scheduleUiUpdate(() -> {
                        try {
                            if (tvUnitMessage != null) {
                                tvUnitMessage.setText(getStringResource("ui.message.control_ready"));
                                logInfo(LogManager.LogCategory.PS, "Header message set to '제어 준비 완료'");
                            }
                        } catch (Exception e) {
                            logError(LogManager.LogCategory.ER, "Failed to set header message", e);
                        }
                    });

                    // entranceCheck를 true로 설정하여 제어 OFF 후 일반 모드로 복귀할 때 자동 검사가 정상적으로 시작되도록 함
                    entranceCheck = true;

                    // 제어 검사 대기 상태 설정 (일반 검사 시작 로직은 실행하지 않음)
                    // 제어 모드에서는 웹에서 수동으로 검사 항목 실행
                    logInfo(LogManager.LogCategory.PS, "Control test ready state set, waiting for web control commands");
                    return; // 일반 검사 시작 로직 실행 안 함
                }

                // 일반 모드일 때는 기존 로직 실행
                if (!canEnterTest()) {
                    logWarn(LogManager.LogCategory.PS, "Test entry blocked: USB or Bluetooth not ready");
                    scheduleUsbReconnect(true);
                    scheduleBluetoothReconnect(true);
                    scheduleUiUpdate(() -> {
                        clAlert.setVisibility(VISIBLE);
                        tvAlertMessage.setText(getStringResource("ui.message.check_bluetooth_usb"));
                    });
                    return;
                } else {
                    if (Math.abs(disconnectCheckCount) > 0) {
                        receivedMessageCnt = 0;
                        sendingMessageCnt = 0;
                        disconnectCheckCount = 0;
                    }
                }
                testItemIdx = 0;
                // ⚠️ 중요: listItemAdapter null 체크 추가
                if (listItemAdapter != null && listItemAdapter.getCount() > testItemIdx) {
                    ((VoTestItem) listItemAdapter.getItem(testItemIdx)).setTest_item_result(Constants.ResultStatus.OK);
                    ((VoTestItem) listItemAdapter.getItem(testItemIdx)).setTest_result_value(Constants.ResultStatus.COMP_AGING_RESPONSE_01 + Constants.Common.LOGGER_DEVIDER_01 + Constants.ResultStatus.COMP_AGING_RESPONSE_01);
                    ((VoTestItem) listItemAdapter.getItem(testItemIdx)).setTest_finish_yn(Constants.ResultStatus.YES);
                } else {
                    logWarn(LogManager.LogCategory.BT, "listItemAdapter not ready, cannot update test item result");
                }
                if (testProcessId.equals(Constants.Common.EMPTY_STRING)) {
                    testProcessId = getCurrentDatetime(timestampFormat);
                    logInfo(LogManager.LogCategory.PS, Constants.LogMessages.TEST_PROCESS_ID_STARTED + testProcessId);
//                                    new Thread(() -> {
//                                        callBarcodeInfoServer();
//                                    }).start();
                }
                entranceCheck = true;
                // Phase 3: Timer 중복 생성 방지 - synchronized 블록으로 동시성 제어
                // Timer가 이미 실행 중이면 Timer 시작만 건너뛰고, 나머지 로직은 계속 실행
                boolean timerAlreadyRunning = false;
                synchronized (unfinishedRestartTimerLock) {
                    // 이미 실행 중이면 중복 시작 방지 (하지만 검사는 계속 진행)
                    if (unfinishedRestartTimerRunning.get()) {
                        logWarn(LogManager.LogCategory.BT, "Unfinished restart timer already running, skipping start");
                        timerAlreadyRunning = true;
                    } else {
                        // 기존 Timer 인스턴스가 있으면 먼저 정리 (안전성 향상)
                        if (tmrUnfinishedRestart != null) {
                            tmrUnfinishedRestart.cancel();
                            tmrUnfinishedRestart.purge();
                            tmrUnfinishedRestart = null;
                        }
                        if (ttUnfinishedRestart != null) {
                            ttUnfinishedRestart.cancel();
                            ttUnfinishedRestart = null;
                        }

                        try {
                            tmrUnfinishedRestart = new Timer();
                            ttUnfinishedRestart = new TimerTask() {
                                @Override
                                public void run() {
                                    if (restartCntUnfinished == totalTimeCnt + restartCntMargin) {
                                        logWarn(LogManager.LogCategory.BT, String.format(Constants.LogMessages.UNFINISHED_RESTART_APP_TERMINATION, restartCntUnfinished, (totalTimeCnt + restartCntMargin)));
                                        // restartCntUnfinished = 0;
                                        // restartApplication(getApplicationContext());
                                        // finishApplication(getApplicationContext());
                                        resetBluetoothSessionKeepUsb();
                                        // 서버로 결과 전달
                                        // Timer를 cancel할 때 unfinishedRestartTimerRunning도 false로 리셋
                                        synchronized (unfinishedRestartTimerLock) {
                                            if (tmrUnfinishedRestart != null) {
                                                tmrUnfinishedRestart.cancel();
                                                tmrUnfinishedRestart.purge();
                                                tmrUnfinishedRestart = null;
                                            }
                                            unfinishedRestartTimerRunning.set(false);
                                        }
                                    }
                                    restartCntUnfinished++;
                                }
                            };
                            unfinishedRestartTimerRunning.set(true);
                            tmrUnfinishedRestart.schedule(ttUnfinishedRestart, 0, Constants.Timeouts.TIMER_INTERVAL_MS);
                        } catch (Exception e) {
                            unfinishedRestartTimerRunning.set(false);
                            logError(LogManager.LogCategory.ER, "Error starting unfinished restart timer", e);
                        }
                    }
                }
                // callBarcodeInfoServer();
                testItemIdx = 1;

                try {
                    // runOnBtWorker(() -> {
                    logInfo(LogManager.LogCategory.US, Constants.LogMessages.USB_SERVICE_STARTED);
                    setFilters();  // Start listening notifications from UsbService
                    startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
                    // });
                } catch (Exception e) {
                    logError(LogManager.LogCategory.ER, Constants.ErrorMessages.USB_SERVICE_START_ERROR, e);
                }

                // 복잡한 UI 업데이트 블록 통합: scheduleUiUpdate 사용
                scheduleUiUpdate(() -> {
                    logWarn(LogManager.LogCategory.BT, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> AAA " + isControlMode);
                    if (clTestResult.getVisibility() == VISIBLE && !isControlMode) {
                        clTestResult.setVisibility(GONE);
                    }
                    setDisplayLightValueChange(Constants.UI.DISPLAY_BRIGHTNESS);
                });
                // tmrBTMessageSend.cancel();
            } else if (readMessage.contains(Constants.TestItemCodes.RE0101)) {
                stopBtMessageTimer();
            }

            if (entranceCheck) {
                // readMessage 유효성 검사
                if (readMessage == null || readMessage.isEmpty()) {
                    logWarn(LogManager.LogCategory.BT, "readMessage is null or empty, skipping message processing");
                    return;
                }

                // STX 문자 위치 확인
                int stxIndex = readMessage.indexOf(Constants.CharCodes.STX);
                if (stxIndex < 0) {
                    logWarn(LogManager.LogCategory.BT, "STX character not found in readMessage: " + readMessage);
                    return;
                }

                // readMessage 길이 검증 (최소 15자 필요: STX 위치 + 1부터 7자, 그리고 13-15 위치)
                int minRequiredLength = Math.max(stxIndex + 7, 15);
                if (readMessage.length() < minRequiredLength) {
                    logWarn(LogManager.LogCategory.BT, "readMessage too short. Length: " + readMessage.length() + ", Required: " + minRequiredLength + ", Message: " + readMessage);
                    return;
                }

                                /*
                                logWarn(LogManager.LogCategory.BT, "readMessage.substring(stxIndex + 1, stxIndex + 7) : " + readMessage.substring(stxIndex + 1, stxIndex + 7));
                                logWarn(LogManager.LogCategory.BT, "readMessage.substring(stxIndex + 7, 13) : " + readMessage.substring(stxIndex + 7, 13));
                                logWarn(LogManager.LogCategory.BT, "readMessage.substring(13, 15) : " + readMessage.substring(13, 15));
                                logWarn(LogManager.LogCategory.BT, "lstSpecData.get(0) : " + lstSpecData.get(0));
                                */

                try {
                    receiveCommand = "";
                    receiveCommand = readMessage.substring(stxIndex + 1, stxIndex + 7);
                    receiveCommandResponse = readMessage.substring(13, 15);
                    // logInfo(LogManager.LogCategory.PS, ">>>>>> 1.receiveCommand " + receiveCommand);
                } catch (StringIndexOutOfBoundsException e) {
                    logError(LogManager.LogCategory.ER, "StringIndexOutOfBoundsException while parsing readMessage: " + readMessage + ", STX index: " + stxIndex, e);
                    return;
                }

                // Log.i(TAG, "▶▶▶▶▶▶▶▶▶▶ [" + testItemCounter + Constants.Common.LOGGER_DEVIDER_01 + arrTestItems[testItemIdx][2] + "][" + currentTestItem + Constants.Common.LOGGER_DEVIDER_01 + receiveCommand + "]");

                // receiveCommand 유효성 검사
                if (receiveCommand == null || receiveCommand.isEmpty()) {
                    logWarn(LogManager.LogCategory.BT, "receiveCommand is null or empty, skipping database query");
                    return;
                }

                // 데이터베이스 조회 등 무거운 작업을 백그라운드로 이동하여 메인 스레드 블로킹 방지
                if (receiveCommand.equals("CM0102") || receiveCommand.equals("CM0103")) {
                    receiveCommand = Constants.TestItemCodes.CM0101;
                }
                final String finalReceiveCommand = receiveCommand;
                final String finalReceiveCommandResponse = receiveCommandResponse; // 초기값을 final 변수로 캡처
                final String finalReadMessage = readMessage; // readMessage를 final 변수로 캡처
                final String finalCurrentTestItem = currentTestItem;

                // runOnBtWorker(() -> {
                try {
                    List<Map<String, String>> specDataResult = getSpecData(finalReceiveCommand);
                    final List<Map<String, String>> finalSpecData = specDataResult;

                    // 복잡한 UI 업데이트 블록 통합: scheduleUiUpdate 사용
                    scheduleUiUpdate(() -> {
                        try {
                            if (finalSpecData != null && !finalSpecData.isEmpty()) {
                                lstSpecData = finalSpecData;
                            } else {
                                lstSpecData = null;
                            }

                            if (lstSpecData == null || lstSpecData.isEmpty()) {
                                logWarn(LogManager.LogCategory.ER, Constants.LogMessages.LST_SPEC_DATA_NULL_OR_EMPTY + finalReceiveCommand + Constants.LogMessages.DB_AND_MEMORY_CACHE_BOTH_CHECKED);
                                return;
                            }

                            receivedMessageCnt++;
                            String currentProcessName = ((VoTestItem) listItemAdapter.getItem(testItemIdx)).getTest_item_name();
                            applyUiBundle(new UiUpdateBundle.Builder().setCurrentProcessName(currentProcessName).setReceivedMessageCnt(receivedMessageCnt).build());
                        } catch (Exception e) {
                            logError(LogManager.LogCategory.ER, Constants.ErrorMessages.ERROR_IN_INITIAL_UI_UPDATE, e);
                            sendBtMessage(finalCurrentTestItem);
                        }
                    });

                    // ANR 방지: 복잡한 데이터 처리와 계산은 백그라운드 스레드에서 수행
                    // runOnBtWorker(() -> {
                    try {
                        // lstSpecData가 null이면 처리 중단
                        if (lstSpecData == null || lstSpecData.size() == 0) {
                            return;
                        }

                        // 복잡한 조건문 처리 시작
                        // 데이터 처리 결과를 저장할 변수들
                        String calculatedReceiveResponseResult = Constants.Common.EMPTY_STRING;
                        String calculatedResultValue = Constants.Common.EMPTY_STRING;
                        String calculatedDecTemperature = Constants.Common.EMPTY_STRING;
                        int calculatedDecTemperatureValue = 0;
                        // String calculatedTemperatureTmp = Constants.Common.EMPTY_STRING;
                        String calculatedReceiveCommandResponse = Constants.Common.EMPTY_STRING;
                        double calculatedDblValTemp = 0.0;
                        int calculatedWattLower = 0;
                        int calculatedWattUpper = 0;
                        String calculatedReceiveCompAgingResponse = Constants.Common.EMPTY_STRING;
                        String calculatedReceiveResponseResultTxt = Constants.Common.EMPTY_STRING;
                        String calculatedResultInfo = Constants.Common.EMPTY_STRING;
                        boolean shouldUpdateDialog = this.shouldUpdateDialog;
                        int dialogColor = 0;
                        String dialogMessage = "";
                        boolean shouldHideDialog = false;
                        String calculatedTemperatureValueCompDiff = "";

                        // ANR 방지: UI 업데이트를 배치 처리하기 위한 변수들
                        String updateTemperature = null;        // 온도 업데이트 값
                        String updateCompWatt = null;          // 컴프레서 와트 업데이트 값
                        String updateHeaterWatt = null;        // 히터 와트 업데이트 값
                        String updatePumpWatt = null;          // 펌프 와트 업데이트 값
                        String updateLogText = null;           // 로그 텍스트 업데이트 값
                        boolean shouldUpdateListAdapter = false; // 리스트 어댑터 업데이트 필요 여부
                        String updateItemCommand = "";         // 업데이트할 아이템 명령어
                        String updateItemResult = "";           // 업데이트할 아이템 결과
                        String updateItemCheckValue = "";      // 업데이트할 아이템 체크 값
                        String updateItemInfo = "";            // 업데이트할 아이템 정보
                        String updateItemNameSuffix = "";      // 업데이트할 아이템 이름 접미사

                        scheduleUiUpdate(() -> {
                            logWarn(LogManager.LogCategory.BT, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> BBB " + isControlMode);
                            if (!isControlMode) { clTestResult.setVisibility(GONE); }

                            tvPopupProcessResult.setText("");
                            tvTestOkCnt.setText("");
                            tvTestNgCnt.setText("");
                        });

                        if (finalReceiveCommand.contains(Constants.TestItemCodes.SN0101) ||
                                finalReceiveCommand.contains(Constants.TestItemCodes.SN0201) ||
                                finalReceiveCommand.contains(Constants.TestItemCodes.SN0301) ||
                                finalReceiveCommand.contains(Constants.TestItemCodes.TA0101) ||
                                finalReceiveCommand.contains(Constants.TestItemCodes.TA0201)) {
                            // UI 업데이트는 scheduleUiUpdate를 통해 메인 스레드에서 실행
                            shouldUpdateDialog = true;

                            if (finalReceiveCommand.contains(Constants.TestItemCodes.SN0101)) {
                                dialogColor = getBaseContext().getResources().getColor(R.color.pink_01);
                                dialogMessage = lstSpecData.get(0).get(Constants.JsonKeys.CLM_TEST_NAME);
                            } else if (finalReceiveCommand.contains(Constants.TestItemCodes.SN0201)) {
                                dialogColor = getBaseContext().getResources().getColor(R.color.yellow_01);
                                dialogMessage = lstSpecData.get(0).get(Constants.JsonKeys.CLM_TEST_NAME);
                            } else if (finalReceiveCommand.contains(Constants.TestItemCodes.SN0301)) {
                                dialogColor = getBaseContext().getResources().getColor(R.color.green_02);
                                dialogMessage = lstSpecData.get(0).get(Constants.JsonKeys.CLM_TEST_NAME);
                            } else if (finalReceiveCommand.contains(Constants.TestItemCodes.TA0101)) {
                                dialogColor = getBaseContext().getResources().getColor(R.color.blue_01);
                                dialogMessage = lstSpecData.get(0).get(Constants.JsonKeys.CLM_TEST_NAME);
                            } else if (finalReceiveCommand.contains(Constants.TestItemCodes.TA0201)) {
                                dialogColor = getBaseContext().getResources().getColor(R.color.yellow_01);
                                dialogMessage = lstSpecData.get(0).get(Constants.JsonKeys.CLM_TEST_NAME);
                            } else {
                                shouldHideDialog = true;
                            }

                            // UI 업데이트를 메인 스레드로 전달 (setVisibility만)
                            if (!shouldHideDialog) {
                                final int finalDialogColor = dialogColor;
                                final String finalDialogMessage = dialogMessage;
                                scheduleUiUpdate(() -> {
                                    clDialogForPreprocess.setVisibility(View.VISIBLE);
                                });
                            }


                            // UI 업데이트는 나중에 배치 처리 (값만 저장)
                            if (testItemCounter > Constants.TestThresholds.TEST_COUNTER_THRESHOLD_5) {
                                if (finalReceiveCommandResponse.equals(lstSpecData.get(0).get(Constants.JsonKeys.CLM_RESPONSE_VALUE))) {
                                    receiveCommandResponseOK = finalReceiveCommand;
                                    calculatedReceiveResponseResult = Constants.ResultStatus.OK;
                                } else {
                                    calculatedReceiveResponseResult = Constants.ResultStatus.NG;
                                }

                                if (calculatedReceiveResponseResult.equals(Constants.ResultStatus.OK)) {
                                    testItemCounter = Integer.parseInt(arrTestItems[testItemIdx][2]);
                                }
                                calculatedResultValue = finalReceiveCommandResponse;
                            }

                            // logInfo(LogManager.LogCategory.PS, ">>>>> lstSpecData.get(0) " + lstSpecData.get(0));
                            logInfo(LogManager.LogCategory.BT, String.format(Constants.LogMessages.TEST_RESPONSE_SIGNAL_RECEIVED + " [T:%d/%d][C:%d/%s][P:%d/%d][S:%s][G:%d=%d-%d][W:%s][R:%s] %s / %s / %s ",
                                    testTotalCounter, totalTimeCnt, testItemCounter, arrTestItems[testItemIdx][2],
                                    testItemIdx, arrTestItems.length, currentTestItem, disconnectCheckCount,
                                    receivedMessageCnt, sendingMessageCnt, decElectricValue, readMessage,
                                    finalReceiveCommandResponse, lstSpecData.get(0).get(Constants.JsonKeys.CLM_RESPONSE_VALUE), calculatedReceiveResponseResult));
                        } else {
                            // UI 업데이트는 나중에 배치 처리 (값만 저장)
                            shouldHideDialog = true;

                            // 온도 처리 (백그라운드에서 계산)
                            if (finalReceiveCommand.contains(Constants.TestItemCodes.TH0101) || finalReceiveCommand.contains(Constants.TestItemCodes.TH0201)) {
                                try {
                                    calculatedReceiveCommandResponse = finalReadMessage.substring(9, 15);
                                    calculatedDecTemperatureValue = Integer.parseInt(calculatedReceiveCommandResponse, 16);

                                    temperatureData = (finalReceiveCommand.equals(Constants.TestItemCodes.TH0101)) ? coldTemperatureData : hotTemperatureData;

                                    for (int i = 0; i < temperatureData.size(); i++) {
                                        temperature12Bit = (int) Math.round(Double.valueOf(temperatureData.get(i).get(Constants.JsonKeys.CLM_12_BIT)));
                                        if (temperature12Bit > calculatedDecTemperatureValue) {
                                            calculatedDecTemperature = temperatureTmp;
                                            // UI 업데이트는 나중에 배치 처리 (값만 저장)
                                            updateTemperature = calculatedDecTemperature;

                                            mapTestTemperature = new HashMap<>();
                                            mapTestTemperature.put(Constants.JsonKeys.CLM_LINE, Constants.Database.LINE_ID);
                                            mapTestTemperature.put(Constants.JsonKeys.CLM_UNIT_ID, Constants.Database.UNIT_ID);
                                            mapTestTemperature.put(Constants.JsonKeys.CLM_TIMESTAMP, currentTimestamp);
                                            mapTestTemperature.put(Constants.JsonKeys.CLM_PRODUCT_TEMPERATURE, calculatedDecTemperature);
                                            lstTestTemperature.add(mapTestTemperature);
                                            break;
                                        }
                                        calculatedTemperatureTmp = temperatureData.get(i).get(Constants.JsonKeys.CLM_TEMPERATURE);
                                    }

                                    if (finalReceiveCommand.equals(Constants.TestItemCodes.TH0101)) {
                                        decTemperatureHotValue = calculatedTemperatureTmp + Constants.Common.DEGREE_CELSIUS;
                                    } else {
                                        decTemperatureColdValue = calculatedTemperatureTmp + Constants.Common.DEGREE_CELSIUS;
                                    }

                                    calculatedReceiveCommandResponse = calculatedReceiveCommandResponse + Constants.Common.COMMA + calculatedTemperatureTmp;
                                    // logInfo(LogManager.LogCategory.TH, Constants.LogMessages.MEASURED_TEMPERATURE + calculatedDecTemperatureValue + Constants.Common.LOGGER_DEVIDER_01 + calculatedTemperatureTmp);

                                    // logInfo(LogManager.LogCategory.TH, "> lstSpecData.get(0):" + lstSpecData.get(0));
                                    // logInfo(LogManager.LogCategory.TH, "> CLM_VALUE:" + lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE) + " CLM_LOWER_VALUE:" + lstSpecData.get(0).get(Constants.JsonKeys.CLM_LOWER_VALUE) + " CLM_UPPER_VALUE:" + lstSpecData.get(0).get(Constants.JsonKeys.CLM_UPPER_VALUE));

                                    if ((!lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE).equals("") && !lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE).isEmpty()) &&
                                            (!lstSpecData.get(0).get(Constants.JsonKeys.CLM_LOWER_VALUE).equals("") && !lstSpecData.get(0).get(Constants.JsonKeys.CLM_LOWER_VALUE).isEmpty()) &&
                                            (!lstSpecData.get(0).get(Constants.JsonKeys.CLM_UPPER_VALUE).equals("") && !lstSpecData.get(0).get(Constants.JsonKeys.CLM_UPPER_VALUE).isEmpty())) {
                                        // 온도 검사 기준값이 존재할 때만 결과 판정
                                        calculatedDblValTemp = Double.parseDouble(calculatedTemperatureTmp);
                                        int temperatureLower = Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE)) - Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_LOWER_VALUE));
                                        int temperatureUpper = Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE)) + Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_UPPER_VALUE));

                                        logInfo(LogManager.LogCategory.BT, String.format(Constants.LogMessages.TEST_RESPONSE_SIGNAL_RECEIVED + " [T:%d/%d][C:%d/%s][C:%d/%d][S:%s][G:%d=%d-%d][R:%s] %d < %.2f < %d",
                                                testTotalCounter, totalTimeCnt, testItemCounter, arrTestItems[testItemIdx][2],
                                                testItemIdx, arrTestItems.length, currentTestItem, disconnectCheckCount,
                                                receivedMessageCnt, sendingMessageCnt, readMessage,
                                                temperatureLower, calculatedDblValTemp, temperatureUpper));

                                        calculatedReceiveResponseResult = (temperatureLower < calculatedDblValTemp && calculatedDblValTemp < temperatureUpper) ? Constants.ResultStatus.OK : Constants.ResultStatus.NG;
                                        calculatedResultValue = String.valueOf(calculatedDblValTemp);
                                    }
                                } catch (Exception e) {
                                    logError(LogManager.LogCategory.ER, Constants.ErrorMessages.TEMPERATURE_PROCESSING_ERROR, e);
                                }
                            }
                            // 소비전력
                            else {
                                if (isWattTrackingCommand(finalReceiveCommand)) {
                                    recordWattForCommand(finalReceiveCommand, decElectricValue);
                                }
                                int wattLower = 0;
                                int wattUpper = 0;

                                switch (finalReceiveCommand) {
                                    case Constants.TestItemCodes.HT0100:
                                        receiveResponseResult = Constants.ResultStatus.OK;
                                        resultValue = receiveResponseResult;
                                        receiveCompAgingResponse = Constants.ResultStatus.COMP_AGING_RESPONSE_01;
                                        break;
                                    case Constants.TestItemCodes.CM0101:
                                        // receiveCommandResponse = finalReadMessage.substring(9, 11);
                                        // decElectricValue
                                        receiveCompAgingResponse = finalReadMessage.substring(9, 11);

                                        calculatedReceiveCommandResponse = finalReadMessage.substring(9, 15);
                                        calculatedDecTemperatureValue = Integer.parseInt(calculatedReceiveCommandResponse, 16);

                                        temperatureData = hotTemperatureData;

                                        for (int i = 0; i < temperatureData.size(); i++) {
                                            temperature12Bit = (int) Math.round(Double.valueOf(temperatureData.get(i).get(Constants.JsonKeys.CLM_12_BIT)));
                                            if (temperature12Bit > calculatedDecTemperatureValue) {
                                                calculatedDecTemperature = temperatureTmp;
                                                // UI 업데이트는 나중에 배치 처리 (값만 저장)
                                                updateTemperature = calculatedDecTemperature;

                                                mapTestTemperature = new HashMap<>();
                                                mapTestTemperature.put(Constants.JsonKeys.CLM_LINE, Constants.Database.LINE_ID);
                                                mapTestTemperature.put(Constants.JsonKeys.CLM_UNIT_ID, Constants.Database.UNIT_ID);
                                                mapTestTemperature.put(Constants.JsonKeys.CLM_TIMESTAMP, currentTimestamp);
                                                mapTestTemperature.put(Constants.JsonKeys.CLM_PRODUCT_TEMPERATURE, decTemperature);
                                                lstTestTemperature.add(mapTestTemperature);
                                                break;
                                            }
                                            temperatureTmp = temperatureData.get(i).get(Constants.JsonKeys.CLM_TEMPERATURE);
                                        }
                                        calculatedTemperatureTmp = temperatureTmp;
                                        receiveCommandResponse = receiveCommandResponse + Constants.Common.LOGGER_DEVIDER_01 + temperatureTmp;
                                        // logInfo(LogManager.LogCategory.PS, "*********> " + receiveCommandResponse);

                                        if (testItemCounter < Constants.TestThresholds.TEST_COUNTER_THRESHOLD_10 && !compAgingStarted) {
                                            compAgingStarted = true;
                                            decTemperatureValueCompStart = temperatureTmp + Constants.Common.DEGREE_CELSIUS;
                                            calculatedTemperatureValueCompDiff = decTemperatureValueCompStart;
                                        } else {
                                            decTemperatureValueCompEnd = temperatureTmp + Constants.Common.DEGREE_CELSIUS;
                                            calculatedTemperatureValueCompDiff = decTemperatureValueCompStart + Constants.Common.ARROW + decTemperatureValueCompEnd;
                                        }
                                        temperatureValueCompDiff = calculatedTemperatureValueCompDiff;

                                                            /*
                                                            if (testItemCounter >= Constants.TestThresholds.TEST_COUNTER_THRESHOLD_30 && sendResultYn.equals(Constants.ResultStatus.NO)) {
                                                                // 20240522 결과업로드 지연 방지
                                                                // ⚠️ ANR 방지: listItemAdapter 접근은 runOnUiThread에서만!
                                                                // runOnUiThread에서 데이터를 수집한 후 HTTP 통신 스레드로 전달
                                                                runOnUiThread(() -> {
                                                                    try {
                                                                        String checkValue = "";
                                                                        // listItemAdapter 접근은 runOnUiThread에서만 수행
                                                                        for (int i = 0; i < listItemAdapter.getCount(); i++) {
                                                                            VoTestItem voTestItem = ((VoTestItem) listItemAdapter.getItem(i));
                                                                            checkValue += "&clm_" + ((i < Constants.TestThresholds.TEST_COUNTER_THRESHOLD_10) ? Constants.Common.ZERO : Constants.Common.EMPTY_STRING) + (i + 1) + "=" + voTestItem.getTest_item_command() + Constants.Common.COLON + voTestItem.getTest_result_check_value();
                                                                        }
                                                                        checkValue += "&clm_pump_watt=" + decElectricValueForPump;
                                                                        checkValue += "&clm_comp_diff=" + decTemperatureValueCompStart + Constants.Common.COMMA + decTemperatureValueCompEnd;
                                                                        checkValue += "&clm_comp_watt=" + decElectricValueForComp;
                                                                        checkValue += "&clm_heater_watt=" + decElectricValueForHeater;
                                                                        checkValue += "&clm_test=" + Constants.ResultStatus.MODE_TYPE_TEST;

                                                                        // 수집한 데이터를 final 변수로 캡처하여 HTTP 통신 스레드로 전달
                                                                        final String finalCheckValue = checkValue;
                                                                        new Thread(() -> {
                                                                            try {
                                                                                String targetUrl = Constants.URLs.HTTP_PROTOCOL + serverIp + Constants.URLs.ENDPOINT_UPDATE_RESULT_TEST_INFO + Constants.URLs.PARAM_PRODUCT_SERIAL_NO + productSerialNo + Constants.URLs.PARAM_TEST_PROCESS + 3 + Constants.URLs.PARAM_TEST_RESULT + ((testNgCnt > 0) ? Constants.ResultStatus.NG : Constants.ResultStatus.OK) + finalCheckValue;
                                                                                targetUrl = Constants.URLs.HTTP_PROTOCOL + serverIp + Constants.URLs.ENDPOINT_UPDATE_RESULT_TEST_INFO + Constants.Common.QUESTION + Constants.URLs.PARAM_PRODUCT_SERIAL_NO + productSerialNo + Constants.Common.AMPERSAND + Constants.URLs.PARAM_TEST_PROCESS + 3 + Constants.Common.AMPERSAND + Constants.URLs.PARAM_TEST_RESULT + Constants.ResultStatus.OK + finalCheckValue;
                                                                                logInfo(LogManager.LogCategory.BI, Constants.LogMessages.TARGET_URL + targetUrl);

                                                                                // 동시성 문제 해결: connection을 로컬 변수로 변경 (각 스레드가 자신만의 connection 사용)
                                                                                HttpURLConnection connection = null;
                                                                                try {
                                                                                    URL url = new URL(targetUrl);
                                                                                    connection = (HttpURLConnection) url.openConnection();

                                                                                    connection.setReadTimeout(Constants.Timeouts.HTTP_READ_TIMEOUT_MS);
                                                                                    connection.setConnectTimeout(Constants.Timeouts.HTTP_CONNECT_TIMEOUT_MS);
                                                                                    connection.setRequestMethod(Constants.HTTP.METHOD_GET);
                                                                                    connection.setDoInput(true);
                                                                                    connection.setDoOutput(true);

                                                                                    int responseCode = connection.getResponseCode();
                                                                                    if (responseCode == HttpURLConnection.HTTP_OK) {
                                                                                        logInfo(LogManager.LogCategory.BI, Constants.LogMessages.HTTP_OK_SUCCESS);
                                                                                        sendResultYn = Constants.ResultStatus.YES;
                                                                                    } else {
                                                                                        logWarn(LogManager.LogCategory.BI, Constants.LogMessages.HTTP_OK_FAILED);
                                                                                    }
                                                                                } finally {
                                                                                    // 리소스 정리 보장
                                                                                    safeDisconnectConnection(connection);
                                                                                }
                                                                            } catch (Exception e) {
                                                                                logError(LogManager.LogCategory.ER, Constants.ErrorMessages.HTTP_RESULT_UPDATE_ERROR, e);
                                                                                e.printStackTrace();
                                                                            }
                                                                        }).start();
                                                                    } catch (Exception e) {
                                                                        logError(LogManager.LogCategory.ER, Constants.ErrorMessages.ERROR_COLLECTING_LIST_ITEM_ADAPTER_DATA, e);
                                                                    }
                                                                });
                                                            }
                                                            */

                                        if (testItemCounter < Constants.TestThresholds.TEST_COUNTER_THRESHOLD_30) {
                                            if ((!lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE_WATT).equals("") && !lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE_WATT).isEmpty()) &&
                                                    (!lstSpecData.get(0).get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT).equals("") && !lstSpecData.get(0).get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT).isEmpty()) &&
                                                    (!lstSpecData.get(0).get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT).equals("") && !lstSpecData.get(0).get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT).isEmpty())) {
                                                if (receiveCommandResponseOK.equals(finalReceiveCommand)) {
                                                    return;
                                                }
                                                // UI 업데이트는 나중에 배치 처리 (값만 저장)
                                                updateCompWatt = String.valueOf(decElectricValue);
                                                decElectricValueForComp = decElectricValue;
                                                calculatedWattLower = Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE_WATT)) - Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT));
                                                calculatedWattUpper = Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE_WATT)) + Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT));
                                                calculatedReceiveResponseResult = (calculatedWattLower < decElectricValue && decElectricValue < calculatedWattUpper) ? Constants.ResultStatus.OK : Constants.ResultStatus.NG;
                                                if (calculatedReceiveResponseResult.equals(Constants.ResultStatus.OK)) {
                                                    receiveCommandResponseOK = finalReceiveCommand;
                                                }
                                                calculatedResultValue = String.valueOf(decElectricValue);
                                            }
                                        }
//                                                    }

//                                                    if(readMessage.indexOf(("\u0002"))==0) {
//                                                        testItemCounter = Integer.parseInt(arrTestItems[testItemIdx][2]);
//                                                    }

                                        logBtTestResponse(finalReceiveCommand, calculatedReceiveCommandResponse, decElectricValue,
                                                calculatedWattLower, calculatedWattUpper,
                                                finalReadMessage.substring(finalReadMessage.indexOf(Constants.CharCodes.STX) + 1, finalReadMessage.indexOf(Constants.CharCodes.ETX)));
                                        break;
                                    case Constants.TestItemCodes.CM0102:
                                        receiveCompAgingResponse = Constants.ResultStatus.COMP_AGING_RESPONSE_02;
                                        testItemCounter = Integer.parseInt(arrTestItems[testItemIdx][2]);
                                        receiveResponseResult = Constants.ResultStatus.OK;

                                        logBtTestResponse(finalReceiveCommand, calculatedReceiveCommandResponse, decElectricValue,
                                                calculatedWattLower, calculatedWattUpper,
                                                finalReadMessage.substring(finalReadMessage.indexOf(Constants.CharCodes.STX) + 1, finalReadMessage.indexOf(Constants.CharCodes.ETX)));
                                        break;
                                    case Constants.TestItemCodes.HT0101:
                                        // receiveCommandResponse = finalReadMessage.substring(9, 11);
                                        // decElectricValue
                                        calculatedReceiveCommandResponse = finalReadMessage.substring(9, 15);
                                        calculatedDecTemperatureValue = Integer.parseInt(calculatedReceiveCommandResponse, 16);

                                        temperatureData = coldTemperatureData;

                                        for (int i = 0; i < temperatureData.size(); i++) {
                                            temperature12Bit = (int) Math.round(Double.valueOf(temperatureData.get(i).get(Constants.JsonKeys.CLM_12_BIT)));
                                            if (temperature12Bit > calculatedDecTemperatureValue) {
                                                calculatedDecTemperature = temperatureTmp;
                                                // UI 업데이트는 나중에 배치 처리 (값만 저장)
                                                updateTemperature = calculatedDecTemperature;

                                                mapTestTemperature = new HashMap<>();
                                                mapTestTemperature.put(Constants.JsonKeys.CLM_LINE, Constants.Database.LINE_ID);
                                                mapTestTemperature.put(Constants.JsonKeys.CLM_UNIT_ID, Constants.Database.UNIT_ID);
                                                mapTestTemperature.put(Constants.JsonKeys.CLM_TIMESTAMP, currentTimestamp);
                                                mapTestTemperature.put(Constants.JsonKeys.CLM_PRODUCT_TEMPERATURE, decTemperature);
                                                lstTestTemperature.add(mapTestTemperature);
                                                break;
                                            }
                                            temperatureTmp = temperatureData.get(i).get(Constants.JsonKeys.CLM_TEMPERATURE);
                                        }

                                        receiveCommandResponse = receiveCommandResponse + Constants.Common.COMMA + temperatureTmp;

                                        if (testItemCounter < Constants.TestThresholds.TEST_COUNTER_THRESHOLD_5) {
                                            if ((!lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE_WATT).equals("") && !lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE_WATT).isEmpty()) &&
                                                    (!lstSpecData.get(0).get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT).equals("") && !lstSpecData.get(0).get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT).isEmpty()) &&
                                                    (!lstSpecData.get(0).get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT).equals("") && !lstSpecData.get(0).get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT).isEmpty())) {
                                                if (!receiveCommandResponseOK.equals(finalReceiveCommand)) {
                                                    // UI 업데이트는 나중에 배치 처리 (값만 저장)
                                                    updateHeaterWatt = String.valueOf(decElectricValue);
                                                    decElectricValueForHeater = decElectricValue;
                                                }
                                                wattLower = Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE_WATT)) - Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT));
                                                wattUpper = Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE_WATT)) + Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT));
                                                receiveResponseResult = (wattLower < decElectricValue && decElectricValue < wattUpper) ? Constants.ResultStatus.OK : Constants.ResultStatus.NG;
                                                if (receiveResponseResult.equals(Constants.ResultStatus.OK)) {
                                                    receiveCommandResponseOK = finalReceiveCommand;
                                                }
                                                resultValue = String.valueOf(decElectricValue);
                                            }
                                        }
                                        resultInfo = Constants.Common.EMPTY_STRING;
                                        // resultInfo = wattLower + " < " + decElectricValue + " < " + wattUpper;
                                        resultInfo = decElectricValue + Constants.Common.WATT_UNIT;
                                        logBtTestResponse(finalReceiveCommand, receiveCommandResponse, decElectricValue, wattLower, wattUpper, null);
                                        break;
                                    case Constants.TestItemCodes.PM0101:
                                        if (testItemCounter > Constants.TestThresholds.TEST_COUNTER_THRESHOLD_5 && testItemCounter < Integer.parseInt(arrTestItems[testItemIdx][2])) {
                                            // UI 업데이트는 나중에 배치 처리 (값만 저장)
                                            updatePumpWatt = String.valueOf(decElectricValue);
                                            decElectricValueForPump = decElectricValue;
                                            wattLower = Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE_WATT)) - Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT));
                                            wattUpper = Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE_WATT)) + Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT));
                                            receiveResponseResult = (wattLower < decElectricValue && decElectricValue < wattUpper) ? Constants.ResultStatus.OK : Constants.ResultStatus.NG;
                                            receiveResponseResult = (1 < decElectricValue) ? Constants.ResultStatus.OK : Constants.ResultStatus.NG;
                                            resultInfo = decElectricValue + Constants.Common.WATT_UNIT;
                                            if (receiveResponseResult.equals(Constants.ResultStatus.OK)) {
                                                receiveCommandResponseOK = finalReceiveCommand;
                                            }
                                            // logInfo(LogManager.LogCategory.BT, String.format(Constants.LogMessages.PUMP_POWER_CONSUMPTION + ": %d < %.0f < %d", wattLower, decElectricValue, wattUpper));
                                        }
                                        resultInfo = Constants.Common.EMPTY_STRING;
                                        // resultInfo = wattLower + " < " + decElectricValue + " < " + wattUpper;
                                        resultInfo = decElectricValue + Constants.Common.WATT_UNIT;
                                        logBtTestResponse(finalReceiveCommand, receiveCommandResponse, decElectricValue, wattLower, wattUpper, null);
                                        break;
                                    case Constants.TestItemCodes.SV0101:
                                        if (testItemCounter > 4) {
                                            wattLower = Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE_WATT)) - Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT));
                                            wattUpper = Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE_WATT)) + Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT));
                                            logBtTestResponse(finalReceiveCommand, receiveCommandResponse, decElectricValue, wattLower, wattUpper, null);
                                            receiveResponseResult = (wattLower < decElectricValue && decElectricValue < wattUpper) ? Constants.ResultStatus.OK : Constants.ResultStatus.NG;
                                            // if(receiveResponseResult.equals(Constants.ResultStatus.OK)) { receiveCommandResponseOK = finalReceiveCommand; }
                                            resultInfo = Constants.Common.EMPTY_STRING;
                                            // resultInfo = wattLower + " < " + decElectricValue + " < " + wattUpper;
                                            resultInfo = decElectricValue + Constants.Common.WATT_UNIT;
                                        }
                                        break;
                                    case Constants.TestItemCodes.SV0201:
                                        if (testItemCounter > 4) {
                                            wattLower = Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE_WATT)) - Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT));
                                            wattUpper = Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE_WATT)) + Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT));
                                            logBtTestResponse(finalReceiveCommand, receiveCommandResponse, decElectricValue, wattLower, wattUpper, null);
                                            receiveResponseResult = (wattLower < decElectricValue && decElectricValue < wattUpper) ? Constants.ResultStatus.OK : Constants.ResultStatus.NG;
                                            if (receiveResponseResult.equals(Constants.ResultStatus.OK)) {
                                                receiveCommandResponseOK = finalReceiveCommand;
                                            }
                                            resultInfo = Constants.Common.EMPTY_STRING;
                                            // resultInfo = wattLower + " < " + decElectricValue + " < " + wattUpper;
                                            resultInfo = decElectricValue + Constants.Common.WATT_UNIT;
                                        }
                                        break;
                                    case Constants.TestItemCodes.SV0301:
                                        if (testItemCounter > 4) {
                                            wattLower = Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE_WATT)) - Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT));
                                            wattUpper = Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE_WATT)) + Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT));
                                            logBtTestResponse(finalReceiveCommand, receiveCommandResponse, decElectricValue, wattLower, wattUpper, null);
                                            receiveResponseResult = (wattLower < decElectricValue && decElectricValue < wattUpper) ? Constants.ResultStatus.OK : Constants.ResultStatus.NG;
                                            if (receiveResponseResult.equals(Constants.ResultStatus.OK)) {
                                                receiveCommandResponseOK = finalReceiveCommand;
                                            }
                                            resultInfo = Constants.Common.EMPTY_STRING;
                                            // resultInfo = wattLower + " < " + decElectricValue + " < " + wattUpper;
                                            resultInfo = decElectricValue + Constants.Common.WATT_UNIT;
                                        }
                                        break;
                                    case Constants.TestItemCodes.SV0401:
                                        if (testItemCounter > 4) {
                                            wattLower = Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE_WATT)) - Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT));
                                            wattUpper = Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE_WATT)) + Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT));
                                            logBtTestResponse(finalReceiveCommand, receiveCommandResponse, decElectricValue, wattLower, wattUpper, null);
                                            receiveResponseResult = (wattLower < decElectricValue && decElectricValue < wattUpper) ? Constants.ResultStatus.OK : Constants.ResultStatus.NG;
                                            if (receiveResponseResult.equals(Constants.ResultStatus.OK)) {
                                                receiveCommandResponseOK = finalReceiveCommand;
                                            }
                                            resultInfo = Constants.Common.EMPTY_STRING;
                                            // resultInfo = wattLower + " < " + decElectricValue + " < " + wattUpper;
                                            resultInfo = decElectricValue + Constants.Common.WATT_UNIT;
                                        }
                                        break;
                                    case Constants.TestItemCodes.CM0100:
                                        receiveResponseResult = Constants.Common.EMPTY_STRING;
                                        receiveResponseResultTxt = Constants.Common.EMPTY_STRING;
                                        // receiveResponseResult = (receiveCompAgingResponse.equals(Constants.ResultStatus.COMP_AGING_RESPONSE_02))?Constants.ResultStatus.OK:Constants.ResultStatus.NG;
                                        // receiveResponseResultTxt = (receiveCompAgingResponse.equals(Constants.ResultStatus.COMP_AGING_RESPONSE_02))?Constants.ResultStatus.SUCCESS_TEXT:Constants.ResultStatus.FAIL_TEXT;
                                        receiveResponseResult = Constants.ResultStatus.OK;
                                        receiveResponseResultTxt = Constants.Common.SUCCESS;
                                        // receiveResponseResultTxt = Constants.Common.COMP_AC_COOLING_PERFORMANCE + receiveResponseResultTxt;
                                        receiveResponseResultTxt = receiveResponseResultTxt;
                                        break;
                                    default:
                                        if (finalReceiveCommandResponse.equals(lstSpecData.get(0).get(Constants.JsonKeys.CLM_RESPONSE_VALUE))) {
                                            receiveCommandResponseOK = finalReceiveCommand;
                                            receiveResponseResult = Constants.ResultStatus.OK;
                                        } else {
                                            receiveResponseResult = Constants.ResultStatus.NG;
                                        }


                                        // logInfo(LogManager.LogCategory.PS, ">>>>> lstSpecData.get(0) " + lstSpecData.get(0));
                                        logBtTestResponseSimple(finalReceiveCommand, finalReceiveCommandResponse,
                                                lstSpecData.get(0).get(Constants.JsonKeys.CLM_RESPONSE_VALUE) + Constants.Common.LOGGER_DEVIDER_01 + receiveResponseResult);
                                        resultValue = String.valueOf(finalReceiveCommandResponse);

                                        // 파우셋 UV LED
                                        if (finalReceiveCommand.equals(Constants.TestItemCodes.UV0201)) {
                                            if (receiveResponseResult.equals(Constants.ResultStatus.NG)) {
                                                // testItemCounter = Integer.parseInt(arrTestItems[testItemIdx][2]);
                                            }
                                        }
                                        break;
                                }

                                if (finalReceiveCommand.equals(Constants.TestItemCodes.CM0101)
                                        || finalReceiveCommand.equals(Constants.TestItemCodes.TH0101)
                                        || finalReceiveCommand.equals(Constants.TestItemCodes.TH0201)) {
                                } else {
                                    calculatedTemperatureTmp = "0";
                                }
                            }
                        }
                        // logInfo(LogManager.LogCategory.PS, Constants.Common.LOG_MESSGE_SEPARATOR);

                        // 계산된 결과를 멤버 변수에 할당
                        if (!calculatedReceiveResponseResult.isEmpty()) {
                            receiveResponseResult = calculatedReceiveResponseResult;
                        }
                        if (!calculatedResultValue.isEmpty()) {
                            resultValue = calculatedResultValue;
                        }
                        if (!calculatedReceiveCommandResponse.isEmpty()) {
                            receiveCommandResponse = calculatedReceiveCommandResponse;
                        }

                        // 리스트 아이템 업데이트 데이터 수집 (백그라운드에서)
                        // ⚠️ 중요: listItemAdapter 직접 접근 제거, 업데이트할 데이터만 수집
                        updateItemCommand = finalReceiveCommand;
                        updateItemResult = receiveResponseResult;
                        updateItemCheckValue = resultValue;

                        // 아이템별 info 설정
                        if (finalReceiveCommand.contains(Constants.TestItemCodes.CM0101)) {
                            updateItemInfo = temperatureValueCompDiff;
                        } else if (finalReceiveCommand.contains(Constants.TestItemCodes.TH0101)) {
                            updateItemInfo = decTemperatureHotValue;
                        } else if (finalReceiveCommand.contains(Constants.TestItemCodes.TH0201)) {
                            updateItemInfo = decTemperatureColdValue;
                        } else if (finalReceiveCommand.contains(Constants.TestItemCodes.SV0101) || finalReceiveCommand.contains(Constants.TestItemCodes.SV0201) ||
                                finalReceiveCommand.contains(Constants.TestItemCodes.SV0301) || finalReceiveCommand.contains(Constants.TestItemCodes.SV0401)) {
                            updateItemInfo = String.valueOf(decElectricValue);
                        }

                        if (Constants.TestItemCodes.CM0100.equals(finalReceiveCommand)) {
                            updateItemNameSuffix = receiveResponseResultTxt;
                        }

                        shouldUpdateListAdapter = true;

                        // logInfo(LogManager.LogCategory.BT, String.format("검사 응답 신호 수신: %s / %s / %s / %.0f W",
                        //         finalReadMessage.substring(finalReadMessage.indexOf((Constants.CharCodes.STX)) + 1, finalReadMessage.indexOf((Constants.CharCodes.ETX))),
                        //         finalReceiveCommand, receiveResponseResult, decElectricValue));

                        // 로그 텍스트 업데이트 (mode_type이 "T"인 경우)
                        if (mode_type.equals("T")) {
                            String compAgingCondition = "";
                            if (finalReceiveCommand.contains(Constants.TestItemCodes.CM01)) {
                                compAgingCondition = decElectricValue + "W / " + receiveCompAgingResponse;
                            } else {
                                compAgingCondition = decElectricValue + Constants.Common.WATT_UNIT;
                            }
                            log_text_param += "[" + getCurrentDatetime(datetimeFormat) + Constants.Common.LOGGER_DEVIDER_01 + String.format(Constants.InitialValues.NUMBER_FORMMATING_03D, testItemCounter) + Constants.Common.LOGGER_DEVIDER_01 + String.format(Constants.InitialValues.NUMBER_FORMMATING_03D, Integer.parseInt(arrTestItems[testItemIdx][2])) + "] " + (finalReadMessage.substring(finalReadMessage.indexOf(("\u0002")) + 1, finalReadMessage.indexOf(("\u0003")))) + Constants.Common.LOGGER_DEVIDER_01 + finalReceiveCommand + Constants.Common.LOGGER_DEVIDER_01 + receiveResponseResult + "(" + receiveCommandResponse + ") / " + compAgingCondition;
                            log_text = "▶ [" + getCurrentDatetime(datetimeFormat) + Constants.Common.LOGGER_DEVIDER_01 + String.format(Constants.InitialValues.NUMBER_FORMMATING_03D, testItemCounter) + Constants.Common.LOGGER_DEVIDER_01 + String.format(Constants.InitialValues.NUMBER_FORMMATING_03D, Integer.parseInt(arrTestItems[testItemIdx][2])) + "] " + (finalReadMessage.substring(finalReadMessage.indexOf(("\u0002")) + 1, finalReadMessage.indexOf(("\u0003")))) + Constants.Common.LOGGER_DEVIDER_01 + finalReceiveCommand + Constants.Common.LOGGER_DEVIDER_01 + receiveResponseResult + "(" + receiveCommandResponse + ") / " + decElectricValue + "W\n" + log_text;
                            updateLogText = log_text;  // UI 업데이트 값 저장

                            // HTTP 통신은 별도 스레드로 유지
                            runOnBtWorker(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        StringBuilder decElectricValueList = new StringBuilder();
                                        if (finalReceiveCommand.contains(Constants.TestItemCodes.RE0101)) {
                                            // ⚠️ 주의: listItemAdapter 접근은 runOnUiThread에서만!
                                            // 여기서는 이미 수집된 데이터 사용 (lstWatt는 멤버 변수)
                                            for (int i = 0; i < lstWatt.size(); i++) {
                                                decElectricValueList.append(lstWatt.get(i)).append(",");
                                            }
                                            if (decElectricValueList.length() > 0) {
                                                decElectricValueList = new StringBuilder(decElectricValueList.substring(0, decElectricValueList.length() - 1));
                                            }
                                            decElectricValueList.insert(0, "&clm_watt_log=");
                                        }

                                        testTaskThread = new ActivityModelTestProcess.RequestTestTaskThreadAsync(ActivityModelTestProcess.this);
                                        urlTestTaskStr = "http://" + serverIp + "/OVIO/TestTaskInfoUpdate.jsp" + "?clm_test_task_log=" + URLEncoder.encode(log_text_param) + "&clm_test_unit_seq=" + unit_no + "&clm_unit_ip=" + ipAddress + "&clm_product_serial_no=" + productSerialNo + "&clm_test_process_id=" + testProcessId + "&clm_model_id=" + globalModelId + decElectricValueList;

                                        testProcessId = (finalReceiveCommand.contains(Constants.TestItemCodes.RE0101)) ? "" : testProcessId;
                                        testTaskThread.execute();
                                        log_text_param = "";
                                    } catch (Exception e) {
                                        logError(LogManager.LogCategory.ER, Constants.ErrorMessages.THREAD_EXECUTION_ERROR, e);
                                    }
                                }
                            });
                        }

                        if (testItemCounter == Integer.parseInt(arrTestItems[testItemIdx][2])) {
                            // sendBtMessage("ST0201");
                        }

                        // 제어 모드 검사 실행 중이었으면 원래 값 복구 및 카운터 업데이트
                        if (isControlTestRunning[0]) {
                            synchronized (controlTestTimerLock) {
                                // 제어 모드 검사 타이머의 카운터 업데이트
                                controlTestItemCounter = testItemCounter;

                                // 제어 모드 검사 수신 정보 저장 (나중에 applyUiUpdateBundle에서 업데이트됨)
                                // 여기서는 기본 정보만 저장
                                controlTestReceiveCommand = finalReceiveCommand;
                                controlTestReceiveResponse = finalReceiveCommandResponse;

                                // 검사 결과 값 저장 (소비전력 또는 온도) - resultValue와 resultInfo 사용
                                // resultValue와 resultInfo는 processBtMessage에서 계산된 값
                                // applyUiUpdateBundle에서 최종적으로 업데이트됨

                                // 센서 검사의 경우 조건 만족 시 검사 종료
                                if (controlTestItemCounter > Constants.TestThresholds.TEST_COUNTER_THRESHOLD_5) {
                                    boolean shouldStopTest = false;

                                    // 센서 검사: 응답 값이 일치하면 종료
                                    if (finalReceiveCommand.contains(Constants.TestItemCodes.SN0101) ||
                                            finalReceiveCommand.contains(Constants.TestItemCodes.SN0201) ||
                                            finalReceiveCommand.contains(Constants.TestItemCodes.SN0301) ||
                                            finalReceiveCommand.contains(Constants.TestItemCodes.FM0101) ||
                                            finalReceiveCommand.contains(Constants.TestItemCodes.TA0101) ||
                                            finalReceiveCommand.contains(Constants.TestItemCodes.TA0201) ||
                                            finalReceiveCommand.contains(Constants.TestItemCodes.TA0301)) {

                                        if (lstSpecData != null && !lstSpecData.isEmpty()) {
                                            String expectedResponse = lstSpecData.get(0).get(Constants.JsonKeys.CLM_RESPONSE_VALUE);
                                            if (finalReceiveCommandResponse.equals(expectedResponse)) {
                                                shouldStopTest = true;
                                                logInfo(LogManager.LogCategory.BT, "Control test condition met: " + controlCurrentTestItem + " (response: " + finalReceiveCommandResponse + ")");
                                            }
                                        }
                                    }
                                    // 소비전력 검사: 조건 만족 시 종료 (일반 모드와 동일하게 시간 도달 시 종료)
                                    // 온도 검사: 조건 만족 시 종료 (일반 모드와 동일하게 시간 도달 시 종료)

                                    if (shouldStopTest) {
                                        logInfo(LogManager.LogCategory.BT, "Control test item completed (condition met): " + controlCurrentTestItem);
                                        stopControlTestTimer();

                                        // 제어 모드 검사 결과 다이얼로그 표시
                                        showControlModeTestResultDialog();
                                    }
                                }

                                // 원래 값 복구
                                entranceCheck = originalEntranceCheck[0];
                                testItemIdx = originalTestItemIdx[0];
                                testItemCounter = originalTestItemCounter[0];
                                currentTestItem = originalCurrentTestItem[0];
                            }
                        }

                        // 카운트 계산은 runOnUiThread에서 수행 (listItemAdapter 접근 필요)

                        if (finalReceiveCommand.equals(Constants.TestItemCodes.RE0101)) {
                            // runOnBtWorker(new Runnable() {
                            // @Override
                            // public void run() {
                            try {
                                // 1) UI 스레드에서 listItemAdapter 접근 + 문자열 조립
                                scheduleUiUpdate(() -> {
                                    try {
                                        StringBuilder builder = new StringBuilder();

                                        for (int i = 0; i < listItemAdapter.getCount(); i++) {
                                            VoTestItem item = (VoTestItem) listItemAdapter.getItem(i);
                                            builder.append("&clm_")
                                                    .append((i < Constants.TestThresholds.TEST_COUNTER_THRESHOLD_10) ? Constants.Common.ZERO : Constants.Common.EMPTY_STRING)
                                                    .append(i + 1)
                                                    .append("=")
                                                    .append(item.getTest_item_command())
                                                    .append(Constants.Common.COLON)
                                                    .append(item.getTest_result_check_value());
                                        }

                                        builder.append("&clm_pump_watt=").append(decElectricValueForPump);
                                        builder.append("&clm_comp_diff=").append(decTemperatureValueCompStart).append(Constants.Common.COMMA).append(decTemperatureValueCompEnd);
                                        builder.append("&clm_comp_watt=").append(decElectricValueForComp);
                                        builder.append("&clm_heater_watt=").append(decElectricValueForHeater);
                                        builder.append("&clm_test=").append(Constants.ResultStatus.MODE_TYPE_TEST);

                                        final String finalCheckValue = builder.toString();
                                        final String finalLmsTestSeq = lmsTestSeq;

                                        // 2) 워커 스레드에서 네트워크 + DB 업데이트
                                        runOnBtWorker(() -> executeResultUpload(finalCheckValue, finalLmsTestSeq));
                                    } catch (Exception e) {
                                        logError(LogManager.LogCategory.ER, Constants.ErrorMessages.ERROR_COLLECTING_LIST_ITEM_ADAPTER_DATA, e);
                                    }
                                });
                                                /*
                                                                // 20240522 결과업로드 지연 방지
                                                                // Phase 6: listItemAdapter 접근 최적화 - scheduleUiUpdate 사용
                                                                // scheduleUiUpdate에서 데이터를 수집한 후 HTTP 통신 스레드로 전달
                                                                scheduleUiUpdate(() -> {
                                                                    try {
                                                                        String checkValue = "";
                                                                        // listItemAdapter 접근은 메인 스레드에서만 수행
                                                                        for (int i = 0; i < listItemAdapter.getCount(); i++) {
                                                                            VoTestItem voTestItem = ((VoTestItem) listItemAdapter.getItem(i));
                                                                            checkValue += "&clm_" + ((i < Constants.TestThresholds.TEST_COUNTER_THRESHOLD_10) ? Constants.Common.ZERO : Constants.Common.EMPTY_STRING) + (i + 1) + "=" + voTestItem.getTest_item_command() + Constants.Common.COLON + voTestItem.getTest_result_check_value();
                                                                        }
                                                                        checkValue += "&clm_pump_watt=" + decElectricValueForPump;
                                                                        checkValue += "&clm_comp_diff=" + decTemperatureValueCompStart + Constants.Common.COMMA + decTemperatureValueCompEnd;
                                                                        checkValue += "&clm_comp_watt=" + decElectricValueForComp;
                                                                        checkValue += "&clm_heater_watt=" + decElectricValueForHeater;
                                                                        checkValue += "&clm_test=" + Constants.ResultStatus.MODE_TYPE_TEST;

                                                                        // 수집한 데이터를 final 변수로 캡처하여 HTTP 통신 스레드로 전달
                                                                        final String finalCheckValue = checkValue;
                                                                        final String finalLmsTestSeq = lmsTestSeq;
                                                        // final String finalCurrentTestItem = (currentTestItem != null) ? currentTestItem : "";
                                                        // runOnBtWorker(() -> {
                                                                            String httpSuccessYn = Constants.ResultStatus.NO; // 기본값은 실패
                                                                            try {
                                                                                String targetUrl = Constants.URLs.HTTP_PROTOCOL + serverIp + Constants.URLs.ENDPOINT_UPDATE_RESULT_TEST_INFO + Constants.URLs.PARAM_PRODUCT_SERIAL_NO + productSerialNo + Constants.URLs.PARAM_TEST_PROCESS + 3 + Constants.URLs.PARAM_TEST_RESULT + ((testNgCnt > 0) ? Constants.ResultStatus.NG : Constants.ResultStatus.OK) + finalCheckValue;
                                                                                targetUrl = Constants.URLs.HTTP_PROTOCOL + serverIp + Constants.URLs.ENDPOINT_UPDATE_RESULT_TEST_INFO + Constants.Common.QUESTION + Constants.URLs.PARAM_PRODUCT_SERIAL_NO + productSerialNo + Constants.Common.AMPERSAND + Constants.URLs.PARAM_TEST_PROCESS + 3 + Constants.Common.AMPERSAND + Constants.URLs.PARAM_TEST_RESULT + Constants.ResultStatus.OK + finalCheckValue;
                                                                                logInfo(LogManager.LogCategory.BI, Constants.LogMessages.TARGET_URL + targetUrl);

                                                                                // HTTP 요청 재시도 로직
                                                                                int retryCount = Constants.Network.HTTP_RETRY_COUNT;
                                                                                boolean requestSuccess = false;

                                                                                for (int attempt = 1; attempt <= retryCount; attempt++) {
                                                                                    HttpURLConnection connection = null;
                                                                                    try {
                                                                                        logInfo(LogManager.LogCategory.BI, "HTTP 요청 시도 [" + attempt + "/" + retryCount + "]");

                                                                                        URL url = new URL(targetUrl);
                                                                                        connection = (HttpURLConnection) url.openConnection();

                                                                                        connection.setReadTimeout(Constants.Timeouts.HTTP_READ_TIMEOUT_MS);
                                                                                        connection.setConnectTimeout(Constants.Timeouts.HTTP_CONNECT_TIMEOUT_MS);
                                                                                        connection.setRequestMethod(Constants.HTTP.METHOD_GET);
                                                                                        connection.setDoInput(true);
                                                                                        connection.setDoOutput(true);

                                                                                        int responseCode = connection.getResponseCode();
                                                                                        if (responseCode == HttpURLConnection.HTTP_OK) {
                                                                                            logInfo(LogManager.LogCategory.BI, Constants.LogMessages.HTTP_OK_SUCCESS + " (시도 " + attempt + "/" + retryCount + ")");
                                                                                            sendResultYn = Constants.ResultStatus.YES;
                                                                                            httpSuccessYn = Constants.ResultStatus.YES;
                                                                                            requestSuccess = true;
                                                                                            break; // 성공 시 루프 종료
                                                                                        } else {
                                                                                            logWarn(LogManager.LogCategory.BI, Constants.LogMessages.HTTP_OK_FAILED + " (시도 " + attempt + "/" + retryCount + ", 응답 코드: " + responseCode + ")");
                                                                                            if (attempt < retryCount) {
                                                                                                // 마지막 시도가 아니면 재시도 전 대기
                                                                                                Thread.sleep(Constants.Network.HTTP_RETRY_DELAY_MS);
                                                                                            }
                                                                                        }
                                                                                    } catch (
                                                                                            Exception e) {
                                                                                        logError(LogManager.LogCategory.ER, Constants.ErrorMessages.HTTP_RESULT_UPDATE_ERROR + " (시도 " + attempt + "/" + retryCount + "): " + e.getMessage(), e);
                                                                                        if (attempt < retryCount) {
                                                                                            // 마지막 시도가 아니면 재시도 전 대기
                                                                                            try {
                                                                                                Thread.sleep(Constants.Network.HTTP_RETRY_DELAY_MS);
                                                                                            } catch (
                                                                                                    InterruptedException ie) {
                                                                                                Thread.currentThread().interrupt();
                                                                                                logError(LogManager.LogCategory.ER, "HTTP 재시도 대기 중 인터럽트 발생", ie);
                                                                                            }
                                                                                        }
                                                                                    } finally {
                                                                                        // 리소스 정리 보장
                                                                                        safeDisconnectConnection(connection);
                                                                                    }
                                                                                }

                                                                                // 모든 재시도 실패 시 최종 실패 처리
                                                                                if (!requestSuccess) {
                                                                                    logInfo(LogManager.LogCategory.PS, "HTTP 요청 실패: " + retryCount + "번 시도 모두 실패");
                                                                                    httpSuccessYn = Constants.ResultStatus.NO;
                                                                                }

                                                                                // HTTP 전송 성공 여부를 tbl_test_history에 업데이트
                                                                                try {
                                                                                    TestData.updateProductTestHistoryHttpSuccess(getBaseContext(), finalLmsTestSeq, httpSuccessYn);
                                                                                } catch (
                                                                                        Exception e) {
                                                                                    logError(LogManager.LogCategory.ER, "Error updating HTTP success status in test history", e);
                                                                                }
                                                            } catch (
                                                                    Exception e) {
                                                                                logError(LogManager.LogCategory.ER, Constants.ErrorMessages.HTTP_RESULT_UPDATE_ERROR, e);
                                                                                e.printStackTrace();
                                                                                // 예외 발생 시에도 실패로 기록
                                                                                try {
                                                                                    TestData.updateProductTestHistoryHttpSuccess(getBaseContext(), finalLmsTestSeq, Constants.ResultStatus.NO);
                                                                                } catch (
                                                                                        Exception ex) {
                                                                                    logError(LogManager.LogCategory.ER, "Error updating HTTP success status in test history", ex);
                                                                                }
                                                                            } finally {
                                                                                // lstMapWattTemp에 저장된 정보들을 tbl_test_history_linear_data 테이블에 저장
                                                                                try {
                                                                                    for (int i = 0; i < lstMapWattTemp.size(); i++) {
                                                                                        String[] wattTempData = lstMapWattTemp.get(i);
                                                                                        if (wattTempData != null && wattTempData.length >= 3) {
                                                                                            String timestamp = wattTempData[0];
                                                                                            String temperature = wattTempData[1];
                                                                                            String watt = wattTempData[2];
                                                                                            String testItem = wattTempData[3];
                                                                                            // testItemId는 현재 테스트 항목 또는 빈 문자열 사용
                                                                                            TestData.insertTestHistoryLinearData(getBaseContext(), finalLmsTestSeq, testItem, timestamp, temperature, watt);
                                                                                        }
                                                                                    }
                                                                                } catch (
                                                                                        Exception e) {
                                                                                    logError(LogManager.LogCategory.ER, "Error saving linear data to test history", e);
                                                                                }
                                                                            }
                                                        // });
                                                                    } catch (Exception e) {
                                                                        logError(LogManager.LogCategory.ER, Constants.ErrorMessages.ERROR_COLLECTING_LIST_ITEM_ADAPTER_DATA, e);
                                                                    }
                                                                });
                                                */

                                stopBtMessageTimer();

                                btSearchOnOff = false;
                                clearBluetoothReconnect();
                                // btSocket.close();
                                btSocket = null;
                                deviceSelected = null;

                                // ============================================================
                                // ⚠️ 개선: SharedPreferences 작업을 백그라운드 스레드로 이동
                                // ============================================================
                                // StrictMode 경고 방지: 디스크 I/O를 메인 스레드에서 제거
                                // 방법 1: SharedPreferences 작업을 백그라운드 스레드에서 처리
                                // 방법 3: commit() 대신 apply() 사용 (비동기 처리)
                                // ============================================================
                                final String finalLmsTestSeq = lmsTestSeq;
                                final String finalProductSerialNo = productSerialNo;
                                runOnBtWorker(() -> {
                                    try {
                                        // 백그라운드 스레드에서 SharedPreferences 작업 수행
                                        SharedPreferences prefs = getApplicationContext().getSharedPreferences(
                                                Constants.SharedPrefKeys.TEST_COOKIE_INFO, MODE_PRIVATE);
                                        SharedPreferences.Editor editor = prefs.edit();
                                        editor.putString(Constants.IntentExtras.TEST_START_DATETIME, finalLmsTestSeq);
                                        editor.putString(Constants.SharedPrefKeys.TEST_PRODUCT_SERIAL_NO, finalProductSerialNo);
                                        // apply() 사용: 비동기 처리로 메인 스레드 블로킹 방지
                                        editor.apply();

                                        // static 변수 업데이트는 메인 스레드에서 수행 (ActivityModelList에서 사용)
                                        runOnUiThread(() -> {
                                            cookie_preferences = prefs;
                                            cookie_info = editor;
                                        });

                                        // logInfo(LogManager.LogCategory.PS, Constants.LogMessages.TEST_START_DATETIME_LMS_TEST_SEQ + finalLmsTestSeq);
                                        // logInfo(LogManager.LogCategory.PS, Constants.LogMessages.TEST_PROCESS_ID_FINISHED + testProcessId);
                                    } catch (Exception e) {
                                        logError(LogManager.LogCategory.ER, "Error saving SharedPreferences in background thread", e);
                                    }
                                });

                                // ============================================================
                                // UI 업데이트는 메인 스레드에서만 처리
                                // ============================================================
                                scheduleUiUpdate(() -> {
                                    setDisplayLightValueChange(Constants.UI.DISPLAY_BRIGHTNESS);
                                    // testProcessId = "";

                                    try {
                                        if (btSocket != null && btSocket.isConnected()) {
                                            try {
                                                btSocket.close();
                                            } catch (
                                                    IOException e) {
                                                logError(LogManager.LogCategory.ER, Constants.ErrorMessages.ERROR_CLOSING_SOCKET, e);
                                            }
                                        }
                                        // btnTestRestart.setOnTouchListener(null);
                                        // tvConnectBtRamp.setText("");
                                        tvConnectBtRamp.setBackgroundColor(getBaseContext().getResources().getColor(R.color.red_01));

                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }

                                    btnTestRestart.setBackgroundColor(getColor(R.color.green_02));
                                    btnTestRestart.setTextColor(getColor(R.color.white));
                                    // getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

                                                                        /*
                                                                        ItemAdapterTestItem listItemResultAdapter = new ItemAdapterTestItem();

                                                                        for (int i = 0; i < listItemAdapter.getCount(); i++) {
                                                                            logDebug(LogManager.LogCategory.RS, String.format(Constants.LogMessages.TEST_RESULT_FORMAT, i, ((VoTestItem) listItemAdapter.getItem(i)).VoMapInfo()));
                                                                            if (((VoTestItem) listItemAdapter.getItem(i)).getTest_item_result().equals(Constants.ResultStatus.NG)) {
                                                                                VoTestItem voTestItem = (VoTestItem) listItemAdapter.getItem(i);
                                                                                voTestItem.setTest_finish_yn(Constants.ResultStatus.YES);
                                                                                voTestItem.setTest_item_result(Constants.ResultStatus.NG);
                                                                                voTestItem.setTest_item_info(voTestItem.getTest_result_value());
                                                                                listItemResultAdapter.addItem(voTestItem);
                                                                            }
                                                                        }

                                                                        listItemResultAdapter.updateListAdapter();
                                                                        */

                                    if (testNgCnt > 0) {
                                        tvPopupProcessResult.setText(Constants.ResultStatus.NG);
                                        tvPopupProcessResult.setTextSize(Dimension.SP, Constants.UI.TEXT_SIZE_LARGE_SP);
                                        tvPopupProcessResultCompValue.setTextSize(Dimension.SP, Constants.UI.TEXT_SIZE_MEDIUM_SP);
                                        tvPopupProcessResultHeaterValue.setTextSize(Dimension.SP, Constants.UI.TEXT_SIZE_MEDIUM_SP);
                                    } else {
                                        tvPopupProcessResult.setText(Constants.ResultStatus.OK);
                                    }
                                    // }

                                    Map<String, String> mapTestHistory = new HashMap<>();
                                    mapTestHistory.put(Constants.JsonKeys.CLM_TEST_RESULT, ((testNgCnt > 0) ? Constants.ResultStatus.NG : Constants.ResultStatus.OK));
                                    mapTestHistory.put(Constants.JsonKeys.CLM_TEST_NG_COUNT, String.valueOf(testNgCnt));
                                    mapTestHistory.put(Constants.JsonKeys.CLM_TEST_OK_COUNT, String.valueOf(testOkCnt));
                                    mapTestHistory.put(Constants.JsonKeys.CLM_TEST_HISTORY_SEQ, lmsTestSeq);
                                    mapTestHistory.put(Constants.JsonKeys.CLM_TEST_MODEL_ID, arrTestModels[0][0]);
                                    mapTestHistory.put(Constants.JsonKeys.CLM_TEST_MODEL_NAME, arrTestModels[0][1]);
                                    mapTestHistory.put(Constants.JsonKeys.CLM_TEST_MODEL_NATIONALITY, arrTestModels[0][2]);
                                    mapTestHistory.put(Constants.JsonKeys.CLM_TEST_TIMESTAMP, lmsTestTimestamp);
                                    mapTestHistory.put(Constants.JsonKeys.CLM_TEST_COMP_VALUE, String.valueOf(currentCompWattValue));
                                    mapTestHistory.put(Constants.JsonKeys.CLM_TEST_HEATER_VALUE, String.valueOf(currentHeaterWattValue));
                                    mapTestHistory.put(Constants.JsonKeys.CLM_COMMENT, "");

                                    // ============================================================
                                    // ⚠️ 개선: SharedPreferences 작업을 백그라운드 스레드로 이동
                                    // ============================================================
                                    // StrictMode 경고 방지: 디스크 I/O를 메인 스레드에서 제거
                                    // 방법 1: SharedPreferences 작업을 백그라운드 스레드에서 처리
                                    // 방법 3: commit() 대신 apply() 사용 (비동기 처리)
                                    // ============================================================
                                    final String testResult = ((testNgCnt > 0) ? Constants.ResultStatus.NG : Constants.ResultStatus.OK);
                                    final ActivityModelTestProcess activity = getMainActivity();
                                    final String heaterValue = (activity != null) ? String.valueOf(activity.currentHeaterWattValueProp) : Constants.Common.ZERO;
                                    final String compValue = (activity != null) ? String.valueOf(activity.currentCompWattValueProp) : Constants.Common.ZERO;

                                    // 메인 스레드에서 static 변수 업데이트 (다른 곳에서 사용될 수 있음)
                                    test_info.putString(Constants.SharedPrefKeys.TEST_RESULT, testResult);
                                    test_info.putString(Constants.SharedPrefKeys.HEATER_VALUE, heaterValue);
                                    test_info.putString(Constants.SharedPrefKeys.COMP_VALUE, compValue);

                                    // 백그라운드 스레드에서 apply() 호출 (비동기 처리)
                                    runOnBtWorker(() -> {
                                        try {
                                            // 백그라운드 스레드에서 새로운 Editor 인스턴스를 얻어서 apply() 호출
                                            SharedPreferences prefs = getApplicationContext().getSharedPreferences(
                                                    Constants.SharedPrefKeys.TEST_COOKIE_INFO, MODE_PRIVATE);
                                            SharedPreferences.Editor editor = prefs.edit();
                                            editor.putString(Constants.SharedPrefKeys.TEST_RESULT, testResult);
                                            editor.putString(Constants.SharedPrefKeys.HEATER_VALUE, heaterValue);
                                            editor.putString(Constants.SharedPrefKeys.COMP_VALUE, compValue);
                                            // apply() 사용: 비동기 처리로 메인 스레드 블로킹 방지
                                            editor.apply();
                                        } catch (Exception e) {
                                            logError(LogManager.LogCategory.ER, "Error saving test_info SharedPreferences in background thread", e);
                                        }
                                    });

                                    // ============================================================
                                    // ⚠️ 개선: SQLite 데이터베이스 작업을 백그라운드 스레드로 이동
                                    // ============================================================
                                    // StrictMode 경고 방지: 디스크 I/O를 메인 스레드에서 제거
                                    // listItemAdapter 데이터를 먼저 수집 (메인 스레드에서)
                                    // ============================================================
                                    final List<Map<String, String>> testHistoryDetailList = new ArrayList<>();
                                    final int adapterCount = listItemAdapter.getCount();

                                    // 메인 스레드에서 listItemAdapter 데이터 수집
                                    for (int i = 0; i < adapterCount; i++) {
                                        VoTestItem voTestItem = (VoTestItem) listItemAdapter.getItem(i);
                                        Map<String, String> mapTestHistoryDetail = new HashMap<>();
                                        mapTestHistoryDetail.put("clm_test_history_seq", lmsTestSeq);
                                        mapTestHistoryDetail.put("clm_test_model_id", voTestItem.getTest_model_id());
                                        mapTestHistoryDetail.put("clm_test_item_seq", voTestItem.getTest_item_seq());
                                        mapTestHistoryDetail.put("clm_test_item_name", voTestItem.getTest_item_name());
                                        mapTestHistoryDetail.put("clm_test_item_command", voTestItem.getTest_item_command());
                                        mapTestHistoryDetail.put("clm_test_item_result", voTestItem.getTest_item_result());
                                        mapTestHistoryDetail.put("clm_test_item_value", voTestItem.getTest_item_value());
                                        mapTestHistoryDetail.put("clm_test_response_value", voTestItem.getTest_response_value());
                                        mapTestHistoryDetail.put("clm_test_result_value", voTestItem.getTest_result_value());
                                        mapTestHistoryDetail.put("clm_test_temperature", voTestItem.getTest_temperature());
                                        mapTestHistoryDetail.put("clm_test_electric_val", voTestItem.getTest_electric_val());
                                        mapTestHistoryDetail.put("clm_test_result_check_value", voTestItem.getTest_result_check_value());
                                        mapTestHistoryDetail.put("clm_bt_raw_message", voTestItem.getTest_bt_raw_message());
                                        mapTestHistoryDetail.put("clm_bt_raw_response", voTestItem.getTest_bt_raw_response());
                                        mapTestHistoryDetail.put("clm_bt_processed_value", voTestItem.getTest_bt_processed_value());

                                        // 상한값/하한값 조회 (lstData에서 해당 test_item_command로 찾기)
                                        String testItemCommand = voTestItem.getTest_item_command();
                                        String medianValue = "";
                                        String upperValue = "";
                                        String lowerValue = "";
                                        if (lstDataTmp != null && testItemCommand != null) {
                                            for (Map<String, String> specData : lstDataTmp) {
                                                if (testItemCommand.equals(specData.get(Constants.JsonKeys.CLM_TEST_COMMAND))) {
                                                    // 와트 기준값이 있는 경우 02 값 사용, 없으면 일반 값 사용
                                                    if (testItemCommand.contains(Constants.TestItemCodes.CM0101) || testItemCommand.contains(Constants.TestItemCodes.HT0101) ||
                                                            testItemCommand.contains(Constants.TestItemCodes.SV0101) || testItemCommand.contains(Constants.TestItemCodes.SV0201) ||
                                                            testItemCommand.contains(Constants.TestItemCodes.SV0301) || testItemCommand.contains(Constants.TestItemCodes.SV0401) ||
                                                            testItemCommand.contains(Constants.TestItemCodes.PM0101)) {
                                                        if (specData.get(Constants.JsonKeys.CLM_VALUE_WATT) != null && !specData.get(Constants.JsonKeys.CLM_VALUE_WATT).isEmpty()) {
                                                            upperValue = specData.get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT) != null ? specData.get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT) : "0";
                                                            lowerValue = specData.get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT) != null ? specData.get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT) : "0";
                                                            medianValue = specData.get(Constants.JsonKeys.CLM_VALUE_WATT) != null ? specData.get(Constants.JsonKeys.CLM_VALUE_WATT) : "0";
                                                            upperValue = String.valueOf(Double.parseDouble(upperValue) + Double.parseDouble(medianValue));
                                                            lowerValue = String.valueOf(Double.parseDouble(medianValue) - Double.parseDouble(lowerValue));
                                                        } else {
                                                            upperValue = specData.get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT) != null ? specData.get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT) : "0";
                                                            lowerValue = specData.get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT) != null ? specData.get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT) : "0";
                                                            medianValue = specData.get(Constants.JsonKeys.CLM_VALUE_WATT) != null ? specData.get(Constants.JsonKeys.CLM_VALUE_WATT) : "0";
                                                            upperValue = String.valueOf(Double.parseDouble(upperValue) + Double.parseDouble(medianValue));
                                                            lowerValue = String.valueOf(Double.parseDouble(medianValue) - Double.parseDouble(lowerValue));
                                                        }
                                                    } else if (testItemCommand.contains(Constants.TestItemCodes.TH0101) || testItemCommand.contains(Constants.TestItemCodes.TH0201) ||
                                                            testItemCommand.contains(Constants.TestItemCodes.TH0301)) {
                                                        if (specData.get(Constants.JsonKeys.CLM_VALUE) != null && !specData.get(Constants.JsonKeys.CLM_VALUE).isEmpty()) {
                                                            upperValue = specData.get(Constants.JsonKeys.CLM_UPPER_VALUE) != null ? specData.get(Constants.JsonKeys.CLM_UPPER_VALUE) : "0";
                                                            lowerValue = specData.get(Constants.JsonKeys.CLM_LOWER_VALUE) != null ? specData.get(Constants.JsonKeys.CLM_LOWER_VALUE) : "0";
                                                            medianValue = specData.get(Constants.JsonKeys.CLM_VALUE) != null ? specData.get(Constants.JsonKeys.CLM_VALUE) : "0";
                                                            upperValue = String.valueOf(Double.parseDouble(upperValue) + Double.parseDouble(medianValue));
                                                            lowerValue = String.valueOf(Double.parseDouble(medianValue) - Double.parseDouble(lowerValue));
                                                        } else {
                                                            upperValue = specData.get(Constants.JsonKeys.CLM_UPPER_VALUE) != null ? specData.get(Constants.JsonKeys.CLM_UPPER_VALUE) : "0";
                                                            lowerValue = specData.get(Constants.JsonKeys.CLM_LOWER_VALUE) != null ? specData.get(Constants.JsonKeys.CLM_LOWER_VALUE) : "0";
                                                            medianValue = specData.get(Constants.JsonKeys.CLM_VALUE) != null ? specData.get(Constants.JsonKeys.CLM_VALUE) : "0";
                                                            upperValue = String.valueOf(Double.parseDouble(upperValue) + Double.parseDouble(medianValue));
                                                            lowerValue = String.valueOf(Double.parseDouble(medianValue) - Double.parseDouble(lowerValue));
                                                        }
                                                    }
                                                    break;
                                                }
                                            }
                                        }
                                        if (testItemCommand.contains(Constants.TestItemCodes.CM0101) || testItemCommand.contains(Constants.TestItemCodes.HT0101) ||
                                                testItemCommand.contains(Constants.TestItemCodes.SV0101) || testItemCommand.contains(Constants.TestItemCodes.SV0201) ||
                                                testItemCommand.contains(Constants.TestItemCodes.SV0301) || testItemCommand.contains(Constants.TestItemCodes.SV0401) ||
                                                testItemCommand.contains(Constants.TestItemCodes.PM0101) ||
                                                testItemCommand.contains(Constants.TestItemCodes.TH0101) || testItemCommand.contains(Constants.TestItemCodes.TH0201) ||
                                                testItemCommand.contains(Constants.TestItemCodes.TH0301)) {
                                            mapTestHistoryDetail.put("clm_test_item_info", lowerValue + " < " + voTestItem.getTest_item_info() + " < " + upperValue);
                                        } else {
                                            mapTestHistoryDetail.put("clm_test_item_info", voTestItem.getTest_item_info());
                                        }
                                        mapTestHistoryDetail.put("clm_test_upper_value", upperValue);
                                        mapTestHistoryDetail.put("clm_test_lower_value", lowerValue);
                                        mapTestHistoryDetail.put("clm_comment", "");
                                        testHistoryDetailList.add(mapTestHistoryDetail);
                                    }

                                    // 백그라운드 스레드에서 데이터베이스 작업 수행
                                    final Map<String, String> finalMapTestHistory = new HashMap<>(mapTestHistory);
                                    final List<Map<String, String>> finalLstDataTmp = lstDataTmp != null ? new ArrayList<>(lstDataTmp) : null;
                                    runOnBtWorker(() -> {
                                        try {
                                            // 메인 테스트 이력 저장
                                            TestData.insertProductTestHistory(getBaseContext(), finalMapTestHistory);

                                            // 검사 상세 내역 테이블 생성
                                            TestData.createProductTestHistoryDetail(getBaseContext());

                                            // 검사 상세 내역 저장
                                            for (Map<String, String> mapTestHistoryDetail : testHistoryDetailList) {
                                                TestData.insertProductTestHistoryDetail(getBaseContext(), mapTestHistoryDetail);
                                            }
                                        } catch (Exception e) {
                                            logError(LogManager.LogCategory.ER, "Error saving test history to database in background thread", e);
                                        }
                                    });

                                    processFinished = true;
                                    clTestResult.setVisibility(VISIBLE);

                                    tvTestOkCnt.setText(String.valueOf(testOkCnt));
                                    tvTestNgCnt.setText(String.valueOf(testNgCnt));

                                    // 서버로 결과 전달

                                    // Phase 3: Timer 중복 생성 방지 - synchronized 블록으로 동시성 제어
                                    synchronized (finishedRestartTimerLock) {
                                        // 이미 실행 중이면 중복 시작 방지
                                        if (finishedRestartTimerRunning.get()) {
                                            logWarn(LogManager.LogCategory.PS, "Finished restart timer already running, skipping start");
                                            return;
                                        }

                                        // 기존 Timer 인스턴스가 있으면 먼저 정리 (안전성 향상)
                                        if (tmrFinishedRestart != null) {
                                            tmrFinishedRestart.cancel();
                                            tmrFinishedRestart.purge();
                                            tmrFinishedRestart = null;
                                        }
                                        if (ttFinishedRestart != null) {
                                            ttFinishedRestart.cancel();
                                            ttFinishedRestart = null;
                                        }

                                        try {
                                            tmrFinishedRestart = new Timer();
                                            ttFinishedRestart = new TimerTask() {
                                                @Override
                                                public void run() {
                                                    // logInfo(LogManager.LogCategory.PS, String.format(Constants.LogMessages.FINISHED_RESTART_APP_TERMINATION + Constants.Common.EMPTY_STRING + "[%d / %d]", restartCntFinished, restartCntMargin));
                                                    if (restartCntFinished == restartCntMargin) {
                                                        restartCntFinished = 0;
                                                        usbReconnectAttempts = 0;
                                                        finishedCorrectly = true;
                                                        // finishApplication(getApplicationContext());
                                                        resetBluetoothSessionKeepUsb();
                                                        if (tmrFinishedRestart != null) {
                                                            tmrFinishedRestart.cancel();
                                                        }
                                                    }
                                                    restartCntFinished++;
                                                }
                                            };
                                            tmrFinishedRestart.schedule(ttFinishedRestart, 0, Constants.Timeouts.TIMER_INTERVAL_MS);
                                            finishedRestartTimerRunning.set(true);
                                        } catch (Exception e) {
                                            finishedRestartTimerRunning.set(false);
                                            logError(LogManager.LogCategory.ER, "Error starting finished restart timer", e);
                                        }
                                    }
                                });
                            } catch (Exception e) {
                                logError(LogManager.LogCategory.ER, Constants.ErrorMessages.THREAD_EXECUTION_ERROR_IN_RE0101, e);
                            }
                            // }
                            // });
                        } else {
                            processFinished = false;
                        }

                        // logInfo(LogManager.LogCategory.PS, ">>>>>> 2.receiveCommand " + receiveCommand);
                        // receiveCommand = "";

                        // Fallback: cached watt values for commands that skipped UI updates this cycle
                        if (finalReceiveCommand.equals(Constants.TestItemCodes.CM0101) || finalReceiveCommand.equals(Constants.TestItemCodes.CM0102)) {
                            updateCompWatt = ensureWattText(Constants.TestItemCodes.CM0101, updateCompWatt);
                        }
                        if (finalReceiveCommand.equals(Constants.TestItemCodes.HT0101)) {
                            updateHeaterWatt = ensureWattText(Constants.TestItemCodes.HT0101, updateHeaterWatt);
                        }
                        if (finalReceiveCommand.equals(Constants.TestItemCodes.PM0101)) {
                            updatePumpWatt = ensureWattText(Constants.TestItemCodes.PM0101, updatePumpWatt);
                        }
                        if (isSolValveCommand(finalReceiveCommand)) {
                            updateItemInfo = ensureWattText(finalReceiveCommand, updateItemInfo);
                        }

                        UiUpdateBundle.Builder uiBuilder = new UiUpdateBundle.Builder()
                                .setDialogVisible(shouldUpdateDialog && !shouldHideDialog)
                                .setDialogHidden(shouldHideDialog)
                                .setDialogColor(dialogColor)
                                .setDialogMessage(dialogMessage)
                                .setTemperatureText(updateTemperature)
                                .setCompWattText(updateCompWatt)
                                .setHeaterWattText(updateHeaterWatt)
                                .setPumpWattText(updatePumpWatt)
                                .setLogText(updateLogText)
                                .setUpdateItemCommand(updateItemCommand)
                                .setUpdateItemResult(updateItemResult)
                                .setUpdateItemCheckValue(updateItemCheckValue)
                                .setUpdateItemInfo(updateItemInfo)
                                .setUpdateItemNameSuffix(updateItemNameSuffix)
                                .setUpdateListAdapter(shouldUpdateListAdapter)
                                .setFinalReceiveCommandResponse(finalReceiveCommandResponse)
                                .setFinalCalculatedResultValue(calculatedResultValue)
                                .setFinalReadMessage(finalReadMessage)
                                .setTemperatureValueCompDiff(temperatureValueCompDiff)
                                .setResultInfo(resultInfo)
                                .setDecTemperatureHotValue(decTemperatureHotValue)
                                .setDecTemperatureColdValue(decTemperatureColdValue)
                                .setFinalCurrentTestItem(finalCurrentTestItem)
                                .setTestItemIdx(testItemIdx)
                                .setTestOkCnt(testOkCnt)
                                .setTestNgCnt(testNgCnt)
                                .setReceiveCommandResponseOK(receiveCommandResponseOK)
                                .setShouldUpdateCounts(true)
                                .setListItemAdapter(listItemAdapter);

                        UiUpdateBundle uiBundle = uiBuilder.build();
                        // Phase 2: 단순 UI 업데이트를 scheduleUiUpdate로 변경
                        scheduleUiUpdate(() -> applyUiBundle(uiBundle));

                    } catch (Exception e) {
                        logError(LogManager.LogCategory.ER, Constants.ErrorMessages.ERROR_IN_BACKGROUND_PROCESSING, e);
                        // 복잡한 UI 업데이트 블록 통합: scheduleUiUpdate 사용
                        scheduleUiUpdate(() -> {
                            sendBtMessage(finalCurrentTestItem);
                        });
                    }
                    // }); // 백그라운드 스레드 종료
                } catch (Exception e) {
                    logError(LogManager.LogCategory.ER, Constants.ErrorMessages.BLUETOOTH_MESSAGE_HANDLING_ERROR, e);
                    // 복잡한 UI 업데이트 블록 통합: scheduleUiUpdate 사용
                    scheduleUiUpdate(() -> {
                        sendBtMessage(finalCurrentTestItem);
                    });
                }
                // });
            }
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, Constants.ErrorMessages.BLUETOOTH_HANDLER_ERROR, e);
        }
        // }
//        if (raw == null || raw.length == 0) {
//            return;
//        }
//
//        final String readMessage;
//        try {
//            readMessage = new String(raw, StandardCharsets.UTF_8);
//        } catch (Exception e) {
//            logError(LogManager.LogCategory.BT, "Failed to decode BT message", e);
//            return;
//        }
//
//        if (arrTestItems == null || listItemAdapter == null) {
//            logWarn(LogManager.LogCategory.BT, "arrTestItems or listItemAdapter not initialized yet, skipping message processing");
//            return;
//        }
//
//        if (testItemIdx > arrTestItems.length - 1) {
//            testItemIdx = arrTestItems.length - 1;
//        }
//
//        if (readMessage.contains(Constants.TestItemCodes.ST0101)) {
//            testItemIdx = 0;
//            if (listItemAdapter.getCount() > testItemIdx) {
//                VoTestItem firstItem = (VoTestItem) listItemAdapter.getItem(testItemIdx);
//                firstItem.setTest_item_result(Constants.ResultStatus.OK);
//                firstItem.setTest_result_value(Constants.ResultStatus.COMP_AGING_RESPONSE_01 + Constants.Common.LOGGER_DEVIDER_01 + Constants.ResultStatus.COMP_AGING_RESPONSE_01);
//                firstItem.setTest_finish_yn(Constants.ResultStatus.YES);
//            } else {
//                logWarn(LogManager.LogCategory.BT, "listItemAdapter not ready, cannot update test item result");
//            }
//            if (testProcessId.equals(Constants.Common.EMPTY_STRING)) {
//                testProcessId = getCurrentDatetime(timestampFormat);
//                logInfo(LogManager.LogCategory.PS, Constants.LogMessages.TEST_PROCESS_ID_STARTED + testProcessId);
//            }
//            entranceCheck = true;
//            ensureUnfinishedRestartTimer();
//
//            scheduleUiUpdate(() -> {
//                clAlert.setVisibility(View.GONE);
//                tvCurrentProcess.setText(((VoTestItem) listItemAdapter.getItem(testItemIdx)).getTest_item_name());
//                tvTestOkCnt.setText(String.valueOf(testOkCnt));
//                tvTestNgCnt.setText(String.valueOf(testNgCnt));
//                lvTestItem.smoothScrollToPosition(testItemIdx);
//            });
//        }
//
//        // 공통 카운터 및 UI 텍스트 갱신
//        testItemCounter++;
//        testTotalCounter++;
//        scheduleUiUpdate(() -> tvEllapsedTimeCnt.setText(String.valueOf(testTotalCounter)));
//
//        // wattTemp 기록
//        wattTemp = new String[]{getCurrentDatetime(timestampFormat), calculatedTemperatureTmp, String.valueOf(decElectricValue), currentTestItem};
//        lstMapWattTemp.add(wattTemp);
//
//        receivedMessageCnt++;
//        String currentProcessName = ((VoTestItem) listItemAdapter.getItem(testItemIdx)).getTest_item_name();
//        scheduleUiUpdate(() -> {
//            tvCurrentProcess.setText(currentProcessName);
//            tvRunWsRamp.setText(receivedMessageCnt + " / " + sendingMessageCnt);
//        });
//
//        // ===== 여기서부터는 기존 runOnBtWorker 블록의 처리 내용 =====
//        if (lstSpecData == null || lstSpecData.isEmpty()) {
//            return;
//        }
//
//        String finalReceiveCommand = currentTestItem;
//        String finalReceiveCommandResponse = readMessage;
//        String finalReadMessage = readMessage;
//
//        String calculatedReceiveResponseResult = Constants.Common.EMPTY_STRING;
//        String calculatedResultValue = Constants.Common.EMPTY_STRING;
//        String calculatedDecTemperature = Constants.Common.EMPTY_STRING;
//        int calculatedDecTemperatureValue = 0;
//        String calculatedReceiveCommandResponse = Constants.Common.EMPTY_STRING;
//        double calculatedDblValTemp = 0.0;
//        int calculatedWattLower = 0;
//        int calculatedWattUpper = 0;
//        String calculatedReceiveCompAgingResponse = Constants.Common.EMPTY_STRING;
//        String calculatedReceiveResponseResultTxt = Constants.Common.EMPTY_STRING;
//        String calculatedResultInfo = Constants.Common.EMPTY_STRING;
//        boolean shouldUpdateDialog = false;
//        int dialogColor = 0;
//        String dialogMessage = "";
//        boolean shouldHideDialog = false;
//        String calculatedTemperatureValueCompDiff = "";
//
//        String updateTemperature = null;
//        String updateCompWatt = null;
//        String updateHeaterWatt = null;
//        String updatePumpWatt = null;
//        String updateLogText = null;
//        boolean shouldUpdateListAdapter = false;
//        String updateItemCommand = "";
//        String updateItemResult = "";
//        String updateItemCheckValue = "";
//        String updateItemInfo = "";
//        String updateItemNameSuffix = "";
//
//        List<Map<String, String>> temperatureData;
//
//        // ---- 이하 내용은 기존 runOnBtWorker 내부 로직을 그대로 이동 (온도/소비전력 분기, UI 업데이트 준비 등) ----
//        // (파일 상의 기존 코드 블록: TH0101/TH0201 온도 계산, CM0101/HT0101/PM0101/SVxxxx 소비전력 처리,
//        //  logBtTestResponse 호출, updateItemXXX 값 세팅, UiUpdateBundle 생성 등)
//        // 해당 블록을 그대로 이 위치로 옮기되, UI 접근은 모두 scheduleUiUpdate()로 감싸 주세요.
//
//        // 예시: 최종 UiUpdateBundle 빌드 및 UI 반영
//        UiUpdateBundle uiBundle = new UiUpdateBundle.Builder()
//                .setDialogVisible(shouldUpdateDialog && !shouldHideDialog)
//                .setDialogHidden(shouldHideDialog)
//                .setDialogColor(dialogColor)
//                .setDialogMessage(dialogMessage)
//                .setTemperatureText(updateTemperature)
//                .setCompWattText(updateCompWatt)
//                .setHeaterWattText(updateHeaterWatt)
//                .setPumpWattText(updatePumpWatt)
//                .setLogText(updateLogText)
//                .setUpdateItemCommand(updateItemCommand)
//                .setUpdateItemResult(updateItemResult)
//                .setUpdateItemCheckValue(updateItemCheckValue)
//                .setUpdateItemInfo(updateItemInfo)
//                .setUpdateItemNameSuffix(updateItemNameSuffix)
//                .setUpdateListAdapter(shouldUpdateListAdapter)
//                .setFinalReceiveCommandResponse(finalReceiveCommandResponse)
//                .setFinalCalculatedResultValue(calculatedResultValue)
//                .setFinalReadMessage(finalReadMessage)
//                .setTemperatureValueCompDiff(temperatureValueCompDiff)
//                .setResultInfo(resultInfo)
//                .setDecTemperatureHotValue(decTemperatureHotValue)
//                .setDecTemperatureColdValue(decTemperatureColdValue)
//                .setFinalCurrentTestItem(finalReceiveCommand)
//                .setTestItemIdx(testItemIdx)
//                .setTestOkCnt(testOkCnt)
//                .setTestNgCnt(testNgCnt)
//                .setReceiveCommandResponseOK(receiveCommandResponseOK)
//                .setShouldUpdateCounts(true)
//                .setListItemAdapter(listItemAdapter)
//                .build();
//
//        scheduleUiUpdate(() -> applyUiBundle(uiBundle));
    }

    private boolean processFinished = false;

    private void initializeAppStateAsync() {
        try {
            Intent intent = getIntent();
            globalModelId = intent.getStringExtra(Constants.IntentExtras.TEST_MODEL_ID);
            // globalModelId = intent.getStringExtra("00000002");
            globalModelName = intent.getStringExtra(Constants.IntentExtras.TEST_MODEL_NAME);
            globalModelNation = intent.getStringExtra(Constants.IntentExtras.TEST_MODEL_NATION);
            globalLastTestStartTimestamp = intent.getStringExtra(Constants.IntentExtras.TEST_START_DATETIME);
            if (globalLastTestStartTimestamp == null) {
                globalLastTestStartTimestamp = lmsTestSeq;
            }

            SharedPreferences testCookie = getSharedPreferences(Constants.SharedPrefKeys.TEST_COOKIE_INFO, MODE_PRIVATE);
            String lastStartFromCookie = testCookie.getString(Constants.SharedPrefKeys.TEST_START_DATETIME, Constants.Common.EMPTY_STRING);
            if (!TextUtils.isEmpty(lastStartFromCookie)) {
                globalLastTestStartTimestamp = lastStartFromCookie;
            }

            // logInfo(LogManager.LogCategory.SI, "globalLastTestStartTimestamp:" + globalLastTestStartTimestamp + " lmsTestSeq:" + lmsTestSeq);
            String reqDateStr = (globalLastTestStartTimestamp == null) ? lmsTestSeq : globalLastTestStartTimestamp;
            min_diff = minDiff(reqDateStr);
            btInfoList = new ArrayList<>();

            ActivityModelTestProcess activity = getMainActivity();
            if (bln_test_yn && activity != null && !activity.isFinishing()) {
                activity.scheduleUiUpdate(() -> {
                    cl_dialog_for_logger.setVisibility(VISIBLE);
                    tv_dialog_for_logger_watt.setVisibility(VISIBLE);
                    tv_dialog_for_logger_temp.setVisibility(VISIBLE);
                });
                test_logger = Constants.Common.EMPTY_STRING;
                test_logger_temp = Constants.Common.EMPTY_STRING;
            }

            scheduleUiUpdate(() -> {
                tvModelNationality.setText(globalModelNation);
                tvModelName.setText(globalModelName);
                tvModelId.setText(globalModelId);
                clAlert.setVisibility(GONE);
                clDialogForPreprocess.setVisibility(GONE);
            });

            postTestInfoColor = getBaseContext().getResources().getColor(R.color.gray_04);
            preferences = getSharedPreferences(Constants.SharedPrefKeys.TEST_INFO, MODE_PRIVATE);
            test_info = preferences.edit();

            loadTemperatureData();

            // logInfo(LogManager.LogCategory.PS, "globalModelId: " + globalModelId);
            // logInfo(LogManager.LogCategory.PS, "globalModelName: " + globalModelName);
            // logInfo(LogManager.LogCategory.PS, "globalModelNation: " + globalModelNation);

            serverDomain = Constants.ServerConfig.SERVER_DOMAIN_192_168_0;
            serverIp = Constants.ServerConfig.SERVER_IP_192_168_0_47;
            serverDomain = Constants.ServerConfig.SERVER_DOMAIN_172_16_1;
            serverIp = Constants.ServerConfig.SERVER_IP_172_16_1_249_8080;
            if (mode_type.equals(Constants.ResultStatus.MODE_TYPE_TEST)) {
                serverIp = Constants.ServerConfig.SERVER_IP_PORT_ITF;
            } else {
                serverIp = Constants.ServerConfig.SERVER_IP_PORT;
            }

            // globalModelId = "00000002";
            urlStr = Constants.URLs.HTTP_PROTOCOL + serverIp + Constants.URLs.ENDPOINT_TEST_INFO_LIST
                    + Constants.Common.QUESTION + Constants.URLs.PARAM_CALL_TYPE + Constants.Common.AMPERSAND
                    + Constants.URLs.PARAM_MODEL_ID + globalModelId;
            // logInfo(LogManager.LogCategory.SI, Constants.LogMessages.SETTING_SERVER_URL + urlStr);

            Path directoryPath = Paths.get(Constants.FilePaths.FOLDER_NAME);
            try {
                Files.createDirectories(directoryPath);
                if (Files.exists(directoryPath)) {
                    // logInfo(LogManager.LogCategory.PS, Constants.LogMessages.DIRECTORY_READY + directoryPath);
                }
            } catch (IOException e) {
                logError(LogManager.LogCategory.ER, Constants.ErrorMessages.DIRECTORY_CREATION_IO_EXCEPTION, e);
            }

            ipAddress = getLocalIpAddress();
            if (!TextUtils.isEmpty(ipAddress)) {
                int unit_no_from_ip = Integer.parseInt(ipAddress.split("\\.")[3]);
                unit_no = (unit_no_from_ip<=Constants.Network.IP_ADDRESS_OFFSET)?unit_no_from_ip:unit_no_from_ip - Constants.Network.IP_ADDRESS_OFFSET;
                // unit_no = Math.abs(Integer.parseInt(ipAddress.split("\\.")[3]) - Constants.Network.IP_ADDRESS_OFFSET);
            }
            urlStrBarcode = Constants.URLs.HTTP_PROTOCOL + serverIp + Constants.URLs.ENDPOINT_PRODUCT_SERIAL_INFO_LIST
                    + Constants.Common.QUESTION + Constants.URLs.PARAM_UNIT_NO + unit_no
                    + Constants.Common.AMPERSAND + Constants.URLs.PARAM_MODEL_ID + globalModelId;

            final String finalUnitNo = String.format(Constants.Common.NUMBER_FORMAT_03D, unit_no);
            scheduleUiUpdate(() -> {
                if (!TextUtils.isEmpty(ipAddress)) {
                    tvRunWsRamp.setText(finalUnitNo);
                    tvRunWsRamp.setBackgroundColor(getBaseContext().getResources().getColor(R.color.blue_01));
                } else {
                    // tvRunWsRamp.setText(Constants.Common.EMPTY_STRING);
                    tvRunWsRamp.setBackgroundColor(getBaseContext().getResources().getColor(R.color.red_01));
                }
            });
            // logInfo(LogManager.LogCategory.SI, Constants.LogMessages.GET_WEB_IP_INFO + ipAddress);

            SharedPreferences testPrefs = getSharedPreferences(Constants.SharedPrefKeys.TEST_INFO, MODE_PRIVATE);
            String testResult = testPrefs.getString(Constants.SharedPrefKeys.TEST_RESULT, Constants.Common.EMPTY_STRING);
            String heaterValue = testPrefs.getString(Constants.SharedPrefKeys.HEATER_VALUE, Constants.Common.EMPTY_STRING);
            String compValue = testPrefs.getString(Constants.SharedPrefKeys.COMP_VALUE, Constants.Common.EMPTY_STRING);
            scheduleUiUpdate(() -> {
                tvUnitId.setText(Constants.Common.UNIT_ID_PREFIX + "-" + finalUnitNo);
                logWarn(LogManager.LogCategory.BT, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> CCC " + isControlMode);
                if (TextUtils.isEmpty(testResult)) {
                    clTestResult.setVisibility(GONE);
                } else {
                    clTestResult.setVisibility(VISIBLE);
                    tvPopupProcessResult.setText(testResult);
                    tvPopupProcessResultCompValue.setText(compValue);
                    tvPopupProcessResultHeaterValue.setText(heaterValue);
                }
            });

            // logInfo(LogManager.LogCategory.SI, Constants.LogMessages.WEBSERVER_STARTED);

            if (usbService == null || !UsbService.SERVICE_CONNECTED || !usbConnPermissionGranted) {
                scheduleUiUpdate(this::restartUsbMonitoring);
                updateUsbLampDisconnected();
                scheduleUsbReconnect(true);
            } else if (!isUsbReady()) {
                updateUsbLampReconnecting();
                scheduleUsbReconnect(true);
            } else {
                updateUsbLampReady();
            }
            scheduleUiUpdate(this::bluetoothOn);

            loadTestSpecData();
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, "Error during async initialization", e);
            scheduleUiUpdate(() -> {
                clAlert.setVisibility(VISIBLE);
                tvAlertMessage.setText(getStringResource("ui.message.server_connection_failed"));
            });
        }
    }

    private void loadTemperatureData() {
        try {
            // logInfo(LogManager.LogCategory.SI, Constants.LogMessages.READ_SETTING_INFO);
            hotTemperatureData = TestData.selectTemperatureData(this, Constants.Database.TEMPERATURE_TYPE_HOT);
            coldTemperatureData = TestData.selectTemperatureData(this, Constants.Database.TEMPERATURE_TYPE_COLD);
            if (hotTemperatureData.isEmpty() || coldTemperatureData.isEmpty()) {
                readTemperatureExcel(Constants.Database.TEMPERATURE_TYPE_HOT, Constants.FilePaths.TEMPERATURE_INFO_XLS);
                readTemperatureExcel(Constants.Database.TEMPERATURE_TYPE_COLD, Constants.FilePaths.TEMPERATURE_INFO_XLS);
                // logInfo(LogManager.LogCategory.SI, Constants.LogMessages.INSERT_TEMP_INFO);
                hotTemperatureData = TestData.selectTemperatureData(this, Constants.Database.TEMPERATURE_TYPE_HOT);
                coldTemperatureData = TestData.selectTemperatureData(this, Constants.Database.TEMPERATURE_TYPE_COLD);
            }
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, Constants.ErrorMessages.TEMPERATURE_DATA_READ_ERROR, e);
        }
    }

    private void loadTestSpecData() {
        AtomicBoolean specDataLoadedFromCache = new AtomicBoolean(false);
        try {
            lstSpecData = TestData.selectTestSpecData(getBaseContext(),
                    Constants.Database.QUERY_AND_1_EQUALS_1 + Constants.Common.SPACE + Constants.Database.QUERY_AND + Constants.Common.SPACE +
                            Constants.JsonKeys.CLM_MODEL_ID + Constants.Common.EQUAL + Constants.Common.SINGLE_QUETATION + globalModelId + Constants.Common.SINGLE_QUETATION);
            if (lstSpecData != null && !lstSpecData.isEmpty()) {
                // if (BuildConfig.DEBUG) {
                for (int i = 0; i < lstSpecData.size(); i++) {
                    logDebug(LogManager.LogCategory.PS, String.format("Test spec data [%d]: %s", i, lstSpecData.get(i)));
                    if (i == 0) {
                        test_version_id = lstSpecData.get(i).get("clm_test_version_id");
                        model_id = lstSpecData.get(i).get("clm_model_id");
                    }
                }
                // }
                scheduleUiUpdate(() -> {
                    try {
                        specDataLoadedFromCache.set(applyTestSpecData(lstSpecData, false));
                        lstDataTmp = lstSpecData;
                        if (!specDataLoadedFromCache.get()) {
                            try {
                                clearHttpHandlerQueue();
                                new ActivityModelTestProcess.RequestThreadAsync(ActivityModelTestProcess.this).execute();
                            } catch (Exception e) {
                                logError(LogManager.LogCategory.ER, Constants.ErrorMessages.SERVER_CONNECTION_ERROR, e);
                                clAlert.setVisibility(VISIBLE);
                                tvAlertMessage.setText(getStringResource("ui.message.server_connection_failed"));
                            }
                        } else {
                            switch (mode_type) {
                                case Constants.ResultStatus.MODE_TYPE_TEST:
                                    String localVersionId = null;
                                    String localModelVersion = null;
                                    if (arrTestModels != null && arrTestModels[0] != null && arrTestModels[0].length > 4) {
                                        localVersionId = arrTestModels[0][3];
                                        localModelVersion = arrTestModels[0][4];
                                    }
                                    new ActivityModelTestProcess.RequestVersionInfoThreadAsync(ActivityModelTestProcess.this, localVersionId, localModelVersion).execute();
                                    break;
                                default:
                                    try {
                                        clearHttpHandlerQueue();
                                        new ActivityModelTestProcess.RequestThreadAsync(ActivityModelTestProcess.this).execute();
                                    } catch (Exception e) {
                                        logError(LogManager.LogCategory.ER, Constants.ErrorMessages.SERVER_CONNECTION_ERROR, e);
                                        clAlert.setVisibility(VISIBLE);
                                        tvAlertMessage.setText(getStringResource("ui.message.server_connection_failed"));
                                    }
                                    break;
                            }
                        }
                    } catch (Exception e) {
                        logWarn(LogManager.LogCategory.ER, Constants.ErrorMessages.LIST_SPEC_DATA_ERROR + e.getMessage());
                    }
                });
            } else {
                scheduleUiUpdate(() -> {
                    try {
                        clearHttpHandlerQueue();
                        new ActivityModelTestProcess.RequestThreadAsync(ActivityModelTestProcess.this).execute();
                    } catch (Exception e) {
                        logError(LogManager.LogCategory.ER, Constants.ErrorMessages.SERVER_CONNECTION_ERROR, e);
                        clAlert.setVisibility(VISIBLE);
                        tvAlertMessage.setText(getStringResource("ui.message.server_connection_failed"));
                    }
                });
            }
        } catch (Exception e) {
            logWarn(LogManager.LogCategory.ER, Constants.ErrorMessages.LIST_SPEC_DATA_ERROR + e.getMessage());
            scheduleUiUpdate(() -> {
                try {
                    clearHttpHandlerQueue();
                    new ActivityModelTestProcess.RequestThreadAsync(ActivityModelTestProcess.this).execute();
                } catch (Exception ex) {
                    logError(LogManager.LogCategory.ER, Constants.ErrorMessages.SERVER_CONNECTION_ERROR, ex);
                }
            });
        }
    }

    /**
     * 워커 스레드에서 HTTP 전송 및 후속 DB 업데이트 수행
     */
    private void executeResultUpload(String checkValue, String lmsSeq) {
        String httpSuccessYn = Constants.ResultStatus.NO;
        String targetUrl = Constants.URLs.HTTP_PROTOCOL + serverIp + Constants.URLs.ENDPOINT_UPDATE_RESULT_TEST_INFO
                + Constants.Common.QUESTION
                + Constants.URLs.PARAM_PRODUCT_SERIAL_NO + productSerialNo
                + Constants.Common.AMPERSAND + Constants.URLs.PARAM_TEST_PROCESS + 3
                + Constants.Common.AMPERSAND + Constants.URLs.PARAM_TEST_RESULT + ((testNgCnt > 0) ? Constants.ResultStatus.NG : Constants.ResultStatus.OK)
                + checkValue;

        // logInfo(LogManager.LogCategory.BI, Constants.LogMessages.TARGET_URL + " >>>>>>>>>>>>>>> " + targetUrl);

        boolean requestSuccess = false;
        for (int attempt = 1; attempt <= Constants.Network.HTTP_RETRY_COUNT; attempt++) {
            HttpURLConnection connection = null;
            try {
                // logInfo(LogManager.LogCategory.BI, "HTTP 요청 시도 [" + attempt + "/" + Constants.Network.HTTP_RETRY_COUNT + "]");
                URL url = new URL(targetUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setReadTimeout(Constants.Timeouts.HTTP_READ_TIMEOUT_MS);
                connection.setConnectTimeout(Constants.Timeouts.HTTP_CONNECT_TIMEOUT_MS);
                connection.setRequestMethod(Constants.HTTP.METHOD_GET);
                connection.setDoInput(true);
                connection.setDoOutput(true);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // logInfo(LogManager.LogCategory.BI, Constants.LogMessages.HTTP_OK_SUCCESS + " (시도 " + attempt + ")");
                    sendResultYn = Constants.ResultStatus.YES;
                    httpSuccessYn = Constants.ResultStatus.YES;
                    requestSuccess = true;
                    break;
                } else {
                    logWarn(LogManager.LogCategory.BI, Constants.LogMessages.HTTP_OK_FAILED + " (시도 " + attempt + ", code=" + responseCode + ")");
                    if (attempt < Constants.Network.HTTP_RETRY_COUNT) {
                        Thread.sleep(Constants.Network.HTTP_RETRY_DELAY_MS);
                    }
                }
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, Constants.ErrorMessages.HTTP_RESULT_UPDATE_ERROR + " (시도 " + attempt + "): " + e.getMessage(), e);
                if (attempt < Constants.Network.HTTP_RETRY_COUNT) {
                    try {
                        Thread.sleep(Constants.Network.HTTP_RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logError(LogManager.LogCategory.ER, "HTTP 재시도 대기 중 인터럽트 발생", ie);
                    }
                }
            } finally {
                safeDisconnectConnection(connection);
            }
        }

        if (!requestSuccess) {
            // logInfo(LogManager.LogCategory.PS, "HTTP 요청 실패: " + Constants.Network.HTTP_RETRY_COUNT + "번 시도 모두 실패");
        }

        try {
            TestData.updateProductTestHistoryHttpSuccess(getBaseContext(), lmsSeq, httpSuccessYn);
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, "Error updating HTTP success status in test history", e);
        }

        try {
            for (String[] wattTempData : lstMapWattTemp) {
                if (wattTempData != null && wattTempData.length >= 4) {
                    TestData.insertTestHistoryLinearData(
                            getBaseContext(),
                            lmsSeq,
                            wattTempData[3],
                            wattTempData[0],
                            wattTempData[1],
                            wattTempData[2]
                    );
                }
            }
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, "Error saving linear data to test history", e);
        }
    }

    // USB 장치 재탐색 및 권한 요청
    private void usbSearch() {
        try {
            final String ACTION_USB_PERMISSION = "itf.com.app.USB_PERMISSION";
            UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
            if (usbManager == null) {
                logWarn(LogManager.LogCategory.US, "UsbManager is null");
                return;
            }

            HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
            if (deviceList == null || deviceList.isEmpty()) {
                // logInfo(LogManager.LogCategory.US, "No USB devices found");
                return;
            }

            for (UsbDevice device : deviceList.values()) {
                // logInfo(LogManager.LogCategory.US, "USB device found: " + device.getDeviceName() + " vid=" + device.getVendorId() + " pid=" + device.getProductId());

                if (!usbManager.hasPermission(device)) {
                    Intent intent = new Intent(ACTION_USB_PERMISSION);
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                    usbManager.requestPermission(device, pendingIntent);
                    // logInfo(LogManager.LogCategory.US, "Requested USB permission for device");
                } else {
                    // logInfo(LogManager.LogCategory.US, "Already has permission for device");
                    if (usbService != null) {
                        // logInfo(LogManager.LogCategory.US, "usbService present - consider reopening device via service");
                        // usbService에 장치 연결 메소드가 있으면 호출 (메소드명/시그니처에 맞게 수정)
                        // 예: usbService.openDevice(device, usbManager);
                    } else {
                        Intent usbIntent = new Intent(getApplicationContext(), UsbService.class);
                        try {
                            stopService(usbIntent);
                        } catch (Exception ignored) {
                        }
                        try {
                            startService(usbIntent);
                        } catch (Exception e) {
                            logWarn(LogManager.LogCategory.US, "Start UsbService failed: " + e.getMessage());
                        }
                        // logInfo(LogManager.LogCategory.US, "UsbService start requested");
                    }
                }
                break; // 첫 번째 장치만 처리(필요 시 제거)
            }
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, "usbSearch error", e);
        }
    }

    private int minDiff(String reqDateStr) throws ParseException {
        Date curDate = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat(timestampFormat);
        Date reqDate = dateFormat.parse(reqDateStr);
        long reqDateTime = reqDate.getTime();
        curDate = dateFormat.parse(dateFormat.format(curDate));
        long curDateTime = curDate.getTime();
        int minute = (int) ((curDateTime - reqDateTime) / 60000);
        // logInfo(LogManager.LogCategory.PS, Constants.LogMessages.REQUEST_TIME + reqDate);
        // logInfo(LogManager.LogCategory.PS, Constants.LogMessages.CURRENT_TIME + curDate);
        // logInfo(LogManager.LogCategory.PS, minute + Constants.LogMessages.MINUTE_DIFFERENCE);
        return minute;
    }

    private void finishApplication(Context mContext) {
        new Thread(() -> {
            try {
                System.out.println("finishApplication: 재탐색 및 재연결 시도 시작 >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 0");
                receiveCompAgingResponse = Constants.ResultStatus.COMP_AGING_RESPONSE_01;
                testTotalCounter = 0;
                testItemCounter = 0;
                testItemIdx = 0;
                entranceCheck = false;
                btSearchOnOff = false; // 재탐색을 위해 플래그 초기화
                System.out.println("finishApplication: 재탐색 및 재연결 시도 시작 >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 1");
                clearBluetoothReconnect();
                System.out.println("finishApplication: 재탐색 및 재연결 시도 시작 >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 2");

                // ========== [수정] 블루투스 리소스 완전 정리 시작 ==========
                // 문제: fab_close 버튼으로 종료 시 블루투스 타이머와 핸들러가 종료되지 않아
                // 계속 메시지를 받는 문제 해결을 위해 리소스 정리 추가

                // 1. 블루투스 연결 스레드(btConnectedThread) 취소
                // 중요: btSocket을 닫기 전에 먼저 스레드를 취소해야 함
                // 그렇지 않으면 스레드가 계속 실행되며 btHandler로 메시지를 전송함
                if (btConnectedThread != null) {
                    try {
                        btConnectedThread.cancel();
                        logInfo(LogManager.LogCategory.BT, "Bluetooth connected thread cancelled in finishApplication");
                    } catch (Exception e) {
                        logWarn(LogManager.LogCategory.BT, "Error canceling Bluetooth connected thread in finishApplication: " + e.getMessage());
                    }
                    btConnectedThread = null;
                }

                // 2. 블루투스 핸들러(btHandler) 정리
                // 중요: btHandler는 메인 루퍼에 연결되어 있어 액티비티가 종료되어도
                // 계속 메시지를 받을 수 있으므로 명시적으로 정리해야 함
                clearBtHandlerQueue();
                logInfo(LogManager.LogCategory.BT, "Bluetooth handler queue cleared in finishApplication");

                // 3. 블루투스 연결 플래그 초기화
                btConnected = false;
                btConnectionInProgress = false;

                // 4. 기존 BT 소켓 안전 종료
                // 중요: btConnectedThread를 먼저 취소한 후 소켓을 닫아야 함
                if (btSocket != null && btSocket.isConnected()) {
                    try {
                        btSocket.close();
                        logInfo(LogManager.LogCategory.BT, "Bluetooth socket closed in finishApplication");
                    } catch (IOException e) {
                        logWarn(LogManager.LogCategory.BT, "Error closing Bluetooth socket in finishApplication: " + e.getMessage());
                    }
                    btSocket = null;
                } else if (btSocket != null) {
                    btSocket = null;
                }
                // ========== [수정] 블루투스 리소스 완전 정리 종료 ==========

                System.out.println("finishApplication: 재탐색 및 재연결 시도 시작 >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 3");
                deviceSelected = null;

                // 메인 스레드에서 타이머/스레드 정리 후 재탐색 시작
                // [수정] 블루투스 리소스 정리 후 재연결 시도
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    try {
                        stopBtMessageTimer();
                        new Thread(() -> {
                            try {
                                bluetoothSearch();
                            } catch (Exception e) {
                                logError(LogManager.LogCategory.ER, Constants.ErrorMessages.BLUETOOTH_SEARCH_ERROR, e);
                            }
                        }).start();
                    } catch (Exception e) {
                        logError(LogManager.LogCategory.ER, Constants.ErrorMessages.ERROR_IN_FINISH_APPLICATION_RECONNECT, e);
                    }
                });// Java
                System.out.println("finishApplication: 재탐색 및 재연결 시도 시작 >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 4");
                // [수정] 중복 bluetoothSearch 호출 제거 (이미 위에서 호출됨)
                // bluetoothSearch 호출 직후에 추가
                // new Thread(() -> {
                //     try {
                //         bluetoothSearch();
                //     } catch (Exception e) {
                //         logError(LogManager.LogCategory.ER, Constants.ErrorMessages.BLUETOOTH_SEARCH_ERROR, e);
                //     }
                // }).start();
                System.out.println("finishApplication: 재탐색 및 재연결 시도 시작 >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 5");

                // USB 재연결 시도 (메인 스레드에서 안전하게 로그/서비스 재시작)
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    try {
                        // 기존 USB 리소스 안전 종료 (변수명은 프로젝트에 맞게 변경)
                        try {
                            if (usbService != null) {
                                usbService = null;
                            }
                        } catch (Exception ignored) {
                        }

                        // USB 서비스가 앱에서 서비스로 구현되어 있다면 재시작 시도
                        try {
                            Intent usbIntent = new Intent(getApplicationContext(), UsbService.class); // 존재하는 서비스 클래스로 변경
                            try {
                                stopService(usbIntent);
                            } catch (Exception ignored) {
                            }
                            startService(usbIntent);
                            // logInfo(LogManager.LogCategory.US, "USB service restart requested");
                        } catch (Exception e) {
                            logWarn(LogManager.LogCategory.US, "USB service restart failed: " + e.getMessage());
                        }

                        // 또는 프로젝트에 usbSearch()가 있다면 재탐색 호출
                        try {
                            usbSearch();
                        } catch (Exception ignored) {
                        }
                    } catch (Exception e) {
                        logError(LogManager.LogCategory.ER, Constants.ErrorMessages.ERROR_IN_FINISH_APPLICATION_RECONNECT, e);
                    }
                });
                System.out.println("finishApplication: 재탐색 및 재연결 시도 시작 >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 7");
                currentTestItem = ST0101;

                // 모든 Timer 정리 (중복 제거 및 누락 방지)
                cleanupAllTimers();

                // ⚠️ 키오스크 모드: KioskModeApplication이 자동으로 모니터링 중지함 (onPause/onStop에서 처리됨)

                if (usbReceiverRegisted) {
                    // unregisterReceiver(usbReceiver);
                }
                if (usbConnPermissionGranted) {
                    unbindService(usbConnection);
                }
                System.out.println("finishApplication: 재탐색 및 재연결 시도 시작 >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 8");

                usbReceiverRegisted = false;
                usbConnPermissionGranted = false;

                // ⚠️ IMPORTANT: Reset SERVICE_CONNECTED flag to ensure proper reconnection
                UsbService.SERVICE_CONNECTED = false;

                // btHandlerTmp 제거됨 (미사용 Handler)
//                if (server != null)
//                    server.closeAllConnections();
//                server.stop();

                // [수정] 블루투스 관련 중복 정리 제거
                // btSearchOnOff와 clearBluetoothReconnect는 이미 위에서 처리됨 (라인 5195, 5197)
                // btSocket과 btConnectedThread도 이미 위에서 정리됨 (라인 5197-5240 부근)
                // btSearchOnOff = false;
                // clearBluetoothReconnect();
                // if (btSocket != null && btSocket.isConnected()) {
                //     try {
                //         btSocket.close();
                //     } catch (IOException e) {
                //         logError(LogManager.LogCategory.ER, Constants.ErrorMessages.ERROR_CLOSING_SOCKET, e);
                //     }
                // }
                // btSocket = null;
                // deviceSelected는 이미 위에서 null로 설정됨 (라인 5243)
                // deviceSelected = null;
                cancelDiscoverySafe();
                System.out.println("finishApplication: 재탐색 및 재연결 시도 시작 >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 9");

                try {
                    Thread.sleep(Constants.Timeouts.THREAD_SLEEP_MS);
                } catch (Exception e) {
                    logError(LogManager.LogCategory.ER, Constants.ErrorMessages.THREAD_SLEEP_ERROR, e);
                }

                // WeakReference 정리 (메모리 누수 방지)
                clearMainActivityReference();
                System.out.println("finishApplication: 재탐색 및 재연결 시도 시작 >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 10");

                Intent intent = getIntent();

                // ⚠️ 중요: 재시작 전에 Intent Extra 값들을 로그로 출력
                String intentModelId = intent.getStringExtra(Constants.IntentExtras.TEST_MODEL_ID);
                String intentModelName = intent.getStringExtra(Constants.IntentExtras.TEST_MODEL_NAME);
                String intentModelNation = intent.getStringExtra(Constants.IntentExtras.TEST_MODEL_NATION);
                String intentTestStartDatetime = intent.getStringExtra(Constants.IntentExtras.TEST_START_DATETIME);

//                logInfo(LogManager.LogCategory.PS, "Intent Extra - TEST_MODEL_ID: " + (intentModelId != null ? intentModelId : "null") +
//                        ", TEST_MODEL_NAME: " + (intentModelName != null ? intentModelName : "null") +
//                        ", TEST_MODEL_NATION: " + (intentModelNation != null ? intentModelNation : "null") +
//                        ", TEST_START_DATETIME: " + (intentTestStartDatetime != null ? intentTestStartDatetime : "null"));

                // ⚠️ 중요: 현재 static 변수 값들을 로그로 출력
                logInfo(LogManager.LogCategory.PS, "Before save to SharedPreferences - globalModelId: " +
                        (globalModelId != null ? globalModelId : "null") +
                        ", globalModelName: " + (globalModelName != null ? globalModelName : "null") +
                        ", globalModelNation: " + (globalModelNation != null ? globalModelNation : "null"));

                // ⚠️ 중요: SharedPreferences에 static 변수 값들 저장
                SharedPreferences prefs = getSharedPreferences(Constants.SharedPrefKeys.TEST_COOKIE_INFO, MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();

                // static 변수가 null이 아니고 비어있지 않으면 저장
                if (globalModelId != null && !globalModelId.isEmpty()) {
                    editor.putString(Constants.IntentExtras.TEST_MODEL_ID, globalModelId);
                }
                if (globalModelName != null && !globalModelName.isEmpty()) {
                    editor.putString(Constants.IntentExtras.TEST_MODEL_NAME, globalModelName);
                }
                if (globalModelNation != null && !globalModelNation.isEmpty()) {
                    editor.putString(Constants.IntentExtras.TEST_MODEL_NATION, globalModelNation);
                }
                editor.commit();
                System.out.println("finishApplication: 재탐색 및 재연결 시도 시작 >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 11");

                // ⚠️ 중요: SharedPreferences에 저장된 값들을 읽어서 로그로 출력
                String savedModelId = prefs.getString(Constants.IntentExtras.TEST_MODEL_ID, null);
                String savedModelName = prefs.getString(Constants.IntentExtras.TEST_MODEL_NAME, null);
                String savedModelNation = prefs.getString(Constants.IntentExtras.TEST_MODEL_NATION, null);

                logInfo(LogManager.LogCategory.PS, "After save to SharedPreferences - TEST_MODEL_ID: " +
                        (savedModelId != null ? savedModelId : "null") +
                        ", TEST_MODEL_NAME: " + (savedModelName != null ? savedModelName : "null") +
                        ", TEST_MODEL_NATION: " + (savedModelNation != null ? savedModelNation : "null"));

                // ⚠️ 중요: Intent에 Extra로 값들 재설정 (재시작 시 전달되도록)
                if (globalModelId != null && !globalModelId.isEmpty()) {
                    intent.putExtra(Constants.IntentExtras.TEST_MODEL_ID, globalModelId);
                }
                if (globalModelName != null && !globalModelName.isEmpty()) {
                    intent.putExtra(Constants.IntentExtras.TEST_MODEL_NAME, globalModelName);
                }
                if (globalModelNation != null && !globalModelNation.isEmpty()) {
                    intent.putExtra(Constants.IntentExtras.TEST_MODEL_NATION, globalModelNation);
                }
                if (intentTestStartDatetime != null && !intentTestStartDatetime.isEmpty()) {
                    intent.putExtra(Constants.IntentExtras.TEST_START_DATETIME, intentTestStartDatetime);
                }
                System.out.println("finishApplication: 재탐색 및 재연결 시도 시작 >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 12");

                // ⚠️ 중요: Intent Extra 재설정 후 값들을 로그로 출력
                String finalIntentModelId = intent.getStringExtra(Constants.IntentExtras.TEST_MODEL_ID);
                String finalIntentModelName = intent.getStringExtra(Constants.IntentExtras.TEST_MODEL_NAME);
                String finalIntentModelNation = intent.getStringExtra(Constants.IntentExtras.TEST_MODEL_NATION);
                String finalIntentTestStartDatetime = intent.getStringExtra(Constants.IntentExtras.TEST_START_DATETIME);

                logInfo(LogManager.LogCategory.PS, "Intent Extra after putExtra - TEST_MODEL_ID: " +
                        (finalIntentModelId != null ? finalIntentModelId : "null") +
                        ", TEST_MODEL_NAME: " + (finalIntentModelName != null ? finalIntentModelName : "null") +
                        ", TEST_MODEL_NATION: " + (finalIntentModelNation != null ? finalIntentModelNation : "null") +
                        ", TEST_START_DATETIME: " + (finalIntentTestStartDatetime != null ? finalIntentTestStartDatetime : "null"));

                // 메인 스레드에서 finish()와 startActivity() 호출
                final Intent finalIntent = intent;
                runOnUiThread(() -> {
                    try {
                        finish();
                        // finishedCorrectly 플래그와 관계없이 항상 재시작
                        // startActivity(finalIntent);
                        logInfo(LogManager.LogCategory.PS, "Activity restarted successfully");
                        finish();
                    } catch (Exception e) {
                        logError(LogManager.LogCategory.ER, "Failed to restart Activity", e);
                    }
                });
                System.out.println("finishApplication: 재탐색 및 재연결 시도 시작 >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 13");
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, Constants.ErrorMessages.APPLICATION_RESTART_FINISH_ERROR, e);
            }
        }).start();
    }

    public void setDisplayLightValueChange(float value) {
        try {
            params = getWindow().getAttributes();
            params.screenBrightness = value;
            getWindow().setAttributes(params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ⚠️ MEMORY LEAK FIX: Changed to static inner class to prevent implicit Activity reference
    static class RequestThreadAsync extends AsyncTask<String, Void, String> {
        private final WeakReference<ActivityModelTestProcess> activityRef;
        private final String url;

        public RequestThreadAsync(ActivityModelTestProcess activity) {
            this.activityRef = new WeakReference<>(activity);
            this.url = activity.urlStr;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                new Thread(() -> {
                    // 동시성 문제 해결: connection을 로컬 변수로 변경 (각 스레드가 자신만의 connection 사용)
                    HttpURLConnection connection = null;
                    try {
                        URL obj = new URL(url);
                        connection = (HttpURLConnection) obj.openConnection();

                        connection.setReadTimeout(Constants.Timeouts.HTTP_READ_TIMEOUT_LONG_MS);
                        connection.setConnectTimeout(Constants.Timeouts.HTTP_CONNECT_TIMEOUT_LONG_MS);
                        connection.setRequestMethod(Constants.HTTP.METHOD_POST);  // POST 방식으로 요청
                        connection.setDoInput(true);  // InputStream 으로 서버로부터 응답을 받겠다는 옵션
                        connection.setDoOutput(true); // OutputStream 으로 POST 데이터를 넘겨 주겠다는 옵션
                        connection.setRequestProperty(Constants.HTTP.HEADER_CONTENT_TYPE, Constants.HTTP.CONTENT_TYPE_JSON);    // application/json 형식으로 전송. (Request body 전달시 application/json 으로 서버에 전달)
                        connection.setRequestProperty(Constants.HTTP.HEADER_CACHE_CONTROL, Constants.HTTP.CACHE_CONTROL_NO_CACHE);

                        try {
                            int retCode = connection.getResponseCode();
                            ActivityModelTestProcess activity = activityRef.get();
                            if (activity != null) {
                                activity.logInfo(LogManager.LogCategory.PS, Constants.LogMessages.HTTP_RESPONSE_CODE + retCode);
                            }
                            if (retCode == HttpURLConnection.HTTP_OK) {
                                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                                // BufferedReader() : 엔터만 경계로 인식하고 받은 데이터를 String 으로 고정, Scanner 에 비해 빠름!
                                // InputStreamReader() : 지정된 문자 집합 내의 문자로 인코딩
                                // getInputStream() : url 에서 데이터를 읽어옴
                                try {
                                    String line = null;
                                    String lineTmp = null;
                                    StringBuilder sb = new StringBuilder();

                                    // 무한 루프 방지를 위해 최대 읽기 횟수 제한
                                    int maxReadCount = Constants.Timeouts.MAX_READ_COUNT;
                                    int readCount = 0;

                                    while (readCount < maxReadCount) {
                                        lineTmp = reader.readLine();
                                        if (lineTmp == null) {
                                            break; // 스트림 끝에 도달
                                        }
                                        readCount++;

                                        if (!lineTmp.trim().equals("")) {
                                            sb.append(lineTmp);
                                            line = lineTmp;
                                        }
                                    }

                                    // 모든 데이터를 읽은 후 한 번만 처리
                                    final String data = (line != null && !line.trim().equals("")) ? line :
                                            (sb.length() > 0 ? sb.toString() : null);

                                    if (data != null && !data.trim().equals("")) {
                                        final String finalData = data;

                                        // JSON 파싱을 백그라운드에서 직접 실행하여 메인 스레드 부하 최소화
                                        // 디바운싱을 위해 지연 실행
                                        ActivityModelTestProcess act = activityRef.get();
                                        if (act != null) {
                                            synchronized (HTTP_HANDLER_LOCK) {
                                                // 이전 작업이 아직 실행되지 않았다면 취소
                                                if (act.pendingHttpTask != null) {
                                                    act.schedulerHandler.removeCallbacks(act.pendingHttpTask);
                                                }

                                                // 새 작업을 백그라운드에서 지연 실행 (디바운싱)
                                                act.pendingHttpTask = new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        ActivityModelTestProcess a = activityRef.get();
                                                        if (a == null) return; // Activity destroyed

                                                        // 백그라운드 스레드에서 직접 jsonParsing 호출
                                                        // jsonParsing 내부에서 이미 Thread를 생성하지만,
                                                        // 메인 스레드를 거치지 않아 부하가 줄어듭니다
                                                        try {
                                                            a.logDebug(LogManager.LogCategory.PS, Constants.LogMessages.PROCESSING_HTTP_RESPONSE_DATA);

                                                            new Thread(() -> {
                                                                try {
                                                                    ActivityModelTestProcess a2 = activityRef.get();
                                                                    if (a2 != null) {
                                                                        a2.logDebug(LogManager.LogCategory.PS, Constants.LogMessages.DELETING_TEST_SPEC_DATA);
                                                                        TestData.deleteTestSpecData(a2.getApplicationContext());
                                                                        a2.logDebug(LogManager.LogCategory.PS, Constants.LogMessages.TEST_SPEC_DATA_DELETED);
                                                                    }
                                                                } catch (Exception e) {
                                                                    Thread.currentThread().interrupt();
                                                                }
                                                            }).start();

                                                            a.jsonParsing("test_spec", finalData);
                                                        } catch (Exception e) {
                                                            a.logError(LogManager.LogCategory.ER, Constants.ErrorMessages.JSON_PARSING_ERROR, e);
                                                        } finally {
                                                            synchronized (HTTP_HANDLER_LOCK) {
                                                                ActivityModelTestProcess a3 = activityRef.get();
                                                                if (a3 != null) {
                                                                    a3.pendingHttpTask = null;
                                                                }
                                                            }
                                                        }
                                                    }
                                                };

                                                // ⚠️ ANR FIX: Use Handler instead of creating Thread + Thread.sleep
                                                // 디바운스 지연 후 백그라운드에서 실행 (여러 요청이 동시에 와도 마지막 것만 처리)
                                                // OLD (WASTEFUL): new Thread(() -> { Thread.sleep(); pendingHttpTask.run(); }).start();
                                                // NEW (EFFICIENT): Use existing schedulerHandler
                                                act.schedulerHandler.postDelayed(() -> {
                                                    ActivityModelTestProcess a = activityRef.get();
                                                    if (a != null) {
                                                        // Run on background thread to avoid blocking main thread
                                                        a.btWorkerExecutor.execute(a.pendingHttpTask);
                                                    }
                                                }, HTTP_DEBOUNCE_DELAY_MS);
                                            }
                                        }
                                    }
                                } finally {
                                    try {
                                        reader.close();
                                    } catch (IOException e) {
                                        ActivityModelTestProcess act = activityRef.get();
                                        if (act != null) {
                                            act.logError(LogManager.LogCategory.ER, Constants.ErrorMessages.ERROR_CLOSING_READER, e);
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            ActivityModelTestProcess act = activityRef.get();
                            if (act != null) {
                                act.logError(LogManager.LogCategory.ER, Constants.ErrorMessages.HTTP_CONNECTION_ERROR, e);
                            }
                        } finally {
                            // 리소스 정리 보장 (finally 블록에서 한 번만 disconnect)
                            ActivityModelTestProcess act = activityRef.get();
                            if (act != null) {
                                act.safeDisconnectConnection(connection);
                            } else if (connection != null) {
                                try {
                                    connection.disconnect();
                                } catch (Exception ignored) {}
                            }
                        }
                    } catch (Exception e) {
                        ActivityModelTestProcess act = activityRef.get();
                        if (act != null) {
                            act.logError(LogManager.LogCategory.ER, Constants.ErrorMessages.HTTP_REQUEST_ERROR, e);
                        }
                        e.printStackTrace();
                    }
                }).start();
            } catch (Exception e) {
                ActivityModelTestProcess act = activityRef.get();
                if (act != null) {
                    act.logError(LogManager.LogCategory.ER, Constants.ErrorMessages.REQUEST_THREAD_ASYNC_ERROR, e);
                }
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
//            // doInBackground() 이후에 수행될 작업
//            // String s 파라미터는 doInBackground() 의 리턴값이다.
//            TextView tv = findViewById(R.id.getText);
//            tv.setText(s);
        }
    }

    // ⚠️ MEMORY LEAK FIX: Changed to static inner class to prevent implicit Activity reference
    static class RequestTestTaskThreadAsync extends AsyncTask<String, Void, String> {
        private final WeakReference<ActivityModelTestProcess> activityRef;
        private final String url;

        public RequestTestTaskThreadAsync(ActivityModelTestProcess activity) {
            this.activityRef = new WeakReference<>(activity);
            this.url = activity.urlTestTaskStr;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                new Thread(() -> {
                    // 동시성 문제 해결: connection을 로컬 변수로 변경 (각 스레드가 자신만의 connection 사용)
                    HttpURLConnection connection = null;
                    try {
                        URL obj = new URL(url);
                        connection = (HttpURLConnection) obj.openConnection();
                        // Log.i(TAG, "▶ [PS] RequestTestTaskThreadAsync.urlTestTaskStr " + urlTestTaskStr);

                        connection.setReadTimeout(Constants.Timeouts.HTTP_READ_TIMEOUT_LONG_MS);
                        connection.setConnectTimeout(Constants.Timeouts.HTTP_CONNECT_TIMEOUT_LONG_MS);
                        connection.setRequestMethod(Constants.HTTP.METHOD_GET);  // POST 방식으로 요청
                        connection.setDoInput(true);  // InputStream 으로 서버로부터 응답을 받겠다는 옵션
                        connection.setDoOutput(true); // OutputStream 으로 POST 데이터를 넘겨 주겠다는 옵션
                        connection.setRequestProperty(Constants.HTTP.HEADER_CONTENT_TYPE, Constants.HTTP.CONTENT_TYPE_JSON);    // application/json 형식으로 전송. (Request body 전달시 application/json 으로 서버에 전달)
                        connection.setRequestProperty(Constants.HTTP.HEADER_CACHE_CONTROL, Constants.HTTP.CACHE_CONTROL_NO_CACHE);

                        try {
                            int retCode = connection.getResponseCode();
                            // Log.i(TAG, "▶ [PS] test task response " + retCode);
                            if (retCode == HttpURLConnection.HTTP_OK) {
                                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                                try {
                                    String line = null;
                                    String lineTmp = null;
                                    StringBuilder sb = new StringBuilder();

                                    // 무한 루프 방지를 위해 최대 읽기 횟수 제한
                                    int readCount = 0;

                                    while (readCount < Constants.Timeouts.MAX_READ_COUNT) {
                                        lineTmp = reader.readLine();
                                        if (lineTmp == null) {
                                            break; // 스트림 끝에 도달
                                        }
                                        readCount++;

                                        if (!lineTmp.trim().equals("")) {
                                            sb.append(lineTmp);
                                            line = lineTmp;
                                        }
                                    }

                                    // 모든 데이터를 읽은 후 한 번만 처리
                                    final String data = (line != null && !line.trim().equals("")) ? line :
                                            (sb.length() > 0 ? sb.toString() : null);

                                    if (data != null && !data.trim().equals("")) {
                                        ActivityModelTestProcess activity = activityRef.get();
                                        if (activity != null) {
                                            activity.schedulerHandler.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    ActivityModelTestProcess act = activityRef.get();
                                                    if (act == null) return; // Activity destroyed
                                                    try {
                                                        act.logDebug(LogManager.LogCategory.PS, Constants.LogMessages.TEST_TASK_RESPONSE_DATA_RECEIVED);
                                                        // jsonParsing("test_spec", finalData);
                                                    } catch (Exception e) {
                                                        act.logError(LogManager.LogCategory.ER, Constants.ErrorMessages.ERROR_PROCESSING_TEST_TASK_DATA, e);
                                                    }
                                                }
                                            });
                                        }
                                    }
                                } finally {
                                    try {
                                        reader.close();
                                    } catch (IOException e) {
                                        ActivityModelTestProcess act = activityRef.get();
                                        if (act != null) {
                                            act.logError(LogManager.LogCategory.ER, Constants.ErrorMessages.ERROR_CLOSING_READER, e);
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            ActivityModelTestProcess act = activityRef.get();
                            if (act != null) {
                                act.logError(LogManager.LogCategory.ER, Constants.ErrorMessages.REQUEST_TEST_TASK_THREAD_ASYNC_CONNECTION_ERROR, e);
                            }
                        } finally {
                            // 리소스 정리 보장 (finally 블록에서 한 번만 disconnect)
                            ActivityModelTestProcess act = activityRef.get();
                            if (act != null) {
                                act.safeDisconnectConnection(connection);
                            } else if (connection != null) {
                                try {
                                    connection.disconnect();
                                } catch (Exception ignored) {}
                            }
                        }
                    } catch (Exception e) {
                        ActivityModelTestProcess act = activityRef.get();
                        if (act != null) {
                            act.logError(LogManager.LogCategory.ER, Constants.ErrorMessages.REQUEST_TEST_TASK_THREAD_ASYNC_REQUEST_ERROR, e);
                        }
                        e.printStackTrace();
                    }
                }).start();
            } catch (Exception e) {
                ActivityModelTestProcess act = activityRef.get();
                if (act != null) {
                    act.logError(LogManager.LogCategory.ER, Constants.ErrorMessages.REQUEST_TEST_TASK_THREAD_ASYNC_ERROR, e);
                }
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
        }
    }

    /**
     * VersionInfoList.jsp 호출하여 네트워크 상태 체크 및 버전 비교
     * ⚠️ MEMORY LEAK FIX: Changed to static inner class to prevent implicit Activity reference
     */
    static class RequestVersionInfoThreadAsync extends AsyncTask<String, Void, String> {
        private final WeakReference<ActivityModelTestProcess> activityRef;
        private final String localVersionId;
        private final String localModelId;
        private final String serverIp;
        private final int unitNo;

        public RequestVersionInfoThreadAsync(ActivityModelTestProcess activity, String localVersionId, String localModelId) {
            this.activityRef = new WeakReference<>(activity);
            this.localVersionId = localVersionId;
            this.localModelId = localModelId;
            this.serverIp = activity.serverIp;
            this.unitNo = activity.unit_no;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                new Thread(() -> {
                    HttpURLConnection connection = null;
                    try {
                        String url = Constants.URLs.HTTP_PROTOCOL + serverIp + Constants.URLs.ENDPOINT_VERSION_INFO_LIST;
                        if (globalModelId != null && !globalModelId.isEmpty()) {
                            url += Constants.Common.QUESTION + Constants.URLs.PARAM_MODEL_ID + globalModelId;
                        }
                        // logInfo(LogManager.LogCategory.PS, "RequestVersionInfoThreadAsync >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> url." + url);
                        URL obj = new URL(url);
                        connection = (HttpURLConnection) obj.openConnection();

                        connection.setReadTimeout(Constants.Timeouts.HTTP_READ_TIMEOUT_LONG_MS);
                        connection.setConnectTimeout(Constants.Timeouts.HTTP_CONNECT_TIMEOUT_LONG_MS);
                        connection.setRequestMethod(Constants.HTTP.METHOD_POST);
                        connection.setDoInput(true);
                        connection.setDoOutput(true);
                        connection.setRequestProperty(Constants.HTTP.HEADER_CONTENT_TYPE, Constants.HTTP.CONTENT_TYPE_JSON);
                        connection.setRequestProperty(Constants.HTTP.HEADER_CACHE_CONTROL, Constants.HTTP.CACHE_CONTROL_NO_CACHE);

                        try {
                            int retCode = connection.getResponseCode();
                            // logInfo(LogManager.LogCategory.SI, "VersionInfoList response code: " + retCode);

                            if (retCode == HttpURLConnection.HTTP_OK) {
                                // 네트워크 상태가 정상이면 tvRunWsRamp를 파란색으로 변경
                                ActivityModelTestProcess activity = activityRef.get();
                                if (activity != null) {
                                    activity.scheduleUiUpdate(() -> {
                                        ActivityModelTestProcess act = activityRef.get();
                                        if (act != null && act.tvRunWsRamp != null) {
                                            act.tvRunWsRamp.setText(String.valueOf(unitNo));
                                            act.tvRunWsRamp.setBackgroundColor(act.getBaseContext().getResources().getColor(R.color.blue_01));
                                        }
                                    });
                                }

                                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                                try {
                                    String line = null;
                                    String lineTmp = null;
                                    StringBuilder sb = new StringBuilder();
                                    int readCount = 0;

                                    while (readCount < Constants.Timeouts.MAX_READ_COUNT) {
                                        lineTmp = reader.readLine();
                                        if (lineTmp == null) {
                                            break;
                                        }
                                        readCount++;
                                        if (!lineTmp.trim().equals("")) {
                                            sb.append(lineTmp);
                                            line = lineTmp;
                                        }
                                    }

                                    final String data = (line != null && !line.trim().equals("")) ? line :
                                            (sb.length() > 0 ? sb.toString() : null);

                                    if (data != null && !data.trim().equals("")) {
                                        // 버전 정보 파싱 및 비교
                                        try {
                                            JSONObject jsonObject = new JSONObject(data);
                                            String serverVersionId = jsonObject.optString(Constants.JsonKeys.CLM_TEST_VERSION_ID, "");
                                            String serverModelId = jsonObject.optString(Constants.JsonKeys.CLM_MODEL_ID, "");

                                            ActivityModelTestProcess act = activityRef.get();
                                            if (act != null) {
                                                // logInfo(LogManager.LogCategory.SI, "Version info received: " + data);
                                                act.logInfo(LogManager.LogCategory.SI, String.format("Version comparison - Local: [%s, %s], Server: [%s, %s]",
                                                        localVersionId, localModelId, serverVersionId, serverModelId));

                                                // 버전이 틀리면 TestInfoList.jsp 호출하여 DB 갱신
                                                boolean versionMismatch = false;
                                                if (localVersionId != null && !localVersionId.isEmpty() &&
                                                        serverVersionId != null && !serverVersionId.isEmpty() &&
                                                        !localVersionId.equals(serverVersionId)) {
                                                    versionMismatch = true;
                                                    act.logWarn(LogManager.LogCategory.SI, "Test version ID mismatch detected. Updating test spec data.");
                                                } else if (localVersionId != null && !localVersionId.isEmpty() &&
                                                        serverVersionId != null && !serverVersionId.isEmpty() &&
                                                        !localVersionId.equals(serverVersionId)) {
                                                    versionMismatch = true;
                                                    act.logWarn(LogManager.LogCategory.SI, "Model version mismatch detected. Updating test spec data.");
                                                }

                                                if (versionMismatch) {
                                                    // TestInfoList.jsp 호출하여 DB 갱신
                                                    act.clearHttpHandlerQueue();
                                                    ActivityModelTestProcess.RequestThreadAsync thread = new ActivityModelTestProcess.RequestThreadAsync(act);
                                                    thread.execute();
                                                    // logInfo(LogManager.LogCategory.SI, "Test spec data update requested due to version mismatch.");
                                                } else {
                                                    act.logInfo(LogManager.LogCategory.SI, "Version matches. No update needed.");
                                                }
                                            }
                                        } catch (JSONException e) {
                                            ActivityModelTestProcess act = activityRef.get();
                                            if (act != null) {
                                                act.logError(LogManager.LogCategory.ER, "Error parsing version info JSON", e);
                                            }
                                        }
                                    }
                                } finally {
                                    try {
                                        reader.close();
                                    } catch (IOException e) {
                                        ActivityModelTestProcess act = activityRef.get();
                                        if (act != null) {
                                            act.logError(LogManager.LogCategory.ER, Constants.ErrorMessages.ERROR_CLOSING_READER, e);
                                        }
                                    }
                                }
                            } else {
                                // 네트워크 상태가 비정상이면 tvRunWsRamp를 빨간색으로 유지
                                ActivityModelTestProcess act = activityRef.get();
                                if (act != null) {
                                    act.logWarn(LogManager.LogCategory.SI, "VersionInfoList request failed with code: " + retCode);
                                    act.scheduleUiUpdate(() -> {
                                        ActivityModelTestProcess a = activityRef.get();
                                        if (a != null && a.tvRunWsRamp != null) {
                                            // tvRunWsRamp.setText(Constants.Common.EMPTY_STRING);
                                            a.tvRunWsRamp.setBackgroundColor(a.getBaseContext().getResources().getColor(R.color.red_01));
                                        }
                                    });
                                }
                            }
                        } catch (Exception e) {
                            ActivityModelTestProcess act = activityRef.get();
                            if (act != null) {
                                act.logError(LogManager.LogCategory.ER, "VersionInfoList connection error", e);
                                // 네트워크 오류 시 tvRunWsRamp를 빨간색으로 유지
                                act.scheduleUiUpdate(() -> {
                                    ActivityModelTestProcess a = activityRef.get();
                                    if (a != null && a.tvRunWsRamp != null) {
                                        // tvRunWsRamp.setText(Constants.Common.EMPTY_STRING);
                                        a.tvRunWsRamp.setBackgroundColor(a.getBaseContext().getResources().getColor(R.color.red_01));
                                    }
                                });
                            }
                        } finally {
                            ActivityModelTestProcess act = activityRef.get();
                            if (act != null) {
                                act.safeDisconnectConnection(connection);
                            } else if (connection != null) {
                                try {
                                    connection.disconnect();
                                } catch (Exception ignored) {}
                            }
                        }
                    } catch (Exception e) {
                        ActivityModelTestProcess act = activityRef.get();
                        if (act != null) {
                            act.logError(LogManager.LogCategory.ER, "VersionInfoList request error", e);
                            // 네트워크 오류 시 tvRunWsRamp를 빨간색으로 유지
                            act.scheduleUiUpdate(() -> {
                                ActivityModelTestProcess a = activityRef.get();
                                if (a != null && a.tvRunWsRamp != null) {
                                    // tvRunWsRamp.setText(Constants.Common.EMPTY_STRING);
                                    a.tvRunWsRamp.setBackgroundColor(a.getBaseContext().getResources().getColor(R.color.red_01));
                                }
                            });
                        }
                    }
                }).start();
            } catch (Exception e) {
                ActivityModelTestProcess act = activityRef.get();
                if (act != null) {
                    act.logError(LogManager.LogCategory.ER, "RequestVersionInfoThreadAsync error", e);
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
        }
    }

    private void jsonParsing(String data_type, String json) {
        if (!Constants.JsonKeys.TEST_SPEC.equals(data_type) || json == null) {
            return;
        }
        runOnBtWorker(() -> processTestSpecJson(json));
    }

    private void processTestSpecJson(String json) {
        try {
            String specSignature = computeSha256(json);
            if (specSignature != null && specSignature.equals(lastSpecSignature)) {
                // logInfo(LogManager.LogCategory.SI, "Test spec unchanged; skipping reload");
                return;
            }

            JSONObject jsonObject = new JSONObject(json);
            JSONArray testItemArray = jsonObject.getJSONArray(Constants.JsonKeys.TEST_SPEC);

            ActivityModelTestProcess activity = getMainActivity();
            if (activity != null) {
                activity.lstData = new ArrayList<>();
            }
            List<Map<String, String>> lstData = (activity != null) ? activity.lstData : new ArrayList<>();

            if (testItemArray.length() == 0) {
                logWarn(LogManager.LogCategory.SI, "Test spec array is empty");
                return;
            }

            JSONObject testItemObject = testItemArray.getJSONObject(0);
            arrTestModels[0][0] = testItemObject.getString(Constants.JsonKeys.CLM_MODEL_ID);
            arrTestModels[0][1] = testItemObject.getString(Constants.JsonKeys.CLM_MODEL_NAME);
            arrTestModels[0][2] = testItemObject.getString(Constants.JsonKeys.CLM_MODEL_NATION);
            arrTestModels[0][3] = testItemObject.getString(Constants.JsonKeys.CLM_TEST_VERSION_ID);
            arrTestModels[0][4] = testItemObject.getString(Constants.JsonKeys.CLM_MODEL_VERSION);

            // 첫 번째 객체에서 버전 정보와 모델 ID 추출 (모든 항목에 동일하게 적용)
            String testVersionId = testItemArray.getJSONObject(0).optString(Constants.JsonKeys.CLM_TEST_VERSION_ID, "");
            String modelId = testItemArray.getJSONObject(0).optString(Constants.JsonKeys.CLM_MODEL_ID, "");

            for (int i = 0; i < testItemArray.length() - 1; i++) {
                testItemObject = testItemArray.getJSONObject(i);
                Map<String, String> mapData = new HashMap<>();
                mapData.put(Constants.JsonKeys.CLM_TEST_SEQ, String.valueOf(i));
                mapData.put(Constants.JsonKeys.CLM_LOWER_VALUE, testItemObject.getString(Constants.JsonKeys.CLM_LOWER_VALUE));
                mapData.put(Constants.JsonKeys.CLM_UPPER_VALUE, testItemObject.getString(Constants.JsonKeys.CLM_UPPER_VALUE));
                mapData.put(Constants.JsonKeys.CLM_LOWER_VALUE_WATT, testItemObject.getString(Constants.JsonKeys.CLM_LOWER_VALUE_WATT));
                mapData.put(Constants.JsonKeys.CLM_UPPER_VALUE_WATT, testItemObject.getString(Constants.JsonKeys.CLM_UPPER_VALUE_WATT));
                mapData.put(Constants.JsonKeys.CLM_TEST_STEP, testItemObject.getString(Constants.JsonKeys.CLM_TEST_STEP));
                mapData.put(Constants.JsonKeys.CLM_TEST_NAME, testItemObject.getString(Constants.JsonKeys.CLM_TEST_NAME));
                mapData.put(Constants.JsonKeys.CLM_TEST_ID, testItemObject.getString(Constants.JsonKeys.CLM_TEST_ID));
                mapData.put(Constants.JsonKeys.CLM_RESPONSE_VALUE, testItemObject.getString(Constants.JsonKeys.CLM_RESPONSE_VALUE));
                mapData.put(Constants.JsonKeys.CLM_TEST_SEC, testItemObject.getString(Constants.JsonKeys.CLM_TEST_SEC));
                mapData.put(Constants.JsonKeys.CLM_TEST_TYPE, testItemObject.getString(Constants.JsonKeys.CLM_TEST_TYPE));
                mapData.put(Constants.JsonKeys.CLM_TEST_COMMAND, testItemObject.getString(Constants.JsonKeys.CLM_TEST_COMMAND));
                mapData.put(Constants.JsonKeys.CLM_VALUE_WATT, testItemObject.getString(Constants.JsonKeys.CLM_VALUE_WATT));
                mapData.put(Constants.JsonKeys.CLM_VALUE, testItemObject.getString(Constants.JsonKeys.CLM_VALUE));
                mapData.put(Constants.JsonKeys.CLM_PRODUCT_SERIAL_NO, testItemObject.getString(Constants.JsonKeys.CLM_PRODUCT_SERIAL_NO));
                mapData.put(Constants.JsonKeys.CLM_COMMENT, "");
                // clm_test_version_id와 clm_model_id 추가
                mapData.put(Constants.JsonKeys.CLM_TEST_VERSION_ID, testVersionId);
                mapData.put(Constants.JsonKeys.CLM_MODEL_ID, modelId);
                lstData.add(mapData);
            }

            // lstDataTmp = lstData;
            // System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>> lstDataTmp.size() " + lstDataTmp.size());

            applyTestSpecData(lstData, true);

            if (specSignature != null) {
                lastSpecSignature = specSignature;
            }
        } catch (JSONException e) {
            logError(LogManager.LogCategory.ER, Constants.ErrorMessages.JSON_PARSING_ERROR, e);
        }
    }

    public void bluetoothSearch() {
        // 블루투스 통신 시작 전에 이전 메시지 큐 정리하여 메인 스레드 과부하 방지
        clearBtHandlerQueue();
        if (ensureBtPermissions(PermissionAction.LIST_PAIRED_DEVICES)) {
            listPairedDevicesSelect();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MULTIPLE_PERMISSIONS:
            case PERMISSION_REQUEST_CODE:
            case PERMISSION_REQUEST_CODE_BT:
                permissionRequestInProgress = false;
                boolean allGranted = grantResults.length > 0;
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        allGranted = false;
                        break;
                    }
                }

                if (allGranted && hasAllBluetoothPermissions()) {
                    // 권한이 허용되었으면 거부 횟수 리셋
                    permissionDenialCount = 0;
                    btPermissionsGranted = true;
                    runPendingPermissionActions();
                } else {
                    // 권한이 거부되었으면 거부 횟수 증가
                    permissionDenialCount++;
                    btPermissionsGranted = false;
                    pendingPermissionActions.clear();
                    handlePermissionDenial(permissions, grantResults);
                }
                break;
        }
    }

    private void handlePermissionDenial(@NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean permanentlyDenied = false;
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[i])) {
                    permanentlyDenied = true;
                    break;
                }
            }
        }
        // showPermissionPrompt(permanentlyDenied);
    }

    private void getPreferences() {
        // 복잡한 UI 업데이트 블록 통합: scheduleUiUpdate 사용
        scheduleUiUpdate(() -> {
            tvSerialNo.setTextColor(postTestInfoColor);
            // tvSerialNo.setText(productSerialNo);
        });
    }

    /**
     * MainActivity 인스턴스를 안전하게 가져옵니다 (WeakReference 사용)
     * Activity가 이미 종료되었거나 GC된 경우 null을 반환합니다.
     *
     * @return ActivityModel_0002 인스턴스 또는 null
     */
    public static ActivityModelTestProcess getMainActivity() {
        if (mainActivityRef != null) {
            ActivityModelTestProcess activity = mainActivityRef.get();
            if (activity == null) {
                // Activity가 GC된 경우 참조 정리
                mainActivityRef = null;
            }
            return activity;
        }
        return null;
    }

    /**
     * Application Context를 안전하게 가져옵니다
     *
     * @return Application Context 또는 null
     */
    public static Context getMainActivityContext() {
        ActivityModelTestProcess activity = getMainActivity();
        return activity != null ? activity.getApplicationContext() : null;
    }

    /**
     * MainActivity 참조를 정리합니다 (메모리 누수 방지)
     */
    private static void clearMainActivityReference() {
        if (mainActivityRef != null) {
            mainActivityRef.clear();
            mainActivityRef = null;
        }
    }

    /**
     * HTTP Handler 메시지 큐 정리 - 메인 스레드 과부하 방지
     * ⚠️ 통합: httpHandler → schedulerHandler로 통합됨
     */
    private void clearHttpHandlerQueue() {
        synchronized (HTTP_HANDLER_LOCK) {
            if (schedulerHandler != null) {
                schedulerHandler.removeCallbacksAndMessages(null);
            }
            pendingHttpTask = null;
        }
    }

    /**
     * HttpURLConnection 리소스 안전하게 정리
     *
     * @param connection 정리할 HttpURLConnection (null 가능)
     */
    private static void safeDisconnectConnection(HttpURLConnection connection) {
        if (connection != null) {
            try {
                connection.disconnect();
            } catch (Exception e) {
                // static 메서드이므로 LogManager의 정적 메서드를 직접 사용
                LogManager.e(LogManager.LogCategory.ER, TAG, "Error disconnecting connection", e);
            }
        }
    }

    /**
     * 블루투스 Handler 메시지 큐 정리 - 메인 스레드 과부하 방지
     */
    private void clearBtHandlerQueue() {
        if (btHandler != null) {
            btHandler.removeCallbacksAndMessages(null);
        }
    }

    // ==================== 로그 관리 시스템 ====================
    // LogManager를 사용하여 로그 관리 (util 패키지의 LogManager 사용)

    /**
     * 편의 메서드들 - LogManager 래퍼
     */
    private void logInfo(LogManager.LogCategory category, String message) {
        // 문자열 리소스 키인지 확인 (log.으로 시작하는 경우)
        String logMessage = message.startsWith("log.")
                ? StringResourceManager.getInstance().getString(message)
                : message;
        logManager.info(category, TAG, logMessage);
    }

    private void logDebug(LogManager.LogCategory category, String message) {
        // 문자열 리소스 키인지 확인 (log.으로 시작하는 경우)
        String logMessage = message.startsWith("log.")
                ? StringResourceManager.getInstance().getString(message)
                : message;
        logManager.debug(category, TAG, logMessage);
    }

    private void logWarn(LogManager.LogCategory category, String message) {
        // 문자열 리소스 키인지 확인 (log.으로 시작하는 경우)
        String logMessage = message.startsWith("log.")
                ? StringResourceManager.getInstance().getString(message)
                : message;
        logManager.warn(category, TAG, logMessage);
    }

    private void logError(LogManager.LogCategory category, String message, Throwable throwable) {
        // 문자열 리소스 키인지 확인 (log.으로 시작하는 경우)
        String logMessage = message.startsWith("log.")
                ? StringResourceManager.getInstance().getString(message)
                : message;
        logManager.error(category, TAG, logMessage, throwable);
    }

    /**
     * 로그 배치 큐 정리
     */
    private void clearLogBatchQueue() {
        logManager.clearLogBatchQueue();
    }

    /**
     * 블루투스 검사 응답 신호 수신 로그 (공통 포맷)
     */
    private void logBtTestResponse(String command, String response, double electricValue,
                                   int wattLower, int wattUpper, String messageInfo) {
        String msg = String.format(Constants.LogMessages.TEST_ITEM_SIGNAL_RECEIVE + Constants.Common.EMPTY_STRING + ">> [T:%d/%d][C:%d/%s][P:%d/%d][S:%s][G:%d=%d-%d][R:%s]%s %d < %.0f < %d",
                testTotalCounter, totalTimeCnt, testItemCounter,
                (testItemIdx < arrTestItems.length && arrTestItems[testItemIdx] != null) ? arrTestItems[testItemIdx][2] : "0",
                testItemIdx, arrTestItems.length,
                currentTestItem, disconnectCheckCount, receivedMessageCnt, sendingMessageCnt,
                readMessage, (messageInfo != null ? "[I:" + messageInfo + "]" : ""),
                wattLower, electricValue, wattUpper);
        logInfo(LogManager.LogCategory.BT, msg);
    }

    /**
     * 블루투스 검사 응답 신호 수신 로그 (간단한 버전)
     */
    private void logBtTestResponseSimple(String command, String response, String result) {
        String deviceSec = "0";
        if (arrTestItems != null && testItemIdx < arrTestItems.length && arrTestItems[testItemIdx] != null) {
            deviceSec = arrTestItems[testItemIdx][2];
        }
        String msg = String.format("검사 응답 신호 수신 > [T:%d/%d][C:%d/%s][P:%d/%d][S:%s][G:%d=%d-%d][R:%s] %s / %s",
                testTotalCounter, totalTimeCnt, testItemCounter,
                deviceSec,
                testItemIdx, (arrTestItems != null) ? arrTestItems.length : 0,
                currentTestItem, disconnectCheckCount, receivedMessageCnt, sendingMessageCnt,
                command, response, result);
        logInfo(LogManager.LogCategory.BT, msg);
    }

    /**
     * 모든 Timer와 TimerTask를 안전하게 정리합니다
     * 메모리 누수 방지를 위해 Activity 종료 시 반드시 호출되어야 합니다
     * <p>
     * Phase 2: 각 Timer를 개별적으로 try-catch로 감싸서
     * 하나의 Timer 정리 실패가 다른 Timer 정리를 방해하지 않도록 함
     */
    private void cleanupAllTimers() {
        logInfo(LogManager.LogCategory.PS, "Starting timer cleanup...");

        // 0. 블루투스 연결 인디케이터 정리
        try {
            stopBtConnectionIndicator();
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, "Error stopping BT connection indicator", e);
        }

        // 1. USB 폴링 정리
        try {
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> stopUsbPolling 1");
            stopUsbPolling();
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, "Error stopping USB polling", e);
        }

        // 2. tmrBTMessageSend 정리
        try {
            stopBtMessageTimer();
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, "Error stopping BT message timer", e);
        }

        // 3. tmrFinishedRestart 정리 (Phase 3: synchronized 블록으로 동시성 제어)
        try {
            synchronized (finishedRestartTimerLock) {
                if (finishedRestartTimerRunning.compareAndSet(true, false)) {
                    if (tmrFinishedRestart != null) {
                        tmrFinishedRestart.cancel();
                        tmrFinishedRestart.purge();
                        tmrFinishedRestart = null;
                    }
                    if (ttFinishedRestart != null) {
                        ttFinishedRestart.cancel();
                        ttFinishedRestart = null;
                    }
                } else {
                    // 실행 중이 아니어도 정리 (안전을 위해)
                    if (tmrFinishedRestart != null) {
                        tmrFinishedRestart.cancel();
                        tmrFinishedRestart.purge();
                        tmrFinishedRestart = null;
                    }
                    if (ttFinishedRestart != null) {
                        ttFinishedRestart.cancel();
                        ttFinishedRestart = null;
                    }
                }
            }
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, "Error cleaning up finished restart timer", e);
        }

        // 4. tmrUnfinishedRestart 정리 (Phase 3: synchronized 블록으로 동시성 제어)
        try {
            synchronized (unfinishedRestartTimerLock) {
                if (unfinishedRestartTimerRunning.compareAndSet(true, false)) {
                    if (tmrUnfinishedRestart != null) {
                        tmrUnfinishedRestart.cancel();
                        tmrUnfinishedRestart.purge();
                        tmrUnfinishedRestart = null;
                    }
                    if (ttUnfinishedRestart != null) {
                        ttUnfinishedRestart.cancel();
                        ttUnfinishedRestart = null;
                    }
                } else {
                    // 실행 중이 아니어도 정리 (안전을 위해)
                    if (tmrUnfinishedRestart != null) {
                        tmrUnfinishedRestart.cancel();
                        tmrUnfinishedRestart.purge();
                        tmrUnfinishedRestart = null;
                    }
                    if (ttUnfinishedRestart != null) {
                        ttUnfinishedRestart.cancel();
                        ttUnfinishedRestart = null;
                    }
                }
            }
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, "Error cleaning up unfinished restart timer", e);
        }

        // 5. tmrRemoteCommand 정리 (미사용이지만 안전을 위해)
        try {
            if (tmrRemoteCommand != null) {
                tmrRemoteCommand.cancel();
                tmrRemoteCommand.purge();
                tmrRemoteCommand = null;
            }
            if (ttRemoteCommand != null) {
                ttRemoteCommand.cancel();
                ttRemoteCommand = null;
            }
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, "Error cleaning up remote command timer", e);
        }

        // 6. checkDuration Timer 정리 (Phase 1: static → instance로 변경됨)
        try {
            if (checkDuration != null) {
                checkDuration.cancel();
                checkDuration.purge();
                checkDuration = null;
            }
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, "Error cleaning up checkDuration timer", e);
        }

        // 7. barcodeRequestTimer 정리 (Phase 1: static → instance로 변경됨)
        try {
            if (barcodeRequestTimer != null) {
                barcodeRequestTimer.cancel();
                barcodeRequestTimer.purge();
                barcodeRequestTimer = null;
            }
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, "Error cleaning up barcode request timer", e);
        }

        // 8. appResetTimerTask 정리
        try {
            if (appResetTimerTask != null) {
                appResetTimerTask.cancel();
                appResetTimerTask = null;
            }
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, "Error cleaning up app reset timer task", e);
        }

        logInfo(LogManager.LogCategory.PS, "All timers cleaned up successfully");
    }

    private void runOnBtWorker(Runnable task) {
        if (task == null) {
            return;
        }
        // Executor가 종료되었는지 확인
        if (btWorkerExecutor == null || btWorkerExecutor.isShutdown() || btWorkerExecutor.isTerminated()) {
            logWarn(LogManager.LogCategory.BT, "btWorkerExecutor is shutdown, cannot execute task");
            return;
        }
        try {
            btWorkerExecutor.execute(task);
        } catch (java.util.concurrent.RejectedExecutionException e) {
            logWarn(LogManager.LogCategory.BT, "Task rejected from btWorkerExecutor: " + e.getMessage());
        }
    }

    private String[] getRequiredPermissions() {
        // Android 버전에 따라 블루투스 권한과 미디어/스토리지 권한을 결합
        String[] btPermissions = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ? MODERN_BT_PERMISSIONS
                : LEGACY_BT_PERMISSIONS;

        String[] mediaPermissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 (API 33) 이상: 미디어 권한
            mediaPermissions = MODERN_MEDIA_PERMISSIONS;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11-12 (API 30-32): READ_EXTERNAL_STORAGE만 요청
            // 중요: Android 11 이상에서는 WRITE_EXTERNAL_STORAGE 권한이 더 이상 효과가 없음
            // Scoped Storage 도입으로 인해 항상 거부(-1)를 반환하므로 요청하지 않음
            mediaPermissions = LEGACY_STORAGE_PERMISSIONS_API30_UP;
        } else {
            // Android 10 이하 (API 29 이하): READ + WRITE 스토리지 권한
            mediaPermissions = LEGACY_STORAGE_PERMISSIONS_API29_DOWN;
        }

        // 두 배열을 결합
        String[] allPermissions = new String[btPermissions.length + mediaPermissions.length];
        System.arraycopy(btPermissions, 0, allPermissions, 0, btPermissions.length);
        System.arraycopy(mediaPermissions, 0, allPermissions, btPermissions.length, mediaPermissions.length);

        return allPermissions;
    }

    private boolean hasBluetoothConnectPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            // 해당 권한이 존재하지 않으므로 항상 true로 간주
            return true;
        }
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasAllBluetoothPermissions() {
        for (String permission : getRequiredPermissions()) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private boolean hasBluetoothScanPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isBluetoothAdapterEnabled() {
        if (mBTAdapter == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasBluetoothConnectPermission()) {
            return false;
        }
        return mBTAdapter.isEnabled();
    }

    @SuppressLint("MissingPermission")
    private Set<BluetoothDevice> getBondedDevicesSafe() {
        if (mBTAdapter == null) {
            return Collections.emptySet();
        }
        if (!hasBluetoothConnectPermission()) {
            logWarn(LogManager.LogCategory.BT, "Missing BLUETOOTH_CONNECT permission; cannot read bonded devices");
            ensureBtPermissions(PermissionAction.LIST_PAIRED_DEVICES);
            return Collections.emptySet();
        }
        try {
            return mBTAdapter.getBondedDevices();
        } catch (SecurityException e) {
            logError(LogManager.LogCategory.ER, "Failed to obtain bonded devices", e);
            return Collections.emptySet();
        }
    }

    private void cancelDiscoverySafe() {
        if (mBTAdapter == null) {
            return;
        }
        if (!hasBluetoothScanPermission()) {
            logWarn(LogManager.LogCategory.BT, "Missing BLUETOOTH_SCAN permission; cannot cancel discovery");
            ensureBtPermissions(null);
            return;
        }
        try {
            mBTAdapter.cancelDiscovery();
        } catch (SecurityException e) {
            logError(LogManager.LogCategory.ER, "Failed to cancel discovery safely", e);
        }
    }

    private boolean ensureBtPermissions(PermissionAction actionOnGrant) {
        if (hasAllBluetoothPermissions()) {
            btPermissionsGranted = true;
            return true;
        }
        if (actionOnGrant != null) {
            pendingPermissionActions.add(actionOnGrant);
        }
        logWarn(LogManager.LogCategory.BT, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Permission B.requestRuntimePermissions getRequiredPermissions().length " + getRequiredPermissions().length);
        List<String> missingPermissions = new ArrayList<>();
        for (String permission : getRequiredPermissions()) {
            logWarn(LogManager.LogCategory.BT, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Permission B.requestRuntimePermissions permission " + permission + " : " + ActivityCompat.checkSelfPermission(this, permission));
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        requestRuntimePermissions();
        return false;
    }

    private void requestRuntimePermissions() {
        if (permissionRequestInProgress) {
            logInfo(LogManager.LogCategory.BT, "Permission request already in progress; skipping duplicate request");
            return;
        }
        List<String> missingPermissions = new ArrayList<>();
        for (String permission : getRequiredPermissions()) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        if (missingPermissions.isEmpty()) {
            permissionDenialCount = 0; // 권한이 이미 허용되었으면 거부 횟수 리셋
            btPermissionsGranted = true;
            runPendingPermissionActions();
            return;
        }
        permissionRequestInProgress = true;
        ActivityCompat.requestPermissions(this, missingPermissions.toArray(new String[0]), MULTIPLE_PERMISSIONS);
    }

    private void runPendingPermissionActions() {
        if (pendingPermissionActions.isEmpty()) {
            return;
        }
        EnumSet<PermissionAction> actions = EnumSet.copyOf(pendingPermissionActions);
        pendingPermissionActions.clear();
        if (actions.contains(PermissionAction.LIST_PAIRED_DEVICES)) {
            listPairedDevicesSelect();
        }
        if (actions.contains(PermissionAction.BT_RECONNECT)) {
            scheduleBluetoothReconnect(true);
        }
    }

    /**
     * USB 연결은 유지한 채 블루투스 세션만 초기화하여 다음 테스트를 준비합니다.
     */
    public void resetBluetoothSessionKeepUsb() {
//         if(usbReconnectAttempts>0) {
//             return;
//         }
        logInfo(LogManager.LogCategory.PS, "> resetBluetoothSessionKeepUsb");
        processFinished = false;
        entranceCheck = false;
        btSearchOnOff = false;
        btConnected = false;
        btConnectionInProgress = false;
        isConnected = false;
        btReconnectDelayMs = Constants.Timeouts.BT_LIST_TIMER_INTERVAL_MS;
        currentTestItem = ST0101;
        testItemIdx = 0;
        testItemCounter = 0;
        testTotalCounter = 0;
        receiveCommand = "";
        receiveCompAgingResponse = Constants.ResultStatus.COMP_AGING_RESPONSE_01;
        sensorNgCount = 0;
        blnBarcodeReceived = false;
        restartCntFinished = 0;
        restartCntUnfinished = 0;
        finishedCorrectly = false;
        sendResultYn = Constants.InitialValues.SEND_RESULT_YN;
        decTemperatureValueCompStart = "";
        decTemperatureValueCompEnd = "";
        temperatureValueCompDiff = "";
        testProcessId = Constants.InitialValues.TEST_PROCESS_ID;
        globalLastTestStartTimestamp = lmsTestSeq;
        shouldUpdateDialog = false;
        receiveCommandEmptyCnt = 0;
        receivedMessageCnt = 0;
        sendingMessageCnt = 0;

        lstMapWattTemp.clear();
        lstWatt.clear();

        cleanupAllTimers();
        stopBtMessageTimer();
        clearBluetoothReconnect();
        if (btConnectedThread != null) {
            try {
                btConnectedThread.cancel();
            } catch (Exception ignored) {
            }
            btConnectedThread = null;
        }
        closeSocketSilently(btSocket);
        btSocket = null;
        deviceSelected = null;

        // 복잡한 UI 업데이트 블록 통합: 여러 View 업데이트를 scheduleUiUpdate로 변경
        scheduleUiUpdate(() -> {
            // tvConnectBtRamp.setText(Constants.LogMessages.NOT_CONNECTED_BLUETOOTH_DEVICE);
            tvConnectBtRamp.setBackgroundColor(getColor(R.color.red_01));
            tvTestOkCnt.setText("0");
            tvTestNgCnt.setText("0");
            tvTemperature.setText("--");
            tvCompWattValue.setText("0");
            tvHeaterWattValue.setText("0");
            tvEllapsedTimeCnt.setText("0");
            tvPumpWattValue.setText("0");
            tv_log.setText("");
            clAlert.setVisibility(GONE);
            clTestResult.setVisibility(View.GONE);
            clDialogForPreprocess.setVisibility(GONE);
            tvPopupProcessResult.setText(Constants.Common.EMPTY_STRING);
            tvPopupProcessResultCompValue.setText(Constants.Common.EMPTY_STRING);
            tvPopupProcessResultHeaterValue.setText(Constants.Common.EMPTY_STRING);
            // tvConnectPlcRamp.setText("");
            tvConnectPlcRamp.setBackgroundColor(getColor(R.color.red_01));
            // tvRunWsRamp.setText("");
            tvRunWsRamp.setBackgroundColor(getColor(R.color.red_01));
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> testOkCnt " + testOkCnt + " testNgCnt " + testNgCnt);
            if (testOkCnt == 0 && testNgCnt == 0) {
                clTestResult.setVisibility(android.view.View.GONE);
            } else {
                tvPopupProcessResult.setText(Constants.ResultStatus.NG);
                tvPopupProcessResult.setTextSize(Dimension.SP, Constants.UI.TEXT_SIZE_LARGE_SP);
                tvPopupProcessResultCompValue.setTextSize(Dimension.SP, Constants.UI.TEXT_SIZE_MEDIUM_SP);
                tvPopupProcessResultHeaterValue.setTextSize(Dimension.SP, Constants.UI.TEXT_SIZE_MEDIUM_SP);
                SharedPreferences testPrefs = getSharedPreferences(Constants.SharedPrefKeys.TEST_INFO, MODE_PRIVATE);
                String testResult = testPrefs.getString(Constants.SharedPrefKeys.TEST_RESULT, Constants.Common.EMPTY_STRING);
                String heaterValue = testPrefs.getString(Constants.SharedPrefKeys.HEATER_VALUE, Constants.Common.EMPTY_STRING);
                String compValue = testPrefs.getString(Constants.SharedPrefKeys.COMP_VALUE, Constants.Common.EMPTY_STRING);
                tvPopupProcessResult.setText(testResult);
                tvPopupProcessResultCompValue.setText(compValue);
                tvPopupProcessResultHeaterValue.setText(heaterValue);
                clTestResult.setVisibility(android.view.View.VISIBLE);
            }
        });

        // rebuildTestItemList();
        rebuildTestItemListAsync();
        restartUsbMonitoring();   // ▼ 새 헬퍼 호출
        // btSearchOnOff = true;
        // bluetoothSearch();
        btReconnectDelayMs = Constants.Timeouts.BT_LIST_TIMER_INTERVAL_MS; // 예: 기존 값의 1/4
        btSearchOnOff = true;
        scheduleBluetoothReconnect(true);

        // 버전 정보 가져오기
        String localVersionId = null;
        String localModelId = null;

        /*
        for(int i=0; i<arrTestModels[0].length; i++) {
            logInfo(LogManager.LogCategory.PS, "> arrTestModels[0][" + i + "]: " + arrTestModels[0][i]);
        }
        */

        if (arrTestModels != null && arrTestModels[0] != null && arrTestModels[0].length > 4) {
            localVersionId = arrTestModels[0][3]; // CLM_TEST_VERSION_ID
            localModelId = arrTestModels[0][4]; // CLM_MODEL_VERSION
        }
        localVersionId = test_version_id;
        localModelId = model_id;
        // logInfo(LogManager.LogCategory.PS, "Local Version ID: " + localVersionId + ", Local Model Version: " + localModelVersion);

        if (mode_type.equals(Constants.ResultStatus.MODE_TYPE_TEST)) {
            // VersionInfoList 호출하여 네트워크 상태 체크 및 버전 비교
            if (localVersionId != null || localModelId != null) {
                new ActivityModelTestProcess.RequestVersionInfoThreadAsync(this, localVersionId, localModelId).execute();
            }
        }
    }

    private void restartUsbMonitoring() {
        // 1) 서비스가 내려가 있으면 다시 바인드
        if (usbService == null || !UsbService.SERVICE_CONNECTED) {
            startService(UsbService.class, usbConnection, null);
        }
        // 2) 권한이 없으면 다시 요청
        if (!usbConnPermissionGranted) {
            usbSearch();
            return; // 권한 승인 브로드캐스트에서 polling이 재개됨
        }
        // 3) 권한/서비스 모두 OK면 바로 폴링 재시작
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> startUsbPolling 2");
        startUsbPolling(true);
        // Phase 2: 단순 UI 업데이트를 scheduleUiUpdate로 변경
        scheduleUiUpdate(() -> {
            tvConnectPlcRamp.setBackgroundColor(getColor(R.color.blue_01));
            // tvConnectPlcRamp.setText(Constants.InitialValues.DEVICE_READY_CHARACTER);
        });
    }

    private void rebuildTestItemListAsync() {
        runOnBtWorker(() -> {
            if (arrTestItems == null || arrTestItems.length == 0) {
                return;
            }

            List<VoTestItem> newItems = new ArrayList<>();
            int total = 0;
            for (int i = 0; i < arrTestItems.length; i++) {
                int seconds = safeParseInt(arrTestItems[i][2]);
                total += (seconds > 1) ? seconds + 1 : seconds;

                Map<String, String> map = new HashMap<>();
                map.put(Constants.Common.TEST_ITEM_SEQ, String.valueOf(i + 1));
                map.put(Constants.Common.TEST_ITEM_NAME, arrTestItems[i][0]);
                map.put(Constants.Common.TEST_ITEM_COMMAND, arrTestItems[i][1]);
                map.put(Constants.Common.TEST_RESPONSE_VALUE, arrTestItems[i][7]);
                map.put(Constants.Common.TEST_ITEM_RESULT, getStringResource("ui.label.pre_process"));
                map.put(Constants.Common.TEST_FINISH_YN, Constants.ResultStatus.NO);
                map.put(Constants.Common.TEST_MODEL_ID, globalModelId);
                newItems.add(new VoTestItem(map));
            }
            final int totalTime = total;

            scheduleUiUpdate(() -> {
                totalTimeCnt = totalTime;
                listItemAdapter.clear();
                for (VoTestItem item : newItems) {
                    listItemAdapter.addItem(item);
                }
                tvTotalTimeCnt.setText(String.valueOf(totalTimeCnt));
                lvTestItem.setAdapter(listItemAdapter);
            });
        });
    }

    private int safeParseInt(String value) {
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private void rebuildTestItemList() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            runOnUiThread(this::rebuildTestItemList);
            return;
        }
        if (arrTestItems == null || arrTestItems.length == 0) {
            return;
        }
        if (listItemAdapter == null) {
            listItemAdapter = new ItemAdapterTestItem();
        } else {
            listItemAdapter.clear();
        }

        totalTimeCnt = 0;
        for (int i = 0; i < arrTestItems.length; i++) {
            try {
                int seconds = Integer.parseInt(arrTestItems[i][2]);
                totalTimeCnt += (seconds > 1) ? seconds + 1 : seconds;
                arrTestItems[i][3] = String.valueOf(totalTimeCnt);
            } catch (Exception ignored) {
            }

            Map<String, String> mapListItem = new HashMap<>();
            mapListItem.put(Constants.Common.TEST_ITEM_SEQ, String.valueOf(i + 1));
            mapListItem.put(Constants.Common.TEST_ITEM_NAME, arrTestItems[i][0]);
            mapListItem.put(Constants.Common.TEST_ITEM_COMMAND, arrTestItems[i][1]);
            mapListItem.put(Constants.Common.TEST_RESPONSE_VALUE, arrTestItems[i][7]);
            mapListItem.put(Constants.Common.TEST_ITEM_RESULT, getStringResource("ui.label.pre_process"));
            mapListItem.put(Constants.Common.TEST_FINISH_YN, Constants.ResultStatus.NO);
            mapListItem.put(Constants.Common.TEST_MODEL_ID, globalModelId);
            listItemAdapter.addItem(new VoTestItem(mapListItem));
        }
        lastTestIdx = listItemAdapter.getCount();

        // Phase 2: 단순 UI 업데이트를 scheduleUiUpdate로 변경
        scheduleUiUpdate(() -> {
            tvTotalTimeCnt.setText(String.valueOf(totalTimeCnt));
            lvTestItem.setAdapter(listItemAdapter);
        });
    }

    private void setupResourceInfoOverlay() {
        if (!Constants.ResultStatus.MODE_TYPE_TEST.equals(mode_type) || tvResourceInfo != null) {
            return;
        }
        FrameLayout content = findViewById(android.R.id.content);
        if (content == null) {
            return;
        }
        tvResourceInfo = new TextView(this);
//        tvResourceInfo.setTextColor(Color.WHITE);
//        tvResourceInfo.setBackgroundColor(0xAA000000);
        tvResourceInfo.setTextColor(Color.BLACK);
        tvResourceInfo.setBackgroundColor(0xFFFFFF);
        tvResourceInfo.setTextSize(20f);
        tvResourceInfo.setPadding(16, 8, 16, 8);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.BOTTOM;
        content.addView(tvResourceInfo, params);
        mainHandler.post(resourceInfoRunnable);
    }

    private void updateResourceInfo() {
        if (!Constants.ResultStatus.MODE_TYPE_TEST.equals(mode_type) || tvResourceInfo == null) {
            return;
        }
        long nativeHeap = Debug.getNativeHeapAllocatedSize() / 1024;
        Runtime runtime = Runtime.getRuntime();
        long javaUsed = (runtime.totalMemory() - runtime.freeMemory()) / 1024;
        long javaTotal = runtime.totalMemory() / 1024;
        String info = "";

        if (mode_type.equals(Constants.ResultStatus.MODE_TYPE_NORMAL)) {
            info = "Powered by ITFACTORY 2025";
        } else {
            info = String.format("Powered by ITFACTORY 2025 / Native: %d KB | Java: %d/%d KB | Ip: %s", nativeHeap, javaUsed, javaTotal, ipAddress);
        }
        tvResourceInfo.setText(info);
    }

    private void showPermissionPrompt(boolean permanentlyDenied) {
        logWarn(LogManager.LogCategory.BT, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Permission C.showPermissionPrompt ");
        if (permissionDialogShowing || isFinishing() || dialogManager == null) {
            return;
        }

        // 이미 다이얼로그가 표시 중이면 무시
        if (dialogManager.isDialogShowing(DialogManager.DialogType.PERMISSION)) {
            return;
        }

        permissionDialogShowing = true;

        // 영구적으로 거부되었거나 재시도 횟수가 초과된 경우 설정 화면으로 안내
        boolean shouldDirectToSettings = permanentlyDenied || permissionDenialCount >= MAX_PERMISSION_DENIAL_COUNT;

        String title = getStringResource("ui.message.bluetooth_permission_required");
        String message;
        if (shouldDirectToSettings) {
            message = getStringResource("ui.message.bluetooth_permission_blocked");
        } else {
            message = getStringResource("ui.message.bluetooth_permission_denied") +
                    "\n(재시도: " + permissionDenialCount + "/" + MAX_PERMISSION_DENIAL_COUNT + ")";
        }

        DialogManager.SimpleDialogConfig config = new DialogManager.SimpleDialogConfig()
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setNegativeButton(getStringResource("ui.message.exit"), (dialog, which) -> {
                    permissionDialogShowing = false;
                    finish();
                });

        if (shouldDirectToSettings) {
            // 설정 화면으로 이동
            config.setPositiveButton(getStringResource("ui.message.open_settings"), (dialog, which) -> {
                permissionDialogShowing = false;
                openAppSettings();
            });
        } else {
            // 권한 요청 재시도
            config.setPositiveButton(getStringResource("ui.message.allow_permission"), (dialog, which) -> {
                permissionDialogShowing = false;
                // 권한 요청을 다시 시도
                requestRuntimePermissions();
            });
        }

        dialogManager.showDialog(DialogManager.DialogType.PERMISSION, config);
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", getPackageName(), null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void startUsbPolling(boolean immediate) {
        if (usbPollingExecutor.isShutdown()) {
            logWarn(LogManager.LogCategory.US, "USB polling executor is shut down; cannot schedule polling");
            return;
        }
        if (usbService == null) {
            logWarn(LogManager.LogCategory.US, "UsbService is null; skipping polling start");
            return;
        }
        if (!usbConnPermissionGranted) {
            logWarn(LogManager.LogCategory.US, "USB permission not granted; skipping polling start");
            scheduleUsbPermissionRecovery();
            return;
        }

        // 명령 큐 초기화 및 시작
        if (usbCommandQueue == null) {
            usbCommandQueue = new UsbCommandQueue();
            usbCommandQueue.setUsbService(usbService);
            usbCommandQueue.start();
            logInfo(LogManager.LogCategory.US, "USB command queue initialized and started");
        }

        boolean pollingActive = usbPollingEnabled && usbPollingFuture != null && !usbPollingFuture.isCancelled();
        if (pollingActive) {
            logDebug(LogManager.LogCategory.US, "USB polling already running; skipping restart");
            return;
        }
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> stopUsbPolling 2");
        stopUsbPolling();
        usbPollingEnabled = true;
        usbPollingRequested = true;
        usbPollingFailureCount = 0;
        long initialDelay = immediate ? 0 : usbPollingIntervalMs;
        try {
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> stopUsbPolling 2-1 usbPollingEnabled:" + usbPollingEnabled + " usbService:" + usbService);
            usbPollingFuture = usbPollingExecutor.scheduleAtFixedRate(() -> {
                if (!usbPollingEnabled) {
                    System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> stopUsbPolling 3");
                    stopUsbPolling();
                    return;
                }
                if (usbService == null || usbCommandQueue == null) {
                    logWarn(LogManager.LogCategory.US, "UsbService or command queue is null; stopping polling");
                    System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> stopUsbPolling 4");
                    stopUsbPolling();
                    return;
                }
                try {
                    // 명령 큐를 통해 폴링 명령 전송
                    UsbCommandQueue.UsbCommand pollingCommand = new UsbCommandQueue.UsbCommand(
                            UsbCommandQueue.CommandType.POLLING,
                            Constants.PLCCommands.RSS0107_DW1006,
                            "Power consumption polling"
                    );

                    boolean enqueued = usbCommandQueue.enqueue(pollingCommand);
                    if (enqueued) {
                        usbPollingFailureCount = 0;
                    } else {
                        usbPollingFailureCount++;
                        logWarn(LogManager.LogCategory.US, "Failed to enqueue polling command (queue may be full)");
                    }

                    if (usbPollingFailureCount >= USB_POLLING_FAILURE_THRESHOLD) {
                        logWarn(LogManager.LogCategory.US, "USB polling failure threshold reached; backing off");
                        stopUsbPolling();
                        usbPollingIntervalMs = USB_POLLING_BACKOFF_MS;
                        startUsbPolling(false);
                    }
                } catch (Exception e) {
                    usbPollingFailureCount++;
                    logError(LogManager.LogCategory.ER, Constants.ErrorMessages.USB_SERVICE_ERROR, e);
                    if (usbPollingFailureCount >= USB_POLLING_FAILURE_THRESHOLD) {
                        logWarn(LogManager.LogCategory.US, "USB polling failure threshold reached; backing off");
                        stopUsbPolling();
                        usbPollingIntervalMs = USB_POLLING_BACKOFF_MS;
                        startUsbPolling(false);
                    }
                }
            }, initialDelay, usbPollingIntervalMs, TimeUnit.MILLISECONDS);
            logInfo(LogManager.LogCategory.US, "USB polling scheduled (interval: " + usbPollingIntervalMs + " ms)");
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, "Failed to schedule USB polling", e);
            usbPollingEnabled = false;
            usbPollingRequested = false;
        }
    }

    private void stopUsbPolling() {
        usbPollingEnabled = false;
        usbPollingRequested = false;
        usbPollingIntervalMs = Constants.Timeouts.USB_TIMER_INTERVAL_MS;
        if (usbPollingFuture != null) {
            usbPollingFuture.cancel(true);
            usbPollingFuture = null;
            logInfo(LogManager.LogCategory.US, "USB polling stopped");
        }

        // 명령 큐도 중지 (선택사항 - 필요시 주석 해제)
        // if (usbCommandQueue != null) {
        //     usbCommandQueue.stop();
        //     usbCommandQueue = null;
        // }
    }

    /**
     * PLC로 명령 전송 (명령 큐를 통해 순차 처리)
     * @param command PLC 명령 문자열
     * @param description 명령 설명
     * @param onSuccess 성공 콜백 (선택)
     * @param onError 실패 콜백 (선택)
     * @return true if command was enqueued successfully
     */
    public boolean sendUsbCommand(String command, String description, Runnable onSuccess, Runnable onError) {
        if (usbCommandQueue == null || !usbCommandQueue.isRunning()) {
            logWarn(LogManager.LogCategory.US, "USB command queue is not running, cannot send command: " + description);
            if (onError != null) {
                onError.run();
            }
            return false;
        }

        if (command == null || command.trim().isEmpty()) {
            logWarn(LogManager.LogCategory.US, "Invalid command, cannot send: " + description);
            if (onError != null) {
                onError.run();
            }
            return false;
        }

        UsbCommandQueue.UsbCommand usbCommand = new UsbCommandQueue.UsbCommand(
                UsbCommandQueue.CommandType.USER_COMMAND,
                command.getBytes(),  // String을 byte[]로 변환
                description,
                onSuccess,
                onError
        );

        // 우선순위 명령으로 큐에 추가 (폴링 명령보다 먼저 처리)
        boolean enqueued = usbCommandQueue.enqueuePriority(usbCommand);
        if (enqueued) {
            logInfo(LogManager.LogCategory.US, "USB command enqueued: " + description);
        } else {
            logWarn(LogManager.LogCategory.US, "Failed to enqueue USB command: " + description);
            if (onError != null) {
                onError.run();
            }
        }

        return enqueued;
    }

    /**
     * PLC로 명령 전송 (간단한 버전)
     * @param command PLC 명령 문자열
     * @param description 명령 설명
     * @return true if command was enqueued successfully
     */
    public boolean sendUsbCommand(String command, String description) {
        return sendUsbCommand(command, description, null, null);
    }

    /**
     * PLC로 명령 전송하고 응답을 받음
     * @param command PLC 명령 문자열
     * @param description 명령 설명
     * @param timeoutMs 타임아웃 (밀리초)
     * @return CompletableFuture<String> 응답 문자열
     */
    public CompletableFuture<String> sendUsbCommandWithResponse(String command, String description, long timeoutMs) {
        CompletableFuture<String> future = new CompletableFuture<>();

        if (usbCommandQueue == null || !usbCommandQueue.isRunning()) {
            future.completeExceptionally(new RuntimeException("USB command queue is not running"));
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

        boolean sent = sendUsbCommand(command, description, onSuccess, onError);
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
     * USB 응답을 처리 (USB Handler에서 호출)
     * @param response USB 응답 문자열
     */
    private void handleUsbResponse(String response) {
        if (response == null || response.isEmpty()) {
            return;
        }

        synchronized (usbResponseLock) {
            // 가장 최근의 대기 중인 응답에 데이터 전달
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

    private void scheduleUsbReconnect(boolean immediate) {
        if (isUsbReconnecting) {
            return;
        }
        isUsbReconnecting = true;
        if (usbReconnectRunnable == null) {
            usbReconnectRunnable = this::attemptUsbReconnect;
        }
        usbReconnectHandler.removeCallbacks(usbReconnectRunnable);
        usbReconnectHandler.postDelayed(usbReconnectRunnable, immediate ? 0 : USB_PERMISSION_RECOVERY_DELAY_MS);
        updateUsbLampReconnecting();
    }

    private void cancelUsbReconnect() {
        usbReconnectHandler.removeCallbacks(usbReconnectRunnable);
        isUsbReconnecting = false;
    }

    private void attemptUsbReconnect() {
        if (!isUsbReconnecting) {
            return;
        }
        boolean success = tryReconnectUsb();
        if (success) {
            cancelUsbReconnect();
            usbReconnectAttempts = 0;
            updateUsbLampReady();
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> startUsbPolling 4");
            startUsbPolling(true);
            return;
        }
        usbReconnectAttempts++;
        if (usbReconnectAttempts >= USB_RETRY_MAX_ATTEMPTS) {
            usbReconnectAttempts = 0;
            logWarn(LogManager.LogCategory.US, "USB reconnect failed after " + usbReconnectAttempts + " attempts");
            cancelUsbReconnect();
            scheduleUsbPermissionRecovery();
            scheduleUiUpdate(() -> {
                updateUsbLampDisconnected();
                clAlert.setVisibility(VISIBLE);
                tvAlertMessage.setText("USB 또는 블루투스 연결을 확인해주세요.");    // USB 또는 블루투스 연결을 확인해주세요.
                resetBluetoothSessionKeepUsb();
            });
        } else {
            usbReconnectHandler.postDelayed(usbReconnectRunnable,
                    USB_PERMISSION_RECOVERY_DELAY_MS * Math.min(usbReconnectAttempts, 5));
        }
    }

    private boolean tryReconnectUsb() {
        try {
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> stopUsbPolling 6");
            stopUsbPolling();
            setFilters();
            startService(UsbService.class, usbConnection, null);
            return usbConnPermissionGranted && usbService != null;
        } catch (Exception e) {
            logError(LogManager.LogCategory.US, "USB reconnect attempt failed", e);
            return false;
        }
    }

    private boolean isUsbReady() {
        System.out.println("> 2.usbConnPermissionGranted " + usbConnPermissionGranted + " / usbService " + usbService + " / usbPollingEnabled " + usbPollingEnabled);
        if (usbConnPermissionGranted) {
            // stopBtMessageTimer();
            tmrReset = new Timer();
            tmrBTMessageSend = new Timer("BtMsgTimer");
            resetCnt = 0;
            usbReconnectAttempts = 0;
            disconnectCheckCount = 0;
            receivedMessageCnt = 0;
            sendingMessageCnt = 0;
        }
        return usbConnPermissionGranted && usbService != null && usbPollingEnabled;
    }

    private boolean isBluetoothReady() {
        return btConnected && btSocket != null && btSocket.isConnected();
    }

    private boolean canEnterTest() {
        System.out.println("> 2.isUsbReady() " + isUsbReady() + " / isBluetoothReady() " + isBluetoothReady());
        return isUsbReady() && isBluetoothReady();
    }

    private void updateUsbLampDisconnected() {
        scheduleUiUpdate(() -> {
            tvConnectPlcRamp.setBackgroundColor(getColor(R.color.red_01));
            // tvConnectPlcRamp.setText("USB OFF");
        });
    }

    private void updateUsbLampReconnecting() {
        scheduleUiUpdate(() -> {
            tvConnectPlcRamp.setBackgroundColor(getColor(R.color.orange_01));
            // tvConnectPlcRamp.setText("USB RETRY");
        });
    }

    private void updateUsbLampReady() {
        scheduleUiUpdate(() -> {
            tvConnectPlcRamp.setBackgroundColor(getColor(R.color.blue_01));
            // tvConnectPlcRamp.setText(Constants.InitialValues.DEVICE_READY_CHARACTER);
        });
    }


    private void recordWattMeasurement(String command, double wattValue) {
        if (TextUtils.isEmpty(command)) {
            return;
        }
        String key = canonicalWattKey(command);
        if (TextUtils.isEmpty(key)) {
            return;
        }
        latestWattByCommand.put(key, wattValue);
    }

    private Double getLatestWattMeasurement(String command) {
        if (TextUtils.isEmpty(command)) {
            return null;
        }
        String key = canonicalWattKey(command);
        return TextUtils.isEmpty(key) ? null : latestWattByCommand.get(key);
    }

    private String canonicalWattKey(String command) {
        if (TextUtils.isEmpty(command)) {
            return null;
        }
        switch (command) {
            case Constants.TestItemCodes.CM0102:
                return Constants.TestItemCodes.CM0101;
            case Constants.TestItemCodes.HT0100:
                return Constants.TestItemCodes.HT0101;
            case Constants.TestItemCodes.SV0201:
            case Constants.TestItemCodes.SV0301:
            case Constants.TestItemCodes.SV0401:
                return Constants.TestItemCodes.SV0101;
            default:
                return command;
        }
    }

    private boolean isWattCommand(String command) {
        if (TextUtils.isEmpty(command)) {
            return false;
        }
        switch (canonicalWattKey(command)) {
            case Constants.TestItemCodes.CM0101:
            case Constants.TestItemCodes.HT0101:
            case Constants.TestItemCodes.PM0101:
            case Constants.TestItemCodes.SV0101:
                return true;
            default:
                return false;
        }
    }

//    private String formatWattValue(double value) {
//        return String.valueOf((int) Math.round(value));
//    }

    private String ensureComponentWattText(String currentText, String... commandKeys) {
        if (!TextUtils.isEmpty(currentText)) {
            return currentText;
        }
        if (commandKeys == null) {
            return currentText;
        }
        for (String key : commandKeys) {
            Double cached = getLatestWattMeasurement(key);
            if (cached != null) {
                return formatWattValue(cached);
            }
        }
        return currentText;
    }

    private String ensureWattInfo(String currentInfo, String commandKey) {
        if (!TextUtils.isEmpty(currentInfo) || !isWattCommand(commandKey)) {
            return currentInfo;
        }
        Double cached = getLatestWattMeasurement(commandKey);
        if (cached == null) {
            return currentInfo;
        }
        return formatWattValue(cached) + Constants.Common.WATT_UNIT;
    }

    private void scheduleUsbPermissionRecovery() {
        if (usbConnPermissionGranted) {
            logDebug(LogManager.LogCategory.US, "USB permission already granted; skipping recovery scheduling");
            return;
        }

        if (usbPermissionRecoveryRunnable == null) {
            usbPermissionRecoveryRunnable = () -> {
                if (usbConnPermissionGranted) {
                    logDebug(LogManager.LogCategory.US, "USB permission granted before recovery runnable executed");
                    return;
                }
                scheduleUiUpdate(() -> {
                    final int finalUsbStatusColor = R.color.red_01;
                    // final String finalUsbStatusText = "";
                    tvConnectPlcRamp.setBackgroundColor(getColor(finalUsbStatusColor));
                    // tvConnectPlcRamp.setText(finalUsbStatusText);
                });
                logInfo(LogManager.LogCategory.US, "Attempting USB permission recovery");
                try {
                    setFilters();
                } catch (Exception e) {
                    logError(LogManager.LogCategory.ER, "Error while setting USB filters during recovery", e);
                }

                new Thread(() -> {
                    try {
                        startService(UsbService.class, usbConnection, null);
                    } catch (Exception e) {
                        logError(LogManager.LogCategory.ER, "Error while rebinding USB service during recovery", e);
                    }
                }).start();
            };
        }

        usbRecoveryHandler.removeCallbacks(usbPermissionRecoveryRunnable);
        usbRecoveryHandler.postDelayed(usbPermissionRecoveryRunnable, USB_PERMISSION_RECOVERY_DELAY_MS);
    }

    private void recordWattForCommand(String command, double watt) {
        if (TextUtils.isEmpty(command)) {
            return;
        }
        latestWattByCommand.put(command, watt);
    }

    private boolean isWattTrackingCommand(String command) {
        return command != null && WATT_TRACKING_COMMANDS.contains(command);
    }

    private boolean isSolValveCommand(String command) {
        return command != null && SOL_VALVE_COMMANDS.contains(command);
    }

    private String ensureWattText(String command, String currentText) {
        if (!TextUtils.isEmpty(currentText)) {
            return currentText;
        }
        if (TextUtils.isEmpty(command)) {
            return currentText;
        }
        Double cached = latestWattByCommand.get(command);
        if (cached == null) {
            return currentText;
        }
        return formatWattValue(cached);
    }

    private String formatWattValue(double watt) {
        double rounded = Math.rint(watt);
        if (Math.abs(watt - rounded) < 0.01d) {
            return String.valueOf((int) rounded);
        }
        return String.format(Locale.US, "%.1f", watt);
    }

    /**
     * 블루투스 연결 시도 인디케이터 시작 (배경색 반복 변경)
     */
    private void startBtConnectionIndicator() {
        if (btConnectionIndicatorRunning) {
            return; // 이미 실행 중이면 중복 시작 방지
        }

        btConnectionIndicatorRunning = true;
        final boolean[] useOrange = {true}; // 색상 토글을 위한 배열

        btConnectionIndicatorRunnable = new Runnable() {
            @Override
            public void run() {
                if (!btConnectionIndicatorRunning || isFinishing() || isDestroyed()) {
                    return;
                }

                // orange_01과 yellow_01 사이를 반복
                int color = useOrange[0] ? R.color.orange_01 : R.color.yellow_01;
                useOrange[0] = !useOrange[0]; // 다음 색상으로 토글

                scheduleUiUpdate(() -> {
                    if (tvConnectBtRamp != null && btConnectionIndicatorRunning) {
                        tvConnectBtRamp.setBackgroundColor(getColor(color));
                    }
                });

                // 다음 변경 예약
                if (btConnectionIndicatorRunning && schedulerHandler != null) {
                    schedulerHandler.postDelayed(this, BT_CONNECTION_INDICATOR_INTERVAL_MS);
                }
            }
        };

        // 즉시 시작
        schedulerHandler.post(btConnectionIndicatorRunnable);
        // logInfo(LogManager.LogCategory.BT, "블루투스 연결 인디케이터 시작");
    }

    /**
     * 블루투스 연결 시도 인디케이터 중지
     */
    private void stopBtConnectionIndicator() {
        if (!btConnectionIndicatorRunning) {
            return; // 실행 중이 아니면 중복 중지 방지
        }

        btConnectionIndicatorRunning = false;

        if (btConnectionIndicatorRunnable != null && schedulerHandler != null) {
            schedulerHandler.removeCallbacks(btConnectionIndicatorRunnable);
            btConnectionIndicatorRunnable = null;
        }

        // 최종 색상 설정
        scheduleUiUpdate(() -> {
            if (tvConnectBtRamp != null) {
                if (btConnected) {
                    // 연결 성공 시 파란색
                    tvConnectBtRamp.setBackgroundColor(getColor(R.color.blue_01));
                } else {
                    // 연결 실패 시 빨간색
                    tvConnectBtRamp.setBackgroundColor(getColor(R.color.red_01));
                }
            }
        });

        // logInfo(LogManager.LogCategory.BT, "블루투스 연결 인디케이터 중지");
    }

    private void scheduleBluetoothReconnect(boolean immediate) {
        if (!btSearchOnOff) {
            return;
        }
        if (btReconnectRunnable == null) {
            btReconnectRunnable = this::attemptBluetoothReconnect;
        }
        schedulerHandler.removeCallbacks(btReconnectRunnable);
        long delay = immediate ? 0 : btReconnectDelayMs;
        schedulerHandler.postDelayed(btReconnectRunnable, delay);
    }

    private void clearBluetoothReconnect() {
        if (btReconnectRunnable != null) {
            schedulerHandler.removeCallbacks(btReconnectRunnable);
        }
        btReconnectRunnable = null;

        // 연결 인디케이터도 중지
        stopBtConnectionIndicator();
    }

    @SuppressLint("MissingPermission")
    private void attemptBluetoothReconnect() {
        if (!btSearchOnOff || btConnectionInProgress || btConnected) {
            return;
        }
        if (mBTAdapter == null) {
            logWarn(LogManager.LogCategory.BT, "Bluetooth adapter null, skipping reconnect");
            return;
        }

        if (!hasBluetoothConnectPermission()) {
            logWarn(LogManager.LogCategory.BT, "BLUETOOTH_CONNECT permission missing; cannot attempt reconnect");
            ensureBtPermissions(PermissionAction.BT_RECONNECT);
            return;
        }

        if (!mBTAdapter.isEnabled()) {
            logWarn(LogManager.LogCategory.BT, "Bluetooth adapter disabled, skipping reconnect");
            return;
        }

        if (mPairedDevices == null || mPairedDevices.isEmpty()) {
            mPairedDevices = getBondedDevicesSafe();
        }

        if (mPairedDevices == null || mPairedDevices.isEmpty()) {
            logInfo(LogManager.LogCategory.BT, "No paired Bluetooth devices available");
            btReconnectDelayMs = Math.min(btReconnectDelayMs, BT_RECONNECT_DELAY_MAX_MS);
            showNoPairedDevicesDialog();
            scheduleBluetoothReconnect(false);
            return;
        }

        BluetoothDevice targetDevice = selectPreferredDevice(mPairedDevices);
        if (targetDevice == null) {
            btReconnectDelayMs = Math.min(btReconnectDelayMs, BT_RECONNECT_DELAY_MAX_MS);
            scheduleBluetoothReconnect(false);
            return;
        }

        // 연결 시도 인디케이터 시작
        startBtConnectionIndicator();

        btConnectionInProgress = true;
        runOnBtWorker(() -> {
            boolean success = connectToDevice(targetDevice);
            btConnectionInProgress = false;
            if (success) {
                btReconnectDelayMs = Constants.Timeouts.BT_LIST_TIMER_INTERVAL_MS;
            } else {
                btReconnectDelayMs = Math.min(btReconnectDelayMs, BT_RECONNECT_DELAY_MAX_MS);
                scheduleBluetoothReconnect(false);
            }
        });
    }

    private BluetoothDevice selectPreferredDevice(Set<BluetoothDevice> devices) {
        if (devices == null || devices.isEmpty()) {
            return null;
        }

        if (deviceSelected != null) {
            for (BluetoothDevice device : devices) {
                if (device != null && deviceSelected.getAddress().equals(device.getAddress())) {
                    return device;
                }
            }
        }

        for (BluetoothDevice device : devices) {
            if (device != null) {
                return device;
            }
        }
        return null;
    }

    private void scheduleConnectionTimeoutCheck() {
        mainHandler.postDelayed(() -> {
            if (!isConnected) {
                cancelConnection();
            }
        }, Constants.Timeouts.CONNECTION_TIMEOUT);
    }

    private void closeSocketSilently(BluetoothSocket socket) {
        if (socket == null) {
            return;
        }
        try {
            socket.close();
        } catch (IOException e) {
            logError(LogManager.LogCategory.ER, Constants.ErrorMessages.ERROR_CLOSING_SOCKET, e);
        }
    }

    @SuppressLint("MissingPermission")
    private boolean connectToDevice(BluetoothDevice device) {
        if (device == null) {
            return false;
        }

        BluetoothSocket tempSocket = null;
        try {
            if (!hasBluetoothConnectPermission()) {
                logWarn(LogManager.LogCategory.BT, "BLUETOOTH_CONNECT permission not granted");
                return false;
            }

            btDeviceName = device.getName();
            btDeviceAddr = device.getAddress();

            boolean alreadyConnected = isDeviceConnectedSafe(device);
            if (alreadyConnected && entranceCheck) {
                logInfo(LogManager.LogCategory.BT, String.format("이미 연결된 블루투스 장비 유지: %s / %s", btDeviceName, btDeviceAddr));
                return true;
            }

            deviceSelected = device;

            if (btSocket != null && btSocket.isConnected()) {
                closeSocketSilently(btSocket);
            }

            tempSocket = createBluetoothSocket(deviceSelected);
            btSocket = tempSocket;

            // ⚠️ CRITICAL ANR PROTECTION: Ensure we're NOT on UI thread
            if (Looper.myLooper() == Looper.getMainLooper()) {
                logError(LogManager.LogCategory.ER, "❌ CRITICAL: btSocket.connect() called on UI thread! This will cause ANR!", new RuntimeException("Stack trace"));
                throw new RuntimeException("btSocket.connect() must not be called on UI thread - will cause ANR!");
            }

            btSocket.connect();  // ⚠️ This is a BLOCKING call (5-15 seconds!)
            isConnected = true;
            btConnected = true;

            btConnectedThread = new ConnectedThreadOptimized(btSocket, btHandler);
            btConnectedThread.start();

            btHandler.obtainMessage(CONNECTING_STATUS, 1, -1, btDeviceName).sendToTarget();
            btConnected = true;
            scheduleConnectionTimeoutCheck();

            // 연결 성공 시 인디케이터 중지
            stopBtConnectionIndicator();

            return true;
        } catch (IOException e) {
            logError(LogManager.LogCategory.ER, Constants.ErrorMessages.BLUETOOTH_SOCKET_CONNECTION_ERROR, e);
            isConnected = false;
            onConnectionFailed();
            closeSocketSilently(tempSocket);
            btSocket = null;
            return false;
        }
    }

    @SuppressLint("MissingPermission")
    private boolean isDeviceConnectedSafe(BluetoothDevice device) {
        if (!hasBluetoothConnectPermission()) {
            return false;
        }
        try {
            Method m = device.getClass().getMethod(Constants.InitialValues.BT_CONNECTED, (Class[]) null);
            return (boolean) m.invoke(device, (Object[]) null);
        } catch (Exception e) {
            logError(LogManager.LogCategory.BT, "Failed to check Bluetooth device connection state", e);
            return false;
        }
    }

    private void refreshSpecCache(List<Map<String, String>> specs) {
        specCache.clear();
        if (specs == null || specs.isEmpty()) {
            return;
        }
        for (Map<String, String> spec : specs) {
            if (spec == null) {
                continue;
            }
            String command = spec.get(Constants.JsonKeys.CLM_TEST_COMMAND);
            if (command == null || command.isEmpty()) {
                continue;
            }
            specCache.put(command, new HashMap<>(spec));
        }
    }

    private boolean applyTestSpecData(List<Map<String, String>> sourceData, boolean persistToDb) {
        if (sourceData == null || sourceData.isEmpty()) {
            return false;
        }

        // ⚠️ 중요: 데이터 정규화는 빠르므로 메인 스레드에서 수행
        List<Map<String, String>> normalizedList = new ArrayList<>();
        for (Map<String, String> item : sourceData) {
            normalizedList.add(new HashMap<>(item));
        }

        lstSpecData = normalizedList;
        refreshSpecCache(normalizedList);

        // ⚠️ 중요: 모든 무거운 작업을 백그라운드 스레드로 이동하여 메인 스레드 블로킹 방지
        new Thread(() -> {
            Context context = getApplicationContext();
            int tempTotalTimeCnt = 0;
            String[][] tempArrTestItems = new String[normalizedList.size()][10];
            String tempValueWatt = Constants.Common.EMPTY_STRING;
            String tempLowerValueWatt = Constants.Common.EMPTY_STRING;
            String tempUpperValueWatt = Constants.Common.EMPTY_STRING;
            String tempProductSerialNo = Constants.Common.EMPTY_STRING;

            // 1. 데이터 처리 루프 (백그라운드 스레드에서 실행)
            for (int i = 0; i < normalizedList.size(); i++) {
                Map<String, String> spec = normalizedList.get(i);
                if (persistToDb) {
                    TestData.insertTestSpecData(context, spec);
                }
                try {
                    int seconds = Integer.parseInt(valueOrEmpty(spec.get(Constants.JsonKeys.CLM_TEST_SEC)));
                    tempTotalTimeCnt += seconds;
                } catch (Exception ignored) {
                }

                tempArrTestItems[i][0] = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_TEST_NAME));
                tempArrTestItems[i][1] = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND));
                tempArrTestItems[i][2] = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_TEST_SEC));
                tempArrTestItems[i][3] = valueOrEmpty(String.valueOf(tempTotalTimeCnt));
                tempArrTestItems[i][4] = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_TEST_TYPE));
                tempArrTestItems[i][5] = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND));
                tempArrTestItems[i][6] = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_TEST_STEP));
                tempArrTestItems[i][7] = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_RESPONSE_VALUE));

                // ⚠️ 중요: Double.parseDouble() 예외 처리 강화
                try {
                    if (Objects.equals(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND), Constants.TestItemCodes.CM0101) ||
                            Objects.equals(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND), Constants.TestItemCodes.HT0101) ||
                            Objects.equals(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND), Constants.TestItemCodes.PM0101) ||
                            Objects.equals(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND), Constants.TestItemCodes.SV0101) ||
                            Objects.equals(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND), Constants.TestItemCodes.SV0201) ||
                            Objects.equals(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND), Constants.TestItemCodes.SV0301) ||
                            Objects.equals(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND), Constants.TestItemCodes.SV0401)
                    ) {
                        String valueWattStr = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_VALUE_WATT));
                        String lowerValueWattStr = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT));
                        String upperValueWattStr = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT));

                        if (!valueWattStr.isEmpty() && !lowerValueWattStr.isEmpty() && !upperValueWattStr.isEmpty()) {
                            double valueWatt = Double.parseDouble(valueWattStr);
                            double lowerValueWatt = Double.parseDouble(lowerValueWattStr);
                            double upperValueWatt = Double.parseDouble(upperValueWattStr);
                            tempArrTestItems[i][8] = String.valueOf(valueWatt - lowerValueWatt);
                            tempArrTestItems[i][9] = String.valueOf(valueWatt + upperValueWatt);
                        } else {
                            tempArrTestItems[i][8] = "0";
                            tempArrTestItems[i][9] = "0";
                        }
                    } else {
                        String testCommand = spec.get(Constants.JsonKeys.CLM_TEST_COMMAND);
                        switch (testCommand) {
                            case Constants.TestItemCodes.TH0101:
                            case Constants.TestItemCodes.TH0201:
                            case Constants.TestItemCodes.TH0301:
                                String valueStr = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_VALUE));
                                String lowerValueStr = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_LOWER_VALUE));
                                String upperValueStr = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_UPPER_VALUE));

                                if (!valueStr.isEmpty() && !lowerValueStr.isEmpty() && !upperValueStr.isEmpty()) {
                                    double value = Double.parseDouble(valueStr);
                                    double lowerValue = Double.parseDouble(lowerValueStr);
                                    double upperValue = Double.parseDouble(upperValueStr);
                                    tempArrTestItems[i][8] = String.valueOf(value - lowerValue);
                                    tempArrTestItems[i][9] = String.valueOf(value + upperValue);
                                } else {
                                    tempArrTestItems[i][8] = "0";
                                    tempArrTestItems[i][9] = "0";
                                }
                                break;
                            default:
                                tempArrTestItems[i][8] = "0";
                                tempArrTestItems[i][9] = "0";
                                break;
                        }
                    }
                } catch (NumberFormatException e) {
                    // 파싱 실패 시 기본값 사용
                    tempArrTestItems[i][8] = "0";
                    tempArrTestItems[i][9] = "0";
                }

                tempValueWatt = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_VALUE_WATT));
                tempLowerValueWatt = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT));
                tempUpperValueWatt = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT));
                tempProductSerialNo = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_PRODUCT_SERIAL_NO));
            }

            // 2. 시간 계산 (백그라운드 스레드에서 실행) - ⚠️ 개선: 메인 스레드에서 제거됨
            int calculatedTotalTimeCnt = 0;
            for (int i = 0; i < tempArrTestItems.length; i++) {
                try {
                    int seconds = Integer.parseInt(tempArrTestItems[i][2]);
                    calculatedTotalTimeCnt += (seconds > 1) ? seconds + 1 : seconds;
                    tempArrTestItems[i][3] = String.valueOf(calculatedTotalTimeCnt);
                } catch (NumberFormatException e) {
                    // 파싱 실패 시 기본값 사용
                    tempArrTestItems[i][3] = String.valueOf(calculatedTotalTimeCnt);
                }
            }

            // 3. UI 업데이트 값 수집 (백그라운드 스레드에서 실행) - ⚠️ 개선: 메인 스레드에서 제거됨
            String compValueWatt = null;
            String compLowerValueWatt = null;
            String compUpperValueWatt = null;
            String pumpValueWatt = null;
            String pumpLowerValueWatt = null;
            String pumpUpperValueWatt = null;
            String heaterValueWatt = null;
            String heaterLowerValueWatt = null;
            String heaterUpperValueWatt = null;

            for (int i = 0; i < normalizedList.size(); i++) {
                try {
                    Map<String, String> spec = normalizedList.get(i);
                    String command = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND));
                    String itemValueWatt = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_VALUE_WATT));
                    String itemLowerValueWatt = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT));
                    String itemUpperValueWatt = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT));

                    // 값 수집 (UI 업데이트는 나중에 한 번에 수행)
                    if (command.contains(Constants.TestItemCodes.CM0101)) {
                        compValueWatt = itemValueWatt;
                        compLowerValueWatt = itemLowerValueWatt;
                        compUpperValueWatt = itemUpperValueWatt;
                    }
                    if (command.contains(Constants.TestItemCodes.PM0101)) {
                        pumpValueWatt = itemValueWatt;
                        pumpLowerValueWatt = itemLowerValueWatt;
                        pumpUpperValueWatt = itemUpperValueWatt;
                    }
                    if (command.contains(Constants.TestItemCodes.HT0101)) {
                        compValueWatt = itemValueWatt;
                        compLowerValueWatt = itemLowerValueWatt;
                        compUpperValueWatt = itemUpperValueWatt;
                        heaterValueWatt = itemValueWatt;
                        heaterLowerValueWatt = itemLowerValueWatt;
                        heaterUpperValueWatt = itemUpperValueWatt;
                    }
                } catch (Exception e) {
                    logError(LogManager.LogCategory.ER, Constants.ErrorMessages.ERROR_IN_UI_UPDATE, e);
                }
            }

            // 4. ListView 아이템 데이터 준비 (백그라운드 스레드에서 실행) - ⚠️ 개선: 메인 스레드에서 제거됨
            // ⚠️ 주의: getString()은 메인 스레드에서만 호출 가능하므로,
            //          문자열 리소스는 메인 스레드에서 미리 가져와서 사용
            //          또는 runOnUiThread 내부에서만 호출
            //
            // 해결 방법: getString() 호출을 제거하고 하드코딩된 문자열 사용
            //            또는 메인 스레드에서 미리 가져온 값을 사용
            //            여기서는 기존 코드와 동일하게 처리하기 위해
            //            getString() 호출은 runOnUiThread 내부로 이동

            // 백그라운드에서 준비 가능한 데이터만 준비
            // getString() 호출이 필요한 부분은 runOnUiThread 내부에서 처리
            List<Map<String, String>> itemsDataToAdd = new ArrayList<>();
            for (int i = 0; i < tempArrTestItems.length; i++) {
                Map<String, String> itemData = new HashMap<>();
                itemData.put(Constants.Common.TEST_ITEM_SEQ, String.valueOf(i + 1));
                itemData.put(Constants.Common.TEST_ITEM_NAME, tempArrTestItems[i][0]);
                itemData.put(Constants.Common.TEST_ITEM_COMMAND, tempArrTestItems[i][1]);
                itemData.put(Constants.Common.TEST_RESPONSE_VALUE, tempArrTestItems[i][7]);
                itemData.put(Constants.Common.TEST_FINISH_YN, Constants.ResultStatus.NO);
                itemData.put(Constants.Common.TEST_MODEL_ID, globalModelId);
                itemsDataToAdd.add(itemData);
            }

            // final 변수로 캡처
            final int finalTotalTimeCnt = calculatedTotalTimeCnt;
            final String[][] finalArrTestItems = tempArrTestItems;
            final String finalValueWatt = tempValueWatt;
            final String finalLowerValueWatt = tempLowerValueWatt;
            final String finalUpperValueWatt = tempUpperValueWatt;
            final String finalProductSerialNo = tempProductSerialNo;
            final List<Map<String, String>> finalItemsDataToAdd = itemsDataToAdd;
            final String finalCompValueWatt = compValueWatt;
            final String finalCompLowerValueWatt = compLowerValueWatt;
            final String finalCompUpperValueWatt = compUpperValueWatt;
            final String finalPumpValueWatt = pumpValueWatt;
            final String finalPumpLowerValueWatt = pumpLowerValueWatt;
            final String finalPumpUpperValueWatt = pumpUpperValueWatt;
            final String finalHeaterValueWatt = heaterValueWatt;
            final String finalHeaterLowerValueWatt = heaterLowerValueWatt;
            final String finalHeaterUpperValueWatt = heaterUpperValueWatt;

            SpecProcessingResult result = new SpecProcessingResult();
            result.arrTestItems = finalArrTestItems;
            result.totalTimeCnt = finalTotalTimeCnt;
            result.valueWatt = finalValueWatt;
            result.lowerValueWatt = finalLowerValueWatt;
            result.upperValueWatt = finalUpperValueWatt;
            result.productSerialNo = finalProductSerialNo;
            result.listItems = finalItemsDataToAdd;
            result.compValueWatt = finalCompValueWatt;
            result.compLowerValueWatt = finalCompLowerValueWatt;
            result.compUpperValueWatt = finalCompUpperValueWatt;
            result.pumpValueWatt = finalPumpValueWatt;
            result.pumpLowerValueWatt = finalPumpLowerValueWatt;
            result.pumpUpperValueWatt = finalPumpUpperValueWatt;
            result.heaterValueWatt = finalHeaterValueWatt;
            result.heaterLowerValueWatt = finalHeaterLowerValueWatt;
            result.heaterUpperValueWatt = finalHeaterUpperValueWatt;

            scheduleUiUpdate(() -> applySpecProcessingResult(result));
        }).start();

        return true;
    }

    private void applySpecProcessingResult(SpecProcessingResult result) {
        if (result == null) {
            return;
        }
        arrTestItems = result.arrTestItems;
        totalTimeCnt = result.totalTimeCnt;
        valueWatt = result.valueWatt;
        lowerValueWatt = result.lowerValueWatt;
        upperValueWatt = result.upperValueWatt;
        productSerialNo = result.productSerialNo;

        if (result.compValueWatt != null) {
            tvCompValueWatt.setText(result.compValueWatt);
            updateRangeViews(tvCompLowerValueWatt, tvCompUpperValueWatt,
                    result.compValueWatt, result.compLowerValueWatt, result.compUpperValueWatt);
        }
        if (result.pumpValueWatt != null) {
            tvPumpValueWatt.setText(result.pumpValueWatt);
            updateRangeViews(tvPumpLowerValueWatt, tvPumpUpperValueWatt,
                    result.pumpValueWatt, result.pumpLowerValueWatt, result.pumpUpperValueWatt);
        }
        if (result.heaterValueWatt != null) {
            tvHeaterValueWatt.setText(result.heaterValueWatt);
            updateRangeViews(tvHeaterLowerValueWatt, tvHeaterUpperValueWatt,
                    result.heaterValueWatt, result.heaterLowerValueWatt, result.heaterUpperValueWatt);
        }

        listItemAdapter = new ItemAdapterTestItem();
        lstTestResult = new ArrayList<>();
        lstTestTemperature = new ArrayList<>();
        String preProcessText = getStringResource("ui.label.pre_process");
        for (Map<String, String> item : result.listItems) {
            Map<String, String> map = new HashMap<>(item);
            map.put(Constants.Common.TEST_ITEM_RESULT, preProcessText);
            listItemAdapter.addItem(new VoTestItem(map));
        }
        tvTotalTimeCnt.setText(String.valueOf(totalTimeCnt));
        lvTestItem.setAdapter(listItemAdapter);
        listItemAdapter.updateListAdapter();
        lastTestIdx = listItemAdapter.getCount();
    }

    private static class SpecProcessingResult {
        String[][] arrTestItems;
        int totalTimeCnt;
        String valueWatt;
        String lowerValueWatt;
        String upperValueWatt;
        String productSerialNo;
        String compValueWatt;
        String compLowerValueWatt;
        String compUpperValueWatt;
        String pumpValueWatt;
        String pumpLowerValueWatt;
        String pumpUpperValueWatt;
        String heaterValueWatt;
        String heaterLowerValueWatt;
        String heaterUpperValueWatt;
        List<Map<String, String>> listItems = new ArrayList<>();
    }

    /*
    private boolean applyTestSpecData(List<Map<String, String>> sourceData, boolean persistToDb) {
        if (sourceData == null || sourceData.isEmpty()) {
            return false;
        }

        // ⚠️ 중요: 데이터 정규화는 빠르므로 메인 스레드에서 수행
        List<Map<String, String>> normalizedList = new ArrayList<>();
        for (Map<String, String> item : sourceData) {
            normalizedList.add(new HashMap<>(item));
        }

        lstSpecData = normalizedList;
        refreshSpecCache(normalizedList);

        // ⚠️ 중요: 무거운 데이터 처리를 백그라운드 스레드로 이동하여 메인 스레드 블로킹 방지
        new Thread(() -> {
            Context context = getApplicationContext();
            int tempTotalTimeCnt = 0;
            String[][] tempArrTestItems = new String[normalizedList.size()][10];
            String tempValueWatt = Constants.Common.EMPTY_STRING;
            String tempLowerValueWatt = Constants.Common.EMPTY_STRING;
            String tempUpperValueWatt = Constants.Common.EMPTY_STRING;
            String tempProductSerialNo = Constants.Common.EMPTY_STRING;

            // 데이터 처리 루프 (백그라운드 스레드에서 실행)
            for (int i = 0; i < normalizedList.size(); i++) {
                Map<String, String> spec = normalizedList.get(i);
                if (persistToDb) {
                    TestData.insertTestSpecData(context, spec);
                }
                try {
                    int seconds = Integer.parseInt(valueOrEmpty(spec.get(Constants.JsonKeys.CLM_TEST_SEC)));
                    tempTotalTimeCnt += seconds;
                } catch (Exception ignored) {
                }

                tempArrTestItems[i][0] = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_TEST_NAME));
                tempArrTestItems[i][1] = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND));
                tempArrTestItems[i][2] = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_TEST_SEC));
                tempArrTestItems[i][3] = valueOrEmpty(String.valueOf(tempTotalTimeCnt));
                tempArrTestItems[i][4] = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_TEST_TYPE));
                tempArrTestItems[i][5] = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND));
                tempArrTestItems[i][6] = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_TEST_STEP));
                tempArrTestItems[i][7] = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_RESPONSE_VALUE));

                // ⚠️ 중요: Double.parseDouble() 예외 처리 강화
                try {
                    if(Objects.equals(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND), Constants.TestItemCodes.CM0101) ||
                        Objects.equals(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND), Constants.TestItemCodes.HT0101) ||
                        Objects.equals(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND), Constants.TestItemCodes.PM0101) ||
                        Objects.equals(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND), Constants.TestItemCodes.SV0101) ||
                        Objects.equals(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND), Constants.TestItemCodes.SV0201) ||
                        Objects.equals(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND), Constants.TestItemCodes.SV0301) ||
                        Objects.equals(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND), Constants.TestItemCodes.SV0401)
                    ) {
                        String valueWattStr = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_VALUE_WATT));
                        String lowerValueWattStr = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT));
                        String upperValueWattStr = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT));

                        if (!valueWattStr.isEmpty() && !lowerValueWattStr.isEmpty() && !upperValueWattStr.isEmpty()) {
                            double valueWatt = Double.parseDouble(valueWattStr);
                            double lowerValueWatt = Double.parseDouble(lowerValueWattStr);
                            double upperValueWatt = Double.parseDouble(upperValueWattStr);
                            tempArrTestItems[i][8] = String.valueOf(valueWatt - lowerValueWatt);
                            tempArrTestItems[i][9] = String.valueOf(valueWatt + upperValueWatt);
                        } else {
                            tempArrTestItems[i][8] = "0";
                            tempArrTestItems[i][9] = "0";
                        }
                    } else {
                        String testCommand = spec.get(Constants.JsonKeys.CLM_TEST_COMMAND);
                        switch (testCommand) {
                            case Constants.TestItemCodes.TH0101:
                            case Constants.TestItemCodes.TH0201:
                            case Constants.TestItemCodes.TH0301:
                                String valueStr = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_VALUE));
                                String lowerValueStr = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_LOWER_VALUE));
                                String upperValueStr = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_UPPER_VALUE));

                                if (!valueStr.isEmpty() && !lowerValueStr.isEmpty() && !upperValueStr.isEmpty()) {
                                    double value = Double.parseDouble(valueStr);
                                    double lowerValue = Double.parseDouble(lowerValueStr);
                                    double upperValue = Double.parseDouble(upperValueStr);
                                    tempArrTestItems[i][8] = String.valueOf(value - lowerValue);
                                    tempArrTestItems[i][9] = String.valueOf(value + upperValue);
                                } else {
                                    tempArrTestItems[i][8] = "0";
                                    tempArrTestItems[i][9] = "0";
                                }
                                break;
                            default:
                                tempArrTestItems[i][8] = "0";
                                tempArrTestItems[i][9] = "0";
                                break;
                        }
                    }
                } catch (NumberFormatException e) {
                    // 파싱 실패 시 기본값 사용
                    tempArrTestItems[i][8] = "0";
                    tempArrTestItems[i][9] = "0";
                }

                tempValueWatt = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_VALUE_WATT));
                tempLowerValueWatt = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT));
                tempUpperValueWatt = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT));
                tempProductSerialNo = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_PRODUCT_SERIAL_NO));
            }

            final int finalTotalTimeCnt = tempTotalTimeCnt;
            final String[][] finalArrTestItems = tempArrTestItems;
            final String finalValueWatt = tempValueWatt;
            final String finalLowerValueWatt = tempLowerValueWatt;
            final String finalUpperValueWatt = tempUpperValueWatt;
            final String finalProductSerialNo = tempProductSerialNo;
            final List<Map<String, String>> finalSpecList = normalizedList;

            // 복잡한 UI 업데이트 블록 통합: scheduleUiUpdate 사용
            scheduleUiUpdate(() -> {
            try {
                ActivityModel_0002 act = getMainActivity();
                if (act == null || act.isFinishing()) {
                    return;
                }

                arrTestItems = finalArrTestItems;
                totalTimeCnt = finalTotalTimeCnt;
                valueWatt = finalValueWatt;
                lowerValueWatt = finalLowerValueWatt;
                upperValueWatt = finalUpperValueWatt;
                productSerialNo = finalProductSerialNo;

                // ⚠️ 중요: UI 업데이트를 배치 처리하여 메인 스레드 블로킹 최소화
                // 모든 업데이트 값을 먼저 수집한 후 한 번에 적용
                String compValueWatt = null;
                String compLowerValueWatt = null;
                String compUpperValueWatt = null;
                String pumpValueWatt = null;
                String pumpLowerValueWatt = null;
                String pumpUpperValueWatt = null;
                String heaterValueWatt = null;
                String heaterLowerValueWatt = null;
                String heaterUpperValueWatt = null;

                for (int i = 0; i < finalSpecList.size(); i++) {
                    try {
                        Map<String, String> spec = finalSpecList.get(i);
                        String command = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND));
                        String itemValueWatt = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_VALUE_WATT));
                        String itemLowerValueWatt = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT));
                        String itemUpperValueWatt = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT));

                        // 값 수집 (UI 업데이트는 나중에 한 번에 수행)
                        if (command.contains(Constants.TestItemCodes.CM0101)) {
                            compValueWatt = itemValueWatt;
                            compLowerValueWatt = itemLowerValueWatt;
                            compUpperValueWatt = itemUpperValueWatt;
                        }
                        if (command.contains(Constants.TestItemCodes.PM0101)) {
                            pumpValueWatt = itemValueWatt;
                            pumpLowerValueWatt = itemLowerValueWatt;
                            pumpUpperValueWatt = itemUpperValueWatt;
                        }
                        if (command.contains(Constants.TestItemCodes.HT0101)) {
                            compValueWatt = itemValueWatt;
                            compLowerValueWatt = itemLowerValueWatt;
                            compUpperValueWatt = itemUpperValueWatt;
                            heaterValueWatt = itemValueWatt;
                            heaterLowerValueWatt = itemLowerValueWatt;
                            heaterUpperValueWatt = itemUpperValueWatt;
                        }
                    } catch (Exception e) {
                        logError(LogManager.LogCategory.ER, Constants.ErrorMessages.ERROR_IN_UI_UPDATE, e);
                    }
                }

                // 한 번에 UI 업데이트 (배치 처리)
                if (compValueWatt != null) {
                    tvCompValueWatt.setText(compValueWatt);
                    updateRangeViews(tvCompLowerValueWatt, tvCompUpperValueWatt, compValueWatt, compLowerValueWatt, compUpperValueWatt);
                }
                if (pumpValueWatt != null) {
                    tvPumpValueWatt.setText(pumpValueWatt);
                    updateRangeViews(tvPumpLowerValueWatt, tvPumpUpperValueWatt, pumpValueWatt, pumpLowerValueWatt, pumpUpperValueWatt);
                }
                if (heaterValueWatt != null) {
                    tvHeaterValueWatt.setText(heaterValueWatt);
                    updateRangeViews(tvHeaterLowerValueWatt, tvHeaterUpperValueWatt, heaterValueWatt, heaterLowerValueWatt, heaterUpperValueWatt);
                }

                totalTimeCnt = 0;
                listItemAdapter = new ItemAdapterTestItem();
                lstTestResult = new ArrayList<>();
                lstTestTemperature = new ArrayList<>();

                // ⚠️ 중요: 로그 출력을 완전히 제거하여 메인 스레드 블로킹 방지
                // 디버그가 필요한 경우에만 주석 해제 (260번의 로그 출력으로 인한 성능 저하 방지)
                // for (int i = 0; i < arrTestItems.length; i++) {
                //     for (int j = 0; j < arrTestItems[i].length; j++) {
                //         logInfo(LogManager.LogCategory.PS, "> [" + i + "][" + j + "] " + arrTestItems[i][j]);
                //     }
                //     logInfo(LogManager.LogCategory.PS, "> ");
                // }

                // 시간 계산 최적화: try-catch로 예외 처리하여 안정성 향상
                for (int i = 0; i < arrTestItems.length; i++) {
                    try {
                        int seconds = Integer.parseInt(arrTestItems[i][2]);
                        totalTimeCnt += (seconds > 1) ? seconds + 1 : seconds;
                        arrTestItems[i][3] = String.valueOf(totalTimeCnt);
                    } catch (NumberFormatException e) {
                        // 파싱 실패 시 기본값 사용
                        arrTestItems[i][3] = String.valueOf(totalTimeCnt);
                    }
                }

                // ⚠️ 중요: ListView 아이템 추가를 배치 처리하여 UI 업데이트 최적화
                // notifyDataSetChanged()를 한 번만 호출하도록 모든 아이템을 먼저 추가
                for (int i = 0; i < arrTestItems.length; i++) {
                    Map<String, String> mapListItem = new HashMap<>();
                    mapListItem.put(Constants.Common.TEST_ITEM_SEQ, String.valueOf(i + 1));
                    mapListItem.put(Constants.Common.TEST_ITEM_NAME, arrTestItems[i][0]);
                    mapListItem.put(Constants.Common.TEST_ITEM_COMMAND, arrTestItems[i][1]);
                    mapListItem.put(Constants.Common.TEST_RESPONSE_VALUE, arrTestItems[i][7]);
                    mapListItem.put(Constants.Common.TEST_ITEM_RESULT, getStringResource("ui.label.pre_process"));
                    mapListItem.put(Constants.Common.TEST_FINISH_YN, Constants.ResultStatus.NO);
                    mapListItem.put(Constants.Common.TEST_MODEL_ID, globalModelId);
                    listItemAdapter.addItem(new VoTestItem(mapListItem));
                }

                // ⚠️ 중요: Handler 재사용 (통합된 mainHandler 사용)
                // UI 업데이트를 다음 프레임으로 지연시켜 메인 스레드 블로킹 방지
                mainHandler.post(() -> {
                    tvTotalTimeCnt.setText(String.valueOf(totalTimeCnt));
                    lvTestItem.setAdapter(listItemAdapter);
                    listItemAdapter.updateListAdapter(); // 한 번만 notifyDataSetChanged() 호출
                    lastTestIdx = listItemAdapter.getCount();
                });
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, Constants.ErrorMessages.ERROR_IN_UI_UPDATE_JSON_PARSING, e);
            }
        });
        }).start(); // 백그라운드 스레드 시작

        return true;
    }
    */

    // ============================================================
    // Phase 1: UI 업데이트 배치 처리 시스템
    // ============================================================

    /**
     * UI 업데이트를 배치 처리로 스케줄링
     * 여러 UI 업데이트를 모아서 하나의 runOnUiThread로 처리하여 메시지 큐 부하 감소
     *
     * @param update UI 업데이트 Runnable
     */
    private void scheduleUiUpdate(Runnable update) {
        if (update == null) {
            return;
        }

        // 이미 메인 스레드에서 실행 중이면 즉시 실행
        if (Looper.myLooper() == Looper.getMainLooper()) {
            try {
                update.run();
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, "Error in immediate UI update", e);
            }
            return;
        }

        synchronized (uiUpdateLock) {
            pendingUiUpdates.add(update);

            // 배치 작업이 예약되지 않았다면 예약
            if (uiUpdateBatchTask == null) {
                uiUpdateBatchTask = () -> {
                    List<Runnable> updates;
                    synchronized (uiUpdateLock) {
                        if (pendingUiUpdates.isEmpty()) {
                            uiUpdateBatchTask = null;
                            return;
                        }
                        updates = new ArrayList<>(pendingUiUpdates);
                        pendingUiUpdates.clear();
                        uiUpdateBatchTask = null;
                    }

                    // 모든 UI 업데이트를 한 번에 실행
                    for (Runnable r : updates) {
                        try {
                            r.run();
                        } catch (Exception e) {
                            logError(LogManager.LogCategory.ER, "Error in UI update batch", e);
                        }
                    }
                };

                // 다음 프레임에 실행 (16ms 지연 = 1 frame @ 60fps)
                mainHandler.postDelayed(uiUpdateBatchTask, UI_UPDATE_BATCH_DELAY_MS);
            }
        }
    }

    /**
     * 즉시 UI 업데이트가 필요한 경우 (중요한 업데이트)
     * 배치 처리 없이 즉시 실행
     *
     * @param update UI 업데이트 Runnable
     */
    private void scheduleUiUpdateImmediate(Runnable update) {
        if (update == null) {
            return;
        }

        // 이미 메인 스레드에서 실행 중이면 즉시 실행
        if (Looper.myLooper() == Looper.getMainLooper()) {
            try {
                update.run();
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, "Error in immediate UI update", e);
            }
            return;
        }

        // 즉시 실행 (배치 처리 없음)
        mainHandler.post(update);
    }

    /**
     * 배치 처리 큐 정리 (Activity destroy 시 호출)
     */
    private void clearUiUpdateBatchQueue() {
        synchronized (uiUpdateLock) {
            if (uiUpdateBatchTask != null) {
                mainHandler.removeCallbacks(uiUpdateBatchTask);
                uiUpdateBatchTask = null;
            }
            pendingUiUpdates.clear();
        }
    }
    // ============================================================

    private void updateRangeViews(TextView lowerView, TextView upperView, String centerValue, String lowerOffset, String upperOffset) {
        try {
            if (lowerView != null) {
                lowerView.setText(String.valueOf(Integer.parseInt(centerValue) - Integer.parseInt(lowerOffset)));
            }
            if (upperView != null) {
                upperView.setText(String.valueOf(Integer.parseInt(centerValue) + Integer.parseInt(upperOffset)));
            }
        } catch (Exception ignored) {
        }
    }

    private String valueOrEmpty(String value) {
        return value == null ? Constants.Common.EMPTY_STRING : value;
    }

    private List<Map<String, String>> getSpecData(String command) {
        if (command == null || command.trim().isEmpty()) {
            logWarn(LogManager.LogCategory.BT, "finalReceiveCommand is null or empty, skipping database query");
            return Collections.emptyList();
        }

        Map<String, String> cached = specCache.get(command);
        if (cached != null) {
            return Collections.singletonList(cached);
        }

        try {
            List<Map<String, String>> specDataResult = TestData.selectTestSpecData(getBaseContext(), Constants.Database.QUERY_AND_CLM_TEST_COMMAND_EQUALS + command + "'");
            if (specDataResult != null && !specDataResult.isEmpty()) {
                specCache.put(command, new HashMap<>(specDataResult.get(0)));
                return specDataResult;
            }
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, "Error querying test spec data for command: " + command, e);
        }
        return Collections.emptyList();
    }

    private String computeSha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            logError(LogManager.LogCategory.ER, "Unable to compute SHA-256 hash", e);
            return null;
        }
    }

    @SuppressLint("MissingPermission")
    private void bluetoothOn() {
        if (mBTAdapter == null) {
            logWarn(LogManager.LogCategory.BT, "Bluetooth adapter not available");
            return;
        }
        if (!hasBluetoothConnectPermission()) {
            ensureBtPermissions(null);
            return;
        }
        if (!mBTAdapter.isEnabled()) { // 블루투스 어댑터 활성화 요청
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            cancelDiscoverySafe();
        }
    }

    private void sendBtMessage(String sendMessage) {
        try {
            if (btConnectedThread != null) {
                if (sendMessage.contains(Constants.TestItemCodes.CM0200)) {
                    logDebug(LogManager.LogCategory.BT, Constants.LogMessages.SEND_BT_MESSAGE_CM0200_TO_CM0100 + sendMessage);
                    sendMessage = Constants.TestItemCodes.CM0100;
                }
                System.out.println(">>>>>>>>>> sendBtMessage.sendMessage " + sendMessage);
                btConnectedThread.write(sendMessage);

                // Thread.sleep 제거: 메인 스레드에서 호출될 경우 블로킹 방지
                // 대신 비동기로 짧은 지연이 필요한 경우 Handler 사용
                // 주의: 이 메서드가 메인 스레드에서 호출되지 않도록 호출부 확인 필요
            }
            // Thread.sleep(100); 제거됨 - 메인 스레드 블로킹 방지
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, Constants.ErrorMessages.SEND_BT_MESSAGE_ERROR, e);
        }
    }

    public static String getCurrentDatetime(String dateformat) {
        SimpleDateFormat dateFormmater = new SimpleDateFormat(dateformat);
        return dateFormmater.format(new Date());
    }

    // Enter here after user selects "yes" or "no" to enabling radio
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent Data) {
        // Check which request we're responding to
        super.onActivityResult(requestCode, resultCode, Data);
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                // Make sure the request was successful
                if (resultCode == RESULT_OK) {
                    // The user picked a contact.
                    // The Intent's data Uri identifies which contact was selected.
                    mBluetoothStatus.setText(getStringResource("ui.label.enabled"));
                } else {
                    mBluetoothStatus.setText(getStringResource("ui.label.disabled"));
                }
                break;
        }
    }

    private void checkBTPermissions() {
        ensureBtPermissions(null);
    }

    @SuppressLint("MissingPermission")
    public void listPairedDevicesSelect() {
        try {
            if (mBTArrayAdapter == null) {
                return;
            }
            if (!hasAllBluetoothPermissions()) {
                ensureBtPermissions(PermissionAction.LIST_PAIRED_DEVICES);
                return;
            }
            if (mBTAdapter == null) {
                logWarn(LogManager.LogCategory.BT, "Bluetooth adapter unavailable; cannot list devices");
                return;
            }
            mBTArrayAdapter.clear();

            mPairedDevices = getBondedDevicesSafe();
            int pairedCount = (mPairedDevices == null) ? 0 : mPairedDevices.size();
            boolean adapterEnabled = isBluetoothAdapterEnabled();
            logInfo(LogManager.LogCategory.BT, String.format("페어링된 블루투스 장비 - enabled: %s, count: %d, entranceCheck: %s",
                    adapterEnabled, pairedCount, entranceCheck));
            if (adapterEnabled) {
                btReconnectDelayMs = Constants.Timeouts.BT_LIST_TIMER_INTERVAL_MS;
                scheduleBluetoothReconnect(true);
            } else {
                // 블루투스 활성화 다이얼로그 표시
                if (dialogManager != null && !dialogManager.isDialogShowing(DialogManager.DialogType.BLUETOOTH_ENABLE)) {
                    DialogManager.SimpleDialogConfig config = new DialogManager.SimpleDialogConfig()
                            .setTitle(getStringResource("dialog.bluetooth.title"))
                            .setMessage(getStringResource("ui.message.bluetooth_enable"))
                            .setNegativeButton(getStringResource("dialog.cancel"), (dialog, which) -> {
                                dialog.cancel();
                            })
                            .setCancelable(true);

                    dialogManager.showDialog(DialogManager.DialogType.BLUETOOTH_ENABLE, config);
                }
            }
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, Constants.ErrorMessages.LIST_PAIRED_DEVICES_SELECT_ERROR, e);
        }
    }

    /**
     * 페어링된 블루투스 장비가 없을 때 다이얼로그 표시
     * 이미 다이얼로그가 표시 중이면 다시 표시하지 않음
     */
    private void showNoPairedDevicesDialog() {
        if (dialogManager == null || isFinishing()) {
            return;
        }

        // 이미 다이얼로그가 표시 중이면 무시
        if (dialogManager.isDialogShowing(DialogManager.DialogType.NO_PAIRED_DEVICES)) {
            return;
        }

        DialogManager.SimpleDialogConfig config = new DialogManager.SimpleDialogConfig()
                .setTitle(getStringResource("dialog.bluetooth.title"))
                .setMessage(getStringResource("ui.message.no_paired_bluetooth"))
                .setNegativeButton(getStringResource("dialog.cancel"), (dialog, which) -> {
                    dialog.cancel();
                })
                .setCancelable(true);

        dialogManager.showDialog(DialogManager.DialogType.NO_PAIRED_DEVICES, config);
    }

    private void onConnectionSuccess() {
        // Handle the success state, e.g., notify the user, start communication, etc.
        logInfo(LogManager.LogCategory.BT, Constants.LogMessages.BLUETOOTH_CONNECTION_SUCCESSFUL);

        // 연결 성공 시 인디케이터 중지
        stopBtConnectionIndicator();
    }

    private void onConnectionFailed() {
        // Handle connection failure, e.g., retry connection or notify user
        logInfo(LogManager.LogCategory.BT, Constants.LogMessages.BLUETOOTH_CONNECTION_FAILED);

        // 연결 실패 시 인디케이터 중지
        stopBtConnectionIndicator();

        scheduleBluetoothReconnect(false);
    }

    private void cancelConnection() {
        if (btSocket != null && btSocket.isConnected()) {
            try {
                btSocket.close();
                logInfo(LogManager.LogCategory.BT, Constants.LogMessages.CONNECTION_ATTEMPT_TIMED_OUT);
            } catch (IOException e) {
                logError(LogManager.LogCategory.ER, Constants.ErrorMessages.BLUETOOTH_CANCEL_CONNECTION_ERROR, e);
                e.printStackTrace();
            }
        }
        isConnected = false;
        btConnected = false;
        btConnectionInProgress = false;
        if (btConnectedThread != null) {
            try {
                btConnectedThread.cancel();
            } catch (Exception e) {
                logWarn(LogManager.LogCategory.BT, "Error canceling Bluetooth thread during cancelConnection: " + e.getMessage());
            }
            btConnectedThread = null;
        }
        scheduleBluetoothReconnect(false);
    }

    @SuppressLint("MissingPermission")
    public boolean isConnected(BluetoothDevice device) {
        if (!hasBluetoothConnectPermission()) {
            ensureBtPermissions(PermissionAction.BT_RECONNECT);
            return false;
        }
        try {
            Method m = device.getClass().getMethod(Constants.InitialValues.BT_CONNECTED, (Class[]) null);
            boolean connected = (boolean) m.invoke(device, (Object[]) null);
            return connected;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressLint("MissingPermission")
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        if (!hasBluetoothConnectPermission()) {
            ensureBtPermissions(PermissionAction.BT_RECONNECT);
            throw new IOException("Missing BLUETOOTH_CONNECT permission");
        }
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
            return (BluetoothSocket) m.invoke(device, BT_MODULE_UUID);
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, Constants.ErrorMessages.COULD_NOT_CREATE_INSECURE_RFCOMM_CONNECTION, e);
        }
        try {
            return device.createRfcommSocketToServiceRecord(BT_MODULE_UUID);
        } catch (SecurityException e) {
            throw new IOException("Failed to create RFCOMM socket due to missing permission", e);
        }
    }


    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            logDebug(LogManager.LogCategory.PS, Constants.LogMessages.USB_INTENT_ACTION + intent.getAction());
            // Phase 4: USB Handler 내부 통합 - UI 업데이트 정보를 저장
            ActivityModelTestProcess activity = getMainActivity();
            int usbStatusColor = -1; // -1이면 UI 업데이트 없음
            String usbStatusText = null;

            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    // Toast.makeText(context, Constants.UIMessages.USB_READY, Toast.LENGTH_SHORT).show();

                    usbConnPermissionGranted = true; // 권한 플래그를 먼저 올려 재시작 로직이 차단되지 않도록 함
                    cancelUsbReconnect();
                    updateUsbLampReady();

                    // ⚠️ 중요: USB 재연결 시 onNewIntent()를 호출하여 액티비티 재실행 방지
                    if (activity != null && !activity.isFinishing()) {
                        // 2. Intent 생성 및 Flag 설정
                        Intent reconnectIntent = new Intent(context, ActivityModelTestProcess.class);
                        reconnectIntent.setAction("USB_RECONNECTED");
                        reconnectIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        reconnectIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                        // 3. startActivity() 호출 (시스템이 자동으로 onNewIntent() 호출)
                        // launchMode="singleTop"과 FLAG_ACTIVITY_SINGLE_TOP으로 인해
                        // Activity가 재실행되지 않고 onNewIntent()만 호출됨
//                        activity.startActivity(reconnectIntent);

                        activity.onNewIntent(reconnectIntent);  // ✅ 생명주기 변경 없음
                        activity.setIntent(reconnectIntent);
                    }
                    if (usbService != null && (usbPollingFuture == null || usbPollingFuture.isCancelled())) {
                        logInfo(LogManager.LogCategory.US, "USB permission granted - starting polling");
                        startUsbPolling(true);
                    }
                    // UI 업데이트: 권한 승인 시 색상 변경 (필요시)
                    // usbStatusColor = R.color.colorAccent; // 필요시 주석 해제
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    // Toast.makeText(context, Constants.UIMessages.USB_PERMISSION_NOT_GRANTED, Toast.LENGTH_SHORT).show();
                    usbStatusColor = R.color.colorAccent;
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    // Toast.makeText(context, Constants.UIMessages.NO_USB_CONNECTED, Toast.LENGTH_SHORT).show();
                    usbStatusColor = R.color.green_01;
                    scheduleUiUpdate(() -> {
                        final int finalUsbStatusColor = R.color.red_01;
                        // final String finalUsbStatusText = "";
                        tvConnectPlcRamp.setBackgroundColor(getColor(finalUsbStatusColor));
                        // tvConnectPlcRamp.setText(finalUsbStatusText);
                    });
                    // USB 연결 해제 시 폴링 정리
                    System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> stopUsbPolling 7");
                    stopUsbPolling();

                    decElectricValue = 0;

                    // ⚠️ 중요: 서비스는 해제하지 않고, 플래그만 초기화
                    // usbService는 null로 설정하지 않음 (재연결 시 자동으로 다시 연결됨)
                    usbConnPermissionGranted = false;  // 권한 플래그만 초기화
                    scheduleUsbPermissionRecovery();
                    scheduleUsbReconnect(false);

//                    if(tmrBTMessageSend!=null) {
//                        tmrBTMessageSend.cancel();
//                        ttBTMessageSend.cancel();
//                        tmrBTMessageSend = null;
//                    }
                    stopBtMessageTimer();
                    disconnectCheckCount = 0;
                    receivedMessageCnt = 0;
                    sendingMessageCnt = 0;

                    usbStatusColor = R.color.red_01;
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    // Toast.makeText(context, Constants.UIMessages.USB_DISCONNECTED, Toast.LENGTH_SHORT).show();
                    stopBtMessageTimer();
                    scheduleUiUpdate(() -> {
                        final int finalUsbStatusColor = R.color.red_01;
                        // final String finalUsbStatusText = "";
                        tvConnectPlcRamp.setBackgroundColor(getColor(finalUsbStatusColor));
                        // tvConnectPlcRamp.setText(finalUsbStatusText);
                    });
                    // USB 연결 해제 시 폴링 정리
                    System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> stopUsbPolling 8");
                    stopUsbPolling();

                    decElectricValue = 0;

                    // ⚠️ 중요: 서비스는 해제하지 않고, 플래그만 초기화
                    // usbService는 null로 설정하지 않음 (재연결 시 자동으로 다시 연결됨)
                    usbConnPermissionGranted = false;  // 권한 플래그만 초기화
                    scheduleUsbPermissionRecovery();
                    scheduleUsbReconnect(false);

//                    if(tmrBTMessageSend!=null) {
//                        tmrBTMessageSend.cancel();
//                        ttBTMessageSend.cancel();
//                        tmrBTMessageSend = null;
//                    }
                    stopBtMessageTimer();
                    disconnectCheckCount = 0;
                    receivedMessageCnt = 0;
                    sendingMessageCnt = 0;

                    usbStatusColor = R.color.red_01;
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    // Toast.makeText(context, Constants.UIMessages.USB_DEVICE_NOT_SUPPORTED, Toast.LENGTH_SHORT).show();
                    usbStatusColor = R.color.orange_01;
                    break;
                default:
                    // Toast.makeText(context, Constants.LogMessages.CONNECTION_STATE + intent.getAction(), Toast.LENGTH_SHORT).show();
                    logInfo(LogManager.LogCategory.PS, Constants.LogMessages.USB_CONNECTION_STATE + intent.getAction());
                    break;
            }

            // Phase 4: 모든 case 처리 후 한 번만 UI 업데이트 (통합)
            if (usbStatusColor != -1 && activity != null && !activity.isFinishing()) {
                final int finalUsbStatusColor = usbStatusColor;
//                final String finalUsbStatusText = usbStatusText;
                scheduleUiUpdate(() -> {
                    tvConnectPlcRamp.setBackgroundColor(getColor(finalUsbStatusColor));
//                    if (finalUsbStatusText != null) {
//                        tvConnectPlcRamp.setText(finalUsbStatusText);
//                    }
                });
            }
        }
    };

    private ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            try {
                // 안전한 타입 체크 및 예외 처리
                if (arg1 instanceof UsbService.UsbBinder) {
                    UsbService.UsbBinder binder = (UsbService.UsbBinder) arg1;
                    usbService = binder.getService();

                    // 명령 큐에 UsbService 설정
                    if (usbCommandQueue != null) {
                        usbCommandQueue.setUsbService(usbService);
                    } else {
                        // 명령 큐가 없으면 생성 및 시작
                        usbCommandQueue = new UsbCommandQueue();
                        usbCommandQueue.setUsbService(usbService);
                        usbCommandQueue.start();
                        logInfo(LogManager.LogCategory.US, "USB command queue initialized");
                    }

                    if (usbService != null) {
                        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> USB Handler 등록 전: usbHandler=" + usbHandler);
                        // Handler가 아직 준비되지 않았다면 즉시 생성 후 등록
                        if (usbHandler == null) {
                            usbHandler = new ActivityModelTestProcess.UsbHandler(ActivityModelTestProcess.this);
                            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> USB Handler 새로 생성");
                        }
                        if (usbHandler != null) {
                            // Register Handler for USB data reception
                            usbService.setHandler(usbHandler);
                            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> USB Handler 등록 완료");
                        } else {
                            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> USB Handler가 null입니다!");
                            logError(LogManager.LogCategory.ER, "USB Handler is null, cannot register with UsbService", null);
                        }
                        logInfo(LogManager.LogCategory.PS, Constants.LogMessages.USB_SERVICE_CONNECTED_SUCCESSFULLY);

                        // 앱 재시작 시 USB 자동 연결: USB 서비스 연결 후 자동으로 연결 메시지 전송 시도
                        // Timer가 아직 시작되지 않은 경우에만 시작 (중복 방지)
                        if (usbPollingFuture == null || usbPollingFuture.isCancelled()) {
                            logInfo(LogManager.LogCategory.US, Constants.LogMessages.USB_SERVICE_CONNECTED);

                            // USB 권한이 이미 부여된 경우 즉시 시작, 그렇지 않은 경우 권한 부여 대기
                            if (usbConnPermissionGranted) {
                                logInfo(LogManager.LogCategory.US, Constants.LogMessages.USB_PERMISSION_ALREADY_GRANTED);

                                // HTTP 통신 관련 runOnUiThread 최적화: scheduleUiUpdate 사용
                                scheduleUiUpdate(() -> {
                                    tvConnectPlcRamp.setBackgroundColor(getBaseContext().getResources().getColor(R.color.blue_01));
                                    // tvConnectPlcRamp.setText(Constants.InitialValues.DEVICE_READY_CHARACTER);
                                });

                                System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> startUsbPolling 5");
                                startUsbPolling(true);
                            } else {
                                logInfo(LogManager.LogCategory.US, Constants.LogMessages.USB_PERMISSION_WAITING);
                            }
                        } else {
                            logDebug(LogManager.LogCategory.US, Constants.LogMessages.USB_CONNECTION_TIMER_ALREADY_RUNNING);
                        }
                    } else {
                        logError(LogManager.LogCategory.ER, Constants.ErrorMessages.USB_SERVICE_IS_NULL_AFTER_BINDING, null);
                        usbService = null;
                    }
                } else {
                    logError(LogManager.LogCategory.ER, Constants.ErrorMessages.INVALID_BINDER_TYPE_EXPECTED_USB_SERVICE_USB_BINDER_GOT +
                            (arg1 != null ? arg1.getClass().getName() : Constants.Common.NULL), null);
                    usbService = null;
                }
            } catch (ClassCastException e) {
                logError(LogManager.LogCategory.ER, Constants.ErrorMessages.CLASS_CAST_EXCEPTION_IN_ONSERVICECONNECTED, e);
                e.printStackTrace();
                usbService = null;
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, Constants.ErrorMessages.CLASS_CAST_EXCEPTION_IN_ONSERVICECONNECTED, e);
                e.printStackTrace();
                usbService = null;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            logInfo(LogManager.LogCategory.PS, Constants.LogMessages.USB_SERVICE_DISCONNECTED);
            // ⚠️ 중요: usbService를 null로 설정하지 않음
            // USB 재연결 시 자동으로 onServiceConnected가 호출되어 다시 설정됨
            // usbService = null;  // 주석 처리하여 액티비티 재시작 방지
            usbConnPermissionGranted = false;  // 권한 플래그만 초기화
        }
    };

    @Override
    public void onResume() {
        super.onResume();

        // ⚠️ 키오스크 모드: onResume에서도 시스템 UI 숨기기 재적용
        setupKioskMode();

        // 권한 상태 확인 (사용자가 설정에서 권한을 허용하고 돌아온 경우 감지)
        // 백그라운드 스레드에서 체크하여 메인 스레드 블로킹 방지
        if (!btPermissionsGranted) {
            runOnBtWorker(() -> {
                // 백그라운드에서 권한 상태 확인
                boolean hasAllPermissions = hasAllBluetoothPermissions();
                if (hasAllPermissions) {
                    // 메인 스레드로 포스팅하여 UI 업데이트 및 대기 중인 작업 실행
                    runOnUiThread(() -> {
                        permissionDenialCount = 0; // 권한이 허용되었으면 거부 횟수 리셋
                        btPermissionsGranted = true;
                        permissionRequestInProgress = false;
                        runPendingPermissionActions();
                        logInfo(LogManager.LogCategory.BT, "Bluetooth permissions granted on resume");
                    });
                }
            });
        }

        // USB 재연결 처리 중이면 스킵
        if (isHandlingUsbReconnection) {
            return;
        }

        try {
            // ⚠️ 중요: 리시버만 체크하고, usbService는 체크하지 않음
            // USB 재연결 시 onServiceDisconnected에서 usbService가 null이 될 수 있지만,
            // 리시버가 등록되어 있으면 재바인딩만 시도하면 됨
            if (usbReceiverRegisted) {
                receivedMessageCnt = 0;
                sendingMessageCnt = 0;
                // 리시버는 이미 등록되어 있으므로, 서비스만 재바인딩 시도 (이미 바인딩되어 있으면 무시됨)
                if (usbService == null) {
                    logDebug(LogManager.LogCategory.US, "USB receiver registered but service is null, attempting rebind");
                    new Thread(() -> {
                        startService(UsbService.class, usbConnection, null);
                    }).start();
                } else {
                    logDebug(LogManager.LogCategory.US, "USB service already initialized, skipping onResume setup");
                }
                return;
            }

            cancelResetTimer();

            // 처음 초기화하는 경우
            new Thread(() -> {
                setFilters();  // Start listening notifications from UsbService
                startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
            }).start();
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, Constants.ErrorMessages.ON_RESUME_USB_SERVICE_ERROR, e);
        }

        // ========== PHASE 1: Start Managers ==========
        startManagers();
        // ========== End Manager Startup ==========
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);  // ⚠️ 중요: 새로운 Intent를 현재 Intent로 설정

        // USB 재연결 확인
        if (intent != null && "USB_RECONNECTED".equals(intent.getAction())) {
            logInfo(LogManager.LogCategory.US, "USB reconnected - onNewIntent called");

            // ⚠️ 플래그를 true로 설정 (onResume()에서 중복 실행 방지)
            isHandlingUsbReconnection = true;

            try {
                resetStateForUsbReconnect();
                // USB 재연결 처리 로직
                handleUsbReconnection();
            } finally {
                // ⚠️ 플래그를 false로 리셋 (예외 발생 시에도 리셋)
                isHandlingUsbReconnection = false;
            }
        }
    }

    /**
     * USB 재연결 시 처리하는 메서드
     * onNewIntent()에서 호출됨
     */
    private void handleUsbReconnection() {
        try {
            // ⚠️ 중요: USB 재연결 시에도 타이머가 자동으로 재시작되도록 함
            // usbService가 null이 아니고 타이머가 없으면 시작 (재연결 시에도 동작)
            if (usbService != null && (usbPollingFuture == null || usbPollingFuture.isCancelled())) {
                // Phase 2: 단순 UI 업데이트를 scheduleUiUpdate로 변경
                scheduleUiUpdate(() -> {
                    tvConnectPlcRamp.setBackgroundColor(getBaseContext().getResources().getColor(R.color.blue_01));
                    // tvConnectPlcRamp.setText(Constants.InitialValues.DEVICE_READY_CHARACTER);
                });

                System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> startUsbPolling 1");
                startUsbPolling(true);
                logInfo(LogManager.LogCategory.US, "USB polling restarted after reconnection");
            }
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, "Error in handleUsbReconnection", e);
        }
    }

    private void resetStateForUsbReconnect() {
        try {
            resetBluetoothSessionKeepUsb();
            disconnectCheckCount = 0;
            receivedMessageCnt = 0;
            sendingMessageCnt = 0;
        } catch (Exception e) {
            logError(LogManager.LogCategory.US, "Failed to reset state for USB reconnect", e);
        }
    }

    private void startResetTimer() {
        synchronized (finishedRestartTimerLock) {
            cancelResetTimer();
            resetCnt = 0;
            tmrReset = new Timer("UsbResetTimer");
            ttReset = new TimerTask() {
                @Override
                public void run() {
                    logInfo(LogManager.LogCategory.PS, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> resetCnt " + resetCnt + " usbReconnectAttempts " + usbReconnectAttempts);
                    if (resetCnt == 30) {
                        disconnectCheckCount = 0;
                        receivedMessageCnt = 0;
                        sendingMessageCnt = 0;
                        usbReconnectAttempts = 0;
                        resetCnt = 0;
                        receiveCommandEmptyCnt = 0;
                        resetBluetoothSessionKeepUsb();
                        cancelResetTimer();
                        return;
                    }
                    resetCnt++;
                }
            };
            tmrReset.schedule(ttReset, 0, 1000);
        }
    }

    private void cancelResetTimer() {
        synchronized (finishedRestartTimerLock) {
            if (tmrReset != null) {
                try {
                    disconnectCheckCount = 0;
                    receivedMessageCnt = 0;
                    sendingMessageCnt = 0;
                    usbReconnectAttempts = 0;
                    resetCnt = 0;
                    receiveCommandEmptyCnt = 0;
                    tmrReset.cancel();
                    tmrReset.purge();
                } catch (Exception ignored) {
                }
            }
            if (ttReset != null) {
                try {
                    ttReset.cancel();
                } catch (Exception ignored) {
                }
            }
            tmrReset = null;
            ttReset = null;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    // ⚠️ 키오스크 모드: BaseKioskActivity에서 onBackPressed, onWindowFocusChanged, onUserInteraction 자동 처리됨
    // KioskModeApplication의 ActivityLifecycleCallbacks가 onCreate, onResume 등에서도 자동 적용함

    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        // 기존 서비스가 연결돼 있다면 안전하게 해제 후 재시작
        boolean needsRestart = usbService == null || !UsbService.SERVICE_CONNECTED;
        if (!needsRestart) {
            logDebug(LogManager.LogCategory.US, "USB service already active; skipping restart");
            receivedMessageCnt = 0;
            sendingMessageCnt = 0;
            disconnectCheckCount = 0;
            return;
        }

        Intent startServiceIntent = new Intent(this, service);
        if (extras != null && !extras.isEmpty()) {
            Set<String> keys = extras.keySet();
            for (String key : keys) {
                String extra = extras.getString(key);
                startServiceIntent.putExtra(key, extra);
            }
        }
        startService(startServiceIntent);

        Intent bindingIntent = new Intent(this, service);
        try {
            bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            logDebug(LogManager.LogCategory.US, "USB service bind attempt: " + e.getMessage());
        }
    }

    private void setFilters() {
        try {
            if (usbReceiverRegisted) {
                return;
            }

            IntentFilter filter = new IntentFilter();
            filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
            filter.addAction(UsbService.ACTION_NO_USB);
            filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
            filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
            filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);

            // ⚠️ 중요: 리시버 중복 등록 예외 방지
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
                } else {
                    ContextCompat.registerReceiver(this, usbReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
                }
                usbReceiverRegisted = true;  // 등록 플래그 설정
            } catch (IllegalArgumentException e) {
                // 이미 등록되어 있는 경우 (예외 발생 시에도 플래그는 true로 설정)
                logDebug(LogManager.LogCategory.US, "USB receiver already registered: " + e.getMessage());
                usbReceiverRegisted = true;
            }
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, Constants.ErrorMessages.SET_FILTERS_ERROR, e);
            // ⚠️ 중요: 예외 발생 시에도 액티비티는 계속 실행되도록 함
        }
    }

    private class UsbHandler extends Handler {
        // private String dataBuffer = "";
        private final WeakReference<ActivityModelTestProcess> mActivity;

        public UsbHandler(ActivityModelTestProcess activity) {
            super(Looper.getMainLooper()); // ⚠️ 중요: 메인 스레드의 Looper를 명시적으로 지정 (없으면 메시지를 받을 수 없음)
            mActivity = new WeakReference<>(activity);
            System.out.println("▶ [US] UsbHandler 생성: Looper=" + getLooper() + ", Thread=" + Thread.currentThread().getName());
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UsbService.MESSAGE_FROM_SERIAL_PORT:
                    // System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> UsbHandler.handleMessage: MESSAGE_FROM_SERIAL_PORT 수신");
                    try {
                        String data = (String) msg.obj;
                        // System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> UsbHandler.handleMessage: data=" + data);
                        try {
                            dataBuffer = data;
                            // UsbService에서 이미 ACK/ETX를 제거한 데이터를 전달하므로 바로 사용
                            // dataBuffer = dataBufferTmp;
                            // logInfo(LogManager.LogCategory.US, "USB dataBuffer: " + dataBuffer);

                            // HTTP 통신 관련 runOnUiThread 최적화: scheduleUiUpdate 사용 (USB Handler 내부이지만 HTTP 통신 후 처리)
                            scheduleUiUpdate(() -> {
                                usbConnPermissionGranted = true;
                                if(tvConnectPlcRamp!=null) {
                                    tvConnectPlcRamp.setBackgroundColor(getBaseContext().getResources().getColor(R.color.blue_01));
                                }
                                // tvConnectPlcRamp.setText(Constants.InitialValues.DEVICE_READY_CHARACTER);
                            });

                            if (dataBuffer.contains(Constants.PLCCommands.RSS_RESPONSE_HEADER) && dataBuffer.length() >= 13) {
                                int s = dataBuffer.indexOf(Constants.PLCCommands.RSS_RESPONSE_HEADER) + Constants.PLCCommands.RSS_RESPONSE_HEADER.length();
                                int e = s + 4;
                                decElectricValue = Integer.parseInt(dataBuffer.substring(s, e), 16);
                                // System.out.println("> USB dataBuffer " + dataBuffer + " decElectricValue " + decElectricValue);
                                // logInfo(LogManager.LogCategory.US, String.format("USB dataBuffer: %s / decElectricValue: %.1f", dataBuffer, decElectricValue));

                                if (!usbReceiverRegisted) {
                                    usbReceiverRegisted = true;
                                }

                                lstWatt.add(decElectricValue);
                                recordWattMeasurement(currentTestItem, decElectricValue);

                                scheduleUiUpdate(() -> {
                                    if (wattValueAnimator != null) {
                                        wattValueAnimator.animateToValue(decElectricValue);
                                    } else if (tvWattValue != null) {
                                        // 폴백: 기존 방식
                                        tvWattValue.setText(String.format("%d", decElectricValue));
                                    }
                                });

                                // 응답 수신 알림 (명령 큐의 응답 대기 상태 해제)
                                if (usbCommandQueue != null) {
                                    usbCommandQueue.notifyResponseReceived();
                                }

                                // USB 응답 처리 (메모리 스캔 등에서 사용)
                                handleUsbResponse(dataBuffer);

                                dataBuffer = "";
                                dataBufferTmp = "";
                            } else {
                                // 다른 응답도 수신 알림
                                if (usbCommandQueue != null) {
                                    usbCommandQueue.notifyResponseReceived();
                                }

                                // USB 응답 처리
                                handleUsbResponse(dataBuffer);
                            }

                            dataBufferTmp = "";
//                            }

                            if (dataBufferTmp.length() > 60) {
                                dataBufferTmp = "";
                            }
                        } catch (Exception e) {
                            dataBuffer = "";
                            dataBufferTmp = "";
                        }
                    } catch (Exception e) {
                        logError(LogManager.LogCategory.ER, Constants.ErrorMessages.USB_HANDLER_MESSAGE_PROCESSING_ERROR, e);
                    }
                    break;
                case UsbService.CTS_CHANGE:
                    // Toast.makeText(mActivity.get(), Constants.LogMessages.CTS_CHANGE, Toast.LENGTH_LONG).show();
                    break;
                case UsbService.DSR_CHANGE:
                    // Toast.makeText(mActivity.get(), Constants.LogMessages.DSR_CHANGE, Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    public void readTemperatureExcel(String tableType, String fileName) {
        new Thread(() -> {
            try {
                try {
                    String tableName = (tableType.equals("1")) ? Constants.Common.TABLE_COLD_TEMPERATURE : Constants.Common.TABLE_HOT_TEMPERATURE;
                    InputStream is = getBaseContext().getResources().getAssets().open(fileName);
                    Workbook wb = Workbook.getWorkbook(is);

                    if (wb != null) {
                        Sheet sheet = wb.getSheet(Integer.parseInt(tableType) - 1);
                        if (sheet != null) {
                            int colTotal = sheet.getColumns();
                            int rowIndexStart = 1;
                            int rowTotal = sheet.getColumn(colTotal - 1).length;

                            List<Map<String, String>> lstTemperature = new ArrayList<Map<String, String>>();
                            Map<String, String> mapTemperature = null;

                            for (int row = rowIndexStart; row < rowTotal; row++) {
                                mapTemperature = new HashMap<String, String>();
                                mapTemperature.put(Constants.Common.CLM_TEMP_SEQ, String.valueOf(row));
                                mapTemperature.put(Constants.JsonKeys.CLM_TEMPERATURE, sheet.getCell(1, row).getContents());
                                mapTemperature.put(Constants.Common.CLM_REGIST, sheet.getCell(2, row).getContents());
                                mapTemperature.put(Constants.Common.CLM_VOLTAGE, sheet.getCell(3, row).getContents());
                                mapTemperature.put(Constants.Common.CLM_10_BIT, sheet.getCell(4, row).getContents());
                                mapTemperature.put(Constants.JsonKeys.CLM_12_BIT, sheet.getCell(6, row).getContents());
                                mapTemperature.put(Constants.JsonKeys.CLM_COMMENT, "");
                                lstTemperature.add(mapTemperature);
                            }

                            // WeakReference를 통한 안전한 Activity 접근
                            ActivityModelTestProcess activity = getMainActivity();
                            if (activity != null && !activity.isFinishing()) {
                                if (lstTemperature.size() != TestData.selectTemperatureData(activity, tableType).size()) {
                                    TestData.deleteTemperatureData(activity, tableType);
                                    for (int i = 0; i < lstTemperature.size(); i++) {
                                        TestData.insertTemperatureData(activity, tableType, lstTemperature.get(i));
                                    }
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    logError(LogManager.LogCategory.ER, Constants.ErrorMessages.READ_TEMPERATURE_EXCEL_IO_EXCEPTION, e);
                } catch (BiffException e) {
                    logError(LogManager.LogCategory.ER, Constants.ErrorMessages.READ_TEMPERATURE_EXCEL_BIFF_EXCEPTION, e);
                }
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, Constants.ErrorMessages.READ_TEMPERATURE_EXCEL_ERROR, e);
                // e.printStackTrace();
            }
        }).start();
    }

    /**
     * ⚠️ MEMORY LEAK FIX: Replaced non-static inner class RequestThreadBarcode with ExecutorService
     * <p>
     * BEFORE (MEMORY LEAK):
     * - Used non-static inner class RequestThreadBarcode extends Thread
     * - Non-static inner classes hold implicit reference to outer Activity
     * - If HTTP request takes 5-30 seconds and user exits → Activity leaked (5-50MB)
     * <p>
     * AFTER (NO LEAK):
     * - Uses btWorkerExecutor (ExecutorService) - already shutdown in onDestroy()
     * - No implicit Activity reference held
     * - Consistent with existing code patterns (used elsewhere in this file)
     */
    public void callBarcodeInfoServer() {
        // ⚠️ MEMORY LEAK FIX: Use btWorkerExecutor instead of creating new Thread
        btWorkerExecutor.execute(() -> {
            try {
                if (aTryingCnt == 5) {
                    aTryingCnt = 0;
                }
                // 동시성 문제 해결: connection을 로컬 변수로 변경 (각 스레드가 자신만의 connection 사용)
                HttpURLConnection connection = null;
                try {
                    urlStrBarcode = Constants.URLs.HTTP_PROTOCOL + serverIp + Constants.URLs.ENDPOINT_PRODUCT_SERIAL_INFO_LIST
                            + Constants.Common.QUESTION + Constants.URLs.PARAM_UNIT_NO + unit_no
                            + Constants.Common.AMPERSAND + Constants.URLs.PARAM_MODEL_ID + globalModelId;
                    System.out.println("><><><>>>>>>>>>>>>>>>>>>>> urlStrBarcode " + urlStrBarcode);
                    URL url = new URL(urlStrBarcode);
                    // Log.i(TAG, "▶ [SI] urlStrBarcode " + urlStrBarcode);
                    connection = (HttpURLConnection) url.openConnection();
                    if (connection != null) {
                        connection.setConnectTimeout(Constants.Timeouts.HTTP_CONNECT_TIMEOUT_MS);
                        connection.setReadTimeout(Constants.Timeouts.HTTP_READ_TIMEOUT_MS);
                        connection.setRequestMethod("GET");
                        connection.setDoInput(true);
                        connection.setDoOutput(true);

                        int resCode = connection.getResponseCode();
                        logInfo(LogManager.LogCategory.BI, String.format(Constants.LogMessages.PRODUCT_SERIAL_INFO_CALL_FORMAT, urlStrBarcode, resCode, (resCode == HttpURLConnection.HTTP_OK)));
                        if (resCode == HttpURLConnection.HTTP_OK) {
                            logInfo(LogManager.LogCategory.BI, String.format(Constants.LogMessages.PRODUCT_SERIAL_INFO_CALL_SUCCESS, resCode));
                            barcodeReadCheck = true;
                            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                            try {
                                String line = null;
                                String lineTmp = null;
                                StringBuilder sb = new StringBuilder();

                                // 무한 루프 방지를 위해 최대 읽기 횟수 제한
                                int maxReadCount = 10000;
                                int readCount = 0;

                                while (readCount < maxReadCount) {
                                    lineTmp = reader.readLine();
                                    if (lineTmp == null) {
                                        break; // 스트림 끝에 도달
                                    }
                                    readCount++;

                                    if (!lineTmp.trim().equals("")) {
                                        sb.append(lineTmp);
                                        line = lineTmp;
                                    }
                                }

                                // 모든 데이터를 읽은 후 한 번만 처리
                                final String data = (line != null && !line.trim().equals("")) ? line :
                                        (sb.length() > 0 ? sb.toString() : null);

                                if (data != null && !data.trim().equals("")) {
                                    logDebug(LogManager.LogCategory.BI, Constants.LogMessages.PRODUCT_SERIAL_INFO_CALL_DATA_RECEIVED);
                                    jsonParsingBarcode(Constants.JsonKeys.PRODUCT_SERIAL, data);
                                }
                            } finally {
                                try {
                                    reader.close();
                                } catch (IOException e) {
                                    logError(LogManager.LogCategory.ER, Constants.ErrorMessages.ERROR_CLOSING_READER_IN_REQUEST_THREAD_BARCODE, e);
                                }
                            }
                        }

                        if (resCode != HttpURLConnection.HTTP_OK) {
                            logWarn(LogManager.LogCategory.BI, String.format(Constants.LogMessages.PRODUCT_SERIAL_INFO_CALL_FAILED, resCode));
                        }
                    }
                } finally {
                    // 리소스 정리 보장
                    safeDisconnectConnection(connection);
                }
            } catch (Exception e) { //예외 처리
                logError(LogManager.LogCategory.ER, Constants.ErrorMessages.REQUEST_THREAD_BARCODE_ERROR, e);
            }
        });
    }

    // ⚠️ MEMORY LEAK FIX: Deleted RequestThreadBarcode class (was non-static inner class)
    // The logic has been moved to callBarcodeInfoServer() using btWorkerExecutor pattern

    public void jsonParsingBarcode(String data_type, String json) {
        new Thread(() -> {
            try {
                logInfo(LogManager.LogCategory.BI, "jsonParsingBarcode 응답: " + json);
                JSONObject jsonObject = new JSONObject(json);
                JSONArray testItemArray = jsonObject.getJSONArray(Constants.JsonKeys.PRODUCT_SERIAL);

                List<Map<String, String>> lstData = new ArrayList<Map<String, String>>();
                Map<String, String> mapData = null;
                logDebug(LogManager.LogCategory.SI, Constants.LogMessages.JSON_PARSING_BARCODE_RECEIVED);

                if (data_type.equals(Constants.JsonKeys.PRODUCT_SERIAL)) {
                    logDebug(LogManager.LogCategory.SI, "testItemArray.length(): " + testItemArray.length());
                    for (int i = 0; i < testItemArray.length(); i++) {
                        JSONObject testItemObject = testItemArray.getJSONObject(i);
                        mapData = new HashMap<String, String>();
                        mapData.put(Constants.JsonKeys.CLM_PRODUCT_SERIAL_NO, testItemObject.getString(Constants.JsonKeys.CLM_PRODUCT_SERIAL_NO));
                        lstData.add(mapData);
                    }

                    logDebug(LogManager.LogCategory.SI, "lstData.size(): " + lstData.size());
                    for (int i = 0; i < lstData.size(); i++) {
                        try {
                            productSerialNo = lstData.get(i).get(Constants.JsonKeys.CLM_PRODUCT_SERIAL_NO);
                            logInfo(LogManager.LogCategory.BI, String.format(Constants.LogMessages.PRODUCT_SERIAL_INFO_CALL, i, productSerialNo));
//                            Log.i(TAG, "▶ productSerialNo(" + i + ") " + productSerialNo);
                        } catch (Exception e) {
                            logError(LogManager.LogCategory.ER, Constants.ErrorMessages.PRODUCT_SERIAL_PARSING_ERROR, e);
                        }
                    }

                    // HTTP 통신 관련 runOnUiThread 최적화: 바코드 요청 후 UI 업데이트를 scheduleUiUpdate로 변경
                    scheduleUiUpdate(() -> {
                        logInfo(LogManager.LogCategory.SI, String.format("globalProductSerialNo: %s, productSerialNo: %s", globalProductSerialNo, productSerialNo));
                        tvSerialNo.setText(productSerialNo);
                        tv_dialog_barcode.setText(productSerialNo);
                        blnBarcodeReceived = true;
                        aTryingCnt = 0;
//                            barcodeRequestTimer.cancel();

                        SharedPreferences test = getSharedPreferences(Constants.SharedPrefKeys.TEST_COOKIE_INFO, MODE_PRIVATE);
                        globalProductSerialNo = test.getString(Constants.SharedPrefKeys.TEST_PRODUCT_SERIAL_NO, "");
                        logInfo(LogManager.LogCategory.SI, String.format("globalProductSerialNo: %s, productSerialNo: %s", globalProductSerialNo, productSerialNo));
                    });

                    test_info.putString(Constants.JsonKeys.CLM_PRODUCT_SERIAL_NO, productSerialNo);
                    test_info.commit();
                    getPreferences();
                }
            } catch (JSONException e) {
                logError(LogManager.LogCategory.ER, Constants.ErrorMessages.JSON_PARSING_BARCODE_JSON_EXCEPTION, e);
            }
        }).start();
    }

    /**
     * 모든 리소스를 안전하게 정리하는 통합 메소드
     * Activity 종료 시 반드시 호출되어야 함
     */
    private void cleanupAllResources() {
        try {
            // 1. 모든 Timer 정리
            cleanupAllTimers();

            // 2. Handler 메시지 큐 정리
            clearHttpHandlerQueue();
            clearBtHandlerQueue();
            if (mainHandler != null) {
                // Stop resourceInfoRunnable before clearing handler
                if (resourceInfoRunnable != null) {
                    mainHandler.removeCallbacks(resourceInfoRunnable);
                    resourceInfoRunnable = null;
                }
                mainHandler.removeCallbacksAndMessages(null);
            }
            if (btHandler != null) {
                btHandler.removeCallbacksAndMessages(null);
            }
            // ⚠️ CRITICAL: Clean up the three missing handlers to prevent memory leaks
            if (usbRecoveryHandler != null) {
                usbRecoveryHandler.removeCallbacksAndMessages(null);
            }
            if (usbReconnectHandler != null) {
                usbReconnectHandler.removeCallbacksAndMessages(null);
            }
            if (schedulerHandler != null) {
                schedulerHandler.removeCallbacksAndMessages(null);
            }

            // 3. USB 관련 리소스 정리
            cleanupUsbResources();

            // 4. Bluetooth 관련 리소스 정리
            cleanupBluetoothResources();

            // 5. AsyncTask 정리
            cleanupAsyncTasks();

            // 6. HTTP 연결 정리
            cleanupHttpConnections();

            // 7. 로그 배치 큐 정리
            clearLogBatchQueue();

            // 8. WeakReference 정리
            clearMainActivityReference();

            // 9. SharedPreferences Editor 정리
            if (test_info != null) {
                test_info.apply(); // 또는 commit()
            }
            if (cookie_info != null) {
                cookie_info.apply(); // 또는 commit()
            }

            logInfo(LogManager.LogCategory.PS, "All resources cleaned up successfully");
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, "Error cleaning up resources", e);
        }
    }

    private void resetActivityState() {
        try {
            logInfo(LogManager.LogCategory.PS, "Resetting ActivityModel_0002 state");

            // 기본 플래그 및 카운터
            entranceCheck = false;
            barcodeReadCheck = false;
            testProcessStarted = false;
            finishedCorrectly = false;
            shouldUpdateDialog = false;
            btPermissionsGranted = false;
            permissionRequestInProgress = false;
            permissionDialogShowing = false;
            btConnectionInProgress = false;
            btConnected = false;
            isConnected = false;
            btSearchOnOff = false;
            blnBarcodeReceived = false;
            btMessageTimerRunning.set(false);
            unfinishedRestartTimerRunning.set(false);
            finishedRestartTimerRunning.set(false);
            remoteCommandTimerRunning.set(false);

            // USB 상태
            usbConnTryCnt = 0;
            usbConnPermissionGranted = false;
            usbPollingRequested = false;
            usbPollingEnabled = false;
            usbPollingFailureCount = 0;
            isUsbReconnecting = false;
            usbReconnectAttempts = 0;
            usbPollingFuture = null;

            // ⚠️ IMPORTANT: Remove Runnables from handlers before nullifying
            if (usbRecoveryHandler != null && usbPermissionRecoveryRunnable != null) {
                usbRecoveryHandler.removeCallbacks(usbPermissionRecoveryRunnable);
            }
            usbPermissionRecoveryRunnable = null;

            if (usbReconnectHandler != null && usbReconnectRunnable != null) {
                usbReconnectHandler.removeCallbacks(usbReconnectRunnable);
            }
            usbReconnectRunnable = null;

            usbService = null;
            usbConnection = null;

            // Bluetooth 상태
            btHandler = null;
            btConnectedThread = null;
            btSocket = null;
            deviceSelected = null;
            btDeviceName = "";
            btDeviceAddr = "";
            btInfoList = null;

            // 타이머/쓰레드 참조
            tmrBTMessageSend = null;
            ttBTMessageSend = null;
            tmrFinishedRestart = null;
            ttFinishedRestart = null;
            tmrUnfinishedRestart = null;
            ttUnfinishedRestart = null;
            appResetTimerTask = null;
            barcodeRequestTimer = null;
            testTaskThread = null;

            // ⚠️ IMPORTANT: Remove Runnables from handlers before nullifying
            if (schedulerHandler != null && pendingHttpTask != null) {
                schedulerHandler.removeCallbacks(pendingHttpTask);
            }
            pendingHttpTask = null;

            if (schedulerHandler != null && btReconnectRunnable != null) {
                schedulerHandler.removeCallbacks(btReconnectRunnable);
            }
            btReconnectRunnable = null;

            if (schedulerHandler != null && btConnectionIndicatorRunnable != null) {
                schedulerHandler.removeCallbacks(btConnectionIndicatorRunnable);
            }
            btConnectionIndicatorRunnable = null;

            // 데이터 및 컬렉션
            if (lstTestResult != null) {
                lstTestResult.clear();
                lstTestResult = null;
            }
            lstTestTemperature = null;
            mapTestTemperature = null;
            lstData = null;
            lstDataTmp = null;
            temperatureData = null;
            coldTemperatureData = null;
            hotTemperatureData = null;
            listItemAdapter = null;
            specCache.clear();
            latestWattByCommand.clear();
            if (pendingUiUpdates != null) {
                synchronized (uiUpdateLock) {
                    pendingUiUpdates.clear();
                    uiUpdateBatchTask = null;
                }
            }

            // 문자열/기타 값 초기화
            log_text = "";
            log_text_param = "";
            productSerialNo = "";
            testProcessId = "";
            readMessage = null;
            receiveCommand = "";
            receiveCommandResponse = "";
            receiveCommandResponseOK = "";
            receiveCompAgingResponse = Constants.InitialValues.RECEIVE_COMP_AGING_RESPONSE;
            receiveResponseResult = Constants.InitialValues.RECEIVE_RESPONSE_RESULT;
            resultInfo = Constants.Common.EMPTY_STRING;
            currentTestItem = Constants.InitialValues.CURRENT_TEST_ITEM;
            valueWatt = Constants.InitialValues.VALUE_WATT;
            lowerValueWatt = Constants.InitialValues.LOWER_VALUE_WATT;
            upperValueWatt = Constants.InitialValues.UPPER_VALUE_WATT;
            currentPumpWattValueArr = "";
            urlStr = "";
            urlTestTaskStr = "";
            urlStrBarcode = "";
            serverIp = "";
            serverDomain = "";
            serverResetIp = "";
            btDeviceName = "";
            btDeviceAddr = "";
            mode_type = Constants.InitialValues.MODE_TYPE;

            // 카운터 초기화
            testOkCnt = 0;
            testNgCnt = 0;
            totalTimeCnt = 0;
            testItemIdx = 0;
            testItemCounter = 0;
            testTotalCounter = 0;
            test_version_id = "";
            model_id = "";
            restartCntFinished = 0;
            restartCntUnfinished = 0;
            restartCntMargin = 0;
            sendingMessageCnt = 0;
            receivedMessageCnt = 0;
            disconnectCheckCount = 0;
            sensorNgCount = 0;
            wattLower = 0;
            wattUpper = 0;

            // 배열/맵 초기화
            arrTestItems = null;
            arrTestItemsZig = null;
            mapTestTemperature = null;

            // ⚠️ IMPORTANT: Remove listeners before nullifying views to prevent memory leaks
            if (fab_close != null) {
                fab_close.setOnClickListener(null);
            }
            if (btnAlertClose != null) {
                btnAlertClose.setOnClickListener(null);
            }
            if (btnTestResultClose != null) {
                btnTestResultClose.setOnClickListener(null);
            }
            if (btnTestRestart != null) {
                btnTestRestart.setOnClickListener(null);
                btnTestRestart.setOnTouchListener(null);  // ⚠️ CRITICAL: Remove touch listener
            }
            if (btnTestHistoryList != null) {
                btnTestHistoryList.setOnClickListener(null);
            }

            // UI 상태
            clAlert = null;
            clDialogForPreprocess = null;
            clTestResult = null;
            cl_dialog_for_logger = null;
            cl_log = null;

            tvAlertMessage = null;
            tvDialogMessage = null;
            tvUnitId = null;
            tvPopupProcessResult = null;
            tvPopupProcessResultCompValue = null;
            tvPopupProcessResultHeaterValue = null;
            tvCompWattValue = null;
            tvHeaterWattValue = null;
            tvPumpWattValue = null;
            tvCompValueWatt = null;
            tvHeaterValueWatt = null;
            tvPumpValueWatt = null;
            tvCompLowerValueWatt = null;
            tvCompUpperValueWatt = null;
            tvPumpLowerValueWatt = null;
            tvPumpUpperValueWatt = null;
            tvHeaterLowerValueWatt = null;
            tvHeaterUpperValueWatt = null;
            tvEllapsedTimeCnt = null;
            tvModelName = null;
            tvModelNationality = null;
            tvSerialNo = null;
            tvModelId = null;
            tv_dialog_barcode = null;
            tv_dialog_title = null;
            tvCurrentProcess = null;
            tvConnectBtRamp = null;
            tvConnectPlcRamp = null;
            tvRunWsRamp = null;
            tvTestOkCnt = null;
            tvTestNgCnt = null;
            tvWattValue = null;
            tv_log = null;
            tvResourceInfo = null;
            mBluetoothStatus = null;
            mReadBuffer = null;
            mReadText = null;
            tv_current_version = null;

            btnAlertClose = null;
            btnTestResultClose = null;
            btnTestRestart = null;
            btnTestHistoryList = null;
            lvTestItem = null;

            binding = null;

            logInfo(LogManager.LogCategory.PS, "Activity state reset completed");
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, "Failed to reset Activity state", e);
        }
    }

    /**
     * USB 관련 리소스 정리
     */
    private void cleanupUsbResources() {
        try {
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> stopUsbPolling 9");
            stopUsbPolling();

            // USB Receiver 해제 (예외 처리 포함)
            if (usbReceiverRegisted) {
                try {
                    unregisterReceiver(usbReceiver);
                } catch (IllegalArgumentException e) {
                    // 이미 해제된 경우 (정상 상황)
                    logDebug(LogManager.LogCategory.US, "USB receiver already unregistered");
                }
                usbReceiverRegisted = false;
            }

            // USB 서비스 바인딩 해제
            if (usbConnPermissionGranted) {
                try {
                    unbindService(usbConnection);
                } catch (IllegalArgumentException e) {
                    // 이미 해제된 경우
                    logDebug(LogManager.LogCategory.US, "USB service already unbound");
                }
                usbConnPermissionGranted = false;
            }

            // USB 서비스 중지 (선택적)
            if (usbService != null) {
                try {
                    Intent usbIntent = new Intent(this, UsbService.class);
                    stopService(usbIntent);
                } catch (Exception e) {
                    logWarn(LogManager.LogCategory.US, "Error stopping USB service: " + e.getMessage());
                }
                usbService = null;
            }

            // ⚠️ IMPORTANT: Reset SERVICE_CONNECTED flag to ensure proper reconnection
            UsbService.SERVICE_CONNECTED = false;

            // ⚠️ IMPORTANT: Nullify ServiceConnection to prevent memory leak
            usbConnection = null;
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, "Error cleaning up USB resources", e);
        }
    }

    /**
     * Bluetooth 관련 리소스 정리
     */
    private void cleanupBluetoothResources() {
        try {
            // DialogManager를 통해 모든 다이얼로그 정리
            if (dialogManager != null) {
                dialogManager.cleanup();
            }

            // Bluetooth ConnectedThread 정리
            if (btConnectedThread != null) {
                try {
                    btConnectedThread.cancel(); // ConnectedThreadOptimized에 cancel() 메소드가 있다고 가정
                } catch (Exception e) {
                    logWarn(LogManager.LogCategory.BT, "Error canceling Bluetooth thread: " + e.getMessage());
                }
                btConnectedThread = null;
            }

            clearBluetoothReconnect();
            btConnectionInProgress = false;
            btReconnectDelayMs = Constants.Timeouts.BT_LIST_TIMER_INTERVAL_MS;

            // Bluetooth Socket 정리
            if (btSocket != null && btSocket.isConnected()) {
                try {
                    btSocket.close();
                } catch (IOException e) {
                    logWarn(LogManager.LogCategory.BT, "Error closing Bluetooth socket: " + e.getMessage());
                }
            }
            btSocket = null;

            // Bluetooth Adapter 정리
            cancelDiscoverySafe();

            // Bluetooth 관련 변수 초기화
            deviceSelected = null;
            btSearchOnOff = false;
            btConnected = false;
            isConnected = false;

        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, "Error cleaning up Bluetooth resources", e);
        }
    }

    /**
     * AsyncTask 정리
     */
    private void cleanupAsyncTasks() {
        try {
            // RequestThreadAsync 정리
            // AsyncTask는 인스턴스 변수로 관리되지 않으므로,
            // 실행 중인 경우에만 cancel() 호출 가능
            // 주의: AsyncTask는 한 번만 실행 가능하므로 재사용 불가

            // RequestTestTaskThreadAsync 정리
            if (testTaskThread != null) {
                try {
                    testTaskThread.cancel(true);
                } catch (Exception e) {
                    logWarn(LogManager.LogCategory.PS, "Error canceling test task thread: " + e.getMessage());
                }
                testTaskThread = null;
            }
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, "Error cleaning up AsyncTasks", e);
        }
    }

    /**
     * HTTP 연결 정리
     */
    private void cleanupHttpConnections() {
        try {
            // Static connection 정리
            safeDisconnectConnection(connection);
            connection = null;

            // 로컬 connection들은 각 메소드에서 finally 블록으로 정리됨
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, "Error cleaning up HTTP connections", e);
        }
    }

    // ==================== 키오스크 모드 관련 메소드 ====================

    /**
     * 완전한 키오스크 모드 설정
     * 시스템 UI 숨기기, 화면 항상 켜두기, 화면 회전 고정 등을 포함
     */
    private void enableFullKioskMode() {
        try {
            // 1. 시스템 UI 숨기기
            setupKioskMode();

            // 2. 화면 항상 켜두기
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            // 3. 화면 회전 고정 (가로 모드)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

            // 4. Screen Pinning 활성화 (선택적 - 필요시 주석 해제)
            // enableScreenPinning();

            logInfo(LogManager.LogCategory.PS, "Full kiosk mode enabled");
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, "Error enabling full kiosk mode", e);
        }
    }

    /**
     * 키오스크 모드 설정 (시스템 UI 완전히 숨기기)
     * Android 버전에 따라 다른 API 사용
     * ⚠️ 중요: 시스템 UI가 나타나지 않도록 지속적으로 모니터링
     */
    private void setupKioskMode() {
        try {
            // Android 11 (API 30) 이상
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                getWindow().setDecorFitsSystemWindows(false);
                WindowInsetsController controller = getWindow().getInsetsController();
                if (controller != null) {
                    // 상태바와 내비게이션 바 숨기기
                    controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                    // ⚠️ 중요: BEHAVIOR_DEFAULT 사용 (스와이프로 나타나지 않도록)
                    // BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE는 스와이프로 나타나게 함
                    controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_DEFAULT);
                }
            } else {
                // Android 10 이하
                View decorView = getWindow().getDecorView();
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
                                    setupKioskMode();
                                }
                            }
                        }
                );
            }

            // ⚠️ 중요: 이벤트 기반으로 즉시 시스템 UI 숨기기
            hideSystemUI();
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, "Error setting up kiosk mode", e);
        }
    }

    private long lastHideSystemUITime = 0;
    private static final long HIDE_SYSTEM_UI_DEBOUNCE_MS = 100;

    /**
     * 키오스크 모드 지속적 모니터링 시작
     * 주기적으로 시스템 UI를 숨겨서 사용자가 접근할 수 없도록 함
     */
    /**
     * 시스템 UI 강제로 숨기기
     * 모든 Android 버전에서 작동하는 강력한 방법
     */
    public void hideSystemUI() {
        try {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastHideSystemUITime < HIDE_SYSTEM_UI_DEBOUNCE_MS) {
                return; // 너무 자주 호출되는 경우 스킵
            }
            lastHideSystemUITime = currentTime;

            View decorView = getWindow().getDecorView();

            // Android 11 (API 30) 이상
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsetsController controller = getWindow().getInsetsController();
                if (controller != null) {
                    controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
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
    private void enableScreenPinning() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // ⚠️ 중요: isLockTaskModeSupported()는 존재하지 않는 메소드입니다
                // Android 6.0 (API 23) 이상에서는 getLockTaskModeState()로 상태 확인 가능
                // 하지만 직접 startLockTask()를 호출하고 예외 처리하는 것이 더 안전합니다

                // Android 6.0 이상에서는 상태 확인 후 호출
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                    if (activityManager != null) {
                        int lockTaskMode = activityManager.getLockTaskModeState();
                        // 이미 Lock Task 모드가 아닌 경우에만 시작
                        if (lockTaskMode == ActivityManager.LOCK_TASK_MODE_NONE) {
                            startLockTask();
                            logInfo(LogManager.LogCategory.PS, "Screen pinning enabled");
                        } else {
                            logDebug(LogManager.LogCategory.PS, "Screen pinning already active (mode: " + lockTaskMode + ")");
                        }
                    }
                } else {
                    // Android 5.0-5.1: 직접 호출 (예외 처리로 안전하게)
                    startLockTask();
                    logInfo(LogManager.LogCategory.PS, "Screen pinning enabled");
                }
            }
        } catch (SecurityException e) {
            // Lock Task 모드가 지원되지 않거나 권한이 없는 경우
            logWarn(LogManager.LogCategory.PS, "Lock task mode not supported or permission denied: " + e.getMessage());
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, "Error enabling screen pinning", e);
        }
    }

    /**
     * Screen Pinning 비활성화
     */
    private void disableScreenPinning() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                stopLockTask();
                logInfo(LogManager.LogCategory.PS, "Screen pinning disabled");
            }
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, "Error disabling screen pinning", e);
        }
    }

    private void applyUiBundle(UiUpdateBundle bundle) {
        if (bundle == null) {
            return;
        }

        if (bundle.dialogVisible) {
            if (clDialogForPreprocess.getVisibility() != VISIBLE) {
                clDialogForPreprocess.setVisibility(VISIBLE);
            }
            tvDialogMessage.setText(bundle.dialogMessage);
            trPreprocessContent.setBackgroundColor(bundle.dialogColor);
        } else if (bundle.dialogHidden) {
            clDialogForPreprocess.setVisibility(INVISIBLE);
            tvDialogMessage.setText(Constants.Common.EMPTY_STRING);
        }

        if (!TextUtils.isEmpty(bundle.currentProcessName)) {
            tvCurrentProcess.setText(bundle.currentProcessName);
        }

        if (!TextUtils.isEmpty(bundle.temperatureText)) {
            tvTemperature.setText(bundle.temperatureText);
        }

        if (!TextUtils.isEmpty(bundle.compWattText)) {
            tvCompWattValue.setText(bundle.compWattText);
            tvPopupProcessResultCompValue.setText(bundle.compWattText);
        }

        if (!TextUtils.isEmpty(bundle.heaterWattText)) {
            tvHeaterWattValue.setText(bundle.heaterWattText);
            tvPopupProcessResultHeaterValue.setText(bundle.heaterWattText);
        }

        if (!TextUtils.isEmpty(bundle.pumpWattText)) {
            tvPumpWattValue.setText(bundle.pumpWattText);
        }

        if (!TextUtils.isEmpty(bundle.logText)) {
            tv_log.setText(bundle.logText);
        }

        if (bundle.updateListAdapter && bundle.listItemAdapter != null && !TextUtils.isEmpty(bundle.updateItemCommand)) {
            boolean itemUpdated = false;
            for (int i = 0; i < bundle.listItemAdapter.getCount(); i++) {
                VoTestItem item = (VoTestItem) bundle.listItemAdapter.getItem(i);
                if (!bundle.updateItemCommand.equals(item.getTest_item_command())) {
                    continue;
                }

                itemUpdated = true;

                if (bundle.receiveCommandResponseOK != null && bundle.receiveCommandResponseOK.equals(bundle.updateItemCommand) && bundle.updateItemResult.equals(Constants.ResultStatus.NG)) {
                    // placeholder for NG specific logging
                }
                if (Constants.TestItemCodes.CM0100.equals(item.getTest_item_command())) {
                    item.setTest_item_name(item.getTest_item_name() + Constants.Common.LOGGER_DEVIDER_01 + bundle.updateItemNameSuffix);
                }
                if (bundle.updateItemCommand.contains(Constants.TestItemCodes.CM0101)) {
                    item.setTest_item_info(bundle.temperatureValueCompDiff);
                }
                if (bundle.updateItemCommand.contains(Constants.TestItemCodes.HT0101) ||
                        bundle.updateItemCommand.contains(Constants.TestItemCodes.PM0101) ||
                        bundle.updateItemCommand.contains(Constants.TestItemCodes.SV0101) ||
                        bundle.updateItemCommand.contains(Constants.TestItemCodes.SV0201) ||
                        bundle.updateItemCommand.contains(Constants.TestItemCodes.SV0301) ||
                        bundle.updateItemCommand.contains(Constants.TestItemCodes.SV0401)) {
                    item.setTest_item_info(bundle.resultInfo);
                }
                if (bundle.updateItemCommand.contains(Constants.TestItemCodes.TH0101)) {
                    item.setTest_item_info(bundle.decTemperatureHotValue);
                }
                if (bundle.updateItemCommand.contains(Constants.TestItemCodes.TH0201)) {
                    item.setTest_item_info(bundle.decTemperatureColdValue);
                }
                item.setTest_result_check_value(bundle.updateItemCheckValue);
                item.setTest_item_result(bundle.updateItemResult);
                item.setTest_finish_yn(Constants.ResultStatus.YES);
                if (bundle.finalReadMessage != null) {
                    item.setTest_bt_raw_message(bundle.finalReadMessage.substring(bundle.finalReadMessage.indexOf(Constants.CharCodes.STX) + 1, bundle.finalReadMessage.indexOf(Constants.CharCodes.ETX)));
                }
                if (bundle.finalReceiveCommandResponse != null) {
                    item.setTest_bt_raw_response(bundle.finalReceiveCommandResponse);
                }
                if (!TextUtils.isEmpty(bundle.finalCalculatedResultValue)) {
                    item.setTest_bt_processed_value(bundle.finalCalculatedResultValue);
                }
            }

            if (bundle.shouldUpdateCounts && itemUpdated) {
                recalcTestCountsFromAdapter(bundle.listItemAdapter);
            }

            bundle.listItemAdapter.updateListAdapter();

            // 제어 모드 검사 실행 중이고 검사 항목이 업데이트되었으면 검사 결과 정보 저장
            if (isControlMode && isControlOn && itemUpdated) {
                synchronized (controlTestTimerLock) {
                    if (controlTestTimerRunning.get() && controlTestItemIdx >= 0 &&
                            bundle.updateItemCommand != null && bundle.updateItemCommand.equals(controlCurrentTestItem)) {
                        // 제어 모드 검사 수신 정보 업데이트
                        controlTestReceiveCommand = bundle.updateItemCommand;
                        controlTestReceiveResponse = bundle.finalReceiveCommandResponse;

                        // 검사 결과 값 저장 (소비전력 또는 온도)
                        String checkValue = bundle.updateItemCheckValue;
                        if (checkValue != null && !checkValue.isEmpty()) {
                            controlTestResultValue = checkValue;
                        } else if (bundle.resultInfo != null && !bundle.resultInfo.isEmpty()) {
                            controlTestResultValue = bundle.resultInfo;
                        } else {
                            // 소비전력 또는 온도 정보 확인
                            if (bundle.compWattText != null && !bundle.compWattText.isEmpty()) {
                                controlTestResultValue = bundle.compWattText;
                            } else if (bundle.heaterWattText != null && !bundle.heaterWattText.isEmpty()) {
                                controlTestResultValue = bundle.heaterWattText;
                            } else if (bundle.pumpWattText != null && !bundle.pumpWattText.isEmpty()) {
                                controlTestResultValue = bundle.pumpWattText;
                            } else if (bundle.temperatureText != null && !bundle.temperatureText.isEmpty()) {
                                controlTestResultValue = bundle.temperatureText;
                            } else if (bundle.finalCalculatedResultValue != null && !bundle.finalCalculatedResultValue.isEmpty()) {
                                controlTestResultValue = bundle.finalCalculatedResultValue;
                            }
                        }

                        controlTestResult = bundle.updateItemResult;
                    }
                }
            }
        }

        if (bundle.finalCurrentTestItem != null && bundle.finalCurrentTestItem.contains(Constants.TestItemCodes.SN0101)) {
            // reserved for additional logic
        }
    }

    private void recalcTestCountsFromAdapter(ItemAdapterTestItem adapter) {
        if (adapter == null) {
            return;
        }

        int calculatedOk = 0;
        int calculatedNg = 0;
        for (int i = 0; i < adapter.getCount(); i++) {
            VoTestItem item = (VoTestItem) adapter.getItem(i);
            String result = item.getTest_item_result();
            switch (result) {
                case Constants.ResultStatus.OK:
                    calculatedOk++;
                    break;
                case Constants.ResultStatus.NG:
                    calculatedNg++;
                    break;
            }
        }

        testOkCnt = calculatedOk;
        testNgCnt = calculatedNg;
        tvTestOkCnt.setText(String.valueOf(calculatedOk));
        tvTestNgCnt.setText(String.valueOf(calculatedNg));
    }

    private static class UiUpdateBundle {
        final boolean dialogVisible;
        final boolean dialogHidden;
        final int dialogColor;
        final String dialogMessage;
        final String temperatureText;
        final String compWattText;
        final String heaterWattText;
        final String pumpWattText;
        final String logText;
        final String updateItemCommand;
        final String updateItemResult;
        final String updateItemCheckValue;
        final String updateItemInfo;
        final String updateItemNameSuffix;
        final boolean updateListAdapter;
        final String finalReceiveCommandResponse;
        final String finalCalculatedResultValue;
        final String finalReadMessage;
        final String temperatureValueCompDiff;
        final String resultInfo;
        final String decTemperatureHotValue;
        final String decTemperatureColdValue;
        final String finalCurrentTestItem;
        final int testItemIdx;
        final int testOkCnt;
        final int testNgCnt;
        final String receiveCommandResponseOK;
        final boolean shouldUpdateCounts;
        final ItemAdapterTestItem listItemAdapter;
        final String currentProcessName;
        final int receivedMessageCnt;

        private UiUpdateBundle(Builder builder) {
            this.dialogVisible = builder.dialogVisible;
            this.dialogHidden = builder.dialogHidden;
            this.dialogColor = builder.dialogColor;
            this.dialogMessage = builder.dialogMessage;
            this.temperatureText = builder.temperatureText;
            this.compWattText = builder.compWattText;
            this.heaterWattText = builder.heaterWattText;
            this.pumpWattText = builder.pumpWattText;
            this.logText = builder.logText;
            this.updateItemCommand = builder.updateItemCommand;
            this.updateItemResult = builder.updateItemResult;
            this.updateItemCheckValue = builder.updateItemCheckValue;
            this.updateItemInfo = builder.updateItemInfo;
            this.updateItemNameSuffix = builder.updateItemNameSuffix;
            this.updateListAdapter = builder.updateListAdapter;
            this.finalReceiveCommandResponse = builder.finalReceiveCommandResponse;
            this.finalCalculatedResultValue = builder.finalCalculatedResultValue;
            this.finalReadMessage = builder.finalReadMessage;
            this.temperatureValueCompDiff = builder.temperatureValueCompDiff;
            this.resultInfo = builder.resultInfo;
            this.decTemperatureHotValue = builder.decTemperatureHotValue;
            this.decTemperatureColdValue = builder.decTemperatureColdValue;
            this.finalCurrentTestItem = builder.finalCurrentTestItem;
            this.testItemIdx = builder.testItemIdx;
            this.testOkCnt = builder.testOkCnt;
            this.testNgCnt = builder.testNgCnt;
            this.receiveCommandResponseOK = builder.receiveCommandResponseOK;
            this.shouldUpdateCounts = builder.shouldUpdateCounts;
            this.listItemAdapter = builder.listItemAdapter;
            this.currentProcessName = builder.currentProcessName;
            this.receivedMessageCnt = builder.receivedMessageCnt;
        }

        static class Builder {
            private boolean dialogVisible;
            private boolean dialogHidden;
            private int dialogColor;
            private String dialogMessage;
            private String temperatureText;
            private String compWattText;
            private String heaterWattText;
            private String pumpWattText;
            private String logText;
            private String updateItemCommand = Constants.Common.EMPTY_STRING;
            private String updateItemResult = Constants.Common.EMPTY_STRING;
            private String updateItemCheckValue = Constants.Common.EMPTY_STRING;
            private String updateItemInfo = Constants.Common.EMPTY_STRING;
            private String updateItemNameSuffix = Constants.Common.EMPTY_STRING;
            private boolean updateListAdapter;
            private String finalReceiveCommandResponse;
            private String finalCalculatedResultValue;
            private String finalReadMessage;
            private String temperatureValueCompDiff;
            private String resultInfo;
            private String decTemperatureHotValue;
            private String decTemperatureColdValue;
            private String finalCurrentTestItem;
            private int testItemIdx;
            private int testOkCnt;
            private int testNgCnt;
            private String receiveCommandResponseOK;
            private boolean shouldUpdateCounts;
            private ItemAdapterTestItem listItemAdapter;
            private String currentProcessName;
            private int receivedMessageCnt;

            Builder setDialogVisible(boolean value) {
                this.dialogVisible = value;
                return this;
            }

            Builder setDialogHidden(boolean value) {
                this.dialogHidden = value;
                return this;
            }

            Builder setDialogColor(int value) {
                this.dialogColor = value;
                return this;
            }

            Builder setDialogMessage(String value) {
                this.dialogMessage = value;
                return this;
            }

            Builder setTemperatureText(String value) {
                this.temperatureText = value;
                return this;
            }

            Builder setCompWattText(String value) {
                this.compWattText = value;
                return this;
            }

            Builder setHeaterWattText(String value) {
                this.heaterWattText = value;
                return this;
            }

            Builder setPumpWattText(String value) {
                this.pumpWattText = value;
                return this;
            }

            Builder setLogText(String value) {
                this.logText = value;
                return this;
            }

            Builder setUpdateItemCommand(String value) {
                this.updateItemCommand = value == null ? Constants.Common.EMPTY_STRING : value;
                return this;
            }

            Builder setUpdateItemResult(String value) {
                this.updateItemResult = value == null ? Constants.Common.EMPTY_STRING : value;
                return this;
            }

            Builder setUpdateItemCheckValue(String value) {
                this.updateItemCheckValue = value == null ? Constants.Common.EMPTY_STRING : value;
                return this;
            }

            Builder setUpdateItemInfo(String value) {
                this.updateItemInfo = value == null ? Constants.Common.EMPTY_STRING : value;
                return this;
            }

            Builder setUpdateItemNameSuffix(String value) {
                this.updateItemNameSuffix = value == null ? Constants.Common.EMPTY_STRING : value;
                return this;
            }

            Builder setUpdateListAdapter(boolean value) {
                this.updateListAdapter = value;
                return this;
            }

            Builder setFinalReceiveCommandResponse(String value) {
                this.finalReceiveCommandResponse = value;
                return this;
            }

            Builder setFinalCalculatedResultValue(String value) {
                this.finalCalculatedResultValue = value;
                return this;
            }

            Builder setFinalReadMessage(String value) {
                this.finalReadMessage = value;
                return this;
            }

            Builder setTemperatureValueCompDiff(String value) {
                this.temperatureValueCompDiff = value;
                return this;
            }

            Builder setResultInfo(String value) {
                this.resultInfo = value;
                return this;
            }

            Builder setDecTemperatureHotValue(String value) {
                this.decTemperatureHotValue = value;
                return this;
            }

            Builder setDecTemperatureColdValue(String value) {
                this.decTemperatureColdValue = value;
                return this;
            }

            Builder setFinalCurrentTestItem(String value) {
                this.finalCurrentTestItem = value;
                return this;
            }

            Builder setTestItemIdx(int value) {
                this.testItemIdx = value;
                return this;
            }

            Builder setTestOkCnt(int value) {
                this.testOkCnt = value;
                return this;
            }

            Builder setTestNgCnt(int value) {
                this.testNgCnt = value;
                return this;
            }

            Builder setReceiveCommandResponseOK(String value) {
                this.receiveCommandResponseOK = value;
                return this;
            }

            Builder setShouldUpdateCounts(boolean value) {
                this.shouldUpdateCounts = value;
                return this;
            }

            Builder setListItemAdapter(ItemAdapterTestItem adapter) {
                this.listItemAdapter = adapter;
                return this;
            }

            Builder setCurrentProcessName(String value) {
                this.currentProcessName = value;
                return this;
            }

            Builder setReceivedMessageCnt(int value) {
                this.receivedMessageCnt = value;
                return this;
            }

            UiUpdateBundle build() {
                return new UiUpdateBundle(this);
            }
        }
    }
}
