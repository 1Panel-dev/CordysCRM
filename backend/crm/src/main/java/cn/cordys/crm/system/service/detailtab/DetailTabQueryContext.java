package cn.cordys.crm.system.service.detailtab;

import cn.cordys.common.dto.condition.BaseCondition;
import cn.cordys.crm.system.dto.form.FormDetailTab;

/**
 * 已完成标签配置校验后的查询上下文。
 * <p>
 * 分页请求与统计请求都继承自 {@link BaseCondition}，handler 统一按基类读取查询条件。
 * </p>
 */
public record DetailTabQueryContext(String formKey,
        String resourceId,
        FormDetailTab tab,
        BaseCondition request,
        String userId,
        String organizationId) {
}
