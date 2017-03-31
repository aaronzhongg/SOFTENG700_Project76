package nz.ac.auckland.nihi.trainer.services.location;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import nz.ac.auckland.nihi.trainer.R.raw;

import org.apache.log4j.Logger;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;

import com.odin.android.services.LocalBinder;

public class DummyGPSServiceImpl extends Service implements IGPSService {

	private static final Logger logger = Logger.getLogger(DummyGPSServiceImpl.class);

	/**
	 * True if we're currently receiving GPS data, false otherwise.
	 */
	private boolean locationServicesEnabled = false;

	/**
	 * The listener to notify of changes and location updates, if any.
	 */
	private GPSServiceListener listener;

	private final Object listenerLock = new Object();

	/**
	 * The last known location, if any.
	 */
	private Location lastKnownLocation;

	/**
	 * The thread on which location updates will be served.
	 */
	// private BackgroundLooperThread looperThread;

	private Thread dummyGPSThread;

	private final ArrayList<Location> fakeLocations = new ArrayList<Location>();

	/**
	 * When the service is first created, connect to GPS.
	 */
	@Override
	public void onCreate() {
		super.onCreate();

		// looperThread = new BackgroundLooperThread();
		// looperThread.start();

		dummyGPSThread = new Thread(dummyGPSTask, "Dummy GPS Thread");
		dummyGPSThread.setDaemon(true);
		dummyGPSThread.start();

	}

	@Override
	public boolean onUnbind(Intent intent) {

		if (dummyGPSThread != null) {
			dummyGPSThread.interrupt();
		}

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
		return lastKnownLocation;
	}

	/**
	 * Sets the listener that will be notified of location and connectivity updates.
	 */
	@Override
	public void setGPSServiceListener(GPSServiceListener listener) {
		synchronized (listenerLock) {
			this.listener = listener;
		}
	}

	private final LocalBinder<IGPSService> binder = new LocalBinder<IGPSService>(this);

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public void onDestroy() {

		if (dummyGPSThread != null) {
			dummyGPSThread.interrupt();
			dummyGPSThread = null;
			locationServicesEnabled = false;
		}

		binder.close();
		super.onDestroy();
	}

	@SuppressLint("SimpleDateFormat")
	private void generateFakeData() throws IOException, ParseException {
		if (fakeLocations.size() == 0) {

			final BufferedReader gpsDataReader = new BufferedReader(new InputStreamReader(getResources()
					.openRawResource(raw.dummy_gps_data)));

			try {

				final String dateformatstring = "'\"'yyyy'-'MM'-'dd' 'kk:mm:ss'\"'";
				SimpleDateFormat dateFormat = new SimpleDateFormat(dateformatstring);

				// Read the first line, its the headers
				gpsDataReader.readLine();

				long offset = -1;

				// Loop until we reach the end of the file
				String line;
				while ((line = gpsDataReader.readLine()) != null) {

					// Get values out of the line
					String[] lineParts = line.split(",");
					// double distance = Double.parseDouble(lineParts[0]);
					long generatedTime = dateFormat.parse(lineParts[1]).getTime();
					double speedKmH = Double.parseDouble(lineParts[2]);
					double latitude = Double.parseDouble(lineParts[3]);
					double longitude = Double.parseDouble(lineParts[4]);

					// Calc some extra values
					long timeValue;
					if (offset < 0) {
						offset = generatedTime;
						timeValue = 0;
					} else {
						timeValue = generatedTime - offset;
					}

					double speedMS = speedKmH * 1000.0 / 3600.0;

					Location loc = new Location("DummyGPSServiceImpl");
					loc.setLatitude(latitude);
					loc.setLongitude(longitude);
					loc.setTime(timeValue);
					loc.setSpeed((float) speedMS);
					loc.setAccuracy(10); // TODO Something a little different, perhaps involving the distance recorded?
					fakeLocations.add(loc);

				}
			} finally {

				// Close the reader
				try {
					gpsDataReader.close();
				} catch (IOException e) {
					logger.error(e);
				}
			}
		}
	}

	/**
	 * Serves dummy GPS updates.
	 */
	private final Runnable dummyGPSTask = new Runnable() {

		@Override
		public void run() {

			try {

				// Load the fake data into memory.
				generateFakeData();

				// Let the listener know we've connected.
				locationServicesEnabled = true;
				synchronized (listenerLock) {
					if (listener != null) {
						listener.gpsConnectivityChanged(true);
					}
				}

				// Get current time.
				long startTime = System.currentTimeMillis();

				int index = 0;
				while (!Thread.interrupted()) {

					// If our index is too big, reset it and the start time.
					if (index == fakeLocations.size()) {
						index = 0;
						startTime = System.currentTimeMillis();
					}

					// Get loc
					Location rawLoc = fakeLocations.get(index);
					Location loc = new Location(rawLoc);

					// Update its generated time to be more realistic.
					loc.setTime(loc.getTime() + startTime);

					// Send it
					synchronized (listenerLock) {
						if (listener != null) {
							listener.newLocationReceived(loc);
						}
					}

					// Figure out how long to wait.
					long waitTime = 3000;
					if (index + 1 != fakeLocations.size()) {
						Location nextLoc = fakeLocations.get(index + 1);
						waitTime = nextLoc.getTime() - rawLoc.getTime();
					}

					// Wait some time
					Thread.sleep(waitTime);
					index++;

				}

			} catch (InterruptedException e) {
				// This is fine.
			} catch (IOException e) {
				logger.error(e);
				throw new RuntimeException(e);
			} catch (ParseException e) {
				logger.error(e);
				throw new RuntimeException(e);
			}

			// Let the listener know we've disconnected.
			locationServicesEnabled = false;
			synchronized (listenerLock) {
				if (listener != null) {
					listener.gpsConnectivityChanged(false);
				}
			}

		}
	};

}
