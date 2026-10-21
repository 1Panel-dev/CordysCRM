package cn.cordys.common.util;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于 {@link CodingUtils} 提供 AES 加解密和 MD5 摘要计算。
 */
public class EncryptUtils extends CodingUtils {

    // 默认的加密密钥
    private static final String secretKey = "www.fit2cloud.cn";

    /**
     * AES加密方法
     *
     * @param o 要加密的对象
     * @return 加密后的字符串，若传入对象为空则返回null
     */
    public static String aesEncrypt(Object o) {
        if (o == null) {
            return null;
        }
        return aesEncrypt(o.toString(), secretKey, generateLegacyIv());
    }

    /**
     * AES解密方法
     *
     * @param o 要解密的对象
     * @return 解密后的字符串，若传入对象为空则返回null
     */
    public static String aesDecrypt(Object o) {
        if (o == null) {
            return null;
        }
        return aesDecrypt(o.toString(), secretKey, generateLegacyIv());
    }

    /**
     * 对列表中的非空指定属性原地进行 AES 解密，并返回包含这些对象的新列表。
     *
     * @param <T>      对象类型
     * @param o        要解密的对象列表
     * @param attrName 需要解密的属性名
     * @return 指定属性非空的对象列表；列表或属性名为 {@code null} 时返回 {@code null}
     */
    public static <T> Object aesDecrypt(List<T> o, String attrName) {
        if (o == null || attrName == null) {
            return null;
        }
        // 对列表中的每个对象属性进行解密
        return o.stream()
                .filter(element -> BeanUtils.getFieldValueByName(attrName, element) != null)
                .peek(element -> {
                    Object fieldValue = BeanUtils.getFieldValueByName(attrName, element);
                    if (fieldValue != null) {
                        String decryptedValue = aesDecrypt(fieldValue.toString(), secretKey, generateLegacyIv());
                        BeanUtils.setFieldValueByName(element, attrName, decryptedValue, String.class);
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * 计算对象字符串表示的 MD5 摘要。
     *
     * @param o 待计算摘要的对象
     * @return MD5 摘要字符串，传入 {@code null} 时返回 {@code null}
     */
    public static String md5Encrypt(Object o) {
        if (o == null) {
            return null;
        }
        return md5(o.toString());
    }
}
