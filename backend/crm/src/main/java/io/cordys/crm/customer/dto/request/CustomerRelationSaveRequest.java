package io.cordys.crm.customer.dto.request;

import io.cordys.common.constants.EnumValue;
import io.cordys.crm.customer.constants.CustomerRelationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 *
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRelationSaveRequest {
    @Size(max = 32)
    @Schema(description = "客户ID")
    private String customerId;

    @Schema(description = "关系类型")
    @EnumValue(enumClass = CustomerRelationType.class)
    private String relationType;
}