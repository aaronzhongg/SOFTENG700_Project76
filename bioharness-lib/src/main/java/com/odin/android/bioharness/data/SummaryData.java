package com.odin.android.bioharness.data;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.log4j.Logger;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "bh_summary_data")
public class SummaryData extends MessageResponse {

	private static final Logger logger = Logger.getLogger(SummaryData.class);

	public static final byte MESSAGE_ID = 0x2B;

	private int deviceIndex;

	@DatabaseField(canBeNull = false)
	private Date deviceCaptureTime;

	private byte stx;

	private byte dlc;

	@DatabaseField(canBeNull = false)
	private int sequenceNum;

	@DatabaseField(canBeNull = false)
	private Date date;

	private byte versionNumber;

	@DatabaseField(canBeNull = false)
	private int heartRate;

	@DatabaseField(canBeNull = false)
	private double respirationRate;

	@DatabaseField(canBeNull = false)
	private double skinTemp;

	@DatabaseField(canBeNull = false)
	private int posture;

	private double vmu;

	private double peakAcceleration;

	private double batteryVoltage;

	@DatabaseField(canBeNull = false)
	private byte batteryLevel;

	private double breathingWaveAmp;

	private double breathingWaveNoise;

	@DatabaseField(canBeNull = false)
	private byte respirationRateConfidence;

	private double ecgAmp;

	private double ecgNoise;

	@DatabaseField(canBeNull = false)
	private byte heartRateConfidence;

	@DatabaseField(canBeNull = false)
	private int heartRateVariability;

	@DatabaseField(canBeNull = false)
	private byte systemConfidence;

	private int gsr;

	private int rog;

	private double xMin;

	private double xMax;

	private double yMin;

	private double yMax;

	private double zMin;

	private double zMax;

	private double deviceInternalTemp;

	@DatabaseField(canBeNull = false)
	private int statusInfo;

	private int linkQuality;

	private byte rssi;

	private byte txPower;

	@DatabaseField(canBeNull = false)
	private double coreTemp;

	private int adcChannel1;

	private int adcChannel2;

	private int adcChannel3;

	private int reserved;

	private byte crc;

	private byte end;

	// Required for ORMLite
	public SummaryData() {

	}

	/**
	 * Constructor for fake BioHarness / test data / etc.
	 */
	public SummaryData(int sequenceNum, int heartRate, double respirationRate, double skinTemp, int posture,
			byte batteryLevel, byte respirationRateConfidence, byte heartRateConfidence, int heartRateVariability,
			int statusInfo, double coreTemp) {

		this.deviceCaptureTime = new Date();
		this.date = new Date();

		this.sequenceNum = sequenceNum;
		this.heartRate = heartRate;
		this.respirationRate = respirationRate;
		this.skinTemp = skinTemp;
		this.posture = posture;
		this.batteryLevel = batteryLevel;
		this.respirationRateConfidence = respirationRateConfidence;
		this.heartRateConfidence = heartRateConfidence;
		this.heartRateVariability = heartRateVariability;
		this.statusInfo = statusInfo;
		this.coreTemp = coreTemp;
	}

	/**
	 * Constructor for legit BioHarness
	 * 
	 * @param buffer
	 */
	public SummaryData(byte[] buffer) {
		super(buffer[1]);

		this.deviceIndex = -1;
		this.deviceCaptureTime = Calendar.getInstance().getTime();

		try {

			this.stx = buffer[0];
			this.dlc = buffer[2];
			logger.debug("SummaryData.ctor(): DLC = " + this.dlc);

			this.sequenceNum = 0xFF & buffer[3];

			int year = merge(buffer[4], buffer[5]);
			int month = buffer[6];
			int day = buffer[7];
			int millisecond = (buffer[11] << 24) + ((buffer[10] & 0xFF) << 16) + ((buffer[9] & 0xFF) << 8)
					+ (buffer[8] & 0xFF);
			GregorianCalendar cal = (GregorianCalendar) GregorianCalendar.getInstance();
			cal.set(year, month - 1, day);
			cal.add(GregorianCalendar.MILLISECOND, millisecond);
			this.date = cal.getTime();

			this.versionNumber = buffer[12];

			this.heartRate = merge(buffer[13], buffer[14]);
			this.respirationRate = merge(buffer[15], buffer[16]) / 10.0;
			this.skinTemp = merge(buffer[17], buffer[18]) / 10.0;
			this.posture = merge(buffer[19], buffer[20]);

			this.vmu = merge(buffer[21], buffer[22]) / 100.0;
			this.peakAcceleration = merge(buffer[23], buffer[24]) / 100.0;

			this.batteryVoltage = merge(buffer[25], buffer[26]) / 1000.0;
			this.batteryLevel = buffer[27];

			this.breathingWaveAmp = merge(buffer[28], buffer[29]) / 1000.0;
			this.breathingWaveNoise = merge(buffer[30], buffer[31]) / 1000.0;
			this.respirationRateConfidence = buffer[32];

			this.ecgAmp = merge(buffer[33], buffer[34]) / 1000000.00;
			this.ecgNoise = merge(buffer[35], buffer[36]) / 1000000.00;

			this.heartRateConfidence = buffer[37];
			this.heartRateVariability = merge(buffer[38], buffer[39]);

			this.systemConfidence = buffer[40];

			this.gsr = merge(buffer[41], buffer[42]);
			this.rog = merge(buffer[43], buffer[44]);

			this.xMin = merge(buffer[45], buffer[46]) / 100.00;
			this.xMax = merge(buffer[47], buffer[48]) / 100.00;
			this.yMin = merge(buffer[49], buffer[50]) / 100.00;
			this.yMax = merge(buffer[51], buffer[52]) / 100.00;
			this.zMin = merge(buffer[53], buffer[54]) / 100.00;
			this.zMax = merge(buffer[55], buffer[56]) / 100.00;

			this.deviceInternalTemp = merge(buffer[57], buffer[58]) / 10.0;
			this.statusInfo = merge(buffer[59], buffer[60]);
			this.linkQuality = 0xFF & buffer[61];
			this.rssi = buffer[62];
			this.txPower = buffer[63];

			this.coreTemp = merge(buffer[64], buffer[65]) / 10.0;

			this.adcChannel1 = merge(buffer[66], buffer[67]);
			this.adcChannel2 = merge(buffer[68], buffer[69]);
			this.adcChannel3 = merge(buffer[70], buffer[71]);

			this.reserved = merge(buffer[72], buffer[73]);

			this.crc = buffer[74];
			this.end = buffer[75];

		} catch (Exception e) {
			logger.error("Failure building summary data", e);
		}
	}

