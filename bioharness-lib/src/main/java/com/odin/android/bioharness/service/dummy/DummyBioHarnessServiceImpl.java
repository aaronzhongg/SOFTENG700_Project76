package com.odin.android.bioharness.service.dummy;

import java.lang.reflect.Constructor;

import nz.ac.auckland.cs.android.utils.BackgroundLooperThread;
import nz.ac.auckland.cs.android.utils.NotificationUtils;

import org.apache.log4j.Logger;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;

import com.odin.android.bioharness.BHConnectivityStatus;
import com.odin.android.bioharness.data.ECGWaveformData;
import com.odin.android.bioharness.data.SummaryData;
import com.odin.android.bioharness.service.BioHarnessStatus;
import com.odin.android.bioharness.service.IBioHarnessService;
import com.odin.android.services.LocalBinder;

/**
 * Allows one to simulate the use of the BioHarness on devices without Bluetooth connections (i.e. emulators).
 * 
 * @author Andrew Meads
 * 
 */
public class DummyBioHarnessServiceImpl extends Service implements IBioHarnessService {

	private static final Logger logger = Logger.getLogger(DummyBioHarnessServiceImpl.class);

	public static final String DUMMY_BH_ADDRESS = "00:00:00:00:00:00";
	private static final int BH_ONGOING_NOTIFICATION_ID = 2;

	private BluetoothDevice dummyBioHarnessDevice;
	private LocalBinder<IBioHarnessService> mBinder;
	private BioHarnessStatus mBHStatus = new BioHarnessStatus();
	private BHConnectivityStatus connStatus = BHConnectivityStatus.DISCONNECTED;
	private IBioHarnessServiceListener mListener;

	private FakeBioHarness dummyBioHarness;

	private boolean stopped = false;

	private BackgroundLooperThread looperThread;

	private FakeBioHarnessECGDataSource ecgSource;

	private Handler bgHandler;

