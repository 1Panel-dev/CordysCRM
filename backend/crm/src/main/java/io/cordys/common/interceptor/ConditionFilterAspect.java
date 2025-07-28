package io.cordys.common.interceptor;


import io.cordys.common.constants.InternalUserView;
import io.cordys.common.dto.condition.BaseCondition;
import io.cordys.common.dto.condition.CombineSearch;
import io.cordys.common.dto.condition.FilterCondition;
import io.cordys.security.SessionUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @Author: jianxing
 * @CreateTime: 2024-08-28  17:31
 * 拦截高级搜索等查询
 * 处理高级搜索等通用查询条件
 * 1. 处理视图查询条件
 * 2. 预先过滤不合法的查询条件
 * 3. 拆分系统字段和自定义字段
 * 4. 处理成员选项中的 CURRENT_USER
 */
@Aspect
@Component
public class ConditionFilterAspect {
    @Pointcut("@annotation(io.cordys.common.interceptor.ConditionFilter)")
    public void pointcut() {
    }

    @Before("pointcut()")
    public void before(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof BaseCondition baseCondition) {
                parseBaseCondition(baseCondition);
            } else {
                try {
                    // 批量操作
                    Method getCondition = arg.getClass().getMethod("getCondition");
                    BaseCondition baseCondition = (BaseCondition) getCondition.invoke(arg);
                    parseBaseCondition(baseCondition);
                } catch (Exception e) {
                    // do nothing
                }
            }
        }
    }

    private void parseBaseCondition(BaseCondition baseCondition) {
        // 处理视图查询条件
        String viewId = baseCondition.getViewId();

        if (StringUtils.isNotBlank(viewId) && !InternalUserView.isInternalUserView(viewId)) {
            // 查询视图
            // todo
        }

        CombineSearch combineSearch = baseCondition.getCombineSearch();
        if (combineSearch == null) {
            return;
        }

        List<FilterCondition> validConditions = getValidConditions(combineSearch.getConditions());
        validConditions.forEach(item -> {
            if (item.getValue() != null && item.getValue() instanceof String strValue
                    && StringUtils.equalsAny(item.getOperator(), FilterCondition.CombineConditionOperator.CONTAINS.name(),
                    FilterCondition.CombineConditionOperator.NOT_CONTAINS.name())) {
                // 转义 mysql 的特殊字符
                item.setValue(BaseCondition.transferKeyword(strValue));
            }
        });
        replaceCurrentUser(validConditions);
    }

    /**
     * 处理成员选项中的 CURRENT_USER
     * 替换当前用户的用户ID
     * @param validConditions
     */
    private void replaceCurrentUser(List<FilterCondition> validConditions) {
        for (FilterCondition validCondition : validConditions) {
            Object value = validCondition.getValue();
            if (value instanceof List arrayValues) {
                for (int i = 0; i < arrayValues.size(); i++) {
                    Object arrayValue = arrayValues.get(i);
                    if (arrayValue != null && StringUtils.equals(arrayValue.toString(), InternalUserView.CURRENT_USER)) {
                        // 替换当前用户的用户ID
                        arrayValues.set(i, SessionUtils.getUserId());
                    }
                }
            }
        }
    }

    public List<FilterCondition> getValidConditions(List<FilterCondition> conditions) {
        if (CollectionUtils.isEmpty(conditions)) {
            return List.of();
        }
        return conditions.stream().filter(FilterCondition::valid).toList();
    }
}
