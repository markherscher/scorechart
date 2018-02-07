package com.herscher.scotchbridge.fragment;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.herscher.scotchbridge.R;
import com.herscher.scotchbridge.model.Player;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.realm.Realm;

public class ScoreModificationFragment extends DialogFragment {
    public static final String PLAYER_ID_KEY = "player_id_key";
    public static final String SCORE_INDEX_KEY = "score_index_key";
    private static final String EDIT_VALUE_KEY = "edit_value_key";
    private static final int MULTIPLE_ADJUST_VALUE = 5;

    private Listener listener;
    private String playerId;
    private int scoreIndex;

    @BindView(R.id.title_text) TextView titleText;
    @BindView(R.id.value_entry) EditText valueEntry;
    @BindView(R.id.delete_button) View deleteButton;

    public static ScoreModificationFragment newInstance(@NonNull String playerId, int scoreIndex) {
        ScoreModificationFragment fragment = new ScoreModificationFragment();
        Bundle args = new Bundle();
        args.putString(PLAYER_ID_KEY, playerId);
        args.putInt(SCORE_INDEX_KEY, scoreIndex);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_score_modification, null);
        ButterKnife.bind(this, view);

        playerId = getArguments().getString(PLAYER_ID_KEY);
        scoreIndex = getArguments().getInt(SCORE_INDEX_KEY);

        if (playerId == null) {
            throw new IllegalStateException("missing player IDs");
        }

        Realm realm = Realm.getDefaultInstance();
        Player player = realm.where(Player.class).equalTo(Player.ID, playerId).findFirst();

        if (player == null) {
            throw new IllegalStateException("Failed to find player");
        }

        int scoreValue = 0;

        if (scoreIndex >= 0) {
            // Editing an existing value
            if (scoreIndex > player.getScores().size()) {
                throw new IllegalArgumentException(String.format(
                        "score index of %d is out of bounds %d",
                        scoreIndex, player.getScores().size()));
            }

            scoreValue = player.getScores().get(scoreIndex).getScoreChange();
            titleText.setText("Adjust Score");
        } else {
            // New value, so don't delete
            deleteButton.setVisibility(View.INVISIBLE);
            titleText.setText("New Score");
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
            listener.onScoreModified(playerId, scoreIndex, getEnteredValue());
        }

        dismiss();
    }

    @OnClick(R.id.delete_button)
    void onDeleteButton() {
        if (listener != null) {
            listener.onScoreDeleted(playerId, scoreIndex);
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
        void onScoreModified(String playerId, int scoreIndex, int scoreValue);

        void onScoreDeleted(String playerId, int scoreIndex);
    }
}
