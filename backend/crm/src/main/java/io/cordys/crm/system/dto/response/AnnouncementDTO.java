package io.cordys.crm.system.dto.response;

import io.cordys.crm.system.domain.Announcement;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class AnnouncementDTO extends Announcement{

    @Schema(description = "公告内容")
    private String contentText;

    @Schema(description = "创建人名称")
    private String createUserName;

    @Schema(description = "操作人")
    private String updateUserName;

    @Schema(description = "部门对应关系")
    private List<OptionScopeDTO> deptIdName;

    @Schema(description = "用户对应关系")
    private List<OptionScopeDTO> userIdName;

}
