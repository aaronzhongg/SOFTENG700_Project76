package com.odin.android.bioharness.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import nz.ac.auckland.cs.android.utils.BackgroundLooperThread;
import nz.ac.auckland.cs.android.utils.NotificationUtils;
import nz.ac.auckland.cs.ormlite.DatabaseManager;
import nz.ac.auckland.cs.ormlite.LocalDatabaseHelper;

import org.apache.log4j.Logger;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.IBinder;

import com.odin.android.bioharness.BHConnectivityStatus;
import com.odin.android.bioharness.BioHarness;
import com.odin.android.bioharness.BioHarnessMessageHandler;
import com.odin.android.bioharness.IBioHarnessMessageReceiver;
import com.odin.android.bioharness.R.string;
import com.odin.android.bioharness.data.BioharnessDBHelper;
import com.odin.android.bioharness.data.ECGWaveformData;
import com.odin.android.bioharness.data.GeneralData;
import com.odin.android.bioharness.data.SummaryData;
import com.odin.android.bioharness.prefs.BioharnessDescription;
import com.odin.android.bioharness.prefs.BioharnessPreferences;
import com.odin.android.services.LocalBinder;

/**
 * {@link IBioHarnessService} implementation to allow applications to interact with the BioHarness running in
 * background.
 * 
 * The service will automatically shutdown if the Bluetooth connection is not running when the service is created.
 * 
 * Prerequisites for using the service:
 * <ol>
 * <li>The main activity must call {@link DatabaseManager#defineDatabase(String, int, Class...)} with a reference to the
 * {@link BioharnessDBHelper} class.</li>
 * <li>The calling activity must ensure that Bluetooth is enabled on the device (and handle any disconnections by
 * prompting the user to re-enable it, or otherwise disconnect the service)</li>
 * <li>The phone supports Bluetooth.</li>
 * </ol>
 * 
 * @author Thiranjith Weerasinghe
 * @author Andrew Meads
 */
public class BioHarnessServiceImpl extends Service implements IBioHarnessService {

	private static final int BH_ONGOING_NOTIFICATION_ID = 2;

	private static final Logger logger = Logger.getLogger(BioHarnessServiceImpl.class);

	private LocalBinder<IBioHarnessService> mBinder;
	private BioHarness mBioHarness = null;
	private BluetoothAdapter mBluetoothAdapter;
	private BioHarnessStatus mBHStatus;
	private boolean mIsMonitoringECGData;
	private boolean mIsRecordData;
	private long mLapRecordingTimeInMillis;
	private int mLapNumber = 1;
	private LocalDatabaseHelper dbHelper;
	private IBioHarnessServiceListener mListener;

	private boolean stopped = false;

	/**
	 * The message handling thread for the BioHarness.
	 */
	private BackgroundLooperThread looperThread;

	/**
	 * The handler for BioHarness messages. Messages will be dealt with on the looper thread.
	 */
	private BioHarnessMessageHandler mHandler;

	/**
	 * Lazily instantiates the DB helper and returns it.
	 * 
	 * @return
	 */
	private LocalDatabaseHelper getDBHelper() {
		if (dbHelper == null) {
			dbHelper = DatabaseManager.getInstance().getDatabaseHelper(this);
		}
		return dbHelper;
	}

