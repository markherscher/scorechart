package com.herscher.scotchbridge.activity;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.herscher.scotchbridge.R;
import com.herscher.scotchbridge.fragment.NameModificationFragment;
import com.herscher.scotchbridge.fragment.SimpleQuestionDialogFragment;
import com.herscher.scotchbridge.model.DeleteHelper;
import com.herscher.scotchbridge.model.Game;
import com.herscher.scotchbridge.model.Player;
import com.herscher.scotchbridge.model.Score;

import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmRecyclerViewAdapter;
import io.realm.RealmResults;

public class GameListActivity extends Activity implements NameModificationFragment.Listener,
        SimpleQuestionDialogFragment.Callbacks {
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

        toolbar.setTitle("All Games");
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

    @Override
    public void onNameModified(@NonNull NameModificationFragment fragment,
                               @Nullable final String itemId, @NonNull final String newName) {
        if (itemId == null) {
            // Create new game
            final String newGameId = UUID.randomUUID().toString();
            final Game newGame = new Game();
            newGame.setId(newGameId);
            newGame.setName(newName);

            realm.executeTransactionAsync(new Realm.Transaction() {
                @Override
                public void execute(Realm r) {
                    r.copyToRealm(newGame);
                }
            }, new Realm.Transaction.OnSuccess() {
                @Override
                public void onSuccess() {
                    // Show the new game
                    Intent intent = new Intent(GameListActivity.this, PlayerListActivity.class);
                    intent.putExtra(PlayerListActivity.GAME_ID_KEY, newGameId);
                    startActivity(intent);
                }
            });
        } else {
            realm.executeTransactionAsync(new Realm.Transaction() {
                @Override
                public void execute(Realm r) {
                    Game game = r.where(Game.class).equalTo("id", itemId).findFirst();
                    if (game != null) {
                        game.setName(newName);
                    }
                }
            });
        }
    }

    @Override
    public void onQuestionChoice(SimpleQuestionDialogFragment fragment,
                                 SimpleQuestionDialogFragment.Choice choice, String data) {
        if (choice == SimpleQuestionDialogFragment.Choice.POSITIVE) {
            DeleteHelper.deleteGame(data, realm, null);
        }
    }

    @OnClick(R.id.create_game)
    void onCreateGameClicked() {
        NameModificationFragment.newInstance(null, "", "Create New Game")
                .show(getFragmentManager(), "NameModificationFragment");
    }

    private void showGame(String gameId) {
        Intent intent = new Intent(this, PlayerListActivity.class);
        intent.putExtra(PlayerListActivity.GAME_ID_KEY, gameId);
        startActivity(intent);
    }

    private RealmChangeListener<RealmResults<Game>> gameChangeListener =
            new RealmChangeListener<RealmResults<Game>>() {
                @Override
                public void onChange(RealmResults<Game> gameList) {
                    // For some reason this looks better slightly delayed
                    final boolean isEmpty = gameList.size() == 0;
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            noGamesText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                        }
                    }, 250);
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
        @BindView(R.id.game_menu) View gameMenu;
        PopupMenu popupMenu;
        Game game;

        GameViewHolder(ViewGroup parent) {
            super(LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.view_game, parent, false));
            ButterKnife.bind(this, itemView);

            popupMenu = new PopupMenu(GameListActivity.this, gameMenu);
            popupMenu.inflate(R.menu.menu_player_list);
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    switch (menuItem.getItemId()) {
                        case R.id.action_delete:
                            if (game != null) {
                                DialogFragment f =
                                        SimpleQuestionDialogFragment.newInstance(
                                                "Delete Game",
                                                "Are you sure you want to delete this game?",
                                                "No",
                                                "Yes",
                                                game.getId());

                                f.show(getFragmentManager(), "DeleteGameFragment");
                            }
                            return true;

                        case R.id.action_rename:
                            if (game != null) {
                                DialogFragment f = NameModificationFragment.newInstance(
                                        game.getId(), game.getName(), "Rename Game");
                                f.show(getFragmentManager(), "NameModificationFragment");
                            }
                            return true;
                    }
                    return false;
                }
            });
        }

        void setGame(Game game) {
            this.game = game;
            name.setText(game.getName());
        }

        @OnClick(R.id.game_holder)
        void onGameClicked() {
            if (game != null) {
                showGame(game.getId());
            }
        }

        @OnClick(R.id.game_menu)
        void showGameMenu() {
            popupMenu.show();
        }
    }
}
