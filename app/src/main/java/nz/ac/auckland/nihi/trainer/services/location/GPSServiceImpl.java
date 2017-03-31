package nz.ac.auckland.nihi.trainer.services.location;

import nz.ac.auckland.cs.android.utils.BackgroundLooperThread;
import nz.ac.auckland.nihi.trainer.R.integer;
import nz.ac.auckland.nihi.trainer.R.string;

import org.apache.log4j.Logger;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.odin.android.services.LocalBinder;

public class GPSServiceImpl extends Service implements IGPSService {

	private static final Logger logger = Logger.getLogger(GPSServiceImpl.class);

	/**
	 * True if we're currently receiving GPS data, false otherwise.
	 */
	private boolean locationServicesEnabled = false;

	/**
	 * The client object that allows us to receive GPS updates from Location Services.
	 */
	private GoogleApiClient locationClient;

	/**
	 * The request object that will be filled with the necessary information to make a request to Location Services to
	 * provide us with periodic location updates.
	 */
	private LocationRequest locationRequest;

	/**
	 * The listener to notify of changes and location updates, if any.
	 */
	private GPSServiceListener listener;

	/**
	 * The last known location, if any.
	 */
	private Location lastKnownLocation;

	/**
	 * The thread on which location updates will be served.
	 */
	private BackgroundLooperThread looperThread;

	/**
	 * When the service is first created, connect to GPS.
	 */
	@Override
	public void onCreate() {
		super.onCreate();

		looperThread = new BackgroundLooperThread();
		looperThread.start();

		// Create the location client
		// locationClient = new LocationClient(this, gpsConnectionCallbacks, gpsConnectionFailedListener);
		if (locationClient == null) {
			locationClient = new GoogleApiClient.Builder(this)
					.addConnectionCallbacks(gpsConnectionCallbacks)
					.addOnConnectionFailedListener(gpsConnectionFailedListener)
					.addApi(LocationServices.API)
					.build();
		}

		// Create a request object that asks for high-accuracy, frequent GPS updates.
		locationRequest = LocationRequest.create();
		int updateInterval = getResources().getInteger(integer.location_update_interval_millis);
		locationRequest.setInterval(updateInterval);
		locationRequest.setFastestInterval(updateInterval);
		locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

		// Connect
		locationClient.connect();
	}

	@Override
	public boolean onUnbind(Intent intent) {

		if (locationClient != null) {
			// locationClient.removeLocationUpdates(locationListener);
            LocationServices.FusedLocationApi.removeLocationUpdates(locationClient, locationListener);
			locationClient.disconnect();
		}

		looperThread.quit();

		return false;
	}

	/**
	 * Returns a value indicating whether we're connected to location services.
	 */
	@Override
	public boolean isConnected() {
		return locationServicesEnabled;
	}

	/**
	 * Gets the last known location if any.
	 */
	@Override
	public Location getLastKnownLocation() {
		if (lastKnownLocation != null) {
			return lastKnownLocation;
		} else if (locationClient != null && locationClient.isConnected()) {
			return LocationServices.FusedLocationApi.getLastLocation(locationClient);
		} else {
			return null;
		}
	}

	/**
	 * Sets the listener that will be notified of location and connectivity updates.
	 */
	@Override
	public void setGPSServiceListener(GPSServiceListener listener) {
		this.listener = listener;
	}

	/**
	 * This handler will be called when we connect or disconnect from the Location service.
	 */
	private final GoogleApiClient.ConnectionCallbacks gpsConnectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {

		/**
		 * Called when we connect to the location service. Request that the service sends us regular location updates.
		 */
		@Override
		public void onConnected(Bundle arg0) {

			// Notify user of connection
			Toast.makeText(GPSServiceImpl.this, string.locationservice_toast_connected, Toast.LENGTH_SHORT).show();
			logger.info("onConnected(): Connected to location services");

			// Begin receiving location updates
			try {
                LocationServices.FusedLocationApi.requestLocationUpdates(locationClient, locationRequest, locationListener, looperThread.getLooper());
				// locationClient.requestLocationUpdates(locationRequest, locationListener, looperThread.getLooper());
			} catch (InterruptedException e) {
				logger.error(e);
				throw new RuntimeException(e);
			}

			// Notify listener of connection
			locationServicesEnabled = true;
			if (listener != null) {
				listener.gpsConnectivityChanged(true);
			}

		}

		/**
		 * Called when we purposefully disconnect from location services.
		 */
		@Override
		public void onConnectionSuspended(int i) {
			// Notify user of disconnection
			Toast.makeText(GPSServiceImpl.this, string.locationservice_toast_disconnected, Toast.LENGTH_SHORT).show();
			logger.info("onDisconnected(): Disconnected from location services");

			// Notify listener of disconnection
			locationServicesEnabled = false;
			if (listener != null) {
				listener.gpsConnectivityChanged(false);
			}
		}
	};

	/**
	 * This handler is called when connection to Google Play Services fails. Hopefully this will never be called!!
	 */
	private final GoogleApiClient.OnConnectionFailedListener gpsConnectionFailedListener = new GoogleApiClient.OnConnectionFailedListener() {

		@Override
		public void onConnectionFailed(ConnectionResult arg0) {

			// Notify user of GPS failure
			Toast.makeText(GPSServiceImpl.this, string.locationservice_toast_failed, Toast.LENGTH_SHORT).show();
			logger.warn("onConnectionFailed(): could not connect to location services!");

			// Notify listener of GPS failure
			locationServicesEnabled = false;
			if (listener != null) {
				listener.gpsConnectivityChanged(false);
			}
		}
	};

	/**
	 * This listener is notified whenever the user's location changes.
	 * 
	 * Updates the map to show that location.
	 */
	private final LocationListener locationListener = new LocationListener() {

		@Override
		public void onLocationChanged(Location location) {

			logger.debug("onLocationChanged(): Received new location from Location Services: [lat="
					+ location.getLatitude() + ", lng=" + location.getLongitude() + ", hasSpeed=" + location.hasSpeed()
					+ ", speed=" + location.getSpeed() + "m/s, acc=" + location.getAccuracy() + "m, provider="
					+ location.getProvider() + "]");

			if (listener != null) {
				listener.newLocationReceived(location);
			}
		}
	};

	private final LocalBinder<IGPSService> binder = new LocalBinder<IGPSService>(this);

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public void onDestroy() {

		binder.close();
		super.onDestroy();
	}

}
