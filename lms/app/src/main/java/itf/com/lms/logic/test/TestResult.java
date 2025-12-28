package itf.com.lms.logic.test;

/**
 * Represents the outcome of a test evaluation.
 */
public class TestResult {
    private final boolean passed;
    private final double measuredValue;
    private final String reason;

    public TestResult(boolean passed, double measuredValue, String reason) {
        this.passed = passed;
        this.measuredValue = measuredValue;
        this.reason = reason;
    }

    public boolean isPassed() {
        return passed;
    }

    public double getMeasuredValue() {
        return measuredValue;
    }

    public String getReason() {
        return reason;
    }
}










