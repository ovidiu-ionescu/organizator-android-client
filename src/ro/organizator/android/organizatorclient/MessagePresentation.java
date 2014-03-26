package ro.organizator.android.organizatorclient;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.TextAppearanceSpan;
import android.widget.TextView;

public class MessagePresentation {
	private static final long oneDayMillis = 23 * 3600 * 1000;

	@SuppressLint("SimpleDateFormat")
	public static void populateMessageInfo(Context context, TextView tv, OrganizatorMessage msg) {
		// will write "yyyy-mm-dd hh:mm:ss -> destinations"
		String timeString = "2013-01-05 22:12:00";
		SimpleDateFormat sdf;
		if(System.currentTimeMillis() - msg.time > oneDayMillis) {
			sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		} else {
			sdf = new SimpleDateFormat("HH:mm:ss");
		}
		timeString = sdf.format(new Date(msg.time));

		SpannableStringBuilder ssb = new SpannableStringBuilder();
		addSpan(context, ssb, timeString, R.style.MessageTime);
		if(!msg.self) {
			addSpan(context, ssb, " " + msg.from + " :", R.style.MessageSender);
		}
		if(msg.self || msg.to.length > 1) {
			addSpan(context, ssb, " -> " + msg.joinedTo, R.style.MessageDestination);
		}

		tv.setText(ssb, TextView.BufferType.SPANNABLE);
	}
	
	private static void addSpan(Context context, SpannableStringBuilder sb, String text, int styleId) {
		int start = sb.length();
		if(text.length() == 0) {
			// don't add an empty span
			return;
		}
		sb.append(text);
		sb.setSpan(new TextAppearanceSpan(context, styleId), start, start + text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	}
}
