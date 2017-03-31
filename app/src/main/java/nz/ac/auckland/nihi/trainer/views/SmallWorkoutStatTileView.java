package nz.ac.auckland.nihi.trainer.views;

import android.content.Context;
import android.util.AttributeSet;

public class SmallWorkoutStatTileView extends AbstractWorkoutStatTileView {

	public SmallWorkoutStatTileView(Context context) {
		super(context);
	}

	public SmallWorkoutStatTileView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SmallWorkoutStatTileView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected int getLayoutId() {
		return nz.ac.auckland.nihi.trainer.R.layout.workout_screen_small_stat_tile;
	}

}
