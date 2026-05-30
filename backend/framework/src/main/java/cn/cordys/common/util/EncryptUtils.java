package cn.cordys.common.util;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 加密解密工具，扩展自CodingUtils，提供AES和MD5的加解密方法。
 */
public class EncryptUtils extends CodingUtils {

    // 默认的加密密钥和向量
    private static final String secretKey = "www.fit2cloud.cn";

    /**
     * AES加密方法
     *
     * @param o 要加密的对象
     *
     * @return 加密后的字符串，若传入对象为空则返回null
     */
    public static String aesEncrypt(Object o) {
        if (o == null) {
            return null;
        }
        return aesEncrypt(o.toString(), secretKey, generateIv());
    }

    /**
     * AES解密方法
     *
     * @param o 要解密的对象
     *
     * @return 解密后的字符串，若传入对象为空则返回null
     */
    public static String aesDecrypt(Object o) {
        if (o == null) {
            return null;
        }
        return aesDecrypt(o.toString(), secretKey, generateIv());
    }

    /**
     * 对列表中的对象属性进行AES解密
     *
     * @param o        要解密的对象列表
     * @param attrName 需要解密的属性名
     * @param <T>      对象类型
     *
     * @return 解密后的对象列表
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
                        String decryptedValue = aesDecrypt(fieldValue.toString(), secretKey, generateIv());
                        BeanUtils.setFieldValueByName(element, attrName, decryptedValue, String.class);
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * MD5加密方法
     *
     * @param o 要加密的对象
     *
     * @return 加密后的MD5字符串，若传入对象为空则返回null
     */
    public static String md5Encrypt(Object o) {
        if (o == null) {
            return null;
        }
        return md5(o.toString());
    }
}
