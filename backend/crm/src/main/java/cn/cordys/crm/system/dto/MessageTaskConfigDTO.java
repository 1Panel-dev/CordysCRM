package cn.cordys.crm.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class MessageTaskConfigDTO implements Serializable {

    @Schema(description = "时间列表")
    private List<TimeDTO> timeList;

    @Schema(description = "通知人员ids,包含特殊值，负责人OWNER/创建人CREATE_USER")
    private List<String> userIds;

    @Schema(description = "通知角色ids")
    private List<String> roleIds;

    @Schema(description = "是否通知负责人")
    private boolean ownerEnable;

    @Schema(description = "负责人层级")
    private int ownerLevel;

    @Schema(description = "是否通知角色")
    private boolean roleEnable;
}
