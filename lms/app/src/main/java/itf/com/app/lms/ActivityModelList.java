package itf.com.app.lms;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;

import itf.com.app.lms.item.ItemAdapterTestModel;
import itf.com.app.lms.kiosk.BaseKioskActivity;
import itf.com.app.lms.util.Constants;
import itf.com.app.lms.util.DialogManager;
import itf.com.app.lms.util.PermissionChecker;
import itf.com.app.lms.util.TestData;
import itf.com.app.lms.vo.VoTestModel;

public class ActivityModelList extends BaseKioskActivity implements View.OnClickListener {

    private static final String TAG = "ActivityModelList";

    // 권한 체크 유틸리티
    private PermissionChecker permissionChecker;

    // 다이얼로그 관리자
    private DialogManager dialogManager;
//    TextView btn_activity_quit = null;


    TextView txtText;
    private ItemAdapterTestModel listItemAdapter = null;
    private ListView itemList = null;
    private LinearLayout ll_popup_header = null;
    private TextView tv_popup_header_title = null;
    public static Context activityModelList = null;

    public static Intent modelIntent = null;
    public static String globalModelId = "";
    public static String globalModelName = "";
    public static String globalModelNation = "";
    public static String globalLastTestStartTimestamp = "";
    public static String globalProductSerialNo = "";

    public static SharedPreferences cookie_preferences;
    public static SharedPreferences.Editor cookie_info = null;

    private String test_model_id = "";
    private String test_model_name = "";
    private String test_model_nation = "";
    FloatingActionButton fab_history = null;
    FloatingActionButton fab_close = null;
    FloatingActionButton fab_refresh = null;
    FloatingActionButton fab_plc_memory = null;

    String urlStr = "";

    public static HttpURLConnection connection = null;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService modelListExecutor = Executors.newSingleThreadExecutor();
    private static int totalTimeCnt = 0;
    static String[][] arrTestItems = null;
    static public String modeType = Constants.InitialValues.MODE_TYPE;
    static public String serverIp = "";

    private List<Map<String, String>> lstData = null;
    String url = "";

    public String mode_type = Constants.InitialValues.MODE_TYPE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //타이틀바 없애기
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_test_model_list);

        activityModelList = this;

        //UI 객체생성
        txtText = findViewById(R.id.tv_param_from_parent);
        ll_popup_header = findViewById(R.id.ll_popup_header);
