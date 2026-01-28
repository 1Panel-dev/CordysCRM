package cn.cordys.crm.system.dto.field;

import cn.cordys.crm.system.domain.User;
import cn.cordys.crm.system.dto.field.base.BaseField;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@JsonTypeName(value = "MEMBER")
@EqualsAndHashCode(callSuper = true)
public class MemberField extends BaseField {

  @Schema(description = "默认值")
  private String defaultValue;

  @Schema(description = "是否当前用户")
  private Boolean hasCurrentUser;

  @Schema(description = "默认值初始化选项")
  private List<User> initialOptions;
}
