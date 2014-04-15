package ro.organizator.android.organizatorclient;

import android.util.Log;

public class Contact implements Comparable<Contact>, Cloneable {
	static final String LOG_TAG = Contact.class.getName();
	
	public int id;
	public String name;
	public String mobile;
	public boolean active;
	public boolean idle;
	public long check;
	public String agent;
	public boolean external;
	public boolean selected;
	Contact(String name) {
		this.name = name;
	}

	@Override
	public int compareTo(Contact c) {
		if(this.external != c.external) {
			return c.external ? -1 : 1; 
		}
		if(this.check != c.check) {
			return this.check < c.check ? 1 : -1;
		}
		return this.name.compareTo(c.name);
	}

	public Contact clone() {
		try {
			return (Contact) super.clone();
		} catch (CloneNotSupportedException e) {
			Log.e(LOG_TAG, "Clone failed", e);
		}
		return null;
	}
}
