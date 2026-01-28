package cn.cordys.crm.opportunity.dto;

import cn.cordys.crm.opportunity.domain.OpportunityRule;
import cn.cordys.crm.system.dto.ScopeNameDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Data
public class OpportunityRuleDTO extends OpportunityRule {

  @Schema(description = "成员集合")
  private List<ScopeNameDTO> members;

  @Schema(description = "管理员集合")
  private List<ScopeNameDTO> owners;

  @Schema(description = "创建人名称")
  private String createUserName;

  @Schema(description = "更新人名称")
  private String updateUserName;
}
