package com.odin.android.bioharness;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.odin.android.bioharness.data.AbstractResponseData;
import com.odin.android.bioharness.data.GeneralData;
import com.odin.android.bioharness.data.SummaryData;
import com.odin.android.bioharness.service.BioharnessMonitoringType;
import com.odin.android.bluetooth.BTConnectThread;
import com.odin.android.bluetooth.BTConnectThread.IBTConnectListener;

/**
 * Main class that interacts with the BioHarness via Bluetooth and send/received messages.
 * 
 * @author Alice Wang
 * @author Thiranjith Weerasinghe
 */
public class BioHarness implements IBTConnectListener {

	private static final Logger logger = Logger.getLogger(BioHarness.class);

	// Message types used by the BioHarness and external handlers
	/**
	 * Contains the ordinal of the current {@link BHConnectivityStatus} as the first argument.
	 */
	static final int MESSAGE_STATE_CHANGE = 1;
	/**
	 * Passes the device name.
	 * 
	 * values: String {@link BioHarness#DEVICE_NAME} representing the device name
	 */
	static final int MESSAGE_DEVICE_NAME = 2;
	/**
	 * Passes a message to be displayed as a toast
	 * 
	 * values: String {@link BioHarness#TOAST_MESSAGE_STRING} containing the message to be displayed to user
	 */
	static final int MESSAGE_TOAST = 3;
	static final int MESSAGE_READ = 4;
	static final int MESSAGE_WRITE = 5;

	// Key names received from the BluetoothChatService Handler
	static final String DEVICE_NAME = "device_name";
	static final String TOAST_MESSAGE_STRING = "toast";
	static final String BIOHARNESS_MESSAGE_ID = "message_id";

	// member fields
	private static final UUID BIOHARNESS_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

	private final Handler mHandler;
	private BTConnectThread mConnectThread;
	private BHConnectivityStatus mState;
	private static final RequestBioHarness request = new RequestBioHarness();

	// private boolean isMonitoring;
	private final BioharnessMonitoringType defaultMonitoringType;

	/**
	 * The type of periodic data currently being sent from the Bioharness. Determines whether listeners will receive
	 * {@link GeneralData}, {@link SummaryData}, or nothing.
	 */
	private BioharnessMonitoringType monitoringType;

	private int numRetries = 1;

	/**
	 * Gets the number of times the device will attempt to connect to the bioharness before giving up.
	 * 
	 * @return
	 */
	public int getNumRetries() {
		return this.numRetries;
	}

	/**
	 * Sets the number of times the device will attempt to connect to the bioharness before giving up.
	 * 
	 * @param numRetries
	 */
	public void setNumRetries(int numRetries) {
		this.numRetries = Math.max(1, numRetries);
	}

	/**
	 * Creates new {@link BioHarness}
	 * 
	 * @param handler
	 *            {@link Handler} to be notified depending on the interactions with the BioHarness (e.g. receiving
	 *            messages from it etc).
	 */
	public BioHarness(Handler handler) {
		this(handler, BioharnessMonitoringType.GeneralData);
	}

	/**
	 * Creates new {@link BioHarness}
	 * 
	 * @param handler
	 *            {@link Handler} to be notified depending on the interactions with the BioHarness (e.g. receiving
	 *            messages from it etc).
	 * @param defaultMonitoringType
	 *            the type of monitoring that should be activated as soon as the Bioharness connects.
	 */
	public BioHarness(Handler handler, BioharnessMonitoringType defaultMonitoringType) {
		mState = BHConnectivityStatus.DISCONNECTED;
		mHandler = handler;
		// isMonitoring = false;
		monitoringType = BioharnessMonitoringType.None;
		this.defaultMonitoringType = defaultMonitoringType;
	}

	/**
	 * Return the current connection state.
	 */
	public synchronized BHConnectivityStatus getState() {
		return mState;
	}

	public synchronized boolean isMonitoring() {
		// return isMonitoring;
		return (monitoringType != BioharnessMonitoringType.None);
	}

	public synchronized BioharnessMonitoringType getMonitoringType() {
		return monitoringType;
	}

