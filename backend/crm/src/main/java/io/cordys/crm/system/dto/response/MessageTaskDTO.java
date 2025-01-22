package io.cordys.crm.system.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class MessageTaskDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "消息配置功能")
    public String type;

    @Schema(description = "消息配置功能名称")
    public String name;

    @Schema(description = "消息配置功能展开内容")
    public List<MessageTaskDetailDTO> messageTaskDetailDTOList;

}
