package itf.com.app.lms.util;

import static itf.com.app.lms.util.Constants.InitialValues.WS_PORT;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Base64;
import android.util.Log;
import android.webkit.WebView;

import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import fi.iki.elonen.NanoHTTPD;
import itf.com.app.lms.R;


public class WebServer extends NanoHTTPD {
    private static final String TAG = WebServer.class.getSimpleName();
    static public String resultResponseValues = "";
    private WebView webView = null;
    private Context context = null;
    private String serverIp = "";
    private final Object serverLifecycleLock = new Object();
    
    // 웹서버 상태 관리
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private int actualPort = WS_PORT;
    private static final int MAX_PORT_RETRY = 5; // 포트 충돌 시 최대 재시도 횟수
    private static final int PORT_RANGE_START = 8080; // 시작 포트
    private static final int PORT_RANGE_END = 8090; // 종료 포트

    // OVIO 로고 data URI 캐시 (HTML 페이지에서 사용)
    private static volatile String cachedOvioLogoDataUri = null;
    
    // 마지막 종료 신호 저장 (웹 클라이언트가 확인할 수 있도록)
    private static volatile String lastShutdownReason = null;
    
    // 제어 ON을 한 클라이언트 정보 저장 (IP + 세션 기반 인증)
    private static volatile String controlOwnerIp = null;
    private static volatile String controlOwnerSessionId = null;
    private static volatile boolean controlOwnerIsAndroidApp = false;
    private static final Object controlOwnerLock = new Object();
    
    // 동시 접속 클라이언트 추적 (IP + SessionId 조합)
    private static final Map<String, Long> activeConnections = new HashMap<>();
    private static final Object activeConnectionsLock = new Object();
    // 세션 ID 저장 (IP -> SessionId 매핑, 쿠키 기반 세션 추적용)
    private static final Map<String, String> ipToSessionIdMap = new HashMap<>();
    private static final Map<String, Long> sessionIdToLastAccess = new HashMap<>();
    // 기본값 (DB에 설정이 없을 때 사용)
    private static final long DEFAULT_CONNECTION_TIMEOUT_MS = 5 * 60 * 1000; // 5분 타임아웃
    private static final int DEFAULT_MAX_CONNECTIONS = 10; // 기본 최대 접속 수
    // 설정 ID
    private static final String SETTING_ID_CONNECTION_TIMEOUT = "WEB_CONNECTION_TIMEOUT_MS";
    private static final String SETTING_ID_MAX_CONNECTIONS = "WEB_MAX_CONNECTIONS";
    // 쿠키 이름
    private static final String SESSION_COOKIE_NAME = "WS_SESSION_ID";

    public WebServer()
    {
        super(WS_PORT);
        this.actualPort = WS_PORT;
    }

    public WebServer(Context context, String serverIp) {
        super(Constants.InitialValues.WS_PORT);
        this.context = context;
        this.serverIp = serverIp;
        this.actualPort = Constants.InitialValues.WS_PORT;
    }

    /**
     * 지정한 포트로 WebServer 생성
     * NanoHTTPD는 생성자에서 포트를 고정하므로, 포트 변경이 필요하면 새 인스턴스를 생성해야 함.
     */
    public WebServer(Context context, String serverIp, int port) {
        super(port);
        this.context = context;
        this.serverIp = serverIp;
        this.actualPort = port;
    }

    public void setWebView(WebView webView) {
        this.webView = webView;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }
    
    /**
     * 웹서버가 실행 중인지 확인
     * @return true if server is running, false otherwise
     */
    public boolean isRunning() {
        return isRunning.get() && super.wasStarted();
    }
    
    /**
     * 실제 사용 중인 포트 번호 반환
     * @return 실제 포트 번호
     */
    public int getActualPort() {
        return actualPort;
    }
    
