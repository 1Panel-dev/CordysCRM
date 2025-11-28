package cn.cordys.crm.contract.dto.response;

import cn.cordys.common.domain.BaseModuleFieldValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ContractListResponse {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "合同名称")
    private String name;

    @Schema(description = "客户id")
    private String customerId;

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "累计金额")
    private BigDecimal amount;

    @Schema(description = "归档状态")
    private String archivedStatus;

    @Schema(description = "合同状态")
    private String status;

    @Schema(description = "负责人")
    private String owner;

    @Schema(description = "负责人名称")
    private String ownerName;

    @Schema(description = "创建人")
    private String createUser;

    @Schema(description = "修改人")
    private String updateUser;

    @Schema(description = "创建时间")
    private Long createTime;

    @Schema(description = "更新时间")
    private Long updateTime;

    @Schema(description = "创建人名称")
    private String createUserName;

    @Schema(description = "更新人名称")
    private String updateUserName;

    @Schema(description = "关联的客户是否在公海")
    private Boolean inCustomerPool;

    @Schema(description = "客户公海id")
    private String poolId;

    @Schema(description = "作废原因")
    private String voidReason;

    @Schema(description = "部门id")
    private String departmentId;

    @Schema(description = "部门名称")
    private String departmentName;

    @Schema(description = "自定义字段")
    private List<BaseModuleFieldValue> moduleFields;
}
