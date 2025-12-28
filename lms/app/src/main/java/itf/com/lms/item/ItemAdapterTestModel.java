package itf.com.lms.item;

import static android.content.Context.MODE_PRIVATE;
import static itf.com.lms.ActivityModelList.activityModelList;
import static itf.com.lms.ActivityModelList.cookie_info;
import static itf.com.lms.ActivityModelList.cookie_preferences;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import itf.com.lms.ActivityModel_0001;
import itf.com.lms.ActivityModel_0002;
import itf.com.lms.ActivityModel_0003;
import itf.com.lms.R;
import itf.com.lms.vo.VoTestModel;

public class ItemAdapterTestModel extends BaseAdapter {

    private static final String TAG = "ItemAdapterTestItem";

    /* 리스트뷰 어댑터 */
        ArrayList<VoTestModel> items = new ArrayList<VoTestModel>();

        @Override
        public int getCount() {
            return items.size();
        }

        public void addItem(VoTestModel item) {
            items.add(item);
        }

        public void updateListAdapter() {
            this.notifyDataSetChanged(); // 그냥 여기서 하자
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup viewGroup) {
            final Context context = viewGroup.getContext();
            final VoTestModel voTestModel = items.get(position);

            if(convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.item_test_model_list, viewGroup, false);

            } else {
                View view = new View(context);
                view = convertView;
            }

//            LinearLayout ll_model_list_item = convertView.findViewById(R.id.ll_model_list_item);
            LinearLayout ll_model_list_item = convertView.findViewById(R.id.ll_model_list_item);
            TextView tv_test_model_no = convertView.findViewById(R.id.tv_test_model_no);
            TextView tv_test_model_name = convertView.findViewById(R.id.tv_test_model_name);
            TextView tv_test_brand_name = convertView.findViewById(R.id.tv_test_brand_name);
            TextView tv_test_model_version = convertView.findViewById(R.id.tv_test_model_version);
            TextView tv_test_model_nationality_name = convertView.findViewById(R.id.tv_test_model_nationality_name);
            TextView tv_test_model_id = convertView.findViewById(R.id.tv_test_model_id);

            tv_test_model_no.setText(voTestModel.getTest_model_no());
            tv_test_model_name.setText(voTestModel.getTest_model_name());
            tv_test_brand_name.setText(voTestModel.getBrand_name());
            tv_test_model_version.setText(voTestModel.getTest_version());
            tv_test_model_nationality_name.setText(voTestModel.getTest_model_nationality_name());
            tv_test_model_id.setText(voTestModel.getTest_model_id());

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Log.i(TAG, "▶ [PS] 모델 선택 " + voTestModel.getTest_model_id() + " " + voTestModel.getTest_model_name());
                    Intent intent = null;

                    for(int i=0; i<getCount(); i++) {
                        // Log.i(TAG, "▶ [PS] 테스트 모델 목록 " + ((VoTestModel) getItem(i)).getTest_model_name() + " / " + voTestModel.getTest_model_name());
                        if(((VoTestModel) getItem(i)).getTest_model_id().equals(voTestModel.getTest_model_id())) {
                            // Log.i(TAG, "▶ [PS] 테스트 모델 목록 선택 " + ((VoTestModel) getItem(i)).getTest_model_name() + " / " + voTestModel.getTest_model_name());
                            try {
                                if(voTestModel.getTest_model_id().equals("00000001")) {
                                    intent = new Intent(activityModelList, ActivityModel_0001.class);
                                }
                                else if(voTestModel.getTest_model_id().equals("00000002")) {
                                    intent = new Intent(activityModelList, ActivityModel_0002.class);
                                }
                                else if(voTestModel.getTest_model_id().equals("00000003")) {
                                    intent = new Intent(activityModelList, ActivityModel_0003.class);
                                }
                            }
                            catch (Exception e) {
                                Log.d(TAG, "▶ [ER].00008 " + e.toString());
                            }
                        }
                    }

                    cookie_preferences = context.getSharedPreferences("test_cookie_info", MODE_PRIVATE);
                    cookie_info = cookie_preferences.edit();

                    cookie_info.putString("test_model_id", voTestModel.getTest_model_id());
                    cookie_info.putString("test_model_name", voTestModel.getTest_model_name());
                    cookie_info.putString("test_model_nation", voTestModel.getTest_model_nationality_name());
                    cookie_info.apply();

                    intent.putExtra("test_model_id", voTestModel.getTest_model_id());
                    intent.putExtra("test_model_name", voTestModel.getTest_model_name());
                    intent.putExtra("test_model_nation", voTestModel.getTest_model_nationality_name());
                    activityModelList.startActivity(intent);
                }
            });

//            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) ll_model_list_item.getLayoutParams();

            convertView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    int x = (int) event.getX();
                    int y = (int) event.getY();
                    int color = 0;

                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        color = androidx.core.content.ContextCompat.getColor(context, R.color.red_06);
                    }
                    if (event.getAction() == MotionEvent.ACTION_MOVE) {
                        color = androidx.core.content.ContextCompat.getColor(context, R.color.red_06);
                    }
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        // color = androidx.core.content.ContextCompat.getColor(context, R.color.white);
                    }
                    ll_model_list_item.setBackgroundColor(color);
                    return false;
                }
            });

            return convertView;
        }
}
