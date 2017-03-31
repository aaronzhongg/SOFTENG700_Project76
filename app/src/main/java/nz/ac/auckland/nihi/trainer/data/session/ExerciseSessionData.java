package nz.ac.auckland.nihi.trainer.data.session;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import nz.ac.auckland.nihi.trainer.data.BloodLactateConcentrationData;
import nz.ac.auckland.nihi.trainer.data.ExerciseNotification;
import nz.ac.auckland.nihi.trainer.data.ExerciseSummary;
import nz.ac.auckland.nihi.trainer.data.Gender;
import nz.ac.auckland.nihi.trainer.data.Route;
import nz.ac.auckland.nihi.trainer.data.RouteCoordinate;
import nz.ac.auckland.nihi.trainer.data.Symptom;
import nz.ac.auckland.nihi.trainer.data.SymptomEntry;
import nz.ac.auckland.nihi.trainer.data.SymptomStrength;
import nz.ac.auckland.nihi.trainer.prefs.NihiPreferences;
import nz.ac.auckland.nihi.trainer.services.workout.ECGDataCache;
import nz.ac.auckland.nihi.trainer.util.VitalSignUtils;
import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.SystemClock;

import com.j256.ormlite.dao.Dao;
import com.odin.android.bioharness.data.ECGWaveformData;
import com.odin.android.bioharness.data.SummaryData;

public class ExerciseSessionData {

	private static final float MILLIS_PER_HOUR = 60 * 60 * 1000;

	/**
	 * Stores all listeners interested in receiving notifications regarding changes in the data held by this object.
	 */
	private final ArrayList<ExerciseSessionDataListener> dataListeners = new ArrayList<ExerciseSessionDataListener>();

	/**
	 * Stores all listeners interested in receiving notifications regarding the completion of a goal.
	 */
	private final ArrayList<ExerciseSessionGoalListener> goalListeners = new ArrayList<ExerciseSessionGoalListener>();

	/**
	 * Stores all heart rate data received by this object.
	 */
	// private final ArrayList<StoredHeartRateData> heartRateData = new ArrayList<StoredHeartRateData>();

	/**
	 * Stores all reliable heart rate data received by this object.
	 */
	// private final ArrayList<StoredHeartRateData> reliableHeartRateData = new
	// ArrayList<ExerciseSessionData.StoredHeartRateData>();

	/**
	 * Stores all location data received by this object.
	 */
	private final ArrayList<Location> gpsData = new ArrayList<Location>();

	/**
	 * The start time of this session. Basically this is a timestamp set at the time this object is created.
	 */
	private final long startTime;

	/**
	 * The number of milliseconds elapsed between the boot time of the device and the time this object was created.
	 */
	private final long startTimeRelativeToBoot;

	/**
	 * The user's blood lactate levels, to be used in Training Impulse calculations.
	 */
	private final BloodLactateConcentrationData bloodLactateData;

	private final Context context;

	private final ExerciseSessionGoal goal;

	private final ArrayList<SymptomEntry> symptoms = new ArrayList<SymptomEntry>();

	private final ArrayList<ExerciseNotification> notifications = new ArrayList<ExerciseNotification>();

	/**
	 * The cache of ECG data.
	 */
	private final ECGDataCache ecgCache = new ECGDataCache();

	private final boolean isMonitoring;

	private StoredHeartRateData lastHeartRateData = null;
	private boolean lastHeartRateUnreliableFlag = true;
	private int lastHeartRateConfidence = 0;
	private int numHeartRatesRecorded = 0;

	private StoredHeartRateData lastReliableHeartRateData = null;
	private StoredHeartRateData previousReliableHeartRateData = null;
	private int numReliableHeartRatesRecorded = 0;
	private long sumReliableHeartRatesRecorded = 0;
	private int minReliableHeartRate = Integer.MAX_VALUE;
	private int maxReliableHeartRate = 0;
	private int avgReliableHeartRate = 0;

