package io.cordys.registry;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 导出线程注册表，用于管理和监控导出任务的线程。
 */
public final class ExportThreadRegistry {
    private static final ConcurrentHashMap<String, Thread> TASK_MAP = new ConcurrentHashMap<>();

    // 私有构造函数防止实例化
    private ExportThreadRegistry() {
        throw new UnsupportedOperationException("工具类不应被实例化");
    }

    /**
     * 注册任务线程
     *
     * @param taskId 任务ID
     * @param thread 任务线程
     */
    public static void register(String taskId, Thread thread) {
        if (taskId != null && thread != null) {
            TASK_MAP.put(taskId, thread);
        }
    }

    /**
     * 停止任务线程并中断它
     *
     * @param taskId 任务ID
     */
    public static void stop(String taskId) {
        Thread thread = TASK_MAP.remove(taskId);
        if (thread != null) {
            thread.interrupt();
        }
    }

    /**
     * 检查任务是否已被中断
     *
     * @param taskId 任务ID
     * @return 如果任务不存在或已被中断返回true，否则返回false
     */
    public static boolean isInterrupted(String taskId) {
        Thread thread = TASK_MAP.get(taskId);
        return thread == null || thread.isInterrupted();
    }

    /**
     * 从注册表中移除任务
     *
     * @param taskId 任务ID
     */
    public static void remove(String taskId) {
        TASK_MAP.remove(taskId);
    }

    /**
     * 检查任务是否存在
     *
     * @param taskId 任务ID
     * @return 如果任务存在返回true
     */
    public static boolean exists(String taskId) {
        return TASK_MAP.containsKey(taskId);
    }
}