package nz.ac.auckland.nihi.trainer.data;

import java.util.Collections;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.log4j.Logger;

public class BloodLactateConcentrationData {

	/**
	 * There must be at least this many data points to use the "personalized" function. Otherwise a default function
	 * will be used.
	 */
	private static final int DATA_POINT_THRESHOLD = 2;

	public static BloodLactateConcentrationData fromString(String string) {
		BloodLactateConcentrationData obj = new BloodLactateConcentrationData();
		if (!"".equals(string)) {
			String[] rawData = string.split(",");
			for (int i = 0; i < rawData.length; i += 2) {
				float propHRR = Float.parseFloat(rawData[i]);
				float bloodLactateConcentration = Float.parseFloat(rawData[i + 1]);
				obj.add(propHRR, bloodLactateConcentration);
			}
		}
		return obj;
	}

	/**
	 * Stores all the data points
	 */
	private final SortedMap<Double, Double> dataPoints = new TreeMap<Double, Double>();

	/**
	 * Used to create the best-fit curve.
	 */
	private final SimpleRegression curveFitter = new SimpleRegression();
	// The estimated values, where y = Ae^(Kx)
	private double estimatedA, estimatedK;
	private boolean dirty = true;

	/**
	 * When this method is called, we will ensure that the best-fit curve is generated. Other methods in this class have
	 * the ability to set the "dirty" bit, which will cause this method to do its stuff. Otherwise this method clears
	 * the dirty bit and subsequent calls will do nothing.
	 */
	private void ensureBestFitGenerated() {
		if (dirty && willUsePersonalizedFunction()) {
			curveFitter.clear();

			// X axis: %HRR values.
			// Y axis: blood lactate concentration values.
			// For each pair, find w, where w = ln(y). Then find the best-fit line on data-points created by (x, w).
			for (double propHRR : dataPoints.keySet()) {
				double x = propHRR;
				double y = dataPoints.get(propHRR);
				double w = Math.log(y);
				curveFitter.addData(x, w);
			}

			// Now we have estimates for a and b, in w = lnA + bx.
			double lnA = curveFitter.getIntercept();
			double b = curveFitter.getSlope();

			// Finally, e^(lnA) is a good estimate for our estimatedA value, and b is a good estimate for estimatedK.
			estimatedA = Math.exp(lnA);
			estimatedK = b;

			dirty = false;
		}
	}

	public int numEntries() {
		return dataPoints.size();
	}

	public void add(double propHRR, double bloodLactateConcentration) {
		dataPoints.put(propHRR, bloodLactateConcentration);
		dirty = true;
	}

	public SortedMap<Double, Double> getDataPoints() {
		return Collections.unmodifiableSortedMap(dataPoints);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		Set<Entry<Double, Double>> entries = dataPoints.entrySet();
		@SuppressWarnings("unchecked")
		Entry<Double, Double>[] arrEntries = new Entry[entries.size()];
		arrEntries = entries.toArray(arrEntries);

		for (int i = 0; i < arrEntries.length; i++) {
			builder.append(arrEntries[i].getKey().toString());
			builder.append(",");
			builder.append(arrEntries[i].getValue().toString());
			if (i < arrEntries.length - 1) {
				builder.append(",");
			}
		}
		Logger.getLogger(BloodLactateConcentrationData.class).info("toString(): " + builder.toString());
		return builder.toString();
	}

	public boolean willUsePersonalizedFunction() {
		return dataPoints.size() >= DATA_POINT_THRESHOLD;
	}

	/**
	 * Estimates the exercise load for a given individual at a given Proportion of HRR. Uses "generalized" functions if
	 * there's not enough data in this object. Otherwise, uses the curve estimated by this object.
	 * 
	 * @return
	 */
	public double estimateLoad(Gender gender, double proportionHRR) {

		if (willUsePersonalizedFunction()) {
			ensureBestFitGenerated();

			return estimatedA * Math.exp(estimatedK * proportionHRR);

		} else if (gender == Gender.Male) {

			return 0.64 * Math.exp(1.92 * proportionHRR);

		} else {

			return 0.86 * Math.exp(1.67 * proportionHRR);

		}

	}

}
