package cn.cordys.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 日志工具类
 **/
public final class LogUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogUtils.class);

    private LogUtils() {
        // 工具类禁止实例化
    }

    /**
     * 通用日志方法
     *
     * @param level
     * @param message
     * @param args
     */
    public static void log(LogLevel level, String message, Object... args) {
        switch (level) {
            case DEBUG -> LOGGER.debug(message, args);
            case INFO -> LOGGER.info(message, args);
            case WARN -> LOGGER.warn(message, args);
            case ERROR -> LOGGER.error(message, args);
        }
    }

    /**
     * info
     *
     * @param message
     * @param args
     */
    public static void info(String message, Object... args) {
        LOGGER.info(message, args);
    }

    /**
     * DEBUG
     *
     * @param message
     * @param args
     */
    public static void debug(String message, Object... args) {
        LOGGER.debug(message, args);
    }

    /**
     * DEBUG 级别 + 方法名（仅在 DEBUG 开启时才计算堆栈）
     */
    public static void debugWithMethod(String message, Object... args) {
        if (!LOGGER.isDebugEnabled()) {
            return;
        }
        LOGGER.debug("[{}] {}", getCallerMethod(), String.format(message, args));
    }

    /**
     * WARN
     *
     * @param message
     * @param args
     */
    public static void warn(String message, Object... args) {
        LOGGER.warn(message, args);
    }

    /**
     * ERROR
     *
     * @param message
     * @param args
     */
    public static void error(String message, Object... args) {
        LOGGER.error(message, args);
    }

    public static void error(String message, Throwable ex) {
        LOGGER.error(message, ex);
    }

    public static void error(Throwable ex) {
        LOGGER.error(ex.getMessage(), ex);
    }

    /**
     * 获取调用方方法名（用于 DEBUG）
     */
    private static String getCallerMethod() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        return stack.length > 4 ? stack[4].getMethodName() : "unknown";
    }

    /**
     * 将异常堆栈转为字符串（仅在确实需要字符串时使用）
     */
    public static String stackTraceToString(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    /**
     * 日志级别枚举
     */
    public enum LogLevel {
        DEBUG, INFO, WARN, ERROR
    }
}
