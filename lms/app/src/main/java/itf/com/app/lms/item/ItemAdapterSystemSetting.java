package itf.com.app.lms.item;

import static android.util.TypedValue.COMPLEX_UNIT_SP;
import static android.view.View.TEXT_ALIGNMENT_CENTER;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.animation.ObjectAnimator;
import android.os.Build;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import itf.com.app.lms.R;
import itf.com.app.lms.util.AppSettings;
import itf.com.app.lms.util.LogManager;
import itf.com.app.lms.util.StringResourceManager;
import itf.com.app.lms.util.TestData;
import itf.com.app.lms.vo.VoSystemSetting;

public class ItemAdapterSystemSetting extends BaseAdapter {

    private static final String TAG = "ItemAdapterSystemSetting";
    private static final String SETTING_ID_DEFAULT_EMAIL_APP = "DEFAULT_EMAIL_APP";
    private static final String SETTING_ID_TEST_EMAIL = "TEST_EMAIL"; // 메일 전달 이메일 주소
    private static final String SETTING_ID_APP_LANGUAGE = "APP_LANGUAGE"; // 애플리케이션 언어

    /* 리스트뷰 어댑터 */
        ArrayList<VoSystemSetting> items = new ArrayList<VoSystemSetting>();
        private boolean checkboxVisibility = false; // 체크박스 visibility 상태 (기본값: visible)
        
        /**
         * 체크박스 상태 변경 리스너 인터페이스
         */
        public interface OnCheckboxStateChangeListener {
            void onCheckboxStateChanged(boolean hasCheckedItems);
        }
        
        /**
         * 체크박스 visibility 상태 변경 리스너 인터페이스
         */
        public interface OnCheckboxVisibilityChangeListener {
            void onCheckboxVisibilityChanged(boolean isVisible);
        }
        
        private OnCheckboxStateChangeListener checkboxStateChangeListener;
        private OnCheckboxVisibilityChangeListener checkboxVisibilityChangeListener;
        
        /**
         * 체크박스 상태 변경 리스너 설정
         */
        public void setOnCheckboxStateChangeListener(OnCheckboxStateChangeListener listener) {
            this.checkboxStateChangeListener = listener;
        }
        
        /**
         * 체크박스 visibility 상태 변경 리스너 설정
         */
        public void setOnCheckboxVisibilityChangeListener(OnCheckboxVisibilityChangeListener listener) {
            this.checkboxVisibilityChangeListener = listener;
        }
        
        /**
         * 체크된 항목이 있는지 확인
         */
        private boolean hasCheckedItems() {
            for (VoSystemSetting item : items) {
                if (item.isChecked()) {
                    return true;
                }
            }
            return false;
        }
        
        /**
         * 체크박스 상태 변경 알림
         */
        private void notifyCheckboxStateChanged() {
            if (checkboxStateChangeListener != null) {
                boolean hasChecked = hasCheckedItems();
                checkboxStateChangeListener.onCheckboxStateChanged(hasChecked);
            }
        }

        @Override
        public int getCount() {
            return items.size();
        }

        public void addItem(VoSystemSetting item) {
            items.add(item);
        }

        public void updateListAdapter() {
            this.notifyDataSetChanged(); // 그냥 여기서 하자
        }

        /**
         * 체크된 항목들의 리스트를 반환
         */
        public ArrayList<VoSystemSetting> getCheckedItems() {
            ArrayList<VoSystemSetting> checkedItems = new ArrayList<>();
            for (VoSystemSetting item : items) {
                if (item.isChecked()) {
                    checkedItems.add(item);
                }
            }
            return checkedItems;
        }

        /**
         * 모든 항목의 체크박스 상태 변경
         */
        public void setAllChecked(boolean checked) {
            for (VoSystemSetting item : items) {
                item.setChecked(checked);
            }
            notifyDataSetChanged();
            notifyCheckboxStateChanged();
        }

        /**
         * 노출된 항목들의 리스트를 반환
         */
        public ArrayList<VoSystemSetting> getVisibleItems() {
            ArrayList<VoSystemSetting> visibleItems = new ArrayList<>();
            for (VoSystemSetting item : items) {
                if (item.isVisible()) {
                    visibleItems.add(item);
                }
            }
            return visibleItems;
        }

        /**
         * 모든 항목의 체크박스 visibility 토글 (GONE/VISIBLE)
         */
        public void toggleAllVisibility() {
            // 체크박스 visibility 상태 토글
            checkboxVisibility = !checkboxVisibility;
            notifyDataSetChanged();
            // visibility 상태 변경 알림
            notifyCheckboxVisibilityChanged();
        }
        
        /**
         * 체크박스 visibility 상태 변경 알림
         */
        private void notifyCheckboxVisibilityChanged() {
            if (checkboxVisibilityChangeListener != null) {
                checkboxVisibilityChangeListener.onCheckboxVisibilityChanged(checkboxVisibility);
            }
        }
        
