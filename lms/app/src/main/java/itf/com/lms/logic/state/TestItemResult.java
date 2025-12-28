package itf.com.lms.logic.state;

public class TestItemResult {
    private final String testItemCode;
    private final boolean passed;
    private final String message;

    public TestItemResult(String testItemCode, boolean passed, String message) {
        this.testItemCode = testItemCode;
        this.passed = passed;
        this.message = message;
    }

    public String getTestItemCode() {
        return testItemCode;
    }

    public boolean isPassed() {
        return passed;
    }

    public String getMessage() {
        return message;
    }
}










