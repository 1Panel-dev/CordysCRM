package io.cordys.common.uid;

import io.cordys.common.util.CommonBeanFactory;
import io.cordys.common.uid.impl.DefaultUidGenerator;

/**
 * IDGenerator 用于生成唯一的 ID。
 * 提供了生成数字 ID 和字符串 ID 的功能。
 */
public class IDGenerator {

    // 默认的 UID 生成器实例
    private static final DefaultUidGenerator DEFAULT_UID_GENERATOR;

    static {
        // 从 CommonBeanFactory 获取 DefaultUidGenerator 实例
        DEFAULT_UID_GENERATOR = CommonBeanFactory.getBean(DefaultUidGenerator.class);
    }

    /**
     * 生成一个唯一的数字 ID。
     *
     * @return 唯一的数字 ID
     */
    public static Long nextNum() {
        return DEFAULT_UID_GENERATOR.getUID();
    }

    /**
     * 生成一个唯一的字符串 ID。
     *
     * @return 唯一的字符串 ID
     */
    public static String nextStr() {
        return String.valueOf(DEFAULT_UID_GENERATOR.getUID());
    }
}
