/*
package itf.com.lms.item;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import itf.com.lms.R;
import itf.com.lms.vo.VoTestItem;

public class ItemAdapterTestItem extends BaseAdapter {

    private static final String TAG = "ItemAdapterTestItem";

    */
/* 리스트뷰 어댑터 *//*

        ArrayList<VoTestItem> items = new ArrayList<VoTestItem>();

        @Override
        public int getCount() {
            return items.size();
        }

        public void addItem(VoTestItem item) {
            items.add(item);
        }

        public void clear() {
            items.clear();
            notifyDataSetChanged();
        }

        public void updateListAdapter() {
            this.notifyDataSetChanged();
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

    static class ViewHolder {
        LinearLayout ll_test_list_item;
        TextView tv_test_item_seq;
        TextView tv_test_item_name;
        TextView tv_test_item_command;
        TextView tv_test_item_value;
        TextView tv_test_item_result;
        TextView tv_test_temperature;
        LinearLayout ll_item_wall;
        TextView tv_test_electric_val;
        TextView tv_test_item_info;
        View vw_custom_divider;
    }

    @Override
    public int getItemViewType(int position){
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        final Context context = viewGroup.getContext();
        final VoTestItem VoTestItem = items.get(position);
        ViewHolder holder = null;

        if(convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.item_test_item_list, viewGroup, false);

            holder = new ViewHolder();
            holder.ll_test_list_item = convertView.findViewById(R.id.ll_test_list_item);
            holder.tv_test_item_seq = convertView.findViewById(R.id.tv_test_item_seq);
            holder.tv_test_item_name = convertView.findViewById(R.id.tv_test_item_name);
            holder.tv_test_item_command = convertView.findViewById(R.id.tv_test_item_command);
            holder.tv_test_item_value = convertView.findViewById(R.id.tv_test_item_value);
            holder.tv_test_item_result = convertView.findViewById(R.id.tv_test_item_result);
            holder.tv_test_temperature = convertView.findViewById(R.id.tv_test_temperature);
            holder.ll_item_wall = convertView.findViewById(R.id.ll_item_wall);
            holder.tv_test_electric_val = convertView.findViewById(R.id.tv_test_electric_val);
            holder.tv_test_item_info = convertView.findViewById(R.id.tv_test_item_info);
            holder.vw_custom_divider = convertView.findViewById(R.id.vw_custom_divider);

            // Store the holder with the view (associate the holder with the view)
            convertView.setTag(holder);
        } else {
//                View view = new View(context);
//                view = convertView;
            holder = (ViewHolder) convertView.getTag();
        }

        holder.tv_test_item_seq.setText(VoTestItem.getTest_item_seq());
        holder.tv_test_item_name.setText(VoTestItem.getTest_item_name());
        holder.tv_test_item_command.setText(VoTestItem.getTest_item_command());
        holder.tv_test_item_value.setText(VoTestItem.getTest_item_value());
        holder.tv_test_item_result.setText(VoTestItem.getTest_item_result());
        holder.tv_test_temperature.setText(VoTestItem.getTest_temperature());
        holder.tv_test_electric_val.setText(VoTestItem.getTest_electric_val());
        holder.tv_test_item_info.setText(VoTestItem.getTest_item_info());

        // VoTestItem.getTest_model_id();

////        if(VoTestItem.getTest_model_id().equals("00000002")) {
//            if(VoTestItem.getTest_item_command().substring(4, 6).equals("00")) {
////            // convertView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0));
////            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.ll_test_list_item.getLayoutParams();
////            params.height = 0;
////            holder.ll_test_list_item.setLayoutParams(params);
////            holder.ll_test_list_item.setBackgroundResource(R.drawable.drawable_border_05);
//                items.remove(position); // Modify the dataset
//                notifyDataSetChanged(); // Notify the adapter of the changes
//            }
////        }

        // Adjust the height of the row dynamically
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) holder.ll_test_list_item.getLayoutParams();

        if(VoTestItem.getTest_model_id().equals("00000002")) {
            if(VoTestItem.getTest_item_command().indexOf("SV")>-1) {
                if(VoTestItem.getTest_item_command().substring(4, 6).equals("00")) {
                    // holder.ll_test_list_item.setBackgroundResource(R.drawable.drawable_border_05);
                    layoutParams.height = 0;
                    holder.vw_custom_divider.setVisibility(View.GONE);
                }
                else {
                    layoutParams.height = 100;
                    holder.vw_custom_divider.setVisibility(View.VISIBLE);
                }
            }
            else {
                layoutParams.height = 100;
                holder.vw_custom_divider.setVisibility(View.VISIBLE);
            }
        }
        else {
            layoutParams.height = 100;
            holder.vw_custom_divider.setVisibility(View.VISIBLE);
        }

        holder.ll_test_list_item.setLayoutParams(layoutParams);

        if(VoTestItem.getTest_finish_yn().equals("Y")) {
            if(VoTestItem.getTest_item_result()!=null) {
                if(VoTestItem.getTest_item_result().equals("NG")) {
//                    Log.i(TAG, "> ItemAdapterTestItem.setTest_item_result:" + "NG");
                    holder.ll_item_wall.setBackgroundColor(androidx.core.content.ContextCompat.getColor(context, R.color.red_06));
                    holder.tv_test_item_result.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.red_01));
                }
                else if(VoTestItem.getTest_item_result().equals("OK")) {
//                    Log.i(TAG, "> ItemAdapterTestItem.setTest_item_result:" + "OK");
                    holder.ll_item_wall.setBackgroundColor(androidx.core.content.ContextCompat.getColor(context, R.color.blue_02));
                    holder.tv_test_item_result.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.blue_01));
                }
                else {
//                    Log.i(TAG, "> ItemAdapterTestItem.setTest_item_result:" + "OK");
                    holder.ll_item_wall.setBackgroundColor(androidx.core.content.ContextCompat.getColor(context, R.color.white));
                    holder.tv_test_item_result.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.black));
                }
            }
        }
        else {
            holder.ll_item_wall.setBackgroundColor(androidx.core.content.ContextCompat.getColor(context, R.color.white));
            holder.tv_test_item_result.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.black));
        }

        //각 아이템 선택 event
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Log.i(TAG, "> " + VoTestItem.getTest_item_command()+" - "+VoTestItem.getTest_item_name()+" 입니당! ");
                // Toast.makeText(context, VoTestItem.getTest_item_seq()+" / "+VoTestItem.getTest_item_name()+" ", Toast.LENGTH_SHORT).show();
            }
        });

        return convertView;  //뷰 객체 반환
    }
}
*/



