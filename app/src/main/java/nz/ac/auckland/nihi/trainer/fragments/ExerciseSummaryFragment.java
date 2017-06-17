package nz.ac.auckland.nihi.trainer.fragments;

import java.sql.SQLException;

import nz.ac.auckland.cs.ormlite.DatabaseManager;
import nz.ac.auckland.cs.ormlite.LocalDatabaseHelper;
import nz.ac.auckland.nihi.trainer.R;
import nz.ac.auckland.nihi.trainer.R.drawable;
import nz.ac.auckland.nihi.trainer.R.id;
import nz.ac.auckland.nihi.trainer.data.DatabaseHelper;
import nz.ac.auckland.nihi.trainer.data.ExerciseSummary;
import nz.ac.auckland.nihi.trainer.data.NihiDBHelper;
import nz.ac.auckland.nihi.trainer.data.Route;
import nz.ac.auckland.nihi.trainer.util.RouteThumbnailLoaderTask;
import nz.ac.auckland.nihi.trainer.views.LargeExerciseSummaryStatsView;
import nz.ac.auckland.nihi.trainer.views.NotificationsView;
import nz.ac.auckland.nihi.trainer.views.SmallExerciseSummaryStatsView;
import nz.ac.auckland.nihi.trainer.views.SymptomsView;

import org.apache.log4j.Logger;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ScrollView;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;

/**
 * This fragment provides a view of one exercise session's worth of exercise summary data. It inclides the minimum,
 * average, and maximum heart rate and speed reached during the session, the duration, distance and used energy, any
 * symptoms noticed during the session, any notifications received from the trainer, and a map showing the locaiton of
 * the session with an option to save that location for future reference / use.
 * 
 * @author Andrew Meads
 * 
 */
public class ExerciseSummaryFragment extends Fragment {

	private static final Logger logger = Logger.getLogger(ExerciseSummaryFragment.class);

	public static final String ARG_SUMMARY_ID = ExerciseSummaryFragment.class + ".Args.SummaryID";

	// private static final String TAG_SYMPTOMS_TAB = ExerciseSummaryFragment.class.getName() + ".Symptoms";
	// private static final String TAG_NOTIFICATIONS_TAB = ExerciseSummaryFragment.class.getName() + ".Notifications";
	// private static final String TAG_MAP_TAB = ExerciseSummaryFragment.class.getName() + ".Map";

	/**
	 * The entire view hierarchy of this fragment
	 */
	private View rootView;

	/**
	 * The map view, for showing route information
	 */
	// private MapView mapView;

	/**
	 * An image of the route will be displayed here, if any.
	 */
	private ImageView imgMap;

	/**
	 * If there is no route, this panel will be hidden.
	 */
	private View pnlMap;

	/**
	 * The view which shows symptoms information.
	 */
	private SymptomsView symptomsView;

	/**
	 * The view which shows notifications.
	 */
	private NotificationsView notificationsView;

	/**
	 * The helper class we can use to access the database.
	 */
	private DatabaseHelper dbHelper;

	/**
	 * The {@link ExerciseSummary} this page views. Populated by a call to the database.
	 */
	private ExerciseSummary summary;

	/**
	 * Lazily creates the {@link #dbHelper} if required, then returns it.
	 * 
	 * @return
	 */
	private DatabaseHelper getDbHelper() {
		if (dbHelper == null) {
			dbHelper = OpenHelperManager.getHelper(getActivity(), DatabaseHelper.class);
		}
		return dbHelper;
	}

	/**
	 * Delegate "OnDestroy" to the map view (required by the API)
	 */
	@Override
	public void onDestroy() {

		if (dbHelper != null) {
			DatabaseManager.getInstance().releaseHelper();
		}

		// if (mapView != null) {
		// mapView.onDestroy();
		// }

		super.onDestroy();
	}

	/**
	 * Delegate "OnResume" to the map view (required by the API)
	 */
	// @Override
	// public void onResume() {
	// super.onResume();
	//
	// if (mapView != null) {
	// mapView.onResume();
	// }
	// }

	/**
	 * Delegate "OnPause" to the map view (required by the API)
	 */
	// @Override
	// public void onPause() {
	//
	// if (mapView != null) {
	// mapView.onPause();
	// }
	//
	// super.onPause();
	// }

	/**
	 * Delegate "OnLowMemory" to the map view (required by the API)
	 */
	// @Override
	// public void onLowMemory() {
	// if (mapView != null) {
	// mapView.onLowMemory();
	// }
	// super.onLowMemory();
	// }

