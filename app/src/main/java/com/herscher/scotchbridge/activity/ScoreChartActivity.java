package com.herscher.scotchbridge.activity;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.support.v7.widget.Toolbar;

import com.herscher.scotchbridge.R;
import com.herscher.scotchbridge.fragment.PlayerModificationFragment;
import com.herscher.scotchbridge.fragment.ScoreModificationFragment;
import com.herscher.scotchbridge.model.Game;
import com.herscher.scotchbridge.model.Player;
import com.herscher.scotchbridge.model.Score;
import com.herscher.scotchbridge.view.ScoreChartColumnView;
import com.herscher.scotchbridge.view.VerticalTextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import butterknife.BindDimen;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.OrderedCollectionChangeSet;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;

public class ScoreChartActivity extends Activity implements ScoreModificationFragment.Listener, PlayerModificationFragment.Listener {
    public final static String GAME_ID_KEY = "game_id_key";

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.header_layout) LinearLayout headerLayout;
    @BindView(R.id.chart_layout) LinearLayout chartLayout;
    @BindDimen(R.dimen.score_chart_name_height) int nameHeight;
    @BindDimen(R.dimen.score_chart_cell_height) int cellHeight;
    @BindDimen(R.dimen.score_chart_cell_width) int cellWidth;
    @BindDimen(R.dimen.score_chart_name_padding) int namePadding;

    private String gameId;
    private Realm realm;
    private Game game;
    private Map<String, RealmResults<Score>> scoreMap;
    private RealmResults<Player> playerResults;
    private List<Column> columnList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_score_chart);
        ButterKnife.bind(this);

        columnList = new ArrayList<>();
        scoreMap = new HashMap<>();
        gameId = getIntent().getStringExtra(GAME_ID_KEY);

        // tODO:
        gameId = "foo";

        if (gameId == null) {
            throw new IllegalStateException("missing game ID");
        }

        realm = Realm.getDefaultInstance();
        //game = realm.where(Game.class).equalTo(Game.ID, gameId).findFirst();

        toolbar.inflateMenu(R.menu.menu_score_chart_activity);
        toolbar.setOnMenuItemClickListener(menuClickListener);
    }

    @Override
    public void onResume() {
        super.onResume();

        realm = Realm.getDefaultInstance();
        game = realm.where(Game.class).equalTo(Game.ID, gameId).findFirstAsync();
        playerResults = realm.where(Player.class).equalTo(Player.GAME_ID, gameId).findAllSortedAsync(Player.ORDER);

        game.addChangeListener(new RealmChangeListener<Game>() {
            @Override
            public void onChange(Game game) {
                toolbar.setTitle(game.getName());
            }
        });

        playerResults.addChangeListener(playerChangeListener);
    }

    @Override
    public void onPause() {
        super.onPause();

        // Remove all change listeners
        game.removeAllChangeListeners();
        playerResults.removeAllChangeListeners();
        for (RealmResults<Score> r : scoreMap.values()) {
            r.removeAllChangeListeners();
        }

        scoreMap.clear();
        realm.close();
        realm = null;
    }

    @Override
    public void onPlayerModified(@Nullable final String playerId, @NonNull final String playerName, final int startingScore) {
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
                    player.setStartingScore(startingScore);
                    player.setGameId(gameId);
                    player.setOrder(maxOrder == null ? 0 : maxOrder.intValue());
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
                        player.setStartingScore(startingScore);
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
                r.where(Player.class).equalTo(Player.ID, playerId).findAll().deleteAllFromRealm();
            }
        });
    }

    @Override
    public void onScoreModified(final String playerId, final String scoreId, final int scoreValue) {
        if (scoreId == null) {
            // A new value
            realm.executeTransactionAsync(
                    new Realm.Transaction() {
                        @Override
                        public void execute(Realm r) {
                            Number maxOrder = r.where(Score.class)
                                    .equalTo(Score.PLAYER_ID, playerId).max(Score.ORDER);

                            Score newScore = new Score();
                            newScore.setId(UUID.randomUUID().toString());
                            newScore.setScoreChange(scoreValue);
                            newScore.setPlayerId(playerId);
                            newScore.setOrder(maxOrder == null ? 0 : maxOrder.intValue());
                            r.copyToRealm(newScore);
                        }
                    });
        } else {
            // An existing value
            realm.executeTransactionAsync(
                    new Realm.Transaction() {
                        @Override
                        public void execute(Realm r) {
                            Score score = r.where(Score.class).equalTo(Score.ID, scoreId).findFirst();
                            if (score != null) {
                                score.setScoreChange(scoreValue);
                            }
                        }
                    });
        }
    }

    @Override
    public void onScoreDeleted(String playerId, final String scoreId) {
        realm.executeTransactionAsync(
                new Realm.Transaction() {
                    @Override
                    public void execute(Realm r) {
                        r.where(Score.class).equalTo(Score.ID, scoreId).findAll().deleteAllFromRealm();
                    }
                });
    }

    private void addPlayerColumn(Player player) {
        int backgroundColor = headerLayout.getChildCount() % 2 == 0 ? 0xFFFFFFFF : 0xFFDDDDDD;

        LinearLayout holderLayout = new LinearLayout(this);
        holderLayout.setOrientation(LinearLayout.VERTICAL);
        holderLayout.setBackgroundColor(backgroundColor);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 1, 1);
        headerLayout.addView(holderLayout, params);

        final String playerId = player.getId();
        holderLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PlayerModificationFragment.newInstance(playerId)
                        .show(getFragmentManager(), "PlayerModificationFragment");
            }
        });

        // Player name
        TextView name = new VerticalTextView(this, null);
        name.setLines(1);
        name.setEllipsize(TextUtils.TruncateAt.END);
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        name.setGravity(Gravity.START);
        name.setText(player.getName());
        name.setPadding(namePadding, 0, 0, 0);
        params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, nameHeight);
        params.gravity = Gravity.CENTER;
        holderLayout.addView(name, params);

        // Total Score
        TextView totalScore = new TextView(this);
        totalScore.setLines(1);
        totalScore.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        totalScore.setTypeface(null, Typeface.BOLD);
        totalScore.setGravity(Gravity.CENTER);
        totalScore.setPadding(0, 0, 0, namePadding);
        holderLayout.addView(totalScore, new LinearLayout.LayoutParams(
                cellWidth, LinearLayout.LayoutParams.WRAP_CONTENT));

        // Column area
        ScoreChartColumnView columnView = new ScoreChartColumnView(this, null);
        columnView.setBackgroundColor(backgroundColor);

        params = new LinearLayout.LayoutParams(
                cellWidth, LinearLayout.LayoutParams.MATCH_PARENT);
        params.setMargins(0, 0, 1, 0);

        chartLayout.addView(columnView, params);

        Column column = new Column(holderLayout, name, totalScore, columnView, player.getId());
        columnList.add(column);
    }

    private void deletePlayerColumn(int index) {
        Column column = columnList.remove(index);
        headerLayout.removeView(column.holderLayout);
        chartLayout.removeView(column.columnView);
    }

    private void refreshPlayer(Player player) {
        for (Column c : columnList) {
            if (c.playerId.equals(player.getId())) {
                c.name.setText(player.getName());
                c.refreshAllScores();
                break;
            }
        }
    }

    private Toolbar.OnMenuItemClickListener menuClickListener =
            new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    switch (menuItem.getItemId()) {
                        case R.id.add_player:
                            PlayerModificationFragment.newInstance(null)
                                    .show(getFragmentManager(), "PlayerModificationFragment");
                            return true;
                        default:
                            break;
                    }
                    return false;
                }
            };

    private OrderedRealmCollectionChangeListener<RealmResults<Player>> playerChangeListener =
            new OrderedRealmCollectionChangeListener<RealmResults<Player>>() {
                @Override
                public void onChange(RealmResults<Player> players, OrderedCollectionChangeSet changeSet) {
                    if (changeSet != null) {
                        for (int i : changeSet.getInsertions()) {
                            // An added player
                            insertPlayer(players.get(i));
                        }

                        for (int i : changeSet.getDeletions()) {
                            // A deleted player
                            final String playerId = columnList.get(i).playerId;
                            scoreMap.remove(playerId).removeAllChangeListeners();
                            deletePlayerColumn(i);

                            // Delete all the scores for this player as well
                            realm.executeTransactionAsync(new Realm.Transaction() {
                                @Override
                                public void execute(Realm r) {
                                    r.where(Score.class).equalTo(Score.PLAYER_ID, playerId)
                                            .findAll().deleteAllFromRealm();
                                }
                            });
                        }

                        for (int i : changeSet.getChanges()) {
                            // A modified player
                            refreshPlayer(players.get(i));
                        }
                    } else {
                        // The first pass
                        for (Player p : players) {
                            insertPlayer(p);
                        }
                    }
                }

                private void insertPlayer(Player player) {
                    RealmResults<Score> scoreResults = realm.where(Score.class).equalTo(
                            Score.PLAYER_ID, player.getId()).findAllSortedAsync(Score.ORDER);
                    scoreMap.put(player.getId(), scoreResults);
                    scoreResults.addChangeListener(new ScoreChangeListener(player.getId()));

                    addPlayerColumn(player);
                }
            };

    private class ScoreChangeListener implements OrderedRealmCollectionChangeListener<RealmResults<Score>> {
        final String playerId;

        ScoreChangeListener(String playerId) {
            this.playerId = playerId;
        }

        @Override
        public void onChange(RealmResults<Score> scores, OrderedCollectionChangeSet changeSet) {
            Column column = null;
            for (Column c : columnList) {
                if (c.playerId.equals(playerId)) {
                    column = c;
                    break;
                }
            }

            if (column == null) {
                return;
            }

            column.refreshAllScores();
        }
    }

    private class Column implements ScoreChartColumnView.Listener {
        final TextView name;
        final TextView totalScore;
        final ScoreChartColumnView columnView;
        final LinearLayout holderLayout;
        final String playerId;

        Column(LinearLayout holderLayout, TextView name, TextView totalScore, ScoreChartColumnView columnView, String playerId) {
            this.holderLayout = holderLayout;
            this.name = name;
            this.totalScore = totalScore;
            this.columnView = columnView;
            this.playerId = playerId;
            columnView.setListener(this);
        }

        @Override
        public void onCellClicked(final int cellIndex) {
            ScoreModificationFragment fragment;
            RealmResults<Score> scoreResults = scoreMap.get(playerId);

            if (scoreResults != null) {
                if (cellIndex >= scoreResults.size()) {
                    // The last cell
                    fragment = ScoreModificationFragment.newInstance(playerId,
                            null);
                } else {
                    // Not the last cell, so edit in place
                    fragment = ScoreModificationFragment.newInstance(playerId,
                            scoreResults.get(cellIndex).getId());
                }

                fragment.show(getFragmentManager(), "ScoreModificationFragment");
            }
        }

        void refreshAllScores() {
            RealmResults<Score> scoreResults = scoreMap.get(playerId);

            if (scoreResults != null) {
                int desiredCellCount = scoreResults.size() + 1;
                // Ensure we have enough
                while (columnView.getCellCount() < desiredCellCount) {
                    columnView.addCell();
                }

                // Ensure we don't have too many
                while (columnView.getCellCount() > desiredCellCount) {
                    columnView.removeCell(scoreResults.size() - 1);
                }

                int currentScore = 0;//player.getStartingScore(); TODO

                for (int i = 0; i < scoreResults.size(); i++) {
                    Score score = scoreResults.get(i);
                    currentScore += score.getScoreChange();
                    columnView.setCellValues(i, score.getScoreChange(), currentScore);
                }

                totalScore.setText(currentScore + "");
            }
        }
    }
}
