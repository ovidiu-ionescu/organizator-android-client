package ro.organizator.android.organizatorclient.fragments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ro.organizator.android.organizatorclient.Contact;
import ro.organizator.android.organizatorclient.DestinationDialogFragment;
import ro.organizator.android.organizatorclient.MessageListFragment;
import ro.organizator.android.organizatorclient.NotificationId;
import ro.organizator.android.organizatorclient.OrganizatorMessage;
import ro.organizator.android.organizatorclient.OrganizatorMessagingService;
import ro.organizator.android.organizatorclient.R;
import ro.organizator.android.organizatorclient.activity.MainActivity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class ChatFragment extends Fragment implements DestinationDialogFragment.DestinationDialogListener {

	public static final String LOG_TAG = ChatFragment.class.getName();

	private int ACTIVE_COLOR;
	private int IDLE_COLOR;
	private int OFFLINE_COLOR;
	
	private MessageListFragment messageListFragment;
	volatile List<Contact> contacts = new ArrayList<Contact>();
	private TextView destinationControl;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_chat, container, false);
		destinationControl = (TextView) rootView.findViewById(R.id.chat_selectDestinations);

		// register for notifications from the service
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(messageReceiver, new IntentFilter(OrganizatorMessagingService.DATA_RECEIVED));

		addSendKeystroke(rootView);

		// add the message list fragment
		messageListFragment = new MessageListFragment();
		messageListFragment.setDestinationListener(this);
		getChildFragmentManager().beginTransaction().replace(R.id.chat_messages_placeholder, messageListFragment).commit();
		
//		View.OnClickListener destinationListener = new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				Log.d(LOG_TAG, "Destination clicked");
//				openDestinationsDialog(v);
//			}
//		};
//		Log.d(LOG_TAG, "Add click listener for chat destination");
//		rootView.findViewById(R.id.chat_selectDestinations).setOnClickListener(destinationListener);
		
		registerForContextMenu(rootView.findViewById(R.id.chat_selectDestinations));

		View.OnClickListener sendListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(LOG_TAG, "Send clicked");
				send(v);
			}
		};
		Log.d(LOG_TAG, "Add click listener for chat destination");
		rootView.findViewById(R.id.chat_sender).setOnClickListener(sendListener);

		setHasOptionsMenu(true);

		// cache the colors
		ACTIVE_COLOR = getActivity().getResources().getColor(R.color.user_active_color);
		IDLE_COLOR = getActivity().getResources().getColor(R.color.user_idle_color);
		OFFLINE_COLOR = getActivity().getResources().getColor(R.color.user_offline_color);

		return rootView;
	}

	private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(LOG_TAG, "Received new data from organizatorMessagingService");
			if(null == getActivity()) {
				Log.e(LOG_TAG, "The activity is gone, this broadcast receiver should have been unsubscribed");
				return;
			}
			if(((MainActivity)getActivity()).serviceBound && ((MainActivity)getActivity()).organizatorMessagingService != null) {
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
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.chat_menu, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		switch (menuItem.getItemId()) {
		case R.id.chat_menu_patraulea:
			patrauleaDormi();
			return true;
		}
			
		return super.onOptionsItemSelected(menuItem);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);
		openDestinationsDialog(view);
	}

	private void patrauleaDormi() {
		((EditText) getView().findViewById(R.id.chat_compose_message)).setText("Patraulea, dormi?");
		send(null);
	}

	public void send(View view) {
		// add a message to the list
		EditText edit = (EditText)getView().findViewById(R.id.chat_compose_message);
		String text = edit.getText().toString();
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
			OrganizatorMessagingService organizatorMessagingService = ((MainActivity)getActivity()).organizatorMessagingService;

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
			EditText edit = (EditText)getView().findViewById(R.id.chat_compose_message);
			edit.setEnabled(true);
			if(result != null && result == 200) {
				edit.setText("");
			}
		}
	}

	@Override
	public void onDestroyView() {
		getChildFragmentManager().beginTransaction().remove(messageListFragment).commitAllowingStateLoss();
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(messageReceiver);
		super.onDestroyView();
	}

	@Override
	public void onResume() {
		super.onResume();
		OrganizatorMessagingService organizatorMessagingService = ((MainActivity)getActivity()).organizatorMessagingService;
		if(organizatorMessagingService != null) {
			organizatorMessagingService.setViewActive(true);
			updateMessagesAndContacts();
	
			NotificationManager nm = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
			nm.cancel(NotificationId.NEW_MESSAGE_RECEIVED);
		} else {
			Log.w(LOG_TAG, "OI: No service in resume");
		}
	}
	
