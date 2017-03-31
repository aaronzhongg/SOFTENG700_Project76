package nz.ac.auckland.nihi.trainer.data.session;

import nz.ac.auckland.nihi.trainer.R.drawable;
import nz.ac.auckland.nihi.trainer.R.string;
import android.content.Context;

public class DurationGoal implements ExerciseSessionGoal {

	private final long millis;

	public DurationGoal(long millis) {
		this.millis = millis;
	}

	@Override
	public boolean isGoalReached(ExerciseSessionData data) {
		return data.getElapsedTimeInMillis() >= millis;
	}

	@Override
	public String getName(Context context) {
		return context.getString(string.goaldialog_label_durationgoal);
	}

	@Override
	public int getIconId() {
		return drawable.nihi_ic_review_duration;
	}

}
