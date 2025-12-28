package itf.com.app.lms.logic.data;

public class TestHistory {
    private final String serialNo;
    private final String modelId;
    private final String testItemCode;
    private final String result;
    private final String timestamp;

    public TestHistory(String serialNo, String modelId, String testItemCode, String result, String timestamp) {
        this.serialNo = serialNo;
        this.modelId = modelId;
        this.testItemCode = testItemCode;
        this.result = result;
        this.timestamp = timestamp;
    }

    public String getSerialNo() {
        return serialNo;
    }

    public String getModelId() {
        return modelId;
    }

    public String getTestItemCode() {
        return testItemCode;
    }

    public String getResult() {
        return result;
    }

    public String getTimestamp() {
        return timestamp;
    }
}










