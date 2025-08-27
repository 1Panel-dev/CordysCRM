package cn.cordys.common.interceptor;

import cn.cordys.crm.system.domain.User;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 用户密码字段脱敏的拦截器。
 * 该拦截器会在查询结果中去除 User 对象的密码字段，以避免敏感信息泄露。
 */
@Intercepts({
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
})
public class UserDesensitizationInterceptor implements Interceptor {

    /**
     * 拦截 query 方法，脱敏 User 对象中的密码字段。
     *
     * @param invocation 方法调用对象
     * @return 脱敏后的结果
     * @throws Throwable 如果执行方法时发生错误
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 执行原始方法
        Object returnValue = invocation.proceed();

        // 如果返回值是 List 类型，处理其中的每个元素
        if (returnValue instanceof List<?>) {
            List<Object> list = new ArrayList<>();
            boolean isDecrypted = false;

            for (Object val : (List<?>) returnValue) {
                if (val instanceof User) {
                    isDecrypted = true;
                    // 将密码字段置为 null，进行脱敏处理
                    ((User) val).setPassword(null);
                }
                list.add(val);
            }

            // 如果有任何脱敏操作，则返回修改后的列表，否则返回原始结果
            return isDecrypted ? list : returnValue;
        }

        // 如果返回值是单个 User 对象，脱敏其密码字段
        if (returnValue instanceof User) {
            ((User) returnValue).setPassword(null);
        }

        return returnValue;
    }

    /**
     * 将目标对象包装成拦截器对象。
     *
     * @param target 需要包装的目标对象
     * @return 包装后的目标对象
     */
    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    /**
     * 设置拦截器的属性。
     *
     * @param properties 拦截器的属性
     */
    @Override
    public void setProperties(Properties properties) {
        // 本拦截器不需要设置属性
    }
}
