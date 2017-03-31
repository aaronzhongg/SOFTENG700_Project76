package nz.ac.auckland.nihi.trainer.views;

import java.text.DecimalFormat;

import nz.ac.auckland.nihi.trainer.R.drawable;
import nz.ac.auckland.nihi.trainer.R.id;
import nz.ac.auckland.nihi.trainer.R.string;
import nz.ac.auckland.nihi.trainer.data.session.ExerciseSessionData;
import nz.ac.auckland.nihi.trainer.data.session.ExerciseSessionDataListener;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

public abstract class AbstractWorkoutStatTileView extends FrameLayout implements ExerciseSessionDataListener {

	// Child views
	private View root;
	private ImageView imgIcon;
	private TextView txtStatType;
	private TextView txtStatValue;

	private ExerciseSessionData exerciseData;

	/**
	 * The stat that's displayed to the user in this view.
	 */
	private ViewedStat viewedStat = ViewedStat.HeartRate;

	public AbstractWorkoutStatTileView(Context context) {
		super(context);
		createUI(context);
	}

	public AbstractWorkoutStatTileView(Context context, AttributeSet attrs) {
		super(context, attrs);
		createUI(context);
	}

	public AbstractWorkoutStatTileView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		createUI(context);
	}

	protected abstract int getLayoutId();

	/**
	 * Creates the UI and stores references to child controls.
	 * 
	 * @param context
	 */
	private void createUI(Context context) {

		setClickable(true);

		LayoutInflater li = LayoutInflater.from(context);
		root = li.inflate(getLayoutId(), this, false);
		this.addView(root, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

		imgIcon = (ImageView) root.findViewById(id.imgStatIcon);
		txtStatType = (TextView) root.findViewById(id.txtStatDescription);
		txtStatValue = (TextView) root.findViewById(id.txtStatValue);

		// Set initial values.
		refreshUI();
	}

	private void refreshUI() {
		int iconId = viewedStat.getIconId();
		String description = viewedStat.getShortName(getContext());
		String unit = viewedStat.getUnit(getContext());
		String value;
		boolean errorColor = false;
		if (exerciseData == null) {
			value = "-";
		} else {
			DecimalFormat formatter = new DecimalFormat("#.##");
			DecimalFormat preciceFormatter = new DecimalFormat("#.####");
			switch (viewedStat) {
			case HeartRate:

				// if (exerciseData.getHeartRate() >= 200 || exerciseData.getHeartRate() < 30) {
				// value = "(?)";
				// }

				// else /* if (exerciseData.isLatestHeartRateReliable()) */{
				value = "" + exerciseData.getHeartRate()/* + " (" */;
				// value += (exerciseData.getLatestHeartRateUnreliableFlag() ? "U" : "R") + ", ";
				// value += exerciseData.getLatestHeartRateConfidence() + "%)";
				// }

				if (!exerciseData.isLatestHeartRateReliable()) {
					value = "? " + value;
					errorColor = true;
				}

				break;
			case PercentHeartRate:
				value = formatter.format(exerciseData.getPercentHRR());
				break;
			case AvgHeartRate:
				value = "" + exerciseData.getAvgHeartRate();
				break;
			case AvgPercentHeartRate:
				value = formatter.format(exerciseData.getAvgPercentHRR());
				break;
			case Speed:
				value = formatter.format(exerciseData.getCurrentSpeed());
				if (exerciseData.isCurrentSpeedEstimate()) {
					value = "? " + value;
					errorColor = true;
				}
				break;
			case AvgSpeed:
				value = formatter.format(exerciseData.getAvgSpeed());
				break;
			case Distance:
				double distInKM = exerciseData.getDistance() / 1000.0;
				value = formatter.format(distInKM); // "" + (int) Math.round(exerciseData.getDistance());
				break;
			case TrainingLoad:
				value = preciceFormatter.format(exerciseData.getCumulativeTrainingLoad());
				break;
			// case CurrentTrainingImpulse:
			// value = preciceFormatter.format(exerciseData.getCurrentTrainingImpulse());
			// break;
			default:
				throw new RuntimeException("Unexpected ViewedStat type: " + viewedStat);
			}
		}

		// TODO Only update these two if different from the last time this method was called?
		imgIcon.setImageResource(iconId);
		txtStatType.setText(description);

		txtStatValue.setText(value + " " + unit);

		if (errorColor) {
			txtStatValue.setTextColor(Color.YELLOW);
		} else {
			txtStatValue.setTextColor(Color.WHITE);
		}
	}

	/**
	 * Whenever the exercise data is updated, refresh this control.
	 */
	@Override
	public void sessionDataChanged(ExerciseSessionData session) {
		refreshUI();
	}

	public ViewedStat getViewedStat() {
		return viewedStat;
	}

	public void setViewedStat(ViewedStat viewedStat) {

		if (viewedStat == null) {
			throw new IllegalArgumentException("viewedStat cannot be null");
		}

		this.viewedStat = viewedStat;
		refreshUI();
	}

	/**
	 * Gets the data that's displayed in this view.
	 * 
	 * @return
	 */
	public ExerciseSessionData getExerciseData() {
		return exerciseData;
	}

	/**
	 * Sets the data that's displayed in the view. The exact data displayed will also depend on the {@link ViewedStat}.
	 * This method also adds a listener to the given {@link ExerciseSessionData} so that this view is notified whenever
	 * it updates.
	 * 
	 * @param exerciseData
	 */
	public void setExerciseData(ExerciseSessionData exerciseData) {
		if (this.exerciseData != null) {
			this.exerciseData.removeDataListener(this);
		}

		this.exerciseData = exerciseData;

		if (this.exerciseData != null) {
			this.exerciseData.addDataListener(this);
		}

		refreshUI();
	}

	/**
	 * Enumerates all possible stats that could be viewed in this view.
	 * 
	 */
	public static enum ViewedStat {

		HeartRate(string.stats_descr_heartrate_short, string.stats_descr_heartrate_long, drawable.nihi_ic_review_hr,
				string.stats_unit_heartrate),

		PercentHeartRate(string.stats_descr_hrr_short, string.stats_descr_hrr_long, drawable.nihi_ic_review_hr,
				string.stats_unit_hrr),

		AvgHeartRate(string.stats_descr_avgheartrate_short, string.stats_descr_avgheartrate_long,
				drawable.nihi_ic_review_hr, string.stats_unit_heartrate),

		AvgPercentHeartRate(string.stats_descr_avghrr_short, string.stats_descr_avghrr_long,
				drawable.nihi_ic_review_hr, string.stats_unit_hrr),

		Speed(string.stats_descr_speed_short, string.stats_descr_speed_long, drawable.nihi_ic_review_speed,
				string.stats_unit_speed),

		AvgSpeed(string.stats_descr_avgspeed_short, string.stats_descr_avgspeed_long, drawable.nihi_ic_review_speed,
				string.stats_unit_speed),

		Distance(string.stats_descr_distance_short, string.stats_descr_distance_long, drawable.nihi_ic_review_distance,
				string.stats_unit_distance),

		TrainingLoad(string.stats_descr_load_short, string.stats_descr_load_long, drawable.nihi_ic_review_energy, -1);
		//
		// CurrentTrainingImpulse(string.descr_current_ti_short, string.descr_current_ti_long,
		// drawable.nihi_ic_review_energy, -1);

		private final int shortNameId, longNameId, iconId, unitId;

		private ViewedStat(int shortNameId, int longNameId, int iconId, int unitId) {
			this.shortNameId = shortNameId;
			this.longNameId = longNameId;
			this.iconId = iconId;
			this.unitId = unitId;
		}

		public String getShortName(Context context) {
			return context.getString(shortNameId);
		}

		public int getShortNameId() {
			return shortNameId;
		}

		public String getLongName(Context context) {
			return context.getString(longNameId);
		}

		public int getLongNameId() {
			return longNameId;
		}

		public int getIconId() {
			return iconId;
		}

		public String getUnit(Context context) {
			if (unitId < 0) {
				return "";
			} else {
				return context.getString(unitId);
			}
		}

		public int getUnitId() {
			return unitId;
		}

	}

}
