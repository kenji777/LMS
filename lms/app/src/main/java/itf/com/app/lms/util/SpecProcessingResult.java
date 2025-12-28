package itf.com.app.lms.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class SpecProcessingResult {
    String[][] arrTestItems;
    int totalTimeCnt;
    String valueWatt;
    String lowerValueWatt;
    String upperValueWatt;
    String productSerialNo;
    String compValueWatt;
    String compLowerValueWatt;
    String compUpperValueWatt;
    String pumpValueWatt;
    String pumpLowerValueWatt;
    String pumpUpperValueWatt;
    String heaterValueWatt;
    String heaterLowerValueWatt;
    String heaterUpperValueWatt;
    List<Map<String, String>> listItems = new ArrayList<>();
}
