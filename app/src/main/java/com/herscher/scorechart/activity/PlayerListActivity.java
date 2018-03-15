package com.herscher.scorechart.activity;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.herscher.scorechart.R;
import com.herscher.scorechart.fragment.NameModificationFragment;
import com.herscher.scorechart.fragment.ScoreModificationFragment;
import com.herscher.scorechart.fragment.SimpleQuestionDialogFragment;
import com.herscher.scorechart.model.DeleteHelper;
import com.herscher.scorechart.model.Game;
import com.herscher.scorechart.model.Player;
import com.herscher.scorechart.model.Score;

import java.util.Locale;
import java.util.UUID;

import butterknife.BindDimen;
import butterknife.BindInt;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmRecyclerViewAdapter;
import io.realm.RealmResults;

public class PlayerListActivity extends Activity implements NameModificationFragment.Listener,
        SimpleQuestionDialogFragment.Callbacks, ScoreModificationFragment.Listener {
    public static final String GAME_ID_KEY = "game_id_key";
    private static final String GAME_RENAME_TAG = "game_rename_tag";
    private static final String PLAYER_RENAME_TAG = "player_rename_tag";
    private static final String GAME_DELETE_TAG = "game_delete_tag";
    private static final String PLAYER_DELETE_TAG = "player_delete_tag";

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.recycler_view) RecyclerView recyclerView;
    @BindView(R.id.no_players_text) TextView noPlayersText;
    @BindInt(R.integer.score_list_column_count) int columnCount;
    @BindDimen(R.dimen.game_list_margin) int recyclerViewItemMargin;

    private String gameId;
    private Game game;
    private PlayerAdapter adapter;
    private Realm realm;
    private RealmResults<Player> playerResults;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_list);
        ButterKnife.bind(this);

        gameId = getIntent().getStringExtra(GAME_ID_KEY);
        if (gameId == null) {
            throw new IllegalArgumentException("missing game ID");
        }

        toolbar.setNavigationIcon(R.drawable.navigation_back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        toolbar.inflateMenu(R.menu.menu_player_list);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_delete:
                        handleGameDelete();
                        return true;
                    case R.id.action_rename:
                        handleGameRename();
                        return true;
                }
                return false;
            }
        });

        adapter = new PlayerAdapter(null);
        recyclerView.setLayoutManager(new GridLayoutManager(this, columnCount));
        recyclerView.addItemDecoration(new MarginItemDecoration());
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        realm = Realm.getDefaultInstance();
        game = realm.where(Game.class).equalTo(Game.ID, gameId).findFirst();
        if (game == null) {
            finish();
            return;
        }

        toolbar.setTitle(game.getName());
        playerResults = realm.where(Player.class).equalTo(Player.GAME_ID, gameId).findAllAsync();
        playerResults.addChangeListener(new PlayerChangeListener());
        adapter.updateData(playerResults);
    }

    @Override
    public void onPause() {
        super.onPause();
        playerResults.removeAllChangeListeners();
        realm.close();
    }

    @Override
    public void onNameModified(@NonNull NameModificationFragment fragment,
                               @Nullable final String itemId, @NonNull final String newName) {
        if (PLAYER_RENAME_TAG.equals(fragment.getTag())) {
            renamePlayer(itemId, newName);
        } else if (GAME_RENAME_TAG.equals(fragment.getTag())) {
            renameGame(itemId, newName);
        }
    }

    @Override
    public void onQuestionChoice(SimpleQuestionDialogFragment fragment,
                                 SimpleQuestionDialogFragment.Choice choice, String data) {
        if (choice == SimpleQuestionDialogFragment.Choice.POSITIVE) {
            if (GAME_DELETE_TAG.equals(fragment.getTag())) {
                DeleteHelper.deleteGame(gameId, realm, new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                });
            } else if (PLAYER_DELETE_TAG.equals(fragment.getTag())) {
                DeleteHelper.deletePlayer(data, realm, null);
            }
        }
    }

    @Override
    public void onScoreModified(final String playerId, int scoreIndex, final int scoreValue) {
        // A new value
        realm.executeTransactionAsync(
                new Realm.Transaction() {
                    @Override
                    public void execute(Realm r) {
                        Score newScore = new Score();
                        newScore.setScoreChange(scoreValue);

                        Player player = r.where(Player.class).equalTo(Player.ID, playerId)
                                .findFirst();
                        if (player != null) {
                            player.getScores().add(newScore);
                        }
                    }
                });
    }

    @Override
    public void onScoreDeleted(String playerId, int scoreIndex) {
        // Can't delete a score from here
    }

    private void renamePlayer(final String itemId, final String newName) {
        if (itemId == null) {
            // A new player
            realm.executeTransactionAsync(new Realm.Transaction() {
                @Override
                public void execute(Realm r) {
                    Number maxOrder = r.where(Player.class)
                            .equalTo(Player.GAME_ID, gameId).max(Player.ORDER);

                    Player player = new Player();
                    player.setId(UUID.randomUUID().toString());
                    player.setName(newName);
                    player.setGameId(gameId);
                    player.setOrder(maxOrder == null ? 0 : maxOrder.intValue() + 1);
                    r.copyToRealm(player);
                }
            });
        } else {
            // Modifying existing player
            realm.executeTransactionAsync(new Realm.Transaction() {
                @Override
                public void execute(Realm r) {
                    Player player = r.where(Player.class).equalTo(Player.ID, itemId).findFirst();
                    if (player != null) {
                        player.setName(newName);
                    }
                }
            });
        }
    }

    private void renameGame(final String itemId, final String newName) {
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm r) {
                Game game = r.where(Game.class).equalTo("id", itemId).findFirst();
                if (game != null) {
                    game.setName(newName);
                }
            }
        }, new Realm.Transaction.OnSuccess() {
            @Override
            public void onSuccess() {
                toolbar.setTitle(game.getName());
            }
        });
    }

    @OnClick(R.id.create_player)
    void onCreatePlayerClicked() {
        NameModificationFragment.newInstance(null, "", "Create New Player")
                .show(getFragmentManager(), PLAYER_RENAME_TAG);
    }

    private void handleGameDelete() {
        DialogFragment f =
                SimpleQuestionDialogFragment.newInstance(
                        "Delete Game",
                        "Are you sure you want to delete this game?",
                        "No",
                        "Yes",
                        game.getId());

        f.show(getFragmentManager(), GAME_DELETE_TAG);
    }

    private void handlePlayerDelete(Player player) {
        DialogFragment f =
                SimpleQuestionDialogFragment.newInstance(
                        "Delete Player",
                        "Are you sure you want to delete this player?",
                        "No",
                        "Yes",
                        player.getId());

        f.show(getFragmentManager(), PLAYER_DELETE_TAG);
    }

    private void handleGameRename() {
        NameModificationFragment.newInstance(gameId, game.getName(), "Rename Game")
                .show(getFragmentManager(), GAME_RENAME_TAG);
    }

    private class PlayerChangeListener implements RealmChangeListener<RealmResults<Player>> {
        @Override
        public void onChange(RealmResults<Player> players) {
            // For some reason this looks better slightly delayed
            final boolean isEmpty = players.size() == 0;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    noPlayersText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                }
            }, 250);
        }
    }

    class PlayerViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.player_name) TextView playerName;
        @BindView(R.id.score) TextView score;
        @BindView(R.id.edit_count) TextView editCount;
        @BindView(R.id.player_menu) View playerMenu;
        PopupMenu popupMenu;
        Player player;

        PlayerViewHolder(ViewGroup parent) {
            super(LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.view_player, parent, false));

            ButterKnife.bind(this, itemView);

            popupMenu = new PopupMenu(PlayerListActivity.this, playerMenu);
            popupMenu.inflate(R.menu.menu_score_list);
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    switch (menuItem.getItemId()) {
                        case R.id.action_delete:
                            if (player != null) {
                                handlePlayerDelete(player);
                            }
                            return true;

                        case R.id.action_rename:
                            if (player != null) {
                                DialogFragment f = NameModificationFragment.newInstance(
                                        player.getId(), player.getName(), "Rename Player");
                                f.show(getFragmentManager(), PLAYER_RENAME_TAG);
                            }
                            return true;
                    }
                    return false;
                }
            });
        }

        @OnClick(R.id.player_menu)
        void onModifyClicked() {
            popupMenu.show();
        }

        @OnClick(R.id.quick_add)
        void onQuickAddClicked() {
            ScoreModificationFragment fragment = ScoreModificationFragment.newInstance(
                    player.getId(), -1);
            fragment.show(getFragmentManager(), "ScoreModificationFragment");
        }

        @OnClick(R.id.container)
        void onContainerClicked() {
            Intent intent = new Intent(PlayerListActivity.this, ScoreListActivity.class);
            intent.putExtra(ScoreListActivity.PLAYER_ID_KEY, player.getId());
            startActivity(intent);
        }

        void setPlayer(Player player) {
            int totalScore = player.getTotalScore();

            this.player = player;
            playerName.setText(player.getName());
            score.setText(totalScore + "");
            editCount.setText(String.format(Locale.US, "Entries: %d",
                    player.getScores().size()));
        }
    }

    class PlayerAdapter extends RealmRecyclerViewAdapter<Player, PlayerViewHolder> {
        PlayerAdapter(@Nullable OrderedRealmCollection<Player> data) {
            super(data, true);
        }

        @NonNull
        @Override
        public PlayerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new PlayerViewHolder(parent);
        }

        @Override
        public void onBindViewHolder(@NonNull PlayerViewHolder holder, int position) {
            holder.setPlayer(getItem(position));
        }
    }

    private class MarginItemDecoration extends RecyclerView.ItemDecoration {
        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            int column = position % columnCount;

            outRect.left = column == 0 ? recyclerViewItemMargin : 0;
            outRect.right = recyclerViewItemMargin;
            outRect.top = recyclerViewItemMargin;
        }
    }
}
