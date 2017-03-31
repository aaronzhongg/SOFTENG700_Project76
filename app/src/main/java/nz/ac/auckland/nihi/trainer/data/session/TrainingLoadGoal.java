package nz.ac.auckland.nihi.trainer.data.session;

import nz.ac.auckland.nihi.trainer.R.drawable;
import nz.ac.auckland.nihi.trainer.R.string;
import android.content.Context;

public class TrainingLoadGoal implements ExerciseSessionGoal {

	private final double load;

	public TrainingLoadGoal(double load) {
		this.load = load;
	}

	@Override
	public boolean isGoalReached(ExerciseSessionData data) {
		return data.getCumulativeTrainingLoad() >= load;
	}

	@Override
	public String getName(Context context) {
		return context.getString(string.goaldialog_label_loadgoal);
	}

	@Override
	public int getIconId() {
		return drawable.nihi_ic_review_energy;
	}

}
