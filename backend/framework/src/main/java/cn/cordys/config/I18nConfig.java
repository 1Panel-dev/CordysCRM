package cn.cordys.config;

import cn.cordys.common.util.Translator;
import jakarta.validation.Validator;
import org.hibernate.validator.HibernateValidator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * 配置类，用于设置国际化和校验相关的Bean。
 * <p>
 * 该配置类包括：
 * 1. 国际化翻译器 (Translator) 的 Bean 配置。
 * 2. 使用 JSR-303 规范的校验器 (Validator)，并设置国际化消息源。
 * </p>
 */
@Configuration
public class I18nConfig {

    /**
     * 创建 Translator Bean，提供国际化的翻译功能。
     * <p>
     * 该 Bean 仅在没有其他 Translator Bean 的情况下创建。
     * </p>
     *
     * @return Translator 对象
     */
    @Bean
    @ConditionalOnMissingBean
    public Translator translator() {
        return new Translator();
    }

    /**
     * 配置 JSR-303 校验的国际化消息源。
     * <p>
     * 使用 Hibernate Validator 作为校验提供者，并将指定的 MessageSource 作为消息源。
     * </p>
     *
     * @param messageSource 消息源，用于提供国际化的错误信息
     *
     * @return 配置好的 LocalValidatorFactoryBean
     */
    @Bean
    public LocalValidatorFactoryBean localValidatorFactoryBean(MessageSource messageSource) {
        LocalValidatorFactoryBean localValidatorFactoryBean = new LocalValidatorFactoryBean();
        localValidatorFactoryBean.setProviderClass(HibernateValidator.class);
        localValidatorFactoryBean.setValidationMessageSource(messageSource);
        return localValidatorFactoryBean;
    }

    /**
     * 创建 Validator Bean，用于执行 JSR-303 校验。
     * <p>
     * 该 Bean 使用 LocalValidatorFactoryBean 作为校验工厂，提供一个验证器实例。
     * </p>
     *
     * @param localValidatorFactoryBean 校验工厂
     *
     * @return 校验器实例
     */
    @Bean
    public Validator validator(LocalValidatorFactoryBean localValidatorFactoryBean) {
        return localValidatorFactoryBean.getValidator();
    }
}
