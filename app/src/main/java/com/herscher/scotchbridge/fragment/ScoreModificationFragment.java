package com.herscher.scotchbridge.fragment;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.herscher.scotchbridge.R;
import com.herscher.scotchbridge.model.Player;
import com.herscher.scotchbridge.model.Score;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.realm.Realm;

public class ScoreModificationFragment extends DialogFragment {
    public static final String PLAYER_ID_KEY = "player_id_key";
    public static final String SCORE_ID_KEY = "score_id_key";
    private static final String EDIT_VALUE_KEY = "edit_value_key";
    private static final int MULTIPLE_ADJUST_VALUE = 5;

    private Listener listener;
    private String playerId;
    private String scoreId;

    @BindView(R.id.title_text) TextView titleText;
    @BindView(R.id.value_entry) EditText valueEntry;
    @BindView(R.id.delete_button) View deleteButton;

    public static ScoreModificationFragment newInstance(@NonNull String playerId, @Nullable String scoreId) {
        ScoreModificationFragment fragment = new ScoreModificationFragment();
        Bundle args = new Bundle();
        args.putString(PLAYER_ID_KEY, playerId);
        args.putString(SCORE_ID_KEY, scoreId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_score_modification, null);
        ButterKnife.bind(this, view);

        playerId = getArguments().getString(PLAYER_ID_KEY);
        scoreId = getArguments().getString(SCORE_ID_KEY);

        if (playerId == null) {
            throw new IllegalStateException("missing player IDs");
        }

        Realm realm = Realm.getDefaultInstance();
        Player player = realm.where(Player.class).equalTo(Player.ID, playerId).findFirst();

        if (player == null) {
            throw new IllegalStateException("Failed to find player");
        }

        int scoreValue = 0;

        if (scoreId != null) {
            // Editing an existing value
            Score score = realm.where(Score.class).equalTo(Score.ID, scoreId).findFirst();
            if (score != null) {
                scoreValue = score.getScoreChange();
            }

            titleText.setText(String.format("Adjust score for %s", player.getName()));
        } else {
            // New value, so don't delete
            deleteButton.setVisibility(View.INVISIBLE);
            titleText.setText(String.format("New score for %s", player.getName()));
        }

        if (savedInstanceState != null && savedInstanceState.containsKey(EDIT_VALUE_KEY)) {
            scoreValue = savedInstanceState.getInt(EDIT_VALUE_KEY);
        }

        setEnteredValue(scoreValue);

        realm.close();
        dialog.setContentView(view);
        return dialog;
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt(EDIT_VALUE_KEY, getEnteredValue());
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof Listener) {
            listener = (Listener) context;
        } else if (getParentFragment() instanceof Listener) {
            listener = (Listener) getParentFragment();
        } else {
            throw new IllegalStateException(
                    "Activity or Fragment must extend ScoreModificationFragment.Listener interface");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @OnClick(R.id.decrement_multiple)
    void onDecrementMultiple() {
        setEnteredValue(getEnteredValue() - MULTIPLE_ADJUST_VALUE);

    }

    @OnClick(R.id.decrement)
    void onDecrement() {
        setEnteredValue(getEnteredValue() - 1);
    }

    @OnClick(R.id.increment)
    void onIncrement() {
        setEnteredValue(getEnteredValue() + 1);
    }

    @OnClick(R.id.increment_multiple)
    void onIncrementMultiple() {
        setEnteredValue(getEnteredValue() + MULTIPLE_ADJUST_VALUE);
    }

    @OnClick(R.id.ok_button)
    void onOkClicked() {
        if (listener != null) {
            listener.onScoreModified(playerId, scoreId, getEnteredValue());
        }

        dismiss();
    }

    @OnClick(R.id.delete_button)
    void onDeleteButton() {
        if (listener != null) {
            listener.onScoreDeleted(playerId, scoreId);
        }

        dismiss();
    }

    private int getEnteredValue() {
        String text = valueEntry.getText().toString().trim();
        if (text.length() > 0) {
            return Integer.valueOf(text);
        } else {
            return 0;
        }
    }

    private void setEnteredValue(int value) {
        valueEntry.setText(value + "");
    }

    public interface Listener {
        void onScoreModified(String playerId, String scoreId, int scoreValue);

        void onScoreDeleted(String playerId, String scoreId);
    }
}
