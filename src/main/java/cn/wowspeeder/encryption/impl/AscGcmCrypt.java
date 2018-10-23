package cn.wowspeeder.encryption.impl;

import cn.wowspeeder.encryption.CryptAeadBase;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.AEADBlockCipher;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class AscGcmCrypt extends CryptAeadBase {
    private static Logger logger = LoggerFactory.getLogger(AscGcmCrypt.class);

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

    /**
     * TCP:[encrypted payload length][length tag][encrypted payload][payload tag]
     * UDP:[salt][encrypted payload][tag]
     * //TODO need return multi chunks
     *
     * @param data
     * @param stream
     * @throws GeneralSecurityException
     * @throws IOException
     */
    @Override
    protected void _encrypt(byte[] data, ByteArrayOutputStream stream) throws GeneralSecurityException, IOException, InvalidCipherTextException {
//        byte[] buffer = new byte[data.length];
//        int noBytesProcessed = encCipher.processBytes(data, 0, data.length, buffer, 0);
//        stream.write(buffer, 0, noBytesProcessed);
        logger.debug("_encrypt data length:{}", data.length);

        ByteBuffer buffer = ByteBuffer.wrap(data);
        while (buffer.hasRemaining()) {
            int nr = Math.min(buffer.remaining(), PAYLOAD_SIZE_MASK);
            ByteBuffer.wrap(encBuffer).putShort((short) nr);
            encCipher.init(true, getCipherParameters(true));
            encCipher.doFinal(
                    encBuffer,
                    encCipher.processBytes(encBuffer, 0, 2, encBuffer, 0)
            );
            stream.write(encBuffer, 0, 2 + getTagLength());
            increment(this.encNonce);

            buffer.get(encBuffer, 2 + getTagLength(), nr);

            encCipher.init(true, getCipherParameters(true));
            encCipher.doFinal(
                    encBuffer,
                    2 + getTagLength() + encCipher.processBytes(encBuffer, 2 + getTagLength(), nr, encBuffer, 2 + getTagLength())
            );
            increment(this.encNonce);

            stream.write(encBuffer, 2 + getTagLength(), nr + getTagLength());
        }
    }

    @Override
    protected void _decrypt(byte[] data, ByteArrayOutputStream stream) throws InvalidCipherTextException {
//        byte[] buffer = new byte[data.length];
//        int noBytesProcessed = decCipher.processBytes(data, 0, data.length, buffer,
//                0);
//        logger.debug("remaining _decrypt");
//        stream.write(buffer, 0, noBytesProcessed);
        logger.debug("ciphertext len:{}", data.length);
        ByteBuffer buffer = ByteBuffer.wrap(data);
//        [encrypted payload length][length tag]

        logger.debug("id:{} remaining {}", hashCode(), buffer.hasRemaining());
        while (buffer.hasRemaining()) {

            buffer.get(decBuffer, 0, 2 + getTagLength());
            decCipher.init(false, getCipherParameters(false));
            decCipher.doFinal(
                    decBuffer,
                    decCipher.processBytes(decBuffer, 0, 2 + getTagLength(), decBuffer, 0)
            );
            increment(decNonce);
//        logger.debug(Arrays.toString(decBuffer));
            int size = ByteBuffer.wrap(decBuffer, 0, 2).getShort();
            logger.debug("payload length:{},remaining:{}", size, buffer.remaining());
            if (size == 0) {
                return;
            }
            //TODO when remaining < size + getTagLength(), should read bytes from tcp conn util full of “size + getTagLength()”
            buffer.get(decBuffer, 0, size + getTagLength());
            decCipher.init(false, getCipherParameters(false));
            decCipher.doFinal(
                    decBuffer,
                    decCipher.processBytes(decBuffer, 0, size + getTagLength(), decBuffer, 0)
            );
            increment(decNonce);
            stream.write(decBuffer, 0, size);
        }
    }
}