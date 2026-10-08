package cn.cordys.common.uid;

import java.math.BigInteger;
import java.util.UUID;

/**
 * IDGenerator 用于生成唯一的 ID。
 * 数字和字符串 ID 均使用带安全随机尾部的 UUIDv7，无需 Spring 容器。
 */
public class IDGenerator {

    /**
     * 生成完整 UUIDv7 对应的正整数，与字符串 ID 使用相同算法。
     * 保留全部 128 位，不可转换为 Long；十进制存储需最多 39 位（如 DECIMAL(39, 0)）。
     *
     * @return 保留时间和全部随机位的数字 ID
     */
    public static BigInteger nextNum() {
        return new BigInteger(UUID(), 16);
    }

    /**
     * 生成 32 位小写十六进制 UUIDv7，兼容现有 VARCHAR(32) 字段。
     * 按毫秒时间大致有序，同一毫秒内使用独立随机值，不保证严格递增。
     * ID 仍会暴露生成时间，不能替代对象级权限校验。
     *
     * @return 不含连字符的 UUIDv7
     */
    public static String nextStr() {
        return nextNum().toString();
    }

    private static String UUID() {
        // randomUUID 使用 SecureRandom；保留 74 位随机数和 RFC variant，仅替换时间与版本位。
        UUID random = UUID.randomUUID();
        long mostSignificantBits = (System.currentTimeMillis() << 16) | 0x7000L
                | (random.getMostSignificantBits() & 0x0FFFL);
        return new UUID(mostSignificantBits, random.getLeastSignificantBits()).toString().replace("-", "");
    }
}
