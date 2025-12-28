package itf.com.app.lms.util;

import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

import static itf.com.app.lms.ActivityModelList.globalModelId;

import itf.com.app.lms.ActivityModelTestProcess;

class RequestVersionInfoThreadAsync extends AsyncTask<String, Void, String> {
    private final WeakReference<ActivityModelTestProcess> activityRef;
    private final String localVersionId;
    private final String localModelId;
    private final String serverIp;
    private final int unitNo;

    RequestVersionInfoThreadAsync(ActivityModelTestProcess activity, String localVersionId, String localModelId) {
        this.activityRef = new WeakReference<>(activity);
        this.localVersionId = localVersionId;
        this.localModelId = localModelId;
        this.serverIp = activity.getServerIp();
        this.unitNo = activity.getUnitNo();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(String... strings) {
        try {
            new Thread(() -> {
                HttpURLConnection connection = null;
                try {
                    String url = Constants.URLs.HTTP_PROTOCOL + serverIp + Constants.URLs.ENDPOINT_VERSION_INFO_LIST;
                    if (globalModelId != null && !globalModelId.isEmpty()) {
                        url += Constants.Common.QUESTION + Constants.URLs.PARAM_MODEL_ID + globalModelId;
                    }
                    // logInfo(LogManager.LogCategory.PS, "RequestVersionInfoThreadAsync >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> url." + url);
                    URL obj = new URL(url);
                    connection = (HttpURLConnection) obj.openConnection();

                    connection.setReadTimeout(Constants.Timeouts.HTTP_READ_TIMEOUT_LONG_MS);
                    connection.setConnectTimeout(Constants.Timeouts.HTTP_CONNECT_TIMEOUT_LONG_MS);
                    connection.setRequestMethod(Constants.HTTP.METHOD_POST);
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    connection.setRequestProperty(Constants.HTTP.HEADER_CONTENT_TYPE, Constants.HTTP.CONTENT_TYPE_JSON);
                    connection.setRequestProperty(Constants.HTTP.HEADER_CACHE_CONTROL, Constants.HTTP.CACHE_CONTROL_NO_CACHE);

                    try {
                        int retCode = connection.getResponseCode();
                        // logInfo(LogManager.LogCategory.SI, "VersionInfoList response code: " + retCode);

                        if (retCode == HttpURLConnection.HTTP_OK) {
                            // 네트워크 상태가 정상일때 tvRunWsRamp를 파란색으로 변경
                            ActivityModelTestProcess activity = activityRef.get();
                            if (activity != null) {
                                activity.updateRunWsRampConnected(unitNo);
                            }

                            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                            try {
                                String line = null;
                                String lineTmp = null;
                                StringBuilder sb = new StringBuilder();
                                int readCount = 0;

                                while (readCount < Constants.Timeouts.MAX_READ_COUNT) {
                                    lineTmp = reader.readLine();
                                    if (lineTmp == null) {
                                        break;
                                    }
                                    readCount++;
                                    if (!lineTmp.trim().equals("")) {
                                        sb.append(lineTmp);
                                        line = lineTmp;
                                    }
                                }

                                final String data = (line != null && !line.trim().equals("")) ? line :
                                        (sb.length() > 0 ? sb.toString() : null);

                                if (data != null && !data.trim().equals("")) {
                                    // 서버 데이터와 로컬 비교
                                    try {
                                        JSONObject jsonObject = new JSONObject(data);
                                        String serverVersionId = jsonObject.optString(Constants.JsonKeys.CLM_TEST_VERSION_ID, "");
                                        String serverModelId = jsonObject.optString(Constants.JsonKeys.CLM_MODEL_ID, "");

                                        ActivityModelTestProcess act = activityRef.get();
                                        if (act != null) {
                                            // logInfo(LogManager.LogCategory.SI, "Version info received: " + data);
                                            act.logInfo(LogManager.LogCategory.SI, String.format("Version comparison - Local: [%s, %s], Server: [%s, %s]",
                                                    localVersionId, localModelId, serverVersionId, serverModelId));

                                            // 서버와 다르면 TestInfoList.jsp 에서 DB 업데이트
                                            boolean versionMismatch = false;
                                            if (localVersionId != null && !localVersionId.isEmpty() &&
                                                    serverVersionId != null && !serverVersionId.isEmpty() &&
                                                    !localVersionId.equals(serverVersionId)) {
                                                versionMismatch = true;
                                                act.logWarn(LogManager.LogCategory.SI, "Test version ID mismatch detected. Updating test spec data.");
                                            } else if (localVersionId != null && !localVersionId.isEmpty() &&
                                                    serverVersionId != null && !serverVersionId.isEmpty() &&
                                                    !localVersionId.equals(serverVersionId)) {
                                                versionMismatch = true;
                                                act.logWarn(LogManager.LogCategory.SI, "Model version mismatch detected. Updating test spec data.");
                                            }

                                            if (versionMismatch) {
                                                // TestInfoList.jsp 에서 DB 업데이트
                                                act.clearHttpHandlerQueue();
                                                RequestThreadAsync thread = new RequestThreadAsync(act);
                                                thread.execute();
                                                // logInfo(LogManager.LogCategory.SI, "Test spec data update requested due to version mismatch.");
                                            } else {
                                                act.logInfo(LogManager.LogCategory.SI, "Version matches. No update needed.");
                                            }
                                        }
                                    } catch (JSONException e) {
                                        ActivityModelTestProcess act = activityRef.get();
                                        if (act != null) {
                                            act.logError(LogManager.LogCategory.ER, "Error parsing version info JSON", e);
                                        }
                                    }
                                }
                            } finally {
                                try {
                                    reader.close();
                                } catch (IOException e) {
                                    ActivityModelTestProcess act = activityRef.get();
                                    if (act != null) {
                                        act.logError(LogManager.LogCategory.ER, Constants.ErrorMessages.ERROR_CLOSING_READER, e);
                                    }
                                }
                            }
                        } else {
                            // 네트워크 상태가 비정상이면 tvRunWsRamp를 빨간색으로
                            ActivityModelTestProcess act = activityRef.get();
                            if (act != null) {
                                act.logWarn(LogManager.LogCategory.SI, "VersionInfoList request failed with code: " + retCode);
                                act.updateRunWsRampDisconnected();
                            }
                        }
                    } catch (Exception e) {
                        ActivityModelTestProcess act = activityRef.get();
                        if (act != null) {
                            act.logError(LogManager.LogCategory.ER, "VersionInfoList connection error", e);
                            // 네트워크 에러 시 tvRunWsRamp 빨간색
                            act.updateRunWsRampDisconnected();
                        }
                    } finally {
                        ActivityModelTestProcess act = activityRef.get();
                        if (act != null) {
                            act.safeDisconnectConnection(connection);
                        } else if (connection != null) {
                            try {
                                connection.disconnect();
                            } catch (Exception ignored) {}
                        }
                    }
                } catch (Exception e) {
                    ActivityModelTestProcess act = activityRef.get();
                    if (act != null) {
                        act.logError(LogManager.LogCategory.ER, "VersionInfoList request error", e);
                        // 네트워크 에러 시 tvRunWsRamp 빨간색
                        act.updateRunWsRampDisconnected();
                    }
                }
            }).start();
        } catch (Exception e) {
            ActivityModelTestProcess act = activityRef.get();
            if (act != null) {
                act.logError(LogManager.LogCategory.ER, "RequestVersionInfoThreadAsync error", e);
            }
        }

        return null;
    }

    @Override
    protected void onPostExecute(String s) {
    }
}
