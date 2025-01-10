package io.cordys.crm.system.dto.request;

import io.cordys.common.pager.condition.BasePageRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
public class UserPageRequest extends BasePageRequest {

    @NotBlank
    private String departmentId;

}
