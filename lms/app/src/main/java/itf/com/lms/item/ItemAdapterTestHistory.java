package itf.com.lms.item;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;

import itf.com.lms.ActivityTestHistoryDetail;
import itf.com.lms.R;
import itf.com.lms.vo.VoTestHistory;

public class ItemAdapterTestHistory extends BaseAdapter {

    private static final String TAG = "ItemAdapterTestHistory";

    /* 리스트뷰 어댑터 */
        ArrayList<VoTestHistory> items = new ArrayList<VoTestHistory>();
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

        public void setOnCheckboxStateChangeListener(OnCheckboxStateChangeListener listener) {
            this.checkboxStateChangeListener = listener;
        }

        public void setOnCheckboxVisibilityChangeListener(OnCheckboxVisibilityChangeListener listener) {
            this.checkboxVisibilityChangeListener = listener;
        }

        private boolean hasCheckedItems() {
            for (VoTestHistory item : items) {
                if (item.isChecked()) return true;
            }
            return false;
        }

        private void notifyCheckboxStateChanged() {
            if (checkboxStateChangeListener != null) {
                checkboxStateChangeListener.onCheckboxStateChanged(hasCheckedItems());
            }
        }

        private void notifyCheckboxVisibilityChanged() {
            if (checkboxVisibilityChangeListener != null) {
                checkboxVisibilityChangeListener.onCheckboxVisibilityChanged(checkboxVisibility);
            }
        }

        @Override
        public int getCount() {
            return items.size();
        }

        public void addItem(VoTestHistory item) {
            items.add(item);
        }

        public void updateListAdapter() {
            this.notifyDataSetChanged(); // 그냥 여기서 하자
        }

        /**
         * 체크된 항목들의 리스트를 반환
         */
        public ArrayList<VoTestHistory> getCheckedItems() {
            ArrayList<VoTestHistory> checkedItems = new ArrayList<>();
            for (VoTestHistory item : items) {
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
            for (VoTestHistory item : items) {
                item.setChecked(checked);
            }
            notifyDataSetChanged();
            notifyCheckboxStateChanged();
        }

        /**
         * 노출된 항목들의 리스트를 반환
         */
        public ArrayList<VoTestHistory> getVisibleItems() {
            ArrayList<VoTestHistory> visibleItems = new ArrayList<>();
            for (VoTestHistory item : items) {
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
            notifyCheckboxVisibilityChanged();
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
            final VoTestHistory voItem = items.get(position);

            if(convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.item_test_history_list, viewGroup, false);

            } else {
                View view = new View(context);
                view = convertView;
            }

            TextView tv_test_result = convertView.findViewById(R.id.tv_test_result);
            TextView tv_test_model_name = convertView.findViewById(R.id.tv_test_model_name);
            TextView tv_test_model_id = convertView.findViewById(R.id.tv_test_model_id);
            TextView tv_test_timestamp = convertView.findViewById(R.id.tv_test_timestamp);
            TextView tv_test_history_seq = convertView.findViewById(R.id.tv_test_history_seq);
            TextView tv_test_history_no = convertView.findViewById(R.id.tv_test_history_no);
            TextView tv_test_ok_count = convertView.findViewById(R.id.tv_test_ok_count);
            TextView tv_test_ng_count = convertView.findViewById(R.id.tv_test_ng_count);

            // 체크박스와 토글 스위치 찾기
            CheckBox cbItemCheck = convertView.findViewById(R.id.cb_item_check);
            Switch switchVisibility = convertView.findViewById(R.id.switch_visibility);

            tv_test_ok_count.setText(voItem.getTest_ok_count());
            tv_test_ng_count.setText(voItem.getTest_ng_count());
            tv_test_result.setText(voItem.getTest_result());
            tv_test_model_name.setText(voItem.getTest_model_name());
            tv_test_model_id.setText(voItem.getTest_model_id());
            tv_test_timestamp.setText(voItem.getTest_timestamp());
            tv_test_history_seq.setText(voItem.getTest_history_seq());
            tv_test_history_no.setText(voItem.getTest_history_no());

            // 체크박스 상태 설정
            if (cbItemCheck != null) {
                cbItemCheck.setChecked(voItem.isChecked());
                // 체크박스 visibility 설정
                cbItemCheck.setVisibility(checkboxVisibility ? View.VISIBLE : View.GONE);
                cbItemCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        voItem.setChecked(isChecked);
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
                        // 검사 상세 내역 Activity로 이동
                        Intent intent = new Intent(context, ActivityTestHistoryDetail.class);
                        intent.putExtra("test_history_seq", voItem.getTest_history_seq());
                        intent.putExtra("test_model_name", voItem.getTest_model_name());
                        intent.putExtra("test_timestamp", voItem.getTest_timestamp());
                        intent.putExtra("test_result", voItem.getTest_result());
                        context.startActivity(intent);
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
}
