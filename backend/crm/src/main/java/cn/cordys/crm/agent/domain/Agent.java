package cn.cordys.crm.agent.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;



@Data
@Table(name = "agent")
public class Agent extends BaseModel {

	@Schema(description = "名称")
	private String name;

	@Schema(description = "模块id")
	private String agentModuleId;

	@Schema(description = "组织id")
	private String organizationId;

	@Schema(description = "同一节点下顺序")
	private Long pos;

	@Schema(description = "应用范围")
	private String scopeId;

	@Schema(description = "嵌入脚本")
	private String script;

	@Schema(description = "描述")
	private String description;
}
