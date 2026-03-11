package cn.cordys.crm.order.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Table(name = "sales_order")
public class Order extends BaseModel {

    @Schema(description = "合同名称")
    private String name;

    @Schema(description = "客户id")
    private String customerId;

    @Schema(description = "合同id")
    private String contractId;

    @Schema(description = "合同负责人")
    private String owner;

    @Schema(description = "金额")
    private BigDecimal amount;

    @Schema(description = "编号")
    private String number;

    @Schema(description = "状态")
    private String stage;

    @Schema(description = "组织id")
    private String organizationId;
}
