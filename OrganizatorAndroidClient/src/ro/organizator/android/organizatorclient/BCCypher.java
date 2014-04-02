package ro.organizator.android.organizatorclient;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import org.spongycastle.crypto.DataLengthException;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.PBEParametersGenerator;
import org.spongycastle.crypto.engines.AESEngine;
import org.spongycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.spongycastle.crypto.modes.CBCBlockCipher;
import org.spongycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.spongycastle.crypto.params.KeyParameter;

import android.util.Base64;

/**
 * Implement the encryption services using Bouncy (Spongy) Castle
 * Uses code from the Internet
 * @author Ovidiu-Laurian Ionescu
 *
 */
public class BCCypher {
	
	/**
	 * 
	 * @param user for salt
	 * @param password
	 */
	static KeyParameter generateKey(String password, String salt) {
		PBEParametersGenerator generator = new PKCS5S2ParametersGenerator();
		generator.init(PBEParametersGenerator.PKCS5PasswordToUTF8Bytes((password).toCharArray()), salt.getBytes(), 100);
		KeyParameter params = (KeyParameter)generator.generateDerivedParameters(256);
		return params;
	}

	private static byte[] cipherData(PaddedBufferedBlockCipher cipher, byte[] data) throws DataLengthException, IllegalStateException, InvalidCipherTextException {
        int minSize = cipher.getOutputSize(data.length);
        byte[] outBuf = new byte[minSize];
        int length1 = cipher.processBytes(data, 0, data.length, outBuf, 0);
        int length2 = cipher.doFinal(outBuf, length1);
        int actualLength = length1 + length2;
        return Arrays.copyOf(outBuf, actualLength);
    }
	
	static String encrypt(String text, String user) throws DataLengthException, IllegalStateException, InvalidCipherTextException, UnsupportedEncodingException {
		KeyParameter keyParameter = generateKey("CheieSecreta", user);
		PaddedBufferedBlockCipher aes = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()));
		aes.init(true, keyParameter);
		return Base64.encodeToString(cipherData(aes, text.getBytes("UTF-16LE")), Base64.DEFAULT);
	}

	static String decrypt(String text, String user) throws DataLengthException, IllegalStateException, InvalidCipherTextException, UnsupportedEncodingException {
		KeyParameter keyParameter = generateKey("CheieSecreta", user);
		PaddedBufferedBlockCipher aes = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()));
		aes.init(false, keyParameter);
		return new String(cipherData(aes, Base64.decode(text, Base64.DEFAULT)), "UTF-16LE");
	}
}
