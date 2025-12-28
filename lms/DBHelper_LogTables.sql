-- ==================== 로그 메시지 테이블 생성 ====================

-- tbl_log_messages 테이블 생성
CREATE TABLE IF NOT EXISTS tbl_log_messages (
    clm_log_id INTEGER PRIMARY KEY AUTOINCREMENT,
    clm_log_type TEXT NOT NULL,              -- ERROR, WARNING, INFO, DEBUG, VERBOSE
    clm_error_id TEXT,                        -- 에러 로그인 경우 고유 ID (예: ERR-001, ERR-BT-001)
    clm_category TEXT NOT NULL,               -- PS, BT, US, SI, ER, CA, TH, BI, RS
    clm_tag TEXT NOT NULL,                    -- 로그 태그 (클래스명 등)
    clm_message TEXT NOT NULL,                -- 로그 메시지
    clm_stack_trace TEXT,                     -- 스택 트레이스 (에러인 경우)
    clm_timestamp TEXT NOT NULL,              -- 로그 발생 시간 (yyyy-MM-dd HH:mm:ss.SSS)
    clm_thread_name TEXT,                     -- 스레드 이름
    clm_user_id TEXT,                         -- 사용자 ID (선택적)
    clm_device_info TEXT,                     -- 기기 정보 (선택적)
    clm_extra_data TEXT,                      -- 추가 데이터 (JSON 형식, 선택적)
    clm_is_synced INTEGER DEFAULT 0,          -- 서버 동기화 여부 (0: 미동기화, 1: 동기화)
    clm_synced_timestamp TEXT                 -- 서버 동기화 시간
);

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_log_type ON tbl_log_messages(clm_log_type);
CREATE INDEX IF NOT EXISTS idx_error_id ON tbl_log_messages(clm_error_id);
CREATE INDEX IF NOT EXISTS idx_category ON tbl_log_messages(clm_category);
CREATE INDEX IF NOT EXISTS idx_timestamp ON tbl_log_messages(clm_timestamp);
CREATE INDEX IF NOT EXISTS idx_synced ON tbl_log_messages(clm_is_synced);

-- ==================== 에러 카탈로그 테이블 생성 ====================

-- tbl_error_catalog 테이블 생성
CREATE TABLE IF NOT EXISTS tbl_error_catalog (
    clm_error_id TEXT PRIMARY KEY,            -- 에러 ID (예: ERR-001, ERR-BT-001)
    clm_error_category TEXT NOT NULL,         -- 에러 카테고리 (BT, ER, US 등)
    clm_error_code TEXT NOT NULL,             -- 에러 코드 (001, 002 등)
    clm_error_name_ko TEXT,                    -- 에러 이름 (한국어)
    clm_error_name_en TEXT,                    -- 에러 이름 (영어)
    clm_error_description_ko TEXT,             -- 에러 설명 (한국어)
    clm_error_description_en TEXT,             -- 에러 설명 (영어)
    clm_severity TEXT NOT NULL,                -- 심각도 (CRITICAL, HIGH, MEDIUM, LOW)
    clm_solution_ko TEXT,                      -- 해결 방법 (한국어)
    clm_solution_en TEXT,                      -- 해결 방법 (영어)
    clm_created_timestamp TEXT NOT NULL,       -- 생성 시간
    clm_updated_timestamp TEXT                 -- 수정 시간
);

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_error_category ON tbl_error_catalog(clm_error_category);
CREATE INDEX IF NOT EXISTS idx_error_severity ON tbl_error_catalog(clm_severity);



