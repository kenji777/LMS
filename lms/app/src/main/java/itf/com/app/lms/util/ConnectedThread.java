package itf.com.app.lms.util;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import static itf.com.app.lms.util.Constants.Bluetooth.MESSAGE_READ;

public class ConnectedThread extends Thread {
    private static final String TAG = "ConnectedThread";
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private Handler mHandler;  // ⚠️ CRITICAL FIX: Not final, so we can null it
    
    // ⚠️ CRITICAL FIX: Add flag to control thread lifecycle
    private volatile boolean shouldRun = true;

    public ConnectedThread(BluetoothSocket socket, Handler handler) {
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
        
        // ⚠️ CRITICAL FIX: Check both shouldRun flag and interrupt status
        while (shouldRun && !Thread.currentThread().isInterrupted()) {
            try {
                // Read from the InputStream
                bytes = mmInStream.available();
                if(bytes != 0) {
                    buffer = new byte[1024];
                    SystemClock.sleep(100); //pause and wait for rest of data. Adjust this depending on your sending speed.
                    bytes = mmInStream.available(); // how many bytes are ready to be read?
                    bytes = mmInStream.read(buffer, 0, bytes); // record how many bytes we actually read
                    
                    // 블루투스 메시지 수신 로그
                    String receivedMessage = new String(buffer, 0, bytes, StandardCharsets.UTF_8);
                    Log.i(TAG, "▶ [BT-RX] Bluetooth message received - Bytes: " + bytes + ", Message: [" + receivedMessage + "]");
                    
                    // ⚠️ CRITICAL FIX: Check handler is not null before using
                    if (mHandler != null) {
                        mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                                .sendToTarget(); // Send the obtained bytes to the UI activity
                    }
                }
            } catch (IOException e) {
                if (shouldRun) {
                    e.printStackTrace();
                }
                break;
            } catch (Exception e) {
                // ⚠️ CRITICAL FIX: Handle thread interruption properly
                Thread.currentThread().interrupt();
                break;
            }
            /*
            } catch (InterruptedException e) {
                // ⚠️ CRITICAL FIX: Handle thread interruption properly
                Thread.currentThread().interrupt();
                break;
            }
            */
        }
        
        Log.i(TAG, "ConnectedThread exiting run loop");
    }

    /* Call this from the main activity to send data to the remote device */
    public void write(String input) {
        byte[] bytes = input.getBytes();           //converts entered String into bytes
        try {
            if (mmOutStream != null) {
                mmOutStream.write(bytes);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error writing to output stream", e);
        }
    }

    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
        // ⚠️ CRITICAL FIX: Proper cleanup to prevent memory leaks
        shouldRun = false;
        
        // Interrupt the thread if it's sleeping
        interrupt();
        
        // Close the socket
        try {
            if (mmSocket != null) {
                mmSocket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing socket", e);
        }
        
        // ⚠️ CRITICAL FIX: Clear Handler reference to prevent memory leak
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        
        Log.i(TAG, "ConnectedThread cancelled and cleaned up");
    }
}
