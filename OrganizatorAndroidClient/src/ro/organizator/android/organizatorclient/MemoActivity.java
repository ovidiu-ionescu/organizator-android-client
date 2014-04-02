package ro.organizator.android.organizatorclient;

import java.io.IOException;

import org.json.JSONException;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.EditText;

public class MemoActivity extends Activity {
	
	static final String LOG_TAG = MemoActivity.class.getName();

	private OrganizatorMessagingService organizatorMessagingService;
	private boolean serviceBound;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		setContentView(R.layout.activity_memo);

		// connect to the service
		doBindService();
		super.onCreate(savedInstanceState);
	}

	private void displayMemo() throws IOException, JSONException {
		Long memoId = getIntent().getLongExtra("memo_id", 0);
		Log.d(LOG_TAG, "Memo Id " + memoId);
		final EditText edit = (EditText) findViewById(R.id.memo);

		final Memo memo = organizatorMessagingService.fetchMemo(memoId);
		runOnUiThread(new Runnable() {
			public void run() {
				edit.setText(memo.memotext);
			}
		});
	}

	public class FetchMemoTask extends AsyncTask<String, Integer, Integer> {

		@Override
		protected Integer doInBackground(String... params) {
			try {
				displayMemo();
			} catch (IOException e) {
				Log.e(LOG_TAG, "Failed to fetch memo", e);
			} catch (JSONException e) {
				Log.e(LOG_TAG, "Failed to search memo", e);
			}

			return null;
		}

		@Override
		protected void onPostExecute(Integer result) {
		}
	}

	void doBindService() {
		bindService(new Intent(MemoActivity.this, OrganizatorMessagingService.class), serviceConnection, Context.BIND_AUTO_CREATE);
		serviceBound = true;
	}

	void doUnbindService() {
		if (serviceBound) {
			// Detach our existing connection.
			unbindService(serviceConnection);
			serviceBound = false;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		doUnbindService();
	}

	private ServiceConnection serviceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			organizatorMessagingService = ((OrganizatorMessagingService.LocalBinder)service).getService();
			if(!organizatorMessagingService.isStarted()) {
				// go to the login activity
				try {
					Intent k = new Intent(getApplicationContext(), LoginActivity.class);
					startActivity(k);
				} catch(Exception e) {
					
				}
				finish();
			} else {
				new FetchMemoTask().execute();
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			organizatorMessagingService = null;
		}
	};	
}
