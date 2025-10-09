package cn.cordys.common.util;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * 加密解密工具类，提供 MD5、BASE64 和 AES 加密解密操作。
 * 支持常见的加密解密算法，简化了加密过程。
 */
public class CodingUtils {

    private static final String UTF_8 = "UTF-8";
    private static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    /**
     * 加密偏移量，AES 加密的初始化向量。
     */
    private static final String GCM_IV = "1Av7hf9PgHusUHRm";
    private static final int GCM_TAG_LENGTH = 128; // GCM 标签长度（以位为单位）

    /**
     * MD5加密（默认UTF-8字符集）
     *
     * @param src 要加密的字符串
     *
     * @return 加密后的MD5字符串
     */
    public static String md5(String src) {
        return md5(src, UTF_8);
    }

    /**
     * MD5加密
     *
     * @param src     要加密的字符串
     * @param charset 使用的字符集
     *
     * @return 加密后的MD5字符串
     */
    public static String md5(String src, String charset) {
        if (StringUtils.isBlank(src)) {
            throw new IllegalArgumentException("Input for MD5 cannot be null or empty");
        }

        try {
            byte[] strTemp = src.getBytes(StringUtils.defaultIfBlank(charset, UTF_8));
            MessageDigest mdTemp = MessageDigest.getInstance("MD5");
            mdTemp.update(strTemp);

            byte[] md = mdTemp.digest();
            char[] str = new char[md.length * 2];
            int k = 0;

            for (byte byte0 : md) {
                str[k++] = HEX_DIGITS[(byte0 >>> 4) & 0xf];
                str[k++] = HEX_DIGITS[byte0 & 0xf];
            }

            return new String(str);
        } catch (Exception e) {
            throw new RuntimeException("MD5 encrypt error:", e);
        }
    }

    /**
     * BASE64解密
     *
     * @param src 待解密的字符串
     *
     * @return 解密后的字符串
     */
    public static String base64Decoding(String src) {
        if (StringUtils.isBlank(src)) {
            throw new IllegalArgumentException("Input for BASE64 decoding cannot be null or empty");
        }

        try {
            byte[] decodedBytes = Base64.decodeBase64(src);
            return new String(decodedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("BASE64 decoding error:", e);
        }
    }

    /**
     * BASE64加密
     *
     * @param src 待加密的字符串
     *
     * @return 加密后的字符串
     */
    public static String base64Encoding(String src) {
        if (StringUtils.isBlank(src)) {
            throw new IllegalArgumentException("Input for BASE64 encoding cannot be null or empty");
        }

        try {
            return Base64.encodeBase64String(src.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("BASE64 encoding error:", e);
        }
    }

    /**
     * AES-GCM加密
     *
     * @param src       待加密的字符串
     * @param secretKey 加密密钥（16字节）
     * @param iv        初始向量（12字节）
     *
     * @return 加密后的字符串
     */
    public static String aesEncrypt(String src, String secretKey, byte[] iv) {
        if (StringUtils.isBlank(src) || StringUtils.isBlank(secretKey)) {
            throw new IllegalArgumentException("Input or secretKey cannot be null or empty");
        }

        try {
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] encryptedBytes = cipher.doFinal(src.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeBase64String(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("AES-GCM encrypt error:", e);
        }
    }

    /**
     * AES-GCM解密
     *
     * @param src       待解密的字符串
     * @param secretKey 解密密钥（16字节）
     * @param iv        初始向量（12字节）
     *
     * @return 解密后的字符串
     */
    public static String aesDecrypt(String src, String secretKey, byte[] iv) {
        if (StringUtils.isBlank(src) || StringUtils.isBlank(secretKey)) {
            throw new IllegalArgumentException("Input or secretKey cannot be null or empty");
        }

        try {
            byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
            if (keyBytes.length != 16) {
                // 密钥不足16字节，填充或截断到16字节
                keyBytes = Arrays.copyOf(keyBytes, 16);
            }

            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            byte[] decodedBytes = Base64.decodeBase64(src);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES-GCM decrypt error:", e);
        }
    }

    /**
     * 生成一个新的AES密钥
     *
     * @return 生成的AES密钥（Base64编码）
     */
    public static String generateSecretKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(128);
            SecretKey secretKey = keyGen.generateKey();
            return Base64.encodeBase64URLSafeString(secretKey.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("Generate AES secret key error:", e);
        }
    }

    public static String generateAK() {
        // 生成长度为 20 的随机字母数字字符串
        return RandomStringUtils.secure().nextAlphabetic(16);
    }

    /**
     * 生成随机IV（用于AES-GCM）
     *
     * @return 随机生成的IV
     */
    public static byte[] generateIv() {
        return GCM_IV.getBytes();
    }

    /**
     * 计算字符串的哈希值（SHA-256）并返回其前16个字符的十六进制表示
     *
     * @param str 需要计算哈希值的字符串
     *
     * @return 字符串的哈希值（16个字符的十六进制表示）
     */
    public static String hashStr(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(str.getBytes(StandardCharsets.UTF_8));

            // 转换为十六进制字符串
            char[] hexChars = new char[digest.length * 2];
            int k = 0;
            for (byte b : digest) {
                hexChars[k++] = HEX_DIGITS[(b >>> 4) & 0xf];
                hexChars[k++] = HEX_DIGITS[b & 0xf];
            }

            // 只使用前16个字符，平衡长度和唯一性
            return new String(hexChars, 0, Math.min(16, hexChars.length));
        } catch (NoSuchAlgorithmException e) {
            // 降级方案：使用多种哈希组合减少冲突
            return str.hashCode() + "-" + str.length();
        }
    }

    public static String aesCBCEncrypt(String src, String secretKey, String iv) {
        if (StringUtils.isBlank(src)) {
            return src;
        }
        if (StringUtils.isBlank(secretKey)) {
            throw new IllegalArgumentException("Input or secretKey cannot be null or empty");
        }
        try {
            byte[] raw = secretKey.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKeySpec = new SecretKeySpec(raw, "AES");
            // "算法/模式/补码方式" ECB
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            IvParameterSpec iv1 = new IvParameterSpec(iv.getBytes());
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, iv1);
            byte[] encrypted = cipher.doFinal(src.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeBase64String(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("AES encrypt error:", e);
        }
    }
}
