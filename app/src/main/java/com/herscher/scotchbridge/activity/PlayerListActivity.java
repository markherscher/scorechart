package com.herscher.scotchbridge.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.herscher.scotchbridge.R;
import com.herscher.scotchbridge.fragment.NameModificationFragment;
import com.herscher.scotchbridge.model.Game;
import com.herscher.scotchbridge.model.Player;

import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmRecyclerViewAdapter;
import io.realm.RealmResults;

public class PlayerListActivity extends Activity implements NameModificationFragment.Listener {
    public static final String GAME_ID_KEY = "game_id_key";
    private static final String GAME_RENAME_TAG = "game_rename_tag";
    private static final String PLAYER_RENAME_TAG = "player_rename_tag";

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.recycler_view) RecyclerView recyclerView;
    @BindView(R.id.no_players_text) TextView noPlayersText;

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
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
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
                finish();
            }
        });
    }

    private void handleGameRename() {
        NameModificationFragment.newInstance(gameId, game.getName(), "Rename Game")
                .show(getFragmentManager(), GAME_RENAME_TAG);
    }

    private class PlayerChangeListener implements RealmChangeListener<RealmResults<Player>> {
        @Override
        public void onChange(RealmResults<Player> players) {
            noPlayersText.setVisibility(players.size() == 0 ? View.VISIBLE : View.INVISIBLE);
        }
    }

    class PlayerViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.player_name) TextView playerName;
        @BindView(R.id.score) TextView score;
        @BindView(R.id.edit_count) TextView editCount;
        String playerId;

        PlayerViewHolder(ViewGroup parent) {
            super(LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.view_player, parent, false));

            ButterKnife.bind(this, itemView);
        }

        @OnClick(R.id.modify_button)
        void onModifyClicked() {
            // todo
        }

        @OnClick(R.id.container)
        void onContainerClicked() {
            Intent intent = new Intent(PlayerListActivity.this, ScoreListActivity.class);
            intent.putExtra(ScoreListActivity.PLAYER_ID_KEY, playerId);
            startActivity(intent);
        }

        void setPlayer(Player player) {
            int totalScore = player.getTotalScore();

            playerId = player.getId();
            playerName.setText(player.getName());
            score.setText(totalScore + "");
            editCount.setText(player.getScores().size() + "");

            if (totalScore < 0) {
                score.setTextColor(ContextCompat.getColor(PlayerListActivity.this,
                        R.color.negative));
            } else {
                score.setTextColor(ContextCompat.getColor(PlayerListActivity.this,
                        R.color.positive));
            }
        }
    }

    class PlayerAdapter extends RealmRecyclerViewAdapter<Player, PlayerViewHolder> {

        PlayerAdapter(@Nullable OrderedRealmCollection<Player> data) {
            super(data, true);
        }

        @Override
        public PlayerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new PlayerViewHolder(parent);
        }

        @Override
        public void onBindViewHolder(PlayerViewHolder holder, int position) {
            holder.setPlayer(getItem(position));
        }
    }
}
