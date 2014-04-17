package ro.organizator.android.organizatorclient;

import java.util.List;

import ro.organizator.android.organizatorclient.DestinationListAdapter.Holder;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.TextView;

public class DestinationDialogFragment extends DialogFragment {
	public static final String LOG_TAG = DestinationDialogFragment.class.getName();

	public interface DestinationDialogListener {
		public void onDialogPositiveClick(List<Contact> contacts);
		public void onDialogNegativeClick(List<Contact> contacts);
		public void setDestination(String[] destination);
	}

	DestinationDialogListener mListener;

	public void setListener(DestinationDialogListener mListener) {
		this.mListener = mListener;
	}

	List<Integer> mSelectedItems;
	List<Contact> contacts;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		// Verify that the host activity implements the callback interface
		try {
			// Instantiate the NoticeDialogListener so we can send events to the
			// host
			if(mListener == null) {
				mListener = (DestinationDialogListener) activity;
			}
		} catch (ClassCastException e) {
			// The activity doesn't implement the interface, throw exception
			throw new ClassCastException(activity.toString() + " must implement DestinationDialogListener");
		}
	}

	private void setDestinationsPreview(TextView text, List<Contact> contacts) {
		// update the info at the top
		StringBuilder sb = new StringBuilder();
		String sep = "";
		for(Contact contact: contacts) {
			if(contact.selected) {
				sb.append(sep).append(contact.name);
				sep = "; ";
			}
		}
		text.setText(sb);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View view = inflater.inflate(R.layout.destination_dialog, null);
		final TextView destinationsPreview = (TextView) view.findViewById(R.id.selected_destinations_info);
		final ListView listView = (ListView) view.findViewById(R.id.destinationListView);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Holder holder = (Holder) view.getTag();
				CheckedTextView tv = holder.text;
				if(tv.isChecked()) {
					tv.setChecked(false);
				} else {
					tv.setChecked(true);
				}
				holder.contact.selected = tv.isChecked();

				setDestinationsPreview(destinationsPreview, contacts);
			}
		});
		listView.setAdapter(new DestinationListAdapter(getActivity(), contacts));
		builder.setView(view)
		// Add action buttons
				.setPositiveButton(R.string.Select, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						if(null != mListener) {
							mListener.onDialogPositiveClick(contacts);
						}
					}
				}).setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						DestinationDialogFragment.this.getDialog().cancel();
					}
				}).setNeutralButton(R.string.Clear, null) 
				;
		setDestinationsPreview(destinationsPreview, contacts);
		final AlertDialog dialog = builder.create();
		
		dialog.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface d) {
				Button clear = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
				clear.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						int i = 0;
						for(Contact contact: contacts) {
							if(contact.selected) {
								contact.selected = false;
							}
							listView.setItemChecked(i, false);
							i++;
						}
						destinationsPreview.setText("");
					}
				});
			}
		});
		
		return dialog;
	}

	@Override
	public void show(FragmentManager manager, String tag) {
		super.show(manager, tag);
	}

	public void setContacts(List<Contact> contacts) {
		this.contacts = contacts;
	}
}
