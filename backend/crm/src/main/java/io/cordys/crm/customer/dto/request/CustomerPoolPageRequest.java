package io.cordys.crm.customer.dto.request;

import io.cordys.common.groups.Created;
import io.cordys.common.groups.Updated;
import io.cordys.common.pager.condition.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CustomerPoolPageRequest extends BasePageRequest {
}
