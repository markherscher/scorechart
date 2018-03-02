package com.herscher.scotchbridge.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.herscher.scotchbridge.R;
import com.herscher.scotchbridge.fragment.PlayerModificationFragment;
import com.herscher.scotchbridge.model.Game;
import com.herscher.scotchbridge.model.Player;

import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmRecyclerViewAdapter;

public class PlayerListActivity extends Activity implements PlayerModificationFragment.Listener {
    public static final String GAME_ID_KEY = "game_id_key";

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.recycler_view) RecyclerView recyclerView;

    private String gameId;
    private PlayerAdapter adapter;
    private Realm realm;

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

        adapter = new PlayerAdapter(null);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        realm = Realm.getDefaultInstance();
        Game game = realm.where(Game.class).equalTo(Game.ID, gameId).findFirst();
        toolbar.setTitle(game.getName());
        adapter.updateData(realm.where(Player.class).equalTo(Player.GAME_ID, gameId).findAllAsync());
    }

    @Override
    public void onPause() {
        super.onPause();
        realm.close();
    }

    @Override
    public void onPlayerModified(@Nullable final String playerId, @NonNull final String playerName) {
        if (playerId == null) {
            // A new player
            realm.executeTransactionAsync(new Realm.Transaction() {
                @Override
                public void execute(Realm r) {
                    Number maxOrder = r.where(Player.class)
                            .equalTo(Player.GAME_ID, gameId).max(Player.ORDER);

                    Player player = new Player();
                    player.setId(UUID.randomUUID().toString());
                    player.setName(playerName);
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
                    Player player = r.where(Player.class).equalTo(Player.ID, playerId).findFirst();
                    if (player != null) {
                        player.setName(playerName);
                    }
                }
            });
        }
    }

    @Override
    public void onPlayerDeleted(@NonNull final String playerId) {
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm r) {
                Player player = r.where(Player.class).equalTo(Player.ID, playerId).findFirst();

                if (player != null) {
                    final Player playerBackup = r.copyFromRealm(player);

                    player.getScores().deleteAllFromRealm();
                    player.deleteFromRealm();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showPlayerDeleteUndoSnackbar(playerBackup);
                        }
                    });
                }
            }
        });
    }

    @OnClick(R.id.create_player)
    void onCreatePlayerClicked() {
        PlayerModificationFragment.newInstance(null)
                .show(getFragmentManager(), "PlayerModificationFragment");
    }

    private void showPlayerDeleteUndoSnackbar(final Player player) {
        Snackbar.make(recyclerView, "Player deleted", Snackbar.LENGTH_LONG)
                .setAction("Undo", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Add the player back
                        realm.executeTransactionAsync(new Realm.Transaction() {
                            @Override
                            public void execute(Realm r) {
                                r.copyToRealm(player);
                            }
                        });
                    }
                })
                .show();
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
