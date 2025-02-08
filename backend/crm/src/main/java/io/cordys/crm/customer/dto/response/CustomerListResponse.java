package io.cordys.crm.customer.dto.response;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import io.cordys.crm.customer.domain.Customer;


/**
 *
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Data
public class CustomerListResponse extends Customer {
    @Schema(description = "创建人名称")
    private String createUserName;

    @Schema(description = "更新人名称")
    private String updateUserName;
}