	private Location lastGPSData = null;
	private int numLocationsRecorded = 0;
	// private Location lastGPSDataWithSpeed = null;
	private float currentSpeed;
	private boolean currentSpeedIsEstimate = true;
	private Location lastGPSDataForDistanceCalc = null;
	// Stores the number of Locations received that are "too close" to count as having moved, since the last one
	// received that was sufficiently far. Used to determine when we should zero the speed estimate.
	private int closeGPSCount = 0;
	private float distanceTravelled = 0;
	// private int numLocationsRecordedWithSpeed = 0;
	private float maxSpeed = 0;
	private float minSpeed = Float.MAX_VALUE;
	private float avgSpeed = 0;

	private double cumulativeTrainingLoad = 0;

	/**
	 * Creates a new {@link ExerciseSessionData} object set to Exercising mode.
	 * 
	 * @param context
	 *            a Context used to access properties.
	 * @param startLocation
	 *            the user's initial {@link Location}, or null if unknown.
	 * @param goal
	 *            the goal of this session, or null if no goal.
	 */
	public ExerciseSessionData(Context context, Location startLocation, ExerciseSessionGoal goal) {
		this(context, startLocation, goal, true);
	}

	/**
	 * Creates a new {@link ExerciseSessionData} object.
	 * 
	 * @param context
	 *            a Context used to access properties.
	 * @param startLocation
	 *            the user's initial {@link Location}, or null if unknown.
	 * @param goal
	 *            the goal of this session, or null if no goal.
	 * @param isMonitoring
	 *            true if actually monitoring, false if we're just using this object as a place to store sensor data.
	 */
	public ExerciseSessionData(Context context, Location startLocation, ExerciseSessionGoal goal, boolean isMonitoring) {
		this.startTime = System.currentTimeMillis();
		this.startTimeRelativeToBoot = SystemClock.elapsedRealtime();
		this.context = context;
		this.bloodLactateData = NihiPreferences.BloodLactateLevels.getValue(context,
				BloodLactateConcentrationData.class, new BloodLactateConcentrationData());

		this.goal = goal;

		this.isMonitoring = isMonitoring;

		// TODO NOT support null location... or maybe do? Who knows... I suppose this is ok...
		if (startLocation != null) {
			// gpsData.add(startLocation);
			// lastGPSData = startLocation;
			addGPSData(startLocation);
		}
	}

	public boolean isMonitoring() {
		return this.isMonitoring;
	}

	/**
	 * Caches ECG data, to be sent to Odin if a symptom is reported.
	 * 
	 * @param ecgData
	 */
	public void cacheECGData(ECGWaveformData ecgData) {
		this.ecgCache.cache(ecgData);
	}

	/**
	 * Gets cached ECG data.
	 * 
	 * @return
	 */
	public ECGDataCache.ECGData getCachedECGData() {
		return this.ecgCache.getCachedData();
	}

	public List<SymptomEntry> getSymptoms() {
		return Collections.unmodifiableList(this.symptoms);
	}

	public List<ExerciseNotification> getNotifications() {
		return Collections.unmodifiableList(this.notifications);
	}

	/**
	 * Adds the given listener to the list of listeners that will be notified when data held by this object changes.
	 */
	public void addDataListener(ExerciseSessionDataListener listener) {
		dataListeners.add(listener);
	}

	/**
	 * Removes the given listener from the list of listeners.
	 */
	public void removeDataListener(ExerciseSessionDataListener listener) {
		dataListeners.remove(listener);
	}

	/**
	 * Adds the given listener to the list of listeners that will be notified when the goal is reached
	 */
	public void addGoalListener(ExerciseSessionGoalListener listener) {
		goalListeners.add(listener);
	}

	/**
	 * Removes the given listener from the list of listeners that will be notified when the goal is reached
	 */
	public void removeGoalListener(ExerciseSessionGoalListener listener) {
		goalListeners.remove(listener);
	}

	/**
	 * Gets the time this object was created, expressed as the number of milliseconds elapsed since Jan 1, 1970.
	 * 
	 * @return
	 */
	public long getStartTimeInMillis() {
		return startTime;
	}

	/**
	 * Gets the elapsed time in milliseconds, betwen the creation of this object and the current time.
	 * 
	 * @return
	 */
	public long getElapsedTimeInMillis() {
		return SystemClock.elapsedRealtime() - this.startTimeRelativeToBoot;
	}

