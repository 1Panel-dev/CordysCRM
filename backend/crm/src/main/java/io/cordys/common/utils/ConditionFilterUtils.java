package io.cordys.common.utils;


import io.cordys.common.constants.InternalUserView;
import io.cordys.common.dto.condition.BaseCondition;
import io.cordys.common.dto.condition.CombineSearch;
import io.cordys.common.dto.condition.FilterCondition;
import io.cordys.common.util.CommonBeanFactory;
import io.cordys.crm.system.service.UserViewService;
import io.cordys.security.SessionUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @Author: jianxing
 * @CreateTime: 2025-07-28  17:31
 * 拦截高级搜索等查询
 * 处理高级搜索等通用查询条件
 * 1. 处理视图查询条件
 * 2. 预先过滤不合法的查询条件
 * 3. 处理成员选项中的 CURRENT_USER
 */
@Aspect
@Component
public class ConditionFilterUtils {

    public static void parseCondition(BaseCondition baseCondition) {
        // 处理视图查询条件
        String viewId = baseCondition.getViewId();

        if (StringUtils.isNotBlank(viewId) && !InternalUserView.isInternalUserView(viewId)) {
            // 查询视图
            UserViewService userViewService = CommonBeanFactory.getBean(UserViewService.class);
            List<FilterCondition> conditions = userViewService.getFilterConditions(viewId);
            if (baseCondition.getCombineSearch() == null) {
                baseCondition.setCombineSearch(new CombineSearch());
            }
            List<FilterCondition> newConditions = baseCondition.getCombineSearch()
                    .getConditions();
            newConditions.addAll(conditions);
            baseCondition.getCombineSearch().setConditions(newConditions);
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
    private static void replaceCurrentUser(List<FilterCondition> validConditions) {
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

    public static List<FilterCondition> getValidConditions(List<FilterCondition> conditions) {
        if (CollectionUtils.isEmpty(conditions)) {
            return List.of();
        }
        return conditions.stream().filter(FilterCondition::valid).toList();
    }
}
