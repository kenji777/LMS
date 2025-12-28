package itf.com.lms.util;

import android.content.Context;

import java.util.List;
import java.util.Map;

/**
 * 앱 설정 캐시/헬퍼 (tbl_setting_info 기반)
 */
public final class AppSettings {
    private AppSettings() {}

    public static final String SETTING_ID_VIBRATION_AMPLITUDE = "VIBRATION_AMPLITUDE";

    // null이면 DEFAULT_AMPLITUDE 사용
    private static volatile Integer vibrationAmplitude = null;

    public static Integer getVibrationAmplitude() {
        return vibrationAmplitude;
    }

    public static void setVibrationAmplitudeFromString(String value) {
        vibrationAmplitude = parseAmplitude(value);
    }

    /**
     * DB/다이얼로그에 저장할 값으로 정규화
     * - DEFAULT/빈 값이면 128 (중간값)
     * - 숫자면 10 단위로 반올림하여 10~250 범위로 저장
     */
    public static String normalizeVibrationAmplitudeValue(String value) {
        Integer amp = parseAmplitude(value);
        // parseAmplitude는 이제 항상 Integer를 반환하므로 null 체크 불필요
        return String.valueOf(amp != null ? amp : 128);
    }

    public static void loadFromDb(Context context) {
        if (context == null) return;
        try {
            String value = null;
            List<Map<String, String>> settings = TestData.selectSettingInfo(context);
            if (settings != null) {
                for (Map<String, String> row : settings) {
                    if (row == null) continue;
                    if (SETTING_ID_VIBRATION_AMPLITUDE.equals(row.get("clm_setting_id"))) {
                        value = row.get("clm_setting_value");
                        break;
                    }
                }
            }
            setVibrationAmplitudeFromString(value);
        } catch (Exception ignored) {
            // ignore
        }
    }

    private static Integer parseAmplitude(String value) {
        if (value == null) return null;
        String v = value.trim();
        if (v.isEmpty()) return 128;  // 빈 값이면 128 (중간값)
        if ("DEFAULT".equalsIgnoreCase(v)) return 128;  // DEFAULT면 128 (중간값)

        try {
            int amp = Integer.parseInt(v);
            // 10 단위로 조절: 10,20,...,250 (255는 10 단위가 아니므로 250으로 클램프)
            if (amp < 10) amp = 10;
            if (amp > 250) amp = 250;
            // nearest 10 (e.g., 55 -> 60)
            amp = ((amp + 5) / 10) * 10;
            if (amp < 10) amp = 10;
            if (amp > 250) amp = 250;
            return amp;
        } catch (Exception e) {
            return 128;  // 파싱 실패 시 128 (중간값)
        }
    }
}


