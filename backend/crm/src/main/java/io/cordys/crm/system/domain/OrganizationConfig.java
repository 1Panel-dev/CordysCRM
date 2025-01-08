package io.cordys.crm.system.domain;

import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 企业设置
 */
@Data
@Table(name = "sys_organization_config")
public class OrganizationConfig extends BaseModel {

    @Schema(description = "配置类型")
    private String type;

    @Schema(description = "企业id")
    private String organizationId;

    @Schema(description = "是否同步（只对同步企业生效）")
    private int sync;

    @Schema(description = "同步来源")
    private String syncResource;
}
