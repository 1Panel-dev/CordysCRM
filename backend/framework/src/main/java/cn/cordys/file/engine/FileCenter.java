package cn.cordys.file.engine;

import cn.cordys.common.util.CommonBeanFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Strings;

import static cn.cordys.file.engine.StorageType.LOCAL;
import static cn.cordys.file.engine.StorageType.S3;

/**
 * 按存储类型获取 S3 或本地文件仓库。
 */
@Slf4j
public class FileCenter {
    // 默认存储类型
    private static StorageType defStorageType = null;

    // 私有构造函数，防止实例化
    private FileCenter() {
    }

    /**
     * 根据给定的存储类型返回对应的 {@link FileRepository} 实现。
     *
     * @param storage 存储类型名称，仅 {@code S3} 匹配 S3 仓库（区分大小写）。
     * @return S3 仓库，或在其他输入（包括 {@code null}）下返回本地仓库。
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
     * 默认存储类型为 {@link StorageType#LOCAL}。
     * </p>
     *
     * @return 默认的 {@link FileRepository} 实现。
     */
    public static FileRepository getDefaultRepository() {
        if (defStorageType == null) {
            defStorageType = LOCAL;
        }
        log.info("Default storage type is set to: {}", defStorageType);
        return getRepository(defStorageType.name());
    }
}
