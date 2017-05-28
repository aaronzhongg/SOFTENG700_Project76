package nz.ac.auckland.nihi.trainer.fragments;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;

import nz.ac.auckland.cs.android.prefs.IPreferences;
import nz.ac.auckland.cs.odin.android.api.prefs.OdinPreferences;
import nz.ac.auckland.nihi.trainer.R.id;
import nz.ac.auckland.nihi.trainer.R.layout;
import nz.ac.auckland.nihi.trainer.R.string;
import nz.ac.auckland.nihi.trainer.data.BloodLactateConcentrationData;
import nz.ac.auckland.nihi.trainer.data.Gender;
import nz.ac.auckland.nihi.trainer.prefs.NihiPreferences;

import org.apache.log4j.Logger;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("NewApi")
public class NihiPreferencesFragment extends Fragment {

	private static final Logger logger = Logger.getLogger(NihiPreferencesFragment.class);

	private DatePicker pckDOB;
	private RadioButton rdoMale, rdoFemale;
	private EditText txtName, txtEmail, txtHeight, txtWeight, txtMaxHR, txtRestingHR;

	private TextView txtNoBloodLactate;

	private LinearLayout pnlBloodLactate, pnlBloodLactateHeaders;
	private ArrayList<LinearLayout> bloodLactateRows = new ArrayList<LinearLayout>();

	// private NumberPicker pckDOB;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View root = inflater.inflate(layout.fragment_nihi_settings, container, false);

		// Find important widgets
		pckDOB = (DatePicker) root.findViewById(id.pckDOB);
		rdoMale = (RadioButton) root.findViewById(id.rdoMale);
		rdoFemale = (RadioButton) root.findViewById(id.rdoFemale);
		txtName = (EditText) root.findViewById(id.txtName);
		txtEmail = (EditText) root.findViewById(id.txtEmail);
		txtHeight = (EditText) root.findViewById(id.txtHeight);
		txtWeight = (EditText) root.findViewById(id.txtWeight);
		txtMaxHR = (EditText) root.findViewById(id.txtMaxHR);
		txtRestingHR = (EditText) root.findViewById(id.txtRestingHR);
		txtNoBloodLactate = (TextView) root.findViewById(id.txtNoBloodLactate);
		pnlBloodLactate = (LinearLayout) root.findViewById(id.pnlBloodLactate);
		pnlBloodLactateHeaders = (LinearLayout) root.findViewById(id.pnlBloodLactateHeaders);

		// Set basic text values
		txtName.setText(NihiPreferences.Name.getStringValue(getActivity(), ""));
		txtEmail.setText(NihiPreferences.Email.getStringValue(getActivity(), ""));
		txtHeight.setText(NihiPreferences.Height.getStringValue(getActivity(), ""));
		txtWeight.setText(NihiPreferences.Weight.getStringValue(getActivity(), ""));
		txtMaxHR.setText(NihiPreferences.MaxHeartRate.getStringValue(getActivity(), ""));
		txtRestingHR.setText(NihiPreferences.RestHeartRate.getStringValue(getActivity(), ""));

		// Set values of the text fields that can't be edited
		EditText txtUsername = (EditText) root.findViewById(id.txtUsername);
		EditText txtPassword = (EditText) root.findViewById(id.txtPassword);
		txtUsername.setText(OdinPreferences.UserName.getStringValue(getActivity(), ""));
		txtPassword.setText(OdinPreferences.Password.getStringValue(getActivity(), ""));

		// Set the gender option buttons
		rdoMale.setChecked(NihiPreferences.Gender.getEnumValue(getActivity(), Gender.class, Gender.Male).equals(
				Gender.Male));
		rdoFemale.setChecked(!rdoMale.isChecked());

		// Set the date-of-birth date picker widget
		Date dob = NihiPreferences.DateOfBirth.getDateValue(getActivity(), null);
		if (dob != null) {
			pckDOB.getCalendarView().setDate(dob.getTime());
		}

