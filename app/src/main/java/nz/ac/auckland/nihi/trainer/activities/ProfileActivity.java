package nz.ac.auckland.nihi.trainer.activities;

import nz.ac.auckland.cs.odin.android.api.activities.OdinFragmentActivity;
import nz.ac.auckland.cs.odin.android.api.asynctask.OdinAsyncTask;
import nz.ac.auckland.cs.odin.android.api.prefs.OdinPreferences;
import nz.ac.auckland.cs.odin.android.api.services.IOdinService;
import nz.ac.auckland.cs.odin.data.JsonMessage;
import nz.ac.auckland.cs.odin.interconnect.common.MessageHandle;
import nz.ac.auckland.nihi.trainer.R.id;
import nz.ac.auckland.nihi.trainer.R.layout;
import nz.ac.auckland.nihi.trainer.R.string;
import nz.ac.auckland.nihi.trainer.fragments.NihiPreferencesFragment;
import nz.ac.auckland.nihi.trainer.messaging.ProfileData;
import nz.ac.auckland.nihi.trainer.prefs.NihiPreferences;

import org.apache.log4j.Logger;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.widget.ScrollView;

public class ProfileActivity extends OdinFragmentActivity {

	private static final Logger logger = Logger.getLogger(ProfileActivity.class);

	// ************************************************************************************************************
	// Application Lifecycle
	// ************************************************************************************************************
	public ProfileActivity() {
		super(false);
	}

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);

		setContentView(layout.profile_screen);

		final FragmentManager fragMgr = getSupportFragmentManager();

		ScrollView scroller = (ScrollView) findViewById(id.scroller);
		((NihiPreferencesFragment) fragMgr.findFragmentById(id.fragment_nihiPrefs))
				.enableNumberPickerScrollFix(scroller);

		findViewById(id.btnOk).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					// ((OdinPreferencesFragment) fragMgr.findFragmentById(id.fragment_odinPrefs)).savePrefs();
					((NihiPreferencesFragment) fragMgr.findFragmentById(id.fragment_nihiPrefs)).savePrefs();

					// finish();
				} catch (Exception ex) {
					logger.error(ex);
					return;
					// TODO Display a dialog showing the save error, and don't close the activity.
				}

				// If the prefs were saved
				// TODO: REMOVE ODIN SERVICE
//				SaveProfileToOdinTask saveToOdinTask = new SaveProfileToOdinTask();
//				saveToOdinTask.execute();
			}
		});

		findViewById(id.btnCancel).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}

	// ************************************************************************************************************

	// ************************************************************************************************************
	// Odin Connectivity
	// ************************************************************************************************************
	// TODO: REMOVE ODIN SERVICE - Profile Stuff
//	private class SaveProfileToOdinTask extends OdinAsyncTask<Void, Void, Void> {
//
//		private ProgressDialog progressDialog;
//
//		public SaveProfileToOdinTask() {
//			super(getOdinService());
//		}
//
//		@Override
//		protected void onPreExecute() {
//			String title = getString(string.goalscreen_dialog_synch);
//			String message = getString(string.goalscreen_dialog_synchwithodin);
//			progressDialog = ProgressDialog.show(ProfileActivity.this, title, message);
//		}
//
//		@Override
//		protected Void doInBackground(IOdinService odinService, Void... notUsed) throws Exception {
//			// Serialize profile data
//			ProfileData profileData = new ProfileData();
//			profileData.setUserId(OdinPreferences.UserID.getLongValue(ProfileActivity.this, 0));
//			profileData.setProfileData(NihiPreferences.serialize(ProfileActivity.this));
//			JsonMessage message = new JsonMessage(profileData);
//
//			// Send to Odin
//			MessageHandle handle = getOdinService().sendAsyncMessage(message);
//
//			// If we were the ones that connected, wait for delivery
//			if (!wasAlreadyConnected()) {
//				handle.waitForDelivery();
//			}
//			return null;
//		}
//
//		@Override
//		protected void onPostExecute() {
//
//			progressDialog.dismiss();
//
//			try {
//				getResult();
//				finish();
//			} catch (Exception error) {
//				AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);
//				builder.setTitle(string.error).setMessage(string.settingsscreen_dialog_cantsave).setCancelable(false)
//						.setPositiveButton(string.ok, new DialogInterface.OnClickListener() {
//
//							@Override
//							public void onClick(DialogInterface dialog, int which) {
//								finish();
//							}
//						}).show();
//			}
//
//		}
//
//	}
	// ************************************************************************************************************

}
