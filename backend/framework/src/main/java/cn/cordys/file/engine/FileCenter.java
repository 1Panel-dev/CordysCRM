package cn.cordys.file.engine;

import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.LogUtils;
import org.apache.commons.lang3.Strings;

import static cn.cordys.file.engine.StorageType.LOCAL;
import static cn.cordys.file.engine.StorageType.S3;

/**
 * FileCenter 类提供了根据存储类型获取对应文件仓库的静态方法。
 * <p>
 * 该类封装了不同存储类型（如 MINIO、LOCAL）的文件仓库获取逻辑，并允许根据存储类型返回相应的 {@link FileRepository} 实现。
 * </p>
 */
public class FileCenter {
    // 默认存储类型
    private static StorageType defStorageType = null;

    // 私有构造函数，防止实例化
    private FileCenter() {
    }

    /**
     * 根据给定的存储类型返回对应的 {@link FileRepository} 实现。
     *
     * @param storage 存储类型枚举值，指示所需的存储实现（如 S3、LOCAL）。
     *
     * @return 返回对应的 {@link FileRepository} 实现，如果存储类型未知，则返回默认的仓库。
     */
    public static FileRepository getRepository(String storage) {
        if (Strings.CS.equals(S3.name(), storage)) {
            return CommonBeanFactory.getBean(S3Repository.class);
        } else {
            return CommonBeanFactory.getBean(LocalRepository.class);
        }
    }

    /**
     * 返回默认的 {@link FileRepository} 实现。
     * <p>
     * 当前默认实现为 {@link LocalRepository}，可以根据实际需求修改此方法以支持其他默认仓库。
     * </p>
     *
     * @return 默认的 {@link FileRepository} 实现。
     */
    public static FileRepository getDefaultRepository() {
        if (defStorageType == null) {
            defStorageType = LOCAL;
        }
        LogUtils.info("Default storage type is set to: " + defStorageType);
        return getRepository(defStorageType.name());
    }
}
