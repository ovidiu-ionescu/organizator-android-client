package ro.organizator.android.organizatorclient;

import java.util.List;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.ListAdapter;

public class DestinationListAdapter implements ListAdapter {

	private int ACTIVE_COLOR;
	private int IDLE_COLOR;
	private int OFFLINE_COLOR;

	Context context;
	List<Contact> contacts;

	public DestinationListAdapter(Context context, List<Contact> contacts) {
		this.context = context;
		this.contacts = contacts;

		// cache the colors
		ACTIVE_COLOR = context.getResources().getColor(R.color.user_active_color);
		IDLE_COLOR = context.getResources().getColor(R.color.user_idle_color);
		OFFLINE_COLOR = context.getResources().getColor(R.color.user_offline_color);
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return contacts.size();
	}

	@Override
	public Object getItem(int i) {
		// TODO Auto-generated method stub
		return contacts.get(i);
	}

	@Override
	public long getItemId(int i) {
		return contacts.get(i).id;
	}

	@Override
	public int getItemViewType(int arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	static class Holder {
		CheckedTextView text;
		Contact contact;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parentView) {
		View rowView = convertView;
		if(null == rowView) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			rowView = inflater.inflate(R.layout.destination_list_row_layout, parentView, false);
			// keep the references in the holder to find the inner views faster
			Holder viewHolder = new Holder();
			viewHolder.text = (CheckedTextView) rowView.findViewById(R.id.selected_destination_text);
			rowView.setTag(viewHolder);
		}
		Contact contact = contacts.get(position);
		Holder holder = (Holder) rowView.getTag();
		holder.contact = contact;
		if(contact.active) {
			if(contact.idle) {
				holder.text.setTextColor(IDLE_COLOR);
			} else {
				holder.text.setTextColor(ACTIVE_COLOR);
			}
		} else {			
			holder.text.setTextColor(OFFLINE_COLOR);
		}

		holder.text.setText(contacts.get(position).name);
		holder.text.setChecked(contact.selected);
		return rowView;
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public boolean isEmpty() {
		return contacts.isEmpty();
	}

	@Override
	public void registerDataSetObserver(DataSetObserver arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean areAllItemsEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isEnabled(int i) {
		return true;
	}

}
