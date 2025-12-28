package itf.com.app.lms.util;

/**
 * 에러 로그 ID 매핑 클래스
 * 
 * 기능:
 * - 에러 로그에 고유 ID 부여
 * - 에러 메시지 패턴으로부터 에러 ID 추론
 * - 에러 카테고리별 ID 생성
 */
public class ErrorIdMapper {
    
    // ==================== 블루투스 관련 에러 ====================
    public static final String ERR_BT_001 = "ERR-BT-001"; // 블루투스 연결 실패
    public static final String ERR_BT_002 = "ERR-BT-002"; // 블루투스 권한 거부
    public static final String ERR_BT_003 = "ERR-BT-003"; // 블루투스 장치 이름 파싱 실패
    public static final String ERR_BT_004 = "ERR-BT-004"; // 블루투스 장치 발견 실패
    public static final String ERR_BT_005 = "ERR-BT-005"; // 블루투스 통신 실패
    
    // ==================== USB 관련 에러 ====================
    public static final String ERR_US_001 = "ERR-US-001"; // USB 연결 실패
    public static final String ERR_US_002 = "ERR-US-002"; // USB 권한 거부
    public static final String ERR_US_003 = "ERR-US-003"; // USB 장치 미지원
    public static final String ERR_US_004 = "ERR-US-004"; // USB 통신 실패
    public static final String ERR_US_005 = "ERR-US-005"; // USB 서비스 시작 실패
    
    // ==================== 서버 관련 에러 ====================
    public static final String ERR_SI_001 = "ERR-SI-001"; // 서버 연결 실패
    public static final String ERR_SI_002 = "ERR-SI-002"; // HTTP 요청 실패
    public static final String ERR_SI_003 = "ERR-SI-003"; // JSON 파싱 실패
    public static final String ERR_SI_004 = "ERR-SI-004"; // 서버 응답 오류
    public static final String ERR_SI_005 = "ERR-SI-005"; // 네트워크 타임아웃
    
    // ==================== 프로세스 관련 에러 ====================
    public static final String ERR_PS_001 = "ERR-PS-001"; // 프로세스 초기화 실패
    public static final String ERR_PS_002 = "ERR-PS-002"; // 프로세스 실행 실패
    public static final String ERR_PS_003 = "ERR-PS-003"; // 프로세스 종료 실패
    public static final String ERR_PS_004 = "ERR-PS-004"; // UI 업데이트 실패
    
    // ==================== 데이터베이스 관련 에러 ====================
    public static final String ERR_DB_001 = "ERR-DB-001"; // 데이터베이스 연결 실패
    public static final String ERR_DB_002 = "ERR-DB-002"; // 데이터베이스 쿼리 실패
    public static final String ERR_DB_003 = "ERR-DB-003"; // 데이터베이스 트랜잭션 실패
    
    // ==================== 일반 에러 ====================
    public static final String ERR_ER_001 = "ERR-ER-001"; // 일반 예외 발생
    public static final String ERR_ER_002 = "ERR-ER-002"; // 메모리 부족
    public static final String ERR_ER_003 = "ERR-ER-003"; // 파일 입출력 오류
    public static final String ERR_ER_004 = "ERR-ER-004"; // 권한 오류
    public static final String ERR_ER_005 = "ERR-ER-005"; // 알 수 없는 오류
    
    // ==================== 캐시 관련 에러 ====================
    public static final String ERR_CA_001 = "ERR-CA-001"; // 캐시 저장 실패
    public static final String ERR_CA_002 = "ERR-CA-002"; // 캐시 조회 실패
    
    // ==================== 온도 관련 에러 ====================
    public static final String ERR_TH_001 = "ERR-TH-001"; // 온도 측정 실패
    public static final String ERR_TH_002 = "ERR-TH-002"; // 온도 처리 오류
    
    // ==================== 바코드 관련 에러 ====================
    public static final String ERR_BI_001 = "ERR-BI-001"; // 바코드 스캔 실패
    public static final String ERR_BI_002 = "ERR-BI-002"; // 바코드 파싱 실패
    
