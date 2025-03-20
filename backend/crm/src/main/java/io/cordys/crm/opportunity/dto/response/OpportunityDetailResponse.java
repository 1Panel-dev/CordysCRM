package io.cordys.crm.opportunity.dto.response;

import io.cordys.common.domain.BaseModuleFieldValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OpportunityDetailResponse {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "商机名称")
    private String name;

    @Schema(description = "客户id")
    private String customerId;

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "金额")
    private BigDecimal amount;

    @Schema(description = "可能性")
    private BigDecimal possible;

    @Schema(description = "意向产品")
    private List<String> products;

    @Schema(description = "联系人")
    private String contactId;

    @Schema(description = "联系人名称")
    private String contactName;

    @Schema(description = "上次修改前的商机阶段")
    private String lastStage;

    @Schema(description = "商机阶段")
    private String stage;

    @Schema(description = "商机状态")
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

    @Schema(description = "自定义字段")
    private List<BaseModuleFieldValue> moduleFields;
}
