package io.cordys.crm.clue.domain;

import jakarta.persistence.Table;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


/**
 * 线索历史责任人
 * 
 * @author jianxing
 * @date 2025-03-12 15:46:27
 */
@Data
@Table(name = "clue_owner")
public class ClueOwner  {

	@Schema(description = "id")
	private String id;

	@Schema(description = "线索id")
	private String clueId;

	@Schema(description = "责任人")
	private String owner;

	@Schema(description = "领取时间")
	private Long collectionTime;

	@Schema(description = "结束时间")
	private Long endTime;

	@Schema(description = "操作人")
	private String operator;
}
