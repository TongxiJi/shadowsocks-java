package cn.wowspeeder.encryption.impl;

import cn.wowspeeder.encryption.CryptAeadBase;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.AEADBlockCipher;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.util.HashMap;
import java.util.Map;


public class AesGcmCrypt extends CryptAeadBase {
    private static Logger logger = LoggerFactory.getLogger(AesGcmCrypt.class);

    public final static String CIPHER_AEAD_128_GCM = "aes-128-gcm";
    //    public final static String CIPHER_AEAD_192_GCM = "aes-192-gcm";
    public final static String CIPHER_AEAD_256_GCM = "aes-256-gcm";

    public static Map<String, String> getCiphers() {
        Map<String, String> ciphers = new HashMap<>();
        ciphers.put(CIPHER_AEAD_128_GCM, AesGcmCrypt.class.getName());
//        ciphers.put(CIPHER_AEAD_192_GCM, AesGcmCrypt.class.getName());
        ciphers.put(CIPHER_AEAD_256_GCM, AesGcmCrypt.class.getName());

        return ciphers;
    }

    public AesGcmCrypt(String name, String password) {
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
    protected void _tcpEncrypt(byte[] data, ByteArrayOutputStream stream) throws GeneralSecurityException, IOException, InvalidCipherTextException {
//        byte[] buffer = new byte[data.length];
//        int noBytesProcessed = encCipher.processBytes(data, 0, data.length, buffer, 0);
//        stream.write(buffer, 0, noBytesProcessed);
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

    /**
     * @param data
     * @param stream
     * @throws InvalidCipherTextException
     */
    @Override
    protected void _tcpDecrypt(byte[] data, ByteArrayOutputStream stream) throws InvalidCipherTextException {
//        byte[] buffer = new byte[data.length];
//        int noBytesProcessed = decCipher.processBytes(data, 0, data.length, buffer,
//                0);
//        logger.debug("remaining _tcpDecrypt");
//        stream.write(buffer, 0, noBytesProcessed);
//        logger.debug("ciphertext len:{}", data.length);
        ByteBuffer buffer = ByteBuffer.wrap(data);
        while (buffer.hasRemaining()) {
            logger.debug("id:{} remaining {} payloadLenRead:{} payloadRead:{}", hashCode(), buffer.hasRemaining(), payloadLenRead, payloadRead);
            if (payloadRead == 0) {
//                [encrypted payload length][length tag]
                int wantLen = 2 + getTagLength() - payloadLenRead;
                int remaining = buffer.remaining();
                if (wantLen <= remaining) {
                    buffer.get(decBuffer, payloadLenRead, wantLen);
                } else {
                    buffer.get(decBuffer, payloadLenRead, remaining);
                    payloadLenRead += remaining;
                    return;
                }
                decCipher.init(false, getCipherParameters(false));
                decCipher.doFinal(
                        decBuffer,
                        decCipher.processBytes(decBuffer, 0, 2 + getTagLength(), decBuffer, 0)
                );
                increment(decNonce);
            }


//            [encrypted payload length][length tag]
            int size = ByteBuffer.wrap(decBuffer, 0, 2).getShort();
            logger.debug("payload length:{},remaining:{},payloadRead:{}", size, buffer.remaining(), payloadRead);
            if (size == 0) {
                //TODO exists?
                return;
            } else {
                int wantLen = getTagLength() + size - payloadRead;
                int remaining = buffer.remaining();
                if (wantLen <= remaining) {
                    buffer.get(decBuffer, 2 + getTagLength() + payloadRead, wantLen);
                } else {
                    buffer.get(decBuffer, 2 + getTagLength() + payloadRead, remaining);
                    payloadRead += remaining;
                    return;
                }
            }

            decCipher.init(false, getCipherParameters(false));
            decCipher.doFinal(
                    decBuffer,
                    (2 + getTagLength()) + decCipher.processBytes(decBuffer, 2 + getTagLength(), size + getTagLength(), decBuffer, 2 + getTagLength())
            );
            increment(decNonce);

            payloadLenRead = 0;
            payloadRead = 0;

            stream.write(decBuffer, 2 + getTagLength(), size);
//            logger.debug("cipher text decode finish");
        }
    }

    @Override
    protected void _udpEncrypt(byte[] data, ByteArrayOutputStream stream) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        int remaining = buffer.remaining();
        buffer.get(encBuffer, 0, remaining);
        encCipher.init(true, getCipherParameters(true));
        encCipher.doFinal(
                encBuffer,
                encCipher.processBytes(encBuffer, 0, remaining, encBuffer, 0)
        );
        stream.write(encBuffer, 0, remaining + getTagLength());
    }

    @Override
    protected void _udpDecrypt(byte[] data, ByteArrayOutputStream stream) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        int remaining = buffer.remaining();
        buffer.get(decBuffer, 0, remaining);
        decCipher.init(false, getCipherParameters(false));
        decCipher.doFinal(
                decBuffer,
                decCipher.processBytes(decBuffer, 0, remaining, decBuffer, 0)
        );
        stream.write(decBuffer, 0, remaining - getTagLength());
    }
}