package itf.com.app.simple_line_test_ovio_new.util;

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

import itf.com.app.simple_line_test_ovio_new.util.Constants;

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
                helper = new DBHelper(context, "itf_temperature_table.db", null, 2);
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
            helper = new DBHelper(context, "itf_temperature_table.db", null, 2);
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
            helper = new DBHelper(context, "itf_temperature_table.db", null, 2);
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
            helper = new DBHelper(context, "itf_temperature_table.db", null, 2);
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
            helper = new DBHelper(context, "itf_temperature_table.db", null, 2);
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
                helper = new DBHelper(context, "itf_temperature_table.db", null, 2);
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
            helper = new DBHelper(context, "itf_temperature_table.db", null, 2);
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
            helper = new DBHelper(context, "itf_temperature_table.db", null, 2);
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
            helper = new DBHelper(context, "itf_temperature_table.db", null, 2);
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
            helper = new DBHelper(context, "itf_temperature_table.db", null, 2);

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
            helper = new DBHelper(context, "itf_temperature_table.db", null, 2);
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
                helper = new DBHelper(context, "itf_temperature_table.db", null, 2);
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
            helper = new DBHelper(context, "itf_temperature_table.db", null, 2);

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
            helper = new DBHelper(context, "itf_temperature_table.db", null, 2);
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
            helper = new DBHelper(context, "itf_temperature_table.db", null, 2);
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
            helper = new DBHelper(context, "itf_temperature_table.db", null, 2);
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
            helper = new DBHelper(context, "itf_temperature_table.db", null, 2);
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
                        helper = new DBHelper(context, "itf_temperature_table.db", null, 2);

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
            helper = new DBHelper(context, "itf_temperature_table.db", null, 2);

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
            helper = new DBHelper(context, "itf_temperature_table.db", null, 2);
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
            helper = new DBHelper(context, "itf_temperature_table.db", null, 2);
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
            helper = new DBHelper(context, "itf_temperature_table.db", null, 2);
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
            helper = new DBHelper(context, "itf_temperature_table.db", null, 2);
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
            helper = new DBHelper(context, "itf_temperature_table.db", null, 2);

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
            helper = new DBHelper(context, "itf_temperature_table.db", null, 2);

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
            helper = new DBHelper(context, "itf_temperature_table.db", null, 2);
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
            helper = new DBHelper(context, "itf_temperature_table.db", null, 2);
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
            helper = new DBHelper(context, "itf_temperature_table.db", null, 2);
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
     * tbl_setting_info 테이블에서 데이터 삭제
     */
    public static boolean deleteSettingInfo(Context context, String settingId) {
        DBHelper helper = null;
        SQLiteDatabase db = null;
        String tableName = "tbl_setting_info";

        try {
            helper = new DBHelper(context, "itf_temperature_table.db", null, 2);
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
}
