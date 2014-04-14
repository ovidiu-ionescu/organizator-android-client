package ro.organizator.android.organizatorclient.fragments;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.json.JSONException;

import ro.organizator.android.organizatorclient.MessageListFragment;
import ro.organizator.android.organizatorclient.OrganizatorMessage;
import ro.organizator.android.organizatorclient.R;
import ro.organizator.android.organizatorclient.activity.MainActivity;
import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

public class ChatSearchFragment extends Fragment {
	
	private static final String LOG_TAG = ChatSearchFragment.class.getName();
	
	MessageListFragment messageListFragment = null;

	public ChatSearchFragment() {
		
	}
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View rootView = inflater.inflate(R.layout.fragment_chatsearch, container, false);
		
		View.OnClickListener searchListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(LOG_TAG, "Chat search clicked");
				search(v);
			}
		};
		Log.d(LOG_TAG, "Add click listener for search chat");
		rootView.findViewById(R.id.chat_search_button).setOnClickListener(searchListener);

		// add the message list fragment
		messageListFragment = new MessageListFragment();
		getChildFragmentManager().beginTransaction().replace(R.id.chat_search_messages_placeholder, messageListFragment).commit();
		
		setHasOptionsMenu(true);

		return rootView;
	}
	
	@Override
	public void onDestroyView() {
		getChildFragmentManager().beginTransaction().remove(messageListFragment).commitAllowingStateLoss();
		super.onDestroyView();
	}

	public void search(View view) {
		EditText edit = (EditText)getView().findViewById(R.id.chat_search_criteria);
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
				final List<OrganizatorMessage> messages = ((MainActivity)getActivity()).organizatorMessagingService.searchMessages(criteria);
				getActivity().runOnUiThread(new Runnable() {
					public void run() {
						messageListFragment.setMessages(messages);
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
			EditText edit = (EditText)getView().findViewById(R.id.chat_search_criteria);
			edit.setEnabled(true);
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.chat_search_menu, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		switch (menuItem.getItemId()) {
		case R.id.chat_search_menu_today:
			searchToday();
			return true;
		case R.id.chat_search_menu_yesterday:
			searchYesterday();
			return true;
		case R.id.chat_search_menu_clear:
			clear();
			return true;
		}
		return super.onOptionsItemSelected(menuItem);
	}

	@SuppressLint("SimpleDateFormat") 
	private void searchToday() {
		EditText edit = (EditText)getView().findViewById(R.id.chat_search_criteria);
		edit.setText("date:" + new SimpleDateFormat("yyyyMMdd").format(new Date()));
		search(edit);
	}

	@SuppressLint("SimpleDateFormat") 
	private void searchYesterday() {
		EditText edit = (EditText)getView().findViewById(R.id.chat_search_criteria);
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, -1);
		edit.setText("date:" + new SimpleDateFormat("yyyyMMdd").format(cal.getTime()));
		search(edit);
	}

	private void clear() {
		EditText edit = (EditText)getView().findViewById(R.id.chat_search_criteria);
		edit.setText("");
		// remove the messages from the result list
		messageListFragment.setMessages(null);
	}
}
