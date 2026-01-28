package cn.cordys.crm.opportunity.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "opportunity_rule")
public class OpportunityRule extends BaseModel {

  @Schema(description = "规则名称")
  private String name;

  @Schema(description = "组织ID")
  private String organizationId;

  @Schema(description = "管理员ID")
  private String ownerId;

  @Schema(description = "范围ID")
  private String scopeId;

  @Schema(description = "启用/禁用")
  private Boolean enable;

  @Schema(description = "自动回收")
  private Boolean auto;

  @Schema(description = "操作符")
  private String operator;

  @Schema(description = "回收条件")
  private String condition;
}
