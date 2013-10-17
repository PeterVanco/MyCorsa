package sk.petervanco.adapter;

import java.util.ArrayList;
import java.util.HashMap;

import sk.petervanco.fragment.ConnectionFragment;
import sk.petervanco.mycorsa.CorsaActivity;
import sk.petervanco.mycorsa.R;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class BtListAdapter extends BaseAdapter {

    public Activity activity;
    private ArrayList<HashMap<String, String>> data;
    private static LayoutInflater inflater = null;
    
    public BtListAdapter(Activity a, ArrayList<HashMap<String, String>> d) {
        activity = a;
        data = d;
        inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
	
	@Override
	public int getCount() {
        return data.size();
	}

	@Override
	public Object getItem(int position) {
        return position;
	}

	@Override
	public long getItemId(int position) {
        return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
        
		View vi = convertView;
        if (convertView == null)
            vi = inflater.inflate(R.layout.bt_discovery_listitem, null);
 
        final TextView mBtDeviceName = (TextView) vi.findViewById(R.id.bt_device_name);
        final TextView mBtDeviceAddress = (TextView) vi.findViewById(R.id.bt_device_address);
        ImageView mBtDeviceImage = (ImageView) vi.findViewById(R.id.bt_device_image);
        Button mBtnConnect = (Button) vi.findViewById(R.id.btn_bt_connect);
        
        mBtnConnect.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {

				if (activity.getClass().toString().equals(CorsaActivity.class.toString())) {
					((CorsaActivity)activity).connectRequest(mBtDeviceName.getText().toString(), mBtDeviceAddress.getText().toString());
				}
			}
		});
        
        HashMap<String, String> mBtDevice = new HashMap<String, String>();
        mBtDevice = data.get(position);
 
        // Setting all values in listview
        mBtDeviceName.setText(mBtDevice.get(ConnectionFragment.KEY_DEVICE));
        mBtDeviceAddress.setText(mBtDevice.get(ConnectionFragment.KEY_ADDRESS));
        mBtDeviceImage.setImageResource(R.drawable.ic_launcher);
        
        return vi;
	}

}




