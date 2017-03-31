package nz.ac.auckland.nihi.trainer.fragments;

import nz.ac.auckland.nihi.trainer.R.id;
import nz.ac.auckland.nihi.trainer.R.layout;
import nz.ac.auckland.nihi.trainer.R.string;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

/**
 * A fragment that presents a dialog allowing the user to enter a username and password to gain access to the system.
 * 
 * @author Andrew Meads
 * 
 */
public class LoginDialogFragment extends DialogFragment {

	private String userName, password;

	private EditText txtUsername, txtPassword;

	private boolean finished = false;

	public interface LoginDialogFragmentListener {

		void onFinishLoginFragment(LoginDialogFragment dialog, boolean cancelled);

	}

	/**
	 * Creates the view and adds its listeners.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View root = inflater.inflate(layout.home_screen_login_dialog, null);

		txtUsername = (EditText) root.findViewById(id.txtUsername);
		txtPassword = (EditText) root.findViewById(id.txtPassword);

		root.findViewById(id.btnLogin).setOnClickListener(btnLoginClickListener);
		root.findViewById(id.btnCancel).setOnClickListener(btnCancelClickListener);

		getDialog().setTitle(string.logindialog_button_login);

		// Add a cancel-by-default listener
		getDialog().setOnDismissListener(new DialogInterface.OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				if (!finished) {
					finished = true;
					((LoginDialogFragmentListener) getActivity()).onFinishLoginFragment(LoginDialogFragment.this, true);
				}
			}
		});

		return root;
	}

	/**
	 * Gets the username entered by the user.
	 * 
	 * @return
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * Gets the password entered by the user.
	 * 
	 * @return
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * When the login button is clicked, sets the username and password, and dismisses the dialog.
	 */
	private final View.OnClickListener btnLoginClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			userName = txtUsername.getText().toString();
			password = txtPassword.getText().toString();
			finished = true;
			dismiss();
			((LoginDialogFragmentListener) getActivity()).onFinishLoginFragment(LoginDialogFragment.this, false);
		}
	};

	/**
	 * When the cancel button is clicked, dismisses the dialog with no result.
	 */
	private final View.OnClickListener btnCancelClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			finished = true;
			dismiss();
			((LoginDialogFragmentListener) getActivity()).onFinishLoginFragment(LoginDialogFragment.this, true);
		}
	};

}