package itf.com.lms.item;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import itf.com.lms.R;
import itf.com.lms.util.Constants;
import itf.com.lms.vo.VoTestItem;

public class ItemAdapterTestDetailItem extends BaseAdapter {

    private static final String TAG = "ItemAdapterTestItem";

    ArrayList<VoTestItem> items = new ArrayList<VoTestItem>();

    @Override
    public int getCount() {
        return items.size();
    }

    public void addItem(VoTestItem item) {
        items.add(item);
    }

    public void updateListAdapter() {
        this.notifyDataSetChanged();
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

    static class ViewHolder {
        LinearLayout ll_test_list_item;
        TextView tv_test_item_seq;
        TextView tv_test_item_name;
        TextView tv_test_item_command;
        TextView tv_test_item_value;
        TextView tv_test_item_result;
        TextView tv_test_temperature;
        LinearLayout ll_item_wall;
        TextView tv_test_electric_val;
        TextView tv_test_item_info;
        View vw_custom_divider;
    }

    @Override
    public int getItemViewType(int position){
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        final Context context = viewGroup.getContext();
        final VoTestItem VoTestItem = items.get(position);
        ViewHolder holder = null;

        if(convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.item_test_item_list, viewGroup, false);

            holder = new ViewHolder();
            holder.ll_test_list_item = convertView.findViewById(R.id.ll_test_list_item);
            holder.tv_test_item_seq = convertView.findViewById(R.id.tv_test_item_seq);
            holder.tv_test_item_name = convertView.findViewById(R.id.tv_test_item_name);
            holder.tv_test_item_command = convertView.findViewById(R.id.tv_test_item_command);
            holder.tv_test_item_value = convertView.findViewById(R.id.tv_test_item_value);
            holder.tv_test_item_result = convertView.findViewById(R.id.tv_test_item_result);
            holder.tv_test_temperature = convertView.findViewById(R.id.tv_test_temperature);
            holder.ll_item_wall = convertView.findViewById(R.id.ll_item_wall);
            holder.tv_test_electric_val = convertView.findViewById(R.id.tv_test_electric_val);
            holder.tv_test_item_info = convertView.findViewById(R.id.tv_test_item_info);
            holder.vw_custom_divider = convertView.findViewById(R.id.vw_custom_divider);

            // Store the holder with the view (associate the holder with the view)
            convertView.setTag(holder);
        } else {
//                View view = new View(context);
//                view = convertView;
            holder = (ViewHolder) convertView.getTag();
        }

        holder.tv_test_item_seq.setText(VoTestItem.getTest_item_seq());
        holder.tv_test_item_name.setText(VoTestItem.getTest_item_name());
        holder.tv_test_item_command.setText(VoTestItem.getTest_item_command());
        holder.tv_test_item_value.setText(VoTestItem.getTest_item_value());
        holder.tv_test_item_result.setText(VoTestItem.getTest_item_result());
        holder.tv_test_temperature.setText(VoTestItem.getTest_temperature());
        holder.tv_test_electric_val.setText(VoTestItem.getTest_electric_val());

        // System.out.println(">>>>>>>>>>>> VoTestItem.getTest_item_command() " + VoTestItem.getTest_item_command() + " " + VoTestItem.getTest_lower_value() + Constants.Common.LOG_LESS_THAN + VoTestItem.getTest_item_info() + Constants.Common.LOG_LESS_THAN + VoTestItem.getTest_upper_value());

        if(VoTestItem.getTest_item_command().contains(Constants.TestItemCodes.CM0101) || VoTestItem.getTest_item_command().contains(Constants.TestItemCodes.HT0101) ||
                VoTestItem.getTest_item_command().contains(Constants.TestItemCodes.SV0101) || VoTestItem.getTest_item_command().contains(Constants.TestItemCodes.SV0201) ||
                VoTestItem.getTest_item_command().contains(Constants.TestItemCodes.SV0301) || VoTestItem.getTest_item_command().contains(Constants.TestItemCodes.SV0401) ||
                VoTestItem.getTest_item_command().contains(Constants.TestItemCodes.PM0101) ||
                VoTestItem.getTest_item_command().contains(Constants.TestItemCodes.TH0101) || VoTestItem.getTest_item_command().contains(Constants.TestItemCodes.TH0201) ||
                VoTestItem.getTest_item_command().contains(Constants.TestItemCodes.TH0301)) {
            if(VoTestItem.getTest_item_info()==null) {}
            else {
                // holder.tv_test_item_info.setText(VoTestItem.getTest_lower_value() + Constants.Common.LOG_LESS_THAN + VoTestItem.getTest_item_info() + Constants.Common.LOG_LESS_THAN + VoTestItem.getTest_upper_value());
                holder.tv_test_item_info.setText(VoTestItem.getTest_item_info());
            }
        }
        else {
            if(VoTestItem.getTest_item_command().contains(Constants.TestItemCodes.CM0100) || VoTestItem.getTest_item_command().contains(Constants.TestItemCodes.HT0100) ||
                    VoTestItem.getTest_item_command().contains(Constants.TestItemCodes.SV0100) || VoTestItem.getTest_item_command().contains(Constants.TestItemCodes.SV0200) ||
                    VoTestItem.getTest_item_command().contains(Constants.TestItemCodes.SV0300) || VoTestItem.getTest_item_command().contains(Constants.TestItemCodes.SV0400) ||
                    VoTestItem.getTest_item_command().contains(Constants.TestItemCodes.PM0100) || VoTestItem.getTest_item_command().contains(Constants.TestItemCodes.UV0100)) {
                holder.tv_test_item_info.setText(VoTestItem.getTest_item_info());
            }
            else {
                if(VoTestItem.getTest_result_check_value()==null) {}
                else {
                    holder.tv_test_item_info.setText(VoTestItem.getTest_response_value() + Constants.Common.LOGGER_DEVIDER_01 + VoTestItem.getTest_result_check_value());
                }
            }
        }

        // VoTestItem.getTest_model_id();

////        if(VoTestItem.getTest_model_id().equals("00000002")) {
//            if(VoTestItem.getTest_item_command().substring(4, 6).equals("00")) {
////            // convertView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0));
////            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.ll_test_list_item.getLayoutParams();
////            params.height = 0;
////            holder.ll_test_list_item.setLayoutParams(params);
////            holder.ll_test_list_item.setBackgroundResource(R.drawable.drawable_border_05);
//                items.remove(position); // Modify the dataset
//                notifyDataSetChanged(); // Notify the adapter of the changes
//            }
////        }

        // Adjust the height of the row dynamically
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) holder.ll_test_list_item.getLayoutParams();

//        if(VoTestItem.getTest_model_id().equals("00000002")) {
            if(VoTestItem.getTest_item_command().contains("SV") ||
                VoTestItem.getTest_item_command().contains("HT") ||
                VoTestItem.getTest_item_command().contains("LD") ||
                VoTestItem.getTest_item_command().contains("PM") ||
                VoTestItem.getTest_item_command().contains("UV")) {
                if(VoTestItem.getTest_item_command().substring(4, 6).equals("00")) {
                    layoutParams.height = 0;
                    holder.vw_custom_divider.setVisibility(View.GONE);
                }
                else {
                    layoutParams.height = 100;
                    holder.vw_custom_divider.setVisibility(View.VISIBLE);
                }
            }
            else {
                layoutParams.height = 100;
                holder.vw_custom_divider.setVisibility(View.VISIBLE);
            }
//        }
//        else {
//            layoutParams.height = 100;
//            holder.vw_custom_divider.setVisibility(View.VISIBLE);
//        }

