package cn.wowspeeder.encryption;

import org.bouncycastle.crypto.InvalidCipherTextException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;

/**
 * crypt 加密
 *
 * @author zhaohui
 */
public interface ICrypt {

    void isForUdp(boolean isForUdp);

    void encrypt(byte[] data, ByteArrayOutputStream stream) throws Exception;

    void encrypt(byte[] data, int length, ByteArrayOutputStream stream) throws Exception;

    void decrypt(byte[] data, ByteArrayOutputStream stream) throws Exception;

    void decrypt(byte[] data, int length, ByteArrayOutputStream stream) throws Exception;

}
