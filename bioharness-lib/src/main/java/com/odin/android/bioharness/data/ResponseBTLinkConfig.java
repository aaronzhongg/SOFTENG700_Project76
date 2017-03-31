package com.odin.android.bioharness.data;

/**
 * Represents the response message received after setting the timeout and lifesign period for the BioHarness Bluetooth
 * link.
 * 
 * @author Alice Wang
 * @author Thiranjith Weerasinghe
 * @see Section 5.25.2 (pg 39) in "BioHarness Bluetooth Comms Link Specification 20100602" pdf for the detailed message
 *      structure.
 */
public class ResponseBTLinkConfig extends AbstractResponseData {

	public static final byte MESSAGE_ID = (byte) 0xa4;

	public ResponseBTLinkConfig(byte[] buffer) {
		super(buffer, "Bluetooth");
	}
}
