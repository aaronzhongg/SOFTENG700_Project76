package nz.ac.auckland.nihi.trainer.activities;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

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
import nz.ac.auckland.nihi.trainer.data.ExerciseSummary;
import nz.ac.auckland.nihi.trainer.data.NihiDBHelper;
import nz.ac.auckland.nihi.trainer.fragments.ExerciseSummaryFragment;
import nz.ac.auckland.nihi.trainer.fragments.MonthlySummaryFragment;
import nz.ac.auckland.nihi.trainer.util.AndroidTextUtils;

import org.apache.log4j.Logger;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.j256.ormlite.dao.Dao;

/**
 * The review activity, which allows the user to browse all data collected by all exercise sessions performed using the
 * app. The activity provides monthly summary data, and per-session data. It also provides a swipe gesture control which
 * allows users to browse through sessions, and a calendar control allowing them to pick specific sessions.
 * 
 * @author Andrew Meads
 * 
 */
public class ReviewActivity extends FragmentActivity /* implements ActionBar.TabListener */{

	public static final String EXTRA_SUMMARY_ID = ReviewActivity.class.getName() + ".SummaryID";

	private static final Logger logger = Logger.getLogger(ReviewActivity.class);

	private static final int REQUEST_CODE_DATE_TIME_PICKER = 1;

