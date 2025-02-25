package io.cordys.crm.system.dto.request;

import io.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;


@Data
public class UserPageRequest extends BasePageRequest {

    @Schema(description = "部门id")
    private List<@NotBlank String> departmentIds;

}
