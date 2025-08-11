package io.cordys.common.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * LogUtils 提供了日志输出的工具方法，支持不同级别的日志记录。
 */
public class LogUtils {
    public static final String DEBUG = "DEBUG";
    public static final String INFO = "INFO";
    public static final String WARN = "WARN";
    public static final String ERROR = "ERROR";

    private static final Logger LOGGER = LoggerFactory.getLogger(LogUtils.class);

    /**
     * 根据日志级别输出日志信息。
     *
     * @param msg   要输出的日志信息
     * @param level 日志级别，支持 DEBUG、INFO、WARN、ERROR
     */
    public static void writeLog(Object msg, String level) {
        String message = getMsg(msg);

        switch (level) {
            case DEBUG:
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(message);
                }
                break;
            case INFO:
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(message);
                }
                break;
            case WARN:
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(message);
                }
                break;
            case ERROR:
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error(message);
                }
                break;
            default:
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error(StringUtils.EMPTY);
                }
        }
    }

    /**
     * 输出 INFO 级别的日志。
     *
     * @param msg 要输出的日志信息
     */
    public static void info(Object msg) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(getMsg(msg));
        }
    }

    /**
     * 输出带有格式化参数的 INFO 级别日志。
     *
     * @param message 格式化的日志信息
     * @param args    参数
     */
    public static void info(String message, Object... args) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(message, args);
        }
    }

    public static void debug(String message, Object... args) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(message, args);
        }
    }

    /**
     * 输出带有两个参数的 INFO 级别日志。
     *
     * @param msg  要输出的日志信息
     * @param arg1 第一个参数
     */
    public static void info(Object msg, Object arg1) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(getMsg(msg), arg1);
        }
    }

    /**
     * 输出带有多个参数的 INFO 级别日志。
     *
     * @param msg  要输出的日志信息
     * @param args 参数数组
     */
    public static void info(Object msg, Object[] args) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(getMsg(msg), args);
        }
    }

    /**
     * 输出 DEBUG 级别的日志。
     *
     * @param msg 要输出的日志信息
     */
    public static void debug(Object msg) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(getMsg(msg));
        }
    }

    /**
     * 输出带有参数的 DEBUG 级别日志。
     *
     * @param msg  要输出的日志信息
     * @param arg1 第一个参数
     */
    public static void debug(Object msg, Object arg1) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(getMsg(msg), arg1);
        }
    }

    /**
     * 输出带有多个参数的 DEBUG 级别日志。
     *
     * @param msg  要输出的日志信息
     * @param args 参数数组
     */
    public static void debug(Object msg, Object[] args) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(getMsg(msg), args);
        }
    }

    /**
     * 输出 WARN 级别的日志。
     *
     * @param msg 要输出的日志信息
     */
    public static void warn(Object msg) {
        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn(getMsg(msg));
        }
    }

    /**
     * 输出带有参数的 WARN 级别日志。
     *
     * @param msg  要输出的日志信息
     * @param arg1 第一个参数
     */
    public static void warn(Object msg, Object arg1) {
        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn(getMsg(msg), arg1);
        }
    }

    /**
     * 输出带有多个参数的 WARN 级别日志。
     *
     * @param msg  要输出的日志信息
     * @param args 参数数组
     */
    public static void warn(Object msg, Object[] args) {
        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn(getMsg(msg), args);
        }
    }

    /**
     * 输出 ERROR 级别的日志。
     *
     * @param msg 要输出的日志信息
     */
    public static void error(Object msg) {
        if (LOGGER.isErrorEnabled()) {
            LOGGER.error(getMsg(msg));
        }
    }

    /**
     * 输出 ERROR 级别的异常日志。
     *
     * @param e 异常信息
     */
    public static void error(Throwable e) {
        if (LOGGER.isErrorEnabled()) {
            LOGGER.error(getMsg(e), e);
        }
    }

    /**
     * 输出带有参数的 ERROR 级别日志。
     *
     * @param msg  要输出的日志信息
     * @param arg1 第一个参数
     */
    public static void error(Object msg, Object arg1) {
        if (LOGGER.isErrorEnabled()) {
            LOGGER.error(getMsg(msg), arg1);
        }
    }

    /**
     * 输出带有多个参数的 ERROR 级别日志。
     *
     * @param msg  要输出的日志信息
     * @param args 参数数组
     */
    public static void error(Object msg, Object[] args) {
        if (LOGGER.isErrorEnabled()) {
            LOGGER.error(getMsg(msg), args);
        }
    }

    /**
     * 输出带有异常信息的 ERROR 级别日志。
     *
     * @param msg 要输出的日志信息
     * @param ex  异常
     */
    public static void error(Object msg, Throwable ex) {
        if (LOGGER.isErrorEnabled()) {
            LOGGER.error(getMsg(msg), ex);
        }
    }

    /**
     * 获取日志信息。
     *
     * @param msg 要输出的日志信息
     * @param ex  异常信息
     * @return 格式化后的日志信息
     */
    private static String getMsg(Object msg, Throwable ex) {
        String message = (msg != null) ? msg.toString() : "null";
        String methodName = getLogMethod();
        String exceptionMessage = (ex != null) ? "[" + ex.getMessage() + "]" : "";
        return "Method[" + methodName + "]" + "[" + message + "]" + exceptionMessage;
    }

    /**
     * 获取日志信息。
     *
     * @param msg 要输出的日志信息
     * @return 格式化后的日志信息
     */
    private static String getMsg(Object msg) {
        return getMsg(msg, null);
    }

    /**
     * 获取调用类名。
     *
     * @return 调用类名
     */
    private static String getLogClass() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        return (stack.length > 3) ? stack[3].getClassName() : StringUtils.EMPTY;
    }

    /**
     * 获取调用方法名。
     *
     * @return 调用方法名
     */
    private static String getLogMethod() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        return (stack.length > 4) ? stack[4].getMethodName() : StringUtils.EMPTY;
    }

    /**
     * 将异常堆栈信息转换为字符串。
     *
     * @param e 异常
     * @return 异常堆栈的字符串表示
     */
    public static String toString(Throwable e) {
        try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
            e.printStackTrace(pw);
            return sw.toString();
        } catch (IOException ex) {
            return ex.getMessage();
        }
    }
}
