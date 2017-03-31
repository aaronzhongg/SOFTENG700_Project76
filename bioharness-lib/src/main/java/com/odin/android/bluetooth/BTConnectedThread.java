package com.odin.android.bluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.log4j.Logger;

import android.bluetooth.BluetoothSocket;

/**
 * A long running {@link Thread} responsible for communicating with the Bluetooth device. E.g. receiving data from it
 * etc.
 * 
 * @author Thiranjith Weerasinghe
 */
class BTConnectedThread extends Thread {

	interface IConnectedTask {
		/**
		 * Performs a short-running task (a blocking call)
		 * 
		 * @param inputStream
		 * @return <code>false</code> if thread is to be terminated.
		 */
		boolean performTask(InputStream inputStream);

		/**
		 * Callback if the {@link BTConnectedThread} is terminated
		 */
		void onConnectionLost();
	}

	private static final Logger logger = Logger.getLogger(BTConnectedThread.class);

	private InputStream mInStream;
	private OutputStream mOutStream;
	private IConnectedTask mConnectedTask;

	BTConnectedThread(BluetoothSocket socket, IConnectedTask connectedTask) throws IOException {
		// Get the input and output streams, using temp objects because member streams are final
		mInStream = socket.getInputStream();
		mOutStream = socket.getOutputStream();

		mConnectedTask = connectedTask;
	}

	@Override
	public void run() {
		while (true) {
			synchronized (this) {				
				if (mInStream == null) {
					mConnectedTask.onConnectionLost();
					break;
				}
			}
			
			if (!mConnectedTask.performTask(mInStream)) {
				mConnectedTask.onConnectionLost();
				close();
				break;
			}
		}
	}

	/**
	 * Write to the connected Bluetooth device
	 * 
	 * @param buffer
	 *            the bytes to write
	 * @return <code>true</code> if data is successfully written. Otherwise return <code>false</code>.
	 */
	boolean write(byte[] buffer) {
		boolean isSuccess = true;
		try {
			logger.debug("writing data..." + buffer.toString());
			mOutStream.write(buffer);
			mOutStream.flush();
		} catch (IOException e) {
			logger.error("exception during write", e);
			isSuccess = false;
		}
		return isSuccess;
	}

	/**
	 * Call this whenever shutting down the Bluetooth connection.
	 */
	synchronized void close() {
		if (mInStream != null) {
			try {
				mInStream.close();
				mInStream = null;
			} catch (IOException e) {
				logger.warn("Failed to close the input stream for BT socket.", e);
			}
		}
		if (mOutStream != null) {
			try {
				mOutStream.close();
				mOutStream = null;
			} catch (IOException e) {
				logger.warn("Failed to close the putput stream for BT socket.", e);
			}
		}
	}
}
