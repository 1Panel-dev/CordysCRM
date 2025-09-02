package cn.cordys.crm.clue.dto.response;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.OptionDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 *
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Data
public class ClueGetResponse {

    @Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Schema(description = "客户名称")
    private String name;

    @Schema(description = "负责人")
    private String owner;

    @Schema(description = "阶段")
    private String stage;

    @Schema(description = "上次修改前的线索阶段")
    private String lastStage;

    @Schema(description = "负责人姓名")
    private String ownerName;

    @Schema(description = "联系人名称")
    private String contact;

    @Schema(description = "联系人电话")
    private String phone;

    @Schema(description = "意向产品")
    private List<String> products;

    @Schema(description = "归属部门")
    private String departmentId;

    @Schema(description = "归属部门名称")
    private String departmentName;

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

    @Schema(description = "领取时间")
    private Long collectionTime;

    @Schema(description = "剩余归属天数")
    private Integer reservedDays;

    @Schema(description = "最新跟进人")
    private String follower;

    @Schema(description = "最新跟进人名称")
    private String followerName;

    @Schema(description = "最新跟进日期")
    private Long followTime;

    @Schema(description = "线索池ID")
    private String poolId;

    @Schema(description = "默认回收公海名称")
    private String recyclePoolName;

    @Schema(description = "转移类型")
    private String transitionType;

    @Schema(description = "失败原因ID")
    private String reasonId;

    @Schema(description = "失败原因ID")
    private String reasonName;

    @Schema(description = "自定义字段")
    private List<BaseModuleFieldValue> moduleFields;

    @Schema(description = "选项集合")
    private Map<String, List<OptionDTO>> optionMap;
}
