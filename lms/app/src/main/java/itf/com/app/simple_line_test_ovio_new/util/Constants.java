package itf.com.app.simple_line_test_ovio_new.util;

/**
 * 하드코딩된 문자열 값들을 관리하는 상수 클래스
 */
public class Constants {
    
    // ==================== 에러 메시지 ====================
    public static class ErrorMessages {
        public static final String BLUETOOTH_CANCEL_CONNECTION_ERROR = "Bluetooth cancel connection error";
        public static final String ERROR_PROCESSING_NEW_INTENT = "error_type_processing_new_intent";
        public static final String INVALID_BINDER_TYPE_EXPECTED_USB_SERVICE_USB_BINDER_GOT = "Invalid binder type. Expected UsbService.UsbBinder, got: ";
        public static final String CLASS_CAST_EXCEPTION_IN_ONSERVICECONNECTED = "ClassCastException in onServiceConnected";
        public static final String USB_SERVICE_IS_NULL_AFTER_BINDING = "UsbService is null after binding";
        public static final String LIST_SPEC_DATA_ERROR = "lstSpecData error: ";
        public static final String ON_DESTROY_ERROR = "onDestroy error: ";
        public static final String GET_LOCAL_IP_ADDRESS_ERROR = "getLocalIpAddress error";
        public static final String TEMPERATURE_DATA_READ_ERROR = "Temperature data read error";
        public static final String SERVER_CONNECTION_ERROR = "Server connection error";
        public static final String DIRECTORY_CREATION_FILE_ALREADY_EXISTS = "Directory creation - FileAlreadyExistsException";
        public static final String DIRECTORY_CREATION_NO_SUCH_FILE = "Directory creation - NoSuchFileException";
        public static final String DIRECTORY_CREATION_IO_EXCEPTION = "Directory creation - IOException";
        public static final String USB_SERVICE_START_ERROR = "USB Service start error";
        public static final String ERROR_IN_INITIAL_UI_UPDATE = "Error in initial UI update";
        public static final String TEMPERATURE_PROCESSING_ERROR = "Temperature processing error";
        public static final String HTTP_RESULT_UPDATE_ERROR = "HTTP result update error";
        public static final String ERROR_COLLECTING_LIST_ITEM_ADAPTER_DATA = "Error collecting listItemAdapter data";
        public static final String THREAD_EXECUTION_ERROR = "Thread execution error";
        public static final String THREAD_EXECUTION_ERROR_IN_RE0101 = "Thread execution error in RE0101";
        public static final String ERROR_IN_UI_UPDATE_BATCH = "Error in UI update batch";
        public static final String ERROR_IN_BACKGROUND_PROCESSING = "Error in background processing";
        public static final String BLUETOOTH_MESSAGE_HANDLING_ERROR = "Bluetooth message handling error";
        public static final String BLUETOOTH_HANDLER_ERROR = "Bluetooth handler error";
        public static final String BLUETOOTH_DEVICE_NAME_PARSING_ERROR = "Bluetooth device name parsing error";
        public static final String ERROR_UPDATING_CM0100_IN_RUN_ON_UI_THREAD = "Error updating CM0100 in runOnUiThread";
        public static final String USB_SERVICE_ERROR = "USB service error";
        public static final String ON_CREATE_ERROR = "onCreate error";
        public static final String APPLICATION_RESTART_FINISH_ERROR = "Application restart/finish error";
        public static final String THREAD_SLEEP_ERROR = "Thread sleep error";
        public static final String ERROR_IN_JSON_PARSING = "Error in jsonParsing";
        public static final String ERROR_CLOSING_READER = "Error closing reader";
        public static final String HTTP_CONNECTION_ERROR = "HTTP connection error";
        public static final String HTTP_REQUEST_ERROR = "HTTP request error";
        public static final String REQUEST_THREAD_ASYNC_ERROR = "RequestThreadAsync error";
        public static final String ERROR_PROCESSING_TEST_TASK_DATA = "Error processing test task data";
        public static final String REQUEST_TEST_TASK_THREAD_ASYNC_CONNECTION_ERROR = "RequestTestTaskThreadAsync connection error";
        public static final String REQUEST_TEST_TASK_THREAD_ASYNC_REQUEST_ERROR = "RequestTestTaskThreadAsync request error";
        public static final String REQUEST_TEST_TASK_THREAD_ASYNC_ERROR = "RequestTestTaskThreadAsync error";
        public static final String TEST_SPEC_DATA_PROCESSING_ERROR = "Test spec data processing error";
        public static final String ERROR_IN_UI_UPDATE = "Error in UI update";
        public static final String ERROR_IN_UI_UPDATE_JSON_PARSING = "Error in UI update (jsonParsing)";
        public static final String ERROR_IN_BACKGROUND_DB_OPERATION = "Error in background DB operation";
        public static final String JSON_PARSING_ERROR = "JSON parsing error";
        public static final String ERROR_DISCONNECTING_CONNECTION = "Error disconnecting connection";
        public static final String ERROR_CLEANING_UP_TIMERS = "Error cleaning up timers";
        public static final String SEND_BT_MESSAGE_ERROR = "sendBtMessage error";
        public static final String BLUETOOTH_SOCKET_CONNECTION_ERROR = "Bluetooth socket connection error";
        public static final String ERROR_CLOSING_SOCKET = "Error closing socket";
        public static final String ERROR_CLOSING_SOCKET_IN_LIST_PAIRED_DEVICES_SELECT = "Error closing socket in listPairedDevicesSelect";
        public static final String BLUETOOTH_DEVICE_CONNECTION_ERROR = "Bluetooth device connection error";
        public static final String LIST_PAIRED_DEVICES_SELECT_ERROR = "listPairedDevicesSelect error";
        public static final String COULD_NOT_CREATE_INSECURE_RFCOMM_CONNECTION = "Could not create Insecure RFComm Connection";
        public static final String ON_RESUME_USB_SERVICE_ERROR = "onResume USB service error";
        public static final String SET_FILTERS_ERROR = "setFilters error";
        public static final String USB_HANDLER_MESSAGE_PROCESSING_ERROR = "UsbHandler message processing error";
        public static final String READ_TEMPERATURE_EXCEL_ERROR = "readTemperatureExcel error";
        public static final String READ_TEMPERATURE_EXCEL_IO_EXCEPTION = "readTemperatureExcel IOException";
        public static final String READ_TEMPERATURE_EXCEL_BIFF_EXCEPTION = "readTemperatureExcel BiffException";
        public static final String ERROR_CLOSING_READER_IN_REQUEST_THREAD_BARCODE = "Error closing reader in RequestThreadBarcode";
        public static final String REQUEST_THREAD_BARCODE_ERROR = "RequestThreadBarcode error";
        public static final String PRODUCT_SERIAL_PARSING_ERROR = "Product serial parsing error";
        public static final String JSON_PARSING_BARCODE_JSON_EXCEPTION = "jsonParsingBarcode JSONException";
        public static final String BLUETOOTH_SEARCH_ERROR = "finding Bluetooth devices error";
        public static final String ERROR_IN_FINISH_APPLICATION_RECONNECT = "error occurred while bluetooth devices reconnecting in finishApplication";
    }
    
