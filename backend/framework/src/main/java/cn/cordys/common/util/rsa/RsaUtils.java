package cn.cordys.common.util.rsa;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA 加密解密工具类，提供 RSA 算法相关的加密、解密、密钥生成等操作。
 * <p>
 * 该类支持使用 RSA 公钥和私钥进行加密、解密、密钥生成等操作。
 * </p>
 */
public class RsaUtils {

    /**
     * 字符编码集
     */
    public static final String CHARSET = StandardCharsets.UTF_8.name();

    /**
     * RSA 算法名称
     */
    public static final String RSA_ALGORITHM = "RSA";

    /**
     * RSA 加密填充方式
     */
    private static final String RSA_CIPHER_TRANSFORMATION_OAEP = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

    /**
     * RSA 密钥对缓存
     */
    private static RsaKey rsaKey;

    /**
     * 获取当前的 RSA 密钥对，如果未创建则生成新的密钥对。
     *
     * @return 当前的 RSA 密钥对
     * @throws NoSuchAlgorithmException 如果无法生成密钥对
     */
    public static RsaKey getRsaKey() throws NoSuchAlgorithmException {
        if (rsaKey == null) {
            rsaKey = createKeys();
        }
        return rsaKey;
    }

    /**
     * 设置 RSA 密钥对。
     *
     * @param rsaKey RSA 密钥对
     */
    public static void setRsaKey(RsaKey rsaKey) {
        RsaUtils.rsaKey = rsaKey;
    }

    /**
     * 创建一个 1024 位的 RSA 密钥对。
     *
     * @return 生成的 RSA 密钥对
     * @throws NoSuchAlgorithmException 如果无法生成密钥对
     */
    public static RsaKey createKeys() throws NoSuchAlgorithmException {
        return createKeys(1024);
    }

    /**
     * 创建一个指定大小的 RSA 密钥对。
     *
     * @param keySize 密钥大小（例如：1024，2048）
     * @return 生成的 RSA 密钥对
     * @throws NoSuchAlgorithmException 如果无法生成密钥对
     */
    public static RsaKey createKeys(int keySize) throws NoSuchAlgorithmException {
        // 创建 RSA 密钥对生成器
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(RSA_ALGORITHM);
        kpg.initialize(keySize);

        // 生成密钥对
        KeyPair keyPair = kpg.generateKeyPair();

        // 获取公钥和私钥并进行 Base64 编码
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        String publicKeyStr = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        String privateKeyStr = Base64.getEncoder().encodeToString(privateKey.getEncoded());

        // 封装密钥对
        RsaKey rsaKey = new RsaKey();
        rsaKey.setPublicKey(publicKeyStr);
        rsaKey.setPrivateKey(privateKeyStr);

        return rsaKey;
    }

    /**
     * 使用私钥对原文进行加密。
     *
     * @param originalText 原文
     * @param privateKey   私钥（Base64 编码）
     * @return 加密后的密文
     * @throws NoSuchAlgorithmException 如果加密失败
     */
    public static String privateEncrypt(String originalText, String privateKey) throws Exception {
        RSAPrivateKey rsaPrivateKey = getPrivateKey(privateKey);
        return privateEncrypt(originalText, rsaPrivateKey);
    }

    /**
     * 使用私钥对原文进行加密。
     *
     * @param originalText 原文
     * @param privateKey   私钥
     * @return 加密后的密文
     * @throws Exception 如果加密失败
     */
    private static String privateEncrypt(String originalText, RSAPrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_CIPHER_TRANSFORMATION_OAEP);
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);

        byte[] encryptedData = rsaSplitCodec(cipher, Cipher.ENCRYPT_MODE, originalText.getBytes(CHARSET), privateKey.getModulus().bitLength());
        return Base64.getEncoder().encodeToString(encryptedData);
    }

    /**
     * 使用私钥对密文进行解密。
     *
     * @param cipherText 密文
     * @param privateKey 私钥（Base64 编码）
     * @return 解密后的原文
     * @throws NoSuchAlgorithmException 如果解密失败
     */
    public static String privateDecrypt(String cipherText, String privateKey) throws Exception {
        RSAPrivateKey rsaPrivateKey = getPrivateKey(privateKey);
        return privateDecrypt(cipherText, rsaPrivateKey);
    }

    /**
     * 使用私钥对密文进行解密。
     *
     * @param cipherText 密文
     * @param privateKey 私钥
     * @return 解密后的原文
     * @throws Exception 如果解密失败
     */
    private static String privateDecrypt(String cipherText, RSAPrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        byte[] decryptedData = rsaSplitCodec(cipher, Cipher.DECRYPT_MODE, Base64.getDecoder().decode(cipherText), privateKey.getModulus().bitLength());
        return new String(decryptedData, CHARSET);
    }

    /**
     * 获取私钥对象。
     *
     * @param privateKey 私钥（Base64 编码）
     * @return 私钥对象
     * @throws NoSuchAlgorithmException 如果无法获取私钥
     */
    private static RSAPrivateKey getPrivateKey(String privateKey) throws NoSuchAlgorithmException {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKey));
            return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException("无效的私钥格式", e);
        }
    }

    /**
     * 分块处理加密和解密过程，避免一次性数据过大导致的内存问题。
     *
     * @param cipher  加解密器
     * @param opmode  操作模式（加密/解密）
     * @param data    待加解密的数据
     * @param keySize 密钥大小
     * @return 处理后的数据
     * @throws Exception 如果处理过程中发生错误
     */
    private static byte[] rsaSplitCodec(Cipher cipher, int opmode, byte[] data, int keySize) throws Exception {
        int maxBlock = (opmode == Cipher.DECRYPT_MODE) ? keySize / 8 : keySize / 8 - 11;
        int offset = 0;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            while (offset < data.length) {
                int length = Math.min(maxBlock, data.length - offset);
                byte[] buffer = cipher.doFinal(data, offset, length);
                out.write(buffer, 0, buffer.length);
                offset += length;
            }
            return out.toByteArray();
        }
    }

    // 使用公钥加密
    public static String publicEncrypt(String originalText, String publicKey) throws Exception {
        RSAPublicKey rsaPublicKey = getPublicKey(publicKey);
        return publicEncrypt(originalText, rsaPublicKey);
    }

    private static RSAPublicKey getPublicKey(String publicKey) throws NoSuchAlgorithmException {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey));
            return (RSAPublicKey) keyFactory.generatePublic(keySpec);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException("无效的公钥格式", e);
        }
    }

    private static String publicEncrypt(String originalText, RSAPublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_CIPHER_TRANSFORMATION_OAEP);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        byte[] encryptedData = rsaSplitCodec(cipher, Cipher.ENCRYPT_MODE, originalText.getBytes(CHARSET), publicKey.getModulus().bitLength());
        return Base64.getEncoder().encodeToString(encryptedData);
    }
}
