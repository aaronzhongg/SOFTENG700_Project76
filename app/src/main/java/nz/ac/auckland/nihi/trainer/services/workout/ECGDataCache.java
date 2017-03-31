package nz.ac.auckland.nihi.trainer.services.workout;

import org.apache.log4j.Logger;

import com.odin.android.bioharness.data.ECGWaveformData;

/**
 * Caches ECG data.
 * 
 * @author Andrew Meads
 * 
 */
public class ECGDataCache {

	private static final Logger logger = Logger.getLogger(ECGDataCache.class);

	public static final int CACHE_SIZE_MILLIS = 10000;
	public static final int SINGLE_SAMPLE_DURATION_MILLIS = 4;
	public static final int CACHE_SIZE = CACHE_SIZE_MILLIS / SINGLE_SAMPLE_DURATION_MILLIS;

	private final int[] sampleCache = new int[CACHE_SIZE];
	private int numCachedSamples = 0;
	private int nextIndex = 0;
	private long timestamp = -1, firstTimestamp = -1;
	private long totalNumSamples;

	public synchronized void cache(ECGWaveformData newData) {

		int[] newSamples = newData.getSamples();
		for (int newSampleIndex = 0; newSampleIndex < newSamples.length; newSampleIndex++) {

			sampleCache[nextIndex] = newSamples[newSampleIndex];
			nextIndex = (nextIndex + 1) % sampleCache.length;

		}

		numCachedSamples = Math.min(CACHE_SIZE, numCachedSamples + newSamples.length);
		totalNumSamples += newSamples.length;

		// Update timetamp
		if (firstTimestamp < 0) {
			firstTimestamp = newData.getDeviceCaptureTime().getTime();
		}

		timestamp = firstTimestamp + 4 * (totalNumSamples - numCachedSamples);

		logger.debug("cache(): Cached " + (newSamples.length * SINGLE_SAMPLE_DURATION_MILLIS)
				+ "ms of ECG data. Total cached is now " + (numCachedSamples * SINGLE_SAMPLE_DURATION_MILLIS) + "ms.");

	}

	public synchronized ECGData getCachedData() {

		int[] returnArray = new int[numCachedSamples];

		if (numCachedSamples < CACHE_SIZE) {
			System.arraycopy(sampleCache, 0, returnArray, 0, numCachedSamples);
		} else {
			System.arraycopy(sampleCache, nextIndex, returnArray, 0, numCachedSamples - nextIndex);

			if (numCachedSamples - nextIndex < CACHE_SIZE) {

				System.arraycopy(sampleCache, 0, returnArray, numCachedSamples - nextIndex, CACHE_SIZE
						- (numCachedSamples - nextIndex));

			}
		}

		return new ECGData(returnArray, returnArray.length * SINGLE_SAMPLE_DURATION_MILLIS, timestamp);

	}

	/**
	 * This is the object that will be returned when requesting ECG data from the cache. It will give us the entirety of
	 * data in the cache, along with the length of time that data represents.
	 * 
	 * @author Andrew Meads
	 * 
	 */
	public static class ECGData {

		private final int[] samples;
		private final int totalDurationMillis;
		private final long startTimestamp;

		private ECGData(int[] samples, int totalDurationMillis, long startTimestamp) {
			this.samples = samples;
			this.totalDurationMillis = totalDurationMillis;
			this.startTimestamp = startTimestamp;
		}

		public int[] getSamples() {
			return samples;
		}

		public int getTotalDurationMillis() {
			return totalDurationMillis;
		}

		public long getStartTimestamp() {
			return startTimestamp;
		}

	}
}
