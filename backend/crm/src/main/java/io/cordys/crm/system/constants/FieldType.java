package io.cordys.crm.system.constants;

import lombok.Getter;

@Getter
public enum FieldType {

	/**
	 * 单行输入
	 */
	INPUT,
	/**
	 * 多行输入
	 */
	TEXTAREA,
	/**
	 * 数字
	 */
	NUMBER,
	/**
	 * 日期时间
	 */
	DATETIME,
	/**
	 * 单选
	 */
	RADIO,
	/**
	 * 多选
	 */
	CHECKBOX,
	/**
	 * 单选下拉
	 */
	SELECT,
	/**
	 * 多选下拉
	 */
	MULTI_SELECT,
	/**
	 * 单选成员
	 */
	MEMBER,
	/**
	 * 多选成员
	 */
	MULTI_MEMBER,
	/**
	 * 单选部门
	 */
	DEPARTMENT,
	/**
	 * 多选部门
	 */
	MULTI_DEPARTMENT,
	/**
	 * 分割线
	 */
	DIVIDER
}
