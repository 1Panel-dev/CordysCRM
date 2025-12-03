package cn.cordys.crm.contract.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Table(name = "contract")
public class Contract extends BaseModel {

    @Schema(description = "合同名称")
    private String name;

    @Schema(description = "客户id")
    private String customerId;

    @Schema(description = "合同负责人")
    private String owner;

    @Schema(description = "金额")
    private BigDecimal amount;

    @Schema(description = "编号")
    private String number;

    @Schema(description = "审核状态")
    private String approvalStatus;

    @Schema(description = "归档状态")
    private String archivedStatus;

    @Schema(description = "合同状态")
    private String status;

    @Schema(description = "作废原因")
    private String voidReason;

    @Schema(description = "组织id")
    private String organizationId;
}
