package itf.com.app.lms.vo;


import java.util.Map;

public class VoTestHistory {

    //    mapData.put("test_history_seq", cur_temperature_data.getString(0));
//    mapData.put("test_model_id", cur_temperature_data.getString(1));
//    mapData.put("test_model_name", cur_temperature_data.getString(2));
//    mapData.put("test_model_nationality", cur_temperature_data.getString(3));
//    mapData.put("test_timestamp", cur_temperature_data.getString(4));
//    mapData.put("comment", cur_temperature_data.getString(5));
//
    /* 아이템의 정보를 담기 위한 클래스 */
    String test_history_no;
    String test_history_seq;

    String test_result;

    String test_model_id;
    String test_model_name;

    String test_model_nationality;

    String test_timestamp;

    String comment;

    String test_ok_count;

    String test_ng_count;

    boolean isVisible = true;  // 노출 여부 (기본값: true)
    boolean isChecked = false; // 체크박스 선택 여부

    public VoTestHistory(Map<String, String> mapProcessInfo) {
//        mapData.put("clm_test_history_seq", cur_temperature_data.getString(0));
//        mapData.put("clm_test_model_id", cur_temperature_data.getString(1));
//        mapData.put("clm_test_model_name", cur_temperature_data.getString(2));
//        mapData.put("clm_test_model_nationality", cur_temperature_data.getString(3));
//        mapData.put("clm_test_timestamp", cur_temperature_data.getString(4));
//        mapData.put("clm_comment", cur_temperature_data.getString(5));

        this.test_result = mapProcessInfo.get("clm_test_result");
        this.test_ok_count = mapProcessInfo.get("clm_test_ok_count");
        this.test_ng_count = mapProcessInfo.get("clm_test_ng_count");
        this.test_history_no = mapProcessInfo.get("clm_test_history_no");
        this.test_history_seq = mapProcessInfo.get("clm_test_history_seq");
        this.test_model_id = mapProcessInfo.get("clm_test_model_id");
        this.test_model_name = mapProcessInfo.get("clm_test_model_name");
        this.test_model_nationality = mapProcessInfo.get("clm_test_model_nationality");
        this.test_timestamp = mapProcessInfo.get("clm_test_timestamp");
        this.comment = mapProcessInfo.get("clm_comment");
    }

    public String getTest_ok_count() {
        return test_ok_count;
    }

    public void setTest_ok_count(String test_ok_count) {
        this.test_ok_count = test_ok_count;
    }

    public String getTest_ng_count() {
        return test_ng_count;
    }

    public void setTest_ng_count(String test_ng_count) {
        this.test_ng_count = test_ng_count;
    }

    public String getTest_history_no() {
        return test_history_no;
    }

    public void setTest_history_no(String test_history_no) {
        this.test_history_no = test_history_no;
    }

    public String getTest_history_seq() {
        return test_history_seq;
    }

    public void setTest_history_seq(String test_history_seq) {
        this.test_history_seq = test_history_seq;
    }

    public String getTest_model_id() {
        return test_model_id;
    }

    public void setTest_model_id(String test_model_id) {
        this.test_model_id = test_model_id;
    }

    public String getTest_model_name() {
        return test_model_name;
    }

    public void setTest_model_name(String test_model_name) {
        this.test_model_name = test_model_name;
    }

    public String getTest_model_nationality() {
        return test_model_nationality;
    }

    public void setTest_model_nationality(String test_model_nationality) {
        this.test_model_nationality = test_model_nationality;
    }

    public String getTest_timestamp() {
        return test_timestamp;
    }

    public void setTest_timestamp(String test_timestamp) {
        this.test_timestamp = test_timestamp;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getTest_result() {
        return test_result;
    }

    public void setTest_result(String test_result) {
        this.test_result = test_result;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean visible) {
        isVisible = visible;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }
}