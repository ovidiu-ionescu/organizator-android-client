package ro.organizator.android.organizatorclient;

import java.util.List;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class MessageArrayAdapter extends ArrayAdapter<OrganizatorMessage> {

	private final Context context;
	private int OWN_MESSAGE_COLOR;
	private int MESSAGE_COLOR;

	public MessageArrayAdapter(Context context, List<OrganizatorMessage> values) {
		super(context, R.layout.msg_row_layout, values);
		this.context = context;

		// cache the colors
		OWN_MESSAGE_COLOR = context.getResources().getColor(R.color.own_message_color);
		MESSAGE_COLOR = context.getResources().getColor(R.color.message_color);
	}

	static class ViewHolder {
		public TextView text;
		public TextView info;
		public OrganizatorMessage message;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		View rowView = convertView;
		if(rowView == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			rowView = inflater.inflate(R.layout.msg_row_layout, parent, false);

			// keep the references in the holder to find the inner views faster
			ViewHolder viewHolder = new ViewHolder();
			viewHolder.text = (TextView) rowView.findViewById(R.id.msg_text);
			viewHolder.info = (TextView) rowView.findViewById(R.id.msg_info);
			rowView.setTag(viewHolder);
		}

		OrganizatorMessage msg = getItem(position);
		OrganizatorMessage prevMsg = null;
		if(position > 0) {
			prevMsg = getItem(position - 1);
		}

		ViewHolder holder = (ViewHolder) rowView.getTag();
		holder.message = msg;
		holder.text.setText(msg.text);
		if(msg.self) {
			holder.text.setGravity(Gravity.RIGHT);
			holder.info.setGravity(Gravity.RIGHT);
		} else {
			holder.text.setGravity(Gravity.LEFT);
			holder.info.setGravity(Gravity.LEFT);
		}

		if( (prevMsg != null && (msg.time - prevMsg.time > 300000)) || // more that 5 minutes since the last message, show the time
				(msg.to != null && (prevMsg == null || !prevMsg.joinedTo.equals(msg.joinedTo) || !msg.from.equals(prevMsg.from)))) {
			MessagePresentation.populateMessageInfo(context, holder.info, msg);
			holder.info.setVisibility(View.VISIBLE);
		} else {
			holder.info.setVisibility(View.GONE);
		}

		if(msg.self) {
			holder.text.setTextColor(OWN_MESSAGE_COLOR);
		} else {
			holder.text.setTextColor(MESSAGE_COLOR);
		}

		return rowView;
	}
}
