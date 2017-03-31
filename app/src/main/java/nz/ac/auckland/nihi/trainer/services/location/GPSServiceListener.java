package nz.ac.auckland.nihi.trainer.services.location;

import android.location.Location;

public interface GPSServiceListener {

	void newLocationReceived(Location location);

	void gpsConnectivityChanged(boolean isConnected);

}
