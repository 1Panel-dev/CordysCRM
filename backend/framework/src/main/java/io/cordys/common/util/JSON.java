package io.cordys.common.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * JSON 工具类，封装了常用的 JSON 序列化和反序列化方法。
 * 支持对象与 JSON 字符串、字节数组、集合、映射等类型的相互转换。
 */
public class JSON {

    // ObjectMapper 实例，用于 JSON 操作
    private static final ObjectMapper objectMapper = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)  // 允许 JSON 中未转义的控制字符
            .build();

    private static final TypeFactory typeFactory = objectMapper.getTypeFactory();

    // 默认最大字符串长度
    public static final int DEFAULT_MAX_STRING_LEN = Integer.MAX_VALUE;

    // 静态初始化块，配置 ObjectMapper
    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);  // 忽略未知属性
        objectMapper.configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true);  // 使用 BigDecimal 处理浮点数
        objectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);  // 允许 JSON 中的注释
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);  // 自动检测所有类的字段属性
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);  // 允许序列化空对象
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);  // 接受单个值作为数组处理
        objectMapper.getFactory()
                .setStreamReadConstraints(StreamReadConstraints.builder().maxStringLength(DEFAULT_MAX_STRING_LEN).build());  // 设置读取字符流时的长度限制
        objectMapper.registerModule(new JavaTimeModule());  // 注册 Java 8 时间模块
    }

    /**
     * 将对象序列化为 JSON 字符串。
     *
     * @param value 需要序列化的对象
     * @return JSON 字符串
     */
    public static String toJSONString(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException e) {
            throw new RuntimeException("JSON 序列化失败", e);
        }
    }

    /**
     * 将对象序列化为格式化的 JSON 字符串（带缩进）。
     *
     * @param value 需要序列化的对象
     * @return 格式化的 JSON 字符串
     */
    public static String toFormatJSONString(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (IOException e) {
            throw new RuntimeException("JSON 序列化失败", e);
        }
    }

    /**
     * 将对象序列化为字节数组。
     *
     * @param value 需要序列化的对象
     * @return JSON 字节数组
     */
    public static byte[] toJSONBytes(Object value) {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (IOException e) {
            throw new RuntimeException("JSON 序列化失败", e);
        }
    }

    /**
     * 将 JSON 字符串反序列化为 Java 对象。
     *
     * @param content JSON 字符串
     * @return 反序列化后的 Java 对象
     */
    public static Object parseObject(String content) {
        return parseObject(content, Object.class);
    }

    /**
     * 将 JSON 字符串反序列化为指定类型的 Java 对象。
     *
     * @param content JSON 字符串
     * @param valueType 目标 Java 类
     * @param <T> Java 类的类型
     * @return 反序列化后的 Java 对象
     */
    public static <T> T parseObject(String content, Class<T> valueType) {
        try {
            return objectMapper.readValue(content, valueType);
        } catch (IOException e) {
            throw new RuntimeException("JSON 反序列化失败", e);
        }
    }

    /**
     * 将 JSON 字符串反序列化为指定类型的 Java 对象。
     *
     * @param content JSON 字符串
     * @param valueType 目标 Java 类型引用
     * @param <T> Java 类的类型
     * @return 反序列化后的 Java 对象
     */
    public static <T> T parseObject(String content, TypeReference<T> valueType) {
        try {
            return objectMapper.readValue(content, valueType);
        } catch (IOException e) {
            throw new RuntimeException("JSON 反序列化失败", e);
        }
    }

    /**
     * 将输入流中的 JSON 数据反序列化为 Java 对象。
     *
     * @param src 输入流
     * @param valueType 目标 Java 类
     * @param <T> Java 类的类型
     * @return 反序列化后的 Java 对象
     */
    public static <T> T parseObject(InputStream src, Class<T> valueType) {
        try {
            return objectMapper.readValue(src, valueType);
        } catch (IOException e) {
            throw new RuntimeException("JSON 反序列化失败", e);
        }
    }

    /**
     * 将 JSON 字符串反序列化为 Java 对象的集合。
     *
     * @param content JSON 字符串
     * @return 反序列化后的集合对象
     */
    public static List parseArray(String content) {
        return parseArray(content, Object.class);
    }

    /**
     * 将 JSON 字符串反序列化为指定类型的 Java 对象的集合。
     *
     * @param content JSON 字符串
     * @param valueType 集合元素类型
     * @param <T> 集合元素类型
     * @return 反序列化后的集合对象
     */
    public static <T> List<T> parseArray(String content, Class<T> valueType) {
        CollectionType javaType = typeFactory.constructCollectionType(List.class, valueType);
        try {
            return objectMapper.readValue(content, javaType);
        } catch (IOException e) {
            throw new RuntimeException("JSON 反序列化失败", e);
        }
    }

    /**
     * 将 JSON 字符串反序列化为指定类型的 Java 对象的集合。
     *
     * @param content JSON 字符串
     * @param valueType 集合元素类型引用
     * @param <T> 集合元素类型
     * @return 反序列化后的集合对象
     */
    public static <T> List<T> parseArray(String content, TypeReference<T> valueType) {
        try {
            JavaType subType = typeFactory.constructType(valueType);
            CollectionType javaType = typeFactory.constructCollectionType(List.class, subType);
            return objectMapper.readValue(content, javaType);
        } catch (IOException e) {
            throw new RuntimeException("JSON 反序列化失败", e);
        }
    }

    /**
     * 将 JSON 字符串反序列化为 Map 对象。
     *
     * @param jsonObject JSON 字符串
     * @return 反序列化后的 Map 对象
     */
    public static Map parseMap(String jsonObject) {
        try {
            return objectMapper.readValue(jsonObject, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 反序列化失败", e);
        }
    }


    public static <T> T parseObject(InputStream src, TypeReference<T> valueType) {
        try {
            return objectMapper.readValue(src, valueType);
        } catch (IOException e) {
            throw new RuntimeException("JSON 反序列化失败", e);
        }
    }

}
