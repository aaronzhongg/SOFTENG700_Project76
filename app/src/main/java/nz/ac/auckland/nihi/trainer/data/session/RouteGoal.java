package nz.ac.auckland.nihi.trainer.data.session;

import nz.ac.auckland.nihi.trainer.R.drawable;
import nz.ac.auckland.nihi.trainer.R.string;
import android.content.Context;

public class RouteGoal implements ExerciseSessionGoal {

	private final String routeId;

	public RouteGoal(String routeId) {
		this.routeId = routeId;
	}

	@Override
	public boolean isGoalReached(ExerciseSessionData data) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName(Context context) {
		return context.getString(string.goaldialog_label_routegoal);
	}

	public String getRouteId() {
		return routeId;
	}

	@Override
	public int getIconId() {
		return drawable.nihi_ic_home_routes;
	}

}
