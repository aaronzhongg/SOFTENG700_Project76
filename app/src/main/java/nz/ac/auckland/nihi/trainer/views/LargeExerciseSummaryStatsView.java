package nz.ac.auckland.nihi.trainer.views;

import android.content.Context;
import android.util.AttributeSet;

public class LargeExerciseSummaryStatsView extends AbstractExerciseSummaryStatsView {

	public LargeExerciseSummaryStatsView(Context context) {
		super(context);
	}

	public LargeExerciseSummaryStatsView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public LargeExerciseSummaryStatsView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected int getLayoutId() {
		return nz.ac.auckland.nihi.trainer.R.layout.exercise_summary_stats_expanded_view;
	}

}
