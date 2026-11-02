package cn.cordys.crm.system.dto.request;

import cn.cordys.common.dto.condition.BaseCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 详情页标签金额统计请求。
 * <p>
 * 统计不分页，因此不继承 {@link DetailTabPageRequest}（其分页参数校验会拒绝
 * 未传 current/pageSize 的统计调用）。查询条件与标签分页接口保持一致。
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DetailTabStatisticRequest extends BaseCondition implements DetailTabQueryRequest {

    @Schema(description = "关联表单 ID；独立内置标签传其稳定标识")
    private String relatedFormId;

    @Schema(description = "关联字段 ID；查询内置或间接关联标签时可为空")
    private String relatedFieldId;
}
