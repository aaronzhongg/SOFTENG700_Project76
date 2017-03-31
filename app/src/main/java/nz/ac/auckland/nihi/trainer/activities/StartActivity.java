package nz.ac.auckland.nihi.trainer.activities;

import nz.ac.auckland.cs.odin.android.api.persistence.OdinDBHelper;
import nz.ac.auckland.cs.odin.android.api.prefs.OdinPreferences;
import nz.ac.auckland.cs.odin.android.api.services.testharness.TestHarnessPropertySetter;
import nz.ac.auckland.cs.odin.android.api.services.testharness.TestHarnessService;
import nz.ac.auckland.cs.odin.android.api.services.testharness.TestHarnessUtils;
import nz.ac.auckland.cs.ormlite.DatabaseManager;
import nz.ac.auckland.nihi.trainer.R.integer;
import nz.ac.auckland.nihi.trainer.R.string;
import nz.ac.auckland.nihi.trainer.data.NihiDBHelper;
import nz.ac.auckland.nihi.trainer.fragments.SelectHostDialogFragment;
import nz.ac.auckland.nihi.trainer.fragments.SelectHostDialogFragment.SelectHostDialogFragmentListener;
import nz.ac.auckland.nihi.trainer.hosts.NihiAppHosts;
import nz.ac.auckland.nihi.trainer.prefs.NihiPreferences;

import org.apache.log4j.Logger;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.odin.android.bioharness.data.BioharnessDBHelper;
import com.odin.android.bioharness.prefs.BioharnessPreferences;

/**
 * Performs some basic setup, makes sure the user has a host selected, then starts the home screen activity.
 * 
 * TODO Initial login check should probably be here also.
 * 
 * @author Andrew Meads
 * 
 */
public class StartActivity extends FragmentActivity implements SelectHostDialogFragmentListener {

	private static final Logger logger = Logger.getLogger(StartActivity.class);

	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Move the log file to a proper location.
		Logger.setupLogFileLocation(this);

		// Define the database.
		DatabaseManager.getInstance().defineDatabase(this, string.db_name, integer.db_version, NihiDBHelper.class,
				BioharnessDBHelper.class, OdinDBHelper.class);

		// If we're running on the test harness, start the test harness service then start the home screen activity.
		if (TestHarnessUtils.isTestHarness()) {
			logger.warn("onCreate(): Running on test harness. Starting TestHarness service.");
			Intent thsIntent = new Intent(getApplicationContext(), TestHarnessService.class);
			thsIntent.putExtra(TestHarnessService.EXTRA_RESET_SOCKET, true);
			startService(thsIntent);

			TestHarnessPropertySetter.addPropertySetterClass(BioharnessPreferences.class);
			TestHarnessPropertySetter.addPropertySetterClass(NihiPreferences.class);

			startHomeScreenActivity();
		}

		// Otherwise, check that we've defined a HostName and Port. If so, start the home screen activity. If not, show
		// a dialog to pick one.
		else {
			OdinPreferences.SurrogateName.setValue(this, "nihi-trainer");
			logger.debug("onCreate(): Not running on test harness.");

			// If we need to pick a host, start the dialog for that
			if (OdinPreferences.HostName.getStringValue(this, null) == null) {
				SelectHostDialogFragment frag = new SelectHostDialogFragment();
				frag.show(getSupportFragmentManager(), "SelectHostDialog");
			}

			// Otherwise we're good to go.
			else {
				startHomeScreenActivity();
			}
		}

		// if (!TestHarnessUtils.isTestHarness()) {
		// OdinPreferences.HostName.setValue(this, "odin.cs.auckland.ac.nz");
		// OdinPreferences.Port.setValue(this, 8081);
		// OdinPreferences.SurrogateName.setValue(this, "nihi-trainer");
		// }
	}

	private void startHomeScreenActivity() {
		Intent intent = new Intent(this, HomeScreenActivity.class);
		startActivityForResult(intent, 0);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		finish();
	}

	/**
	 * Called when we select a host. Saves our selection and starts the main activity.
	 */
	@Override
	public void onFinishSelectHost(NihiAppHosts host) {
		OdinPreferences.HostName.setValue(this, host.getHostName());
		OdinPreferences.Port.setValue(this, host.getPort());
		startHomeScreenActivity();
	}

}
