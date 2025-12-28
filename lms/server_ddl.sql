-- ============================================
-- OVIO 서버용 데이터베이스 DDL
-- 모델 정보 테이블 및 버전 관리 테이블
-- ============================================

-- 1. 모델 정보 테이블
CREATE TABLE IF NOT EXISTS tbl_model_info (
    clm_model_seq VARCHAR(50) PRIMARY KEY COMMENT '모델 시퀀스',
    clm_client_name VARCHAR(100) COMMENT '클라이언트 이름',
    clm_client_id VARCHAR(50) COMMENT '클라이언트 ID',
    clm_test_step VARCHAR(50) COMMENT '테스트 단계',
    clm_company_key VARCHAR(100) COMMENT '회사 키',
    clm_model_id VARCHAR(100) COMMENT '모델 ID',
    clm_model_name VARCHAR(200) COMMENT '모델 이름',
    clm_model_version VARCHAR(50) COMMENT '모델 버전',
    clm_comment TEXT COMMENT '비고',
    clm_regist_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
    clm_update_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',
    INDEX idx_model_id (clm_model_id),
    INDEX idx_client_id (clm_client_id),
    INDEX idx_update_timestamp (clm_update_timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='모델 정보 테이블';

-- 2. 모델 정보 버전 관리 테이블
CREATE TABLE IF NOT EXISTS tbl_model_info_version_history (
    clm_version_history_seq BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '버전 히스토리 시퀀스',
    clm_model_seq VARCHAR(50) NOT NULL COMMENT '모델 시퀀스',
    clm_client_name VARCHAR(100) COMMENT '클라이언트 이름',
    clm_client_id VARCHAR(50) COMMENT '클라이언트 ID',
    clm_test_step VARCHAR(50) COMMENT '테스트 단계',
    clm_company_key VARCHAR(100) COMMENT '회사 키',
    clm_model_id VARCHAR(100) COMMENT '모델 ID',
    clm_model_name VARCHAR(200) COMMENT '모델 이름',
    clm_model_version VARCHAR(50) COMMENT '모델 버전',
    clm_comment TEXT COMMENT '비고',
    clm_version_number INT NOT NULL COMMENT '버전 번호',
    clm_change_type VARCHAR(20) NOT NULL COMMENT '변경 유형 (INSERT, UPDATE, DELETE)',
    clm_change_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '변경 일시',
    INDEX idx_model_seq (clm_model_seq),
    INDEX idx_model_id (clm_model_id),
    INDEX idx_version_number (clm_version_number),
    INDEX idx_change_timestamp (clm_change_timestamp),
    FOREIGN KEY (clm_model_seq) REFERENCES tbl_model_info(clm_model_seq) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='모델 정보 버전 관리 테이블';

-- 3. 테스트 스펙 데이터 버전 관리 테이블
CREATE TABLE IF NOT EXISTS tbl_test_spec_data_version_history (
    clm_version_history_seq BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '버전 히스토리 시퀀스',
    clm_test_seq VARCHAR(50) NOT NULL COMMENT '테스트 시퀀스',
    clm_model_id VARCHAR(100) COMMENT '모델 ID',
    clm_model_expand_id VARCHAR(100) COMMENT '모델 확장 ID',
    clm_test_command VARCHAR(200) COMMENT '테스트 명령',
    clm_test_name VARCHAR(200) COMMENT '테스트 이름',
    clm_test_type VARCHAR(50) COMMENT '테스트 타입',
    clm_test_response_value VARCHAR(200) COMMENT '테스트 응답 값',
    clm_test_upper_value VARCHAR(100) COMMENT '테스트 상한 값',
    clm_test_lower_value VARCHAR(100) COMMENT '테스트 하한 값',
    clm_test_upper_value_02 VARCHAR(100) COMMENT '테스트 상한 값 02',
    clm_test_lower_value_02 VARCHAR(100) COMMENT '테스트 하한 값 02',
    clm_comment TEXT COMMENT '비고',
    clm_value_watt VARCHAR(100) COMMENT '와트 값',
    clm_value VARCHAR(100) COMMENT '값',
    clm_test_step VARCHAR(50) COMMENT '테스트 단계',
    clm_test_id VARCHAR(100) COMMENT '테스트 ID',
    clm_test_sec VARCHAR(50) COMMENT '테스트 초',
    clm_product_serial_no VARCHAR(100) COMMENT '제품 시리얼 번호',
    clm_lower_value_watt VARCHAR(100) COMMENT '하한 와트 값',
    clm_upper_value_watt VARCHAR(100) COMMENT '상한 와트 값',
    clm_test_version_id VARCHAR(100) COMMENT '테스트 버전 ID',
    clm_version_number INT NOT NULL COMMENT '버전 번호',
    clm_change_type VARCHAR(20) NOT NULL COMMENT '변경 유형 (INSERT, UPDATE, DELETE)',
    clm_change_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '변경 일시',
    INDEX idx_test_seq (clm_test_seq),
    INDEX idx_model_id (clm_model_id),
    INDEX idx_version_number (clm_version_number),
    INDEX idx_change_timestamp (clm_change_timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='테스트 스펙 데이터 버전 관리 테이블';

-- 참고: tbl_test_spec_data 테이블이 이미 존재한다고 가정
-- 만약 존재하지 않는다면 아래 DDL을 사용하여 생성할 수 있습니다.
/*
CREATE TABLE IF NOT EXISTS tbl_test_spec_data (
    clm_test_seq VARCHAR(50) PRIMARY KEY COMMENT '테스트 시퀀스',
    clm_model_id VARCHAR(100) COMMENT '모델 ID',
    clm_model_expand_id VARCHAR(100) COMMENT '모델 확장 ID',
    clm_test_command VARCHAR(200) COMMENT '테스트 명령',
    clm_test_name VARCHAR(200) COMMENT '테스트 이름',
    clm_test_type VARCHAR(50) COMMENT '테스트 타입',
    clm_test_response_value VARCHAR(200) COMMENT '테스트 응답 값',
    clm_test_upper_value VARCHAR(100) COMMENT '테스트 상한 값',
    clm_test_lower_value VARCHAR(100) COMMENT '테스트 하한 값',
    clm_test_upper_value_02 VARCHAR(100) COMMENT '테스트 상한 값 02',
    clm_test_lower_value_02 VARCHAR(100) COMMENT '테스트 하한 값 02',
    clm_comment TEXT COMMENT '비고',
    clm_value_watt VARCHAR(100) COMMENT '와트 값',
    clm_value VARCHAR(100) COMMENT '값',
    clm_test_step VARCHAR(50) COMMENT '테스트 단계',
    clm_test_id VARCHAR(100) COMMENT '테스트 ID',
    clm_test_sec VARCHAR(50) COMMENT '테스트 초',
    clm_product_serial_no VARCHAR(100) COMMENT '제품 시리얼 번호',
    clm_lower_value_watt VARCHAR(100) COMMENT '하한 와트 값',
    clm_upper_value_watt VARCHAR(100) COMMENT '상한 와트 값',
    clm_test_version_id VARCHAR(100) COMMENT '테스트 버전 ID',
    clm_regist_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
    clm_update_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',
    INDEX idx_model_id (clm_model_id),
    INDEX idx_test_id (clm_test_id),
    INDEX idx_update_timestamp (clm_update_timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='테스트 스펙 데이터 테이블';
*/

-- ============================================
-- 트리거 예제: tbl_model_info 변경 시 버전 히스토리 자동 저장
-- ============================================

-- 모델 정보 업데이트 시 버전 히스토리 저장 트리거
DELIMITER $$

CREATE TRIGGER IF NOT EXISTS trg_model_info_update_version
AFTER UPDATE ON tbl_model_info
FOR EACH ROW
BEGIN
    DECLARE v_version_number INT;
    
    -- 다음 버전 번호 가져오기
    SELECT COALESCE(MAX(clm_version_number), 0) + 1
    INTO v_version_number
    FROM tbl_model_info_version_history
    WHERE clm_model_seq = NEW.clm_model_seq;
    
    -- 버전 히스토리에 저장
    INSERT INTO tbl_model_info_version_history (
        clm_model_seq,
        clm_client_name,
        clm_client_id,
        clm_test_step,
        clm_company_key,
        clm_model_id,
        clm_model_name,
        clm_model_version,
        clm_comment,
        clm_version_number,
        clm_change_type,
        clm_change_timestamp
    ) VALUES (
        OLD.clm_model_seq,
        OLD.clm_client_name,
        OLD.clm_client_id,
        OLD.clm_test_step,
        OLD.clm_company_key,
        OLD.clm_model_id,
        OLD.clm_model_name,
        OLD.clm_model_version,
        OLD.clm_comment,
        v_version_number,
        'UPDATE',
        NOW()
    );
END$$

DELIMITER ;

-- 모델 정보 삽입 시 버전 히스토리 저장 트리거
DELIMITER $$

CREATE TRIGGER IF NOT EXISTS trg_model_info_insert_version
AFTER INSERT ON tbl_model_info
FOR EACH ROW
BEGIN
    INSERT INTO tbl_model_info_version_history (
        clm_model_seq,
        clm_client_name,
        clm_client_id,
        clm_test_step,
        clm_company_key,
        clm_model_id,
        clm_model_name,
        clm_model_version,
        clm_comment,
        clm_version_number,
        clm_change_type,
        clm_change_timestamp
    ) VALUES (
        NEW.clm_model_seq,
        NEW.clm_client_name,
        NEW.clm_client_id,
        NEW.clm_test_step,
        NEW.clm_company_key,
        NEW.clm_model_id,
        NEW.clm_model_name,
        NEW.clm_model_version,
        NEW.clm_comment,
        1,
        'INSERT',
        NOW()
    );
END$$

DELIMITER ;

-- ============================================
-- 유용한 쿼리 예제
-- ============================================

-- 특정 모델의 버전 히스토리 조회
-- SELECT * FROM tbl_model_info_version_history 
-- WHERE clm_model_seq = 'MODEL_SEQ_VALUE' 
-- ORDER BY clm_version_number DESC;

-- 최신 버전 정보 조회
-- SELECT m.*, 
--        (SELECT MAX(clm_version_number) FROM tbl_model_info_version_history WHERE clm_model_seq = m.clm_model_seq) as latest_version
-- FROM tbl_model_info m;

-- 특정 날짜 이후 변경된 모델 정보 조회
-- SELECT * FROM tbl_model_info 
-- WHERE clm_update_timestamp >= '2024-01-01 00:00:00'
-- ORDER BY clm_update_timestamp DESC;


