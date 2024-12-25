package io.cordys.common.util.rsa;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * RSA 密钥对类，封装公钥和私钥。
 * <p>
 * 该类用于存储 RSA 加密算法中的公钥和私钥，供加密解密操作使用。
 * </p>
 */
@Setter
@Getter
public class RsaKey implements Serializable {

    /**
     * 公钥，使用 RSA 加密时的公钥部分。
     */
    private String publicKey;

    /**
     * 私钥，使用 RSA 解密时的私钥部分。
     */
    private String privateKey;
}
