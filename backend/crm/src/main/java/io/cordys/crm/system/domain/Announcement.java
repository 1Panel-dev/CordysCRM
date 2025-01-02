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
    private byte[] content;

    @NotBlank(message = "{announcement.startTime.not_blank}", groups = {Created.class, Updated.class})
    @Schema(description = "开始时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long startTime;

    @NotBlank(message = "{announcement.endTime.not_blank}", groups = {Created.class, Updated.class})
    @Schema(description = "结束时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long endTime;

    @Schema(description = "链接")
    private String url;

    @Size(min = 1, max = 1000, message = "{announcement.receiver.length_range}", groups = {Created.class, Updated.class})
    @NotBlank(message = "{announcement.receiver.not_blank}", groups = {Created.class, Updated.class})
    private String receiver;

    @Size(min = 1, max = 32, message = "{announcement.organizationId.length_range}", groups = {Created.class, Updated.class})
    @NotBlank(message = "{announcement.organizationId.not_blank}", groups = {Created.class, Updated.class})
    private String organizationId;

    @Size(min = 1, max = 64, message = "{announcement.receiver_type.length_range}", groups = {Created.class, Updated.class})
    @NotBlank(message = "{announcement.receiver_type.not_blank}", groups = {Created.class, Updated.class})
    private String receiverType;
}
