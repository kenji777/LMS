package itf.com.app.lms.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SpecProcessingResult {
    public String[][] arrTestItems;
    public int totalTimeCnt;
    public String valueWatt;
    public String lowerValueWatt;
    public String upperValueWatt;
    public String productSerialNo;
    public String compValueWatt;
    public String compLowerValueWatt;
    public String compUpperValueWatt;
    public String pumpValueWatt;
    public String pumpLowerValueWatt;
    public String pumpUpperValueWatt;
    public String heaterValueWatt;
    public String heaterLowerValueWatt;
    public String heaterUpperValueWatt;
    public List<Map<String, String>> listItems = new ArrayList<>();
}
