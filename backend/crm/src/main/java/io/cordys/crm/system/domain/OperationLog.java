package io.cordys.crm.system.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

import java.io.Serial;

@Data
@Schema(description = "操作日志")
@Table(name = "sys_operation_log")
public class OperationLog {

    @Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Schema(description = "组织id", requiredMode = Schema.RequiredMode.REQUIRED)
    private String organizationId;

    @Schema(description = "操作类型/add/update/delete", requiredMode = Schema.RequiredMode.REQUIRED)
    private String type;

    @Schema(description = "操作模块", requiredMode = Schema.RequiredMode.REQUIRED)
    private String module;

    @Schema(description = "操作详情")
    private String details;

    @Schema(description = "资源id", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sourceId;

    @Schema(description = "操作路径")
    private String path;

    @Schema(description = "操作方法", requiredMode = Schema.RequiredMode.REQUIRED)
    private String method;

    @Schema(description = "登录地")
    private String loginAddress;

    @Schema(description = "平台", requiredMode = Schema.RequiredMode.REQUIRED)
    private String platform;

    @Schema(description = "创建人")
    private String createUser;

    @Schema(description = "创建时间")
    private Long createTime;

    @Serial
    private static final long serialVersionUID = 1L;
}