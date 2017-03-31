package com.odin.android.bioharness.data;

import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Logger;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Represents the general data packet (received periodically from the BioHarness)
 * 
 * @author Thiranjith Weerasinghe
 * @author Andrew Meads
 * @see 9. Appendix B (pg 63) in "BioHarness Bluetooth Comms Link Specification 20100602" pdf for the detailed message
 *      structure.
 */
@DatabaseTable(tableName = "bh_general_data")
public class GeneralData extends MessageResponse {

	private static final Logger logger = Logger.getLogger(GeneralData.class);

	public static final byte MESSAGE_ID = 0x20;

	/**
	 * Number of bits in short (256)
	 */
	private static final int SHORT_SIZE = 256;

	@DatabaseField(canBeNull = false)
	private Date date;

	@DatabaseField(canBeNull = false)
	private Date deviceCaptureTime;

	@DatabaseField(canBeNull = false)
	private int lapMarker;

	@DatabaseField(canBeNull = false)
	private int deviceIndex;

	@DatabaseField(canBeNull = false)
	private int sequenceNum;

	@DatabaseField(canBeNull = false)
	private short heartRate;

	@DatabaseField(canBeNull = false)
	private double respirationRate;

	@DatabaseField(canBeNull = false)
	private double skinTemp;

	@DatabaseField(canBeNull = false)
	private short posture;

	@DatabaseField(canBeNull = false)
	private double bloodPressure;

	private byte stx;
	private byte dlc;
	private double vmu;
	private double peakAccerleration;
	private double voltage;
	private double breathingWaveAmp;
	private double ecgAmp;
	private double ecgNoise;
	private double xMin;
	private double xMax;
	private double yMin;
	private double yMax;
	private double zMin;
	private double zMax;
	private short systemChannel;
	private int gsr;
	private short spo2;
	private short alarmStatus;
	private short bwbStatus;
	private byte crc;
	private byte end;

	// Required for ORMLite
	public GeneralData() {
	}

	public GeneralData(byte messageId, Date date, Date deviceCaptureTime, int deviceIndex, int lapMarker,
			int sequenceNumber, short heartRate, float respirationRate, float skinTemperature, short posture,
			int bloodPressure) {
		super(messageId);

		this.date = date;
		this.deviceCaptureTime = deviceCaptureTime;
		this.deviceIndex = deviceIndex;
		this.lapMarker = lapMarker;
		this.sequenceNum = sequenceNumber;
		this.heartRate = heartRate;
		this.respirationRate = respirationRate;
		this.skinTemp = skinTemperature;
		this.posture = posture;
		this.bloodPressure = bloodPressure;
	}

	public GeneralData(byte[] buffer) {
		super(buffer[1]); // message id
		this.deviceIndex = -1;
		this.deviceCaptureTime = Calendar.getInstance().getTime();
		try {
			stx = buffer[0];
			dlc = buffer[2];

			sequenceNum = (0xFF & buffer[3]);

			int year = merge(buffer[4], buffer[5]);
			int month = buffer[6];
			int day = buffer[7];
			// LS (as 2 bytes) and then MS (as 2 bytes)
			int millisecond = (buffer[11] << 24) + ((buffer[10] & 0xFF) << 16) + ((buffer[9] & 0xFF) << 8)
					+ (buffer[8] & 0xFF);
			logger.debug("millisecond calc: " + millisecond + ", day: " + day + ", month: " + month + ", year: " + year
					+ ", seq: " + sequenceNum);
			date = new Date(new Date(year - 1900, month - 1, day).getTime() + millisecond);
			logger.debug("date calc: " + DATE_FORMAT.format(date));

			heartRate = (short) merge(buffer[12], buffer[13]);
			respirationRate = merge(buffer[14], buffer[15]) / 10.0;
			skinTemp = merge(buffer[16], buffer[17]) / 10.0;
			posture = (short) merge(buffer[18], buffer[19]);

			vmu = merge(buffer[20], buffer[21]) / 100.0;
			peakAccerleration = merge(buffer[22], buffer[23]) / 100.0;
			voltage = merge(buffer[24], buffer[25]) / 1000.0;
			breathingWaveAmp = merge(buffer[26], buffer[27]) / 1000.0;
			ecgAmp = merge(buffer[28], buffer[29]) / 1000000.00;
			ecgNoise = merge(buffer[30], buffer[31]) / 1000000.00;

			xMin = merge(buffer[32], buffer[33]) / 100.00;
			xMax = merge(buffer[34], buffer[35]) / 100.00;
			yMin = merge(buffer[36], buffer[37]) / 100.00;
			yMax = merge(buffer[38], buffer[39]) / 100.00;
			zMin = merge(buffer[40], buffer[41]) / 100.00;
			zMax = merge(buffer[42], buffer[43]) / 100.00;

			systemChannel = 0; // ignore
			gsr = merge(buffer[46], buffer[47]);
			spo2 = (short) merge(buffer[48], buffer[49]);

			bloodPressure = merge(buffer[50], buffer[51]) / 1000.0;

			alarmStatus = (short) merge(buffer[52], buffer[53]);
			bwbStatus = (short) merge(buffer[54], buffer[55]);

			crc = buffer[56];
			end = buffer[57];
		} catch (Exception e) {
			logger.error("Failure building general data", e);
		}
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

	public String getFormattedBHCapturedDate() {
		return DATE_FORMAT.format(this.date);
	}

	public String getFormattedDeviceCapturedDate() {
		return DATE_FORMAT.format(this.deviceCaptureTime);
	}

	public int getLapMarker() {
		return lapMarker;
	}

	public void setLapMarker(int lapMarker) {
		this.lapMarker = lapMarker;
	}

	public int getSequenceNum() {
		return sequenceNum;
	}

	public int getHeartRate() {
		return heartRate;
	}

	public Date getCapturedTime() {
		return date;
	}

	public double getVoltage() {
		return voltage;
	}

	public double getXMin() {
		return xMin;
	}

	public double getXMax() {
		return xMax;
	}

	public double getYMin() {
		return yMin;
	}

	public double getYMax() {
		return yMax;
	}

	public double getZMin() {
		return zMin;
	}

	public double getZMax() {
		return zMax;
	}

	public double getRespirationRate() {
		return respirationRate;
	}

	public double getSkinTemp() {
		return skinTemp;
	}

	public short getPosture() {
		return posture;
	}

	public double getBloodPressure() {
		return bloodPressure;
	}

	public double getEcgAmp() {
		return ecgAmp;
	}

	public double getEcgNoise() {
		return ecgNoise;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.getId()).append(",");
		sb.append(this.sequenceNum).append(",").append(DATE_FORMAT.format(this.date)).append(",");
		sb.append(this.heartRate).append(",").append(this.skinTemp).append(",");
		sb.append(this.respirationRate).append(",").append(this.posture).append(",");
		sb.append(this.bloodPressure);
		return sb.toString();
	}
}
