package nz.ac.auckland.nihi.trainer.activities;

import java.util.Calendar;

import nz.ac.auckland.cs.odin.android.api.activities.OdinFragmentActivity;
import nz.ac.auckland.cs.odin.android.api.prefs.OdinPreferences;
import nz.ac.auckland.cs.odin.android.api.services.testharness.TestHarnessService;
import nz.ac.auckland.cs.odin.android.api.services.testharness.TestHarnessUIControllerCommandReceiver;
import nz.ac.auckland.cs.odin.android.api.services.testharness.TestHarnessUtils;
import nz.ac.auckland.cs.ormlite.DatabaseManager;
import nz.ac.auckland.cs.ormlite.LocalDatabaseHelper;
import nz.ac.auckland.nihi.trainer.R;
import nz.ac.auckland.nihi.trainer.R.anim;
import nz.ac.auckland.nihi.trainer.R.id;
import nz.ac.auckland.nihi.trainer.R.string;
import nz.ac.auckland.nihi.trainer.data.NihiDBHelper;
import nz.ac.auckland.nihi.trainer.fragments.LoginDialogFragment;
import nz.ac.auckland.nihi.trainer.fragments.LoginDialogFragment.LoginDialogFragmentListener;
import nz.ac.auckland.nihi.trainer.prefs.NihiPreferences;
import nz.ac.auckland.nihi.trainer.services.downloader.DownloaderBroadcastReceiver;
import nz.ac.auckland.nihi.trainer.util.DownloadAllDataTask;
import nz.ac.auckland.nihi.trainer.util.DownloadAllDataTask.DownloadAllDataTaskListener;
import nz.ac.auckland.nihi.trainer.util.OdinIDUtils;

import org.apache.log4j.Logger;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

/**
 * Landing screen activity for the NIHI trainer app. Facilitates navigation to other areas of the app.
 * 
 * @author Andrew Meads
 * 
 */
