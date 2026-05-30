package cn.cordys.crm.system.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;

@Data
public class Schedule extends BaseModel {
    @Serial
    private static final long serialVersionUID = 1L;
    @Schema(description = "id")
    private String id;
    @Schema(description = "Qrtz UUID")
    private String key;
    @Schema(description = "组织ID")
    private String organizationId;
    @Schema(description = "执行类型 cron", requiredMode = Schema.RequiredMode.REQUIRED)
    private String type;
    @Schema(description = "cron 表达式", requiredMode = Schema.RequiredMode.REQUIRED)
    private String value;
    @Schema(description = "Schedule Job Class Name", requiredMode = Schema.RequiredMode.REQUIRED)
    private String job;
    @Schema(description = "资源类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private String resourceType;
    @Schema(description = "是否开启")
    private Boolean enable;
    @Schema(description = "资源ID")
    private String resourceId;
    @Schema(description = "创建人")
    private String createUser;
    @Schema(description = "创建时间")
    private Long createTime;
    @Schema(description = "更新时间")
    private Long updateTime;
    @Schema(description = "名称")
    private String name;
    @Schema(description = "配置")
    private String config;
    @Schema(description = "业务ID")
    private Long num;
}