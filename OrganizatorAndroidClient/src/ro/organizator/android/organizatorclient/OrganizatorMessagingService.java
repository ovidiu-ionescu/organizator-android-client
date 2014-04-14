package ro.organizator.android.organizatorclient;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

public class OrganizatorMessagingService extends IntentService implements OnSharedPreferenceChangeListener {

	public static final String LED_NOTIFICATION = "led_notification";
	public static final String LED_NOTIFICATION_COLOR = "led_notification_color";
	public static final String LAST_NOTIFICATION_TO_SONY_WATCH = "last_notification_to_sony_watch";
	public static final String SEND_NOTIFICATIONS_TO_SONY_WATCH = "send_notifications_to_sony_watch";
	public static final String NOTIFICATIONS_NEW_MESSAGE = "notifications_new_message";
	public static final String NOTIFICATIONS_NEW_MESSAGE_VIBRATE = "notifications_new_message_vibrate";
	public static final String NOTIFICATIONS_NEW_MESSAGE_RINGTONE = "notifications_new_message_ringtone";
	public static final String NOTIFICATIONS_SERVICE_RUNNING = "notifications_service_running";

	private static final String LOG_TAG = OrganizatorMessagingService.class.getName() + " OI:";
	
	private static final String ORGANIZATOR_CLIENT_MESSAGING_SERVICE_IS_RUNNING = "Organizator Client Messaging Service is Running.";
	private static final String ORGANIZATOR_CLIENT_TICKER = "Organizator Client";
	public static final String UPDATED_CONTACTS = "updatedContacts";
	public static final String UPDATED_MESSAGES = "updatedMessages";
	volatile boolean started;
	volatile boolean viewActive;
	volatile public boolean shutdown;

	private CharSequence username;
	private CharSequence password;

	/*
	 * Options corresponding to preferences
	 */
	volatile boolean newMessageNotify = true;
	volatile boolean serviceStatusNotification = false;
	volatile String newMessageRingTone = "";
	volatile boolean vibrateEnabled = false;
	volatile boolean notifySonyWatch = true;
	volatile long lastMessageIdForWearable = 0;
	volatile boolean ledNotification = true;
	volatile int ledNotificationColor = 0xFFFF0000;
	
	boolean loggingEnabled = false;
	boolean devMessages = false;

	public static final String DATA_RECEIVED = "data-received";

	public static final String ORGANIZATOR_URL = "https://ionescu.net/organizator/";

	public static final String USERAGENT = "OrganizatorClient/1.0 (Android)";

	volatile List<Contact> contactList = new ArrayList<Contact>();
	Map<String, Contact> contacts = new HashMap<String, Contact>();
	int contactCounter;
	static CharSequence separator = "\n--endofsection";

	public volatile OrganizatorMessage lastSentMessage;
	
	private long lastProcessedId;
	long lastServerTime;
	volatile List<OrganizatorMessage> messages = new ArrayList<OrganizatorMessage>();

	public OrganizatorMessagingService() {
		super("OrganizatorMessagingService");
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(LOG_TAG, "Register for preference changes");
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		// read some preferences
		serviceStatusNotification = sharedPreferences.getBoolean(NOTIFICATIONS_SERVICE_RUNNING, true);
		newMessageRingTone = sharedPreferences.getString(NOTIFICATIONS_NEW_MESSAGE_RINGTONE, "");
		vibrateEnabled = sharedPreferences.getBoolean(NOTIFICATIONS_NEW_MESSAGE_VIBRATE, false);
		newMessageNotify = sharedPreferences.getBoolean(NOTIFICATIONS_NEW_MESSAGE, true);
		notifySonyWatch = sharedPreferences.getBoolean(SEND_NOTIFICATIONS_TO_SONY_WATCH, false);
		String lastMessageIdForWearableString = sharedPreferences.getString(LAST_NOTIFICATION_TO_SONY_WATCH, "0");
		lastMessageIdForWearable = lastMessageIdForWearableString.isEmpty() ? 0 : Long.parseLong(lastMessageIdForWearableString, 10);
		ledNotification = sharedPreferences.getBoolean(LED_NOTIFICATION, true);
		ledNotificationColor = sharedPreferences.getInt(LED_NOTIFICATION_COLOR, 0xFFFF0000);
		
		sharedPreferences.registerOnSharedPreferenceChangeListener(this);
	}
	
