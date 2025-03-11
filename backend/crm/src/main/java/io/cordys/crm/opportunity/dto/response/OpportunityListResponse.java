package io.cordys.crm.opportunity.dto.response;

import io.cordys.common.domain.BaseModuleFieldValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class OpportunityListResponse {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "商机名称")
    private String opportunityName;

    @Schema(description = "客户名称")
    private String customerName;

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

    @Schema(description = "剩余归属天数")
    private Integer reservedDays;

    @Schema(description = "最新跟进人")
    private String lastFollower;

    @Schema(description = "最新跟进人名称")
    private String lastFollowerName;

    @Schema(description = "最新跟进日期")
    private Long lastFollowTime;

    @Schema(description = "自定义字段集合")
    private List<? extends BaseModuleFieldValue> moduleFields;
}
