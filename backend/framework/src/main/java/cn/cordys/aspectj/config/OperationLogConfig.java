package cn.cordys.aspectj.config;

import cn.cordys.aspectj.aop.OperationLogAopAdvisor;
import cn.cordys.aspectj.aop.OperationLogSource;
import cn.cordys.aspectj.aop.OperationOperationLogInterceptor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.*;
import org.springframework.core.type.AnnotationMetadata;

/**
 * 日志记录配置类，默认启用日志记录功能。
 * 不依赖 @EnableLogRecord 注解，直接配置日志记录相关的切面、拦截器等。
 */
@Configuration
public class OperationLogConfig implements ImportAware {

    /**
     * 创建 LogRecordOperationSource 实例，提供日志记录操作的元数据。
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public OperationLogSource logRecordOperationSource() {
        return new OperationLogSource();
    }

    /**
     * 创建 LogRecordAopAdvisor 实例，配置日志记录的切面和拦截器。
     * @param operationLogInterceptor LogRecordInterceptor 实例
     * @return LogRecordAopAdvisor 实例
     */
    @DependsOn("logRecordInterceptor")
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public OperationLogAopAdvisor logRecordAdvisor(OperationOperationLogInterceptor operationLogInterceptor) {
        OperationLogAopAdvisor advisor = new OperationLogAopAdvisor();
        advisor.setOperationLogSource(logRecordOperationSource());
        advisor.setAdvice(operationLogInterceptor);
        return advisor;
    }

    /**
     * 创建 LogRecordInterceptor 实例，配置日志记录的拦截器。
     * @return LogRecordInterceptor 实例
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public OperationOperationLogInterceptor logRecordInterceptor() {
        OperationOperationLogInterceptor interceptor = new OperationOperationLogInterceptor();
        interceptor.setOperationLogSource(logRecordOperationSource());
        return interceptor;
    }

    /**
     * 设置导入的元数据，在此处读取日志记录相关的配置。
     * @param importMetadata 导入的元数据
     */
    @Override
    public void setImportMetadata(@NotNull AnnotationMetadata importMetadata) {
        // 如果需要在导入时读取其他的配置信息，可以在这里处理
        // LogUtils.info("Log record configuration is enabled by default");
    }
}
