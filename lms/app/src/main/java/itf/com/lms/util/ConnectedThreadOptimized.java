package itf.com.lms.util;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.SystemClock;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

// ActivityModel_0001과 ActivityModel_0002 모두에서 사용 가능하도록 수정

/**
 * 최적화된 ConnectedThread 클래스
 * 메시지 디바운싱을 통해 메인 스레드 과부하를 방지합니다.
 * ActivityModel_0002에서만 사용됩니다.
 */
public class ConnectedThreadOptimized extends Thread {
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private final Handler mHandler;
    
    // 메시지 디바운싱을 위한 변수 추가
    private long lastMessageTime = 0;
    private static final long MESSAGE_THROTTLE_MS = 50; // 50ms마다 최대 1번만 전송

    public ConnectedThreadOptimized(BluetoothSocket socket, Handler handler) {
        mmSocket = socket;
        mHandler = handler;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) { }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024];  // buffer store for the stream
        int bytes; // bytes returned from read()
        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                // Read from the InputStream
                bytes = mmInStream.available();
                if(bytes != 0) {
                    buffer = new byte[1024];
                    SystemClock.sleep(100); //pause and wait for rest of data. Adjust this depending on your sending speed.
                    bytes = mmInStream.available(); // how many bytes are ready to be read?
                    bytes = mmInStream.read(buffer, 0, bytes); // record how many bytes we actually read
                    
                    // 메시지 디바운싱: 일정 시간 간격으로만 메시지 전송하여 메인 스레드 과부하 방지
                    long currentTime = SystemClock.uptimeMillis();
                    if (currentTime - lastMessageTime >= MESSAGE_THROTTLE_MS) {
                        // MESSAGE_READ는 ActivityModel_0001과 ActivityModel_0002 모두에서 동일한 값(2)을 사용
                        mHandler.obtainMessage(2, bytes, -1, buffer)
                                .sendToTarget(); // Send the obtained bytes to the UI activity
                        lastMessageTime = currentTime;
                    }
                } else {
                    // 데이터가 없을 때는 sleep 추가하여 CPU 사용량 감소
                    SystemClock.sleep(50);
                }
            } catch (IOException e) {
                e.printStackTrace();

                break;
            }
        }
        // return null;
    }

    /* Call this from the main activity to send data to the remote device */
    public void write(String input) {
        byte[] bytes = input.getBytes();           //converts entered String into bytes
        try {
            mmOutStream.write(bytes);
        } catch (IOException e) { }
    }

    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }
}



