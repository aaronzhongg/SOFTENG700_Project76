package nz.ac.auckland.nihi.trainer.views;

import java.util.ArrayList;
import java.util.Collection;

import nz.ac.auckland.nihi.trainer.R;
import nz.ac.auckland.nihi.trainer.R.id;
import nz.ac.auckland.nihi.trainer.data.ExerciseSummary;
import nz.ac.auckland.nihi.trainer.data.Symptom;
import nz.ac.auckland.nihi.trainer.data.SymptomEntry;
import nz.ac.auckland.nihi.trainer.util.AndroidTextUtils;
import nz.ac.auckland.nihi.trainer.util.UiUtils;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SymptomsView extends FrameLayout {

	private View root;

	private final ArrayList<View> children = new ArrayList<View>();

	public SymptomsView(Context context) {
		super(context);
		createUI(context);
	}

	public SymptomsView(Context context, AttributeSet attrs) {
		super(context, attrs);
		createUI(context);
	}

	public SymptomsView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		createUI(context);
	}

	private void createUI(Context context) {

		// Inflate the layout and add it to this.
		LayoutInflater li = LayoutInflater.from(context);
		root = li.inflate(nz.ac.auckland.nihi.trainer.R.layout.exercise_summary_symptoms, this, false);
		addView(root, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
	}

	public void setSymptoms(ExerciseSummary summary) {
		if (summary == null) {
			setSymptoms((Collection<SymptomEntry>) null);
		} else {
			setSymptoms(summary.getSymptoms());
		}
	}

	public void setSymptoms(Collection<SymptomEntry> symptoms) {
		LayoutInflater li = LayoutInflater.from(getContext());
		boolean symptomAdded = false;

		for (View v : children) {
			((LinearLayout) v.getParent()).removeView(v);
		}
		children.clear();

		for (Symptom symptomType : Symptom.values()) {
			String panelName = "pnl" + symptomType.toString();
			LinearLayout panel = (LinearLayout) root.findViewById(UiUtils.getViewID(panelName, getContext()));

			if (symptoms != null) {
				// Loop through all symptoms we have...
				int symptomsAdded = 0;
				for (SymptomEntry symptom : symptoms) {
					// If this symptom entry matches the symptom type we're currently adding...
					if (symptom.getSymptom() == symptomType) {

						symptomAdded = true;

						// Create a view to show the symptom data. Get the text boxes that we'll populate.
						LinearLayout row = (LinearLayout) li.inflate(R.layout.exercise_summary_symptoms_row, panel,
								false);
						TextView txtSymptomLevel = (TextView) row.findViewById(id.txtSymptomLevel);
						TextView txtTime = (TextView) row.findViewById(id.txtTime);

						// Populate
						String desc = AndroidTextUtils.getSymptomStrengthDescription(getContext(),
								symptom.getStrength());
						txtSymptomLevel.setText(desc);
						txtTime.setText(AndroidTextUtils.getRelativeTimeStringSeconds(symptom
								.getRelativeTimeInSeconds()));

						// Finish up
						panel.addView(row);
						children.add(row);
						symptomsAdded++;
					}
				}

				// If we didn't add any symptoms of this type, hide the panel.
				if (symptomsAdded == 0) {
					panel.setVisibility(View.GONE);
				}

				// If we didn't add any symptoms of this type, add a dummy control.
				// if (symptomsAdded == 0) {
				// TextView dummy = new TextView(getContext());
				// dummy.setText("-");
				// dummy.setTextColor(getResources().getColor(android.R.color.darker_gray));
				// dummy.setTextAppearance(getContext(), android.R.attr.textAppearanceSmall);
				// panel.addView(dummy);
				// }
			}
		}

		// If no symptoms were added at all, make the "none" box visible.
		root.findViewById(id.txtBlank).setVisibility(symptomAdded ? View.GONE : View.VISIBLE);
	}
}
