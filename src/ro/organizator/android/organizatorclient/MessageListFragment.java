package ro.organizator.android.organizatorclient;

import java.util.ArrayList;
import java.util.List;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class MessageListFragment extends ListFragment {
	
	private ArrayAdapter<OrganizatorMessage> adapter;

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
				System.out.println("OI: Long click on item");
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
		for (OrganizatorMessage msg : msgs) {
			adapter.add(msg);
		}
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		FragmentActivity activity = getActivity();
		if(!(activity instanceof ChatActivity)) {
			return;
		}
		ChatActivity chatActivity = (ChatActivity) getActivity();
		OrganizatorMessage msg = ((MessageArrayAdapter.ViewHolder)v.getTag()).message;
		if(msg.self) {
			chatActivity.setDestination(msg.to);
		} else {
			List<String> dest = new ArrayList<String>(msg.to.length);
			dest.add(msg.from);

			for (int i = 0; i < msg.to.length; i++) {
				String to = msg.to[i];
				if(!to.equals(msg.from)) {
					dest.add(to);
				}
			}

			chatActivity.setDestination(dest.toArray(new String[dest.size()]));
		}
	}
}
