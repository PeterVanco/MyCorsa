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
package sk.petervanco.mycorsa;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import shared.ui.actionscontentview.ActionsContentView;
import sk.petervanco.adapter.ActionsAdapter;
import sk.petervanco.fragment.ConnectionFragment;
import sk.petervanco.fragment.DisconnectionFragment;
import sk.petervanco.fragment.FirmwareFragment;
import sk.petervanco.fragment.ModeFragment;
import sk.petervanco.fragment.SandboxFragment;
import sk.petervanco.fragment.WebViewFragment;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.haarman.listviewanimations.swinginadapters.prepared.SwingRightInAnimationAdapter;
import com.ipaulpro.afilechooser.utils.FileUtils;

public class CorsaActivity extends FragmentActivity {

  private static final String TAG = "CorsaActivity";
  private static final String STATE_URI = "state:uri";
  private static final String STATE_FRAGMENT_TAG = "state:fragment_tag";

  private SettingsChangedListener mSettingsChangedListener;

  private ActionsContentView viewActionsContentView;

  private Uri currentUri = ConnectionFragment.CONNECTION_URI;
  private String currentContentFragmentTag = null;

  private BluetoothAdapter 	mBluetoothAdapter = null;
  private CorsaService 		mCorsaService = null;
  private StringBuffer 		mOutStringBuffer;
  private String 			mConnectedDeviceName = null;
  private String 			mConnectedDeviceAddress = null;

  // Key names received from the BluetoothChatService Handler
  public static final String DEVICE_NAME = "device_name";
  public static final String TOAST = "toast";
  

  // Intent request codes
  private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
  private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
  private static final int REQUEST_ENABLE_BT = 3;
  private static final int REQUEST_GET_CONTENT = 4;  
  
  // Message types sent from the CorsaService Handler
  public static final int MESSAGE_STATE_CHANGE = 1;
  public static final int MESSAGE_READ = 2;
  public static final int MESSAGE_WRITE = 3;
  public static final int MESSAGE_DEVICE_NAME = 4;
  public static final int MESSAGE_TOAST = 5;
  public static final int MESSAGE_DFU_BEGIN = 6;
  public static final int MESSAGE_DFU_PROGRESS = 7;
  public static final int MESSAGE_DFU_END = 8;
  public static final int MESSAGE_DFU_FAILED = 9;
  
  public static final String EXTRA_DEVICE_NAME = "device_name";  
  public static final String EXTRA_DEVICE_ADDRESS = "device_address";
  private BlockingQueue<String> mMessageQueue = new ArrayBlockingQueue<String>(64);

  private ActionsAdapter actionsAdapter;
  private NotificationManager mNotifyManager = null;
  private NotificationCompat.Builder mBuilder = null;

  public CorsaService getBindedService() {
	  return mCorsaService;
  }
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mSettingsChangedListener = new SettingsChangedListener();

    setContentView(R.layout.corsa);

    viewActionsContentView = (ActionsContentView) findViewById(R.id.actionsContentView);
    viewActionsContentView.setSwipingType(ActionsContentView.SWIPING_ALL);

	mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	mBuilder = new NotificationCompat.Builder(this);
	Intent notificationIntent = new Intent(getApplicationContext(), CorsaActivity.class);
    notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP );
    PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    mBuilder.setContentIntent(pendingIntent);
	
    
    final ListView viewActionsList = (ListView) findViewById(R.id.actions);
    actionsAdapter = new ActionsAdapter(this);
    actionsAdapter.SetVisibilityLevel(getResources().getInteger(R.integer.show_everytime));
    SwingRightInAnimationAdapter swingRightInAnimationAdapter = new SwingRightInAnimationAdapter(actionsAdapter);
    swingRightInAnimationAdapter.setAbsListView(viewActionsList);
    viewActionsList.setAdapter(swingRightInAnimationAdapter);
    //viewActionsList.setAdapter(actionsAdapter);
    viewActionsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> adapter, View v, int position, long flags) {
        final Uri uri = actionsAdapter.getItem(position);
        updateContent(uri);
        viewActionsContentView.showContent();
      }
    });

    if (savedInstanceState != null) {
      currentUri = Uri.parse(savedInstanceState.getString(STATE_URI));
      currentContentFragmentTag = savedInstanceState.getString(STATE_FRAGMENT_TAG);
    }

    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    // If the adapter is null, then Bluetooth is not supported
    if (mBluetoothAdapter == null) {
        Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
        finish();
        return;
    }

    
    updateContent(currentUri);
  }

  public String getConnectedDeviceName() {
	return mConnectedDeviceName;
}

