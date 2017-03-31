package com.odin.android.bioharness.service;

import com.odin.android.bioharness.BioHarness;

/**
 * A pojo representing snapshot of current {@link BioHarness} status
 * 
 * @author Thiranjith Weerasinghe
 */
public final class BioHarnessStatus {

	private boolean mIsConnected;
	private boolean mIsMonitoringData;
	private boolean mIsMonitoringECGData;
	private boolean mIsRecordingData;
	private long mRecordingStartTimeInMillis;
	private int mLapCount;

	public BioHarnessStatus() {
		mIsConnected = false;
		mIsMonitoringData = false;
		mIsMonitoringECGData = false;
		mIsRecordingData = false;
		mRecordingStartTimeInMillis = 0;
		mLapCount = 0;
	}

	/**
	 * @return <code>true</code> if the bioharness is currently connected
	 * 
	 * @see BioHarness#isConnected()
	 */
	public boolean isConnected() {
		return mIsConnected;
	}

	/**
	 * @return <code>true</code> if the bioharness is monitoring data
	 */
	public boolean isMonitoringData() {
		return mIsMonitoringData;
	}

	/**
	 * @return <code>true</code> if the service is set to record data to the database. Note that the BioHarness may or
	 *         may not be currently connected at this point.
	 */
	public boolean isRecordingData() {
		return mIsRecordingData;
	}

	/**
	 * @return <code>true</code> if the bioharness is monitoring ECG data. This implies that {@link #isMonitoringData()}
	 *         is also <code>true</code>
	 */
	public boolean isMonitoringECGData() {
		return mIsMonitoringECGData;
	}

	/**
	 * @return the time that data recording has started in milliseconds. Only valid if {@link #isRecordingData()} is
	 *         <code>true</code>.
	 */
	public long getRecordingStartTimeInMillis() {
		return mRecordingStartTimeInMillis;
	}

	/**
	 * @return the number of laps being currently recorded. Only valid if {@link #isRecordingData()} is
	 *         <code>true</code>.
	 */
	public int getLapCount() {
		return mLapCount;
	}

	public void setIsConnected(boolean isConnected) {
		mIsConnected = isConnected;
	}

	public void setIsMonitoringData(boolean isMonitoringData, boolean isMonitoringECGData) {
		mIsMonitoringData = isMonitoringData;
		mIsMonitoringECGData = isMonitoringECGData;
	}

	public void setIsRecordingData(boolean isRecordingData, int lapCount, long recordingStartTimeInMillis) {
		mIsRecordingData = isRecordingData;
		mLapCount = lapCount;
		mRecordingStartTimeInMillis = recordingStartTimeInMillis;
	}
}
