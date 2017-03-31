package com.odin.android.bioharness.data;

/**
 * Represents the response message received after requesting the BioHarness to enable/disable BT transmission of the ECG
 * waveform packet (periodic).
 * 
 * @author Alice Wang
 * @author Thiranjith Weerasinghe
 * @see Section 5.17.2 (pg 31) in "BioHarness Bluetooth Comms Link Specification 20100602" pdf for the detailed message
 *      structure.
 * @see ECGWaveformData for the data containing ECG waveform.
 */
public class ResponseSetupECGWaveformData extends AbstractResponseData {

	public static final byte MESSAGE_ID = (byte) 0x16;

	public ResponseSetupECGWaveformData(byte[] buffer) {
		super(buffer, "ECG");
	}
}