	/**
	 * Gets the heart rate, which is the last recorded heart rate.
	 */
	public int getHeartRate() {
		// if (heartRateData.size() == 0) {
		// return 0;
		// } else {
		// return heartRateData.get(heartRateData.size() - 1).heartRate;
		// }
		if (lastHeartRateData == null) {
			return 0;
		} else {
			return lastHeartRateData.heartRate;
		}
	}

	// private double getInstantaneousTrainingLoad(int endIndex) {
	//
	// if (reliableHeartRateData.size() <= endIndex) {
	// throw new RuntimeException();
	// }
	//
	// long epochEndMillis = reliableHeartRateData.get(endIndex).relativeTimestamp;
	// long epochStartMillis = 0;
	// if (endIndex > 0) {
	// epochStartMillis = reliableHeartRateData.get(endIndex - 1).relativeTimestamp;
	// }
	//
	// double percentHRR = getPercentHRR(reliableHeartRateData.get(endIndex).heartRate);
	//
	// // Calculate the length of the impulse
	// long epochLengthMillis = epochEndMillis - epochStartMillis;
	// double epochLengthMinutes = epochLengthMillis / 60000.0;
	//
	// // Get the current Proportion HRR
	// double proportionHRR = percentHRR / 100.0;
	//
	// // Calculate the training impulse.
	// return getTrainingLoad(epochLengthMinutes, proportionHRR);
	// }

	public double getCumulativeTrainingLoad() {

		// If we have not HR data we cannot calculate this.
		// if (reliableHeartRateData.size() == 0) {
		// return 0;
		// }
		//
		// double cumulativeTI = 0;
		//
		// for (int i = 0; i < reliableHeartRateData.size(); i++) {
		// cumulativeTI += getInstantaneousTrainingLoad(i);
		// }
		//
		// return cumulativeTI;

		return cumulativeTrainingLoad;

	}

	private double getInstantaneousTrainingLoad(StoredHeartRateData startHeartRate, StoredHeartRateData endHeartRate) {

		long epochEndMillis = endHeartRate.relativeTimestamp;
		long epochStartMillis = 0;
		if (startHeartRate != null) {
			epochStartMillis = startHeartRate.relativeTimestamp;
		}

		double percentHRR = getPercentHRR(endHeartRate.heartRate);

		// Calculate the length of the impulse
		long epochLengthMillis = epochEndMillis - epochStartMillis;
		double epochLengthMinutes = epochLengthMillis / 60000.0;

		// Get the current Proportion HRR
		double proportionHRR = percentHRR / 100.0;

		// Calculate the training impulse.
		return getTrainingLoad(epochLengthMinutes, proportionHRR);
	}

	/**
	 * Gets the training impulse for an epoch of a given length with a given Proportion of HRR.
	 * 
	 * @param epochLengthInMinutes
	 * @param epochProportionHRR
	 * @return
	 */
	private double getTrainingLoad(double epochLengthInMinutes, double epochProportionHRR) {
		return Math.max(
				0,
				epochLengthInMinutes
						* epochProportionHRR
						* this.bloodLactateData.estimateLoad(
								NihiPreferences.Gender.getEnumValue(context, Gender.class, Gender.Male),
								epochProportionHRR));
	}

	public boolean isLatestHeartRateReliable() {
		// if (heartRateData.size() == 0)
		// return false;
		//
		// return heartRateData.get(heartRateData.size() - 1).reliable;
		if (lastHeartRateData == null) {
			return false;
		} else {
			return lastHeartRateData.reliable;
		}
	}

	/**
	 * Gets the average heart rate for the session. Currently this is just calculated as the average of all recorded
	 * reliable heart rate values. In the future it may be more accurate to also take into account the elapsed time
	 * between when each of these readings was taken.
	 * 
	 * @return
	 */
	public int getAvgHeartRate() {
		// if (reliableHeartRateData.size() == 0) {
		// return 0;
		// } else {
		// int sum = 0;
		// for (StoredHeartRateData hr : reliableHeartRateData) {
		// sum += hr.heartRate;
		// }
		// return sum / reliableHeartRateData.size();
		// }
		return avgReliableHeartRate;
	}

