package io.cordys.common.util.rsa;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * RSA 加密解密工具类，提供 RSA 算法相关的加密和解密操作。
 * <p>
 * 该类支持使用 RSA 公钥和私钥进行加密、解密、密钥生成等操作。
 * </p>
 */
public class RsaUtils {

    /**
     * 默认字符集 UTF-8。
     */
    public static final String CHARSET = StandardCharsets.UTF_8.name();

    /**
     * RSA 加密算法名称。
     */
    public static final String RSA_ALGORITHM = "RSA/ECB/OAEPWITHSHA-256ANDMGF1PADDING";

    /**
     * RSA 密钥对，缓存的 RSA 公私钥对象。
     */
    private static RsaKey rsaKey;

    /**
     * 获取缓存的 RSA 密钥对，如果未创建则调用方法生成。
     *
     * @return 已缓存的 RSA 密钥对
     * @throws NoSuchAlgorithmException 如果找不到指定的算法
     */
    public static RsaKey getRsaKey() throws NoSuchAlgorithmException {
        if (rsaKey == null) {
            rsaKey = createKeys();
        }
        return rsaKey;
    }

    /**
     * 设置 RSA 密钥对缓存。
     *
     * @param rsaKey RSA 密钥对
     * @throws NoSuchAlgorithmException 如果找不到指定的算法
     */
    public static void setRsaKey(RsaKey rsaKey) throws NoSuchAlgorithmException {
        RsaUtils.rsaKey = rsaKey;
    }

    /**
     * 创建一个新的 RSA 密钥对，默认密钥长度为 1024 位。
     *
     * @return 生成的 RSA 密钥对
     * @throws NoSuchAlgorithmException 如果找不到指定的算法
     */
    public static RsaKey createKeys() throws NoSuchAlgorithmException {
        return createKeys(1024);
    }

    /**
     * 创建 RSA 密钥对，指定密钥长度。
     *
     * @param keySize 密钥长度，推荐 1024 或 2048 位
     * @return 生成的 RSA 密钥对
     * @throws NoSuchAlgorithmException 如果找不到指定的算法
     */
    public static RsaKey createKeys(int keySize) throws NoSuchAlgorithmException {
        // 为 RSA 算法创建 KeyPairGenerator 对象
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(RSA_ALGORITHM);
        kpg.initialize(keySize); // 初始化 KeyPairGenerator 对象，设置密钥长度
        KeyPair keyPair = kpg.generateKeyPair(); // 生成密钥对

        // 获取公钥并编码为 Base64 字符串
        Key publicKey = keyPair.getPublic();
        String publicKeyStr = new String(Base64.encodeBase64(publicKey.getEncoded()));

        // 获取私钥并编码为 Base64 字符串
        Key privateKey = keyPair.getPrivate();
        String privateKeyStr = new String(Base64.encodeBase64(privateKey.getEncoded()));

        // 创建 RsaKey 对象并设置公私钥
        RsaKey rsaKey = new RsaKey();
        rsaKey.setPublicKey(publicKeyStr);
        rsaKey.setPrivateKey(privateKeyStr);

        return rsaKey;
    }

    /**
     * 使用公钥加密数据。
     *
     * @param originalText 原文
     * @param publicKey    公钥
     * @return 加密后的密文
     * @throws NoSuchAlgorithmException 如果找不到指定的算法
     */
    public static String publicEncrypt(String originalText, String publicKey) throws NoSuchAlgorithmException {
        RSAPublicKey rsaPublicKey = getPublicKey(publicKey);
        return publicEncrypt(originalText, rsaPublicKey);
    }

    /**
     * 使用公钥解密数据。
     *
     * @param cipherText 密文
     * @param publicKey  公钥
     * @return 解密后的原文
     * @throws NoSuchAlgorithmException 如果找不到指定的算法
     */
    public static String publicDecrypt(String cipherText, String publicKey) throws NoSuchAlgorithmException {
        RSAPublicKey rsaPublicKey = getPublicKey(publicKey);
        return publicDecrypt(cipherText, rsaPublicKey);
    }