	private final CompoundButton.OnCheckedChangeListener toggleFavouritesButtonClickListener = new CompoundButton.OnCheckedChangeListener() {

		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			Route route = (Route) buttonView.getTag();
			route.setFavorite(isChecked);

			try {
				getDbHelper().getRoutesDAO().update(route);
			} catch (SQLException e) {
				logger.error(e);
				throw new RuntimeException(e);
			}

			int icon = isChecked ? drawable.nihi_ic_review_favorite : drawable.nihi_ic_review_nonfavorite;
			buttonView.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0);

		}
	};

	/**
	 * When asked to create a view, this fragment will inflate the exercise summary fragment layout. It will then
	 * populate the Symptoms, Notifications, and Map tabs within that fragment with additional layouts according to the
	 * tab.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		// Get ExerciseSummary from database by looking at the "id" argument.
		if (summary == null) {
			Bundle args = getArguments();
			String summaryId = args.getString(ARG_SUMMARY_ID);
//			try {
//				summary = getDbHelper().getExerciseSummaryDAO()
//						.queryForId(summaryId);
//			} catch (SQLException e) {
//				logger.error("Error getting ExerciseSummary with id = " + summaryId + " from database", e);
//				throw new RuntimeException(e);
//			}
		}

		// Inflate main layout
		rootView = inflater.inflate(R.layout.fragment_exercise_summary, container, false);

		// Assign values to small and large stats views
		SmallExerciseSummaryStatsView smallView = (SmallExerciseSummaryStatsView) rootView
				.findViewById(id.collapsedView);
		LargeExerciseSummaryStatsView largeView = (LargeExerciseSummaryStatsView) rootView
				.findViewById(id.expandedView);

		smallView.setExerciseSummary(summary);
		largeView.setExerciseSummary(summary);

		// Set the date
		// TextView txtDate = (TextView) rootView.findViewById(id.txtDate);
		// GregorianCalendar cal = new GregorianCalendar();
		// cal.setTime(summary.getDate());
		// String weekday = cal.getDisplayName(GregorianCalendar.DAY_OF_WEEK, GregorianCalendar.LONG,
		// Locale.getDefault());
		// String dayOfMonth = "" + cal.get(GregorianCalendar.DAY_OF_MONTH);
		// String month = cal.getDisplayName(GregorianCalendar.MONTH, GregorianCalendar.LONG, Locale.getDefault());
		// String date = weekday + " " + dayOfMonth + " " + month;
		// txtDate.setText(date.toUpperCase(Locale.getDefault()));

		// Load personal details and set the absolute max HR (for %HRR calculations)
		// int absMaxHR = NihiPreferences.MaxHeartRate.getIntValue(getActivity(), -1);
		// if (absMaxHR > 0) {
		// smallView.setAbsoluteMaximumHR(absMaxHR);
		// largeView.setAbsoluteMaximumHR(absMaxHR);
		// }

		// Get map view, and call its "onCreate" method, which is required behavior when using this view.
		// mapView = (MapView) rootView.findViewById(id.map);
		// mapView.onCreate(savedInstanceState);
		imgMap = (ImageView) rootView.findViewById(id.imgMap);
		pnlMap = rootView.findViewById(id.pnlMap);

		// Display the route thumbnail, or hide the route section if no route was followed for this summary.
		// TODO Offload this to background task
		if (summary.getFollowedRoute() == null) {
			pnlMap.setVisibility(View.GONE);
		} else {
			try {
				Dao<Route, Integer> routeDao = getDbHelper().getRoutesDAO();
				routeDao.refresh(summary.getFollowedRoute());
				// String fileName = summary.getFollowedRoute().getThumbnailFileName();
				//
				// // If the filename is not null, display it now.
				// if (fileName != null && new File(fileName).exists()) {
				// Bitmap image = BitmapFactory.decodeFile(fileName);
				// imgMap.setImageBitmap(image);
				// }
				//
				// // Otherwise start the task that will generate the file and display it once loaded.
				// else {
				View prgLoadThumbnail = pnlMap.findViewById(id.prgLoadThumbnail);
				RouteThumbnailLoaderTask task = new RouteThumbnailLoaderTask(getActivity(), summary.getFollowedRoute(),
						routeDao, imgMap, prgLoadThumbnail);
				task.execute();
				// }

				CompoundButton btnSaveToFavorites = (CompoundButton) rootView.findViewById(id.btnSaveToFavorites);
				btnSaveToFavorites.setTag(summary.getFollowedRoute());
				btnSaveToFavorites.setChecked(summary.getFollowedRoute().isFavorite());
				int icon = btnSaveToFavorites.isChecked() ? drawable.nihi_ic_review_favorite
						: drawable.nihi_ic_review_nonfavorite;
				btnSaveToFavorites.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0);
				btnSaveToFavorites.setOnCheckedChangeListener(toggleFavouritesButtonClickListener);

			} catch (SQLException e) {
				logger.error("Error getting Route with id = " + summary.getFollowedRoute().getId() + " from database",
						e);
				throw new RuntimeException(e);
			}
		}

		// Symptoms
		symptomsView = (SymptomsView) rootView.findViewById(id.symptomsView);
		symptomsView.setSymptoms(summary);

		// Notifications
		notificationsView = (NotificationsView) rootView.findViewById(id.notificationsView);
		notificationsView.setNotifications(summary);

		// Set the view up in a scroll viewer.
		ScrollView scroller = new ScrollView(getActivity());
		scroller.setFillViewport(true);
		scroller.addView(rootView);

		return scroller;

	}

}
