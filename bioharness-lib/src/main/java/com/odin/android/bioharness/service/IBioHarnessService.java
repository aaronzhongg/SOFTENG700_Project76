package com.odin.android.bioharness.service;

import android.bluetooth.BluetoothDevice;

import com.odin.android.bioharness.BHConnectivityStatus;
import com.odin.android.bioharness.data.ECGWaveformData;
import com.odin.android.bioharness.data.GeneralData;
import com.odin.android.bioharness.data.SummaryData;

/**
 * Service interface defining the functionality to allow applications to interact with the BioHarness running in
 * background.
 * 
 * @author Thiranjith Weerasinghe
 */
public interface IBioHarnessService {
	/**
	 * Callback handler to be notified when certain events occur within the {@link IBioHarnessService}
	 * 
	 * @author Thiranjith Weerasinghe
	 */
	public static interface IBioHarnessServiceListener {
		void onDisconnectedFromBioharness();

		void onConnectingToBioharness();

		void onConnectedToBioharness();

		void onReceiveGeneralData(GeneralData data);

		void onReceivedMessage(String message);

		void onDeviceConnected(String deviceName);

		void onReceiveSummaryData(SummaryData newData);

		void onReceiveECGData(ECGWaveformData ecgData);
	}

	void setBioHarnessServiceListener(IBioHarnessServiceListener listener);

	/**
	 * Connect to the given Zephyr BioHarness
	 * 
	 * @param bluetoothAddress
	 *            the address of the bioharness
	 */
	void connectToBioHarness(String bluetoothAddress);

	/**
	 * Connect to the Zephyr BioHarness at the address stored in the preferences.
	 */
	void connectToBioHarnessFromPrefs();

	/**
	 * Sets the number of retries for the bioharness connect algorithm.
	 * 
	 * @param numRetries
	 */
	void setNumRetries(int numRetries);

	/**
	 * Disconnect the bioharness (including stopping all data being monitored) if it is currently connected. Otherwise,
	 * the operation will be ignored.
	 */
	void disconnectBioHarness();

	/**
	 * @return {@link BioHarnessStatus} representing the current snapshot of how the BioHarness is being used.
	 */
	BioHarnessStatus getBioHarnessStatus();

	/**
	 * @param enableDataRecording
	 *            <code>true</code> to enable data recording. <code>false</code> to disable data recording.
	 */
	void setRecordingData(boolean enableDataRecording);

	/**
	 * Change the ECG data monitoring status
	 * 
	 * @param startRecording
	 *            <code>true</code> if ECG data is currently being recorded.
	 */
	void setMonitorECGData(boolean startRecording);

	/**
	 * Clear ECG and/or General data without user confirmation.
	 * 
	 * @param deleteECGData
	 * @param deleteGeneralData
	 */
	void clearDatabase(boolean deleteECGData, boolean deleteGeneralData);

	void exportAllDataToCSV(String filePrefix, String filePath);

	void exportGeneralDataToCSV(String filePrefix, String filePath);

	void exportECGDataToCSV(String filePrefix, String filePath);

	void incrementLapCount();

	void resetLapCount();

	BHConnectivityStatus getStatus();

	BluetoothDevice getDevice();
}
