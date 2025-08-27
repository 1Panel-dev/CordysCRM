package cn.cordys.crm.system.utils;

public class ArchUtils {

    /**
     * 将各种 os.arch 的叫法统一为 amd64 或 arm64
     */
    public static String getNormalizedArch() {
        String arch = System.getProperty("os.arch");
        if (arch == null) {
            return "unknown";
        }

        arch = arch.toLowerCase();

        if (arch.contains("aarch64") || arch.contains("arm64")) {
            return "arm64";
        } else if (arch.contains("amd64") || arch.contains("x86_64") || arch.contains("x64")) {
            return "amd64";
        } else {
            return arch; // 其他架构原样返回
        }
    }
}
