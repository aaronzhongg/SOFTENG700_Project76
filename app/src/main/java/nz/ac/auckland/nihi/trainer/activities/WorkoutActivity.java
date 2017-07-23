package nz.ac.auckland.nihi.trainer.activities;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Random;

import nz.ac.auckland.cs.odin.android.api.services.testharness.TestHarnessService;
import nz.ac.auckland.cs.odin.android.api.services.testharness.TestHarnessUIControllerCommandReceiver;
import nz.ac.auckland.cs.odin.android.api.services.testharness.TestHarnessUtils;
import nz.ac.auckland.cs.odin.interconnect.common.EndpointConnectionStatus;
import nz.ac.auckland.nihi.trainer.R.anim;
import nz.ac.auckland.nihi.trainer.R.drawable;
import nz.ac.auckland.nihi.trainer.R.id;
import nz.ac.auckland.nihi.trainer.R.layout;
import nz.ac.auckland.nihi.trainer.R.string;
import nz.ac.auckland.nihi.trainer.data.DatabaseHelper;
import nz.ac.auckland.nihi.trainer.data.ExerciseSummary;
import nz.ac.auckland.nihi.trainer.data.Route;
import nz.ac.auckland.nihi.trainer.data.RouteCoordinate;
import nz.ac.auckland.nihi.trainer.data.Symptom;
import nz.ac.auckland.nihi.trainer.data.SymptomStrength;
import nz.ac.auckland.nihi.trainer.data.session.ExerciseSessionData;
import nz.ac.auckland.nihi.trainer.data.session.ExerciseSessionGoal;
import nz.ac.auckland.nihi.trainer.data.session.FreeWorkoutGoal;
import nz.ac.auckland.nihi.trainer.data.session.RouteGoal;
import nz.ac.auckland.nihi.trainer.fragments.ConnectionProgressDialogFragment;
import nz.ac.auckland.nihi.trainer.fragments.WorkoutScreenStatsViewFragment;
import nz.ac.auckland.nihi.trainer.fragments.WorkoutScreenStatsViewFragment.StatsViewButtonClickListener;
import nz.ac.auckland.nihi.trainer.messaging.AnswerData;
import nz.ac.auckland.nihi.trainer.messaging.QuestionData;
import nz.ac.auckland.nihi.trainer.services.workout.IWorkoutService;
import nz.ac.auckland.nihi.trainer.services.workout.WorkoutService;
import nz.ac.auckland.nihi.trainer.services.workout.WorkoutServiceListener;
import nz.ac.auckland.nihi.trainer.services.workout.WorkoutServiceStatus;
import nz.ac.auckland.nihi.trainer.util.AndroidTextUtils;
import nz.ac.auckland.nihi.trainer.util.LocationUtils;
import nz.ac.auckland.nihi.trainer.views.AbstractWorkoutStatTileView.ViewedStat;

import org.apache.log4j.Logger;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.odin.android.bioharness.BHConnectivityStatus;
import com.odin.android.bioharness.prefs.BioharnessDescription;
import com.odin.android.bioharness.prefs.BioharnessPreferences;
import com.odin.android.services.LocalBinder;

public class WorkoutActivity extends FragmentActivity implements WorkoutServiceListener, StatsViewButtonClickListener {

	private static final Logger logger = Logger.getLogger(WorkoutActivity.class);

	// ************************************************************************************************************
	// Instance variables
	// ************************************************************************************************************

	private static final int REQUEST_ENABLE_BT = 1;
	private static final int REQUEST_DISCOVER_DEVICE = 2;
	private static final int REQUEST_TILE_TYPE = 3;
	private static final int REQUEST_GOAL_TYPE = 4;
	private static final int REQUEST_SELECT_SYMPTOM = 5;
	private static final int MESSAGE_UPDATE_TIMER = 1;

	// UI Elements
	private View pnlMap;
	private Button btnStartWorkout, btnStopWorkout, btnSelectWorkout, btnSymptoms, btnNotifications;
	private SupportMapFragment mapFragment;
	private WorkoutScreenStatsViewFragment largeStatsFragment, smallStatsFragment;
	private ToggleButton btnToggleMap;
	private ConnectionProgressDialogFragment connectionDialog;

	// A value indicating if we're currently showing the map.
	private boolean isShowingMap = false;

	// The intent used to start / stop the Workout service.
	private Intent workoutServiceIntent;

	// The service that acts as the logic layer for a workout session.
	private LocalBinder<IWorkoutService> workoutService;

	// The intent to start the "enable Bluetooth" activity
	private Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

	// The goal that will be targeted by a particular exercise session, if any.
	private ExerciseSessionGoal goal;

	// Allows the test harness to interact with this activity.
	private TestHarnessUIControllerCommandReceiver testHarnessUIController;

	// The thread that periodically sends timer update messages to the UI.
	private Thread timerUpdateThread;

	// The handler that processes timer update messages on the UI thread.
	private Handler timerUpdateHandler;

	// The database helper, required to load Route data.
	private DatabaseHelper dbHelper;

	// The map marker that shows the user's current location.
	private Marker userMapMarker;

	// The map line that shows the current route, if any.
	private Polyline routePolyline;

	// If true, this will show the connection dialog on resume.
	private boolean showConnectionDialogOnResume = false;

