package nz.ac.auckland.nihi.trainer.data.session;

import java.io.Serializable;

import android.content.Context;

/**
 * A goal that can be reached during the course of a single exercise session.
 * 
 * @author Andrew Meads
 * 
 */
public interface ExerciseSessionGoal extends Serializable {

	/**
	 * Returns a value indicating whether the goal has been achieved for the given data.
	 * 
	 * @param data
	 * @return
	 */
	boolean isGoalReached(ExerciseSessionData data);

	String getName(Context context);

	int getIconId();

}
