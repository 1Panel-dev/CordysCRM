package io.cordys.crm.clue.dto;

import io.cordys.crm.clue.domain.CluePool;
import io.cordys.crm.system.dto.ScopeNameDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class CluePoolDTO extends CluePool {

	@Schema(description = "成员集合")
	private List<ScopeNameDTO> members;
	@Schema(description = "管理员集合")
	private List<ScopeNameDTO> owners;
	@Schema(description = "创建人名称")
	private String createUserName;
	@Schema(description = "更新人名称")
	private String updateUserName;
	@Schema(description = "领取规则")
	private CluePoolPickRuleDTO pickRule;
	@Schema(description = "回收规则")
	private CluePoolRecycleRuleDTO recycleRule;
	@Schema(description = "字段配置")
	private List<CluePoolFieldConfigDTO> fieldConfigs;

	@Schema(description = "是否可编辑")
	private Boolean editable;
}
