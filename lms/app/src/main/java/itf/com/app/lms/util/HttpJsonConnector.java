package itf.com.app.lms.util;



import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpJsonConnector extends Thread{

    @Override
    public void run(){
        try{
            URL url = new URL("URL 주소를 입력");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            if(conn != null){
                conn.setConnectTimeout(5000);
                conn.setRequestMethod("GET");

                int resCode = conn.getResponseCode();

                if(resCode == HttpURLConnection.HTTP_OK){
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line = null;

                    while(true){
                        line = reader.readLine();

                        Log.d("JsonParsing", "line : " + line);

                        if(line == null){
                            break;
                        }
                    }
                    }
                }
                conn.disconnect();
            }
        }catch (Exception e){

        }
        // return null;
    }
}