package io.cordys.crm.system.dto.request;

import io.cordys.common.groups.Created;
import io.cordys.common.groups.Updated;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class AnnouncementRequest{

    @Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{user.id.not_blank}", groups = {Updated.class})
    @Size(min = 1, max = 32, message = "{user.id.length_range}", groups = {Created.class, Updated.class})
    private String id;

    @Size(min = 1, max = 255, message = "{announcement.subject.length_range}", groups = {Created.class, Updated.class})
    @NotBlank(message = "{announcement.subject.not_blank}", groups = {Created.class, Updated.class})
    @Schema(description = "公告标题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String subject;

    @NotBlank(message = "{announcement.content.not_blank}", groups = {Created.class, Updated.class})
    @Schema(description = "公告内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @NotBlank(message = "{announcement.startTime.not_blank}", groups = {Created.class, Updated.class})
    @Schema(description = "开始时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long startTime;

    @NotBlank(message = "{announcement.endTime.not_blank}", groups = {Created.class, Updated.class})
    @Schema(description = "结束时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long endTime;

    @Schema(description = "链接")
    private String url;

    @Size(min = 1, max = 32, message = "{announcement.organizationId.length_range}", groups = {Created.class, Updated.class})
    @NotBlank(message = "{announcement.organizationId.not_blank}", groups = {Created.class, Updated.class})
    private String organizationId;

    @Schema(description = "部门ID")
    private List<String> deptIds;

    @Schema(description = "角色ID")
    private List<String> roleIds;

    @Schema(description = "用户ID")
    private List<String> userIds;


}
