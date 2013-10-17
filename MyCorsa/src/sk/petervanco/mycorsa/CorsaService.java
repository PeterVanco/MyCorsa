package sk.petervanco.mycorsa;

import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class CorsaService extends Service {

	static final String TAG = "CorsaService";
	
    // Name for the SDP record when creating server socket
    private static final String NAME_SECURE = "BluetoothChatSecure";
    private static final String NAME_INSECURE = "BluetoothChatInsecure";

    // Unique UUID for this application
    private static final UUID MY_UUID_SECURE =
        UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final UUID MY_UUID_INSECURE =
        UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private int mBtState;
    
    // Constants that indicate the current connection state
    public static final int BT_STATE_NONE = 0;       // we're doing nothing
    public static final int BT_STATE_CONNECTING = 1; // now initiating an outgoing connection
    public static final int BT_STATE_CONNECTED = 2;  // now connected to a remote device	
	
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
    
    private synchronized void setBtState(int state) {
        Log.d(TAG, "State change: " + mBtState + " -> " + state);
        mBtState = state;

        mHandler.obtainMessage(CorsaActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    public synchronized int getBtState() {
        return mBtState;
    }    
    
    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    public synchronized void start() {
        Log.d(TAG, "Starting");

        /*
        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        setState(STATE_LISTEN);

        // Start the thread to listen on a BluetoothServerSocket
        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = new AcceptThread(true);
            mSecureAcceptThread.start();
        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread(false);
            mInsecureAcceptThread.start();
        }
        */
    }    
    
    public void write(byte[] out) {
    	
    	Log.d(TAG, "Writing to BT");
    	
    	/*
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
        */
    	
    }    
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
		Log.d(TAG, "Service destroyed!");
    }    
	
	
}
