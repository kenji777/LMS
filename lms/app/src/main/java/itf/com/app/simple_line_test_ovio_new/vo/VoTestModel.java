package itf.com.app.simple_line_test_ovio_new.vo;


import java.util.Map;

public class VoTestModel {
    String test_version;
    String model_version;
    String test_model_no;
    String test_model_id;
    String test_model_name;
    String brand_id;
    String brand_name;
    String test_model_nationality_id;
    String test_model_nationality_name;
    String reg_timestamp;
    String comment;

    public VoTestModel(Map<String, String> mapProcessInfo) {
//        mapModelInfo.put("clm_test_model_no", "001");
//        mapModelInfo.put("clm_model_version", lstData.get(i).get("clm_model_version"));
//        mapModelInfo.put("clm_test_version", lstData.get(i).get("clm_test_version"));
//        mapModelInfo.put("clm_test_model_id", lstData.get(i).get("clm_model_id"));
//        mapModelInfo.put("clm_brand_name", lstData.get(i).get("clm_client_name"));
//        mapModelInfo.put("clm_test_model_name", lstData.get(i).get("clm_model_name"));
//        mapModelInfo.put("clm_test_model_nationality_id", lstData.get(i).get("clm_test_step"));
//        mapModelInfo.put("clm_test_model_nationality_name", lstData.get(i).get("clm_test_step"));
        this.model_version = mapProcessInfo.get("clm_model_version");
        this.test_model_no = mapProcessInfo.get("clm_test_model_no");
        this.test_version = mapProcessInfo.get("clm_test_version");
        this.test_model_id = mapProcessInfo.get("clm_test_model_id");
        this.brand_id = mapProcessInfo.get("clm_brand_id");
        this.brand_name = mapProcessInfo.get("clm_brand_name");
        this.test_model_name = mapProcessInfo.get("clm_test_model_name");
        this.test_model_nationality_id = mapProcessInfo.get("clm_test_model_nationality_id");
        this.test_model_nationality_name = mapProcessInfo.get("clm_test_model_nationality_name");
        this.reg_timestamp = mapProcessInfo.get("clm_reg_timestamp");
        this.comment = mapProcessInfo.get("clm_comment");
    }

    public String getBrand_id() {
        return brand_id;
    }

    public void setBrand_id(String brand_id) {
        this.brand_id = brand_id;
    }

    public String getBrand_name() {
        return brand_name;
    }

    public void setBrand_name(String brand_name) {
        this.brand_name = brand_name;
    }

    public String getTest_version() {
        return test_version;
    }

    public void setTest_version(String test_version) {
        this.test_version = test_version;
    }

    public String getModel_version() {
        return model_version;
    }

    public void setModel_version(String model_version) {
        this.model_version = model_version;
    }

    public String getTest_model_no() {
        return test_model_no;
    }

    public void setTest_model_no(String test_model_no) {
        this.test_model_no = test_model_no;
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

    public String getTest_model_nationality_id() {
        return test_model_nationality_id;
    }

    public void setTest_model_nationality_id(String test_model_nationality_id) {
        this.test_model_nationality_id = test_model_nationality_id;
    }

    public String getTest_model_nationality_name() {
        return test_model_nationality_name;
    }

    public void setTest_model_nationality_name(String test_model_nationality_name) {
        this.test_model_nationality_name = test_model_nationality_name;
    }

    public String getReg_timestamp() {
        return reg_timestamp;
    }

    public void setReg_timestamp(String reg_timestamp) {
        this.reg_timestamp = reg_timestamp;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}