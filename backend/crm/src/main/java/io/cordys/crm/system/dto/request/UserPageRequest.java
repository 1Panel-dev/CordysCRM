package io.cordys.crm.system.dto.request;

import io.cordys.common.pager.condition.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
public class UserPageRequest extends BasePageRequest {

    @NotBlank
    @Schema(description = "部门id")
    private String departmentId;

}
