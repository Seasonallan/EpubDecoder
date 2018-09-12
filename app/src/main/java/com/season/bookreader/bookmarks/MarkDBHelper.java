package com.season.bookreader.bookmarks;

import com.season.bookreader.dbase.BaseDBHelper;
import com.season.bookreader.dbase.DBConfig;

/**
 * 全局 数据库
 * @author Administrator
 *
 */
public class MarkDBHelper extends BaseDBHelper {
 
	public MarkDBHelper() {
		super(DBConfig.DB_NAME, DBConfig.DB_VERSION);
	} 

	private static MarkDBHelper sDbHelper;
	public static MarkDBHelper getDBHelper(){
		if (sDbHelper == null) {
			sDbHelper = new MarkDBHelper();
		}
		return sDbHelper;
	}
	
	
	/**
	 * 获取需要创建在该库中的实体
	 * @return
	 */
    public Class<?>[] getDaoLists(){
    	return new Class<?>[]{BookMark.class};
    }
    
}
