package itf.com.lms.logic.test;

/**
 * Represents a single step in a test sequence.
 * Immutable: use Builder to construct.
 */
public class TestStep {

    public enum TestStepType {
        BLUETOOTH_COMMAND,
        USB_DATA_COLLECTION,
        WAIT,
        EVALUATE
    }

    private final TestStepType type;
    private final String command;
    private final long timeoutMs;
    private final String expectedValue;

    private TestStep(Builder builder) {
        this.type = builder.type;
        this.command = builder.command;
        this.timeoutMs = builder.timeoutMs;
        this.expectedValue = builder.expectedValue;
    }

    public static Builder builder() {
        return new Builder();
    }

    public TestStepType getType() {
        return type;
    }

    public String getCommand() {
        return command;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public String getExpectedValue() {
        return expectedValue;
    }

    public static class Builder {
        private TestStepType type = TestStepType.WAIT;
        private String command = "";
        private long timeoutMs = 0;
        private String expectedValue = "";

        public Builder type(TestStepType type) {
            this.type = type;
            return this;
        }

        public Builder command(String command) {
            this.command = command;
            return this;
        }

        public Builder timeout(long timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }

        public Builder expectedValue(String expectedValue) {
            this.expectedValue = expectedValue;
            return this;
        }

        public TestStep build() {
            return new TestStep(this);
        }
    }
}










