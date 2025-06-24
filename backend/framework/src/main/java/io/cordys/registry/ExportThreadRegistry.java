package io.cordys.registry;

import java.util.concurrent.ConcurrentHashMap;

public class ExportThreadRegistry {
    private static final ConcurrentHashMap<String, Thread> taskMap = new ConcurrentHashMap<>();

    public static void register(String taskId, Thread thread) {
        taskMap.put(taskId, thread);
    }

    public static void stop(String taskId) {
        if (taskMap.containsKey(taskId)) {
            taskMap.get(taskId).interrupt();
            taskMap.remove(taskId);
        }
    }

    public static boolean isStop(String taskId) {
        if (taskMap.containsKey(taskId)) {
            return taskMap.get(taskId).isInterrupted();
        }
        return true;
    }

    public static void remove(String taskId) {
        taskMap.remove(taskId);
    }
}