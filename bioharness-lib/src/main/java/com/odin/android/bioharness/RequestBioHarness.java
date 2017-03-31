package com.odin.android.bioharness;

/**
 * Convenience class to create messages to send to the BioHarness.
 * 
 * @author Alice Wang
 * @author Thiranjith Weerasinghe
 */
public class RequestBioHarness {
	private static final int DATA_INDEX = 3;
	/**
	 * The STX field (Start of Text) is a standard ASCII control character (0x02) and denotes the start of the message.
	 * Although the protocol does not guarantee this value will not appear again within a message, it gives some
	 * delimiting to the message and therefore a start character to search for when receiving data.
	 */
	private static final byte STX = 0x02;
	/**
	 * The ETX field (End of Text) is a standard ASCII control character (0x03) and denotes the end of the message. Note
	 * that although this is the last field within the request message, the response message has no ETX value, instead
	 * using an ACK/NAK in its place.
	 */
	private static final byte ETX = 0x03;

	private static final byte BYTE_ZERO = 0x00;
	private static final byte BYTE_ONE = 0x01;
	private static final byte BYTE_TWO = 0x02;

	private static final CRC CRC = new CRC();

	/**
	 * Generates a message to set BT link configuration with the BioHarness
	 * 
	 * sets both link timeout and lifesign period to 0 as we don't want to send any lifesign messages.
	 * 
	 * @see section 5.25 in BioHarness BT Comms Link specification
	 */
	public byte[] requestBTLinkConfig() {
		byte[] data = new byte[9];

		// TODO msg id does not work as 0xa4 (169) is a negative number when converted to a byte
		data[0] = STX;
		data[1] = (byte) (0xa4); // msg id
		data[2] = 4; // dlc
		data[3] = BYTE_ZERO;
		data[4] = BYTE_ZERO;
		data[5] = BYTE_ZERO;
		data[6] = BYTE_ZERO;
		data[7] = (byte) CRC.pushBlock(0, DATA_INDEX, 4, data);
		data[8] = ETX;

		return data;
	}

	/**
	 * Generates the message that sets the general data packet transmit state (enable/disable state).
	 * 
	 * @param isEnabled
	 *            true if request data, false to stop data transmission
	 * @return byte string of request message
	 * @see section 5.15 in BioHarness BT Comms Link specification
	 */
	public byte[] setupGeneralData(boolean isEnabled) {

		// setup hr packets
		byte[] data = new byte[6];
		data[0] = STX;
		data[1] = 0x14; // msg ID
		data[2] = BYTE_ONE; // dlc
		data[3] = isEnabled ? BYTE_ONE : BYTE_ZERO; // turn on data stream every second
		data[4] = (byte) CRC.pushBlock(0, DATA_INDEX, 1, data);
		data[5] = ETX;

		return data;
	}

	/**
	 * Generates the message that sets the summary data packet transmit state (enable/disable state).
	 * 
	 * @param transmissionRate
	 *            the transmission rate in seconds. If 0, then disabled.
	 * @return byte string of request message
	 * @see section 5.15 in BioHarness BT Comms Link specification
	 */
	public byte[] setupSummaryData(short transmissionRate) {

		// setup hr packets
		byte[] data = new byte[7];
		data[0] = STX;
		data[1] = (byte) 0xBD; // msg ID
		data[2] = BYTE_TWO; // dlc
		data[3] = (byte) (transmissionRate & 0xFF);
		data[4] = (byte) ((transmissionRate >> 8) & 0xFF);
		data[5] = (byte) CRC.pushBlock(0, DATA_INDEX, 2, data);
		data[6] = ETX;

		return data;
	}

	/**
	 * Generates the message that sets the ECG waveform packet transmit state (enable/disable state).
	 * 
	 * @param isEnabled
	 *            true if request data, false to stop data transmission
	 * @return byte string of request message
	 * @see section 5.17 in BioHarness BT Comms Link specification
	 */
	public byte[] setupECGWaveformData(boolean isEnabled) {

		byte[] data = new byte[6];
		data[0] = STX;
		data[1] = 0X16; // msg ID
		data[2] = BYTE_ONE; // dlc
		data[3] = isEnabled ? BYTE_ONE : BYTE_ZERO; // turn on data stream every second
		data[4] = (byte) CRC.pushBlock(0, DATA_INDEX, 1, data);
		data[5] = ETX;

		return data;
	}

}
