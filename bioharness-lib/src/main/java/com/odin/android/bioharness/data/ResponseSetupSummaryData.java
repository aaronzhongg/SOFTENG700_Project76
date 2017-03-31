package com.odin.android.bioharness.data;

/**
 * Represents the response message received after requesting the BioHarness to enable/disable BT transmission of the
 * general data packet (periodic).
 * 
 * @author Alice Wang
 * @author Thiranjith Weerasinghe
 * @see Section 5.15.2 (pg 29) in "BioHarness Bluetooth Comms Link Specification 20100602" pdf for the detailed message
 *      structure.
 * @see GeneralData for the structure of the general data packet
 */
public class ResponseSetupSummaryData extends AbstractResponseData {

	public static final byte MESSAGE_ID = (byte) 0xBD;

	public ResponseSetupSummaryData(byte[] buffer) {
		super(buffer, "summary data");
	}
}