	public void asyncSetMonitoringType(final BioharnessMonitoringType monitoringType, final long delayMillis) {
		new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(delayMillis);
				} catch (InterruptedException e) {
				}
				setMonitoringType(monitoringType);
			}
		}.start();
	}

	public synchronized void setMonitoringType(BioharnessMonitoringType monitoringType) {
		if (monitoringType == null) {
			monitoringType = BioharnessMonitoringType.None;
		}

		if (this.monitoringType != monitoringType) {

			// Disable old monitoring
			if (this.monitoringType == BioharnessMonitoringType.GeneralData) {
				setupGeneralDataMonitoring(false);
			} else if (this.monitoringType == BioharnessMonitoringType.SummaryData) {
				setupSummaryDataMonitoring(0);
			}

			// Enable new monitoring
			if (monitoringType == BioharnessMonitoringType.GeneralData) {
				setupGeneralDataMonitoring(true);
			} else if (monitoringType == BioharnessMonitoringType.SummaryData) {
				setupSummaryDataMonitoring(1);
			}

			this.monitoringType = monitoringType;
		}
	}

	private void setupGeneralDataMonitoring(boolean isMonitoring) {
		byte[] data = request.setupGeneralData(isMonitoring);
		write(data);
	}

	private void setupSummaryDataMonitoring(int transmissionRate) {
		byte[] data = request.setupSummaryData((short) transmissionRate);
		write(data);
	}

	/**
	 * Sets the monitoring status to denote whether we are actively connected to the BioHarness and monitoring data from
	 * it or not. Depending on the monitoring status, this method also sends a message to the BioHarness to start/stop
	 * receiving general data.
	 * 
	 * @param isMonitoring
	 */
	// private void setMonitoring(boolean isMonitoring) {
	// this.isMonitoring = isMonitoring;
	//
	// byte[] data = request.setupGeneralData(isMonitoring);
	// write(data);
	// }
	//
	// private void setSummaryMonitoring(int transmissionRate) {
	// this.isMonitoring = (transmissionRate > 0);
	// byte[] data = request.setupSummaryData((short) transmissionRate);
	// write(data);
	// }

	public synchronized boolean isConnected() {
		return mState == BHConnectivityStatus.CONNECTED;
	}

	public synchronized boolean isResting() {
		return mState == BHConnectivityStatus.DISCONNECTED;
	}

	public synchronized BHConnectivityStatus getStatus() {
		return mState;
	}

	public void startReceivingECGWaveFormData() {
		byte[] msg = request.setupECGWaveformData(true);
		write(msg);
	}

	public void stopReceivingECGWaveFormData() {
		byte[] msg = request.setupECGWaveformData(false);
		write(msg);
	}

	@Override
	public void onConnectionEstablished(BluetoothDevice device) {
		logger.debug("onConnectionEstablished starting ");

		if (defaultMonitoringType != BioharnessMonitoringType.None) {
			asyncSetMonitoringType(defaultMonitoringType, 800l);
		}

		/*
		 * Send the name of the connected BioHarness back to the UI Activity The only parameter to the message is the
		 * device name
		 */
		Message msg = mHandler.obtainMessage(BioHarness.MESSAGE_DEVICE_NAME);
		Bundle bundle = new Bundle();
		bundle.putString(BioHarness.DEVICE_NAME, device.getName());
		msg.setData(bundle);
		mHandler.sendMessage(msg);

		setState(BHConnectivityStatus.CONNECTED);

		logger.debug("onConnectionEstablished finished");
	}

	@Override
	public void onConnectionLost() {
		logger.debug("BEGIN onConnectionLost");

		synchronized (this) {
			mConnectThread = null;
			// isMonitoring = false;
			monitoringType = BioharnessMonitoringType.None;
			setState(BHConnectivityStatus.DISCONNECTED);
		}

		/*
		 * Tell the main activity about the problem with the connection. Only one parameter in the message so we won't
		 * use an identifier to name it.
		 */
		Message msg = mHandler.obtainMessage(BioHarness.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(BioHarness.TOAST_MESSAGE_STRING, "connectionLost(): Device connection was lost");
		msg.setData(bundle);
		mHandler.sendMessage(msg);

		logger.debug("END onConnectionLost");
	}

	@Override
	public boolean performTask(InputStream inputStream) {
		return readDataFromBioHarness(inputStream);
	}

	private final byte STX = 0x02;

	private boolean readDataFromBioHarness(InputStream inputStream) {
		byte[] buffer = new byte[1024];
		byte msgId;
		byte dlc;
		byte bytes;
		int bufferIndex;
		try {
			bufferIndex = 0;

			while ((bytes = (byte) inputStream.read()) != STX)
				;

			buffer[bufferIndex++] = bytes; // stx

			msgId = (byte) inputStream.read();// msg id
			buffer[bufferIndex++] = msgId;
			// logger.debug("in while loop: msg id read" + msgId);

			dlc = (byte) inputStream.read();// data load count
			buffer[bufferIndex++] = (byte) dlc;
			// logger.debug("in while loop: data load count read. dlc = " + dlc);

			while ((dlc--) > 0) {
				int b = inputStream.read();
				// logger.debug( "data retreived");
				buffer[bufferIndex++] = (byte) b;
				if (bufferIndex >= 7 && bufferIndex <= 12) {
					// logger.debug("[" + (bufferIndex - 1) + "] = " + b + " (" + buffer[bufferIndex - 1] + ")");
				}
			}
			// logger.debug("done reading payload");

			buffer[bufferIndex++] = (byte) inputStream.read();// CRC

			buffer[bufferIndex] = (byte) inputStream.read();// ETX or ACK/NAK

			logger.debug("ConnectedThread: msg id read" + msgId);
			// if (msgId == 32) {
			// logger.debug(String.format("m: %d, d: %d, ms1: %d, ms2: %d, ms3: %d, ms4: %d", buffer[6], buffer[7],
			// buffer[8], buffer[9], buffer[10], buffer[11]));
			// logger.debug(String.format("millis (8): %d", AbstractResponseData.getMillisForDay(buffer, 8)));
			// logger.debug(String.format("millis (test): %d",
			// AbstractResponseData.getMillisForDay(new byte[] { -128, 72, 60, 2 }, 0)));
			// logger.debug(String.format("millis (test): %d",
			// AbstractResponseData.getMillisForDay(new byte[] { 16, 125, 57, 2 }, 0)));
			// }
			Message msg = BioHarness.this.mHandler.obtainMessage(BioHarness.MESSAGE_READ, buffer);
			Bundle bundle = new Bundle();
			bundle.putByte(BioHarness.BIOHARNESS_MESSAGE_ID, msgId);
			msg.setData(bundle);
			BioHarness.this.mHandler.sendMessage(msg);

			return true;
		} catch (IOException e) {
			logger.error("Error when reading data from BioHarness...", e);
			// BioHarness.this.onConnectionLost();
			return false;
		}
	}

	private LinkedBlockingQueue<byte[]> mQueue;

	/**
	 * An alternate way of reading data using a queue (not fully tested)
	 */
	private void readFromBioHarnessOpt2(InputStream inputStream) {
		logger.info("Begin mConnectedThread");

		byte[] buffer = new byte[512];
		try {
			while (true) {
				int nbRead = 0;

				if (inputStream == null)
					break;
				nbRead = inputStream.read(buffer);

				if (nbRead <= 0)
					continue;
				try {
					byte[] data = new byte[nbRead];
					System.arraycopy(buffer, 0, data, 0, nbRead);
					this.mQueue.put(data);
				} catch (InterruptedException localInterruptedException) {
				}
			}
		} catch (IOException e) {
			logger.error("ConnectedThread: disconnected", e);
			BioHarness.this.onConnectionLost();
		}
	}

	private void initConditionsForReadionOpt2() {
		mQueue = new LinkedBlockingQueue<byte[]>();
		new Thread() {
			private boolean mmIsBreak = false;
			private int mmLength = -1;
			private final byte C_STX = 2;
			private final byte PAYLOAD_POS = 3;
			private ByteArrayOutputStream mmBuffer = new ByteArrayOutputStream();

			public void run() {
				try {
					Thread.sleep(100L);
				} catch (InterruptedException e) {
				}
				while (true) {
					if (mmIsBreak) {
						break;
					}
					try {
						byte[] load = mQueue.take();
						processLoad(load);
					} catch (InterruptedException e) {
						break;
					}
				}
			}

			public void interrupt() {
				mmIsBreak = true;
				super.interrupt();
			};

			private void processLoad(byte[] load) {
				logger.info("load length: " + load.length);
				List<byte[]> packets = new ArrayList<byte[]>();
				for (int index = 0; index < load.length; index++) {
					if (this.mmLength < 0) {
						if (load[index] == C_STX) {
							this.mmBuffer.reset();
							this.mmLength = 5; // min length
						}
					}

					if (this.mmLength < 0)
						continue;
					if (this.mmBuffer.size() <= this.mmLength) {
						this.mmBuffer.write(load[index]);
					}
					if (this.mmBuffer.size() == PAYLOAD_POS) {
						this.mmLength += load[index];
					}
					if (this.mmBuffer.size() < this.mmLength)
						continue;
					packets.add(this.mmBuffer.toByteArray());
					logger.debug(String.format("written a packet [%d] - %d.", mmBuffer.size(), mmLength));
					this.mmBuffer.reset();
					this.mmLength = -1;
				}

				logger.debug("submitting " + packets.size() + " data packets");
				for (byte[] packet : packets) {
					notifyHandler(packet, packet[1]);
				}
			}

			private void notifyHandler(byte[] payload, byte messageId) {
				logger.debug(String.format("read messageId: %d with payload length %d.", messageId, payload.length));
				if (messageId == 32) {
					logger.debug(String.format("m: %d, d: %d, ms1: %d, ms2: %d, ms3: %d, ms4: %d", payload[6],
							payload[7], payload[8], payload[9], payload[10], payload[11]));
					long millis = ((payload[11] & 0xFF) << 24) | ((payload[10] & 0xFF) << 16)
							| ((payload[9] & 0xFF) << 8) | (payload[8] & 0xFF);
					long seconds = millis / 1000; // millis = 33244280
					long minutes = seconds / 60;
					logger.debug(String.format("Time: %d: %d: %d: %d. (millis= %d)", (int) (minutes / 60),
							(minutes % 60), (seconds % 60), (millis % 1000), millis));
					logger.debug(String.format("millis (8): %d", AbstractResponseData.getMillisForDay(payload, 8)));
				}

				Message msg = BioHarness.this.mHandler.obtainMessage(BioHarness.MESSAGE_READ, payload);
				Bundle bundle = new Bundle();
				bundle.putByte(BioHarness.BIOHARNESS_MESSAGE_ID, messageId);
				msg.setData(bundle);
				BioHarness.this.mHandler.sendMessage(msg);
			}

		}.start();
	}

	/**
	 * Start the ConnectThread to initiate a connection to a remote device. The connection will happen asynchronously
	 * and the caller will be notified via the handler
	 * 
	 * @param device
	 *            The BluetoothDevice to connect with
	 * @see IBioHarnessMessageReceiver#onBioHarnessConnectivityStatusChanged(BHConnectivityStatus)
	 */
	public synchronized void connect(BluetoothDevice device) {
		logger.debug("connect(): starting connection to " + device);

		// If a connection attempt is currently in progress, cancel it!
		if (mState == BHConnectivityStatus.CONNECTING) {
			if (mConnectThread != null) {
				mConnectThread.cancel();
				mConnectThread = null;
			}
		}

		// Make the connection
		try {
			mConnectThread = new BTConnectThread(device, BIOHARNESS_UUID, this, this.numRetries);
			mConnectThread.start();
			setState(BHConnectivityStatus.CONNECTING);
		} catch (IOException e) {
			logger.error("Failed to connect to the BioHarness", e);
			setState(BHConnectivityStatus.DISCONNECTED);
		}

	}

	/**
	 * Gets the {@link BluetoothDevice} representing the physical {@link BioHarness}, if any. This method will return
	 * <code>null</code> if the bioharness is disconnected.
	 * 
	 * @return
	 */
	public synchronized BluetoothDevice getDevice() {
		if (mConnectThread == null) {
			return null;
		} else {
			return mConnectThread.getDevice();
		}
	}

	/**
	 * Start monitoring data in a separate thread as soon as the Bioharness is connected.
	 */
	// private void startMonitoringData() {
	// new Thread() {
	// public void run() {
	// logger.info("start sending msg to write");
	// try {
	// Thread.sleep(800l);
	// } catch (InterruptedException e) {
	// e.printStackTrace();
	// }
	// setMonitoring(true);
	// logger.info("auto started monitoring");
	// };
	// }.start();
	// }

	/**
	 * Stop all connections, put the BioHarness service back into the resting state.
	 * 
	 * The method ensures that all data monitoring activities are stopped (i.e general data and ecg data)
	 */
	public synchronized void stop() {
		logger.debug("stop() starting ");

		if (mConnectThread != null) {
			byte[] data = request.setupGeneralData(false); // cancel data packets being sent
			write(data);
			mConnectThread.cancel();
			mConnectThread = null;
		}

		setState(BHConnectivityStatus.DISCONNECTED);
		// setMonitoring(false);
		setMonitoringType(BioharnessMonitoringType.None);
		stopReceivingECGWaveFormData();
		logger.debug("stop() finished");
	}

	/**
	 * Set the current state of the HxmService connection to the device
	 * 
	 * @param state
	 *            an integer defining the current connection state
	 */
	private synchronized void setState(BHConnectivityStatus state) {
		logger.debug("setState() " + mState + "->" + state);
		mState = state;

		mHandler.obtainMessage(BioHarness.MESSAGE_STATE_CHANGE, state.ordinal(), -1).sendToTarget();
	}

	/**
	 * Write to the ConnectedThread in ansychronized manner
	 * 
	 * @param out
	 *            the bytes to write
	 */
	private void write(byte[] out) {
		final BTConnectThread connectedThread;

		synchronized (this) {
			if (mState != BHConnectivityStatus.CONNECTED) {
				return;
			}
			connectedThread = mConnectThread;
		}
		connectedThread.sendData(out);
	}

}
