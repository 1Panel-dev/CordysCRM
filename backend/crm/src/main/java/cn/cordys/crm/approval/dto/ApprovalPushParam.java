package cn.cordys.crm.approval.dto;

import cn.cordys.crm.approval.constants.ExecuteTimingEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalPushParam extends ApprovalResourceBaseParam{
	private String resourceId;
	private String formKey;
	private String comment;
	private String orgId;
	private String userId;
	private ExecuteTimingEnum executeTimingEnum;
	private String updateFields;
}
