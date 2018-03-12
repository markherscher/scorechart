package com.herscher.scorechart.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.app.DialogFragment;
import android.support.v7.app.AlertDialog;

public class SimpleQuestionDialogFragment extends DialogFragment {
    public enum Choice {
        POSITIVE,
        NEGATIVE
    }

    private static final String MESSAGE_KEY = "message-key";
    private static final String TITLE_KEY = "title-key";
    private static final String NEGATIVE_BUTTON_KEY = "negative-key";
    private static final String POSITIVE_BUTTON_KEY = "positive-key";
    private static final String DATA_KEY = "data-key";

    private String title;
    private String message;
    private String negativeButtonText;
    private String positiveButtonText;
    private String data;
    private Callbacks callbacks;

    public interface Callbacks {
        void onQuestionChoice(SimpleQuestionDialogFragment fragment, Choice choice, String data);
    }

    public static SimpleQuestionDialogFragment newInstance(String title,
                                                           String message,
                                                           String negativeButtonText,
                                                           String positiveButtonText,
                                                           String data) {
        SimpleQuestionDialogFragment frag = new SimpleQuestionDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(MESSAGE_KEY, message);
        bundle.putString(TITLE_KEY, title);
        bundle.putString(NEGATIVE_BUTTON_KEY, negativeButtonText);
        bundle.putString(POSITIVE_BUTTON_KEY, positiveButtonText);
        bundle.putString(DATA_KEY, data);
        frag.setArguments(bundle);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();

        if (bundle != null) {
            message = bundle.getString(MESSAGE_KEY);
            title = bundle.getString(TITLE_KEY);
            negativeButtonText = bundle.getString(NEGATIVE_BUTTON_KEY);
            positiveButtonText = bundle.getString(POSITIVE_BUTTON_KEY);
            data = bundle.getString(DATA_KEY);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof Callbacks) {
            callbacks = (Callbacks) context;
        } else if (getParentFragment() instanceof Callbacks) {
            callbacks = (Callbacks) getParentFragment();
        } else {
            throw new IllegalStateException("Activity or Fragment must extend SimpleQuestionDialogFragment.Callbacks interface");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callbacks = null;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title)
                .setMessage(message)
                .setNegativeButton(negativeButtonText, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.cancel();
                        if (callbacks != null) {
                            callbacks.onQuestionChoice(SimpleQuestionDialogFragment.this,
                                    Choice.NEGATIVE, data);
                        }
                    }
                })
                .setPositiveButton(positiveButtonText, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.dismiss();
                        if (callbacks != null) {
                            callbacks.onQuestionChoice(SimpleQuestionDialogFragment.this,
                                    Choice.POSITIVE, data);
                        }
                    }
                });
        return builder.create();
    }
}
