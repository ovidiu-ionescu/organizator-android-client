package ro.organizator.android.organizatorclient;

import java.io.IOException;
import android.widget.CheckBox;

import org.apache.http.client.ClientProtocolException;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends Activity {

	private EditText mUserView;
	private EditText mPasswordView;
	private CheckBox mToggleNumericPassword;

	private View mLoginFormView;
	private View mLoginStatusView;
	private TextView mLoginStatusMessageView;
	private volatile String lastUserName;
	private volatile boolean lastNumericPassword;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);

		mUserView = (EditText) findViewById(R.id.user);
		mPasswordView = (EditText) findViewById(R.id.password);
		mToggleNumericPassword = (CheckBox) findViewById(R.id.toggleNumericPassword);

		findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				attemptLogin();
			}
		});

		mLoginFormView = findViewById(R.id.login_form);
		mLoginStatusView = findViewById(R.id.login_status);
		mLoginStatusMessageView = (TextView) findViewById(R.id.login_status_message);

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		lastUserName = settings.getString("username", "");
		String encryptedPassword = settings.getString("parola", "");
		if(!encryptedPassword.isEmpty()) {
			try {
				mPasswordView.setText(Cypher.decrypt("LoginActivity", encryptedPassword));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		boolean lastNumericPassword = settings.getBoolean("numericPassword", false);
		mToggleNumericPassword.setChecked(lastNumericPassword);
		toggleNumericPassword(mToggleNumericPassword);
		mUserView.setText(lastUserName);
		if(lastUserName.length() > 0) {
			mPasswordView.requestFocus();
		}
	}

	public void attemptLogin() {
		showProgress(true);
		String password = mPasswordView.getText().toString();
		String username = mUserView.getText().toString();
		new LoginTask().execute(username, password);
//		new LoginTask().execute("username", "password");
	}

	class LoginTask extends AsyncTask<String, Integer, Integer> {

		@Override
		protected Integer doInBackground(String... params) {
			try {
				return OrganizatorMessagingService.login(params[0], params[1]);
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}

		protected void onPostExecute(Integer responseCode) {
			showProgress(false);
			if(responseCode == null) {
				alert("Could not connect, response code is null");
				return;
			}
			switch(responseCode) {
			case 200:
				alert("Login failed");
				mPasswordView.setError(getString(R.string.error_incorrect_username_or_password));
				mPasswordView.requestFocus();
				break;
			case 302:
				alert("Login successfull");

				// save the user name for next time
				String username = mUserView.getText().toString();
				boolean numericPassword = mToggleNumericPassword.isChecked();
				if(!username.equals(lastUserName) || lastNumericPassword != numericPassword) {
					SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
					SharedPreferences.Editor editor = settings.edit();
					editor.putString("username", username);
					editor.putBoolean("numericPassword", numericPassword);
					try {
						editor.putString("parola", Cypher.encrypt("LoginActivity", mPasswordView.getText().toString()));
					} catch (Exception e) {
						e.printStackTrace();
					}
					editor.apply();
				}

				// start the service
				Intent intent = new Intent(getApplicationContext(), OrganizatorMessagingService.class);
				intent.putExtra("username", username);
				intent.putExtra("password", mPasswordView.getText().toString());
				startService(intent);

				// start the chat activity
				try {
					Intent k = new Intent(getApplicationContext(), ChatActivity.class);
					startActivity(k);
				} catch(Exception e) {
					
				}
				finish();
				break;
			default:
				alert("Login failed: " + responseCode);
			}
		}
	}


	private void alert(CharSequence text) {
		int duration = Toast.LENGTH_LONG;
		Toast toast = Toast.makeText(getApplicationContext(), text, duration);
		toast.show();
	}

	// copied from the Google example

	/**
	 * Shows the progress UI and hides the login form.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	private void showProgress(final boolean show) {
		// On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
		// for very easy animations. If available, use these APIs to fade-in
		// the progress spinner.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

			mLoginStatusView.setVisibility(View.VISIBLE);
			mLoginStatusView.animate().setDuration(shortAnimTime).alpha(show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
				}
			});

			mLoginFormView.setVisibility(View.VISIBLE);
			mLoginFormView.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
				}
			});
		} else {
			// The ViewPropertyAnimator APIs are not available, so simply show
			// and hide the relevant UI components.
			mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
			mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
		}
	}

	public void toggleNumericPassword(View view) {
		boolean checked = ((CheckBox) view).isChecked();
		if(checked) {
			mPasswordView.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
		} else {
			mPasswordView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		}
	}
}