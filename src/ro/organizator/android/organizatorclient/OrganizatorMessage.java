package ro.organizator.android.organizatorclient;

public class OrganizatorMessage {
	long id;
	long time;
	String text;
	String from;
	String[] to;
	String joinedTo = "";
	boolean self;

	public OrganizatorMessage() {
	}

	public OrganizatorMessage(long id, String text, boolean self) {
		this.id = id;
		this.text = text;
		this.self = self;
	}
}