public String getConnectedDeviceAddress() {
	return mConnectedDeviceAddress;
}

public void callStartDFU(String filename) {
	mCorsaService.startUpgradeFirmware(filename);
}

@Override
  public void onBackPressed() {
    final Fragment currentFragment = getSupportFragmentManager().findFragmentByTag(currentContentFragmentTag);
    if (currentFragment instanceof WebViewFragment) {
      final WebViewFragment webFragment = (WebViewFragment) currentFragment;
      if (webFragment.onBackPressed())
        return;
    }

    super.onBackPressed();
  }

  public void onActionsButtonClick(View view) {
    if (viewActionsContentView.isActionsShown())
      viewActionsContentView.showContent();
    else
      viewActionsContentView.showActions();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    outState.putString(STATE_URI, currentUri.toString());
    outState.putString(STATE_FRAGMENT_TAG, currentContentFragmentTag);

    super.onSaveInstanceState(outState);
  }

  private void setupChat() {
      Log.d(TAG, "setupChat()");

      /*
      // Initialize the array adapter for the conversation thread
      mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
      mConversationView = (ListView) findViewById(R.id.in);
      mConversationView.setAdapter(mConversationArrayAdapter);

      // Initialize the compose field with a listener for the return key
      mOutEditText = (EditText) findViewById(R.id.edit_text_out);
      mOutEditText.setOnEditorActionListener(mWriteListener);

      // Initialize the send button with a listener that for click events
      mSendButton = (Button) findViewById(R.id.button_send);
      mSendButton.setOnClickListener(new OnClickListener() {
          public void onClick(View v) {
              // Send a message using content of the edit text widget
              TextView view = (TextView) findViewById(R.id.edit_text_out);
              String message = view.getText().toString();
              sendMessage(message);
          }
      });
      */

      mCorsaService = new CorsaService(this, mHandler);
      mOutStringBuffer = new StringBuffer("");  
  }
  
  public void requestFileLocation() {
	  Intent target = FileUtils.createGetContentIntent();
		// Create the chooser Intent
		Intent intent = Intent.createChooser(target, "Vyberte súbor firmware");
		try {
			startActivityForResult(intent, REQUEST_GET_CONTENT);
		} catch (ActivityNotFoundException e) {
			// The reason for the existence of aFileChooser
		}        	  
  }
  
  public void disconnectBluetooth()
  {
	  mCorsaService.disconnect();
	  
  }
  
  @Override
  public void onStart() {
      super.onStart();
      Log.d(TAG, "OnStart");
      
      if (!mBluetoothAdapter.isEnabled()) {
          Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
          startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
      } else {
          if (mCorsaService == null) {
        	  setupChat();      	  
          }
      }
  }

  @Override
  public synchronized void onResume() {
      super.onResume();
      Log.d(TAG, "OnResume");

      // Performing this check in onResume() covers the case in which BT was
      // not enabled during onStart(), so we were paused to enable it...
      // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
      if (mCorsaService != null) {
          // Only if the state is STATE_NONE, do we know that we haven't started already
          if (mCorsaService.getBtState() == CorsaService.BT_STATE_NONE) {
            // Start the Bluetooth chat services
        	  mCorsaService.start();
          }
      }
  }  

  
  public void sendMessage(String message) {
      // Check that we're actually connected before trying anything
      if (mCorsaService.getBtState() != CorsaService.BT_STATE_CONNECTED) {
          Toast.makeText(this, R.string.info_disconnected, Toast.LENGTH_SHORT).show();
          return;
      }

      // Check that there's actually something to send
      if (message.length() > 0) {
          // Get the message bytes and tell the BluetoothChatService to write
          byte[] send = message.getBytes();
          mCorsaService.write(send);

          // Reset out string buffer to zero and clear the edit text field
          mOutStringBuffer.setLength(0);
      }
      
      //mMessageQueue.add(message);
  }  

  
  // The Handler that gets information back from the BluetoothChatService
  private final Handler mHandler = new HandlerExtension();  
  

  