    // ==================== UI 메시지 ====================
    public static class UIMessages {
        public static final String SERVER_CONNECTION_FAILED = "서버 연결 실패";
        public static final String TURNED_OFF = "Turned off";
        public static final String PERMISSION_REQUEST_MESSAGE = "권한 요청에 동의 해주셔야 이용 가능합니다. 설정에서 권한 허용 하시기 바랍니다.";
        public static final String DIRECTORY_CREATED = " 디렉토리가 생성되었습니다.";
        public static final String ALREADY_ON = "Already on";
        public static final String USB_READY = "USB Ready";
        public static final String USB_PERMISSION_NOT_GRANTED = "USB Permission not granted";
        public static final String NO_USB_CONNECTED = "No USB connected";
        public static final String USB_DISCONNECTED = "USB disconnected";
        public static final String USB_DEVICE_NOT_SUPPORTED = "USB device not supported";
        public static final String BLUETOOTH_ENABLE_MESSAGE = "태블릿의 블루투스 연결을 활성화 해주십시요.";
        public static final String BLUETOOTH = "블루투스";
        public static final String CANCEL = "취소";
    }
    
    // ==================== URL 패턴 및 엔드포인트 ====================
    public static class URLs {
        public static final String HTTP_PROTOCOL = "http://";
        public static final String ENDPOINT_TEST_INFO_LIST = "/OVIO/TestInfoList.jsp";
        public static final String ENDPOINT_PRODUCT_SERIAL_INFO_LIST = "/OVIO/ProductSerialInfoList.jsp";
        public static final String ENDPOINT_UPDATE_RESULT_TEST_INFO = "/OVIO/UpdateResultTestInfo.jsp";
        public static final String ENDPOINT_TEST_TASK_INFO_UPDATE = "/OVIO/TestTaskInfoUpdate.jsp";
        public static final String ENDPOINT_VERSION_INFO_LIST = "/OVIO/VersionInfoList.jsp";
        public static final String ENDPOINT_SETTING_INFO_LIST = "/OVIO/SettingInfoList.jsp";
        public static final String ENDPOINT_APPLICATION_CLIENT_INFO_LIST = "/OVIO/ApplicationClientInfoList.jsp";
        public static final String PARAM_CALL_TYPE = "clm_call_type=S";
        public static final String PARAM_MODEL_ID = "clm_model_id" + Common.EQUAL;
        public static final String PARAM_UNIT_NO = "clm_unit_no" + Common.EQUAL;
        public static final String PARAM_PRODUCT_SERIAL_NO = "clm_product_serial_no" + Common.EQUAL;
        public static final String PARAM_TEST_PROCESS = "clm_test_process" + Common.EQUAL;
        public static final String PARAM_TEST_RESULT = "clm_test_result" + Common.EQUAL;
        public static final String PARAM_TEST_TASK_LOG = "clm_test_task_log" + Common.EQUAL;
        public static final String PARAM_TEST_UNIT_SEQ = "clm_test_unit_seq" + Common.EQUAL;
        public static final String PARAM_UNIT_IP = "clm_unit_ip" + Common.EQUAL;
        public static final String PARAM_TEST_PROCESS_ID = "clm_test_process_id" + Common.EQUAL;
        public static final String PARAM_WATT_LOG = "clm_watt_log" + Common.EQUAL;
    }
    
