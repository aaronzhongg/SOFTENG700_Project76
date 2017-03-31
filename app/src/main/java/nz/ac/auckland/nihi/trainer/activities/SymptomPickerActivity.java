package nz.ac.auckland.nihi.trainer.activities;

import java.util.List;

import nz.ac.auckland.cs.odin.android.api.services.testharness.TestHarnessService;
import nz.ac.auckland.cs.odin.android.api.services.testharness.TestHarnessUIControllerCommandReceiver;
import nz.ac.auckland.cs.odin.android.api.services.testharness.TestHarnessUtils;
import nz.ac.auckland.nihi.trainer.R.id;
import nz.ac.auckland.nihi.trainer.R.layout;
import nz.ac.auckland.nihi.trainer.data.Symptom;
import nz.ac.auckland.nihi.trainer.data.SymptomEntry;
import nz.ac.auckland.nihi.trainer.data.SymptomStrength;
import nz.ac.auckland.nihi.trainer.util.AndroidTextUtils;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

public class SymptomPickerActivity extends Activity {

	public static final String EXTRA_SYMPTOM = SymptomPickerActivity.class.getName() + ".Symptom";
	public static final String EXTRA_SYMPTOM_STRENGTH = SymptomPickerActivity.class.getName() + ".SymptomStrength";
	public static final String EXTRA_PRIOR_SYMPTOMS = SymptomPickerActivity.class.getName() + ".PriorSymptoms";

	// UI
	private Spinner spinnerSymptom, spinnerSeverity;

	/**
	 * Allows the test harness to interact with this activity.
	 */
	private TestHarnessUIControllerCommandReceiver testHarnessUIController;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setResult(RESULT_CANCELED);
		setContentView(layout.symptom_picker);

		spinnerSymptom = (Spinner) findViewById(id.spinnerSymptom);
		spinnerSymptom.setAdapter(new SymptomAdapter());
		spinnerSeverity = (Spinner) findViewById(id.spinnerSeverity);
		spinnerSeverity.setAdapter(new SymptomStrengthAdapter());

		ListView lstPriorSymptoms = (ListView) findViewById(id.lstPriorSymptoms);
		lstPriorSymptoms.setAdapter(new PriorSymptomAdapter((List<SymptomEntry>) getIntent().getSerializableExtra(
				EXTRA_PRIOR_SYMPTOMS)));

		findViewById(id.btnOk).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent extras = new Intent();
				extras.putExtra(EXTRA_SYMPTOM, (Symptom) spinnerSymptom.getSelectedItem());
				extras.putExtra(EXTRA_SYMPTOM_STRENGTH, (SymptomStrength) spinnerSeverity.getSelectedItem());
				setResult(RESULT_OK, extras);
				finish();
			}
		});

		findViewById(id.btnCancel).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});

		findViewById(id.btnClose).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});

		// If using the test harness, create the receiver. Don't hook it up just yet though.
		if (TestHarnessUtils.isTestHarness()) {
			testHarnessUIController = new TestHarnessUIControllerCommandReceiver(this);
		}

	}

	@Override
	protected void onResume() {
		super.onResume();
		if (TestHarnessUtils.isTestHarness()) {
			LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(testHarnessUIController,
					TestHarnessService.IF_PROCESS_COMMAND);
		}
	}

	@Override
	protected void onPause() {
		if (TestHarnessUtils.isTestHarness()) {
			LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(testHarnessUIController);
		}
		super.onPause();
	}

	private class PriorSymptomAdapter extends ArrayAdapter<SymptomEntry> {

		public PriorSymptomAdapter(List<SymptomEntry> entries) {
			super(SymptomPickerActivity.this, layout.symptom_picker_symptom_row);
			this.addAll(entries);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			View view = null;
			if (convertView == null) {
				LayoutInflater li = LayoutInflater.from(getContext());
				view = li.inflate(layout.symptom_picker_symptom_row, parent, false);
			} else {
				view = convertView;
			}

			SymptomEntry entry = (SymptomEntry) getItem(position);

			TextView txtSymptom = (TextView) view.findViewById(id.txtSymptom);
			txtSymptom.setText(AndroidTextUtils.getSymptomName(SymptomPickerActivity.this, entry.getSymptom()));

			TextView txtSymptomStrength = (TextView) view.findViewById(id.txtSymptomStrength);
			txtSymptomStrength.setText(AndroidTextUtils.getSymptomStrengthDescription(SymptomPickerActivity.this,
					entry.getStrength()));

			TextView txtTime = (TextView) view.findViewById(id.txtTime);
			txtTime.setText(AndroidTextUtils.getRelativeTimeStringSeconds(entry.getRelativeTimeInSeconds()));

			return view;
		}

	}

	private class SymptomAdapter extends ArrayAdapter<Symptom> {

		public SymptomAdapter() {
			super(SymptomPickerActivity.this, layout.simple_dropdown_item_1line);
			this.addAll(Symptom.values());
		}

		@Override
		public View getDropDownView(int position, View convertView, ViewGroup parent) {
			TextView view = (TextView) super.getDropDownView(position, convertView, parent);

			view.setText(AndroidTextUtils.getSymptomName(SymptomPickerActivity.this, getItem(position)));

			return view;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView view = (TextView) super.getView(position, convertView, parent);
			view.setText(AndroidTextUtils.getSymptomName(SymptomPickerActivity.this, getItem(position)));

			return view;
		}

	}

	private class SymptomStrengthAdapter extends ArrayAdapter<SymptomStrength> {

		public SymptomStrengthAdapter() {
			super(SymptomPickerActivity.this, layout.simple_dropdown_item_1line);
			SymptomStrength[] validStrengths = new SymptomStrength[SymptomStrength.values().length - 1];
			System.arraycopy(SymptomStrength.values(), 0, validStrengths, 0, validStrengths.length);
			this.addAll(validStrengths);
		}

		@Override
		public View getDropDownView(int position, View convertView, ViewGroup parent) {
			TextView view = (TextView) super.getDropDownView(position, convertView, parent);
			String desc = AndroidTextUtils.getSymptomStrengthDescription(getContext(), this.getItem(position));
			view.setText(desc);

			return view;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView view = (TextView) super.getView(position, convertView, parent);
			String desc = AndroidTextUtils.getSymptomStrengthDescription(getContext(), this.getItem(position));
			view.setText(desc);

			return view;
		}

	}

}