        holder.ll_test_list_item.setLayoutParams(layoutParams);

        if(VoTestItem.getTest_finish_yn().equals("Y")) {
            if(VoTestItem.getTest_item_result()!=null) {
                if(VoTestItem.getTest_item_result().equals("NG")) {
//                    Log.i(TAG, "> ItemAdapterTestItem.setTest_item_result:" + "NG");
                    holder.ll_item_wall.setBackgroundColor(androidx.core.content.ContextCompat.getColor(context, R.color.red_06));
                    holder.tv_test_item_result.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.red_01));
                }
                else if(VoTestItem.getTest_item_result().equals("OK")) {
//                    Log.i(TAG, "> ItemAdapterTestItem.setTest_item_result:" + "OK");
                    holder.ll_item_wall.setBackgroundColor(androidx.core.content.ContextCompat.getColor(context, R.color.blue_02));
                    holder.tv_test_item_result.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.blue_01));
                }
                else {
//                    Log.i(TAG, "> ItemAdapterTestItem.setTest_item_result:" + "OK");
                    holder.ll_item_wall.setBackgroundColor(androidx.core.content.ContextCompat.getColor(context, R.color.white));
                    holder.tv_test_item_result.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.black));
                }
            }
        }
        else {
            holder.ll_item_wall.setBackgroundColor(androidx.core.content.ContextCompat.getColor(context, R.color.white));
            holder.tv_test_item_result.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.black));
        }

        //각 아이템 선택 event
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Log.i(TAG, "> " + VoTestItem.getTest_item_command()+" - "+VoTestItem.getTest_item_name()+" 입니다! ");
                // Toast.makeText(context, VoTestItem.getTest_item_seq()+" / "+VoTestItem.getTest_item_name()+" ", Toast.LENGTH_SHORT).show();
            }
        });

        return convertView;  //뷰 객체 반환
    }

    public void clear() {
        items.clear();
        notifyDataSetChanged();
    }
}