	/**
	 * Gets the maximum recorded reliable heart rate value, or 0 if there are none.
	 * 
	 * @return
	 */
	public int getMaxHeartRate() {
		// if (reliableHeartRateData.size() == 0) {
		// return 0;
		// } else {
		// int max = 0;
		// for (StoredHeartRateData hr : reliableHeartRateData) {
		// max = Math.max(max, hr.heartRate);
		// }
		// return max;
		// }
		return maxReliableHeartRate;
	}

	/**
	 * Gets the minimum recorded reliable heart rate value, or 0 if there are none.
	 * 
	 * @return
	 */
	public int getMinHeartRate() {
		// if (reliableHeartRateData.size() == 0) {
		// return 0;
		// } else {
		// int min = Integer.MAX_VALUE;
		// for (StoredHeartRateData hr : reliableHeartRateData) {
		// min = Math.min(min, hr.heartRate);
		// }
		// return min;
		// }

		if (minReliableHeartRate == Integer.MAX_VALUE) {
			return 0;
		} else {
			return minReliableHeartRate;
		}
	}

	/**
	 * Gets the most recent reliable recorded heart rate, or 0 if there are none.
	 * 
	 * @return
	 */
	public int getMostRecentReliableHeartRate() {
		// if (reliableHeartRateData.size() > 0) {
		// return reliableHeartRateData.get(reliableHeartRateData.size() - 1).heartRate;
		// } else {
		// return 0;
		// }

		if (lastReliableHeartRateData == null) {
			return 0;
		} else {
			return lastReliableHeartRateData.heartRate;
		}
	}

	/**
	 * Gets the most recent reliable recorded Percentage HRR value, or 0 if there are none.
	 * 
	 * @return
	 */
	public float getMostRecentReliablePercentHRR() {
		return getPercentHRR(getMostRecentReliableHeartRate());
	}

	/**
	 * Gets the user's current percentage of heart rate reserve (%HRR). This can be calculated as: <br/>
	 * %HRR = 100 * ((HRcurrent - HRresting) / (HRmax - HRresting))
	 * 
	 * @return
	 */
	public float getPercentHRR() {
		return getPercentHRR(getHeartRate());
	}

	/**
	 * Gets the user's average percentage of heart rate reserve (%HRR). This can be calculated as: <br/>
	 * %HRR = 100 * ((HRaverage - HRresting) / (HRmax - HRresting))
	 * 
	 * @return
	 */
	public float getAvgPercentHRR() {
		return getPercentHRR(getAvgHeartRate());
	}

	/**
	 * Gets the percentage of heart rate reserve (%HRR) for the given heart rate. This can be calculated as: <br/>
	 * %HRR = 100 * ((heartRate - HRresting) / (HRmax - HRresting))
	 * 
	 * @return
	 */
	private float getPercentHRR(int heartRate) {

		int restingHeartRate = NihiPreferences.RestHeartRate.getIntValue(context, -1);
		int maxHeartRate = NihiPreferences.MaxHeartRate.getIntValue(context, -1);

		return VitalSignUtils.getPercentHRR(heartRate, restingHeartRate, maxHeartRate);
	}

	/**
	 * Gets the user's current location. This will be the initial given location, or the most recently stored location
	 * value.
	 * 
	 * @return
	 */
	public Location getLocation() {
		// if (gpsData.size() == 0) {
		// return null;
		// }
		// return gpsData.get(gpsData.size() - 1);
		return lastGPSData;
	}

