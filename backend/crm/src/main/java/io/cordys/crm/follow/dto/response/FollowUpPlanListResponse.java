package io.cordys.crm.follow.dto.response;

import io.cordys.common.domain.BaseModuleFieldValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class FollowUpPlanListResponse {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "客户id")
    private String customerId;

    @Schema(description = "商机id")
    private String opportunityId;

    @Schema(description = "类型")
    private String type;

    @Schema(description = "线索id")
    private String clueId;

    @Schema(description = "计划内容")
    private String content;

    @Schema(description = "组织id")
    private String organizationId;

    @Schema(description = "负责人")
    private String owner;

    @Schema(description = "负责人名称")
    private String ownerName;

    @Schema(description = "联系人")
    private String contactId;

    @Schema(description = "联系人名称")
    private String contactName;

    @Schema(description = "创建人")
    private String createUser;

    @Schema(description = "创建人名称")
    private String createUserName;

    @Schema(description = "更新人")
    private String updateUser;

    @Schema(description = "更新人名称")
    private String updateUserName;

    @Schema(description = "创建时间")
    private Long createTime;

    @Schema(description = "更新时间")
    private Long updateTime;

    @Schema(description = "计划开始时间")
    private Long estimatedTime;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "自定义字段集合")
    private List<BaseModuleFieldValue> moduleFields;
}
