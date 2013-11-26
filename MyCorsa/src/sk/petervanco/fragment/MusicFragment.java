package sk.petervanco.fragment;

import java.io.File;
import java.io.IOException;

import sk.petervanco.mycorsa.CorsaActivity;
import sk.petervanco.mycorsa.R;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;


public class MusicFragment extends Fragment implements OnClickListener {

	private CorsaActivity mActivity = null;

	private TextView mInfoMusicFilename;

	private TextView mInfoControlFilename;

	  public static final String TAG = MusicFragment.class.getSimpleName();
		MediaPlayer mp = new MediaPlayer();
	
	private static final String MUSIC_SCHEME = "settings";
	  private static final String MUSIC_AUTHORITY = "music";
	  public static final Uri MUSIC_URI = new Uri.Builder()
												  .scheme(MUSIC_SCHEME)
												  .authority(MUSIC_AUTHORITY)
												  .build();
	
	  
	  @Override
	  public View onCreateView(LayoutInflater inflater, ViewGroup container,
	      Bundle savedInstanceState) {

		  mActivity = (CorsaActivity) getActivity();
		  
	    final View v = inflater.inflate(R.layout.music, container, false);


	    mInfoMusicFilename = (TextView) v.findViewById(R.id.text_music_file);
	    mInfoControlFilename = (TextView) v.findViewById(R.id.text_musiccontrol_file);
	    
	    v.findViewById(R.id.btn_choose_music).setOnClickListener(this);
	    v.findViewById(R.id.choose_control_file).setOnClickListener(this);
	    v.findViewById(R.id.play_pause).setOnClickListener(this);
	    
//
//	    TextView name = (TextView) v.findViewById(R.id.disconnection_bt_name);
//	    name.setText(mActivity.getConnectedDeviceName());
//	    TextView address = (TextView) v.findViewById(R.id.disconnection_bt_address);
//	    address.setText(mActivity.getConnectedDeviceAddress());
	    
  	    return v;
	  }

	  	  
	  private String mMusicFile;
	  private String mControlFile;
	  
	public void SetMusicFilename(File file) {
		
		mMusicFile = file.getAbsolutePath();
		
		String shortName = file.getName();
		if (shortName.length() > 20)
			shortName = shortName.substring(0, 20) + " ...";
		mInfoMusicFilename.setText(shortName);
	}		
  
	public void SetControlFilename(File file) {
		
		mControlFile = file.getAbsolutePath();
		
		String shortName = file.getName();
		if (shortName.length() > 20)
			shortName = shortName.substring(0, 20) + " ...";
		mInfoControlFilename.setText(shortName);
		
		mActivity.getBindedService().prepareMCS(mControlFile);
		
	}		
  
	
	
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		
		case R.id.btn_choose_music:
			mActivity.requestFileLocation(CorsaActivity.REQUEST_GET_FILE_MUSIC);
			break;

		case R.id.choose_control_file:
			mActivity.requestFileLocation(CorsaActivity.REQUEST_GET_FILE_MUSIC_CONTROL);
			break;
			
		case R.id.play_pause:
			try {
				
				if (mp.isPlaying()) {
					mp.stop();
				}
				else {
					mActivity.getBindedService().startMCS();
					
					mp.reset();
					mp.setDataSource(mMusicFile);
					mp.prepare();
					mp.start();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;

		default:
			break;
		}
		
	}

}