	/**
	 * The {@link BioHarnessMessageHandler}, running on the {@link BackgroundLooperThread}, will transform BioHarness
	 * messages and forward them to this object for handling.
	 */
	private final IBioHarnessMessageReceiver messageReceiver = new IBioHarnessMessageReceiver() {

		/**
		 * Notifies listener of changes in Bioharness connectivity.
		 */
		public void onBioHarnessConnectivityStatusChanged(BHConnectivityStatus status) {
			logger.info("BH connectivity status changed to " + status);
			showNotificationIcon(status); // Show new status in notification area
			switch (status) {
			case CONNECTED:
				resetLapCount();
				if (mListener != null) {
					mListener.onConnectedToBioharness();
				}
				break;
			case CONNECTING:
				if (mListener != null) {
					mListener.onConnectingToBioharness();
				}
				break;
			case DISCONNECTED:
				mIsRecordData = false;
				if (mListener != null) {
					mListener.onDisconnectedFromBioharness();
				}
				break;
			}
		}

		/**
		 * Saves received ECG data to the database.
		 */
		@Override
		public void onReceiveECGData(ECGWaveformData ecgData) {
			ecgData.setLapMarker(mLapNumber);
			if (mIsRecordData) {
				try {
					getDBHelper().getSectionHelper(BioharnessDBHelper.class).getECGDataDAO().create(ecgData);
				} catch (SQLException e) {
					logger.warn("onReceiveECGData(): Could not save ECG data to database.", e);
				}
			}
			if (mListener != null) {
				mListener.onReceiveECGData(ecgData);
			}
		}

		/**
		 * Saves received GeneralData to the database, and notifies the listener that this data has been received.
		 */
		@Override
		public void onReceiveGeneralData(GeneralData newData, GeneralData oldData) {
			newData.setLapMarker(mLapNumber);
			if (mIsRecordData) {
				try {
					getDBHelper().getSectionHelper(BioharnessDBHelper.class).getGeneralDataDAO().create(newData);
				} catch (SQLException e) {
					logger.warn("onReceiveGeneralData(): Could not save General data to database.", e);
				}
			}
			if (mListener != null) {
				mListener.onReceiveGeneralData(newData);
			}
		}

		/**
		 * Notifies the listener when the devices has been connected.
		 */
		@Override
		public void onDeviceConnected(String deviceName) {
			if (mListener != null) {
				mListener.onDeviceConnected(deviceName);
			}
		}

		/**
		 * Notifies the listener when a message has been received.
		 */
		@Override
		public void onReceivedMessage(String message) {
			if (mListener != null) {
				mListener.onReceivedMessage(message);
			}
		}

		@Override
		public void onReceiveSummaryData(SummaryData newData, SummaryData oldData) {
			// newData.setLapMarker(mLapNumber);
			if (mIsRecordData) {
				try {
					getDBHelper().getSectionHelper(BioharnessDBHelper.class).getSummaryDataDAO().create(newData);
				} catch (SQLException e) {
					logger.warn("onReceiveSummaryData(): Could not save Summary data to database.", e);
				}
			}
			if (mListener != null) {
				mListener.onReceiveSummaryData(newData);
			}
		};
	};

	/**
	 * Returns the binder that can be used to access the service functionality.
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	/**
	 * Called when the service is created. Begins the process of setting up the Bioharness.
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		logger.info("onCreate()");

		// Create the background thread and handler to handle messages from the BioHarness
		try {
			looperThread = new BackgroundLooperThread("BioHarness Looper Thread");
			looperThread.start();
			mHandler = new BioHarnessMessageHandler(looperThread.getLooper(), messageReceiver);
		} catch (InterruptedException e) {
			// This should never be called.
			logger.error(e);
			throw new RuntimeException(e);
		}

		// Create the binder that consumers can access to consume this service.
		mBinder = new LocalBinder<IBioHarnessService>(this);

		// Get the phone's Bluetooth adapter. Stop the service immediately if Bluetooth is not supported.
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			logger.error("onCreate(): Phone does not support bluetooth");
			this.stopSelf();
			return;
		}

		// Init values
		mBHStatus = new BioHarnessStatus();
		mIsRecordData = false;
		mIsMonitoringECGData = false;
		mLapRecordingTimeInMillis = 0;

		setUpBioHarness();

		// Display initial notification
		showNotificationIcon(getStatus());
	}

	/**
	 * When the service is destroyed, disconnect from the database, the binder, and the Bioharness.
	 */
	@Override
	public void onDestroy() {

		stopped = true;

		super.onDestroy();
		logger.debug("onDestroy()");

		// Close binder
		if (mBinder != null) {
			mBinder.close();
			mBinder = null;
		}

		disconnectFromBioHarness();

		// Release DB connection
		if (dbHelper != null) {
			dbHelper = null;
			DatabaseManager.getInstance().releaseHelper();
		}

		// Clear notification
		NotificationUtils.clearNotification(this, BH_ONGOING_NOTIFICATION_ID);

		// Stop the background thread.
		if (looperThread != null) {
			looperThread.quit();
		}
	}

