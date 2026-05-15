package cn.cordys.crm.approval.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResourceSnapshotApprovalParam {

	private String resourceId;
	private String approvalStatus;
}