    private static String publicDecrypt(String cipherText, RSAPublicKey publicKey) {
        try {
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, publicKey);
            String v = new String(rsaSplitCodec(cipher, Cipher.DECRYPT_MODE, Base64.decodeBase64(cipherText), publicKey.getModulus().bitLength()), CHARSET);
            if (StringUtils.isBlank(v)) {
                return cipherText;
            }
            return v;
        } catch (Exception e) {
            throw new RuntimeException("解密字符串[" + cipherText + "]时遇到异常", e);
        }
    }

    /**
     * 使用私钥加密数据。
     *
     * @param originalText 原文
     * @param privateKey   私钥
     * @return 加密后的密文
     * @throws NoSuchAlgorithmException 如果找不到指定的算法
     */
    public static String privateEncrypt(String originalText, String privateKey) throws NoSuchAlgorithmException {
        RSAPrivateKey rsaPrivateKey = getPrivateKey(privateKey);
        return privateEncrypt(originalText, rsaPrivateKey);
    }

    /**
     * 使用私钥解密数据。
     *
     * @param cipherText 密文
     * @param privateKey 私钥
     * @return 解密后的原文
     * @throws NoSuchAlgorithmException 如果找不到指定的算法
     */
    public static String privateDecrypt(String cipherText, String privateKey) throws NoSuchAlgorithmException {
        RSAPrivateKey rsaPrivateKey = getPrivateKey(privateKey);
        return privateDecrypt(cipherText, rsaPrivateKey);
    }

    /**
     * 根据公钥字符串获取公钥对象。
     *
     * @param publicKey 公钥字符串（Base64 编码）
     * @return 公钥对象
     * @throws NoSuchAlgorithmException 如果找不到指定的算法
     */
    private static RSAPublicKey getPublicKey(String publicKey) throws NoSuchAlgorithmException {
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(Base64.decodeBase64(publicKey));
        try {
            return (RSAPublicKey) keyFactory.generatePublic(x509KeySpec);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException("公钥解析失败", e);
        }
    }

    /**
     * 使用公钥加密数据。
     *
     * @param originalText 原文
     * @param publicKey    公钥对象
     * @return 加密后的密文
     */
    private static String publicEncrypt(String originalText, RSAPublicKey publicKey) {
        try {
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return Base64.encodeBase64URLSafeString(rsaSplitCodec(cipher, Cipher.ENCRYPT_MODE, originalText.getBytes(CHARSET), publicKey.getModulus().bitLength()));
        } catch (Exception e) {
            throw new RuntimeException("加密时遇到异常", e);
        }
    }

    /**
     * 根据私钥字符串获取私钥对象。
     *
     * @param privateKey 私钥字符串（Base64 编码）
     * @return 私钥对象
     * @throws NoSuchAlgorithmException 如果找不到指定的算法
     */
    private static RSAPrivateKey getPrivateKey(String privateKey) throws NoSuchAlgorithmException {
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(Base64.decodeBase64(privateKey));
        try {
            return (RSAPrivateKey) keyFactory.generatePrivate(pkcs8KeySpec);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException("私钥解析失败", e);
        }
    }

    /**
     * 使用私钥解密数据。
     *
     * @param cipherText 密文
     * @param privateKey 私钥对象
     * @return 解密后的原文
     */
    private static String privateDecrypt(String cipherText, RSAPrivateKey privateKey) {
        try {
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            String result = new String(rsaSplitCodec(cipher, Cipher.DECRYPT_MODE, Base64.decodeBase64(cipherText), privateKey.getModulus().bitLength()), CHARSET);
            return StringUtils.isBlank(result) ? cipherText : result;
        } catch (Exception e) {
            throw new RuntimeException("解密时遇到异常", e);
        }
    }

    /**
     * 私钥加密操作。
     *
     * @param originalText 原文
     * @param privateKey   私钥对象
     * @return 加密后的密文
     */
    private static String privateEncrypt(String originalText, RSAPrivateKey privateKey) {
        try {
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, privateKey);
            return Base64.encodeBase64URLSafeString(rsaSplitCodec(cipher, Cipher.ENCRYPT_MODE, originalText.getBytes(CHARSET), privateKey.getModulus().bitLength()));
        } catch (Exception e) {
            throw new RuntimeException("加密时遇到异常", e);
        }
    }

    /**
     * 使用公钥或私钥进行加解密操作，分块处理数据。
     *
     * @param cipher  加解密的 Cipher 对象
     * @param opmode  操作模式（加密或解密）
     * @param data    要加解密的数据
     * @param keySize 密钥长度
     * @return 加解密后的字节数组
     */
    private static byte[] rsaSplitCodec(Cipher cipher, int opmode, byte[] data, int keySize) {
        int maxBlock = (opmode == Cipher.DECRYPT_MODE) ? keySize / 8 : keySize / 8 - 11;
        int offSet = 0;
        byte[] buff;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            while (data.length > offSet) {
                int length = Math.min(data.length - offSet, maxBlock);
                buff = cipher.doFinal(data, offSet, length);
                out.write(buff, 0, buff.length);
                offSet += length;
            }
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("加解密时发生异常", e);
        }
    }
}