    // ==================== 결과 관련 에러 ====================
    public static final String ERR_RS_001 = "ERR-RS-001"; // 결과 저장 실패
    public static final String ERR_RS_002 = "ERR-RS-002"; // 결과 조회 실패
    
    /**
     * 에러 ID를 카테고리로부터 자동 생성
     * 
     * @param category 로그 카테고리
     * @param errorNumber 에러 번호 (001, 002, ...)
     * @return 에러 ID (예: ERR-BT-001)
     */
    public static String generateErrorId(LogManager.LogCategory category, int errorNumber) {
        return String.format("ERR-%s-%03d", category.getCode(), errorNumber);
    }
    
    /**
     * 메시지 패턴으로부터 에러 ID를 추론
     * 
     * @param category 로그 카테고리
     * @param message 로그 메시지
     * @param throwable 예외 객체
     * @return 추론된 에러 ID
     */
    public static String inferErrorId(LogManager.LogCategory category, String message, Throwable throwable) {
        if (message == null) {
            message = "";
        }
        String lowerMessage = message.toLowerCase();
        String throwableMessage = throwable != null && throwable.getMessage() != null 
            ? throwable.getMessage().toLowerCase() 
            : "";
        String combinedMessage = lowerMessage + " " + throwableMessage;
        
        // 블루투스 관련
        if (category == LogManager.LogCategory.BT) {
            if (combinedMessage.contains("연결 실패") || combinedMessage.contains("connection failed") 
                || combinedMessage.contains("connect failed") || combinedMessage.contains("연결할 수 없")) {
                return ERR_BT_001;
            }
            if (combinedMessage.contains("권한") || combinedMessage.contains("permission") 
                || combinedMessage.contains("권한 거부") || combinedMessage.contains("permission denied")) {
                return ERR_BT_002;
            }
            if (combinedMessage.contains("파싱") || combinedMessage.contains("parsing") 
                || combinedMessage.contains("parse") || combinedMessage.contains("이름 파싱")) {
                return ERR_BT_003;
            }
            if (combinedMessage.contains("발견") || combinedMessage.contains("discover") 
                || combinedMessage.contains("device not found")) {
                return ERR_BT_004;
            }
            if (combinedMessage.contains("통신") || combinedMessage.contains("communication") 
                || combinedMessage.contains("read failed") || combinedMessage.contains("write failed")) {
                return ERR_BT_005;
            }
        }
        
        // USB 관련
        if (category == LogManager.LogCategory.US) {
            if (combinedMessage.contains("연결 실패") || combinedMessage.contains("connection failed") 
                || combinedMessage.contains("connect failed")) {
                return ERR_US_001;
            }
            if (combinedMessage.contains("권한") || combinedMessage.contains("permission") 
                || combinedMessage.contains("권한 거부") || combinedMessage.contains("permission denied")) {
                return ERR_US_002;
            }
            if (combinedMessage.contains("미지원") || combinedMessage.contains("not supported") 
                || combinedMessage.contains("unsupported")) {
                return ERR_US_003;
            }
            if (combinedMessage.contains("통신") || combinedMessage.contains("communication") 
                || combinedMessage.contains("read failed") || combinedMessage.contains("write failed")) {
                return ERR_US_004;
            }
            if (combinedMessage.contains("서비스 시작") || combinedMessage.contains("service start") 
                || combinedMessage.contains("start failed")) {
                return ERR_US_005;
            }
        }
        
        // 서버 관련
        if (category == LogManager.LogCategory.SI) {
            if (combinedMessage.contains("연결 실패") || combinedMessage.contains("connection failed") 
                || combinedMessage.contains("connect failed") || combinedMessage.contains("서버 연결")) {
                return ERR_SI_001;
            }
            if (combinedMessage.contains("http") || combinedMessage.contains("요청") 
                || combinedMessage.contains("request failed") || combinedMessage.contains("http error")) {
                return ERR_SI_002;
            }
            if (combinedMessage.contains("json") || combinedMessage.contains("파싱") 
                || combinedMessage.contains("parse") || combinedMessage.contains("parsing")) {
                return ERR_SI_003;
            }
            if (combinedMessage.contains("응답") || combinedMessage.contains("response") 
                || combinedMessage.contains("server error")) {
                return ERR_SI_004;
            }
            if (combinedMessage.contains("타임아웃") || combinedMessage.contains("timeout") 
                || combinedMessage.contains("network")) {
                return ERR_SI_005;
            }
        }
        
        // 프로세스 관련
        if (category == LogManager.LogCategory.PS) {
            if (combinedMessage.contains("초기화") || combinedMessage.contains("initialization") 
                || combinedMessage.contains("init failed")) {
                return ERR_PS_001;
            }
            if (combinedMessage.contains("실행") || combinedMessage.contains("execution") 
                || combinedMessage.contains("execute failed")) {
                return ERR_PS_002;
            }
            if (combinedMessage.contains("종료") || combinedMessage.contains("termination") 
                || combinedMessage.contains("destroy")) {
                return ERR_PS_003;
            }
            if (combinedMessage.contains("ui 업데이트") || combinedMessage.contains("ui update") 
                || combinedMessage.contains("update failed")) {
                return ERR_PS_004;
            }
        }
        
        // 데이터베이스 관련
        if (combinedMessage.contains("database") || combinedMessage.contains("db") 
            || combinedMessage.contains("데이터베이스")) {
            if (combinedMessage.contains("연결") || combinedMessage.contains("connection")) {
                return ERR_DB_001;
            }
            if (combinedMessage.contains("쿼리") || combinedMessage.contains("query") 
                || combinedMessage.contains("sql")) {
                return ERR_DB_002;
            }
            if (combinedMessage.contains("트랜잭션") || combinedMessage.contains("transaction")) {
                return ERR_DB_003;
            }
        }
        
        // 캐시 관련
        if (category == LogManager.LogCategory.CA) {
            if (combinedMessage.contains("저장") || combinedMessage.contains("save") 
                || combinedMessage.contains("store")) {
                return ERR_CA_001;
            }
            if (combinedMessage.contains("조회") || combinedMessage.contains("get") 
                || combinedMessage.contains("retrieve")) {
                return ERR_CA_002;
            }
        }
        
        // 온도 관련
        if (category == LogManager.LogCategory.TH) {
            if (combinedMessage.contains("측정") || combinedMessage.contains("measure") 
                || combinedMessage.contains("temperature")) {
                return ERR_TH_001;
            }
            if (combinedMessage.contains("처리") || combinedMessage.contains("process") 
                || combinedMessage.contains("processing")) {
                return ERR_TH_002;
            }
        }
        
        // 바코드 관련
        if (category == LogManager.LogCategory.BI) {
            if (combinedMessage.contains("스캔") || combinedMessage.contains("scan") 
                || combinedMessage.contains("barcode")) {
                return ERR_BI_001;
            }
            if (combinedMessage.contains("파싱") || combinedMessage.contains("parse")) {
                return ERR_BI_002;
            }
        }
        
        // 결과 관련
        if (category == LogManager.LogCategory.RS) {
            if (combinedMessage.contains("저장") || combinedMessage.contains("save")) {
                return ERR_RS_001;
            }
            if (combinedMessage.contains("조회") || combinedMessage.contains("get") 
                || combinedMessage.contains("retrieve")) {
                return ERR_RS_002;
            }
        }
        
        // 일반 에러
        if (category == LogManager.LogCategory.ER) {
            if (combinedMessage.contains("메모리") || combinedMessage.contains("memory") 
                || combinedMessage.contains("outofmemory")) {
                return ERR_ER_002;
            }
            if (combinedMessage.contains("파일") || combinedMessage.contains("file") 
                || combinedMessage.contains("io")) {
                return ERR_ER_003;
            }
            if (combinedMessage.contains("권한") || combinedMessage.contains("permission")) {
                return ERR_ER_004;
            }
            return ERR_ER_001;
        }
        
        // 기본값: 카테고리 기반 일반 에러
        return generateErrorId(category, 1);
    }
}

