package com.herscher.scotchbridge.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Player extends RealmObject {
    public static final String ID = "id";
    public static final String GAME_ID = "gameId";
    public static final String NAME = "name";
    public static final String STARTING_SCORE = "startingScore";
    public static final String SCORES = "scores";

    @PrimaryKey private String id;
    //private String gameId;
    private RealmList<Score> scores;
    private String name;
    private int startingScore;

    public Player() {
        id = "";
        //gameId = "";
        name = "";
        startingScore = 0;
        scores = new RealmList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /*
    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }*/

    @NonNull
    public RealmList<Score> getScores() {
        return scores;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@Nullable String name) {
        this.name = name == null ? "" : name;
    }

    public int getStartingScore() {
        return startingScore;
    }

    public void setStartingScore(int startingScore) {
        this.startingScore = startingScore;
    }

    public void deleteChildrenFromRealm() {
        scores.deleteAllFromRealm();
    }

    @Override
    public String toString() {
        return name;
    }
}
