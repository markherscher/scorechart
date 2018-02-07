package com.herscher.scotchbridge.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;

public class Player extends RealmObject {
    public static final String ID = "id";
    public static final String GAME_ID = "gameId";
    public static final String NAME = "name";
    public static final String ORDER = "order";

    @PrimaryKey private String id;
    @Index private String gameId;
    private String name;
    private int order;
    private RealmList<Score> scores;

    public Player() {
        id = "";
        gameId = "";
        name = "";
        order = 0;
        scores = new RealmList<>();
    }

    @NonNull
    public RealmList<Score> getScores() {
        return scores;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@Nullable String name) {
        this.name = name == null ? "" : name;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public int getTotalScore() {
        int total = 0;

        for (Score s : scores) {
            total += s.getScoreChange();
        }

        return total;
    }

    public int getRunningTotal(int scoreIndex) {
        int total = 0;

        for (int i = 0; i <= scoreIndex; i++) {
            total += scores.get(i).getScoreChange();
        }

        return total;
    }

    @Override
    public String toString() {
        return name;
    }
}
