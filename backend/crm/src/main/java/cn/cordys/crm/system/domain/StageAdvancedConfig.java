package cn.cordys.crm.system.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "stage_advanced_config")
public class StageAdvancedConfig extends BaseModel {

    @Schema(description = "源id")
    private String originId;

    @Schema(description = "目标id")
    private String targetId;

    @Schema(description = "是否允许流转")
    private Boolean enable;

    @Schema(description = "字段配置")
    private String fieldConfig;

    @Schema(description = "模块类型(order-订单/contract-合同)")
    private String moduleType;

    @Schema(description = "组织id")
    private String organizationId;
}