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
import itf.com.app.lms.util.UiUpdateBundle;
import itf.com.app.lms.util.RequestThreadAsync;
import itf.com.app.lms.util.RequestTestTaskThreadAsync;
import itf.com.app.lms.util.RequestVersionInfoThreadAsync;
import itf.com.app.lms.util.SpecProcessingResult;
import itf.com.app.lms.vo.VoTestItem;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;

@RequiresApi(api = Build.VERSION_CODES.S)
public class ActivityModelTestProcess extends BaseKioskActivity {

    private static final String TAG = ActivityModelTestProcess.class.getSimpleName();

    private static final UUID BT_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public final static int REQUEST_ENABLE_BT = 1;
    public final static int MESSAGE_READ = 2;
    public final static int CONNECTING_STATUS = 3;


    private static final int PERMISSION_REQUEST_CODE = 1000;
    private static final int PERMISSION_REQUEST_CODE_BT = 1001;

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

    private WattValueAnimator wattValueAnimator;
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

    private boolean isControlMode = false;  // 제어 모드 활성화 여부
    private boolean isControlOn = false;   // 제어 ON/OFF 상태 (기본값: OFF)
    private boolean controlOwnerIsAndroidApp = false;  // 제어 ON을 안드로이드 앱에서 했는지 여부
    private static final int CONTROL_ST0101_REQUIRED_COUNT = 3;  // 제어 모드 진입을 위한 ST0101 응답 횟수
    private int controlSt0101SuccessCount = 0;  // 수신한 ST0101 응답 누적 횟수
    private boolean controlModeReady = false;   // 제어 모드 진입 완료 여부

    private final AtomicBoolean waitingForControlResponse = new AtomicBoolean(false);
    private String pendingControlCommand = null;  // 대기 중인 명령
    private Timer controlResponseTimeoutTimer = null;
    private TimerTask controlResponseTimeoutTask = null;
    private final Object controlResponseLock = new Object();
    private static final long CONTROL_RESPONSE_TIMEOUT_MS = 10000; // 10초 타임아웃

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

    private float originalDialogMessageTextSize = 0f;  // tvDialogMessage의 원래 텍스트 크기
    private int originalDialogBarcodeVisibility = GONE;  // tv_dialog_barcode의 원래 visibility
    private String originalDialogTitleText = null;    // tv_dialog_title의 원래 텍스트
    private boolean isControlModeDialogConfigured = false;  // 제어 모드 다이얼로그 설정 여부

    static boolean usbReceiverRegisted = false;

    private TextView btnTestRestart;

    private BluetoothManager bluetoothManager;
    private NetworkManager networkManager;
    private itf.com.app.lms.conn.usb.UsbConnectionManager usbConnectionManager;

    private itf.com.app.lms.managers.TimerManager timerManager;
    private itf.com.app.lms.managers.PermissionManager permissionManager;
    private itf.com.app.lms.services.LoggingService loggingService;

    private TestProcessProcessor testProcessProcessor;
    private ControlModeProcessor controlModeProcessor;
    private MessageProcessor messageProcessor;
    private DataParser dataParser;

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
    private volatile String lastSpecSignature = "";

    private Handler btHandler;
    private ConnectedThreadOptimized btConnectedThread; // 최적화된 클래스 사용
    private BluetoothSocket btSocket = null;

    private TextView tvPopupProcessResultCompValue;
    private TextView tvPopupProcessResultHeaterValue;
    private TextView tvResourceInfo;

    private ActivityMainBinding binding;

    private final List<Runnable> pendingUiUpdates = new ArrayList<>();
    private final Object uiUpdateLock = new Object();
    private Runnable uiUpdateBatchTask = null;
    private static final long UI_UPDATE_BATCH_DELAY_MS = 16; // 1 frame (60fps)

    private String readMessage = null;

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


    static private final String decTemperature = "";
    static private final double decTemperatureValue = 0;
    private List<Map<String, String>> coldTemperatureData = null;
    private List<Map<String, String>> hotTemperatureData = null;
    private List<Map<String, String>> temperatureData = null;
    private boolean entranceCheck = false;
    private boolean barcodeReadCheck = false;

    private int decElectricValue = 0;
    private String resultValue = "";
    static private String resultInfo = Constants.Common.EMPTY_STRING;
    private double decElectricValueForComp = 0;
    private double decElectricValueForHeater = 0;
    private double decElectricValueForPump = 0;


    private TextView tvTemperature = null;


    public static int tmrAppResetCnt = 0;
    public static int tmrAppResetCnt2 = 0;
    public static int tmrAppResetCnt1 = 0;
    private TimerTask appResetTimerTask = null;
    private Timer tmrBTMessageSend = null;
    private TimerTask ttBTMessageSend = null;

    private boolean shouldUpdateDialog = false;

    private final AtomicBoolean btMessageTimerRunning = new AtomicBoolean(false);
    private final Object btMessageTimerLock = new Object();

