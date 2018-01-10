package com.herscher.scotchbridge.activity;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.herscher.scotchbridge.R;
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
import java.util.Random;
import java.util.UUID;

import butterknife.BindDimen;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmConfiguration;
import io.realm.RealmList;

public class ScoreChartActivity extends Activity implements ScoreModificationFragment.Listener {
    public final static String GAME_ID_KEY = "game_id_key";

    @BindView(R.id.header_layout) LinearLayout headerLayout;
    @BindView(R.id.chart_layout) LinearLayout chartLayout;
    @BindDimen(R.dimen.score_chart_name_height) int nameHeight;
    @BindDimen(R.dimen.score_chart_cell_height) int cellHeight;
    @BindDimen(R.dimen.score_chart_cell_width) int cellWidth;
    @BindDimen(R.dimen.score_chart_name_padding) int namePadding;

    private String gameId;
    private Realm realm;
    private Game game;
    private Map<Player, Column> columnMap;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_score_chart);
        ButterKnife.bind(this);

        columnMap = new HashMap<>();
        gameId = getIntent().getStringExtra(GAME_ID_KEY);

        // tODO:
        gameId = "foo";

        if (gameId == null) {
            throw new IllegalStateException("missing game ID");
        }

        realm = Realm.getDefaultInstance();
        game = realm.where(Game.class).equalTo(Game.ID, gameId).findFirst();

        if (game == null) {
            throw new IllegalStateException("game not found");
        }

        for (Player player : game.getPlayers()) {
            addPlayerColumn(player);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (realm == null) {
            realm = Realm.getDefaultInstance();
            game = realm.where(Game.class).equalTo(Game.ID, gameId).findFirst();
        }

        // Add all change listeners
        for (Player p : game.getPlayers()) {
            p.getScores().addChangeListener(new PlayerScoreChangeListener(p));
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        // Remove all change listeners
        for (Player p : game.getPlayers()) {
            p.getScores().removeAllChangeListeners();
        }

        realm.close();
        realm = null;
    }

    @Override
    public void onAccepted(final String playerId, final String scoreId, final int scoreValue) {
        if (scoreId == null) {
            // A new value
            realm.executeTransactionAsync(
                    new Realm.Transaction() {
                        @Override
                        public void execute(Realm r) {
                            Score newScore = new Score();
                            newScore.setId(UUID.randomUUID().toString());
                            newScore.setScoreChange(scoreValue);

                            Player player = r.where(Player.class).equalTo(Player.ID, playerId).findFirst();
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
                            Score score = r.where(Score.class).equalTo(Score.ID, scoreId).findFirst();
                            if (score != null) {
                                score.setScoreChange(scoreValue);
                            }
                        }
                    });
        }
    }

    @Override
    public void onDeleted(String playerId, final String scoreId) {
        realm.executeTransactionAsync(
                new Realm.Transaction() {
                    @Override
                    public void execute(Realm r) {
                        r.where(Score.class).equalTo(Score.ID, scoreId).findFirst().deleteFromRealm();
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

        Column column = new Column(name, totalScore, columnView, player.getId());
        columnMap.put(player, column);
        column.refreshAllScores();
    }

    private class PlayerScoreChangeListener implements RealmChangeListener<RealmList<Score>> {
        final Player player;

        PlayerScoreChangeListener(Player player) {
            this.player = player;
        }

        @Override
        public void onChange(RealmList<Score> scores) {
            Column column = columnMap.get(player);
            if (column != null) {
                column.refreshAllScores();
            }
        }
    }

    private class Column implements ScoreChartColumnView.Listener {
        final TextView name;
        final TextView totalScore;
        final ScoreChartColumnView columnView;
        final String playerId;

        Column(TextView name, TextView totalScore, ScoreChartColumnView columnView, String playerId) {
            this.name = name;
            this.totalScore = totalScore;
            this.columnView = columnView;
            this.playerId = playerId;
            columnView.setListener(this);
        }

        @Override
        public void onCellClicked(final int cellIndex) {
            ScoreModificationFragment fragment;
            Player player = realm.where(Player.class).equalTo(Player.ID, playerId).findFirst();

            if (cellIndex >= player.getScores().size()) {
                // The last cell
                fragment = ScoreModificationFragment.newInstance(player.getId(),
                        null);
            } else {
                // Not the last cell, so edit in place
                fragment = ScoreModificationFragment.newInstance(player.getId(),
                        player.getScores().get(cellIndex).getId());
            }

            fragment.show(getFragmentManager(), "ScoreModificationFragment");
        }

        void refreshAllScores() {
            Player player = realm.where(Player.class).equalTo(Player.ID, playerId).findFirst();
            int desiredCellCount = player.getScores().size() + 1;
            // Ensure we have enough
            while (columnView.getCellCount() < desiredCellCount) {
                columnView.addCell();
            }

            // Ensure we don't have too many
            while (columnView.getCellCount() > desiredCellCount) {
                columnView.removeCell(player.getScores().size() - 1);
            }

            int currentScore = player.getStartingScore();

            for (int i = 0; i < player.getScores().size(); i++) {
                Score score = player.getScores().get(i);
                currentScore += score.getScoreChange();
                columnView.setCellValues(i, score.getScoreChange(), currentScore);
            }

            totalScore.setText(currentScore + "");
        }
    }
}
