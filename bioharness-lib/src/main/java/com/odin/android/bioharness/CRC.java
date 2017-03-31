package com.odin.android.bioharness;

/**
 * An 8-bit CRC is used and has a polynomial of 0x8C (CRC-8), with the accumulator being initialised to zero before the
 * CRC calculation begins.
 * 
 * @see section 4.1.5 in 'BioHarness Bluetooth Comms Link Specification' pdf.
 * 
 * @author Alice Wang
 * @author Thiranjith Weerasinghe
 */
public class CRC {

	/** Zephyr packet constants */
	private final static int CHECKSUM_POLYNOMIAL = 0x8C;

	public int pushBlock(int currentCRC, int block, int count, byte[] packet) {
		int crc = currentCRC;

		for (; count > 0; --count, block++) {
			crc = checksumPushByte(crc, (packet[block] & 0xff) /* readUnsignedByte */);
		}
		return crc;
	}

	private int checksumPushByte(int currentChecksum, int newByte) {
		currentChecksum = (currentChecksum ^ newByte);
		for (int bit = 0; bit < Byte.SIZE; bit++) {
			if ((currentChecksum & 1) == 1)
				currentChecksum = ((currentChecksum >> 1) ^ CHECKSUM_POLYNOMIAL);
			else
				currentChecksum = (currentChecksum >> 1);
		}
		return currentChecksum;
	}
}