    // ==================== 서버 설정값 ====================
    public static class ServerConfig {
        public static final String SERVER_DOMAIN_192_168_0 = "192.168.0.";
        public static final String SERVER_IP_192_168_0_47 = "192.168.0.47";
        public static final String SERVER_DOMAIN_172_18_88 = "172.18.88.";
        public static final String SERVER_IP_172_18_88_31 = "172.18.88.31";
        public static final String SERVER_DOMAIN_172_16_1 = "172.16.1.";
        public static final String SERVER_IP_172_16_1_250_8080 = "172.16.1.250:8080";
        public static final String SERVER_IP_172_16_1_249_8080 = "172.16.1.249:8080";
        public static final String SERVER_IP_172_16_1_250 = "172.16.1.250";
        public static final String SERVER_IP_172_16_1_249 = "172.16.1.249";
        public static final String SERVER_IP = "172.16.1.250";
        public static final String SERVER_PORT = "8080";
        public static final String SERVER_PORT_8080 = "8080";
        public static final String SERVER_IP_ITFACTORY_DDNS = "itfactoryddns.iptime.org";
        public static final String SERVER_PORT_ITFACTORY_DDNS = "10004";
        public static final String SERVER_IP_PORT_ITF = SERVER_IP_ITFACTORY_DDNS + Common.COLON + SERVER_PORT_ITFACTORY_DDNS;
        public static final String SERVER_IP_PORT = SERVER_IP_172_16_1_249 + Common.COLON + SERVER_PORT_8080;
    }
    
    // ==================== 파일 경로 ====================
    public static class FilePaths {
        public static final String FOLDER_NAME = "/data/data/itf.com.app.simple_line_test_ovio_new/www";
        public static final String TEMPERATURE_INFO_XLS = "itf_temperature_info.xls";
    }
    
    // ==================== SharedPreferences 키 ====================
    public static class SharedPrefKeys {
        public static final String TEST_COOKIE_INFO = "test_cookie_info";
        public static final String TEST_INFO = "TestInfo";
        public static final String TEST_START_DATETIME = "test_start_datetime";
        public static final String TEST_PRODUCT_SERIAL_NO = "test_product_serial_no";
        public static final String TEST_RESULT = "test_result";
        public static final String HEATER_VALUE = "heater_value";
        public static final String COMP_VALUE = "comp_value";
        public static final String PRODUCT_SERIAL_NO = "product_serial_no";
    }
    
    // ==================== 테스트 아이템 코드 ====================
    public static class TestItemCodes {
        public static final String ST0101 = "ST0101";
        public static final String ST0201 = "ST0201";
        public static final String RE0101 = "RE0101";
        public static final String CM01 = "CM01";
        public static final String CM0101 = "CM0101";
        public static final String CM0100 = "CM0100";
        public static final String CM0102 = "CM0102";
        public static final String CM0103 = "CM0103";
        public static final String CM0200 = "CM0200";
        public static final String TH0101 = "TH0101";
        public static final String TH0201 = "TH0201";
        public static final String TH0301 = "TH0301";
        public static final String SV0101 = "SV0101";
        public static final String SV0100 = "SV0100";
        public static final String SV0201 = "SV0201";
        public static final String SV0200 = "SV0200";
        public static final String SV0301 = "SV0301";
        public static final String SV0300 = "SV0300";
        public static final String SV0401 = "SV0401";
        public static final String SV0400 = "SV0400";
        public static final String SN0101 = "SN0101";
        public static final String SN0201 = "SN0201";
        public static final String SN0301 = "SN0301";
        public static final String TA0101 = "TA0101";
        public static final String TA0201 = "TA0201";
        public static final String TA0301 = "TA0301";
        public static final String FM0101 = "FM0101";
        public static final String HT0100 = "HT0100";
        public static final String HT0101 = "HT0101";
        public static final String PM0101 = "PM0101";
        public static final String PM0100 = "PM0100";
        public static final String UV0201 = "UV0201";
        public static final String UV0200 = "UV0200";
        public static final String UV0101 = "UV0101";
        public static final String UV0100 = "UV0100";
    }
    
