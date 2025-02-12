package io.cordys.crm.system.constants;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@Getter
public enum FieldType {

	/**
	 * 单行输入
	 */
	INPUT("input"),
	/**
	 * 多行输入
	 */
	TEXTAREA("textarea"),
	/**
	 * 数字
	 */
	NUMBER("number"),
	/**
	 * 日期时间
	 */
	DATETIME("datetime"),
	/**
	 * 单选
	 */
	RADIO("radio"),
	/**
	 * 多选
	 */
	CHECKBOX("checkbox"),
	/**
	 * 单选下拉
	 */
	SELECT("select"),
	/**
	 * 多选下拉
	 */
	MULTI_SELECT("multi_select"),
	/**
	 * 单选成员
	 */
	MEMBER("member"),
	/**
	 * 多选成员
	 */
	MULTI_MEMBER("multi_member"),
	/**
	 * 单选部门
	 */
	DEPARTMENT("department"),
	/**
	 * 多选部门
	 */
	MULTI_DEPARTMENT("multi_department"),
	/**
	 * 分割线
	 */
	DIVIDER("divider");

	private final String type;

	FieldType(String type) {
		this.type = type;
	}

	public static boolean hasOption(String type) {
		return StringUtils.equalsAny(type, RADIO.type, CHECKBOX.type, SELECT.type, MULTI_SELECT.type);
	}
}
