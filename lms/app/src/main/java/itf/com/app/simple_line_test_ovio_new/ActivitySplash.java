package itf.com.app.simple_line_test_ovio_new;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import itf.com.app.simple_line_test_ovio_new.kiosk.BaseKioskActivity;
import itf.com.app.simple_line_test_ovio_new.kiosk.KioskModeApplication;
import itf.com.app.simple_line_test_ovio_new.util.Constants;
import itf.com.app.simple_line_test_ovio_new.util.TestData;
import itf.com.app.simple_line_test_ovio_new.util.WebServer;

public class ActivitySplash extends BaseKioskActivity {
    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DISPLAY_LENGTH = 2000; // 최소 2초 표시
    public String mode_type = Constants.InitialValues.MODE_TYPE;
    private static String serverIp = "";
    private Handler periodicCheckHandler = null;
    private Runnable periodicCheckRunnable = null;
    private AlertDialog clientIdDialog = null;
    private WebView webView = null; // WebView 구현용 (주석 처리됨)
    
    // 네이티브 다이얼로그 UI 요소
    private EditText etBusinessNumber = null;
    private TextView tvClientId = null;
    private TextView tvErrorMessage = null;
    private Button btnRegister = null;
    private ProgressBar progressBar = null;
    private LinearLayout clientIdLayout = null;
    private Handler lookupHandler = null;
    private Runnable lookupRunnable = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 전체 화면 설정
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.activity_splash);
        
        // 서버 IP 초기화
        if (mode_type.equals(Constants.ResultStatus.MODE_TYPE_TEST)) {
            serverIp = Constants.ServerConfig.SERVER_IP_PORT_ITF;
        } else {
            serverIp = Constants.ServerConfig.SERVER_IP_PORT;
        }

        if(Constants.InitialValues.ACCOUNT_TYPE.equals(Constants.ResultStatus.ACCOUNT_TYPE_CLOUD)) {
            // SettingInfo 다운로드 및 저장
            new LoadSettingInfoTask().execute();
        }
        else if(Constants.InitialValues.ACCOUNT_TYPE.equals(Constants.ResultStatus.ACCOUNT_TYPE_LOCAL)) {
            // 로컬 계정도 기본 설정 로우 생성 + 공통 설정 로드
            TestData.loadCommonSettingsOnce(this);
            Intent intent = new Intent(ActivitySplash.this, ActivityModelList.class);
            startActivity(intent);
            finish();
        }
    }

    private class LoadSettingInfoTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                // 서버 URL 구성
                String urlStr = Constants.URLs.HTTP_PROTOCOL + serverIp + Constants.URLs.ENDPOINT_SETTING_INFO_LIST;
                
                Log.i(TAG, "SettingInfoList.jsp 호출: " + urlStr);
                
                URL url = new URL(urlStr);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setReadTimeout(Constants.Timeouts.HTTP_READ_TIMEOUT_MS);
                connection.setConnectTimeout(Constants.Timeouts.HTTP_CONNECT_TIMEOUT_MS);
                connection.setRequestMethod(Constants.HTTP.METHOD_GET);
                connection.setDoInput(true);
                
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.trim().isEmpty()) {
                            sb.append(line);
                        }
                    }
                    
                    String jsonResponse = sb.toString();
                    Log.i(TAG, "SettingInfo 응답: " + jsonResponse);
                    
                    if (jsonResponse != null && !jsonResponse.trim().isEmpty()) {
                        // JSON 파싱 및 저장 (JSONObject에서 "setting_info" 키로 JSONArray 추출)
                        JSONObject jsonObject = new JSONObject(jsonResponse);
                        JSONArray jsonArray = null;
                        if (jsonObject.has(Constants.JsonKeys.SETTING_INFO)) {
                            jsonArray = jsonObject.getJSONArray(Constants.JsonKeys.SETTING_INFO);
                        } else {
                            // "setting_info" 키가 없으면 전체를 배열로 간주
                            jsonArray = new JSONArray(jsonResponse);
                        }
                        return saveSettingInfoToDatabase(jsonArray);
                    }
                    }
                } else {
                    Log.e(TAG, "HTTP 응답 코드: " + responseCode);
                }
                
                connection.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "SettingInfo 다운로드 실패", e);
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            // 최소 표시 시간 보장을 위해 지연
            new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // 클라이언트 ID 등록 확인
                    if (!checkClientIdRegistration()) {
                        // 클라이언트 ID가 등록되지 않았으면 등록 다이얼로그 표시
                        showClientIdRegistrationDialog();
                    } else {
                        // 메인 액티비티로 이동
                        Intent intent = new Intent(ActivitySplash.this, ActivityModelList.class);
                        startActivity(intent);
                        finish();
                    }
                }
            }, SPLASH_DISPLAY_LENGTH);
        }
    }

    public static String getCurrentDatetime(String dateformat) {
        SimpleDateFormat dateFormmater = new SimpleDateFormat(dateformat);
        return dateFormmater.format(new Date());
    }

    private boolean saveSettingInfoToDatabase(JSONArray jsonArray) {
        try {
            // 기존 데이터 삭제 (선택사항 - 필요시 주석 해제)
            // TestData.deleteSettingInfo(SplashActivity.this);
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                
                // JSON에서 데이터 추출 (서버 응답 구조에 맞게 수정 필요)
                java.util.Map<String, String> settingData = new java.util.HashMap<>();
                settingData.put("clm_setting_seq", getJsonString(jsonObject, "clm_setting_seq"));
                settingData.put("clm_setting_name_kr", getJsonString(jsonObject, "clm_setting_name_kr"));
                settingData.put("clm_setting_name_en", getJsonString(jsonObject, "clm_setting_name_en"));
                settingData.put("clm_setting_id", getJsonString(jsonObject, "clm_setting_id"));
                settingData.put("clm_setting_value", getJsonString(jsonObject, "clm_setting_value"));
                settingData.put("clm_comment", getJsonString(jsonObject, "clm_comment"));
                settingData.put("clm_test_timestamp", getCurrentDatetime(Constants.DateTimeFormats.TIMESTAMP_FORMAT));
                
                // DB에 저장
                TestData.insertSettingInfo(ActivitySplash.this, settingData);
            }

            // 서버에서 내려오지 않는 로컬 기본 설정 로우 보강 + 공통 설정 값 로드
            TestData.loadCommonSettingsOnce(this);
            
            Log.i(TAG, "SettingInfo 저장 완료: " + jsonArray.length() + "건");
            return true;
        } catch (JSONException e) {
            Log.e(TAG, "JSON 파싱 오류", e);
            return false;
        }
    }

    private String getJsonString(JSONObject jsonObject, String key) {
        try {
            if (jsonObject.has(key) && !jsonObject.isNull(key)) {
                return jsonObject.getString(key);
            }
        } catch (JSONException e) {
            Log.w(TAG, "JSON 키 읽기 실패: " + key, e);
        }
        return "";
    }

    /**
     * clm_setting_seq='000000'이 등록되어 있는지 확인
     */
    private boolean checkClientIdRegistration() {
        try {
            List<Map<String, String>> settingList = TestData.selectSettingInfoBySeq(this, "000000");
            if (settingList != null && !settingList.isEmpty()) {
                Log.i(TAG, "클라이언트 ID가 이미 등록되어 있습니다.");
                return true;
            }
            Log.i(TAG, "클라이언트 ID가 등록되지 않았습니다.");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "클라이언트 ID 확인 실패", e);
            return false;
        }
    }

    private final int businessSerialNoMaxLength = 12; // 999-99-99999 = 12자

    /**
     * 클라이언트 ID 등록 다이얼로그 표시 (네이티브 구현)
     */
    private void showClientIdRegistrationDialog() {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }

            // 다이얼로그 레이아웃 생성
            LinearLayout layoutDialog = new LinearLayout(this);
            layoutDialog.setOrientation(LinearLayout.VERTICAL);
            layoutDialog.setPadding(60, 40, 60, 40);
            layoutDialog.setMinimumWidth(800);

            // 다이얼로그 레이아웃 생성
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(60, 40, 60, 40);
            layout.setMinimumWidth(800);

            // 사업자 번호 입력 레이블
            TextView labelBusinessNumber = new TextView(this);
            labelBusinessNumber.setText("사업자 번호");
            labelBusinessNumber.setTextSize(18);
            labelBusinessNumber.setPadding(0, 0, 0, 10);
            layout.addView(labelBusinessNumber);



            // 사업자 번호 입력 필드
            etBusinessNumber = new EditText(this);
            etBusinessNumber.setHint("예: 123-45-67890");
            etBusinessNumber.setTextSize(18);
            etBusinessNumber.setPadding(20, 15, 20, 15);
            etBusinessNumber.setInputType(InputType.TYPE_CLASS_TEXT);
            etBusinessNumber.setFilters(new InputFilter[] {new InputFilter.LengthFilter(businessSerialNoMaxLength)}); // 999-99-99999 = 12자
            layout.addView(etBusinessNumber);

            // 고객 ID 표시 영역
            clientIdLayout = new LinearLayout(this);
            clientIdLayout.setOrientation(LinearLayout.HORIZONTAL);
            clientIdLayout.setPadding(0, 20, 0, 10);
            clientIdLayout.setVisibility(View.GONE);

            TextView labelClientId = new TextView(this);
            labelClientId.setText("고객 ID: ");
            labelClientId.setTextSize(18);
            labelClientId.setPadding(0, 0, 10, 0);

            tvClientId = new TextView(this);
            tvClientId.setTextSize(18);
            tvClientId.setTextColor(getResources().getColor(R.color.blue_for_ovio));

            clientIdLayout.addView(labelClientId);
            clientIdLayout.addView(tvClientId);
            layout.addView(clientIdLayout);

            // 에러 메시지
            tvErrorMessage = new TextView(this);
            tvErrorMessage.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            tvErrorMessage.setTextSize(14);
            tvErrorMessage.setPadding(0, 10, 0, 10);
            tvErrorMessage.setVisibility(View.GONE);
            layout.addView(tvErrorMessage);

            // 진행 표시
            progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleSmall);
            progressBar.setVisibility(View.GONE);
            layout.addView(progressBar);

            // 등록 버튼
            btnRegister = new Button(this);
            btnRegister.setText("등록");
            btnRegister.setTextSize(18);
            btnRegister.setPadding(0, 20, 0, 20);
            btnRegister.setEnabled(false);
            layout.addView(btnRegister);

            // 사업자 번호 포맷팅 및 실시간 조회
            TextWatcher businessNumberWatcher = new TextWatcher() {
                private boolean isFormatting = false; // 무한 루프 방지 플래그
                private int previousLength = 0;

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    previousLength = s.length();
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    // 무한 루프 방지: 포맷팅 중이면 무시
                    if (isFormatting) {
                        return;
                    }

                    String input = s.toString();
                    // 숫자만 추출
                    String numbersOnly = input.replaceAll("[^0-9]", "");
                    
                    // 숫자가 없으면 포맷팅하지 않음
                    if (numbersOnly.isEmpty()) {
                        // 디바운싱된 조회 취소
                        if (lookupHandler != null && lookupRunnable != null) {
                            lookupHandler.removeCallbacks(lookupRunnable);
                        }
                        // 고객 ID 숨기기
                        if (clientIdLayout != null) {
                            clientIdLayout.setVisibility(View.GONE);
                        }
                        if (btnRegister != null) {
                            btnRegister.setEnabled(false);
                        }
                        if (tvErrorMessage != null) {
                            tvErrorMessage.setVisibility(View.GONE);
                        }
                        return;
                    }

                    // 실시간 하이픈 자동 추가
                    StringBuilder formatted = new StringBuilder();
                    int currentCursorPos = etBusinessNumber.getSelectionStart();
                    
                    for (int i = 0; i < numbersOnly.length(); i++) {
                        if (i == 3) {
                            formatted.append('-');
                        } else if (i == 5) {
                            formatted.append('-');
                        }
                        formatted.append(numbersOnly.charAt(i));
                    }
                    
                    String formattedStr = formatted.toString();
                    
                    // 포맷팅이 변경되었으면 적용
                    if (!input.equals(formattedStr)) {
                        isFormatting = true; // 플래그 설정
                        
                        // TextWatcher를 일시적으로 제거하여 무한 루프 방지
                        etBusinessNumber.removeTextChangedListener(this);
                        
                        // 커서 위치 계산
                        int newCursorPos = calculateNewCursorPosition(
                            input, formattedStr, currentCursorPos, previousLength, numbersOnly.length());
                        
                        s.replace(0, s.length(), formattedStr);
                        
                        // 다음 이벤트 루프에서 커서 위치 설정 및 TextWatcher 재추가
                        String finalFormattedStr = formattedStr;
                        etBusinessNumber.post(() -> {
                            try {
                                etBusinessNumber.setSelection(Math.min(newCursorPos, finalFormattedStr.length()));
                            } catch (Exception e) {
                                Log.e(TAG, "커서 위치 설정 오류", e);
                            }
                            etBusinessNumber.addTextChangedListener(this);
                            isFormatting = false; // 플래그 해제
                        });
                        
                        // 포맷팅된 문자열로 조회 로직 실행
                        formattedStr = formatted.toString();
                    }

                    // 디바운싱된 조회 (10자리 숫자 입력 시)
                    if (numbersOnly.length() >= 10) {
                        if (lookupHandler != null && lookupRunnable != null) {
                            lookupHandler.removeCallbacks(lookupRunnable);
                        }
                        lookupHandler = new Handler(Looper.getMainLooper());
                        String finalFormattedStr1 = formattedStr;
                        lookupRunnable = () -> {
                            lookupClientId(finalFormattedStr1);
                        };
                        lookupHandler.postDelayed(lookupRunnable, 500);
                    } else {
                        // 입력이 부족하면 고객 ID 숨기기
                        if (lookupHandler != null && lookupRunnable != null) {
                            lookupHandler.removeCallbacks(lookupRunnable);
                        }
                        if (clientIdLayout != null) {
                            clientIdLayout.setVisibility(View.GONE);
                        }
                        if (btnRegister != null) {
                            btnRegister.setEnabled(false);
                        }
                        if (tvErrorMessage != null) {
                            tvErrorMessage.setVisibility(View.GONE);
                        }
                    }
                }
                
                /**
                 * 커서 위치 계산 (하이픈 추가/제거 시 정확한 위치 계산)
                 */
                private int calculateNewCursorPosition(String oldText, String newText, 
                                                      int oldCursorPos, int oldLength, int numbersCount) {
                    if (oldText == null || newText == null) {
                        return 0;
                    }
                    
                    // 커서 위치 이전의 숫자 개수 계산
                    int numbersBeforeCursor = 0;
                    for (int i = 0; i < Math.min(oldCursorPos, oldText.length()); i++) {
                        if (Character.isDigit(oldText.charAt(i))) {
                            numbersBeforeCursor++;
                        }
                    }
                    
                    // 새 텍스트에서 해당 숫자 개수만큼의 위치 찾기
                    int newCursorPos = 0;
                    int numbersFound = 0;
                    for (int i = 0; i < newText.length(); i++) {
                        if (Character.isDigit(newText.charAt(i))) {
                            numbersFound++;
                            if (numbersFound > numbersBeforeCursor) {
                                newCursorPos = i;
                                break;
                            }
                        }
                        newCursorPos = i + 1;
                    }
                    
                    // 하이픈이 추가되는 위치(3번째, 5번째 숫자 뒤)에 커서가 있으면 하이픈 뒤로 이동
                    if (newCursorPos < newText.length() && newText.charAt(newCursorPos) == '-') {
                        newCursorPos++;
                    }
                    
                    return Math.min(Math.max(0, newCursorPos), newText.length());
                }
            };
            
            etBusinessNumber.addTextChangedListener(businessNumberWatcher);

            // 등록 버튼 클릭
            btnRegister.setOnClickListener(v -> {
                String businessNumber = etBusinessNumber.getText().toString().trim();
                if (businessNumber.isEmpty()) {
                    showError("사업자 번호를 입력해주세요.");
                    return;
                }

                String clientId = tvClientId.getText().toString().trim();
                if (clientId.isEmpty()) {
                    showError("고객 ID를 먼저 조회해주세요.");
                    return;
                }

                // 등록 처리
                registerClientId(clientId);
            });

            // 다이얼로그 생성
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("고객 ID 등록 필요");
            builder.setView(layout);
            builder.setCancelable(false);

            clientIdDialog = builder.create();
            clientIdDialog.setOnShowListener(dialogInterface -> {
                // 키보드 표시 설정
                Window dialogWindow = clientIdDialog.getWindow();
                if (dialogWindow != null) {
                    dialogWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN |
                                                  WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                    dialogWindow.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
                    dialogWindow.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
                    
                    // 다이얼로그 위치 조정
                    WindowManager.LayoutParams params = dialogWindow.getAttributes();
                    params.gravity = Gravity.CENTER;
                    params.y = -100;
                    dialogWindow.setAttributes(params);
                }

                // 입력 필드에 포커스
                etBusinessNumber.postDelayed(() -> {
                    etBusinessNumber.requestFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.showSoftInput(etBusinessNumber, InputMethodManager.SHOW_IMPLICIT);
                    }
                }, 300);
            });

            // 주기적으로 DB 확인
            startPeriodicCheck(clientIdDialog);

            clientIdDialog.show();
        });
    }

    /**
     * 사업자 번호 포맷팅 (999-99-99999 형식)
     */
    private String formatBusinessNumber(String input) {
        if (input == null) {
            return "";
        }
        // 숫자만 추출
        String numbers = input.replaceAll("[^0-9]", "");
        if (numbers.length() <= 3) {
            return numbers;
        } else if (numbers.length() <= 5) {
            return numbers.substring(0, 3) + "-" + numbers.substring(3);
        } else {
            return numbers.substring(0, 3) + "-" + numbers.substring(3, 5) + "-" + numbers.substring(5, Math.min(10, numbers.length()));
        }
    }

    /**
     * 서버에서 클라이언트 ID 조회
     */
    private void lookupClientId(String businessNumber) {
        if (businessNumber == null || businessNumber.trim().isEmpty()) {
            return;
        }

        // 포맷팅된 사업자 번호 사용 (하이픈 포함)
        new LookupClientIdTask().execute(businessNumber);
    }

    /**
     * 클라이언트 ID 조회 AsyncTask
     */
    private class LookupClientIdTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }
            if (tvErrorMessage != null) {
                tvErrorMessage.setVisibility(View.GONE);
            }
        }

        @Override
        protected String doInBackground(String... params) {
            String businessNumber = params[0];
            try {
                // 서버 URL 구성
                String urlStr = Constants.URLs.HTTP_PROTOCOL + serverIp + 
                               Constants.URLs.ENDPOINT_APPLICATION_CLIENT_INFO_LIST +
                               "?clm_business_serial_no=" + java.net.URLEncoder.encode(businessNumber, "UTF-8");
                
                Log.i(TAG, "ApplicationClientInfoList.jsp 호출: " + urlStr);
                
                URL url = new URL(urlStr);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setReadTimeout(Constants.Timeouts.HTTP_READ_TIMEOUT_MS);
                connection.setConnectTimeout(Constants.Timeouts.HTTP_CONNECT_TIMEOUT_MS);
                connection.setRequestMethod(Constants.HTTP.METHOD_GET);
                connection.setDoInput(true);
                
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.trim().isEmpty()) {
                            sb.append(line);
                        }
                    }
                    
                    String jsonResponse = sb.toString();
                    Log.i(TAG, "ApplicationClientInfoList 응답: " + jsonResponse);
                    
                    // JSON 파싱
                    JSONObject jsonObject = new JSONObject(jsonResponse);
                    JSONArray jsonArray = null;
                    
                    // "application_client_info" 키 확인
                    if (jsonObject.has(Constants.JsonKeys.APPLICATION_CLIENT_INFO)) {
                        jsonArray = jsonObject.getJSONArray(Constants.JsonKeys.APPLICATION_CLIENT_INFO);
                    }
