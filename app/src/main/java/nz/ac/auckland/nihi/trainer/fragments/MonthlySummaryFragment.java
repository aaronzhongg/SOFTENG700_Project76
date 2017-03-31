package nz.ac.auckland.nihi.trainer.fragments;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import nz.ac.auckland.cs.ormlite.DatabaseManager;
import nz.ac.auckland.cs.ormlite.LocalDatabaseHelper;
import nz.ac.auckland.nihi.trainer.data.ExerciseSummary;
import nz.ac.auckland.nihi.trainer.data.NihiDBHelper;
import nz.ac.auckland.nihi.trainer.views.SmallExerciseSummaryStatsView;

import org.apache.log4j.Logger;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.j256.ormlite.dao.Dao;

public class MonthlySummaryFragment extends Fragment {

	public static final String ARG_MONTH = MonthlySummaryFragment.class.getName() + ".Month";
	private static final Logger logger = Logger.getLogger(MonthlySummaryFragment.class);

	/**
	 * The helper class we can use to access the database.
	 */
	private LocalDatabaseHelper dbHelper;

	/**
	 * Lazily creates the {@link #dbHelper} if required, then returns it.
	 * 
	 * @return
	 */
	private LocalDatabaseHelper getDbHelper() {
		if (dbHelper == null) {
			dbHelper = DatabaseManager.getInstance().getDatabaseHelper(getActivity());
		}
		return dbHelper;
	}

	/**
	 * Kill the DB connection when we kill the fragment.
	 */
	@Override
	public void onDestroy() {

		if (dbHelper != null) {
			DatabaseManager.getInstance().releaseHelper();
		}

		super.onDestroy();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		SmallExerciseSummaryStatsView statsView = new SmallExerciseSummaryStatsView(getActivity());

		// Get the month to display
		Bundle args = getArguments();
		GregorianCalendar month = new GregorianCalendar();
		month.setTimeInMillis(args.getLong(ARG_MONTH));

		// Get the minimum timestamp possible for this month.
		month.set(Calendar.DAY_OF_MONTH, 1);
		month.set(Calendar.HOUR_OF_DAY, 0);
		month.set(Calendar.MINUTE, 0);
		month.set(Calendar.SECOND, 0);
		month.set(Calendar.MILLISECOND, 0);
		Date minTimestamp = month.getTime();

		// Get the maximum timestamp possible for this month.
		month.set(Calendar.DAY_OF_MONTH, month.getActualMaximum(Calendar.DAY_OF_MONTH));
		month.set(Calendar.HOUR_OF_DAY, month.getActualMaximum(Calendar.HOUR_OF_DAY));
		month.set(Calendar.MINUTE, month.getActualMaximum(Calendar.MINUTE));
		month.set(Calendar.SECOND, month.getActualMaximum(Calendar.SECOND));
		month.set(Calendar.MILLISECOND, month.getActualMaximum(Calendar.MILLISECOND));
		Date maxTimestamp = month.getTime();

		List<ExerciseSummary> summaries = null;
		try {

			Dao<ExerciseSummary, String> dao = getDbHelper().getSectionHelper(NihiDBHelper.class)
					.getExerciseSummaryDAO();

			summaries = dao.queryBuilder().where().between("date", minTimestamp, maxTimestamp).query();

		} catch (SQLException e) {
			logger.error("onCreateView(): Error getting ExerciseSummaries within a date range");
			throw new RuntimeException(e);
		}

		ExerciseSummary average = new ExerciseSummary(summaries);
		statsView.setExerciseSummary(average);

		// Get ExerciseSummary from database by looking at the "id" argument.
		// if (summary == null) {
		// Bundle args = getArguments();
		// int summaryId = args.getInt(ARG_SUMMARY_ID);
		// try {
		// summary = getDbHelper().getSectionHelper(NihiDBHelper.class).getExerciseSummaryDAO()
		// .queryForId(summaryId);
		// } catch (SQLException e) {
		// logger.error("Error getting ExerciseSummary with id = " + summaryId + " from database", e);
		// throw new RuntimeException(e);
		// }
		// }

		return statsView;
	}

}
