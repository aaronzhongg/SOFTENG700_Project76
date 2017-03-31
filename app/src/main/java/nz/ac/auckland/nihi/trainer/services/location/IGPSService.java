package nz.ac.auckland.nihi.trainer.services.location;

import android.location.Location;

public interface IGPSService {

	boolean isConnected();

	Location getLastKnownLocation();

	void setGPSServiceListener(GPSServiceListener listener);
}
