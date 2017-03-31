package nz.ac.auckland.nihi.trainer.views;

import java.util.Collection;

import nz.ac.auckland.nihi.trainer.R.id;
import nz.ac.auckland.nihi.trainer.R.layout;
import nz.ac.auckland.nihi.trainer.R.string;
import nz.ac.auckland.nihi.trainer.data.ExerciseNotification;
import nz.ac.auckland.nihi.trainer.data.ExerciseSummary;
import nz.ac.auckland.nihi.trainer.util.AndroidTextUtils;
import nz.ac.auckland.nihi.trainer.util.UiUtils;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class NotificationsView extends LinearLayout {

	public NotificationsView(Context context) {
		super(context);
		createUI(context);
	}

	public NotificationsView(Context context, AttributeSet attrs) {
		super(context, attrs);
		createUI(context);
	}

	public NotificationsView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		createUI(context);
	}

	private void createUI(Context context) {
		this.setOrientation(VERTICAL);
	}

	public void setNotifications(ExerciseSummary summary) {
		if (summary == null) {
			setNotifications((Collection<ExerciseNotification>) null);
		} else {
			setNotifications(summary.getNotifications());
		}
	}

	public void setNotifications(Collection<ExerciseNotification> notifications) {

		removeAllViews();

		LayoutInflater li = LayoutInflater.from(getContext());

		// Add a dummy control if there's no notifications.
		if (notifications == null || notifications.size() == 0) {
			FontableTextView dummy = (FontableTextView) li.inflate(layout.single_fontable_textview, this, false);// new
																													// FontableTextView(getContext());

			dummy.setText(string.placeholder_novalue);
			// dummy.setTextColor(getResources().getColor(android.R.color.darker_gray));
			// dummy.setTextAppearance(getContext(), android.R.attr.textAppearanceSmall);
			LayoutParams lparams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			lparams.setMargins(0, 0, 0, UiUtils.toPx(8, getContext()));
			addView(dummy, lparams);
			return;
		}

		// Add one row per notification.
		for (ExerciseNotification n : notifications) {

			// Create and populate row
			View row = li.inflate(layout.exercise_summary_notifications_row, this, false);
			TextView txtNotification = (TextView) row.findViewById(id.txtNotification);
			txtNotification.setText(n.getNotification());
			TextView txtTime = (TextView) row.findViewById(id.txtTime);
			txtTime.setText(AndroidTextUtils.getRelativeTimeStringSeconds(n.getRelativeTimeInSeconds()));

			// Set bottom margin for row
			LayoutParams lparams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			lparams.setMargins(0, 0, 0, UiUtils.toPx(8, getContext()));

			// Add it
			addView(row, lparams);

		}

	}

}
