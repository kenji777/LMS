package itf.com.app.lms.vo;


import java.util.HashMap;
import java.util.Map;

public class VoTestItem {
    /* 아이템의 정보를 담기 위한 클래스 */
    String test_item_seq;

    String test_model_id;

    String test_item_name;
    String test_item_command;

    String test_item_result;

    String test_item_value;

    String test_item_info;

    String test_finish_yn;

    String test_temperature;

    String test_electric_val;

    String test_response_value;

    String test_result_value;

    String test_result_check_value;
    String test_model_name;
    String test_model_nation;
    
    // 블루투스 수신값 관련
    String test_bt_raw_message;
    String test_bt_raw_response;
    String test_bt_processed_value;
    
    // 상한값/하한값 관련
    String test_upper_value;
    String test_lower_value;

    public VoTestItem(Map<String, String> mapProcessInfo) {
        this.test_item_seq = mapProcessInfo.get("test_item_seq");
        this.test_model_id = mapProcessInfo.get("test_model_id");
        this.test_model_name = mapProcessInfo.get("test_model_name");
        this.test_model_nation = mapProcessInfo.get("test_model_nation");
        this.test_item_name = mapProcessInfo.get("test_item_name");
        this.test_item_command = mapProcessInfo.get("test_item_command");
        this.test_item_result = mapProcessInfo.get("test_item_result");
        this.test_finish_yn = mapProcessInfo.get("test_finish_yn");
        this.test_item_value = mapProcessInfo.get("test_item_value");
        this.test_temperature = mapProcessInfo.get("test_temperature");
        this.test_electric_val = mapProcessInfo.get("test_electric_val");
        this.test_response_value = mapProcessInfo.get("test_response_value");
        this.test_result_value = mapProcessInfo.get("test_result_value");
        this.test_item_info = mapProcessInfo.get("test_item_info");
        this.test_result_check_value = mapProcessInfo.get("test_result_check_value");
        this.test_bt_raw_message = mapProcessInfo.get("test_bt_raw_message");
        this.test_bt_raw_response = mapProcessInfo.get("test_bt_raw_response");
        this.test_bt_processed_value = mapProcessInfo.get("test_bt_processed_value");
        this.test_upper_value = mapProcessInfo.get("test_upper_value");
        this.test_lower_value = mapProcessInfo.get("test_lower_value");
    }

    public Map<String, String> VoMapInfo() {
        Map<String, String> mapProcessInfo = new HashMap<>();
        mapProcessInfo.put("test_item_seq", this.test_item_seq);
        mapProcessInfo.put("test_model_id", this.test_model_id);
        mapProcessInfo.put("test_model_nation", this.test_model_nation);
        mapProcessInfo.put("test_model_name", this.test_model_name);
        mapProcessInfo.put("test_item_name", this.test_item_name);
        mapProcessInfo.put("test_item_command", this.test_item_command);
        mapProcessInfo.put("test_item_result", this.test_item_result);
        mapProcessInfo.put("test_finish_yn", this.test_finish_yn);
        mapProcessInfo.put("test_item_value", this.test_item_value);
        mapProcessInfo.put("test_temperature", this.test_temperature);
        mapProcessInfo.put("test_electric_val", this.test_electric_val);
        mapProcessInfo.put("test_response_value", this.test_response_value);
        mapProcessInfo.put("test_result_value", this.test_result_value);
        mapProcessInfo.put("test_item_info", this.test_item_info);
        mapProcessInfo.put("test_result_check_value", this.test_result_check_value);
        mapProcessInfo.put("test_bt_raw_message", this.test_bt_raw_message);
        mapProcessInfo.put("test_bt_raw_response", this.test_bt_raw_response);
        mapProcessInfo.put("test_bt_processed_value", this.test_bt_processed_value);
        mapProcessInfo.put("test_upper_value", this.test_upper_value);
        mapProcessInfo.put("test_lower_value", this.test_lower_value);

        return  mapProcessInfo;
    }