    // ==================== 결과/상태 값 ====================
    public static class ResultStatus {
        public static final String OK = "OK";
        public static final String NG = "NG";
        public static final String YES = "Y";
        public static final String NO = "N";
        public static final String MODE_TYPE_TEST = "T";
        public static final String MODE_TYPE_NORMAL = "N";
        public static final String ACCOUNT_TYPE_CLOUD = "C";
        public static final String ACCOUNT_TYPE_LOCAL = "L";
        public static final String REMOTE_COMMAND_CONDITION_S = "S";
        public static final String COMP_AGING_RESPONSE_01 = "01";
        public static final String COMP_AGING_RESPONSE_02 = "02";
    }
    
    // ==================== 데이터베이스 관련 ====================
    public static class Database {
        public static final String QUERY_AND = "and";
        public static final String QUERY_AND_1_EQUALS_1 = "and 1=1";
        public static final String QUERY_AND_CLM_TEST_COMMAND_EQUALS = "and clm_test_command='";
        public static final String TEMPERATURE_TYPE_HOT = "1";
        public static final String TEMPERATURE_TYPE_COLD = "2";
        public static final String LINE_ID = "lineId";
        public static final String UNIT_ID = "unitId";
    }
    
    // ==================== JSON 키 ====================
    public static class JsonKeys {
        public static final String TEST_SPEC = "test_spec";
        public static final String PRODUCT_SERIAL = "product_serial";
        public static final String CLM_TEST_COMMAND = "clm_test_command";
        public static final String CLM_TEST_NAME = "clm_test_name";
        public static final String CLM_TEST_RESPONSE_VALUE = "clm_test_response_value";
        public static final String CLM_MODEL_ID = "clm_model_id";
        public static final String CLM_MODEL_NAME = "clm_model_name";
        public static final String CLM_MODEL_NATION = "clm_model_nation";
        public static final String CLM_TEST_VERSION_ID = "clm_test_version_id";
        public static final String CLM_MODEL_VERSION = "clm_model_version";
        public static final String CLM_VALUE = "clm_value";
        public static final String CLM_TEST_LOWER_VALUE = "clm_test_lower_value";
        public static final String CLM_TEST_UPPER_VALUE = "clm_test_upper_value";
        public static final String CLM_12_BIT = "clm_12_bit";
        public static final String CLM_TEMPERATURE = "clm_temperature";
        public static final String CLM_LINE = "clm_line";
        public static final String CLM_UNIT_ID = "clm_unit_id";
        public static final String CLM_TIMESTAMP = "clm_timestamp";
        public static final String CLM_PRODUCT_TEMPERATURE = "clm_product_temperature";
        public static final String CLM_PRODUCT_SERIAL_NO = "clm_product_serial_no";
        public static final String CLM_TEST_RESULT = "clm_test_result";
        public static final String CLM_TEST_NG_COUNT = "clm_test_ng_count";
        public static final String CLM_TEST_OK_COUNT = "clm_test_ok_count";
        public static final String CLM_TEST_HISTORY_SEQ = "clm_test_history_seq";
        public static final String CLM_TEST_MODEL_ID = "clm_test_model_id";
        public static final String CLM_TEST_MODEL_NAME = "clm_test_model_name";
        public static final String CLM_TEST_MODEL_NATIONALITY = "clm_test_model_nationality";
        public static final String CLM_TEST_TIMESTAMP = "clm_test_timestamp";
        public static final String CLM_TEST_COMP_VALUE = "clm_test_comp_value";
        public static final String CLM_TEST_HEATER_VALUE = "clm_test_heater_value";
        public static final String CLM_COMMENT = "clm_comment";
        public static final String CLM_TEST_SEQ = "clm_test_seq";
        public static final String CLM_LOWER_VALUE = "clm_lower_value";
        public static final String CLM_UPPER_VALUE = "clm_upper_value";
        public static final String CLM_LOWER_VALUE_WATT = "clm_lower_value_watt";
        public static final String CLM_UPPER_VALUE_WATT = "clm_upper_value_watt";
        public static final String CLM_TEST_STEP = "clm_test_step";
        public static final String CLM_TEST_ID = "clm_test_id";
        public static final String CLM_RESPONSE_VALUE = "clm_response_value";
        public static final String CLM_TEST_SEC = "clm_test_sec";
        public static final String CLM_TEST_TYPE = "clm_test_type";
        public static final String CLM_VALUE_WATT = "clm_value_watt";
        public static final String CLM_TEST_LOWER_VALUE_02 = "clm_test_lower_value_02";
        public static final String CLM_TEST_UPPER_VALUE_02 = "clm_test_upper_value_02";
        public static final String SETTING_INFO = "setting_info";
        public static final String APPLICATION_CLIENT_INFO = "application_client_info";
        public static final String CLM_CLIENT_ID = "clm_client_id";
        public static final String CLM_SETTING_ID = "clm_setting_id";
        public static final String CLM_SETTING_VALUE = "clm_setting_value";
        public static final String CLM_SETTING_SEQ = "clm_setting_seq";
        public static final String CLM_BUSINESS_SERIAL_NO = "clm_business_serial_no";
    }
    
