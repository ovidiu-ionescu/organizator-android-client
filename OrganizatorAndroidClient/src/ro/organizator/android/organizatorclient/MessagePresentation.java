package ro.organizator.android.organizatorclient;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Spannable;
import android.text.Spannable.Factory;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
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
	
	/*
	 * http://stackoverflow.com/questions/3341702/displaying-emoticons-in-android
	 */
	private static final Factory spannableFactory = Spannable.Factory.getInstance();

	private static final Map<Pattern, Integer> emoticons = new HashMap<Pattern, Integer>();

	static {
		addPattern(emoticons, ":)", R.drawable.emo_im_happy);
		addPattern(emoticons, ":-)", R.drawable.emo_im_happy);
		addPattern(emoticons, ":-(", R.drawable.emo_im_sad);
		addPattern(emoticons, ":-(", R.drawable.emo_im_sad);
		addPattern(emoticons, ":-D", R.drawable.emo_im_laughing);
		addPattern(emoticons, ":D", R.drawable.emo_im_laughing);
		addPattern(emoticons, ":'(", R.drawable.emo_im_crying);
		addPattern(emoticons, ":-/", R.drawable.emo_im_undecided);
		addPattern(emoticons, ":-[", R.drawable.emo_im_embarrassed);
		addPattern(emoticons, "O:-)", R.drawable.emo_im_angel);
		addPattern(emoticons, ":-!", R.drawable.emo_im_foot_in_mouth);
		addPattern(emoticons, ":-$", R.drawable.emo_im_money_mouth);
		addPattern(emoticons, "B-)", R.drawable.emo_im_cool);
		addPattern(emoticons, ":-*", R.drawable.emo_im_kissing);
		addPattern(emoticons, ":O", R.drawable.emo_im_yelling);
		addPattern(emoticons, "=-O", R.drawable.emo_im_surprised);
		addPattern(emoticons, ":-P", R.drawable.emo_im_tongue_sticking_out);
		addPattern(emoticons, ":P", R.drawable.emo_im_tongue_sticking_out);
		addPattern(emoticons, ":-p", R.drawable.emo_im_tongue_sticking_out);
		addPattern(emoticons, ":p", R.drawable.emo_im_tongue_sticking_out);
		addPattern(emoticons, ";-)", R.drawable.emo_im_winking);
		addPattern(emoticons, ":-X", R.drawable.emo_im_lips_are_sealed);
		addPattern(emoticons, "o.O", R.drawable.emo_im_wtf);
	}

	private static void addPattern(Map<Pattern, Integer> map, String smile, int resource) {
	    map.put(Pattern.compile(Pattern.quote(smile)), resource);
	}

	public static boolean addSmiles(Context context, Spannable spannable) {
	    boolean hasChanges = false;
	    for (Entry<Pattern, Integer> entry : emoticons.entrySet()) {
	        Matcher matcher = entry.getKey().matcher(spannable);
	        while (matcher.find()) {
	            boolean set = true;
	            // remove all image spans in the matched text. If there are spans that go over the start or end then skip
	            for (ImageSpan span : spannable.getSpans(matcher.start(), matcher.end(), ImageSpan.class)) {
	                if (spannable.getSpanStart(span) >= matcher.start() && spannable.getSpanEnd(span) <= matcher.end())
	                    spannable.removeSpan(span);
	                else {
	                    set = false;
	                    break;
	                }
	            }
	            if (set) {
	                hasChanges = true;
	                spannable.setSpan(new ImageSpan(context, entry.getValue()),
	                        matcher.start(), matcher.end(),
	                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	            }
	        }
	    }
	    return hasChanges;
	}

	public static Spannable getSmiledText(Context context, CharSequence text) {
	    Spannable spannable = spannableFactory.newSpannable(text);
	    addSmiles(context, spannable);
	    return spannable;
	}	
}
