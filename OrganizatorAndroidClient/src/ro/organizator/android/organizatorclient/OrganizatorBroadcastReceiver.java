package ro.organizator.android.organizatorclient;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

public class OrganizatorBroadcastReceiver extends BroadcastReceiver {

	static final String LOG_TAG = OrganizatorBroadcastReceiver.class.getName();

	@Override
	public void onReceive(Context context, Intent arg1) {

		ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		if(activeNetworkInfo == null || !activeNetworkInfo.isConnected()) {
			Log.d(LOG_TAG, "Nothing to do, there is no network connectivity");
			return;
		}

		// fetch the credentials
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
//		SharedPreferences settings = context.getApplicationContext().getSharedPreferences("Organizator", Context.MODE_PRIVATE);
		String username = settings.getString("username", "");
		String encryptedPassword = settings.getString("parola", "");

//		Debug.waitForDebugger();
		Log.i(LOG_TAG, "OI: at boot, get Organizator preferences, enc is: " + encryptedPassword);
		String password = "";
		if(!encryptedPassword.isEmpty()) {
			try {
				Log.d(LOG_TAG, "Decrypt the password for loggin in");
				password = BCCypher.decrypt(encryptedPassword, username);

				Log.d(LOG_TAG, "OI: Make intent to start OrganizatorMessagingService");
				// start the service
				Intent intent = new Intent(context, OrganizatorMessagingService.class);
				intent.putExtra("username", username);
				intent.putExtra("password", password);
				Log.i(LOG_TAG, "Starting Organizator service");
				context.startService(intent);

			} catch (Exception e) {
				Log.e(LOG_TAG, "OI: " + e);
				e.printStackTrace();
			}
		} else {
			Log.e(LOG_TAG, "OI: No password, Organizator service can not be started");
		}
	}
}
