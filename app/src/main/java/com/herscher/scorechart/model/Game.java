package com.herscher.scorechart.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Game extends RealmObject {
    public static final String ID = "id";
    public static final String NAME = "name";

    @PrimaryKey private String id;
    private String name;

    public Game() {
        id = "";
        name = "";
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@Nullable String id) {
        this.id = id == null ? "" : id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@Nullable String name) {
        this.name = name == null ? "" : name;
    }

    @Override
    public String toString() {
        return name;
    }
}
