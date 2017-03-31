package nz.ac.auckland.nihi.trainer.activities;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import nz.ac.auckland.cs.odin.android.api.activities.OdinFragmentActivity;
import nz.ac.auckland.cs.odin.android.api.asynctask.OdinAsyncTask;
import nz.ac.auckland.cs.odin.android.api.prefs.OdinPreferences;
import nz.ac.auckland.cs.odin.android.api.services.IOdinService;
import nz.ac.auckland.cs.odin.data.JsonMessage;
import nz.ac.auckland.cs.odin.interconnect.common.MessageHandle;
import nz.ac.auckland.cs.ormlite.DatabaseManager;
import nz.ac.auckland.cs.ormlite.LocalDatabaseHelper;
import nz.ac.auckland.nihi.trainer.R.anim;
import nz.ac.auckland.nihi.trainer.R.id;
import nz.ac.auckland.nihi.trainer.R.layout;
import nz.ac.auckland.nihi.trainer.R.string;
import nz.ac.auckland.nihi.trainer.data.ExerciseSummary;
import nz.ac.auckland.nihi.trainer.data.Goal;
import nz.ac.auckland.nihi.trainer.data.GoalType;
import nz.ac.auckland.nihi.trainer.data.NihiDBHelper;
import nz.ac.auckland.nihi.trainer.fragments.AddGoalDialogFragment;
import nz.ac.auckland.nihi.trainer.fragments.AddGoalDialogFragment.AddGoalDialogFragmentListener;
import nz.ac.auckland.nihi.trainer.messaging.GoalData;
import nz.ac.auckland.nihi.trainer.prefs.NihiPreferences;
import nz.ac.auckland.nihi.trainer.util.AndroidTextUtils;
import nz.ac.auckland.nihi.trainer.util.UiUtils;

import org.apache.log4j.Logger;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.j256.ormlite.dao.Dao;

/**
 * 
 * 
 * @author Andrew Meads
 * 
 */
public class GoalsActivity extends OdinFragmentActivity implements AddGoalDialogFragmentListener {

	// ************************************************************************************************************
	// Instance Variables
	// ************************************************************************************************************

	private static final Logger logger = Logger.getLogger(GoalsActivity.class);

	// Summaries for the week in which this activity was invoked.
	private List<ExerciseSummary> weeklySummaries;

	// Goals list view.
	private ListView lstGoals;

	// Database access object.
	private LocalDatabaseHelper dbHelper;

	// ************************************************************************************************************

	// ************************************************************************************************************
	// Activity Lifecycle
	// ************************************************************************************************************

	public GoalsActivity() {
		super(false);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(layout.goals_screen);

		// Grab the weekly summaries we'll use for calculation.
		populateSummariesList();

		lstGoals = (ListView) findViewById(id.lstGoals);

		findViewById(id.btnAddGoal).setOnClickListener(btnAddGoalClickListener);

		populateListView();

		// Write the header text.
		Calendar firstDayOfWeek = Calendar.getInstance();
		Calendar lastDayOfWeek = Calendar.getInstance();
		populateCalendars(firstDayOfWeek, lastDayOfWeek);

		TextView lblWeek = (TextView) findViewById(id.lblWeek);
		// Goals for Monday XXth - Sunday YYth
		lblWeek.setText("Goals for Monday " + AndroidTextUtils.ordinal(this, firstDayOfWeek.get(Calendar.DAY_OF_MONTH))
				+ " - Sunday " + AndroidTextUtils.ordinal(this, lastDayOfWeek.get(Calendar.DAY_OF_MONTH)));
	}

	// ************************************************************************************************************

	// ************************************************************************************************************
	// UI Interaction
	// ************************************************************************************************************

	/**
	 * Populates the list view with all goals.
	 */
	private void populateListView() {
		lstGoals.setAdapter(new GoalsAdapter(getAllGoals()));
	}

