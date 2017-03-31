package nz.ac.auckland.nihi.trainer.data.session;

import nz.ac.auckland.nihi.trainer.R.drawable;
import nz.ac.auckland.nihi.trainer.R.string;
import android.content.Context;

public class IntensityGoal implements ExerciseSessionGoal {

	private final float percentHRR;

	public IntensityGoal(float percentHRR) {
		this.percentHRR = percentHRR;
	}

	@Override
	public boolean isGoalReached(ExerciseSessionData data) {
		return data.getAvgPercentHRR() >= percentHRR;
	}

	@Override
	public String getName(Context context) {
		return context.getString(string.goaldialog_label_loadgoal);
	}

	@Override
	public int getIconId() {
		return drawable.nihi_ic_review_hr;
	}

}
