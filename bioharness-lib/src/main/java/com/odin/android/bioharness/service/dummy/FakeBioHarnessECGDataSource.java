package com.odin.android.bioharness.service.dummy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;

import org.apache.log4j.Logger;

import android.content.Context;
import android.os.Handler;

import com.odin.android.bioharness.R.raw;
import com.odin.android.bioharness.data.ECGWaveformData;

public class FakeBioHarnessECGDataSource {

	private static final Logger logger = Logger.getLogger(FakeBioHarnessECGDataSource.class);

	public static final int MESSAGE_DUMMY_BH_ECG_DATA = 1000;

	private final int[] fakeECGData;

	private Thread thread;

	private final Handler handler;

	private int sequenceNum = 0;
	private int currentOffset = 0;

	public FakeBioHarnessECGDataSource(Handler handler, Context context) {
		this.handler = handler;

		// Load fake ECG data.
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(context.getResources()
					.openRawResource(raw.dummy_ecg_data)));

			String data = reader.readLine();
			String[] items = data.split(",");
			fakeECGData = new int[items.length];
			for (int i = 0; i < items.length; i++) {
				fakeECGData[i] = Integer.parseInt(items[i]);
			}

		} catch (IOException e) {
			logger.error(e);
			throw new RuntimeException(e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					logger.error(e);
				}
			}
		}
	}

	public synchronized void start() {
		if (thread == null) {

			thread = new Thread(sendFakeECGTask);
			thread.start();
		}
	}

	public synchronized void stop() {
		if (thread != null) {
			thread.interrupt();
			thread = null;
			sequenceNum = 0;
		}
	}

	private final Runnable sendFakeECGTask = new Runnable() {

		@Override
		public void run() {
			try {

				long nextSleepInterval = 1000;
				while (!Thread.interrupted()) {
					Thread.sleep(nextSleepInterval);

					// Create array to store values
					int[] values = new int[63];
					Date time = new Date(System.currentTimeMillis());

					// Copy data into array
					if (fakeECGData.length - currentOffset >= values.length) {
						System.arraycopy(fakeECGData, currentOffset, values, 0, values.length);
					} else {
						System.arraycopy(fakeECGData, currentOffset, values, 0, fakeECGData.length - currentOffset);
						System.arraycopy(fakeECGData, 0, values, fakeECGData.length - currentOffset, values.length
								- (fakeECGData.length - currentOffset));
					}

					currentOffset = (currentOffset + values.length) % fakeECGData.length;

					ECGWaveformData packet = new ECGWaveformData(time, time, 0, 0, sequenceNum, values);
					handler.obtainMessage(MESSAGE_DUMMY_BH_ECG_DATA, packet).sendToTarget();
					sequenceNum++;
					nextSleepInterval = values.length * 4;
				}
			} catch (InterruptedException e) {

			}
		}
	};

}
