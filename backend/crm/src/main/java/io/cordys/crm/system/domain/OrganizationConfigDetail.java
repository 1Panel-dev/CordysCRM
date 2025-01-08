package io.cordys.crm.system.domain;

import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 企业设置详情表;
 */
@Data
@Table(name = "sys_organization_config_detail")
public class OrganizationConfigDetail extends BaseModel {
    @Schema(description = "配置id")
    private String configId;

    @Schema(description = "配置内容")
    private byte[] content;

    @Schema(description = "配置内容类型")
    private String type;
}

