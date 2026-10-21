package cn.cordys.file.engine;

/**
 * 文件存储类型：S3 对象存储和本地存储。
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
     * 忽略大小写匹配；输入为空或无匹配项时返回 {@link #LOCAL}。
     * </p>
     *
     * @param storageType 字符串表示的存储类型
     * @return 匹配的存储类型，无匹配项时返回 {@link #LOCAL}
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
