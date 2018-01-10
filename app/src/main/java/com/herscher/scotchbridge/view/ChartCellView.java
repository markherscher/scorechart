package com.herscher.scotchbridge.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.herscher.scotchbridge.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ChartCellView extends FrameLayout {
    @BindView(R.id.incremental_score) TextView incrementalScore;
    @BindView(R.id.overall_score) TextView overallScore;

    public ChartCellView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        View view = inflate(context, R.layout.view_chart_cell, this);
        ButterKnife.bind(this, view);
    }

    public void setIncrementalScore(@Nullable Integer value) {
        String text;

        if (value == null) {
            text = "";
        } else {
            text = "" + value;
        }

        incrementalScore.setText(text);
    }

    public void setOverallScore(@Nullable Integer value) {
        String text;

        if (value == null) {
            text = "";
        } else {
            text = "" + value;
        }

        overallScore.setText(text);
    }
}
