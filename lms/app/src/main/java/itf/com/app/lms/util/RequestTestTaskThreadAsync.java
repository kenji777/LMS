package itf.com.app.lms.util;

import android.os.AsyncTask;

import itf.com.app.lms.ActivityModelTestProcess;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

public class RequestTestTaskThreadAsync extends AsyncTask<String, Void, String> {
    private final WeakReference<ActivityModelTestProcess> activityRef;
    private final String url;

    public RequestTestTaskThreadAsync(ActivityModelTestProcess activity) {
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
                HttpURLConnection connection = null;
                try {
                    URL obj = new URL(url);
                    connection = (HttpURLConnection) obj.openConnection();

                    connection.setReadTimeout(Constants.Timeouts.HTTP_READ_TIMEOUT_LONG_MS);
                    connection.setConnectTimeout(Constants.Timeouts.HTTP_CONNECT_TIMEOUT_LONG_MS);
                    connection.setRequestMethod(Constants.HTTP.METHOD_GET);
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    connection.setRequestProperty(Constants.HTTP.HEADER_CONTENT_TYPE, Constants.HTTP.CONTENT_TYPE_JSON);
                    connection.setRequestProperty(Constants.HTTP.HEADER_CACHE_CONTROL, Constants.HTTP.CACHE_CONTROL_NO_CACHE);

                    try {
                        int retCode = connection.getResponseCode();
                        if (retCode == HttpURLConnection.HTTP_OK) {
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
                                    ActivityModelTestProcess activity = activityRef.get();
                                    if (activity != null) {
                                        activity.postToScheduler(new Runnable() {
                                            @Override
                                            public void run() {
                                                ActivityModelTestProcess act = activityRef.get();
                                                if (act == null) return;
                                                try {
                                                    act.logDebug(LogManager.LogCategory.PS, Constants.LogMessages.TEST_TASK_RESPONSE_DATA_RECEIVED);
                                                } catch (Exception e) {
                                                    act.logError(LogManager.LogCategory.ER, Constants.ErrorMessages.ERROR_PROCESSING_TEST_TASK_DATA, e);
                                                }
                                            }
                                        });
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
                        }
                    } catch (Exception e) {
                        ActivityModelTestProcess act = activityRef.get();
                        if (act != null) {
                            act.logError(LogManager.LogCategory.ER, Constants.ErrorMessages.REQUEST_TEST_TASK_THREAD_ASYNC_CONNECTION_ERROR, e);
                        }
                    } finally {
                        ActivityModelTestProcess act = activityRef.get();
                        if (act != null) {
                            ActivityModelTestProcess.safeDisconnectConnection(connection);
                        } else if (connection != null) {
                            try {
                                connection.disconnect();
                            } catch (Exception ignored) {
                            }
                        }
                    }
                } catch (Exception e) {
                    ActivityModelTestProcess act = activityRef.get();
                    if (act != null) {
                        act.logError(LogManager.LogCategory.ER, Constants.ErrorMessages.REQUEST_TEST_TASK_THREAD_ASYNC_ERROR, e);
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
