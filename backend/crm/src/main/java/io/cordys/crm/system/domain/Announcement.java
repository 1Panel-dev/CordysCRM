package io.cordys.crm.system.domain;

import io.cordys.common.domain.BaseModel;
import io.cordys.common.groups.Created;
import io.cordys.common.groups.Updated;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class Announcement extends BaseModel {
    @Size(min = 1, max = 255, message = "{announcement.subject.length_range}", groups = {Created.class, Updated.class})
    @NotBlank(message = "{announcement.subject.not_blank}", groups = {Created.class, Updated.class})
    @Schema(description = "公告标题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String subject;
    @NotBlank(message = "{announcement.content.not_blank}", groups = {Created.class, Updated.class})
    @Schema(description = "公告内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;
    @NotBlank(message = "{announcement.start_time.not_blank}", groups = {Created.class, Updated.class})
    @Schema(description = "开始时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long startTime;
    @NotBlank(message = "{announcement.end_time.not_blank}", groups = {Created.class, Updated.class})
    @Schema(description = "结束时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long endTime;
    @Schema(description = "链接")
    private String url;
    @Size(min = 1, max = 50, message = "{announcement.organization_id.length_range}", groups = {Created.class, Updated.class})
    @NotBlank(message = "{announcement.organization_id.not_blank}", groups = {Created.class, Updated.class})
    @Schema(description = "接收人（组织架构）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String organizationId;
}
