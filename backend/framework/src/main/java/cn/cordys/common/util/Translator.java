package cn.cordys.common.util;

import jakarta.annotation.Resource;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

/**
 * 翻译工具类，用于从消息源中获取本地化的消息。
 * <p>
 * 该类提供了根据消息键（key）从消息源中获取翻译的功能。
 * </p>
 */
public class Translator {

    private static MessageSource messageSource;

    /**
     * 根据给定的消息键获取翻译内容。
     *
     * @param key 消息键
     *
     * @return 翻译后的消息，如果没有找到对应的消息，则返回 "Not Support Key: " + key
     */
    public static String get(String key) {
        return messageSource.getMessage(key, null, "Not Support Key: " + key, LocaleContextHolder.getLocale());
    }

    /**
     * 根据给定的消息键获取翻译内容，如果没有找到对应的消息，则返回指定的默认消息。
     *
     * @param key            消息键
     * @param defaultMessage 默认消息
     *
     * @return 翻译后的消息，若未找到则返回默认消息
     */
    public static String get(String key, String defaultMessage) {
        return messageSource.getMessage(key, null, defaultMessage, LocaleContextHolder.getLocale());
    }

    /**
     * 根据给定的消息键和指定的语言环境获取翻译内容。
     *
     * @param key    消息键
     * @param locale 指定的语言环境
     *
     * @return 翻译后的消息
     */
    public static String get(String key, Locale locale) {
        return messageSource.getMessage(key, null, locale);
    }

    /**
     * 根据给定的消息键和参数获取翻译内容。
     * 支持格式化参数的插入。
     *
     * @param key  消息键
     * @param args 格式化参数
     *
     * @return 翻译后的消息
     */
    public static String getWithArgs(String key, Object... args) {
        return messageSource.getMessage(key, args, "Not Support Key: " + key, LocaleContextHolder.getLocale());
    }

    /**
     * 注入 MessageSource 用于国际化消息处理。
     *
     * @param messageSource Spring 的 MessageSource 实例
     */
    @Resource
    public void setMessageSource(MessageSource messageSource) {
        Translator.messageSource = messageSource;
    }
}
