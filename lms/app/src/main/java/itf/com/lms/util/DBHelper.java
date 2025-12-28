package itf.com.lms.util;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {
    public DBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.disableWriteAheadLogging();
    }

    // DBHelper 생성자
    public DBHelper(Context context, String name, int version) {
        super(context, name, null, version);
    }

    String sql = "";

    @Override
    public void onCreate(SQLiteDatabase db) {
//        sql = "DROP TABLE if exists tbl_product_temperature;";
//        db.execSQL(sql);
//        sql = "";
//
//        sql = "DROP TABLE if exists tbl_product_watt;";
//        db.execSQL(sql);
//        sql = "";
//
//        sql = "DROP TABLE if exists tbl_hot_temperature;";
//        db.execSQL(sql);
//        sql = "";
//
//        sql = "DROP TABLE if exists tbl_cold_temperature;";
//        db.execSQL(sql);
//        sql = "";
//
//        sql = "DROP TABLE if exists tbl_line_info;";
//        db.execSQL(sql);
//        sql = "";

        sql = "CREATE TABLE if not exists tbl_line_info ("
                + "clm_line_unit_id,"
                + "clm_line_id,"
                + "clm_unit_id,"
                + "clm_comment text);";
        Log.i(TAG, "> sql : " + sql);

        db.execSQL(sql);
        sql = "";

        sql = "CREATE TABLE if not exists tbl_product_temperature ("
                + "clm_line,"
                + "clm_unit_id text,"
                + "clm_timestamp text,"
                + "clm_product_temperature text,"
                + "clm_comment text);";
        Log.i(TAG, "> sql : " + sql);

        db.execSQL(sql);
        sql = "";

        sql = "CREATE TABLE if not exists tbl_product_watt ("
                + "clm_line,"
                + "clm_unit_id text,"
                + "clm_timestamp text,"
                + "clm_product_watt text,"
                + "clm_product_ampere text,"
                + "clm_comment text);";
        Log.i(TAG, "> sql : " + sql);

        db.execSQL(sql);
        sql = "";

        sql = "CREATE TABLE if not exists tbl_hot_temperature ("
                + "clm_temp_seq,"
                + "clm_temperature text,"
                + "clm_regist text,"
                + "clm_voltage text,"
                + "clm_10_bit text,"
                + "clm_12_bit text,"
                + "clm_comment text);";
        Log.i(TAG, "> sql : " + sql);

        db.execSQL(sql);
        sql = "";

        sql = "CREATE TABLE if not exists tbl_cold_temperature ("
                + "clm_temp_seq,"
                + "clm_temperature text,"
                + "clm_regist text,"
                + "clm_voltage text,"
                + "clm_10_bit text,"
                + "clm_12_bit text,"
                + "clm_comment text);";
        Log.i(TAG, "> sql : " + sql);

        db.execSQL(sql);
        sql = "";

        sql = "CREATE TABLE if not exists tbl_test_history_linear_data ("
                + "clm_test_history_seq,"
                + "clm_test_item_id text,"
                + "clm_test_timestamp text,"
                + "clm_temperature text,"
                + "clm_watt text,"
                + "clm_comment text);";
        Log.i(TAG, "> sql : " + sql);

        db.execSQL(sql);
        sql = "";

        sql = "CREATE TABLE if not exists tbl_test_history ("
                + "clm_test_history_seq,"
                + "clm_test_model_id text,"
                + "clm_test_model_name text,"
                + "clm_test_model_nationality text,"
                + "clm_test_timestamp text,"
                + "clm_test_result text,"
                + "clm_test_ng_count text,"
                + "clm_test_ok_count text,"
                + "clm_comment text);";
        Log.i(TAG, "> sql : " + sql);

        db.execSQL(sql);
        sql = "";

        sql = "CREATE TABLE if not exists tbl_test_history_detail ("
                + "clm_test_history_detail_seq,"
                + "clm_test_history_seq text,"
                + "clm_test_item_seq text,"
                + "clm_test_item_name text,"
                + "clm_test_item_command text,"
                + "clm_test_item_result text,"
                + "clm_test_item_value text,"
                + "clm_test_response_value text,"
                + "clm_test_result_value text,"
                + "clm_test_temperature text,"
                + "clm_test_electric_val text,"
                + "clm_comment text);";
        Log.i(TAG, "> sql : " + sql);

        db.execSQL(sql);
        sql = "";

        sql = "CREATE TABLE if not exists tbl_test_spec_data ("
                + "clm_test_seq primary key,"
                + "clm_test_command text,"
                + "clm_test_name text,"
                + "clm_test_type text,"
                + "clm_test_response_value text,"
                + "clm_test_upper_value text,"
                + "clm_test_lower_value text,"
                + "clm_test_upper_value_02 text,"
                + "clm_test_lower_value_02 text,"
                + "clm_comment text,"
                + "clm_value_watt text,"
                + "clm_value text,"
                + "clm_test_step text,"
                + "clm_test_id text,"
                + "clm_test_sec text,"
                + "clm_product_serial_no text,"
                + "clm_lower_value_watt text,"
                + "clm_upper_value_watt text,"
                + "clm_test_version_id text,"
                + "clm_model_id text);";
        Log.i(TAG, "> sql : " + sql);

        db.execSQL(sql);
        sql = "";


        /*
        mapTestWatt.put("line", lineId);
        mapTestWatt.put("unit_id", unitId);
        mapTestWatt.put("timestamp", currentTimestamp);
        mapTestWatt.put("ellapsed_time_cnt", String.valueOf(testItemTimeCnt));
        mapTestWatt.put("watt", String.valueOf(wattValue));
        */

        sql = "CREATE TABLE if not exists tbl_test_watt_data ("
                + "clm_test_seq,"
                + "clm_line_id text,"
                + "clm_unit_id text,"
                + "clm_test_timestamp text,"
                + "clm_ellapsed_time_cnt text,"
                + "clm_watt_value text);";
        Log.i(TAG, "> sql : " + sql);

        db.execSQL(sql);
        sql = "";

        sql = "CREATE TABLE if not exists tbl_test_setting_data ("
                + "clm_setting_seq,"
                + "clm_setting_id text,"
                + "clm_setting_value text,"
                + "clm_comment text,"
                + "clm_test_timestamp text);";
        Log.i(TAG, "> sql : " + sql);

        db.execSQL(sql);
        sql = "";

        sql = "CREATE TABLE if not exists tbl_setting_info ("
                + "clm_setting_seq text,"
                + "clm_setting_name_kr text,"
                + "clm_setting_name_en text,"
                + "clm_setting_id text,"
                + "clm_setting_value text,"
                + "clm_comment text,"
                + "clm_test_timestamp text);";
        Log.i(TAG, "> sql : " + sql);

        db.execSQL(sql);
        sql = "";
    }

    String TAG = "DBHelper";

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        sql = "DROP TABLE if exists tbl_hot_temperature;";
        db.execSQL(sql);

        sql = "DROP TABLE if exists tbl_cold_temperature;";
        db.execSQL(sql);

        sql = "DROP TABLE if exists tbl_test_spec_data;";
        db.execSQL(sql);

        onCreate(db);
    }
}