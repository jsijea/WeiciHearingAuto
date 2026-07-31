package io.weici.hearing;

import java.security.MessageDigest;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class CryptoUtils {
    private static final String SALT = "w*#%7@$&c";
    private static final String AES_KEY = "ac14c13680bdf7a0";

    public static String calcPasswordHash(String password) {
        try {
            String raw = SALT + password + SALT;
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(raw.getBytes("UTF-8"));
            return bytesToHex(digest);
        } catch (Exception e) {
            throw new RuntimeException("MD5加密失败", e);
        }
    }

    public static String aesEcbEncrypt(String plaintext) {
        try {
            SecretKey key = new SecretKeySpec(AES_KEY.getBytes("UTF-8"), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(plaintext.getBytes("UTF-8"));
            return bytesToHex(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("AES加密失败", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
