package nz.ac.auckland.nihi.trainer.services.workout;

import nz.ac.auckland.nihi.trainer.data.session.ExerciseSessionData;
import nz.ac.auckland.nihi.trainer.messaging.QuestionData;
import android.location.Location;

/**
 * Listens for notifications from an {@link IWorkoutService} service. All notifications will occur on the UI thread.
 * 
 * @author Andrew Meads
 * 
 */
public interface WorkoutServiceListener {

	/**
	 * Called when the session object changes. Listeners should dereference the old session object and reference the new
	 * one.
	 * 
	 * @param newSession
	 */
	void sessionChanged(ExerciseSessionData newSession);

	/**
	 * Called when the service status changes. The new status object is given, along with the type of status change.
	 * 
	 * @param changeType
	 * @param status
	 */
	void workoutServiceStatusChanged(WorkoutServiceStatusChangeType changeType, WorkoutServiceStatus status);

	/**
	 * Called when the GPS service reports a new location. This is reported here as well as in the session object, as
	 * parties are likely to want to display this data seperately.
	 * 
	 * @param newLocation
	 */
	void locationChanged(Location newLocation);

	/**
	 * Called when a question is asked by the doctor monitoring the participant. It is expected that this will trigger a
	 * UI element that will eventually result in a call back to
	 * {@link IWorkoutService#replyToQuestion(nz.ac.auckland.nihi.trainer.messaging.AnswerData)}.
	 * 
	 * @param question
	 */
	void questionReceivedFromDoctor(QuestionData question);

	void serviceConnectionError(WorkoutServiceConnectionErrorType errorType);

	/**
	 * Enumerates the possible communication error types for the workout service.
	 */
	public enum WorkoutServiceConnectionErrorType {
		BluetoothDisabled, NoBluetoothAddress, BioharnessError, GPSError, OdinError;
	}

	/**
	 * Enumerates the possible status changes.
	 */
	public enum WorkoutServiceStatusChangeType {

		Bluetooth, BluetoothAddress, BioharnessStatus, GPSStatus, OdinStatus, MonitoringStatus;

	}

}
