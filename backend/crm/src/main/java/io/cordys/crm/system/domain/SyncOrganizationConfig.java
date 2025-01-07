package io.cordys.crm.system.domain;

import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 同步组织架构配置表;
 */
@Data
@Table(name = "sys_sync_organization_config")
public class SyncOrganizationConfig extends BaseModel {
    @Schema(description = "组织ID")
    private String organizationId;

    @Schema(description = "是否启用")
    private boolean enable;

    @Schema(description = "应用ID")
    private String agentId;

    @Schema(description = "企业ID")
    private String corpId;

    @Schema(description = "企业密钥")
    private String secret;

    @Schema(description = "平台")
    private String resource;

    @Schema(description = "回调url")
    private String url;

    @Schema(description = "是否同步")
    private boolean sync;

    @Schema(description = "同步平台")
    private String syncResource;
}
