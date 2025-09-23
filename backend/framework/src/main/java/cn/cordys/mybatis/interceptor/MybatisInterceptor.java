package cn.cordys.mybatis.interceptor;

import cn.cordys.common.util.BeanUtils;
import cn.cordys.config.MybatisInterceptorConfig;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MyBatis 拦截器，用于对数据库操作的参数进行加密或解密处理。
 * <p>
 * 本拦截器支持对更新和查询操作的参数进行加密处理，
 * 并在返回结果时根据配置进行解密操作。
 * </p>
 */
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
})
public class MybatisInterceptor implements Interceptor {

    @Setter
    @Getter
    private List<MybatisInterceptorConfig> interceptorConfigList;

    private final ConcurrentHashMap<String, Class> classMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<String, Map<String, MybatisInterceptorConfig>>> interceptorConfigMap = new ConcurrentHashMap<>();

    /**
     * 拦截目标方法并进行加密或解密处理。
     *
     * @param invocation 当前调用的信息
     *
     * @return 处理后的结果对象
     *
     * @throws Throwable 如果处理过程中出现异常
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        String methodName = invocation.getMethod().getName();
        Object parameter = invocation.getArgs()[1];

        // 对更新操作的参数进行处理
        if (parameter != null && methodName.equals("update")) {
            invocation.getArgs()[1] = process(parameter);
        }

        // 执行目标方法
        Object returnValue = invocation.proceed();
        Object result = returnValue;

        // 对查询结果进行解密处理
        if (returnValue instanceof ArrayList<?>) {
            List<Object> list = new ArrayList<>();
            boolean isDecrypted = false;
            for (Object val : (ArrayList<?>) returnValue) {
                Object a = undo(val);
                if (a != val) {
                    isDecrypted = true;
                    list.add(a);
                } else {
                    break;
                }
            }
            if (isDecrypted) {
                result = list;
            }
        } else {
            result = undo(returnValue);
        }
        return result;
    }

    /**
     * 获取与指定对象相关的配置。
     *
     * @param p 目标对象
     *
     * @return 对象的配置信息
     */
    private Map<String, Map<String, MybatisInterceptorConfig>> getConfig(Object p) {
        Map<String, Map<String, MybatisInterceptorConfig>> result = new HashMap<>();
        if (p == null) {
            return null;
        }

        String pClassName = p.getClass().getName();
        if (interceptorConfigMap.get(pClassName) != null) {
            return interceptorConfigMap.get(pClassName);
        }

        Map<String, List<MybatisInterceptorConfig>> m = new HashMap<>();
        for (MybatisInterceptorConfig interceptorConfig : interceptorConfigList) {
            String className = interceptorConfig.getModelName();
            String attrName = interceptorConfig.getAttrName();
            if (StringUtils.isNotBlank(className)) {
                Class<?> c = classMap.get(className);
                if (c == null) {
                    try {
                        c = Class.forName(className);
                        classMap.put(className, c);
                    } catch (ClassNotFoundException e) {
                        continue;
                    }
                }
                if (c.isInstance(p)) {
                    result.computeIfAbsent(attrName, k -> new HashMap<>());
                    if (StringUtils.isNotBlank(interceptorConfig.getInterceptorMethod())) {
                        result.get(attrName).put(Methods.encrypt.name(), interceptorConfig);
                    }
                    if (StringUtils.isNotBlank(interceptorConfig.getInterceptorMethod())) {
                        result.get(attrName).put(Methods.decrypt.name(), interceptorConfig);
                    }
                }
            }
        }
        interceptorConfigMap.put(pClassName, result);
        return result;
    }