	// If true, this will dismiss the connection dialog on resume.
	private boolean dismissConnectionDialogOnResume = false;

	// Determines whether we can see the activity.
	private boolean activityVisible = true;

	//Determine whether the navigation tts should be enabled
	private boolean isPaused = true;

	// The map.
	private GoogleMap theMap;

	//Current route
	private Route currentRoute;

	//The next point that the user needs to navigate to
	private LinkedList<RouteCoordinate> remainingDestinations;

	//TTS

	// ************************************************************************************************************

	/**
	 * Lazily creates the {@link #dbHelper} if required, then returns it.
	 * 
	 * @return
	 */
	private DatabaseHelper getDbHelper() {
		if (dbHelper == null) {
			dbHelper = OpenHelperManager.getHelper(this, DatabaseHelper.class);		}
		return dbHelper;
	}

	// ************************************************************************************************************
	// Lifecycle
	// ************************************************************************************************************
	/**
	 * If Bluetooth is enabled, start the Workout service. Otherwise, start the "Enable Bluetooth" activity.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		timerUpdateHandler = new Handler(this.timerUpdateHandlerCallback);

		logger.info("testing");
		// Create the user interface and set its inital properties.
		buildUI();
		setUIState(null);

		//enable tts
//		this.tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
//			@Override
//			public void onInit(int status) {
//			}
//		});


		// If Bluetooth is enabled, or if this is the test harness, start the workout service.
		if (TestHarnessUtils.isTestHarness() || BluetoothAdapter.getDefaultAdapter().isEnabled()) {
			// Bind to the Workout service
			workoutServiceIntent = new Intent(this, WorkoutService.class);
			startService(workoutServiceIntent);
			bindService(workoutServiceIntent, workoutServiceConn, BIND_AUTO_CREATE);
		}

		// If not, start the activity to enable Bluetooth.
		else {
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}

		// If using the test harness, create the receiver. Don't hook it up just yet though.
		if (TestHarnessUtils.isTestHarness()) {
			testHarnessUIController = new TestHarnessUIControllerCommandReceiver(this);
		}

	}

	/**
	 * When the activity is destroyed, unbind from the workout service.
	 */
	@Override
	protected void onDestroy() {

		// Force-stop the timer if required.
		if (timerUpdateThread != null) {
			timerUpdateThread.interrupt();
			timerUpdateThread = null;
		}

		// if we're in the middle of a session, stop it gracefully.
		// TODO Maybe refactor so we don't stop the session here.
		if (workoutService != null) {
			workoutService.getService().setWorkoutServiceListener(null);

			if (workoutService.getService().getStatus().isMonitoring()) {
				workoutService.getService().endWorkout();
				logger.debug("onDestroy(): Stopped an in-progress session before quitting activity.");
			}
		}

		// Unbind from, and stop, the workout service here.
		// TODO Maybe don't stop the service.
		if (workoutServiceIntent != null) {
			unbindService(workoutServiceConn);
			stopService(workoutServiceIntent);
			logger.debug("onDestroy(): Unbound from Workout service.");
		}

		super.onDestroy();
	}

	/**
	 * When this activity is resumed, register this activity as the activity that receives test harness commands.
	 */
	@Override
	protected void onResume() {

		activityVisible = true;

		if (TestHarnessUtils.isTestHarness()) {
			LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(testHarnessUIController,
					TestHarnessService.IF_PROCESS_COMMAND);
		}
		super.onResume();

		if (showConnectionDialogOnResume) {
			showConnectionDialogOnResume = false;
			if (this.connectionDialog != null && !this.connectionDialog.isVisible()) {
				connectionDialog.show(getSupportFragmentManager(), "ConnectionDialog");
			}
		}

		if (dismissConnectionDialogOnResume) {
			dismissConnectionDialogOnResume = false;
			if (this.connectionDialog != null && this.connectionDialog.isVisible()) {
				connectionDialog.dismiss();
			}
			connectionDialog = null;
		}
	}

	/**
	 * When this activity is paused, unregister from the test harness service so we no longer receive commands.
	 */
	@Override
	protected void onPause() {

		activityVisible = false;

		if (TestHarnessUtils.isTestHarness()) {
			LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(testHarnessUIController);
		}
		super.onPause();
	}

