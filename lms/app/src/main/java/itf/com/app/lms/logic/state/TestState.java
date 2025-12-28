package itf.com.app.lms.logic.state;

public enum TestState {
    IDLE("준비"),
    INITIALIZING("초기화 중"),
    READY("대기"),
    BARCODE_SCANNING("바코드 스캔 중"),
    BARCODE_VALIDATING("바코드 확인 중"),
    TESTING("테스트 진행 중"),
    EVALUATING("결과 평가 중"),
    COMPLETED("완료"),
    FAILED("실패"),
    TIMEOUT("시간 초과"),
    ERROR("오류");

    private final String description;

    TestState(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}










