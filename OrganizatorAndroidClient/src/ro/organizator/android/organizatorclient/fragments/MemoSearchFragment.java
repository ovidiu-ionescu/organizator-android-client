package ro.organizator.android.organizatorclient.fragments;

import java.io.IOException;
import java.util.List;

import org.json.JSONException;

import ro.organizator.android.organizatorclient.MemoActivity;
import ro.organizator.android.organizatorclient.MemoHit;
import ro.organizator.android.organizatorclient.OrganizatorMessagingService;
import ro.organizator.android.organizatorclient.R;
import ro.organizator.android.organizatorclient.activity.MainActivity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

public class MemoSearchFragment extends Fragment {

	private static final String LOG_TAG = ChatSearchFragment.class.getName();

	ListView memoHitsList;

	public MemoSearchFragment() {
		
	}
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_memosearch, container, false);
		
		memoHitsList = (ListView) rootView.findViewById(R.id.memo_hits);
		memoHitsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
				final MemoHit memoHit = (MemoHit) parent.getItemAtPosition(position);
				Intent intent = new Intent(getActivity(), MemoActivity.class);
				intent.putExtra("memo_id", memoHit.id);
				startActivity(intent);
			}
		});

		View.OnClickListener searchListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(LOG_TAG, "Chat search clicked");
				search(v);
			}
		};
		Log.d(LOG_TAG, "Add click listener for search chat");
		rootView.findViewById(R.id.memo_searcher).setOnClickListener(searchListener);
		

		return rootView;
	}

	public void search(View view) {
		EditText edit = (EditText)getView().findViewById(R.id.memo_search_criteria);
		String text = edit.getText().toString();

		// disable the text editor, we'll enable it back after sending the message
		edit.setEnabled(false);

		new SearchTask().execute(text);
	}

	public class SearchTask extends AsyncTask<String, Integer, Integer> {

		@Override
		protected Integer doInBackground(String... params) {
			String criteria = params[0];
			OrganizatorMessagingService organizatorMessagingService = ((MainActivity)getActivity()).organizatorMessagingService;
			if(null == organizatorMessagingService) {
				Log.e(LOG_TAG, "OrganizatorMessagingService not bound");
			}
			try {
				final List<MemoHit> memoHits = organizatorMessagingService.searchMemos(criteria);
				getActivity().runOnUiThread(new Runnable() {
					public void run() {
						memoHitsList.setAdapter(new MemoSearchArrayAdapter(getActivity(), R.layout.memo_hit_item, memoHits));
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
			EditText edit = (EditText)getView().findViewById(R.id.memo_search_criteria);
			edit.setEnabled(true);
		}
	}

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
}
