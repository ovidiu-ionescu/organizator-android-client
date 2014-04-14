package ro.organizator.android.organizatorclient;

public class OrganizatorMessage {
	public long id;
	public long time;
	public String text;
	public String from;
	public String[] to;
	public String joinedTo = "";
	public boolean self;

	public OrganizatorMessage() {
	}

	public OrganizatorMessage(long id, String text, boolean self) {
		this.id = id;
		this.text = text;
		this.self = self;
	}
}
