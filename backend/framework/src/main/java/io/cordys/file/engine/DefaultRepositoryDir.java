package io.cordys.file.engine;

/**
 * 默认的资源目录
 */
public class DefaultRepositoryDir {

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

    public static String getTmpDir() {
        return TMP_DIR;
    }

    public static String getPicDir() {
        return PIC_DIR;
    }

    public static String getExportDir() {
        return EXPORT_DIR;
    }

    public static String getDefaultDir() {
        return DEFAULT_DIR;
    }
}
