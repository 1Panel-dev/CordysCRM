package cn.cordys.aspectj.dto;

import cn.cordys.common.groups.Created;
import cn.cordys.common.groups.Updated;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 日志数据传输对象（DTO），继承自操作日志（OperationLog）。
 * 用于封装操作日志的具体数据，包含变更前后内容、是否需要历史记录等信息。
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class LogDTO {
    @Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{operation_log.id.not_blank}", groups = {Updated.class})
    @Size(min = 1, max = 50, message = "{operation_log.id.length_range}", groups = {Created.class, Updated.class})
    private String id;

    @Schema(description = "组织id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{operation_log.organization_id.not_blank}", groups = {Created.class})
    @Size(min = 1, max = 50, message = "{operation_log.organization_id.length_range}", groups = {Created.class, Updated.class})
    private String organizationId;

    @Schema(description = "操作时间")
    private Long createTime;

    @Schema(description = "操作人")
    private String createUser;

    @Schema(description = "资源id")
    private String resourceId;

    @Schema(description = "资源名称")
    private String resourceName;

    /**
     * 无需对比的操作日志详情
     * 例如可以移入公海
     * 详情记录为：客户 xxx 移入公海 xxx
     */
    @Schema(description = "日志详情")
    private String detail;

    @Schema(description = "操作方法", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{operation_log.method.not_blank}", groups = {Created.class})
    @Size(min = 1, max = 255, message = "{operation_log.method.length_range}", groups = {Created.class, Updated.class})
    private String method;

    @Schema(description = "操作类型/add/update/delete", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{operation_log.type.not_blank}", groups = {Created.class})
    @Size(min = 1, max = 20, message = "{operation_log.type.length_range}", groups = {Created.class, Updated.class})
    private String type;

    @Schema(description = "操作模块")
    private String module;

    @Schema(description = "操作路径")
    private String path;

    @Schema(description = "登录地")
    private String loginAddress;

    @Schema(description = "平台")
    private String platform;

    /**
     * 原始值
     */
    private Object originalValue;

    /**
     * 修改后的值
     */
    private Object modifiedValue;

    /**
     * 默认构造函数
     */
    public LogDTO() {
    }

    /**
     * 带参构造函数，用于快速初始化日志数据
     *
     * @param organizationId 组织ID
     * @param resourceId     数据源ID
     * @param createUser     创建用户
     * @param type           日志类型
     * @param module         模块
     */
    public LogDTO(String organizationId, String resourceId, String createUser, String type, String module, String resourceName) {
        this.setOrganizationId(organizationId);
        this.setResourceId(resourceId);
        this.setCreateUser(createUser);
        this.setType(type);
        this.setModule(module);
        this.setResourceName(resourceName);
        this.setCreateTime(System.currentTimeMillis());
    }
}
