package cn.wowspeeder.encryption;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import javax.crypto.SecretKey;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.StreamBlockCipher;
import org.bouncycastle.crypto.StreamCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

public abstract class CryptBase implements ICrypt {

	protected final String _name;
	protected final SecretKey _key;
	protected final ShadowSocksKey _ssKey;
	protected final int _ivLength;
	protected final int _keyLength;
	protected boolean _ignoreIVSet;
	protected boolean _encryptIVSet;
	protected boolean _decryptIVSet;
	protected byte[] _encryptIV;
	protected byte[] _decryptIV;
	protected final Lock encLock = new ReentrantLock();
	protected final Lock decLock = new ReentrantLock();
	protected StreamCipher encCipher;
	protected StreamCipher decCipher;
	private Logger logger = Logger.getLogger(CryptBase.class.getName());

	public CryptBase(String name, String password) {
		_name = name.toLowerCase();
		_ivLength = getIVLength();
		_keyLength = getKeyLength();
		_ssKey = new ShadowSocksKey(password, _keyLength);
		_key = getKey();
	}

	@Override
	public void ivSetIgnore(boolean ignore) {
		this._ignoreIVSet = ignore;
	}

	protected void setIV(byte[] iv, boolean isEncrypt) {
		if (_ivLength == 0) {
			return;
		}
		CipherParameters cipherParameters = null;

		if (isEncrypt) {
			cipherParameters = getCipherParameters(iv);
			try {
				encCipher = getCipher(isEncrypt);
			} catch (InvalidAlgorithmParameterException e) {
				logger.info(e.toString());
			}
			encCipher.init(isEncrypt, cipherParameters);
		} else {
            _decryptIV = Arrays.copyOfRange(iv,0,_ivLength);
			cipherParameters = getCipherParameters(iv);
			try {
				decCipher = getCipher(isEncrypt);
			} catch (InvalidAlgorithmParameterException e) {
				logger.info(e.toString());
			}
			decCipher.init(isEncrypt, cipherParameters);
		}
	}

	protected CipherParameters getCipherParameters(byte[] iv){
		_decryptIV = Arrays.copyOfRange(iv,0,_ivLength);
		return new ParametersWithIV(new KeyParameter(_key.getEncoded()), _decryptIV);
	}

	@Override
	public void encrypt(byte[] data, ByteArrayOutputStream stream) {
		synchronized (encLock) {
			stream.reset();
			if (!_encryptIVSet || _ignoreIVSet) {
				_encryptIVSet = true;
				byte[] iv = randomBytes(_ivLength);
				setIV(iv, true);
				try {
					stream.write(iv);
				} catch (IOException e) {
					logger.info(e.toString());
				}

			}

			_encrypt(data, stream);
		}
	}

	@Override
	public void encrypt(byte[] data, int length, ByteArrayOutputStream stream) {
        byte[] d = Arrays.copyOfRange(data,0,length);
		encrypt(d, stream);
	}

	@Override
	public void decrypt(byte[] data, ByteArrayOutputStream stream) {
		byte[] temp;
		synchronized (decLock) {
			stream.reset();
			if (!_decryptIVSet || _ignoreIVSet) {
				_decryptIVSet = true;
				setIV(data, false);
                temp = Arrays.copyOfRange(data,_ivLength,data.length);
			} else {
				temp = data;
			}

			_decrypt(temp, stream);
		}
	}

	@Override
	public void decrypt(byte[] data, int length, ByteArrayOutputStream stream) {
		byte[] d = Arrays.copyOfRange(data,0,length);
		decrypt(d, stream);
	}

	private byte[] randomBytes(int size) {
		byte[] bytes = new byte[size];
		new SecureRandom().nextBytes(bytes);
		return bytes;
	}

	protected abstract StreamCipher getCipher(boolean isEncrypted)
			throws InvalidAlgorithmParameterException;

	protected abstract SecretKey getKey();

	protected abstract void _encrypt(byte[] data, ByteArrayOutputStream stream);

	protected abstract void _decrypt(byte[] data, ByteArrayOutputStream stream);

	protected abstract int getIVLength();

	protected abstract int getKeyLength();
}
