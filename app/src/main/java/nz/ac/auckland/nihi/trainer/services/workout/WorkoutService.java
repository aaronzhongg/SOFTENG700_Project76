package nz.ac.auckland.nihi.trainer.services.workout;

import java.sql.SQLException;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import nz.ac.auckland.cs.android.utils.NotificationUtils;
import nz.ac.auckland.cs.odin.android.api.MessageHandler;
import nz.ac.auckland.cs.odin.android.api.prefs.OdinPreferences;
import nz.ac.auckland.cs.odin.android.api.services.IOdinService;
import nz.ac.auckland.cs.odin.android.api.services.OdinAccessingService;
import nz.ac.auckland.cs.odin.android.api.services.testharness.TestHarnessUtils;
import nz.ac.auckland.cs.odin.data.BasicStringMessage;
import nz.ac.auckland.cs.odin.data.JsonMessage;
import nz.ac.auckland.cs.odin.data.Message;
import nz.ac.auckland.cs.odin.interconnect.common.EndpointConnectionStatus;
import nz.ac.auckland.cs.ormlite.DatabaseManager;
import nz.ac.auckland.cs.ormlite.LocalDatabaseHelper;
import nz.ac.auckland.nihi.trainer.R;
import nz.ac.auckland.nihi.trainer.R.string;
import nz.ac.auckland.nihi.trainer.activities.NotificationViewerActivity;
import nz.ac.auckland.nihi.trainer.data.ExerciseNotification;
import nz.ac.auckland.nihi.trainer.data.ExerciseSummary;
import nz.ac.auckland.nihi.trainer.data.NihiDBHelper;
import nz.ac.auckland.nihi.trainer.data.Route;
import nz.ac.auckland.nihi.trainer.data.Symptom;
import nz.ac.auckland.nihi.trainer.data.SymptomEntry;
import nz.ac.auckland.nihi.trainer.data.SymptomStrength;
import nz.ac.auckland.nihi.trainer.data.session.ExerciseSessionData;
import nz.ac.auckland.nihi.trainer.data.session.ExerciseSessionGoal;
import nz.ac.auckland.nihi.trainer.data.session.ExerciseSessionGoalListener;
import nz.ac.auckland.nihi.trainer.messaging.AnswerData;
import nz.ac.auckland.nihi.trainer.messaging.ECGDataResponse;
import nz.ac.auckland.nihi.trainer.messaging.MessageData;
import nz.ac.auckland.nihi.trainer.messaging.QuestionData;
import nz.ac.auckland.nihi.trainer.prefs.NihiPreferences;
import nz.ac.auckland.nihi.trainer.rules.ExampleRules;
import nz.ac.auckland.nihi.trainer.rules.RulesUtils;
import nz.ac.auckland.nihi.trainer.services.location.DummyGPSServiceImpl;
import nz.ac.auckland.nihi.trainer.services.location.GPSServiceImpl;
import nz.ac.auckland.nihi.trainer.services.location.GPSServiceListener;
import nz.ac.auckland.nihi.trainer.services.location.IGPSService;
import nz.ac.auckland.nihi.trainer.services.workout.ECGDataCache.ECGData;
import nz.ac.auckland.nihi.trainer.services.workout.WorkoutServiceListener.WorkoutServiceConnectionErrorType;
import nz.ac.auckland.nihi.trainer.services.workout.WorkoutServiceListener.WorkoutServiceStatusChangeType;
import nz.ac.auckland.nihi.trainer.util.AndroidTextUtils;

import org.apache.log4j.Logger;
import org.jeasy.rules.annotation.Rule;
import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rules;
import org.jeasy.rules.api.RulesEngine;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;

import com.j256.ormlite.dao.Dao;
import com.odin.android.bioharness.BHConnectivityStatus;
import com.odin.android.bioharness.data.ECGWaveformData;
import com.odin.android.bioharness.data.GeneralData;
import com.odin.android.bioharness.data.SummaryData;
import com.odin.android.bioharness.prefs.BioharnessDescription;
import com.odin.android.bioharness.prefs.BioharnessPreferences;
import com.odin.android.bioharness.service.BioHarnessServiceImpl;
import com.odin.android.bioharness.service.IBioHarnessService;
import com.odin.android.bioharness.service.IBioHarnessService.IBioHarnessServiceListener;
import com.odin.android.bioharness.service.dummy.DummyBioHarnessServiceImpl;
import com.odin.android.services.LocalBinder;

import static org.jeasy.rules.core.RulesEngineBuilder.aNewRulesEngine;

/**
 * Yet another version of the workout service.
 * 
 * @author Andrew Meads
 * 
 */