    private void startBtMessageTimer() {
        synchronized (btMessageTimerLock) {
            if (btMessageTimerRunning.get()) {
                logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0001"));
                return;
            }

            if (!isControlMode) {
                if (!btConnected || btSocket == null || !btSocket.isConnected()) {
                    logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0002"));
                    return;
                }
            }

            if (tmrBTMessageSend != null || ttBTMessageSend != null) {
                stopBtMessageTimer();
            }

            if (!isControlMode) {
                updateHeaderBackgroundColor(isControlOn);
            }

            btMessageTimerRunning.set(true);

            try {
                tmrBTMessageSend = new Timer("BtMsgTimer");
                ttBTMessageSend = new TimerTask() {
                    @Override
                    public void run() {
                        if (isControlMode && isControlOn) {
                            if(controlSt0101SuccessCount<3) {
                                sendBtMessage(ST0101);
                            }
                            return; // 검사 진행 안 함
                        }

                        if (!isControlMode) {
                            if (!btConnected || btSocket == null || !btSocket.isConnected()) {
                                logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0003"));
                                stopBtMessageTimer();
                                scheduleBluetoothReconnect(false);
                                return;
                            }
                        } else {
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
                            if (arrTestItems == null || arrTestItems.length == 0) {
                                logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0004"));
                                return;
                            }
                            if (testItemIdx < arrTestItems.length) {
                                currentTestItem = arrTestItems[testItemIdx][1];
                                disconnectCheckCount = receivedMessageCnt - sendingMessageCnt;
                                if (disconnectCheckCount > Constants.TestThresholds.DISCONNECT_CHECK_THRESHOLD || Math.abs(disconnectCheckCount) > Constants.TestThresholds.DISCONNECT_CHECK_THRESHOLD) {
                                    logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0005", testTotalCounter, totalTimeCnt, testItemCounter, arrTestItems[testItemIdx][2], testItemIdx, arrTestItems.length, currentTestItem, disconnectCheckCount, receivedMessageCnt, sendingMessageCnt, receiveCommandEmptyCnt));
                                    stopBtMessageTimer();
                                    scheduleUiUpdate(() -> {
                                        final int finalUsbStatusColor = R.color.red_01;
                                        final String finalUsbStatusText = "";
                                        tvConnectBtRamp.setBackgroundColor(getColor(finalUsbStatusColor));
                                        if (finalUsbStatusText != null) {
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
                                }
                                logInfo(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0006", String.format(Constants.LogMessages.TEST_ITEM_SIGNAL_SENT + Constants.Common.EMPTY_STRING + " [T:%d/%d][C:%d/%s][P:%d/%d][S:%s][G:%d=%d-%d][W:%s]",
                                        testTotalCounter, totalTimeCnt, testItemCounter, arrTestItems[testItemIdx][2],
                                        testItemIdx, arrTestItems.length, currentTestItem, disconnectCheckCount,
                                        receivedMessageCnt, sendingMessageCnt, decElectricValue)));

                                if (Integer.parseInt(arrTestItems[testItemIdx][2]) <= testItemCounter) {
                                    testItemCounter = 0;
                                    testItemIdx++;
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
                                                logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0007", CM0101Index));
                                            }
                                        } catch (Exception e) {
                                            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0008"), e);
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
                logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0009"), e);
            }
        }
    }

    int resetCnt = 0;
    Timer tmrReset = null;
    TimerTask ttReset = null;

    private void stopBtMessageTimer() {
        synchronized (btMessageTimerLock) {
            if (btMessageTimerRunning.compareAndSet(true, false)) {
                if (!isControlMode) {
                    updateHeaderBackgroundColor(false);
                }

                if (tmrBTMessageSend != null) {
                    try {
                        tmrBTMessageSend.cancel();
                        tmrBTMessageSend.purge();
                        tmrBTMessageSend = null;
                    } catch (Exception e) {
                        logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0010"), e);
                    }
                }
                if (ttBTMessageSend != null) {
                    try {
                        ttBTMessageSend.cancel();
                        ttBTMessageSend = null;
                    } catch (Exception e) {
                        logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0011"), e);
                    }
                }
            } else {
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

    private void updateHeaderBackgroundColor(boolean isControlOn) {
        runOnUiThread(() -> {
            try {
                if (binding == null) {
                    return;
                }

                android.widget.LinearLayout llTopHeader = binding.llTopHeader;
                if (llTopHeader == null) {
                    return;
                }

                boolean isTestWaiting = currentTestItem == null ||
                        currentTestItem.isEmpty() ||
                        currentTestItem.equals(Constants.InitialValues.CURRENT_TEST_ITEM);

                logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0012", isControlOn, isTestWaiting));
                if (isControlOn && isTestWaiting) {
                    logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0013"));
                    llTopHeader.setBackgroundColor(0xFFCC0000); // #cc0000
                } else {
                    logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0014"));
                    llTopHeader.setBackgroundColor(getColor(R.color.blue_for_ovio));
                }
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0015"), e);
            }
        });
    }

    private void toggleControlMode() {
        boolean isTestRunning = currentTestItem != null &&
                !currentTestItem.isEmpty() &&
                !currentTestItem.equals(Constants.InitialValues.CURRENT_TEST_ITEM);

        if (isTestRunning) {
            runOnUiThread(() -> {
                android.widget.Toast.makeText(this, getStringResource("ui.message.test_control_disabled"), android.widget.Toast.LENGTH_SHORT).show();
            });
            return;
        }

        logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0016", isControlOn));
        isControlOn = !isControlOn;
        logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0017", isControlOn));

        if (isControlOn) {
            isControlMode = true;  // 제어 모드 활성화
            controlOwnerIsAndroidApp = true;  // 안드로이드 앱에서 제어 ON
            stopAllTimers();
            updateHeaderBackgroundColorToRed();

            notifyWebServerControlOwner(true);
        } else {
            if (!controlOwnerIsAndroidApp) {
                runOnUiThread(() -> {
                    android.widget.Toast.makeText(this, getStringResource("ui.message.control_off_only_by_owner"), android.widget.Toast.LENGTH_SHORT).show();
                });
                isControlOn = !isControlOn;
                return;
            }

            isControlMode = false;  // 제어 모드 비활성화
            controlOwnerIsAndroidApp = false;  // 제어 소유자 초기화

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

            notifyWebClientShutdown("CONTROL_MODE_EXIT");
            notifyWebServerControlOwner(false);
            restoreTimersAfterControlMode();
            updateHeaderBackgroundColor(false);
        }

        updateControlModeButton();
    }

    private void restoreTimersAfterControlMode() {
        try {
            if (btConnected && btSocket != null && btSocket.isConnected()) {
                synchronized (btMessageTimerLock) {
                    if (!btMessageTimerRunning.get()) {
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
                                logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0018"), e);
                            }
                        }

                        btMessageTimerRunning.set(true);

                        try {
                            tmrBTMessageSend = new Timer("BtMsgTimer");
                            ttBTMessageSend = new TimerTask() {
                                @Override
                                public void run() {
                                    if (!btConnected || btSocket == null || !btSocket.isConnected()) {
                                        logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0003"));
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
                                        if (arrTestItems == null || arrTestItems.length == 0) {
                                            logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0004"));
                                            return;
                                        }
                                        if (testItemIdx < arrTestItems.length) {
                                            currentTestItem = arrTestItems[testItemIdx][1];
                                            disconnectCheckCount = receivedMessageCnt - sendingMessageCnt;
                                            if (disconnectCheckCount > Constants.TestThresholds.DISCONNECT_CHECK_THRESHOLD || Math.abs(disconnectCheckCount) > Constants.TestThresholds.DISCONNECT_CHECK_THRESHOLD) {
                                                logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0005", testTotalCounter, totalTimeCnt, testItemCounter, arrTestItems[testItemIdx][2], testItemIdx, arrTestItems.length, currentTestItem, disconnectCheckCount, receivedMessageCnt, sendingMessageCnt, receiveCommandEmptyCnt));
                                                stopBtMessageTimer();
                                                scheduleUiUpdate(() -> {
                                                    final int finalUsbStatusColor = R.color.red_01;
                                                    final String finalUsbStatusText = "";
                                                    tvConnectBtRamp.setBackgroundColor(getColor(finalUsbStatusColor));
                                                    if (finalUsbStatusText != null) {
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

                                    scheduleUiUpdate(() -> tvEllapsedTimeCnt.setText(String.valueOf(testTotalCounter)));

                                    wattTemp = new String[]{getCurrentDatetime(timestampFormat), calculatedTemperatureTmp, String.valueOf(decElectricValue), currentTestItem};
                                    lstMapWattTemp.add(wattTemp);

                                    testItemCounter++;
                                    testTotalCounter++;
                                }
                            };
                            tmrBTMessageSend.schedule(ttBTMessageSend, 0, Constants.Timeouts.TIMER_INTERVAL_MS);
                            btMessageTimerRunning.set(true);
                            logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0019"));
                        } catch (Exception e) {
                            btMessageTimerRunning.set(false);
                            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0020"), e);
                        }
                    }
                }
            } else {
                logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0021"));
            }
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0022"), e);
        }
    }

    private void stopAllTimers() {
        try {
            stopBtMessageTimer();

            stopFinishedRestartTimer();

            stopUnfinishedRestartTimer();

            stopRemoteCommandTimer();

            stopResetTimer();

            stopCheckDurationTimer();

            stopBarcodeRequestTimer();

            stopAppResetTimerTask();


            logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0023"));
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0024"), e);
        }
    }

    private void stopFinishedRestartTimer() {
        synchronized (finishedRestartTimerLock) {
            if (finishedRestartTimerRunning.compareAndSet(true, false)) {
                if (tmrFinishedRestart != null) {
                    try {
                        tmrFinishedRestart.cancel();
                        tmrFinishedRestart.purge();
                        tmrFinishedRestart = null;
                    } catch (Exception e) {
                        logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0025"), e);
                    }
                }
                if (ttFinishedRestart != null) {
                    try {
                        ttFinishedRestart.cancel();
                        ttFinishedRestart = null;
                    } catch (Exception e) {
                        logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0026"), e);
                    }
                }
            } else {
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

    private void stopUnfinishedRestartTimer() {
        synchronized (unfinishedRestartTimerLock) {
            if (unfinishedRestartTimerRunning.compareAndSet(true, false)) {
                if (tmrUnfinishedRestart != null) {
                    try {
                        tmrUnfinishedRestart.cancel();
                        tmrUnfinishedRestart.purge();
                        tmrUnfinishedRestart = null;
                    } catch (Exception e) {
                        logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0027"), e);
                    }
                }
                if (ttUnfinishedRestart != null) {
                    try {
                        ttUnfinishedRestart.cancel();
                        ttUnfinishedRestart = null;
                    } catch (Exception e) {
                        logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0028"), e);
                    }
                }
            } else {
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

    private void stopRemoteCommandTimer() {
        if (tmrRemoteCommand != null) {
            try {
                tmrRemoteCommand.cancel();
                tmrRemoteCommand.purge();
                tmrRemoteCommand = null;
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0029"), e);
            }
        }
        if (ttRemoteCommand != null) {
            try {
                ttRemoteCommand.cancel();
                ttRemoteCommand = null;
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0030"), e);
            }
        }
    }

    private void stopResetTimer() {
        if (tmrReset != null) {
            try {
                tmrReset.cancel();
                tmrReset.purge();
                tmrReset = null;
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0031"), e);
            }
        }
        if (ttReset != null) {
            try {
                ttReset.cancel();
                ttReset = null;
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0032"), e);
            }
        }
    }

    private void stopCheckDurationTimer() {
        if (checkDuration != null) {
            try {
                checkDuration.cancel();
                checkDuration.purge();
                checkDuration = null;
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0033"), e);
            }
        }
    }

    private void stopBarcodeRequestTimer() {
        if (barcodeRequestTimer != null) {
            try {
                barcodeRequestTimer.cancel();
                barcodeRequestTimer.purge();
                barcodeRequestTimer = null;
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0034"), e);
            }
        }
    }

    private void stopAppResetTimerTask() {
        if (appResetTimerTask != null) {
            try {
                appResetTimerTask.cancel();
                appResetTimerTask = null;
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0035"), e);
            }
        }
    }


    private void updateHeaderBackgroundColorToRed() {
        runOnUiThread(() -> {
            try {
                if (binding == null) {
                    return;
                }

                android.widget.LinearLayout llTopHeader = binding.llTopHeader;
                if (llTopHeader == null) {
                    return;
                }

                logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0036"));
                llTopHeader.setBackgroundColor(0xFFFF0000); // red
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0037"), e);
            }
        });
    }

    private void updateControlModeButton() {
        runOnUiThread(() -> {
            try {
                if (fab_control_mode == null) {
                    return;
                }

                if (isControlOn) {
                    fab_control_mode.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFF0000)); // #FF9800
                    fab_control_mode.setImageResource(R.drawable.remote_gen_24dp_ffffff_fill0_wght400_grad0_opsz24);
                } else {
                    fab_control_mode.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.blue_for_ovio)));
                    fab_control_mode.setImageResource(R.drawable.remote_gen_24dp_ffffff_fill0_wght400_grad0_opsz24);
                }
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0038"), e);
            }
        });
    }

    public boolean isControlMode() {
        return isControlMode;
    }

    public boolean isControlOn() {
        return isControlOn;
    }

    public void notifyControlRequestAccess() {
        if (!isControlOn || !isControlMode) {
            return; // 제어 ON 상태가 아니면 표시하지 않음
        }

        runOnUiThread(() -> {
            try {
                if (tvUnitId == null) {
                    return;
                }

                if (originalUnitIdText == null) {
                    originalUnitIdText = tvUnitId.getText() != null ? tvUnitId.getText().toString() : "";
                }


                String displayText = " 제어 사용자 접근";

                tvUnitMessage.setText(displayText);

            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0039"), e);
            }
        });
    }

    public void clearControlRequestAccessMessage() {
        runOnUiThread(() -> {
            try {
                if (tvUnitMessage != null) {
                    tvUnitMessage.setText("");
                }

                if (controlRequestAccessHandler != null) {
                    controlRequestAccessHandler.removeCallbacks(controlRequestAccessRunnable);
                }
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0040"), e);
            }
        });
    }

    public void startWaitingForControlResponse(String command) {
        synchronized (controlResponseLock) {
            if (!isControlMode || !isControlOn) {
                return; // 제어 모드가 아니면 대기하지 않음
            }

            waitingForControlResponse.set(true);
            pendingControlCommand = command;

            stopControlResponseTimeout();

            controlResponseTimeoutTimer = new Timer("ControlResponseTimeout");
            controlResponseTimeoutTask = new TimerTask() {
                @Override
                public void run() {
                    synchronized (controlResponseLock) {
                        if (waitingForControlResponse.get()) {
                            waitingForControlResponse.set(false);
                            String timeoutCommand = pendingControlCommand;
                            pendingControlCommand = null;

                            scheduleUiUpdate(() -> {
                                showControlResponseDialog(timeoutCommand, null, true);
                            });

                            logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0041", timeoutCommand));
                        }
                    }
                }
            };
            controlResponseTimeoutTimer.schedule(controlResponseTimeoutTask, CONTROL_RESPONSE_TIMEOUT_MS);

            logInfo(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0042", command));
        }
    }

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

    private void handleControlModeResponse(String response) {
        synchronized (controlResponseLock) {
            if (!waitingForControlResponse.get()) {
                return; // 응답 대기 중이 아니면 무시
            }

            stopControlResponseTimeout();

            String command = pendingControlCommand;
            String responseMessage = response;

            waitingForControlResponse.set(false);
            pendingControlCommand = null;

            scheduleUiUpdate(() -> {
                showControlResponseDialog(command, responseMessage, false);
            });

            logInfo(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0043", responseMessage, command));
        }
    }

    private void showControlResponseDialog(String command, String response, boolean isTimeout) {
        try {
            if (clDialogForPreprocess == null || tvDialogMessage == null) {
                logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0044"));
                return;
            }

            boolean isFromWebControl = isControlMode && isControlOn;

            if (isFromWebControl) {
                if (!isControlModeDialogConfigured) {
                    originalDialogMessageTextSize = tvDialogMessage.getTextSize() / getResources().getDisplayMetrics().scaledDensity;

                    if (tv_dialog_barcode != null) {
                        originalDialogBarcodeVisibility = tv_dialog_barcode.getVisibility();
                    }

                    if (tv_dialog_title != null) {
                        originalDialogTitleText = tv_dialog_title.getText() != null ? tv_dialog_title.getText().toString() : "";
                    }

                    isControlModeDialogConfigured = true;
                }

                tvDialogMessage.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f);

                if (tv_dialog_barcode != null) {
                    tv_dialog_barcode.setVisibility(GONE);
                }

                if (tv_dialog_title != null) {
                    tv_dialog_title.setText(getStringResource("ui.message.control_mode_test"));
                }
            } else {
                if (isControlModeDialogConfigured) {
                    if (originalDialogMessageTextSize > 0) {
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
            }

            String message;
            if (isTimeout) {
                message = getStringResource("ui.message.command_timeout", command);
            } else {
                message = getStringResource("ui.message.command_response", command, (response != null ? response : getStringResource("ui.label.none")));
            }

            tvDialogMessage.setText(message);
            clDialogForPreprocess.setVisibility(VISIBLE);

            mainHandler.postDelayed(() -> {
                if (clDialogForPreprocess != null) {
                    clDialogForPreprocess.setVisibility(GONE);
                    tvDialogMessage.setText("");

                    if (!isControlMode || !isControlOn) {
                        if (isControlModeDialogConfigured) {
                            if (originalDialogMessageTextSize > 0) {
                                tvDialogMessage.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, originalDialogMessageTextSize);
                            }

                            if (tv_dialog_barcode != null && originalDialogBarcodeVisibility != GONE) {
                                tv_dialog_barcode.setVisibility(originalDialogBarcodeVisibility);
                            }

                            if (tv_dialog_title != null && originalDialogTitleText != null) {
                                tv_dialog_title.setText(originalDialogTitleText);
                            }

                            isControlModeDialogConfigured = false;
                        }
                    }
                }
            }, 5000);

        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0045"), e);
        }
    }

    public void executeControlModeTestItem(String command) {
        if (!isControlMode || !isControlOn) {
            logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0046"));
            return;
        }

        if (arrTestItems == null || arrTestItems.length == 0) {
            logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0047"));
            return;
        }

        int foundIndex = -1;
        for (int i = 0; i < arrTestItems.length; i++) {
            if (arrTestItems[i][1].equals(command)) {
                foundIndex = i;
                break;
            }
        }

        if (foundIndex == -1) {
            logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0048", command));
            return;
        }

        stopControlTestTimer();

        synchronized (controlTestTimerLock) {
            controlTestItemIdx = foundIndex;
            controlTestItemCounter = 0;
            controlCurrentTestItem = command;
            controlTestTimerRunning.set(true);
        }

        logInfo(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0049"));

        try {
            controlTestTimer = new Timer("ControlTestTimer");
            controlTestTimerTask = new TimerTask() {
                @Override
                public void run() {
                    synchronized (controlTestTimerLock) {
                        if (!controlTestTimerRunning.get() || controlTestItemIdx == -1) {
                            return;
                        }

                        if (!btConnected || btSocket == null || !btSocket.isConnected()) {
                            logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0050"));
                            stopControlTestTimer();
                            return;
                        }

                        int testDuration = 0;
                        try {
                            testDuration = Integer.parseInt(arrTestItems[controlTestItemIdx][2]);
                            testDuration = (testDuration==1)?2:testDuration;
                        } catch (NumberFormatException e) {
                            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0051", arrTestItems[controlTestItemIdx][2]), e);
                            stopControlTestTimer();
                            return;
                        }

                        sendBtMessage(controlCurrentTestItem);
                        controlTestItemCounter++;

                        logInfo(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0052", controlTestItemCounter, testDuration, controlCurrentTestItem));

                        if (controlTestItemCounter >= testDuration) {
                            logInfo(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0053", controlCurrentTestItem));
                            stopControlTestTimer();

                            showControlTestResultDialog();
                        }
                    }
                }
            };

            controlTestTimer.schedule(controlTestTimerTask, 0, Constants.Timeouts.TIMER_INTERVAL_MS);

        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0054"), e);
            synchronized (controlTestTimerLock) {
                controlTestTimerRunning.set(false);
                controlTestItemIdx = -1;
                controlTestItemCounter = 0;
                controlCurrentTestItem = null;
            }
        }
    }

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
        }
    }

    private void forceExitControlModeForRestart() {
        isControlMode = false;
        isControlOn = false;
        controlOwnerIsAndroidApp = false;
        controlModeReady = false;
        controlSt0101SuccessCount = 0;
        waitingForControlResponse.set(false);
        pendingControlCommand = null;

        stopControlTestTimer();
        clearControlTestInfo();

        updateHeaderBackgroundColor(false);

        notifyWebServerControlOwner(false);

        logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0055"));
    }

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

    private void showControlModeTestResultDialog() {
        if (!isControlMode || !isControlOn) {
            return; // 제어 모드가 아니면 표시하지 않음
        }

        synchronized (controlTestTimerLock) {
            if (controlCurrentTestItem == null) {
                logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0056"));
                return;
            }

            try {
                String command = controlCurrentTestItem;
                String receiveCommand = controlTestReceiveCommand != null ? controlTestReceiveCommand : "";
                String receiveResponse = controlTestReceiveResponse != null ? controlTestReceiveResponse : "";
                String resultValue = controlTestResultValue != null ? controlTestResultValue : "";
                String result = controlTestResult != null ? controlTestResult : "";

                StringBuilder message = new StringBuilder();
                message.append(getStringResource("ui.message.control_command", command)).append("\n");
                message.append(getStringResource("ui.message.received_command", receiveCommand)).append("\n");

                if (!receiveResponse.isEmpty()) {
                    message.append(getStringResource("ui.message.received_response", receiveResponse)).append("\n");
                }

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

                scheduleUiUpdate(() -> {
                    try {
                        if (clDialogForPreprocess == null || tvDialogMessage == null) {
                            logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0044"));
                            return;
                        }

                        boolean isFromWebControl = isControlMode && isControlOn;

                        if (isFromWebControl) {
                            if (!isControlModeDialogConfigured) {
                                originalDialogMessageTextSize = tvDialogMessage.getTextSize() / getResources().getDisplayMetrics().scaledDensity;

                                if (tv_dialog_barcode != null) {
                                    originalDialogBarcodeVisibility = tv_dialog_barcode.getVisibility();
                                }

                                if (tv_dialog_title != null) {
                                    originalDialogTitleText = tv_dialog_title.getText() != null ? tv_dialog_title.getText().toString() : "";
                                }

                                isControlModeDialogConfigured = true;
                            }


                            if (tv_dialog_barcode != null) {
                            }

                            if (tv_dialog_title != null) {
                                tv_dialog_title.setText(getStringResource("ui.message.control_mode_test"));
                            }
                        }

                        tvDialogMessage.setText(finalMessage);
                        clDialogForPreprocess.setVisibility(VISIBLE);

                        mainHandler.postDelayed(() -> {
                            if (clDialogForPreprocess != null) {
                                clDialogForPreprocess.setVisibility(GONE);
                                tvDialogMessage.setText("");

                                clearControlTestInfo();

                                if (!isControlMode || !isControlOn) {
                                    if (isControlModeDialogConfigured) {
                                        if (originalDialogMessageTextSize > 0) {
                                            tvDialogMessage.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, originalDialogMessageTextSize);
                                        }

                                        if (tv_dialog_barcode != null && originalDialogBarcodeVisibility != GONE) {
                                            tv_dialog_barcode.setVisibility(originalDialogBarcodeVisibility);
                                        }

                                        if (tv_dialog_title != null && originalDialogTitleText != null) {
                                            tv_dialog_title.setText(originalDialogTitleText);
                                        }

                                        isControlModeDialogConfigured = false;
                                    }
                                }
                            }
                        }, 5000);

                        logInfo(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0057", command));
                    } catch (Exception e) {
                        logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0058"), e);
                    }
                });
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0058"), e);
            }
        }
    }

    private void showControlTestResultDialog() {
        if (!isControlMode || !isControlOn) {
            return; // 제어 모드가 아니면 표시하지 않음
        }

        if (listItemAdapter == null || controlTestItemIdx < 0 || controlTestItemIdx >= listItemAdapter.getCount()) {
            logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0059"));
            return;
        }

        try {
            VoTestItem testItem = (VoTestItem) listItemAdapter.getItem(controlTestItemIdx);
            String testResult = testItem.getTest_item_result();
            String testCheckValue = testItem.getTest_result_check_value();

            recalcTestCountsFromAdapter(listItemAdapter);

            scheduleUiUpdate(() -> {
                try {
                    if (clTestResult == null || tvPopupProcessResult == null) {
                        logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0060"));
                        return;
                    }

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

                    if (tvPopupProcessResultCompValue != null && testItem.getTest_item_command().contains(Constants.TestItemCodes.CM0101)) {
                        tvPopupProcessResultCompValue.setText(testCheckValue);
                    }
                    if (tvPopupProcessResultHeaterValue != null && testItem.getTest_item_command().contains(Constants.TestItemCodes.HT0101)) {
                        tvPopupProcessResultHeaterValue.setText(testCheckValue);
                    }

                    if (tvTestOkCnt != null) {
                        tvTestOkCnt.setText(String.valueOf(testOkCnt));
                    }
                    if (tvTestNgCnt != null) {
                        tvTestNgCnt.setText(String.valueOf(testNgCnt));
                    }

                    clTestResult.setVisibility(VISIBLE);

                    logInfo(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0061"));
                } catch (Exception e) {
                    logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0062"), e);
                }
            });
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0062"), e);
        }
    }

    private void notifyWebServerControlOwner(boolean isAndroidAppOwner) {
        new Thread(() -> {
            try {
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

                            String postData = "is_android_app=" + (isAndroidAppOwner ? "true" : "false");
                            java.io.OutputStream os = connection.getOutputStream();
                            os.write(postData.getBytes("UTF-8"));
                            os.flush();
                            os.close();

                            int responseCode = connection.getResponseCode();
                            if (responseCode == 200) {
                                logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0063", isAndroidAppOwner));
                            } else {
                                logWarn(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0064", responseCode));
                            }
                        } finally {
                            connection.disconnect();
                        }
                    }
                }
            } catch (Exception e) {
                logWarn(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0065", e.getMessage()));
            }
        }).start();
    }

    private void notifyWebClientShutdown(String reason) {
        new Thread(() -> {
            try {
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

                            String postData = "reason=" + java.net.URLEncoder.encode(reason, "UTF-8");
                            java.io.OutputStream os = connection.getOutputStream();
                            os.write(postData.getBytes("UTF-8"));
                            os.flush();
                            os.close();

                            int responseCode = connection.getResponseCode();
                            if (responseCode == 200) {
                                logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0066", reason));
                            } else {
                                logWarn(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0067", responseCode));
                            }
                        } finally {
                            connection.disconnect();
                        }
                    }
                }
            } catch (Exception e) {
                logWarn(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0068", e.getMessage()));
            }
        }).start();
    }

    private String originalUnitIdText = null;
    private final Handler controlRequestAccessHandler = new Handler(Looper.getMainLooper());
    private final Runnable controlRequestAccessRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (tvUnitMessage != null) {
                    logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0069"));
                    tvUnitMessage.setText("");
                }
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0040"), e);
            }
        }
    };

    private final Handler schedulerHandler = new Handler(Looper.getMainLooper());
    private Runnable btReconnectRunnable = null;
    private long btReconnectDelayMs = Constants.Timeouts.BT_LIST_TIMER_INTERVAL_MS;
    private static final long BT_RECONNECT_DELAY_MAX_MS = Constants.Timeouts.BT_LIST_TIMER_INTERVAL_MS * 60;
    private boolean btConnectionInProgress = false;

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



    int temperature12Bit = 0;
    String temperatureTmp = Constants.InitialValues.TEMPERATURE_TMP;

    String valueWatt = Constants.InitialValues.VALUE_WATT;
    String lowerValueWatt = Constants.InitialValues.LOWER_VALUE_WATT;
    String upperValueWatt = Constants.InitialValues.UPPER_VALUE_WATT;


    public boolean productSerialNoYn = false;
    public static List<Map<String, String>> btInfoList = null;

    String urlStr = "";
    String urlTestTaskStr = "";
    RequestTestTaskThreadAsync testTaskThread = null;
    static String urlStrBarcode = "";

    private static final Object HTTP_HANDLER_LOCK = new Object();
    private Runnable pendingHttpTask = null;
    private static final long HTTP_DEBOUNCE_DELAY_MS = 100; // 100ms 디바운스

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
    private UsbService usbService;
    private ActivityModelTestProcess.UsbHandler usbHandler;
    private UsbCommandQueue usbCommandQueue;
    private final java.util.concurrent.ConcurrentHashMap<String, CompletableFuture<String>> pendingUsbResponses = new java.util.concurrent.ConcurrentHashMap<>();
    private final Object usbResponseLock = new Object();
    public List<Integer> lstWatt = new ArrayList<>();
    public List<String[]> lstMapWattTemp = new ArrayList<>();
    private String[] wattTemp = null;

    public HttpURLConnection connection = null;


    boolean finishedCorrectly = false;
    Timer tmrFinishedRestart = null;
    TimerTask ttFinishedRestart = null;
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

    private static final String[] MODERN_MEDIA_PERMISSIONS = {
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
    };

    private static final String[] LEGACY_STORAGE_PERMISSIONS_API30_UP = {
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

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
    String receiveResponseResult = Constants.InitialValues.RECEIVE_RESPONSE_RESULT;
    String receiveResponseResultTxt = "";

    Timer tmrUnfinishedRestart = null;
    TimerTask ttUnfinishedRestart = null;
    private final AtomicBoolean unfinishedRestartTimerRunning = new AtomicBoolean(false);
    private final Object unfinishedRestartTimerLock = new Object();
    FloatingActionButton fab_close = null;
    FloatingActionButton fab_control_mode = null;
    int min_diff = 0;
    int reload_min_diff = 20;

    int sendingMessageCnt = 0;
    int receivedMessageCnt = 0;
    int disconnectCheckCount = 0;

    static int postTestInfoColor = 0;

    private Timer checkDuration = null;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable resourceInfoRunnable = new Runnable() {
        @Override
        public void run() {
            updateResourceInfo();
            mainHandler.postDelayed(this, Constants.Timeouts.TIMER_INTERVAL_MS);
        }
    };
    private boolean isConnected = false;


    static boolean blnBarcodeReceived = false;
    static int aTryingCnt = 0;

    String iRemoteCommandCallCondition = Constants.InitialValues.REMOTE_COMMAND_CALL_CONDITION;
    int iRemoteCommandCnt = 0;
    Timer tmrRemoteCommand = null;
    TimerTask ttRemoteCommand = null;
    private final AtomicBoolean remoteCommandTimerRunning = new AtomicBoolean(false);

    private String decTemperatureColdValue = "";
    private String decTemperatureHotValue = "";
    private String decTemperatureValueCompStart = "";
    private boolean compAgingStarted = false;
    private String decTemperatureValueCompEnd = "";
    private String temperatureValueCompDiff = "";

    private boolean btConnected = false;
    private boolean isHandlingUsbReconnection = false;

    private Timer barcodeRequestTimer = null;

    private String calculatedTemperatureTmp = Constants.Common.ZERO;


    private String test_version_id = "";
    private String model_id = "";

    private static final LogManager logManager = LogManager.getInstance();

    @Override
    public void onDestroy() {
        try {
            cleanupManagers();

            super.onDestroy();
            logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0070"));


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

            stopBtMessageTimer();
            stopBtConnectionIndicator();
            clearBluetoothReconnect();

            if (btSocket != null) {
                try {
                    if (btSocket.isConnected()) {
                        btSocket.close();
                    }
                } catch (Exception e) {
                    logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0071", e.getMessage()));
                }
                btSocket = null;
            }

            if (btConnectedThread != null) {
                try {
                    btConnectedThread.cancel();
                } catch (Exception e) {
                    logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0072", e.getMessage()));
                }
                btConnectedThread = null;
            }

            btConnected = false;
            btConnectionInProgress = false;

            if (btWorkerExecutor != null && !btWorkerExecutor.isShutdown()) {
                btWorkerExecutor.shutdownNow();
                try {
                    if (!btWorkerExecutor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                        logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0073"));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0074"));
                }
            }

            if (usbPollingExecutor != null && !usbPollingExecutor.isShutdown()) {
                usbPollingExecutor.shutdownNow();
                try {
                    if (!usbPollingExecutor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                        logWarn(LogManager.LogCategory.US, getStringResource("log.activity_model_test_process.0075"));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logWarn(LogManager.LogCategory.US, getStringResource("log.activity_model_test_process.0076"));
                }
            }

            cleanupAllResources();

            if (binding != null) {
                binding = null;
            }

            clearUiUpdateBatchQueue();

            if (dialogManager != null) {
                dialogManager.cleanup();  // ⚠️ CRITICAL FIX: Use cleanup() instead of dismissAllDialogs()
                dialogManager = null;
            }

            if (wattValueAnimator != null) {
                wattValueAnimator.cleanup();
                wattValueAnimator = null;
            }

            resetActivityState();

        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0077", e), e);
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
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0078"), e);
        }
        return null;
    }

    private void enableStrictMode() {
        android.os.StrictMode.setThreadPolicy(
                new android.os.StrictMode.ThreadPolicy.Builder()
                        .detectNetwork()

                        .detectDiskReads()

                        .detectDiskWrites()

                        .detectCustomSlowCalls()

                        .penaltyLog()


                        .build()
        );

        android.os.StrictMode.setVmPolicy(
                new android.os.StrictMode.VmPolicy.Builder()
                        .detectLeakedSqlLiteObjects()

                        .detectLeakedClosableObjects()

                        .detectActivityLeaks()

                        .detectLeakedRegistrationObjects()

                        .penaltyLog()


                        .build()
        );

        logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0079"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            if (BuildConfig.DEBUG) {
                enableStrictMode();  // Configure StrictMode detection
            }


            logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0080", entranceCheck, currentTestItem));

            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            checkAndRequestPermissionsAsync();  // 백그라운드에서 체크, 메인으로 포스팅

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

            if (tvWattValue != null) {
                wattValueAnimator = new WattValueAnimator(tvWattValue);
            }

            trPreprocessContent = binding.trPreprocessContent;


            tv_current_version = binding.tvCurrentVersion;

            cl_dialog_for_logger = binding.clDialogForLogger;    // 20240522 바코드 인식 표시
            tv_dialog_for_logger_watt = binding.tvDialogForLoggerWatt;    // 20240522 바코드 인식 표시
            tv_dialog_for_logger_temp = binding.tvDialogForLoggerTemp;    // 20240522 바코드 인식 표시

            fab_close = binding.fabClose;

            fab_close.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    notifyWebClientShutdown("FAB_CLOSE");
                    finishApplication(getApplicationContext());
                }
            });

            fab_control_mode = binding.fabControlMode;
            if (fab_control_mode != null) {
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


            mReadBuffer.setMovementMethod(ScrollingMovementMethod.getInstance());

            mainActivityRef = new WeakReference<>(this);

            cl_log = binding.clLog;
            tv_log = binding.tvLog;
            finishedCorrectly = false;
            testProcessId = Constants.InitialValues.TEST_PROCESS_ID;

            tv_log.setMovementMethod(ScrollingMovementMethod.getInstance());

            ActivityModelTestProcess activity = getMainActivity();
            if (activity != null && !activity.isFinishing()) {
                activity.scheduleUiUpdate(() -> {
                    switch (mode_type) {
                        case Constants.ResultStatus.MODE_TYPE_NORMAL:
                            cl_log.setVisibility(GONE);
                            tv_log.setText(Constants.Common.EMPTY_STRING);
                            break;
                        case Constants.ResultStatus.MODE_TYPE_TEST:
                            cl_log.setVisibility(GONE);
                            tv_log.setText("");
                            break;
                    }
                });
            }

            binding.getRoot().post(this::startDeferredInitialization);

            initializeManagers();
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0081"), e);
        }
    }


    private void checkAndRequestPermissionsAsync() {
        runOnBtWorker(() -> {
            List<String> missingPermissions = new ArrayList<>();
            for (String permission : getRequiredPermissions()) {
                if (ActivityCompat.checkSelfPermission(this, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    missingPermissions.add(permission);
                }
            }

            final List<String> finalMissingPermissions = missingPermissions;
            final boolean allGranted = finalMissingPermissions.isEmpty();

            runOnUiThread(() -> {
                if (allGranted) {
                    permissionDenialCount = 0; // 권한이 이미 허용되었으면 거부 횟수 리셋
                    btPermissionsGranted = true;
                    runPendingPermissionActions();
                } else {
                    logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0082"));
                    requestRuntimePermissionsOnMainThread(finalMissingPermissions);
                }
            });
        });
    }

    private void requestRuntimePermissionsOnMainThread(List<String> missingPermissions) {
        if (permissionRequestInProgress) {
            logInfo(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0083"));
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
        dialogManager = new DialogManager(this);

        initializeServiceManagers();

        try {
            usbHandler = new ActivityModelTestProcess.UsbHandler(this);

            setupResourceInfoOverlay();
            setDisplayLightValueChange(0.5f);
            ensureAdaptersInitialized();
            runOnBtWorker(this::initializeAppStateAsync);
            wireDeferredListeners();
            initBluetoothHandler();
            startBluetoothConnectionFlow();
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0084"), e);
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
                logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0085", isControlMode, isControlOn));
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
                            logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0086", isControlMode, isControlOn));
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
                        if (clTestResult.getVisibility() == VISIBLE && !isControlMode) {
                            clTestResult.setVisibility(GONE);
                        }

                        String handlerMessage = new String(payload, StandardCharsets.UTF_8);

                        runOnBtWorker(() -> processBtMessage(payload));
                        break;
                    case CONNECTING_STATUS:
                        if (msg.arg1 == 1) {
                            logInfo(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0087"));
                            scheduleUiUpdate(() -> {
                                try {
                                    String[] btDeviceNameInfo = btDeviceName.split(Constants.Common.UNDER_SCORE);
                                    tvConnectBtRamp.setBackgroundColor(getBaseContext().getResources().getColor(R.color.blue_01));
                                } catch (Exception e) {
                                    logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0088"), e);
                                }
                            });
                            stopBtMessageTimer();
                            startBtMessageTimer();
                        } else {
                            logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0089"));
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
            logInfo(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0090"));
            return;
        }
        logInfo(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0091"));
        new Thread(this::bluetoothSearch).start();
    }


    private void initializeManagers() {
        logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0092"));

        try {
            initializeBluetoothManager();
            initializeNetworkManager();

            logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0093"));
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0094"), e);
        }
    }

    private void initializeServiceManagers() {
        try {
            timerManager = new TimerManager();
            logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0095"));

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
                }
            });
            logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0096"));

            loggingService = new LoggingService();
            logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0097"));

            testProcessProcessor = new TestProcessProcessor();
            controlModeProcessor = new ControlModeProcessor();
            messageProcessor = new MessageProcessor();
            dataParser = new DataParser();
            logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0098"));
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0099"), e);
        }
    }

    private void initializeBluetoothManager() {
        bluetoothManager = new BluetoothManager(
                getApplicationContext(),
                new BluetoothManager.BluetoothListener() {
                    @Override
                    public void onMessageReceived(byte[] data) {
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
                                logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0100"), e);
                            }
                        });
                    }

                    @Override
                    public void onError(BluetoothManager.BluetoothError error, String message) {
                        logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0101", error, message), null);
                        runOnUiThread(() -> {
                            try {
                                clAlert.setVisibility(View.VISIBLE);
                                tvAlertMessage.setText("Bluetooth Error: " + message);
                            } catch (Exception e) {
                                logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0102"), e);
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
        logInfo(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0103"));
    }


    private void initializeNetworkManager() {
        networkManager = new NetworkManager(
                getApplicationContext(),
                new NetworkManager.NetworkListener() {
                    @Override
                    public void onTestSpecReceived(List<Map<String, String>> specData) {
                        lstData = specData;

                        logInfo(LogManager.LogCategory.SI, getStringResource("log.activity_model_test_process.0104"));
                    }

                    @Override
                    public void onBarcodeInfoReceived(String serialNo, Map<String, String> productInfo) {
                        runOnUiThread(() -> {
                            try {
                                globalProductSerialNo = serialNo;
                                barcodeReadCheck = true;

                                String modelId = productInfo.get("model_id");
                                String modelName = productInfo.get("model_name");

                                if (modelId != null) globalModelId = modelId;
                                if (modelName != null) globalModelName = modelName;

                                logInfo(LogManager.LogCategory.BI, getStringResource("log.activity_model_test_process.0105", serialNo, modelName));
                            } catch (Exception e) {
                                logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0106"), e);
                            }
                        });
                    }

                    @Override
                    public void onUploadComplete(boolean success, String message) {
                        logInfo(LogManager.LogCategory.SI, getStringResource("log.activity_model_test_process.0107", (success ? "succeeded" : "failed"), message));
                    }

                    @Override
                    public void onVersionInfoReceived(String version) {
                        logInfo(LogManager.LogCategory.SI, getStringResource("log.activity_model_test_process.0108", version));
                    }

                    @Override
                    public void onError(NetworkManager.NetworkError error, String message) {
                        logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0109", error, message), null);
                    }

                    @Override
                    public void onProgress(int progress, String message) {
                        logDebug(LogManager.LogCategory.SI, getStringResource("log.activity_model_test_process.0110", progress, message));
                    }
                }
        );

        networkManager.initialize();

        if (serverIp != null && !serverIp.isEmpty()) {
            networkManager.setServerIpAddresses(serverIp, serverResetIp, serverDomain);
        }

        logInfo(LogManager.LogCategory.SI, getStringResource("log.activity_model_test_process.0111"));
    }

    private void startManagers() {
        logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0112"));

        if (bluetoothManager != null && btSearchOnOff) {
            bluetoothManager.startDeviceSearch();
        }


        if (networkManager != null && globalModelId != null) {
            networkManager.fetchTestSpec(globalModelId);
        }
    }

    private void cleanupManagers() {
        logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0113"));

        if (bluetoothManager != null) {
            try {
                bluetoothManager.cleanup();
                bluetoothManager = null;
                logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0114"));
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0115"), e);
            }
        }

        if (usbConnectionManager != null) {
            try {
                usbConnectionManager.cleanup();
                usbConnectionManager = null;
                logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0116"));
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0117"), e);
            }
        }

        if (networkManager != null) {
            try {
                networkManager.cleanup();
                networkManager = null;
                logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0118"));
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0119"), e);
            }
        }

        if (timerManager != null) {
            try {
                timerManager.stopAllTimers();
                timerManager = null;
                logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0120"));
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0121"), e);
            }
        }

        if (permissionManager != null) {
            permissionManager = null;
        }

        if (loggingService != null) {
            loggingService = null;
        }

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

        logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0122"));
    }


    private void processBtMessage(byte[] raw) {

        try {
            if (!btConnected) {
                btConnected = true;
                clearBluetoothReconnect();
            }
            readMessage = new String(raw, StandardCharsets.UTF_8);


            if (!(isControlMode && isControlOn)) {
                controlSt0101SuccessCount = 0;
                controlModeReady = false;
            }


            final boolean[] isControlTestRunning = {false};
            final boolean[] originalEntranceCheck = {entranceCheck};
            final int[] originalTestItemIdx = {testItemIdx};
            final int[] originalTestItemCounter = {testItemCounter};
            final String[] originalCurrentTestItem = {currentTestItem};

            synchronized (controlTestTimerLock) {
                isControlTestRunning[0] = (isControlMode && isControlOn && controlTestTimerRunning.get() && controlTestItemIdx >= 0);
            }

            if (isControlTestRunning[0]) {
                originalEntranceCheck[0] = entranceCheck;
                entranceCheck = true;

                originalTestItemIdx[0] = testItemIdx;
                originalTestItemCounter[0] = testItemCounter;
                originalCurrentTestItem[0] = currentTestItem;

                synchronized (controlTestTimerLock) {
                    testItemIdx = controlTestItemIdx;
                    testItemCounter = controlTestItemCounter;
                    currentTestItem = controlCurrentTestItem;
                }

            } else if (isControlMode && isControlOn && waitingForControlResponse.get()) {
                handleControlModeResponse(readMessage);
                return; // 제어 모드 응답 처리 후 일반 처리 건너뜀
            }

            if (arrTestItems == null || listItemAdapter == null) {
                logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0123"));
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


            if (readMessage.contains(ST0101)) {
                if (isControlMode && isControlOn) {
                    if (!controlModeReady) {
                        controlSt0101SuccessCount++;
                        logInfo(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0124"));

                        if (controlSt0101SuccessCount >= CONTROL_ST0101_REQUIRED_COUNT) {
                            controlModeReady = true;
                            logInfo(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0125"));
                        } else {
                            logInfo(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0126"));
                            return; // 3회 누적 전에는 제어 모드 진입하지 않음
                        }
                    }

                    logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0127"));

                    scheduleUiUpdate(() -> {
                        try {
                            if (tvUnitMessage != null) {
                                tvUnitMessage.setText(getStringResource("ui.message.control_ready"));
                                logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0128"));
                            }
                        } catch (Exception e) {
                            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0129"), e);
                        }
                    });

                    entranceCheck = true;

                    logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0130"));
                    return; // 일반 검사 시작 로직 실행 안 함
                }

                if (!canEnterTest()) {
                    logWarn(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0131"));
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
                if (listItemAdapter != null && listItemAdapter.getCount() > testItemIdx) {
                    ((VoTestItem) listItemAdapter.getItem(testItemIdx)).setTest_item_result(Constants.ResultStatus.OK);
                    ((VoTestItem) listItemAdapter.getItem(testItemIdx)).setTest_result_value(Constants.ResultStatus.COMP_AGING_RESPONSE_01 + Constants.Common.LOGGER_DEVIDER_01 + Constants.ResultStatus.COMP_AGING_RESPONSE_01);
                    ((VoTestItem) listItemAdapter.getItem(testItemIdx)).setTest_finish_yn(Constants.ResultStatus.YES);
                } else {
                    logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0132"));
                }
                if (testProcessId.equals(Constants.Common.EMPTY_STRING)) {
                    testProcessId = getCurrentDatetime(timestampFormat);
                    logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0133", testProcessId));
                }
                entranceCheck = true;
                boolean timerAlreadyRunning = false;
                synchronized (unfinishedRestartTimerLock) {
                    if (unfinishedRestartTimerRunning.get()) {
                        logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0134"));
                        timerAlreadyRunning = true;
                    } else {
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
                                        logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0135", restartCntUnfinished, (totalTimeCnt + restartCntMargin)));
                                        resetBluetoothSessionKeepUsb();
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
                            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0136"), e);
                        }
                    }
                }
                testItemIdx = 1;

                try {
                    logInfo(LogManager.LogCategory.US, getStringResource("log.activity_model_test_process.0137"));
                    setFilters();  // Start listening notifications from UsbService
                    startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
                } catch (Exception e) {
                    logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0138"), e);
                }

                scheduleUiUpdate(() -> {
                    logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0139", isControlMode));
                    if (clTestResult.getVisibility() == VISIBLE && !isControlMode) {
                        clTestResult.setVisibility(GONE);
                    }
                    setDisplayLightValueChange(Constants.UI.DISPLAY_BRIGHTNESS);
                });
            } else if (readMessage.contains(Constants.TestItemCodes.RE0101)) {
                stopBtMessageTimer();
            }

            if (entranceCheck) {
                if (readMessage == null || readMessage.isEmpty()) {
                    logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0140"));
                    return;
                }

                int stxIndex = readMessage.indexOf(Constants.CharCodes.STX);
                if (stxIndex < 0) {
                    logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0141", readMessage));
                    return;
                }

                int minRequiredLength = Math.max(stxIndex + 7, 15);
                if (readMessage.length() < minRequiredLength) {
                    logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0142", readMessage.length(), minRequiredLength, readMessage));
                    return;
                }


                try {
                    receiveCommand = "";
                    receiveCommand = readMessage.substring(stxIndex + 1, stxIndex + 7);
                    receiveCommandResponse = readMessage.substring(13, 15);
                } catch (StringIndexOutOfBoundsException e) {
                    logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0143", readMessage, stxIndex), e);
                    return;
                }


                if (receiveCommand == null || receiveCommand.isEmpty()) {
                    logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0144"));
                    return;
                }

                if (receiveCommand.equals("CM0102") || receiveCommand.equals("CM0103")) {
                    receiveCommand = Constants.TestItemCodes.CM0101;
                }
                final String finalReceiveCommand = receiveCommand;
                final String finalReceiveCommandResponse = receiveCommandResponse; // 초기값을 final 변수로 캡처
                final String finalReadMessage = readMessage; // readMessage를 final 변수로 캡처
                final String finalCurrentTestItem = currentTestItem;

                try {
                    List<Map<String, String>> specDataResult = getSpecData(finalReceiveCommand);
                    final List<Map<String, String>> finalSpecData = specDataResult;

                    scheduleUiUpdate(() -> {
                        try {
                            if (finalSpecData != null && !finalSpecData.isEmpty()) {
                                lstSpecData = finalSpecData;
                            } else {
                                lstSpecData = null;
                            }

                            if (lstSpecData == null || lstSpecData.isEmpty()) {
                                logWarn(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0145", finalReceiveCommand));
                                return;
                            }

                            receivedMessageCnt++;
                            String currentProcessName = ((VoTestItem) listItemAdapter.getItem(testItemIdx)).getTest_item_name();
                            applyUiBundle(new UiUpdateBundle.Builder().setCurrentProcessName(currentProcessName).setReceivedMessageCnt(receivedMessageCnt).build());
                        } catch (Exception e) {
                            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0146"), e);
                            sendBtMessage(finalCurrentTestItem);
                        }
                    });

                    try {
                        if (lstSpecData == null || lstSpecData.size() == 0) {
                            return;
                        }

                        String calculatedReceiveResponseResult = Constants.Common.EMPTY_STRING;
                        String calculatedResultValue = Constants.Common.EMPTY_STRING;
                        String calculatedDecTemperature = Constants.Common.EMPTY_STRING;
                        int calculatedDecTemperatureValue = 0;
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
                            logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0147", isControlMode));
                            if (!isControlMode) { clTestResult.setVisibility(GONE); }

                            tvPopupProcessResult.setText("");
                            tvTestOkCnt.setText("");
                            tvTestNgCnt.setText("");
                        });

                        String dialogCommand = resolveDialogCommand(finalReceiveCommand);
                        if (dialogCommand != null) {
                            shouldUpdateDialog = true;

                            switch (dialogCommand) {
                                case Constants.TestItemCodes.SN0101:
                                    dialogColor = getBaseContext().getResources().getColor(R.color.pink_01);
                                    dialogMessage = lstSpecData.get(0).get(Constants.JsonKeys.CLM_TEST_NAME);
                                    break;
                                case Constants.TestItemCodes.SN0201:
                                    dialogColor = getBaseContext().getResources().getColor(R.color.yellow_01);
                                    dialogMessage = lstSpecData.get(0).get(Constants.JsonKeys.CLM_TEST_NAME);
                                    break;
                                case Constants.TestItemCodes.SN0301:
                                    dialogColor = getBaseContext().getResources().getColor(R.color.green_02);
                                    dialogMessage = lstSpecData.get(0).get(Constants.JsonKeys.CLM_TEST_NAME);
                                    break;
                                case Constants.TestItemCodes.TA0101:
                                    dialogColor = getBaseContext().getResources().getColor(R.color.blue_01);
                                    dialogMessage = lstSpecData.get(0).get(Constants.JsonKeys.CLM_TEST_NAME);
                                    break;
                                case Constants.TestItemCodes.TA0201:
                                    dialogColor = getBaseContext().getResources().getColor(R.color.yellow_01);
                                    dialogMessage = lstSpecData.get(0).get(Constants.JsonKeys.CLM_TEST_NAME);
                                    break;
                                default:
                                    shouldHideDialog = true;
                                    break;
                            }

                            if (!shouldHideDialog) {
                                final int finalDialogColor = dialogColor;
                                final String finalDialogMessage = dialogMessage;
                                scheduleUiUpdate(() -> {
                                    clDialogForPreprocess.setVisibility(View.VISIBLE);
                                    clDialogForPreprocess.setBackgroundColor(finalDialogColor);
                                    tvDialogMessage.setText(finalDialogMessage);
                                });
                            }
                        }

                        if (!calculatedReceiveResponseResult.isEmpty()) {
                            receiveResponseResult = calculatedReceiveResponseResult;
                        }
                        if (!calculatedResultValue.isEmpty()) {
                            resultValue = calculatedResultValue;
                        }
                        if (!calculatedReceiveCommandResponse.isEmpty()) {
                            receiveCommandResponse = calculatedReceiveCommandResponse;
                        }

                        updateItemCommand = finalReceiveCommand;
                        updateItemResult = receiveResponseResult;
                        updateItemCheckValue = resultValue;

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

                            runOnBtWorker(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        StringBuilder decElectricValueList = new StringBuilder();
                                        if (finalReceiveCommand.contains(Constants.TestItemCodes.RE0101)) {
                                            for (int i = 0; i < lstWatt.size(); i++) {
                                                decElectricValueList.append(lstWatt.get(i)).append(",");
                                            }
                                            if (decElectricValueList.length() > 0) {
                                                decElectricValueList = new StringBuilder(decElectricValueList.substring(0, decElectricValueList.length() - 1));
                                            }
                                            decElectricValueList.insert(0, "&clm_watt_log=");
                                        }

                                        testTaskThread = new RequestTestTaskThreadAsync(ActivityModelTestProcess.this);
                                        urlTestTaskStr = "http://" + serverIp + "/OVIO/TestTaskInfoUpdate.jsp" + "?clm_test_task_log=" + URLEncoder.encode(log_text_param) + "&clm_test_unit_seq=" + unit_no + "&clm_unit_ip=" + ipAddress + "&clm_product_serial_no=" + productSerialNo + "&clm_test_process_id=" + testProcessId + "&clm_model_id=" + globalModelId + decElectricValueList;

                                        testProcessId = (finalReceiveCommand.contains(Constants.TestItemCodes.RE0101)) ? "" : testProcessId;
                                        testTaskThread.execute();
                                        log_text_param = "";
                                    } catch (Exception e) {
                                        logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0148"), e);
                                    }
                                }
                            });
                        }

                        if (testItemCounter == Integer.parseInt(arrTestItems[testItemIdx][2])) {
                        }

                        if (isControlTestRunning[0]) {
                            synchronized (controlTestTimerLock) {
                                controlTestItemCounter = testItemCounter;

                                controlTestReceiveCommand = finalReceiveCommand;
                                controlTestReceiveResponse = finalReceiveCommandResponse;


                                if (controlTestItemCounter > Constants.TestThresholds.TEST_COUNTER_THRESHOLD_5) {
                                    boolean shouldStopTest = false;

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
                                                logInfo(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0149"));
                                            }
                                        }
                                    }

                                    if (shouldStopTest) {
                                        logInfo(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0150", controlCurrentTestItem));
                                        stopControlTestTimer();

                                        showControlModeTestResultDialog();
                                    }
                                }

                                entranceCheck = originalEntranceCheck[0];
                                testItemIdx = originalTestItemIdx[0];
                                testItemCounter = originalTestItemCounter[0];
                                currentTestItem = originalCurrentTestItem[0];
                            }
                        }


                        if (finalReceiveCommand.equals(Constants.TestItemCodes.RE0101)) {
                            try {
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

                                        runOnBtWorker(() -> executeResultUpload(finalCheckValue, finalLmsTestSeq));
                                    } catch (Exception e) {
                                        logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0151"), e);
                                    }
                                });

                                stopBtMessageTimer();

                                btSearchOnOff = false;
                                clearBluetoothReconnect();
                                btSocket = null;
                                deviceSelected = null;

                                final String finalLmsTestSeq = lmsTestSeq;
                                final String finalProductSerialNo = productSerialNo;
                                runOnBtWorker(() -> {
                                    try {
                                        SharedPreferences prefs = getApplicationContext().getSharedPreferences(
                                                Constants.SharedPrefKeys.TEST_COOKIE_INFO, MODE_PRIVATE);
                                        SharedPreferences.Editor editor = prefs.edit();
                                        editor.putString(Constants.IntentExtras.TEST_START_DATETIME, finalLmsTestSeq);
                                        editor.putString(Constants.SharedPrefKeys.TEST_PRODUCT_SERIAL_NO, finalProductSerialNo);
                                        editor.apply();

                                        runOnUiThread(() -> {
                                            cookie_preferences = prefs;
                                            cookie_info = editor;
                                        });

                                    } catch (Exception e) {
                                        logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0152"), e);
                                    }
                                });

                                scheduleUiUpdate(() -> {
                                    setDisplayLightValueChange(Constants.UI.DISPLAY_BRIGHTNESS);

                                    try {
                                        if (btSocket != null && btSocket.isConnected()) {
                                            try {
                                                btSocket.close();
                                            } catch (
                                                    IOException e) {
                                                logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0153"), e);
                                            }
                                        }
                                        tvConnectBtRamp.setBackgroundColor(getBaseContext().getResources().getColor(R.color.red_01));

                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }

                                    btnTestRestart.setBackgroundColor(getColor(R.color.green_02));
                                    btnTestRestart.setTextColor(getColor(R.color.white));


                                    if (testNgCnt > 0) {
                                        tvPopupProcessResult.setText(Constants.ResultStatus.NG);
                                        tvPopupProcessResult.setTextSize(Dimension.SP, Constants.UI.TEXT_SIZE_LARGE_SP);
                                        tvPopupProcessResultCompValue.setTextSize(Dimension.SP, Constants.UI.TEXT_SIZE_MEDIUM_SP);
                                        tvPopupProcessResultHeaterValue.setTextSize(Dimension.SP, Constants.UI.TEXT_SIZE_MEDIUM_SP);
                                    } else {
                                        tvPopupProcessResult.setText(Constants.ResultStatus.OK);
                                    }

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

                                    final String testResult = ((testNgCnt > 0) ? Constants.ResultStatus.NG : Constants.ResultStatus.OK);
                                    final ActivityModelTestProcess activity = getMainActivity();
                                    final String heaterValue = (activity != null) ? String.valueOf(activity.currentHeaterWattValueProp) : Constants.Common.ZERO;
                                    final String compValue = (activity != null) ? String.valueOf(activity.currentCompWattValueProp) : Constants.Common.ZERO;

                                    test_info.putString(Constants.SharedPrefKeys.TEST_RESULT, testResult);
                                    test_info.putString(Constants.SharedPrefKeys.HEATER_VALUE, heaterValue);
                                    test_info.putString(Constants.SharedPrefKeys.COMP_VALUE, compValue);

                                    runOnBtWorker(() -> {
                                        try {
                                            SharedPreferences prefs = getApplicationContext().getSharedPreferences(
                                                    Constants.SharedPrefKeys.TEST_COOKIE_INFO, MODE_PRIVATE);
                                            SharedPreferences.Editor editor = prefs.edit();
                                            editor.putString(Constants.SharedPrefKeys.TEST_RESULT, testResult);
                                            editor.putString(Constants.SharedPrefKeys.HEATER_VALUE, heaterValue);
                                            editor.putString(Constants.SharedPrefKeys.COMP_VALUE, compValue);
                                            editor.apply();
                                        } catch (Exception e) {
                                            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0154"), e);
                                        }
                                    });

                                    final List<Map<String, String>> testHistoryDetailList = new ArrayList<>();
                                    final int adapterCount = listItemAdapter.getCount();

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

                                        String testItemCommand = voTestItem.getTest_item_command();
                                        String medianValue = "";
                                        String upperValue = "";
                                        String lowerValue = "";
                                        if (lstDataTmp != null && testItemCommand != null) {
                                            for (Map<String, String> specData : lstDataTmp) {
                                                if (testItemCommand.equals(specData.get(Constants.JsonKeys.CLM_TEST_COMMAND))) {
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

                                    final Map<String, String> finalMapTestHistory = new HashMap<>(mapTestHistory);
                                    final List<Map<String, String>> finalLstDataTmp = lstDataTmp != null ? new ArrayList<>(lstDataTmp) : null;
                                    runOnBtWorker(() -> {
                                        try {
                                            TestData.insertProductTestHistory(getBaseContext(), finalMapTestHistory);

                                            TestData.createProductTestHistoryDetail(getBaseContext());

                                            for (Map<String, String> mapTestHistoryDetail : testHistoryDetailList) {
                                                TestData.insertProductTestHistoryDetail(getBaseContext(), mapTestHistoryDetail);
                                            }
                                        } catch (Exception e) {
                                            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0155"), e);
                                        }
                                    });

                                    processFinished = true;
                                    clTestResult.setVisibility(VISIBLE);

                                    tvTestOkCnt.setText(String.valueOf(testOkCnt));
                                    tvTestNgCnt.setText(String.valueOf(testNgCnt));


                                    synchronized (finishedRestartTimerLock) {
                                        if (finishedRestartTimerRunning.get()) {
                                            logWarn(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0156"));
                                            return;
                                        }

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
                                                    if (restartCntFinished == restartCntMargin) {
                                                        restartCntFinished = 0;
                                                        usbReconnectAttempts = 0;
                                                        finishedCorrectly = true;
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
                                            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0157"), e);
                                        }
                                    }
                                });
                            } catch (Exception e) {
                                logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0158"), e);
                            }
                        } else {
                            processFinished = false;
                        }


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
                        scheduleUiUpdate(() -> applyUiBundle(uiBundle));

                    } catch (Exception e) {
                        logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0159"), e);
                        scheduleUiUpdate(() -> {
                            sendBtMessage(finalCurrentTestItem);
                        });
                    }
                } catch (Exception e) {
                    logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0160"), e);
                    scheduleUiUpdate(() -> {
                        sendBtMessage(finalCurrentTestItem);
                    });
                }
            }
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0161"), e);
        }
    }

    private boolean processFinished = false;

    private void initializeAppStateAsync() {
        try {
            Intent intent = getIntent();
            globalModelId = intent.getStringExtra(Constants.IntentExtras.TEST_MODEL_ID);
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


            serverDomain = Constants.ServerConfig.SERVER_DOMAIN_192_168_0;
            serverIp = Constants.ServerConfig.SERVER_IP_192_168_0_47;
            serverDomain = Constants.ServerConfig.SERVER_DOMAIN_172_16_1;
            serverIp = Constants.ServerConfig.SERVER_IP_172_16_1_249_8080;
            if (mode_type.equals(Constants.ResultStatus.MODE_TYPE_TEST)) {
                serverIp = Constants.ServerConfig.SERVER_IP_PORT_ITF;
            } else {
                serverIp = Constants.ServerConfig.SERVER_IP_PORT;
            }

            urlStr = Constants.URLs.HTTP_PROTOCOL + serverIp + Constants.URLs.ENDPOINT_TEST_INFO_LIST
                    + Constants.Common.QUESTION + Constants.URLs.PARAM_CALL_TYPE + Constants.Common.AMPERSAND
                    + Constants.URLs.PARAM_MODEL_ID + globalModelId;

            Path directoryPath = Paths.get(Constants.FilePaths.FOLDER_NAME);
            try {
                Files.createDirectories(directoryPath);
                if (Files.exists(directoryPath)) {
                }
            } catch (IOException e) {
                logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0162"), e);
            }

            ipAddress = getLocalIpAddress();
            if (!TextUtils.isEmpty(ipAddress)) {
                int unit_no_from_ip = Integer.parseInt(ipAddress.split("\\.")[3]);
                unit_no = (unit_no_from_ip<=Constants.Network.IP_ADDRESS_OFFSET)?unit_no_from_ip:unit_no_from_ip - Constants.Network.IP_ADDRESS_OFFSET;
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
                    tvRunWsRamp.setBackgroundColor(getBaseContext().getResources().getColor(R.color.red_01));
                }
            });

            SharedPreferences testPrefs = getSharedPreferences(Constants.SharedPrefKeys.TEST_INFO, MODE_PRIVATE);
            String testResult = testPrefs.getString(Constants.SharedPrefKeys.TEST_RESULT, Constants.Common.EMPTY_STRING);
            String heaterValue = testPrefs.getString(Constants.SharedPrefKeys.HEATER_VALUE, Constants.Common.EMPTY_STRING);
            String compValue = testPrefs.getString(Constants.SharedPrefKeys.COMP_VALUE, Constants.Common.EMPTY_STRING);
            scheduleUiUpdate(() -> {
                tvUnitId.setText(Constants.Common.UNIT_ID_PREFIX + "-" + finalUnitNo);
                logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0163", isControlMode));
                if (TextUtils.isEmpty(testResult)) {
                    clTestResult.setVisibility(GONE);
                } else {
                    clTestResult.setVisibility(VISIBLE);
                    tvPopupProcessResult.setText(testResult);
                    tvPopupProcessResultCompValue.setText(compValue);
                    tvPopupProcessResultHeaterValue.setText(heaterValue);
                }
            });


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
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0164"), e);
            scheduleUiUpdate(() -> {
                clAlert.setVisibility(VISIBLE);
                tvAlertMessage.setText(getStringResource("ui.message.server_connection_failed"));
            });
        }
    }

    private void loadTemperatureData() {
        try {
            hotTemperatureData = TestData.selectTemperatureData(this, Constants.Database.TEMPERATURE_TYPE_HOT);
            coldTemperatureData = TestData.selectTemperatureData(this, Constants.Database.TEMPERATURE_TYPE_COLD);
            if (hotTemperatureData.isEmpty() || coldTemperatureData.isEmpty()) {
                readTemperatureExcel(Constants.Database.TEMPERATURE_TYPE_HOT, Constants.FilePaths.TEMPERATURE_INFO_XLS);
                readTemperatureExcel(Constants.Database.TEMPERATURE_TYPE_COLD, Constants.FilePaths.TEMPERATURE_INFO_XLS);
                hotTemperatureData = TestData.selectTemperatureData(this, Constants.Database.TEMPERATURE_TYPE_HOT);
                coldTemperatureData = TestData.selectTemperatureData(this, Constants.Database.TEMPERATURE_TYPE_COLD);
            }
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0165"), e);
        }
    }

    private void loadTestSpecData() {
        AtomicBoolean specDataLoadedFromCache = new AtomicBoolean(false);
        try {
            lstSpecData = TestData.selectTestSpecData(getBaseContext(),
                    Constants.Database.QUERY_AND_1_EQUALS_1 + Constants.Common.SPACE + Constants.Database.QUERY_AND + Constants.Common.SPACE +
                            Constants.JsonKeys.CLM_MODEL_ID + Constants.Common.EQUAL + Constants.Common.SINGLE_QUETATION + globalModelId + Constants.Common.SINGLE_QUETATION);
            if (lstSpecData != null && !lstSpecData.isEmpty()) {
                for (int i = 0; i < lstSpecData.size(); i++) {
                    logDebug(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0166", i, lstSpecData.get(i)));
                    if (i == 0) {
                        test_version_id = lstSpecData.get(i).get("clm_test_version_id");
                        model_id = lstSpecData.get(i).get("clm_model_id");
                    }
                }
                scheduleUiUpdate(() -> {
                    try {
                        specDataLoadedFromCache.set(applyTestSpecData(lstSpecData, false));
                        lstDataTmp = lstSpecData;
                        if (!specDataLoadedFromCache.get()) {
                            try {
                                clearHttpHandlerQueue();
                                new RequestThreadAsync(ActivityModelTestProcess.this).execute();
                            } catch (Exception e) {
                                logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0167"), e);
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
                                    new RequestVersionInfoThreadAsync(ActivityModelTestProcess.this, localVersionId, localModelVersion).execute();
                                    break;
                                default:
                                    try {
                                        clearHttpHandlerQueue();
                                        new RequestThreadAsync(ActivityModelTestProcess.this).execute();
                                    } catch (Exception e) {
                                        logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0167"), e);
                                        clAlert.setVisibility(VISIBLE);
                                        tvAlertMessage.setText(getStringResource("ui.message.server_connection_failed"));
                                    }
                                    break;
                            }
                        }
                    } catch (Exception e) {
                        logWarn(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0168", e.getMessage()));
                    }
                });
            } else {
                scheduleUiUpdate(() -> {
                    try {
                        clearHttpHandlerQueue();
                        new RequestThreadAsync(ActivityModelTestProcess.this).execute();
                    } catch (Exception e) {
                        logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0167"), e);
                        clAlert.setVisibility(VISIBLE);
                        tvAlertMessage.setText(getStringResource("ui.message.server_connection_failed"));
                    }
                });
            }
        } catch (Exception e) {
            logWarn(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0168", e.getMessage()));
            scheduleUiUpdate(() -> {
                try {
                    clearHttpHandlerQueue();
                    new RequestThreadAsync(ActivityModelTestProcess.this).execute();
                } catch (Exception ex) {
                    logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0167"), ex);
                }
            });
        }
    }

    private void executeResultUpload(String checkValue, String lmsSeq) {
        String httpSuccessYn = Constants.ResultStatus.NO;
        String targetUrl = Constants.URLs.HTTP_PROTOCOL + serverIp + Constants.URLs.ENDPOINT_UPDATE_RESULT_TEST_INFO
                + Constants.Common.QUESTION
                + Constants.URLs.PARAM_PRODUCT_SERIAL_NO + productSerialNo
                + Constants.Common.AMPERSAND + Constants.URLs.PARAM_TEST_PROCESS + 3
                + Constants.Common.AMPERSAND + Constants.URLs.PARAM_TEST_RESULT + ((testNgCnt > 0) ? Constants.ResultStatus.NG : Constants.ResultStatus.OK)
                + checkValue;


        boolean requestSuccess = false;
        for (int attempt = 1; attempt <= Constants.Network.HTTP_RETRY_COUNT; attempt++) {
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
                    sendResultYn = Constants.ResultStatus.YES;
                    httpSuccessYn = Constants.ResultStatus.YES;
                    requestSuccess = true;
                    break;
                } else {
                    logWarn(LogManager.LogCategory.BI, getStringResource("log.activity_model_test_process.0169", attempt, responseCode));
                    if (attempt < Constants.Network.HTTP_RETRY_COUNT) {
                        Thread.sleep(Constants.Network.HTTP_RETRY_DELAY_MS);
                    }
                }
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0170", attempt, e.getMessage()), e);
                if (attempt < Constants.Network.HTTP_RETRY_COUNT) {
                    try {
                        Thread.sleep(Constants.Network.HTTP_RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0171"), ie);
                    }
                }
            } finally {
                safeDisconnectConnection(connection);
            }
        }

        if (!requestSuccess) {
        }

        try {
            TestData.updateProductTestHistoryHttpSuccess(getBaseContext(), lmsSeq, httpSuccessYn);
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0172"), e);
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
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0173"), e);
        }
    }

    private void usbSearch() {
        try {
            final String ACTION_USB_PERMISSION = "itf.com.app.USB_PERMISSION";
            UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
            if (usbManager == null) {
                logWarn(LogManager.LogCategory.US, getStringResource("log.activity_model_test_process.0174"));
                return;
            }

            HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
            if (deviceList == null || deviceList.isEmpty()) {
                return;
            }

            for (UsbDevice device : deviceList.values()) {

                if (!usbManager.hasPermission(device)) {
                    Intent intent = new Intent(ACTION_USB_PERMISSION);
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                    usbManager.requestPermission(device, pendingIntent);
                } else {
                    if (usbService != null) {
                    } else {
                        Intent usbIntent = new Intent(getApplicationContext(), UsbService.class);
                        try {
                            stopService(usbIntent);
                        } catch (Exception ignored) {
                        }
                        try {
                            startService(usbIntent);
                        } catch (Exception e) {
                            logWarn(LogManager.LogCategory.US, getStringResource("log.activity_model_test_process.0175", e.getMessage()));
                        }
                    }
                }
                break; // 첫 번째 장치만 처리(필요 시 제거)
            }
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0176"), e);
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
        return minute;
    }

    private void finishApplication(Context mContext) {
        new Thread(() -> {
            try {
                logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0177"));
                receiveCompAgingResponse = Constants.ResultStatus.COMP_AGING_RESPONSE_01;
                testTotalCounter = 0;
                testItemCounter = 0;
                testItemIdx = 0;
                entranceCheck = false;
                btSearchOnOff = false; // 재탐색을 위해 플래그 초기화
                logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0178"));
                clearBluetoothReconnect();
                logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0179"));


                if (btConnectedThread != null) {
                    try {
                        btConnectedThread.cancel();
                        logInfo(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0180"));
                    } catch (Exception e) {
                        logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0181", e.getMessage()));
                    }
                    btConnectedThread = null;
                }

                clearBtHandlerQueue();
                logInfo(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0182"));

                btConnected = false;
                btConnectionInProgress = false;

                if (btSocket != null && btSocket.isConnected()) {
                    try {
                        btSocket.close();
                        logInfo(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0183"));
                    } catch (IOException e) {
                        logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0184", e.getMessage()));
                    }
                    btSocket = null;
                } else if (btSocket != null) {
                    btSocket = null;
                }

                logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0185"));
                deviceSelected = null;

                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    try {
                        stopBtMessageTimer();
                        new Thread(() -> {
                            try {
                                bluetoothSearch();
                            } catch (Exception e) {
                                logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0186"), e);
                            }
                        }).start();
                    } catch (Exception e) {
                        logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0187"), e);
                    }
                });// Java
                logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0188"));
                logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0189"));

                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    try {
                        try {
                            if (usbService != null) {
                                usbService = null;
                            }
                        } catch (Exception ignored) {
                        }

                        try {
                            Intent usbIntent = new Intent(getApplicationContext(), UsbService.class); // 존재하는 서비스 클래스로 변경
                            try {
                                stopService(usbIntent);
                            } catch (Exception ignored) {
                            }
                            startService(usbIntent);
                        } catch (Exception e) {
                            logWarn(LogManager.LogCategory.US, getStringResource("log.activity_model_test_process.0190", e.getMessage()));
                        }

                        try {
                            usbSearch();
                        } catch (Exception ignored) {
                        }
                    } catch (Exception e) {
                        logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0187"), e);
                    }
                });
                logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0191"));
                currentTestItem = ST0101;

                cleanupAllTimers();


                if (usbReceiverRegisted) {
                }
                if (usbConnPermissionGranted) {
                    unbindService(usbConnection);
                }
                logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0192"));

                usbReceiverRegisted = false;
                usbConnPermissionGranted = false;

                UsbService.SERVICE_CONNECTED = false;


                cancelDiscoverySafe();
                logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0193"));

                try {
                    Thread.sleep(Constants.Timeouts.THREAD_SLEEP_MS);
                } catch (Exception e) {
                    logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0194"), e);
                }

                clearMainActivityReference();
                logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0195"));

                Intent intent = getIntent();

                String intentModelId = intent.getStringExtra(Constants.IntentExtras.TEST_MODEL_ID);
                String intentModelName = intent.getStringExtra(Constants.IntentExtras.TEST_MODEL_NAME);
                String intentModelNation = intent.getStringExtra(Constants.IntentExtras.TEST_MODEL_NATION);
                String intentTestStartDatetime = intent.getStringExtra(Constants.IntentExtras.TEST_START_DATETIME);


                logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0196", (globalModelId != null ? globalModelId : "null"), (globalModelName != null ? globalModelName : "null"), (globalModelNation != null ? globalModelNation : "null")));

                SharedPreferences prefs = getSharedPreferences(Constants.SharedPrefKeys.TEST_COOKIE_INFO, MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();

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
                logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0197"));

                String savedModelId = prefs.getString(Constants.IntentExtras.TEST_MODEL_ID, null);
                String savedModelName = prefs.getString(Constants.IntentExtras.TEST_MODEL_NAME, null);
                String savedModelNation = prefs.getString(Constants.IntentExtras.TEST_MODEL_NATION, null);

                logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0198", (savedModelId != null ? savedModelId : "null"), (savedModelName != null ? savedModelName : "null"), (savedModelNation != null ? savedModelNation : "null")));

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
                logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0199"));

                String finalIntentModelId = intent.getStringExtra(Constants.IntentExtras.TEST_MODEL_ID);
                String finalIntentModelName = intent.getStringExtra(Constants.IntentExtras.TEST_MODEL_NAME);
                String finalIntentModelNation = intent.getStringExtra(Constants.IntentExtras.TEST_MODEL_NATION);
                String finalIntentTestStartDatetime = intent.getStringExtra(Constants.IntentExtras.TEST_START_DATETIME);

                logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0200", (finalIntentModelId != null ? finalIntentModelId : "null"), (finalIntentModelName != null ? finalIntentModelName : "null"), (finalIntentModelNation != null ? finalIntentModelNation : "null"), (finalIntentTestStartDatetime != null ? finalIntentTestStartDatetime : "null")));

                final Intent finalIntent = intent;
                runOnUiThread(() -> {
                    try {
                        finish();
                        logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0201"));
                        finish();
                    } catch (Exception e) {
                        logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0202"), e);
                    }
                });
                logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0203"));
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0204"), e);
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




    public String getUrlTestTaskStr() {
        return urlTestTaskStr;
    }

    public void postToScheduler(Runnable task) {
        if (task == null) {
            return;
        }
        schedulerHandler.post(task);
    }

    public String getUrlStr() {
        return urlStr;
    }

    public String getServerIp() {
        return serverIp;
    }

    public int getUnitNo() {
        return unit_no;
    }

    public void updateRunWsRampConnected(int unitNo) {
        scheduleUiUpdate(() -> {
            if (tvRunWsRamp != null) {
                tvRunWsRamp.setText(String.valueOf(unitNo));
                tvRunWsRamp.setBackgroundColor(getBaseContext().getResources().getColor(R.color.blue_01));
            }
        });
    }

    public void updateRunWsRampDisconnected() {
        scheduleUiUpdate(() -> {
            if (tvRunWsRamp != null) {
                tvRunWsRamp.setBackgroundColor(getBaseContext().getResources().getColor(R.color.red_01));
            }
        });
    }

    public void scheduleHttpParsing(String finalData) {
        if (finalData == null || finalData.trim().isEmpty()) {
            return;
        }
        synchronized (HTTP_HANDLER_LOCK) {
            if (pendingHttpTask != null) {
                schedulerHandler.removeCallbacks(pendingHttpTask);
            }

            pendingHttpTask = new Runnable() {
                @Override
                public void run() {
                    try {
                        logDebug(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0205"));

                        new Thread(() -> {
                            try {
                                logDebug(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0206"));
                                TestData.deleteTestSpecData(getApplicationContext());
                                logDebug(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0207"));
                            } catch (Exception e) {
                                Thread.currentThread().interrupt();
                            }
                        }).start();

                        jsonParsing("test_spec", finalData);
                    } catch (Exception e) {
                        logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0208"), e);
                    } finally {
                        synchronized (HTTP_HANDLER_LOCK) {
                            pendingHttpTask = null;
                        }
                    }
                }
            };

            schedulerHandler.postDelayed(() -> {
                btWorkerExecutor.execute(pendingHttpTask);
            }, HTTP_DEBOUNCE_DELAY_MS);
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
                logWarn(LogManager.LogCategory.SI, getStringResource("log.activity_model_test_process.0209"));
                return;
            }

            JSONObject testItemObject = testItemArray.getJSONObject(0);
            arrTestModels[0][0] = testItemObject.getString(Constants.JsonKeys.CLM_MODEL_ID);
            arrTestModels[0][1] = testItemObject.getString(Constants.JsonKeys.CLM_MODEL_NAME);
            arrTestModels[0][2] = testItemObject.getString(Constants.JsonKeys.CLM_MODEL_NATION);
            arrTestModels[0][3] = testItemObject.getString(Constants.JsonKeys.CLM_TEST_VERSION_ID);
            arrTestModels[0][4] = testItemObject.getString(Constants.JsonKeys.CLM_MODEL_VERSION);

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
                mapData.put(Constants.JsonKeys.CLM_TEST_VERSION_ID, testVersionId);
                mapData.put(Constants.JsonKeys.CLM_MODEL_ID, modelId);
                lstData.add(mapData);
            }


            applyTestSpecData(lstData, true);

            if (specSignature != null) {
                lastSpecSignature = specSignature;
            }
        } catch (JSONException e) {
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0208"), e);
        }
    }

    public void bluetoothSearch() {
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
                    permissionDenialCount = 0;
                    btPermissionsGranted = true;
                    runPendingPermissionActions();
                } else {
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
    }

    private void getPreferences() {
        scheduleUiUpdate(() -> {
            tvSerialNo.setTextColor(postTestInfoColor);
        });
    }

    public static ActivityModelTestProcess getMainActivity() {
        if (mainActivityRef != null) {
            ActivityModelTestProcess activity = mainActivityRef.get();
            if (activity == null) {
                mainActivityRef = null;
            }
            return activity;
        }
        return null;
    }

    public static Context getMainActivityContext() {
        ActivityModelTestProcess activity = getMainActivity();
        return activity != null ? activity.getApplicationContext() : null;
    }

    private static void clearMainActivityReference() {
        if (mainActivityRef != null) {
            mainActivityRef.clear();
            mainActivityRef = null;
        }
    }

    public void clearHttpHandlerQueue() {
        synchronized (HTTP_HANDLER_LOCK) {
            if (schedulerHandler != null) {
                schedulerHandler.removeCallbacksAndMessages(null);
            }
            pendingHttpTask = null;
        }
    }

    public static void safeDisconnectConnection(HttpURLConnection connection) {
        if (connection != null) {
            try {
                connection.disconnect();
            } catch (Exception e) {
                LogManager.e(LogManager.LogCategory.ER, TAG, "Error disconnecting connection", e);
            }
        }
    }

    private void clearBtHandlerQueue() {
        if (btHandler != null) {
            btHandler.removeCallbacksAndMessages(null);
        }
    }


    public void logInfo(LogManager.LogCategory category, String message) {
        String logMessage = message.startsWith("log.")
                ? StringResourceManager.getInstance().getString(message)
                : message;
        logManager.info(category, TAG, logMessage);
    }

    public void logDebug(LogManager.LogCategory category, String message) {
        String logMessage = message.startsWith("log.")
                ? StringResourceManager.getInstance().getString(message)
                : message;
        logManager.debug(category, TAG, logMessage);
    }

    public void logWarn(LogManager.LogCategory category, String message) {
        String logMessage = message.startsWith("log.")
                ? StringResourceManager.getInstance().getString(message)
                : message;
        logManager.warn(category, TAG, logMessage);
    }

    public void logError(LogManager.LogCategory category, String message, Throwable throwable) {
        String logMessage = message.startsWith("log.")
                ? StringResourceManager.getInstance().getString(message)
                : message;
        logManager.error(category, TAG, logMessage, throwable);
    }

    private void clearLogBatchQueue() {
        logManager.clearLogBatchQueue();
    }

    private void logBtTestResponse(String command, String response, double electricValue,
                                   int wattLower, int wattUpper, String messageInfo) {
        String msg = String.format(Constants.LogMessages.TEST_ITEM_SIGNAL_RECEIVE + Constants.Common.EMPTY_STRING + ">> [T:%d/%d][C:%d/%s][P:%d/%d][S:%s][G:%d=%d-%d][R:%s]%s %d < %.0f < %d",
                testTotalCounter, totalTimeCnt, testItemCounter,
                (testItemIdx < arrTestItems.length && arrTestItems[testItemIdx] != null) ? arrTestItems[testItemIdx][2] : "0",
                testItemIdx, arrTestItems.length,
                currentTestItem, disconnectCheckCount, receivedMessageCnt, sendingMessageCnt,
                readMessage, (messageInfo != null ? "[I:" + messageInfo + "]" : ""),
                wattLower, electricValue, wattUpper);
        logInfo(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0006", msg));
    }

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
        logInfo(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0006", msg));
    }

    private void cleanupAllTimers() {
        logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0210"));

        try {
            stopBtConnectionIndicator();
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0211"), e);
        }

        try {
            logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0212"));
            stopUsbPolling();
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0213"), e);
        }

        try {
            stopBtMessageTimer();
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0214"), e);
        }

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
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0215"), e);
        }

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
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0216"), e);
        }

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
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0217"), e);
        }

        try {
            if (checkDuration != null) {
                checkDuration.cancel();
                checkDuration.purge();
                checkDuration = null;
            }
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0218"), e);
        }

        try {
            if (barcodeRequestTimer != null) {
                barcodeRequestTimer.cancel();
                barcodeRequestTimer.purge();
                barcodeRequestTimer = null;
            }
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0219"), e);
        }

        try {
            if (appResetTimerTask != null) {
                appResetTimerTask.cancel();
                appResetTimerTask = null;
            }
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0220"), e);
        }

        logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0221"));
    }

    private void runOnBtWorker(Runnable task) {
        if (task == null) {
            return;
        }
        if (btWorkerExecutor == null || btWorkerExecutor.isShutdown() || btWorkerExecutor.isTerminated()) {
            logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0222"));
            return;
        }
        try {
            btWorkerExecutor.execute(task);
        } catch (java.util.concurrent.RejectedExecutionException e) {
            logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0223", e.getMessage()));
        }
    }

    private String[] getRequiredPermissions() {
        String[] btPermissions = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ? MODERN_BT_PERMISSIONS
                : LEGACY_BT_PERMISSIONS;

        String[] mediaPermissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mediaPermissions = MODERN_MEDIA_PERMISSIONS;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            mediaPermissions = LEGACY_STORAGE_PERMISSIONS_API30_UP;
        } else {
            mediaPermissions = LEGACY_STORAGE_PERMISSIONS_API29_DOWN;
        }

        String[] allPermissions = new String[btPermissions.length + mediaPermissions.length];
        System.arraycopy(btPermissions, 0, allPermissions, 0, btPermissions.length);
        System.arraycopy(mediaPermissions, 0, allPermissions, btPermissions.length, mediaPermissions.length);

        return allPermissions;
    }

    private boolean hasBluetoothConnectPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
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
            logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0224"));
            ensureBtPermissions(PermissionAction.LIST_PAIRED_DEVICES);
            return Collections.emptySet();
        }
        try {
            return mBTAdapter.getBondedDevices();
        } catch (SecurityException e) {
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0225"), e);
            return Collections.emptySet();
        }
    }

    private void cancelDiscoverySafe() {
        if (mBTAdapter == null) {
            return;
        }
        if (!hasBluetoothScanPermission()) {
            logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0226"));
            ensureBtPermissions(null);
            return;
        }
        try {
            mBTAdapter.cancelDiscovery();
        } catch (SecurityException e) {
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0227"), e);
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
        logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0228", getRequiredPermissions().length));
        List<String> missingPermissions = new ArrayList<>();
        for (String permission : getRequiredPermissions()) {
            logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0229", permission, ActivityCompat.checkSelfPermission(this, permission)));
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        requestRuntimePermissions();
        return false;
    }

    private void requestRuntimePermissions() {
        if (permissionRequestInProgress) {
            logInfo(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0083"));
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

    public void resetBluetoothSessionKeepUsb() {
        logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0230"));
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

        scheduleUiUpdate(() -> {
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
            tvConnectPlcRamp.setBackgroundColor(getColor(R.color.red_01));
            tvRunWsRamp.setBackgroundColor(getColor(R.color.red_01));
            logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0231", testOkCnt, testNgCnt));
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

        rebuildTestItemListAsync();
        restartUsbMonitoring();   // ▼ 새 헬퍼 호출
        btReconnectDelayMs = Constants.Timeouts.BT_LIST_TIMER_INTERVAL_MS; // 예: 기존 값의 1/4
        btSearchOnOff = true;
        scheduleBluetoothReconnect(true);

        String localVersionId = null;
        String localModelId = null;


        if (arrTestModels != null && arrTestModels[0] != null && arrTestModels[0].length > 4) {
            localVersionId = arrTestModels[0][3]; // CLM_TEST_VERSION_ID
            localModelId = arrTestModels[0][4]; // CLM_MODEL_VERSION
        }
        localVersionId = test_version_id;
        localModelId = model_id;

        if (mode_type.equals(Constants.ResultStatus.MODE_TYPE_TEST)) {
            if (localVersionId != null || localModelId != null) {
                new RequestVersionInfoThreadAsync(this, localVersionId, localModelId).execute();
            }
        }
    }

    private void restartUsbMonitoring() {
        if (usbService == null || !UsbService.SERVICE_CONNECTED) {
            startService(UsbService.class, usbConnection, null);
        }
        if (!usbConnPermissionGranted) {
            usbSearch();
            return; // 권한 승인 브로드캐스트에서 polling이 재개됨
        }
        logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0232"));
        startUsbPolling(true);
        scheduleUiUpdate(() -> {
            tvConnectPlcRamp.setBackgroundColor(getColor(R.color.blue_01));
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



    private void startUsbPolling(boolean immediate) {
        if (usbPollingExecutor.isShutdown()) {
            logWarn(LogManager.LogCategory.US, getStringResource("log.activity_model_test_process.0233"));
            return;
        }
        if (usbService == null) {
            logWarn(LogManager.LogCategory.US, getStringResource("log.activity_model_test_process.0234"));
            return;
        }
        if (!usbConnPermissionGranted) {
            logWarn(LogManager.LogCategory.US, getStringResource("log.activity_model_test_process.0235"));
            scheduleUsbPermissionRecovery();
            return;
        }

        if (usbCommandQueue == null) {
            usbCommandQueue = new UsbCommandQueue();
            usbCommandQueue.setUsbService(usbService);
            usbCommandQueue.start();
            logInfo(LogManager.LogCategory.US, getStringResource("log.activity_model_test_process.0236"));
        }

        boolean pollingActive = usbPollingEnabled && usbPollingFuture != null && !usbPollingFuture.isCancelled();
        if (pollingActive) {
            logDebug(LogManager.LogCategory.US, getStringResource("log.activity_model_test_process.0237"));
            return;
        }
        logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0238"));
        stopUsbPolling();
        usbPollingEnabled = true;
        usbPollingRequested = true;
        usbPollingFailureCount = 0;
        long initialDelay = immediate ? 0 : usbPollingIntervalMs;
        try {
            logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0239", usbPollingEnabled, usbService));
            usbPollingFuture = usbPollingExecutor.scheduleAtFixedRate(() -> {
                if (!usbPollingEnabled) {
                    logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0240"));
                    stopUsbPolling();
                    return;
                }
                if (usbService == null || usbCommandQueue == null) {
                    logWarn(LogManager.LogCategory.US, getStringResource("log.activity_model_test_process.0241"));
                    logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0242"));
                    stopUsbPolling();
                    return;
                }
                try {
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
                        logWarn(LogManager.LogCategory.US, getStringResource("log.activity_model_test_process.0243"));
                    }

                    if (usbPollingFailureCount >= USB_POLLING_FAILURE_THRESHOLD) {
                        logWarn(LogManager.LogCategory.US, getStringResource("log.activity_model_test_process.0244"));
                        stopUsbPolling();
                        usbPollingIntervalMs = USB_POLLING_BACKOFF_MS;
                        startUsbPolling(false);
                    }
                } catch (Exception e) {
                    usbPollingFailureCount++;
                    logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0245"), e);
                    if (usbPollingFailureCount >= USB_POLLING_FAILURE_THRESHOLD) {
                        logWarn(LogManager.LogCategory.US, getStringResource("log.activity_model_test_process.0244"));
                        stopUsbPolling();
                        usbPollingIntervalMs = USB_POLLING_BACKOFF_MS;
                        startUsbPolling(false);
                    }
                }
            }, initialDelay, usbPollingIntervalMs, TimeUnit.MILLISECONDS);
            logInfo(LogManager.LogCategory.US, getStringResource("log.activity_model_test_process.0246"));
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0247"), e);
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
            logInfo(LogManager.LogCategory.US, getStringResource("log.activity_model_test_process.0248"));
        }

    }

    public boolean sendUsbCommand(String command, String description, Runnable onSuccess, Runnable onError) {
        if (usbCommandQueue == null || !usbCommandQueue.isRunning()) {
            logWarn(LogManager.LogCategory.US, getStringResource("log.activity_model_test_process.0249", description));
            if (onError != null) {
                onError.run();
            }
            return false;
        }

        if (command == null || command.trim().isEmpty()) {
            logWarn(LogManager.LogCategory.US, getStringResource("log.activity_model_test_process.0250", description));
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

        boolean enqueued = usbCommandQueue.enqueuePriority(usbCommand);
        if (enqueued) {
            logInfo(LogManager.LogCategory.US, getStringResource("log.activity_model_test_process.0251", description));
        } else {
            logWarn(LogManager.LogCategory.US, getStringResource("log.activity_model_test_process.0252", description));
            if (onError != null) {
                onError.run();
            }
        }

        return enqueued;
    }

    public boolean sendUsbCommand(String command, String description) {
        return sendUsbCommand(command, description, null, null);
    }

    public CompletableFuture<String> sendUsbCommandWithResponse(String command, String description, long timeoutMs) {
        CompletableFuture<String> future = new CompletableFuture<>();

        if (usbCommandQueue == null || !usbCommandQueue.isRunning()) {
            future.completeExceptionally(new RuntimeException("USB command queue is not running"));
            return future;
        }

        String responseKey = "resp_" + System.currentTimeMillis() + "_" + description.hashCode();

        synchronized (usbResponseLock) {
            pendingUsbResponses.put(responseKey, future);
        }

        Runnable onSuccess = () -> {
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

    private void handleUsbResponse(String response) {
        if (response == null || response.isEmpty()) {
            return;
        }

        synchronized (usbResponseLock) {
            if (!pendingUsbResponses.isEmpty()) {
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
            logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0253"));
            startUsbPolling(true);
            return;
        }
        usbReconnectAttempts++;
        if (usbReconnectAttempts >= USB_RETRY_MAX_ATTEMPTS) {
            usbReconnectAttempts = 0;
            logWarn(LogManager.LogCategory.US, getStringResource("log.activity_model_test_process.0254"));
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
            logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0255"));
            stopUsbPolling();
            setFilters();
            startService(UsbService.class, usbConnection, null);
            return usbConnPermissionGranted && usbService != null;
        } catch (Exception e) {
            logError(LogManager.LogCategory.US, getStringResource("log.activity_model_test_process.0256"), e);
            return false;
        }
    }

    private boolean isUsbReady() {
        logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0257", usbConnPermissionGranted, usbService, usbPollingEnabled));
        if (usbConnPermissionGranted) {
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
        logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0258", isUsbReady(), isBluetoothReady()));
        return isUsbReady() && isBluetoothReady();
    }

    private void updateUsbLampDisconnected() {
        scheduleUiUpdate(() -> {
            tvConnectPlcRamp.setBackgroundColor(getColor(R.color.red_01));
        });
    }

    private void updateUsbLampReconnecting() {
        scheduleUiUpdate(() -> {
            tvConnectPlcRamp.setBackgroundColor(getColor(R.color.orange_01));
        });
    }

    private void updateUsbLampReady() {
        scheduleUiUpdate(() -> {
            tvConnectPlcRamp.setBackgroundColor(getColor(R.color.blue_01));
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





    private void scheduleUsbPermissionRecovery() {
        if (usbConnPermissionGranted) {
            logDebug(LogManager.LogCategory.US, getStringResource("log.activity_model_test_process.0259"));
            return;
        }

        if (usbPermissionRecoveryRunnable == null) {
            usbPermissionRecoveryRunnable = () -> {
                if (usbConnPermissionGranted) {
                    logDebug(LogManager.LogCategory.US, getStringResource("log.activity_model_test_process.0260"));
                    return;
                }
                scheduleUiUpdate(() -> {
                    final int finalUsbStatusColor = R.color.red_01;
                    tvConnectPlcRamp.setBackgroundColor(getColor(finalUsbStatusColor));
                });
                logInfo(LogManager.LogCategory.US, getStringResource("log.activity_model_test_process.0261"));
                try {
                    setFilters();
                } catch (Exception e) {
                    logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0262"), e);
                }

                new Thread(() -> {
                    try {
                        startService(UsbService.class, usbConnection, null);
                    } catch (Exception e) {
                        logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0263"), e);
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

                int color = useOrange[0] ? R.color.orange_01 : R.color.yellow_01;
                useOrange[0] = !useOrange[0]; // 다음 색상으로 토글

                scheduleUiUpdate(() -> {
                    if (tvConnectBtRamp != null && btConnectionIndicatorRunning) {
                        tvConnectBtRamp.setBackgroundColor(getColor(color));
                    }
                });

                if (btConnectionIndicatorRunning && schedulerHandler != null) {
                    schedulerHandler.postDelayed(this, BT_CONNECTION_INDICATOR_INTERVAL_MS);
                }
            }
        };

        schedulerHandler.post(btConnectionIndicatorRunnable);
    }

    private void stopBtConnectionIndicator() {
        if (!btConnectionIndicatorRunning) {
            return; // 실행 중이 아니면 중복 중지 방지
        }

        btConnectionIndicatorRunning = false;

        if (btConnectionIndicatorRunnable != null && schedulerHandler != null) {
            schedulerHandler.removeCallbacks(btConnectionIndicatorRunnable);
            btConnectionIndicatorRunnable = null;
        }

        scheduleUiUpdate(() -> {
            if (tvConnectBtRamp != null) {
                if (btConnected) {
                    tvConnectBtRamp.setBackgroundColor(getColor(R.color.blue_01));
                } else {
                    tvConnectBtRamp.setBackgroundColor(getColor(R.color.red_01));
                }
            }
        });

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

        stopBtConnectionIndicator();
    }

    @SuppressLint("MissingPermission")
    private void attemptBluetoothReconnect() {
        if (!btSearchOnOff || btConnectionInProgress || btConnected) {
            return;
        }
        if (mBTAdapter == null) {
            logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0264"));
            return;
        }

        if (!hasBluetoothConnectPermission()) {
            logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0265"));
            ensureBtPermissions(PermissionAction.BT_RECONNECT);
            return;
        }

        if (!mBTAdapter.isEnabled()) {
            logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0266"));
            return;
        }

        if (mPairedDevices == null || mPairedDevices.isEmpty()) {
            mPairedDevices = getBondedDevicesSafe();
        }

        if (mPairedDevices == null || mPairedDevices.isEmpty()) {
            logInfo(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0267"));
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
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0153"), e);
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
                logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0268"));
                return false;
            }

            btDeviceName = device.getName();
            btDeviceAddr = device.getAddress();

            boolean alreadyConnected = isDeviceConnectedSafe(device);
            if (alreadyConnected && entranceCheck) {
                logInfo(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0269", btDeviceName, btDeviceAddr));
                return true;
            }

            deviceSelected = device;

            if (btSocket != null && btSocket.isConnected()) {
                closeSocketSilently(btSocket);
            }

            tempSocket = createBluetoothSocket(deviceSelected);
            btSocket = tempSocket;

            if (Looper.myLooper() == Looper.getMainLooper()) {
                logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0270"), new RuntimeException("Stack trace"));
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

            stopBtConnectionIndicator();

            return true;
        } catch (IOException e) {
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0271"), e);
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
            logError(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0272"), e);
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

        List<Map<String, String>> normalizedList = new ArrayList<>();
        for (Map<String, String> item : sourceData) {
            normalizedList.add(new HashMap<>(item));
        }

        lstSpecData = normalizedList;
        refreshSpecCache(normalizedList);

        new Thread(() -> {
            Context context = getApplicationContext();
            int tempTotalTimeCnt = 0;
            String[][] tempArrTestItems = new String[normalizedList.size()][10];
            String tempValueWatt = Constants.Common.EMPTY_STRING;
            String tempLowerValueWatt = Constants.Common.EMPTY_STRING;
            String tempUpperValueWatt = Constants.Common.EMPTY_STRING;
            String tempProductSerialNo = Constants.Common.EMPTY_STRING;

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
                    tempArrTestItems[i][8] = "0";
                    tempArrTestItems[i][9] = "0";
                }

                tempValueWatt = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_VALUE_WATT));
                tempLowerValueWatt = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT));
                tempUpperValueWatt = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT));
                tempProductSerialNo = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_PRODUCT_SERIAL_NO));
            }

            int calculatedTotalTimeCnt = 0;
            for (int i = 0; i < tempArrTestItems.length; i++) {
                try {
                    int seconds = Integer.parseInt(tempArrTestItems[i][2]);
                    calculatedTotalTimeCnt += (seconds > 1) ? seconds + 1 : seconds;
                    tempArrTestItems[i][3] = String.valueOf(calculatedTotalTimeCnt);
                } catch (NumberFormatException e) {
                    tempArrTestItems[i][3] = String.valueOf(calculatedTotalTimeCnt);
                }
            }

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
                    logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0273"), e);
                }
            }


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




    private String resolveDialogCommand(String command) {
        if (command == null) {
            return null;
        }
        if (command.contains(Constants.TestItemCodes.SN0101)) {
            return Constants.TestItemCodes.SN0101;
        }
        if (command.contains(Constants.TestItemCodes.SN0201)) {
            return Constants.TestItemCodes.SN0201;
        }
        if (command.contains(Constants.TestItemCodes.SN0301)) {
            return Constants.TestItemCodes.SN0301;
        }
        if (command.contains(Constants.TestItemCodes.TA0101)) {
            return Constants.TestItemCodes.TA0101;
        }
        if (command.contains(Constants.TestItemCodes.TA0201)) {
            return Constants.TestItemCodes.TA0201;
        }
        return null;
    }

    private void scheduleUiUpdate(Runnable update) {
        if (update == null) {
            return;
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            try {
                update.run();
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0274"), e);
            }
            return;
        }

        synchronized (uiUpdateLock) {
            pendingUiUpdates.add(update);

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

                    for (Runnable r : updates) {
                        try {
                            r.run();
                        } catch (Exception e) {
                            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0275"), e);
                        }
                    }
                };

                mainHandler.postDelayed(uiUpdateBatchTask, UI_UPDATE_BATCH_DELAY_MS);
            }
        }
    }


    private void clearUiUpdateBatchQueue() {
        synchronized (uiUpdateLock) {
            if (uiUpdateBatchTask != null) {
                mainHandler.removeCallbacks(uiUpdateBatchTask);
                uiUpdateBatchTask = null;
            }
            pendingUiUpdates.clear();
        }
    }

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
            logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0276"));
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
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0277", command), e);
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
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0278"), e);
            return null;
        }
    }

    @SuppressLint("MissingPermission")
    private void bluetoothOn() {
        if (mBTAdapter == null) {
            logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0279"));
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
                    logDebug(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0280", sendMessage));
                    sendMessage = Constants.TestItemCodes.CM0100;
                }
                logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0281", sendMessage));
                btConnectedThread.write(sendMessage);

            }
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0282"), e);
        }
    }

    public static String getCurrentDatetime(String dateformat) {
        SimpleDateFormat dateFormmater = new SimpleDateFormat(dateformat);
        return dateFormmater.format(new Date());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent Data) {
        super.onActivityResult(requestCode, resultCode, Data);
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK) {
                    mBluetoothStatus.setText(getStringResource("ui.label.enabled"));
                } else {
                    mBluetoothStatus.setText(getStringResource("ui.label.disabled"));
                }
                break;
        }
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
                logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0283"));
                return;
            }
            mBTArrayAdapter.clear();

            mPairedDevices = getBondedDevicesSafe();
            int pairedCount = (mPairedDevices == null) ? 0 : mPairedDevices.size();
            boolean adapterEnabled = isBluetoothAdapterEnabled();
            logInfo(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0284", adapterEnabled, pairedCount, entranceCheck));
            if (adapterEnabled) {
                btReconnectDelayMs = Constants.Timeouts.BT_LIST_TIMER_INTERVAL_MS;
                scheduleBluetoothReconnect(true);
            } else {
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
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0285"), e);
        }
    }

    private void showNoPairedDevicesDialog() {
        if (dialogManager == null || isFinishing()) {
            return;
        }

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


    private void onConnectionFailed() {
        logInfo(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0286"));

        stopBtConnectionIndicator();

        scheduleBluetoothReconnect(false);
    }

    private void cancelConnection() {
        if (btSocket != null && btSocket.isConnected()) {
            try {
                btSocket.close();
                logInfo(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0287"));
            } catch (IOException e) {
                logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0288"), e);
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
                logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0289", e.getMessage()));
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
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0290"), e);
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
            logDebug(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0291", intent.getAction()));
            ActivityModelTestProcess activity = getMainActivity();
            int usbStatusColor = -1; // -1이면 UI 업데이트 없음
            String usbStatusText = null;

            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED

                    usbConnPermissionGranted = true; // 권한 플래그를 먼저 올려 재시작 로직이 차단되지 않도록 함
                    cancelUsbReconnect();
                    updateUsbLampReady();

                    if (activity != null && !activity.isFinishing()) {
                        Intent reconnectIntent = new Intent(context, ActivityModelTestProcess.class);
                        reconnectIntent.setAction("USB_RECONNECTED");
                        reconnectIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        reconnectIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);


                        activity.onNewIntent(reconnectIntent);  // ✅ 생명주기 변경 없음
                        activity.setIntent(reconnectIntent);
                    }
                    if (usbService != null && (usbPollingFuture == null || usbPollingFuture.isCancelled())) {
                        logInfo(LogManager.LogCategory.US, getStringResource("log.activity_model_test_process.0292"));
                        startUsbPolling(true);
                    }
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    usbStatusColor = R.color.colorAccent;
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    usbStatusColor = R.color.green_01;
                    scheduleUiUpdate(() -> {
                        final int finalUsbStatusColor = R.color.red_01;
                        tvConnectPlcRamp.setBackgroundColor(getColor(finalUsbStatusColor));
                    });
                    logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0293"));
                    stopUsbPolling();

                    decElectricValue = 0;

                    usbConnPermissionGranted = false;  // 권한 플래그만 초기화
                    scheduleUsbPermissionRecovery();
                    scheduleUsbReconnect(false);

                    stopBtMessageTimer();
                    disconnectCheckCount = 0;
                    receivedMessageCnt = 0;
                    sendingMessageCnt = 0;

                    usbStatusColor = R.color.red_01;
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    stopBtMessageTimer();
                    scheduleUiUpdate(() -> {
                        final int finalUsbStatusColor = R.color.red_01;
                        tvConnectPlcRamp.setBackgroundColor(getColor(finalUsbStatusColor));
                    });
                    logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0294"));
                    stopUsbPolling();

                    decElectricValue = 0;

                    usbConnPermissionGranted = false;  // 권한 플래그만 초기화
                    scheduleUsbPermissionRecovery();
                    scheduleUsbReconnect(false);

                    stopBtMessageTimer();
                    disconnectCheckCount = 0;
                    receivedMessageCnt = 0;
                    sendingMessageCnt = 0;

                    usbStatusColor = R.color.red_01;
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    usbStatusColor = R.color.orange_01;
                    break;
                default:
                    logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0295", intent.getAction()));
                    break;
            }

            if (usbStatusColor != -1 && activity != null && !activity.isFinishing()) {
                final int finalUsbStatusColor = usbStatusColor;
                scheduleUiUpdate(() -> {
                    tvConnectPlcRamp.setBackgroundColor(getColor(finalUsbStatusColor));
                });
            }
        }
    };

    private ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            try {
                if (arg1 instanceof UsbService.UsbBinder) {
                    UsbService.UsbBinder binder = (UsbService.UsbBinder) arg1;
                    usbService = binder.getService();

                    if (usbCommandQueue != null) {
                        usbCommandQueue.setUsbService(usbService);
                    } else {
                        usbCommandQueue = new UsbCommandQueue();
                        usbCommandQueue.setUsbService(usbService);
                        usbCommandQueue.start();
                        logInfo(LogManager.LogCategory.US, getStringResource("log.activity_model_test_process.0296"));
                    }

                    if (usbService != null) {
                        logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0297", usbHandler));
                        if (usbHandler == null) {
                            usbHandler = new ActivityModelTestProcess.UsbHandler(ActivityModelTestProcess.this);
                            logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0298"));
                        }
                        if (usbHandler != null) {
                            usbService.setHandler(usbHandler);
                            logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0299"));
                        } else {
                            logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0300"));
                            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0301"), null);
                        }
                        logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0302"));

                        if (usbPollingFuture == null || usbPollingFuture.isCancelled()) {
                            logInfo(LogManager.LogCategory.US, getStringResource("log.activity_model_test_process.0303"));

                            if (usbConnPermissionGranted) {
                                logInfo(LogManager.LogCategory.US, getStringResource("log.activity_model_test_process.0304"));

                                scheduleUiUpdate(() -> {
                                    tvConnectPlcRamp.setBackgroundColor(getBaseContext().getResources().getColor(R.color.blue_01));
                                });

                                logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0305"));
                                startUsbPolling(true);
                            } else {
                                logInfo(LogManager.LogCategory.US, getStringResource("log.activity_model_test_process.0306"));
                            }
                        } else {
                            logDebug(LogManager.LogCategory.US, getStringResource("log.activity_model_test_process.0307"));
                        }
                    } else {
                        logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0308"), null);
                        usbService = null;
                    }
                } else {
                    logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0309", (arg1 != null ? arg1.getClass().getName() : Constants.Common.NULL)), null);
                    usbService = null;
                }
            } catch (ClassCastException e) {
                logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0310"), e);
                e.printStackTrace();
                usbService = null;
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0310"), e);
                e.printStackTrace();
                usbService = null;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0311"));
            usbConnPermissionGranted = false;  // 권한 플래그만 초기화
        }
    };

    @Override
    public void onResume() {
        super.onResume();

        setupKioskMode();

        if (!btPermissionsGranted) {
            runOnBtWorker(() -> {
                boolean hasAllPermissions = hasAllBluetoothPermissions();
                if (hasAllPermissions) {
                    runOnUiThread(() -> {
                        permissionDenialCount = 0; // 권한이 허용되었으면 거부 횟수 리셋
                        btPermissionsGranted = true;
                        permissionRequestInProgress = false;
                        runPendingPermissionActions();
                        logInfo(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0312"));
                    });
                }
            });
        }

        if (isHandlingUsbReconnection) {
            return;
        }

        try {
            if (usbReceiverRegisted) {
                receivedMessageCnt = 0;
                sendingMessageCnt = 0;
                if (usbService == null) {
                    logDebug(LogManager.LogCategory.US, getStringResource("log.activity_model_test_process.0313"));
                    new Thread(() -> {
                        startService(UsbService.class, usbConnection, null);
                    }).start();
                } else {
                    logDebug(LogManager.LogCategory.US, getStringResource("log.activity_model_test_process.0314"));
                }
                return;
            }

            cancelResetTimer();

            new Thread(() -> {
                setFilters();  // Start listening notifications from UsbService
                startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
            }).start();
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0315"), e);
        }

        startManagers();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);  // ⚠️ 중요: 새로운 Intent를 현재 Intent로 설정

        if (intent != null && "USB_RECONNECTED".equals(intent.getAction())) {
            logInfo(LogManager.LogCategory.US, getStringResource("log.activity_model_test_process.0316"));

            isHandlingUsbReconnection = true;

            try {
                resetStateForUsbReconnect();
                handleUsbReconnection();
            } finally {
                isHandlingUsbReconnection = false;
            }
        }
    }

    private void handleUsbReconnection() {
        try {
            if (usbService != null && (usbPollingFuture == null || usbPollingFuture.isCancelled())) {
                scheduleUiUpdate(() -> {
                    tvConnectPlcRamp.setBackgroundColor(getBaseContext().getResources().getColor(R.color.blue_01));
                });

                logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0317"));
                startUsbPolling(true);
                logInfo(LogManager.LogCategory.US, getStringResource("log.activity_model_test_process.0318"));
            }
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0319"), e);
        }
    }

    private void resetStateForUsbReconnect() {
        try {
            resetBluetoothSessionKeepUsb();
            disconnectCheckCount = 0;
            receivedMessageCnt = 0;
            sendingMessageCnt = 0;
        } catch (Exception e) {
            logError(LogManager.LogCategory.US, getStringResource("log.activity_model_test_process.0320"), e);
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
                    logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0321", resetCnt, usbReconnectAttempts));
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


    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        boolean needsRestart = usbService == null || !UsbService.SERVICE_CONNECTED;
        if (!needsRestart) {
            logDebug(LogManager.LogCategory.US, getStringResource("log.activity_model_test_process.0322"));
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
            logDebug(LogManager.LogCategory.US, getStringResource("log.activity_model_test_process.0323", e.getMessage()));
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

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
                } else {
                    ContextCompat.registerReceiver(this, usbReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
                }
                usbReceiverRegisted = true;  // 등록 플래그 설정
            } catch (IllegalArgumentException e) {
                logDebug(LogManager.LogCategory.US, getStringResource("log.activity_model_test_process.0324", e.getMessage()));
                usbReceiverRegisted = true;
            }
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0325"), e);
        }
    }

    private class UsbHandler extends Handler {
        private final WeakReference<ActivityModelTestProcess> mActivity;

        public UsbHandler(ActivityModelTestProcess activity) {
            super(Looper.getMainLooper()); // ⚠️ 중요: 메인 스레드의 Looper를 명시적으로 지정 (없으면 메시지를 받을 수 없음)
            mActivity = new WeakReference<>(activity);
            logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0326", getLooper(), Thread.currentThread().getName()));
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UsbService.MESSAGE_FROM_SERIAL_PORT:
                    try {
                        String data = (String) msg.obj;
                        try {
                            dataBuffer = data;

                            scheduleUiUpdate(() -> {
                                usbConnPermissionGranted = true;
                                if(tvConnectPlcRamp!=null) {
                                    tvConnectPlcRamp.setBackgroundColor(getBaseContext().getResources().getColor(R.color.blue_01));
                                }
                            });

                            if (dataBuffer.contains(Constants.PLCCommands.RSS_RESPONSE_HEADER) && dataBuffer.length() >= 13) {
                                int s = dataBuffer.indexOf(Constants.PLCCommands.RSS_RESPONSE_HEADER) + Constants.PLCCommands.RSS_RESPONSE_HEADER.length();
                                int e = s + 4;
                                decElectricValue = Integer.parseInt(dataBuffer.substring(s, e), 16);

                                if (!usbReceiverRegisted) {
                                    usbReceiverRegisted = true;
                                }

                                lstWatt.add(decElectricValue);
                                recordWattMeasurement(currentTestItem, decElectricValue);

                                scheduleUiUpdate(() -> {
                                    if (wattValueAnimator != null) {
                                        wattValueAnimator.animateToValue(decElectricValue);
                                    } else if (tvWattValue != null) {
                                        tvWattValue.setText(String.format("%d", decElectricValue));
                                    }
                                });

                                if (usbCommandQueue != null) {
                                    usbCommandQueue.notifyResponseReceived();
                                }

                                handleUsbResponse(dataBuffer);

                                dataBuffer = "";
                                dataBufferTmp = "";
                            } else {
                                if (usbCommandQueue != null) {
                                    usbCommandQueue.notifyResponseReceived();
                                }

                                handleUsbResponse(dataBuffer);
                            }

                            dataBufferTmp = "";

                            if (dataBufferTmp.length() > 60) {
                                dataBufferTmp = "";
                            }
                        } catch (Exception e) {
                            dataBuffer = "";
                            dataBufferTmp = "";
                        }
                    } catch (Exception e) {
                        logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0327"), e);
                    }
                    break;
                case UsbService.CTS_CHANGE:
                    break;
                case UsbService.DSR_CHANGE:
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
                    logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0328"), e);
                } catch (BiffException e) {
                    logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0329"), e);
                }
            } catch (Exception e) {
                logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0330"), e);
            }
        }).start();
    }

    public void callBarcodeInfoServer() {
        btWorkerExecutor.execute(() -> {
            try {
                if (aTryingCnt == 5) {
                    aTryingCnt = 0;
                }
                HttpURLConnection connection = null;
                try {
                    urlStrBarcode = Constants.URLs.HTTP_PROTOCOL + serverIp + Constants.URLs.ENDPOINT_PRODUCT_SERIAL_INFO_LIST
                            + Constants.Common.QUESTION + Constants.URLs.PARAM_UNIT_NO + unit_no
                            + Constants.Common.AMPERSAND + Constants.URLs.PARAM_MODEL_ID + globalModelId;
                    logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0331", urlStrBarcode));
                    URL url = new URL(urlStrBarcode);
                    connection = (HttpURLConnection) url.openConnection();
                    if (connection != null) {
                        connection.setConnectTimeout(Constants.Timeouts.HTTP_CONNECT_TIMEOUT_MS);
                        connection.setReadTimeout(Constants.Timeouts.HTTP_READ_TIMEOUT_MS);
                        connection.setRequestMethod("GET");
                        connection.setDoInput(true);
                        connection.setDoOutput(true);

                        int resCode = connection.getResponseCode();
                        logInfo(LogManager.LogCategory.BI, getStringResource("log.activity_model_test_process.0332", urlStrBarcode, resCode, (resCode == HttpURLConnection.HTTP_OK)));
                        if (resCode == HttpURLConnection.HTTP_OK) {
                            logInfo(LogManager.LogCategory.BI, getStringResource("log.activity_model_test_process.0333", resCode));
                            barcodeReadCheck = true;
                            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                            try {
                                String line = null;
                                String lineTmp = null;
                                StringBuilder sb = new StringBuilder();

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

                                final String data = (line != null && !line.trim().equals("")) ? line :
                                        (sb.length() > 0 ? sb.toString() : null);

                                if (data != null && !data.trim().equals("")) {
                                    logDebug(LogManager.LogCategory.BI, getStringResource("log.activity_model_test_process.0334"));
                                    jsonParsingBarcode(Constants.JsonKeys.PRODUCT_SERIAL, data);
                                }
                            } finally {
                                try {
                                    reader.close();
                                } catch (IOException e) {
                                    logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0335"), e);
                                }
                            }
                        }

                        if (resCode != HttpURLConnection.HTTP_OK) {
                            logWarn(LogManager.LogCategory.BI, getStringResource("log.activity_model_test_process.0336", resCode));
                        }
                    }
                } finally {
                    safeDisconnectConnection(connection);
                }
            } catch (Exception e) { //예외 처리
                logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0337"), e);
            }
        });
    }


    public void jsonParsingBarcode(String data_type, String json) {
        new Thread(() -> {
            try {
                logInfo(LogManager.LogCategory.BI, getStringResource("log.activity_model_test_process.0338", json));
                JSONObject jsonObject = new JSONObject(json);
                JSONArray testItemArray = jsonObject.getJSONArray(Constants.JsonKeys.PRODUCT_SERIAL);

                List<Map<String, String>> lstData = new ArrayList<Map<String, String>>();
                Map<String, String> mapData = null;
                logDebug(LogManager.LogCategory.SI, getStringResource("log.activity_model_test_process.0339"));

                if (data_type.equals(Constants.JsonKeys.PRODUCT_SERIAL)) {
                    logDebug(LogManager.LogCategory.SI, getStringResource("log.activity_model_test_process.0340", testItemArray.length()));
                    for (int i = 0; i < testItemArray.length(); i++) {
                        JSONObject testItemObject = testItemArray.getJSONObject(i);
                        mapData = new HashMap<String, String>();
                        mapData.put(Constants.JsonKeys.CLM_PRODUCT_SERIAL_NO, testItemObject.getString(Constants.JsonKeys.CLM_PRODUCT_SERIAL_NO));
                        lstData.add(mapData);
                    }

                    logDebug(LogManager.LogCategory.SI, getStringResource("log.activity_model_test_process.0341", lstData.size()));
                    for (int i = 0; i < lstData.size(); i++) {
                        try {
                            productSerialNo = lstData.get(i).get(Constants.JsonKeys.CLM_PRODUCT_SERIAL_NO);
                            logInfo(LogManager.LogCategory.BI, getStringResource("log.activity_model_test_process.0342", i, productSerialNo));
                        } catch (Exception e) {
                            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0343"), e);
                        }
                    }

                    scheduleUiUpdate(() -> {
                        logInfo(LogManager.LogCategory.SI, getStringResource("log.activity_model_test_process.0344", globalProductSerialNo, productSerialNo));
                        tvSerialNo.setText(productSerialNo);
                        tv_dialog_barcode.setText(productSerialNo);
                        blnBarcodeReceived = true;
                        aTryingCnt = 0;

                        SharedPreferences test = getSharedPreferences(Constants.SharedPrefKeys.TEST_COOKIE_INFO, MODE_PRIVATE);
                        globalProductSerialNo = test.getString(Constants.SharedPrefKeys.TEST_PRODUCT_SERIAL_NO, "");
                        logInfo(LogManager.LogCategory.SI, getStringResource("log.activity_model_test_process.0344", globalProductSerialNo, productSerialNo));
                    });

                    test_info.putString(Constants.JsonKeys.CLM_PRODUCT_SERIAL_NO, productSerialNo);
                    test_info.commit();
                    getPreferences();
                }
            } catch (JSONException e) {
                logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0345"), e);
            }
        }).start();
    }

    private void cleanupAllResources() {
        try {
            cleanupAllTimers();

            clearHttpHandlerQueue();
            clearBtHandlerQueue();
            if (mainHandler != null) {
                if (resourceInfoRunnable != null) {
                    mainHandler.removeCallbacks(resourceInfoRunnable);
                    resourceInfoRunnable = null;
                }
                mainHandler.removeCallbacksAndMessages(null);
            }
            if (btHandler != null) {
                btHandler.removeCallbacksAndMessages(null);
            }
            if (usbRecoveryHandler != null) {
                usbRecoveryHandler.removeCallbacksAndMessages(null);
            }
            if (usbReconnectHandler != null) {
                usbReconnectHandler.removeCallbacksAndMessages(null);
            }
            if (schedulerHandler != null) {
                schedulerHandler.removeCallbacksAndMessages(null);
            }

            cleanupUsbResources();

            cleanupBluetoothResources();

            cleanupAsyncTasks();

            cleanupHttpConnections();

            clearLogBatchQueue();

            clearMainActivityReference();

            if (test_info != null) {
                test_info.apply(); // 또는 commit()
            }
            if (cookie_info != null) {
                cookie_info.apply(); // 또는 commit()
            }

            logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0346"));
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0347"), e);
        }
    }

    private void resetActivityState() {
        try {
            logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0348"));

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

            usbConnTryCnt = 0;
            usbConnPermissionGranted = false;
            usbPollingRequested = false;
            usbPollingEnabled = false;
            usbPollingFailureCount = 0;
            isUsbReconnecting = false;
            usbReconnectAttempts = 0;
            usbPollingFuture = null;

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

            btHandler = null;
            btConnectedThread = null;
            btSocket = null;
            deviceSelected = null;
            btDeviceName = "";
            btDeviceAddr = "";
            btInfoList = null;

            tmrBTMessageSend = null;
            ttBTMessageSend = null;
            tmrFinishedRestart = null;
            ttFinishedRestart = null;
            tmrUnfinishedRestart = null;
            ttUnfinishedRestart = null;
            appResetTimerTask = null;
            barcodeRequestTimer = null;
            testTaskThread = null;

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

            arrTestItems = null;
            arrTestItemsZig = null;
            mapTestTemperature = null;

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

            logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0349"));
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0350"), e);
        }
    }

    private void cleanupUsbResources() {
        try {
            logInfo(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0351"));
            stopUsbPolling();

            if (usbReceiverRegisted) {
                try {
                    unregisterReceiver(usbReceiver);
                } catch (IllegalArgumentException e) {
                    logDebug(LogManager.LogCategory.US, getStringResource("log.activity_model_test_process.0352"));
                }
                usbReceiverRegisted = false;
            }

            if (usbConnPermissionGranted) {
                try {
                    unbindService(usbConnection);
                } catch (IllegalArgumentException e) {
                    logDebug(LogManager.LogCategory.US, getStringResource("log.activity_model_test_process.0353"));
                }
                usbConnPermissionGranted = false;
            }

            if (usbService != null) {
                try {
                    Intent usbIntent = new Intent(this, UsbService.class);
                    stopService(usbIntent);
                } catch (Exception e) {
                    logWarn(LogManager.LogCategory.US, getStringResource("log.activity_model_test_process.0354", e.getMessage()));
                }
                usbService = null;
            }

            UsbService.SERVICE_CONNECTED = false;

            usbConnection = null;
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0355"), e);
        }
    }

    private void cleanupBluetoothResources() {
        try {
            if (dialogManager != null) {
                dialogManager.dismissAllDialogs();
            }

            if (btConnectedThread != null) {
                try {
                    btConnectedThread.cancel(); // ConnectedThreadOptimized에 cancel() 메소드가 있다고 가정
                } catch (Exception e) {
                    logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0356", e.getMessage()));
                }
                btConnectedThread = null;
            }

            clearBluetoothReconnect();
            btConnectionInProgress = false;
            btReconnectDelayMs = Constants.Timeouts.BT_LIST_TIMER_INTERVAL_MS;

            if (btSocket != null && btSocket.isConnected()) {
                try {
                    btSocket.close();
                } catch (IOException e) {
                    logWarn(LogManager.LogCategory.BT, getStringResource("log.activity_model_test_process.0357", e.getMessage()));
                }
            }
            btSocket = null;

            cancelDiscoverySafe();

            deviceSelected = null;
            btSearchOnOff = false;
            btConnected = false;
            isConnected = false;

        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0358"), e);
        }
    }

    private void cleanupAsyncTasks() {
        try {

            if (testTaskThread != null) {
                try {
                    testTaskThread.cancel(true);
                } catch (Exception e) {
                    logWarn(LogManager.LogCategory.PS, getStringResource("log.activity_model_test_process.0359", e.getMessage()));
                }
                testTaskThread = null;
            }
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0360"), e);
        }
    }

    private void cleanupHttpConnections() {
        try {
            safeDisconnectConnection(connection);
            connection = null;

        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0361"), e);
        }
    }



    private void setupKioskMode() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                getWindow().setDecorFitsSystemWindows(false);
                WindowInsetsController controller = getWindow().getInsetsController();
                if (controller != null) {
                    controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                    controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_DEFAULT);
                }
            } else {
                View decorView = getWindow().getDecorView();
                int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                decorView.setSystemUiVisibility(uiOptions);

                decorView.setOnSystemUiVisibilityChangeListener(
                        new View.OnSystemUiVisibilityChangeListener() {
                            @Override
                            public void onSystemUiVisibilityChange(int visibility) {
                                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                                    setupKioskMode();
                                }
                            }
                        }
                );
            }

            hideSystemUI();
        } catch (Exception e) {
            logError(LogManager.LogCategory.ER, getStringResource("log.activity_model_test_process.0362"), e);
        }
    }

    private long lastHideSystemUITime = 0;
    private static final long HIDE_SYSTEM_UI_DEBOUNCE_MS = 100;

    public void hideSystemUI() {
        try {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastHideSystemUITime < HIDE_SYSTEM_UI_DEBOUNCE_MS) {
                return; // 너무 자주 호출되는 경우 스킵
            }
            lastHideSystemUITime = currentTime;

            View decorView = getWindow().getDecorView();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsetsController controller = getWindow().getInsetsController();
                if (controller != null) {
                    controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                }
            } else {
                int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                decorView.setSystemUiVisibility(uiOptions);
            }
        } catch (Exception e) {
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

            if (isControlMode && isControlOn && itemUpdated) {
                synchronized (controlTestTimerLock) {
                    if (controlTestTimerRunning.get() && controlTestItemIdx >= 0 &&
                            bundle.updateItemCommand != null && bundle.updateItemCommand.equals(controlCurrentTestItem)) {
                        controlTestReceiveCommand = bundle.updateItemCommand;
                        controlTestReceiveResponse = bundle.finalReceiveCommandResponse;

                        String checkValue = bundle.updateItemCheckValue;
                        if (checkValue != null && !checkValue.isEmpty()) {
                            controlTestResultValue = checkValue;
                        } else if (bundle.resultInfo != null && !bundle.resultInfo.isEmpty()) {
                            controlTestResultValue = bundle.resultInfo;
                        } else {
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
}
