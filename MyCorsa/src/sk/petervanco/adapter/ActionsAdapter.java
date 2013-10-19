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
package sk.petervanco.adapter;

import sk.petervanco.mycorsa.R;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class ActionsAdapter extends BaseAdapter {

  private static final int VIEW_TYPE_CATEGORY = 0;
  private static final int VIEW_TYPE_SETTINGS = 1;
  private static final int VIEW_TYPE_SITES = 2;
  private static final int VIEW_TYPES_COUNT = 3;

  private final LayoutInflater mInflater;

  private String[] mTitles;
  private String[] mUrls;
  private int[]    mIcons;
  private int	   mVisibilityLevel = 0;
  private Resources mResources = null;

  private void LoadArrays() {
	  
	  	int[] Visiblity;
	    Visiblity = mResources.getIntArray(R.array.actions_disconnected_visibility);
	    String[] mTempTitles = mResources.getStringArray(R.array.actions_names);
	    String[] mTempUrls = mResources.getStringArray(R.array.actions_links);
	    
	    final TypedArray iconsArray = mResources.obtainTypedArray(R.array.actions_icons);
	    final int count = iconsArray.length();
	    int[] mTempIcons = new int[count];
	    for (int i = 0; i < count; ++i ) {
	      mTempIcons[i] = iconsArray.getResourceId(i, 0);
	    }
	    iconsArray.recycle();
	    
	    int VisibilitySatisfiedElements = 0;
	    for (int i = 0; i < Visiblity.length; i++) {
	    	if (Visiblity[i] <= mVisibilityLevel)
	    		VisibilitySatisfiedElements++;
	    }
	    
	    Log.d("ADAPTER", "Satisfied = " + VisibilitySatisfiedElements);
	    
	    mTitles = new String[VisibilitySatisfiedElements];
	    mUrls = new String[VisibilitySatisfiedElements];
	    mIcons = new int[VisibilitySatisfiedElements];
	    
	    for (int tempi = 0, i = 0; tempi < count; tempi++) {
	    	if (Visiblity[tempi] <= mVisibilityLevel)
	    	{
	    		mTitles[i] = mTempTitles[tempi];
	    		mUrls[i] = mTempUrls[tempi];
	  		  	mIcons[i] = mTempIcons[tempi];
	  		  	i++;
	    	}
	    }
	    
	  
  }
  
  public ActionsAdapter(Context context) {
    mInflater = LayoutInflater.from(context);
    mResources = context.getResources();
    LoadArrays();
  }

  public void SetVisibilityLevel(int Level) {
	  mVisibilityLevel = Level;
	  LoadArrays();
	  this.notifyDataSetChanged();
  }
  
  @Override
  public int getCount() {
    return mUrls.length;
  }

  @Override
  public Uri getItem(int position) {
    return Uri.parse(mUrls[position]);
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @SuppressLint("DefaultLocale")
  @Override	
  public View getView(int position, View convertView, ViewGroup parent) {
    final int type = getItemViewType(position);

    final ViewHolder holder;
    if (convertView == null) {
      if (type == VIEW_TYPE_CATEGORY)
        convertView = mInflater.inflate(R.layout.category_list_item, parent, false);
      else
        convertView = mInflater.inflate(R.layout.action_list_item, parent, false);

      holder = new ViewHolder();
      holder.text = (TextView) convertView.findViewById(android.R.id.text1);
      convertView.setTag(holder);
    } else {
      holder = (ViewHolder) convertView.getTag();
    }

    if (type != VIEW_TYPE_CATEGORY) {
    	
      final Drawable icon = convertView.getContext().getResources().getDrawable(mIcons[position]);
      icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
      //icon.setAlpha(0);
      holder.text.setCompoundDrawables(icon, null, null, null);
      holder.text.setText(mTitles[position]);
    } else {
      holder.text.setText(mTitles[position].toUpperCase());
    }

    return convertView;
  }

  @Override
  public int getViewTypeCount() {
    return VIEW_TYPES_COUNT;
  }

  @Override
  public int getItemViewType(int position) {
    final Uri uri = Uri.parse(mUrls[position]);
    final String scheme = uri.getScheme();
    if ("category".equals(scheme))
      return VIEW_TYPE_CATEGORY;
    else if ("settings".equals(scheme))
      return VIEW_TYPE_SETTINGS;
    return VIEW_TYPE_SITES;
  }

  @Override
  public boolean isEnabled(int position) {
    return getItemViewType(position) != VIEW_TYPE_CATEGORY;
  }

  private static class ViewHolder {
    TextView text;
  }

}
