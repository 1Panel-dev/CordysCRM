package cn.cordys.common.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.CollectionType;
import tools.jackson.databind.type.TypeFactory;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * JSON 工具类，封装了常用的 JSON 序列化和反序列化方法。
 * 支持对象与 JSON 字符串、字节数组、集合、映射等类型的相互转换。
 */
public class JSON {

    // 默认最大字符串长度
    public static final int DEFAULT_MAX_STRING_LEN = Integer.MAX_VALUE;
    // ObjectMapper 实例，用于 JSON 操作
    private static final ObjectMapper objectMapper = JsonMapper.builder(JsonFactory.builder()
            .streamReadConstraints(StreamReadConstraints.builder().maxStringLength(DEFAULT_MAX_STRING_LEN).build())
            .build())
            .configureForJackson2()
            .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS, JsonReadFeature.ALLOW_JAVA_COMMENTS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .changeDefaultVisibility(visibility -> visibility.withFieldVisibility(JsonAutoDetect.Visibility.ANY))
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .build();
    private static final TypeFactory typeFactory = objectMapper.getTypeFactory();

    /**
     * 将对象序列化为 JSON 字符串。
     *
     * @param value 需要序列化的对象
     * @return JSON 字符串
     */
    public static String toJSONString(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException e) {
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
        } catch (JacksonException e) {
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
        } catch (JacksonException e) {
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
     * @param <T>       Java 类的类型
     * @param content   JSON 字符串
     * @param valueType 目标 Java 类
     * @return 反序列化后的 Java 对象
     */
    public static <T> T parseObject(String content, Class<T> valueType) {
        try {
            return objectMapper.readValue(content, valueType);
        } catch (JacksonException e) {
            throw new RuntimeException("JSON 反序列化失败", e);
        }
    }

    /**
     * 将 JSON 字符串反序列化为指定类型的 Java 对象。
     *
     * @param <T>       Java 类的类型
     * @param content   JSON 字符串
     * @param valueType 目标 Java 类型引用
     * @return 反序列化后的 Java 对象
     */
    public static <T> T parseObject(String content, TypeReference<T> valueType) {
        try {
            return objectMapper.readValue(content, valueType);
        } catch (JacksonException e) {
            throw new RuntimeException("JSON 反序列化失败", e);
        }
    }

    /**
     * 将输入流中的 JSON 数据反序列化为 Java 对象。
     *
     * @param <T>       Java 类的类型
     * @param src       输入流
     * @param valueType 目标 Java 类
     * @return 反序列化后的 Java 对象
     */
    public static <T> T parseObject(InputStream src, Class<T> valueType) {
        try {
            return objectMapper.readValue(src, valueType);
        } catch (JacksonException e) {
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
     * @param <T>       集合元素类型
     * @param content   JSON 字符串
     * @param valueType 集合元素类型
     * @return 反序列化后的集合对象
     */
    public static <T> List<T> parseArray(String content, Class<T> valueType) {
        CollectionType javaType = typeFactory.constructCollectionType(List.class, valueType);
        try {
            return objectMapper.readValue(content, javaType);
        } catch (JacksonException e) {
            throw new RuntimeException("JSON 反序列化失败", e);
        }
    }

    /**
     * 将 JSON 字符串反序列化为指定类型的 Java 对象的集合。
     *
     * @param <T>       集合元素类型
     * @param content   JSON 字符串
     * @param valueType 集合元素类型引用
     * @return 反序列化后的集合对象
     */
    public static <T> List<T> parseArray(String content, TypeReference<T> valueType) {
        try {
            JavaType subType = typeFactory.constructType(valueType);
            CollectionType javaType = typeFactory.constructCollectionType(List.class, subType);
            return objectMapper.readValue(content, javaType);
        } catch (JacksonException e) {
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
            return objectMapper.readValue(jsonObject, new TypeReference<>() {
            });
        } catch (JacksonException e) {
            throw new RuntimeException("JSON 反序列化失败", e);
        }
    }

    public static Map<String, Object> parseToMap(String jsonObject) {
        try {
            return objectMapper.readValue(jsonObject, new TypeReference<>() {
            });
        } catch (JacksonException e) {
            throw new RuntimeException("JSON 反序列化失败", e);
        }
    }

    public static <T> T parseObject(InputStream src, TypeReference<T> valueType) {
        try {
            return objectMapper.readValue(src, valueType);
        } catch (JacksonException e) {
            throw new RuntimeException("JSON 反序列化失败", e);
        }
    }

    public static final ObjectMapper MAPPER = JsonMapper.builderWithJackson2Defaults().build();

}