    // ==================== 날짜/시간 포맷 ====================
    public static class DateTimeFormats {
        public static final String DATETIME_FORMAT = "yyyy/MM/dd HH:mm:ss";
        public static final String DATETIME_FORMAT_BY_MIN = "yyyy/MM/dd HH:mm";
        public static final String TIMESTAMP_FORMAT = "yyyyMMddHHmmssSSS";
    }
    
    // ==================== 기타 상수값 ====================
    public static class Common {
        public static final String NULL = "null";
        public static final String ZERO = "0";
        public static final String DEGREE_CELSIUS = "°C";
        public static final String WATT_UNIT = "W";
        public static String UNIT_ID_PREFIX = "";
        public static final String TEST_MODEL_ID = "test_model_id";
        public static final String TEST_MODEL_NAME = "test_model_name";
        public static final String TEST_MODEL_NATION = "test_model_nation";
        public static final String TEST_ITEM_COMMAND = "test_item_command";
        public static final String TEST_RESPONSE_VALUE = "test_response_value";
        public static final String TEST_ITEM_RESULT = "test_item_result";
        public static final String TEST_FINISH_YN = "test_finish_yn";
        // public static final String TEST_MODEL_ID_KEY = "test_model_id";
        public static final String TEST_ITEM_SEQ = "test_item_seq";
        public static final String TEST_ITEM_NAME = "test_item_name";
        public static final String EMPTY_STRING = "";
        public static final String SPACE = " ";
        public static final String COMMA = ",";
        public static final String SINGLE_QUETATION = "'";
        public static final String DOUBLE_QUETATION = "\"";
        public static final String COLON = ":";
        public static final String EQUAL = "=";
        public static final String QUESTION = "?";
        public static final String AMPERSAND = "&";
        public static final String SLASH = "/";
        public static final String UNDER_SCORE = "_";
        public static final String LOG_PREFIX = "▶ [";
        public static final String LOG_SEPARATOR = "] ";
        public static final String LOG_FORMAT = "▶ [%s] %s";
        public static final String LOG_BRACKET_OPEN = "[";
        public static final String LOG_BRACKET_CLOSE = "]";
        public static final String LOG_PARENTHESIS_OPEN = "(";
        public static final String LOG_PARENTHESIS_CLOSE = ")";
        public static final String LOG_SEPARATOR_SLASH = " / ";
        public static final String LOG_BIGGER_THAN = " > ";
        public static final String LOG_LESS_THAN = " < ";
        public static final String NUMBER_FORMAT_03D = "%03d";
        public static final String HTTP_METHOD_GET = "GET";
        public static final String HTTP_METHOD_POST = "POST";
        public static final String CONTENT_TYPE_JSON = "application/json";
        public static final String CACHE_CONTROL_NO_CACHE = "no-cache";
        public static final String CONTENT_TYPE_HEADER = "Content-Type";
        public static final String CACHE_CONTROL_HEADER = "Cache-Control";
        public static final String TABLE_COLD_TEMPERATURE = "tbl_cold_temperature";
        public static final String TABLE_HOT_TEMPERATURE = "tbl_hot_temperature";
        public static final String CLM_TEMP_SEQ = "clm_temp_seq";
        public static final String CLM_REGIST = "clm_regist";
        public static final String CLM_VOLTAGE = "clm_voltage";
        public static final String CLM_10_BIT = "clm_10_bit";
        public static final String COMP_COOLING_PERFORMANCE = "COMP 냉각 성능 ";
        public static final String COMP_AC_COOLING_PERFORMANCE = "COMP(AC) 냉각 성능 ";
        public static final String SUCCESS = "성공";
        public static final String FAILURE = "실패";
        public static final String ARROW = " ▶ ";
        public static final String LOGGER_DEVIDER_01 = " / ";
        public static final String LOG_MESSGE_SEPARATOR = "--------------------------------------------------------------------------<";
    }
    
