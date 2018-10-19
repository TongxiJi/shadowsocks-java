package cn.wowspeeder.encryption.impl;

import cn.wowspeeder.encryption.CryptAeadBase;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.AEADBlockCipher;
import org.bouncycastle.crypto.modes.GCMBlockCipher;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.util.HashMap;
import java.util.Map;


public class AscGcmCrypt extends CryptAeadBase {

    public final static String CIPHER_AEAD_128_GCM = "aes-128-gcm";
    //    public final static String CIPHER_AEAD_192_GCM = "aes-192-gcm";
    public final static String CIPHER_AEAD_256_GCM = "aes-256-gcm";

    public static Map<String, String> getCiphers() {
        Map<String, String> ciphers = new HashMap<>();
        ciphers.put(CIPHER_AEAD_128_GCM, AscGcmCrypt.class.getName());
//        ciphers.put(CIPHER_AEAD_192_GCM, AscGcmCrypt.class.getName());
        ciphers.put(CIPHER_AEAD_256_GCM, AscGcmCrypt.class.getName());

        return ciphers;
    }

    public AscGcmCrypt(String name, String password) {
        super(name, password);
    }

    //	Nonce Size
    @Override
    public int getKeyLength() {
        switch (_name) {
            case CIPHER_AEAD_128_GCM:
                return 16;
//            case CIPHER_AEAD_192_GCM:
//                return 24;
            case CIPHER_AEAD_256_GCM:
                return 32;
        }
        return 0;
    }

    @Override
    protected AEADBlockCipher getCipher(boolean isEncrypted)
            throws GeneralSecurityException {
        switch (_name) {
            case CIPHER_AEAD_128_GCM:
            case CIPHER_AEAD_256_GCM:
                return new GCMBlockCipher(new AESEngine());
            default:
                throw new InvalidAlgorithmParameterException(_name);
        }
    }

    @Override
    public int getSaltLength() {
        switch (_name) {
            case CIPHER_AEAD_128_GCM:
                return 16;
//            case CIPHER_AEAD_192_GCM:
//              return 24;
            case CIPHER_AEAD_256_GCM:
                return 32;
        }
        return 0;
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
    protected void _encrypt(byte[] data, ByteArrayOutputStream stream) throws GeneralSecurityException, IOException {
//        byte[] buffer = new byte[data.length];
//        int noBytesProcessed = encCipher.processBytes(data, 0, data.length, buffer, 0);
//        stream.write(buffer, 0, noBytesProcessed);
        decCipher.init(true, getCipherParameters(true));
        boolean readOut = false;

        ByteBuffer buffer = ByteBuffer.wrap(data);

        ByteBuffer chunkBuff = ByteBuffer.wrap(encBuffer.array(), 2 + getTagLength(), 2 + getTagLength() + PAYLOAD_SIZE_MASK);
        while (!readOut) {
            readOut = false;
        }
    }

    @Override
    protected void _decrypt(byte[] data, ByteArrayOutputStream stream) throws GeneralSecurityException, IOException {
        decCipher.init(false, getCipherParameters(false));
        byte[] buffer = new byte[data.length];
        int noBytesProcessed = decCipher.processBytes(data, 0, data.length, buffer,
                0);
        stream.write(buffer, 0, noBytesProcessed);
    }
}