    public String getTest_model_name() {
        return test_model_name;
    }

    public void setTest_model_name(String test_model_name) {
        this.test_model_name = test_model_name;
    }

    public String getTest_model_nation() {
        return test_model_nation;
    }

    public void setTest_model_nation(String test_model_nation) {
        this.test_model_nation = test_model_nation;
    }

    public String getTest_model_id() {
        return test_model_id;
    }

    public void setTest_model_id(String test_model_id) {
        this.test_model_id = test_model_id;
    }

    public String getTest_result_check_value() {
        return test_result_check_value;
    }

    public void setTest_result_check_value(String test_result_check_value) {
        this.test_result_check_value = test_result_check_value;
    }

    public String getTest_item_info() {
        return test_item_info;
    }

    public void setTest_item_info(String test_item_info) {
        this.test_item_info = test_item_info;
    }

    public String getTest_result_value() {
        return test_result_value;
    }

    public void setTest_result_value(String test_result_value) {
        this.test_result_value = test_result_value;
    }

    public String getTest_response_value() {
        return test_response_value;
    }

    public void setTest_response_value(String test_response_value) {
        this.test_response_value = test_response_value;
    }

    public String getTest_electric_val() {
        return test_electric_val;
    }

    public void setTest_electric_val(String test_electric_val) {
        this.test_electric_val = test_electric_val;
    }

    public String getTest_temperature() {
        return test_temperature;
    }

    public void setTest_temperature(String test_temperature) {
        this.test_temperature = test_temperature;
    }

    public String getTest_item_value() {
        return test_item_value;
    }

    public void setTest_item_value(String test_item_value) {
        this.test_item_value = test_item_value;
    }

    public String getTest_finish_yn() {
        return test_finish_yn;
    }

    public void setTest_finish_yn(String test_finish_yn) {
        this.test_finish_yn = test_finish_yn;
    }

    public String getTest_item_result() {
        return test_item_result;
    }

    public void setTest_item_result(String test_item_result) {
//        if(test_item_result.equals("Y")) {
//            this.test_item_result = "검사완료";
//        }
//        else if(test_item_result.equals("N")) {
//            this.test_item_result = "검사전";
//        }
//        else if(test_item_result.equals("P")) {
//            this.test_item_result = "검사중";
//        }
//        this.test_item_result = (test_item_result.equals("Y"))?"OK":"NG";
//        this.test_item_result = test_item_result;
        this.test_item_result = test_item_result;
    }

    public String getTest_item_seq() {
        return test_item_seq;
    }

    public void setTest_item_seq(String test_item_seq) {
        this.test_item_seq = test_item_seq;
    }

    public String getTest_item_name() {
        return test_item_name;
    }

    public void setTest_item_name(String test_item_name) {
        this.test_item_name = test_item_name;
    }

    public String getTest_item_command() {
        return test_item_command;
    }

    public void setTest_item_command(String test_item_command) {
        this.test_item_command = test_item_command;
    }

    public String getTest_bt_raw_message() {
        return test_bt_raw_message;
    }

    public void setTest_bt_raw_message(String test_bt_raw_message) {
        this.test_bt_raw_message = test_bt_raw_message;
    }

    public String getTest_bt_raw_response() {
        return test_bt_raw_response;
    }

    public void setTest_bt_raw_response(String test_bt_raw_response) {
        this.test_bt_raw_response = test_bt_raw_response;
    }

    public String getTest_bt_processed_value() {
        return test_bt_processed_value;
    }

    public void setTest_bt_processed_value(String test_bt_processed_value) {
        this.test_bt_processed_value = test_bt_processed_value;
    }

    public String getTest_upper_value() {
        return test_upper_value;
    }

    public void setTest_upper_value(String test_upper_value) {
        this.test_upper_value = test_upper_value;
    }

    public String getTest_lower_value() {
        return test_lower_value;
    }

    public void setTest_lower_value(String test_lower_value) {
        this.test_lower_value = test_lower_value;
    }
}