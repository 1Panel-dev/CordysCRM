package cn.cordys.crm.approval.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ApprovalPushParam extends ApprovalResourceBaseParam{
	@Schema(description = "变更说明")
	private String comment;
}