    // ==================== 로그 메시지 ====================
    public static class LogMessages {
        public static final String CONNECTION_STATE = "Connection state ";
        public static final String DIRECTORY_READY = "Directory ready: ";
        public static final String ACTIVITY_IS_NULL_OR_FINISHIG_CANNOT_PROCESS_TEST_SPEC_DATA = "Activity is null or finishing, cannot process test spec data";
        public static final String CTS_CHANGE = "CTS_CHANGE";
        public static final String DSR_CHANGE = "DSR_CHANGE";
        public static final String ON_DESTROY = "onDestroy";
        public static final String ON_CREATE_ENTRANCE_CHECK = "onCreate entranceCheck:";
        public static final String INSERT_TEMP_INFO = "insert temp info";
        public static final String SETTING_SERVER_URL = "setting server url: ";
        public static final String GET_WEB_IP_INFO = "get web IP info: ";
        public static final String WEBSERVER_STARTED = "webserver started";
        public static final String TEST_RESPONSE_SIGNAL_RECEIVED = "검사 응답 신호 수신";
        public static final String MEASURED_TEMPERATURE = "측정 온도: ";
        public static final String PRODUCT_SERIAL_INFO_CALL = "제품 시리얼 번호 정보 호출[%d]: %s";
        public static final String PRODUCT_SERIAL_INFO_CALL_SUCCESS = "제품 시리얼 번호 정보 호출 성공 (code: %d)";
        public static final String PRODUCT_SERIAL_INFO_CALL_FAILED = "제품 시리얼 번호 정보 호출 실패 (code: %d)";
        public static final String PRODUCT_SERIAL_INFO_CALL_DATA_RECEIVED = "제품 시리얼 번호 정보 호출 data received";
        public static final String READ_SETTING_INFO = "read setting info";
        public static final String USB_SERVICE_STARTED = "USB Service started";
        public static final String USB_SERVICE_INITIALIZATION = "USB Service initialization";
        public static final String USB_SERVICE_NOT_CONNECTED = "USB 서비스 체결 안됨";
        public static final String USB_SERVICE_CONNECTED = "USB 서비스 연결됨. 자동 연결 메시지 전송 준비";
        public static final String USB_PERMISSION_ALREADY_GRANTED = "USB 권한이 이미 부여되어 있음. 자동 연결 메시지 전송 시작";
        public static final String USB_PERMISSION_WAITING = "USB 권한 대기 중. 권한 부여 후 자동 연결 시작됨";
        public static final String USB_AUTO_CONNECT_TIMER_STARTED = "USB 자동 연결 Timer 시작됨 (500ms 간격)";
        public static final String USB_CONNECTION_TIMER_ALREADY_RUNNING = "USB 연결 Timer가 이미 실행 중입니다.";
        public static final String USB_CONNECTION_MESSAGE_SENT = "USB 연결 메시지 전송: ";
        public static final String BLUETOOTH_DEVICE_CONNECTION_SUCCESS = "블루투스 디바이스 연결 성공";
        public static final String BLUETOOTH_DEVICE_CONNECTION_FAILED = "블루투스 디바이스 연결 실패";
        public static final String PAIRED_BLUETOOTH_DEVICES = "페어링된 블루투스 장비";
        public static final String CONNECTED_BLUETOOTH_DEVICE = "연결된 블루투스 장비";
        public static final String NOT_CONNECTED_BLUETOOTH_DEVICE = "연결되지 않은 블루투스 장비";
        public static final String BLUETOOTH_CONNECTION_SUCCESSFUL = "Bluetooth connection successful!";
        public static final String BLUETOOTH_CONNECTION_FAILED = "Bluetooth connection failed!";
        public static final String CONNECTION_ATTEMPT_TIMED_OUT = "Connection attempt timed out and was canceled.";
        public static final String TEST_ITEM_SIGNAL_SENT = "검사 항목 신호 송신";
        public static final String TEST_ITEM_SIGNAL_RECEIVE = "검사 항목 신호 수신";
        public static final String NO_RECEIVED_DATA = "수신 없음";
        public static final String PUMP_POWER_CONSUMPTION = "펌프 소비 전력";
        public static final String ALL_TIMERS_CLEANED_UP = "All timers cleaned up successfully";
        public static final String PROCESSING_HTTP_RESPONSE_DATA = "Processing HTTP response data";
        public static final String DELETING_TEST_SPEC_DATA = "Deleting test spec data";
        public static final String TEST_SPEC_DATA_DELETED = "Test spec data deleted";
        public static final String TEST_TASK_RESPONSE_DATA_RECEIVED = "Test task response data received";
        public static final String JSON_PARSING_BARCODE_RECEIVED = "jsonParsingBarcode - globalProductSerialNo JSON received";
        public static final String GLOBAL_PRODUCT_SERIAL_NO = "globalProductSerialNo:";
        public static final String PRODUCT_SERIAL_NO_INFO = "productSerialNo:";
        public static final String TEST_PROCESS_ID_STARTED = "testProcessId started: ";
        public static final String TEST_PROCESS_ID_FINISHED = "testProcessId finished: ";
        public static final String TEST_START_DATETIME_LMS_TEST_SEQ = "test_start_datetime.lmsTestSeq: ";
        public static final String REQUEST_TIME = "요청시간: ";
        public static final String CURRENT_TIME = "현재시간: ";
        public static final String MINUTE_DIFFERENCE = "분 차이";
        public static final String SERVER_NOT_CONNECTED_BLUETOOTH_CONNECTION_ATTEMPT = "서버 미연결 후 블루투스 연결 시도";
        public static final String SERVER_CONNECTED_BLUETOOTH_CONNECTION_ATTEMPT = "서버 연결 후 블루투스 연결 시도";
        public static final String BT_LIST_TIMER_TASK_EXECUTED = "btListTimerTask executed";
        public static final String USB_INTENT_ACTION = "USB intent action: ";
        public static final String USB_CONNECTION_STATE = "USB Connection state: ";
        public static final String USB_SERVICE_CONNECTED_SUCCESSFULLY = "USB Service connected successfully";
        public static final String USB_SERVICE_DISCONNECTED = "USB Service disconnected";
        public static final String HTTP_RESPONSE_CODE = "HTTP response code: ";
        public static final String HTTP_OK_SUCCESS = "HTTP OK 성공";
        public static final String HTTP_OK_FAILED = "HTTP OK 안됨";
        public static final String TARGET_URL = "targetUrl: ";
        public static final String MEMORY_CACHE_COMMAND_FOUND = "메모리 캐시에서 명령 찾음: ";
        public static final String DB_DATA_QUERY_SUCCESS = "DB에서 데이터 조회 성공: ";
        public static final String MEMORY_CACHE_DATA_USED = "메모리 캐시에서 데이터 사용 (DB 조회 실패): ";
        public static final String LST_SPEC_DATA_NULL_OR_EMPTY = "lstSpecData is null or empty for command: ";
        public static final String DB_AND_MEMORY_CACHE_BOTH_CHECKED = " (DB 및 메모리 캐시 모두 확인됨)";
        public static final String CM0101_INDEX_NOT_FOUND = "CM0101 인덱스를 찾을 수 없거나 범위를 벗어남: ";
        public static final String TEST_RESULT_FORMAT = "Test result [%d]: %s";
        public static final String TEST_SPEC_DATA_FORMAT = "Test spec data [%d]: %s";
        public static final String SCENARIO_SETTING_INFO_FORMAT = "scinario setting info[%d]: %s / test_name:%s / test_type:%s / response:%s / value:%s / lower_value:%s / upper_value:%s";
        public static final String TEST_DATA_INSERT_FORMAT = "TestData.insertTestSpecData[%d]: %s";
        public static final String PRODUCT_SERIAL_INFO_CALL_FORMAT = "제품 시리얼 번호 정보 호출: %s (code: %d, OK: %s)";
        public static final String UNFINISHED_RESTART_APP_TERMINATION = "tmrUnfinishedRestart 검사 어플리케이션 종료 실패 재시작 [%d / %d]";
        public static final String FINISHED_RESTART_APP_TERMINATION = "tmrFinishedRestart 검사 어플리케이션 종료";
        public static final String SEND_BT_MESSAGE_CM0200_TO_CM0100 = "sendBtMessage CM0200 -> CM0100: ";
    }
    
