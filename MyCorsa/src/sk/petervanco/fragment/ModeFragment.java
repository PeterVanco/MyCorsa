
package sk.petervanco.fragment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import sk.petervanco.color.ColorPicker;
import sk.petervanco.color.ColorPicker.OnColorChangedListener;
import sk.petervanco.color.SVBar;
import sk.petervanco.color.ValueBar;
import sk.petervanco.color.ValueBar.OnValueChangedListener;
import sk.petervanco.mycorsa.CorsaActivity;
import sk.petervanco.mycorsa.R;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Toast;

public class ModeFragment extends Fragment implements OnClickListener {
	
  public static final String 	TAG = ModeFragment.class.getSimpleName();

  private static final String 	MODE_SCHEME = "settings";
  private static final String 	MODE_AUTHORITY = "mode";
  public static final  Uri 		MODE_URI = new Uri.Builder()
													  .scheme(MODE_SCHEME)
													  .authority(MODE_AUTHORITY)
													  .build();
  private CorsaActivity activity = null;
  private ValueBar mAnimationSpeed = null;
  ColorPicker mSolidColor;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

	activity = (CorsaActivity)getActivity();
	 
    final View v = inflater.inflate(R.layout.mode, container, false);

    v.findViewById(R.id.btn_mode_solid).setOnClickListener(this);
    v.findViewById(R.id.btn_mode_fade).setOnClickListener(this);
    v.findViewById(R.id.btn_mode_rainbow).setOnClickListener(this);
    v.findViewById(R.id.btn_upload_firmware).setOnClickListener(this);

    RadioGroup door_logic = (RadioGroup)(v.findViewById(R.id.radiogroup_door_logic));
    door_logic.setOnCheckedChangeListener(new OnCheckedChangeListener() {
		
		@Override
		public void onCheckedChanged(RadioGroup group, int radio) {

			switch (radio) {
			case R.id.radio_disable_door_logic:
				Toast.makeText(activity, "Disabling door logic", Toast.LENGTH_SHORT).show();
				activity.sendMessage("disable\n");
				break;

			case R.id.radio_enable_door_logic:
				Toast.makeText(activity, "Enabling door logic", Toast.LENGTH_SHORT).show();
				activity.sendMessage("enable\n");
				break;
			}
			
		}
	});

    RadioGroup door_trigger = (RadioGroup)(v.findViewById(R.id.radiogroup_door_trigger));
    door_trigger.setOnCheckedChangeListener(new OnCheckedChangeListener() {
		
		@Override
		public void onCheckedChanged(RadioGroup group, int radio) {

			switch (radio) {
			case R.id.radio_2_blinks:
				Toast.makeText(activity, "2 Blinks", Toast.LENGTH_SHORT).show();
				activity.sendMessage("blink2\n");
				break;

			case R.id.radio_3_blinks:
				Toast.makeText(activity, "3 Blinks", Toast.LENGTH_SHORT).show();
				activity.sendMessage("blink3\n");
				break;
			}
			
		}
	});
    
    mAnimationSpeed = (ValueBar) v.findViewById(R.id.animSpeed);
    mAnimationSpeed.setColor(Color.WHITE);
    mAnimationSpeed.setOnValueChangedListener(new OnValueChangedListener() {
		
		@Override
		public void onValueChanged(int color) {
			
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			try {
				bos.write("speed".getBytes());
			} catch (IOException e) {}
  			bos.write('0' + Color.red(color) / (0xff / 9));
  			bos.write('\n');
  			Log.d(TAG, bos.toString());
  			activity.sendMessage(bos.toString());			
			
		}
	});

    
    mSolidColor = (ColorPicker) v.findViewById(R.id.solid_colorpicker);
    mSolidColor.addSVBar((SVBar) v.findViewById(R.id.solid_svbar));
    mSolidColor.setOnColorChangedListener(new OnColorChangedListener() {
		
		@Override
		public void onColorChanged(int color) {
			
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			bos.write((int)'0' + Color.red(color) / (0xff / 9));
			bos.write((int)'0' + Color.green(color) / (0xff / 9));
			bos.write((int)'0' + Color.blue(color) / (0xff / 9));
			bos.write('\n');
			Log.d(TAG, bos.toString());
			activity.sendMessage(bos.toString());
			
		}
	});

    return v;
  } 
  
  @Override
  public void onClick(View v) {

	  ByteArrayOutputStream bos = new ByteArrayOutputStream();
	  int actualColor = mSolidColor.getColor();
	  
	  switch(v.getId()) {
          case R.id.btn_mode_rainbow:
        	  Log.d(TAG, "Connecting to BCU");
        	  activity.sendMessage("bow\n");
          break;

          case R.id.btn_mode_solid:
        	  
	  			bos.write((int)'0' + Color.red(actualColor) / (0xff / 9));
	  			bos.write((int)'0' + Color.green(actualColor) / (0xff / 9));
	  			bos.write((int)'0' + Color.blue(actualColor) / (0xff / 9));
	  			bos.write('\n');
	  			Log.d(TAG, bos.toString());
	  			activity.sendMessage(bos.toString());
	  		break;

          case R.id.btn_mode_fade:

				try {
					bos.write("fade".getBytes());
				} catch (IOException e) {}
	  			bos.write((int)'0' + Color.red(actualColor) / (0xff / 9));
	  			bos.write((int)'0' + Color.green(actualColor) / (0xff / 9));
	  			bos.write((int)'0' + Color.blue(actualColor) / (0xff / 9));
	  			bos.write('\n');
	  			Log.d(TAG, bos.toString());
	  			activity.sendMessage(bos.toString());
	  		break;

          case R.id.btn_upload_firmware:
        	  activity.requestFileLocation();
        	  break;
	  }

  }  


  public void onRadioButtonClicked(View view) {
	    // Is the button now checked?
	    boolean checked = ((RadioButton) view).isChecked();
	    
	    Toast.makeText(getActivity(), "radio", Toast.LENGTH_SHORT).show();
	    
	    // Check which radio button was clicked
	    switch(view.getId()) {
	        case R.id.radio_disable_door_logic:
	            if (checked)
	            	
	            break;
	        case R.id.radio_enable_door_logic:
	            if (checked)

	            break;
	    }
	}  
  
}