	@Override
	public void onCreate() {
		super.onCreate();

		looperThread = new BackgroundLooperThread("Dummy Bioharness Message rcv thread");
		looperThread.start();

		// Set a dummy device.
		try {
			Constructor<?> ctor = BluetoothDevice.class.getDeclaredConstructors()[0];
			ctor.setAccessible(true);
			dummyBioHarnessDevice = (BluetoothDevice) ctor.newInstance(DUMMY_BH_ADDRESS);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		try {
			bgHandler = new Handler(looperThread.getLooper(), dummyBioHarnessHandlerCallback);
		} catch (InterruptedException e) {
		}
		dummyBioHarness = new FakeBioHarness(bgHandler, 2000);

		mBinder = new LocalBinder<IBioHarnessService>(this);

		showNotificationIcon(getStatus());
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		stopped = true;

		// Clear listener
		mListener = null;

		if (mBinder != null) {
			mBinder.close();
			mBinder = null;
		}

		// Stop BioHarness, if started
		if (ecgSource != null) {
			ecgSource.stop();
		}
		dummyBioHarness.stop();

		// Clear notification
		NotificationUtils.clearNotification(this, BH_ONGOING_NOTIFICATION_ID);

		looperThread.quit();
	}

	private void showNotificationIcon(BHConnectivityStatus connectionStatus) {
		if (!stopped) {
			int iconId;
			String tickerText;
			String content;
			switch (connectionStatus) {
			case CONNECTING:
				iconId = com.odin.android.bioharness.R.drawable.ic_bioharness_connecting;
				tickerText = "Connecting to Bioharness...";
				content = "Connecting...";
				break;
			case CONNECTED:
				iconId = com.odin.android.bioharness.R.drawable.ic_bioharness_connected;
				tickerText = "Connected to Bioharness.";
				content = "Connected.";
				break;
			case DISCONNECTED:
				iconId = com.odin.android.bioharness.R.drawable.ic_bioharness_disconnected;
				tickerText = "Disconnected from Bioharness.";
				content = "Disconnected.";
				break;
			default:
				throw new RuntimeException("Unsupported icon type: " + connectionStatus);
			}

			NotificationUtils.showOngoingNotification(this, iconId, tickerText, "Bioharness Status",
					BH_ONGOING_NOTIFICATION_ID, content, null);
		}
	}

	@Override
	public void setBioHarnessServiceListener(IBioHarnessServiceListener listener) {
		mListener = listener;
	}

	@Override
	public void connectToBioHarness(String zephyrBioHarness) {
		if (!dummyBioHarness.isConnected()) {
			connStatus = BHConnectivityStatus.CONNECTING;
			mBHStatus.setIsConnected(false);
			mBHStatus.setIsMonitoringData(false, false);
			showNotificationIcon(getStatus());
		}
		dummyBioHarness.start();
	}

	@Override
	public void setNumRetries(int numRetries) {
		// Do nothing as this dummy service will always connect first time anyway.
	}

	@Override
	public void connectToBioHarnessFromPrefs() {
		connectToBioHarness(null);
	}

	@Override
	public void disconnectBioHarness() {
		dummyBioHarness.stop();
		if (ecgSource != null) {
			ecgSource.stop();
			ecgSource = null;
		}
	}

	@Override
	public void setMonitorECGData(boolean startRecording) {
		if (startRecording && ecgSource == null) {
			ecgSource = new FakeBioHarnessECGDataSource(bgHandler, this);
			ecgSource.start();
		} else if (!startRecording && ecgSource != null) {
			ecgSource.stop();
			ecgSource = null;
		}
	}

	@Override
	public BioHarnessStatus getBioHarnessStatus() {
		return mBHStatus;
	}

	@Override
	public BHConnectivityStatus getStatus() {
		return connStatus;
	}

	@Override
	public BluetoothDevice getDevice() {
		return dummyBioHarnessDevice;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}

	/**
	 * Handles messages from the dummy BioHarness.
	 */
	private final Handler.Callback dummyBioHarnessHandlerCallback = new Handler.Callback() {
		@Override
		public boolean handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case FakeBioHarness.MESSAGE_DUMMY_BH_CONNECTED:
				connStatus = BHConnectivityStatus.CONNECTED;
				mBHStatus.setIsConnected(true);
				mBHStatus.setIsMonitoringData(true, false);
				showNotificationIcon(getStatus());
				if (mListener != null) {
					mListener.onConnectedToBioharness();
				}
				return true;
			case FakeBioHarness.MESSAGE_DUMMY_BH_CONNECTING:
				if (connStatus != BHConnectivityStatus.CONNECTING) {
					connStatus = BHConnectivityStatus.CONNECTING;
					mBHStatus.setIsConnected(false);
					mBHStatus.setIsMonitoringData(false, false);
					showNotificationIcon(getStatus());
				}
				if (mListener != null) {
					mListener.onConnectingToBioharness();
				}
				return true;
			case FakeBioHarness.MESSAGE_DUMMY_BH_DISCONNECTED:
				connStatus = BHConnectivityStatus.DISCONNECTED;
				mBHStatus.setIsConnected(false);
				mBHStatus.setIsMonitoringData(false, false);
				showNotificationIcon(getStatus());
				if (mListener != null) {
					mListener.onDisconnectedFromBioharness();
				}
				return true;
			case FakeBioHarness.MESSAGE_DUMMY_BH_SUMMARY_DATA:
				if (mListener != null) {
					mListener.onReceiveSummaryData((SummaryData) msg.obj);
				}
				return true;
			case FakeBioHarnessECGDataSource.MESSAGE_DUMMY_BH_ECG_DATA:
				if (mListener != null) {
					mListener.onReceiveECGData((ECGWaveformData) msg.obj);
				}
				return true;
			default:
				return false;
			}
		}
	};

	@Override
	public void incrementLapCount() {
		mBHStatus.setIsRecordingData(false, mBHStatus.getLapCount() + 1, 0);
	}

	@Override
	public void resetLapCount() {
		mBHStatus.setIsRecordingData(false, 0, 0);
	}

	// Below this line are methods unsupported by the dummy service. Calling any of these will have no effect.
	// --------------------------------------------------------------------------------------------------------

	@Override
	public void setRecordingData(boolean enableDataRecording) {
		// TODO Auto-generated method stub

	}

	@Override
	public void clearDatabase(boolean deleteECGData, boolean deleteGeneralData) {
		// TODO Auto-generated method stub

	}

	@Override
	public void exportAllDataToCSV(String filePrefix, String filePath) {
		// TODO Auto-generated method stub

	}

	@Override
	public void exportGeneralDataToCSV(String filePrefix, String filePath) {
		// TODO Auto-generated method stub

	}

	@Override
	public void exportECGDataToCSV(String filePrefix, String filePath) {
		// TODO Auto-generated method stub

	}

}
