package cn.cordys.crm.agent.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;


@Data
@Table(name = "agent_collection")
public class AgentCollection extends BaseModel {

	@Schema(description = "用户id")
	private String userId;

	@Schema(description = "智能体id")
	private String agent_id;
}