        /**
         * 현재 체크박스 visibility 상태 반환
         */
        public boolean isCheckboxVisible() {
            return checkboxVisibility;
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public String test_serial_no = "";
        String test_finish_yn = "N";
        int listCnt = 0;

        @Override
        public View getView(int position, View convertView, ViewGroup viewGroup) {
            final Context context = viewGroup.getContext();
            final VoSystemSetting voItem = items.get(position);

            if(convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.item_system_setting_list, viewGroup, false);

            } else {
                View view = new View(context);
                view = convertView;
            }

            // NOTE:
            // 현재 row 레이아웃(`item_system_setting_list`)에는 test-history용 TextView(tv_test_result 등)가 없습니다.
            // 따라서 시스템 설정/히스토리 혼용 화면에서도 크래시가 나지 않도록,
            // 실제 존재하는 View ID(tv_setting_name/tv_setting_value/tv_test_timestamp 등)로 매핑해서 표시합니다.
            TextView tvSettingName = convertView.findViewById(R.id.tv_setting_name);
            TextView tvSettingId = convertView.findViewById(R.id.tv_setting_id);
            TextView tvSettingValue = convertView.findViewById(R.id.tv_setting_value);
            TextView tvComment = convertView.findViewById(R.id.tv_comment);
            TextView tvTestTimestamp = convertView.findViewById(R.id.tv_test_timestamp);
            TextView tvTestModelId = convertView.findViewById(R.id.tv_test_model_id);

            // 체크박스와 토글 스위치 찾기
            CheckBox cbItemCheck = convertView.findViewById(R.id.cb_item_check);
            Switch switchVisibility = convertView.findViewById(R.id.switch_visibility);

            // 표시 값 매핑 (tbl_setting_info 기반)
            if (tvSettingName != null) {
                tvSettingName.setText(voItem.getDisplaySettingName());
            }
            if (tvSettingId != null) {
                tvSettingId.setText(voItem.getSetting_id() != null ? voItem.getSetting_id() : "");
            }
            if (tvTestModelId != null) {
                tvTestModelId.setText(voItem.getSetting_id() != null ? voItem.getSetting_id() : "");
            }
            if (tvSettingValue != null) {
                // DEFAULT_EMAIL_APP: 패키지명을 앱 이름으로 표시 + 아이콘 표시
                if (SETTING_ID_DEFAULT_EMAIL_APP.equals(voItem.getSetting_id())) {
                    String pkg = voItem.getSetting_value() != null ? voItem.getSetting_value().trim() : "";
                    if (!pkg.isEmpty()) {
                        AppDisplayInfo info = resolveAppDisplayInfo(context, pkg);
                        tvSettingValue.setText(info.appName);
                        applyLeftIcon(tvSettingValue, info.icon, 24);
                    } else {
                        tvSettingValue.setText("");
                        tvSettingValue.setCompoundDrawables(null, null, null, null);
                    }
                } else {
                    tvSettingValue.setText(voItem.getSetting_value() != null ? voItem.getSetting_value() : "");
                    // 재사용 row에서 아이콘이 남지 않도록 항상 제거
                    tvSettingValue.setCompoundDrawables(null, null, null, null);
                }
            }
            if (tvComment != null) {
                tvComment.setText(voItem.getComment() != null ? voItem.getComment() : "");
            }
            if (tvTestTimestamp != null) {
                tvTestTimestamp.setText(formatSettingTimestamp(voItem.getTest_timestamp()));
            }

            // 체크박스 상태 설정
            if (cbItemCheck != null) {
                cbItemCheck.setChecked(voItem.isChecked());
                // 체크박스 visibility 설정
                cbItemCheck.setVisibility(checkboxVisibility ? View.VISIBLE : View.GONE);
                cbItemCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        voItem.setChecked(isChecked);
                        // 체크박스 상태 변경 알림
                        notifyCheckboxStateChanged();
                    }
                });
            }

            // 토글 스위치 상태 설정
            if (switchVisibility != null) {
                switchVisibility.setChecked(voItem.isVisible());
                View finalConvertView = convertView;
                switchVisibility.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        voItem.setVisible(isChecked);
                        // 노출 여부에 따라 아이템의 가시성 조정
                        if (isChecked) {
                            finalConvertView.setAlpha(1.0f);
                        } else {
                            finalConvertView.setAlpha(0.5f);
                        }
                    }
                });
                
                // 초기 가시성 상태 반영
                if (voItem.isVisible()) {
                    convertView.setAlpha(1.0f);
                } else {
                    convertView.setAlpha(0.5f);
                }
            }

            // 상세 버튼 클릭 이벤트
            View btnDetail = convertView.findViewById(R.id.btn_detail);
            if (btnDetail != null) {
                btnDetail.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // setting_id가 없으면 저장/편집 불가
                        if (voItem.getSetting_id() == null || voItem.getSetting_id().trim().isEmpty()) {
                            Toast.makeText(context, "setting_id가 비어있어 수정할 수 없습니다.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // '기본 이메일 어플리케이션' 설정은 이메일 앱 선택 다이얼로그 표시
                        if (SETTING_ID_DEFAULT_EMAIL_APP.equals(voItem.getSetting_id())) {
                            showEmailAppSelectionDialog(context, voItem);
                            return;
                        }

                        // '메일 전달 이메일 주소' 설정은 이메일 주소 입력 다이얼로그 표시
                        if (SETTING_ID_TEST_EMAIL.equals(voItem.getSetting_id())) {
                            showTestEmailEditDialog(context, voItem);
                            return;
                        }

                        // '애플리케이션 언어' 설정은 언어 선택 다이얼로그 표시
                        if (SETTING_ID_APP_LANGUAGE.equals(voItem.getSetting_id())) {
                            showLanguageSelectionDialog(context, voItem);
                            return;
                        }

                        // '진동 강도' 설정은 키보드 입력 대신 슬라이더 방식
                        if (AppSettings.SETTING_ID_VIBRATION_AMPLITUDE.equals(voItem.getSetting_id())) {
                            showVibrationAmplitudeSliderDialog(context, voItem);
                            return;
                        }

                        // 그 외 설정: 일반 설정값(clm_setting_value) 수정 다이얼로그
                        showGeneralSettingValueEditDialog(context, voItem);
                    }
                });
            }
            
            //각 아이템 선택 event
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
//                    Toast.makeText(context, VoTestItem.getProcess_no()+" 번 - "+VoTestItem.getProcess_title()+" 입니당! ", Toast.LENGTH_SHORT).show();
                }
            });

            return convertView;  //뷰 객체 반환
        }

        private static class AppDisplayInfo {
            final String appName;
            final Drawable icon;

            AppDisplayInfo(String appName, Drawable icon) {
                this.appName = appName;
                this.icon = icon;
            }
        }

        /**
         * 패키지명 -> 앱 이름 + 아이콘
         */
        private AppDisplayInfo resolveAppDisplayInfo(Context context, String packageName) {
            try {
                PackageManager pm = context.getPackageManager();
                ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
                CharSequence label = pm.getApplicationLabel(ai);
                Drawable icon = pm.getApplicationIcon(ai);
                String name = (label != null) ? label.toString() : packageName;
                return new AppDisplayInfo(name, icon);
            } catch (Exception e) {
                return new AppDisplayInfo(packageName, null);
            }
        }

        /**
         * TextView 왼쪽에 앱 아이콘 붙이기 (dp 단위)
         */
        private void applyLeftIcon(TextView tv, Drawable icon, int sizeDp) {
            if (tv == null) return;
            if (icon == null) {
                tv.setCompoundDrawables(null, null, null, null);
                return;
            }

            int px = (int) (sizeDp * tv.getResources().getDisplayMetrics().density);
            icon.setBounds(0, 0, px, px);
            tv.setCompoundDrawables(icon, null, null, null);
            tv.setCompoundDrawablePadding((int) (8 * tv.getResources().getDisplayMetrics().density));
        }

        /**
         * '설정일시' 표시 포맷: yyyy/MM/dd HH:mm (24h)
         * raw 예시: yyyyMMddHHmmss(14) / yyyyMMddHHmm(12)
         */
        private String formatSettingTimestamp(String raw) {
            if (raw == null) return "";
            String s = raw.trim();
            if (s.isEmpty()) return "";

            try {
                String digits = s.replaceAll("[^0-9]", "");
                SimpleDateFormat out = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());

                if (digits.length() >= 14) {
                    digits = digits.substring(0, 14);
                    SimpleDateFormat in = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
                    Date d = in.parse(digits);
                    if (d != null) return out.format(d);
                } else if (digits.length() >= 12) {
                    digits = digits.substring(0, 12);
                    SimpleDateFormat in = new SimpleDateFormat("yyyyMMddHHmm", Locale.getDefault());
                    Date d = in.parse(digits);
                    if (d != null) return out.format(d);
                }
            } catch (Exception ignored) {
                // fall through
            }

            return s;
        }

        /**
         * 이메일 앱 선택 다이얼로그 (icon + name)
         * - 선택 결과는 tbl_setting_info.clm_setting_value에 packageName으로 저장
         */
        private void showEmailAppSelectionDialog(Context context, VoSystemSetting voItem) {
            try {
                PackageManager pm = context.getPackageManager();
                if (pm == null) {
                    Toast.makeText(context, StringResourceManager.getInstance().getString("ui.message.error.package_manager_unavailable"), Toast.LENGTH_SHORT).show();
                    return;
                }

                String currentPackageName = voItem.getSetting_value() != null ? voItem.getSetting_value().trim() : "";
                List<EmailAppInfo> appList = queryEmailApps(pm, currentPackageName);

                if (appList.isEmpty()) {
                    Toast.makeText(context, StringResourceManager.getInstance().getString("ui.message.error.email_app_not_found"), Toast.LENGTH_SHORT).show();
                    return;
                }

                int selectedIndex = 0;
                for (int i = 0; i < appList.size(); i++) {
                    if (appList.get(i).packageName != null && appList.get(i).packageName.equals(currentPackageName)) {
                        selectedIndex = i;
                        break;
                    }
                }

                final EmailAppListAdapter adapter = new EmailAppListAdapter(context, appList, selectedIndex);
                ListView listView = new ListView(context);
                listView.setAdapter(adapter);
                listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                listView.setItemChecked(selectedIndex, true);

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                View headerView = LayoutInflater.from(context).inflate(R.layout.dialog_consistent_header, null);
                TextView headerTitle = headerView.findViewById(R.id.tv_dialog_header_title);
                if (headerTitle != null) {
                    headerTitle.setText(StringResourceManager.getInstance().getString("ui.message.email_app_selection_title"));
                }
                builder.setCustomTitle(headerView);
                builder.setView(listView);

                builder.setPositiveButton(StringResourceManager.getInstance().getString("ui.message.save"), null);

                builder.setNegativeButton("취소", (dialog, which) -> dialog.dismiss());
                AlertDialog dialog = builder.create();
                dialog.setOnShowListener(d -> {
                    // 다이얼로그 Window에 시스템 UI 숨기기 설정
                    hideSystemUIForDialog(dialog);
                    
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                        int idx = adapter.getSelectedIndex();
                        if (idx < 0 || idx >= appList.size()) {
                            Toast.makeText(context, StringResourceManager.getInstance().getString("ui.message.error.no_app_selected"), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        EmailAppInfo selected = appList.get(idx);
                        if (selected == null || selected.packageName == null || selected.packageName.trim().isEmpty()) {
                            Toast.makeText(context, StringResourceManager.getInstance().getString("ui.message.error.invalid_app_info"), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        StringResourceManager srm = StringResourceManager.getInstance();
                        if (context.getApplicationContext() != null) {
                            srm.initialize(context.getApplicationContext());
                        }
                        showConfirmSaveDialog(
                                context,
                                srm.getString("ui.message.save_confirm"),
                                srm.getString("ui.message.email_app_save_confirm",
                                        (selected.appName != null ? selected.appName : selected.packageName),
                                        selected.packageName),
                                () -> {
                                    saveDefaultEmailApp(context, voItem, selected.packageName.trim());
                                    dialog.dismiss();
                                }
                        );
                    });
                });
                dialog.show();

            } catch (Exception e) {
                Toast.makeText(context, StringResourceManager.getInstance().getString("ui.message.error.loading_email_apps"), Toast.LENGTH_SHORT).show();
            }
        }

        private void saveDefaultEmailApp(Context context, VoSystemSetting voItem, String packageName) {
            try {
                String now = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());

                java.util.Map<String, String> data = new java.util.HashMap<>();
                // seq는 있으면 유지
                if (voItem.getSetting_seq() != null && !voItem.getSetting_seq().trim().isEmpty()) {
                    data.put("clm_setting_seq", voItem.getSetting_seq().trim());
                }
                data.put("clm_setting_id", voItem.getSetting_id() != null ? voItem.getSetting_id() : "");
                data.put("clm_setting_value", packageName);
                data.put("clm_comment", voItem.getComment() != null ? voItem.getComment() : "");
                data.put("clm_test_timestamp", now);

                boolean ok = TestData.insertSettingInfo(context, data);
                if (ok) {
                    voItem.setSetting_value(packageName);
                    voItem.setTest_timestamp(now);
                    notifyDataSetChanged();
                    StringResourceManager srm = StringResourceManager.getInstance();
                    if (context.getApplicationContext() != null) {
                        srm.initialize(context.getApplicationContext());
                    }
                    Toast.makeText(context, srm.getString("ui.message.email_app_saved"), Toast.LENGTH_SHORT).show();
                } else {
                    StringResourceManager srm = StringResourceManager.getInstance();
                    if (context.getApplicationContext() != null) {
                        srm.initialize(context.getApplicationContext());
                    }
                    Toast.makeText(context, srm.getString("ui.message.save_failed"), Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                StringResourceManager srm = StringResourceManager.getInstance();
                if (context.getApplicationContext() != null) {
                    srm.initialize(context.getApplicationContext());
                }
                Toast.makeText(context, srm.getString("ui.message.save_error"), Toast.LENGTH_SHORT).show();
            }
        }

        /**
         * 메일 전달 이메일 주소(TEST_EMAIL) 수정 다이얼로그
         */
        private void showTestEmailEditDialog(Context context, VoSystemSetting voItem) {
            try {
                /*
                final EditText et = new EditText(context);
                et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                et.setHint("예) user@example.com");
                et.setPadding(10, 10, 10, 10);
                et.setTextSize(COMPLEX_UNIT_SP, 48f);
                et.setBackgroundColor(context.getResources().getColor(R.color.gray_04));
                et.setText(voItem.getSetting_value() != null ? voItem.getSetting_value() : "");
                et.setSelection(et.getText() != null ? et.getText().length() : 0);
                */

                final EditText et = new EditText(context);
                et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                et.setHint("ex) user@example.com");
                et.setPadding(dpToPx(context, 10), dpToPx(context, 10), dpToPx(context, 10), dpToPx(context, 10));
                et.setTextSize(COMPLEX_UNIT_SP, 48f);
                et.setBackgroundColor(context.getResources().getColor(R.color.gray_05));
                et.setTextAlignment(TEXT_ALIGNMENT_CENTER);
                et.setText(voItem.getSetting_value() != null ? voItem.getSetting_value() : "");
                et.setTextAlignment(TEXT_ALIGNMENT_CENTER);

                String initialText = voItem.getSetting_value() != null ? voItem.getSetting_value() : "";
                et.setText(initialText);
                et.setSelection(initialText != null ? initialText.length() : 0);

                // clear 버튼 Drawable 준비
                Drawable clearDrawable = null;
                try {
                    clearDrawable = context.getDrawable(android.R.drawable.ic_menu_close_clear_cancel);
                } catch (Exception ignored) {
                    try {
                        clearDrawable = context.getResources().getDrawable(android.R.drawable.ic_menu_close_clear_cancel);
                    } catch (Exception ignored2) { /* ignore */ }
                }
                if (clearDrawable != null) {
                    int sizePx = dpToPx(context, 20);
                    clearDrawable.setBounds(0, 0, sizePx, sizePx);
                    et.setCompoundDrawables(null, null, clearDrawable, null);
                    et.setCompoundDrawablePadding(dpToPx(context, 8));
                    if (et.getText() == null || et.getText().length() == 0) {
                        et.setCompoundDrawables(null, null, null, null);
                    }
                }

                // 텍스트 변화에 따라 clear 버튼 표시/숨김
                Drawable finalClearDrawable = clearDrawable;
                et.addTextChangedListener(new android.text.TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                        if (finalClearDrawable == null) return;
                        if (s == null || s.length() == 0) {
                            et.setCompoundDrawables(null, null, null, null);
                        } else {
                            et.setCompoundDrawables(null, null, finalClearDrawable, null);
                        }
                    }
                    @Override public void afterTextChanged(android.text.Editable s) {}
                });

                // Drawable 클릭으로 텍스트 삭제 처리 및 키보드 노출 시 애니메이션
                AlertDialog[] dialogRef = new AlertDialog[1];  // 다이얼로그 참조를 저장하기 위한 배열
                et.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, android.view.MotionEvent event) {
                        if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                            Drawable[] draws = et.getCompoundDrawables();
                            if (draws != null && draws.length >= 3) {
                                Drawable right = draws[2];
                                if (right != null) {
                                    int x = (int) event.getX();
                                    int width = v.getWidth();
                                    int paddingRight = v.getPaddingRight();
                                    int drawableWidth = right.getBounds().width();
                                    if (x >= width - paddingRight - drawableWidth) {
                                        et.setText("");
                                        et.setCompoundDrawables(null, null, null, null);
                                        return true;
                                    }
                                }
                            }
                        } else if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                            // EditText 터치 시 키보드가 올라올 것을 예상하고 애니메이션 시작
                            if (dialogRef[0] != null) {
                                animateDialogUp(dialogRef[0], "showTestEmailEditDialog");
                            }
                        }
                        return false;
                    }
                });
                
                // 포커스 리스너로도 키보드 노출/숨김 애니메이션 처리
                et.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (dialogRef[0] != null) {
                            if (hasFocus) {
                                // 포커스를 받으면 키보드가 올라올 것을 예상하고 애니메이션 시작
                                Log.d(TAG, "EditText 포커스 획득 - 위로 이동 애니메이션 시작 (showTestEmailEditDialog)");
                                animateDialogUp(dialogRef[0], "showTestEmailEditDialog");
                            } else {
                                // 포커스를 잃으면 키보드가 내려갈 것을 예상하고 원래 위치로 복귀
                                Log.d(TAG, "EditText 포커스 해제 - 원래 위치로 복귀 애니메이션 시작 (showTestEmailEditDialog)");
                                // 약간의 지연 후 실행 (키보드가 완전히 내려간 후)
                                v.postDelayed(() -> animateDialogDown(dialogRef[0], "showTestEmailEditDialog"), 200);
                            }
                        }
                    }
                });
                et.setSelection(et.getText() != null ? et.getText().length() : 0);

                // EditText에 마진을 주려면 부모 컨테이너에 추가하고 LayoutParams로 마진 지정
                LinearLayout container = new LinearLayout(context);
                container.setOrientation(LinearLayout.VERTICAL);
                int leftRightMargin = dpToPx(context, 16);
                int topMargin = dpToPx(context, 12);
                LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                etLp.setMargins(leftRightMargin, topMargin, leftRightMargin, topMargin);
                container.addView(et, etLp);

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                View headerView = LayoutInflater.from(context).inflate(R.layout.dialog_consistent_header, null);
                TextView headerTitle = headerView.findViewById(R.id.tv_dialog_header_title);
                if (headerTitle != null) {
                    headerTitle.setText("메일 전달 이메일 주소 변경");
                }
                builder.setCustomTitle(headerView);
                builder.setView(container);

                builder.setPositiveButton("저장", null);

                builder.setNegativeButton("취소", (dialog, which) -> dialog.dismiss());
                AlertDialog dialog = builder.create();
                dialogRef[0] = dialog;  // 다이얼로그 참조 저장
                
                // 뒤로 가기 버튼 처리: 키보드 숨기기 및 포커스 해제
                dialog.setOnKeyListener((d, keyCode, event) -> {
                    if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
                        // EditText에 포커스가 있는 경우
                        if (et.hasFocus()) {
                            Log.d(TAG, "뒤로 가기 버튼 감지 (ACTION_DOWN) - 키보드 숨기기 및 포커스 해제 (showTestEmailEditDialog)");
                            
                            // 즉시 포커스 해제 (키보드가 사라지기 전에)
                            et.clearFocus();
                            
                            // 키보드 숨기기
                            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                            if (imm != null) {
                                imm.hideSoftInputFromWindow(et.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                            }
                            
                            // 다이얼로그 위치 복귀 (즉시 실행)
                            animateDialogDown(dialog, "showTestEmailEditDialog");
                            
                            // 이벤트 소비하여 다이얼로그가 닫히지 않도록 함
                            return true;
                        }
                    }
                    return false;  // 기본 동작 수행 (다이얼로그 닫기)
                });
                
                dialog.setOnShowListener(d -> {
                    // 다이얼로그 Window에 시스템 UI 숨기기 설정
                    hideSystemUIForDialog(dialog);
                    
                    // 키보드 표시 시 다이얼로그 위로 이동 설정
                    setupDialogWindowForKeyboard(dialog, "showTestEmailEditDialog");
                    
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                        String email = et.getText() != null ? et.getText().toString().trim() : "";
                        StringResourceManager srm = StringResourceManager.getInstance();
                        if (context.getApplicationContext() != null) {
                            srm.initialize(context.getApplicationContext());
                        }
                        if (email.isEmpty()) {
                            Toast.makeText(context, srm.getString("ui.message.error.email_required"), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // 간단한 형식 체크 (강제는 아님)
                        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                            Toast.makeText(context, srm.getString("ui.message.error.email_invalid"), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        showConfirmSaveDialog(
                                context,
                                srm.getString("ui.message.save_confirm"),
                                srm.getString("ui.message.email_address_save_confirm", email),
                                () -> {
                                    saveSettingValue(context, voItem, email);
                                    dialog.dismiss();
                                }
                        );
                    });
                });
                dialog.show();
            } catch (Exception e) {
                Toast.makeText(context, "다이얼로그 표시 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        }

        /**
         * 언어 선택 다이얼로그
         * - 지원하는 언어 목록 표시
         * - 선택한 언어를 tbl_setting_info에 저장
         * - 앱 재시작하여 언어 변경 적용
         */
        private void showLanguageSelectionDialog(Context context, VoSystemSetting voItem) {
            try {
                StringResourceManager stringManager = StringResourceManager.getInstance();
                if (context != null && context.getApplicationContext() != null) {
                    stringManager.initialize(context.getApplicationContext());
                }
                
                String[] languageDisplayNames = stringManager.getLanguageDisplayNames();
                List<String> supportedLanguages = stringManager.getSupportedLanguages();
                String currentLanguage = voItem.getSetting_value() != null ? voItem.getSetting_value().trim() : "ko";
                
                // 현재 선택된 언어 인덱스 찾기
                int selectedIndex = 0;
                for (int i = 0; i < supportedLanguages.size(); i++) {
                    if (supportedLanguages.get(i).equals(currentLanguage)) {
                        selectedIndex = i;
                        break;
                    }
                }
                
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                View headerView = LayoutInflater.from(context).inflate(R.layout.dialog_consistent_header, null);
                TextView headerTitle = headerView.findViewById(R.id.tv_dialog_header_title);
                if (headerTitle != null) {
                    headerTitle.setText("언어 선택 / Select Language");
                }
                builder.setCustomTitle(headerView);
                
                builder.setSingleChoiceItems(languageDisplayNames, selectedIndex, (dialog, which) -> {
                    // 선택은 즉시 반영하지 않고, 확인 버튼에서 처리
                });
                
                builder.setPositiveButton("확인", null);
                builder.setNegativeButton("취소", (dialog, which) -> dialog.dismiss());
                
                AlertDialog dialog = builder.create();
                dialog.setOnShowListener(d -> {
                    // 다이얼로그 Window에 시스템 UI 숨기기 설정
                    hideSystemUIForDialog(dialog);
                    
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                        int selectedPosition = ((android.widget.ListView) dialog.findViewById(android.R.id.list)).getCheckedItemPosition();
                        if (selectedPosition >= 0 && selectedPosition < supportedLanguages.size()) {
                            String selectedLanguageCode = supportedLanguages.get(selectedPosition);
                            
                            // 언어 변경 확인 다이얼로그
                            StringResourceManager srm = StringResourceManager.getInstance();
                            if (context.getApplicationContext() != null) {
                                srm.initialize(context.getApplicationContext());
                            }
                            showConfirmSaveDialog(
                                    context,
                                    srm.getString("ui.message.language_selection"),
                                    srm.getString("ui.message.language_change_confirm", languageDisplayNames[selectedPosition], selectedLanguageCode),
                                    () -> {
                                        // 언어 저장
                                        saveLanguageSetting(context, voItem, selectedLanguageCode);
                                        dialog.dismiss();
                                        
                                        // 앱 재시작
                                        restartApplicationForLanguageChange(context);
                                    }
                            );
                        }
                    });
                });
                
                dialog.show();
            } catch (Exception e) {
                LogManager.e(LogManager.LogCategory.ER, TAG, "Error showing language selection dialog", e);
                StringResourceManager srm = StringResourceManager.getInstance();
                if (context.getApplicationContext() != null) {
                    srm.initialize(context.getApplicationContext());
                }
                Toast.makeText(context, srm.getString("ui.message.error.language_selection_dialog"), Toast.LENGTH_SHORT).show();
            }
        }
        
        /**
         * 언어 설정 저장
         */
        private void saveLanguageSetting(Context context, VoSystemSetting voItem, String languageCode) {
            try {
                StringResourceManager stringManager = StringResourceManager.getInstance();
                stringManager.setLanguage(languageCode);
                
                String now = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());
                
                java.util.Map<String, String> data = new java.util.HashMap<>();
                if (voItem.getSetting_seq() != null && !voItem.getSetting_seq().trim().isEmpty()) {
                    data.put("clm_setting_seq", voItem.getSetting_seq().trim());
                }
                data.put("clm_setting_id", voItem.getSetting_id() != null ? voItem.getSetting_id() : "");
                data.put("clm_setting_value", languageCode);
                data.put("clm_comment", voItem.getComment() != null ? voItem.getComment() : "");
                data.put("clm_test_timestamp", now);
                
                boolean ok = TestData.insertSettingInfo(context, data);
                StringResourceManager srm = StringResourceManager.getInstance();
                if (context.getApplicationContext() != null) {
                    srm.initialize(context.getApplicationContext());
                }
                if (ok) {
                    voItem.setSetting_value(languageCode);
                    voItem.setTest_timestamp(now);
                    notifyDataSetChanged();
                    LogManager.i(LogManager.LogCategory.PS, TAG, "Language setting saved: " + languageCode);
                } else {
                    Toast.makeText(context, srm.getString("ui.message.error.language_save_failed"), Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                LogManager.e(LogManager.LogCategory.ER, TAG, "Error saving language setting", e);
                StringResourceManager srm = StringResourceManager.getInstance();
                if (context.getApplicationContext() != null) {
                    srm.initialize(context.getApplicationContext());
                }
                Toast.makeText(context, srm.getString("ui.message.error.language_save_error"), Toast.LENGTH_SHORT).show();
            }
        }
        
        /**
         * 언어 변경을 위한 앱 재시작
         */
        private void restartApplicationForLanguageChange(Context context) {
            try {
                android.content.Intent intent = context.getPackageManager()
                        .getLaunchIntentForPackage(context.getPackageName());
                if (intent != null) {
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    if (context instanceof android.app.Activity) {
                        ((android.app.Activity) context).finish();
                    }
                    // 프로세스 종료
                    android.os.Process.killProcess(android.os.Process.myPid());
                }
            } catch (Exception e) {
                LogManager.e(LogManager.LogCategory.ER, TAG, "Error restarting application", e);
            }
        }
        
        /**
         * 진동 강도(VIBRATION_AMPLITUDE) 수정 다이얼로그
         * - 키보드 입력이 아닌 슬라이더(SeekBar)로 10 단위 조절
         * - DEFAULT 사용 가능
         */
        private void showVibrationAmplitudeSliderDialog(Context context, VoSystemSetting voItem) {
            try {
                // 10~250, 10단위 (총 25칸: 10,20,...,250)
                final int min = 10;
                final int max = 250;
                final int step = 10;
                final int steps = (max - min) / step; // 24

                // 현재 값 파싱 (DEFAULT면 128로 처리)
                String raw = voItem.getSetting_value() != null ? voItem.getSetting_value().trim() : "";
                boolean isDefault = raw.isEmpty() || "DEFAULT".equalsIgnoreCase(raw);
                int current = 130; // DEFAULT일 때 중간값(128) 사용
                if (!isDefault) {
                    try {
                        // 저장값은 이미 10단위로 정규화되지만, 혹시 모르니 정규화 후 사용
                        String normalized = AppSettings.normalizeVibrationAmplitudeValue(raw);
                        if (!"DEFAULT".equalsIgnoreCase(normalized)) {
                            current = Integer.parseInt(normalized);
                        } else {
                            isDefault = true;
                            current = 130; // DEFAULT면 128로 설정
                        }
                    } catch (Exception ignored) {
                        isDefault = true;
                        current = 130; // DEFAULT면 128로 설정
                    }
                }
                if (current < min) current = min;
                if (current > max) current = max;
                current = ((current + 5) / 10) * 10;

                // content view (코드로 생성)
                LinearLayout root = new LinearLayout(context);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setGravity(android.view.Gravity.CENTER_HORIZONTAL);  // 추가
                int pad = dpToPx(context, 16);
                root.setPadding(pad, pad, pad, pad);

                TextView tvDesc = new TextView(context);
                tvDesc.setTextAlignment(TEXT_ALIGNMENT_CENTER);
                tvDesc.setText("진동 강도를 슬라이드로 조절합니다.\n'DEFAULT 사용' 시 기본 진동값을 사용 합니다.");
                tvDesc.setTextSize(COMPLEX_UNIT_SP, 30f);
                root.addView(tvDesc);

                CheckBox cbDefault = new CheckBox(context);
                cbDefault.setText("DEFAULT 사용");
                cbDefault.setChecked(isDefault);
                cbDefault.setTextSize(COMPLEX_UNIT_SP, 20f);
                cbDefault.setTypeface(null, android.graphics.Typeface.BOLD);  // 폰트 굵게
                LinearLayout.LayoutParams cbLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,  // MATCH_PARENT → WRAP_CONTENT
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                cbLp.gravity = android.view.Gravity.CENTER_HORIZONTAL;
                cbLp.topMargin = dpToPx(context, 12);
                cbDefault.setLayoutParams(cbLp);
                root.addView(cbDefault);

                TextView tvValue = new TextView(context);
                tvValue.setTextSize(COMPLEX_UNIT_SP, 24f);
                // tvValue.setTypeface(null, android.graphics.Typeface.BOLD);  // 폰트 굵게
                tvValue.setText("현재 값: " + current);  // DEFAULT도 128로 표시
                LinearLayout.LayoutParams valLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,  // 변경: MATCH_PARENT → WRAP_CONTENT
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                valLp.gravity = android.view.Gravity.CENTER_HORIZONTAL;  // 추가: 중앙 정렬
                valLp.topMargin = dpToPx(context, 12);
                tvValue.setLayoutParams(valLp);
                root.addView(tvValue);

                SeekBar seekBar = new SeekBar(context);
                seekBar.setMax(steps);
                int initialProgress = (current - min) / step;
                if (initialProgress < 0) initialProgress = 0;
                if (initialProgress > steps) initialProgress = steps;
                seekBar.setProgress(initialProgress);
                seekBar.setEnabled(!isDefault);
                LinearLayout.LayoutParams sbLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                sbLp.topMargin = dpToPx(context, 8);
                seekBar.setLayoutParams(sbLp);
                root.addView(seekBar);

                // 값 표시 갱신
                SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
                    @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                        int v = min + (progress * step);
                        tvValue.setText("현재 값: " + v);
                    }
                    @Override public void onStartTrackingTouch(SeekBar sb) {}
                    @Override public void onStopTrackingTouch(SeekBar sb) {}
                };
                seekBar.setOnSeekBarChangeListener(listener);

                cbDefault.setOnCheckedChangeListener((buttonView, checked) -> {
                    seekBar.setEnabled(!checked);
                    if (checked) {
                        // DEFAULT 사용 체크 시 SeekBar를 중간(130)으로 이동
                        int defaultProgress = (130 - min) / step;
                        if (defaultProgress < 0) defaultProgress = 0;
                        if (defaultProgress > steps) defaultProgress = steps;
                        seekBar.setProgress(defaultProgress);
                        tvValue.setText("현재 값: 130");
                    } else {
                        // 체크 해제 시 현재 SeekBar 값 표시
                        int v = min + (seekBar.getProgress() * step);
                        tvValue.setText("현재 값: " + v);
                    }
                });

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                View headerView = LayoutInflater.from(context).inflate(R.layout.dialog_consistent_header, null);
                TextView headerTitle = headerView.findViewById(R.id.tv_dialog_header_title);
                if (headerTitle != null) {
                    headerTitle.setText("진동 강도 변경");
                }
                builder.setCustomTitle(headerView);
                builder.setView(root);
                builder.setPositiveButton("저장", null);
                builder.setNegativeButton("취소", (dialog, which) -> dialog.dismiss());

                AlertDialog dialog = builder.create();
                dialog.setOnShowListener(d -> {
                    // 다이얼로그 Window에 시스템 UI 숨기기 설정
                    hideSystemUIForDialog(dialog);
                    
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                        String valueToSave;
                        if (cbDefault.isChecked()) {
                            // DEFAULT 사용 체크 시 128로 저장
                            valueToSave = "128";
                        } else {
                            int selected = min + (seekBar.getProgress() * step);
                            valueToSave = String.valueOf(selected);
                        }

                        showConfirmSaveDialog(
                                context,
                                "저장 확인",
                                "진동 강도를 저장하시겠습니까?\n\n" + valueToSave,
                                () -> {
                                    saveSettingValue(context, voItem, valueToSave);
                                    dialog.dismiss();
                                }
                        );
                    });
                });
                dialog.show();

            } catch (Exception e) {
                Toast.makeText(context, "다이얼로그 표시 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        }

        /**
         * 일반 설정값(clm_setting_value) 수정 다이얼로그
         */
        private void showGeneralSettingValueEditDialog(Context context, VoSystemSetting voItem) {
            try {
                /*
                final EditText et = new EditText(context);
                et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                et.setMinLines(1);
                et.setMaxLines(5);
                et.setHint("설정값을 입력해 주세요.");
                et.setText(voItem.getSetting_value() != null ? voItem.getSetting_value() : "");
                et.setSelection(et.getText() != null ? et.getText().length() : 0);

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                View headerView = LayoutInflater.from(context).inflate(R.layout.dialog_consistent_header, null);
                TextView headerTitle = headerView.findViewById(R.id.tv_dialog_header_title);
                if (headerTitle != null) {
                    // 설정명(한글)이 있으면 표시, 없으면 setting_id 표시
                    String name = voItem.getDisplaySettingName();
                    headerTitle.setText(name != null && !name.trim().isEmpty() ? name : "설정값 수정");
                }
                builder.setCustomTitle(headerView);
                builder.setView(et);
                */

                final EditText et = new EditText(context);
                et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                et.setHint("000");
                et.setPadding(dpToPx(context, 10), dpToPx(context, 10), dpToPx(context, 10), dpToPx(context, 10));
                et.setTextSize(COMPLEX_UNIT_SP, 48f);
                et.setBackgroundColor(context.getResources().getColor(R.color.gray_05));
                et.setTextAlignment(TEXT_ALIGNMENT_CENTER);
                et.setText(voItem.getSetting_value() != null ? voItem.getSetting_value() : "");
                et.setTextAlignment(TEXT_ALIGNMENT_CENTER);

                String initialText = voItem.getSetting_value() != null ? voItem.getSetting_value() : "";
                et.setText(initialText);
                et.setSelection(initialText != null ? initialText.length() : 0);

// clear 버튼 Drawable 준비
                Drawable clearDrawable = null;
                try {
                    clearDrawable = context.getDrawable(android.R.drawable.ic_menu_close_clear_cancel);
                } catch (Exception ignored) {
                    try {
                        clearDrawable = context.getResources().getDrawable(android.R.drawable.ic_menu_close_clear_cancel);
                    } catch (Exception ignored2) { /* ignore */ }
                }
                if (clearDrawable != null) {
                    int sizePx = dpToPx(context, 20);
                    clearDrawable.setBounds(0, 0, sizePx, sizePx);
                    et.setCompoundDrawables(null, null, clearDrawable, null);
                    et.setCompoundDrawablePadding(dpToPx(context, 8));
                    if (et.getText() == null || et.getText().length() == 0) {
                        et.setCompoundDrawables(null, null, null, null);
                    }
                }

                // 텍스트 변화에 따라 clear 버튼 표시/숨김
                Drawable finalClearDrawable = clearDrawable;
                et.addTextChangedListener(new android.text.TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                        if (finalClearDrawable == null) return;
                        if (s == null || s.length() == 0) {
                            et.setCompoundDrawables(null, null, null, null);
                        } else {
                            et.setCompoundDrawables(null, null, finalClearDrawable, null);
                        }
                    }
                    @Override public void afterTextChanged(android.text.Editable s) {}
                });

                // Drawable 클릭으로 텍스트 삭제 처리 및 키보드 노출 시 애니메이션
                AlertDialog[] dialogRef2 = new AlertDialog[1];  // 다이얼로그 참조를 저장하기 위한 배열
                et.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, android.view.MotionEvent event) {
                        if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                            Drawable[] draws = et.getCompoundDrawables();
                            if (draws != null && draws.length >= 3) {
                                Drawable right = draws[2];
                                if (right != null) {
                                    int x = (int) event.getX();
                                    int width = v.getWidth();
                                    int paddingRight = v.getPaddingRight();
                                    int drawableWidth = right.getBounds().width();
                                    if (x >= width - paddingRight - drawableWidth) {
                                        et.setText("");
                                        et.setCompoundDrawables(null, null, null, null);
                                        return true;
                                    }
                                }
                            }
                        } else if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                            // EditText 터치 시 키보드가 올라올 것을 예상하고 애니메이션 시작
                            if (dialogRef2[0] != null) {
                                animateDialogUp(dialogRef2[0], "showGeneralSettingValueEditDialog");
                            }
                        }
                        return false;
                    }
                });
                
                // 포커스 리스너로도 키보드 노출/숨김 애니메이션 처리
                et.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (dialogRef2[0] != null) {
                            if (hasFocus) {
                                // 포커스를 받으면 키보드가 올라올 것을 예상하고 애니메이션 시작
                                Log.d(TAG, "EditText 포커스 획득 - 위로 이동 애니메이션 시작 (showGeneralSettingValueEditDialog)");
                                animateDialogUp(dialogRef2[0], "showGeneralSettingValueEditDialog");
                            } else {
                                // 포커스를 잃으면 키보드가 내려갈 것을 예상하고 원래 위치로 복귀
                                Log.d(TAG, "EditText 포커스 해제 - 원래 위치로 복귀 애니메이션 시작 (showGeneralSettingValueEditDialog)");
                                // 약간의 지연 후 실행 (키보드가 완전히 내려간 후)
                                v.postDelayed(() -> animateDialogDown(dialogRef2[0], "showGeneralSettingValueEditDialog"), 200);
                            }
                        }
                    }
                });
                et.setSelection(et.getText() != null ? et.getText().length() : 0);

                // EditText에 마진을 주려면 부모 컨테이너에 추가하고 LayoutParams로 마진 지정
                LinearLayout container = new LinearLayout(context);
                container.setOrientation(LinearLayout.VERTICAL);
                int leftRightMargin = dpToPx(context, 16);
                int topMargin = dpToPx(context, 12);
                LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                etLp.setMargins(leftRightMargin, topMargin, leftRightMargin, topMargin);
                container.addView(et, etLp);

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                View headerView = LayoutInflater.from(context).inflate(R.layout.dialog_consistent_header, null);
                TextView headerTitle = headerView.findViewById(R.id.tv_dialog_header_title);
                if (headerTitle != null) {
                    // 설정명(한글)이 있으면 표시, 없으면 setting_id 표시
                    String name = voItem.getDisplaySettingName();
                    headerTitle.setText(name != null && !name.trim().isEmpty() ? name : "설정값 수정");
                }
                builder.setCustomTitle(headerView);
                builder.setView(container);

                builder.setPositiveButton("저장", null);

                builder.setNegativeButton("취소", (dialog, which) -> dialog.dismiss());
                AlertDialog dialog = builder.create();
                dialogRef2[0] = dialog;  // 다이얼로그 참조 저장
                
                // 뒤로 가기 버튼 처리: 키보드 숨기기 및 포커스 해제
                dialog.setOnKeyListener((d, keyCode, event) -> {
                    if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
                        // EditText에 포커스가 있는 경우
                        if (et.hasFocus()) {
                            Log.d(TAG, "뒤로 가기 버튼 감지 (ACTION_DOWN) - 키보드 숨기기 및 포커스 해제 (showGeneralSettingValueEditDialog)");
                            
                            // 즉시 포커스 해제 (키보드가 사라지기 전에)
                            et.clearFocus();
                            
                            // 키보드 숨기기
                            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                            if (imm != null) {
                                imm.hideSoftInputFromWindow(et.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                            }
                            
                            // 다이얼로그 위치 복귀 (즉시 실행)
                            animateDialogDown(dialog, "showGeneralSettingValueEditDialog");
                            
                            // 이벤트 소비하여 다이얼로그가 닫히지 않도록 함
                            return true;
                        }
                    }
                    return false;  // 기본 동작 수행 (다이얼로그 닫기)
                });
                
                dialog.setOnShowListener(d -> {
                    // 다이얼로그 Window에 시스템 UI 숨기기 설정
                    hideSystemUIForDialog(dialog);
                    
                    // 키보드 표시 시 다이얼로그 위로 이동 설정
                    setupDialogWindowForKeyboard(dialog, "showGeneralSettingValueEditDialog");
                    
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                        String value = et.getText() != null ? et.getText().toString().trim() : "";
                        // 빈 값도 허용할지 정책이 애매하므로 일단 막음
                        StringResourceManager srm = StringResourceManager.getInstance();
                        if (context.getApplicationContext() != null) {
                            srm.initialize(context.getApplicationContext());
                        }
                        if (value.isEmpty()) {
                            Toast.makeText(context, srm.getString("ui.message.error.setting_value_required"), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        showConfirmSaveDialog(
                                context,
                                srm.getString("ui.message.save_confirm"),
                                srm.getString("ui.message.setting_value_save_confirm", value),
                                () -> {
                                    saveSettingValue(context, voItem, value);
                                    dialog.dismiss();
                                }
                        );
                    });
                });
                dialog.show();

            } catch (Exception e) {
                Toast.makeText(context, "다이얼로그 표시 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        }

        /**
         * 다이얼로그의 Window에 시스템 UI 숨기기 설정 (키오스크 모드)
         */
        private void hideSystemUIForDialog(AlertDialog dialog) {
            if (dialog == null) return;
            try {
                Window window = dialog.getWindow();
                if (window == null) return;

                View decorView = window.getDecorView();
                if (decorView == null) return;

                // Android 11 (API 30) 이상
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    WindowInsetsController controller = window.getInsetsController();
                    if (controller != null) {
                        // 상태바와 내비게이션 바 즉시 숨기기
                        controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                        // BEHAVIOR_DEFAULT로 설정하여 스와이프로 나타나지 않도록 함
                        controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_DEFAULT);
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
                
                Log.d(TAG, "hideSystemUIForDialog: 시스템 UI 숨기기 설정 완료");
            } catch (Exception e) {
                Log.w(TAG, "hideSystemUIForDialog: 오류 발생: " + e.getMessage());
            }
        }

        /**
         * 다이얼로그의 Window에 키보드가 올라올 때 위로 이동하도록 설정
         * - dialog.show() 후에 호출해야 함
         * - 키보드 표시/숨김 이벤트를 감지하여 로그 기록
         */
        private void setupDialogWindowForKeyboard(AlertDialog dialog, String dialogName) {
            if (dialog == null) {
                Log.w(TAG, "setupDialogWindowForKeyboard: dialog is null (dialogName=" + dialogName + ")");
                return;
            }
            try {
                Window window = dialog.getWindow();
                if (window == null) {
                    Log.w(TAG, "setupDialogWindowForKeyboard: window is null (dialogName=" + dialogName + ")");
                    return;
                }

                // 키보드가 올라올 때 다이얼로그 크기를 조정하여 위로 이동시킴
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                Log.d(TAG, "setupDialogWindowForKeyboard: SOFT_INPUT_ADJUST_RESIZE 설정 완료 (dialogName=" + dialogName + ")");

                // 키보드 표시/숨김 이벤트 감지를 위한 ViewTreeObserver 설정
                View dialogView = dialog.findViewById(android.R.id.content);
                if (dialogView != null) {
                    ViewTreeObserver observer = dialogView.getViewTreeObserver();
                    if (observer != null && observer.isAlive()) {
                        final int[] previousHeight = {-1};
                        final boolean[] isKeyboardVisible = {false};  // 키보드 표시 상태 추적
                        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                try {
                                    int currentHeight = dialogView.getRootView().getHeight();
                                    if (previousHeight[0] == -1) {
                                        previousHeight[0] = currentHeight;
                                        Log.d(TAG, "setupDialogWindowForKeyboard: 초기 뷰 높이=" + currentHeight + " (dialogName=" + dialogName + ")");
                                    } else if (previousHeight[0] != currentHeight) {
                                        int heightDiff = previousHeight[0] - currentHeight;
                                        Log.d(TAG, "setupDialogWindowForKeyboard: 높이 변화 감지 - 변화량=" + heightDiff + "px, 현재=" + currentHeight + ", 이전=" + previousHeight[0] + " (dialogName=" + dialogName + ")");
                                        
                                        if (heightDiff > 100) {
                                            // 키보드가 표시됨 (높이가 100px 이상 감소)
                                            if (!isKeyboardVisible[0]) {
                                                Log.d(TAG, "setupDialogWindowForKeyboard: 키보드 표시 감지 - 높이 변화=" + heightDiff + "px (dialogName=" + dialogName + ")");
                                                isKeyboardVisible[0] = true;
                                                // 약간의 지연 후 애니메이션 적용 (시스템 조정 후)
                                                dialogView.postDelayed(() -> animateDialogUp(dialog, dialogName), 50);
                                            }
                                        } else if (heightDiff < -100) {
                                            // 키보드가 숨겨짐 (높이가 100px 이상 증가)
                                            if (isKeyboardVisible[0]) {
                                                Log.d(TAG, "setupDialogWindowForKeyboard: 키보드 숨김 감지 - 높이 변화=" + heightDiff + "px (dialogName=" + dialogName + ")");
                                                isKeyboardVisible[0] = false;
                                                // 약간의 지연 후 애니메이션 적용 (시스템 조정 후)
                                                dialogView.postDelayed(() -> animateDialogDown(dialog, dialogName), 50);
                                            }
                                        }
                                        previousHeight[0] = currentHeight;
                                    }
                                } catch (Exception e) {
                                    Log.w(TAG, "setupDialogWindowForKeyboard: onGlobalLayout 오류 (dialogName=" + dialogName + "): " + e.getMessage());
                                }
                            }
                        });
                        Log.d(TAG, "setupDialogWindowForKeyboard: ViewTreeObserver 리스너 등록 완료 (dialogName=" + dialogName + ")");
                    } else {
                        Log.w(TAG, "setupDialogWindowForKeyboard: ViewTreeObserver를 가져올 수 없음 (dialogName=" + dialogName + ")");
                    }
                } else {
                    Log.w(TAG, "setupDialogWindowForKeyboard: dialogView를 찾을 수 없음 (dialogName=" + dialogName + ")");
                }
            } catch (Exception e) {
                Log.e(TAG, "setupDialogWindowForKeyboard: 오류 발생 (dialogName=" + dialogName + "): " + e.getMessage(), e);
            }
        }

        /**
         * 다이얼로그를 위로 이동시키는 애니메이션 (키보드 노출 시)
         * - 항상 고정된 위치(-50dp)로 이동하여 누적 방지
         */
        private void animateDialogUp(AlertDialog dialog, String dialogName) {
            if (dialog == null) {
                Log.w(TAG, "animateDialogUp: dialog is null (dialogName=" + dialogName + ")");
                return;
            }
            try {
                Window window = dialog.getWindow();
                if (window == null) {
                    Log.w(TAG, "animateDialogUp: window is null (dialogName=" + dialogName + ")");
                    return;
                }

                View dialogView = window.getDecorView();
                if (dialogView == null) {
                    Log.w(TAG, "animateDialogUp: dialogView is null (dialogName=" + dialogName + ")");
                    return;
                }

                // 현재 Y 위치
                float currentY = dialogView.getTranslationY();
                // 위로 이동할 고정 거리 (키보드 높이의 약 1/3 정도)
                float targetY = -dpToPx(dialogView.getContext(), 50);
                
                // 이미 목표 위치에 있거나 더 위에 있으면 애니메이션 생략
                if (currentY <= targetY) {
                    Log.d(TAG, "animateDialogUp: 이미 목표 위치에 있음 - currentY=" + currentY + ", targetY=" + targetY + " (dialogName=" + dialogName + ")");
                    return;
                }

                // 기존 애니메이션 취소 (중복 방지)
                dialogView.clearAnimation();

                // 항상 고정된 위치로 이동 (누적 방지)
                ObjectAnimator animator = ObjectAnimator.ofFloat(dialogView, "translationY", currentY, targetY);
                animator.setDuration(300);  // 300ms 애니메이션
                animator.setInterpolator(new DecelerateInterpolator());  // 감속 인터폴레이터
                
                // 애니메이션 완료 후 확실히 목표 위치로 설정
                animator.addListener(new android.animation.AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(android.animation.Animator animation) {
                        dialogView.setTranslationY(targetY);
                        Log.d(TAG, "animateDialogUp: 애니메이션 완료 - translationY=" + targetY + "으로 설정 (dialogName=" + dialogName + ")");
                    }
                });
                
                animator.start();

                Log.d(TAG, "animateDialogUp: 애니메이션 시작 - currentY=" + currentY + " -> targetY=" + targetY + " (dialogName=" + dialogName + ")");
            } catch (Exception e) {
                Log.e(TAG, "animateDialogUp: 오류 발생 (dialogName=" + dialogName + "): " + e.getMessage(), e);
            }
        }

        /**
         * 다이얼로그를 원래 위치로 복귀시키는 애니메이션 (키보드 숨김 시)
         */
        private void animateDialogDown(AlertDialog dialog, String dialogName) {
            if (dialog == null) {
                Log.w(TAG, "animateDialogDown: dialog is null (dialogName=" + dialogName + ")");
                return;
            }
            try {
                Window window = dialog.getWindow();
                if (window == null) {
                    Log.w(TAG, "animateDialogDown: window is null (dialogName=" + dialogName + ")");
                    return;
                }

                View dialogView = window.getDecorView();
                if (dialogView == null) {
                    Log.w(TAG, "animateDialogDown: dialogView is null (dialogName=" + dialogName + ")");
                    return;
                }

                // 현재 Y 위치
                float currentY = dialogView.getTranslationY();
                Log.d(TAG, "animateDialogDown: 현재 translationY=" + currentY + " (dialogName=" + dialogName + ")");
                
                // translationY가 0에 가까우면 애니메이션 불필요
                if (Math.abs(currentY) < 1.0f) {
                    Log.d(TAG, "animateDialogDown: 이미 원래 위치에 있음, 애니메이션 생략 (dialogName=" + dialogName + ")");
                    // 그래도 강제로 0으로 설정 (혹시 모를 오차 방지)
                    dialogView.setTranslationY(0f);
                    return;
                }

                // 기존 애니메이션 취소 (중복 방지)
                dialogView.clearAnimation();
                
                // 원래 위치로 복귀 (translationY를 0으로)
                ObjectAnimator animator = ObjectAnimator.ofFloat(dialogView, "translationY", currentY, 0f);
                animator.setDuration(300);  // 300ms 애니메이션
                animator.setInterpolator(new DecelerateInterpolator());  // 감속 인터폴레이터
                
                // 애니메이션 완료 후 확실히 0으로 설정
                animator.addListener(new android.animation.AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(android.animation.Animator animation) {
                        dialogView.setTranslationY(0f);
                        Log.d(TAG, "animateDialogDown: 애니메이션 완료 - translationY=0으로 설정 (dialogName=" + dialogName + ")");
                    }
                });
                
                animator.start();

                Log.d(TAG, "animateDialogDown: 애니메이션 시작 - 원래 위치로 복귀 (currentY=" + currentY + " -> 0, dialogName=" + dialogName + ")");
            } catch (Exception e) {
                Log.e(TAG, "animateDialogDown: 오류 발생 (dialogName=" + dialogName + "): " + e.getMessage(), e);
                // 오류 발생 시에도 강제로 0으로 설정
                try {
                    Window window = dialog.getWindow();
                    if (window != null) {
                        View dialogView = window.getDecorView();
                        if (dialogView != null) {
                            dialogView.setTranslationY(0f);
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        /**
         * 저장 전 확인 다이얼로그
         */
        private void showConfirmSaveDialog(Context context, String title, String message, Runnable onConfirm) {
            try {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);

                View headerView = LayoutInflater.from(context).inflate(R.layout.dialog_consistent_header, null);
                TextView headerTitle = headerView.findViewById(R.id.tv_dialog_header_title);
                StringResourceManager srm = StringResourceManager.getInstance();
                if (context.getApplicationContext() != null) {
                    srm.initialize(context.getApplicationContext());
                }
                if (headerTitle != null) {
                    headerTitle.setText(title != null ? title : srm.getString("ui.message.save_confirm"));
                }
                builder.setCustomTitle(headerView);

                builder.setMessage(message != null ? message : "");
                builder.setPositiveButton(srm.getString("ui.message.confirm"), (d, w) -> {
                    if (onConfirm != null) {
                        onConfirm.run();
                    }
                    d.dismiss();
                });
                builder.setNegativeButton("취소", (d, w) -> d.dismiss());
                AlertDialog confirmDialog = builder.create();
                confirmDialog.setOnShowListener(d -> {
                    // 다이얼로그 Window에 시스템 UI 숨기기 설정
                    hideSystemUIForDialog(confirmDialog);
                });
                confirmDialog.show();
            } catch (Exception e) {
                // 확인 다이얼로그가 실패하더라도 저장은 하지 않음
                Toast.makeText(context, "확인 다이얼로그 표시 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        }

        /**
         * 공통: 설정값 저장 (tbl_setting_info upsert)
         */
        private void saveSettingValue(Context context, VoSystemSetting voItem, String newValue) {
            try {
                String now = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());

                String valueToSave = newValue;
                // 진동 강도 설정은 10 단위로 정규화해서 저장
                if (AppSettings.SETTING_ID_VIBRATION_AMPLITUDE.equals(voItem.getSetting_id())) {
                    valueToSave = AppSettings.normalizeVibrationAmplitudeValue(newValue);
                }

                java.util.Map<String, String> data = new java.util.HashMap<>();
                if (voItem.getSetting_seq() != null && !voItem.getSetting_seq().trim().isEmpty()) {
                    data.put("clm_setting_seq", voItem.getSetting_seq().trim());
                }
                data.put("clm_setting_id", voItem.getSetting_id() != null ? voItem.getSetting_id() : "");
                data.put("clm_setting_value", valueToSave != null ? valueToSave : "");
                data.put("clm_comment", voItem.getComment() != null ? voItem.getComment() : "");
                data.put("clm_test_timestamp", now);

                boolean ok = TestData.insertSettingInfo(context, data);
                if (ok) {
                    voItem.setSetting_value(valueToSave);
                    voItem.setTest_timestamp(now);
                    notifyDataSetChanged();
                    Toast.makeText(context, "설정값이 저장되었습니다.", Toast.LENGTH_SHORT).show();

                    // 즉시 반영: 진동 강도 설정 변경 시 앱 캐시 업데이트
                    if (AppSettings.SETTING_ID_VIBRATION_AMPLITUDE.equals(voItem.getSetting_id())) {
                        AppSettings.setVibrationAmplitudeFromString(valueToSave);
                    }
                } else {
                    Toast.makeText(context, "저장에 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(context, "저장 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        }

        private int dpToPx(Context context, int dp) {
            if (context == null) return dp;
            return (int) (dp * context.getResources().getDisplayMetrics().density);
        }

        /**
         * 이메일 전송 가능한 앱 목록 조회 (중복 제거)
         * - Gmail이 queryIntentActivities에 안 잡히는 기기/상황이 있어 현재 설정된 패키지는 resolveActivity로 보강
         */
        private List<EmailAppInfo> queryEmailApps(PackageManager pm, String currentPackageName) {
            Set<String> seen = new HashSet<>();
            List<EmailAppInfo> result = new ArrayList<>();

            // 후보 Intent들 (기기별/앱별로 매칭이 달라서 복수로 조회)
            List<Intent> intents = new ArrayList<>();
            Intent i1 = new Intent(Intent.ACTION_SEND);
            i1.setType("text/plain");
            intents.add(i1);

            Intent i2 = new Intent(Intent.ACTION_SEND);
            i2.setType("message/rfc822");
            intents.add(i2);

            Intent i3 = new Intent(Intent.ACTION_SEND);
            i3.setType("text/csv");
            intents.add(i3);

            for (Intent intent : intents) {
                List<ResolveInfo> infos = pm.queryIntentActivities(intent, 0);
                if (infos == null) continue;
                for (ResolveInfo ri : infos) {
                    if (ri == null || ri.activityInfo == null) continue;
                    String pkg = ri.activityInfo.packageName;
                    if (pkg == null || pkg.trim().isEmpty()) continue;
                    if (seen.contains(pkg)) continue;
                    seen.add(pkg);

                    EmailAppInfo app = buildEmailAppInfo(pm, pkg);
                    if (app != null) result.add(app);
                }
            }

            // 보강: 현재 설정된 패키지가 목록에 없지만 실제로는 작동하는 경우가 있어 resolveActivity로 확인 후 추가
            if (currentPackageName != null && !currentPackageName.trim().isEmpty() && !seen.contains(currentPackageName)) {
                try {
                    Intent testIntent = new Intent(Intent.ACTION_SEND);
                    testIntent.setType("text/plain");
                    testIntent.setPackage(currentPackageName);
                    if (pm.resolveActivity(testIntent, 0) != null) {
                        EmailAppInfo app = buildEmailAppInfo(pm, currentPackageName);
                        if (app != null) {
                            seen.add(currentPackageName);
                            result.add(0, app); // 상단에 노출
                        }
                    }
                } catch (Exception ignored) {
                    // ignore
                }
            }

            // 선택 표시
            for (EmailAppInfo app : result) {
                app.isSelected = (app.packageName != null && app.packageName.equals(currentPackageName));
            }

            return result;
        }

        private EmailAppInfo buildEmailAppInfo(PackageManager pm, String packageName) {
            try {
                ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
                CharSequence label = pm.getApplicationLabel(ai);
                Drawable icon = pm.getApplicationIcon(ai);

                EmailAppInfo app = new EmailAppInfo();
                app.packageName = packageName;
                app.appName = (label != null) ? label.toString() : packageName;
                app.icon = icon;
                return app;
            } catch (Exception e) {
                EmailAppInfo app = new EmailAppInfo();
                app.packageName = packageName;
                app.appName = packageName;
                app.icon = null;
                return app;
            }
        }

        private static class EmailAppInfo {
            String packageName;
            String appName;
            Drawable icon;
            boolean isSelected;
        }

        private static class EmailAppListAdapter extends BaseAdapter {
            private final Context context;
            private final List<EmailAppInfo> list;
            private int selectedIndex;

            EmailAppListAdapter(Context context, List<EmailAppInfo> list, int selectedIndex) {
                this.context = context;
                this.list = list;
                this.selectedIndex = selectedIndex;
            }

            @Override
            public int getCount() {
                return list != null ? list.size() : 0;
            }

            @Override
            public Object getItem(int position) {
                return list != null ? list.get(position) : null;
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            int getSelectedIndex() {
                return selectedIndex;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ViewHolder holder;
                if (convertView == null) {
                    convertView = LayoutInflater.from(context).inflate(R.layout.item_email_app_selection, parent, false);
                    holder = new ViewHolder();
                    holder.ivIcon = convertView.findViewById(R.id.iv_app_icon);
                    holder.tvName = convertView.findViewById(R.id.tv_app_name);
                    holder.rb = convertView.findViewById(R.id.rb_selected);
                    convertView.setTag(holder);
                } else {
                    holder = (ViewHolder) convertView.getTag();
                }

                EmailAppInfo app = list.get(position);
                if (holder.ivIcon != null) {
                    holder.ivIcon.setImageDrawable(app.icon);
                }
                if (holder.tvName != null) {
                    holder.tvName.setText(app.appName != null ? app.appName : app.packageName);
                }
                if (holder.rb != null) {
                    holder.rb.setChecked(position == selectedIndex);
                }

                convertView.setOnClickListener(v -> {
                    selectedIndex = position;
                    notifyDataSetChanged();
                });

                return convertView;
            }

            static class ViewHolder {
                ImageView ivIcon;
                TextView tvName;
                RadioButton rb;
            }
        }
}
