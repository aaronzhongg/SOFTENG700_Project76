package nz.ac.auckland.nihi.trainer.activities;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import nz.ac.auckland.cs.odin.android.api.services.IOdinService;
import nz.ac.auckland.cs.odin.android.api.services.testharness.TestHarnessUtils;
import nz.ac.auckland.cs.ormlite.DatabaseManager;
import nz.ac.auckland.cs.ormlite.LocalDatabaseHelper;
import nz.ac.auckland.nihi.trainer.R.anim;
import nz.ac.auckland.nihi.trainer.R.drawable;
import nz.ac.auckland.nihi.trainer.R.id;
import nz.ac.auckland.nihi.trainer.R.layout;
import nz.ac.auckland.nihi.trainer.R.string;
import nz.ac.auckland.nihi.trainer.data.NihiDBHelper;
import nz.ac.auckland.nihi.trainer.data.Route;
import nz.ac.auckland.nihi.trainer.services.location.DummyGPSServiceImpl;
import nz.ac.auckland.nihi.trainer.services.location.GPSServiceImpl;
import nz.ac.auckland.nihi.trainer.services.location.GPSServiceListener;
import nz.ac.auckland.nihi.trainer.services.location.IGPSService;
import nz.ac.auckland.nihi.trainer.util.LocationUtils;
import nz.ac.auckland.nihi.trainer.util.RouteThumbnailLoaderTask;

import org.apache.log4j.Logger;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.j256.ormlite.dao.Dao;
import com.odin.android.services.LocalBinder;

/**
 * Displays all routes. Filterable by location and / or favourite-ness.
 * 
 * @author Andrew Meads
 * 
 */
public class RoutesActivity extends Activity implements GPSServiceListener {

	// ************************************************************************************************************
	// Instance Variables
	// ************************************************************************************************************

	public static final int NEARBY_ROUTE_MAX_DISTANCE_METERS = 5000;

	// If true, this activity will have a "selection mode" which will return a route ID rather than starting a workout.
	public static final String EXTRA_SELECTION_MODE = RoutesActivity.class.getName() + ".SelectionMode";

	// An extra signifying the ID of a route.
	public static final String EXTRA_ROUTE_ID = RoutesActivity.class.getName() + ".RouteID";

	// Signals a location update to the UI.
	private static final int MESSAGE_UPDATE_LOCATION = 12345;

	// Logs errors / info
	private static final Logger logger = Logger.getLogger(RoutesActivity.class);

	// The GPS service used to filter by location and also to view the distance between the user and a route.
	private LocalBinder<IGPSService> gpsService;

	// The helper allowing us to access the database.
	private LocalDatabaseHelper dbHelper;

	// The UI element that displays routes to the user.
	private ExpandableListView lstRoutes;

	// Which routes the user is currently viewing.
	private Viewing viewing = Viewing.All;

	// All TextViews displaying the distance to particular routes, sorted by the IDs of those routes.
	private HashMap<String, TextView> locationTextBoxes = new HashMap<String, TextView>();

	// All routes currently displayed in the UI, sorted by ID.
	private HashMap<String, Route> routesById = new HashMap<String, Route>();

	// The Handler that will delegate location updates to the UI thread.
	private Handler uihandler;

	// The Odin service connection, to be used to synchronize route data.
	private IOdinService odinService;

	// The progress dialog to be displayed to the user when we're synchronizing data.
	private ProgressDialog dlgOdinProgress;

	// True if in selection mode, false otherwise.
	private boolean isSelectionMode;

	// ************************************************************************************************************

	// ************************************************************************************************************
	// Activity Lifecycle
	// ************************************************************************************************************

