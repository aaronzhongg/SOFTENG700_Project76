package com.odin.android.bioharness.data;

import org.apache.log4j.Logger;

/**
 * Abstract class representing generic response messages sent by the BioHarness
 * 
 * @author Thiranjith Weerasinghe
 */
public abstract class AbstractResponseData extends MessageResponse {

	private static final Logger logger = Logger.getLogger(AbstractResponseData.class);

	private byte stx;
	private byte dlc;
	private byte crc;
	private byte end;

	/**
	 * Creates a new {@link AbstractResponseData}
	 * 
	 * @param buffer
	 * @param responseDescription
	 */
	AbstractResponseData(byte[] buffer, String responseDescription) {
		super(buffer[1]);
		int bufferIndex = 0;

		try {
			stx = buffer[bufferIndex++];
			bufferIndex++; // message id
			dlc = buffer[bufferIndex++];	// expects to be 0
			crc = buffer[bufferIndex++];
			end = buffer[bufferIndex];		// either ACK or NAK
		} catch (Exception e) {
			end = NAK;
			logger.error("Failed to build " + responseDescription + " response", e);
		}
	}

	/**
	 * @return <code>true</code> if the Bluetooth link settings were set properly within the BioHarness.
	 */
	public final boolean getResponse() {
		return end == ACK;
	}

	public static long getMillisForDay(byte[] buffer, int startIdx) {
		long millis = 0L;
		millis = (buffer[startIdx] & 0xFF);
		millis |= (buffer[startIdx + 1] & 0xFF) << 8;
		millis |= (buffer[startIdx + 2] & 0xFF) << 16;
		millis |= (buffer[startIdx + 3] & 0xFF) << 24;
		return millis;
	}

}
