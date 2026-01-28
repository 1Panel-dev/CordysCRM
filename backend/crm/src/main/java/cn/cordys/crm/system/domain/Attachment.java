package cn.cordys.crm.system.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * @author song-cc-rock
 */
@Data
@Table(name = "sys_attachment")
public class Attachment extends BaseModel {

  @Schema(description = "名称")
  private String name;

  @Schema(description = "类型")
  private String type;

  @Schema(description = "大小")
  private Long size;

  @Schema(description = "存储方式")
  private String storage;

  @Schema(description = "资源ID")
  private String resourceId;

  @Schema(description = "组织ID")
  private String organizationId;
}
