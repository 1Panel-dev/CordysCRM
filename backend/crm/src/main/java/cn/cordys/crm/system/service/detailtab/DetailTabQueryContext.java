package cn.cordys.crm.system.service.detailtab;

import cn.cordys.crm.system.dto.form.FormDetailTab;
import cn.cordys.crm.system.dto.request.DetailTabPageRequest;

/**
 * 已完成标签配置校验后的查询上下文。
 */
public record DetailTabQueryContext(String formKey,
                                    String resourceId,
                                    FormDetailTab tab,
                                    DetailTabPageRequest request,
                                    String userId,
                                    String organizationId) {
}
