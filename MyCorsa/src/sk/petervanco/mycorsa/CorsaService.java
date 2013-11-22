package sk.petervanco.mycorsa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.UUID;

import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

public class CorsaService extends Service {

	static final String TAG = "CorsaService";
	
    // Name for the SDP record when creating server socket
    private static final String NAME_SECURE = "BluetoothChatSecure";
    private static final String NAME_INSECURE = "BluetoothChatInsecure";

    // Unique UUID for this application
    private static final UUID MY_UUID_SECURE =
        UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final UUID MY_UUID_INSECURE =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private int mBtState;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private Boolean mDfuInProgress = false;
    private String[] mHexRecords;
    private int mHexRecordsPtr = 0;
    private String mBuffer = "";
    
    // Constants that indicate the current connection state
    public static final int BT_STATE_NONE = 0;       // we're doing nothing
    public static final int BT_STATE_CONNECTING = 1; // now initiating an outgoing connection
    public static final int BT_STATE_CONNECTED = 2;  // now connected to a remote device	
    public static final int BT_STATE_DISCONNECTED = 3;  // now connected to a remote device	

	private static final long DFU_TIMEOUT = 5000;
	
    public CorsaService(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mBtState = BT_STATE_NONE;
        mHandler = handler;
        Log.d(TAG, "Service constructor");
    }
    
	@Override
	public IBinder onBind(Intent arg0) {
		Log.d(TAG, "Service bind");
		return null;
	}
	
    @Override
    public void onCreate() {
        super.onCreate();
        
		Log.d(TAG, "Service created");
    }
    
    public static String convertStreamToString(InputStream is) throws Exception {
	    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
	    StringBuilder sb = new StringBuilder();
	    String line = null;
	    while ((line = reader.readLine()) != null) {
	      sb.append(line).append("\n");
	    }
	    return sb.toString();
	}

	public static String getStringFromFile (String filePath) throws Exception {
	    File fl = new File(filePath);
	    FileInputStream fin = new FileInputStream(fl);
	    String ret = convertStreamToString(fin);
	    //Make sure you close all streams.
	    fin.close();        
	    return ret;
	}
	
	private void continueDfu() {
		Log.d("DFU", mHexRecordsPtr + " / " + (mHexRecords.length - 1));
		
		if (mHexRecordsPtr <= mHexRecords.length) {
			int i;
			String feed = "";
			for (i = 0; i < 5; i++) {
				feed += mHexRecords[mHexRecordsPtr + i];// + "\n";
				if (mHexRecordsPtr + i + 1 == mHexRecords.length) {
					feed += "#EOF\n";
					break;
				}
			}
			feed += "\n";
			Log.d("DFU", "Sending: " + feed);
			
//			try {
//				Thread.sleep(250);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			
			write(feed.getBytes());

			Message msg = mHandler.obtainMessage(CorsaActivity.MESSAGE_DFU_PROGRESS, (mHexRecords.length - 1), mHexRecordsPtr);
	        mHandler.sendMessage(msg);			
            
			timeoutHandler.removeCallbacks(timeoutRunnable);
			timeoutHandler.postDelayed(timeoutRunnable, DFU_TIMEOUT);
		}
	}
	
    Handler timeoutHandler = new Handler();
    Runnable timeoutRunnable = new Runnable() {

        @Override
        public void run() {

	        Message msg = mHandler.obtainMessage(CorsaActivity.MESSAGE_DFU_FAILED);
	        mHandler.sendMessage(msg);
//        	Toast.makeText(mContext, "Firmware update failed", Toast.LENGTH_SHORT).show();
			Log.d("DFU", "(failed)");
        	mDfuInProgress = false;
        }
    };
	