public void onActivityResult(int requestCode, int resultCode, Intent data) {
      Log.d(TAG, "onActivityResult " + resultCode);
      Log.d(TAG, "onActivityResult request code: " + requestCode);
      switch (requestCode) {
		case REQUEST_GET_CONTENT:	
			// If the file selection was successful
			if (resultCode == RESULT_OK) {		
				if (data != null) {
					// Get the URI of the selected file
					final Uri uri = data.getData();
	
					try {
						// Create a file instance from the URI
						final File file = FileUtils.getFile(uri);
						final String filePath = file.getAbsolutePath();
						
						if (filePath.toLowerCase(Locale.US).endsWith(".hex"))
						{
							Fragment actualFragment = getSupportFragmentManager().findFragmentByTag(currentContentFragmentTag);
							if (FirmwareFragment.class.equals(actualFragment.getClass()))
							{
								((FirmwareFragment) actualFragment).SetFirmwareFilename(file);
							}
						}
						else
							Log.d(TAG, "Filetype must be a firmware image");
						//Toast.makeText(this, content, Toast.LENGTH_LONG).show();
					} catch (Exception e) {
						Log.e("FileSelectorTestActivity", "File select error", e);
					}
				}
			} 
		break;
      case REQUEST_ENABLE_BT:
          // When the request to enable Bluetooth returns
          if (resultCode == Activity.RESULT_OK) {
              // Bluetooth is now enabled, so set up a chat session
              setupChat();
          } else {
              // User did not enable Bluetooth or an error occurred
              Log.d(TAG, "BT not enabled");
              Toast.makeText(this, R.string.info_bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
              finish();
          }
      }
  }  
  
  
  public void connectRequest(String name, String address) {
		Toast.makeText(getApplicationContext(), "Connecting to " + name, Toast.LENGTH_SHORT).show();
        
//		Intent intent = new Intent();
//        intent.putExtra(EXTRA_DEVICE_NAME, name);
//        intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        mCorsaService.connect(device, false);
  }
  
  
  public void updateContent(Uri uri) {
    final Fragment fragment;
    final String tag;

    final FragmentManager fm = getSupportFragmentManager();
    final FragmentTransaction tr = fm.beginTransaction();

    int positionDiff = actionsAdapter.getItemPosition(uri) - actionsAdapter.getItemPosition(currentUri);
    Log.d(TAG, actionsAdapter.getItemPosition(uri) + " POS " +actionsAdapter.getItemPosition(currentUri));
    if (positionDiff > 0)
    	tr.setCustomAnimations(R.anim.slide_up_enter, R.anim.slide_up_exit);
    else if (positionDiff < 0)
    	tr.setCustomAnimations(R.anim.slide_down_enter, R.anim.slide_down_exit);

    if (!currentUri.equals(uri)) {
      final Fragment currentFragment = fm.findFragmentByTag(currentContentFragmentTag);
      if (currentFragment != null)
        tr.hide(currentFragment);
    }

    if (mCorsaService != null) {
        if (ConnectionFragment.CONNECTION_URI.equals(uri) && mCorsaService.getBtState() == CorsaService.BT_STATE_CONNECTED)
        	uri = DisconnectionFragment.DISCONNECTION_URI;
    }
    
    if (ConnectionFragment.CONNECTION_URI.equals(uri)) {
        tag = ConnectionFragment.TAG;
        final Fragment foundFragment = fm.findFragmentByTag(tag);
        if (foundFragment != null) {
          fragment = foundFragment;
        } else {
          fragment = new ConnectionFragment();
        }
      } else if (DisconnectionFragment.DISCONNECTION_URI.equals(uri)) {
      tag = DisconnectionFragment.TAG;
      final Fragment foundFragment = fm.findFragmentByTag(tag);
      if (foundFragment != null) {
        fragment = foundFragment;
      } else {
        fragment = new DisconnectionFragment();
      }
    } else if (FirmwareFragment.FIRMWARE_URI.equals(uri)) {
        tag = FirmwareFragment.TAG;
        final FirmwareFragment foundFragment = (FirmwareFragment) fm.findFragmentByTag(tag);
        if (foundFragment != null) {
          fragment = foundFragment;
        } else {
          fragment = new FirmwareFragment();
        }
      } else if (SandboxFragment.SETTINGS_URI.equals(uri)) {
	      tag = SandboxFragment.TAG;
	      final SandboxFragment foundFragment = (SandboxFragment) fm.findFragmentByTag(tag);
	      if (foundFragment != null) {
	        foundFragment.setOnSettingsChangedListener(mSettingsChangedListener);
	        fragment = foundFragment;
	      } else {
	        final SandboxFragment settingsFragment = new SandboxFragment();
	        settingsFragment.setOnSettingsChangedListener(mSettingsChangedListener);
	        fragment = settingsFragment;
	      }
    } else if (ModeFragment.MODE_URI.equals(uri)) {
		tag = ModeFragment.TAG;
		final ModeFragment foundFragment = (ModeFragment) fm.findFragmentByTag(tag);
		if (foundFragment != null) {
		  fragment = foundFragment;
		} else {
		  fragment = new ModeFragment();
		}
    } else if (uri != null) {
        tag = WebViewFragment.TAG;
	  final WebViewFragment webViewFragment;
	  final Fragment foundFragment = fm.findFragmentByTag(tag);
	  if (foundFragment != null) {
	    fragment = foundFragment;
	    webViewFragment = (WebViewFragment) fragment;
	  } else {
	    webViewFragment = new WebViewFragment();
	    fragment = webViewFragment;
	  }
	  webViewFragment.setUrl(uri.toString());
    } else {
      return;
    }

    if (fragment.isAdded()) {
      tr.show(fragment);
    } else {
      tr.replace(R.id.content, fragment, tag);
    }
    tr.commit();

    currentUri = uri;
    currentContentFragmentTag = tag;
  }

  
  private final class HandlerExtension extends Handler {
	@Override
      public void handleMessage(Message msg) {
		switch (msg.what) {
          case MESSAGE_STATE_CHANGE:
              Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
              switch (msg.arg1) {
              case CorsaService.BT_STATE_CONNECTED:
            	  actionsAdapter.SetVisibilityLevel(getResources().getInteger(R.integer.only_while_connected));
            	  updateContent(ModeFragment.MODE_URI);

            	  //setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                  //mConversationArrayAdapter.clear();
                  break;
              case CorsaService.BT_STATE_CONNECTING:
                  //setStatus(R.string.title_connecting);
                  break;
              case CorsaService.BT_STATE_NONE:
            	  break;
              case CorsaService.BT_STATE_DISCONNECTED:
            	  actionsAdapter.SetVisibilityLevel(getResources().getInteger(R.integer.show_everytime));
            	  updateContent(ConnectionFragment.CONNECTION_URI);
            	  break;
              }
              break;
          case MESSAGE_WRITE:
        	  /*
              byte[] writeBuf = (byte[]) msg.obj;
              // construct a string from the buffer
              String writeMessage = new String(writeBuf);
              mConversationArrayAdapter.add("Me:  " + writeMessage);
              */
              break;
          case MESSAGE_READ:
              byte[] readBuf = (byte[]) msg.obj;
              // construct a string from the valid bytes in the buffer
              String readMessage = new String(readBuf, 0, msg.arg1);

              break;
          case MESSAGE_DEVICE_NAME:
              // save the connected device's name
              mConnectedDeviceName = msg.getData().getString(EXTRA_DEVICE_NAME);
              mConnectedDeviceAddress = msg.getData().getString(EXTRA_DEVICE_ADDRESS);
              Toast.makeText(getApplicationContext(), "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
              break;
          case MESSAGE_TOAST:
              Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                             Toast.LENGTH_SHORT).show();
              break;
          case MESSAGE_DFU_BEGIN:
        		mBuilder.setContentTitle("Firmware update")
        	    	    .setContentText("Waiting for start")
        	    	    .setSmallIcon(R.drawable.ic_launcher)
        				.setProgress(100, 0, false);

        		mNotifyManager.notify(0, mBuilder.build()); 	
        	  break;
          case MESSAGE_DFU_PROGRESS:
	      		mBuilder.setContentTitle("Firmware update")
		    	    .setContentText("Update in progress")
		    	    .setSmallIcon(R.drawable.ic_launcher)
					.setProgress(msg.arg1, msg.arg2, false);
	      		Log.d("DFU", "Progress " + msg.arg1 + " / " + msg.arg2);
	      		mNotifyManager.notify(0, mBuilder.build()); 
	      		
	      	    final Fragment currentFragment = getSupportFragmentManager().findFragmentByTag(currentContentFragmentTag);
	      	    if (currentFragment instanceof FirmwareFragment) {
	      	    	((FirmwareFragment)currentFragment).setUpgradeProgress(100 * msg.arg2 / msg.arg1);
	      	    }
	      		
        	  break;
          case MESSAGE_DFU_END:
	      		mBuilder.setContentTitle("Firmware update done")
	      				.setContentText("")
	      				.setSmallIcon(R.drawable.ic_launcher)
	      				.setProgress(0, 0, false);
	      		mNotifyManager.notify(0, mBuilder.build()); 
	      		disconnectBluetooth();
        	  break;
          case MESSAGE_DFU_FAILED:
	      		mBuilder.setContentTitle("Firmware update failed")
	      				.setContentText("Device timeouted, please try again")
	      				.setSmallIcon(R.drawable.ic_launcher)
	      				.setProgress(0, 0, false);
	      		mNotifyManager.notify(0, mBuilder.build());
        	  break;
          }
      }
}

private class SettingsChangedListener implements SandboxFragment.OnSettingsChangedListener {
    private final float mDensity = getResources().getDisplayMetrics().density;
    private final int mAdditionaSpacingWidth = (int) (100 * mDensity);

    @Override
    public void onSettingChanged(int prefId, int value) {
      switch (prefId) {
      case SandboxFragment.PREF_SPACING_TYPE:
        final int currentType = viewActionsContentView.getSpacingType();
        if (currentType == value)
          return;

        final int spacingWidth = viewActionsContentView.getSpacingWidth();
        if (value == ActionsContentView.SPACING_ACTIONS_WIDTH) {
          viewActionsContentView.setSpacingWidth(spacingWidth + mAdditionaSpacingWidth);
        } else if (value == ActionsContentView.SPACING_RIGHT_OFFSET) {
          viewActionsContentView.setSpacingWidth(spacingWidth - mAdditionaSpacingWidth);
        }
        viewActionsContentView.setSpacingType(value);
        return;
      case SandboxFragment.PREF_SPACING_WIDTH:
        final int width;
        if (viewActionsContentView.getSpacingType() == ActionsContentView.SPACING_ACTIONS_WIDTH)
          width = (int) (value * mDensity) + mAdditionaSpacingWidth;
        else
          width = (int) (value * mDensity);
        viewActionsContentView.setSpacingWidth(width);
        return;
      case SandboxFragment.PREF_SPACING_ACTIONS_WIDTH:
        viewActionsContentView.setActionsSpacingWidth((int) (value * mDensity));
        return;
      case SandboxFragment.PREF_SHOW_SHADOW:
        viewActionsContentView.setShadowVisible(value == 1);
        return;
      case SandboxFragment.PREF_FADE_TYPE:
        viewActionsContentView.setFadeType(value);
        return;
      case SandboxFragment.PREF_FADE_MAX_VALUE:
        viewActionsContentView.setFadeValue(value);
        return;
      case SandboxFragment.PREF_SWIPING_TYPE:
        viewActionsContentView.setSwipingType(value);
        return;
      case SandboxFragment.PREF_SWIPING_EDGE_WIDTH:
        viewActionsContentView.setSwipingEdgeWidth(value);
        return;
      case SandboxFragment.PREF_FLING_DURATION:
        viewActionsContentView.setFlingDuration(value);
        return;
      default:
        break;
      }
    }
  }
}
