package itf.com.app.lms.util;

import static itf.com.app.lms.util.Constants.InitialValues.DATABASE_VERSION;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 다국어 문자열 리소스 관리 클래스
 * 
 * 기능:
 * - tbl_setting_info에서 언어 설정 조회
 * - tbl_string_resources에서 문자열 조회
 * - 메모리 캐싱으로 성능 최적화
 * - 런타임 언어 변경 지원
 */
public class StringResourceManager {
    
    // ==================== 싱글톤 인스턴스 ====================
    
    private static volatile StringResourceManager instance;
    private static final Object INSTANCE_LOCK = new Object();
    
    // ==================== 상수 ====================
    
    private static final String SETTING_ID_LANGUAGE = "APP_LANGUAGE";
    private static final String DEFAULT_LANGUAGE = "ko";
    private static final String DATABASE_NAME = "itf_temperature_table.db";
    
    // 지원하는 언어 목록
    private static final String[] SUPPORTED_LANGUAGES = {"ko", "en", "zh", "ja"};
    private static final String[] LANGUAGE_DISPLAY_NAMES = {"한국어", "English", "中文", "日本語"};
    
    // ==================== 인스턴스 변수 ====================
    
    private String currentLanguage = DEFAULT_LANGUAGE;
    private final Map<String, Map<String, String>> stringCache = new ConcurrentHashMap<>();
    private final List<LanguageChangeListener> languageChangeListeners = new ArrayList<>();
    private Context applicationContext;
    
    // ==================== 생성자 ====================
    
    private StringResourceManager() {
        // private 생성자
    }
    
