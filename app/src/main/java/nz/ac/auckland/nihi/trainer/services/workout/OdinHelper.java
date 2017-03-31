package nz.ac.auckland.nihi.trainer.services.workout;

import java.util.Locale;

import nz.ac.auckland.cs.odin.android.api.services.IOdinService;
import nz.ac.auckland.cs.odin.data.JsonMessage;
import nz.ac.auckland.cs.odin.data.Message;
import nz.ac.auckland.cs.odin.interconnect.common.InterconnectException;
import nz.ac.auckland.nihi.trainer.data.ExerciseSummary;
import nz.ac.auckland.nihi.trainer.data.SymptomEntry;
import nz.ac.auckland.nihi.trainer.messaging.AnswerData;
import nz.ac.auckland.nihi.trainer.messaging.BeginSessionData;
import nz.ac.auckland.nihi.trainer.messaging.BioharnessData;
import nz.ac.auckland.nihi.trainer.messaging.EndSessionData;
import nz.ac.auckland.nihi.trainer.messaging.LocationData;
import nz.ac.auckland.nihi.trainer.messaging.MessageData;
import nz.ac.auckland.nihi.trainer.messaging.QuestionData;
import nz.ac.auckland.nihi.trainer.messaging.SymptomData;
import nz.ac.auckland.nihi.trainer.services.workout.ECGDataCache.ECGData;
import nz.ac.auckland.nihi.trainer.util.VitalSignUtils;

import org.apache.log4j.Logger;

import android.location.Location;

import com.odin.android.bioharness.data.SummaryData;

/**
 * Helper methods to send workout data to / receive workout data from Odin.
 * 
 * @author Andrew Meads
 * 
 */
public final class OdinHelper {

	private static final Logger logger = Logger.getLogger(OdinHelper.class);

