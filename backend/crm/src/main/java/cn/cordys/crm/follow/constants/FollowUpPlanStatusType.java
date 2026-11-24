package cn.cordys.crm.follow.constants;

import cn.cordys.common.util.Translator;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.List;
import java.util.Locale;

/**
 * 跟进计划状态
 * <p>
 * 状态不在跟进计划表单字段内, 是主表上的业务字段; 导入导出时需要单独做名称转换.
 */
@Getter
public enum FollowUpPlanStatusType {

    /**
     * 未开始
     */
    PREPARED("common.notStarted"),
    /**
     * 进行中
     */
    UNDERWAY("common.inProgress"),
    /**
     * 已完成
     */
    COMPLETED("common.completed"),
    /**
     * 已取消
     */
    CANCELLED("common.canceled");

    /**
     * 状态名称的国际化 Key
     */
    private final String i18nKey;

    FollowUpPlanStatusType(String i18nKey) {
        this.i18nKey = i18nKey;
    }

    /**
     * 支持的语言: 导入文件可能是在其他语言环境下下载的模板
     */
    private static final List<Locale> SUPPORTED_LOCALES = List.of(Locale.SIMPLIFIED_CHINESE, Locale.US);

    /**
     * 解析状态: 支持状态值(如 PREPARED)以及各语言下的状态名称(如 未开始)
     *
     * @param value 状态值或状态名称
     * @return 状态, 无法解析时返回 null
     */
    public static FollowUpPlanStatusType of(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        String text = value.trim();
        for (FollowUpPlanStatusType type : values()) {
            if (Strings.CI.equals(type.name(), text)) {
                return type;
            }
        }
        for (Locale locale : SUPPORTED_LOCALES) {
            for (FollowUpPlanStatusType type : values()) {
                if (Strings.CI.equals(Translator.get(type.getI18nKey(), locale), text)) {
                    return type;
                }
            }
        }
        return null;
    }

    /**
     * 状态值 => 当前语言下的状态名称
     *
     * @param value      状态值
     * @param locale     语言环境
     * @param defaultVal 无法解析时的返回值
     * @return 状态名称
     */
    public static String labelOf(String value, Locale locale, String defaultVal) {
        FollowUpPlanStatusType type;
        try {
            type = StringUtils.isBlank(value) ? null : FollowUpPlanStatusType.valueOf(value);
        } catch (IllegalArgumentException e) {
            type = null;
        }
        if (type == null) {
            return defaultVal;
        }
        return Translator.get(type.getI18nKey(), locale != null ? locale : Locale.getDefault(), defaultVal);
    }
}
