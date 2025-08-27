package cn.cordys.crm.follow.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;


@Data
@Table(name = "follow_up_record")
public class FollowUpRecord extends BaseModel {

	@Schema(description = "客户id")
	private String customerId;

	@Schema(description = "商机id")
	private String opportunityId;

	@Schema(description = "类型")
	private String type;

	@Schema(description = "线索id")
	private String clueId;

	@Schema(description = "跟进内容")
	private String content;

	@Schema(description = "组织id")
	private String organizationId;

	@Schema(description = "跟进时间")
	private Long followTime;

	@Schema(description = "跟进方式")
	private String followMethod;

	@Schema(description = "负责人")
	private String owner;

	@Schema(description = "联系人")
	private String contactId;
}
