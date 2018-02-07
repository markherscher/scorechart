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

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.realm.Realm;

public class PlayerModificationFragment extends DialogFragment {
    public static final String PLAYER_ID_KEY = "player_id_key";

    private String playerId;
    private Listener listener;

    @BindView(R.id.delete_button) View deleteButton;
    @BindView(R.id.title_text) TextView titleText;
    @BindView(R.id.name) EditText name;

    public static PlayerModificationFragment newInstance(@Nullable String playerId) {
        PlayerModificationFragment fragment = new PlayerModificationFragment();
        Bundle args = new Bundle();
        args.putString(PLAYER_ID_KEY, playerId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_player_modification, null);
        ButterKnife.bind(this, view);

        playerId = getArguments().getString(PLAYER_ID_KEY);

        if (playerId == null) {
            // Creating a new player
            deleteButton.setVisibility(View.INVISIBLE);
            titleText.setText("Add Player");
        } else {
            // Editing an existing player
            Realm realm = Realm.getDefaultInstance();
            Player player = realm.where(Player.class).equalTo(Player.ID, playerId).findFirst();

            if (player == null) {
                throw new IllegalStateException("Failed to find player");
            }

            name.setText(player.getName());
            titleText.setText("Edit Player");
            realm.close();
        }

        dialog.setContentView(view);
        return dialog;
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
                    "Activity or Fragment must extend PlayerModificationFragment.Listener interface");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @OnClick(R.id.ok_button)
    void onOkClicked() {
        boolean isError = false;
        String playerName = name.getText().toString().trim();

        if (playerName.length() == 0) {
            name.setError("Name is required");
            isError = true;
        } else {
            name.setError(null);
        }

        if (!isError) {
            if (listener != null) {
                listener.onPlayerModified(playerId, playerName);
            }

            dismiss();
        }
    }

    @OnClick(R.id.delete_button)
    void onDeleteButton() {
        if (listener != null) {
            listener.onPlayerDeleted(playerId);
        }

        dismiss();
    }

    public interface Listener {
        void onPlayerModified(@Nullable String playerId, @NonNull String playerName);

        void onPlayerDeleted(@NonNull String playerId);
    }
}