	/**
	 * Gets the approximate distance in meters covered during the exercise session to date. This value is obtained by
	 * summing the distances between successive gps coordinates received.
	 * 
	 * @return
	 */
	public float getDistance() {
		// if (gpsData.size() <= 1) {
		// return 0;
		// }
		//
		// boolean updateLoc1 = true;
		// Location loc1 = null;
		//
		// float distance = 0;
		// for (int i = 0; i < gpsData.size() - 1; i++) {
		//
		// if (updateLoc1) {
		// loc1 = gpsData.get(i);
		// updateLoc1 = false;
		// }
		// Location loc2 = gpsData.get(i + 1);
		// float newDistance = loc1.distanceTo(loc2);
		//
		// // Ignore distance changes smaller than the accuracy of loc1.
		// float acc = loc1.hasAccuracy() ? loc1.getAccuracy() : 2;
		// if (newDistance >= acc) {
		// updateLoc1 = true;
		// distance += newDistance;
		// }
		// }
		// return distance;

		return distanceTravelled;
	}

	/**
	 * Gets the user's estimated average speed, in kilometers per hour, for this session to date. Currently this is made
	 * by averaging all valid speeds. This is not very accurate.
	 * 
	 * @return
	 */
	public float getAvgSpeed() {
		// if (gpsData.size() == 0) {
		// return 0;
		// }
		//
		// int validSpeeds = 0;
		// float totalSpeed = 0;
		//
		// for (Location loc : gpsData) {
		// if (loc.hasSpeed()) {
		// float speed = loc.getSpeed();
		// totalSpeed += speed;
		// validSpeeds++;
		// }
		// }
		//
		// if (validSpeeds == 0)
		// return 0;
		//
		// float avgSpeedMetersPerSecond = totalSpeed / (float) validSpeeds;
		// return avgSpeedMetersPerSecond * (3600.0f / 1000.0f);

		// float distanceInKm = getDistance() / 1000.0f;
		// float timeInHours = (float) getElapsedTimeInMillis() / MILLIS_PER_HOUR;
		//
		// return distanceInKm / timeInHours;

		return avgSpeed;
	}

	public float getMaxSpeed() {
		// if (gpsData.size() == 0) {
		// return 0;
		// }
		//
		// float maxSpeed = 0;
		//
		// for (Location loc : gpsData) {
		// float speed = loc.getSpeed();
		// if (speed != 0f) {
		// maxSpeed = Math.max(maxSpeed, speed);
		// }
		// }
		//
		// return maxSpeed * (3600.0f / 1000.0f);

		return maxSpeed;
	}

	public float getMinSpeed() {
		// if (gpsData.size() == 0) {
		// return 0;
		// }
		//
		// float minSpeed = Float.MAX_VALUE;
		//
		// for (Location loc : gpsData) {
		// float speed = loc.getSpeed();
		// if (speed != 0f) {
		// minSpeed = Math.min(minSpeed, speed);
		// }
		// }
		//
		// if (minSpeed == Float.MAX_VALUE) {
		// return 0;
		// } else {
		// return minSpeed * (3600.0f / 1000.0f);
		// }

		if (minSpeed == Float.MAX_VALUE) {
			return 0;
		} else {
			return minSpeed;
		}
	}

	/**
	 * Gets the user's estimated current speed, in kilometers per hour. This value is obtained directly from the GPS.
	 * 
	 * @return
	 */
	public float getCurrentSpeed() {
		// if (gpsData.size() == 0) {
		// return 0;
		// }
		//
		// else {
		// // float metersPerSecond = gpsData.get(gpsData.size() - 1).getSpeed();
		// // return metersPerSecond * (3600.0f / 1000.0f);
		// for (int i = gpsData.size() - 1; i >= 0; i--) {
		// Location loc = gpsData.get(i);
		// if (loc.hasSpeed()) {
		// return loc.getSpeed() * (3600.0f / 1000.0f);
		// }
		// }
		// return 0;
		// }

		// if (lastGPSDataWithSpeed == null) {
		// return 0;
		// } else {
		// return lastGPSDataWithSpeed.getSpeed() * (3600.0f / 1000.0f);
		// }
		return currentSpeed;

		// Location loc1 = gpsData.get(gpsData.size() - 2);
		// Location loc2 = gpsData.get(gpsData.size() - 1);
		//
		// // TODO Use a monotonic timekeeping method that won't get f0kd if the user changes the wall clock time in the
		// // middle of an exercise session.
		//
		// // long elapsedTimeNanos = loc2.getElapsedRealtimeNanos() - loc1.getElapsedRealtimeNanos();
		// long elapsedTimeMillis = loc2.getTime() - loc1.getTime(); // elapsedTimeNanos / 1000000;
		// float elapsedTimeHours = (float) elapsedTimeMillis / MILLIS_PER_HOUR;
		//
		// float distanceInKm = (loc1.distanceTo(loc2)) / 1000.0f;
		//
		// return distanceInKm / elapsedTimeHours;
	}

