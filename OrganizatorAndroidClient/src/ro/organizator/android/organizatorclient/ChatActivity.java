package ro.organizator.android.organizatorclient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.util.Log;

public class ChatActivity extends ActionBarActivity implements DestinationDialogFragment.DestinationDialogListener {

	public static final String LOG_TAG = ChatActivity.class.getName();
	
	private MessageListFragment messageListFragment;
	private OrganizatorMessagingService organizatorMessagingService;
	private boolean serviceBound;
	volatile List<Contact> contacts = new ArrayList<Contact>();
	private Button destinationButton;
	GestureDetector gestureDetector;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);
		destinationButton = (Button) findViewById(R.id.selectDestinationsButton);

		messageListFragment = ((MessageListFragment) getSupportFragmentManager().findFragmentById(R.id.messages));
		registerForContextMenu(destinationButton);

		// connect to the service
		doBindService();

		// register for notifications from the service
		LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter(OrganizatorMessagingService.DATA_RECEIVED));
		gestureDetector = new GestureDetector(this, simpleOnGestureListener);

		addSendKeystroke();

	}

	private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(serviceBound && organizatorMessagingService != null) {
				if(intent.getBooleanExtra(OrganizatorMessagingService.UPDATED_MESSAGES, false)) {
					updateMessages();
				}
				if(intent.getBooleanExtra(OrganizatorMessagingService.UPDATED_CONTACTS, false)) {
					updateContacts();
				}
			}
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.d(LOG_TAG, "Inflate the menu; this adds items to the action bar if it is present.");
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_chat, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		switch (menuItem.getItemId()) {
		case R.id.menu_chat_search:
			gotoChatSearchActivity();
			return true;
		case R.id.menu_memo_search:
			gotoMemoSearchActivity();
			return true;
		case R.id.menu_settings:
			gotoSettingsActivity();
			return true;
		case R.id.menu_exit:
			exitApp();
			return true;
		}
			
		return super.onOptionsItemSelected(menuItem);
	}

	private void gotoChatSearchActivity() {
		Intent i = new Intent(this, ChatSearchActivity.class);
		startActivity(i);
	}

	private void gotoMemoSearchActivity() {
		Intent i = new Intent(this, MemoSearchActivity.class);
		startActivity(i);
	}

	private void gotoSettingsActivity() {
		Intent i = new Intent(this, OrganizatorSettingsActivity.class);
		startActivity(i);
	}

	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		menu.setHeaderTitle("Select Action");
		if(v.getId() == R.id.selectDestinationsButton) {
			menu.add(0, 5, 0, "Exit Application");
		} else {
			menu.add(1, 2, 0, "Show message with links");
		}
