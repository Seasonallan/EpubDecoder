package com.season.bookreader.tagspan;

import com.season.lib.text.style.ClickActionSpan;
import com.season.bookreader.media.IMedia;

public interface IMediaSpan extends ClickActionSpan,ReaderMediaPlayer.PlayerListener,IMedia {
	public boolean isPlay();
	
	public long computePositionByLocal(int x,int y);
}
