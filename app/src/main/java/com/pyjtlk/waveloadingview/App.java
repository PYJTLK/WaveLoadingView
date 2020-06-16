package com.pyjtlk.waveloadingview;

import android.app.Application;

import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

public class App extends Application {
    private static RefWatcher refWatcher;

    @Override
    public void onCreate() {
        super.onCreate();

        LeakCanary.isInAnalyzerProcess(this);

        refWatcher = LeakCanary.install(this);
    }

    public static RefWatcher getWatcher(){
        return refWatcher;
    }
}
