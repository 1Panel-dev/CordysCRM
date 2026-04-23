package cn.cordys.crm.approval.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ApprovalCcNode {

	@Schema(description = "抄送人ID")
	private String ccUserId;
	@Schema(description = "抄送人名称")
	private String ccUserName;
	@Schema(description = "抄送人头像")
	private String ccUserAvatar;
}
