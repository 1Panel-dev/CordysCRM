package io.cordys.file.engine;

/**
 * 默认的资源目录
 */
public class DefaultRepositoryDir {
    /**
     * 系统级别资源的根目录
     */
    private static final String SYSTEM_ROOT_DIR = "system";

    private static final String TMP_DIR = "tmp";

    private static final String DEFAULT_DIR = "/opt/cordys/data/files/";

    public static String getSystemRootDir() {
        return SYSTEM_ROOT_DIR;
    }

    public static String getTmpDir() {
        return TMP_DIR;
    }

    public static String getDefaultDir() {
        return DEFAULT_DIR;
    }
}
