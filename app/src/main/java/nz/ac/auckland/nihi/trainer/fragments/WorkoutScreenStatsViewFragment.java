package nz.ac.auckland.nihi.trainer.fragments;

import java.util.ArrayList;
import java.util.List;

import nz.ac.auckland.nihi.trainer.data.session.ExerciseSessionData;
import nz.ac.auckland.nihi.trainer.views.AbstractWorkoutStatTileView;
import nz.ac.auckland.nihi.trainer.views.AbstractWorkoutStatTileView.ViewedStat;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public abstract class WorkoutScreenStatsViewFragment extends Fragment {

	private ViewGroup root;

	public interface StatsViewButtonClickListener {

		void statsViewButtonClicked(WorkoutScreenStatsViewFragment fragment, int buttonId,
				ViewedStat currentlyViewedStat);

	}

	private final List<AbstractWorkoutStatTileView> statViews = new ArrayList<AbstractWorkoutStatTileView>();

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		root = (ViewGroup) inflater.inflate(getLayoutId(), container, false);

		// @formatter:off
		ViewedStat[] viewedStatDefaults = new ViewedStat[] {
				ViewedStat.HeartRate,
				ViewedStat.Speed,
				ViewedStat.Distance,
				ViewedStat.TrainingLoad
		};
		// @formatter:on

		int counter = 0;
		for (int i = 0; i < root.getChildCount(); i++) {
			View child = root.getChildAt(i);
			if (child instanceof AbstractWorkoutStatTileView) {
				AbstractWorkoutStatTileView statView = (AbstractWorkoutStatTileView) child;
				statView.setViewedStat(viewedStatDefaults[counter]);
				counter = (counter + 1) % viewedStatDefaults.length;
				statView.setOnClickListener(tileClickListener);
				statViews.add(statView);
			}
		}

		return root;
	}

	public void setExerciseSession(ExerciseSessionData session) {
		for (AbstractWorkoutStatTileView tile : statViews) {
			tile.setExerciseData(session);
		}
	}

	public void setViewedStat(int buttonId, ViewedStat stat) {
		for (AbstractWorkoutStatTileView view : statViews) {
			if (buttonId == view.getId()) {
				view.setViewedStat(stat);
				return;
			}
		}
	}

	private final View.OnClickListener tileClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			((StatsViewButtonClickListener) getActivity()).statsViewButtonClicked(WorkoutScreenStatsViewFragment.this,
					v.getId(), ((AbstractWorkoutStatTileView) v).getViewedStat());
		}
	};

	public void setVisibility(int visibility) {
		root.setVisibility(visibility);
	}

	protected abstract int getLayoutId();

}
