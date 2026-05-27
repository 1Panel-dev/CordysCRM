package cn.cordys.crm.approval.dto;

import lombok.Data;

@Data
public class ResourceApprovalFieldUpdateParam {
	/**
	 * 字段ID
	 */
	private String fieldId;
	/**
	 * 字段更新值
	 */
	private Object fieldValue;
	/**
	 * 是否开启
	 */
	private boolean enable;
}
