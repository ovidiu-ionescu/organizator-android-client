package ro.organizator.android.organizatorclient;

import java.util.ArrayList;
import java.util.List;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class MessageListFragment extends ListFragment {

	static final String LOG_TAG = MessageListFragment.class.getName();

	private ArrayAdapter<OrganizatorMessage> adapter;

	private DestinationDialogFragment.DestinationDialogListener destinationListener;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		List<OrganizatorMessage> msgs = new ArrayList<OrganizatorMessage>();

		adapter = new MessageArrayAdapter(getActivity(), msgs);
		getListView().setTranscriptMode(AbsListView.TRANSCRIPT_MODE_NORMAL);

		// make the list stack from the bottom e.g.: 
		getListView().setStackFromBottom(true);

		setListAdapter(adapter);

		getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){ 
			@Override 
			public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) { 
				Log.d(LOG_TAG, "Long click on item");
				final Dialog dialog = new Dialog(getActivity());
				dialog.setContentView(R.layout.message_detail_dialog);
				dialog.setTitle("Message Details");
				TextView textView = (TextView) dialog.findViewById(R.id.detail_msg_text);
				OrganizatorMessage msg = ((MessageArrayAdapter.ViewHolder)v.getTag()).message;
				textView.setText(msg.text);
				dialog.setCancelable(true);
				dialog.show();
				return true;
			}
		});

	}

	public void addMessage(OrganizatorMessage msg) {
		adapter.add(msg);
	}

	public void setMessages(List<OrganizatorMessage> msgs) {
		adapter.clear();
		if(msgs == null) {
			return;
		}
		for (OrganizatorMessage msg : msgs) {
			adapter.add(msg);
		}
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		if(null == destinationListener) {
			return;
		}

		OrganizatorMessage msg = ((MessageArrayAdapter.ViewHolder)v.getTag()).message;
		if(msg.self) {
			destinationListener.setDestination(msg.to);
		} else {
			List<String> dest = new ArrayList<String>(msg.to.length);
			dest.add(msg.from);

			for (int i = 0; i < msg.to.length; i++) {
				String to = msg.to[i];
				if(!to.equals(msg.from)) {
					dest.add(to);
				}
			}
			destinationListener.setDestination(dest.toArray(new String[dest.size()]));
		}
	}

	public void setDestinationListener(DestinationDialogFragment.DestinationDialogListener destinationListener) {
		this.destinationListener = destinationListener;
	}
}
