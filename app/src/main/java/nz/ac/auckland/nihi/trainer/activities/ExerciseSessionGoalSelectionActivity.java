package nz.ac.auckland.nihi.trainer.activities;

import nz.ac.auckland.nihi.trainer.R.id;
import nz.ac.auckland.nihi.trainer.R.layout;
import nz.ac.auckland.nihi.trainer.data.session.DistanceGoal;
import nz.ac.auckland.nihi.trainer.data.session.DurationGoal;
import nz.ac.auckland.nihi.trainer.data.session.ExerciseSessionGoal;
import nz.ac.auckland.nihi.trainer.data.session.FreeWorkoutGoal;
import nz.ac.auckland.nihi.trainer.data.session.RouteGoal;
import nz.ac.auckland.nihi.trainer.data.session.TrainingLoadGoal;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.ToggleButton;

public class ExerciseSessionGoalSelectionActivity extends Activity {

	public static final String EXTRA_GOAL = ExerciseSessionGoalSelectionActivity.class.getName() + ".Goal";
	// public static final String EXTRA_IS_FREE_WORKOUT = ExerciseSessionGoalSelectionActivity.class.getName()
	// + ".IsFreeWorkout";
	// public static final String EXTRA_ROUTE = ExerciseSessionGoalSelectionActivity.class.getName() + ".IsRoute";

	private static final int REQUEST_SELECT_ROUTE = 1236543;

	// Contains a list of human-readable representations of all valid distances.
	private static final String[] DISTANCE_PICKER_LABELS = new String[201];

	private static final String[] IMPULSE_PICKER_LABELS = new String[71];

	// Populates the labels arrays for distance and impulse.
	static {

		// Distance
		int count = 0;
		for (int i = 0; i < 20; i++) {
			for (int j = 0; j < 10; j++) {
				DISTANCE_PICKER_LABELS[count] = i + "." + j;
				count++;
			}
		}
		DISTANCE_PICKER_LABELS[200] = "20.0";

		// Impulse
		for (int i = 0; i <= 20; i++) {
			IMPULSE_PICKER_LABELS[i] = Integer.toString(i * 5);
		}
		for (int i = 21; i <= 30; i++) {
			IMPULSE_PICKER_LABELS[i] = Integer.toString(100 + ((i - 20) * 10));
		}
		for (int i = 31; i <= 70; i++) {
			IMPULSE_PICKER_LABELS[i] = Integer.toString(200 + ((i - 30) * 20));
		}
	}

	// Main toggle buttons
	private ToggleButton btnFreeWorkout, btnDurationGoal, btnDistanceGoal, /* btnIntensityGoal, */btnImpulseGoal,
			btnRouteGoal, btnFollowRoute;

	// Panels
	private View pnlDurationGoal, pnlDistanceGoal, /* pnlIntensityGoal, */pnlImpulseGoal;

	// Panel "OK" buttons
	private View btnDurationOk, btnDistanceOk, /* btnIntensityOk, */btnImpulseOk;

	// Value selectors
	private NumberPicker pckDurationHours, pckDurationMinutes, pckDistance, pckImpulse;

	// Impulse textbox
	// private EditText txtImpulse;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setResult(RESULT_CANCELED);

		setContentView(layout.goal_picker);

		// txtImpulse = (EditText) findViewById(id.txtTrainingImpulse);

		pnlDurationGoal = findViewById(id.pnlDurationGoal);
		pnlDistanceGoal = findViewById(id.pnlDistanceGoal);
		// pnlIntensityGoal = findViewById(id.pnlIntensityGoal);
		pnlImpulseGoal = findViewById(id.pnlTrainingImpulseGoal);

		btnFreeWorkout = (ToggleButton) findViewById(id.btnFreeWorkout);
		btnFreeWorkout.setOnCheckedChangeListener(mainButtonListener);
		btnDurationGoal = (ToggleButton) findViewById(id.btnDurationGoal);
		btnDurationGoal.setOnCheckedChangeListener(mainButtonListener);
		btnDistanceGoal = (ToggleButton) findViewById(id.btnDistanceGoal);
		btnDistanceGoal.setOnCheckedChangeListener(mainButtonListener);
		// btnIntensityGoal = (ToggleButton) findViewById(id.btnIntensityGoal);
		// btnIntensityGoal.setOnCheckedChangeListener(mainButtonListener);
		btnImpulseGoal = (ToggleButton) findViewById(id.btnTrainingImpulseGoal);
		btnImpulseGoal.setOnCheckedChangeListener(mainButtonListener);
		btnRouteGoal = (ToggleButton) findViewById(id.btnFollowRoute);
		btnRouteGoal.setOnCheckedChangeListener(mainButtonListener);