	/**
	 * Allows the test harness to interact with this activity.
	 */
	private TestHarnessUIControllerCommandReceiver testHarnessUIController;

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide fragments for daily summaries. We use a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter} which will automatically kill fragments to free memory.
	 * We do this because there may be many dates for which there is exercise data, and keeping them all in memory would
	 * be wasteful.
	 */
	private DailySummaryPagesAdapter dailySummaryPagerAdapter;

	/**
	 * The {@link ViewPager} that will host daily summaries.
	 */
	private ViewPager dailySummaryPager;

	/**
	 * The {@link ViewPager} that will host the monthly summaries.
	 */
	private ViewPager monthlySummaryPager;

	private TextView txtMonthTitle, txtDayTitle;

	/**
	 * The helper allowing us to access the database.
	 */
	private LocalDatabaseHelper dbHelper;

	/**
	 * The list of exercise summaries. Will be created and populated in the {@link #onCreate(Bundle)} method. NOTE:
	 * These summaries here will only have the ID and Date columns populated - we'll load the rest of the data for
	 * individual summaries later, in the {@link ExerciseSummaryFragment} class.
	 */
	private List<ExerciseSummary> summaries;

	/**
	 * The list of valid dates, represented as millisecond values. Will be created and populated in the
	 * {@link #onCreate(Bundle)} method.
	 */
	private long[] validDates;

	/**
	 * The list of valid months, represented as millisecond values corresponding to 00:00:00, on the first of those
	 * months.
	 */
	private ArrayList<Long> validMonths;

	/**
	 * The Odin service connection, to be used to synchronize summary and route data.
	 */
	// private IOdinService odinService;

	/**
	 * The progress dialog to be displayed to the user when we're synchronizing data.
	 */
	// private ProgressDialog dlgOdinProgress;

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

	private void showNoDataDialog() {
		// If you've used this app before, you might be able to download your old workout data. Would you like to try
		// this now?
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(string.summaryscreen_dialog_nodata).setMessage(string.summaryscreen_dialog_nodata_message)
				.setNegativeButton(string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						finish();
					}
				})/*
				 * .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				 * 
				 * @Override public void onClick(DialogInterface dialog, int which) { beginOdinSynchronize(); }
				 * }).setCancelable(false)
				 */.show();
	}

	/**
	 * Creates the view layout and populates the pagers with monthly and daily summary data.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.review_screen);

		txtMonthTitle = (TextView) findViewById(id.txtThisMonth);
		txtDayTitle = (TextView) findViewById(id.txtDailyTitle);

		if (!populateUIFromDatabase()) {
			showNoDataDialog();
		}

		// If using the test harness, create the receiver. Don't hook it up just yet though.
		if (TestHarnessUtils.isTestHarness()) {
			testHarnessUIController = new TestHarnessUIControllerCommandReceiver(this);
		}

	}

	private boolean populateUIFromDatabase() {
		// Get the Dates and IDs for all Exercise Summary data from the database.
		try {
			LocalDatabaseHelper dbHelper = getDbHelper();
			Dao<ExerciseSummary, String> dao = dbHelper.getSectionHelper(NihiDBHelper.class).getExerciseSummaryDAO();
			long uid = OdinPreferences.UserID.getLongValue(this, -1);
			summaries = dao.queryBuilder().selectColumns("id", "date").where().eq("userId", uid).query();

			// Get outta here if there's no summaries.
			if (summaries == null || summaries.size() == 0) {
				return false;
			}

			Collections.sort(summaries, new Comparator<ExerciseSummary>() {
				@Override
				public int compare(ExerciseSummary lhs, ExerciseSummary rhs) {
					return lhs.getDate().compareTo(rhs.getDate());
				}
			});
		} catch (SQLException e) {
			logger.error("onCreate(): could not get ExerciseSummary data from database");
			throw new RuntimeException(e);
		}

		// Create a list of long values corresponding to the dates for those summaries.
		validDates = new long[summaries.size()];
		validMonths = new ArrayList<Long>();
		int index = 0;
		for (ExerciseSummary result : summaries) {
			Calendar cal = GregorianCalendar.getInstance();
			cal.setTime(result.getDate());

			// Record the exact time here
			validDates[index++] = cal.getTimeInMillis();

			// Set to the first of the calendar's month (i.e. set day to 1 and everything else to 0 except month and
			// year).
			cal.set(Calendar.DAY_OF_MONTH, 1);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);

			// Add the month to the month list if it is in fact a new month.
			if (validMonths.size() == 0 || !validMonths.get(validMonths.size() - 1).equals(cal.getTimeInMillis())) {

				validMonths.add(cal.getTimeInMillis());

			}
		}

		// Get the pager for monthly summary data.
		monthlySummaryPager = (ViewPager) findViewById(R.id.pgrMonthly);
		monthlySummaryPager.setAdapter(new MonthlySummaryPagesAdapter(getSupportFragmentManager()));
		monthlySummaryPager.setOnPageChangeListener(monthlySummaryPagerPageChangeListener);
		txtMonthTitle.setText(monthlySummaryPager.getAdapter().getPageTitle(0));

		// This object is what provides the ViewPager with its fragments. It will be in charge of creating a fragment
		// for each date for which there is valid exercise data.
		dailySummaryPagerAdapter = new DailySummaryPagesAdapter(getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		dailySummaryPager = (ViewPager) findViewById(R.id.pager);
		dailySummaryPager.setAdapter(dailySummaryPagerAdapter);
		dailySummaryPager.setOnPageChangeListener(dailySummaryPagerPageChangeListener);
		txtDayTitle.setText(dailySummaryPager.getAdapter().getPageTitle(0));

		int dailyIndexToSelect = -1;

		// If specified, we shall browse to a particular summary item.
		String summaryId = getIntent().getStringExtra(EXTRA_SUMMARY_ID);
		if (summaryId != null) {
			for (int i = 0; i < summaries.size(); i++) {
				ExerciseSummary summary = summaries.get(i);
				if (summary.getId().equals(summaryId)) {
					dailyIndexToSelect = i;
					break;
				}
			}
		}

		// Otherwise we'll browse to the latest one.
		if (dailyIndexToSelect < 0) {
			dailyIndexToSelect = summaries.size() - 1;
		}

		dailySummaryPager.setCurrentItem(dailyIndexToSelect);

		return true;
	}

	/**
	 * A listener for page changes on the monthly view, that will make sure a day in the corresponding month is selected
	 * in the daily view. If any day in the newly selected month is already visible, it will be unchanged. If not, the
	 * first day in that month for which there is exercise data available will be shown.
	 * 
	 * TODO In addition, this will eventually cause the view pager to be resized to exactly fit the contained monthly
	 * summary.
	 */
	private final ViewPager.OnPageChangeListener monthlySummaryPagerPageChangeListener = new OnPageChangeListener() {

		// private boolean changing = false;

		@Override
		public void onPageSelected(int position) {

			// Set title page
			txtMonthTitle.setText(monthlySummaryPager.getAdapter().getPageTitle(position));

			// if (changing)
			// return;

			// Get the current date selected in the daily view
			GregorianCalendar currentSelectedDay = new GregorianCalendar();
			currentSelectedDay.setTimeInMillis(validDates[dailySummaryPager.getCurrentItem()]);

			// Get the current month in the monthly view
			GregorianCalendar currentSelectedMonth = new GregorianCalendar();
			currentSelectedMonth.setTimeInMillis(validMonths.get(position));

			// Are the months the same? If so, do nothing.
			boolean sameMonth = currentSelectedDay.get(Calendar.YEAR) == currentSelectedMonth.get(Calendar.YEAR)
					&& currentSelectedDay.get(Calendar.MONTH) == currentSelectedMonth.get(Calendar.MONTH);

			// Otherwise, scroll the daily view to the first day in that month for which there is data.
			if (!sameMonth) {
				// changing = true;
				for (int newPosition = 0; newPosition < validDates.length; newPosition++) {
					GregorianCalendar newSelectedDay = new GregorianCalendar();
					newSelectedDay.setTimeInMillis(validDates[newPosition]);
					sameMonth = newSelectedDay.get(Calendar.YEAR) == currentSelectedMonth.get(Calendar.YEAR)
							&& newSelectedDay.get(Calendar.MONTH) == currentSelectedMonth.get(Calendar.MONTH);

					if (sameMonth) {
						dailySummaryPager.setCurrentItem(newPosition);
						break;
					}
				}
				// changing = false;
			}
		}

		@Override
		public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
		}

		@Override
		public void onPageScrollStateChanged(int state) {
		}
	};

	/**
	 * A listener for page changes on the daily view, that will make sure the corresponding month is selected in the
	 * monthly view.
	 */
	private final ViewPager.OnPageChangeListener dailySummaryPagerPageChangeListener = new OnPageChangeListener() {

		@Override
		public void onPageSelected(int position) {
			// Set the title TextView to the page title.
			txtDayTitle.setText(dailySummaryPager.getAdapter().getPageTitle(position));

			// Get the current date selected in the daily view
			GregorianCalendar currentSelectedDay = new GregorianCalendar();
			currentSelectedDay.setTimeInMillis(validDates[position]);

			// Get the current month in the monthly view
			GregorianCalendar currentSelectedMonth = new GregorianCalendar();
			currentSelectedMonth.setTimeInMillis(validMonths.get(monthlySummaryPager.getCurrentItem()));

			// Are the months the same? If so, do nothing.
			boolean sameMonth = currentSelectedDay.get(Calendar.YEAR) == currentSelectedMonth.get(Calendar.YEAR)
					&& currentSelectedDay.get(Calendar.MONTH) == currentSelectedMonth.get(Calendar.MONTH);

			// Otherwise, find the monthly summary with the same month as the currently selected day and select it.
			if (!sameMonth) {
				// changing = true;
				for (int newPosition = 0; newPosition < validMonths.size(); newPosition++) {
					GregorianCalendar newSelectedMonth = new GregorianCalendar();
					newSelectedMonth.setTimeInMillis(validMonths.get(newPosition));
					sameMonth = newSelectedMonth.get(Calendar.YEAR) == currentSelectedDay.get(Calendar.YEAR)
							&& newSelectedMonth.get(Calendar.MONTH) == currentSelectedDay.get(Calendar.MONTH);

					if (sameMonth) {
						monthlySummaryPager.setCurrentItem(newPosition);
						break;
					}
				}
				// changing = false;
			}
		}

		@Override
		public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
		}

		@Override
		public void onPageScrollStateChanged(int state) {
		}
	};

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to a monthly summary page.
	 */
	public class MonthlySummaryPagesAdapter extends FragmentStatePagerAdapter {

		public MonthlySummaryPagesAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			Bundle args = new Bundle();
			args.putLong(MonthlySummaryFragment.ARG_MONTH, validMonths.get(position));
			MonthlySummaryFragment frag = new MonthlySummaryFragment();
			frag.setArguments(args);
			return frag;
		}

		@Override
		public int getCount() {
			return validMonths.size();
		}

		/**
		 * The "title" of each page is equal to the year + month.
		 */
		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();

			GregorianCalendar date = new GregorianCalendar();
			date.setTimeInMillis(validMonths.get(position));
			return "Summary - " + date.getDisplayName(Calendar.MONTH, Calendar.LONG, l) + " " + date.get(Calendar.YEAR);
		}
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to a daily summary page.
	 */
	public class DailySummaryPagesAdapter extends FragmentStatePagerAdapter {

		public DailySummaryPagesAdapter(FragmentManager fm) {
			super(fm);
		}

		/**
		 * Creates and returns a {@link ExerciseSummaryFragment} pointing to the summary data for the date corresponding
		 * to <code>position</code>.
		 */
		@Override
		public Fragment getItem(int position) {
			Fragment fragment = new ExerciseSummaryFragment();
			Bundle args = new Bundle();
			args.putString(ExerciseSummaryFragment.ARG_SUMMARY_ID, summaries.get(position).getId());
			fragment.setArguments(args);
			return fragment;
		}

		/**
		 * The number of possible fragments is equal to the number of exercise summary data items.
		 */
		@Override
		public int getCount() {
			return summaries.size();
		}

		/**
		 * The "title" of each page is equal to the day + the day-of-month.
		 */
		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();

			// Work out the date string
			GregorianCalendar date = new GregorianCalendar();
			date.setTimeInMillis(validDates[position]);
			String dateString = date.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, l) + " "
					+ AndroidTextUtils.ordinal(ReviewActivity.this, date.get(Calendar.DAY_OF_MONTH));

			// Work out how many share the same day but come beforehand.
			int count = 0;
			for (int i = position - 1; i >= 0; i--) {
				GregorianCalendar otherDate = new GregorianCalendar();
				otherDate.setTimeInMillis(validDates[i]);
				if (date.get(Calendar.YEAR) == otherDate.get(Calendar.YEAR)
						&& date.get(Calendar.DAY_OF_YEAR) == otherDate.get(Calendar.DAY_OF_YEAR)) {
					count++;
				} else {
					break;
				}
			}

			// Put some parentheses with the count value + 1 if its > 0
			if (count > 0) {
				dateString += " (" + (count + 1) + ")";
			}

			return dateString;
		}
	}

	/**
	 * Change the animation to a nice sliding animation when we quit out of this activity
	 */
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		overridePendingTransition(anim.push_right_in, anim.push_right_out);
	}

	/**
	 * When we resume, we will reconnect the test harness if we're in test mode.
	 */
	@Override
	protected void onResume() {
		if (TestHarnessUtils.isTestHarness()) {
			LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(testHarnessUIController,
					TestHarnessService.IF_PROCESS_COMMAND);
		}
		super.onResume();
	}

	/**
	 * When we pause, we'll disconnect the test harness if we're in test mode.
	 */
	@Override
	protected void onPause() {
		if (TestHarnessUtils.isTestHarness()) {
			LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(testHarnessUIController);
		}
		super.onPause();
	}

	/**
	 * When this activity is destroyed, release its connection to the database, if any.
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (dbHelper != null) {
			DatabaseManager.getInstance().releaseHelper();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.review_screen, menu);
		return true;
	}

	/**
	 * When we click the "select date" button (the calendar icon in the top-right corner) we'll start an activity that
	 * will allow us to choose from all valid dates. A date is valid if we have exercise data for that date.
	 * 
	 * When we click the "synchronize" button, we'll attempt to connect to Odin and sync our Summary data.
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == id.action_select_date) {

			Intent i = new Intent(getApplicationContext(), DatePickerActivity.class);
			i.putExtra(DatePickerActivity.EXTRA_VALID_DATES, validDates);
			startActivityForResult(i, REQUEST_CODE_DATE_TIME_PICKER);
			return true;
		}

		// else if (item.getItemId() == id.action_synchronize_data) {
		//
		// beginOdinSynchronize();
		// return true;
		// }

		else {
			return super.onOptionsItemSelected(item);
		}
	}

	// private void beginOdinSynchronize() {
	// // Show progress dialog
	// dlgOdinProgress = ProgressDialog.show(this, "Synchronizing", "Synchronizing with Odin...", true);
	//
	// // Start odin service
	// bindService(new Intent(this, OdinService.class), odinConn, BIND_AUTO_CREATE);
	// }

	/**
	 * When we get a result from a child activity, if its a date from a date picker, we'll browse to that date's
	 * summary.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == REQUEST_CODE_DATE_TIME_PICKER && resultCode == RESULT_OK) {
			int index = data.getIntExtra(DatePickerActivity.EXTRA_INDEX, -1);
			if (index >= 0) {
				dailySummaryPager.setCurrentItem(index);
			}
		}
	}

	// /**
	// * Handles the connection to the OdinServie.
	// *
	// * When obtained, if not connected, then connect. If connected, start the data sync process.
	// */
	// private final ServiceConnection odinConn = new ServiceConnection() {
	//
	// @Override
	// public void onServiceDisconnected(ComponentName name) {
	// // TODO Auto-generated method stub
	//
	// }
	//
	// @Override
	// public void onServiceConnected(ComponentName name, IBinder service) {
	// odinService = (IOdinService) service;
	// SynchronizeWithOdinTask syncTask = new SynchronizeWithOdinTask();
	// syncTask.execute();
	// }
	// };

	// /**
	// * The task that requests exercise summaries from Odin, picks out those that we don't have in our local database,
	// * then displays them in the UI thread.
	// *
	// * @author anhyd_000
	// *
	// */
	// private class SynchronizeWithOdinTask extends AsyncTask<Void, Void, List<ExerciseSummary>> {
	//
	// @Override
	// protected List<ExerciseSummary> doInBackground(Void... params) {
	//
	// try {
	//
	// // Make sure we're connected to Odin.
	// boolean needToConnect = odinService.getConnectionStatus() == EndpointConnectionStatus.DISCONNECTED;
	// if (needToConnect) {
	// odinService.beginConnect();
	// }
	// odinService.waitFor(EndpointConnectionStatus.CONNECTED);
	// logger.info("SynchronizeWithOdinTask.doInBackground(): Connected to Odin.");
	//
	// // Send the request, wait for response.
	// BasicStringMessage request = new BasicStringMessage(ExerciseSummary.ODIN_MESSAGE_SYNC_SUMMARIES);
	// MessageHandle responseHandle = odinService.sendRequest(request);
	// Message responseMessage = responseHandle.waitForResponse();
	//
	// // Disconnect from Odin and unbind from the service. Only disconnect if we were the ones who connected
	// // in the first place.
	// if (needToConnect) {
	// odinService.disconnect();
	// }
	// unbindService(odinConn);
	//
	// ParticipantDataResponse response = null;
	// if (responseMessage instanceof JsonMessage) {
	// JsonMessage jsonResponse = (JsonMessage) responseMessage;
	// response = (ParticipantDataResponse) jsonResponse.deserialize(ParticipantDataResponse.class);
	// }
	// if (response == null) {
	// throw new Exception("No sync response!");
	// }
	//
	// if (response.getSummaries() == null) {
	// logger.info("SynchronizeWithOdinTask.doInBackground(): Received a null summaries object from Odin.");
	// } else {
	// logger.info("SynchronizeWithOdinTask.doInBackground(): Received " + response.getSummaries().length
	// + " summaries from Odin.");
	// }
	//
	// // If the response was empty get outta here.
	// if (response.getSummaries() == null || response.getSummaries().length == 0) {
	// return null;// new ArrayList<ExerciseSummary>();
	// }
	//
	// // Get the DAO for ExerciseSummary data.
	// Dao<ExerciseSummary, String> dao = dbHelper.getSectionHelper(NihiDBHelper.class)
	// .getExerciseSummaryDAO();
	//
	// // For each returned summary...
	// for (SummaryData summaryDataFromOdin : response.getSummaries()) {
	// ExerciseSummary summaryFromOdin = summaryDataFromOdin.summary;
	//
	// // Search the DB for a local one that matches.
	// // TODO HACK: +/- 1000 milliseconds counts as the same.
	// Date timestampMin = new Date(summaryFromOdin.getDate().getTime() - 1000);
	// Date timestampMax = new Date(summaryFromOdin.getDate().getTime() + 1000);
	// long uid = OdinPreferences.UserID.getLongValue(ReviewActivity.this, -1);
	// ExerciseSummary localSummary = dao.queryBuilder().selectColumns("userId", "date").where()
	// .eq("userId", uid).and().gt("date", timestampMin).and().lt("date", timestampMax)
	// .queryForFirst();
	//
	// // If there isn't, then save it to the db.
	// if (localSummary == null) {
	//
	// // If the remote summary has route data...
	// if (summaryFromOdin.getFollowedRoute() != null) {
	//
	// // Get its ID, and find the returned Route with the same ID.
	// RouteData routeDataFromOdin = null;
	// for (RouteData rd : response.getRoutes()) {
	// if (rd.route.getId().equals(summaryFromOdin.getFollowedRoute().getId())) {
	// routeDataFromOdin = rd;
	// break;
	//
	// }
	// }
	//
	// // If the route was found..
	// if (routeDataFromOdin != null) {
	//
	// // .. See if its already in the DB.
	// Dao<Route, String> routeDao = getDbHelper().getSectionHelper(NihiDBHelper.class)
	// .getRoutesDAO();
	// Route localRoute = routeDao.queryForId(routeDataFromOdin.route.getId());
	//
	// // If not, add it.
	// if (localRoute == null) {
	//
	// Route remoteRoute = routeDataFromOdin.route;
	// routeDao.assignEmptyForeignCollection(remoteRoute, "gpsCoordinates");
	// routeDao.create(remoteRoute);
	// for (RouteCoordinate coord : routeDataFromOdin.coords) {
	// coord.setRoute(remoteRoute);
	// remoteRoute.getGpsCoordinates().add(coord);
	// }
	// }
	//
	// }
	//
	// // If not, set the route to null.
	// else {
	// summaryFromOdin.setFollowedRoute(null);
	// }
	//
	// }
	//
	// dao.assignEmptyForeignCollection(summaryFromOdin, "symptoms");
	// dao.assignEmptyForeignCollection(summaryFromOdin, "notifications");
	// dao.create(summaryFromOdin);
	//
	// for (SymptomEntry s : summaryDataFromOdin.symptoms) {
	// s.setSummary(summaryFromOdin);
	// summaryFromOdin.getSymptoms().add(s);
	// }
	//
	// for (ExerciseNotification n : summaryDataFromOdin.notifications) {
	// n.setSummary(summaryFromOdin);
	// summaryFromOdin.getNotifications().add(n);
	// }
	//
	// logger.info("SynchronizeWithOdinTask.doInBackground(): Summary from Odin wasn't found in local DB and will be added now.");
	//
	// }
	//
	// else {
	// logger.info("SynchronizeWithOdinTask.doInBackground(): Summary from Odin was already in local DB.");
	// }
	//
	// }
	//
	// // TODO Maybe change in the future but I really don't need to return anything here.
	// return null;
	//
	// } catch (Exception ex) {
	// throw new RuntimeException(ex);
	// }
	// }
	//
	// protected void onPostExecute(java.util.List<ExerciseSummary> result) {
	//
	// // Close the progress dialog and reload the page.
	// populateUIFromDatabase();
	// dlgOdinProgress.dismiss();
	//
	// }
	//
	// }

}
