package com.season.bookreader.media;


public interface IMedia {
	public String getVoiceSrc();

	public long getStartPosition();
	
	public boolean contains(long position);
}
