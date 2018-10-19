package cn.wowspeeder.encryption;

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

    void saltSetIgnore(boolean ignore);

    void encrypt(byte[] data, ByteArrayOutputStream stream) throws GeneralSecurityException, IOException;

    void encrypt(byte[] data, int length, ByteArrayOutputStream stream) throws GeneralSecurityException, IOException;

    void decrypt(byte[] data, ByteArrayOutputStream stream) throws GeneralSecurityException, IOException;

    void decrypt(byte[] data, int length, ByteArrayOutputStream stream) throws GeneralSecurityException, IOException;

}