	public static void sendLocationData(IOdinService odin, Location location, float speedKMPH, double distance) {
		LocationData data = new LocationData();
		data.setGeneratedTime(location.getTime());
		data.setReceivedTime(System.currentTimeMillis());
		data.setLatitude(location.getLatitude());
		data.setLongitude(location.getLongitude());
		data.setCumulativeDistance(distance);
		data.setHasSpeed(true/* location.hasSpeed() */);
		data.setInstantaneousSpeed(speedKMPH * (1000.0f / 3600.0f) /* location.getSpeed() */);
		data.setAccuracy(location.getAccuracy());
		JsonMessage message = new JsonMessage(data);
		try {
			odin.sendAsyncMessage(message);
		} catch (InterconnectException e) {
			logger.error("sendLocationData(): " + e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	public static void sendVitalSignData(IOdinService odin, SummaryData vitalSignData, int restingHeartRate,
			int maxHeartRate, double load) {
		int heartRate = vitalSignData.getHeartRate();
		if (true/* heartRate < 200 && heartRate >= 30 */) {
			try {
				BioharnessData toOdin = new BioharnessData();
				toOdin.setHeartRate(heartRate);
				toOdin.setBreathingRate(vitalSignData.getRespirationRate());
				toOdin.setSkinTemperature(vitalSignData.getSkinTemp());
				toOdin.setPostureAngle(vitalSignData.getPosture());
				toOdin.setPercentHRR(Math.round(VitalSignUtils.getPercentHRR(heartRate, restingHeartRate, maxHeartRate)));
				toOdin.setTimeStamp(System.currentTimeMillis());
				toOdin.setHeartRateReliable(vitalSignData.isHeartRateReliable());
				toOdin.setLoad(load);
				odin.sendAsyncMessage(new JsonMessage(toOdin));
				// logger.info("onReceiveSummaryData(): Sent heart rate to Odin");
			} catch (InterconnectException e) {
				logger.error("sendVitalSignData(): " + e.getMessage(), e);
				throw new RuntimeException(e);
			}
		}
	}

	public static void sendStartSessionPacket(IOdinService odin) {
		try {
			BeginSessionData bsData = new BeginSessionData();
			/* MessageHandle handle = */odin.sendAsyncMessage(new JsonMessage(bsData));
			/* Message response = */// handle.waitForResponse();
		} catch (InterconnectException e) {
			logger.error("sendStartSessionPacket(): " + e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	public static void sendEndSessionPacket(IOdinService odin, ExerciseSummary summary) {

		EndSessionData endOfSession = new EndSessionData();

		endOfSession.setValues(summary);

		// endOfSession.setAvgSpeed(summary.getAvgSpeed());
		// endOfSession.setMaxSpeed(summary.getMaxSpeed());
		// endOfSession.setMinSpeed(summary.getMinSpeed());
		//
		// endOfSession.setMinHeartRate(summary.getMinHeartRate());
		// endOfSession.setAvgHeartRate(summary.getAvgHeartRate());
		// endOfSession.setMaxHeartRate(summary.getMaxHeartRate());
		//
		// endOfSession.setDistanceInMetres(summary.getDistanceInMetres());
		// endOfSession.setDurationInSeconds(summary.getDurationInSeconds());
		// endOfSession.setTrainingLoad(summary.getTrainingLoad());

		// TODO Notifications and Symptoms

		try {

			odin.sendAsyncMessage(new JsonMessage(endOfSession));

		} catch (Exception e) {
			logger.error("sendEndSessionPacket(): " + e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	public static void sendSymptomData(IOdinService odin, SymptomEntry symptomEntry, /* long sessionStartTime, */
			ECGDataCache.ECGData ecgData) {
		SymptomData data = new SymptomData();
		data.setRelativeTimeMillis(symptomEntry.getRelativeTimeInSeconds() * 1000L);
		data.setSeverity(symptomEntry.getStrength().ordinal());
		data.setSymptom(symptomEntry.getSymptom().toString().toUpperCase(Locale.getDefault()));

		Object[] processedData = generateECGSampleArrays(ecgData);
		data.setEcgDataStartTimestamp(ecgData.getStartTimestamp()/* - sessionStartTime */);
		data.setEcgData((int[]) processedData[0]);
		data.setEcgDataTimestampOffsets((long[]) processedData[1]);

		JsonMessage message = new JsonMessage(data);

		try {
			odin.sendAsyncMessage(message);
		} catch (InterconnectException e) {
			logger.error("sendSymptomData(): " + e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	public static void sendAnswer(IOdinService odin, AnswerData answer) {

		JsonMessage message = new JsonMessage(answer);

		try {
			odin.sendAsyncMessage(message);
		} catch (InterconnectException e) {
			logger.error("sendAnswer(): " + e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	public static Object[] generateECGSampleArrays(ECGData data) {

		// Pull out a small sample of the total ECG data. TODO A more intelligent algorithm.
		final int DIV = 5;
		int[] samples = data.getSamples();
		int[] samplesToSend = new int[samples.length / DIV];
		for (int i = 0; i < samplesToSend.length; i++) {
			samplesToSend[i] = samples[i * DIV];
		}
		long[] timestampsToSend = new long[samplesToSend.length];
		for (int i = 0; i < timestampsToSend.length; i++) {
			timestampsToSend[i] = /* ecgData.getStartTimestamp() + */i * 4 * DIV;
		}

		return new Object[] { samplesToSend, timestampsToSend };

	}

	public static MessageData getMessageFromDoctor(Message odinMessage) {
		if (odinMessage instanceof JsonMessage) {
			if (((JsonMessage) odinMessage).getName().equals(MessageData.class.getName())) {

				try {
					return (MessageData) ((JsonMessage) odinMessage).deserialize(MessageData.class);
				} catch (Exception e) {
					logger.error("getMessageFromDoctor(): " + e.getMessage(), e);
					// return null;
					// throw new RuntimeException(e);
				}

			}
		}
		return null;
	}

	public static QuestionData getQuestionFromDoctor(Message odinMessage) {
		if (odinMessage instanceof JsonMessage) {
			if (((JsonMessage) odinMessage).getName().equals(QuestionData.class.getName())) {

				try {
					return (QuestionData) ((JsonMessage) odinMessage).deserialize(QuestionData.class);
				} catch (Exception e) {
					logger.error("getQuestionFromDoctor(): " + e.getMessage(), e);
					// return null;
					// throw new RuntimeException(e);
				}

			}
		}
		return null;
	}

}
