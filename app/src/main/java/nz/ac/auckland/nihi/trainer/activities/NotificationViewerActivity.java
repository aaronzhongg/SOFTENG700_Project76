package nz.ac.auckland.nihi.trainer.activities;

import java.util.List;
import java.util.Locale;

import nz.ac.auckland.cs.odin.android.api.services.testharness.TestHarnessUtils;
import nz.ac.auckland.nihi.trainer.R.id;
import nz.ac.auckland.nihi.trainer.R.layout;
import nz.ac.auckland.nihi.trainer.data.ExerciseNotification;
import nz.ac.auckland.nihi.trainer.data.session.ExerciseSessionData;
import nz.ac.auckland.nihi.trainer.services.workout.IWorkoutService;
import nz.ac.auckland.nihi.trainer.services.workout.WorkoutService;
import nz.ac.auckland.nihi.trainer.util.AndroidTextUtils;

import org.apache.log4j.Logger;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.odin.android.services.LocalBinder;

public class NotificationViewerActivity extends Activity {

	private static final Logger logger = Logger.getLogger(NotificationViewerActivity.class);

	// private final List<View> speechButtons = new ArrayList<View>();

	private ListView lstNotifications;

	private TextToSpeech tts;
	private boolean ttsEnabled;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(layout.notification_viewer);
		lstNotifications = (ListView) findViewById(id.lstNotifications);

		findViewById(id.btnClose).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});

		findViewById(id.btnOk).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});

		// Bind to the workout service. This will allow us to get its data.
		bindService(new Intent(this, WorkoutService.class), conn, BIND_AUTO_CREATE);

		if (!TestHarnessUtils.isTestHarness()) {
			tts = new TextToSpeech(this, ttsOnInitListener);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (tts != null) {
			tts.stop();
			tts.shutdown();
		}

		unbindService(conn);
	}

	private final ServiceConnection conn = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub

		}

		/**
		 * When the workout service connects, get its current session and get the notifications from there.
		 */
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			LocalBinder<IWorkoutService> workoutService = (LocalBinder<IWorkoutService>) service;

			workoutService.getService().clearNotificationTrayIcon();
			ExerciseSessionData currentSession = workoutService.getService().getCurrentSession();
			if (currentSession != null) {

				List<ExerciseNotification> notifications = currentSession.getNotifications();
				lstNotifications.setAdapter(new NotificationAdapter(notifications));

			}
		}
	};

	private final View.OnClickListener speechButtonClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			if (ttsEnabled) {
				// for (View button : speechButtons) {
				// button.setEnabled(false);
				// }
				tts.speak(v.getTag().toString(), TextToSpeech.QUEUE_FLUSH, null);
			}
		}
	};

	// @SuppressLint("NewApi")
	// private final UtteranceProgressListener utteranceProgressListener = new UtteranceProgressListener() {
	//
	// @Override
	// public void onStart(String utteranceId) {
	// // TODO Auto-generated method stub
	//
	// }
	//
	// @Override
	// public void onError(String utteranceId) {
	// // TODO Auto-generated method stub
	//
	// }
	//
	// @Override
	// public void onDone(String utteranceId) {
	// for (View button : speechButtons) {
	// button.setEnabled(true);
	// }
	// }
	// };

	@SuppressLint("NewApi")
	private final TextToSpeech.OnInitListener ttsOnInitListener = new TextToSpeech.OnInitListener() {

		@Override
		public void onInit(int status) {
			if (status == TextToSpeech.SUCCESS) {

				int result = tts.setLanguage(Locale.UK);

				if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
					logger.error("ttsOnInitListener.onInit(): This Language is not supported");
				} else {
					ttsEnabled = true;
					// tts.setOnUtteranceProgressListener(utteranceProgressListener);
					// for (View button : speechButtons) {
					// button.setEnabled(true);
					// }
					logger.info("ttsOnInitListener.onInit(): Text-to-speech enabled!");
				}

			} else {
				logger.error("ttsOnInitListener.onInit(): text-to-speech initialization failed.");
			}
		}
	};

	/**
	 * Displays notifications in a list view.
	 * 
	 */
	private class NotificationAdapter extends ArrayAdapter<ExerciseNotification> {

		private NotificationAdapter(List<ExerciseNotification> notifications) {
			super(NotificationViewerActivity.this,
					nz.ac.auckland.nihi.trainer.R.layout.notification_viewer_notification_row);

			// speechButtons.clear();

			addAll(notifications);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			View view = null;
			if (convertView == null) {
				LayoutInflater li = LayoutInflater.from(getContext());
				view = li.inflate(layout.notification_viewer_notification_row, parent, false);
			} else {
				view = convertView;
			}

			ExerciseNotification notification = (ExerciseNotification) getItem(position);

			TextView txtNotification = (TextView) view.findViewById(id.txtNotification);
			txtNotification.setText(notification.getNotification());

			TextView txtTime = (TextView) view.findViewById(id.txtTime);
			txtTime.setText(AndroidTextUtils.getRelativeTimeStringSeconds(notification.getRelativeTimeInSeconds()));

			View speechButton = view.findViewById(id.btnSpeak);
			// speechButton.setEnabled(true);
			speechButton.setTag(notification.getNotification());
			speechButton.setOnClickListener(speechButtonClickListener);
			// if (!speechButtons.contains(speechButton)) {
			// speechButtons.add(speechButton);
			// }

			return view;
		}
	}

}
