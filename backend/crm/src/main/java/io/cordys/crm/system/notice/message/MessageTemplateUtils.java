package io.cordys.crm.system.notice.message;


import io.cordys.common.util.CommonBeanFactory;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.constants.NotificationConstants;
import io.cordys.crm.system.mapper.ExtUserMapper;
import io.cordys.security.UserDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.text.StringSubstitutor;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageTemplateUtils {

    private static void setFieldNameMap(Field[] moduleFields, Map<String, String> moduleMap) {
        for (Field moduleField : moduleFields) {
            Schema annotation = moduleField.getAnnotation(Schema.class);
            String name = "";
            if (annotation != null) {
                name = Translator.get(annotation.description());
            }
            moduleMap.put(moduleField.getName(), name);
        }
    }

    /**
     * 获取模块翻译后的名称map
     *
     * @return Map<String, String> moduleMap
     */
    public static Map<String, String> getModuleMap() {
        Field[] moduleFields = FieldUtils.getAllFields(NotificationConstants.Module.class);
        Map<String, String> moduleMap = new HashMap<>();
        setFieldNameMap(moduleFields, moduleMap);
        return moduleMap;
    }


    /**
     * 获取事件翻译后的名称map
     *
     * @return Map<String, String> eventMap
     */
    public static Map<String, String> getEventMap() {
        Map<String, String> eventMap = new HashMap<>();
        Field[] eventFields = FieldUtils.getAllFields(NotificationConstants.Event.class);
        setFieldNameMap(eventFields, eventMap);
        return eventMap;
    }

    /**
     * 获取事件的默认模版
     *
     * @return Map<String, String> defaultTemplateMap
     */
    public static Map<String, String> getDefaultTemplateMap() {
        Map<String, String> defaultTemplateMap = new HashMap<>();
        Field[] defaultTemplateFields = FieldUtils.getAllFields(NotificationConstants.TemplateText.class);
        setFieldNameMap(defaultTemplateFields, defaultTemplateMap);
        return defaultTemplateMap;
    }


    /**
     * 获取接收人的特殊值
     *
     * @return List<String> defaultRelatedUsers
     */
    public static List<String> getDefaultRelatedUser() {
        Field[] defaultRelatedUserFields = FieldUtils.getAllFields(NotificationConstants.RelatedUser.class);
        List<String> defaultRelatedUsers = new ArrayList<>();
        for (Field defaultRelatedUserField : defaultRelatedUserFields) {
            defaultRelatedUsers.add(defaultRelatedUserField.getName());
        }
        return defaultRelatedUsers;
    }

    /**
     * 获取接收人的特殊值
     *
     * @return List<String> defaultRelatedUsers
     */
    public static Map<String, String> getDefaultRelatedUserMap() {
        Map<String, String> defaultRelatedUserMap = new HashMap<>();
        Field[] defaultRelatedUserFields = FieldUtils.getAllFields(NotificationConstants.RelatedUser.class);
        MessageTemplateUtils.setFieldNameMap(defaultRelatedUserFields, defaultRelatedUserMap);
        return defaultRelatedUserMap;
    }

    public static String getContent(String template, Map<String, Object> context) {
        // 处理 null
        context.forEach((k, v) -> {
            if (v == null) {
                context.put(k, StringUtils.EMPTY);
            }
        });
        // 处理时间格式的数据
        handleTime(context);
        // 处理人相关的数据
        handleUser(context);
        StringSubstitutor sub = new StringSubstitutor(context);
        return sub.replace(template);
    }

    public static void handleTime(Map<String, Object> context) {
        context.forEach((k, v) -> {
            if (StringUtils.endsWithIgnoreCase(k, "Time")) {
                try {
                    String value = v.toString();
                    long time = Long.parseLong(value);
                    v = DateFormatUtils.format(time, "yyyy-MM-dd HH:mm:ss");
                    context.put(k, v);
                } catch (Exception ignore) {
                }
            }
        });
    }
    public static void handleUser(Map<String, Object> context) {
        ExtUserMapper userMapper = CommonBeanFactory.getBean(ExtUserMapper.class);
        context.forEach((k, v) -> {
            if (StringUtils.endsWithIgnoreCase(k, "User")) {
                try {
                    String value = v.toString();
                    assert userMapper != null;
                    UserDTO user = userMapper.selectById(value);
                    context.put(k, user.getName());
                } catch (Exception ignore) {
                }
            }
        });
    }

    public static String getTranslateTemplate(String template) {
        Map<String, Object> map = new HashMap<>();
        if (StringUtils.isNotBlank(template) && template.contains("${OPERATOR}")) {
            template = template.replace("${OPERATOR}", "<" + Translator.get("message.operator") + ">");
        }
        if (StringUtils.isNotBlank(template) && template.contains("${count}")) {
            template = template.replace("${count}", "<n>");
        }
        Map<String, String> defaultRelatedUserMap = getDefaultRelatedUserMap();
        defaultRelatedUserMap.remove("FOLLOW_PEOPLE");
        map.putAll(defaultRelatedUserMap);
        return getContent(template, map);
    }


}