	/**
	 * Called when an Activity gives us a result. Delegates the result code and data (if required) to an appropriate
	 * method based on the request code.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		// If REQUEST_ENABLE_BT, then this result is from the
		// activity that allows the user to enable Bluetooth.
		if (requestCode == REQUEST_ENABLE_BT) {
			logger.debug("onActivityResult(): Received response for REQUEST_ENABLE_BT");
			onEnableBluetoothResponse(resultCode);
		}

		// If REQUEST_DISCOVER_DEVICE, then this result is from the activity that allows
		// the user to select a Bluetooth device to which to connect.
		else if (requestCode == REQUEST_DISCOVER_DEVICE) {
			logger.debug("onActivityResult(): Received response for REQUEST_DISCOVER_DEVICE");
			onDiscoverDeviceResponse(resultCode, data);
		}

		// If REQUEST_TILE_TYPE, then this result is from the activity that allows the user to change the type of a
		// tile.
		else if (requestCode == REQUEST_TILE_TYPE) {
			logger.debug("onActivityResult(): Received response for REQUEST_TILE_TYPE");
			onChangeTileResponse(resultCode, data);
		}

		// If REQUEST_GOAL_TYPE, then this result is from the activity that allows the user to change the goal type.
		else if (requestCode == REQUEST_GOAL_TYPE) {
			logger.debug("onActivityResult(): Received response for REQUEST_GOAL_TYPE");
			onChangeGoalResponse(resultCode, data);
		}

		// If REQUEST_SELECT_SYMPTOM, then this result is from the activity that allows the user to add a new symptom.
		else if (requestCode == REQUEST_SELECT_SYMPTOM) {
			logger.debug("onActivityResult(): Received response for REQUEST_SELECT_SYMPTOM");
			onSelectSymptomResponse(resultCode, data);
		}
	}

	// ************************************************************************************************************

	// ************************************************************************************************************
	// Responses from Child activities
	// ************************************************************************************************************

	/**
	 * Called when an activity returns from allowing the user to select a new symptom. If they did, report the new
	 * symptom to the service.
	 */
	private void onSelectSymptomResponse(int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			Symptom symptom = (Symptom) data.getSerializableExtra(SymptomPickerActivity.EXTRA_SYMPTOM);
			SymptomStrength strength = (SymptomStrength) data
					.getSerializableExtra(SymptomPickerActivity.EXTRA_SYMPTOM_STRENGTH);

			workoutService.getService().reportSymptom(symptom, strength);
		}
	}

	/**
	 * Called when an activity returns from allowing the user to change the goal type. If the user selected a new goal
	 * type, update the goal type. and UI to match.
	 */
	private void onChangeGoalResponse(int resultCode, Intent data) {

		if (resultCode == RESULT_OK) {
			ExerciseSessionGoal goal = (ExerciseSessionGoal) data
					.getSerializableExtra(ExerciseSessionGoalSelectionActivity.EXTRA_GOAL);

			if (goal == null) {
				goal = new FreeWorkoutGoal();
			}

			changeGoal(goal);
		}
	}

	/**
	 * Called when an activity returns from allowing the user to select a new tile. If the user chose a valid tile,
	 * changes the corresponding tile.
	 */
	private void onChangeTileResponse(int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			int tileId = data.getIntExtra(TileTypeSelectorActivity.EXTRA_TILE_ID, 0);
			int fragmentId = data.getIntExtra(TileTypeSelectorActivity.EXTRA_FRAGMENT_ID, 0);
			ViewedStat tileType = (ViewedStat) data.getSerializableExtra(TileTypeSelectorActivity.EXTRA_TILE_TYPE);
			if (tileId != 0 && fragmentId != 0 && tileType != null) {
				WorkoutScreenStatsViewFragment frag = (largeStatsFragment.getId() == fragmentId) ? largeStatsFragment
						: smallStatsFragment;
				frag.setViewedStat(tileId, tileType);
			}

			logger.debug("onChangeTileResponse(): Changed tile type [type = " + tileType + ", tileId = " + tileId + "]");
		}
	}

	/**
	 * This method is called upon returning from the "enable bluetooth" activity. If the user enabled Bluetooth, and we
	 * need to start the Workout service, we will do this here. Otherwise, if the user did NOT enable Bluetooth, we will
	 * quit as Bluetooth is required.
	 */
	private void onEnableBluetoothResponse(int resultCode) {
		if (resultCode == RESULT_OK) {
			logger.debug("onEnableBluetoothResponse(): Bluetooth enabled. Starting Workout Service.");
			workoutServiceIntent = new Intent(this, WorkoutService.class);
			startService(workoutServiceIntent);
			bindService(workoutServiceIntent, workoutServiceConn, BIND_AUTO_CREATE);
		}

		else {
			logger.warn("onEnableBluetoothResponse(): User elected not to enable Bluetooth. Activity will now finish.");
			finish();
		}
	}

	/**
	 * This method is called upon returning from the "discover device" activity. If the user selected a new Bluetooth
	 * device, we will save its address to the preferences and resume (or start) trying to connect to the Bioharness. If
	 * not, we will quit, because a valid address must be chosen.
	 * 
	 * @param resultCode
	 * @param data
	 */
	private void onDiscoverDeviceResponse(int resultCode, Intent data) {

		if (resultCode == RESULT_OK) {
			String address = data.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
			String name = data.getStringExtra(DeviceListActivity.EXTRA_DEVICE_NAME);
			BioharnessDescription desc = new BioharnessDescription(name, address);
			BioharnessPreferences.BioharnessDescription.setValue(this, desc);
			logger.debug("onDiscoverDeviceResponse(): User selected a new Bluetooth device. Will notify service of this fact.");
			workoutService.getService().retryBioharnessConnection();
		}

		else {
			logger.warn("onDiscoverDeviceResponse(): User elected not to select a Bluetooth device. Activity will now finish.");
			finish();
		}
	}

	// ************************************************************************************************************

	// ************************************************************************************************************
	// Application methods (called in response to user interactions)
	// ************************************************************************************************************

	/**
	 * Changes the goal to the given goal. Then, if the goal is a route goal, displays that route on the map. Otherwise,
	 * clears the route from the map.
	 * 
	 * @param newGoal
	 */
	private void changeGoal(ExerciseSessionGoal newGoal) {

		if (newGoal == null) {
			throw new RuntimeException("New goal cannot be null.");
		}

		if (workoutService != null && workoutService.getService().getStatus().isMonitoring()) {
			throw new RuntimeException("Cannot change goal while running a session.");
		}

		this.goal = newGoal;
		btnSelectWorkout.setText(goal.getName(this));
		logger.debug("changeGoal(): Changed goal [goal = " + goal + "]");

		if (newGoal instanceof RouteGoal) {
			showRoutePolyline(((RouteGoal) newGoal).getRouteId());
			setMapMode(true);
		} else {
			clearRoutePolyline();
		}

	}

	/**
	 * Called when the user manually stops the workout. Prompts them to stop or continue. If they select "continue",
	 * switches the button back to stopwatch mode and then exits. If they select "stop", perform cleanup then take the
	 * user to the exercise summary screen.
	 */
	private void promptStopWorkout(final boolean finishOnStop) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(string.workoutscreen_dialog_stopworkout)
				.setMessage(string.workoutscreen_dialog_stopworkout_message)
				.setPositiveButton(string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						stopWorkout(finishOnStop);
					}
				}).setNegativeButton(string.workoutscreen_dialog_stopworkout_nokeepgoing, null).show();
	}

	/**
	 * Stops the workout.
	 */
	public void stopWorkout(final boolean finishOnStop) {

		// Stop the stopwatch
		timerUpdateThread.interrupt();
		try {
			timerUpdateThread.join();
		} catch (InterruptedException e) {
		}

		// Notify the service that the workout should cease
		ExerciseSummary summary = workoutService.getService().endWorkout();
		//Back to route select for now
		finish();
		Intent routeIntent = new Intent(getApplicationContext(), RoutesActivity.class);
		if (routeIntent != null) {
			startActivity(routeIntent);
			overridePendingTransition(anim.push_left_in, anim.push_left_out);
		}
		// TODO: Implement this bit with our app -> Display workout summary activity or finish activity, depending on value of variable.
//		if (finishOnStop) {
//			finish();
//			overridePendingTransition(anim.push_right_in, anim.push_right_out);
//		} else {
//			Intent summaryIntent = new Intent(this, ReviewActivity.class);
//			summaryIntent.putExtra(ReviewActivity.EXTRA_SUMMARY_ID, summary.getId());
//			startActivity(summaryIntent);
//		}
	}

	// ************************************************************************************************************

	// ************************************************************************************************************
	// Workout Service interaction
	// ************************************************************************************************************

	/**
	 * The {@link ServiceConnection} that allows us to bind to the {@link WorkoutService Workout} service. When a
	 * service connection is obtained, obtain a reference to the service interface and set its listener to our internal
	 *  WorkoutActivity#workoutServiceListener} object. Then, hookup the UI to the service's sesison object, and
	 * set the state of the UI to match.
	 */
	private final ServiceConnection workoutServiceConn = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
		}

		@SuppressWarnings("unchecked")
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {

			// Get a reference to the service
			workoutService = (LocalBinder<IWorkoutService>) service;
			workoutService.getService().setWorkoutServiceListener(WorkoutActivity.this);

			// Hook up the tiles
			largeStatsFragment.setExerciseSession(workoutService.getService().getCurrentSession());
			smallStatsFragment.setExerciseSession(workoutService.getService().getCurrentSession());

			// Set the UI state based on the service's current state.
			setUIState(workoutService.getService().getStatus());

			// If there's no valid Bioharness address, start the activity to choose one.
			if (!workoutService.getService().getStatus().hasBioharnessAddress()) {
				startActivityForResult(new Intent(WorkoutActivity.this, DeviceListActivity.class),
						REQUEST_DISCOVER_DEVICE);
			}

			// Otherwise, set the initial goal, if any.
			else {

				// Set the initial goal of this screen, to be either a free workout or a route goal if one was provided
				// in the intent which started this activity.
				String routeId = getIntent().getStringExtra(RoutesActivity.EXTRA_ROUTE_ID);
				if (routeId == null) {
					changeGoal(new FreeWorkoutGoal());
				} else {
					changeGoal(new RouteGoal(routeId));
					try {
						currentRoute = dbHelper.getRoutesDAO().queryForId(routeId);
						remainingDestinations = new LinkedList<RouteCoordinate>(currentRoute.getGpsCoordinates());
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}

			}
		}
	};

	/**
	 * When the workout service reports a location change, update the map view.
	 */
	@Override
	public void locationChanged(Location newLocation) {
		showOnMap(newLocation);
		giveNavigationInstructions(newLocation);
	}

	private void giveNavigationInstructions(Location location){
		//if no more destinations left do not try to fetch destination
		//if the user has not started the workout or paused it, do not try to tts
		if(remainingDestinations.size() < 1 || this.isPaused){
			return;
		}
		RouteCoordinate currentDestination = remainingDestinations.getFirst();
		//Check if within a certain radius of this destination
		double distance = LocationUtils.distanceBetweenCoordinates(location.getLatitude(), currentDestination.getLatitude(),
				location.getLongitude(), currentDestination.getLongitude());
		//if it's close enough, give the requite instruction
		if(distance <= 50){
			WorkoutService.tts.speak(currentDestination.getInstruction(), TextToSpeech.QUEUE_ADD, null);
			remainingDestinations.removeFirst();
		}else{
			//WorkoutService.tts.speak("Distance is:" + distance, TextToSpeech.QUEUE_ADD, null);
		}
	}

	/**
	 * When the workout service reports a new session, unbind from the old one and bind to the new one.
	 */
	@Override
	public void sessionChanged(ExerciseSessionData newSession) {
		largeStatsFragment.setExerciseSession(newSession);
		smallStatsFragment.setExerciseSession(newSession);
	}

	/**
	 * When the workout service's status changes, update the UI to match.
	 */
	@Override
	public void workoutServiceStatusChanged(WorkoutServiceStatusChangeType changeType, WorkoutServiceStatus status) {
		setUIState(workoutService.getService().getStatus());
	}

	/**
	 * When the workout service reports an error, try to allow the user to handle that error if possible. If not, then
	 * quit the activity.
	 */
	@Override
	public void serviceConnectionError(WorkoutServiceConnectionErrorType errorType) {
		switch (errorType) {

		// If there's a Bioharness error, show a dialog prompting the user to retry the Bioharness connection. If they
		// accept, launch the device discovery activity. If not, quit.
		case BioharnessError:
			logger.warn("serviceConnectionError(): Error reported of type BioharnessError. Prompting the user to check their Bluetooth settings.");
			BioharnessPreferences.BioharnessDescription.clearValue(WorkoutActivity.this);
			AlertDialog.Builder builder = new AlertDialog.Builder(WorkoutActivity.this);
			builder.setTitle(string.workoutscreen_dialog_nobioharness)
					.setMessage(string.workoutscreen_dialog_nobioharness_message)
					.setPositiveButton(string.workoutscreen_dialog_nobioharness_retry,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									Intent i = new Intent(WorkoutActivity.this, DeviceListActivity.class);
									startActivityForResult(i, REQUEST_DISCOVER_DEVICE);
								}
							}).setNegativeButton(string.cancel, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							finish();
						}
					}).show();
			break;

		default:
			logger.error("serviceConnectionError(): Error reported of type " + errorType
					+ ", which cannot be handled by the user. Activity will now finish.");
			finish();
			break;
		}
	}

	/**
	 * When a question is received from the doctor, bring up a prompt for replying to that question.
	 */
	// TODO Implement a version of this for older phones?
	@SuppressLint("NewApi")
	@Override
	public void questionReceivedFromDoctor(QuestionData question) {
		logger.info("questionReceivedFromDoctor(): prompting participant for question: '" + question.getQuestion()
				+ "'");

		// TODO Expand this to include responses other than yes / no.
		AlertDialog.Builder builder = new AlertDialog.Builder(WorkoutActivity.this);
		final Boolean[] answer = new Boolean[1];
		answer[0] = null;
		builder.setTitle("question").setMessage(question.getQuestion())
				.setPositiveButton(QuestionData.RESPONSE_YES, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						answer[0] = Boolean.TRUE;
					}

				}).setNegativeButton(QuestionData.RESPONSE_NO, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						answer[0] = Boolean.FALSE;
					}

				}).setOnDismissListener(new DialogInterface.OnDismissListener() {

					@Override
					public void onDismiss(DialogInterface dialog) {
						AnswerData answerData = new AnswerData();
						if (answer[0] == Boolean.TRUE) {
							answerData.setAnswer(QuestionData.RESPONSE_YES);
						} else if (answer[0] == Boolean.FALSE) {
							answerData.setAnswer(QuestionData.RESPONSE_NO);
						} else {
							answerData.setAnswer(QuestionData.DID_NOT_RESPOND);
						}

						workoutService.getService().replyToQuestion(answerData);
					}
				});
	}

	// private final DialogInterface.OnClickListener questionDialogClickListener = new DialogInterface.OnClickListener()
	// {
	//
	// @Override
	// public void onClick(DialogInterface dialog, int which) {
	// // TODO Auto-generated method stub
	//
	// }
	// };

	// ************************************************************************************************************

	// ************************************************************************************************************
	// Stopwatch timer
	// ************************************************************************************************************
	/**
	 * Handles timer updates on the UI thread.
	 */
	private final Handler.Callback timerUpdateHandlerCallback = new Handler.Callback() {

		@Override
		public boolean handleMessage(Message msg) {
			if (msg.what == MESSAGE_UPDATE_TIMER) {
				ExerciseSessionData session = workoutService == null ? null : workoutService.getService()
						.getCurrentSession();

				// TODO Probably will be a separate timer text view in the future, rather than the button.
				if (session != null) {
					btnStopWorkout.setText(AndroidTextUtils.getRelativeTimeStringMillis(session
							.getElapsedTimeInMillis()));
					//logger.info("Heart rate is: " + session.getHeartRate());
					return true;
				}
			}
			return false;
		}
	};

	/**
	 * Until interrupted, continuously posts "timer update" messages to the timer update handler, then sleeps for a
	 * period of time.
	 */
	private final Runnable timerUpdateTask = new Runnable() {

		@Override
		public void run() {
			try {
				while (!Thread.interrupted()) {
					Thread.sleep(100);
					timerUpdateHandler.obtainMessage(MESSAGE_UPDATE_TIMER).sendToTarget();
				}
			} catch (InterruptedException eInterrupt) {
			}
		}
	};

	// ************************************************************************************************************

	// ************************************************************************************************************
	// User Interface / UI Callback methods
	// ************************************************************************************************************

	/**
	 * Builds the UI from XML, and obtains references to all relevant views.
	 */
	private void buildUI() {

		// Load the content view, and setup the map component, if one exists.
		if (TestHarnessUtils.isTestHarness()) {
			// Don't load a map component if on emulator coz it requires the use of Google Play Services...
			setContentView(layout.workout_screen_nomap);
			mapFragment = null;
		} else {
			setContentView(layout.workout_screen);
			mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(id.map);

			// Add a custom marker to the map to show the user's location.
			mapFragment.getMapAsync(new OnMapReadyCallback() {
				@Override
				public void onMapReady(GoogleMap googleMap) {

					final MarkerOptions markerOpt = new MarkerOptions();
					markerOpt.icon(BitmapDescriptorFactory.fromResource(drawable.nihi_map_marker)).anchor(0.5f, 1f)
							.draggable(false).position(new LatLng(0, 0));

					WorkoutActivity.this.userMapMarker = googleMap.addMarker(markerOpt);
                    WorkoutActivity.this.theMap = googleMap;

				}
			});

		}
		pnlMap = findViewById(id.pnlMap);

		// "Start Workout" button
		btnStartWorkout = (Button) findViewById(id.btnStartWorkout);
		btnStartWorkout.setOnClickListener(btnStartWorkoutClickListener);

		// "Stop Workout" button
		btnStopWorkout = (Button) findViewById(id.btnStopWorkout);
		btnStopWorkout.setOnClickListener(btnStopWorkoutClickListener);

		// "Select Workout" button
		btnSelectWorkout = (Button) findViewById(id.btnSelectWorkout);
		btnSelectWorkout.setOnClickListener(btnSelectWorkoutClickListener);

		// "Symptoms" button
		btnSymptoms = (Button) findViewById(id.btnSymptoms);
		btnSymptoms.setOnClickListener(btnSymptomsClickListener);

		// "Notifications" button
		btnNotifications = (Button) findViewById(id.btnNotifications);
		btnNotifications.setOnClickListener(btnNotificationsClickListener);

		// "Show / Hide Map" button
		btnToggleMap = (ToggleButton) findViewById(id.btnToggleMap);
		if (TestHarnessUtils.isTestHarness()) {
			btnToggleMap.setEnabled(false);
			btnToggleMap.setText("[TEST MODE]");
		} else {
			btnToggleMap.setOnCheckedChangeListener(btnToggleMapToggleListener);
		}

		// Stats fragments (containing the stat view tiles)
		largeStatsFragment = (WorkoutScreenStatsViewFragment) getSupportFragmentManager().findFragmentById(
				id.largeStatsFragment);
		smallStatsFragment = (WorkoutScreenStatsViewFragment) getSupportFragmentManager().findFragmentById(
				id.smallStatsFragment);
		smallStatsFragment.setVisibility(View.GONE);
	}

	/**
	 * Sets the visibility / enabled status of various widgets to correspond to the given state.
	 * 
	 * UPDATE: This also controls whether the timer is running! YAY!
	 * 
	 *
	 */
	private void setUIState(WorkoutServiceStatus status) {

		boolean shouldEnableTimer = false;

		// If we can't start a new workout, and we're not monitoring, then we're in the "setup" phase.
		if (status == null || !(status.canStartNewWorkout() || status.isMonitoring())) {
			btnSymptoms.setEnabled(false);
			btnNotifications.setEnabled(false);
			btnSelectWorkout.setEnabled(false);
			btnStartWorkout.setVisibility(View.INVISIBLE);
			btnStopWorkout.setVisibility(View.INVISIBLE);
		}

		// If we're monitoring, we're in the "workout" phase.
		else if (status.isMonitoring()) {
			btnSymptoms.setEnabled(true);
			btnNotifications.setEnabled(true);
			btnSelectWorkout.setEnabled(false);
			btnStartWorkout.setVisibility(View.INVISIBLE);
			btnStopWorkout.setVisibility(View.VISIBLE);
			shouldEnableTimer = true;
		}

		// if we're not monitoring (but we can start a new workout), we're in the "ready" phase.
		else {
			btnSymptoms.setEnabled(false);
			btnNotifications.setEnabled(false);
			btnSelectWorkout.setEnabled(true);
			btnStartWorkout.setVisibility(View.VISIBLE);
			btnStopWorkout.setVisibility(View.INVISIBLE);
		}

		// If we're not fully connected to both Odin and the Bioharness, show the connection dialog.
		if (status == null || status.getBioharnessConnectivityStatus() != BHConnectivityStatus.CONNECTED) {
			//	TODO: REMOVE ODIN CONNECTION
//				|| status.getOdinConnectivityStatus() != EndpointConnectionStatus.CONNECTED) {


			if (this.connectionDialog == null) {
				this.connectionDialog = new ConnectionProgressDialogFragment();

				if (activityVisible) {
					this.connectionDialog.show(this.getSupportFragmentManager(), "ConnectionDialog");
				} else {
					dismissConnectionDialogOnResume = false;
					showConnectionDialogOnResume = true;
				}
			}

			if (status != null) {
				this.connectionDialog.setBioharnessConnectionStatus(status.getBioharnessConnectivityStatus());
				//	TODO: REMOVE ODIN CONNECTION
//				this.connectionDialog.setOdinConnectionStatus(status.getOdinConnectivityStatus());
			} else {
				this.connectionDialog.setBioharnessConnectionStatus(BHConnectivityStatus.DISCONNECTED);
				this.connectionDialog.setOdinConnectionStatus(EndpointConnectionStatus.DISCONNECTED);
			}

		}

		// Otherwise, hide it.
		else {
			if (this.connectionDialog != null) {
				if (activityVisible) {
					this.connectionDialog.dismiss();
					this.connectionDialog = null;
				} else {
					dismissConnectionDialogOnResume = true;
					showConnectionDialogOnResume = false;
				}
			}
		}

		// If the timer should be enabled, but isn't, enable it now. Also enable navigation
		if (shouldEnableTimer && timerUpdateThread == null) {
			timerUpdateThread = new Thread(timerUpdateTask);
			timerUpdateThread.setDaemon(true);
			timerUpdateThread.start();
			isPaused = false;
			WorkoutService.setIsPaused(false);
		}

		// If the timer should be disabled, but is enabled, disable it now.
		else if (!shouldEnableTimer && timerUpdateThread != null) {
			timerUpdateThread.interrupt();
			try {
				timerUpdateThread.join();
			} catch (InterruptedException e) {
			}
			timerUpdateThread = null;
			isPaused = true;
			WorkoutService.setIsPaused(true);
		}

	}

	/**
	 * Shows the given location on the map, if the map is visible.
	 * 
	 * @param location
	 */
	private void showOnMap(Location location) {
		final double RADIUS_KM = 1.5; // the distance, in KM, to show around the point.

		if (!TestHarnessUtils.isTestHarness() && isShowingMap) {
			LatLng gmLocation = new LatLng(location.getLatitude(), location.getLongitude());

			// Update user's location marker
			if (this.userMapMarker != null) {
				this.userMapMarker.setPosition(gmLocation);
			}

			// For now, just display RADIUS_KM in all directions around the given location.
			LatLng northBounds = LocationUtils.getLocation(gmLocation, 0, RADIUS_KM);
			LatLng southBounds = LocationUtils.getLocation(gmLocation, 180, RADIUS_KM);
			LatLng eastBounds = LocationUtils.getLocation(gmLocation, 90, RADIUS_KM);
			LatLng westBounds = LocationUtils.getLocation(gmLocation, 270, RADIUS_KM);

			LatLngBounds bounds;
			int padding;

			if (routePolyline == null) {
				bounds = LatLngBounds.builder().include(northBounds).include(southBounds).include(eastBounds)
						.include(westBounds).include(gmLocation).build();
				padding = 0;
			} else {
				LatLngBounds.Builder builder = LatLngBounds.builder().include(gmLocation);
				for (LatLng pt : routePolyline.getPoints()) {
					builder.include(pt);
				}
				bounds = builder.build();
				padding = 100;
			}

			final CameraUpdate camUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);

            if (this.theMap != null) {
                try {
                    this.theMap.moveCamera(camUpdate);

                } catch (IllegalStateException ex) {
                    // Map not yet initialized.
                    logger.info("showOnMap(): IllegalStateException when trying to move camera. Will delegate task to listener.");
                    final View mapView = mapFragment.getView();
                    if (mapView.getViewTreeObserver().isAlive()) {
                        mapView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
                            @SuppressWarnings("deprecation")
                            @SuppressLint("NewApi")
                            @Override
                            public void onGlobalLayout() {
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                                    mapView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                                } else {
                                    mapView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                }
                                WorkoutActivity.this.theMap.moveCamera(camUpdate);
                            }
                        });
                    }
                }
            }
		}
	}

	/**
	 * If there's a route polyline currently displayed on the map, clear it.
	 */
	private void clearRoutePolyline() {
		if (routePolyline != null) {
			routePolyline.remove();
			routePolyline = null;
		}
	}

	/**
	 * Loads the route with the given ID from the database, then displays it on the map.
	 * 
	 * @param routeId
	 */
	private void showRoutePolyline(String routeId) {
		try {

			Route route = getDbHelper().getRoutesDAO().queryForId(routeId);
			showRoutePolyline(route);

		} catch (SQLException e) {
			logger.error(e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Displays the given route on the map.
	 * 
	 * @param route
	 */
	private void showRoutePolyline(Route route) {
		clearRoutePolyline();
		if (route != null) {

			PolylineOptions polyOpt = new PolylineOptions();
			polyOpt.add(LocationUtils.toLatLngArray(route)).color(Color.RED).width(10);
            if (this.theMap != null) {
                routePolyline = this.theMap.addPolyline(polyOpt);
            }
		}
	}

	/**
	 * Sets a value indicating whether the map is shown or not.
	 * 
	 * @param mapMode
	 */
	private void setMapMode(boolean mapMode) {
		if (this.isShowingMap != mapMode) {

			if (mapMode) {

				largeStatsFragment.setVisibility(View.GONE);
				smallStatsFragment.setVisibility(View.VISIBLE);
				// pnlLargeTiles.setVisibility(View.GONE);
				// pnlSmallTiles.setVisibility(View.VISIBLE);
				pnlMap.setVisibility(View.VISIBLE);

				// float alpha = getResources().getFraction(fraction.alpha_semi_transparent, 1, 1);
				// float alpha = ALPHA_TRANSPARENT;
				// pnlSmallTiles.setAlpha(alpha);
				// pnlBottomButtons.setAlpha(alpha);
				// pnlTopButtons.setAlpha(alpha);
				// pnlStopwatch.setAlpha(alpha);

			} else {

				largeStatsFragment.setVisibility(View.VISIBLE);
				smallStatsFragment.setVisibility(View.GONE);
				// pnlSmallTiles.setVisibility(View.GONE);
				pnlMap.setVisibility(View.GONE);
				// pnlLargeTiles.setVisibility(View.VISIBLE);

				// float alpha = getResources().getFraction(fraction.alpha_lighter, 1, 1);
				// float alpha = ALPHA_OPAQUE;
				// pnlSmallTiles.setAlpha(OPAQUE);
				// pnlBottomButtons.setAlpha(alpha);
				// pnlTopButtons.setAlpha(alpha);
				// pnlStopwatch.setAlpha(OPAQUE);

			}

			this.isShowingMap = mapMode;
			this.btnToggleMap.setChecked(mapMode);

		}

		if (this.isShowingMap) {
			Location loc = workoutService == null ? null : workoutService.getService().getLastKnownLocation();
			if (loc != null) {
				showOnMap(loc);
			}
		}
	}

	/**
	 * If the user is exercising, prompt to finish. If not, just finish.
	 */
	@Override
	public void onBackPressed() {

		if (workoutService != null && workoutService.getService().getStatus().isMonitoring()) {
			if (TestHarnessUtils.isTestHarness()) {
				stopWorkout(true);
			} else {
				promptStopWorkout(true);
			}
		} else {
			super.onBackPressed();
			overridePendingTransition(anim.push_right_in, anim.push_right_out);
		}
	}

	/**
	 * This listener listens for changes in the map button's "checked" status and adjusts the map visibility to match.
	 */
	private final CompoundButton.OnCheckedChangeListener btnToggleMapToggleListener = new CompoundButton.OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			setMapMode(isChecked);
		}
	};

	/**
	 * This listener listens for clicks on the "select workout" button, and launches an activity allowing the user to
	 * perform this task.
	 */
	private final View.OnClickListener btnSelectWorkoutClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(WorkoutActivity.this, ExerciseSessionGoalSelectionActivity.class);
			startActivityForResult(intent, REQUEST_GOAL_TYPE);
		}
	};

	@Override
	public void statsViewButtonClicked(WorkoutScreenStatsViewFragment fragment, int buttonId,
			ViewedStat currentlyViewedStat) {
		Intent i = new Intent(WorkoutActivity.this, TileTypeSelectorActivity.class);
		i.putExtra(TileTypeSelectorActivity.EXTRA_TILE_TYPE, currentlyViewedStat);
		i.putExtra(TileTypeSelectorActivity.EXTRA_TILE_ID, buttonId);
		i.putExtra(TileTypeSelectorActivity.EXTRA_FRAGMENT_ID, fragment.getId());
		startActivityForResult(i, REQUEST_TILE_TYPE);
	}

	/**
	 * This listener listens for clicks on the "start workout" button, and starts a workout if possible when this
	 * happens.
	 */
	private final View.OnClickListener btnStartWorkoutClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {

			// Starts the workout.
			workoutService.getService().startWorkout(goal);
		}
	};

	/**
	 * This listener listens for clicks on the "stop workout" button, and prompts the user whether to actually stop the
	 * workout when this happens.
	 */
	private final View.OnClickListener btnStopWorkoutClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			promptStopWorkout(false);
		}
	};

	/**
	 * This listener listens for clicks on the "notifications" button, and launches the "view notifications" activity.
	 */
	private final View.OnClickListener btnNotificationsClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			startActivity(new Intent(WorkoutActivity.this, NotificationViewerActivity.class));
		}
	};

	/**
	 * This listener listens for clicks on the "symptoms" button, and launches the "choose symptom" activity.
	 */
	private final View.OnClickListener btnSymptomsClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(WorkoutActivity.this, SymptomPickerActivity.class);
			intent.putExtra(SymptomPickerActivity.EXTRA_PRIOR_SYMPTOMS, (Serializable) workoutService.getService()
					.getCurrentSession().getSymptoms());
			startActivityForResult(intent, REQUEST_SELECT_SYMPTOM);
		}
	};

	// ************************************************************************************************************

	// ************************************************************************************************************
	// Test harness methods
	// ************************************************************************************************************
	void testSendSymptom() {
		if (workoutService != null && workoutService.getService().getStatus().isMonitoring()) {
			Random rnd = new Random();
			Symptom symptom = Symptom.values()[rnd.nextInt(Symptom.values().length)];
			SymptomStrength strength = SymptomStrength.values()[rnd.nextInt(SymptomStrength.values().length - 1)];

			workoutService.getService().reportSymptom(symptom, strength);

		}
	}
	// ************************************************************************************************************
}