    // ==================== Intent Extra 키 ====================
    public static class IntentExtras {
        public static final String TEST_MODEL_ID = "test_model_id";
        public static final String TEST_MODEL_NAME = "test_model_name";
        public static final String TEST_MODEL_NATION = "test_model_nation";
        public static final String TEST_START_DATETIME = "test_start_datetime";
    }
    
    // ==================== HTTP 관련 ====================
    public static class HTTP {
        public static final String METHOD_GET = "GET";
        public static final String METHOD_POST = "POST";
        public static final String CONTENT_TYPE_JSON = "application/json";
        public static final String CACHE_CONTROL_NO_CACHE = "no-cache";
        public static final String HEADER_CONTENT_TYPE = "Content-Type";
        public static final String HEADER_CACHE_CONTROL = "Cache-Control";
    }
    
    // ==================== PLC 명령 ====================
    public static class PLCCommands {
        public static final String RSS0107_DW1006 = "\u000500RSS0107%DW1006\u0004";
        public static final String RSS_RESPONSE_HEADER = "00RSS0102";
    }
    
    // ==================== 문자 코드 ====================
    public static class CharCodes {
        public static final String STX = "\u0002";
        public static final String ETX = "\u0003";
        public static final String ACK = "\u0006";
    }
    
    // ==================== 테스트 모델 배열 ====================
    public static class TestModels {
        public static final String[][] ARR_TEST_MODELS = {{"", "", "", "", ""}};
    }
    
