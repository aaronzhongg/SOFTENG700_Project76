package com.odin.android.bluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.apache.log4j.Logger;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import com.odin.android.bluetooth.BTConnectedThread.IConnectedTask;

/**
 * Thread responsible for establishing a connection with a Bluetooth device
 * 
 * @author Thiranjith Weerasinghe
 * @see http://developer.android.com/guide/topics/wireless/bluetooth.html#ConnectingAsAClient
 */
public final class BTConnectThread extends Thread {

	public interface IBTConnectListener {
		/**
		 * Callback method that get invoked when it fails to establish connectivity with the Bluetooth device. The
		 * {@link IBTConnectListener} must re-establish a new connection with the Bluetooth device by starting a new
		 * thread.
		 */
		void onConnectionLost();

		/**
		 * Get notified when a successful Bluetooth connection is established with the Bluetooth device.
		 * 
		 * @param device
		 *            {@link BluetoothDevice} that was connected
		 */
		void onConnectionEstablished(BluetoothDevice device);

		/**
		 * For the listener to read data sent by the Bluetooth device and perform its own tasks. The task should not be
		 * a long running task since this is a blocking call (i.e. Bluetooth device will be blocked till the listener is
		 * done reading from the inputStream).
		 * 
		 * @param inputStream
		 * @return <code>true</code> if <code>inputStream</code> was read successfully. Otherwise (e.g. fails to read or
		 *         an error happens) return <code>false</code>.
		 */
		boolean performTask(InputStream inputStream);
	}

	private static final Logger logger = Logger.getLogger(BTConnectThread.class);

	private final UUID mDeviceUuid;
	private final Object mSocketLock = new Object();
	private BluetoothSocket mSocket;
	private final BluetoothDevice mDevice;
	private final IBTConnectListener mListener;
	private BTConnectedThread mConnectedThread;
	private final int numRetries;

	/**
	 * 
	 * @param device
	 * @param deviceUuid
	 * @param listener
	 * @throws IOException
	 *             if fails to establish RFCOMM socket with the Bluetooth device
	 */
	public BTConnectThread(BluetoothDevice device, UUID deviceUuid, IBTConnectListener listener, int numRetries)
			throws IOException {
		mDevice = device;
		mListener = listener;
		mDeviceUuid = deviceUuid;
		mConnectedThread = null;
		this.numRetries = numRetries;

		// get the socket to enable communication with the bluetooth device
		synchronized (mSocketLock) {
			mSocket = device.createRfcommSocketToServiceRecord(mDeviceUuid);
		}
	}

	public synchronized BluetoothDevice getDevice() {
		return mDevice;
	}

	@Override
	public void run() {
		logger.info("begin mConnectThread");

		final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		if (adapter != null) {
			adapter.cancelDiscovery();
		}

		for (int attempt = 0; attempt < numRetries; attempt++) {
			try {

				logger.debug("run(): Attempting to connect to Bioharness...");

				// This is a blocking call and will only return on a successful connection or an exception
				synchronized (mSocketLock) {
					if (mSocket != null) {
						mSocket.connect();
						mConnectedThread = new BTConnectedThread(mSocket, new IConnectedTask() {
							@Override
							public boolean performTask(InputStream inputStream) {
								return mListener.performTask(inputStream);
							}

							@Override
							public void onConnectionLost() {
								cancel();
							}
						});
					}
				}
				if (mConnectedThread != null) {
					mConnectedThread.start();
					mListener.onConnectionEstablished(mDevice);
					logger.debug("run(): Connection successful!");
				}

				else {
					logger.warn("run(): null mSocket when trying to create mConnectedThread.");
				}

				break;

			} catch (IOException e) {
				int retriesRemaining = numRetries - attempt - 1;
				logger.warn("run(): Could not connect to Bioharness. " + retriesRemaining + " retries remaining.");

				if (retriesRemaining == 0) {
					onConnectionLost();
				} else {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException eInterrupted) {
						logger.warn("run(): Connect thread was interrupted during retries, so will exit.");
						break;
					}
				}
			}
		}
	}

	/**
	 * Sends the data to the Bluetooth device
	 * 
	 * @param dataForBluetoothDevice
	 * @return <code>true</code> if data was sent to the device successfully.
	 */
	public boolean sendData(byte[] dataForBluetoothDevice) {
		boolean isSuccess = false;
		if (mConnectedThread != null) {
			isSuccess = mConnectedThread.write(dataForBluetoothDevice);
		}
		return isSuccess;
	}

	/**
	 * Will cancel an in-progress connection, and close the socket
	 */
	public void cancel() {
		if (mConnectedThread != null) {
			mConnectedThread.close();
			mConnectedThread = null;
		}
		onConnectionLost();
	}

	private void onConnectionLost() {
		closeRfcommSocket();
		mListener.onConnectionLost();
	}

	private void closeRfcommSocket() {
		synchronized (mSocketLock) {
			if (mSocket != null) {
				try {
					mSocket.close();
					mSocket = null;
				} catch (IOException e) {
					logger.warn("Failed to close the BT socket", e);
				}
			}
		}
	}
}
