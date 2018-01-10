package com.herscher.scotchbridge.view;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import com.herscher.scotchbridge.R;

import butterknife.BindDimen;
import butterknife.ButterKnife;

public class ScoreChartColumnView extends LinearLayout {
    @BindDimen(R.dimen.score_chart_cell_height) int cellHeight;
    @BindDimen(R.dimen.score_chart_cell_width) int cellWidth;
    private Listener listener;
    private ChartCellView lastCell;
    private int backgroundColor;

    public ScoreChartColumnView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        ButterKnife.bind(this);
        setOrientation(LinearLayout.VERTICAL);
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    public void addCell() {
        ChartCellView cellView = new ChartCellView(getContext(), null);
        cellView.setOnClickListener(cellClickListener);
        addView(cellView, new LinearLayout.LayoutParams(cellWidth, cellHeight));

        cellView.setBackgroundColor(0xFF00FFFF);

        if (lastCell != null) {
            lastCell.setBackgroundColor(backgroundColor);
        }

        lastCell = cellView;
    }

    @Override
    public void setBackgroundColor(int color) {
        backgroundColor = color;
        super.setBackgroundColor(color);
    }

    public void removeCell(int cellIndex) {
        removeViewAt(cellIndex);
    }

    public void setCellValues(int cellIndex, @Nullable Integer incrementalValue, @Nullable Integer overallValue) {
        View view = getChildAt(cellIndex);
        ((ChartCellView) view).setIncrementalScore(incrementalValue);
        ((ChartCellView) view).setOverallScore(overallValue);
    }

    public int getCellCount() {
        return getChildCount();
    }

    private View.OnClickListener cellClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (listener != null) {
                for (int i = 0; i < getChildCount(); i++) {
                    if (getChildAt(i) == view) {
                        listener.onCellClicked(i);
                        return;
                    }
                }
            }
        }
    };

    public interface Listener {
        void onCellClicked(int cellIndex);
    }
}
