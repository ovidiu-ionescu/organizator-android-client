package ro.organizator.android.organizatorclient;

import java.io.IOException;
import java.util.List;

import org.json.JSONException;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.widget.EditText;

public class ChatSearchActivity extends FragmentActivity {
	
	static final String LOG_TAG = ChatSearchActivity.class.getName();

	private OrganizatorMessagingService organizatorMessagingService;
	private boolean serviceBound;
	GestureDetector gestureDetector;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chatsearch);

		// connect to the service
		doBindService();
		gestureDetector = new GestureDetector(this, simpleOnGestureListener);;

	}

	public void search(View view) {
		EditText edit = (EditText)findViewById(R.id.search_criteria);
		String text = edit.getText().toString();

		// disable the text editor, we'll enable it back after sending the message
		edit.setEnabled(false);

		new SearchTask().execute(text);
	}

	public class SearchTask extends AsyncTask<String, Integer, Integer> {

		@Override
		protected Integer doInBackground(String... params) {
			String criteria = params[0];
			try {
				final List<OrganizatorMessage> messages = organizatorMessagingService.searchMessages(criteria);
				runOnUiThread(new Runnable() {
					public void run() {
						((MessageListFragment) getSupportFragmentManager().findFragmentById(R.id.messages)).setMessages(messages);
					}
				});
			} catch (IOException e) {
				Log.e(LOG_TAG, "Search failed", e);
			} catch (JSONException e) {
				Log.e(LOG_TAG, "Search failed", e);
			}

			return null;
		}

		@Override
		protected void onPostExecute(Integer result) {
			EditText edit = (EditText)findViewById(R.id.search_criteria);
			edit.setEnabled(true);
// TODO put the search results in the list
		
		}
	}

	void doBindService() {
		bindService(new Intent(ChatSearchActivity.this, OrganizatorMessagingService.class), serviceConnection, Context.BIND_AUTO_CREATE);
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
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			organizatorMessagingService = null;
		}
	};	

	private void gotoChatActivity() {
		Intent i = new Intent(this, ChatActivity.class);
		startActivity(i);
	}

	/**
	 * The swipe on the action takes precedence. This is the only place to intercept it reliably.
	 */
	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		boolean ret = gestureDetector.onTouchEvent(event); 
		if(!ret) {
			return super.dispatchTouchEvent(event);
		}
		return ret;
	}

	SimpleOnGestureListener simpleOnGestureListener = new SimpleOnGestureListener() {

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			float SWIPE_MAX_OFF_PATH = 50, SWIPE_MIN_DISTANCE = 50, SWIPE_THRESHOLD_VELOCITY = 200;
			
			if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
				return false;
			// right to left swipe
			if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
			} else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
				gotoChatActivity();
				return true;
			}

			return super.onFling(e1, e2, velocityX, velocityY);
		}

	};

}
