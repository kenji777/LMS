package itf.com.app.lms.util;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 초기 에러 카탈로그 데이터 삽입
 * TestData.java에 추가할 메서드
 */
public class TestData_ErrorCatalog {
    
    private static final String TAG = "TestData";
    
    /**
     * 초기 에러 카탈로그 데이터 삽입
     */
    public static void insertInitialErrorCatalog(Context context) {
        DBHelper helper = null;
        SQLiteDatabase db = null;
        
        try {
            helper = new DBHelper(context, "itf_temperature_table.db", null, 
                Constants.InitialValues.DATABASE_VERSION);
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
}



