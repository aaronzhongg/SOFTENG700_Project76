package com.odin.android.bioharness;

/**
 * Enum representing the possible states when a user attempts to connect to the BioHarness (via Bluetooth)
 * 
 * @author Thiranjith Weerasinghe
 */
public enum BHConnectivityStatus {
	/**
	 * The initial state when the device is not connected to the application (e.g. either yet to be connected or
	 * disconnected by the user).
	 */
	DISCONNECTED,
	/**
	 * The user invoked an action to connect to the BioHarness. This represents the state where the phone is attempting
	 * to enable Bluetooth (if disabled) and/or waiting for the user to correctly select a BioHarness to pair with the
	 * phone.
	 * 
	 * Initiating status: {@link BHConnectivityStatus#DISCONNECTED}
	 * 
	 * Resultant status:
	 * <ul>
	 * <li>{@link BHConnectivityStatus#DISCONNECTED} - if the user didn't enable bluetooth or didn't pair the BioHarness
	 * correctly with the device</li>
	 * <li>{@link BHConnectivityStatus#CONNECTED} - if the user successfully paired with the BioHarnes</li>
	 * </ul>
	 */
	CONNECTING,
	/**
	 * State representing a BioHarness connected to the user. The application can now interact with the BioHarness using
	 * Bluetooth protocol.
	 * 
	 * Initiating status: {@link BHConnectivityStatus#CONNECTTING}
	 * 
	 * Resultant status: {@link BHConnectivityStatus#DISCONNECTED} when the user closes the application or explicitly
	 * disconnects from the Bluetooth application.
	 */
	CONNECTED
}