	private synchronized void parseDfuMessage(String s) {
        
		mBuffer += s;
		
		
		while (true)
		{
			Log.d("DFU", "(rec) " + mBuffer);

			if (mBuffer.startsWith("#OK\n")) {
				mBuffer = mBuffer.substring(mBuffer.indexOf("\n") + 1);
				mHexRecordsPtr += 5;
				Log.d("DFU", "OK call");
				continueDfu();
			}
			else if (mBuffer.startsWith("#RESEND\n")) {
				mBuffer = mBuffer.substring(mBuffer.indexOf("\n") + 1);
				Log.d("DFU", "#RESEND");
				continueDfu();
			}
			else if (mBuffer.startsWith("#CHECK\n")) {
				mBuffer = mBuffer.substring(mBuffer.indexOf("\n") + 1);
				Log.d("DFU", "#CHECK-OK");
				write("#CHECK-OK\n".getBytes());
			}
	        else if (mBuffer.startsWith("#DFU-WAIT\n")) {
	        	mBuffer = mBuffer.substring(mBuffer.indexOf("\n") + 1);
				Log.d("DFU", "#DFU-WAIT");
				timeoutHandler.removeCallbacks(timeoutRunnable);
				timeoutHandler.postDelayed(timeoutRunnable, DFU_TIMEOUT);
	        }
	        else if (mBuffer.startsWith("#EOF-OK\n")) {
	        	mBuffer = mBuffer.substring(mBuffer.indexOf("\n") + 1);
				Log.d("DFU", "#EOF-OK");
				timeoutHandler.removeCallbacks(timeoutRunnable);
				
		        Message msg = mHandler.obtainMessage(CorsaActivity.MESSAGE_DFU_END);
		        mHandler.sendMessage(msg);
			
				mDfuInProgress = false;
	        }
	        else if (mBuffer.startsWith("#START\n")) {
	        	mBuffer = mBuffer.substring(mBuffer.indexOf("\n") + 1);
				Log.d("DFU", "#DFU-READY");
				Log.d("DFU", "READY call");
				continueDfu();
	        }
	        else
	        {
	        	break;
	        }
		}
	}
	
	
    public synchronized void startUpgradeFirmware(String filePath) {
    	
		try {
			Log.d("DFU", "File: " + filePath);
			String content = getStringFromFile(filePath);
			Log.d("DFU", "Content: " + content.substring(0,30).toString());
			mHexRecords = content.split("\n");
			Log.d("DFU", "Record: " + mHexRecords[0]);
			mHexRecordsPtr = 0;
			mDfuInProgress = true;
			mBuffer = "";
			write("#DFU\n".getBytes());		
			
	        Message msg = mHandler.obtainMessage(CorsaActivity.MESSAGE_DFU_BEGIN);
	        mHandler.sendMessage(msg);
			
			timeoutHandler.postDelayed(timeoutRunnable, DFU_TIMEOUT);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    private synchronized void setBtState(int state) {
        Log.d(TAG, "State change: " + mBtState + " -> " + state);
        mBtState = state;

        mHandler.obtainMessage(CorsaActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    public synchronized int getBtState() {
        return mBtState;
    }    
    
    public synchronized void disconnect() {
        if (mBtState == BT_STATE_CONNECTED) {
            // disconnect
        	if (mConnectedThread != null) {
            	mConnectedThread.cancel(); 
            	mConnectedThread = null;
            }
            setBtState(BT_STATE_DISCONNECTED);
        }
    }
    
    public synchronized void connect(BluetoothDevice device, boolean secure) {
        Log.d(TAG, "Connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mBtState == BT_STATE_CONNECTING) {
            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device, secure);
        mConnectThread.start();
        setBtState(BT_STATE_CONNECTING);
    }
    
    public synchronized void start() {
        Log.d(TAG, "Starting");

        
        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        setBtState(BT_STATE_NONE);
    }    
    
    public void write(byte[] out) {
    	
    	Log.d(TAG, "Writing to BT");
    	
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mBtState != BT_STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    	
    }    
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
		Log.d(TAG, "Service destroyed!");
    }    
	
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(CorsaActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(CorsaActivity.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        CorsaService.this.start();
    }
    
    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(CorsaActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(CorsaActivity.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        setBtState(BT_STATE_DISCONNECTED);

        // Start the service over to restart listening mode
        CorsaService.this.start();
    }    

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {
        Log.d(TAG, "connected, Socket Type:" + socketType);

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(CorsaActivity.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(CorsaActivity.EXTRA_DEVICE_NAME, device.getName());
        bundle.putString(CorsaActivity.EXTRA_DEVICE_ADDRESS, device.getAddress());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setBtState(BT_STATE_CONNECTED);
    }
    
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;

        public ConnectThread(BluetoothDevice device, boolean secure) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                if (secure) {
                    tmp = device.createRfcommSocketToServiceRecord(
                            MY_UUID_SECURE);
                } else {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(
                            MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " + mSocketType +
                            " socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (CorsaService.this) {
                mConnectThread = null;
            }

            // Start the connected thread

            connected(mmSocket, mmDevice, mSocketType);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            Log.d(TAG, "create ConnectedThread: " + socketType);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    // Send the obtained bytes to the UI Activity
                	String dfuMessage = new String(buffer, 0, bytes);
        			Log.d("DFU", "(read) " + bytes + " " + dfuMessage + " " + mDfuInProgress);
                    if (mDfuInProgress) {
                    	parseDfuMessage(dfuMessage);
                    }
                    else {
                        mHandler.obtainMessage(CorsaActivity.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    // Start the service over to restart listening mode
                    CorsaService.this.start();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(CorsaActivity.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }	
}
