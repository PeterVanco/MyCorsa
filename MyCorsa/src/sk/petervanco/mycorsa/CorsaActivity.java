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

import shared.ui.actionscontentview.ActionsContentView;
import sk.petervanco.adapter.ActionsAdapter;
import sk.petervanco.fragment.ConnectionFragment;
import sk.petervanco.fragment.ModeFragment;
import sk.petervanco.fragment.SandboxFragment;
import sk.petervanco.fragment.WebViewFragment;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

public class CorsaActivity extends FragmentActivity {

  private static final String TAG = "CorsaActivity";
  private static final String STATE_URI = "state:uri";
  private static final String STATE_FRAGMENT_TAG = "state:fragment_tag";

  private SettingsChangedListener mSettingsChangedListener;

  private ActionsContentView viewActionsContentView;

  private Uri currentUri = ConnectionFragment.ABOUT_URI;
  private String currentContentFragmentTag = null;

  private BluetoothAdapter 	mBluetoothAdapter = null;
  private CorsaService 		mCorsaService = null;
  private StringBuffer 		mOutStringBuffer;

  // Key names received from the BluetoothChatService Handler
  public static final String DEVICE_NAME = "device_name";
  public static final String TOAST = "toast";
  

  // Intent request codes
  private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
  private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
  private static final int REQUEST_ENABLE_BT = 3;
  
  // Message types sent from the CorsaService Handler
  public static final int MESSAGE_STATE_CHANGE = 1;
  public static final int MESSAGE_READ = 2;
  public static final int MESSAGE_WRITE = 3;
  public static final int MESSAGE_DEVICE_NAME = 4;
  public static final int MESSAGE_TOAST = 5;  
  
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mSettingsChangedListener = new SettingsChangedListener();

    setContentView(R.layout.example);

    viewActionsContentView = (ActionsContentView) findViewById(R.id.actionsContentView);
    viewActionsContentView.setSwipingType(ActionsContentView.SWIPING_EDGE);

    final ListView viewActionsList = (ListView) findViewById(R.id.actions);
    final ActionsAdapter actionsAdapter = new ActionsAdapter(this);
    viewActionsList.setAdapter(actionsAdapter);
    viewActionsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> adapter, View v, int position,
          long flags) {
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

  
  private void sendMessage(String message) {
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
  }  
  
  // The Handler that gets information back from the BluetoothChatService
  private final Handler mHandler = new Handler() {
      @Override
      public void handleMessage(Message msg) {
          switch (msg.what) {
          case MESSAGE_STATE_CHANGE:
              Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
              /*
              switch (msg.arg1) {
              case BluetoothChatService.STATE_CONNECTED:
                  setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                  mConversationArrayAdapter.clear();
                  break;
              case BluetoothChatService.STATE_CONNECTING:
                  setStatus(R.string.title_connecting);
                  break;
              case BluetoothChatService.STATE_LISTEN:
              case BluetoothChatService.STATE_NONE:
                  setStatus(R.string.title_not_connected);
                  break;
              }
              */
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
        	  /*
              byte[] readBuf = (byte[]) msg.obj;
              // construct a string from the valid bytes in the buffer
              String readMessage = new String(readBuf, 0, msg.arg1);
              mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
              */
              break;
          case MESSAGE_DEVICE_NAME:
        	  /*
              // save the connected device's name
              mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
              Toast.makeText(getApplicationContext(), "Connected to "
                             + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                             */
              break;
          case MESSAGE_TOAST:
              Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                             Toast.LENGTH_SHORT).show();
              break;
          }
      }
  };  
  
  private void connectDevice(Intent data, boolean secure) {
      Log.d(TAG, "Connect intent");
	  /*
      // Get the device MAC address
      String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
      // Get the BluetoothDevice object
      BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
      // Attempt to connect to the device
      */
      //CorsaService.connect(device, secure);
  }
  
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
      Log.d(TAG, "onActivityResult " + resultCode);
      switch (requestCode) {
      case REQUEST_CONNECT_DEVICE_SECURE:
          // When DeviceListActivity returns with a device to connect
          if (resultCode == Activity.RESULT_OK) {
              connectDevice(data, true);
          }
          break;
      case REQUEST_CONNECT_DEVICE_INSECURE:
          // When DeviceListActivity returns with a device to connect
          if (resultCode == Activity.RESULT_OK) {
              connectDevice(data, false);
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
  }
  
  
  public void updateContent(Uri uri) {
    final Fragment fragment;
    final String tag;

    final FragmentManager fm = getSupportFragmentManager();
    final FragmentTransaction tr = fm.beginTransaction();

    if (!currentUri.equals(uri)) {
      final Fragment currentFragment = fm.findFragmentByTag(currentContentFragmentTag);
      if (currentFragment != null)
        tr.hide(currentFragment);
    }

    if (ConnectionFragment.ABOUT_URI.equals(uri)) {
      tag = ConnectionFragment.TAG;
      final Fragment foundFragment = fm.findFragmentByTag(tag);
      if (foundFragment != null) {
        fragment = foundFragment;
        Toast.makeText(getApplicationContext(), "Ahoj", Toast.LENGTH_SHORT).show();
      } else {
        Toast.makeText(getApplicationContext(), "Cau", Toast.LENGTH_SHORT).show();
        fragment = new ConnectionFragment();
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
