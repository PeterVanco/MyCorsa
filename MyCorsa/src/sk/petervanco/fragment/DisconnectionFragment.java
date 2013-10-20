package sk.petervanco.fragment;

import sk.petervanco.mycorsa.CorsaActivity;
import sk.petervanco.mycorsa.R;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;


public class DisconnectionFragment extends Fragment implements OnClickListener {

	private CorsaActivity mActivity = null;

	  public static final String TAG = DisconnectionFragment.class.getSimpleName();
	
	private static final String DISCONNECTION_SCHEME = "settings";
	  private static final String DISCONNECTION_AUTHORITY = "disconnection";
	  public static final Uri DISCONNECTION_URI = new Uri.Builder()
												  .scheme(DISCONNECTION_SCHEME)
												  .authority(DISCONNECTION_AUTHORITY)
												  .build();
	
	  
	  @Override
	  public View onCreateView(LayoutInflater inflater, ViewGroup container,
	      Bundle savedInstanceState) {

		  mActivity = (CorsaActivity) getActivity();
		  
	    final View v = inflater.inflate(R.layout.disconnection, container, false);

	    v.findViewById(R.id.btn_bt_disconnect).setOnClickListener(this);

	    TextView name = (TextView) v.findViewById(R.id.disconnection_bt_name);
	    name.setText(mActivity.getConnectedDeviceName());
	    TextView address = (TextView) v.findViewById(R.id.disconnection_bt_address);
	    address.setText(mActivity.getConnectedDeviceAddress());
	    
  	    return v;
	  }

	  	  
	
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		
		case R.id.btn_bt_disconnect:
			mActivity.disconnectBluetooth();
			break;

		default:
			break;
		}
		
	}

}
