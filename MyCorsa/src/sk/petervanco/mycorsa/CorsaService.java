package sk.petervanco.mycorsa;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class CorsaService extends Service {

	static final String TAG = "CorsaService";
	
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
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
		Log.d(TAG, "Service destroyed!");
    }    
	
	
}
