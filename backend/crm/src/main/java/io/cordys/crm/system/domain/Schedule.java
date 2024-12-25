package io.cordys.crm.system.domain;

import io.cordys.common.groups.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

@Data
public class Schedule implements Serializable {
    @Schema(description = "", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{schedule.id.not_blank}", groups = {Updated.class})
    @Size(min = 1, max = 50, message = "{schedule.id.length_range}", groups = {Created.class, Updated.class})
    private String id;

    @Schema(description = "qrtz UUID")
    private String key;

    @Schema(description = "组织ID")
    private String organizationId;

    @Schema(description = "执行类型 cron", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{schedule.type.not_blank}", groups = {Created.class})
    @Size(min = 1, max = 50, message = "{schedule.type.length_range}", groups = {Created.class, Updated.class})
    private String type;

    @Schema(description = "cron 表达式", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{schedule.value.not_blank}", groups = {Created.class})
    @Size(min = 1, max = 255, message = "{schedule.value.length_range}", groups = {Created.class, Updated.class})
    private String value;

    @Schema(description = "Schedule Job Class Name", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{schedule.job.not_blank}", groups = {Created.class})
    @Size(min = 1, max = 64, message = "{schedule.job.length_range}", groups = {Created.class, Updated.class})
    private String job;

    @Schema(description = "资源类型 API_IMPORT,API_SCENARIO,UI_SCENARIO,LOAD_TEST,TEST_PLAN,CLEAN_REPORT,BUG_SYNC", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{schedule.resource_type.not_blank}", groups = {Created.class})
    @Size(min = 1, max = 50, message = "{schedule.resource_type.length_range}", groups = {Created.class, Updated.class})
    private String resourceType;

    @Schema(description = "是否开启")
    private Boolean enable;

    @Schema(description = "资源ID，api_scenario ui_scenario load_test")
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

    @Serial
    private static final long serialVersionUID = 1L;
}