	private void showNotificationIcon(BHConnectivityStatus connectionStatus) {
		if (!stopped) {
			int iconId;
			String tickerText;
			String content;
			switch (connectionStatus) {
			case CONNECTING:
				iconId = com.odin.android.bioharness.R.drawable.ic_bioharness_connecting;
				tickerText = getString(string.bioharness_connecting); // "Connecting to Bioharness...";
				content = getString(string.bioharness_connecting_short); // "Connecting...";
				break;
			case CONNECTED:
				iconId = com.odin.android.bioharness.R.drawable.ic_bioharness_connected;
				tickerText = getString(string.bioharness_connected); // "Connected to Bioharness.";
				content = getString(string.bioharness_connected_short); // "Connected.";
				break;
			case DISCONNECTED:
				iconId = com.odin.android.bioharness.R.drawable.ic_bioharness_disconnected;
				tickerText = getString(string.bioharness_disconnected); // "Disconnected from Bioharness.";
				content = getString(string.bioharness_disconnected_short); // "Disconnected.";
				break;
			default:
				throw new RuntimeException("Unsupported icon type: " + connectionStatus);
			}

			String title = getString(string.bioharness_notification_status);
			NotificationUtils.showOngoingNotification(this, iconId, tickerText, title, BH_ONGOING_NOTIFICATION_ID,
					content, null);
		}
	}

	// -- IBioHarnessService methods

	/**
	 * Sets the listener that will receive data and notifications from the Bioharness.
	 * 
	 * TODO This should really be changed to a broadcast mechanism.
	 */
	@Override
	public void setBioHarnessServiceListener(IBioHarnessServiceListener listener) {
		mListener = listener;
	}

	/**
	 * Connects to the given Bluetooth device, assuming it is a Bioharness.
	 */
	@Override
	public void connectToBioHarness(String zephyrBioHarness) {
		mBioHarness.connect(BluetoothAdapter.getDefaultAdapter().getRemoteDevice(zephyrBioHarness));
	}

	@Override
	public void connectToBioHarnessFromPrefs() {
		BioharnessDescription descr = BioharnessPreferences.BioharnessDescription.getValue(this,
				BioharnessDescription.class, null);
		connectToBioHarness(descr.getAddress());
	}

	@Override
	public void setNumRetries(int numRetries) {
		mBioHarness.setNumRetries(numRetries);
	}

	/**
	 * Disconnects from the Bioharness, if connected.
	 */
	@Override
	public void disconnectBioHarness() {
		if (mBioHarness.isConnected()) {
			disconnectFromBioHarness();
		}
	}

	/**
	 * Gets the status of the Bioharness, including information regarding whether the Bioharness is connected, whether
	 * its monitoring, and whether its recording.
	 */
	@Override
	public BioHarnessStatus getBioHarnessStatus() {
		boolean isMonitoringData = mBioHarness != null && mBioHarness.isMonitoring();
		mBHStatus.setIsMonitoringData(isMonitoringData, isMonitoringData && mIsMonitoringECGData);
		mBHStatus.setIsConnected(mBioHarness != null && mBioHarness.isConnected());
		mBHStatus.setIsRecordingData(mIsRecordData, mLapNumber, mLapRecordingTimeInMillis);

		return mBHStatus;
	}

	/**
	 * Sets a value indicating whether we should record data to the database when its received.
	 */
	@Override
	public void setRecordingData(boolean enableDataRecording) {
		mIsRecordData = enableDataRecording;
		mLapRecordingTimeInMillis = mIsRecordData ? System.currentTimeMillis() : 0;
	}

	/**
	 * Sets a value indicating whether ECG monitoring should be enabled. This method does nothing if the Bioharness is
	 * not conencted.
	 */
	@Override
	public void setMonitorECGData(boolean startRecording) {
		if (!mBioHarness.isConnected()) {
			logger.warn("setMonitorECGData(): Not changing ecg status because bioharness is not currently connected.");
			return;
		}
		if (startRecording) {
			mBioHarness.startReceivingECGWaveFormData();
			logger.info("setMonitorECGData(): Wrote packet to start receiving ECG data");
		} else {
			mBioHarness.stopReceivingECGWaveFormData();
			logger.info("setMonitorECGData(): Wrote packet to stop receiving ECG data");
		}
		mIsMonitoringECGData = startRecording;
	}

