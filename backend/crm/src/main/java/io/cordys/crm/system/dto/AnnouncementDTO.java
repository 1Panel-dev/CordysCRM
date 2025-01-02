package io.cordys.crm.system.dto;

import io.cordys.crm.system.domain.Announcement;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AnnouncementDTO extends Announcement {

    @Schema(description = "创建人名称")
    private String createUserName;

    @Schema(description = "接收人名称")
    private String organizationName;
}
