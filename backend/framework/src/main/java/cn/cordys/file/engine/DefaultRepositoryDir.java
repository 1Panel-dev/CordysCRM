package cn.cordys.file.engine;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 默认的资源目录工具类
 */
public final class DefaultRepositoryDir {

    /**
     * 临时目录
     */
    private static final String TMP_DIR = "/tmp";

    /**
     * 文件默认目录
     */
    private static final String DEFAULT_DIR = "/opt/cordys/data/files";

    /**
     * 导出目录
     */
    private static final String EXPORT_DIR = "/export";

    /**
     * 图片正式目录
     */
    private static final String PIC_DIR = "/pic";

    /**
     * 私有构造方法，防止实例化
     */
    private DefaultRepositoryDir() {
        // 工具类不应被实例化
    }

    /**
     * 获取临时目录路径
     *
     * @return 临时目录路径
     */
    public static String getTmpDir() {
        return TMP_DIR;
    }

    /**
     * 获取图片目录路径
     *
     * @return 图片目录路径
     */
    public static String getPicDir() {
        return PIC_DIR;
    }

    /**
     * 获取指定组织的导出目录路径
     *
     * @param orgId 组织ID
     * @return 导出目录路径
     */
    public static String getExportDir(String orgId) {
        return Path.of(orgId, EXPORT_DIR).toString();
    }

    /**
     * 获取默认目录路径
     *
     * @return 默认目录路径
     */
    public static String getDefaultDir() {
        return DEFAULT_DIR;
    }

    /**
     * 获取完整的临时目录路径
     *
     * @return 完整的临时目录路径
     */
    public static Path getFullTmpPath() {
        return Paths.get(DEFAULT_DIR, TMP_DIR);
    }

    /**
     * 获取完整的图片目录路径
     *
     * @return 完整的图片目录路径
     */
    public static Path getFullPicPath() {
        return Paths.get(DEFAULT_DIR, PIC_DIR);
    }

    /**
     * 获取指定组织的完整导出目录路径
     *
     * @param orgId 组织ID
     * @return 完整的导出目录路径
     */
    public static Path getFullExportPath(String orgId) {
        return Paths.get(DEFAULT_DIR, orgId, EXPORT_DIR);
    }
}