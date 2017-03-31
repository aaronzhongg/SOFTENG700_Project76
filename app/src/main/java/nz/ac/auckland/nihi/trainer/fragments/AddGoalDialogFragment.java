package nz.ac.auckland.nihi.trainer.fragments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import nz.ac.auckland.cs.odin.android.api.prefs.OdinPreferences;
import nz.ac.auckland.nihi.trainer.R.id;
import nz.ac.auckland.nihi.trainer.R.layout;
import nz.ac.auckland.nihi.trainer.R.string;
import nz.ac.auckland.nihi.trainer.data.Goal;
import nz.ac.auckland.nihi.trainer.data.GoalType;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class AddGoalDialogFragment extends DialogFragment {

	public interface AddGoalDialogFragmentListener {

		void onFinishAddGoalDialog(Goal goal);

	}

	private List<Goal> currentGoals;

	private Spinner cboGoalTypes;
	private EditText txtWeeklyTarget;

	public void setCurrentGoals(List<Goal> currentGoals) {
		this.currentGoals = currentGoals;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View root = inflater.inflate(layout.goals_screen_new_goal_dialog, null);

		ArrayList<GoalType> goalTypes = new ArrayList<GoalType>();
		for (GoalType type : GoalType.values()) {
			boolean shouldAdd = true;
			if (currentGoals != null) {
				for (Goal g : currentGoals) {
					if (g.getGoalType() == type) {
						shouldAdd = false;
						break;
					}
				}
			}
			if (shouldAdd) {
				goalTypes.add(type);
			}
		}
		cboGoalTypes = (Spinner) root.findViewById(id.cboGoalType);
		cboGoalTypes.setAdapter(new GoalTypeAdapter(goalTypes));

		txtWeeklyTarget = (EditText) root.findViewById(id.txtWeeklyTarget);

		getDialog().setTitle(string.goalscreen_dialog_newgoal);
		root.findViewById(id.btnOk).setOnClickListener(btnOkListener);
		root.findViewById(id.btnCancel).setOnClickListener(btnCancelClickListener);

		return root;
	}

	private final View.OnClickListener btnOkListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {

			Goal goal = new Goal();
			goal.setUserId(OdinPreferences.UserID.getLongValue(getActivity(), -1));
			goal.setGoalType((GoalType) cboGoalTypes.getSelectedItem());
			String strWeeklyTarget = txtWeeklyTarget.getText().toString();
			if (!strWeeklyTarget.equals("")) {
				goal.setWeeklyTarget(Integer.valueOf(strWeeklyTarget));
			} else {
				return;
			}

			// ((GoalsActivity) getActivity()).addGoal(goal);
			((AddGoalDialogFragmentListener) getActivity()).onFinishAddGoalDialog(goal);
			dismiss();

		}
	};

	private final View.OnClickListener btnCancelClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			// ((AddGoalFragmentListener) getActivity()).onFinishAddGoalDialog(AddGoalFragment.this, true);
			dismiss();
		}
	};

	private class GoalTypeAdapter extends ArrayAdapter<GoalType> {

		public GoalTypeAdapter(Collection<GoalType> types) {
			super(AddGoalDialogFragment.this.getActivity(), layout.simple_dropdown_item_1line);
			this.addAll(types);
		}

		@Override
		public View getDropDownView(int position, View convertView, ViewGroup parent) {
			TextView view = (TextView) super.getDropDownView(position, convertView, parent);
			view.setText(this.getItem(position).toString());

			return view;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView view = (TextView) super.getView(position, convertView, parent);
			view.setText(this.getItem(position).toString());

			return view;
		}

	}
}
