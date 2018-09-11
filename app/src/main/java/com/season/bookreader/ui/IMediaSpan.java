package com.season.bookreader.ui;

import com.lectek.lereader.core.text.style.ClickActionSpan;

public interface IMediaSpan extends ClickActionSpan,ReaderMediaPlayer.PlayerListener,IMedia{
	public boolean isPlay();
	
	public long computePositionByLocal(int x,int y);
}