	/**
	 * Gets a value if the current speed value ({@link #getCurrentSpeed()}) is generated by the GPS or an estimation
	 * algorithm.
	 * 
	 * @return <code>true</code> if the current speed value is generated by an estimation algorithm, <code>false</code>
	 *         otherwise.
	 */
	public boolean isCurrentSpeedEstimate() {
		return this.currentSpeedIsEstimate;
	}

	/**
	 * Adds a new heart rate value and fires a change event.
	 * 
	 * @param heartRate
	 */
	public void addHeartRateData(SummaryData heartRate) {
		StoredHeartRateData shrd = new StoredHeartRateData();
		shrd.heartRate = heartRate.getHeartRate();
		shrd.reliable = heartRate.isHeartRateReliable();
		shrd.relativeTimestamp = getElapsedTimeInMillis();

		// heartRateData.add(shrd);
		// if (shrd.reliable) {
		// reliableHeartRateData.add(shrd);
		// }

		// Store new HR data
		lastHeartRateData = shrd;
		lastHeartRateConfidence = heartRate.getHeartRateConfidence();
		lastHeartRateUnreliableFlag = heartRate.getHRUnreliableFlag();
		numHeartRatesRecorded++;

		// If reliable, also store the reliable data and update min, max, avg heart rates along with training load.
		if (shrd.reliable) {
			previousReliableHeartRateData = lastReliableHeartRateData;
			lastReliableHeartRateData = shrd;

			sumReliableHeartRatesRecorded += shrd.heartRate;
			numReliableHeartRatesRecorded++;
			avgReliableHeartRate = (int) (sumReliableHeartRatesRecorded / numReliableHeartRatesRecorded);

			// avgReliableHeartRate = (avgReliableHeartRate * numReliableHeartRatesRecorded + shrd.heartRate)
			// / (numReliableHeartRatesRecorded + 1);
			// numReliableHeartRatesRecorded++;

			if (shrd.heartRate < minReliableHeartRate) {
				minReliableHeartRate = shrd.heartRate;
			}

			if (shrd.heartRate > maxReliableHeartRate) {
				maxReliableHeartRate = shrd.heartRate;
			}

			cumulativeTrainingLoad += getInstantaneousTrainingLoad(previousReliableHeartRateData,
					lastReliableHeartRateData);

		}

		fireChangeEvent();
		checkGoal();
	}

