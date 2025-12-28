package itf.com.lms;

import static itf.com.lms.ActivityModelList.cookie_info;
import static itf.com.lms.ActivityModelList.cookie_preferences;
import static itf.com.lms.ActivityModelList.globalLastTestStartTimestamp;
import static itf.com.lms.ActivityModelList.globalModelId;
import static itf.com.lms.ActivityModelList.globalModelName;
import static itf.com.lms.ActivityModelList.globalModelNation;
import static itf.com.lms.ActivityModelList.globalProductSerialNo;
import static itf.com.lms.ActivityModelList.modelIntent;
import static itf.com.lms.util.WebServer.resultResponseValues;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
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
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import itf.com.lms.item.ItemAdapterTestItem;
import itf.com.lms.kiosk.BaseKioskActivity;
import itf.com.lms.util.ConnectedThread;
import itf.com.lms.util.Constants;
import itf.com.lms.util.TestData;
import itf.com.lms.util.UsbService;
import itf.com.lms.vo.VoTestItem;
import itf.com.lms.R;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;

public class ActivityModel_0003 extends BaseKioskActivity {

    private static final String TAG = ActivityModel_0003.class.getSimpleName();

    private static final UUID BT_MODULE_UUID = UUID.fromString(Constants.InitialValues.BT_MODULE_UUID_SERIAL_PORT_SERVICE);

    public final static int REQUEST_ENABLE_BT = 1;
    public final static int MESSAGE_READ = 2;
    public final static int CONNECTING_STATUS = 3;

    private ConstraintLayout clDialogForPreprocess = null;
    private TextView tvDialogMessage = null;
    private TextView tvUnitId = null;


    static Handler btHandlerTmp = null;

    private ConstraintLayout clTestResult;
    private ConstraintLayout cl_dialog_for_logger;
    private TextView tv_dialog_for_logger_watt;
    private TextView tv_dialog_for_logger_temp;
    private TextView tvPopupProcessResult;
    public TextView tvWattValue;
    private TextView tvCompWattValue;
    private TextView tvHeaterWattValue;
    private TextView tvPumpWattValue;
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
    private TextView tvModelNationality;
    public TextView tvSerialNo;
    private TextView tvTotalTimeCnt;
    private TextView tvEllapsedTimeCnt;
    // GUI Components
    private TextView tvCurrentProcess;
    private TextView mBluetoothStatus;
    private static TextView mReadBuffer;
    private static TextView mReadText;
    private Button btnTestResultClose;
    private ListView lvTestItem;
    private ConstraintLayout cl_log;
    private TextView tv_log;
    public String mode_type = "T";
    private String log_text = "";
    private String log_text_param = "";

    static boolean usbReceiverRegisted = false;

    private TextView btnTestRestart;

    static public BluetoothAdapter mBTAdapter;
    private Set<BluetoothDevice> mPairedDevices;
    private ArrayAdapter<String> mBTArrayAdapter;

    private Handler btHandler;
    private ConnectedThread btConnectedThread;
    private BluetoothSocket btSocket = null;

    private TextView tvPopupProcessResultCompValue;
    private TextView tvPopupProcessResultHeaterValue;

    private String readMessage = null;
//    private static String readMessagePlcTmp = null;
//    private String readMessageBTTmp = null;

    private static ItemAdapterTestItem listItemAdapter = null;
    private String sendResultYn = "N";



    private int totalTimeCnt = 0;
    private int testItemIdx = 0;
    private int testItemCounter = 0;
    private int compressureResultCnt = 0;
    private int testTotalCounter = 0;
    private String currentTestItem = "ST0101";
    private String datetimeFormat = "yyyy/MM/dd HH:mm:ss";
    private String timestampFormat = "yyyyMMddHHmmssSSS";

    private Timer tmrVoltageProcess = null;
    private TimerTask tmrVoltageTimerTask = null;


    static private String decTemperature = "";
    static private double decTemperatureValue = 0;
    private List<Map<String, String>> coldTemperatureData = null;
    private List<Map<String, String>> hotTemperatureData = null;
    private List<Map<String, String>> temperatureData = null;
    private boolean entranceCheck = false;
    private boolean barcodeReadCheck = false;

    private double decElectricValue = 0;
    private double decElectricValueTmp = 0;
    private String resultValue = "";
    static private String resultInfo = "";
    private double decElectricValueForComp = 0;
    private double decElectricValueForHeater = 0;
    private double decElectricValueForPump = 0;

    private String plcCommand = "";

    private static TextView tvTemperature = null;


    public static int tmrAppResetCnt = 0;
    public static int tmrAppResetCnt2 = 0;
    public static int tmrAppResetCnt1 = 0;
    private TimerTask appResetTimerTask = null;
    private Timer tmrBTList = null;
    private TimerTask btListTimerTask = null;
    private Timer tmrBTMessageSend = null;
    private TimerTask ttBTMessageSend = null;

    public List<Map<String, String>> lstTestResult = null;
    private static List<Map<String, String>> lstTestTemperature = null;
    private static Map<String, String> mapTestTemperature = null;

    public static TextView tvAlertMessage = null;
    private static TextView btnTestHistoryList = null;

    private ConstraintLayout clAlert = null;
    private Button btnAlertClose = null;


    static String[][] arrTestItems = null;
    static String[][] arrTestItemsZig = null;
    String productSerialNo = "";
    static String testProcessId = "";

    static public String serverIp = "";
    static public String serverDomain = "";
    static public String serverResetIp = "";


    static private String btDeviceName = "";
    static private String btDeviceAddr = "";
    static public ActivityModel_0003 mainActivity = null;

    static int usbConnTryCnt = 0;
    static boolean usbConnPermissionGranted = false;

    public static final String REGEXP_CHAR_NUM = "^[0-9a-zA-Zㄱ-ㅎ가-힣]*$";
    public List<Map<String, String>> lstData = null;
    static BluetoothDevice deviceSelected = null;
    private String dataBuffer = "";

    String currentTimestamp = "";

    String[][] arrTestModels = {{"WP000001", "CHP-4600N", "jp"}};

    // DBHelper dbHelper;


    int temperature12Bit = 0;
    String temperatureTmp = "0";

    String valueWatt = "0";
    String lowerValueWatt = "0";
    String upperValueWatt = "0";
    String valueWattLog = "";


    public boolean productSerialNoYn = false;
    public static List<Map<String, String>> btInfoList = null;
    private Map<String, String> btInfoMap = null;

    String urlStr = "";
    String urlTestTaskStr = "";
    ActivityModel_0003.RequestTestTaskThreadAsync testTaskThread = null;
    static String urlStrBarcode = "";
    static Handler httpHandler = new Handler();

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
    List<Map<String, String>> lstWattData = null;

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
    private ActivityModel_0003.UsbHandler usbHandler;
    private String folder_name = "";
    public List<Double> lstWatt = new ArrayList<>();

    public static HttpURLConnection connection = null;


    boolean finishedCorrectly = false;
    Timer tmrFinishedRestart = null;
    TimerTask ttFinishedRestart = null;

    int restartCntFinished = 0;
    int restartCntUnfinished = 0;
    int restartCntMargin = 0;

    public static final int MULTIPLE_PERMISSIONS = 1801;
    private String[] permissions = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION};

    int wattLower = 0;
    int wattUpper = 0;
    int pumpOkCnt = 0;
    static boolean bln_test_yn = false;
    static String test_logger = "";
    static String test_logger_temp = "";
    String receiveCommand = "";
    String decElectricValueList = "";
    String receiveCommandResponseOK = "";
    String receiveCommandResponse = "";
    String receiveCompAgingResponse = "01";  // 08 버전 생산라인
    // static String receiveCompAgingResponse = "";  // 09 버전 테스트
    String receiveResponseResult = "NG";
    String receiveResponseResultTxt = "";
    String tempTmp = "";
    String tempTmp2 = "0";

    Timer tmrUnfinishedRestart = null;
    TimerTask ttUnfinishedRestart = null;
    FloatingActionButton fab_close = null;
    int min_diff = 0;
    int reload_min_diff = 20;

    int sendingMessageCnt = 0;
    int receivedMessageCnt = 0;
    int disconnectCheckCount = 0;

    static int postTestInfoColor = 0;

    public static Timer checkDuration = null;

    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isConnected = false;
    private static final long CONNECTION_TIMEOUT = 2000; // 10 seconds timeout


    static boolean blnBarcodeReceived = false;
    static int aTryingCnt = 0;

    String iRemoteCommandCallCondition = "S";
    int iRemoteCommandCnt = 0;
    Timer tmrRemoteCommand = null;
    TimerTask ttRemoteCommand = null;

    private String decTemperatureTmp = "";
    private String decTemperatureColdValue = "";
    private String decTemperatureHotValue = "";
    private String decTemperatureValueCompStart = "";
    private boolean compAgingStarted = false;
    private String decTemperatureValueCompEnd = "";
    private String temperatureValueCompDiff = "";
    private String currentPBAVersion = "";


    static Timer barcodeRequestTimer = null;

    // DON'T FORGET to stop the server
    @Override
    public void onDestroy() {
        try {
            super.onDestroy();
            Log.i(TAG, "▶ [PS] onDestroy ");
            unregisterReceiver(usbReceiver);
            finishApplication(getApplicationContext());
            // 서버로 결과 전달
        } catch (Exception e) {
            Log.d(TAG, "▶ [ER].00047 : " + e.toString());
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
            Log.i(TAG, "▶ [ER].00033 " + e.toString());
            Log.d(TAG, e.toString());
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Log.i(TAG, "▶ [PS] onCreate entranceCheck:" + entranceCheck + " / currentTestItem:" + currentTestItem);
            setContentView(R.layout.activity_main);
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 1000);

            btnTestResultClose = (Button) findViewById(R.id.btn_test_result_close);

            tvConnectBtRamp = (TextView) findViewById(R.id.tv_connect_bt_ramp);
            tvConnectPlcRamp = (TextView) findViewById(R.id.tv_connect_plc_ramp);
            tvRunWsRamp = (TextView) findViewById(R.id.tv_run_ws_ramp);

            tvTestOkCnt = (TextView) findViewById(R.id.tv_test_ok_cnt);
            tvTestNgCnt = (TextView) findViewById(R.id.tv_test_ng_cnt);

            clAlert = (ConstraintLayout) findViewById(R.id.cl_alert);
            btnAlertClose = (Button) findViewById(R.id.btn_alert_close);
            clTestResult = (ConstraintLayout) findViewById(R.id.cl_test_result);

            clDialogForPreprocess = (ConstraintLayout) findViewById(R.id.cl_dialog_for_preprocess);
            tvDialogMessage = (TextView) findViewById(R.id.tv_dialog_message);

            tvUnitId = (TextView) findViewById(R.id.tv_unit_id);

            tvPopupProcessResult = (TextView) findViewById(R.id.tv_popup_process_result);
            tvPopupProcessResultCompValue = (TextView) findViewById(R.id.tv_popup_process_result_comp_value);
            tvPopupProcessResultHeaterValue = (TextView) findViewById(R.id.tv_popup_process_result_heater_value);
            tvCompWattValue = (TextView) findViewById(R.id.tv_comp_watt_value);
            tvHeaterWattValue = (TextView) findViewById(R.id.tv_heater_watt_value);
            tvPumpWattValue = (TextView) findViewById(R.id.tv_pump_watt_value);

            tvCompValueWatt = (TextView) findViewById(R.id.tv_comp_value_watt);
            tvHeaterValueWatt = (TextView) findViewById(R.id.tv_heater_value_watt);
            tvPumpValueWatt = (TextView) findViewById(R.id.tv_pump_value_watt);
            tvCompLowerValueWatt = (TextView) findViewById(R.id.tv_comp_lower_value_watt);
            tvCompUpperValueWatt = (TextView) findViewById(R.id.tv_comp_upper_value_watt);
            tvHeaterLowerValueWatt = (TextView) findViewById(R.id.tv_heater_lower_value_watt);
            tvHeaterUpperValueWatt = (TextView) findViewById(R.id.tv_heater_upper_value_watt);
            tvPumpLowerValueWatt = (TextView) findViewById(R.id.tv_pump_lower_value_watt);
            tvPumpUpperValueWatt = (TextView) findViewById(R.id.tv_pump_upper_value_watt);

            tvEllapsedTimeCnt = (TextView) findViewById(R.id.tv_ellapsed_time_cnt);
            tvModelName = (TextView) findViewById(R.id.tv_model_name);
            tvModelNationality = (TextView) findViewById(R.id.tv_model_nationality);
            tvSerialNo = (TextView) findViewById(R.id.tv_serial_no);
            tvModelId = (TextView) findViewById(R.id.tv_model_id);
            tv_dialog_barcode = (TextView) findViewById(R.id.tv_dialog_barcode);
            tvCurrentProcess = (TextView) findViewById(R.id.tv_current_process);
            tvTotalTimeCnt = (TextView) findViewById(R.id.tv_total_time_cnt);
            mBluetoothStatus = (TextView) findViewById(R.id.bluetooth_status);
            mReadBuffer = (TextView) findViewById(R.id.read_buffer);
            mReadText = (TextView) findViewById(R.id.receive_text);
            lvTestItem = (ListView) findViewById(R.id.lst_test_item);
            tvTemperature = (TextView) findViewById(R.id.tv_temperature);

            btnTestRestart = (TextView) findViewById(R.id.btn_test_restart);

            tvAlertMessage = (TextView) findViewById(R.id.tv_alert_message);
            btnTestHistoryList = (TextView) findViewById(R.id.btn_test_history_list);

            tvWattValue = (TextView) findViewById(R.id.tv_watt_value);

            trPreprocessContent = (TableRow) findViewById(R.id.tr_preprocess_content);

            tvPopupProcessResultCompValue = (TextView) findViewById(R.id.tv_popup_process_result_comp_value);
            tvPopupProcessResultHeaterValue = (TextView) findViewById(R.id.tv_popup_process_result_heater_value);

            tv_current_version = (TextView) findViewById(R.id.tv_current_version);

            cl_dialog_for_logger = (ConstraintLayout) findViewById(R.id.cl_dialog_for_logger);    // 20240522 바코드 인식 표시
            tv_dialog_for_logger_watt = (TextView) findViewById(R.id.tv_dialog_for_logger_watt);    // 20240522 바코드 인식 표시
            tv_dialog_for_logger_temp = (TextView) findViewById(R.id.tv_dialog_for_logger_temp);    // 20240522 바코드 인식 표시

            fab_close = findViewById(R.id.fab_close);

            fab_close.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finishApplication(getApplicationContext());
                    // 서버로 결과 전달
                }
            });

            mBTArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
            mBTAdapter = BluetoothAdapter.getDefaultAdapter(); // get a handle on the bluetooth radio

            restartCntMargin = 60 * 10;
            // restartCntMargin = 60;

            folder_name = "/data/data/itf.com.app.simple_line_test_ovio/www";

            mReadBuffer.setMovementMethod(ScrollingMovementMethod.getInstance());

            mainActivity = this;

            cl_log = (ConstraintLayout) findViewById(R.id.cl_log);
            tv_log = (TextView) findViewById(R.id.tv_log);
            finishedCorrectly = false;
            testProcessId = "";

            tv_log.setMovementMethod(ScrollingMovementMethod.getInstance());

            mainActivity.runOnUiThread(new Runnable() {
                public void run() {
                    if (mode_type.equals("N")) {
                        cl_log.setVisibility(View.GONE);
                        tv_log.setText("");
                    } else if (mode_type.equals("T")) {
                        // cl_log.setVisibility(View.VISIBLE);
                        cl_log.setVisibility(View.GONE);
                        tv_log.setText("");
                    }
                }
            });

            usbHandler = new ActivityModel_0003.UsbHandler(this);

            modelIntent = getIntent();
            globalModelId = modelIntent.getStringExtra("test_model_id");
            globalModelName = modelIntent.getStringExtra("test_model_name");
            globalModelNation = modelIntent.getStringExtra("test_model_nation");
            globalLastTestStartTimestamp = (modelIntent.getStringExtra("test_start_datetime") == null) ? lmsTestSeq : modelIntent.getStringExtra("test_start_datetime");
            globalLastTestStartTimestamp = modelIntent.getStringExtra("test_start_datetime");

            SharedPreferences test = getSharedPreferences("test_cookie_info", MODE_PRIVATE);
            globalLastTestStartTimestamp = test.getString("test_start_datetime", "");

            globalLastTestStartTimestamp = (globalLastTestStartTimestamp.equals("")) ? lmsTestSeq : globalLastTestStartTimestamp;

            Log.i(TAG, "▶ [SI] globalLastTestStartTimestamp:" + globalLastTestStartTimestamp + " lmsTestSeq:" + lmsTestSeq);

            String reqDateStr = (globalLastTestStartTimestamp == null) ? lmsTestSeq : globalLastTestStartTimestamp;
//            Date curDate = new Date();
//            SimpleDateFormat dateFormat = new SimpleDateFormat(timestampFormat);
//            Date reqDate = dateFormat.parse(reqDateStr);
//            long reqDateTime = reqDate.getTime();
//            curDate = dateFormat.parse(dateFormat.format(curDate));
//            long curDateTime = curDate.getTime();
//            long minute = (curDateTime - reqDateTime) / 60000;
//            Log.i(TAG, "▶ [PS] 요청시간 : " + reqDate);
//            Log.i(TAG, "▶ [PS] 현재시간 : " + curDate);
//            Log.i(TAG, "▶ [PS] " + minute+"분 차이");
            min_diff = minDiff(reqDateStr);

            if (min_diff < reload_min_diff) {
//                // 테스트 수행 안함
//                clAlert.setVisibility(View.VISIBLE);
//                tvAlertMessage.setText("서버 연결 실패");
//                return;
            }

            btInfoList = new ArrayList<Map<String, String>>();

            if (bln_test_yn) {
                mainActivity.runOnUiThread(new Runnable() {
                    public void run() {
                        cl_dialog_for_logger.setVisibility(View.VISIBLE);
                        tv_dialog_for_logger_watt.setVisibility(View.VISIBLE);
                        tv_dialog_for_logger_temp.setVisibility(View.VISIBLE);
                    }
                });
                test_logger = "";
                test_logger_temp = "";
            }

//            if (arrTestModels[0][2].equals("jp")) {
//                modelNationality = "일본향";
//            }
            runOnUiThread(new Runnable() {
                public void run() {
                    tvModelNationality.setText(globalModelNation);
                    tvModelName.setText(globalModelName);
                    tvModelId.setText(globalModelId);

                    clAlert.setVisibility(View.GONE);
                    clDialogForPreprocess.setVisibility(View.GONE);
                }
            });

            // dbHelper = new DBHelper(ActivityModel_0003.this, "itf_temperature_table.db", 2);

            postTestInfoColor = getBaseContext().getResources().getColor(R.color.gray_04);

            //Editor를 preferences에 쓰겠다고 연결
            preferences = getSharedPreferences("TestInfo", MODE_PRIVATE);
            test_info = preferences.edit();

            setDisplayLightValueChange(0.5f);


            new Thread(() -> {
                try {
                    Log.i(TAG, "▶ [SI] read setting info ");
                    hotTemperatureData = TestData.selectTemperatureData(this, "1");
                    coldTemperatureData = TestData.selectTemperatureData(this, "2");

                    if (hotTemperatureData.size() > 0 && coldTemperatureData.size() > 0) {
                    } else {
                        // TestData.deleteTemperatureData(this, "1");
                        // TestData.deleteTemperatureData(this, "2");
                        // Log.i(TAG, "▶ [SI] delete temp info ");
                        readTemperatureExcel("1", "itf_temperature_info.xls");
                        readTemperatureExcel("2", "itf_temperature_info.xls");
                        Log.i(TAG, "▶ [SI] insert temp info ");

                        hotTemperatureData = TestData.selectTemperatureData(this, "1");
                        coldTemperatureData = TestData.selectTemperatureData(this, "2");
                    }

                    // RequestThread thread = new RequestThread(); // Thread 생성
                    // thread.start(); // Thread 시작
                } catch (Exception e) {
                    Log.d(TAG, "▶ [ER].00000 " + e);
                    return;
                }
            }).start();

            Log.i(TAG, "▶ [PS] globalModelId " + globalModelId);
            Log.i(TAG, "▶ [PS] globalModelName " + globalModelName);
            Log.i(TAG, "▶ [PS] globalModelNation " + globalModelNation);

            serverDomain = "192.168.0.";
            serverIp = "192.168.0.47";
            // serverDomain = "172.18.88.";
            // serverIp = "172.18.88.31";
            serverDomain = "172.16.1.";
            serverIp = "172.16.1.250:8080";
            // serverIp = "172.16.1.249:8080";
            // serverIp = "itfactoryddns.iptime.org:10004";

            urlStr = "http://" + serverIp + "/OVIO/TestInfoList.jsp";
            urlStr = "http://" + serverIp + "/OVIO/TestInfoList.jsp" + "?clm_call_type=S&clm_model_id=" + globalModelId;
            Log.i(TAG, "▶ [SI] setting server url " + urlStr);

            try {
                // urlStr = "http://" + serverIp + "/OVIO/TestInfoList.jsp" + "?clm_call_type=S";
                // urlStr = "http://" + serverIp + "/OVIO/TestInfoList.jsp" + "?clm_call_type=S&clm_model_id=" + globalModelId;
                // Log.i(TAG, "▶ [SI] setting server url.process_info " + urlStr);
                // String urlParamStr += (urlStr + "clm_call_type=S");
                ActivityModel_0003.RequestThreadAsync thread = new ActivityModel_0003.RequestThreadAsync(); // Thread 생성
                thread.execute(); // Thread 시작
            } catch (Exception e) {
                Log.d(TAG, "▶ [ER].00001 " + e);
                clAlert.setVisibility(View.VISIBLE);
                tvAlertMessage.setText("서버 연결 실패");
                return;
            }

            // urlStr = "http://" + serverIp + "/OVIO/TestInfoList.jsp";
            // urlStr = "http://" + serverIp + "/OVIO/TestInfoList.jsp" + "?clm_call_type=S&clm_model_id=" + globalModelId;
            // Log.i(TAG, "▶ [SI] setting server url " + urlStr);

            try {
            } catch (Exception e) {
                Log.d(TAG, "▶ [ER].00001 " + e);
                clAlert.setVisibility(View.VISIBLE);
                tvAlertMessage.setText("서버 연결 실패");
                return;
            }

            Path directoryPath = Paths.get(folder_name);
            try {            // 디렉토리 생성
                Files.createDirectory(directoryPath);
                System.out.println(directoryPath + " 디렉토리가 생성되었습니다.");
            } catch (FileAlreadyExistsException e) {
                Log.d(TAG, "▶ [ER].00034 " + e.toString());
            } catch (NoSuchFileException e) {
                Log.d(TAG, "▶ [ER].00035 " + e.toString());
            } catch (IOException e) {
                Log.d(TAG, "▶ [ER].00036 " + e.toString());
            }

            ipAddress = getLocalIpAddress();
//            int unit_no_value = Integer.parseInt(String.valueOf(ipAddress).split("\\.")[3]);
//            unit_no = (unit_no_value>100)?unit_no_value - 100:unit_no_value;
            unit_no = Integer.parseInt(String.valueOf(ipAddress).split("\\.")[3]) - 100;
            urlStrBarcode = "http://" + serverIp + "/OVIO/ProductSerialInfoList.jsp?clm_unit_no=" + unit_no + "&clm_model_id=" + globalModelId;;
            // urlStrBarcode = "http://" + serverIp + "/OVIO/ProductSerialInfoList.jsp?clm_unit_no=" + unit_no;

            if (ipAddress != null) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        tvRunWsRamp.setText(String.valueOf(unit_no));
                        tvRunWsRamp.setBackgroundColor(getBaseContext().getResources().getColor(R.color.blue_01));
                    }
                });
            } else {
                runOnUiThread(new Runnable() {
                    public void run() {
                        tvRunWsRamp.setText("");
                        tvRunWsRamp.setBackgroundColor(getBaseContext().getResources().getColor(R.color.red_01));
                    }
                });
            }
            Log.i(TAG, "▶ [SI] get web IP info " + ipAddress);

            // tvUnitId.setText((String.format("%-2s", String.valueOf(4)).replace(" ", "0")) + "-" + (String.format("%-2s", String.valueOf(unit_no)).replace(" ", "0")));
            runOnUiThread(new Runnable() {
                public void run() {
                    tvUnitId.setText("1-" + unit_no);

                    SharedPreferences test = getSharedPreferences("TestInfo", MODE_PRIVATE);
                    String test_result = test.getString("test_result", "");
                    String heater_value = test.getString("heater_value", "");
                    String comp_value = test.getString("comp_value", "");

                    if (test_result.equals("")) {
                        clTestResult.setVisibility(View.GONE);
                    } else {
                        clTestResult.setVisibility(View.VISIBLE);
                        tvPopupProcessResult.setText(test_result);
                        tvPopupProcessResultCompValue.setText(comp_value);
                        tvPopupProcessResultHeaterValue.setText(heater_value);
                    }
                }
            });

            // server = new WebServer();
//            try {
//                server.start();
//            } catch (IOException ioe) {
//                Log.w("Httpd", "The server could not start.");
//            }
            Log.i(TAG, "▶ [SI] webserver started ");

            bluetoothOn();
            Toast.makeText(getApplicationContext(), "Turned off", Toast.LENGTH_SHORT).show();

            // TestData.selectWattData();
            lstWattData = TestData.selectWattData(getBaseContext(), "");

            // Log.i(TAG, "▶ [SI] lstWattData.size() " + lstWattData.size());

            listItemAdapter = new ItemAdapterTestItem();

            lstTestResult = new ArrayList<Map<String, String>>();
            lstTestTemperature = new ArrayList<Map<String, String>>();


            List<Map<String, String>> lstTestItem = new ArrayList<Map<String, String>>();
            // lvTestHistory.setAdapter(listHistoryAdapter);

            btnTestResultClose.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void onClick(View view) {
                    clTestResult.setVisibility(View.GONE);
                }
            });

            btnTestRestart.setOnClickListener(new TextView.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // restartApplication(getApplicationContext());
                    finishApplication(getApplicationContext());
                    // 서버로 결과 전달
                }
            });

            btnTestRestart.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN: {
                            // restartApplication(getApplicationContext());
                            finishApplication(getApplicationContext());
                            // 서버로 결과 전달
                            return true;
                        }
                        case MotionEvent.ACTION_MOVE: {
                            return true;
                        }
                        case MotionEvent.ACTION_UP: {
                            return false;
                        }
                        default:
                            return false;
                    }
                }
            });

            btnTestHistoryList.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void onClick(View view) {
                }
            });

            try {
                lstSpecData = TestData.selectTestSpecData(getBaseContext(), "and 1=1");

                for (int i = 0; i < lstSpecData.size(); i++) {
                    Log.i(TAG, "▶ [PS] " + i + " " + lstSpecData.get(i));
                }
            } catch (Exception e) {
                Log.i(TAG, "▶ [ER].00003 " + lstSpecData.get(0));
                sendBtMessage(currentTestItem);
                return;
            }

            btHandler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    if (msg.what == MESSAGE_READ) {
                        try {
                            readMessage = new String((byte[]) msg.obj, StandardCharsets.UTF_8);
                            Log.i(TAG, "▶------------------------------------------------------------------ readMessage.indexOf(\"\\u0002\") " + readMessage.indexOf("\u0002"));
                            if(readMessage.equals("\u0002")) {
                                Log.i(TAG, "▶ [BT] stx only[" + readMessage + "] return");
                                return;
                            }
                            else {
                                if(readMessage.indexOf("\u0002")==0) {
                                    // Log.i(TAG, "▶ [BT] stx, cmd[" + readMessage + "]");
                                }
                                else {
                                    // Log.i(TAG, "▶ [BT] cmd only[" + readMessage + "]");
                                    // readMessage = "\u0002" + readMessage;
                                    readMessage = "\u0002" + readMessage;
                                }
                            }

                            Log.i(TAG, "▶ [BT] readMess[" + readMessage + "]");
                            // Log.i(TAG, "▶ [BT] entranceCheck >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> " + entranceCheck + " " + currentTestItem + " " + readMessage);

                            if (testItemIdx > arrTestItems.length - 1) {
                                testItemIdx = arrTestItems.length - 1;
                            }

                            if (readMessage.indexOf("ST0101") > -1) {
                                testItemIdx = 0;
                                ((VoTestItem) listItemAdapter.getItem(testItemIdx)).setTest_item_result("OK");
                                ((VoTestItem) listItemAdapter.getItem(testItemIdx)).setTest_result_value("01 / 01");
                                ((VoTestItem) listItemAdapter.getItem(testItemIdx)).setTest_finish_yn("Y");
                                if (testProcessId.equals("")) {
                                    testProcessId = getCurrentDatetime(timestampFormat);
                                    Log.i(TAG, "▶ [PS] testProcessId started " + testProcessId);
//                                    new Thread(() -> {
//                                        callBarcodeInfoServer();
//                                    }).start();
                                }
                                entranceCheck = true;
                                if (tmrUnfinishedRestart == null) {
                                    tmrUnfinishedRestart = new Timer();
                                    ttUnfinishedRestart = new TimerTask() {
                                        @Override
                                        public void run() {
                                            if (restartCntUnfinished == totalTimeCnt + restartCntMargin) {
                                                Log.i(TAG, "▶ [BT] tmrUnfinishedRestart 검사 어플리케이션 종료 실패 재시작 [" + restartCntUnfinished + " / " + (totalTimeCnt + restartCntMargin) + "]");
                                                restartCntUnfinished = 0;
                                                // restartApplication(getApplicationContext());
                                                finishApplication(getApplicationContext());
                                                // 서버로 결과 전달
                                                tmrUnfinishedRestart.cancel();
                                            }
                                            restartCntUnfinished++;
                                        }
                                    };
                                    tmrUnfinishedRestart.schedule(ttUnfinishedRestart, 0, 1000);
                                }
                                // callBarcodeInfoServer();
                                testItemIdx = 1;

                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        clTestResult.setVisibility(View.GONE);
                                        setDisplayLightValueChange(0.5f);
                                    }
                                });
                                // tmrBTMessageSend.cancel();
                            } else if (readMessage.indexOf("RE0101") > -1) {
                                if (tmrVoltageProcess != null) {
                                    // tmrVoltageProcess.cancel();
                                }

                                if (tmrBTMessageSend != null) {
                                    tmrBTMessageSend.cancel();
                                }
                            }

                            if (entranceCheck) {
                                Log.i(TAG, "▶ [BT][" + readMessage + "]");
                                receiveCommand = readMessage.substring(readMessage.indexOf(("\u0002")) + 1, 7);
                                receiveCommandResponse = readMessage.substring(13, 15);

                                // Log.i(TAG, "▶▶▶▶▶▶▶▶▶▶ [" + testItemCounter + " / " + arrTestItems[testItemIdx][2] + "][" + currentTestItem + " / " + receiveCommand + "]");

                                try {
                                    if (receiveCommand.contains("CM0102") || receiveCommand.indexOf("CM0103") > -1) {
//                                        receiveCommand = "CM0101";
                                    }

                                    if(receiveCommand.equals(currentTestItem)) {}
                                    else {
                                        if(receiveCommand.equals("CM0102") || receiveCommand.equals("CM0103")) {}
                                        else {
                                            receiveCommand = currentTestItem;
                                        }
                                    }

                                    lstSpecData = TestData.selectTestSpecData(getBaseContext(), "and clm_test_command='" + receiveCommand + "'");
                                } catch (Exception e) {
                                    Log.i(TAG, "▶ [ER].00003 " + lstSpecData.get(0));
                                    sendBtMessage(currentTestItem);
                                    return;
                                }

//                                if (receiveCommandResponse.equals(lstSpecData.get(0).get("clm_test_response_value"))) {
//                                    receiveCommandResponseOK = receiveCommand;
//                                    receiveResponseResult = "OK";
//                                } else {
//                                    receiveResponseResult = "NG";
//                                }

//                                Log.i(TAG, "▶ [BT] 검사 응답 신호 수신 [T:" + testTotalCounter + " / " + totalTimeCnt + "][C:" + testItemCounter + " / " + arrTestItems[testItemIdx][2] + "][C:" + testItemIdx + " / " + arrTestItems.length + "][S:" + currentTestItem + "][G:" + disconnectCheckCount + "=" + receivedMessageCnt + "-" + sendingMessageCnt + "][R:" + receiveCommand + "] " + receiveCommandResponse + " / " + lstSpecData.get(0).get("clm_test_response_value"));

                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        receivedMessageCnt++;
                                        tvCurrentProcess.setText(((VoTestItem) listItemAdapter.getItem(testItemIdx)).getTest_item_name());

                                        if (receiveCommand.indexOf("SN0101") > -1 || receiveCommand.indexOf("SN0201") > -1 || receiveCommand.indexOf("SN0301") > -1 || receiveCommand.indexOf("TA0101") > -1 || receiveCommand.indexOf("TA0201") > -1 || receiveCommand.indexOf("TA0301") > -1 || receiveCommand.indexOf("FM0101") > -1) {
                                            // Log.i(TAG, "▶ [BT] 센서 검사 수신 응답 [" + testItemCounter + " / " + arrTestItems[testItemIdx][2] + "][" + testItemIdx + " / " + arrTestItems.length + "] " + currentTestItem);
                                            if (clDialogForPreprocess.getVisibility() != View.VISIBLE) {
                                                clDialogForPreprocess.setVisibility(View.VISIBLE);
                                                tvDialogMessage.setText("");
                                            }

                                            if (receiveCommand.indexOf("SN0101") > -1) {
                                                preprocessColor = getBaseContext().getResources().getColor(R.color.pink_01);
                                                tvDialogMessage.setText(lstSpecData.get(0).get("clm_test_name"));
                                                trPreprocessContent.setBackgroundColor(preprocessColor);
                                            } else if (receiveCommand.indexOf("SN0201") > -1) {
                                                preprocessColor = getBaseContext().getResources().getColor(R.color.green_02);
                                                tvDialogMessage.setText(lstSpecData.get(0).get("clm_test_name"));
                                                trPreprocessContent.setBackgroundColor(preprocessColor);
                                            } else if (receiveCommand.indexOf("SN0301") > -1) {
                                                preprocessColor = getBaseContext().getResources().getColor(R.color.green_02);
                                                tvDialogMessage.setText(lstSpecData.get(0).get("clm_test_name"));
                                                trPreprocessContent.setBackgroundColor(preprocessColor);
                                            } else if (receiveCommand.indexOf("TA0101") > -1) {
                                                preprocessColor = getBaseContext().getResources().getColor(R.color.blue_01);
                                                tvDialogMessage.setText(lstSpecData.get(0).get("clm_test_name"));
                                                trPreprocessContent.setBackgroundColor(preprocessColor);
                                            } else if (receiveCommand.indexOf("TA0201") > -1) {
                                                preprocessColor = getBaseContext().getResources().getColor(R.color.yellow_01);
                                                tvDialogMessage.setText(lstSpecData.get(0).get("clm_test_name"));
                                                trPreprocessContent.setBackgroundColor(preprocessColor);
                                            } else if (receiveCommand.indexOf("TA0301") > -1) {
                                                preprocessColor = getBaseContext().getResources().getColor(R.color.yellow_01);
                                                tvDialogMessage.setText(lstSpecData.get(0).get("clm_test_name"));
                                                trPreprocessContent.setBackgroundColor(preprocessColor);
                                            } else if (receiveCommand.indexOf("FM0101") > -1) {
                                                preprocessColor = getBaseContext().getResources().getColor(R.color.pink_01);
                                                tvDialogMessage.setText(lstSpecData.get(0).get("clm_test_name"));
                                                trPreprocessContent.setBackgroundColor(preprocessColor);
                                            } else {
                                                clDialogForPreprocess.setVisibility(View.INVISIBLE);
                                                tvDialogMessage.setText("");
                                            }

                                            if (testItemCounter > 5) {

                                                /*
                                                // 09 : 버전 적용
                                                if (receiveCommand.indexOf("SN0201") > -1 || receiveCommand.indexOf("TA0201") > -1) {
                                                    if(receiveCommandResponse.equals("01")) {
                                                        receiveCommandResponse = "00";
                                                    }
                                                    else {
                                                        receiveCommandResponse = "01";
                                                    }
                                                }
                                                *
                                                 */

                                                if (receiveCommandResponse.equals(lstSpecData.get(0).get("clm_test_response_value"))) {
                                                    receiveCommandResponseOK = receiveCommand;
                                                    receiveResponseResult = "OK";
                                                } else {
                                                    receiveResponseResult = "NG";
                                                }

                                                if (receiveResponseResult.equals("OK")) {
                                                    testItemCounter = Integer.parseInt(arrTestItems[testItemIdx][2]);
                                                }
                                                resultValue = receiveCommandResponse;
                                            }

                                            Log.i(TAG, "▶ [BT] 검사 응답 신호 수신 [T:" + testTotalCounter + " / " + totalTimeCnt + "][C:" + testItemCounter + " / " + arrTestItems[testItemIdx][2] + "][P:" + testItemIdx + " / " + arrTestItems.length + "][S:" + currentTestItem + "][G:" + disconnectCheckCount + "=" + receivedMessageCnt + "-" + sendingMessageCnt + "][R:" + receiveCommand + "] " + receiveCommandResponse + " / " + lstSpecData.get(0).get("clm_test_response_value") + " / " + receiveResponseResult);
                                        } else {
                                            clDialogForPreprocess.setVisibility(View.INVISIBLE);
                                            tvDialogMessage.setText("");

                                            // Log.d(TAG, "▶ >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> receiveCommand " + receiveCommand + " " + receiveCommand.indexOf("TH0101"));
                                            // 온도
                                            if (receiveCommand.indexOf("TH0101") > -1 || receiveCommand.indexOf("TH0201") > -1 || receiveCommand.indexOf("TH0301") > -1) {
                                                try {
                                                    // 20250826 추가
//                                                    new Thread(() -> {
                                                    try {
                                                        // Log.d(TAG, "▶ >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> X " + dblValTemp);
                                                        // long longHex = parseUnsignedHex(testResultValue);
                                                        receiveCommandResponse = readMessage.substring(9, 15);
                                                        decTemperatureValue = Integer.parseInt(receiveCommandResponse, 16);
                                                        // Log.d(TAG, "▶ >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> A " + dblValTemp);

                                                        // Log.i(TAG, "▶ [TH] coldTemperatureData.size() " + coldTemperatureData.size());
                                                        // Log.i(TAG, "▶ [TH] hotTemperatureData.size() " + hotTemperatureData.size());
                                                        temperatureData = (receiveCommand.equals("TH0201")) ? hotTemperatureData : coldTemperatureData;
                                                        // if(receiveCommand.equals("TH0301")) { temperatureData = hotTemperatureData; };
                                                        // if(receiveCommand.equals("TH0101")) { temperatureData = coldTemperatureData; };
                                                        // Log.d(TAG, "▶ >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> B " + dblValTemp);

                                                        for (int i = 0; i < temperatureData.size(); i++) {
                                                            Log.i(TAG, "▶ [TH] temperatureData.get(" +  i+ ").get(\"clm_12_bit\") " + temperatureData.get(i).get("clm_12_bit"));
                                                            temperature12Bit = (int) Math.round(Double.valueOf(temperatureData.get(i).get("clm_12_bit")));
                                                            if (temperature12Bit > decTemperatureValue) {
                                                                decTemperature = temperatureTmp;
                                                                tvTemperature.setText(String.valueOf(decTemperature));

                                                                mapTestTemperature = new HashMap<>();
                                                                mapTestTemperature.put("clm_line", "lineId");
                                                                mapTestTemperature.put("clm_unit_id", "unitId");
                                                                mapTestTemperature.put("clm_timestamp", currentTimestamp);
                                                                mapTestTemperature.put("clm_product_temperature", decTemperature);
                                                                lstTestTemperature.add(mapTestTemperature);
                                                                break;
                                                            }
                                                            temperatureTmp = temperatureData.get(i).get("clm_temperature");
                                                        }
                                                        // Log.d(TAG, "▶ >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> C " + temperatureTmp);
                                                        Log.i(TAG, "▶ [TH] 측정 온도 [R:" + receiveCommand + "]" + decTemperatureValue + " / " + temperatureTmp);

//                                                    if(receiveCommand.equals("TH0101")) { decTemperatureHotValue = temperatureTmp + "°C"; }
//                                                    else if(receiveCommand.equals("TH0201")) { decTemperatureColdValue = temperatureTmp + "°C"; }
//                                                    else if(receiveCommand.equals("TH0301")) { decTemperatureColdValue = temperatureTmp + "°C"; }

                                                        if(receiveCommand.equals("TH0201")) {
                                                            decTemperatureColdValue = temperatureTmp + "°C";
                                                            tempTmp = decTemperatureColdValue;
                                                        }
                                                        else {
                                                            assert temperatureTmp != null;
                                                            if(Double.parseDouble(temperatureTmp)>50) {
                                                                // temperatureTmp = tempTmp2;
                                                            }
                                                            decTemperatureHotValue = temperatureTmp + "°C";
                                                            tempTmp = decTemperatureHotValue;
                                                            tempTmp2 = tempTmp.replace("°C", "");
                                                        }

                                                        receiveCommandResponse = receiveCommandResponse + "," + temperatureTmp;
                                                        // Log.i(TAG, "▶ [TH] 측정 온도 [R:" + receiveCommand + "]" + decTemperatureValue + " / " + temperatureTmp);

                                                        dblValTemp = Double.parseDouble(temperatureTmp);
                                                        int temperatureLower = Integer.parseInt(lstSpecData.get(0).get("clm_value")) - Integer.parseInt(lstSpecData.get(0).get("clm_test_lower_value"));
                                                        int temperatureUpper = Integer.parseInt(lstSpecData.get(0).get("clm_value")) + Integer.parseInt(lstSpecData.get(0).get("clm_test_upper_value"));
                                                        Log.i(TAG, "▶ [BT] 검사 응답 신호 수신 [T:" + testTotalCounter + " / " + totalTimeCnt + "][C:" + testItemCounter + " / " + arrTestItems[testItemIdx][2] + "][C:" + testItemIdx + " / " + arrTestItems.length + "][S:" + currentTestItem + "][G:" + disconnectCheckCount + "=" + receivedMessageCnt + "-" + sendingMessageCnt + "][R:" + receiveCommand + "][T:" + decTemperatureValue + " / " + temperatureTmp + "] " + temperatureLower + " < " + dblValTemp + " < " + temperatureUpper);

                                                        receiveResponseResult = (temperatureLower < dblValTemp && dblValTemp < temperatureUpper) ? "OK" : "NG";
                                                        // resultValue = dblValTemp;
                                                        // resultValue = String.valueOf(dblValTemp);
                                                        Log.d(TAG, "▶ >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> " + tempTmp);
                                                    } catch (Exception e) {
                                                        Log.d(TAG, "▶ [ER].00095 " + e.toString());
                                                    }
//                                                    });
                                                } catch (Exception e) {
                                                    Log.d(TAG, "▶ [ER].00005 " + e.toString());
                                                }
                                            }
                                            // 소비전력
                                            else {
                                                int wattLower = 0;
                                                int wattUpper = 0;

                                                if (receiveCommand.equals("HT0100")) {
                                                    receiveResponseResult = "OK";
                                                    resultValue = receiveResponseResult;
                                                    receiveCompAgingResponse = "01";
                                                }
                                                else if (receiveCommand.equals("CM0101")) {
                                                    // receiveCommandResponse = readMessage.substring(9, 11);
                                                    // decElectricValue
                                                    receiveCompAgingResponse = readMessage.substring(9, 11);
//                                                    if(receiveCompAgingResponse.equals("01")) {
//                                                        testItemCounter = Integer.parseInt(arrTestItems[testItemIdx][2]);
//                                                        receiveResponseResult = "OK";
//                                                    }
//                                                    else {
                                                    receiveCommandResponse = readMessage.substring(9, 15);
//                                                    Log.i(TAG, "▶ [TH] receiveCommandResponse " + receiveCommandResponse);
                                                    decTemperatureValue = Integer.parseInt(receiveCommandResponse, 16);
//                                                    Log.i(TAG, "▶ [TH] decTemperatureValue " + decTemperatureValue);

                                                    temperatureData = hotTemperatureData;

                                                    for (int i = 0; i < temperatureData.size(); i++) {
                                                        temperature12Bit = (int) Math.round(Double.valueOf(temperatureData.get(i).get("clm_12_bit")));
//                                                        Log.i(TAG, "▶ [TH] temperatureData.get(" +  i+ ").get(\"clm_12_bit\") " + temperatureData.get(i).get("clm_12_bit") + " / " + temperature12Bit);
                                                        if (temperature12Bit > decTemperatureValue) {
                                                            decTemperature = temperatureTmp;
                                                            tvTemperature.setText(String.valueOf(decTemperature));

                                                            mapTestTemperature = new HashMap<>();
                                                            mapTestTemperature.put("clm_line", "lineId");
                                                            mapTestTemperature.put("clm_unit_id", "unitId");
                                                            mapTestTemperature.put("clm_timestamp", currentTimestamp);
                                                            mapTestTemperature.put("clm_product_temperature", decTemperature);
                                                            lstTestTemperature.add(mapTestTemperature);
                                                            break;
                                                        }
                                                        temperatureTmp = temperatureData.get(i).get("clm_temperature");
                                                    }

                                                    receiveCommandResponse = receiveCommandResponse + "," + temperatureTmp;

                                                    if(testItemCounter<10 && !compAgingStarted) {
                                                        compAgingStarted = true;
                                                        decTemperatureValueCompStart = temperatureTmp + "°C";;
                                                        temperatureValueCompDiff = decTemperatureValueCompStart;
                                                    }
                                                    else {
                                                        decTemperatureValueCompEnd = temperatureTmp + "°C";
                                                        temperatureValueCompDiff = decTemperatureValueCompStart + " ▶ " + decTemperatureValueCompEnd;
                                                    }

                                                    if(sendResultYn.equals("N")) {
                                                        runOnUiThread(new Runnable() {
                                                            public void run() {
                                                                tvCompWattValue.setText(String.valueOf(decElectricValue));
                                                                decElectricValueForComp = decElectricValue;
                                                                tvPopupProcessResultCompValue.setText(String.valueOf(decElectricValueForComp));
                                                            }
                                                        });
                                                    }

                                                    if (testItemCounter >= 30 && sendResultYn.equals("N")) {
                                                        // 20240522 결과업로드 지연 방지
                                                        new Thread(() -> {
                                                            try {
                                                                String checkValue = "";
                                                                for (int i = 0; i < listItemAdapter.getCount(); i++) {
                                                                    VoTestItem voTestItem = ((VoTestItem) listItemAdapter.getItem(i));
                                                                    checkValue += "&clm_" + ((i < 10) ? "0" : "") + (i + 1) + "=" + voTestItem.getTest_item_command() + ":" + voTestItem.getTest_result_check_value();
                                                                }
                                                                checkValue += "&clm_pump_watt=" + decElectricValueForPump;
                                                                checkValue += "&clm_comp_diff=" + decTemperatureValueCompStart + "," + decTemperatureValueCompEnd;
                                                                checkValue += "&clm_comp_watt=" + decElectricValueForComp;
                                                                checkValue += "&clm_heater_watt=" + decElectricValueForHeater;
                                                                checkValue += "&clm_test=T";
                                                                String targetUrl = "http://" + serverIp + "/OVIO/UpdateResultTestInfo.jsp?clm_product_serial_no=" + productSerialNo + "&clm_test_process=" + 3 + "&clm_test_result=" + ((testNgCnt > 0) ? "NG" : "OK") + checkValue;
                                                                targetUrl = "http://" + serverIp + "/OVIO/UpdateResultTestInfo.jsp?clm_product_serial_no=" + productSerialNo + "&clm_test_process=" + 3 + "&clm_test_result=" + "OK" + "&clm_model_id=" + globalModelId + checkValue;
                                                                // targetUrl = "http://" + serverIp + "/OVIO/UpdateResultTestInfo.jsp?clm_product_serial_no=" + productSerialNo + "&clm_test_process=" + 3 + "&clm_test_result=" + "OK" + checkValue;
                                                                Log.i(TAG, "▶ [BI] targetUrl " + targetUrl);

                                                                URL url = new URL(targetUrl);
                                                                connection = (HttpURLConnection) url.openConnection();

                                                                connection.setReadTimeout(300); // read 시 타임아웃 시간
                                                                connection.setConnectTimeout(500);  // 서버 접속 시 연결 타임아웃 시간
                                                                connection.setRequestMethod("GET");
                                                                connection.setDoInput(true);
                                                                connection.setDoOutput(true);

                                                                int responseCode = connection.getResponseCode();
                                                                if (responseCode == HttpURLConnection.HTTP_OK) {
                                                                    Log.i(TAG, "HTTP OK 성공");
                                                                    sendResultYn = "Y";
                                                                    connection.disconnect();
                                                                } else {
                                                                    Log.i(TAG, "HTTP OK 안됨");
                                                                    connection.disconnect();
                                                                }

                                                                if (connection != null) {
                                                                    connection.disconnect();
                                                                }
                                                            } catch (Exception e) {
                                                                Log.d(TAG, "▶ [ER].00007 " + e.toString());
                                                                e.printStackTrace();
                                                            }
                                                        }).start();
                                                    }

                                                    if (testItemCounter < 30) {
                                                        if (receiveCommandResponseOK.equals(receiveCommand)) {
                                                            return;
                                                        }
                                                        runOnUiThread(new Runnable() {
                                                            public void run() {
                                                                // tvCompWattValue.setText(String.valueOf(decElectricValue));
//                                                                tvCompWattValue.setText(String.valueOf(decElectricValueForComp));
//                                                                decElectricValueForComp = decElectricValue;
//                                                                tvPopupProcessResultCompValue.setText(String.valueOf(decElectricValueForComp));
                                                            }
                                                        });
                                                        wattLower = Integer.parseInt(lstSpecData.get(0).get("clm_value_watt")) - Integer.parseInt(lstSpecData.get(0).get("clm_test_lower_value_02"));
                                                        wattUpper = Integer.parseInt(lstSpecData.get(0).get("clm_value_watt")) + Integer.parseInt(lstSpecData.get(0).get("clm_test_upper_value_02"));
                                                        receiveResponseResult = (wattLower < decElectricValue && decElectricValue < wattUpper) ? "OK" : "NG";
                                                        if(receiveResponseResult.equals("OK")) { receiveCommandResponseOK = receiveCommand; }
                                                        resultValue = String.valueOf(decElectricValue);
                                                    }
//                                                    }

//                                                    if(readMessage.indexOf(("\u0002"))==0) {
//                                                        testItemCounter = Integer.parseInt(arrTestItems[testItemIdx][2]);
//                                                    }

                                                    Log.i(TAG, "▶ [BT] 검사 응답 신호 수신.01 [T:" + testTotalCounter + " / " + totalTimeCnt + "][C:" + testItemCounter + " / " + arrTestItems[testItemIdx][2] + "][C:" + testItemIdx + " / " + arrTestItems.length + "][S:" + currentTestItem + "][G:" + disconnectCheckCount + "=" + receivedMessageCnt + "-" + sendingMessageCnt + "][R:" + receiveCommand + "][I:" + readMessage.substring(readMessage.indexOf("\u0002") + 1, readMessage.indexOf("\u0003")) + "][T:" + decTemperatureValueCompStart + " ▶ " + decTemperatureValueCompEnd + "]  " + wattLower + " < " + decElectricValue + " < " + wattUpper);
                                                }
                                                else if(receiveCommand.equals("CM0102")) {
                                                    receiveCompAgingResponse = "02";
                                                    testItemCounter = Integer.parseInt(arrTestItems[testItemIdx][2]);
                                                    // receiveResponseResult = "OK";
                                                    compressureResultCnt++;

                                                    if (compressureResultCnt>=10) {
                                                        // receiveCommandResponseOK = receiveCommand;
                                                        receiveResponseResult = "OK";
                                                    } else {
                                                        receiveResponseResult = "NG";
                                                    }

                                                    if (receiveResponseResult.equals("OK")) {
                                                        testItemCounter = Integer.parseInt(arrTestItems[testItemIdx][2]);
                                                    }

                                                    Log.i(TAG, "▶ [BT] 검사 응답 신호 수신.02 [T:" + testTotalCounter + " / " + totalTimeCnt + "][C:" + testItemCounter + " / " + arrTestItems[testItemIdx][2] + "][C:" + testItemIdx + " / " + arrTestItems.length + "][S:" + currentTestItem + "][G:" + disconnectCheckCount + "=" + receivedMessageCnt + "-" + sendingMessageCnt + "][R:" + receiveCommand + "][I:" + readMessage.substring(readMessage.indexOf("\u0002") + 1, readMessage.indexOf("\u0003")) + "][T:" + decTemperatureValueCompStart + " ▶ " + decTemperatureValueCompEnd + "] " + wattLower + " < " + decElectricValue + " < " + wattUpper);
                                                }
                                                /*
                                                else if(receiveCommand.equals("CM0103")) {
                                                    receiveCompAgingResponse = "03";
                                                    testItemCounter = Integer.parseInt(arrTestItems[testItemIdx][2]);
                                                    // receiveResponseResult = "OK";
                                                    compressureResultCnt++;

                                                    if (compressureResultCnt>=10) {
                                                        // receiveCommandResponseOK = receiveCommand;
                                                        receiveResponseResult = "OK";
                                                    } else {
                                                        receiveResponseResult = "NG";
                                                    }

                                                    if (receiveResponseResult.equals("OK")) {
                                                        testItemCounter = Integer.parseInt(arrTestItems[testItemIdx][2]);
                                                    }

                                                    Log.i(TAG, "▶ [BT] 검사 응답 신호 수신 [T:" + testTotalCounter + " / " + totalTimeCnt + "][C:" + testItemCounter + " / " + arrTestItems[testItemIdx][2] + "][C:" + testItemIdx + " / " + arrTestItems.length + "][S:" + currentTestItem + "][G:" + disconnectCheckCount + "=" + receivedMessageCnt + "-" + sendingMessageCnt + "][R:" + receiveCommand + "][I:" + readMessage.substring(readMessage.indexOf("\u0002") + 1, readMessage.indexOf("\u0003")) + "] " + wattLower + " < " + decElectricValue + " < " + wattUpper);
                                                }
                                                */
                                                else if(receiveCommand.equals("VR0101")) {
//                                                    receiveCompAgingResponse = "02";
//                                                    testItemCounter = Integer.parseInt(arrTestItems[testItemIdx][2]);
                                                    receiveResponseResult = "OK";

                                                    Log.i(TAG, "▶ [BT] 검사 응답 신호 수신 [T:" + testTotalCounter + " / " + totalTimeCnt + "][C:" + testItemCounter + " / " + arrTestItems[testItemIdx][2] + "][C:" + testItemIdx + " / " + arrTestItems.length + "][S:" + currentTestItem + "][G:" + disconnectCheckCount + "=" + receivedMessageCnt + "-" + sendingMessageCnt + "][R:" + receiveCommand + "][I:" + readMessage.substring(readMessage.indexOf("\u0002") + 1, readMessage.indexOf("\u0003")) + "] " + wattLower + " < " + decElectricValue + " < " + wattUpper);
                                                }
                                                else if(receiveCommand.equals("RE0101")) {
//                                                    receiveCompAgingResponse = "02";
//                                                    testItemCounter = Integer.parseInt(arrTestItems[testItemIdx][2]);
                                                    receiveResponseResult = "OK";

                                                    Log.i(TAG, "▶ [BT] 검사 응답 신호 수신 [T:" + testTotalCounter + " / " + totalTimeCnt + "][C:" + testItemCounter + " / " + arrTestItems[testItemIdx][2] + "][C:" + testItemIdx + " / " + arrTestItems.length + "][S:" + currentTestItem + "][G:" + disconnectCheckCount + "=" + receivedMessageCnt + "-" + sendingMessageCnt + "][R:" + receiveCommand + "][I:" + readMessage.substring(readMessage.indexOf("\u0002") + 1, readMessage.indexOf("\u0003")) + "] " + wattLower + " < " + decElectricValue + " < " + wattUpper);
                                                }
                                                else if (receiveCommand.equals("HT0101")) {
                                                    // receiveCommandResponse = readMessage.substring(9, 11);
                                                    // decElectricValue
                                                    receiveCommandResponse = readMessage.substring(9, 15);
                                                    decTemperatureValue = Integer.parseInt(receiveCommandResponse, 16);

                                                    temperatureData = coldTemperatureData;

                                                    for (int i = 0; i < temperatureData.size(); i++) {
                                                        // Log.i(TAG, "▶ [TH] temperatureData.get(" +  i+ ").get(\"clm_12_bit\") " + temperatureData.get(i).get("clm_12_bit"));
                                                        temperature12Bit = (int) Math.round(Double.valueOf(temperatureData.get(i).get("clm_12_bit")));
                                                        if (temperature12Bit > decTemperatureValue) {
                                                            decTemperature = temperatureTmp;
                                                            tvTemperature.setText(String.valueOf(decTemperature));

                                                            mapTestTemperature = new HashMap<>();
                                                            mapTestTemperature.put("clm_line", "lineId");
                                                            mapTestTemperature.put("clm_unit_id", "unitId");
                                                            mapTestTemperature.put("clm_timestamp", currentTimestamp);
                                                            mapTestTemperature.put("clm_product_temperature", decTemperature);
                                                            lstTestTemperature.add(mapTestTemperature);
                                                            break;
                                                        }
                                                        temperatureTmp = temperatureData.get(i).get("clm_temperature");
                                                    }

                                                    receiveCommandResponse = receiveCommandResponse + "," + temperatureTmp;

                                                    if (testItemCounter < 5) {
                                                        runOnUiThread(new Runnable() {
                                                            public void run() {
                                                                if (receiveCommandResponseOK.equals(receiveCommand)) {
                                                                    return;
                                                                }
                                                                tvHeaterWattValue.setText(String.valueOf(decElectricValue));
                                                                decElectricValueForHeater = decElectricValue;
                                                                tvPopupProcessResultHeaterValue.setText(String.valueOf(decElectricValue));
                                                            }
                                                        });
                                                        wattLower = Integer.parseInt(lstSpecData.get(0).get("clm_value_watt")) - Integer.parseInt(lstSpecData.get(0).get("clm_test_lower_value_02"));
                                                        wattUpper = Integer.parseInt(lstSpecData.get(0).get("clm_value_watt")) + Integer.parseInt(lstSpecData.get(0).get("clm_test_upper_value_02"));
                                                        receiveResponseResult = (wattLower < decElectricValue && decElectricValue < wattUpper) ? "OK" : "NG";
                                                        if (receiveResponseResult.equals("OK")) {
                                                            receiveCommandResponseOK = receiveCommand;
                                                            testItemCounter = Integer.parseInt(arrTestItems[testItemIdx][2]);
                                                        }
                                                        resultValue = String.valueOf(decElectricValue);
                                                    }
                                                    resultInfo = "";
                                                    resultInfo = wattLower + " < " + decElectricValue + " < " + wattUpper;
                                                    Log.i(TAG, "▶ [BT] 검사 응답 신호 수신 [T:" + testTotalCounter + " / " + totalTimeCnt + "][C:" + testItemCounter + " / " + arrTestItems[testItemIdx][2] + "][C:" + testItemIdx + " / " + arrTestItems.length + "][S:" + currentTestItem + "][G:" + disconnectCheckCount + "=" + receivedMessageCnt + "-" + sendingMessageCnt + "][R:" + receiveCommand + "] " + wattLower + " < " + decElectricValue + " < " + wattUpper);
                                                } else if (receiveCommand.equals("PM0101")) {
//                                                    if (testItemCounter > 3 && testItemCounter < Integer.parseInt(arrTestItems[testItemIdx][2])) {
//                                                        runOnUiThread(new Runnable() {
//                                                            public void run() {
//                                                                if (receiveCommandResponseOK.equals(receiveCommand)) {
//                                                                    return;
//                                                                }
//                                                                tvPumpWattValue.setText(String.valueOf(decElectricValue));
//                                                            }
//                                                        });
//                                                        tvPumpWattValue.setText(String.valueOf(decElectricValue));
                                                    decElectricValueForPump = decElectricValue;
                                                    wattLower = Integer.parseInt(lstSpecData.get(0).get("clm_value_watt")) - Integer.parseInt(lstSpecData.get(0).get("clm_test_lower_value_02"));
                                                    wattUpper = Integer.parseInt(lstSpecData.get(0).get("clm_value_watt")) + Integer.parseInt(lstSpecData.get(0).get("clm_test_upper_value_02"));
                                                    receiveResponseResult = (wattLower < decElectricValue && decElectricValue < wattUpper) ? "OK" : "NG";
                                                    receiveResponseResult = (-1 < decElectricValue) ? "OK" : (pumpOkCnt>0)?"OK":"NG";
//                                                        receiveResponseResult = "OK";
//                                                        resultValue = String.valueOf(decElectricValue);
                                                    if (receiveResponseResult.equals("OK")) {
                                                        receiveCommandResponseOK = receiveCommand;
                                                        runOnUiThread(new Runnable() {
                                                            public void run() {
                                                                if(pumpOkCnt<=0) {
                                                                    if(decElectricValue>decElectricValueTmp) {
                                                                        tvPumpWattValue.setText(String.valueOf(decElectricValue));
                                                                        decElectricValueForPump = decElectricValue;
                                                                        resultValue = String.valueOf(decElectricValue);
                                                                    }
                                                                }
                                                                decElectricValueTmp = decElectricValue;
                                                            }
                                                        });
//                                                            testItemCounter = Integer.parseInt(arrTestItems[testItemIdx][2]);
                                                        pumpOkCnt++;
                                                    }
                                                    Log.i(TAG, "▶ [PM] 펌프 소비 전력 " + wattLower + " < " + decElectricValue + " < " + wattUpper);
//                                                    }
                                                    resultInfo = "";
                                                    resultInfo = wattLower + " < " + decElectricValue + " < " + wattUpper;
                                                    Log.i(TAG, "▶ [BT] 검사 응답 신호 수신 [T:" + testTotalCounter + " / " + totalTimeCnt + "][C:" + testItemCounter + " / " + arrTestItems[testItemIdx][2] + "][C:" + testItemIdx + " / " + arrTestItems.length + "][S:" + currentTestItem + "][G:" + disconnectCheckCount + "=" + receivedMessageCnt + "-" + sendingMessageCnt + "][R:" + receiveCommand + "] " + wattLower + " < " + decElectricValue + " < " + wattUpper);
                                                } else if (receiveCommand.equals("SV0101")) {
                                                    if(testItemCounter>4) {
                                                        wattLower = Integer.parseInt(lstSpecData.get(0).get("clm_value_watt")) - Integer.parseInt(lstSpecData.get(0).get("clm_test_lower_value_02"));
                                                        wattUpper = Integer.parseInt(lstSpecData.get(0).get("clm_value_watt")) + Integer.parseInt(lstSpecData.get(0).get("clm_test_upper_value_02"));
                                                        Log.i(TAG, "▶ [BT] 검사 응답 신호 수신 [T:" + testTotalCounter + " / " + totalTimeCnt + "][C:" + testItemCounter + " / " + arrTestItems[testItemIdx][2] + "][C:" + testItemIdx + " / " + arrTestItems.length + "][S:" + currentTestItem + "][G:" + disconnectCheckCount + "=" + receivedMessageCnt + "-" + sendingMessageCnt + "][R:" + receiveCommand + "] " + wattLower + " < " + decElectricValue + " < " + wattUpper);
                                                        receiveResponseResult = (wattLower < decElectricValue && decElectricValue < wattUpper) ? "OK" : "NG";
                                                        // if(receiveResponseResult.equals("OK")) { receiveCommandResponseOK = receiveCommand; }
                                                        resultValue = String.valueOf(decElectricValue);
                                                        resultInfo = "";
                                                        resultInfo = wattLower + " < " + decElectricValue + " < " + wattUpper;
                                                    }
                                                } else if (receiveCommand.equals("FM0100")) {
                                                    if(testItemCounter>4) {
                                                        wattLower = Integer.parseInt(lstSpecData.get(0).get("clm_value_watt")) - Integer.parseInt(lstSpecData.get(0).get("clm_test_lower_value_02"));
                                                        wattUpper = Integer.parseInt(lstSpecData.get(0).get("clm_value_watt")) + Integer.parseInt(lstSpecData.get(0).get("clm_test_upper_value_02"));
                                                        Log.i(TAG, "▶ [BT] 검사 응답 신호 수신 [T:" + testTotalCounter + " / " + totalTimeCnt + "][C:" + testItemCounter + " / " + arrTestItems[testItemIdx][2] + "][C:" + testItemIdx + " / " + arrTestItems.length + "][S:" + currentTestItem + "][G:" + disconnectCheckCount + "=" + receivedMessageCnt + "-" + sendingMessageCnt + "][R:" + receiveCommand + "] " + wattLower + " < " + decElectricValue + " < " + wattUpper);
                                                        receiveResponseResult = (wattLower < decElectricValue && decElectricValue < wattUpper) ? "OK" : "NG";
                                                        // if(receiveResponseResult.equals("OK")) { receiveCommandResponseOK = receiveCommand; }
                                                        resultValue = String.valueOf(decElectricValue);
                                                        resultInfo = "";
                                                        resultInfo = wattLower + " < " + decElectricValue + " < " + wattUpper;
                                                    }
                                                } else if (receiveCommand.equals("SV0201")) {
                                                    if(testItemCounter>4) {
                                                        wattLower = Integer.parseInt(lstSpecData.get(0).get("clm_value_watt")) - Integer.parseInt(lstSpecData.get(0).get("clm_test_lower_value_02"));
                                                        wattUpper = Integer.parseInt(lstSpecData.get(0).get("clm_value_watt")) + Integer.parseInt(lstSpecData.get(0).get("clm_test_upper_value_02"));
                                                        Log.i(TAG, "▶ [BT] 검사 응답 신호 수신 [T:" + testTotalCounter + " / " + totalTimeCnt + "][C:" + testItemCounter + " / " + arrTestItems[testItemIdx][2] + "][C:" + testItemIdx + " / " + arrTestItems.length + "][S:" + currentTestItem + "][G:" + disconnectCheckCount + "=" + receivedMessageCnt + "-" + sendingMessageCnt + "][R:" + receiveCommand + "] " + wattLower + " < " + decElectricValue + " < " + wattUpper);
                                                        receiveResponseResult = (wattLower < decElectricValue && decElectricValue < wattUpper) ? "OK" : "NG";
                                                        if (receiveResponseResult.equals("OK")) {
                                                            receiveCommandResponseOK = receiveCommand;
                                                        }
                                                        resultValue = String.valueOf(decElectricValue);
                                                        resultInfo = "";
                                                        resultInfo = wattLower + " < " + decElectricValue + " < " + wattUpper;
                                                    }
                                                } else if (receiveCommand.equals("SV0301")) {
                                                    if(testItemCounter>4) {
                                                        wattLower = Integer.parseInt(lstSpecData.get(0).get("clm_value_watt")) - Integer.parseInt(lstSpecData.get(0).get("clm_test_lower_value_02"));
                                                        wattUpper = Integer.parseInt(lstSpecData.get(0).get("clm_value_watt")) + Integer.parseInt(lstSpecData.get(0).get("clm_test_upper_value_02"));
                                                        Log.i(TAG, "▶ [BT] 검사 응답 신호 수신 [T:" + testTotalCounter + " / " + totalTimeCnt + "][C:" + testItemCounter + " / " + arrTestItems[testItemIdx][2] + "][C:" + testItemIdx + " / " + arrTestItems.length + "][S:" + currentTestItem + "][G:" + disconnectCheckCount + "=" + receivedMessageCnt + "-" + sendingMessageCnt + "][R:" + receiveCommand + "] " + wattLower + " < " + decElectricValue + " < " + wattUpper);
                                                        receiveResponseResult = (wattLower < decElectricValue && decElectricValue < wattUpper) ? "OK" : "NG";
                                                        if (receiveResponseResult.equals("OK")) {
                                                            receiveCommandResponseOK = receiveCommand;
                                                        }
                                                        resultValue = String.valueOf(decElectricValue);
                                                        resultInfo = "";
                                                        resultInfo = wattLower + " < " + decElectricValue + " < " + wattUpper;
                                                    }
                                                } else if (receiveCommand.equals("SV0401")) {
                                                    if(testItemCounter>4) {
                                                        wattLower = Integer.parseInt(lstSpecData.get(0).get("clm_value_watt")) - Integer.parseInt(lstSpecData.get(0).get("clm_test_lower_value_02"));
                                                        wattUpper = Integer.parseInt(lstSpecData.get(0).get("clm_value_watt")) + Integer.parseInt(lstSpecData.get(0).get("clm_test_upper_value_02"));
                                                        Log.i(TAG, "▶ [BT] 검사 응답 신호 수신 [T:" + testTotalCounter + " / " + totalTimeCnt + "][C:" + testItemCounter + " / " + arrTestItems[testItemIdx][2] + "][C:" + testItemIdx + " / " + arrTestItems.length + "][S:" + currentTestItem + "][G:" + disconnectCheckCount + "=" + receivedMessageCnt + "-" + sendingMessageCnt + "][R:" + receiveCommand + "] " + wattLower + " < " + decElectricValue + " < " + wattUpper);
                                                        receiveResponseResult = (wattLower < decElectricValue && decElectricValue < wattUpper) ? "OK" : "NG";
                                                        if (receiveResponseResult.equals("OK")) {
                                                            receiveCommandResponseOK = receiveCommand;
                                                        }
                                                        resultValue = String.valueOf(decElectricValue);
                                                        resultInfo = "";
                                                        resultInfo = wattLower + " < " + decElectricValue + " < " + wattUpper;
                                                    }
                                                }
                                                else if(receiveCommand.equals("CM0100")) {
                                                    receiveResponseResult = "";
                                                    receiveResponseResultTxt = "";
                                                    // receiveResponseResult = (receiveCompAgingResponse.equals("02"))?"OK":"NG";
                                                    // receiveResponseResultTxt = (receiveCompAgingResponse.equals("02"))?"성공":"실패";
                                                    receiveResponseResult = (receiveCompAgingResponse.equals("02"))?"OK":"OK";
                                                    receiveResponseResultTxt = (receiveCompAgingResponse.equals("02"))?"성공":"성공";
                                                    receiveResponseResultTxt = "COMP(AC) 냉각 성능 " + receiveResponseResultTxt;
                                                }
                                                else {
                                                    if (receiveCommandResponse.equals(lstSpecData.get(0).get("clm_test_response_value"))) {
                                                        receiveCommandResponseOK = receiveCommand;
                                                        receiveResponseResult = "OK";
                                                    } else {
                                                        receiveResponseResult = "NG";
                                                    }

                                                    Log.i(TAG, "▶ [BT] 검사 응답 신호 수신 [T:" + testTotalCounter + " / " + totalTimeCnt + "][C:" + testItemCounter + " / " + arrTestItems[testItemIdx][2] + "][C:" + testItemIdx + " / " + arrTestItems.length + "][S:" + currentTestItem + "][G:" + disconnectCheckCount + "=" + receivedMessageCnt + "-" + sendingMessageCnt + "][R:" + receiveCommand + "] " + receiveCommandResponse + " / " + lstSpecData.get(0).get("clm_test_response_value") + " / " + receiveResponseResult);
                                                    resultValue = String.valueOf(receiveCommandResponse);

                                                    // 파우셋 UV LED
                                                    if (receiveCommand.equals("UV0201")) {
                                                        if (receiveResponseResult.equals("NG")) {
                                                            // testItemCounter = Integer.parseInt(arrTestItems[testItemIdx][2]);
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        for (int i = 0; i < listItemAdapter.getCount(); i++) {
                                            if (receiveCommand.equals(((VoTestItem) listItemAdapter.getItem(i)).getTest_item_command())) {
                                                if (receiveCommandResponseOK.equals(receiveCommand) && receiveResponseResult.equals("NG")) {}
                                                if("CM0100".equals(((VoTestItem) listItemAdapter.getItem(i)).getTest_item_command())) {
                                                    ((VoTestItem) listItemAdapter.getItem(i)).setTest_item_name(((VoTestItem) listItemAdapter.getItem(i)).getTest_item_name() + " / " + receiveResponseResultTxt);
                                                }
                                                if(receiveCommand.indexOf("CM0101")>-1) { ((VoTestItem) listItemAdapter.getItem(i)).setTest_item_info(temperatureValueCompDiff); }
                                                if(receiveCommand.indexOf("TH0101")>-1) { ((VoTestItem) listItemAdapter.getItem(i)).setTest_item_info(tempTmp); }
                                                if(receiveCommand.indexOf("TH0201")>-1) { ((VoTestItem) listItemAdapter.getItem(i)).setTest_item_info(tempTmp); }
                                                if(receiveCommand.indexOf("TH0301")>-1) { ((VoTestItem) listItemAdapter.getItem(i)).setTest_item_info(tempTmp); }
                                                if(receiveCommand.indexOf("SV0101")>-1) { ((VoTestItem) listItemAdapter.getItem(i)).setTest_item_info(String.valueOf(decElectricValue)); }
                                                if(receiveCommand.indexOf("SV0201")>-1) { ((VoTestItem) listItemAdapter.getItem(i)).setTest_item_info(String.valueOf(decElectricValue)); }
                                                if(receiveCommand.indexOf("SV0301")>-1) { ((VoTestItem) listItemAdapter.getItem(i)).setTest_item_info(String.valueOf(decElectricValue)); }
                                                if(receiveCommand.indexOf("SV0401")>-1) { ((VoTestItem) listItemAdapter.getItem(i)).setTest_item_info(String.valueOf(decElectricValue)); }
                                                if(receiveCommand.indexOf("FM0100")>-1) { ((VoTestItem) listItemAdapter.getItem(i)).setTest_item_info(String.valueOf(decElectricValue)); }
                                                ((VoTestItem) listItemAdapter.getItem(i)).setTest_result_check_value(resultValue);
                                                ((VoTestItem) listItemAdapter.getItem(i)).setTest_item_result(receiveResponseResult);
                                                ((VoTestItem) listItemAdapter.getItem(i)).setTest_finish_yn("Y");
                                                Log.i(TAG, "▶ [BT] " + (readMessage.substring(readMessage.indexOf(("\u0002")) + 1, readMessage.indexOf(("\u0003")))) + " / " + receiveCommand + " / " + receiveResponseResult + " / " + decElectricValue + " W");
                                                if (mode_type.equals("T")) {
                                                    // runOnUiThread(new Runnable() {
                                                    // public void run() {

                                                    String compAgingCondition = "";
                                                    if(receiveCommand.indexOf("CM01")>-1) {
                                                        compAgingCondition = decElectricValue + "W / " + receiveCompAgingResponse;
                                                    }
                                                    else {
                                                        compAgingCondition = decElectricValue + "W";
                                                    }
                                                    log_text_param += "[" + getCurrentDatetime(datetimeFormat) + " / " + String.format("%03d", testItemCounter) + " / " + String.format("%03d", Integer.parseInt(arrTestItems[testItemIdx][2])) + "] " + (readMessage.substring(readMessage.indexOf(("\u0002")) + 1, readMessage.indexOf(("\u0003")))) + " / " + receiveCommand + " / " + receiveResponseResult + "(" + receiveCommandResponse + ") / " + compAgingCondition;
                                                    log_text = "▶ [" + getCurrentDatetime(datetimeFormat) + " / " + String.format("%03d", testItemCounter) + " / " + String.format("%03d", Integer.parseInt(arrTestItems[testItemIdx][2])) + "] " + (readMessage.substring(readMessage.indexOf(("\u0002")) + 1, readMessage.indexOf(("\u0003")))) + " / " + receiveCommand + " / " + receiveResponseResult + "(" + receiveCommandResponse + ") / " + decElectricValue + "W\n" + log_text;
                                                    // log_text = "▶ [" + getCurrentDatetime(datetimeFormat) + " / " + String.format("%03d", testItemCounter) + "] " + (readMessage.substring(readMessage.indexOf(("\u0002")) + 1, readMessage.indexOf(("\u0003")))) + " / " + receiveCommand + " / " + receiveResponseResult + "(" + receiveCommandResponse + ") / " + decElectricValue + " W\n" + log_text;
                                                    tv_log.setText(log_text);

                                                    new Thread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            try {
                                                                // log_text_param += "[" + getCurrentDatetime(datetimeFormat) + " / " + String.format("%03d", testItemCounter) + " / " + String.format("%03d", Integer.parseInt(arrTestItems[testItemIdx][2])) + "] " + (readMessage.substring(readMessage.indexOf(("\u0002")) + 1, readMessage.indexOf(("\u0003")))) + " / " + receiveCommand + " / " + receiveResponseResult + "(" + receiveCommandResponse + ") / " + decElectricValue + "W";
                                                                String decElectricValueList = "";
                                                                if (receiveCommand.indexOf("RE0101") > -1) {
                                                                    for (int i = 0; i < lstWatt.size(); i++) {
                                                                        decElectricValueList += lstWatt.get(i) + ",";
                                                                    }

                                                                    decElectricValueList = decElectricValueList.substring(0, decElectricValueList.length() - 1);
                                                                    decElectricValueList = "&clm_watt_log=" + decElectricValueList;
                                                                }

                                                                testTaskThread = new ActivityModel_0003.RequestTestTaskThreadAsync();
                                                                urlTestTaskStr = "http://" + serverIp + "/OVIO/TestTaskInfoUpdate.jsp" + "?clm_test_task_log=" + URLEncoder.encode(log_text_param) + "&clm_test_unit_seq=" + unit_no + "&clm_unit_ip=" + ipAddress + "&clm_product_serial_no=" + productSerialNo + "&clm_test_process_id=" + testProcessId + "&clm_model_id=" + globalModelId + decElectricValueList;

                                                                testProcessId = (receiveCommand.indexOf("RE0101") > -1) ? "" : testProcessId;
                                                                // testProcessId = "";
                                                                // Log.i(TAG, "▶ [PS] urlTestTaskStr " + urlTestTaskStr);
                                                                testTaskThread.execute();
                                                                log_text_param = "";
                                                            }
                                                            catch (Exception e) {
                                                                Log.d(TAG, "▶ [ER].00008 " + e.toString());
                                                            }
                                                        }
                                                    }).start();
                                                    // }
                                                    // });
                                                }
                                                if (testItemCounter == Integer.parseInt(arrTestItems[testItemIdx][2])) {
                                                    // sendBtMessage("ST0201");
                                                }
                                            }
                                        }

                                        testOkCnt = 0;
                                        testNgCnt = 0;
                                        for (int i = 0; i < listItemAdapter.getCount(); i++) {
                                            if (((VoTestItem) listItemAdapter.getItem(i)).getTest_item_result().equals("OK")) {
                                                testOkCnt++;
                                            }
                                            if (((VoTestItem) listItemAdapter.getItem(i)).getTest_item_result().equals("NG")) {
                                                testNgCnt++;
                                            }
                                        }
                                        tvTestOkCnt.setText(String.valueOf(testOkCnt));
                                        tvTestNgCnt.setText(String.valueOf(testNgCnt));

                                        if (receiveCommand.equals("RE0101") || receiveCommand.contains("CM0102") || receiveCommand.contains("CM0103")) {
                                            new Thread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    try {
//                                                        if (server != null) {
//                                                            server.closeAllConnections();
//                                                            server.stop();
//                                                        }

                                                        tmrBTMessageSend.cancel();

                                                        btSearchOnOff = false;
                                                        btSocket.close();
                                                        btSocket = null;
                                                        deviceSelected = null;
                                                        runOnUiThread(new Runnable() {
                                                            public void run() {
                                                                setDisplayLightValueChange(0.5f);
                                                                cookie_preferences = getApplicationContext().getSharedPreferences("test_cookie_info", MODE_PRIVATE);
                                                                cookie_info = cookie_preferences.edit();

                                                                cookie_info.putString("test_start_datetime", lmsTestSeq);
                                                                cookie_info.putString("test_product_serial_no", productSerialNo);
                                                                cookie_info.commit();
                                                                Log.i(TAG, "▶ [PS] test_start_datetime.lmsTestSeq " + lmsTestSeq);
                                                                Log.i(TAG, "▶ [PS] testProcessId finished " + testProcessId);
                                                                // testProcessId = "";

                                                                // btnTestRestart.setOnTouchListener(null);
                                                                tvConnectBtRamp.setText("");
                                                                tvConnectBtRamp.setBackgroundColor(getBaseContext().getResources().getColor(R.color.red_01));

                                                                btnTestRestart.setBackgroundColor(getColor(R.color.green_02));
                                                                btnTestRestart.setTextColor(getColor(R.color.white));
                                                                // getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

                                                                ItemAdapterTestItem listItemResultAdapter = new ItemAdapterTestItem();

                                                                for (int i = 0; i < listItemAdapter.getCount(); i++) {
                                                                    Log.i(TAG, "▶ [RS] " + i + " " + ((VoTestItem) listItemAdapter.getItem(i)).VoMapInfo());
                                                                    if (((VoTestItem) listItemAdapter.getItem(i)).getTest_item_result().equals("NG")) {
                                                                        VoTestItem voTestItem = (VoTestItem) listItemAdapter.getItem(i);
                                                                        voTestItem.setTest_finish_yn("Y");
                                                                        voTestItem.setTest_item_result("NG");
                                                                        voTestItem.setTest_item_info(voTestItem.getTest_result_value());
                                                                        listItemResultAdapter.addItem(voTestItem);
                                                                    }
                                                                }

                                                                listItemResultAdapter.updateListAdapter();

                                                                if (testNgCnt > 0) {
                                                                    tvPopupProcessResult.setText("NG");
                                                                    tvPopupProcessResult.setTextSize(Dimension.SP, 150);
                                                                    tvPopupProcessResultCompValue.setTextSize(Dimension.SP, 60);
                                                                    tvPopupProcessResultHeaterValue.setTextSize(Dimension.SP, 60);
                                                                } else {
                                                                    tvPopupProcessResult.setText("OK");
                                                                }
                                                                // }

                                                                Map<String, String> mapTestHistory = new HashMap<>();
                                                                mapTestHistory.put("clm_test_result", ((testNgCnt > 0) ? "NG" : "OK"));
                                                                mapTestHistory.put("clm_test_ng_count", String.valueOf(testNgCnt));
                                                                mapTestHistory.put("clm_test_ok_count", String.valueOf(testOkCnt));
                                                                mapTestHistory.put("clm_test_history_seq", lmsTestSeq);
                                                                mapTestHistory.put("clm_test_model_id", arrTestModels[0][0]);
                                                                mapTestHistory.put("clm_test_model_name", arrTestModels[0][1]);
                                                                mapTestHistory.put("clm_test_model_nationality", arrTestModels[0][2]);
                                                                mapTestHistory.put("clm_test_timestamp", lmsTestTimestamp);
                                                                mapTestHistory.put("clm_test_comp_value", String.valueOf(currentCompWattValue));
                                                                mapTestHistory.put("clm_test_heater_value", String.valueOf(currentHeaterWattValue));
                                                                mapTestHistory.put("clm_comment", "");

                                                                test_info.putString("test_result", ((testNgCnt > 0) ? "NG" : "OK"));
                                                                test_info.putString("heater_value", String.valueOf(mainActivity.currentHeaterWattValueProp));
                                                                test_info.putString("comp_value", String.valueOf(mainActivity.currentCompWattValueProp));
                                                                test_info.commit();

                                                                TestData.insertProductTestHistory(getBaseContext(), mapTestHistory);

                                                                // ⚠️ 검사 상세 내역 저장
                                                                // 테이블이 없으면 생성
                                                                TestData.createProductTestHistoryDetail(getBaseContext());

                                                                // listItemAdapter의 모든 항목을 상세 내역으로 저장
                                                                for (int i = 0; i < listItemAdapter.getCount(); i++) {
                                                                    VoTestItem voTestItem = (VoTestItem) listItemAdapter.getItem(i);
                                                                    Map<String, String> mapTestHistoryDetail = new HashMap<>();
                                                                    mapTestHistoryDetail.put("clm_test_history_seq", lmsTestSeq);
                                                                    mapTestHistoryDetail.put("clm_test_item_seq", voTestItem.getTest_item_seq());
                                                                    mapTestHistoryDetail.put("clm_test_item_name", voTestItem.getTest_item_name());
                                                                    mapTestHistoryDetail.put("clm_test_item_command", voTestItem.getTest_item_command());
                                                                    mapTestHistoryDetail.put("clm_test_item_result", voTestItem.getTest_item_result());
                                                                    mapTestHistoryDetail.put("clm_test_item_value", voTestItem.getTest_item_value());
                                                                    mapTestHistoryDetail.put("clm_test_response_value", voTestItem.getTest_response_value());
                                                                    mapTestHistoryDetail.put("clm_test_result_value", voTestItem.getTest_result_value());
                                                                    mapTestHistoryDetail.put("clm_test_temperature", voTestItem.getTest_temperature());
                                                                    mapTestHistoryDetail.put("clm_test_electric_val", voTestItem.getTest_electric_val());
                                                                    mapTestHistoryDetail.put("clm_comment", "");
                                                                    TestData.insertProductTestHistoryDetail(getBaseContext(), mapTestHistoryDetail);
                                                                }

                                                                clTestResult.setVisibility(View.VISIBLE);

                                                                tvTestOkCnt.setText(String.valueOf(testOkCnt));
                                                                tvTestNgCnt.setText(String.valueOf(testNgCnt));

                                                                // 서버로 결과 전달

                                                                tmrFinishedRestart = new Timer();
                                                                ttFinishedRestart = new TimerTask() {
                                                                    @Override
                                                                    public void run() {
                                                                        Log.i(TAG, "▶ [BT] tmrFinishedRestart 검사 어플리케이션 종료 [" + restartCntFinished + " / " + restartCntMargin + "]");
                                                                        if (restartCntFinished == restartCntMargin) {
                                                                            restartCntFinished = 0;
                                                                            // restartApplication(getApplicationContext());
                                                                            finishedCorrectly = true;
                                                                            finishApplication(getApplicationContext());
                                                                            // 서버로 결과 전달
                                                                            tmrFinishedRestart.cancel();
                                                                        }
                                                                        restartCntFinished++;
                                                                    }
                                                                };
                                                                tmrFinishedRestart.schedule(ttFinishedRestart, 0, 1000);
                                                            }
                                                        });
                                                    } catch (Exception e) {
                                                        Log.d(TAG, "▶ [ER].00008 " + e.toString());
                                                    }
                                                }
                                            }).start();
                                        }

                                        receiveCommand = "";
                                    }
                                });

                                if (currentTestItem.indexOf("SN0101") > -1 || currentTestItem.indexOf("SN0201") > -1 || currentTestItem.indexOf("SN0301") > -1 || currentTestItem.indexOf("TA0101") > -1 || currentTestItem.indexOf("TA0201") > -1 || currentTestItem.indexOf("TA0301") > -1) {
                                    // Log.i(TAG, "▶ [BT] 검사 시작 신호 전송 [" + testItemCounter + " / " + arrTestItems[testItemIdx][2] + "][" + testItemIdx + " / " + arrTestItems.length + "].1 " + currentTestItem);
                                } else if (currentTestItem.indexOf("CM0101") > -1) {

                                }

                                if (testItemIdx == arrTestItems.length - 1) {
                                    // tmrBTMessageSend.cancel();
                                }

                                listItemAdapter.updateListAdapter();
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "▶ [ER] 00037 : " + e.toString());
                        }
                    }

                    if (msg.what == CONNECTING_STATUS) {
                        char[] sConnected;
                        if (msg.arg1 == 1) {
                            Log.i(TAG, "▶ [BT] 블루투스 디바이스 연결 성공 ");

                            runOnUiThread(new Runnable() {
                                public void run() {
                                    try {
                                        String[] btDeviceNameInfo = btDeviceName.split("_");
                                        tvConnectBtRamp.setText(btDeviceNameInfo[btDeviceNameInfo.length - 2] + "/" + btDeviceNameInfo[btDeviceNameInfo.length - 1]);
                                        tvConnectBtRamp.setBackgroundColor(getBaseContext().getResources().getColor(R.color.blue_01));
                                    } catch (Exception e) {
                                        Log.d(TAG, "▶ [ER].00067 " + e.toString());
                                    }
                                    // return null;
                                }
                            });

                            tmrBTMessageSend = new Timer();
                            ttBTMessageSend = new TimerTask() {
                                @Override
                                public void run() {
                                    if (!entranceCheck) {
                                        if (!barcodeReadCheck) {
                                            new Thread(() -> {
                                                callBarcodeInfoServer();
                                            }).start();
                                        }
                                        sendBtMessage(currentTestItem);
                                    } else {
                                        if (testItemIdx < arrTestItems.length) {
                                            currentTestItem = arrTestItems[testItemIdx][1];
                                            disconnectCheckCount = receivedMessageCnt - sendingMessageCnt;
                                            if (disconnectCheckCount > 5) {
                                                Log.i(TAG, "▶ [BT] 검사 항목 신호 송신.0 [" + currentTestItem + " / " + receiveCommand + "][T:" + testTotalCounter + " / " + totalTimeCnt + "][C:" + testItemCounter + " / " + arrTestItems[testItemIdx][2] + "][P:" + testItemIdx + " / " + arrTestItems.length + "][S:" + currentTestItem + "][G:" + disconnectCheckCount + "=" + receivedMessageCnt + "-" + sendingMessageCnt + "] 수신 없음 ");
                                                if (receiveCommand.equals("")) {
                                                    Log.i(TAG, "▶ [BT] 검사 항목 신호 송신.1 [" + currentTestItem + " / " + receiveCommand + "][T:" + testTotalCounter + " / " + totalTimeCnt + "][C:" + testItemCounter + " / " + arrTestItems[testItemIdx][2] + "][P:" + testItemIdx + " / " + arrTestItems.length + "][S:" + currentTestItem + "][G:" + disconnectCheckCount + "=" + receivedMessageCnt + "-" + sendingMessageCnt + "] 수신 없음 ");
                                                    /*
                                                    tmrBTMessageSend.cancel();
                                                    runOnUiThread(new Runnable() {
                                                        public void run() {
                                                            clAlert.setVisibility(View.GONE);
                                                            // tv_log.setText("");
                                                            if (clDialogForPreprocess.getVisibility() == View.VISIBLE) {
                                                                clDialogForPreprocess.setVisibility(View.GONE);
                                                                tvDialogMessage.setText("");
                                                            }
                                                            clAlert.setVisibility(View.VISIBLE);
                                                            tvAlertMessage.setText(getString(R.string.txt_received_no_data_from_product));
                                                            btnAlertClose.setOnClickListener(new Button.OnClickListener() {
                                                                @Override
                                                                public void onClick(View view) {
                                                                    clAlert.setVisibility(View.GONE);
                                                                }
                                                            });
                                                        }
                                                    });
                                                    */
                                                } else {
                                                }
                                            }
                                            Log.i(TAG, "▶ [BT] 검사 항목 신호 송신 [T:" + testTotalCounter + " / " + totalTimeCnt + "][C:" + testItemCounter + " / " + arrTestItems[testItemIdx][2] + "][P:" + testItemIdx + " / " + arrTestItems.length + "][S:" + currentTestItem + "][G:" + disconnectCheckCount + "=" + receivedMessageCnt + "-" + sendingMessageCnt + "]");

                                            if (testItemCounter == 1) {
                                            } else if (testItemCounter > 1 && testItemCounter < 10) {
                                            }

                                            if (Integer.parseInt(arrTestItems[testItemIdx][2]) <= testItemCounter) {
                                                testItemCounter = 0;
                                                testItemIdx++;
                                                runOnUiThread(new Runnable() {
                                                    public void run() {
                                                        lvTestItem.smoothScrollToPosition(testItemIdx);
                                                    }
                                                });
                                            } else {
                                                if (currentTestItem.indexOf("SN0101") > -1 || currentTestItem.indexOf("SN0201") > -1 || currentTestItem.indexOf("SN0301") > -1 || currentTestItem.indexOf("TA0101") > -1 || currentTestItem.indexOf("TA0201") > -1 || currentTestItem.indexOf("TA0301") > -1) {
                                                } else if (currentTestItem.indexOf("CM0101") > -1) {
                                                }
                                            }
                                            if (currentTestItem.equals("CM0100")) {
                                                int CM0101Index = 0;
                                                for (int i = 0; i < listItemAdapter.getCount(); i++) {
                                                    if ("CM0101".equals(((VoTestItem) listItemAdapter.getItem(i)).getTest_item_command())) {
                                                        CM0101Index = i;
                                                    }
                                                }

                                                // receiveResponseResult = (receiveCompAgingResponse.equals("02")) ? "OK" : "NG";
                                                // String receiveResponseResultTxt = (receiveCompAgingResponse.equals("02")) ? "성공" : "실패";
                                                receiveResponseResult = (receiveCompAgingResponse.equals("02")) ? "OK" : "OK";
                                                String receiveResponseResultTxt = (receiveCompAgingResponse.equals("02")) ? "성공" : "성공";

                                                ((VoTestItem) listItemAdapter.getItem(CM0101Index + 1)).setTest_item_name("COMP 냉각 성능 " + receiveResponseResultTxt);
                                                ((VoTestItem) listItemAdapter.getItem(CM0101Index + 1)).setTest_item_result(receiveResponseResult);
                                                ((VoTestItem) listItemAdapter.getItem(CM0101Index + 1)).setTest_result_value(receiveResponseResult + " / 01");
                                                ((VoTestItem) listItemAdapter.getItem(CM0101Index + 1)).setTest_finish_yn("Y");
                                            }

                                            sendingMessageCnt++;
                                            int testItemIdxTmp = 0;
                                            if (testItemIdx != arrTestItems.length) {
                                                sendBtMessage(currentTestItem);
                                                testItemIdxTmp = testItemIdx;
                                            } else {
                                                testItemIdxTmp = testItemIdx - 1;
                                            }
                                            log_text_param = "[" + getCurrentDatetime(datetimeFormat) + " / " + String.format("%03d", testItemCounter) + " / " + String.format("%03d", Integer.parseInt(arrTestItems[testItemIdxTmp][2])) + "] " + currentTestItem + " \t";
                                        }
                                    }

                                    runOnUiThread(new Runnable() {
                                        public void run() {
                                            tvEllapsedTimeCnt.setText(String.valueOf(testTotalCounter));
                                        }
                                    });

                                    testItemCounter++;
                                    testTotalCounter++;
                                }
                            };
                            tmrBTMessageSend.schedule(ttBTMessageSend, 0, 1000);
                        } else {
                            Log.i(TAG, "▶ [BT] 블루투스 디바이스 연결 실패 ");
                        }
                    }
                }
            };

            try {
                new Thread(() -> {
                    Log.i(TAG, "▶ [US] ▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶");
                    setFilters();  // Start listening notifications from UsbService
                    startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
                }).start();
            } catch (Exception e) {
                Log.d(TAG, "▶ [ER].00927 " + e.toString());
            }

            tmrVoltageProcess = new Timer();
            tmrVoltageTimerTask = new TimerTask() {
                @Override
                public void run() {
                    try {
                        // Log.i(TAG, "▶ [US] USB 서비스 체결 ");
                        if (usbService != null) {
                            plcCommand = "\u000500RSS0107%DW1006\u0004";
                            usbService.write(plcCommand.getBytes());
                        } else {
                            Log.i(TAG, "▶ [US] USB 서비스 체결 안됨 ");
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "▶ [ER].00013 " + e.toString());
                    }
                }
            };

            btSearchOnOff = true;
            if (!btSearchOnOff) {
                Log.i(TAG, "▶ [BT] 서버 미연결 후 블루투스 연결 시도");
            } else {
                Log.i(TAG, "▶ [BT] 서버 연결 후 블루투스 연결 시도 >>> ");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        bluetoothSearch();
                    }
                }).start();
            }
        } catch (Exception e) {
            Log.e(TAG, "▶ e " + e.toString());
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
        Log.i(TAG, "▶ [PS] 요청시간 : " + reqDate);
        Log.i(TAG, "▶ [PS] 현재시간 : " + curDate);
        Log.i(TAG, "▶ [PS] " + minute + "분 차이");

        return minute;
    }

    private void restartApplication(Context mContext) {
        new Thread(() -> {
            try {
                resultResponseValues = "";
                testTotalCounter = 0;
                testItemCounter = 0;
                testItemIdx = 0;
                entranceCheck = false;
                currentTestItem = "ST0101";

                if (tmrFinishedRestart != null) {
                    tmrFinishedRestart.cancel();
                    tmrFinishedRestart = null;
                }

                if (ttFinishedRestart != null) {
                    ttFinishedRestart.cancel();
                    ttFinishedRestart = null;
                }

                usbConnTryCnt = 0;
                if (usbReceiverRegisted) {
                    unregisterReceiver(usbReceiver);
                }
                if (usbConnPermissionGranted) {
                    unbindService(usbConnection);
                }

                usbReceiverRegisted = false;
                usbConnPermissionGranted = false;

//                if (server != null) {
//                    server.closeAllConnections();
//                    server.stop();
//                }

                if (tmrBTList != null) {
                    tmrBTList.cancel();
                    tmrBTList = null;
                }

                btSearchOnOff = false;
                if (btSocket != null) {
                    btSocket.close();
                }
                btSocket = null;
                deviceSelected = null;
                if (appResetTimerTask != null) {
                    appResetTimerTask = null;
                }
                if (tmrVoltageProcess != null) {
                    // tmrVoltageProcess.cancel();
                }
                if (tmrBTList != null) {
                    tmrBTList.cancel();
                }
                moveTaskToBack(true);
                finishAndRemoveTask();
                PackageManager packageManager = mContext.getPackageManager();
                Intent intent = packageManager.getLaunchIntentForPackage(mContext.getPackageName());
                ComponentName componentName = intent.getComponent();
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(0);
            } catch (Exception e) {
                Log.d(TAG, "▶ [ER].00118 " + e.toString());
            }
        }).start();
    }

    private void finishApplication(Context mContext) {
        new Thread(() -> {
            try {
                receiveCompAgingResponse = "01";
                testTotalCounter = 0;
                testItemCounter = 0;
                testItemIdx = 0;
                entranceCheck = false;
                currentTestItem = "ST0101";

                if (tmrFinishedRestart != null) {
                    tmrFinishedRestart.cancel();
                    tmrFinishedRestart = null;
                }

                if (ttFinishedRestart != null) {
                    ttFinishedRestart.cancel();
                    ttFinishedRestart = null;
                }

                if (tmrUnfinishedRestart != null) {
                    tmrUnfinishedRestart.cancel();
                    tmrUnfinishedRestart = null;
                }

                if (ttUnfinishedRestart != null) {
                    ttUnfinishedRestart.cancel();
                    ttUnfinishedRestart = null;
                }

                if (usbReceiverRegisted) {
                    unregisterReceiver(usbReceiver);
                }
                if (usbConnPermissionGranted) {
                    unbindService(usbConnection);
                }

                usbReceiverRegisted = false;
                usbConnPermissionGranted = false;

                if (tmrBTList != null) {
                    tmrBTList.cancel();
                    tmrBTList = null;
                }

                if (btListTimerTask != null) {
                    btListTimerTask.cancel();
                    btListTimerTask = null;
                }

                if (tmrBTMessageSend != null) {
                    tmrBTMessageSend.cancel();
                    tmrBTMessageSend = null;
                }

                if (btHandlerTmp != null) {
                    btHandlerTmp.removeCallbacksAndMessages(null);
                }

                if (btHandlerTmp != null) {
                    btHandlerTmp.removeCallbacksAndMessages(null);
                }
//                if (server != null)
//                    server.closeAllConnections();
//                server.stop();

                btSearchOnOff = false;
                if (btSocket != null) {
                    btSocket.close();
                }
                btSocket = null;
                deviceSelected = null;
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(mainActivity, new String[]{Manifest.permission.BLUETOOTH_SCAN}, 1001);
                }
                mBTAdapter.cancelDiscovery();

                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    Log.d(TAG, "▶ [ER] 00039 : " + e);
                }

                Intent intent = getIntent();
                finish();
                if (finishedCorrectly) {
                    finishedCorrectly = false;
                    startActivity(intent);
                }
            } catch (Exception e) {
                Log.d(TAG, "▶ [ER].00118 " + e.toString());
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

    class RequestThreadAsync extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                new Thread(() -> {
                    try {
                        String url = urlStr;
                        URL obj = new URL(url);
                        connection = (HttpURLConnection) obj.openConnection();

                        connection.setReadTimeout(3000); // read 시 타임아웃 시간
                        connection.setConnectTimeout(5000);  // 서버 접속 시 연결 타임아웃 시간
                        connection.setRequestMethod("POST");  // POST 방식으로 요청
                        connection.setDoInput(true);  // InputStream 으로 서버로부터 응답을 받겠다는 옵션
                        connection.setDoOutput(true); // OutputStream 으로 POST 데이터를 넘겨 주겠다는 옵션
                        connection.setRequestProperty("Content-Type", "application/json");    // application/json 형식으로 전송. (Request body 전달시 application/json 으로 서버에 전달)
                        connection.setRequestProperty("Cache-Control", "no-cache");

                        try {
                            int retCode = connection.getResponseCode();
                            Log.i(TAG, "▶ [PS] retCode data " + retCode);
                            if (retCode == HttpURLConnection.HTTP_OK) {
                                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                                // BufferedReader() : 엔터만 경계로 인식하고 받은 데이터를 String 으로 고정, Scanner 에 비해 빠름!
                                // InputStreamReader() : 지정된 문자 집합 내의 문자로 인코딩
                                // getInputStream() : url 에서 데이터를 읽어옴
                                String line = null;
                                String lineTmp = null;
                                while (true) {
                                    lineTmp = reader.readLine();
                                    if (lineTmp != null) {
                                        if (!lineTmp.equals("")) {
                                            line = lineTmp;
                                        }
                                    }
                                    if (lineTmp == null)
                                        break;

                                    final String data = line;

                                    httpHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (data != null) {
                                                if (!data.equals("")) {
                                                    Log.i(TAG, "▶ [PS] process data " + data);
                                                    jsonParsing("test_spec", data);
                                                }
                                            }
                                        }
                                    });
                                }
                                reader.close();
                            }

                            if (connection != null) {
                                // tmrVoltageProcess.schedule(tmrVoltageTimerTask, 0, 300);
                                // Log.i(TAG, "▶ [SI] conn.disconnect().1 >>>>>>>>>>>>>>>>>>>>>>>>>>> " + urlStr);
                                connection.disconnect();
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "▶ [ER].00919 " + e);
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "▶ [ER].00917 " + e);
                        e.printStackTrace();
                    }
                }).start();
            } catch (Exception e) {
                Log.d(TAG, "▶ [ER].00918 " + e);
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

    class RequestTestTaskThreadAsync extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                new Thread(() -> {
                    try {
                        String url = urlTestTaskStr;
                        URL obj = new URL(url);
                        connection = (HttpURLConnection) obj.openConnection();
                        // Log.i(TAG, "▶ [PS] RequestTestTaskThreadAsync.urlTestTaskStr " + urlTestTaskStr);

                        connection.setReadTimeout(750); // read 시 타임아웃 시간
                        connection.setConnectTimeout(999);  // 서버 접속 시 연결 타임아웃 시간
                        connection.setRequestMethod("GET");  // POST 방식으로 요청
                        connection.setDoInput(true);  // InputStream 으로 서버로부터 응답을 받겠다는 옵션
                        connection.setDoOutput(true); // OutputStream 으로 POST 데이터를 넘겨 주겠다는 옵션
                        connection.setRequestProperty("Content-Type", "application/json");    // application/json 형식으로 전송. (Request body 전달시 application/json 으로 서버에 전달)
                        connection.setRequestProperty("Cache-Control", "no-cache");

                        try {
                            int retCode = connection.getResponseCode();
                            // Log.i(TAG, "▶ [PS] test task response " + retCode);
                            if (retCode == HttpURLConnection.HTTP_OK) {
                                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                                String line = null;
                                String lineTmp = null;
                                while (true) {
                                    lineTmp = reader.readLine();
                                    if (lineTmp != null) {
                                        if (!lineTmp.equals("")) {
                                            line = lineTmp;
                                        }
                                    }
                                    if (lineTmp == null)
                                        break;

                                    final String data = line;

                                    httpHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (data != null) {
                                                if (!data.equals("")) {
                                                    Log.i(TAG, "▶ [PS] test task response data " + data);
                                                    // jsonParsing("test_spec", data);
                                                }
                                            }
                                        }
                                    });
                                }
                                reader.close();
                            }

                            if (connection != null) {
                                // tmrVoltageProcess.schedule(tmrVoltageTimerTask, 0, 300);
                                // Log.i(TAG, "▶ [SI] conn.disconnect().1 >>>>>>>>>>>>>>>>>>>>>>>>>>> " + urlStr);
                                connection.disconnect();
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "▶ [ER].00819 " + e);
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "▶ [ER].00817 " + e);
                        e.printStackTrace();
                    }
                }).start();
            } catch (Exception e) {
                Log.d(TAG, "▶ [ER].00818 " + e);
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
        }
    }

    private void jsonParsing(String data_type, String json) {
        new Thread(() -> {
            try {
                JSONObject jsonObject = new JSONObject(json);
                JSONArray testItemArray = jsonObject.getJSONArray("test_spec");

                List<Map<String, String>> lstData = new ArrayList<Map<String, String>>();
                Map<String, String> mapData = null;

                if (data_type.equals("test_spec")) {
                    for (int i = 0; i < testItemArray.length() - 1; i++) {
                        JSONObject testItemObject = testItemArray.getJSONObject(i);
                        mapData = new HashMap<String, String>();
                        mapData.put("clm_test_seq", String.valueOf(i));
                        mapData.put("clm_lower_value", testItemObject.getString("clm_lower_value"));
                        mapData.put("clm_upper_value", testItemObject.getString("clm_upper_value"));
                        mapData.put("clm_lower_value_watt", testItemObject.getString("clm_lower_value_watt"));
                        mapData.put("clm_upper_value_watt", testItemObject.getString("clm_upper_value_watt"));
                        mapData.put("clm_test_step", testItemObject.getString("clm_test_step"));
                        mapData.put("clm_test_name", testItemObject.getString("clm_test_name"));
                        mapData.put("clm_test_id", testItemObject.getString("clm_test_id"));
                        mapData.put("clm_response_value", testItemObject.getString("clm_response_value"));
                        mapData.put("clm_test_sec", testItemObject.getString("clm_test_sec"));
                        mapData.put("clm_test_type", testItemObject.getString("clm_test_type"));
                        mapData.put("clm_test_command", testItemObject.getString("clm_test_command"));
                        mapData.put("clm_value_watt", testItemObject.getString("clm_value_watt"));
                        mapData.put("clm_value", testItemObject.getString("clm_value"));
                        mapData.put("clm_product_serial_no", testItemObject.getString("clm_product_serial_no"));
                        // mapData.put("clm_value_watt", testItemObject.getString("clm_value_watt"));
                        mapData.put("clm_comment", "");
                        lstData.add(mapData);
                    }

                    totalTimeCnt = 0;
                    arrTestItems = new String[lstData.size()][8];

                    runOnUiThread(new Runnable() {
                        public void run() {
                            TestData.deleteTestSpecData(mainActivity);

                            for (int i = 0; i < lstData.size(); i++) {
                                try {
                                    int iTmp = i;
                                    TestData.insertTestSpecData(mainActivity.getApplicationContext(), lstData.get(iTmp));
                                    // Log.i(TAG, "▶ " + lstData.get(i).get("clm_test_command") + " : " + lstData.get(i));
                                    totalTimeCnt += Integer.parseInt(lstData.get(iTmp).get("clm_test_sec"));
                                    // Log.i(TAG, "▶ [SI] >>>>>>>>> " + iTmp + " " + lstData.get(iTmp));
                                    arrTestItems[iTmp][0] = lstData.get(iTmp).get("clm_test_name");
                                    arrTestItems[iTmp][1] = lstData.get(iTmp).get("clm_test_command");
                                    arrTestItems[iTmp][2] = lstData.get(iTmp).get("clm_test_sec");
                                    arrTestItems[iTmp][3] = lstData.get(iTmp).get(String.valueOf(totalTimeCnt));
                                    arrTestItems[iTmp][4] = lstData.get(iTmp).get("clm_test_type");
                                    arrTestItems[iTmp][5] = lstData.get(iTmp).get("clm_test_command");
                                    arrTestItems[iTmp][6] = lstData.get(iTmp).get("clm_test_step");
                                    arrTestItems[iTmp][7] = lstData.get(iTmp).get("clm_response_value");
//                                    arrTestItems[iTmp][8] = lstData.get(iTmp).get("clm_response_value");
//                                    arrTestItems[iTmp][9] = lstData.get(iTmp).get("clm_response_value");
                                    valueWatt = lstData.get(iTmp).get("clm_value_watt");
                                    lowerValueWatt = lstData.get(iTmp).get("clm_lower_value_watt");
                                    upperValueWatt = lstData.get(iTmp).get("clm_upper_value_watt");
                                    Log.i(TAG, "▶ [SI] scinario setting info[" + iTmp + "] " + lstData.get(iTmp).get("clm_test_command") + " / test_name:" + arrTestItems[iTmp][0] + " / test_type:" + arrTestItems[iTmp][4] + " / value:" + valueWatt + " / lower_value:" + lowerValueWatt + " / upper_value:" + upperValueWatt);
                                    if (lstData.get(iTmp).get("clm_test_command").indexOf("CM0101") > -1) {
//                                runOnUiThread(new Runnable() {
//                                    public void run() {
                                        tvCompValueWatt.setText(valueWatt);
                                        tvCompLowerValueWatt.setText(String.valueOf(Integer.parseInt(valueWatt) - Integer.parseInt(lowerValueWatt)));
                                        tvCompUpperValueWatt.setText(String.valueOf(Integer.parseInt(valueWatt) + Integer.parseInt(upperValueWatt)));
//                                    }
//                                });
                                    }
                                    if (lstData.get(iTmp).get("clm_test_command").indexOf("PM0101") > -1) {
//                                runOnUiThread(new Runnable() {
//                                    public void run() {
                                        tvPumpValueWatt.setText(valueWatt);
                                        tvPumpLowerValueWatt.setText(String.valueOf(Integer.parseInt(valueWatt) - Integer.parseInt(lowerValueWatt)));
                                        tvPumpUpperValueWatt.setText(String.valueOf(Integer.parseInt(valueWatt) + Integer.parseInt(upperValueWatt)));
//                                    }
//                                });
                                    }
                                    if (lstData.get(iTmp).get("clm_test_command").indexOf("HT0101") > -1) {
//                                runOnUiThread(new Runnable() {
//                                    public void run() {
                                        tvCompValueWatt.setText(valueWatt);
                                        tvCompLowerValueWatt.setText(String.valueOf(Integer.parseInt(valueWatt) - Integer.parseInt(lowerValueWatt)));
                                        tvCompUpperValueWatt.setText(String.valueOf(Integer.parseInt(valueWatt) + Integer.parseInt(upperValueWatt)));
                                        // Log.i(TAG, "▶ [D].4 " + lstData.get(i).get("clm_value_watt"));
                                        tvHeaterValueWatt.setText(lstData.get(iTmp).get("clm_value_watt"));
                                        tvHeaterLowerValueWatt.setText(String.valueOf(Integer.parseInt(valueWatt) - Integer.parseInt(lowerValueWatt)));
                                        tvHeaterUpperValueWatt.setText(String.valueOf(Integer.parseInt(valueWatt) + Integer.parseInt(upperValueWatt)));
//                                    }
//                                });
                                    }
                                    productSerialNo = lstData.get(iTmp).get("clm_product_serial_no");
                                    // Log.i(TAG, "▶ init test info array(" + i + ") " + arrTestItems[i][1] + " / " + arrTestItems[i][2] + " / " + arrTestItems[i][3] + " / " + arrTestItems[i][4] + " / " + totalTimeCnt);
                                } catch (Exception e) {
                                    Log.d(TAG, "▶ [ER].00014 : " + e);
                                }
                            }

                            // tvSerialNo.setText(productSerialNo);
                            totalTimeCnt = 0;

                            listItemAdapter = new ItemAdapterTestItem();

                            lstTestResult = new ArrayList<Map<String, String>>();
                            lstTestTemperature = new ArrayList<Map<String, String>>();

                            List<Map<String, String>> lstTestItem = new ArrayList<Map<String, String>>();

                            for (int i = 0; i < arrTestItems.length; i++) {
                                totalTimeCnt += (Integer.valueOf(arrTestItems[i][2]) > 1) ? Integer.valueOf(arrTestItems[i][2]) + 1 : Integer.valueOf(arrTestItems[i][2]);
                                arrTestItems[i][3] = String.valueOf(totalTimeCnt);
                            }

                            Map<String, String> mapListItem = null;
                            for (int i = 0; i < arrTestItems.length; i++) {
                                mapListItem = new HashMap<String, String>();
                                mapListItem.put("test_item_seq", String.valueOf(i + 1));
                                mapListItem.put("test_item_name", arrTestItems[i][0]);
                                mapListItem.put("test_item_command", arrTestItems[i][1]);
                                mapListItem.put("test_response_value", arrTestItems[i][7]);
                                mapListItem.put("test_item_result", getString(R.string.txt_pre_process));
                                mapListItem.put("test_finish_yn", "N");
                                mapListItem.put("test_model_id", globalModelId);
//                                mapListItem.put("test_model_name", arrTestItems[i][8]);
//                                mapListItem.put("test_model_nation", arrTestItems[i][9]);
                                listItemAdapter.addItem(new VoTestItem(mapListItem));
                            }

                            runOnUiThread(new Runnable() {
                                public void run() {
                                    tvTotalTimeCnt.setText(String.valueOf(totalTimeCnt));
                                    lvTestItem.setAdapter(listItemAdapter);
                                    lastTestIdx = listItemAdapter.getCount();
                                    // return null;
                                }
                            });
                        }
                    });
                }
            } catch (JSONException e) {
                Log.d(TAG, "▶ [ER].00015 : " + e);
            }
        }).start();
    }

    public void bluetoothSearch() {
        checkBTPermissions();
        int result = 0;
        List<String> permissionList = new ArrayList<>();

        for (String pm : permissions) {
            result = ContextCompat.checkSelfPermission(this, pm);

            if (result != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(pm);
            }
        }

        if (!permissionList.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionList.toArray(new String[permissionList.size()]), MULTIPLE_PERMISSIONS);
            // return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MULTIPLE_PERMISSIONS: {
                boolean isDeny = false;
                if (grantResults.length > 0) {
                    for (int i = 0; i < permissions.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            isDeny = true;
                        }
                    }
                }

                if (isDeny) {
                    showNoPermissionToastAndFinish();
                } else {
                    tmrBTList = new Timer();
                    btListTimerTask = new TimerTask() {
                        @Override
                        public void run() {
                            Log.i(TAG, "▶ [BT] btListTimerTask ");
                            listPairedDevicesSelect();
                        }
                    };
                    tmrBTList.schedule(btListTimerTask, 0, 2500);
                }
            }
        }
    }

    private void showNoPermissionToastAndFinish() {

        Toast toast = Toast.makeText(this, "권한 요청에 동의 해주셔야 이용 가능합니다. 설정에서 권한 허용 하시기 바랍니다.", Toast.LENGTH_SHORT);
        toast.show();

        finish();
    }

    private void getPreferences() {
        runOnUiThread(new Runnable() {
            public void run() {
                tvSerialNo.setTextColor(postTestInfoColor);
                // tvSerialNo.setText(productSerialNo);
            }
        });
    }

    private void bluetoothOn() {
        if (!mBTAdapter.isEnabled()) { //블루트스 어댑터를 사용가능하게 하기
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);

            mBTAdapter.cancelDiscovery();
        } else {
            Toast.makeText(getApplicationContext(), "Already on", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendBtMessage(String sendMessage) {
        try {



            if (btConnectedThread != null) {
                if(sendMessage.indexOf("CM0200")>-1) {
                    Log.d(TAG, "▶ >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> : " + sendMessage);
                    sendMessage = "CM0100";
                }
                btConnectedThread.write(sendMessage);
            }
            Thread.sleep(100);





        } catch (Exception e) {
            Log.d(TAG, "▶ [ER] 00039 : " + e);

            /*
            try {
                btSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(BT_UUID);
                btSocket.connect();
                mmOutStream = btSocket.getOutputStream();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            */

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
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.
                mBluetoothStatus.setText(getString(R.string.sEnabled));
            } else
                mBluetoothStatus.setText(getString(R.string.sDisabled));
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkBTPermissions() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            if (permissionCheck != 0) {
                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
            } else {
                Log.d("checkPermission", "No need to check permissions. SDK version < LoLLIPOP");
            }
        }
    }

    public void listPairedDevicesSelect() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // listPairedDevices();
                try {
                    mBTArrayAdapter.clear();
                    if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1000);
                    }

                    mPairedDevices = mBTAdapter.getBondedDevices();
                    Log.i(TAG, "▶ [BT] 페어링된 블루투스 장비 " + mBTAdapter.isEnabled() + " " + mPairedDevices.size() + " " + entranceCheck);
                    if (mBTAdapter.isEnabled()) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                // boolean blnConnectedDeviceYn = false;

                                try {
                                    for (BluetoothDevice device : mPairedDevices) {
                                        Method m = device.getClass().getMethod("isConnected", (Class[]) null);
                                        boolean connected = (boolean) m.invoke(device, (Object[]) null);

                                        if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1000);
                                        }

                                        btDeviceName = device.getName();
                                        btDeviceAddr = device.getAddress();

                                        if (connected == true) {
                                            // blnConnectedDeviceYn = true;
                                            Log.i(TAG, "▶ [BT] 연결된 블루투스 장비 " + btDeviceName + " / " + btDeviceAddr);

                                            if (!entranceCheck) {
                                                try {
                                                    deviceSelected = mBTAdapter.getRemoteDevice(btDeviceAddr);
                                                    btSocket = createBluetoothSocket(deviceSelected);
                                                    btSocket.connect();
                                                    isConnected = true;
                                                    connected = true;
                                                } catch (IOException e) {
                                                    // Handle connection failure
                                                    Log.d(TAG, "▶ [ER] 00718 " + e.toString());
                                                    e.printStackTrace();
                                                    isConnected = false;
                                                    onConnectionFailed();

                                                    deviceSelected = mBTAdapter.getRemoteDevice(btDeviceAddr);
                                                    btSocket = createBluetoothSocket(deviceSelected);
                                                    btSocket.connect();
                                                    isConnected = true;
                                                    connected = true;
                                                }
                                            }
                                        } else {
                                            // blnConnectedDeviceYn = false;
                                            Log.i(TAG, "▶ [BT] 연결되지 않은 블루투스 장비 " + btDeviceName + " / " + btDeviceAddr);

                                            try {
                                                deviceSelected = mBTAdapter.getRemoteDevice(btDeviceAddr);
                                                btSocket = createBluetoothSocket(deviceSelected);
                                                btSocket.connect();
                                                isConnected = true;
                                                connected = true;
                                            } catch (IOException e) {
                                                // Handle connection failure
                                                e.printStackTrace();
                                                isConnected = false;
                                                onConnectionFailed();
                                            }

                                            Log.i(TAG, "▶ [BT] 연결되지 않은 블루투스 장비 " + device.getName() + " / " + device.getAddress());
                                        }

                                        if (connected) {
                                            btConnectedThread = new ConnectedThread(btSocket, btHandler);
                                            btConnectedThread.start();

                                            btHandler.obtainMessage(CONNECTING_STATUS, 1, -1, btDeviceName).sendToTarget();
                                            btListTimerTask.cancel();
                                        }
                                    }
                                } catch (Exception e) {
                                    Log.d(TAG, "▶ [ER] 00018 " + e.toString());
                                }
                            }
                        }).start();

                        // Schedule timeout check after CONNECTION_TIMEOUT milliseconds
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (!isConnected) {
                                    // If still not connected after timeout, cancel the connection attempt
                                    cancelConnection();
                                }
                            }
                        }, CONNECTION_TIMEOUT);
                    } else {
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getApplicationContext());
                        alertDialog.setTitle("블루투스");
                        alertDialog.setMessage("태블릿의 블루투스 연결을 활성화 해주십시요.");
                        alertDialog.setNegativeButton("취소",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                    }
                                });
                        alertDialog.show();
                    }

//                            if(!blnConnectedDeviceYn) {}
//                            else {
//                                tmrBTList.cancel();
//                            }
                } catch (Exception e) {
                    Log.d(TAG, "▶ [ER].00126 " + e.toString());
                }
            }
        }).start();
    }

    private void onConnectionSuccess() {
        // Handle the success state, e.g., notify the user, start communication, etc.
        System.out.println("Bluetooth connection successful!");
    }

    private void onConnectionFailed() {
        // Handle connection failure, e.g., retry connection or notify user
        System.out.println("Bluetooth connection failed!");
    }

    private void cancelConnection() {
        if (btSocket != null) {
            try {
                btSocket.close();
                System.out.println("Connection attempt timed out and was canceled.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isConnected(BluetoothDevice device) {
        try {
            Method m = device.getClass().getMethod("isConnected", (Class[]) null);
            boolean connected = (boolean) m.invoke(device, (Object[]) null);
            return connected;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
            return (BluetoothSocket) m.invoke(device, BT_MODULE_UUID);
        } catch (Exception e) {
            Log.e(TAG, "Could not create Insecure RFComm Connection", e);
        }
        if (ActivityCompat.checkSelfPermission(ActivityModel_0003.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1000);
        }
        return device.createRfcommSocketToServiceRecord(BT_MODULE_UUID);
    }


    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e(TAG, "▶ [PS] intent.getAction() " + intent.getAction());
            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    runOnUiThread(new Runnable() {
                        public void run() {
                            usbConnPermissionGranted = true;
                            // tmrUsbTryTimer.cancel();
                            tvConnectPlcRamp.setBackgroundColor(getBaseContext().getResources().getColor(R.color.blue_01));
                            tvConnectPlcRamp.setText("R");
                            // tmrVoltageProcess.schedule(tmrVoltageTimerTask, 0, 1000);
                        }
                    });
                    if (usbService != null) {
                        tmrVoltageProcess.schedule(tmrVoltageTimerTask, 0, 100);
                    }
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    runOnUiThread(new Runnable() {
                        public void run() {
                            tvConnectPlcRamp.setBackgroundColor(getBaseContext().getResources().getColor(R.color.colorAccent));
                        }
                    });
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    runOnUiThread(new Runnable() {
                        public void run() {
                            tvConnectPlcRamp.setBackgroundColor(getBaseContext().getResources().getColor(R.color.green_01));
                        }
                    });
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    runOnUiThread(new Runnable() {
                        public void run() {
                            tvConnectPlcRamp.setBackgroundColor(getBaseContext().getResources().getColor(R.color.red_01));
                        }
                    });
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    runOnUiThread(new Runnable() {
                        public void run() {
                            tvConnectPlcRamp.setBackgroundColor(getBaseContext().getResources().getColor(R.color.orange_01));
                        }
                    });
                    break;
                default:
                    Toast.makeText(context, "Connection state " + intent.getAction(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "▶ [PS] USB Connection state " + intent.getAction());
                    break;
            }
        }
    };

    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbService.UsbBinder) arg1).getService();
            usbService.setHandler(usbHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        try {
            new Thread(() -> {
                setFilters();  // Start listening notifications from UsbService
                startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
            }).start();
        } catch (Exception e) {
            Log.d(TAG, "▶ [ER].00927 " + e.toString());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setFilters() {
        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
            filter.addAction(UsbService.ACTION_NO_USB);
            filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
            filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
            filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
            registerReceiver(usbReceiver, filter);
        } catch (Exception e) {
            Log.d(TAG, "▶ [ER].00030 " + e.toString());
        }
    }

    private class UsbHandler extends Handler {
        // private String dataBuffer = "";
        private final WeakReference<ActivityModel_0003> mActivity;

        public UsbHandler(ActivityModel_0003 activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UsbService.MESSAGE_FROM_SERIAL_PORT:
                    try {
                        String data = (String) msg.obj;
                        try {
                            dataBuffer += data;
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    usbConnPermissionGranted = true;
                                    tvConnectPlcRamp.setBackgroundColor(getBaseContext().getResources().getColor(R.color.blue_01));
                                    tvConnectPlcRamp.setText("R");
                                }
                            });

                            dataBuffer = (dataBuffer.replace("\u0003", "")).replace("\u0006", "");
//
                            if (dataBuffer.indexOf("00RSS0102") > -1 && dataBuffer.length() >= 13) {
                                int s = dataBuffer.indexOf("00RSS0102") + "00RSS0102".length();
                                int e = s + 4;
                                decElectricValue = Integer.parseInt(dataBuffer.substring(s, e), 16);
                                // watt_value = Integer.parseInt(dataBuffer.substring(s, e), 16);
                                valueWattLog = dataBuffer + " / " + s + " / " + e + " / " + dataBuffer.substring(s, e) + " / " + decElectricValue;
                                // Log.i(TAG, "▶ [US] " + valueWattLog);

                                if (!usbReceiverRegisted) {
                                    usbReceiverRegisted = true;
                                }

                                lstWatt.add(decElectricValue);
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        tvConnectPlcRamp.setBackgroundColor(getBaseContext().getResources().getColor(R.color.blue_01));
                                        tvWattValue.setText(String.valueOf((int) decElectricValue));
                                    }
                                });
                                dataBuffer = "";
                            }

                            if (dataBuffer.length() > 60) {
                                dataBuffer = "";
                            }
                        } catch (Exception e) {
                            dataBuffer = "";
                        }
                    } catch (Exception e) {
                        Log.i(TAG, "▶ [ER].00031 " + e.toString());
                    }
                    break;
                case UsbService.CTS_CHANGE:
                    Toast.makeText(mActivity.get(), "CTS_CHANGE", Toast.LENGTH_LONG).show();
                    break;
                case UsbService.DSR_CHANGE:
                    Toast.makeText(mActivity.get(), "DSR_CHANGE", Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

//    private static void setLogText(String tranType, String logText) {
//        if (tranType.equals("B")) {
//            readMessageBTTmp = "\n" + logText + readMessageBTTmp;
//            mainActivity.runOnUiThread(new Runnable() {
//                public void run() {
//                    mReadBuffer.setText(readMessageBTTmp);
//                }
//            });
//            // Log.i(TAG, "[B]" + logText);
//        } else if (tranType.equals("P")) {
//            mainActivity.runOnUiThread(new Runnable() {
//                public void run() {
//                    readMessagePlcTmp = "\n" + logText + readMessagePlcTmp;
//                    mReadText.setText(readMessagePlcTmp);
//                }
//            });
//        }
//    }

    public void readTemperatureExcel(String tableType, String fileName) {
        new Thread(() -> {
            try {
                try {
                    String tableName = (tableType.equals("1")) ? "tbl_cold_temperature" : "tbl_hot_temperature";
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
                                mapTemperature.put("clm_temp_seq", String.valueOf(row));
                                mapTemperature.put("clm_temperature", sheet.getCell(1, row).getContents());
                                mapTemperature.put("clm_regist", sheet.getCell(2, row).getContents());
                                mapTemperature.put("clm_voltage", sheet.getCell(3, row).getContents());
                                mapTemperature.put("clm_10_bit", sheet.getCell(4, row).getContents());
                                mapTemperature.put("clm_12_bit", sheet.getCell(6, row).getContents());
                                mapTemperature.put("clm_comment", "");
                                lstTemperature.add(mapTemperature);
                            }

                            if (lstTemperature.size() != TestData.selectTemperatureData(mainActivity, tableType).size()) {
                                TestData.deleteTemperatureData(mainActivity, tableType);
                                for (int i = 0; i < lstTemperature.size(); i++) {
                                    TestData.insertTemperatureData(mainActivity, tableType, lstTemperature.get(i));
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    Log.d(TAG, "▶ [ER].00026 " + e.toString());
                    e.printStackTrace();
                } catch (BiffException e) {
                    Log.d(TAG, "▶ [ER].00027 " + e.toString());
                    e.printStackTrace();
                }
            } catch (Exception e) {
                Log.d(TAG, "▶ [ER].00045 " + e.toString());
                e.printStackTrace();
            }
        }).start();
    }

    public void callBarcodeInfoServer() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ActivityModel_0003.RequestThreadBarcode thread = new ActivityModel_0003.RequestThreadBarcode(); // Thread 생성
                thread.start(); // Thread 시작
            }
        }).start();
    }

    public class RequestThreadBarcode extends Thread {
        @Override
        public void run() {
            try {
                if (aTryingCnt == 5) {
                    aTryingCnt = 0;
                }
                URL url = new URL(urlStrBarcode);
                // Log.i(TAG, "▶ [SI] urlStrBarcode " + urlStrBarcode);
                connection = (HttpURLConnection) url.openConnection();
                if (connection != null) {
                    connection.setConnectTimeout(3000);
                    connection.setRequestMethod("GET");
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    // Log.i(TAG, "▶ [PS] conn.connect().3 >>>>>>>>>>>>>>>>>>>>>>>>>>> " + urlStrBarcode);

                    int resCode = connection.getResponseCode();
                    Log.i(TAG, "▶ [PS] 제품 시리얼 번호 정보 호출 >>> " + urlStrBarcode + " " + resCode + " " + HttpURLConnection.HTTP_OK + " " + (resCode == HttpURLConnection.HTTP_OK));
                    if (resCode == HttpURLConnection.HTTP_OK) {
                        Log.i(TAG, "▶ [PS] 제품 시리얼 번호 정보 호출 성공 " + resCode + " " + (resCode == HttpURLConnection.HTTP_OK));
                        barcodeReadCheck = true;
                        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        String line = null;
                        String lineTmp = null;
                        while (true) {
                            lineTmp = reader.readLine();
                            if (lineTmp != null) {
                                if (!lineTmp.equals("")) {
                                    line = lineTmp;
                                }
                            }
                            if (lineTmp == null)
                                break;

                            final String data = line;

                            if (data != null) {
                                if (!data.equals("")) {
                                    Log.i(TAG, "▶ [PS] 제품 시리얼 번호 정보 호출 data " + resCode + " " + data);
                                    jsonParsingBarcode("product_serial", data);
                                }
                            }
                        }
                        reader.close();
                    }

                    if (resCode != HttpURLConnection.HTTP_OK) {
                        Log.i(TAG, "▶ [PS] 제품 시리얼 번호 정보 호출 실패 " + resCode + " " + (resCode == HttpURLConnection.HTTP_OK));
                    }

                    if (connection != null) {
                        connection.disconnect();
                    }
                } else {
                    // return null;
                }
            } catch (Exception e) { //예외 처리
                Log.d(TAG, "▶ [ER].00016 " + e);
            }
        }
    }

    public void jsonParsingBarcode(String data_type, String json) {
        new Thread(() -> {
            try {
                JSONObject jsonObject = new JSONObject(json);
                JSONArray testItemArray = jsonObject.getJSONArray("product_serial");

                List<Map<String, String>> lstData = new ArrayList<Map<String, String>>();
                Map<String, String> mapData = null;
                Log.i(TAG, "▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶ [SI] globalProductSerialNo:" + json);

                if (data_type.equals("product_serial")) {
                    Log.i(TAG, "▶ [SI] testItemArray.length():" + testItemArray.length());
                    for (int i = 0; i < testItemArray.length(); i++) {
                        JSONObject testItemObject = testItemArray.getJSONObject(i);
                        mapData = new HashMap<String, String>();
                        mapData.put("clm_product_serial_no", testItemObject.getString("clm_product_serial_no"));
                        lstData.add(mapData);
                    }

                    Log.i(TAG, "▶ [SI] lstData.size():" + lstData.size());
                    for (int i = 0; i < lstData.size(); i++) {
                        try {
                            productSerialNo = lstData.get(i).get("clm_product_serial_no");
                            Log.i(TAG, "▶ [PS] 제품 시리얼 번호 정보 호출[" + i + "] >>> " + productSerialNo);
//                            Log.i(TAG, "▶ productSerialNo(" + i + ") " + productSerialNo);
                        } catch (Exception e) {
                            Log.d(TAG, "▶ [ER].00014 : " + e);
                        }
                    }

                    runOnUiThread(new Runnable() {
                        public void run() {
                            Log.i(TAG, "▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶ [SI] globalProductSerialNo:" + globalProductSerialNo + " productSerialNo:" + productSerialNo);
                            tvSerialNo.setText(productSerialNo);
                            tv_dialog_barcode.setText(productSerialNo);
                            blnBarcodeReceived = true;
                            aTryingCnt = 0;
//                            barcodeRequestTimer.cancel();

                            SharedPreferences test = getSharedPreferences("test_cookie_info", MODE_PRIVATE);
                            globalProductSerialNo = test.getString("test_product_serial_no", "");
                            Log.i(TAG, "▶ [SI] globalProductSerialNo:" + globalProductSerialNo + " productSerialNo:" + productSerialNo);

//                            if(globalProductSerialNo.equals(productSerialNo)) {
//                                tmrBTMessageSend.cancel();
//                                clAlert.setVisibility(View.GONE);
//                                tv_log.setText("");
//                                clAlert.setVisibility(View.VISIBLE);
//                                tvAlertMessage.setText(getString(R.string.txt_duplicated_product_serial_no));
//                                btnAlertClose.setOnClickListener(new Button.OnClickListener() {
//                                    @Override
//                                    public void onClick(View view) {
//                                        finishedCorrectly = true;
//                                        finishApplication(getApplicationContext());
//                                    }
//                                });
//                                return;
//                            }
                        }
                    });

                    test_info.putString("product_serial_no", productSerialNo);
                    test_info.commit();
                    getPreferences();
                }
            } catch (JSONException e) {
                Log.d(TAG, "▶ [ER].00085.1 : " + e);
            }
        }).start();
    }
}

//package itf.com.lms;
//
//import static android.view.View.GONE;
//import static android.view.View.VISIBLE;
//import static itf.com.lms.ActivityModelList.cookie_info;
//import static itf.com.lms.ActivityModelList.cookie_preferences;
//import static itf.com.lms.ActivityModelList.globalLastTestStartTimestamp;
//import static itf.com.lms.ActivityModelList.globalModelId;
//import static itf.com.lms.ActivityModelList.globalModelName;
//import static itf.com.lms.ActivityModelList.globalModelNation;
//import static itf.com.lms.ActivityModelList.globalProductSerialNo;
//
//import android.Manifest;
//import android.annotation.SuppressLint;
//import android.app.ActivityManager;
//import android.app.PendingIntent;
//import android.bluetooth.BluetoothAdapter;
//import android.bluetooth.BluetoothDevice;
//import android.bluetooth.BluetoothSocket;
//import android.content.BroadcastReceiver;
//import android.content.ComponentName;
//import android.content.Context;
//import android.content.DialogInterface;
//import android.content.Intent;
//import android.content.IntentFilter;
//import android.content.ServiceConnection;
//import android.content.SharedPreferences;
//import android.content.pm.ActivityInfo;
//import android.content.pm.PackageManager;
//import android.graphics.Color;
//import android.hardware.usb.UsbDevice;
//import android.hardware.usb.UsbManager;
//import android.net.Uri;
//import android.os.AsyncTask;
//import android.os.Build;
//import android.os.Bundle;
//import android.os.Debug;
//import android.os.Handler;
//import android.os.IBinder;
//import android.os.Looper;
//import android.os.Message;
//import android.provider.Settings;
//import android.text.TextUtils;
//import android.text.method.ScrollingMovementMethod;
//import android.util.Log;
//import android.view.Gravity;
//import android.view.MotionEvent;
//import android.view.View;
//import android.view.ViewGroup;
//import android.view.WindowInsets;
//import android.view.WindowInsetsController;
//import android.view.WindowManager;
//import android.widget.ArrayAdapter;
//import android.widget.Button;
//import android.widget.FrameLayout;
//import android.widget.ListView;
//import android.widget.TableRow;
//import android.widget.TextView;
//
//import androidx.annotation.Dimension;
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AlertDialog;
//import androidx.constraintlayout.widget.ConstraintLayout;
//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//
//import com.google.android.material.floatingactionbutton.FloatingActionButton;
//
//import org.apache.log4j.Logger;
//import org.json.JSONArray;
//import org.json.JSONException;
//import org.json.JSONObject;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.lang.ref.WeakReference;
//import java.lang.reflect.Method;
//import java.net.HttpURLConnection;
//import java.net.Inet4Address;
//import java.net.InetAddress;
//import java.net.NetworkInterface;
//import java.net.SocketException;
//import java.net.URL;
//import java.net.URLEncoder;
//import java.nio.charset.StandardCharsets;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.security.MessageDigest;
//import java.security.NoSuchAlgorithmException;
//import java.text.ParseException;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.Date;
//import java.util.EnumSet;
//import java.util.Enumeration;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Locale;
//import java.util.Map;
//import java.util.Objects;
//import java.util.Set;
//import java.util.Timer;
//import java.util.TimerTask;
//import java.util.UUID;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.ScheduledFuture;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicBoolean;
//
//import itf.com.lms.databinding.ActivityMainBinding;
//import itf.com.lms.item.ItemAdapterTestItem;
//import itf.com.lms.kiosk.BaseKioskActivity;
//import itf.com.lms.util.ConnectedThreadOptimized;
//import itf.com.lms.util.Constants;
//import itf.com.lms.util.TestData;
//import itf.com.lms.util.UsbService;
//import itf.com.lms.vo.VoTestItem;
//import jxl.Sheet;
//import jxl.Workbook;
//import jxl.read.biff.BiffException;
//
//public class ActivityModel_0003 extends BaseKioskActivity {
//
//    // ============================================================================================
//    // Constants (static final)
//    // ============================================================================================
//
//    // String Constants
//    private static final String TAG = ActivityModel_0003.class.getSimpleName();
//    private static final String REGEXP_CHAR_NUM = "^[0-9a-zA-Zㄱ-ㅎ가-힣]*$";
//    private static final String decTemperature = "";
//
//    // UUID Constants
//    private static final UUID BT_MODULE_UUID = UUID.fromString(Constants.InitialValues.BT_MODULE_UUID_SERIAL_PORT_SERVICE);
//
//    // Integer Constants
//    public final static int REQUEST_ENABLE_BT = 1;
//    public final static int MESSAGE_READ = 2;
//    public final static int CONNECTING_STATUS = 3;
//    private static final int PERMISSION_REQUEST_CODE = 1000;
//    private static final int PERMISSION_REQUEST_CODE_BT = 1001;
//    public static final int MULTIPLE_PERMISSIONS = 1801;
//    public static final int WS_PORT = 8080;
//    private static final int USB_POLLING_FAILURE_THRESHOLD = 5;
//    private static final int USB_RETRY_MAX_ATTEMPTS = 10;
//    private static final int BT_RETRY_MAX_ATTEMPTS = 3;
//    private static final int MAX_PERMISSION_DENIAL_COUNT = 3;
//
//    // Long Constants
//    private static final long UI_UPDATE_BATCH_DELAY_MS = 16; // 1 frame (60fps)
//    private static final long USB_POLLING_BACKOFF_MS = Constants.Timeouts.USB_TIMER_INTERVAL_MS * 5;
//    private static final long USB_PERMISSION_RECOVERY_DELAY_MS = 1000L;
//    private static final long BT_RECONNECT_DELAY_MAX_MS = Constants.Timeouts.BT_LIST_TIMER_INTERVAL_MS * 60;
//    private static final long HTTP_DEBOUNCE_DELAY_MS = 100; // 100ms 디바운스
//
//    // Double Constants
//    private static final double decTemperatureValue = 0;
//
//    // Set Constants
//    private static final Set<String> WATT_TRACKING_COMMANDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
//            Constants.TestItemCodes.CM0101,
//            Constants.TestItemCodes.CM0102,
//            Constants.TestItemCodes.HT0101,
//            Constants.TestItemCodes.PM0101,
//            Constants.TestItemCodes.SV0101,
//            Constants.TestItemCodes.SV0201,
//            Constants.TestItemCodes.SV0301,
//            Constants.TestItemCodes.SV0401
//    )));
//    private static final Set<String> SOL_VALVE_COMMANDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
//            Constants.TestItemCodes.SV0101,
//            Constants.TestItemCodes.SV0201,
//            Constants.TestItemCodes.SV0301,
//            Constants.TestItemCodes.SV0401
//    )));
//
//    // String Array Constants
//    private static final String[] MODERN_BT_PERMISSIONS = {
//            Manifest.permission.BLUETOOTH_CONNECT,
//            Manifest.permission.BLUETOOTH_SCAN,
//            Manifest.permission.BLUETOOTH,
//            Manifest.permission.BLUETOOTH_ADMIN,
//            Manifest.permission.ACCESS_COARSE_LOCATION,
//            Manifest.permission.ACCESS_BACKGROUND_LOCATION
//    };
//    private static final String[] LEGACY_BT_PERMISSIONS = {
//            Manifest.permission.BLUETOOTH,
//            Manifest.permission.BLUETOOTH_ADMIN,
//            Manifest.permission.ACCESS_FINE_LOCATION,
//            Manifest.permission.ACCESS_COARSE_LOCATION,
//            Manifest.permission.ACCESS_BACKGROUND_LOCATION
//    };
//    private static final String[] MODERN_MEDIA_PERMISSIONS = {
//            Manifest.permission.READ_MEDIA_IMAGES,
//            Manifest.permission.READ_MEDIA_VIDEO,
//            Manifest.permission.READ_MEDIA_AUDIO
//    };
//    private static final String[] LEGACY_STORAGE_PERMISSIONS = {
//            Manifest.permission.READ_EXTERNAL_STORAGE,
//            Manifest.permission.WRITE_EXTERNAL_STORAGE
//    };
//
//    // Logger
//    private static final Logger log = Logger.getLogger(ActivityModel_0003.class);
//
//    // ============================================================================================
//    // Static Variables
//    // ============================================================================================
//
//    // String Static
//    static String testProcessId = "";
//    static public String serverIp = "";
//    static public String serverDomain = "";
//    static public String serverResetIp = "";
//    static private String btDeviceName = "";
//    static private String btDeviceAddr = "";
//    static String urlStrBarcode = "";
//    static private String resultInfo = Constants.Common.EMPTY_STRING;
//    static String testLogger = Constants.Common.EMPTY_STRING;
//    static String testLoggerTemp = Constants.Common.EMPTY_STRING;
//
//    // Integer Static
//    static int usbConnTryCnt = 0;
//    static int lastTestIdx = 0;
//    static int sensorNgCount = 0;
//    static int postTestInfoColor = 0;
//    static int aTryingCnt = 0;
//    public static int tmrAppResetCnt = 0;
//    public static int tmrAppResetCnt2 = 0;
//    public static int tmrAppResetCnt1 = 0;
//
//    // Boolean Static
//    static boolean usbReceiverRegisted = false;
//    static boolean usbConnPermissionGranted = false;
//    static boolean blnTestYn = false;
//    static boolean blnBarcodeReceived = false;
//    public static boolean btSearchOnOff = false;
//
//    // TextView Static
//    private static TextView mReadBuffer;
//    private static TextView mReadText;
//    private static TextView tvTemperature = null;
//    public static TextView tvAlertMessage = null;
//    private static TextView btnTestHistoryList = null;
//
//    // Array Static
//    static String[][] arrTestItems = null;
//
//    // List/Map Static
//    private static List<Map<String, String>> lstTestTemperature = null;
//    private static Map<String, String> mapTestTemperature = null;
//    public static List<Map<String, String>> btInfoList = null;
//    static SharedPreferences.Editor test_info = null;
//
//    // Adapter Static
//    private static ItemAdapterTestItem listItemAdapter = null;
//
//    // Bluetooth Static
//    static public BluetoothAdapter mBTAdapter;
//    static BluetoothDevice deviceSelected = null;
//
//    // HTTP Static
//    public static HttpURLConnection connection = null;
//
//    // Network Static
//    public static String ipAddress;
//
//    // WeakReference Static
//    private static WeakReference<ActivityModel_0003> mainActivityRef = null;
//
//    // Object Static
//    private static final Object HTTP_HANDLER_LOCK = new Object();
//    private static Runnable pendingHttpTask = null;
//
//    // ============================================================================================
//    // Public Variables
//    // ============================================================================================
//
//    // TextView Public
//    public TextView tvWattValue;
//    public TextView tvSerialNo;
//    public TextView tvCurrentVersion = null;
//
//    // String Public
//    public String modeType = Constants.InitialValues.MODE_TYPE;
//    public String currentPumpWattValueArr = "";
//
//    // Double Public
//    public double currentPumpWattValue = 0;
//    public double currentCompWattValue = 0;
//    public double currentHeaterWattValue = 0;
//    public double currentCompWattValueProp = 0;
//    public double currentHeaterWattValueProp = 0;
//
//    // List Public
//    public List<Map<String, String>> lstTestResult = null;
//    public List<Map<String, String>> lstData = null;
//    public List<Map<String, String>> lstDataTmp = null;
//    public List<Double> lstWatt = new ArrayList<>();
//    public List<String[]> lstMapWattTemp = new ArrayList<>();
//
//    // ============================================================================================
//    // Private Variables - UI Components (View)
//    // ============================================================================================
//
//    // ConstraintLayout
//    private ConstraintLayout clDialogForPreprocess = null;
//    private ConstraintLayout clTestResult;
//    private ConstraintLayout clDialogForLogger;
//    private ConstraintLayout clLog;
//    private ConstraintLayout clAlert = null;
//
//    // TextView
//    private TextView tvDialogMessage = null;
//    private TextView tvUnitId = null;
//    private TextView tvDialogForLoggerWatt;
//    private TextView tvDialogForLoggerTemp;
//    private TextView tvPopupProcessResult;
//    private TextView tvCompWattValue;
//    private TextView tvHeaterWattValue;
//    private TextView tvPumpWattValue;
//    private TextView tvConnectBtRamp;
//    private TextView tvConnectPlcRamp;
//    private TextView tvRunWsRamp;
//    private TextView tvTestOkCnt;
//    private TextView tvTestNgCnt;
//    private TextView tvCompValueWatt;
//    private TextView tvHeaterValueWatt;
//    private TextView tvPumpValueWatt;
//    private TextView tvCompLowerValueWatt;
//    private TextView tvCompUpperValueWatt;
//    private TextView tvPumpLowerValueWatt;
//    private TextView tvPumpUpperValueWatt;
//    private TextView tvHeaterLowerValueWatt;
//    private TextView tvHeaterUpperValueWatt;
//    private TextView tvModelName;
//    private TextView tvModelId;
//    private TextView tvDialogBarcode;
//    private TextView tvModelNationality;
//    private TextView tvTotalTimeCnt;
//    private TextView tvEllapsedTimeCnt;
//    private TextView tvCurrentProcess;
//    private TextView mBluetoothStatus;
//    private TextView tvLog;
//    private TextView btnTestRestart;
//    private TextView tvPopupProcessResultCompValue;
//    private TextView tvPopupProcessResultHeaterValue;
//    private TextView tvResourceInfo;
//
//    // Button
//    private Button btnTestResultClose;
//    private Button btnAlertClose = null;
//
//    // ListView
//    private ListView lvTestItem;
//
//    // TableRow
//    private TableRow trPreprocessContent = null;
//
//    // FloatingActionButton
//    private FloatingActionButton fab_close = null;
//
//    // ViewBinding
//    private ActivityMainBinding binding;
//
//    // WindowManager
//    private WindowManager.LayoutParams params;
//
//    // ============================================================================================
//    // Private Variables - Executors and Handlers
//    // ============================================================================================
//
//    // ExecutorService
//    private final ExecutorService btWorkerExecutor = Executors.newCachedThreadPool(r -> {
//        Thread thread = new Thread(r, "BtWorker");
//        thread.setPriority(Thread.NORM_PRIORITY - 1);
//        return thread;
//    });
//
//    // ScheduledExecutorService
//    private final ScheduledExecutorService usbPollingExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
//        Thread thread = new Thread(r, "UsbPolling");
//        thread.setPriority(Thread.NORM_PRIORITY - 2);
//        thread.setDaemon(true);
//        return thread;
//    });
//    private ScheduledFuture<?> usbPollingFuture = null;
//
//    // Handler
//    private Handler btHandler;
//    private final Handler schedulerHandler = new Handler(Looper.getMainLooper());
//    private final Handler usbRecoveryHandler = new Handler(Looper.getMainLooper());
//    private final Handler usbReconnectHandler = new Handler(Looper.getMainLooper());
//    private final Handler mainHandler = new Handler(Looper.getMainLooper());
//    private Handler hdlTestEntrance = null;
//    private Handler hdlTestTimerTask = null;
//
//    // Message
//    private Message msgTestEntrance = null;
//    private Message msgTestTimerTask = null;
//
//    // Runnable
//    private Runnable uiUpdateBatchTask = null;
//    private Runnable btReconnectRunnable = null;
//    private Runnable usbPermissionRecoveryRunnable = null;
//    private Runnable usbReconnectRunnable = null;
//    private final Runnable resourceInfoRunnable = new Runnable() {
//        @Override
//        public void run() {
//            updateResourceInfo();
//            mainHandler.postDelayed(this, Constants.Timeouts.TIMER_INTERVAL_MS);
//        }
//    };
//
//    // ============================================================================================
//    // Private Variables - Bluetooth
//    // ============================================================================================
//
//    // Bluetooth Components
//    private Set<BluetoothDevice> mPairedDevices;
//    private ArrayAdapter<String> mBTArrayAdapter;
//    private ConnectedThreadOptimized btConnectedThread;
//    private BluetoothSocket btSocket = null;
//
//    // Bluetooth State
//    private boolean btConnected = false;
//    private boolean btConnectionInProgress = false;
//    private long btReconnectDelayMs = Constants.Timeouts.BT_LIST_TIMER_INTERVAL_MS;
//
//    // ============================================================================================
//    // Private Variables - USB
//    // ============================================================================================
//
//    // USB Service
//    private UsbService usbService;
//    private ActivityModel_0003.UsbHandler usbHandler;
//
//    // USB Polling
//    private long usbPollingIntervalMs = Constants.Timeouts.USB_TIMER_INTERVAL_MS;
//    private volatile boolean usbPollingEnabled = false;
//    private int usbPollingFailureCount = 0;
//    private boolean usbPollingRequested = false;
//    private boolean isUsbReconnecting = false;
//    private int usbReconnectAttempts = 0;
//
//    // ============================================================================================
//    // Private Variables - Timer
//    // ============================================================================================
//
//    // Timer
//    private Timer tmrBTMessageSend = null;
//    private Timer tmrReset = null;
//    private Timer tmrFinishedRestart = null;
//    private Timer tmrUnfinishedRestart = null;
//    private Timer tmrRemoteCommand = null;
//    private Timer checkDuration = null;
//    private Timer barcodeRequestTimer = null;
//
//    // TimerTask
//    private TimerTask ttBTMessageSend = null;
//    private TimerTask ttReset = null;
//    private TimerTask ttFinishedRestart = null;
//    private TimerTask ttUnfinishedRestart = null;
//    private TimerTask ttRemoteCommand = null;
//    private TimerTask appResetTimerTask = null;
//
//    // Timer Locks
//    private final AtomicBoolean btMessageTimerRunning = new AtomicBoolean(false);
//    private final Object btMessageTimerLock = new Object();
//    private final AtomicBoolean finishedRestartTimerRunning = new AtomicBoolean(false);
//    private final Object finishedRestartTimerLock = new Object();
//    private final AtomicBoolean unfinishedRestartTimerRunning = new AtomicBoolean(false);
//    private final Object unfinishedRestartTimerLock = new Object();
//    private final AtomicBoolean remoteCommandTimerRunning = new AtomicBoolean(false);
//    private final Object remoteCommandTimerLock = new Object();
//
//    // ============================================================================================
//    // Private Variables - Test Data and State
//    // ============================================================================================
//
//    // Test Counters
//    private int testOkCnt = 0;
//    private int testNgCnt = 0;
//    private int totalTimeCnt = 0;
//    private int testItemIdx = 0;
//    private int testItemCounter = 0;
//    private int testTotalCounter = 0;
//    private int resetCnt = 0;
//    private int restartCntFinished = 0;
//    private int restartCntUnfinished = 0;
//    private int restartCntMargin = 0;
//    private int sendingMessageCnt = 0;
//    private int receivedMessageCnt = 0;
//    private int disconnectCheckCount = 0;
//    private int receiveCommandEmptyCnt = 0;
//    private int iRemoteCommandCnt = 0;
//    private int uvCheckCnt = 0;
//    private int min_diff = 0;
//    private int reload_min_diff = 20;
//    private int wattLower = 0;
//    private int wattUpper = 0;
//    private int temperature12Bit = 0;
//    private int unit_no = 0;
//    private int preprocessColor = 0;
//    private double dblValTemp = 0;
//
//    // Test Items and Commands
//    private String currentTestItem = Constants.InitialValues.CURRENT_TEST_ITEM;
//    private String productSerialNo = "";
//    private String currentTimestamp = "";
//    private String[][] arrTestModels = Constants.TestModels.ARR_TEST_MODELS;
//    private String temperatureTmp = Constants.InitialValues.TEMPERATURE_TMP;
//    private String valueWatt = Constants.InitialValues.VALUE_WATT;
//    private String lowerValueWatt = Constants.InitialValues.LOWER_VALUE_WATT;
//    private String upperValueWatt = Constants.InitialValues.UPPER_VALUE_WATT;
//    private String receiveCommand = "";
//    private String receiveCommandTmp = "";
//    private String decElectricValueList = "";
//    private String receiveCommandResponseOK = "";
//    private String receiveCommandResponse = "";
//    private String receiveCompAgingResponse = Constants.InitialValues.RECEIVE_COMP_AGING_RESPONSE;
//    private String receiveResponseResult = Constants.InitialValues.RECEIVE_RESPONSE_RESULT;
//    private String receiveResponseResultTxt = "";
//    private String iRemoteCommandCallCondition = Constants.InitialValues.REMOTE_COMMAND_CALL_CONDITION;
//    private String urlStr = "";
//    private String urlTestTaskStr = "";
//    private String unit_id = "";
//    private String test_version_id = "";
//    private String model_id = "";
//    private String calculatedTemperatureTmp = Constants.Common.ZERO;
//    private String readMessage = null;
//    private String logText = "";
//    private String logTextParam = "";
//    private String sendResultYn = Constants.InitialValues.SEND_RESULT_YN;
//    private String dataBuffer = "";
//    private String plcCommand = "";
//    private String resultValue = "";
//    private volatile String lastSpecSignature = "";
//    private String decTemperatureColdValue = "";
//    private String decTemperatureHotValue = "";
//    private String decTemperatureValueCompStart = "";
//    private String decTemperatureValueCompEnd = "";
//    private String temperatureValueCompDiff = "";
//    private final String currentPBAVersion = "";
//    private final String datetimeFormat = Constants.DateTimeFormats.DATETIME_FORMAT;
//    private final String timestampFormat = Constants.DateTimeFormats.TIMESTAMP_FORMAT;
//
//    private String lmsTestSeq = getCurrentDatetime(timestampFormat);
//    private String lmsTestTimestamp = getCurrentDatetime(datetimeFormat);
//
//    // Test Data Lists
//    private List<Map<String, String>> coldTemperatureData = null;
//    private List<Map<String, String>> hotTemperatureData = null;
//    private List<Map<String, String>> temperatureData = null;
//    private List<Map<String, String>> lstSpecData = null;
//    private String[] wattTemp = null;
//
//    // Test Values
//    private double decElectricValue = 0;
//    private double decElectricValueForComp = 0;
//    private double decElectricValueForHeater = 0;
//    private double decElectricValueForPump = 0;
//
//    // ============================================================================================
//    // Private Variables - State Flags
//    // ============================================================================================
//
//    // Boolean Flags
//    private boolean entranceCheck = false;
//    private boolean barcodeReadCheck = false;
//    private boolean testProcessStarted = false;
//    private boolean finishedCorrectly = false;
//    private boolean shouldUpdateDialog = false;
//    private boolean compAgingStarted = false;
//    private boolean isHandlingUsbReconnection = false;
//    private boolean isConnected = false;
//    private boolean btPermissionsGranted = false;
//    private boolean permissionRequestInProgress = false;
//    private boolean permissionDialogShowing = false;
//    private boolean processFinished = false;
//    private boolean processFinishedTmp = false;
//
//    // Integer Counters
//    private int permissionDenialCount = 0;
//
//    // ============================================================================================
//    // Private Variables - Cache and Maps
//    // ============================================================================================
//
//    // Map
//    private final Map<String, Map<String, String>> specCache = new ConcurrentHashMap<>();
//    private final Map<String, Double> latestWattByCommand = new ConcurrentHashMap<>();
//
//    // List
//    private final List<Runnable> pendingUiUpdates = new ArrayList<>();
//
//    // Object Locks
//    private final Object uiUpdateLock = new Object();
//
//    // EnumSet
//    private final EnumSet<PermissionAction> pendingPermissionActions = EnumSet.noneOf(PermissionAction.class);
//
//    // ============================================================================================
//    // Private Variables - AsyncTask
//    // ============================================================================================
//
//    ActivityModel_0003.RequestTestTaskThreadAsync testTaskThread = null;
//
//    // ============================================================================================
//    // Private Variables - SharedPreferences
//    // ============================================================================================
//
//    private SharedPreferences preferences;
//
//    // ============================================================================================
//    // Private Enum
//    // ============================================================================================
//
//    private enum PermissionAction {
//        LIST_PAIRED_DEVICES,
//        BT_RECONNECT
//    }
//
//    private void startBtMessageTimer() {
//        // Phase 3: Timer 중복 생성 방지 - synchronized 블록으로 동시성 제어
//        synchronized (btMessageTimerLock) {
//            // 이미 실행 중이면 중복 시작 방지
//            if (btMessageTimerRunning.get()) {
//                logWarn(LogCategory.BT, "BT message timer already running, skipping start");
//                return;
//            }
//
//            // 기존 Timer 정리
//            if (tmrBTMessageSend != null || ttBTMessageSend != null) {
//                stopBtMessageTimer();
//            }
//            try {
//                tmrBTMessageSend = new Timer("BtMsgTimer");
//                ttBTMessageSend = new TimerTask() {
//                    @Override
//                    public void run() {
//                        if (!btConnected || btSocket == null || !btSocket.isConnected()) {
//                            logWarn(LogCategory.BT, "Bluetooth socket not connected; stopping message timer");
//                            stopBtMessageTimer();
//                            scheduleBluetoothReconnect(false);
//                            return;
//                        }
//
//                        if (!entranceCheck) {
//                            if (!barcodeReadCheck) {
//                                runOnBtWorker(() -> callBarcodeInfoServer());
//                            }
//                            sendBtMessage(currentTestItem);
//                        } else {
//                            // ⚠️ 중요: arrTestItems가 초기화되기 전에 접근하는 것을 방지
//                            if (arrTestItems == null || arrTestItems.length == 0) {
//                                logWarn(LogCategory.BT, "arrTestItems not initialized yet, skipping message processing");
//                                return;
//                            }
//                            if (testItemIdx < arrTestItems.length) {
//                                currentTestItem = arrTestItems[testItemIdx][1];
//                                disconnectCheckCount = receivedMessageCnt - sendingMessageCnt;
//                                // logInfo(LogCategory.PS, ">>>>>>>>>>>>>>>>> readMessage " + readMessage);
//                                // if ((disconnectCheckCount > Constants.TestThresholds.DISCONNECT_CHECK_THRESHOLD || disconnectCheckCount < -Constants.TestThresholds.DISCONNECT_CHECK_THRESHOLD) && (readMessage.equals("") || readMessage==null)) {
//                                if (disconnectCheckCount > Constants.TestThresholds.DISCONNECT_CHECK_THRESHOLD || Math.abs(disconnectCheckCount) > Constants.TestThresholds.DISCONNECT_CHECK_THRESHOLD) {
//                                    // if (receiveCommand.equals("")) {
////                                        if(receiveCommandEmptyCnt<=BT_RETRY_MAX_ATTEMPTS) {
////                                            // logInfo(LogCategory.PS, ">>>>>>>>>>>>>>>>> receiveCommandEmptyCnt " + receiveCommandEmptyCnt);
////                                            receiveCommandEmptyCnt++;
////                                        }
////                                        else {
//                                    logWarn(LogCategory.BT, String.format("검사 항목 신호 송신.B [T:%d/%d][C:%d/%s][P:%d/%d][S:%s][G:%d=%d-%d] 수신 없음 %s",
//                                            testTotalCounter, totalTimeCnt, testItemCounter, arrTestItems[testItemIdx][2],
//                                            testItemIdx, arrTestItems.length, currentTestItem, disconnectCheckCount,
//                                            receivedMessageCnt, sendingMessageCnt, receiveCommandEmptyCnt));
//                                    stopBtMessageTimer();
//                                    // Phase 5: Bluetooth Handler 내부 통합 - scheduleUiUpdate 사용
//                                    scheduleUiUpdate(() -> {
//                                        final int finalUsbStatusColor = R.color.red_01;
//                                        final String finalUsbStatusText = "";
//                                        tvConnectBtRamp.setBackgroundColor(getColor(finalUsbStatusColor));
//                                        if (finalUsbStatusText != null) {
//                                            // tvConnectBtRamp.setText(finalUsbStatusText);
//                                        }
//
//                                        clAlert.setVisibility(GONE);
//                                        if (clDialogForPreprocess.getVisibility() == VISIBLE) {
//                                            clDialogForPreprocess.setVisibility(GONE);
//                                            tvDialogMessage.setText("");
//                                        }
//                                        clAlert.setVisibility(VISIBLE);
//                                        tvAlertMessage.setText(getString(R.string.txt_received_no_data_from_product));
//                                        btnAlertClose.setOnClickListener(new Button.OnClickListener() {
//                                            @Override
//                                            public void onClick(View view) {
//                                                clAlert.setVisibility(GONE);
//                                            }
//                                        });
//                                    });
//
//                                    startResetTimer();
//                                    // }
//                                    // }
//                                }
//                                logInfo(LogCategory.BT, String.format(Constants.LogMessages.TEST_ITEM_SIGNAL_SENT + Constants.Common.EMPTY_STRING + "[T:%d/%d][C:%d/%s][P:%d/%d][S:%s][G:%d=%d-%d]",
//                                        testTotalCounter, totalTimeCnt, testItemCounter, arrTestItems[testItemIdx][2],
//                                        testItemIdx, arrTestItems.length, currentTestItem, disconnectCheckCount,
//                                        receivedMessageCnt, sendingMessageCnt));
//
//                                if (Integer.parseInt(arrTestItems[testItemIdx][2]) <= testItemCounter) {
//                                    testItemCounter = 0;
//                                    testItemIdx++;
//                                    // Phase 2: 단순 UI 업데이트를 scheduleUiUpdate로 변경
//                                    scheduleUiUpdate(() -> lvTestItem.smoothScrollToPosition(testItemIdx));
//                                }
//
//                                if (currentTestItem.equals(Constants.TestItemCodes.CM0100)) {
//                                    final String finalReceiveResponseResult = Constants.ResultStatus.OK;
//                                    final String finalReceiveResponseResultTxt = Constants.Common.SUCCESS;
//                                    runOnUiThread(() -> {
//                                        try {
//                                            int CM0101Index = -1;
//                                            for (int i = 0; i < listItemAdapter.getCount(); i++) {
//                                                if (Constants.TestItemCodes.CM0101.equals(((VoTestItem) listItemAdapter.getItem(i)).getTest_item_command())) {
//                                                    CM0101Index = i;
//                                                    break;
//                                                }
//                                            }
//
//                                            if (CM0101Index >= 0 && CM0101Index + 1 < listItemAdapter.getCount()) {
//                                                ((VoTestItem) listItemAdapter.getItem(CM0101Index + 1)).setTest_item_name(Constants.Common.COMP_COOLING_PERFORMANCE + finalReceiveResponseResultTxt);
//                                                ((VoTestItem) listItemAdapter.getItem(CM0101Index + 1)).setTest_item_result(finalReceiveResponseResult);
//                                                ((VoTestItem) listItemAdapter.getItem(CM0101Index + 1)).setTest_result_value(finalReceiveResponseResult + " / 01");
//                                                ((VoTestItem) listItemAdapter.getItem(CM0101Index + 1)).setTest_finish_yn(Constants.ResultStatus.YES);
//                                                listItemAdapter.updateListAdapter();
//                                            } else {
//                                                logWarn(LogCategory.BT, Constants.LogMessages.CM0101_INDEX_NOT_FOUND + CM0101Index);
//                                            }
//                                        } catch (Exception e) {
//                                            logError(LogCategory.ER, Constants.ErrorMessages.ERROR_UPDATING_CM0100_IN_RUN_ON_UI_THREAD, e);
//                                        }
//                                    });
//                                } else if (currentTestItem.equals(Constants.TestItemCodes.RE0101)) {
//                                }
//
//                                sendingMessageCnt++;
//                                int testItemIdxTmp = (testItemIdx != arrTestItems.length) ? testItemIdx : testItemIdx - 1;
//                                if (testItemIdx != arrTestItems.length) {
//                                    sendBtMessage(currentTestItem);
//                                }
//                                logTextParam = "[" + getCurrentDatetime(datetimeFormat) + Constants.Common.LOGGER_DEVIDER_01 + String.format(Constants.InitialValues.NUMBER_FORMMATING_03D, testItemCounter) + Constants.Common.LOGGER_DEVIDER_01 + String.format(Constants.InitialValues.NUMBER_FORMMATING_03D, Integer.parseInt(arrTestItems[testItemIdxTmp][2])) + "] " + currentTestItem + " \t";
//                            }
//                        }
//
//                        // Phase 2: 단순 UI 업데이트를 scheduleUiUpdate로 변경
//                        scheduleUiUpdate(() -> tvEllapsedTimeCnt.setText(String.valueOf(testTotalCounter)));
//
//                        wattTemp = new String[]{getCurrentDatetime(timestampFormat), calculatedTemperatureTmp, String.valueOf(decElectricValue), currentTestItem};
//                        lstMapWattTemp.add(wattTemp);
//
//                        testItemCounter++;
//                        testTotalCounter++;
//                    }
//                };
//                tmrBTMessageSend.schedule(ttBTMessageSend, 0, Constants.Timeouts.TIMER_INTERVAL_MS);
//                btMessageTimerRunning.set(true);
//            } catch (Exception e) {
//                btMessageTimerRunning.set(false);
//                logError(LogCategory.ER, "Error starting BT message timer", e);
//            }
//        }
//    }
//
////    int resetCnt = 0;
////    Timer tmrReset = null;
////    TimerTask ttReset = null;
//
//    private void stopBtMessageTimer() {
//        // Phase 3: Timer 중복 생성 방지 - synchronized 블록으로 동시성 제어
//        synchronized (btMessageTimerLock) {
//            if (btMessageTimerRunning.compareAndSet(true, false)) {
//                if (tmrBTMessageSend != null) {
//                    try {
//                        tmrBTMessageSend.cancel();
//                        tmrBTMessageSend.purge();
//                        tmrBTMessageSend = null;
//                    } catch (Exception e) {
//                        logError(LogCategory.ER, "Error canceling BT message timer", e);
//                    }
//                }
//                if (ttBTMessageSend != null) {
//                    try {
//                        ttBTMessageSend.cancel();
//                        ttBTMessageSend = null;
//                    } catch (Exception e) {
//                        logError(LogCategory.ER, "Error canceling BT message timer task", e);
//                    }
//                }
//            } else {
//                // 이미 정지된 상태이지만 안전을 위해 한 번 더 정리
//                if (tmrBTMessageSend != null) {
//                    try {
//                        tmrBTMessageSend.cancel();
//                        tmrBTMessageSend.purge();
//                        tmrBTMessageSend = null;
//                    } catch (Exception ignored) {
//                    }
//                }
//                if (ttBTMessageSend != null) {
//                    try {
//                        ttBTMessageSend.cancel();
//                        ttBTMessageSend = null;
//                    } catch (Exception ignored) {
//                    }
//                }
//            }
//        }
//    }
//
//
//    // DON'T FORGET to stop the server
//    @Override
//    public void onDestroy() {
//        try {
//            super.onDestroy();
//            logInfo(LogCategory.PS, Constants.LogMessages.ON_DESTROY);
//
//            /*
//            // 모든 Timer 정리 (메모리 누수 방지)
//            cleanupAllTimers();
//
//            // 로그 배치 큐 정리
//            clearLogBatchQueue();
//
//            // WeakReference 정리 (메모리 누수 방지)
//            clearMainActivityReference();
//
//            unregisterReceiver(usbReceiver);
//
//            // finishApplication(getApplicationContext());
//            resetBluetoothSessionKeepUsb();
//            */
//
//            btWorkerExecutor.shutdownNow();
//            usbPollingExecutor.shutdownNow();
//            cleanupAllResources();
//
//            // ⚠️ ViewBinding 정리: 메모리 누수 방지
//            if (binding != null) {
//                binding = null;
//            }
//
//            // ⚠️ UI 업데이트 배치 처리 큐 정리
//            clearUiUpdateBatchQueue();
//
//            resetActivityState();
//
//            // 서버로 결과 전달
//        } catch (Exception e) {
//            logError(LogCategory.ER, Constants.ErrorMessages.ON_DESTROY_ERROR + e, e);
//        }
//    }
//
//    public String getLocalIpAddress() {
//        try {
//            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
//                NetworkInterface intf = en.nextElement();
//                for (Enumeration<InetAddress> enumIpAddr = intf
//                        .getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
//                    InetAddress inetAddress = enumIpAddr.nextElement();
//                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
//                        String ipAddr = inetAddress.getHostAddress();
//                        return ipAddr;
//                    }
//                }
//            }
//        } catch (SocketException e) {
//            logError(LogCategory.ER, Constants.ErrorMessages.GET_LOCAL_IP_ADDRESS_ERROR, e);
//        }
//        return null;
//    }
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        try {
//            // ⚠️ 키오스크 모드: KioskModeApplication이 자동으로 적용함 (onCreate에서 enableFullKioskMode 호출됨)
//
//            logInfo(LogCategory.PS, Constants.LogMessages.ON_CREATE_ENTRANCE_CHECK + entranceCheck + " / currentTestItem:" + currentTestItem);
//
//            // ⚠️ ViewBinding 사용: findViewById 호출 제거 및 성능 최적화
//            binding = ActivityMainBinding.inflate(getLayoutInflater());
//            setContentView(binding.getRoot());
//
//            // onCreate() - 메인 스레드
//            checkAndRequestPermissionsAsync();  // 백그라운드에서 체크, 메인으로 포스팅
////            btPermissionsGranted = hasAllBluetoothPermissions();
////            if (!btPermissionsGranted) {
////                requestRuntimePermissions();
////            }
//
//            // ⚠️ ViewBinding을 통한 View 접근: findViewById 호출 제거
//            btnTestResultClose = binding.btnTestResultClose;
//
//            tvConnectBtRamp = binding.tvConnectBtRamp;
//            tvConnectPlcRamp = binding.tvConnectPlcRamp;
//            tvRunWsRamp = binding.tvRunWsRamp;
//
//            tvTestOkCnt = binding.tvTestOkCnt;
//            tvTestNgCnt = binding.tvTestNgCnt;
//
//            clAlert = binding.clAlert;
//            btnAlertClose = binding.btnAlertClose;
//            clTestResult = binding.clTestResult;
//
//            clDialogForPreprocess = binding.clDialogForPreprocess;
//            tvDialogMessage = binding.tvDialogMessage;
//
//            tvUnitId = binding.tvUnitId;
//
//            tvPopupProcessResult = binding.tvPopupProcessResult;
//            tvPopupProcessResultCompValue = binding.tvPopupProcessResultCompValue;
//            tvPopupProcessResultHeaterValue = binding.tvPopupProcessResultHeaterValue;
//            tvCompWattValue = binding.tvCompWattValue;
//            tvHeaterWattValue = binding.tvHeaterWattValue;
//            tvPumpWattValue = binding.tvPumpWattValue;
//
//            tvCompValueWatt = binding.tvCompValueWatt;
//            tvHeaterValueWatt = binding.tvHeaterValueWatt;
//            tvPumpValueWatt = binding.tvPumpValueWatt;
//            tvCompLowerValueWatt = binding.tvCompLowerValueWatt;
//            tvCompUpperValueWatt = binding.tvCompUpperValueWatt;
//            tvHeaterLowerValueWatt = binding.tvHeaterLowerValueWatt;
//            tvHeaterUpperValueWatt = binding.tvHeaterUpperValueWatt;
//            tvPumpLowerValueWatt = binding.tvPumpLowerValueWatt;
//            tvPumpUpperValueWatt = binding.tvPumpUpperValueWatt;
//
//            tvEllapsedTimeCnt = binding.tvEllapsedTimeCnt;
//            tvModelName = binding.tvModelName;
//            tvModelNationality = binding.tvModelNationality;
//            tvSerialNo = binding.tvSerialNo;
//            tvModelId = binding.tvModelId;
//            tvDialogBarcode = binding.tvDialogBarcode;
//            tvCurrentProcess = binding.tvCurrentProcess;
//            tvTotalTimeCnt = binding.tvTotalTimeCnt;
//            mBluetoothStatus = binding.bluetoothStatus;
//            mReadBuffer = binding.readBuffer;
//            mReadText = binding.receiveText;
//            lvTestItem = binding.lstTestItem;
//            tvTemperature = binding.tvTemperature;
//
//            btnTestRestart = binding.btnTestRestart;
//
//            tvAlertMessage = binding.tvAlertMessage;
//            btnTestHistoryList = binding.btnTestHistoryList;
//
//            tvWattValue = binding.tvWattValue;
//
//            trPreprocessContent = binding.trPreprocessContent;
//
//            // ⚠️ 중복 호출 제거됨 (764-765번 라인)
//
//            tvCurrentVersion = binding.tvCurrentVersion;
//
//            clDialogForLogger = binding.clDialogForLogger;    // 20240522 바코드 인식 표시
//            tvDialogForLoggerWatt = binding.tvDialogForLoggerWatt;    // 20240522 바코드 인식 표시
//            tvDialogForLoggerTemp = binding.tvDialogForLoggerTemp;    // 20240522 바코드 인식 표시
//
//            fab_close = binding.fabClose;
//
//            fab_close.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    finishApplication(getApplicationContext());
//                    // resetBluetoothSessionKeepUsb();
//                    // 서버로 결과 전달
//                }
//            });
//
//            mBTArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
//            mBTAdapter = BluetoothAdapter.getDefaultAdapter(); // get a handle on the bluetooth radio
//
//            restartCntMargin = Constants.Network.RESTART_CNT_MARGIN_MULTIPLIER * Constants.Timeouts.MINUTE_TO_MULTIPLE;
//            // restartCntMargin = 60;
//
//            // String folder_name = Constants.FilePaths.FOLDER_NAME;
//
//            mReadBuffer.setMovementMethod(ScrollingMovementMethod.getInstance());
//
//            // WeakReference로 Activity 참조 저장 (메모리 누수 방지)
//            mainActivityRef = new WeakReference<>(this);
//
//            clLog = binding.clLog;
//            tvLog = binding.tvLog;
//            finishedCorrectly = false;
//            testProcessId = Constants.InitialValues.TEST_PROCESS_ID;
//
//            tvLog.setMovementMethod(ScrollingMovementMethod.getInstance());
//
//            // HTTP 통신 관련 runOnUiThread 최적화: scheduleUiUpdate 사용
//            ActivityModel_0003 activity = getMainActivity();
//            if (activity != null && !activity.isFinishing()) {
//                activity.scheduleUiUpdate(() -> {
//                    if (modeType.equals(Constants.ResultStatus.MODE_TYPE_NORMAL)) {
//                        clLog.setVisibility(GONE);
//                        tvLog.setText(Constants.Common.EMPTY_STRING);
//                    } else if (modeType.equals(Constants.ResultStatus.MODE_TYPE_TEST)) {
//                        // clLog.setVisibility(View.VISIBLE);
//                        clLog.setVisibility(GONE);
//                        tvLog.setText("");
//                    }
//                });
//            }
//
//            binding.getRoot().post(this::startDeferredInitialization);
//        } catch (Exception e) {
//            logError(LogCategory.ER, Constants.ErrorMessages.ON_CREATE_ERROR, e);
//        }
//    }
//
//    // 개선된 권한 처리 방식 (샘플 코드)
//
//    /**
//     * 백그라운드 스레드에서 권한 상태를 확인하고,
//     * 필요한 경우 메인 스레드로 권한 요청을 포스팅
//     */
//    private void checkAndRequestPermissionsAsync() {
//        runOnBtWorker(() -> {
//            // 1. 백그라운드에서 권한 상태 확인 (스레드 안전)
//            List<String> missingPermissions = new ArrayList<>();
//            for (String permission : getRequiredPermissions()) {
//                if (ActivityCompat.checkSelfPermission(this, permission)
//                        != PackageManager.PERMISSION_GRANTED) {
//                    missingPermissions.add(permission);
//                }
//            }
//
//            // 2. 결과를 final 변수로 캡처
//            final List<String> finalMissingPermissions = missingPermissions;
//            final boolean allGranted = finalMissingPermissions.isEmpty();
//
//            // 3. UI 업데이트 및 권한 요청은 메인 스레드로 포스팅
//            runOnUiThread(() -> {
//                if (allGranted) {
//                    permissionDenialCount = 0; // 권한이 이미 허용되었으면 거부 횟수 리셋
//                    btPermissionsGranted = true;
//                    runPendingPermissionActions();
//                } else {
//                    // 권한 요청은 메인 스레드에서만 가능
//                    requestRuntimePermissionsOnMainThread(finalMissingPermissions);
//                }
//            });
//        });
//    }
//
//    /**
//     * 메인 스레드에서 실행되는 권한 요청 메서드
//     */
//    private void requestRuntimePermissionsOnMainThread(List<String> missingPermissions) {
//        if (permissionRequestInProgress) {
//            logInfo(LogCategory.BT, "Permission request already in progress; skipping duplicate request");
//            return;
//        }
//        if (missingPermissions.isEmpty()) {
//            permissionDenialCount = 0; // 권한이 이미 허용되었으면 거부 횟수 리셋
//            btPermissionsGranted = true;
//            runPendingPermissionActions();
//            return;
//        }
//        permissionRequestInProgress = true;
//        ActivityCompat.requestPermissions(
//                this,
//                missingPermissions.toArray(new String[0]),
//                MULTIPLE_PERMISSIONS
//        );
//    }
//
//    private void startDeferredInitialization() {
//        try {
//            usbHandler = new ActivityModel_0003.UsbHandler(this);
//            if (Constants.ResultStatus.MODE_TYPE_TEST.equals(modeType)) {
//                setupResourceInfoOverlay();
//            }
//
//            setDisplayLightValueChange(0.5f);
//
//            ensureAdaptersInitialized();
//
//            runOnBtWorker(this::initializeAppStateAsync);
//
//            wireDeferredListeners();
//
//            initBluetoothHandler();
//            startBluetoothConnectionFlow();
//        } catch (Exception e) {
//            logError(LogCategory.ER, "Deferred initialization failed", e);
//        }
//    }
//
//    private void ensureAdaptersInitialized() {
//        if (listItemAdapter == null) {
//            listItemAdapter = new ItemAdapterTestItem();
//        }
//        if (lstTestResult == null) {
//            lstTestResult = new ArrayList<>();
//        } else {
//            lstTestResult.clear();
//        }
//        if (lstTestTemperature == null) {
//            lstTestTemperature = new ArrayList<>();
//        } else {
//            lstTestTemperature.clear();
//        }
//    }
//
//    private void wireDeferredListeners() {
//        if (btnTestResultClose != null) {
//            btnTestResultClose.setOnClickListener(view -> clTestResult.setVisibility(GONE));
//        }
//
//        if (btnTestRestart != null) {
//            btnTestRestart.setOnClickListener(view -> finishApplication(getApplicationContext()));
//            btnTestRestart.setOnTouchListener(new View.OnTouchListener() {
//                @Override
//                public boolean onTouch(View v, MotionEvent event) {
//                    switch (event.getAction()) {
//                        case MotionEvent.ACTION_DOWN:
//                            resetBluetoothSessionKeepUsb();
//                            return true;
//                        case MotionEvent.ACTION_MOVE:
//                            return true;
//                        case MotionEvent.ACTION_UP:
//                        default:
//                            return false;
//                    }
//                }
//            });
//        }
//
//        if (btnTestHistoryList != null) {
//            btnTestHistoryList.setOnClickListener(view -> {
//            });
//        }
//    }
//
//    /**
//     * Static Bluetooth Handler with WeakReference to prevent memory leak
//     * 메모리 누수 방지를 위한 정적 블루투스 핸들러
//     */
//    private static class BtHandler extends Handler {
//        private final WeakReference<ActivityModel_0003> activityRef;
//
//        public BtHandler(ActivityModel_0003 activity) {
//            super(Looper.getMainLooper());
//            this.activityRef = new WeakReference<>(activity);
//        }
//
//        @Override
//        public void handleMessage(@NonNull Message msg) {
//            ActivityModel_0003 activity = activityRef.get();
//            if (activity == null || activity.isFinishing()) {
//                return; // Activity가 없거나 종료 중이면 처리하지 않음
//            }
//
//            switch (msg.what) {
//                case MESSAGE_READ:
//                    byte[] payload = Arrays.copyOf((byte[]) msg.obj, msg.arg1);
//                    if (activity.clTestResult.getVisibility() == VISIBLE) {
//                        activity.clTestResult.setVisibility(GONE);
//                    }
//                    activity.runOnBtWorker(() -> activity.processBtMessage(payload));
//                    break;
//                case CONNECTING_STATUS:
//                    if (msg.arg1 == 1) {
//                        activity.logInfo(LogCategory.BT, Constants.LogMessages.BLUETOOTH_DEVICE_CONNECTION_SUCCESS);
//                        activity.scheduleUiUpdate(() -> {
//                            try {
//                                String[] btDeviceNameInfo = activity.btDeviceName.split(Constants.Common.UNDER_SCORE);
//                                activity.tvConnectBtRamp.setBackgroundColor(activity.getBaseContext().getResources().getColor(R.color.blue_01));
//                                // tvConnectBtRamp.setText(...) 유지 필요 시 추가
//                            } catch (Exception e) {
//                                activity.logError(LogCategory.ER, Constants.ErrorMessages.BLUETOOTH_DEVICE_NAME_PARSING_ERROR, e);
//                            }
//                        });
//                        activity.stopBtMessageTimer();
//                        activity.startBtMessageTimer();
//                    } else {
//                        activity.logWarn(LogCategory.BT, Constants.LogMessages.BLUETOOTH_DEVICE_CONNECTION_FAILED);
//                    }
//                    break;
//                default:
//                    break;
//            }
//        }
//    }
//
//    private void initBluetoothHandler() {
//        // ⚠️ 메모리 누수 방지: 정적 내부 클래스 + WeakReference 사용
//        btHandler = new BtHandler(this);
//        /*
//        btHandler = new Handler(Looper.getMainLooper()) {
//            @Override
//            public void handleMessage(@NonNull Message msg) {
//                switch (msg.what) {
//                    case MESSAGE_READ:
//                        byte[] payload = Arrays.copyOf((byte[]) msg.obj, msg.arg1);
//                        if (clTestResult.getVisibility() == VISIBLE) {
//                            clTestResult.setVisibility(GONE);
//                        }
//                        runOnBtWorker(() -> processBtMessage(payload));
//                        break;
//                    case CONNECTING_STATUS:
//                        if (msg.arg1 == 1) {
//                            logInfo(LogCategory.BT, Constants.LogMessages.BLUETOOTH_DEVICE_CONNECTION_SUCCESS);
//                            scheduleUiUpdate(() -> {
//                                try {
//                                    String[] btDeviceNameInfo = btDeviceName.split(Constants.Common.UNDER_SCORE);
//                                    tvConnectBtRamp.setBackgroundColor(getBaseContext().getResources().getColor(R.color.blue_01));
//                                    // tvConnectBtRamp.setText(...) 유지 필요 시 추가
//                                } catch (Exception e) {
//                                    logError(LogCategory.ER, Constants.ErrorMessages.BLUETOOTH_DEVICE_NAME_PARSING_ERROR, e);
//                                }
//                            });
//                            stopBtMessageTimer();
//                            startBtMessageTimer();
//                        } else {
//                            logWarn(LogCategory.BT, Constants.LogMessages.BLUETOOTH_DEVICE_CONNECTION_FAILED);
//                        }
//                        break;
//                    default:
//                        break;
//                }
//            }
//        };
//        */
//    }
//
//    private void startBluetoothConnectionFlow() {
//        btSearchOnOff = true;
//        if (!btSearchOnOff) {
//            logInfo(LogCategory.BT, Constants.LogMessages.SERVER_NOT_CONNECTED_BLUETOOTH_CONNECTION_ATTEMPT);
//            return;
//        }
//        logInfo(LogCategory.BT, Constants.LogMessages.SERVER_CONNECTED_BLUETOOTH_CONNECTION_ATTEMPT);
//        new Thread(this::bluetoothSearch).start();
//    }
//
//    /**
//     * Bluetooth MESSAGE_READ 처리를 백그라운드에서 수행하고,
//     * UI 변경은 scheduleUiUpdate()를 통해 전달하는 통합 메서드
//     */
//    private void processBtMessage(byte[] raw) {
//
//        // if (msg.what == MESSAGE_READ) {
//        try {
//            if (!btConnected) {
//                btConnected = true;
//                clearBluetoothReconnect();
//            }
//            readMessage = new String(raw, StandardCharsets.UTF_8);
//            // Log.i(TAG, "▶ [BT] entranceCheck >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> " + entranceCheck + " " + currentTestItem + " " + readMessage);
//
//            // ⚠️ 중요: arrTestItems와 listItemAdapter가 초기화되기 전에 블루투스 메시지가 도착할 수 있으므로 null 체크 추가
//            if (arrTestItems == null || listItemAdapter == null) {
//                logWarn(LogCategory.BT, "arrTestItems or listItemAdapter not initialized yet, skipping message processing");
//                return;
//            }
//
//            if (testItemIdx > arrTestItems.length - 1) {
//                testItemIdx = arrTestItems.length - 1;
//            }
//
//            // logWarn(LogCategory.BT, "readMessage : " + readMessage.length() + ", Message: " + readMessage);
//
//            if (readMessage.contains(Constants.TestItemCodes.ST0101)) {
//                if (!canEnterTest()) {
//                    logWarn(LogCategory.PS, "Test entry blocked: USB or Bluetooth not ready");
//                    scheduleUsbReconnect(true);
//                    scheduleBluetoothReconnect(true);
//                    scheduleUiUpdate(() -> {
//                        clAlert.setVisibility(VISIBLE);
//                        tvAlertMessage.setText("블루투스 또는 USB 연결을 확인해주세요.");
//                    });
//                    return;
//                } else {
//                    if (Math.abs(disconnectCheckCount) > 0) {
//                        receivedMessageCnt = 0;
//                        sendingMessageCnt = 0;
//                        disconnectCheckCount = 0;
//                    }
//                }
//                testItemIdx = 0;
//                // ⚠️ 중요: listItemAdapter null 체크 추가
//                if (listItemAdapter != null && listItemAdapter.getCount() > testItemIdx) {
//                    ((VoTestItem) listItemAdapter.getItem(testItemIdx)).setTest_item_result(Constants.ResultStatus.OK);
//                    ((VoTestItem) listItemAdapter.getItem(testItemIdx)).setTest_result_value(Constants.ResultStatus.COMP_AGING_RESPONSE_01 + Constants.Common.LOGGER_DEVIDER_01 + Constants.ResultStatus.COMP_AGING_RESPONSE_01);
//                    ((VoTestItem) listItemAdapter.getItem(testItemIdx)).setTest_finish_yn(Constants.ResultStatus.YES);
//                } else {
//                    logWarn(LogCategory.BT, "listItemAdapter not ready, cannot update test item result");
//                }
//                if (testProcessId.equals(Constants.Common.EMPTY_STRING)) {
//                    testProcessId = getCurrentDatetime(timestampFormat);
//                    logInfo(LogCategory.PS, Constants.LogMessages.TEST_PROCESS_ID_STARTED + testProcessId);
////                                    new Thread(() -> {
////                                        callBarcodeInfoServer();
////                                    }).start();
//                }
//                entranceCheck = true;
//                // Phase 3: Timer 중복 생성 방지 - synchronized 블록으로 동시성 제어
//                // Timer가 이미 실행 중이면 Timer 시작만 건너뛰고, 나머지 로직은 계속 실행
//                boolean timerAlreadyRunning = false;
//                synchronized (unfinishedRestartTimerLock) {
//                    // 이미 실행 중이면 중복 시작 방지 (하지만 검사는 계속 진행)
//                    if (unfinishedRestartTimerRunning.get()) {
//                        logWarn(LogCategory.BT, "Unfinished restart timer already running, skipping start");
//                        timerAlreadyRunning = true;
//                    } else {
//                        // 기존 Timer 인스턴스가 있으면 먼저 정리 (안전성 향상)
//                        if (tmrUnfinishedRestart != null) {
//                            tmrUnfinishedRestart.cancel();
//                            tmrUnfinishedRestart.purge();
//                            tmrUnfinishedRestart = null;
//                        }
//                        if (ttUnfinishedRestart != null) {
//                            ttUnfinishedRestart.cancel();
//                            ttUnfinishedRestart = null;
//                        }
//
//                        try {
//                            tmrUnfinishedRestart = new Timer();
//                            ttUnfinishedRestart = new TimerTask() {
//                                @Override
//                                public void run() {
//                                    if (restartCntUnfinished == totalTimeCnt + restartCntMargin) {
//                                        logWarn(LogCategory.BT, String.format(Constants.LogMessages.UNFINISHED_RESTART_APP_TERMINATION, restartCntUnfinished, (totalTimeCnt + restartCntMargin)));
//                                        // restartCntUnfinished = 0;
//                                        // restartApplication(getApplicationContext());
//                                        // finishApplication(getApplicationContext());
//                                        resetBluetoothSessionKeepUsb();
//                                        // 서버로 결과 전달
//                                        // Timer를 cancel할 때 unfinishedRestartTimerRunning도 false로 리셋
//                                        synchronized (unfinishedRestartTimerLock) {
//                                            if (tmrUnfinishedRestart != null) {
//                                                tmrUnfinishedRestart.cancel();
//                                                tmrUnfinishedRestart.purge();
//                                                tmrUnfinishedRestart = null;
//                                            }
//                                            unfinishedRestartTimerRunning.set(false);
//                                        }
//                                    }
//                                    restartCntUnfinished++;
//                                }
//                            };
//                            unfinishedRestartTimerRunning.set(true);
//                            tmrUnfinishedRestart.schedule(ttUnfinishedRestart, 0, Constants.Timeouts.TIMER_INTERVAL_MS);
//                        } catch (Exception e) {
//                            unfinishedRestartTimerRunning.set(false);
//                            logError(LogCategory.ER, "Error starting unfinished restart timer", e);
//                        }
//                    }
//                }
//                // callBarcodeInfoServer();
//                testItemIdx = 1;
//
//                try {
//                    // runOnBtWorker(() -> {
//                    logInfo(LogCategory.US, Constants.LogMessages.USB_SERVICE_STARTED);
//                    setFilters();  // Start listening notifications from UsbService
//                    startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
//                    // });
//                } catch (Exception e) {
//                    logError(LogCategory.ER, Constants.ErrorMessages.USB_SERVICE_START_ERROR, e);
//                }
//
//                // 복잡한 UI 업데이트 블록 통합: scheduleUiUpdate 사용
//                scheduleUiUpdate(() -> {
//                    clTestResult.setVisibility(GONE);
//                    setDisplayLightValueChange(Constants.UI.DISPLAY_BRIGHTNESS);
//                });
//                // tmrBTMessageSend.cancel();
//            } else if (readMessage.contains(Constants.TestItemCodes.RE0101)) {
//                stopBtMessageTimer();
//            }
//
//            if (entranceCheck) {
//                // readMessage 유효성 검사
//                if (readMessage == null || readMessage.isEmpty()) {
//                    logWarn(LogCategory.BT, "readMessage is null or empty, skipping message processing");
//                    return;
//                }
//
//                // STX 문자 위치 확인
//                int stxIndex = readMessage.indexOf(Constants.CharCodes.STX);
//                if (stxIndex < 0) {
//                    logWarn(LogCategory.BT, "STX character not found in readMessage: " + readMessage);
//                    return;
//                }
//
//                // readMessage 길이 검증 (최소 15자 필요: STX 위치 + 1부터 7자, 그리고 13-15 위치)
//                int minRequiredLength = Math.max(stxIndex + 7, 15);
//                if (readMessage.length() < minRequiredLength) {
//                    logWarn(LogCategory.BT, "readMessage too short. Length: " + readMessage.length() + ", Required: " + minRequiredLength + ", Message: " + readMessage);
//                    return;
//                }
//
//                                /*
//                                logWarn(LogCategory.BT, "readMessage.substring(stxIndex + 1, stxIndex + 7) : " + readMessage.substring(stxIndex + 1, stxIndex + 7));
//                                logWarn(LogCategory.BT, "readMessage.substring(stxIndex + 7, 13) : " + readMessage.substring(stxIndex + 7, 13));
//                                logWarn(LogCategory.BT, "readMessage.substring(13, 15) : " + readMessage.substring(13, 15));
//                                logWarn(LogCategory.BT, "lstSpecData.get(0) : " + lstSpecData.get(0));
//                                */
//
//                try {
//                    receiveCommand = "";
//                    receiveCommand = readMessage.substring(stxIndex + 1, stxIndex + 7);
//                    receiveCommandResponse = readMessage.substring(13, 15);
//                    // logInfo(LogCategory.PS, ">>>>>> 1.receiveCommand " + receiveCommand);
//                } catch (StringIndexOutOfBoundsException e) {
//                    logError(LogCategory.ER, "StringIndexOutOfBoundsException while parsing readMessage: " + readMessage + ", STX index: " + stxIndex, e);
//                    return;
//                }
//
//                // Log.i(TAG, "▶▶▶▶▶▶▶▶▶▶ [" + testItemCounter + Constants.Common.LOGGER_DEVIDER_01 + arrTestItems[testItemIdx][2] + "][" + currentTestItem + Constants.Common.LOGGER_DEVIDER_01 + receiveCommand + "]");
//
//                // receiveCommand 유효성 검사
//                if (receiveCommand == null || receiveCommand.isEmpty()) {
//                    logWarn(LogCategory.BT, "receiveCommand is null or empty, skipping database query");
//                    return;
//                }
//
//                // 데이터베이스 조회 등 무거운 작업을 백그라운드로 이동하여 메인 스레드 블로킹 방지
//                if (receiveCommand.equals("CM0102") || receiveCommand.equals("CM0103")) {
//                    receiveCommandTmp = receiveCommand;
//                    receiveCommand = Constants.TestItemCodes.CM0101;
//                }
//                final String finalReceiveCommand = receiveCommand;
//                final String finalReceiveCommandTmp = receiveCommandTmp;
//                final String finalReceiveCommandResponse = receiveCommandResponse; // 초기값을 final 변수로 캡처
//                final String finalReadMessage = readMessage; // readMessage를 final 변수로 캡처
//                final String finalCurrentTestItem = currentTestItem;
//
//                // runOnBtWorker(() -> {
//                try {
//                    List<Map<String, String>> specDataResult = getSpecData(finalReceiveCommand);
//                    final List<Map<String, String>> finalSpecData = specDataResult;
//
//                    // 복잡한 UI 업데이트 블록 통합: scheduleUiUpdate 사용
//                    scheduleUiUpdate(() -> {
//                        try {
//                            if (finalSpecData != null && !finalSpecData.isEmpty()) {
//                                lstSpecData = finalSpecData;
//                            } else {
//                                lstSpecData = null;
//                            }
//
//                            if (lstSpecData == null || lstSpecData.isEmpty()) {
//                                logWarn(LogCategory.ER, Constants.LogMessages.LST_SPEC_DATA_NULL_OR_EMPTY + finalReceiveCommand + Constants.LogMessages.DB_AND_MEMORY_CACHE_BOTH_CHECKED);
//                                return;
//                            }
//
//                            receivedMessageCnt++;
//                            String currentProcessName = ((VoTestItem) listItemAdapter.getItem(testItemIdx)).getTest_item_name();
//                            applyUiBundle(new UiUpdateBundle.Builder().setCurrentProcessName(currentProcessName).setReceivedMessageCnt(receivedMessageCnt).build());
//                        } catch (Exception e) {
//                            logError(LogCategory.ER, Constants.ErrorMessages.ERROR_IN_INITIAL_UI_UPDATE, e);
//                            sendBtMessage(finalCurrentTestItem);
//                        }
//                    });
//
//                    // ANR 방지: 복잡한 데이터 처리와 계산은 백그라운드 스레드에서 수행
//                    // runOnBtWorker(() -> {
//                    try {
//                        // lstSpecData가 null이면 처리 중단
//                        if (lstSpecData == null || lstSpecData.size() == 0) {
//                            return;
//                        }
//
//                        // 복잡한 조건문 처리 시작
//                        // 데이터 처리 결과를 저장할 변수들
//                        String calculatedReceiveResponseResult = Constants.Common.EMPTY_STRING;
//                        String calculatedResultValue = Constants.Common.EMPTY_STRING;
//                        String calculatedDecTemperature = Constants.Common.EMPTY_STRING;
//                        int calculatedDecTemperatureValue = 0;
//                        // String calculatedTemperatureTmp = Constants.Common.EMPTY_STRING;
//                        String calculatedReceiveCommandResponse = Constants.Common.EMPTY_STRING;
//                        double calculatedDblValTemp = 0.0;
//                        int calculatedWattLower = 0;
//                        int calculatedWattUpper = 0;
//                        String calculatedReceiveCompAgingResponse = Constants.Common.EMPTY_STRING;
//                        String calculatedReceiveResponseResultTxt = Constants.Common.EMPTY_STRING;
//                        String calculatedResultInfo = Constants.Common.EMPTY_STRING;
//                        boolean shouldUpdateDialog = this.shouldUpdateDialog;
//                        int dialogColor = 0;
//                        String dialogMessage = "";
//                        boolean shouldHideDialog = false;
//                        String calculatedTemperatureValueCompDiff = "";
//
//                        // ANR 방지: UI 업데이트를 배치 처리하기 위한 변수들
//                        String updateTemperature = null;        // 온도 업데이트 값
//                        String updateCompWatt = null;          // 컴프레서 와트 업데이트 값
//                        String updateHeaterWatt = null;        // 히터 와트 업데이트 값
//                        String updatePumpWatt = null;          // 펌프 와트 업데이트 값
//                        String updateLogText = null;           // 로그 텍스트 업데이트 값
//                        boolean shouldUpdateListAdapter = false; // 리스트 어댑터 업데이트 필요 여부
//                        String updateItemCommand = "";         // 업데이트할 아이템 명령어
//                        String updateItemResult = "";           // 업데이트할 아이템 결과
//                        String updateItemCheckValue = "";      // 업데이트할 아이템 체크 값
//                        String updateItemInfo = "";            // 업데이트할 아이템 정보
//                        String updateItemNameSuffix = "";      // 업데이트할 아이템 이름 접미사
//
//                        scheduleUiUpdate(() -> {
//                            clTestResult.setVisibility(GONE);
//
//                            tvPopupProcessResult.setText("");
//                            tvTestOkCnt.setText("");
//                            tvTestNgCnt.setText("");
//                        });
//
//                        if (finalReceiveCommand.contains(Constants.TestItemCodes.SN0101) ||
//                                finalReceiveCommand.contains(Constants.TestItemCodes.SN0201) ||
//                                finalReceiveCommand.contains(Constants.TestItemCodes.SN0301) ||
//                                finalReceiveCommand.contains(Constants.TestItemCodes.TA0101) ||
//                                finalReceiveCommand.contains(Constants.TestItemCodes.TA0201) ||
//                                finalReceiveCommand.contains(Constants.TestItemCodes.TA0301) ||
//                                finalReceiveCommand.contains(Constants.TestItemCodes.FM0101)) {
//                            // UI 업데이트는 작은 runOnUiThread 블록으로 분리
//                            shouldUpdateDialog = true;
//                            clDialogForPreprocess.setVisibility(View.VISIBLE);
//                            if (finalReceiveCommand.contains(Constants.TestItemCodes.SN0101)) {
//                                dialogColor = getBaseContext().getResources().getColor(R.color.pink_01);
//                                dialogMessage = lstSpecData.get(0).get(Constants.JsonKeys.CLM_TEST_NAME);
//                            } else if (finalReceiveCommand.contains(Constants.TestItemCodes.SN0201)) {
//                                dialogColor = getBaseContext().getResources().getColor(R.color.yellow_01);
//                                dialogMessage = lstSpecData.get(0).get(Constants.JsonKeys.CLM_TEST_NAME);
//                            } else if (finalReceiveCommand.contains(Constants.TestItemCodes.SN0301)) {
//                                dialogColor = getBaseContext().getResources().getColor(R.color.green_02);
//                                dialogMessage = lstSpecData.get(0).get(Constants.JsonKeys.CLM_TEST_NAME);
//                            } else if (finalReceiveCommand.contains(Constants.TestItemCodes.TA0101)) {
//                                dialogColor = getBaseContext().getResources().getColor(R.color.blue_01);
//                                dialogMessage = lstSpecData.get(0).get(Constants.JsonKeys.CLM_TEST_NAME);
//                            } else if (finalReceiveCommand.contains(Constants.TestItemCodes.TA0201)) {
//                                dialogColor = getBaseContext().getResources().getColor(R.color.yellow_01);
//                                dialogMessage = lstSpecData.get(0).get(Constants.JsonKeys.CLM_TEST_NAME);
//                            } else if (finalReceiveCommand.contains(Constants.TestItemCodes.TA0301)) {
//                                dialogColor = getBaseContext().getResources().getColor(R.color.yellow_01);
//                                dialogMessage = lstSpecData.get(0).get(Constants.JsonKeys.CLM_TEST_NAME);
//                            } else if (finalReceiveCommand.contains(Constants.TestItemCodes.FM0101)) {
//                                dialogColor = getBaseContext().getResources().getColor(R.color.pink_01);
//                                dialogMessage = lstSpecData.get(0).get(Constants.JsonKeys.CLM_TEST_NAME);
//                            } else {
//                                shouldHideDialog = true;
//                            }
//
//
//                            // UI 업데이트는 나중에 배치 처리 (값만 저장)
//                            if (testItemCounter > Constants.TestThresholds.TEST_COUNTER_THRESHOLD_5) {
//                                if (finalReceiveCommandResponse.equals(lstSpecData.get(0).get(Constants.JsonKeys.CLM_RESPONSE_VALUE))) {
//                                    receiveCommandResponseOK = finalReceiveCommand;
//                                    calculatedReceiveResponseResult = Constants.ResultStatus.OK;
//                                } else {
//                                    calculatedReceiveResponseResult = Constants.ResultStatus.NG;
//                                }
//
//                                if (calculatedReceiveResponseResult.equals(Constants.ResultStatus.OK)) {
//                                    testItemCounter = Integer.parseInt(arrTestItems[testItemIdx][2]);
//                                }
//                                calculatedResultValue = finalReceiveCommandResponse;
//                            }
//
//                            // logInfo(LogCategory.PS, ">>>>> lstSpecData.get(0) " + lstSpecData.get(0));
//                            logInfo(LogCategory.BT, String.format(Constants.LogMessages.TEST_RESPONSE_SIGNAL_RECEIVED + " [T:%d/%d][C:%d/%s][P:%d/%d][S:%s][G:%d=%d-%d][R:%s] %s / %s / %s ",
//                                    testTotalCounter, totalTimeCnt, testItemCounter, arrTestItems[testItemIdx][2],
//                                    testItemIdx, arrTestItems.length, currentTestItem, disconnectCheckCount,
//                                    receivedMessageCnt, sendingMessageCnt, readMessage,
//                                    finalReceiveCommandResponse, lstSpecData.get(0).get(Constants.JsonKeys.CLM_RESPONSE_VALUE), calculatedReceiveResponseResult));
//                        } else {
//                            // UI 업데이트는 나중에 배치 처리 (값만 저장)
//                            shouldHideDialog = true;
//
//                            // 온도 처리 (백그라운드에서 계산)
//                            if (finalReceiveCommand.contains(Constants.TestItemCodes.TH0101) || finalReceiveCommand.contains(Constants.TestItemCodes.TH0201)) {
//                                try {
//                                    calculatedReceiveCommandResponse = finalReadMessage.substring(9, 15);
//                                    calculatedDecTemperatureValue = Integer.parseInt(calculatedReceiveCommandResponse, 16);
//
//                                    temperatureData = (finalReceiveCommand.equals(Constants.TestItemCodes.TH0101)) ? coldTemperatureData : hotTemperatureData;
//
//                                    for (int i = 0; i < temperatureData.size(); i++) {
//                                        temperature12Bit = (int) Math.round(Double.valueOf(temperatureData.get(i).get(Constants.JsonKeys.CLM_12_BIT)));
//                                        if (temperature12Bit > calculatedDecTemperatureValue) {
//                                            calculatedDecTemperature = temperatureTmp;
//                                            // UI 업데이트는 나중에 배치 처리 (값만 저장)
//                                            updateTemperature = calculatedDecTemperature;
//
//                                            mapTestTemperature = new HashMap<>();
//                                            mapTestTemperature.put(Constants.JsonKeys.CLM_LINE, Constants.Database.LINE_ID);
//                                            mapTestTemperature.put(Constants.JsonKeys.CLM_UNIT_ID, Constants.Database.UNIT_ID);
//                                            mapTestTemperature.put(Constants.JsonKeys.CLM_TIMESTAMP, currentTimestamp);
//                                            mapTestTemperature.put(Constants.JsonKeys.CLM_PRODUCT_TEMPERATURE, calculatedDecTemperature);
//                                            lstTestTemperature.add(mapTestTemperature);
//                                            break;
//                                        }
//                                        calculatedTemperatureTmp = temperatureData.get(i).get(Constants.JsonKeys.CLM_TEMPERATURE);
//                                    }
//
//                                    if (finalReceiveCommand.equals(Constants.TestItemCodes.TH0101)) {
//                                        decTemperatureHotValue = calculatedTemperatureTmp + Constants.Common.DEGREE_CELSIUS;
//                                    } else {
//                                        decTemperatureColdValue = calculatedTemperatureTmp + Constants.Common.DEGREE_CELSIUS;
//                                    }
//
//                                    calculatedReceiveCommandResponse = calculatedReceiveCommandResponse + Constants.Common.COMMA + calculatedTemperatureTmp;
//                                    // logInfo(LogCategory.TH, Constants.LogMessages.MEASURED_TEMPERATURE + calculatedDecTemperatureValue + Constants.Common.LOGGER_DEVIDER_01 + calculatedTemperatureTmp);
//
//                                    // logInfo(LogCategory.TH, "> lstSpecData.get(0):" + lstSpecData.get(0));
//                                    // logInfo(LogCategory.TH, "> CLM_VALUE:" + lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE) + " CLM_LOWER_VALUE:" + lstSpecData.get(0).get(Constants.JsonKeys.CLM_LOWER_VALUE) + " CLM_UPPER_VALUE:" + lstSpecData.get(0).get(Constants.JsonKeys.CLM_UPPER_VALUE));
//
//                                    if ((!lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE).equals("") && !lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE).isEmpty()) &&
//                                            (!lstSpecData.get(0).get(Constants.JsonKeys.CLM_LOWER_VALUE).equals("") && !lstSpecData.get(0).get(Constants.JsonKeys.CLM_LOWER_VALUE).isEmpty()) &&
//                                            (!lstSpecData.get(0).get(Constants.JsonKeys.CLM_UPPER_VALUE).equals("") && !lstSpecData.get(0).get(Constants.JsonKeys.CLM_UPPER_VALUE).isEmpty())) {
//                                        // 온도 검사 기준값이 존재할 때만 결과 판정
//                                        calculatedDblValTemp = Double.parseDouble(calculatedTemperatureTmp);
//                                        int temperatureLower = Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE)) - Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_LOWER_VALUE));
//                                        int temperatureUpper = Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE)) + Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_UPPER_VALUE));
//
//                                        logInfo(LogCategory.BT, String.format(Constants.LogMessages.TEST_RESPONSE_SIGNAL_RECEIVED + " [T:%d/%d][C:%d/%s][C:%d/%d][S:%s][G:%d=%d-%d][R:%s] %d < %.2f < %d",
//                                                testTotalCounter, totalTimeCnt, testItemCounter, arrTestItems[testItemIdx][2],
//                                                testItemIdx, arrTestItems.length, currentTestItem, disconnectCheckCount,
//                                                receivedMessageCnt, sendingMessageCnt, readMessage,
//                                                temperatureLower, calculatedDblValTemp, temperatureUpper));
//
//                                        calculatedReceiveResponseResult = (temperatureLower < calculatedDblValTemp && calculatedDblValTemp < temperatureUpper) ? Constants.ResultStatus.OK : Constants.ResultStatus.NG;
//                                        calculatedResultValue = String.valueOf(calculatedDblValTemp);
//                                    }
//                                } catch (Exception e) {
//                                    logError(LogCategory.ER, Constants.ErrorMessages.TEMPERATURE_PROCESSING_ERROR, e);
//                                }
//                            }
//                            // 소비전력
//                            else {
//                                if (isWattTrackingCommand(finalReceiveCommand)) {
//                                    recordWattForCommand(finalReceiveCommand, decElectricValue);
//                                }
//                                int wattLower = 0;
//                                int wattUpper = 0;
//
//                                if (finalReceiveCommand.equals(Constants.TestItemCodes.HT0100)) {
//                                    receiveResponseResult = Constants.ResultStatus.OK;
//                                    resultValue = receiveResponseResult;
//                                    receiveCompAgingResponse = Constants.ResultStatus.COMP_AGING_RESPONSE_01;
//                                } else if (finalReceiveCommand.equals(Constants.TestItemCodes.CM0101)) {
//                                    // receiveCommandResponse = finalReadMessage.substring(9, 11);
//                                    // decElectricValue
//                                    receiveCompAgingResponse = finalReadMessage.substring(9, 11);
//
//                                    calculatedReceiveCommandResponse = finalReadMessage.substring(9, 15);
//                                    calculatedDecTemperatureValue = Integer.parseInt(calculatedReceiveCommandResponse, 16);
//
//                                    temperatureData = hotTemperatureData;
//
//                                    for (int i = 0; i < temperatureData.size(); i++) {
//                                        temperature12Bit = (int) Math.round(Double.valueOf(temperatureData.get(i).get(Constants.JsonKeys.CLM_12_BIT)));
//                                        if (temperature12Bit > calculatedDecTemperatureValue) {
//                                            calculatedDecTemperature = temperatureTmp;
//                                            // UI 업데이트는 나중에 배치 처리 (값만 저장)
//                                            updateTemperature = calculatedDecTemperature;
//
//                                            mapTestTemperature = new HashMap<>();
//                                            mapTestTemperature.put(Constants.JsonKeys.CLM_LINE, Constants.Database.LINE_ID);
//                                            mapTestTemperature.put(Constants.JsonKeys.CLM_UNIT_ID, Constants.Database.UNIT_ID);
//                                            mapTestTemperature.put(Constants.JsonKeys.CLM_TIMESTAMP, currentTimestamp);
//                                            mapTestTemperature.put(Constants.JsonKeys.CLM_PRODUCT_TEMPERATURE, decTemperature);
//                                            lstTestTemperature.add(mapTestTemperature);
//                                            break;
//                                        }
//                                        temperatureTmp = temperatureData.get(i).get(Constants.JsonKeys.CLM_TEMPERATURE);
//                                    }
//                                    calculatedTemperatureTmp = temperatureTmp;
//                                    receiveCommandResponse = receiveCommandResponse + Constants.Common.LOGGER_DEVIDER_01 + temperatureTmp;
//                                    // logInfo(LogCategory.PS, "*********> " + receiveCommandResponse);
//
//                                    if (testItemCounter < Constants.TestThresholds.TEST_COUNTER_THRESHOLD_10 && !compAgingStarted) {
//                                        compAgingStarted = true;
//                                        decTemperatureValueCompStart = temperatureTmp + Constants.Common.DEGREE_CELSIUS;
//                                        calculatedTemperatureValueCompDiff = decTemperatureValueCompStart;
//                                    } else {
//                                        decTemperatureValueCompEnd = temperatureTmp + Constants.Common.DEGREE_CELSIUS;
//                                        calculatedTemperatureValueCompDiff = decTemperatureValueCompStart + Constants.Common.ARROW + decTemperatureValueCompEnd;
//                                    }
//                                    temperatureValueCompDiff = calculatedTemperatureValueCompDiff;
//
//                                                            /*
//                                                            if (testItemCounter >= Constants.TestThresholds.TEST_COUNTER_THRESHOLD_30 && sendResultYn.equals(Constants.ResultStatus.NO)) {
//                                                                // 20240522 결과업로드 지연 방지
//                                                                // ⚠️ ANR 방지: listItemAdapter 접근은 runOnUiThread에서만!
//                                                                // runOnUiThread에서 데이터를 수집한 후 HTTP 통신 스레드로 전달
//                                                                runOnUiThread(() -> {
//                                                                    try {
//                                                                        String checkValue = "";
//                                                                        // listItemAdapter 접근은 runOnUiThread에서만 수행
//                                                                        for (int i = 0; i < listItemAdapter.getCount(); i++) {
//                                                                            VoTestItem voTestItem = ((VoTestItem) listItemAdapter.getItem(i));
//                                                                            checkValue += "&clm_" + ((i < Constants.TestThresholds.TEST_COUNTER_THRESHOLD_10) ? Constants.Common.ZERO : Constants.Common.EMPTY_STRING) + (i + 1) + "=" + voTestItem.getTest_item_command() + Constants.Common.COLON + voTestItem.getTest_result_check_value();
//                                                                        }
//                                                                        checkValue += "&clm_pump_watt=" + decElectricValueForPump;
//                                                                        checkValue += "&clm_comp_diff=" + decTemperatureValueCompStart + Constants.Common.COMMA + decTemperatureValueCompEnd;
//                                                                        checkValue += "&clm_comp_watt=" + decElectricValueForComp;
//                                                                        checkValue += "&clm_heater_watt=" + decElectricValueForHeater;
//                                                                        checkValue += "&clm_test=" + Constants.ResultStatus.MODE_TYPE_TEST;
//
//                                                                        // 수집한 데이터를 final 변수로 캡처하여 HTTP 통신 스레드로 전달
//                                                                        final String finalCheckValue = checkValue;
//                                                                        new Thread(() -> {
//                                                                            try {
//                                                                                String targetUrl = Constants.URLs.HTTP_PROTOCOL + serverIp + Constants.URLs.ENDPOINT_UPDATE_RESULT_TEST_INFO + Constants.URLs.PARAM_PRODUCT_SERIAL_NO + productSerialNo + Constants.URLs.PARAM_TEST_PROCESS + 3 + Constants.URLs.PARAM_TEST_RESULT + ((testNgCnt > 0) ? Constants.ResultStatus.NG : Constants.ResultStatus.OK) + finalCheckValue;
//                                                                                targetUrl = Constants.URLs.HTTP_PROTOCOL + serverIp + Constants.URLs.ENDPOINT_UPDATE_RESULT_TEST_INFO + Constants.Common.QUESTION + Constants.URLs.PARAM_PRODUCT_SERIAL_NO + productSerialNo + Constants.Common.AMPERSAND + Constants.URLs.PARAM_TEST_PROCESS + 3 + Constants.Common.AMPERSAND + Constants.URLs.PARAM_TEST_RESULT + Constants.ResultStatus.OK + finalCheckValue;
//                                                                                logInfo(LogCategory.BI, Constants.LogMessages.TARGET_URL + targetUrl);
//
//                                                                                // 동시성 문제 해결: connection을 로컬 변수로 변경 (각 스레드가 자신만의 connection 사용)
//                                                                                HttpURLConnection connection = null;
//                                                                                try {
//                                                                                    URL url = new URL(targetUrl);
//                                                                                    connection = (HttpURLConnection) url.openConnection();
//
//                                                                                    connection.setReadTimeout(Constants.Timeouts.HTTP_READ_TIMEOUT_MS);
//                                                                                    connection.setConnectTimeout(Constants.Timeouts.HTTP_CONNECT_TIMEOUT_MS);
//                                                                                    connection.setRequestMethod(Constants.HTTP.METHOD_GET);
//                                                                                    connection.setDoInput(true);
//                                                                                    connection.setDoOutput(true);
//
//                                                                                    int responseCode = connection.getResponseCode();
//                                                                                    if (responseCode == HttpURLConnection.HTTP_OK) {
//                                                                                        logInfo(LogCategory.BI, Constants.LogMessages.HTTP_OK_SUCCESS);
//                                                                                        sendResultYn = Constants.ResultStatus.YES;
//                                                                                    } else {
//                                                                                        logWarn(LogCategory.BI, Constants.LogMessages.HTTP_OK_FAILED);
//                                                                                    }
//                                                                                } finally {
//                                                                                    // 리소스 정리 보장
//                                                                                    safeDisconnectConnection(connection);
//                                                                                }
//                                                                            } catch (Exception e) {
//                                                                                logError(LogCategory.ER, Constants.ErrorMessages.HTTP_RESULT_UPDATE_ERROR, e);
//                                                                                e.printStackTrace();
//                                                                            }
//                                                                        }).start();
//                                                                    } catch (Exception e) {
//                                                                        logError(LogCategory.ER, Constants.ErrorMessages.ERROR_COLLECTING_LIST_ITEM_ADAPTER_DATA, e);
//                                                                    }
//                                                                });
//                                                            }
//                                                            */
//
//                                    if (testItemCounter < Constants.TestThresholds.TEST_COUNTER_THRESHOLD_30) {
//                                        if ((!lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE_WATT).equals("") && !lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE_WATT).isEmpty()) &&
//                                                (!lstSpecData.get(0).get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT).equals("") && !lstSpecData.get(0).get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT).isEmpty()) &&
//                                                (!lstSpecData.get(0).get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT).equals("") && !lstSpecData.get(0).get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT).isEmpty())) {
//                                            if (receiveCommandResponseOK.equals(finalReceiveCommand)) {
//                                                return;
//                                            }
//                                            // UI 업데이트는 나중에 배치 처리 (값만 저장)
//                                            updateCompWatt = String.valueOf(decElectricValue);
//                                            decElectricValueForComp = decElectricValue;
//                                            calculatedWattLower = Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE_WATT)) - Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT));
//                                            calculatedWattUpper = Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE_WATT)) + Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT));
//                                            calculatedReceiveResponseResult = (calculatedWattLower < decElectricValue && decElectricValue < calculatedWattUpper) ? Constants.ResultStatus.OK : Constants.ResultStatus.NG;
//                                            if (calculatedReceiveResponseResult.equals(Constants.ResultStatus.OK)) {
//                                                receiveCommandResponseOK = finalReceiveCommand;
//                                            }
//                                            calculatedResultValue = String.valueOf(decElectricValue);
//                                        }
//                                    }
////                                                    }
//
////                                                    if(readMessage.indexOf(("\u0002"))==0) {
////                                                        testItemCounter = Integer.parseInt(arrTestItems[testItemIdx][2]);
////                                                    }
//
//                                    logBtTestResponse(finalReceiveCommand, calculatedReceiveCommandResponse, decElectricValue,
//                                            calculatedWattLower, calculatedWattUpper,
//                                            finalReadMessage.substring(finalReadMessage.indexOf(Constants.CharCodes.STX) + 1, finalReadMessage.indexOf(Constants.CharCodes.ETX)));
//                                } else if (finalReceiveCommand.equals(Constants.TestItemCodes.CM0102)) {
//                                    receiveCompAgingResponse = Constants.ResultStatus.COMP_AGING_RESPONSE_02;
//                                    testItemCounter = Integer.parseInt(arrTestItems[testItemIdx][2]);
//                                    receiveResponseResult = Constants.ResultStatus.OK;
//
//                                    logBtTestResponse(finalReceiveCommand, calculatedReceiveCommandResponse, decElectricValue,
//                                            calculatedWattLower, calculatedWattUpper,
//                                            finalReadMessage.substring(finalReadMessage.indexOf(Constants.CharCodes.STX) + 1, finalReadMessage.indexOf(Constants.CharCodes.ETX)));
//                                } else if (finalReceiveCommand.equals(Constants.TestItemCodes.HT0101)) {
//                                    // receiveCommandResponse = finalReadMessage.substring(9, 11);
//                                    // decElectricValue
//                                    calculatedReceiveCommandResponse = finalReadMessage.substring(9, 15);
//                                    calculatedDecTemperatureValue = Integer.parseInt(calculatedReceiveCommandResponse, 16);
//
//                                    temperatureData = coldTemperatureData;
//
//                                    for (int i = 0; i < temperatureData.size(); i++) {
//                                        temperature12Bit = (int) Math.round(Double.valueOf(temperatureData.get(i).get(Constants.JsonKeys.CLM_12_BIT)));
//                                        if (temperature12Bit > calculatedDecTemperatureValue) {
//                                            calculatedDecTemperature = temperatureTmp;
//                                            // UI 업데이트는 나중에 배치 처리 (값만 저장)
//                                            updateTemperature = calculatedDecTemperature;
//
//                                            mapTestTemperature = new HashMap<>();
//                                            mapTestTemperature.put(Constants.JsonKeys.CLM_LINE, Constants.Database.LINE_ID);
//                                            mapTestTemperature.put(Constants.JsonKeys.CLM_UNIT_ID, Constants.Database.UNIT_ID);
//                                            mapTestTemperature.put(Constants.JsonKeys.CLM_TIMESTAMP, currentTimestamp);
//                                            mapTestTemperature.put(Constants.JsonKeys.CLM_PRODUCT_TEMPERATURE, decTemperature);
//                                            lstTestTemperature.add(mapTestTemperature);
//                                            break;
//                                        }
//                                        temperatureTmp = temperatureData.get(i).get(Constants.JsonKeys.CLM_TEMPERATURE);
//                                    }
//
//                                    receiveCommandResponse = receiveCommandResponse + Constants.Common.COMMA + temperatureTmp;
//
//                                    if (testItemCounter < Constants.TestThresholds.TEST_COUNTER_THRESHOLD_5) {
//                                        if ((!lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE_WATT).equals("") && !lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE_WATT).isEmpty()) &&
//                                                (!lstSpecData.get(0).get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT).equals("") && !lstSpecData.get(0).get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT).isEmpty()) &&
//                                                (!lstSpecData.get(0).get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT).equals("") && !lstSpecData.get(0).get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT).isEmpty())) {
//                                            if (!receiveCommandResponseOK.equals(finalReceiveCommand)) {
//                                                // UI 업데이트는 나중에 배치 처리 (값만 저장)
//                                                updateHeaterWatt = String.valueOf(decElectricValue);
//                                                decElectricValueForHeater = decElectricValue;
//                                            }
//                                            wattLower = Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE_WATT)) - Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT));
//                                            wattUpper = Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE_WATT)) + Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT));
//                                            receiveResponseResult = (wattLower < decElectricValue && decElectricValue < wattUpper) ? Constants.ResultStatus.OK : Constants.ResultStatus.NG;
//                                            if (receiveResponseResult.equals(Constants.ResultStatus.OK)) {
//                                                receiveCommandResponseOK = finalReceiveCommand;
//                                            }
//                                            resultValue = String.valueOf(decElectricValue);
//                                        }
//                                    }
//                                    resultInfo = Constants.Common.EMPTY_STRING;
//                                    // resultInfo = wattLower + " < " + decElectricValue + " < " + wattUpper;
//                                    resultInfo = decElectricValue + Constants.Common.WATT_UNIT;
//                                    logBtTestResponse(finalReceiveCommand, receiveCommandResponse, decElectricValue, wattLower, wattUpper, null);
//                                } else if (finalReceiveCommand.equals(Constants.TestItemCodes.PM0101)) {
//                                    if (testItemCounter > Constants.TestThresholds.TEST_COUNTER_THRESHOLD_5 && testItemCounter < Integer.parseInt(arrTestItems[testItemIdx][2])) {
//                                        // UI 업데이트는 나중에 배치 처리 (값만 저장)
//                                        updatePumpWatt = String.valueOf(decElectricValue);
//                                        decElectricValueForPump = decElectricValue;
//                                        wattLower = Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE_WATT)) - Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT));
//                                        wattUpper = Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE_WATT)) + Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT));
//                                        receiveResponseResult = (wattLower < decElectricValue && decElectricValue < wattUpper) ? Constants.ResultStatus.OK : Constants.ResultStatus.NG;
//                                        receiveResponseResult = (0 < decElectricValue) ? Constants.ResultStatus.OK : Constants.ResultStatus.NG;
//                                        resultInfo = decElectricValue + Constants.Common.WATT_UNIT;
//                                        if (receiveResponseResult.equals(Constants.ResultStatus.OK)) {
//                                            receiveCommandResponseOK = finalReceiveCommand;
//                                        }
//                                        // logInfo(LogCategory.BT, String.format(Constants.LogMessages.PUMP_POWER_CONSUMPTION + ": %d < %.0f < %d", wattLower, decElectricValue, wattUpper));
//                                    }
//                                    resultInfo = Constants.Common.EMPTY_STRING;
//                                    // resultInfo = wattLower + " < " + decElectricValue + " < " + wattUpper;
//                                    resultInfo = decElectricValue + Constants.Common.WATT_UNIT;
//                                    logBtTestResponse(finalReceiveCommand, receiveCommandResponse, decElectricValue, wattLower, wattUpper, null);
//                                } else if (finalReceiveCommand.equals(Constants.TestItemCodes.SV0101)) {
//                                    if (testItemCounter > 4) {
//                                        wattLower = Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE_WATT)) - Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT));
//                                        wattUpper = Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE_WATT)) + Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT));
//                                        logBtTestResponse(finalReceiveCommand, receiveCommandResponse, decElectricValue, wattLower, wattUpper, null);
//                                        receiveResponseResult = (wattLower < decElectricValue && decElectricValue < wattUpper) ? Constants.ResultStatus.OK : Constants.ResultStatus.NG;
//                                        // if(receiveResponseResult.equals(Constants.ResultStatus.OK)) { receiveCommandResponseOK = finalReceiveCommand; }
//                                        resultInfo = Constants.Common.EMPTY_STRING;
//                                        // resultInfo = wattLower + " < " + decElectricValue + " < " + wattUpper;
//                                        resultInfo = decElectricValue + Constants.Common.WATT_UNIT;
//                                    }
//                                } else if (finalReceiveCommand.equals(Constants.TestItemCodes.SV0201)) {
//                                    if (testItemCounter > 4) {
//                                        wattLower = Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE_WATT)) - Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT));
//                                        wattUpper = Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE_WATT)) + Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT));
//                                        logBtTestResponse(finalReceiveCommand, receiveCommandResponse, decElectricValue, wattLower, wattUpper, null);
//                                        receiveResponseResult = (wattLower < decElectricValue && decElectricValue < wattUpper) ? Constants.ResultStatus.OK : Constants.ResultStatus.NG;
//                                        if (receiveResponseResult.equals(Constants.ResultStatus.OK)) {
//                                            receiveCommandResponseOK = finalReceiveCommand;
//                                        }
//                                        resultInfo = Constants.Common.EMPTY_STRING;
//                                        // resultInfo = wattLower + " < " + decElectricValue + " < " + wattUpper;
//                                        resultInfo = decElectricValue + Constants.Common.WATT_UNIT;
//                                    }
//                                } else if (finalReceiveCommand.equals(Constants.TestItemCodes.SV0301)) {
//                                    if (testItemCounter > 4) {
//                                        wattLower = Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE_WATT)) - Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT));
//                                        wattUpper = Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE_WATT)) + Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT));
//                                        logBtTestResponse(finalReceiveCommand, receiveCommandResponse, decElectricValue, wattLower, wattUpper, null);
//                                        receiveResponseResult = (wattLower < decElectricValue && decElectricValue < wattUpper) ? Constants.ResultStatus.OK : Constants.ResultStatus.NG;
//                                        if (receiveResponseResult.equals(Constants.ResultStatus.OK)) {
//                                            receiveCommandResponseOK = finalReceiveCommand;
//                                        }
//                                        resultInfo = Constants.Common.EMPTY_STRING;
//                                        // resultInfo = wattLower + " < " + decElectricValue + " < " + wattUpper;
//                                        resultInfo = decElectricValue + Constants.Common.WATT_UNIT;
//                                    }
//                                } else if (finalReceiveCommand.equals(Constants.TestItemCodes.SV0401)) {
//                                    if (testItemCounter > 4) {
//                                        wattLower = Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE_WATT)) - Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT));
//                                        wattUpper = Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_VALUE_WATT)) + Integer.parseInt(lstSpecData.get(0).get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT));
//                                        logBtTestResponse(finalReceiveCommand, receiveCommandResponse, decElectricValue, wattLower, wattUpper, null);
//                                        receiveResponseResult = (wattLower < decElectricValue && decElectricValue < wattUpper) ? Constants.ResultStatus.OK : Constants.ResultStatus.NG;
//                                        if (receiveResponseResult.equals(Constants.ResultStatus.OK)) {
//                                            receiveCommandResponseOK = finalReceiveCommand;
//                                        }
//                                        resultInfo = Constants.Common.EMPTY_STRING;
//                                        // resultInfo = wattLower + " < " + decElectricValue + " < " + wattUpper;
//                                        resultInfo = decElectricValue + Constants.Common.WATT_UNIT;
//                                    }
//                                } else if (finalReceiveCommand.equals(Constants.TestItemCodes.CM0100)) {
//                                    receiveResponseResult = Constants.Common.EMPTY_STRING;
//                                    receiveResponseResultTxt = Constants.Common.EMPTY_STRING;
//                                    // receiveResponseResult = (receiveCompAgingResponse.equals(Constants.ResultStatus.COMP_AGING_RESPONSE_02))?Constants.ResultStatus.OK:Constants.ResultStatus.NG;
//                                    // receiveResponseResultTxt = (receiveCompAgingResponse.equals(Constants.ResultStatus.COMP_AGING_RESPONSE_02))?Constants.ResultStatus.SUCCESS_TEXT:Constants.ResultStatus.FAIL_TEXT;
//                                    receiveResponseResult = Constants.ResultStatus.OK;
//                                    receiveResponseResultTxt = Constants.Common.SUCCESS;
//                                    // receiveResponseResultTxt = Constants.Common.COMP_AC_COOLING_PERFORMANCE + receiveResponseResultTxt;
//                                    receiveResponseResultTxt = receiveResponseResultTxt;
//                                } else {
//                                    if (finalReceiveCommandResponse.equals(lstSpecData.get(0).get(Constants.JsonKeys.CLM_RESPONSE_VALUE))) {
//                                        receiveCommandResponseOK = finalReceiveCommand;
//                                        receiveResponseResult = Constants.ResultStatus.OK;
//                                    } else {
//                                        receiveResponseResult = Constants.ResultStatus.NG;
//                                    }
//
//
//                                    // logInfo(LogCategory.PS, ">>>>> lstSpecData.get(0) " + lstSpecData.get(0));
//                                    logBtTestResponseSimple(finalReceiveCommand, finalReceiveCommandResponse,
//                                            lstSpecData.get(0).get(Constants.JsonKeys.CLM_RESPONSE_VALUE) + Constants.Common.LOGGER_DEVIDER_01 + receiveResponseResult);
//                                    resultValue = String.valueOf(finalReceiveCommandResponse);
//
//                                    // 파우셋 UV LED
//                                    if (finalReceiveCommand.equals(Constants.TestItemCodes.UV0201)) {
//                                        if (receiveResponseResult.equals(Constants.ResultStatus.NG)) {
//                                            // testItemCounter = Integer.parseInt(arrTestItems[testItemIdx][2]);
//                                        }
//                                    }
//                                }
//
//                                if (finalReceiveCommand.equals(Constants.TestItemCodes.CM0101)
//                                        || finalReceiveCommand.equals(Constants.TestItemCodes.TH0101)
//                                        || finalReceiveCommand.equals(Constants.TestItemCodes.TH0201)) {
//                                } else {
//                                    calculatedTemperatureTmp = "0";
//                                }
//                            }
//                        }
//                        // logInfo(LogCategory.PS, Constants.Common.LOG_MESSGE_SEPARATOR);
//
//                        // 계산된 결과를 멤버 변수에 할당
//                        if (!calculatedReceiveResponseResult.isEmpty()) {
//                            receiveResponseResult = calculatedReceiveResponseResult;
//                        }
//                        if (!calculatedResultValue.isEmpty()) {
//                            resultValue = calculatedResultValue;
//                        }
//                        if (!calculatedReceiveCommandResponse.isEmpty()) {
//                            receiveCommandResponse = calculatedReceiveCommandResponse;
//                        }
//
//                        // 리스트 아이템 업데이트 데이터 수집 (백그라운드에서)
//                        // ⚠️ 중요: listItemAdapter 직접 접근 제거, 업데이트할 데이터만 수집
//                        updateItemCommand = finalReceiveCommand;
//                        updateItemResult = receiveResponseResult;
//                        updateItemCheckValue = resultValue;
//
//                        // 아이템별 info 설정
//                        if (finalReceiveCommand.contains(Constants.TestItemCodes.CM0101)) {
//                            updateItemInfo = temperatureValueCompDiff;
//                        } else if (finalReceiveCommand.contains(Constants.TestItemCodes.TH0101)) {
//                            updateItemInfo = decTemperatureHotValue;
//                        } else if (finalReceiveCommand.contains(Constants.TestItemCodes.TH0201)) {
//                            updateItemInfo = decTemperatureColdValue;
//                        } else if (finalReceiveCommand.contains(Constants.TestItemCodes.SV0101) || finalReceiveCommand.contains(Constants.TestItemCodes.SV0201) ||
//                                finalReceiveCommand.contains(Constants.TestItemCodes.SV0301) || finalReceiveCommand.contains(Constants.TestItemCodes.SV0401)) {
//                            updateItemInfo = String.valueOf(decElectricValue);
//                        }
//
//                        if (Constants.TestItemCodes.CM0100.equals(finalReceiveCommand)) {
//                            updateItemNameSuffix = receiveResponseResultTxt;
//                        }
//
//                        shouldUpdateListAdapter = true;
//
//                        // logInfo(LogCategory.BT, String.format("검사 응답 신호 수신: %s / %s / %s / %.0f W",
//                        //         finalReadMessage.substring(finalReadMessage.indexOf((Constants.CharCodes.STX)) + 1, finalReadMessage.indexOf((Constants.CharCodes.ETX))),
//                        //         finalReceiveCommand, receiveResponseResult, decElectricValue));
//
//                        // 로그 텍스트 업데이트 (mode_type이 "T"인 경우)
//                        if (modeType.equals("T")) {
//                            String compAgingCondition = "";
//                            if (finalReceiveCommand.contains(Constants.TestItemCodes.CM01)) {
//                                compAgingCondition = decElectricValue + "W / " + receiveCompAgingResponse;
//                            } else {
//                                compAgingCondition = decElectricValue + Constants.Common.WATT_UNIT;
//                            }
//                            logTextParam += "[" + getCurrentDatetime(datetimeFormat) + Constants.Common.LOGGER_DEVIDER_01 + String.format(Constants.InitialValues.NUMBER_FORMMATING_03D, testItemCounter) + Constants.Common.LOGGER_DEVIDER_01 + String.format(Constants.InitialValues.NUMBER_FORMMATING_03D, Integer.parseInt(arrTestItems[testItemIdx][2])) + "] " + (finalReadMessage.substring(finalReadMessage.indexOf(("\u0002")) + 1, finalReadMessage.indexOf(("\u0003")))) + Constants.Common.LOGGER_DEVIDER_01 + finalReceiveCommand + Constants.Common.LOGGER_DEVIDER_01 + receiveResponseResult + "(" + receiveCommandResponse + ") / " + compAgingCondition;
//                            logText = "▶ [" + getCurrentDatetime(datetimeFormat) + Constants.Common.LOGGER_DEVIDER_01 + String.format(Constants.InitialValues.NUMBER_FORMMATING_03D, testItemCounter) + Constants.Common.LOGGER_DEVIDER_01 + String.format(Constants.InitialValues.NUMBER_FORMMATING_03D, Integer.parseInt(arrTestItems[testItemIdx][2])) + "] " + (finalReadMessage.substring(finalReadMessage.indexOf(("\u0002")) + 1, finalReadMessage.indexOf(("\u0003")))) + Constants.Common.LOGGER_DEVIDER_01 + finalReceiveCommand + Constants.Common.LOGGER_DEVIDER_01 + receiveResponseResult + "(" + receiveCommandResponse + ") / " + decElectricValue + "W\n" + logText;
//                            updateLogText = logText;  // UI 업데이트 값 저장
//
//                            // HTTP 통신은 별도 스레드로 유지
//                            runOnBtWorker(new Runnable() {
//                                @Override
//                                public void run() {
//                                    try {
//                                        StringBuilder decElectricValueList = new StringBuilder();
//                                        if (finalReceiveCommand.contains(Constants.TestItemCodes.RE0101)) {
//                                            // ⚠️ 주의: listItemAdapter 접근은 runOnUiThread에서만!
//                                            // 여기서는 이미 수집된 데이터 사용 (lstWatt는 멤버 변수)
//                                            for (int i = 0; i < lstWatt.size(); i++) {
//                                                decElectricValueList.append(lstWatt.get(i)).append(",");
//                                            }
//                                            if (decElectricValueList.length() > 0) {
//                                                decElectricValueList = new StringBuilder(decElectricValueList.substring(0, decElectricValueList.length() - 1));
//                                            }
//                                            decElectricValueList.insert(0, "&clm_watt_log=");
//                                        }
//
//                                        testTaskThread = new ActivityModel_0003.RequestTestTaskThreadAsync();
//                                        urlTestTaskStr = "http://" + serverIp + "/OVIO/TestTaskInfoUpdate.jsp" + "?clm_test_task_log=" + URLEncoder.encode(logTextParam) + "&clm_test_unit_seq=" + unit_no + "&clm_unit_ip=" + ipAddress + "&clm_product_serial_no=" + productSerialNo + "&clm_test_process_id=" + testProcessId + "&clm_model_id=" + globalModelId + decElectricValueList;
//
//                                        // testProcessId = (finalReceiveCommand.contains(Constants.TestItemCodes.RE0101)) ? "" : testProcessId;
//                                        testProcessId = (finalReceiveCommand.equals(Constants.TestItemCodes.RE0101) ||
//                                                (finalReceiveCommandTmp.equals(Constants.TestItemCodes.CM0102) && finalReceiveCommand.equals(Constants.TestItemCodes.CM0101)) ||
//                                                (finalReceiveCommandTmp.equals(Constants.TestItemCodes.CM0103) && finalReceiveCommand.equals(Constants.TestItemCodes.CM0101)) ||
//                                                (processFinishedTmp && finalReceiveCommand.equals(Constants.TestItemCodes.CM0101))) ? "" : testProcessId;
//
//                                        testTaskThread.execute();
//                                        logTextParam = "";
//                                    } catch (Exception e) {
//                                        logError(LogCategory.ER, Constants.ErrorMessages.THREAD_EXECUTION_ERROR, e);
//                                    }
//                                }
//                            });
//                        }
//
//                        if (testItemCounter == Integer.parseInt(arrTestItems[testItemIdx][2])) {
//                            // sendBtMessage("ST0201");
//                        }
//
//                        if (testItemCounter > Integer.parseInt(arrTestItems[testItemIdx][2]) - Constants.TestThresholds.TEST_COUNTER_THRESHOLD_5) {
//                            processFinishedTmp = true;
//                        }
//
//                        // processFinishedYn = false;
//                        // 카운트 계산은 runOnUiThread에서 수행 (listItemAdapter 접근 필요)
//                        if (finalReceiveCommand.equals(Constants.TestItemCodes.RE0101) ||
//                                (finalReceiveCommandTmp.equals(Constants.TestItemCodes.CM0102) && finalReceiveCommand.equals(Constants.TestItemCodes.CM0101)) ||
//                                (finalReceiveCommandTmp.equals(Constants.TestItemCodes.CM0103) && finalReceiveCommand.equals(Constants.TestItemCodes.CM0101)) ||
//                                (processFinishedTmp && finalReceiveCommand.equals(Constants.TestItemCodes.CM0101))) {
//                            if (!processFinished) {
//                                // runOnBtWorker(new Runnable() {
//                                // @Override
//                                // public void run() {
//                                try {
//                                    // 1) UI 스레드에서 listItemAdapter 접근 + 문자열 조립
//                                    scheduleUiUpdate(() -> {
//                                        try {
//                                            StringBuilder builder = new StringBuilder();
//
//                                            for (int i = 0; i < listItemAdapter.getCount(); i++) {
//                                                VoTestItem item = (VoTestItem) listItemAdapter.getItem(i);
//                                                builder.append("&clm_")
//                                                        .append((i < Constants.TestThresholds.TEST_COUNTER_THRESHOLD_10) ? Constants.Common.ZERO : Constants.Common.EMPTY_STRING)
//                                                        .append(i + 1)
//                                                        .append("=")
//                                                        .append(item.getTest_item_command())
//                                                        .append(Constants.Common.COLON)
//                                                        .append(item.getTest_result_check_value());
//                                            }
//
//                                            builder.append("&clm_pump_watt=").append(decElectricValueForPump);
//                                            builder.append("&clm_comp_diff=").append(decTemperatureValueCompStart).append(Constants.Common.COMMA).append(decTemperatureValueCompEnd);
//                                            builder.append("&clm_comp_watt=").append(decElectricValueForComp);
//                                            builder.append("&clm_heater_watt=").append(decElectricValueForHeater);
//                                            builder.append("&clm_test=").append(Constants.ResultStatus.MODE_TYPE_TEST);
//
//                                            final String finalCheckValue = builder.toString();
//                                            final String finalLmsTestSeq = lmsTestSeq;
//
//                                            // 2) 워커 스레드에서 네트워크 + DB 업데이트
//                                            runOnBtWorker(() -> executeResultUpload(finalCheckValue, finalLmsTestSeq));
//                                        } catch (Exception e) {
//                                            logError(LogCategory.ER, Constants.ErrorMessages.ERROR_COLLECTING_LIST_ITEM_ADAPTER_DATA, e);
//                                        }
//                                    });
//                                                /*
//                                                                // 20240522 결과업로드 지연 방지
//                                                                // Phase 6: listItemAdapter 접근 최적화 - scheduleUiUpdate 사용
//                                                                // scheduleUiUpdate에서 데이터를 수집한 후 HTTP 통신 스레드로 전달
//                                                                scheduleUiUpdate(() -> {
//                                                                    try {
//                                                                        String checkValue = "";
//                                                                        // listItemAdapter 접근은 메인 스레드에서만 수행
//                                                                        for (int i = 0; i < listItemAdapter.getCount(); i++) {
//                                                                            VoTestItem voTestItem = ((VoTestItem) listItemAdapter.getItem(i));
//                                                                            checkValue += "&clm_" + ((i < Constants.TestThresholds.TEST_COUNTER_THRESHOLD_10) ? Constants.Common.ZERO : Constants.Common.EMPTY_STRING) + (i + 1) + "=" + voTestItem.getTest_item_command() + Constants.Common.COLON + voTestItem.getTest_result_check_value();
//                                                                        }
//                                                                        checkValue += "&clm_pump_watt=" + decElectricValueForPump;
//                                                                        checkValue += "&clm_comp_diff=" + decTemperatureValueCompStart + Constants.Common.COMMA + decTemperatureValueCompEnd;
//                                                                        checkValue += "&clm_comp_watt=" + decElectricValueForComp;
//                                                                        checkValue += "&clm_heater_watt=" + decElectricValueForHeater;
//                                                                        checkValue += "&clm_test=" + Constants.ResultStatus.MODE_TYPE_TEST;
//
//                                                                        // 수집한 데이터를 final 변수로 캡처하여 HTTP 통신 스레드로 전달
//                                                                        final String finalCheckValue = checkValue;
//                                                                        final String finalLmsTestSeq = lmsTestSeq;
//                                                        // final String finalCurrentTestItem = (currentTestItem != null) ? currentTestItem : "";
//                                                        // runOnBtWorker(() -> {
//                                                                            String httpSuccessYn = Constants.ResultStatus.NO; // 기본값은 실패
//                                                                            try {
//                                                                                String targetUrl = Constants.URLs.HTTP_PROTOCOL + serverIp + Constants.URLs.ENDPOINT_UPDATE_RESULT_TEST_INFO + Constants.URLs.PARAM_PRODUCT_SERIAL_NO + productSerialNo + Constants.URLs.PARAM_TEST_PROCESS + 3 + Constants.URLs.PARAM_TEST_RESULT + ((testNgCnt > 0) ? Constants.ResultStatus.NG : Constants.ResultStatus.OK) + finalCheckValue;
//                                                                                targetUrl = Constants.URLs.HTTP_PROTOCOL + serverIp + Constants.URLs.ENDPOINT_UPDATE_RESULT_TEST_INFO + Constants.Common.QUESTION + Constants.URLs.PARAM_PRODUCT_SERIAL_NO + productSerialNo + Constants.Common.AMPERSAND + Constants.URLs.PARAM_TEST_PROCESS + 3 + Constants.Common.AMPERSAND + Constants.URLs.PARAM_TEST_RESULT + Constants.ResultStatus.OK + finalCheckValue;
//                                                                                logInfo(LogCategory.BI, Constants.LogMessages.TARGET_URL + targetUrl);
//
//                                                                                // HTTP 요청 재시도 로직
//                                                                                int retryCount = Constants.Network.HTTP_RETRY_COUNT;
//                                                                                boolean requestSuccess = false;
//
//                                                                                for (int attempt = 1; attempt <= retryCount; attempt++) {
//                                                                                    HttpURLConnection connection = null;
//                                                                                    try {
//                                                                                        logInfo(LogCategory.BI, "HTTP 요청 시도 [" + attempt + "/" + retryCount + "]");
//
//                                                                                        URL url = new URL(targetUrl);
//                                                                                        connection = (HttpURLConnection) url.openConnection();
//
//                                                                                        connection.setReadTimeout(Constants.Timeouts.HTTP_READ_TIMEOUT_MS);
//                                                                                        connection.setConnectTimeout(Constants.Timeouts.HTTP_CONNECT_TIMEOUT_MS);
//                                                                                        connection.setRequestMethod(Constants.HTTP.METHOD_GET);
//                                                                                        connection.setDoInput(true);
//                                                                                        connection.setDoOutput(true);
//
//                                                                                        int responseCode = connection.getResponseCode();
//                                                                                        if (responseCode == HttpURLConnection.HTTP_OK) {
//                                                                                            logInfo(LogCategory.BI, Constants.LogMessages.HTTP_OK_SUCCESS + " (시도 " + attempt + "/" + retryCount + ")");
//                                                                                            sendResultYn = Constants.ResultStatus.YES;
//                                                                                            httpSuccessYn = Constants.ResultStatus.YES;
//                                                                                            requestSuccess = true;
//                                                                                            break; // 성공 시 루프 종료
//                                                                                        } else {
//                                                                                            logWarn(LogCategory.BI, Constants.LogMessages.HTTP_OK_FAILED + " (시도 " + attempt + "/" + retryCount + ", 응답 코드: " + responseCode + ")");
//                                                                                            if (attempt < retryCount) {
//                                                                                                // 마지막 시도가 아니면 재시도 전 대기
//                                                                                                Thread.sleep(Constants.Network.HTTP_RETRY_DELAY_MS);
//                                                                                            }
//                                                                                        }
//                                                                                    } catch (
//                                                                                            Exception e) {
//                                                                                        logError(LogCategory.ER, Constants.ErrorMessages.HTTP_RESULT_UPDATE_ERROR + " (시도 " + attempt + "/" + retryCount + "): " + e.getMessage(), e);
//                                                                                        if (attempt < retryCount) {
//                                                                                            // 마지막 시도가 아니면 재시도 전 대기
//                                                                                            try {
//                                                                                                Thread.sleep(Constants.Network.HTTP_RETRY_DELAY_MS);
//                                                                                            } catch (
//                                                                                                    InterruptedException ie) {
//                                                                                                Thread.currentThread().interrupt();
//                                                                                                logError(LogCategory.ER, "HTTP 재시도 대기 중 인터럽트 발생", ie);
//                                                                                            }
//                                                                                        }
//                                                                                    } finally {
//                                                                                        // 리소스 정리 보장
//                                                                                        safeDisconnectConnection(connection);
//                                                                                    }
//                                                                                }
//
//                                                                                // 모든 재시도 실패 시 최종 실패 처리
//                                                                                if (!requestSuccess) {
//                                                                                    logInfo(LogCategory.PS, "HTTP 요청 실패: " + retryCount + "번 시도 모두 실패");
//                                                                                    httpSuccessYn = Constants.ResultStatus.NO;
//                                                                                }
//
//                                                                                // HTTP 전송 성공 여부를 tbl_test_history에 업데이트
//                                                                                try {
//                                                                                    TestData.updateProductTestHistoryHttpSuccess(getBaseContext(), finalLmsTestSeq, httpSuccessYn);
//                                                                                } catch (
//                                                                                        Exception e) {
//                                                                                    logError(LogCategory.ER, "Error updating HTTP success status in test history", e);
//                                                                                }
//                                                            } catch (
//                                                                    Exception e) {
//                                                                                logError(LogCategory.ER, Constants.ErrorMessages.HTTP_RESULT_UPDATE_ERROR, e);
//                                                                                e.printStackTrace();
//                                                                                // 예외 발생 시에도 실패로 기록
//                                                                                try {
//                                                                                    TestData.updateProductTestHistoryHttpSuccess(getBaseContext(), finalLmsTestSeq, Constants.ResultStatus.NO);
//                                                                                } catch (
//                                                                                        Exception ex) {
//                                                                                    logError(LogCategory.ER, "Error updating HTTP success status in test history", ex);
//                                                                                }
//                                                                            } finally {
//                                                                                // lstMapWattTemp에 저장된 정보들을 tbl_test_history_linear_data 테이블에 저장
//                                                                                try {
//                                                                                    for (int i = 0; i < lstMapWattTemp.size(); i++) {
//                                                                                        String[] wattTempData = lstMapWattTemp.get(i);
//                                                                                        if (wattTempData != null && wattTempData.length >= 3) {
//                                                                                            String timestamp = wattTempData[0];
//                                                                                            String temperature = wattTempData[1];
//                                                                                            String watt = wattTempData[2];
//                                                                                            String testItem = wattTempData[3];
//                                                                                            // testItemId는 현재 테스트 항목 또는 빈 문자열 사용
//                                                                                            TestData.insertTestHistoryLinearData(getBaseContext(), finalLmsTestSeq, testItem, timestamp, temperature, watt);
//                                                                                        }
//                                                                                    }
//                                                                                } catch (
//                                                                                        Exception e) {
//                                                                                    logError(LogCategory.ER, "Error saving linear data to test history", e);
//                                                                                }
//                                                                            }
//                                                        // });
//                                                                    } catch (Exception e) {
//                                                                        logError(LogCategory.ER, Constants.ErrorMessages.ERROR_COLLECTING_LIST_ITEM_ADAPTER_DATA, e);
//                                                                    }
//                                                                });
//                                                */
//
//                                    stopBtMessageTimer();
//
//                                    btSearchOnOff = false;
//                                    clearBluetoothReconnect();
//                                    // btSocket.close();
//                                    btSocket = null;
//                                    deviceSelected = null;
//                                    // 복잡한 UI 업데이트 블록 통합: scheduleUiUpdate 사용
//                                    scheduleUiUpdate(() -> {
//                                        setDisplayLightValueChange(Constants.UI.DISPLAY_BRIGHTNESS);
//                                        cookie_preferences = getApplicationContext().getSharedPreferences(Constants.SharedPrefKeys.TEST_COOKIE_INFO, MODE_PRIVATE);
//                                        cookie_info = cookie_preferences.edit();
//
//                                        cookie_info.putString(Constants.IntentExtras.TEST_START_DATETIME, lmsTestSeq);
//                                        cookie_info.putString(Constants.SharedPrefKeys.TEST_PRODUCT_SERIAL_NO, productSerialNo);
//                                        cookie_info.commit();
//                                        // logInfo(LogCategory.PS, Constants.LogMessages.TEST_START_DATETIME_LMS_TEST_SEQ + lmsTestSeq);
//                                        // logInfo(LogCategory.PS, Constants.LogMessages.TEST_PROCESS_ID_FINISHED + testProcessId);
//                                        // testProcessId = "";
//
//                                        try {
//                                            if (btSocket != null && btSocket.isConnected()) {
//                                                try {
//                                                    btSocket.close();
//                                                } catch (
//                                                        IOException e) {
//                                                    logError(LogCategory.ER, Constants.ErrorMessages.ERROR_CLOSING_SOCKET, e);
//                                                }
//                                            }
//                                            // btnTestRestart.setOnTouchListener(null);
//                                            // tvConnectBtRamp.setText("");
//                                            tvConnectBtRamp.setBackgroundColor(getBaseContext().getResources().getColor(R.color.red_01));
//
//                                        } catch (Exception e) {
//                                            throw new RuntimeException(e);
//                                        }
//
//                                        btnTestRestart.setBackgroundColor(getColor(R.color.green_02));
//                                        btnTestRestart.setTextColor(getColor(R.color.white));
//                                        // getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
//
//                                                                        /*
//                                                                        ItemAdapterTestItem listItemResultAdapter = new ItemAdapterTestItem();
//
//                                                                        for (int i = 0; i < listItemAdapter.getCount(); i++) {
//                                                                            logDebug(LogCategory.RS, String.format(Constants.LogMessages.TEST_RESULT_FORMAT, i, ((VoTestItem) listItemAdapter.getItem(i)).VoMapInfo()));
//                                                                            if (((VoTestItem) listItemAdapter.getItem(i)).getTest_item_result().equals(Constants.ResultStatus.NG)) {
//                                                                                VoTestItem voTestItem = (VoTestItem) listItemAdapter.getItem(i);
//                                                                                voTestItem.setTest_finish_yn(Constants.ResultStatus.YES);
//                                                                                voTestItem.setTest_item_result(Constants.ResultStatus.NG);
//                                                                                voTestItem.setTest_item_info(voTestItem.getTest_result_value());
//                                                                                listItemResultAdapter.addItem(voTestItem);
//                                                                            }
//                                                                        }
//
//                                                                        listItemResultAdapter.updateListAdapter();
//                                                                        */
//
//                                        if (testNgCnt > 0) {
//                                            tvPopupProcessResult.setText(Constants.ResultStatus.NG);
//                                            tvPopupProcessResult.setTextSize(Dimension.SP, Constants.UI.TEXT_SIZE_LARGE_SP);
//                                            tvPopupProcessResultCompValue.setTextSize(Dimension.SP, Constants.UI.TEXT_SIZE_MEDIUM_SP);
//                                            tvPopupProcessResultHeaterValue.setTextSize(Dimension.SP, Constants.UI.TEXT_SIZE_MEDIUM_SP);
//                                        } else {
//                                            tvPopupProcessResult.setText(Constants.ResultStatus.OK);
//                                        }
//                                        // }
//
//                                        Map<String, String> mapTestHistory = new HashMap<>();
//                                        mapTestHistory.put(Constants.JsonKeys.CLM_TEST_RESULT, ((testNgCnt > 0) ? Constants.ResultStatus.NG : Constants.ResultStatus.OK));
//                                        mapTestHistory.put(Constants.JsonKeys.CLM_TEST_NG_COUNT, String.valueOf(testNgCnt));
//                                        mapTestHistory.put(Constants.JsonKeys.CLM_TEST_OK_COUNT, String.valueOf(testOkCnt));
//                                        mapTestHistory.put(Constants.JsonKeys.CLM_TEST_HISTORY_SEQ, lmsTestSeq);
//                                        mapTestHistory.put(Constants.JsonKeys.CLM_TEST_MODEL_ID, arrTestModels[0][0]);
//                                        mapTestHistory.put(Constants.JsonKeys.CLM_TEST_MODEL_NAME, arrTestModels[0][1]);
//                                        mapTestHistory.put(Constants.JsonKeys.CLM_TEST_MODEL_NATIONALITY, arrTestModels[0][2]);
//                                        mapTestHistory.put(Constants.JsonKeys.CLM_TEST_TIMESTAMP, lmsTestTimestamp);
//                                        mapTestHistory.put(Constants.JsonKeys.CLM_TEST_COMP_VALUE, String.valueOf(currentCompWattValue));
//                                        mapTestHistory.put(Constants.JsonKeys.CLM_TEST_HEATER_VALUE, String.valueOf(currentHeaterWattValue));
//                                        mapTestHistory.put(Constants.JsonKeys.CLM_COMMENT, "");
//
//                                        test_info.putString(Constants.SharedPrefKeys.TEST_RESULT, ((testNgCnt > 0) ? Constants.ResultStatus.NG : Constants.ResultStatus.OK));
//                                        // WeakReference를 통한 안전한 Activity 접근
//                                        ActivityModel_0003 activity = getMainActivity();
//                                        if (activity != null) {
//                                            test_info.putString(Constants.SharedPrefKeys.HEATER_VALUE, String.valueOf(activity.currentHeaterWattValueProp));
//                                            test_info.putString(Constants.SharedPrefKeys.COMP_VALUE, String.valueOf(activity.currentCompWattValueProp));
//                                        } else {
//                                            // Activity가 null인 경우 기본값 사용
//                                            test_info.putString(Constants.SharedPrefKeys.HEATER_VALUE, Constants.Common.ZERO);
//                                            test_info.putString(Constants.SharedPrefKeys.COMP_VALUE, Constants.Common.ZERO);
//                                        }
//                                        test_info.commit();
//
//                                        TestData.insertProductTestHistory(getBaseContext(), mapTestHistory);
//
//                                        // ⚠️ 검사 상세 내역 저장
//                                        // 테이블이 없으면 생성
//                                        TestData.createProductTestHistoryDetail(getBaseContext());
//
//                                        String cm0101DgreeInfo = "";
//
//                                        // listItemAdapter의 모든 항목을 상세 내역으로 저장
//                                        for (int i = 0; i < listItemAdapter.getCount(); i++) {
//                                            VoTestItem voTestItem = (VoTestItem) listItemAdapter.getItem(i);
//                                            Map<String, String> mapTestHistoryDetail = new HashMap<>();
//                                            mapTestHistoryDetail.put("clm_test_history_seq", lmsTestSeq);
//                                            mapTestHistoryDetail.put("clm_test_model_id", voTestItem.getTest_model_id());
//                                            mapTestHistoryDetail.put("clm_test_item_seq", voTestItem.getTest_item_seq());
//                                            mapTestHistoryDetail.put("clm_test_item_name", voTestItem.getTest_item_name());
//                                            mapTestHistoryDetail.put("clm_test_item_command", voTestItem.getTest_item_command());
//                                            mapTestHistoryDetail.put("clm_test_item_result", voTestItem.getTest_item_result());
//                                            mapTestHistoryDetail.put("clm_test_item_value", voTestItem.getTest_item_value());
//                                            mapTestHistoryDetail.put("clm_test_response_value", voTestItem.getTest_response_value());
//                                            mapTestHistoryDetail.put("clm_test_result_value", voTestItem.getTest_result_value());
//                                            mapTestHistoryDetail.put("clm_test_temperature", voTestItem.getTest_temperature());
//                                            mapTestHistoryDetail.put("clm_test_electric_val", voTestItem.getTest_electric_val());
//                                            mapTestHistoryDetail.put("clm_test_result_check_value", voTestItem.getTest_result_check_value());
//                                            mapTestHistoryDetail.put("clm_bt_raw_message", voTestItem.getTest_bt_raw_message());
//                                            mapTestHistoryDetail.put("clm_bt_raw_response", voTestItem.getTest_bt_raw_response());
//                                            mapTestHistoryDetail.put("clm_bt_processed_value", voTestItem.getTest_bt_processed_value());
//
//                                            // 상한값/하한값 조회 (lstData에서 해당 test_item_command로 찾기)
//                                            // System.out.println("X.?????????????????> voTestItem.getTest_item_command() " + voTestItem.getTest_item_command() + " lstDataTmp.size() " + lstDataTmp.size());
//                                            String testItemCommand = voTestItem.getTest_item_command();
//                                            String medianValue = "";
//                                            String upperValue = "";
//                                            String lowerValue = "";
//                                            if (lstDataTmp != null && testItemCommand != null) {
//                                                for (Map<String, String> specData : lstDataTmp) {
//                                                    // System.out.println("0.?????????????????> " + testItemCommand + " " + specData.get(Constants.JsonKeys.CLM_TEST_COMMAND));
//                                                    if (testItemCommand.equals(specData.get(Constants.JsonKeys.CLM_TEST_COMMAND))) {
//                                                        // System.out.println("1.?????????????????> " + testItemCommand + " " + specData.get(Constants.JsonKeys.CLM_VALUE_WATT) + " " + specData.get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT) + " " + specData.get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT));
//                                                        // 와트 기준값이 있는 경우 02 값 사용, 없으면 일반 값 사용
//                                                        if (testItemCommand.contains(Constants.TestItemCodes.CM0101)) {
//                                                            if (specData.get(Constants.JsonKeys.CLM_VALUE_WATT) != null && !specData.get(Constants.JsonKeys.CLM_VALUE_WATT).isEmpty()) {
//                                                                upperValue = specData.get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT) != null ? specData.get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT) : "0";
//                                                                lowerValue = specData.get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT) != null ? specData.get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT) : "0";
//                                                                medianValue = specData.get(Constants.JsonKeys.CLM_VALUE_WATT) != null ? specData.get(Constants.JsonKeys.CLM_VALUE_WATT) : "0";
//                                                                upperValue = String.valueOf(Double.parseDouble(upperValue) + Double.parseDouble(medianValue)) + Constants.Common.WATT_UNIT;
//                                                                lowerValue = String.valueOf(Double.parseDouble(medianValue) - Double.parseDouble(lowerValue)) + Constants.Common.WATT_UNIT;
//                                                            } else {
//                                                                upperValue = specData.get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT) != null ? specData.get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT) : "0";
//                                                                lowerValue = specData.get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT) != null ? specData.get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT) : "0";
//                                                                medianValue = specData.get(Constants.JsonKeys.CLM_VALUE_WATT) != null ? specData.get(Constants.JsonKeys.CLM_VALUE_WATT) : "0";
//                                                                upperValue = String.valueOf(Double.parseDouble(upperValue) + Double.parseDouble(medianValue)) + Constants.Common.WATT_UNIT;
//                                                                lowerValue = String.valueOf(Double.parseDouble(medianValue) - Double.parseDouble(lowerValue)) + Constants.Common.WATT_UNIT;
//                                                            }
//                                                        } else if (testItemCommand.contains(Constants.TestItemCodes.HT0101) ||
//                                                                testItemCommand.contains(Constants.TestItemCodes.SV0101) || testItemCommand.contains(Constants.TestItemCodes.SV0201) ||
//                                                                testItemCommand.contains(Constants.TestItemCodes.SV0301) || testItemCommand.contains(Constants.TestItemCodes.SV0401) ||
//                                                                testItemCommand.contains(Constants.TestItemCodes.PM0101)) {
//                                                            if (specData.get(Constants.JsonKeys.CLM_VALUE_WATT) != null && !specData.get(Constants.JsonKeys.CLM_VALUE_WATT).isEmpty()) {
//                                                                upperValue = specData.get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT) != null ? specData.get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT) : "0";
//                                                                lowerValue = specData.get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT) != null ? specData.get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT) : "0";
//                                                                medianValue = specData.get(Constants.JsonKeys.CLM_VALUE_WATT) != null ? specData.get(Constants.JsonKeys.CLM_VALUE_WATT) : "0";
//                                                                upperValue = String.valueOf(Double.parseDouble(upperValue) + Double.parseDouble(medianValue)) + Constants.Common.WATT_UNIT;
//                                                                lowerValue = String.valueOf(Double.parseDouble(medianValue) - Double.parseDouble(lowerValue)) + Constants.Common.WATT_UNIT;
//                                                            } else {
//                                                                upperValue = specData.get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT) != null ? specData.get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT) : "0";
//                                                                lowerValue = specData.get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT) != null ? specData.get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT) : "0";
//                                                                medianValue = specData.get(Constants.JsonKeys.CLM_VALUE_WATT) != null ? specData.get(Constants.JsonKeys.CLM_VALUE_WATT) : "0";
//                                                                upperValue = String.valueOf(Double.parseDouble(upperValue) + Double.parseDouble(medianValue)) + Constants.Common.WATT_UNIT;
//                                                                lowerValue = String.valueOf(Double.parseDouble(medianValue) - Double.parseDouble(lowerValue)) + Constants.Common.WATT_UNIT;
//                                                            }
//                                                        } else if (testItemCommand.contains(Constants.TestItemCodes.TH0101) || testItemCommand.contains(Constants.TestItemCodes.TH0201) ||
//                                                                testItemCommand.contains(Constants.TestItemCodes.TH0301)) {
//                                                            if (specData.get(Constants.JsonKeys.CLM_VALUE) != null && !specData.get(Constants.JsonKeys.CLM_VALUE).isEmpty()) {
//                                                                upperValue = specData.get(Constants.JsonKeys.CLM_UPPER_VALUE) != null ? specData.get(Constants.JsonKeys.CLM_UPPER_VALUE) : "0";
//                                                                lowerValue = specData.get(Constants.JsonKeys.CLM_LOWER_VALUE) != null ? specData.get(Constants.JsonKeys.CLM_LOWER_VALUE) : "0";
//                                                                medianValue = specData.get(Constants.JsonKeys.CLM_VALUE) != null ? specData.get(Constants.JsonKeys.CLM_VALUE) : "0";
//                                                                upperValue = String.valueOf(Double.parseDouble(upperValue) + Double.parseDouble(medianValue)) + Constants.Common.DEGREE_CELSIUS;
//                                                                lowerValue = String.valueOf(Double.parseDouble(medianValue) - Double.parseDouble(lowerValue)) + Constants.Common.DEGREE_CELSIUS;
//                                                            } else {
//                                                                upperValue = specData.get(Constants.JsonKeys.CLM_UPPER_VALUE) != null ? specData.get(Constants.JsonKeys.CLM_UPPER_VALUE) : "0";
//                                                                lowerValue = specData.get(Constants.JsonKeys.CLM_LOWER_VALUE) != null ? specData.get(Constants.JsonKeys.CLM_LOWER_VALUE) : "0";
//                                                                medianValue = specData.get(Constants.JsonKeys.CLM_VALUE) != null ? specData.get(Constants.JsonKeys.CLM_VALUE) : "0";
//                                                                upperValue = String.valueOf(Double.parseDouble(upperValue) + Double.parseDouble(medianValue)) + Constants.Common.DEGREE_CELSIUS;
//                                                                lowerValue = String.valueOf(Double.parseDouble(medianValue) - Double.parseDouble(lowerValue)) + Constants.Common.DEGREE_CELSIUS;
//                                                            }
//                                                        }
//                                                        break;
//                                                    }
//                                                }
//                                            }
//
//                                            if (testItemCommand.contains(Constants.TestItemCodes.HT0101) ||
//                                                    testItemCommand.contains(Constants.TestItemCodes.SV0101) || testItemCommand.contains(Constants.TestItemCodes.SV0201) ||
//                                                    testItemCommand.contains(Constants.TestItemCodes.SV0301) || testItemCommand.contains(Constants.TestItemCodes.SV0401) ||
//                                                    testItemCommand.contains(Constants.TestItemCodes.PM0101) ||
//                                                    testItemCommand.contains(Constants.TestItemCodes.TH0101) || testItemCommand.contains(Constants.TestItemCodes.TH0201) ||
//                                                    testItemCommand.contains(Constants.TestItemCodes.TH0301)) {
//                                                mapTestHistoryDetail.put("clm_test_item_info", lowerValue + " < " + voTestItem.getTest_item_info() + " < " + upperValue);
//                                            } else if (testItemCommand.contains(Constants.TestItemCodes.CM0101)) {
//                                                mapTestHistoryDetail.put("clm_test_item_info", lowerValue + " < " + voTestItem.getTest_result_check_value() + " < " + upperValue);
//                                                cm0101DgreeInfo = voTestItem.getTest_item_info();
//                                            } else if (testItemCommand.contains(Constants.TestItemCodes.CM0100)) {
//                                                mapTestHistoryDetail.put("clm_test_item_info", cm0101DgreeInfo);
//                                            } else {
//                                                mapTestHistoryDetail.put("clm_test_item_info", voTestItem.getTest_item_info());
//                                            }
//                                            mapTestHistoryDetail.put("clm_test_upper_value", upperValue);
//                                            mapTestHistoryDetail.put("clm_test_lower_value", lowerValue);
//                                            mapTestHistoryDetail.put("clm_comment", "");
//
//                                            if (testItemCommand.equals(Constants.TestItemCodes.RE0101)) {
//                                                mapTestHistoryDetail.put("clm_test_item_result", "OK");
//                                                mapTestHistoryDetail.put("clm_test_result_check_value", "00");
//                                            }
//                                            TestData.insertProductTestHistoryDetail(getBaseContext(), mapTestHistoryDetail);
//                                        }
//
//                                        processFinished = true;
//                                        clTestResult.setVisibility(VISIBLE);
//                                        tvTestOkCnt.setText(String.valueOf(testOkCnt));
//                                        tvTestNgCnt.setText(String.valueOf(testNgCnt));
//
//                                        // 서버로 결과 전달
//                                        // Phase 3: Timer 중복 생성 방지 - synchronized 블록으로 동시성 제어
//                                        synchronized (finishedRestartTimerLock) {
//                                            // 이미 실행 중이면 중복 시작 방지
//                                            if (finishedRestartTimerRunning.get()) {
//                                                logWarn(LogCategory.PS, "Finished restart timer already running, skipping start");
//                                                return;
//                                            }
//
//                                            // 기존 Timer 인스턴스가 있으면 먼저 정리 (안전성 향상)
//                                            if (tmrFinishedRestart != null) {
//                                                tmrFinishedRestart.cancel();
//                                                tmrFinishedRestart.purge();
//                                                tmrFinishedRestart = null;
//                                            }
//                                            if (ttFinishedRestart != null) {
//                                                ttFinishedRestart.cancel();
//                                                ttFinishedRestart = null;
//                                            }
//
//                                            try {
//                                                tmrFinishedRestart = new Timer();
//                                                ttFinishedRestart = new TimerTask() {
//                                                    @Override
//                                                    public void run() {
//                                                        // logInfo(LogCategory.PS, String.format(Constants.LogMessages.FINISHED_RESTART_APP_TERMINATION + Constants.Common.EMPTY_STRING + "[%d / %d]", restartCntFinished, restartCntMargin));
//                                                        if (restartCntFinished == restartCntMargin) {
//                                                            restartCntFinished = 0;
//                                                            usbReconnectAttempts = 0;
//                                                            finishedCorrectly = true;
//                                                            // finishApplication(getApplicationContext());
//                                                            resetBluetoothSessionKeepUsb();
//                                                            if (tmrFinishedRestart != null) {
//                                                                tmrFinishedRestart.cancel();
//                                                            }
//                                                        }
//                                                        restartCntFinished++;
//                                                    }
//                                                };
//                                                tmrFinishedRestart.schedule(ttFinishedRestart, 0, Constants.Timeouts.TIMER_INTERVAL_MS);
//                                                finishedRestartTimerRunning.set(true);
//                                            } catch (Exception e) {
//                                                finishedRestartTimerRunning.set(false);
//                                                logError(LogCategory.ER, "Error starting finished restart timer", e);
//                                            }
//                                        }
//                                    });
//                                } catch (Exception e) {
//                                    logError(LogCategory.ER, Constants.ErrorMessages.THREAD_EXECUTION_ERROR_IN_RE0101, e);
//                                }
//                                // }
//                                // });
//
//                                // processFinishedYn = true;
//                            }
//                        } else {
//                            processFinished = false;
//                        }
//
//                        // logInfo(LogCategory.PS, ">>>>>> 2.receiveCommand " + receiveCommand);
//                        // receiveCommand = "";
//
//                        // Fallback: cached watt values for commands that skipped UI updates this cycle
//                        if (finalReceiveCommand.equals(Constants.TestItemCodes.CM0101) || finalReceiveCommand.equals(Constants.TestItemCodes.CM0102)) {
//                            updateCompWatt = ensureWattText(Constants.TestItemCodes.CM0101, updateCompWatt);
//                        }
//                        if (finalReceiveCommand.equals(Constants.TestItemCodes.HT0101)) {
//                            updateHeaterWatt = ensureWattText(Constants.TestItemCodes.HT0101, updateHeaterWatt);
//                        }
//                        if (finalReceiveCommand.equals(Constants.TestItemCodes.PM0101)) {
//                            updatePumpWatt = ensureWattText(Constants.TestItemCodes.PM0101, updatePumpWatt);
//                        }
//                        if (isSolValveCommand(finalReceiveCommand)) {
//                            updateItemInfo = ensureWattText(finalReceiveCommand, updateItemInfo);
//                        }
//
//                        UiUpdateBundle.Builder uiBuilder = new UiUpdateBundle.Builder()
//                                .setDialogVisible(shouldUpdateDialog && !shouldHideDialog)
//                                .setDialogHidden(shouldHideDialog)
//                                .setDialogColor(dialogColor)
//                                .setDialogMessage(dialogMessage)
//                                .setTemperatureText(updateTemperature)
//                                .setCompWattText(updateCompWatt)
//                                .setHeaterWattText(updateHeaterWatt)
//                                .setPumpWattText(updatePumpWatt)
//                                .setLogText(updateLogText)
//                                .setUpdateItemCommand(updateItemCommand)
//                                .setUpdateItemResult(updateItemResult)
//                                .setUpdateItemCheckValue(updateItemCheckValue)
//                                .setUpdateItemInfo(updateItemInfo)
//                                .setUpdateItemNameSuffix(updateItemNameSuffix)
//                                .setUpdateListAdapter(shouldUpdateListAdapter)
//                                .setFinalReceiveCommandResponse(finalReceiveCommandResponse)
//                                .setFinalCalculatedResultValue(calculatedResultValue)
//                                .setFinalReadMessage(finalReadMessage)
//                                .setTemperatureValueCompDiff(temperatureValueCompDiff)
//                                .setResultInfo(resultInfo)
//                                .setDecTemperatureHotValue(decTemperatureHotValue)
//                                .setDecTemperatureColdValue(decTemperatureColdValue)
//                                .setFinalCurrentTestItem(finalCurrentTestItem)
//                                .setTestItemIdx(testItemIdx)
//                                .setTestOkCnt(testOkCnt)
//                                .setTestNgCnt(testNgCnt)
//                                .setReceiveCommandResponseOK(receiveCommandResponseOK)
//                                .setShouldUpdateCounts(true)
//                                .setListItemAdapter(listItemAdapter);
//
//                        UiUpdateBundle uiBundle = uiBuilder.build();
//                        // Phase 2: 단순 UI 업데이트를 scheduleUiUpdate로 변경
//                        scheduleUiUpdate(() -> applyUiBundle(uiBundle));
//
//                    } catch (Exception e) {
//                        logError(LogCategory.ER, Constants.ErrorMessages.ERROR_IN_BACKGROUND_PROCESSING, e);
//                        // 복잡한 UI 업데이트 블록 통합: scheduleUiUpdate 사용
//                        scheduleUiUpdate(() -> {
//                            sendBtMessage(finalCurrentTestItem);
//                        });
//                    }
//                    // }); // 백그라운드 스레드 종료
//                } catch (Exception e) {
//                    logError(LogCategory.ER, Constants.ErrorMessages.BLUETOOTH_MESSAGE_HANDLING_ERROR, e);
//                    // 복잡한 UI 업데이트 블록 통합: scheduleUiUpdate 사용
//                    scheduleUiUpdate(() -> {
//                        sendBtMessage(finalCurrentTestItem);
//                    });
//                }
//                // });
//            }
//        } catch (Exception e) {
//            logError(LogCategory.ER, Constants.ErrorMessages.BLUETOOTH_HANDLER_ERROR, e);
//        }
//        // }
////        if (raw == null || raw.length == 0) {
////            return;
////        }
////
////        final String readMessage;
////        try {
////            readMessage = new String(raw, StandardCharsets.UTF_8);
////        } catch (Exception e) {
////            logError(LogCategory.BT, "Failed to decode BT message", e);
////            return;
////        }
////
////        if (arrTestItems == null || listItemAdapter == null) {
////            logWarn(LogCategory.BT, "arrTestItems or listItemAdapter not initialized yet, skipping message processing");
////            return;
////        }
////
////        if (testItemIdx > arrTestItems.length - 1) {
////            testItemIdx = arrTestItems.length - 1;
////        }
////
////        if (readMessage.contains(Constants.TestItemCodes.ST0101)) {
////            testItemIdx = 0;
////            if (listItemAdapter.getCount() > testItemIdx) {
////                VoTestItem firstItem = (VoTestItem) listItemAdapter.getItem(testItemIdx);
////                firstItem.setTest_item_result(Constants.ResultStatus.OK);
////                firstItem.setTest_result_value(Constants.ResultStatus.COMP_AGING_RESPONSE_01 + Constants.Common.LOGGER_DEVIDER_01 + Constants.ResultStatus.COMP_AGING_RESPONSE_01);
////                firstItem.setTest_finish_yn(Constants.ResultStatus.YES);
////            } else {
////                logWarn(LogCategory.BT, "listItemAdapter not ready, cannot update test item result");
////            }
////            if (testProcessId.equals(Constants.Common.EMPTY_STRING)) {
////                testProcessId = getCurrentDatetime(timestampFormat);
////                logInfo(LogCategory.PS, Constants.LogMessages.TEST_PROCESS_ID_STARTED + testProcessId);
////            }
////            entranceCheck = true;
////            ensureUnfinishedRestartTimer();
////
////            scheduleUiUpdate(() -> {
////                clAlert.setVisibility(View.GONE);
////                tvCurrentProcess.setText(((VoTestItem) listItemAdapter.getItem(testItemIdx)).getTest_item_name());
////                tvTestOkCnt.setText(String.valueOf(testOkCnt));
////                tvTestNgCnt.setText(String.valueOf(testNgCnt));
////                lvTestItem.smoothScrollToPosition(testItemIdx);
////            });
////        }
////
////        // 공통 카운터 및 UI 텍스트 갱신
////        testItemCounter++;
////        testTotalCounter++;
////        scheduleUiUpdate(() -> tvEllapsedTimeCnt.setText(String.valueOf(testTotalCounter)));
////
////        // wattTemp 기록
////        wattTemp = new String[]{getCurrentDatetime(timestampFormat), calculatedTemperatureTmp, String.valueOf(decElectricValue), currentTestItem};
////        lstMapWattTemp.add(wattTemp);
////
////        receivedMessageCnt++;
////        String currentProcessName = ((VoTestItem) listItemAdapter.getItem(testItemIdx)).getTest_item_name();
////        scheduleUiUpdate(() -> {
////            tvCurrentProcess.setText(currentProcessName);
////            tvRunWsRamp.setText(receivedMessageCnt + " / " + sendingMessageCnt);
////        });
////
////        // ===== 여기서부터는 기존 runOnBtWorker 블록의 처리 내용 =====
////        if (lstSpecData == null || lstSpecData.isEmpty()) {
////            return;
////        }
////
////        String finalReceiveCommand = currentTestItem;
////        String finalReceiveCommandResponse = readMessage;
////        String finalReadMessage = readMessage;
////
////        String calculatedReceiveResponseResult = Constants.Common.EMPTY_STRING;
////        String calculatedResultValue = Constants.Common.EMPTY_STRING;
////        String calculatedDecTemperature = Constants.Common.EMPTY_STRING;
////        int calculatedDecTemperatureValue = 0;
////        String calculatedReceiveCommandResponse = Constants.Common.EMPTY_STRING;
////        double calculatedDblValTemp = 0.0;
////        int calculatedWattLower = 0;
////        int calculatedWattUpper = 0;
////        String calculatedReceiveCompAgingResponse = Constants.Common.EMPTY_STRING;
////        String calculatedReceiveResponseResultTxt = Constants.Common.EMPTY_STRING;
////        String calculatedResultInfo = Constants.Common.EMPTY_STRING;
////        boolean shouldUpdateDialog = false;
////        int dialogColor = 0;
////        String dialogMessage = "";
////        boolean shouldHideDialog = false;
////        String calculatedTemperatureValueCompDiff = "";
////
////        String updateTemperature = null;
////        String updateCompWatt = null;
////        String updateHeaterWatt = null;
////        String updatePumpWatt = null;
////        String updateLogText = null;
////        boolean shouldUpdateListAdapter = false;
////        String updateItemCommand = "";
////        String updateItemResult = "";
////        String updateItemCheckValue = "";
////        String updateItemInfo = "";
////        String updateItemNameSuffix = "";
////
////        List<Map<String, String>> temperatureData;
////
////        // ---- 이하 내용은 기존 runOnBtWorker 내부 로직을 그대로 이동 (온도/소비전력 분기, UI 업데이트 준비 등) ----
////        // (파일 상의 기존 코드 블록: TH0101/TH0201 온도 계산, CM0101/HT0101/PM0101/SVxxxx 소비전력 처리,
////        //  logBtTestResponse 호출, updateItemXXX 값 세팅, UiUpdateBundle 생성 등)
////        // 해당 블록을 그대로 이 위치로 옮기되, UI 접근은 모두 scheduleUiUpdate()로 감싸 주세요.
////
////        // 예시: 최종 UiUpdateBundle 빌드 및 UI 반영
////        UiUpdateBundle uiBundle = new UiUpdateBundle.Builder()
////                .setDialogVisible(shouldUpdateDialog && !shouldHideDialog)
////                .setDialogHidden(shouldHideDialog)
////                .setDialogColor(dialogColor)
////                .setDialogMessage(dialogMessage)
////                .setTemperatureText(updateTemperature)
////                .setCompWattText(updateCompWatt)
////                .setHeaterWattText(updateHeaterWatt)
////                .setPumpWattText(updatePumpWatt)
////                .setLogText(updateLogText)
////                .setUpdateItemCommand(updateItemCommand)
////                .setUpdateItemResult(updateItemResult)
////                .setUpdateItemCheckValue(updateItemCheckValue)
////                .setUpdateItemInfo(updateItemInfo)
////                .setUpdateItemNameSuffix(updateItemNameSuffix)
////                .setUpdateListAdapter(shouldUpdateListAdapter)
////                .setFinalReceiveCommandResponse(finalReceiveCommandResponse)
////                .setFinalCalculatedResultValue(calculatedResultValue)
////                .setFinalReadMessage(finalReadMessage)
////                .setTemperatureValueCompDiff(temperatureValueCompDiff)
////                .setResultInfo(resultInfo)
////                .setDecTemperatureHotValue(decTemperatureHotValue)
////                .setDecTemperatureColdValue(decTemperatureColdValue)
////                .setFinalCurrentTestItem(finalReceiveCommand)
////                .setTestItemIdx(testItemIdx)
////                .setTestOkCnt(testOkCnt)
////                .setTestNgCnt(testNgCnt)
////                .setReceiveCommandResponseOK(receiveCommandResponseOK)
////                .setShouldUpdateCounts(true)
////                .setListItemAdapter(listItemAdapter)
////                .build();
////
////        scheduleUiUpdate(() -> applyUiBundle(uiBundle));
//    }
//
////    private boolean processFinished = false;
////    private boolean processFinishedTmp = false;
//
//    private void initializeAppStateAsync() {
//        try {
//            Intent intent = getIntent();
//            globalModelId = intent.getStringExtra(Constants.IntentExtras.TEST_MODEL_ID);
//            // globalModelId = intent.getStringExtra("00000002");
//            globalModelName = intent.getStringExtra(Constants.IntentExtras.TEST_MODEL_NAME);
//            globalModelNation = intent.getStringExtra(Constants.IntentExtras.TEST_MODEL_NATION);
//            globalLastTestStartTimestamp = intent.getStringExtra(Constants.IntentExtras.TEST_START_DATETIME);
//            if (globalLastTestStartTimestamp == null) {
//                globalLastTestStartTimestamp = lmsTestSeq;
//            }
//
//            SharedPreferences testCookie = getSharedPreferences(Constants.SharedPrefKeys.TEST_COOKIE_INFO, MODE_PRIVATE);
//            String lastStartFromCookie = testCookie.getString(Constants.SharedPrefKeys.TEST_START_DATETIME, Constants.Common.EMPTY_STRING);
//            if (!TextUtils.isEmpty(lastStartFromCookie)) {
//                globalLastTestStartTimestamp = lastStartFromCookie;
//            }
//
//            // logInfo(LogCategory.SI, "globalLastTestStartTimestamp:" + globalLastTestStartTimestamp + " lmsTestSeq:" + lmsTestSeq);
//            String reqDateStr = (globalLastTestStartTimestamp == null) ? lmsTestSeq : globalLastTestStartTimestamp;
//            min_diff = minDiff(reqDateStr);
//            btInfoList = new ArrayList<>();
//
//            ActivityModel_0003 activity = getMainActivity();
//            if (blnTestYn && activity != null && !activity.isFinishing()) {
//                activity.scheduleUiUpdate(() -> {
//                    clDialogForLogger.setVisibility(VISIBLE);
//                    tvDialogForLoggerWatt.setVisibility(VISIBLE);
//                    tvDialogForLoggerTemp.setVisibility(VISIBLE);
//                });
//                testLogger = Constants.Common.EMPTY_STRING;
//                testLoggerTemp = Constants.Common.EMPTY_STRING;
//            }
//
//            scheduleUiUpdate(() -> {
//                tvModelNationality.setText(globalModelNation);
//                tvModelName.setText(globalModelName);
//                tvModelId.setText(globalModelId);
//                clAlert.setVisibility(GONE);
//                clDialogForPreprocess.setVisibility(GONE);
//            });
//
//            postTestInfoColor = getBaseContext().getResources().getColor(R.color.gray_04);
//            preferences = getSharedPreferences(Constants.SharedPrefKeys.TEST_INFO, MODE_PRIVATE);
//            test_info = preferences.edit();
//
//            loadTemperatureData();
//
//            // logInfo(LogCategory.PS, "globalModelId: " + globalModelId);
//            // logInfo(LogCategory.PS, "globalModelName: " + globalModelName);
//            // logInfo(LogCategory.PS, "globalModelNation: " + globalModelNation);
//
//            serverDomain = Constants.ServerConfig.SERVER_DOMAIN_192_168_0;
//            serverIp = Constants.ServerConfig.SERVER_IP_192_168_0_47;
//            serverDomain = Constants.ServerConfig.SERVER_DOMAIN_172_16_1;
//            serverIp = Constants.ServerConfig.SERVER_IP_172_16_1_249_8080;
//            if (modeType.equals(Constants.ResultStatus.MODE_TYPE_TEST)) {
//                serverIp = Constants.ServerConfig.SERVER_IP_PORT_ITF;
//            } else {
//                serverIp = Constants.ServerConfig.SERVER_IP_PORT;
//            }
//
//            globalModelId = "00000002";
//            urlStr = Constants.URLs.HTTP_PROTOCOL + serverIp + Constants.URLs.ENDPOINT_TEST_INFO_LIST
//                    + Constants.Common.QUESTION + Constants.URLs.PARAM_CALL_TYPE + Constants.Common.AMPERSAND
//                    + Constants.URLs.PARAM_MODEL_ID + globalModelId;
//            // logInfo(LogCategory.SI, Constants.LogMessages.SETTING_SERVER_URL + urlStr);
//
//            Path directoryPath = Paths.get(Constants.FilePaths.FOLDER_NAME);
//            try {
//                Files.createDirectories(directoryPath);
//                if (Files.exists(directoryPath)) {
//                    // logInfo(LogCategory.PS, Constants.LogMessages.DIRECTORY_READY + directoryPath);
//                }
//            } catch (IOException e) {
//                logError(LogCategory.ER, Constants.ErrorMessages.DIRECTORY_CREATION_IO_EXCEPTION, e);
//            }
//
//            ipAddress = getLocalIpAddress();
//            if (!TextUtils.isEmpty(ipAddress)) {
//                unit_no = Integer.parseInt(ipAddress.split("\\.")[3]) - Constants.Network.IP_ADDRESS_OFFSET;
//            }
//            urlStrBarcode = Constants.URLs.HTTP_PROTOCOL + serverIp + Constants.URLs.ENDPOINT_PRODUCT_SERIAL_INFO_LIST
//                    + Constants.Common.QUESTION + Constants.URLs.PARAM_UNIT_NO + String.valueOf(unit_no).replace("-", "*")
//                    + Constants.Common.AMPERSAND + Constants.URLs.PARAM_MODEL_ID + globalModelId;
//
//            final int finalUnitNo = unit_no;
//            scheduleUiUpdate(() -> {
//                if (!TextUtils.isEmpty(ipAddress)) {
//                    tvRunWsRamp.setText(String.valueOf(finalUnitNo));
//                    tvRunWsRamp.setBackgroundColor(getBaseContext().getResources().getColor(R.color.blue_01));
//                } else {
//                    // tvRunWsRamp.setText(Constants.Common.EMPTY_STRING);
//                    tvRunWsRamp.setBackgroundColor(getBaseContext().getResources().getColor(R.color.red_01));
//                }
//            });
//            // logInfo(LogCategory.SI, Constants.LogMessages.GET_WEB_IP_INFO + ipAddress);
//
//            SharedPreferences testPrefs = getSharedPreferences(Constants.SharedPrefKeys.TEST_INFO, MODE_PRIVATE);
//            String testResult = testPrefs.getString(Constants.SharedPrefKeys.TEST_RESULT, Constants.Common.EMPTY_STRING);
//            String heaterValue = testPrefs.getString(Constants.SharedPrefKeys.HEATER_VALUE, Constants.Common.EMPTY_STRING);
//            String compValue = testPrefs.getString(Constants.SharedPrefKeys.COMP_VALUE, Constants.Common.EMPTY_STRING);
//            scheduleUiUpdate(() -> {
//                tvUnitId.setText(Constants.Common.UNIT_ID_PREFIX + finalUnitNo);
//                if (TextUtils.isEmpty(testResult)) {
//                    clTestResult.setVisibility(GONE);
//                } else {
//                    clTestResult.setVisibility(VISIBLE);
//                    tvPopupProcessResult.setText(testResult);
//                    tvPopupProcessResultCompValue.setText(compValue);
//                    tvPopupProcessResultHeaterValue.setText(heaterValue);
//                }
//            });
//
//            // logInfo(LogCategory.SI, Constants.LogMessages.WEBSERVER_STARTED);
//
//            if (usbService == null || !UsbService.SERVICE_CONNECTED || !usbConnPermissionGranted) {
//                scheduleUiUpdate(this::restartUsbMonitoring);
//                updateUsbLampDisconnected();
//                scheduleUsbReconnect(true);
//            } else if (!isUsbReady()) {
//                updateUsbLampReconnecting();
//                scheduleUsbReconnect(true);
//            } else {
//                updateUsbLampReady();
//            }
//            scheduleUiUpdate(this::bluetoothOn);
//
//            loadTestSpecData();
//        } catch (Exception e) {
//            logError(LogCategory.ER, "Error during async initialization", e);
//            scheduleUiUpdate(() -> {
//                clAlert.setVisibility(VISIBLE);
//                tvAlertMessage.setText(Constants.UIMessages.SERVER_CONNECTION_FAILED);
//            });
//        }
//    }
//
//    private void loadTemperatureData() {
//        try {
//            // logInfo(LogCategory.SI, Constants.LogMessages.READ_SETTING_INFO);
//            hotTemperatureData = TestData.selectTemperatureData(this, Constants.Database.TEMPERATURE_TYPE_HOT);
//            coldTemperatureData = TestData.selectTemperatureData(this, Constants.Database.TEMPERATURE_TYPE_COLD);
//            if (hotTemperatureData.isEmpty() || coldTemperatureData.isEmpty()) {
//                readTemperatureExcel(Constants.Database.TEMPERATURE_TYPE_HOT, Constants.FilePaths.TEMPERATURE_INFO_XLS);
//                readTemperatureExcel(Constants.Database.TEMPERATURE_TYPE_COLD, Constants.FilePaths.TEMPERATURE_INFO_XLS);
//                // logInfo(LogCategory.SI, Constants.LogMessages.INSERT_TEMP_INFO);
//                hotTemperatureData = TestData.selectTemperatureData(this, Constants.Database.TEMPERATURE_TYPE_HOT);
//                coldTemperatureData = TestData.selectTemperatureData(this, Constants.Database.TEMPERATURE_TYPE_COLD);
//            }
//        } catch (Exception e) {
//            logError(LogCategory.ER, Constants.ErrorMessages.TEMPERATURE_DATA_READ_ERROR, e);
//        }
//    }
//
//    private void loadTestSpecData() {
//        AtomicBoolean specDataLoadedFromCache = new AtomicBoolean(false);
//        try {
//            lstSpecData = TestData.selectTestSpecData(getBaseContext(),
//                    Constants.Database.QUERY_AND_1_EQUALS_1 + Constants.Common.SPACE + Constants.Database.QUERY_AND + Constants.Common.SPACE +
//                            Constants.JsonKeys.CLM_MODEL_ID + Constants.Common.EQUAL + Constants.Common.SINGLE_QUETATION + globalModelId + Constants.Common.SINGLE_QUETATION);
//            if (lstSpecData != null && !lstSpecData.isEmpty()) {
//                // if (BuildConfig.DEBUG) {
//                for (int i = 0; i < lstSpecData.size(); i++) {
//                    logDebug(LogCategory.PS, String.format("Test spec data [%d]: %s", i, lstSpecData.get(i)));
//                    if (i == 0) {
//                        test_version_id = lstSpecData.get(i).get("clm_test_version_id");
//                        model_id = lstSpecData.get(i).get("clm_model_id");
//                    }
//                }
//                // }
//                scheduleUiUpdate(() -> {
//                    try {
//                        specDataLoadedFromCache.set(applyTestSpecData(lstSpecData, false));
//                        lstDataTmp = lstSpecData;
//                        clearHttpHandlerQueue();
//                        new ActivityModel_0003.RequestThreadAsync().execute();
//                        if (!specDataLoadedFromCache.get()) {
////                            try {
////                                clearHttpHandlerQueue();
////                                new ActivityModel_0003.RequestThreadAsync().execute();
////                            } catch (Exception e) {
////                                logError(LogCategory.ER, Constants.ErrorMessages.SERVER_CONNECTION_ERROR, e);
////                                clAlert.setVisibility(VISIBLE);
////                                tvAlertMessage.setText(Constants.UIMessages.SERVER_CONNECTION_FAILED);
////                            }
//                        } else if (modeType.equals(Constants.ResultStatus.MODE_TYPE_TEST)) {
//                            String localVersionId = null;
//                            String localModelVersion = null;
//                            if (arrTestModels != null && arrTestModels[0] != null && arrTestModels[0].length > 4) {
//                                localVersionId = arrTestModels[0][3];
//                                localModelVersion = arrTestModels[0][4];
//                            }
//                            new ActivityModel_0003.RequestVersionInfoThreadAsync(localVersionId, localModelVersion).execute();
//                        } else {
//                            try {
//                                clearHttpHandlerQueue();
//                                new ActivityModel_0003.RequestThreadAsync().execute();
//                            } catch (Exception e) {
//                                logError(LogCategory.ER, Constants.ErrorMessages.SERVER_CONNECTION_ERROR, e);
//                                clAlert.setVisibility(VISIBLE);
//                                tvAlertMessage.setText(Constants.UIMessages.SERVER_CONNECTION_FAILED);
//                            }
//                        }
//                    } catch (Exception e) {
//                        logWarn(LogCategory.ER, Constants.ErrorMessages.LIST_SPEC_DATA_ERROR + e.getMessage());
//                    }
//                });
//            } else {
//                scheduleUiUpdate(() -> {
//                    try {
//                        clearHttpHandlerQueue();
//                        new ActivityModel_0003.RequestThreadAsync().execute();
//                    } catch (Exception e) {
//                        logError(LogCategory.ER, Constants.ErrorMessages.SERVER_CONNECTION_ERROR, e);
//                        clAlert.setVisibility(VISIBLE);
//                        tvAlertMessage.setText(Constants.UIMessages.SERVER_CONNECTION_FAILED);
//                    }
//                });
//            }
//        } catch (Exception e) {
//            logWarn(LogCategory.ER, Constants.ErrorMessages.LIST_SPEC_DATA_ERROR + e.getMessage());
//            scheduleUiUpdate(() -> {
//                try {
//                    clearHttpHandlerQueue();
//                    new ActivityModel_0003.RequestThreadAsync().execute();
//                } catch (Exception ex) {
//                    logError(LogCategory.ER, Constants.ErrorMessages.SERVER_CONNECTION_ERROR, ex);
//                }
//            });
//        }
//    }
//
//    /**
//     * 워커 스레드에서 HTTP 전송 및 후속 DB 업데이트 수행
//     */
//    private void executeResultUpload(String checkValue, String lmsSeq) {
//        String httpSuccessYn = Constants.ResultStatus.NO;
//        String targetUrl = Constants.URLs.HTTP_PROTOCOL + serverIp + Constants.URLs.ENDPOINT_UPDATE_RESULT_TEST_INFO
//                + Constants.Common.QUESTION
//                + Constants.URLs.PARAM_PRODUCT_SERIAL_NO + productSerialNo
//                + Constants.Common.AMPERSAND + Constants.URLs.PARAM_TEST_PROCESS + 3
//                + Constants.Common.AMPERSAND + Constants.URLs.PARAM_TEST_RESULT + ((testNgCnt > 0) ? Constants.ResultStatus.NG : Constants.ResultStatus.OK)
//                + checkValue;
//
//        // logInfo(LogCategory.BI, Constants.LogMessages.TARGET_URL + " >>>>>>>>>>>>>>> " + targetUrl);
//
//        boolean requestSuccess = false;
//        for (int attempt = 1; attempt <= Constants.Network.HTTP_RETRY_COUNT; attempt++) {
//            HttpURLConnection connection = null;
//            try {
//                // logInfo(LogCategory.BI, "HTTP 요청 시도 [" + attempt + "/" + Constants.Network.HTTP_RETRY_COUNT + "]");
//                URL url = new URL(targetUrl);
//                connection = (HttpURLConnection) url.openConnection();
//                connection.setReadTimeout(Constants.Timeouts.HTTP_READ_TIMEOUT_MS);
//                connection.setConnectTimeout(Constants.Timeouts.HTTP_CONNECT_TIMEOUT_MS);
//                connection.setRequestMethod(Constants.HTTP.METHOD_GET);
//                connection.setDoInput(true);
//                connection.setDoOutput(true);
//
//                int responseCode = connection.getResponseCode();
//                if (responseCode == HttpURLConnection.HTTP_OK) {
//                    // logInfo(LogCategory.BI, Constants.LogMessages.HTTP_OK_SUCCESS + " (시도 " + attempt + ")");
//                    sendResultYn = Constants.ResultStatus.YES;
//                    httpSuccessYn = Constants.ResultStatus.YES;
//                    requestSuccess = true;
//                    break;
//                } else {
//                    logWarn(LogCategory.BI, Constants.LogMessages.HTTP_OK_FAILED + " (시도 " + attempt + ", code=" + responseCode + ")");
//                    if (attempt < Constants.Network.HTTP_RETRY_COUNT) {
//                        Thread.sleep(Constants.Network.HTTP_RETRY_DELAY_MS);
//                    }
//                }
//            } catch (Exception e) {
//                logError(LogCategory.ER, Constants.ErrorMessages.HTTP_RESULT_UPDATE_ERROR + " (시도 " + attempt + "): " + e.getMessage(), e);
//                if (attempt < Constants.Network.HTTP_RETRY_COUNT) {
//                    try {
//                        Thread.sleep(Constants.Network.HTTP_RETRY_DELAY_MS);
//                    } catch (InterruptedException ie) {
//                        Thread.currentThread().interrupt();
//                        logError(LogCategory.ER, "HTTP 재시도 대기 중 인터럽트 발생", ie);
//                    }
//                }
//            } finally {
//                safeDisconnectConnection(connection);
//            }
//        }
//
//        if (!requestSuccess) {
//            // logInfo(LogCategory.PS, "HTTP 요청 실패: " + Constants.Network.HTTP_RETRY_COUNT + "번 시도 모두 실패");
//        }
//
//        try {
//            TestData.updateProductTestHistoryHttpSuccess(getBaseContext(), lmsSeq, httpSuccessYn);
//        } catch (Exception e) {
//            logError(LogCategory.ER, "Error updating HTTP success status in test history", e);
//        }
//
//        try {
//            for (String[] wattTempData : lstMapWattTemp) {
//                if (wattTempData != null && wattTempData.length >= 4) {
//                    TestData.insertTestHistoryLinearData(
//                            getBaseContext(),
//                            lmsSeq,
//                            wattTempData[3],
//                            wattTempData[0],
//                            wattTempData[1],
//                            wattTempData[2]
//                    );
//                }
//            }
//        } catch (Exception e) {
//            logError(LogCategory.ER, "Error saving linear data to test history", e);
//        }
//    }
//
//    // USB 장치 재탐색 및 권한 요청
//    private void usbSearch() {
//        try {
//            final String ACTION_USB_PERMISSION = "itf.com.app.USB_PERMISSION";
//            UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
//            if (usbManager == null) {
//                logWarn(LogCategory.US, "UsbManager is null");
//                return;
//            }
//
//            HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
//            if (deviceList == null || deviceList.isEmpty()) {
//                // logInfo(LogCategory.US, "No USB devices found");
//                return;
//            }
//
//            for (UsbDevice device : deviceList.values()) {
//                // logInfo(LogCategory.US, "USB device found: " + device.getDeviceName() + " vid=" + device.getVendorId() + " pid=" + device.getProductId());
//
//                if (!usbManager.hasPermission(device)) {
//                    Intent intent = new Intent(ACTION_USB_PERMISSION);
//                    PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
//                    usbManager.requestPermission(device, pendingIntent);
//                    // logInfo(LogCategory.US, "Requested USB permission for device");
//                } else {
//                    // logInfo(LogCategory.US, "Already has permission for device");
//                    if (usbService != null) {
//                        // logInfo(LogCategory.US, "usbService present - consider reopening device via service");
//                        // usbService에 장치 연결 메소드가 있으면 호출 (메소드명/시그니처에 맞게 수정)
//                        // 예: usbService.openDevice(device, usbManager);
//                    } else {
//                        Intent usbIntent = new Intent(getApplicationContext(), UsbService.class);
//                        try {
//                            stopService(usbIntent);
//                        } catch (Exception ignored) {
//                        }
//                        try {
//                            startService(usbIntent);
//                        } catch (Exception e) {
//                            logWarn(LogCategory.US, "Start UsbService failed: " + e.getMessage());
//                        }
//                        // logInfo(LogCategory.US, "UsbService start requested");
//                    }
//                }
//                break; // 첫 번째 장치만 처리(필요 시 제거)
//            }
//        } catch (Exception e) {
//            logError(LogCategory.ER, "usbSearch error", e);
//        }
//    }
//
//    private int minDiff(String reqDateStr) throws ParseException {
//        Date curDate = new Date();
//        SimpleDateFormat dateFormat = new SimpleDateFormat(timestampFormat);
//        Date reqDate = dateFormat.parse(reqDateStr);
//        long reqDateTime = reqDate.getTime();
//        curDate = dateFormat.parse(dateFormat.format(curDate));
//        long curDateTime = curDate.getTime();
//        int minute = (int) ((curDateTime - reqDateTime) / 60000);
//        // logInfo(LogCategory.PS, Constants.LogMessages.REQUEST_TIME + reqDate);
//        // logInfo(LogCategory.PS, Constants.LogMessages.CURRENT_TIME + curDate);
//        // logInfo(LogCategory.PS, minute + Constants.LogMessages.MINUTE_DIFFERENCE);
//        return minute;
//    }
//
//    private void finishApplication(Context mContext) {
//        new Thread(() -> {
//            try {
//                receiveCompAgingResponse = Constants.ResultStatus.COMP_AGING_RESPONSE_01;
//                testTotalCounter = 0;
//                testItemCounter = 0;
//                testItemIdx = 0;
//                entranceCheck = false;
//                btSearchOnOff = false; // 재탐색을 위해 플래그 초기화
//                clearBluetoothReconnect();
//                // 기존 BT 소켓 안전 종료
//                if (btSocket != null && btSocket.isConnected()) {
//                    try {
//                        btSocket.close();
//                    } catch (IOException e) {
//                        logWarn(LogCategory.BT, "Error closing Bluetooth socket in finishApplication: " + e.getMessage());
//                    }
//                    btSocket = null;
//                } else if (btSocket != null) {
//                    btSocket = null;
//                }
//                deviceSelected = null;
//                // 메인 스레드에서 타이머/스레드 정리 후 재탐색 시작
//                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
//                    try {
//                        stopBtMessageTimer();
//                        new Thread(() -> {
//                            try {
//                                bluetoothSearch();
//                            } catch (Exception e) {
//                                logError(LogCategory.ER, Constants.ErrorMessages.BLUETOOTH_SEARCH_ERROR, e);
//                            }
//                        }).start();
//                    } catch (Exception e) {
//                        logError(LogCategory.ER, Constants.ErrorMessages.ERROR_IN_FINISH_APPLICATION_RECONNECT, e);
//                    }
//                });// Java
//                // bluetoothSearch 호출 직후에 추가
//                new Thread(() -> {
//                    try {
//                        bluetoothSearch();
//                    } catch (Exception e) {
//                        logError(LogCategory.ER, Constants.ErrorMessages.BLUETOOTH_SEARCH_ERROR, e);
//                    }
//                }).start();
//
//                // USB 재연결 시도 (메인 스레드에서 안전하게 로그/서비스 재시작)
//                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
//                    try {
//                        // 기존 USB 리소스 안전 종료 (변수명은 프로젝트에 맞게 변경)
//                        try {
//                            if (usbService != null) {
//                                usbService = null;
//                            }
//                        } catch (Exception ignored) {
//                        }
//
//                        // USB 서비스가 앱에서 서비스로 구현되어 있다면 재시작 시도
//                        try {
//                            Intent usbIntent = new Intent(getApplicationContext(), UsbService.class); // 존재하는 서비스 클래스로 변경
//                            try {
//                                stopService(usbIntent);
//                            } catch (Exception ignored) {
//                            }
//                            startService(usbIntent);
//                            // logInfo(LogCategory.US, "USB service restart requested");
//                        } catch (Exception e) {
//                            logWarn(LogCategory.US, "USB service restart failed: " + e.getMessage());
//                        }
//
//                        // 또는 프로젝트에 usbSearch()가 있다면 재탐색 호출
//                        try {
//                            usbSearch();
//                        } catch (Exception ignored) {
//                        }
//                    } catch (Exception e) {
//                        logError(LogCategory.ER, Constants.ErrorMessages.ERROR_IN_FINISH_APPLICATION_RECONNECT, e);
//                    }
//                });
//                currentTestItem = Constants.TestItemCodes.ST0101;
//
//                // 모든 Timer 정리 (중복 제거 및 누락 방지)
//                cleanupAllTimers();
//
//                // ⚠️ 키오스크 모드: KioskModeApplication이 자동으로 모니터링 중지함 (onPause/onStop에서 처리됨)
//
//                if (usbReceiverRegisted) {
//                    // unregisterReceiver(usbReceiver);
//                }
//                if (usbConnPermissionGranted) {
//                    unbindService(usbConnection);
//                }
//
//                usbReceiverRegisted = false;
//                usbConnPermissionGranted = false;
//
//                // btHandlerTmp 제거됨 (미사용 Handler)
////                if (server != null)
////                    server.closeAllConnections();
////                server.stop();
//
//                btSearchOnOff = false;
//                clearBluetoothReconnect();
//                if (btSocket != null && btSocket.isConnected()) {
//                    try {
//                        btSocket.close();
//                    } catch (IOException e) {
//                        logError(LogCategory.ER, Constants.ErrorMessages.ERROR_CLOSING_SOCKET, e);
//                    }
//                }
//                btSocket = null;
//                deviceSelected = null;
//                cancelDiscoverySafe();
//
//                try {
//                    Thread.sleep(Constants.Timeouts.THREAD_SLEEP_MS);
//                } catch (Exception e) {
//                    logError(LogCategory.ER, Constants.ErrorMessages.THREAD_SLEEP_ERROR, e);
//                }
//
//                // WeakReference 정리 (메모리 누수 방지)
//                clearMainActivityReference();
//
//                Intent intent = getIntent();
//
//                // ⚠️ 중요: 재시작 전에 Intent Extra 값들을 로그로 출력
//                String intentModelId = intent.getStringExtra(Constants.IntentExtras.TEST_MODEL_ID);
//                String intentModelName = intent.getStringExtra(Constants.IntentExtras.TEST_MODEL_NAME);
//                String intentModelNation = intent.getStringExtra(Constants.IntentExtras.TEST_MODEL_NATION);
//                String intentTestStartDatetime = intent.getStringExtra(Constants.IntentExtras.TEST_START_DATETIME);
//
////                logInfo(LogCategory.PS, "Intent Extra - TEST_MODEL_ID: " + (intentModelId != null ? intentModelId : "null") +
////                        ", TEST_MODEL_NAME: " + (intentModelName != null ? intentModelName : "null") +
////                        ", TEST_MODEL_NATION: " + (intentModelNation != null ? intentModelNation : "null") +
////                        ", TEST_START_DATETIME: " + (intentTestStartDatetime != null ? intentTestStartDatetime : "null"));
//
//                // ⚠️ 중요: 현재 static 변수 값들을 로그로 출력
//                logInfo(LogCategory.PS, "Before save to SharedPreferences - globalModelId: " +
//                        (globalModelId != null ? globalModelId : "null") +
//                        ", globalModelName: " + (globalModelName != null ? globalModelName : "null") +
//                        ", globalModelNation: " + (globalModelNation != null ? globalModelNation : "null"));
//
//                // ⚠️ 중요: SharedPreferences에 static 변수 값들 저장
//                SharedPreferences prefs = getSharedPreferences(Constants.SharedPrefKeys.TEST_COOKIE_INFO, MODE_PRIVATE);
//                SharedPreferences.Editor editor = prefs.edit();
//
//                // static 변수가 null이 아니고 비어있지 않으면 저장
//                if (globalModelId != null && !globalModelId.isEmpty()) {
//                    editor.putString(Constants.IntentExtras.TEST_MODEL_ID, globalModelId);
//                }
//                if (globalModelName != null && !globalModelName.isEmpty()) {
//                    editor.putString(Constants.IntentExtras.TEST_MODEL_NAME, globalModelName);
//                }
//                if (globalModelNation != null && !globalModelNation.isEmpty()) {
//                    editor.putString(Constants.IntentExtras.TEST_MODEL_NATION, globalModelNation);
//                }
//                editor.commit();
//
//                // ⚠️ 중요: SharedPreferences에 저장된 값들을 읽어서 로그로 출력
//                String savedModelId = prefs.getString(Constants.IntentExtras.TEST_MODEL_ID, null);
//                String savedModelName = prefs.getString(Constants.IntentExtras.TEST_MODEL_NAME, null);
//                String savedModelNation = prefs.getString(Constants.IntentExtras.TEST_MODEL_NATION, null);
//
//                logInfo(LogCategory.PS, "After save to SharedPreferences - TEST_MODEL_ID: " +
//                        (savedModelId != null ? savedModelId : "null") +
//                        ", TEST_MODEL_NAME: " + (savedModelName != null ? savedModelName : "null") +
//                        ", TEST_MODEL_NATION: " + (savedModelNation != null ? savedModelNation : "null"));
//
//                // ⚠️ 중요: Intent에 Extra로 값들 재설정 (재시작 시 전달되도록)
//                if (globalModelId != null && !globalModelId.isEmpty()) {
//                    intent.putExtra(Constants.IntentExtras.TEST_MODEL_ID, globalModelId);
//                }
//                if (globalModelName != null && !globalModelName.isEmpty()) {
//                    intent.putExtra(Constants.IntentExtras.TEST_MODEL_NAME, globalModelName);
//                }
//                if (globalModelNation != null && !globalModelNation.isEmpty()) {
//                    intent.putExtra(Constants.IntentExtras.TEST_MODEL_NATION, globalModelNation);
//                }
//                if (intentTestStartDatetime != null && !intentTestStartDatetime.isEmpty()) {
//                    intent.putExtra(Constants.IntentExtras.TEST_START_DATETIME, intentTestStartDatetime);
//                }
//
//                // ⚠️ 중요: Intent Extra 재설정 후 값들을 로그로 출력
//                String finalIntentModelId = intent.getStringExtra(Constants.IntentExtras.TEST_MODEL_ID);
//                String finalIntentModelName = intent.getStringExtra(Constants.IntentExtras.TEST_MODEL_NAME);
//                String finalIntentModelNation = intent.getStringExtra(Constants.IntentExtras.TEST_MODEL_NATION);
//                String finalIntentTestStartDatetime = intent.getStringExtra(Constants.IntentExtras.TEST_START_DATETIME);
//
//                logInfo(LogCategory.PS, "Intent Extra after putExtra - TEST_MODEL_ID: " +
//                        (finalIntentModelId != null ? finalIntentModelId : "null") +
//                        ", TEST_MODEL_NAME: " + (finalIntentModelName != null ? finalIntentModelName : "null") +
//                        ", TEST_MODEL_NATION: " + (finalIntentModelNation != null ? finalIntentModelNation : "null") +
//                        ", TEST_START_DATETIME: " + (finalIntentTestStartDatetime != null ? finalIntentTestStartDatetime : "null"));
//
//                finish();
//                if (finishedCorrectly) {
//                    finishedCorrectly = false;
//                    startActivity(intent);
//                }
//            } catch (Exception e) {
//                logError(LogCategory.ER, Constants.ErrorMessages.APPLICATION_RESTART_FINISH_ERROR, e);
//            }
//        }).start();
//    }
//
//    public void setDisplayLightValueChange(float value) {
//        try {
//            params = getWindow().getAttributes();
//            params.screenBrightness = value;
//            getWindow().setAttributes(params);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    class RequestThreadAsync extends AsyncTask<String, Void, String> {
//        @Override
//        protected void onPreExecute() {
//            super.onPreExecute();
//        }
//
//        @Override
//        protected String doInBackground(String... strings) {
//            try {
//                new Thread(() -> {
//                    // 동시성 문제 해결: connection을 로컬 변수로 변경 (각 스레드가 자신만의 connection 사용)
//                    HttpURLConnection connection = null;
//                    try {
//                        String url = urlStr;
//                        URL obj = new URL(url);
//                        connection = (HttpURLConnection) obj.openConnection();
//
//                        connection.setReadTimeout(Constants.Timeouts.HTTP_READ_TIMEOUT_LONG_MS);
//                        connection.setConnectTimeout(Constants.Timeouts.HTTP_CONNECT_TIMEOUT_LONG_MS);
//                        connection.setRequestMethod(Constants.HTTP.METHOD_POST);  // POST 방식으로 요청
//                        connection.setDoInput(true);  // InputStream 으로 서버로부터 응답을 받겠다는 옵션
//                        connection.setDoOutput(true); // OutputStream 으로 POST 데이터를 넘겨 주겠다는 옵션
//                        connection.setRequestProperty(Constants.HTTP.HEADER_CONTENT_TYPE, Constants.HTTP.CONTENT_TYPE_JSON);    // application/json 형식으로 전송. (Request body 전달시 application/json 으로 서버에 전달)
//                        connection.setRequestProperty(Constants.HTTP.HEADER_CACHE_CONTROL, Constants.HTTP.CACHE_CONTROL_NO_CACHE);
//
//                        try {
//                            int retCode = connection.getResponseCode();
//                            logInfo(LogCategory.PS, Constants.LogMessages.HTTP_RESPONSE_CODE + retCode);
//                            if (retCode == HttpURLConnection.HTTP_OK) {
//                                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//                                // BufferedReader() : 엔터만 경계로 인식하고 받은 데이터를 String 으로 고정, Scanner 에 비해 빠름!
//                                // InputStreamReader() : 지정된 문자 집합 내의 문자로 인코딩
//                                // getInputStream() : url 에서 데이터를 읽어옴
//                                try {
//                                    String line = null;
//                                    String lineTmp = null;
//                                    StringBuilder sb = new StringBuilder();
//
//                                    // 무한 루프 방지를 위해 최대 읽기 횟수 제한
//                                    int maxReadCount = Constants.Timeouts.MAX_READ_COUNT;
//                                    int readCount = 0;
//
//                                    while (readCount < maxReadCount) {
//                                        lineTmp = reader.readLine();
//                                        if (lineTmp == null) {
//                                            break; // 스트림 끝에 도달
//                                        }
//                                        readCount++;
//
//                                        if (!lineTmp.trim().equals("")) {
//                                            sb.append(lineTmp);
//                                            line = lineTmp;
//                                        }
//                                    }
//
//                                    // 모든 데이터를 읽은 후 한 번만 처리
//                                    final String data = (line != null && !line.trim().isEmpty()) ? line :
//                                            (sb.length() > 0 ? sb.toString() : null);
//
//                                    if (data != null && !data.trim().isEmpty()) {
//                                        final String finalData = data;
//
//                                        // JSON 파싱을 백그라운드에서 직접 실행하여 메인 스레드 부하 최소화
//                                        // 디바운싱을 위해 지연 실행
//                                        synchronized (HTTP_HANDLER_LOCK) {
//                                            // 이전 작업이 아직 실행되지 않았다면 취소
//                                            if (pendingHttpTask != null) {
//                                                schedulerHandler.removeCallbacks(pendingHttpTask);
//                                            }
//
//                                            // 새 작업을 백그라운드에서 지연 실행 (디바운싱)
//                                            pendingHttpTask = new Runnable() {
//                                                @Override
//                                                public void run() {
//                                                    // 백그라운드 스레드에서 직접 jsonParsing 호출
//                                                    // jsonParsing 내부에서 이미 Thread를 생성하지만,
//                                                    // 메인 스레드를 거치지 않아 부하가 줄어듭니다
//                                                    try {
//                                                        logDebug(LogCategory.PS, Constants.LogMessages.PROCESSING_HTTP_RESPONSE_DATA);
//
//                                                        new Thread(() -> {
//                                                            try {
//                                                                logDebug(LogCategory.PS, Constants.LogMessages.DELETING_TEST_SPEC_DATA);
//                                                                TestData.deleteTestSpecData(getApplicationContext());
//                                                                logDebug(LogCategory.PS, Constants.LogMessages.TEST_SPEC_DATA_DELETED);
//                                                            } catch (Exception e) {
//                                                                Thread.currentThread().interrupt();
//                                                            }
//                                                        }).start();
//
//                                                        jsonParsing("test_spec", finalData);
//                                                    } catch (Exception e) {
//                                                        logError(LogCategory.ER, Constants.ErrorMessages.JSON_PARSING_ERROR, e);
//                                                    } finally {
//                                                        synchronized (HTTP_HANDLER_LOCK) {
//                                                            pendingHttpTask = null;
//                                                        }
//                                                    }
//                                                }
//                                            };
//
//                                            // 디바운스 지연 후 백그라운드에서 실행 (여러 요청이 동시에 와도 마지막 것만 처리)
//                                            new Thread(() -> {
//                                                try {
//                                                    Thread.sleep(HTTP_DEBOUNCE_DELAY_MS);
//                                                    pendingHttpTask.run();
//                                                } catch (InterruptedException e) {
//                                                    Thread.currentThread().interrupt();
//                                                }
//                                            }).start();
//                                        }
//                                    }
//                                } finally {
//                                    try {
//                                        reader.close();
//                                    } catch (IOException e) {
//                                        logError(LogCategory.ER, Constants.ErrorMessages.ERROR_CLOSING_READER, e);
//                                    }
//                                }
//                            }
//                        } catch (Exception e) {
//                            logError(LogCategory.ER, Constants.ErrorMessages.HTTP_CONNECTION_ERROR, e);
//                        } finally {
//                            // 리소스 정리 보장 (finally 블록에서 한 번만 disconnect)
//                            safeDisconnectConnection(connection);
//                        }
//                    } catch (Exception e) {
//                        logError(LogCategory.ER, Constants.ErrorMessages.HTTP_REQUEST_ERROR, e);
//                        e.printStackTrace();
//                    }
//                }).start();
//            } catch (Exception e) {
//                logError(LogCategory.ER, Constants.ErrorMessages.REQUEST_THREAD_ASYNC_ERROR, e);
//                e.printStackTrace();
//            }
//
//            return null;
//        }
//
//        @Override
//        protected void onPostExecute(String s) {
////            // doInBackground() 이후에 수행될 작업
////            // String s 파라미터는 doInBackground() 의 리턴값이다.
////            TextView tv = findViewById(R.id.getText);
////            tv.setText(s);
//        }
//    }
//
//    class RequestTestTaskThreadAsync extends AsyncTask<String, Void, String> {
//        @Override
//        protected void onPreExecute() {
//            super.onPreExecute();
//        }
//
//        @Override
//        protected String doInBackground(String... strings) {
//            try {
//                new Thread(() -> {
//                    // 동시성 문제 해결: connection을 로컬 변수로 변경 (각 스레드가 자신만의 connection 사용)
//                    HttpURLConnection connection = null;
//                    try {
//                        String url = urlTestTaskStr;
//                        URL obj = new URL(url);
//                        connection = (HttpURLConnection) obj.openConnection();
//                        // Log.i(TAG, "▶ [PS] RequestTestTaskThreadAsync.urlTestTaskStr " + urlTestTaskStr);
//
//                        connection.setReadTimeout(Constants.Timeouts.HTTP_READ_TIMEOUT_LONG_MS);
//                        connection.setConnectTimeout(Constants.Timeouts.HTTP_CONNECT_TIMEOUT_LONG_MS);
//                        connection.setRequestMethod(Constants.HTTP.METHOD_GET);  // POST 방식으로 요청
//                        connection.setDoInput(true);  // InputStream 으로 서버로부터 응답을 받겠다는 옵션
//                        connection.setDoOutput(true); // OutputStream 으로 POST 데이터를 넘겨 주겠다는 옵션
//                        connection.setRequestProperty(Constants.HTTP.HEADER_CONTENT_TYPE, Constants.HTTP.CONTENT_TYPE_JSON);    // application/json 형식으로 전송. (Request body 전달시 application/json 으로 서버에 전달)
//                        connection.setRequestProperty(Constants.HTTP.HEADER_CACHE_CONTROL, Constants.HTTP.CACHE_CONTROL_NO_CACHE);
//
//                        try {
//                            int retCode = connection.getResponseCode();
//                            // Log.i(TAG, "▶ [PS] test task response " + retCode);
//                            if (retCode == HttpURLConnection.HTTP_OK) {
//                                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//                                try {
//                                    String line = null;
//                                    String lineTmp = null;
//                                    StringBuilder sb = new StringBuilder();
//
//                                    // 무한 루프 방지를 위해 최대 읽기 횟수 제한
//                                    int readCount = 0;
//
//                                    while (readCount < Constants.Timeouts.MAX_READ_COUNT) {
//                                        lineTmp = reader.readLine();
//                                        if (lineTmp == null) {
//                                            break; // 스트림 끝에 도달
//                                        }
//                                        readCount++;
//
//                                        if (!lineTmp.trim().isEmpty()) {
//                                            sb.append(lineTmp);
//                                            line = lineTmp;
//                                        }
//                                    }
//
//                                    // 모든 데이터를 읽은 후 한 번만 처리
//                                    final String data = (line != null && !line.trim().isEmpty()) ? line :
//                                            (sb.length() > 0 ? sb.toString() : null);
//
//                                    if (data != null && !data.trim().isEmpty()) {
//                                        schedulerHandler.post(new Runnable() {
//                                            @Override
//                                            public void run() {
//                                                try {
//                                                    logDebug(LogCategory.PS, Constants.LogMessages.TEST_TASK_RESPONSE_DATA_RECEIVED);
//                                                    // jsonParsing("test_spec", finalData);
//                                                } catch (Exception e) {
//                                                    logError(LogCategory.ER, Constants.ErrorMessages.ERROR_PROCESSING_TEST_TASK_DATA, e);
//                                                }
//                                            }
//                                        });
//                                    }
//                                } finally {
//                                    try {
//                                        reader.close();
//                                    } catch (IOException e) {
//                                        logError(LogCategory.ER, Constants.ErrorMessages.ERROR_CLOSING_READER, e);
//                                    }
//                                }
//                            }
//                        } catch (Exception e) {
//                            logError(LogCategory.ER, Constants.ErrorMessages.REQUEST_TEST_TASK_THREAD_ASYNC_CONNECTION_ERROR, e);
//                        } finally {
//                            // 리소스 정리 보장 (finally 블록에서 한 번만 disconnect)
//                            safeDisconnectConnection(connection);
//                        }
//                    } catch (Exception e) {
//                        logError(LogCategory.ER, Constants.ErrorMessages.REQUEST_TEST_TASK_THREAD_ASYNC_REQUEST_ERROR, e);
//                        e.printStackTrace();
//                    }
//                }).start();
//            } catch (Exception e) {
//                logError(LogCategory.ER, Constants.ErrorMessages.REQUEST_TEST_TASK_THREAD_ASYNC_ERROR, e);
//                e.printStackTrace();
//            }
//
//            return null;
//        }
//
//        @Override
//        protected void onPostExecute(String s) {
//        }
//    }
//
//    /**
//     * VersionInfoList.jsp 호출하여 네트워크 상태 체크 및 버전 비교
//     */
//    class RequestVersionInfoThreadAsync extends AsyncTask<String, Void, String> {
//        private final String localVersionId;
//        private final String localModelId;
//
//        public RequestVersionInfoThreadAsync(String localVersionId, String localModelId) {
//            this.localVersionId = localVersionId;
//            this.localModelId = localModelId;
//        }
//
//        @Override
//        protected void onPreExecute() {
//            super.onPreExecute();
//        }
//
//        @Override
//        protected String doInBackground(String... strings) {
//            try {
//                new Thread(() -> {
//                    HttpURLConnection connection = null;
//                    try {
//                        String url = Constants.URLs.HTTP_PROTOCOL + serverIp + Constants.URLs.ENDPOINT_VERSION_INFO_LIST;
//                        if (globalModelId != null && !globalModelId.isEmpty()) {
//                            url += Constants.Common.QUESTION + Constants.URLs.PARAM_MODEL_ID + globalModelId;
//                        }
//                        // logInfo(LogCategory.PS, "RequestVersionInfoThreadAsync.url " + url);
//                        URL obj = new URL(url);
//                        connection = (HttpURLConnection) obj.openConnection();
//
//                        connection.setReadTimeout(Constants.Timeouts.HTTP_READ_TIMEOUT_LONG_MS);
//                        connection.setConnectTimeout(Constants.Timeouts.HTTP_CONNECT_TIMEOUT_LONG_MS);
//                        connection.setRequestMethod(Constants.HTTP.METHOD_POST);
//                        connection.setDoInput(true);
//                        connection.setDoOutput(true);
//                        connection.setRequestProperty(Constants.HTTP.HEADER_CONTENT_TYPE, Constants.HTTP.CONTENT_TYPE_JSON);
//                        connection.setRequestProperty(Constants.HTTP.HEADER_CACHE_CONTROL, Constants.HTTP.CACHE_CONTROL_NO_CACHE);
//
//                        try {
//                            int retCode = connection.getResponseCode();
//                            // logInfo(LogCategory.SI, "VersionInfoList response code: " + retCode);
//
//                            if (retCode == HttpURLConnection.HTTP_OK) {
//                                // 네트워크 상태가 정상이면 tvRunWsRamp를 파란색으로 변경
//                                scheduleUiUpdate(() -> {
//                                    tvRunWsRamp.setText(String.valueOf(unit_no).replace("-", "*"));
//                                    tvRunWsRamp.setBackgroundColor(getBaseContext().getResources().getColor(R.color.blue_01));
//                                });
//
//                                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//                                try {
//                                    String line = null;
//                                    String lineTmp = null;
//                                    StringBuilder sb = new StringBuilder();
//                                    int readCount = 0;
//
//                                    while (readCount < Constants.Timeouts.MAX_READ_COUNT) {
//                                        lineTmp = reader.readLine();
//                                        if (lineTmp == null) {
//                                            break;
//                                        }
//                                        readCount++;
//                                        if (!lineTmp.trim().isEmpty()) {
//                                            sb.append(lineTmp);
//                                            line = lineTmp;
//                                        }
//                                    }
//
//                                    final String data = (line != null && !line.trim().isEmpty()) ? line :
//                                            (sb.length() > 0 ? sb.toString() : null);
//
//                                    if (data != null && !data.trim().isEmpty()) {
//                                        // 버전 정보 파싱 및 비교
//                                        try {
//                                            JSONObject jsonObject = new JSONObject(data);
//                                            String serverVersionId = jsonObject.optString(Constants.JsonKeys.CLM_TEST_VERSION_ID, "");
//                                            String serverModelId = jsonObject.optString(Constants.JsonKeys.CLM_MODEL_ID, "");
//
//                                            // logInfo(LogCategory.SI, "Version info received: " + data);
//                                            logInfo(LogCategory.SI, String.format("Version comparison - Local: [%s, %s], Server: [%s, %s]",
//                                                    localVersionId, localModelId, serverVersionId, serverModelId));
//
//                                            // 버전이 틀리면 TestInfoList.jsp 호출하여 DB 갱신
//                                            boolean versionMismatch = false;
//                                            if (localVersionId != null && !localVersionId.isEmpty() &&
//                                                    serverVersionId != null && !serverVersionId.isEmpty() &&
//                                                    !localVersionId.equals(serverVersionId)) {
//                                                versionMismatch = true;
//                                                logWarn(LogCategory.SI, "Test version ID mismatch detected. Updating test spec data.");
//                                            } else if (localVersionId != null && !localVersionId.isEmpty() &&
//                                                    serverVersionId != null && !serverVersionId.isEmpty() &&
//                                                    !localVersionId.equals(serverVersionId)) {
//                                                versionMismatch = true;
//                                                logWarn(LogCategory.SI, "Model version mismatch detected. Updating test spec data.");
//                                            }
//
//                                            if (versionMismatch) {
//                                                // TestInfoList.jsp 호출하여 DB 갱신
//                                                clearHttpHandlerQueue();
//                                                ActivityModel_0003.RequestThreadAsync thread = new ActivityModel_0003.RequestThreadAsync();
//                                                thread.execute();
//                                                // logInfo(LogCategory.SI, "Test spec data update requested due to version mismatch.");
//                                            } else {
//                                                logInfo(LogCategory.SI, "Version matches. No update needed.");
//                                            }
//                                        } catch (JSONException e) {
//                                            logError(LogCategory.ER, "Error parsing version info JSON", e);
//                                        }
//                                    }
//                                } finally {
//                                    try {
//                                        reader.close();
//                                    } catch (IOException e) {
//                                        logError(LogCategory.ER, Constants.ErrorMessages.ERROR_CLOSING_READER, e);
//                                    }
//                                }
//                            } else {
//                                // 네트워크 상태가 비정상이면 tvRunWsRamp를 빨간색으로 유지
//                                logWarn(LogCategory.SI, "VersionInfoList request failed with code: " + retCode);
//                                scheduleUiUpdate(() -> {
//                                    // tvRunWsRamp.setText(Constants.Common.EMPTY_STRING);
//                                    tvRunWsRamp.setBackgroundColor(getBaseContext().getResources().getColor(R.color.red_01));
//                                });
//                            }
//                        } catch (Exception e) {
//                            logError(LogCategory.ER, "VersionInfoList connection error", e);
//                            // 네트워크 오류 시 tvRunWsRamp를 빨간색으로 유지
//                            scheduleUiUpdate(() -> {
//                                // tvRunWsRamp.setText(Constants.Common.EMPTY_STRING);
//                                tvRunWsRamp.setBackgroundColor(getBaseContext().getResources().getColor(R.color.red_01));
//                            });
//                        } finally {
//                            safeDisconnectConnection(connection);
//                        }
//                    } catch (Exception e) {
//                        logError(LogCategory.ER, "VersionInfoList request error", e);
//                        // 네트워크 오류 시 tvRunWsRamp를 빨간색으로 유지
//                        scheduleUiUpdate(() -> {
//                            // tvRunWsRamp.setText(Constants.Common.EMPTY_STRING);
//                            tvRunWsRamp.setBackgroundColor(getBaseContext().getResources().getColor(R.color.red_01));
//                        });
//                    }
//                }).start();
//            } catch (Exception e) {
//                logError(LogCategory.ER, "RequestVersionInfoThreadAsync error", e);
//            }
//
//            return null;
//        }
//
//        @Override
//        protected void onPostExecute(String s) {
//        }
//    }
//
//    private void jsonParsing(String data_type, String json) {
//        if (!Constants.JsonKeys.TEST_SPEC.equals(data_type) || json == null) {
//            return;
//        }
//        runOnBtWorker(() -> processTestSpecJson(json));
//    }
//
//    private void processTestSpecJson(String json) {
//        try {
//            String specSignature = computeSha256(json);
//            if (specSignature != null && specSignature.equals(lastSpecSignature)) {
//                // logInfo(LogCategory.SI, "Test spec unchanged; skipping reload");
//                return;
//            }
//
//            JSONObject jsonObject = new JSONObject(json);
//            JSONArray testItemArray = jsonObject.getJSONArray(Constants.JsonKeys.TEST_SPEC);
//
//            ActivityModel_0003 activity = getMainActivity();
//            if (activity != null) {
//                activity.lstData = new ArrayList<>();
//            }
//            List<Map<String, String>> lstData = (activity != null) ? activity.lstData : new ArrayList<>();
//
//            if (testItemArray.length() == 0) {
//                logWarn(LogCategory.SI, "Test spec array is empty");
//                return;
//            }
//
//            JSONObject testItemObject = testItemArray.getJSONObject(0);
//            arrTestModels[0][0] = testItemObject.getString(Constants.JsonKeys.CLM_MODEL_ID);
//            arrTestModels[0][1] = testItemObject.getString(Constants.JsonKeys.CLM_MODEL_NAME);
//            arrTestModels[0][2] = testItemObject.getString(Constants.JsonKeys.CLM_MODEL_NATION);
//            arrTestModels[0][3] = testItemObject.getString(Constants.JsonKeys.CLM_TEST_VERSION_ID);
//            arrTestModels[0][4] = testItemObject.getString(Constants.JsonKeys.CLM_MODEL_VERSION);
//
//            // System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>> " + arrTestModels[0][0] + " " + arrTestModels[0][1]);
//
//            // 첫 번째 객체에서 버전 정보와 모델 ID 추출 (모든 항목에 동일하게 적용)
//            String testVersionId = testItemArray.getJSONObject(0).optString(Constants.JsonKeys.CLM_TEST_VERSION_ID, "");
//            String modelId = testItemArray.getJSONObject(0).optString(Constants.JsonKeys.CLM_MODEL_ID, "");
//
//            for (int i = 0; i < testItemArray.length() - 1; i++) {
//                testItemObject = testItemArray.getJSONObject(i);
//                Map<String, String> mapData = new HashMap<>();
//                mapData.put(Constants.JsonKeys.CLM_TEST_SEQ, String.valueOf(i));
//                mapData.put(Constants.JsonKeys.CLM_LOWER_VALUE, testItemObject.getString(Constants.JsonKeys.CLM_LOWER_VALUE));
//                mapData.put(Constants.JsonKeys.CLM_UPPER_VALUE, testItemObject.getString(Constants.JsonKeys.CLM_UPPER_VALUE));
//                mapData.put(Constants.JsonKeys.CLM_LOWER_VALUE_WATT, testItemObject.getString(Constants.JsonKeys.CLM_LOWER_VALUE_WATT));
//                mapData.put(Constants.JsonKeys.CLM_UPPER_VALUE_WATT, testItemObject.getString(Constants.JsonKeys.CLM_UPPER_VALUE_WATT));
//                mapData.put(Constants.JsonKeys.CLM_TEST_STEP, testItemObject.getString(Constants.JsonKeys.CLM_TEST_STEP));
//                mapData.put(Constants.JsonKeys.CLM_TEST_NAME, testItemObject.getString(Constants.JsonKeys.CLM_TEST_NAME));
//                mapData.put(Constants.JsonKeys.CLM_TEST_ID, testItemObject.getString(Constants.JsonKeys.CLM_TEST_ID));
//                mapData.put(Constants.JsonKeys.CLM_RESPONSE_VALUE, testItemObject.getString(Constants.JsonKeys.CLM_RESPONSE_VALUE));
//                mapData.put(Constants.JsonKeys.CLM_TEST_SEC, testItemObject.getString(Constants.JsonKeys.CLM_TEST_SEC));
//                mapData.put(Constants.JsonKeys.CLM_TEST_TYPE, testItemObject.getString(Constants.JsonKeys.CLM_TEST_TYPE));
//                mapData.put(Constants.JsonKeys.CLM_TEST_COMMAND, testItemObject.getString(Constants.JsonKeys.CLM_TEST_COMMAND));
//                mapData.put(Constants.JsonKeys.CLM_VALUE_WATT, testItemObject.getString(Constants.JsonKeys.CLM_VALUE_WATT));
//                mapData.put(Constants.JsonKeys.CLM_VALUE, testItemObject.getString(Constants.JsonKeys.CLM_VALUE));
//                mapData.put(Constants.JsonKeys.CLM_PRODUCT_SERIAL_NO, testItemObject.getString(Constants.JsonKeys.CLM_PRODUCT_SERIAL_NO));
//                mapData.put(Constants.JsonKeys.CLM_COMMENT, "");
//                // clm_test_version_id와 clm_model_id 추가
//                mapData.put(Constants.JsonKeys.CLM_TEST_VERSION_ID, testVersionId);
//                mapData.put(Constants.JsonKeys.CLM_MODEL_ID, modelId);
//                lstData.add(mapData);
//            }
//
//            // lstDataTmp = lstData;
//            // System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>> lstDataTmp.size() " + lstDataTmp.size());
//
//            applyTestSpecData(lstData, true);
//
//            if (specSignature != null) {
//                lastSpecSignature = specSignature;
//            }
//        } catch (JSONException e) {
//            logError(LogCategory.ER, Constants.ErrorMessages.JSON_PARSING_ERROR, e);
//        }
//    }
//
//    public void bluetoothSearch() {
//        // 블루투스 통신 시작 전에 이전 메시지 큐 정리하여 메인 스레드 과부하 방지
//        clearBtHandlerQueue();
//        if (ensureBtPermissions(PermissionAction.LIST_PAIRED_DEVICES)) {
//            listPairedDevicesSelect();
//        }
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == MULTIPLE_PERMISSIONS || requestCode == PERMISSION_REQUEST_CODE || requestCode == PERMISSION_REQUEST_CODE_BT) {
//            permissionRequestInProgress = false;
//            boolean allGranted = grantResults.length > 0;
//            for (int grantResult : grantResults) {
//                if (grantResult != PackageManager.PERMISSION_GRANTED) {
//                    allGranted = false;
//                    break;
//                }
//            }
//
//            if (allGranted && hasAllBluetoothPermissions()) {
//                // 권한이 허용되었으면 거부 횟수 리셋
//                permissionDenialCount = 0;
//                btPermissionsGranted = true;
//                runPendingPermissionActions();
//            } else {
//                // 권한이 거부되었으면 거부 횟수 증가
//                permissionDenialCount++;
//                btPermissionsGranted = false;
//                pendingPermissionActions.clear();
//                handlePermissionDenial(permissions, grantResults);
//            }
//        }
//    }
//
//    private void handlePermissionDenial(@NonNull String[] permissions, @NonNull int[] grantResults) {
//        boolean permanentlyDenied = false;
//        for (int i = 0; i < permissions.length; i++) {
//            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
//                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[i])) {
//                    permanentlyDenied = true;
//                    break;
//                }
//            }
//        }
//        // showPermissionPrompt(permanentlyDenied);
//    }
//
//    private void getPreferences() {
//        // 복잡한 UI 업데이트 블록 통합: scheduleUiUpdate 사용
//        scheduleUiUpdate(() -> {
//            tvSerialNo.setTextColor(postTestInfoColor);
//            // tvSerialNo.setText(productSerialNo);
//        });
//    }
//
//    /**
//     * MainActivity 인스턴스를 안전하게 가져옵니다 (WeakReference 사용)
//     * Activity가 이미 종료되었거나 GC된 경우 null을 반환합니다.
//     *
//     * @return ActivityModel_0003 인스턴스 또는 null
//     */
//    public static ActivityModel_0003 getMainActivity() {
//        if (mainActivityRef != null) {
//            ActivityModel_0003 activity = mainActivityRef.get();
//            if (activity == null) {
//                // Activity가 GC된 경우 참조 정리
//                mainActivityRef = null;
//            }
//            return activity;
//        }
//        return null;
//    }
//
//    /**
//     * Application Context를 안전하게 가져옵니다
//     *
//     * @return Application Context 또는 null
//     */
//    public static Context getMainActivityContext() {
//        ActivityModel_0003 activity = getMainActivity();
//        return activity != null ? activity.getApplicationContext() : null;
//    }
//
//    /**
//     * MainActivity 참조를 정리합니다 (메모리 누수 방지)
//     */
//    private static void clearMainActivityReference() {
//        if (mainActivityRef != null) {
//            mainActivityRef.clear();
//            mainActivityRef = null;
//        }
//    }
//
//    /**
//     * HTTP Handler 메시지 큐 정리 - 메인 스레드 과부하 방지
//     * ⚠️ 통합: httpHandler → schedulerHandler로 통합됨
//     */
//    private void clearHttpHandlerQueue() {
//        synchronized (HTTP_HANDLER_LOCK) {
//            if (schedulerHandler != null) {
//                schedulerHandler.removeCallbacksAndMessages(null);
//            }
//            pendingHttpTask = null;
//        }
//    }
//
//    /**
//     * HttpURLConnection 리소스 안전하게 정리
//     *
//     * @param connection 정리할 HttpURLConnection (null 가능)
//     */
//    private static void safeDisconnectConnection(HttpURLConnection connection) {
//        if (connection != null) {
//            try {
//                connection.disconnect();
//            } catch (Exception e) {
//                Log.e(TAG, "▶ [ER] Error disconnecting connection", e);
//            }
//        }
//    }
//
//    /**
//     * 블루투스 Handler 메시지 큐 정리 - 메인 스레드 과부하 방지
//     */
//    private void clearBtHandlerQueue() {
//        if (btHandler != null) {
//            btHandler.removeCallbacksAndMessages(null);
//        }
//    }
//
//    // ==================== 로그 관리 시스템 ====================
//
//    /**
//     * 로그 카테고리 정의
//     */
//    private enum LogCategory {
//        PS(resolveCode("PS", "PS"), resolveName("PS", "Process")),      // 프로세스 관련
//        BT(resolveCode("BT", "BT"), resolveName("BT", "Bluetooth")),     // 블루투스 관련
//        US(resolveCode("US", "US"), resolveName("US", "USB")),           // USB 관련
//        SI(resolveCode("SI", "SI"), resolveName("SI", "ServerInfo")),   // 서버 정보
//        ER(resolveCode("ER", "ER"), resolveName("ER", "Error")),         // 에러
//        CA(resolveCode("CA", "CA"), resolveName("CA", "Cache")),         // 캐시
//        TH(resolveCode("TH", "TH"), resolveName("TH", "Temperature")),   // 온도
//        BI(resolveCode("BI", "BI"), resolveName("BI", "BarcodeInfo")),   // 바코드 정보
//        RS(resolveCode("RS", "RS"), resolveName("RS", "Result"));        // 결과
//
//        private final String code;
//        private final String name;
//
//        LogCategory(String code, String name) {
//            this.code = code;
//            this.name = name;
//        }
//
//        public String getCode() {
//            return code;
//        }
//
//        public String getName() {
//            return name;
//        }
//
//        // 가능한 Constants 필드명을 시도하여 값을 가져오고, 없으면 기본값을 반환
//        private static String resolveCode(String key, String defaultVal) {
//            return resolveConstant(key, defaultVal, true);
//        }
//
//        private static String resolveName(String key, String defaultVal) {
//            return resolveConstant(key, defaultVal, false);
//        }
//
//        private static String resolveConstant(String key, String defaultVal, boolean forCode) {
//            try {
//                Class<?> c = Constants.class;
//                String[] candidates;
//                if (forCode) {
//                    candidates = new String[]{
//                            "LOGCAT_" + key + "_CODE",
//                            "LOG_CATEGORY_" + key + "_CODE",
//                            "LOG_" + key + "_CODE",
//                            key + "_CODE",
//                            "LOGCAT_" + key,
//                            "LOG_CATEGORY_" + key,
//                            "LOG_" + key,
//                            key
//                    };
//                } else {
//                    candidates = new String[]{
//                            "LOGCAT_" + key + "_NAME",
//                            "LOG_CATEGORY_" + key + "_NAME",
//                            "LOG_" + key + "_NAME",
//                            key + "_NAME",
//                            "LOGCAT_" + key,
//                            "LOG_CATEGORY_" + key,
//                            "LOG_" + key,
//                            key
//                    };
//                }
//
//                for (String fieldName : candidates) {
//                    try {
//                        java.lang.reflect.Field f = c.getField(fieldName);
//                        if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
//                            Object v = f.get(null);
//                            if (v instanceof String) {
//                                return (String) v;
//                            }
//                        }
//                    } catch (NoSuchFieldException | IllegalAccessException ignored) {
//                        // 존재하지 않거나 접근 불가하면 다음 후보로
//                    }
//                }
//            } catch (Exception ignored) {
//                // Constants 클래스 접근 실패 시 기본값 반환
//            }
//            return defaultVal;
//        }
//    }
//
//    /**
//     * 로그 레벨 정의
//     */
//    private enum LogLevel {
//        VERBOSE(0, Constants.InitialValues.LOG_LEVEL_VERBOSE),
//        DEBUG(1, Constants.InitialValues.LOG_LEVEL_DEBUG),
//        INFO(2, Constants.InitialValues.LOG_LEVEL_INFO),
//        WARN(3, Constants.InitialValues.LOG_LEVEL_WARN),
//        ERROR(4, Constants.InitialValues.LOG_LEVEL_ERROR);
//
//        private final int priority;
//        private final String code;
//
//        LogLevel(int priority, String code) {
//            this.priority = priority;
//            this.code = code;
//        }
//
//        public int getPriority() {
//            return priority;
//        }
//
//        public String getCode() {
//            return code;
//        }
//    }
//
//    // 로그 레벨 설정 (빌드 타입에 따라 변경 가능)
//    private static final LogLevel MIN_LOG_LEVEL = LogLevel.DEBUG;
//    private static final boolean ENABLE_LOG_BATCHING = true;
//    private static final long LOG_BATCH_INTERVAL_MS = 200; // 200ms마다 배치 처리
//
//    // 로그 배치 처리를 위한 변수 (mainHandler 사용)
//    private static final Object LOG_BATCH_LOCK = new Object();
//    private static final List<LogEntry> logBatchQueue = new ArrayList<>();
//    private static Runnable logBatchTask = null;
//
//    /**
//     * 로그 엔트리 클래스
//     */
//    private static class LogEntry {
//        final LogLevel level;
//        final LogCategory category;
//        final String message;
//        final Throwable throwable;
//        final long timestamp;
//
//        LogEntry(LogLevel level, LogCategory category, String message, Throwable throwable) {
//            this.level = level;
//            this.category = category;
//            this.message = message;
//            this.throwable = throwable;
//            this.timestamp = System.currentTimeMillis();
//        }
//    }
//
//    /**
//     * 통합 로그 메서드 - 배치 처리 지원
//     */
//    private void log(LogLevel level, LogCategory category, String message, Throwable throwable) {
//        // 로그 레벨 체크
//        if (level.getPriority() < MIN_LOG_LEVEL.getPriority()) {
//            return;
//        }
//
//        // 로그 포맷 통일
//        String formattedMessage = String.format("▶ [%s] %s", category.getCode(), message);
//
//        if (ENABLE_LOG_BATCHING && level.getPriority() >= LogLevel.INFO.getPriority()) {
//            // 배치 처리 대상 로그
//            synchronized (LOG_BATCH_LOCK) {
//                logBatchQueue.add(new LogEntry(level, category, formattedMessage, throwable));
//
//                // 배치 작업이 예약되지 않았다면 예약
//                if (logBatchTask == null) {
//                    logBatchTask = new Runnable() {
//                        @Override
//                        public void run() {
//                            flushLogBatch();
//                        }
//                    };
//                    mainHandler.postDelayed(logBatchTask, LOG_BATCH_INTERVAL_MS);
//                }
//            }
//        } else {
//            // 즉시 출력 (ERROR, WARN, 중요한 DEBUG)
//            outputLog(level, formattedMessage, throwable);
//        }
//    }
//
//    /**
//     * 로그 배치 플러시
//     */
//    private void flushLogBatch() {
//        synchronized (LOG_BATCH_LOCK) {
//            if (logBatchQueue.isEmpty()) {
//                logBatchTask = null;
//                return;
//            }
//
//            // 배치 로그를 한 번에 출력
//            List<LogEntry> batch = new ArrayList<>(logBatchQueue);
//            logBatchQueue.clear();
//            logBatchTask = null;
//
//            // 배치 로그 출력
//            for (LogEntry entry : batch) {
//                outputLog(entry.level, entry.message, entry.throwable);
//            }
//        }
//    }
//
//    /**
//     * 실제 로그 출력
//     */
//    private void outputLog(LogLevel level, String message, Throwable throwable) {
//        switch (level) {
//            case VERBOSE:
//                if (throwable != null) {
//                    Log.v(TAG, message, throwable);
//                } else {
//                    Log.v(TAG, message);
//                }
//                break;
//            case DEBUG:
//                if (throwable != null) {
//                    Log.d(TAG, message, throwable);
//                } else {
//                    Log.d(TAG, message);
//                }
//                break;
//            case INFO:
//                if (throwable != null) {
//                    Log.i(TAG, message, throwable);
//                } else {
//                    Log.i(TAG, message);
//                }
//                break;
//            case WARN:
//                if (throwable != null) {
//                    Log.w(TAG, message, throwable);
//                } else {
//                    Log.w(TAG, message);
//                }
//                break;
//            case ERROR:
//                if (throwable != null) {
//                    Log.e(TAG, message, throwable);
//                } else {
//                    Log.e(TAG, message);
//                }
//                break;
//        }
//    }
//
//    /**
//     * 편의 메서드들
//     */
//    private void logInfo(LogCategory category, String message) {
//        log(LogLevel.INFO, category, message, null);
//    }
//
//    private void logDebug(LogCategory category, String message) {
//        log(LogLevel.DEBUG, category, message, null);
//    }
//
//    private void logWarn(LogCategory category, String message) {
//        log(LogLevel.WARN, category, message, null);
//    }
//
//    private void logError(LogCategory category, String message, Throwable throwable) {
//        log(LogLevel.ERROR, category, message, throwable);
//    }
//
//    /**
//     * 로그 배치 큐 정리
//     */
//    private void clearLogBatchQueue() {
//        synchronized (LOG_BATCH_LOCK) {
//            if (logBatchTask != null) {
//                mainHandler.removeCallbacks(logBatchTask);
//                logBatchTask = null;
//            }
//            // 남은 로그를 플러시
//            flushLogBatch();
//        }
//    }
//
//    /**
//     * 블루투스 검사 응답 신호 수신 로그 (공통 포맷)
//     */
//    private void logBtTestResponse(String command, String response, double electricValue,
//                                   int wattLower, int wattUpper, String messageInfo) {
//        String msg = String.format(Constants.LogMessages.TEST_ITEM_SIGNAL_RECEIVE + Constants.Common.EMPTY_STRING + "[T:%d/%d][C:%d/%s][P:%d/%d][S:%s][G:%d=%d-%d][R:%s]%s %d < %.0f < %d",
//                testTotalCounter, totalTimeCnt, testItemCounter,
//                (testItemIdx < arrTestItems.length && arrTestItems[testItemIdx] != null) ? arrTestItems[testItemIdx][2] : "0",
//                testItemIdx, arrTestItems.length,
//                currentTestItem, disconnectCheckCount, receivedMessageCnt, sendingMessageCnt,
//                readMessage, (messageInfo != null ? "[I:" + messageInfo + "]" : ""),
//                wattLower, electricValue, wattUpper);
//        logInfo(LogCategory.BT, msg);
//    }
//
//    /**
//     * 블루투스 검사 응답 신호 수신 로그 (간단한 버전)
//     */
//    private void logBtTestResponseSimple(String command, String response, String result) {
//        String deviceSec = "0";
//        if (arrTestItems != null && testItemIdx < arrTestItems.length && arrTestItems[testItemIdx] != null) {
//            deviceSec = arrTestItems[testItemIdx][2];
//        }
//        String msg = String.format("검사 응답 신호 수신 [T:%d/%d][C:%d/%s][P:%d/%d][S:%s][G:%d=%d-%d][R:%s] %s / %s",
//                testTotalCounter, totalTimeCnt, testItemCounter,
//                deviceSec,
//                testItemIdx, (arrTestItems != null) ? arrTestItems.length : 0,
//                currentTestItem, disconnectCheckCount, receivedMessageCnt, sendingMessageCnt,
//                command, response, result);
//        logInfo(LogCategory.BT, msg);
//    }
//
//    /**
//     * 모든 Timer와 TimerTask를 안전하게 정리합니다
//     * 메모리 누수 방지를 위해 Activity 종료 시 반드시 호출되어야 합니다
//     * <p>
//     * Phase 2: 각 Timer를 개별적으로 try-catch로 감싸서
//     * 하나의 Timer 정리 실패가 다른 Timer 정리를 방해하지 않도록 함
//     */
//    private void cleanupAllTimers() {
//        logInfo(LogCategory.PS, "Starting timer cleanup...");
//
//        // 1. USB 폴링 정리
//        try {
//            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> stopUsbPolling 1");
//            stopUsbPolling();
//        } catch (Exception e) {
//            logError(LogCategory.ER, "Error stopping USB polling", e);
//        }
//
//        // 2. tmrBTMessageSend 정리
//        try {
//            stopBtMessageTimer();
//        } catch (Exception e) {
//            logError(LogCategory.ER, "Error stopping BT message timer", e);
//        }
//
//        // 3. tmrFinishedRestart 정리 (Phase 3: synchronized 블록으로 동시성 제어)
//        try {
//            synchronized (finishedRestartTimerLock) {
//                if (finishedRestartTimerRunning.compareAndSet(true, false)) {
//                    if (tmrFinishedRestart != null) {
//                        tmrFinishedRestart.cancel();
//                        tmrFinishedRestart.purge();
//                        tmrFinishedRestart = null;
//                    }
//                    if (ttFinishedRestart != null) {
//                        ttFinishedRestart.cancel();
//                        ttFinishedRestart = null;
//                    }
//                } else {
//                    // 실행 중이 아니어도 정리 (안전을 위해)
//                    if (tmrFinishedRestart != null) {
//                        tmrFinishedRestart.cancel();
//                        tmrFinishedRestart.purge();
//                        tmrFinishedRestart = null;
//                    }
//                    if (ttFinishedRestart != null) {
//                        ttFinishedRestart.cancel();
//                        ttFinishedRestart = null;
//                    }
//                }
//            }
//        } catch (Exception e) {
//            logError(LogCategory.ER, "Error cleaning up finished restart timer", e);
//        }
//
//        // 4. tmrUnfinishedRestart 정리 (Phase 3: synchronized 블록으로 동시성 제어)
//        try {
//            synchronized (unfinishedRestartTimerLock) {
//                if (unfinishedRestartTimerRunning.compareAndSet(true, false)) {
//                    if (tmrUnfinishedRestart != null) {
//                        tmrUnfinishedRestart.cancel();
//                        tmrUnfinishedRestart.purge();
//                        tmrUnfinishedRestart = null;
//                    }
//                    if (ttUnfinishedRestart != null) {
//                        ttUnfinishedRestart.cancel();
//                        ttUnfinishedRestart = null;
//                    }
//                } else {
//                    // 실행 중이 아니어도 정리 (안전을 위해)
//                    if (tmrUnfinishedRestart != null) {
//                        tmrUnfinishedRestart.cancel();
//                        tmrUnfinishedRestart.purge();
//                        tmrUnfinishedRestart = null;
//                    }
//                    if (ttUnfinishedRestart != null) {
//                        ttUnfinishedRestart.cancel();
//                        ttUnfinishedRestart = null;
//                    }
//                }
//            }
//        } catch (Exception e) {
//            logError(LogCategory.ER, "Error cleaning up unfinished restart timer", e);
//        }
//
//        // 5. tmrRemoteCommand 정리 (미사용이지만 안전을 위해)
//        try {
//            if (tmrRemoteCommand != null) {
//                tmrRemoteCommand.cancel();
//                tmrRemoteCommand.purge();
//                tmrRemoteCommand = null;
//            }
//            if (ttRemoteCommand != null) {
//                ttRemoteCommand.cancel();
//                ttRemoteCommand = null;
//            }
//        } catch (Exception e) {
//            logError(LogCategory.ER, "Error cleaning up remote command timer", e);
//        }
//
//        // 6. checkDuration Timer 정리 (Phase 1: static → instance로 변경됨)
//        try {
//            if (checkDuration != null) {
//                checkDuration.cancel();
//                checkDuration.purge();
//                checkDuration = null;
//            }
//        } catch (Exception e) {
//            logError(LogCategory.ER, "Error cleaning up checkDuration timer", e);
//        }
//
//        // 7. barcodeRequestTimer 정리 (Phase 1: static → instance로 변경됨)
//        try {
//            if (barcodeRequestTimer != null) {
//                barcodeRequestTimer.cancel();
//                barcodeRequestTimer.purge();
//                barcodeRequestTimer = null;
//            }
//        } catch (Exception e) {
//            logError(LogCategory.ER, "Error cleaning up barcode request timer", e);
//        }
//
//        // 8. appResetTimerTask 정리
//        try {
//            if (appResetTimerTask != null) {
//                appResetTimerTask.cancel();
//                appResetTimerTask = null;
//            }
//        } catch (Exception e) {
//            logError(LogCategory.ER, "Error cleaning up app reset timer task", e);
//        }
//
//        logInfo(LogCategory.PS, "All timers cleaned up successfully");
//    }
//
//    private void runOnBtWorker(Runnable task) {
//        if (task == null) {
//            return;
//        }
//        btWorkerExecutor.execute(task);
//    }
//
//    private String[] getRequiredPermissions() {
//        // Android 버전에 따라 블루투스 권한과 미디어/스토리지 권한을 결합
//        String[] btPermissions = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
//                ? MODERN_BT_PERMISSIONS
//                : LEGACY_BT_PERMISSIONS;
//
//        String[] mediaPermissions;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            // Android 13 (API 33) 이상: 미디어 권한
//            mediaPermissions = MODERN_MEDIA_PERMISSIONS;
//        } else {
//            // Android 12 이하: 스토리지 권한
//            mediaPermissions = LEGACY_STORAGE_PERMISSIONS;
//        }
//
//        // 두 배열을 결합
//        String[] allPermissions = new String[btPermissions.length + mediaPermissions.length];
//        System.arraycopy(btPermissions, 0, allPermissions, 0, btPermissions.length);
//        System.arraycopy(mediaPermissions, 0, allPermissions, btPermissions.length, mediaPermissions.length);
//
//        return allPermissions;
//    }
//
//    private boolean hasBluetoothConnectPermission() {
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
//            // 해당 권한이 존재하지 않으므로 항상 true로 간주
//            return true;
//        }
//        return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
//    }
//
//    private boolean hasAllBluetoothPermissions() {
//        for (String permission : getRequiredPermissions()) {
//            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
//                return false;
//            }
//        }
//        return true;
//    }
//
//    private boolean hasBluetoothScanPermission() {
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
//            return true;
//        }
//        return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
//    }
//
//    private boolean isBluetoothAdapterEnabled() {
//        if (mBTAdapter == null) {
//            return false;
//        }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasBluetoothConnectPermission()) {
//            return false;
//        }
//        return mBTAdapter.isEnabled();
//    }
//
//    @SuppressLint("MissingPermission")
//    private Set<BluetoothDevice> getBondedDevicesSafe() {
//        if (mBTAdapter == null) {
//            return Collections.emptySet();
//        }
//        if (!hasBluetoothConnectPermission()) {
//            logWarn(LogCategory.BT, "Missing BLUETOOTH_CONNECT permission; cannot read bonded devices");
//            ensureBtPermissions(PermissionAction.LIST_PAIRED_DEVICES);
//            return Collections.emptySet();
//        }
//        try {
//            return mBTAdapter.getBondedDevices();
//        } catch (SecurityException e) {
//            logError(LogCategory.ER, "Failed to obtain bonded devices", e);
//            return Collections.emptySet();
//        }
//    }
//
//    private void cancelDiscoverySafe() {
//        if (mBTAdapter == null) {
//            return;
//        }
//        if (!hasBluetoothScanPermission()) {
//            logWarn(LogCategory.BT, "Missing BLUETOOTH_SCAN permission; cannot cancel discovery");
//            ensureBtPermissions(null);
//            return;
//        }
//        try {
//            mBTAdapter.cancelDiscovery();
//        } catch (SecurityException e) {
//            logError(LogCategory.ER, "Failed to cancel discovery safely", e);
//        }
//    }
//
//    private boolean ensureBtPermissions(PermissionAction actionOnGrant) {
//        if (hasAllBluetoothPermissions()) {
//            btPermissionsGranted = true;
//            return true;
//        }
//        if (actionOnGrant != null) {
//            pendingPermissionActions.add(actionOnGrant);
//        }
//        requestRuntimePermissions();
//        return false;
//    }
//
//    private void requestRuntimePermissions() {
//        if (permissionRequestInProgress) {
//            logInfo(LogCategory.BT, "Permission request already in progress; skipping duplicate request");
//            return;
//        }
//        List<String> missingPermissions = new ArrayList<>();
//        for (String permission : getRequiredPermissions()) {
//            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
//                missingPermissions.add(permission);
//            }
//        }
//        if (missingPermissions.isEmpty()) {
//            permissionDenialCount = 0; // 권한이 이미 허용되었으면 거부 횟수 리셋
//            btPermissionsGranted = true;
//            runPendingPermissionActions();
//            return;
//        }
//        permissionRequestInProgress = true;
//        ActivityCompat.requestPermissions(this, missingPermissions.toArray(new String[0]), MULTIPLE_PERMISSIONS);
//    }
//
//    private void runPendingPermissionActions() {
//        if (pendingPermissionActions.isEmpty()) {
//            return;
//        }
//        EnumSet<PermissionAction> actions = EnumSet.copyOf(pendingPermissionActions);
//        pendingPermissionActions.clear();
//        if (actions.contains(PermissionAction.LIST_PAIRED_DEVICES)) {
//            listPairedDevicesSelect();
//        }
//        if (actions.contains(PermissionAction.BT_RECONNECT)) {
//            scheduleBluetoothReconnect(true);
//        }
//    }
//
//    /**
//     * USB 연결은 유지한 채 블루투스 세션만 초기화하여 다음 테스트를 준비합니다.
//     */
//    private void resetBluetoothSessionKeepUsb() {
////         if(usbReconnectAttempts>0) {
////             return;
////         }
////        logInfo(LogCategory.PS, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> resetBluetoothSessionKeepUsb");
//        processFinished = false;
//        processFinishedTmp = false;
//        entranceCheck = false;
//        btSearchOnOff = false;
//        btConnected = false;
//        btConnectionInProgress = false;
//        isConnected = false;
//        btReconnectDelayMs = Constants.Timeouts.BT_LIST_TIMER_INTERVAL_MS;
//        currentTestItem = Constants.TestItemCodes.ST0101;
//        testItemIdx = 0;
//        testItemCounter = 0;
//        testTotalCounter = 0;
//        receiveCommand = "";
//        receiveCompAgingResponse = Constants.ResultStatus.COMP_AGING_RESPONSE_01;
//        sensorNgCount = 0;
//        blnBarcodeReceived = false;
//        restartCntFinished = 0;
//        restartCntUnfinished = 0;
//        finishedCorrectly = false;
//        sendResultYn = Constants.InitialValues.SEND_RESULT_YN;
//        decTemperatureValueCompStart = "";
//        decTemperatureValueCompEnd = "";
//        temperatureValueCompDiff = "";
//        testProcessId = Constants.InitialValues.TEST_PROCESS_ID;
//        globalLastTestStartTimestamp = lmsTestSeq;
//        shouldUpdateDialog = false;
//        receiveCommandEmptyCnt = 0;
//        receivedMessageCnt = 0;
//        sendingMessageCnt = 0;
//
//        lstMapWattTemp.clear();
//        lstWatt.clear();
//
//        cleanupAllTimers();
//        stopBtMessageTimer();
//        clearBluetoothReconnect();
//        if (btConnectedThread != null) {
//            try {
//                btConnectedThread.cancel();
//            } catch (Exception ignored) {
//            }
//            btConnectedThread = null;
//        }
//        closeSocketSilently(btSocket);
//        btSocket = null;
//        deviceSelected = null;
//
//        // 복잡한 UI 업데이트 블록 통합: 여러 View 업데이트를 scheduleUiUpdate로 변경
//        scheduleUiUpdate(() -> {
//            // tvConnectBtRamp.setText(Constants.LogMessages.NOT_CONNECTED_BLUETOOTH_DEVICE);
//            tvConnectBtRamp.setBackgroundColor(getColor(R.color.red_01));
//            tvTestOkCnt.setText("0");
//            tvTestNgCnt.setText("0");
//            tvTemperature.setText("--");
//            tvCompWattValue.setText("0");
//            tvHeaterWattValue.setText("0");
//            tvEllapsedTimeCnt.setText("0");
//            tvPumpWattValue.setText("0");
//            tvLog.setText("");
//            clAlert.setVisibility(GONE);
//            clTestResult.setVisibility(View.GONE);
//            clDialogForPreprocess.setVisibility(GONE);
//            tvPopupProcessResult.setText(Constants.Common.EMPTY_STRING);
//            tvPopupProcessResultCompValue.setText(Constants.Common.EMPTY_STRING);
//            tvPopupProcessResultHeaterValue.setText(Constants.Common.EMPTY_STRING);
//            // tvConnectPlcRamp.setText("");
//            tvConnectPlcRamp.setBackgroundColor(getColor(R.color.red_01));
//            // tvRunWsRamp.setText("");
//            tvRunWsRamp.setBackgroundColor(getColor(R.color.red_01));
//            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> testOkCnt " + testOkCnt + " testNgCnt " + testNgCnt);
//            if (testOkCnt == 0 && testNgCnt == 0) {
//                clTestResult.setVisibility(android.view.View.GONE);
//            } else {
//                tvPopupProcessResult.setText(Constants.ResultStatus.NG);
//                tvPopupProcessResult.setTextSize(Dimension.SP, Constants.UI.TEXT_SIZE_LARGE_SP);
//                tvPopupProcessResultCompValue.setTextSize(Dimension.SP, Constants.UI.TEXT_SIZE_MEDIUM_SP);
//                tvPopupProcessResultHeaterValue.setTextSize(Dimension.SP, Constants.UI.TEXT_SIZE_MEDIUM_SP);
//                SharedPreferences testPrefs = getSharedPreferences(Constants.SharedPrefKeys.TEST_INFO, MODE_PRIVATE);
//                String testResult = testPrefs.getString(Constants.SharedPrefKeys.TEST_RESULT, Constants.Common.EMPTY_STRING);
//                String heaterValue = testPrefs.getString(Constants.SharedPrefKeys.HEATER_VALUE, Constants.Common.EMPTY_STRING);
//                String compValue = testPrefs.getString(Constants.SharedPrefKeys.COMP_VALUE, Constants.Common.EMPTY_STRING);
//                tvPopupProcessResult.setText(testResult);
//                tvPopupProcessResultCompValue.setText(compValue);
//                tvPopupProcessResultHeaterValue.setText(heaterValue);
//                clTestResult.setVisibility(android.view.View.VISIBLE);
//            }
//        });
//
//        // rebuildTestItemList();
//        rebuildTestItemListAsync();
//        restartUsbMonitoring();   // ▼ 새 헬퍼 호출
//        // btSearchOnOff = true;
//        // bluetoothSearch();
//        btReconnectDelayMs = Constants.Timeouts.BT_LIST_TIMER_INTERVAL_MS; // 예: 기존 값의 1/4
//        btSearchOnOff = true;
//        scheduleBluetoothReconnect(true);
//
//        // 버전 정보 가져오기
//        String localVersionId = null;
//        String localModelId = null;
//
//        /*
//        for(int i=0; i<arrTestModels[0].length; i++) {
//            logInfo(LogCategory.PS, "> arrTestModels[0][" + i + "]: " + arrTestModels[0][i]);
//        }
//        */
//
//        if (arrTestModels != null && arrTestModels[0] != null && arrTestModels[0].length > 4) {
//            localVersionId = arrTestModels[0][3]; // CLM_TEST_VERSION_ID
//            localModelId = arrTestModels[0][4]; // CLM_MODEL_VERSION
//        }
//        localVersionId = test_version_id;
//        localModelId = model_id;
//        // logInfo(LogCategory.PS, "Local Version ID: " + localVersionId + ", Local Model Version: " + localModelVersion);
//
//        if (modeType.equals(Constants.ResultStatus.MODE_TYPE_TEST)) {
//            // VersionInfoList 호출하여 네트워크 상태 체크 및 버전 비교
//            if (localVersionId != null || localModelId != null) {
//                new ActivityModel_0003.RequestVersionInfoThreadAsync(localVersionId, localModelId).execute();
//            }
//        }
//    }
//
//    private void restartUsbMonitoring() {
//        // 1) 서비스가 내려가 있으면 다시 바인드
//        if (usbService == null || !UsbService.SERVICE_CONNECTED) {
//            startService(UsbService.class, usbConnection, null);
//        }
//        // 2) 권한이 없으면 다시 요청
//        if (!usbConnPermissionGranted) {
//            usbSearch();
//            return; // 권한 승인 브로드캐스트에서 polling이 재개됨
//        }
//        // 3) 권한/서비스 모두 OK면 바로 폴링 재시작
//        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> startUsbPolling 2");
//        startUsbPolling(true);
//        // Phase 2: 단순 UI 업데이트를 scheduleUiUpdate로 변경
//        scheduleUiUpdate(() -> {
//            tvConnectPlcRamp.setBackgroundColor(getColor(R.color.blue_01));
//            // tvConnectPlcRamp.setText(Constants.InitialValues.DEVICE_READY_CHARACTER);
//        });
//    }
//
//    private void rebuildTestItemListAsync() {
//        runOnBtWorker(() -> {
//            if (arrTestItems == null || arrTestItems.length == 0) {
//                return;
//            }
//
//            List<VoTestItem> newItems = new ArrayList<>();
//            int total = 0;
//            for (int i = 0; i < arrTestItems.length; i++) {
//                int seconds = safeParseInt(arrTestItems[i][2]);
//                total += (seconds > 1) ? seconds + 1 : seconds;
//
//                Map<String, String> map = new HashMap<>();
//                map.put(Constants.Common.TEST_ITEM_SEQ, String.valueOf(i + 1));
//                map.put(Constants.Common.TEST_ITEM_NAME, arrTestItems[i][0]);
//                map.put(Constants.Common.TEST_ITEM_COMMAND, arrTestItems[i][1]);
//                map.put(Constants.Common.TEST_RESPONSE_VALUE, arrTestItems[i][7]);
//                map.put(Constants.Common.TEST_ITEM_RESULT, getString(R.string.txt_pre_process));
//                map.put(Constants.Common.TEST_FINISH_YN, Constants.ResultStatus.NO);
//                map.put(Constants.Common.TEST_MODEL_ID, globalModelId);
//                newItems.add(new VoTestItem(map));
//            }
//            final int totalTime = total;
//
//            scheduleUiUpdate(() -> {
//                totalTimeCnt = totalTime;
//                listItemAdapter.clear();
//                for (VoTestItem item : newItems) {
//                    listItemAdapter.addItem(item);
//                }
//                tvTotalTimeCnt.setText(String.valueOf(totalTimeCnt));
//                lvTestItem.setAdapter(listItemAdapter);
//            });
//        });
//    }
//
//    private int safeParseInt(String value) {
//        if (value == null) {
//            return 0;
//        }
//        try {
//            return Integer.parseInt(value.trim());
//        } catch (NumberFormatException ignored) {
//            return 0;
//        }
//    }
//
//    private void rebuildTestItemList() {
//        if (Looper.myLooper() != Looper.getMainLooper()) {
//            runOnUiThread(this::rebuildTestItemList);
//            return;
//        }
//        if (arrTestItems == null || arrTestItems.length == 0) {
//            return;
//        }
//        if (listItemAdapter == null) {
//            listItemAdapter = new ItemAdapterTestItem();
//        } else {
//            listItemAdapter.clear();
//        }
//
//        totalTimeCnt = 0;
//        for (int i = 0; i < arrTestItems.length; i++) {
//            try {
//                int seconds = Integer.parseInt(arrTestItems[i][2]);
//                totalTimeCnt += (seconds > 1) ? seconds + 1 : seconds;
//                arrTestItems[i][3] = String.valueOf(totalTimeCnt);
//            } catch (Exception ignored) {
//            }
//
//            Map<String, String> mapListItem = new HashMap<>();
//            mapListItem.put(Constants.Common.TEST_ITEM_SEQ, String.valueOf(i + 1));
//            mapListItem.put(Constants.Common.TEST_ITEM_NAME, arrTestItems[i][0]);
//            mapListItem.put(Constants.Common.TEST_ITEM_COMMAND, arrTestItems[i][1]);
//            mapListItem.put(Constants.Common.TEST_RESPONSE_VALUE, arrTestItems[i][7]);
//            mapListItem.put(Constants.Common.TEST_ITEM_RESULT, getString(R.string.txt_pre_process));
//            mapListItem.put(Constants.Common.TEST_FINISH_YN, Constants.ResultStatus.NO);
//            mapListItem.put(Constants.Common.TEST_MODEL_ID, globalModelId);
//            listItemAdapter.addItem(new VoTestItem(mapListItem));
//        }
//        lastTestIdx = listItemAdapter.getCount();
//
//        // Phase 2: 단순 UI 업데이트를 scheduleUiUpdate로 변경
//        scheduleUiUpdate(() -> {
//            tvTotalTimeCnt.setText(String.valueOf(totalTimeCnt));
//            lvTestItem.setAdapter(listItemAdapter);
//        });
//    }
//
//    private void setupResourceInfoOverlay() {
//        if (!Constants.ResultStatus.MODE_TYPE_TEST.equals(modeType) || tvResourceInfo != null) {
//            return;
//        }
//        FrameLayout content = findViewById(android.R.id.content);
//        if (content == null) {
//            return;
//        }
//        tvResourceInfo = new TextView(this);
//        tvResourceInfo.setTextColor(Color.WHITE);
//        tvResourceInfo.setBackgroundColor(0xAA000000);
//        tvResourceInfo.setTextSize(10f);
//        tvResourceInfo.setPadding(16, 8, 16, 8);
//        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
//                ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.WRAP_CONTENT
//        );
//        params.gravity = Gravity.BOTTOM;
//        content.addView(tvResourceInfo, params);
//        mainHandler.post(resourceInfoRunnable);
//    }
//
//    private void updateResourceInfo() {
//        if (!Constants.ResultStatus.MODE_TYPE_TEST.equals(modeType) || tvResourceInfo == null) {
//            return;
//        }
//        long nativeHeap = Debug.getNativeHeapAllocatedSize() / 1024;
//        Runtime runtime = Runtime.getRuntime();
//        long javaUsed = (runtime.totalMemory() - runtime.freeMemory()) / 1024;
//        long javaTotal = runtime.totalMemory() / 1024;
//        String info = String.format("Native: %d KB | Java: %d/%d KB", nativeHeap, javaUsed, javaTotal);
//        tvResourceInfo.setText(info);
//    }
//
//    private void showPermissionPrompt(boolean permanentlyDenied) {
//        if (permissionDialogShowing || isFinishing()) {
//            return;
//        }
//        permissionDialogShowing = true;
//        // Dialog는 즉시 표시가 필요하므로 scheduleUiUpdateImmediate 사용
//        scheduleUiUpdateImmediate(() -> {
//            if (isFinishing() || isDestroyed()) {
//                permissionDialogShowing = false;
//                return;
//            }
//
//            // 영구적으로 거부되었거나 재시도 횟수가 초과된 경우 설정 화면으로 안내
//            boolean shouldDirectToSettings = permanentlyDenied || permissionDenialCount >= MAX_PERMISSION_DENIAL_COUNT;
//
//            String title = "블루투스 권한 필요";
//            String message;
//            if (shouldDirectToSettings) {
//                message = "블루투스 권한이 필요합니다.\n\n" +
//                        "시스템에서 권한 요청이 차단되었습니다.\n\n" +
//                        "설정 화면에서 직접 권한을 허용해주세요.\n\n" +
//                        "설정 화면으로 이동하시겠습니까?";
//            } else {
//                message = "블루투스 권한이 거부되었습니다.\n\n" +
//                        "권한을 허용해야 기기가 정상 동작합니다.\n\n" +
//                        "아래 '권한 허용' 버튼을 눌러 권한을 허용해주세요.\n" +
//                        "(재시도: " + permissionDenialCount + "/" + MAX_PERMISSION_DENIAL_COUNT + ")";
//            }
//
//            AlertDialog.Builder builder = new AlertDialog.Builder(this)
//                    .setTitle(title)
//                    .setMessage(message)
//                    .setCancelable(false)
//                    .setNegativeButton("종료", (dialog, which) -> {
//                        permissionDialogShowing = false;
//                        finish();
//                    });
//
//            if (shouldDirectToSettings) {
//                // 설정 화면으로 이동
//                builder.setPositiveButton("설정 열기", (dialog, which) -> {
//                    permissionDialogShowing = false;
//                    openAppSettings();
//                });
//            } else {
//                // 권한 요청 재시도
//                builder.setPositiveButton("권한 허용", (dialog, which) -> {
//                    permissionDialogShowing = false;
//                    // 권한 요청을 다시 시도
//                    requestRuntimePermissions();
//                });
//            }
//
//            AlertDialog dialog = builder.create();
//            dialog.setOnDismissListener(d -> permissionDialogShowing = false);
//            dialog.show();
//        });
//    }
//
//    private void openAppSettings() {
//        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//        intent.setData(Uri.fromParts("package", getPackageName(), null));
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        startActivity(intent);
//    }
//
//    private void startUsbPolling(boolean immediate) {
//        if (usbPollingExecutor.isShutdown()) {
//            logWarn(LogCategory.US, "USB polling executor is shut down; cannot schedule polling");
//            return;
//        }
//        if (usbService == null) {
//            logWarn(LogCategory.US, "UsbService is null; skipping polling start");
//            return;
//        }
//        if (!usbConnPermissionGranted) {
//            logWarn(LogCategory.US, "USB permission not granted; skipping polling start");
//            scheduleUsbPermissionRecovery();
//            return;
//        }
//        boolean pollingActive = usbPollingEnabled && usbPollingFuture != null && !usbPollingFuture.isCancelled();
//        if (pollingActive) {
//            logDebug(LogCategory.US, "USB polling already running; skipping restart");
//            return;
//        }
//        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> stopUsbPolling 2");
//        stopUsbPolling();
//        usbPollingEnabled = true;
//        usbPollingRequested = true;
//        usbPollingFailureCount = 0;
//        long initialDelay = immediate ? 0 : usbPollingIntervalMs;
//        try {
//            usbPollingFuture = usbPollingExecutor.scheduleAtFixedRate(() -> {
//                if (!usbPollingEnabled) {
//                    System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> stopUsbPolling 3");
//                    stopUsbPolling();
//                    return;
//                }
//                if (usbService == null) {
//                    logWarn(LogCategory.US, "UsbService became null during polling; stopping polling");
//                    System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> stopUsbPolling 4");
//                    stopUsbPolling();
//                    return;
//                }
//                try {
//                    plcCommand = Constants.PLCCommands.RSS0107_DW1006;
//                    usbService.write(plcCommand.getBytes());
//                    usbPollingFailureCount = 0;
//                } catch (Exception e) {
//                    usbPollingFailureCount++;
//                    logError(LogCategory.ER, Constants.ErrorMessages.USB_SERVICE_ERROR, e);
//                    if (usbPollingFailureCount >= USB_POLLING_FAILURE_THRESHOLD) {
//                        logWarn(LogCategory.US, "USB polling failure threshold reached; backing off");
//                        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> stopUsbPolling 5");
//                        stopUsbPolling();
//                        usbPollingIntervalMs = USB_POLLING_BACKOFF_MS;
//                        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> startUsbPolling 3");
//                        startUsbPolling(false);
//                    }
//                }
//            }, initialDelay, usbPollingIntervalMs, TimeUnit.MILLISECONDS);
//            logInfo(LogCategory.US, "USB polling scheduled (interval: " + usbPollingIntervalMs + " ms)");
//        } catch (Exception e) {
//            logError(LogCategory.ER, "Failed to schedule USB polling", e);
//            usbPollingEnabled = false;
//            usbPollingRequested = false;
//        }
//    }
//
//    private void stopUsbPolling() {
//        usbPollingEnabled = false;
//        usbPollingRequested = false;
//        usbPollingIntervalMs = Constants.Timeouts.USB_TIMER_INTERVAL_MS;
//        if (usbPollingFuture != null) {
//            usbPollingFuture.cancel(true);
//            usbPollingFuture = null;
//            logInfo(LogCategory.US, "USB polling stopped");
//        }
//    }
//
//    private void scheduleUsbReconnect(boolean immediate) {
//        if (isUsbReconnecting) {
//            return;
//        }
//        isUsbReconnecting = true;
//        if (usbReconnectRunnable == null) {
//            usbReconnectRunnable = this::attemptUsbReconnect;
//        }
//        usbReconnectHandler.removeCallbacks(usbReconnectRunnable);
//        usbReconnectHandler.postDelayed(usbReconnectRunnable, immediate ? 0 : USB_PERMISSION_RECOVERY_DELAY_MS);
//        updateUsbLampReconnecting();
//    }
//
//    private void cancelUsbReconnect() {
//        usbReconnectHandler.removeCallbacks(usbReconnectRunnable);
//        isUsbReconnecting = false;
//    }
//
//    private void attemptUsbReconnect() {
//        if (!isUsbReconnecting) {
//            return;
//        }
//        boolean success = tryReconnectUsb();
//        if (success) {
//            cancelUsbReconnect();
//            usbReconnectAttempts = 0;
//            updateUsbLampReady();
//            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> startUsbPolling 4");
//            startUsbPolling(true);
//            return;
//        }
//        usbReconnectAttempts++;
//        if (usbReconnectAttempts >= USB_RETRY_MAX_ATTEMPTS) {
//            usbReconnectAttempts = 0;
//            logWarn(LogCategory.US, "USB reconnect failed after " + usbReconnectAttempts + " attempts");
//            cancelUsbReconnect();
//            scheduleUsbPermissionRecovery();
//            scheduleUiUpdate(() -> {
//                updateUsbLampDisconnected();
//                clAlert.setVisibility(VISIBLE);
//                tvAlertMessage.setText("USB 또는 블루투스 연결을 확인해주세요.");    // USB 또는 블루투스 연결을 확인해주세요.
//                resetBluetoothSessionKeepUsb();
//            });
//        } else {
//            usbReconnectHandler.postDelayed(usbReconnectRunnable,
//                    USB_PERMISSION_RECOVERY_DELAY_MS * Math.min(usbReconnectAttempts, 5));
//        }
//    }
//
//    private boolean tryReconnectUsb() {
//        try {
//            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> stopUsbPolling 6");
//            stopUsbPolling();
//            setFilters();
//            startService(UsbService.class, usbConnection, null);
//            return usbConnPermissionGranted && usbService != null;
//        } catch (Exception e) {
//            logError(LogCategory.US, "USB reconnect attempt failed", e);
//            return false;
//        }
//    }
//
//    private boolean isUsbReady() {
//        System.out.println(">>>>>>>>>>>>>>>>>>> usbConnPermissionGranted " + usbConnPermissionGranted + " / usbService " + usbService + " / usbPollingEnabled " + usbPollingEnabled);
//        if (usbConnPermissionGranted) {
//            // stopBtMessageTimer();
//            tmrReset = new Timer();
//            tmrBTMessageSend = new Timer("BtMsgTimer");
//            resetCnt = 0;
//            usbReconnectAttempts = 0;
//            disconnectCheckCount = 0;
//            receivedMessageCnt = 0;
//            sendingMessageCnt = 0;
//        }
//        return usbConnPermissionGranted && usbService != null && usbPollingEnabled;
//    }
//
//    private boolean isBluetoothReady() {
//        return btConnected && btSocket != null && btSocket.isConnected();
//    }
//
//    private boolean canEnterTest() {
//        System.out.println(">>>>>>>>>>>>>>>>>>> isUsbReady() " + isUsbReady() + " / isBluetoothReady() " + isBluetoothReady());
//        return isUsbReady() && isBluetoothReady();
//    }
//
//    private void updateUsbLampDisconnected() {
//        scheduleUiUpdate(() -> {
//            tvConnectPlcRamp.setBackgroundColor(getColor(R.color.red_01));
//            // tvConnectPlcRamp.setText("USB OFF");
//        });
//    }
//
//    private void updateUsbLampReconnecting() {
//        scheduleUiUpdate(() -> {
//            tvConnectPlcRamp.setBackgroundColor(getColor(R.color.orange_01));
//            // tvConnectPlcRamp.setText("USB RETRY");
//        });
//    }
//
//    private void updateUsbLampReady() {
//        scheduleUiUpdate(() -> {
//            tvConnectPlcRamp.setBackgroundColor(getColor(R.color.blue_01));
//            // tvConnectPlcRamp.setText(Constants.InitialValues.DEVICE_READY_CHARACTER);
//        });
//    }
//
//
//    private void recordWattMeasurement(String command, double wattValue) {
//        if (TextUtils.isEmpty(command)) {
//            return;
//        }
//        String key = canonicalWattKey(command);
//        if (TextUtils.isEmpty(key)) {
//            return;
//        }
//        latestWattByCommand.put(key, wattValue);
//    }
//
//    private Double getLatestWattMeasurement(String command) {
//        if (TextUtils.isEmpty(command)) {
//            return null;
//        }
//        String key = canonicalWattKey(command);
//        return TextUtils.isEmpty(key) ? null : latestWattByCommand.get(key);
//    }
//
//    private String canonicalWattKey(String command) {
//        if (TextUtils.isEmpty(command)) {
//            return null;
//        }
//        switch (command) {
//            case Constants.TestItemCodes.CM0102:
//                return Constants.TestItemCodes.CM0101;
//            case Constants.TestItemCodes.HT0100:
//                return Constants.TestItemCodes.HT0101;
//            case Constants.TestItemCodes.SV0201:
//            case Constants.TestItemCodes.SV0301:
//            case Constants.TestItemCodes.SV0401:
//                return Constants.TestItemCodes.SV0101;
//            default:
//                return command;
//        }
//    }
//
//    private boolean isWattCommand(String command) {
//        if (TextUtils.isEmpty(command)) {
//            return false;
//        }
//        switch (canonicalWattKey(command)) {
//            case Constants.TestItemCodes.CM0101:
//            case Constants.TestItemCodes.HT0101:
//            case Constants.TestItemCodes.PM0101:
//            case Constants.TestItemCodes.SV0101:
//                return true;
//            default:
//                return false;
//        }
//    }
//
////    private String formatWattValue(double value) {
////        return String.valueOf((int) Math.round(value));
////    }
//
//    private String ensureComponentWattText(String currentText, String... commandKeys) {
//        if (!TextUtils.isEmpty(currentText)) {
//            return currentText;
//        }
//        if (commandKeys == null) {
//            return currentText;
//        }
//        for (String key : commandKeys) {
//            Double cached = getLatestWattMeasurement(key);
//            if (cached != null) {
//                return formatWattValue(cached);
//            }
//        }
//        return currentText;
//    }
//
//    private String ensureWattInfo(String currentInfo, String commandKey) {
//        if (!TextUtils.isEmpty(currentInfo) || !isWattCommand(commandKey)) {
//            return currentInfo;
//        }
//        Double cached = getLatestWattMeasurement(commandKey);
//        if (cached == null) {
//            return currentInfo;
//        }
//        return formatWattValue(cached) + Constants.Common.WATT_UNIT;
//    }
//
//    private void scheduleUsbPermissionRecovery() {
//        if (usbConnPermissionGranted) {
//            logDebug(LogCategory.US, "USB permission already granted; skipping recovery scheduling");
//            return;
//        }
//
//        if (usbPermissionRecoveryRunnable == null) {
//            usbPermissionRecoveryRunnable = () -> {
//                if (usbConnPermissionGranted) {
//                    logDebug(LogCategory.US, "USB permission granted before recovery runnable executed");
//                    return;
//                }
//                scheduleUiUpdate(() -> {
//                    final int finalUsbStatusColor = R.color.red_01;
//                    // final String finalUsbStatusText = "";
//                    tvConnectPlcRamp.setBackgroundColor(getColor(finalUsbStatusColor));
//                    // tvConnectPlcRamp.setText(finalUsbStatusText);
//                });
//                logInfo(LogCategory.US, "Attempting USB permission recovery");
//                try {
//                    setFilters();
//                } catch (Exception e) {
//                    logError(LogCategory.ER, "Error while setting USB filters during recovery", e);
//                }
//
//                new Thread(() -> {
//                    try {
//                        startService(UsbService.class, usbConnection, null);
//                    } catch (Exception e) {
//                        logError(LogCategory.ER, "Error while rebinding USB service during recovery", e);
//                    }
//                }).start();
//            };
//        }
//
//        usbRecoveryHandler.removeCallbacks(usbPermissionRecoveryRunnable);
//        usbRecoveryHandler.postDelayed(usbPermissionRecoveryRunnable, USB_PERMISSION_RECOVERY_DELAY_MS);
//    }
//
//    private void recordWattForCommand(String command, double watt) {
//        if (TextUtils.isEmpty(command)) {
//            return;
//        }
//        latestWattByCommand.put(command, watt);
//    }
//
//    private boolean isWattTrackingCommand(String command) {
//        return command != null && WATT_TRACKING_COMMANDS.contains(command);
//    }
//
//    private boolean isSolValveCommand(String command) {
//        return command != null && SOL_VALVE_COMMANDS.contains(command);
//    }
//
//    private String ensureWattText(String command, String currentText) {
//        if (!TextUtils.isEmpty(currentText)) {
//            return currentText;
//        }
//        if (TextUtils.isEmpty(command)) {
//            return currentText;
//        }
//        Double cached = latestWattByCommand.get(command);
//        if (cached == null) {
//            return currentText;
//        }
//        return formatWattValue(cached);
//    }
//
//    private String formatWattValue(double watt) {
//        double rounded = Math.rint(watt);
//        if (Math.abs(watt - rounded) < 0.01d) {
//            return String.valueOf((int) rounded);
//        }
//        return String.format(Locale.US, "%.1f", watt);
//    }
//
//    private void scheduleBluetoothReconnect(boolean immediate) {
//        if (!btSearchOnOff) {
//            return;
//        }
//        if (btReconnectRunnable == null) {
//            btReconnectRunnable = this::attemptBluetoothReconnect;
//        }
//        schedulerHandler.removeCallbacks(btReconnectRunnable);
//        long delay = immediate ? 0 : btReconnectDelayMs;
//        schedulerHandler.postDelayed(btReconnectRunnable, delay);
//    }
//
//    private void clearBluetoothReconnect() {
//        if (btReconnectRunnable != null) {
//            schedulerHandler.removeCallbacks(btReconnectRunnable);
//        }
//    }
//
//    @SuppressLint("MissingPermission")
//    private void attemptBluetoothReconnect() {
//        if (!btSearchOnOff || btConnectionInProgress || btConnected) {
//            return;
//        }
//        if (mBTAdapter == null) {
//            logWarn(LogCategory.BT, "Bluetooth adapter null, skipping reconnect");
//            return;
//        }
//
//        if (!hasBluetoothConnectPermission()) {
//            logWarn(LogCategory.BT, "BLUETOOTH_CONNECT permission missing; cannot attempt reconnect");
//            ensureBtPermissions(PermissionAction.BT_RECONNECT);
//            return;
//        }
//
//        if (!mBTAdapter.isEnabled()) {
//            logWarn(LogCategory.BT, "Bluetooth adapter disabled, skipping reconnect");
//            return;
//        }
//
//        if (mPairedDevices == null || mPairedDevices.isEmpty()) {
//            mPairedDevices = getBondedDevicesSafe();
//        }
//
//        if (mPairedDevices == null || mPairedDevices.isEmpty()) {
//            logInfo(LogCategory.BT, "No paired Bluetooth devices available");
//            btReconnectDelayMs = Math.min(btReconnectDelayMs, BT_RECONNECT_DELAY_MAX_MS);
//            scheduleBluetoothReconnect(false);
//            return;
//        }
//
//        BluetoothDevice targetDevice = selectPreferredDevice(mPairedDevices);
//        if (targetDevice == null) {
//            btReconnectDelayMs = Math.min(btReconnectDelayMs, BT_RECONNECT_DELAY_MAX_MS);
//            scheduleBluetoothReconnect(false);
//            return;
//        }
//
//        btConnectionInProgress = true;
//        runOnBtWorker(() -> {
//            boolean success = connectToDevice(targetDevice);
//            btConnectionInProgress = false;
//            if (success) {
//                btReconnectDelayMs = Constants.Timeouts.BT_LIST_TIMER_INTERVAL_MS;
//            } else {
//                btReconnectDelayMs = Math.min(btReconnectDelayMs, BT_RECONNECT_DELAY_MAX_MS);
//                scheduleBluetoothReconnect(false);
//            }
//        });
//    }
//
//    private BluetoothDevice selectPreferredDevice(Set<BluetoothDevice> devices) {
//        if (devices == null || devices.isEmpty()) {
//            return null;
//        }
//
//        if (deviceSelected != null) {
//            for (BluetoothDevice device : devices) {
//                if (device != null && deviceSelected.getAddress().equals(device.getAddress())) {
//                    return device;
//                }
//            }
//        }
//
//        for (BluetoothDevice device : devices) {
//            if (device != null) {
//                return device;
//            }
//        }
//        return null;
//    }
//
//    private void scheduleConnectionTimeoutCheck() {
//        mainHandler.postDelayed(() -> {
//            if (!isConnected) {
//                cancelConnection();
//            }
//        }, Constants.Timeouts.CONNECTION_TIMEOUT);
//    }
//
//    private void closeSocketSilently(BluetoothSocket socket) {
//        if (socket == null) {
//            return;
//        }
//        try {
//            socket.close();
//        } catch (IOException e) {
//            logError(LogCategory.ER, Constants.ErrorMessages.ERROR_CLOSING_SOCKET, e);
//        }
//    }
//
//    @SuppressLint("MissingPermission")
//    private boolean connectToDevice(BluetoothDevice device) {
//        if (device == null) {
//            return false;
//        }
//
//        BluetoothSocket tempSocket = null;
//        try {
//            if (!hasBluetoothConnectPermission()) {
//                logWarn(LogCategory.BT, "BLUETOOTH_CONNECT permission not granted");
//                return false;
//            }
//
//            btDeviceName = device.getName();
//            btDeviceAddr = device.getAddress();
//
//            boolean alreadyConnected = isDeviceConnectedSafe(device);
//            if (alreadyConnected && entranceCheck) {
//                logInfo(LogCategory.BT, String.format("이미 연결된 블루투스 장비 유지: %s / %s", btDeviceName, btDeviceAddr));
//                return true;
//            }
//
//            deviceSelected = device;
//
//            if (btSocket != null && btSocket.isConnected()) {
//                closeSocketSilently(btSocket);
//            }
//
//            tempSocket = createBluetoothSocket(deviceSelected);
//            btSocket = tempSocket;
//            btSocket.connect();
//            isConnected = true;
//            btConnected = true;
//
//            btConnectedThread = new ConnectedThreadOptimized(btSocket, btHandler);
//            btConnectedThread.start();
//
//            btHandler.obtainMessage(CONNECTING_STATUS, 1, -1, btDeviceName).sendToTarget();
//            btConnected = true;
//            scheduleConnectionTimeoutCheck();
//            return true;
//        } catch (IOException e) {
//            logError(LogCategory.ER, Constants.ErrorMessages.BLUETOOTH_SOCKET_CONNECTION_ERROR, e);
//            isConnected = false;
//            onConnectionFailed();
//            closeSocketSilently(tempSocket);
//            btSocket = null;
//            return false;
//        }
//    }
//
//    @SuppressLint("MissingPermission")
//    private boolean isDeviceConnectedSafe(BluetoothDevice device) {
//        if (!hasBluetoothConnectPermission()) {
//            return false;
//        }
//        try {
//            Method m = device.getClass().getMethod(Constants.InitialValues.BT_CONNECTED, (Class[]) null);
//            return (boolean) m.invoke(device, (Object[]) null);
//        } catch (Exception e) {
//            logError(LogCategory.BT, "Failed to check Bluetooth device connection state", e);
//            return false;
//        }
//    }
//
//    private void refreshSpecCache(List<Map<String, String>> specs) {
//        specCache.clear();
//        if (specs == null || specs.isEmpty()) {
//            return;
//        }
//        for (Map<String, String> spec : specs) {
//            if (spec == null) {
//                continue;
//            }
//            String command = spec.get(Constants.JsonKeys.CLM_TEST_COMMAND);
//            if (command == null || command.isEmpty()) {
//                continue;
//            }
//            specCache.put(command, new HashMap<>(spec));
//        }
//    }
//
//    private boolean applyTestSpecData(List<Map<String, String>> sourceData, boolean persistToDb) {
//        if (sourceData == null || sourceData.isEmpty()) {
//            return false;
//        }
//
//        // ⚠️ 중요: 데이터 정규화는 빠르므로 메인 스레드에서 수행
//        List<Map<String, String>> normalizedList = new ArrayList<>();
//        for (Map<String, String> item : sourceData) {
//            normalizedList.add(new HashMap<>(item));
//        }
//
//        lstSpecData = normalizedList;
//        refreshSpecCache(normalizedList);
//
//        // ⚠️ 중요: 모든 무거운 작업을 백그라운드 스레드로 이동하여 메인 스레드 블로킹 방지
//        new Thread(() -> {
//            Context context = getApplicationContext();
//            int tempTotalTimeCnt = 0;
//            String[][] tempArrTestItems = new String[normalizedList.size()][10];
//            String tempValueWatt = Constants.Common.EMPTY_STRING;
//            String tempLowerValueWatt = Constants.Common.EMPTY_STRING;
//            String tempUpperValueWatt = Constants.Common.EMPTY_STRING;
//            String tempProductSerialNo = Constants.Common.EMPTY_STRING;
//
//            // 1. 데이터 처리 루프 (백그라운드 스레드에서 실행)
//            for (int i = 0; i < normalizedList.size(); i++) {
//                Map<String, String> spec = normalizedList.get(i);
//                if (persistToDb) {
//                    TestData.insertTestSpecData(context, spec);
//                }
//                try {
//                    int seconds = Integer.parseInt(valueOrEmpty(spec.get(Constants.JsonKeys.CLM_TEST_SEC)));
//                    tempTotalTimeCnt += seconds;
//                } catch (Exception ignored) {
//                }
//
//                tempArrTestItems[i][0] = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_TEST_NAME));
//                tempArrTestItems[i][1] = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND));
//                tempArrTestItems[i][2] = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_TEST_SEC));
//                tempArrTestItems[i][3] = valueOrEmpty(String.valueOf(tempTotalTimeCnt));
//                tempArrTestItems[i][4] = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_TEST_TYPE));
//                tempArrTestItems[i][5] = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND));
//                tempArrTestItems[i][6] = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_TEST_STEP));
//                tempArrTestItems[i][7] = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_RESPONSE_VALUE));
//
//                // ⚠️ 중요: Double.parseDouble() 예외 처리 강화
//                try {
//                    if (Objects.equals(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND), Constants.TestItemCodes.CM0101) ||
//                            Objects.equals(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND), Constants.TestItemCodes.HT0101) ||
//                            Objects.equals(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND), Constants.TestItemCodes.PM0101) ||
//                            Objects.equals(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND), Constants.TestItemCodes.SV0101) ||
//                            Objects.equals(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND), Constants.TestItemCodes.SV0201) ||
//                            Objects.equals(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND), Constants.TestItemCodes.SV0301) ||
//                            Objects.equals(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND), Constants.TestItemCodes.SV0401)
//                    ) {
//                        String valueWattStr = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_VALUE_WATT));
//                        String lowerValueWattStr = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT));
//                        String upperValueWattStr = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT));
//
//                        if (!valueWattStr.isEmpty() && !lowerValueWattStr.isEmpty() && !upperValueWattStr.isEmpty()) {
//                            double valueWatt = Double.parseDouble(valueWattStr);
//                            double lowerValueWatt = Double.parseDouble(lowerValueWattStr);
//                            double upperValueWatt = Double.parseDouble(upperValueWattStr);
//                            tempArrTestItems[i][8] = String.valueOf(valueWatt - lowerValueWatt);
//                            tempArrTestItems[i][9] = String.valueOf(valueWatt + upperValueWatt);
//                        } else {
//                            tempArrTestItems[i][8] = "0";
//                            tempArrTestItems[i][9] = "0";
//                        }
//                    } else if (spec.get(Constants.JsonKeys.CLM_TEST_COMMAND).equals(Constants.TestItemCodes.TH0101) ||
//                            spec.get(Constants.JsonKeys.CLM_TEST_COMMAND).equals(Constants.TestItemCodes.TH0201) ||
//                            spec.get(Constants.JsonKeys.CLM_TEST_COMMAND).equals(Constants.TestItemCodes.TH0301)
//                    ) {
//                        String valueStr = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_VALUE));
//                        String lowerValueStr = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_LOWER_VALUE));
//                        String upperValueStr = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_UPPER_VALUE));
//
//                        if (!valueStr.isEmpty() && !lowerValueStr.isEmpty() && !upperValueStr.isEmpty()) {
//                            double value = Double.parseDouble(valueStr);
//                            double lowerValue = Double.parseDouble(lowerValueStr);
//                            double upperValue = Double.parseDouble(upperValueStr);
//                            tempArrTestItems[i][8] = String.valueOf(value - lowerValue);
//                            tempArrTestItems[i][9] = String.valueOf(value + upperValue);
//                        } else {
//                            tempArrTestItems[i][8] = "0";
//                            tempArrTestItems[i][9] = "0";
//                        }
//                    } else {
//                        tempArrTestItems[i][8] = "0";
//                        tempArrTestItems[i][9] = "0";
//                    }
//                } catch (NumberFormatException e) {
//                    // 파싱 실패 시 기본값 사용
//                    tempArrTestItems[i][8] = "0";
//                    tempArrTestItems[i][9] = "0";
//                }
//
//                tempValueWatt = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_VALUE_WATT));
//                tempLowerValueWatt = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT));
//                tempUpperValueWatt = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT));
//                tempProductSerialNo = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_PRODUCT_SERIAL_NO));
//            }
//
//            // 2. 시간 계산 (백그라운드 스레드에서 실행) - ⚠️ 개선: 메인 스레드에서 제거됨
//            int calculatedTotalTimeCnt = 0;
//            for (int i = 0; i < tempArrTestItems.length; i++) {
//                try {
//                    int seconds = Integer.parseInt(tempArrTestItems[i][2]);
//                    calculatedTotalTimeCnt += (seconds > 1) ? seconds + 1 : seconds;
//                    tempArrTestItems[i][3] = String.valueOf(calculatedTotalTimeCnt);
//                } catch (NumberFormatException e) {
//                    // 파싱 실패 시 기본값 사용
//                    tempArrTestItems[i][3] = String.valueOf(calculatedTotalTimeCnt);
//                }
//            }
//
//            // 3. UI 업데이트 값 수집 (백그라운드 스레드에서 실행) - ⚠️ 개선: 메인 스레드에서 제거됨
//            String compValueWatt = null;
//            String compLowerValueWatt = null;
//            String compUpperValueWatt = null;
//            String pumpValueWatt = null;
//            String pumpLowerValueWatt = null;
//            String pumpUpperValueWatt = null;
//            String heaterValueWatt = null;
//            String heaterLowerValueWatt = null;
//            String heaterUpperValueWatt = null;
//
//            for (int i = 0; i < normalizedList.size(); i++) {
//                try {
//                    Map<String, String> spec = normalizedList.get(i);
//                    String command = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND));
//                    String itemValueWatt = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_VALUE_WATT));
//                    String itemLowerValueWatt = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT));
//                    String itemUpperValueWatt = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT));
//
//                    // 값 수집 (UI 업데이트는 나중에 한 번에 수행)
//                    if (command.contains(Constants.TestItemCodes.CM0101)) {
//                        compValueWatt = itemValueWatt;
//                        compLowerValueWatt = itemLowerValueWatt;
//                        compUpperValueWatt = itemUpperValueWatt;
//                    }
//                    if (command.contains(Constants.TestItemCodes.PM0101)) {
//                        pumpValueWatt = itemValueWatt;
//                        pumpLowerValueWatt = itemLowerValueWatt;
//                        pumpUpperValueWatt = itemUpperValueWatt;
//                    }
//                    if (command.contains(Constants.TestItemCodes.HT0101)) {
//                        compValueWatt = itemValueWatt;
//                        compLowerValueWatt = itemLowerValueWatt;
//                        compUpperValueWatt = itemUpperValueWatt;
//                        heaterValueWatt = itemValueWatt;
//                        heaterLowerValueWatt = itemLowerValueWatt;
//                        heaterUpperValueWatt = itemUpperValueWatt;
//                    }
//                } catch (Exception e) {
//                    logError(LogCategory.ER, Constants.ErrorMessages.ERROR_IN_UI_UPDATE, e);
//                }
//            }
//
//            // 4. ListView 아이템 데이터 준비 (백그라운드 스레드에서 실행) - ⚠️ 개선: 메인 스레드에서 제거됨
//            // ⚠️ 주의: getString()은 메인 스레드에서만 호출 가능하므로,
//            //          문자열 리소스는 메인 스레드에서 미리 가져와서 사용
//            //          또는 runOnUiThread 내부에서만 호출
//            //
//            // 해결 방법: getString() 호출을 제거하고 하드코딩된 문자열 사용
//            //            또는 메인 스레드에서 미리 가져온 값을 사용
//            //            여기서는 기존 코드와 동일하게 처리하기 위해
//            //            getString() 호출은 runOnUiThread 내부로 이동
//
//            // 백그라운드에서 준비 가능한 데이터만 준비
//            // getString() 호출이 필요한 부분은 runOnUiThread 내부에서 처리
//            List<Map<String, String>> itemsDataToAdd = new ArrayList<>();
//            for (int i = 0; i < tempArrTestItems.length; i++) {
//                Map<String, String> itemData = new HashMap<>();
//                itemData.put(Constants.Common.TEST_ITEM_SEQ, String.valueOf(i + 1));
//                itemData.put(Constants.Common.TEST_ITEM_NAME, tempArrTestItems[i][0]);
//                itemData.put(Constants.Common.TEST_ITEM_COMMAND, tempArrTestItems[i][1]);
//                itemData.put(Constants.Common.TEST_RESPONSE_VALUE, tempArrTestItems[i][7]);
//                itemData.put(Constants.Common.TEST_FINISH_YN, Constants.ResultStatus.NO);
//                itemData.put(Constants.Common.TEST_MODEL_ID, globalModelId);
//                itemsDataToAdd.add(itemData);
//            }
//
//            // final 변수로 캡처
//            final int finalTotalTimeCnt = calculatedTotalTimeCnt;
//            final String[][] finalArrTestItems = tempArrTestItems;
//            final String finalValueWatt = tempValueWatt;
//            final String finalLowerValueWatt = tempLowerValueWatt;
//            final String finalUpperValueWatt = tempUpperValueWatt;
//            final String finalProductSerialNo = tempProductSerialNo;
//            final List<Map<String, String>> finalItemsDataToAdd = itemsDataToAdd;
//            final String finalCompValueWatt = compValueWatt;
//            final String finalCompLowerValueWatt = compLowerValueWatt;
//            final String finalCompUpperValueWatt = compUpperValueWatt;
//            final String finalPumpValueWatt = pumpValueWatt;
//            final String finalPumpLowerValueWatt = pumpLowerValueWatt;
//            final String finalPumpUpperValueWatt = pumpUpperValueWatt;
//            final String finalHeaterValueWatt = heaterValueWatt;
//            final String finalHeaterLowerValueWatt = heaterLowerValueWatt;
//            final String finalHeaterUpperValueWatt = heaterUpperValueWatt;
//
//            SpecProcessingResult result = new SpecProcessingResult();
//            result.arrTestItems = finalArrTestItems;
//            result.totalTimeCnt = finalTotalTimeCnt;
//            result.valueWatt = finalValueWatt;
//            result.lowerValueWatt = finalLowerValueWatt;
//            result.upperValueWatt = finalUpperValueWatt;
//            result.productSerialNo = finalProductSerialNo;
//            result.listItems = finalItemsDataToAdd;
//            result.compValueWatt = finalCompValueWatt;
//            result.compLowerValueWatt = finalCompLowerValueWatt;
//            result.compUpperValueWatt = finalCompUpperValueWatt;
//            result.pumpValueWatt = finalPumpValueWatt;
//            result.pumpLowerValueWatt = finalPumpLowerValueWatt;
//            result.pumpUpperValueWatt = finalPumpUpperValueWatt;
//            result.heaterValueWatt = finalHeaterValueWatt;
//            result.heaterLowerValueWatt = finalHeaterLowerValueWatt;
//            result.heaterUpperValueWatt = finalHeaterUpperValueWatt;
//
//            scheduleUiUpdate(() -> applySpecProcessingResult(result));
//        }).start();
//
//        return true;
//    }
//
//    private void applySpecProcessingResult(SpecProcessingResult result) {
//        if (result == null) {
//            return;
//        }
//        arrTestItems = result.arrTestItems;
//        totalTimeCnt = result.totalTimeCnt;
//        valueWatt = result.valueWatt;
//        lowerValueWatt = result.lowerValueWatt;
//        upperValueWatt = result.upperValueWatt;
//        productSerialNo = result.productSerialNo;
//
//        if (result.compValueWatt != null) {
//            tvCompValueWatt.setText(result.compValueWatt);
//            updateRangeViews(tvCompLowerValueWatt, tvCompUpperValueWatt,
//                    result.compValueWatt, result.compLowerValueWatt, result.compUpperValueWatt);
//        }
//        if (result.pumpValueWatt != null) {
//            tvPumpValueWatt.setText(result.pumpValueWatt);
//            updateRangeViews(tvPumpLowerValueWatt, tvPumpUpperValueWatt,
//                    result.pumpValueWatt, result.pumpLowerValueWatt, result.pumpUpperValueWatt);
//        }
//        if (result.heaterValueWatt != null) {
//            tvHeaterValueWatt.setText(result.heaterValueWatt);
//            updateRangeViews(tvHeaterLowerValueWatt, tvHeaterUpperValueWatt,
//                    result.heaterValueWatt, result.heaterLowerValueWatt, result.heaterUpperValueWatt);
//        }
//
//        listItemAdapter = new ItemAdapterTestItem();
//        lstTestResult = new ArrayList<>();
//        lstTestTemperature = new ArrayList<>();
//        String preProcessText = getString(R.string.txt_pre_process);
//        for (Map<String, String> item : result.listItems) {
//            Map<String, String> map = new HashMap<>(item);
//            map.put(Constants.Common.TEST_ITEM_RESULT, preProcessText);
//            listItemAdapter.addItem(new VoTestItem(map));
//        }
//        tvTotalTimeCnt.setText(String.valueOf(totalTimeCnt));
//        lvTestItem.setAdapter(listItemAdapter);
//        listItemAdapter.updateListAdapter();
//        lastTestIdx = listItemAdapter.getCount();
//    }
//
//    private static class SpecProcessingResult {
//        String[][] arrTestItems;
//        int totalTimeCnt;
//        String valueWatt;
//        String lowerValueWatt;
//        String upperValueWatt;
//        String productSerialNo;
//        String compValueWatt;
//        String compLowerValueWatt;
//        String compUpperValueWatt;
//        String pumpValueWatt;
//        String pumpLowerValueWatt;
//        String pumpUpperValueWatt;
//        String heaterValueWatt;
//        String heaterLowerValueWatt;
//        String heaterUpperValueWatt;
//        List<Map<String, String>> listItems = new ArrayList<>();
//    }
//
//    /*
//    private boolean applyTestSpecData(List<Map<String, String>> sourceData, boolean persistToDb) {
//        if (sourceData == null || sourceData.isEmpty()) {
//            return false;
//        }
//
//        // ⚠️ 중요: 데이터 정규화는 빠르므로 메인 스레드에서 수행
//        List<Map<String, String>> normalizedList = new ArrayList<>();
//        for (Map<String, String> item : sourceData) {
//            normalizedList.add(new HashMap<>(item));
//        }
//
//        lstSpecData = normalizedList;
//        refreshSpecCache(normalizedList);
//
//        // ⚠️ 중요: 무거운 데이터 처리를 백그라운드 스레드로 이동하여 메인 스레드 블로킹 방지
//        new Thread(() -> {
//            Context context = getApplicationContext();
//            int tempTotalTimeCnt = 0;
//            String[][] tempArrTestItems = new String[normalizedList.size()][10];
//            String tempValueWatt = Constants.Common.EMPTY_STRING;
//            String tempLowerValueWatt = Constants.Common.EMPTY_STRING;
//            String tempUpperValueWatt = Constants.Common.EMPTY_STRING;
//            String tempProductSerialNo = Constants.Common.EMPTY_STRING;
//
//            // 데이터 처리 루프 (백그라운드 스레드에서 실행)
//            for (int i = 0; i < normalizedList.size(); i++) {
//                Map<String, String> spec = normalizedList.get(i);
//                if (persistToDb) {
//                    TestData.insertTestSpecData(context, spec);
//                }
//                try {
//                    int seconds = Integer.parseInt(valueOrEmpty(spec.get(Constants.JsonKeys.CLM_TEST_SEC)));
//                    tempTotalTimeCnt += seconds;
//                } catch (Exception ignored) {
//                }
//
//                tempArrTestItems[i][0] = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_TEST_NAME));
//                tempArrTestItems[i][1] = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND));
//                tempArrTestItems[i][2] = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_TEST_SEC));
//                tempArrTestItems[i][3] = valueOrEmpty(String.valueOf(tempTotalTimeCnt));
//                tempArrTestItems[i][4] = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_TEST_TYPE));
//                tempArrTestItems[i][5] = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND));
//                tempArrTestItems[i][6] = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_TEST_STEP));
//                tempArrTestItems[i][7] = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_RESPONSE_VALUE));
//
//                // ⚠️ 중요: Double.parseDouble() 예외 처리 강화
//                try {
//                    if(Objects.equals(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND), Constants.TestItemCodes.CM0101) ||
//                        Objects.equals(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND), Constants.TestItemCodes.HT0101) ||
//                        Objects.equals(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND), Constants.TestItemCodes.PM0101) ||
//                        Objects.equals(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND), Constants.TestItemCodes.SV0101) ||
//                        Objects.equals(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND), Constants.TestItemCodes.SV0201) ||
//                        Objects.equals(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND), Constants.TestItemCodes.SV0301) ||
//                        Objects.equals(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND), Constants.TestItemCodes.SV0401)
//                    ) {
//                        String valueWattStr = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_VALUE_WATT));
//                        String lowerValueWattStr = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT));
//                        String upperValueWattStr = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT));
//
//                        if (!valueWattStr.isEmpty() && !lowerValueWattStr.isEmpty() && !upperValueWattStr.isEmpty()) {
//                            double valueWatt = Double.parseDouble(valueWattStr);
//                            double lowerValueWatt = Double.parseDouble(lowerValueWattStr);
//                            double upperValueWatt = Double.parseDouble(upperValueWattStr);
//                            tempArrTestItems[i][8] = String.valueOf(valueWatt - lowerValueWatt);
//                            tempArrTestItems[i][9] = String.valueOf(valueWatt + upperValueWatt);
//                        } else {
//                            tempArrTestItems[i][8] = "0";
//                            tempArrTestItems[i][9] = "0";
//                        }
//                    }
//                    else if(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND).equals(Constants.TestItemCodes.TH0101) ||
//                            spec.get(Constants.JsonKeys.CLM_TEST_COMMAND).equals(Constants.TestItemCodes.TH0201) ||
//                            spec.get(Constants.JsonKeys.CLM_TEST_COMMAND).equals(Constants.TestItemCodes.TH0301)
//                    ) {
//                        String valueStr = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_VALUE));
//                        String lowerValueStr = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_LOWER_VALUE));
//                        String upperValueStr = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_UPPER_VALUE));
//
//                        if (!valueStr.isEmpty() && !lowerValueStr.isEmpty() && !upperValueStr.isEmpty()) {
//                            double value = Double.parseDouble(valueStr);
//                            double lowerValue = Double.parseDouble(lowerValueStr);
//                            double upperValue = Double.parseDouble(upperValueStr);
//                            tempArrTestItems[i][8] = String.valueOf(value - lowerValue);
//                            tempArrTestItems[i][9] = String.valueOf(value + upperValue);
//                        } else {
//                            tempArrTestItems[i][8] = "0";
//                            tempArrTestItems[i][9] = "0";
//                        }
//                    }
//                    else {
//                        tempArrTestItems[i][8] = "0";
//                        tempArrTestItems[i][9] = "0";
//                    }
//                } catch (NumberFormatException e) {
//                    // 파싱 실패 시 기본값 사용
//                    tempArrTestItems[i][8] = "0";
//                    tempArrTestItems[i][9] = "0";
//                }
//
//                tempValueWatt = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_VALUE_WATT));
//                tempLowerValueWatt = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT));
//                tempUpperValueWatt = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT));
//                tempProductSerialNo = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_PRODUCT_SERIAL_NO));
//            }
//
//            final int finalTotalTimeCnt = tempTotalTimeCnt;
//            final String[][] finalArrTestItems = tempArrTestItems;
//            final String finalValueWatt = tempValueWatt;
//            final String finalLowerValueWatt = tempLowerValueWatt;
//            final String finalUpperValueWatt = tempUpperValueWatt;
//            final String finalProductSerialNo = tempProductSerialNo;
//            final List<Map<String, String>> finalSpecList = normalizedList;
//
//            // 복잡한 UI 업데이트 블록 통합: scheduleUiUpdate 사용
//            scheduleUiUpdate(() -> {
//            try {
//                ActivityModel_0003 act = getMainActivity();
//                if (act == null || act.isFinishing()) {
//                    return;
//                }
//
//                arrTestItems = finalArrTestItems;
//                totalTimeCnt = finalTotalTimeCnt;
//                valueWatt = finalValueWatt;
//                lowerValueWatt = finalLowerValueWatt;
//                upperValueWatt = finalUpperValueWatt;
//                productSerialNo = finalProductSerialNo;
//
//                // ⚠️ 중요: UI 업데이트를 배치 처리하여 메인 스레드 블로킹 최소화
//                // 모든 업데이트 값을 먼저 수집한 후 한 번에 적용
//                String compValueWatt = null;
//                String compLowerValueWatt = null;
//                String compUpperValueWatt = null;
//                String pumpValueWatt = null;
//                String pumpLowerValueWatt = null;
//                String pumpUpperValueWatt = null;
//                String heaterValueWatt = null;
//                String heaterLowerValueWatt = null;
//                String heaterUpperValueWatt = null;
//
//                for (int i = 0; i < finalSpecList.size(); i++) {
//                    try {
//                        Map<String, String> spec = finalSpecList.get(i);
//                        String command = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_TEST_COMMAND));
//                        String itemValueWatt = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_VALUE_WATT));
//                        String itemLowerValueWatt = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_LOWER_VALUE_WATT));
//                        String itemUpperValueWatt = valueOrEmpty(spec.get(Constants.JsonKeys.CLM_UPPER_VALUE_WATT));
//
//                        // 값 수집 (UI 업데이트는 나중에 한 번에 수행)
//                        if (command.contains(Constants.TestItemCodes.CM0101)) {
//                            compValueWatt = itemValueWatt;
//                            compLowerValueWatt = itemLowerValueWatt;
//                            compUpperValueWatt = itemUpperValueWatt;
//                        }
//                        if (command.contains(Constants.TestItemCodes.PM0101)) {
//                            pumpValueWatt = itemValueWatt;
//                            pumpLowerValueWatt = itemLowerValueWatt;
//                            pumpUpperValueWatt = itemUpperValueWatt;
//                        }
//                        if (command.contains(Constants.TestItemCodes.HT0101)) {
//                            compValueWatt = itemValueWatt;
//                            compLowerValueWatt = itemLowerValueWatt;
//                            compUpperValueWatt = itemUpperValueWatt;
//                            heaterValueWatt = itemValueWatt;
//                            heaterLowerValueWatt = itemLowerValueWatt;
//                            heaterUpperValueWatt = itemUpperValueWatt;
//                        }
//                    } catch (Exception e) {
//                        logError(LogCategory.ER, Constants.ErrorMessages.ERROR_IN_UI_UPDATE, e);
//                    }
//                }
//
//                // 한 번에 UI 업데이트 (배치 처리)
//                if (compValueWatt != null) {
//                    tvCompValueWatt.setText(compValueWatt);
//                    updateRangeViews(tvCompLowerValueWatt, tvCompUpperValueWatt, compValueWatt, compLowerValueWatt, compUpperValueWatt);
//                }
//                if (pumpValueWatt != null) {
//                    tvPumpValueWatt.setText(pumpValueWatt);
//                    updateRangeViews(tvPumpLowerValueWatt, tvPumpUpperValueWatt, pumpValueWatt, pumpLowerValueWatt, pumpUpperValueWatt);
//                }
//                if (heaterValueWatt != null) {
//                    tvHeaterValueWatt.setText(heaterValueWatt);
//                    updateRangeViews(tvHeaterLowerValueWatt, tvHeaterUpperValueWatt, heaterValueWatt, heaterLowerValueWatt, heaterUpperValueWatt);
//                }
//
//                totalTimeCnt = 0;
//                listItemAdapter = new ItemAdapterTestItem();
//                lstTestResult = new ArrayList<>();
//                lstTestTemperature = new ArrayList<>();
//
//                // ⚠️ 중요: 로그 출력을 완전히 제거하여 메인 스레드 블로킹 방지
//                // 디버그가 필요한 경우에만 주석 해제 (260번의 로그 출력으로 인한 성능 저하 방지)
//                // for (int i = 0; i < arrTestItems.length; i++) {
//                //     for (int j = 0; j < arrTestItems[i].length; j++) {
//                //         logInfo(LogCategory.PS, "> [" + i + "][" + j + "] " + arrTestItems[i][j]);
//                //     }
//                //     logInfo(LogCategory.PS, "> ");
//                // }
//
//                // 시간 계산 최적화: try-catch로 예외 처리하여 안정성 향상
//                for (int i = 0; i < arrTestItems.length; i++) {
//                    try {
//                        int seconds = Integer.parseInt(arrTestItems[i][2]);
//                        totalTimeCnt += (seconds > 1) ? seconds + 1 : seconds;
//                        arrTestItems[i][3] = String.valueOf(totalTimeCnt);
//                    } catch (NumberFormatException e) {
//                        // 파싱 실패 시 기본값 사용
//                        arrTestItems[i][3] = String.valueOf(totalTimeCnt);
//                    }
//                }
//
//                // ⚠️ 중요: ListView 아이템 추가를 배치 처리하여 UI 업데이트 최적화
//                // notifyDataSetChanged()를 한 번만 호출하도록 모든 아이템을 먼저 추가
//                for (int i = 0; i < arrTestItems.length; i++) {
//                    Map<String, String> mapListItem = new HashMap<>();
//                    mapListItem.put(Constants.Common.TEST_ITEM_SEQ, String.valueOf(i + 1));
//                    mapListItem.put(Constants.Common.TEST_ITEM_NAME, arrTestItems[i][0]);
//                    mapListItem.put(Constants.Common.TEST_ITEM_COMMAND, arrTestItems[i][1]);
//                    mapListItem.put(Constants.Common.TEST_RESPONSE_VALUE, arrTestItems[i][7]);
//                    mapListItem.put(Constants.Common.TEST_ITEM_RESULT, getString(R.string.txt_pre_process));
//                    mapListItem.put(Constants.Common.TEST_FINISH_YN, Constants.ResultStatus.NO);
//                    mapListItem.put(Constants.Common.TEST_MODEL_ID, globalModelId);
//                    listItemAdapter.addItem(new VoTestItem(mapListItem));
//                }
//
//                // ⚠️ 중요: Handler 재사용 (통합된 mainHandler 사용)
//                // UI 업데이트를 다음 프레임으로 지연시켜 메인 스레드 블로킹 방지
//                mainHandler.post(() -> {
//                    tvTotalTimeCnt.setText(String.valueOf(totalTimeCnt));
//                    lvTestItem.setAdapter(listItemAdapter);
//                    listItemAdapter.updateListAdapter(); // 한 번만 notifyDataSetChanged() 호출
//                    lastTestIdx = listItemAdapter.getCount();
//                });
//            } catch (Exception e) {
//                logError(LogCategory.ER, Constants.ErrorMessages.ERROR_IN_UI_UPDATE_JSON_PARSING, e);
//            }
//        });
//        }).start(); // 백그라운드 스레드 시작
//
//        return true;
//    }
//    */
//
//    // ============================================================
//    // Phase 1: UI 업데이트 배치 처리 시스템
//    // ============================================================
//
//    /**
//     * UI 업데이트를 배치 처리로 스케줄링
//     * 여러 UI 업데이트를 모아서 하나의 runOnUiThread로 처리하여 메시지 큐 부하 감소
//     *
//     * @param update UI 업데이트 Runnable
//     */
//    private void scheduleUiUpdate(Runnable update) {
//        if (update == null) {
//            return;
//        }
//
//        // 이미 메인 스레드에서 실행 중이면 즉시 실행
//        if (Looper.myLooper() == Looper.getMainLooper()) {
//            try {
//                update.run();
//            } catch (Exception e) {
//                logError(LogCategory.ER, "Error in immediate UI update", e);
//            }
//            return;
//        }
//
//        synchronized (uiUpdateLock) {
//            pendingUiUpdates.add(update);
//
//            // 배치 작업이 예약되지 않았다면 예약
//            if (uiUpdateBatchTask == null) {
//                uiUpdateBatchTask = () -> {
//                    List<Runnable> updates;
//                    synchronized (uiUpdateLock) {
//                        if (pendingUiUpdates.isEmpty()) {
//                            uiUpdateBatchTask = null;
//                            return;
//                        }
//                        updates = new ArrayList<>(pendingUiUpdates);
//                        pendingUiUpdates.clear();
//                        uiUpdateBatchTask = null;
//                    }
//
//                    // 모든 UI 업데이트를 한 번에 실행
//                    for (Runnable r : updates) {
//                        try {
//                            r.run();
//                        } catch (Exception e) {
//                            logError(LogCategory.ER, "Error in UI update batch", e);
//                        }
//                    }
//                };
//
//                // 다음 프레임에 실행 (16ms 지연 = 1 frame @ 60fps)
//                mainHandler.postDelayed(uiUpdateBatchTask, UI_UPDATE_BATCH_DELAY_MS);
//            }
//        }
//    }
//
//    /**
//     * 즉시 UI 업데이트가 필요한 경우 (중요한 업데이트)
//     * 배치 처리 없이 즉시 실행
//     *
//     * @param update UI 업데이트 Runnable
//     */
//    private void scheduleUiUpdateImmediate(Runnable update) {
//        if (update == null) {
//            return;
//        }
//
//        // 이미 메인 스레드에서 실행 중이면 즉시 실행
//        if (Looper.myLooper() == Looper.getMainLooper()) {
//            try {
//                update.run();
//            } catch (Exception e) {
//                logError(LogCategory.ER, "Error in immediate UI update", e);
//            }
//            return;
//        }
//
//        // 즉시 실행 (배치 처리 없음)
//        mainHandler.post(update);
//    }
//
//    /**
//     * 배치 처리 큐 정리 (Activity destroy 시 호출)
//     */
//    private void clearUiUpdateBatchQueue() {
//        synchronized (uiUpdateLock) {
//            if (uiUpdateBatchTask != null) {
//                mainHandler.removeCallbacks(uiUpdateBatchTask);
//                uiUpdateBatchTask = null;
//            }
//            pendingUiUpdates.clear();
//        }
//    }
//    // ============================================================
//
//    private void updateRangeViews(TextView lowerView, TextView upperView, String centerValue, String lowerOffset, String upperOffset) {
//        try {
//            if (lowerView != null) {
//                lowerView.setText(String.valueOf(Integer.parseInt(centerValue) - Integer.parseInt(lowerOffset)));
//            }
//            if (upperView != null) {
//                upperView.setText(String.valueOf(Integer.parseInt(centerValue) + Integer.parseInt(upperOffset)));
//            }
//        } catch (Exception ignored) {
//        }
//    }
//
//    private String valueOrEmpty(String value) {
//        return value == null ? Constants.Common.EMPTY_STRING : value;
//    }
//
//    private List<Map<String, String>> getSpecData(String command) {
//        if (command == null || command.trim().isEmpty()) {
//            logWarn(LogCategory.BT, "finalReceiveCommand is null or empty, skipping database query");
//            return Collections.emptyList();
//        }
//
//        Map<String, String> cached = specCache.get(command);
//        if (cached != null) {
//            return Collections.singletonList(cached);
//        }
//
//        try {
//            List<Map<String, String>> specDataResult = TestData.selectTestSpecData(getBaseContext(), Constants.Database.QUERY_AND_CLM_TEST_COMMAND_EQUALS + command + "'");
//            if (specDataResult != null && !specDataResult.isEmpty()) {
//                specCache.put(command, new HashMap<>(specDataResult.get(0)));
//                return specDataResult;
//            }
//        } catch (Exception e) {
//            logError(LogCategory.ER, "Error querying test spec data for command: " + command, e);
//        }
//        return Collections.emptyList();
//    }
//
//    private String computeSha256(String value) {
//        try {
//            MessageDigest digest = MessageDigest.getInstance("SHA-256");
//            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
//            StringBuilder sb = new StringBuilder();
//            for (byte b : hash) {
//                sb.append(String.format("%02x", b));
//            }
//            return sb.toString();
//        } catch (NoSuchAlgorithmException e) {
//            logError(LogCategory.ER, "Unable to compute SHA-256 hash", e);
//            return null;
//        }
//    }
//
//    @SuppressLint("MissingPermission")
//    private void bluetoothOn() {
//        if (mBTAdapter == null) {
//            logWarn(LogCategory.BT, "Bluetooth adapter not available");
//            return;
//        }
//        if (!hasBluetoothConnectPermission()) {
//            ensureBtPermissions(null);
//            return;
//        }
//        if (!mBTAdapter.isEnabled()) { // 블루투스 어댑터 활성화 요청
//            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
//            cancelDiscoverySafe();
//        }
//    }
//
//    private void sendBtMessage(String sendMessage) {
//        try {
//            if (btConnectedThread != null) {
//                if (sendMessage.contains(Constants.TestItemCodes.CM0200)) {
//                    logDebug(LogCategory.BT, Constants.LogMessages.SEND_BT_MESSAGE_CM0200_TO_CM0100 + sendMessage);
//                    sendMessage = Constants.TestItemCodes.CM0100;
//                }
//                btConnectedThread.write(sendMessage);
//
//                // Thread.sleep 제거: 메인 스레드에서 호출될 경우 블로킹 방지
//                // 대신 비동기로 짧은 지연이 필요한 경우 Handler 사용
//                // 주의: 이 메서드가 메인 스레드에서 호출되지 않도록 호출부 확인 필요
//            }
//            // Thread.sleep(100); 제거됨 - 메인 스레드 블로킹 방지
//        } catch (Exception e) {
//            logError(LogCategory.ER, Constants.ErrorMessages.SEND_BT_MESSAGE_ERROR, e);
//        }
//    }
//
//    public static String getCurrentDatetime(String dateformat) {
//        SimpleDateFormat dateFormmater = new SimpleDateFormat(dateformat);
//        return dateFormmater.format(new Date());
//    }
//
//    // Enter here after user selects "yes" or "no" to enabling radio
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent Data) {
//        // Check which request we're responding to
//        super.onActivityResult(requestCode, resultCode, Data);
//        if (requestCode == REQUEST_ENABLE_BT) {
//            // Make sure the request was successful
//            if (resultCode == RESULT_OK) {
//                // The user picked a contact.
//                // The Intent's data Uri identifies which contact was selected.
//                mBluetoothStatus.setText(getString(R.string.sEnabled));
//            } else
//                mBluetoothStatus.setText(getString(R.string.sDisabled));
//        }
//    }
//
//    private void checkBTPermissions() {
//        ensureBtPermissions(null);
//    }
//
//    @SuppressLint("MissingPermission")
//    public void listPairedDevicesSelect() {
//        try {
//            if (mBTArrayAdapter == null) {
//                return;
//            }
//            if (!hasAllBluetoothPermissions()) {
//                ensureBtPermissions(PermissionAction.LIST_PAIRED_DEVICES);
//                return;
//            }
//            if (mBTAdapter == null) {
//                logWarn(LogCategory.BT, "Bluetooth adapter unavailable; cannot list devices");
//                return;
//            }
//            mBTArrayAdapter.clear();
//
//            mPairedDevices = getBondedDevicesSafe();
//            int pairedCount = (mPairedDevices == null) ? 0 : mPairedDevices.size();
//            boolean adapterEnabled = isBluetoothAdapterEnabled();
//            logInfo(LogCategory.BT, String.format("페어링된 블루투스 장비 - enabled: %s, count: %d, entranceCheck: %s",
//                    adapterEnabled, pairedCount, entranceCheck));
//            if (adapterEnabled) {
//                btReconnectDelayMs = Constants.Timeouts.BT_LIST_TIMER_INTERVAL_MS;
//                scheduleBluetoothReconnect(true);
//            } else {
//                AlertDialog.Builder alertDialog = new AlertDialog.Builder(getApplicationContext());
//                alertDialog.setTitle(Constants.UIMessages.BLUETOOTH);
//                alertDialog.setMessage(Constants.UIMessages.BLUETOOTH_ENABLE_MESSAGE);
//                alertDialog.setNegativeButton(Constants.UIMessages.CANCEL,
//                        new DialogInterface.OnClickListener() {
//                            public void onClick(DialogInterface dialog, int which) {
//                                dialog.cancel();
//                            }
//                        });
//                alertDialog.show();
//            }
//        } catch (Exception e) {
//            logError(LogCategory.ER, Constants.ErrorMessages.LIST_PAIRED_DEVICES_SELECT_ERROR, e);
//        }
//    }
//
//    private void onConnectionSuccess() {
//        // Handle the success state, e.g., notify the user, start communication, etc.
//        logInfo(LogCategory.BT, Constants.LogMessages.BLUETOOTH_CONNECTION_SUCCESSFUL);
//    }
//
//    private void onConnectionFailed() {
//        // Handle connection failure, e.g., retry connection or notify user
//        logInfo(LogCategory.BT, Constants.LogMessages.BLUETOOTH_CONNECTION_FAILED);
//        scheduleBluetoothReconnect(false);
//    }
//
//    private void cancelConnection() {
//        if (btSocket != null && btSocket.isConnected()) {
//            try {
//                btSocket.close();
//                logInfo(LogCategory.BT, Constants.LogMessages.CONNECTION_ATTEMPT_TIMED_OUT);
//            } catch (IOException e) {
//                logError(LogCategory.ER, Constants.ErrorMessages.BLUETOOTH_CANCEL_CONNECTION_ERROR, e);
//                e.printStackTrace();
//            }
//        }
//        isConnected = false;
//        btConnected = false;
//        btConnectionInProgress = false;
//        if (btConnectedThread != null) {
//            try {
//                btConnectedThread.cancel();
//            } catch (Exception e) {
//                logWarn(LogCategory.BT, "Error canceling Bluetooth thread during cancelConnection: " + e.getMessage());
//            }
//            btConnectedThread = null;
//        }
//        scheduleBluetoothReconnect(false);
//    }
//
//    @SuppressLint("MissingPermission")
//    public boolean isConnected(BluetoothDevice device) {
//        if (!hasBluetoothConnectPermission()) {
//            ensureBtPermissions(PermissionAction.BT_RECONNECT);
//            return false;
//        }
//        try {
//            Method m = device.getClass().getMethod(Constants.InitialValues.BT_CONNECTED, (Class[]) null);
//            boolean connected = (boolean) m.invoke(device, (Object[]) null);
//            return connected;
//        } catch (Exception e) {
//            throw new IllegalStateException(e);
//        }
//    }
//
//    @SuppressLint("MissingPermission")
//    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
//        if (!hasBluetoothConnectPermission()) {
//            ensureBtPermissions(PermissionAction.BT_RECONNECT);
//            throw new IOException("Missing BLUETOOTH_CONNECT permission");
//        }
//        try {
//            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
//            return (BluetoothSocket) m.invoke(device, BT_MODULE_UUID);
//        } catch (Exception e) {
//            logError(LogCategory.ER, Constants.ErrorMessages.COULD_NOT_CREATE_INSECURE_RFCOMM_CONNECTION, e);
//        }
//        try {
//            return device.createRfcommSocketToServiceRecord(BT_MODULE_UUID);
//        } catch (SecurityException e) {
//            throw new IOException("Failed to create RFCOMM socket due to missing permission", e);
//        }
//    }
//
//
//    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            logDebug(LogCategory.PS, Constants.LogMessages.USB_INTENT_ACTION + intent.getAction());
//            // Phase 4: USB Handler 내부 통합 - UI 업데이트 정보를 저장
//            ActivityModel_0003 activity = getMainActivity();
//            int usbStatusColor = -1; // -1이면 UI 업데이트 없음
//            String usbStatusText = null;
//
//            switch (intent.getAction()) {
//                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
//                    // Toast.makeText(context, Constants.UIMessages.USB_READY, Toast.LENGTH_SHORT).show();
//
//                    usbConnPermissionGranted = true; // 권한 플래그를 먼저 올려 재시작 로직이 차단되지 않도록 함
//                    cancelUsbReconnect();
//                    updateUsbLampReady();
//
//                    // ⚠️ 중요: USB 재연결 시 onNewIntent()를 호출하여 액티비티 재실행 방지
//                    if (activity != null && !activity.isFinishing()) {
//                        // 2. Intent 생성 및 Flag 설정
//                        Intent reconnectIntent = new Intent(context, ActivityModel_0003.class);
//                        reconnectIntent.setAction("USB_RECONNECTED");
//                        reconnectIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
//                        reconnectIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//
//                        // 3. startActivity() 호출 (시스템이 자동으로 onNewIntent() 호출)
//                        // launchMode="singleTop"과 FLAG_ACTIVITY_SINGLE_TOP으로 인해
//                        // Activity가 재실행되지 않고 onNewIntent()만 호출됨
////                        activity.startActivity(reconnectIntent);
//
//                        activity.onNewIntent(reconnectIntent);  // ✅ 생명주기 변경 없음
//                        activity.setIntent(reconnectIntent);
//                    }
//                    if (usbService != null && (usbPollingFuture == null || usbPollingFuture.isCancelled())) {
//                        logInfo(LogCategory.US, "USB permission granted - starting polling");
//                        startUsbPolling(true);
//                    }
//                    // UI 업데이트: 권한 승인 시 색상 변경 (필요시)
//                    // usbStatusColor = R.color.colorAccent; // 필요시 주석 해제
//                    break;
//                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
//                    // Toast.makeText(context, Constants.UIMessages.USB_PERMISSION_NOT_GRANTED, Toast.LENGTH_SHORT).show();
//                    usbStatusColor = R.color.colorAccent;
//                    break;
//                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
//                    // Toast.makeText(context, Constants.UIMessages.NO_USB_CONNECTED, Toast.LENGTH_SHORT).show();
//                    usbStatusColor = R.color.green_01;
//                    scheduleUiUpdate(() -> {
//                        final int finalUsbStatusColor = R.color.red_01;
//                        // final String finalUsbStatusText = "";
//                        tvConnectPlcRamp.setBackgroundColor(getColor(finalUsbStatusColor));
//                        // tvConnectPlcRamp.setText(finalUsbStatusText);
//                    });
//                    // USB 연결 해제 시 폴링 정리
//                    System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> stopUsbPolling 7");
//                    stopUsbPolling();
//
//                    decElectricValue = 0;
//
//                    // ⚠️ 중요: 서비스는 해제하지 않고, 플래그만 초기화
//                    // usbService는 null로 설정하지 않음 (재연결 시 자동으로 다시 연결됨)
//                    usbConnPermissionGranted = false;  // 권한 플래그만 초기화
//                    scheduleUsbPermissionRecovery();
//                    scheduleUsbReconnect(false);
//
////                    if(tmrBTMessageSend!=null) {
////                        tmrBTMessageSend.cancel();
////                        ttBTMessageSend.cancel();
////                        tmrBTMessageSend = null;
////                    }
//                    stopBtMessageTimer();
//                    disconnectCheckCount = 0;
//                    receivedMessageCnt = 0;
//                    sendingMessageCnt = 0;
//
//                    usbStatusColor = R.color.red_01;
//                    break;
//                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
//                    // Toast.makeText(context, Constants.UIMessages.USB_DISCONNECTED, Toast.LENGTH_SHORT).show();
//                    stopBtMessageTimer();
//                    scheduleUiUpdate(() -> {
//                        final int finalUsbStatusColor = R.color.red_01;
//                        // final String finalUsbStatusText = "";
//                        tvConnectPlcRamp.setBackgroundColor(getColor(finalUsbStatusColor));
//                        // tvConnectPlcRamp.setText(finalUsbStatusText);
//                    });
//                    // USB 연결 해제 시 폴링 정리
//                    System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> stopUsbPolling 8");
//                    stopUsbPolling();
//
//                    decElectricValue = 0;
//
//                    // ⚠️ 중요: 서비스는 해제하지 않고, 플래그만 초기화
//                    // usbService는 null로 설정하지 않음 (재연결 시 자동으로 다시 연결됨)
//                    usbConnPermissionGranted = false;  // 권한 플래그만 초기화
//                    scheduleUsbPermissionRecovery();
//                    scheduleUsbReconnect(false);
//
////                    if(tmrBTMessageSend!=null) {
////                        tmrBTMessageSend.cancel();
////                        ttBTMessageSend.cancel();
////                        tmrBTMessageSend = null;
////                    }
//                    stopBtMessageTimer();
//                    disconnectCheckCount = 0;
//                    receivedMessageCnt = 0;
//                    sendingMessageCnt = 0;
//
//                    usbStatusColor = R.color.red_01;
//                    break;
//                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
//                    // Toast.makeText(context, Constants.UIMessages.USB_DEVICE_NOT_SUPPORTED, Toast.LENGTH_SHORT).show();
//                    usbStatusColor = R.color.orange_01;
//                    break;
//                default:
//                    // Toast.makeText(context, Constants.LogMessages.CONNECTION_STATE + intent.getAction(), Toast.LENGTH_SHORT).show();
//                    logInfo(LogCategory.PS, Constants.LogMessages.USB_CONNECTION_STATE + intent.getAction());
//                    break;
//            }
//
//            // Phase 4: 모든 case 처리 후 한 번만 UI 업데이트 (통합)
//            if (usbStatusColor != -1 && activity != null && !activity.isFinishing()) {
//                final int finalUsbStatusColor = usbStatusColor;
////                final String finalUsbStatusText = usbStatusText;
//                scheduleUiUpdate(() -> {
//                    tvConnectPlcRamp.setBackgroundColor(getColor(finalUsbStatusColor));
////                    if (finalUsbStatusText != null) {
////                        tvConnectPlcRamp.setText(finalUsbStatusText);
////                    }
//                });
//            }
//        }
//    };
//
//    private final ServiceConnection usbConnection = new ServiceConnection() {
//        @Override
//        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
//            try {
//                // 안전한 타입 체크 및 예외 처리
//                if (arg1 instanceof UsbService.UsbBinder) {
//                    UsbService.UsbBinder binder = (UsbService.UsbBinder) arg1;
//                    usbService = binder.getService();
//
//                    if (usbService != null) {
//                        usbService.setHandler(usbHandler);
//                        logInfo(LogCategory.PS, Constants.LogMessages.USB_SERVICE_CONNECTED_SUCCESSFULLY);
//
//                        // 앱 재시작 시 USB 자동 연결: USB 서비스 연결 후 자동으로 연결 메시지 전송 시도
//                        // Timer가 아직 시작되지 않은 경우에만 시작 (중복 방지)
//                        if (usbPollingFuture == null || usbPollingFuture.isCancelled()) {
//                            logInfo(LogCategory.US, Constants.LogMessages.USB_SERVICE_CONNECTED);
//
//                            // USB 권한이 이미 부여된 경우 즉시 시작, 그렇지 않은 경우 권한 부여 대기
//                            if (usbConnPermissionGranted) {
//                                logInfo(LogCategory.US, Constants.LogMessages.USB_PERMISSION_ALREADY_GRANTED);
//
//                                // HTTP 통신 관련 runOnUiThread 최적화: scheduleUiUpdate 사용
//                                scheduleUiUpdate(() -> {
//                                    tvConnectPlcRamp.setBackgroundColor(getBaseContext().getResources().getColor(R.color.blue_01));
//                                    // tvConnectPlcRamp.setText(Constants.InitialValues.DEVICE_READY_CHARACTER);
//                                });
//
//                                System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> startUsbPolling 5");
//                                startUsbPolling(true);
//                            } else {
//                                logInfo(LogCategory.US, Constants.LogMessages.USB_PERMISSION_WAITING);
//                            }
//                        } else {
//                            logDebug(LogCategory.US, Constants.LogMessages.USB_CONNECTION_TIMER_ALREADY_RUNNING);
//                        }
//                    } else {
//                        logError(LogCategory.ER, Constants.ErrorMessages.USB_SERVICE_IS_NULL_AFTER_BINDING, null);
//                        usbService = null;
//                    }
//                } else {
//                    logError(LogCategory.ER, Constants.ErrorMessages.INVALID_BINDER_TYPE_EXPECTED_USB_SERVICE_USB_BINDER_GOT +
//                            (arg1 != null ? arg1.getClass().getName() : Constants.Common.NULL), null);
//                    usbService = null;
//                }
//            } catch (ClassCastException e) {
//                logError(LogCategory.ER, Constants.ErrorMessages.CLASS_CAST_EXCEPTION_IN_ONSERVICECONNECTED, e);
//                e.printStackTrace();
//                usbService = null;
//            } catch (Exception e) {
//                logError(LogCategory.ER, Constants.ErrorMessages.CLASS_CAST_EXCEPTION_IN_ONSERVICECONNECTED, e);
//                e.printStackTrace();
//                usbService = null;
//            }
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName arg0) {
//            logInfo(LogCategory.PS, Constants.LogMessages.USB_SERVICE_DISCONNECTED);
//            // ⚠️ 중요: usbService를 null로 설정하지 않음
//            // USB 재연결 시 자동으로 onServiceConnected가 호출되어 다시 설정됨
//            // usbService = null;  // 주석 처리하여 액티비티 재시작 방지
//            usbConnPermissionGranted = false;  // 권한 플래그만 초기화
//        }
//    };
//
//    @Override
//    public void onResume() {
//        super.onResume();
//
//        // ⚠️ 키오스크 모드: onResume에서도 시스템 UI 숨기기 재적용
//        setupKioskMode();
//
//        // 권한 상태 확인 (사용자가 설정에서 권한을 허용하고 돌아온 경우 감지)
//        // 백그라운드 스레드에서 체크하여 메인 스레드 블로킹 방지
//        if (!btPermissionsGranted) {
//            runOnBtWorker(() -> {
//                // 백그라운드에서 권한 상태 확인
//                boolean hasAllPermissions = hasAllBluetoothPermissions();
//                if (hasAllPermissions) {
//                    // 메인 스레드로 포스팅하여 UI 업데이트 및 대기 중인 작업 실행
//                    runOnUiThread(() -> {
//                        permissionDenialCount = 0; // 권한이 허용되었으면 거부 횟수 리셋
//                        btPermissionsGranted = true;
//                        permissionRequestInProgress = false;
//                        runPendingPermissionActions();
//                        logInfo(LogCategory.BT, "Bluetooth permissions granted on resume");
//                    });
//                }
//            });
//        }
//
//        // USB 재연결 처리 중이면 스킵
//        if (isHandlingUsbReconnection) {
//            return;
//        }
//
//        try {
//            // ⚠️ 중요: 리시버만 체크하고, usbService는 체크하지 않음
//            // USB 재연결 시 onServiceDisconnected에서 usbService가 null이 될 수 있지만,
//            // 리시버가 등록되어 있으면 재바인딩만 시도하면 됨
//            if (usbReceiverRegisted) {
//                receivedMessageCnt = 0;
//                sendingMessageCnt = 0;
//                // 리시버는 이미 등록되어 있으므로, 서비스만 재바인딩 시도 (이미 바인딩되어 있으면 무시됨)
//                if (usbService == null) {
//                    logDebug(LogCategory.US, "USB receiver registered but service is null, attempting rebind");
//                    new Thread(() -> {
//                        startService(UsbService.class, usbConnection, null);
//                    }).start();
//                } else {
//                    logDebug(LogCategory.US, "USB service already initialized, skipping onResume setup");
//                }
//                return;
//            }
//
//            cancelResetTimer();
//
//            // 처음 초기화하는 경우
//            new Thread(() -> {
//                setFilters();  // Start listening notifications from UsbService
//                startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
//            }).start();
//        } catch (Exception e) {
//            logError(LogCategory.ER, Constants.ErrorMessages.ON_RESUME_USB_SERVICE_ERROR, e);
//        }
//    }
//
//    @Override
//    protected void onNewIntent(Intent intent) {
//        super.onNewIntent(intent);
//        setIntent(intent);  // ⚠️ 중요: 새로운 Intent를 현재 Intent로 설정
//
//        // USB 재연결 확인
//        if (intent != null && "USB_RECONNECTED".equals(intent.getAction())) {
//            logInfo(LogCategory.US, "USB reconnected - onNewIntent called");
//
//            // ⚠️ 플래그를 true로 설정 (onResume()에서 중복 실행 방지)
//            isHandlingUsbReconnection = true;
//
//            try {
//                resetStateForUsbReconnect();
//                // USB 재연결 처리 로직
//                handleUsbReconnection();
//            } finally {
//                // ⚠️ 플래그를 false로 리셋 (예외 발생 시에도 리셋)
//                isHandlingUsbReconnection = false;
//            }
//        }
//    }
//
//    /**
//     * USB 재연결 시 처리하는 메서드
//     * onNewIntent()에서 호출됨
//     */
//    private void handleUsbReconnection() {
//        try {
//            // ⚠️ 중요: USB 재연결 시에도 타이머가 자동으로 재시작되도록 함
//            // usbService가 null이 아니고 타이머가 없으면 시작 (재연결 시에도 동작)
//            if (usbService != null && (usbPollingFuture == null || usbPollingFuture.isCancelled())) {
//                // Phase 2: 단순 UI 업데이트를 scheduleUiUpdate로 변경
//                scheduleUiUpdate(() -> {
//                    tvConnectPlcRamp.setBackgroundColor(getBaseContext().getResources().getColor(R.color.blue_01));
//                    // tvConnectPlcRamp.setText(Constants.InitialValues.DEVICE_READY_CHARACTER);
//                });
//
//                System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> startUsbPolling 1");
//                startUsbPolling(true);
//                logInfo(LogCategory.US, "USB polling restarted after reconnection");
//            }
//        } catch (Exception e) {
//            logError(LogCategory.ER, "Error in handleUsbReconnection", e);
//        }
//    }
//
//    private void resetStateForUsbReconnect() {
//        try {
//            resetBluetoothSessionKeepUsb();
//            disconnectCheckCount = 0;
//            receivedMessageCnt = 0;
//            sendingMessageCnt = 0;
//        } catch (Exception e) {
//            logError(LogCategory.US, "Failed to reset state for USB reconnect", e);
//        }
//    }
//
//    private void startResetTimer() {
//        synchronized (finishedRestartTimerLock) {
//            cancelResetTimer();
//            resetCnt = 0;
//            tmrReset = new Timer("UsbResetTimer");
//            ttReset = new TimerTask() {
//                @Override
//                public void run() {
//                    logInfo(LogCategory.PS, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> resetCnt " + resetCnt + " usbReconnectAttempts " + usbReconnectAttempts);
//                    if (resetCnt == 30) {
//                        disconnectCheckCount = 0;
//                        receivedMessageCnt = 0;
//                        sendingMessageCnt = 0;
//                        usbReconnectAttempts = 0;
//                        resetCnt = 0;
//                        receiveCommandEmptyCnt = 0;
//                        resetBluetoothSessionKeepUsb();
//                        cancelResetTimer();
//                        return;
//                    }
//                    resetCnt++;
//                }
//            };
//            tmrReset.schedule(ttReset, 0, 1000);
//        }
//    }
//
//    private void cancelResetTimer() {
//        synchronized (finishedRestartTimerLock) {
//            if (tmrReset != null) {
//                try {
//                    disconnectCheckCount = 0;
//                    receivedMessageCnt = 0;
//                    sendingMessageCnt = 0;
//                    usbReconnectAttempts = 0;
//                    resetCnt = 0;
//                    receiveCommandEmptyCnt = 0;
//                    tmrReset.cancel();
//                    tmrReset.purge();
//                } catch (Exception ignored) {
//                }
//            }
//            if (ttReset != null) {
//                try {
//                    ttReset.cancel();
//                } catch (Exception ignored) {
//                }
//            }
//            tmrReset = null;
//            ttReset = null;
//        }
//    }
//
//    @Override
//    public void onPause() {
//        super.onPause();
//    }
//
//    // ⚠️ 키오스크 모드: BaseKioskActivity에서 onBackPressed, onWindowFocusChanged, onUserInteraction 자동 처리됨
//    // KioskModeApplication의 ActivityLifecycleCallbacks가 onCreate, onResume 등에서도 자동 적용함
//
//    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
//        // 기존 서비스가 연결돼 있다면 안전하게 해제 후 재시작
//        boolean needsRestart = usbService == null || !UsbService.SERVICE_CONNECTED;
//        if (!needsRestart) {
//            logDebug(LogCategory.US, "USB service already active; skipping restart");
//            receivedMessageCnt = 0;
//            sendingMessageCnt = 0;
//            disconnectCheckCount = 0;
//            return;
//        }
//
//        Intent startServiceIntent = new Intent(this, service);
//        if (extras != null && !extras.isEmpty()) {
//            Set<String> keys = extras.keySet();
//            for (String key : keys) {
//                String extra = extras.getString(key);
//                startServiceIntent.putExtra(key, extra);
//            }
//        }
//        startService(startServiceIntent);
//
//        Intent bindingIntent = new Intent(this, service);
//        try {
//            bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
//        } catch (Exception e) {
//            logDebug(LogCategory.US, "USB service bind attempt: " + e.getMessage());
//        }
//    }
//
//    private void setFilters() {
//        try {
//            if (usbReceiverRegisted) {
//                return;
//            }
//
//            IntentFilter filter = new IntentFilter();
//            filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
//            filter.addAction(UsbService.ACTION_NO_USB);
//            filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
//            filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
//            filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
//
//            // ⚠️ 중요: 리시버 중복 등록 예외 방지
//            try {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                    registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
//                } else {
//                    ContextCompat.registerReceiver(this, usbReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
//                }
//                usbReceiverRegisted = true;  // 등록 플래그 설정
//            } catch (IllegalArgumentException e) {
//                // 이미 등록되어 있는 경우 (예외 발생 시에도 플래그는 true로 설정)
//                logDebug(LogCategory.US, "USB receiver already registered: " + e.getMessage());
//                usbReceiverRegisted = true;
//            }
//        } catch (Exception e) {
//            logError(LogCategory.ER, Constants.ErrorMessages.SET_FILTERS_ERROR, e);
//            // ⚠️ 중요: 예외 발생 시에도 액티비티는 계속 실행되도록 함
//        }
//    }
//
//    private class UsbHandler extends Handler {
//        // private String dataBuffer = "";
//        private final WeakReference<ActivityModel_0003> mActivity;
//
//        public UsbHandler(ActivityModel_0003 activity) {
//            mActivity = new WeakReference<>(activity);
//        }
//
//        @Override
//        public void handleMessage(Message msg) {
//            switch (msg.what) {
//                case UsbService.MESSAGE_FROM_SERIAL_PORT:
//                    try {
//                        String data = (String) msg.obj;
//                        try {
//                            dataBuffer += data;
//                            // HTTP 통신 관련 runOnUiThread 최적화: scheduleUiUpdate 사용 (USB Handler 내부이지만 HTTP 통신 후 처리)
//                            scheduleUiUpdate(() -> {
//                                usbConnPermissionGranted = true;
//                                tvConnectPlcRamp.setBackgroundColor(getBaseContext().getResources().getColor(R.color.blue_01));
//                                // tvConnectPlcRamp.setText(Constants.InitialValues.DEVICE_READY_CHARACTER);
//                            });
//
//                            dataBuffer = (dataBuffer.replace(Constants.CharCodes.ETX, "")).replace(Constants.CharCodes.ACK, "");
//                            // logInfo(LogCategory.US, "▶ [US] dataBuffer " + dataBuffer);
//
//                            if (dataBuffer.contains(Constants.PLCCommands.RSS_RESPONSE_HEADER) && dataBuffer.length() >= 13) {
//                                int s = dataBuffer.indexOf(Constants.PLCCommands.RSS_RESPONSE_HEADER) + Constants.PLCCommands.RSS_RESPONSE_HEADER.length();
//                                int e = s + 4;
//                                decElectricValue = Integer.parseInt(dataBuffer.substring(s, e), 16);
//
//                                if (!usbReceiverRegisted) {
//                                    usbReceiverRegisted = true;
//                                }
//
//                                lstWatt.add(decElectricValue);
//                                recordWattMeasurement(currentTestItem, decElectricValue);
//                                dataBuffer = "";
//                            }
//
//                            if (dataBuffer.length() > 60) {
//                                dataBuffer = "";
//                            }
//                        } catch (Exception e) {
//                            dataBuffer = "";
//                        }
//                    } catch (Exception e) {
//                        logError(LogCategory.ER, Constants.ErrorMessages.USB_HANDLER_MESSAGE_PROCESSING_ERROR, e);
//                    }
//                    break;
//                case UsbService.CTS_CHANGE:
//                    // Toast.makeText(mActivity.get(), Constants.LogMessages.CTS_CHANGE, Toast.LENGTH_LONG).show();
//                    break;
//                case UsbService.DSR_CHANGE:
//                    // Toast.makeText(mActivity.get(), Constants.LogMessages.DSR_CHANGE, Toast.LENGTH_LONG).show();
//                    break;
//            }
//        }
//    }
//
//    public void readTemperatureExcel(String tableType, String fileName) {
//        new Thread(() -> {
//            try {
//                try {
//                    String tableName = (tableType.equals("1")) ? Constants.Common.TABLE_COLD_TEMPERATURE : Constants.Common.TABLE_HOT_TEMPERATURE;
//                    InputStream is = getBaseContext().getResources().getAssets().open(fileName);
//                    Workbook wb = Workbook.getWorkbook(is);
//
//                    if (wb != null) {
//                        Sheet sheet = wb.getSheet(Integer.parseInt(tableType) - 1);
//                        if (sheet != null) {
//                            int colTotal = sheet.getColumns();
//                            int rowIndexStart = 1;
//                            int rowTotal = sheet.getColumn(colTotal - 1).length;
//
//                            List<Map<String, String>> lstTemperature = new ArrayList<Map<String, String>>();
//                            Map<String, String> mapTemperature = null;
//
//                            for (int row = rowIndexStart; row < rowTotal; row++) {
//                                mapTemperature = new HashMap<String, String>();
//                                mapTemperature.put(Constants.Common.CLM_TEMP_SEQ, String.valueOf(row));
//                                mapTemperature.put(Constants.JsonKeys.CLM_TEMPERATURE, sheet.getCell(1, row).getContents());
//                                mapTemperature.put(Constants.Common.CLM_REGIST, sheet.getCell(2, row).getContents());
//                                mapTemperature.put(Constants.Common.CLM_VOLTAGE, sheet.getCell(3, row).getContents());
//                                mapTemperature.put(Constants.Common.CLM_10_BIT, sheet.getCell(4, row).getContents());
//                                mapTemperature.put(Constants.JsonKeys.CLM_12_BIT, sheet.getCell(6, row).getContents());
//                                mapTemperature.put(Constants.JsonKeys.CLM_COMMENT, "");
//                                lstTemperature.add(mapTemperature);
//                            }
//
//                            // WeakReference를 통한 안전한 Activity 접근
//                            ActivityModel_0003 activity = getMainActivity();
//                            if (activity != null && !activity.isFinishing()) {
//                                if (lstTemperature.size() != TestData.selectTemperatureData(activity, tableType).size()) {
//                                    TestData.deleteTemperatureData(activity, tableType);
//                                    for (int i = 0; i < lstTemperature.size(); i++) {
//                                        TestData.insertTemperatureData(activity, tableType, lstTemperature.get(i));
//                                    }
//                                }
//                            }
//                        }
//                    }
//                } catch (IOException e) {
//                    logError(LogCategory.ER, Constants.ErrorMessages.READ_TEMPERATURE_EXCEL_IO_EXCEPTION, e);
//                } catch (BiffException e) {
//                    logError(LogCategory.ER, Constants.ErrorMessages.READ_TEMPERATURE_EXCEL_BIFF_EXCEPTION, e);
//                }
//            } catch (Exception e) {
//                logError(LogCategory.ER, Constants.ErrorMessages.READ_TEMPERATURE_EXCEL_ERROR, e);
//                // e.printStackTrace();
//            }
//        }).start();
//    }
//
//    public void callBarcodeInfoServer() {
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                ActivityModel_0003.RequestThreadBarcode thread = new ActivityModel_0003.RequestThreadBarcode(); // Thread 생성
//                thread.start(); // Thread 시작
//            }
//        }).start();
//    }
//
//    public class RequestThreadBarcode extends Thread {
//        @Override
//        public void run() {
//            try {
//                if (aTryingCnt == 5) {
//                    aTryingCnt = 0;
//                }
//                // 동시성 문제 해결: connection을 로컬 변수로 변경 (각 스레드가 자신만의 connection 사용)
//                HttpURLConnection connection = null;
//                try {
//                    URL url = new URL(urlStrBarcode);
//                    // Log.i(TAG, "▶ [SI] urlStrBarcode " + urlStrBarcode);
//                    connection = (HttpURLConnection) url.openConnection();
//                    if (connection != null) {
//                        connection.setConnectTimeout(Constants.Timeouts.HTTP_CONNECT_TIMEOUT_MS);
//                        connection.setReadTimeout(Constants.Timeouts.HTTP_READ_TIMEOUT_MS);
//                        connection.setRequestMethod("GET");
//                        connection.setDoInput(true);
//                        connection.setDoOutput(true);
//
//                        int resCode = connection.getResponseCode();
//                        logInfo(LogCategory.BI, String.format(Constants.LogMessages.PRODUCT_SERIAL_INFO_CALL_FORMAT, urlStrBarcode, resCode, (resCode == HttpURLConnection.HTTP_OK)));
//                        if (resCode == HttpURLConnection.HTTP_OK) {
//                            logInfo(LogCategory.BI, String.format(Constants.LogMessages.PRODUCT_SERIAL_INFO_CALL_SUCCESS, resCode));
//                            barcodeReadCheck = true;
//                            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//                            try {
//                                String line = null;
//                                String lineTmp = null;
//                                StringBuilder sb = new StringBuilder();
//
//                                // 무한 루프 방지를 위해 최대 읽기 횟수 제한
//                                int maxReadCount = 10000;
//                                int readCount = 0;
//
//                                while (readCount < maxReadCount) {
//                                    lineTmp = reader.readLine();
//                                    if (lineTmp == null) {
//                                        break; // 스트림 끝에 도달
//                                    }
//                                    readCount++;
//
//                                    if (!lineTmp.trim().equals("")) {
//                                        sb.append(lineTmp);
//                                        line = lineTmp;
//                                    }
//                                }
//
//                                // 모든 데이터를 읽은 후 한 번만 처리
//                                final String data = (line != null && !line.trim().equals("")) ? line :
//                                        (sb.length() > 0 ? sb.toString() : null);
//
//                                if (data != null && !data.trim().equals("")) {
//                                    logDebug(LogCategory.BI, Constants.LogMessages.PRODUCT_SERIAL_INFO_CALL_DATA_RECEIVED);
//                                    jsonParsingBarcode(Constants.JsonKeys.PRODUCT_SERIAL, data);
//                                }
//                            } finally {
//                                try {
//                                    reader.close();
//                                } catch (IOException e) {
//                                    logError(LogCategory.ER, Constants.ErrorMessages.ERROR_CLOSING_READER_IN_REQUEST_THREAD_BARCODE, e);
//                                }
//                            }
//                        }
//
//                        if (resCode != HttpURLConnection.HTTP_OK) {
//                            logWarn(LogCategory.BI, String.format(Constants.LogMessages.PRODUCT_SERIAL_INFO_CALL_FAILED, resCode));
//                        }
//                    }
//                } finally {
//                    // 리소스 정리 보장
//                    safeDisconnectConnection(connection);
//                }
//            } catch (Exception e) { //예외 처리
//                logError(LogCategory.ER, Constants.ErrorMessages.REQUEST_THREAD_BARCODE_ERROR, e);
//            }
//        }
//    }
//
//    public void jsonParsingBarcode(String data_type, String json) {
//        new Thread(() -> {
//            try {
//                JSONObject jsonObject = new JSONObject(json);
//                JSONArray testItemArray = jsonObject.getJSONArray(Constants.JsonKeys.PRODUCT_SERIAL);
//
//                List<Map<String, String>> lstData = new ArrayList<Map<String, String>>();
//                Map<String, String> mapData = null;
//                logDebug(LogCategory.SI, Constants.LogMessages.JSON_PARSING_BARCODE_RECEIVED);
//
//                if (data_type.equals(Constants.JsonKeys.PRODUCT_SERIAL)) {
//                    logDebug(LogCategory.SI, "testItemArray.length(): " + testItemArray.length());
//                    for (int i = 0; i < testItemArray.length(); i++) {
//                        JSONObject testItemObject = testItemArray.getJSONObject(i);
//                        mapData = new HashMap<String, String>();
//                        mapData.put(Constants.JsonKeys.CLM_PRODUCT_SERIAL_NO, testItemObject.getString(Constants.JsonKeys.CLM_PRODUCT_SERIAL_NO));
//                        lstData.add(mapData);
//                    }
//
//                    logDebug(LogCategory.SI, "lstData.size(): " + lstData.size());
//                    for (int i = 0; i < lstData.size(); i++) {
//                        try {
//                            productSerialNo = lstData.get(i).get(Constants.JsonKeys.CLM_PRODUCT_SERIAL_NO);
//                            logInfo(LogCategory.BI, String.format(Constants.LogMessages.PRODUCT_SERIAL_INFO_CALL, i, productSerialNo));
////                            Log.i(TAG, "▶ productSerialNo(" + i + ") " + productSerialNo);
//                        } catch (Exception e) {
//                            logError(LogCategory.ER, Constants.ErrorMessages.PRODUCT_SERIAL_PARSING_ERROR, e);
//                        }
//                    }
//
//                    // HTTP 통신 관련 runOnUiThread 최적화: 바코드 요청 후 UI 업데이트를 scheduleUiUpdate로 변경
//                    scheduleUiUpdate(() -> {
//                        logInfo(LogCategory.SI, String.format("globalProductSerialNo: %s, productSerialNo: %s", globalProductSerialNo, productSerialNo));
//                        tvSerialNo.setText(productSerialNo);
//                        tvDialogBarcode.setText(productSerialNo);
//                        blnBarcodeReceived = true;
//                        aTryingCnt = 0;
////                            barcodeRequestTimer.cancel();
//
//                        SharedPreferences test = getSharedPreferences(Constants.SharedPrefKeys.TEST_COOKIE_INFO, MODE_PRIVATE);
//                        globalProductSerialNo = test.getString(Constants.SharedPrefKeys.TEST_PRODUCT_SERIAL_NO, "");
//                        logInfo(LogCategory.SI, String.format("globalProductSerialNo: %s, productSerialNo: %s", globalProductSerialNo, productSerialNo));
//                    });
//
//                    test_info.putString(Constants.JsonKeys.CLM_PRODUCT_SERIAL_NO, productSerialNo);
//                    test_info.commit();
//                    getPreferences();
//                }
//            } catch (JSONException e) {
//                logError(LogCategory.ER, Constants.ErrorMessages.JSON_PARSING_BARCODE_JSON_EXCEPTION, e);
//            }
//        }).start();
//    }
//
//    /**
//     * 모든 리소스를 안전하게 정리하는 통합 메소드
//     * Activity 종료 시 반드시 호출되어야 함
//     */
//    private void cleanupAllResources() {
//        try {
//            // 1. 모든 Timer 정리
//            cleanupAllTimers();
//
//            // 2. Handler 메시지 큐 정리
//            clearHttpHandlerQueue();
//            clearBtHandlerQueue();
//            if (mainHandler != null) {
//                mainHandler.removeCallbacksAndMessages(null);
//            }
//            if (btHandler != null) {
//                btHandler.removeCallbacksAndMessages(null);
//            }
//
//            // 3. USB 관련 리소스 정리
//            cleanupUsbResources();
//
//            // 4. Bluetooth 관련 리소스 정리
//            cleanupBluetoothResources();
//
//            // 5. AsyncTask 정리
//            cleanupAsyncTasks();
//
//            // 6. HTTP 연결 정리
//            cleanupHttpConnections();
//
//            // 7. 로그 배치 큐 정리
//            clearLogBatchQueue();
//
//            // 8. WeakReference 정리
//            clearMainActivityReference();
//
//            // 9. SharedPreferences Editor 정리
//            if (test_info != null) {
//                test_info.apply(); // 또는 commit()
//            }
//            if (cookie_info != null) {
//                cookie_info.apply(); // 또는 commit()
//            }
//
//            logInfo(LogCategory.PS, "All resources cleaned up successfully");
//        } catch (Exception e) {
//            logError(LogCategory.ER, "Error cleaning up resources", e);
//        }
//    }
//
//    private void resetActivityState() {
//        try {
//            logInfo(LogCategory.PS, "Resetting ActivityModel_0003 state");
//
//            // 기본 플래그 및 카운터
//            entranceCheck = false;
//            barcodeReadCheck = false;
//            testProcessStarted = false;
//            finishedCorrectly = false;
//            shouldUpdateDialog = false;
//            btPermissionsGranted = false;
//            permissionRequestInProgress = false;
//            permissionDialogShowing = false;
//            btConnectionInProgress = false;
//            btConnected = false;
//            isConnected = false;
//            btSearchOnOff = false;
//            blnBarcodeReceived = false;
//            btMessageTimerRunning.set(false);
//            unfinishedRestartTimerRunning.set(false);
//            finishedRestartTimerRunning.set(false);
//            remoteCommandTimerRunning.set(false);
//
//            // USB 상태
//            usbConnTryCnt = 0;
//            usbConnPermissionGranted = false;
//            usbPollingRequested = false;
//            usbPollingEnabled = false;
//            usbPollingFailureCount = 0;
//            isUsbReconnecting = false;
//            usbReconnectAttempts = 0;
//            usbPollingFuture = null;
//            usbPermissionRecoveryRunnable = null;
//            usbReconnectRunnable = null;
//            usbService = null;
//
//            // Bluetooth 상태
//            btHandler = null;
//            btConnectedThread = null;
//            btSocket = null;
//            deviceSelected = null;
//            btDeviceName = "";
//            btDeviceAddr = "";
//            btInfoList = null;
//
//            // 타이머/쓰레드 참조
//            tmrBTMessageSend = null;
//            ttBTMessageSend = null;
//            tmrFinishedRestart = null;
//            ttFinishedRestart = null;
//            tmrUnfinishedRestart = null;
//            ttUnfinishedRestart = null;
//            appResetTimerTask = null;
//            barcodeRequestTimer = null;
//            testTaskThread = null;
//            pendingHttpTask = null;
//            btReconnectRunnable = null;
//
//            // 데이터 및 컬렉션
//            if (lstTestResult != null) {
//                lstTestResult.clear();
//                lstTestResult = null;
//            }
//            lstTestTemperature = null;
//            mapTestTemperature = null;
//            lstData = null;
//            lstDataTmp = null;
//            temperatureData = null;
//            coldTemperatureData = null;
//            hotTemperatureData = null;
//            listItemAdapter = null;
//            specCache.clear();
//            latestWattByCommand.clear();
//            if (pendingUiUpdates != null) {
//                synchronized (uiUpdateLock) {
//                    pendingUiUpdates.clear();
//                    uiUpdateBatchTask = null;
//                }
//            }
//
//            // 문자열/기타 값 초기화
//            logText = "";
//            logTextParam = "";
//            productSerialNo = "";
//            testProcessId = "";
//            readMessage = null;
//            receiveCommand = "";
//            receiveCommandResponse = "";
//            receiveCommandResponseOK = "";
//            receiveCompAgingResponse = Constants.InitialValues.RECEIVE_COMP_AGING_RESPONSE;
//            receiveResponseResult = Constants.InitialValues.RECEIVE_RESPONSE_RESULT;
//            resultInfo = Constants.Common.EMPTY_STRING;
//            currentTestItem = Constants.InitialValues.CURRENT_TEST_ITEM;
//            valueWatt = Constants.InitialValues.VALUE_WATT;
//            lowerValueWatt = Constants.InitialValues.LOWER_VALUE_WATT;
//            upperValueWatt = Constants.InitialValues.UPPER_VALUE_WATT;
//            currentPumpWattValueArr = "";
//            urlStr = "";
//            urlTestTaskStr = "";
//            urlStrBarcode = "";
//            serverIp = "";
//            serverDomain = "";
//            serverResetIp = "";
//            btDeviceName = "";
//            btDeviceAddr = "";
//            modeType = Constants.InitialValues.MODE_TYPE;
//
//            // 카운터 초기화
//            testOkCnt = 0;
//            testNgCnt = 0;
//            totalTimeCnt = 0;
//            testItemIdx = 0;
//            testItemCounter = 0;
//            testTotalCounter = 0;
//            test_version_id = "";
//            model_id = "";
//            restartCntFinished = 0;
//            restartCntUnfinished = 0;
//            restartCntMargin = 0;
//            sendingMessageCnt = 0;
//            receivedMessageCnt = 0;
//            disconnectCheckCount = 0;
//            sensorNgCount = 0;
//            wattLower = 0;
//            wattUpper = 0;
//
//            // 배열/맵 초기화
//            arrTestItems = null;
//            mapTestTemperature = null;
//
//            // UI 상태
//            clAlert = null;
//            clDialogForPreprocess = null;
//            clTestResult = null;
//            clDialogForLogger = null;
//            clLog = null;
//
//            tvAlertMessage = null;
//            tvDialogMessage = null;
//            tvUnitId = null;
//            tvPopupProcessResult = null;
//            tvPopupProcessResultCompValue = null;
//            tvPopupProcessResultHeaterValue = null;
//            tvCompWattValue = null;
//            tvHeaterWattValue = null;
//            tvPumpWattValue = null;
//            tvCompValueWatt = null;
//            tvHeaterValueWatt = null;
//            tvPumpValueWatt = null;
//            tvCompLowerValueWatt = null;
//            tvCompUpperValueWatt = null;
//            tvPumpLowerValueWatt = null;
//            tvPumpUpperValueWatt = null;
//            tvHeaterLowerValueWatt = null;
//            tvHeaterUpperValueWatt = null;
//            tvEllapsedTimeCnt = null;
//            tvModelName = null;
//            tvModelNationality = null;
//            tvSerialNo = null;
//            tvModelId = null;
//            tvDialogBarcode = null;
//            tvCurrentProcess = null;
//            tvConnectBtRamp = null;
//            tvConnectPlcRamp = null;
//            tvRunWsRamp = null;
//            tvTestOkCnt = null;
//            tvTestNgCnt = null;
//            tvWattValue = null;
//            tvLog = null;
//            tvResourceInfo = null;
//            mBluetoothStatus = null;
//            mReadBuffer = null;
//            mReadText = null;
//            tvCurrentVersion = null;
//
//            btnAlertClose = null;
//            btnTestResultClose = null;
//            btnTestRestart = null;
//            btnTestHistoryList = null;
//            lvTestItem = null;
//
//            binding = null;
//
//            logInfo(LogCategory.PS, "Activity state reset completed");
//        } catch (Exception e) {
//            logError(LogCategory.ER, "Failed to reset Activity state", e);
//        }
//    }
//
//    /**
//     * USB 관련 리소스 정리
//     */
//    private void cleanupUsbResources() {
//        try {
//            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> stopUsbPolling 9");
//            stopUsbPolling();
//
//            // USB Receiver 해제 (예외 처리 포함)
//            if (usbReceiverRegisted) {
//                try {
//                    unregisterReceiver(usbReceiver);
//                } catch (IllegalArgumentException e) {
//                    // 이미 해제된 경우 (정상 상황)
//                    logDebug(LogCategory.US, "USB receiver already unregistered");
//                }
//                usbReceiverRegisted = false;
//            }
//
//            // USB 서비스 바인딩 해제
//            if (usbConnPermissionGranted) {
//                try {
//                    unbindService(usbConnection);
//                } catch (IllegalArgumentException e) {
//                    // 이미 해제된 경우
//                    logDebug(LogCategory.US, "USB service already unbound");
//                }
//                usbConnPermissionGranted = false;
//            }
//
//            // USB 서비스 중지 (선택적)
//            if (usbService != null) {
//                try {
//                    Intent usbIntent = new Intent(this, UsbService.class);
//                    stopService(usbIntent);
//                } catch (Exception e) {
//                    logWarn(LogCategory.US, "Error stopping USB service: " + e.getMessage());
//                }
//                usbService = null;
//            }
//        } catch (Exception e) {
//            logError(LogCategory.ER, "Error cleaning up USB resources", e);
//        }
//    }
//
//    /**
//     * Bluetooth 관련 리소스 정리
//     */
//    private void cleanupBluetoothResources() {
//        try {
//            // Bluetooth ConnectedThread 정리
//            if (btConnectedThread != null) {
//                try {
//                    btConnectedThread.cancel(); // ConnectedThreadOptimized에 cancel() 메소드가 있다고 가정
//                } catch (Exception e) {
//                    logWarn(LogCategory.BT, "Error canceling Bluetooth thread: " + e.getMessage());
//                }
//                btConnectedThread = null;
//            }
//
//            clearBluetoothReconnect();
//            btConnectionInProgress = false;
//            btReconnectDelayMs = Constants.Timeouts.BT_LIST_TIMER_INTERVAL_MS;
//
//            // Bluetooth Socket 정리
//            if (btSocket != null && btSocket.isConnected()) {
//                try {
//                    btSocket.close();
//                } catch (IOException e) {
//                    logWarn(LogCategory.BT, "Error closing Bluetooth socket: " + e.getMessage());
//                }
//            }
//            btSocket = null;
//
//            // Bluetooth Adapter 정리
//            cancelDiscoverySafe();
//
//            // Bluetooth 관련 변수 초기화
//            deviceSelected = null;
//            btSearchOnOff = false;
//            btConnected = false;
//            isConnected = false;
//
//        } catch (Exception e) {
//            logError(LogCategory.ER, "Error cleaning up Bluetooth resources", e);
//        }
//    }
//
//    /**
//     * AsyncTask 정리
//     */
//    private void cleanupAsyncTasks() {
//        try {
//            // RequestThreadAsync 정리
//            // AsyncTask는 인스턴스 변수로 관리되지 않으므로,
//            // 실행 중인 경우에만 cancel() 호출 가능
//            // 주의: AsyncTask는 한 번만 실행 가능하므로 재사용 불가
//
//            // RequestTestTaskThreadAsync 정리
//            if (testTaskThread != null) {
//                try {
//                    testTaskThread.cancel(true);
//                } catch (Exception e) {
//                    logWarn(LogCategory.PS, "Error canceling test task thread: " + e.getMessage());
//                }
//                testTaskThread = null;
//            }
//        } catch (Exception e) {
//            logError(LogCategory.ER, "Error cleaning up AsyncTasks", e);
//        }
//    }
//
//    /**
//     * HTTP 연결 정리
//     */
//    private void cleanupHttpConnections() {
//        try {
//            // Static connection 정리
//            safeDisconnectConnection(connection);
//            connection = null;
//
//            // 로컬 connection들은 각 메소드에서 finally 블록으로 정리됨
//        } catch (Exception e) {
//            logError(LogCategory.ER, "Error cleaning up HTTP connections", e);
//        }
//    }
//
//    // ==================== 키오스크 모드 관련 메소드 ====================
//
//    /**
//     * 완전한 키오스크 모드 설정
//     * 시스템 UI 숨기기, 화면 항상 켜두기, 화면 회전 고정 등을 포함
//     */
//    private void enableFullKioskMode() {
//        try {
//            // 1. 시스템 UI 숨기기
//            setupKioskMode();
//
//            // 2. 화면 항상 켜두기
//            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//
//            // 3. 화면 회전 고정 (가로 모드)
//            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//
//            // 4. Screen Pinning 활성화 (선택적 - 필요시 주석 해제)
//            // enableScreenPinning();
//
//            logInfo(LogCategory.PS, "Full kiosk mode enabled");
//        } catch (Exception e) {
//            logError(LogCategory.ER, "Error enabling full kiosk mode", e);
//        }
//    }
//
//    /**
//     * 키오스크 모드 설정 (시스템 UI 완전히 숨기기)
//     * Android 버전에 따라 다른 API 사용
//     * ⚠️ 중요: 시스템 UI가 나타나지 않도록 지속적으로 모니터링
//     */
//    private void setupKioskMode() {
//        try {
//            // Android 11 (API 30) 이상
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                getWindow().setDecorFitsSystemWindows(false);
//                WindowInsetsController controller = getWindow().getInsetsController();
//                if (controller != null) {
//                    // 상태바와 내비게이션 바 숨기기
//                    controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
//                    // ⚠️ 중요: BEHAVIOR_DEFAULT 사용 (스와이프로 나타나지 않도록)
//                    // BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE는 스와이프로 나타나게 함
//                    controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_DEFAULT);
//                }
//            } else {
//                // Android 10 이하
//                View decorView = getWindow().getDecorView();
//                int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN
//                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
//                decorView.setSystemUiVisibility(uiOptions);
//
//                // 시스템 UI가 다시 나타나는 것을 방지하는 리스너
//                decorView.setOnSystemUiVisibilityChangeListener(
//                        new View.OnSystemUiVisibilityChangeListener() {
//                            @Override
//                            public void onSystemUiVisibilityChange(int visibility) {
//                                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
//                                    // 시스템 UI가 다시 나타나면 즉시 숨기기
//                                    setupKioskMode();
//                                }
//                            }
//                        }
//                );
//            }
//
//            // ⚠️ 중요: 이벤트 기반으로 즉시 시스템 UI 숨기기
//            hideSystemUI();
//        } catch (Exception e) {
//            logError(LogCategory.ER, "Error setting up kiosk mode", e);
//        }
//    }
//
//    private long lastHideSystemUITime = 0;
//    private static final long HIDE_SYSTEM_UI_DEBOUNCE_MS = 100;
//
//    /**
//     * 키오스크 모드 지속적 모니터링 시작
//     * 주기적으로 시스템 UI를 숨겨서 사용자가 접근할 수 없도록 함
//     */
//    /**
//     * 시스템 UI 강제로 숨기기
//     * 모든 Android 버전에서 작동하는 강력한 방법
//     */
//    public void hideSystemUI() {
//        try {
//            long currentTime = System.currentTimeMillis();
//            if (currentTime - lastHideSystemUITime < HIDE_SYSTEM_UI_DEBOUNCE_MS) {
//                return; // 너무 자주 호출되는 경우 스킵
//            }
//            lastHideSystemUITime = currentTime;
//
//            View decorView = getWindow().getDecorView();
//
//            // Android 11 (API 30) 이상
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                WindowInsetsController controller = getWindow().getInsetsController();
//                if (controller != null) {
//                    controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
//                }
//            } else {
//                // Android 10 이하
//                int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN
//                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
//                decorView.setSystemUiVisibility(uiOptions);
//            }
//        } catch (Exception e) {
//            // 조용히 실패 (너무 자주 호출되므로 로그는 남기지 않음)
//        }
//    }
//
//    /**
//     * Screen Pinning 활성화 (앱 고정 모드)
//     * Android 5.0 (API 21) 이상에서 지원
//     * 주의: 사용자가 뒤로가기 + 최근 앱 버튼을 동시에 누르면 해제 가능
//     */
//    private void enableScreenPinning() {
//        try {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                // ⚠️ 중요: isLockTaskModeSupported()는 존재하지 않는 메소드입니다
//                // Android 6.0 (API 23) 이상에서는 getLockTaskModeState()로 상태 확인 가능
//                // 하지만 직접 startLockTask()를 호출하고 예외 처리하는 것이 더 안전합니다
//
//                // Android 6.0 이상에서는 상태 확인 후 호출
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                    ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
//                    if (activityManager != null) {
//                        int lockTaskMode = activityManager.getLockTaskModeState();
//                        // 이미 Lock Task 모드가 아닌 경우에만 시작
//                        if (lockTaskMode == ActivityManager.LOCK_TASK_MODE_NONE) {
//                            startLockTask();
//                            logInfo(LogCategory.PS, "Screen pinning enabled");
//                        } else {
//                            logDebug(LogCategory.PS, "Screen pinning already active (mode: " + lockTaskMode + ")");
//                        }
//                    }
//                } else {
//                    // Android 5.0-5.1: 직접 호출 (예외 처리로 안전하게)
//                    startLockTask();
//                    logInfo(LogCategory.PS, "Screen pinning enabled");
//                }
//            }
//        } catch (SecurityException e) {
//            // Lock Task 모드가 지원되지 않거나 권한이 없는 경우
//            logWarn(LogCategory.PS, "Lock task mode not supported or permission denied: " + e.getMessage());
//        } catch (Exception e) {
//            logError(LogCategory.ER, "Error enabling screen pinning", e);
//        }
//    }
//
//    /**
//     * Screen Pinning 비활성화
//     */
//    private void disableScreenPinning() {
//        try {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                stopLockTask();
//                logInfo(LogCategory.PS, "Screen pinning disabled");
//            }
//        } catch (Exception e) {
//            logError(LogCategory.ER, "Error disabling screen pinning", e);
//        }
//    }
//
//    private void applyUiBundle(UiUpdateBundle bundle) {
//        if (bundle == null) {
//            return;
//        }
//
//        if (bundle.dialogVisible) {
//            if (clDialogForPreprocess.getVisibility() != VISIBLE) {
//                clDialogForPreprocess.setVisibility(VISIBLE);
//            }
//            tvDialogMessage.setText(bundle.dialogMessage);
//            trPreprocessContent.setBackgroundColor(bundle.dialogColor);
//        } else if (bundle.dialogHidden) {
//            clDialogForPreprocess.setVisibility(View.INVISIBLE);
//            tvDialogMessage.setText(Constants.Common.EMPTY_STRING);
//        }
//
//        if (!TextUtils.isEmpty(bundle.currentProcessName)) {
//            tvCurrentProcess.setText(bundle.currentProcessName);
//        }
//
//        if (!TextUtils.isEmpty(bundle.temperatureText)) {
//            tvTemperature.setText(bundle.temperatureText);
//        }
//
//        if (!TextUtils.isEmpty(bundle.compWattText)) {
//            tvCompWattValue.setText(bundle.compWattText);
//            tvPopupProcessResultCompValue.setText(bundle.compWattText);
//        }
//
//        if (!TextUtils.isEmpty(bundle.heaterWattText)) {
//            tvHeaterWattValue.setText(bundle.heaterWattText);
//            tvPopupProcessResultHeaterValue.setText(bundle.heaterWattText);
//        }
//
//        if (!TextUtils.isEmpty(bundle.pumpWattText)) {
//            tvPumpWattValue.setText(bundle.pumpWattText);
//        }
//
//        if (!TextUtils.isEmpty(bundle.logText)) {
//            tvLog.setText(bundle.logText);
//        }
//
//        if (bundle.updateListAdapter && bundle.listItemAdapter != null && !TextUtils.isEmpty(bundle.updateItemCommand)) {
//            boolean itemUpdated = false;
//            for (int i = 0; i < bundle.listItemAdapter.getCount(); i++) {
//                VoTestItem item = (VoTestItem) bundle.listItemAdapter.getItem(i);
//                if (!bundle.updateItemCommand.equals(item.getTest_item_command())) {
//                    continue;
//                }
//
//                itemUpdated = true;
//
//                if (bundle.receiveCommandResponseOK != null && bundle.receiveCommandResponseOK.equals(bundle.updateItemCommand) && bundle.updateItemResult.equals(Constants.ResultStatus.NG)) {
//                    // placeholder for NG specific logging
//                }
//                if (Constants.TestItemCodes.CM0100.equals(item.getTest_item_command())) {
//                    item.setTest_item_name(item.getTest_item_name() + Constants.Common.LOGGER_DEVIDER_01 + bundle.updateItemNameSuffix);
//                }
//                if (bundle.updateItemCommand.contains(Constants.TestItemCodes.CM0101)) {
//                    item.setTest_item_info(bundle.temperatureValueCompDiff);
//                }
//                if (bundle.updateItemCommand.contains(Constants.TestItemCodes.HT0101) ||
//                        bundle.updateItemCommand.contains(Constants.TestItemCodes.PM0101) ||
//                        bundle.updateItemCommand.contains(Constants.TestItemCodes.SV0101) ||
//                        bundle.updateItemCommand.contains(Constants.TestItemCodes.SV0201) ||
//                        bundle.updateItemCommand.contains(Constants.TestItemCodes.SV0301) ||
//                        bundle.updateItemCommand.contains(Constants.TestItemCodes.SV0401)) {
//                    item.setTest_item_info(bundle.resultInfo);
//                }
//                if (bundle.updateItemCommand.contains(Constants.TestItemCodes.TH0101)) {
//                    item.setTest_item_info(bundle.decTemperatureHotValue);
//                }
//                if (bundle.updateItemCommand.contains(Constants.TestItemCodes.TH0201)) {
//                    item.setTest_item_info(bundle.decTemperatureColdValue);
//                }
//                item.setTest_result_check_value(bundle.updateItemCheckValue);
//                item.setTest_item_result(bundle.updateItemResult);
//                item.setTest_finish_yn(Constants.ResultStatus.YES);
//                if (bundle.finalReadMessage != null) {
//                    item.setTest_bt_raw_message(bundle.finalReadMessage.substring(bundle.finalReadMessage.indexOf(Constants.CharCodes.STX) + 1, bundle.finalReadMessage.indexOf(Constants.CharCodes.ETX)));
//                }
//                if (bundle.finalReceiveCommandResponse != null) {
//                    item.setTest_bt_raw_response(bundle.finalReceiveCommandResponse);
//                }
//                if (!TextUtils.isEmpty(bundle.finalCalculatedResultValue)) {
//                    item.setTest_bt_processed_value(bundle.finalCalculatedResultValue);
//                }
//            }
//
//            if (bundle.shouldUpdateCounts && itemUpdated) {
//                recalcTestCountsFromAdapter(bundle.listItemAdapter);
//            }
//
//            bundle.listItemAdapter.updateListAdapter();
//        }
//
//        if (bundle.finalCurrentTestItem != null && bundle.finalCurrentTestItem.contains(Constants.TestItemCodes.SN0101)) {
//            // reserved for additional logic
//        }
//    }
//
//    private void recalcTestCountsFromAdapter(ItemAdapterTestItem adapter) {
//        if (adapter == null) {
//            return;
//        }
//
//        int calculatedOk = 0;
//        int calculatedNg = 0;
//        for (int i = 0; i < adapter.getCount(); i++) {
//            VoTestItem item = (VoTestItem) adapter.getItem(i);
//            if (Constants.ResultStatus.OK.equals(item.getTest_item_result())) {
//                calculatedOk++;
//            } else if (Constants.ResultStatus.NG.equals(item.getTest_item_result())) {
//                calculatedNg++;
//            }
//        }
//
//        testOkCnt = calculatedOk;
//        testNgCnt = calculatedNg;
//        tvTestOkCnt.setText(String.valueOf(calculatedOk));
//        tvTestNgCnt.setText(String.valueOf(calculatedNg));
//    }
//
//    private static class UiUpdateBundle {
//        final boolean dialogVisible;
//        final boolean dialogHidden;
//        final int dialogColor;
//        final String dialogMessage;
//        final String temperatureText;
//        final String compWattText;
//        final String heaterWattText;
//        final String pumpWattText;
//        final String logText;
//        final String updateItemCommand;
//        final String updateItemResult;
//        final String updateItemCheckValue;
//        final String updateItemInfo;
//        final String updateItemNameSuffix;
//        final boolean updateListAdapter;
//        final String finalReceiveCommandResponse;
//        final String finalCalculatedResultValue;
//        final String finalReadMessage;
//        final String temperatureValueCompDiff;
//        final String resultInfo;
//        final String decTemperatureHotValue;
//        final String decTemperatureColdValue;
//        final String finalCurrentTestItem;
//        final int testItemIdx;
//        final int testOkCnt;
//        final int testNgCnt;
//        final String receiveCommandResponseOK;
//        final boolean shouldUpdateCounts;
//        final ItemAdapterTestItem listItemAdapter;
//        final String currentProcessName;
//        final int receivedMessageCnt;
//
//        private UiUpdateBundle(Builder builder) {
//            this.dialogVisible = builder.dialogVisible;
//            this.dialogHidden = builder.dialogHidden;
//            this.dialogColor = builder.dialogColor;
//            this.dialogMessage = builder.dialogMessage;
//            this.temperatureText = builder.temperatureText;
//            this.compWattText = builder.compWattText;
//            this.heaterWattText = builder.heaterWattText;
//            this.pumpWattText = builder.pumpWattText;
//            this.logText = builder.logText;
//            this.updateItemCommand = builder.updateItemCommand;
//            this.updateItemResult = builder.updateItemResult;
//            this.updateItemCheckValue = builder.updateItemCheckValue;
//            this.updateItemInfo = builder.updateItemInfo;
//            this.updateItemNameSuffix = builder.updateItemNameSuffix;
//            this.updateListAdapter = builder.updateListAdapter;
//            this.finalReceiveCommandResponse = builder.finalReceiveCommandResponse;
//            this.finalCalculatedResultValue = builder.finalCalculatedResultValue;
//            this.finalReadMessage = builder.finalReadMessage;
//            this.temperatureValueCompDiff = builder.temperatureValueCompDiff;
//            this.resultInfo = builder.resultInfo;
//            this.decTemperatureHotValue = builder.decTemperatureHotValue;
//            this.decTemperatureColdValue = builder.decTemperatureColdValue;
//            this.finalCurrentTestItem = builder.finalCurrentTestItem;
//            this.testItemIdx = builder.testItemIdx;
//            this.testOkCnt = builder.testOkCnt;
//            this.testNgCnt = builder.testNgCnt;
//            this.receiveCommandResponseOK = builder.receiveCommandResponseOK;
//            this.shouldUpdateCounts = builder.shouldUpdateCounts;
//            this.listItemAdapter = builder.listItemAdapter;
//            this.currentProcessName = builder.currentProcessName;
//            this.receivedMessageCnt = builder.receivedMessageCnt;
//        }
//
//        static class Builder {
//            private boolean dialogVisible;
//            private boolean dialogHidden;
//            private int dialogColor;
//            private String dialogMessage;
//            private String temperatureText;
//            private String compWattText;
//            private String heaterWattText;
//            private String pumpWattText;
//            private String logText;
//            private String updateItemCommand = Constants.Common.EMPTY_STRING;
//            private String updateItemResult = Constants.Common.EMPTY_STRING;
//            private String updateItemCheckValue = Constants.Common.EMPTY_STRING;
//            private String updateItemInfo = Constants.Common.EMPTY_STRING;
//            private String updateItemNameSuffix = Constants.Common.EMPTY_STRING;
//            private boolean updateListAdapter;
//            private String finalReceiveCommandResponse;
//            private String finalCalculatedResultValue;
//            private String finalReadMessage;
//            private String temperatureValueCompDiff;
//            private String resultInfo;
//            private String decTemperatureHotValue;
//            private String decTemperatureColdValue;
//            private String finalCurrentTestItem;
//            private int testItemIdx;
//            private int testOkCnt;
//            private int testNgCnt;
//            private String receiveCommandResponseOK;
//            private boolean shouldUpdateCounts;
//            private ItemAdapterTestItem listItemAdapter;
//            private String currentProcessName;
//            private int receivedMessageCnt;
//
//            Builder setDialogVisible(boolean value) {
//                this.dialogVisible = value;
//                return this;
//            }
//
//            Builder setDialogHidden(boolean value) {
//                this.dialogHidden = value;
//                return this;
//            }
//
//            Builder setDialogColor(int value) {
//                this.dialogColor = value;
//                return this;
//            }
//
//            Builder setDialogMessage(String value) {
//                this.dialogMessage = value;
//                return this;
//            }
//
//            Builder setTemperatureText(String value) {
//                this.temperatureText = value;
//                return this;
//            }
//
//            Builder setCompWattText(String value) {
//                this.compWattText = value;
//                return this;
//            }
//
//            Builder setHeaterWattText(String value) {
//                this.heaterWattText = value;
//                return this;
//            }
//
//            Builder setPumpWattText(String value) {
//                this.pumpWattText = value;
//                return this;
//            }
//
//            Builder setLogText(String value) {
//                this.logText = value;
//                return this;
//            }
//
//            Builder setUpdateItemCommand(String value) {
//                this.updateItemCommand = value == null ? Constants.Common.EMPTY_STRING : value;
//                return this;
//            }
//
//            Builder setUpdateItemResult(String value) {
//                this.updateItemResult = value == null ? Constants.Common.EMPTY_STRING : value;
//                return this;
//            }
//
//            Builder setUpdateItemCheckValue(String value) {
//                this.updateItemCheckValue = value == null ? Constants.Common.EMPTY_STRING : value;
//                return this;
//            }
//
//            Builder setUpdateItemInfo(String value) {
//                this.updateItemInfo = value == null ? Constants.Common.EMPTY_STRING : value;
//                return this;
//            }
//
//            Builder setUpdateItemNameSuffix(String value) {
//                this.updateItemNameSuffix = value == null ? Constants.Common.EMPTY_STRING : value;
//                return this;
//            }
//
//            Builder setUpdateListAdapter(boolean value) {
//                this.updateListAdapter = value;
//                return this;
//            }
//
//            Builder setFinalReceiveCommandResponse(String value) {
//                this.finalReceiveCommandResponse = value;
//                return this;
//            }
//
//            Builder setFinalCalculatedResultValue(String value) {
//                this.finalCalculatedResultValue = value;
//                return this;
//            }
//
//            Builder setFinalReadMessage(String value) {
//                this.finalReadMessage = value;
//                return this;
//            }
//
//            Builder setTemperatureValueCompDiff(String value) {
//                this.temperatureValueCompDiff = value;
//                return this;
//            }
//
//            Builder setResultInfo(String value) {
//                this.resultInfo = value;
//                return this;
//            }
//
//            Builder setDecTemperatureHotValue(String value) {
//                this.decTemperatureHotValue = value;
//                return this;
//            }
//
//            Builder setDecTemperatureColdValue(String value) {
//                this.decTemperatureColdValue = value;
//                return this;
//            }
//
//            Builder setFinalCurrentTestItem(String value) {
//                this.finalCurrentTestItem = value;
//                return this;
//            }
//
//            Builder setTestItemIdx(int value) {
//                this.testItemIdx = value;
//                return this;
//            }
//
//            Builder setTestOkCnt(int value) {
//                this.testOkCnt = value;
//                return this;
//            }
//
//            Builder setTestNgCnt(int value) {
//                this.testNgCnt = value;
//                return this;
//            }
//
//            Builder setReceiveCommandResponseOK(String value) {
//                this.receiveCommandResponseOK = value;
//                return this;
//            }
//
//            Builder setShouldUpdateCounts(boolean value) {
//                this.shouldUpdateCounts = value;
//                return this;
//            }
//
//            Builder setListItemAdapter(ItemAdapterTestItem adapter) {
//                this.listItemAdapter = adapter;
//                return this;
//            }
//
//            Builder setCurrentProcessName(String value) {
//                this.currentProcessName = value;
//                return this;
//            }
//
//            Builder setReceivedMessageCnt(int value) {
//                this.receivedMessageCnt = value;
//                return this;
//            }
//
//            UiUpdateBundle build() {
//                return new UiUpdateBundle(this);
//            }
//        }
//    }
//}