	/**
	 * Deletes Bioharness data from the database. Which data are deleted depends on the values of the arguments to this
	 * method.
	 * 
	 * @param deleteECGData
	 *            <code>true</code> if ECG data should be deleted, <code>false</code> otherwise.
	 * @param deleteGeneralData
	 *            <code>true</code> if general data should be deleted, <code>false</code> otherwise.
	 */
	@Override
	public void clearDatabase(boolean deleteECGData, boolean deleteGeneralData) {
		BioharnessDBHelper db = getDBHelper().getSectionHelper(BioharnessDBHelper.class);
		try {
			if (deleteECGData) {
				int numRows = db.getECGDataDAO().deleteBuilder().delete();
				logger.debug("clearDatabase(): ECG data successfully deleted (" + numRows + " rows)");
			}
			if (deleteGeneralData) {
				int numRows = db.getGeneralDataDAO().deleteBuilder().delete();
				logger.debug("clearDatabase(): General data successfully deleted (" + numRows + " rows)");
			}
		} catch (SQLException e) {
			logger.warn("clearDatabase(): could not delete some data", e);
		}
	}

	/**
	 * Exports all Bioharness data in the database to a CSV file, saved on the SD card.
	 */
	@Override
	public void exportAllDataToCSV(String filePrefix, String filePath) {
		exportGeneralDataToCSV(filePrefix, filePath);
		exportECGDataToCSV(filePrefix, filePath);
	}

	/**
	 * Exports ECG data in the database to a CSV file, saved on the SD card.
	 */
	@Override
	public void exportECGDataToCSV(String filePrefix, String filePath) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_hh-mm-ss");
		File outputFile = new File(filePath, filePrefix + "_ecg_" + dateFormat.format(new Date()) + ".csv");
		logger.info("exportECGDataToCSV(): start saving ECG data to " + outputFile.getAbsolutePath());
		if (!outputFile.getParentFile().exists()) {
			boolean isSuccess = outputFile.getParentFile().mkdirs();
			logger.info("exportECGDataToCSV(): created parent folder hierarchy: " + isSuccess);
		}

		List<ECGWaveformData> allData = null;
		try {
			allData = getDBHelper().getSectionHelper(BioharnessDBHelper.class).getECGDataDAO().queryForAll();
		} catch (SQLException sqlEx) {
			logger.error("exportECGDataToCSV(): could not get ECG data from database", sqlEx);
			return;
		}

		try {
			outputFile.createNewFile();
			BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
			logger.info("exportECGDataToCSV(): Writing ecg data: " + allData.size() + " items. ");
			writer.newLine();
			writer.write("lap, sequenceNumber, dh_captured_datetime, devie_captured_datetime, dataIndex, ecgSampleIndex, sampleNum, ecgValue");
			writer.newLine();
			int lastLapMarker = -1;
			int dataIndex = 1;
			int ecgSampleIndex = 1;
			for (ECGWaveformData ecgData : allData) {
				// only include lap markers when a new lap is started to make it easier to navigate in .csv file
				String lapMarker = "";
				if (lastLapMarker != ecgData.getLapMarker()) {
					lastLapMarker = ecgData.getLapMarker();
					lapMarker = String.valueOf(lastLapMarker);
				}
				int[] ecgSamples = ecgData.getSamples();
				for (int idx = 0; idx < ecgSamples.length; idx++) {
					// record data
					StringBuilder sb = new StringBuilder();
					sb.append(lapMarker).append(",");
					if (lapMarker.length() > 0) {
						lapMarker = ""; // to ensure lap marker is only shown for the very first row of the data
					}
					if (idx == 0) {
						sb.append(ecgData.getSequenceNumber()).append(",");
						sb.append(ecgData.getFormattedBHCapturedDate()).append(",");
						sb.append(ecgData.getFormattedDeviceCapturedDate()).append(",");
						sb.append(dataIndex).append(",");
					} else {
						sb.append(","); // sequence number is empty
						sb.append(ecgData.getFormattedBHCapturedDataForSample(idx)).append(","); // bh_captured_datetime
						sb.append(",,"); // devie_captured_datetime, dataIndex empty
					}
					sb.append(ecgSampleIndex).append(",");
					sb.append((idx + 1)).append(",");
					sb.append(ecgSamples[idx]);

					writer.write(sb.toString());
					writer.newLine();

					ecgSampleIndex++;
				}
				dataIndex++;
			}

			writer.flush();
			writer.close();
		} catch (IOException e) {
			logger.error("exportECGDataToCSV(): Failed to export data.", e);
		}

