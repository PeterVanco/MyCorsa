package sk.petervanco.fragment;

import java.io.File;

import sk.petervanco.mycorsa.CorsaActivity;
import sk.petervanco.mycorsa.R;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ProgressBar;
import android.widget.TextView;

public class FirmwareFragment extends Fragment implements OnClickListener {

	private CorsaActivity mActivity = null;

	public static final String TAG = FirmwareFragment.class.getSimpleName();

	private static final String FIRMWARE_SCHEME = "settings";
	private static final String FIRMWARE_AUTHORITY = "firmware";
	public  static final Uri 	FIRMWARE_URI = new Uri.Builder()
											  .scheme(FIRMWARE_SCHEME)
											  .authority(FIRMWARE_AUTHORITY)
											  .build();
	
	private static TextView mInfoFilename = null;
	private String mDfuFile = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		mActivity = (CorsaActivity) getActivity();
		  
	    final View v = inflater.inflate(R.layout.firmware, container, false);
	
	    v.findViewById(R.id.btn_choose_file).setOnClickListener(this);
	    v.findViewById(R.id.btn_upload_firmware).setOnClickListener(this);

	    mInfoFilename = (TextView) v.findViewById(R.id.file_name);

	    
	    return v;
	}

	public static void ExpandView(final View v) {
	    v.measure(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
	    final int targtetHeight = v.getMeasuredHeight();

	    v.getLayoutParams().height = 0;
	    v.setVisibility(View.VISIBLE);
	    Animation a = new Animation()
	    {
	        @Override
	        protected void applyTransformation(float interpolatedTime, Transformation t) {
	            v.getLayoutParams().height = interpolatedTime == 1
	                    ? LayoutParams.WRAP_CONTENT
	                    : (int)(targtetHeight * interpolatedTime);
	            v.requestLayout();
	        }

	        @Override
	        public boolean willChangeBounds() {
	            return true;
	        }
	    };

	    // 1dp/ms
	    a.setDuration(1000);
	    v.startAnimation(a);
	}

	public static void CollapseView(final View v) {
	    final int initialHeight = v.getMeasuredHeight();

	    Animation a = new Animation()
	    {
	        @Override
	        protected void applyTransformation(float interpolatedTime, Transformation t) {
	            if(interpolatedTime == 1){
	                v.setVisibility(View.GONE);
	            }else{
	                v.getLayoutParams().height = initialHeight - (int)(initialHeight * interpolatedTime);
	                v.requestLayout();
	            }
	        }

	        @Override
	        public boolean willChangeBounds() {
	            return true;
	        }
	    };

	    // 1dp/ms
	    a.setDuration(1000);
	    v.startAnimation(a);
	}
	
	public void SetFirmwareValidity(Boolean valid) {
		if (valid) {
			ExpandView(getView().findViewById(R.id.firmware_warning));
			ExpandView(getView().findViewById(R.id.btn_upload_firmware));
		}
	}
	
	public void SetFirmwareFilename(File file) {
		
		mDfuFile = file.getAbsolutePath();
		
		String shortName = file.getName();
		if (shortName.length() > 20)
			shortName = shortName.substring(0, 20) + " ...";
		mInfoFilename.setText(shortName);
		SetFirmwareValidity(true);
	}
	
	public void setUpgradeProgress(int progress) {
		
		TextView perc_text = (TextView) getView().findViewById(R.id.upgrade_percentage);
		perc_text.setText(String.format(getResources().getString(R.string.percentage_done), progress));
		ProgressBar perc_bar = (ProgressBar) getView().findViewById(R.id.upgrade_percentage_bar);
		perc_bar.setProgress(progress);
	}
	
	@Override
	public void onClick(View v) {

		switch (v.getId()) {
		
        case R.id.btn_upload_firmware:
			ExpandView(getView().findViewById(R.id.upgrade_progress));
			setUpgradeProgress(0);
			mActivity.getBindedService().startUpgradeFirmware(mDfuFile);
      	  	break;

        case R.id.btn_choose_file:
        	mActivity.requestFileLocation();
        	break;

		default:
			break;
		}
		
	}

}