	@Override 
	public void onDestroy() {
		super.onDestroy();
		Log.d(LOG_TAG, "Unregister for preference changes");
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putString(LAST_NOTIFICATION_TO_SONY_WATCH, Long.toString(lastMessageIdForWearable, 10));
		editor.apply();
		sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);			
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(LOG_TAG, "Intent in OrganizatorMessagingService");
		try {
			log("Handling the service intent");
			username = intent.getCharSequenceExtra("username");
			password = intent.getCharSequenceExtra("password");
			intent.getBooleanExtra("devmessages", false);
			started = true;
			fg();
			int retries = 0;
			for(;!shutdown;) {
				try {
					if(retries > 0) {
						if(devMessages) {
							notifyError("Retrying to connect " + retries, "");
						} else {
							if(retries == 1) {
								notifyError("Retrying to connect", "");
							}
						}
					}
					if(isNetworkAvailable()) {
						processIncomingMessages();
						retries = 0;
					} else {
						retries++;
						if(retries > 7) {
							Log.e(LOG_TAG, "Too much waiting, stopping the service until network is back");
							stopSelf();
							return;
						}
						Log.d(LOG_TAG, "No network available, go to sleep");
						waitForBetterConnectivity(retries);
					}
					removeNotification(NotificationId.ERROR_RECEIVING_MESSAGES);
				} catch(IOException ioe) {
					Log.e(LOG_TAG, "Error fetching messages: ",  ioe);
					retries++;
					waitForBetterConnectivity(retries);
				}
			}
			stopSelf();
		} catch (JSONException e) {
			Log.e(LOG_TAG, "Problem with server Json", e);
		}
	}

	private void waitForBetterConnectivity(int retries) {
		try {
			long sleep = 60;
			if(retries < 6) {
				sleep = 1 << (retries - 1);
			}
			Log.d(LOG_TAG, "Sleep for " + sleep + " seconds");
			Thread.sleep(sleep * 1000);
		} catch (InterruptedException e) {
			Log.e(LOG_TAG, "Thread interrupted " + e);
		}
	}

	private void processIncomingMessages() throws IOException, JSONException {

		URL url = new URL(ORGANIZATOR_URL + "xchat/listMessages");
		HttpsURLConnection conn = prepareConnection(url);
		conn.setRequestProperty("X-Requested-With", "XMLHttpRequest");
		if(lastServerTime > 0) {
			conn.setRequestProperty("If-Modified-Since", Long.toString(lastServerTime, 10));
		}
		conn.setRequestMethod("GET");
		conn.connect();

		int responseCode = conn.getResponseCode();
		log("Server replied: " + responseCode);
		if(responseCode == 401) {
			Log.e(LOG_TAG, "OI: Need to login again");
			conn.disconnect();
			lastProcessedId = 0;
			lastServerTime = 0;
			login(username, password);
			return;
		}
		if(responseCode != 200) {
			return;
		}
		StringBuilder sb = new StringBuilder();
		Reader reader = new InputStreamReader(conn.getInputStream(), "UTF-8");
		for(int notification = 0; fetchServerMessages(reader, sb); notification++) {
			log("Received from server " + sb.length() + " characters");
			log(sb.toString().replace('\n', ' ').replace('\r', ' '));
			log("SEPARATOR " + notification);
			alert(sb.toString());

			processServerMessages(sb.toString());
			removeNotifyError();
			sb.setLength(0);
		}
		conn.disconnect();
	}

	private void alert(CharSequence text) {
		if(!viewActive) return;
		int duration = Toast.LENGTH_LONG;
		Toast toast = Toast.makeText(getApplicationContext(), text, duration);
		toast.show();
	}

	public class LocalBinder extends Binder {
		public OrganizatorMessagingService getService() {
			return OrganizatorMessagingService.this;
		}
	}

	private final IBinder mBinder = new LocalBinder();

	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	private static boolean fetchServerMessages(Reader reader, StringBuilder sb) throws IOException {
		int cur = 0;
		int end = separator.length();
		int c = -1;
		for(c = reader.read(); c != -1; c = reader.read()) {
			sb.append((char) c);
			// convert to the same line end
			if(c == '\r') {
				c = '\n';
			}
			
			if(cur > 0 && separator.charAt(cur) != c) {
				cur = 0;
			}
			if(separator.charAt(cur) == c) {
				cur ++;
			}
			if(cur == end) {
				// trim the separator
				sb.setLength(sb.length() - end);
				return true;
			}
		}
		return false;
	}

	private void processServerMessages(String jsonString) throws JSONException {
		long receivedMessages = 0;
		boolean updatedMessages = false;
		OrganizatorMessage lastReceivedMessage = null;
		JSONObject jsonObject = new JSONObject(jsonString);
		JSONArray jsonMessages = jsonObject.getJSONArray("messages");
		synchronized(this.messages) {
			for(int i = 0; i < jsonMessages.length(); i++) {
				JSONObject jsonMsg = jsonMessages.getJSONObject(i);
				long id = jsonMsg.getLong("id");
				if(lastProcessedId >= id) {
					continue;
				}
				updatedMessages = true;
				lastProcessedId = id;
				OrganizatorMessage msg = new OrganizatorMessage();
				msg.id = id;
				msg.time = jsonMsg.getLong("time");
				msg.text = jsonMsg.getString("text");
				msg.from = jsonMsg.getString("from");
				if(!jsonMsg.isNull("self")) {
					msg.self = jsonMsg.getBoolean("self");
				}
				JSONArray jsonTo = jsonMsg.getJSONArray("to");
				String[] to = new String[jsonTo.length()];
				for(int j = 0; j < jsonTo.length(); j++) {
					to[j] = jsonTo.getString(j);
				}
				msg.to = to;
				msg.joinedTo = TextUtils.join(", ", msg.to);
				// TODO: also process the mainTo
	
				messages.add(msg);
				if(!msg.self) {
					receivedMessages++;
					lastReceivedMessage = msg;
					// send the message to the wearable, if present
					notifyMessageWearable(msg);
				}
			}
			if(lastServerTime > 0) {
				if(receivedMessages == 1) {
					notifyNewMessage(lastReceivedMessage.from, lastReceivedMessage.text, lastReceivedMessage.time);
				}
				if(receivedMessages > 1) {
					notifyNewMessage(receivedMessages  + " new messages in Organizator", "", lastReceivedMessage.time);
				}
			}
		}
		// now the contacts
		boolean updatedContacts = false;
		long serverTime = jsonObject.getLong("serverTime");
		JSONObject jsonContacts = jsonObject.getJSONObject("contacts");
		for (@SuppressWarnings("unchecked") Iterator<String> i = jsonContacts.keys(); i.hasNext();) {
			String contactName = i.next();
			Contact contact = new Contact(contactName);
			// active, check
			JSONObject jsonContact = jsonContacts.getJSONObject(contactName);
			if(!jsonContact.isNull("active")) {
				contact.active = jsonContact.getBoolean("active");
			}
			if(!jsonContact.isNull("check")) {
				contact.check = jsonContact.getLong("check");
			}
			if(contact.active && serverTime - contact.check > 15000) {
				contact.idle = true;
			}
			if(contactName.contains("@")) {
				contact.external = true;
			}

			Contact existing = contacts.get(contact.name);
			if(existing != null) {
				if(existing.active != contact.active || existing.check != contact.check || existing.idle != contact.idle || existing.agent != contact.agent) {
					updatedContacts = true;
					existing.active = contact.active;
					existing.check = contact.check;
					existing.idle = contact.idle;
					existing.agent = contact.agent;
				}
			} else {
				updatedContacts = true;
				contact.id = ++contactCounter;
				contacts.put(contactName, contact);
			}
		}
	
		lastServerTime = serverTime;
		if(updatedContacts) {
			List<Contact> contactList = new ArrayList<Contact>(contacts.values());
		
			Collections.sort(contactList, new Comparator<Contact>() {
				@Override
				public int compare(Contact c1, Contact c2) {
					if(c1.external != c2.external) {
						return c2.external ? -1 : 1; 
					}
					if(c1.check != c2.check) {
						return c1.check < c2.check ? 1 : -1;
					}
					return c1.name.compareTo(c2.name);
				}
			});
			this.contactList = contactList;
		}

		notifyActivities(updatedMessages, updatedContacts);

	}

	public List<OrganizatorMessage> getMessageList() {
		return messages;
	}

	public List<Contact> getContacts() {
		return contactList;
	}

	public boolean isStarted() {
		return started;
	}

	/**
	 * Associate the service notifications with the chat activity.
	 * Tell the system the Organizator service is important, a foreground service.
	 */
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN) 
	private void fg() {
		Context ctx = getApplicationContext();
		Intent intent = new Intent(OrganizatorMessagingService.this, ChatActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx);

		builder.setContentIntent(contentIntent)
					.setSmallIcon(R.drawable.tulip_bw)
//		            .setLargeIcon(BitmapFactory.decodeResource(res, R.drawable.tulip_bw))
					.setTicker(ORGANIZATOR_CLIENT_TICKER)
					.setWhen(System.currentTimeMillis())
					.setContentTitle(ORGANIZATOR_CLIENT_TICKER)
					.setContentText(ORGANIZATOR_CLIENT_MESSAGING_SERVICE_IS_RUNNING);
		if(!serviceStatusNotification) {
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
				builder.setPriority(Notification.PRIORITY_MIN);
			}
		}
		Notification notification = builder.build();
		startForeground(NotificationId.ORGANIZATOR_RUNNING, notification);
	}

	public void notifyNewMessage(String ticker, String content, long time) {
		if(!newMessageNotify) {
			return;
		}

		if(viewActive) {
			try {
				if(!newMessageRingTone.isEmpty()) {
					Uri notificationSound = Uri.parse(newMessageRingTone);
					Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notificationSound);
					r.play();
				}
				if(vibrateEnabled) {
					Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
					vibrator.vibrate(300);
				}
			} catch (Exception e) {
				Log.e(LOG_TAG, "Failed to make noise", e);
			}
			return;
		}

		Context ctx = getApplicationContext();
		Intent intent = new Intent(ctx, ChatActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx);

		builder.setContentIntent(contentIntent)
					.setSmallIcon(R.drawable.tulip_bw)
