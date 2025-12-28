package itf.com.app.lms.logic.data;

public class TestResultModel {
    private final String serialNo;
    private final String testItemCode;
    private final boolean passed;
    private final double measuredValue;
    private final String message;

    public TestResultModel(String serialNo, String testItemCode, boolean passed, double measuredValue, String message) {
        this.serialNo = serialNo;
        this.testItemCode = testItemCode;
        this.passed = passed;
        this.measuredValue = measuredValue;
        this.message = message;
    }

    public String getSerialNo() {
        return serialNo;
    }

    public String getTestItemCode() {
        return testItemCode;
    }

    public boolean isPassed() {
        return passed;
    }

    public double getMeasuredValue() {
        return measuredValue;
    }

    public String getMessage() {
        return message;
    }
}










