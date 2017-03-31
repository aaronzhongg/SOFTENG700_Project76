package nz.ac.auckland.nihi.trainer.views;

import java.text.DecimalFormat;

import nz.ac.auckland.nihi.trainer.R.id;
import nz.ac.auckland.nihi.trainer.data.ExerciseSummary;
import nz.ac.auckland.nihi.trainer.prefs.NihiPreferences;
import nz.ac.auckland.nihi.trainer.util.AndroidTextUtils;
import nz.ac.auckland.nihi.trainer.util.VitalSignUtils;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * A {@link View} that shows all stats for an {@link ExerciseSummary}, including duration, energy, distance, and min,
 * avg, and max heart rate, %HRR, and speed.
 * 
 * Must be overridden to provide the id for the layout to use.
 * 
 * @author Andrew Meads
 * 
 */
public abstract class AbstractExerciseSummaryStatsView extends FrameLayout {

	private static final int DEFAULT_VALUE = 0;

	private ExerciseSummary summary;
	// private int absoluteMaximumHR;

	private View root;
	private TextView txtDuration;
	private TextView txtDistance;
	private TextView txtCumulativeTI;
	private TextView txtMinSpeed;
	private TextView txtAvgSpeed;
	private TextView txtMaxSpeed;
	private TextView txtMinHR;
	private TextView txtAvgHR;
	private TextView txtMaxHR;
	private TextView txtMinHRPercent;
	private TextView txtAvgHRPercent;
	private TextView txtMaxHRPercent;

	/**
	 * Creates a new {@link AbstractExerciseSummaryStatsView}.
	 * 
	 * @param context
	 */
	public AbstractExerciseSummaryStatsView(Context context) {
		super(context);
		createUI(context);
	}

	/**
	 * Creates a new {@link AbstractExerciseSummaryStatsView}.
	 * 
	 * @param context
	 * @param attrs
	 */
	public AbstractExerciseSummaryStatsView(Context context, AttributeSet attrs) {
		super(context, attrs);
		createUI(context);
	}

	/**
	 * Creates a new {@link AbstractExerciseSummaryStatsView}.
	 * 
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	public AbstractExerciseSummaryStatsView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		createUI(context);
	}

	/**
	 * When overridden in derived classes, provides the id for the layout to use.
	 * 
	 * @return
	 */
	protected abstract int getLayoutId();

