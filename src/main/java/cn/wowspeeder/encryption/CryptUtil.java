package cn.wowspeeder.encryption;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;

public class CryptUtil {

	private static Logger logger = LoggerFactory.getLogger(CryptUtil.class);

	public static byte[] encrypt(ICrypt crypt, Object msg) {
		byte[] data = null;
		ByteArrayOutputStream _remoteOutStream = null;
		try {
			_remoteOutStream = new ByteArrayOutputStream();
			ByteBuf bytebuff = (ByteBuf) msg;
//			if (!bytebuff.hasArray()) {
				int len = bytebuff.readableBytes();
				byte[] arr = new byte[len];
				bytebuff.getBytes(0, arr);
				crypt.encrypt(arr, arr.length, _remoteOutStream);
				data = _remoteOutStream.toByteArray();
//			}
		} catch (Exception e) {
			logger.error("encrypt error", e);
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

	public static byte[] decrypt(ICrypt crypt, Object msg) {
		byte[] data = null;
		ByteArrayOutputStream _localOutStream = null;
		try {
			_localOutStream = new ByteArrayOutputStream();
			ByteBuf bytebuff = (ByteBuf) msg;
//			if (!bytebuff.hasArray()) {
				int len = bytebuff.readableBytes();
				byte[] arr = new byte[len];
				bytebuff.getBytes(0, arr);
				crypt.decrypt(arr, arr.length, _localOutStream);
				data = _localOutStream.toByteArray();
//			}
		} catch (Exception e) {
			logger.error("encrypt error", e);
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
