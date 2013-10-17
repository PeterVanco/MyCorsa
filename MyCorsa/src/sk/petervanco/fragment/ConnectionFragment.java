/*******************************************************************************
 * Copyright 2012 Steven Rudenko
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package sk.petervanco.fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import sk.petervanco.adapter.BtListAdapter;
import sk.petervanco.mycorsa.R;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

public class ConnectionFragment extends Fragment implements OnClickListener{
  
  public static final String TAG = ConnectionFragment.class.getSimpleName();

  private static final String ABOUT_SCHEME = "settings";
  private static final String ABOUT_AUTHORITY = "connection";
  public static final Uri ABOUT_URI = new Uri.Builder()
											  .scheme(ABOUT_SCHEME)
											  .authority(ABOUT_AUTHORITY)
											  .build();
  
  
  public static final String KEY_DEVICE  = "name"; // parent node
  public static final String KEY_ADDRESS = "address";
  public static final String KEY_IMAGE   = "image";  

  private BluetoothAdapter mBtAdapter;
  private ArrayList<HashMap<String, String>> mPairedDevices;
  private BtListAdapter mBtListAdapter;
  
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {

    final View v = inflater.inflate(R.layout.connection, container, false);

    v.findViewById(R.id.btn_bt_discover).setOnClickListener(this);
    
    mPairedDevices = new ArrayList<HashMap<String,String>>();
    
    
    
    mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

    if (pairedDevices.size() > 0) {
        //findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
        for (BluetoothDevice device : pairedDevices) {
        	HashMap<String, String> mPair = new HashMap<String, String>();
        	mPair.put(KEY_DEVICE, device.getName());
        	mPair.put(KEY_ADDRESS, device.getAddress());
        	mPair.put(KEY_IMAGE, device.getBluetoothClass().toString());
        	mPairedDevices.add(mPair);
        }
    } else {
        String noDevices = getResources().getText(R.string.no_device).toString();
    	HashMap<String, String> mPair = new HashMap<String, String>();
    	mPair.put(KEY_DEVICE, noDevices);
    	mPair.put(KEY_ADDRESS, "");
    	mPair.put(KEY_IMAGE, "");
    	mPairedDevices.add(mPair);
    }    


    mBtListAdapter = new BtListAdapter(getActivity(), mPairedDevices);
    ListView mBtDevicesList = (ListView) v.findViewById(R.id.bt_devices);
    mBtDevicesList.setAdapter(mBtListAdapter);
    mBtDevicesList.setOnItemClickListener(new OnItemClickListener() {
    	 
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			// TODO Auto-generated method stub
		}
	});    
    
    return v;
  }

  public void onClick(View v) {

	  switch (v.getId()) {
	  
	  	case R.id.btn_bt_discover:
	  		Toast.makeText(getActivity(), "Discovering", Toast.LENGTH_SHORT).show();
		break;

	  	default:
		break;
	}
  
  }

}
