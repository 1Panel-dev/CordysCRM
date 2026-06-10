package cn.cordys.crm.approval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 资源审批后置更新参数
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResourceApprovalPostUpdateParam {

	/**
	 * 更新字段参数
	 */
	private List<ResourceApprovalFieldUpdateParam> fields;

	/**
	 * 资源ID
	 */
	private String resourceId;

	/**
	 * 操作人
	 */
	private String operator;
}
