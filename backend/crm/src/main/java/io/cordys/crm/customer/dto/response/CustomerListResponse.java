package io.cordys.crm.customer.dto.response;

import io.cordys.common.domain.BaseModuleFieldValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;


/**
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Data
public class CustomerListResponse {
    @Schema(description = "ID")
    private String id;

    @Schema(description = "客户名称")
    private String name;

    @Schema(description = "负责人")
    private String owner;

    @Schema(description = "负责人名称")
    private String ownerName;

    @Schema(description = "是否在公海池")
    private Boolean inSharedPool;

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

    @Schema(description = "归属部门")
    private String departmentId;

    @Schema(description = "归属部门名称")
    private String departmentName;

    @Schema(description = "最近跟进时间")
    private Long latestFollowUpTime;

    @Schema(description = "创建时间")
    private Long collectionTime;

    @Schema(description = "剩余归属天数")
    private Integer reservedDays;

    @Schema(description = "最新跟进人")
    private String follower;

    @Schema(description = "最新跟进人名称")
    private String followerName;

    @Schema(description = "最新跟进日期")
    private Long followTime;

    @Schema(description = "公海ID")
    private String poolId;

    @Schema(description = "默认回收公海名称")
    private String recyclePoolName;

    @Schema(description = "自定义字段集合")
    private List<BaseModuleFieldValue> moduleFields;
}
