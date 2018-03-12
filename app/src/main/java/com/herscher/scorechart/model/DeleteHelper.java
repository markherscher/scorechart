package com.herscher.scorechart.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import io.realm.Realm;
import io.realm.RealmResults;

public class DeleteHelper {
    public static void deleteGame(@NonNull final String gameId, @NonNull Realm realm,
                                  @Nullable final Runnable successAction) {
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm r) {
                Game game = r.where(Game.class).equalTo(Game.ID, gameId).findFirst();
                if (game != null) {
                    game.deleteFromRealm();
                }

                RealmResults<Player> players = r.where(Player.class).equalTo(Player.GAME_ID, gameId).findAll();
                for (Player p : players) {
                    p.getScores().deleteAllFromRealm();
                }

                players.deleteAllFromRealm();
            }
        }, new Realm.Transaction.OnSuccess() {
            @Override
            public void onSuccess() {
                if (successAction != null) {
                    successAction.run();
                }
            }
        });
    }

    public static void deletePlayer(@NonNull final String playerId, @NonNull Realm realm,
                                    @Nullable final Runnable successAction) {
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm r) {
                Player player = r.where(Player.class).equalTo(Player.ID, playerId).findFirst();

                if (player != null) {
                    player.getScores().deleteAllFromRealm();
                    player.deleteFromRealm();
                }
            }
        }, new Realm.Transaction.OnSuccess() {
            @Override
            public void onSuccess() {
                if (successAction != null) {
                    successAction.run();
                }
            }
        });
    }
}
