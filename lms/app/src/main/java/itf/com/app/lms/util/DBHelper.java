package itf.com.app.lms.util;

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
                + "clm_model_id text,"
                + "clm_model_expand_id text,"
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
                + "clm_test_version_id text);";
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

        sql = "CREATE TABLE if not exists tbl_model_info ("
                + "clm_model_seq primary key,"
                + "clm_client_name text,"
                + "clm_client_id text,"
                + "clm_test_step text,"
                + "clm_company_key text,"
                + "clm_model_id text,"
                + "clm_model_name text,"
                + "clm_model_version text,"
                + "clm_comment text,"
                + "clm_regist_timestamp text,"
                + "clm_update_timestamp text);";
        Log.i(TAG, "> sql : " + sql);

        db.execSQL(sql);
        sql = "";

        sql = "CREATE TABLE if not exists tbl_model_info_version_history ("
                + "clm_version_history_seq primary key,"
                + "clm_model_seq text,"
                + "clm_client_name text,"
                + "clm_client_id text,"
                + "clm_test_step text,"
                + "clm_company_key text,"
                + "clm_model_id text,"
                + "clm_model_name text,"
                + "clm_model_version text,"
                + "clm_comment text,"
                + "clm_version_number text,"
                + "clm_change_type text,"
                + "clm_change_timestamp text);";
        Log.i(TAG, "> sql : " + sql);

        db.execSQL(sql);
        sql = "";

        sql = "CREATE TABLE if not exists tbl_test_spec_data_version_history ("
                + "clm_version_history_seq primary key,"
                + "clm_test_seq text,"
                + "clm_model_id text,"
                + "clm_model_expand_id text,"
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
                + "clm_version_number text,"
                + "clm_change_type text,"
                + "clm_change_timestamp text);";
        Log.i(TAG, "> sql : " + sql);

        db.execSQL(sql);
        sql = "";

        // ==================== 문자열 리소스 테이블 ====================
        sql = "CREATE TABLE if not exists tbl_string_resources ("
                + "clm_string_key text primary key,"
                + "clm_string_category text not null,"
                + "clm_string_ko text,"
                + "clm_string_en text,"
                + "clm_string_zh text,"
                + "clm_string_ja text,"
                + "clm_description text,"
                + "clm_is_user_visible integer default 1,"
                + "clm_created_timestamp text,"
                + "clm_updated_timestamp text);";
        Log.i(TAG, "> sql : " + sql);
        db.execSQL(sql);
        sql = "";

        // 인덱스 생성 (조회 성능 향상)
        sql = "CREATE INDEX if not exists idx_string_category ON tbl_string_resources(clm_string_category);";
        Log.i(TAG, "> sql : " + sql);
        db.execSQL(sql);
        sql = "";

        sql = "CREATE INDEX if not exists idx_string_key ON tbl_string_resources(clm_string_key);";
        Log.i(TAG, "> sql : " + sql);
        db.execSQL(sql);
        sql = "";

        // ==================== 로그 메시지 테이블 ====================
        sql = "CREATE TABLE IF NOT EXISTS tbl_log_messages ("
                + "clm_log_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "clm_log_type TEXT NOT NULL,"
                + "clm_error_id TEXT,"
                + "clm_category TEXT NOT NULL,"
                + "clm_tag TEXT NOT NULL,"
                + "clm_message TEXT NOT NULL,"
                + "clm_stack_trace TEXT,"
                + "clm_timestamp TEXT NOT NULL,"
                + "clm_thread_name TEXT,"
                + "clm_user_id TEXT,"
                + "clm_device_info TEXT,"
                + "clm_extra_data TEXT,"
                + "clm_is_synced INTEGER DEFAULT 0,"
                + "clm_synced_timestamp TEXT"
                + ");";
        Log.i(TAG, "> sql : " + sql);
        db.execSQL(sql);
        sql = "";

        // 로그 메시지 테이블 인덱스
        sql = "CREATE INDEX IF NOT EXISTS idx_log_type ON tbl_log_messages(clm_log_type);";
        Log.i(TAG, "> sql : " + sql);
        db.execSQL(sql);
        sql = "";

        sql = "CREATE INDEX IF NOT EXISTS idx_error_id ON tbl_log_messages(clm_error_id);";
        Log.i(TAG, "> sql : " + sql);
        db.execSQL(sql);
        sql = "";

        sql = "CREATE INDEX IF NOT EXISTS idx_category ON tbl_log_messages(clm_category);";
        Log.i(TAG, "> sql : " + sql);
        db.execSQL(sql);
        sql = "";

        sql = "CREATE INDEX IF NOT EXISTS idx_timestamp ON tbl_log_messages(clm_timestamp);";
        Log.i(TAG, "> sql : " + sql);
        db.execSQL(sql);
        sql = "";

        sql = "CREATE INDEX IF NOT EXISTS idx_synced ON tbl_log_messages(clm_is_synced);";
        Log.i(TAG, "> sql : " + sql);
        db.execSQL(sql);
        sql = "";

        // ==================== 에러 카탈로그 테이블 ====================
        sql = "CREATE TABLE IF NOT EXISTS tbl_error_catalog ("
                + "clm_error_id TEXT PRIMARY KEY,"
                + "clm_error_category TEXT NOT NULL,"
                + "clm_error_code TEXT NOT NULL,"
                + "clm_error_name_ko TEXT,"
                + "clm_error_name_en TEXT,"
                + "clm_error_description_ko TEXT,"
                + "clm_error_description_en TEXT,"
                + "clm_severity TEXT NOT NULL,"
                + "clm_solution_ko TEXT,"
                + "clm_solution_en TEXT,"
                + "clm_created_timestamp TEXT NOT NULL,"
                + "clm_updated_timestamp TEXT"
                + ");";
        Log.i(TAG, "> sql : " + sql);
        db.execSQL(sql);
        sql = "";

        // 에러 카탈로그 테이블 인덱스
        sql = "CREATE INDEX IF NOT EXISTS idx_error_category ON tbl_error_catalog(clm_error_category);";
        Log.i(TAG, "> sql : " + sql);
        db.execSQL(sql);
        sql = "";

        sql = "CREATE INDEX IF NOT EXISTS idx_error_severity ON tbl_error_catalog(clm_severity);";
        Log.i(TAG, "> sql : " + sql);
        db.execSQL(sql);
        sql = "";

        // ==================== APP_LANGUAGE 초기 데이터 ====================
        // tbl_setting_info에 APP_LANGUAGE가 없으면 추가
        sql = "INSERT OR IGNORE INTO tbl_setting_info ("
                + "clm_setting_seq,"
                + "clm_setting_name_kr,"
                + "clm_setting_name_en,"
                + "clm_setting_id,"
                + "clm_setting_value,"
                + "clm_comment,"
                + "clm_test_timestamp"
                + ") SELECT "
                + "'1',"
                + "'애플리케이션 언어',"
                + "'Application Language',"
                + "'APP_LANGUAGE',"
                + "'ko',"
                + "'애플리케이션 표시 언어를 선택합니다',"
                + "datetime('now')"
                + " WHERE NOT EXISTS (SELECT 1 FROM tbl_setting_info WHERE clm_setting_id = 'APP_LANGUAGE');";
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