		allData.clear();
		logger.info("exportECGDataToCSV(): done saving ecg data");
	}

	/**
	 * Exports General Bioharness data in the database to a CSV file, saved on the SD card.
	 */
	@Override
	public void exportGeneralDataToCSV(String filePrefix, String filePath) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_hh-mm-ss");
		File outputFile = new File(filePath, filePrefix + "_gd_" + dateFormat.format(new Date()) + ".csv");
		logger.info("exportGeneralDataToCSV(): start saving general data to " + outputFile.getAbsolutePath());

		// write general data
		if (!outputFile.getParentFile().exists()) {
			boolean isSuccess = outputFile.getParentFile().mkdirs();
			logger.info("exportGeneralDataToCSV(): created parent folder hierarchy: " + isSuccess);
		}

		List<GeneralData> allData = null;
		try {
			allData = getDBHelper().getSectionHelper(BioharnessDBHelper.class).getGeneralDataDAO().queryForAll();
		} catch (SQLException sqlEx) {
			logger.error("exportGeneralDataToCSV(): could not get General data from database", sqlEx);
			return;
		}

		try {
			outputFile.createNewFile();
			BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
			logger.info("exportGeneralDataToCSV(): Writing general data: " + allData.size() + " items.");
			writer.newLine();
			writer.write("lap, index, id, sequenceNumber, bh_captured_datetime, devie_captured_datetime, heart_rate, skin_temp, breathing_rate, posture, blood_pressure");
			writer.newLine();
			int lastLapMarker = -1;
			int index = 1;
			for (GeneralData generalData : allData) {
				StringBuilder sb = new StringBuilder();
				// only include lap markers when a new lap is started to make it easier to navigate in .csv file
				String lapMarker = "";
				if (lastLapMarker != generalData.getLapMarker()) {
					lastLapMarker = generalData.getLapMarker();
					lapMarker = String.valueOf(lastLapMarker);
				}
				sb.append(lapMarker).append(",");

				sb.append(index).append(",");
				sb.append(generalData.getId()).append(",");
				sb.append(generalData.getSequenceNum()).append(",");
				sb.append(generalData.getFormattedBHCapturedDate()).append(",");
				sb.append(generalData.getFormattedDeviceCapturedDate()).append(",");
				sb.append(generalData.getHeartRate()).append(",");
				sb.append(generalData.getSkinTemp()).append(",");
				sb.append(generalData.getRespirationRate()).append(",");
				sb.append(generalData.getPosture()).append(",");
				sb.append(generalData.getBloodPressure());

				writer.write(sb.toString());
				writer.newLine();
				index++;
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			logger.error("exportGeneralDataToCSV(): Failed to export data.", e);
		}
		allData.clear();
		logger.info("exportGeneralDataToCSV(): done saving general data");
	}

	/**
	 * Add one to the lap counter.
	 */
	@Override
	public void incrementLapCount() {
		mLapNumber++;
	}

	/**
	 * Set the lap counter to 1 and the lap recording time to 0.
	 */
	@Override
	public void resetLapCount() {
		mLapNumber = 1;
		mLapRecordingTimeInMillis = 0;
	}

	// -- private methods

	/**
	 * Creates the Bioharness object. This should only be called once each time the service is created.
	 */
	private void setUpBioHarness() {
		logger.debug("setUpBioHarness()");
		if (mBioHarness != null) {
			throw new IllegalStateException(
					"Cannot setup the BioHarness instance when there is one already created! Check the Actvity lifecycle for application logic error!");
		}
		// mBioHarness = new BioHarness(mHandler);
		mBioHarness = new BioHarness(mHandler, BioharnessMonitoringType.SummaryData);
	}

	/**
	 * Disconnects from the bioharness.
	 */
	private void disconnectFromBioHarness() {
		logger.info("disconnectFromBioHarness...");
		if (mBioHarness != null) {
			mBioHarness.stop();
		}
	}

	/**
	 * Gets the status of the bioharness.
	 */
	@Override
	public synchronized BHConnectivityStatus getStatus() {
		if (mBioHarness == null) {
			return BHConnectivityStatus.DISCONNECTED;
		} else {
			return mBioHarness.getStatus();
		}
	}

	/**
	 * Gets the {@link BluetoothDevice} to which we're currently connecting / connected, if any. Returns
	 * <code>null</code> if disconnected.
	 */
	@Override
	public synchronized BluetoothDevice getDevice() {
		if (mBioHarness == null) {
			return null;
		} else {
			return mBioHarness.getDevice();
		}
	}
}
