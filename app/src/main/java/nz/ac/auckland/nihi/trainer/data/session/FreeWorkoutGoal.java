package nz.ac.auckland.nihi.trainer.data.session;

import nz.ac.auckland.nihi.trainer.R.string;
import android.content.Context;

public class FreeWorkoutGoal implements ExerciseSessionGoal {

	@Override
	public boolean isGoalReached(ExerciseSessionData data) {
		return false;
	}

	@Override
	public String getName(Context context) {
		return context.getString(string.goaldialog_label_freeworkout);
	}

	@Override
	public int getIconId() {
		return 0;
	}

}
