package itf.com.lms.logic.data;

/**
 * Test specification model (immutable).
 */
public class TestSpec {
    private final String testItemCode;
    private final String testItemName;
    private final double lowerLimit;
    private final double upperLimit;
    private final String unit;
    private final long timeoutMs;

    private TestSpec(Builder builder) {
        this.testItemCode = builder.testItemCode;
        this.testItemName = builder.testItemName;
        this.lowerLimit = builder.lowerLimit;
        this.upperLimit = builder.upperLimit;
        this.unit = builder.unit;
        this.timeoutMs = builder.timeoutMs;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getTestItemCode() {
        return testItemCode;
    }

    public String getTestItemName() {
        return testItemName;
    }

    public double getLowerLimit() {
        return lowerLimit;
    }

    public double getUpperLimit() {
        return upperLimit;
    }

    public String getUnit() {
        return unit;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    @Override
    public String toString() {
        return "TestSpec{" +
                "testItemCode='" + testItemCode + '\'' +
                ", lowerLimit=" + lowerLimit +
                ", upperLimit=" + upperLimit +
                ", unit='" + unit + '\'' +
                ", timeoutMs=" + timeoutMs +
                '}';
    }

    public static class Builder {
        private String testItemCode;
        private String testItemName;
        private double lowerLimit;
        private double upperLimit;
        private String unit = "";
        private long timeoutMs;

        public Builder testItemCode(String testItemCode) {
            this.testItemCode = testItemCode;
            return this;
        }

        public Builder testItemName(String testItemName) {
            this.testItemName = testItemName;
            return this;
        }

        public Builder lowerLimit(double lowerLimit) {
            this.lowerLimit = lowerLimit;
            return this;
        }

        public Builder upperLimit(double upperLimit) {
            this.upperLimit = upperLimit;
            return this;
        }

        public Builder unit(String unit) {
            this.unit = unit;
            return this;
        }

        public Builder timeout(long timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }

        public TestSpec build() {
            return new TestSpec(this);
        }
    }
}










