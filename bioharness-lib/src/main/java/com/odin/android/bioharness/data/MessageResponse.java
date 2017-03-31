package com.odin.android.bioharness.data;

import java.text.SimpleDateFormat;

import com.j256.ormlite.field.DatabaseField;
import com.odin.android.bioharness.BioHarness;

/**
 * Abstract class to represent different message types received from the BioHarness.
 * 
 * @author Alice Wang
 * @author Andrew Meads
 * @see BioHarness Bluetooth Comms Link Specification 20100602.pdf for message specific details.
 */
public abstract class MessageResponse {
	protected static final byte ACK = 0x06;
	protected static final byte NAK = 0x15;

	@DatabaseField(canBeNull = false)
	private byte mMessageId;

	// Primary key, for database entries. Must be specified in the class, for ORMLite.
	@DatabaseField(generatedId = true, canBeNull = false)
	private int id;

	static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS");

	// Required for ORMLite
	protected MessageResponse() {
	}

	MessageResponse(byte messageId) {
		mMessageId = messageId;
	}

	/**
	 * Get the message id as defined in the BioHarness BT specification document.
	 * 
	 * @see Section 8 (Appendix A) for an overview of message types.
	 */
	public final byte getId() {
		return mMessageId;
	}

	/**
	 * Combines the high and low bytes into a 16-bit word
	 * 
	 * @param low
	 * @param high
	 * @return
	 */
	final int merge(byte low, byte high) {
		return (high << 8 | (0xFF & low));
	}

	/**
	 * Gets the ID stored in the database, as a primary key.
	 * 
	 * @return
	 */
	public int getDB_Id() {
		return id;
	}

	/**
	 * Returns an array of size 8, with each boolean corresponding to one bit in the given byte. arr[0] is the LSB,
	 * while arr[7] is the MSB.
	 * 
	 * @param theByte
	 * @return
	 */
	protected static final boolean[] getBits(byte theByte) {
		boolean a = (theByte & 0x1) != 0;
		boolean b = (theByte & 0x2) != 0;
		boolean c = (theByte & 0x4) != 0;
		boolean d = (theByte & 0x8) != 0;
		boolean e = (theByte & 0x10) != 0;
		boolean f = (theByte & 0x20) != 0;
		boolean g = (theByte & 0x40) != 0;
		boolean h = (theByte & 0x80) != 0;

		return new boolean[] { a, b, c, d, e, f, g, h };
	}
}
