package sk.petervanco.fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import sk.petervanco.adapter.BtListAdapter;
import sk.petervanco.mycorsa.R;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
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

  private static final String CONNECTION_SCHEME = "settings";
  private static final String CONNECTION_AUTHORITY = "connection";
  public static final Uri CONNECTION_URI = new Uri.Builder()
											  .scheme(CONNECTION_SCHEME)
											  .authority(CONNECTION_AUTHORITY)
											  .build();
  
  
  public static final String KEY_DEVICE  = "name"; // parent node
  public static final String KEY_ADDRESS = "address";
  public static final String KEY_IMAGE   = "image";  

  private BluetoothAdapter mBtAdapter;
  private ArrayList<HashMap<String, String>> mPairedDevices;
  private BtListAdapter mBtListAdapter;
  
  private void copyPairedDevices() {
	  
	  mPairedDevices.clear();
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
	  
  }
  
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {

    final View v = inflater.inflate(R.layout.connection, container, false);

    v.findViewById(R.id.btn_bt_discover).setOnClickListener(this);
    
    // Register for broadcasts when a device is discovered
    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
    getActivity().registerReceiver(mReceiver, filter);

    // Register for broadcasts when discovery has finished
    filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
    getActivity().registerReceiver(mReceiver, filter);
    
    mPairedDevices = new ArrayList<HashMap<String,String>>();
    mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    copyPairedDevices();


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

  private void doDiscovery() {
      Log.d(TAG, "doDiscovery()");

      copyPairedDevices();
  	  mBtListAdapter.notifyDataSetChanged();

      // Indicate scanning in the title
      getActivity().setProgressBarIndeterminateVisibility(true);
      getActivity().setTitle(R.string.info_discovery);

      // Turn on sub-title for new devices
      //findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

      // If we're already discovering, stop it
      if (mBtAdapter.isDiscovering()) {
          mBtAdapter.cancelDiscovery();
      }

      // Request discover from BluetoothAdapter
      mBtAdapter.startDiscovery();
  }  
  
  private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
          String action = intent.getAction();

          // When discovery finds a device
          if (BluetoothDevice.ACTION_FOUND.equals(action)) {
              // Get the BluetoothDevice object from the Intent
              BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
              // If it's already paired, skip it, because it's been listed already
              if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
				HashMap<String, String> mPair = new HashMap<String, String>();
				mPair.put(KEY_DEVICE, device.getName());
				mPair.put(KEY_ADDRESS, device.getAddress());
				mPair.put(KEY_IMAGE, device.getBluetoothClass().toString());
				mPairedDevices.add(mPair);
				mBtListAdapter.notifyDataSetChanged();
              }
          // When discovery is finished, change the Activity title
          } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
              getActivity().setProgressBarIndeterminateVisibility(false);
              getActivity().setTitle(R.string.app_name);
//              if (mNewDevicesArrayAdapter.getCount() == 0) {
//                  String noDevices = getResources().getText(R.string.none_found).toString();
//                  mNewDevicesArrayAdapter.add(noDevices);
//              }
          }
      }
  };  
  
  public void onClick(View v) {

	  switch (v.getId()) {
	  
	  	case R.id.btn_bt_discover:
	  		Toast.makeText(getActivity(), "Discovering", Toast.LENGTH_SHORT).show();
	  		doDiscovery();
	  		break;

	  	default:
		break;
	}
  
  }

}
