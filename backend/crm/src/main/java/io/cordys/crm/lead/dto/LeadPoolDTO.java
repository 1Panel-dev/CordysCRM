package io.cordys.crm.lead.dto;

import io.cordys.crm.lead.domain.LeadPool;
import io.cordys.crm.lead.dto.request.LeadPoolPickRuleSaveRequest;
import io.cordys.crm.lead.dto.request.LeadPoolRecycleRuleSaveRequest;
import io.cordys.crm.system.dto.ScopeNameDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class LeadPoolDTO extends LeadPool {

	@Schema(description = "成员集合")
	private List<ScopeNameDTO> members;
	@Schema(description = "管理员集合")
	private List<ScopeNameDTO> owners;
	@Schema(description = "创建人名称")
	private String createUserName;
	@Schema(description = "更新人名称")
	private String updateUserName;
	@Schema(description = "规则")
	private LeadPoolRuleDTO rule;
}
