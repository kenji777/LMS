package itf.com.lms.vo;


import java.util.Map;

public class VoSystemSetting {

    // tbl_setting_info 기반 설정 VO
    private String setting_seq;
    private String setting_id;
    private String setting_value;
    private String comment;
    private String test_timestamp;
    private String clm_setting_name_kr;
    private String clm_setting_name_en;

    boolean isVisible = true;  // 노출 여부 (기본값: true)
    boolean isChecked = false; // 체크박스 선택 여부

    public VoSystemSetting(Map<String, String> mapProcessInfo) {
        this.setting_seq = mapProcessInfo.get("clm_setting_seq");
        this.setting_id = mapProcessInfo.get("clm_setting_id");
        this.setting_value = mapProcessInfo.get("clm_setting_value");
        this.comment = mapProcessInfo.get("clm_comment");
        this.test_timestamp = mapProcessInfo.get("clm_test_timestamp");
        this.clm_setting_name_kr = mapProcessInfo.get("clm_setting_name_kr");
        this.clm_setting_name_en = mapProcessInfo.get("clm_setting_name_en");
    }

    public String getSetting_seq() {
        return setting_seq;
    }

    public void setSetting_seq(String setting_seq) {
        this.setting_seq = setting_seq;
    }

    public String getSetting_id() {
        return setting_id;
    }

    public void setSetting_id(String setting_id) {
        this.setting_id = setting_id;
    }

    public String getSetting_value() {
        return setting_value;
    }

    public void setSetting_value(String setting_value) {
        this.setting_value = setting_value;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getTest_timestamp() {
        return test_timestamp;
    }

    public void setTest_timestamp(String test_timestamp) {
        this.test_timestamp = test_timestamp;
    }

    public String getClm_setting_name_kr() {
        return clm_setting_name_kr;
    }

    public void setClm_setting_name_kr(String clm_setting_name_kr) {
        this.clm_setting_name_kr = clm_setting_name_kr;
    }

    public String getClm_setting_name_en() {
        return clm_setting_name_en;
    }

    public void setClm_setting_name_en(String clm_setting_name_en) {
        this.clm_setting_name_en = clm_setting_name_en;
    }

    /**
     * 화면 표시용 설정명 (kr 우선, 없으면 id)
     */
    public String getDisplaySettingName() {
        if (clm_setting_name_kr != null && !clm_setting_name_kr.trim().isEmpty()) {
            return clm_setting_name_kr.trim();
        }
        if (setting_id != null) {
            return setting_id;
        }
        return "";
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