		// Add rows for current blood lactate concentration data
		BloodLactateConcentrationData bloodLactateData = NihiPreferences.BloodLactateLevels.getValue(getActivity(),
				BloodLactateConcentrationData.class, null);
		if (bloodLactateData != null && bloodLactateData.numEntries() > 0) {
			DecimalFormat formatter = new DecimalFormat("#.##");
			txtNoBloodLactate.setVisibility(View.INVISIBLE);
			pnlBloodLactateHeaders.setVisibility(View.VISIBLE);

			LayoutInflater li = getActivity().getLayoutInflater();
			SortedMap<Double, Double> dataSet = bloodLactateData.getDataPoints();
			for (Double propHRR : dataSet.keySet()) {
				Double bloodLactateLevel = dataSet.get(propHRR);

				LinearLayout row = (LinearLayout) li.inflate(layout.fragment_nihi_settings_blood_lactate_row,
						pnlBloodLactate, false);

				((EditText) row.findViewById(id.txtBloodLactate)).setText(formatter.format(bloodLactateLevel));
				((EditText) row.findViewById(id.txtPropHRR)).setText(formatter.format(propHRR));

				row.findViewById(id.btnRemoveBloodLactate).setOnClickListener(btnRemoveBloodLactateListener);
				pnlBloodLactate.addView(row, pnlBloodLactate.getChildCount() - 1);
				bloodLactateRows.add(row);
			}
		}
		root.findViewById(id.btnAddBloodLactate).setOnClickListener(btnAddBloodLactateListener);

		// Button hookups
		root.findViewById(id.btnSwitchUser).setOnClickListener(btnSwitchUserClickListener);
		//root.findViewById(id.btnEmailLogs).setOnClickListener(btnEmailLogsClickListener);
		root.findViewById(id.btnClearLogs).setOnClickListener(btnClearLogsClickListener);

		return root;
	}

	/**
	 * When the "Email Logs" button is clicked, email the logs. Present a confirmation dialog first.
	 */
