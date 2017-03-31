package nz.ac.auckland.nihi.trainer.activities;

import nz.ac.auckland.nihi.trainer.R.id;
import nz.ac.auckland.nihi.trainer.R.layout;
import nz.ac.auckland.nihi.trainer.views.AbstractWorkoutStatTileView.ViewedStat;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

public class TileTypeSelectorActivity extends Activity {

	public static final String EXTRA_TILE_TYPE = TileTypeSelectorActivity.class.getName() + ".TileType";
	public static final String EXTRA_TILE_ID = TileTypeSelectorActivity.class.getName() + ".TileID";
	public static final String EXTRA_FRAGMENT_ID = TileTypeSelectorActivity.class.getName() + ".FragmentID";

	private int tileId;
	private int fragmentId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setResult(RESULT_CANCELED);
		setContentView(layout.tile_picker);

		tileId = getIntent().getIntExtra(EXTRA_TILE_ID, 0);
		fragmentId = getIntent().getIntExtra(EXTRA_FRAGMENT_ID, 0);

		// Pre-select the appropriate button if the appropriate value was passed in
		ViewedStat stat = (ViewedStat) getIntent().getSerializableExtra(EXTRA_TILE_TYPE);
		if (stat != null) {
			switch (stat) {
			case AvgHeartRate:
				((ToggleButton) findViewById(id.btnAvgHeartRate)).setChecked(true);
				break;
			case AvgPercentHeartRate:
				((ToggleButton) findViewById(id.btnAvgPercentHeartRate)).setChecked(true);
				break;
			case AvgSpeed:
				((ToggleButton) findViewById(id.btnAvgSpeed)).setChecked(true);
				break;
			case Distance:
				((ToggleButton) findViewById(id.btnDistance)).setChecked(true);
				break;
			case TrainingLoad:
				((ToggleButton) findViewById(id.btnCumulativeTrainingImpulse)).setChecked(true);
				break;
			// case CurrentTrainingImpulse:
			// ((ToggleButton) findViewById(id.btnCurrentTrainingImpulse)).setChecked(true);
			// break;
			case HeartRate:
				((ToggleButton) findViewById(id.btnHeartRate)).setChecked(true);
				break;
			case PercentHeartRate:
				((ToggleButton) findViewById(id.btnPercentHeartRate)).setChecked(true);
				break;
			case Speed:
				((ToggleButton) findViewById(id.btnSpeed)).setChecked(true);
				break;
			}
		}

		// Hook up listener to buttons
		((ToggleButton) findViewById(id.btnHeartRate)).setOnCheckedChangeListener(buttonSelectListener);
		((ToggleButton) findViewById(id.btnAvgHeartRate)).setOnCheckedChangeListener(buttonSelectListener);
		((ToggleButton) findViewById(id.btnPercentHeartRate)).setOnCheckedChangeListener(buttonSelectListener);
		((ToggleButton) findViewById(id.btnAvgPercentHeartRate)).setOnCheckedChangeListener(buttonSelectListener);
		((ToggleButton) findViewById(id.btnSpeed)).setOnCheckedChangeListener(buttonSelectListener);
		((ToggleButton) findViewById(id.btnAvgSpeed)).setOnCheckedChangeListener(buttonSelectListener);
		((ToggleButton) findViewById(id.btnDistance)).setOnCheckedChangeListener(buttonSelectListener);
		((ToggleButton) findViewById(id.btnCumulativeTrainingImpulse)).setOnCheckedChangeListener(buttonSelectListener);
		// ((ToggleButton) findViewById(id.btnCurrentTrainingImpulse)).setOnCheckedChangeListener(buttonSelectListener);

		// Enable the close button
		findViewById(id.btnClose).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}

	private final CompoundButton.OnCheckedChangeListener buttonSelectListener = new CompoundButton.OnCheckedChangeListener() {

		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			ViewedStat stat = Enum.valueOf(ViewedStat.class, buttonView.getTag().toString());
			Intent result = new Intent();
			result.putExtra(EXTRA_TILE_TYPE, stat);
			result.putExtra(EXTRA_TILE_ID, tileId);
			result.putExtra(EXTRA_FRAGMENT_ID, fragmentId);
			setResult(RESULT_OK, result);
			finish();
		}
	};

}
