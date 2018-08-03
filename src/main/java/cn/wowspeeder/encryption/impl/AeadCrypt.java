package cn.wowspeeder.encryption.impl;

import cn.wowspeeder.encryption.CryptAeadBase;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.modes.AEADBlockCipher;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.HKDFParameters;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.util.HashMap;
import java.util.Map;


//TODO unfinished
public class AeadCrypt extends CryptAeadBase {

    public final static String CIPHER_AEAD_128_GCM = "aes-128-gcm";
    public final static String CIPHER_AEAD_192_GCM = "aes-192-gcm";
    public final static String CIPHER_AEAD_256_GCM = "aes-256-gcm";

    public final static byte[] SUB_KEY_INFO = "ss-subkey".getBytes();

    public static Map<String, String> getCiphers() {
        Map<String, String> ciphers = new HashMap<>();
        ciphers.put(CIPHER_AEAD_128_GCM, AeadCrypt.class.getName());
        ciphers.put(CIPHER_AEAD_192_GCM, AeadCrypt.class.getName());
        ciphers.put(CIPHER_AEAD_256_GCM, AeadCrypt.class.getName());

        return ciphers;
    }

    public AeadCrypt(String name, String password) {
        super(name, password);
    }

    //	Nonce Size
    @Override
    public int getKeyLength() {
        switch (_name) {
            case CIPHER_AEAD_128_GCM:
                return 16;
            case CIPHER_AEAD_192_GCM:
                return 24;
            case CIPHER_AEAD_256_GCM:
                return 32;
        }
        return 0;
    }

    @Override
    protected AEADBlockCipher getCipher(boolean isEncrypted)
            throws InvalidAlgorithmParameterException {
        AESEngine engine = new AESEngine();
        GCMBlockCipher cipher = null;

        switch (_name) {
            case CIPHER_AEAD_128_GCM:
                cipher = new GCMBlockCipher(engine);
                break;
            case CIPHER_AEAD_192_GCM:
                cipher = new GCMBlockCipher(engine);
                break;
            case CIPHER_AEAD_256_GCM:
                cipher = new GCMBlockCipher(engine);
                break;
            default:
                throw new InvalidAlgorithmParameterException(_name);
        }

        return cipher;
    }

    @Override
    public int getSaltLength() {
        switch (_name) {
            case CIPHER_AEAD_128_GCM:
                return 16;
            case CIPHER_AEAD_192_GCM:
                return 24;
            case CIPHER_AEAD_256_GCM:
                return 32;
        }
        return 0;
    }

    @Override
    protected SecretKey getKey() {
        return new SecretKeySpec(_ssKey.getEncoded(), "AES");
    }

    @Override
    protected int getNonceLength() {
        return 12;
    }

    @Override
    protected int getTagLength() {
        return 16;
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


    private static byte[] hkdfSHA1(String secret, byte[] salt, byte[] info, int subKeyLength) {
        Digest hash = new SHA1Digest();
        byte[] ikm = secret.getBytes();
        byte[] okm = new byte[subKeyLength];
        HKDFParameters params = new HKDFParameters(ikm, salt, info);
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(hash);
        hkdf.init(params);
        hkdf.generateBytes(okm, 0, okm.length);
        return okm;
    }
}