    /**
     * 포트가 사용 가능한지 확인
     * @param port 확인할 포트 번호
     * @return true if port is available, false otherwise
     */
    private static boolean isPortAvailable(int port) {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            serverSocket.close();
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Port " + port + " is not available: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 사용 가능한 포트 찾기
     * @param preferredPort 선호하는 포트 번호
     * @return 사용 가능한 포트 번호, 없으면 -1
     */
    private static int findAvailablePort(int preferredPort) {
        // 먼저 선호하는 포트 확인
        if (isPortAvailable(preferredPort)) {
            return preferredPort;
        }
        
        // 포트 범위 내에서 사용 가능한 포트 찾기
        for (int port = PORT_RANGE_START; port <= PORT_RANGE_END; port++) {
            if (port != preferredPort && isPortAvailable(port)) {
                Log.i(TAG, "Found available port: " + port + " (preferred port " + preferredPort + " was in use)");
                return port;
            }
        }
        
        Log.e(TAG, "No available port found in range " + PORT_RANGE_START + "-" + PORT_RANGE_END);
        return -1;
    }

    /**
     * 선호 포트부터 범위 내에서 실제로 start()까지 성공하는 WebServer 인스턴스를 생성/시작
     * @return started WebServer instance, or null if failed
     */
    public static WebServer startNewServerWithPortFallback(Context context, String serverIp, int preferredPort) {
        int[] candidatePorts;
        // 선호 포트가 범위 밖일 수도 있으니, 안전하게 후보 목록 생성
        if (preferredPort >= PORT_RANGE_START && preferredPort <= PORT_RANGE_END) {
            int size = (PORT_RANGE_END - PORT_RANGE_START + 1);
            candidatePorts = new int[size];
            int idx = 0;
            candidatePorts[idx++] = preferredPort;
            for (int port = PORT_RANGE_START; port <= PORT_RANGE_END; port++) {
                if (port != preferredPort) {
                    candidatePorts[idx++] = port;
                }
            }
        } else {
            int size = (PORT_RANGE_END - PORT_RANGE_START + 1);
            candidatePorts = new int[size];
            int idx = 0;
            for (int port = PORT_RANGE_START; port <= PORT_RANGE_END; port++) {
                candidatePorts[idx++] = port;
            }
        }

        for (int port : candidatePorts) {
            // 빠른 사전 체크 (레이스가 있을 수 있으니 start() 실패는 허용)
            if (!isPortAvailable(port)) {
                continue;
            }

            WebServer server = new WebServer(context, serverIp, port);
            boolean started = server.startServer();
            if (started) {
                return server;
            }
        }

        return null;
    }
    
    /**
     * 웹서버 시작 (포트 충돌 처리 포함)
     * @return true if started successfully, false otherwise
     */
    public boolean startServer() {
        synchronized (serverLifecycleLock) {
            if (isRunning.get()) {
                Log.w(TAG, "WebServer is already running on port " + actualPort);
                return true;
            }

            // 네트워크 상태 확인
            if (context != null && !isNetworkAvailable()) {
                Log.w(TAG, "Network is not available, but starting server anyway for local access");
            }

            // 웹서버 설정 초기화 (DB에 설정이 없으면 추가)
            if (context != null) {
                initializeWebServerSettings();
            }

            try {
                // NOTE:
                // - 현재 프로젝트에 포함된 NanoHTTPD 버전은 start() 오버로드(timeout/daemon)와
                //   setReuseAddr(true) API가 없습니다.
                // - 따라서 여기서는 기본 start()만 사용합니다.
                // - 포트 충돌/재시작 문제는 상위(KioskModeApplication)에서 인스턴스 재생성 + 포트 폴백으로 해결합니다.
                super.start();
                isRunning.set(true);
                Log.i(TAG, "WebServer started successfully on port " + actualPort);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Failed to start WebServer on port " + actualPort, e);
                isRunning.set(false);
                return false;
            }
        }
    }
    
    /**
     * 웹서버 중지
     * @return true if stopped successfully, false otherwise
     */
    public boolean stopServer() {
        if (!isRunning.get()) {
            Log.w(TAG, "WebServer is not running");
            return true;
        }
        
        try {
            super.stop();
            isRunning.set(false);
            Log.i(TAG, "WebServer stopped successfully");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop WebServer", e);
            isRunning.set(false);
            return false;
        }
    }
    
    /**
     * 웹서버 재시작
     * @return true if restarted successfully, false otherwise
     */
    public boolean restartServer() {
        Log.i(TAG, "Restarting WebServer...");
        stopServer();
        try {
            Thread.sleep(500); // 잠시 대기 후 재시작
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return startServer();
    }
    
    /**
     * 네트워크 연결 상태 확인
     * @return true if network is available, false otherwise
     */
    private boolean isNetworkAvailable() {
        if (context == null) {
            return false;
        }
        
        try {
            ConnectivityManager connectivityManager = 
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager == null) {
                return false;
            }
            
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        } catch (Exception e) {
            Log.w(TAG, "Error checking network availability", e);
            return false;
        }
    }
    
    /**
     * 웹서버 상태 정보 반환 (디버깅용)
     * @return 상태 정보 문자열
     */
    public String getStatusInfo() {
        return String.format("WebServer Status - Running: %s, Port: %d, Context: %s, ServerIp: %s",
            isRunning.get(), actualPort, 
            context != null ? "Set" : "Null",
            serverIp != null && !serverIp.isEmpty() ? serverIp : "Not Set");
    }
    
    /**
     * 현재 접속 중인 사용자 수 반환
     * 타임아웃된 연결은 자동으로 제거됩니다.
     * @return 현재 접속 중인 사용자 수
     */
    public static int getActiveConnectionCount() {
        synchronized (activeConnectionsLock) {
            // 타임아웃된 연결 제거
            long currentTime = System.currentTimeMillis();
            long connectionTimeout = DEFAULT_CONNECTION_TIMEOUT_MS;
            // Context가 있는 경우 DB에서 설정값 가져오기 (정적 메서드이므로 직접 접근 불가)
            // 타임아웃 체크는 간단하게 기본값 사용
            activeConnections.entrySet().removeIf(entry -> 
                currentTime - entry.getValue() > connectionTimeout);
            
            return activeConnections.size();
        }
    }
    
    /**
     * 현재 접속 중인 사용자 수 반환 (인스턴스 메서드)
     * DB 설정값을 사용하여 타임아웃된 연결을 정확하게 제거합니다.
     * @return 현재 접속 중인 사용자 수
     */
    public int getActiveConnectionCountWithTimeout() {
        synchronized (activeConnectionsLock) {
            // 타임아웃된 연결 제거
            long currentTime = System.currentTimeMillis();
            long connectionTimeout = getConnectionTimeoutMs();
            activeConnections.entrySet().removeIf(entry -> 
                currentTime - entry.getValue() > connectionTimeout);
            
            // 타임아웃된 세션 ID 제거
            sessionIdToLastAccess.entrySet().removeIf(entry -> 
                currentTime - entry.getValue() > connectionTimeout);
            
            return activeConnections.size();
        }
    }
    
    /**
     * 최대 접속 가능한 사용자 수 반환
     * @return 최대 접속 가능한 사용자 수
     */
    public int getMaxConnectionCount() {
        return getMaxConnections();
    }

    @Override
    public Response serve(IHTTPSession session) {
        // 접속 제한 확인 (정적 파일 제외)
        String uri = session.getUri();
        if (uri != null && !uri.startsWith("/static/") && !uri.equals("/favicon.ico")) {
            if (!checkAndRegisterConnection(session)) {
                // 접속 제한 초과
                JSONObject errorJson = new JSONObject();
                try {
                    errorJson.put("success", false);
                    errorJson.put("message", "동시 접속 가능한 사용자 수를 초과했습니다. 잠시 후 다시 시도해주세요.");
                    errorJson.put("error_code", "MAX_CONNECTIONS_EXCEEDED");
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to create error JSON", e);
                }
                return newResponse(Response.Status.INTERNAL_ERROR, Constants.HTTP.CONTENT_TYPE_JSON, errorJson.toString());
            }
        }
        
        String method = session.getMethod().name();

        // 일부 브라우저/환경에서 trailing slash가 붙는 경우가 있어 라우팅을 안정화
        if (uri != null && uri.length() > 1 && uri.endsWith("/")) {
            uri = uri.substring(0, uri.length() - 1);
        }

        // /status 엔드포인트 처리 - 테스트 상태 조회
        if ("/status".equals(uri) && "GET".equals(method)) {
            Response response = handleTestStatus(session);
            return addSessionCookieToResponse(response, session);
        } else if ("/status/json".equals(uri) && "GET".equals(method)) {
            Response response = handleTestStatusJson(session);
            return addSessionCookieToResponse(response, session);
        }

        // /test_history 엔드포인트 처리 - 검사 이력 목록(웹)
        if ("/test_history".equals(uri) && "GET".equals(method)) {
            Log.i(TAG, "Web request: " + method + " " + uri);
            Response response = handleTestHistoryListPage(session);
            return addSessionCookieToResponse(response, session);
        } else if ("/test_history/detail".equals(uri) && "GET".equals(method)) {
            Log.i(TAG, "Web request: " + method + " " + uri + " parms=" + (session != null ? session.getParms() : "null"));
            Response response = handleTestHistoryDetailPage(session);
            return addSessionCookieToResponse(response, session);
        }
        
        // /test_item/execute 엔드포인트 처리 - 테스트 항목 실행
        if ("/test_item/execute".equals(uri) && "POST".equals(method)) {
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> execute.sessionId " + session.getParms());
            Response response = handleTestItemExecute(session);
            return addSessionCookieToResponse(response, session);
        }
        
        // /test/restart 엔드포인트 처리 - 재검사 (finishApplication 호출)
        if ("/test/restart".equals(uri) && "POST".equals(method)) {
            try {
                Response response = handleTestRestart(session);
                return addSessionCookieToResponse(response, session);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
        
        // /test/control/toggle 엔드포인트 처리 - 제어 버튼 (타이머 시작/중지)
        if ("/test/control/toggle".equals(uri) && "POST".equals(method)) {
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> toggle.sessionId " + session.getParms());
            Response response = handleTestControlToggle(session);
            return addSessionCookieToResponse(response, session);
        }
        
        // /test/control/start 엔드포인트 처리 - 제어 시작 신호 (제어 요청 접근 메시지 표시)
        if ("/test/control/start".equals(uri) && "POST".equals(method)) {
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>  start.sessionId " + session.getParms());
            Response response = handleTestControlStart(session);
            return addSessionCookieToResponse(response, session);
        }
        
        // /test/control/end 엔드포인트 처리 - 제어 종료 신호 (제어 요청 접근 메시지 삭제)
        if ("/test/control/end".equals(uri) && "POST".equals(method)) {
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>    end.sessionId " + session.getParms());
            Response response = handleTestControlEnd(session);
            return addSessionCookieToResponse(response, session);
        }
        
        // /test/shutdown/notify 엔드포인트 처리 - 애플리케이션 종료 신호 (웹 클라이언트에 알림)
        if ("/test/shutdown/notify".equals(uri) && "POST".equals(method)) {
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>    notify.sessionId " + session.getParms());
            Response response = handleShutdownNotify(session);
            return addSessionCookieToResponse(response, session);
        }
        
        // /test/control/owner 엔드포인트 처리 - 제어 소유자 정보 (안드로이드 앱에서 제어 ON/OFF 시)
        if ("/test/control/owner".equals(uri) && "POST".equals(method)) {
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>    owner.sessionId " + session.getParms());
            Response response = handleControlOwner(session);
            return addSessionCookieToResponse(response, session);
        }
        
        // /client_id 엔드포인트 처리
        if ("/client_id".equals(uri)) {
            if ("GET".equals(method)) {
                Response response = createClientIdForm();
                return addSessionCookieToResponse(response, session);
            } else if ("POST".equals(method)) {
                Response response = handleClientIdPage(session);
                return addSessionCookieToResponse(response, session);
            }
        } else if ("/client_id/lookup".equals(uri) && "GET".equals(method)) {
            Response response = handleClientIdLookup(session);
            return addSessionCookieToResponse(response, session);
        }

        String answer = "";
        String responseBody = "";

        try {
//             Log.i(TAG, "> [SI] request file info >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> " + ipAddress);
//             Log.i(TAG, "> [SI] request file info >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> " + session.getParms());
//             Log.i(TAG, "> [SI] btSearchOnOff >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> " + btSearchOnOff);
//            if(!btSearchOnOff) {
//                if (session != null) {
//                    if (!session.getUri().equals("") && !session.getParms().equals("")) {
//                        // Open file from SD Card
//                        Log.i(TAG, "> [SI] request file info " + session.getUri() + " " + session.getParms());
//                        Log.i(TAG, "> [SI] bt connection handler search ON start");
//
//                        if(session.getParms().get("clm_request_type").equals("V")) {
//                            responseBody = resultResponseValues;
//                        }
//
//                        if(session.getParms().get("clm_call_type").equals("R")) {
//                            try {
//                            }
//                            catch (Exception e) {
//                                Log.d(TAG, "> [ER].00049 " + e);
//                            }
//                        }
//                        else if(session.getParms().get("clm_call_type").equals("B")) {
//                            try {
//                            }
//                            catch (Exception e) {
//                                Log.d(TAG, "> [ER].00049 " + e);
//                            }
//                        }
//                        else
//                        {
//                            try {
//                                if(tmrAppReset!=null) {
//                                    tmrAppReset.cancel();
//                                }
//                            }
//                            catch (Exception e) {
//                                Log.d(TAG, "> [ER].00041 " + e);
//                                clAlert.setVisibility(View.VISIBLE);
//                                tvAlertMessage.setText("서버 연결 실패");
//                                // return;
//                            }
//
//                            btSearchOnOff = true;
//                        }
//                    }
//                }
//                else
//                {
//                    if (session != null) {
//                        if (session.getParms().get("clm_request_type").equals("V")) {
//                            responseBody = resultResponseValues;
//                        }
//                    }
//
//                    try {
//                        if(tmrAppReset!=null) {
//                            tmrAppReset.cancel();
//                        }
//                    }
//                    catch (Exception e) {
//                        Log.d(TAG, "> [ER].00045 " + e);
//                        clAlert.setVisibility(View.VISIBLE);
//                        tvAlertMessage.setText("서버 연결 실패");
//                        // return;
//                    }
//
//                    btSearchOnOff = true;
//                }
//            }
//            else {
//
//            }
            /*
            File root = Environment.getExternalStorageDirectory();
            FileReader index = new FileReader("/data/data/itf.com.app.simple_line_test_ovio/www/index.html");
            BufferedReader reader = new BufferedReader(index);
            String line = "";

            btSearchOnOff = true;

            while ((line = reader.readLine()) != null) {
                answer += line;
            }
            reader.close();
            */
        }
        // catch(IOException ioe) {
        //     Log.d(TAG, "> [ER].00A01 : " + ioe);
        // }
        catch(Exception e) {
            Log.d(TAG, "> [ER].00A02 : " + e);
        }

        return new NanoHTTPD.Response(responseBody);
    }

    /**
     * 클라이언트 ID 등록 폼 HTML 생성
     */
    private Response createClientIdForm() {
        String html = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <link rel=\"stylesheet\" href=\"https://cdn.jsdelivr.net/gh/moonspam/NanumSquare@2.0/nanumsquare.css\">\n" +
                "    <title>고객 ID 등록</title>\n" +
                "    <style>\n" +
                "        * { font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; }\n" +
                "        body { font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; padding: 20px; background-color: #f5f5f5; }\n" +
                "        .container { max-width: 500px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; }\n" +
                "        h1 { text-align: center; color: #333; margin-bottom: 30px; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; }\n" +
                "        .form-group { margin-bottom: 20px; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; }\n" +
                "        label { display: block; margin-bottom: 5px; color: #555; font-weight: bold; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; }\n" +
                "        input[type=\"text\"] { width: 100%; padding: 12px; border: 1px solid #ddd; border-radius: 5px; font-size: 16px; box-sizing: border-box; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; }\n" +
                "        .client-id-info { margin-top: 15px; padding: 15px; background-color: #e8f5e9; border-radius: 5px; display: none; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; }\n" +
                "        .client-id-info.show { display: block; }\n" +
                "        .client-id-label { font-weight: bold; color: #2e7d32; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; }\n" +
                "        .client-id-value { color: #1b5e20; font-size: 18px; margin-top: 5px; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; }\n" +
                "        button { width: 100%; padding: 12px; background-color: #2196F3; color: white; border: none; border-radius: 5px; font-size: 16px; cursor: pointer; margin-top: 10px; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; }\n" +
                "        button:disabled { background-color: #ccc; cursor: not-allowed; }\n" +
                "        button:hover:not(:disabled) { background-color: #1976D2; }\n" +
                "        .error { color: red; margin-top: 10px; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"container\">\n" +
                "        <h1>고객 ID 등록</h1>\n" +
                "        <form id=\"clientIdForm\">\n" +
                "            <div class=\"form-group\">\n" +
                "                <label for=\"business_number\">사업자 번호</label>\n" +
                "                <input type=\"text\" id=\"business_number\" name=\"business_number\" placeholder=\"예: 123-45-67890\" autofocus required>\n" +
                "            </div>\n" +
                "            <div class=\"client-id-info\" id=\"client_id_info\">\n" +
                "                <div class=\"client-id-label\">고객 ID:</div>\n" +
                "                <div class=\"client-id-value\" id=\"client_id_value\"></div>\n" +
                "            </div>\n" +
                "            <button type=\"submit\" id=\"submitBtn\" disabled>등록</button>\n" +
                "            <div class=\"error\" id=\"errorMsg\"></div>\n" +
                "        </form>\n" +
                "    </div>\n" +
                "    <script>\n" +
                "        const businessNumberInput = document.getElementById('business_number');\n" +
                "        const clientIdInfo = document.getElementById('client_id_info');\n" +
                "        const clientIdValue = document.getElementById('client_id_value');\n" +
                "        const submitBtn = document.getElementById('submitBtn');\n" +
                "        const errorMsg = document.getElementById('errorMsg');\n" +
                "        let lookupTimeout = null;\n" +
                "\n" +
                "        // 사업자 번호 포맷팅 함수\n" +
                "        function formatBusinessNumber(value) {\n" +
                "            const numbers = value.replace(/[^0-9]/g, '');\n" +
                "            if (numbers.length <= 3) return numbers;\n" +
                "            if (numbers.length <= 5) return numbers.slice(0, 3) + '-' + numbers.slice(3);\n" +
                "            return numbers.slice(0, 3) + '-' + numbers.slice(3, 5) + '-' + numbers.slice(5, 10);\n" +
                "        }\n" +
                "\n" +
                "        // 입력 이벤트 처리\n" +
                "        businessNumberInput.addEventListener('input', function(e) {\n" +
                "            const cursorPos = this.selectionStart;\n" +
                "            const oldValue = this.value;\n" +
                "            const newValue = formatBusinessNumber(this.value);\n" +
                "            \n" +
                "            if (oldValue !== newValue) {\n" +
                "                this.value = newValue;\n" +
                "                const diff = newValue.length - oldValue.length;\n" +
                "                this.setSelectionRange(cursorPos + diff, cursorPos + diff);\n" +
                "            }\n" +
                "\n" +
                "            // 디바운싱된 AJAX 호출\n" +
                "            clearTimeout(lookupTimeout);\n" +
                "            const businessNumberOnly = newValue.replace(/[^0-9]/g, '');\n" +
                "            \n" +
                "            if (businessNumberOnly.length >= 10) {\n" +
                "                lookupTimeout = setTimeout(() => {\n" +
                "                    lookupClientId(businessNumberOnly);\n" +
                "                }, 500);\n" +
                "            } else {\n" +
                "                clientIdInfo.classList.remove('show');\n" +
                "                submitBtn.disabled = true;\n" +
                "                errorMsg.textContent = '';\n" +
                "            }\n" +
                "        });\n" +
                "\n" +
                "        // AJAX로 클라이언트 ID 조회\n" +
                "        function lookupClientId(businessNumber) {\n" +
                "            errorMsg.textContent = '';\n" +
                "            fetch('/client_id/lookup?business_number=' + encodeURIComponent(businessNumber))\n" +
                "                .then(response => response.json())\n" +
                "                .then(data => {\n" +
                "                    if (data.success && data.client_id) {\n" +
                "                        clientIdValue.textContent = data.client_id;\n" +
                "                        clientIdInfo.classList.add('show');\n" +
                "                        submitBtn.disabled = false;\n" +
                "                    } else {\n" +
                "                        clientIdInfo.classList.remove('show');\n" +
                "                        submitBtn.disabled = true;\n" +
                "                        errorMsg.textContent = data.message || '해당 사업자 번호로 고객 ID를 찾을 수 없습니다.';\n" +
                "                    }\n" +
                "                })\n" +
                "                .catch(error => {\n" +
                "                    console.error('Error:', error);\n" +
                "                    clientIdInfo.classList.remove('show');\n" +
                "                    submitBtn.disabled = true;\n" +
                "                    errorMsg.textContent = '조회 중 오류가 발생했습니다.';\n" +
                "                });\n" +
                "        }\n" +
                "\n" +
                "        // 폼 제출 처리\n" +
                "        document.getElementById('clientIdForm').addEventListener('submit', function(e) {\n" +
                "            e.preventDefault();\n" +
                "            const businessNumber = businessNumberInput.value;\n" +
                "            \n" +
                "            const formData = new FormData();\n" +
                "            formData.append('business_number', businessNumber);\n" +
                "            \n" +
                "            fetch('/client_id', {\n" +
                "                method: 'POST',\n" +
                "                body: formData\n" +
                "            })\n" +
                "            .then(response => response.text())\n" +
                "            .then(data => {\n" +
                "                alert('고객 ID가 등록되었습니다.');\n" +
                "                location.reload();\n" +
                "            })\n" +
                "            .catch(error => {\n" +
                "                console.error('Error:', error);\n" +
                "                errorMsg.textContent = '등록 중 오류가 발생했습니다.';\n" +
                "            });\n" +
                "        });\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";

        return newResponse(Response.Status.OK, NanoHTTPD.MIME_HTML, html);
    }

    /**
     * 클라이언트 ID 등록 처리 (POST)
     */
    private Response handleClientIdPage(IHTTPSession session) {
        try {
            if (context == null) {
                Log.e(TAG, "Context가 null입니다. WebServer 생성 시 Context를 전달해야 합니다.");
                return newResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "서버 초기화 오류");
            }

            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            String businessNumber = session.getParms().get("business_number");

            if (businessNumber == null || businessNumber.trim().isEmpty()) {
                return newResponse(Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT, "사업자 번호가 필요합니다.");
            }

            // 사업자 번호 포맷팅 (하이픈 포함)
            String businessNumberFormatted = formatBusinessNumber(businessNumber);
            
            // 서버에서 클라이언트 ID 조회
            String clientId = fetchClientIdFromServer(businessNumberFormatted);
            if (clientId == null || clientId.isEmpty()) {
                return newResponse(Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT, "해당 사업자 번호로 고객 ID를 찾을 수 없습니다.");
            }

            // DB에 저장
            Map<String, String> settingData = new HashMap<>();
            settingData.put("clm_setting_seq", "000000");
            settingData.put("clm_setting_id", "CLIENT_ID");
            settingData.put("clm_setting_value", clientId);
            settingData.put("clm_comment", "고객 ID");
            settingData.put("clm_test_timestamp", getCurrentDatetime(Constants.DateTimeFormats.TIMESTAMP_FORMAT));

            boolean success = TestData.insertSettingInfo(context, settingData);
            if (success) {
                Log.i(TAG, "클라이언트 ID 등록 성공: " + clientId);
                return newResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, "고객 ID가 등록되었습니다.");
            } else {
                return newResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "데이터베이스 저장 실패");
            }
        } catch (Exception e) {
            Log.e(TAG, "클라이언트 ID 등록 처리 오류", e);
            return newResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 클라이언트 ID 조회 (AJAX - GET)
     */
    private Response handleClientIdLookup(IHTTPSession session) {
        try {
            String businessNumber = session.getParms().get("business_number");
            if (businessNumber == null || businessNumber.trim().isEmpty()) {
                return newResponse(Response.Status.BAD_REQUEST, Constants.HTTP.CONTENT_TYPE_JSON, 
                    "{\"success\": false, \"message\": \"사업자 번호가 필요합니다.\"}");
            }

            // JavaScript에서 하이픈이 제거된 값을 보내므로, 하이픈을 다시 추가하여 서버에 전송
            String businessNumberFormatted = formatBusinessNumber(businessNumber);
            
            // 서버에서 클라이언트 ID 조회
            String clientId = fetchClientIdFromServer(businessNumberFormatted);
            
            if (clientId != null && !clientId.isEmpty()) {
                return newResponse(Response.Status.OK, Constants.HTTP.CONTENT_TYPE_JSON, 
                    "{\"success\": true, \"client_id\": \"" + clientId + "\"}");
            } else {
                return newResponse(Response.Status.OK, Constants.HTTP.CONTENT_TYPE_JSON, 
                    "{\"success\": false, \"message\": \"해당 사업자 번호로 고객 ID를 찾을 수 없습니다.\"}");
            }
        } catch (Exception e) {
            Log.e(TAG, "클라이언트 ID 조회 오류", e);
            return newResponse(Response.Status.INTERNAL_ERROR, Constants.HTTP.CONTENT_TYPE_JSON, 
                "{\"success\": false, \"message\": \"조회 중 오류가 발생했습니다.\"}");
        }
    }

    /**
     * 사업자 번호 포맷팅 (999-99-99999 형식)
     */
    private String formatBusinessNumber(String businessNumber) {
        if (businessNumber == null) {
            return "";
        }
        // 숫자만 추출
        String numbers = businessNumber.replaceAll("[^0-9]", "");
        if (numbers.length() <= 3) {
            return numbers;
        } else if (numbers.length() <= 5) {
            return numbers.substring(0, 3) + "-" + numbers.substring(3);
        } else {
            return numbers.substring(0, 3) + "-" + numbers.substring(3, 5) + "-" + numbers.substring(5, Math.min(10, numbers.length()));
        }
    }

    /**
     * 서버에서 클라이언트 ID 조회
     */
    private String fetchClientIdFromServer(String businessNumber) {
        try {
            if (serverIp == null || serverIp.isEmpty()) {
                Log.e(TAG, "serverIp가 설정되지 않았습니다.");
                return null;
            }

            // 하이픈 포함하여 서버에 전송
            String urlStr = Constants.URLs.HTTP_PROTOCOL + serverIp + 
                           Constants.URLs.ENDPOINT_APPLICATION_CLIENT_INFO_LIST +
                           "?clm_business_serial_no=" + java.net.URLEncoder.encode(businessNumber, "UTF-8");
            
            Log.i(TAG, "ApplicationClientInfoList.jsp 호출: " + urlStr);
            
            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setReadTimeout(Constants.Timeouts.HTTP_READ_TIMEOUT_MS);
            connection.setConnectTimeout(Constants.Timeouts.HTTP_CONNECT_TIMEOUT_MS);
            connection.setRequestMethod(Constants.HTTP.METHOD_GET);
            connection.setDoInput(true);
            
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        sb.append(line);
                    }
                }
                
                String jsonResponse = sb.toString();
                Log.i(TAG, "ApplicationClientInfoList 응답: " + jsonResponse);
                
                // JSON 파싱
                JSONObject jsonObject = new JSONObject(jsonResponse);
                JSONArray jsonArray = null;
                
                if (jsonObject.has(Constants.JsonKeys.APPLICATION_CLIENT_INFO)) {
                    jsonArray = jsonObject.getJSONArray(Constants.JsonKeys.APPLICATION_CLIENT_INFO);
                }
                
                if (jsonArray != null && jsonArray.length() > 0) {
                    JSONObject firstItem = jsonArray.getJSONObject(0);
                    String clientId = firstItem.optString(Constants.JsonKeys.CLM_CLIENT_ID, "");
                    
                    if (!clientId.isEmpty()) {
                        Log.i(TAG, "고객 ID 추출 성공: " + clientId);
                        // 키보드 숨기기
                        hideKeyboard();
                        return clientId;
                    }
                }
                
                Log.w(TAG, "고객 ID를 찾을 수 없습니다.");
                return null;
                }
            } else {
                Log.e(TAG, "HTTP 응답 코드: " + responseCode);
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "서버에서 클라이언트 ID 조회 실패", e);
            return null;
        }
    }

    /**
     * 키보드 숨기기
     */
    private void hideKeyboard() {
        if (webView != null) {
            webView.post(() -> {
                webView.evaluateJavascript("document.getElementById('business_number')?.blur();", null);
            });
        }
    }

    /**
     * 현재 날짜/시간 반환
     */
    private String getCurrentDatetime(String dateformat) {
        SimpleDateFormat dateFormmater = new SimpleDateFormat(dateformat);
        return dateFormmater.format(new Date());
    }

    /**
     * Response 생성 헬퍼 메서드
     */
    private Response newResponse(Response.Status status, String mimeType, String txt) {
        Response response = new Response(status, mimeType, txt);
        response.addHeader("Access-Control-Allow-Origin", "*");
        return response;
    }
    
    /**
     * 응답에 세션 쿠키 추가
     * @param response HTTP 응답
     * @param session HTTP 세션 (쿠키에서 세션 ID 추출용)
     * @return 쿠키가 추가된 응답
     */
    private Response addSessionCookieToResponse(Response response, IHTTPSession session) {
        if (response == null || session == null) {
            return response;
        }
        
        try {
            // 클라이언트 ID 생성
            String clientId = generateClientSessionId(session, null);
            String sessionId = extractSessionIdFromClientId(clientId);
            
            if (sessionId != null) {
                // 쿠키 설정 (30일 유효)
                String cookieValue = SESSION_COOKIE_NAME + "=" + sessionId + "; Path=/; Max-Age=" + (30 * 24 * 60 * 60) + "; HttpOnly";
                response.addHeader("Set-Cookie", cookieValue);
                // Log.d(TAG, "[쿠키] 응답에 쿠키 추가: " + SESSION_COOKIE_NAME + "=" + sessionId);
            } else {
                Log.w(TAG, "[쿠키] 세션 ID를 추출할 수 없어 쿠키를 설정하지 않음. clientId: " + clientId);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to add session cookie to response: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 테스트 상태 정보를 담는 내부 클래스
     */
    private static class TestStatusInfo {
        String modelId = "";
        String modelName = "";
        String currentTestItem = "";
        int testItemIdx = 0;
        int totalTestItems = 0;
        int testItemCounter = 0;
        int testTotalCounter = 0;
        String testResult = "";
        boolean isTestRunning = false;
        java.util.List<TestItemInfo> testItemList = new java.util.ArrayList<>();
        
        // 상단 정보
        String unitId = "";  // 단위 ID
        String temperature = "0";
        String wattValue = "0";
        int totalTimeCnt = 0;  // 설정시간 (초)
        int spentDuration = 0;  // 진행시간 (초)
        
        // 램프 상태 (true = 연결됨/정상, false = 연결 안됨/비정상)
        boolean rampSvr = false;  // 웹서버
        boolean rampBt = false;  // 블루투스
        boolean rampUsb = false;  // USB/PLC
        
        // 제어 모드 상태
        boolean isControlMode = false;  // 제어 모드 활성화 여부
        boolean isControlOn = false;   // 제어 ON/OFF 상태
        
        // 중간 섹션 데이터 (기준값, 상한값, 하한값, 측정값)
        String compValueWatt = "0";  // COMP 기준값
        String heaterValueWatt = "0";  // 히터 기준값
        String pumpValueWatt = "0";  // 펌프 기준값
        String compUpperValueWatt = "0";  // COMP 상한값
        String heaterUpperValueWatt = "0";  // 히터 상한값
        String pumpUpperValueWatt = "0";  // 펌프 상한값
        String compLowerValueWatt = "0";  // COMP 하한값
        String heaterLowerValueWatt = "0";  // 히터 하한값
        String pumpLowerValueWatt = "0";  // 펌프 하한값
        String compWattValue = "0";  // COMP 측정값
        String heaterWattValue = "0";  // 히터 측정값
        String pumpWattValue = "0";  // 펌프 측정값
        
        static class TestItemInfo {
            String seq;
            String name;
            String command;
            String result;
            String finishYn;
            String resultValue;
            
            TestItemInfo(String seq, String name, String command, String result, String finishYn, String resultValue) {
                this.seq = seq != null ? seq : "";
                this.name = name != null ? name : "";
                this.command = command != null ? command : "";
                this.result = result != null ? result : "";
                this.finishYn = finishYn != null ? finishYn : "";
                this.resultValue = resultValue != null ? resultValue : "";
            }
        }
    }
    
    /**
     * 현재 테스트 상태 정보 가져오기
     */
    private TestStatusInfo getTestStatusInfo() {
        TestStatusInfo status = new TestStatusInfo();
        
        try {
            // ActivityModel_0002에서 현재 Activity 인스턴스 가져오기
            Object activity = null;

            try {
                Class<?> activityModelClass = Class.forName("itf.com.app.lms.ActivityModelTestProcess");
                java.lang.reflect.Method getMainActivityMethod = activityModelClass.getMethod("getMainActivity");
                activity = getMainActivityMethod.invoke(null);
            } catch (Exception e) {
                Log.d(TAG, "Could not get Activity instance: " + e.getMessage());
            }
            /*
            try {
                Class<?> activityModelClass = Class.forName("itf.com.app.simple_line_test_ovio_new.ActivityModel_0002");
                java.lang.reflect.Method getMainActivityMethod = activityModelClass.getMethod("getMainActivity");
                activity = getMainActivityMethod.invoke(null);
            } catch (Exception e) {
                // ActivityModel_0002가 없으면 ActivityModel_0001 시도
                try {
                    Class<?> activityModelClass = Class.forName("itf.com.app.simple_line_test_ovio_new.ActivityModel_0001");
                    java.lang.reflect.Method getMainActivityMethod = activityModelClass.getMethod("getMainActivity");
                    activity = getMainActivityMethod.invoke(null);
                } catch (Exception e2) {
                    // ActivityModel_0003 시도
                    try {
                        Class<?> activityModelClass = Class.forName("itf.com.app.simple_line_test_ovio_new.ActivityModel_0003");
                        java.lang.reflect.Method getMainActivityMethod = activityModelClass.getMethod("getMainActivity");
                        activity = getMainActivityMethod.invoke(null);
                    } catch (Exception e3) {
                        Log.d(TAG, "Could not get Activity instance: " + e3.getMessage());
                    }
                }
            }
            */
            
            if (activity != null) {
                // globalModelId, globalModelName 가져오기
                try {
                    Class<?> activityModelListClass = Class.forName("itf.com.app.lms.ActivityModelList");
                    java.lang.reflect.Field globalModelIdField = activityModelListClass.getField("globalModelId");
                    java.lang.reflect.Field globalModelNameField = activityModelListClass.getField("globalModelName");
                    
                    Object modelIdObj = globalModelIdField.get(null);
                    Object modelNameObj = globalModelNameField.get(null);
                    
                    status.modelId = modelIdObj != null ? modelIdObj.toString() : "";
                    status.modelName = modelNameObj != null ? modelNameObj.toString() : "";
                } catch (Exception e) {
                    Log.d(TAG, "Could not get model info: " + e.getMessage());
                }
                
                // 단위 ID 가져오기
                try {
                    java.lang.reflect.Field tvUnitIdField = activity.getClass().getDeclaredField("tvUnitId");
                    tvUnitIdField.setAccessible(true);
                    Object tvUnitIdObj = tvUnitIdField.get(activity);
                    if (tvUnitIdObj != null) {
                        java.lang.reflect.Method getTextMethod = tvUnitIdObj.getClass().getMethod("getText");
                        Object textObj = getTextMethod.invoke(tvUnitIdObj);
                        if (textObj != null) {
                            status.unitId = textObj.toString().trim();
                        }
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Could not get unit ID: " + e.getMessage());
                }
                
                // 현재 테스트 정보 가져오기 (리플렉션 사용)
                // ActivityModel 인스턴스에서만 정보 가져오기 (MainActivity 사용 안 함)
                try {
                    // Activity 인스턴스에서 테스트 정보 가져오기
                    java.lang.reflect.Field currentTestItemField = activity.getClass().getDeclaredField("currentTestItem");
                    currentTestItemField.setAccessible(true);
                    Object currentTestItemObj = currentTestItemField.get(activity);
                    status.currentTestItem = currentTestItemObj != null ? currentTestItemObj.toString() : "";
                    
                    java.lang.reflect.Field testItemIdxField = activity.getClass().getDeclaredField("testItemIdx");
                    testItemIdxField.setAccessible(true);
                    Object testItemIdxObj = testItemIdxField.get(activity);
                    if (testItemIdxObj instanceof Integer) {
                        status.testItemIdx = (Integer) testItemIdxObj;
                    }
                    
                    java.lang.reflect.Field testItemCounterField = activity.getClass().getDeclaredField("testItemCounter");
                    testItemCounterField.setAccessible(true);
                    Object testItemCounterObj = testItemCounterField.get(activity);
                    if (testItemCounterObj instanceof Integer) {
                        status.testItemCounter = (Integer) testItemCounterObj;
                    }
                    
                    java.lang.reflect.Field testTotalCounterField = activity.getClass().getDeclaredField("testTotalCounter");
                    testTotalCounterField.setAccessible(true);
                    Object testTotalCounterObj = testTotalCounterField.get(activity);
                    if (testTotalCounterObj instanceof Integer) {
                        status.testTotalCounter = (Integer) testTotalCounterObj;
                    }
                    
                    // 테스트 목록 가져오기
                    java.lang.reflect.Field listItemAdapterField = activity.getClass().getDeclaredField("listItemAdapter");
                    listItemAdapterField.setAccessible(true);
                    Object listItemAdapterObj = listItemAdapterField.get(activity);
                    
                    if (listItemAdapterObj != null) {
                        // ItemAdapterTestItem의 getCount() 메서드 호출
                        java.lang.reflect.Method getCountMethod = listItemAdapterObj.getClass().getMethod("getCount");
                        int count = (Integer) getCountMethod.invoke(listItemAdapterObj);
                        status.totalTestItems = count;
                        
                        // 각 테스트 항목 정보 가져오기
                        java.lang.reflect.Method getItemMethod = listItemAdapterObj.getClass().getMethod("getItem", int.class);
                        for (int i = 0; i < count; i++) {
                            Object voTestItem = getItemMethod.invoke(listItemAdapterObj, i);
                            if (voTestItem != null) {
                                // VoTestItem의 필드 가져오기
                                java.lang.reflect.Method getNameMethod = voTestItem.getClass().getMethod("getTest_item_name");
                                java.lang.reflect.Method getCommandMethod = voTestItem.getClass().getMethod("getTest_item_command");
                                java.lang.reflect.Method getResultMethod = voTestItem.getClass().getMethod("getTest_item_result");
                                java.lang.reflect.Method getFinishYnMethod = voTestItem.getClass().getMethod("getTest_finish_yn");
                                java.lang.reflect.Method getResultValueMethod = voTestItem.getClass().getMethod("getTest_result_value");
                                java.lang.reflect.Method getSeqMethod = voTestItem.getClass().getMethod("getTest_item_seq");
                                
                                String seq = getSeqMethod != null ? (String) getSeqMethod.invoke(voTestItem) : String.valueOf(i + 1);
                                String name = getNameMethod != null ? (String) getNameMethod.invoke(voTestItem) : "";
                                String command = getCommandMethod != null ? (String) getCommandMethod.invoke(voTestItem) : "";
                                String result = getResultMethod != null ? (String) getResultMethod.invoke(voTestItem) : "";
                                String finishYn = getFinishYnMethod != null ? (String) getFinishYnMethod.invoke(voTestItem) : "";
                                String resultValue = getResultValueMethod != null ? (String) getResultValueMethod.invoke(voTestItem) : "";
                                
                                status.testItemList.add(new TestStatusInfo.TestItemInfo(seq, name, command, result, finishYn, resultValue));
                            }
                        }
                    }
                    
                    // 테스트 실행 중 여부 판단
                    status.isTestRunning = !status.currentTestItem.isEmpty() && 
                                          (status.currentTestItem.equals(Constants.InitialValues.CURRENT_TEST_ITEM) == false);
                    
                    // 테스트가 시작되지 않았을 때 (초기값이거나 실행 중이 아닐 때) currentTestItem을 빈 문자열로 처리
                    if (!status.isTestRunning || status.currentTestItem.equals(Constants.InitialValues.CURRENT_TEST_ITEM)) {
                        status.currentTestItem = "";
                    }
                    
                    // 테스트가 시작되지 않았을 때 modelId를 빈 문자열로 처리 (웹 화면에서 '대기 중' 표시)
                    if (!status.isTestRunning) {
                        status.modelId = "";
                    }
                    
                    // 온도 정보 가져오기
                    try {
                        java.lang.reflect.Field tvTemperatureField = activity.getClass().getDeclaredField("tvTemperature");
                        tvTemperatureField.setAccessible(true);
                        Object tvTemperatureObj = tvTemperatureField.get(activity);
                        if (tvTemperatureObj != null) {
                            java.lang.reflect.Method getTextMethod = tvTemperatureObj.getClass().getMethod("getText");
                            Object textObj = getTextMethod.invoke(tvTemperatureObj);
                            if (textObj != null) {
                                String tempText = textObj.toString().trim();
                                // "℃" 제거
                                tempText = tempText.replace("℃", "").trim();
                                status.temperature = tempText.isEmpty() ? "0" : tempText;
                            }
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "Could not get temperature: " + e.getMessage());
                    }
                    
                    // 소비전력 정보 가져오기
                    try {
                        java.lang.reflect.Field tvWattValueField = activity.getClass().getDeclaredField("tvWattValue");
                        tvWattValueField.setAccessible(true);
                        Object tvWattValueObj = tvWattValueField.get(activity);
                        if (tvWattValueObj != null) {
                            java.lang.reflect.Method getTextMethod = tvWattValueObj.getClass().getMethod("getText");
                            Object textObj = getTextMethod.invoke(tvWattValueObj);
                            if (textObj != null) {
                                String wattText = textObj.toString().trim();
                                // "Watt" 제거
                                wattText = wattText.replace("Watt", "").trim();
                                status.wattValue = wattText.isEmpty() ? "0" : wattText;
                            }
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "Could not get watt value: " + e.getMessage());
                    }
                    
                    // 설정시간 (totalTimeCnt) 가져오기
                    try {
                        java.lang.reflect.Field totalTimeCntField = activity.getClass().getDeclaredField("totalTimeCnt");
                        totalTimeCntField.setAccessible(true);
                        Object totalTimeCntObj = totalTimeCntField.get(activity);
                        if (totalTimeCntObj instanceof Integer) {
                            status.totalTimeCnt = (Integer) totalTimeCntObj;
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "Could not get totalTimeCnt: " + e.getMessage());
                    }
                    
                    // 진행시간 계산 (현재 테스트 항목의 경과 시간)
                    // testItemIdx와 arrTestItems를 사용하여 계산
                    try {
                        java.lang.reflect.Field arrTestItemsField = activity.getClass().getDeclaredField("arrTestItems");
                        arrTestItemsField.setAccessible(true);
                        Object arrTestItemsObj = arrTestItemsField.get(activity);
                        if (arrTestItemsObj instanceof String[][]) {
                            String[][] arrTestItems = (String[][]) arrTestItemsObj;
                            if (status.testItemIdx > 0 && status.testItemIdx <= arrTestItems.length) {
                                // 이전 항목들의 시간 합계
                                int spentTime = 0;
                                for (int i = 0; i < status.testItemIdx - 1 && i < arrTestItems.length; i++) {
                                    try {
                                        if (arrTestItems[i].length > 2) {
                                            int seconds = Integer.parseInt(arrTestItems[i][2]);
                                            spentTime += (seconds > 1) ? seconds + 1 : seconds;
                                        }
                                    } catch (Exception ignored) {}
                                }
                                status.spentDuration = spentTime;
                            }
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "Could not calculate spent duration: " + e.getMessage());
                    }
                    
                    // 제어 모드 상태 가져오기
                    try {
                        java.lang.reflect.Field isControlModeField = activity.getClass().getDeclaredField("isControlMode");
                        isControlModeField.setAccessible(true);
                        Object isControlModeObj = isControlModeField.get(activity);
                        if (isControlModeObj instanceof Boolean) {
                            status.isControlMode = (Boolean) isControlModeObj;
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "Could not get control mode status: " + e.getMessage());
                    }
                    
                    try {
                        java.lang.reflect.Field isControlOnField = activity.getClass().getDeclaredField("isControlOn");
                        isControlOnField.setAccessible(true);
                        Object isControlOnObj = isControlOnField.get(activity);
                        if (isControlOnObj instanceof Boolean) {
                            status.isControlOn = (Boolean) isControlOnObj;
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "Could not get control on status: " + e.getMessage());
                    }
                    
                    // 램프 상태 가져오기 (TextView의 배경색으로 확인)
                    try {
                        // 웹서버 램프
                        try {
                            java.lang.reflect.Field tvRunWsRampField = activity.getClass().getDeclaredField("tvRunWsRamp");
                            tvRunWsRampField.setAccessible(true);
                            Object tvRunWsRampObj = tvRunWsRampField.get(activity);
                            if (tvRunWsRampObj != null) {
                                // 배경색 확인을 위해 getBackground() 호출
                                // 하지만 리플렉션으로 색상 확인이 복잡하므로, 웹서버 실행 상태로 판단
                                status.rampSvr = isRunning();
                            }
                        } catch (Exception e) {
                            status.rampSvr = isRunning();
                        }
                        
                        // 블루투스 램프
                        try {
                            java.lang.reflect.Field tvConnectBtRampField = activity.getClass().getDeclaredField("tvConnectBtRamp");
                            tvConnectBtRampField.setAccessible(true);
                            Object tvConnectBtRampObj = tvConnectBtRampField.get(activity);
                            if (tvConnectBtRampObj != null) {
                                // isConnected 필드 확인
                                try {
                                    java.lang.reflect.Field isConnectedField = activity.getClass().getDeclaredField("isConnected");
                                    isConnectedField.setAccessible(true);
                                    Object isConnectedObj = isConnectedField.get(activity);
                                    if (isConnectedObj instanceof Boolean) {
                                        status.rampBt = (Boolean) isConnectedObj;
                                    }
                                } catch (Exception e) {
                                    // btSocket으로 확인
                                    try {
                                        java.lang.reflect.Field btSocketField = activity.getClass().getDeclaredField("btSocket");
                                        btSocketField.setAccessible(true);
                                        Object btSocketObj = btSocketField.get(activity);
                                        if (btSocketObj != null) {
                                            java.lang.reflect.Method isConnectedMethod = btSocketObj.getClass().getMethod("isConnected");
                                            Object connectedObj = isConnectedMethod.invoke(btSocketObj);
                                            if (connectedObj instanceof Boolean) {
                                                status.rampBt = (Boolean) connectedObj;
                                            }
                                        }
                                    } catch (Exception e2) {
                                        // 기본값 false
                                        status.rampBt = false;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "Could not get BT ramp status: " + e.getMessage());
                        }
                        
                        // USB/PLC 램프
                        try {
                            java.lang.reflect.Field tvConnectPlcRampField = activity.getClass().getDeclaredField("tvConnectPlcRamp");
                            tvConnectPlcRampField.setAccessible(true);
                            Object tvConnectPlcRampObj = tvConnectPlcRampField.get(activity);
                            if (tvConnectPlcRampObj != null) {
                                // UsbService 확인
                                try {
                                    java.lang.reflect.Field usbServiceField = activity.getClass().getDeclaredField("usbService");
                                    usbServiceField.setAccessible(true);
                                    Object usbServiceObj = usbServiceField.get(activity);
                                    if (usbServiceObj != null) {
                                        // USB 서비스가 있고 연결되어 있는지 확인
                                        try {
                                            java.lang.reflect.Method isConnectedMethod = usbServiceObj.getClass().getMethod("isConnected");
                                            Object connectedObj = isConnectedMethod.invoke(usbServiceObj);
                                            if (connectedObj instanceof Boolean) {
                                                status.rampUsb = (Boolean) connectedObj;
                                            } else {
                                                status.rampUsb = true; // 서비스가 있으면 연결된 것으로 간주
                                            }
                                        } catch (Exception e) {
                                            status.rampUsb = true; // 서비스가 있으면 연결된 것으로 간주
                                        }
                                    } else {
                                        status.rampUsb = false;
                                    }
                                } catch (Exception e) {
                                    status.rampUsb = false;
                                }
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "Could not get USB ramp status: " + e.getMessage());
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "Could not get ramp status: " + e.getMessage());
                    }
                    
                    // 중간 섹션 데이터 가져오기 (기준값, 상한값, 하한값, 측정값)
                    try {
                        // 기준값
                        try {
                            java.lang.reflect.Field tvCompValueWattField = activity.getClass().getDeclaredField("tvCompValueWatt");
                            tvCompValueWattField.setAccessible(true);
                            Object tvCompValueWattObj = tvCompValueWattField.get(activity);
                            if (tvCompValueWattObj != null) {
                                java.lang.reflect.Method getTextMethod = tvCompValueWattObj.getClass().getMethod("getText");
                                Object textObj = getTextMethod.invoke(tvCompValueWattObj);
                                if (textObj != null) {
                                    status.compValueWatt = textObj.toString().trim();
                                }
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "Could not get comp value watt: " + e.getMessage());
                        }
                        
                        try {
                            java.lang.reflect.Field tvHeaterValueWattField = activity.getClass().getDeclaredField("tvHeaterValueWatt");
                            tvHeaterValueWattField.setAccessible(true);
                            Object tvHeaterValueWattObj = tvHeaterValueWattField.get(activity);
                            if (tvHeaterValueWattObj != null) {
                                java.lang.reflect.Method getTextMethod = tvHeaterValueWattObj.getClass().getMethod("getText");
                                Object textObj = getTextMethod.invoke(tvHeaterValueWattObj);
                                if (textObj != null) {
                                    status.heaterValueWatt = textObj.toString().trim();
                                }
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "Could not get heater value watt: " + e.getMessage());
                        }
                        
                        try {
                            java.lang.reflect.Field tvPumpValueWattField = activity.getClass().getDeclaredField("tvPumpValueWatt");
                            tvPumpValueWattField.setAccessible(true);
                            Object tvPumpValueWattObj = tvPumpValueWattField.get(activity);
                            if (tvPumpValueWattObj != null) {
                                java.lang.reflect.Method getTextMethod = tvPumpValueWattObj.getClass().getMethod("getText");
                                Object textObj = getTextMethod.invoke(tvPumpValueWattObj);
                                if (textObj != null) {
                                    status.pumpValueWatt = textObj.toString().trim();
                                }
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "Could not get pump value watt: " + e.getMessage());
                        }
                        
                        // 상한값
                        try {
                            java.lang.reflect.Field tvCompUpperValueWattField = activity.getClass().getDeclaredField("tvCompUpperValueWatt");
                            tvCompUpperValueWattField.setAccessible(true);
                            Object tvCompUpperValueWattObj = tvCompUpperValueWattField.get(activity);
                            if (tvCompUpperValueWattObj != null) {
                                java.lang.reflect.Method getTextMethod = tvCompUpperValueWattObj.getClass().getMethod("getText");
                                Object textObj = getTextMethod.invoke(tvCompUpperValueWattObj);
                                if (textObj != null) {
                                    status.compUpperValueWatt = textObj.toString().trim();
                                }
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "Could not get comp upper value watt: " + e.getMessage());
                        }
                        
                        try {
                            java.lang.reflect.Field tvHeaterUpperValueWattField = activity.getClass().getDeclaredField("tvHeaterUpperValueWatt");
                            tvHeaterUpperValueWattField.setAccessible(true);
                            Object tvHeaterUpperValueWattObj = tvHeaterUpperValueWattField.get(activity);
                            if (tvHeaterUpperValueWattObj != null) {
                                java.lang.reflect.Method getTextMethod = tvHeaterUpperValueWattObj.getClass().getMethod("getText");
                                Object textObj = getTextMethod.invoke(tvHeaterUpperValueWattObj);
                                if (textObj != null) {
                                    status.heaterUpperValueWatt = textObj.toString().trim();
                                }
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "Could not get heater upper value watt: " + e.getMessage());
                        }
                        
                        try {
                            java.lang.reflect.Field tvPumpUpperValueWattField = activity.getClass().getDeclaredField("tvPumpUpperValueWatt");
                            tvPumpUpperValueWattField.setAccessible(true);
                            Object tvPumpUpperValueWattObj = tvPumpUpperValueWattField.get(activity);
                            if (tvPumpUpperValueWattObj != null) {
                                java.lang.reflect.Method getTextMethod = tvPumpUpperValueWattObj.getClass().getMethod("getText");
                                Object textObj = getTextMethod.invoke(tvPumpUpperValueWattObj);
                                if (textObj != null) {
                                    status.pumpUpperValueWatt = textObj.toString().trim();
                                }
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "Could not get pump upper value watt: " + e.getMessage());
                        }
                        
                        // 하한값
                        try {
                            java.lang.reflect.Field tvCompLowerValueWattField = activity.getClass().getDeclaredField("tvCompLowerValueWatt");
                            tvCompLowerValueWattField.setAccessible(true);
                            Object tvCompLowerValueWattObj = tvCompLowerValueWattField.get(activity);
                            if (tvCompLowerValueWattObj != null) {
                                java.lang.reflect.Method getTextMethod = tvCompLowerValueWattObj.getClass().getMethod("getText");
                                Object textObj = getTextMethod.invoke(tvCompLowerValueWattObj);
                                if (textObj != null) {
                                    status.compLowerValueWatt = textObj.toString().trim();
                                }
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "Could not get comp lower value watt: " + e.getMessage());
                        }
                        
                        try {
                            java.lang.reflect.Field tvHeaterLowerValueWattField = activity.getClass().getDeclaredField("tvHeaterLowerValueWatt");
                            tvHeaterLowerValueWattField.setAccessible(true);
                            Object tvHeaterLowerValueWattObj = tvHeaterLowerValueWattField.get(activity);
                            if (tvHeaterLowerValueWattObj != null) {
                                java.lang.reflect.Method getTextMethod = tvHeaterLowerValueWattObj.getClass().getMethod("getText");
                                Object textObj = getTextMethod.invoke(tvHeaterLowerValueWattObj);
                                if (textObj != null) {
                                    status.heaterLowerValueWatt = textObj.toString().trim();
                                }
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "Could not get heater lower value watt: " + e.getMessage());
                        }
                        
                        try {
                            java.lang.reflect.Field tvPumpLowerValueWattField = activity.getClass().getDeclaredField("tvPumpLowerValueWatt");
                            tvPumpLowerValueWattField.setAccessible(true);
                            Object tvPumpLowerValueWattObj = tvPumpLowerValueWattField.get(activity);
                            if (tvPumpLowerValueWattObj != null) {
                                java.lang.reflect.Method getTextMethod = tvPumpLowerValueWattObj.getClass().getMethod("getText");
                                Object textObj = getTextMethod.invoke(tvPumpLowerValueWattObj);
                                if (textObj != null) {
                                    status.pumpLowerValueWatt = textObj.toString().trim();
                                }
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "Could not get pump lower value watt: " + e.getMessage());
                        }
                        
                        // 측정값
                        try {
                            java.lang.reflect.Field tvCompWattValueField = activity.getClass().getDeclaredField("tvCompWattValue");
                            tvCompWattValueField.setAccessible(true);
                            Object tvCompWattValueObj = tvCompWattValueField.get(activity);
                            if (tvCompWattValueObj != null) {
                                java.lang.reflect.Method getTextMethod = tvCompWattValueObj.getClass().getMethod("getText");
                                Object textObj = getTextMethod.invoke(tvCompWattValueObj);
                                if (textObj != null) {
                                    status.compWattValue = textObj.toString().trim();
                                }
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "Could not get comp watt value: " + e.getMessage());
                        }
                        
                        try {
                            java.lang.reflect.Field tvHeaterWattValueField = activity.getClass().getDeclaredField("tvHeaterWattValue");
                            tvHeaterWattValueField.setAccessible(true);
                            Object tvHeaterWattValueObj = tvHeaterWattValueField.get(activity);
                            if (tvHeaterWattValueObj != null) {
                                java.lang.reflect.Method getTextMethod = tvHeaterWattValueObj.getClass().getMethod("getText");
                                Object textObj = getTextMethod.invoke(tvHeaterWattValueObj);
                                if (textObj != null) {
                                    status.heaterWattValue = textObj.toString().trim();
                                }
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "Could not get heater watt value: " + e.getMessage());
                        }
                        
                        try {
                            java.lang.reflect.Field tvPumpWattValueField = activity.getClass().getDeclaredField("tvPumpWattValue");
                            tvPumpWattValueField.setAccessible(true);
                            Object tvPumpWattValueObj = tvPumpWattValueField.get(activity);
                            if (tvPumpWattValueObj != null) {
                                java.lang.reflect.Method getTextMethod = tvPumpWattValueObj.getClass().getMethod("getText");
                                Object textObj = getTextMethod.invoke(tvPumpWattValueObj);
                                if (textObj != null) {
                                    status.pumpWattValue = textObj.toString().trim();
                                }
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "Could not get pump watt value: " + e.getMessage());
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "Could not get middle section data: " + e.getMessage());
                    }
                    
                } catch (Exception e) {
                    Log.d(TAG, "Could not get test info from activity: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting test status info", e);
        }
        
        return status;
    }
    
    /**
     * 테스트 상태 웹페이지 생성 (HTML) - activity_main.xml 레이아웃과 동일한 구조
     */
    private Response handleTestStatus(IHTTPSession session) {
        try {
            // 접속 수 제한 체크 (context가 있는 경우에만)
            if (context != null) {
                int currentConnections = getActiveConnectionCountWithTimeout();
                int maxConnections = getMaxConnections();

                Log.w(TAG, "[상태 조회 차단] 현재 접속자 수(" + currentConnections + ") / 최대 접속 수(" + maxConnections + ")");
                if (currentConnections > maxConnections) {
                    Log.w(TAG, "[상태 조회 차단] 현재 접속자 수(" + currentConnections + ")가 최대 접속 수(" + maxConnections + ")를 초과했습니다.");
                    
                    // 접근 불가 다이얼로그를 포함한 HTML 페이지 반환
                    String errorHtml = "<!DOCTYPE html>\n" +
                            "<html>\n" +
                            "<head>\n" +
                            "    <meta charset=\"UTF-8\">\n" +
                            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                            "    <title>접속 제한</title>\n" +
                            "    <style>\n" +
                            "        * { margin: 0; padding: 0; box-sizing: border-box; }\n" +
                            "        body { \n" +
                            "            font-family: 'NanumSquare', '나눔스퀘어', Arial, sans-serif;\n" +
                            "            background: #f5f5f5;\n" +
                            "            display: flex;\n" +
                            "            justify-content: center;\n" +
                            "            align-items: center;\n" +
                            "            min-height: 100vh;\n" +
                            "            padding: 20px;\n" +
                            "        }\n" +
                            "        .dialog-container {\n" +
                            "            background: white;\n" +
                            "            border-radius: 10px;\n" +
                            "            box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);\n" +
                            "            max-width: 500px;\n" +
                            "            width: 100%;\n" +
                            "            padding: 40px;\n" +
                            "            text-align: center;\n" +
                            "        }\n" +
                            "        .dialog-icon {\n" +
                            "            width: 80px;\n" +
                            "            height: 80px;\n" +
                            "            margin: 0 auto 20px;\n" +
                            "            background: #ff4444;\n" +
                            "            border-radius: 50%;\n" +
                            "            display: flex;\n" +
                            "            align-items: center;\n" +
                            "            justify-content: center;\n" +
                            "            font-size: 40px;\n" +
                            "            color: white;\n" +
                            "        }\n" +
                            "        .dialog-title {\n" +
                            "            font-size: 24px;\n" +
                            "            font-weight: bold;\n" +
                            "            color: #333;\n" +
                            "            margin-bottom: 15px;\n" +
                            "        }\n" +
                            "        .dialog-message {\n" +
                            "            font-size: 16px;\n" +
                            "            color: #666;\n" +
                            "            line-height: 1.6;\n" +
                            "            margin-bottom: 20px;\n" +
                            "        }\n" +
                            "        .dialog-info {\n" +
                            "            background: #f9f9f9;\n" +
                            "            border-radius: 5px;\n" +
                            "            padding: 15px;\n" +
                            "            margin: 20px 0;\n" +
                            "            font-size: 14px;\n" +
                            "            color: #555;\n" +
                            "        }\n" +
                            "        .dialog-info strong {\n" +
                            "            color: #ff4444;\n" +
                            "        }\n" +
                            "        .dialog-button {\n" +
                            "            background: #2196F3;\n" +
                            "            color: white;\n" +
                            "            border: none;\n" +
                            "            border-radius: 5px;\n" +
                            "            padding: 12px 30px;\n" +
                            "            font-size: 16px;\n" +
                            "            cursor: pointer;\n" +
                            "            margin-top: 20px;\n" +
                            "            transition: background 0.3s;\n" +
                            "        }\n" +
                            "        .dialog-button:hover {\n" +
                            "            background: #1976D2;\n" +
                            "        }\n" +
                            "    </style>\n" +
                            "    <script>\n" +
                            "        function refreshPage() {\n" +
                            "            location.reload();\n" +
                            "        }\n" +
                            "        // 5초마다 자동 새로고침\n" +
                            "        setInterval(refreshPage, 5000);\n" +
                            "    </script>\n" +
                            "</head>\n" +
                            "<body>\n" +
                            "    <div class=\"dialog-container\">\n" +
                            "        <div class=\"dialog-icon\">⚠</div>\n" +
                            "        <div class=\"dialog-title\">접속 제한</div>\n" +
                            "        <div class=\"dialog-message\">\n" +
                            "            동시 접속 가능한 사용자 수를 초과했습니다.<br>\n" +
                            "            잠시 후 다시 시도해주세요.\n" +
                            "        </div>\n" +
                            "        <div class=\"dialog-info\">\n" +
                            "            현재 접속자: <strong>" + currentConnections + "명</strong><br>\n" +
                            "            최대 접속 가능: <strong>" + maxConnections + "명</strong>\n" +
                            "        </div>\n" +
                            "        <button class=\"dialog-button\" onclick=\"refreshPage()\">새로고침</button>\n" +
                            "    </div>\n" +
                            "</body>\n" +
                            "</html>";
                    return newResponse(Response.Status.FORBIDDEN, "text/html; charset=utf-8", errorHtml);
                }
            }
            
            TestStatusInfo status = getTestStatusInfo();
            
            // OK/NG 카운트 계산
            int okCount = 0;
            int ngCount = 0;
            for (TestStatusInfo.TestItemInfo item : status.testItemList) {
                if ("Y".equals(item.finishYn) || "YES".equals(item.finishYn)) {
                    if ("OK".equals(item.result) || "Y".equals(item.result)) {
                        okCount++;
                    } else if ("NG".equals(item.result) || "N".equals(item.result)) {
                        ngCount++;
                    }
                }
            }
            
            String html = "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\">\n" +
                    "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                    "    <link rel=\"stylesheet\" href=\"https://cdn.jsdelivr.net/gh/moonspam/NanumSquare@2.0/nanumsquare.css\">\n" +
                    "    <title>검사 키트 상태</title>\n" +
                    "    <style>\n" +
                    "        * { margin: 0; padding: 0; box-sizing: border-box; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; }\n" +
                    "        body { font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; background: #EEEEEE; margin: 0; padding: 0; }\n" +
                    "        .table-layout { width: 100%; height: 100vh; display: table; table-layout: fixed; }\n" +
                    "        .table-row { display: table-row; }\n" +
                    "        .table-cell { display: table-cell; vertical-align: top; }\n" +
                    "        \n" +
                    "        /* 상단 헤더 */\n" +
                    "        .header { width: 100%; height: 50px; background: #092c74; display: flex; align-items: center; padding: 0 10px; }\n" +
                    "        .header-left { flex: 5; display: flex; align-items: center; padding-left: 10px; gap: 10px; }\n" +
                    "        .logoImg { width: 107px; height: 30px; object-fit: contain; }\n" +
                    "        .header-unit-id { color: white; font-size: 28px; font-weight: bold; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; }\n" +
                    "        .header-center { flex: 8; display: flex; align-items: center; justify-content: center; gap: 20px; color: white; font-size: 20px; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; }\n" +
                    "        .header-info-item { display: flex; align-items: center; gap: 5px; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; }\n" +
                    "        .header-info-label { font-size: 20px; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; }\n" +
                    "        .header-info-value { font-size: 24px; font-weight: bold; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; }\n" +
                    "        .header-right { flex: 2; display: flex; justify-content: flex-end; align-items: center; gap: 10px; padding-right: 10px; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; }\n" +
                    "        .status-badge { width: 50px; height: 25px; color: white; text-align: center; line-height: 25px; font-size: 10px; font-weight: bold; padding: 5px; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; }\n" +
                    "        .status-badge.connected { background: #2196F3; }\n" +
                    "        .status-badge.disconnected { background: #FF0000; }\n" +
                    "        \n" +
                    "        /* 메인 콘텐츠 영역 */\n" +
                    "        .main-content { width: 100%; height: calc(100vh - 50px); display: flex; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; }\n" +
                    "        .left-panel { flex: 1; background: white; padding: 0; overflow-y: auto; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; }\n" +
                    "        .right-panel { flex: 1.5; background: white; padding: 0; overflow-y: auto; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; }\n" +
                    "        \n" +
                    "        /* 정보 섹션 */\n" +
                    "        .info-section { width: 100%; margin-bottom: 0; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; }\n" +
                    "        .info-header { width: 100%; height: 45px; background: #000000; color: white; display: flex; align-items: center; justify-content: center; font-size: 18px; padding: 5px; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; }\n" +
                    "        .info-content { padding: 10px; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; }\n" +
                    "        .info-text { font-size: 45px; font-weight: bold; color: #000000; padding: 5px 10px; letter-spacing: -0.05em; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; }\n" +
                    "        \n" +
                    "        /* OK/NG 카운트 */\n" +
                    "        .count-container { display: flex; width: 100%; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; }\n" +
                    "        .count-section { flex: 1; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; }\n" +
                    "        .count-value { font-size: 50px; font-weight: bold; text-align: center; padding: 10px; color: #000000; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; }\n" +
                    "        \n" +
                    "        /* 테스트 항목 리스트 */\n" +
                    "        .test-list-header { width: 100%; height: 45px; background: #000000; color: white; display: flex; align-items: center; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; position: sticky; top: 0; z-index: 100; }\n" +
                    "        .test-list-header-cell { height: 100%; display: flex; align-items: center; justify-content: center; font-size: 18px; padding: 5px; border-right: 0px solid #333; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; }\n" +
                    "        .test-list-header-cell:last-child { border-right: none; }\n" +
                    "        .test-list { width: 100%; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; }\n" +
                    "        .test-item-row { width: 100%; display: flex; border-bottom: 1px solid #ddd; background: white; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; }\n" +
                    "        .test-item-row.current { background: #f0fff4; }\n" +
                    "        .test-item-row.current_bak { background: #f0fff4; border-left: 5px solid #28a745; }\n" +
                    "        .test-item-cell { padding: 10px; display: flex; align-items: center; justify-content: center; font-size: 16px; border-right: 0px solid #ddd; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; }\n" +
                    "        .test-item-cell:last-child { border-right: none; }\n" +
                    "        .test-item-cell.no { flex: 0.05; min-width: 50px; }\n" +
                    "        .test-item-cell.name { flex: 0.8; justify-content: flex-start; text-align: left; }\n" +
                    "        .test-item-cell.result { flex: 0.1; }\n" +
                    "        .test-item-cell.value { flex: 0.15; font-size: 14px; }\n" +
                    "        .test-item-cell.action { flex: 0.2; text-align:right; }\n" +
                    "        .execute-btn { background: #2196F3; color: white; border: none; padding: 8px 16px; border-radius: 4px; cursor: pointer; font-size: 14px; font-weight: bold; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; }\n" +
                    "        .execute-btn:hover { background: #1976D2; }\n" +
                    "        .execute-btn:disabled { background: #ccc; cursor: not-allowed; }\n" +
                    "        .status-ok { color: #28a745; font-weight: bold; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; }\n" +
                    "        .status-ng { color: #FF0000; font-weight: bold; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; }\n" +
                    "        .status-pending { color: #999; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; }\n" +
                    "        .status-running { color: #17a2b8; font-weight: bold; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; }\n" +
                    "        \n" +
                    "        .timestamp { text-align: center; color: #666; font-size: 12px; padding: 10px; background: #f5f5f5; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; }\n" +
                    "        \n" +
                    "        /* 중간 섹션 - 테스트 상세 정보 */\n" +
                    "        .middle-section { width: 100%; background: white; border-top: 1px solid #ddd; }\n" +
                    "        .middle-section-table { width: 100%; border-collapse: collapse; table-layout: fixed; }\n" +
                    "        .middle-section-header { background: #000000; color: #FCFFFFFF; text-align: center; padding: 10px; font-size: 24px; font-weight: normal; border-right: 1px solid #333; height: 48px; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; letter-spacing: -0.05em; }\n" +
                    "        .middle-section-header:last-child { border-right: none; }\n" +
                    "        .middle-section-item-col { background: #4A4A4A; color: white; text-align: center; padding: 6px; font-size: 22px; font-weight: bold; border-right: 1px solid #ddd; border-bottom: 1px solid #ddd; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; letter-spacing: -0.05em; }\n" +
                    "        .middle-section-value-col { background: white; color: black; text-align: right; padding: 6px; font-size: 22px; border-right: 1px solid #ddd; border-bottom: 1px solid #ddd; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; letter-spacing: -0.05em; }\n" +
                    "        .middle-section-value-col:last-child { border-right: none; }\n" +
                    "        .middle-section-large-value { background: white; color: black; text-align: right; padding: 4px 10px; font-size: 60px; font-weight: bold; border-right: 1px solid #ddd; vertical-align: middle; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; }\n" +
                    "        .middle-section-large-value:last-child { border-right: none; }\n" +
                    "        .middle-section-large-button { background: white; color: black; text-align: center; padding: 4px 10px; font-size: 24px; font-weight: bold; border-right: 1px solid #ddd; border-bottom: 1px solid #ddd; vertical-align: middle; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; cursor: pointer; }\n" +
                    "        .middle-section-button { background: #4CAF50; color: white; text-align: center; padding: 10px; font-size: 32px; font-weight: bold; border-right: 1px solid #ddd; vertical-align: middle; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; letter-spacing: -0.05em; }\n" +
                    "        .middle-section-button-control { background: #FF9800; color: white; text-align: center; padding: 10px; font-size: 32px; font-weight: bold; border-right: 1px solid #ddd; vertical-align: middle; font-family: 'NanumSquare', '나눔스퀘어', sans-serif !important; letter-spacing: -0.05em; }\n" +
                    "        .middle-section-button:last-child, .middle-section-button-control:last-child { border-right: none; }\n" +
                    "    </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "    <div class=\"table-layout\">\n" +
                    "        <!-- 상단 헤더 -->\n" +
                    "        <div class=\"table-row\">\n" +
                    "            <div class=\"table-cell\">\n" +
                    "                <div class=\"header\">\n" +
                    "                    <div class=\"header-left\">\n" +
                    "                        <img class=\"logoImg\" src=\"" + getOvioLogoDataUri() + "\" alt=\"OVIO\" />\n" +
                    "                        <div class=\"header-unit-id\" id=\"header-unit-id\">" + (status.unitId.isEmpty() ? "검사 키트" : status.unitId) + "</div>\n" +
                    "                    </div>\n" +
                    "                    <div class=\"header-center\">\n" +
                    "                        <!--\n" +
                    "                        <div class=\"header-info-item\">\n" +
                    "                            <span class=\"header-info-label\">온도:</span>\n" +
                    "                            <span class=\"header-info-value\">" + status.temperature + "℃</span>\n" +
                    "                        </div>\n" +
                    "                        <div class=\"header-info-item\">\n" +
                    "                            <span class=\"header-info-label\">소비전력:</span>\n" +
                    "                            <span class=\"header-info-value\">" + status.wattValue + "Watt</span>\n" +
                    "                        </div>\n" +
                    "                        <div class=\"header-info-item\">\n" +
                    "                            <span class=\"header-info-label\">진행:</span>\n" +
                    "                            <span class=\"header-info-value\">" + status.spentDuration + "</span>\n" +
                    "                            <span style=\"margin: 0 5px;\">/</span>\n" +
                    "                            <span class=\"header-info-value\">" + status.totalTimeCnt + "</span>\n" +
                    "                            <span style=\"font-size: 20px; margin-left: 3px;\">sec</span>\n" +
                    "                        </div>\n" +
                    "                        -->\n" +
                    "                    </div>\n" +
                    "                    <div class=\"header-right\">\n" +
                    "                        <div class=\"status-badge " + (status.rampSvr ? "connected" : "disconnected") + "\">SVR</div>\n" +
                    "                        <div class=\"status-badge " + (status.rampBt ? "connected" : "disconnected") + "\">BT</div>\n" +
                    "                        <div class=\"status-badge " + (status.rampUsb ? "connected" : "disconnected") + "\">USB</div>\n" +
                    "                    </div>\n" +
                    "                </div>\n" +
                    "            </div>\n" +
                    "        </div>\n" +
                    "        \n" +
                    "        <!-- 중간 섹션 - 테스트 상세 정보 -->\n" +
                    "        <div class=\"table-row\">\n" +
                    "            <div class=\"table-cell\">\n" +
                    "                <div class=\"middle-section\">\n" +
                    "                    <table class=\"middle-section-table\">\n" +
                    "                        <thead>\n" +
                    "                            <tr>\n" +
                    "                                <th class=\"middle-section-header\" style=\"width: 10%;\">항목</th>\n" +
                    "                                <th class=\"middle-section-header\" style=\"width: 8%;\">기준값</th>\n" +
                    "                                <th class=\"middle-section-header\" style=\"width: 8%;\">상한값</th>\n" +
                    "                                <th class=\"middle-section-header\" style=\"width: 8%;\">하한값</th>\n" +
                    "                                <th class=\"middle-section-header\" style=\"width: 8%;\">측정값</th>\n" +
                    "                                <th class=\"middle-section-header\" style=\"width: 12%;\">소비전력(W)</th>\n" +
                    "                                <th class=\"middle-section-header\" style=\"width: 12%;\">진행시간(초)</th>\n" +
                    "                                <th class=\"middle-section-header\" style=\"width: 12%;\">설정시간(초)</th>\n" +
                    "                                <th class=\"middle-section-header\" style=\"width: 5%;\">재검사</th>\n" +
                    "                                <th class=\"middle-section-header\" style=\"width: 5%;\">제어</th>\n" +
                    "                            </tr>\n" +
                    "                        </thead>\n" +
                    "                        <tbody>\n" +
                    "                            <tr>\n" +
                    "                                <td class=\"middle-section-item-col\">COMP</td>\n" +
                    "                                <td class=\"middle-section-value-col\">" + status.compValueWatt + "</td>\n" +
                    "                                <td class=\"middle-section-value-col\">" + status.compUpperValueWatt + "</td>\n" +
                    "                                <td class=\"middle-section-value-col\">" + status.compLowerValueWatt + "</td>\n" +
                    "                                <td class=\"middle-section-value-col\">" + status.compWattValue + "</td>\n" +
                    "                                <td class=\"middle-section-large-value\" rowspan=\"3\">" + status.wattValue + "</td>\n" +
                    "                                <td class=\"middle-section-large-value\" rowspan=\"3\">" + status.testTotalCounter + "</td>\n" +
                    "                                <td class=\"middle-section-large-value\" rowspan=\"3\">" + status.totalTimeCnt + "</td>\n" +
                    "                                <td class=\"middle-section-large-button\" id=\"restart-button\" onclick=\"restartTest()\" style=\"cursor: pointer; background: #4CAF50; color: white;\" rowspan=\"3\">재검사</td>\n" +
                    "                                <td class=\"middle-section-large-button\" id=\"control-button\" onclick=\"controlTest()\" style=\"cursor: " + (status.isTestRunning ? "not-allowed" : "pointer") + "; background: " + (status.isTestRunning ? "#ccc" : "#4CAF50") + "; color: " + (status.isTestRunning ? "#666" : "white") + ";\" " + (status.isTestRunning ? "disabled" : "") + " rowspan=\"3\">제어 OFF</td>\n" +
                    "                            </tr>\n" +
                    "                            <tr>\n" +
                    "                                <td class=\"middle-section-item-col\">히터</td>\n" +
                    "                                <td class=\"middle-section-value-col\">" + status.heaterValueWatt + "</td>\n" +
                    "                                <td class=\"middle-section-value-col\">" + status.heaterUpperValueWatt + "</td>\n" +
                    "                                <td class=\"middle-section-value-col\">" + status.heaterLowerValueWatt + "</td>\n" +
                    "                                <td class=\"middle-section-value-col\">" + status.heaterWattValue + "</td>\n" +
                    "                            </tr>\n" +
                    "                            <tr>\n" +
                    "                                <td class=\"middle-section-item-col\">펌프</td>\n" +
                    "                                <td class=\"middle-section-value-col\">" + status.pumpValueWatt + "</td>\n" +
                    "                                <td class=\"middle-section-value-col\">" + status.pumpUpperValueWatt + "</td>\n" +
                    "                                <td class=\"middle-section-value-col\">" + status.pumpLowerValueWatt + "</td>\n" +
                    "                                <td class=\"middle-section-value-col\">" + status.pumpWattValue + "</td>\n" +
                    "                            </tr>\n" +
                    "                        </tbody>\n" +
                    "                    </table>\n" +
                    "                </div>\n" +
                    "            </div>\n" +
                    "        </div>\n" +
                    "        \n" +
                    "        <!-- 메인 콘텐츠 -->\n" +
                    "        <div class=\"table-row\">\n" +
                    "            <div class=\"table-cell\">\n" +
                    "                <div class=\"main-content\">\n" +
                    "                    <!-- 왼쪽 패널 -->\n" +
                    "                    <div class=\"left-panel\">\n" +
                    "                        <!-- 시리얼 번호 -->\n" +
                    "                        <div class=\"info-section\" id=\"info-serial\">\n" +
                    "                            <div class=\"info-header\">시리얼 번호</div>\n" +
                    "                            <div class=\"info-content\">\n" +
                    "                                <div class=\"info-text\" id=\"text-serial\">" + 
                    (status.isTestRunning ? 
                        (status.modelId.isEmpty() || status.modelId.trim().isEmpty() ? "POP DERIAL NO" : status.modelId) 
                        : "대기 중") + "</div>\n" +
                    "                            </div>\n" +
                    "                        </div>\n" +
                    "                        \n" +
                    "                        <!-- 모델명 -->\n" +
                    "                        <div class=\"info-section\" id=\"info-model\">\n" +
                    "                            <div class=\"info-header\">모델명</div>\n" +
                    "                            <div class=\"info-content\">\n" +
                    "                                <div class=\"info-text\" id=\"text-model\">" + (status.modelName.isEmpty() ? "대기 중" : status.modelName) + "</div>\n" +
                    "                            </div>\n" +
                    "                        </div>\n" +
                    "                        \n" +
                    "                        <!-- 현재 테스트 프로세스 -->\n" +
                    "                        <div class=\"info-section\" id=\"info-current\">\n" +
                    "                            <div class=\"info-header\">테스트 프로세스 리스트</div>\n" +
                    "                            <div class=\"info-content\">\n" +
                    "                                <div class=\"info-text\" id=\"text-current\">" + (status.currentTestItem.isEmpty() ? "대기 중" : status.currentTestItem) + "</div>\n" +
                    "                            </div>\n" +
                    "                        </div>\n" +
                    "                        \n" +
                    "                        <!-- OK/NG 카운트 -->\n" +
                    "                        <div class=\"info-section\" id=\"info-count\">\n" +
                    "                            <div class=\"count-container\">\n" +
                    "                                <div class=\"count-section\">\n" +
                    "                                    <div class=\"info-header\">OK</div>\n" +
                    "                                    <div class=\"count-value\" id=\"count-ok\">" + okCount + "</div>\n" +
                    "                                </div>\n" +
                    "                                <div class=\"count-section\">\n" +
                    "                                    <div class=\"info-header\">NG</div>\n" +
                    "                                    <div class=\"count-value\" id=\"count-ng\">" + ngCount + "</div>\n" +
                    "                                </div>\n" +
                    "                            </div>\n" +
                    "                        </div>\n" +
                    "                    </div>\n" +
                    "                    \n" +
                    "                    <!-- 오른쪽 패널 (테스트 항목 리스트) -->\n" +
                    "                    <div class=\"right-panel\">\n" +
                    "                        <div class=\"test-list-header\">\n" +
                    "                            <div class=\"test-list-header-cell\" style=\"flex: 0.05;\">No</div>\n" +
                    "                            <div class=\"test-list-header-cell\" style=\"flex: 0.8;\">테스트 프로세스 리스트</div>\n" +
                    "                            <div class=\"test-list-header-cell\" style=\"flex: 0.1;\">결과</div>\n" +
                    "                            <div class=\"test-list-header-cell\" style=\"flex: 0.15;\">결과값</div>\n" +
                    "                            <div class=\"test-list-header-cell\" style=\"flex: 0.1; text-align:right;\">실행</div>\n" +
                    "                        </div>\n" +
                    "                        <div class=\"test-list\">\n";
            
            if (status.testItemList.isEmpty()) {
                html += "                            <div class=\"test-item-row\">\n" +
                        "                                <div class=\"test-item-cell\" style=\"flex: 1; text-align: center; padding: 40px; color: #999;\">테스트 항목이 없습니다.</div>\n" +
                        "                                <div class=\"test-item-cell action\"></div>\n" +
                        "                            </div>\n";
            } else {
                for (int i = 0; i < status.testItemList.size(); i++) {
                    TestStatusInfo.TestItemInfo item = status.testItemList.get(i);
                    // 진행시간이 0초이면 어떤 항목도 '진행 중'으로 표시하지 않음
                    boolean isCurrent = status.spentDuration > 0 && item.command.equals(status.currentTestItem);
                    boolean isCompleted = "Y".equals(item.finishYn) || "YES".equals(item.finishYn);
                    
                    String rowClass = isCurrent ? "current" : "";
                    String resultClass = "status-pending";
                    String resultText = "대기";
                    
                    if (isCurrent) {
                        resultClass = "status-running";
                        resultText = "진행 중";
                    } else if (isCompleted) {
                        if ("OK".equals(item.result) || "Y".equals(item.result)) {
                            resultClass = "status-ok";
                            resultText = "OK";
                        } else if ("NG".equals(item.result) || "N".equals(item.result)) {
                            resultClass = "status-ng";
                            resultText = "NG";
                        } else {
                            resultClass = "status-ok";
                            resultText = "완료";
                        }
                    }
                    
                    String itemName = item.name.isEmpty() ? item.command : item.name;
                    String resultValue = (item.resultValue != null && !item.resultValue.isEmpty()) ? item.resultValue : "-";
                    
                    // 블루투스, 서버, PLC 연결 상태 확인하여 버튼 활성화/비활성화
                    // 최초에는 제어 버튼이 비활성화 상태이므로 모든 실행 버튼 비활성화
                    // 제어 버튼이 눌려지면 실행 버튼 활성화
                    boolean isBtConnected = status.rampBt;
                    boolean isSvrConnected = status.rampSvr;
                    boolean isUsbConnected = status.rampUsb;
                    // 초기 HTML 생성 시에는 제어 버튼이 비활성화 상태이므로 모든 실행 버튼 비활성화
                    boolean isControlEnabled = false; // 초기값: 비활성화
                    boolean isButtonDisabled = item.command.isEmpty() || !isBtConnected || !isSvrConnected || !isUsbConnected || !isControlEnabled;
                    String disabledAttr = isButtonDisabled ? "disabled" : "";
                    String disabledTitle = "";
                    if (!isBtConnected) {
                        disabledTitle = " title=\"블루투스가 연결되지 않았습니다.1\"";
                    } else if (!isSvrConnected) {
                        disabledTitle = " title=\"서버가 연결되지 않았습니다.\"";
                    } else if (!isUsbConnected) {
                        disabledTitle = " title=\"PLC가 연결되지 않았습니다.\"";
                    } else if (!isControlEnabled) {
                        disabledTitle = " title=\"제어 버튼을 먼저 활성화해주세요.\"";
                    }

                    html += "                            <div class=\"test-item-row " + rowClass + "\">\n" +
                            "                                <div class=\"test-item-cell no\" style=\"flex: 0.05;\">" + (i + 1) + "</div>\n" +
                            "                                <div class=\"test-item-cell name\" style=\"flex: 0.8;\">" + itemName + "</div>\n" +
                            "                                <div class=\"test-item-cell result " + resultClass + "\" style=\"flex: 0.1;\">" + resultText + "</div>\n" +
                            "                                <div class=\"test-item-cell value\" style=\"flex: 0.15;\">" + resultValue + "</div>\n" +
                            "                                <div class=\"test-item-cell action\" style=\"flex: 0.2; text-align:right;\">\n" +
                            "                                    <button class=\"execute-btn\" onclick=\"executeTestItem('" + item.command + "')\" " + disabledAttr + disabledTitle + ">실행</button>\n" +
                            "                                </div>\n" +
                            "                            </div>\n";
                }
            }
            
            html += "                        </div>\n" +
                    "                    </div>\n" +
                    "                </div>\n" +
                    "            </div>\n" +
                    "        </div>\n" +
                    "    </div>\n" +
                    "    <div class=\"timestamp\" id=\"timestamp\">\n" +
                    "        마지막 업데이트: " + getCurrentDatetime("yyyy-MM-dd HH:mm:ss") + " (5초마다 자동 갱신)\n" +
                    "    </div>\n" +
                    "    <script>\n" +
                    "        // 고유 sessionId 생성 및 저장\n" +
                    "        function generateSessionId() {\n" +
                    "            console.log('>>>>>>>>>>>>>>>>>>>>>>>> generateSessionId.Date.now() ' + Date.now())\n" +
                    "            return 'session_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);\n" +
                    "        }\n" +
                    "        \n" +
                    "        // localStorage에서 sessionId 가져오기 또는 생성\n" +
                    "        let clientSessionId = localStorage.getItem('control_session_id');\n" +
                    "        if (!clientSessionId) {\n" +
                    "            clientSessionId = generateSessionId();\n" +
                    "            localStorage.setItem('control_session_id', clientSessionId);\n" +
                    "        }\n" +
                    "        \n" +
                    "        let isBtConnected = " + status.rampBt + ";\n" +
                    "        let isSvrConnected = " + status.rampSvr + ";\n" +
                    "        let isUsbConnected = " + status.rampUsb + ";\n" +
                    "        let isControlEnabled = false; // 초기값: 제어 버튼 OFF 상태\n" +
                    "        let lastShutdownReason = null; // 마지막으로 받은 종료 원인\n" +
                    "        \n" +
                    "        // AJAX로 데이터 업데이트\n" +
                    "        function updateStatus() {\n" +
                    "            fetch('/status/json')\n" +
                    "                .then(response => {\n" +
                    "                    if (!response.ok) {\n" +
                    "                        throw new Error('HTTP error! status: ' + response.status);\n" +
                    "                    }\n" +
                    "                    return response.text().then(text => {\n" +
                    "                        if (!text || text.trim() === '') {\n" +
                    "                            console.error('Empty response from server');\n" +
                    "                            return null;\n" +
                    "                        }\n" +
                    "                        try {\n" +
                    "                            return JSON.parse(text);\n" +
                    "                        } catch (e) {\n" +
                    "                            console.error('Invalid JSON:', text.substring(0, 100));\n" +
                    "                            return null;\n" +
                    "                        }\n" +
                    "                    });\n" +
                    "                })\n" +
                    "                .then(data => {\n" +
                    "                    if (!data) {\n" +
                    "                        console.error('No data received');\n" +
                    "                        return;\n" +
                    "                    }\n" +
                    "                    if (data.error) {\n" +
                    "                        console.error('Error:', data.message);\n" +
                    "                        return;\n" +
                    "                    }\n" +
                    "                    \n" +
                    "                    // 종료 원인 저장 (나중에 연결이 끊어졌을 때 사용)\n" +
                    "                    if (data.last_shutdown_reason !== null && data.last_shutdown_reason !== undefined) {\n" +
                    "                        lastShutdownReason = data.last_shutdown_reason;\n" +
                    "                    }\n" +
                    "                    \n" +
                    "                    // 연결 상태 업데이트\n" +
                    "                    isBtConnected = data.ramp_bt || false;\n" +
                    "                    isSvrConnected = data.ramp_svr || false;\n" +
                    "                    isUsbConnected = data.ramp_usb || false;\n" +
                    "                    \n" +
                    "                    // 상단 헤더 업데이트\n" +
                    "                    const unitIdEl = document.getElementById('header-unit-id');\n" +
                    "                    if (unitIdEl) unitIdEl.textContent = data.unit_id || '검사 키트';\n" +
                    "                    \n" +
                    "                    // 램프 상태 업데이트\n" +
                    "                    updateRampStatus('SVR', data.ramp_svr);\n" +
                    "                    updateRampStatus('BT', data.ramp_bt);\n" +
                    "                    updateRampStatus('USB', data.ramp_usb);\n" +
                    "                    \n" +
                    "                    // 왼쪽 패널 업데이트\n" +
                    "                    updateLeftPanel(data);\n" +
                    "                    \n" +
                    "                    // 중간 섹션 업데이트\n" +
                    "                    updateMiddleSection(data);\n" +
                    "                    \n" +
                    "                    // 테스트 항목 리스트 업데이트\n" +
                    "                    // 제어 버튼 상태 업데이트 (안드로이드 애플리케이션의 실제 상태로 동기화)\n" +
                    "                    const controlButton = document.getElementById('control-button');\n" +
                    "                    if (controlButton) {\n" +
                    "                        // 테스트 진행 중이면 제어 버튼 비활성화\n" +
                    "                        if (data.is_test_running) {\n" +
                    "                            controlButton.style.backgroundColor = '#ccc';\n" +
                    "                            controlButton.style.color = '#666';\n" +
                    "                            controlButton.style.cursor = 'not-allowed';\n" +
                    "                            controlButton.setAttribute('disabled', 'true');\n" +
                    "                            controlButton.onclick = null;\n" +
                    "                        } else {\n" +
                    "                            // 테스트가 진행 중이 아닐 때만 제어 버튼 활성화\n" +
                    "                            controlButton.removeAttribute('disabled');\n" +
                    "                            controlButton.style.cursor = 'pointer';\n" +
                    "                            controlButton.onclick = controlTest;\n" +
                    "                            \n" +
                    "                            // 안드로이드 애플리케이션의 실제 제어 상태 확인\n" +
                    "                            // is_control_mode: 제어 모드 활성화 여부\n" +
                    "                            // is_control_on: 제어 ON/OFF 상태\n" +
                    "                            // timer_running: 타이머 실행 상태 (하위 호환성)\n" +
                    "                            const androidControlMode = data.is_control_mode || false;\n" +
                    "                            const androidControlOn = data.is_control_on || false;\n" +
                    "                            const timerRunning = data.timer_running || false;\n" +
                    "                            \n" +
                    "                            // 안드로이드 애플리케이션이 제어 모드가 아니거나 제어 OFF 상태면 웹도 OFF로 동기화\n" +
                    "                            const shouldBeEnabled = androidControlMode && androidControlOn && timerRunning;\n" +
                    "                            \n" +
                    "                            const previousControlState = isControlEnabled;\n" +
                    "                            \n" +
                    "                            // 안드로이드 애플리케이션 상태와 웹 상태가 다르면 동기화\n" +
                    "                            if (isControlEnabled !== shouldBeEnabled) {\n" +
                    "                                isControlEnabled = shouldBeEnabled;\n" +
                    "                                \n" +
                    "                                // 안드로이드 애플리케이션이 제어 OFF 상태로 변경된 경우 (연결 끊김 등)\n" +
                    "                                if (previousControlState && !shouldBeEnabled) {\n" +
                    "                                    console.log('Android app control state changed to OFF, syncing web page');\n" +
                    "                                                                        // 제어 종료 신호 전달\n" +
                    "                                                                        console.log('>>>>>>>>>>>>>>>>>>>>>>>> end.clientSessionId ' + clientSessionId)\n" +
                                    "                                    const formDataEnd = new FormData();\n" +
                                    "                                    formDataEnd.append('session_id', clientSessionId);\n" +
                                    "                                    fetch('/test/control/end', {\n" +
                                    "                                        method: 'POST',\n" +
                                    "                                        body: formDataEnd\n" +
                                    "                                    })\n" +
                    "                                    .then(response => {\n" +
                    "                                        if (response.ok) {\n" +
                    "                                            console.log('Control end signal sent due to Android app state change');\n" +
                    "                                        }\n" +
                    "                                    })\n" +
                    "                                    .catch(error => {\n" +
                    "                                        console.error('Error sending control end signal:', error);\n" +
                    "                                    });\n" +
                    "                                }\n" +
                    "                                \n" +
                    "                                // 안드로이드 애플리케이션이 제어 ON 상태로 변경된 경우\n" +
                    "                                if (!previousControlState && shouldBeEnabled) {\n" +
                    "                                    console.log('Android app control state changed to ON, syncing web page');\n" +
                    "                                                    console.log('>>>>>>>>>>>>>>>>>>>>>>>> start.clientSessionId ' + clientSessionId)\n" +
                    "                                                                        // 제어 시작 신호 전달\n" +
                                    "                                    const formDataStart = new FormData();\n" +
                                    "                                    formDataStart.append('session_id', clientSessionId);\n" +
                                    "                                    fetch('/test/control/start', {\n" +
                                    "                                        method: 'POST',\n" +
                                    "                                        body: formDataStart\n" +
                                    "                                    })\n" +
                    "                                    .then(response => {\n" +
                    "                                        if (response.ok) {\n" +
                    "                                            console.log('Control start signal sent due to Android app state change');\n" +
                    "                                        }\n" +
                    "                                    })\n" +
                    "                                    .catch(error => {\n" +
                    "                                        console.error('Error sending control start signal:', error);\n" +
                    "                                    });\n" +
                    "                                }\n" +
                    "                            }\n" +
                    "                            \n" +
                            "                            // 제어 버튼 스타일 업데이트\n" +
                            "                            if (isControlEnabled) {\n" +
                            "                                controlButton.style.backgroundColor = '#FF9800';\n" +
                            "                                controlButton.style.color = 'white';\n" +
                            "                                controlButton.textContent = '제어 ON';\n" +
                            "                                \n" +
                            "                                // 제어 ON 상태가 되면 즉시 실행 버튼 활성화\n" +
                            "                                const executeButtons = document.querySelectorAll('.execute-btn');\n" +
                            "                                executeButtons.forEach(btn => {\n" +
                            "                                    const command = btn.getAttribute('onclick');\n" +
                            "                                    if (command && command.includes('executeTestItem')) {\n" +
                            "                                        // 블루투스, 서버, PLC 연결 상태 확인\n" +
                            "                                        if (isBtConnected && isSvrConnected && isUsbConnected) {\n" +
                            "                                            btn.removeAttribute('disabled');\n" +
                            "                                            btn.style.cursor = 'pointer';\n" +
                            "                                            btn.style.opacity = '1';\n" +
                            "                                            btn.removeAttribute('title');\n" +
                            "                                        } else {\n" +
                            "                                            btn.setAttribute('disabled', 'true');\n" +
                            "                                            btn.style.cursor = 'not-allowed';\n" +
                            "                                            btn.style.opacity = '0.5';\n" +
                            "                                            if (!isBtConnected) {\n" +
                            "                                                btn.setAttribute('title', '블루투스가 연결되지 않았습니다.2');\n" +
                            "                                            } else if (!isSvrConnected) {\n" +
                            "                                                btn.setAttribute('title', '서버가 연결되지 않았습니다.');\n" +
                            "                                            } else if (!isUsbConnected) {\n" +
                            "                                                btn.setAttribute('title', 'PLC가 연결되지 않았습니다.');\n" +
                            "                                            }\n" +
                            "                                        }\n" +
                            "                                    }\n" +
                            "                                });\n" +
                            "                            } else {\n" +
                            "                                controlButton.style.backgroundColor = '#4CAF50';\n" +
                            "                                controlButton.style.color = 'white';\n" +
                            "                                controlButton.textContent = '제어 OFF';\n" +
                            "                                \n" +
                            "                                // 제어 OFF 상태가 되면 모든 실행 버튼 비활성화\n" +
                            "                                const executeButtons = document.querySelectorAll('.execute-btn');\n" +
                            "                                executeButtons.forEach(btn => {\n" +
                            "                                    btn.setAttribute('disabled', 'true');\n" +
                            "                                    btn.style.cursor = 'not-allowed';\n" +
                            "                                    btn.style.opacity = '0.5';\n" +
                            "                                    btn.setAttribute('title', '제어 버튼을 먼저 활성화해주세요.');\n" +
                            "                                });\n" +
                            "                            }\n" +
                    "                        }\n" +
                    "                    }\n" +
                    "                    \n" +
                    "                    updateTestItemList(data);\n" +
                    "                    \n" +
                    "                    // 재검사 버튼 상태 업데이트 (제어 버튼이 ON이면 비활성화)\n" +
                    "                    const restartButton = document.getElementById('restart-button');\n" +
                    "                    if (restartButton) {\n" +
                    "                        if (isControlEnabled) {\n" +
                    "                            restartButton.style.backgroundColor = '#ccc';\n" +
                    "                            restartButton.style.color = '#666';\n" +
                    "                            restartButton.style.cursor = 'not-allowed';\n" +
                    "                            restartButton.setAttribute('disabled', 'true');\n" +
                    "                            restartButton.onclick = null;\n" +
                    "                        } else {\n" +
                    "                            restartButton.style.backgroundColor = '#4CAF50';\n" +
                    "                            restartButton.style.color = 'white';\n" +
                    "                            restartButton.style.cursor = 'pointer';\n" +
                    "                            restartButton.removeAttribute('disabled');\n" +
                    "                            restartButton.onclick = restartTest;\n" +
                    "                        }\n" +
                    "                    }\n" +
                    "                    \n" +
                    "                    // 타임스탬프 업데이트\n" +
                    "                    const timestampEl = document.getElementById('timestamp');\n" +
                    "                    if (timestampEl) timestampEl.textContent = '마지막 업데이트: ' + data.timestamp + ' (5초마다 자동 갱신)';\n" +
                    "                })\n" +
                    "                .catch(error => {\n" +
                    "                    console.error('Update error:', error);\n" +
                    "                    \n" +
                    "                    // 웹 화면에서 '제어 OFF' 이벤트를 발생시키기 전에 안드로이드 애플리케이션에서 종료가 발생한 경우\n" +
                    "                    if (isControlEnabled) {\n" +
                    "                        // 제어 ON 상태일 때 연결이 끊어진 경우\n" +
                    "                        console.log('Android app connection lost while control is ON');\n" +
                    "                        \n" +
                    "                        // 제어 상태를 OFF로 변경\n" +
                    "                        isControlEnabled = false;\n" +
                    "                        \n" +
                    "                        // 제어 버튼 스타일 업데이트\n" +
                    "                        const controlButton = document.getElementById('control-button');\n" +
                    "                        if (controlButton) {\n" +
                    "                            controlButton.style.backgroundColor = '#4CAF50';\n" +
                    "                            controlButton.style.color = 'white';\n" +
                    "                            controlButton.textContent = '제어 OFF';\n" +
                    "                        }\n" +
                    "                        \n" +
                    "                        // 재검사 버튼 활성화\n" +
                    "                        const restartButton = document.getElementById('restart-button');\n" +
                    "                        if (restartButton) {\n" +
                    "                            restartButton.style.backgroundColor = '#4CAF50';\n" +
                    "                            restartButton.style.color = 'white';\n" +
                    "                            restartButton.style.cursor = 'pointer';\n" +
                    "                            restartButton.removeAttribute('disabled');\n" +
                    "                            restartButton.onclick = restartTest;\n" +
                        "                        }\n" +
                        "                        \n" +
                        "                        // 마지막으로 받은 종료 원인 확인하여 적절한 메시지 표시\n" +
                        "                        let shutdownMessage = '어플리케이션이 종료되었습니다.'; // 기본 메시지\n" +
                        "                        if (typeof lastShutdownReason !== 'undefined' && lastShutdownReason !== null) {\n" +
                        "                            if (lastShutdownReason === 'CONTROL_MODE_EXIT') {\n" +
                        "                                shutdownMessage = '단말이 연결을 종료했습니다.';\n" +
                        "                            } else if (lastShutdownReason === 'FAB_CLOSE') {\n" +
                        "                                shutdownMessage = '단말이 검사를 종료했습니다.';\n" +
                        "                            } else if (lastShutdownReason === 'UNEXPECTED') {\n" +
                        "                                shutdownMessage = '어플리케이션이 종료되었습니다.';\n" +
                        "                            }\n" +
                        "                        }\n" +
                        "                        \n" +
                        "                        // 연결 종료 메시지 표시\n" +
                        "                        alert(shutdownMessage);\n" +
                        "                    }\n" +
                    "                });\n" +
                    "        }\n" +
                    "        \n" +
                    "        function updateRampStatus(type, connected) {\n" +
                    "            const badges = document.querySelectorAll('.status-badge');\n" +
                    "            badges.forEach(badge => {\n" +
                    "                if (badge.textContent === type) {\n" +
                    "                    badge.className = 'status-badge ' + (connected ? 'connected' : 'disconnected');\n" +
                    "                }\n" +
                    "            });\n" +
                    "        }\n" +
                    "        \n" +
                    "        function updateLeftPanel(data) {\n" +
                    "            // 시리얼 번호\n" +
                    "            const serialEl = document.getElementById('text-serial');\n" +
                    "            if (serialEl) {\n" +
                    "                if (!data.is_test_running) {\n" +
                    "                    // 테스트가 시작되지 않았을 때\n" +
                    "                    serialEl.textContent = '대기 중';\n" +
                    "                } else if (!data.model_id || data.model_id.trim() === '') {\n" +
                    "                    // 테스트가 시작되었지만 시리얼 번호가 공란일 때\n" +
                    "                    serialEl.textContent = 'POP DERIAL NO';\n" +
                    "                } else {\n" +
                    "                    // 시리얼 번호가 있을 때\n" +
                    "                    serialEl.textContent = data.model_id;\n" +
                    "                }\n" +
                    "            }\n" +
                    "            \n" +
                    "            // 모델명\n" +
                    "            const modelEl = document.getElementById('text-model');\n" +
                    "            if (modelEl) modelEl.textContent = data.model_name || '대기 중';\n" +
                    "            \n" +
                    "            // 현재 테스트 프로세스\n" +
                    "            const currentEl = document.getElementById('text-current');\n" +
                    "            if (currentEl) currentEl.textContent = data.current_test_item || '대기 중';\n" +
                    "            \n" +
                    "            // OK/NG 카운트\n" +
                    "            const okCountEl = document.getElementById('count-ok');\n" +
                    "            if (okCountEl) okCountEl.textContent = data.ok_count || 0;\n" +
                    "            \n" +
                    "            const ngCountEl = document.getElementById('count-ng');\n" +
                    "            if (ngCountEl) ngCountEl.textContent = data.ng_count || 0;\n" +
                    "        }\n" +
                    "        \n" +
                    "        function updateMiddleSection(data) {\n" +
                    "            // 기준값\n" +
                    "            const rows = document.querySelectorAll('.middle-section-table tbody tr');\n" +
                    "            if (rows.length >= 3) {\n" +
                    "                // COMP 행\n" +
                    "                const compCells = rows[0].querySelectorAll('.middle-section-value-col');\n" +
                    "                if (compCells.length >= 4) {\n" +
                    "                    compCells[0].textContent = data.comp_value_watt || '0';\n" +
                    "                    compCells[1].textContent = data.comp_upper_value_watt || '0';\n" +
                    "                    compCells[2].textContent = data.comp_lower_value_watt || '0';\n" +
                    "                    compCells[3].textContent = data.comp_watt_value || '0';\n" +
                    "                }\n" +
                    "                // 히터 행\n" +
                    "                const heaterCells = rows[1].querySelectorAll('.middle-section-value-col');\n" +
                    "                if (heaterCells.length >= 4) {\n" +
                    "                    heaterCells[0].textContent = data.heater_value_watt || '0';\n" +
                    "                    heaterCells[1].textContent = data.heater_upper_value_watt || '0';\n" +
                    "                    heaterCells[2].textContent = data.heater_lower_value_watt || '0';\n" +
                    "                    heaterCells[3].textContent = data.heater_watt_value || '0';\n" +
                    "                }\n" +
                    "                // 펌프 행\n" +
                    "                const pumpCells = rows[2].querySelectorAll('.middle-section-value-col');\n" +
                    "                if (pumpCells.length >= 4) {\n" +
                    "                    pumpCells[0].textContent = data.pump_value_watt || '0';\n" +
                    "                    pumpCells[1].textContent = data.pump_upper_value_watt || '0';\n" +
                    "                    pumpCells[2].textContent = data.pump_lower_value_watt || '0';\n" +
                    "                    pumpCells[3].textContent = data.pump_watt_value || '0';\n" +
                    "                }\n" +
                    "            }\n" +
                    "            \n" +
                    "            // 소비전력, 진행시간, 설정시간\n" +
                    "            const largeValues = document.querySelectorAll('.middle-section-large-value');\n" +
                    "            if (largeValues.length >= 3) {\n" +
                    "                largeValues[0].textContent = data.watt_value || '0';\n" +
                    "                largeValues[1].textContent = data.test_total_counter || '0';\n" +
                    "                largeValues[2].textContent = data.total_time_cnt || '0';\n" +
                    "            }\n" +
                    "        }\n" +
                    "        \n" +
                    "        function updateTestItemList(data) {\n" +
                    "            const testList = document.querySelector('.test-list');\n" +
                    "            if (!testList || !data.test_items) return;\n" +
                    "            \n" +
                    "            // 기존 리스트 제거 (헤더 제외)\n" +
                    "            const existingRows = testList.querySelectorAll('.test-item-row');\n" +
                    "            existingRows.forEach(row => row.remove());\n" +
                    "            \n" +
                    "            // 새 리스트 생성\n" +
                    "            if (data.test_items.length === 0) {\n" +
                    "                const emptyRow = document.createElement('div');\n" +
                    "                emptyRow.className = 'test-item-row';\n" +
                    "                emptyRow.innerHTML = '<div class=\"test-item-cell\" style=\"flex: 1; text-align: center; padding: 40px; color: #999;\">테스트 항목이 없습니다.</div><div class=\"test-item-cell action\"></div>';\n" +
                    "                testList.appendChild(emptyRow);\n" +
                    "            } else {\n" +
                    "                data.test_items.forEach((item, index) => {\n" +
                    "                    // 진행시간이 0초이면 어떤 항목도 '진행 중'으로 표시하지 않음\n" +
                    "                    const spentDuration = parseInt(data.spent_duration || 0);\n" +
                    "                    const isCurrent = spentDuration > 0 && (item.is_current || false);\n" +
                    "                    const isCompleted = item.finish_yn === 'Y' || item.finish_yn === 'YES';\n" +
                    "                    \n" +
                    "                    let resultClass = 'status-pending';\n" +
                    "                    let resultText = '대기';\n" +
                    "                    \n" +
                    "                    if (isCurrent) {\n" +
                    "                        resultClass = 'status-running';\n" +
                    "                        resultText = '진행 중';\n" +
                    "                    } else if (isCompleted) {\n" +
                    "                        if (item.result === 'OK' || item.result === 'Y') {\n" +
                    "                            resultClass = 'status-ok';\n" +
                    "                            resultText = 'OK';\n" +
                    "                        } else if (item.result === 'NG' || item.result === 'N') {\n" +
                    "                            resultClass = 'status-ng';\n" +
                    "                            resultText = 'NG';\n" +
                    "                        } else {\n" +
                    "                            resultClass = 'status-ok';\n" +
                    "                            resultText = '완료';\n" +
                    "                        }\n" +
                    "                    }\n" +
                    "                    \n" +
                    "                    const itemName = item.name || item.command || '';\n" +
                    "                    const resultValue = item.result_value || '-';\n" +
                    "                    const rowClass = isCurrent ? 'current' : '';\n" +
                    "                    // 실행 버튼 활성화 조건: command가 있고, 블루투스/서버/PLC가 모두 연결되고, 제어 버튼이 활성화된 상태\n" +
                    "                    const isButtonEnabled = item.command && isBtConnected && isSvrConnected && isUsbConnected && isControlEnabled;\n" +
                    "                    const disabledAttr = !isButtonEnabled ? 'disabled' : '';\n" +
                    "                    let disabledTitle = '';\n" +
                    "                    if (!item.command) {\n" +
                    "                        disabledTitle = ' title=\"명령어가 없습니다.\"';\n" +
                    "                    } else if (!isBtConnected) {\n" +
                    "                        disabledTitle = ' title=\"블루투스가 연결되지 않았습니다.3\"';\n" +
                    "                    } else if (!isSvrConnected) {\n" +
                    "                        disabledTitle = ' title=\"서버가 연결되지 않았습니다.\"';\n" +
                    "                    } else if (!isUsbConnected) {\n" +
                    "                        disabledTitle = ' title=\"PLC가 연결되지 않았습니다.\"';\n" +
                    "                    } else if (!isControlEnabled) {\n" +
                    "                        disabledTitle = ' title=\"제어 버튼을 먼저 활성화해주세요.\"';\n" +
                    "                    }\n" +
                    "                    \n" +
                    "                    const row = document.createElement('div');\n" +
                    "                    row.className = 'test-item-row ' + rowClass;\n" +
                    "                    row.innerHTML = \n" +
                    "                        '<div class=\"test-item-cell no\">' + (index + 1) + '</div>' +\n" +
                    "                        '<div class=\"test-item-cell name\">' + itemName + '</div>' +\n" +
                    "                        '<div class=\"test-item-cell result ' + resultClass + '\">' + resultText + '</div>' +\n" +
                    "                        '<div class=\"test-item-cell value\">' + resultValue + '</div>' +\n" +
                    "                        '<div class=\"test-item-cell action\">' +\n" +
                    "                            '<button class=\"execute-btn\" onclick=\"executeTestItem(\\'' + item.command + '\\')\" ' + disabledAttr + disabledTitle + '>실행</button>' +\n" +
                    "                        '</div>';\n" +
                    "                    testList.appendChild(row);\n" +
                    "                });\n" +
                    "            }\n" +
                    "        }\n" +
                    "        \n" +
                    "        // 5초마다 자동 업데이트\n" +
                    "        setInterval(updateStatus, 1000);\n" +
                    "        \n" +
                    "        // 페이지 로드 시 즉시 업데이트\n" +
                    "        updateStatus();\n" +
                    "        \n" +
                    "        function restartTest() {\n" +
                    "            // 제어 버튼이 ON 상태이면 재검사 불가\n" +
                    "            if (isControlEnabled) {\n" +
                    "                alert('제어 버튼이 활성화된 상태에서는 재검사를 할 수 없습니다.\\n제어 버튼을 먼저 비활성화해주세요.');\n" +
                    "                return;\n" +
                    "            }\n" +
                    "            \n" +
                    "            if (!confirm('재검사를 시작하시겠습니까?\\n현재 진행 중인 테스트가 초기화됩니다.')) {\n" +
                    "                return;\n" +
                    "            }\n" +
                    "            \n" +
                    "            // 재검사 버튼 상태 업데이트 (처리 중)\n" +
                    "            const restartButton = document.getElementById('restart-button');\n" +
                    "            if (restartButton) {\n" +
                    "                restartButton.style.backgroundColor = '#FF9800';\n" +
                    "                restartButton.style.color = 'white';\n" +
                    "                restartButton.textContent = '재검사 중...';\n" +
                    "                restartButton.style.cursor = 'not-allowed';\n" +
                    "            }\n" +
                    "            \n" +
                    "            fetch('/test/restart', {\n" +
                    "                method: 'POST'\n" +
                    "            })\n" +
                    "            .then(response => {\n" +
                    "                if (!response.ok) {\n" +
                    "                    throw new Error('HTTP error! status: ' + response.status);\n" +
                    "                }\n" +
                    "                return response.text().then(text => {\n" +
                    "                    if (!text || text.trim() === '') {\n" +
                    "                        throw new Error('Empty response');\n" +
                    "                    }\n" +
                    "                    try {\n" +
                    "                        return JSON.parse(text);\n" +
                    "                    } catch (e) {\n" +
                    "                        throw new Error('Invalid JSON');\n" +
                    "                    }\n" +
                    "                });\n" +
                    "            })\n" +
                    "            .then(data => {\n" +
                    "                if (data && data.success) {\n" +
                    "                    // 재검사 버튼 상태 업데이트 (완료)\n" +
                    "                    if (restartButton) {\n" +
                    "                        restartButton.style.backgroundColor = '#4CAF50';\n" +
                    "                        restartButton.style.color = 'white';\n" +
                    "                        restartButton.textContent = '재검사 완료';\n" +
                    "                        restartButton.style.cursor = 'pointer';\n" +
                    "                        // 2초 후 기본 상태로 복귀\n" +
                    "                        setTimeout(() => {\n" +
                    "                            restartButton.style.backgroundColor = 'white';\n" +
                    "                            restartButton.style.color = 'black';\n" +
                    "                            restartButton.textContent = '재검사';\n" +
                    "                        }, 2000);\n" +
                    "                    }\n" +
                    "                    alert('재검사가 시작되었습니다.');\n" +
                    "                    // 데이터 업데이트\n" +
                    "                    setTimeout(() => updateStatus(), 1000);\n" +
                    "                } else {\n" +
                    "                    // 재검사 버튼 상태 복귀 (실패)\n" +
                    "                    if (restartButton) {\n" +
                    "                        restartButton.style.backgroundColor = 'white';\n" +
                    "                        restartButton.style.color = 'black';\n" +
                    "                        restartButton.textContent = '재검사';\n" +
                    "                        restartButton.style.cursor = 'pointer';\n" +
                    "                    }\n" +
                    "                    alert('재검사 실패: ' + (data && data.message ? data.message : '알 수 없는 오류'));\n" +
                    "                }\n" +
                    "            })\n" +
                    "            .catch(error => {\n" +
                    "                console.error('Error:', error);\n" +
                    "                // 재검사 버튼 상태 복귀 (에러)\n" +
                    "                if (restartButton) {\n" +
                    "                    restartButton.style.backgroundColor = 'white';\n" +
                    "                    restartButton.style.color = 'black';\n" +
                    "                    restartButton.textContent = '재검사';\n" +
                    "                    restartButton.style.cursor = 'pointer';\n" +
                    "                }\n" +
                    "                alert('재검사 중 오류가 발생했습니다.');\n" +
                    "            });\n" +
                    "        }\n" +
                    "        \n" +
                    "        function controlTest() {\n" +
                    "            console.log('>>>>>>>>>>>>>>> controlTest.clientSessionId.0 ' + clientSessionId);\n" +
                    "            if(clientSessionId=='') { \n" +
                    "                clientSessionId = generateSessionId(); \n" +
                    "            }\n" +
                    "            console.log('>>>>>>>>>>>>>>> controlTest.clientSessionId.1 ' + clientSessionId);\n" +
                    "            // 테스트 진행 중인지 확인 (로컬 변수 또는 서버에서 최신 상태 확인)\n" +
                    "            // 먼저 현재 상태를 확인하기 위해 서버에 요청\n" +
                    "            fetch('/status/json')\n" +
                    "                .then(response => response.text().then(text => {\n" +
                    "                    if (!text || text.trim() === '') return null;\n" +
                    "                    try {\n" +
                    "                        return JSON.parse(text);\n" +
                    "                    } catch (e) {\n" +
                    "                        return null;\n" +
                    "                    }\n" +
                    "                }))\n" +
                    "                .then(statusData => {\n" +
                    "                    if (statusData && statusData.is_test_running) {\n" +
                    "                        alert('테스트 중에는 제어가 불가능합니다.');\n" +
                    "                        return;\n" +
                    "                    }\n" +
                    "                    \n" +
                    "                    // 테스트가 진행 중이 아니면 제어 요청 진행\n" +
                    "                    console.log('>>>>>>>>>>>>>>> controlTest.0');\n" +
                    "                    performControlToggle();\n" +
                    "                })\n" +
                    "                .catch(error => {\n" +
                    "                    console.error('Error checking test status:', error);\n" +
                    "                    // 상태 확인 실패 시에도 제어 요청 시도 (서버에서 검증)\n" +
                    "                    console.log('>>>>>>>>>>>>>>> controlTest.1');\n" +
                    "                    performControlToggle();\n" +
                    "                });\n" +
                    "        }\n" +
                    "        \n" +
                    "        function performControlToggle() {\n" +
                    "            // 서버에 타이머 제어 요청\n" +
                    "            console.log('>>>>>>>>>>>>>>> clientSessionId ' + clientSessionId);\n" +
                    "            const formDataToggle = new FormData();\n" +
                    "            formDataToggle.append('session_id', clientSessionId);\n" +
                    "            fetch('/test/control/toggle', {\n" +
                    "                method: 'POST',\n" +
                    "                body: formDataToggle\n" +
                    "            })\n" +
                    "            .then(response => {\n" +
                    "                const contentType = response.headers.get('content-type');\n" +
                    "                const isJson = contentType && contentType.includes('application/json');\n" +
                    "                \n" +
                    "                return response.text().then(text => {\n" +
                    "                    if (!text || text.trim() === '') {\n" +
                    "                        if (!response.ok) {\n" +
                    "                            throw new Error('Empty response from server (status: ' + response.status + ')');\n" +
                    "                        }\n" +
                    "                        return null;\n" +
                    "                    }\n" +
                    "                    \n" +
                    "                    if (isJson) {\n" +
                    "                        try {\n" +
                    "                            const jsonData = JSON.parse(text);\n" +
                    "                            // HTTP 에러 상태이지만 JSON 응답이 있는 경우 메시지 포함\n" +
                    "                            if (!response.ok) {\n" +
                    "                                const error = new Error(jsonData.message || 'HTTP error! status: ' + response.status);\n" +
                    "                                error.data = jsonData;\n" +
                    "                                throw error;\n" +
                    "                            }\n" +
                    "                            return jsonData;\n" +
                    "                        } catch (e) {\n" +
                    "                            if (e.data) {\n" +
                    "                                throw e; // 이미 처리된 JSON 에러\n" +
                    "                            }\n" +
                    "                            throw new Error('Invalid JSON: ' + text.substring(0, 100));\n" +
                    "                        }\n" +
                    "                    } else {\n" +
                    "                        if (!response.ok) {\n" +
                    "                            throw new Error('HTTP error! status: ' + response.status + ', response: ' + (text.substring(0, 100) || 'empty'));\n" +
                    "                        }\n" +
                    "                        throw new Error('Invalid response format. Expected JSON, got: ' + (text.substring(0, 100) || 'empty'));\n" +
                    "                    }\n" +
                    "                });\n" +
                    "            })\n" +
                    "            .then(data => {\n" +
                    "                if (data && data.success) {\n" +
                    "                    // 제어 버튼 상태 업데이트\n" +
                    "                    const previousControlState = isControlEnabled;\n" +
                    "                    isControlEnabled = data.timer_running || false;\n" +
                    "                    \n" +
                    "                    // 제어 ON 상태로 변경되었을 때 제어 시작 신호 전달\n" +
                    "                    if (!previousControlState && isControlEnabled) {\n" +
                    "                                                // 제어를 사용하던 사용자가 제어를 시작하는 신호\n" +
                        "                        const formDataStart2 = new FormData();\n" +
                        "                        formDataStart2.append('session_id', clientSessionId);\n" +
                        "                        fetch('/test/control/start', {\n" +
                        "                            method: 'POST',\n" +
                        "                            body: formDataStart2\n" +
                        "                        })\n" +
                    "                        .then(response => {\n" +
                    "                            if (response.ok) {\n" +
                    "                                console.log('Control start signal sent successfully');\n" +
                    "                            }\n" +
                    "                        })\n" +
                    "                        .catch(error => {\n" +
                    "                            console.error('Error sending control start signal:', error);\n" +
                    "                        });\n" +
                    "                    }\n" +
                    "                    \n" +
                    "                    // 제어 OFF 상태로 변경되었을 때 제어 종료 신호 전달\n" +
                    "                    if (previousControlState && !isControlEnabled) {\n" +
                    "                                                // 제어를 사용하던 사용자가 제어를 종료하는 신호\n" +
                        "                        const formDataEnd2 = new FormData();\n" +
                        "                        formDataEnd2.append('session_id', clientSessionId);\n" +
                        "                        fetch('/test/control/end', {\n" +
                        "                            method: 'POST',\n" +
                        "                            body: formDataEnd2\n" +
                        "                        })\n" +
                    "                        .then(response => {\n" +
                    "                            if (response.ok) {\n" +
                    "                                console.log('Control end signal sent successfully');\n" +
                    "                            }\n" +
                    "                        })\n" +
                    "                        .catch(error => {\n" +
                    "                            console.error('Error sending control end signal:', error);\n" +
                    "                        });\n" +
                    "                    }\n" +
                    "                    \n" +
                    "                    // 제어 버튼 스타일 업데이트 (ON/OFF 상태에 따라 다른 색상)\n" +
                    "                    const controlButton = document.getElementById('control-button');\n" +
                    "                    if (controlButton) {\n" +
                    "                        if (isControlEnabled) {\n" +
                    "                            controlButton.style.backgroundColor = '#FF9800';\n" +
                    "                            controlButton.style.color = 'white';\n" +
                    "                            controlButton.textContent = '제어 ON';\n" +
                    "                            \n" +
                    "                            // 제어 ON 상태가 되면 즉시 실행 버튼 활성화\n" +
                    "                            const executeButtons = document.querySelectorAll('.execute-btn');\n" +
                    "                            executeButtons.forEach(btn => {\n" +
                    "                                const command = btn.getAttribute('onclick');\n" +
                    "                                if (command && command.includes('executeTestItem')) {\n" +
                    "                                    // 블루투스, 서버, PLC 연결 상태 확인\n" +
                    // "                                    if (isBtConnected && isSvrConnected && isUsbConnected) {\n" +
                    "                                        btn.removeAttribute('disabled');\n" +
                    "                                        btn.setAttribute('disabled', 'false');\n" +
                    "                                        btn.style.cursor = 'pointer';\n" +
                    "                                        btn.style.opacity = '1';\n" +
                    "                                        btn.removeAttribute('title');\n" +
                    "                                        console.log('>>>>>>>>>>>>>>>>>>>>>>>> 0.command ' + command + ' / command.includes(executeTestItem) ' + command.includes('executeTestItem'))\n" +
                    /*
                    "                                    } else {\n" +
                    "                                        btn.setAttribute('disabled', 'true');\n" +
                    "                                        btn.style.cursor = 'not-allowed';\n" +
                    "                                        btn.style.opacity = '0.5';\n" +
                    "                                        if (!isBtConnected) {\n" +
                    "                                            btn.setAttribute('title', '블루투스가 연결되지 않았습니다.4');\n" +
                    "                                        } else if (!isSvrConnected) {\n" +
                    "                                            btn.setAttribute('title', '서버가 연결되지 않았습니다.');\n" +
                    "                                        } else if (!isUsbConnected) {\n" +
                    "                                            btn.setAttribute('title', 'PLC가 연결되지 않았습니다.');\n" +
                    "                                        }\n" +
                    "                                    }\n" +
                    */
                    "                                }\n" +
                    "                            });\n" +
                    "                        } else {\n" +
                    "                            controlButton.style.backgroundColor = '#4CAF50';\n" +
                    "                            controlButton.style.color = 'white';\n" +
                    "                            controlButton.textContent = '제어 OFF';\n" +
                    "                            \n" +
                    "                            // 제어 OFF 상태가 되면 모든 실행 버튼 비활성화\n" +
                    "                            const executeButtons = document.querySelectorAll('.execute-btn');\n" +
                    "                            executeButtons.forEach(btn => {\n" +
                    "                                btn.setAttribute('disabled', 'true');\n" +
                    "                                btn.style.cursor = 'not-allowed';\n" +
                    "                                btn.style.opacity = '0.5';\n" +
                    "                                btn.setAttribute('title', '제어 버튼을 먼저 활성화해주세요.');\n" +
                    "                            });\n" +
                    "                        }\n" +
                    "                    }\n" +
                    "                    \n" +
                    "                    // 재검사 버튼 상태 업데이트 (제어 버튼이 ON이면 비활성화)\n" +
                    "                    const restartButton = document.getElementById('restart-button');\n" +
                    "                    if (restartButton) {\n" +
                    "                        if (isControlEnabled) {\n" +
                    "                            restartButton.style.backgroundColor = '#ccc';\n" +
                    "                            restartButton.style.color = '#666';\n" +
                    "                            restartButton.style.cursor = 'not-allowed';\n" +
                    "                            restartButton.setAttribute('disabled', 'true');\n" +
                    "                            restartButton.onclick = null;\n" +
                    "                        } else {\n" +
                    "                            restartButton.style.backgroundColor = '#4CAF50';\n" +
                    "                            restartButton.style.color = 'white';\n" +
                    "                            restartButton.style.cursor = 'pointer';\n" +
                    "                            restartButton.removeAttribute('disabled');\n" +
                    "                            restartButton.onclick = restartTest;\n" +
                    "                        }\n" +
                    "                    }\n" +
                    "                    \n" +
                    "                    // 실행 버튼 상태 즉시 업데이트를 위해 상태 데이터를 가져와서 updateTestItemList 호출\n" +
                    "                    fetch('/status/json')\n" +
                    "                        .then(response => response.text().then(text => {\n" +
                    "                            if (!text || text.trim() === '') return null;\n" +
                    "                            try {\n" +
                    "                                return JSON.parse(text);\n" +
                    "                            } catch (e) {\n" +
                    "                                return null;\n" +
                    "                            }\n" +
                    "                        }))\n" +
                    "                        .then(statusData => {\n" +
                    "                            if (statusData) {\n" +
                    "                                // updateTestItemList(statusData);\n" +
                    "                            }\n" +
                    "                        })\n" +
                    "                        .catch(err => console.error('Failed to update test item list:', err));\n" +
                    "                    \n" +
                    "                    // 전체 상태 업데이트\n" +
                    "                    updateStatus();\n" +
                    "                } else {\n" +
                    "                    alert('제어 실패: ' + (data && data.message ? data.message : '알 수 없는 오류'));\n" +
                    "                }\n" +
                    "            })\n" +
                    "            .catch(error => {\n" +
                    "                console.error('Error:', error);\n" +
                    "                // 에러 객체에 data 속성이 있으면 서버에서 반환한 메시지 사용\n" +
                    "                const errorMessage = (error.data && error.data.message) ? error.data.message : (error.message || '알 수 없는 오류');\n" +
                    "                alert(errorMessage);\n" +
                    "            });\n" +
                    "        }\n" +
                    "        \n" +
                    "        function executeTestItem(command) {\n" +
                    "            if (!command || command.trim() === '') {\n" +
                    "                alert('명령어가 없습니다.');\n" +
                    "                return;\n" +
                    "            }\n" +
                    "            \n" +
                    "            if (!isBtConnected) {\n" +
                    "                alert('블루투스가 연결되지 않았습니다.\\n블루투스 연결 후 다시 시도해주세요.');\n" +
                    "                return;\n" +
                    "            }\n" +
                    "            \n" +
                    "            if (!isSvrConnected) {\n" +
                    "                alert('서버가 연결되지 않았습니다.\\n서버 연결 후 다시 시도해주세요.');\n" +
                    "                return;\n" +
                    "            }\n" +
                    "            \n" +
                    "            if (!isUsbConnected) {\n" +
                    "                alert('PLC가 연결되지 않았습니다.\\nPLC 연결 후 다시 시도해주세요.');\n" +
                    "                return;\n" +
                    "            }\n" +
                    "            \n" +
                    "            if (!isControlEnabled) {\n" +
                    "                alert('제어 버튼을 먼저 활성화해주세요.\\n제어 버튼을 클릭하여 활성화한 후 다시 시도해주세요.');\n" +
                    "                return;\n" +
                    "            }\n" +
                    "            \n" +
                    "            if (!confirm('테스트 항목 [' + command + ']을(를) 실행하시겠습니까?')) {\n" +
                    "                return;\n" +
                    "            }\n" +
                    "            \n" +
                    "            const formData = new FormData();\n" +
                    "            formData.append('command', command);\n" +
                    "            \n" +
                    "            fetch('/test_item/execute', {\n" +
                    "                method: 'POST',\n" +
                    "                body: formData\n" +
                    "            })\n" +
                    "            .then(response => response.json())\n" +
                    "            .then(data => {\n" +
                    "                if (data.success) {\n" +
                    "                    alert('테스트 항목이 실행되었습니다.');\n" +
                    "                    // 페이지 새로고침 대신 데이터만 업데이트\n" +
                    "                    setTimeout(() => updateStatus(), 1000);\n" +
                    "                } else {\n" +
                    "                    alert('실행 실패: ' + (data.message || '알 수 없는 오류'));\n" +
                    "                }\n" +
                    "            })\n" +
                    "            .catch(error => {\n" +
                    "                console.error('Error:', error);\n" +
                    "                alert('실행 중 오류가 발생했습니다.');\n" +
                    "            });\n" +
                    "        }\n" +
                    "    </script>\n" +
                    "</body>\n" +
                    "</html>";
            
            return newResponse(Response.Status.OK, NanoHTTPD.MIME_HTML, html);
        } catch (Exception e) {
            Log.e(TAG, "테스트 상태 페이지 생성 오류", e);
            return newResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_HTML, 
                "<html><body><h1>오류 발생</h1><p>" + e.getMessage() + "</p></body></html>");
        }
    }
    
    /**
     * 테스트 상태 JSON API
     */
    private Response handleTestStatusJson(IHTTPSession session) {
        try {
            TestStatusInfo status = getTestStatusInfo();
            
            // OK/NG 카운트 계산
            int okCount = 0;
            int ngCount = 0;
            for (TestStatusInfo.TestItemInfo item : status.testItemList) {
                if ("Y".equals(item.finishYn) || "YES".equals(item.finishYn)) {
                    if ("OK".equals(item.result) || "Y".equals(item.result)) {
                        okCount++;
                    } else if ("NG".equals(item.result) || "N".equals(item.result)) {
                        ngCount++;
                    }
                }
            }
            
            // 타이머 상태 확인
            boolean isTimerRunning = false;
            try {
                Object activity = null;

                try {
                    Class<?> activityModelClass = Class.forName("itf.com.app.lms.ActivityModelTestProcess");
                    java.lang.reflect.Method getMainActivityMethod = activityModelClass.getMethod("getMainActivity");
                    activity = getMainActivityMethod.invoke(null);
                } catch (Exception e) {
                    Log.d(TAG, "Could not get Activity instance: " + e.getMessage());
                }
                /*
                try {
                    Class<?> activityModelClass = Class.forName("itf.com.app.simple_line_test_ovio_new.ActivityModel_0002");
                    java.lang.reflect.Method getMainActivityMethod = activityModelClass.getMethod("getMainActivity");
                    activity = getMainActivityMethod.invoke(null);
                } catch (Exception e) {
                    try {
                        Class<?> activityModelClass = Class.forName("itf.com.app.simple_line_test_ovio_new.ActivityModel_0001");
                        java.lang.reflect.Method getMainActivityMethod = activityModelClass.getMethod("getMainActivity");
                        activity = getMainActivityMethod.invoke(null);
                    } catch (Exception e2) {
                        try {
                            Class<?> activityModelClass = Class.forName("itf.com.app.simple_line_test_ovio_new.ActivityModel_0003");
                            java.lang.reflect.Method getMainActivityMethod = activityModelClass.getMethod("getMainActivity");
                            activity = getMainActivityMethod.invoke(null);
                        } catch (Exception e3) {
                            // Activity를 찾을 수 없음
                        }
                    }
                }
                */
                
                if (activity != null) {
                    try {
                        java.lang.reflect.Field tmrBTMessageSendField = activity.getClass().getDeclaredField("tmrBTMessageSend");
                        tmrBTMessageSendField.setAccessible(true);
                        Object tmrBTMessageSendObj = tmrBTMessageSendField.get(activity);
                        isTimerRunning = (tmrBTMessageSendObj != null);
                    } catch (Exception e) {
                        // 타이머 필드를 찾을 수 없음
                    }
                }
            } catch (Exception e) {
                // 타이머 상태 확인 실패
            }
            
            JSONObject json = new JSONObject();
            json.put("model_id", status.modelId);
            json.put("model_name", status.modelName);
            json.put("current_test_item", status.currentTestItem);
            json.put("test_item_idx", status.testItemIdx);
            json.put("total_test_items", status.totalTestItems);
            json.put("test_item_counter", status.testItemCounter);
            json.put("test_total_counter", status.testTotalCounter);
            json.put("is_test_running", status.isTestRunning);
            json.put("timer_running", isTimerRunning);  // 타이머 상태 추가
            json.put("timestamp", getCurrentDatetime("yyyy-MM-dd HH:mm:ss"));
            
            // 상단 정보
            json.put("unit_id", status.unitId);
            json.put("temperature", status.temperature);
            json.put("watt_value", status.wattValue);
            json.put("total_time_cnt", status.totalTimeCnt);
            json.put("spent_duration", status.spentDuration);
            
            // 램프 상태
            json.put("ramp_svr", status.rampSvr);
            json.put("ramp_bt", status.rampBt);
            json.put("ramp_usb", status.rampUsb);
            
            // 제어 모드 상태
            json.put("is_control_mode", status.isControlMode);
            json.put("is_control_on", status.isControlOn);
            
            // 마지막 종료 신호 (웹 클라이언트가 확인할 수 있도록)
            String shutdownReason = getLastShutdownReason();
            if (shutdownReason != null && !shutdownReason.isEmpty()) {
                json.put("last_shutdown_reason", shutdownReason);
            } else {
                json.put("last_shutdown_reason", JSONObject.NULL);
            }
            
            // 중간 섹션 데이터
            json.put("comp_value_watt", status.compValueWatt);
            json.put("heater_value_watt", status.heaterValueWatt);
            json.put("pump_value_watt", status.pumpValueWatt);
            json.put("comp_upper_value_watt", status.compUpperValueWatt);
            json.put("heater_upper_value_watt", status.heaterUpperValueWatt);
            json.put("pump_upper_value_watt", status.pumpUpperValueWatt);
            json.put("comp_lower_value_watt", status.compLowerValueWatt);
            json.put("heater_lower_value_watt", status.heaterLowerValueWatt);
            json.put("pump_lower_value_watt", status.pumpLowerValueWatt);
            json.put("comp_watt_value", status.compWattValue);
            json.put("heater_watt_value", status.heaterWattValue);
            json.put("pump_watt_value", status.pumpWattValue);
            
            // OK/NG 카운트
            json.put("ok_count", okCount);
            json.put("ng_count", ngCount);
            
            JSONArray testItemsArray = new JSONArray();
            for (TestStatusInfo.TestItemInfo item : status.testItemList) {
                JSONObject itemJson = new JSONObject();
                itemJson.put("seq", item.seq);
                itemJson.put("name", item.name);
                itemJson.put("command", item.command);
                itemJson.put("result", item.result);
                itemJson.put("finish_yn", item.finishYn);
                itemJson.put("result_value", item.resultValue);
                // 진행시간이 0초이면 어떤 항목도 '진행 중'으로 표시하지 않음
                boolean isCurrent = status.spentDuration > 0 && item.command.equals(status.currentTestItem);
                itemJson.put("is_current", isCurrent);
                testItemsArray.put(itemJson);
            }
            json.put("test_items", testItemsArray);
            
            return newResponse(Response.Status.OK, Constants.HTTP.CONTENT_TYPE_JSON, json.toString());
        } catch (Exception e) {
            Log.e(TAG, "테스트 상태 JSON 생성 오류", e);
            JSONObject errorJson = new JSONObject();
            try {
                errorJson.put("error", true);
                errorJson.put("message", e.getMessage());
            } catch (Exception e2) {
                // JSON 생성 실패 시 빈 객체 반환
            }
            return newResponse(Response.Status.INTERNAL_ERROR, Constants.HTTP.CONTENT_TYPE_JSON, errorJson.toString());
        }
    }

    /**
     * 검사 이력 목록 웹페이지 생성 - activity_test_history.xml 레이아웃/디자인을 최대한 유사하게 구현
     * URL: GET /test_history
     */
    private Response handleTestHistoryListPage(IHTTPSession session) {
        try {
            if (context == null) {
                return newResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_HTML,
                        "<html><body><h1>서버 초기화 오류</h1><p>context is null</p></body></html>");
            }

            List<Map<String, String>> list = TestData.selectTestHistory(context, "");
            if (list == null) list = new ArrayList<>();

            StringBuilder rows = new StringBuilder();
            int no = 1;
            for (int i = list.size() - 1; i >= 0; i--) {
                Map<String, String> m = list.get(i);
                if (m == null) continue;

                String seq = safe(m.get("clm_test_history_seq"));
                String tsRaw = safe(m.get("clm_test_timestamp"));
                String ts = escapeHtml(formatTimestampToUi(tsRaw));
                String modelName = escapeHtml(safe(m.get("clm_test_model_name")));
                String result = safe(m.get("clm_test_result"));
                String okCnt = escapeHtml(safe(m.get("clm_test_ok_count")));
                String ngCnt = escapeHtml(safe(m.get("clm_test_ng_count")));

                String resultClass = "result";
                if ("OK".equalsIgnoreCase(result)) resultClass += " ok";
                else if ("NG".equalsIgnoreCase(result)) resultClass += " ng";

                rows.append("<div class=\"row\">")
                        .append("<div class=\"cell no\">").append(no++).append("</div>")
                        .append("<div class=\"cell datetime\">").append(ts).append("</div>")
                        .append("<div class=\"cell model\">").append(modelName).append("</div>")
                        .append("<div class=\"cell ").append(resultClass).append("\">").append(escapeHtml(result)).append("</div>")
                        .append("<div class=\"cell okcnt\">").append(okCnt).append("</div>")
                        .append("<div class=\"cell ngcnt\">").append(ngCnt).append("</div>")
                        .append("<div class=\"cell detail\"><a class=\"detailBtn\" href=\"/test_history/detail?seq=")
                        .append(urlEncode(seq))
                        .append("\">상세</a></div>")
                        .append("</div>");
            }

            String html = "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "  <meta charset=\"UTF-8\" />\n" +
                    "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />\n" +
                    "  <link rel=\"stylesheet\" href=\"https://cdn.jsdelivr.net/gh/moonspam/NanumSquare@2.0/nanumsquare.css\">\n" +
                    "  <title>검사 이력</title>\n" +
                    "  <style>\n" +
                    "    * { margin:0; padding:0; box-sizing:border-box; font-family:'NanumSquare','나눔스퀘어',sans-serif !important; }\n" +
                    "    body { background:#EEEEEE; }\n" +
                    "    /* 상단 블루 바 (activity_test_history.xml 상단 영역 유사) */\n" +
                    "    .topbar { height:50px; background:#092c74; display:flex; align-items:center; padding:0 10px; }\n" +
                    "    .logoImg { width:107px; height:30px; object-fit:contain; }\n" +
                    "    .spacer { flex:1; }\n" +
                    "    .ver { color:#fff; font-size:18px; opacity:0.9; }\n" +
                    "    /* 회색 헤더 (ll_popup_header 유사) */\n" +
                    "    .header { background:#333333; padding:10px; display:flex; align-items:center; justify-content:center; position:relative; }\n" +
                    "    .headerTitle { color:#fff; font-size:32px; height:53px; display:flex; align-items:center; justify-content:center; }\n" +
                    "    .headerAction { position:absolute; right:10px; top:50%; transform:translateY(-50%); color:#fff; text-decoration:none; font-size:16px; opacity:0.9; }\n" +
                    "    /* 리스트 헤더 */\n" +
                    "    .listHeader { height:65px; background:#333333; display:flex; align-items:center; padding:5px; }\n" +
                    "    .listHeader .cell { color:#fff; font-size:22px; font-weight:700; text-align:center; }\n" +
                    "    /* 리스트 row */\n" +
                    "    .list { background:#FFFFFF; }\n" +
                    "    .row { min-height:65px; display:flex; align-items:center; padding:5px; border-bottom:1px solid rgba(0,0,0,0.08); }\n" +
                    "    .row:nth-child(even){ background:#FAFAFA; }\n" +
                    "    .cell { padding:10px; color:#333333; font-size:22px; }\n" +
                    "    .no { width:5%; text-align:center; }\n" +
                    "    .datetime { width:30%; text-align:center; }\n" +
                    "    .model { width:30%; text-align:center; font-weight:700; }\n" +
                    "    .result { width:10%; text-align:center; font-weight:800; }\n" +
                    "    .result.ok { color:#4CAF50; }\n" +
                    "    .result.ng { color:#FF0000; }\n" +
                    "    .okcnt { width:10%; text-align:center; }\n" +
                    "    .ngcnt { width:10%; text-align:center; }\n" +
                    "    .detail { width:5%; text-align:center; }\n" +
                    "    .detailBtn { display:inline-block; padding:6px 10px; border-radius:6px; background:#092c74; color:#fff; text-decoration:none; font-size:16px; font-weight:800; }\n" +
                    "    .container { width:100%; }\n" +
                    "  </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "  <div class=\"container\">\n" +
                    "    <div class=\"topbar\">\n" +
                    "      <img class=\"logoImg\" src=\"" + getOvioLogoDataUri() + "\" alt=\"OVIO\" />\n" +
                    "      <div class=\"spacer\"></div>\n" +
                    "      <div class=\"ver\"></div>\n" +
                    "    </div>\n" +
                    "    <div class=\"header\">\n" +
                    "      <div class=\"headerTitle\">검사 이력</div>\n" +
                    "      <a class=\"headerAction\" href=\"/status\">상태</a>\n" +
                    "    </div>\n" +
                    "    <div class=\"listHeader\">\n" +
                    "      <div class=\"cell no\">No</div>\n" +
                    "      <div class=\"cell datetime\">검사일시</div>\n" +
                    "      <div class=\"cell model\">모델명</div>\n" +
                    "      <div class=\"cell result\">결과</div>\n" +
                    "      <div class=\"cell okcnt\">OK</div>\n" +
                    "      <div class=\"cell ngcnt\">NG</div>\n" +
                    "      <div class=\"cell detail\">상세</div>\n" +
                    "    </div>\n" +
                    "    <div class=\"list\">" + rows + "</div>\n" +
                    "  </div>\n" +
                    "</body>\n" +
                    "</html>";

            return newResponse(Response.Status.OK, NanoHTTPD.MIME_HTML, html);
        } catch (Exception e) {
            Log.e(TAG, "검사 이력 페이지 생성 오류", e);
            return newResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_HTML,
                    "<html><body><h1>오류 발생</h1><pre>" + escapeHtml(String.valueOf(e)) + "</pre></body></html>");
        }
    }

    /**
     * 검사 이력 상세 페이지
     * URL: GET /test_history/detail?seq=...
     */
    private Response handleTestHistoryDetailPage(IHTTPSession session) {
        try {
            if (context == null) {
                return newResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_HTML,
                        "<html><body><h1>서버 초기화 오류</h1><p>context is null</p></body></html>");
            }

            String seq = "";
            try {
                Map<String, String> p = session.getParms();
                if (p != null) seq = safe(p.get("seq"));
            } catch (Exception ignored) {
            }

            if (seq.isEmpty()) {
                return newResponse(Response.Status.BAD_REQUEST, NanoHTTPD.MIME_HTML,
                        "<html><body><h1>잘못된 요청</h1><p>seq 파라미터가 필요합니다.</p></body></html>");
            }

            List<Map<String, String>> details = TestData.selectTestHistoryDetail(context, seq);
            if (details == null) details = new ArrayList<>();

            StringBuilder rows = new StringBuilder();
            for (Map<String, String> d : details) {
                if (d == null) continue;
                rows.append("<tr>")
                        .append("<td>").append(escapeHtml(safe(d.get("clm_test_item_seq")))).append("</td>")
                        .append("<td>").append(escapeHtml(safe(d.get("clm_test_item_name")))).append("</td>")
                        .append("<td>").append(escapeHtml(safe(d.get("clm_test_item_result")))).append("</td>")
                        .append("<td>").append(escapeHtml(safe(d.get("clm_test_item_value")))).append("</td>")
                        .append("<td>").append(escapeHtml(safe(d.get("clm_test_response_value")))).append("</td>")
                        .append("<td>").append(escapeHtml(safe(d.get("clm_test_result_value")))).append("</td>")
                        .append("</tr>");
            }

            String html = "<!DOCTYPE html>\n" +
                    "<html><head>\n" +
                    "  <meta charset=\"UTF-8\" />\n" +
                    "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />\n" +
                    "  <link rel=\"stylesheet\" href=\"https://cdn.jsdelivr.net/gh/moonspam/NanumSquare@2.0/nanumsquare.css\">\n" +
                    "  <title>검사 이력 상세</title>\n" +
                    "  <style>\n" +
                    "    * { margin:0; padding:0; box-sizing:border-box; font-family:'NanumSquare','나눔스퀘어',sans-serif !important; }\n" +
                    "    body { background:#EEEEEE; }\n" +
                    "    .topbar { height:50px; background:#092c74; display:flex; align-items:center; padding:0 10px; }\n" +
                    "    .logoImg { width:107px; height:30px; object-fit:contain; }\n" +
                    "    .header { background:#333333; padding:10px; display:flex; align-items:center; justify-content:center; position:relative; }\n" +
                    "    .headerTitle { color:#fff; font-size:32px; height:53px; display:flex; align-items:center; justify-content:center; }\n" +
                    "    .back { position:absolute; left:10px; top:50%; transform:translateY(-50%); color:#fff; text-decoration:none; font-size:16px; font-weight:800; }\n" +
                    "    .card { background:#fff; margin:10px; padding:10px; border-radius:8px; }\n" +
                    "    table { width:100%; border-collapse:collapse; }\n" +
                    "    th { background:#333333; color:#fff; padding:10px; font-size:18px; }\n" +
                    "    td { padding:10px; border-bottom:1px solid rgba(0,0,0,0.08); font-size:18px; color:#333; }\n" +
                    "  </style>\n" +
                    "</head><body>\n" +
                    "  <div class=\"topbar\"><img class=\"logoImg\" src=\"" + getOvioLogoDataUri() + "\" alt=\"OVIO\" /></div>\n" +
                    "  <div class=\"header\"><a class=\"back\" href=\"/test_history\">목록</a><div class=\"headerTitle\">검사 이력 상세</div></div>\n" +
                    "  <div class=\"card\">\n" +
                    "    <div style=\"font-size:18px; margin-bottom:10px;\"><b>SEQ:</b> " + escapeHtml(seq) + "</div>\n" +
                    "    <table>\n" +
                    "      <thead>\n" +
                    "        <tr>\n" +
                    "          <th style=\"width:8%\">No</th>\n" +
                    "          <th style=\"width:28%\">항목명</th>\n" +
                    "          <th style=\"width:12%\">결과</th>\n" +
                    "          <th style=\"width:17%\">값</th>\n" +
                    "          <th style=\"width:17%\">응답</th>\n" +
                    "          <th style=\"width:18%\">결과값</th>\n" +
                    "        </tr>\n" +
                    "      </thead>\n" +
                    "      <tbody>\n" +
                    rows +
                    "      </tbody>\n" +
                    "    </table>\n" +
                    "  </div>\n" +
                    "</body></html>";

            return newResponse(Response.Status.OK, NanoHTTPD.MIME_HTML, html);
        } catch (Exception e) {
            Log.e(TAG, "검사 이력 상세 페이지 생성 오류", e);
            return newResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_HTML,
                    "<html><body><h1>오류 발생</h1><pre>" + escapeHtml(String.valueOf(e)) + "</pre></body></html>");
        }
    }

    /**
     * OVIO 로고를 Android drawable에서 PNG로 렌더링하여 data URI로 반환 (캐시됨)
     */
    private String getOvioLogoDataUri() {
        String cached = cachedOvioLogoDataUri;
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        try {
            if (context == null) {
                return "";
            }

            Drawable drawable = ContextCompat.getDrawable(context, R.drawable.ovio_logo_top);
            if (drawable == null) {
                return "";
            }

            int w = drawable.getIntrinsicWidth();
            int h = drawable.getIntrinsicHeight();
            if (w <= 0 || h <= 0) {
                // fallback size roughly matching activity_test_history.xml image view (107dp x 50dp)
                float density = context.getResources().getDisplayMetrics().density;
                w = Math.max(1, (int) (107 * density));
                h = Math.max(1, (int) (50 * density));
            }

            Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, w, h);
            drawable.draw(canvas);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            byte[] png = baos.toByteArray();

            String b64 = Base64.encodeToString(png, Base64.NO_WRAP);
            String dataUri = "data:image/png;base64," + b64;
            cachedOvioLogoDataUri = dataUri;
            return dataUri;
        } catch (Exception e) {
            Log.w(TAG, "Failed to generate OVIO logo data URI: " + e.getMessage());
            return "";
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String formatTimestampToUi(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.isEmpty()) return "";
        try {
            String digits = s.replaceAll("[^0-9]", "");
            if (digits.length() >= 14) {
                digits = digits.substring(0, 14);
                Date d = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).parse(digits);
                if (d != null) return new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(d);
            } else if (digits.length() >= 12) {
                digits = digits.substring(0, 12);
                Date d = new SimpleDateFormat("yyyyMMddHHmm", Locale.getDefault()).parse(digits);
                if (d != null) return new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(d);
            }
        } catch (Exception ignored) {
        }
        return s;
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s == null ? "" : s, "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * 테스트 항목 실행 처리
     */
    private Response handleTestItemExecute(IHTTPSession session) {
        try {
            if (context == null) {
                JSONObject errorJson = new JSONObject();
                errorJson.put("success", false);
                errorJson.put("message", "서버 초기화 오류");
                return newResponse(Response.Status.INTERNAL_ERROR, Constants.HTTP.CONTENT_TYPE_JSON, errorJson.toString());
            }
            
            // POST 데이터 파싱
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            String command = session.getParms().get("command");
            
            if (command == null || command.trim().isEmpty()) {
                JSONObject errorJson = new JSONObject();
                errorJson.put("success", false);
                errorJson.put("message", "명령어가 없습니다.");
                return newResponse(Response.Status.BAD_REQUEST, Constants.HTTP.CONTENT_TYPE_JSON, errorJson.toString());
            }
            
            // ActivityModel 인스턴스 가져오기
            Object activity = null;

            try {
                Class<?> activityModelClass = Class.forName("itf.com.app.lms.ActivityModelTestProcess");
                java.lang.reflect.Method getMainActivityMethod = activityModelClass.getMethod("getMainActivity");
                activity = getMainActivityMethod.invoke(null);
            } catch (Exception e) {
                Log.d(TAG, "Could not get Activity instance: " + e.getMessage());
            }

            /*
            try {
                Class<?> activityModelClass = Class.forName("itf.com.app.simple_line_test_ovio_new.ActivityModel_0002");
                java.lang.reflect.Method getMainActivityMethod = activityModelClass.getMethod("getMainActivity");
                activity = getMainActivityMethod.invoke(null);
            } catch (Exception e) {
                try {
                    Class<?> activityModelClass = Class.forName("itf.com.app.simple_line_test_ovio_new.ActivityModel_0001");
                    java.lang.reflect.Method getMainActivityMethod = activityModelClass.getMethod("getMainActivity");
                    activity = getMainActivityMethod.invoke(null);
                } catch (Exception e2) {
                    try {
                        Class<?> activityModelClass = Class.forName("itf.com.app.simple_line_test_ovio_new.ActivityModel_0003");
                        java.lang.reflect.Method getMainActivityMethod = activityModelClass.getMethod("getMainActivity");
                        activity = getMainActivityMethod.invoke(null);
                    } catch (Exception e3) {
                        Log.d(TAG, "Could not get Activity instance: " + e3.getMessage());
                    }
                }
            }
            */
            
            if (activity == null) {
                JSONObject errorJson = new JSONObject();
                errorJson.put("success", false);
                errorJson.put("message", "Activity 인스턴스를 찾을 수 없습니다.");
                return newResponse(Response.Status.INTERNAL_ERROR, Constants.HTTP.CONTENT_TYPE_JSON, errorJson.toString());
            }
            
            // 제어 모드 확인
            boolean isControlMode = false;
            boolean isControlOn = false;
            try {
                java.lang.reflect.Field isControlModeField = activity.getClass().getDeclaredField("isControlMode");
                isControlModeField.setAccessible(true);
                Object isControlModeObj = isControlModeField.get(activity);
                if (isControlModeObj instanceof Boolean) {
                    isControlMode = (Boolean) isControlModeObj;
                }
                
                java.lang.reflect.Field isControlOnField = activity.getClass().getDeclaredField("isControlOn");
                isControlOnField.setAccessible(true);
                Object isControlOnObj = isControlOnField.get(activity);
                if (isControlOnObj instanceof Boolean) {
                    isControlOn = (Boolean) isControlOnObj;
                }
            } catch (Exception e) {
                Log.d(TAG, "Could not check control mode status: " + e.getMessage());
            }
            
            // 제어 모드일 때는 일반 모드와 동일한 검사 로직 수행
            if (isControlMode && isControlOn) {
                try {
                    // 제어 모드 검사 실행 메서드 호출 (일반 모드와 동일한 검사 로직 수행)
                    java.lang.reflect.Method executeControlModeTestItemMethod = activity.getClass().getDeclaredMethod("executeControlModeTestItem", String.class);
                    executeControlModeTestItemMethod.setAccessible(true);
                    executeControlModeTestItemMethod.invoke(activity, command);
                    
                    Log.i(TAG, "Control mode test item executed: " + command);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to execute control mode test item: " + command, e);
                    // 제어 모드 검사 실행 실패 시 일반 sendBtMessage로 폴백
                    try {
                        java.lang.reflect.Method sendBtMessageMethod = activity.getClass().getDeclaredMethod("sendBtMessage", String.class);
                        sendBtMessageMethod.setAccessible(true);
                        sendBtMessageMethod.invoke(activity, command);
                        
                        // 응답 대기 시작
                        java.lang.reflect.Method startWaitingMethod = activity.getClass().getDeclaredMethod("startWaitingForControlResponse", String.class);
                        startWaitingMethod.setAccessible(true);
                        startWaitingMethod.invoke(activity, command);
                        Log.i(TAG, "Fallback: Test item executed with simple response waiting: " + command);
                    } catch (Exception e2) {
                        Log.e(TAG, "Failed to execute test item (fallback): " + command, e2);
                        JSONObject errorJson = new JSONObject();
                        try {
                            errorJson.put("success", false);
                            errorJson.put("message", "명령 실행 실패: " + e2.getMessage());
                        } catch (Exception e3) {
                            // JSON 생성 실패
                        }
                        return newResponse(Response.Status.INTERNAL_ERROR, Constants.HTTP.CONTENT_TYPE_JSON, errorJson.toString());
                    }
                }
            } else {
                // 일반 모드일 때는 단순히 명령만 전송
                try {
                    /*
                    java.lang.reflect.Method sendBtMessageMethod = activity.getClass().getDeclaredMethod("sendBtMessage", String.class);
                    sendBtMessageMethod.setAccessible(true);
                    sendBtMessageMethod.invoke(activity, command);
                    
                    Log.i(TAG, "Test item executed: " + command);
                    */
                    Log.i(TAG, "Test item '" + command + "' execution failed!!!");
                    JSONObject errorJson = new JSONObject();
                    errorJson.put("success", false);
                    errorJson.put("message", "일반 모드에서는 명령 실행 불가!!!");
                    return newResponse(Response.Status.INTERNAL_ERROR, Constants.HTTP.CONTENT_TYPE_JSON, errorJson.toString());
                } catch (Exception e) {
                    Log.e(TAG, "Failed to execute test item: " + command, e);
                    JSONObject errorJson = new JSONObject();
                    try {
                        errorJson.put("success", false);
                        errorJson.put("message", "명령 실행 실패: " + e.getMessage());
                    } catch (Exception e2) {
                        // JSON 생성 실패
                    }
                    return newResponse(Response.Status.INTERNAL_ERROR, Constants.HTTP.CONTENT_TYPE_JSON, errorJson.toString());
                }
            }
            
            // 성공 응답 반환
            JSONObject successJson = new JSONObject();
            try {
                successJson.put("success", true);
                successJson.put("message", "테스트 항목이 실행되었습니다: " + command);
                return newResponse(Response.Status.OK, Constants.HTTP.CONTENT_TYPE_JSON, successJson.toString());
            } catch (Exception e) {
                Log.e(TAG, "Failed to create success JSON", e);
                return newResponse(Response.Status.OK, Constants.HTTP.CONTENT_TYPE_JSON, 
                    "{\"success\": true, \"message\": \"테스트 항목이 실행되었습니다: " + command + "\"}");
            }
        } catch (Exception e) {
            Log.e(TAG, "테스트 항목 실행 처리 오류", e);
            JSONObject errorJson = new JSONObject();
            try {
                errorJson.put("success", false);
                errorJson.put("message", "서버 오류: " + e.getMessage());
            } catch (Exception e2) {
                // JSON 생성 실패
            }
            return newResponse(Response.Status.INTERNAL_ERROR, Constants.HTTP.CONTENT_TYPE_JSON, errorJson.toString());
        }
    }

    private String sessionIdParam = "";

    /**
     * 제어 시작 신호 처리 (제어 요청 접근 메시지 표시)
     */
    private Response handleTestControlStart(IHTTPSession session) {
        try {
            if (context == null) {
                JSONObject errorJson = new JSONObject();
                errorJson.put("success", false);
                errorJson.put("message", "서버 초기화 오류");
                return newResponse(Response.Status.INTERNAL_ERROR, Constants.HTTP.CONTENT_TYPE_JSON, errorJson.toString());
            }
            
            // POST 데이터에서 sessionId 파라미터 읽기
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            sessionIdParam = session.getParms().get("session_id");
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> start.sessionIdParam: " + sessionIdParam);
            
            Log.i(TAG, "[제어 시작 신호] sessionId: " + (sessionIdParam != null ? sessionIdParam : "없음") +
                      " | 클라이언트: " + getCurrentClientInfo(session, sessionIdParam));
            
            // ActivityModel 인스턴스 가져오기
            Object activity = null;

            try {
                Class<?> activityModelClass = Class.forName("itf.com.app.lms.ActivityModelTestProcess");
                java.lang.reflect.Method getMainActivityMethod = activityModelClass.getMethod("getMainActivity");
                activity = getMainActivityMethod.invoke(null);
            } catch (Exception e) {
                Log.d(TAG, "Could not get Activity instance: " + e.getMessage());
            }
            /*
            try {
                Class<?> activityModelClass = Class.forName("itf.com.app.simple_line_test_ovio_new.ActivityModel_0002");
                java.lang.reflect.Method getMainActivityMethod = activityModelClass.getMethod("getMainActivity");
                activity = getMainActivityMethod.invoke(null);
            } catch (Exception e) {
                try {
                    Class<?> activityModelClass = Class.forName("itf.com.app.simple_line_test_ovio_new.ActivityModel_0001");
                    java.lang.reflect.Method getMainActivityMethod = activityModelClass.getMethod("getMainActivity");
                    activity = getMainActivityMethod.invoke(null);
                } catch (Exception e2) {
                    try {
                        Class<?> activityModelClass = Class.forName("itf.com.app.simple_line_test_ovio_new.ActivityModel_0003");
                        java.lang.reflect.Method getMainActivityMethod = activityModelClass.getMethod("getMainActivity");
                        activity = getMainActivityMethod.invoke(null);
                    } catch (Exception e3) {
                        Log.d(TAG, "Could not get Activity instance: " + e3.getMessage());
                    }
                }
            }
            */
            
            if (activity == null) {
                JSONObject errorJson = new JSONObject();
                try {
                    errorJson.put("success", false);
                    errorJson.put("message", "Activity 인스턴스를 찾을 수 없습니다.");
                    String jsonString = errorJson.toString();
                    if (jsonString == null || jsonString.isEmpty()) {
                        jsonString = "{\"success\": false, \"message\": \"Activity 인스턴스를 찾을 수 없습니다.\"}";
                    }
                    return newResponse(Response.Status.INTERNAL_ERROR, Constants.HTTP.CONTENT_TYPE_JSON, jsonString);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to create error JSON for null activity", e);
                    return newResponse(Response.Status.INTERNAL_ERROR, Constants.HTTP.CONTENT_TYPE_JSON, 
                        "{\"success\": false, \"message\": \"Activity 인스턴스를 찾을 수 없습니다.\"}");
                }
            }
            
            // 제어 요청 접근 메시지 표시
            try {
                java.lang.reflect.Method notifyControlRequestAccessMethod = activity.getClass().getMethod("notifyControlRequestAccess");
                notifyControlRequestAccessMethod.invoke(activity);
                Log.d(TAG, "Control request access message displayed via /test/control/start");
            } catch (Exception e) {
                Log.d(TAG, "Could not display control request access message: " + e.getMessage());
            }
            
            JSONObject successJson = new JSONObject();
            try {
                successJson.put("success", true);
                successJson.put("message", "제어 시작 신호가 전달되었습니다.");
                String jsonString = successJson.toString();
                if (jsonString == null || jsonString.isEmpty()) {
                    jsonString = "{\"success\": true, \"message\": \"제어 시작 신호가 전달되었습니다.\"}";
                }
                return newResponse(Response.Status.OK, Constants.HTTP.CONTENT_TYPE_JSON, jsonString);
            } catch (Exception jsonEx) {
                Log.e(TAG, "Failed to create success JSON", jsonEx);
                return newResponse(Response.Status.OK, Constants.HTTP.CONTENT_TYPE_JSON, 
                    "{\"success\": true, \"message\": \"제어 시작 신호가 전달되었습니다.\"}");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to handle control start", e);
            JSONObject errorJson = new JSONObject();
            try {
                errorJson.put("success", false);
                errorJson.put("message", "제어 시작 신호 처리 중 오류가 발생했습니다: " + e.getMessage());
                String jsonString = errorJson.toString();
                if (jsonString == null || jsonString.isEmpty()) {
                    jsonString = "{\"success\": false, \"message\": \"제어 시작 신호 처리 중 오류가 발생했습니다.\"}";
                }
                return newResponse(Response.Status.INTERNAL_ERROR, Constants.HTTP.CONTENT_TYPE_JSON, jsonString);
            } catch (Exception jsonEx) {
                Log.e(TAG, "Failed to create error JSON", jsonEx);
                return newResponse(Response.Status.INTERNAL_ERROR, Constants.HTTP.CONTENT_TYPE_JSON, 
                    "{\"success\": false, \"message\": \"제어 시작 신호 처리 중 오류가 발생했습니다.\"}");
            }
        }
    }
    
    /**
     * 제어 종료 신호 처리 (제어 요청 접근 메시지 삭제)
     */
    private Response handleTestControlEnd(IHTTPSession session) {
        try {
            if (context == null) {
                JSONObject errorJson = new JSONObject();
                errorJson.put("success", false);
                errorJson.put("message", "서버 초기화 오류");
                return newResponse(Response.Status.INTERNAL_ERROR, Constants.HTTP.CONTENT_TYPE_JSON, errorJson.toString());
            }
            
            // POST 데이터에서 sessionId 파라미터 읽기
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            String sessionIdParam = this.sessionIdParam;
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>   end.sessionIdParam: " + sessionIdParam);
            
            Log.i(TAG, "[제어 종료 신호] sessionId: " + (sessionIdParam != null ? sessionIdParam : "없음") +
                      " | 클라이언트: " + getCurrentClientInfo(session, sessionIdParam));
            
            // ActivityModel 인스턴스 가져오기
            Object activity = null;

            try {
                Class<?> activityModelClass = Class.forName("itf.com.app.lms.ActivityModelTestProcess");
                java.lang.reflect.Method getMainActivityMethod = activityModelClass.getMethod("getMainActivity");
                activity = getMainActivityMethod.invoke(null);
            } catch (Exception e) {
                Log.d(TAG, "Could not get Activity instance: " + e.getMessage());
            }
            /*
            try {
                Class<?> activityModelClass = Class.forName("itf.com.app.simple_line_test_ovio_new.ActivityModel_0002");
                java.lang.reflect.Method getMainActivityMethod = activityModelClass.getMethod("getMainActivity");
                activity = getMainActivityMethod.invoke(null);
            } catch (Exception e) {
                try {
                    Class<?> activityModelClass = Class.forName("itf.com.app.simple_line_test_ovio_new.ActivityModel_0001");
                    java.lang.reflect.Method getMainActivityMethod = activityModelClass.getMethod("getMainActivity");
                    activity = getMainActivityMethod.invoke(null);
                } catch (Exception e2) {
                    try {
                        Class<?> activityModelClass = Class.forName("itf.com.app.simple_line_test_ovio_new.ActivityModel_0003");
                        java.lang.reflect.Method getMainActivityMethod = activityModelClass.getMethod("getMainActivity");
                        activity = getMainActivityMethod.invoke(null);
                    } catch (Exception e3) {
                        Log.d(TAG, "Could not get Activity instance: " + e3.getMessage());
                    }
                }
            }
            */
            
            if (activity == null) {
                JSONObject errorJson = new JSONObject();
                try {
                    errorJson.put("success", false);
                    errorJson.put("message", "Activity 인스턴스를 찾을 수 없습니다.");
                    String jsonString = errorJson.toString();
                    if (jsonString == null || jsonString.isEmpty()) {
                        jsonString = "{\"success\": false, \"message\": \"Activity 인스턴스를 찾을 수 없습니다.\"}";
                    }
                    return newResponse(Response.Status.INTERNAL_ERROR, Constants.HTTP.CONTENT_TYPE_JSON, jsonString);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to create error JSON for null activity", e);
                    return newResponse(Response.Status.INTERNAL_ERROR, Constants.HTTP.CONTENT_TYPE_JSON, 
                        "{\"success\": false, \"message\": \"Activity 인스턴스를 찾을 수 없습니다.\"}");
                }
            }
            
            // 제어 요청 접근 메시지 삭제
            try {
                java.lang.reflect.Method clearControlRequestAccessMessageMethod = activity.getClass().getMethod("clearControlRequestAccessMessage");
                clearControlRequestAccessMessageMethod.invoke(activity);
                Log.d(TAG, "Control request access message cleared via /test/control/end");
            } catch (Exception e) {
                Log.d(TAG, "Could not clear control request access message: " + e.getMessage());
            }
            
            JSONObject successJson = new JSONObject();
            try {
                successJson.put("success", true);
                successJson.put("message", "제어 종료 신호가 전달되었습니다.");
                String jsonString = successJson.toString();
                if (jsonString == null || jsonString.isEmpty()) {
                    jsonString = "{\"success\": true, \"message\": \"제어 종료 신호가 전달되었습니다.\"}";
                }
                return newResponse(Response.Status.OK, Constants.HTTP.CONTENT_TYPE_JSON, jsonString);
            } catch (Exception jsonEx) {
                Log.e(TAG, "Failed to create success JSON", jsonEx);
                return newResponse(Response.Status.OK, Constants.HTTP.CONTENT_TYPE_JSON, 
                    "{\"success\": true, \"message\": \"제어 종료 신호가 전달되었습니다.\"}");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to handle control end", e);
            JSONObject errorJson = new JSONObject();
            try {
                errorJson.put("success", false);
                errorJson.put("message", "제어 종료 신호 처리 중 오류가 발생했습니다: " + e.getMessage());
                String jsonString = errorJson.toString();
                if (jsonString == null || jsonString.isEmpty()) {
                    jsonString = "{\"success\": false, \"message\": \"제어 종료 신호 처리 중 오류가 발생했습니다.\"}";
                }
                return newResponse(Response.Status.INTERNAL_ERROR, Constants.HTTP.CONTENT_TYPE_JSON, jsonString);
            } catch (Exception jsonEx) {
                Log.e(TAG, "Failed to create error JSON", jsonEx);
                return newResponse(Response.Status.INTERNAL_ERROR, Constants.HTTP.CONTENT_TYPE_JSON, 
                    "{\"success\": false, \"message\": \"제어 종료 신호 처리 중 오류가 발생했습니다.\"}");
            }
        }
    }
    
    /**
     * 애플리케이션 종료 신호 처리 (웹 클라이언트에 알림)
     */
    private Response handleShutdownNotify(IHTTPSession session) {
        try {
            // POST 데이터에서 종료 원인 읽기
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            String shutdownReason = session.getParms().get("reason");
            
            if (shutdownReason == null || shutdownReason.isEmpty()) {
                shutdownReason = "UNEXPECTED"; // 기본값
            }
            
            // 마지막 종료 신호 저장
            lastShutdownReason = shutdownReason;
            Log.d(TAG, "Shutdown notification received: " + shutdownReason);
            
            JSONObject successJson = new JSONObject();
            try {
                successJson.put("success", true);
                successJson.put("message", "종료 신호가 전달되었습니다.");
                successJson.put("reason", shutdownReason);
                String jsonString = successJson.toString();
                if (jsonString == null || jsonString.isEmpty()) {
                    jsonString = "{\"success\": true, \"message\": \"종료 신호가 전달되었습니다.\"}";
                }
                return newResponse(Response.Status.OK, Constants.HTTP.CONTENT_TYPE_JSON, jsonString);
            } catch (Exception jsonEx) {
                Log.e(TAG, "Failed to create success JSON", jsonEx);
                return newResponse(Response.Status.OK, Constants.HTTP.CONTENT_TYPE_JSON, 
                    "{\"success\": true, \"message\": \"종료 신호가 전달되었습니다.\"}");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to handle shutdown notify", e);
            JSONObject errorJson = new JSONObject();
            try {
                errorJson.put("success", false);
                errorJson.put("message", "종료 신호 처리 중 오류가 발생했습니다: " + e.getMessage());
                String jsonString = errorJson.toString();
                if (jsonString == null || jsonString.isEmpty()) {
                    jsonString = "{\"success\": false, \"message\": \"종료 신호 처리 중 오류가 발생했습니다.\"}";
                }
                return newResponse(Response.Status.INTERNAL_ERROR, Constants.HTTP.CONTENT_TYPE_JSON, jsonString);
            } catch (Exception jsonEx) {
                Log.e(TAG, "Failed to create error JSON", jsonEx);
                return newResponse(Response.Status.INTERNAL_ERROR, Constants.HTTP.CONTENT_TYPE_JSON, 
                    "{\"success\": false, \"message\": \"종료 신호 처리 중 오류가 발생했습니다.\"}");
            }
        }
    }
    
    /**
     * 마지막 종료 신호 가져오기
     */
    public static String getLastShutdownReason() {
        return lastShutdownReason;
    }
    
    /**
     * 마지막 종료 신호 초기화
     */
    public static void clearLastShutdownReason() {
        lastShutdownReason = null;
    }
    
    /**
     * 제어 소유자 정보 처리 (안드로이드 앱에서 제어 ON/OFF 시)
     */
    private Response handleControlOwner(IHTTPSession session) {
        try {
            // POST 데이터에서 안드로이드 앱 여부 읽기
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            String isAndroidAppStr = session.getParms().get("is_android_app");
            
            boolean isAndroidApp = "true".equalsIgnoreCase(isAndroidAppStr);
            String currentClient = getCurrentClientInfo(session, null);
            String previousOwner = getControlOwnerInfo();
            
            synchronized (controlOwnerLock) {
                if (isAndroidApp) {
                    // 안드로이드 앱에서 제어 ON
                    Log.i(TAG, "[제어 소유자 정보 처리] 이벤트: 안드로이드 앱 제어 ON" +
                              " | 현재 제어 소유자: " + previousOwner +
                              " | 요청자: " + currentClient);
                    setControlOwner(session, true, null);
                } else {
                    // 안드로이드 앱에서 제어 OFF (소유자 초기화)
                    Log.i(TAG, "[제어 소유자 정보 처리] 이벤트: 안드로이드 앱 제어 OFF" +
                              " | 현재 제어 소유자: " + previousOwner +
                              " | 요청자: " + currentClient);
                    clearControlOwner();
                }
            }
            
            JSONObject successJson = new JSONObject();
            try {
                successJson.put("success", true);
                successJson.put("message", "제어 소유자 정보가 업데이트되었습니다.");
                String jsonString = successJson.toString();
                if (jsonString == null || jsonString.isEmpty()) {
                    jsonString = "{\"success\": true, \"message\": \"제어 소유자 정보가 업데이트되었습니다.\"}";
                }
                return newResponse(Response.Status.OK, Constants.HTTP.CONTENT_TYPE_JSON, jsonString);
            } catch (Exception jsonEx) {
                Log.e(TAG, "Failed to create success JSON", jsonEx);
                return newResponse(Response.Status.OK, Constants.HTTP.CONTENT_TYPE_JSON, 
                    "{\"success\": true, \"message\": \"제어 소유자 정보가 업데이트되었습니다.\"}");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to handle control owner", e);
            JSONObject errorJson = new JSONObject();
            try {
                errorJson.put("success", false);
                errorJson.put("message", "제어 소유자 정보 처리 중 오류가 발생했습니다: " + e.getMessage());
                String jsonString = errorJson.toString();
                if (jsonString == null || jsonString.isEmpty()) {
                    jsonString = "{\"success\": false, \"message\": \"제어 소유자 정보 처리 중 오류가 발생했습니다.\"}";
                }
                return newResponse(Response.Status.INTERNAL_ERROR, Constants.HTTP.CONTENT_TYPE_JSON, jsonString);
            } catch (Exception jsonEx) {
                Log.e(TAG, "Failed to create error JSON", jsonEx);
                return newResponse(Response.Status.INTERNAL_ERROR, Constants.HTTP.CONTENT_TYPE_JSON, 
                    "{\"success\": false, \"message\": \"제어 소유자 정보 처리 중 오류가 발생했습니다.\"}");
            }
        }
    }
    
    /**
     * 웹서버 설정 초기화 (DB에 설정이 없으면 기본값으로 추가)
     */
    private void initializeWebServerSettings() {
        if (context == null) {
            return;
        }
        
        try {
            // 연결 타임아웃 설정 확인 및 추가
            String timeoutValue = itf.com.app.lms.util.TestData.getSettingValue(
                context, SETTING_ID_CONNECTION_TIMEOUT);
            if (timeoutValue == null || timeoutValue.trim().isEmpty()) {
                Map<String, String> settingData = new HashMap<>();
                settingData.put("clm_setting_seq", "000000");
                settingData.put("clm_setting_id", SETTING_ID_CONNECTION_TIMEOUT);
                settingData.put("clm_setting_value", String.valueOf(DEFAULT_CONNECTION_TIMEOUT_MS));
                settingData.put("clm_comment", "웹서버 연결 타임아웃 (밀리초)");
                settingData.put("clm_test_timestamp", getCurrentDatetime(Constants.DateTimeFormats.TIMESTAMP_FORMAT));
                TestData.insertSettingInfo(context, settingData);
                Log.i(TAG, "WebServer setting initialized: " + SETTING_ID_CONNECTION_TIMEOUT + " = " + DEFAULT_CONNECTION_TIMEOUT_MS);
            }
            
            // 최대 접속 수 설정 확인 및 추가
            String maxConnValue = itf.com.app.lms.util.TestData.getSettingValue(
                context, SETTING_ID_MAX_CONNECTIONS);
            if (maxConnValue == null || maxConnValue.trim().isEmpty()) {
                Map<String, String> settingData = new HashMap<>();
                settingData.put("clm_setting_seq", "000000");
                settingData.put("clm_setting_id", SETTING_ID_MAX_CONNECTIONS);
                settingData.put("clm_setting_value", String.valueOf(DEFAULT_MAX_CONNECTIONS));
                settingData.put("clm_comment", "웹서버 최대 동시 접속 수");
                settingData.put("clm_test_timestamp", getCurrentDatetime(Constants.DateTimeFormats.TIMESTAMP_FORMAT));
                TestData.insertSettingInfo(context, settingData);
                Log.i(TAG, "WebServer setting initialized: " + SETTING_ID_MAX_CONNECTIONS + " = " + DEFAULT_MAX_CONNECTIONS);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize WebServer settings", e);
        }
    }
    
    /**
     * 연결 타임아웃 가져오기 (DB에서 조회, 없으면 기본값)
     * @return 연결 타임아웃 (밀리초)
     */
    private long getConnectionTimeoutMs() {
        if (context == null) {
            return DEFAULT_CONNECTION_TIMEOUT_MS;
        }
        
        try {
            String value = itf.com.app.lms.util.TestData.getSettingValue(
                context, SETTING_ID_CONNECTION_TIMEOUT);
            if (value != null && !value.trim().isEmpty()) {
                try {
                    long timeout = Long.parseLong(value.trim());
                    return timeout > 0 ? timeout : DEFAULT_CONNECTION_TIMEOUT_MS;
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Invalid " + SETTING_ID_CONNECTION_TIMEOUT + " value: " + value);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to get " + SETTING_ID_CONNECTION_TIMEOUT + " setting: " + e.getMessage());
        }
        
        return DEFAULT_CONNECTION_TIMEOUT_MS;
    }
    
    /**
     * 최대 접속 수 가져오기 (DB에서 조회, 없으면 기본값)
     */
    private int getMaxConnections() {
        if (context == null) {
            return DEFAULT_MAX_CONNECTIONS;
        }
        
        try {
            String value = itf.com.app.lms.util.TestData.getSettingValue(
                context, SETTING_ID_MAX_CONNECTIONS);
            if (value != null && !value.trim().isEmpty()) {
                try {
                    int maxConn = Integer.parseInt(value.trim());
                    return maxConn > 0 ? maxConn : DEFAULT_MAX_CONNECTIONS;
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Invalid " + SETTING_ID_MAX_CONNECTIONS + " value: " + value);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to get " + SETTING_ID_MAX_CONNECTIONS + " setting: " + e.getMessage());
        }
        
        return DEFAULT_MAX_CONNECTIONS;
    }
    
    /**
     * 접속 제한 확인 및 등록
     * @param session HTTP 세션
     * @return true: 접속 허용, false: 접속 제한 초과
     */
    private boolean checkAndRegisterConnection(IHTTPSession session) {
        synchronized (activeConnectionsLock) {
            // 타임아웃된 연결 제거
            long currentTime = System.currentTimeMillis();
            long connectionTimeout = getConnectionTimeoutMs();
            activeConnections.entrySet().removeIf(entry -> 
                currentTime - entry.getValue() > connectionTimeout);
            
            // 타임아웃된 세션 ID 제거
            sessionIdToLastAccess.entrySet().removeIf(entry -> 
                currentTime - entry.getValue() > connectionTimeout);
            
            // 클라이언트 식별자 생성
            String clientId = generateClientSessionId(session, null);
            
            // 이미 등록된 연결인지 확인
            if (activeConnections.containsKey(clientId)) {
                // 기존 연결 갱신
                activeConnections.put(clientId, currentTime);
                return true;
            }
            
            // 최대 접속 수 확인
            int maxConnections = getMaxConnections();
            if (activeConnections.size() >= maxConnections) {
                Log.w(TAG, "Max connections exceeded: " + activeConnections.size() + "/" + maxConnections);
                // return false;
            }
            
            // 새 연결 등록
            activeConnections.put(clientId, currentTime);
            Log.d(TAG, "[접속 등록] 새 연결 등록: " + clientId + " (Total: " + activeConnections.size() + "/" + maxConnections + ")");
            return true;
        }
    }
    
    /**
     * 연결 해제 (명시적 호출 시 사용)
     * @param session HTTP 세션
     */
    private void unregisterConnection(IHTTPSession session) {
        synchronized (activeConnectionsLock) {
            String clientId = generateClientSessionId(session, null);
            if (activeConnections.remove(clientId) != null) {
                Log.d(TAG, "Connection unregistered: " + clientId + " (Remaining: " + activeConnections.size() + ")");
            }
        }
    }
    
    /**
     * 클라이언트 IP 주소 가져오기
     */
    private String getClientIpAddress(IHTTPSession session) {
        try {
            // NanoHTTPD의 IHTTPSession에서 IP 주소 가져오기
            // getRemoteIpAddress() 메서드가 없을 수 있으므로 다른 방법 시도
            try {
                java.lang.reflect.Method getRemoteIpAddressMethod = session.getClass().getMethod("getRemoteIpAddress");
                Object ipObj = getRemoteIpAddressMethod.invoke(session);
                if (ipObj != null) {
                    return ipObj.toString();
                }
            } catch (Exception e) {
                // getRemoteIpAddress() 메서드가 없는 경우
            }
            
            // 대안: getRemoteHostName() 사용
            try {
                java.lang.reflect.Method getRemoteHostNameMethod = session.getClass().getMethod("getRemoteHostName");
                Object hostObj = getRemoteHostNameMethod.invoke(session);
                if (hostObj != null) {
                    return hostObj.toString();
                }
            } catch (Exception e) {
                // getRemoteHostName() 메서드도 없는 경우
            }
            
            // 최후의 수단: 세션 해시코드 사용
            return "session_" + System.identityHashCode(session);
        } catch (Exception e) {
            Log.w(TAG, "Failed to get client IP address: " + e.getMessage());
            return "unknown";
        }
    }
    
    /**
     * 클라이언트 식별자 생성 (쿠키 기반 세션 ID 사용)
     * @param session HTTP 세션
     * @param sessionIdParam 클라이언트에서 전달한 sessionId 파라미터 (null 가능)
     * @return 클라이언트 식별자 (쿠키가 있으면 세션 ID만, 없으면 "IP|SessionId")
     */
    private String generateClientSessionId(IHTTPSession session, String sessionIdParam) {
        try {
            // sessionId 파라미터가 있으면 사용
            if (sessionIdParam != null && !sessionIdParam.trim().isEmpty()) {
                return sessionIdParam.trim();
            }
            
            // 쿠키에서 세션 ID 확인
            String sessionCookie = getSessionCookieFromRequest(session);
            if (sessionCookie != null && !sessionCookie.trim().isEmpty()) {
                // 쿠키에 세션 ID가 있으면 세션 ID만 사용 (쿠키가 있으면 세션 ID가 고유하므로 IP 불필요)
                // 세션 ID의 마지막 접근 시간 갱신
                synchronized (activeConnectionsLock) {
                    sessionIdToLastAccess.put(sessionCookie.trim(), System.currentTimeMillis());
                }
                String clientId = sessionCookie.trim();
                // Log.d(TAG, "[세션 ID] 쿠키에서 세션 ID 사용: " + clientId);
                return clientId;
            }
            
            // 쿠키가 없으면 IP 기반으로 세션 ID 관리
            String ip = getClientIpAddress(session);
            synchronized (activeConnectionsLock) {
                String existingSessionId = ipToSessionIdMap.get(ip);
                if (existingSessionId != null) {
                    // 기존 세션 ID가 타임아웃되지 않았는지 확인
                    Long lastAccess = sessionIdToLastAccess.get(existingSessionId);
                    if (lastAccess != null) {
                        long currentTime = System.currentTimeMillis();
                        long connectionTimeout = getConnectionTimeoutMs();
                        if (currentTime - lastAccess <= connectionTimeout) {
                            // 기존 세션 ID가 유효하면 재사용
                            sessionIdToLastAccess.put(existingSessionId, currentTime);
                            String clientId = ip + "|" + existingSessionId;
                            Log.d(TAG, "[세션 ID] 기존 세션 ID 재사용: " + clientId);
                            return clientId;
                        } else {
                            // 타임아웃된 세션 ID 제거
                            Log.d(TAG, "[세션 ID] 타임아웃된 세션 ID 제거: " + existingSessionId);
                            ipToSessionIdMap.remove(ip);
                            sessionIdToLastAccess.remove(existingSessionId);
                        }
                    }
                }
                
                // 새 세션 ID 생성
                String newSessionId = generateNewSessionId();
                ipToSessionIdMap.put(ip, newSessionId);
                sessionIdToLastAccess.put(newSessionId, System.currentTimeMillis());
                String clientId = ip + "|" + newSessionId;
                Log.d(TAG, "[세션 ID] 새 세션 ID 생성: " + clientId + " (쿠키 없음)");
                return clientId;
            }
            
        } catch (Exception e) {
            Log.w(TAG, "Failed to generate client session ID: " + e.getMessage());
            return getClientIpAddress(session) + "|" + (sessionIdParam != null ? sessionIdParam : "unknown");
        }
    }
    
    /**
     * HTTP 요청에서 세션 쿠키 추출
     * @param session HTTP 세션
     * @return 세션 쿠키 값 (없으면 null)
     */
    private String getSessionCookieFromRequest(IHTTPSession session) {
        try {
            // NanoHTTPD의 IHTTPSession에서 쿠키 가져오기
            java.lang.reflect.Method getHeadersMethod = session.getClass().getMethod("getHeaders");
            Object headersObj = getHeadersMethod.invoke(session);
            if (headersObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, String> headers = (Map<String, String>) headersObj;
                
                // Cookie 헤더 찾기
                String cookieHeader = headers.get("cookie");
                if (cookieHeader == null) {
                    cookieHeader = headers.get("Cookie");
                }
                
                if (cookieHeader != null) {
                    // Log.d(TAG, "[쿠키] Cookie 헤더 발견: " + cookieHeader);
                    // Cookie 헤더 파싱: "WS_SESSION_ID=abc123; other=value"
                    String[] cookies = cookieHeader.split(";");
                    for (String cookie : cookies) {
                        String[] parts = cookie.trim().split("=", 2);
                        if (parts.length == 2 && SESSION_COOKIE_NAME.equals(parts[0].trim())) {
                            String sessionId = parts[1].trim();
                            // Log.d(TAG, "[쿠키] 세션 ID 추출 성공: " + sessionId);
                            return sessionId;
                        }
                    }
                    // Log.d(TAG, "[쿠키] " + SESSION_COOKIE_NAME + " 쿠키를 찾을 수 없음");
                } else {
                    Log.d(TAG, "[쿠키] Cookie 헤더가 없음");
                }
            } else {
                Log.d(TAG, "[쿠키] 헤더가 Map이 아님");
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to get session cookie: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 새로운 세션 ID 생성 (UUID 기반)
     * @return 새 세션 ID
     */
    private String generateNewSessionId() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * 클라이언트 ID에서 세션 ID 추출
     * @param clientId 클라이언트 ID (쿠키가 있으면 세션 ID만, 없으면 "IP|SessionId")
     * @return 세션 ID
     */
    private String extractSessionIdFromClientId(String clientId) {
        if (clientId == null) {
            return null;
        }
        // "|"가 없으면 clientId 자체가 세션 ID (쿠키 기반)
        int pipeIndex = clientId.indexOf("|");
        if (pipeIndex >= 0 && pipeIndex < clientId.length() - 1) {
            // "IP|SessionId" 형식인 경우
            return clientId.substring(pipeIndex + 1);
        } else {
            // 세션 ID만 있는 경우 (쿠키 기반)
            return clientId;
        }
    }
    
    /**
     * 제어 소유자 정보를 문자열로 반환
     */
    private String getControlOwnerInfo() {
        synchronized (controlOwnerLock) {
            if (controlOwnerIsAndroidApp) {
                return "Android App";
            } else if (controlOwnerIp != null && controlOwnerSessionId != null) {
                // return "Web Client [IP: " + controlOwnerIp + ", SessionId: " + controlOwnerSessionId + "]";
                return controlOwnerSessionId;
            } else {
                return "None";
            }
        }
    }
    
    /**
     * 현재 클라이언트 정보를 문자열로 반환
     */
    private String getCurrentClientInfo(IHTTPSession session, String sessionIdParam) {
        if (session == null) {
            return "Android App";
        }
        String clientIp = getClientIpAddress(session);
        String clientSessionId = generateClientSessionId(session, sessionIdParam);
        return "Web Client [IP: " + clientIp + ", SessionId: " + clientSessionId + "]";
    }
    
    /**
     * 제어 소유자 확인 (IP + 세션 기반)
     */
    private boolean isControlOwner(IHTTPSession session, String sessionIdParam) {
        synchronized (controlOwnerLock) {
            String currentOwner = getControlOwnerInfo();
            String currentClient = getCurrentClientInfo(session, sessionIdParam);
            
            // 안드로이드 앱에서 제어 ON을 한 경우
            if (controlOwnerIsAndroidApp) {
                Log.i(TAG, "[제어 소유자 확인] 현재 제어 소유자: " + currentOwner + 
                          " | 현재 요청자: " + currentClient + 
                          " | 결과: 거부 (안드로이드 앱이 제어권 보유)");
                return false; // 웹 클라이언트는 안드로이드 앱이 제어 ON을 한 경우 제어 OFF 불가
            }
            
            // 제어 소유자가 없으면 모든 클라이언트가 제어 가능
            if (controlOwnerIp == null && controlOwnerSessionId == null) {
                Log.i(TAG, "[제어 소유자 확인] 현재 제어 소유자: " + currentOwner + 
                          " | 현재 요청자: " + currentClient + 
                          " | 결과: 허용 (제어 소유자 없음)");
                return true;
            }
            
            String clientIp = getClientIpAddress(session);
            String clientSessionId = generateClientSessionId(session, sessionIdParam);
            
            // IP와 세션 ID 모두 일치해야 함
            boolean isOwner = controlOwnerIp != null && controlOwnerIp.equals(clientIp) &&
                           controlOwnerSessionId != null && controlOwnerSessionId.equals(clientSessionId);
            
            Log.i(TAG, "[제어 소유자 확인] 현재 제어 소유자: " + currentOwner + 
                      " | 현재 요청자: " + currentClient + 
                      " | IP 일치: " + (controlOwnerIp != null && controlOwnerIp.equals(clientIp)) +
                      " | 세션 ID 일치: " + (controlOwnerSessionId != null && controlOwnerSessionId.equals(clientSessionId)) +
                      " | 결과: " + (isOwner ? "허용" : "거부"));
            
            return isOwner;
        }
    }
    
    /**
     * 제어 소유자 설정 (제어 ON 시)
     */
    private void setControlOwner(IHTTPSession session, boolean isAndroidApp, String sessionIdParam) {
        synchronized (controlOwnerLock) {
            String previousOwner = getControlOwnerInfo();
            String newOwner;
            
            if (isAndroidApp) {
                controlOwnerIsAndroidApp = true;
                controlOwnerIp = null;
                controlOwnerSessionId = null;
                newOwner = "Android App";
            } else {
                controlOwnerIsAndroidApp = false;
                if (session != null) {
                    controlOwnerIp = getClientIpAddress(session);
                    // controlOwnerSessionId = generateClientSessionId(session, sessionIdParam);
                    controlOwnerSessionId = generateClientSessionId(session, sessionIdParam);
                    newOwner = "Web Client [IP: " + controlOwnerIp + ", SessionId: " + controlOwnerSessionId + "]";
                    System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> newOwner: " + newOwner);
                } else {
                    // session이 null인 경우 (안드로이드 앱에서 직접 호출)
                    controlOwnerIp = null;
                    controlOwnerSessionId = null;
                    newOwner = "Android App";
                }
            }
            
            Log.i(TAG, "[제어 소유자 설정] 이전 제어 소유자: " + previousOwner + 
                      " | 새로운 제어 소유자: " + newOwner + 
                      " | 이벤트: 제어 ON");
        }
    }
    
    /**
     * 제어 소유자 초기화 (제어 OFF 시)
     */
    private void clearControlOwner() {
        synchronized (controlOwnerLock) {
            String previousOwner = getControlOwnerInfo();
            controlOwnerIp = null;
            controlOwnerSessionId = null;
            controlOwnerIsAndroidApp = false;
            Log.i(TAG, "[제어 소유자 초기화] 이전 제어 소유자: " + previousOwner + 
                      " | 새로운 제어 소유자: None" + 
                      " | 이벤트: 제어 OFF");
        }
    }
    
    /**
     * 제어 버튼 토글 처리 (타이머 시작/중지)
     */
    private Response handleTestControlToggle(IHTTPSession session) {
        try {
            if (context == null) {
                JSONObject errorJson = new JSONObject();
                errorJson.put("success", false);
                errorJson.put("message", "서버 초기화 오류");
                return newResponse(Response.Status.INTERNAL_ERROR, Constants.HTTP.CONTENT_TYPE_JSON, errorJson.toString());
            }
            
            // POST 데이터에서 sessionId 파라미터 읽기
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            String sessionIdParam = session.getParms().get("session_id");

            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> handleTestControlToggle.sessionIdParam " + sessionIdParam);
            
            // ActivityModel 인스턴스 가져오기
            Object activity = null;

            try {
                Class<?> activityModelClass = Class.forName("itf.com.app.lms.ActivityModelTestProcess");
                java.lang.reflect.Method getMainActivityMethod = activityModelClass.getMethod("getMainActivity");
                activity = getMainActivityMethod.invoke(null);
            } catch (Exception e) {
                Log.d(TAG, "Could not get Activity instance: " + e.getMessage());
            }
            /*
            try {
                Class<?> activityModelClass = Class.forName("itf.com.app.simple_line_test_ovio_new.ActivityModel_0002");
                java.lang.reflect.Method getMainActivityMethod = activityModelClass.getMethod("getMainActivity");
                activity = getMainActivityMethod.invoke(null);
            } catch (Exception e) {
                try {
                    Class<?> activityModelClass = Class.forName("itf.com.app.simple_line_test_ovio_new.ActivityModel_0001");
                    java.lang.reflect.Method getMainActivityMethod = activityModelClass.getMethod("getMainActivity");
                    activity = getMainActivityMethod.invoke(null);
                } catch (Exception e2) {
                    try {
                        Class<?> activityModelClass = Class.forName("itf.com.app.simple_line_test_ovio_new.ActivityModel_0003");
                        java.lang.reflect.Method getMainActivityMethod = activityModelClass.getMethod("getMainActivity");
                        activity = getMainActivityMethod.invoke(null);
                    } catch (Exception e3) {
                        Log.d(TAG, "Could not get Activity instance: " + e3.getMessage());
                    }
                }
            }
            */

            if (activity == null) {
                JSONObject errorJson = new JSONObject();
                try {
                    errorJson.put("success", false);
                    errorJson.put("message", "Activity 인스턴스를 찾을 수 없습니다.");
                    errorJson.put("timer_running", false);
                    String jsonString = errorJson.toString();
                    if (jsonString == null || jsonString.isEmpty()) {
                        jsonString = "{\"success\": false, \"message\": \"Activity 인스턴스를 찾을 수 없습니다.\", \"timer_running\": false}";
                    }
                    return newResponse(Response.Status.INTERNAL_ERROR, Constants.HTTP.CONTENT_TYPE_JSON, jsonString);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to create error JSON for null activity", e);
                    return newResponse(Response.Status.INTERNAL_ERROR, Constants.HTTP.CONTENT_TYPE_JSON, 
                        "{\"success\": false, \"message\": \"Activity 인스턴스를 찾을 수 없습니다.\", \"timer_running\": false}");
                }
            }
            
            // 테스트 진행 중인지 확인
            boolean isTestRunning = false;
            try {
                java.lang.reflect.Field currentTestItemField = activity.getClass().getDeclaredField("currentTestItem");
                currentTestItemField.setAccessible(true);
                Object currentTestItemObj = currentTestItemField.get(activity);
                String currentTestItem = currentTestItemObj != null ? currentTestItemObj.toString() : "";
                
                // currentTestItem이 비어있지 않고 초기값이 아니면 테스트 진행 중
                isTestRunning = !currentTestItem.isEmpty() && 
                               !currentTestItem.equals(Constants.InitialValues.CURRENT_TEST_ITEM);
            } catch (Exception e) {
                Log.d(TAG, "Could not check test running status: " + e.getMessage());
            }
            
            // 테스트 진행 중이면 제어 불가
            if (isTestRunning) {
                JSONObject errorJson = new JSONObject();
                try {
                    errorJson.put("success", false);
                    errorJson.put("message", "테스트 중에는 제어가 불가능합니다.");
                    errorJson.put("timer_running", false);
                    String jsonString = errorJson.toString();
                    if (jsonString == null || jsonString.isEmpty()) {
                        jsonString = "{\"success\": false, \"message\": \"테스트 중에는 제어가 불가능합니다.\", \"timer_running\": false}";
                    }
                    return newResponse(Response.Status.BAD_REQUEST, Constants.HTTP.CONTENT_TYPE_JSON, jsonString);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to create error JSON for test running", e);
                    return newResponse(Response.Status.BAD_REQUEST, Constants.HTTP.CONTENT_TYPE_JSON, 
                        "{\"success\": false, \"message\": \"테스트 중에는 제어가 불가능합니다.\", \"timer_running\": false}");
                }
            }
            
            // 제어 모드 상태 확인
            boolean isControlMode = false;
            try {
                java.lang.reflect.Field isControlModeField = activity.getClass().getDeclaredField("isControlMode");
                isControlModeField.setAccessible(true);
                Object isControlModeObj = isControlModeField.get(activity);
                if (isControlModeObj instanceof Boolean) {
                    isControlMode = (Boolean) isControlModeObj;
                }
            } catch (Exception e) {
                Log.d(TAG, "Could not check control mode status: " + e.getMessage());
            }
            
            // 제어 모드가 아니면 제어 불가
            if (!isControlMode) {
                JSONObject errorJson = new JSONObject();
                try {
                    errorJson.put("success", false);
                    errorJson.put("message", "단말이 '제어 상태'가 아닙니다.");
                    errorJson.put("timer_running", false);
                    String jsonString = errorJson.toString();
                    if (jsonString == null || jsonString.isEmpty()) {
                        jsonString = "{\"success\": false, \"message\": \"단말이 '제어 상태'가 아닙니다.\", \"timer_running\": false}";
                    }
                    return newResponse(Response.Status.BAD_REQUEST, Constants.HTTP.CONTENT_TYPE_JSON, jsonString);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to create error JSON for control mode", e);
                    return newResponse(Response.Status.BAD_REQUEST, Constants.HTTP.CONTENT_TYPE_JSON, 
                        "{\"success\": false, \"message\": \"단말이 '제어 상태'가 아닙니다.\", \"timer_running\": false}");
                }
            }
            
            // 타이머 상태 확인
            boolean isTimerRunning = false;
            try {
                java.lang.reflect.Field btMessageTimerRunningField = activity.getClass().getDeclaredField("btMessageTimerRunning");
                btMessageTimerRunningField.setAccessible(true);
                Object btMessageTimerRunningObj = btMessageTimerRunningField.get(activity);
                if (btMessageTimerRunningObj instanceof java.util.concurrent.atomic.AtomicBoolean) {
                    java.util.concurrent.atomic.AtomicBoolean atomicBoolean = (java.util.concurrent.atomic.AtomicBoolean) btMessageTimerRunningObj;
                    isTimerRunning = atomicBoolean.get();
                }
            } catch (Exception e) {
                Log.d(TAG, "Could not check timer running status: " + e.getMessage());
                // 타이머 객체로 확인 시도
                try {
                    java.lang.reflect.Field tmrBTMessageSendField = activity.getClass().getDeclaredField("tmrBTMessageSend");
                    tmrBTMessageSendField.setAccessible(true);
                    Object tmrBTMessageSendObj = tmrBTMessageSendField.get(activity);
                    isTimerRunning = (tmrBTMessageSendObj != null);
                } catch (Exception e2) {
                    Log.d(TAG, "Could not check timer object: " + e2.getMessage());
                }
            }
            
            // 제어 요청 접근 알림 (제어 ON 상태일 때만)
            if (isControlMode) {
                try {
                    java.lang.reflect.Method notifyControlRequestAccessMethod = activity.getClass().getMethod("notifyControlRequestAccess");
                    notifyControlRequestAccessMethod.invoke(activity);
                    Log.d(TAG, "Control request access notification sent to Android app");
                } catch (Exception e) {
                    Log.d(TAG, "Could not notify control request access: " + e.getMessage());
                }
            }
            
            // 타이머 시작/중지
            try {
                String currentClient = getCurrentClientInfo(session, sessionIdParam);
                String currentOwner = getControlOwnerInfo();
                
                if (isTimerRunning) {
                    // 타이머 중지 (제어 OFF 상태가 됨)
                    Log.i(TAG, "[제어 토글 요청] 이벤트: 제어 OFF 요청" +
                              " | 현재 제어 소유자: " + currentOwner +
                              " | 요청자: " + currentClient +
                              " | sessionId: " + (sessionIdParam != null ? sessionIdParam : "없음"));

                    // 프로세스 소유자/접근자 구분 2025/12/22
                    System.out.println("> handleTestControlToggle.isTimerRunning.currentOwner " + currentOwner + " isTimerRunning " + sessionIdParam);
                    
                    // 제어 소유자 확인 (제어 ON을 한 사용자만 제어 OFF 가능)
                    // if (!isControlOwner(session, sessionIdParam)) {
                    if (!currentOwner.equals(sessionIdParam)) {
                        Log.w(TAG, "[제어 토글 요청] 제어 OFF 거부" +
                                  " | 현재 제어 소유자: " + currentOwner +
                                  " | 요청자: " + currentClient +
                                  " | 이유: 제어 소유자가 아님");
                        JSONObject errorJson = new JSONObject();
                        try {
                            errorJson.put("success", false);
                            errorJson.put("message", "제어 OFF는 제어 ON을 한 사용자만 가능합니다.");
                            errorJson.put("timer_running", true);
                            String jsonString = errorJson.toString();
                            if (jsonString == null || jsonString.isEmpty()) {
                                jsonString = "{\"success\": false, \"message\": \"제어 OFF는 제어 ON을 한 사용자만 가능합니다.\", \"timer_running\": true}";
                            }
                            return newResponse(Response.Status.FORBIDDEN, Constants.HTTP.CONTENT_TYPE_JSON, jsonString);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to create error JSON for control owner check", e);
                            return newResponse(Response.Status.FORBIDDEN, Constants.HTTP.CONTENT_TYPE_JSON, 
                                "{\"success\": false, \"message\": \"제어 OFF는 제어 ON을 한 사용자만 가능합니다.\", \"timer_running\": true}");
                        }
                    }
                    
                    // 제어 요청 접근 메시지 삭제
                    try {
                        java.lang.reflect.Method clearControlRequestAccessMessageMethod = activity.getClass().getMethod("clearControlRequestAccessMessage");
                        clearControlRequestAccessMessageMethod.invoke(activity);
                        Log.d(TAG, "Control request access message cleared");
                    } catch (Exception e) {
                        Log.d(TAG, "Could not clear control request access message: " + e.getMessage());
                    }

                    // 타이머 중지
                    java.lang.reflect.Method stopBtMessageTimerMethod = activity.getClass().getDeclaredMethod("stopBtMessageTimer");
                    stopBtMessageTimerMethod.setAccessible(true);
                    stopBtMessageTimerMethod.invoke(activity);
                    
                    // 제어 모드 해제 (일반 모드로 복귀)
                    try {
                        java.lang.reflect.Field isControlModeField = activity.getClass().getDeclaredField("isControlMode");
                        isControlModeField.setAccessible(true);
                        isControlModeField.setBoolean(activity, false);
                        
                        java.lang.reflect.Field isControlOnField = activity.getClass().getDeclaredField("isControlOn");
                        isControlOnField.setAccessible(true);
                        isControlOnField.setBoolean(activity, false);
                        
                        Log.d(TAG, "Control mode disabled: isControlMode=false, isControlOn=false");
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to disable control mode", e);
                        // 제어 모드 해제 실패해도 계속 진행
                    }
                    
                    // 헤더 메시지 제거
                    try {
                        java.lang.reflect.Method runOnUiThreadMethod = activity.getClass().getMethod("runOnUiThread", Runnable.class);
                        Object finalActivity = activity;
                        runOnUiThreadMethod.invoke(activity, new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    java.lang.reflect.Field tvUnitMessageField = finalActivity.getClass().getDeclaredField("tvUnitMessage");
                                    tvUnitMessageField.setAccessible(true);
                                    Object tvUnitMessageObj = tvUnitMessageField.get(finalActivity);
                                    if (tvUnitMessageObj != null) {
                                        java.lang.reflect.Method setTextMethod = tvUnitMessageObj.getClass().getMethod("setText", CharSequence.class);
                                        setTextMethod.invoke(tvUnitMessageObj, "");
                                        Log.d(TAG, "Header message cleared");
                                    }
                                } catch (Exception e) {
                                    Log.d(TAG, "Could not clear header message: " + e.getMessage());
                                }
                            }
                        });
                    } catch (Exception e) {
                        Log.d(TAG, "Could not clear header message via runOnUiThread: " + e.getMessage());
                    }
                    
                    // 블루투스 연결 상태 확인 후 자동 검사 시작 또는 재연결
                    try {
                        java.lang.reflect.Field btConnectedField = activity.getClass().getDeclaredField("btConnected");
                        btConnectedField.setAccessible(true);
                        Object btConnectedObj = btConnectedField.get(activity);
                        boolean btConnected = btConnectedObj instanceof Boolean ? (Boolean) btConnectedObj : false;
                        
                        java.lang.reflect.Field btSocketField = activity.getClass().getDeclaredField("btSocket");
                        btSocketField.setAccessible(true);
                        Object btSocketObj = btSocketField.get(activity);
                        boolean isSocketConnected = false;
                        if (btSocketObj != null) {
                            try {
                                java.lang.reflect.Method isConnectedMethod = btSocketObj.getClass().getMethod("isConnected");
                                Object connectedObj = isConnectedMethod.invoke(btSocketObj);
                                isSocketConnected = connectedObj instanceof Boolean ? (Boolean) connectedObj : false;
                            } catch (Exception e) {
                                // isConnected 메서드가 없는 경우
                                Log.d(TAG, "Could not check socket connection status: " + e.getMessage());
                            }
                        }
                        
                        if (btConnected && isSocketConnected) {
                            // 블루투스 연결되어 있으면 자동 검사 시작
                            try {
                                java.lang.reflect.Method startBtMessageTimerMethod = activity.getClass().getDeclaredMethod("startBtMessageTimer");
                                startBtMessageTimerMethod.setAccessible(true);
                                startBtMessageTimerMethod.invoke(activity);
                                Log.i(TAG, "Bluetooth connected, auto test started after control OFF");
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to start auto test after control OFF", e);
                            }
                        } else {
                            // 블루투스 연결 안 되어 있으면 재연결 타이머 시작
                            try {
                                java.lang.reflect.Method scheduleBluetoothReconnectMethod = activity.getClass().getDeclaredMethod("scheduleBluetoothReconnect", boolean.class);
                                scheduleBluetoothReconnectMethod.setAccessible(true);
                                scheduleBluetoothReconnectMethod.invoke(activity, false);
                                Log.i(TAG, "Bluetooth not connected, reconnect timer started after control OFF");
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to start reconnect timer after control OFF", e);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to check Bluetooth connection status or start auto test", e);
                        // 블루투스 상태 확인 실패해도 계속 진행
                    }
                    
                    Log.i(TAG, "[제어 토글 완료] 제어 OFF 성공" +
                              " | 이전 제어 소유자: " + currentOwner +
                              " | 요청자: " + currentClient +
                              " | BT 메시지 타이머 중지됨");
                    
                    // 제어 소유자 초기화
                    clearControlOwner();
                    
                    JSONObject successJson = new JSONObject();
                    try {
                        successJson.put("success", true);
                        successJson.put("message", "타이머가 중지되었습니다.");
                        successJson.put("timer_running", false);
                        String jsonString = successJson.toString();
                        if (jsonString == null || jsonString.isEmpty()) {
                            jsonString = "{\"success\": true, \"message\": \"타이머가 중지되었습니다.\", \"timer_running\": false}";
                        }
                        return newResponse(Response.Status.OK, Constants.HTTP.CONTENT_TYPE_JSON, jsonString);
                    } catch (Exception jsonEx) {
                        Log.e(TAG, "Failed to create success JSON", jsonEx);
                        return newResponse(Response.Status.OK, Constants.HTTP.CONTENT_TYPE_JSON, 
                            "{\"success\": true, \"message\": \"타이머가 중지되었습니다.\", \"timer_running\": false}");
                    }
                } else {
                    // 제어 모드일 때는 블루투스 연결 상태와 관계없이 타이머 시작 가능
                    Log.i(TAG, "[제어 토글 요청] 이벤트: 제어 ON 요청" +
                              " | 현재 제어 소유자: " + currentOwner +
                              " | 요청자: " + currentClient);
                    
                    // 제어 모드 설정 (isControlMode = true, isControlOn = true)
                    try {
                        java.lang.reflect.Field isControlModeField = activity.getClass().getDeclaredField("isControlMode");
                        isControlModeField.setAccessible(true);
                        isControlModeField.setBoolean(activity, true);
                        
                        java.lang.reflect.Field isControlOnField = activity.getClass().getDeclaredField("isControlOn");
                        isControlOnField.setAccessible(true);
                        isControlOnField.setBoolean(activity, true);
                        
                        Log.d(TAG, "Control mode enabled: isControlMode=true, isControlOn=true");
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to set control mode", e);
                        // 제어 모드 설정 실패 시 에러 응답
                        JSONObject errorJson = new JSONObject();
                        try {
                            errorJson.put("success", false);
                            errorJson.put("message", "제어 모드 설정 실패: " + e.getMessage());
                            errorJson.put("timer_running", false);
                            return newResponse(Response.Status.INTERNAL_ERROR, Constants.HTTP.CONTENT_TYPE_JSON, errorJson.toString());
                        } catch (Exception jsonEx) {
                            return newResponse(Response.Status.INTERNAL_ERROR, Constants.HTTP.CONTENT_TYPE_JSON, 
                                "{\"success\": false, \"message\": \"제어 모드 설정 실패\"}");
                        }
                    }
                    
                    // ST0101 명령 전송 (제어 검사 대기 상태로 전환)
                    try {
                        java.lang.reflect.Method sendBtMessageMethod = activity.getClass().getDeclaredMethod("sendBtMessage", String.class);
                        sendBtMessageMethod.setAccessible(true);
                        sendBtMessageMethod.invoke(activity, Constants.TestItemCodes.ST0101);
                        Log.i(TAG, "ST0101 command sent for control mode activation");
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to send ST0101 command", e);
                        // ST0101 전송 실패해도 제어 모드는 활성화되었으므로 계속 진행
                    }
                    
                    // 타이머 시작 (제어 모드에서는 블루투스 연결 상태와 관계없이 시작 가능)
                    try {
                        java.lang.reflect.Method startBtMessageTimerMethod = activity.getClass().getDeclaredMethod("startBtMessageTimer");
                        startBtMessageTimerMethod.setAccessible(true);
                        startBtMessageTimerMethod.invoke(activity);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to start BT message timer", e);
                        // 타이머 시작 실패 시 에러 응답
                        JSONObject errorJson = new JSONObject();
                        try {
                            errorJson.put("success", false);
                            errorJson.put("message", "타이머 시작 실패: " + e.getMessage());
                            errorJson.put("timer_running", false);
                            return newResponse(Response.Status.INTERNAL_ERROR, Constants.HTTP.CONTENT_TYPE_JSON, errorJson.toString());
                        } catch (Exception jsonEx) {
                            return newResponse(Response.Status.INTERNAL_ERROR, Constants.HTTP.CONTENT_TYPE_JSON, 
                                "{\"success\": false, \"message\": \"타이머 시작 실패\"}");
                        }
                    }
                    
                    // 제어 소유자 설정 (웹 클라이언트)
                    setControlOwner(session, false, sessionIdParam);
                    
                    // 타이머가 실제로 시작되었는지 확인
                    boolean timerActuallyStarted = false;
                    try {
                        java.lang.reflect.Field btMessageTimerRunningField = activity.getClass().getDeclaredField("btMessageTimerRunning");
                        btMessageTimerRunningField.setAccessible(true);
                        Object btMessageTimerRunningObj = btMessageTimerRunningField.get(activity);
                        if (btMessageTimerRunningObj instanceof java.util.concurrent.atomic.AtomicBoolean) {
                            java.util.concurrent.atomic.AtomicBoolean atomicBoolean = (java.util.concurrent.atomic.AtomicBoolean) btMessageTimerRunningObj;
                            timerActuallyStarted = atomicBoolean.get();
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "Could not verify timer started status: " + e.getMessage());
                    }
                    
                    Log.i(TAG, "[제어 토글 완료] 제어 ON 성공" +
                              " | 새로운 제어 소유자: " + getControlOwnerInfo() +
                              " | 요청자: " + currentClient +
                              " | BT 메시지 타이머 시작됨: " + timerActuallyStarted);
                    
                    JSONObject successJson = new JSONObject();
                    try {
                        successJson.put("success", true);
                        successJson.put("message", "타이머가 시작되었습니다.");
                        successJson.put("timer_running", timerActuallyStarted);
                        String jsonString = successJson.toString();
                        if (jsonString == null || jsonString.isEmpty()) {
                            jsonString = "{\"success\": true, \"message\": \"타이머가 시작되었습니다.\", \"timer_running\": " + timerActuallyStarted + "}";
                        }
                        return newResponse(Response.Status.OK, Constants.HTTP.CONTENT_TYPE_JSON, jsonString);
                    } catch (Exception jsonEx) {
                        Log.e(TAG, "Failed to create success JSON", jsonEx);
                        return newResponse(Response.Status.OK, Constants.HTTP.CONTENT_TYPE_JSON, 
                            "{\"success\": true, \"message\": \"타이머가 시작되었습니다.\", \"timer_running\": " + timerActuallyStarted + "}");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to toggle timer: " + (isTimerRunning ? "stop" : "start"), e);
                JSONObject errorJson = new JSONObject();
                try {
                    errorJson.put("success", false);
                    String errorMessage = e.getMessage();
                    if (errorMessage == null || errorMessage.isEmpty()) {
                        errorMessage = "타이머 제어 중 오류가 발생했습니다.";
                    }
                    errorJson.put("message", "타이머 제어 실패: " + errorMessage);
                } catch (Exception e2) {
                    Log.e(TAG, "Failed to create error JSON", e2);
                    // 최소한의 JSON 응답 보장
                    return newResponse(Response.Status.INTERNAL_ERROR, Constants.HTTP.CONTENT_TYPE_JSON, 
                        "{\"success\": false, \"message\": \"타이머 제어 실패\"}");
                }
                String jsonString = errorJson.toString();
                if (jsonString == null || jsonString.isEmpty()) {
                    jsonString = "{\"success\": false, \"message\": \"타이머 제어 실패\"}";
                }
                return newResponse(Response.Status.INTERNAL_ERROR, Constants.HTTP.CONTENT_TYPE_JSON, jsonString);
            }
        } catch (Exception e) {
            Log.e(TAG, "제어 버튼 토글 처리 오류", e);
            JSONObject errorJson = new JSONObject();
            try {
                errorJson.put("success", false);
                String errorMessage = e.getMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "서버 오류가 발생했습니다.";
                }
                errorJson.put("message", "서버 오류: " + errorMessage);
            } catch (Exception e2) {
                Log.e(TAG, "Failed to create error JSON", e2);
                // 최소한의 JSON 응답 보장
                return newResponse(Response.Status.INTERNAL_ERROR, Constants.HTTP.CONTENT_TYPE_JSON, 
                    "{\"success\": false, \"message\": \"서버 오류\"}");
            }
            String jsonString = errorJson.toString();
            if (jsonString == null || jsonString.isEmpty()) {
                jsonString = "{\"success\": false, \"message\": \"서버 오류\"}";
            }
            return newResponse(Response.Status.INTERNAL_ERROR, Constants.HTTP.CONTENT_TYPE_JSON, jsonString);
        }
    }
    
    /**
     * 재검사 처리 (finishApplication 호출)
     */
    private Response handleTestRestart(IHTTPSession session) throws JSONException {
        if (context == null) {
            JSONObject errorJson = new JSONObject();
            errorJson.put("success", false);
            errorJson.put("message", "서버 초기화 오류");
            return newResponse(Response.Status.INTERNAL_ERROR, Constants.HTTP.CONTENT_TYPE_JSON, errorJson.toString());
        }

        Object activity = null;
        try {

            try {
                Class<?> activityModelClass = Class.forName("itf.com.app.lms.ActivityModelTestProcess");
                java.lang.reflect.Method getMainActivityMethod = activityModelClass.getMethod("getMainActivity");
                activity = getMainActivityMethod.invoke(null);
            } catch (Exception e) {
                Log.d(TAG, "Could not get Activity instance: " + e.getMessage());
            }
            /*
            try {
                Class<?> activityModelClass = Class.forName("itf.com.app.simple_line_test_ovio_new.ActivityModel_0002");
                java.lang.reflect.Method getMainActivityMethod = activityModelClass.getMethod("getMainActivity");
                activity = getMainActivityMethod.invoke(null);
                ActivityModel_0002 activityModel = (ActivityModel_0002) activity;
                assert activityModel != null;
                activityModel.resetBluetoothSessionKeepUsb();
            } catch (Exception e) {
                try {
                    Class<?> activityModelClass = Class.forName("itf.com.app.simple_line_test_ovio_new.ActivityModel_0001");
                    java.lang.reflect.Method getMainActivityMethod = activityModelClass.getMethod("getMainActivity");
                    activity = getMainActivityMethod.invoke(null);
                } catch (Exception e2) {
                    try {
                        Class<?> activityModelClass = Class.forName("itf.com.app.simple_line_test_ovio_new.ActivityModel_0003");
                        java.lang.reflect.Method getMainActivityMethod = activityModelClass.getMethod("getMainActivity");
                        activity = getMainActivityMethod.invoke(null);
                    } catch (Exception e3) {
                        Log.d(TAG, "Could not get Activity instance: " + e3.getMessage());
                    }
                }
            }
            */

            // 비동기로 실행되므로 즉시 성공 응답 반환
            JSONObject successJson = new JSONObject();
            successJson.put("success", true);
            successJson.put("message", "재검사가 시작되었습니다.");
            return newResponse(Response.Status.OK, Constants.HTTP.CONTENT_TYPE_JSON, successJson.toString());
        }
        catch (Exception e) {
            JSONObject errorJson = new JSONObject();
            errorJson.put("success", false);
            errorJson.put("message", "재검사 시도 중 오류가 발생했습니다.");
            return newResponse(Response.Status.BAD_REQUEST, Constants.HTTP.CONTENT_TYPE_JSON, errorJson.toString());
        }

        /*
        if (activity == null) {
            JSONObject errorJson = new JSONObject();
            errorJson.put("success", false);
            errorJson.put("message", "Activity 인스턴스를 찾을 수 없습니다.");
            return newResponse(Response.Status.INTERNAL_ERROR, Constants.HTTP.CONTENT_TYPE_JSON, errorJson.toString());
        }
        */

//        try {
//            if (context == null) {
//                JSONObject errorJson = new JSONObject();
//                errorJson.put("success", false);
//                errorJson.put("message", "서버 초기화 오류");
//                return newResponse(Response.Status.INTERNAL_ERROR, Constants.HTTP.CONTENT_TYPE_JSON, errorJson.toString());
//            }
//
//            // ActivityModel 인스턴스 가져오기
//            Object activity = null;
//            try {
//                Class<?> activityModelClass = Class.forName("itf.com.app.simple_line_test_ovio_new.ActivityModel_0002");
//                java.lang.reflect.Method getMainActivityMethod = activityModelClass.getMethod("getMainActivity");
//                activity = getMainActivityMethod.invoke(null);
//            } catch (Exception e) {
//                try {
//                    Class<?> activityModelClass = Class.forName("itf.com.app.simple_line_test_ovio_new.ActivityModel_0001");
//                    java.lang.reflect.Method getMainActivityMethod = activityModelClass.getMethod("getMainActivity");
//                    activity = getMainActivityMethod.invoke(null);
//                } catch (Exception e2) {
//                    try {
//                        Class<?> activityModelClass = Class.forName("itf.com.app.simple_line_test_ovio_new.ActivityModel_0003");
//                        java.lang.reflect.Method getMainActivityMethod = activityModelClass.getMethod("getMainActivity");
//                        activity = getMainActivityMethod.invoke(null);
//                    } catch (Exception e3) {
//                        Log.d(TAG, "Could not get Activity instance: " + e3.getMessage());
//                    }
//                }
//            }
//
//            if (activity == null) {
//                JSONObject errorJson = new JSONObject();
//                errorJson.put("success", false);
//                errorJson.put("message", "Activity 인스턴스를 찾을 수 없습니다.");
//                return newResponse(Response.Status.INTERNAL_ERROR, Constants.HTTP.CONTENT_TYPE_JSON, errorJson.toString());
//            }
//
//            // finishApplication 메서드를 메인 스레드에서 호출
//            try {
//                // Activity가 Activity 인스턴스인지 확인
//                if (!(activity instanceof android.app.Activity)) {
//                    JSONObject errorJson = new JSONObject();
//                    errorJson.put("success", false);
//                    errorJson.put("message", "Activity 인스턴스가 올바르지 않습니다.");
//                    return newResponse(Response.Status.INTERNAL_ERROR, Constants.HTTP.CONTENT_TYPE_JSON, errorJson.toString());
//                }
//
//                android.app.Activity activityInstance = (android.app.Activity) activity;
//
//                // Activity가 이미 종료 중이거나 파괴되었는지 확인
//                try {
//                    java.lang.reflect.Method isFinishingMethod = activityInstance.getClass().getMethod("isFinishing");
//                    boolean isFinishing = (Boolean) isFinishingMethod.invoke(activityInstance);
//                    if (isFinishing) {
//                        JSONObject errorJson = new JSONObject();
//                        errorJson.put("success", false);
//                        errorJson.put("message", "Activity가 이미 종료 중입니다.");
//                        return newResponse(Response.Status.BAD_REQUEST, Constants.HTTP.CONTENT_TYPE_JSON, errorJson.toString());
//                    }
//                } catch (Exception e) {
//                    // isFinishing 메서드가 없거나 호출 실패 시 무시하고 계속 진행
//                    Log.d(TAG, "Could not check isFinishing: " + e.getMessage());
//                }
//
//                // isDestroyed 확인 (API 17+)
//                try {
//                    java.lang.reflect.Method isDestroyedMethod = activityInstance.getClass().getMethod("isDestroyed");
//                    boolean isDestroyed = (Boolean) isDestroyedMethod.invoke(activityInstance);
//                    if (isDestroyed) {
//                        JSONObject errorJson = new JSONObject();
//                        errorJson.put("success", false);
//                        errorJson.put("message", "Activity가 이미 파괴되었습니다.");
//                        return newResponse(Response.Status.BAD_REQUEST, Constants.HTTP.CONTENT_TYPE_JSON, errorJson.toString());
//                    }
//                } catch (Exception e) {
//                    // isDestroyed 메서드가 없거나 호출 실패 시 무시하고 계속 진행
//                    Log.d(TAG, "Could not check isDestroyed: " + e.getMessage());
//                }
//
//                // finishApplication 호출 전에 USB 핸들러 메시지 정리 및 리소스 정리
//                try {
//                    // USB 핸들러 메시지 정리
//                    java.lang.reflect.Field usbHandlerField = activity.getClass().getDeclaredField("usbHandler");
//                    usbHandlerField.setAccessible(true);
//                    Object usbHandlerObj = usbHandlerField.get(activity);
//                    if (usbHandlerObj != null && usbHandlerObj instanceof android.os.Handler) {
//                        android.os.Handler usbHandler = (android.os.Handler) usbHandlerObj;
//                        usbHandler.removeCallbacksAndMessages(null);
//                        Log.d(TAG, "USB Handler messages cleared before finishApplication");
//                    }
//
//                    // cleanupUsbResources 메서드 호출 (있는 경우)
//                    try {
//                        java.lang.reflect.Method cleanupUsbResourcesMethod = activity.getClass().getDeclaredMethod("cleanupUsbResources");
//                        cleanupUsbResourcesMethod.setAccessible(true);
//                        cleanupUsbResourcesMethod.invoke(activity);
//                        Log.d(TAG, "USB resources cleaned up before finishApplication");
//                    } catch (Exception e) {
//                        // cleanupUsbResources 메서드가 없거나 호출 실패 시 무시
//                        Log.d(TAG, "Could not call cleanupUsbResources: " + e.getMessage());
//                    }
//                } catch (Exception e) {
//                    Log.d(TAG, "Could not clear USB Handler messages: " + e.getMessage());
//                }
//
//                // 메인 스레드에서 finishApplication 호출
//                Object finalActivity = activity;
//                activityInstance.runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        try {
//                            // 다시 한 번 Activity 상태 확인
//                            try {
//                                java.lang.reflect.Method isFinishingMethod = activityInstance.getClass().getMethod("isFinishing");
//                                boolean isFinishing = (Boolean) isFinishingMethod.invoke(activityInstance);
//                                if (isFinishing) {
//                                    Log.w(TAG, "Activity is finishing, skipping finishApplication");
//                                    return;
//                                }
//                            } catch (Exception e) {
//                                // 무시하고 계속 진행
//                            }
//
//                            // finishApplication 호출 전에 다시 한 번 USB 핸들러 메시지 정리
//                            try {
//                                java.lang.reflect.Field usbHandlerField = finalActivity.getClass().getDeclaredField("usbHandler");
//                                usbHandlerField.setAccessible(true);
//                                Object usbHandlerObj = usbHandlerField.get(finalActivity);
//                                if (usbHandlerObj != null && usbHandlerObj instanceof android.os.Handler) {
//                                    android.os.Handler usbHandler = (android.os.Handler) usbHandlerObj;
//                                    usbHandler.removeCallbacksAndMessages(null);
//                                }
//                            } catch (Exception e) {
//                                // 무시하고 계속 진행
//                            }
//
//                            java.lang.reflect.Method finishApplicationMethod = finalActivity.getClass().getDeclaredMethod("finishApplication", Context.class);
//                            finishApplicationMethod.setAccessible(true);
//                            finishApplicationMethod.invoke(finalActivity, context);
//                            Log.i(TAG, "Test restart (finishApplication) executed on main thread");
//                        } catch (Exception e) {
//                            Log.e(TAG, "Failed to execute test restart: finishApplication", e);
//                        }
//                    }
//                });
//
//                // 비동기로 실행되므로 즉시 성공 응답 반환
//                JSONObject successJson = new JSONObject();
//                successJson.put("success", true);
//                successJson.put("message", "재검사가 시작되었습니다.");
//                return newResponse(Response.Status.OK, Constants.HTTP.CONTENT_TYPE_JSON, successJson.toString());
//            } catch (Exception e) {
//                Log.e(TAG, "Failed to execute test restart: finishApplication", e);
//                JSONObject errorJson = new JSONObject();
//                try {
//                    errorJson.put("success", false);
//                    errorJson.put("message", "재검사 실행 실패: " + e.getMessage());
//                } catch (Exception e2) {
//                    // JSON 생성 실패
//                }
//                return newResponse(Response.Status.INTERNAL_ERROR, Constants.HTTP.CONTENT_TYPE_JSON, errorJson.toString());
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "테스트 재시작 처리 오류", e);
//            JSONObject errorJson = new JSONObject();
//            try {
//                errorJson.put("success", false);
//                errorJson.put("message", "서버 오류: " + e.getMessage());
//            } catch (Exception e2) {
//                // JSON 생성 실패
//            }
//            return newResponse(Response.Status.INTERNAL_ERROR, Constants.HTTP.CONTENT_TYPE_JSON, errorJson.toString());
//        }
    }
}