	public boolean isHeartRateReliable() {
		// return (getHRUnreliableFlag() && getHeartRateConfidence() >= 75)
		// || (!getHRUnreliableFlag() && getHeartRateConfidence() >= 30);

		// return !getHRUnreliableFlag() || (getHRUnreliableFlag() && getHeartRateConfidence() >= 50);

		return getHeartRateConfidence() >= 50;
	}

	public int getDeviceWornDetectionLevel() {
		return 0x3 & this.statusInfo;
	}

	public boolean getButtonPressDetectionFlag() {
		return (0x4 & this.statusInfo) != 0;
	}

	public boolean getNotFittedToGarmentFlag() {
		return (0x8 & this.statusInfo) != 0;
	}

	public boolean getHRUnreliableFlag() {
		return (0x10 & this.statusInfo) != 0;
	}

	public boolean getRespirationRateUnreliableFlag() {
		return (0x20 & this.statusInfo) != 0;
	}

	public boolean getSkinTempUnreliableFlag() {
		return (0x40 & this.statusInfo) != 0;
	}

	public boolean getPostureUnreliableFlag() {
		return (0x80 & this.statusInfo) != 0;
	}

	public boolean getActivityUnreliableFlag() {
		return (0x100 & this.statusInfo) != 0;
	}

	public boolean getHeartRateVariabilityUnreliableFlag() {
		return (0x200 & this.statusInfo) != 0;
	}

	public boolean getCoreTempUnreliableFlag() {
		return (0x400 & this.statusInfo) != 0;
	}

	public Date getDeviceCaptureTime() {
		return deviceCaptureTime;
	}

	// public void setDeviceCaptureTime(Date deviceCaptureTime) {
	// this.deviceCaptureTime = deviceCaptureTime;
	// }

	public int getSequenceNum() {
		return sequenceNum;
	}

	// public void setSequenceNum(int sequenceNum) {
	// this.sequenceNum = sequenceNum;
	// }

	public int getHeartRate() {
		return heartRate;
	}

	// public void setHeartRate(int heartRate) {
	// this.heartRate = heartRate;
	// }

	public double getRespirationRate() {
		return respirationRate;
	}

	// public void setRespirationRate(double respirationRate) {
	// this.respirationRate = respirationRate;
	// }

	public double getSkinTemp() {
		return skinTemp;
	}

	// public void setSkinTemp(double skinTemp) {
	// this.skinTemp = skinTemp;
	// }

	public int getPosture() {
		return posture;
	}

	// public void setPosture(int posture) {
	// this.posture = posture;
	// }

	public byte getBatteryLevel() {
		return batteryLevel;
	}

	// public void setBatteryLevel(byte batteryLevel) {
	// this.batteryLevel = batteryLevel;
	// }

	public byte getRespirationRateConfidence() {
		return respirationRateConfidence;
	}

	// public void setRespirationRateConfidence(byte respirationRateConfidence) {
	// this.respirationRateConfidence = respirationRateConfidence;
	// }

	public byte getHeartRateConfidence() {
		return heartRateConfidence;
	}

	// public void setHeartRateConfidence(byte heartRateConfidence) {
	// this.heartRateConfidence = heartRateConfidence;
	// }

	public int getHeartRateVariability() {
		return heartRateVariability;
	}

	// public void setHeartRateVariability(int heartRateVariability) {
	// this.heartRateVariability = heartRateVariability;
	// }

	public byte getSystemConfidence() {
		return systemConfidence;
	}

	// public void setSystemConfidence(byte systemConfidence) {
	// this.systemConfidence = systemConfidence;
	// }

	// public int getStatusInfo() {
	// return statusInfo;
	// }

	// public void setStatusInfo(int statusInfo) {
	// this.statusInfo = statusInfo;
	// }

	public double getCoreTemp() {
		return coreTemp;
	}

	// public void setCoreTemp(double coreTemp) {
	// this.coreTemp = coreTemp;
	// }
}
