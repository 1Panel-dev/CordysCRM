package io.cordys.crm.clue.domain;

import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "clue_pool_relation")
public class CluePoolRelation extends BaseModel {

	@Schema(description = "线索池ID")
	private String poolId;
	@Schema(description = "线索ID")
	private String clueId;
	@Schema(description = "是否领取")
	private Boolean picked;
	@Schema(description = "上一次领取人")
	private String lastPickUserId;
	@Schema(description = "上一次领取时间")
	private Long lastPickTime;
}
