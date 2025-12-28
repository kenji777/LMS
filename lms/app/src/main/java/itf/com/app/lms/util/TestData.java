package itf.com.app.lms.util;

import static itf.com.app.lms.util.Constants.InitialValues.DATABASE_VERSION;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TestData {
    private static final String TAG = "TemperatureData";
    private static List<Map<String, String>> lstData = null;
//    private static Cursor cur_temperature_data;

    // 앱 프로세스 기준 1회만 기본 설정 로우 보강 + 캐시 로드를 수행하기 위한 플래그
    private static volatile boolean sCommonSettingsLoaded = false;

    /**
     * 공통 설정 로우 보강 + 캐시 로드 (프로세스 1회)
     * - ActivitySplash 등 앱 초기 구간에서 호출하는 용도
     * - IMPORTANT: 기존 사용자 설정값을 절대 덮어쓰지 않기 위해 "없을 때만 INSERT"로 동작
     */
    public static void loadCommonSettingsOnce(Context context) {
        if (context == null) return;

        // 이미 수행된 경우에도 캐시 로드는 재시도 (안전)
        if (sCommonSettingsLoaded) {
            try {
                AppSettings.loadFromDb(context);
                loadUnitIdPrefixFromDb(context);
            } catch (Exception ignored) {}
            return;
        }

        synchronized (TestData.class) {
            if (sCommonSettingsLoaded) {
                try {
                    AppSettings.loadFromDb(context);
                    loadUnitIdPrefixFromDb(context);
                } catch (Exception ignored) {}
                return;
            }

            DBHelper helper = null;
            SQLiteDatabase db = null;
            try {
                helper = new DBHelper(context, "itf_temperature_table.db", null, DATABASE_VERSION);
                db = helper.getWritableDatabase();

                String now = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());

                // 진동 강도 설정 로우 보장 (10 단위/DEFAULT)
                ensureSettingRowExistsIfMissing(
                        db,
                        AppSettings.SETTING_ID_VIBRATION_AMPLITUDE,
                        "DEFAULT",
                        "진동 강도 (10~250, 10단위 또는 DEFAULT)",
                        now
                );

                // (추가 기본 설정이 필요하면 여기에 ensureSettingRowExistsIfMissing(...)로 확장)

                // 메모리 캐시 로드
                AppSettings.loadFromDb(context);
                loadUnitIdPrefixFromDb(context);

                sCommonSettingsLoaded = true;
            } catch (Exception e) {
                Log.w(TAG, "> TestData.loadCommonSettingsOnce.e : " + e);
            } finally {
                if (db != null) {
                    try { db.close(); } catch (Exception ignored) {}
                }
                if (helper != null) {
                    try { helper.close(); } catch (Exception ignored) {}
                }
            }
        }
    }

    /**
     * clm_setting_id 기준으로 로우가 없을 때만 INSERT (기존 값 덮어쓰기 방지)
     */
    private static void ensureSettingRowExistsIfMissing(SQLiteDatabase db,
                                                       String settingId,
                                                       String defaultValue,
                                                       String comment,
                                                       String nowTimestamp) {
        if (db == null) return;
        if (settingId == null || settingId.trim().isEmpty()) return;

        String tableName = "tbl_setting_info";
        Cursor cursor = null;
        try {
            boolean exists = false;
            cursor = db.rawQuery(
                    "SELECT COUNT(*) FROM " + tableName + " WHERE clm_setting_id = ?",
                    new String[]{settingId}
            );
            if (cursor != null && cursor.moveToFirst()) {
                exists = cursor.getInt(0) > 0;
            }
            if (exists) return;

            ContentValues values = new ContentValues();
            values.put("clm_setting_id", settingId);
            values.put("clm_setting_value", defaultValue != null ? defaultValue : "");
            values.put("clm_comment", comment != null ? comment : "");
            values.put("clm_test_timestamp", nowTimestamp != null ? nowTimestamp : "");

            long rowId = db.insert(tableName, null, values);
            Log.i(TAG, "> TestData.ensureSettingRowExistsIfMissing.INSERT: clm_setting_id=" + settingId + ", rowId=" + rowId);
        } catch (Exception e) {
            Log.w(TAG, "> TestData.ensureSettingRowExistsIfMissing.e : " + e);
        } finally {
            if (cursor != null) {
                try { cursor.close(); } catch (Exception ignored) {}
            }
        }
    }

    /**
     * tbl_setting_info에서 UNIT_ID_PREFIX 설정값을 읽어 Constants.Common.UNIT_ID_PREFIX에 설정
     */
    private static void loadUnitIdPrefixFromDb(Context context) {
        if (context == null) return;
        try {
            String value = null;
            List<Map<String, String>> settings = selectSettingInfo(context);
            if (settings != null) {
                for (Map<String, String> row : settings) {
                    if (row == null) continue;
                    // System.out.println(">>>>>>>>>>>>>> loadUnitIdPrefixFromDb.clm_setting_value " + row.get("clm_setting_id") + " : " + row.get("clm_setting_value"));
                    if ("UNIT_ID_PREFIX".equals(row.get("clm_setting_id"))) {
                        value = row.get("clm_setting_value");
                        break;
                    }
                }
            }
            // System.out.println(">>>>>>>>>>>>>> loadUnitIdPrefixFromDb.value " + value);
            // Constants.Common.UNIT_ID_PREFIX 설정
            Constants.Common.UNIT_ID_PREFIX = (value != null && !value.trim().isEmpty()) ? String.format(Constants.Common.NUMBER_FORMAT_03D, Integer.parseInt(value.trim())) : "000";
            Log.i(TAG, "> TestData.loadUnitIdPrefixFromDb: UNIT_ID_PREFIX=" + Constants.Common.UNIT_ID_PREFIX);
        } catch (Exception e) {
            Log.w(TAG, "> TestData.loadUnitIdPrefixFromDb.e : " + e);
            Constants.Common.UNIT_ID_PREFIX = "000";
        }
    }

    public static boolean deleteProductTemperatureData(Context context, String tableType) {
        DBHelper helper = null;
        SQLiteDatabase db = null;
        String tableName = "tbl_product_temperature";

        try {
            helper = new DBHelper(context, "itf_temperature_table.db", null, DATABASE_VERSION);
            db = helper.getWritableDatabase();

            String sql = "";
            sql = "DELETE FROM " + tableName + ";";
            db.execSQL(sql);
            Log.i(TAG, "> 3." + tableName + ".sql " + sql);
        }
        catch (Exception e) {
            Log.e(TAG, "> TestData.deleteProductTemperatureData.e.1 : " + e);
            return false;
        }
        finally {
            db.close();
            helper.close();
        }

        return true;
    }

    public static boolean insertProductTemperatureData(Context context, Map<String, String> rowData) {
        DBHelper helper = null;
        SQLiteDatabase db = null;
        String tableName = "tbl_product_temperature";

        try {
            helper = new DBHelper(context, "itf_temperature_table.db", null, DATABASE_VERSION);
            db = helper.getWritableDatabase();

            String sql = "";
            sql = "INSERT INTO " + tableName + "('clm_line', 'clm_unit_id', 'clm_timestamp', 'clm_product_temperature', 'clm_comment') values('" + rowData.get("clm_line") + "', '" + rowData.get("clm_unit_id") + "', '" + rowData.get("clm_timestamp") + "', '" + rowData.get("clm_product_temperature") + "', '" + rowData.get("clm_comment") + "');";
            db.execSQL(sql);
            db.close();

//            Log.i(TAG, "> " + tableName + ".sql " + sql);
        }
        catch (Exception e) {
            Log.e(TAG, "> TestData.insertTestData.e.1 : " + e);
            return false;
        }
        finally {
            db.close();
            helper.close();
        }

//        selectTemperatureData(context, "1");

        return true;
    }

    public static boolean deleteTestHistoryData(Context context) {
        DBHelper helper = null;
        SQLiteDatabase db = null;
        String tableName = "tbl_test_history";

        try {
            helper = new DBHelper(context, "itf_temperature_table.db", null, DATABASE_VERSION);
            db = helper.getWritableDatabase();

            String sql = "";
            sql = "DELETE FROM " + tableName + ";";
            db.execSQL(sql);
            Log.i(TAG, "> 4." + tableName + ".sql " + sql);
        }
        catch (Exception e) {
            Log.e(TAG, "> TestData.deleteTestData.e.2 : " + e);
            return false;
        }
        finally {
            db.close();
            helper.close();
        }

        return true;
    }

    public static boolean updateTestHistoryData(Context context, Map<String, String> rowData) {
        DBHelper helper = null;
        SQLiteDatabase db = null;
        String tableName = "tbl_test_history";
        String updateKeyValue = "";

        try {
            helper = new DBHelper(context, "itf_temperature_table.db", null, DATABASE_VERSION);
            db = helper.getWritableDatabase();

            for( String key : rowData.keySet() ){
                System.out.println( String.format("키 : %s, 값 : %s", key, rowData.get(key)) );
                if(rowData.get(key)!=null) {
                    updateKeyValue  = "";
                    updateKeyValue += key + "='" + rowData.get(key) + "', ";
                }
            }

            String sql = "";
            sql = "UPDATE " + tableName + " SET " + updateKeyValue + " ";
            sql = "WHERE clm_test_history_seq='" + rowData.get("clm_test_history_seq") + "';";
            db.execSQL(sql);
            Log.i(TAG, "> 4.updateTestHistoryData " + tableName + ".sql " + sql);
        }
        catch (Exception e) {
            Log.e(TAG, "> TestData.deleteTestData.e.2 : " + e);
            return false;
        }
        finally {
            db.close();
            helper.close();
        }

        return true;
    }

    public static boolean insertProductTestHistory(Context context, Map<String, String> rowData) {
        DBHelper helper = null;
        SQLiteDatabase db = null;
        String tableName = "tbl_test_history";

        if(rowData.get("clm_test_history_seq") != null) {
            try {
                helper = new DBHelper(context, "itf_temperature_table.db", null, DATABASE_VERSION);
                db = helper.getWritableDatabase();

//            sql = "CREATE TABLE if not exists tbl_test_history ("
//                    + "clm_test_history_seq primary key,"
//                    + "clm_test_model_id text,"
//                    + "clm_test_model_name text,"
//                    + "clm_test_model_nationality text,"
//                    + "clm_test_timestamp text,"
//                    + "clm_comment text);";
//            Log.i(TAG, "> sql : " + sql);

//                + "clm_test_result text,"
//                + "clm_test_ng_count text,"
//                + "clm_test_ok_count text,"
                String sql = "";
                sql = "INSERT INTO " + tableName + "('clm_test_history_seq', 'clm_test_model_id', 'clm_test_model_name', 'clm_test_model_nationality', 'clm_test_timestamp', 'clm_test_result', 'clm_test_ng_count', 'clm_test_ok_count', 'clm_comment') values('" + rowData.get("clm_test_history_seq") + "', '" + rowData.get("clm_test_model_id") + "', '" + rowData.get("clm_test_model_name") + "', '" + rowData.get("clm_test_model_nationality") + "', '" + rowData.get("clm_test_timestamp") + "', '" + rowData.get("clm_test_result") + "', '" + rowData.get("clm_test_ng_count") + "', '" + rowData.get("clm_test_ok_count") + "', '" + rowData.get("clm_comment") + "');";
                Log.i(TAG, "> " + tableName + ".sql " + sql);
                db.execSQL(sql);
                db.close();

//            Log.i(TAG, "> " + tableName + ".sql " + sql);
            }
            catch (Exception e) {
                Log.d(TAG, "> TestData.insertProductTestHistory.e.1 : " + e);
                return false;
            }
            finally {
                db.close();
                helper.close();
            }
        }

//        selectTemperatureData(context, "1");

        return true;
    }

    public static boolean createProductTestHistory(Context context) {
        DBHelper helper = null;
        SQLiteDatabase db = null;
        String tableName = "tbl_test_history";

        try {
            helper = new DBHelper(context, "itf_temperature_table.db", null, DATABASE_VERSION);
            db = helper.getWritableDatabase();

            String sql = "";
            sql = "CREATE TABLE if not exists tbl_test_history ("
                    + "clm_test_history_seq primary key,"
                    + "clm_test_model_id text,"
                    + "clm_test_model_name text,"
                    + "clm_test_model_nationality text,"
                    + "clm_test_timestamp text,"
                    + "clm_test_result text,"
                    + "clm_test_ng_count text,"
                    + "clm_test_ok_count text,"
                    + "clm_comment text);";

            db.execSQL(sql);
            db.close();

            Log.i(TAG, "> " + tableName + ".sql " + sql);
        }
        catch (Exception e) {
            Log.e(TAG, "> TestData.createProductTestHistory.e.1 : " + e);
            return false;
        }
        finally {
            db.close();
            helper.close();
        }

//        selectTemperatureData(context, "1");

        return true;
    }

    /**
     * tbl_test_history 테이블의 clm_http_success_yn 컬럼 업데이트
     */
    public static boolean updateProductTestHistoryHttpSuccess(Context context, String testHistorySeq, String httpSuccessYn) {
        DBHelper helper = null;
        SQLiteDatabase db = null;
        String tableName = "tbl_test_history";

        try {
            helper = new DBHelper(context, "itf_temperature_table.db", null, DATABASE_VERSION);
            db = helper.getWritableDatabase();

            // 컬럼이 없으면 추가
            try {
                // String alterSql = "ALTER TABLE " + tableName + " ADD COLUMN clm_http_success_yn text;";
                // db.execSQL(alterSql);
                // Log.i(TAG, "> " + tableName + ".ALTER TABLE sql " + alterSql);
            } catch (Exception e) {
                // 컬럼이 이미 존재하는 경우 무시
                Log.d(TAG, "> clm_http_success_yn column may already exist: " + e.getMessage());
            }

            // 업데이트
            String sql = "UPDATE " + tableName + " SET clm_http_success_yn = '" + httpSuccessYn + "' WHERE clm_test_history_seq = '" + testHistorySeq + "';";
            Log.i(TAG, "> " + tableName + ".UPDATE sql " + sql);
            db.execSQL(sql);
        }
        catch (Exception e) {
            Log.e(TAG, "> TestData.updateProductTestHistoryHttpSuccess.e.1 : " + e);
            return false;
        }
        finally {
            if (db != null) {
                db.close();
            }
            if (helper != null) {
                helper.close();
            }
        }

        return true;
    }

    /**
     * tbl_test_history_linear_data 테이블에 데이터 삽입
     */
    public static boolean insertTestHistoryLinearData(Context context, String testHistorySeq, String testItemId, String timestamp, String temperature, String watt) {
        DBHelper helper = null;
        SQLiteDatabase db = null;
        String tableName = "tbl_test_history_linear_data";

        try {
            helper = new DBHelper(context, "itf_temperature_table.db", null, DATABASE_VERSION);
            db = helper.getWritableDatabase();

            String sql = "";
            sql = "INSERT INTO " + tableName + "('clm_test_history_seq', 'clm_test_item_id', 'clm_test_timestamp', 'clm_temperature', 'clm_watt', 'clm_comment') values('" 
                    + (testHistorySeq != null ? testHistorySeq : "") + "', '" 
                    + (testItemId != null ? testItemId : "") + "', '" 
                    + (timestamp != null ? timestamp : "") + "', '" 
                    + (temperature != null ? temperature : "") + "', '" 
                    + (watt != null ? watt : "") + "', '" 
                    + "');";
            // Log.i(TAG, "> " + tableName + ".sql " + sql);
            db.execSQL(sql);
        }
        catch (Exception e) {
            Log.e(TAG, "> TestData.insertTestHistoryLinearData.e.1 : " + e);
            return false;
        }
        finally {
            if (db != null) {
                db.close();
            }
            if (helper != null) {
                helper.close();
            }
        }

        return true;
    }

    /**
     * tbl_test_history_linear_data 테이블에서 데이터 조회
     */
    public static List<Map<String, String>> selectTestHistoryLinearData(Context context, String testHistorySeq) {
        DBHelper helper = null;
        SQLiteDatabase db = null;
        Cursor cur_data = null;
        String tableName = "tbl_test_history_linear_data";
        int rowCnt = 0;
        Map<String, String> mapData = null;
        List<Map<String, String>> lstData = null;

        try {
            helper = new DBHelper(context, "itf_temperature_table.db", null, DATABASE_VERSION);

            String sql = "";
            db = helper.getReadableDatabase();
            sql = "SELECT * FROM " + tableName + " WHERE clm_test_history_seq='" + testHistorySeq + "' ORDER BY clm_test_timestamp ASC;";
            cur_data = db.rawQuery(sql, null);

            lstData = new ArrayList<Map<String, String>>();

            while (cur_data.moveToNext()){
                mapData = new HashMap<String, String>();
                mapData.put("clm_test_history_seq", cur_data.getString(0));
                mapData.put("clm_test_item_id", cur_data.getString(1));
                mapData.put("clm_test_timestamp", cur_data.getString(2));
                mapData.put("clm_temperature", cur_data.getString(3));
                mapData.put("clm_watt", cur_data.getString(4));
                mapData.put("clm_comment", cur_data.getString(5));
                lstData.add(mapData);
                rowCnt++;
            }
        }
        catch (Exception e) {
            Log.e(TAG, "> TestData.selectTestHistoryLinearData.e.1 : " + e);
        }
        finally {
            if (cur_data != null) {
                try {
                    cur_data.close();
                } catch (Exception e) {
                    Log.e(TAG, "> TestData.selectTestHistoryLinearData.finally.cur_data.close : " + e);
                }
            }
            if (helper != null) {
                try {
                    helper.close();
                } catch (Exception e) {
                    Log.e(TAG, "> TestData.selectTestHistoryLinearData.finally.helper.close : " + e);
                }
            }
        }

        // lstData가 null인 경우 빈 리스트 반환
        if (lstData == null) {
            lstData = new ArrayList<Map<String, String>>();
        }
        return lstData;
    }

    /**
     * 검사 상세 내역 테이블 생성
     */
    public static boolean createProductTestHistoryDetail(Context context) {
        DBHelper helper = null;
        SQLiteDatabase db = null;
        String tableName = "tbl_test_history_detail";

        try {
            helper = new DBHelper(context, "itf_temperature_table.db", null, DATABASE_VERSION);
            db = helper.getWritableDatabase();

            String sql = "";
            sql = "CREATE TABLE if not exists " + tableName + " ("
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
                    + "clm_test_item_info text,"
                    + "clm_test_upper_value text,"
                    + "clm_test_lower_value text,"
                    + "clm_test_result_check_value text,"
                    + "clm_bt_raw_message text,"
                    + "clm_bt_raw_response text,"
                    + "clm_bt_processed_value text,"
                    + "clm_comment text);";

            db.execSQL(sql);
            
            // 기존 테이블에 새 컬럼 추가 (컬럼이 없으면 추가)
            addColumnIfMissing(db, tableName, "clm_test_item_info", "text");
            addColumnIfMissing(db, tableName, "clm_test_upper_value", "text");
            addColumnIfMissing(db, tableName, "clm_test_lower_value", "text");
            addColumnIfMissing(db, tableName, "clm_test_result_check_value", "text");
            addColumnIfMissing(db, tableName, "clm_bt_raw_message", "text");
            
            addColumnIfMissing(db, tableName, "clm_bt_raw_response", "text");
            addColumnIfMissing(db, tableName, "clm_bt_processed_value", "text");

            Log.i(TAG, "> " + tableName + ".sql " + sql);
        }
        catch (Exception e) {
            Log.e(TAG, "> TestData.createProductTestHistoryDetail.e.1 : " + e);
            return false;
        }
        finally {
            if (db != null) {
                db.close();
            }
            if (helper != null) {
                helper.close();
            }
        }

        return true;
    }

    /**
     * 검사 상세 내역 저장
     */
    public static boolean insertProductTestHistoryDetail(Context context, Map<String, String> rowData) {
        DBHelper helper = null;
        SQLiteDatabase db = null;
        String tableName = "tbl_test_history_detail";

        if(rowData.get("clm_test_history_seq") != null) {
            try {
                helper = new DBHelper(context, "itf_temperature_table.db", null, DATABASE_VERSION);
                db = helper.getWritableDatabase();

                String sql = "";
                sql = "INSERT INTO " + tableName + "('clm_test_history_seq', 'clm_test_item_seq', 'clm_test_item_name', 'clm_test_item_command', 'clm_test_item_result', 'clm_test_item_value', 'clm_test_response_value', 'clm_test_result_value', 'clm_test_temperature', 'clm_test_electric_val', 'clm_test_item_info', 'clm_test_upper_value', 'clm_test_lower_value', 'clm_test_result_check_value', 'clm_bt_raw_message', 'clm_bt_raw_response', 'clm_bt_processed_value', 'clm_comment') values('" 
                        + rowData.get("clm_test_history_seq") + "', '"
                        + (rowData.get("clm_test_item_seq") != null ? rowData.get("clm_test_item_seq") : "") + "', '"
                        + (rowData.get("clm_test_item_name") != null ? rowData.get("clm_test_item_name") : "") + "', '" 
                        + (rowData.get("clm_test_item_command") != null ? rowData.get("clm_test_item_command") : "") + "', '" 
                        + (rowData.get("clm_test_item_result") != null ? rowData.get("clm_test_item_result") : "") + "', '" 
                        + (rowData.get("clm_test_item_value") != null ? rowData.get("clm_test_item_value") : "") + "', '" 
                        + (rowData.get("clm_test_response_value") != null ? rowData.get("clm_test_response_value") : "") + "', '" 
                        + (rowData.get("clm_test_result_value") != null ? rowData.get("clm_test_result_value") : "") + "', '" 
                        + (rowData.get("clm_test_temperature") != null ? rowData.get("clm_test_temperature") : "") + "', '" 
                        + (rowData.get("clm_test_electric_val") != null ? rowData.get("clm_test_electric_val") : "") + "', '" 
                        + (rowData.get("clm_test_item_info") != null ? rowData.get("clm_test_item_info") : "") + "', '" 
                        + (rowData.get("clm_test_upper_value") != null ? rowData.get("clm_test_upper_value") : "") + "', '" 
                        + (rowData.get("clm_test_lower_value") != null ? rowData.get("clm_test_lower_value") : "") + "', '" 
                        + (rowData.get("clm_test_result_check_value") != null ? rowData.get("clm_test_result_check_value") : "") + "', '" 
                        + (rowData.get("clm_bt_raw_message") != null ? rowData.get("clm_bt_raw_message").replace("'", "''") : "") + "', '" 
                        + (rowData.get("clm_bt_raw_response") != null ? rowData.get("clm_bt_raw_response") : "") + "', '" 
                        + (rowData.get("clm_bt_processed_value") != null ? rowData.get("clm_bt_processed_value") : "") + "', '" 
                        + (rowData.get("clm_comment") != null ? rowData.get("clm_comment") : "") + "');";
                Log.i(TAG, "> " + tableName + ".sql " + sql);
                db.execSQL(sql);
                db.close();
            }
            catch (Exception e) {
                Log.d(TAG, "> TestData.insertProductTestHistoryDetail.e.1 : " + e);
                return false;
            }
            finally {
                db.close();
                helper.close();
            }
        }

        return true;
    }

    /**
     * 검사 상세 내역 조회
     */
    public static List<Map<String, String>> selectTestHistoryDetail(Context context, String testHistorySeq) {
        DBHelper helper = null;
        SQLiteDatabase db = null;
        Cursor cur_detail_data = null;
        String tableName = "tbl_test_history_detail";
        int rowCnt = 0;
        Map<String, String> mapData = null;
        List<Map<String, String>> lstData = null;

        try {
            helper = new DBHelper(context, "itf_temperature_table.db", null, DATABASE_VERSION);

            String sql = "";
            db = helper.getReadableDatabase();
            sql = "SELECT * FROM " + tableName + " WHERE clm_test_history_seq='" + testHistorySeq + "' ORDER BY CAST(clm_test_item_seq AS INTEGER) ASC;";
            cur_detail_data = db.rawQuery(sql, null);
            Log.i(TAG, "> TestData.selectTestHistoryDetail.sql : " + sql);

            lstData = new ArrayList<Map<String, String>>();

            while (cur_detail_data.moveToNext()){
                mapData = new HashMap<String, String>();
                mapData.put("clm_test_history_detail_seq", cur_detail_data.getString(0));
                mapData.put("clm_test_history_seq", cur_detail_data.getString(1));
                mapData.put("clm_test_item_seq", cur_detail_data.getString(2));
                mapData.put("clm_test_item_name", cur_detail_data.getString(3));
                mapData.put("clm_test_item_command", cur_detail_data.getString(4));
                mapData.put("clm_test_item_result", cur_detail_data.getString(5));
                mapData.put("clm_test_item_value", cur_detail_data.getString(6));
                mapData.put("clm_test_response_value", cur_detail_data.getString(7));
                mapData.put("clm_test_result_value", cur_detail_data.getString(8));
                mapData.put("clm_test_temperature", cur_detail_data.getString(9));
                mapData.put("clm_test_electric_val", cur_detail_data.getString(10));
                mapData.put("clm_comment", cur_detail_data.getString(11));
                // 새로 추가된 컬럼들 (컬럼이 없을 수 있으므로 try-catch로 처리)
                try {
                    mapData.put("clm_test_item_info", cur_detail_data.getString(12));
                    mapData.put("clm_test_upper_value", cur_detail_data.getString(13));
                    mapData.put("clm_test_lower_value", cur_detail_data.getString(14));
                    mapData.put("clm_test_result_check_value", cur_detail_data.getString(15));
                    mapData.put("clm_bt_raw_message", cur_detail_data.getString(16));
                    mapData.put("clm_bt_raw_response", cur_detail_data.getString(17));
                    mapData.put("clm_bt_processed_value", cur_detail_data.getString(18));
                } catch (Exception e) {
                    // 기존 테이블에 컬럼이 없는 경우를 대비
                    try {
                        mapData.put("clm_test_item_info", cur_detail_data.getColumnCount() > 11 ? cur_detail_data.getString(11) : "");
                        mapData.put("clm_test_upper_value", cur_detail_data.getColumnCount() > 12 ? cur_detail_data.getString(12) : "");
                        mapData.put("clm_test_lower_value", cur_detail_data.getColumnCount() > 13 ? cur_detail_data.getString(13) : "");
                        mapData.put("clm_test_result_check_value", cur_detail_data.getColumnCount() > 14 ? cur_detail_data.getString(14) : "");
                        mapData.put("clm_bt_raw_message", cur_detail_data.getColumnCount() > 15 ? cur_detail_data.getString(15) : "");
                        mapData.put("clm_bt_raw_response", cur_detail_data.getColumnCount() > 16 ? cur_detail_data.getString(16) : "");
                        mapData.put("clm_bt_processed_value", cur_detail_data.getColumnCount() > 17 ? cur_detail_data.getString(17) : "");
                        mapData.put("clm_comment", cur_detail_data.getColumnCount() > 11 ? cur_detail_data.getString(cur_detail_data.getColumnCount() - 1) : "");
                    }
                    catch (Exception ex) {
                        mapData.put("clm_test_item_info", "");
                        mapData.put("clm_test_upper_value", "");
                        mapData.put("clm_test_lower_value", "");
                        mapData.put("clm_test_result_check_value", "");
                        mapData.put("clm_bt_raw_message", "");
                        mapData.put("clm_bt_raw_response", "");
                        mapData.put("clm_bt_processed_value", "");
                        mapData.put("clm_comment", cur_detail_data.getColumnCount() > 11 ? cur_detail_data.getString(11) : "");
                    }
                }
                lstData.add(mapData);
                rowCnt++;
            }
        }
        catch (Exception e) {
            Log.e(TAG, "> TestData.selectTestHistoryDetail.e.1 : " + e);
        }
        finally {
            if (cur_detail_data != null) {
                cur_detail_data.close();
            }
            helper.close();
        }

        return lstData;
    }

    public static boolean insertProductWattData(Context context, Map<String, String> rowData) {
        DBHelper helper = null;
        SQLiteDatabase db = null;
        String tableName = "tbl_product_watt";

        try {
            helper = new DBHelper(context, "itf_temperature_table.db", null, DATABASE_VERSION);
            db = helper.getWritableDatabase();

            String sql = "";
            sql = "INSERT INTO " + tableName + "('clm_line', 'clm_unit_id', 'clm_timestamp', 'clm_product_watt', 'clm_product_ampere', 'clm_comment') values('" + rowData.get("clm_line") + "', '" + rowData.get("clm_unit_id") + "', '" + rowData.get("clm_timestamp") + "', '" + rowData.get("clm_product_watt") + "', '" + rowData.get("clm_product_ampere") + "', '" + rowData.get("clm_comment") + "');";
            db.execSQL(sql);
            db.close();

//            Log.i(TAG, "> " + tableName + ".sql " + sql);
        }
        catch (Exception e) {
            Log.e(TAG, "> TestData.insertProductWattData.e.1 : " + e);
            return false;
        }
        finally {
            db.close();
            helper.close();
        }

//        selectTemperatureData(context, "1");

        return true;
    }

    public static boolean deleteTemperatureData(Context context, String tableType) {
        DBHelper helper = null;
        SQLiteDatabase db = null;
        String tableName = (tableType.equals("1")) ? "tbl_cold_temperature" : "tbl_hot_temperature";

        try {
            helper = new DBHelper(context, "itf_temperature_table.db", null, DATABASE_VERSION);
            db = helper.getWritableDatabase();

            String sql = "";
            sql = "DELETE FROM " + tableName + ";";
            db.execSQL(sql);
            Log.i(TAG, "> 5." + tableName + ".sql " + sql);
        }
        catch (Exception e) {
            Log.e(TAG, "> TestData.deleteTestData.e.1 : " + e);
            return false;
        }
        finally {
            db.close();
            helper.close();
        }

        return true;
    }

    public static boolean insertTestSpecData(Context context, Map<String, String> rowData) {
        DBHelper helper = null;
        SQLiteDatabase db = null;
        String tableName = "tbl_test_spec_data";

        try {
            helper = new DBHelper(context, "itf_temperature_table.db", null, DATABASE_VERSION);
            db = helper.getWritableDatabase();
            ensureTestSpecColumns(db, tableName);

            ContentValues values = new ContentValues();
            values.put("clm_test_seq", safeGet(rowData, Constants.JsonKeys.CLM_TEST_ID));
            values.put("clm_test_command", safeGet(rowData, Constants.JsonKeys.CLM_TEST_COMMAND));
            values.put("clm_test_name", safeGet(rowData, Constants.JsonKeys.CLM_TEST_NAME));
            values.put("clm_test_type", safeGet(rowData, Constants.JsonKeys.CLM_TEST_TYPE));
            values.put("clm_test_response_value", safeGet(rowData, Constants.JsonKeys.CLM_RESPONSE_VALUE));
            values.put("clm_test_upper_value", safeGet(rowData, Constants.JsonKeys.CLM_UPPER_VALUE));
            values.put("clm_test_lower_value", safeGet(rowData, Constants.JsonKeys.CLM_LOWER_VALUE));
            values.put("clm_test_upper_value_02", safeGet(rowData, Constants.JsonKeys.CLM_UPPER_VALUE_WATT));
            values.put("clm_test_lower_value_02", safeGet(rowData, Constants.JsonKeys.CLM_LOWER_VALUE_WATT));
            values.put("clm_comment", safeGet(rowData, Constants.JsonKeys.CLM_COMMENT));
            values.put("clm_value_watt", safeGet(rowData, Constants.JsonKeys.CLM_VALUE_WATT));
            values.put("clm_value", safeGet(rowData, Constants.JsonKeys.CLM_VALUE));
            values.put("clm_test_step", safeGet(rowData, Constants.JsonKeys.CLM_TEST_STEP));
            values.put("clm_test_id", safeGet(rowData, Constants.JsonKeys.CLM_TEST_ID));
            values.put("clm_test_sec", safeGet(rowData, Constants.JsonKeys.CLM_TEST_SEC));
            values.put("clm_product_serial_no", safeGet(rowData, Constants.JsonKeys.CLM_PRODUCT_SERIAL_NO));
            values.put("clm_lower_value_watt", safeGet(rowData, Constants.JsonKeys.CLM_LOWER_VALUE_WATT));
            values.put("clm_upper_value_watt", safeGet(rowData, Constants.JsonKeys.CLM_UPPER_VALUE_WATT));
            values.put("clm_test_version_id", safeGet(rowData, Constants.JsonKeys.CLM_TEST_VERSION_ID));
            values.put("clm_model_id", safeGet(rowData, Constants.JsonKeys.CLM_MODEL_ID));

            db.insertWithOnConflict(tableName, null, values, SQLiteDatabase.CONFLICT_REPLACE);

//            Log.i(TAG, "> " + tableName + ".sql " + sql);
        }
        catch (Exception e) {
            Log.e(TAG, "> [D] TestData.insertTestSpecData.e.1 : " + e);
            return false;
        }
        finally {
            db.close();
            helper.close();
        }

//        selectTemperatureData(context, "1");

        return true;
    }

    public static boolean insertWattData(Context context, Map<String, String> rowData) {
        DBHelper helper = null;
        SQLiteDatabase db = null;
        String tableName = "tbl_test_watt_data";

        /*
        + "clm_test_seq primary key,"
        + "clm_line_id text,"
        + "clm_test_timestamp text,"
        + "clm_ellapsed_time_cnt text,"
        + "clm_watt_value text);";
        */

        try {
            helper = new DBHelper(context, "itf_temperature_table.db", null, DATABASE_VERSION);
            db = helper.getWritableDatabase();

            /*
            mapTestWatt.put("clm_test_history_seq", lmsTestSeq);
            mapTestWatt.put("line", lineId);
            mapTestWatt.put("unit_id", unitId);
            mapTestWatt.put("timestamp", currentTimestamp);
            mapTestWatt.put("ellapsed_time_cnt", String.valueOf(testItemTimeCnt));
            mapTestWatt.put("watt", String.valueOf(wattValue));
            */

            String sql = "";
            sql = "INSERT INTO " + tableName + "('clm_test_seq', 'clm_line_id', 'clm_unit_id', 'clm_test_timestamp', 'clm_ellapsed_time_cnt', 'clm_watt_value') values('" + rowData.get("clm_test_history_seq") + "', '" + rowData.get("line") + "', '" + rowData.get("unit_id") + "', '" + rowData.get("timestamp") + "', '" + rowData.get("ellapsed_time_cnt") + "', '" + rowData.get("watt") + "');";
            // Log.i(TAG, "> [D] " + tableName + ".insert.sql " + sql);
            db.execSQL(sql);
            db.close();

//            Log.i(TAG, "> " + tableName + ".sql " + sql);
        }
        catch (Exception e) {
            Log.e(TAG, "> [D] TestData.insertTestSpecData.e.1 : " + e);
            return false;
        }
        finally {
            db.close();
            helper.close();
        }

//        selectTemperatureData(context, "1");

        return true;
    }

    public static List<Map<String, String>> selectWattData(Context context, String queryCondition) {
        try {
            Log.i(TAG, "▶ [SI] bt connection handler search ON.2.0");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    DBHelper helper = null;
                    SQLiteDatabase db = null;
                    Cursor cur_data = null;
                    String tableName = "tbl_test_watt_data";
                    int rowCnt = 0;
                    Map<String, String> mapData = null;
                    // List<Map<String, String>> lstData = null;

                    try {
                        helper = new DBHelper(context, "itf_temperature_table.db", null, DATABASE_VERSION);

                        String sql = "";
                        db = helper.getReadableDatabase();
                        sql = "SELECT * FROM " + tableName + " WHERE 1=1 " + queryCondition + " ORDER BY clm_test_seq DESC;";
                        // sql = "SELECT * FROM " + tableName + " WHERE 1=1 ORDER BY clm_test_seq DESC;";
                        // Log.i(TAG, "> [D] sql " + sql);
                        cur_data = db.rawQuery(sql,null);

                        lstData = new ArrayList<Map<String, String>>();

                        while (cur_data.moveToNext()){
                            // sql = "INSERT INTO " + tableName + "('clm_test_seq', 'clm_line_id', 'clm_unit_id', 'clm_test_timestamp', 'clm_ellapsed_time_cnt', 'clm_watt_value')";
                            mapData = new HashMap<String, String>();
                            mapData.put("clm_test_seq", cur_data.getString(0));
                            mapData.put("clm_line_id", cur_data.getString(1));
                            mapData.put("clm_unit_id", cur_data.getString(2));
                            mapData.put("clm_test_timestamp", cur_data.getString(3));
                            mapData.put("clm_ellapsed_time_cnt", cur_data.getString(4));
                            mapData.put("clm_watt_value", cur_data.getString(5));
                            lstData.add(mapData);
                            rowCnt++;
                        }

                        // return lstData;
                    }
                    catch (Exception e) {
                        Log.e(TAG, "> TestData.selectWattData.e.1 : " + e);
                    }
                    finally {
                        cur_data.close();
                        helper.close();
                    }
                    // return lstData;
                }
            }).start();
        }
        catch (Exception e) {
            Log.e(TAG, "> TestData.selectWattData.e.1 : " + e);
        }

        return lstData;
    }

    public static List<Map<String, String>> selectTestSpecData(Context context, String queryCondition) {
        DBHelper helper = null;
        SQLiteDatabase db = null;
        Cursor cur_data = null;
        String tableName = "tbl_test_spec_data";
        int rowCnt = 0;
        Map<String, String> mapData = null;
        List<Map<String, String>> lstData = null;

        try {
            helper = new DBHelper(context, "itf_temperature_table.db", null, DATABASE_VERSION);

            String sql = "";
            db = helper.getReadableDatabase();
            sql = "SELECT * FROM " + tableName + " WHERE 1=1 " + queryCondition + " ORDER BY CAST(clm_test_step as integer) ASC;";
            // sql = "SELECT * FROM " + tableName + " WHERE 1=1 ORDER BY clm_test_seq DESC;";
            Log.i(TAG, "▶ [SQ] selectTestSpecData " + sql);
            cur_data = db.rawQuery(sql,null);

            lstData = new ArrayList<Map<String, String>>();

            while (cur_data.moveToNext()){
                mapData = new HashMap<String, String>();
                String testSeq = getColumnValue(cur_data, "clm_test_seq");
                String testCommand = getColumnValue(cur_data, "clm_test_command");
                String testName = getColumnValue(cur_data, "clm_test_name");
                String testType = getColumnValue(cur_data, "clm_test_type");
                String responseValue = getColumnValue(cur_data, "clm_test_response_value");
                String upperValue = getColumnValue(cur_data, "clm_test_upper_value");
                String lowerValue = getColumnValue(cur_data, "clm_test_lower_value");
                String upperValue02 = getColumnValue(cur_data, "clm_test_upper_value_02");
                String lowerValue02 = getColumnValue(cur_data, "clm_test_lower_value_02");
                String comment = getColumnValue(cur_data, "clm_comment");
                String valueWatt = getColumnValue(cur_data, "clm_value_watt");
                String value = getColumnValue(cur_data, "clm_value");
                String testStep = getColumnValue(cur_data, "clm_test_step");
                String testId = getColumnValue(cur_data, "clm_test_id");
                String testSec = getColumnValue(cur_data, "clm_test_sec");
                String productSerialNo = getColumnValue(cur_data, "clm_product_serial_no");
                String lowerValueWatt = getColumnValue(cur_data, "clm_lower_value_watt");
                if (lowerValueWatt.isEmpty()) {
                    lowerValueWatt = lowerValue02;
                }
                String upperValueWatt = getColumnValue(cur_data, "clm_upper_value_watt");
                if (upperValueWatt.isEmpty()) {
                    upperValueWatt = upperValue02;
                }
                String testVersionId = getColumnValue(cur_data, "clm_test_version_id");
                String modelId = getColumnValue(cur_data, "clm_model_id");

                mapData.put("clm_test_seq", testSeq);
                mapData.put(Constants.JsonKeys.CLM_TEST_SEQ, testSeq);
                mapData.put("clm_test_command", testCommand);
                mapData.put(Constants.JsonKeys.CLM_TEST_COMMAND, testCommand);
                mapData.put("clm_test_name", testName);
                mapData.put(Constants.JsonKeys.CLM_TEST_NAME, testName);
                mapData.put("clm_test_type", testType);
                mapData.put(Constants.JsonKeys.CLM_TEST_TYPE, testType);
                mapData.put("clm_test_response_value", responseValue);
                mapData.put(Constants.JsonKeys.CLM_RESPONSE_VALUE, responseValue);
                mapData.put("clm_test_upper_value", upperValue);
                mapData.put(Constants.JsonKeys.CLM_UPPER_VALUE, upperValue);
                mapData.put("clm_test_lower_value", lowerValue);
                mapData.put(Constants.JsonKeys.CLM_LOWER_VALUE, lowerValue);
                mapData.put("clm_test_upper_value_02", upperValue02);
                mapData.put("clm_test_lower_value_02", lowerValue02);
                mapData.put(Constants.JsonKeys.CLM_UPPER_VALUE_WATT, upperValueWatt);
                mapData.put(Constants.JsonKeys.CLM_LOWER_VALUE_WATT, lowerValueWatt);
                mapData.put("clm_comment", comment);
                mapData.put(Constants.JsonKeys.CLM_COMMENT, comment);
                mapData.put("clm_value_watt", valueWatt);
                mapData.put(Constants.JsonKeys.CLM_VALUE_WATT, valueWatt);
                mapData.put("clm_value", value);
                mapData.put(Constants.JsonKeys.CLM_VALUE, value);
                mapData.put("clm_test_step", testStep);
                mapData.put(Constants.JsonKeys.CLM_TEST_STEP, testStep);
                mapData.put("clm_test_id", testId);
                mapData.put(Constants.JsonKeys.CLM_TEST_ID, testId);
                mapData.put("clm_test_sec", testSec);
                mapData.put(Constants.JsonKeys.CLM_TEST_SEC, testSec);
                mapData.put("clm_product_serial_no", productSerialNo);
                mapData.put(Constants.JsonKeys.CLM_PRODUCT_SERIAL_NO, productSerialNo);
                mapData.put("clm_lower_value_watt", lowerValueWatt);
                mapData.put("clm_upper_value_watt", upperValueWatt);
                mapData.put("clm_test_version_id", testVersionId);
                mapData.put(Constants.JsonKeys.CLM_TEST_VERSION_ID, testVersionId);
                mapData.put("clm_model_id", modelId);
                mapData.put(Constants.JsonKeys.CLM_MODEL_ID, modelId);

                lstData.add(mapData);
                rowCnt++;
            }
        }
        catch (Exception e) {
            Log.e(TAG, "> TestData.selectTestSpecData.e.1 : " + e);
            // 에러 발생 시 빈 리스트 반환
            if (lstData == null) {
                lstData = new ArrayList<Map<String, String>>();
            }
        }
        finally {
            // Cursor가 null이 아닐 때만 close() 호출
            if (cur_data != null) {
                try {
                    cur_data.close();
                } catch (Exception e) {
                    Log.e(TAG, "> TestData.selectTestSpecData.finally.cur_data.close : " + e);
                }
            }
            if (helper != null) {
                try {
                    helper.close();
                } catch (Exception e) {
                    Log.e(TAG, "> TestData.selectTestSpecData.finally.helper.close : " + e);
                }
            }
        }

        // lstData가 null인 경우 빈 리스트 반환
        if (lstData == null) {
            lstData = new ArrayList<Map<String, String>>();
        }
        return lstData;
    }

    public static boolean deleteTestSpecData(Context context) {
        DBHelper helper = null;
        SQLiteDatabase db = null;
        String tableName = "tbl_test_spec_data";

        try {
            helper = new DBHelper(context, "itf_temperature_table.db", null, DATABASE_VERSION);
            db = helper.getWritableDatabase();

            String sql = "";
            sql = "DELETE FROM " + tableName + ";";
            db.execSQL(sql);
            Log.i(TAG, "> 1." + tableName + ".sql " + sql);
        }
        catch (Exception e) {
            Log.e(TAG, "> TestData.deleteTestData.e.2 : " + e);
            return false;
        }
        finally {
            db.close();
            helper.close();
        }

        return true;
    }

    public static boolean deleteLineInfo(Context context) {
        DBHelper helper = null;
        SQLiteDatabase db = null;
        String tableName = "tbl_line_info";

        try {
            helper = new DBHelper(context, "itf_temperature_table.db", null, DATABASE_VERSION);
            db = helper.getWritableDatabase();

            String sql = "";
            sql = "DELETE FROM " + tableName + ";";
            db.execSQL(sql);
            Log.i(TAG, "> 2." + tableName + ".sql " + sql);
        }
        catch (Exception e) {
            Log.e(TAG, "> TestData.deleteLineInfo.e.1 : " + e);
            return false;
        }
        finally {
            db.close();
            helper.close();
        }

        return true;
    }

    public static boolean insertLineInfo(Context context, Map<String, String> rowData) {
        DBHelper helper = null;
        SQLiteDatabase db = null;
        String tableName = "tbl_line_info";

        try {
            helper = new DBHelper(context, "itf_temperature_table.db", null, DATABASE_VERSION);
            db = helper.getWritableDatabase();

            String sql = "";
            sql = "INSERT INTO " + tableName + "('clm_line_id', 'clm_unit_id', 'clm_comment') values('" + rowData.get("clm_line_id") + "', '" + rowData.get("clm_unit_id") + "', '" + rowData.get("clm_comment") + "');";
            db.execSQL(sql);
            db.close();

//            Log.i(TAG, "> " + tableName + ".sql " + sql);
        }
        catch (Exception e) {
            Log.e(TAG, "> TestData.insertLineInfo.e.1 : " + e);
            return false;
        }
        finally {
            db.close();
            helper.close();
        }

//        selectTemperatureData(context, "1");

        return true;
    }

    public static boolean insertTemperatureData(Context context, String tableType, Map<String, String> rowData) {
        DBHelper helper = null;
        SQLiteDatabase db = null;
        String tableName = (tableType.equals("1")) ? "tbl_cold_temperature" : "tbl_hot_temperature";

        try {
            helper = new DBHelper(context, "itf_temperature_table.db", null, DATABASE_VERSION);
            db = helper.getWritableDatabase();

            String sql = "";
            sql = "INSERT INTO " + tableName + "('clm_temp_seq', 'clm_temperature', 'clm_regist', 'clm_voltage', 'clm_10_bit', 'clm_12_bit', 'clm_comment') values('" + rowData.get("clm_temp_seq") + "', '" + rowData.get("clm_temperature") + "', '" + rowData.get("clm_regist") + "', '" + rowData.get("clm_voltage") + "', '" + rowData.get("clm_10_bit") + "', '" + rowData.get("clm_12_bit") + "', '" + rowData.get("clm_comment") + "');";
            db.execSQL(sql);
            db.close();

//            Log.i(TAG, "> " + tableName + ".sql " + sql);
        }
        catch (Exception e) {
            Log.e(TAG, "> TestData.insertTemperatureData.e.1 : " + e);
            return false;
        }
        finally {
            db.close();
            helper.close();
        }

//        selectTemperatureData(context, "1");

        return true;
    }

    public static List<Map<String, String>> selectTestHistory(Context context, String query_condition) {
        DBHelper helper = null;
        SQLiteDatabase db = null;
        Cursor cur_temperature_data = null;
        String tableName = "tbl_test_history";
        int rowCnt = 0;
        Map<String, String> mapData = null;
        List<Map<String, String>> lstData = null;

        try {
            helper = new DBHelper(context, "itf_temperature_table.db", null, DATABASE_VERSION);

            String sql = "";
            db = helper.getReadableDatabase();
            sql = "SELECT * FROM " + tableName + " WHERE clm_test_model_name<>'null' ORDER BY clm_test_history_seq ASC;";
            Log.i(TAG, "> selectTestHistory " + tableName + ".sql " + sql);
            cur_temperature_data = db.rawQuery(sql,null);

            lstData = new ArrayList<Map<String, String>>();

            while (cur_temperature_data.moveToNext()){
                mapData = new HashMap<String, String>();
                mapData.put("clm_test_history_seq", cur_temperature_data.getString(0));
                mapData.put("clm_test_model_id", cur_temperature_data.getString(1));
                mapData.put("clm_test_model_name", cur_temperature_data.getString(2));
                mapData.put("clm_test_model_nationality", cur_temperature_data.getString(3));
                mapData.put("clm_test_timestamp", cur_temperature_data.getString(4));
                mapData.put("clm_test_result", cur_temperature_data.getString(5));
                mapData.put("clm_test_ng_count", cur_temperature_data.getString(6));
                mapData.put("clm_test_ok_count", cur_temperature_data.getString(7));
                mapData.put("clm_comment", cur_temperature_data.getString(8));
                lstData.add(mapData);
                rowCnt++;
            }
        }
        catch (Exception e) {
            Log.e(TAG, "> TestData.selectTestHistory.e.1 : " + e);
        }
        finally {
            cur_temperature_data.close();
            helper.close();
        }

        return lstData;
    }

    public static List<Map<String, String>> selectTemperatureData(Context context, String tableType) {
        DBHelper helper = null;
        SQLiteDatabase db = null;
        Cursor cur_temperature_data = null;
        String tableName = (tableType.equals("1")) ? "tbl_cold_temperature" : "tbl_hot_temperature";
        int rowCnt = 0;
        Map<String, String> mapTemperature = null;
        List<Map<String, String>> lstTemperature = null;

        try {
            helper = new DBHelper(context, "itf_temperature_table.db", null, DATABASE_VERSION);

            String sql = "";
            db = helper.getReadableDatabase();
            sql = "SELECT * FROM " + tableName + " ORDER BY CAST(clm_temperature as integer) DESC;";
            cur_temperature_data = db.rawQuery(sql,null);
//            Log.i(TAG, "> select." + tableName + ".sql " + sql);
//            Log.i(TAG, "> " + tableName + ".sql " + cur_temperature_data.getCount());

            lstTemperature = new ArrayList<Map<String, String>>();

            while (cur_temperature_data.moveToNext()){
                mapTemperature = new HashMap<String, String>();
//                Log.i(TAG, "> selectTemperatureData." + rowCnt + " " + cur_temperature_data.getString(0) + " " + cur_temperature_data.getString(1) + " " + cur_temperature_data.getString(2) + " " + cur_temperature_data.getString(3) + " " + cur_temperature_data.getString(4) + " " + cur_temperature_data.getString(5) + " " + cur_temperature_data.getString(6));
                mapTemperature.put("clm_temp_seq", cur_temperature_data.getString(0));
                mapTemperature.put("clm_temperature", cur_temperature_data.getString(1));
                mapTemperature.put("clm_regist", cur_temperature_data.getString(2));
                mapTemperature.put("clm_voltage", cur_temperature_data.getString(3));
                mapTemperature.put("clm_10_bit", cur_temperature_data.getString(4));
                mapTemperature.put("clm_12_bit", cur_temperature_data.getString(5));
                mapTemperature.put("clm_comment", cur_temperature_data.getString(6));
                lstTemperature.add(mapTemperature);
                rowCnt++;
            }
        }
        catch (Exception e) {
            Log.e(TAG, "> TestData.selectTemperatureData.e.1 : " + e);
        }
        finally {
            cur_temperature_data.close();
            helper.close();
        }

        return lstTemperature;
    }

    private static void addColumnIfMissing(SQLiteDatabase db, String tableName, String columnName, String columnType) {
        if (db == null || tableName == null || columnName == null || columnType == null) {
            return;
        }

        if (columnExists(db, tableName, columnName)) {
            return;
        }

        String alterSql = "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType + ";";
        db.execSQL(alterSql);
        Log.i(TAG, "> " + tableName + ".ALTER TABLE sql " + alterSql);
    }

    private static boolean columnExists(SQLiteDatabase db, String tableName, String columnName) {
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("PRAGMA table_info(" + tableName + ")", null);
            int nameIndex = cursor.getColumnIndex("name");
            while (cursor.moveToNext()) {
                if (columnName.equalsIgnoreCase(cursor.getString(nameIndex))) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "> columnExists error : " + e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return false;
    }

    private static void ensureTestSpecColumns(SQLiteDatabase db, String tableName) {
        addColumnIfMissing(db, tableName, "clm_test_step", "text");
        addColumnIfMissing(db, tableName, "clm_test_id", "text");
        addColumnIfMissing(db, tableName, "clm_test_sec", "text");
        addColumnIfMissing(db, tableName, "clm_product_serial_no", "text");
        addColumnIfMissing(db, tableName, "clm_lower_value_watt", "text");
        addColumnIfMissing(db, tableName, "clm_upper_value_watt", "text");
        addColumnIfMissing(db, tableName, "clm_test_version_id", "text");
        addColumnIfMissing(db, tableName, "clm_model_id", "text");
    }

    private static String safeGet(Map<String, String> map, String key) {
        if (map == null || key == null) {
            return "";
        }
        String value = map.get(key);
        return value == null ? "" : value;
    }

    private static String getColumnValue(Cursor cursor, String columnName) {
        if (cursor == null || columnName == null) {
            return "";
        }
        int index = cursor.getColumnIndex(columnName);
        if (index == -1) {
            return "";
        }
        String value = cursor.getString(index);
        return value == null ? "" : value;
    }

    /**
     * tbl_setting_info 테이블에 설정 정보 저장 (INSERT/UPDATE)
     * clm_setting_id가 이미 존재하면 UPDATE, 없으면 INSERT
     */
    public static boolean insertSettingInfo(Context context, Map<String, String> settingData) {
        DBHelper helper = null;
        SQLiteDatabase db = null;
        String tableName = "tbl_setting_info";

        try {
            helper = new DBHelper(context, "itf_temperature_table.db", null, DATABASE_VERSION);
            db = helper.getWritableDatabase();

            String settingSeq = safeGet(settingData, "clm_setting_seq");
            String settingId = safeGet(settingData, "clm_setting_id");
            String settingValue = safeGet(settingData, "clm_setting_value");
            String comment = safeGet(settingData, "clm_comment");
            String testTimestamp = safeGet(settingData, "clm_test_timestamp");

            // clm_setting_id가 이미 존재하는지 확인
            Cursor cursor = null;
            boolean exists = false;
            try {
                String query = "SELECT COUNT(*) FROM " + tableName + " WHERE clm_setting_id = ?";
                cursor = db.rawQuery(query, new String[]{settingId});
                if (cursor != null && cursor.moveToFirst()) {
                    exists = cursor.getInt(0) > 0;
                }
            } catch (Exception e) {
                Log.e(TAG, "> TestData.insertSettingInfo.checkExists.e.1 : " + e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            ContentValues values = new ContentValues();
            if (!settingSeq.isEmpty()) {
                values.put("clm_setting_seq", settingSeq);
            }
            values.put("clm_setting_id", settingId);
            values.put("clm_setting_value", settingValue);
            values.put("clm_comment", comment);
            values.put("clm_test_timestamp", testTimestamp);

            if (exists) {
                // UPDATE
                int rowsAffected = db.update(tableName, values, "clm_setting_id = ?", new String[]{settingId});
                Log.i(TAG, "> TestData.insertSettingInfo.UPDATE: clm_setting_id=" + settingId + ", rowsAffected=" + rowsAffected);
            } else {
                // INSERT
                long rowId = db.insert(tableName, null, values);
                Log.i(TAG, "> TestData.insertSettingInfo.INSERT: clm_setting_id=" + settingId + ", rowId=" + rowId);
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "> TestData.insertSettingInfo.e.1 : " + e);
            return false;
        } finally {
            if (db != null) {
                db.close();
            }
            if (helper != null) {
                helper.close();
            }
        }
    }

    /**
     * tbl_setting_info 테이블에서 clm_setting_seq로 조회
     */
    public static List<Map<String, String>> selectSettingInfoBySeq(Context context, String settingSeq) {
        DBHelper helper = null;
        SQLiteDatabase db = null;
        Cursor cursor = null;
        String tableName = "tbl_setting_info";
        List<Map<String, String>> lstData = null;

        try {
            helper = new DBHelper(context, "itf_temperature_table.db", null, DATABASE_VERSION);
            db = helper.getReadableDatabase();

            String sql = "SELECT * FROM " + tableName + " WHERE clm_setting_seq = ?";
            cursor = db.rawQuery(sql, new String[]{settingSeq});

            lstData = new ArrayList<Map<String, String>>();

            while (cursor.moveToNext()) {
                Map<String, String> mapData = new HashMap<String, String>();
                mapData.put("clm_setting_seq", getColumnValue(cursor, "clm_setting_seq"));
                mapData.put("clm_setting_id", getColumnValue(cursor, "clm_setting_id"));
                mapData.put("clm_setting_value", getColumnValue(cursor, "clm_setting_value"));
                mapData.put("clm_comment", getColumnValue(cursor, "clm_comment"));
                mapData.put("clm_test_timestamp", getColumnValue(cursor, "clm_test_timestamp"));
                lstData.add(mapData);
            }
        } catch (Exception e) {
            Log.e(TAG, "> TestData.selectSettingInfoBySeq.e.1 : " + e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (helper != null) {
                helper.close();
            }
        }

        if (lstData == null) {
            lstData = new ArrayList<Map<String, String>>();
        }
        return lstData;
    }

    /**
     * tbl_setting_info 테이블의 전체 설정 목록 조회
     */
    public static List<Map<String, String>> selectSettingInfo(Context context) {
        DBHelper helper = null;
        SQLiteDatabase db = null;
        Cursor cursor = null;
        String tableName = "tbl_setting_info";
        List<Map<String, String>> lstData = null;

        try {
            helper = new DBHelper(context, "itf_temperature_table.db", null, DATABASE_VERSION);
            db = helper.getReadableDatabase();

            // setting_seq가 숫자 문자열인 경우가 많아서 정렬 안정성을 위해 INTEGER 캐스팅 시도
            // 컬럼이 없거나 캐스팅이 실패하더라도 앱이 죽지 않도록 기본 정렬도 함께 둠
            String sql = "SELECT * FROM " + tableName + " ORDER BY CAST(clm_setting_seq AS INTEGER) ASC, clm_setting_id ASC";
            cursor = db.rawQuery(sql, null);

            lstData = new ArrayList<Map<String, String>>();
            while (cursor.moveToNext()) {
                Map<String, String> mapData = new HashMap<String, String>();
                mapData.put("clm_setting_seq", getColumnValue(cursor, "clm_setting_seq"));
                mapData.put("clm_setting_id", getColumnValue(cursor, "clm_setting_id"));
                mapData.put("clm_setting_value", getColumnValue(cursor, "clm_setting_value"));
                mapData.put("clm_comment", getColumnValue(cursor, "clm_comment"));
                mapData.put("clm_test_timestamp", getColumnValue(cursor, "clm_test_timestamp"));

                // (선택) 컬럼이 없으면 getColumnValue가 ""를 반환
                mapData.put("clm_setting_name_kr", getColumnValue(cursor, "clm_setting_name_kr"));
                mapData.put("clm_setting_name_en", getColumnValue(cursor, "clm_setting_name_en"));

                lstData.add(mapData);
            }
        } catch (Exception e) {
            Log.e(TAG, "> TestData.selectSettingInfo.e.1 : " + e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (helper != null) {
                helper.close();
            }
            if (db != null) {
                db.close();
            }
        }

        if (lstData == null) {
            lstData = new ArrayList<Map<String, String>>();
        }
        return lstData;
    }

    /**
     * tbl_setting_info 테이블에서 clm_setting_id로 설정 값 조회
     * @param context 컨텍스트
     * @param settingId 설정 ID
     * @return 설정 값 (없으면 null)
     */
    public static String getSettingValue(Context context, String settingId) {
        DBHelper helper = null;
        SQLiteDatabase db = null;
        Cursor cursor = null;
        String tableName = "tbl_setting_info";
        String value = null;

        try {
            helper = new DBHelper(context, "itf_temperature_table.db", null, DATABASE_VERSION);
            db = helper.getReadableDatabase();

            String sql = "SELECT clm_setting_value FROM " + tableName + " WHERE clm_setting_id = ?";
            cursor = db.rawQuery(sql, new String[]{settingId});

            if (cursor != null && cursor.moveToFirst()) {
                value = getColumnValue(cursor, "clm_setting_value");
            }
        } catch (Exception e) {
            Log.e(TAG, "> TestData.getSettingValue.e.1 : " + e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (helper != null) {
                helper.close();
            }
            if (db != null) {
                db.close();
            }
        }

        return value;
    }

    /**
     * tbl_setting_info 테이블에서 데이터 삭제
     */
    public static boolean deleteSettingInfo(Context context, String settingId) {
        DBHelper helper = null;
        SQLiteDatabase db = null;
        String tableName = "tbl_setting_info";

        try {
            helper = new DBHelper(context, "itf_temperature_table.db", null, DATABASE_VERSION);
            db = helper.getWritableDatabase();

            int rowsAffected = db.delete(tableName, "clm_setting_id = ?", new String[]{settingId});
            Log.i(TAG, "> TestData.deleteSettingInfo: clm_setting_id=" + settingId + ", rowsAffected=" + rowsAffected);

            return true;
        } catch (Exception e) {
            Log.e(TAG, "> TestData.deleteSettingInfo.e.1 : " + e);
            return false;
        } finally {
            if (db != null) {
                db.close();
            }
            if (helper != null) {
                helper.close();
            }
        }
    }

    /**
     * tbl_model_info 테이블에 모델 정보 저장 (INSERT/UPDATE)
     * 중복 판정 키: clm_model_id (요청사항 반영)
     */
    public static boolean insertModelInfo(Context context, Map<String, String> modelData) {
        DBHelper helper = null;
        SQLiteDatabase db = null;
        String tableName = "tbl_model_info";

        try {
            helper = new DBHelper(context, "itf_temperature_table.db", null, DATABASE_VERSION);
            db = helper.getWritableDatabase();

            String modelSeq = safeGet(modelData, "clm_model_seq");
            String clientName = safeGet(modelData, "clm_client_name");
            String clientId = safeGet(modelData, "clm_client_id");
            String testStep = safeGet(modelData, "clm_test_step");
            String companyKey = safeGet(modelData, "clm_company_key");
            String modelId = safeGet(modelData, "clm_model_id");
            String modelName = safeGet(modelData, "clm_model_name");
            String modelVersion = safeGet(modelData, "clm_model_version");
            String comment = safeGet(modelData, "clm_comment");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String currentTimestamp = sdf.format(new Date());

            // 서버가 동일 clm_model_seq를 여러 모델에 사용할 수 있으므로
            // PK 충돌 방지를 위해 clm_model_seq가 비었으면 model_id를 사용
            if (modelSeq.isEmpty() && !modelId.isEmpty()) {
                modelSeq = modelId;
            }

            // clm_model_id가 이미 존재하는지 확인 (중복 판정 키 변경)
            Cursor cursor = null;
            boolean exists = false;
            Map<String, String> oldData = null;
            try {
                String query = "SELECT * FROM " + tableName + " WHERE clm_model_id = ?";
                cursor = db.rawQuery(query, new String[]{modelId});
                if (cursor != null && cursor.moveToFirst()) {
                    exists = true;
                    oldData = new HashMap<>();
                    oldData.put("clm_model_seq", getColumnValue(cursor, "clm_model_seq"));
                    oldData.put("clm_client_name", getColumnValue(cursor, "clm_client_name"));
                    oldData.put("clm_client_id", getColumnValue(cursor, "clm_client_id"));
                    oldData.put("clm_test_step", getColumnValue(cursor, "clm_test_step"));
                    oldData.put("clm_company_key", getColumnValue(cursor, "clm_company_key"));
                    oldData.put("clm_model_id", getColumnValue(cursor, "clm_model_id"));
                    oldData.put("clm_model_name", getColumnValue(cursor, "clm_model_name"));
                    oldData.put("clm_model_version", getColumnValue(cursor, "clm_model_version"));
                    oldData.put("clm_comment", getColumnValue(cursor, "clm_comment"));
                }
            } catch (Exception e) {
                Log.e(TAG, "> TestData.insertModelInfo.checkExists.e.1 : " + e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            ContentValues values = new ContentValues();
            values.put("clm_model_seq", modelSeq);
            values.put("clm_client_name", clientName);
            values.put("clm_client_id", clientId);
            values.put("clm_test_step", testStep);
            values.put("clm_company_key", companyKey);
            values.put("clm_model_id", modelId);
            values.put("clm_model_name", modelName);
            values.put("clm_model_version", modelVersion);
            values.put("clm_comment", comment);
            values.put("clm_update_timestamp", currentTimestamp);

            if (exists) {
                // UPDATE - 변경사항이 있으면 버전 히스토리에 저장
                if (oldData != null && hasModelInfoChanged(oldData, modelData)) {
                    insertModelInfoVersionHistory(context, oldData, "UPDATE", currentTimestamp);
                }
                values.put("clm_update_timestamp", currentTimestamp);
                db.update(tableName, values, "clm_model_id = ?", new String[]{modelId});
                Log.i(TAG, "> TestData.insertModelInfo: Updated model_id=" + modelId);
            } else {
                // INSERT
                values.put("clm_regist_timestamp", currentTimestamp);
                values.put("clm_update_timestamp", currentTimestamp);
                db.insert(tableName, null, values);
                insertModelInfoVersionHistory(context, modelData, "INSERT", currentTimestamp);
                Log.i(TAG, "> TestData.insertModelInfo: Inserted model_id=" + modelId);
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "> TestData.insertModelInfo.e.1 : " + e);
            return false;
        } finally {
            if (db != null) {
                db.close();
            }
            if (helper != null) {
                helper.close();
            }
        }
    }

    /**
     * 모델 정보 변경 여부 확인
     */
    private static boolean hasModelInfoChanged(Map<String, String> oldData, Map<String, String> newData) {
        String[] fields = {"clm_client_name", "clm_client_id", "clm_test_step", "clm_company_key",
                "clm_model_id", "clm_model_name", "clm_model_version", "clm_comment"};
        for (String field : fields) {
            String oldValue = safeGet(oldData, field);
            String newValue = safeGet(newData, field);
            if (!oldValue.equals(newValue)) {
                return true;
            }
        }
        return false;
    }

    /**
     * tbl_model_info_version_history 테이블에 버전 히스토리 저장
     */
    private static boolean insertModelInfoVersionHistory(Context context, Map<String, String> modelData, String changeType, String changeTimestamp) {
        DBHelper helper = null;
        SQLiteDatabase db = null;
        String tableName = "tbl_model_info_version_history";

        try {
            helper = new DBHelper(context, "itf_temperature_table.db", null, DATABASE_VERSION);
            db = helper.getWritableDatabase();

            // 버전 번호 생성 (현재 최대 버전 + 1) - 기준: clm_model_id
            String modelId = safeGet(modelData, "clm_model_id");
            String versionNumber = getNextVersionNumber(db, tableName, "clm_model_id", modelId);

            ContentValues values = new ContentValues();
            values.put("clm_model_seq", safeGet(modelData, "clm_model_seq"));
            values.put("clm_client_name", safeGet(modelData, "clm_client_name"));
            values.put("clm_client_id", safeGet(modelData, "clm_client_id"));
            values.put("clm_test_step", safeGet(modelData, "clm_test_step"));
            values.put("clm_company_key", safeGet(modelData, "clm_company_key"));
            values.put("clm_model_id", safeGet(modelData, "clm_model_id"));
            values.put("clm_model_name", safeGet(modelData, "clm_model_name"));
            values.put("clm_model_version", safeGet(modelData, "clm_model_version"));
            values.put("clm_comment", safeGet(modelData, "clm_comment"));
            values.put("clm_version_number", versionNumber);
            values.put("clm_change_type", changeType);
            values.put("clm_change_timestamp", changeTimestamp);

            db.insert(tableName, null, values);
            Log.i(TAG, "> TestData.insertModelInfoVersionHistory: Saved version history for model_id=" + modelId);

            return true;
        } catch (Exception e) {
            Log.e(TAG, "> TestData.insertModelInfoVersionHistory.e.1 : " + e);
            return false;
        } finally {
            if (db != null) {
                db.close();
            }
            if (helper != null) {
                helper.close();
            }
        }
    }

    /**
     * 다음 버전 번호 가져오기
     */
    private static String getNextVersionNumber(SQLiteDatabase db, String tableName, String keyColumn, String keyValue) {
        Cursor cursor = null;
        try {
            String query = "SELECT MAX(CAST(clm_version_number AS INTEGER)) FROM " + tableName + " WHERE " + keyColumn + " = ?";
            cursor = db.rawQuery(query, new String[]{keyValue});
            if (cursor != null && cursor.moveToFirst() && !cursor.isNull(0)) {
                int maxVersion = cursor.getInt(0);
                return String.valueOf(maxVersion + 1);
            }
        } catch (Exception e) {
            Log.e(TAG, "> TestData.getNextVersionNumber.e.1 : " + e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return "1";
    }

    /**
     * tbl_test_spec_data 테이블 업데이트 시 버전 히스토리 저장
     * 기존 insertTestSpecData 메서드를 수정하여 버전 관리 기능 추가
     */
    public static boolean insertTestSpecDataWithVersion(Context context, Map<String, String> rowData) {
        DBHelper helper = null;
        SQLiteDatabase db = null;
        String tableName = "tbl_test_spec_data";

        try {
            helper = new DBHelper(context, "itf_temperature_table.db", null, DATABASE_VERSION);
            db = helper.getWritableDatabase();

            String testSeq = safeGet(rowData, "clm_test_seq");

            // 기존 데이터 확인
            Cursor cursor = null;
            boolean exists = false;
            Map<String, String> oldData = null;
            try {
                String query = "SELECT * FROM " + tableName + " WHERE clm_test_seq = ?";
                cursor = db.rawQuery(query, new String[]{testSeq});
                if (cursor != null && cursor.moveToFirst()) {
                    exists = true;
                    oldData = new HashMap<>();
                    String[] columns = cursor.getColumnNames();
                    for (String column : columns) {
                        oldData.put(column, getColumnValue(cursor, column));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "> TestData.insertTestSpecDataWithVersion.checkExists.e.1 : " + e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String currentTimestamp = sdf.format(new Date());

            ContentValues values = new ContentValues();
            for (Map.Entry<String, String> entry : rowData.entrySet()) {
                values.put(entry.getKey(), entry.getValue());
            }

            if (exists) {
                // UPDATE - 변경사항이 있으면 버전 히스토리에 저장
                if (oldData != null && hasTestSpecDataChanged(oldData, rowData)) {
                    insertTestSpecDataVersionHistory(context, oldData, "UPDATE", currentTimestamp);
                }
                db.update(tableName, values, "clm_test_seq = ?", new String[]{testSeq});
                Log.i(TAG, "> TestData.insertTestSpecDataWithVersion: Updated test_seq=" + testSeq);
            } else {
                // INSERT
                db.insert(tableName, null, values);
                insertTestSpecDataVersionHistory(context, rowData, "INSERT", currentTimestamp);
                Log.i(TAG, "> TestData.insertTestSpecDataWithVersion: Inserted test_seq=" + testSeq);
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "> TestData.insertTestSpecDataWithVersion.e.1 : " + e);
            return false;
        } finally {
            if (db != null) {
                db.close();
            }
            if (helper != null) {
                helper.close();
            }
        }
    }

    /**
     * 테스트 스펙 데이터 변경 여부 확인
     */
    private static boolean hasTestSpecDataChanged(Map<String, String> oldData, Map<String, String> newData) {
        for (Map.Entry<String, String> entry : newData.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("clm_")) {
                String oldValue = safeGet(oldData, key);
                String newValue = safeGet(newData, key);
                if (!oldValue.equals(newValue)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * tbl_test_spec_data_version_history 테이블에 버전 히스토리 저장
     */
    private static boolean insertTestSpecDataVersionHistory(Context context, Map<String, String> testSpecData, String changeType, String changeTimestamp) {
        DBHelper helper = null;
        SQLiteDatabase db = null;
        String tableName = "tbl_test_spec_data_version_history";

        try {
            helper = new DBHelper(context, "itf_temperature_table.db", null, DATABASE_VERSION);
            db = helper.getWritableDatabase();

            String testSeq = safeGet(testSpecData, "clm_test_seq");
            String versionNumber = getNextVersionNumber(db, tableName, "clm_test_seq", testSeq);

            ContentValues values = new ContentValues();
            for (Map.Entry<String, String> entry : testSpecData.entrySet()) {
                if (entry.getKey().startsWith("clm_")) {
                    values.put(entry.getKey(), entry.getValue());
                }
            }
            values.put("clm_version_number", versionNumber);
            values.put("clm_change_type", changeType);
            values.put("clm_change_timestamp", changeTimestamp);

            db.insert(tableName, null, values);
            Log.i(TAG, "> TestData.insertTestSpecDataVersionHistory: Saved version history for test_seq=" + testSeq);

            return true;
        } catch (Exception e) {
            Log.e(TAG, "> TestData.insertTestSpecDataVersionHistory.e.1 : " + e);
            return false;
        } finally {
            if (db != null) {
                db.close();
            }
            if (helper != null) {
                helper.close();
            }
        }
    }

    /**
     * 초기 문자열 리소스 데이터 마이그레이션
     * strings.xml과 Constants.java의 문자열을 tbl_string_resources에 삽입
     * 이미 존재하는 키는 업데이트하지 않음 (기존 번역 보존)
     */
    public static void migrateInitialStringResources(Context context) {
        DBHelper helper = null;
        SQLiteDatabase db = null;

        try {
            helper = new DBHelper(context, "itf_temperature_table.db", null, DATABASE_VERSION);
            db = helper.getWritableDatabase();

            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

            // 초기 문자열 데이터 배열
            String[][] initialStrings = {
                // UI 버튼
                {"button.close", "button", "닫 기", "Close", "", "", "닫기 버튼", "1", timestamp, timestamp},
                {"button.confirm", "button", "확 인", "Confirm", "", "", "확인 버튼", "1", timestamp, timestamp},
                {"button.change", "button", "수정", "Edit", "", "", "수정 버튼", "1", timestamp, timestamp},
                {"button.detail", "button", "상세", "Detail", "", "", "상세 버튼", "1", timestamp, timestamp},
                
                // UI 화면
                {"ui.screen.test_history.title", "ui", "검사 이력", "Test History", "", "", "검사 이력 화면 제목", "1", timestamp, timestamp},
                {"ui.screen.system_setting.title", "ui", "시스템 설정", "System Settings", "", "", "시스템 설정 화면 제목", "1", timestamp, timestamp},
                {"ui.screen.test_model_list.title", "ui", "검사 모델", "Test Model List", "", "", "검사 모델 목록 화면 제목", "1", timestamp, timestamp},
                
                // UI 라벨
                {"ui.label.test_line", "ui", "검사라인", "Test Line", "", "", "검사라인 라벨", "1", timestamp, timestamp},
                {"ui.label.test_result", "ui", "검사결과", "Test Result", "", "", "검사결과 라벨", "1", timestamp, timestamp},
                {"ui.label.test_model", "ui", "검사모델", "Test Model", "", "", "검사모델 라벨", "1", timestamp, timestamp},
                {"ui.label.model_name", "ui", "모델명", "Model Name", "", "", "모델명 라벨", "1", timestamp, timestamp},
                {"ui.label.client_name", "ui", "고객명", "Client Name", "", "", "고객명 라벨", "1", timestamp, timestamp},
                {"ui.label.nationality", "ui", "국가", "Nationality", "", "", "국가 라벨", "1", timestamp, timestamp},
                {"ui.label.setting_datetime", "ui", "설정일시", "Setting DateTime", "", "", "설정일시 라벨", "1", timestamp, timestamp},
                {"ui.label.test_datetime", "ui", "검사일시", "Test DateTime", "", "", "검사일시 라벨", "1", timestamp, timestamp},
                {"ui.label.test_comment", "ui", "특이사항", "Comment", "", "", "특이사항 라벨", "1", timestamp, timestamp},
                {"ui.label.test_process_list", "ui", "검사항목", "Test Process List", "", "", "검사항목 라벨", "1", timestamp, timestamp},
                {"ui.label.start_time", "ui", "시작시간 : ", "Start Time: ", "", "", "시작시간 라벨", "1", timestamp, timestamp},
                {"ui.label.total_spent_time", "ui", "소요시간 : ", "Total Time: ", "", "", "소요시간 라벨", "1", timestamp, timestamp},
                {"ui.label.watt", "ui", "소비전력(W)", "Power Consumption(W)", "", "", "소비전력 라벨", "1", timestamp, timestamp},
                {"ui.label.version", "ui", "버 전", "Version", "", "", "버전 라벨", "1", timestamp, timestamp},
                {"ui.label.ok", "ui", "OK", "OK", "", "", "OK 라벨", "1", timestamp, timestamp},
                {"ui.label.ng", "ui", "NG", "NG", "", "", "NG 라벨", "1", timestamp, timestamp},
                {"ui.label.no", "ui", "No", "No", "", "", "No 라벨", "1", timestamp, timestamp},
                {"ui.label.enabled", "ui", "Enabled", "Enabled", "", "", "활성화 라벨", "1", timestamp, timestamp},
                {"ui.label.disabled", "ui", "Disabled", "Disabled", "", "", "비활성화 라벨", "1", timestamp, timestamp},
                {"ui.label.pre_process", "ui", "검사전", "Pre Process", "", "", "검사전 라벨", "1", timestamp, timestamp},
                
                // 에러 메시지
                {"error.server.connection_failed", "error", "서버 연결 실패", "Server connection failed", "", "", "서버 연결 실패 에러", "1", timestamp, timestamp},
                {"error.bluetooth.connection_failed", "error", "블루투스 연결 실패", "Bluetooth connection failed", "", "", "블루투스 연결 실패 에러", "1", timestamp, timestamp},
                {"error.usb.connection_failed", "error", "USB 연결 실패", "USB connection failed", "", "", "USB 연결 실패 에러", "1", timestamp, timestamp},
                {"error.permission.denied", "error", "권한이 거부되었습니다", "Permission denied", "", "", "권한 거부 에러", "1", timestamp, timestamp},
                
                // UI 메시지
                {"ui.message.permission_request", "ui", "권한 요청에 동의 해주셔야 이용 가능합니다. 설정에서 권한 허용 하시기 바랍니다.", "Please agree to the permission request to use this feature. Please allow permissions in settings.", "", "", "권한 요청 메시지", "1", timestamp, timestamp},
                {"ui.message.bluetooth_enable", "ui", "태블릿의 블루투스 연결을 활성화 해주십시요.", "Please enable Bluetooth connection on the tablet.", "", "", "블루투스 활성화 메시지", "1", timestamp, timestamp},
                {"ui.message.no_received_data", "ui", "제품으로부터 수신된 신호가 없습니다.", "No signal received from product.", "", "", "수신 데이터 없음 메시지", "1", timestamp, timestamp},
                {"ui.message.server_connection_failed", "ui", "서버 연결 실패", "Server connection failed", "", "", "서버 연결 실패 메시지", "1", timestamp, timestamp},
                {"ui.message.test_control_disabled", "ui", "테스트 중에는 제어가 불가능합니다.", "Control is not available during testing.", "", "", "테스트 중 제어 불가 메시지", "1", timestamp, timestamp},
                {"ui.message.control_off_only_by_owner", "ui", "제어 OFF는 제어 ON을 한 사용자만 가능합니다.", "Control OFF can only be done by the user who turned Control ON.", "", "", "제어 OFF 권한 메시지", "1", timestamp, timestamp},
                {"ui.message.check_bluetooth_usb", "ui", "블루투스 또는 USB 연결을 확인해주세요.", "Please check Bluetooth or USB connection.", "", "", "연결 확인 메시지", "1", timestamp, timestamp},
                {"ui.message.control_ready", "ui", "제어 준비 완료", "Control ready", "", "", "제어 준비 완료 메시지", "1", timestamp, timestamp},
                {"ui.message.control_mode_test", "ui", "제어 모드 검사", "Control Mode Test", "", "", "제어 모드 검사 제목", "1", timestamp, timestamp},
                {"ui.message.bluetooth_permission_required", "ui", "블루투스 권한 필요", "Bluetooth Permission Required", "", "", "블루투스 권한 필요 제목", "1", timestamp, timestamp},
                {"ui.message.bluetooth_permission_blocked", "ui", "블루투스 권한이 필요합니다.\n\n시스템에서 권한 요청이 차단되었습니다.\n\n설정 화면에서 직접 권한을 허용해주세요.\n\n설정 화면으로 이동하시겠습니까?", "Bluetooth permission is required.\n\nThe permission request has been blocked by the system.\n\nPlease allow the permission directly in the settings screen.\n\nWould you like to go to the settings screen?", "", "", "블루투스 권한 차단 메시지", "1", timestamp, timestamp},
                {"ui.message.bluetooth_permission_denied", "ui", "블루투스 권한이 거부되었습니다.\n\n권한을 허용해야 기기가 정상 동작합니다.\n\n아래 '권한 허용' 버튼을 눌러 권한을 허용해주세요.", "Bluetooth permission has been denied.\n\nPermission must be granted for the device to work properly.\n\nPlease click the 'Allow Permission' button below to grant permission.", "", "", "블루투스 권한 거부 메시지", "1", timestamp, timestamp},
                {"ui.message.allow_permission", "ui", "권한 허용", "Allow Permission", "", "", "권한 허용 버튼", "1", timestamp, timestamp},
                {"ui.message.exit", "ui", "종료", "Exit", "", "", "종료 버튼", "1", timestamp, timestamp},
                {"ui.message.go_to_settings", "ui", "설정 화면으로 이동", "Go to Settings", "", "", "설정 화면 이동 버튼", "1", timestamp, timestamp},
                {"ui.message.command_timeout", "ui", "명령: %s\n응답 시간 초과, 일정 시간 동안 블루투스로부터 응답을 받지 못했습니다.", "Command: %s\nResponse timeout, no response received from Bluetooth for a certain period.", "", "", "명령 타임아웃 메시지", "1", timestamp, timestamp},
                {"ui.message.command_response", "ui", "명령: %s\n응답: %s", "Command: %s\nResponse: %s", "", "", "명령 응답 메시지", "1", timestamp, timestamp},
                {"ui.message.control_command", "ui", "제어 명령어: %s", "Control Command: %s", "", "", "제어 명령어 라벨", "1", timestamp, timestamp},
                {"ui.message.received_command", "ui", "수신 명령어: %s", "Received Command: %s", "", "", "수신 명령어 라벨", "1", timestamp, timestamp},
                {"ui.message.received_response", "ui", "수신 응답: %s", "Received Response: %s", "", "", "수신 응답 라벨", "1", timestamp, timestamp},
                {"ui.message.power_consumption", "ui", "소비전력: %s", "Power Consumption: %s", "", "", "소비전력 라벨", "1", timestamp, timestamp},
                {"ui.message.temperature", "ui", "온도: %s", "Temperature: %s", "", "", "온도 라벨", "1", timestamp, timestamp},
                {"ui.message.result_value", "ui", "결과 값: %s", "Result Value: %s", "", "", "결과 값 라벨", "1", timestamp, timestamp},
                {"ui.message.test_result", "ui", "검사 결과: %s", "Test Result: %s", "", "", "검사 결과 라벨", "1", timestamp, timestamp},
                {"ui.label.none", "ui", "없음", "None", "", "", "없음 라벨", "1", timestamp, timestamp},
                {"ui.message.open_settings", "ui", "설정 열기", "Open Settings", "", "", "설정 열기 버튼", "1", timestamp, timestamp},
                {"ui.message.no_paired_bluetooth", "ui", "페어링된 블루투스 장비가 없습니다!", "No paired Bluetooth devices!", "", "", "페어링된 블루투스 없음 메시지", "1", timestamp, timestamp},
                {"ui.message.save_confirm", "ui", "저장 확인", "Save Confirmation", "", "", "저장 확인 제목", "1", timestamp, timestamp},
                {"ui.message.save_success", "ui", "저장되었습니다.", "Saved successfully.", "", "", "저장 성공 메시지", "1", timestamp, timestamp},
                {"ui.message.save_failed", "ui", "저장에 실패했습니다.", "Save failed.", "", "", "저장 실패 메시지", "1", timestamp, timestamp},
                {"ui.message.save_error", "ui", "저장 중 오류가 발생했습니다.", "An error occurred while saving.", "", "", "저장 오류 메시지", "1", timestamp, timestamp},
                {"ui.message.email_app_saved", "ui", "기본 이메일 앱이 저장되었습니다.", "Default email app has been saved.", "", "", "이메일 앱 저장 성공", "1", timestamp, timestamp},
                {"ui.message.email_address_save_confirm", "ui", "메일 전달 이메일 주소를 저장하시겠습니까?\n\n%s", "Do you want to save the email address for mail delivery?\n\n%s", "", "", "이메일 주소 저장 확인", "1", timestamp, timestamp},
                {"ui.message.email_app_save_confirm", "ui", "기본 이메일 앱을 저장하시겠습니까?\n\n%s\n(%s)", "Do you want to save the default email app?\n\n%s\n(%s)", "", "", "이메일 앱 저장 확인", "1", timestamp, timestamp},
                {"ui.message.setting_value_save_confirm", "ui", "설정값을 저장하시겠습니까?\n\n%s", "Do you want to save the setting value?\n\n%s", "", "", "설정값 저장 확인", "1", timestamp, timestamp},
                {"ui.message.error.package_manager_unavailable", "ui", "패키지 매니저를 가져올 수 없습니다.", "Cannot get package manager.", "", "", "패키지 매니저 오류", "1", timestamp, timestamp},
                {"ui.message.error.email_app_not_found", "ui", "이메일 앱을 찾을 수 없습니다.", "Email app not found.", "", "", "이메일 앱 없음", "1", timestamp, timestamp},
                {"ui.message.error.no_app_selected", "ui", "선택된 앱이 없습니다.", "No app selected.", "", "", "앱 미선택", "1", timestamp, timestamp},
                {"ui.message.error.invalid_app_info", "ui", "선택된 앱 정보가 올바르지 않습니다.", "Selected app information is invalid.", "", "", "앱 정보 오류", "1", timestamp, timestamp},
                {"ui.message.error.loading_email_apps", "ui", "이메일 앱 목록을 불러오는 중 오류가 발생했습니다.", "An error occurred while loading email app list.", "", "", "이메일 앱 목록 로드 오류", "1", timestamp, timestamp},
                {"ui.message.error.email_required", "ui", "이메일 주소를 입력해 주세요.", "Please enter an email address.", "", "", "이메일 주소 필수", "1", timestamp, timestamp},
                {"ui.message.error.email_invalid", "ui", "이메일 형식이 올바르지 않습니다.", "Email format is invalid.", "", "", "이메일 형식 오류", "1", timestamp, timestamp},
                {"ui.message.error.setting_value_required", "ui", "설정값을 입력해 주세요.", "Please enter a setting value.", "", "", "설정값 필수", "1", timestamp, timestamp},
                {"ui.message.error.dialog_display", "ui", "다이얼로그 표시 중 오류가 발생했습니다.", "An error occurred while displaying the dialog.", "", "", "다이얼로그 표시 오류", "1", timestamp, timestamp},
                {"ui.message.error.language_selection_dialog", "ui", "언어 선택 다이얼로그 표시 중 오류가 발생했습니다.", "An error occurred while displaying the language selection dialog.", "", "", "언어 선택 다이얼로그 오류", "1", timestamp, timestamp},
                {"ui.message.error.language_save_failed", "ui", "언어 설정 저장에 실패했습니다.", "Failed to save language setting.", "", "", "언어 설정 저장 실패", "1", timestamp, timestamp},
                {"ui.message.error.language_save_error", "ui", "언어 설정 저장 중 오류가 발생했습니다.", "An error occurred while saving language setting.", "", "", "언어 설정 저장 오류", "1", timestamp, timestamp},
                {"ui.message.error.confirm_dialog_display", "ui", "확인 다이얼로그 표시 중 오류가 발생했습니다.", "An error occurred while displaying the confirmation dialog.", "", "", "확인 다이얼로그 오류", "1", timestamp, timestamp},
                {"ui.message.setting_id_empty", "ui", "setting_id가 비어있어 수정할 수 없습니다.", "Cannot edit because setting_id is empty.", "", "", "setting_id 비어있음", "1", timestamp, timestamp},
                {"ui.message.language_change_confirm", "ui", "언어를 변경하면 앱이 재시작됩니다.\n\n선택한 언어: %s\n(%s)", "The app will restart when you change the language.\n\nSelected language: %s\n(%s)", "", "", "언어 변경 확인", "1", timestamp, timestamp},
                {"ui.message.language_selection", "ui", "언어 선택 / Select Language", "Language Selection", "", "", "언어 선택 제목", "1", timestamp, timestamp},
                {"ui.message.vibration_amplitude_save_confirm", "ui", "진동 강도를 저장하시겠습니까?\n\n%s", "Do you want to save the vibration amplitude?\n\n%s", "", "", "진동 강도 저장 확인", "1", timestamp, timestamp},
                {"ui.message.vibration_amplitude_title", "ui", "진동 강도 변경", "Change Vibration Amplitude", "", "", "진동 강도 변경 제목", "1", timestamp, timestamp},
                {"ui.message.email_app_selection_title", "ui", "이메일 앱 선택", "Select Email App", "", "", "이메일 앱 선택 제목", "1", timestamp, timestamp},
                {"ui.message.test_email_edit_title", "ui", "메일 전달 이메일 주소 변경", "Change Email Address for Mail Delivery", "", "", "이메일 주소 변경 제목", "1", timestamp, timestamp},
                {"ui.message.setting_value_edit", "ui", "설정값 수정", "Edit Setting Value", "", "", "설정값 수정 제목", "1", timestamp, timestamp},
                {"ui.message.save", "ui", "저장", "Save", "", "", "저장 버튼", "1", timestamp, timestamp},
                {"ui.message.confirm", "ui", "확인", "Confirm", "", "", "확인 버튼", "1", timestamp, timestamp},
                 
                 // 로그 메시지
                 {"log.ps.initializing_managers", "log", "Initializing communication managers...", "Initializing communication managers...", "", "", "통신 관리자 초기화 중", "1", timestamp, timestamp},
                 {"log.ps.all_managers_initialized", "log", "All managers initialized successfully", "All managers initialized successfully", "", "", "모든 관리자 초기화 완료", "1", timestamp, timestamp},
                 {"log.ps.starting_managers", "log", "Starting communication managers...", "Starting communication managers...", "", "", "통신 관리자 시작 중", "1", timestamp, timestamp},
                 {"log.ps.cleaning_up_managers", "log", "Cleaning up communication managers...", "Cleaning up communication managers...", "", "", "통신 관리자 정리 중", "1", timestamp, timestamp},
                 {"log.ps.bluetooth_manager_cleaned", "log", "BluetoothManager cleaned up", "BluetoothManager cleaned up", "", "", "BluetoothManager 정리 완료", "1", timestamp, timestamp},
                 {"log.ps.network_manager_cleaned", "log", "NetworkManager cleaned up", "NetworkManager cleaned up", "", "", "NetworkManager 정리 완료", "1", timestamp, timestamp},
                 {"log.ps.manager_cleanup_complete", "log", "Manager cleanup complete", "Manager cleanup complete", "", "", "관리자 정리 완료", "1", timestamp, timestamp},
                 {"log.ps.all_timers_stopped", "log", "All timers stopped for control mode (USB polling timer kept running)", "All timers stopped for control mode (USB polling timer kept running)", "", "", "모든 타이머 중지", "1", timestamp, timestamp},
                 {"log.ps.force_exit_control_mode", "log", "Force exit control mode due to test restart", "Force exit control mode due to test restart", "", "", "테스트 재시작으로 제어 모드 강제 종료", "1", timestamp, timestamp},
                 {"log.ps.strictmode_enabled", "log", "✅ StrictMode enabled: ThreadPolicy (network, disk I/O) + VmPolicy (memory leaks)", "✅ StrictMode enabled: ThreadPolicy (network, disk I/O) + VmPolicy (memory leaks)", "", "", "StrictMode 활성화", "1", timestamp, timestamp},
                 {"log.ps.control_mode_st0101_received", "log", "Control mode: ST0101 received, setting control test ready state", "Control mode: ST0101 received, setting control test ready state", "", "", "제어 모드 ST0101 수신", "1", timestamp, timestamp},
                 {"log.ps.header_message_set", "log", "Header message set to '제어 준비 완료'", "Header message set to 'Control Ready'", "", "", "헤더 메시지 설정", "1", timestamp, timestamp},
                 {"log.ps.control_test_ready", "log", "Control test ready state set, waiting for web control commands", "Control test ready state set, waiting for web control commands", "", "", "제어 테스트 준비 상태 설정", "1", timestamp, timestamp},
                 {"log.ps.all_resources_cleaned", "log", "All resources cleaned up successfully", "All resources cleaned up successfully", "", "", "모든 리소스 정리 완료", "1", timestamp, timestamp},
                 {"log.ps.all_timers_cleaned", "log", "All timers cleaned up successfully", "All timers cleaned up successfully", "", "", "모든 타이머 정리 완료", "1", timestamp, timestamp},
                 {"log.ps.starting_timer_cleanup", "log", "Starting timer cleanup...", "Starting timer cleanup...", "", "", "타이머 정리 시작", "1", timestamp, timestamp},
                 {"log.bt.timer_restarted", "log", "BT message timer restarted after control mode exit", "BT message timer restarted after control mode exit", "", "", "블루투스 메시지 타이머 재시작", "1", timestamp, timestamp},
                 {"log.bt.timer_already_running", "log", "BT message timer already running, skipping start", "BT message timer already running, skipping start", "", "", "블루투스 메시지 타이머 이미 실행 중", "1", timestamp, timestamp},
                 {"log.bt.not_connected", "log", "Bluetooth not connected, cannot start message timer", "Bluetooth not connected, cannot start message timer", "", "", "블루투스 미연결", "1", timestamp, timestamp},
                 {"log.bt.socket_not_connected", "log", "Bluetooth socket not connected; stopping message timer", "Bluetooth socket not connected; stopping message timer", "", "", "블루투스 소켓 미연결", "1", timestamp, timestamp},
                 {"log.bt.test_items_not_initialized", "log", "arrTestItems not initialized yet, skipping message processing", "arrTestItems not initialized yet, skipping message processing", "", "", "테스트 항목 미초기화", "1", timestamp, timestamp},
                 {"log.bt.skipping_timer_restart", "log", "Bluetooth not connected, skipping BT message timer restart", "Bluetooth not connected, skipping BT message timer restart", "", "", "블루투스 타이머 재시작 건너뜀", "1", timestamp, timestamp},
                 {"log.bt.permission_request_in_progress", "log", "Permission request already in progress; skipping duplicate request", "Permission request already in progress; skipping duplicate request", "", "", "권한 요청 이미 진행 중", "1", timestamp, timestamp},
                 {"log.bt.bluetooth_manager_initialized", "log", "BluetoothManager initialized", "BluetoothManager initialized", "", "", "BluetoothManager 초기화 완료", "1", timestamp, timestamp},
                 {"log.si.network_manager_initialized", "log", "NetworkManager initialized", "NetworkManager initialized", "", "", "NetworkManager 초기화 완료", "1", timestamp, timestamp},
                 {"log.error.initializing_managers", "log", "Error initializing managers", "Error initializing managers", "", "", "관리자 초기화 오류", "1", timestamp, timestamp},
                 {"log.error.updating_header_bg", "log", "Error updating header background color", "Error updating header background color", "", "", "헤더 배경색 업데이트 오류", "1", timestamp, timestamp},
                 {"log.error.cleaning_bt_timer", "log", "Error cleaning up BT message timer before restart", "Error cleaning up BT message timer before restart", "", "", "BT 메시지 타이머 정리 오류", "1", timestamp, timestamp},
                 {"log.error.restarting_bt_timer", "log", "Error restarting BT message timer after control mode", "Error restarting BT message timer after control mode", "", "", "BT 메시지 타이머 재시작 오류", "1", timestamp, timestamp},
                 {"log.error.restoring_timers", "log", "Error restoring timers after control mode", "Error restoring timers after control mode", "", "", "타이머 복원 오류", "1", timestamp, timestamp},
                 {"log.error.stopping_all_timers", "log", "Error stopping all timers", "Error stopping all timers", "", "", "모든 타이머 중지 오류", "1", timestamp, timestamp},
                 {"log.error.canceling_finished_restart_timer", "log", "Error canceling finished restart timer", "Error canceling finished restart timer", "", "", "완료 재시작 타이머 취소 오류", "1", timestamp, timestamp},
                 {"log.error.canceling_unfinished_restart_timer", "log", "Error canceling unfinished restart timer", "Error canceling unfinished restart timer", "", "", "미완료 재시작 타이머 취소 오류", "1", timestamp, timestamp},
                 {"log.error.canceling_remote_command_timer", "log", "Error canceling remote command timer", "Error canceling remote command timer", "", "", "원격 명령 타이머 취소 오류", "1", timestamp, timestamp},
                 {"log.error.canceling_check_duration_timer", "log", "Error canceling check duration timer", "Error canceling check duration timer", "", "", "체크 지속 시간 타이머 취소 오류", "1", timestamp, timestamp},
                 {"log.error.canceling_barcode_request_timer", "log", "Error canceling barcode request timer", "Error canceling barcode request timer", "", "", "바코드 요청 타이머 취소 오류", "1", timestamp, timestamp},
                 {"log.error.updating_header_bg_red", "log", "Error updating header background color to red", "Error updating header background color to red", "", "", "헤더 배경색 빨강 업데이트 오류", "1", timestamp, timestamp},
                 {"log.error.updating_control_mode_button", "log", "Error updating control mode button", "Error updating control mode button", "", "", "제어 모드 버튼 업데이트 오류", "1", timestamp, timestamp},
                 {"log.error.cleaning_bluetooth_manager", "log", "Error cleaning up BluetoothManager", "Error cleaning up BluetoothManager", "", "", "BluetoothManager 정리 오류", "1", timestamp, timestamp},
                 {"log.error.cleaning_network_manager", "log", "Error cleaning up NetworkManager", "Error cleaning up NetworkManager", "", "", "NetworkManager 정리 오류", "1", timestamp, timestamp},
                 {"log.error.cleaning_resources", "log", "Error cleaning up resources", "Error cleaning up resources", "", "", "리소스 정리 오류", "1", timestamp, timestamp},
                 {"log.error.updating_http_success_status", "log", "Error updating HTTP success status in test history", "Error updating HTTP success status in test history", "", "", "HTTP 성공 상태 업데이트 오류", "1", timestamp, timestamp},
                 {"log.error.in_ui_update", "log", "Error in immediate UI update", "Error in immediate UI update", "", "", "즉시 UI 업데이트 오류", "1", timestamp, timestamp},
                 {"log.error.in_ui_update_batch", "log", "Error in UI update batch", "Error in UI update batch", "", "", "UI 업데이트 배치 오류", "1", timestamp, timestamp},
                 {"log.warn.list_item_adapter_not_ready", "log", "listItemAdapter not ready, cannot update test item result", "listItemAdapter not ready, cannot update test item result", "", "", "리스트 아이템 어댑터 준비 안됨", "1", timestamp, timestamp},
                 {"log.warn.interrupted_bt_worker", "log", "Interrupted while waiting for btWorkerExecutor termination", "Interrupted while waiting for btWorkerExecutor termination", "", "", "BT 워커 실행자 종료 대기 중단", "1", timestamp, timestamp},
                 {"log.warn.interrupted_usb_polling", "log", "Interrupted while waiting for usbPollingExecutor termination", "Interrupted while waiting for usbPollingExecutor termination", "", "", "USB 폴링 실행자 종료 대기 중단", "1", timestamp, timestamp},
                 {"log.warn.lock_task_mode_not_supported", "log", "Lock task mode not supported or permission denied: %s", "Lock task mode not supported or permission denied: %s", "", "", "Lock task 모드 미지원", "1", timestamp, timestamp},
                 
                 // 다이얼로그
                {"dialog.alert.title", "dialog", "알 림", "Alert", "", "", "알림 다이얼로그 제목", "1", timestamp, timestamp},
                {"dialog.cancel", "dialog", "취소", "Cancel", "", "", "취소 버튼", "1", timestamp, timestamp},
                {"dialog.bluetooth.title", "dialog", "블루투스", "Bluetooth", "", "", "블루투스 다이얼로그 제목", "1", timestamp, timestamp},
                
                // 시스템 설정
                {"ui.setting.language", "ui", "애플리케이션 언어", "Application Language", "", "", "언어 설정 항목", "1", timestamp, timestamp},
                {"ui.setting.language.description", "ui", "애플리케이션 표시 언어를 선택합니다", "Select the application display language", "", "", "언어 설정 설명", "1", timestamp, timestamp},
            };

            // INSERT OR IGNORE로 중복 방지
            String sql = "INSERT OR IGNORE INTO tbl_string_resources (" +
                    "clm_string_key, clm_string_category, clm_string_ko, clm_string_en, " +
                    "clm_string_zh, clm_string_ja, clm_description, clm_is_user_visible, " +
                    "clm_created_timestamp, clm_updated_timestamp" +
                    ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            db.beginTransaction();
            try {
                for (String[] row : initialStrings) {
                    db.execSQL(sql, row);
                }
                db.setTransactionSuccessful();
                Log.i(TAG, "> TestData.migrateInitialStringResources: Migrated " + initialStrings.length + " strings");
            } finally {
                db.endTransaction();
            }

        } catch (Exception e) {
            Log.e(TAG, "> TestData.migrateInitialStringResources.e.1 : " + e);
        } finally {
            if (helper != null) {
                helper.close();
            }
        }
    }
    
    /**
     * 초기 에러 카탈로그 데이터 삽입
     */
    public static void insertInitialErrorCatalog(Context context) {
        DBHelper helper = null;
        SQLiteDatabase db = null;
        
        try {
            helper = new DBHelper(context, "itf_temperature_table.db", null, DATABASE_VERSION);
            db = helper.getWritableDatabase();
            
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());
            
            String[][] errorCatalog = {
                // 블루투스 관련
                {"ERR-BT-001", "BT", "001", "블루투스 연결 실패", "Bluetooth Connection Failed",
                 "블루투스 장치와의 연결에 실패했습니다.", "Failed to connect to Bluetooth device.",
                 "HIGH", "블루투스가 활성화되어 있는지 확인하고, 장치가 페어링되어 있는지 확인하세요.",
                 "Check if Bluetooth is enabled and the device is paired.", timestamp, timestamp},
                
                {"ERR-BT-002", "BT", "002", "블루투스 권한 거부", "Bluetooth Permission Denied",
                 "블루투스 권한이 거부되었습니다.", "Bluetooth permission has been denied.",
                 "HIGH", "설정에서 블루투스 권한을 허용해주세요.", 
                 "Please allow Bluetooth permission in settings.", timestamp, timestamp},
                
                {"ERR-BT-003", "BT", "003", "블루투스 장치 이름 파싱 실패", "Bluetooth Device Name Parsing Failed",
                 "블루투스 장치 이름을 파싱하는 중 오류가 발생했습니다.", "An error occurred while parsing Bluetooth device name.",
                 "MEDIUM", "블루투스 장치를 다시 연결하거나 앱을 재시작하세요.",
                 "Reconnect the Bluetooth device or restart the app.", timestamp, timestamp},
                
                {"ERR-BT-004", "BT", "004", "블루투스 장치 발견 실패", "Bluetooth Device Discovery Failed",
                 "블루투스 장치를 발견하지 못했습니다.", "Failed to discover Bluetooth device.",
                 "MEDIUM", "블루투스가 활성화되어 있는지 확인하고, 장치가 검색 가능한 상태인지 확인하세요.",
                 "Check if Bluetooth is enabled and the device is discoverable.", timestamp, timestamp},
                
                {"ERR-BT-005", "BT", "005", "블루투스 통신 실패", "Bluetooth Communication Failed",
                 "블루투스 통신 중 오류가 발생했습니다.", "An error occurred during Bluetooth communication.",
                 "HIGH", "블루투스 연결을 확인하고, 장치를 다시 연결하세요.",
                 "Check Bluetooth connection and reconnect the device.", timestamp, timestamp},
                
                // USB 관련
                {"ERR-US-001", "US", "001", "USB 연결 실패", "USB Connection Failed",
                 "USB 장치와의 연결에 실패했습니다.", "Failed to connect to USB device.",
                 "HIGH", "USB 케이블이 제대로 연결되어 있는지 확인하세요.",
                 "Check if the USB cable is properly connected.", timestamp, timestamp},
                
                {"ERR-US-002", "US", "002", "USB 권한 거부", "USB Permission Denied",
                 "USB 권한이 거부되었습니다.", "USB permission has been denied.",
                 "HIGH", "설정에서 USB 권한을 허용해주세요.",
                 "Please allow USB permission in settings.", timestamp, timestamp},
                
                {"ERR-US-003", "US", "003", "USB 장치 미지원", "USB Device Not Supported",
                 "연결된 USB 장치가 지원되지 않습니다.", "The connected USB device is not supported.",
                 "MEDIUM", "지원되는 USB 장치인지 확인하세요.",
                 "Check if the USB device is supported.", timestamp, timestamp},
                
                {"ERR-US-004", "US", "004", "USB 통신 실패", "USB Communication Failed",
                 "USB 통신 중 오류가 발생했습니다.", "An error occurred during USB communication.",
                 "HIGH", "USB 케이블을 다시 연결하거나 다른 케이블을 사용해보세요.",
                 "Reconnect the USB cable or try a different cable.", timestamp, timestamp},
                
                {"ERR-US-005", "US", "005", "USB 서비스 시작 실패", "USB Service Start Failed",
                 "USB 서비스를 시작하는 중 오류가 발생했습니다.", "An error occurred while starting USB service.",
                 "HIGH", "앱을 재시작하거나 기기를 재부팅하세요.",
                 "Restart the app or reboot the device.", timestamp, timestamp},
                
                // 서버 관련
                {"ERR-SI-001", "SI", "001", "서버 연결 실패", "Server Connection Failed",
                 "서버와의 연결에 실패했습니다.", "Failed to connect to server.",
                 "HIGH", "네트워크 연결을 확인하고 서버 주소가 올바른지 확인하세요.",
                 "Check network connection and verify server address.", timestamp, timestamp},
                
                {"ERR-SI-002", "SI", "002", "HTTP 요청 실패", "HTTP Request Failed",
                 "HTTP 요청 중 오류가 발생했습니다.", "An error occurred during HTTP request.",
                 "HIGH", "네트워크 연결을 확인하고 서버가 정상 작동하는지 확인하세요.",
                 "Check network connection and verify server is running.", timestamp, timestamp},
                
                {"ERR-SI-003", "SI", "003", "JSON 파싱 실패", "JSON Parsing Failed",
                 "JSON 데이터를 파싱하는 중 오류가 발생했습니다.", "An error occurred while parsing JSON data.",
                 "MEDIUM", "서버 응답 형식을 확인하거나 서버 관리자에게 문의하세요.",
                 "Check server response format or contact server administrator.", timestamp, timestamp},
                
                {"ERR-SI-004", "SI", "004", "서버 응답 오류", "Server Response Error",
                 "서버로부터 오류 응답을 받았습니다.", "Received error response from server.",
                 "MEDIUM", "서버 로그를 확인하거나 서버 관리자에게 문의하세요.",
                 "Check server logs or contact server administrator.", timestamp, timestamp},
                
                {"ERR-SI-005", "SI", "005", "네트워크 타임아웃", "Network Timeout",
                 "네트워크 요청이 시간 초과되었습니다.", "Network request timed out.",
                 "MEDIUM", "네트워크 연결을 확인하고 다시 시도하세요.",
                 "Check network connection and try again.", timestamp, timestamp},
                
                // 프로세스 관련
                {"ERR-PS-001", "PS", "001", "프로세스 초기화 실패", "Process Initialization Failed",
                 "프로세스를 초기화하는 중 오류가 발생했습니다.", "An error occurred while initializing process.",
                 "HIGH", "앱을 재시작하거나 기기를 재부팅하세요.",
                 "Restart the app or reboot the device.", timestamp, timestamp},
                
                {"ERR-PS-002", "PS", "002", "프로세스 실행 실패", "Process Execution Failed",
                 "프로세스를 실행하는 중 오류가 발생했습니다.", "An error occurred while executing process.",
                 "HIGH", "앱을 재시작하거나 설정을 확인하세요.",
                 "Restart the app or check settings.", timestamp, timestamp},
                
                {"ERR-PS-003", "PS", "003", "프로세스 종료 실패", "Process Termination Failed",
                 "프로세스를 종료하는 중 오류가 발생했습니다.", "An error occurred while terminating process.",
                 "MEDIUM", "앱을 강제 종료하거나 기기를 재부팅하세요.",
                 "Force close the app or reboot the device.", timestamp, timestamp},
                
                {"ERR-PS-004", "PS", "004", "UI 업데이트 실패", "UI Update Failed",
                 "UI를 업데이트하는 중 오류가 발생했습니다.", "An error occurred while updating UI.",
                 "MEDIUM", "화면을 새로고침하거나 앱을 재시작하세요.",
                 "Refresh the screen or restart the app.", timestamp, timestamp},
                
                // 데이터베이스 관련
                {"ERR-DB-001", "DB", "001", "데이터베이스 연결 실패", "Database Connection Failed",
                 "데이터베이스에 연결하는 중 오류가 발생했습니다.", "An error occurred while connecting to database.",
                 "HIGH", "앱 데이터를 삭제하고 다시 설치하거나 기기를 재부팅하세요.",
                 "Clear app data and reinstall or reboot the device.", timestamp, timestamp},
                
                {"ERR-DB-002", "DB", "002", "데이터베이스 쿼리 실패", "Database Query Failed",
                 "데이터베이스 쿼리 실행 중 오류가 발생했습니다.", "An error occurred while executing database query.",
                 "MEDIUM", "데이터베이스 무결성을 확인하거나 앱을 재시작하세요.",
                 "Check database integrity or restart the app.", timestamp, timestamp},
                
                {"ERR-DB-003", "DB", "003", "데이터베이스 트랜잭션 실패", "Database Transaction Failed",
                 "데이터베이스 트랜잭션 실행 중 오류가 발생했습니다.", "An error occurred while executing database transaction.",
                 "MEDIUM", "데이터베이스 무결성을 확인하거나 앱을 재시작하세요.",
                 "Check database integrity or restart the app.", timestamp, timestamp},
                
                // 캐시 관련
                {"ERR-CA-001", "CA", "001", "캐시 저장 실패", "Cache Save Failed",
                 "캐시를 저장하는 중 오류가 발생했습니다.", "An error occurred while saving cache.",
                 "LOW", "캐시를 삭제하고 다시 시도하세요.",
                 "Clear cache and try again.", timestamp, timestamp},
                
                {"ERR-CA-002", "CA", "002", "캐시 조회 실패", "Cache Retrieve Failed",
                 "캐시를 조회하는 중 오류가 발생했습니다.", "An error occurred while retrieving cache.",
                 "LOW", "캐시를 삭제하고 다시 시도하세요.",
                 "Clear cache and try again.", timestamp, timestamp},
                
                // 온도 관련
                {"ERR-TH-001", "TH", "001", "온도 측정 실패", "Temperature Measurement Failed",
                 "온도를 측정하는 중 오류가 발생했습니다.", "An error occurred while measuring temperature.",
                 "MEDIUM", "온도 센서 연결을 확인하거나 센서를 교체하세요.",
                 "Check temperature sensor connection or replace sensor.", timestamp, timestamp},
                
                {"ERR-TH-002", "TH", "002", "온도 처리 오류", "Temperature Processing Error",
                 "온도 데이터를 처리하는 중 오류가 발생했습니다.", "An error occurred while processing temperature data.",
                 "MEDIUM", "온도 센서 데이터를 확인하거나 센서를 교체하세요.",
                 "Check temperature sensor data or replace sensor.", timestamp, timestamp},
                
                // 바코드 관련
                {"ERR-BI-001", "BI", "001", "바코드 스캔 실패", "Barcode Scan Failed",
                 "바코드를 스캔하는 중 오류가 발생했습니다.", "An error occurred while scanning barcode.",
                 "MEDIUM", "바코드 스캐너를 확인하거나 바코드를 다시 스캔하세요.",
                 "Check barcode scanner or rescan barcode.", timestamp, timestamp},
                
                {"ERR-BI-002", "BI", "002", "바코드 파싱 실패", "Barcode Parsing Failed",
                 "바코드 데이터를 파싱하는 중 오류가 발생했습니다.", "An error occurred while parsing barcode data.",
                 "MEDIUM", "바코드 형식을 확인하거나 바코드를 다시 스캔하세요.",
                 "Check barcode format or rescan barcode.", timestamp, timestamp},
                
                // 결과 관련
                {"ERR-RS-001", "RS", "001", "결과 저장 실패", "Result Save Failed",
                 "검사 결과를 저장하는 중 오류가 발생했습니다.", "An error occurred while saving test result.",
                 "HIGH", "데이터베이스 연결을 확인하거나 앱을 재시작하세요.",
                 "Check database connection or restart the app.", timestamp, timestamp},
                
                {"ERR-RS-002", "RS", "002", "결과 조회 실패", "Result Retrieve Failed",
                 "검사 결과를 조회하는 중 오류가 발생했습니다.", "An error occurred while retrieving test result.",
                 "MEDIUM", "데이터베이스 연결을 확인하거나 앱을 재시작하세요.",
                 "Check database connection or restart the app.", timestamp, timestamp},
                
                // 일반 에러
                {"ERR-ER-001", "ER", "001", "일반 예외 발생", "General Exception",
                 "예상치 못한 오류가 발생했습니다.", "An unexpected error occurred.",
                 "MEDIUM", "앱을 재시작하거나 개발자에게 문의하세요.",
                 "Restart the app or contact the developer.", timestamp, timestamp},
                
                {"ERR-ER-002", "ER", "002", "메모리 부족", "Out of Memory",
                 "메모리가 부족하여 작업을 수행할 수 없습니다.", "Insufficient memory to perform operation.",
                 "HIGH", "앱을 재시작하거나 기기를 재부팅하세요.",
                 "Restart the app or reboot the device.", timestamp, timestamp},
                
                {"ERR-ER-003", "ER", "003", "파일 입출력 오류", "File I/O Error",
                 "파일을 읽거나 쓰는 중 오류가 발생했습니다.", "An error occurred while reading or writing file.",
                 "MEDIUM", "파일 권한을 확인하거나 저장 공간을 확인하세요.",
                 "Check file permissions or storage space.", timestamp, timestamp},
                
                {"ERR-ER-004", "ER", "004", "권한 오류", "Permission Error",
                 "필요한 권한이 없어 작업을 수행할 수 없습니다.", "Cannot perform operation due to missing permission.",
                 "HIGH", "설정에서 필요한 권한을 허용해주세요.",
                 "Please allow required permissions in settings.", timestamp, timestamp},
                
                {"ERR-ER-005", "ER", "005", "알 수 없는 오류", "Unknown Error",
                 "알 수 없는 오류가 발생했습니다.", "An unknown error occurred.",
                 "MEDIUM", "앱을 재시작하거나 개발자에게 문의하세요.",
                 "Restart the app or contact the developer.", timestamp, timestamp},
            };
            
            String sql = "INSERT OR IGNORE INTO tbl_error_catalog (" +
                "clm_error_id, clm_error_category, clm_error_code, " +
                "clm_error_name_ko, clm_error_name_en, " +
                "clm_error_description_ko, clm_error_description_en, " +
                "clm_severity, clm_solution_ko, clm_solution_en, " +
                "clm_created_timestamp, clm_updated_timestamp" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            db.beginTransaction();
            try {
                for (String[] row : errorCatalog) {
                    db.execSQL(sql, row);
                }
                db.setTransactionSuccessful();
                Log.i(TAG, "> TestData.insertInitialErrorCatalog: Inserted " + errorCatalog.length + " error catalog entries");
            } finally {
                db.endTransaction();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "> TestData.insertInitialErrorCatalog.e.1 : " + e);
        } finally {
            if (helper != null) {
                helper.close();
            }
        }
    }


    public static void migrateActivityModelTestProcessLogStrings(Context context) {
        DBHelper helper = null;
        SQLiteDatabase db = null;

        try {
            helper = new DBHelper(context, "itf_temperature_table.db", null, DATABASE_VERSION);
            db = helper.getWritableDatabase();

            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

            String[][] logStrings = {
                {"log.activity_model_test_process.0001", "log", "\"BT message timer already running, skipping start\"", "\"BT message timer already running, skipping start\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0001)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0002", "log", "\"Bluetooth not connected, cannot start message timer\"", "\"Bluetooth not connected, cannot start message timer\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0002)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0003", "log", "\"Bluetooth socket not connected; stopping message timer\"", "\"Bluetooth socket not connected; stopping message timer\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0003)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0004", "log", "\"arrTestItems not initialized yet, skipping message processing\"", "\"arrTestItems not initialized yet, skipping message processing\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0004)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0005", "log", "\"ê²ì¬ í­ëª© ì í¸ ì¡ì .B [T:%d/%d][C:%d/%s][P:%d/%d][S:%s][G:%d=%d-%d] ìì  ìì %s\"", "\"ê²ì¬ í­ëª© ì í¸ ì¡ì .B [T:%d/%d][C:%d/%s][P:%d/%d][S:%s][G:%d=%d-%d] ìì  ìì %s\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0005)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0006", "log", "%s", "%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0006)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0007", "log", "\"CM0101 ì¸ë±ì¤ë¥¼ ì°¾ì ì ìê±°ë ë²ìë¥¼ ë²ì´ë¨: \"%s", "\"CM0101 ì¸ë±ì¤ë¥¼ ì°¾ì ì ìê±°ë ë²ìë¥¼ ë²ì´ë¨: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0007)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0008", "log", "\"Error updating CM0100 in runOnUiThread\"", "\"Error updating CM0100 in runOnUiThread\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0008)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0009", "log", "\"Error starting BT message timer\"", "\"Error starting BT message timer\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0009)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0010", "log", "\"Error canceling BT message timer\"", "\"Error canceling BT message timer\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0010)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0011", "log", "\"Error canceling BT message timer task\"", "\"Error canceling BT message timer task\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0011)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0012", "log", "\">>>>>>>>>>>>>>>>> Setting header background color to red for control mode isControlOn \"%s\" isTestWaiting \"%s", "\">>>>>>>>>>>>>>>>> Setting header background color to red for control mode isControlOn \"%s\" isTestWaiting \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0012)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0013", "log", "\">>>>>>>>>>>>>>>>> Setting header background color to red for control mode AAAAA\"", "\">>>>>>>>>>>>>>>>> Setting header background color to red for control mode AAAAA\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0013)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0014", "log", "\">>>>>>>>>>>>>>>>> Setting header background color to red for control mode CCCCC\"", "\">>>>>>>>>>>>>>>>> Setting header background color to red for control mode CCCCC\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0014)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0015", "log", "\"Error updating header background color\"", "\"Error updating header background color\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0015)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0016", "log", "\">>>>>>>>>>>>>>>>> Setting header background color to red for control mode isControlOn.before \"%s", "\">>>>>>>>>>>>>>>>> Setting header background color to red for control mode isControlOn.before \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0016)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0017", "log", "\">>>>>>>>>>>>>>>>> Setting header background color to red for control mode isControlOn.after \"%s", "\">>>>>>>>>>>>>>>>> Setting header background color to red for control mode isControlOn.after \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0017)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0018", "log", "\"Error cleaning up BT message timer before restart\"", "\"Error cleaning up BT message timer before restart\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0018)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0019", "log", "\"BT message timer restarted after control mode exit\"", "\"BT message timer restarted after control mode exit\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0019)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0020", "log", "\"Error restarting BT message timer after control mode\"", "\"Error restarting BT message timer after control mode\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0020)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0021", "log", "\"Bluetooth not connected, skipping BT message timer restart\"", "\"Bluetooth not connected, skipping BT message timer restart\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0021)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0022", "log", "\"Error restoring timers after control mode\"", "\"Error restoring timers after control mode\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0022)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0023", "log", "\"All timers stopped for control mode (USB polling timer kept running)\"", "\"All timers stopped for control mode (USB polling timer kept running)\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0023)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0024", "log", "\"Error stopping all timers\"", "\"Error stopping all timers\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0024)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0025", "log", "\"Error canceling finished restart timer\"", "\"Error canceling finished restart timer\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0025)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0026", "log", "\"Error canceling finished restart timer task\"", "\"Error canceling finished restart timer task\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0026)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0027", "log", "\"Error canceling unfinished restart timer\"", "\"Error canceling unfinished restart timer\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0027)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0028", "log", "\"Error canceling unfinished restart timer task\"", "\"Error canceling unfinished restart timer task\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0028)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0029", "log", "\"Error canceling remote command timer\"", "\"Error canceling remote command timer\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0029)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0030", "log", "\"Error canceling remote command timer task\"", "\"Error canceling remote command timer task\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0030)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0031", "log", "\"Error canceling reset timer\"", "\"Error canceling reset timer\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0031)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0032", "log", "\"Error canceling reset timer task\"", "\"Error canceling reset timer task\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0032)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0033", "log", "\"Error canceling check duration timer\"", "\"Error canceling check duration timer\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0033)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0034", "log", "\"Error canceling barcode request timer\"", "\"Error canceling barcode request timer\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0034)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0035", "log", "\"Error canceling app reset timer task\"", "\"Error canceling app reset timer task\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0035)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0036", "log", "\">>>>>>>>>>>>>>>>> Setting header background color to red for control mode BBBB\"", "\">>>>>>>>>>>>>>>>> Setting header background color to red for control mode BBBB\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0036)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0037", "log", "\"Error updating header background color to red\"", "\"Error updating header background color to red\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0037)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0038", "log", "\"Error updating control mode button\"", "\"Error updating control mode button\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0038)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0039", "log", "\"Error displaying control request access message\"", "\"Error displaying control request access message\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0039)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0040", "log", "\"Error clearing control request access message\"", "\"Error clearing control request access message\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0040)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0041", "log", "\"Control mode response timeout for command: \"%s", "\"Control mode response timeout for command: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0041)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0042", "log", "\"Started waiting for control response: \"%s", "\"Started waiting for control response: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0042)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0043", "log", "\"Control mode response received: \"%s\" for command: \"%s", "\"Control mode response received: \"%s\" for command: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0043)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0044", "log", "\"Dialog components not initialized\"", "\"Dialog components not initialized\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0044)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0045", "log", "\"Failed to show control response dialog\"", "\"Failed to show control response dialog\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0045)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0046", "log", "\"Not in control mode, cannot execute control test item\"", "\"Not in control mode, cannot execute control test item\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0046)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0047", "log", "\"arrTestItems not initialized, cannot execute control test item\"", "\"arrTestItems not initialized, cannot execute control test item\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0047)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0048", "log", "\"Test item not found for command: \"%s", "\"Test item not found for command: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0048)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0049", "log", "\"Starting control mode test item: \" + command + \" (index: \" + foundIndex + \", duration: \" + arrTestItems[foundIndex][2] + \" seconds)\"", "\"Starting control mode test item: \" + command + \" (index: \" + foundIndex + \", duration: \" + arrTestItems[foundIndex][2] + \" seconds)\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0049)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0050", "log", "\"Bluetooth not connected, stopping control test timer\"", "\"Bluetooth not connected, stopping control test timer\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0050)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0051", "log", "\"Invalid test duration: \"%s", "\"Invalid test duration: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0051)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0052", "log", "\"Control test item signal sent [C:%d/%d][S:%s]\"", "\"Control test item signal sent [C:%d/%d][S:%s]\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0052)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0053", "log", "\"Control test item completed: \"%s", "\"Control test item completed: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0053)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0054", "log", "\"Failed to start control test timer\"", "\"Failed to start control test timer\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0054)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0055", "log", "\"Force exit control mode due to test restart\"", "\"Force exit control mode due to test restart\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0055)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0056", "log", "\"Cannot show control mode test result dialog: no test item\"", "\"Cannot show control mode test result dialog: no test item\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0056)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0057", "log", "\"Control mode test result dialog shown: \"%s", "\"Control mode test result dialog shown: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0057)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0058", "log", "\"Failed to show control mode test result dialog\"", "\"Failed to show control mode test result dialog\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0058)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0059", "log", "\"Cannot show control test result dialog: invalid test item index\"", "\"Cannot show control test result dialog: invalid test item index\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0059)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0060", "log", "\"Test result dialog components not initialized\"", "\"Test result dialog components not initialized\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0060)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0061", "log", "\"Control test result dialog shown: \" + testResult + \" (OK: \" + testOkCnt + \", NG: \" + testNgCnt + \")\"", "\"Control test result dialog shown: \" + testResult + \" (OK: \" + testOkCnt + \", NG: \" + testNgCnt + \")\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0061)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0062", "log", "\"Failed to show control test result dialog\"", "\"Failed to show control test result dialog\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0062)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0063", "log", "\"Control owner notification sent to web server: \"%s", "\"Control owner notification sent to web server: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0063)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0064", "log", "\"Failed to send control owner notification, response code: \"%s", "\"Failed to send control owner notification, response code: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0064)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0065", "log", "\"Failed to notify web server of control owner: \"%s", "\"Failed to notify web server of control owner: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0065)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0066", "log", "\"Shutdown notification sent to web client: \"%s", "\"Shutdown notification sent to web client: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0066)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0067", "log", "\"Failed to send shutdown notification, response code: \"%s", "\"Failed to send shutdown notification, response code: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0067)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0068", "log", "\"Failed to notify web client of shutdown: \"%s", "\"Failed to notify web client of shutdown: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0068)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0069", "log", "\">>>>>>>>>>>>>>>>>>>>>>>>>>>>>> controlRequestAccessRunnable \"", "\">>>>>>>>>>>>>>>>>>>>>>>>>>>>>> controlRequestAccessRunnable \"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0069)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0070", "log", "\"onDestroy\"", "\"onDestroy\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0070)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0071", "log", "\"Error closing Bluetooth socket in onDestroy: \"%s", "\"Error closing Bluetooth socket in onDestroy: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0071)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0072", "log", "\"Error canceling Bluetooth connected thread: \"%s", "\"Error canceling Bluetooth connected thread: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0072)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0073", "log", "\"btWorkerExecutor did not terminate within timeout\"", "\"btWorkerExecutor did not terminate within timeout\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0073)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0074", "log", "\"Interrupted while waiting for btWorkerExecutor termination\"", "\"Interrupted while waiting for btWorkerExecutor termination\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0074)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0075", "log", "\"usbPollingExecutor did not terminate within timeout\"", "\"usbPollingExecutor did not terminate within timeout\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0075)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0076", "log", "\"Interrupted while waiting for usbPollingExecutor termination\"", "\"Interrupted while waiting for usbPollingExecutor termination\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0076)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0077", "log", "\"onDestroy error: \"%s", "\"onDestroy error: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0077)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0078", "log", "\"getLocalIpAddress error\"", "\"getLocalIpAddress error\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0078)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0079", "log", "\"â StrictMode enabled: ThreadPolicy (network, disk I/O) + VmPolicy (memory leaks)\"", "\"â StrictMode enabled: ThreadPolicy (network, disk I/O) + VmPolicy (memory leaks)\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0079)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0080", "log", "\"onCreate entranceCheck:\"%s\" / currentTestItem:\"%s", "\"onCreate entranceCheck:\"%s\" / currentTestItem:\"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0080)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0081", "log", "\"onCreate error\"", "\"onCreate error\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0081)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0082", "log", "\">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Permission A.requestRuntimePermissionsOnMainThread \"", "\">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Permission A.requestRuntimePermissionsOnMainThread \"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0082)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0083", "log", "\"Permission request already in progress; skipping duplicate request\"", "\"Permission request already in progress; skipping duplicate request\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0083)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0084", "log", "\"Deferred initialization failed\"", "\"Deferred initialization failed\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0084)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0085", "log", "\">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> EEE isControlMode \"%s\" / isControlOn \"%s", "\">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> EEE isControlMode \"%s\" / isControlOn \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0085)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0086", "log", "\">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> FFF isControlMode \"%s\" / isControlOn \"%s", "\">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> FFF isControlMode \"%s\" / isControlOn \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0086)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0087", "log", "\"ë¸ë£¨í¬ì¤ ëë°ì´ì¤ ì°ê²° ì±ê³µ\"", "\"ë¸ë£¨í¬ì¤ ëë°ì´ì¤ ì°ê²° ì±ê³µ\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0087)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0088", "log", "\"Bluetooth device name parsing error\"", "\"Bluetooth device name parsing error\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0088)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0089", "log", "\"ë¸ë£¨í¬ì¤ ëë°ì´ì¤ ì°ê²° ì¤í¨\"", "\"ë¸ë£¨í¬ì¤ ëë°ì´ì¤ ì°ê²° ì¤í¨\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0089)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0090", "log", "\"ìë² ë¯¸ì°ê²° í ë¸ë£¨í¬ì¤ ì°ê²° ìë\"", "\"ìë² ë¯¸ì°ê²° í ë¸ë£¨í¬ì¤ ì°ê²° ìë\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0090)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0091", "log", "\"ìë² ì°ê²° í ë¸ë£¨í¬ì¤ ì°ê²° ìë\"", "\"ìë² ì°ê²° í ë¸ë£¨í¬ì¤ ì°ê²° ìë\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0091)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0092", "log", "\"Initializing communication managers...\"", "\"Initializing communication managers...\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0092)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0093", "log", "\"All managers initialized successfully\"", "\"All managers initialized successfully\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0093)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0094", "log", "\"Error initializing managers\"", "\"Error initializing managers\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0094)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0095", "log", "\"TimerManager initialized\"", "\"TimerManager initialized\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0095)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0096", "log", "\"PermissionManager initialized\"", "\"PermissionManager initialized\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0096)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0097", "log", "\"LoggingService initialized\"", "\"LoggingService initialized\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0097)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0098", "log", "\"All processors initialized\"", "\"All processors initialized\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0098)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0099", "log", "\"Error initializing service managers\"", "\"Error initializing service managers\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0099)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0100", "log", "\"Error updating BT UI\"", "\"Error updating BT UI\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0100)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0101", "log", "\"Bluetooth error: \"%s\" - \"%s", "\"Bluetooth error: \"%s\" - \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0101)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0102", "log", "\"Error showing BT error\"", "\"Error showing BT error\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0102)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0103", "log", "\"BluetoothManager initialized\"", "\"BluetoothManager initialized\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0103)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0104", "log", "\"Test spec data received: \" + (specData != null ? specData.size() : 0) + \" items\"", "\"Test spec data received: \" + (specData != null ? specData.size() : 0) + \" items\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0104)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0105", "log", "\"Barcode info received: \"%s\" -> \"%s", "\"Barcode info received: \"%s\" -> \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0105)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0106", "log", "\"Error processing barcode info\"", "\"Error processing barcode info\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0106)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0107", "log", "\"Upload \"%s\": \"%s", "\"Upload \"%s\": \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0107)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0108", "log", "\"Version: \"%s", "\"Version: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0108)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0109", "log", "\"Network error: \"%s\" - \"%s", "\"Network error: \"%s\" - \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0109)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0110", "log", "\"Network progress: \"%s\"% - \"%s", "\"Network progress: \"%s\"% - \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0110)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0111", "log", "\"NetworkManager initialized\"", "\"NetworkManager initialized\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0111)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0112", "log", "\"Starting communication managers...\"", "\"Starting communication managers...\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0112)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0113", "log", "\"Cleaning up communication managers...\"", "\"Cleaning up communication managers...\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0113)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0114", "log", "\"BluetoothManager cleaned up\"", "\"BluetoothManager cleaned up\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0114)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0115", "log", "\"Error cleaning up BluetoothManager\"", "\"Error cleaning up BluetoothManager\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0115)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0116", "log", "\"UsbConnectionManager cleaned up\"", "\"UsbConnectionManager cleaned up\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0116)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0117", "log", "\"Error cleaning up UsbConnectionManager\"", "\"Error cleaning up UsbConnectionManager\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0117)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0118", "log", "\"NetworkManager cleaned up\"", "\"NetworkManager cleaned up\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0118)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0119", "log", "\"Error cleaning up NetworkManager\"", "\"Error cleaning up NetworkManager\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0119)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0120", "log", "\"TimerManager cleaned up\"", "\"TimerManager cleaned up\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0120)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0121", "log", "\"Error cleaning up TimerManager\"", "\"Error cleaning up TimerManager\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0121)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0122", "log", "\"Manager cleanup complete\"", "\"Manager cleanup complete\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0122)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0123", "log", "\"arrTestItems or listItemAdapter not initialized yet, skipping message processing\"", "\"arrTestItems or listItemAdapter not initialized yet, skipping message processing\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0123)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0124", "log", "\"Control mode: ST0101 response received (\" + controlSt0101SuccessCount + \"/\" + CONTROL_ST0101_REQUIRED_COUNT + \")\"", "\"Control mode: ST0101 response received (\" + controlSt0101SuccessCount + \"/\" + CONTROL_ST0101_REQUIRED_COUNT + \")\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0124)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0125", "log", "\"Control mode: ST0101 responses completed. Entering control mode.\"", "\"Control mode: ST0101 responses completed. Entering control mode.\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0125)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0126", "log", "\"Control mode: Waiting for more ST0101 responses to enter control mode.\"", "\"Control mode: Waiting for more ST0101 responses to enter control mode.\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0126)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0127", "log", "\"Control mode: ST0101 received, setting control test ready state\"", "\"Control mode: ST0101 received, setting control test ready state\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0127)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0128", "log", "\"Header message set to 'ì ì´ ì¤ë¹ ìë£'\"", "\"Header message set to 'ì ì´ ì¤ë¹ ìë£'\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0128)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0129", "log", "\"Failed to set header message\"", "\"Failed to set header message\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0129)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0130", "log", "\"Control test ready state set, waiting for web control commands\"", "\"Control test ready state set, waiting for web control commands\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0130)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0131", "log", "\"Test entry blocked: USB or Bluetooth not ready\"", "\"Test entry blocked: USB or Bluetooth not ready\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0131)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0132", "log", "\"listItemAdapter not ready, cannot update test item result\"", "\"listItemAdapter not ready, cannot update test item result\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0132)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0133", "log", "\"testProcessId started: \"%s", "\"testProcessId started: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0133)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0134", "log", "\"Unfinished restart timer already running, skipping start\"", "\"Unfinished restart timer already running, skipping start\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0134)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0135", "log", "\"tmrUnfinishedRestart ê²ì¬ ì´íë¦¬ì¼ì´ì ì¢ë£ ì¤í¨ ì¬ìì [%d / %d]\"", "\"tmrUnfinishedRestart ê²ì¬ ì´íë¦¬ì¼ì´ì ì¢ë£ ì¤í¨ ì¬ìì [%d / %d]\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0135)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0136", "log", "\"Error starting unfinished restart timer\"", "\"Error starting unfinished restart timer\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0136)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0137", "log", "\"USB Service started\"", "\"USB Service started\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0137)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0138", "log", "\"USB Service start error\"", "\"USB Service start error\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0138)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0139", "log", "\">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> AAA \"%s", "\">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> AAA \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0139)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0140", "log", "\"readMessage is null or empty, skipping message processing\"", "\"readMessage is null or empty, skipping message processing\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0140)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0141", "log", "\"STX character not found in readMessage: \"%s", "\"STX character not found in readMessage: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0141)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0142", "log", "\"readMessage too short. Length: \"%s\", Required: \"%s\", Message: \"%s", "\"readMessage too short. Length: \"%s\", Required: \"%s\", Message: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0142)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0143", "log", "\"StringIndexOutOfBoundsException while parsing readMessage: \"%s\", STX index: \"%s", "\"StringIndexOutOfBoundsException while parsing readMessage: \"%s\", STX index: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0143)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0144", "log", "\"receiveCommand is null or empty, skipping database query\"", "\"receiveCommand is null or empty, skipping database query\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0144)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0145", "log", "\"lstSpecData is null or empty for command: \"%s\" (DB ë° ë©ëª¨ë¦¬ ìºì ëª¨ë íì¸ë¨)\"", "\"lstSpecData is null or empty for command: \"%s\" (DB ë° ë©ëª¨ë¦¬ ìºì ëª¨ë íì¸ë¨)\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0145)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0146", "log", "\"Error in initial UI update\"", "\"Error in initial UI update\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0146)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0147", "log", "\">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> BBB \"%s", "\">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> BBB \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0147)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0148", "log", "\"Thread execution error\"", "\"Thread execution error\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0148)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0149", "log", "\"Control test condition met: \" + controlCurrentTestItem + \" (response: \" + finalReceiveCommandResponse + \")\"", "\"Control test condition met: \" + controlCurrentTestItem + \" (response: \" + finalReceiveCommandResponse + \")\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0149)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0150", "log", "\"Control test item completed (condition met): \"%s", "\"Control test item completed (condition met): \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0150)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0151", "log", "\"Error collecting listItemAdapter data\"", "\"Error collecting listItemAdapter data\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0151)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0152", "log", "\"Error saving SharedPreferences in background thread\"", "\"Error saving SharedPreferences in background thread\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0152)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0153", "log", "\"Error closing socket\"", "\"Error closing socket\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0153)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0154", "log", "\"Error saving test_info SharedPreferences in background thread\"", "\"Error saving test_info SharedPreferences in background thread\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0154)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0155", "log", "\"Error saving test history to database in background thread\"", "\"Error saving test history to database in background thread\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0155)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0156", "log", "\"Finished restart timer already running, skipping start\"", "\"Finished restart timer already running, skipping start\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0156)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0157", "log", "\"Error starting finished restart timer\"", "\"Error starting finished restart timer\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0157)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0158", "log", "\"Thread execution error in RE0101\"", "\"Thread execution error in RE0101\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0158)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0159", "log", "\"Error in background processing\"", "\"Error in background processing\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0159)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0160", "log", "\"Bluetooth message handling error\"", "\"Bluetooth message handling error\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0160)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0161", "log", "\"Bluetooth handler error\"", "\"Bluetooth handler error\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0161)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0162", "log", "\"Directory creation - IOException\"", "\"Directory creation - IOException\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0162)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0163", "log", "\">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> CCC \"%s", "\">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> CCC \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0163)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0164", "log", "\"Error during async initialization\"", "\"Error during async initialization\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0164)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0165", "log", "\"Temperature data read error\"", "\"Temperature data read error\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0165)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0166", "log", "\"Test spec data [%d]: %s\"", "\"Test spec data [%d]: %s\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0166)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0167", "log", "\"Server connection error\"", "\"Server connection error\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0167)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0168", "log", "\"lstSpecData error: \"%s", "\"lstSpecData error: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0168)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0169", "log", "\"HTTP OK ìë¨\"\" (ìë \"%s\", code=\"%s\")\"", "\"HTTP OK ìë¨\"\" (ìë \"%s\", code=\"%s\")\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0169)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0170", "log", "\"HTTP result update error\"\" (ìë \"%s\"): \"%s", "\"HTTP result update error\"\" (ìë \"%s\"): \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0170)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0171", "log", "\"HTTP ì¬ìë ëê¸° ì¤ ì¸í°ë½í¸ ë°ì\"", "\"HTTP ì¬ìë ëê¸° ì¤ ì¸í°ë½í¸ ë°ì\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0171)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0172", "log", "\"Error updating HTTP success status in test history\"", "\"Error updating HTTP success status in test history\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0172)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0173", "log", "\"Error saving linear data to test history\"", "\"Error saving linear data to test history\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0173)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0174", "log", "\"UsbManager is null\"", "\"UsbManager is null\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0174)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0175", "log", "\"Start UsbService failed: \"%s", "\"Start UsbService failed: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0175)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0176", "log", "\"usbSearch error\"", "\"usbSearch error\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0176)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0177", "log", "\"finishApplication: ì¬íì ë° ì¬ì°ê²° ìë ìì >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 0\"", "\"finishApplication: ì¬íì ë° ì¬ì°ê²° ìë ìì >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 0\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0177)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0178", "log", "\"finishApplication: ì¬íì ë° ì¬ì°ê²° ìë ìì >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 1\"", "\"finishApplication: ì¬íì ë° ì¬ì°ê²° ìë ìì >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 1\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0178)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0179", "log", "\"finishApplication: ì¬íì ë° ì¬ì°ê²° ìë ìì >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 2\"", "\"finishApplication: ì¬íì ë° ì¬ì°ê²° ìë ìì >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 2\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0179)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0180", "log", "\"Bluetooth connected thread cancelled in finishApplication\"", "\"Bluetooth connected thread cancelled in finishApplication\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0180)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0181", "log", "\"Error canceling Bluetooth connected thread in finishApplication: \"%s", "\"Error canceling Bluetooth connected thread in finishApplication: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0181)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0182", "log", "\"Bluetooth handler queue cleared in finishApplication\"", "\"Bluetooth handler queue cleared in finishApplication\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0182)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0183", "log", "\"Bluetooth socket closed in finishApplication\"", "\"Bluetooth socket closed in finishApplication\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0183)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0184", "log", "\"Error closing Bluetooth socket in finishApplication: \"%s", "\"Error closing Bluetooth socket in finishApplication: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0184)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0185", "log", "\"finishApplication: ì¬íì ë° ì¬ì°ê²° ìë ìì >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 3\"", "\"finishApplication: ì¬íì ë° ì¬ì°ê²° ìë ìì >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 3\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0185)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0186", "log", "\"finding Bluetooth devices error\"", "\"finding Bluetooth devices error\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0186)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0187", "log", "\"error occurred while bluetooth devices reconnecting in finishApplication\"", "\"error occurred while bluetooth devices reconnecting in finishApplication\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0187)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0188", "log", "\"finishApplication: ì¬íì ë° ì¬ì°ê²° ìë ìì >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 4\"", "\"finishApplication: ì¬íì ë° ì¬ì°ê²° ìë ìì >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 4\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0188)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0189", "log", "\"finishApplication: ì¬íì ë° ì¬ì°ê²° ìë ìì >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 5\"", "\"finishApplication: ì¬íì ë° ì¬ì°ê²° ìë ìì >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 5\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0189)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0190", "log", "\"USB service restart failed: \"%s", "\"USB service restart failed: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0190)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0191", "log", "\"finishApplication: ì¬íì ë° ì¬ì°ê²° ìë ìì >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 7\"", "\"finishApplication: ì¬íì ë° ì¬ì°ê²° ìë ìì >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 7\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0191)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0192", "log", "\"finishApplication: ì¬íì ë° ì¬ì°ê²° ìë ìì >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 8\"", "\"finishApplication: ì¬íì ë° ì¬ì°ê²° ìë ìì >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 8\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0192)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0193", "log", "\"finishApplication: ì¬íì ë° ì¬ì°ê²° ìë ìì >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 9\"", "\"finishApplication: ì¬íì ë° ì¬ì°ê²° ìë ìì >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 9\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0193)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0194", "log", "\"Thread sleep error\"", "\"Thread sleep error\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0194)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0195", "log", "\"finishApplication: ì¬íì ë° ì¬ì°ê²° ìë ìì >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 10\"", "\"finishApplication: ì¬íì ë° ì¬ì°ê²° ìë ìì >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 10\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0195)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0196", "log", "\"Before save to SharedPreferences - globalModelId: \"%s\", globalModelName: \"%s\", globalModelNation: \"%s", "\"Before save to SharedPreferences - globalModelId: \"%s\", globalModelName: \"%s\", globalModelNation: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0196)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0197", "log", "\"finishApplication: ì¬íì ë° ì¬ì°ê²° ìë ìì >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 11\"", "\"finishApplication: ì¬íì ë° ì¬ì°ê²° ìë ìì >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 11\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0197)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0198", "log", "\"After save to SharedPreferences - TEST_MODEL_ID: \"%s\", TEST_MODEL_NAME: \"%s\", TEST_MODEL_NATION: \"%s", "\"After save to SharedPreferences - TEST_MODEL_ID: \"%s\", TEST_MODEL_NAME: \"%s\", TEST_MODEL_NATION: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0198)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0199", "log", "\"finishApplication: ì¬íì ë° ì¬ì°ê²° ìë ìì >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 12\"", "\"finishApplication: ì¬íì ë° ì¬ì°ê²° ìë ìì >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 12\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0199)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0200", "log", "\"Intent Extra after putExtra - TEST_MODEL_ID: \"%s\", TEST_MODEL_NAME: \"%s\", TEST_MODEL_NATION: \"%s\", TEST_START_DATETIME: \"%s", "\"Intent Extra after putExtra - TEST_MODEL_ID: \"%s\", TEST_MODEL_NAME: \"%s\", TEST_MODEL_NATION: \"%s\", TEST_START_DATETIME: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0200)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0201", "log", "\"Activity restarted successfully\"", "\"Activity restarted successfully\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0201)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0202", "log", "\"Failed to restart Activity\"", "\"Failed to restart Activity\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0202)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0203", "log", "\"finishApplication: ì¬íì ë° ì¬ì°ê²° ìë ìì >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 13\"", "\"finishApplication: ì¬íì ë° ì¬ì°ê²° ìë ìì >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 13\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0203)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0204", "log", "\"Application restart/finish error\"", "\"Application restart/finish error\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0204)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0205", "log", "\"Processing HTTP response data\"", "\"Processing HTTP response data\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0205)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0206", "log", "\"Deleting test spec data\"", "\"Deleting test spec data\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0206)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0207", "log", "\"Test spec data deleted\"", "\"Test spec data deleted\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0207)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0208", "log", "\"JSON parsing error\"", "\"JSON parsing error\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0208)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0209", "log", "\"Test spec array is empty\"", "\"Test spec array is empty\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0209)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0210", "log", "\"Starting timer cleanup...\"", "\"Starting timer cleanup...\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0210)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0211", "log", "\"Error stopping BT connection indicator\"", "\"Error stopping BT connection indicator\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0211)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0212", "log", "\">>>>>>>>>>>>>>>>>>>>>>>> stopUsbPolling 1\"", "\">>>>>>>>>>>>>>>>>>>>>>>> stopUsbPolling 1\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0212)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0213", "log", "\"Error stopping USB polling\"", "\"Error stopping USB polling\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0213)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0214", "log", "\"Error stopping BT message timer\"", "\"Error stopping BT message timer\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0214)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0215", "log", "\"Error cleaning up finished restart timer\"", "\"Error cleaning up finished restart timer\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0215)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0216", "log", "\"Error cleaning up unfinished restart timer\"", "\"Error cleaning up unfinished restart timer\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0216)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0217", "log", "\"Error cleaning up remote command timer\"", "\"Error cleaning up remote command timer\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0217)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0218", "log", "\"Error cleaning up checkDuration timer\"", "\"Error cleaning up checkDuration timer\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0218)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0219", "log", "\"Error cleaning up barcode request timer\"", "\"Error cleaning up barcode request timer\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0219)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0220", "log", "\"Error cleaning up app reset timer task\"", "\"Error cleaning up app reset timer task\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0220)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0221", "log", "\"All timers cleaned up successfully\"", "\"All timers cleaned up successfully\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0221)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0222", "log", "\"btWorkerExecutor is shutdown, cannot execute task\"", "\"btWorkerExecutor is shutdown, cannot execute task\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0222)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0223", "log", "\"Task rejected from btWorkerExecutor: \"%s", "\"Task rejected from btWorkerExecutor: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0223)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0224", "log", "\"Missing BLUETOOTH_CONNECT permission; cannot read bonded devices\"", "\"Missing BLUETOOTH_CONNECT permission; cannot read bonded devices\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0224)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0225", "log", "\"Failed to obtain bonded devices\"", "\"Failed to obtain bonded devices\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0225)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0226", "log", "\"Missing BLUETOOTH_SCAN permission; cannot cancel discovery\"", "\"Missing BLUETOOTH_SCAN permission; cannot cancel discovery\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0226)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0227", "log", "\"Failed to cancel discovery safely\"", "\"Failed to cancel discovery safely\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0227)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0228", "log", "\">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Permission B.requestRuntimePermissions getRequiredPermissions().length \"%s", "\">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Permission B.requestRuntimePermissions getRequiredPermissions().length \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0228)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0229", "log", "\">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Permission B.requestRuntimePermissions permission \"%s\" : \"%s", "\">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Permission B.requestRuntimePermissions permission \"%s\" : \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0229)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0230", "log", "\"> resetBluetoothSessionKeepUsb\"", "\"> resetBluetoothSessionKeepUsb\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0230)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0231", "log", "\">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> testOkCnt \"%s\" testNgCnt \"%s", "\">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> testOkCnt \"%s\" testNgCnt \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0231)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0232", "log", "\">>>>>>>>>>>>>>>>>>>>>>>> startUsbPolling 2\"", "\">>>>>>>>>>>>>>>>>>>>>>>> startUsbPolling 2\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0232)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0233", "log", "\"USB polling executor is shut down; cannot schedule polling\"", "\"USB polling executor is shut down; cannot schedule polling\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0233)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0234", "log", "\"UsbService is null; skipping polling start\"", "\"UsbService is null; skipping polling start\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0234)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0235", "log", "\"USB permission not granted; skipping polling start\"", "\"USB permission not granted; skipping polling start\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0235)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0236", "log", "\"USB command queue initialized and started\"", "\"USB command queue initialized and started\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0236)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0237", "log", "\"USB polling already running; skipping restart\"", "\"USB polling already running; skipping restart\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0237)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0238", "log", "\">>>>>>>>>>>>>>>>>>>>>>>> stopUsbPolling 2\"", "\">>>>>>>>>>>>>>>>>>>>>>>> stopUsbPolling 2\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0238)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0239", "log", "\">>>>>>>>>>>>>>>>>>>>>>>> stopUsbPolling 2-1 usbPollingEnabled:\"%s\" usbService:\"%s", "\">>>>>>>>>>>>>>>>>>>>>>>> stopUsbPolling 2-1 usbPollingEnabled:\"%s\" usbService:\"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0239)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0240", "log", "\">>>>>>>>>>>>>>>>>>>>>>>> stopUsbPolling 3\"", "\">>>>>>>>>>>>>>>>>>>>>>>> stopUsbPolling 3\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0240)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0241", "log", "\"UsbService or command queue is null; stopping polling\"", "\"UsbService or command queue is null; stopping polling\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0241)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0242", "log", "\">>>>>>>>>>>>>>>>>>>>>>>> stopUsbPolling 4\"", "\">>>>>>>>>>>>>>>>>>>>>>>> stopUsbPolling 4\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0242)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0243", "log", "\"Failed to enqueue polling command (queue may be full)\"", "\"Failed to enqueue polling command (queue may be full)\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0243)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0244", "log", "\"USB polling failure threshold reached; backing off\"", "\"USB polling failure threshold reached; backing off\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0244)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0245", "log", "\"USB service error\"", "\"USB service error\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0245)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0246", "log", "\"USB polling scheduled (interval: \" + usbPollingIntervalMs + \" ms)\"", "\"USB polling scheduled (interval: \" + usbPollingIntervalMs + \" ms)\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0246)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0247", "log", "\"Failed to schedule USB polling\"", "\"Failed to schedule USB polling\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0247)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0248", "log", "\"USB polling stopped\"", "\"USB polling stopped\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0248)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0249", "log", "\"USB command queue is not running, cannot send command: \"%s", "\"USB command queue is not running, cannot send command: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0249)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0250", "log", "\"Invalid command, cannot send: \"%s", "\"Invalid command, cannot send: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0250)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0251", "log", "\"USB command enqueued: \"%s", "\"USB command enqueued: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0251)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0252", "log", "\"Failed to enqueue USB command: \"%s", "\"Failed to enqueue USB command: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0252)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0253", "log", "\">>>>>>>>>>>>>>>>>>>>>>>> startUsbPolling 4\"", "\">>>>>>>>>>>>>>>>>>>>>>>> startUsbPolling 4\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0253)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0254", "log", "\"USB reconnect failed after \" + usbReconnectAttempts + \" attempts\"", "\"USB reconnect failed after \" + usbReconnectAttempts + \" attempts\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0254)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0255", "log", "\">>>>>>>>>>>>>>>>>>>>>>>> stopUsbPolling 6\"", "\">>>>>>>>>>>>>>>>>>>>>>>> stopUsbPolling 6\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0255)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0256", "log", "\"USB reconnect attempt failed\"", "\"USB reconnect attempt failed\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0256)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0257", "log", "\"> 2.usbConnPermissionGranted \"%s\" / usbService \"%s\" / usbPollingEnabled \"%s", "\"> 2.usbConnPermissionGranted \"%s\" / usbService \"%s\" / usbPollingEnabled \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0257)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0258", "log", "\"> 2.isUsbReady() \"%s\" / isBluetoothReady() \"%s", "\"> 2.isUsbReady() \"%s\" / isBluetoothReady() \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0258)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0259", "log", "\"USB permission already granted; skipping recovery scheduling\"", "\"USB permission already granted; skipping recovery scheduling\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0259)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0260", "log", "\"USB permission granted before recovery runnable executed\"", "\"USB permission granted before recovery runnable executed\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0260)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0261", "log", "\"Attempting USB permission recovery\"", "\"Attempting USB permission recovery\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0261)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0262", "log", "\"Error while setting USB filters during recovery\"", "\"Error while setting USB filters during recovery\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0262)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0263", "log", "\"Error while rebinding USB service during recovery\"", "\"Error while rebinding USB service during recovery\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0263)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0264", "log", "\"Bluetooth adapter null, skipping reconnect\"", "\"Bluetooth adapter null, skipping reconnect\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0264)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0265", "log", "\"BLUETOOTH_CONNECT permission missing; cannot attempt reconnect\"", "\"BLUETOOTH_CONNECT permission missing; cannot attempt reconnect\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0265)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0266", "log", "\"Bluetooth adapter disabled, skipping reconnect\"", "\"Bluetooth adapter disabled, skipping reconnect\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0266)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0267", "log", "\"No paired Bluetooth devices available\"", "\"No paired Bluetooth devices available\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0267)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0268", "log", "\"BLUETOOTH_CONNECT permission not granted\"", "\"BLUETOOTH_CONNECT permission not granted\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0268)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0269", "log", "\"ì´ë¯¸ ì°ê²°ë ë¸ë£¨í¬ì¤ ì¥ë¹ ì ì§: %s / %s\"", "\"ì´ë¯¸ ì°ê²°ë ë¸ë£¨í¬ì¤ ì¥ë¹ ì ì§: %s / %s\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0269)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0270", "log", "\"â CRITICAL: btSocket.connect() called on UI thread! This will cause ANR!\"", "\"â CRITICAL: btSocket.connect() called on UI thread! This will cause ANR!\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0270)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0271", "log", "\"Bluetooth socket connection error\"", "\"Bluetooth socket connection error\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0271)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0272", "log", "\"Failed to check Bluetooth device connection state\"", "\"Failed to check Bluetooth device connection state\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0272)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0273", "log", "\"Error in UI update\"", "\"Error in UI update\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0273)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0274", "log", "\"Error in immediate UI update\"", "\"Error in immediate UI update\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0274)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0275", "log", "\"Error in UI update batch\"", "\"Error in UI update batch\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0275)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0276", "log", "\"finalReceiveCommand is null or empty, skipping database query\"", "\"finalReceiveCommand is null or empty, skipping database query\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0276)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0277", "log", "\"Error querying test spec data for command: \"%s", "\"Error querying test spec data for command: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0277)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0278", "log", "\"Unable to compute SHA-256 hash\"", "\"Unable to compute SHA-256 hash\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0278)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0279", "log", "\"Bluetooth adapter not available\"", "\"Bluetooth adapter not available\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0279)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0280", "log", "\"sendBtMessage CM0200 -> CM0100: \"%s", "\"sendBtMessage CM0200 -> CM0100: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0280)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0281", "log", "\">>>>>>>>>> sendBtMessage.sendMessage \"%s", "\">>>>>>>>>> sendBtMessage.sendMessage \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0281)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0282", "log", "\"sendBtMessage error\"", "\"sendBtMessage error\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0282)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0283", "log", "\"Bluetooth adapter unavailable; cannot list devices\"", "\"Bluetooth adapter unavailable; cannot list devices\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0283)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0284", "log", "\"íì´ë§ë ë¸ë£¨í¬ì¤ ì¥ë¹ - enabled: %s, count: %d, entranceCheck: %s\"", "\"íì´ë§ë ë¸ë£¨í¬ì¤ ì¥ë¹ - enabled: %s, count: %d, entranceCheck: %s\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0284)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0285", "log", "\"listPairedDevicesSelect error\"", "\"listPairedDevicesSelect error\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0285)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0286", "log", "\"Bluetooth connection failed!\"", "\"Bluetooth connection failed!\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0286)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0287", "log", "\"Connection attempt timed out and was canceled.\"", "\"Connection attempt timed out and was canceled.\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0287)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0288", "log", "\"Bluetooth cancel connection error\"", "\"Bluetooth cancel connection error\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0288)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0289", "log", "\"Error canceling Bluetooth thread during cancelConnection: \"%s", "\"Error canceling Bluetooth thread during cancelConnection: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0289)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0290", "log", "\"Could not create Insecure RFComm Connection\"", "\"Could not create Insecure RFComm Connection\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0290)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0291", "log", "\"USB intent action: \"%s", "\"USB intent action: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0291)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0292", "log", "\"USB permission granted - starting polling\"", "\"USB permission granted - starting polling\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0292)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0293", "log", "\">>>>>>>>>>>>>>>>>>>>>>>> stopUsbPolling 7\"", "\">>>>>>>>>>>>>>>>>>>>>>>> stopUsbPolling 7\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0293)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0294", "log", "\">>>>>>>>>>>>>>>>>>>>>>>> stopUsbPolling 8\"", "\">>>>>>>>>>>>>>>>>>>>>>>> stopUsbPolling 8\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0294)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0295", "log", "\"USB Connection state: \"%s", "\"USB Connection state: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0295)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0296", "log", "\"USB command queue initialized\"", "\"USB command queue initialized\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0296)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0297", "log", "\">>>>>>>>>>>>>>>>>>>>>>>> USB Handler ë±ë¡ ì : usbHandler=\"%s", "\">>>>>>>>>>>>>>>>>>>>>>>> USB Handler ë±ë¡ ì : usbHandler=\"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0297)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0298", "log", "\">>>>>>>>>>>>>>>>>>>>>>>> USB Handler ìë¡ ìì±\"", "\">>>>>>>>>>>>>>>>>>>>>>>> USB Handler ìë¡ ìì±\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0298)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0299", "log", "\">>>>>>>>>>>>>>>>>>>>>>>> USB Handler ë±ë¡ ìë£\"", "\">>>>>>>>>>>>>>>>>>>>>>>> USB Handler ë±ë¡ ìë£\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0299)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0300", "log", "\">>>>>>>>>>>>>>>>>>>>>>>> USB Handlerê° nullìëë¤!\"", "\">>>>>>>>>>>>>>>>>>>>>>>> USB Handlerê° nullìëë¤!\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0300)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0301", "log", "\"USB Handler is null, cannot register with UsbService\"", "\"USB Handler is null, cannot register with UsbService\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0301)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0302", "log", "\"USB Service connected successfully\"", "\"USB Service connected successfully\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0302)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0303", "log", "\"USB ìë¹ì¤ ì°ê²°ë¨. ìë ì°ê²° ë©ìì§ ì ì¡ ì¤ë¹\"", "\"USB ìë¹ì¤ ì°ê²°ë¨. ìë ì°ê²° ë©ìì§ ì ì¡ ì¤ë¹\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0303)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0304", "log", "\"USB ê¶íì´ ì´ë¯¸ ë¶ì¬ëì´ ìì. ìë ì°ê²° ë©ìì§ ì ì¡ ìì\"", "\"USB ê¶íì´ ì´ë¯¸ ë¶ì¬ëì´ ìì. ìë ì°ê²° ë©ìì§ ì ì¡ ìì\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0304)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0305", "log", "\">>>>>>>>>>>>>>>>>>>>>>>> startUsbPolling 5\"", "\">>>>>>>>>>>>>>>>>>>>>>>> startUsbPolling 5\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0305)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0306", "log", "\"USB ê¶í ëê¸° ì¤. ê¶í ë¶ì¬ í ìë ì°ê²° ììë¨\"", "\"USB ê¶í ëê¸° ì¤. ê¶í ë¶ì¬ í ìë ì°ê²° ììë¨\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0306)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0307", "log", "\"USB ì°ê²° Timerê° ì´ë¯¸ ì¤í ì¤ìëë¤.\"", "\"USB ì°ê²° Timerê° ì´ë¯¸ ì¤í ì¤ìëë¤.\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0307)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0308", "log", "\"UsbService is null after binding\"", "\"UsbService is null after binding\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0308)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0309", "log", "\"Invalid binder type. Expected UsbService.UsbBinder, got: \"%s", "\"Invalid binder type. Expected UsbService.UsbBinder, got: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0309)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0310", "log", "\"ClassCastException in onServiceConnected\"", "\"ClassCastException in onServiceConnected\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0310)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0311", "log", "\"USB Service disconnected\"", "\"USB Service disconnected\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0311)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0312", "log", "\"Bluetooth permissions granted on resume\"", "\"Bluetooth permissions granted on resume\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0312)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0313", "log", "\"USB receiver registered but service is null, attempting rebind\"", "\"USB receiver registered but service is null, attempting rebind\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0313)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0314", "log", "\"USB service already initialized, skipping onResume setup\"", "\"USB service already initialized, skipping onResume setup\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0314)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0315", "log", "\"onResume USB service error\"", "\"onResume USB service error\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0315)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0316", "log", "\"USB reconnected - onNewIntent called\"", "\"USB reconnected - onNewIntent called\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0316)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0317", "log", "\">>>>>>>>>>>>>>>>>>>>>>>> startUsbPolling 1\"", "\">>>>>>>>>>>>>>>>>>>>>>>> startUsbPolling 1\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0317)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0318", "log", "\"USB polling restarted after reconnection\"", "\"USB polling restarted after reconnection\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0318)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0319", "log", "\"Error in handleUsbReconnection\"", "\"Error in handleUsbReconnection\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0319)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0320", "log", "\"Failed to reset state for USB reconnect\"", "\"Failed to reset state for USB reconnect\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0320)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0321", "log", "\">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> resetCnt \"%s\" usbReconnectAttempts \"%s", "\">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> resetCnt \"%s\" usbReconnectAttempts \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0321)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0322", "log", "\"USB service already active; skipping restart\"", "\"USB service already active; skipping restart\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0322)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0323", "log", "\"USB service bind attempt: \"%s", "\"USB service bind attempt: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0323)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0324", "log", "\"USB receiver already registered: \"%s", "\"USB receiver already registered: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0324)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0325", "log", "\"setFilters error\"", "\"setFilters error\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0325)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0326", "log", "\"â¶ [US] UsbHandler ìì±: Looper=\"%s\", Thread=\"%s", "\"â¶ [US] UsbHandler ìì±: Looper=\"%s\", Thread=\"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0326)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0327", "log", "\"UsbHandler message processing error\"", "\"UsbHandler message processing error\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0327)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0328", "log", "\"readTemperatureExcel IOException\"", "\"readTemperatureExcel IOException\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0328)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0329", "log", "\"readTemperatureExcel BiffException\"", "\"readTemperatureExcel BiffException\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0329)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0330", "log", "\"readTemperatureExcel error\"", "\"readTemperatureExcel error\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0330)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0331", "log", "\"><><><>>>>>>>>>>>>>>>>>>>> urlStrBarcode \"%s", "\"><><><>>>>>>>>>>>>>>>>>>>> urlStrBarcode \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0331)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0332", "log", "\"ì í ìë¦¬ì¼ ë²í¸ ì ë³´ í¸ì¶: %s (code: %d, OK: %s)\"", "\"ì í ìë¦¬ì¼ ë²í¸ ì ë³´ í¸ì¶: %s (code: %d, OK: %s)\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0332)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0333", "log", "\"ì í ìë¦¬ì¼ ë²í¸ ì ë³´ í¸ì¶ ì±ê³µ (code: %d)\"", "\"ì í ìë¦¬ì¼ ë²í¸ ì ë³´ í¸ì¶ ì±ê³µ (code: %d)\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0333)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0334", "log", "\"ì í ìë¦¬ì¼ ë²í¸ ì ë³´ í¸ì¶ data received\"", "\"ì í ìë¦¬ì¼ ë²í¸ ì ë³´ í¸ì¶ data received\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0334)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0335", "log", "\"Error closing reader in RequestThreadBarcode\"", "\"Error closing reader in RequestThreadBarcode\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0335)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0336", "log", "\"ì í ìë¦¬ì¼ ë²í¸ ì ë³´ í¸ì¶ ì¤í¨ (code: %d)\"", "\"ì í ìë¦¬ì¼ ë²í¸ ì ë³´ í¸ì¶ ì¤í¨ (code: %d)\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0336)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0337", "log", "\"RequestThreadBarcode error\"", "\"RequestThreadBarcode error\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0337)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0338", "log", "\"jsonParsingBarcode ìëµ: \"%s", "\"jsonParsingBarcode ìëµ: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0338)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0339", "log", "\"jsonParsingBarcode - globalProductSerialNo JSON received\"", "\"jsonParsingBarcode - globalProductSerialNo JSON received\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0339)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0340", "log", "\"testItemArray.length(): \"%s", "\"testItemArray.length(): \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0340)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0341", "log", "\"lstData.size(): \"%s", "\"lstData.size(): \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0341)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0342", "log", "\"ì í ìë¦¬ì¼ ë²í¸ ì ë³´ í¸ì¶[%d]: %s\"", "\"ì í ìë¦¬ì¼ ë²í¸ ì ë³´ í¸ì¶[%d]: %s\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0342)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0343", "log", "\"Product serial parsing error\"", "\"Product serial parsing error\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0343)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0344", "log", "\"globalProductSerialNo: %s, productSerialNo: %s\"", "\"globalProductSerialNo: %s, productSerialNo: %s\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0344)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0345", "log", "\"jsonParsingBarcode JSONException\"", "\"jsonParsingBarcode JSONException\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0345)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0346", "log", "\"All resources cleaned up successfully\"", "\"All resources cleaned up successfully\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0346)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0347", "log", "\"Error cleaning up resources\"", "\"Error cleaning up resources\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0347)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0348", "log", "\"Resetting ActivityModel_0002 state\"", "\"Resetting ActivityModel_0002 state\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0348)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0349", "log", "\"Activity state reset completed\"", "\"Activity state reset completed\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0349)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0350", "log", "\"Failed to reset Activity state\"", "\"Failed to reset Activity state\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0350)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0351", "log", "\">>>>>>>>>>>>>>>>>>>>>>>> stopUsbPolling 9\"", "\">>>>>>>>>>>>>>>>>>>>>>>> stopUsbPolling 9\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0351)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0352", "log", "\"USB receiver already unregistered\"", "\"USB receiver already unregistered\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0352)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0353", "log", "\"USB service already unbound\"", "\"USB service already unbound\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0353)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0354", "log", "\"Error stopping USB service: \"%s", "\"Error stopping USB service: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0354)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0355", "log", "\"Error cleaning up USB resources\"", "\"Error cleaning up USB resources\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0355)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0356", "log", "\"Error canceling Bluetooth thread: \"%s", "\"Error canceling Bluetooth thread: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0356)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0357", "log", "\"Error closing Bluetooth socket: \"%s", "\"Error closing Bluetooth socket: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0357)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0358", "log", "\"Error cleaning up Bluetooth resources\"", "\"Error cleaning up Bluetooth resources\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0358)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0359", "log", "\"Error canceling test task thread: \"%s", "\"Error canceling test task thread: \"%s", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0359)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0360", "log", "\"Error cleaning up AsyncTasks\"", "\"Error cleaning up AsyncTasks\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0360)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0361", "log", "\"Error cleaning up HTTP connections\"", "\"Error cleaning up HTTP connections\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0361)", "0", timestamp, timestamp},
                {"log.activity_model_test_process.0362", "log", "\"Error setting up kiosk mode\"", "\"Error setting up kiosk mode\"", "", "", "ActivityModelTestProcess log (log.activity_model_test_process.0362)", "0", timestamp, timestamp},
            };

            db.beginTransaction();
            try {
                String sql = "INSERT OR IGNORE INTO tbl_string_resources (" +
                        "clm_string_key, clm_string_category, clm_string_ko, clm_string_en, clm_string_zh, clm_string_ja, " +
                        "clm_description, clm_is_user_visible, clm_created_timestamp, clm_updated_timestamp) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                for (String[] row : logStrings) {
                    db.execSQL(sql, row);
                }

                db.setTransactionSuccessful();
                Log.i(TAG, "> TestData.migrateActivityModelTestProcessLogStrings: Migrated " + logStrings.length + " log strings");
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            Log.e(TAG, "> TestData.migrateActivityModelTestProcessLogStrings.e.1 : " + e);
        } finally {
            if (helper != null) {
                helper.close();
            }
        }
    }

    public static void insertStringResource(Context context, String key, String category, String ko, String en,
                                            String zh, String ja, String description, int isUserVisible) {
        DBHelper helper = null;
        SQLiteDatabase db = null;

        try {
            helper = new DBHelper(context, "itf_temperature_table.db", null, DATABASE_VERSION);
            db = helper.getWritableDatabase();

            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            ContentValues values = new ContentValues();
            values.put("clm_string_key", key);
            values.put("clm_string_category", category);
            values.put("clm_string_ko", ko);
            values.put("clm_string_en", en);
            values.put("clm_string_zh", zh);
            values.put("clm_string_ja", ja);
            values.put("clm_description", description);
            values.put("clm_is_user_visible", isUserVisible);
            values.put("clm_created_timestamp", timestamp);
            values.put("clm_updated_timestamp", timestamp);

            db.insertWithOnConflict("tbl_string_resources", null, values, SQLiteDatabase.CONFLICT_IGNORE);
        } catch (Exception e) {
            Log.e(TAG, "> TestData.insertStringResource.e : " + e);
        } finally {
            if (helper != null) {
                helper.close();
            }
        }
    }

    public static void updateStringResource(Context context, String key, String category, String ko, String en,
                                            String zh, String ja, String description, int isUserVisible) {
        DBHelper helper = null;
        SQLiteDatabase db = null;

        try {
            helper = new DBHelper(context, "itf_temperature_table.db", null, DATABASE_VERSION);
            db = helper.getWritableDatabase();

            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            ContentValues values = new ContentValues();
            values.put("clm_string_category", category);
            values.put("clm_string_ko", ko);
            values.put("clm_string_en", en);
            values.put("clm_string_zh", zh);
            values.put("clm_string_ja", ja);
            values.put("clm_description", description);
            values.put("clm_is_user_visible", isUserVisible);
            values.put("clm_updated_timestamp", timestamp);

            db.update("tbl_string_resources", values, "clm_string_key = ?", new String[] { key });
        } catch (Exception e) {
            Log.e(TAG, "> TestData.updateStringResource.e : " + e);
        } finally {
            if (helper != null) {
                helper.close();
            }
        }
    }

    public static void deleteStringResource(Context context, String key) {
        DBHelper helper = null;
        SQLiteDatabase db = null;

        try {
            helper = new DBHelper(context, "itf_temperature_table.db", null, DATABASE_VERSION);
            db = helper.getWritableDatabase();
            db.delete("tbl_string_resources", "clm_string_key = ?", new String[] { key });
        } catch (Exception e) {
            Log.e(TAG, "> TestData.deleteStringResource.e : " + e);
        } finally {
            if (helper != null) {
                helper.close();
            }
        }
    }

    public static Map<String, String> getStringResourceByKey(Context context, String key) {
        DBHelper helper = null;
        SQLiteDatabase db = null;
        Cursor cursor = null;
        Map<String, String> result = new HashMap<>();

        try {
            helper = new DBHelper(context, "itf_temperature_table.db", null, DATABASE_VERSION);
            db = helper.getReadableDatabase();

            cursor = db.rawQuery("SELECT * FROM tbl_string_resources WHERE clm_string_key = ?", new String[] { key });
            if (cursor.moveToFirst()) {
                result.put("clm_string_key", cursor.getString(cursor.getColumnIndexOrThrow("clm_string_key")));
                result.put("clm_string_category", cursor.getString(cursor.getColumnIndexOrThrow("clm_string_category")));
                result.put("clm_string_ko", cursor.getString(cursor.getColumnIndexOrThrow("clm_string_ko")));
                result.put("clm_string_en", cursor.getString(cursor.getColumnIndexOrThrow("clm_string_en")));
                result.put("clm_string_zh", cursor.getString(cursor.getColumnIndexOrThrow("clm_string_zh")));
                result.put("clm_string_ja", cursor.getString(cursor.getColumnIndexOrThrow("clm_string_ja")));
                result.put("clm_description", cursor.getString(cursor.getColumnIndexOrThrow("clm_description")));
                result.put("clm_is_user_visible", String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow("clm_is_user_visible"))));
                result.put("clm_created_timestamp", cursor.getString(cursor.getColumnIndexOrThrow("clm_created_timestamp")));
                result.put("clm_updated_timestamp", cursor.getString(cursor.getColumnIndexOrThrow("clm_updated_timestamp")));
            }
        } catch (Exception e) {
            Log.e(TAG, "> TestData.getStringResourceByKey.e : " + e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (helper != null) {
                helper.close();
            }
        }

        return result;
    }

    public static List<Map<String, String>> getStringResourcesByCategory(Context context, String category) {
        DBHelper helper = null;
        SQLiteDatabase db = null;
        Cursor cursor = null;
        List<Map<String, String>> result = new ArrayList<>();

        try {
            helper = new DBHelper(context, "itf_temperature_table.db", null, DATABASE_VERSION);
            db = helper.getReadableDatabase();

            cursor = db.rawQuery("SELECT * FROM tbl_string_resources WHERE clm_string_category = ?", new String[] { category });
            while (cursor.moveToNext()) {
                Map<String, String> row = new HashMap<>();
                row.put("clm_string_key", cursor.getString(cursor.getColumnIndexOrThrow("clm_string_key")));
                row.put("clm_string_category", cursor.getString(cursor.getColumnIndexOrThrow("clm_string_category")));
                row.put("clm_string_ko", cursor.getString(cursor.getColumnIndexOrThrow("clm_string_ko")));
                row.put("clm_string_en", cursor.getString(cursor.getColumnIndexOrThrow("clm_string_en")));
                row.put("clm_string_zh", cursor.getString(cursor.getColumnIndexOrThrow("clm_string_zh")));
                row.put("clm_string_ja", cursor.getString(cursor.getColumnIndexOrThrow("clm_string_ja")));
                row.put("clm_description", cursor.getString(cursor.getColumnIndexOrThrow("clm_description")));
                row.put("clm_is_user_visible", String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow("clm_is_user_visible"))));
                row.put("clm_created_timestamp", cursor.getString(cursor.getColumnIndexOrThrow("clm_created_timestamp")));
                row.put("clm_updated_timestamp", cursor.getString(cursor.getColumnIndexOrThrow("clm_updated_timestamp")));
                result.add(row);
            }
        } catch (Exception e) {
            Log.e(TAG, "> TestData.getStringResourcesByCategory.e : " + e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (helper != null) {
                helper.close();
            }
        }

        return result;
    }

}
