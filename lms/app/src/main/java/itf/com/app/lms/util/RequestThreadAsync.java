package itf.com.app.lms.util;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

import itf.com.app.lms.ActivityModelTestProcess;

class RequestThreadAsync extends AsyncTask<String, Void, String> {
    private final WeakReference<ActivityModelTestProcess> activityRef;
    private final String url;

    RequestThreadAsync(ActivityModelTestProcess activity) {
        this.activityRef = new WeakReference<>(activity);
        this.url = activity.getUrlStr();
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

                    connection.setReadTimeout(Constants.Timeouts.HTTP_READ_TIMEOUT_LONG_MS);
                    connection.setConnectTimeout(Constants.Timeouts.HTTP_CONNECT_TIMEOUT_LONG_MS);
                    connection.setRequestMethod(Constants.HTTP.METHOD_POST);  // POST 방식으로 요청
                    connection.setDoInput(true);  // InputStream 으로 서버로부터 응답을 받겠다는 옵션
                    connection.setDoOutput(true); // OutputStream 으로 POST 데이터를 넘겨 주겠다는 옵션
                    connection.setRequestProperty(Constants.HTTP.HEADER_CONTENT_TYPE, Constants.HTTP.CONTENT_TYPE_JSON);    // application/json 형식으로 전송. (Request body 전달시 application/json 으로 서버에 전달)
                    connection.setRequestProperty(Constants.HTTP.HEADER_CACHE_CONTROL, Constants.HTTP.CACHE_CONTROL_NO_CACHE);

                    try {
                        int retCode = connection.getResponseCode();
                        ActivityModelTestProcess activity = activityRef.get();
                        if (activity != null) {
                            activity.logInfo(LogManager.LogCategory.PS, Constants.LogMessages.HTTP_RESPONSE_CODE + retCode);
                        }
                        if (retCode == HttpURLConnection.HTTP_OK) {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                            // BufferedReader() : 엔터만 경계로 인식하고 받은 데이터를 String 으로 고정, Scanner 에 비해 빠름!
                            // InputStreamReader() : 지정된 문자 집합 내의 문자로 인코딩
                            // getInputStream() : url 에서 데이터를 읽어옴
                            try {
                                String line = null;
                                String lineTmp = null;
                                StringBuilder sb = new StringBuilder();

                                // 무한 루프 방지를 위해 최대 읽기 횟수 제한
                                int maxReadCount = Constants.Timeouts.MAX_READ_COUNT;
                                int readCount = 0;

                                while (readCount < maxReadCount) {
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
                                    final String finalData = data;

                                    // JSON 파싱을 백그라운드에서 직접 실행하여 메인 스레드 부하 최소화
                                    // 디바운싱을 위해 지연 실행
                                    ActivityModelTestProcess act = activityRef.get();
                                    if (act != null) {
                                        synchronized (ActivityModelTestProcess.HTTP_HANDLER_LOCK) {
                                            // 이전 작업이 아직 실행되지 않았다면 취소
                                            Runnable pendingTask = act.getPendingHttpTask();
                                            if (pendingTask != null) {
                                                act.getSchedulerHandler().removeCallbacks(pendingTask);
                                            }

                                            // 새 작업을 백그라운드에서 지연 실행 (디바운싱)
                                            act.setPendingHttpTask(new Runnable() {
                                                @Override
                                                public void run() {
                                                    ActivityModelTestProcess a = activityRef.get();
                                                    if (a == null) return; // Activity destroyed

                                                    // 백그라운드 스레드에서 직접 jsonParsing 호출
                                                    // jsonParsing 내부에서 이미 Thread를 생성하지만,
                                                    // 메인 스레드를 거치지 않아 부하가 줄어듭니다
                                                    try {
                                                        a.logDebug(LogManager.LogCategory.PS, Constants.LogMessages.PROCESSING_HTTP_RESPONSE_DATA);

                                                        new Thread(() -> {
                                                            try {
                                                                ActivityModelTestProcess a2 = activityRef.get();
                                                                if (a2 != null) {
                                                                    a2.logDebug(LogManager.LogCategory.PS, Constants.LogMessages.DELETING_TEST_SPEC_DATA);
                                                                    TestData.deleteTestSpecData(a2.getApplicationContext());
                                                                    a2.logDebug(LogManager.LogCategory.PS, Constants.LogMessages.TEST_SPEC_DATA_DELETED);
                                                                }
                                                            } catch (Exception e) {
                                                                Thread.currentThread().interrupt();
                                                            }
                                                        }).start();

                                                        a.jsonParsing("test_spec", finalData);
                                                    } catch (Exception e) {
                                                        a.logError(LogManager.LogCategory.ER, Constants.ErrorMessages.JSON_PARSING_ERROR, e);
                                                    } finally {
                                                        synchronized (ActivityModelTestProcess.HTTP_HANDLER_LOCK) {
                                                            ActivityModelTestProcess a3 = activityRef.get();
                                                            if (a3 != null) {
                                                                a3.setPendingHttpTask(null);
                                                            }
                                                        }
                                                    }
                                                }
                                            });

                                            // ⚠️ ANR FIX: Use Handler instead of creating Thread + Thread.sleep
                                            // 디바운스 지연 후 백그라운드에서 실행 (여러 요청이 동시에 와도 마지막 것만 처리)
                                            // OLD (WASTEFUL): new Thread(() -> { Thread.sleep(); pendingHttpTask.run(); }).start();
                                            // NEW (EFFICIENT): Use existing schedulerHandler
                                            act.getSchedulerHandler().postDelayed(() -> {
                                                ActivityModelTestProcess a = activityRef.get();
                                                if (a != null && a.getPendingHttpTask() != null) {
                                                    // Run on background thread to avoid blocking main thread
                                                    a.getBtWorkerExecutor().execute(a.getPendingHttpTask());
                                                }
                                            }, ActivityModelTestProcess.HTTP_DEBOUNCE_DELAY_MS);
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
                        }
                    } catch (Exception e) {
                        ActivityModelTestProcess act = activityRef.get();
                        if (act != null) {
                            act.logError(LogManager.LogCategory.ER, Constants.ErrorMessages.HTTP_CONNECTION_ERROR, e);
                        }
                    } finally {
                        // 리소스 정리 보장 (finally 블록에서 한 번만 disconnect)
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
                act.logError(LogManager.LogCategory.ER, Constants.ErrorMessages.REQUEST_THREAD_ASYNC_ERROR, e);
            }
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onPostExecute(String s) {
//            // doInBackground() 이후에 수행될 작업
//            // String s 파라미터는 doInBackground() 의 리턴값이다.
//            TextView tv = findViewById(R.id.getText);
//            tv.setText(s);
    }
}
