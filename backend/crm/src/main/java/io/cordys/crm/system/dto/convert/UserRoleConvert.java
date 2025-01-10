package io.cordys.crm.system.dto.convert;

import io.cordys.common.dto.OptionDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class UserRoleConvert extends OptionDTO {
    @Schema(description = "选项ID")
    private String userId;
}
