package nz.ac.auckland.nihi.trainer.util;

import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.Date;

import nz.ac.auckland.cs.odin.android.api.asynctask.OdinAsyncTask;
import nz.ac.auckland.cs.odin.android.api.prefs.OdinPreferences;
import nz.ac.auckland.cs.odin.android.api.services.IOdinService;
import nz.ac.auckland.cs.odin.data.BasicStringMessage;
import nz.ac.auckland.cs.odin.data.JsonMessage;
import nz.ac.auckland.cs.odin.interconnect.common.MessageHandle;
import nz.ac.auckland.nihi.trainer.R.string;
import nz.ac.auckland.nihi.trainer.data.ExerciseNotification;
import nz.ac.auckland.nihi.trainer.data.ExerciseSummary;
import nz.ac.auckland.nihi.trainer.data.Goal;
import nz.ac.auckland.nihi.trainer.data.NihiDBHelper;
import nz.ac.auckland.nihi.trainer.data.Route;
import nz.ac.auckland.nihi.trainer.data.RouteCoordinate;
import nz.ac.auckland.nihi.trainer.data.SymptomEntry;
import nz.ac.auckland.nihi.trainer.messaging.ParticipantDataResponse;
import nz.ac.auckland.nihi.trainer.messaging.ParticipantDataResponse.RouteData;
import nz.ac.auckland.nihi.trainer.messaging.ParticipantDataResponse.SummaryData;
import nz.ac.auckland.nihi.trainer.messaging.RequestTypes;
import nz.ac.auckland.nihi.trainer.prefs.NihiPreferences;

import org.apache.log4j.Logger;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.j256.ormlite.dao.Dao;

/**
 * This class is responsible for downloading all data regarding a specific user.
 * 
 * @author Andrew Meads
 * 
 */
public class DownloadAllDataTask extends OdinAsyncTask<Void, Void, Void> {

	public interface DownloadAllDataTaskListener {

		void onDownloadAllDataComplete(boolean success);

	}

	private static final Logger logger = Logger.getLogger(DownloadAllDataTask.class);

	private final Context context;
	private final NihiDBHelper dbHelper;
	private ProgressDialog progressDialog;
	private final DownloadAllDataTaskListener completionListener;
	private final boolean disconnectWhenComplete;

	public DownloadAllDataTask(Context context, IOdinService odinService, NihiDBHelper dbHelper,
			DownloadAllDataTaskListener listener, boolean disconnectWhenComplete) {

		super(odinService, false);

		this.context = context;
		this.dbHelper = dbHelper;
		this.completionListener = listener;
		this.disconnectWhenComplete = disconnectWhenComplete;
	}

	@Override
	protected void onPreExecute() {
		String title = context.getString(string.goalscreen_dialog_synch);
		String message = context.getString(string.goalscreen_dialog_synchwithodin);
		progressDialog = ProgressDialog.show(context, title, message);
	}

	// HACK: Refactor this.
	public void doInCurrentThread(IOdinService odinService) throws Exception {
		doInBackground(odinService);
	}

	@Override
	protected Void doInBackground(IOdinService odinService, Void... unUsed) throws Exception {
		// Send request to Odin
		BasicStringMessage request = new BasicStringMessage(RequestTypes.ODIN_MESSAGE_SYNC_ALL);
		MessageHandle handle = odinService.sendRequest(request);

		// Get response
		JsonMessage responseMessage = (JsonMessage) handle.waitForResponse();
		ParticipantDataResponse response = (ParticipantDataResponse) responseMessage
				.deserialize(ParticipantDataResponse.class);

		processPreferences(response);

		processRoutes(response);

		processSummaries(response);

		processGoals(response);

		// HACK! Need to refactor this AND the HomeScreen together so they work properly.
		if (disconnectWhenComplete) {
			odinService.disconnect();
		}

		return null;
	}

	private void processGoals(ParticipantDataResponse response) throws SQLException {
		if (response.getGoals() != null) {

			Dao<Goal, String> dao = dbHelper.getGoalsDAO();

			for (Goal goal : response.getGoals()) {
				dao.createOrUpdate(goal);
			}

		}
	}

