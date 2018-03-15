package com.herscher.scorechart.fragment;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.herscher.scorechart.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class NameModificationFragment extends DialogFragment {
    public static final String ITEM_ID_KEY = "item_id_key";
    public static final String EXISTING_NAME_KEY = "existing-name_key";
    public static final String TITLE_KEY = "title_key";

    private String itemId;
    private Listener listener;

    @BindView(R.id.title_text) TextView title;
    @BindView(R.id.name) EditText name;

    public static NameModificationFragment newInstance(@Nullable String itemId,
                                                       @NonNull String existingName,
                                                       @NonNull String title) {
        NameModificationFragment fragment = new NameModificationFragment();
        Bundle args = new Bundle();
        args.putString(ITEM_ID_KEY, itemId);
        args.putString(EXISTING_NAME_KEY, existingName);
        args.putString(TITLE_KEY, title);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_name_modification, null);
        ButterKnife.bind(this, view);

        Bundle args = getArguments();
        itemId = args.getString(ITEM_ID_KEY);
        String titleText = args.getString(TITLE_KEY);
        String existingName = savedInstanceState == null ? args.getString(EXISTING_NAME_KEY) :
                savedInstanceState.getString(EXISTING_NAME_KEY);

        if (existingName == null) {
            existingName = "";
        }

        title.setText(titleText);
        name.setText(existingName);
        name.setSelection(existingName.length());
        name.setImeOptions(EditorInfo.IME_ACTION_DONE);
        name.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    onOkClicked();
                    return true;
                }
                return false;
            }
        });

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
                    "Activity or Fragment must extend NameModificationFragment.Listener interface");
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // Show the keyboard (for some reason it doesn't want to by default)
        super.onActivityCreated(savedInstanceState);
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putString(EXISTING_NAME_KEY, name.getText().toString());
    }

    @OnClick(R.id.ok_button)
    void onOkClicked() {
        boolean isError = false;
        String nameText = name.getText().toString().trim();

        if (nameText.length() == 0) {
            name.setError("Name is required");
            isError = true;
        } else {
            name.setError(null);
        }

        if (!isError) {
            if (listener != null) {
                listener.onNameModified(this, itemId, nameText);
            }

            dismiss();
        }
    }

    @OnClick(R.id.cancel_button)
    void onDeleteButton() {
        dismiss();
    }

    public interface Listener {
        void onNameModified(@NonNull NameModificationFragment fragment, @Nullable String itemId, @NonNull String newName);
    }
}
