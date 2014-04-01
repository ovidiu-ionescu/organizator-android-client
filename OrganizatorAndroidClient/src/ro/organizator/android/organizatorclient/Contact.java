package ro.organizator.android.organizatorclient;

public class Contact implements Comparable<Contact>, Cloneable {
	int id;
	String name;
	boolean active;
	boolean idle;
	long check;
	String agent;
	boolean external;
	boolean selected;
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
			e.printStackTrace();
		}
		return null;
	}
}
