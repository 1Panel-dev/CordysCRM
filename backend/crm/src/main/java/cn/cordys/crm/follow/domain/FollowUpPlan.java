package cn.cordys.crm.follow.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "follow_up_plan")
public class FollowUpPlan extends BaseModel {

  @Schema(description = "客户id")
  private String customerId;

  @Schema(description = "商机id")
  private String opportunityId;

  @Schema(description = "类型")
  private String type;

  @Schema(description = "线索id")
  private String clueId;

  @Schema(description = "预计沟通内容")
  private String content;

  @Schema(description = "组织id")
  private String organizationId;

  @Schema(description = "负责人")
  private String owner;

  @Schema(description = "联系人")
  private String contactId;

  @Schema(description = "预计开始时间")
  private Long estimatedTime;

  @Schema(description = "跟进方式")
  private String method;

  @Schema(description = "状态")
  private String status;

  @Schema(description = "是否转为跟进记录")
  private Boolean converted;
}
