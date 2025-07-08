package io.cordys.crm.customer.dto.response;

import io.cordys.crm.customer.domain.CustomerContact;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CustomerContactRepeatResponse extends CustomerContact {

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "负责人名称")
    private String ownerName;

}
