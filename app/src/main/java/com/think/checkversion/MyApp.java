package com.think.checkversion;

import android.app.Application;

import org.xutils.x;

/**
 * Created by think on 2017/12/13.
 */

public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        //xutils的初始化.
        //设置是否输入日志.
        x.Ext.setDebug(true);
        //这一步之后, 我们就可以在任何地方使用x.app()来获取Application的实例了.
        x.Ext.init(this);
    }
}
