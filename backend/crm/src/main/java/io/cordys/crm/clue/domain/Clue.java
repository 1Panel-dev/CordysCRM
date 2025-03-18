package io.cordys.crm.clue.domain;

import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;


/**
 * 线索
 * 
 * @author jianxing
 * @date 2025-03-10 11:57:28
 */
@Data
@Table(name = "clue")
public class Clue extends BaseModel {

	@Schema(description = "客户名称")
	private String name;

	@Schema(description = "负责人")
	private String owner;

	@Schema(description = "状态")
	private String status;

	@Schema(description = "联系人名称")
	private String contact;

	@Schema(description = "联系人电话")
	private String phone;

	@Schema(description = "组织id")
	private String organizationId;

	@Schema(description = "创建时间")
	private Long collectionTime;

	@Schema(description = "是否在线索池")
	private Boolean inSharedPool;

	@Schema(description = "转移成客户或者线索")
	private String transitionType;

	@Schema(description = "客户id或者线索id")
	private String transitionId;

	@Schema(description = "最新跟进人")
	private String follower;

	@Schema(description = "最新跟进时间")
	private Long followTime;

	@Schema(description = "线索池ID")
	private String poolId;
}
