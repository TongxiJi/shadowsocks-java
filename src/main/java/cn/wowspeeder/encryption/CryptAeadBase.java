package cn.wowspeeder.encryption;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.modes.AEADBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.bouncycastle.crypto.params.KeyParameter;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


//TODO unfinished
public abstract class CryptAeadBase implements ICrypt {
    private static InternalLogger logger = InternalLoggerFactory.getInstance(CryptAeadBase.class);

    protected static int PAYLOAD_SIZE_MASK = 0x3FFF;

    private static byte[] info = "ss-subkey".getBytes();


    protected final String _name;
    protected final ShadowSocksKey _ssKey;
    protected final int _keyLength;
    protected boolean _ignoreSaltSet;
    protected boolean _encryptSaltSet;
    protected boolean _decryptSaltSet;
    protected final Lock encLock = new ReentrantLock();
    protected final Lock decLock = new ReentrantLock();
    protected AEADBlockCipher encCipher;
    protected AEADBlockCipher decCipher;
    private byte[] encSubkey;
    private byte[] decSubkey;
    protected byte[] encNonce = new byte[getNonceLength()];
    protected byte[] decNonce = new byte[getNonceLength()];

    protected ByteBuffer encBuffer = ByteBuffer.allocate(2 + getTagLength() + PAYLOAD_SIZE_MASK + getTagLength());
    protected ByteBuffer decBuffer = ByteBuffer.allocate(PAYLOAD_SIZE_MASK + getTagLength());

    public CryptAeadBase(String name, String password) {
        _name = name.toLowerCase();
        _keyLength = getKeyLength();
        _ssKey = new ShadowSocksKey(password, _keyLength);
    }

    @Override
    public void saltSetIgnore(boolean ignore) {
        this._ignoreSaltSet = ignore;
    }

    private byte[] genSubkey(byte[] salt) {
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA1Digest());
        hkdf.init(new HKDFParameters(_ssKey.getEncoded(), salt, info));
        byte[] okm = new byte[getKeyLength()];
        hkdf.generateBytes(okm, 0, getKeyLength());
        return okm;
    }


    protected byte[] increment(byte[] nonce) {
        for (int i = 0; i < nonce.length; i++) {
            ++nonce[i];
            if (nonce[i] != 0) {
                break;
            }
        }
        return nonce;
    }


    protected CipherParameters getCipherParameters(boolean forEncryption) {
        AEADParameters parameters = new AEADParameters(
                new KeyParameter(forEncryption ? encSubkey : decSubkey),
                getTagLength() * 8,
                forEncryption ? encNonce : decNonce);
        return parameters;
    }

    @Override
    public void encrypt(byte[] data, ByteArrayOutputStream stream) throws GeneralSecurityException, IOException {
        synchronized (encLock) {
            stream.reset();
            if (!_encryptSaltSet || _ignoreSaltSet) {
                byte[] salt = randomBytes(getSaltLength());
                stream.write(salt);
                encSubkey = genSubkey(salt);
                encCipher = getCipher(true);
                _encryptSaltSet = true;
            }
            _encrypt(data, stream);
        }
    }

    @Override
    public void encrypt(byte[] data, int length, ByteArrayOutputStream stream) throws GeneralSecurityException, IOException {
        byte[] d = Arrays.copyOfRange(data, 0, length);
        encrypt(d, stream);
    }

    @Override
    public void decrypt(byte[] data, ByteArrayOutputStream stream) throws GeneralSecurityException, IOException {
        byte[] temp;
        synchronized (decLock) {
            stream.reset();
            ByteBuffer buffer = ByteBuffer.wrap(data);
            if (decCipher == null || _ignoreSaltSet) {
                _decryptSaltSet = true;
                byte[] salt = new byte[getSaltLength()];
                buffer.get(salt);
                decSubkey = genSubkey(salt);
                decCipher = getCipher(false);
                temp = new byte[buffer.remaining()];
                buffer.get(temp);
            } else {
                temp = data;
            }
            _decrypt(temp, stream);
        }
    }

    @Override
    public void decrypt(byte[] data, int length, ByteArrayOutputStream stream) throws GeneralSecurityException, IOException {
        byte[] d = Arrays.copyOfRange(data, 0, length);
        decrypt(d, stream);
    }

    private byte[] randomBytes(int size) {
        byte[] bytes = new byte[size];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    protected abstract AEADBlockCipher getCipher(boolean isEncrypted)
            throws GeneralSecurityException;

    protected abstract void _encrypt(byte[] data, ByteArrayOutputStream stream) throws GeneralSecurityException, IOException;

    protected abstract void _decrypt(byte[] data, ByteArrayOutputStream stream) throws GeneralSecurityException, IOException;

    protected abstract int getKeyLength();

    protected abstract int getSaltLength();

    protected abstract int getNonceLength();

    protected abstract int getTagLength();
}