	@Override
	public void onFinishAddGoalDialog(Goal goal) {
		try {

			Dao<Goal, String> dao = getDbHelper().getSectionHelper(NihiDBHelper.class).getGoalsDAO();
			dao.create(goal);
			populateListView();
			syncWithOdin();

		} catch (SQLException e) {
			logger.error(e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * When the "delete goal" button is clicked, prompts the user to delete that goal. If they accept, removes it then
	 * refreshes the screen.
	 */
	private final View.OnClickListener btnDeleteGoalClickListener = new View.OnClickListener() {

		@Override
		public void onClick(final View v) {

			AlertDialog.Builder builder = new AlertDialog.Builder(GoalsActivity.this);
			builder.setTitle(string.goalscreen_dialog_removegoal).setMessage(string.goalscreen_dialog_areyousure)
					.setCancelable(true).setPositiveButton(string.yes, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							Goal goal = (Goal) v.getTag();
							try {
								Dao<Goal, String> dao = getDbHelper().getSectionHelper(NihiDBHelper.class)
										.getGoalsDAO();
								dao.delete(goal);
								populateListView();
								syncWithOdin();
							} catch (SQLException e) {
								logger.error(e);
								throw new RuntimeException(e);
							}

						}
					}).setNegativeButton(string.no, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub

						}
					});
			builder.show();

		}
	};

	/**
	 * When the "Add Goal" button is clicked, opens the "Add Goal" dialog.
	 */
	private final View.OnClickListener btnAddGoalClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			AddGoalDialogFragment fraggers = new AddGoalDialogFragment();
			// Dao<Goal, String> dao = getDbHelper().getSectionHelper(NihiDBHelper.class).getGoalsDAO();
			List<Goal> goals = getAllGoals();
			if (goals.size() < GoalType.values().length) {
				fraggers.setCurrentGoals(goals);
				fraggers.show(getSupportFragmentManager(), "AddGoalDialog");
			}
		}
	};

	// ************************************************************************************************************

	// ************************************************************************************************************
	// Goals Adapter
	// ************************************************************************************************************

	private List<Goal> getAllGoals() {
		try {
			long userId = OdinPreferences.UserID.getLongValue(this, 0);
			return getDbHelper().getSectionHelper(NihiDBHelper.class).getGoalsDAO().queryBuilder().where()
					.eq("userId", userId).query();
		} catch (SQLException ex) {
			logger.error(ex);
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Adapts a list of Goals to display in a ListView.
	 */
	private class GoalsAdapter extends ArrayAdapter<Goal> {

		public GoalsAdapter(List<Goal> goals) {
			super(GoalsActivity.this, 0, goals);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			View root = convertView;
			if (root == null) {
				LayoutInflater li = LayoutInflater.from(GoalsActivity.this);
				root = li.inflate(layout.goals_screen_goal_row, parent, false);
			}

			Goal goal = getItem(position);

			root.findViewById(id.btnRemoveGoal).setTag(goal);
			root.findViewById(id.btnRemoveGoal).setOnClickListener(btnDeleteGoalClickListener);

			TextView lblGoalType = (TextView) root.findViewById(id.lblGoalType);
			TextView lblGoalProgress = (TextView) root.findViewById(id.lblGoalProgress);
			TextView lblGoalTarget = (TextView) root.findViewById(id.lblGoalTarget);
			TextView lblGoalUnits = (TextView) root.findViewById(id.lblGoalUnit);
			ProgressBar prgGoalProgress = (ProgressBar) root.findViewById(id.prgGoalProgress);

			lblGoalType.setText(goal.getGoalType().toString());
			lblGoalTarget.setText(Integer.valueOf(goal.getWeeklyTarget()).toString());
			lblGoalUnits.setText(goal.getGoalType().getUnits());

			// Percentage progress
			int restingHR = NihiPreferences.RestHeartRate.getIntValue(GoalsActivity.this, -1);
			int maxHR = NihiPreferences.MaxHeartRate.getIntValue(GoalsActivity.this, -1);
			int percentageProgress = goal.getGoalType().getProgressPercentage(weeklySummaries, goal.getWeeklyTarget(),
					restingHR, maxHR);

			lblGoalProgress.setText(percentageProgress + "%");
			prgGoalProgress.setMax(100);
			prgGoalProgress.setProgress(Math.min(100, percentageProgress));

			// Goal icon
			ImageView imgGoalIcon = (ImageView) root.findViewById(id.imgGoalIcon);
			imgGoalIcon.setImageResource(UiUtils.getDrawableID(goal.getGoalType().getImageName(), GoalsActivity.this));

			// int percent = (int) ((double) goal.getCurrentProgress() / (double) goal.getWeeklyTarget());
			// String strPercent = AndroidTextUtils.padLeft(Integer.valueOf(percent).toString(), "0", 2) + "%";
			// lblGoalProgress.setText(strPercent);

			// prgGoalProgress.setMax(goal.getWeeklyTarget());
			// prgGoalProgress.setProgress(goal.getCurrentProgress());

			return root;

		}

	}

	// ************************************************************************************************************

	// ************************************************************************************************************
	// Misc.
	// ************************************************************************************************************
	/**
	 * Lazily creates the {@link #dbHelper} if required, then returns it.
	 */
	private LocalDatabaseHelper getDbHelper() {
		if (dbHelper == null) {
			dbHelper = DatabaseManager.getInstance().getDatabaseHelper(this);
		}
		return dbHelper;
	}

	/**
	 * Ensures that the correct animation is displayed when we exit this activity.
	 */
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		setResult(RESULT_CANCELED);
		overridePendingTransition(anim.push_right_in, anim.push_right_out);
	}

	/**
	 * Sets the two given calendars to the first and last days of the week. This is calculated by moving the
	 * fistDayOfWeek calendar back until a Monday is hit, and the lastDayOfWeek calendar forward till a Sunday is hit.
	 * Null may be passed into either argument; in this case, that value will not be calculated.
	 * 
	 * @param firstDayOfWeek
	 * @param lastDayOfWeek
	 */
	private void populateCalendars(Calendar firstDayOfWeek, Calendar lastDayOfWeek) {
		if (firstDayOfWeek != null) {
			while (firstDayOfWeek.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
				firstDayOfWeek.add(Calendar.DAY_OF_MONTH, -1);
			}
			firstDayOfWeek.set(Calendar.HOUR_OF_DAY, 0);
			firstDayOfWeek.set(Calendar.MINUTE, 0);
			firstDayOfWeek.set(Calendar.SECOND, 0);
			firstDayOfWeek.set(Calendar.MILLISECOND, 0);
		}

		if (lastDayOfWeek != null) {
			while (lastDayOfWeek.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
				lastDayOfWeek.add(Calendar.DAY_OF_MONTH, 1);
			}
			lastDayOfWeek.set(Calendar.HOUR_OF_DAY, 23);
			lastDayOfWeek.set(Calendar.MINUTE, 59);
			lastDayOfWeek.set(Calendar.SECOND, 59);
			lastDayOfWeek.set(Calendar.MILLISECOND, 999);
		}
	}

	/**
	 * Populates the exercise summaries list with the list of summaries for this week.
	 */
	private void populateSummariesList() {
		try {
			Dao<ExerciseSummary, String> dao = getDbHelper().getSectionHelper(NihiDBHelper.class)
					.getExerciseSummaryDAO();

			// Get the first day of this week with its time set to 00:00:00.000
			Calendar firstDayOfWeek = Calendar.getInstance();
			populateCalendars(firstDayOfWeek, null);

			// Get right now.
			Calendar now = Calendar.getInstance();

			// Get their millisecond values.
			Date timestampMin = firstDayOfWeek.getTime();
			Date timestampMax = now.getTime();

			// Get all summaries recorded between those two times.
			List<ExerciseSummary> summaries = dao.queryBuilder().where().between("date", timestampMin, timestampMax)
					.query();
			this.weeklySummaries = summaries;

		} catch (SQLException e) {
			logger.error(e);
			throw new RuntimeException(e);
		}
	}

	// ************************************************************************************************************

	// ************************************************************************************************************
	// Odin Connectivity
	// ************************************************************************************************************

	private void syncWithOdin() {
		SaveGoalsToOdinTask task = new SaveGoalsToOdinTask();
		task.execute();
	}

	private class SaveGoalsToOdinTask extends OdinAsyncTask<Void, Void, Void> {

		private ProgressDialog progressDialog;

		public SaveGoalsToOdinTask() {
			super(getOdinService());
		}

		@Override
		protected void onPreExecute() {
			String title = getString(string.goalscreen_dialog_synch);
			String message = getString(string.goalscreen_dialog_synchwithodin);
			progressDialog = ProgressDialog.show(GoalsActivity.this, title, message);
		}

		@Override
		protected Void doInBackground(IOdinService odinService, Void... notUsed) throws Exception {

			// Serialize goal data
			GoalData goalData = new GoalData();
			List<Goal> goals = getAllGoals();
			goalData.setGoals(goals.toArray(new Goal[goals.size()]));
			JsonMessage message = new JsonMessage(goalData);

			// Send to Odin
			MessageHandle handle = odinService.sendAsyncMessage(message);
			if (!wasAlreadyConnected()) {
				handle.waitForDelivery();
			}

			return null;
		}

		@Override
		protected void onPostExecute() {

			progressDialog.dismiss();

			try {
				getResult();
			} catch (Exception error) {
				AlertDialog.Builder builder = new AlertDialog.Builder(GoalsActivity.this);
				builder.setTitle(string.error).setMessage(string.goalscreen_dialog_problemsynching)
						.setCancelable(false).setPositiveButton(string.ok, new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								// finish();
							}
						}).show();
			}

		}

	}
	// ************************************************************************************************************

}