    /**
     * StringResourceManager 인스턴스 가져오기 (싱글톤)
     */
    public static StringResourceManager getInstance() {
        if (instance == null) {
            synchronized (INSTANCE_LOCK) {
                if (instance == null) {
                    instance = new StringResourceManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * 초기화 (Application Context 필요)
     */
    public void initialize(Context context) {
        if (applicationContext == null) {
            applicationContext = context.getApplicationContext();
            // 현재 언어 로드
            currentLanguage = loadLanguageFromDatabase();
            // 현재 언어의 문자열 캐시 로드
            loadStringsForLanguage(currentLanguage);
        }
    }
    
    // ==================== 언어 관리 ====================
    
    /**
     * 현재 언어 조회
     */
    public String getCurrentLanguage() {
        if (currentLanguage == null || currentLanguage.isEmpty()) {
            currentLanguage = loadLanguageFromDatabase();
        }
        return currentLanguage;
    }
    
    /**
     * 언어 설정 및 저장
     * @param languageCode 언어 코드 ("ko", "en", "zh", "ja")
     */
    public void setLanguage(String languageCode) {
        if (languageCode == null || languageCode.isEmpty()) {
            return;
        }
        
        // 지원하는 언어인지 확인
        boolean isSupported = false;
        for (String lang : SUPPORTED_LANGUAGES) {
            if (lang.equals(languageCode)) {
                isSupported = true;
                break;
            }
        }
        
        if (!isSupported) {
            LogManager.w(LogManager.LogCategory.ER, "StringResourceManager", 
                "Unsupported language code: " + languageCode);
            return;
        }
        
        String oldLanguage = currentLanguage;
        currentLanguage = languageCode;
        
        // DB에 저장
        saveLanguageToDatabase(languageCode);
        
        // 새 언어의 문자열 로드 (캐시에 없으면)
        if (!stringCache.containsKey(languageCode)) {
            loadStringsForLanguage(languageCode);
        }
        
        // 언어 변경 리스너 호출
        notifyLanguageChanged(languageCode);
        
        LogManager.i(LogManager.LogCategory.PS, "StringResourceManager", 
            "Language changed from " + oldLanguage + " to " + languageCode);
    }
    
    /**
     * 지원하는 언어 목록 조회
     */
    public List<String> getSupportedLanguages() {
        List<String> languages = new ArrayList<>();
        for (String lang : SUPPORTED_LANGUAGES) {
            languages.add(lang);
        }
        return languages;
    }
    
    /**
     * 언어 표시명 조회
     */
    public String[] getLanguageDisplayNames() {
        return LANGUAGE_DISPLAY_NAMES.clone();
    }
    
    // ==================== 문자열 조회 ====================
    
    /**
     * 기본 문자열 조회
     * @param key 문자열 키
     * @return 번역된 문자열
     */
    public String getString(String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        
        String language = getCurrentLanguage();
        
        // 캐시에서 조회
        Map<String, String> languageCache = stringCache.get(language);
        if (languageCache != null && languageCache.containsKey(key)) {
            return languageCache.get(key);
        }
        
        // DB에서 조회
        String value = loadStringFromDatabase(key, language);
        if (value != null && !value.isEmpty()) {
            // 캐시에 저장
            if (languageCache == null) {
                languageCache = new ConcurrentHashMap<>();
                stringCache.put(language, languageCache);
            }
            languageCache.put(key, value);
            return value;
        }
        
        // 폴백: 한국어 시도
        if (!language.equals("ko")) {
            value = loadStringFromDatabase(key, "ko");
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        
        // 폴백: 영어 시도
        if (!language.equals("en")) {
            value = loadStringFromDatabase(key, "en");
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        
        // 모두 실패하면 키 반환 (디버깅 용이)
        LogManager.w(LogManager.LogCategory.ER, "StringResourceManager", 
            "String not found for key: " + key + " (language: " + language + ")");
        return key;
    }
    
    /**
     * 파라미터가 있는 문자열 조회
     * @param key 문자열 키
     * @param args 파라미터 (예: "Hello %s" -> getString("greeting", "World"))
     * @return 번역된 문자열
     */
    public String getString(String key, Object... args) {
        String template = getString(key);
        if (template == null || template.isEmpty() || args == null || args.length == 0) {
            return template;
        }
        
        try {
            return String.format(template, args);
        } catch (Exception e) {
            LogManager.e(LogManager.LogCategory.ER, "StringResourceManager", 
                "Error formatting string: " + key, e);
            return template;
        }
    }
    
    /**
     * 에러 메시지 조회 (편의 메서드)
     */
    public String getErrorString(String subKey) {
        return getString("error." + subKey);
    }
    
    /**
     * UI 문자열 조회 (편의 메서드)
     */
    public String getUIString(String subKey) {
        return getString("ui." + subKey);
    }
    
    /**
     * 버튼 텍스트 조회 (편의 메서드)
     */
    public String getButtonString(String subKey) {
        return getString("button." + subKey);
    }
    
    /**
     * 다이얼로그 메시지 조회 (편의 메서드)
     */
    public String getDialogString(String subKey) {
        return getString("dialog." + subKey);
    }
    
    // ==================== 캐시 관리 ====================
    
    /**
     * 캐시 초기화 및 DB에서 다시 로드
     */
    public void reloadStrings() {
        stringCache.clear();
        loadStringsForLanguage(getCurrentLanguage());
    }
    
    /**
     * 특정 언어의 캐시만 초기화
     */
    public void clearCache(String languageCode) {
        stringCache.remove(languageCode);
    }
    
    /**
     * 전체 캐시 초기화
     */
    public void clearAllCache() {
        stringCache.clear();
    }
    
    // ==================== 언어 변경 이벤트 ====================
    
    /**
     * 언어 변경 리스너 등록
     */
    public void addLanguageChangeListener(LanguageChangeListener listener) {
        if (listener != null && !languageChangeListeners.contains(listener)) {
            languageChangeListeners.add(listener);
        }
    }
    
    /**
     * 언어 변경 리스너 제거
     */
    public void removeLanguageChangeListener(LanguageChangeListener listener) {
        languageChangeListeners.remove(listener);
    }
    
    /**
     * 언어 변경 인터페이스
     */
    public interface LanguageChangeListener {
        void onLanguageChanged(String newLanguage);
    }
    
    /**
     * 언어 변경 알림
     */
    private void notifyLanguageChanged(String newLanguage) {
        for (LanguageChangeListener listener : languageChangeListeners) {
            try {
                listener.onLanguageChanged(newLanguage);
            } catch (Exception e) {
                LogManager.e(LogManager.LogCategory.ER, "StringResourceManager", 
                    "Error notifying language change listener", e);
            }
        }
    }
    
    // ==================== 내부 메서드 ====================
    
    /**
     * DB에서 언어 설정 조회
     */
    private String loadLanguageFromDatabase() {
        if (applicationContext == null) {
            return DEFAULT_LANGUAGE;
        }
        
        DBHelper helper = null;
        SQLiteDatabase db = null;
        Cursor cursor = null;
        
        try {
            helper = new DBHelper(applicationContext, DATABASE_NAME, null, Constants.InitialValues.DATABASE_VERSION);
            db = helper.getReadableDatabase();
            
            String sql = "SELECT clm_setting_value FROM tbl_setting_info WHERE clm_setting_id = ?";
            cursor = db.rawQuery(sql, new String[]{SETTING_ID_LANGUAGE});
            
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex("clm_setting_value");
                if (columnIndex >= 0) {
                    String language = cursor.getString(columnIndex);
                    if (language != null && !language.isEmpty()) {
                        return language;
                    }
                }
            }
        } catch (Exception e) {
            LogManager.e(LogManager.LogCategory.ER, "StringResourceManager", 
                "Error loading language from database", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (helper != null) {
                helper.close();
            }
        }
        
        return DEFAULT_LANGUAGE;
    }
    
    /**
     * DB에 언어 설정 저장
     */
    private void saveLanguageToDatabase(String languageCode) {
        if (applicationContext == null) {
            return;
        }
        
        DBHelper helper = null;
        SQLiteDatabase db = null;
        
        try {
            helper = new DBHelper(applicationContext, DATABASE_NAME, null, Constants.InitialValues.DATABASE_VERSION);
            db = helper.getWritableDatabase();
            
            // UPDATE 또는 INSERT
            android.content.ContentValues values = new android.content.ContentValues();
            values.put("clm_setting_value", languageCode);
            values.put("clm_test_timestamp", "datetime('now')");

            int rowsAffected = db.update("tbl_setting_info", values, "clm_setting_id = ?", new String[]{SETTING_ID_LANGUAGE});

            if (rowsAffected == 0) {
                // INSERT (없으면 추가)
                values.clear();
                values.put("clm_setting_seq", "1");
                values.put("clm_setting_name_kr", "애플리케이션 언어");
                values.put("clm_setting_name_en", "Application Language");
                values.put("clm_setting_id", SETTING_ID_LANGUAGE);
                values.put("clm_setting_value", languageCode);
                values.put("clm_comment", "애플리케이션 표시 언어를 선택합니다");
                values.put("clm_test_timestamp", "datetime('now')");
                db.insert("tbl_setting_info", null, values);
            }
        } catch (Exception e) {
            LogManager.e(LogManager.LogCategory.ER, "StringResourceManager", 
                "Error saving language to database", e);
        } finally {
            if (helper != null) {
                helper.close();
            }
        }
    }
    
    /**
     * DB에서 문자열 조회 (캐시 미스 시)
     */
    private String loadStringFromDatabase(String key, String language) {
        if (applicationContext == null || key == null || language == null) {
            return null;
        }
        
        DBHelper helper = null;
        SQLiteDatabase db = null;
        Cursor cursor = null;
        
        try {
            helper = new DBHelper(applicationContext, DATABASE_NAME, null, Constants.InitialValues.DATABASE_VERSION);
            db = helper.getReadableDatabase();
            
            String columnName = "clm_string_" + language;
            String sql = "SELECT " + columnName + " FROM tbl_string_resources WHERE clm_string_key = ?";
            cursor = db.rawQuery(sql, new String[]{key});
            
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(columnName);
                if (columnIndex >= 0) {
                    String value = cursor.getString(columnIndex);
                    if (value != null && !value.isEmpty()) {
                        return value;
                    }
                }
            }
        } catch (Exception e) {
            LogManager.e(LogManager.LogCategory.ER, "StringResourceManager", 
                "Error loading string from database: " + key + " (language: " + language + ")", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (helper != null) {
                helper.close();
            }
        }
        
        return null;
    }
    
    /**
     * DB에서 특정 언어의 모든 문자열 로드 (캐시 초기화 시)
     */
    private void loadStringsForLanguage(String language) {
        if (applicationContext == null || language == null) {
            return;
        }
        
        DBHelper helper = null;
        SQLiteDatabase db = null;
        Cursor cursor = null;
        
        try {
            helper = new DBHelper(applicationContext, DATABASE_NAME, null, Constants.InitialValues.DATABASE_VERSION);
            db = helper.getReadableDatabase();
            
            String columnName = "clm_string_" + language;
            String sql = "SELECT clm_string_key, " + columnName + " FROM tbl_string_resources WHERE " + columnName + " IS NOT NULL AND " + columnName + " != ''";
            cursor = db.rawQuery(sql, null);
            
            Map<String, String> languageCache = new ConcurrentHashMap<>();
            
            if (cursor != null) {
                int keyIndex = cursor.getColumnIndex("clm_string_key");
                int valueIndex = cursor.getColumnIndex(columnName);
                
                while (cursor.moveToNext()) {
                    if (keyIndex >= 0 && valueIndex >= 0) {
                        String key = cursor.getString(keyIndex);
                        String value = cursor.getString(valueIndex);
                        if (key != null && value != null && !value.isEmpty()) {
                            languageCache.put(key, value);
                        }
                    }
                }
            }
            
            stringCache.put(language, languageCache);
            
            LogManager.i(LogManager.LogCategory.PS, "StringResourceManager", 
                "Loaded " + languageCache.size() + " strings for language: " + language);
        } catch (Exception e) {
            LogManager.e(LogManager.LogCategory.ER, "StringResourceManager", 
                "Error loading strings for language: " + language, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (helper != null) {
                helper.close();
            }
        }
    }
}

