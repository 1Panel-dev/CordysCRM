package cn.cordys.crm.approval.dto;

import cn.cordys.common.util.JSON;
import lombok.Data;

import java.util.List;

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

	public Object getFieldValue() {
		if (fieldValue == null) {
			return null;
		}
		if (fieldValue instanceof List<?>) {
			return JSON.toJSONString(fieldValue);
		}
		return fieldValue;
	}
}
