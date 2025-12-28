package itf.com.app.lms;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import itf.com.app.lms.item.ItemAdapterTestHistory;
import itf.com.app.lms.kiosk.BaseKioskActivity;
import itf.com.app.lms.util.TestData;
import itf.com.app.lms.vo.VoTestHistory;

public class ActivityTestHistoryList extends BaseKioskActivity implements View.OnClickListener {

    private static final String TAG = "ActivityTestHistoryList";
    TextView btn_activity_quit = null;

    TextView txtText;
    private ItemAdapterTestHistory listItemAdapter = null;
    private ListView itemList = null;
    private ConstraintLayout ll_popup_header = null;
    private TextView tv_popup_header_title = null;
    private Context mContext = null;
    private FloatingActionButton fab_close = null;
    private FloatingActionButton fab_info_list_mail = null;
    private FloatingActionButton fab_toggle_checkbox = null;
    private boolean allChecked = false; // 전체 선택 상태 추적
    private ImageButton ib_header_action = null;
    CheckBox cb_item_check_all = null;
    private final ExecutorService mailExecutor = Executors.newSingleThreadExecutor();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //타이틀바 없애기
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_test_history);

        mContext = this;

        //UI 객체생성
        txtText = findViewById(R.id.tv_param_from_parent);
        ll_popup_header = findViewById(R.id.ll_popup_header);
        btn_activity_quit = findViewById(R.id.btn_activity_quit);
        tv_popup_header_title = findViewById(R.id.tv_popup_header_title);
        ib_header_action = findViewById(R.id.ib_header_action);

        fab_close = findViewById(R.id.fab_close);
        fab_info_list_mail = findViewById(R.id.fab_info_list_mail);
        fab_toggle_checkbox = findViewById(R.id.fab_toggle_checkbox);
        cb_item_check_all = findViewById(R.id.cb_item_check_all);

        fab_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!triggerClickVibration("fab_close")) {
                    warnIfNoVibratorOnce();
                }
                restartApplication(getApplicationContext());
            }
        });

        // visibility 토글 FAB 클릭 이벤트
        fab_toggle_checkbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!triggerClickVibration("fab_toggle_checkbox")) {
                    warnIfNoVibratorOnce();
                }
                // cb_item_check_all.toggle(); // 체크박스 토글
                toggleAllVisibility();
            }
        });

        // 메일 전달 FAB 클릭 이벤트
        if (fab_info_list_mail != null) {
            fab_info_list_mail.setOnClickListener(v -> {
                if (!triggerClickVibration("fab_info_list_mail")) {
                    warnIfNoVibratorOnce();
                }
                sendTestResultsByEmail();
            });
        }

        ib_header_action.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ActivityTestHistoryList.this, ActivitySystemSetting.class);
                startActivity(intent);
                finish();
            }
        });

        // ll_list_header에서 체크박스 찾아서 이벤트 연결
        LinearLayout llListHeader = findViewById(R.id.ll_list_header);
        if (llListHeader != null) {
            // cb_item_check_all = llListHeader.findViewById(R.id.cb_item_check_all);
            if (cb_item_check_all != null) {
                cb_item_check_all.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        toggleAllCheckboxes(isChecked);
                    }
                });
            }
        }

        //데이터 가져오기
        Intent intent = getIntent();
        String data = intent.getStringExtra("data");
//        String strOKYn = intent.getStringExtra("ok_yn");
        txtText.setText(data);
//        Log.i(TAG, "> ActivityTestHistoryList.strOKYn " + R.string.txt_ok + " " + strOKYn);