public class HomeScreenActivity extends OdinFragmentActivity implements LoginDialogFragmentListener,
		DownloadAllDataTaskListener {

	// ************************************************************************************************************
	// Instance variables
	// ************************************************************************************************************
	private static final int DLG_NO_GOOGLE_PLAY_API = 1;
	private static final int REQUEST_ENTER_PROFILE_INFO = 2;

	private static final Logger logger = Logger.getLogger(HomeScreenActivity.class);

	private Button btnReview;
	private Button btnRoutes;
	private Button btnFriends;
	private Button btnProfile;
	private Button btnGoals;
	private Button btnWorkout;

	private boolean isLoggingIn = false;

	/**
	 * Allows the test harness to interact with this activity.
	 */
	private TestHarnessUIControllerCommandReceiver testHarnessUIController;

	/**
	 * The helper allowing us to access the database.
	 */
	private LocalDatabaseHelper dbHelper;

	// ************************************************************************************************************

	// ************************************************************************************************************
	// Activity Lifecycle
	// ************************************************************************************************************

	public HomeScreenActivity() {
		super(false);
	}

	/**
	 * On create, build the UI and hook up listeners. Also, as this is the application entry point, create the database
	 * if it isn't already created, and start test harness service if we're running on the test harness machine.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		if (TestHarnessUtils.isTestHarness() || checkBluetoothAvailable()) {

			super.onCreate(savedInstanceState);

			setContentView(R.layout.home_screen);

			// If we're using the test harness, create the command receiver and make the test harness label visible.
			if (TestHarnessUtils.isTestHarness()) {
				findViewById(id.txtTestHarness).setVisibility(View.VISIBLE);
				testHarnessUIController = new TestHarnessUIControllerCommandReceiver(this);
			}

			// Add listeners to all buttons
			btnReview = (Button) findViewById(id.btnReview);
			btnReview.setOnClickListener(buttonClickListener);

			btnRoutes = (Button) findViewById(id.btnRoutes);
			btnRoutes.setOnClickListener(buttonClickListener);

			btnFriends = (Button) findViewById(id.btnFriends);
			btnFriends.setOnClickListener(buttonClickListener);

			btnProfile = (Button) findViewById(id.btnProfile);
			btnProfile.setOnClickListener(buttonClickListener);

			btnGoals = (Button) findViewById(id.btnGoals);
			btnGoals.setOnClickListener(buttonClickListener);

			btnWorkout = (Button) findViewById(id.btnWorkout);
			btnWorkout.setOnClickListener(buttonClickListener);

			startDailyDownloadTask();

		}
	}

	/**
	 * Starts a task that will automatically synchronize route information and such every day.
	 * 
	 * TODO Refactor so the user can enable / disable this.
	 */
	private void startDailyDownloadTask() {

		final int REQUEST_NIHI_DOWLNLOADER = 0;

		Intent downloader = new Intent(this, DownloaderBroadcastReceiver.class);
		PendingIntent recurringDownload = PendingIntent.getBroadcast(this, REQUEST_NIHI_DOWLNLOADER, downloader,
				PendingIntent.FLAG_CANCEL_CURRENT);

		AlarmManager alarms = (AlarmManager) getSystemService(ALARM_SERVICE);

		// Get the time for tomorrow at 1am
		Calendar updateTime = Calendar.getInstance();
		updateTime.add(Calendar.DAY_OF_YEAR, 1);
		updateTime.set(Calendar.HOUR_OF_DAY, 1);
		updateTime.set(Calendar.MINUTE, 0);

		// Set the updater to go off at 1am tomorrow, then roughly daily afterwards.
		// Calling this SHOULD also cancel any previous alarm set.
		alarms.setInexactRepeating(AlarmManager.RTC_WAKEUP, updateTime.getTimeInMillis(), AlarmManager.INTERVAL_DAY,
				recurringDownload);

	}

	/**
	 * When we "resume", that is, when we first load after onCreate or when the user goes "back" to this activity, first
	 * check that Google Play Services is installed. If not, direct the user to download it. Once we've done that, hook
	 * up the test harness if we're running in test mode. Finally, check whether the user has previously logged in and /
	 * or has profile data available, and take the appropriate action based on this.
	 */
	@Override
	protected void onResume() {
		super.onResume();

		// Check for Google Play Services installed on the device. If not present, redirect the user to the play store
		// to download it. Only do this if not emulator.
		if (!TestHarnessUtils.isTestHarness()) {
			int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
			if (result != ConnectionResult.SUCCESS) {
				btnReview.setEnabled(false);
				btnRoutes.setEnabled(false);
				btnFriends.setEnabled(false);
				btnProfile.setEnabled(false);
				btnGoals.setEnabled(false);
				btnWorkout.setEnabled(false);
				Dialog dlg = GooglePlayServicesUtil.getErrorDialog(result, this, DLG_NO_GOOGLE_PLAY_API);
				dlg.show();
			}
		}

		// Hook up to the test harness if required
		if (TestHarnessUtils.isTestHarness()) {
			LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(testHarnessUIController,
					TestHarnessService.IF_PROCESS_COMMAND);
		}

		// If we're NOT on the test harness, or already trying to login, do a login check (this will be handled by the
		// test harness script if we are in test mode).
		else if (!isLoggingIn) {

			//	TODO: REMOVE ODIN CONNECTION - remove login
//			// Check if someone's logged in previously.
//			boolean alreadyLoggedIn = OdinPreferences.UserName.getStringValue(this, null) != null;
			boolean alreadyLoggedIn = true;

			// If they have...
			if (alreadyLoggedIn) {
				// If they need to enter profile info, go to the profile screen now.
				if (profileScreenRequired()) {
					// TODO: REMOVE ODIN SERVICE - Remove profile screen (entering user details)
//					forceProfileScreen();
				}
			}

			//	TODO: REMOVE ODIN CONNECTION
			// If they haven't, bring up the login dialog.
//			else {
//				showLoginDialog();
//			}

		}
	}

	/**
	 * When we pause (i.e. go to the background), unhook from the test harness if we're running in test mode.
	 */
	@Override
	protected void onPause() {
		if (TestHarnessUtils.isTestHarness()) {
			LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(testHarnessUIController);
		}
		super.onPause();
	}

	// ************************************************************************************************************

	// ************************************************************************************************************
	// Odin / Login handler methods
	// ************************************************************************************************************
//	TODO: REMOVE ODIN CONNECTION - Remove login
//	/**
//	 * Shows the login dialog.
//	 */
//	private void showLoginDialog() {
//		isLoggingIn = true;
//		LoginDialogFragment dialog = new LoginDialogFragment();
//		dialog.show(getSupportFragmentManager(), "LoginDialog");
//	}

	/**
	 * Called when the user has finished with the login dialog. If they have cancelled, then exit. If not, then begin
	 * connecting to Odin to verify the correct login.
	 * 
	 * @param dialog
	 * @param cancelled
	 */
	public void onFinishLoginFragment(LoginDialogFragment dialog, boolean cancelled) {

//		if (cancelled) {
//			finish();
//		} else {
//
//			// Save variables
//			long id = OdinIDUtils.generateID(dialog.getUserName());
//			OdinPreferences.UserID.setValue(this, id);
//			String username = dialog.getUserName();
//			OdinPreferences.UserName.setValue(this, username);
//			String password = dialog.getPassword();
//			OdinPreferences.Password.setValue(this, password);
//
//			// Attempt to synchronize.
//			// DownloadAllDataTask downloadTask = new DownloadAllDataTask(this, getOdinService(), getDbHelper()
//			// .getSectionHelper(NihiDBHelper.class), this, true);
//			// downloadTask.execute();
//			beginOdinConnect();
//
//		}

	}

	/**
	 * Called when we try to connect to Odin, but can't due to an IO error. Alerts the user then closes.
	 */
	@Override
	protected void odinHostUnreachable() {
		isLoggingIn = false;
		OdinPreferences.UserID.clearValue(this);
		OdinPreferences.UserName.clearValue(this);
		OdinPreferences.Password.clearValue(this);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setCancelable(false).setTitle(string.homescreen_dialog_cantconnect)
				.setMessage(string.homescreen_dialog_cantconnect_message)
				.setPositiveButton(string.ok, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {

						finish();

					}
				}).show();
	}

	/**
	 * Called when we try to connect to Odin, but can't due to incorrect login details. Prompts the user to enter their
	 * login details again.
	 */
	//	TODO: REMOVE ODIN CONNECTION - remove login
