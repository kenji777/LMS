package itf.com.app.lms.util;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

import itf.com.app.lms.ActivityModelTestProcess;

class RequestTestTaskThreadAsync extends AsyncTask<String, Void, String> {
    private final WeakReference<ActivityModelTestProcess> activityRef;
    private final String url;

    RequestTestTaskThreadAsync(ActivityModelTestProcess activity) {
        this.activityRef = new WeakReference<>(activity);
        this.url = activity.getUrlTestTaskStr();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(String... strings) {
        try {
            new Thread(() -> {
                // 동시성 문제 해결: connection을 로컬 변수로 변경 (각 스레드가 자신만의 connection 사용)
                HttpURLConnection connection = null;
                try {
                    URL obj = new URL(url);
                    connection = (HttpURLConnection) obj.openConnection();
                    // Log.i(TAG, "▶ [PS] RequestTestTaskThreadAsync.urlTestTaskStr " + urlTestTaskStr);

                    connection.setReadTimeout(Constants.Timeouts.HTTP_READ_TIMEOUT_LONG_MS);
                    connection.setConnectTimeout(Constants.Timeouts.HTTP_CONNECT_TIMEOUT_LONG_MS);
                    connection.setRequestMethod(Constants.HTTP.METHOD_GET);  // POST 방식으로 요청
                    connection.setDoInput(true);  // InputStream 으로 서버로부터 응답을 받겠다는 옵션
                    connection.setDoOutput(true); // OutputStream 으로 POST 데이터를 넘겨 주겠다는 옵션
                    connection.setRequestProperty(Constants.HTTP.HEADER_CONTENT_TYPE, Constants.HTTP.CONTENT_TYPE_JSON);    // application/json 형식으로 전송. (Request body 전달시 application/json 으로 서버에 전달)
                    connection.setRequestProperty(Constants.HTTP.HEADER_CACHE_CONTROL, Constants.HTTP.CACHE_CONTROL_NO_CACHE);

                    try {
                        int retCode = connection.getResponseCode();
                        // Log.i(TAG, "▶ [PS] test task response " + retCode);
                        if (retCode == HttpURLConnection.HTTP_OK) {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                            try {
                                String line = null;
                                String lineTmp = null;
                                StringBuilder sb = new StringBuilder();

                                // 무한 루프 방지를 위해 최대 읽기 횟수 제한
                                int readCount = 0;

                                while (readCount < Constants.Timeouts.MAX_READ_COUNT) {
                                    lineTmp = reader.readLine();
                                    if (lineTmp == null) {
                                        break; // 스트림 끝에 도달
                                    }
                                    readCount++;

                                    if (!lineTmp.trim().equals("")) {
                                        sb.append(lineTmp);
                                        line = lineTmp;
                                    }
                                }

                                // 모든 데이터를 읽은 후 한 번만 처리
                                final String data = (line != null && !line.trim().equals("")) ? line :
                                        (sb.length() > 0 ? sb.toString() : null);

                                if (data != null && !data.trim().equals("")) {
                                    ActivityModelTestProcess activity = activityRef.get();
                                    if (activity != null) {
                                        activity.getSchedulerHandler().post(new Runnable() {
                                            @Override
                                            public void run() {
                                                ActivityModelTestProcess act = activityRef.get();
                                                if (act == null) return; // Activity destroyed
                                                try {
                                                    if (data.contains(Constants.ResultStatus.RESULT_OK)) {
                                                        // logInfo(LogManager.LogCategory.PS, "Test task info updated: " + data);
                                                    } else {
                                                        // logWarn(LogManager.LogCategory.PS, "Test task update failed: " + data);
                                                    }
                                                } catch (Exception e) {
                                                    act.logError(LogManager.LogCategory.ER, "RequestTestTaskThreadAsync handle response error", e);
                                                }
                                            }
                                        });
                                    }
                                }
                            } finally {
                                try {
                                    reader.close();
                                } catch (Exception e) {
                                    ActivityModelTestProcess act = activityRef.get();
                                    if (act != null) {
                                        act.logError(LogManager.LogCategory.ER, Constants.ErrorMessages.ERROR_CLOSING_READER, e);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        ActivityModelTestProcess act = activityRef.get();
                        if (act != null) {
                            act.logError(LogManager.LogCategory.ER, Constants.ErrorMessages.HTTP_CONNECTION_ERROR, e);
                        }
                        e.printStackTrace();
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
                        act.logError(LogManager.LogCategory.ER, Constants.ErrorMessages.HTTP_REQUEST_ERROR, e);
                    }
                    e.printStackTrace();
                }
            }).start();
        } catch (Exception e) {
            ActivityModelTestProcess act = activityRef.get();
            if (act != null) {
                act.logError(LogManager.LogCategory.ER, Constants.ErrorMessages.REQUEST_TEST_TASK_THREAD_ASYNC_ERROR, e);
            }
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onPostExecute(String s) {
    }
}