	/**
	 * Creates the UI and populates it with default values.
	 * 
	 * @param context
	 */
	private void createUI(Context context) {
		LayoutInflater li = LayoutInflater.from(context);
		root = li.inflate(getLayoutId(), this, false);
		this.addView(root, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

		txtDuration = (TextView) root.findViewById(id.txtDuration);
		txtDistance = (TextView) root.findViewById(id.txtDistance);
		txtCumulativeTI = (TextView) root.findViewById(id.txtCumulativeImpulse);
		txtMinSpeed = (TextView) root.findViewById(id.txtMinSpeed);
		txtAvgSpeed = (TextView) root.findViewById(id.txtAvgSpeed);
		txtMaxSpeed = (TextView) root.findViewById(id.txtMaxSpeed);
		txtMinHR = (TextView) root.findViewById(id.txtMinHR);
		txtAvgHR = (TextView) root.findViewById(id.txtAvgHR);
		txtMaxHR = (TextView) root.findViewById(id.txtMaxHR);
		txtMinHRPercent = (TextView) root.findViewById(id.txtMinHRPercent);
		txtAvgHRPercent = (TextView) root.findViewById(id.txtAvgHRPercent);
		txtMaxHRPercent = (TextView) root.findViewById(id.txtMaxHRPercent);

		refresh();
	}

	/**
	 * Refreshes the UI based on the values contained within the provided {@link ExerciseSummary}. Handles
	 * <code>null</code> values by filling all views with the default value.
	 */
	public final void refresh() {

		int restingHR = NihiPreferences.RestHeartRate.getIntValue(getContext(), -1);
		int maximalHR = NihiPreferences.MaxHeartRate.getIntValue(getContext(), -1);

		DecimalFormat formatter = new DecimalFormat("#.##");

		if (txtDuration != null) {
			int duration = summary == null ? DEFAULT_VALUE : summary.getDurationInSeconds();
			txtDuration.setText(AndroidTextUtils.getRelativeTimeStringSeconds(duration));
		}

		if (txtDistance != null) {
			int distance = summary == null ? DEFAULT_VALUE : summary.getDistanceInMetres();
			double distanceInKM = ((double) distance) / 1000.0;
			txtDistance.setText(formatter.format(distanceInKM));
		}

		if (txtCumulativeTI != null) {
			double impulse = summary == null ? DEFAULT_VALUE : summary.getTrainingLoad();
			txtCumulativeTI.setText(formatter.format(impulse));
		}

		if (txtMinSpeed != null) {
			float speed = summary == null ? DEFAULT_VALUE : summary.getMinSpeed();
			txtMinSpeed.setText(formatter.format(speed));
		}

		if (txtAvgSpeed != null) {
			float speed = summary == null ? DEFAULT_VALUE : summary.getAvgSpeed();
			txtAvgSpeed.setText(formatter.format(speed));
		}

		if (txtMaxSpeed != null) {
			float speed = summary == null ? DEFAULT_VALUE : summary.getMaxSpeed();
			txtMaxSpeed.setText(formatter.format(speed));
		}

		if (txtMinHR != null) {
			int hr = summary == null ? DEFAULT_VALUE : summary.getMinHeartRate();
			txtMinHR.setText(Integer.toString(hr));
		}

		if (txtAvgHR != null) {
			int hr = summary == null ? DEFAULT_VALUE : summary.getAvgHeartRate();
			txtAvgHR.setText(Integer.toString(hr));
		}

		if (txtMaxHR != null) {
			int hr = summary == null ? DEFAULT_VALUE : summary.getMaxHeartRate();
			txtMaxHR.setText(Integer.toString(hr));
		}

		if (txtMinHRPercent != null) {
			double percent = (summary == null) ? 0 : VitalSignUtils.getPercentHRR(summary.getMinHeartRate(), restingHR,
					maximalHR);
			txtMinHRPercent.setText(formatter.format(percent));
		}

		if (txtAvgHRPercent != null) {
			double percent = (summary == null) ? 0 : VitalSignUtils.getPercentHRR(summary.getAvgHeartRate(), restingHR,
					maximalHR);
			txtAvgHRPercent.setText(formatter.format(percent));
		}

		if (txtMaxHRPercent != null) {
			double percent = (summary == null) ? 0 : VitalSignUtils.getPercentHRR(summary.getMaxHeartRate(), restingHR,
					maximalHR);
			txtMaxHRPercent.setText(formatter.format(percent));
		}

	}

	/**
	 * Gets the {@link ExerciseSummary} that this displays.
	 * 
	 * @return
	 */
	public final ExerciseSummary getExerciseSummary() {
		return summary;
	}

	/**
	 * Sets the {@link ExerciseSummary} that this displays. Setting this causes the view to refresh itself.
	 * 
	 * @param summary
	 */
	public final void setExerciseSummary(ExerciseSummary summary) {
		this.summary = summary;
		refresh();
	}

	/**
	 * Gets the absolute max HR to use in %HRR calculations. If this is not set, 0%HRR will always be displayed.
	 * 
	 * @return
	 */
	// public int getAbsoluteMaximumHR() {
	// return absoluteMaximumHR;
	// }

	/**
	 * Sets the absolute max HR to use in %HRR calculations. If this is not set, 0%HRR will always be displayed. Setting
	 * this causes the view to refresh itself.
	 * 
	 * @return
	 */
	// public void setAbsoluteMaximumHR(int absoluteMaximumHR) {
	// this.absoluteMaximumHR = absoluteMaximumHR;
	// refresh();
	// }
}
