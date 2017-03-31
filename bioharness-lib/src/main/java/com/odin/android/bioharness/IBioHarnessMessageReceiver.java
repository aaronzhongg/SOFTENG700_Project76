package com.odin.android.bioharness;

import com.odin.android.bioharness.data.ECGWaveformData;
import com.odin.android.bioharness.data.GeneralData;
import com.odin.android.bioharness.data.SummaryData;

/**
 * Interface that allows applications to customise how to respond to messages received by the
 * {@link BioHarnessMessageHandler}.
 * 
 * @author Thiranjith Weerasinghe
 */
public interface IBioHarnessMessageReceiver {

	/**
	 * Callback to notify when the {@link BioHarness}'s status is changed
	 * 
	 * @param status
	 *            {@link BHConnectivityStatus}
	 */
	void onBioHarnessConnectivityStatusChanged(BHConnectivityStatus status);

	/**
	 * Notified whenever new {@link GeneralData} is received.
	 * 
	 * @param newData
	 *            Latest {@link GeneralData} received from the BioHarness.
	 * @param oldData
	 *            {@link GeneralData} received prior to this. Can be <code>null</code>.
	 */
	void onReceiveGeneralData(GeneralData newData, GeneralData oldData);

	/**
	 * Notified whenever new {@link SummaryData} is received.
	 * 
	 * @param newData
	 *            Latest {@link SummaryData} received from the BioHarness.
	 * @param oldData
	 *            {@link SummaryData} received prior to this. Can be <code>null</code>.
	 */
	void onReceiveSummaryData(SummaryData newData, SummaryData oldData);

	/**
	 * Callback when a BioHarness connects with the application
	 * 
	 * @param deviceName
	 *            name of the connected device
	 */
	void onDeviceConnected(String deviceName);

	/**
	 * Receives a generic message from the {@link BioHarness}
	 * 
	 * @param message
	 */
	void onReceivedMessage(String message);

	/**
	 * Callback when an ecg data is received from the {@link BioHarness}
	 * 
	 * @param ecgdata
	 */
	void onReceiveECGData(ECGWaveformData ecgdata);
}
