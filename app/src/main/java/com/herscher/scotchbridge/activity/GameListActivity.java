package com.herscher.scotchbridge.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.herscher.scotchbridge.R;
import com.herscher.scotchbridge.model.Game;
import com.herscher.scotchbridge.model.Player;
import com.herscher.scotchbridge.model.Score;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmRecyclerViewAdapter;
import io.realm.RealmResults;

public class GameListActivity extends Activity {
    @BindView(R.id.recycler_view) RecyclerView recyclerView;
    @BindView(R.id.no_games_text) TextView noGamesText;
    @BindView(R.id.toolbar) Toolbar toolbar;

    private Realm realm;
    private GameRecyclerAdapter adapter;
    private RealmResults<Game> gameResults;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_list);
        ButterKnife.bind(this);

        toolbar.setTitle("List of Games");
        adapter = new GameRecyclerAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        realm = Realm.getDefaultInstance();
        gameResults = realm.where(Game.class).findAllAsync();
        gameResults.addChangeListener(gameChangeListener);
        adapter.updateData(gameResults);
    }

    @Override
    public void onPause() {
        super.onPause();
        gameResults.removeAllChangeListeners();
        realm.close();
    }

    @OnClick(R.id.create_game)
    void onCreateGameClicked() {
        Game newGame = new Game();
        newGame.setId(UUID.randomUUID().toString());
        newGame.setName("Game Name");
        createNewGame(newGame, null, null);
    }

    private void showGame(String gameId) {
        Intent intent = new Intent(this, PlayerListActivity.class);
        intent.putExtra(PlayerListActivity.GAME_ID_KEY, gameId);
        startActivity(intent);
    }

    private void showGameDeleteUndoSnackbar(final Game game, final List<Player> players) {
        Snackbar.make(recyclerView, "Game deleted", Snackbar.LENGTH_LONG)
                .setAction("Undo", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Add the game back
                        createNewGame(game, players, new ArrayList<Score>());
                    }
                })
                .show();
    }

    private void createNewGame(@NonNull final Game game, final List<Player> players, final List<Score> scores) {
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm r) {
                r.copyToRealm(game);

                if (players != null) {
                    r.copyToRealm(players);
                }

                if (scores != null) {
                    r.copyToRealm(scores);
                }
            }
        });
    }

    private RealmChangeListener<RealmResults<Game>> gameChangeListener =
            new RealmChangeListener<RealmResults<Game>>() {
                @Override
                public void onChange(RealmResults<Game> gameList) {
                    // For some reason this looks better slightly delayed
                    final boolean isEmpty = gameList.size() == 0;
                    new Handler().post(new Runnable() {
                        @Override
                        public void run() {
                            noGamesText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                        }
                    });
                }
            };

    private class GameRecyclerAdapter extends RealmRecyclerViewAdapter<Game, GameViewHolder> {
        GameRecyclerAdapter() {
            super(null, true);
        }

        @Override
        public GameViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new GameViewHolder(parent);
        }

        @Override
        public void onBindViewHolder(GameViewHolder holder, int position) {
            holder.setGame(getItem(position));
        }
    }

    class GameViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.name) TextView name;
        String gameId;

        GameViewHolder(ViewGroup parent) {
            super(LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.view_game, parent, false));
            ButterKnife.bind(this, itemView);
        }

        void setGame(Game game) {
            gameId = game.getId();
            name.setText(game.getName());
        }

        @OnClick(R.id.game_holder)
        void onGameClicked() {
            showGame(gameId);
        }

        @OnClick(R.id.delete_button)
        void onDeleteClicked() {
            realm.executeTransactionAsync(new Realm.Transaction() {
                @Override
                public void execute(Realm r) {
                    Game game = r.where(Game.class).equalTo(Game.ID, gameId).findFirst();
                    final Game gameBackup = r.copyFromRealm(game);

                    if (game != null) {
                        game.deleteFromRealm();
                    }

                    RealmResults<Player> players = r.where(Player.class).equalTo(Player.GAME_ID, gameId).findAll();
                    final List<Player> playersBackup = r.copyFromRealm(players);

                    for (Player p : players) {
                        p.getScores().deleteAllFromRealm();
                    }

                    players.deleteAllFromRealm();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showGameDeleteUndoSnackbar(gameBackup, playersBackup);
                        }
                    });
                }
            });
        }
    }
}
