package io.cordys.common.interceptor;

import java.lang.annotation.*;

/**
 * 标记需要处理 BaseCondition 查询条件
 * 由 ConditionFilterAspect 处理切面逻辑
 * {@link ConditionFilterAspect}
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConditionFilter {

}
