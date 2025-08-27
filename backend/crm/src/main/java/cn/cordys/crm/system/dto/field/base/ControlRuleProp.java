package cn.cordys.crm.system.dto.field.base;

import lombok.Data;

import java.util.List;

@Data
public class ControlRuleProp {

	/**
	 * 选项值ID
	 */
	private String value;
	/**
	 * 字段显示ID集合
	 */
	private List<String> fieldIds;
}
