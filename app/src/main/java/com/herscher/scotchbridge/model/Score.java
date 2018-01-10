package com.herscher.scotchbridge.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Score extends RealmObject {
    public static final String ID = "id";
    public static final String SCORE_CHANGE = "scoreChange";
    public static final String NOTES = "notes";

    @PrimaryKey private String id;
    private String notes;
    private int scoreChange;

    public Score() {
        id = "";
        notes = "";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @NonNull
    public String getNotes() {
        return notes;
    }

    public void setNotes(@Nullable String notes) {
        this.notes = notes == null ? "" : notes;
    }

    public int getScoreChange() {
        return scoreChange;
    }

    public void setScoreChange(int value) {
        scoreChange = value;
    }
}
