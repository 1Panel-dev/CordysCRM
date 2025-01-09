package io.cordys.common.util;

public class ServiceUtils {
    /**
     * 保存资源名称，在处理 NOT_FOUND 异常时，拼接资源名称
     */
    private static final ThreadLocal<String> resourceName = new ThreadLocal<>();

    public static String getResourceName() {
        return resourceName.get();
    }

    public static void clearResourceName() {
        resourceName.remove();
    }

    //用于排序的num
    public static final int NUM_STEP = 4096;

}
