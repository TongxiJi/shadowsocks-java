package cn.wowspeeder.encryption;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;

public class CryptUtil {

    private static Logger logger = LoggerFactory.getLogger(CryptUtil.class);

    public static byte[] encrypt(ICrypt crypt, Object msg) throws Exception {
        byte[] data = null;
        ByteArrayOutputStream _remoteOutStream = null;
        try {
            _remoteOutStream = new ByteArrayOutputStream(64 * 1024);
            ByteBuf bytebuff = (ByteBuf) msg;
            int len = bytebuff.readableBytes();
            byte[] arr = new byte[len];
            bytebuff.getBytes(0, arr);
            crypt.encrypt(arr, arr.length, _remoteOutStream);
            data = _remoteOutStream.toByteArray();
        } finally {
            if (_remoteOutStream != null) {
                try {
                    _remoteOutStream.close();
                } catch (IOException e) {
                }
            }
        }
        return data;
    }

    public static byte[] decrypt(ICrypt crypt, Object msg) throws Exception {
        byte[] data = null;
        ByteArrayOutputStream _localOutStream = null;
        try {
            _localOutStream = new ByteArrayOutputStream(64 * 1024);
            ByteBuf bytebuff = (ByteBuf) msg;
            int len = bytebuff.readableBytes();
            byte[] arr = new byte[len];
            bytebuff.getBytes(0, arr);
//            logger.debug("before:" + Arrays.toString(arr));
            crypt.decrypt(arr, arr.length, _localOutStream);
            data = _localOutStream.toByteArray();
//            logger.debug("after:" + Arrays.toString(data));
        } finally {
            if (_localOutStream != null) {
                try {
                    _localOutStream.close();
                } catch (IOException e) {
                }
            }
        }
        return data;
    }

}
