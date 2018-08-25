package cn.wowspeeder.encryption;

import org.bouncycastle.asn1.cms.GCMParameters;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.modes.AEADBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;

import javax.crypto.SecretKey;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;


//TODO unfinished
public abstract class CryptAeadBase implements ICrypt {
	private Logger logger = Logger.getLogger(CryptAeadBase.class.getName());

	protected final String _name;
	protected final SecretKey _key;
	protected final ShadowSocksKey _ssKey;
	protected final int _subKeyLength;
	protected final int _keyLength;
	protected boolean _ignoreSubKeySet;
	protected boolean _encryptSubKeySet;
	protected boolean _decryptSubKeySet;
	protected byte[] _encryptSubKey;
	protected byte[] _decryptSubKey;
	protected final Lock encLock = new ReentrantLock();
	protected final Lock decLock = new ReentrantLock();
	protected AEADBlockCipher encCipher;
	protected AEADBlockCipher decCipher;

	public CryptAeadBase(String name, String password) {
		_name = name.toLowerCase();
		_subKeyLength = getSaltLength();
		_keyLength = getKeyLength();
		_ssKey = new ShadowSocksKey(password, _keyLength);
		_key = getKey();
	}

	@Override
	public void ivSetIgnore(boolean ignore) {
		this._ignoreSubKeySet = ignore;
	}

	protected void setSubKey(byte[] subKey, boolean isEncrypt) {
		if (_subKeyLength == 0) {
			return;
		}
		AEADParameters cipherParameters = null;

		if (isEncrypt) {
			cipherParameters = getCipherParameters(subKey);
			try {
				encCipher = getCipher(isEncrypt);
			} catch (InvalidAlgorithmParameterException e) {
				logger.info(e.toString());
			}
			encCipher.init(isEncrypt, cipherParameters);
		} else {
            _decryptSubKey = Arrays.copyOfRange(subKey,0, _subKeyLength);
			cipherParameters = getCipherParameters(subKey);
			try {
				decCipher = getCipher(isEncrypt);
			} catch (InvalidAlgorithmParameterException e) {
				logger.info(e.toString());
			}
			decCipher.init(isEncrypt, cipherParameters);
		}
	}

	protected AEADParameters getCipherParameters(byte[] subKey){
		_decryptSubKey = Arrays.copyOfRange(subKey,0, _subKeyLength);
		return null;//new AEADParameters(new KeyParameter(subKey));
	}

	@Override
	public void encrypt(byte[] data, ByteArrayOutputStream stream) {
		synchronized (encLock) {
			stream.reset();
			if (!_encryptSubKeySet || _ignoreSubKeySet) {
				_encryptSubKeySet = true;
				byte[] subKey = randomBytes(_subKeyLength);
				setSubKey(subKey, true);
				try {
					stream.write(subKey);
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
			if (!_decryptSubKeySet || _ignoreSubKeySet) {
				_decryptSubKeySet = true;
				setSubKey(data, false);
                temp = Arrays.copyOfRange(data, _subKeyLength,data.length);
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

	protected abstract AEADBlockCipher getCipher(boolean isEncrypted)
			throws InvalidAlgorithmParameterException;

	protected abstract SecretKey getKey();

	protected abstract void _encrypt(byte[] data, ByteArrayOutputStream stream);

	protected abstract void _decrypt(byte[] data, ByteArrayOutputStream stream);

	protected abstract int getKeyLength();

	protected abstract int getSaltLength();

	protected abstract  int getNonceLength();

	protected abstract int getTagLength();
}