// TODO the life cycle has to be resolved.
//	@Override
//	protected void onNewIntent(Intent intent) {
//		if(Intent.ACTION_SEND.equals(intent.getAction()) && intent.getType() != null) {
//			if ("text/plain".equals(intent.getType())) {
//				handleSendText(intent);
//			}
//		}
//	}
//
//	@Override
//	protected void onResume() {
//		super.onResume();
//		if(organizatorMessagingService != null) {
//			organizatorMessagingService.setViewActive(true);
//			updateMessagesAndContacts();
//
//			NotificationManager nm = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
//			nm.cancel(NotificationId.NEW_MESSAGE_RECEIVED);
//		} else {
//			Log.w(LOG_TAG, "OI: No service in resume");
//		}
//	}
//
//	@Override
//	public void onPause() {
//		super.onPause();
//		if(organizatorMessagingService != null) {
//			organizatorMessagingService.setViewActive(false);
//		}
//	}

	/**
	 * Update the list of messages on the screen
	 */
	private void updateMessages() {
		OrganizatorMessagingService organizatorMessagingService = ((MainActivity)getActivity()).organizatorMessagingService;
		if(organizatorMessagingService == null) return;

		List<OrganizatorMessage> messageList = organizatorMessagingService.getMessageList();
		synchronized(messageList) {
			messageListFragment.setMessages(messageList);
		}
	}

	private void updateContacts() {
		OrganizatorMessagingService organizatorMessagingService = ((MainActivity)getActivity()).organizatorMessagingService;
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
		getActivity().runOnUiThread(new Runnable() {
			public void run() {			
				Toast.makeText(getActivity(), error, Toast.LENGTH_LONG).show();
			}
		});
	}

	public void openDestinationsDialog(View view) {
		OrganizatorMessagingService organizatorMessagingService = ((MainActivity)getActivity()).organizatorMessagingService;
		DestinationDialogFragment destinationDialogFragment = new DestinationDialogFragment();
		// clone the contacts before sending them
		List<Contact> contacts = organizatorMessagingService.getContacts();
		List<Contact> copy = new ArrayList<Contact>(contacts.size());
		for(Contact c: contacts) {
			copy.add(c.clone());
		}
		destinationDialogFragment.setContacts(copy);
		destinationDialogFragment.setListener(this);

		destinationDialogFragment.show(getChildFragmentManager(), "destinationsFragment");
	}

	public void updateDestinationsOnScreen() {
		destinationControl.post(new Runnable() {
			public void run() {
				int color = OFFLINE_COLOR;
				SpannableStringBuilder sb = new SpannableStringBuilder();
				String sep = "";
				for (Contact contact : contacts) {
					if (contact.selected) {
						sb.append(sep);
						if(sep.length() > 0) {
							sb.setSpan(new ForegroundColorSpan(color), sb.length() - sep.length(), sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
						}
						sb.append(contact.name);
						int end = sb.length();
						// color the name according to online status
//						ClickableSpan span = new URLSpan("");
						ForegroundColorSpan span;
						if(contact.active) {
							if(contact.idle) {
								color = IDLE_COLOR;	
							} else {
								color = ACTIVE_COLOR;
							}
						} else {
							color = OFFLINE_COLOR;
						}
						span = new ForegroundColorSpan(color);
						Log.d(LOG_TAG, "Apply span at " + (end - contact.name.length()) + ", " + (end));
						sb.setSpan(span, end - contact.name.length(), end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
						
						sep = ", ";
					}
				}
				destinationControl.setText(sb);
			}
		});
	}

	@Override
	public void onDialogPositiveClick(List<Contact> contacts) {
		updateContacts(contacts, this.contacts);
	}

	@Override
	public void onDialogNegativeClick(List<Contact> contacts) {
		// don't do anything, keep the current destination selection
	}

	/**
	 * Adds the listener for the key combination that sends a message
	 */
	private void addSendKeystroke(View view) {
		EditText edit = (EditText)view.findViewById(R.id.chat_compose_message);
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
		EditText edit = (EditText)getView().findViewById(R.id.chat_compose_message);
		String text = edit.getText().toString();
		if(text.isEmpty()) {
			edit.setText(sharedText);
		} else {
			edit.setText(text + " " + sharedText);
		}
	}
}
