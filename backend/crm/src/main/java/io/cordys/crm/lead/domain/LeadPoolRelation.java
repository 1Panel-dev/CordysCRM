package io.cordys.crm.lead.domain;

import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "lead_pool_relation")
public class LeadPoolRelation extends BaseModel {

	@Schema(description = "线索池ID")
	private String poolId;
	@Schema(description = "到期提醒")
	private String leadId;
	@Schema(description = "上一次领取人")
	private String lastPickUserId;
	@Schema(description = "上一次领取时间")
	private Long lastPickTime;
}
