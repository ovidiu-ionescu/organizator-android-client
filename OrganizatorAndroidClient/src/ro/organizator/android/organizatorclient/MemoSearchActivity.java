package ro.organizator.android.organizatorclient;

import java.io.IOException;
import java.util.List;

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
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

public class MemoSearchActivity extends Activity {

	static final String LOG_TAG = MemoSearchActivity.class.getName();
	
	private OrganizatorMessagingService organizatorMessagingService;
	private boolean serviceBound;
	GestureDetector gestureDetector;
	ListView memoHitsList;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_memosearch);

		// connect to the service
		doBindService();

		memoHitsList = (ListView) findViewById(R.id.memo_hits);

		memoHitsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
				final MemoHit memoHit = (MemoHit) parent.getItemAtPosition(position);
				Intent intent = new Intent(MemoSearchActivity.this, MemoActivity.class);
				intent.putExtra("memo_id", memoHit.id);
				startActivity(intent);
			}

		});
		
		gestureDetector = new GestureDetector(this, simpleOnGestureListener);
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
				final List<MemoHit> memoHits = organizatorMessagingService.searchMemos(criteria);
				runOnUiThread(new Runnable() {
					public void run() {
						memoHitsList.setAdapter(new MemoSearchArrayAdapter(MemoSearchActivity.this, R.layout.memo_hit_item, memoHits));
					}
				});
			} catch (IOException e) {
				Log.e(LOG_TAG, "Failed to search memo", e);
			} catch (JSONException e) {
				Log.e(LOG_TAG, "Failed to search memo", e);
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
		bindService(new Intent(MemoSearchActivity.this, OrganizatorMessagingService.class), serviceConnection, Context.BIND_AUTO_CREATE);
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

	private class MemoSearchArrayAdapter extends ArrayAdapter<MemoHit> {

		public MemoSearchArrayAdapter(Context context, int textViewResourceId, List<MemoHit> objects) {
			super(context, textViewResourceId, objects);
		}

		@Override
		public long getItemId(int position) {
			return getItem(position).id;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

	}

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
				gotoChatActivity();
				return true;
			} else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
			}

			return super.onFling(e1, e2, velocityX, velocityY);
		}

	};

}
