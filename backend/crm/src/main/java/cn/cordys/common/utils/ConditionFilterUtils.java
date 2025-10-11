package cn.cordys.common.utils;


import cn.cordys.common.constants.InternalUserView;
import cn.cordys.common.dto.BaseTreeNode;
import cn.cordys.common.dto.condition.BaseCondition;
import cn.cordys.common.dto.condition.CombineSearch;
import cn.cordys.common.dto.condition.FilterCondition;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.JSON;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.mapper.ExtAttachmentMapper;
import cn.cordys.crm.system.service.DepartmentService;
import cn.cordys.crm.system.service.UserViewService;
import cn.cordys.security.SessionUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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
            String searchMode = userViewService.getSearchMode(viewId);
            List<FilterCondition> conditions = userViewService.getFilterConditions(viewId);
            if (baseCondition.getCombineSearch() == null) {
                baseCondition.setCombineSearch(new CombineSearch());
            }
            if (baseCondition.getViewCombineSearch() == null) {
                baseCondition.setViewCombineSearch(new CombineSearch());
            }
            List<FilterCondition> newConditions = baseCondition.getCombineSearch()
                    .getConditions();

            //新增子节点数据
            List<BaseTreeNode> tree = CommonBeanFactory.getBean(DepartmentService.class).getTree(OrganizationContext.getOrganizationId());
            buildConditions(conditions, tree);

            baseCondition.getViewCombineSearch().setConditions(newConditions);
            baseCondition.getViewCombineSearch().setSearchMode(baseCondition.getCombineSearch()
                    .getSearchMode());
            baseCondition.getCombineSearch().setConditions(conditions);
            baseCondition.getCombineSearch().setSearchMode(searchMode);
        }

        CombineSearch combineSearch = baseCondition.getCombineSearch();
        if (combineSearch == null) {
            return;
        }

        List<FilterCondition> validConditions = getValidConditions(combineSearch.getConditions());
        validConditions.forEach(item -> {
            Object combineValue = item.getCombineValue();
            String combineOperator = item.getCombineOperator();
            item.setValue(combineValue);
            item.setOperator(combineOperator);
            if (item.getValue() != null && item.getCombineValue() instanceof String strValue
                    && Strings.CS.equalsAny(item.getCombineOperator(), FilterCondition.CombineConditionOperator.CONTAINS.name(),
                    FilterCondition.CombineConditionOperator.NOT_CONTAINS.name())) {
                // 转义 mysql 的特殊字符
                item.setValue(BaseCondition.transferKeyword(strValue));
            }

            if (item.getValue() != null && Strings.CS.contains(item.getType(), "MULTIPLE")
                    && Strings.CS.equalsAny(item.getCombineOperator(), FilterCondition.CombineConditionOperator.EQUALS.name(),
                    FilterCondition.CombineConditionOperator.NOT_EQUALS.name())) {
                // 转义 mysql 的特殊字符
                item.setValue(JSON.toJSONString(item.getCombineValue()));
            }

            if (item.getValue() != null && Strings.CS.equals(item.getType(), FieldType.ATTACHMENT.name())) {
                // 附件类型转义名称
                List<String> attachmentNames = List.of(item.getCombineValue().toString().split(StringUtils.SPACE));
                ExtAttachmentMapper attachmentMapper = CommonBeanFactory.getBean(ExtAttachmentMapper.class);
                List<String> attachmentIds = attachmentMapper.getAttachmentIdsByNames(attachmentNames);
                item.setValue(CollectionUtils.isEmpty(attachmentIds) ? attachmentNames : attachmentIds);
            }
        });
        replaceCurrentUser(validConditions);
    }


    /**
     * 包含新增子节点数据
     *
     * @param conditions
     */
    private static void buildConditions(List<FilterCondition> conditions, List<BaseTreeNode> tree) {
        if (CollectionUtils.isNotEmpty(conditions)) {
            conditions.forEach(condition -> {
                if (CollectionUtils.isNotEmpty(condition.getContainChildIds())) {
                    condition.getContainChildIds().forEach(id -> {
                        List<String> ids = getIds(tree, id);
                        ids.addAll(condition.getContainChildIds());
                        condition.setValue(ids.stream().distinct().toList());
                    });
                }
            });
        }
    }


    public static List<String> getIds(List<BaseTreeNode> tree, String targetId) {
        List<String> ids = new ArrayList<>();
        for (BaseTreeNode node : tree) {
            BaseTreeNode targetNode = findNode(node, targetId);
            if (targetNode != null) {
                collectIds(targetNode, ids);
                break;
            }
        }
        return ids;
    }

    private static BaseTreeNode findNode(BaseTreeNode node, String targetId) {
        if (node.getId().equals(targetId)) {
            return node;
        }
        for (BaseTreeNode child : node.getChildren()) {
            BaseTreeNode result = findNode(child, targetId);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private static void collectIds(BaseTreeNode node, List<String> ids) {
        ids.add(node.getId());
        for (BaseTreeNode child : node.getChildren()) {
            collectIds(child, ids);
        }
    }

    /**
     * 处理成员选项中的 CURRENT_USER
     * 替换当前用户的用户ID
     *
     * @param validConditions
     */
    private static void replaceCurrentUser(List<FilterCondition> validConditions) {
        for (FilterCondition validCondition : validConditions) {
            Object value = validCondition.getCombineValue();
            if (value instanceof List arrayValues) {
                for (int i = 0; i < arrayValues.size(); i++) {
                    Object arrayValue = arrayValues.get(i);
                    if (arrayValue != null && Strings.CS.equals(arrayValue.toString(), InternalUserView.CURRENT_USER)) {
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