	/**
	 * When we create the activity, build the UI and start the GPS service.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(layout.routes_screen);

		this.isSelectionMode = getIntent().getBooleanExtra(EXTRA_SELECTION_MODE, false);

		uihandler = new Handler(uiHandlerCallback);

		lstRoutes = (ExpandableListView) findViewById(id.lstRoutes);
		View btnAll = findViewById(id.btnAll);
		btnAll.setTag(Viewing.All);
		btnAll.setOnClickListener(viewButtonClickListener);
		View btnFav = findViewById(id.btnFavorites);
		btnFav.setTag(Viewing.Favorites);
		btnFav.setOnClickListener(viewButtonClickListener);
		View btnNearby = findViewById(id.btnNearby);
		btnNearby.setTag(Viewing.Nearby);
		btnNearby.setOnClickListener(viewButtonClickListener);

		if (TestHarnessUtils.isTestHarness()) {
			bindService(new Intent(this, DummyGPSServiceImpl.class), gpsConn, BIND_AUTO_CREATE);
		} else {
			bindService(new Intent(this, GPSServiceImpl.class), gpsConn, BIND_AUTO_CREATE);
		}
	}

	/**
	 * When we finish the activity, unbind from the GPS service.
	 */
	@Override
	protected void onDestroy() {

		if (gpsService != null) {
			gpsService.getService().setGPSServiceListener(null);
			unbindService(gpsConn);
		}

		super.onDestroy();
	}

	// @Override
	// public boolean onCreateOptionsMenu(Menu menu) {
	// MenuInflater inf = new MenuInflater(this);
	// inf.inflate(R.menu.routes_screen, menu);
	// return true;
	// }

	// ************************************************************************************************************