	/**
	 * Adds a new location value and fires a change event.
	 * 
	 * @param loc
	 */
	public void addGPSData(Location loc) {
		if (loc != null) {

			// save new location data
			lastGPSData = loc;
			numLocationsRecorded++;

			// Variables to store speed and distance estimates
			float speedEstimateMetersPerSecond = -1;
			float distanceEstimateMeters = -1;

			// Set initial GPS value for speed / distance estimates if required
			if (lastGPSDataForDistanceCalc == null) {
				lastGPSDataForDistanceCalc = loc;
			}

			// Otherwise, calculate speed and distance estimates based on just latitude / longitude
			else {
				// Estimate the distance, in meters, from the previous GPS value to this one
				float acc = lastGPSDataForDistanceCalc.hasAccuracy() ? lastGPSDataForDistanceCalc.getAccuracy() : 2;
				float newDistance = lastGPSDataForDistanceCalc.distanceTo(loc);

				// If the distance is large enough that the new GPS location appears outside the accuracy radius of the
				// previous value...
				if (newDistance >= acc) {

					// Store the distance estimate
					distanceEstimateMeters = newDistance;

					// Get and store the speed estimate
					long timeEstimateMillis = loc.getTime() - lastGPSDataForDistanceCalc.getTime();
					float timeEstimateSeconds = timeEstimateMillis / 1000.0f;
					speedEstimateMetersPerSecond = distanceEstimateMeters / timeEstimateSeconds;

					// Update variables
					closeGPSCount = 0;
					lastGPSDataForDistanceCalc = loc;
				}

				// Otherwise, keep track of the number of locations since the last one which was "far enough"
				else {
					closeGPSCount++;

					// If we've had too many of these, zero the speed estimate.
					if (closeGPSCount > 2) {
						speedEstimateMetersPerSecond = 0;
						closeGPSCount = 0;
					}
				}
			}

			// If we estimated a new distance, update the distance travelled
			if (distanceEstimateMeters >= 0) {
				distanceTravelled += distanceEstimateMeters;
			}

			// If the current GPS data has speed data, use that.
			if (loc.hasSpeed()) {
				currentSpeed = loc.getSpeed() * (3600.0f / 1000.0f);
				currentSpeedIsEstimate = false;
			}

			// Otherwise, use the estimate if we have it.
			else if (speedEstimateMetersPerSecond >= 0) {
				currentSpeed = speedEstimateMetersPerSecond * (3600.0f / 1000.0f);
				currentSpeedIsEstimate = true;
			}

			else {
				currentSpeed = 0;
				currentSpeedIsEstimate = true;
			}

			// Update the average speed - use a total distance over total time metric.
			if (this.getElapsedTimeInMillis() > 0) {
				float averageSpeedMetersPerMillisecond = distanceTravelled / this.getElapsedTimeInMillis();
				this.avgSpeed = averageSpeedMetersPerMillisecond * (3600.0f);// * 1000.0f / 1000.0f);
			} else {
				this.avgSpeed = 0;
			}

			// Add the GPS data to the saved GPS data list if its sufficiently different from previous ones.
			synchronized (gpsData) {
				if (gpsData.size() == 0 || distanceEstimateMeters >= 0) {
					gpsData.add(loc);
				}
			}

			fireChangeEvent();
			checkGoal();
		}
	}

	/**
	 * HACK: Called whenever the timer updates in the activity. Calling this will result in any goal listeners being
	 * notified of the goal having been achieved, if that goal is a now-complete duration goal.
	 */
	// public void checkDurationGoal() {
	// if (goal != null && goal instanceof DurationGoal) {
	// checkGoal();
	// }
	// }

	/**
	 * Gets the goal, if any.
	 * 
	 * @return
	 */
	public ExerciseSessionGoal getGoal() {
		return this.goal;
	}

	/**
	 * Checks if the goal has been reached. If so, fires the "goal reached" event.
	 */
	private void checkGoal() {
		if (isGoalReached()) {
			onGoalReached();
		}
	}

	/**
	 * Returns a value indicating whether the goal for this exercise session has been achieved.
	 * 
	 * @return
	 */
	public boolean isGoalReached() {
		if (goal == null) {
			return false;
		} else {
			return goal.isGoalReached(this);
		}
	}

	public SymptomEntry addSymptomEntry(Symptom symptom, SymptomStrength strength) {
		int relTime = (int) (getElapsedTimeInMillis() / 1000.0);
		SymptomEntry entry = new SymptomEntry(symptom, strength, relTime);
		symptoms.add(entry);
		return entry;
	}

	public ExerciseNotification addNotification(String notification) {
		int relTime = (int) (getElapsedTimeInMillis() / 1000.0);
		ExerciseNotification notificationEntry = new ExerciseNotification(notification, relTime);
		notifications.add(notificationEntry);
		return notificationEntry;
	}

