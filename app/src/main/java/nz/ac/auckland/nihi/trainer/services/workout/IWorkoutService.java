package nz.ac.auckland.nihi.trainer.services.workout;

import nz.ac.auckland.nihi.trainer.data.ExerciseSummary;
import nz.ac.auckland.nihi.trainer.data.Symptom;
import nz.ac.auckland.nihi.trainer.data.SymptomStrength;
import nz.ac.auckland.nihi.trainer.data.session.ExerciseSessionData;
import nz.ac.auckland.nihi.trainer.data.session.ExerciseSessionGoal;
import nz.ac.auckland.nihi.trainer.messaging.AnswerData;
import android.location.Location;

public interface IWorkoutService {

	WorkoutServiceStatus getStatus();

	void setWorkoutServiceListener(WorkoutServiceListener listener);

	ExerciseSessionData getCurrentSession();

	void retryOdinConnection();

	void retryBioharnessConnection();

	ExerciseSessionData startWorkout(ExerciseSessionGoal goal);

	ExerciseSummary endWorkout();

	void replyToQuestion(AnswerData answer);

	void reportSymptom(Symptom symptom, SymptomStrength strength);

	Location getLastKnownLocation();

	void clearNotificationTrayIcon();
}
