package nz.ac.auckland.nihi.trainer.fragments;

import java.util.ArrayList;
import java.util.List;

import nz.ac.auckland.nihi.trainer.R.id;
import nz.ac.auckland.nihi.trainer.R.layout;
import nz.ac.auckland.nihi.trainer.R.string;
import nz.ac.auckland.nihi.trainer.hosts.NihiAppHosts;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

/**
 * Allows the selection of a host (i.e. testing or production).
 * 
 * @author Andrew Meads
 * 
 */
public class SelectHostDialogFragment extends DialogFragment {

	// private RadioButton rsdoTesting, rdoProduction, rdoAzure;
	private List<RadioButton> radioButtons = new ArrayList<RadioButton>();

	public interface SelectHostDialogFragmentListener {
		void onFinishSelectHost(NihiAppHosts host);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View root = inflater.inflate(layout.select_host_dialog, null);

		RadioGroup radioButtonGroup = (RadioGroup) root.findViewById(id.radioGroup1);

		// boolean checked = true;
		for (NihiAppHosts host : NihiAppHosts.values()) {

			RadioButton button = new RadioButton(getActivity());
			button.setText(host.getName());
			button.setTag(host);
			// button.setChecked(checked);
			// checked = false;
			radioButtonGroup.addView(button);
			this.radioButtons.add(button);

		}

		// rdoTesting = (RadioButton) root.findViewById(id.rdoTesting);
		// rdoProduction = (RadioButton) root.findViewById(id.rdoProduction);
		// rdoAzure = (RadioButton) root.findViewById(id.rdoAzure);

		root.findViewById(id.btnOk).setOnClickListener(btnOkListener);

		this.getDialog().setTitle(string.hostdialog_label_selecthost);

		return root;

	}

	private final View.OnClickListener btnOkListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {

			// NihiAppHosts host = rdoTesting.isChecked() ? NihiAppHosts.ODIN_CS_AUCKLAND_AC_NZ : rdoProduction
			// .isChecked() ? NihiAppHosts.METIS_CTRU_AUCKLAND_AC_NZ : NihiAppHosts.ODIN_CLOUDAPP_NET;
			NihiAppHosts host = null;
			for (RadioButton btn : radioButtons) {
				if (btn.isChecked()) {
					host = (NihiAppHosts) btn.getTag();
					break;
				}
			}

			if (host != null) {
				((SelectHostDialogFragmentListener) getActivity()).onFinishSelectHost(host);
				dismiss();
			}

		}
	};

}
