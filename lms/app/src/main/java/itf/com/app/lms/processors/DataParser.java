package itf.com.app.lms.processors;

import itf.com.app.lms.util.Constants;
import itf.com.app.lms.util.LogManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;

/**
 * DataParser - Handles data parsing (JSON, Excel, etc.)
 *
 * Responsibilities:
 * - JSON parsing (test spec, barcode info, etc.)
 * - Excel file parsing (temperature data)
 * - Data transformation
 * - Error handling
 *
 * Usage:
 * <pre>
 * DataParser parser = new DataParser();
 * List&lt;Map&lt;String, String&gt;&gt; specData = parser.parseTestSpecJson(json);
 * List&lt;Map&lt;String, String&gt;&gt; tempData = parser.readTemperatureExcel(inputStream, tableType);
 * </pre>
 */
public class DataParser {

    private static final String TAG = "DataParser";

    /**
     * Parse test spec JSON data
     */
    public List<Map<String, String>> parseTestSpecJson(String json) {
        List<Map<String, String>> result = new ArrayList<>();
        
        if (json == null || json.isEmpty()) {
            LogManager.w(LogManager.LogCategory.SI, TAG, "Test spec JSON is null or empty");
            return result;
        }

        try {
            JSONObject jsonObject = new JSONObject(json);
            JSONArray dataArray = jsonObject.optJSONArray("data");
            
            if (dataArray != null) {
                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject item = dataArray.getJSONObject(i);
                    Map<String, String> map = new HashMap<>();
                    
                    // Extract common fields
                    if (item.has("test_item_seq")) {
                        map.put("test_item_seq", String.valueOf(item.getInt("test_item_seq")));
                    }
                    if (item.has("test_item_command")) {
                        map.put("test_item_command", item.getString("test_item_command"));
                    }
                    if (item.has("test_item_name")) {
                        map.put("test_item_name", item.getString("test_item_name"));
                    }
                    if (item.has("test_item_count")) {
                        map.put("test_item_count", String.valueOf(item.getInt("test_item_count")));
                    }
                    if (item.has("test_item_spec_lower")) {
                        map.put("test_item_spec_lower", item.getString("test_item_spec_lower"));
                    }
                    if (item.has("test_item_spec_upper")) {
                        map.put("test_item_spec_upper", item.getString("test_item_spec_upper"));
                    }
                    
                    result.add(map);
                }
            }
            
            LogManager.i(LogManager.LogCategory.SI, TAG, 
                    "Parsed " + result.size() + " test spec items");
        } catch (JSONException e) {
            LogManager.e(LogManager.LogCategory.ER, TAG, "Error parsing test spec JSON", e);
        }

        return result;
    }

    /**
     * Parse barcode info JSON data
     */
    public Map<String, String> parseBarcodeJson(String json) {
        Map<String, String> result = new HashMap<>();
        
        if (json == null || json.isEmpty()) {
            LogManager.w(LogManager.LogCategory.BI, TAG, "Barcode JSON is null or empty");
            return result;
        }

        try {
            JSONObject jsonObject = new JSONObject(json);
            
            // Extract barcode info fields
            if (jsonObject.has("product_serial_no")) {
                result.put("product_serial_no", jsonObject.getString("product_serial_no"));
            }
            if (jsonObject.has("model_id")) {
                result.put("model_id", jsonObject.getString("model_id"));
            }
            if (jsonObject.has("model_name")) {
                result.put("model_name", jsonObject.getString("model_name"));
            }
            if (jsonObject.has("model_nation")) {
                result.put("model_nation", jsonObject.getString("model_nation"));
            }
            
            LogManager.i(LogManager.LogCategory.BI, TAG, "Parsed barcode info");
        } catch (JSONException e) {
            LogManager.e(LogManager.LogCategory.ER, TAG, "Error parsing barcode JSON", e);
        }

        return result;
    }

    /**
     * Read temperature data from Excel file
     */
    public List<Map<String, String>> readTemperatureExcel(InputStream inputStream, String tableType) {
        List<Map<String, String>> result = new ArrayList<>();
        
        if (inputStream == null) {
            LogManager.w(LogManager.LogCategory.TH, TAG, "Temperature Excel input stream is null");
            return result;
        }

        try {
            Workbook workbook = Workbook.getWorkbook(inputStream);
            if (workbook == null) {
                LogManager.w(LogManager.LogCategory.TH, TAG, "Failed to read Excel workbook");
                return result;
            }

            int sheetIndex = tableType.equals("1") ? 0 : 1; // Cold: 0, Hot: 1
            Sheet sheet = workbook.getSheet(sheetIndex);
            
            if (sheet != null) {
                int colTotal = sheet.getColumns();
                int rowIndexStart = 1;
                int rowTotal = sheet.getColumn(colTotal - 1).length;

                for (int row = rowIndexStart; row < rowTotal; row++) {
                    Map<String, String> map = new HashMap<>();
                    map.put(Constants.Common.CLM_TEMP_SEQ, String.valueOf(row));
                    map.put(Constants.JsonKeys.CLM_TEMPERATURE, sheet.getCell(1, row).getContents());
                    map.put(Constants.Common.CLM_REGIST, sheet.getCell(2, row).getContents());
                    map.put(Constants.Common.CLM_VOLTAGE, sheet.getCell(3, row).getContents());
                    map.put(Constants.Common.CLM_10_BIT, sheet.getCell(4, row).getContents());
                    map.put(Constants.JsonKeys.CLM_12_BIT, sheet.getCell(6, row).getContents());
                    map.put(Constants.JsonKeys.CLM_COMMENT, "");
                    result.add(map);
                }
            }

            workbook.close();
            LogManager.i(LogManager.LogCategory.TH, TAG, 
                    "Read " + result.size() + " temperature data items from Excel");
        } catch (IOException e) {
            LogManager.e(LogManager.LogCategory.ER, TAG, 
                    "IO error reading temperature Excel", e);
        } catch (BiffException e) {
            LogManager.e(LogManager.LogCategory.ER, TAG, 
                    "Biff error reading temperature Excel", e);
        } catch (Exception e) {
            LogManager.e(LogManager.LogCategory.ER, TAG, 
                    "Error reading temperature Excel", e);
        }

        return result;
    }

    /**
     * Parse JSON data by type
     */
    public Object parseJsonByType(String dataType, String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }

        switch (dataType) {
            case "test_spec":
                return parseTestSpecJson(json);
            case "barcode":
                return parseBarcodeJson(json);
            default:
                LogManager.w(LogManager.LogCategory.SI, TAG, 
                        "Unknown data type: " + dataType);
                return null;
        }
    }

    /**
     * Validate JSON format
     */
    public boolean validateJson(String json) {
        if (json == null || json.isEmpty()) {
            return false;
        }

        try {
            new JSONObject(json);
            return true;
        } catch (JSONException e) {
            try {
                new JSONArray(json);
                return true;
            } catch (JSONException e2) {
                return false;
            }
        }
    }
}