    // ==================== 블루투스 메시지 상수 ====================
    public static class Bluetooth {
        public static final int MESSAGE_READ = 2;
        public static final int CONNECTING_STATUS = 3;
        public static final int REQUEST_ENABLE_BT = 1;
    }
    
    // ==================== 초기값 ====================
    public static class InitialValues {
        public static final String BT_MODULE_UUID_SERIAL_PORT_SERVICE = "00001101-0000-1000-8000-00805F9B34FB";
        public static final String BT_CONNECTED = "isConnected";
        public static final String DEVICE_READY_CHARACTER = "R";
        public static final String LOG_LEVEL_VERBOSE = "V";
        public static final String LOG_LEVEL_DEBUG = "D";
        public static final String LOG_LEVEL_INFO = "I";
        public static final String LOG_LEVEL_WARN = "W";
        public static final String LOG_LEVEL_ERROR = "E";
        public static final String CURRENT_TEST_ITEM = "ST0101";
        public static final int WS_PORT = 8080;
        // public static final String ACCOUNT_TYPE = ResultStatus.ACCOUNT_TYPE_LOCAL;
        public static final String ACCOUNT_TYPE = ResultStatus.ACCOUNT_TYPE_CLOUD;
        public static final String MODE_TYPE = ResultStatus.MODE_TYPE_TEST;
        // public static final String MODE_TYPE = ResultStatus.MODE_TYPE_NORMAL;
        public static final String SEND_RESULT_YN = "N";
        public static final String RECEIVE_RESPONSE_RESULT = "NG";
        public static final String REMOTE_COMMAND_CALL_CONDITION = "S";
        public static final String RECEIVE_COMP_AGING_RESPONSE = "01";
        public static final String TEST_PROCESS_ID = "";
        public static final String TEMPERATURE_TMP = "0";
        public static final String VALUE_WATT = "0";
        public static final String LOWER_VALUE_WATT = "0";
        public static final String UPPER_VALUE_WATT = "0";
        public static final String NUMBER_FORMMATING_03D = "%03d";
        public static final int DATABASE_VERSION = 6;
        public static String USB_RECEIVED_DATA = "";
        public static String USB_RECEIVED_DATA_TMP = "";
    }
    
    // ==================== 타임아웃 및 지연 시간 ====================
    public static class Timeouts {
        // HTTP 타임아웃
        public static final int HTTP_CONNECT_TIMEOUT_MS = 10000; // 10초
        public static final int HTTP_READ_TIMEOUT_MS = 10000; // 10초
        public static final int HTTP_CONNECT_TIMEOUT_LONG_MS = 20000; // 20초
        public static final int HTTP_READ_TIMEOUT_LONG_MS = 20000; // 20초
        
        // Timer 간격
        public static final long TIMER_INTERVAL_MS = 1000; // 1초
        public static final long USB_TIMER_INTERVAL_MS = 500; // 0.5초
        public static final long BT_LIST_TIMER_INTERVAL_MS = 1500; // 2.5초
        
        // Thread 및 기타 지연 시간
        public static final int THREAD_SLEEP_MS = 1000; // 1초
        public static final long CONNECTION_TIMEOUT = 2000; // 2초 타임아웃
        
        // 최대 읽기 횟수
        public static final int MAX_READ_COUNT = 10000;

        public static final int MINUTE_TO_MULTIPLE = 1; // 1분을 밀리초로 변환
    }
    
    // ==================== 테스트 카운터 임계값 ====================
    public static class TestThresholds {
        public static final int TEST_COUNTER_THRESHOLD_1 = 1;
        public static final int TEST_COUNTER_THRESHOLD_5 = 5;
        public static final int TEST_COUNTER_THRESHOLD_10 = 10;
        public static final int TEST_COUNTER_THRESHOLD_30 = 30;
        public static final int DISCONNECT_CHECK_THRESHOLD = 10;
    }
    
    // ==================== 네트워크 관련 상수 ====================
    public static class Network {
        public static final int IP_ADDRESS_OFFSET = 100; // IP 주소 오프셋
        public static final int RESTART_CNT_MARGIN_MULTIPLIER = 60; // 재시작 카운트 마진 배수
        public static final int HTTP_RETRY_COUNT = 3; // HTTP 요청 재시도 횟수
        public static final int HTTP_RETRY_DELAY_MS = 1000; // HTTP 재시도 간 지연 시간 (밀리초)
    }
    
    // ==================== UI 관련 상수 ====================
    public static class UI {
        public static final float DISPLAY_BRIGHTNESS = 0.5f; // 화면 밝기
        public static final int TEXT_SIZE_LARGE_SP = 150; // 큰 텍스트 크기
        public static final int TEXT_SIZE_MEDIUM_SP = 60; // 중간 텍스트 크기
    }
}