//                    else if (jsonObject.has(Constants.JsonKeys.CLIENT_INFO)) {
//                        jsonArray = jsonObject.getJSONArray(Constants.JsonKeys.CLIENT_INFO);
//                    }
                    
                    if (jsonArray != null && jsonArray.length() > 0) {
                        JSONObject clientInfo = jsonArray.getJSONObject(0);
                        if (clientInfo.has(Constants.JsonKeys.CLM_CLIENT_ID)) {
                            String clientId = clientInfo.getString(Constants.JsonKeys.CLM_CLIENT_ID);
                            Log.i(TAG, "고객 ID 추출 성공: " + clientId);
                            return clientId;
                        }
                    }
                    }
                } else {
                    Log.e(TAG, "HTTP 응답 코드: " + responseCode);
                }
                
                connection.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "클라이언트 ID 조회 실패", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String clientId) {
            super.onPostExecute(clientId);
            
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            
            if (clientId != null && !clientId.isEmpty()) {
                // 고객 ID 표시
                if (tvClientId != null) {
                    tvClientId.setText(clientId);
                }
                if (clientIdLayout != null) {
                    clientIdLayout.setVisibility(View.VISIBLE);
                }
                
                // 등록 버튼 활성화
                if (btnRegister != null) {
                    btnRegister.setEnabled(true);
                }
                
                // 에러 메시지 숨기기
                if (tvErrorMessage != null) {
                    tvErrorMessage.setVisibility(View.GONE);
                }
                
                // 키보드 숨기기
                hideKeyboard();
            } else {
                // 고객 ID를 찾을 수 없음
                showError("해당 사업자 번호로 고객 ID를 찾을 수 없습니다.");
                if (btnRegister != null) {
                    btnRegister.setEnabled(false);
                }
            }
        }
    }

    /**
     * 클라이언트 ID 등록
     */
    private void registerClientId(String clientId) {
        new RegisterClientIdTask().execute(clientId);
    }

    /**
     * 클라이언트 ID 등록 AsyncTask
     */
    private class RegisterClientIdTask extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... params) {
            String clientId = params[0];
            try {
                Map<String, String> settingData = new java.util.HashMap<>();
                settingData.put("clm_setting_seq", "000000");
                settingData.put("clm_setting_id", "CLIENT_ID");
                settingData.put("clm_setting_value", clientId);
                settingData.put("clm_comment", "고객 ID");
                settingData.put("clm_test_timestamp", getCurrentDatetime(Constants.DateTimeFormats.TIMESTAMP_FORMAT));
                
                boolean success = TestData.insertSettingInfo(ActivitySplash.this, settingData);
                if (success) {
                    Log.i(TAG, "클라이언트 ID 등록 성공: " + clientId);
                }
                return success;
            } catch (Exception e) {
                Log.e(TAG, "클라이언트 ID 등록 실패", e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            if (success) {
                // 등록 성공 시 다이얼로그 닫고 메인 액티비티로 이동
                if (clientIdDialog != null && clientIdDialog.isShowing()) {
                    clientIdDialog.dismiss();
                }
                stopPeriodicCheck();
                Intent intent = new Intent(ActivitySplash.this, ActivityModelList.class);
                startActivity(intent);
                finish();
            } else {
                showError("등록 중 오류가 발생했습니다.");
            }
        }
    }

    /**
     * 에러 메시지 표시
     */
    private void showError(String message) {
        runOnUiThread(() -> {
            if (tvErrorMessage != null) {
                tvErrorMessage.setText(message);
                tvErrorMessage.setVisibility(View.VISIBLE);
            }
        });
    }

    /**
     * 키보드 숨기기
     */
    private void hideKeyboard() {
        runOnUiThread(() -> {
            if (etBusinessNumber != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(etBusinessNumber.getWindowToken(), 0);
                }
            }
        });
    }

    /**
     * 클라이언트 ID 등록 다이얼로그 표시 (WebView 사용) - 주석 처리됨
     * 향후 WebView 구현이 필요할 경우 주석을 해제하여 사용할 수 있습니다.
     */
    /*
    private void showClientIdRegistrationDialog_WebView() {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("고객 ID 등록 필요");

            // WebView 생성
            webView = new WebView(this);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setDomStorageEnabled(true);
            webView.getSettings().setLoadWithOverviewMode(true);
            webView.getSettings().setUseWideViewPort(true);
            
            // WebChromeClient 설정 (JavaScript 다이얼로그 처리)
            webView.setWebChromeClient(new WebChromeClient() {
                @Override
                public boolean onJsAlert(WebView view, String url, String message, android.webkit.JsResult result) {
                    // Activity가 종료 중이면 다이얼로그를 표시하지 않음
                    if (isFinishing() || isDestroyed()) {
                        result.cancel();
                        return true;
                    }
                    // 기본 동작 수행
                    return false;
                }

                @Override
                public boolean onJsConfirm(WebView view, String url, String message, android.webkit.JsResult result) {
                    if (isFinishing() || isDestroyed()) {
                        result.cancel();
                        return true;
                    }
                    return false;
                }

                @Override
                public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, android.webkit.JsPromptResult result) {
                    if (isFinishing() || isDestroyed()) {
                        result.cancel();
                        return true;
                    }
                    return false;
                }
            });
            
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    // 페이지 로드 후 포커스 요청
                    view.postDelayed(() -> {
                        view.requestFocusFromTouch();
                        view.evaluateJavascript("document.getElementById('business_number')?.focus();", null);
                    }, 200);
                    view.postDelayed(() -> {
                        view.evaluateJavascript("document.getElementById('business_number')?.focus();", null);
                    }, 500);
                    view.postDelayed(() -> {
                        view.evaluateJavascript("document.getElementById('business_number')?.focus();", null);
                    }, 1000);
                }
            });

            // WebServer에 WebView 및 serverIp 설정
            WebServer webServer = ((KioskModeApplication) getApplication()).getWebServer();
            if (webServer != null) {
                webServer.setWebView(webView);
                webServer.setServerIp(serverIp);
            }

            // 로컬 서버 URL 로드
            webView.loadUrl("http://localhost:" + Constants.InitialValues.WS_PORT + "/client_id");

            builder.setView(webView);
            builder.setCancelable(false);

            clientIdDialog = builder.create();
            clientIdDialog.setOnShowListener(dialogInterface -> {
                // 다이얼로그 표시 후 포커스 요청
                webView.postDelayed(() -> {
                    webView.requestFocusFromTouch();
                    webView.evaluateJavascript("document.getElementById('business_number')?.focus();", null);
                }, 200);
                webView.postDelayed(() -> {
                    webView.evaluateJavascript("document.getElementById('business_number')?.focus();", null);
                }, 500);
                webView.postDelayed(() -> {
                    webView.evaluateJavascript("document.getElementById('business_number')?.focus();", null);
                }, 1000);

                // 다이얼로그 창 설정 (키보드 표시를 위해)
                Window dialogWindow = clientIdDialog.getWindow();
                if (dialogWindow != null) {
                    dialogWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                    dialogWindow.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
                    dialogWindow.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
                    
                    // 다이얼로그 위치 조정 (키보드가 올라올 때)
                    WindowManager.LayoutParams params = dialogWindow.getAttributes();
                    params.gravity = Gravity.CENTER;
                    params.y = -100; // 위로 100px 이동
                    dialogWindow.setAttributes(params);
                }
            });

            // 주기적으로 DB 확인 (클라이언트 ID가 등록되면 다이얼로그 닫기)
            startPeriodicCheck(clientIdDialog);

            clientIdDialog.show();
        });
    }
    */

    /**
     * 주기적으로 클라이언트 ID 등록 여부 확인
     */
    private void startPeriodicCheck(AlertDialog dialog) {
        stopPeriodicCheck();
        periodicCheckHandler = new Handler(Looper.getMainLooper());
        periodicCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (isFinishing() || isDestroyed() || dialog == null || !dialog.isShowing()) {
                    stopPeriodicCheck();
                    return;
                }

                if (checkClientIdRegistration()) {
                    // 클라이언트 ID가 등록되었으면 다이얼로그 닫고 메인 액티비티로 이동
                    dialog.dismiss();
                    stopPeriodicCheck();
                    Intent intent = new Intent(ActivitySplash.this, ActivityModelList.class);
                    startActivity(intent);
                    finish();
                } else {
                    // 1초 후 다시 확인
                    if (periodicCheckHandler != null && periodicCheckRunnable != null) {
                        periodicCheckHandler.postDelayed(this, 1000);
                    }
                }
            }
        };
        periodicCheckHandler.postDelayed(periodicCheckRunnable, 1000);
    }

    /**
     * 주기적 확인 중지
     */
    private void stopPeriodicCheck() {
        if (periodicCheckHandler != null && periodicCheckRunnable != null) {
            periodicCheckHandler.removeCallbacks(periodicCheckRunnable);
            periodicCheckRunnable = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // 주기적 확인 중지
        stopPeriodicCheck();
        
        // WebView 정리
        if (webView != null) {
            try {
                webView.stopLoading();
                webView.clearHistory();
                webView.clearCache(true);
                webView.loadUrl("about:blank");
                webView.onPause();
                webView.removeAllViews();
                webView.destroyDrawingCache();
                webView.pauseTimers();
                webView = null;
            } catch (Exception e) {
                Log.e(TAG, "WebView 정리 중 오류", e);
            }
        }
        
        // 다이얼로그 정리
        if (clientIdDialog != null && clientIdDialog.isShowing()) {
            try {
                clientIdDialog.dismiss();
            } catch (Exception e) {
                Log.e(TAG, "다이얼로그 닫기 중 오류", e);
            }
            clientIdDialog = null;
        }
    }
}

