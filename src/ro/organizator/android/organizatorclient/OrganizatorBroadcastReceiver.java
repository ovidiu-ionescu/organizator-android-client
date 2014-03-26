package ro.organizator.android.organizatorclient;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class OrganizatorBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent arg1) {

		// fetch the credentials
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		String username = settings.getString("username", "");
		String encryptedPassword = settings.getString("parola", "");

		String password = "";
		if(!encryptedPassword.isEmpty()) {
			try {
				password = Cypher.decrypt("LoginActivity", encryptedPassword);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// start the service
		Intent intent = new Intent(context, OrganizatorMessagingService.class);
		intent.putExtra("username", username);
		intent.putExtra("password", password);
		context.startService(intent);

	}

}
