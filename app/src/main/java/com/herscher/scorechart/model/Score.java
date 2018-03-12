package com.herscher.scorechart.model;

import io.realm.RealmObject;

public class Score extends RealmObject {
    public static final String SCORE_CHANGE = "scoreChange";

    private int scoreChange;

    public int getScoreChange() {
        return scoreChange;
    }

    public void setScoreChange(int value) {
        scoreChange = value;
    }
}