public class WorkoutService extends Service implements IWorkoutService, IBioHarnessServiceListener,
		GPSServiceListener, ExerciseSessionGoalListener {

	private static final Logger logger = Logger.getLogger(WorkoutService.class);

	// ************************************************************************************************************
	// Instance variables
	// ************************************************************************************************************

	public static final int NUM_BIOHARNESS_RETRIES_KNOWN_ADDRESS = 10;
	public static final int NUM_BIOHARNESS_RETRIES_INITIAL_CONNECTION = 5;

	// The ID of the message notification displayed to the user.
	private static final int NOTIFICATION_RECV_MSG = 1000;
	private static final int NOTIFICATION_REACH_GOAL = 1001;

	// Intents for GPS, Bioharness, and Odin services.
	private Intent bioharnessServiceIntent, gpsServiceIntent;

	// Bioharness service reference
	private LocalBinder<IBioHarnessService> bioharnessService;

	// GPS service reference
	private LocalBinder<IGPSService> gpsService;

	// The ExerciseSession object, where we store data incoming from sensors.
	private ExerciseSessionData currentSession;

	// Listens for notifications from this service.
	private WorkoutServiceListener listener;

	// Handles messages on the UI thread.
	private Handler uiHandler;

	// Indicators for whether we're trying to connect to various services. If these are true when we receive a
	// "disconnected" notification, its a good indicator of initial connection to a particular service failing.
	private boolean connectingToBioharness = false; /* connectingToGPS = false, */ /*connectingToOdin = false TODO: REMOVE ODIN CONNECTION*/

	// Indicates whether we're deliberately trying to disconnect from the Bioharness (so we don't auto-retry connecting
	// when this flag is true).
	private boolean disconnectingFromBioharness = false;

	// The database connection
	private LocalDatabaseHelper dbHelper;

	// The engine for text-to-speech.
	public static TextToSpeech tts;

	// True if tts initialization is complete, false otherwise.
	private boolean ttsEnabled = true;

	//rule engine
//	private RulesEngine rulesEngine = aNewRulesEngine()
//			.withSkipOnFirstAppliedRule(true)
//			.withSilentMode(true)
//			.build();;
//	private Rules rules = new Rules();
//	private Facts facts = new Facts();
//	private ExampleRules exampleRule = new ExampleRules();
	private RulesUtils heartRateRulesUtils;
	private RulesUtils speedRulesUtils;
	private static String feedback = "";

	// ************************************************************************************************************

	// ************************************************************************************************************
	// IWorkoutService Implementation
	// ************************************************************************************************************

	/**
	 * Gets the singleton status object for this service. It will always be up-to-date.
	 */
	@Override
	public WorkoutServiceStatus getStatus() {
		return this.statusObject;
	}

	/**
	 * Gets the current session object used to track sensor data.
	 */
	@Override
	public ExerciseSessionData getCurrentSession() {
		return this.currentSession;
	}

	/**
	 * Sets the listener which will listen for notifications from this service.
	 */
	@Override
	public void setWorkoutServiceListener(WorkoutServiceListener listener) {
		this.listener = listener;
	}

	/**
	 * If we're disconected from the Bioharness, this method attempts to start connecting to it. If not, this method
	 * does nothing.
	 */
	@Override
	public void retryBioharnessConnection() {
		retryBioharnessConnection(NUM_BIOHARNESS_RETRIES_INITIAL_CONNECTION);
	}

	private void retryBioharnessConnection(int numRetries) {
		if (bioharnessService.getService().getStatus() == BHConnectivityStatus.DISCONNECTED) {
			connectingToBioharness = true;
			bioharnessService.getService().setNumRetries(numRetries);
			bioharnessService.getService().connectToBioHarnessFromPrefs();
		}
	}

	/**
	 * If we're disconnected from Odin, this method attempts to start connecting to it. If not, this method does
	 * nothing.
	 */
//	TODO: REMOVE ODIN CONNECTION
//	@Override
//	public void retryOdinConnection() {
//		if (getOdinService().getConnectionStatus() == EndpointConnectionStatus.DISCONNECTED) {
//			connectingToOdin = true;
//			// getOdinService().beginConnect();
//			this.beginOdinConnect();
//		}
//	}

	/**
	 * If we're not currently monitoring, and are connected to the Bioharness, Odin, and the GPS, starts a new
	 * monitoring session and returns the new session object. If any of these assertions are false, throws an Exception.
	 */
	@Override
	public ExerciseSessionData startWorkout(ExerciseSessionGoal goal) {

		//TODO do timing of feedback via rules instead of runnables
//		Runnable ttsRunnable = new Runnable() {
//			public void run() {
//				speak(feedback);
//			}
//		};
//		ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);
//		exec.scheduleAtFixedRate(ttsRunnable , 0, 1, TimeUnit.MINUTES);

		// Verify that we're not currently in a monitoring session
		if (currentSession.isMonitoring()) {
			throw new UnsupportedOperationException(
					"Cannot start a new monitoring session without ending the previous one.");
		}

		// Verify that we're connected to GPS
		if (!gpsService.getService().isConnected()) {
			throw new UnsupportedOperationException("Must be connected to the GPS service.");
		}

		// Verify that we're connected to the Bioharness
		if (bioharnessService.getService().getStatus() != BHConnectivityStatus.CONNECTED) {
			throw new UnsupportedOperationException("Must be connected to the Bioharness.");
		}

		//	TODO: REMOVE ODIN CONNECTION
//		// Verify that we're connected to Odin
//		if (getOdinService().getConnectionStatus() != EndpointConnectionStatus.CONNECTED) {
//			throw new UnsupportedOperationException("Must be connected to Odin.");
//		}

		//	TODO: REMOVE ODIN CONNECTION
		// Send the start signal to Odin
//		OdinHelper.sendStartSessionPacket(getOdinService());

		// Create the session object and notify listeners.
		Location currentLocation = gpsService.getService().getLastKnownLocation();
		currentSession = new ExerciseSessionData(this, currentLocation, goal);
		currentSession.addGoalListener(this);
		uiHandler.obtainMessage(MSG_SESSION_CHANGE, currentSession).sendToTarget();
		uiHandler.obtainMessage(MSG_STATUS_CHANGE, WorkoutServiceStatusChangeType.MonitoringStatus).sendToTarget();

		return currentSession;
	}

	/**
	 * If we're currently monitoring, stop monitoring and generate a summary of the session. If not, throw an exception.
	 */
	@Override
	public ExerciseSummary endWorkout() {
		// Verify we're monitoring
		if (!currentSession.isMonitoring()) {
			throw new UnsupportedOperationException("Cannot stop a session that's not started!");
		}

		clearNotificationTrayIcon();
		NotificationUtils.clearNotification(this, NOTIFICATION_REACH_GOAL);

//		// Create the summary
		ExerciseSummary summary = null;
//		try {
//			// Create summary, save to DB.
//			Dao<ExerciseSummary, String> summaryDao = getDbHelper().getSectionHelper(NihiDBHelper.class)
//					.getExerciseSummaryDAO();
//			Dao<Route, String> routeDao = getDbHelper().getSectionHelper(NihiDBHelper.class).getRoutesDAO();
//			summary = currentSession.generateSummary(OdinPreferences.UserID.getLongValue(this, -1L),
//					NihiPreferences.Name.getStringValue(this, "Unknown"), summaryDao, routeDao);
//		} catch (SQLException e) {
//			// Won't happen, we can ignore this.
//			logger.error("endWorkout(): " + e.getMessage(), e);
//			//Logger.flushLogOnShutdown();
//			throw new RuntimeException(e);
//		}

		//	TODO: REMOVE ODIN CONNECTION - SEND SESSION SUMMARY TO ODIN
		// Notify Odin that we're done.
//		OdinHelper.sendEndSessionPacket(getOdinService(), summary);

		// Create a new session object in the "non-monitoring" state. Notify listeners.
		currentSession = new ExerciseSessionData(this, null, null, false);
		uiHandler.obtainMessage(MSG_SESSION_CHANGE, currentSession).sendToTarget();
		uiHandler.obtainMessage(MSG_STATUS_CHANGE, WorkoutServiceStatusChangeType.MonitoringStatus).sendToTarget();

		// Return the summary.
		return summary;
	}

	/**
	 * If we're monitoring, this adds a new symptom to the monitoring session and sends it to Odin. If not, throws an
	 * exception.
	 */
	@Override
	public void reportSymptom(Symptom symptom, SymptomStrength strength) {

		// Verify we're monitoring
		if (!currentSession.isMonitoring()) {
			throw new UnsupportedOperationException("Can't report symptoms when there's no session!");
		}

		// Add symptom to session data
		SymptomEntry entry = currentSession.addSymptomEntry(symptom, strength);

		//	TODO: REMOVE ODIN CONNECTION
		// Notify Odin
//		OdinHelper.sendSymptomData(getOdinService(), entry, /* currentSession.getStartTimeInMillis(), */
//				currentSession.getCachedECGData());
	}

	/**
	 * If we're monitoring, this replies to a previously answered question. If not, throws an exception.
	 */
	@Override
	public void replyToQuestion(AnswerData answer) {

		// Verify we're monitoring
		if (!currentSession.isMonitoring()) {
			throw new UnsupportedOperationException("Can't reply to questions when there's no session!");
		}

		//	TODO: REMOVE ODIN CONNECTION - SEND ANSWER TO ODIN
		// Notify Odin
//		OdinHelper.sendAnswer(getOdinService(), answer);

	}

	// ************************************************************************************************************

	// ************************************************************************************************************
	// ExerciseSessionGoalListener implementation
	// ************************************************************************************************************
	@Override
	public void goalReached(ExerciseSessionData session) {
		if (session == currentSession) {

			// We only want this to be called the first time
			session.removeGoalListener(this);
			ExerciseSessionGoal goal = session.getGoal();

			// Build and display notification to user.
			String tickerText = getString(string.workoutservice_notification_goalreached);
			String title = getString(string.workoutservice_notification_goalreached_title);

			// Build text do display in message body on dropdown thingy
			long millis = session.getElapsedTimeInMillis();
			String relativeTimeString = AndroidTextUtils.getRelativeTimeStringSeconds((int) (millis / 1000));

			String messageBody = getString(string.workoutservice_notification_reachedgoalin) + relativeTimeString;

			// Show notification in app bar. Play a default sound if TTS is not enabled.
			Uri soundUri = ttsEnabled ? null : RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
			NotificationUtils.showNotification(this, goal.getIconId(), tickerText, title, messageBody,
					NOTIFICATION_REACH_GOAL, 0, null, 0x00000000, 0, soundUri);

			// If TTS is enabled, build a more friendly version of the message body to actually speak, then do so.
			if (ttsEnabled) {
				int seconds = (int) (millis / 1000);
				int hours = seconds / 3600;
				seconds -= (hours * 3600);
				int minutes = seconds / 60;
				seconds -= (minutes * 60);

				String utterance = getString(string.workoutservice_utterance_reachedgoal);
				String utteranceHours = getString(string.workoutservice_utterance_hours);
				String utteranceMinutes = getString(string.workoutservice_utterance_minutes);
				String utteranceSeconds = getString(string.workoutservice_utterance_seconds);

				if (hours > 0) {
					utterance += hours + utteranceHours;
				}

				utterance += minutes + utteranceMinutes + seconds + utteranceSeconds;

				speak(utterance);
			}

		} else {
			logger.warn("goalReached(): Called for unexpected session!");
		}
	}

	// ************************************************************************************************************

	// ************************************************************************************************************
	// Tray notifications
	// ************************************************************************************************************
	/**
	 * Shows a notification in the taskbar about the given notification.
	 * 
	 * @param notification
	 */
	private void showNotificationTrayIcon(ExerciseNotification notification) {

		// Build text to display in app bar
		String tickerText = getString(string.workoutservice_notification_receivedmessage);
		tickerText += " '" + notification.getNotification() + "'";
		if (tickerText.length() > 40) {
			tickerText = tickerText.substring(0, 35) + " ...'";
		}
		String title = getString(string.workoutservice_notification_receivedmessage_title);

		// Build text do display in message body on dropdown thingy
		String messageBody = AndroidTextUtils.getRelativeTimeStringSeconds(notification.getRelativeTimeInSeconds())
				+ ": " + notification.getNotification();

		// Show notification in app bar. Play a default sound if TTS isn't enabled, otherwise speak the notification.
		Uri soundUri = ttsEnabled ? null : RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		NotificationUtils.showNotification(this, R.drawable.envelope, tickerText, title, messageBody,
				NOTIFICATION_RECV_MSG, 0, NotificationViewerActivity.class, 0x00000000, 0, soundUri);

		// Speak the notification if TTS is enabled.
		speak(notification.getNotification());
	}

	/**
	 * Clears any ExerciseNotification icons in the taskbar.
	 */
	@Override
	public void clearNotificationTrayIcon() {
		NotificationUtils.clearNotification(this, NOTIFICATION_RECV_MSG);
	}

	// ************************************************************************************************************

	// ************************************************************************************************************
	// Odin connection
	// ************************************************************************************************************

	/**
	 * When we receive a message from Odin, if it is a message from the doctor, report it to the user and add it to the
	 * session.
	 */
	//	TODO: REMOVE ODIN CONNECTION
//	@Override
//	public void asyncMessageReceived(Message asyncMessage) throws Exception {
//
//		// If the incoming async message is a question from the doctor to the participant, grab it and forward it to the
//		// listener on the UI thread.
//		QuestionData question = OdinHelper.getQuestionFromDoctor(asyncMessage);
//		if (question != null) {
//			uiHandler.obtainMessage(MSG_QUESTION, question).sendToTarget();
//		}
//
//		// DEPRECATED - Messages are now received as a Request, rather than an Async Message - so we can return a
//		// response indicating that the notification has arrived at the device.
//
//		// MessageData message = OdinHelper.getMessageFromDoctor(asyncMessage);
//		// if (message != null) {
//		//
//		// // If we receive a message outside of monitoring, warn in the logs but show it anyway.
//		// if (!currentSession.isMonitoring()) {
//		// logger.warn("asyncMessageReceived(): Received a message outside of monitoring!");
//		// }
//		//
//		// logger.info("asyncMessageReceived(): Received message from doctor: " + message.getMessage());
//		//
//		// // Add to the current exercise session for storage.
//		// ExerciseNotification notification = currentSession.addNotification(message.getMessage());
//		//
//		// showNotificationTrayIcon(notification);
//		// }
//
//	}

	/**
	 * When we receive a request from Odin, if it is a request for ECG data, return the latest data available.
	 */
	//	TODO: REMOVE ODIN CONNECTION
//	@Override
//	public Message requestReceived(Message request) throws Exception {
//
//		if (request instanceof BasicStringMessage) {
//			String strRequest = ((BasicStringMessage) request).getString();
//			if (strRequest.equals("ECG")) {
//
//				logger.info("requestReceived(): Received a request for ECG data.");
//				ECGData data = currentSession.getCachedECGData();
//				Object[] processedData = OdinHelper.generateECGSampleArrays(data);
//
//				ECGDataResponse response = new ECGDataResponse();
//				response.setEcgData((int[]) processedData[0]);
//				response.setEcgTimeOffsets((long[]) processedData[1]);
//				response.setEcgStartTimeAsLong(data.getStartTimestamp());
//
//				return new JsonMessage(response);
//
//			}
//		}
//
//		else if (request instanceof JsonMessage) {
//
//			MessageData message = OdinHelper.getMessageFromDoctor(request);
//			if (message != null) {
//
//				// If we receive a message outside of monitoring, warn in the logs but show it anyway.
//				if (!currentSession.isMonitoring()) {
//					logger.warn("asyncMessageReceived(): Received a message outside of monitoring!");
//				}
//
//				logger.info("asyncMessageReceived(): Received message from doctor: " + message.getMessage());
//
//				// Add to the current exercise session for storage.
//				ExerciseNotification notification = currentSession.addNotification(message.getMessage());
//
//				showNotificationTrayIcon(notification);
//
//				return new BasicStringMessage("OK");
//			}
//
//		}
//
//		throw new UnsupportedOperationException("unknown request type or invalid content.");
//	}

	/**
	 * When a connection to the {@link IOdinService} is obtained, notify listeners of this fact. Additionally, begin
	 * attempting to connect if we're not already connecting / connected.
	 */
	//	TODO: REMOVE ODIN CONNECTION
//	@Override
//	protected void onOdinServiceConnectionObtained(IOdinService odinService) {
//
//		// If we're already connected or connecting, we're not going to start connecting here. So we'll need to notify
//		// listeners of the status change here.
//		if (odinService.getConnectionStatus() != EndpointConnectionStatus.DISCONNECTED) {
//
//			// If we're already connecting, set that flag
//			connectingToOdin = (odinService.getConnectionStatus() == EndpointConnectionStatus.ATTEMPTING_CONNECT);
//
//			uiHandler.obtainMessage(MSG_STATUS_CHANGE, WorkoutServiceStatusChangeType.OdinStatus).sendToTarget();
//		}
//
//		// If we're disconnected, then we'll attempt to connect. Doing this will be enough to eventually notify the
//		// listeners so we don't need to send an additional notification.
//		else {
//			connectingToOdin = true;
//			this.beginOdinConnect();
//		}
//	}

	/**
	 * When our Odin connection status changes, we'll notify listeners of this change. Additionally, if we just became
	 * Disconnected while we were trying to connect originally, notify users of an error.
	 */
	//	TODO: REMOVE ODIN CONNECTION
//	@Override
//	protected void onOdinConnectionStatusChanged(EndpointConnectionStatus status) {
//		uiHandler.obtainMessage(MSG_STATUS_CHANGE, WorkoutServiceStatusChangeType.OdinStatus).sendToTarget();
//
//		// If we disconnected while trying to connect, notify the user of an error.
//		if (connectingToOdin && status == EndpointConnectionStatus.DISCONNECTED) {
//			uiHandler.obtainMessage(MSG_ERROR, WorkoutServiceConnectionErrorType.OdinError).sendToTarget();
//		}
//
//		// Clear the "connecting" flag if we're not connecting.
//		if (status != EndpointConnectionStatus.ATTEMPTING_CONNECT) {
//			connectingToOdin = false;
//		}
//	}

	// ************************************************************************************************************

	// ************************************************************************************************************
	// Bioharness connection
	// ************************************************************************************************************
	/**
	 * Handles connection to the Bioharness service. When connected to the Bioharness service, we will immediately begin
	 * trying to connect over Bluetooth, assuming Bluetooth is available, we have a valid Bioharness address, and the
	 * Bioharness service isn't already trying to connect or is connected.
	 */
	private final ServiceConnection bioharnessConn = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			bioharnessService = null;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			bioharnessService = (LocalBinder<IBioHarnessService>) service;
			bioharnessService.getService().setBioHarnessServiceListener(WorkoutService.this);

			// If we're not Disconnected, we'll send a status update to any listeners.
			if (bioharnessService.getService().getStatus() != BHConnectivityStatus.DISCONNECTED) {
				uiHandler.obtainMessage(MSG_STATUS_CHANGE, WorkoutServiceStatusChangeType.BioharnessStatus)
						.sendToTarget();
			}

			// If we're already trying to connect here, set the appropriate flag.
			if (bioharnessService.getService().getStatus() == BHConnectivityStatus.CONNECTING) {
				connectingToBioharness = true;
			}

			// Otherwise, if we're disconnected, begin trying to connect if Bluetooth is enabled and we have a valid BT
			// address.
			else if (bioharnessService.getService().getStatus() == BHConnectivityStatus.DISCONNECTED
					&& statusObject.isBluetoothEnabled() && statusObject.hasBioharnessAddress()) {

				connectingToBioharness = true;
				bioharnessService.getService().setNumRetries(NUM_BIOHARNESS_RETRIES_INITIAL_CONNECTION);
				bioharnessService.getService().connectToBioHarnessFromPrefs();
			}

		}
	};

	/**
	 * When we connect to the Bioharness, notify listeners of this.
	 */
	@Override
	public void onConnectedToBioharness() {
		logger.info("WE CONNECTED WITH THE HARNESS");
		connectingToBioharness = false;
		uiHandler.obtainMessage(MSG_STATUS_CHANGE, WorkoutServiceStatusChangeType.BioharnessStatus).sendToTarget();

		// Start monitoring ECG data. HOW THE EFF WAS THIS NOT HERE BEFORE?!
		bioharnessService.getService().setMonitorECGData(true);
	}

	/**
	 * When we are connecting to the Bioharness, notify listeners of this.
	 */
	@Override
	public void onConnectingToBioharness() {
		uiHandler.obtainMessage(MSG_STATUS_CHANGE, WorkoutServiceStatusChangeType.BioharnessStatus).sendToTarget();
	}

	/**
	 * When we are disconnected from the Bioharness, notify listeners of this. Additionally, if we were trying to
	 * connect to the Bioharness, notify listeners of an error.
	 */
	@Override
	public void onDisconnectedFromBioharness() {
		uiHandler.obtainMessage(MSG_STATUS_CHANGE, WorkoutServiceStatusChangeType.BioharnessStatus).sendToTarget();

		// If we were trying to connect to the Bioharness, send an error message to notify the user that they may need
		// to change their settings.
		if (connectingToBioharness) {
			connectingToBioharness = false;
			uiHandler.obtainMessage(MSG_ERROR, WorkoutServiceConnectionErrorType.BioharnessError).sendToTarget();
		}

		// Otherwise, if we're not deliberately trying to disconnect, then retry the connection.
		else if (!disconnectingFromBioharness) {
			retryBioharnessConnection(NUM_BIOHARNESS_RETRIES_KNOWN_ADDRESS);
		}

		disconnectingFromBioharness = false;
	}

	/**
	 * When we receive ECG data, cache it in the current session.
	 */
	@Override
	public void onReceiveECGData(ECGWaveformData ecgData) {
		this.currentSession.cacheECGData(ecgData);
	}

	/**
	 * When we receive Summary data, add it to the current session. Additionally, if in monitoring mode, send it to
	 * Odin.
	 */
	@Override
	public void onReceiveSummaryData(SummaryData newData) {

		// Update resting HR if the recorded value is reliable and less than the user's entered resting HR. Update max
		// HR if the recorded value is reliable and greater than the user's max HR.
		// if (newData.isHeartRateReliable()) {
		// int restHR = NihiPreferences.RestHeartRate.getIntValue(WorkoutService.this, -1);
		// if (restHR < 0 || restHR >= 0 && restHR > newData.getHeartRate()) {
		// NihiPreferences.RestHeartRate.setValue(WorkoutService.this, newData.getHeartRate());
		// }
		// int maxHR = NihiPreferences.MaxHeartRate.getIntValue(WorkoutService.this, -1);
		// if (maxHR < 0 || maxHR >= 0 && maxHR < newData.getHeartRate()) {
		// NihiPreferences.MaxHeartRate.setValue(WorkoutService.this, newData.getHeartRate());
		// }
		// }

		// logger.debug("onReceiveSummaryData(): before addHeartRateData(newData)");

		// Add HR data to session
		this.currentSession.addHeartRateData(newData);

		//TODO: example rule implementation
//		rules.register(exampleRule);
//		logger.info(newData.getHeartRate());
//		facts.put("heartRate", newData.getHeartRate());
//		rulesEngine.fire(rules, facts);

		heartRateRulesUtils.fireTimedHeartrateRules(newData.getHeartRate(), this.currentSession.getElapsedTimeInMillis());
		speedRulesUtils.fireTimedSpeedRules(this.currentSession.getCurrentSpeed(), this.currentSession.getElapsedTimeInMillis());

		// logger.debug("onReceiveSummaryData(): before sendVitalSignData(newData)");

		// If monitoring, send to Odin
		if (this.currentSession.isMonitoring()) {
			int restHR = NihiPreferences.RestHeartRate.getIntValue(WorkoutService.this, -1);
			int maxHR = NihiPreferences.MaxHeartRate.getIntValue(WorkoutService.this, -1);
			//	TODO: REMOVE ODIN CONNECTION - SENDING DATA TO ODIN
//			OdinHelper.sendVitalSignData(getOdinService(), newData, restHR, maxHR,
//					currentSession.getCumulativeTrainingLoad());
		}

		// logger.debug("onReceiveSummaryData(): after sendVitalSignData(newData)");
	}

	// ************************************************************************************************************

	// ************************************************************************************************************
	// GPS Connection
	// ************************************************************************************************************
	/**
	 * Handles connection to the GPS service.
	 */
	private final ServiceConnection gpsConn = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			gpsService = null;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			gpsService = (LocalBinder<IGPSService>) service;
			gpsService.getService().setGPSServiceListener(WorkoutService.this);
		}
	};

	/**
	 * When GPS connectivity changes, notify listeners of this fact.
	 */
	@Override
	public void gpsConnectivityChanged(boolean isConnected) {
		uiHandler.obtainMessage(MSG_STATUS_CHANGE, WorkoutServiceStatusChangeType.GPSStatus).sendToTarget();
	}

	/**
	 * When we receive a new location, notify listeners of this fact. Additionally, add the new location data to the
	 * current session. Finally, if we're in monitoring mode, send the data to Odin.
	 */
	@Override
	public void newLocationReceived(Location location) {

		uiHandler.obtainMessage(MSG_LOCATION_CHANGE, location).sendToTarget();
		currentSession.addGPSData(location);
		if (currentSession.isMonitoring()) {
			// Location clone = location.hasSpeed() ? location : new Location(location);
			// if (clone != location) {
			// clone.setSpeed(currentSession.getCurrentSpeed() * 1000.0f / 3600.0f);
			// }
			//	TODO: REMOVE ODIN CONNECTION - LOCATION CHANGE
//			OdinHelper.sendLocationData(getOdinService(), location, currentSession.getCurrentSpeed(),
//					currentSession.getDistance());
		}
	}

	@Override
	public Location getLastKnownLocation() {
		return gpsService == null ? null : gpsService.getService().getLastKnownLocation();
	}

	// ************************************************************************************************************

	// ************************************************************************************************************
	// Lifecycle
	// ************************************************************************************************************
	/**
	 * When we create this service, connect to the BioHarness, GPS, and Odin services.
	 */
	@Override
	public void onCreate() {
		this.uiHandler = new Handler(uiHandlerCallback);
		this.currentSession = new ExerciseSessionData(this, null, null, false);

		super.onCreate(); // Handles Odin connection

		// If we're NOT in test harness mode, establish a connection to the Bioharness and GPS services.
		if (!TestHarnessUtils.isTestHarness()) {
			bioharnessServiceIntent = new Intent(this, BioHarnessServiceImpl.class);
			gpsServiceIntent = new Intent(this, GPSServiceImpl.class);
		}

		// If we ARE in test harness mode, establish a connection to the dummy Bioharness and GPS serviecs instead.
		else {
			BioharnessDescription dummyDesc = new BioharnessDescription("Dummy",
					DummyBioHarnessServiceImpl.DUMMY_BH_ADDRESS);
			BioharnessPreferences.BioharnessDescription.setValue(this, dummyDesc);
			bioharnessServiceIntent = new Intent(this, DummyBioHarnessServiceImpl.class);
			gpsServiceIntent = new Intent(this, DummyGPSServiceImpl.class);
		}

		// Initialize text-to-speech
		if (!TestHarnessUtils.isTestHarness()) {
			this.tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
				@Override
				public void onInit(int status) {
				}
			});
		}

		heartRateRulesUtils = new RulesUtils(300000, this.tts);
		speedRulesUtils = new RulesUtils(150000, this.tts);

		// Bind to the Odin, Bluetooth and GPS services
		bindService(bioharnessServiceIntent, bioharnessConn, BIND_AUTO_CREATE);
		bindService(gpsServiceIntent, gpsConn, BIND_AUTO_CREATE);
	}

	/**
	 * When someone binds to us, give them something they can use to interact.
	 */
	@Override
	public IBinder onBind(Intent arg0) {
		return new LocalBinder<IWorkoutService>(this);
	}

	/**
	 * When we finish up here, unbind from the Bioharness and GPS Make sure we're disconnected first.
	 */
	@Override
	public void onDestroy() {
		//	TODO: REMOVE ODIN CONNECTION - DISCONNECT FROM ODIN
		// Disconnect from Odin if we need to.
		// TODO Really should refactor this so we can actually STOP the odin service.
//		if (getOdinService() != null) {
//			// getOdinService().disconnect();
//			getOdinService().scheduleDisconnect();
//		}

		if (bioharnessService != null) {
			disconnectingFromBioharness = true;
			bioharnessService.getService().setBioHarnessServiceListener(null);
			bioharnessService.getService().disconnectBioHarness();
			bioharnessService.close();
			unbindService(bioharnessConn);
		}

		if (gpsService != null) {
			gpsService.getService().setGPSServiceListener(null);
			// gpsService.getService().stopGPSMonitoring();
			// gpsService.close();
			unbindService(gpsConn);
		}

		if (tts != null) {
			tts.stop();
			tts.shutdown();
		}

		super.onDestroy();
	}

	// ************************************************************************************************************

	// ************************************************************************************************************
	// Status object
	// ************************************************************************************************************
	public final WorkoutServiceStatus statusObject = new WorkoutServiceStatus() {

		@Override
		public boolean isMonitoring() {
			return currentSession == null ? false : currentSession.isMonitoring();
		}

		@Override
		public boolean isConnectedToGPS() {
			return gpsService == null ? false : gpsService.getService().isConnected();
		}

		@Override
		public boolean isBluetoothEnabled() {
			return TestHarnessUtils.isTestHarness() ? true : BluetoothAdapter.getDefaultAdapter().isEnabled();
		}

		@Override
		public boolean hasBioharnessAddress() {
			return TestHarnessUtils.isTestHarness() ? true : BioharnessPreferences.BioharnessDescription.getValue(
					WorkoutService.this, BioharnessDescription.class, null) != null;
		}

		//	TODO: REMOVE ODIN CONNECTION - GET ODIN CONNECTIVITY STATUS
//		@Override
//		public EndpointConnectionStatus getOdinConnectivityStatus() {
//			return getOdinService() == null ? EndpointConnectionStatus.DISCONNECTED : getOdinService()
//					.getConnectionStatus();
//		}

		@Override
		public BHConnectivityStatus getBioharnessConnectivityStatus() {
			return bioharnessService == null ? BHConnectivityStatus.DISCONNECTED : bioharnessService.getService()
					.getStatus();
		}

		/**
		 * A convenience method. If this returns true it means that: we're not monitoring and we are connected to GPS,
		 * Odin, and Bioharness.
		 */
		@Override
		public boolean canStartNewWorkout() {
			return !isMonitoring() && isConnectedToGPS()
					&& getBioharnessConnectivityStatus() == BHConnectivityStatus.CONNECTED;
			//	TODO: REMOVE ODIN CONNECTION
//					&& getOdinConnectivityStatus() == EndpointConnectionStatus.CONNECTED;
		}
	};

	// ************************************************************************************************************

	// ************************************************************************************************************
	// Handler that will delegate to the UI thread
	// ************************************************************************************************************

	// Handled message types
	private static final int MSG_STATUS_CHANGE = 100, MSG_SESSION_CHANGE = 101, MSG_LOCATION_CHANGE = 102,
			MSG_ERROR = 103, MSG_QUESTION = 104;

	private final Handler.Callback uiHandlerCallback = new Handler.Callback() {
		@Override
		public boolean handleMessage(android.os.Message msg) {
			if (listener != null) {
				switch (msg.what) {
				case MSG_STATUS_CHANGE:
					listener.workoutServiceStatusChanged((WorkoutServiceStatusChangeType) msg.obj, statusObject);
					return true;

				case MSG_SESSION_CHANGE:
					listener.sessionChanged((ExerciseSessionData) msg.obj);
					return true;

				case MSG_LOCATION_CHANGE:
					listener.locationChanged((Location) msg.obj);
					return true;

				case MSG_ERROR:
					listener.serviceConnectionError((WorkoutServiceConnectionErrorType) msg.obj);
					return true;

				case MSG_QUESTION:
					listener.questionReceivedFromDoctor((QuestionData) msg.obj);
					return true;

				default:
					return false;
				}
			} else {
				return false;
			}
		}
	};

	// ************************************************************************************************************

	// ************************************************************************************************************
	// Text-to-speech
	// ************************************************************************************************************

	/**
	 * If the TTS system is enabled, speaks the given text.
	 * 
	 * @param text
	 */
	private void speak(String text) {
			this.tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
	}

	private final TextToSpeech.OnInitListener ttsOnInitListener = new TextToSpeech.OnInitListener() {

		@Override
		public void onInit(int status) {
			if (status == TextToSpeech.SUCCESS) {

				// TODO GERMAN!!!!!
				int result = tts.setLanguage(Locale.UK);

				if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
					logger.error("ttsOnInitListener.onInit(): This Language is not supported");
				} else {
					ttsEnabled = true;
					logger.info("ttsOnInitListener.onInit(): Text-to-speech enabled!");
				}

			} else {
				logger.error("ttsOnInitListener.onInit(): text-to-speech initialization failed.");
			}
		}
	};

	// ************************************************************************************************************

	// ************************************************************************************************************
	// Database
	// ************************************************************************************************************
	/**
	 * Lazily creates the {@link #dbHelper} if required, then returns it.
	 * 
	 * @return
	 */
	private LocalDatabaseHelper getDbHelper() {
		if (dbHelper == null) {
			dbHelper = DatabaseManager.getInstance().getDatabaseHelper(this);
		}
		return dbHelper;
	}
	public static String getFeedback(){
		return feedback;
	}
	public static void setFeedback(String f){
		feedback = f;
	}

	// ************************************************************************************************************

	// ************************************************************************************************************
	// Unused methods
	// ************************************************************************************************************
	// from bioharness listener
	@Override
	public void onDeviceConnected(String deviceName) {
	}

	// from bioharness listener
	@Override
	public void onReceivedMessage(String message) {
	}

	// from bioharness listener
	@Override
	public void onReceiveGeneralData(GeneralData data) {
	}
	// ************************************************************************************************************

}