//		            .setLargeIcon(BitmapFactory.decodeResource(res, R.drawable.tulip_bw))
					.setTicker(ticker)
					.setWhen(System.currentTimeMillis())
					.setAutoCancel(true)
					.setContentTitle(ticker)
					.setContentText(content)
					;

		if(!newMessageRingTone.isEmpty()) {
			Uri notificationSound = Uri.parse(newMessageRingTone);
			builder.setSound(notificationSound);
		}
		long[] vibratePattern = { 0, 300 }; 
		if(vibrateEnabled) {
			builder.setVibrate(vibratePattern);
		}
		if(ledNotification) {
			builder.setLights(ledNotificationColor, 500, 500);
		}
		Notification n = builder.build();

		nm.notify(NotificationId.NEW_MESSAGE_RECEIVED, n);
	}

	/**
	 * Sends the message to the service that will display it on the watch
	 * @param msg
	 */
	private void notifyMessageWearable(OrganizatorMessage msg) {
		if(msg.id <= lastMessageIdForWearable) {
			// message is old
			return;
		}
		lastMessageIdForWearable = msg.id;

		if(!notifySonyWatch) {
			return;
		}
        Intent serviceIntent = new Intent();
        serviceIntent.setClassName("com.sonymobile.smartconnect.extension.notificationsample", "com.sonymobile.smartconnect.extension.notificationsample.SampleExtensionService");
        serviceIntent.setAction("Organizator");
        serviceIntent.putExtra("FROM", msg.from);
        serviceIntent.putExtra("TO", msg.joinedTo);
        serviceIntent.putExtra("TEXT", msg.text);
        serviceIntent.putExtra("TIME", msg.time);
        
        startService(serviceIntent);
	}

	private void notifyError(CharSequence ticker, CharSequence content) {
		putNotification(ticker, content, NotificationId.ORGANIZATOR_RUNNING, R.drawable.tulip_empty_bw, !serviceStatusNotification);
	}

	private void removeNotifyError() {
		putNotification(ORGANIZATOR_CLIENT_TICKER, ORGANIZATOR_CLIENT_MESSAGING_SERVICE_IS_RUNNING, NotificationId.ORGANIZATOR_RUNNING, R.drawable.tulip_bw, !serviceStatusNotification);
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN) 
	private void putNotification(CharSequence ticker, CharSequence content, int type, int icon, boolean lowPriority) {
		Context ctx = getApplicationContext();
		Intent intent = new Intent(ctx, ChatActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx);

		builder.setContentIntent(contentIntent)
					.setSmallIcon(icon)
//		            .setLargeIcon(BitmapFactory.decodeResource(res, R.drawable.tulip_bw))
					.setTicker(ticker)
					.setWhen(System.currentTimeMillis())
					.setAutoCancel(true)
					.setContentTitle(ticker)
					.setContentText(content)
					;
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			if(lowPriority) {
				builder.setPriority(Notification.PRIORITY_MIN);
			}
		}
		Notification n = builder.build();

		nm.notify(type, n);
	}

	private void removeNotification(int type) {
		NotificationManager nm = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(type);
	}

	public void setViewActive(boolean viewActive) {
		this.viewActive = viewActive;
	}

	/**
	 * An activity might decide to send a notification to the fragments this way
	 */
	public void forceSendNewDataNotifications() {
		Log.d(LOG_TAG, "Force a new data intent to be broadcast");
		notifyActivities(true, true);
	}

	private void notifyActivities(boolean updatedMessages, boolean updatedContacts) {
		Intent intent = new Intent(DATA_RECEIVED);
		intent.putExtra(UPDATED_MESSAGES, updatedMessages);
		intent.putExtra(UPDATED_CONTACTS, updatedContacts);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	public void log(Object o) {
		if(!loggingEnabled) return;
		Log.i(LOG_TAG, "OI: " + o.toString());
	}

	static Integer login(CharSequence name, CharSequence pwd) throws ClientProtocolException, IOException {
		CookieManager cookieManager = new java.net.CookieManager();
		CookieHandler.setDefault(cookieManager);
		
		StringBuilder postDataBuilder = new StringBuilder();
		postDataBuilder.append("j_username=").append(URLEncoder.encode(name.toString(), "UTF-8")).append('&');
		postDataBuilder.append("j_password=").append(URLEncoder.encode(pwd.toString(), "UTF-8")).append('&');
		postDataBuilder.append("offsetJanuary=").append(URLEncoder.encode("1", "UTF-8")).append('&');
		postDataBuilder.append("offsetJuly=").append(URLEncoder.encode("1", "UTF-8")).append('&');
		byte[] postData = postDataBuilder.toString().getBytes();

		URL url = new URL(OrganizatorMessagingService.ORGANIZATOR_URL + "login");
		HttpsURLConnection conn = prepareConnection(url);
		conn.setDoOutput(true);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Length", Integer.toString(postData.length));
		conn.connect();

		OutputStream out = conn.getOutputStream();
		out.write(postData);
		out.close();

		int responseCode = conn.getResponseCode();
		conn.disconnect();
		return responseCode;
	}

	public int postMessage(OrganizatorMessage msg) throws IOException {
		int code = postMessage2(msg);
//		if(code == 401) {
//			login(username, password);
//			return postMessage2(msg);
//		}
		return code;
	}

	private static int postMessage2(OrganizatorMessage msg) throws IOException {
		StringBuilder postDataBuilder = new StringBuilder();
		postDataBuilder.append("message=").append(URLEncoder.encode(msg.text, "UTF-8")).append('&');
		for (String to: msg.to) {
			postDataBuilder.append("to").append(URLEncoder.encode("[]", "UTF-8")).append("=").append(URLEncoder.encode(to, "UTF-8")).append('&');
		}
		byte[] postData = postDataBuilder.toString().getBytes();

		URL url = new URL(OrganizatorMessagingService.ORGANIZATOR_URL + "chatjson.ovi?method=sendMessage");
		HttpsURLConnection conn = prepareConnection(url);

		conn.setDoOutput(true);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Length", Integer.toString(postData.length));
		conn.connect();

		OutputStream out = conn.getOutputStream();
		out.write(postData);
		out.close();

		int responseCode = conn.getResponseCode();
		if(200 == responseCode) {
			String loginHeader = conn.getHeaderField("Organizator-Login");
			if(loginHeader != null) {
				responseCode = 450;
			} else {
				String contentType = conn.getHeaderField("Content-Type");
				if(contentType == null || !contentType.startsWith("application/json")) {
					responseCode = 451;
				}
			}
		}
		conn.disconnect();
		return responseCode;
	}

	private OrganizatorMessage transformMsg(JSONObject jsonMsg) throws JSONException {
		OrganizatorMessage msg = new OrganizatorMessage();
		msg.id = jsonMsg.getLong("id");;
		msg.time = jsonMsg.getLong("time");
		msg.text = jsonMsg.getString("text");
		msg.from = jsonMsg.getString("from");
		if(!jsonMsg.isNull("self")) {
			msg.self = jsonMsg.getBoolean("self");
		}
		JSONArray jsonTo = jsonMsg.getJSONArray("to");
		String[] to = new String[jsonTo.length()];
		for(int j = 0; j < jsonTo.length(); j++) {
			to[j] = jsonTo.getString(j);
		}
		msg.to = to;
		msg.joinedTo = TextUtils.join(", ", msg.to);
		// TODO: also process the mainTo

		return msg;
	}

	public List<OrganizatorMessage> searchMessages(String criteria) throws IOException, JSONException {
		List<OrganizatorMessage> messages = new ArrayList<OrganizatorMessage>();
		JSONArray jsonMessages = fetchJsonObject(OrganizatorMessagingService.ORGANIZATOR_URL + "chatjson.ovi?method=searchMessages", new String[] {"message", criteria}).getJSONArray("messages");
		for(int i = 0; i < jsonMessages.length(); i++) {
			messages.add(transformMsg(jsonMessages.getJSONObject(i)));
		}
		return messages;
	}

	public List<MemoHit> searchMemos(String criteria) throws IOException, JSONException {
		List<MemoHit> memoHits = new ArrayList<MemoHit>();

		JSONArray jsonMemoHits = fetchJsonObject(OrganizatorMessagingService.ORGANIZATOR_URL + "memo/search", new String[] {"search", criteria}).getJSONArray("memos");
		for(int i = 0; i < jsonMemoHits.length(); i++) {
			JSONObject jsonMemoHit = jsonMemoHits.getJSONObject(i);
			MemoHit memoHit = new MemoHit();
			memoHit.id = jsonMemoHit.getLong("id");
			memoHit.title = jsonMemoHit.getString("title");
			if(!jsonMemoHit.isNull("group_id")) {
				memoHit.groupId = jsonMemoHit.getLong("group_id");
			}
			memoHit.userId = jsonMemoHit.getLong("userId");
			memoHits.add(memoHit);
		}
		return memoHits;
	}

	Memo fetchMemo(long id) throws IOException, JSONException {
		Memo memo = new Memo();
		JSONObject jsonMemo = fetchJsonObject(OrganizatorMessagingService.ORGANIZATOR_URL + "memo/" + id, new String[] {}).getJSONObject("memo");
		memo.id = jsonMemo.getLong("id");
		memo.memotext = jsonMemo.getString("title") + jsonMemo.getString("memotext");
		return memo;
	}

	static JSONObject fetchJsonObject(String url, String[] params)  throws IOException, JSONException {
		StringBuilder postDataBuilder = new StringBuilder();
		for (int i = 0; i < params.length; i+= 2) {
			if(i > 0) {
				postDataBuilder.append('&');
			}
			postDataBuilder.append(params[i]).append("=").append(URLEncoder.encode(params[i + 1], "UTF-8"));
		}
		byte[] postData = postDataBuilder.toString().getBytes();
		HttpsURLConnection conn = prepareConnection(new URL(url));

		conn.setRequestProperty("X-Requested-With", "XMLHttpRequest");
		if(params.length > 0) {
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Length", Integer.toString(postData.length));
			conn.connect();

			OutputStream out = conn.getOutputStream();
			out.write(postData);
			out.close();
		} else {
			conn.setDoOutput(false);
			conn.setRequestMethod("GET");
			conn.connect();
		}
		Reader reader = new InputStreamReader(conn.getInputStream(), "UTF-8");
		StringBuilder sb = new StringBuilder();
		for(int c = reader.read(); c != -1; c = reader.read()) {
			sb.append((char) c);
		}
		conn.disconnect();

		return new JSONObject(sb.toString());
	}

	static HttpsURLConnection prepareConnection(URL url) throws IOException {
		HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
		conn.setInstanceFollowRedirects(false);
		conn.setUseCaches(false);
		conn.setRequestProperty("User-Agent", USERAGENT);

		conn.setConnectTimeout(30000);
		conn.setReadTimeout(90000);

		return conn;
	}

	private boolean isNetworkAvailable() {
	    ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
	    return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if(NOTIFICATIONS_SERVICE_RUNNING.equals(key)) {
			serviceStatusNotification = sharedPreferences.getBoolean(key, true);
			Log.d(LOG_TAG, "serviceStatusNotification changed to: " + serviceStatusNotification);
		} else if(NOTIFICATIONS_NEW_MESSAGE_RINGTONE.equals(key)) {
			newMessageRingTone = sharedPreferences.getString(key, "");
			Log.d(LOG_TAG, "Ringtone: [" + newMessageRingTone + "]");
		} else if(NOTIFICATIONS_NEW_MESSAGE_VIBRATE.equals(key)) {
			vibrateEnabled = sharedPreferences.getBoolean(key, false);
			Log.d(LOG_TAG, "Vibrate: " + vibrateEnabled);
		} else if(NOTIFICATIONS_NEW_MESSAGE.equals(key)) {
			newMessageNotify = sharedPreferences.getBoolean(key, true);
			Log.d(LOG_TAG, "Notify new messages: " + newMessageNotify);
		} else if(SEND_NOTIFICATIONS_TO_SONY_WATCH.equals(key)) {
			notifySonyWatch = sharedPreferences.getBoolean(key, false);
			Log.d(LOG_TAG, "Send notifications to Sony watch: " + notifySonyWatch);
		} else if(LAST_NOTIFICATION_TO_SONY_WATCH.equals(key)) {
			String lastMessageIdForWearableString = sharedPreferences.getString(LAST_NOTIFICATION_TO_SONY_WATCH, "0");
			lastMessageIdForWearable = lastMessageIdForWearableString.isEmpty() ? 0 : Long.parseLong(lastMessageIdForWearableString, 10); 
			Log.d(LOG_TAG, "Last message id sent to wearable: " + lastMessageIdForWearable);
		} else if(LED_NOTIFICATION_COLOR.equals(key)) {
			ledNotificationColor = sharedPreferences.getInt(key, 0xFFFF0000);
			Log.d(LOG_TAG, "Let light notification color: " + ledNotificationColor);
		} else if(LED_NOTIFICATION.equals(key)) {
			ledNotification = sharedPreferences.getBoolean(key, true);
			Log.d(LOG_TAG, "Let light notification: " + ledNotification);
		}
	}
}