	/**
	 * Generates a summary of this session and saves it to the database.
	 * 
	 * TODO This method REALLY shouldn't be in this class...
	 * 
	 * @return
	 * @throws SQLException
	 */
	public ExerciseSummary generateSummary(long userId, String creatorName, Dao<ExerciseSummary, String> summaryDao,
			Dao<Route, String> routeDao) throws SQLException {
		ExerciseSummary summary = new ExerciseSummary();

		summary.setDate(new Date(getStartTimeInMillis()));
		summary.setDurationInSeconds((int) Math.round((double) getElapsedTimeInMillis() / 1000.0));

		summary.setDistanceInMetres(Math.round(getDistance()));

		summary.setCumulativeImpulse(getCumulativeTrainingLoad());

		// If we followed a route, set the summary's route to that value.
		if (this.goal instanceof RouteGoal) {
			Route followedRoute = routeDao.queryForId(((RouteGoal) this.goal).getRouteId());
			// Route followedRoute = routeDao.queryBuilder().selectColumns("id").where()
			// .eq("id", ((RouteGoal) this.goal).getRouteId()).queryForFirst();
			summary.setFollowedRoute(followedRoute);
		}

		// Otherwise, if we travelled more than 30 meters, create a new route.
		else if (getDistance() > 30) {

			Route followedRoute = new Route();
			followedRoute.setName("unused");
			followedRoute.setCreatedTimestamp(System.currentTimeMillis());
			followedRoute.setCreatorName(creatorName);
			followedRoute.setUserId(userId);
			// followedRoute.setName(Long.toHexString(System.currentTimeMillis()));
			followedRoute.setFavorite(false);
			followedRoute.setThumbnailFileName(null); // We will populate this in the review screen / routes screen upon
														// browsing the route for the first time.

			routeDao.assignEmptyForeignCollection(followedRoute, "gpsCoordinates");

			// Save route to DB
			routeDao.create(followedRoute);

			// Now for the GPS coordinates
			synchronized (this.gpsData) {
				for (Location loc : this.gpsData) {
					RouteCoordinate coord = new RouteCoordinate(loc.getLatitude(), loc.getLongitude());
					coord.setRoute(followedRoute);
					followedRoute.getGpsCoordinates().add(coord);
				}
			}

			summary.setFollowedRoute(followedRoute);

		}

		// Otherwise, no route for this summary.
		else {
			summary.setFollowedRoute(null);
		}

		summary.setAvgHeartRate(getAvgHeartRate());
		summary.setMinHeartRate(getMinHeartRate());
		summary.setMaxHeartRate(getMaxHeartRate());

		summary.setAvgSpeed(getAvgSpeed());
		summary.setMinSpeed(getMinSpeed());
		summary.setMaxSpeed(getMaxSpeed());

		summary.setUserId(userId);

		summaryDao.assignEmptyForeignCollection(summary, "symptoms");
		summaryDao.assignEmptyForeignCollection(summary, "notifications");
		summaryDao.create(summary);

		for (SymptomEntry symptom : symptoms) {
			summary.getSymptoms().add(symptom);
		}

		for (ExerciseNotification notification : notifications) {
			summary.getNotifications().add(notification);
		}

		return summary;
	}

	private static class StoredHeartRateData {
		int heartRate;
		boolean reliable;
		long relativeTimestamp;
	}

	/**
	 * Notifies all listeners that this object's data has changed.
	 */
	private void fireChangeEvent() {
		uiHandler.obtainMessage(CHANGE_EVENT).sendToTarget();
	}

	/**
	 * Fires the "goal reached" event for all goal listeners.
	 */
	private void onGoalReached() {
		uiHandler.obtainMessage(GOAL_REACHED).sendToTarget();
	}

	// Basically extra data for debug purposes.

	public int getLatestHeartRateConfidence() {
		return lastHeartRateConfidence;
	}

	public boolean getLatestHeartRateUnreliableFlag() {
		return lastHeartRateUnreliableFlag;
	}

	private static final int CHANGE_EVENT = 0, GOAL_REACHED = 1;

	/**
	 * Makes sure listeners are notified on the UI thread.
	 */
	@SuppressLint("HandlerLeak")
	private final Handler uiHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case CHANGE_EVENT:
				for (ExerciseSessionDataListener l : dataListeners) {
					l.sessionDataChanged(ExerciseSessionData.this);
				}
				break;
			case GOAL_REACHED:
				// Creating new array list coz we want to remove ourselves from this list during the goal reached event.
				for (ExerciseSessionGoalListener l : new ArrayList<ExerciseSessionGoalListener>(goalListeners)) {
					l.goalReached(ExerciseSessionData.this);
				}
				break;
			}
		}
	};
}
