package io.cordys.crm.search.response.global;

import io.cordys.common.domain.BaseModuleFieldValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class GlobalOpportunityResponse {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "商机名称")
    private String name;

    @Schema(description = "客户id")
    private String customerId;

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "负责人")
    private String owner;

    @Schema(description = "负责人名称")
    private String ownerName;

    @Schema(description = "意向产品")
    private List<String> products;

    @Schema(description = "商机阶段")
    private String stage;

    @Schema(description = "部门名称")
    private String departmentName;

    @Schema(description = "是否有当前数据的权限")
    private boolean hasPermission;

    @Schema(description = "自定义字段集合")
    private List<BaseModuleFieldValue> moduleFields;
}
