package com.season.bookreader;

import android.app.Application;
import android.content.Context;

import com.lectek.lereader.core.util.ContextUtil;

/**
 * Disc:
 * User: SeasonAllan(451360508@qq.com)
 * Time: 2018-09-11 11:35
 */
public class BaseApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ContextUtil.init(this);
    }
}
