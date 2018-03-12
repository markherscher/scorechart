package com.herscher.scorechart.core;

import android.app.Application;

import io.realm.Realm;

public class ScoreApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Realm.init(getApplicationContext());
    }
}