//        btn_activity_quit = findViewById(R.id.btn_activity_quit);
        tv_popup_header_title = findViewById(R.id.tv_popup_header_title);

        fab_history = findViewById(R.id.fab_history);
        fab_close = findViewById(R.id.fab_close);
        fab_refresh = findViewById(R.id.fab_refresh);
        fab_plc_memory = findViewById(R.id.fab_plc_memory);

        if (mode_type.equals(Constants.ResultStatus.MODE_TYPE_NORMAL)) {
            fab_history.setVisibility(View.GONE);
        }
        else {
            fab_history.setVisibility(View.VISIBLE);
        }

        fab_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // DialogManager를 사용하여 앱 종료 확인 다이얼로그 표시
                if (dialogManager != null) {
                    dialogManager.showAppExitConfirmDialog(
                            () -> {
                                // 종료 버튼 클릭 시 실행
                                restartApplication(getApplicationContext());
                            },
                            null // 취소 버튼 클릭 시 아무 작업도 하지 않음
                    );
                } else {
                    // DialogManager가 초기화되지 않은 경우 직접 종료
                    restartApplication(getApplicationContext());
                }
            }
        });

        fab_refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchModelListAfterLayout();
            }
        });

        fab_history.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent activityIntent = null;
                    activityIntent = new Intent(activityModelList, ActivityTestHistoryList.class);
                    activityModelList.startActivity(activityIntent);
                }
                catch (Exception e) {
                    Log.d(TAG, "▶ [ER].00920 " + e.toString());
                }
            }
        });

        fab_plc_memory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent activityIntent = new Intent(activityModelList, ActivityPlcMemoryViewer.class);
                    activityModelList.startActivity(activityIntent);
                }
                catch (Exception e) {
                    Log.e(TAG, "Failed to start PLC Memory Viewer: " + e.toString());
                    Toast.makeText(activityModelList, "PLC 메모리 뷰어 실행 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 불필요한 레이아웃 인플레이션 제거 (사용되지 않는 header 변수)
        // final View header = getLayoutInflater().inflate(R.layout.item_test_history_list_header, null, false) ;

        //데이터 가져오기
        Intent intent = getIntent();
        String data = intent.getStringExtra("data");
//        String strOKYn = intent.getStringExtra("ok_yn");
        txtText.setText(data);
//        }

        // SharedPreferences 읽기를 백그라운드 스레드로 이동하여 메인 스레드 블로킹 방지
        new Thread(() -> {
            SharedPreferences test = getSharedPreferences("test_cookie_info", MODE_PRIVATE);
            test_model_id = test.getString("test_model_id", "");
            test_model_name = test.getString("test_model_name", "");
            test_model_nation = test.getString("test_model_nation", "");

//        Log.i(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> test_model_id " + test_model_id);
//        Log.i(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> test_model_name " + test_model_name);
//        Log.i(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> test_model_nation " + test_model_nation);

            // Activity 전환을 UI 스레드로 포스팅하여 초기 렌더링 완료 후 실행
            runOnUiThread(() -> {
                if(!test_model_id.equals("")) {
                    Intent activityIntent = null;

            /*
            if(test_model_id.equals("00000001")) {
                System.out.println("> ActivityModelList >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 00000001");
                globalModelId = test_model_id;
                activityIntent = new Intent(activityModelList, ActivityModel_0001.class);
            }
            else if(test_model_id.equals("00000002")) {
                System.out.println("> ActivityModelList >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 00000002");
                globalModelId = test_model_id;
                activityIntent = new Intent(activityModelList, ActivityModel_0002.class);
            }
            else if(test_model_id.equals("00000003")) {
                System.out.println("> ActivityModelList >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 00000003");
                globalModelId = test_model_id;
                activityIntent = new Intent(activityModelList, ActivityModel_0003.class);
            }
            */

                    globalModelId = test_model_id;
                    activityIntent = new Intent(activityModelList, ActivityModelTestProcess.class);

                    if(activityIntent != null) {
                        activityIntent.putExtra("test_model_id", test_model_id);
                        activityIntent.putExtra("test_model_name", test_model_name);
                        activityIntent.putExtra("test_model_nation", test_model_nation);
                        activityModelList.startActivity(activityIntent);
                    }
                }
            });
        }).start();

        // 다이얼로그 관리자 초기화
        dialogManager = new DialogManager(this);

        // 권한 체크 유틸리티 초기화
        permissionChecker = new PermissionChecker(this);

        // Activity가 완전히 초기화된 후 권한 체크 (500ms 지연)
        // 권한이 이미 있으면 다이얼로그를 표시하지 않음
        View root = findViewById(android.R.id.content);
        if (root != null) {
            root.postDelayed(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    // 권한이 이미 있으면 체크만 하고 다이얼로그 표시하지 않음
                    if (permissionChecker.hasAllRequiredPermissions()) {
                        Log.i(TAG, "▶ [PS] All permissions already granted, skipping dialog");
                        // 권한 체크 완료 상태로 설정하여 재체크 방지
                        permissionChecker.setPermissionCheckCompleted();
                    } else {
                        // 권한이 없을 때만 권한 요청 다이얼로그 표시
                        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 권한체크 진입 1");
                        permissionChecker.checkAndRequestPermissions();
                    }
                }
            }, 500);
        } else {
            mainHandler.postDelayed(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    // 권한이 이미 있으면 체크만 하고 다이얼로그 표시하지 않음
                    if (permissionChecker.hasAllRequiredPermissions()) {
                        Log.i(TAG, "▶ [PS] All permissions already granted, skipping dialog");
                        // 권한 체크 완료 상태로 설정하여 재체크 방지
                        permissionChecker.setPermissionCheckCompleted();
                    } else {
                        // 권한이 없을 때만 권한 요청 다이얼로그 표시
                        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 권한체크 진입 2");
                        permissionChecker.checkAndRequestPermissions();
                    }
                }
            }, 500);
        }

        fetchModelListAfterLayout();
    }

    private void restartApplication(Context mContext) {
        new Thread(() -> {
            try {
                moveTaskToBack(true);
                finishAndRemoveTask();
                PackageManager packageManager = mContext.getPackageManager();
                Intent intent = packageManager.getLaunchIntentForPackage(mContext.getPackageName());
                assert intent != null;
                ComponentName componentName = intent.getComponent();
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(0);
            }
            catch (Exception e) {
                Log.d(TAG, "▶ [ER].00118 " + e.toString());
            }
        }).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            // 설정 화면에서 돌아온 경우 권한 재체크
            // 권한이 이미 있으면 다이얼로그를 표시하지 않음
            if (permissionChecker != null) {
                // 권한이 이미 있으면 재체크하지 않음
                if (permissionChecker.hasAllRequiredPermissions()) {
                    Log.i(TAG, "▶ [PS] All permissions already granted in onResume, skipping dialog");
                    // 권한 체크 완료 상태로 설정하여 재체크 방지
                    permissionChecker.setPermissionCheckCompleted();
                    return;
                }

                // 권한이 없고, 체크가 완료되지 않았을 때만 재체크
                if (!permissionChecker.isPermissionCheckCompleted()) {
                    permissionChecker.resetPermissionCheck();
                    View root = findViewById(android.R.id.content);
                    if (root != null) {
                        root.postDelayed(() -> {
                            if (!isFinishing() && !isDestroyed()) {
                                // 권한이 없을 때만 권한 요청 다이얼로그 표시
                                System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 권한체크 진입 3");
                                permissionChecker.checkAndRequestPermissions();
                            }
                        }, 300);
                    } else {
                        mainHandler.postDelayed(() -> {
                            if (!isFinishing() && !isDestroyed()) {
                                // 권한이 없을 때만 권한 요청 다이얼로그 표시
                                System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 권한체크 진입 4");
                                permissionChecker.checkAndRequestPermissions();
                            }
                        }, 300);
                    }
                }
            }
//            RequestThreadAsync thread = new RequestThreadAsync();
//            thread.execute();
//            // listReset();
        }
        catch (Exception e) {
            Log.d(TAG, "▶ [ER].00927 " + e.toString());
        }
    }

    private void listReset(List<Map<String, String>> lstData) {
        listItemAdapter = new ItemAdapterTestModel();
        itemList = findViewById(R.id.lv_process_result_list);

        List<Map<String, String>> lstModelList = new ArrayList<>();
        Map<String, String> mapModelInfo = null;

        for(int i=0; i<lstData.size(); i++) {
            mapModelInfo = new HashMap<String, String>();
            mapModelInfo.put("clm_test_model_no", String.format("%03d", i+1));
            mapModelInfo.put("clm_model_version", lstData.get(i).get("clm_model_version"));
            mapModelInfo.put("clm_test_version", lstData.get(i).get("clm_test_version"));
            mapModelInfo.put("clm_test_model_id", lstData.get(i).get("clm_model_id"));
            mapModelInfo.put("clm_brand_name", lstData.get(i).get("clm_client_name"));
            mapModelInfo.put("clm_test_model_name", lstData.get(i).get("clm_model_name"));
            mapModelInfo.put("clm_test_model_nationality_id", lstData.get(i).get("clm_test_step"));
            mapModelInfo.put("clm_test_model_nationality_name", lstData.get(i).get("clm_test_step"));
            lstModelList.add(mapModelInfo);
        }

        for(int i=0; i<lstModelList.size(); i++) {
            mapModelInfo = lstModelList.get(i);
            // mapModelInfo.put("clm_test_history_no", String.valueOf(i+1));
            listItemAdapter.addItem(new VoTestModel(mapModelInfo));
        }


        runOnUiThread(new Runnable() {
            public void run() {
                itemList.setAdapter(listItemAdapter);
            }
        });
    }

    public void onClick(View v) {
        if (v.getId() == R.id.btn_activity_quit) {
            ActivityModelList.this.finish();
        }
    }

    //확인 버튼 클릭
    public void mOnClose(View v){
        //데이터 전달하기
        Intent intent = new Intent();
        intent.putExtra("result", "Close Popup");
        setResult(RESULT_OK, intent);

        //액티비티(팝업) 닫기
        finish();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // ⚠️ 키오스크 모드: 터치 이벤트 발생 시 즉시 시스템 UI 숨기기
        // 상단 스와이프로 시스템 UI가 나타나는 것을 방지
        if (event.getAction() == MotionEvent.ACTION_DOWN ||
                event.getAction() == MotionEvent.ACTION_MOVE) {
            hideSystemUI(); // BaseKioskActivity의 메소드 호출
        }

        // ⚠️ 중요: super.onTouchEvent()를 호출하여 BaseKioskActivity의 로직도 실행
        boolean superResult = super.onTouchEvent(event);

        //바깥레이어 클릭시 안닫히게
        return event.getAction() != MotionEvent.ACTION_OUTSIDE && superResult;
    }

    private void fetchModelListAfterLayout() {
        View root = findViewById(android.R.id.content);
        if (root != null) {
            root.post(this::startModelListFetch);
        } else {
            mainHandler.post(this::startModelListFetch);
        }
    }

    private void startModelListFetch() {
        modelListExecutor.execute(this::performModelListRequest);
    }

    private void performModelListRequest() {
        HttpURLConnection localConnection = null;
        try {
            serverIp = "172.16.1.250:8080";
            serverIp = "172.16.1.249:8080";
            serverIp = "itfactoryddns.iptime.org:10004";
            if (mode_type.equals(Constants.ResultStatus.MODE_TYPE_TEST)) {
                serverIp = Constants.ServerConfig.SERVER_IP_PORT_ITF;
            } else {
                serverIp = Constants.ServerConfig.SERVER_IP_PORT;
            }

            urlStr = "http://" + serverIp + "/OVIO/ModelInfoList.jsp";
            Log.i(TAG, "▶ [PS] urlStr " + urlStr);
            url = urlStr;
            URL obj = new URL(url);
            localConnection = (HttpURLConnection) obj.openConnection();
            connection = localConnection;

            localConnection.setReadTimeout(3000);
            localConnection.setConnectTimeout(5000);
            localConnection.setRequestMethod("POST");
            localConnection.setDoInput(true);
            localConnection.setDoOutput(true);
            localConnection.setRequestProperty("Content-Type", "application/json");
            localConnection.setRequestProperty("Cache-Control", "no-cache");

            int retCode = localConnection.getResponseCode();
            Log.i(TAG, "▶ [PS] retCode data " + retCode);
            if (retCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(localConnection.getInputStream()))) {
                    StringBuilder payloadBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!TextUtils.isEmpty(line)) {
                            payloadBuilder.append(line);
                        }
                    }
                    runOnUiThread(() -> {
                        final String payload = payloadBuilder.toString();
                        if (!TextUtils.isEmpty(payload)) {
                            mainHandler.post(() -> {
                                Log.i(TAG, "▶ [PS] process data " + payload);
                                jsonParsing("model_list", payload);
                            });
                        }
                    });
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "▶ [ER].00919 " + e);
        } finally {
            if (localConnection != null) {
                localConnection.disconnect();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // PermissionChecker에 결과 전달
        if (permissionChecker != null) {
            permissionChecker.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // DialogManager 정리
        if (dialogManager != null) {
            dialogManager.cleanup();  // ⚠️ CRITICAL FIX: Use cleanup() instead of dismissAllDialogs()
            dialogManager = null;
        }

        modelListExecutor.shutdownNow();
    }

    private void jsonParsing(String data_type, String json) {
        new Thread(() -> {
            try {
                JSONObject jsonObject = new JSONObject(json);
                JSONArray testItemArray = jsonObject.getJSONArray("model_list");

                lstData = new ArrayList<Map<String, String>>();
                Map<String, String> mapData = null;

//                Log.i(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>> data_type " + data_type);
//                Log.i(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>> json " + json);
//                Log.i(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>> testItemArray.length() " + testItemArray.length());

                if (data_type.equals("model_list")) {
                    for (int i = 0; i < testItemArray.length(); i++) {
                        JSONObject testItemObject = testItemArray.getJSONObject(i);
                        mapData = new HashMap<String, String>();
                        mapData.put("clm_test_seq", String.valueOf(i));
                        mapData.put("clm_client_name", testItemObject.getString("clm_client_name"));
                        mapData.put("clm_client_id", testItemObject.getString("clm_client_id"));
                        mapData.put("clm_model_seq", testItemObject.getString("clm_model_seq"));
                        mapData.put("clm_test_step", testItemObject.getString("clm_test_step"));
                        mapData.put("clm_company_key", testItemObject.getString("clm_company_key"));
                        mapData.put("clm_model_id", testItemObject.getString("clm_model_id"));
                        mapData.put("clm_model_name", testItemObject.getString("clm_model_name"));
                        mapData.put("clm_model_version", testItemObject.getString("clm_model_version"));
                        mapData.put("clm_comment", "");
                        lstData.add(mapData);

                        // 모델 정보를 데이터베이스에 저장
                        try {
                            Map<String, String> modelInfoData = new HashMap<String, String>();
                            modelInfoData.put("clm_model_seq", testItemObject.getString("clm_model_seq"));
                            modelInfoData.put("clm_client_name", testItemObject.getString("clm_client_name"));
                            modelInfoData.put("clm_client_id", testItemObject.getString("clm_client_id"));
                            modelInfoData.put("clm_test_step", testItemObject.getString("clm_test_step"));
                            modelInfoData.put("clm_company_key", testItemObject.getString("clm_company_key"));
                            modelInfoData.put("clm_model_id", testItemObject.getString("clm_model_id"));
                            modelInfoData.put("clm_model_name", testItemObject.getString("clm_model_name"));
                            modelInfoData.put("clm_model_version", testItemObject.getString("clm_model_version"));
                            modelInfoData.put("clm_comment", "");

                            boolean success = TestData.insertModelInfo(activityModelList, modelInfoData);
                            if (success) {
                                Log.i(TAG, "▶ [PS] Model info saved to database: " + testItemObject.getString("clm_model_id"));
                            } else {
                                Log.e(TAG, "▶ [ER] Failed to save model info to database: " + testItemObject.getString("clm_model_id"));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "▶ [ER] Error saving model info to database: " + e.getMessage());
                        }
                    }

                    Log.i(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>> mapData.size() " + (mapData != null ? mapData.size() : 0));

                    totalTimeCnt = 0;
                    arrTestItems = new String[lstData.size()][mapData != null ? mapData.size() : 0];

                    runOnUiThread(new Runnable() {
                        public void run() {
                            // > TestData.deleteTestSpecData(activityModelList);
                            listReset(lstData);
                        }
                    });
                }
            }
            catch (JSONException e) {
                Log.d(TAG, "▶ [ER].00015 : " + e);
            }
        }).start();
    }
}