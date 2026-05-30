package cn.cordys.file.engine;

/**
 * 存储类型枚举类，用于标识不同的文件存储方式。
 * <p>
 * 此枚举类定义了三种存储方式：MINIO、GIT 和 LOCAL。
 * </p>
 */
public enum StorageType {

    /**
     * 标准S3协议 存储，通常用于对象存储。
     */
    S3,

    /**
     * LOCAL 存储，通常用于本地文件存储。
     */
    LOCAL;

    /**
     * 根据字符串获取对应的枚举值。
     * <p>
     * 如果提供的字符串不匹配任何枚举值，返回 {@code null}。
     * </p>
     *
     * @param storageType 字符串表示的存储类型
     *
     * @return 对应的枚举值，或 {@code null} 如果没有匹配的枚举
     */
    public static StorageType fromString(String storageType) {
        if (storageType != null) {
            for (StorageType type : StorageType.values()) {
                if (type.name().equalsIgnoreCase(storageType)) {
                    return type;
                }
            }
        }
        return LOCAL;
    }

}
