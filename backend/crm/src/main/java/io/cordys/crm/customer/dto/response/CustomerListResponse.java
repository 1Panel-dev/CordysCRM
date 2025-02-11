package io.cordys.crm.customer.dto.response;

import io.cordys.crm.common.dto.ModuleFieldValueDTO;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;


/**
 *
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Data
public class CustomerListResponse {
    @Schema(description = "ID")
    private String id;

    @Schema(description = "客户名称")
    private String name;

    @Schema(description = "标签")
    private String tags;

    @Schema(description = "是否在公海池")
    private Boolean inSharedPool;

    @Schema(description = "最终成交状态")
    private String dealStatus;

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

    // todo
    @Schema(description = "最近跟进时间")
    private Long latestFollowUpTime;

    @Schema(description = "最长归属天数")
    private Integer reservedMaxDays = 180;

    @Schema(description = "剩余归属天数")
    private Integer reservedDays;

    @Schema(description = "自定义字段集合")
    private List<ModuleFieldValueDTO> moduleFields;
}
