package io.cordys.crm.customer.dto.response;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import io.cordys.crm.customer.domain.CustomerContact;

/**
 *
 * @author jianxing
 * @date 2025-02-24 11:06:10
 */
@Data
public class CustomerContactGetResponse extends CustomerContact {
    @Schema(description = "归属部门")
    private String departmentId;

    @Schema(description = "归属部门名称")
    private String departmentName;

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "创建人名称")
    private String createUserName;

    @Schema(description = "更新人名称")
    private String updateUserName;
}
