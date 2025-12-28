package itf.com.app.lms.vo;


import java.util.Map;

public class VoTestItemSpec {
    /* 아이템의 정보를 담기 위한 클래스 */
    String test_seq;
    String test_command;
    String test_name;
    String test_type;
    String test_response_value;
    String test_upper_value_watt;
    String test_lower_value_watt;
    String test_upper_value_temp;
    String test_lower_value_temp;
    String comment;

    public VoTestItemSpec(Map<String, String> mapProcessInfo) {
        this.test_seq = mapProcessInfo.get("test_seq");
        this.test_command = mapProcessInfo.get("test_command");
        this.test_name = mapProcessInfo.get("test_name");
        this.test_type = mapProcessInfo.get("test_type");
        this.test_response_value = mapProcessInfo.get("test_response_value");
        this.test_upper_value_watt = mapProcessInfo.get("test_upper_value_watt");
        this.test_lower_value_watt = mapProcessInfo.get("test_lower_value_watt");
        this.test_upper_value_temp = mapProcessInfo.get("test_upper_value_temp");
        this.test_lower_value_temp = mapProcessInfo.get("test_lower_value_temp");
        this.comment = mapProcessInfo.get("comment");
    }

    public String getTest_seq() {
        return test_seq;
    }

    public void setTest_seq(String test_seq) {
        this.test_seq = test_seq;
    }

    public String getTest_command() {
        return test_command;
    }

    public void setTest_command(String test_command) {
        this.test_command = test_command;
    }

    public String getTest_name() {
        return test_name;
    }

    public void setTest_name(String test_name) {
        this.test_name = test_name;
    }

    public String getTest_type() {
        return test_type;
    }

    public void setTest_type(String test_type) {
        this.test_type = test_type;
    }

    public String getTest_response_value() {
        return test_response_value;
    }

    public void setTest_response_value(String test_response_value) {
        this.test_response_value = test_response_value;
    }

    public String getTest_upper_value_watt() {
        return test_upper_value_watt;
    }

    public void setTest_upper_value_watt(String test_upper_value_watt) {
        this.test_upper_value_watt = test_upper_value_watt;
    }

    public String getTest_lower_value_watt() {
        return test_lower_value_watt;
    }

    public void setTest_lower_value_watt(String test_lower_value_watt) {
        this.test_lower_value_watt = test_lower_value_watt;
    }

    public String getTest_upper_value_temp() {
        return test_upper_value_temp;
    }

    public void setTest_upper_value_temp(String test_upper_value_temp) {
        this.test_upper_value_temp = test_upper_value_temp;
    }

    public String getTest_lower_value_temp() {
        return test_lower_value_temp;
    }

    public void setTest_lower_value_temp(String test_lower_value_temp) {
        this.test_lower_value_temp = test_lower_value_temp;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}