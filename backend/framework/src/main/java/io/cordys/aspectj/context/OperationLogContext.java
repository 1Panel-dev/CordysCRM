package io.cordys.aspectj.context;

import java.util.*;

/**
 * 用于记录日志上下文变量的工具类，支持方法级别和全局变量的管理。
 * <p>
 * 本类使用了 {@link InheritableThreadLocal}，以确保子线程能够继承父线程的变量。
 * </p>
 */
public class OperationLogContext {

    /**
     * 存储方法级别变量的栈，每个方法调用对应一个栈帧。
     */
    private static final InheritableThreadLocal<Deque<Map<String, Object>>> VARIABLE_MAP_STACK = new InheritableThreadLocal<>();

    /**
     * 存储全局变量的映射。
     */
    private static final InheritableThreadLocal<Map<String, Object>> GLOBAL_VARIABLE_MAP = new InheritableThreadLocal<>();

    // 防止实例化工具类
    private OperationLogContext() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 向当前方法栈中放入变量。
     *
     * @param name  变量名
     * @param value 变量值
     */
    public static void putVariable(String name, Object value) {
        // 初始化栈
        Deque<Map<String, Object>> mapStack = Optional.ofNullable(VARIABLE_MAP_STACK.get())
                .orElseGet(() -> {
                    Deque<Map<String, Object>> stack = new ArrayDeque<>();
                    VARIABLE_MAP_STACK.set(stack);
                    return stack;
                });

        if (mapStack.isEmpty()) {
            mapStack.push(new HashMap<>());
        }
        mapStack.peek().put(name, value);
    }

    /**
     * 向全局变量映射中放入变量。
     *
     * @param name  变量名
     * @param value 变量值
     */
    public static void putGlobalVariable(String name, Object value) {
        Map<String, Object> globalMap = Optional.ofNullable(GLOBAL_VARIABLE_MAP.get())
                .orElseGet(() -> {
                    Map<String, Object> map = new HashMap<>();
                    GLOBAL_VARIABLE_MAP.set(map);
                    return map;
                });
        globalMap.put(name, value);
    }

    /**
     * 获取当前方法栈中指定的变量值。
     *
     * @param key 变量名
     * @return 变量值，如果找不到则返回 null
     */
    public static Object getVariable(String key) {
        Map<String, Object> variableMap = Optional.ofNullable(VARIABLE_MAP_STACK.get())
                .map(Deque::peek)
                .orElse(null);
        return variableMap == null ? null : variableMap.get(key);
    }

    /**
     * 获取当前方法栈中或全局变量映射中指定的变量值。
     * <p>
     * 若在方法栈中找不到变量，则会尝试从全局变量映射中获取。
     * </p>
     *
     * @param key 变量名
     * @return 变量值，如果找不到则返回 null
     */
    public static Object getMethodOrGlobal(String key) {
        // 优先从方法栈中获取
        Object result = Optional.ofNullable(VARIABLE_MAP_STACK.get())
                .map(stack -> {
                    assert stack.peek() != null;
                    return stack.peek().get(key);
                })
                .orElse(null);

        if (result == null) {
            // 若方法栈中没有，则从全局变量中获取
            result = Optional.ofNullable(GLOBAL_VARIABLE_MAP.get())
                    .map(map -> map.get(key))
                    .orElse(null);
        }

        return result;
    }

    /**
     * 获取当前方法栈中的所有变量。
     *
     * @return 当前方法栈中的变量映射，若栈为空，则返回一个空的 HashMap
     */
    public static Map<String, Object> getVariables() {
        return Optional.ofNullable(VARIABLE_MAP_STACK.get())
                .map(Deque::peek)
                .orElse(new HashMap<>());
    }

    /**
     * 获取全局变量映射。
     *
     * @return 全局变量映射，若未初始化，则返回一个空的 HashMap
     */
    public static Map<String, Object> getGlobalVariableMap() {
        return Optional.ofNullable(GLOBAL_VARIABLE_MAP.get())
                .orElse(new HashMap<>());
    }

    /**
     * 清除当前方法栈中的变量。
     */
    public static void clear() {
        Optional.ofNullable(VARIABLE_MAP_STACK.get()).ifPresent(Deque::pop);
    }

    /**
     * 清除全局变量映射中的所有变量。
     */
    public static void clearGlobal() {
        Optional.ofNullable(GLOBAL_VARIABLE_MAP.get()).ifPresent(Map::clear);
    }

    /**
     * 在进入方法时初始化一个空的 Span 放入栈中，方法执行完后会弹出。
     * <p>
     * 该方法通常用于日志追踪中的 Span 管理。
     * </p>
     */
    public static void putEmptySpan() {
        Deque<Map<String, Object>> mapStack = Optional.ofNullable(VARIABLE_MAP_STACK.get())
                .orElseGet(() -> {
                    Deque<Map<String, Object>> stack = new ArrayDeque<>();
                    VARIABLE_MAP_STACK.set(stack);
                    return stack;
                });
        mapStack.push(new HashMap<>());

        if (GLOBAL_VARIABLE_MAP.get() == null) {
            GLOBAL_VARIABLE_MAP.set(new HashMap<>());
        }
    }
}