		btnDurationOk = findViewById(id.btnDurationOk);
		btnDurationOk.setOnClickListener(durationOkClickedListener);
		btnDistanceOk = findViewById(id.btnDistanceOk);
		btnDistanceOk.setOnClickListener(distanceOkClickedListener);
		// btnIntensityOk = findViewById(id.btnIntensityOk);
		// btnIntensityOk.setOnClickListener(intensityOkClickedListener);
		btnImpulseOk = findViewById(id.btnImpulseOk);
		btnImpulseOk.setOnClickListener(impulseOkClickedListener);

		btnFollowRoute = (ToggleButton) findViewById(id.btnFollowRoute);
		btnFollowRoute.setOnClickListener(btnRouteClickedListener);

		pckDurationHours = (NumberPicker) findViewById(id.pckDurationHours);
		pckDurationMinutes = (NumberPicker) findViewById(id.pckDurationMinutes);
		pckDistance = (NumberPicker) findViewById(id.pckDistance);
		// pckIntensity = (NumberPicker) findViewById(id.pckIntensity);
		// pckEnergy = (NumberPicker) findViewById(id.pckEnergy);
		pckImpulse = (NumberPicker) findViewById(id.pckTrainingImpulse);

		pckDistance.setDisplayedValues(DISTANCE_PICKER_LABELS);
		pckImpulse.setDisplayedValues(IMPULSE_PICKER_LABELS);

		// Disable keyboard input for number pickers
		pckDurationHours.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
		pckDurationMinutes.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
		pckDistance.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
		pckImpulse.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
		// pckIntensity.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
		// pckEnergy.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

		// Enable the close buttons
		findViewById(id.btnClose).setOnClickListener(closeListener);

		// TODO These probably shouldn't just close the activity, rather, they
		// should revert to the previous state. But whatevs.
		findViewById(id.btnDurationCancel).setOnClickListener(closeListener);
		findViewById(id.btnDistanceCancel).setOnClickListener(closeListener);
		// findViewById(id.btnIntensityCancel).setOnClickListener(closeListener);
		findViewById(id.btnImpulseCancel).setOnClickListener(closeListener);