	private void processSummaries(ParticipantDataResponse response) throws SQLException {
		if (response.getSummaries() == null) {
			logger.info("doInBackground(): Received a null summaries object from Odin.");
		} else {
			logger.info("doInBackground(): Received " + response.getSummaries().length + " summaries from Odin.");
		}

		// If there's exercise summary data...
		if (response.getSummaries() != null) {

			// Get the DAO for ExerciseSummary data.
			Dao<ExerciseSummary, String> dao = dbHelper.getExerciseSummaryDAO();

			// For each returned summary...
			for (SummaryData summaryDataFromOdin : response.getSummaries()) {
				ExerciseSummary summaryFromOdin = summaryDataFromOdin.summary;

				// Search the DB for a local one that matches.
				// TODO HACK: +/- 1000 milliseconds counts as the same.
				Date timestampMin = new Date(summaryFromOdin.getDate().getTime() - 1000);
				Date timestampMax = new Date(summaryFromOdin.getDate().getTime() + 1000);
				long uid = OdinPreferences.UserID.getLongValue(context, -1);
				ExerciseSummary localSummary = dao.queryBuilder().selectColumns("userId", "date").where()
						.eq("userId", uid).and().between("date", timestampMin, timestampMax).queryForFirst();

				// If there isn't, then save it to the db.
				if (localSummary == null) {

					dao.assignEmptyForeignCollection(summaryFromOdin, "symptoms");
					dao.assignEmptyForeignCollection(summaryFromOdin, "notifications");
					dao.create(summaryFromOdin);

					for (SymptomEntry s : summaryDataFromOdin.symptoms) {
						s.setSummary(summaryFromOdin);
						summaryFromOdin.getSymptoms().add(s);
					}

					for (ExerciseNotification n : summaryDataFromOdin.notifications) {
						n.setSummary(summaryFromOdin);
						summaryFromOdin.getNotifications().add(n);
					}

					logger.info("doInBackground(): Summary from Odin wasn't found in local DB and will be added now.");

				}

				else {
					logger.info("doInBackground(): Summary from Odin was already in local DB.");
				}

			}
		}
	}

	private void processRoutes(ParticipantDataResponse response) throws SQLException {
		// If there's route data...
		if (response.getRoutes() != null) {

			// Get the DAO for Routes data.
			Dao<Route, String> routeDao = dbHelper.getRoutesDAO();

			// For each returned route...
			for (RouteData routeDataFromOdin : response.getRoutes()) {
				Route routeFromOdin = routeDataFromOdin.route;

				// Search the DB for a local one that matches.
				Route localRoute = routeDao.queryForId(routeFromOdin.getId());

				// If there isn't, then save it to the db.
				if (localRoute == null) {

					routeDao.assignEmptyForeignCollection(routeFromOdin, "gpsCoordinates");
					routeDao.create(routeFromOdin);

					for (RouteCoordinate coord : routeDataFromOdin.coords) {
						coord.setRoute(routeFromOdin);
						routeFromOdin.getGpsCoordinates().add(coord);
					}

					logger.info("doInBackground(): Route from Odin wasn't found in local DB and will be added now (ID = "
							+ routeFromOdin.getId() + ")");

				}

				else {
					logger.info("doInBackground(): Route from Odin was already in local DB (ID = "
							+ routeFromOdin.getId() + ")");
				}

			}
		}
	}

	private void processPreferences(ParticipantDataResponse response) throws FileNotFoundException {
		// Overwrite preferences with preferences returned, if any. Or, delete all preferences otherwise.
		if (response.getProfileData() != null) {
			NihiPreferences.deserialize(context, response.getProfileData().getProfileData());
			logger.info("doInBackground(): Overwrote profile data with that from Odin.");
		} else {
			logger.info("doInBackground(): Received a null profile object from Odin. Deleting all NihiPreferences.");
			NihiPreferences.clearAll(context);
		}
	}

	/**
	 * When complete, dismiss the progress dialog and report the result.
	 */
	@Override
	protected void onPostExecute() {

		progressDialog.dismiss();

		try {

			getResult();

			if (completionListener != null) {
				completionListener.onDownloadAllDataComplete(true);
			}

		} catch (Exception error) {
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setTitle(string.error)
					.setMessage(context.getString(string.datasync_dialog_cantsync) + error.getMessage())
					.setCancelable(false).setPositiveButton(string.ok, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (completionListener != null) {
								completionListener.onDownloadAllDataComplete(false);
							}
						}
					}).show();
		}

	}
}