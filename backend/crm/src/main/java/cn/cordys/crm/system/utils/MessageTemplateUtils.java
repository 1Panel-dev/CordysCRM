package cn.cordys.crm.system.utils;

import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.mapper.ExtUserMapper;
import cn.cordys.security.UserDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.text.StringSubstitutor;

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
    Field[] defaultTemplateFields =
        FieldUtils.getAllFields(NotificationConstants.TemplateText.class);
    setFieldNameMap(defaultTemplateFields, defaultTemplateMap);
    return defaultTemplateMap;
  }

  public static String getTemplate(String event) {
    Map<String, String> defaultTemplateMap = getDefaultTemplateMap();
    return defaultTemplateMap.get(event + "_TEXT");
  }

  public static String getContent(String template, Map<String, Object> context) {
    // 处理 null
    context.forEach(
        (k, v) -> {
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
    context.forEach(
        (k, v) -> {
          if (Strings.CI.endsWith(k, "Time")) {
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
    context.forEach(
        (k, v) -> {
          if (Strings.CI.endsWith(k, "User")) {
            try {
              String value = v.toString();
              assert userMapper != null;
              UserDTO user = userMapper.selectByPhoneOrEmail(value);
              context.put(k, user.getName());
            } catch (Exception ignore) {
            }
          }
        });
  }
}
