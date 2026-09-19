package cn.cordys.crm.system.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 详情页标签数据分页请求。
 *
 * <p>标签统一通过 {@code relatedFormId} 定位；同一关联表单可配置多个自定义标签时，
 * 再通过 {@code relatedFieldId} 区分。标签名称允许修改，因此不能作为查询标识。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DetailTabPageRequest extends BasePageRequest {

    @Schema(description = "关联表单 ID；独立内置标签传其稳定标识")
    private String relatedFormId;

    @Schema(description = "关联字段 ID；查询内置或间接关联标签时可为空")
    private String relatedFieldId;
}