		// Allow number pickers to scroll properly, given that they're inside a ScrollView.
		enableNumberPickerScrollFix();

	}

	/**
	 * Allows all the number picker controls to scroll properly, given that they're inside a ScrollView.
	 */
	private void enableNumberPickerScrollFix() {
		ScrollView scroller = (ScrollView) findViewById(id.scroller);
		scroller.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				pckDurationHours.getParent().requestDisallowInterceptTouchEvent(false);
				pckDurationMinutes.getParent().requestDisallowInterceptTouchEvent(false);
				pckDistance.getParent().requestDisallowInterceptTouchEvent(false);
				pckImpulse.getParent().requestDisallowInterceptTouchEvent(false);
				// pckIntensity.getParent().requestDisallowInterceptTouchEvent(false);
				// pckEnergy.getParent().requestDisallowInterceptTouchEvent(false);
				return false;
			}
		});

		View.OnTouchListener childTouchListener = new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				v.getParent().requestDisallowInterceptTouchEvent(true);
				return false;
			}
		};
		pckDurationHours.setOnTouchListener(childTouchListener);
		pckDurationMinutes.setOnTouchListener(childTouchListener);
		pckDistance.setOnTouchListener(childTouchListener);
		pckImpulse.setOnTouchListener(childTouchListener);
		// pckIntensity.setOnTouchListener(childTouchListener);
		// pckEnergy.setOnTouchListener(childTouchListener);
	}

	/**
	 * Called when we return from the route selection activity. If a route was chosen, finish with the corresponding
	 * goal.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_SELECT_ROUTE && resultCode == RESULT_OK) {
			String routeId = data.getStringExtra(RoutesActivity.EXTRA_ROUTE_ID);
			finishWith(new RouteGoal(routeId));
		}
	}

	/**
	 * A handler that will finish the activity with a Duration goal.
	 */
	private final View.OnClickListener durationOkClickedListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {

			long millis = (pckDurationHours.getValue() * 60 + pckDurationMinutes.getValue()) * 60000;
			finishWith(new DurationGoal(millis));
		}
	};

	/**
	 * A handler that will finish the activity with a Distance goal.
	 */
	private final View.OnClickListener distanceOkClickedListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			int pickerValue = pckDistance.getValue();
			int meters = pickerValue * 100;
			finishWith(new DistanceGoal(meters));
		}
	};

	// /**
	// * A handler that will finish the activity with an Intensity goal.
	// */
	// private final View.OnClickListener intensityOkClickedListener = new View.OnClickListener() {
	// @Override
	// public void onClick(View v) {
	// float percentHRR = pckIntensity.getValue();
	// finishWith(new IntensityGoal(percentHRR));
	// }
	// };

	/**
	 * A handler that will finish the activity with an Impulse goal.
	 */
	private final View.OnClickListener impulseOkClickedListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			try {

				int val = pckImpulse.getValue();
				String label = IMPULSE_PICKER_LABELS[val];
				int load = Integer.parseInt(label);

				finishWith(new TrainingLoadGoal(load));
				// finishWith(new TrainingLoadGoal(Double.parseDouble(txtImpulse.getText().toString())));
			} catch (NumberFormatException e) {

			}
		}
	};

	/**
	 * When the route button is clicked, start the routes activity in selection mode to choose a route.
	 */
	private final View.OnClickListener btnRouteClickedListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			Intent intent = new Intent(ExerciseSessionGoalSelectionActivity.this, RoutesActivity.class);
			intent.putExtra(RoutesActivity.EXTRA_SELECTION_MODE, true);
			startActivityForResult(intent, REQUEST_SELECT_ROUTE);
		}
	};

	/**
	 * Finishes the activity with the given goal.
	 * 
	 * @param goal
	 */
	private void finishWith(ExerciseSessionGoal goal) {
		Intent data = new Intent();
		data.putExtra(EXTRA_GOAL, goal);
		setResult(RESULT_OK, data);
		finish();
	}

	/**
	 * Closes the activity with no result when certain buttons are pressed
	 */
	private final View.OnClickListener closeListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			finish();
		}
	};

	/**
	 * Handles click events on the main buttons.
	 * 
	 * Causes all other main buttons to become deselected, and shows the appropriate sub-panel if one exists for the
	 * given button. Finally, finishes the activity with the "free workout" option enabled if the "free workout" button
	 * was clicked.
	 */
	private final CompoundButton.OnCheckedChangeListener mainButtonListener = new CompoundButton.OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			if (isChecked) {

				// If this was "free workout", then finish appropriately.
				if (buttonView == btnFreeWorkout) {
					finishWith(new FreeWorkoutGoal());
				}

				else {

					deselectOthers((ToggleButton) buttonView);
					showAppropriatePanel((ToggleButton) buttonView);

					// TODO If this was "select a route", fire up the route selection logic.
					if (buttonView == btnRouteGoal) {

					}
				}
			}
		}
	};

	/**
	 * Sets the checked status of the given button to true, and of all others to false.
	 * 
	 * @param button
	 */
	private void deselectOthers(ToggleButton button) {
		btnFreeWorkout.setChecked(button == btnFreeWorkout);
		btnDurationGoal.setChecked(button == btnDurationGoal);
		btnDistanceGoal.setChecked(button == btnDistanceGoal);
		// btnIntensityGoal.setChecked(button == btnIntensityGoal);
		btnImpulseGoal.setChecked(button == btnImpulseGoal);
		btnRouteGoal.setChecked(button == btnRouteGoal);
	}

	/**
	 * Shows the panel corresponding with the given button. Hides all others.
	 * 
	 * @param button
	 */
	private void showAppropriatePanel(ToggleButton button) {
		View panel = null;
		if (button == btnDurationGoal)
			panel = pnlDurationGoal;
		else if (button == btnDistanceGoal)
			panel = pnlDistanceGoal;
		// else if (button == btnIntensityGoal)
		// panel = pnlIntensityGoal;
		else if (button == btnImpulseGoal)
			panel = pnlImpulseGoal;

		pnlDurationGoal.setVisibility(panel == pnlDurationGoal ? View.VISIBLE : View.GONE);
		pnlDistanceGoal.setVisibility(panel == pnlDistanceGoal ? View.VISIBLE : View.GONE);
		// pnlIntensityGoal.setVisibility(panel == pnlIntensityGoal ? View.VISIBLE : View.GONE);
		pnlImpulseGoal.setVisibility(panel == pnlImpulseGoal ? View.VISIBLE : View.GONE);
	}

}
