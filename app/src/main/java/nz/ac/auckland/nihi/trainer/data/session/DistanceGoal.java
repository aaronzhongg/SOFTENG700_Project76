package nz.ac.auckland.nihi.trainer.data.session;

import nz.ac.auckland.nihi.trainer.R.drawable;
import nz.ac.auckland.nihi.trainer.R.string;
import android.content.Context;

public class DistanceGoal implements ExerciseSessionGoal {

	private final float meters;

	public DistanceGoal(float meters) {
		this.meters = meters;
	}

	@Override
	public boolean isGoalReached(ExerciseSessionData data) {
		return data.getDistance() >= meters;
	}

	@Override
	public String getName(Context context) {
		return context.getString(string.goaldialog_label_distancegoal);
	}

	@Override
	public int getIconId() {
		return drawable.nihi_ic_review_distance;
	}

}
