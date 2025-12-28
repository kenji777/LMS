package itf.com.lms;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import itf.com.lms.renderer.CustomXAxisRenderer;
import itf.com.lms.renderer.CustomYAxisRenderer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import itf.com.lms.item.ItemAdapterTestDetailItem;
import itf.com.lms.kiosk.BaseKioskActivity;
import itf.com.lms.util.TestData;
import itf.com.lms.vo.VoTestItem;
import itf.com.lms.R;

public class ActivityTestHistoryDetail extends BaseKioskActivity implements View.OnClickListener {

    private static final String TAG = "TestHistoryDetail";
    TextView btn_activity_quit = null;

    TextView txtText;
    private ItemAdapterTestDetailItem listItemAdapter = null;
    private ListView itemList = null;
    private LinearLayout ll_popup_header = null;
    private TextView tv_popup_header_title = null;
    private Context mContext = null;
    private String testHistorySeq = "";
    private FloatingActionButton fab_close = null;
    private FloatingActionButton fab_chart = null;
    private ImageButton ib_test_hisotry_comment = null;
    private ImageButton ib_test_hisotry_comment_delete = null;
    private EditText et_test_detail_comment = null;
    private boolean isTestDetailComment = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //타이틀바 없애기
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_test_history_detail);

        mContext = this;

        //UI 객체생성
        txtText = findViewById(R.id.tv_param_from_parent);
        ll_popup_header = findViewById(R.id.ll_popup_header);
        btn_activity_quit = findViewById(R.id.btn_activity_quit);
        tv_popup_header_title = findViewById(R.id.tv_popup_header_title);
        ib_test_hisotry_comment = findViewById(R.id.ib_test_hisotry_comment);
        ib_test_hisotry_comment_delete = findViewById(R.id.ib_test_hisotry_comment_delete);
        et_test_detail_comment = findViewById(R.id.te_test_detail_comment);

        fab_chart = findViewById(R.id.fab_chart);
        fab_close = findViewById(R.id.fab_close);

        fab_chart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showChartDialog();
            }
        });

        et_test_detail_comment.setEnabled(false);
        et_test_detail_comment.setFocusable(false);
        et_test_detail_comment.setFocusableInTouchMode(false);

        ib_test_hisotry_comment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isTestDetailComment) {
                    isTestDetailComment = true;
                    et_test_detail_comment.setEnabled(true);
                    et_test_detail_comment.setFocusable(true);
                    et_test_detail_comment.setFocusableInTouchMode(true);
                    et_test_detail_comment.setBackgroundColor(getBaseContext().getResources().getColor(R.color.white));
                    ib_test_hisotry_comment.setImageResource(R.drawable.save_24dp_ffffff_fill0_wght400_grad0_opsz24);
                    ib_test_hisotry_comment_delete.setAlpha(1f);
                    ib_test_hisotry_comment_delete.setEnabled(true);
                    // 키보드 자동 표시
                    showKeyboard(et_test_detail_comment);
                }
                else {
                    // 키보드 자동 숨김
                    hideKeyboard(et_test_detail_comment);
                    // 저장 확인 다이얼로그 표시
                    showSaveCommentDialog();
                }
            }
        });

        // EditText 포커스 리스너 추가
        et_test_detail_comment.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && isTestDetailComment) {
                    // 포커스를 얻으면 키보드 표시
                    showKeyboard(et_test_detail_comment);
                }
            }
        });

        ib_test_hisotry_comment_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                et_test_detail_comment.setText("");
            }
        });

        fab_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                restartApplication(getApplicationContext());
            }
        });

        // Intent에서 검사 히스토리 SEQ 가져오기
        Intent intent = getIntent();
        testHistorySeq = intent.getStringExtra("test_history_seq");
        String testModelName = intent.getStringExtra("test_model_name");
        String testTimestamp = intent.getStringExtra("test_timestamp");
        String testResult = intent.getStringExtra("test_result");

        if (testHistorySeq == null || testHistorySeq.isEmpty()) {
            Log.e(TAG, "test_history_seq is null or empty");
            finish();
            return;
        }

        // 헤더 정보 설정
        if (tv_popup_header_title != null) {
            String headerText = "검사 상세 내역 ▶";
            if (testModelName != null && !testModelName.isEmpty()) {
                headerText += " " + testModelName;
            }
            if (testTimestamp != null && !testTimestamp.isEmpty()) {
                headerText += " (" + testTimestamp + ")";
            }
            tv_popup_header_title.setText(headerText);
        }

        listItemAdapter = new ItemAdapterTestDetailItem();
        itemList = findViewById(R.id.lv_test_history_detail_list);

        // btn_activity_quit 리스너 설정 (데이터 유무와 관계없이 항상 설정)
        if (btn_activity_quit != null) {
            btn_activity_quit.setOnClickListener(this);
            btn_activity_quit.setClickable(true);
            btn_activity_quit.setFocusable(true);
        } else {
            Log.e(TAG, "btn_activity_quit is null");
        }

        // 검사 상세 내역 조회
        List<Map<String, String>> lstTestHistory = TestData.selectTestHistory(this, testHistorySeq);
        List<Map<String, String>> lstTestHistoryDetail = TestData.selectTestHistoryDetail(this, testHistorySeq);
        int testItemSeqCount = 0;

        if(!lstTestHistory.isEmpty()) {
            for (int i = 0; i < lstTestHistory.size(); i++) {
                // System.out.println("<><><><><><> lstTestHistory.get(" + i + ") " + lstTestHistory.get(i).toString());
                et_test_detail_comment.setText(lstTestHistory.get(i).get("clm_comment"));
            }
        }

        if(!lstTestHistoryDetail.isEmpty()) {
            // 리스트 어댑터에 데이터 추가
            for (int i = 0; i < lstTestHistoryDetail.size(); i++) {
                Map<String, String> mapDetail = lstTestHistoryDetail.get(i);
                Map<String, String> mapListItem = new java.util.HashMap<>();
                // System.out.println("<><><><><><> mapDetail " + mapDetail.toString());

                if(mapDetail.get("clm_test_item_command").contains("SV") ||
                        mapDetail.get("clm_test_item_command").contains("HT") ||
                        mapDetail.get("clm_test_item_command").contains("LD") ||
                        mapDetail.get("clm_test_item_command").contains("PM") ||
                        mapDetail.get("clm_test_item_command").contains("UV")) {
                    if(mapDetail.get("clm_test_item_command").substring(4, 6).equals("00")) {
                        testItemSeqCount += 0;
                    }
                    else {
                        testItemSeqCount++;
                    }
                }
                else {
                    testItemSeqCount++;
                }

                // mapListItem.put("test_item_seq", mapDetail.get("clm_test_item_seq"));
                mapListItem.put("test_item_seq", String.valueOf(testItemSeqCount));
                mapListItem.put("test_item_name", mapDetail.get("clm_test_item_name"));
                mapListItem.put("test_item_command", mapDetail.get("clm_test_item_command"));
                mapListItem.put("test_item_result", mapDetail.get("clm_test_item_result"));
                mapListItem.put("test_item_value", mapDetail.get("clm_test_item_value"));
                mapListItem.put("test_response_value", mapDetail.get("clm_test_response_value"));
                mapListItem.put("test_result_value", mapDetail.get("clm_test_result_value"));
                mapListItem.put("test_temperature", mapDetail.get("clm_test_temperature"));
                mapListItem.put("test_electric_val", mapDetail.get("clm_test_electric_val"));
                mapListItem.put("test_item_info", mapDetail.get("clm_test_item_info") != null ? mapDetail.get("clm_test_item_info") : "");
                mapListItem.put("test_result_check_value", mapDetail.get("clm_test_result_check_value") != null ? mapDetail.get("clm_test_result_check_value") : "");
                mapListItem.put("test_upper_value", mapDetail.get("clm_test_upper_value") != null ? mapDetail.get("clm_test_upper_value") : "");
                mapListItem.put("test_lower_value", mapDetail.get("clm_test_lower_value") != null ? mapDetail.get("clm_test_lower_value") : "");
                mapListItem.put("test_bt_raw_message", mapDetail.get("clm_bt_raw_message") != null ? mapDetail.get("clm_bt_raw_message") : "");
                mapListItem.put("test_bt_raw_response", mapDetail.get("clm_bt_raw_response") != null ? mapDetail.get("clm_bt_raw_response") : "");
                mapListItem.put("test_bt_processed_value", mapDetail.get("clm_bt_processed_value") != null ? mapDetail.get("clm_bt_processed_value") : "");
                mapListItem.put("test_finish_yn", "Y"); // 저장된 데이터는 모두 완료된 것으로 표시
                mapListItem.put("test_model_id", ""); // 필요시 추가

                // System.out.println(">>>>>>>>>>>>>>>>> test_model_id:" + mapListItem.get("test_model_id") + " test_item_command:" + mapListItem.get("test_item_command") + " test_item_info:" + mapListItem.get("test_item_info"));

                listItemAdapter.addItem(new VoTestItem(mapListItem));
            }

            itemList.setAdapter(listItemAdapter);
        } else {
            Log.w(TAG, "검사 상세 내역 데이터가 없습니다.");
        }
        
        // tbl_test_history_linear_data 테이블에서 데이터 존재 여부 확인
        List<Map<String, String>> linearData = TestData.selectTestHistoryLinearData(this, testHistorySeq);
        int linearDataCount = (linearData != null) ? linearData.size() : 0;
        
        // linear_data가 0개이면 fab_chart 버튼 비활성화
        if (fab_chart != null) {
            if (linearDataCount == 0) {
                fab_chart.setEnabled(false);
                fab_chart.setAlpha(0.5f); // 반투명 처리로 비활성화 표시
                Log.i(TAG, "linear_data가 없어 fab_chart 버튼을 비활성화했습니다. (개수: " + linearDataCount + ")");
            } else {
                fab_chart.setEnabled(true);
                fab_chart.setAlpha(1.0f); // 완전 불투명으로 활성화 표시
                Log.i(TAG, "linear_data가 있어 fab_chart 버튼을 활성화했습니다. (개수: " + linearDataCount + ")");
            }
        }
        
        Log.i(TAG, "검사 상세 내역 로드 완료: " + lstTestHistoryDetail.size() + "개 항목, linear_data: " + linearDataCount + "개");
        
        // comment 로드
        loadComment();
    }
    
    /**
     * 특이사항(comment) 로드
     */
    private void loadComment() {
        if (testHistorySeq == null || testHistorySeq.isEmpty()) {
            return;
        }
        
        try {
            List<Map<String, String>> testHistoryList = TestData.selectTestHistory(this, "");
            if (testHistoryList != null) {
                for (Map<String, String> history : testHistoryList) {
                    if (testHistorySeq.equals(history.get("clm_test_history_seq"))) {
                        String comment = history.get("clm_comment");
                        if (comment != null && et_test_detail_comment != null) {
                            et_test_detail_comment.setText(comment);
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading comment: " + e.getMessage(), e);
        }
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
            Log.i(TAG, "btn_activity_quit clicked, finishing activity");
            ActivityTestHistoryDetail.this.finish();
        } else {
            Log.w(TAG, "onClick called with unexpected view id: " + v.getId());
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

    /**
     * 차트 다이얼로그 표시
     */
    private void showChartDialog() {
        try {
            // 데이터 조회
            List<Map<String, String>> linearData = TestData.selectTestHistoryLinearData(this, testHistorySeq);
            
            if (linearData == null || linearData.isEmpty()) {
                new AlertDialog.Builder(this)
                    .setTitle("알림")
                    .setMessage("표시할 차트 데이터가 없습니다.")
                    .setPositiveButton("확인", null)
                    .show();
                return;
            }

            // 다이얼로그 레이아웃 인플레이트
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_chart, null);
            LineChart lineChart = dialogView.findViewById(R.id.lineChart);
            TextView btnChartClose = dialogView.findViewById(R.id.btn_chart_close);

            // 차트 설정
            setupChart(lineChart, linearData);

            // 다이얼로그 생성
            AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

            // 닫기 버튼 클릭 이벤트
            btnChartClose.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });

            dialog.show();

            if (dialog.getWindow() != null) {
                DisplayMetrics dm = getResources().getDisplayMetrics();
                dialog.getWindow().setLayout(
                    (int) (dm.widthPixels * 0.9f),
                    (int) (dm.heightPixels * 0.85f)
                );

                // View chartView = dialogView.findViewById(R.id.lineChart);
                if (lineChart != null) {
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                    );
                    // int horizontalPadding = (int) (dm.widthPixels * 0.05f);
                    // params.setMargins(horizontalPadding, 0, horizontalPadding, 0);
                    params.width = (int) (dm.widthPixels * 0.9f);
                    params.height = (int) (dm.heightPixels * 0.85f);
                    lineChart.setLayoutParams(params);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing chart dialog", e);
            new AlertDialog.Builder(this)
                .setTitle("오류")
                .setMessage("차트를 표시하는 중 오류가 발생했습니다: " + e.getMessage())
                .setPositiveButton("확인", null)
                .show();
        }
    }

    /**
     * 차트 설정 및 데이터 추가
     */
    private void setupChart(LineChart lineChart, List<Map<String, String>> linearData) {
        try {
            // 온도 데이터 리스트
            List<Entry> temperatureEntries = new ArrayList<>();
            // 와트 데이터 리스트
            List<Entry> wattEntries = new ArrayList<>();

            // 데이터 파싱 및 Entry 생성
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.getDefault());
            List<String> timestampLabels = new ArrayList<>();
            long baseTimestamp = 0; // 첫 번째 타임스탬프를 기준점으로 저장
            
            // 먼저 첫 번째 타임스탬프를 기준점으로 설정
            if (!linearData.isEmpty()) {
                Map<String, String> firstData = linearData.get(0);
                String firstTimestampStr = firstData.get("clm_test_timestamp");
                if (firstTimestampStr != null && !firstTimestampStr.isEmpty()) {
                    try {
                        Date firstTimestamp = dateFormat.parse(firstTimestampStr);
                        if (firstTimestamp != null) {
                            baseTimestamp = firstTimestamp.getTime();
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Error parsing first timestamp: " + firstTimestampStr, e);
                    }
                }
            }

            // 기준점으로부터의 경과 시간 계산 (밀리초)
            long elapsedMillis = 0;
            // 초로 변환
            // float elapsedSeconds = 0;
            int elapsedSeconds = 0;
            
            for (int i = 0; i < linearData.size(); i++) {
                Map<String, String> data = linearData.get(i);
                String timestampStr = data.get("clm_test_timestamp");
                String temperatureStr = data.get("clm_temperature");
                String wattStr = data.get("clm_watt");

                try {
                    // X축: 첫 번째 타임스탬프로부터의 경과 시간(초)
                    float xValue = 0f;
                    String timeLabel = "0초";
                    
                    if (timestampStr != null && !timestampStr.isEmpty() && baseTimestamp > 0) {
                        try {
                            Date timestamp = dateFormat.parse(timestampStr);
                            if (timestamp != null) {
                                // 기준점으로부터의 경과 시간 계산 (밀리초)
                                elapsedMillis = timestamp.getTime() - baseTimestamp;
                                // 초로 변환
                                elapsedSeconds = (int)(elapsedMillis / 1000f);
                                xValue = elapsedSeconds;

                                timeLabel = String.format(Locale.getDefault(), "%d", elapsedSeconds);
                                /*
                                // 레이블 생성 (초 단위로 표시)
                                if (elapsedSeconds < 60) {
                                    timeLabel = String.format(Locale.getDefault(), "%.1f초", elapsedSeconds);
                                } else if (elapsedSeconds < 3600) {
                                    int minutes = (int)(elapsedSeconds / 60);
                                    float seconds = elapsedSeconds % 60;
                                    timeLabel = String.format(Locale.getDefault(), "%d: %.1f", minutes, seconds);
                                } else {
                                    int hours = (int)(elapsedSeconds / 3600);
                                    int minutes = (int)((elapsedSeconds % 3600) / 60);
                                    float seconds = elapsedSeconds % 60;
                                    timeLabel = String.format(Locale.getDefault(), "%d: %d", hours, minutes);
                                }
                                */
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Error parsing timestamp: " + timestampStr, e);
                            xValue = i; // 파싱 실패 시 인덱스 사용
                            timeLabel = String.valueOf(i + 1);
                        }
                    } else {
                        xValue = i; // 타임스탬프가 없으면 인덱스 사용
                        timeLabel = String.valueOf(i + 1);
                    }
                    
                    timestampLabels.add(timeLabel);

                    // Y축: 온도 값
                    if (temperatureStr != null && !temperatureStr.isEmpty() && !temperatureStr.equals("null")) {
                        try {
                            float temperature = Float.parseFloat(temperatureStr);
                            temperatureEntries.add(new Entry(xValue, temperature));
                        } catch (NumberFormatException e) {
                            Log.w(TAG, "Invalid temperature value: " + temperatureStr);
                        }
                    }

                    // Y축: 와트 값
                    if (wattStr != null && !wattStr.isEmpty() && !wattStr.equals("null")) {
                        try {
                            float watt = Float.parseFloat(wattStr);
                            wattEntries.add(new Entry(xValue, watt));
                        } catch (NumberFormatException e) {
                            Log.w(TAG, "Invalid watt value: " + wattStr);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing data at index " + i, e);
                }
            }

            // 데이터셋 생성
            LineDataSet temperatureDataSet = null;
            LineDataSet wattDataSet = null;

            if (!temperatureEntries.isEmpty()) {
                temperatureDataSet = new LineDataSet(temperatureEntries, "온도");
                temperatureDataSet.setColor(Color.parseColor("#2196F3")); // 파란색
                temperatureDataSet.setCircleColor(Color.parseColor("#2196F3"));
                temperatureDataSet.setLineWidth(2f);
                temperatureDataSet.setCircleRadius(4f);
                temperatureDataSet.setDrawCircleHole(false);
                temperatureDataSet.setValueTextSize(6f);
                temperatureDataSet.setDrawValues(false); // 값 표시 안 함
            }

            if (!wattEntries.isEmpty()) {
                wattDataSet = new LineDataSet(wattEntries, "소비전력");
                wattDataSet.setColor(Color.parseColor("#4CAF50")); // 초록색
                wattDataSet.setCircleColor(Color.parseColor("#4CAF50"));
                wattDataSet.setLineWidth(2f);
                wattDataSet.setCircleRadius(4f);
                wattDataSet.setDrawCircleHole(false);
                wattDataSet.setValueTextSize(6f);
                wattDataSet.setDrawValues(false); // 값 표시 안 함
            }

            // 실제 데이터의 X값 범위 계산 (서브 그리드용)
            float minXValue = Float.MAX_VALUE;
            float maxXValue = Float.MIN_VALUE;
            
            // 온도 데이터에서 X값 범위 찾기
            for (Entry entry : temperatureEntries) {
                float xVal = entry.getX();
                if (xVal < minXValue) minXValue = xVal;
                if (xVal > maxXValue) maxXValue = xVal;
            }
            
            // 와트 데이터에서 X값 범위 찾기
            for (Entry entry : wattEntries) {
                float xVal = entry.getX();
                if (xVal < minXValue) minXValue = xVal;
                if (xVal > maxXValue) maxXValue = xVal;
            }
            
            // 데이터가 없을 경우 기본값 설정
            if (minXValue == Float.MAX_VALUE) minXValue = 0f;
            if (maxXValue == Float.MIN_VALUE) maxXValue = 100f;

            // LineData 생성
            LineData lineData = new LineData();
            if (temperatureDataSet != null) {
                lineData.addDataSet(temperatureDataSet);
            }
            if (wattDataSet != null) {
                lineData.addDataSet(wattDataSet);
            }

            assert temperatureDataSet != null;
            temperatureDataSet.setDrawCircles(false);
            temperatureDataSet.setDrawCircleHole(false); // 이미 false지만 유지하고 싶으면 그대로

            assert wattDataSet != null;
            wattDataSet.setDrawCircles(false);
            wattDataSet.setDrawCircleHole(false);

            temperatureDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            temperatureDataSet.setCubicIntensity(0.2f); // 0–1 사이 값, 높을수록 더 휘어짐

            wattDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            wattDataSet.setCubicIntensity(0.2f);

            // 차트 설정
            lineChart.setData(lineData);
            lineChart.getDescription().setEnabled(false);
            lineChart.setTouchEnabled(true);
            lineChart.setDragEnabled(true);
            lineChart.setScaleEnabled(true);
            lineChart.setPinchZoom(true);
            lineChart.setDrawGridBackground(false);
            lineChart.setExtraOffsets(20f, 20f, 20f, 10f);  // dp 단위가 아닌 픽셀 단위

            float minTemperature = temperatureDataSet != null ? temperatureDataSet.getYMin() : Float.MAX_VALUE;
            float minWatt = wattDataSet != null ? wattDataSet.getYMin() : Float.MAX_VALUE;

            float globalMin = Math.min(minTemperature, minWatt);
            if (globalMin == Float.MAX_VALUE) {
                globalMin = 0f;
            }

            // 최소값 아래로 여유를 두기 위한 padding (예: 전체 범위의 5% 또는 고정값)
            float padding = Math.max(Math.abs(globalMin) * 0.05f, 5f);
            float axisMin = globalMin - 50;

            // Y축 적용
            YAxis leftAxis = lineChart.getAxisLeft();
            leftAxis.setAxisMinimum(axisMin);
            leftAxis.setDrawGridLines(true); // 메인 그리드 표시
            leftAxis.setLabelCount(6, true); // 레이블 개수 설정 (서브 그리드 계산에 필요)

            // X축 설정
            XAxis xAxis = lineChart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setDrawGridLines(true);
            xAxis.setGranularity(1f);
            xAxis.setLabelCount(Math.min(linearData.size(), 10), true); // 최대 10개 레이블
            xAxis.setTextSize(10f);
            xAxis.setTextColor(Color.BLACK);
            xAxis.setLabelRotationAngle(-45f); // 레이블 회전


            // 범례 설정
            Legend legend = lineChart.getLegend();
            legend.setYEntrySpace(5f);
            legend.setForm(Legend.LegendForm.CIRCLE);
            legend.setEnabled(true);
            legend.setTextSize(14f);
            legend.setYOffset(5f);
            legend.setTextColor(Color.BLACK);
            
            // 범례 클릭으로 데이터셋 토글 기능 추가
            setupLegendToggle(lineChart, temperatureDataSet, wattDataSet);

            // X축 레이블 포맷터 (타임스탬프 표시)
            final List<String> finalTimestampLabels = timestampLabels;
            xAxis.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    int index = (int) value;
                    if (index >= 0 && index < finalTimestampLabels.size()) {
                        return finalTimestampLabels.get(index);
                    }
                    return "";
                }
            });

            /*
            // Y축 설정 (왼쪽)
            YAxis leftAxis = lineChart.getAxisLeft();
            leftAxis.setDrawGridLines(true);
            leftAxis.setTextSize(10f);
            leftAxis.setTextColor(Color.BLACK);
            leftAxis.setAxisMinimum(0f);
            */

            // Y축 설정 (오른쪽)
            YAxis rightAxis = lineChart.getAxisRight();
            rightAxis.setEnabled(false);

            // 커스텀 렌더러 설정 (서브 그리드 추가)
            ViewPortHandler viewPortHandler = lineChart.getViewPortHandler();
            
            // X축 커스텀 렌더러 설정
            Transformer xTransformer = lineChart.getTransformer(YAxis.AxisDependency.LEFT);
            // Transformer xTransformer = lineChart.getTransformer(xAxis.getAxisDependency());
            CustomXAxisRenderer customXAxisRenderer = new CustomXAxisRenderer(viewPortHandler, xAxis, xTransformer);
            customXAxisRenderer.setSubGridGranularity(0.5f); // 메인 그리드의 절반 간격
            customXAxisRenderer.setSubGridColor(0x99CCCCCC); // 흐린 회색 (30% 투명도, 더 잘 보이도록)
            customXAxisRenderer.setSubGridWidth(1f); // 선 두께 (더 잘 보이도록)
            // 실제 데이터의 X값 범위 설정 (서브 그리드가 전체 범위에 그려지도록)
            customXAxisRenderer.setDataRange(minXValue, maxXValue);
            lineChart.setXAxisRenderer(customXAxisRenderer);
            
            // Y축 커스텀 렌더러 설정
            Transformer yTransformer = lineChart.getTransformer(leftAxis.getAxisDependency());
            CustomYAxisRenderer customYAxisRenderer = new CustomYAxisRenderer(viewPortHandler, leftAxis, yTransformer);
            customYAxisRenderer.setSubGridGranularity(10f); // 서브 그리드 간격 (Y축 값 기준, 필요시 조정)
            customYAxisRenderer.setSubGridColor(0x99CCCCCC); // 흐린 회색 (30% 투명도, 더 잘 보이도록)
            customYAxisRenderer.setSubGridWidth(1f); // 선 두께 (더 잘 보이도록)
            lineChart.setRendererLeftYAxis(customYAxisRenderer);

            // 애니메이션
            lineChart.animateX(1000);

            // 차트 새로고침
            lineChart.invalidate();
        } catch (Exception e) {
            Log.e(TAG, "Error setting up chart", e);
            throw e;
        }
    }
    
    /**
     * 범례 클릭으로 데이터셋 표시/숨김 토글 기능 설정
     */
    private void setupLegendToggle(final LineChart lineChart, 
                                   final LineDataSet temperatureDataSet, 
                                   final LineDataSet wattDataSet) {
        // 차트 터치 이벤트 리스너 설정
        lineChart.setOnChartGestureListener(new OnChartGestureListener() {
            @Override
            public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
            }

            @Override
            public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
            }

            @Override
            public void onChartLongPressed(MotionEvent me) {
            }

            @Override
            public void onChartDoubleTapped(MotionEvent me) {
            }

            @Override
            public void onChartSingleTapped(MotionEvent me) {
                // 범례 영역 클릭 확인
                Legend legend = lineChart.getLegend();
                if (legend != null && legend.isEnabled()) {
                    // 범례 위치 계산 (MPAndroidChart v3.1.0 API 사용)
                    // 범례는 일반적으로 차트 하단에 위치
                    float legendY = lineChart.getHeight() - legend.getYOffset() - legend.getTextSize() * 2;
                    float legendHeight = legend.getTextSize() * 2 + legend.getYEntrySpace() * 2;
                    
                    // 터치 위치가 범례 영역인지 확인
                    if (me.getY() >= legendY && me.getY() <= legendY + legendHeight) {
                        // 범례 항목 클릭 확인 (getEntries()는 배열을 반환)
                        LegendEntry[] entries = legend.getEntries();
                        if (entries != null && entries.length > 0) {
                            // 텍스트 너비 측정을 위한 Paint 객체 생성
                            android.graphics.Paint textPaint = new android.graphics.Paint();
                            textPaint.setTextSize(legend.getTextSize());
                            textPaint.setTypeface(android.graphics.Typeface.DEFAULT);
                            
                            // 범례 항목의 실제 위치를 정확하게 계산
                            float legendX = legend.getXOffset();
                            float currentX = legendX;
                            float formSize = legend.getFormSize(); // 범례 폼(아이콘) 크기
                            float xEntrySpace = legend.getXEntrySpace(); // 범례 항목 간 X 간격
                            
                            for (int i = 0; i < entries.length; i++) {
                                LegendEntry entry = entries[i];
                                String label = entry.label;
                                
                                // 각 범례 항목의 실제 너비 계산
                                // 폼(아이콘) 너비 + 폼과 텍스트 간격 + 텍스트 너비
                                float formWidth = formSize;
                                float formToTextSpace = 5f; // 폼과 텍스트 사이 간격 (일반적으로 5px)
                                float textWidth = textPaint.measureText(label != null ? label : "");
                                float entryWidth = formWidth + formToTextSpace + textWidth;
                                
                                // 현재 항목의 시작 X 위치
                                float entryStartX = currentX;
                                // 현재 항목의 끝 X 위치
                                float entryEndX = currentX + entryWidth;
                                
                                // 터치 위치가 이 항목의 범위 내에 있는지 확인
                                if (me.getX() >= entryStartX && me.getX() <= entryEndX) {
                                    // 범례 항목의 레이블 텍스트로 정확한 데이터셋 찾기
                                    Log.d(TAG, "Legend clicked: index=" + i + ", label=" + label + 
                                          ", x=" + me.getX() + ", entryStartX=" + entryStartX + ", entryEndX=" + entryEndX);
                                    
                                    // 레이블 텍스트로 데이터셋 토글
                                    toggleDataSetByLabel(lineChart, label, temperatureDataSet, wattDataSet);
                                    break;
                                }
                                
                                // 다음 항목의 시작 위치 계산 (현재 항목 너비 + 항목 간 간격)
                                currentX = entryEndX + xEntrySpace;
                            }
                        }
                    }
                }
            }

            @Override
            public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {
            }

            @Override
            public void onChartScale(MotionEvent me, float scaleX, float scaleY) {
            }

            @Override
            public void onChartTranslate(MotionEvent me, float dX, float dY) {
            }
        });
    }
    
    /**
     * 데이터셋 표시/숨김 토글 (인덱스 기반)
     */
    private void toggleDataSet(LineChart lineChart, int index, 
                               LineDataSet temperatureDataSet, 
                               LineDataSet wattDataSet) {
        LineData lineData = lineChart.getData();
        if (lineData == null) return;
        
        // 인덱스에 해당하는 데이터셋 찾기
        if (index == 0 && temperatureDataSet != null) {
            // 온도 데이터셋 토글
            temperatureDataSet.setVisible(!temperatureDataSet.isVisible());
            lineChart.notifyDataSetChanged();
            lineChart.invalidate();
        } else if (index == 1 && wattDataSet != null) {
            // 와트 데이터셋 토글
            wattDataSet.setVisible(!wattDataSet.isVisible());
            lineChart.notifyDataSetChanged();
            lineChart.invalidate();
        }
    }
    
    /**
     * 데이터셋 표시/숨김 토글 (레이블 텍스트 기반 - 더 정확함)
     */
    private void toggleDataSetByLabel(LineChart lineChart, String label,
                                     LineDataSet temperatureDataSet, 
                                     LineDataSet wattDataSet) {
        if (label == null) return;
        
        LineDataSet targetDataSet = null;
        
        // 레이블 텍스트로 데이터셋 찾기
        if (label.contains("온도") && temperatureDataSet != null) {
            targetDataSet = temperatureDataSet;
        } else if (label.contains("소비전력") && wattDataSet != null) {
            targetDataSet = wattDataSet;
        }
        
        if (targetDataSet != null) {
            // 데이터셋 표시/숨김 토글
            targetDataSet.setVisible(!targetDataSet.isVisible());
            lineChart.notifyDataSetChanged();
            lineChart.invalidate();
            Log.d(TAG, "Toggled dataset: " + label + ", visible=" + targetDataSet.isVisible());
        } else {
            Log.w(TAG, "Dataset not found for label: " + label);
        }
    }
    
    /**
     * 키보드 표시
     */
    private void showKeyboard(View view) {
        if (view != null) {
            view.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            }
        }
    }
    
    /**
     * 키보드 숨김
     */
    private void hideKeyboard(View view) {
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
            view.clearFocus();
        }
    }
    
    /**
     * 저장 확인 다이얼로그 표시
     */
    private void showSaveCommentDialog() {
        new AlertDialog.Builder(this)
            .setTitle("저장 확인")
            .setMessage("기록한 특이사항을 저장하시겠습니까?")
            .setPositiveButton("저장", new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    // 저장 로직 실행
                    saveComment();
                    // 편집 모드 종료
                    isTestDetailComment = false;
                    et_test_detail_comment.setEnabled(false);
                    et_test_detail_comment.setFocusable(false);
                    et_test_detail_comment.setFocusableInTouchMode(false);
                    et_test_detail_comment.setBackgroundColor(getBaseContext().getResources().getColor(R.color.yellow_02));
                    ib_test_hisotry_comment.setImageResource(R.drawable.edit_note_24dp_ffffff_fill0_wght400_grad0_opsz24__1_);
                    ib_test_hisotry_comment_delete.setImageResource(R.drawable.delete_24dp_ffffff_fill0_wght400_grad0_opsz24__1_);
                    ib_test_hisotry_comment_delete.setAlpha(.3f);
                    ib_test_hisotry_comment_delete.setEnabled(false);
                    Map<String, String> rowData = new HashMap<String, String>();
                    rowData.put("clm_comment", et_test_detail_comment.getText().toString());
                    // TestData.updateTestHistoryData(getApplicationContext(), rowData);
                    dialog.dismiss();
                }
            })
            .setNegativeButton("취소", new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    // 편집 모드 유지
                    isTestDetailComment = true;
                    et_test_detail_comment.setEnabled(true);
                    et_test_detail_comment.setFocusable(true);
                    et_test_detail_comment.setFocusableInTouchMode(true);
                    et_test_detail_comment.setBackgroundColor(getBaseContext().getResources().getColor(R.color.white));
                    ib_test_hisotry_comment.setImageResource(R.drawable.save_24dp_ffffff_fill0_wght400_grad0_opsz24);
                    ib_test_hisotry_comment_delete.setImageResource(R.drawable.delete_24dp_ffffff_fill0_wght400_grad0_opsz24__1_);
                    ib_test_hisotry_comment_delete.setAlpha(.3f);
                    ib_test_hisotry_comment_delete.setEnabled(false);
                    // 포커스를 다시 주고 키보드 표시
                    et_test_detail_comment.requestFocus();
                    showKeyboard(et_test_detail_comment);
                    dialog.dismiss();
                }
            })
            .setCancelable(false)
            .show();
    }
    
    /**
     * 특이사항 저장
     */
    private void saveComment() {
        if (testHistorySeq == null || testHistorySeq.isEmpty()) {
            Log.e(TAG, "testHistorySeq is null or empty, cannot save comment");
            return;
        }
        
        String comment = et_test_detail_comment.getText().toString();
        
        try {
            itf.com.lms.util.DBHelper helper = 
                new itf.com.lms.util.DBHelper(this, "itf_temperature_table.db", null, 2);
            android.database.sqlite.SQLiteDatabase db = helper.getWritableDatabase();
            
            String sql = "UPDATE tbl_test_history SET clm_comment = ? WHERE clm_test_history_seq = ?";
            db.execSQL(sql, new String[]{comment, testHistorySeq});
            
            db.close();
            helper.close();
            
            Log.i(TAG, "Comment saved successfully for test_history_seq: " + testHistorySeq);
        } catch (Exception e) {
            Log.e(TAG, "Error saving comment: " + e.getMessage(), e);
        }
    }
}