//	@Override
//	protected void odinLoginFailed() {
//		isLoggingIn = false;
//		OdinPreferences.UserID.clearValue(this);
//		OdinPreferences.UserName.clearValue(this);
//		OdinPreferences.Password.clearValue(this);
//		AlertDialog.Builder builder = new AlertDialog.Builder(this);
//		builder.setCancelable(false).setTitle(string.homescreen_dialog_invalidlogin)
//				.setMessage(string.homescreen_dialog_invalidlogin_message)
//				.setPositiveButton(string.ok, new DialogInterface.OnClickListener() {
//					@Override
//					public void onClick(DialogInterface dialog, int which) {
//						showLoginDialog();
//					}
//				}).show();
//	}

	/**
	 * Called when we try to connect to Odin, and succeed. Here we will download all user data and save it to the local
	 * DB.
	 */
	@Override
	protected void odinLoginSuccess() {

		DownloadAllDataTask downloadTask = new DownloadAllDataTask(this, getOdinService(), getDbHelper()
				.getSectionHelper(NihiDBHelper.class), this, true);

		downloadTask.execute();

	}

	/**
	 * Called when we've finished logging in and synchronizing data with Odin.
	 */
	@Override
	public void onDownloadAllDataComplete(boolean success) {

		isLoggingIn = false;

		// If successful, resume checking for profile data.
		if (success) {
			// If they need to enter profile info, go to the profile screen now.
			if (profileScreenRequired()) {
				// TODO: REMOVE ODIN SERVICE - Profile screen (adding user details)
//				forceProfileScreen();
			}
		}

		// Otherwise get outta here.
		else {
			finish();
		}
	}

	// ************************************************************************************************************

	// ************************************************************************************************************
	// Profile methods
	// ************************************************************************************************************
	/**
	 * Display a dialog to the user notifying them that they are required to go to the profile screen. When they click
	 * "OK", do it.
	 */
	private void forceProfileScreen() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setCancelable(false).setTitle(string.homescreen_dialog_goprofile)
				.setMessage(string.homescreen_dialog_goprofile_message)
				.setPositiveButton(string.ok, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {

						Intent i = new Intent(getApplicationContext(), ProfileActivity.class);
						startActivity(i);

					}
				}).show();
	}

	// ************************************************************************************************************

	// ************************************************************************************************************
	// UI Interaction
	// ************************************************************************************************************

	/**
	 * When we press the back button, change the animation that will play when the activity closes.
	 */
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		overridePendingTransition(anim.push_right_in, anim.push_right_out);
	}

	/**
	 * When we click a main menu button, navigate to the appropriate activity based on which button was clicked.
	 */
	private final View.OnClickListener buttonClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {

			Intent intent = null;

			if (v.getId() == id.btnReview) {
				intent = new Intent(getApplicationContext(), ReviewActivity.class);

			} else if (v.getId() == id.btnRoutes) {
				intent = new Intent(getApplicationContext(), RoutesActivity.class);

			} else if (v.getId() == id.btnFriends) {

			} else if (v.getId() == id.btnProfile) {
				intent = new Intent(getApplicationContext(), ProfileActivity.class);

			} else if (v.getId() == id.btnGoals) {
				intent = new Intent(getApplicationContext(), GoalsActivity.class);

			} else if (v.getId() == id.btnWorkout) {
				intent = new Intent(getApplicationContext(), WorkoutActivity.class);
			}

			if (intent != null) {
				startActivity(intent);
				overridePendingTransition(anim.push_left_in, anim.push_left_out);
			}
		}
	};

	// ************************************************************************************************************

	// ************************************************************************************************************
	// Misc.
	// ************************************************************************************************************
	/**
	 * Returns a value indicating whether the user is required to go to the profile screen.
	 * 
	 * @return
	 */
	private boolean profileScreenRequired() {
		return !TestHarnessUtils.isTestHarness()
				&& (!OdinPreferences.HostName.hasValue(this) || !OdinPreferences.Password.hasValue(this)
						|| !OdinPreferences.Port.hasValue(this) || !OdinPreferences.SurrogateName.hasValue(this)
						|| !OdinPreferences.UserID.hasValue(this) || !OdinPreferences.UserName.hasValue(this)
						|| !NihiPreferences.DateOfBirth.hasValue(this) || !NihiPreferences.Gender.hasValue(this)
						|| !NihiPreferences.Height.hasValue(this) || !NihiPreferences.MaxHeartRate.hasValue(this)
						|| !NihiPreferences.Name.hasValue(this) || !NihiPreferences.RestHeartRate.hasValue(this) || !NihiPreferences.Weight
							.hasValue(this));
	}

	/**
	 * Returns a value indicating whether Bluetooth is available on the device running this app. If Bluetooth is not
	 * available, this method additionally displays a dialog to the user indicating that they will not be able to use
	 * this app as it requires Bluetooth.
	 * 
	 * @return
	 */
	private boolean checkBluetoothAvailable() {
		// Make sure Bluetooth is available on this device. If not, then GTFO.
		BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
		if (bluetooth == null) {

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setCancelable(false).setPositiveButton(string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			}).setTitle(string.homescreen_dialog_nobluetooth).setMessage(string.homescreen_dialog_nobluetooth_message)
					.show();

			return false;

		} else {
			return true;
		}
	}

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

	// ************************************************************************************************************

}
