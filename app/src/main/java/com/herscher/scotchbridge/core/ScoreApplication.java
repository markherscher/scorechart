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

        // TODO; some seed data
        Realm realm = Realm.getDefaultInstance();
        Game game = realm.where(Game.class).equalTo(Game.ID, "foo").findFirst();

        if (game == null) {
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm r) {
                    Game g = new Game();
                    g.setId("foo");
                    g.setName("Magic");

                    Player p1 = new Player();
                    p1.setId(UUID.randomUUID().toString());
                    p1.setName("Player 1");
                    p1.setGameId("foo");
                    p1.setOrder(0);

                    Player p2 = new Player();
                    p2.setId(UUID.randomUUID().toString());
                    p2.setName("Player 2");
                    p2.setGameId("foo");
                    p2.setOrder(1);


                    r.copyToRealm(g);
                    r.copyToRealm(p1);
                    r.copyToRealm(p2);
                }
            });
        }

        realm.close();
    }
}