//        if(strOKYn.equals(getString(R.string.txt_ok))) {
//            Log.i(TAG, "> ActivityTestHistoryList.strOKYn.OK " + strOKYn);
//            ll_popup_header.setBackgroundColor(ContextCompat.getColor(mContext, R.color.blue_01));
//            tv_popup_header_title.setText(R.string.txt_ok_list);
//        }
//        else if(strOKYn.equals(getString(R.string.txt_ng))) {
//            Log.i(TAG, "> ActivityTestHistoryList.strOKYn.NG " + strOKYn);
//            ll_popup_header.setBackgroundColor(ContextCompat.getColor(mContext, R.color.red_02));
//            tv_popup_header_title.setText(R.string.txt_ng_list);
//        }

        listItemAdapter = new ItemAdapterTestHistory();
        itemList = findViewById(R.id.lv_process_result_list);

        // 체크박스 상태 변경 리스너 설정 -> 메일 버튼 활성/비활성 및 색상 변경
        listItemAdapter.setOnCheckboxStateChangeListener(hasCheckedItems -> updateMailButtonColor(hasCheckedItems));

        // 체크박스 visibility 상태 변경 리스너 설정 -> 토글 버튼 색상 변경
        listItemAdapter.setOnCheckboxVisibilityChangeListener(isVisible -> updateToggleCheckboxButtonColor(isVisible));

        List<Map<String, String>> lstTestHistory = TestData.selectTestHistory(this, "");
        // List<Map<String, String>> lstProcessResult = mainActivity.lstTestResult;

        Map<String, String> mapListItem = null;
        for(int i=lstTestHistory.size()-1; i>=0; i--) {
            mapListItem = lstTestHistory.get(i);
            /*
//            mapListItem.put("process_no", valueOf(i));
//            mapListItem.put("process_title", "process_title" + i);
//            mapListItem.put("process_result", ((i%2==0)?"OK":"NG"));
////            lstProcessResultList.add(mapListItem);

            /*tv_test_datetime.setText(voTestHistory.getTest_datetime());
            tv_test_result.setText(voTestHistory.getTest_result());
            tv_test_ok_count.setText(voTestHistory.getTest_ok_count());
            tv_test_ng_count.setText(voTestHistory.getTest_ng_count());
            tv_test_line.setText(voTestHistory.getTest_line());
            tv_test_model_id.setText(voTestHistory.getTest_line());* /

            mapListItem.put("test_no", String.valueOf(i));
            mapListItem.put("test_datetime", "9999/12/30");
            mapListItem.put("test_result", "OK");
            mapListItem.put("test_ok_count", "10");
            mapListItem.put("test_ng_count", "0");
            mapListItem.put("test_line", "4" + "라인");
            mapListItem.put("test_model_name", "test_model_name");
            mapListItem.put("test_serial_no", "test_serial_no");
            mapListItem.put("test_datetime", "9999/12/30");
             */

            mapListItem.put("clm_test_history_no", String.valueOf(i+1));
            Log.i(TAG, ">>> " + i + " " + mapListItem);
            
            // if(mapListItem.get("test_result").equals(getString(R.string.txt_ok))) {
                listItemAdapter.addItem(new VoTestHistory(mapListItem));
            // }
        }

        btn_activity_quit.setOnClickListener(this);

        itemList.setAdapter(listItemAdapter);
        
        // 초기 체크박스 visibility 상태 설정
        if (cb_item_check_all != null && listItemAdapter != null) {
            boolean isVisible = listItemAdapter.isCheckboxVisible();
            cb_item_check_all.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        }

        // 초기 메일 버튼 상태 설정 (체크된 항목이 없으므로 gray_04)
        updateMailButtonColor(false);

        // 초기 체크박스 토글 버튼 상태 설정 (비활성화 상태이므로 blue_for_ovio)
        updateToggleCheckboxButtonColor(listItemAdapter.isCheckboxVisible());
    }

    private void restartApplication(Context mContext) {
        // 현재 액티비티만 종료
        try {
            Log.i(TAG, "finishing ActivityTestHistoryDetail");
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error finishing activity: " + e.toString(), e);
        }
    }

    public void onClick(View v) {
        if (v.getId() == R.id.btn_activity_quit) {
            ActivityTestHistoryList.this.finish();
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

//    @Override
//    public void onBackPressed() {
//        //안드로이드 백버튼 막기
//        return;
//    }

    /**
     * 체크된 항목들의 리스트를 반환
     */
    public ArrayList<VoTestHistory> getCheckedItems() {
        if (listItemAdapter != null) {
            return listItemAdapter.getCheckedItems();
        }
        return new ArrayList<>();
    }

    /**
     * 노출된 항목들의 리스트를 반환
     */
    public ArrayList<VoTestHistory> getVisibleItems() {
        if (listItemAdapter != null) {
            return listItemAdapter.getVisibleItems();
        }
        return new ArrayList<>();
    }

    /**
     * 모든 체크박스 토글 (전체 선택/해제)
     */
    private void toggleAllCheckboxes(boolean checked) {
        if (listItemAdapter != null) {
            allChecked = checked;
            listItemAdapter.setAllChecked(checked);
            Log.i(TAG, "All checkboxes toggled: " + checked);
        }
    }

    /**
     * 모든 항목의 visibility 토글
     */
    private void toggleAllVisibility() {
        if (listItemAdapter != null) {
            listItemAdapter.toggleAllVisibility();
            // 헤더의 체크박스도 함께 토글
            if (cb_item_check_all != null) {
                boolean isVisible = listItemAdapter.isCheckboxVisible();
                cb_item_check_all.setVisibility(isVisible ? View.VISIBLE : View.GONE);
            }
            Log.i(TAG, "All visibility toggled");
        }
    }

    /**
     * 메일 버튼의 배경색을 체크박스 상태에 따라 업데이트
     */
    private void updateMailButtonColor(boolean hasCheckedItems) {
        if (fab_info_list_mail == null) return;

        if (hasCheckedItems) {
            fab_info_list_mail.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.blue_for_ovio));
            fab_info_list_mail.setEnabled(true);
        } else {
            fab_info_list_mail.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.gray_04));
            fab_info_list_mail.setEnabled(false);
        }
    }

    /**
     * 체크박스 토글 버튼의 배경색을 체크박스 visibility 상태에 따라 업데이트
     */
    private void updateToggleCheckboxButtonColor(boolean isVisible) {
        if (fab_toggle_checkbox == null) return;

        // 체크박스가 활성화(visible)되어 있을 때: gray_04, 비활성일 때: blue_for_ovio
        if (isVisible) {
            fab_toggle_checkbox.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.gray_04));
        } else {
            fab_toggle_checkbox.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.blue_for_ovio));
        }
    }

    /**
     * 선택된 검사 결과를 CSV로 생성하고, tbl_setting_info(TEST_EMAIL)의 이메일 주소로 전달
     */
    private void sendTestResultsByEmail() {
        ArrayList<VoTestHistory> checkedItems = getCheckedItems();
        if (checkedItems == null || checkedItems.isEmpty()) {
            Toast.makeText(this, "선택된 항목이 없습니다.", Toast.LENGTH_SHORT).show();
            updateMailButtonColor(false);
            return;
        }

        String recipientEmail = getTestEmailAddressFromSettings();
        if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
            Toast.makeText(this, "메일 전달 이메일 주소(TEST_EMAIL)가 설정되어 있지 않습니다.", Toast.LENGTH_LONG).show();
            return;
        }

        // CSV 생성은 백그라운드에서 수행 (StrictMode 회피)
        mailExecutor.execute(() -> {
            try {
                File csvFile = createTestResultsCsvFile(checkedItems);
                if (csvFile == null || !csvFile.exists()) {
                    runOnUiThread(() -> Toast.makeText(this, "CSV 파일 생성에 실패했습니다.", Toast.LENGTH_SHORT).show());
                    return;
                }

                runOnUiThread(() -> {
                    try {
                        sendEmailWithAttachment(recipientEmail, csvFile);
                        clearAllCheckboxes();
                    } catch (Exception e) {
                        Toast.makeText(this, "이메일 전송 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "CSV 생성 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private String getTestEmailAddressFromSettings() {
        try {
            List<Map<String, String>> settings = TestData.selectSettingInfo(this);
            for (Map<String, String> row : settings) {
                if (row == null) continue;
                String id = row.get("clm_setting_id");
                if ("TEST_EMAIL".equals(id)) {
                    return row.get("clm_setting_value");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load TEST_EMAIL from tbl_setting_info", e);
        }
        return null;
    }

    private String getDefaultEmailAppPackage() {
        try {
            List<Map<String, String>> settings = TestData.selectSettingInfo(this);
            for (Map<String, String> row : settings) {
                if (row == null) continue;
                String id = row.get("clm_setting_id");
                if ("DEFAULT_EMAIL_APP".equals(id)) {
                    String pkg = row.get("clm_setting_value");
                    return pkg != null ? pkg.trim() : null;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load DEFAULT_EMAIL_APP from tbl_setting_info", e);
        }
        return null;
    }

    private File createTestResultsCsvFile(ArrayList<VoTestHistory> checkedItems) throws Exception {
        File exportDir = new File(getExternalFilesDir(null), "export");
        if (!exportDir.exists() && !exportDir.mkdirs()) {
            return null;
        }

        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File csvFile = new File(exportDir, "test_results_" + ts + ".csv");

        // UTF-8 BOM + CSV (엑셀 한글 깨짐 방지)
        try (FileOutputStream fos = new FileOutputStream(csvFile);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {

            writer.write('\uFEFF'); // BOM

            // 헤더
            writer.write("test_history_seq,test_timestamp,test_model_id,test_model_name,test_result,ok_count,ng_count\n");

            for (VoTestHistory history : checkedItems) {
                if (history == null) continue;

                writer.write(escapeCsvField(history.getTest_history_seq()) + ",");
                writer.write(escapeCsvField(history.getTest_timestamp()) + ",");
                writer.write(escapeCsvField(history.getTest_model_id()) + ",");
                writer.write(escapeCsvField(history.getTest_model_name()) + ",");
                writer.write(escapeCsvField(history.getTest_result()) + ",");
                writer.write(escapeCsvField(history.getTest_ok_count()) + ",");
                writer.write(escapeCsvField(history.getTest_ng_count()) + "\n");

                // 상세 내역 추가
                List<Map<String, String>> details = TestData.selectTestHistoryDetail(this, history.getTest_history_seq());
                writer.write("detail_seq,test_item_seq,test_item_name,test_item_result,test_item_value,test_response_value,test_result_value,comment\n");
                if (details != null) {
                    for (Map<String, String> d : details) {
                        if (d == null) continue;
                        writer.write(escapeCsvField(d.get("clm_test_history_detail_seq")) + ",");
                        writer.write(escapeCsvField(d.get("clm_test_item_seq")) + ",");
                        writer.write(escapeCsvField(d.get("clm_test_item_name")) + ",");
                        writer.write(escapeCsvField(d.get("clm_test_item_result")) + ",");
                        writer.write(escapeCsvField(d.get("clm_test_item_value")) + ",");
                        writer.write(escapeCsvField(d.get("clm_test_response_value")) + ",");
                        writer.write(escapeCsvField(d.get("clm_test_result_value")) + ",");
                        writer.write(escapeCsvField(d.get("clm_comment")) + "\n");
                    }
                }
                writer.write("\n");
            }
        }

        Log.i(TAG, "CSV 파일 작성 완료: " + csvFile.getAbsolutePath());
        return csvFile;
    }

    private String escapeCsvField(String v) {
        if (v == null) return "";
        String s = v;
        boolean needQuote = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        if (s.contains("\"")) {
            s = s.replace("\"", "\"\"");
        }
        return needQuote ? ("\"" + s + "\"") : s;
    }

    private void sendEmailWithAttachment(String recipientEmail, File csvFile) {
        Uri uri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".provider",
                csvFile
        );

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{recipientEmail});
        intent.putExtra(Intent.EXTRA_SUBJECT, "검사 결과 CSV");
        intent.putExtra(Intent.EXTRA_TEXT, "검사 결과 CSV 파일을 첨부합니다.");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // 기본 이메일 앱 설정이 있으면 우선 시도
        String defaultEmailPkg = getDefaultEmailAppPackage();
        if (defaultEmailPkg != null && !defaultEmailPkg.isEmpty()) {
            intent.setPackage(defaultEmailPkg);
            PackageManager pm = getPackageManager();
            ResolveInfo resolved = pm.resolveActivity(intent, 0);
            if (resolved == null) {
                // 설정된 앱이 현재 인텐트를 처리 못하면 패키지 해제하고 chooser
                intent.setPackage(null);
            }
        }

        startActivity(Intent.createChooser(intent, "이메일 앱 선택"));
    }

    private void clearAllCheckboxes() {
        if (listItemAdapter != null) {
            listItemAdapter.setAllChecked(false);
            updateMailButtonColor(false);
        }
        if (cb_item_check_all != null) {
            cb_item_check_all.setChecked(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            mailExecutor.shutdownNow();
        } catch (Exception ignored) {
            // ignore
        }
    }
}