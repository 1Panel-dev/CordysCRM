package cn.cordys.crm.system.service.detailtab;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.condition.FilterCondition;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.permission.RelatedResourcePermissionService;
import cn.cordys.common.permission.ResourcePermissionService;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.crm.form.dto.request.CustomFormDataPageRequest;
import cn.cordys.crm.form.service.CustomFormDataService;
import cn.cordys.crm.system.domain.ModuleField;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 客户、商机详情下自定义关联表单的数据查询。
 * <p>
 * 关联条件由已保存的标签配置生成并追加到用户搜索条件中。即使客户端伪造或删除筛选条件，
 * 查询仍然只能返回指向当前详情资源的数据。
 * </p>
 */
@Component
@RequiredArgsConstructor
public class CustomRelatedDetailTabDataHandler implements DetailTabDataHandler {

    @Resource
    private ResourcePermissionService resourcePermissionService;
    @Resource
    private RelatedResourcePermissionService relatedResourcePermissionService;
    @Resource
    private CustomFormDataService customFormDataService;
    @Resource
    private BaseMapper<ModuleField> moduleFieldBaseMapper;

    /**
     * 处理自定义表单的查询
     */
    @Override
    public boolean supports(DetailTabQueryContext context) {
        if (StringUtils.isNotBlank(context.tab().getInternalKey())
                || context.tab().getRelatedForm() == null
                || context.tab().getRelatedField() == null) {
            return false;
        }
        String relatedFormId = context.tab().getRelatedForm().getIdAsString();
        return FormKey.ofKey(relatedFormId) == null;
    }

    @Override
    public PagerWithOption<?> page(DetailTabQueryContext context) {
        boolean relatedOwner = relatedResourcePermissionService.isOwner(context.formKey(), context.resourceId(),
                context.userId(), context.organizationId());
        DeptDataPermissionDTO customerScope = FormKey.CUSTOMER.getKey().equals(context.formKey())
                ? relatedResourcePermissionService.getCustomerTabDataPermission(context.resourceId(), context.userId(),
                        context.organizationId())
                : null;
        if (!relatedOwner) {
            resourcePermissionService.checkPermission(PermissionConstants.CUSTOM_FORM_READ);
        }

        String relatedFormId = context.tab().getRelatedForm().getIdAsString();
        String relatedFieldId = context.tab().getRelatedField().getIdAsString();
        ModuleField moduleField = moduleFieldBaseMapper.selectByPrimaryKey(relatedFieldId);
        if (moduleField == null) {
            throw new GenericException(Translator.get("module.tab.not_exist"));
        }
        CustomFormDataPageRequest request = BeanUtils.copyBean(new CustomFormDataPageRequest(), context.request());
        request.setCustomFormId(relatedFormId);
        // 详情页不接受客户端视图条件，避免视图配置改变固定关联查询的语义。
        request.setViewId(null);
        List<FilterCondition> filters = context.request().getFilters();
        filters.add(buildRelationFilter(relatedFieldId, context.resourceId()));
        if (customerScope != null) {
            if (customerScope.getSelf()) {
                FilterCondition condition = new FilterCondition();
                condition.setName("owner");
                condition.setOperator(FilterCondition.CombineConditionOperator.EQUALS.name());
                condition.setValue(context.userId());
                filters.add(condition);
            } else {
                Set<String> isolatedOwners = relatedResourcePermissionService.getIsolatedOwnerIds(context.resourceId(),
                        context.userId(), context.organizationId());
                if (!isolatedOwners.isEmpty()) {
                    FilterCondition condition = new FilterCondition();
                    condition.setName("owner");
                    condition.setOperator(FilterCondition.CombineConditionOperator.NOT_IN.name());
                    condition.setValue(List.copyOf(isolatedOwners));
                    filters.add(condition);
                }
            }
        }
        request.setFilters(filters);
        ConditionFilterUtils.parseCondition(request, relatedFormId);

        // 关联只读范围由宿主关系决定，功能权限仍在上方校验。
        if (relatedOwner || customerScope != null) {
            return customFormDataService.page(request, context.userId(), context.organizationId(), false, true);
        }
        return customFormDataService.page(request, context.userId(), context.organizationId(), false);
    }

    private FilterCondition buildRelationFilter(String relatedFieldId, String resourceId) {
        FilterCondition condition = new FilterCondition();
        condition.setName(relatedFieldId);
        condition.setOperator(FilterCondition.CombineConditionOperator.EQUALS.name());
        condition.setValue(resourceId);
        return condition;
    }
}