	// ************************************************************************************************************
	// GPS Service Interaction
	// ************************************************************************************************************
	/**
	 * When we've obtained a service connection for the first time, populate the list view.
	 */
	private final ServiceConnection gpsConn = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
		}

		@SuppressWarnings("unchecked")
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			gpsService = (LocalBinder<IGPSService>) service;
			gpsService.getService().setGPSServiceListener(RoutesActivity.this);
			populateListView();
		}
	};

	@Override
	public void gpsConnectivityChanged(boolean isConnected) {

	}

	@Override
	public void newLocationReceived(Location location) {
		uihandler.obtainMessage(MESSAGE_UPDATE_LOCATION, location).sendToTarget();
	}

	/**
	 * Handles messages to update the location information on the UI.
	 */
	private final Handler.Callback uiHandlerCallback = new Handler.Callback() {

		@Override
		public boolean handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MESSAGE_UPDATE_LOCATION:
				DecimalFormat formatter = new DecimalFormat("#.##");
				synchronized (locationTextBoxes) {
					for (String id : locationTextBoxes.keySet()) {
						TextView txtDistance = locationTextBoxes.get(id);
						Route route = routesById.get(id);
						double routeDistance = LocationUtils.getDistanceToRouteInMeters(route, (Location) msg.obj);
						txtDistance.setText(formatter.format(routeDistance / 1000.0));
					}
				}
				return true;
			default:
				return false;
			}
		}
	};

	// ************************************************************************************************************

	// ************************************************************************************************************
	// Odin Service Interaction
	// ************************************************************************************************************
	// private void beginOdinSynchronize() {
	// // Show progress dialog
	// dlgOdinProgress = ProgressDialog.show(this, "Synchronizing", "Synchronizing with Odin...", true);
	//
	// // Start odin service
	// bindService(new Intent(this, OdinService.class), odinConn, BIND_AUTO_CREATE);
	// }
	//
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
	//
	// /**
	// * The task that requests exercise summaries from Odin, picks out those that we don't have in our local database,
	// * then displays them in the UI thread.
	// *
	// * @author anhyd_000
	// *
	// */
	// private class SynchronizeWithOdinTask extends AsyncTask<Void, Void, List<Route>> {
	//
	// @Override
	// protected List<Route> doInBackground(Void... params) {
	//
	// try {
	//
	// // Make sure we're connected to Odin.
	// boolean needToConnect = odinService.getConnectionStatus() == EndpointConnectionStatus.DISCONNECTED;
	// // if (needToConnect) {
	// // odinService.beginConnect();
	// // }
	// // odinService.waitFor(EndpointConnectionStatus.CONNECTED);
	// odinService.connect();
	// logger.info("SynchronizeWithOdinTask.doInBackground(): Connected to Odin.");
	//
	// // Send the request, wait for response.
	// BasicStringMessage request = new BasicStringMessage(Route.ODIN_MESSAGE_SYNC_ROUTES);
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
	// // Get the response.
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
	// logger.info("SynchronizeWithOdinTask.doInBackground(): Received a null routes object from Odin.");
	// } else {
	// logger.info("SynchronizeWithOdinTask.doInBackground(): Received " + response.getRoutes().length
	// + " routes from Odin.");
	// }
	//
	// // If the response has no routes, get outta here.
	// if (response.getRoutes() == null || response.getRoutes().length == 0) {
	// return null;
	// }
	//
	// // Get the DAO for Routes data.
	// Dao<Route, String> dao = dbHelper.getSectionHelper(NihiDBHelper.class).getRoutesDAO();
	//
	// // For each returned route...
	// for (RouteData routeDataFromOdin : response.getRoutes()) {
	// Route routeFromOdin = routeDataFromOdin.route;
	//
	// // Search the DB for a local one that matches.
	// Route localRoute = dao.queryForId(routeFromOdin.getId());
	//
	// // If there isn't, then save it to the db.
	// if (localRoute == null) {
	//
	// dao.assignEmptyForeignCollection(routeFromOdin, "gpsCoordinates");
	// dao.create(routeFromOdin);
	//
	// for (RouteCoordinate coord : routeDataFromOdin.coords) {
	// coord.setRoute(routeFromOdin);
	// routeFromOdin.getGpsCoordinates().add(coord);
	// }
	//
	// logger.info("SynchronizeWithOdinTask.doInBackground(): Route from Odin wasn't found in local DB and will be added now (ID = "
	// + routeFromOdin.getId() + ")");
	//
	// }
	//
	// else {
	// logger.info("SynchronizeWithOdinTask.doInBackground(): Route from Odin was already in local DB (ID = "
	// + routeFromOdin.getId() + ")");
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
	// protected void onPostExecute(java.util.List<Route> result) {
	//
	// // Close the progress dialog and reload the page.
	// populateListView();
	// dlgOdinProgress.dismiss();
	//
	// }
	//
	// }

	// ************************************************************************************************************

	// ************************************************************************************************************
	// UI Interaction
	// ************************************************************************************************************
	/**
	 * When one of the three top buttons is clicked, updates the list view to show the corresponding routes.
	 */
	private final View.OnClickListener viewButtonClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			if (v.getTag() != viewing) {
				viewing = (Viewing) v.getTag();
				populateListView();
			}
		}
	};

	/**
	 * When an "add / remove from favourites" button is clicked, toggles the corresponding route's "isFavourite" status.
	 * Then saves the new status in the database.
	 */
	private final CompoundButton.OnCheckedChangeListener toggleFavouritesButtonClickListener = new CompoundButton.OnCheckedChangeListener() {

		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			Route route = (Route) buttonView.getTag();
			route.setFavorite(isChecked);

			try {
				getDbHelper().getSectionHelper(NihiDBHelper.class).getRoutesDAO().update(route);
			} catch (SQLException e) {
				logger.error(e);
				throw new RuntimeException(e);
			}

			int icon = isChecked ? drawable.nihi_ic_review_favorite : drawable.nihi_ic_review_nonfavorite;
			buttonView.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0);

		}
	};

	/**
	 * When the "Start Workout" button is clicked, launches the Workout activity with its goal preset to a route goal
	 * with the corresponding route.
	 */
	private final View.OnClickListener startWorkoutButtonClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {

			Route route = (Route) v.getTag();

			// If this was selection mode, pass the route ID back to the calling activity.
			if (isSelectionMode) {
				Intent result = new Intent();
				result.putExtra(EXTRA_ROUTE_ID, route.getId());
				setResult(RESULT_OK, result);
				finish();
			}

			// otherwise launch the workout activity.
			else {
				Intent intent = new Intent(getApplicationContext(), WorkoutActivity.class);
				intent.putExtra(EXTRA_ROUTE_ID, route.getId());
				startActivity(intent);
			}
		}
	};

	/**
	 * Populates the UI with a subset of all routes in the database, depending on the "viewing" variable.
	 */
	private void populateListView() {

		try {
			List<Route> routes = null;
			Dao<Route, String> routeDao = getDbHelper().getSectionHelper(NihiDBHelper.class).getRoutesDAO();

			// Load the subset of routes to display
			switch (viewing) {
			case Nearby:
				findViewById(id.underlineNearby).setVisibility(View.VISIBLE);
				findViewById(id.underlineAll).setVisibility(View.INVISIBLE);
				findViewById(id.underlineFavorites).setVisibility(View.INVISIBLE);
				// TODO If possible, formulate a min / max lat / lng. Rather than getting everything and filtering by
				// distance.
				// TODO This has the potential to be heinously slow.
				final Location loc = gpsService == null ? null : gpsService.getService().getLastKnownLocation();
				if (loc != null) {
					List<Route> temp = routeDao.queryForAll();
					routes = new ArrayList<Route>(temp);

					// Filter for only nearby
					for (int i = 0; i < routes.size(); i++) {
						double distance = LocationUtils.getDistanceToRouteInMeters(routes.get(i), loc);
						if (distance > NEARBY_ROUTE_MAX_DISTANCE_METERS) {
							routes.remove(i);
							i--; // Since we removed one, put the index back one.
						}
					}

					// Sort by distance
					Collections.sort(routes, new Comparator<Route>() {
						@Override
						public int compare(Route lhs, Route rhs) {
							double lhsDistance = LocationUtils.getDistanceToRouteInMeters(lhs, loc);
							double rhsDistance = LocationUtils.getDistanceToRouteInMeters(rhs, loc);
							return Double.valueOf(lhsDistance).compareTo(Double.valueOf(rhsDistance));
						}
					});
				}
				break;

			case Favorites:
				findViewById(id.underlineNearby).setVisibility(View.INVISIBLE);
				findViewById(id.underlineAll).setVisibility(View.INVISIBLE);
				findViewById(id.underlineFavorites).setVisibility(View.VISIBLE);
				routes = routeDao.queryBuilder().where().eq("isFavorite", true).query();
				break;

			case All:
				findViewById(id.underlineNearby).setVisibility(View.INVISIBLE);
				findViewById(id.underlineAll).setVisibility(View.VISIBLE);
				findViewById(id.underlineFavorites).setVisibility(View.INVISIBLE);
				routes = routeDao.queryForAll();
				break;
			}

			// Update the UI to display these routes and no others.
			synchronized (locationTextBoxes) {
				routesById.clear();
				locationTextBoxes.clear();
				if (routes != null) {
					for (Route r : routes) {
						routesById.put(r.getId(), r);
					}
					lstRoutes.setAdapter(new RoutesAdapter(routes));
				} else {
					lstRoutes.setAdapter((ExpandableListAdapter) null);
				}
			}

		} catch (SQLException e) {
			logger.error(e);
			throw new RuntimeException(e);
		}

	}

	// /**
	// * When the "synchronize" button is clicked, begins synchronizing route data with Odin.
	// *
	// * @param item
	// * @return
	// */
	// @Override
	// public boolean onOptionsItemSelected(MenuItem item) {
	//
	// if (item.getItemId() == id.action_synchronize_data) {
	// beginOdinSynchronize();
	// return true;
	// } else {
	// return super.onOptionsItemSelected(item);
	// }
	// }

	// ************************************************************************************************************

	// ************************************************************************************************************
	// Routes Adapter
	// ************************************************************************************************************
	/**
	 * This adapter will generate the UI elements to show collapsed and expanded versions of the Routes.
	 * 
	 * @author Andrew Meads
	 * 
	 */
	private class RoutesAdapter extends BaseExpandableListAdapter {

		// The routes to display to the user
		private final List<Route> routes;

		public RoutesAdapter(List<Route> routes) {
			this.routes = routes;
		}

		/**
		 * Generates the expanded view, containing a large map image and other information.
		 */
		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView,
				ViewGroup parent) {

			View root = convertView;
			if (root == null) {
				LayoutInflater li = LayoutInflater.from(RoutesActivity.this);
				root = li.inflate(layout.routes_screen_routeview_expanded, parent, false);
			}

			Route route = routes.get(groupPosition);
			if (route != root.getTag()) {
				root.setTag(route);

				TextView txtCreatedBy = (TextView) root.findViewById(id.txtCreatedBy);
				txtCreatedBy.setText(route.getCreatorName());

				ImageView imgThumbnail = (ImageView) root.findViewById(id.imgMap);

				// if (route.getThumbnailFileName() != null && new File(route.getThumbnailFileName()).exists()) {
				// Bitmap image = BitmapFactory.decodeFile(route.getThumbnailFileName());
				// imgThumbnail.setImageBitmap(image);
				// } else {
				generateThumbnail(parent.getContext(), route, imgThumbnail, null);
				// }

				ToggleButton btnToggleFavourites = (ToggleButton) root.findViewById(id.btnToggleFavourites);
				btnToggleFavourites.setChecked(route.isFavorite());
				btnToggleFavourites.setTag(route);
				btnToggleFavourites.setOnCheckedChangeListener(toggleFavouritesButtonClickListener);
				int icon = btnToggleFavourites.isChecked() ? drawable.nihi_ic_review_favorite
						: drawable.nihi_ic_review_nonfavorite;
				btnToggleFavourites.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0);

				View btnStartWorkout = root.findViewById(id.btnStartWorkout);
				if (isSelectionMode) {
					((Button) btnStartWorkout).setText(string.routescreen_button_selectroute);
				}
				btnStartWorkout.setTag(route);
				btnStartWorkout.setOnClickListener(startWorkoutButtonClickListener);

			}
			return root;
		}

		/**
		 * Generates the collapsed view, containing a small map image and location and length info.
		 */
		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {

			View root = convertView;
			if (root == null) {
				LayoutInflater li = LayoutInflater.from(RoutesActivity.this);
				root = li.inflate(layout.routes_screen_routeview_collapsed, parent, false);
			}

			Route route = routes.get(groupPosition);

			if (route != root.getTag()) {
				root.setTag(route);

				ImageView imgThumbnail = (ImageView) root.findViewById(id.imgRouteThumbnail);

				// if (route.getThumbnailFileName() != null && new File(route.getThumbnailFileName()).exists()) {
				// Bitmap image = BitmapFactory.decodeFile(route.getThumbnailFileName());
				// imgThumbnail.setImageBitmap(image);
				// } else {
				//
				View progressBar = root.findViewById(id.prgThumbnailLoading);
				generateThumbnail(parent.getContext(), route, imgThumbnail, progressBar);
				// }

				// TextView txtName = (TextView) root.findViewById(id.txtRouteName);
				// txtName.setText(route.getName());

				DecimalFormat formatter = new DecimalFormat("#.##");
				TextView txtLength = (TextView) root.findViewById(id.txtRouteLength);
				double routeLength = LocationUtils.getRouteLengthInMeters(route);
				txtLength.setText(formatter.format(routeLength / 1000.0));

				// Distance-to-route text box.
				TextView txtDistance = (TextView) root.findViewById(id.txtDistanceToRoute);
				if (gpsService == null || !gpsService.getService().isConnected()
						|| gpsService.getService().getLastKnownLocation() == null) {

					txtDistance.setText("?");

				} else {
					double routeDistance = LocationUtils.getDistanceToRouteInMeters(route, gpsService.getService()
							.getLastKnownLocation());
					txtDistance.setText(formatter.format(routeDistance / 1000.0));
				}

				synchronized (locationTextBoxes) {
					locationTextBoxes.put(route.getId(), txtDistance);
				}
			}

			return root;
		}

		/**
		 * Sets up a background task to load a thumbnail and display it in the given image view when done.
		 */
		private void generateThumbnail(Context context, Route route, ImageView imageView, View progressBar) {
			try {
				Dao<Route, String> routeDao = getDbHelper().getSectionHelper(NihiDBHelper.class).getRoutesDAO();

				RouteThumbnailLoaderTask loadTask = new RouteThumbnailLoaderTask(context, route, routeDao, imageView,
						progressBar);
				loadTask.execute();

			} catch (SQLException e) {
				logger.error(e);
				throw new RuntimeException(e);
			}
		}

		@Override
		public Object getChild(int groupPosition, int childPosition) {
			return routes.get(groupPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return groupPosition;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return 1;
		}

		@Override
		public Object getGroup(int groupPosition) {
			return routes.get(groupPosition);
		}

		@Override
		public int getGroupCount() {
			return routes.size();
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return false;
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
	 * Enumerates the possible kinds of routes the user is currently viewing
	 */
	private enum Viewing {
		All, Favorites, Nearby;
	}
	// ************************************************************************************************************
}
