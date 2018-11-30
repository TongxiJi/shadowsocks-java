package cn.wowspeeder.encryption;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import cn.wowspeeder.encryption.impl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CryptFactory {

    private static Logger logger = LoggerFactory.getLogger(CryptFactory.class);

    private static Map<String, String> crypts = new HashMap<String, String>();

    static {
        crypts.putAll(AesCrypt.getCiphers());
        crypts.putAll(CamelliaCrypt.getCiphers());
        crypts.putAll(BlowFishCrypt.getCiphers());
        crypts.putAll(SeedCrypt.getCiphers());
        crypts.putAll(Rc4Md5Crypt.getCiphers());
        crypts.putAll(Chacha20Crypt.getCiphers());
        crypts.putAll(AesGcmCrypt.getCiphers());
    }

    public static ICrypt get(String name, String password, boolean forUdp) {
        String className = crypts.get(name);
        if (className == null) {
            return null;
        }

        try {
            Class<?> clazz = Class.forName(className);
            Constructor<?> constructor = clazz.getConstructor(String.class,
                    String.class);
            ICrypt crypt = (ICrypt) constructor.newInstance(name, password);
            crypt.isForUdp(forUdp);
            return crypt;
        } catch (Exception e) {
            logger.error("get crypt error", e);
        }

        return null;
    }

    public static ICrypt get(String name, String password) {
        return get(name, password, false);
    }
}
