package cn.cordys.crm.system.dto.field;

import cn.cordys.crm.system.dto.field.base.BaseField;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@JsonTypeName(value = "ATTACHMENT")
@EqualsAndHashCode(callSuper = true)
public class AttachmentField extends BaseField {

  @Schema(description = "仅允许单文件上传")
  private Boolean onlyOne;

  @Schema(description = "允许上传的文件类型，逗号分隔")
  private String accept;

  @Schema(description = "单个文件大小限制, 默认100MB")
  private String limitSize;
}
