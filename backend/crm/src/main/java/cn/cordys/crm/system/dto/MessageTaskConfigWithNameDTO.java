package cn.cordys.crm.system.dto;

import cn.cordys.common.dto.OptionDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

@Data
public class MessageTaskConfigWithNameDTO extends MessageTaskConfigDTO implements Serializable {

  @Schema(description = "通知人员ids,包含特殊值，负责人OWNER/创建人CREATE_USER")
  private List<OptionDTO> userIdNames;

  @Schema(description = "通知角色ids")
  private List<OptionDTO> roleIdNames;
}
