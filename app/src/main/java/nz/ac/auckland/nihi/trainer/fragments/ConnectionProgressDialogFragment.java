package nz.ac.auckland.nihi.trainer.fragments;

import nz.ac.auckland.cs.odin.interconnect.common.EndpointConnectionStatus;
import nz.ac.auckland.nihi.trainer.R.drawable;
import nz.ac.auckland.nihi.trainer.R.id;
import nz.ac.auckland.nihi.trainer.R.layout;
import nz.ac.auckland.nihi.trainer.R.string;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.odin.android.bioharness.BHConnectivityStatus;

/**
 * Displays a "Connecting..." message, an indeterminite progress bar, and the connectivity status icons for Odin and the
 * Bioharness.
 * 
 * @author Andrew Meads
 * 
 */
public class ConnectionProgressDialogFragment extends DialogFragment {

	private ImageView imgOdin, imgBioharness;
	private EndpointConnectionStatus odinConnectionStatus = EndpointConnectionStatus.ATTEMPTING_CONNECT;
	private BHConnectivityStatus bhConnectionStatus = BHConnectivityStatus.CONNECTING;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View root = inflater.inflate(layout.workout_screen_connect_dialog, container, false);
		imgOdin = (ImageView) root.findViewById(id.imgOdin);
		imgBioharness = (ImageView) root.findViewById(id.imgBioharness);
		update();

		getDialog().setTitle(string.connectivitydlg_label_connecting);
		getDialog().setCancelable(false);

		return root;

	}

	public void setOdinConnectionStatus(EndpointConnectionStatus status) {
		this.odinConnectionStatus = status;
		update();
	}

	public void setBioharnessConnectionStatus(BHConnectivityStatus status) {
		this.bhConnectionStatus = status;
		update();
	}

	private void update() {
		if (imgOdin != null) {
			switch (odinConnectionStatus) {
			case ATTEMPTING_CONNECT:
				imgOdin.setImageResource(drawable.ic_connecting);
				break;
			case CONNECTED:
				imgOdin.setImageResource(drawable.ic_connected);
				break;
			case DISCONNECTED:
				imgOdin.setImageResource(drawable.ic_disconnected);
				break;
			}
		}
		if (imgBioharness != null) {
			switch (bhConnectionStatus) {
			case CONNECTED:
				imgBioharness.setImageResource(drawable.ic_bioharness_connected);
				break;
			case CONNECTING:
				imgBioharness.setImageResource(drawable.ic_bioharness_connecting);
				break;
			case DISCONNECTED:
				imgBioharness.setImageResource(drawable.ic_bioharness_disconnected);
				break;
			}
		}
	}
}
