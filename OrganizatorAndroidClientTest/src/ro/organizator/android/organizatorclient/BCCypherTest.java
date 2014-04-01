package ro.organizator.android.organizatorclient;

import android.util.Log;
import junit.framework.TestCase;

public class BCCypherTest extends TestCase {

	static final String LOG_TAG = BCCypherTest.class.getName();
	
	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public final void testGenerateKey() {
		BCCypher.generateKey("user", "password");
	}

	public final void testEncryptionDecription() throws Exception {
		String plainText = "Angelus Dominini";
		String user = "Mariae";
		String encoded = BCCypher.encrypt(plainText, user);
		String decoded = BCCypher.decrypt(encoded, user);
		Log.i(LOG_TAG, decoded);
		assertEquals("Text should be the same", plainText, decoded);
	}
	
}
