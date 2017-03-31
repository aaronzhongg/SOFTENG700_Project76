package nz.ac.auckland.nihi.trainer.services.downloader;

import nz.ac.auckland.cs.odin.android.api.asynctask.OdinAsyncTask;
import nz.ac.auckland.cs.odin.android.api.services.IOdinService;
import nz.ac.auckland.cs.odin.android.api.services.OdinAccessingService;
import nz.ac.auckland.cs.odin.data.BasicStringMessage;
import nz.ac.auckland.cs.odin.data.JsonMessage;
import nz.ac.auckland.cs.odin.interconnect.common.MessageHandle;
import nz.ac.auckland.cs.ormlite.DatabaseManager;
import nz.ac.auckland.cs.ormlite.LocalDatabaseHelper;
import nz.ac.auckland.nihi.trainer.data.NihiDBHelper;
import nz.ac.auckland.nihi.trainer.data.Route;
import nz.ac.auckland.nihi.trainer.data.RouteCoordinate;
import nz.ac.auckland.nihi.trainer.messaging.ParticipantDataResponse;
import nz.ac.auckland.nihi.trainer.messaging.ParticipantDataResponse.RouteData;
import nz.ac.auckland.nihi.trainer.messaging.RequestTypes;

import org.apache.log4j.Logger;

import android.content.Intent;
import android.os.IBinder;

import com.j256.ormlite.dao.Dao;

/**
 * A service which will, when onStartCommand is called, sync some data with the server.
 * 
 * @author Andrew Meads
 * 
 */
public class DownloaderService extends OdinAccessingService {

	private static final Logger logger = Logger.getLogger(DownloaderService.class);
	private LocalDatabaseHelper dbHelper;

	private MySyncTask mySyncTask;

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

	@Override
	public void onDestroy() {

		super.onDestroy();
	}

	@Override
	public synchronized int onStartCommand(Intent intent, int flags, int startId) {

		if (mySyncTask == null) {
			mySyncTask = new MySyncTask();
			mySyncTask.execute();
		}

		return START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	private class MySyncTask extends OdinAsyncTask<Void, Void, Void> {

		public MySyncTask() {
			super(getOdinService());
		}

		@Override
		protected Void doInBackground(IOdinService odinService, Void... args) throws Exception {

			try {
				logger.info("syncTask.run(): Beginning route sync task.");

				// Send message to get route data, await response
				BasicStringMessage syncMessage = new BasicStringMessage(RequestTypes.ODIN_MESSAGE_SYNC_ROUTES);
				MessageHandle handle = odinService.sendRequest(syncMessage);
				JsonMessage responseMessage = (JsonMessage) handle.waitForResponse();
				ParticipantDataResponse response = (ParticipantDataResponse) responseMessage
						.deserialize(ParticipantDataResponse.class);

				// Add given routes to database
				if (response.getRoutes() != null && response.getRoutes().length > 0) {
					Dao<Route, String> dao = getDbHelper().getSectionHelper(NihiDBHelper.class).getRoutesDAO();
					for (RouteData routeFromOdin : response.getRoutes()) {

						// Check if already exists
						Route localRoute = dao.queryForId(routeFromOdin.route.getId());
						if (localRoute == null) {

							// If not, create.
							dao.assignEmptyForeignCollection(routeFromOdin.route, "gpsCoordinates");
							dao.create(routeFromOdin.route);
							for (RouteCoordinate coord : routeFromOdin.coords) {
								routeFromOdin.route.getGpsCoordinates().add(coord);
							}

						}
					}
				}

				logger.info("syncTask.run(): Route sync task completed successfully.");
			} finally {

				stopSelf();

			}

			return null;
		}

		@Override
		protected void onPostExecute() {
			synchronized (DownloaderService.this) {
				mySyncTask = null;
			}
		}
	}

}