//	private final View.OnClickListener btnEmailLogsClickListener = new View.OnClickListener() {
//
//		@Override
//		public void onClick(View v) {
//			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//			builder.setTitle(string.settingsscreen_dialog_emaillogs)
//					.setMessage(string.settingsscreen_dialog_emaillogs_message)
//					.setPositiveButton(string.yes, new DialogInterface.OnClickListener() {
//
//						@Override
//						public void onClick(DialogInterface dialog, int which) {
//
//							// Actually email the logs
//							try {
//								File logFileArchive = Logger.archiveLogFiles(getActivity());
//
//								if (logFileArchive == null) {
//									Toast.makeText(getActivity(), string.settingsscreen_toast_nologfiles,
//											Toast.LENGTH_SHORT).show();
//								}
//
//								else {
//									String emailAddress = "amea020@aucklanduni.ac.nz";
//									String subject = "Log files for REMOTE-CR User "
//											+ OdinPreferences.UserName.getStringValue(getActivity(),
//													"[Unknown Username]");
//									String message = "";
//
//									Uri fileUri = Uri.fromFile(logFileArchive);
//
//									Intent emailIntent = new Intent(Intent.ACTION_SEND);
//									emailIntent.setType("message/rfc822");
//									emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { emailAddress });
//									emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
//									emailIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
//									emailIntent.putExtra(Intent.EXTRA_TEXT, message);
//
//									String chooserTitle = getString(string.settingsscreen_chooser_emaillogs);
//									startActivity(Intent.createChooser(emailIntent, chooserTitle));
//								}
//							} catch (IOException ex) {
//								logger.error("Can't archive logs: " + ex.getMessage(), ex);
//							}
//
//						}
//					}).setNegativeButton(string.no, null).setCancelable(true).show();
//		}
//	};

	/**
	 * When the "Clear Logs" button is clicked, clear the logs. Present a confirmation dialog first.
	 */
	private final View.OnClickListener btnClearLogsClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(string.settingsscreen_dialog_clearlogs)
					.setMessage(string.settingsscreen_dialog_clearlogs_message)
					.setPositiveButton(string.yes, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {

							// Actually clear the logs
							//Logger.clearAllLogs(getActivity());

							Toast.makeText(getActivity(), string.settingsscreen_toast_logscleared, Toast.LENGTH_SHORT)
									.show();

						}
					}).setNegativeButton(string.no, null).setCancelable(true).show();
		}
	};

	/**
	 * When the "Switch User" button is clicked, prompts the user if they really want to do this. If so, clears the
	 * username and password data then finishes the parent activity.
	 */
	private final View.OnClickListener btnSwitchUserClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(string.settingsscreen_dialog_switchuser)
					.setMessage(string.settingsscreen_dialog_switchuser_message)
					.setPositiveButton(string.yes, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {

							OdinPreferences.UserName.clearValue(getActivity());
							OdinPreferences.Password.clearValue(getActivity());
							getActivity().finish();

						}
					}).setNegativeButton(string.no, null).setCancelable(true).show();
		}
	};

	private final View.OnClickListener btnAddBloodLactateListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			LayoutInflater li = getActivity().getLayoutInflater();
			LinearLayout row = (LinearLayout) li.inflate(layout.fragment_nihi_settings_blood_lactate_row,
					pnlBloodLactate, false);
			row.findViewById(id.btnRemoveBloodLactate).setOnClickListener(btnRemoveBloodLactateListener);
			pnlBloodLactate.addView(row, pnlBloodLactate.getChildCount() - 1);
			bloodLactateRows.add(row);

			txtNoBloodLactate.setVisibility(View.INVISIBLE);
			pnlBloodLactateHeaders.setVisibility(View.VISIBLE);
		}
	};

	private final View.OnClickListener btnRemoveBloodLactateListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			LinearLayout row = (LinearLayout) v.getParent();
			v.setOnClickListener(null);
			bloodLactateRows.remove(row);
			pnlBloodLactate.removeView(row);

			if (bloodLactateRows.size() == 0) {
				txtNoBloodLactate.setVisibility(View.VISIBLE);
				pnlBloodLactateHeaders.setVisibility(View.GONE);
			}
		}
	};

	public void savePrefs() {

		// Basic text fields
		saveStringPref(txtName, NihiPreferences.Name);
		saveStringPref(txtEmail, NihiPreferences.Email);
		saveIntPref(txtHeight, NihiPreferences.Height);
		saveIntPref(txtWeight, NihiPreferences.Weight);
		saveIntPref(txtMaxHR, NihiPreferences.MaxHeartRate);
		saveIntPref(txtRestingHR, NihiPreferences.RestHeartRate);

		if (rdoMale.isChecked()) {
			NihiPreferences.Gender.setValue(getActivity(), Gender.Male);
		} else {
			NihiPreferences.Gender.setValue(getActivity(), Gender.Female);
		}

		Date dob = new Date(pckDOB.getCalendarView().getDate());
		NihiPreferences.DateOfBirth.setValue(getActivity(), dob);

		// Blood lactate data
		BloodLactateConcentrationData blcData = new BloodLactateConcentrationData();
		for (LinearLayout ll : bloodLactateRows) {
			EditText txtBLC = (EditText) ll.findViewById(id.txtBloodLactate);
			EditText txtHRR = (EditText) ll.findViewById(id.txtPropHRR);
			String strBLC = txtBLC.getText().toString();
			String strHRR = txtHRR.getText().toString();
			if (!("".equals(strBLC) || "".equals(strHRR))) {
				float blc = Float.parseFloat(strBLC);
				float hrr = Float.parseFloat(strHRR);
				blcData.add(hrr, blc);
			}
		}

		if (blcData.numEntries() > 0) {
			NihiPreferences.BloodLactateLevels.setValue(getActivity(), blcData);
		} else {
			NihiPreferences.BloodLactateLevels.clearValue(getActivity());
		}

	}

	private void saveStringPref(EditText textbox, IPreferences pref) {
		String value = textbox.getText().toString();
		if ("".equals(value)) {
			pref.clearValue(getActivity());
		} else {
			pref.setValue(getActivity(), textbox.getText().toString());
		}
	}

	private void saveIntPref(EditText textbox, IPreferences pref) {
		String value = textbox.getText().toString();
		int intValue = -1;
		try {
			intValue = Integer.parseInt(value);
			pref.setValue(getActivity(), intValue);
		} catch (NumberFormatException e) {
			pref.clearValue(getActivity());
		}
	}

	// private void saveLongPref(EditText textbox, IPreferences pref) {
	// String value = textbox.getText().toString();
	// long lngvalue = -1;
	// try {
	// lngvalue = Long.parseLong(value);
	// pref.setValue(getActivity(), lngvalue);
	// } catch (NumberFormatException e) {
	// pref.clearValue(getActivity());
	// }
	// }

	/**
	 * Allows all the number picker controls to scroll properly, given that they're inside a ScrollView.
	 * 
	 * TODO if more than just this Fragment contains a scrollable sub-view, this will need to be refactored.
	 */
	public void enableNumberPickerScrollFix(ScrollView scroller) {

		// HACK: Get references to the internal number pickers.
		Field[] fields = DatePicker.class.getDeclaredFields();
		final List<View> numberPickers = new ArrayList<View>();
		for (Field f : fields) {
			// if (f.getType().getName().equals("com.android.internal.widget.NumberPicker")) {
			if (f.getType().equals(NumberPicker.class)) {
				f.setAccessible(true);
				try {
					numberPickers.add((View) f.get(pckDOB));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}

		scroller.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// pckDOB.getParent().requestDisallowInterceptTouchEvent(false);
				for (View np : numberPickers) {
					np.getParent().requestDisallowInterceptTouchEvent(false);
				}
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
		for (View np : numberPickers) {
			np.setOnTouchListener(childTouchListener);
		}
		// pckDOB.setOnTouchListener(childTouchListener);
	}
}
