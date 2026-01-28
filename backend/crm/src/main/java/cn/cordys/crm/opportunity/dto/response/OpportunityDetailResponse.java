package cn.cordys.crm.opportunity.dto.response;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.crm.system.domain.Attachment;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.Data;

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

  @Schema(description = "商机阶段名称")
  private String stageName;

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

  @Schema(description = "结束时间")
  private Long expectedEndTime;

  @Schema(description = "结束时间")
  private Long actualEndTime;

  @Schema(description = "失败原因")
  private String failureReason;

  @Schema(description = "关联的客户是否在公海")
  private Boolean inCustomerPool;

  @Schema(description = "客户公海id")
  private String poolId;

  @Schema(description = "剩余归属天数")
  private Integer reservedDays;

  @Schema(description = "最新跟进人")
  private String follower;

  @Schema(description = "最新跟进人名称")
  private String followerName;

  @Schema(description = "最新跟进日期")
  private Long followTime;

  @Schema(description = "部门id")
  private String departmentId;

  @Schema(description = "部门名称")
  private String departmentName;

  @Schema(description = "组织ID")
  private String organizationId;

  @Schema(description = "自定义字段")
  private List<BaseModuleFieldValue> moduleFields;

  @Schema(description = "选项集合")
  private Map<String, List<OptionDTO>> optionMap;

  /** 附件集合 */
  @Schema(description = "附件集合")
  private Map<String, List<Attachment>> attachmentMap;
}
