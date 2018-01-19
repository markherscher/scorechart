package com.herscher.scotchbridge.core;

import android.app.Application;

import com.herscher.scotchbridge.model.Game;
import com.herscher.scotchbridge.model.Player;

import java.util.UUID;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class ScoreApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Realm.init(getApplicationContext());
        RealmConfiguration.Builder builder = new RealmConfiguration.Builder();
        Realm.setDefaultConfiguration(builder.build());
    }
}
