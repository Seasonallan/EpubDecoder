package com.season.bookreader.digests;

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.season.bookreader.R;
import com.season.bookreader.model.BookDigestColorItem;

import java.util.ArrayList;


public class BookDigestColorItemAdapter extends BaseAdapter {
	
	private LayoutInflater inflater;
	private ArrayList<BookDigestColorItem> bookDigestColorItems;
	private int oldSelect = 0;
	private Activity mContext;
	private int mHight;
	public BookDigestColorItemAdapter(Activity context, ArrayList<BookDigestColorItem> bookDigestColorItems){
		super();
		this.mContext = context;
		inflater = LayoutInflater.from(context);
		this.bookDigestColorItems = bookDigestColorItems;
		setImaHight();
		
        
	}

	private void setImaHight() {
		WindowManager windowManager = mContext.getWindowManager();
        Display display = windowManager.getDefaultDisplay();
        int screenWidth = display.getWidth();
        int margin =  BookDigestsRemarksDialog.LAYOUT_MARGIN;
        int hspac = BookDigestsRemarksDialog.HSPAC;
        mHight = (screenWidth - 2 * margin - 4 * hspac) / 5;
	}

	@Override
	public int getCount() {
		if(bookDigestColorItems != null){
			return bookDigestColorItems.size();
		}
		return 0;
	}

	@Override
	public BookDigestColorItem getItem(int position) {
		if(position < getCount()){
			return bookDigestColorItems.get(position);
		}
		return null;
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		ViewHolder viewHolder;
		if(convertView == null){
			convertView = newView();
			viewHolder = new ViewHolder();
			viewHolder.contentIV = (ImageView) convertView.findViewById(R.id.content_iv);
			viewHolder.selectTV = (ImageView) convertView.findViewById(R.id.select_iv);
			convertView.setTag(viewHolder);
		}else{
			viewHolder = (ViewHolder) convertView.getTag();
		}
		
		BookDigestColorItem item = getItem(position);
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, mHight);
		viewHolder.contentIV.setLayoutParams(lp);
		viewHolder.contentIV.setBackgroundDrawable(new ColorDrawable(item.id));
		if(item.isSelected){
			oldSelect = position;
			viewHolder.selectTV.setVisibility(View.VISIBLE);
		}else{
			viewHolder.selectTV.setVisibility(View.GONE);
		}
		return convertView;
	}
	
	private View newView(){
		return inflater.inflate(R.layout.bookdigest_color_item, null);
	}
	public void setSeleted(int position){
		if(position != oldSelect){
			getItem(oldSelect).isSelected = false;
			getItem(position).isSelected = true;
			oldSelect = position;
			notifyDataSetChanged();
		}
	}
	private class ViewHolder {
		
		public ImageView contentIV;
		public ImageView selectTV;
		
	}

}

