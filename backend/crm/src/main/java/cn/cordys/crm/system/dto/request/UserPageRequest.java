package cn.cordys.crm.system.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Data;

@Data
public class UserPageRequest extends BasePageRequest {

  @Schema(description = "部门id")
  private List<@NotBlank String> departmentIds;
}
