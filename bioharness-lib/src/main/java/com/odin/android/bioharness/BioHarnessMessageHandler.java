package com.odin.android.bioharness;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.odin.android.bioharness.data.ECGWaveformData;
import com.odin.android.bioharness.data.GeneralData;
import com.odin.android.bioharness.data.MessageResponse;
import com.odin.android.bioharness.data.ResponseBTLinkConfig;
import com.odin.android.bioharness.data.ResponseSetupECGWaveformData;
import com.odin.android.bioharness.data.ResponseSetupGeneralData;
import com.odin.android.bioharness.data.ResponseSetupSummaryData;
import com.odin.android.bioharness.data.SummaryData;

/**
 * Handles messages received from the BioHarness in the UI thread.
 * 
 * @author Thiranjith Weerasinghe
 */
public final class BioHarnessMessageHandler extends Handler {

	private static final Logger logger = Logger.getLogger(BioHarnessMessageHandler.class);

	/**
	 * Maximum number of {@link ECGWaveformData} to be stored at any one time
	 */
	private static final int MAX_ELEMENTS = 20;

	private MessageResponse mResponse;
	private GeneralData mGeneralData;
	private SummaryData mSummaryData;
	private final IBioHarnessMessageReceiver mMessageReceiver;
	private ArrayList<ECGWaveformData> mECGArray;
	private final int mECGEnvelopeLimit;

	/**
	 * Creates a new {@link BioHarnessMessageHandler} that holds at most 20 ecg data in memory at a time.
	 * 
	 * @param messageReceiver
	 */
	public BioHarnessMessageHandler(IBioHarnessMessageReceiver messageReceiver) {
		this(messageReceiver, MAX_ELEMENTS);
	}

	public BioHarnessMessageHandler(IBioHarnessMessageReceiver messageReceiver, int ecgEnvelopeLimit) {
		this(Looper.getMainLooper(), messageReceiver, ecgEnvelopeLimit);
	}

	public BioHarnessMessageHandler(Looper looper, IBioHarnessMessageReceiver messageReceiver) {
		this(looper, messageReceiver, MAX_ELEMENTS);
	}

	/**
	 * Creates a new {@link BioHarnessMessageHandler} that holds the specified amount ecg data in memory at a time.
	 * 
	 * @param messageReceiver
	 * @param ecgEnvelopeLimit
	 *            a positive integer
	 */
	public BioHarnessMessageHandler(Looper looper, IBioHarnessMessageReceiver messageReceiver, int ecgEnvelopeLimit) {
		super(looper);

		if (ecgEnvelopeLimit <= 0) {
			throw new IllegalArgumentException("The ECG envelope limit must be a positive integer");
		}
		mResponse = null;
		mMessageReceiver = messageReceiver;
		mECGArray = new ArrayList<ECGWaveformData>();
		mGeneralData = null;
		mSummaryData = null;
		mECGEnvelopeLimit = ecgEnvelopeLimit;
	}

	/**
	 * @return the last captured {@link GeneralData}. Could be <code>null</code> if no {@link GeneralData} has been
	 *         received.
	 */
	public GeneralData getGeneralData() {
		return mGeneralData;
	}

	@Override
	public void handleMessage(Message msg) {
		logger.debug("handleMessage: " + msg.what);
		switch (msg.what) {
		case BioHarness.MESSAGE_STATE_CHANGE:
			logger.debug("handleMessage():  MESSAGE_STATE_CHANGE: " + msg.arg1);
			BHConnectivityStatus status = BHConnectivityStatus.values()[msg.arg1];
			mMessageReceiver.onBioHarnessConnectivityStatusChanged(status);
			break;
		case BioHarness.MESSAGE_READ: {
			byte id = (byte) msg.getData().getByte(BioHarness.BIOHARNESS_MESSAGE_ID);
			byte[] data;
			data = (byte[]) msg.obj;
			boolean isSuccess;
			switch (id) {
			case (ResponseSetupSummaryData.MESSAGE_ID):
				mResponse = new ResponseSetupSummaryData(data);
				isSuccess = ((ResponseSetupSummaryData) mResponse).getResponse();
				logger.info("response: SetupSummaryData: " + isSuccess);
				break;
			case (ResponseBTLinkConfig.MESSAGE_ID):
				mResponse = new ResponseBTLinkConfig(data);
				isSuccess = ((ResponseBTLinkConfig) mResponse).getResponse();
				logger.info("response: BTLinkConfig: " + isSuccess);
				break;
			case (ResponseSetupGeneralData.MESSAGE_ID):
				mResponse = new ResponseSetupGeneralData(data);
				isSuccess = ((ResponseSetupGeneralData) mResponse).getResponse();
				logger.info("response: SetupGeneralData: " + isSuccess);
				break;
			case (ResponseSetupECGWaveformData.MESSAGE_ID):
				mResponse = new ResponseSetupECGWaveformData(data);
				isSuccess = ((ResponseSetupECGWaveformData) mResponse).getResponse();
				logger.debug("response: SetupECGData: " + isSuccess);
				break;
			case (GeneralData.MESSAGE_ID):
				final GeneralData oldData = mGeneralData;
				mGeneralData = new GeneralData(data);
				mResponse = mGeneralData;
				mMessageReceiver.onReceiveGeneralData(mGeneralData, oldData);
				break;
			case (ECGWaveformData.MESSAGE_ID):
				// TODO still lags
				final ECGWaveformData ecgdata;
				synchronized (BioHarnessMessageHandler.class) {
					while (mECGArray.size() >= mECGEnvelopeLimit) {
						mECGArray.remove(0);
					}
					ecgdata = new ECGWaveformData(data);
					mECGArray.add(ecgdata);
				}
				mMessageReceiver.onReceiveECGData(ecgdata);
				// logger.debug(" -- data size = " +ecgdata.data.length + ", @ " + ecgdata.millisecond + ", seq:" +
				// ecgdata.sequenceNum + ", id: " + ecgdata.msgId);
				break;
			case SummaryData.MESSAGE_ID:
				final SummaryData oldSData = mSummaryData;
				mSummaryData = new SummaryData(data);
				mResponse = mSummaryData;
				mMessageReceiver.onReceiveSummaryData(mSummaryData, oldSData);
				break;
			default:
				logger.warn("data object is not recognized (messageID = 0x" + Integer.toHexString(id) + " [" + id
						+ "])");
			}
			break;
		}
		case BioHarness.MESSAGE_DEVICE_NAME:
			mMessageReceiver.onDeviceConnected(msg.getData().getString(BioHarness.DEVICE_NAME));
			break;
		case BioHarness.MESSAGE_TOAST:
			mMessageReceiver.onReceivedMessage(msg.getData().getString(BioHarness.TOAST_MESSAGE_STRING));
			break;
		}
	}

	/**
	 * @return a copy of the currently captured {@link ECGWaveformData}
	 */
	public ArrayList<ECGWaveformData> getECGDataCopy() {
		synchronized (BioHarnessMessageHandler.class) {
			return new ArrayList<ECGWaveformData>(mECGArray);
		}
	}
}
