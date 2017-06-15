package nz.ac.auckland.nihi.trainer.services.workout;

import nz.ac.auckland.cs.odin.interconnect.common.EndpointConnectionStatus;

import com.odin.android.bioharness.BHConnectivityStatus;

/**
 * An interface for a status object allowing interested parties to query the connectivity status of the workout service.
 * 
 * @author Andrew Meads
 * 
 */
public interface WorkoutServiceStatus {

	boolean isBluetoothEnabled();

	boolean hasBioharnessAddress();

	BHConnectivityStatus getBioharnessConnectivityStatus();

	boolean isConnectedToGPS();

	//	TODO: REMOVE ODIN CONNECTION
//	EndpointConnectionStatus getOdinConnectivityStatus();

	boolean isMonitoring();

	boolean canStartNewWorkout();
}