/*
		menu.setHeaderTitle("Select Destination(s)");
		menu.add(0, v.getId(), 0, "Online");
		menu.setGroupEnabled(0, false);

		menu.add(1, v.getId(), 0, "Action 1");
		menu.add(1, v.getId(), 0, "Action 2");
		menu.setGroupCheckable(1, true, false);

		menu.add(2, v.getId(), 0, "Idle");
		menu.setGroupEnabled(2, false);

		menu.add(4, v.getId(), 0, "Offline");
*/
	}

	public boolean onContextItemSelected(MenuItem item) {
		switch(item.getItemId()) {

		case 5:
			// Exit application
			Toast.makeText(this, "Exit App", Toast.LENGTH_SHORT).show();
			exitApp();
			break;
		}

//		if(item.getTitle().equals("Action 1")) {
//			Toast.makeText(this, "action 1", Toast.LENGTH_SHORT).show();
//		} else if(item.getTitle().equals("Action 1")) {
//			return false;
//		} else {	
//			return false;
//		}
		return true;
	}

	private void exitApp() {
		new AlertDialog.Builder(this)
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setTitle(R.string.exit_application)
        .setMessage(R.string.exit_application_are_you_sure)
        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                //Stop the activity
        		if(organizatorMessagingService != null) {
        			organizatorMessagingService.shutdown = true;
        			organizatorMessagingService.stopSelf();
        		}
        		ChatActivity.this.finish();
            }

        })
        .setNegativeButton(R.string.no, null)
        .show();
	}
	
	public void send(View view) {
		// add a message to the list
		EditText edit = (EditText)findViewById(R.id.compose_message);
		String text = edit.getText().toString();
//		edit.setText("");
		// disable the text editor, we'll enable it back after sending the message
		edit.setEnabled(false);
		OrganizatorMessage msg = new OrganizatorMessage(50, text, true);
	
		// gather the destinations
		List<String> dest = new ArrayList<String>();
		for(Contact contact: contacts) {
			if(contact.selected) {
				dest.add(contact.name);
			}
		}
		if(dest.isEmpty()) {
			return;
		}
		msg.to = dest.toArray(new String[dest.size()]);
		msg.joinedTo = TextUtils.join(", ", msg.to);

		new SendTask().execute(msg);
	}

	public class SendTask extends AsyncTask<OrganizatorMessage, Integer, Integer> {

		@Override
		protected Integer doInBackground(OrganizatorMessage... params) {
			OrganizatorMessage msg = params[0];
			organizatorMessagingService.lastSentMessage = msg;
			try {
				int code = organizatorMessagingService.postMessage(msg);
				if(code != 200) {
					notifyFailedToSendMessage(msg, "Return code: " + code);
				}
				return code;
			} catch (IOException e) {
				notifyFailedToSendMessage(msg, e.getMessage());
				Log.e(LOG_TAG, "Failed to send message", e);
			}

			return null;
		}

		@Override
		protected void onPostExecute(Integer result) {
			EditText edit = (EditText)findViewById(R.id.compose_message);
			edit.setEnabled(true);
			if(result != null && result == 200) {
				edit.setText("");
			}
		}
	}

	public void sendNotification(View view) {
		Intent intent = new Intent(this, ChatActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

		NotificationManager nm = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

		Resources res = this.getResources();
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

		builder.setContentIntent(contentIntent)
		            .setSmallIcon(R.drawable.tulip_bw)
//		            .setLargeIcon(BitmapFactory.decodeResource(res, R.drawable.tulip_bw))
		            .setTicker(res.getString(R.string.ticker))
		            .setWhen(System.currentTimeMillis())
		            .setAutoCancel(true)
		            .setContentTitle(res.getString(R.string.notification_title))
		            .setContentText("New message from friend");
		Notification n = builder.build();

		nm.notify(2, n);
		try {
	        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
	        Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
	        r.play();
	    } catch (Exception e) {}
	}

	private ServiceConnection serviceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
	        // This is called when the connection with the service has been
	        // established, giving us the service object we can use to
	        // interact with the service.  Because we have bound to a explicit
	        // service that we know is running in our own process, we can
	        // cast its IBinder to a concrete class and directly access it.
			organizatorMessagingService = ((OrganizatorMessagingService.LocalBinder)service).getService();
			Toast.makeText(ChatActivity.this, "Connected to service", Toast.LENGTH_SHORT).show();
			if(!organizatorMessagingService.isStarted()) {
				// go to the login activity
				try {
					Intent k = new Intent(getApplicationContext(), LoginActivity.class);
					startActivity(k);
				} catch(Exception e) {
					
				}
				finish();
			} else {
				updateMessagesAndContacts();
			}
		}

		public void onServiceDisconnected(ComponentName className) {
	        // This is called when the connection with the service has been
	        // unexpectedly disconnected -- that is, its process crashed.
	        // Because it is running in our same process, we should never
	        // see this happen.
			organizatorMessagingService = null;
			Toast.makeText(ChatActivity.this, "Disconnected from service", Toast.LENGTH_SHORT).show();
		}
	};	
	
	void doBindService() {
		bindService(new Intent(ChatActivity.this, OrganizatorMessagingService.class), serviceConnection, Context.BIND_AUTO_CREATE);
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

	@Override
	protected void onNewIntent(Intent intent) {
		if(Intent.ACTION_SEND.equals(intent.getAction()) && intent.getType() != null) {
			if ("text/plain".equals(intent.getType())) {
				handleSendText(intent);
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if(organizatorMessagingService != null) {
			organizatorMessagingService.setViewActive(true);
			updateMessagesAndContacts();

			NotificationManager nm = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
			nm.cancel(NotificationId.NEW_MESSAGE_RECEIVED);
		} else {
			Log.w(LOG_TAG, "OI: No service in resume");
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if(organizatorMessagingService != null) {
			organizatorMessagingService.setViewActive(false);
		}
	}

	/**
	 * Update the list of messages on the screen
	 */
	private void updateMessages() {
		if(organizatorMessagingService == null) return;

		List<OrganizatorMessage> messageList = organizatorMessagingService.getMessageList();
		synchronized(messageList) {
			messageListFragment.setMessages(messageList);
		}
	}

	private void updateContacts() {
		if(organizatorMessagingService == null) return;
		List<Contact> oldContacts = contacts;
		List<Contact> newContacts = organizatorMessagingService.getContacts();
		updateContacts(oldContacts, newContacts);
	}

	synchronized private void updateContacts(List<Contact> oldContacts, List<Contact> newContacts) {
		// now transfer the selection
		for(Contact newContact: newContacts) {
			for(Contact oldContact: oldContacts) {
				if(newContact.id == oldContact.id) {
					newContact.selected = oldContact.selected;
					break;
				}
			}
		}
		contacts = newContacts;
		updateDestinationsOnScreen();
	}

	public void setDestination(String[] destination) {
		for(Contact contact: contacts) {
			contact.selected = false;
			for(String dest: destination) {
				if(contact.name.equals(dest)) {
					contact.selected = true;
					break;
				}
			}
		}
		updateDestinationsOnScreen();
	}

	private void updateMessagesAndContacts() {
		updateMessages();
		updateContacts();
	}

	private void notifyFailedToSendMessage(OrganizatorMessage msg, final String error) {
		ChatActivity.this.runOnUiThread(new Runnable() {
			public void run() {			
				Toast.makeText(ChatActivity.this, error, Toast.LENGTH_LONG).show();
			}
		});
	}

	public void openDestinationsDialog(View view) {
		DestinationDialogFragment destinationDialogFragment = new DestinationDialogFragment();
		// clone the contacts before sending them
		List<Contact> contacts = organizatorMessagingService.getContacts();
		List<Contact> copy = new ArrayList<Contact>(contacts.size());
		for(Contact c: contacts) {
			copy.add(c.clone());
		}
		destinationDialogFragment.setContacts(copy);

		destinationDialogFragment.show(getSupportFragmentManager(), "destinationsFragment");
	}

	public void updateDestinationsOnScreen() {
		destinationButton.post(new Runnable() {
			public void run() {
				StringBuilder sb = new StringBuilder();
				String sep = "";
				for (Contact contact : contacts) {
					if (contact.selected) {
						sb.append(sep).append(contact.name);
						sep = ", ";
					}
				}
				destinationButton.setText(sb);
			}
		});
	}

	@Override
	public void onDialogPositiveClick(List<Contact> contacts) {
		updateContacts(contacts, this.contacts);
	}

	@Override
	public void onDialogNegativeClick(List<Contact> contacts) {
		// TODO Auto-generated method stub
		
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

			Log.d(LOG_TAG, "onFling: \n" + e1.toString() + "\n" + e2.toString() + "\n" + "velocityX= "
					+ String.valueOf(velocityX) + "\n" + "velocityY= " + String.valueOf(velocityY) + "\n" + "orizontal=" + (e1.getX() - e2.getX()));
			if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
				return true;
			// right to left swipe
			if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
//				Toast.makeText(ChatActivity.this, "Search Chat", Toast.LENGTH_SHORT).show();
				gotoChatSearchActivity();
				return true;
			} else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
//				Toast.makeText(ChatActivity.this, "Search Memos", Toast.LENGTH_SHORT).show();
				gotoMemoSearchActivity();
				return true;
			}

			return super.onFling(e1, e2, velocityX, velocityY);
		}

	};

	/**
	 * Adds the listener for the key combination that sends a message
	 */
	private void addSendKeystroke() {
		EditText edit = (EditText)findViewById(R.id.compose_message);
		edit.setOnKeyListener(new View.OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if((event.getMetaState() & KeyEvent.META_CTRL_ON) != 0 && keyCode == KeyEvent.KEYCODE_ENTER) {
					send(null);
					return true;
				}
				return false;
			}
		});
	}

	/**
	 * Handles text send from other applications to this one
	 * @param intent
	 */
	private void handleSendText(Intent intent) {
		String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
		EditText edit = (EditText)findViewById(R.id.compose_message);
		String text = edit.getText().toString();
		if(text.isEmpty()) {
			edit.setText(sharedText);
		} else {
			edit.setText(text + " " + sharedText);
		}
	}
}
