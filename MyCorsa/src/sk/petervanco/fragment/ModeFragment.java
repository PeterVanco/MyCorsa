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

import sk.petervanco.mycorsa.R;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ModeFragment extends Fragment {
	
  public static final String 	TAG = ModeFragment.class.getSimpleName();

  private static final String 	MODE_SCHEME = "settings";
  private static final String 	MODE_AUTHORITY = "mode";
  public static final Uri 		MODE_URI = new Uri.Builder()
													  .scheme(MODE_SCHEME)
													  .authority(MODE_AUTHORITY)
													  .build();

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {

    final View v = inflater.inflate(R.layout.mode, container, false);

    /*
    v.findViewById(R.id.play).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        final Activity a = getActivity();
        if (a instanceof CorsaActivity) {
          final CorsaActivity examplesActivity = (CorsaActivity) a;
          examplesActivity.updateContent(SandboxFragment.SETTINGS_URI);
        }
      }
    });
    */

    return v;
  }
}