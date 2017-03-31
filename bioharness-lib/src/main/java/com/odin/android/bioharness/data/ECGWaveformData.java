package com.odin.android.bioharness.data;

import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Logger;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Represents the ECG Waveform data transmitted by the BioHarness (received periodically from the BioHarness after
 * enabling ECG waveform). Once enabled (by the "Set ECG Waveform Packet Transmission State" message), the packet is
 * sent periodically to a remote unit (every 252ms). Each ECG Waveform sample is 4ms later than the previous one.
 * 
 * @author Thiranjith Weerasinghe
 * @see Section 6.4 (pg 54) in "BioHarness Bluetooth Comms Link Specification 20100602" pdf for the detailed message
 *      structure.
 */
@DatabaseTable(tableName = "bh_ecg_data")
public class ECGWaveformData extends MessageResponse {
	private static final Logger logger = Logger.getLogger(ECGWaveformData.class);

	/**
	 * The time difference between two ECG samples in milliseconds.
	 * 
	 * NOTE: The data is captured at 252Hz.
	 */
	private static final int SAMPLE_PERIOD_IN_MILLIS = 4;
	public static final byte MESSAGE_ID = 0x22;

	private static final int DATA_OFFSET = 12;
	private static final int SAMPLE_SIZE = 63;

	@DatabaseField(canBeNull = false)
	private Date capturedTime;

	@DatabaseField(canBeNull = false)
	private int lapMarker;

	@DatabaseField(canBeNull = false)
	private int sequenceNum;

	@DatabaseField(canBeNull = false)
	private int deviceIndex;

	@DatabaseField(canBeNull = false)
	private Date deviceCaptureTime;
	/**
	 * Stores a bit-packed sequence of data as received from the BioHarness containing 63 samples (each sample
	 * consisting of 10-bit value) as identified in section 6.4.1 (pg 54)
	 */
	@DatabaseField(canBeNull = false, dataType = DataType.BYTE_ARRAY)
	private byte[] rawBitpackedData;

	private byte stx;
	private byte dlc;

	private byte crc;
	private byte end;

	private int[] processedData;

	// Required for ORMLite
	public ECGWaveformData() {
	}

	public ECGWaveformData(Date capturedTime, Date deviceCaptureTime, int deviceIndex, int lapMarker,
			int sequenceNumber, int[] processedData) {
		super(MESSAGE_ID);

		this.lapMarker = lapMarker;
		this.processedData = processedData;
		this.sequenceNum = sequenceNumber;
		this.capturedTime = capturedTime;
		this.deviceCaptureTime = deviceCaptureTime;
		this.deviceIndex = deviceIndex;
	}

	public ECGWaveformData(Date capturedTime, Date deviceCaptureTime, int deviceIndex, int lapMarker,
			int sequenceNumber, byte[] rawData) {
		super(MESSAGE_ID);

		this.lapMarker = lapMarker;
		this.rawBitpackedData = rawData;
		this.sequenceNum = sequenceNumber;
		this.capturedTime = capturedTime;
		this.deviceCaptureTime = deviceCaptureTime;
		this.deviceIndex = deviceIndex;
	}

	public ECGWaveformData(byte[] buffer) {
		super(buffer[1]); // message id

		this.deviceCaptureTime = Calendar.getInstance().getTime();
		this.deviceIndex = -1;
		rawBitpackedData = new byte[80];

		try {
			stx = buffer[0];
			dlc = buffer[2];
			sequenceNum = (0xFF & buffer[3]);

			int year = merge(buffer[4], buffer[5]);
			int month = buffer[6];
			int day = buffer[7];
			int millisecond = (buffer[11] << 24) + ((buffer[10] & 0xFF) << 16) + ((buffer[9] & 0xFF) << 8)
					+ (buffer[8] & 0xFF);

			capturedTime = new Date(new Date(year - 1900, month - 1, day).getTime() + millisecond);

			// logger.info("data[12]: " + buffer[DATA_OFFSET] + ", buffer[90]: " + buffer[90]);
			System.arraycopy(buffer, DATA_OFFSET, rawBitpackedData, 0, 80);
			// for(int idx = 0; idx < 80; idx++) {
			// logger.info("p[" + (idx %5) + "] idx: " + (DATA_OFFSET + idx) + " - val: " + rawBitpackedData[idx]);
			// }
			// logger.warn("-- processed:");
			// int[] processed = getSamples();
			// for(int idx =0; idx < processed.length; idx++) {
			// logger.info("[ " + idx + " ] = " + processed[idx]);
			// }

			crc = buffer[91];
			end = buffer[92];
		} catch (Exception e) {
			logger.error("Failed to build ECG data", e);
		}
	}

	public String getFormattedBHCapturedDate() {
		return DATE_FORMAT.format(this.capturedTime);
	}

	/**
	 * Get the time for the given sample
	 * 
	 * @param sampleIndex
	 *            sample index starting with <code>zero</code>.
	 * @return
	 */
	public String getFormattedBHCapturedDataForSample(int sampleIndex) {
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.setTime(this.capturedTime);
		cal.add(Calendar.MILLISECOND, SAMPLE_PERIOD_IN_MILLIS * sampleIndex);

		return DATE_FORMAT.format(cal.getTime());
	}

	public String getFormattedDeviceCapturedDate() {
		return DATE_FORMAT.format(this.deviceCaptureTime);
	}

	public int getLapMarker() {
		return lapMarker;
	}

	/**
	 * @return the index assigned to this datapoint by the smartphone
	 */
	public int getDeviceIndex() {
		return deviceIndex;
	}

	/**
	 * @return the time that the datapoint was captured by the smart-phone
	 */
	public Date getDeviceCaptureTime() {
		return deviceCaptureTime;
	}

	public int[] getSamples() {

		if (processedData != null) {
			return processedData;
		}

		int[] data = new int[SAMPLE_SIZE];

		int index = 0;
		int samples = 0;
		int value;
		int temp;
		while (samples < SAMPLE_SIZE) // 63 samples per ECG packet
		{
			switch (index % 5) {
			case 0:
			default:
				temp = rawBitpackedData[index++];
				value = temp < 0 ? 256 + temp : temp;
				value += ((int) rawBitpackedData[index] & 0x03) << 8;
				data[samples] = value;
				break;
			case 1:
				temp = rawBitpackedData[index++] >>> 2;
				value = (temp & 0x3f);
				value += ((int) rawBitpackedData[index] & 0x0F) << 6;
				data[samples] = value;
				break;
			case 2:
				temp = rawBitpackedData[index++] >>> 4;
				value = (temp & 0x0f);
				value += ((int) rawBitpackedData[index] & 0x3F) << 4;
				data[samples] = value;
				break;
			case 3:
				temp = rawBitpackedData[index++] >>> 6;
				value = (temp & 0x03);
				temp = (int) rawBitpackedData[index++];
				value += temp < 0 ? (256 + temp) << 2 : temp << 2;
				data[samples] = value;
				break;
			}
			samples++;
		}
		return data;
	}

	public int getSequenceNumber() {
		return sequenceNum;
	}

	public Date getCapturedTime() {
		return capturedTime;
	}

	public byte[] getRawData() {
		return rawBitpackedData;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.getId()).append(",").append(this.sequenceNum).append(",");
		sb.append(this.capturedTime.getTime());
		for (byte rawData : rawBitpackedData) {
			sb.append(",").append(rawData);
		}
		return sb.toString();
	}

	public void setLapMarker(int lapMarker) {
		this.lapMarker = lapMarker;
	}
}
