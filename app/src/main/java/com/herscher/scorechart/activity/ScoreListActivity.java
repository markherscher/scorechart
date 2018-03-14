package com.herscher.scorechart.activity;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.herscher.scorechart.R;
import com.herscher.scorechart.fragment.NameModificationFragment;
import com.herscher.scorechart.fragment.ScoreModificationFragment;
import com.herscher.scorechart.fragment.SimpleQuestionDialogFragment;
import com.herscher.scorechart.model.DeleteHelper;
import com.herscher.scorechart.model.Player;
import com.herscher.scorechart.model.Score;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmRecyclerViewAdapter;
import io.realm.RealmResults;

public class ScoreListActivity extends Activity implements ScoreModificationFragment.Listener,
        NameModificationFragment.Listener, SimpleQuestionDialogFragment.Callbacks {
    public final static String PLAYER_ID_KEY = "player_id_key";
    private final static String RENAME_PLAYER_TAG = "rename_player_tag";
    private final static String DELETE_PLAYER_TAG = "delete_player_tag";

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.recycler_view) RecyclerView recyclerView;
    @BindView(R.id.total_score) TextView totalScore;
    @BindView(R.id.entry_count) TextView entryCount;
    @BindView(R.id.no_scores_text) TextView noScoresText;
    private Realm realm;
    private String playerId;
    private RealmResults<Player> playerResults;
    private Player player;
    private ScoreListAdapter scoreListAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_score_list);
        ButterKnife.bind(this);

        playerId = getIntent().getStringExtra(PLAYER_ID_KEY);
        if (playerId == null) {
            throw new IllegalArgumentException("missing player ID");
        }

        toolbar.setNavigationIcon(R.drawable.navigation_back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        toolbar.inflateMenu(R.menu.menu_score_list);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_delete:
                        handlePlayerDelete();
                        return true;

                    case R.id.action_rename:
                        NameModificationFragment fragment = NameModificationFragment.newInstance(
                                playerId, player.getName(), "Rename Player");
                        fragment.show(getFragmentManager(), RENAME_PLAYER_TAG);
                        return true;
                }
                return false;
            }
        });

        scoreListAdapter = new ScoreListAdapter(null);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(scoreListAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        realm = Realm.getDefaultInstance();
        playerResults = realm.where(Player.class).equalTo(Player.ID, playerId).findAllAsync();
        playerResults.addChangeListener(new PlayerChangeListener());
    }

    @Override
    public void onPause() {
        super.onPause();
        playerResults.removeAllChangeListeners();
        player = null;
        realm.close();
    }

    @Override
    public void onScoreModified(final String playerId, final int scoreIndex, final int scoreValue) {
        if (scoreIndex < 0) {
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
        } else {
            // An existing value
            realm.executeTransactionAsync(
                    new Realm.Transaction() {
                        @Override
                        public void execute(Realm r) {
                            Player player = r.where(Player.class).equalTo(Player.ID, playerId)
                                    .findFirst();
                            if (player != null && player.getScores().size() > scoreIndex) {
                                player.getScores().get(scoreIndex).setScoreChange(scoreValue);
                            }
                        }
                    }, new Realm.Transaction.OnSuccess() {
                        @Override
                        public void onSuccess() {
                            scoreListAdapter.notifyItemRangeChanged(scoreIndex,
                                    scoreListAdapter.getItemCount() - scoreIndex);
                        }
                    });
        }
    }

    @Override
    public void onScoreDeleted(final String playerId, final int scoreIndex) {
        realm.executeTransactionAsync(
                new Realm.Transaction() {
                    @Override
                    public void execute(Realm r) {
                        Player player = r.where(Player.class).equalTo(Player.ID, playerId)
                                .findFirst();
                        if (player != null) {
                            player.getScores().remove(scoreIndex);
                        }
                    }
                }, new Realm.Transaction.OnSuccess() {
                    @Override
                    public void onSuccess() {
                        scoreListAdapter.notifyItemRangeChanged(scoreIndex,
                                scoreListAdapter.getItemCount() - scoreIndex);
                    }
                });
    }

    @OnClick(R.id.create_score)
    void onCreateScoreClicked() {
        ScoreModificationFragment fragment = ScoreModificationFragment.newInstance(playerId, -1);
        fragment.show(getFragmentManager(), "ScoreModificationFragment");
    }

    @Override
    public void onNameModified(@NonNull NameModificationFragment fragment,
                               @Nullable final String itemId, @NonNull final String newName) {
        if (RENAME_PLAYER_TAG.equals(fragment.getTag())) {
            realm.executeTransactionAsync(new Realm.Transaction() {
                @Override
                public void execute(Realm r) {
                    Player player = r.where(Player.class).equalTo("id", itemId).findFirst();
                    if (player != null) {
                        player.setName(newName);
                    }
                }
            }, new Realm.Transaction.OnSuccess() {
                @Override
                public void onSuccess() {
                    toolbar.setTitle(player.getName());
                }
            });
        }
    }

    @Override
    public void onQuestionChoice(SimpleQuestionDialogFragment fragment,
                                 SimpleQuestionDialogFragment.Choice choice, String data) {
        if (choice == SimpleQuestionDialogFragment.Choice.POSITIVE) {
            DeleteHelper.deletePlayer(playerId, realm, new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            });
        }
    }

    private void handlePlayerDelete() {
        DialogFragment f =
                SimpleQuestionDialogFragment.newInstance(
                        "Delete Player",
                        "Are you sure you want to delete this player?",
                        "No",
                        "Yes",
                        playerId);

        f.show(getFragmentManager(), DELETE_PLAYER_TAG);
    }

    private class PlayerChangeListener implements RealmChangeListener<RealmResults<Player>> {
        boolean scoresSet;

        @Override
        public void onChange(RealmResults<Player> playerList) {
            player = playerList.first(null);

            if (player != null) {
                toolbar.setTitle(player.getName());
                entryCount.setText(String.format("Entries: %d", player.getScores().size()));
                totalScore.setText(String.format("Score: %d", player.getTotalScore()));
                noScoresText.setVisibility(player.getScores().size() == 0 ? View.VISIBLE : View.GONE);

                if (!scoresSet) {
                    scoreListAdapter.updateData(player.getScores());
                    scoresSet = true;
                }
            }
        }
    }

    class ScoreViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.incremental_score) TextView incrementalScore;
        @BindView(R.id.overall_score) TextView overallScore;
        @BindView(R.id.entry_count) TextView entryCount;
        int scoreIndex;

        ScoreViewHolder(ViewGroup parent) {
            super(LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.view_score, parent, false));

            ButterKnife.bind(this, itemView);
        }

        void showScore(int index) {
            if (player != null) {
                Score score = player.getScores().get(index);
                scoreIndex = index;
                incrementalScore.setText(valueToString(score.getScoreChange(), true));
                overallScore.setText(valueToString(player.getRunningTotal(index), false));
                entryCount.setText((index + 1) + "");

                if (score.getScoreChange() < 0) {
                    incrementalScore.setTextColor(ContextCompat.getColor(ScoreListActivity.this,
                            R.color.negative));
                } else {
                    incrementalScore.setTextColor(ContextCompat.getColor(ScoreListActivity.this,
                            R.color.positive));
                }
            }
        }

        String valueToString(int value, boolean showPositive) {
            if (showPositive && value >= 0) {
                return "+" + value;
            } else {
                return value + "";
            }
        }

        @OnClick(R.id.score_holder)
        void onClicked() {
            ScoreModificationFragment fragment = ScoreModificationFragment.newInstance(playerId,
                    scoreIndex);
            fragment.show(getFragmentManager(), "ScoreModificationFragment");
        }
    }

    class ScoreListAdapter extends RealmRecyclerViewAdapter<Score, ScoreViewHolder> {
        ScoreListAdapter(@Nullable OrderedRealmCollection<Score> data) {
            super(data, true);
        }

        @Override
        public ScoreViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ScoreViewHolder(parent);
        }

        @Override
        public void onBindViewHolder(ScoreViewHolder holder, int position) {
            holder.showScore(position);
        }
    }
}