    /**
     * 处理加密操作。
     *
     * @param obj 目标对象
     *
     * @return 加密后的对象
     *
     * @throws Throwable 如果加密过程中出现异常
     */
    private Object process(Object obj) throws Throwable {
        if (obj instanceof Map paramMap) {
            for (Object key : paramMap.keySet()) {
                if (paramMap.get(key) != null) {
                    paramMap.put(key, process(paramMap.get(key)));
                }
            }
            return paramMap;
        }

        Map<String, Map<String, MybatisInterceptorConfig>> localInterceptorConfigMap = getConfig(obj);
        if (MapUtils.isEmpty(localInterceptorConfigMap)) {
            return obj;
        }
        Object newObject = obj.getClass().getDeclaredConstructor().newInstance();
        BeanUtils.copyBean(newObject, obj);
        for (String attrName : localInterceptorConfigMap.keySet()) {
            if (MapUtils.isEmpty(localInterceptorConfigMap.get(attrName))) {
                continue;
            }
            MybatisInterceptorConfig interceptorConfig = localInterceptorConfigMap.get(attrName).get(Methods.encrypt.name());
            if (interceptorConfig == null || StringUtils.isBlank(interceptorConfig.getInterceptorClass())
                    || StringUtils.isBlank(interceptorConfig.getInterceptorMethod())) {
                continue;
            }
            Object fieldValue = BeanUtils.getFieldValueByName(interceptorConfig.getAttrName(), newObject);
            if (fieldValue != null) {
                Class<?> processClazz = Class.forName(interceptorConfig.getInterceptorClass());
                Method method = processClazz.getMethod(interceptorConfig.getInterceptorMethod(), Object.class);
                Object processedValue = method.invoke(null, fieldValue);
                if (processedValue instanceof byte[]) {
                    BeanUtils.setFieldValueByName(newObject, interceptorConfig.getAttrName(), processedValue, byte[].class);
                } else {
                    BeanUtils.setFieldValueByName(newObject, interceptorConfig.getAttrName(), processedValue, fieldValue.getClass());
                }
            }
        }

        return newObject;
    }

    /**
     * 处理解密操作。
     *
     * @param obj 目标对象
     *
     * @return 解密后的对象
     *
     * @throws Throwable 如果解密过程中出现异常
     */
    private Object undo(Object obj) throws Throwable {
        Map<String, Map<String, MybatisInterceptorConfig>> localDecryptConfigMap = getConfig(obj);
        if (MapUtils.isEmpty(localDecryptConfigMap)) {
            return obj;
        }
        Object result = obj.getClass().getDeclaredConstructor().newInstance();
        BeanUtils.copyBean(result, obj);

        for (String attrName : localDecryptConfigMap.keySet()) {
            if (MapUtils.isEmpty(localDecryptConfigMap.get(attrName))) {
                continue;
            }
            MybatisInterceptorConfig interceptorConfig = localDecryptConfigMap.get(attrName).get(Methods.decrypt.name());
            if (interceptorConfig == null || StringUtils.isBlank(interceptorConfig.getUndoClass())
                    || StringUtils.isBlank(interceptorConfig.getUndoMethod())) {
                continue;
            }
            Object fieldValue = BeanUtils.getFieldValueByName(interceptorConfig.getAttrName(), result);
            if (fieldValue != null) {
                Class<?> processClazz = Class.forName(interceptorConfig.getUndoClass());
                Object undoValue;
                if (fieldValue instanceof List) {
                    Method method = processClazz.getMethod(interceptorConfig.getUndoMethod(), List.class, String.class);
                    method.invoke(null, fieldValue, interceptorConfig.getAttrNameForList());
                } else {
                    Method method = processClazz.getMethod(interceptorConfig.getUndoMethod(), Object.class);
                    undoValue = method.invoke(null, fieldValue);
                    if (undoValue instanceof byte[]) {
                        BeanUtils.setFieldValueByName(result, interceptorConfig.getAttrName(), undoValue, byte[].class);
                    } else {
                        BeanUtils.setFieldValueByName(result, interceptorConfig.getAttrName(), undoValue, fieldValue.getClass());
                    }
                }
            }
        }
        return result;
    }

    /**
     * 创建插件对象。
     *
     * @param target 目标对象
     *
     * @return 包装后的对象
     */
    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    /**
     * 设置属性。
     *
     * @param properties 配置属性
     */
    @Override
    public void setProperties(Properties properties) {
        // TODO: 如果有配置需求，可以在这里处理
    }

    private enum Methods {
        encrypt, decrypt
    }
}
