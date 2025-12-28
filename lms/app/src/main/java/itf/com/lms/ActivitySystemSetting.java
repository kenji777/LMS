package itf.com.lms;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
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

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import itf.com.lms.item.ItemAdapterSystemSetting;
import itf.com.lms.kiosk.BaseKioskActivity;
import itf.com.lms.util.TestData;
import itf.com.lms.vo.VoSystemSetting;
import itf.com.lms.R;

public class ActivitySystemSetting extends BaseKioskActivity implements View.OnClickListener {

    private static final String TAG = "ActivityTestHistoryList";
    TextView btn_activity_quit = null;

    TextView txtText;
    private ItemAdapterSystemSetting listItemAdapter = null;
    private ListView itemList = null;
    private ConstraintLayout ll_popup_header = null;
    private TextView tv_popup_header_title = null;
    private Context mContext = null;
    private ImageButton ib_header_action = null;
    private FloatingActionButton fab_close = null;
//    private FloatingActionButton fab_toggle_checkbox = null;
//    private FloatingActionButton fab_info_list_mail = null;
    private boolean allChecked = false; // 전체 선택 상태 추적
    CheckBox cb_item_check_all = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //타이틀바 없애기
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_system_setting);

        mContext = this;

        //UI 객체생성
        txtText = findViewById(R.id.tv_param_from_parent);
        ll_popup_header = findViewById(R.id.ll_popup_header);
        btn_activity_quit = findViewById(R.id.btn_activity_quit);
        tv_popup_header_title = findViewById(R.id.tv_popup_header_title);
        ib_header_action = findViewById(R.id.ib_header_action);

        fab_close = findViewById(R.id.fab_close);
        // fab_toggle_checkbox = findViewById(R.id.fab_toggle_checkbox);
        // fab_info_list_mail = findViewById(R.id.fab_info_list_mail);
        cb_item_check_all = findViewById(R.id.cb_item_check_all);

        fab_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                restartApplication(getApplicationContext());
            }
        });

        ib_header_action.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.fromParts("package", getPackageName(), null));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });

        /*
        // visibility 토글 FAB 클릭 이벤트
        fab_toggle_checkbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // cb_item_check_all.toggle(); // 체크박스 토글
                toggleAllVisibility();
            }
        });
        */

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

        listItemAdapter = new ItemAdapterSystemSetting();
        itemList = findViewById(R.id.lv_process_result_list);
        
        // 체크박스 상태 변경 리스너 설정
        listItemAdapter.setOnCheckboxStateChangeListener(new ItemAdapterSystemSetting.OnCheckboxStateChangeListener() {
            @Override
            public void onCheckboxStateChanged(boolean hasCheckedItems) {
                updateMailButtonColor(hasCheckedItems);
            }
        });
        
        // 체크박스 visibility 상태 변경 리스너 설정
        listItemAdapter.setOnCheckboxVisibilityChangeListener(new ItemAdapterSystemSetting.OnCheckboxVisibilityChangeListener() {
            @Override
            public void onCheckboxVisibilityChanged(boolean isVisible) {
                updateToggleCheckboxButtonColor(isVisible);
            }
        });

        // tbl_setting_info에서 시스템 설정 목록 로드
        List<Map<String, String>> lstSettingInfo = TestData.selectSettingInfo(this);
        Map<String, String> mapListItem = null;
        for (int i = 0; i < lstSettingInfo.size(); i++) {
            mapListItem = lstSettingInfo.get(i);
            Log.i(TAG, ">>> setting[" + i + "] " + mapListItem);
                listItemAdapter.addItem(new VoSystemSetting(mapListItem));
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
        if (listItemAdapter != null) {
            boolean isVisible = listItemAdapter.isCheckboxVisible();
            updateToggleCheckboxButtonColor(isVisible);
        } else {
            // 기본값: 비활성화 상태 (blue_for_ovio)
            updateToggleCheckboxButtonColor(false);
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
            ActivitySystemSetting.this.finish();
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
    public ArrayList<VoSystemSetting> getCheckedItems() {
        if (listItemAdapter != null) {
            return listItemAdapter.getCheckedItems();
        }
        return new ArrayList<>();
    }

    /**
     * 노출된 항목들의 리스트를 반환
     */
    public ArrayList<VoSystemSetting> getVisibleItems() {
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
     * @param hasCheckedItems 체크된 항목이 있는지 여부
     */
    private void updateMailButtonColor(boolean hasCheckedItems) {
        /*
        if (fab_info_list_mail != null) {
            if (hasCheckedItems) {
                // 하나라도 선택되면 blue_for_ovio
                fab_info_list_mail.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.blue_for_ovio)
                );
                fab_info_list_mail.setEnabled(true);
            } else {
                // 하나도 선택되지 않으면 gray_04
                fab_info_list_mail.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.gray_04)
                );
                fab_info_list_mail.setEnabled(false);
            }
        }
        */
    }
    
    /**
     * 체크박스 토글 버튼의 배경색을 체크박스 visibility 상태에 따라 업데이트
     * @param isVisible 체크박스가 활성화(visible)되어 있는지 여부
     */
    private void updateToggleCheckboxButtonColor(boolean isVisible) {
        /*
        if (fab_toggle_checkbox != null) {
            if (isVisible) {
                // 체크박스가 활성화되어 있을 때: gray_04
                fab_toggle_checkbox.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.gray_04)
                );
            } else {
                // 체크박스가 비활성화되어 있을 때: blue_for_ovio
                fab_toggle_checkbox.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.blue_for_ovio)
                );
            }
        }
        */
    }
}