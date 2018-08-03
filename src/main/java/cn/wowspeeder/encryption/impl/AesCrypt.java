package cn.wowspeeder.encryption.impl;

import java.io.ByteArrayOutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.StreamBlockCipher;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CFBBlockCipher;
import org.bouncycastle.crypto.modes.OFBBlockCipher;
import cn.wowspeeder.encryption.CryptSteamBase;

/**
 * AES 实现类
 * 
 * @author zhaohui
 * 
 */
public class AesCrypt extends CryptSteamBase {

	public final static String CIPHER_AES_128_CFB = "aes-128-cfb";
	public final static String CIPHER_AES_192_CFB = "aes-192-cfb";
	public final static String CIPHER_AES_256_CFB = "aes-256-cfb";
	public final static String CIPHER_AES_128_OFB = "aes-128-ofb";
	public final static String CIPHER_AES_192_OFB = "aes-192-ofb";
	public final static String CIPHER_AES_256_OFB = "aes-256-ofb";

	public static Map<String, String> getCiphers() {
		Map<String, String> ciphers = new HashMap<>();
		ciphers.put(CIPHER_AES_128_CFB, AesCrypt.class.getName());
		ciphers.put(CIPHER_AES_192_CFB, AesCrypt.class.getName());
		ciphers.put(CIPHER_AES_256_CFB, AesCrypt.class.getName());
		ciphers.put(CIPHER_AES_128_OFB, AesCrypt.class.getName());
		ciphers.put(CIPHER_AES_192_OFB, AesCrypt.class.getName());
		ciphers.put(CIPHER_AES_256_OFB, AesCrypt.class.getName());

		return ciphers;
	}

	public AesCrypt(String name, String password) {
		super(name, password);
	}

	@Override
	public int getKeyLength() {
		if (_name.equals(CIPHER_AES_128_CFB)
				|| _name.equals(CIPHER_AES_128_OFB)) {
			return 16;
		} else if (_name.equals(CIPHER_AES_192_CFB)
				|| _name.equals(CIPHER_AES_192_OFB)) {
			return 24;
		} else if (_name.equals(CIPHER_AES_256_CFB)
				|| _name.equals(CIPHER_AES_256_OFB)) {
			return 32;
		}

		return 0;
	}

	@Override
	protected StreamBlockCipher getCipher(boolean isEncrypted)
			throws InvalidAlgorithmParameterException {
		AESEngine engine = new AESEngine();
		StreamBlockCipher cipher;

		if (_name.equals(CIPHER_AES_128_CFB)) {
			cipher = new CFBBlockCipher(engine, getIVLength() * 8);
		} else if (_name.equals(CIPHER_AES_192_CFB)) {
			cipher = new CFBBlockCipher(engine, getIVLength() * 8);
		} else if (_name.equals(CIPHER_AES_256_CFB)) {
			cipher = new CFBBlockCipher(engine, getIVLength() * 8);
		} else if (_name.equals(CIPHER_AES_128_OFB)) {
			cipher = new OFBBlockCipher(engine, getIVLength() * 8);
		} else if (_name.equals(CIPHER_AES_192_OFB)) {
			cipher = new OFBBlockCipher(engine, getIVLength() * 8);
		} else if (_name.equals(CIPHER_AES_256_OFB)) {
			cipher = new OFBBlockCipher(engine, getIVLength() * 8);
		} else {
			throw new InvalidAlgorithmParameterException(_name);
		}

		return cipher;
	}

	@Override
	public int getIVLength() {
		return 16;
	}

	@Override
	protected SecretKey getKey() {
		return new SecretKeySpec(_ssKey.getEncoded(), "AES");
	}

	@Override
	protected void _encrypt(byte[] data, ByteArrayOutputStream stream) {
		int noBytesProcessed;
		byte[] buffer = new byte[data.length];

		noBytesProcessed = encCipher.processBytes(data, 0, data.length, buffer,
				0);
		stream.write(buffer, 0, noBytesProcessed);
	}

	@Override
	protected void _decrypt(byte[] data, ByteArrayOutputStream stream) {
		int noBytesProcessed;
		byte[] buffer = new byte[data.length];

		noBytesProcessed = decCipher.processBytes(data, 0, data.length, buffer,
				0);
		stream.write(buffer, 0, noBytesProcessed);
	}
}
