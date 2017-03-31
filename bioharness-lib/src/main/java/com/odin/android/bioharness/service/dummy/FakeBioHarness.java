package com.odin.android.bioharness.service.dummy;

import java.util.Random;

import android.os.Handler;

import com.odin.android.bioharness.data.SummaryData;

public class FakeBioHarness {

	public static final int MESSAGE_DUMMY_BH_SUMMARY_DATA = 100;
	public static final int MESSAGE_DUMMY_BH_CONNECTED = 101;
	public static final int MESSAGE_DUMMY_BH_CONNECTING = 102;
	public static final int MESSAGE_DUMMY_BH_DISCONNECTED = 103;

	final float LOWER = 60, HIGHER = 180, RANGE = HIGHER - LOWER;
	final float LOWER_OFFS = 0;
	final float HIGHER_OFFS = 10;

	private Thread thread;

	private final Handler handler;
	private final long intervalMillis;

	private int sequenceNum = 0;
	private int heartRate = 70;

	private static final Random rnd = new Random();

	public FakeBioHarness(Handler handler, long intervalMillis) {
		this.handler = handler;
		this.intervalMillis = intervalMillis;
	}

	public synchronized boolean isConnected() {
		return thread != null;
	}

	public synchronized void start() {
		if (thread == null) {

			handler.obtainMessage(MESSAGE_DUMMY_BH_CONNECTING).sendToTarget();

			thread = new Thread(genFakeDataTask);
			thread.start();
		}
	}

	public synchronized void stop() {
		if (thread != null) {
			thread.interrupt();
			thread = null;
			sequenceNum = 0;
			heartRate = 70;
		}
	}

	private final Runnable genFakeDataTask = new Runnable() {

		@Override
		public void run() {
			try {

				// Send a "connected" message before starting.
				// Thread.sleep(2000);
				handler.obtainMessage(MESSAGE_DUMMY_BH_CONNECTED).sendToTarget();

				while (!Thread.interrupted()) {
					Thread.sleep(intervalMillis);

					SummaryData data = new SummaryData(sequenceNum, heartRate, 0, 0, 0, (byte) 100, (byte) 100,
							(byte) 100, 0, 0, 0);

					sequenceNum++;

					float fractionOfRange = (heartRate - LOWER) / RANGE;
					int thisOffset = Math.round(fractionOfRange * HIGHER_OFFS + (1 - fractionOfRange) * LOWER_OFFS);
					heartRate += (rnd.nextInt(11) - thisOffset);

					handler.obtainMessage(MESSAGE_DUMMY_BH_SUMMARY_DATA, data).sendToTarget();

				}
			} catch (InterruptedException e) {

			}

			// Send a "disconnected" message just before finishing.
			handler.obtainMessage(MESSAGE_DUMMY_BH_DISCONNECTED).sendToTarget();
		}
	};

}
