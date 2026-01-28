package cn.cordys.crm.system.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class MessageTaskDTO implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  @Schema(description = "消息配置模块")
  public String module;

  @Schema(description = "消息配置模块名称")
  public String moduleName;

  @Schema(description = "消息配置功能展开内容")
  public List<MessageTaskDetailDTO> messageTaskDetailDTOList;
}
