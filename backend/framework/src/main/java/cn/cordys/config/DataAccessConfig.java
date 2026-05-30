package cn.cordys.config;

import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.DataAccessLayer;
import jakarta.annotation.Resource;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.ResolvableType;

import java.util.Objects;

/**
 * 数据访问配置类。
 * 提供 MyBatis 的 {@link BaseMapper} 的动态注入支持。
 */
@Configuration
public class DataAccessConfig {

    /**
     * 注入的 MyBatis {@link SqlSession}，用于操作数据库。
     */
    @Resource
    private SqlSession sqlSession;

    /**
     * 提供泛型 {@link BaseMapper} 的动态实例。
     * 使用 Spring 的原型作用域，每次注入时动态解析泛型类型并实例化。
     *
     * @param injectionPoint 当前注入点的信息，用于解析目标泛型类型
     * @param <E>            泛型参数，表示实体类类型
     *
     * @return 对应实体类的 {@link BaseMapper} 实例
     *
     * @throws IllegalArgumentException 如果注入点的字段类型无法解析
     */
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public <E> BaseMapper<E> simpleBaseMapper(InjectionPoint injectionPoint) {
        // 解析注入点字段的泛型类型
        ResolvableType resolved = ResolvableType.forField(
                Objects.requireNonNull(injectionPoint.getField(), "InjectionPoint 的字段信息不能为空")
        );

        // 获取泛型参数类型并验证
        @SuppressWarnings("unchecked")
        Class<E> parameterClass = (Class<E>) Objects.requireNonNull(
                resolved.getGeneric(0).resolve(),
                "无法解析泛型参数类型，请确认使用了明确的泛型声明"
        );

        // 返回对应的 BaseMapper 实例
        return DataAccessLayer.with(parameterClass, sqlSession);
    }
}
