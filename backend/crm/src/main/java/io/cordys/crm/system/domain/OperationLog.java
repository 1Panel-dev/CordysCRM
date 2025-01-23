package io.cordys.crm.system.domain;

import jakarta.persistence.Table;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


/**
 * 操作日志
 *
 * @author jianxing
 * @date 2025-01-23 14:21:05
 */
@Data
@Table(name = "sys_operation_log")
public class OperationLog  {

    @Schema(description = "主键")
    private String id;

    @Schema(description = "组织id")
    private String organizationId;

    @Schema(description = "操作类型/add/update/delete")
    private String type;

    @Schema(description = "操作模块")
    private String module;

    @Schema(description = "资源id")
    private String resourceId;

    @Schema(description = "资源名称")
    private String resourceName;

    @Schema(description = "登录地")
    private String loginAddress;

    @Schema(description = "平台")
    private String platform;

    @Schema(description = "操作时间")
    private Long createTime;

    @Schema(description = "操作人")
    private String createUser;

    @Schema(description = "操作路径")
    private String path;

    @Schema(description = "操作方法")
    private String method;
}
